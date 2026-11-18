import React,{createContext,useContext,useEffect,useState} from 'react';import * as SecureStore from 'expo-secure-store';import * as AuthSession from 'expo-auth-session';import * as WebBrowser from 'expo-web-browser';import {closeDatabase} from './storage';
WebBrowser.maybeCompleteAuthSession();
export type Session={accessToken:string;expiresAt:number;scope:{tenantId:string;companyId:string;branchId:string;actorId:string};authorities:string[]};
const C=createContext<{session:Session|null;login:()=>Promise<void>;logout:()=>Promise<void>;error:string;ready:boolean}>({session:null,login:async()=>{},logout:async()=>{},error:'',ready:false});
const endpoint=process.env.EXPO_PUBLIC_API_URL??'';const issuer=process.env.EXPO_PUBLIC_OIDC_ISSUER??'';const clientId=process.env.EXPO_PUBLIC_OIDC_CLIENT_ID??'';
export function scopeKey(s:Session){return [s.scope.tenantId,s.scope.companyId,s.scope.branchId,s.scope.actorId].join(':');}
export function hasAuthority(session: Session, authority: string) {
  return session.authorities.includes(authority) || session.authorities.includes('SCOPE_' + authority);
}

export async function request<T>(
  session: Session, path: string, body?: unknown, operationId?: string, signal?: AbortSignal,
): Promise<T> {
  if (!endpoint.startsWith('https://')) throw Error('Configure an HTTPS Java API URL');
  if (session.expiresAt <= Date.now()) {
    throw Object.assign(Error('Session expired. Sign in again to continue.'), {status: 401});
  }
  const response = await fetch(endpoint.replace(/\/+$/, '') + '/api/v1/' + path, {
    method: body === undefined ? 'GET' : 'POST',
    headers: {
      Authorization: 'Bearer ' + session.accessToken,
      Accept: 'application/json, application/problem+json',
      ...(body === undefined ? {} : {'Content-Type': 'application/json'}),
      ...(operationId ? {'Idempotency-Key': operationId, 'X-Request-ID': operationId} : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  });
  if (!response.ok) {
    const problem = await response.json().catch(() => null);
    const message = problem?.detail ?? problem?.message;
    const fallback = response.status === 401 ? 'Sign in again to continue.'
      : response.status === 403 ? 'Your current session does not have permission.'
      : 'Java API request failed (' + response.status + ').';
    throw Object.assign(Error(typeof message === 'string' && message ? message : fallback), {status: response.status});
  }
  return await response.json() as T;
}
export function SessionProvider({children}:{children:React.ReactNode}){const [session,setSession]=useState<Session|null>(null),[error,setError]=useState(''),[ready,setReady]=useState(false);const discovery=AuthSession.useAutoDiscovery(issuer.startsWith('https://')?issuer:null);const [authRequest,,prompt]=AuthSession.useAuthRequest({clientId,redirectUri:AuthSession.makeRedirectUri({scheme:'quincaisoft'}),responseType:AuthSession.ResponseType.Code,usePKCE:true,scopes:['openid','profile','catalog:read','sales:read','sales:write']},discovery);
useEffect(()=>{SecureStore.getItemAsync('quincai_session').then(v=>{if(v){const s=JSON.parse(v) as Session;if(s.expiresAt>Date.now())setSession(s);}}).catch(e=>setError(String(e))).finally(()=>setReady(true));},[]);
async function login(){try{setError('');if(!issuer.startsWith('https://')||!discovery||!authRequest)throw Error('Configure and wait for the HTTPS OIDC issuer');const result=await prompt();if(result.type!=='success')return;const tokens=await AuthSession.exchangeCodeAsync({clientId,code:result.params.code,redirectUri:AuthSession.makeRedirectUri({scheme:'quincaisoft'}),extraParams:{code_verifier:authRequest.codeVerifier??''}},discovery);const provisional={accessToken:tokens.accessToken,expiresAt:Date.now()+(tokens.expiresIn??300)*1000} as Session;const me=await request<Pick<Session,'scope'|'authorities'>>(provisional,'session');const s={...provisional,...me};await SecureStore.setItemAsync('quincai_session',JSON.stringify(s),{keychainAccessible:SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY});setSession(s);}catch(e){setError(String(e));}}
async function logout(){await SecureStore.deleteItemAsync('quincai_session');await closeDatabase();setSession(null);}
return <C.Provider value={{session,login,logout,error,ready}}>{children}</C.Provider>;}export function useSession(){return useContext(C);}

