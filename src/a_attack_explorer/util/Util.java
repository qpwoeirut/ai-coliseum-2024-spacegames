package a_attack_explorer.util;

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
}