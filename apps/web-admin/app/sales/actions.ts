'use server';
import {api,Sale,ApiError} from '@/lib/api';import {redirect} from 'next/navigation';import {revalidatePath} from 'next/cache';import {headers} from 'next/headers';import {appUrl} from '@/lib/auth';import {quantityMilli} from '@/lib/input';
export type Result={error:string};
const uuid=/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
async function origin(){if((await headers()).get('origin')!==appUrl().origin)throw new Error('Request origin rejected.');}
function required(fd:FormData,key:string){const v=String(fd.get(key)??'');if(!v)throw new Error('Complete all required fields.');return v;}
function id(fd:FormData,key:string){const v=required(fd,key);if(!uuid.test(v))throw new Error('Choose a valid '+key+'.');return v;}
export async function createSale(_:Result,fd:FormData):Promise<Result>{let created:Sale;
try{await origin();const quantity=quantityMilli(required(fd,'quantity'));const businessDate=required(fd,'businessDate');if(!/^\d{4}-\d{2}-\d{2}$/.test(businessDate))throw new Error('Choose a business date.');created=await api<Sale>('sales',{method:'POST',body:JSON.stringify({operationId:id(fd,'operationId'),warehouseId:id(fd,'warehouseId'),businessDate,lines:[{productId:id(fd,'productId'),quantityMilli:String(quantity)}]})});}catch(error){return {error:error instanceof ApiError?error.message:error instanceof Error?error.message:'Unable to create sale.'}}
revalidatePath('/sales');redirect('/sales/'+created.id);}
export async function postSale(_:Result,fd:FormData):Promise<Result>{let saleId='';try{await origin();saleId=id(fd,'saleId');const version=required(fd,'version');if(!/^\d+$/.test(version)||BigInt(version)>BigInt(Number.MAX_SAFE_INTEGER))throw new Error('Refresh this sale before posting.');const reason=required(fd,'reason').trim();if(reason.length<3)throw new Error('Give a reason for posting.');await api<Sale>('sales/'+saleId+'/post',{method:'POST',body:JSON.stringify({operationId:id(fd,'operationId'),version:Number(version),reason})});}catch(error){return {error:error instanceof Error?error.message:'Unable to post sale.'}}revalidatePath('/sales');redirect('/sales/'+saleId);}
