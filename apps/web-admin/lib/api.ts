import 'server-only';import {session} from './auth';
export type Product={id:string;sku:string;name:string;baseUnit:string;sellingPriceXaf:string;version:string;active:boolean};
export type Sale={id:string;status:string;businessDate:string;totalXaf:string;version:number;warehouseId:string;lines?:{id:string;productId:string;quantityMilli:string;unitPriceXaf:string;lineTotalXaf:string}[]};
export class ApiError extends Error{constructor(public status:number){super(status===401?'Sign in to access your workspace.':status===403?'Your role does not permit this operation.':'The business service is unavailable. Try again.')}}
export async function api<T>(path:string,init?:RequestInit):Promise<T>{
 const auth=await session();if(!auth)throw new ApiError(401);
 const base=new URL(process.env.JAVA_API_URL??'http://localhost:8080');
 const response=await fetch(new URL('/api/v1/'+path,base),{...init,cache:'no-store',headers:{'Content-Type':'application/json','Authorization':'Bearer '+auth.accessToken,...init?.headers}});
 if(!response.ok)throw new ApiError(response.status);return response.json() as Promise<T>;
}
export const products=()=>api<Product[]>('products');export const sales=()=>api<Sale[]>('sales');
export function xaf(value:string){try{return new Intl.NumberFormat('fr-CM',{style:'currency',currency:'XAF',maximumFractionDigits:0}).format(BigInt(value))}catch{return '—'}}
