package a_turtleplayer;

import aic2024.user.*;

import a_turtleplayer.util.Util;

public class HqPlayer extends BasePlayer {
    final int MIN_OXYGEN = (int) GameConstants.MIN_OXYGEN_ASTRONAUT + 1;

    HqPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        final float VISION = uc.getType().getVisionRange();
        while (true) {
            final AstronautInfo[] enemies = uc.senseAstronauts(VISION, uc.getOpponent());
            broadcastEnemies(enemies);
            if (enemies.length > 0) {
                buildShield();
            } else if (uc.getRound() % 4 == 0) {
                Direction d = Direction.values()[(int)(uc.getRandomDouble() * 8)];
                if (uc.canEnlistAstronaut(d, MIN_OXYGEN, null)) {
                    uc.enlistAstronaut(d, MIN_OXYGEN, null);
                }
            }

            uc.yield();
        }
    }

    void buildShield() {
        for (Direction d : Direction.values()) {
            if (uc.canEnlistAstronaut(d, MIN_OXYGEN, null)) {
                uc.enlistAstronaut(d, MIN_OXYGEN, null);
            }
        }
    }

    void broadcastEnemies(AstronautInfo[] enemies) {
        uc.cleanBroadcastBuffer();
        for (int i = 500; i >= 0; --i) {
            if (uc.canPerformAction(ActionType.BROADCAST, null, enemies.length)) {
                uc.performAction(ActionType.BROADCAST, null, enemies.length);
            }
        }
    }
}
