import {NextRequest,NextResponse} from 'next/server';
import {appUrl} from '@/lib/auth';
export async function POST(request:NextRequest){
 if(request.headers.get('origin')!==appUrl().origin)return new NextResponse('Origin denied',{status:403});
 const locale=(await request.formData()).get('locale');if(locale!=='en'&&locale!=='fr')return new NextResponse('Invalid language',{status:400});
 let path='/';try{const referer=new URL(request.headers.get('referer')??'');if(referer.origin===appUrl().origin)path=referer.pathname+referer.search;}catch{}
 const response=NextResponse.redirect(new URL(path,appUrl()),303);response.cookies.set('quincai_platform_language',locale,{httpOnly:true,secure:process.env.NODE_ENV==='production',sameSite:'lax',path:'/',maxAge:31536000});return response;
}
