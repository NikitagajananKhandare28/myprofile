import { auth } from "./firebase.js";

const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080";

export async function apiPost(path) {
  const token = await auth.currentUser.getIdToken();
  const res = await fetch(API_BASE + path, {
    method: "POST",
    headers: { Authorization: "Bearer " + token },
  });
  if (!res.ok) {
    let message = "Request failed";
    try {
      message = (await res.json()).message || message;
    } catch (e) {
      // keep the default message
    }
    throw new Error(message);
  }
  return res.json().catch(() => ({}));
}
