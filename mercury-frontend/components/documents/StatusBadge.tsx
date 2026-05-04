import type { DocumentStatus } from '@/types';

const CONFIG: Record<DocumentStatus, { label: string; bg: string; color: string; spin?: boolean }> = {
  PENDING:    { label: 'PENDING',    bg: 'var(--status-pending)',    color: 'var(--cold)' },
  PROCESSING: { label: 'PROCESSING', bg: 'var(--status-processing)', color: 'var(--warning)', spin: true },
  COMPLETED:  { label: 'COMPLETED',  bg: 'var(--status-completed)',  color: 'var(--success)' },
  FAILED:     { label: 'FAILED',     bg: 'var(--status-failed)',     color: 'var(--error)' },
};

export default function StatusBadge({ status }: { status: DocumentStatus }) {
  const { label, bg, color, spin } = CONFIG[status] ?? CONFIG.PENDING;
  return (
    <span style={{
      display: 'inline-flex',
      alignItems: 'center',
      gap: '5px',
      padding: '2px 8px',
      borderRadius: '2px',
      background: bg,
      color,
      fontSize: '9px',
      letterSpacing: '0.1em',
      fontWeight: 500,
    }}>
      {spin && (
        <svg width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
          style={{ animation: 'spin 1s linear infinite', flexShrink: 0 }}>
          <path d="M21 12a9 9 0 1 1-6.219-8.56" />
        </svg>
      )}
      {label}
    </span>
  );
}
