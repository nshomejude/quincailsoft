# Agent runbook: deploy QuincaiSoft to quincaisoft.com

Audience: an AI deploying agent (Claude Code or similar) with shell access to the production server. Humans: see `docs/DEPLOY.md` for the same stack in plain language.

## 0. Ground rules
- **Honest status.** This stack has NEVER been built or run end to end. The Docker images, Caddyfile, Keycloak realm import, Java backend image and CI were all written without a Docker daemon or Java 25. Expect first-run failures; diagnose from logs, fix in the repo on a branch, and report. Never claim success without the checks in section 6.
- **Stop and ask the human** before: changing DNS, deleting any volume or database, opening firewall ports beyond 22/80/443, running anything with `down -v`, touching an existing service already on the server, or putting any secret in git/chat.
- **Secrets:** generate with `openssl rand -hex 32`, write only to `infrastructure/prod/.env.prod` (chmod 600). Never print them back, never commit them. Hand them to the human via their password manager channel, not in the transcript.
- Do not deploy from a feature branch. Deploy `main`.

## 1. Inputs to get from the human first
| Need | Why |
|---|---|
| SSH access (host, user, key) to the prepared server | Deploy target |
| Confirmation DNS A records exist for `@`, `www`, `app`, `platform`, `shop`, `auth` -> server IP | Certificates cannot issue without them |
| Email for Let's Encrypt (`ACME_EMAIL`) | Cert notices |
| Human's public IP(s) (`ADMIN_IPS`) | Restricts Keycloak admin console |
| Whether anything else already runs on ports 80/443 | Caddy needs them |
| The real contact email for the marketing site (currently placeholder `contact@quincaisoft.com`) | Content accuracy |

## 2. Server preflight (read-only checks)
```bash
uname -a; cat /etc/os-release | head -2
nproc; free -h; df -h /            # want >=2 CPU, >=4 GB RAM, >=40 GB free
docker --version; docker compose version   # install via get.docker.com if missing
ss -ltnp | grep -E ':(80|443)\s'   # must be empty
dig +short A quincaisoft.com app.quincaisoft.com platform.quincaisoft.com shop.quincaisoft.com auth.quincaisoft.com www.quincaisoft.com   # all = server public IP
curl -s ifconfig.me                # the server IP to compare
```
Fix: install Docker; firewall `ufw allow OpenSSH; ufw allow 80/tcp; ufw allow 443; ufw enable`. Do NOT open 5432, 6379, 8080, 8180.
If DNS does not match, stop and tell the human; do not proceed.

## 3. Get the code
```bash
git clone https://github.com/nshomejude/quincailsoft.git /opt/quincaisoft   # needs read access; private repo -> human supplies deploy key/token
cd /opt/quincaisoft && git checkout main && git log --oneline -1
cd infrastructure/prod
```

## 4. Configure
```bash
cp .env.prod.example .env.prod && chmod 600 .env.prod
```
Fill every empty value. For each `*_PASSWORD` / `*_SECRET`: `openssl rand -hex 32` (hex only; values go inside URLs). Set `DOMAIN`, `ACME_EMAIL`, `ADMIN_IPS`, leave `TAG=latest`. Validate:
```bash
docker compose --env-file .env.prod config -q && echo OK
grep -c '=$' .env.prod    # must print 0 (no empty values)
```

## 5. Build and start
```bash
docker compose --env-file .env.prod build 2>&1 | tail -50
docker compose --env-file .env.prod up -d
docker compose --env-file .env.prod ps
```
Build takes 10-20 min. Known risk points, in order of likelihood:
1. **`backend/Dockerfile`** (Java 25 + Maven wrapper): never executed. If the Temurin 25 tag or `./mvnw -pl api-server -am` fails, read the Maven error, fix the Dockerfile/pom invocation, rebuild. The repo's own evidence says `./mvnw verify` passed on Java 25, so the code compiles; the Dockerfile wrapper is the unproven part.
2. **Image tags** (`keycloak:26.4.0`, `redis:8-alpine`, `caddy:2.10-alpine`, `postgres:18.7-bookworm`) were chosen from memory. If a pull fails with "manifest unknown", pick the nearest existing tag from the registry, edit `compose.yaml`, note it in your report.
3. **Keycloak realm import:** it substitutes `${QUINCAI_WEB_CLIENT_SECRET}` etc. from the container environment. Check `docker compose logs keycloak | grep -i -E 'import|error|realm'`. If substitution did not happen, the clients will have the literal string as secret; fix by setting the secret in the Keycloak admin console or by templating the JSON before mount.
4. **API start order:** API may restart 1-2 times while Keycloak (issuer) and Postgres become ready. Acceptable if it settles to healthy within ~3 min. Flyway runs as `quincai_migrator`, runtime as `quincai_runtime`; failures here are migration/role errors, read `logs api`.
5. **Caddyfile:** validate with `docker compose exec caddy caddy validate --config /etc/caddy/Caddyfile` if the path differs, check `compose.yaml`.

Wait until: `docker compose --env-file .env.prod ps` shows every service `running`/`healthy`.

## 6. Verification checklist (all must pass; report each result)
```bash
D=quincaisoft.com
curl -sI https://$D | head -1                       # 200
curl -sI https://www.$D | grep -i '^location'       # -> https://quincaisoft.com
curl -s https://$D/robots.txt | head -5
curl -s https://$D/sitemap.xml | head -5
curl -s https://$D/llms.txt | head -3
curl -sI https://app.$D | head -1                   # 200 (login page)
curl -sI https://platform.$D | head -1              # 200/307
curl -sI https://shop.$D | head -1                  # 200
curl -s https://auth.$D/realms/quincaisoft/.well-known/openid-configuration | head -c 200   # JSON issuer
curl -sI https://auth.$D/admin/ | head -1           # 403 from a non-allowed IP
curl -sI https://$D | grep -i strict-transport      # HSTS present
curl -s -o /dev/null -w '%{http_code}\n' https://$D/api/v1/me   # must NOT reach the API: expect 404 on marketing
curl -s -o /dev/null -w '%{http_code}\n' http://$D:8080 --max-time 5   # must fail (port closed)
nc -zv -w3 $(curl -s ifconfig.me) 5432 6379 8080 8180   # all must be closed (run from OUTSIDE the server if possible)
```
Also: `docker compose --env-file .env.prod logs --tail=30 api web-admin storefront | grep -i -E 'error|exception'` should be quiet.

## 7. First user (needs the human)
Keycloak starts with NO users. Real sign-in requires a user with attributes `tenant_id`, `company_id`, `branch_id` (UUIDs from platform onboarding), `authorization_epoch=0`, and multi-valued `permissions`. The platform API's exact permission strings and how a tenant gets created (`/admin/onboarding`) are not verified. Do NOT invent tenant UUIDs. Create the Keycloak admin-console steps list for the human (see DEPLOY.md section 6) and ask them for values. Then test `https://app.quincaisoft.com` sign-in and report the result.

## 8. Backups (set up before declaring done)
Install the cron from DEPLOY.md section 8 (`pg_dump` of `quincaisoft` and `keycloak`, 14-day retention). Run one backup now and verify the file size is non-zero. Tell the human off-server copies are still their responsibility.

## 9. Rollback
Code rollback: `cd /opt/quincaisoft && git checkout <previous-good-sha> && cd infrastructure/prod && docker compose --env-file .env.prod up -d --build`. Data rollback: restore the latest dump per DEPLOY.md (needs human OK, it overwrites data). A half-failed first deploy: `docker compose --env-file .env.prod down` (no `-v`) is safe; volumes stay.

## 10. Final report must contain
Each checklist line in section 6 with pass/fail; image tags you changed; any repo fix you made (branch + commit, never force-push main); what remains blocked on the human (first user, contact email, DNS, off-server backups); and the explicit statement of what was NOT tested (Orange Money, live MTN, offline mobile posting, real merchant data).

## Known product limits to repeat to the human, not hide
- Sessions end after 1 hour (no refresh token). No SMTP, so no password-reset mail.
- Reports: 5 of 76 exist. Document templates: 4 of 26. Customers are read-only (no create endpoint). No sale void.
- UI body text on most admin pages is English only.
- Privacy and terms pages are drafts needing legal review.
