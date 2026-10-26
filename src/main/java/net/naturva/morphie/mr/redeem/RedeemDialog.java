package net.naturva.morphie.mr.redeem;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.naturva.morphie.mr.MorphRedeem;
import net.naturva.morphie.mr.util.StringUtils;
import org.bukkit.entity.Player;

import java.util.List;

// The Java redeem screen: a dialog with one text box and a Redeem / Cancel row.
// Nothing here goes near chat, so a muted player can use it like anyone else
public class RedeemDialog {

	private static final String AMOUNT_KEY = "amount";

	// Dialog buttons and boxes take an explicit pixel width. The box is wider so a full amount is visible
	private static final int BUTTON_WIDTH = 150;
	private static final int INPUT_WIDTH = 200;

	// An int has at most 10 digits, so a box this size can never hold a value that fails to parse by length
	private static final int MAX_LENGTH = 10;

	public static void open(MorphRedeem plugin, Player player, String skill) {
		CreditRedeem redeem = new CreditRedeem(plugin);

		List<DialogBody> body = List.of(DialogBody.plainMessage(component(redeem.text(player, "Dialog.Body", skill))));

		List<DialogInput> inputs = List.of(DialogInput.text(AMOUNT_KEY, Component.empty())
				.maxLength(MAX_LENGTH)
				.width(INPUT_WIDTH)
				.build());

		// uses(1) because the dialog closes on a press, so the callback is never wanted twice.
		// Reopening builds fresh buttons with fresh callbacks. The callback arrives on the main thread
		ActionButton redeemButton = ActionButton.builder(component(plugin.getMessage("Dialog.RedeemButton")))
				.width(BUTTON_WIDTH)
				.action(DialogAction.customClick((view, audience) -> {
					if (!(audience instanceof Player clicker) || !clicker.isOnline()) return;
					redeem.apply(clicker, skill, view.getText(AMOUNT_KEY));
				}, ClickCallback.Options.builder().uses(1).build()))
				.build();

		// A null action just closes the dialog, which is all cancelling is. Escape does the same
		ActionButton cancel = ActionButton.create(component(plugin.getMessage("Dialog.CancelButton")), null, BUTTON_WIDTH, null);

		Dialog dialog = Dialog.create(builder -> builder.empty()
				.base(DialogBase.builder(component(redeem.text(player, "Dialog.Title", skill)))
						.canCloseWithEscape(true)
						.afterAction(DialogBase.DialogAfterAction.CLOSE)
						.body(body)
						.inputs(inputs)
						.build())
				.type(DialogType.multiAction(List.of(redeemButton))
						.exitAction(cancel)
						.columns(2)
						.build()));

		player.showDialog(dialog);
	}

	// Everything in this plugin is written with & codes, but dialogs want Components
	private static Component component(String text) {
		return LegacyComponentSerializer.legacySection().deserialize(new StringUtils().addColor(text));
	}
}
