'use client';

import { useEffect, useState } from 'react';
import { Copy, Check, LogOut } from 'lucide-react';

const STORAGE_KEY = 'mercury_user';

export default function UserBadge() {
  const [user, setUser] = useState<{ id: string; name: string } | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) setUser(JSON.parse(raw));
    } catch {}
  }, []);

  if (!user) return null;

  function copyId() {
    navigator.clipboard.writeText(user!.id).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  }

  function logout() {
    localStorage.removeItem(STORAGE_KEY);
    window.location.reload();
  }

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
      <span style={{ fontSize: '10px', color: 'var(--text-muted-col)', letterSpacing: '0.08em' }}>
        {user.name}
      </span>
      <button
        onClick={copyId}
        title="Copy User ID for browser extension"
        style={iconBtnStyle(copied ? '#4a9e6b' : undefined)}
      >
        {copied ? <Check size={10} /> : <Copy size={10} />}
        {copied ? 'Copied!' : 'Copy ID'}
      </button>
      <button
        onClick={logout}
        title="Logout"
        style={iconBtnStyle()}
      >
        <LogOut size={10} />
        Logout
      </button>
    </div>
  );
}

function iconBtnStyle(color?: string): React.CSSProperties {
  return {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '4px',
    padding: '4px 8px',
    border: '1px solid #1c1912',
    borderRadius: '2px',
    background: 'transparent',
    color: color ?? 'var(--text-muted-col)',
    fontSize: '9px',
    letterSpacing: '0.1em',
    textTransform: 'uppercase',
    cursor: 'pointer',
    fontFamily: 'inherit',
    transition: 'color 0.2s, border-color 0.2s',
  };
}
