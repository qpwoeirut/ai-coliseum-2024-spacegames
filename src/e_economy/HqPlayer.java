package e_economy;

import aic2024.user.*;
import e_economy.util.Util;

public class HqPlayer extends BasePlayer {
    HqPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        final float VISION = uc.getType().getVisionRange();

        final Location[] oppHQLoc = Util.symmetricLocations(uc.getParent().getLocation(), uc.getMapWidth(), uc.getMapHeight());
        double sum = 0;
        for (int i = 0; i < oppHQLoc.length; i++) {
            sum += Math.sqrt(oppHQLoc[i].distanceSquared(uc.getLocation()));
        }
        final double average = sum / ((double) oppHQLoc.length);

        while (true) {
            final AstronautInfo[] enemies = uc.senseAstronauts(VISION, uc.getOpponent());

            final int optimalOxygen = average <= 20 ?
                    (int) GameConstants.MIN_OXYGEN_ASTRONAUT + uc.getRound() / 25 :
                    (int) GameConstants.MIN_OXYGEN_ASTRONAUT + 10;

            if (needShield(enemies)) {
                buildShield();
            } else if (uc.getRound() % 4 == 0) {
                final Direction d = Direction.values()[(int) (uc.getRandomDouble() * 8)];
                if (uc.canEnlistAstronaut(d, optimalOxygen, CarePackage.SURVIVAL_KIT)) {
                    uc.enlistAstronaut(d, optimalOxygen,  CarePackage.SURVIVAL_KIT);
                } else if (uc.canEnlistAstronaut(d, optimalOxygen, null)) {
                    uc.enlistAstronaut(d, optimalOxygen, null);
                }
            }

            uc.yield();
        }
    }

    boolean needShield(AstronautInfo[] enemies) {
        int enemyScore = enemies.length;
        for (int i = enemies.length; i --> 0;) {
            enemyScore += 24 / enemies[i].getLocation().distanceSquared(uc.getLocation());
            if (enemies[i].getCarePackage() == CarePackage.REINFORCED_SUIT) return true;
        }
        return enemyScore >= 4;
    }

    void buildShield() {
        for (Direction d : Direction.values()) {
            if (uc.canEnlistAstronaut(d, (int) GameConstants.MIN_OXYGEN_ASTRONAUT, CarePackage.REINFORCED_SUIT)) {
                uc.enlistAstronaut(d, (int) GameConstants.MIN_OXYGEN_ASTRONAUT, CarePackage.REINFORCED_SUIT);
            } else if (uc.canEnlistAstronaut(d, (int) GameConstants.MIN_OXYGEN_ASTRONAUT, CarePackage.SURVIVAL_KIT)) {
                uc.enlistAstronaut(d, (int) GameConstants.MIN_OXYGEN_ASTRONAUT, CarePackage.SURVIVAL_KIT);
            } else if (uc.canEnlistAstronaut(d, (int) GameConstants.MIN_OXYGEN_ASTRONAUT, null)) {
                uc.enlistAstronaut(d, (int) GameConstants.MIN_OXYGEN_ASTRONAUT, null);
            }
        }
    }
}
