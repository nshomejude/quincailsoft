import {NextRequest,NextResponse} from 'next/server';import {logout,appUrl} from '@/lib/auth';
export async function POST(request:NextRequest){if(request.headers.get('origin')!==appUrl().origin)return new NextResponse('Forbidden',{status:403});await logout();return NextResponse.redirect(appUrl(),303)}
