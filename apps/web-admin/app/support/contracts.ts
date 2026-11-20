export type Grant={id:string;tenantId:string;companyId:string;branchId:string;ownerActorId:string;operatorId:string;approvedBy:string|null;status:string;expiresAt:string;version:string;ticketReference:string;reason:string};
export type SupportState={ok:boolean;message:string};
export function supportInput(fd:FormData,now=Date.now()){
 const text=(name:string,max=2000)=>{const value=String(fd.get(name)??'').trim();if(!value||value.length>max||/[\u0000-\u001f]/.test(value))throw new Error('Complete every field using the permitted length.');return value;};
 const uuid=(name:string)=>{const v=text(name,36);if(!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(v))throw new Error('Enter a valid record or operator ID.');return v;};
 const operationId=uuid('operationId'),action=text('action',20),reason=text('reason');if(reason.length<3)throw new Error('Complete every field using the permitted length.');
 if(action==='revoke'){const expected=text('expectedVersion',20);if(!/^(0|[1-9][0-9]*)$/.test(expected)||!Number.isSafeInteger(Number(expected)))throw new Error('Reload the grant before continuing.');return {operationId,path:'support/grants/'+uuid('id')+'/revoke',body:{operationId,expectedVersion:Number(expected),reason}};}
 if(action!=='consent')throw new Error('Unsupported support action.');
 const expiresAt=text('expiresAt',40),expiry=Date.parse(expiresAt);if(!/Z$|[+-]\d{2}:\d{2}$/.test(expiresAt)||!Number.isFinite(expiry)||expiry<=now||expiry>now+24*60*60*1000)throw new Error('Consent must expire within the next 24 hours. Use an ISO timestamp with timezone.');
 return {operationId,path:'support/grants',body:{operationId,operatorId:uuid('operatorId'),ticketReference:text('ticketReference',200),reason,expiresAt:new Date(expiry).toISOString()}};
}
