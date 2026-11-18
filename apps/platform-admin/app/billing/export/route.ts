import {NextResponse} from 'next/server';
import {platformBillingCsv,PlatformError} from '@/lib/api';
import {billingQuery} from '@/lib/billing-query';
import {language} from '@/lib/language';
import {translate} from '@/lib/i18n';
export const dynamic='force-dynamic';
export async function GET(request:Request){
 const locale=await language(),q=new URL(request.url).searchParams;
 const headers={'Cache-Control':'private, no-store','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer'};
 let query;try{for(const name of ['tenantId','limit','cursor'])if(q.getAll(name).length>1)throw new Error('Use one value per billing filter.');query=billingQuery({tenantId:q.get('tenantId')??undefined,limit:q.get('limit')??undefined,cursor:q.get('cursor')??undefined});}catch(e){return new NextResponse(translate(locale,e instanceof Error?e.message:'Check the submitted values.'),{status:400,headers});}
 try{const result=await platformBillingCsv(query.params);return new NextResponse(result.csv,{headers:{...headers,'Content-Type':'text/csv; charset=utf-8','Content-Disposition':'attachment; filename="subscription-invoices-page.csv"',...(result.nextCursor?{'X-Next-Cursor':result.nextCursor}:{}),...(result.cutoff?{'X-Export-Cutoff':result.cutoff}:{})}});}
 catch(e){return new NextResponse(translate(locale,e instanceof PlatformError?e.message:'The service could not complete this request. Retry with the same operation ID.'),{status:e instanceof PlatformError?e.status:502,headers});}
}
