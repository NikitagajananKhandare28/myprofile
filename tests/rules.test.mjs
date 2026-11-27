import { readFileSync } from "node:fs";
import { after, before, beforeEach, describe, it } from "node:test";
import { assertFails, assertSucceeds, initializeTestEnvironment } from "@firebase/rules-unit-testing";
import { collection, doc, getDoc, getDocs, serverTimestamp, setDoc } from "firebase/firestore";

const rules = readFileSync(new URL("../firebase/firestore.rules", import.meta.url), "utf8");
let env;

before(async () => {
  env = await initializeTestEnvironment({ projectId: "demo-myprofile", firestore: { rules } });
});
after(async () => { await env.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); });

const alice = () => env.authenticatedContext("alice", { phone_number: "+919999900001" }).firestore();
const bob = () => env.authenticatedContext("bob", { phone_number: "+919999900002" }).firestore();
const anon = () => env.unauthenticatedContext().firestore();
const boss = () => env.authenticatedContext("boss", { admin: true }).firestore();
const seed = (fn) => env.withSecurityRulesDisabled(async (ctx) => fn(ctx.firestore()));

const newProfile = (uid) => ({
  ownerUid: uid, displayName: "Test User", profileType: "Student", city: "Pune",
  bio: "Hello", status: "draft", schemaVersion: 1,
  createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
});

const contact = (visibility) => ({
  phone: "+919999900001", phoneVisibility: visibility,
  whatsapp: "", whatsappVisibility: "private",
  email: "", emailVisibility: "private",
  location: "", locationVisibility: "private",
  updatedAt: serverTimestamp(),
});

const publicDoc = (uid, name) => ({
  ownerUid: uid, username: name, displayName: "Alice", profileType: "Student",
  updatedAt: serverTimestamp(),
});

describe("profiles", () => {
  it("owner can create a draft profile", async () => {
    await assertSucceeds(setDoc(doc(alice(), "profiles/alice"), newProfile("alice")));
  });

  it("cannot create a profile for someone else", async () => {
    await assertFails(setDoc(doc(alice(), "profiles/bob"), newProfile("bob")));
  });

  it("cannot create a profile that is already published", async () => {
    await assertFails(setDoc(doc(alice(), "profiles/alice"), { ...newProfile("alice"), status: "published" }));
  });

  it("only the owner can read a profile", async () => {
    await seed((db) => setDoc(doc(db, "profiles/alice"), { ownerUid: "alice", displayName: "Alice" }));
    await assertSucceeds(getDoc(doc(alice(), "profiles/alice")));
    await assertFails(getDoc(doc(bob(), "profiles/alice")));
    await assertFails(getDoc(doc(anon(), "profiles/alice")));
  });
});

describe("public profiles and usernames", () => {
  it("a public profile can be read by name but not listed", async () => {
    await seed((db) => setDoc(doc(db, "publicProfiles/alice-pro"), publicDoc("alice", "alice-pro")));
    await assertSucceeds(getDoc(doc(anon(), "publicProfiles/alice-pro")));
    await assertFails(getDocs(collection(anon(), "publicProfiles")));
  });

  it("owner can publish under a name they own", async () => {
    await seed(async (db) => {
      await setDoc(doc(db, "usernames/alice-pro"), { uid: "alice" });
      await setDoc(doc(db, "profiles/alice"), { ownerUid: "alice", status: "draft" });
      await setDoc(doc(db, "users/alice"), { isMinor: false });
    });
    await assertSucceeds(setDoc(doc(alice(), "publicProfiles/alice-pro"), publicDoc("alice", "alice-pro")));
  });

  it("cannot publish under a name owned by someone else", async () => {
    await seed(async (db) => {
      await setDoc(doc(db, "usernames/taken-name"), { uid: "bob" });
      await setDoc(doc(db, "profiles/alice"), { ownerUid: "alice", status: "draft" });
      await setDoc(doc(db, "users/alice"), { isMinor: false });
    });
    await assertFails(setDoc(doc(alice(), "publicProfiles/taken-name"), publicDoc("alice", "taken-name")));
  });

  it("reserved and invalid names cannot be claimed", async () => {
    await assertFails(setDoc(doc(alice(), "usernames/admin"), { uid: "alice", createdAt: serverTimestamp() }));
    await assertFails(setDoc(doc(alice(), "usernames/ab"), { uid: "alice", createdAt: serverTimestamp() }));
    await assertSucceeds(setDoc(doc(alice(), "usernames/alice-pro"), { uid: "alice", createdAt: serverTimestamp() }));
  });
});

describe("contact privacy", () => {
  it("a minor cannot make contact details public", async () => {
    await seed(async (db) => {
      await setDoc(doc(db, "users/alice"), { isMinor: true });
      await setDoc(doc(db, "profiles/alice"), { ownerUid: "alice", status: "draft" });
    });
    await assertFails(setDoc(doc(alice(), "profiles/alice/contact/main"), contact("public")));
    await assertSucceeds(setDoc(doc(alice(), "profiles/alice/contact/main"), contact("private")));
  });

  it("an adult can make contact details public", async () => {
    await seed(async (db) => {
      await setDoc(doc(db, "users/alice"), { isMinor: false });
      await setDoc(doc(db, "profiles/alice"), { ownerUid: "alice", status: "draft" });
    });
    await assertSucceeds(setDoc(doc(alice(), "profiles/alice/contact/main"), contact("public")));
  });

  it("another user cannot read someone's contact details", async () => {
    await seed((db) => setDoc(doc(db, "profiles/alice/contact/main"), { phone: "+919999900001" }));
    await assertFails(getDoc(doc(bob(), "profiles/alice/contact/main")));
  });
});

describe("reports", () => {
  const report = () => ({
    username: "alice-pro", reason: "Spam", message: "test",
    status: "open", createdAt: serverTimestamp(),
  });

  it("anyone can file a report", async () => {
    await assertSucceeds(setDoc(doc(anon(), "reports/r1"), report()));
  });

  it("reports cannot be filed with a made-up reason", async () => {
    await assertFails(setDoc(doc(anon(), "reports/r2"), { ...report(), reason: "Because" }));
  });

  it("only admins can read reports", async () => {
    await seed((db) => setDoc(doc(db, "reports/r1"), { username: "alice-pro", status: "open" }));
    await assertFails(getDoc(doc(anon(), "reports/r1")));
    await assertFails(getDoc(doc(alice(), "reports/r1")));
    await assertSucceeds(getDoc(doc(boss(), "reports/r1")));
  });
});