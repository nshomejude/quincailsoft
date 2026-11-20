import {text,uuid,version} from './validation.ts';
export type SupportGrant={id:string;tenantId:string;companyId:string;branchId:string;ownerActorId:string;operatorId:string;approvedBy:string|null;status:string;expiresAt:string;version:string;ticketReference:string;reason:string};
export type TenantStatus={tenantId:string;status:string;epoch:number;lastDecisionId:string|null};
export type LegalHold={id:string;tenantId:string;reference:string;reason:string;status:string;placedBy:string;releasedBy:string|null;version:string};
export type GovernanceRequest={id:string;tenantId:string;kind:string;status:string;requestedBy:string;approvedBy:string|null;expectedTargetVersion:number;version:string;reference:string;reason:string;holdId:string|null};
export type Diagnostics={grantId:string;tenantId:string;companyId:string;branchId:string;generatedAt:string;productCount:string;postedSaleCount:string;stockPositionCount:string};
export function governanceInput(fd:FormData){
 const operationId=uuid(text(fd,'operationId',36)),action=text(fd,'action',40),expectedVersion=version(text(fd,'expectedVersion',20)),reason=text(fd,'reason',2000);if(reason.length<3)throw new Error('Provide a justification of at least three characters.');
 if(action==='approve-support'||action==='approve-request')return {operationId,path:'/admin/governance/'+(action==='approve-support'?'support-grants':'requests')+'/'+uuid(text(fd,'id',36))+'/approve',body:{operationId,expectedVersion,reason}};
 if(action!=='request')throw new Error('Unknown governance action.');
 const tenantId=uuid(text(fd,'tenantId',36)),kind=text(fd,'kind',40);
 if(!['SUSPEND','RESUME','PLACE_LEGAL_HOLD','RELEASE_LEGAL_HOLD'].includes(kind))throw new Error('Select a supported governance decision.');
 const holdId=kind==='RELEASE_LEGAL_HOLD'?uuid(text(fd,'holdId',36)):null;
 return {operationId,path:'/admin/governance/tenants/'+tenantId+'/requests',body:{operationId,kind,expectedVersion,holdId,reference:text(fd,'reference',200),reason}};
}
