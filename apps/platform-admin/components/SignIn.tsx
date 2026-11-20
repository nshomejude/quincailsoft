
import {translate as t} from '@/lib/i18n';
import {language} from '@/lib/language';
import {configured} from '@/lib/auth';
export default async function SignIn(){const locale=await language();return <main className="sign-in"><p className="eyebrow">{t(locale,"Secure platform workspace")}</p><h1>{t(locale,"Manage merchants with confidence.")}</h1><p className="muted">{t(locale,"Review onboarding, verify owners and manage subscription documents from a separate operator workspace.")}</p><section className="card"><h2>{t(locale,"Operator sign-in")}</h2><p>{t(locale,"An enrolled platform operator account is required. Merchant accounts cannot administer tenants.")}</p>{configured()?<a className="button" href="/auth/login">{t(locale,"Sign in with platform identity")}</a>:<p className="notice">{t(locale,"Platform identity is not configured. Contact the deployment administrator to enable operator sign-in.")}</p>}</section></main>}
