# MyProfile (working title)

One person → One profile → One shareable identity.

A platform where anyone can create, publish and share a digital profile
via a unique URL or QR code, without building a website.

## Repository layout
| Folder | Purpose |
|---|---|
| `android/` | Android app (Java, XML, ViewBinding) |
| `backend/` | Spring Boot REST API |
| `web-public/` | React public profile website |
| `web-admin/` | React admin dashboard |
| `firebase/` | Firestore/Storage rules, indexes, hosting config |
| `docs/` | Architecture, API, testing, deployment, handover |

## Status
- [x] Phase 1: Repository scaffold
- [ ] Phase 2: Android Studio and Gradle

## Security rules for contributors
- Never commit `google-services.json`, service-account JSON, keystores or `.env` files.
- All privileged operations go through the backend, never directly from clients.

## Branching
`main` is always releasable. Work happens in `feature/<phase>-<name>` branches.