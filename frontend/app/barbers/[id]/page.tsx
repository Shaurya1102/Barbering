'use client';

import Image from 'next/image';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { API_URL, addFavourite, ApiError, createAppointment, fetchAvailability, fetchBarber, fetchBarberReviews, fetchBarberServices } from '../../../lib/api';
import type { Availability, Barber, Offering, Review } from '../../../lib/api';
import { useAuth } from '../../components/AuthProvider';

function localDateValue(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function dateValueInZone(date: Date, timeZone: string) {
  const values = new Intl.DateTimeFormat('en-US', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(date);
  const part = (type: Intl.DateTimeFormatPartTypes) => values.find((value) => value.type === type)?.value ?? '';
  return `${part('year')}-${part('month')}-${part('day')}`;
}

function addCalendarDays(value: string, daysToAdd: number) {
  const [year, month, day] = value.split('-').map(Number);
  const date = new Date(Date.UTC(year, month - 1, day + daysToAdd));
  return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}-${String(date.getUTCDate()).padStart(2, '0')}`;
}

function formatPrice(value: number) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(value);
}

export default function BarberProfilePage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const { user } = useAuth();
  const barberId = Number(params.id);
  const [barber, setBarber] = useState<Barber | null>(null);
  const [services, setServices] = useState<Offering[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [serviceId, setServiceId] = useState<number | null>(null);
  const [date, setDate] = useState(() => localDateValue(new Date()));
  const [availability, setAvailability] = useState<Availability | null>(null);
  const [loading, setLoading] = useState(true);
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [selectedSlot, setSelectedSlot] = useState('');
  const [booking, setBooking] = useState(false);

  useEffect(() => {
    let cancelled = false;
    if (!Number.isFinite(barberId)) { setError('This barber could not be found.'); setLoading(false); return; }
    Promise.all([fetchBarber(barberId), fetchBarberServices(barberId), fetchBarberReviews(barberId)])
      .then(([profile, offerings, barberReviews]) => {
        if (cancelled) return;
        setBarber(profile); setServices(offerings); setReviews(barberReviews);
        setDate(dateValueInZone(new Date(), profile.timezoneId));
        setServiceId(offerings[0]?.id ?? null);
      })
      .catch((err: unknown) => { if (!cancelled) setError(err instanceof ApiError ? err.message : 'Could not load this barber.'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [barberId]);

  const loadAvailability = useCallback(() => {
    if (!serviceId || !date) { setAvailability(null); return; }
    let cancelled = false;
    setSlotsLoading(true); setAvailability(null); setSelectedSlot(''); setError('');
    fetchAvailability(barberId, serviceId, date)
      .then((data) => { if (!cancelled) setAvailability(data); })
      .catch((err: unknown) => { if (!cancelled) setError(err instanceof ApiError ? err.message : 'Could not load availability.'); })
      .finally(() => { if (!cancelled) setSlotsLoading(false); });
    return () => { cancelled = true; };
  }, [barberId, serviceId, date]);

  useEffect(() => loadAvailability(), [loadAvailability]);

  const selectedService = useMemo(() => services.find((service) => service.id === serviceId) ?? null, [services, serviceId]);
  const minDate = barber ? dateValueInZone(new Date(), barber.timezoneId) : localDateValue(new Date());
  const maxDate = addCalendarDays(minDate, 90);

  async function book() {
    if (booking || !selectedSlot || !selectedService) return;
    setBooking(true);
    setError(''); setNotice('');
    try {
      const appointment = await createAppointment({ barberId, serviceId: selectedService.id, startAt: selectedSlot });
      router.push(`/booking/confirm/${appointment.id}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not place the booking request.');
      loadAvailability();
    } finally { setBooking(false); }
  }

  async function favourite() {
    if (!barber) return;
    setError(''); setNotice('');
    try { await addFavourite(barber.id); setNotice('Saved to your favourites.'); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'Could not save this barber.'); }
  }

  if (loading) return <div className="mx-auto max-w-5xl px-5 py-12 text-neutral-500">Loading barber profile…</div>;
  if (error && !barber) return <div role="alert" className="mx-auto max-w-5xl px-5 py-12 text-red-700">{error}</div>;
  if (!barber) return null;

  return (
    <main className="profile-page page-wrap">
      {error && <p role="alert" className="mb-5 rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      {notice && <p role="status" className="mb-5 rounded-xl bg-green-50 p-3 text-sm text-green-800">{notice}</p>}
      <div className="profile-grid lg:grid-cols-[1.4fr_.8fr]">
        <section>
          <div className="profile-gallery">
            {barber.photos[0] ? <Image src={`${API_URL}${barber.photos[0].url}`} alt={barber.photos[0].altText ?? `${barber.displayName}'s shop`} fill unoptimized className="object-cover" /> : <div className="search-card-placeholder" aria-label={`No photo uploaded for ${barber.displayName}`}>{barber.displayName.trim().charAt(0).toUpperCase() || 'B'}</div>}
          </div>
          {barber.photos.length > 1 && <div className="mt-3 grid grid-cols-4 gap-3">{barber.photos.slice(1).map((photo) => <div key={photo.id} className="relative h-20 overflow-hidden rounded-xl bg-neutral-100"><Image src={`${API_URL}${photo.url}`} alt={photo.altText ?? `${barber.displayName}'s work`} fill unoptimized className="object-cover" /></div>)}</div>}
          <div className="profile-title-row">
            <div><p className="eyebrow">Barber profile</p><h1 className="profile-name">{barber.displayName}</h1><p className="profile-location">{[barber.streetAddress, barber.city, barber.region, barber.postalCode, barber.country].filter(Boolean).join(', ') || 'Location not added'}</p></div>
            <p className="rounded-full bg-amber-50 px-4 py-2 text-sm text-amber-900">{barber.reviewCount ? `★ ${barber.averageRating.toFixed(1)} · ${barber.reviewCount} reviews` : 'New profile'}</p>
          </div>
          {barber.bio && <p className="mt-5 whitespace-pre-wrap leading-7 text-neutral-700">{barber.bio}</p>}
          {user?.role === 'CUSTOMER' && <button type="button" onClick={favourite} className="mt-5 rounded-full border px-4 py-2 text-sm font-medium hover:bg-neutral-50">♡ Save barber</button>}

          <section className="profile-section">
            <h2 className="profile-section-title">Services</h2>
            {services.length === 0 ? <div className="empty-state mt-4 text-left"><p className="text-sm text-neutral-600">This barber has not listed any services yet.</p></div> : <div className="service-list">{services.map((service) => <button type="button" key={service.id} aria-pressed={service.id === serviceId} onClick={() => setServiceId(service.id)} className="service-choice"><span><span className="block font-semibold text-neutral-900">{service.name}</span>{service.description && <span className="mt-1 block text-sm leading-6 text-neutral-600">{service.description}</span>}<span className="mt-1.5 block text-xs text-neutral-500">{service.durationMinutes} minutes</span></span><span className="whitespace-nowrap font-semibold text-neutral-900">{formatPrice(service.priceAmount)}</span></button>)}</div>}
          </section>

          <section className="profile-section">
            <h2 className="profile-section-title">From customers</h2>
            {reviews.length === 0 ? <p className="mt-3 text-sm text-neutral-600">No reviews yet.</p> : <div className="mt-4 space-y-3">{reviews.map((review) => <article key={review.id} className="review-card"><div className="flex justify-between gap-3"><p className="font-semibold">{review.customerName}</p><p className="whitespace-nowrap text-sm text-amber-800">{'★'.repeat(review.rating)}<span className="text-neutral-300">{'★'.repeat(5 - review.rating)}</span></p></div>{review.reviewText && <p className="mt-3 text-sm leading-6 text-neutral-700">{review.reviewText}</p>}<p className="mt-3 text-xs text-neutral-400">{new Date(review.createdAt).toLocaleDateString()}</p></article>)}</div>}
          </section>
        </section>

        <aside className="booking-card">
          <h2 className="booking-card-title">Request a time</h2>
          <p className="mt-1 text-sm text-neutral-500">Times shown in {barber.timezoneId}.</p>
          {!user && <p className="mt-4 rounded-xl bg-neutral-50 p-3 text-sm text-neutral-600">Sign in as a customer to request a time. The barber will confirm your request.</p>}
          {user?.role === 'BARBER' && <p className="mt-4 rounded-xl bg-neutral-50 p-3 text-sm text-neutral-600">Customer accounts can request appointments.</p>}
          <label className="mt-5 block text-sm font-medium">Service<select value={serviceId ?? ''} onChange={(event) => setServiceId(Number(event.target.value) || null)} className="mt-1 w-full rounded-xl border px-3 py-2.5" disabled={!services.length}><option value="">Choose service</option>{services.map((service) => <option key={service.id} value={service.id}>{service.name} · {formatPrice(service.priceAmount)} · {service.durationMinutes} min</option>)}</select></label>
          <label className="mt-4 block text-sm font-medium">Date<input type="date" value={date} min={minDate} max={maxDate} onChange={(event) => setDate(event.target.value)} className="mt-1 w-full rounded-xl border px-3 py-2.5" /></label>
          <div className="mt-5"><div className="flex items-center justify-between"><h3 className="text-sm font-semibold">Available times</h3>{selectedService && <span className="text-xs text-neutral-500">{selectedService.durationMinutes} min</span>}</div>
            {slotsLoading && <p className="mt-3 text-sm text-neutral-500">Checking availability…</p>}
            {!slotsLoading && availability?.slots.length === 0 && <p className="mt-3 text-sm text-neutral-500">No times are available for this date.</p>}
            {!slotsLoading && availability && availability.slots.length > 0 && <div className="slot-grid mt-3">{availability.slots.map((slot) => {
              const ambiguous = availability.slots.filter((candidate) => candidate.localStartTime === slot.localStartTime).length > 1;
              const offset = slot.startAt.match(/([+-]\d{2}:\d{2})$/)?.[1];
              return <button key={slot.startAt} type="button" aria-pressed={selectedSlot === slot.startAt} onClick={() => setSelectedSlot(slot.startAt)} className="slot-button">{slot.localStartTime.slice(0, 5)}{ambiguous && offset ? ` (${offset})` : ''}</button>;
            })}</div>}
          </div>
          {user?.role === 'CUSTOMER' ? <button type="button" disabled={!selectedSlot || booking} onClick={book} className="mt-6 w-full rounded-xl bg-neutral-950 px-4 py-3 font-semibold text-white disabled:cursor-not-allowed disabled:bg-neutral-300">{booking ? 'Sending request…' : 'Request this time'}</button> : !user ? <Link href="/auth/login" className="mt-6 block rounded-xl bg-neutral-950 px-4 py-3 text-center font-semibold text-white">Log in to book</Link> : null}
          <p className="mt-3 text-xs leading-5 text-neutral-500">Your request is pending until the barber accepts it.</p>
        </aside>
      </div>
    </main>
  );
}
