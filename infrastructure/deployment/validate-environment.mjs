import { readFileSync, statSync } from 'node:fs';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

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
  return failures;
}
if(process.argv[1] && import.meta.url===pathToFileURL(resolve(process.argv[1])).href) {
  const failures=validateEnvironment(process.env);
  if(failures.length) { for(const failure of failures) console.error(failure); process.exitCode=1; }
  else console.log('Deployment environment checks PASS. This is not application release acceptance.');
}
