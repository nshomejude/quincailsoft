import {NextRequest,NextResponse} from 'next/server';
import {session} from '@/lib/auth';
import {reportExportTarget,type ReportDefinition} from '@/lib/report-contract';
export async function GET(request:NextRequest,{params}:{params:Promise<{code:string}>}){
 const {code}=await params;
 if(!/^[a-z][a-z0-9-]{0,63}$/.test(code))return new NextResponse('Unknown report',{status:404});
 const auth=await session();if(!auth)return new NextResponse('Sign in required',{status:401});
 const base=process.env.JAVA_API_URL??'http://localhost:8080';
 const headers={Authorization:'Bearer '+auth.accessToken};
 const catalogResponse=await fetch(new URL('/api/v1/reports/catalog',base),{headers,cache:'no-store'});
 if(!catalogResponse.ok)return new NextResponse('Report catalog unavailable',{status:catalogResponse.status});
 const target=reportExportTarget(code,await catalogResponse.json() as ReportDefinition[],request.nextUrl.searchParams,base);
 if(!target)return new NextResponse('Unknown report',{status:404});
 const response=await fetch(target,{headers,cache:'no-store'});
 if(!response.ok)return new NextResponse('Report export unavailable',{status:response.status});
 return new NextResponse(await response.text(),{headers:{'Content-Type':'text/csv; charset=utf-8','Content-Disposition':'attachment; filename="quincaisoft-'+code+'.csv"','X-Report-Has-More':response.headers.get('X-Report-Has-More')??'false','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}});
}
