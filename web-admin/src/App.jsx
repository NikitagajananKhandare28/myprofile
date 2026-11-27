import { useCallback, useEffect, useState } from "react";
import { onAuthStateChanged, signInWithEmailAndPassword, signOut } from "firebase/auth";
import {
  collection, doc, getCountFromServer, getDoc, getDocs, limit,
  orderBy, query, serverTimestamp, where, writeBatch,
} from "firebase/firestore";
import { auth, db } from "./firebase.js";

function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  async function submit(event) {
    event.preventDefault();
    setError("");
    try {
      await signInWithEmailAndPassword(auth, email, password);
    } catch (e) {
      setError("Sign-in failed.");
    }
  }

  return (
    <form className="login" onSubmit={submit}>
      <h1>MyProfile Admin</h1>
      <input type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
      <input type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} required />
      <button type="submit">Sign in</button>
      {error ? <div className="error">{error}</div> : null}
    </form>
  );
}

async function count(q) {
  const snap = await getCountFromServer(q);
  return snap.data().count;
}

function when(value) {
  return value && value.toDate ? value.toDate().toLocaleString() : "";
}

function Dashboard() {
  const [stats, setStats] = useState(null);
  const [reports, setReports] = useState([]);
  const [logs, setLogs] = useState([]);
  const [name, setName] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  const refresh = useCallback(async () => {
    try {
      const profiles = collection(db, "profiles");
      const [total, published, suspended, open] = await Promise.all([
        count(profiles),
        count(query(profiles, where("status", "==", "published"))),
        count(query(profiles, where("status", "==", "suspended"))),
        count(query(collection(db, "reports"), where("status", "==", "open"))),
      ]);
      setStats({ total, published, suspended, open });

      const r = await getDocs(query(collection(db, "reports"), orderBy("createdAt", "desc"), limit(50)));
      setReports(r.docs.map((d) => ({ id: d.id, ...d.data() })));

      const l = await getDocs(query(collection(db, "auditLogs"), orderBy("createdAt", "desc"), limit(20)));
      setLogs(l.docs.map((d) => ({ id: d.id, ...d.data() })));
    } catch (e) {
      setMessage("Could not load data: " + e.message);
    }
  }, []);

  useEffect(() => { refresh(); }, [refresh]);

  async function run(action) {
    setBusy(true);
    setMessage("");
    try {
      await action();
      setMessage("Done.");
      await refresh();
    } catch (e) {
      setMessage("Failed: " + e.message);
    }
    setBusy(false);
  }

  function takeDown(report) {
    return run(async () => {
      const publicRef = doc(db, "publicProfiles", report.username);
      const snap = await getDoc(publicRef);
      const batch = writeBatch(db);
      if (snap.exists()) {
        batch.delete(publicRef);
        batch.update(doc(db, "profiles", snap.data().ownerUid), {
          status: "suspended",
          updatedAt: serverTimestamp(),
        });
      }
      batch.update(doc(db, "reports", report.id), {
        status: "actioned",
        action: "taken_down",
        reviewedBy: auth.currentUser.uid,
        reviewedAt: serverTimestamp(),
      });
      batch.set(doc(collection(db, "auditLogs")), {
        actorUid: auth.currentUser.uid,
        action: "takedown",
        target: report.username,
        reportId: report.id,
        createdAt: serverTimestamp(),
      });
      await batch.commit();
    });
  }

  function dismiss(report) {
    return run(async () => {
      const batch = writeBatch(db);
      batch.update(doc(db, "reports", report.id), {
        status: "dismissed",
        action: "none",
        reviewedBy: auth.currentUser.uid,
        reviewedAt: serverTimestamp(),
      });
      batch.set(doc(collection(db, "auditLogs")), {
        actorUid: auth.currentUser.uid,
        action: "dismiss",
        target: report.username,
        reportId: report.id,
        createdAt: serverTimestamp(),
      });
      await batch.commit();
    });
  }

  function reinstate() {
    return run(async () => {
      const clean = name.trim().toLowerCase();
      const claim = await getDoc(doc(db, "usernames", clean));
      if (!claim.exists()) throw new Error("No profile with that name");
      const batch = writeBatch(db);
      batch.update(doc(db, "profiles", claim.data().uid), {
        status: "unpublished",
        updatedAt: serverTimestamp(),
      });
      batch.set(doc(collection(db, "auditLogs")), {
        actorUid: auth.currentUser.uid,
        action: "reinstate",
        target: clean,
        reportId: "",
        createdAt: serverTimestamp(),
      });
      await batch.commit();
      setName("");
    });
  }

  return (
    <div className="wrap">
      <div className="top">
        <h1>MyProfile Admin</h1>
        <button className="secondary" onClick={() => signOut(auth)}>Sign out</button>
      </div>

      {stats ? (
        <div className="stats">
          <div className="stat"><b>{stats.total}</b>Profiles</div>
          <div className="stat"><b>{stats.published}</b>Published</div>
          <div className="stat"><b>{stats.suspended}</b>Suspended</div>
          <div className="stat"><b>{stats.open}</b>Open reports</div>
        </div>
      ) : null}

      {message ? <p className={message.startsWith("Done") ? "ok" : "error"}>{message}</p> : null}

      <h2>Reports</h2>
      {reports.length === 0 ? <p className="muted">No reports.</p> : null}
      {reports.map((r) => (
        <div className="card" key={r.id}>
          <div className="row">
            <div>
              <strong>{r.username}</strong> <span className="tag">{r.status}</span>
              <div>{r.reason}</div>
              {r.message ? <div className="muted">{r.message}</div> : null}
              <div className="muted">{when(r.createdAt)}</div>
            </div>
            {r.status === "open" ? (
              <div className="row">
                <a href={"https://myprofile-app-4caf7.web.app/" + r.username} target="_blank" rel="noopener noreferrer">View page</a>
                <button className="danger" disabled={busy} onClick={() => takeDown(r)}>Take down and suspend</button>
                <button className="secondary" disabled={busy} onClick={() => dismiss(r)}>Dismiss</button>
              </div>
            ) : null}
          </div>
        </div>
      ))}

      <h2>Reinstate a suspended profile</h2>
      <div className="row" style={{ justifyContent: "flex-start" }}>
        <input placeholder="profile-name" value={name} onChange={(e) => setName(e.target.value)} />
        <button disabled={busy || !name.trim()} onClick={reinstate}>Reinstate</button>
      </div>
      <p className="muted">The owner can then publish again from the app.</p>

      <h2>Audit log</h2>
      {logs.length === 0 ? <p className="muted">Nothing yet.</p> : null}
      {logs.map((l) => (
        <div className="card" key={l.id}>
          <strong>{l.action}</strong> on {l.target} <span className="muted">by {l.actorUid} at {when(l.createdAt)}</span>
        </div>
      ))}
    </div>
  );
}

export default function App() {
  const [state, setState] = useState({ checking: true, user: null, admin: false });

  useEffect(() => onAuthStateChanged(auth, async (user) => {
    if (!user) {
      setState({ checking: false, user: null, admin: false });
      return;
    }
    const token = await user.getIdTokenResult(true);
    setState({ checking: false, user, admin: token.claims.admin === true });
  }), []);

  if (state.checking) return <p className="muted" style={{ textAlign: "center", marginTop: "20vh" }}>Loading...</p>;
  if (!state.user) return <Login />;
  if (!state.admin) {
    return (
      <div className="login">
        <h1>Not authorised</h1>
        <p className="muted">This account does not have admin access.</p>
        <button onClick={() => signOut(auth)}>Sign out</button>
      </div>
    );
  }
  return <Dashboard />;
}