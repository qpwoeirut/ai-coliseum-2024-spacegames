package e_economy;

import aic2024.user.UnitController;
import e_economy.util.Communication;

abstract public class BasePlayer {
    protected final UnitController uc;
    protected final Communication comms;

    BasePlayer(UnitController uc) {
        this.uc = uc;
        this.comms = new Communication(uc);
    }

    protected final int DIR_INCREMENT = 10_000;

    // a = [(round(25 * math.cos(2 * math.pi * (i / 16))), round(25 * math.sin(2 * math.pi * (i / 16)))) for i in range(16)][::-1]
    // str(a[9:] + a[:9]).replace('(', '{').replace(')', '}').replace('[', '{').replace(']', '}')
    // Rearranged to match the given Direction enum's order
    // NW, NNW, N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW
    protected final int[][] DIRECTIONS = new int[][]{
            {-18,  18},
            {-10,  23},
            {  0,  25},
            { 10,  23},
            { 18,  18},
            { 23,  10},
            { 25,   0},
            { 23, -10},
            { 18, -18},
            { 10, -23},
            {  0, -25},
            {-10, -23},
            {-18, -18},
            {-23, -10},
            {-25,   0},
            {-23,  10}
    };
    protected final int DIRS = DIRECTIONS.length;

    protected void endTurn() {
//        debugBytecode("end of turn");
        final int currentRound = uc.getRound();
        if (uc.getRound() == currentRound) uc.yield();
    }

    protected void debug(String message) {
        if (uc.getRound() <= 400) uc.println(message);
    }

    protected void debugBytecode(String message) {
        debug(message + " " + uc.getEnergyUsed());
    }
}