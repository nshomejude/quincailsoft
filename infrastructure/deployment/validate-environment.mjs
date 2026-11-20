import { readFileSync, statSync } from 'node:fs';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import {originHost} from './ingress/render.mjs';

export function validateEnvironment(env, inspectSecret = path => {
  const info=statSync(path);
  return { mode:info.mode, isFile:info.isFile(), size:info.size, value:readFileSync(path,'utf8') };
}) {
  const failures=[];
  const required=['QUINCAI_API_IMAGE','QUINCAI_DATABASE_URL','QUINCAI_DATABASE_USER',
    'QUINCAI_OIDC_ISSUER','QUINCAI_OIDC_AUDIENCE','QUINCAI_DATABASE_PASSWORD_FILE'];
  for (const name of required) if(!env[name]?.trim()) failures.push(`${name} is required`);
  if(!/^[^\s]+@sha256:[a-f0-9]{64}$/.test(env.QUINCAI_API_IMAGE??'')) failures.push('API image must use an immutable sha256 digest');
  try {
    const db=new URL((env.QUINCAI_DATABASE_URL??'').replace(/^jdbc:/,''));
    if(!env.QUINCAI_DATABASE_URL?.startsWith('jdbc:postgresql://')||db.protocol!=='postgresql:'||!db.hostname||db.pathname.length<2) throw new Error();
    if(db.username||db.password) failures.push('Keep database credentials out of the JDBC URL');
    if(db.searchParams.get('sslmode')!=='verify-full') failures.push('Database TLS must use sslmode=verify-full');
  } catch { failures.push('A valid PostgreSQL JDBC URL is required'); }
  try {
    const issuer=new URL(env.QUINCAI_OIDC_ISSUER);
    if(issuer.protocol!=='https:'||issuer.username||issuer.password||issuer.search||issuer.hash) throw new Error();
  } catch { failures.push('OIDC issuer must be an HTTPS URL without credentials, query or fragment'); }
  if(['postgres','quincai_migrator'].includes(env.QUINCAI_DATABASE_USER)) failures.push('Use a dedicated least-privileged runtime database role');
  if(env.QUINCAI_MIGRATIONS_ENABLED && env.QUINCAI_MIGRATIONS_ENABLED!=='false') failures.push('Run migrations separately from the runtime account');
  if(env.QUINCAI_DATABASE_PASSWORD) failures.push('Use the mounted secret file instead of an environment password');
  if(env.QUINCAI_DATABASE_PASSWORD_FILE) {
    try {
      const file=inspectSecret(env.QUINCAI_DATABASE_PASSWORD_FILE);
      if(!file.isFile||file.size===0||file.value.trim().length<20) failures.push('Database password secret must be a nonempty regular file with a strong generated password');
      if((file.mode&0o077)!==0) failures.push('Secret file must not be accessible by group or other users');
    } catch { failures.push('Database password secret file cannot be read'); }
  }
  if(env.QUINCAI_DEPLOYMENT_MODE!=='api-only') {
    for(const name of ['QUINCAI_MERCHANT_IMAGE','QUINCAI_PLATFORM_IMAGE','QUINCAI_MIGRATION_IMAGE','QUINCAI_INGRESS_IMAGE'])if(!/^[^\s]+@sha256:[a-f0-9]{64}$/.test(env[name]??''))failures.push(`${name} must be an immutable sha256 image`);
    for(const name of ['OIDC_CLIENT_ID','PLATFORM_OIDC_CLIENT_ID','OIDC_SCOPES','PLATFORM_OIDC_SCOPES','QUINCAI_PLATFORM_AUDIENCE','QUINCAI_MIGRATION_USER','QUINCAI_INGRESS_CONFIG_FILE','QUINCAI_DATABASE_CA_FILE','QUINCAI_TLS_CERT_FILE'])if(!env[name]?.trim())failures.push(`${name} is required`);
    try {if(originHost(env.APP_URL)===originHost(env.PLATFORM_APP_URL))failures.push('Merchant and platform origins must differ');}catch{failures.push('Distinct HTTPS merchant/platform origins on port 443 are required');}
    try {const issuer=new URL(env.PLATFORM_OIDC_ISSUER);if(issuer.protocol!=='https:'||issuer.username||issuer.password||issuer.search||issuer.hash)throw Error();}catch{failures.push('Platform issuer must be a clean HTTPS URL');}
    if(env.PLATFORM_OIDC_ISSUER===env.QUINCAI_OIDC_ISSUER||env.QUINCAI_PLATFORM_AUDIENCE===env.QUINCAI_OIDC_AUDIENCE)failures.push('Control plane requires separate issuer and audience');
    if(env.OIDC_CLIENT_ID===env.PLATFORM_OIDC_CLIENT_ID)failures.push('Use separate merchant/platform OIDC clients');
    if(env.QUINCAI_MIGRATION_USER===env.QUINCAI_DATABASE_USER||env.QUINCAI_MIGRATION_USER==='postgres')failures.push('Use a separate non-superuser migration role');
    for(const name of ['OIDC_ALLOW_HTTP','PLATFORM_OIDC_ALLOW_HTTP','QUINCAI_PREVIEW_DEMO'])if(env[name]==='true')failures.push(`${name} is forbidden in deployment`);
    for(const name of ['OIDC_CLIENT_SECRET','PLATFORM_OIDC_CLIENT_SECRET','REDIS_URL','QUINCAI_MIGRATION_PASSWORD'])if(env[name])failures.push(`${name} must use a secret file`);
    for(const name of ['QUINCAI_MIGRATION_PASSWORD_FILE','OIDC_CLIENT_SECRET_FILE','PLATFORM_OIDC_CLIENT_SECRET_FILE','MERCHANT_REDIS_URL_FILE','PLATFORM_REDIS_URL_FILE','QUINCAI_TLS_KEY_FILE']) {
      if(!env[name]){failures.push(`${name} is required`);continue;}
      try {const file=inspectSecret(env[name]);if(!file.isFile||file.size===0||file.value.trim().length<20||(file.mode&0o077)!==0)failures.push(`${name} must be a private nonempty regular secret file`);
        if(name.includes('REDIS')){const redis=new URL(file.value.trim());if(redis.protocol!=='rediss:'||!redis.hostname||!redis.password)failures.push(`${name} must contain an authenticated TLS Redis URL`);}
      }catch{failures.push(`${name} cannot be read or is invalid`);}
    }
    if(env.MERCHANT_REDIS_URL_FILE===env.PLATFORM_REDIS_URL_FILE)failures.push('Use separate merchant/platform Redis credentials and ACLs');
    try{const db=new URL((env.QUINCAI_DATABASE_URL??'').replace(/^jdbc:/,''));if(db.searchParams.get('sslrootcert')!=='/run/secrets/database_ca.pem')failures.push('JDBC URL must reference mounted database CA at /run/secrets/database_ca.pem');}catch{}
    for(const name of ['QUINCAI_DATABASE_CA_FILE','QUINCAI_TLS_CERT_FILE','QUINCAI_INGRESS_CONFIG_FILE'])if(env[name])try{const file=inspectSecret(env[name]);if(!file.isFile||file.size===0)failures.push(`${name} must be a nonempty regular file`);}catch{failures.push(`${name} cannot be read`);}
  }
  return failures;
}
if(process.argv[1] && import.meta.url===pathToFileURL(resolve(process.argv[1])).href) {
  const failures=validateEnvironment(process.env);
  if(failures.length) { for(const failure of failures) console.error(failure); process.exitCode=1; }
  else console.log('Deployment environment checks PASS. This is not application release acceptance.');
}
