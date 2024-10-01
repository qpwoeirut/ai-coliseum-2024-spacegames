package a_turtleplayer;

import a_turtleplayer.util.Util;
import aic2024.user.*;

public class AstronautPlayer extends BasePlayer {
    AstronautPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        final float VISION = GameConstants.ASTRONAUT_VISION_RANGE;
        Direction toHq = Util.toAdjacentHq(uc);
        toHq = toHq == null ? Direction.NORTH : toHq;

        while (true) {
            if (uc.getAstronautInfo().isBeingConstructed()) {
                uc.yield();
            }
            if (uc.pollBroadcast().getMessage() == 0) {
                CarePackageInfo pkg = choosePackage(uc.senseCarePackages(VISION));
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
