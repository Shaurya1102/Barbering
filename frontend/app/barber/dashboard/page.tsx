'use client';

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { ApiError, cancelBarberAppointment, fetchAvailability, fetchBarberDashboard, fetchMyBarberAppointments, rescheduleBarberAppointment, transitionAppointment } from '../../../lib/api';
import type { Appointment, Availability, BarberDashboard } from '../../../lib/api';

function dateInZone(date: Date, timeZone: string) {
  const values = new Intl.DateTimeFormat('en-US', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(date);
  const part = (type: Intl.DateTimeFormatPartTypes) => values.find((value) => value.type === type)?.value ?? '';
  return `${part('year')}-${part('month')}-${part('day')}`;
}

function addDays(value: string, days: number) {
  const [year, month, day] = value.split('-').map(Number);
  const date = new Date(Date.UTC(year, month - 1, day + days));
  return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}-${String(date.getUTCDate()).padStart(2, '0')}`;
}

function AppointmentRow({ appointment, onChange }: { appointment: Appointment; onChange: () => void }) {
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [rescheduling, setRescheduling] = useState(false);
  const [availability, setAvailability] = useState<Availability | null>(null);
  const [slot, setSlot] = useState('');
  const displayTime = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short', timeZone: appointment.timezoneId }).format(new Date(appointment.startAt));
  async function act(status: 'CONFIRMED' | 'REJECTED' | 'COMPLETED' | 'NO_SHOW') {
    setBusy(true); setError(''); setMessage('');
    try { await transitionAppointment(appointment.id, status); setMessage(status === 'CONFIRMED' ? 'Appointment confirmed.' : 'Appointment updated.'); onChange(); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'Could not update this appointment.'); }
    finally { setBusy(false); }
  }
  async function cancel() {
    if (!window.confirm('Cancel this confirmed appointment?')) return;
    setBusy(true); setError(''); setMessage('');
    try { await cancelBarberAppointment(appointment.id); setMessage('Appointment cancelled.'); onChange(); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'Could not cancel this appointment.'); }
    finally { setBusy(false); }
  }
  async function checkAvailability(date: string) {
    setAvailability(null); setSlot(''); setError('');
    if (!date) return;
    try { setAvailability(await fetchAvailability(appointment.barberId, appointment.serviceId, date)); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'Could not check availability.'); }
  }
  async function reschedule() {
    if (!slot || busy) return;
    setBusy(true); setError(''); setMessage('');
    try { await rescheduleBarberAppointment(appointment.id, slot); setMessage('Appointment rescheduled.'); setRescheduling(false); setSlot(''); setAvailability(null); onChange(); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'Could not reschedule this appointment.'); }
    finally { setBusy(false); }
  }
  const canManage = appointment.status === 'CONFIRMED' && Date.parse(appointment.startAt) > Date.now();
  const today = dateInZone(new Date(), appointment.timezoneId);
  const maxDateValue = addDays(today, 90);
  return <article className="workspace-row">
    <div className="flex flex-wrap justify-between gap-3"><div><p className="font-semibold text-neutral-900">{appointment.serviceName} · {appointment.customerName}</p><p className="mt-1 text-sm text-neutral-600">{displayTime} · {appointment.durationMinutes} min</p>{appointment.customerNote && <p className="mt-2 text-sm text-neutral-500">Note: {appointment.customerNote}</p>}</div><span className={`status-chip status-${appointment.status.toLowerCase().replace('_', '-')}`}>{appointment.status.replace('_', ' ')}</span></div>
    {error && <p role="alert" className="mt-3 text-sm text-red-700">{error}</p>}
    {message && <p role="status" className="mt-3 text-sm text-green-700">{message}</p>}
    {appointment.status === 'PENDING' && <div className="mt-4 flex flex-wrap gap-2"><button disabled={busy} onClick={() => act('CONFIRMED')} className="button-dark !min-h-10 !px-4 !py-2">{busy ? 'Updating…' : 'Accept request'}</button><button disabled={busy} onClick={() => act('REJECTED')} className="button-secondary !min-h-10 !px-4 !py-2">Decline</button></div>}
    {canManage && <div className="mt-4 flex flex-wrap gap-2"><button type="button" disabled={busy} onClick={cancel} className="button-secondary !min-h-10 !px-4 !py-2 text-red-700 disabled:opacity-50">Cancel appointment</button><button type="button" disabled={busy} onClick={() => { setRescheduling((open) => !open); setAvailability(null); setSlot(''); setError(''); }} className="button-secondary !min-h-10 !px-4 !py-2 disabled:opacity-50">Reschedule</button></div>}
    {rescheduling && canManage && <div className="mt-4 rounded-xl bg-neutral-50 p-4"><label className="block text-sm font-medium">Choose a new date<input type="date" min={today} max={maxDateValue} onChange={(event) => void checkAvailability(event.target.value)} className="mt-1 w-full rounded-lg border bg-white px-3 py-2" /></label>{availability && (availability.slots.length ? <div className="mt-3 grid grid-cols-3 gap-2 sm:grid-cols-5">{availability.slots.map((time) => {
      const ambiguous = availability.slots.filter((candidate) => candidate.localStartTime === time.localStartTime).length > 1;
      const offset = time.startAt.match(/([+-]\d{2}:\d{2})$/)?.[1];
      return <button key={time.startAt} type="button" aria-pressed={slot === time.startAt} onClick={() => setSlot(time.startAt)} className={`rounded-lg border px-2 py-2 text-sm ${slot === time.startAt ? 'bg-neutral-900 text-white' : 'bg-white'}`}>{time.localStartTime.slice(0, 5)}{ambiguous && offset ? ` (${offset})` : ''}</button>;
    })}</div> : <p className="mt-3 text-sm text-neutral-500">No times are available on this date.</p>)}{slot && <button type="button" disabled={busy} onClick={reschedule} className="button-dark mt-4 disabled:opacity-50">{busy ? 'Rescheduling…' : 'Confirm new time'}</button>}</div>}
    {appointment.status === 'CONFIRMED' && Date.parse(appointment.endAt) < Date.now() && <div className="mt-4 flex flex-wrap gap-2"><button disabled={busy} onClick={() => act('COMPLETED')} className="button-dark !min-h-10 !px-4 !py-2">Mark completed</button><button disabled={busy} onClick={() => act('NO_SHOW')} className="button-secondary !min-h-10 !px-4 !py-2">Mark no-show</button></div>}
  </article>;
}

export default function BarberDashboardPage() {
  const [data, setData] = useState<BarberDashboard | null>(null);
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [error, setError] = useState('');
  const load = useCallback(() => {
    setError('');
    Promise.all([fetchBarberDashboard(), fetchMyBarberAppointments()]).then(([dashboard, allAppointments]) => {
      setData(dashboard);
      setAppointments(allAppointments);
    }).catch((err: unknown) => {
      if (err instanceof ApiError && err.status === 401) window.location.assign('/auth/login');
      else if (err instanceof ApiError && err.status === 403) setError('This dashboard is available to barber accounts.');
      else setError(err instanceof Error ? err.message : 'Could not load your dashboard.');
    });
  }, []);
  useEffect(() => load(), [load]);

  if (error && !data) return <main className="workspace-page page-wrap"><p className="eyebrow">Barber workspace</p><h1 className="workspace-title">Your schedule.</h1><div role="alert" className="notice-error mt-5">{error === 'Failed to fetch' ? 'We could not reach the booking service. Check your connection and try again.' : error}</div><button type="button" className="button-dark mt-4" onClick={load}>Try again</button></main>;
  if (!data) return <main className="workspace-page page-wrap"><p className="text-sm text-neutral-500">Loading your schedule…</p></main>;
  const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: data.earnings.currency, maximumFractionDigits: 2 }).format(data.earnings.completedRevenue);
  const now = Date.now();
  const todayAppointments = data.today.filter((item) => item.status === 'CONFIRMED' && Date.parse(item.endAt) >= now);
  const futureAppointments = data.upcoming.filter((item) => item.status === 'CONFIRMED'
    && dateInZone(new Date(item.startAt), item.timezoneId) > dateInZone(new Date(now), item.timezoneId));
  const history = appointments.filter((item) => ['COMPLETED', 'NO_SHOW', 'REJECTED', 'CANCELLED'].includes(item.status)
    || (item.status === 'CONFIRMED' && Date.parse(item.endAt) < now)).sort((a, b) => Date.parse(b.startAt) - Date.parse(a.startAt));

  return <main className="workspace-page page-wrap">
    <div className="workspace-header"><div><p className="eyebrow">Barber workspace</p><h1 className="workspace-title">Welcome, {data.user.displayName}.</h1><p className="workspace-subtitle">{data.user.email} · Your requests, appointments, and completed revenue.</p></div><div className="flex flex-wrap gap-2"><Link href="/barber/manage" className="button-secondary">Manage profile</Link><Link href="/search" className="button-dark">Public discovery</Link></div></div>
    {error && <p role="alert" className="notice-error mt-5">{error}</p>}
    <section className="mt-8 grid gap-3 sm:grid-cols-3"><div className="workspace-stat"><p className="workspace-stat-label">Pending requests</p><p className="workspace-stat-value">{data.pending.length}</p></div><div className="workspace-stat"><p className="workspace-stat-label">Today</p><p className="workspace-stat-value">{data.today.length}</p></div><div className="workspace-stat"><p className="workspace-stat-label">Completed revenue</p><p className="workspace-stat-value text-[1.7rem]">{money}</p><p className="mt-2 text-xs text-neutral-500">Lifetime total</p></div></section>
    <section className="workspace-section"><h2 className="workspace-section-title">Booking requests</h2><div className="mt-4 space-y-3">{data.pending.length ? data.pending.map((item) => <AppointmentRow key={item.id} appointment={item} onChange={load} />) : <p className="empty-state text-left text-sm text-neutral-600">No pending requests. New customer requests will appear here.</p>}</div></section>
    <section className="workspace-section"><h2 className="workspace-section-title">Today’s schedule</h2><div className="mt-4 space-y-3">{todayAppointments.length ? todayAppointments.map((item) => <AppointmentRow key={item.id} appointment={item} onChange={load} />) : <p className="empty-state text-left text-sm text-neutral-600">No confirmed appointments remain today.</p>}</div></section>
    <section className="workspace-section"><h2 className="workspace-section-title">Upcoming appointments</h2><div className="mt-4 space-y-3">{futureAppointments.length ? futureAppointments.map((item) => <AppointmentRow key={item.id} appointment={item} onChange={load} />) : <p className="empty-state text-left text-sm text-neutral-600">No confirmed appointments coming up.</p>}</div></section>
    <section className="workspace-section"><h2 className="workspace-section-title">Appointment history</h2><div className="mt-4 space-y-3">{history.length ? history.map((item) => <AppointmentRow key={item.id} appointment={item} onChange={load} />) : <p className="empty-state text-left text-sm text-neutral-600">Completed and cancelled appointments will appear here.</p>}</div></section>
  </main>;
}
