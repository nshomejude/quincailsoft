import {NextRequest,NextResponse} from 'next/server';import {completeLogin,appUrl} from '@/lib/auth';
export async function GET(request:NextRequest){try{await completeLogin(new URL(request.url));return NextResponse.redirect(appUrl())}catch{return NextResponse.redirect(new URL('/?auth=failed',appUrl()))}}
