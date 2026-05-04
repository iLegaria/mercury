'use client';

import { useState, useEffect, ReactNode, useCallback } from 'react';
import { UserContext } from '@/context/UserContext';
import { usersApi } from '@/lib/users';
import { ApiError } from '@/lib/api';

const STORAGE_KEY = 'mercury_user';

interface StoredUser { id: string; name: string; email: string }

function load(): StoredUser | null {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null'); } catch { return null; }
}
function save(u: StoredUser) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(u));
}

type Phase = 'loading' | 'setup' | 'ready';
type Tab = 'create' | 'login';

const inputStyle: React.CSSProperties = {
  width: '100%',
  background: 'var(--background)',
  border: '1px solid #121420',
  color: 'var(--foreground)',
  padding: '10px 14px',
  fontSize: '13px',
  fontFamily: 'inherit',
  outline: 'none',
  borderRadius: '2px',
  boxSizing: 'border-box',
};

const labelStyle: React.CSSProperties = {
  display: 'block',
  fontSize: '9px',
  letterSpacing: '0.16em',
  textTransform: 'uppercase',
  color: 'var(--text-muted-col)',
  marginBottom: '6px',
};

export default function UserGuard({ children }: { children: ReactNode }) {
  const [phase, setPhase] = useState<Phase>('loading');
  const [user, setUser] = useState<StoredUser | null>(null);
  const [tab, setTab] = useState<Tab>('create');
  const [form, setForm] = useState({ name: '', email: '' });
  const [loginId, setLoginId] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const stored = load();
    if (!stored) { setPhase('setup'); return; }
    usersApi.get(stored.id)
      .then(() => { setUser(stored); setPhase('ready'); })
      .catch(e => {
        if (e instanceof ApiError && e.status === 404) {
          localStorage.removeItem(STORAGE_KEY);
          setPhase('setup');
        } else {
          setUser(stored);
          setPhase('ready');
        }
      });
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY);
    setUser(null);
    setForm({ name: '', email: '' });
    setLoginId('');
    setError('');
    setTab('create');
    setPhase('setup');
  }, []);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const created = await usersApi.create(form.email, form.name);
      const stored: StoredUser = { id: created.id, name: created.name, email: created.email };
      save(stored);
      setUser(stored);
      setPhase('ready');
    } catch {
      setError('Could not connect to backend. Make sure it is running.');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const found = await usersApi.get(loginId.trim());
      const stored: StoredUser = { id: found.id, name: found.name, email: found.email };
      save(stored);
      setUser(stored);
      setPhase('ready');
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) {
        setError('User ID not found.');
      } else {
        setError('Could not connect to backend. Make sure it is running.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (phase === 'loading') {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '60vh', color: 'var(--text-muted-col)', fontSize: '11px', letterSpacing: '0.1em' }}>
        INITIALIZING...
      </div>
    );
  }

  if (phase === 'setup') {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh' }}>
        <div style={{
          background: 'var(--card)',
          border: '1px solid #121420',
          padding: '40px',
          width: '100%',
          maxWidth: '400px',
          borderRadius: '4px',
        }}>
          <div style={{ fontSize: '9px', letterSpacing: '0.22em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '8px' }}>
            {tab === 'create' ? 'First Launch' : 'Welcome Back'}
          </div>
          <h2 style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '20px', fontWeight: 600, letterSpacing: '0.04em', color: 'var(--foreground)', marginBottom: '20px' }}>
            Enter Mercury
          </h2>

          {/* Tabs */}
          <div style={{ display: 'flex', gap: '0', marginBottom: '24px', borderBottom: '1px solid #121420' }}>
            {(['create', 'login'] as Tab[]).map(t => (
              <button
                key={t}
                onClick={() => { setTab(t); setError(''); }}
                style={{
                  flex: 1,
                  padding: '8px',
                  border: 'none',
                  borderBottom: tab === t ? '2px solid var(--mercury-accent)' : '2px solid transparent',
                  background: 'transparent',
                  color: tab === t ? 'var(--mercury-accent)' : 'var(--text-muted-col)',
                  fontSize: '9px',
                  letterSpacing: '0.14em',
                  textTransform: 'uppercase',
                  cursor: 'pointer',
                  fontFamily: 'inherit',
                  marginBottom: '-1px',
                  transition: 'color 0.2s',
                }}
              >
                {t === 'create' ? 'New Account' : 'Existing Account'}
              </button>
            ))}
          </div>

          {tab === 'create' ? (
            <form onSubmit={handleCreate} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <label style={labelStyle}>Name</label>
                <input type="text" required value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} placeholder="Your name" style={inputStyle} />
              </div>
              <div>
                <label style={labelStyle}>Email</label>
                <input type="email" required value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} placeholder="you@example.com" style={inputStyle} />
              </div>
              {error && <p style={{ fontSize: '11px', color: 'var(--error)', margin: 0 }}>{error}</p>}
              <button type="submit" disabled={submitting} style={submitBtnStyle(submitting)}>
                {submitting ? 'Connecting...' : 'Create →'}
              </button>
            </form>
          ) : (
            <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <label style={labelStyle}>User ID</label>
                <input type="text" required value={loginId} onChange={e => setLoginId(e.target.value)} placeholder="Paste your user ID" style={inputStyle} />
              </div>
              {error && <p style={{ fontSize: '11px', color: 'var(--error)', margin: 0 }}>{error}</p>}
              <button type="submit" disabled={submitting} style={submitBtnStyle(submitting)}>
                {submitting ? 'Verifying...' : 'Login →'}
              </button>
            </form>
          )}
        </div>
      </div>
    );
  }

  return (
    <UserContext.Provider value={{ userId: user!.id, userName: user!.name, logout }}>
      {children}
    </UserContext.Provider>
  );
}

function submitBtnStyle(submitting: boolean): React.CSSProperties {
  return {
    marginTop: '8px',
    padding: '11px 24px',
    border: '1px solid var(--mercury-accent)',
    color: 'var(--mercury-accent)',
    background: 'transparent',
    fontSize: '11px',
    letterSpacing: '0.08em',
    textTransform: 'uppercase',
    fontFamily: 'inherit',
    cursor: submitting ? 'wait' : 'pointer',
    opacity: submitting ? 0.6 : 1,
    transition: 'all 0.2s',
    borderRadius: '2px',
  };
}
