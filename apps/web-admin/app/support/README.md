# Merchant support consent

This owner-facing page uses the existing merchant OIDC session and `/api/v1/me`
scope, then calls the typed Java support-grant endpoints. The current scoped
owner can consent to one named operator for at most 24 hours, and revoke a
pending or active grant with its version. Support still needs independent
platform approval. No impersonation token, tenant override, record browser or
business posting capability is created.

English/French copy follows the existing `quincai_locale` preference. The page
uses the merchant shell without editing shared navigation or credentials.
The IdP must grant `support:read` / `support:write` to the intended owner roles;
Java also verifies current scoped ownership. Mutations include stable operation
identity, correlation and explicit origin checking. Failed responses do not
become success; revalidation occurs only after API success.

Architecture §§11, 17, 20 and M35/M40 support consent boundaries apply. Tests
cover bounded expiry, named operator payload, revocation version checks and
French copy. Authenticated owner/operator/reviewer acceptance and genuine
screenshots are coordinated by the root; compilation is not visual acceptance.
