package f_fighting;

import aic2024.user.*;
import f_fighting.util.Util;

import java.util.Arrays;

public class HqPlayer extends BasePlayer {
    final int MAX_SPAWN_OXYGEN = 50;
    final double DECAY = 0.999;

    HqPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        final float VISION = uc.getType().getVisionRange();

        final int[] dirScores = new int[DIRS];
        for (int i = DIRS; i-- > 0; ) dirScores[i] = DIR_INCREMENT;

        final int[] messagesReceived = new int[100];

        while (true) {
            updateDirScores(dirScores, messagesReceived);
            double totalScore = 0;
            for (int i = DIRS; i --> 0; ) totalScore += dirScores[i];
            totalScore = Math.max(1.0, totalScore / (DIRS * DIR_INCREMENT));

            final double spawnChance = Math.min(2 / totalScore, uc.getRound() / 30.0);

            final AstronautInfo[] enemies = uc.senseAstronauts(VISION, uc.getOpponent());
            if (needShield(enemies)) {
                buildShield();
            } else if (uc.getRandomDouble() < Math.max(0.2, spawnChance)) {
                // Don't spawn too much in first rounds. Wait for care packages to land.

                final int[] scores = new int[8];
                final int dir = chooseDirection(dirScores, scores);

                final double score = scores[dir] / (4.0 * DIR_INCREMENT);
                final int spawnOxygen = (int) Math.min(MAX_SPAWN_OXYGEN, 10 + uc.getRound() / 50.0 + score);

                uc.println("score oxy " + score + " " + spawnOxygen);

                final Direction d = Direction.values()[dir];

                final int kitOxygen = Math.min((int) GameConstants.MIN_OXYGEN_ASTRONAUT, (spawnOxygen + 1) / 2);
                if (uc.canEnlistAstronaut(d, kitOxygen, CarePackage.SURVIVAL_KIT)) {
                    uc.enlistAstronaut(d, kitOxygen, CarePackage.SURVIVAL_KIT);
                } else if (uc.canEnlistAstronaut(d, spawnOxygen, CarePackage.RADIO)) {
                    uc.enlistAstronaut(d, spawnOxygen, CarePackage.RADIO);
                } else if (uc.canEnlistAstronaut(d, (int) GameConstants.MIN_OXYGEN_ASTRONAUT, CarePackage.DOME)) {
                    uc.enlistAstronaut(d, (int) GameConstants.MIN_OXYGEN_ASTRONAUT, CarePackage.DOME);
                } else if (uc.canEnlistAstronaut(d, spawnOxygen, null)) {
                    uc.enlistAstronaut(d, spawnOxygen, null);
                }
            }

            uc.yield();
        }
    }

    void updateDirScores(final int[] dirScores, final int[] messagesReceived) {
        uc.println("start dirScores " + Arrays.toString(dirScores));

        // Decay old information
        for (int i = DIRS; i --> 0; ) {
            dirScores[i] = Math.max(DIR_INCREMENT, (int) Math.round(dirScores[i] * DECAY));
        }

        final int n_recv = comms.receiveMessagesFromAstronauts(messagesReceived);
//        uc.println("n_recv " + n_recv);
        for (int i = n_recv; i-- > 0; ) {
            if (comms.typeOf(messagesReceived[i]) == comms.ASPHYXIATED) {
                final int dir = messagesReceived[i] % comms.MAX_PAYLOAD;
//                uc.println("asphyxiated " + dir);
                dirScores[dir] += DIR_INCREMENT;
            }
            // Ignore other messages for now
        }

        uc.println("end dirScores " + Arrays.toString(dirScores));

        comms.broadcastMessage(comms.dirScoresMessage(dirScores));
    }

    int chooseDirection(int[] dirScores, int[] score) {
        for (int i = DIRS; i --> 0; ) {
            score[ (i - 1) / 2     ] += dirScores[i];
            score[((i + 1) / 2) % 8] += dirScores[i];
        }
        final int[] chance = new int[8];
        for (int i = 8; i --> 0; ) {
            if (!uc.canEnlistAstronaut(Direction.values()[i], (int)GameConstants.MIN_OXYGEN_ASTRONAUT, null)) {
                chance[i] = 0;
            } else {
                chance[i] = 10_000_000 / score[i];
            }
        }
        return Util.weightedRandom(uc.getRandomDouble(), chance);
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
