package b_bugnav;

import aic2024.user.*;
import b_bugnav.util.MapRecorder;
import b_bugnav.util.Mover;
import b_bugnav.util.Util;

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
        Direction toHq = Util.toAdjacentHq(uc);
        toHq = toHq == null ? Direction.NORTH : toHq;
        final Location target = uc.getLocation().add(toHq.dx * 30, toHq.dy * 30);

        while (true) {
            if (uc.getAstronautInfo().isBeingConstructed()) {
                mapRecorder.recordInfo(100);
                uc.yield();
            }
            if (uc.pollBroadcast().getMessage() == 0) {
                CarePackageInfo pkg = choosePackage(uc.senseCarePackages(VISION));
                if (pkg == null) mover.moveToward(target);
                if (pkg != null) retrievePackage(pkg);
            }
            mapRecorder.recordInfo(100);
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
        } else {
            mover.moveToward(pkg.getLocation());
        }
    }
}
