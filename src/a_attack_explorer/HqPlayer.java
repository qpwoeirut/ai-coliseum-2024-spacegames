package a_attack_explorer;

import a_attack_explorer.util.Util;
import aic2024.user.*;

public class HqPlayer extends BasePlayer {
    final int MIN_OXYGEN = (int) GameConstants.MIN_OXYGEN_ASTRONAUT + 1;
    int OPTIMAL_OXYGEN = (int) GameConstants.MIN_OXYGEN_ASTRONAUT + 10;

    HqPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        final float VISION = uc.getType().getVisionRange();
        calculateOptimalOxygen();
        while (true) {
            int turnNumber = uc.getRound();
            final AstronautInfo[] enemies = uc.senseAstronauts(VISION, uc.getOpponent());
//            broadcastEnemies(enemies);
            final Location[] oppHQLoc = findOppHQ();
            double sum = 0;
            for (int i = 0; i < oppHQLoc.length; i++){
                sum += Math.sqrt(oppHQLoc[i].distanceSquared(uc.getLocation()));
            }
            double average = sum/((double)oppHQLoc.length);
            if (average <= 20){
                OPTIMAL_OXYGEN = (int) GameConstants.MIN_OXYGEN_ASTRONAUT + turnNumber/25;
            }
            else{
                OPTIMAL_OXYGEN = (int) GameConstants.MIN_OXYGEN_ASTRONAUT + 10;
            }
            broadcastDirections(oppHQLoc);
            if (enemies.length > 0) {
                buildShield();
            } else if (uc.getRound() % 4 == 0) {
                Direction d = Direction.values()[(int) (uc.getRandomDouble() * 8)];
                uc.println(GameConstants.MIN_OXYGEN_ASTRONAUT);
                uc.println(OPTIMAL_OXYGEN);
                uc.println(uc.canEnlistAstronaut(d, OPTIMAL_OXYGEN, null));
                if (uc.canEnlistAstronaut(d, OPTIMAL_OXYGEN, null)) {
                    uc.enlistAstronaut(d, OPTIMAL_OXYGEN, null);
                }
            }

            uc.yield();
        }
    }

    Location[] findOppHQ(){
        int height = uc.getMapHeight();
        int width = uc.getMapWidth();
        int x = uc.getLocation().x;
        int y = uc.getLocation().y;
        Location[] ans = new Location[3];
        ans[0] = new Location(x, height-y);
        ans[1] = new Location(width-x, y);
        ans[2] = new Location(width-x, height-y);
        return ans;
    }

    void calculateOptimalOxygen(){
        int turnNumber = uc.getRound();
        int totalOxygen = Util.getOxygenOfStructure(uc);
        OPTIMAL_OXYGEN = Math.min(totalOxygen/20,turnNumber/200+10)+ (int) GameConstants.MIN_OXYGEN_ASTRONAUT;
    }
    void buildShield() {
        for (Direction d : Direction.values()) {
            if (uc.canEnlistAstronaut(d, MIN_OXYGEN, null)) {
                uc.enlistAstronaut(d, MIN_OXYGEN, null);
            }
        }
    }

    void broadcastDirections(Location[] locations){
        uc.cleanBroadcastBuffer();
        for (int i = 500; i >= 0; --i) {
            Location chosen = locations[(int)(Math.random()*3)];
//            Location chosen = locations[2];
            int val = chosen.x*1000 + chosen.y;
            if (uc.canPerformAction(ActionType.BROADCAST, null, val)) {
                uc.performAction(ActionType.BROADCAST, null, val);
            }
        }
    }
    void broadcastEnemies(AstronautInfo[] enemies) {
        uc.cleanBroadcastBuffer();
        for (int i = 500; i >= 0; --i) {
            if (uc.canPerformAction(ActionType.BROADCAST, null, enemies.length)) {
                uc.performAction(ActionType.BROADCAST, null, enemies.length);
            }
        }
    }
}
