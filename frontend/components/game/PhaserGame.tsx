'use client';

import { useEffect, useRef, useCallback } from 'react';

interface GameClearedData {
  score: number;
  clearTimeMs: number;
  intimacyPoints: number;
}

interface PhaserGameProps {
  sessionId: string;
  token: string;
  onGameCleared?: (data: GameClearedData) => void;
  onGameOver?: (data: { reason: string; score: number; elapsedTimeMs: number }) => void;
}

export default function PhaserGame({ sessionId, token, onGameCleared, onGameOver }: PhaserGameProps) {
  const gameContainerRef = useRef<HTMLDivElement>(null);
  const gameInstanceRef = useRef<Phaser.Game | null>(null);

  const handleGameCleared = useCallback((e: Event) => {
    const customEvent = e as CustomEvent<GameClearedData>;
    onGameCleared?.(customEvent.detail);
  }, [onGameCleared]);

  const handleGameOver = useCallback((e: Event) => {
    const customEvent = e as CustomEvent<{ reason: string; score: number; elapsedTimeMs: number }>;
    onGameOver?.(customEvent.detail);
  }, [onGameOver]);

  useEffect(() => {
    // Listen for game events from Phaser
    window.addEventListener('game-cleared', handleGameCleared);
    window.addEventListener('game-over', handleGameOver);

    return () => {
      window.removeEventListener('game-cleared', handleGameCleared);
      window.removeEventListener('game-over', handleGameOver);
    };
  }, [handleGameCleared, handleGameOver]);

  useEffect(() => {
    if (!gameContainerRef.current || gameInstanceRef.current) return;

    // Set URL params for Phaser to read (sessionId, token)
    const url = new URL(window.location.href);
    url.searchParams.set('sessionId', sessionId);
    url.searchParams.set('token', token);
    window.history.replaceState({}, '', url.toString());

    // Dynamically import Phaser (SSR-safe)
    const initGame = async () => {
      const Phaser = (await import('phaser')).default;
      const { default: Level } = await import('@/game/scenes/Level');
      const { default: Preload } = await import('@/game/scenes/Preload');
      const { default: Lobby } = await import('@/game/scenes/Lobby');

      class Boot extends Phaser.Scene {
        constructor() { super('Boot'); }
        preload() {
          this.load.pack('pack', '/game/assets/preload-asset-pack.json');
        }
        create() { this.scene.start('Preload'); }
      }

      const game = new Phaser.Game({
        width: 390,
        height: 844,
        backgroundColor: '#2f2f2f',
        parent: gameContainerRef.current!,
        pixelArt: true,
        antialias: false,
        roundPixels: true,
        physics: {
          default: 'arcade',
          arcade: { gravity: { x: 0, y: 800 }, debug: false },
        },
        scale: {
          mode: Phaser.Scale.FIT,
          autoCenter: Phaser.Scale.CENTER_BOTH,
        },
        scene: [Boot, Lobby, Preload, Level],
      });

      gameInstanceRef.current = game;
    };

    initGame();

    return () => {
      if (gameInstanceRef.current) {
        gameInstanceRef.current.destroy(true);
        gameInstanceRef.current = null;
      }
    };
  }, [sessionId, token]);

  return (
    <div
      ref={gameContainerRef}
      id="game-container"
      style={{ width: '100%', maxWidth: '390px', margin: '0 auto' }}
    />
  );
}
