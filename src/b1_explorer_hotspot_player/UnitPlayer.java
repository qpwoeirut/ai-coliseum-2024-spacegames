package b1_explorer_hotspot_player;

import aic2024.user.*;

public class UnitPlayer {
	public void run(UnitController uc) {
		if (uc.isStructure() && uc.getType() == StructureType.HQ) {
			new HqPlayer(uc).run();
		} else if (uc.isStructure() && uc.getType() == StructureType.SETTLEMENT) {
			new SettlementPlayer(uc).run();
		} else {
			new AstronautPlayer(uc).run();
		}
	}
}
