import type { FlashcardCard } from '@/types';

interface Props {
  card: FlashcardCard;
  onReview: (quality: number) => void;
  disabled: boolean;
}

function sm2Preview(card: FlashcardCard, quality: number): number {
  if (quality < 3) return 1;
  if (card.repetitions === 0) return 1;
  if (card.repetitions === 1) return 6;
  return Math.max(1, Math.round(card.intervalDays * card.easeFactor));
}

const BUTTONS = [
  { quality: 0, label: 'Again',  color: 'var(--error)' },
  { quality: 2, label: 'Hard',   color: 'var(--warning)' },
  { quality: 4, label: 'Good',   color: 'var(--mercury-accent)' },
  { quality: 5, label: 'Easy',   color: 'var(--success)' },
] as const;

export default function ReviewControls({ card, onReview, disabled }: Props) {
  return (
    <div>
      <div style={{ fontSize: '9px', letterSpacing: '0.18em', textTransform: 'uppercase', color: 'var(--text-muted-col)', marginBottom: '10px', textAlign: 'center' }}>
        How well did you know this?
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '8px' }}>
        {BUTTONS.map(({ quality, label, color }) => {
          const days = sm2Preview(card, quality);
          return (
            <button
              key={quality}
              onClick={() => onReview(quality)}
              disabled={disabled}
              style={{
                padding: '12px 8px',
                border: `1px solid ${color}33`,
                background: `${color}0d`,
                color,
                fontFamily: 'inherit',
                cursor: disabled ? 'not-allowed' : 'pointer',
                opacity: disabled ? 0.5 : 1,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '4px',
                transition: 'all 0.15s',
                borderRadius: '2px',
              }}
            >
              <span style={{ fontSize: '11px', fontWeight: 500, letterSpacing: '0.04em' }}>{label}</span>
              <span style={{ fontSize: '9px', color: 'var(--text-muted-col)', letterSpacing: '0.04em' }}>
                in {days}d
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
