import { readFileSync } from "node:fs";
import admin from "firebase-admin";

const [email, password] = process.argv.slice(2);
if (!email) {
  console.error("Usage: node set-admin.mjs <email> [password]");
  process.exit(1);
}

const key = JSON.parse(readFileSync(new URL("./serviceAccountKey.json", import.meta.url)));
admin.initializeApp({ credential: admin.credential.cert(key) });

let user;
try {
  user = await admin.auth().getUserByEmail(email);
} catch (error) {
  if (error.code !== "auth/user-not-found") throw error;
  if (!password || password.length < 12) {
    console.error("User does not exist. Pass a password of at least 12 characters.");
    process.exit(1);
  }
  user = await admin.auth().createUser({ email, password, emailVerified: true });
}

await admin.auth().setCustomUserClaims(user.uid, { admin: true });
console.log("Admin access granted to", email);
process.exit(0);