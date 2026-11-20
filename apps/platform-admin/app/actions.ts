'use server';
import {headers} from 'next/headers';
import {revalidatePath} from 'next/cache';
import {platform,PlatformError} from '@/lib/api';
import {appUrl} from '@/lib/auth';
import type {FormState,Profile} from '@/lib/contracts';
import {text,optionalText,uuid,version,money,billingTotal,date,sha256} from '@/lib/validation';
async function command(fd:FormData,path:string,build:(id:string)=>object,refresh:string,success:string):Promise<FormState>{
 try{if((await headers()).get('origin')!==appUrl().origin)throw new Error('Origin denied. Open the form from the configured platform URL.');const operationId=uuid(text(fd,'operationId',36));await platform(path,build(operationId),operationId);revalidatePath(refresh);return {ok:true,message:success};}
 catch(error){return {ok:false,message:error instanceof PlatformError?error.message+(error.requestId?' Reference: '+error.requestId:''):error instanceof Error&&!(error instanceof TypeError)&&error.name!=='TimeoutError'?error.message:'The response is unavailable. Check the record before retrying this same operation.'};}
}
export async function requestTenant(_:FormState,fd:FormData){return command(fd,'/admin/onboarding',operationId=>({operationId,displayName:text(fd,'displayName',120),reason:text(fd,'reason',2000)}),'/','Request recorded for independent review.');}
export async function approveTenant(_:FormState,fd:FormData){const id=uuid(String(fd.get('id')));return command(fd,`/admin/onboarding/${id}/approve`,operationId=>({operationId,version:version(text(fd,'version'))}),`/onboarding/${id}`,'Request approved. Continue with company and owner configuration.');}
export async function configureTenant(_:FormState,fd:FormData){const id=uuid(String(fd.get('id')));return command(fd,`/admin/onboarding/${id}/configuration`,operationId=>{
 const profile:Profile={legalName:text(fd,'legalName',200),registrationNumber:text(fd,'registrationNumber',100),address:text(fd,'address',500),ownerSubject:uuid(text(fd,'ownerSubject',36)),ownerEmail:text(fd,'ownerEmail',254),region:text(fd,'region',80),timezone:text(fd,'timezone',80),locale:text(fd,'locale',2),documentPrefix:text(fd,'documentPrefix',16),openingMode:text(fd,'openingMode',20),privacyVersion:text(fd,'privacyVersion',100),termsVersion:text(fd,'termsVersion',100),planCode:text(fd,'planCode',80)};
 if(!/^[A-Z0-9-]{1,16}$/.test(profile.documentPrefix))throw new Error('Document prefix uses uppercase letters, digits and hyphens.');return {operationId,requestVersion:version(text(fd,'version')),profile};},`/onboarding/${id}`,'Configuration saved. Verify the intended owner identity next.');}
export async function verifyOwner(_:FormState,fd:FormData){const id=uuid(String(fd.get('id')));return command(fd,`/admin/onboarding/${id}/configuration/verify-owner`,operationId=>{const ownerToken=String(fd.get('ownerToken')??'');if(!ownerToken||ownerToken.length>32768)throw new Error('Provide the owner identity proof.');return {operationId,version:version(text(fd,'version')),ownerToken};},`/onboarding/${id}`,'Owner identity verified. A separate operator must approve the configuration.');}
export async function approveConfiguration(_:FormState,fd:FormData){const id=uuid(String(fd.get('id')));return command(fd,`/admin/onboarding/${id}/configuration/approve`,operationId=>({operationId,version:version(text(fd,'version'))}),`/onboarding/${id}`,'Configuration approved. Check the provisioning requirements before activation.');}
export async function provisionTrial(_:FormState,fd:FormData){const id=uuid(String(fd.get('id')));return command(fd,`/admin/onboarding/${id}/provision-trial`,operationId=>({operationId,configurationVersion:version(text(fd,'version')),branchName:text(fd,'branchName',120),warehouseName:text(fd,'warehouseName',120),businessDate:date(text(fd,'businessDate',10))}),`/onboarding/${id}`,'Provisioning completed. Reload to inspect tenant IDs, completed checks and trial expiry.');}
export async function issueInvoice(_:FormState,fd:FormData){return command(fd,'/admin/billing/invoices',operationId=>{
 const subtotalXaf=money(text(fd,'subtotalXaf',19)),discountXaf=money(text(fd,'discountXaf',19)),taxXaf=money(text(fd,'taxXaf',19));billingTotal(subtotalXaf,discountXaf,taxXaf);
 const periodStart=date(text(fd,'periodStart',10)),periodEnd=date(text(fd,'periodEnd',10)),dueDate=date(text(fd,'dueDate',10));if(periodEnd<periodStart||dueDate<periodStart)throw new Error('Billing end and due date cannot precede period start.');
 return {operationId,tenantId:uuid(text(fd,'tenantId',36)),originalId:fd.get('originalId')?uuid(text(fd,'originalId',36)):null,issuerName:text(fd,'issuerName'),issuerTaxReference:text(fd,'issuerTaxReference'),customerName:text(fd,'customerName'),customerTaxReference:optionalText(fd,'customerTaxReference',200),description:text(fd,'description'),jurisdiction:text(fd,'jurisdiction'),periodStart,periodEnd,dueDate,subtotalXaf,discountXaf,taxXaf};},'/billing','Billing document issued. This does not confirm payment or refund settlement.');}

export async function subscriptionCommand(_:FormState,fd:FormData):Promise<FormState>{
 const kind=String(fd.get('command'));
 let path='/admin/subscriptions';
 try {
 if(kind==='plan')path+='/plans';
 else if(kind==='commercial-terms')path+='/commercial-terms';
 else if(kind==='approve-commercial-terms')path+='/commercial-terms/'+uuid(text(fd,'id',36))+'/approve';
 else if(kind==='notice'||kind==='payment')path+='/'+kind+'s';
 else if(kind==='approve-notice'||kind==='approve-payment')path+='/'+(kind==='approve-notice'?'notices':'payments')+'/'+uuid(text(fd,'id',36))+'/approve';
 else if(kind==='activate-payment')path+='/payments/'+uuid(text(fd,'id',36))+'/activate';
 else if(kind==='expire'||kind==='cancel')path+='/'+uuid(text(fd,'tenantId',36))+'/'+kind;
 else throw new Error('Unknown subscription action.');
 }catch(e){return {ok:false,message:e instanceof Error?e.message:'Invalid action'};}
 return command(fd,path,operationId=>{
 const instant=(name:string)=>{const value=text(fd,name,40);if(!/Z$|[+-]\d{2}:\d{2}$/.test(value)||!Number.isFinite(Date.parse(value)))throw new Error('Use an ISO timestamp with timezone for '+name);return new Date(value).toISOString();};
 if(kind==='plan'){const capacity=(name:string)=>{const n=version(text(fd,name));if(n<1||n>2147483647)throw new Error('Capacity must be a positive whole number.');return n;};return {operationId,plan:{code:text(fd,'code',80),name:text(fd,'name',200),monthlyPriceXaf:money(text(fd,'monthlyPriceXaf',19)),seatLimit:capacity('seatLimit'),branchLimit:capacity('branchLimit'),warehouseLimit:capacity('warehouseLimit'),commerceAccess:fd.get('commerceAccess')==='on',effectiveAt:instant('effectiveAt')}};}
 if(kind.startsWith('approve-')||kind==='activate-payment')return {operationId};
 const tenantId=uuid(text(fd,'tenantId',36));
 if(kind==='commercial-terms'){const basis=text(fd,'basis',20);if(basis!=='PRORATED'&&basis!=='NEGOTIATED')throw new Error('Select prorated or negotiated terms.');return {operationId,tenantId,invoiceId:uuid(text(fd,'invoiceId',36)),planCode:text(fd,'planCode',80),basis,evidenceReference:text(fd,'evidenceReference'),evidenceSha256:sha256(text(fd,'evidenceSha256',64)),reason:text(fd,'reason')};}
 if(kind==='notice')return {operationId,tenantId,subscriptionVersion:version(text(fd,'version')),deliveryReference:text(fd,'deliveryReference'),deliveredAt:instant('deliveredAt'),graceEndsAt:instant('graceEndsAt'),contractReference:text(fd,'contractReference'),reason:text(fd,'reason')};
 if(kind==='payment'){const evidenceSha256=sha256(text(fd,'evidenceSha256',64));return {operationId,tenantId,invoiceId:uuid(text(fd,'invoiceId',36)),planCode:text(fd,'planCode',80),amountXaf:money(text(fd,'amountXaf',19)),bankReference:text(fd,'bankReference'),evidenceSha256,reason:text(fd,'reason')};}
 return {operationId,expectedVersion:version(text(fd,'version')),reason:text(fd,'reason')};
 },'/subscriptions','Subscription action recorded. Reload to inspect the current status.');
}
