'use client';

import Link from 'next/link';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { signup, ApiError } from '../../../lib/api';
import type { Role } from '../../../lib/api';
import { useAuth } from '../../components/AuthProvider';

export default function SignupPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<Role>('CUSTOMER');
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
      const user = await signup(name, email, password, role);
      setUser(user);
      setMessage('Your account is ready. Taking you to your workspace…');
      window.setTimeout(() => router.push(user.role === 'BARBER' ? '/barber/dashboard' : '/client/dashboard'), 500);
    } catch (err) {
      if (err instanceof ApiError) {
        const details = err.details ? (Array.isArray(err.details) ? err.details.join(' ') : Object.values(err.details).join(' ')) : '';
        setError(details ? `${err.message} ${details}` : err.message);
      } else setError('Could not reach the server. Please try again.');
      setBusy(false);
    }
  };

  return <main className="auth-page">
    <aside className="auth-story">
      <Link href="/" className="relative z-10 text-xs font-semibold uppercase tracking-[.16em] text-[#e1c5a7]">Barbering</Link>
      <div><p className="auth-story-mark" aria-hidden="true">B.</p><h2 className="auth-story-title">Good work deserves to be easy to find.</h2><p className="auth-story-copy">Join the local community making it simpler to discover barbers and plan the next visit.</p></div>
      <span className="relative z-10 text-xs text-[#c0b3a5]">A better connection between chair and client</span>
    </aside>

    <section className="auth-form-wrap" aria-labelledby="signup-title">
      <p className="eyebrow">Start here</p>
      <h1 id="signup-title" className="auth-title mt-2">Create your account</h1>
      <p className="auth-note">Choose how you’ll use Barbering. You can update your profile after joining.</p>

      <form className="auth-form" onSubmit={handleSubmit}>
        <label htmlFor="account-role" className="field-label">I’m joining as
          <select id="account-role" name="role" className="field-control" value={role} onChange={(event) => setRole(event.target.value as Role)}>
            <option value="CUSTOMER">A customer</option><option value="BARBER">A barber</option>
          </select>
        </label>
        <label htmlFor="name" className="field-label">Full name
          <input id="name" name="name" type="text" autoComplete="name" placeholder="Your name" className="field-control" required maxLength={100} value={name} onChange={(event) => setName(event.target.value)} />
        </label>
        <label htmlFor="email" className="field-label">Email address
          <input id="email" name="email" type="email" autoComplete="email" placeholder="you@example.com" className="field-control" required value={email} onChange={(event) => setEmail(event.target.value)} />
        </label>
        <label htmlFor="password" className="field-label">Password
          <span className="relative block">
            <input id="password" name="password" type={showPassword ? 'text' : 'password'} autoComplete="new-password" placeholder="Create a password" className="field-control pr-16" required minLength={8} value={password} onChange={(event) => setPassword(event.target.value)} />
            <button type="button" onClick={() => setShowPassword((visible) => !visible)} className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-[#805535]">{showPassword ? 'Hide' : 'Show'}</button>
          </span>
        </label>
        {error && <p role="alert" className="notice-error">{error}</p>}
        {message && <p role="status" className="notice-success">{message}</p>}
        <button type="submit" disabled={busy} className="button-dark w-full">{busy ? 'Creating account…' : 'Create account'}</button>
      </form>

      <p className="mt-6 text-sm text-neutral-600">Already have an account? <Link href="/auth/login" className="font-semibold text-[#805535] underline underline-offset-4">Sign in</Link></p>
    </section>
  </main>;
}
