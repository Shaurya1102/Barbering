'use client';

import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { ApiError, requestRecommendations } from '../../../lib/api';
import type { RecommendationItem } from '../../../lib/api';
import { useAuth } from '../../components/AuthProvider';

const field = 'mt-1 w-full rounded-xl border px-3 py-2.5 text-sm';

export default function RecommenderPage() {
  const { user, loading, error: authError, retry } = useAuth();
  const router = useRouter();
  const [recommendations, setRecommendations] = useState<RecommendationItem[]>([]);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  useEffect(() => { if (!loading && !authError && !user) router.replace('/auth/login'); }, [authError, loading, user, router]);

  async function submit(form: FormData) {
    setBusy(true); setError(''); setMessage(''); setRecommendations([]);
    try {
      const result = await requestRecommendations({ hairTexture: String(form.get('texture')), hairLength: String(form.get('length')), preferences: String(form.get('preferences')), maintenanceTolerance: String(form.get('maintenance')), context: String(form.get('context') ?? '') });
      setRecommendations(result.recommendations);
    } catch (err) {
      if (err instanceof ApiError && err.status === 503) setMessage(err.message);
      else setError(err instanceof ApiError ? err.message : 'Could not request style recommendations.');
    } finally { setBusy(false); }
  }

  if (loading) return <main className="ai-page"><p className="text-neutral-500">Checking your account…</p></main>;
  if (authError) return <main className="ai-page"><p role="alert" className="notice-error">{authError}</p><button type="button" className="button-dark mt-4" onClick={() => void retry()}>Try again</button></main>;
  if (!user) return <main className="ai-page"><p className="text-neutral-500">Taking you to sign in…</p></main>;
  return <main className="ai-page">
    <p className="eyebrow">Style assistant</p>
    <h1>Find a style that fits your routine.</h1>
    <p className="mt-3 max-w-2xl leading-7 text-neutral-600">Share what your hair is like and what you want. Recommendations are suggestions, not guarantees.</p>
    <form action={submit} className="ai-panel space-y-4">
      <label className="block text-sm font-medium">Hair texture<select name="texture" required className={field}><option value="">Choose one</option>{['Straight', 'Wavy', 'Curly', 'Coily', 'Not sure'].map((item) => <option key={item}>{item}</option>)}</select></label>
      <label className="block text-sm font-medium">Current length<select name="length" required className={field}><option value="">Choose one</option>{['Very short', 'Short', 'Medium', 'Long', 'Growing it out'].map((item) => <option key={item}>{item}</option>)}</select></label>
      <label className="block text-sm font-medium">What do you like or want to change?<textarea name="preferences" required maxLength={500} rows={3} placeholder="For example: clean around the sides, keep some length on top" className={field} /></label>
      <label className="block text-sm font-medium">How much daily upkeep feels right?<select name="maintenance" required className={field}><option value="">Choose one</option><option>Low</option><option>Some</option><option>High</option></select></label>
      <label className="block text-sm font-medium">Where do you want the style to work? <span className="font-normal text-neutral-500">(optional)</span><input name="context" maxLength={120} placeholder="Work, sport, everyday…" className={field} /></label>
      <button disabled={busy} className="button-dark">{busy ? 'Checking…' : 'Get suggestions'}</button>
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      {message && <p role="status" className="rounded-xl bg-amber-50 p-4 text-sm text-amber-950">{message}</p>}
    </form>
    {recommendations.length > 0 && <section className="mt-8 space-y-3"><h2 className="section-heading">Ideas to discuss with your barber</h2>{recommendations.map((item, index) => <article key={`${item.name}-${index}`} className="ai-result"><h3 className="font-semibold">{item.name}</h3><p className="mt-2 text-sm leading-6 text-neutral-600">{item.explanation}</p><p className="mt-2 text-sm"><span className="font-medium">Upkeep:</span> {item.maintenance}</p></article>)}</section>}
  </main>;
}
