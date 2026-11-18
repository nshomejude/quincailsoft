import {uuid} from './validation.ts';
export type BillingQuery={tenantId?:string|string[];cursor?:string|string[];limit?:string|string[]};
export function billingQuery(input:BillingQuery){
 const scalar=(value:string|string[]|undefined)=>{if(Array.isArray(value))throw new Error('Use one value per billing filter.');return value;};
 const tenantId=uuid(scalar(input.tenantId)??'');
 const rawLimit=scalar(input.limit)??'50';
 if(!/^[1-9][0-9]*$/.test(rawLimit)||Number(rawLimit)>100)throw new Error('Choose a page size from 1 to 100.');
 const limit=Number(rawLimit),cursor=scalar(input.cursor)??'';
 if(cursor.length>4096||/[\u0000-\u0020\u007f]/.test(cursor))throw new Error('The page cursor is invalid. Start again from the first page.');
 const params=new URLSearchParams({tenantId,limit:String(limit)});if(cursor)params.set('cursor',cursor);
 return {tenantId,limit,cursor,params};
}
