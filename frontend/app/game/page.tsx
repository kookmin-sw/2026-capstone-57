'use client';

import { useSearchParams, useRouter } from 'next/navigation';
import { useState, Suspense } from 'react';
import dynamic from 'next/dynamic';

// Dynamic import to avoid SSR issues with Phaser
const PhaserGame = dynamic(() => import('@/components/game/PhaserGame'), {
  ssr: false,
  loading: () => (
    <div className="flex items-center justify-center h-screen">
      <p className="text-lg">게임 로딩 중...</p>
    </div>
  ),
});

interface GameResult {
  type: 'cleared' | 'gameover';
  score: number;
  clearTimeMs?: number;
  intimacyPoints?: number;
  reason?: string;
  elapsedTimeMs?: number;
}

function GameContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const [gameResult, setGameResult] = useState<GameResult | null>(null);

  const sessionId = searchParams.get('sessionId') || '';
  const token = searchParams.get('token') || '';

  if (!sessionId || !token) {
    return (
      <div className="flex items-center justify-center h-screen">
        <p className="text-red-500">세션 정보가 없습니다.</p>
      </div>
    );
  }

  if (gameResult) {
    return (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <div className="bg-white rounded-2xl p-6 mx-4 max-w-sm w-full text-center">
          {gameResult.type === 'cleared' ? (
            <>
              <h2 className="text-2xl font-bold mb-4">🎉 게임 클리어!</h2>
              <div className="space-y-2 mb-6">
                <p>점수: <span className="font-bold">{gameResult.score}</span></p>
                <p>클리어 시간: <span className="font-bold">{Math.round((gameResult.clearTimeMs || 0) / 1000)}초</span></p>
                <p>친밀도 포인트: <span className="font-bold">+{gameResult.intimacyPoints}</span></p>
              </div>
            </>
          ) : (
            <>
              <h2 className="text-2xl font-bold mb-4">게임 종료</h2>
              <div className="space-y-2 mb-6">
                <p>사유: {gameResult.reason === 'TIMEOUT' ? '시간 초과' : '연결 끊김'}</p>
                <p>점수: <span className="font-bold">{gameResult.score}</span></p>
              </div>
            </>
          )}
          <button
            onClick={() => router.back()}
            className="w-full py-3 bg-blue-500 text-white rounded-lg font-medium"
          >
            돌아가기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="h-screen w-screen flex items-center justify-center bg-gray-900">
      <PhaserGame
        sessionId={sessionId}
        token={token}
        onGameCleared={(data) => {
          setGameResult({
            type: 'cleared',
            score: data.score,
            clearTimeMs: data.clearTimeMs,
            intimacyPoints: data.intimacyPoints,
          });
        }}
        onGameOver={(data) => {
          setGameResult({
            type: 'gameover',
            score: data.score,
            reason: data.reason,
            elapsedTimeMs: data.elapsedTimeMs,
          });
        }}
      />
    </div>
  );
}

export default function GamePage() {
  return (
    <Suspense fallback={<div className="flex items-center justify-center h-screen"><p>로딩 중...</p></div>}>
      <GameContent />
    </Suspense>
  );
}
