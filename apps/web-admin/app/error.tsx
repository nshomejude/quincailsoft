'use client';
export default function ErrorView({reset}:{reset:()=>void}){return <section className="panel" role="alert"><h1>Unable to load this view</h1><p>Your request could not be completed. No success has been assumed.</p><button onClick={reset}>Try again</button></section>}
