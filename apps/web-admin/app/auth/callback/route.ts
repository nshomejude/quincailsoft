import {NextRequest, NextResponse} from 'next/server';
import {appUrl, completeLogin} from '@/lib/auth';

export async function GET(request: NextRequest) {
    try {
        const callback = new URL('/auth/callback', appUrl());
        callback.search = request.nextUrl.search;
        await completeLogin(callback);
        return NextResponse.redirect(appUrl());
    } catch (error) {
        console.warn('OIDC callback failed', error instanceof Error ? error.name : 'unknown error');
        return NextResponse.redirect(new URL('/?auth=failed', appUrl()));
    }
}
