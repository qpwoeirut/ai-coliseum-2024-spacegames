package c_combinedplayer;

import aic2024.user.*;

public class HqPlayer extends BasePlayer {
    final int MIN_OXYGEN = (int) GameConstants.MIN_OXYGEN_ASTRONAUT + 1;

    HqPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        final float VISION = uc.getType().getVisionRange();

        final Location[] oppHQLoc = findOppHQ();
        double sum = 0;
        for (int i = 0; i < oppHQLoc.length; i++) {
            sum += Math.sqrt(oppHQLoc[i].distanceSquared(uc.getLocation()));
        }
        final double average = sum / ((double) oppHQLoc.length);

        while (true) {
            int turnNumber = uc.getRound();
            final AstronautInfo[] enemies = uc.senseAstronauts(VISION, uc.getOpponent());
//            broadcastEnemies(enemies);

            final int optimalOxygen = average <= 20 ?
                    (int) GameConstants.MIN_OXYGEN_ASTRONAUT + turnNumber / 25 :
                    (int) GameConstants.MIN_OXYGEN_ASTRONAUT + 10;

            broadcastDirections(oppHQLoc);
            if (enemies.length > 0) {
                buildShield();
            } else if (uc.getRound() % 4 == 0) {
                final Direction d = Direction.values()[(int) (uc.getRandomDouble() * 8)];
                if (uc.canEnlistAstronaut(d, optimalOxygen, null)) {
                    uc.enlistAstronaut(d, optimalOxygen, null);
                }
            }

            uc.yield();
        }
    }

    Location[] findOppHQ() {
        int height = uc.getMapHeight();
        int width = uc.getMapWidth();
        int x = uc.getLocation().x;
        int y = uc.getLocation().y;
        Location[] ans = new Location[3];
        ans[0] = new Location(x, height - y);
        ans[1] = new Location(width - x, y);
        ans[2] = new Location(width - x, height - y);
        return ans;
    }

    void buildShield() {
        for (Direction d : Direction.values()) {
            if (uc.canEnlistAstronaut(d, MIN_OXYGEN, null)) {
                uc.enlistAstronaut(d, MIN_OXYGEN, null);
            }
        }
    }

    void broadcastDirections(Location[] locations) {
        uc.cleanBroadcastBuffer();
        for (int i = 500; i-- > 0; ) {
            Location chosen = locations[(int) (uc.getRandomDouble() * 3)];
            int val = chosen.x * 1000 + chosen.y;
            if (uc.canPerformAction(ActionType.BROADCAST, null, val)) {
                uc.performAction(ActionType.BROADCAST, null, val);
            }
        }
    }
}
