import {NextResponse} from 'next/server';import {startLogin,appUrl} from '@/lib/auth';
export async function GET(){try{return NextResponse.redirect(await startLogin())}catch{return NextResponse.redirect(new URL('/?auth=unavailable',appUrl()))}}
