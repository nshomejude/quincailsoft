import 'server-only';
import { randomBytes } from 'node:crypto';
import { cookies } from 'next/headers';
import { createClient } from 'redis';
import * as oidc from 'openid-client';

const cookieName='quincai_web';
const globalRedis=globalThis as typeof globalThis & {quincaiRedis?:ReturnType<typeof createClient>};
async function redis(){
 const client=globalRedis.quincaiRedis??=createClient({url:process.env.REDIS_URL??'redis://localhost:6379'});
 if(!client.isOpen)await client.connect();
 return client;
}
export function appUrl(){return new URL(process.env.APP_URL??'http://localhost:3100');}
export function configured(){return !!process.env.OIDC_ISSUER&&!!process.env.OIDC_CLIENT_ID;}
let configuration:Promise<oidc.Configuration>|undefined;
export async function oidcConfig(){
 if(!configured())throw new Error('OIDC_NOT_CONFIGURED');
 configuration??=oidc.discovery(new URL(process.env.OIDC_ISSUER!),process.env.OIDC_CLIENT_ID!,process.env.OIDC_CLIENT_SECRET||undefined,undefined,process.env.NODE_ENV!=='production'&&process.env.OIDC_ALLOW_HTTP==='true'?{execute:[oidc.allowInsecureRequests]}:undefined);
 return configuration;
}
const cookieOptions={httpOnly:true,secure:process.env.NODE_ENV==='production',sameSite:'lax' as const,path:'/'};
export type Session={accessToken:string;expiresAt:number;name:string;scopes:string[]};
export async function session():Promise<Session|null>{
 const id=(await cookies()).get(cookieName)?.value;
 if(!id||!/^[a-f0-9]{64}$/.test(id))return null;
 const raw=await (await redis()).get('quincai:web:session:'+id);
 if(!raw)return null;
 const result=JSON.parse(raw) as Session;
 return result.expiresAt>Date.now()?result:null;
}
export async function startLogin(){
 const config=await oidcConfig();const verifier=oidc.randomPKCECodeVerifier(),state=oidc.randomState(),nonce=oidc.randomNonce();
 const id=randomBytes(32).toString('hex');
 await (await redis()).set('quincai:web:login:'+id,JSON.stringify({verifier,state,nonce}),{EX:600});
 (await cookies()).set('quincai_login',id,{...cookieOptions,maxAge:600});
 return oidc.buildAuthorizationUrl(config,{redirect_uri:new URL('/auth/callback',appUrl()).href,...(process.env.NODE_ENV==='development'&&process.env.QUINCAI_PREVIEW_DEMO==='true'?{prompt:'login'}:{}),scope:process.env.OIDC_SCOPES??'openid profile',code_challenge:await oidc.calculatePKCECodeChallenge(verifier),code_challenge_method:'S256',state,nonce});
}
export async function completeLogin(url:URL){
 const jar=await cookies(),id=jar.get('quincai_login')?.value;
 if(!id||!/^[a-f0-9]{64}$/.test(id))throw new Error('LOGIN_EXPIRED');
 const raw=await (await redis()).getDel('quincai:web:login:'+id);jar.delete('quincai_login');
 if(!raw)throw new Error('LOGIN_EXPIRED');
 const flow=JSON.parse(raw);
 const tokens=await oidc.authorizationCodeGrant(await oidcConfig(),url,{pkceCodeVerifier:flow.verifier,expectedState:flow.state,expectedNonce:flow.nonce,idTokenExpected:true});
 const ttl=Math.min(tokens.expiresIn()??300,3600);
 const data:Session={accessToken:tokens.access_token,expiresAt:Date.now()+ttl*1000,name:String(tokens.claims()?.name??tokens.claims()?.sub??'Staff'),scopes:(tokens.scope??'').split(' ')};
 const sid=randomBytes(32).toString('hex');
 await (await redis()).set('quincai:web:session:'+sid,JSON.stringify(data),{EX:ttl});jar.set(cookieName,sid,{...cookieOptions,maxAge:ttl});
}
export async function logout(){const jar=await cookies(),id=jar.get(cookieName)?.value;if(id)await(await redis()).del('quincai:web:session:'+id);jar.delete(cookieName);}
