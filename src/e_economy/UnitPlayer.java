package e_economy;

import aic2024.user.StructureType;
import aic2024.user.UnitController;

public class UnitPlayer {
	public void run(UnitController uc) {
		while (true) {
			try {
				if (uc.isStructure() && uc.getType() == StructureType.HQ) {
					new HqPlayer(uc).run();
				} else if (uc.isStructure() && uc.getType() == StructureType.SETTLEMENT) {
					new SettlementPlayer(uc).run();
				} else {
					new AstronautPlayer(uc).run();
				}
			} catch (Exception e) {
				uc.println("ERROR! " + e);
			}
		}
	}
}
