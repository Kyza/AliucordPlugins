package com.aliucord.plugins;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.utils.RxUtils;
import com.aliucord.wrappers.messages.MessageWrapper;
import com.discord.api.commands.ApplicationCommandType;
import com.discord.api.utcdatetime.UtcDateTime;
import com.discord.models.commands.ApplicationCommandOption;
import com.discord.models.user.CoreUser;
import com.discord.stores.SelectedChannelAnalyticsLocation;
import com.discord.stores.StoreChannelsSelected;
import com.discord.stores.StoreStream;

import java.util.Arrays;
import java.util.List;

import rx.Subscription;

public class GhostMessage extends Plugin {
	private static Logger logger = new Logger("GhostMessage");
	private Subscription messagesSubscription;

	private static ApplicationCommandOption enabledArg = new ApplicationCommandOption(ApplicationCommandType.BOOLEAN, "enabled", "Whether to enable or disable deleting messages.", null, true, false, null, null);
	private static ApplicationCommandOption permanentArg = new ApplicationCommandOption(ApplicationCommandType.BOOLEAN, "permanent", "Whether to automatically disable deleting messages.", null, false, false, null, null);
	private static List<ApplicationCommandOption> arguments = Arrays.asList(enabledArg, permanentArg);

	@NonNull
	@Override
	public Manifest getManifest() {
		Manifest manifest = new Manifest();
		manifest.authors = new Manifest.Author[]{ new Manifest.Author("Kyza", 220584715265114113L) };
		manifest.description = "Deletes all messages you send immediately.";
		manifest.version = "1.0.5";
		manifest.updateUrl = "https://raw.githubusercontent.com/Kyza/AliucordPlugins/builds/updater.json";
		return manifest;
	}

	@Override
	public void start(Context context) {
		this.commands.registerCommand("ghostmessage", "Enables or disables GhostMessage.", arguments, args -> {
			boolean enabled = (boolean)args.get("enabled");
			boolean permanent = (boolean)this.nullish(args.get("permanent"), false);

			this.sets.setBool("permanent", permanent);
			this.toggleGhosts(enabled);

			return new CommandsAPI.CommandResult(null, null, false);
		});

		if (this.sets.getBool("enabled", false)) {
			this.toggleGhosts(true);
		}
	}

	private PinePatchFn onNavigate = new PinePatchFn(callFrame -> {
		if (this.sets.getBool("enabled", false) && !this.sets.getBool("permanent", false)) {
			this.sets.setBool("enabled", false);
			this.toggleGhosts(false, true);
		}
	});

	private void toggleGhosts(boolean enable) {
		this.toggleGhosts(enable, false);
	}
	private void toggleGhosts(boolean enable, boolean automatically) {
		this.sets.setBool("enabled", enable);
		if (enable) {
			this.enableGhosts();
		} else {
			this.disableGhosts(automatically);
		}
	}

	private void enableGhosts() {
		this.messagesSubscription = RxUtils.subscribe(RxUtils.onBackpressureBuffer(StoreStream.getGatewaySocket().getMessageCreate()), RxUtils.createActionSubscriber(message -> {
			if (message == null) return;
			MessageWrapper wrappedMessage = new MessageWrapper(message);
			CoreUser coreUser = new CoreUser(wrappedMessage.getAuthor());
			if (wrappedMessage.getEditedTimestamp() == null && coreUser.getId() == StoreStream.getUsers().getMe().getId() && StoreStream.getChannelsSelected().getId() == wrappedMessage.getChannelId()) {
				StoreStream.getMessages().deleteMessage(message);
			}
		}));
		this.patcher.patch(StoreChannelsSelected.class, "handleGuildSelected", new Class[]{}, this.onNavigate);
		this.patcher.patch(StoreChannelsSelected.class, "trySelectChannel", new Class[]{long.class, long.class, Long.class, SelectedChannelAnalyticsLocation.class}, this.onNavigate);
		this.logger.info(Utils.appActivity, "Enabled GhostMessage.");
	}

	private void disableGhosts(boolean automatically) {
		if (this.messagesSubscription != null) this.messagesSubscription.unsubscribe();
		// All patches can be disabled.
		// Normally save only ones that need to be and disable them individually here.
		this.patcher.unpatchAll();
		if (automatically) {
			this.logger.info(Utils.appActivity, "Disabled GhostMessage automatically.");
		} else {
			this.logger.info(Utils.appActivity, "Disabled GhostMessage.");
		}
	}

	@Override
	public void stop(Context context) {
		this.toggleGhosts(false);
		this.patcher.unpatchAll();
		this.commands.unregisterAll();
	}

	private static Object nullish(Object obj, Object value) {
		return (obj != null) ? obj : value;
	}
}
