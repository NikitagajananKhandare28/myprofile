# Developer handover

## What is built
See REQUIREMENTS_MATRIX.md.

## Known limitations
- Photo quota is enforced by the app, not the server.
- Rate limiting is in memory per instance.
- The public page updates when the owner returns from the builder or taps Update; there is no background job.
- Admin dashboard runs locally and has no multi-factor sign-in.
- Deactivated users keep a valid token for up to about an hour.
- Behind Firebase Hosting many visitors may share one IP for rate-limit purposes.

## Open decisions
- Final product name and application ID (cannot change after the first Play upload).
- Minimum user age and guardian-consent flow for India (DPDP Act): needs legal advice.
- Real SMS provider and cost plan.
- Company ownership transfer: GitHub, Firebase and Google Cloud, Play Console, keystore, domain.

## Access checklist
[ ] GitHub  [ ] Firebase and Google Cloud  [ ] Play Console  [ ] Domain  [ ] Release keystore  [ ] Admin account
