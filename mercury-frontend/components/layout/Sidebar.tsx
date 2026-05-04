'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { LayoutDashboard, FileText, MessageSquare, Brain, CreditCard, Scissors } from 'lucide-react';

const NAV = [
  { href: '/',            label: 'Dashboard',  icon: LayoutDashboard },
  { href: '/documents',   label: 'Documents',  icon: FileText },
  { href: '/ask',         label: 'Ask',        icon: MessageSquare },
  { href: '/quiz',        label: 'Quiz',       icon: Brain },
  { href: '/flashcards',  label: 'Flashcards', icon: CreditCard },
  { href: '/snippets',    label: 'Snippets',   icon: Scissors },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="layout-sidebar" style={{
      width: '220px',
      flexShrink: 0,
      borderRight: '1px solid #121420',
      background: 'rgba(13,11,18,0.6)',
      backdropFilter: 'blur(8px)',
      overflowY: 'auto',
    }}>
      <nav style={{ padding: '24px 0' }}>
        <div style={{ padding: '0 16px', marginBottom: '8px', fontSize: '9px', letterSpacing: '0.2em', textTransform: 'uppercase', color: 'var(--text-muted-col)' }}>
          Navigation
        </div>
        {NAV.map(({ href, label, icon: Icon }) => {
          const active = pathname === href || (href !== '/' && pathname.startsWith(href));
          return (
            <Link
              key={href}
              href={href}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                padding: '10px 16px',
                margin: '2px 8px',
                borderRadius: '4px',
                fontSize: '12px',
                letterSpacing: '0.04em',
                textDecoration: 'none',
                color: active ? 'var(--mercury-accent)' : '#9e9678',
                background: active ? 'var(--accent-dim)' : 'transparent',
                borderLeft: active ? '2px solid var(--mercury-accent)' : '2px solid transparent',
                transition: 'all 0.2s',
              }}
            >
              <Icon size={14} />
              {label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
