package e_economy;

import aic2024.user.*;
import e_economy.util.MapRecorder;
import e_economy.util.Mover;
import e_economy.util.Util;

public class AstronautPlayer extends BasePlayer {
    private final MapRecorder mapRecorder;
    private final Mover mover;

    AstronautPlayer(UnitController uc) {
        super(uc);
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
        uc.yield();  // Astronauts can't read broadcasts for the first round they're alive

        final Location target = Util.symmetricLocations(uc.getParent().getLocation(), uc.getMapWidth(), uc.getMapHeight())[(int) (uc.getRandomDouble() * 3)];

        while (true) {
            boolean actedStructure = false;
            for (int i = 8; i-- > 0; ) {
                final int result = tryTargetStructure();
                if (result == ACTED) continue;
                actedStructure = result == TARGETED;
                break;
            }

            boolean actedAstronaut = false;
            for (int i = 8; i-- > 0 && !actedStructure; ) {
                final int result = trySabotageAstronaut();
                if (result == ACTED) continue;
                actedAstronaut = result == TARGETED;
                break;
            }

            boolean actedPackage = false;
            for (int i = 8; i-- > 0 && !actedStructure && !actedAstronaut; ) {
                final int result = tryRetrievePackage();
                if (result == ACTED) continue;
                actedPackage = result == TARGETED;
                break;
            }

            if (!actedStructure && !actedAstronaut && !actedPackage) {
                mover.moveToward(target);
            }
            if (uc.getAstronautInfo().getOxygen() <= Util.oxygenCost(uc)) {
                terraformTowardHq();
            }

            mapRecorder.recordInfo(100);
            uc.yield();
        }
    }

    int tryTargetStructure() {
        targetStructure = chooseStructure(uc.senseStructures(GameConstants.ASTRONAUT_VISION_RANGE, uc.getOpponent()));
        if (targetStructure != null) return sabotage(targetStructure.getLocation());
        return NO_ACTION;
    }

    int trySabotageAstronaut() {
        AstronautInfo targAstro = chooseAstronaut(uc.senseAstronauts(GameConstants.ASTRONAUT_VISION_RANGE, uc.getOpponent()));
        if (targAstro != null) return sabotage(targAstro.getLocation());
        return NO_ACTION;
    }

    int tryRetrievePackage() {
        targetPackage = choosePackage(uc.senseCarePackages(GameConstants.ASTRONAUT_VISION_RANGE));
        if (targetPackage != null) return retrievePackage(targetPackage);
        return NO_ACTION;
    }

    CarePackageInfo choosePackage(CarePackageInfo[] packages) {
        int bestIndex = -1;
        float bestScore = -1;
        for (int i = packages.length; i-- > 0; ) {
            float score = 100f / uc.getLocation().distanceSquared(packages[i].getLocation());
            if (packages[i].getCarePackageType() == CarePackage.PLANTS) {
                score += (1000 - uc.getRound()) * GameConstants.OXYGEN_PLANT;
            } else if (packages[i].getCarePackageType() == CarePackage.OXYGEN_TANK) {
                score += GameConstants.OXYGEN_TANK_AMOUNT;
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
            final float distScore = 1000f / (dist * dist);
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
            if (targetStructure != null && targetStructure.getLocation() != structures[i].getLocation()) {
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
}
