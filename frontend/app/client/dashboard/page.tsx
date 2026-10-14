'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { ApiError, cancelAppointment, fetchAvailability, fetchCustomerDashboard, removeFavourite, rescheduleAppointment, reviewAppointment, updateUserProfile } from '../../../lib/api';
import type { Appointment, Availability, CustomerDashboard } from '../../../lib/api';
import { useAuth } from '../../components/AuthProvider';

function displayTime(appointment: Appointment) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short', timeZone: appointment.timezoneId }).format(new Date(appointment.startAt));
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

function AppointmentCard({ appointment, onChange }: { appointment: Appointment; onChange: () => void }) {
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [rescheduling, setRescheduling] = useState(false);
  const [availability, setAvailability] = useState<Availability | null>(null);
  const [slot, setSlot] = useState('');
  const [rating, setRating] = useState(5);
  const [reviewText, setReviewText] = useState('');
  const [busy, setBusy] = useState(false);
  const moreThanDayAhead = Date.parse(appointment.startAt) - Date.now() > 24 * 60 * 60 * 1000;
  const canChange = moreThanDayAhead && ['PENDING', 'CONFIRMED'].includes(appointment.status);
  const completed = appointment.status === 'COMPLETED';

  async function cancel() {
    setBusy(true); setError(''); setMessage('');
    try { await cancelAppointment(appointment.id); setMessage('Appointment cancelled.'); onChange(); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'Could not cancel this appointment.'); }
    finally { setBusy(false); }
  }

  async function checkAvailability(value: string) {
    setAvailability(null); setSlot(''); setError('');
    if (!value) return;
    try { setAvailability(await fetchAvailability(appointment.barberId, appointment.serviceId, value)); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'Could not check availability.'); }
  }

  async function reschedule() {
    if (!slot) return;
    setBusy(true); setError(''); setMessage('');
    try { await rescheduleAppointment(appointment.id, slot, appointment.customerNote ?? undefined); setMessage('New time requested. The barber must confirm it.'); setRescheduling(false); onChange(); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'Could not reschedule this appointment.'); }
    finally { setBusy(false); }
  }

  async function submitReview() {
    setBusy(true); setError(''); setMessage('');
    try { await reviewAppointment(appointment.id, rating, reviewText || undefined); setMessage('Thanks for sharing your review.'); onChange(); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'Could not submit this review.'); }
    finally { setBusy(false); }
  }

  return <article className="appointment-card">
    <div className="flex flex-wrap items-start justify-between gap-3">
      <div><h3 className="font-semibold">{appointment.serviceName}</h3><p className="mt-1 text-sm text-neutral-600">with {appointment.barberName}</p><p className="mt-2 text-sm text-neutral-500">{displayTime(appointment)} · {appointment.durationMinutes} min · {appointment.timezoneId}</p></div>
      <span className={`status-chip status-${appointment.status.toLowerCase().replace('_', '-')}`}>{appointment.status.replace('_', ' ')}</span>
    </div>
    {appointment.customerNote && <p className="mt-3 text-sm text-neutral-600">Your note: {appointment.customerNote}</p>}
    {error && <p role="alert" className="mt-3 text-sm text-red-700">{error}</p>}
    {message && <p role="status" className="mt-3 text-sm text-green-700">{message}</p>}
    <div className="mt-4 flex flex-wrap gap-2">
      <Link href={`/barbers/${appointment.barberId}`} className="button-secondary !min-h-9 !px-3 !py-2 !text-xs">Barber profile</Link>
      {canChange && <button type="button" disabled={busy} onClick={cancel} className="button-secondary !min-h-9 !px-3 !py-2 !text-xs text-red-700 disabled:opacity-50">Cancel</button>}
      {canChange && <button type="button" disabled={busy} onClick={() => setRescheduling(!rescheduling)} className="button-secondary !min-h-9 !px-3 !py-2 !text-xs disabled:opacity-50">Reschedule</button>}
    </div>
    {rescheduling && <div className="mt-4 rounded-xl bg-neutral-50 p-4">
      <label className="block text-sm font-medium">Choose a new date<input type="date" min={dateValueInZone(new Date(), appointment.timezoneId)} max={addCalendarDays(dateValueInZone(new Date(), appointment.timezoneId), 90)} onChange={(event) => checkAvailability(event.target.value)} className="mt-1 w-full rounded-lg border bg-white px-3 py-2" /></label>
      {availability && (availability.slots.length ? <div className="mt-3 grid grid-cols-3 gap-2 sm:grid-cols-5">{availability.slots.map((time) => {
        const ambiguous = availability.slots.filter((candidate) => candidate.localStartTime === time.localStartTime).length > 1;
        const offset = time.startAt.match(/([+-]\d{2}:\d{2})$/)?.[1];
        return <button key={time.startAt} type="button" onClick={() => setSlot(time.startAt)} className={`rounded-lg border px-2 py-2 text-sm ${slot === time.startAt ? 'bg-neutral-900 text-white' : 'bg-white'}`}>{time.localStartTime.slice(0, 5)}{ambiguous && offset ? ` (${offset})` : ''}</button>;
      })}</div> : <p className="mt-3 text-sm text-neutral-500">No times are available on this date.</p>)}
      {slot && <button type="button" disabled={busy} onClick={reschedule} className="button-dark mt-4 disabled:opacity-50">{busy ? 'Requesting…' : 'Request new time'}</button>}
    </div>}
    {completed && !appointment.reviewed && <div className="mt-4 border-t pt-4">
      <h4 className="text-sm font-semibold">How was your visit?</h4>
      <div className="mt-2 flex flex-wrap items-center gap-3"><label className="text-sm">Rating <select value={rating} onChange={(event) => setRating(Number(event.target.value))} className="ml-2 rounded-lg border px-2 py-1">{[5, 4, 3, 2, 1].map((value) => <option key={value} value={value}>{value} star{value === 1 ? '' : 's'}</option>)}</select></label><input value={reviewText} maxLength={2000} onChange={(event) => setReviewText(event.target.value)} placeholder="Write a short review (optional)" className="min-w-56 flex-1 rounded-lg border px-3 py-2 text-sm" /><button type="button" disabled={busy} onClick={submitReview} className="rounded-lg bg-neutral-900 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">{busy ? 'Submitting…' : 'Submit review'}</button></div>
    </div>}
    {appointment.reviewed && <p className="mt-3 text-xs text-neutral-500">Review submitted</p>}
  </article>;
}

export default function ClientDashboardPage() {
  const [data, setData] = useState<CustomerDashboard | null>(null);
  const [error, setError] = useState('');
  const router = useRouter();
  const load = useCallback(() => {
    setError('');
    fetchCustomerDashboard().then(setData).catch((err: unknown) => {
      if (err instanceof ApiError && err.status === 401) router.replace('/auth/login');
      else setError(err instanceof Error ? err.message : 'Could not load your dashboard.');
    });
  }, [router]);
  useEffect(() => load(), [load]);

  async function unsave(barberId: number) {
    try { await removeFavourite(barberId); load(); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'Could not remove that favourite.'); }
  }

  if (error && !data) return <main className="workspace-page page-wrap"><p className="eyebrow">Customer account</p><h1 className="workspace-title">Your appointments.</h1><div role="alert" className="notice-error mt-5">{error === 'Failed to fetch' ? 'We could not reach your account right now. Check your connection and try again.' : error}</div><button type="button" className="button-dark mt-4" onClick={load}>Try again</button></main>;
  if (!data) return <main className="workspace-page page-wrap"><p className="text-sm text-neutral-500">Loading your appointments…</p></main>;

  return <main className="workspace-page page-wrap">
    <div className="workspace-header"><div><p className="eyebrow">Customer account</p><h1 className="workspace-title">Welcome, {data.user.displayName}.</h1><p className="workspace-subtitle">Your bookings and favourite barbers.</p></div><Link href="/search" className="button-dark">Find a barber <span aria-hidden="true">→</span></Link></div>
    {error && <p role="alert" className="notice-error mt-5">{error}</p>}
    <section className="mt-8 grid gap-3 sm:grid-cols-3"><div className="workspace-stat"><p className="workspace-stat-label">Upcoming</p><p className="workspace-stat-value">{data.upcoming.length}</p></div><div className="workspace-stat"><p className="workspace-stat-label">Awaiting barber</p><p className="workspace-stat-value">{data.pending.length}</p></div><div className="workspace-stat"><p className="workspace-stat-label">Favourite barbers</p><p className="workspace-stat-value">{data.favourites.length}</p></div></section>
    <section className="workspace-section"><h2 className="workspace-section-title">Upcoming appointments</h2><div className="mt-4 space-y-3">{data.upcoming.length ? data.upcoming.map((appointment) => <AppointmentCard key={appointment.id} appointment={appointment} onChange={load} />) : <p className="empty-state text-left text-sm text-neutral-600">No upcoming appointments yet. Find a barber and request your first time.</p>}</div></section>
    {data.pending.length > 0 && <section className="workspace-section"><h2 className="workspace-section-title">Waiting for confirmation</h2><div className="mt-4 space-y-3">{data.pending.map((appointment) => <AppointmentCard key={appointment.id} appointment={appointment} onChange={load} />)}</div></section>}
    <section id="favourites" className="workspace-section scroll-mt-24"><h2 className="workspace-section-title">Favourite barbers</h2>{data.favourites.length ? <div className="mt-4 grid gap-3 sm:grid-cols-2">{data.favourites.map((barber) => <article key={barber.barberId} className="workspace-row flex items-center justify-between gap-4"><div><Link href={`/barbers/${barber.barberId}`} className="font-semibold text-neutral-900 hover:underline">{barber.barberName}</Link><p className="mt-1 text-sm text-neutral-500">{barber.city || 'Location not added'} · ★ {barber.averageRating.toFixed(1)} ({barber.reviewCount})</p></div><button type="button" onClick={() => unsave(barber.barberId)} className="text-sm font-medium text-neutral-600 underline underline-offset-4">Remove</button></article>)}</div> : <p className="empty-state mt-4 text-left text-sm text-neutral-600">Save a barber from their profile to see them here.</p>}</section>
    <section className="workspace-section"><h2 className="workspace-section-title">Appointment history</h2><div className="mt-4 space-y-3">{data.past.length ? data.past.map((appointment) => <AppointmentCard key={appointment.id} appointment={appointment} onChange={load} />) : <p className="empty-state text-left text-sm text-neutral-600">Completed and cancelled visits will show here.</p>}</div></section>
    <section className="workspace-section"><h2 className="workspace-section-title">Your profile</h2><p className="mt-2 text-sm text-neutral-600">Account email: {data.user.email}</p><ProfileEditor displayName={data.user.displayName} onSaved={load} /></section>
  </main>;
}

function ProfileEditor({ displayName, onSaved }: { displayName: string; onSaved: () => void }) {
  const { setUser } = useAuth();
  const [name, setName] = useState(displayName);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  useEffect(() => setName(displayName), [displayName]);
  async function save() {
    setMessage(''); setError('');
    try { setUser(await updateUserProfile(name)); setMessage('Profile updated.'); onSaved(); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'Could not update your profile.'); }
  }
  return <div className="workspace-panel mt-4 flex max-w-xl flex-wrap items-end gap-3"><label className="field-label flex-1">Display name<input value={name} onChange={(event) => setName(event.target.value)} maxLength={100} className="field-control" /></label><button type="button" onClick={save} className="button-dark">Save</button>{message && <p role="status" className="w-full text-sm text-green-700">{message}</p>}{error && <p role="alert" className="w-full text-sm text-red-700">{error}</p>}</div>;
}
