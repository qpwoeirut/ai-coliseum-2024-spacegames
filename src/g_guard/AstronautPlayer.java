package g_guard;

import aic2024.user.*;
import g_guard.util.Communication;
import g_guard.util.MapRecorder;
import g_guard.util.Mover;
import g_guard.util.Util;

public class AstronautPlayer extends BasePlayer {
    private final Communication comms;
    private final MapRecorder mapRecorder;
    private final Mover mover;

    AstronautPlayer(UnitController uc) {
        super(uc);
        comms = new Communication(uc);
        mapRecorder = new MapRecorder(uc);
        mover = new Mover(uc, mapRecorder);
    }

    private StructureInfo targetStructure = null;
    private CarePackageInfo targetPackage = null;

    private final int NO_ACTION = 0;
    private final int TARGETED = 1;
    private final int ACTED = 2;

    void run() {
        mapRecorder.recordInfo(100);
        uc.yield();  // Astronauts won't receive broadcasts from before they're spawned

        final int[] dirsMessage = comms.receiveDirectionsMessage();
        final int[] dirChance = new int[DIRS];
        if (dirsMessage == null) {
            for (int i = DIRS; i-- > 0; ) dirChance[i] = 1;
        } else {
            for (int i = DIRS; i-- > 0; ) {
                double score = dirsMessage[i + 1] / (double)DIR_INCREMENT;
                dirChance[i] = (int)(1_000_000 / Math.max(1, score * score));
            }
        }

//        uc.println("dirChance " + Arrays.toString(dirChance));

        final int targetDirection = chooseDirection(dirChance);
        final Location target = targetTowardDirection(targetDirection);

//        uc.println("target " + targetDirection + " " + target);

        while (true) {
            if (uc.getAstronautInfo().getCarePackage() == CarePackage.DOME) {
                tryPlaceDome();
            }
            boolean actedStructure = false;
            for (int i = 8; i-- > 0; ) {
                final int result = trySabotageStructure();
                if (result == ACTED || (result == TARGETED && uc.getAstronautInfo().getCurrentMovementCooldown() < GameConstants.MOVEMENT_COOLDOWN)) continue;
                actedStructure = result == TARGETED;
                break;
            }

            boolean actedAstronaut = false;
            for (int i = 8; i-- > 0 && !actedStructure; ) {
                final int result = trySabotageAstronaut();
                if (result == ACTED || (result == TARGETED && uc.getAstronautInfo().getCurrentMovementCooldown() < GameConstants.MOVEMENT_COOLDOWN)) continue;
                actedAstronaut = result == TARGETED;
                break;
            }

            boolean actedPackage = false;
            for (int i = 8; i-- > 0 && !actedStructure && !actedAstronaut; ) {
                final int result = tryRetrievePackage();
                if (result == ACTED || (result == TARGETED && uc.getAstronautInfo().getCurrentMovementCooldown() < GameConstants.MOVEMENT_COOLDOWN)) continue;
                actedPackage = result == TARGETED;
                break;
            }
//            uc.println("acted " + actedStructure + " " +  actedAstronaut + " " + actedPackage);

            if (!actedStructure && !actedAstronaut && !actedPackage) {
//                uc.println("move to " + target);
                mover.moveToward(target);
                mover.moveToward(target);
            }
            if (uc.getAstronautInfo().getOxygen() <= Util.oxygenCost(uc)) {
                trySabotageStructure(2);
                trySabotageAstronaut(2);
                tryRetrievePackage(2);

                comms.broadcastMessage(comms.asphyxiatedMessage(targetDirection));
                terraformTowardHq();  // In case astronaut has radio
            }

            mapRecorder.recordInfo(100);
            uc.yield();
        }
    }

    boolean tryPlaceDome() {
        final Direction toHq = uc.getLocation().directionTo(uc.getParent().getLocation());
        return tryPlaceDome(toHq) ||
                tryPlaceDome(toHq.rotateLeft()) ||
                tryPlaceDome(toHq.rotateRight()) ||
                tryPlaceDome(toHq.rotateLeft().rotateLeft()) ||
                tryPlaceDome(toHq.rotateRight().rotateRight()) ||
                tryPlaceDome(toHq.opposite().rotateLeft()) ||
                tryPlaceDome(toHq.opposite().rotateRight()) ||
                tryPlaceDome(toHq.opposite());
    }

    boolean tryPlaceDome(Direction dir) {
        if (uc.isDomed(uc.getLocation().add(dir)) && uc.getAstronautInfo().getOxygen() > 2) return false;
        if (uc.canPerformAction(ActionType.BUILD_DOME, dir, 0) && Util.isOpenTile(uc, dir)) {
            uc.performAction(ActionType.BUILD_DOME, dir, 0);
            return true;
        }
        return false;
    }

    int trySabotageStructure() {
        return trySabotageStructure(GameConstants.ASTRONAUT_VISION_RANGE);
    }

    int trySabotageStructure(int range) {
        targetStructure = chooseStructure(uc.senseStructures(range, uc.getOpponent()));
        if (targetStructure != null) return sabotage(targetStructure.getLocation());
        return NO_ACTION;
    }

    int trySabotageAstronaut() {
        return trySabotageAstronaut(GameConstants.ASTRONAUT_VISION_RANGE);
    }

    int trySabotageAstronaut(int range) {
        AstronautInfo targAstro = chooseAstronaut(uc.senseAstronauts(range, uc.getOpponent()));
        if (targAstro != null) return sabotage(targAstro.getLocation());
        return NO_ACTION;
    }

    int tryRetrievePackage() {
        return tryRetrievePackage(GameConstants.ASTRONAUT_VISION_RANGE);
    }

    int tryRetrievePackage(int range) {
        targetPackage = choosePackage(uc.senseCarePackages(range));
        if (targetPackage != null) return retrievePackage(targetPackage);
        return NO_ACTION;
    }

    CarePackageInfo choosePackage(CarePackageInfo[] packages) {
        int bestIndex = -1;
        float bestScore = -1;
        for (int i = packages.length; i-- > 0; ) {
            float score = 2000f / (uc.getLocation().distanceSquared(packages[i].getLocation()) * uc.getAstronautInfo().getOxygen() + 1);
            if (packages[i].getCarePackageType() == CarePackage.PLANTS) {
                score += (1000 - uc.getRound()) * GameConstants.OXYGEN_PLANT;
            } else if (packages[i].getCarePackageType() == CarePackage.OXYGEN_TANK) {
                score += GameConstants.OXYGEN_TANK_AMOUNT;
            } else if (packages[i].getCarePackageType() == CarePackage.REINFORCED_SUIT) {
                ++score;
            }

            if (targetPackage != null && targetPackage.getLocation() == packages[i].getLocation()) {
                score += 5;  // Try to avoid switching targets
            }

            if (bestScore < score) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex == -1 ? null : packages[bestIndex];
    }

    int retrievePackage(CarePackageInfo pkg) {
        Direction dir = uc.getLocation().directionTo(pkg.getLocation());
        if (uc.getLocation().distanceSquared(pkg.getLocation()) <= 2 && uc.canPerformAction(ActionType.RETRIEVE, dir, 0)) {
            uc.performAction(ActionType.RETRIEVE, dir, 0);
            targetPackage = null;
            return ACTED;
        } else {
            mover.moveToward(pkg.getLocation());
        }
        return TARGETED;
    }

    AstronautInfo chooseAstronaut(AstronautInfo[] astronauts) {
        int bestIndex = -1;
        float bestScore = uc.getAstronautInfo().getOxygen() - 0.001f;
        for (int i = astronauts.length; i-- > 0; ) {
            final float dist = uc.getParent().getLocation().distanceSquared(astronauts[i].getLocation());
            final float distScore = 1000f / Math.max(1, dist * dist);
            final float score = astronauts[i].getOxygen() + distScore;

            if (bestScore < score) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex == -1 ? null : astronauts[bestIndex];
    }

    StructureInfo chooseStructure(StructureInfo[] structures) {
        int bestIndex = -1;
        float bestScore = -1;
        for (int i = structures.length; i-- > 0; ) {
            float score = Math.max(0, GameConstants.ASTRONAUT_VISION_RANGE - uc.getLocation().distanceSquared(structures[i].getLocation()));
            if (structures[i].getType().equals(StructureType.HQ)) {
                score += 100000;
            }
            if (targetStructure != null && targetStructure.getLocation() == structures[i].getLocation()) {
                score += 5;  // Try to avoid switching targets
            }
            if (bestScore < score) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex == -1 ? null : structures[bestIndex];
    }

    int sabotage(Location target) {
        boolean acted = false;
        Direction dir = uc.getLocation().directionTo(target);
        if (uc.getLocation().distanceSquared(target) <= 2) {
            while (uc.canPerformAction(ActionType.SABOTAGE, dir, 0)) {
                uc.performAction(ActionType.SABOTAGE, dir, 0);
                acted = true;
            }
            return acted ? ACTED : TARGETED;
        } else {
            mover.moveToward(target);
        }
        return TARGETED;
    }

    void terraformTowardHq() {
        final Direction toHq = uc.getLocation().directionTo(uc.getParent().getLocation());
        if (uc.canPerformAction(ActionType.TERRAFORM, toHq,                             0)) uc.performAction(ActionType.TERRAFORM, toHq,                             0);
        if (uc.canPerformAction(ActionType.TERRAFORM, toHq.rotateLeft(),                0)) uc.performAction(ActionType.TERRAFORM, toHq.rotateLeft(),                0);
        if (uc.canPerformAction(ActionType.TERRAFORM, toHq.rotateRight(),               0)) uc.performAction(ActionType.TERRAFORM, toHq.rotateRight(),               0);
        if (uc.canPerformAction(ActionType.TERRAFORM, Direction.ZERO,                   0)) uc.performAction(ActionType.TERRAFORM, Direction.ZERO,                   0);
        if (uc.canPerformAction(ActionType.TERRAFORM, toHq.rotateLeft().rotateLeft(),   0)) uc.performAction(ActionType.TERRAFORM, toHq.rotateLeft().rotateLeft(),   0);
        if (uc.canPerformAction(ActionType.TERRAFORM, toHq.rotateRight().rotateRight(), 0)) uc.performAction(ActionType.TERRAFORM, toHq.rotateRight().rotateRight(), 0);
        if (uc.canPerformAction(ActionType.TERRAFORM, toHq.opposite().rotateLeft(),     0)) uc.performAction(ActionType.TERRAFORM, toHq.opposite().rotateLeft(),     0);
        if (uc.canPerformAction(ActionType.TERRAFORM, toHq.opposite().rotateRight(),    0)) uc.performAction(ActionType.TERRAFORM, toHq.opposite().rotateRight(),    0);
        if (uc.canPerformAction(ActionType.TERRAFORM, toHq.opposite(),                  0)) uc.performAction(ActionType.TERRAFORM, toHq.opposite(),                  0);
    }

    int chooseDirection(int[] dirChance) {
        final Direction awayFromHq = uc.getParent().getLocation().directionTo(uc.getLocation());

        final int awayIndex = (awayFromHq.ordinal() * 2) + DIRS;  // Add DIRS for easier modding
        final int total = dirChance[(awayIndex - 2) % DIRS] +
                dirChance[(awayIndex - 1) % DIRS] +
                dirChance[ awayIndex      % DIRS] +
                dirChance[(awayIndex + 1) % DIRS] +
                dirChance[(awayIndex + 2) % DIRS];
        int selection = (int)(uc.getRandomDouble() * total);
        if (selection < dirChance[(awayIndex - 2) % DIRS]) return (awayIndex - 2) % DIRS;
        selection -=    dirChance[(awayIndex - 2) % DIRS];
        if (selection < dirChance[(awayIndex - 1) % DIRS]) return (awayIndex - 1) % DIRS;
        selection -=    dirChance[(awayIndex - 1) % DIRS];
        if (selection < dirChance[ awayIndex      % DIRS]) return  awayIndex      % DIRS;
        selection -=    dirChance[ awayIndex      % DIRS];
        if (selection < dirChance[(awayIndex + 1) % DIRS]) return (awayIndex + 1) % DIRS;

        return (awayIndex + 2) % DIRS;
    }

    Location targetTowardDirection(int dirIndex) {
        return uc.getLocation().add(DIRECTIONS[dirIndex][0], DIRECTIONS[dirIndex][1]);
    }
}
