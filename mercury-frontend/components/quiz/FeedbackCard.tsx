'use client';

import { CheckCircle, XCircle, AlertCircle } from 'lucide-react';
import type { AnswerFeedback, QuizVerdict } from '@/types';

interface Props {
  feedback: AnswerFeedback;
  onNext: () => void;
}

const VERDICT_CONFIG: Record<QuizVerdict, {
  icon: React.ElementType;
  label: string;
  border: string;
  bg: string;
  color: string;
}> = {
  CORRECT: {
    icon: CheckCircle,
    label: 'Correct',
    border: 'rgba(74,158,107,0.3)',
    bg: 'rgba(74,158,107,0.05)',
    color: 'var(--success)',
  },
  PARTIALLY_CORRECT: {
    icon: AlertCircle,
    label: 'Partially Correct',
    border: 'rgba(200,150,50,0.35)',
    bg: 'rgba(200,150,50,0.06)',
    color: '#c8a04a',
  },
  INCORRECT: {
    icon: XCircle,
    label: 'Incorrect',
    border: 'rgba(192,80,74,0.3)',
    bg: 'rgba(192,80,74,0.05)',
    color: 'var(--error)',
  },
};

export default function FeedbackCard({ feedback, onNext }: Props) {
  const verdict: QuizVerdict = feedback.verdict ?? (feedback.isCorrect ? 'CORRECT' : 'INCORRECT');
  const cfg = VERDICT_CONFIG[verdict];
  const Icon = cfg.icon;

  return (
    <div style={{
      border: `1px solid ${cfg.border}`,
      background: cfg.bg,
      padding: '24px 28px',
      borderRadius: '2px',
      marginTop: '20px',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
        <Icon size={16} style={{ color: cfg.color, flexShrink: 0 }} />
        <span style={{
          fontFamily: 'var(--font-cinzel), Cinzel, serif',
          fontSize: '13px', fontWeight: 600, letterSpacing: '0.06em',
          color: cfg.color,
        }}>
          {cfg.label}
        </span>
      </div>

      <p style={{ fontSize: '12.5px', color: 'var(--foreground)', lineHeight: 1.8, margin: 0, whiteSpace: 'pre-wrap' }}>
        {feedback.feedback}
      </p>

      {!feedback.sessionComplete && (
        <button
          onClick={onNext}
          style={{
            marginTop: '20px',
            padding: '9px 22px',
            border: '1px solid var(--mercury-accent)',
            color: 'var(--mercury-accent)',
            background: 'transparent',
            fontSize: '11px', letterSpacing: '0.08em', textTransform: 'uppercase',
            fontFamily: 'inherit', cursor: 'pointer', borderRadius: '2px',
          }}
        >
          Next Question →
        </button>
      )}
    </div>
  );
}
