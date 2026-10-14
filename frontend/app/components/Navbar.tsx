'use client';

import Image from 'next/image';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { useAuth } from './AuthProvider';

export default function Navbar() {
  const { user, loading, error: authError, retry, signOut } = useAuth();
  const router = useRouter();
  const [mobileOpen, setMobileOpen] = useState(false);

  async function logOut() {
    await signOut();
    setMobileOpen(false);
    router.replace('/');
    router.refresh();
  }

  function closeMenu() { setMobileOpen(false); }

  return (
    <header className="site-header">
      <nav className="site-nav" aria-label="Main navigation">
        <Link href="/" className="site-brand" aria-label="Barbering home">
          <Image src="/logo.png" alt="" width={64} height={64} priority />
          <span className="site-brand-copy"><span className="site-brand-title">BARBERING</span><span className="site-brand-caption">Find your chair</span></span>
        </Link>

        <div className="nav-links">
          <Link href="/search" className="nav-link">Find a barber</Link>
          <Link href="/for-barbers" className="nav-link">For barbers</Link>
          {loading ? <span className="nav-link" aria-live="polite">Checking account…</span> : authError ? <>
            <span className="nav-link" role="status">Account status unavailable</span>
            <button type="button" className="nav-pill nav-outline" onClick={() => void retry()}>Retry</button>
          </> : user ? <>
            {user.role === 'BARBER' ? <>
              <Link href="/barber/dashboard" className="nav-link">Workspace</Link>
              <Link href="/barber/manage" className="nav-link">Manage profile</Link>
            </> : <>
              <Link href="/client/dashboard" className="nav-link">My bookings</Link>
              <Link href="/client/dashboard#favourites" className="nav-link">Favourites</Link>
            </>}
            <Link href="/ai/reccomender" className="nav-link">Style recommendations</Link>
            <Link href="/ai/image-generator" className="nav-link">Hairstyle preview</Link>
            <span className="nav-link max-w-32 truncate" title={user.displayName}>{user.displayName}</span>
            <button type="button" className="nav-pill nav-outline" onClick={logOut}>Log out</button>
          </> : <>
            <Link href="/auth/login" className="nav-link">Log in</Link>
            <Link href="/auth/signup" className="nav-pill nav-pill-dark">Create account</Link>
          </>}
        </div>

        <button type="button" className="mobile-menu-button" aria-label={mobileOpen ? 'Close navigation menu' : 'Open navigation menu'} aria-expanded={mobileOpen} aria-controls="mobile-navigation" onClick={() => setMobileOpen((open) => !open)}>
          <span aria-hidden="true" className="text-xl leading-none">{mobileOpen ? '×' : '☰'}</span>
        </button>
      </nav>
      {mobileOpen && <nav id="mobile-navigation" aria-label="Mobile navigation" className="mobile-nav">
        <Link href="/search" onClick={closeMenu}>Find a barber</Link>
        <Link href="/for-barbers" onClick={closeMenu}>For barbers</Link>
        {loading ? <p className="px-3 py-2 text-sm text-neutral-500">Checking account…</p> : authError ? <>
          <p role="status" className="px-3 py-2 text-sm text-amber-900">Account status unavailable.</p>
          <button type="button" onClick={() => void retry()}>Try again</button>
        </> : user ? <>
          {user.role === 'BARBER' ? <>
            <Link href="/barber/dashboard" onClick={closeMenu}>Workspace</Link>
            <Link href="/barber/manage" onClick={closeMenu}>Manage profile</Link>
          </> : <>
            <Link href="/client/dashboard" onClick={closeMenu}>My bookings</Link>
            <Link href="/client/dashboard#favourites" onClick={closeMenu}>Favourites</Link>
          </>}
          <Link href="/ai/reccomender" onClick={closeMenu}>Style recommendations</Link>
          <Link href="/ai/image-generator" onClick={closeMenu}>Hairstyle preview</Link>
          <p className="px-3 py-2 text-xs text-neutral-500">Signed in as {user.displayName}</p>
          <button type="button" onClick={logOut}>Log out</button>
        </> : <>
          <Link href="/auth/login" onClick={closeMenu}>Log in</Link>
          <Link href="/auth/signup" onClick={closeMenu}>Create account</Link>
        </>}
      </nav>}
    </header>
  );
}
