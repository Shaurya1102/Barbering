import Link from 'next/link';

const features = [
  ['01', 'A profile that works for you', 'Share your location, story, services, prices, and portfolio with customers searching nearby.'],
  ['02', 'Hours that reflect your week', 'Set your regular opening times and recurring breaks so customers see slots that fit your schedule.'],
  ['03', 'One place for appointment requests', 'Review requests, update their status, follow your schedule, and see completed revenue in your workspace.'],
];

export default function ForBarbersPage() {
  return <main className="content-page">
    <section className="content-hero">
      <p className="eyebrow">For independent barbers</p>
      <h1>Put your craft in front of the people looking for it.</h1>
      <p className="mt-6 max-w-2xl text-base leading-8 text-neutral-600">Create a public profile, list the services you offer, set your hours, and manage appointment requests in one place.</p>
      <div className="mt-7 flex flex-wrap gap-3"><Link href="/auth/signup" className="button-dark">Join as a barber <span aria-hidden="true">→</span></Link><Link href="/auth/login" className="button-secondary">Sign in to your workspace</Link></div>
    </section>
    <section className="feature-grid">{features.map(([number, title, text]) => <article className="feature-card" key={number}><span className="feature-card-number">{number}</span><h2>{title}</h2><p>{text}</p></article>)}</section>
  </main>;
}
