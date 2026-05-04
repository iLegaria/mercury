'use client';

import { useEffect, useRef, useState } from 'react';
import { useParams } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { useUser } from '@/context/UserContext';
import { quizApi } from '@/lib/quiz';
import FeedbackCard from '@/components/quiz/FeedbackCard';
import ResultsScreen from '@/components/quiz/ResultsScreen';
import type { QuizSession, AnswerFeedback } from '@/types';

type Phase = 'loading' | 'question' | 'streaming' | 'feedback' | 'complete';

export default function QuizSessionPage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const { userId } = useUser();
  const [session, setSession] = useState<QuizSession | null>(null);
  const [answer, setAnswer] = useState('');
  const [streamingText, setStreamingText] = useState('');
  const [feedback, setFeedback] = useState<AnswerFeedback | null>(null);
  const [phase, setPhase] = useState<Phase>('loading');
  const [error, setError] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const cancelStreamRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    quizApi.getSession(sessionId)
      .then(s => { setSession(s); setPhase(s.status === 'COMPLETED' ? 'complete' : 'question'); })
      .catch(() => setError('Session not found.'));
  }, [sessionId]);

  useEffect(() => {
    if (phase === 'question') {
      setAnswer('');
      setStreamingText('');
      textareaRef.current?.focus();
    }
  }, [phase]);

  // Cleanup stream on unmount
  useEffect(() => () => { cancelStreamRef.current?.(); }, []);

  function submitAnswer() {
    if (!answer.trim() || phase !== 'question') return;
    setPhase('streaming');
    setStreamingText('');
    setError('');

    const cancel = quizApi.submitAnswerStream(sessionId, userId, answer.trim(), {
      onToken: (token) => setStreamingText(prev => prev + token),
      onComplete: (fb) => {
        // Attach the accumulated text as the feedback body
        setStreamingText(prev => {
          const fullText = prev;
          setFeedback({ ...fb, feedback: fullText });
          return prev;
        });
        if (fb.sessionComplete) {
          setPhase('complete');
          setSession(prev => prev ? { ...prev, correctAnswers: fb.finalScore ?? 0, status: 'COMPLETED' } : prev);
        } else {
          setPhase('feedback');
        }
      },
      onError: () => {
        setError('Failed to submit answer.');
        setPhase('question');
      },
    });
    cancelStreamRef.current = cancel;
  }

  function handleNext() {
    if (!feedback?.nextQuestion) return;
    setSession(prev => prev ? { ...prev, currentQuestion: feedback.nextQuestion! } : prev);
    setFeedback(null);
    setPhase('question');
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); submitAnswer(); }
  }

  if (phase === 'loading') {
    return <div style={{ color: 'var(--text-muted-col)', fontSize: '11px', letterSpacing: '0.1em', paddingTop: '40px' }}>LOADING SESSION...</div>;
  }
  if (error && !session) {
    return <div style={{ color: 'var(--error)', fontSize: '12px' }}>{error}</div>;
  }

  const q = session?.currentQuestion;

  if (phase === 'complete' && session) {
    return <ResultsScreen correct={session.correctAnswers} total={session.totalQuestions} />;
  }

  return (
    <div style={{ maxWidth: '680px' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '32px' }}>
        <div>
          <div style={{ fontSize: '9px', letterSpacing: '0.22em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '4px' }}>
            LLM-Graded
          </div>
          <h1 style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '20px', fontWeight: 600, letterSpacing: '0.04em', color: 'var(--foreground)' }}>
            Quiz Session
          </h1>
        </div>
        {q && (
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '22px', fontWeight: 600, color: 'var(--mercury-accent)' }}>
              {q.questionIndex + 1}<span style={{ fontSize: '14px', color: 'var(--text-muted-col)' }}>/{q.totalQuestions}</span>
            </div>
            <div style={{ fontSize: '9px', color: 'var(--text-muted-col)', letterSpacing: '0.08em' }}>
              {session?.correctAnswers ?? 0} correct so far
            </div>
          </div>
        )}
      </div>

      {/* Progress bar */}
      {q && (
        <div style={{ height: '2px', background: '#1c1912', marginBottom: '32px', borderRadius: '1px', overflow: 'hidden' }}>
          <div style={{
            height: '100%',
            width: `${((q.questionIndex) / q.totalQuestions) * 100}%`,
            background: 'var(--mercury-accent)',
            transition: 'width 0.4s ease',
          }} />
        </div>
      )}

      {/* Question */}
      {q && (
        <div style={{
          background: 'var(--card)', border: '1px solid #121420',
          borderLeft: '2px solid var(--mercury-accent)',
          padding: '24px 28px', marginBottom: '20px', borderRadius: '0 2px 2px 0',
        }}>
          <div style={{ fontSize: '9px', letterSpacing: '0.18em', textTransform: 'uppercase', color: 'var(--text-muted-col)', marginBottom: '10px' }}>
            Question {q.questionIndex + 1}
          </div>
          <p style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '17px', fontWeight: 600, letterSpacing: '0.03em', color: 'var(--foreground)', lineHeight: 1.5, margin: 0 }}>
            {q.questionText}
          </p>
        </div>
      )}

      {/* Answer area */}
      {phase === 'question' && (
        <div>
          <label style={{ display: 'block', fontSize: '9px', letterSpacing: '0.16em', textTransform: 'uppercase', color: 'var(--text-muted-col)', marginBottom: '8px' }}>
            Your Answer
          </label>
          <textarea
            ref={textareaRef}
            value={answer}
            onChange={e => setAnswer(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Type your answer... (Enter to submit, Shift+Enter for new line)"
            rows={4}
            style={{
              width: '100%', background: 'var(--background)', border: '1px solid #1c1912',
              color: 'var(--foreground)', padding: '14px 16px', fontSize: '13px',
              fontFamily: 'inherit', outline: 'none', resize: 'vertical',
              lineHeight: 1.7, borderRadius: '2px', boxSizing: 'border-box',
            }}
          />
          {error && <p style={{ fontSize: '11px', color: 'var(--error)', marginTop: '8px' }}>{error}</p>}
          <button
            onClick={submitAnswer}
            disabled={!answer.trim()}
            style={{
              marginTop: '12px', padding: '10px 28px',
              border: '1px solid var(--mercury-accent)', color: 'var(--mercury-accent)',
              background: 'transparent', fontSize: '11px', letterSpacing: '0.08em', textTransform: 'uppercase',
              fontFamily: 'inherit',
              cursor: !answer.trim() ? 'not-allowed' : 'pointer',
              opacity: !answer.trim() ? 0.5 : 1,
              transition: 'all 0.2s', borderRadius: '2px',
            }}
          >
            Submit Answer →
          </button>
        </div>
      )}

      {/* Streaming: feedback appears token by token, no verdict yet */}
      {phase === 'streaming' && (
        <div style={{
          border: '1px solid #1c1912',
          background: 'var(--card)',
          padding: '24px 28px',
          borderRadius: '2px',
          marginTop: '4px',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
            <Loader2 size={14} style={{ color: 'var(--mercury-accent)', animation: 'spin 1s linear infinite' }} />
            <span style={{ fontSize: '9px', letterSpacing: '0.18em', textTransform: 'uppercase', color: 'var(--mercury-accent)' }}>
              Evaluating
            </span>
          </div>
          <p style={{ fontSize: '12.5px', color: 'var(--foreground)', lineHeight: 1.8, margin: 0, whiteSpace: 'pre-wrap' }}>
            {streamingText}
            <span style={{ display: 'inline-block', width: '2px', height: '14px', background: 'var(--mercury-accent)', marginLeft: '2px', verticalAlign: 'text-bottom', animation: 'pulse 1s ease-in-out infinite' }} />
          </p>
        </div>
      )}

      {/* Feedback: verdict + colored card */}
      {phase === 'feedback' && feedback && (
        <FeedbackCard feedback={feedback} onNext={handleNext} />
      )}
    </div>
  );
}
