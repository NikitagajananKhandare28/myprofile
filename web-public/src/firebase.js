import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";

const firebaseConfig = {
  apiKey: "PASTE_YOUR_API_KEY",
  authDomain: "myprofile-app-4caf7.firebaseapp.com",
  projectId: "myprofile-app-4caf7",
  appId: "PASTE_YOUR_WEB_APP_ID",
};

const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);