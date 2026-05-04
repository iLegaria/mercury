import Link from 'next/link';

interface Props {
  correct: number;
  total: number;
}

export default function ResultsScreen({ correct, total }: Props) {
  const pct = total > 0 ? Math.round((correct / total) * 100) : 0;
  const passed = pct >= 60;

  return (
    <div style={{ maxWidth: '480px', margin: '0 auto', textAlign: 'center', paddingTop: '32px' }}>
      <div style={{ fontSize: '9px', letterSpacing: '0.24em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '12px' }}>
        Quiz Complete
      </div>
      <div style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '64px', fontWeight: 700, color: passed ? 'var(--mercury-accent)' : 'var(--error)', lineHeight: 1, marginBottom: '8px' }}>
        {correct}<span style={{ fontSize: '32px', color: 'var(--text-muted-col)' }}>/{total}</span>
      </div>
      <div style={{ fontSize: '12px', color: 'var(--text-muted-col)', marginBottom: '28px', letterSpacing: '0.04em' }}>
        {pct}% correct
      </div>

      {/* Progress bar */}
      <div style={{ height: '3px', background: '#1c1912', borderRadius: '2px', marginBottom: '36px', overflow: 'hidden' }}>
        <div style={{
          height: '100%',
          width: `${pct}%`,
          background: passed ? 'var(--mercury-accent)' : 'var(--error)',
          transition: 'width 0.8s ease',
        }} />
      </div>

      <p style={{ fontSize: '12px', color: 'var(--text-muted-col)', marginBottom: '32px', lineHeight: 1.8 }}>
        {pct === 100 ? 'Perfect score. The knowledge is yours.' :
         passed ? 'Good work. Review your documents to close the gaps.' :
         'Keep studying. Upload more material and try again.'}
      </p>

      <div style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}>
        <Link href="/quiz" style={{
          padding: '10px 24px', border: '1px solid var(--mercury-accent)', color: 'var(--mercury-accent)',
          textDecoration: 'none', fontSize: '11px', letterSpacing: '0.08em', textTransform: 'uppercase', borderRadius: '2px',
        }}>
          Try Again →
        </Link>
        <Link href="/documents" style={{
          padding: '10px 24px', border: '1px solid #1c1912', color: 'var(--text-muted-col)',
          textDecoration: 'none', fontSize: '11px', letterSpacing: '0.08em', textTransform: 'uppercase', borderRadius: '2px',
        }}>
          Documents
        </Link>
      </div>
    </div>
  );
}
