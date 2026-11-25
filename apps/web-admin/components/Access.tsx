import Link from 'next/link';
import {ApiError} from '@/lib/api';
import {locale, messages} from '@/lib/locale';

// Statuses that describe a request the user can correct, not an outage.
const BUSINESS_STATUSES = [400, 404, 409, 412, 422];

export async function Access({error}: {error: unknown}) {
  const t = messages[await locale()];
  const status = error instanceof ApiError ? error.status : 503;
  const business = BUSINESS_STATUSES.includes(status);

  if (status === 403) {
    return (
      <section className="panel" role="alert">
        <p className="eyebrow">ACCESS RESTRICTED · 403</p>
        <h2>{t.accessDeniedTitle}</h2>
        <p>{t.accessDeniedBody}</p>
        <p className="muted">Contact your administrator to review your assigned role and branch.</p>
        <Link className="button" href="/">{t.backHome}</Link>
      </section>
    );
  }

  const eyebrow =
    status === 401 ? 'AUTHENTICATION REQUIRED' : business ? 'REQUEST NEEDS ATTENTION' : 'CONNECTION UNAVAILABLE';
  const heading =
    status === 401
      ? 'Sign in to your workspace'
      : business
        ? 'The request could not be completed'
        : 'We could not load your business data';
  const body =
    status === 401
      ? 'Use your organisation’s secure identity provider to continue.'
      : business && error instanceof ApiError
        ? error.message
        : 'Check that the business service and identity provider are configured and running.';

  return (
    <section className="panel">
      <p className="eyebrow">{eyebrow}</p>
      <h2>{heading}</h2>
      <p>{body}</p>
      {status === 401 ? (
        <a className="button" href="/auth/login">Continue to secure sign-in</a>
      ) : (
        <Link className="button" href="/">{t.backHome}</Link>
      )}
    </section>
  );
}
