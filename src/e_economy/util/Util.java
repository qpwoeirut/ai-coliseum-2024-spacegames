package e_economy.util;

import aic2024.user.*;

import java.util.Arrays;

public class Util {
    public static Direction toCenter(Location loc, int width, int height) {
        return loc.directionTo(new Location(width / 2, height / 2));
    }

    public static Direction toAdjacentHq(UnitController uc) {
        StructureInfo structure = uc.senseStructure(uc.getLocation().add(Direction.NORTH));
        if (structure != null && structure.getType() == StructureType.HQ) return Direction.NORTH;

        structure = uc.senseStructure(uc.getLocation().add(Direction.NORTHEAST));
        if (structure != null && structure.getType() == StructureType.HQ) return Direction.NORTHEAST;

        structure = uc.senseStructure(uc.getLocation().add(Direction.EAST));
        if (structure != null && structure.getType() == StructureType.HQ) return Direction.EAST;

        structure = uc.senseStructure(uc.getLocation().add(Direction.SOUTHEAST));
        if (structure != null && structure.getType() == StructureType.HQ) return Direction.SOUTHEAST;

        structure = uc.senseStructure(uc.getLocation().add(Direction.SOUTH));
        if (structure != null && structure.getType() == StructureType.HQ) return Direction.SOUTHEAST;

        structure = uc.senseStructure(uc.getLocation().add(Direction.SOUTHWEST));
        if (structure != null && structure.getType() == StructureType.HQ) return Direction.SOUTHWEST;

        structure = uc.senseStructure(uc.getLocation().add(Direction.WEST));
        if (structure != null && structure.getType() == StructureType.HQ) return Direction.WEST;

        structure = uc.senseStructure(uc.getLocation().add(Direction.NORTHWEST));
        if (structure != null && structure.getType() == StructureType.HQ) return Direction.NORTHWEST;

        return null;
    }

    public static int randRange(UnitController uc, int low, int high){
        //TODO: maybe change to math.random instead of uc random
        // [low, high]
        return low + (int) uc.getRandomDouble()*(high-low+1);

    }

    public static Direction randomDirection(UnitController uc, Direction mainDirection, int spread){
        //TODO: add distribution to direction
        Direction[] directions = Direction.values();
        int idx = Arrays.asList(directions).indexOf(mainDirection);
        return directions[randRange(uc, idx-spread, idx+spread)%8];
    }

    public static boolean isEmptyTile(UnitController uc){
        return true;
    }

    public static int getOxygenOfStructure(UnitController uc){
        return 5000;
    }

    public static boolean tryMove(UnitController uc, Direction dir) {
        if (uc.canPerformAction(ActionType.MOVE, dir, 0)) {
            uc.performAction(ActionType.MOVE, dir, 0);
//            uc.println("try " + dir + " success");
            return true;
        }
//        uc.println("try " + dir + " fail");
        return false;
    }

    public static boolean makeRandomMove(UnitController uc) {
        final int i = (int) (uc.getRandomDouble() * 8);
        return tryMove(uc, Direction.values()[i]) ||
                tryMove(uc, Direction.values()[(i + 1) % 8]) ||
                tryMove(uc, Direction.values()[(i + 2) % 8]) ||
                tryMove(uc, Direction.values()[(i + 3) % 8]) ||
                tryMove(uc, Direction.values()[(i + 4) % 8]) ||
                tryMove(uc, Direction.values()[(i + 5) % 8]) ||
                tryMove(uc, Direction.values()[(i + 6) % 8]) ||
                tryMove(uc, Direction.values()[(i + 7) % 8]);
    }

    public static float visionRadius(UnitController uc) {
        return uc.isStructure() ? uc.getType().getVisionRange() : GameConstants.ASTRONAUT_VISION_RANGE;
    }

    public static float oxygenCost(UnitController uc) {
        float cost = 1;
        if (uc.isTerraformed(uc.getLocation())) cost *= 0.5f;
        if (uc.getAstronautInfo().getCarePackage() == CarePackage.SURVIVAL_KIT) cost *= 0.5f;
        return cost;
    }

    public static Location[] symmetricLocations(Location loc, int width, int height) {
        Location[] symmetries = new Location[3];
        symmetries[0] = new Location(width - loc.x, loc.y);
        symmetries[1] = new Location(loc.x, height - loc.y);
        symmetries[2] = new Location(width - loc.y, height - loc.y);
        return symmetries;
    }

    public static int weightedRandom(double randVal, int[] weights) {
        int total = 0;
        for (int i = weights.length; i --> 0; ) total += weights[i];
        int random = (int)(randVal * total);
        for (int i = weights.length; i --> 0; ) {
            if (random < weights[i]) return i;
            random -= weights[i];
        }
        return 0;
    }
}