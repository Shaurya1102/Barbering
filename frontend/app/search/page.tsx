'use client';

import Image from 'next/image';
import Link from 'next/link';
import { Suspense, useEffect, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { API_URL, ApiError, searchBarbers } from '../../lib/api';
import type { BarberSearchPage } from '../../lib/api';

function Results() {
  const params = useSearchParams();
  const query = params.get('query') ?? '';
  const location = params.get('location') ?? '';
  const service = params.get('service') ?? '';
  const page = Number(params.get('page') ?? 0) || 0;
  const [results, setResults] = useState<BarberSearchPage | null>(null);
  const [error, setError] = useState('');
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setResults(null);
    setError('');
    searchBarbers({ query, location, service, page, size: 12 })
      .then((data) => { if (!cancelled) setResults(data); })
      .catch((err: unknown) => { if (!cancelled) setError(err instanceof ApiError ? err.message : 'Could not load barbers. Please try again.'); });
    return () => { cancelled = true; };
  }, [query, location, service, page, attempt]);

  const pageHref = (target: number) => {
    const next = new URLSearchParams(params.toString());
    next.set('page', String(target));
    return `/search?${next.toString()}`;
  };

  return <main className="search-page">
    <div className="search-head">
      <div>
        <p className="eyebrow">Find your chair</p>
        <h1>Barbers worth the visit.</h1>
        <p className="mt-3 max-w-2xl text-sm leading-7 text-neutral-600">Explore local profiles, compare customer feedback, and find the right fit for your next appointment.</p>
      </div>
      {results && <p className="result-count" aria-live="polite">{results.totalItems} {results.totalItems === 1 ? 'barber' : 'barbers'} found</p>}
    </div>

    <form action="/search" method="get" className="search-form">
      <label className="field-label">Barber or style<input name="query" defaultValue={query} placeholder="Name or haircut" className="field-control" /></label>
      <label className="field-label">Location<input name="location" defaultValue={location} placeholder="City or area" className="field-control" /></label>
      <label className="field-label">Service<input name="service" defaultValue={service} placeholder="Fade, beard trim…" className="field-control" /></label>
      <button type="submit" className="button-primary">Search <span aria-hidden="true">→</span></button>
    </form>

    {error && <div role="alert" className="mt-7 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"><p>{error}</p><button type="button" className="button-secondary !min-h-9 !px-4 !py-2" onClick={() => setAttempt((value) => value + 1)}>Try again</button></div>}
    {!error && !results && <div aria-label="Loading barber profiles" className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">{[0, 1, 2].map((item) => <div key={item} className="overflow-hidden rounded-2xl border border-neutral-200 bg-white"><div className="h-56 animate-pulse bg-[#e8e2d9]" /><div className="space-y-3 p-5"><div className="h-5 w-2/3 animate-pulse rounded bg-[#e8e2d9]" /><div className="h-3 w-1/2 animate-pulse rounded bg-[#eee9e2]" /><div className="h-10 animate-pulse rounded bg-[#eee9e2]" /></div></div>)}</div>}
    {!error && results?.items.length === 0 && <div className="empty-state mt-8"><span className="mx-auto grid h-12 w-12 place-items-center rounded-full bg-[#eee8df] font-serif text-xl text-[#805535]" aria-hidden="true">B</span><h2 className="mt-4 font-serif text-2xl text-neutral-900">No barbers found just yet.</h2><p className="mx-auto mt-2 max-w-md text-sm leading-6 text-neutral-600">Try a nearby area or a broader service search. You can also clear the filters and browse every profile.</p><Link className="button-secondary mt-5" href="/search">Clear filters</Link></div>}
    {results && results.items.length > 0 && <div className="mt-7 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      {results.items.map((barber) => <Link key={barber.id} href={`/barbers/${barber.id}`} className="search-card group">
        <div className="search-card-image">
          {barber.photos[0] ? <Image src={`${API_URL}${barber.photos[0].url}`} alt={barber.photos[0].altText ?? `${barber.displayName}'s portfolio`} fill unoptimized className="object-cover" /> : <div className="search-card-placeholder" aria-hidden="true">{barber.displayName.trim().charAt(0).toUpperCase() || 'B'}</div>}
        </div>
        <div className="search-card-body">
          <div className="search-card-meta"><h2 className="search-card-name">{barber.displayName}</h2><span className="rating-chip" aria-label={`${barber.averageRating.toFixed(1)} out of 5 from ${barber.reviewCount} reviews`}>{barber.reviewCount > 0 ? `★ ${barber.averageRating.toFixed(1)} · ${barber.reviewCount}` : 'New profile'}</span></div>
          <p className="search-card-location">{[barber.city, barber.region, barber.country].filter(Boolean).join(', ') || 'Location not added'}</p>
          <p className="search-card-bio">{barber.bio || 'Explore this barber’s profile for services, availability, and more.'}</p>
          <span className="search-card-cta">View profile and services <span aria-hidden="true">↗</span></span>
        </div>
      </Link>)}
    </div>}

    {results && results.totalPages > 1 && <nav aria-label="Search results pages" className="mt-9 flex items-center justify-center gap-3 text-sm">
      {page > 0 ? <Link className="button-secondary !min-h-10 !py-2" href={pageHref(page - 1)}>Previous</Link> : <span aria-hidden="true" />}
      <span className="px-2 text-neutral-500">Page {page + 1} of {results.totalPages}</span>
      {page + 1 < results.totalPages ? <Link className="button-secondary !min-h-10 !py-2" href={pageHref(page + 1)}>Next</Link> : <span aria-hidden="true" />}
    </nav>}
  </main>;
}

export default function SearchPage() {
  return <Suspense fallback={<main className="search-page"><p className="eyebrow">Find your chair</p><h1 className="mt-3 font-serif text-4xl">Loading discovery…</h1></main>}><Results /></Suspense>;
}
