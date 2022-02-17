/**
 * @author Aleksey Terzi
 *
 */

package com.aleksey.castlegates.plugins.citadel;

import com.aleksey.castlegates.CastleGates;
import com.aleksey.castlegates.database.ReinforcementInfo;
import com.aleksey.castlegates.utils.Helper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.model.Reinforcement;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class CitadelManager extends Thread implements ICitadelManager, Runnable {
	private static class UpdatedReinforcement {
		public Reinforcement rein;
		public boolean deleted;

		public UpdatedReinforcement(Reinforcement rein, boolean deleted) {
			this.rein = rein;
			this.deleted = deleted;
		}
	}

	private long lastExecute = System.currentTimeMillis();
    private AtomicBoolean kill = new AtomicBoolean(false);
	private AtomicBoolean run = new AtomicBoolean(false);

    private List<UpdatedReinforcement> updatedReinforcements = new ArrayList<UpdatedReinforcement>();
	private Queue<UpdatedReinforcement> localUpdatedReinforcements = new ArrayDeque<UpdatedReinforcement>();

    public void init() {
    	startThread();
    }

    public void close() {
    	terminateThread();
    }

	public double getMaxRedstoneDistance() {
		return CastleGates.getConfigManager().getMaxRedstoneDistance();
	}

	public ICitadel getCitadel(List<Player> players, Location loc) {
		Reinforcement playerRein = null;
    	boolean hasAccess = false;
    	boolean useJukeAlert = false;

		Reinforcement rein = Citadel.getInstance().getReinforcementManager().getReinforcement(loc);

		hasAccess = rein == null || !(rein instanceof Reinforcement);

		if(!hasAccess) {
			playerRein = (Reinforcement)rein;

			if (players != null && players.size() > 0) {
				for (Player player : players) {
					if (playerRein.hasPermission(player, PermissionType.getPermission("DOORS")) || player.hasPermission("citadel.admin")) {
						hasAccess = true;
						break;
					}
				}
			}

			if(!hasAccess) {
				if(CastleGates.getJukeAlertManager().hasJukeAlertAccess(loc, playerRein.getGroup().getName())) {
					hasAccess = true;
					useJukeAlert = true;
				}
			}
		}

		return new com.aleksey.castlegates.plugins.citadel.Citadel(players, playerRein, hasAccess, useJukeAlert);
	}

	public boolean isReinforced(Location loc) {
		Reinforcement rein = Citadel.getInstance().getReinforcementManager().getReinforcement(loc);

		return rein != null && (rein instanceof Reinforcement);
	}

	public boolean canBypass(Player player, Location loc) {
		Reinforcement rein = Citadel.getInstance().getReinforcementManager().getReinforcement(loc);

		if(rein == null || !(rein instanceof Reinforcement)) return true;

		if(player == null) return false;

		Reinforcement playerRein = (Reinforcement)rein;

		return playerRein.hasPermission(player, PermissionType.getPermission("BYPASS"))
			|| player.hasPermission("citadel.admin.bypassmode");
	}

	public boolean canViewInformation(Player player, Location loc) {
		Reinforcement rein = Citadel.getInstance().getReinforcementManager().getReinforcement(loc);

		if(rein == null || !(rein instanceof Reinforcement)) return true;

		if(player == null) return false;

		Reinforcement playerRein = (Reinforcement)rein;

		return playerRein.hasPermission(player, PermissionType.getPermission("INFO"))
			|| player.hasPermission("citadel.admin.ctinfodetails");
	}

	public ReinforcementInfo removeReinforcement(Location loc) {
		Reinforcement rein = Citadel.getInstance().getReinforcementManager().getReinforcement(loc);

		if(rein == null || !(rein instanceof Reinforcement)) return null;

		Reinforcement playerRein = (Reinforcement)rein;

		ReinforcementInfo info = new ReinforcementInfo();
		info.material = playerRein.getType().getItem().getType();
		info.insecure = playerRein.isInsecure();
		info.group_id = playerRein.getGroupId();
		info.maturation_time = playerRein.getType().getMaturationTime();
		info.lore = Helper.getLore(playerRein.getType().getItem());
		info.acid_time = playerRein.getType().getAcidTime();

		synchronized(this.updatedReinforcements) {
			this.updatedReinforcements.add(new UpdatedReinforcement(rein, true));
		}

		return info;
	}

	public boolean createReinforcement(ReinforcementInfo info, Location loc) {
		if(info == null) return false;

		Group group = GroupManager.getGroup(info.group_id);
		ItemStack stack = getItemStack(info);

		Reinforcement rein = new Reinforcement(loc, Citadel.getInstance().getReinforcementTypeManager().getByItemStack(stack), group);

		synchronized(this.updatedReinforcements) {
			this.updatedReinforcements.add(new UpdatedReinforcement(rein, false));
		}

		return true;
	}

	private static ItemStack getItemStack(ReinforcementInfo info) {
		Material material = info.material;
		ItemStack stack = new ItemStack(material);

		Helper.setLore(stack, info.lore);

		return stack;
	}

    public void startThread() {
        setName("CastleGates CitadelManager Thread");
        setPriority(Thread.MIN_PRIORITY);
        start();

        CastleGates.getPluginLogger().log(Level.INFO, "CitadelManager thread started");
    }

	public void terminateThread() {
		this.kill.set(true);

		while (this.run.get()) {
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		saveData();

		CastleGates.getPluginLogger().log(Level.INFO, "CitadelManager thread stopped");
	}

    @Override
    public void run() {
    	this.run.set(true);

    	try {
			while (!this.isInterrupted() && !this.kill.get()) {
				try {
					long timeWait = lastExecute + CastleGates.getConfigManager().getDataWorkerRate() - System.currentTimeMillis();
					lastExecute = System.currentTimeMillis();
					if (timeWait > 0) {
						Thread.sleep(timeWait);
					}

					saveData();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
    		this.run.set(false);
		}
	}

	private void saveData() {
		try {
			synchronized (this.updatedReinforcements) {
				if (this.updatedReinforcements.size() == 0) return;

				this.localUpdatedReinforcements.addAll(this.updatedReinforcements);
				this.updatedReinforcements.clear();
			}

			UpdatedReinforcement updated;

			while ((updated = this.localUpdatedReinforcements.poll()) != null) {
				if (updated.deleted) {
					updated.rein.setHealth(0);
				} else {
					Citadel.getInstance().getReinforcementManager().putReinforcement(updated.rein);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
