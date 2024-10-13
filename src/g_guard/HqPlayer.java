package g_guard;

import aic2024.user.*;
import g_guard.util.Util;

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
            for (int i = DIRS; i --> 0; ) totalScore += dirScores[i] * dirScores[i];
            totalScore = Math.max(1.0, totalScore / (DIRS * DIRS * DIR_INCREMENT * DIR_INCREMENT));

            final double spawnChance = Math.min(2 / totalScore, uc.getRound() / 30.0);

            final AstronautInfo[] enemies = uc.senseAstronauts(VISION, uc.getOpponent());
            if (buildShield(enemies)) ;
            else if (uc.getRandomDouble() < Math.max(0.2, spawnChance)) {
                // Don't spawn too much in first rounds. Wait for care packages to land.

                final int[] scores = new int[8];
                final int dir = chooseDirection(dirScores, scores);

                final double score = scores[dir] / (4.0 * DIR_INCREMENT);
                final int spawnOxygen = (int) Math.min(MAX_SPAWN_OXYGEN, 10 + uc.getRound() / 100.0 + 5 * score);

                uc.println("score oxy " + score + " " + spawnOxygen);

                final Direction d = Direction.values()[dir];

                final int kitOxygen = Math.max((int) GameConstants.MIN_OXYGEN_ASTRONAUT, (spawnOxygen + 1) / 2);
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
                final Location edgeOfAstroRange = uc.getLocation().add(Direction.values()[i].dx * 5, Direction.values()[i].dy * 5);
                if (uc.isOutOfMap(edgeOfAstroRange)) chance[i] = 0;
                else chance[i] = 10_000_000 / Math.max(1, score[i]);
            }
        }
        return Util.weightedRandom(uc.getRandomDouble(), chance);
    }

    /**
     * Based on the nearby enemies, build a shield of proportional strength facing the enemies.
     * <br>
     * Spawn suits to match their suits, then spawn regular astronauts to match their numbers.
     * Ignore astronauts that are too far away. Assume that we are destroying any hyperjumps within 58 distance.
     * A distance of 34 requires two turns for enemy astronauts to contact the shield.
     * This won't work on maps where hyperjumps are behind a wall, but we'd probably lose those anyway so whatever.
     *
     * @param enemies nearby enemies
     * @return whether a shield was built
     */
    boolean buildShield(AstronautInfo[] enemies) {
        final int[] directionScore = new int[8];
        for (int i = enemies.length; i --> 0; ) {
            final int dir = uc.getLocation().directionTo(enemies[i].getLocation()).ordinal();
            if (enemies[i].getCarePackage() == CarePackage.REINFORCED_SUIT && Util.hitsFromSuit(enemies[i].getOxygen()) > 1) {
                directionScore[dir] += (int)(10 * enemies[i].getOxygen());
            } else {
                directionScore[dir] += 34 / uc.getLocation().distanceSquared(enemies[i].getLocation());
            }
        }
        final int total = directionScore[0] + directionScore[1] + directionScore[2] + directionScore[3] +
                directionScore[4] + directionScore[5] + directionScore[6] + directionScore[7];

        if (total >= uc.getParent().getHealth()) {
            for (int d = 8; d --> 0; ) {
                directionScore[d] += total * 10;
            }
        }
        uc.println("buildShield " + Arrays.toString(directionScore) + " " + total);

        boolean built = false;
        for (int d = 8; d --> 0; ) {
            final double score =
                    Math.max(directionScore[(d + 6) % 8],
                            Math.max(directionScore[(d + 7) % 8],
                                    Math.max(directionScore[d],
                                            Math.max(directionScore[(d + 1) % 8], directionScore[(d + 2) % 8]))));

            uc.println("score " + score + " " +  Direction.values()[d]);
            if (uc.canEnlistAstronaut(Direction.values()[d], (int) (score / 10), CarePackage.REINFORCED_SUIT)) {
                uc.enlistAstronaut(Direction.values()[d], (int) (score / 10), CarePackage.REINFORCED_SUIT);
                built = true;
            } else if (score > 0 && uc.canEnlistAstronaut(Direction.values()[d], (int) GameConstants.MIN_OXYGEN_ASTRONAUT, null)) {
                uc.enlistAstronaut(Direction.values()[d], (int) GameConstants.MIN_OXYGEN_ASTRONAUT, null);
                built = true;
            }
        }

        return built;
    }
}
