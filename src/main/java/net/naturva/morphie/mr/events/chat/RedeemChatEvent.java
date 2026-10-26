package net.naturva.morphie.mr.events.chat;

import net.naturva.morphie.mr.MorphRedeem;
import net.naturva.morphie.mr.redeem.CreditRedeem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

// The chat fallback. Only reached when neither the dialog nor the Bedrock form could be shown
// (see CreditRedeem.start). The amount itself is handled by CreditRedeem.apply on the main thread,
// since this event is async and the award touches player data and mcMMO
public class RedeemChatEvent implements Listener {

	private MorphRedeem plugin;

	public RedeemChatEvent(MorphRedeem plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onChat(AsyncPlayerChatEvent e) {
		Player player = e.getPlayer();
		if (plugin.addCredits.containsKey(player)) {
			e.setCancelled(true);
			String skill = this.plugin.addCredits.get(player);
			String chatMessage = e.getMessage();
			this.plugin.addCredits.remove(player);

			Bukkit.getScheduler().runTask(plugin, () -> {
				if (!player.isOnline()) return;
				new CreditRedeem(plugin).apply(player, skill, chatMessage);
			});
		}
	}
}
