'use client';

import Image from 'next/image';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { useAuth } from '../../components/AuthProvider';

export default function ImageGeneratorPage() {
  const { user, loading, error: authError, retry } = useAuth();
  const router = useRouter();
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState('');
  const [category, setCategory] = useState('Haircut');
  const [style, setStyle] = useState('');
  useEffect(() => { if (!loading && !authError && !user) router.replace('/auth/login'); }, [authError, loading, user, router]);
  useEffect(() => {
    if (!file) { setPreview(''); return; }
    const objectUrl = URL.createObjectURL(file); setPreview(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [file]);

  if (loading) return <main className="ai-page"><p className="text-neutral-500">Checking your account…</p></main>;
  if (authError) return <main className="ai-page"><p role="alert" className="notice-error">{authError}</p><button type="button" className="button-dark mt-4" onClick={() => void retry()}>Try again</button></main>;
  if (!user) return <main className="ai-page"><p className="text-neutral-500">Taking you to sign in…</p></main>;
  return <main className="ai-page">
    <p className="eyebrow">Style preview</p>
    <h1>Explore a hairstyle idea.</h1>
    <p className="mt-3 max-w-2xl leading-7 text-neutral-600">Choose a photo and describe a style to prepare your request. Image generation is not configured yet. Your selected photo stays in this browser and is not uploaded.</p>
    <div className="ai-panel grid gap-6 md:grid-cols-[1fr_.8fr]">
      <div>
        <label className="field-label">Choose a photo<input type="file" accept="image/jpeg,image/png,image/webp" onChange={(event) => setFile(event.target.files?.[0] ?? null)} className="field-control !h-auto !py-3" /></label>
        <label className="field-label mt-5">Style category<select value={category} onChange={(event) => setCategory(event.target.value)} className="field-control"><option>Haircut</option><option>Hair color</option><option>Beard</option></select></label>
        <label className="field-label mt-5">What would you like to try?<textarea value={style} onChange={(event) => setStyle(event.target.value)} rows={4} maxLength={500} placeholder="Describe a cut, texture, or beard style" className="field-control !h-auto" /></label>
        <button type="button" disabled className="button-dark mt-5">Generation unavailable</button>
        <p role="status" className="mt-3 rounded-xl bg-amber-50 p-4 text-sm text-amber-950">A hairstyle image provider has not been connected. No result is generated and no photo is sent.</p>
      </div>
      <div className="flex min-h-64 items-center justify-center overflow-hidden rounded-2xl border border-[#e8e2d9] bg-[#f2eee7] p-3">
        {preview ? <Image src={preview} alt="Local photo preview" width={480} height={480} unoptimized className="max-h-96 w-auto object-contain" /> : <p className="px-8 text-center text-sm text-neutral-500">Your selected image preview will appear here.</p>}
      </div>
    </div>
    <p className="mt-4 text-xs text-neutral-500">{file ? `${file.name} · ${category}${style ? ` · ${style}` : ''}` : 'Choose a photo to preview it locally.'}</p>
  </main>;
}
