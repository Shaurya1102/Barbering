'use client';

import Link from 'next/link';
import { useState } from 'react';
import Image from 'next/image';
import { useRouter } from 'next/navigation'; // Import useRouter

export default function SignupPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState('');
  const router = useRouter(); // Initialize router

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch('http://localhost:8080/auth/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, email, password }),
      });
      const text = await res.text();
      setMessage(text);
      if (res.ok) {
        // Optionally, handle login here (e.g., save token)
        setTimeout( () => {
          router.push('/');
        }, 1200) // Redirect to Home page
      } else {
        const text = await res.text();
        setMessage(text || 'Signup failed');
      }
    } catch (err) {
      setMessage('Signup failed');
    }
  };

  return (
    <main className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="max-w-md w-full bg-white shadow-xl rounded-2xl p-8 space-y-6">
        <div className="text-center">
          <Link href="/"><Image
            src="/logo.png"
            alt="Logo"
            width={200}
            height={100}
            className="mx-auto w- h-32"
          /></Link>
          <h1 className="text-2xl font-bold mt-4">Create Account</h1>
          <p className="text-sm text-gray-500">Join Barbering today</p>
        </div>

        <form className="space-y-4" onSubmit={handleSubmit}>
          <div>
            <label className="block text-sm font-medium text-gray-700">
              Name
            </label>
            <input
              type="text"
              placeholder="Full Name"
              className="mt-1 w-full rounded-xl border px-4 py-3 shadow-sm focus:ring-blue-500 focus:border-blue-500"
              required
              value={name}
              onChange={e => setName(e.target.value)}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700">
              Email
            </label>
            <input
              type="email"
              placeholder="you@example.com"
              className="mt-1 w-full rounded-xl border px-4 py-3 shadow-sm focus:ring-blue-500 focus:border-blue-500"
              required
              value={email}
              onChange={e => setEmail(e.target.value)}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700">
              Password
            </label>
            <div className="relative">
              <input
                type={showPassword ? 'text' : 'password'}
                placeholder="••••••••"
                className="mt-1 w-full rounded-xl border px-4 py-3 shadow-sm focus:ring-blue-500 focus:border-blue-500"
                required
                value={password}
                onChange={e => setPassword(e.target.value)}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 transform -translate-y-1/2 text-sm text-gray-500"
              >
                {showPassword ? 'Hide' : 'Show'}
              </button>
            </div>
          </div>

          <button
            type="submit"
            className="w-full bg-blue-600 text-white py-3 rounded-xl font-semibold hover:bg-blue-700 transition"
          >
            Sign Up
          </button>
        </form>

        {message && (
          <div className="text-center text-green-600 font-semibold">{message}</div>
        )}

        <div className="text-center text-sm text-gray-500">
          Already have an account?{' '}
          <Link href="/auth/login" className="text-blue-600 hover:underline">
            Login
          </Link>
        </div>
      </div>
    </main>
  );
}
