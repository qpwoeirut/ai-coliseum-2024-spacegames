package a1_hotspot_turtle_player.util;

import aic2024.user.*;

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

    public static boolean isEmptyTile(UnitController uc){
        return true;
    }

    public static int getOxygenOfStructure(UnitController uc){
        return 5000;
    }
}
