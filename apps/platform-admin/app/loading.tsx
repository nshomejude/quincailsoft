
import {translate as t} from '@/lib/i18n';
import {language} from '@/lib/language';
export default async function Loading(){const locale=await language();return <main aria-busy="true"><p className="eyebrow">{t(locale,"Platform workspace")}</p><h1>{t(locale,"Loading current records…")}</h1><p className="loading" role="status">{t(locale,"Retrieving authorized data from the service.")}</p></main>}
