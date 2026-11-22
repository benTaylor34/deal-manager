import { createContext, useContext, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';

/**
 * DEMO-ONLY: the backend has two in-memory users so you can switch roles from the header.
 * A real deployment would sign in through the bank's identity provider instead.
 */
export type Role = 'RM' | 'PRICING_COMMITTEE';

export interface DemoUser {
  username: string;
  password: string;
  role: Role;
  label: string;
}

export const DEMO_USERS: DemoUser[] = [
  { username: 'rm.user', password: 'demo', role: 'RM', label: 'Relationship Manager' },
  { username: 'committee.user', password: 'demo', role: 'PRICING_COMMITTEE', label: 'Pricing Committee' },
];

const STORAGE_KEY = 'deal-manager.user';

function initialUser(): DemoUser {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    const found = DEMO_USERS.find((u) => u.username === saved);
    if (found) return found;
  } catch {
    // storage unavailable: fall through to default
  }
  return DEMO_USERS[0];
}

let currentAuthHeader = basic(initialUser());

function basic(user: DemoUser) {
  return 'Basic ' + btoa(`${user.username}:${user.password}`);
}

export function authHeader() {
  return currentAuthHeader;
}

interface CurrentUserContextValue {
  user: DemoUser;
  switchUser: (username: string) => void;
}

const CurrentUserContext = createContext<CurrentUserContextValue | null>(null);

export function CurrentUserProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<DemoUser>(initialUser);
  const queryClient = useQueryClient();

  const switchUser = (username: string) => {
    const next = DEMO_USERS.find((u) => u.username === username) ?? DEMO_USERS[0];
    currentAuthHeader = basic(next);
    try {
      localStorage.setItem(STORAGE_KEY, next.username);
    } catch {
      // ignore
    }
    setUser(next);
    queryClient.invalidateQueries();
  };

  return <CurrentUserContext.Provider value={{ user, switchUser }}>{children}</CurrentUserContext.Provider>;
}

export function useCurrentUser() {
  const ctx = useContext(CurrentUserContext);
  if (!ctx) throw new Error('useCurrentUser must be used inside CurrentUserProvider');
  return ctx;
}
