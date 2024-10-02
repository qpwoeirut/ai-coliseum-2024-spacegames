package c_combinedplayer;

import aic2024.user.*;
import c_combinedplayer.util.MapRecorder;
import c_combinedplayer.util.Mover;

public class AstronautPlayer extends BasePlayer {
    private final MapRecorder mapRecorder;
    private final Mover mover;

    AstronautPlayer(UnitController uc) {
        super(uc);
        mapRecorder = new MapRecorder(uc);
        mover = new Mover(uc, mapRecorder);
    }

    void run() {
        final float VISION = GameConstants.ASTRONAUT_VISION_RANGE;

        mapRecorder.recordInfo(100);
        uc.yield();  // Astronauts can't read broadcasts for the first round they're alive

        int msg = uc.pollBroadcast().getMessage();
        int x = (msg - msg % 1000) / 1000;
        int y = msg % 1000;
        Location target = new Location(x, y);

        while (true) {
            //broadcasts are used to give opponent hq locations
            StructureInfo targStructure = chooseStructure(uc.senseStructures(VISION, uc.getOpponent()));
            if (targStructure != null) sabotage(targStructure.getLocation());

            AstronautInfo targAstro = chooseAstronaut(uc.senseAstronauts(VISION, uc.getOpponent()));
            if (targAstro != null) sabotage(targAstro.getLocation());

            CarePackageInfo pkg = choosePackage(uc.senseCarePackages(VISION));
            if (pkg != null) retrievePackage(pkg);

            if (uc.getAstronautInfo().getOxygen() <= 1) {
                uc.performAction(ActionType.TERRAFORM, Direction.ZERO, 0);
            }
            if (uc.senseTileType(uc.getLocation()) == TileType.HOT_ZONE) {
                uc.yield();
            }

            mover.moveToward(target);

            mapRecorder.recordInfo(100);
            uc.yield();
        }
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
        } else {
            mover.moveToward(pkg.getLocation());
        }
    }

    AstronautInfo chooseAstronaut(AstronautInfo[] astronauts) {
        int bestIndex = -1;
        float bestScore = -1;
        for (int i = astronauts.length; i-- > 0; ) {
            if (uc.getLocation().distanceSquared(astronauts[i].getLocation()) <= 2) {
                continue;
            }
            float score = astronauts[i].getOxygen();
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
            if (bestScore < score) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex == -1 ? null : structures[bestIndex];
    }

    void sabotage(Location target) {
        Direction dir = uc.getLocation().directionTo(target);
        if (uc.getLocation().distanceSquared(target) <= 2 && uc.canPerformAction(ActionType.SABOTAGE, dir, 0)) {
            uc.performAction(ActionType.SABOTAGE, dir, 0);
        } else {
            mover.moveToward(target);
        }
    }
}
