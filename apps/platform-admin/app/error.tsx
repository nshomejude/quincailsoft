'use client';

import {translate as t} from '@/lib/i18n';
import {useLanguage} from '@/components/Language';
export default function ErrorPage({reset}:{reset:()=>void}){const locale=useLanguage();return <main><h1>{t(locale,"The operation could not be completed")}</h1><p>{t(locale,"Review your access and current record state, then reload. No success is assumed.")}</p><button onClick={reset}>{t(locale,"Reload")}</button></main>}