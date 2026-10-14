'use client';

import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { fetchMe, logoutRequest } from '../../lib/api';
import type { User } from '../../lib/api';

interface AuthContextValue {
  user: User | null;
  /** True until the initial GET /auth/me hydration settles. */
  loading: boolean;
  /** A failed account check is distinct from a confirmed signed-out session. */
  error: string;
  setUser: (user: User | null) => void;
  retry: () => Promise<void>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue>({
  user: null,
  loading: true,
  error: '',
  setUser: () => {},
  retry: async () => {},
  signOut: async () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUserState] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const setUser = useCallback((nextUser: User | null) => {
    setUserState(nextUser);
    if (nextUser) setError('');
  }, []);

  const retry = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setUser(await fetchMe());
    } catch {
      setError('We could not verify your account. Check your connection and try again.');
    } finally {
      setLoading(false);
    }
  }, [setUser]);

  useEffect(() => {
    void retry();
  }, [retry]);

  const signOut = useCallback(async () => {
    try {
      await logoutRequest();
    } catch {
      // Session already gone or backend offline — clear local state regardless.
    }
    setUser(null);
  }, [setUser]);

  return (
    <AuthContext.Provider value={{ user, loading, error, setUser, retry, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  return useContext(AuthContext);
}
