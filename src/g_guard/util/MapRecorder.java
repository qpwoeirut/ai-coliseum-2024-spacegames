package g_guard.util;

import aic2024.user.Location;
import aic2024.user.MapObject;
import aic2024.user.UnitController;

// Based on https://github.com/carlguo866/battlecode23-gonefishin/blob/main/src/bot1/MapRecorder.java
public class MapRecorder {
    UnitController uc;

    public MapRecorder(UnitController uc) {
        this.uc = uc;
    }

    public final char SEEN_BIT = 1 << 4;
    public final char PASSABLE_BIT = 1 << 7;

    // Idea from https://github.com/carlguo866/battlecode23-gonefishin/blob/main/src/bot1/Constants.java
    // TODO: make sure the bytecode system works same in AIC as BC
    public final String ONE_HUNDRED_LEN_STRING = "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0";
    public final String SIX_HUNDRED_LEN_STRING = ONE_HUNDRED_LEN_STRING + ONE_HUNDRED_LEN_STRING + ONE_HUNDRED_LEN_STRING + ONE_HUNDRED_LEN_STRING + ONE_HUNDRED_LEN_STRING + ONE_HUNDRED_LEN_STRING;
    public final String MAP_LEN_STRING = SIX_HUNDRED_LEN_STRING + SIX_HUNDRED_LEN_STRING + SIX_HUNDRED_LEN_STRING + SIX_HUNDRED_LEN_STRING + SIX_HUNDRED_LEN_STRING + SIX_HUNDRED_LEN_STRING;
    public final char[] vals = MAP_LEN_STRING.toCharArray();

    public boolean maybePassable(Location loc) {
        if (uc.isOutOfMap(loc)) return false;
        final int val = vals[loc.x * uc.getMapHeight() + loc.y];
        if ((val & SEEN_BIT) == 0) return true;
        return (val & PASSABLE_BIT) > 0;
    }

    // record what we can sense on the map, perform sym check if needed
    // always called at the end of a turn and will run until all bytecode consumed
    public void recordInfo(int bytecodeLimit) {
        // Record water first since it matters for maybePassable
        Location[] locs = uc.senseObjects(MapObject.WATER, Util.visionRadius(uc));
        for (int i = locs.length; i-- > 0; ) {
            if (uc.getEnergyLeft() <= bytecodeLimit) return;
            vals[locs[i].x * uc.getMapHeight() + locs[i].y] = SEEN_BIT;
        }

        // Record land
        locs = uc.senseObjects(MapObject.LAND, Util.visionRadius(uc));
        for (int i = locs.length; i-- > 0; ) {
            if (uc.getEnergyLeft() <= bytecodeLimit) return;
            vals[locs[i].x * uc.getMapHeight() + locs[i].y] = SEEN_BIT | PASSABLE_BIT;
        }
    }
}