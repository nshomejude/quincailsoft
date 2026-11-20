import {NextResponse} from 'next/server';
import {platformHtml,PlatformError} from '@/lib/api';
import {language} from '@/lib/language';
import {translate} from '@/lib/i18n';
import {uuid} from '@/lib/validation';
export const dynamic='force-dynamic';
export async function GET(_:Request,{params}:{params:Promise<{kind:string;id:string}>}){
 const locale=await language(),{kind,id}=await params;
 if(kind!=='invoices'&&kind!=='payments')return new NextResponse(null,{status:404});
 try{uuid(id)}catch{return new NextResponse(null,{status:404});}
 const headers={'Cache-Control':'private, no-store','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer'};
 try{const html=await platformHtml(`/admin/${kind==='invoices'?'billing/invoices':'subscriptions/payments'}/${id}/print?locale=${locale}`);return new NextResponse(html,{headers:{...headers,'Content-Type':'text/html; charset=utf-8','Content-Security-Policy':"sandbox allow-modals; default-src 'none'; style-src 'unsafe-inline'; img-src data:; base-uri 'none'; form-action 'none'; frame-ancestors 'none'"}});}
 catch(e){return new NextResponse(translate(locale,e instanceof PlatformError?e.message:'The service could not complete this request. Retry with the same operation ID.'),{status:e instanceof PlatformError?e.status:502,headers:{...headers,'Content-Type':'text/plain; charset=utf-8'}});}
}
