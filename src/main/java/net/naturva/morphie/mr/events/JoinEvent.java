package net.naturva.morphie.mr.events;

import net.naturva.morphie.mr.MorphRedeem;
import net.naturva.morphie.mr.util.StringUtils;
import net.naturva.morphie.mr.util.UpdateChecker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinEvent implements Listener {
    private MorphRedeem plugin;

    public JoinEvent(MorphRedeem plugin) {
        this.plugin = plugin;
    }

}
