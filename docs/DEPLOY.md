# Deploying QuincaiSoft to quincaisoft.com

This guide is written for someone who is not a developer. Follow the steps in order. Commands go in a terminal on your server (connected with SSH). Copy them exactly.

## What you are setting up

One Linux server runs everything in Docker containers:

| Address | What it is |
|---|---|
| quincaisoft.com and www.quincaisoft.com | Public marketing website (www sends visitors to quincaisoft.com) |
| app.quincaisoft.com | The business application (sign-in required) |
| platform.quincaisoft.com | Operator console for QuincaiSoft staff |
| shop.quincaisoft.com | Merchant online shops |
| auth.quincaisoft.com | Sign-in service (Keycloak) |

HTTPS certificates are obtained and renewed automatically (Caddy). The database, Redis and the API are never opened to the internet.

## 1. What you need before you start

- A Linux server (Ubuntu 24.04 LTS recommended) with at least 4 GB RAM, 2 CPU, 40 GB disk, and a fixed public IPv4 address.
- Access to the DNS settings for quincaisoft.com (at your domain registrar).
- An email address for certificate notices.
- Your own public IP address (search "what is my IP"). It is used to restrict the sign-in admin console.

## 2. DNS records

At your domain registrar, create these records, all pointing to the server's public IP (replace `203.0.113.10`):

| Type | Name | Value |
|---|---|---|
| A | `@` (quincaisoft.com) | 203.0.113.10 |
| A | `www` | 203.0.113.10 |
| A | `app` | 203.0.113.10 |
| A | `platform` | 203.0.113.10 |
| A | `shop` | 203.0.113.10 |
| A | `auth` | 203.0.113.10 |

If the server also has an IPv6 address, add matching AAAA records. Wait until `ping app.quincaisoft.com` shows your server's IP (usually minutes, sometimes hours). Certificates cannot be issued before DNS is correct.

## 3. Prepare the server (once)

1. Connect: `ssh root@203.0.113.10` (or your admin user).
2. Install Docker and Git:
   ```
   curl -fsSL https://get.docker.com | sh
   apt-get install -y git
   ```
3. Open only the needed ports in the firewall:
   ```
   ufw allow OpenSSH
   ufw allow 80/tcp
   ufw allow 443
   ufw enable
   ```
   (`443` covers both TCP and UDP for HTTP/3.) Do not open 5432, 6379, 8080 or 8180.
4. Download the code:
   ```
   git clone https://github.com/nshomejude/quincailsoft.git /opt/quincaisoft
   cd /opt/quincaisoft/infrastructure/prod
   ```

## 4. Create the secrets file

```
cp .env.prod.example .env.prod
chmod 600 .env.prod
nano .env.prod
```

Fill in every empty value:

- `ACME_EMAIL`: your email address.
- `ADMIN_IPS`: your public IP address (several can be separated by spaces).
- Each `...PASSWORD` and `...SECRET`: generate a fresh one for each line by running `openssl rand -hex 32` and pasting the result. Use different values for each line.
- Keep a copy of `.env.prod` in a password manager. If it is lost the database cannot be unlocked. **Never email it or commit it to Git.**

Important: the database passwords in `.env.prod` are used only when the database is first created. Changing them later does not change the existing database.

## 5. Start everything

```
cd /opt/quincaisoft/infrastructure/prod
docker compose --env-file .env.prod up -d --build
```

The first run builds all the programs and can take 10 to 20 minutes. Check progress:

```
docker compose --env-file .env.prod ps
docker compose --env-file .env.prod logs -f --tail=50 api
```

Everything should show `running` / `healthy`. The API may restart once or twice while the sign-in service starts; that is normal.

Note: the `marketing` service is built from `apps/marketing`. If that folder is not in the code yet, the build will stop with an error. In that case, wait for the marketing site to be added, or ask a developer to temporarily remove the `marketing` service.

## 6. Keycloak first-run (sign-in service)

The first start automatically creates a realm called `quincaisoft` with two applications (`quincaisoft-web` and `platform`) and the permission scopes they need. No users exist yet.

1. From the computer whose IP you entered in `ADMIN_IPS`, open `https://auth.quincaisoft.com/admin/`.
2. Sign in with `KEYCLOAK_ADMIN_USER` and `KEYCLOAK_ADMIN_PASSWORD` from `.env.prod`.
3. In the left menu, switch the realm (top-left drop-down) from `master` to `quincaisoft`.
4. Create the first user: Users > Add user. Fill in username, email, first and last name. Click Create.
5. Credentials tab > Set password (turn "Temporary" off if you want to keep it).
6. Attributes tab: add these attributes. The API refuses to let a user in without them.
   - `tenant_id`, `company_id`, `branch_id`: the identifiers of that user's business (each must be a UUID created by QuincaiSoft's onboarding; ask your developer for the values).
   - `authorization_epoch`: `0`
   - `permissions`: one entry per permission the user needs, for example `catalog:read`, `sales:read`, `sales:write`, `sales:post`, `inventory:read`, `reports:read`, `purchasing:write`. Use the "add value" option to enter several.
7. QuincaiSoft staff who use platform.quincaisoft.com sign in through the separate `platform` application (the realm already defines its scopes `platform:tenants:read`, `platform:tenants:write` and `platform:tenants:approve`). Their user needs the same attributes as in step 6; ask a developer which permission values the platform API expects.
8. Open `https://app.quincaisoft.com` and click "Sign in securely". If something goes wrong, a red message now appears on the page ("Sign-in did not complete" or "Sign-in is temporarily unavailable"), and the reason is written to the logs: `docker compose --env-file .env.prod logs web-admin`.
9. Strongly recommended: in the `master` realm, create a personal admin account with two-factor authentication and delete or disable the bootstrap admin.

## 7. Check that it works

- `https://quincaisoft.com`, `https://app.quincaisoft.com`, `https://shop.quincaisoft.com` open with a padlock.
- `https://www.quincaisoft.com` jumps to `https://quincaisoft.com`.
- `https://auth.quincaisoft.com/admin/` shows "Forbidden" from any computer that is not in `ADMIN_IPS`.
- Test https://www.ssllabs.com/ssltest/ (optional).

## 8. Backups (PostgreSQL)

Run a backup now and then every day. Backups include the business database and the sign-in database.

One-off backup:

```
mkdir -p /opt/backups
cd /opt/quincaisoft/infrastructure/prod
docker compose --env-file .env.prod exec -T postgres pg_dump -U postgres -Fc quincaisoft > /opt/backups/quincaisoft-$(date +%F).dump
docker compose --env-file .env.prod exec -T postgres pg_dump -U postgres -Fc keycloak > /opt/backups/keycloak-$(date +%F).dump
```

Daily automatic backup at 02:15, keeping 14 days: run `crontab -e` and add (one line):

```
15 2 * * * cd /opt/quincaisoft/infrastructure/prod && for d in quincaisoft keycloak; do docker compose --env-file .env.prod exec -T postgres pg_dump -U postgres -Fc $d > /opt/backups/$d-$(date +\%F).dump; done; find /opt/backups -name '*.dump' -mtime +14 -delete
```

Copy `/opt/backups` off the server regularly (another server, or object storage). A backup that lives only on the same disk is not a backup. Also make server-level snapshots with your hosting provider.

Restore (this replaces current data; stop the app first):

```
docker compose --env-file .env.prod stop api web-admin platform-admin storefront keycloak
docker compose --env-file .env.prod exec -T postgres dropdb -U postgres quincaisoft
docker compose --env-file .env.prod exec -T postgres createdb -U postgres -O quincai_migrator quincaisoft
docker compose --env-file .env.prod exec -T postgres pg_restore -U postgres -d quincaisoft --no-owner --role=quincai_migrator < /opt/backups/quincaisoft-YYYY-MM-DD.dump
docker compose --env-file .env.prod start keycloak api web-admin platform-admin storefront
```

Test a restore on a spare server at least once before you depend on it.

## 9. Updating to a new version

```
cd /opt/quincaisoft
git fetch --all --tags
git pull
cd infrastructure/prod
docker compose --env-file .env.prod up -d --build
```

Take a database backup (section 8) first, because database upgrades applied by a new version cannot always be reversed. Alternatively use the manual **Deploy (manual)** workflow in GitHub Actions (Actions tab > Deploy > Run workflow). It needs the secrets listed at the top of `.github/workflows/deploy.yml`.

## 10. Rolling back

If a new version misbehaves:

1. Find the last good version: `git log --oneline -10` (or `cat infrastructure/prod/.previous-deployed-ref` if the deploy workflow wrote it).
2. Go back to it and rebuild:
   ```
   cd /opt/quincaisoft
   git checkout <good-commit-id>
   cd infrastructure/prod
   docker compose --env-file .env.prod up -d --build
   ```
3. If the new version also changed the database structure, restore the backup from before the update (section 8) after step 2. Without a backup, ask a developer before rolling back across a database change.
4. When fixed, return to the latest code with `git checkout main && git pull`.

## 11. Everyday commands

| Task | Command (run in `/opt/quincaisoft/infrastructure/prod`) |
|---|---|
| Status | `docker compose --env-file .env.prod ps` |
| Logs of one service | `docker compose --env-file .env.prod logs --tail=100 api` |
| Restart one service | `docker compose --env-file .env.prod restart web-admin` |
| Stop everything | `docker compose --env-file .env.prod down` (data is kept; never add `-v`) |

Never run `docker compose down -v`: it deletes the database.

## 12. Known limits and things to decide

- Sign-in users and their tenant/branch/permission attributes are created by hand in Keycloak until an onboarding flow is built.
- The sign-in session lasts one hour (no refresh tokens yet); users are asked to sign in again after that.
- No email server is configured in Keycloak, so "forgot password" emails do not work until you add SMTP settings in the realm (Realm settings > Email).
- Orange Money is unavailable and MTN has only been tested with a simulator; keep payment features off until live credentials are approved.
