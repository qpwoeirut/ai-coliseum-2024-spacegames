package c_combinedplayer;

import aic2024.user.*;
import c_combinedplayer.util.Util;

public class AstronautPlayer extends BasePlayer {
    AstronautPlayer(UnitController uc) {
        super(uc);
    }


    void run() {

        AstronautInfo astro = uc.getAstronautInfo();

        if(astro.isBeingConstructed()){
            uc.yield();
        }

        final float VISION = GameConstants.ASTRONAUT_VISION_RANGE;
        Direction toHq = Util.toAdjacentHq(uc);
        toHq = toHq == null ? Util.toCenter(uc.getLocation(), uc.getMapWidth(), uc.getMapHeight()).opposite() : toHq;
        toHq = Util.toCenter(uc.getLocation(), uc.getMapWidth(), uc.getMapHeight()).opposite();
        toHq = Util.randomDirection(uc, toHq, 1);

        int msg = uc.pollBroadcast().getMessage();
        int x = (msg - msg%1000)/1000;
        int y = msg%1000;
        Location target = new Location(x,y);

        while (true) {
            uc.drawLineDebug(uc.getLocation(), target, 10, 10, 10);
            uc.drawLineDebug(uc.getLocation(), uc.getLocation().add(moveToward(target)), 100, 0, 0);
            
            //broadcasts are used to give opponent hq locations
            StructureInfo targStructure = chooseStructure(uc.senseStructures(VISION, uc.getOpponent()));
            if (targStructure != null) attackStructure(targStructure);
            AstronautInfo targAstro = chooseAstronaut(uc.senseAstronauts(VISION, uc.getOpponent()));
            if (targAstro != null) killAstronaut(targAstro);
            CarePackageInfo pkg = choosePackage(uc.senseCarePackages(VISION));
            if (pkg != null) retrievePackage(pkg);
            if (uc.getAstronautInfo().getOxygen() <= 1){
                uc.performAction(ActionType.TERRAFORM, Direction.ZERO, 0);
            }
            if (uc.senseTileType(uc.getLocation()) == TileType.HOT_ZONE){
                uc.yield();
            }
            if (uc.canPerformAction(ActionType.MOVE, moveToward(target), 0)) {
                uc.drawLineDebug(uc.getLocation(), uc.getLocation().add(moveToward(target)), 0, 0, 100);
                uc.performAction(ActionType.MOVE, moveToward(target), 0);
            }

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

    Direction moveToward(Location loc){
        Direction initdir = uc.getLocation().directionTo(loc);
        int at = (int)(Math.random()*2);
        int rot = 0;
        while (rot < 8) {
            if (!uc.canPerformAction(ActionType.MOVE, initdir, 0)) {
                for (int i = 0; i < rot; i++) {
                    if (at == 1) {
                        initdir = initdir.rotateRight();
                    } else {
                        initdir = initdir.rotateLeft();
                    }
                }
            } else {
                return initdir;
            }
            rot ++;
            at = 1-at;
        }
        return Direction.ZERO;
    }

    void retrievePackage(CarePackageInfo pkg) {
        Direction movedir = moveToward(pkg.getLocation());
        Direction dir = uc.getLocation().directionTo(pkg.getLocation());
        if (uc.getLocation().distanceSquared(pkg.getLocation()) <= 2 && uc.canPerformAction(ActionType.RETRIEVE, dir, 0)) {
            uc.performAction(ActionType.RETRIEVE, dir, 0);
        } else if (uc.canPerformAction(ActionType.MOVE, movedir, 0)) {
            uc.performAction(ActionType.MOVE, movedir, 0);
        }
    }

    AstronautInfo chooseAstronaut(AstronautInfo[] astronauts) {
        int bestIndex = -1;
        float bestScore = -1;
        for (int i = astronauts.length; i-- > 0; ) {
            if (uc.getLocation().distanceSquared(astronauts[i].getLocation()) <= 2){
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
    void killAstronaut(AstronautInfo astronaut){
        Direction movedir = moveToward(astronaut.getLocation());
        Direction dir = uc.getLocation().directionTo(astronaut.getLocation());
        if (uc.getLocation().distanceSquared(astronaut.getLocation()) <= 2 && uc.canPerformAction(ActionType.SABOTAGE, dir, 0)) {
            uc.performAction(ActionType.SABOTAGE, dir, 0);
        }
        else if (uc.canPerformAction(ActionType.MOVE, movedir, 0)) {
            uc.performAction(ActionType.MOVE, movedir, 0);
        }
    }

    StructureInfo chooseStructure(StructureInfo[] structures) {
        int bestIndex = -1;
        float bestScore = -1;
        for (int i = structures.length; i-- > 0; ) {
            float score = Math.max(0, GameConstants.ASTRONAUT_VISION_RANGE - uc.getLocation().distanceSquared(structures[i].getLocation()));
            if (structures[i].getType().equals(StructureType.HQ)){
                score += 100000;
            }
            if (bestScore < score) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex == -1 ? null : structures[bestIndex];
    }
    void attackStructure(StructureInfo structure){
        Direction movedir = moveToward(structure.getLocation());
        Direction dir = uc.getLocation().directionTo(structure.getLocation());
        if (uc.getLocation().distanceSquared(structure.getLocation()) <= 2 && uc.canPerformAction(ActionType.SABOTAGE, dir, 0)) {
            uc.performAction(ActionType.SABOTAGE, dir, 0);
        }
        else if (uc.canPerformAction(ActionType.MOVE, movedir, 0)) {
            uc.performAction(ActionType.MOVE, movedir, 0);
        }
    }
}
