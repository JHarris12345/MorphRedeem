package net.naturva.morphie.mr.redeem;

import net.naturva.morphie.mr.MorphRedeem;
import net.naturva.morphie.mr.util.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.regex.Pattern;

// The Bedrock redeem screen. A Bedrock client cannot render a dialog at all (nothing appears), so
// Bedrock players get a Floodgate form instead: one text box for the amount. Closing the form is the
// cancel.
//
// Every reference to Floodgate and Cumulus in this plugin lives in this class, and it is only ever
// touched after CreditRedeem has checked that floodgate is installed. No Floodgate or Cumulus type
// may appear in a method signature here, or a caller would pull the class in just by being verified
// and fail on a server without floodgate
public class BedrockRedeemPrompt {

	// Bedrock renders the plain legacy codes but prints the §x hex form as raw characters, so hex
	// colour is dropped rather than shown as gibberish
	private static final Pattern HEX_SEQUENCE = Pattern.compile("(?i)§x(?:§[0-9a-f]){6}");

	public static boolean isBedrockPlayer(Player player) {
		return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
	}

	// Returns false if floodgate would not deliver the form, so the caller can fall back
	public static boolean open(MorphRedeem plugin, Player player, String skill) {
		CreditRedeem redeem = new CreditRedeem(plugin);

		// An empty label leaves just the box and its placeholder
		CustomForm.Builder form = CustomForm.builder()
				.title(text(redeem.text(player, "Dialog.Title", skill)))
				.label(text(redeem.text(player, "Dialog.Body", skill)))
				.input("", text(plugin.getMessage("Dialog.Placeholder")));

		// The response arrives on Floodgate's thread, not the server thread, so hop before touching anything
		form.validResultHandler(response -> {
			String typed = response.asInput(0);

			Bukkit.getScheduler().runTask(plugin, () -> {
				if (!player.isOnline()) return;
				redeem.apply(player, skill, typed);
			});
		});

		return FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
	}

	private static String text(String text) {
		return HEX_SEQUENCE.matcher(new StringUtils().addColor(text)).replaceAll("");
	}
}
