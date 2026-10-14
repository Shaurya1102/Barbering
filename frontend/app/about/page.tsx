import Link from 'next/link';

export default function AboutPage() {
  return <main className="content-page">
    <section className="content-hero">
      <p className="eyebrow">About Barbering</p>
      <h1>Make finding the right barber feel simple.</h1>
      <p className="mt-6 max-w-2xl text-base leading-8 text-neutral-600">Barbering brings local barber profiles, service menus, reviews, and appointment requests together, so customers can make a more informed choice before they book.</p>
      <Link href="/search" className="button-dark mt-7">Explore barber profiles <span aria-hidden="true">→</span></Link>
    </section>
    <section className="feature-grid">
      <article className="feature-card"><span className="feature-card-number">01</span><h2>Clear choices</h2><p>Public profiles help you review a barber’s location, listed services, prices, and customer feedback.</p></article>
      <article className="feature-card"><span className="feature-card-number">02</span><h2>Appointments with context</h2><p>Choose a listed service and request from the availability shown for that barber.</p></article>
      <article className="feature-card"><span className="feature-card-number">03</span><h2>A place to return</h2><p>Customers can keep favourites and manage appointment requests from their account.</p></article>
    </section>
  </main>;
}
