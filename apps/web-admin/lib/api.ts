import 'server-only';import {session} from './auth';
export type Product={id:string;sku:string;name:string;baseUnit:string;sellingPriceXaf:string;taxBasisPoints:number;version:string;active:boolean;categoryId:string|null;brand:string;barcode:string;packSizeMilli:string;reorderLevelMilli:string;trackStock:boolean;trackSerial:boolean;trackExpiry:boolean;description:string};
export type Named={id:string;name:string;active?:boolean;version?:string};
export type Sale={id:string;status:string;businessDate:string;totalXaf:string;version:number;warehouseId:string;customerId:string;lines:{id:string;productId:string;quantityMilli:string;unitPriceXaf:string;lineTotalXaf:string}[]};
export type Me={scope:{tenantId:string;companyId:string;branchId:string;actorId:string};authorities:string[]};
export class ApiError extends Error{constructor(public status:number,message?:string,public code?:string,public correlationId?:string){super(message??(status===401?'Sign in to access your workspace.':status===403?'Your role does not permit this operation.':'The business service is unavailable. Try again.'))}}
export async function api<T>(path:string,init?:RequestInit):Promise<T>{
 const auth=await session();if(!auth)throw new ApiError(401);
 const base=new URL(process.env.JAVA_API_URL??'http://localhost:8080');
 const response=await fetch(new URL('/api/v1/'+path,base),{...init,cache:'no-store',headers:{'Content-Type':'application/json','Authorization':'Bearer '+auth.accessToken,...init?.headers}});
 if(!response.ok){const data=await response.json().catch(()=>({}));throw new ApiError(response.status,typeof data.message==='string'?data.message:undefined,data.code,data.correlationId)}return response.json() as Promise<T>;
}
export const products=()=>api<Product[]>('products');export const sales=()=>api<Sale[]>('sales');
export const permitted=(me:Me,permission:string)=>me.authorities.some(a=>a===permission||a==='SCOPE_'+permission);
export {xaf,units} from './format';
