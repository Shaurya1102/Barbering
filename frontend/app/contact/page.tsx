import Link from 'next/link';

export default function ContactPage() {
  return <main className="content-page">
    <section className="content-hero">
      <p className="eyebrow">A hand when you need one</p>
      <h1>Let’s get you to the right place.</h1>
      <p className="mt-6 max-w-2xl text-base leading-8 text-neutral-600">Barbering does not currently offer an in-app messaging inbox. For a booking, start from the barber’s public profile and use the appointment tools attached to your account.</p>
    </section>
    <section className="feature-grid">
      <article className="feature-card"><span className="feature-card-number">01</span><h2>Looking to book?</h2><p>Search local profiles, compare services, and request a time directly from a barber’s page.</p><Link className="mt-5 inline-flex text-sm font-semibold text-[#805535] underline underline-offset-4" href="/search">Find a barber →</Link></article>
      <article className="feature-card"><span className="feature-card-number">02</span><h2>Already requested?</h2><p>Sign in to see the current status of your requests and manage eligible appointments.</p><Link className="mt-5 inline-flex text-sm font-semibold text-[#805535] underline underline-offset-4" href="/auth/login">Go to your account →</Link></article>
      <article className="feature-card"><span className="feature-card-number">03</span><h2>Are you a barber?</h2><p>Keep your profile, services, prices, hours, and recurring breaks up to date in your workspace.</p><Link className="mt-5 inline-flex text-sm font-semibold text-[#805535] underline underline-offset-4" href="/for-barbers">Barber workspace →</Link></article>
    </section>
  </main>;
}
