'use server';
import {headers} from 'next/headers';
import {revalidatePath} from 'next/cache';
import {api,ApiError,permitted,type Me} from '@/lib/api';
import {appUrl} from '@/lib/auth';
import {supportInput,type SupportState} from './contracts';
export async function supportAction(_:SupportState,fd:FormData):Promise<SupportState>{
 try{if((await headers()).get('origin')!==appUrl().origin)throw new Error('Open this form from the configured merchant URL.');const input=supportInput(fd),me=await api<Me>('me');if(!permitted(me,'support:write'))throw new ApiError(403);await api(input.path,{method:'POST',headers:{'Idempotency-Key':input.operationId,'X-Request-ID':crypto.randomUUID()},body:JSON.stringify(input.body)});revalidatePath('/support');return {ok:true,message:'The operation was recorded. Reload to inspect the current grant.'};}
 catch(e){const message=e instanceof ApiError?e.status===401?'Sign in to access support consent.':e.status===403?'Your role or ownership does not allow this action.':[404,409,412].includes(e.status)?'The grant changed or cannot be used. Reload and review its status.':[400,422].includes(e.status)?'Check the submitted consent and expiry.':'The service response is unavailable. Check the grant before retrying the same operation.':e instanceof Error&&!(e instanceof TypeError)&&e.name!=='TimeoutError'?e.message:'The service response is unavailable. Check the grant before retrying the same operation.';return {ok:false,message};}
}
