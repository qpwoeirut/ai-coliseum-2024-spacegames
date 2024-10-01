package a_explorer_hotspot_player;

import a_explorer_hotspot_player.util.Util;
import aic2024.engine.Game;
import aic2024.user.*;

import javax.swing.*;

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
            //broadcasts are used to give opponent hq locations
            CarePackageInfo pkg = choosePackage(uc.senseCarePackages(VISION));
            if (pkg != null) retrievePackage(pkg);
            if (uc.getAstronautInfo().getOxygen() <= 1){
                uc.performAction(ActionType.TERRAFORM, Direction.ZERO, 0);
            }
            if (pkg == null && uc.senseTileType(uc.getLocation()) == TileType.HOT_ZONE){
                uc.yield();
            }
            if (pkg == null && uc.canPerformAction(ActionType.MOVE, moveToward(target), 0)) {
                uc.performAction(ActionType.MOVE, toHq.opposite(), 0);
            }
            uc.yield();
        }
    }

    CarePackageInfo choosePackage(CarePackageInfo[] packages) {
        int bestIndex = -1;
        float bestScore = 0;
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
        int x = loc.x;
        int y = loc.y;
        int atx = uc.getLocation().x;
        int aty = uc.getLocation().y;
        Direction initdir = uc.getLocation().directionTo(loc);
        if (!uc.canPerformAction(ActionType.MOVE, initdir, 0)){
            initdir = initdir.rotateRight();
        }
        else{
            return initdir;
        }
        if (!uc.canPerformAction(ActionType.MOVE, initdir, 0)){
            initdir = initdir.rotateLeft();
            initdir = initdir.rotateLeft();
        }
        else{
            return initdir;
        }
        if (!uc.canPerformAction(ActionType.MOVE, initdir, 0)){
            initdir = initdir.rotateRight();
            initdir = initdir.rotateRight();
            initdir = initdir.rotateRight();
        }
        else{
            return initdir;
        }
        if (!uc.canPerformAction(ActionType.MOVE, initdir, 0)){
            initdir = initdir.rotateLeft();
            initdir = initdir.rotateLeft();
            initdir = initdir.rotateLeft();
            initdir = initdir.rotateLeft();
        }
        else{
            return initdir;
        }
        if (uc.canPerformAction(ActionType.MOVE, initdir, 0)){
            return initdir;
        }
        return Direction.ZERO;
    }

    void retrievePackage(CarePackageInfo pkg) {
        Direction dir = uc.getLocation().directionTo(pkg.getLocation());
        if (uc.getLocation().distanceSquared(pkg.getLocation()) <= 2 && uc.canPerformAction(ActionType.RETRIEVE, dir, 0)) {
            uc.performAction(ActionType.RETRIEVE, dir, 0);
        } else if (uc.canPerformAction(ActionType.MOVE, dir, 0)) {
            uc.performAction(ActionType.MOVE, dir, 0);
        }
    }
}
