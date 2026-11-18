import {notFound} from 'next/navigation';
import {platform} from '@/lib/api';
import {uuid} from '@/lib/validation';
import {language} from '@/lib/language';
import {translate as t} from '@/lib/i18n';
import type {Diagnostics} from '@/lib/governance';
export const dynamic='force-dynamic';
export default async function SupportDiagnostics({params}:{params:Promise<{id:string}>}){
 const locale=await language(),{id}=await params;try{uuid(id)}catch{notFound();}
 let data:Diagnostics;try{data=await platform<Diagnostics>('/admin/governance/support-grants/'+id+'/diagnostics');}catch(e){return <main><a href="/governance">{t(locale,'Support & governance')}</a><h1>{t(locale,'Redacted support diagnostics')}</h1><p className="notice error" role="alert">{t(locale,e instanceof Error?e.message:'Service unavailable')}</p></main>;}
 return <main><a href="/governance">{t(locale,'Support & governance')}</a><h1>{t(locale,'Redacted support diagnostics')}</h1><p className="notice">{t(locale,'This read is audited under the approved, unexpired grant for its named operator. Only scoped counts are returned; no merchant impersonation or customer records are exposed.')}</p><section className="card"><dl className="key-value">{[['Grant ID',data.grantId],['Tenant',data.tenantId],['Company',data.companyId],['Branch',data.branchId],['Generated at',data.generatedAt],['Products',String(data.productCount)],['Posted sales',String(data.postedSaleCount)],['Stock positions',String(data.stockPositionCount)]].map(([k,v])=><div className="wide" key={k}><dt>{t(locale,k)}</dt><dd className="mono">{v}</dd></div>)}</dl></section></main>;
}
