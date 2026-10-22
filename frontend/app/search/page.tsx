'use client';

import Image from 'next/image';
import Link from 'next/link';
import { Suspense, useEffect, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import {
  API_URL, ApiError, approveMyGoogleClaim, fetchMe, fetchMyGoogleClaim, rejectMyGoogleClaim,
  requestGoogleClaim, searchBarbers, searchNearbyBarbers,
} from '../../lib/api';
import type { BarberSearchPage, DiscoveryListing, DiscoveryResult, PlaceClaim, User } from '../../lib/api';

const RADIUS_OPTIONS = [5, 10, 25, 50];

function distanceLabel(km: number): string {
  return km < 1 ? `${Math.round(km * 1000)} m away` : `${km.toFixed(1)} km away`;
}

function freshnessLabel(reviewFreshness?: string | null, lastReviewAt?: string | null): string {
  if (reviewFreshness === 'FRESH') return 'recent reviews';
  if (reviewFreshness === 'STALE') return 'no recent reviews';
  if (reviewFreshness === 'OUTDATED') return 'review activity is dated — verify before visiting';
  if (!lastReviewAt) return 'no reliable review date';
  const months = (Date.now() - new Date(lastReviewAt).getTime()) / (1000 * 60 * 60 * 24 * 30.44);
  if (months <= 3) return 'recent reviews';
  if (months <= 8) return `${Math.max(1, Math.round(months))} months ago`;
  return 'older reviews';
}

function formatDate(value: string | null): string {
  if (!value) return '—';
  return new Date(value).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

const GOOGLE_STATUS_MESSAGES: Record<DiscoveryResult['googleStatus'], string> = {
  ok: '',
  unavailable: 'Google Maps is not configured on this server — showing member profiles only.',
  timeout: 'Google Maps timed out — showing member profiles only. Try again in a moment.',
  quota_or_authorization: 'Google Maps rejected the request (quota or authorization) — showing member profiles only.',
  bad_response: 'Google Maps returned an unexpected response — showing member profiles only.',
};

function ListingChips({ listing, showOpenNow = false }: { listing: DiscoveryListing; showOpenNow?: boolean }) {
  return <div className="mt-2 flex flex-wrap gap-1.5 text-xs">
    <span className="rating-chip">{distanceLabel(listing.distanceKm)}</span>
    {showOpenNow && listing.openNow != null ? <span className="rating-chip">{listing.openNow ? 'Open now' : 'Closed right now'}</span> : null}
    {listing.rating != null && listing.reviewCount != null
      ? <span className="rating-chip" aria-label={`Rated ${listing.rating.toFixed(1)} out of 5 from ${listing.reviewCount} reviews`}>★ {listing.rating.toFixed(1)} · {listing.reviewCount}</span>
      : null}
    <span className="rating-chip">{freshnessLabel(listing.reviewFreshness, listing.lastReviewAt)}</span>
  </div>;
}

function MemberListingCard({ listing }: { listing: DiscoveryListing }) {
  const initial = listing.name.trim().charAt(0).toUpperCase() || 'B';
  return (
    <Link href={listing.barberId ? `/barbers/${listing.barberId}` : '/search'} className="search-card group">
      <div className="search-card-image"><div className="search-card-placeholder" aria-hidden="true">{initial}</div></div>
      <div className="search-card-body">
        <div className="search-card-meta"><h2 className="search-card-name">{listing.name}</h2></div>
        <ListingChips listing={listing} />
        <p className="search-card-location">{listing.address || 'Location not added'}</p>
        <span className="search-card-cta">View profile and services <span aria-hidden="true">↗</span></span>
      </div>
    </Link>
  );
}

function GoogleListingCard({ listing, canClaim, claim, claimBusy, onClaim }: {
  listing: DiscoveryListing;
  canClaim: boolean;
  claim: PlaceClaim | null;
  claimBusy: boolean;
  onClaim: (placeId: string) => void;
}) {
  const initial = listing.name.trim().charAt(0).toUpperCase() || 'G';
  const alreadyClaimedHere = claim != null && claim.googlePlaceId === listing.placeId;
  return (
    <div className="search-card group">
      <div className="search-card-image"><div className="search-card-placeholder" aria-hidden="true">{initial}</div></div>
      <div className="search-card-body">
        <p className="eyebrow">Google Maps listing</p>
        <div className="search-card-meta"><h2 className="search-card-name">{listing.name}</h2></div>
        <ListingChips listing={listing} showOpenNow />
        <p className="search-card-location">{listing.address || 'Address not available'}</p>
        {(listing.websiteUri || listing.phone) && (
          <p className="mt-1 flex flex-wrap gap-x-3 text-xs text-neutral-600">
            {listing.websiteUri ? <a href={listing.websiteUri} target="_blank" rel="noopener noreferrer">Website ↗</a> : null}
            {listing.phone ? <span>{listing.phone}</span> : null}
          </p>
        )}
        <div className="mt-3 flex flex-wrap items-center gap-2 text-sm">
          {listing.mapsUri ? <a className="button-secondary !min-h-9 !px-4 !py-2" href={listing.mapsUri} target="_blank" rel="noopener noreferrer">Open in Google Maps <span aria-hidden="true">↗</span></a> : null}
          {listing.directionsUri ? <a className="button-secondary !min-h-9 !px-4 !py-2" href={listing.directionsUri} target="_blank" rel="noopener noreferrer">Directions <span aria-hidden="true">↗</span></a> : null}
          {canClaim && listing.placeId ? (
            alreadyClaimedHere
              ? <span className="text-xs text-neutral-500">Claim status: {claim?.status}</span>
              : <button type="button" className="button-primary !min-h-9 !px-4 !py-2" disabled={claimBusy} onClick={() => onClaim(listing.placeId as string)}>Claim this listing</button>
          ) : null}
        </div>
      </div>
    </div>
  );
}

function ClaimPanel({ me, claim, claimBusy, claimMessage, onApprove, onReject }: {
  me: User | null;
  claim: PlaceClaim | null;
  claimBusy: boolean;
  claimMessage: string;
  onApprove: () => void;
  onReject: () => void;
}) {
  if (me?.role !== 'BARBER') return null;
  return (
    <section aria-label="Your Google place claim" className="mt-8 rounded-2xl border border-neutral-200 bg-white p-5">
      <h3 className="font-serif text-lg text-neutral-900">Your Google place claim</h3>
      {claim ? (
        <div className="mt-3 space-y-2 text-sm text-neutral-700">
          <p><span className="text-neutral-500">Place ID:</span> <code>{claim.googlePlaceId}</code></p>
          <p><span className="text-neutral-500">Status:</span> {claim.status}</p>
          <p><span className="text-neutral-500">Requested:</span> {formatDate(claim.requestedAt)}</p>
          {claim.reviewedAt ? <p><span className="text-neutral-500">Reviewed:</span> {formatDate(claim.reviewedAt)}</p> : null}
          {claim.status === 'PENDING' && (
            <div className="flex flex-wrap gap-2 pt-1">
              <button type="button" className="button-primary !min-h-9 !px-4 !py-2" disabled={claimBusy} onClick={onApprove}>Approve claim</button>
              <button type="button" className="button-secondary !min-h-9 !px-4 !py-2" disabled={claimBusy} onClick={onReject}>Reject claim</button>
            </div>
          )}
        </div>
      ) : (
        <p className="mt-3 text-sm leading-6 text-neutral-600">You have not claimed a Google place yet. Use “Claim this listing” on a nearby Google Maps result to start.</p>
      )}
      {claimMessage && <p role="status" className="mt-3 text-sm text-neutral-700">{claimMessage}</p>}
    </section>
  );
}

function Results() {
  const params = useSearchParams();
  const query = params.get('query') ?? '';
  const location = params.get('location') ?? '';
  const service = params.get('service') ?? '';
  const page = Number(params.get('page') ?? 0) || 0;

  const [results, setResults] = useState<BarberSearchPage | null>(null);
  const [error, setError] = useState('');
  const [attempt, setAttempt] = useState(0);

  // Near-me discovery state (consent-gated geolocation).
  const [me, setMe] = useState<User | null>(null);
  const [geoState, setGeoState] = useState<'idle' | 'locating' | 'granted' | 'denied'>('idle');
  const [coords, setCoords] = useState<{ lat: number; lng: number } | null>(null);
  const [radiusKm, setRadiusKm] = useState(25);
  const [minRating, setMinRating] = useState('');
  const [openNow, setOpenNow] = useState(false);
  const [nearbyResult, setNearbyResult] = useState<DiscoveryResult | null>(null);
  const [nearbyError, setNearbyError] = useState('');

  // Claim state (BARBER only).
  const [claim, setClaim] = useState<PlaceClaim | null>(null);
  const [claimBusy, setClaimBusy] = useState(false);
  const [claimMessage, setClaimMessage] = useState('');

  useEffect(() => { fetchMe().then(setMe).catch(() => setMe(null)); }, []);

  useEffect(() => {
    let cancelled = false;
    setResults(null);
    setError('');
    searchBarbers({ query, location, service, page, size: 12 })
      .then((data) => { if (!cancelled) setResults(data); })
      .catch((err: unknown) => { if (!cancelled) setError(err instanceof ApiError ? err.message : 'Could not load barbers. Please try again.'); });
    return () => { cancelled = true; };
  }, [query, location, service, page, attempt]);

  useEffect(() => {
    if (me?.role === 'BARBER') fetchMyGoogleClaim().then(setClaim).catch(() => setClaim(null));
  }, [me]);

  const nearbyLoading = coords != null && nearbyResult == null && nearbyError === '';

  useEffect(() => {
    if (!coords) return;
    let cancelled = false;
    setNearbyResult(null);
    setNearbyError('');
    searchNearbyBarbers({ lat: coords.lat, lng: coords.lng, radiusKm, minRating: minRating === '' ? undefined : Number(minRating), openNow })
      .then((data) => { if (!cancelled) setNearbyResult(data); })
      .catch((err: unknown) => { if (!cancelled) setNearbyError(err instanceof ApiError ? err.message : 'Could not load nearby listings. Please try again.'); });
    return () => { cancelled = true; };
  }, [coords, radiusKm, minRating, openNow]);

  const locate = () => {
    if (!('geolocation' in navigator)) { setGeoState('denied'); setNearbyError("Your browser doesn't support location sharing."); return; }
    setGeoState('locating');
    setNearbyError('');
    navigator.geolocation.getCurrentPosition(
      (position) => { setCoords({ lat: position.coords.latitude, lng: position.coords.longitude }); setGeoState('granted'); },
      () => { setGeoState('denied'); setNearbyError("We couldn't get your location. Check your browser's location permission and try again."); },
      { enableHighAccuracy: false, timeout: 10000, maximumAge: 60000 },
    );
  };

  const claimListing = (placeId: string) => {
    setClaimBusy(true);
    setClaimMessage('');
    requestGoogleClaim(placeId)
      .then((next) => { setClaim(next); setClaimMessage('Claim requested — approve or reject it in the panel below.'); })
      .catch((err: unknown) => { setClaimMessage(err instanceof ApiError ? err.message : 'Could not submit the claim. Please try again.'); })
      .finally(() => { setClaimBusy(false); });
  };

  const reviewClaim = (action: 'approve' | 'reject') => {
    if (!claim) return;
    setClaimBusy(true);
    setClaimMessage('');
    const call = action === 'approve' ? approveMyGoogleClaim(claim.id) : rejectMyGoogleClaim(claim.id);
    call
      .then((next) => { setClaim(next); setClaimMessage(action === 'approve' ? 'Claim approved — your profile now points to this Google place.' : 'Claim rejected.'); })
      .catch((err: unknown) => { setClaimMessage(err instanceof ApiError ? err.message : 'Could not update the claim. Please try again.'); })
      .finally(() => { setClaimBusy(false); });
  };

  const pageHref = (target: number) => {
    const next = new URLSearchParams(params.toString());
    next.set('page', String(target));
    return `/search?${next.toString()}`;
  };

  const showNearby = coords != null;

  return <main className="search-page">
    <div className="search-head">
      <div>
        <p className="eyebrow">Find your chair</p>
        <h1>Barbers worth the visit.</h1>
        <p className="mt-3 max-w-2xl text-sm leading-7 text-neutral-600">Explore local profiles, compare customer feedback, and find the right fit for your next appointment.</p>
      </div>
      {!showNearby && results ? <p className="result-count" aria-live="polite">{results.totalItems} {results.totalItems === 1 ? 'barber' : 'barbers'} found</p> : null}
    </div>

    <form action="/search" method="get" className="search-form">
      <label className="field-label">Barber or style<input name="query" defaultValue={query} placeholder="Name or haircut" className="field-control" /></label>
      <label className="field-label">Location<input name="location" defaultValue={location} placeholder="City or area" className="field-control" /></label>
      <label className="field-label">Service<input name="service" defaultValue={service} placeholder="Fade, beard trim…" className="field-control" /></label>
      <button type="submit" className="button-primary">Search <span aria-hidden="true">→</span></button>
    </form>

    <section aria-label="Nearby discovery" className="mt-7 rounded-2xl border border-neutral-200 bg-white p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="font-serif text-xl text-neutral-900">Near me</h2>
          <p className="mt-1 max-w-xl text-sm leading-6 text-neutral-600">Share your location to see nearby barbers and Google Maps listings, sorted by distance. We only ask when you click — nothing is sent until then.</p>
        </div>
        {geoState === 'idle' && <button type="button" className="button-primary" onClick={locate}>Use my location</button>}
        {geoState === 'locating' && <span className="text-sm text-neutral-500">Finding your location…</span>}
      </div>

      {showNearby && (
        <div className="mt-4 flex flex-wrap items-end gap-3">
          <label className="field-label">Radius<select value={radiusKm} onChange={(event) => setRadiusKm(Number(event.target.value))} className="field-control">{RADIUS_OPTIONS.map((value) => <option key={value} value={value}>{value} km</option>)}</select></label>
          <label className="field-label">Minimum rating<select value={minRating} onChange={(event) => setMinRating(event.target.value)} className="field-control"><option value="">Any</option><option value="3">3+</option><option value="4">4+</option></select></label>
          <label className="flex items-center gap-2 pb-2 text-sm text-neutral-700"><input type="checkbox" checked={openNow} onChange={(event) => setOpenNow(event.target.checked)} />Open now</label>
        </div>
      )}

      {nearbyError && <p role="alert" className="mt-3 text-sm text-red-700">{nearbyError}</p>}
      {showNearby && nearbyResult ? (
        <>
          {!nearbyResult.googleAvailable && GOOGLE_STATUS_MESSAGES[nearbyResult.googleStatus] !== '' && (
            <p role="status" className="mt-3 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-800">{GOOGLE_STATUS_MESSAGES[nearbyResult.googleStatus]}</p>
          )}
          {nearbyLoading ? null : nearbyResult.listings.length === 0 ? (
            <div className="empty-state mt-6"><span className="mx-auto grid h-12 w-12 place-items-center rounded-full bg-[#eee8df] font-serif text-xl text-[#805535]" aria-hidden="true">B</span><h3 className="mt-4 font-serif text-2xl text-neutral-900">Nothing nearby just yet.</h3><p className="mx-auto mt-2 max-w-md text-sm leading-6 text-neutral-600">No member profiles or Google listings matched within {radiusKm} km. Try a larger radius or fewer filters.</p></div>
          ) : (
            <div className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {nearbyResult.listings.map((listing, index) => listing.source === 'MEMBER'
                ? <MemberListingCard key={`member-${index}`} listing={listing} />
                : <GoogleListingCard key={`google-${index}`} listing={listing} canClaim={me?.role === 'BARBER'} claim={claim} claimBusy={claimBusy} onClaim={claimListing} />)}
            </div>
          )}
          {nearbyResult.googleAttribution && (
            <p className="mt-3 text-center text-xs text-neutral-400">{nearbyResult.googleAttribution}</p>
          )}
        </>
      ) : null}
      {showNearby && nearbyLoading && <div aria-label="Loading nearby listings" className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">{[0, 1, 2].map((item) => <div key={item} className="overflow-hidden rounded-2xl border border-neutral-200 bg-white"><div className="h-40 animate-pulse bg-[#e8e2d9]" /><div className="space-y-3 p-5"><div className="h-5 w-2/3 animate-pulse rounded bg-[#e8e2d9]" /><div className="h-3 w-1/2 animate-pulse rounded bg-[#eee9e2]" /></div></div>)}</div>}
    </section>

    {showNearby ? (
      <ClaimPanel me={me} claim={claim} claimBusy={claimBusy} claimMessage={claimMessage} onApprove={() => reviewClaim('approve')} onReject={() => reviewClaim('reject')} />
    ) : (
      <>
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

        <p className="mt-9 text-center text-xs text-neutral-400">Nearby results include data from the Google Places API (New). © Google</p>
      </>
    )}
  </main>;
}

export default function SearchPage() {
  return <Suspense fallback={<main className="search-page"><p className="eyebrow">Find your chair</p><h1 className="mt-3 font-serif text-4xl">Loading discovery…</h1></main>}><Results /></Suspense>;
}
