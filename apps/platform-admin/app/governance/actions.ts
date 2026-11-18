'use server';
import {headers} from 'next/headers';
import {revalidatePath} from 'next/cache';
import {appUrl} from '@/lib/auth';
import {platform,PlatformError} from '@/lib/api';
import {governanceInput} from '@/lib/governance';
import type {FormState} from '@/lib/contracts';
export async function governanceAction(_:FormState,fd:FormData):Promise<FormState>{
 try{if((await headers()).get('origin')!==appUrl().origin)throw new Error('Origin denied. Open the form from the configured platform URL.');const input=governanceInput(fd);await platform(input.path,input.body,input.operationId);revalidatePath('/governance');return {ok:true,message:'Governance action recorded. Reload to inspect the current decision.'};}
 catch(e){return {ok:false,message:e instanceof PlatformError?e.message:e instanceof Error&&!(e instanceof TypeError)&&e.name!=='TimeoutError'?e.message:'The response is unavailable. Check the record before retrying this same operation.'};}
}
