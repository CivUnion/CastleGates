package com.aleksey.castlegates.plugins.jukealert;

import org.bukkit.Location;

public class JukeAlertManager implements IJukeAlertManager {
    //private SnitchManager snitchManager = JukeAlert.getInstance().getSnitchManager();

	public boolean hasJukeAlertAccess(Location location, String groupName) {
		//if(!CastleGates.getConfigManager().getInteractWithSnitches()) return false;

		//Set<Snitch> snitches = this.snitchManager.findSnitches(location.getWorld(), location);

		//if (snitches.size() > 0) {
			//double distance = CastleGates.getCitadelManager().getMaxRedstoneDistance();

			//for (Snitch snitch : snitches) {
				//if (snitch.getGroup().getName().equalsIgnoreCase(groupName)
						//&& snitch.shouldToggleLevers()
						//&& snitch.getLoc().distance(location) <= distance
						//)
				//{
					//return true;
				//}
			//}
		//}

		return false;
	}
}
