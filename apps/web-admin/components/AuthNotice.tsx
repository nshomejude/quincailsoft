// Shown on the home page when /auth/login or /auth/callback redirected back with ?auth=failed|unavailable.
const messages: Record<string, { title: string; body: string }> = {
  failed: {
    title: 'Sign-in did not complete',
    body: 'We could not verify your sign-in. Please try again. If this keeps happening, contact your administrator.'
  },
  unavailable: {
    title: 'Sign-in is temporarily unavailable',
    body: 'The sign-in service could not be reached. Please try again in a few minutes.'
  }
};

export function AuthNotice({ state }: { state?: string }) {
  const message = state ? messages[state] : undefined;
  if (!message) return null;
  return (
    <section className="panel" role="alert" style={{ borderLeft: '4px solid #b3261e' }}>
      <h2 style={{ marginTop: 0 }}>{message.title}</h2>
      <p>{message.body}</p>
    </section>
  );
}
