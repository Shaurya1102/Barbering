'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { login, ApiError } from '../../../lib/api';
import { useAuth } from '../../components/AuthProvider';

export default function LoginPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const router = useRouter();
  const { setUser } = useAuth();

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setMessage('');
    setError('');
    setBusy(true);
    try {
      const user = await login(email, password);
      setUser(user);
      setMessage('Welcome back. Taking you to your workspace…');
      window.setTimeout(() => router.push(user.role === 'BARBER' ? '/barber/dashboard' : '/client/dashboard'), 500);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not reach the server. Please try again.');
      setBusy(false);
    }
  };

  return <main className="auth-page">
    <aside className="auth-story">
      <Link href="/" className="relative z-10 text-xs font-semibold uppercase tracking-[.16em] text-[#e1c5a7]">Barbering</Link>
      <div><p className="auth-story-mark" aria-hidden="true">B.</p><h2 className="auth-story-title">Your next good appointment starts here.</h2><p className="auth-story-copy">Keep the barbers you like and the bookings you make in one thoughtful place.</p></div>
      <span className="relative z-10 text-xs text-[#c0b3a5]">Find your chair · book with confidence</span>
    </aside>

    <section className="auth-form-wrap" aria-labelledby="login-title">
      <p className="eyebrow">Welcome back</p>
      <h1 id="login-title" className="auth-title mt-2">Sign in to Barbering</h1>
      <p className="auth-note">Access your appointments, saved barbers, and profile.</p>

      <form className="auth-form" onSubmit={handleSubmit}>
        <label htmlFor="email" className="field-label">Email address
          <input id="email" name="email" type="email" autoComplete="email" placeholder="you@example.com" className="field-control" required value={email} onChange={(event) => setEmail(event.target.value)} />
        </label>
        <label htmlFor="password" className="field-label">Password
          <span className="relative block">
            <input id="password" name="password" type={showPassword ? 'text' : 'password'} autoComplete="current-password" placeholder="Enter your password" className="field-control pr-16" required value={password} onChange={(event) => setPassword(event.target.value)} />
            <button type="button" onClick={() => setShowPassword((visible) => !visible)} className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-[#805535]">{showPassword ? 'Hide' : 'Show'}</button>
          </span>
        </label>

        {error && <p role="alert" className="notice-error">{error}</p>}
        {message && <p role="status" className="notice-success">{message}</p>}
        <button type="submit" disabled={busy} className="button-dark w-full">{busy ? 'Signing in…' : 'Sign in'}</button>
      </form>

      <p className="mt-6 text-sm text-neutral-600">New to Barbering? <Link href="/auth/signup" className="font-semibold text-[#805535] underline underline-offset-4">Create an account</Link></p>
    </section>
  </main>;
}
