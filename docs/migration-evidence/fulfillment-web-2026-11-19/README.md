# Fulfillment web checkpoint — 19 November 2026

Continuation branch: codex/platform-continuation-2026-11-19; backend base: 52a8461.

Dedicated fulfillment queue, creation and detail pages now call the Java APIs. Navigation and posted-sale preparation link are included. Source contract generation now normalizes Windows paths and refuses an empty Java inventory; generated contracts and TypeScript SDK are refreshed.

Executed before checkpoint: web typecheck, 28 consumer tests, production Webpack build; 13 contract/parser tests, contract validation and SDK typecheck. Project rules and whitespace check passed when preparing this checkpoint. PostgreSQL signed queue and HTTP permission evidence is recorded in the preceding backend commits; these web changes do not constitute full workflow acceptance.

Actual Chrome browser evidence covers owner/auditor queue tabs, empty search, role-dependent create action, rejection of an unposted sale and auditor write denial. Screenshots show the actual local Java-backed empty queue. The preview has no stocked posted sale; creation, partial dispatch and completion through the browser remain NOT_RUN. Detail printing, narrow-width fidelity and complete original-screen comparison remain incomplete. No historical dataset was imported. Mobile and native desktop remain paused.

Local runtimes, database files, dependencies, credentials and logs under .local-tools are deliberately excluded from Git. The separately preserved unfinished checkpoint remains on codex/preserved-local-checkpoint-2026-11-19 at 25d2db885512cf4446732883356512d992128a22.
