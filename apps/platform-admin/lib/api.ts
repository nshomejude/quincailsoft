import 'server-only';
import {session} from './auth';
export type {RequestItem} from './contracts';
export class PlatformError extends Error {constructor(public status:number,public requestId:string|null){super(({400:'Check the submitted values.',401:'Your session has expired. Sign in again.',403:'Your current operator permissions do not allow this action.',404:'The requested record is not available.',409:'The record or operation changed. Reload and check its latest state.',422:'The workflow requirements have not been met.'} as Record<number,string>)[status]??'The service could not complete this request. Retry with the same operation ID.');}}
export async function platform<T>(path:string,body?:object,operation?:string):Promise<T>{
 const s=await session();if(!s)throw new PlatformError(401,null);
 const base=process.env.PLATFORM_API_URL;if(!base)throw new Error('Platform API is not configured.');
 if(!path.startsWith('/admin/')||path.startsWith('//'))throw new Error('Invalid platform route.');
 const response=await fetch(new URL(path,base),{method:body?'POST':'GET',headers:{Authorization:'Bearer '+s.accessToken,...(body?{'Content-Type':'application/json','Idempotency-Key':operation!,'X-Request-ID':crypto.randomUUID()}:{})},body:body?JSON.stringify(body):undefined,cache:'no-store',signal:AbortSignal.timeout(30000)});
 if(!response.ok)throw new PlatformError(response.status,response.headers.get('X-Request-ID'));
 return response.json() as Promise<T>;
}
export async function platformHtml(path:string):Promise<string>{
 const s=await session();if(!s)throw new PlatformError(401,null);
 if(!/^\/admin\/(billing\/invoices|subscriptions\/payments)\/[0-9a-f-]{36}\/print\?locale=(en|fr)$/i.test(path))throw new Error('Invalid platform route.');
 const base=process.env.PLATFORM_API_URL;if(!base)throw new Error('Platform API is not configured.');
 const response=await fetch(new URL(path,base),{headers:{Authorization:'Bearer '+s.accessToken,Accept:'text/html'},cache:'no-store',signal:AbortSignal.timeout(30000),redirect:'error'});
 if(!response.ok)throw new PlatformError(response.status,response.headers.get('X-Request-ID'));
 if(!response.headers.get('content-type')?.toLowerCase().startsWith('text/html'))throw new Error('Unexpected document format.');
 return response.text();
}
