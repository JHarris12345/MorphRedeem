package net.naturva.morphie.mr.redeem;

import com.gmail.nossr50.api.ExperienceAPI;
import net.md_5.bungee.api.ChatColor;
import net.naturva.morphie.mr.MorphRedeem;
import net.naturva.morphie.mr.util.DataManager;
import net.naturva.morphie.mr.util.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

// The credit redeem flow: a player picks a skill in the menu and is asked how many credits to add.
//
// The amount is asked for with a dialog (Java) or a form (Bedrock), never chat if it can be helped.
// Chat goes through the proxy, and a muted player's chat is cancelled there before this server ever
// sees it, so the old "write the amount in chat" prompt was one a muted player could never answer.
// A dialog or a form is a packet of its own and is not touched by a mute. The chat prompt only
// remains as the fallback when neither could be shown.
//
// All three paths end in apply(), which holds the one copy of the parse / validate / award logic
public class CreditRedeem {

	private final MorphRedeem plugin;

	public CreditRedeem(MorphRedeem plugin) {
		this.plugin = plugin;
	}

	// Called from the menu click. Closes the menu and asks for the amount
	public void start(Player player, String skill) {
		player.closeInventory();

		if (openBedrockForm(player, skill)) return;
		if (openDialog(player, skill)) return;

		// Chat fallback: exactly the old flow
		String credits = new DataManager(plugin).getData(player.getUniqueId(), "Credits");
		player.sendMessage(new StringUtils().addColor(plugin.getMessage("Prefix") + plugin.getMessage("CreditAssignmentMessage").replace("%CREDITS%", credits)));
		plugin.addCredits.put(player, skill);
	}

	// The whole Bedrock branch sits behind "is floodgate installed": floodgate is the thing that
	// would deliver the form, so without it there is no form to send and no reason to load the class
	// that names its API. Checking the player first would load BedrockRedeemPrompt on a server with
	// no floodgate and fail on the classes it references
	private boolean openBedrockForm(Player player, String skill) {
		if (!Bukkit.getPluginManager().isPluginEnabled("floodgate")) return false;

		try {
			if (!BedrockRedeemPrompt.isBedrockPlayer(player)) return false;
			return BedrockRedeemPrompt.open(plugin, player, skill);
		} catch (Throwable t) {
			plugin.getLogger().warning("Could not show the redeem form to " + player.getName() + ": " + t);
			return false;
		}
	}

	private boolean openDialog(Player player, String skill) {
		try {
			RedeemDialog.open(plugin, player, skill);
			return true;
		} catch (Throwable t) {
			plugin.getLogger().warning("Could not show the redeem dialog to " + player.getName() + ": " + t);
			return false;
		}
	}

	// Applies whatever was typed, from chat, the dialog box or the form. Must run on the main thread
	public void apply(Player player, String skill, String input) {
		UUID uuid = player.getUniqueId();
		StringUtils strings = new StringUtils();

		String chatMessage = input == null ? "" : input;
		String ignoreFromChat = plugin.getMessage("IgnoreFormat");
		if (chatMessage.contains(ignoreFromChat)) {
			chatMessage = chatMessage.replaceAll(ignoreFromChat, "");
		}
		if (chatMessage.contains(" ")) {
			chatMessage = chatMessage.replaceAll(" ", "");
		}
		chatMessage = ChatColor.stripColor(strings.addColor(chatMessage));

		int amountToAdd;
		try {
			amountToAdd = Integer.parseInt(chatMessage);
		} catch (NumberFormatException e1) {
			player.sendMessage(strings.addColor(plugin.getMessage("ErrorPrefix") + plugin.getMessage("InvalidNumber")));
			return;
		}

		int credits = Integer.parseInt(new DataManager(plugin).getData(uuid, "Credits"));
		if (credits < amountToAdd) {
			player.sendMessage(strings.addColor(plugin.getMessage("ErrorPrefix") + plugin.getMessage("InvalidCredits")));
			return;
		}
		if (amountToAdd < 0) {
			player.sendMessage(strings.addColor(plugin.getMessage("ErrorPrefix") + plugin.getMessage("InvalidNumberNegative")));
			return;
		}
		if (amountToAdd == 0) {
			player.sendMessage(strings.addColor(plugin.getMessage("Prefix") + plugin.getMessage("CreditAssignmentCanceled")));
			return;
		}

		int cap = ExperienceAPI.getLevelCap(skill);
		if (ExperienceAPI.getLevel(player, skill) + amountToAdd > cap) {
			String message = plugin.getMessage("SkillCapReached");
			if (message.contains("%SKILL%")) {
				message = message.replaceAll("%SKILL%", skill);
			}
			if (message.contains("%CAP%")) {
				message = message.replaceAll("%CAP%", "" + cap);
			}
			if (message.contains("%LEVEL%")) {
				message = message.replaceAll("%LEVEL%", "" + (ExperienceAPI.getLevel(player, skill) + amountToAdd));
			}
			player.sendMessage(strings.addColor(plugin.getMessage("ErrorPrefix") + message));
			return;
		}

		new DataManager(plugin).updateData(uuid, +amountToAdd, "Credits_Spent", "add");
		new DataManager(plugin).updateData(uuid, -amountToAdd, "Credits", "remove");

		ExperienceAPI.addLevel(player, skill, amountToAdd);
		String message = plugin.getMessage("CreditAssignmentSuccess");
		if (message.contains("%SKILL%")) {
			message = message.replaceAll("%SKILL%", skill);
		}
		if (message.contains("%CREDITS%")) {
			message = message.replaceAll("%CREDITS%", "" + amountToAdd);
		}
		player.sendMessage(strings.addColor(plugin.getMessage("Prefix") + message));
	}

	// The text shown on the dialog and the form, with the skill and credit balance filled in
	public String text(Player player, String key, String skill) {
		String credits = new DataManager(plugin).getData(player.getUniqueId(), "Credits");
		return plugin.getMessage(key).replace("%SKILL%", skill).replace("%CREDITS%", credits);
	}
}
