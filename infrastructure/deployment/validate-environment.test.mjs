import test from 'node:test';
import assert from 'node:assert/strict';
import { validateEnvironment } from './validate-environment.mjs';
const valid={QUINCAI_DEPLOYMENT_MODE:'api-only',QUINCAI_API_IMAGE:'example.invalid/quincai@sha256:'+'a'.repeat(64),QUINCAI_DATABASE_URL:'jdbc:postgresql://db.example.invalid:5432/quincai?sslmode=verify-full',QUINCAI_DATABASE_USER:'runtime',QUINCAI_OIDC_ISSUER:'https://identity.example.invalid/realms/merchant',QUINCAI_OIDC_AUDIENCE:'quincai-api',QUINCAI_DATABASE_PASSWORD_FILE:'/private/runtime-secret'};
const secret=()=>({mode:0o100600,isFile:true,size:32,value:'synthetic-test-password-1234567890'});
test('accepts deployment contract without exposing secret',()=>assert.deepEqual(validateEnvironment(valid,secret),[]));
for(const [name,value] of [['QUINCAI_API_IMAGE','quincai:latest'],['QUINCAI_DATABASE_URL','jdbc:postgresql://db/q?sslmode=disable'],['QUINCAI_DATABASE_URL','jdbc:postgresql://user:secret@db/q?sslmode=verify-full'],['QUINCAI_DATABASE_USER','postgres'],['QUINCAI_OIDC_ISSUER','http://idp/realm'],['QUINCAI_OIDC_ISSUER','https://idp/realm?secret=x'],['QUINCAI_MIGRATIONS_ENABLED','true'],['QUINCAI_DATABASE_PASSWORD','not-allowed']]) {
  test(`rejects unsafe ${name}: ${value.split(':')[0]}`,()=>assert.ok(validateEnvironment({...valid,[name]:value},secret).length));
}
test('rejects missing fields',()=>assert.ok(validateEnvironment({},secret).length>=6));
test('rejects group-readable credentials',()=>assert.ok(validateEnvironment(valid,()=>({...secret(),mode:0o100640})).length));
test('rejects unavailable secret',()=>assert.ok(validateEnvironment(valid,()=>{throw new Error('not found');}).length));
