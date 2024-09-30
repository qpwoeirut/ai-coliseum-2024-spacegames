package a_turtleplayer;

import aic2024.user.UnitController;

public class SettlementPlayer extends BasePlayer {
    SettlementPlayer(UnitController uc) {
        super(uc);
    }

    void run() {
        while (true) {
            uc.yield();
        }
    }
}
