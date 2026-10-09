package com.example.adminquiz.models;

/** Deterministic persona rules (no AI needed). Rules are checked in priority order. */
public final class PersonaCalculator {
    private PersonaCalculator() {
    }

    public static Persona pick(double accuracy, double speed, double consistency,
                               double firstHalfAcc, double secondHalfAcc, double avgDifficulty, int questions) {
        if (questions >= 3 && accuracy >= 0.999) {
            return Persona.PERFECTIONIST;
        }
        if (questions >= 4 && firstHalfAcc >= 0 && firstHalfAcc <= 0.5
                && secondHalfAcc >= 0.8 && secondHalfAcc - firstHalfAcc >= 0.3) {
            return Persona.COMEBACK_KING;
        }
        if (speed >= 0.6 && accuracy >= 0.6) {
            return Persona.SPEED_DEMON;
        }
        if (accuracy >= 0.85 && avgDifficulty >= 2.0) {
            return Persona.KNOWLEDGE_MACHINE;
        }
        if (speed <= 0.35 && accuracy >= 0.7) {
            return Persona.DEEP_THINKER;
        }
        if (consistency >= 0.7 && accuracy >= 0.6) {
            return Persona.STRATEGIST;
        }
        if (accuracy >= 0.5 && consistency < 0.4) {
            return Persona.LUCKY_GENIUS;
        }
        if (accuracy < 0.5 || speed >= 0.6) {
            return Persona.RISK_TAKER;
        }
        return Persona.STRATEGIST;
    }
}
