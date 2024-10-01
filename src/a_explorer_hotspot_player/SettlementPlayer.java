package a_explorer_hotspot_player;

import a_explorer_hotspot_player.BasePlayer;
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
