import Link from 'next/link';

export default function Home() {
  return <>
    <main>
    <section className="home-hero">
      <div className="home-hero-inner">
        <div className="home-copy">
          <p className="eyebrow">Good cuts start with a good find</p>
          <h1 className="display-heading">Your next great barber is closer than you think.</h1>
          <p className="home-lede">Discover independent barbers, compare their services and prices, and request a time that works for you.</p>

          <form action="/search" method="get" className="home-search">
            <label className="field-label">Location
              <input name="location" placeholder="City or neighbourhood" className="field-control" />
            </label>
            <label className="field-label">Service or barber
              <input name="query" placeholder="A fade, a name…" className="field-control" />
            </label>
            <button type="submit" className="button-primary">Find a barber <span aria-hidden="true">→</span></button>
          </form>
          <div className="home-browse-row">
            <Link href="/search" className="text-link">Browse all barbers</Link>
            <Link href="/for-barbers" className="text-link">I’m a barber</Link>
          </div>
        </div>

        <div className="home-art" aria-label="Barbering helps you compare services and request an appointment">
          <div className="home-art-mark" aria-hidden="true">B.</div>
          <div className="home-art-copy">
            <p className="home-art-note">A more considered appointment</p>
            <p className="home-art-title">Know the service. See the price. Choose your time.</p>
          </div>
        </div>
      </div>
    </section>

    <section className="home-proof">
      <div className="page-wrap home-proof-grid">
        <article className="home-proof-item"><h2>Explore your area</h2><p>Search public barber profiles by name, location, and the service you have in mind.</p></article>
        <article className="home-proof-item"><h2>Know before you go</h2><p>Compare real service menus, prices, appointment lengths, and customer reviews.</p></article>
        <article className="home-proof-item"><h2>Keep it all together</h2><p>Request an appointment, save barbers, and manage your bookings from your account.</p></article>
      </div>
    </section>
    </main>

    <footer className="home-footer">
      <div className="home-footer-inner">
        <span>Barbering · Find your chair</span>
        <nav className="home-footer-links" aria-label="Footer navigation">
          <Link href="/about">About</Link><Link href="/contact">Contact</Link><Link href="/for-barbers">For barbers</Link>
        </nav>
      </div>
    </footer>
  </>;
}
