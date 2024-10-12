package c_deeznuts;

import aic2024.user.*;

public class HqPlayer extends BasePlayer {
    HqPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        final float VISION = uc.getType().getVisionRange();

        final Location[] oppHQLoc = findOppHQ();
        boolean[] validHQSpot = {true, true, true};
        while (true) {
            for (int i = 0 ; i < 3; i++){
                if (validHQSpot[i]){
                    uc.drawPointDebug(oppHQLoc[i], 10, 0, 0);
                }
            }

            final AstronautInfo[] enemies = uc.senseAstronauts(VISION, uc.getOpponent());
//            broadcastEnemies(enemies);
            BroadcastInfo message = uc.pollBroadcast();
            int a = 0;
            while (message != null){
                a++;
                int msg = message.getMessage();
                int loc = msg%100;
                boolean good = (msg < 100);
                if (good){
                    for (int i = 0; i < 3; i++){
                        validHQSpot[i] = false;
                    }
                    validHQSpot[loc] = true;
                }
                else{
                    validHQSpot[loc] = false;
                }
                message = uc.pollBroadcast();
            }
            uc.drawPointDebug(new Location(0, a), 255, 0, 0);
            int optimalOxygen = (int) GameConstants.MIN_OXYGEN_ASTRONAUT + 10;
            optimalOxygen += 4-(optimalOxygen%4);
            if ((int)(uc.getRandomDouble() * 7) == 1){
                for (int i = 0; i < 3; i++){
                    if (validHQSpot[i]){
                        optimalOxygen += i;
                        optimalOxygen += 4*Math.ceil((Math.ceil(Math.sqrt(oppHQLoc[i].distanceSquared(uc.getLocation())))-optimalOxygen)/4.0);
                        optimalOxygen += 10;
                        break;
                    }
                }
                for (Direction d: Direction.values()){
                    if (uc.canEnlistAstronaut(d, (int) optimalOxygen, CarePackage.REINFORCED_SUIT)) {
                        uc.enlistAstronaut(d, (int) optimalOxygen, CarePackage.REINFORCED_SUIT);
                    } else if (uc.canEnlistAstronaut(d, (int) optimalOxygen, CarePackage.SURVIVAL_KIT)) {
                        uc.enlistAstronaut(d, (int) optimalOxygen, CarePackage.SURVIVAL_KIT);
                    } else if (uc.canEnlistAstronaut(d, (int) optimalOxygen, null)) {
                        uc.enlistAstronaut(d, (int) optimalOxygen, null);
                    }
                }
            }
            int gatherOxygen = (int) GameConstants.MIN_OXYGEN_ASTRONAUT + 10;

            if (needShield(enemies)) {
                buildShield();
            } else if (uc.getRound() % 4 == 0) {
                final Direction d = Direction.values()[(int) (uc.getRandomDouble() * 8)];
                if (uc.canEnlistAstronaut(d, gatherOxygen, CarePackage.SURVIVAL_KIT)) {
                    uc.enlistAstronaut(d, gatherOxygen,  CarePackage.SURVIVAL_KIT);
                } else if (uc.canEnlistAstronaut(d, gatherOxygen, null)) {
                    uc.enlistAstronaut(d, gatherOxygen, null);
                }
            }

            uc.yield();
        }
    }

    Location[] findOppHQ() {
        int height = uc.getMapHeight();
        int width = uc.getMapWidth();
        int x = uc.getLocation().x;
        int y = uc.getLocation().y;
        Location[] ans = new Location[3];
        ans[0] = new Location(x, height - y);
        ans[1] = new Location(width - x, y);
        ans[2] = new Location(width - x, height - y);
        return ans;
    }

    boolean needShield(AstronautInfo[] enemies) {
        int enemyScore = enemies.length;
        for (int i = enemies.length; i --> 0;) {
            if (enemies[i].getLocation().distanceSquared(uc.getLocation()) <= 20) ++enemyScore;
            if (enemies[i].getCarePackage() == CarePackage.REINFORCED_SUIT) return true;
        }
        return enemyScore >= 2;
    }

    void buildShield() {
        for (Direction d : Direction.values()) {
            if (uc.canEnlistAstronaut(d, (int) GameConstants.MIN_OXYGEN_ASTRONAUT, CarePackage.REINFORCED_SUIT)) {
                uc.enlistAstronaut(d, (int) GameConstants.MIN_OXYGEN_ASTRONAUT, CarePackage.REINFORCED_SUIT);
            } else if (uc.canEnlistAstronaut(d, (int) GameConstants.MIN_OXYGEN_ASTRONAUT, CarePackage.SURVIVAL_KIT)) {
                uc.enlistAstronaut(d, (int) GameConstants.MIN_OXYGEN_ASTRONAUT, CarePackage.SURVIVAL_KIT);
            } else if (uc.canEnlistAstronaut(d, (int) GameConstants.MIN_OXYGEN_ASTRONAUT, null)) {
                uc.enlistAstronaut(d, (int) GameConstants.MIN_OXYGEN_ASTRONAUT, null);
            }
        }
    }

    void broadcastDirections(Location[] locations) {
        uc.cleanBroadcastBuffer();
        for (int i = 500; i-- > 0; ) {
            Location chosen = locations[(int) (uc.getRandomDouble() * 3)];
            int val = chosen.x * 1000 + chosen.y;
            if (uc.canPerformAction(ActionType.BROADCAST, null, val)) {
                uc.performAction(ActionType.BROADCAST, null, val);
            }
        }
    }
}
