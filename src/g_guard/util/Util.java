package g_guard.util;

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

    public static boolean isOpenTile(UnitController uc, Direction dir) {
        return isEmptyTile(uc, uc.getLocation().add(dir)) &&
                isEmptyTile(uc, uc.getLocation().add(dir).add(Direction.NORTH)) &&
                isEmptyTile(uc, uc.getLocation().add(dir).add(Direction.NORTHWEST)) &&
                isEmptyTile(uc, uc.getLocation().add(dir).add(Direction.WEST)) &&
                isEmptyTile(uc, uc.getLocation().add(dir).add(Direction.SOUTHWEST)) &&
                isEmptyTile(uc, uc.getLocation().add(dir).add(Direction.SOUTH)) &&
                isEmptyTile(uc, uc.getLocation().add(dir).add(Direction.SOUTHEAST)) &&
                isEmptyTile(uc, uc.getLocation().add(dir).add(Direction.EAST)) &&
                isEmptyTile(uc, uc.getLocation().add(dir).add(Direction.NORTHEAST));
    }

    public static boolean isEmptyTile(UnitController uc, Location loc) {
        final MapObject obj = uc.senseObjectAtLocation(loc);
        return !uc.isOutOfMap(loc) && (obj == MapObject.TERRAFORMED || obj == MapObject.DOMED_TILE || obj == MapObject.LAND) && uc.senseStructure(loc) == null;
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
        return (int)(randVal * weights.length);
    }

    public static int hitsFromSuit(float oxygen) {
        if (oxygen <= 5) return 1;
        if (oxygen <= 15) return 2;
        if (oxygen <= 35) return 3;
        if (oxygen <= 75) return 4;
        if (oxygen <= 155) return 5;
        if (oxygen <= 315) return 6;
        if (oxygen <= 635) return 7;
        if (oxygen <= 1275) return 7;
        if (oxygen <= 2555) return 8;
        if (oxygen <= 5115) return 9;
        if (oxygen <= 10235) return 10;
        if (oxygen <= 20475) return 11;
        return 12;
    }
}