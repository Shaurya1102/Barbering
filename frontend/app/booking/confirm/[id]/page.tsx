'use client';

import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { ApiError, fetchMyAppointments } from '../../../../lib/api';
import type { Appointment } from '../../../../lib/api';

export default function BookingConfirmPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [appointment, setAppointment] = useState<Appointment | null>(null);
  const [error, setError] = useState('');
  const [loaded, setLoaded] = useState(false);
  const load = useCallback(() => {
    let cancelled = false;
    setLoaded(false);
    setError('');
    setAppointment(null);
    fetchMyAppointments().then((items) => {
      if (!cancelled) {
        setAppointment(items.find((item) => item.id === Number(id)) ?? null);
        setLoaded(true);
      }
    }).catch((err: unknown) => {
      if (cancelled) return;
      if (err instanceof ApiError && err.status === 401) router.replace('/auth/login');
      else setError(err instanceof Error ? err.message : 'Could not load this appointment.');
      setLoaded(true);
    });
    return () => { cancelled = true; };
  }, [id, router]);
  useEffect(() => load(), [load]);

  return <main className="content-page max-w-3xl">
    {error && <p role="alert" className="rounded-xl bg-red-50 p-4 text-red-700">{error}</p>}
    {!loaded && !error && <p className="text-neutral-500">Loading appointment…</p>}
    {error && <button type="button" onClick={load} className="button-dark mt-4">Try again</button>}
    {loaded && !error && !appointment && <section className="panel p-6 sm:p-9"><h1 className="text-2xl font-semibold">Appointment unavailable</h1><p className="mt-3 text-neutral-600">This appointment could not be found in your account.</p><Link href="/client/dashboard" className="button-dark mt-6 inline-flex">View my bookings</Link></section>}
    {appointment && <section className="panel p-6 sm:p-9">
      <p className="text-sm font-semibold uppercase tracking-[.15em] text-amber-800">Request sent</p>
      <h1 className="mt-2 text-3xl font-semibold">Your appointment request is in.</h1>
      <p className="mt-3 leading-7 text-neutral-600">{appointment.barberName} will review your request. You’ll see it in your bookings when its status changes.</p>
      <dl className="appointment-summary mt-7 grid gap-4 sm:grid-cols-2">
        <div><dt className="text-xs uppercase text-neutral-500">Service</dt><dd className="mt-1 font-medium">{appointment.serviceName}</dd></div>
        <div><dt className="text-xs uppercase text-neutral-500">Status</dt><dd className="mt-1 font-medium">{appointment.status}</dd></div>
        <div><dt className="text-xs uppercase text-neutral-500">Date and time</dt><dd className="mt-1 font-medium">{new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short', timeZone: appointment.timezoneId }).format(new Date(appointment.startAt))} ({appointment.timezoneId})</dd></div>
        <div><dt className="text-xs uppercase text-neutral-500">Duration</dt><dd className="mt-1 font-medium">{appointment.durationMinutes} minutes</dd></div>
      </dl>
      <div className="mt-7 flex flex-wrap gap-3"><Link href="/client/dashboard" className="rounded-xl bg-neutral-950 px-5 py-3 text-sm font-semibold text-white">View my bookings</Link><Link href={`/barbers/${appointment.barberId}`} className="rounded-xl border px-5 py-3 text-sm font-semibold">Barber profile</Link></div>
    </section>}
  </main>;
}
