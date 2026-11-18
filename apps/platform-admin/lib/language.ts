import 'server-only';
import {cookies,headers} from 'next/headers';
import {preferredLanguage} from './i18n';
export async function language(){return preferredLanguage((await cookies()).get('quincai_platform_language')?.value,(await headers()).get('accept-language'));}
