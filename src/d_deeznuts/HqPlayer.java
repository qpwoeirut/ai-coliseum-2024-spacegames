package d_deeznuts;

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
                uc.drawPointDebug(new Location(a,0), 255, 0, 0);
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
                uc.println("RECIEVED: " + loc);
                uc.drawPointDebug(oppHQLoc[loc], 0, 255, 0);
                message = uc.pollBroadcast();
            }
            int spotsLeft = 0;
            for (int i = 0; i < 3; i++){
                if (validHQSpot[i]) {
                    spotsLeft += 1;
                }
            }
            uc.drawPointDebug(new Location(0, a), 255, 0, 0);
            int optimalOxygen = (int) GameConstants.MIN_OXYGEN_ASTRONAUT + 5;
            optimalOxygen += 3-(optimalOxygen%3);

            if ((int)(uc.getRandomDouble() * 7) == 1 && spotsLeft > 1){
                while (true){
                    int ind = (int)(uc.getRandomDouble()*3);
                    if (validHQSpot[ind]){
                        optimalOxygen += ind;
                        optimalOxygen += 3*Math.ceil((Math.ceil(Math.sqrt(oppHQLoc[ind].distanceSquared(uc.getLocation())))-optimalOxygen)/3.0);
                        break;
                    }
                }
                int numSpawned = 0;
                for (Direction d: Direction.values()){
                    if (numSpawned > 3){
                        break;
                    }
                    numSpawned ++;
                    if (uc.canEnlistAstronaut(d, (int) optimalOxygen, CarePackage.REINFORCED_SUIT)) {
                        uc.enlistAstronaut(d, (int) optimalOxygen, CarePackage.REINFORCED_SUIT);
                    } else if (uc.canEnlistAstronaut(d, (int) optimalOxygen, CarePackage.SURVIVAL_KIT)) {
                        uc.enlistAstronaut(d, (int) optimalOxygen, CarePackage.SURVIVAL_KIT);
                    } else if (uc.canEnlistAstronaut(d, (int) optimalOxygen, null)) {
                        uc.enlistAstronaut(d, (int) optimalOxygen, null);
                    }
                }
            }
            int gatherOxygen = (int) GameConstants.MIN_OXYGEN_ASTRONAUT + Math.min(10, uc.getRound()/50) + (int)(uc.getRandomDouble()*3);

            if (uc.getRound()>800 && spotsLeft == 1){
                int ind = 0;
                for (int i = 0; i < 3; i++){
                    if (validHQSpot[i]){
                        ind = i;
                        break;
                    }
                }
                optimalOxygen += ind;
                optimalOxygen += 3*Math.ceil((Math.ceil(Math.sqrt(oppHQLoc[ind].distanceSquared(uc.getLocation())))-optimalOxygen)/3.0);

                for (Direction d: Direction.values()){
                    if (uc.canEnlistAstronaut(d, (int) optimalOxygen*4, CarePackage.REINFORCED_SUIT) && (int)(uc.getRandomDouble()*5) == 1) {
                        uc.enlistAstronaut(d, (int) optimalOxygen*4, CarePackage.REINFORCED_SUIT);
                    } else if (uc.canEnlistAstronaut(d, (int) optimalOxygen, CarePackage.SURVIVAL_KIT)) {
                        uc.enlistAstronaut(d, (int) optimalOxygen, CarePackage.SURVIVAL_KIT);
                    } else if (uc.canEnlistAstronaut(d, (int) optimalOxygen, null)) {
                        uc.enlistAstronaut(d, (int) optimalOxygen, null);
                    }
                }
            }

            if (needShield(enemies)) {
                uc.drawPointDebug(uc.getLocation(), 0, 0, 255);
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
        ans[0] = new Location(x, height - y-1);
        ans[1] = new Location(width - x-1, y);
        ans[2] = new Location(width - x-1, height - y-1);
        return ans;
    }

    boolean needShield(AstronautInfo[] enemies) {
        int enemyScore = enemies.length;
        for (int i = enemies.length; i --> 0;) {
            if (enemies[i].getLocation().distanceSquared(uc.getLocation()) <= 20) ++enemyScore;
            if (enemies[i].getCarePackage() == CarePackage.REINFORCED_SUIT) return true;
        }
        return enemyScore >= 4;
    }

    void buildShield() {
        for (Direction d : Direction.values()) {
            if (uc.canEnlistAstronaut(d, (int) GameConstants.MIN_OXYGEN_ASTRONAUT, CarePackage.REINFORCED_SUIT)) {
                uc.enlistAstronaut(d, (int) GameConstants.MIN_OXYGEN_ASTRONAUT, CarePackage.REINFORCED_SUIT);
            }
            else if (uc.canEnlistAstronaut(d, (int) GameConstants.MIN_OXYGEN_ASTRONAUT, CarePackage.SURVIVAL_KIT)) {
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
