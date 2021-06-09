package com.aliucord.plugins;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.utils.RxUtils;
import com.discord.api.commands.ApplicationCommandType;
import com.discord.models.commands.ApplicationCommandOption;
import com.discord.stores.SelectedChannelAnalyticsLocation;
import com.discord.stores.StoreChannelsSelected;
import com.discord.stores.StoreStream;

import java.util.Arrays;
import java.util.List;

import rx.Subscription;

public class GhostMessage extends Plugin {
	public static Logger logger = new Logger("GhostMessage");
	public Subscription subscription;

	@NonNull
	@Override
	public Manifest getManifest() {
		Manifest manifest = new Manifest();
		manifest.authors = new Manifest.Author[]{ new Manifest.Author("Kyza", 220584715265114113L) };
		manifest.description = "Deletes all messages you send immediately.";
		manifest.version = "1.0.2";
		manifest.updateUrl = "https://raw.githubusercontent.com/Kyza/AliucordPlugins/builds/updater.json";
		return manifest;
	}

	@Override
	public void start(Context context) {
		ApplicationCommandOption enabledArg = new ApplicationCommandOption(ApplicationCommandType.BOOLEAN, "enabled", "Whether to enable or disable deleting messages.", null, true, false, null, null);
		ApplicationCommandOption permanentArg = new ApplicationCommandOption(ApplicationCommandType.BOOLEAN, "permanent", "Whether to automatically disable deleting messages.", null, false, false, null, null);
		List<ApplicationCommandOption> arguments = Arrays.asList(enabledArg, permanentArg);

		commands.registerCommand("ghostmessage", "Enables or disables Ghost Message.", arguments, args -> {
			Boolean enabled = (Boolean)args.get("enabled");
			Boolean permanent = (Boolean)this.nullish(args.get("permanent"), false);

			this.sets.setBool("enabled", enabled);
			this.sets.setBool("permanent", permanent);

			if (enabled) {
				this.logger.info(Utils.appActivity, "Enabled Ghost Message.");
				return new CommandsAPI.CommandResult(null, null, false);
			}
			this.logger.info(Utils.appActivity, "Disabled Ghost Message.");
			return new CommandsAPI.CommandResult(null, null, false);
		});

		this.subscription = RxUtils.subscribe(RxUtils.onBackpressureBuffer(StoreStream.getGatewaySocket().getMessageCreate()), RxUtils.createActionSubscriber(message -> {
			if (message == null) return;
			// It's for readability ok don't judge me.
			if (this.sets.getBool("enabled", false)) {
				if (message.getEditedTimestamp() == 0 && message.getAuthor().i() == StoreStream.getUsers().getMe().getId()) {
					if (StoreStream.getChannelsSelected().getId() == message.getChannelId()) {
						StoreStream.getMessages().deleteMessage(message);
					}
				}
			}
		}));

		patcher.patch(StoreChannelsSelected.class, "handleGuildSelected", new Class[]{}, this.onNavigate);
		patcher.patch(StoreChannelsSelected.class, "trySelectChannel", new Class[]{long.class, long.class, Long.class, SelectedChannelAnalyticsLocation.class}, this.onNavigate);
	}

	public PinePatchFn onNavigate = new PinePatchFn(callFrame -> {
		if (!this.sets.getBool("permanent", false) && this.sets.getBool("enabled", false)) {
			this.sets.setBool("enabled", false);
			this.logger.info(Utils.appActivity, "Disabled Ghost Message automatically.");
		}
	});

	@Override
	public void stop(Context context) {
		patcher.unpatchAll();
		commands.unregisterAll();
		if (subscription != null) subscription.unsubscribe();
	}

	public static Object nullish(Object obj, Object value) {
		return (obj != null) ? obj : value;
	}
}
