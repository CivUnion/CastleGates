/**
 * Created by Aleksey on 02.06.2017.
 */

package com.aleksey.castlegates.plugins.citadel;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import vg.civcraft.mc.citadel.model.Reinforcement;

public class Citadel implements ICitadel {
    private List<Player> players;
    private String groupName;
    private boolean hasAccess;
    private boolean useJukeAlert;

    public Citadel(List<Player> players, Reinforcement playerRein, boolean hasAccess, boolean useJukeAlert) {
        this.players = players;
        this.groupName = playerRein != null ? playerRein.getGroup().getName() : null;
        this.hasAccess = hasAccess;
        this.useJukeAlert = useJukeAlert;
    }

    public boolean useJukeAlert() { return this.useJukeAlert; }

    public String getGroupName() {
        return this.groupName;
    }

    public boolean canAccessDoors(Location location) {
        if(!this.hasAccess) return false;

        Reinforcement rein = vg.civcraft.mc.citadel.Citadel.getInstance().getReinforcementManager().getReinforcement(location);

        return rein == null || !(rein instanceof Reinforcement)
            ? this.groupName == null
            : this.groupName != null && this.groupName.equalsIgnoreCase(((Reinforcement)rein).getGroup().getName());
    }
}