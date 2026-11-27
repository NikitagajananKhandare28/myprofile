import { useEffect, useState } from "react";
import { addDoc, collection, doc, getDoc, serverTimestamp } from "firebase/firestore";
import { db } from "./firebase.js";

const REASONS = [
  "Fake or impersonation",
  "Inappropriate content",
  "Spam",
  "Personal information exposed",
  "Other",
];

function currentPath() {
  return decodeURIComponent(window.location.pathname)
    .replace(/^\/+|\/+$/g, "")
    .toLowerCase();
}

function safeHttps(url) {
  return typeof url === "string" && url.startsWith("https://") ? url : null;
}

function digitsOnly(value) {
  return String(value || "").replace(/[^0-9]/g, "");
}

function Section({ title, children }) {
  return (
    <section className="card">
      <h2>{title}</h2>
      {children}
    </section>
  );
}

function Entries({ title, items, titleKey, subKey, periodKey, descKey }) {
  if (!Array.isArray(items) || items.length === 0) return null;
  return (
    <Section title={title}>
      {items.map((item, index) => (
        <div className="entry" key={index}>
          <div className="entry-title">{item[titleKey]}</div>
          {subKey && item[subKey] ? <div>{item[subKey]}</div> : null}
          {periodKey && item[periodKey] ? <div className="muted">{item[periodKey]}</div> : null}
          {descKey && item[descKey] ? <p className="desc">{item[descKey]}</p> : null}
        </div>
      ))}
    </Section>
  );
}

function ReportBox({ username }) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState(REASONS[0]);
  const [message, setMessage] = useState("");
  const [state, setState] = useState("idle");

  async function submit(event) {
    event.preventDefault();
    setState("sending");
    try {
      await addDoc(collection(db, "reports"), {
        username,
        reason,
        message: message.slice(0, 300),
        status: "open",
        createdAt: serverTimestamp(),
      });
      setState("sent");
    } catch (error) {
      setState("error");
    }
  }

  if (state === "sent") {
    return <p className="muted center">Thank you. Your report was sent.</p>;
  }

  return (
    <div className="report">
      {!open ? (
        <button className="link-button" onClick={() => setOpen(true)}>
          Report this profile
        </button>
      ) : (
        <form onSubmit={submit}>
          <select value={reason} onChange={(e) => setReason(e.target.value)}>
            {REASONS.map((r) => (
              <option key={r}>{r}</option>
            ))}
          </select>
          <textarea
            maxLength={300}
            placeholder="Tell us more (optional)"
            value={message}
            onChange={(e) => setMessage(e.target.value)}
          />
          <button type="submit" disabled={state === "sending"}>
            Send report
          </button>
          {state === "error" ? <p className="muted">Could not send. Try again later.</p> : null}
        </form>
      )}
    </div>
  );
}

function Profile({ username }) {
  const [state, setState] = useState({ loading: true, data: null, error: false });

  useEffect(() => {
    getDoc(doc(db, "publicProfiles", username))
      .then((snap) => {
        setState({ loading: false, data: snap.exists() ? snap.data() : null, error: false });
        if (snap.exists()) document.title = snap.data().displayName + " | MyProfile";
      })
      .catch(() => setState({ loading: false, data: null, error: true }));
  }, [username]);

  if (state.loading) return <p className="center muted">Loading...</p>;
  if (state.error) return <p className="center muted">Something went wrong. Please try again.</p>;
  if (!state.data) {
    return (
      <div className="page">
        <h1 className="center">Profile not found</h1>
        <p className="center muted">This profile does not exist or is not public.</p>
      </div>
    );
  }

  const p = state.data;
  const c = p.contact || {};
  const phone = String(c.phone || "").replace(/[^0-9+]/g, "");
  const wa = digitsOnly(c.whatsapp);
  const emailOk = typeof c.email === "string" && /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(c.email);
  const links = Array.isArray(p.socialLinks) ? p.socialLinks : [];

  return (
    <div className="page">
      <header className="header">
        {typeof p.photoUrl === "string" && p.photoUrl.startsWith("https://") ? (
          <img className="avatar-img" src={p.photoUrl} alt="" />
        ) : (
          <div className="avatar">{(p.displayName || "?").charAt(0).toUpperCase()}</div>
        )}
        <h1>{p.displayName}</h1>
        <div className="type">{p.profileType}</div>
        {p.city ? <div className="muted">{p.city}</div> : null}
        {p.bio ? <p className="bio">{p.bio}</p> : null}
      </header>

      {p.aboutMe ? (
        <Section title="About">
          <p className="desc">{p.aboutMe}</p>
        </Section>
      ) : null}

      {Array.isArray(p.skills) && p.skills.length > 0 ? (
        <Section title="Skills">
          <div className="chips">
            {p.skills.map((skill, index) => (
              <span className="chip" key={index}>{skill}</span>
            ))}
          </div>
        </Section>
      ) : null}

      <Entries title="Work experience" items={p.experience}
        titleKey="role" subKey="organization" periodKey="duration" descKey="description" />
      <Entries title="Education" items={p.education}
        titleKey="qualification" subKey="institution" periodKey="year" />
      <Entries title="Services" items={p.services}
        titleKey="name" subKey="price" descKey="description" />
      <Entries title="Achievements" items={p.achievements}
        titleKey="title" periodKey="date" descKey="description" />

      {Array.isArray(p.gallery) && p.gallery.some((g) => safeHttps(g.url)) ? (
        <Section title="Gallery">
          {p.gallery.filter((g) => safeHttps(g.url)).map((g, index) => (
            <figure className="photo" key={index}>
              <img src={safeHttps(g.url)} alt={g.caption || ""} loading="lazy" />
              {g.caption ? <figcaption className="muted">{g.caption}</figcaption> : null}
            </figure>
          ))}
        </Section>
      ) : null}

      {links.some((l) => safeHttps(l.url)) ? (
        <Section title="Links">
          <div className="buttons">
            {links.filter((l) => safeHttps(l.url)).map((l, index) => (
              <a className="button" key={index} href={safeHttps(l.url)}
                target="_blank" rel="noopener noreferrer">{l.platform}</a>
            ))}
          </div>
        </Section>
      ) : null}

      {phone || wa || emailOk || c.location ? (
        <Section title="Contact">
          <div className="buttons">
            {phone ? <a className="button" href={"tel:" + phone}>Call</a> : null}
            {wa ? <a className="button" href={"https://wa.me/" + wa}
              target="_blank" rel="noopener noreferrer">WhatsApp</a> : null}
            {emailOk ? <a className="button" href={"mailto:" + c.email}>Email</a> : null}
          </div>
          {c.location ? <p className="muted">{c.location}</p> : null}
        </Section>
      ) : null}

      <footer className="footer">
        <div>Made with MyProfile</div>
        <ReportBox username={username} />
      </footer>
    </div>
  );
}

function Landing() {
  return (
    <div className="page">
      <h1 className="center">MyProfile</h1>
      <p className="center muted">Your digital identity. No website required.</p>
    </div>
  );
}

function DeleteAccountPage() {
  return (
    <div className="page">
      <h1>Delete your MyProfile account</h1>
      <p>You can delete your account and all its data at any time inside the app:</p>
      <ol>
        <li>Open the MyProfile app and log in.</li>
        <li>On the home screen, tap <strong>Delete my account</strong>.</li>
        <li>Confirm. Your profile, sections, public page and profile name are removed.</li>
      </ol>
      <p>If you cannot open the app, contact the app owner at the support address listed on the store page.</p>
    </div>
  );
}

function PrivacyPage() {
  return (
    <div className="page">
      <h1>Privacy Policy</h1>
      <p className="muted">Last updated: [date]. This is the policy for the MyProfile app and website, run by [Company legal name], [address] ("we"). Contact: [support email].</p>

      <h2>What we collect</h2>
      <ul>
        <li>Your mobile number, used only to log in (verified by Firebase Authentication, a Google service).</li>
        <li>Details you enter: name, profile type, city, bio, About me, skills, education, work experience, services, achievements and social links.</li>
        <li>Contact details you choose to add (phone, WhatsApp, email, area).</li>
        <li>Photos you upload.</li>
        <li>If you are under 18: that fact, and that a parent or guardian agreed.</li>
        <li>Reports about a profile: the profile name, the reason and any message sent.</li>
        <li>Basic technical logs needed to run and secure the service.</li>
      </ul>

      <h2>What becomes public</h2>
      <p>Nothing is public until you publish your profile. When you publish, only the sections you switched on are shown to anyone with your link. Your login number is never shown. Contact details are shown only if you switch them on. For users under 18, contact details are never shown.</p>

      <h2>How we use your data</h2>
      <p>To run your account, show the profile you publish, keep the service secure, handle reports, and meet legal duties. We do not sell your data and we do not show advertising.</p>

      <h2>Who we share it with</h2>
      <p>Our service providers: Google Firebase (authentication, database, file storage and hosting). They process data on our behalf. We disclose data when the law requires it.</p>

      <h2>Keeping and deleting your data</h2>
      <p>We keep your data until you delete your account. You can delete your account in the app (Home, then Delete my account), which removes your profile, sections, photos, public page and profile name. See <a href="/delete-account">how to delete your account</a>.</p>

      <h2>Children</h2>
      <p>People under 18 may only use MyProfile with the agreement of a parent or guardian. We hide their contact details and do not offer public search or direct messages.</p>

      <h2>Your rights</h2>
      <p>You can view and change your information in the app, and delete it at any time. For other requests, contact [support email]. Grievance officer: [name and email].</p>

      <h2>Changes</h2>
      <p>If we change this policy we will update the date above and, for important changes, tell you in the app.</p>
    </div>
  );
}

function TermsPage() {
  return (
    <div className="page">
      <h1>Terms of Use</h1>
      <p className="muted">Last updated: [date]. Provided by [Company legal name].</p>
      <h2>Your account</h2>
      <p>You are responsible for your account and for what you publish. Use your real identity and details you have the right to share.</p>
      <h2>Content rules</h2>
      <p>Do not publish anything illegal, hateful, misleading, harmful to children, or that exposes other people's private information. Do not impersonate others. We may remove content or suspend accounts that break these rules.</p>
      <h2>Reports</h2>
      <p>Anyone can report a profile from its page. We review reports and may remove profiles.</p>
      <h2>Under 18</h2>
      <p>If you are under 18 you must have a parent or guardian's agreement to use MyProfile.</p>
      <h2>Ending your use</h2>
      <p>You can delete your account at any time in the app. We may suspend or end access if these terms are broken.</p>
      <h2>Liability</h2>
      <p>The service is provided as is, to the extent the law allows. [Limitation of liability and governing law to be completed with legal advice.]</p>
      <h2>Contact</h2>
      <p>[support email]</p>
    </div>
  );
}

export default function App() {
  const path = currentPath();
  if (path === "") return <Landing />;
  if (path === "delete-account") return <DeleteAccountPage />;
  if (path === "privacy") return <PrivacyPage />;
  if (path === "terms") return <TermsPage />;
  return <Profile username={path} />;
}