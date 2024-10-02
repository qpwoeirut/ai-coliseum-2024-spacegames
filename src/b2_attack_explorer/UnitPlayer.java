package b2_attack_explorer;

import aic2024.user.StructureType;
import aic2024.user.UnitController;

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
