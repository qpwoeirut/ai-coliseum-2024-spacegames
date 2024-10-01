package b_bugnav;

import aic2024.user.UnitController;

abstract public class BasePlayer {
    protected final UnitController uc;

    BasePlayer(UnitController uc) {
        this.uc = uc;
    }

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