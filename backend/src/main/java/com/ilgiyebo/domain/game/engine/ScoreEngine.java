package com.ilgiyebo.domain.game.engine;

import org.springframework.stereotype.Component;

@Component
public class ScoreEngine {

    // Base score for completing the game
    private static final int BASE_CLEAR_SCORE = 500;

    // Points per cooperation action
    private static final int COOPERATION_BONUS = 50;

    // Time bonus multiplier (points per second remaining)
    private static final double TIME_BONUS_PER_SECOND = 2.0;

    // Maximum intimacy points achievable
    private static final int MAX_INTIMACY_POINTS = 100;

    // Score threshold for maximum intimacy
    private static final int MAX_SCORE_THRESHOLD = 1000;

    /**
     * Updates the current score based on cooperation count and remaining time.
     * Called every tick to keep score current.
     */
    public void updateScore(GameState state) {
        int cooperationScore = state.getCooperationCount() * COOPERATION_BONUS;
        double timeBonus = (state.getRemainingTimeMs() / 1000.0) * TIME_BONUS_PER_SECOND;
        state.setScore(cooperationScore + (int) timeBonus);
    }

    /**
     * Calculates the final score when the game is cleared.
     */
    public int calculateFinalScore(GameState state) {
        int cooperationScore = state.getCooperationCount() * COOPERATION_BONUS;
        double timeBonus = (state.getRemainingTimeMs() / 1000.0) * TIME_BONUS_PER_SECOND;
        return BASE_CLEAR_SCORE + cooperationScore + (int) timeBonus;
    }

    /**
     * Calculates a partial score on timeout (no clear bonus).
     */
    public int calculatePartialScore(GameState state) {
        return state.getCooperationCount() * COOPERATION_BONUS;
    }

    /**
     * Converts game score and clear time to intimacy points.
     * Higher score and faster clear time yield more intimacy points.
     */
    public int calculateIntimacyPoints(int score, long clearTimeMs) {
        // Score-based component (0 to 70 points)
        double scoreRatio = Math.min((double) score / MAX_SCORE_THRESHOLD, 1.0);
        int scorePoints = (int) (scoreRatio * 70);

        // Time-based component (0 to 30 points, faster = more points)
        // Assume 180 seconds is the max time limit
        double maxTimeMs = 180_000.0;
        double timeRatio = Math.max(0, 1.0 - (clearTimeMs / maxTimeMs));
        int timePoints = (int) (timeRatio * 30);

        return Math.min(scorePoints + timePoints, MAX_INTIMACY_POINTS);
    }
}
