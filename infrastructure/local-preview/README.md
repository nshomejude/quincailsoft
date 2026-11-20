# Windows local web preview

Requires Docker Desktop in Linux container mode. From the repository root, run:

```powershell
.\infrastructure\local-preview\Preview.ps1 Start
.\infrastructure\local-preview\Preview.ps1 Logins
```

After startup succeeds, visit http://localhost:3100. Choose Continue as owner, cashier, or auditor on the Keycloak sign-in page. Each button submits that demo account through Keycloak; the role and branch claims still apply. The loopback-only login theme intentionally exposes the generated demo account passwords to the local browser. Database passwords and the OIDC client secret stay server-side. The preview uses a Keycloak realm, Redis web sessions, the Spring Boot API and a separate PostgreSQL 18 database with synthetic data. Browser, identity and API ports bind to loopback. PostgreSQL and Redis are reachable only inside the Compose network.

Next.js watches web source for hot reload. After Java edits, run `Preview.ps1 RestartApi` to build the new executable and restart Spring Boot. Wait for other Maven runs in this checkout to finish first. `Preview.ps1 Status` shows the running services. `Preview.ps1 Stop` shuts them down and retains the preview database. When invoking the script from the repository root, use the full relative path shown above.

Use this preview for the Java-backed workflows currently available. The seed contains catalog, customer, supplier and warehouse records. Economic balances require legitimate API workflows. Scope includes the merchant owner, cashier, and read-only auditor. Platform administration, live payment-provider outcomes, full report and document coverage, customer portal ownership, and the remaining operations endpoints have their own acceptance work. See the work allocation and parity register for current evidence.
