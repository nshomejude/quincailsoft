import { CONTACT_EMAIL, type Lang } from "@/lib/site";
import { t } from "@/lib/content";

/**
 * JavaScript-free form posting to a mailto: placeholder.
 * TODO(Opesware): replace the mailto action with a server-side handler (with
 * spam protection) once the receiving inbox is confirmed.
 */
export function ContactForm({ lang, showBranches = false }: { lang: Lang; showBranches?: boolean }) {
  const d = t(lang).pricing;
  const subject = lang === "en" ? "QuincaiSoft enquiry" : "Demande QuincaiSoft";
  return (
    <form className="form" action={`mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent(subject)}`} method="post" encType="text/plain">
      <div>
        <label htmlFor="f-name">{d.name}</label>
        <input id="f-name" name="name" autoComplete="name" required />
      </div>
      <div>
        <label htmlFor="f-email">{d.email}</label>
        <input id="f-email" name="email" type="email" autoComplete="email" required />
      </div>
      <div>
        <label htmlFor="f-company">{d.company}</label>
        <input id="f-company" name="company" autoComplete="organization" />
      </div>
      <div>
        <label htmlFor="f-phone">{d.phone}</label>
        <input id="f-phone" name="phone" type="tel" autoComplete="tel" />
      </div>
      {showBranches && (
        <div>
          <label htmlFor="f-branches">{d.branches}</label>
          <input id="f-branches" name="branches" inputMode="numeric" />
        </div>
      )}
      <div>
        <label htmlFor="f-message">{d.message}</label>
        <textarea id="f-message" name="message" rows={5} required />
      </div>
      <button type="submit" className="btn">{d.submit}</button>
      <p className="small">{d.note}</p>
      <p className="small todo">{d.todo}</p>
    </form>
  );
}
