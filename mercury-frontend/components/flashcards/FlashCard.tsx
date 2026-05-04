import type { FlashcardCard } from '@/types';

interface Props {
  card: FlashcardCard;
  flipped: boolean;
  onReveal: () => void;
}

export default function FlashCard({ card, flipped, onReveal }: Props) {
  return (
    <div className="card-scene" style={{ width: '100%', height: '280px', marginBottom: '24px' }}>
      <div className={`card-inner${flipped ? ' flipped' : ''}`}>
        {/* Front — question */}
        <div className="card-face" style={{
          background: 'var(--card)',
          border: '1px solid #121420',
          borderLeft: '2px solid var(--mercury-accent)',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '36px 40px',
          textAlign: 'center',
          cursor: flipped ? 'default' : 'pointer',
        }}>
          <div style={{ fontSize: '9px', letterSpacing: '0.2em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '20px' }}>
            Question
          </div>
          <p style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '17px', fontWeight: 600, letterSpacing: '0.03em', color: 'var(--foreground)', lineHeight: 1.6, margin: '0 0 28px' }}>
            {card.question}
          </p>
          {!flipped && (
            <button
              onClick={onReveal}
              style={{
                padding: '9px 22px',
                border: '1px solid #1c1912',
                color: 'var(--text-muted-col)',
                background: 'transparent',
                fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase',
                fontFamily: 'inherit', cursor: 'pointer', borderRadius: '2px',
                transition: 'all 0.2s',
              }}
            >
              Reveal Answer
            </button>
          )}
        </div>

        {/* Back — question + answer */}
        <div className="card-face card-back-face" style={{
          background: 'var(--card)',
          border: '1px solid #121420',
          borderLeft: '2px solid var(--success)',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          padding: '28px 40px',
          overflowY: 'auto',
        }}>
          <div style={{ fontSize: '9px', letterSpacing: '0.2em', textTransform: 'uppercase', color: 'var(--text-muted-col)', marginBottom: '8px' }}>
            Question
          </div>
          <p style={{ fontSize: '12px', color: 'var(--text-muted-col)', marginBottom: '20px', lineHeight: 1.6 }}>
            {card.question}
          </p>
          <div style={{ height: '1px', background: '#1c1912', marginBottom: '20px' }} />
          <div style={{ fontSize: '9px', letterSpacing: '0.2em', textTransform: 'uppercase', color: 'var(--success)', marginBottom: '10px' }}>
            {card.answer.includes('|') ? 'Accepted Answers' : 'Answer'}
          </div>
          {card.answer.includes('|') ? (
            <ul style={{ margin: 0, padding: 0, listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '6px' }}>
              {card.answer.split('|').map(a => a.trim()).filter(Boolean).map((a, i) => (
                <li key={i} style={{ display: 'flex', alignItems: 'baseline', gap: '8px', fontSize: '13px', color: 'var(--foreground)', lineHeight: 1.5 }}>
                  <span style={{ flexShrink: 0, width: '16px', height: '16px', borderRadius: '50%', background: 'var(--accent-dim)', border: '1px solid var(--border-accent)', color: 'var(--mercury-accent)', fontSize: '9px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{i + 1}</span>
                  {a}
                </li>
              ))}
            </ul>
          ) : (
            <p style={{ fontSize: '14px', color: 'var(--foreground)', lineHeight: 1.7, margin: 0 }}>
              {card.answer}
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
