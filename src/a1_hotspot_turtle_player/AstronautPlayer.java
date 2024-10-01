package a1_hotspot_turtle_player;

import a1_hotspot_turtle_player.util.Util;
import aic2024.engine.Game;
import aic2024.user.*;

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

        while (true) {
            if (uc.pollBroadcast().getMessage() == 0) {
                CarePackageInfo pkg = choosePackage(uc.senseCarePackages(VISION));
                if (pkg == null && uc.senseTileType(uc.getLocation()) == TileType.HOT_ZONE){
                    uc.yield();
                }
                if (pkg == null && uc.canPerformAction(ActionType.MOVE, toHq.opposite(), 0)) {
                    uc.performAction(ActionType.MOVE, toHq.opposite(), 0);
                }
                if (pkg != null) retrievePackage(pkg);

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

    void retrievePackage(CarePackageInfo pkg) {
        Direction dir = uc.getLocation().directionTo(pkg.getLocation());
        if (uc.getLocation().distanceSquared(pkg.getLocation()) <= 2 && uc.canPerformAction(ActionType.RETRIEVE, dir, 0)) {
            uc.performAction(ActionType.RETRIEVE, dir, 0);
        } else if (uc.canPerformAction(ActionType.MOVE, dir, 0)) {
            uc.performAction(ActionType.MOVE, dir, 0);
        }
    }
}
