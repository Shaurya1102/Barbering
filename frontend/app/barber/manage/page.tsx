'use client';

import Image from 'next/image';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { API_URL, ApiError, createBreak, createMyService, deleteBarberPhoto, fetchBreaks, fetchMyBarberProfile, fetchMyServices, fetchWorkingHours, removeBreak, removeMyService, updateMyBarberProfile, updateMyService, updateUserProfile, updateWorkingHours, uploadBarberPhoto } from '../../../lib/api';
import type { Barber, BreakTime, DayOfWeek, Offering } from '../../../lib/api';
import { useAuth } from '../../components/AuthProvider';

const days: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
type HourDraft = Record<DayOfWeek, { enabled: boolean; startTime: string; endTime: string }>;
const blankHours = (): HourDraft => Object.fromEntries(days.map((day) => [day, { enabled: false, startTime: '09:00', endTime: '17:00' }])) as HourDraft;
const baseFields = 'mt-1 w-full rounded-lg border px-3 py-2 text-sm';

export default function BarberManagePage() {
  const [profile, setProfile] = useState<Barber | null>(null);
  const [services, setServices] = useState<Offering[]>([]);
  const [breaks, setBreaks] = useState<BreakTime[]>([]);
  const [hours, setHours] = useState<HourDraft>(blankHours);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [addingService, setAddingService] = useState(false);
  const { user, loading: authLoading, error: authError, retry: retryAuth, setUser } = useAuth();
  const router = useRouter();
  const load = useCallback(async () => {
    setError('');
    try {
      const [barber, offerings, working, breakList] = await Promise.all([fetchMyBarberProfile(), fetchMyServices(), fetchWorkingHours(), fetchBreaks()]);
      setProfile(barber); setServices(offerings); setBreaks(breakList);
      const draft = blankHours();
      working.forEach((entry) => { draft[entry.dayOfWeek] = { enabled: entry.enabled, startTime: entry.startTime.slice(0, 5), endTime: entry.endTime.slice(0, 5) }; });
      setHours(draft);
    } catch (err) { setError(err instanceof ApiError ? err.message : 'Could not load barber settings.'); }
  }, []);
  useEffect(() => {
    if (authLoading || authError) return;
    if (!user) { router.replace('/auth/login'); return; }
    if (user.role !== 'BARBER') { router.replace('/client/dashboard'); return; }
    void load();
  }, [authError, authLoading, load, router, user]);

  async function saveProfile(form: FormData) {
    const payload = Object.fromEntries(['bio', 'city', 'region', 'streetAddress', 'postalCode', 'country', 'timezoneId'].map((key) => [key, String(form.get(key) ?? '')]));
    try { const updated = await updateMyBarberProfile(payload); setProfile(updated); setNotice('Profile saved.'); setError(''); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'Could not save the profile.'); }
  }

  async function saveHours() {
    try {
      const payload = days.map((day) => ({ dayOfWeek: day, ...hours[day] }));
      await updateWorkingHours(payload); setNotice('Working hours saved.'); setError(''); await load();
    } catch (err) { setError(err instanceof ApiError ? err.message : 'Could not save working hours.'); }
  }

  async function addService(form: FormData) {
    if (addingService) return;
    setAddingService(true);
    try {
      await createMyService({ name: String(form.get('name')), description: String(form.get('description') ?? ''), priceAmount: Number(form.get('price')), durationMinutes: Number(form.get('duration')) });
      setNotice('Service added.'); setError(''); await load();
    } catch (err) { setError(err instanceof ApiError ? err.message : 'Could not add this service.'); }
    finally { setAddingService(false); }
  }

  async function addBreak(form: FormData) {
    try {
      await createBreak({ dayOfWeek: String(form.get('dayOfWeek')) as DayOfWeek, startTime: String(form.get('startTime')), endTime: String(form.get('endTime')), reason: String(form.get('reason') ?? '') });
      setNotice('Recurring break added.'); setError(''); await load();
    } catch (err) { setError(err instanceof ApiError ? err.message : 'Could not add this break.'); }
  }

  async function uploadPhoto(form: FormData) {
    const file = form.get('file');
    if (!(file instanceof File) || file.size === 0) { setError('Choose an image first.'); return; }
    try { await uploadBarberPhoto(file, String(form.get('altText') ?? '')); setNotice('Photo uploaded.'); setError(''); await load(); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'Could not upload this photo.'); }
  }

  if (authLoading) return <main className="workspace-page page-wrap"><p className="text-sm text-neutral-500">Checking your account…</p></main>;
  if (authError) return <main className="workspace-page page-wrap"><p role="alert" className="notice-error">{authError}</p><button type="button" className="button-dark mt-4" onClick={() => void retryAuth()}>Try again</button></main>;
  if (!user || user.role !== 'BARBER') return <main className="workspace-page page-wrap"><p className="text-sm text-neutral-500">Taking you to your account…</p></main>;
  if (!profile && !error) return <main className="workspace-page page-wrap"><p className="text-sm text-neutral-500">Loading barber settings…</p></main>;

  return <main className="workspace-page page-wrap">
    <div className="workspace-header"><div><p className="eyebrow">Barber workspace</p><h1 className="workspace-title">Manage your profile.</h1><p className="workspace-subtitle">Keep your public details, services, and availability current.</p></div><Link className="button-secondary" href={profile ? `/barbers/${profile.id}` : '/search'}>View public profile</Link></div>
    {error && <div role="alert" className="notice-error mt-5">{error}{!profile && <button type="button" className="ml-3 underline" onClick={() => void load()}>Try again</button>}</div>}
    {notice && <p role="status" className="notice-success mt-5">{notice}</p>}

    {profile && <section className="workspace-form-section mt-8">
      <h2 className="text-xl font-semibold">Public profile</h2>
      <p className="mt-1 text-sm text-neutral-500">Shown on your discovery page.</p>
      <NameEditor displayName={profile.displayName} onSaved={async (user) => { setUser(user); await load(); setNotice('Display name saved.'); }} onError={setError} />
      <form action={saveProfile} className="mt-5 grid gap-4 sm:grid-cols-2">
        <label className="text-sm font-medium sm:col-span-2">About you<textarea name="bio" defaultValue={profile.bio ?? ''} maxLength={2000} rows={4} className={baseFields} /></label>
        <label className="text-sm font-medium">City<input name="city" defaultValue={profile.city ?? ''} maxLength={120} className={baseFields} /></label>
        <label className="text-sm font-medium">Region / state<input name="region" defaultValue={profile.region ?? ''} maxLength={120} className={baseFields} /></label>
        <label className="text-sm font-medium sm:col-span-2">Street address<input name="streetAddress" defaultValue={profile.streetAddress ?? ''} maxLength={255} className={baseFields} /></label>
        <label className="text-sm font-medium">Postal code<input name="postalCode" defaultValue={profile.postalCode ?? ''} maxLength={32} className={baseFields} /></label>
        <label className="text-sm font-medium">Country<input name="country" defaultValue={profile.country ?? ''} maxLength={120} className={baseFields} /></label>
        <label className="text-sm font-medium sm:col-span-2">Time zone<input name="timezoneId" defaultValue={profile.timezoneId} placeholder="Asia/Kolkata" className={baseFields} /><span className="mt-1 block text-xs font-normal text-neutral-500">Use an IANA zone such as Asia/Kolkata or America/New_York.</span></label>
        <button className="w-fit rounded-lg bg-neutral-900 px-4 py-2.5 text-sm font-semibold text-white">Save profile</button>
      </form>
    </section>}

    <section className="workspace-form-section">
      <h2 className="text-xl font-semibold">Services and prices</h2>
      <form action={addService} className="mt-4 grid gap-3 sm:grid-cols-2">
        <input name="name" required maxLength={100} placeholder="Service name (e.g. Haircut)" className={baseFields} />
        <input name="description" maxLength={1000} placeholder="Short description" className={baseFields} />
        <label className="text-sm">Price<input name="price" required type="number" min="0" step="0.01" placeholder="₹" className={baseFields} /></label>
        <label className="text-sm">Duration (minutes)<input name="duration" required type="number" min="5" max="480" step="5" defaultValue="30" className={baseFields} /></label>
        <button disabled={addingService} className="w-fit rounded-lg bg-neutral-900 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50 sm:col-span-2">{addingService ? 'Adding…' : 'Add service'}</button>
      </form>
      <div className="mt-5 space-y-3">{services.length === 0 && <p className="text-sm text-neutral-500">No services yet. Add the services customers can book.</p>}{services.map((service) => <ServiceRow key={service.id} service={service} onChange={load} onError={setError} />)}</div>
    </section>

    <section className="workspace-form-section">
      <h2 className="text-xl font-semibold">Working hours</h2><p className="mt-1 text-sm text-neutral-500">Set one weekly opening range for each day. Leave a day closed when you are unavailable.</p>
      <div className="mt-4 divide-y">{days.map((day) => <div key={day} className="hours-row grid items-center gap-3 py-3 sm:grid-cols-[1fr_auto_1fr_1fr]">
        <label className="flex items-center gap-3 text-sm font-medium"><input type="checkbox" checked={hours[day].enabled} onChange={(event) => setHours((prev) => ({ ...prev, [day]: { ...prev[day], enabled: event.target.checked } }))} />{day[0] + day.slice(1).toLowerCase()}</label>
        <span className="hidden text-sm text-neutral-400 sm:block">Open</span><input aria-label={`${day} opening time`} type="time" value={hours[day].startTime} disabled={!hours[day].enabled} onChange={(event) => setHours((prev) => ({ ...prev, [day]: { ...prev[day], startTime: event.target.value } }))} className={baseFields} />
        <input aria-label={`${day} closing time`} type="time" value={hours[day].endTime} disabled={!hours[day].enabled} onChange={(event) => setHours((prev) => ({ ...prev, [day]: { ...prev[day], endTime: event.target.value } }))} className={baseFields} />
      </div>)}</div>
      <button type="button" onClick={saveHours} className="mt-4 rounded-lg bg-neutral-900 px-4 py-2.5 text-sm font-semibold text-white">Save working hours</button>
    </section>

    <section className="workspace-form-section">
      <h2 className="text-xl font-semibold">Recurring breaks</h2><p className="mt-1 text-sm text-neutral-500">Breaks repeat each week and must fit inside that day’s working hours.</p>
      <form action={addBreak} className="mt-4 grid gap-3 sm:grid-cols-4">
        <label className="text-sm">Day<select name="dayOfWeek" className={baseFields}>{days.map((day) => <option key={day}>{day}</option>)}</select></label>
        <label className="text-sm">From<input type="time" name="startTime" required className={baseFields} /></label>
        <label className="text-sm">To<input type="time" name="endTime" required className={baseFields} /></label>
        <label className="text-sm">Reason<input name="reason" maxLength={120} placeholder="Lunch" className={baseFields} /></label>
        <button className="w-fit rounded-lg bg-neutral-900 px-4 py-2.5 text-sm font-semibold text-white sm:col-span-4">Add break</button>
      </form>
      <div className="mt-4 space-y-2">{breaks.map((entry) => <BreakRow key={entry.id} entry={entry} onChange={load} onError={setError} />)}{breaks.length === 0 && <p className="text-sm text-neutral-500">No recurring breaks set.</p>}</div>
    </section>

    <section className="workspace-form-section">
      <h2 className="text-xl font-semibold">Photos</h2><p className="mt-1 text-sm text-neutral-500">JPEG, PNG, and WebP images up to 5 MB.</p>
      <form action={uploadPhoto} className="mt-4 grid gap-3 sm:grid-cols-2"><input type="file" name="file" accept="image/jpeg,image/png,image/webp" required className={baseFields} /><input name="altText" maxLength={160} placeholder="Image description (optional)" className={baseFields} /><button className="w-fit rounded-lg bg-neutral-900 px-4 py-2.5 text-sm font-semibold text-white sm:col-span-2">Upload photo</button></form>
      {profile?.photos.length ? <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-4">{profile.photos.map((photo) => <div key={photo.id} className="relative h-36 overflow-hidden rounded-xl bg-neutral-100"><Image src={`${API_URL}${photo.url}`} alt={photo.altText ?? 'Barber portfolio image'} fill unoptimized className="object-cover" /><button type="button" onClick={() => void deletePhoto(photo.id, load, setError)} className="absolute right-2 top-2 rounded-full bg-black/70 px-3 py-1 text-xs text-white">Remove</button></div>)}</div> : <p className="mt-4 text-sm text-neutral-500">No portfolio photos yet.</p>}
      {profile && <p className="mt-4 text-sm"><a className="underline" href={`/barbers/${profile.id}`}>View public profile</a></p>}
    </section>
  </main>;
}

function NameEditor({ displayName, onSaved, onError }: { displayName: string; onSaved: (user: Awaited<ReturnType<typeof updateUserProfile>>) => void; onError: (value: string) => void }) {
  const [name, setName] = useState(displayName);
  useEffect(() => setName(displayName), [displayName]);
  return <form action={async () => {
    try { onSaved(await updateUserProfile(name)); }
    catch (err) { onError(err instanceof ApiError ? err.message : 'Could not update your display name.'); }
  }} className="mt-4 flex max-w-xl flex-wrap items-end gap-3">
    <label className="flex-1 text-sm font-medium">Display name<input required maxLength={100} value={name} onChange={(event) => setName(event.target.value)} className={baseFields} /></label>
    <button className="rounded-lg border px-4 py-2.5 text-sm font-semibold">Save name</button>
  </form>;
}

function ServiceRow({ service, onChange, onError }: { service: Offering; onChange: () => void; onError: (value: string) => void }) {
  const [price, setPrice] = useState(String(service.priceAmount));
  const [duration, setDuration] = useState(String(service.durationMinutes));
  const [name, setName] = useState(service.name);
  const [description, setDescription] = useState(service.description ?? '');
  useEffect(() => {
    setName(service.name); setDescription(service.description ?? '');
    setPrice(String(service.priceAmount)); setDuration(String(service.durationMinutes));
  }, [service]);
  async function save(enabled = service.enabled) {
    onError('');
    const parsedPrice = Number(price);
    const parsedDuration = Number(duration);
    if (!name.trim()) { onError('Service name is required.'); return; }
    if (!Number.isFinite(parsedPrice) || parsedPrice < 0) { onError('Enter a valid non-negative price.'); return; }
    if (!Number.isInteger(parsedDuration) || parsedDuration < 5 || parsedDuration > 480) { onError('Duration must be between 5 and 480 minutes.'); return; }
    try { await updateMyService(service.id, { name: name.trim(), description, priceAmount: parsedPrice, durationMinutes: parsedDuration, enabled }); onChange(); }
    catch (err) { onError(err instanceof ApiError ? err.message : 'Could not update this service.'); }
  }
  async function disable() {
    onError(''); try { await removeMyService(service.id); onChange(); }
    catch (err) { onError(err instanceof ApiError ? err.message : 'Could not disable this service.'); }
  }
  return <div className="workspace-row service-editor grid items-end gap-3 sm:grid-cols-[1fr_130px_140px_auto_auto]">
    <div className="space-y-2"><label className="block text-xs text-neutral-500">Service name<input required maxLength={100} value={name} onChange={(event) => setName(event.target.value)} className={baseFields} /></label><label className="block text-xs text-neutral-500">Description<input maxLength={1000} value={description} onChange={(event) => setDescription(event.target.value)} className={baseFields} /></label><p className="text-xs text-neutral-500">{service.enabled ? 'Available to book' : 'Disabled'}</p></div>
    <label className="text-xs text-neutral-500">Price<input type="number" required min="0" step="0.01" value={price} onChange={(event) => setPrice(event.target.value)} className={baseFields} /></label>
    <label className="text-xs text-neutral-500">Minutes<input type="number" required min="5" max="480" step="5" value={duration} onChange={(event) => setDuration(event.target.value)} className={baseFields} /></label>
    <button type="button" onClick={() => void save()} className="rounded-lg border px-3 py-2 text-sm">Save</button>
    <button type="button" onClick={() => void (service.enabled ? disable() : save(true))} className="rounded-lg border px-3 py-2 text-sm">{service.enabled ? 'Disable' : 'Enable'}</button>
  </div>;
}

function BreakRow({ entry, onChange, onError }: { entry: BreakTime; onChange: () => void; onError: (value: string) => void }) {
  return <div className="workspace-row break-row flex items-center justify-between gap-3 text-sm"><span>{entry.dayOfWeek} · {entry.startTime.slice(0, 5)}–{entry.endTime.slice(0, 5)}{entry.reason ? ` · ${entry.reason}` : ''}</span><button type="button" onClick={async () => { try { await removeBreak(entry.id); onChange(); } catch (err) { onError(err instanceof ApiError ? err.message : 'Could not remove this break.'); } }} className="text-neutral-600 underline">Remove</button></div>;
}

async function deletePhoto(id: number, reload: () => void, onError: (value: string) => void) {
  try { await deleteBarberPhoto(id); reload(); }
  catch (err) { onError(err instanceof ApiError ? err.message : 'Could not remove this photo.'); }
}
