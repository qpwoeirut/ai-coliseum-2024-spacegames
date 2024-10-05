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

    void run() {
        mapRecorder.recordInfo(100);
        uc.yield();  // Astronauts can't read broadcasts for the first round they're alive

        int msg = uc.pollBroadcast().getMessage();
        int x = (msg - msg % 1000) / 1000;
        int y = msg % 1000;
        Location target = new Location(x, y);

        while (true) {
            boolean actedStructure = false;
            for (int i = 8; i-- > 0 && tryTargetStructure(); ) actedStructure = true;

            boolean actedAstronaut = false;
            for (int i = 8; !actedStructure && i-- > 0 && trySabotageAstronaut(); ) actedAstronaut = true;

            boolean actedPackage = false;
            for (int i = 8; !actedStructure && !actedAstronaut && i-- > 0 && tryRetrievePackage(); ) actedPackage = true;

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

    boolean tryTargetStructure() {
        targetStructure = chooseStructure(uc.senseStructures(GameConstants.ASTRONAUT_VISION_RANGE, uc.getOpponent()));
        if (targetStructure != null) {
            sabotage(targetStructure.getLocation());
            return true;
        }
        return false;
    }

    boolean trySabotageAstronaut() {
        AstronautInfo targAstro = chooseAstronaut(uc.senseAstronauts(GameConstants.ASTRONAUT_VISION_RANGE, uc.getOpponent()));
        if (targAstro != null) {
            sabotage(targAstro.getLocation());
            return true;
        }
        return false;
    }

    boolean tryRetrievePackage() {
        targetPackage = choosePackage(uc.senseCarePackages(GameConstants.ASTRONAUT_VISION_RANGE));
        if (targetPackage != null) {
            retrievePackage(targetPackage);
            return true;
        }
        return false;
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

    void retrievePackage(CarePackageInfo pkg) {
        Direction dir = uc.getLocation().directionTo(pkg.getLocation());
        if (uc.getLocation().distanceSquared(pkg.getLocation()) <= 2 && uc.canPerformAction(ActionType.RETRIEVE, dir, 0)) {
            uc.performAction(ActionType.RETRIEVE, dir, 0);
            targetPackage = null;
        } else {
            mover.moveToward(pkg.getLocation());
        }
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

    void sabotage(Location target) {
        Direction dir = uc.getLocation().directionTo(target);
        if (uc.getLocation().distanceSquared(target) <= 2) {
            while (uc.canPerformAction(ActionType.SABOTAGE, dir, 0)) uc.performAction(ActionType.SABOTAGE, dir, 0);
        } else {
            mover.moveToward(target);
        }
    }

    void terraformTowardHq() {
        final Direction toHq = uc.getLocation().directionTo(uc.getParent().getLocation());
        if (uc.canPerformAction(ActionType.TERRAFORM, toHq, 0)) uc.performAction(ActionType.TERRAFORM, toHq, 0);
        if (uc.canPerformAction(ActionType.TERRAFORM, toHq.rotateLeft(), 0)) uc.performAction(ActionType.TERRAFORM, toHq.rotateLeft(), 0);
        if (uc.canPerformAction(ActionType.TERRAFORM, toHq.rotateRight(), 0)) uc.performAction(ActionType.TERRAFORM, toHq.rotateRight(), 0);
        if (uc.canPerformAction(ActionType.TERRAFORM, Direction.ZERO, 0)) uc.performAction(ActionType.TERRAFORM, Direction.ZERO, 0);
        if (uc.canPerformAction(ActionType.TERRAFORM, toHq.rotateLeft().rotateLeft(), 0)) uc.performAction(ActionType.TERRAFORM, toHq.rotateLeft().rotateLeft(), 0);
        if (uc.canPerformAction(ActionType.TERRAFORM, toHq.rotateRight().rotateRight(), 0)) uc.performAction(ActionType.TERRAFORM, toHq.rotateRight().rotateRight(), 0);
        if (uc.canPerformAction(ActionType.TERRAFORM, toHq.opposite().rotateLeft(), 0)) uc.performAction(ActionType.TERRAFORM, toHq.opposite().rotateLeft(), 0);
        if (uc.canPerformAction(ActionType.TERRAFORM, toHq.opposite().rotateRight(), 0)) uc.performAction(ActionType.TERRAFORM, toHq.opposite().rotateRight(), 0);
        if (uc.canPerformAction(ActionType.TERRAFORM, toHq.opposite(), 0)) uc.performAction(ActionType.TERRAFORM, toHq.opposite(), 0);
    }
}
