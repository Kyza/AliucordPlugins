package com.aliucord.plugins;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aliucord.Utils;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.Logger;

import com.aliucord.patcher.PinePatchFn;
import com.discord.api.commands.ApplicationCommandType;
import com.discord.models.commands.ApplicationCommandOption;

import com.discord.stores.StoreStream;

import java.util.*;

public class GhostMessage extends Plugin {
	public static Logger logger = new Logger("GhostMessage");

	@NonNull
	@Override
	public Manifest getManifest() {
		Manifest manifest = new Manifest();
		manifest.authors = new Manifest.Author[]{ new Manifest.Author("Kyza", 220584715265114113L) };
		manifest.description = "Deletes all messages you send immediately.";
		manifest.version = "1.0.1";
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
				this.logger.info(context, "Enabled Ghost Message.");
				return new CommandsAPI.CommandResult("Enabled Ghost Message.", null, false);
			}
			this.logger.info(context, "Disabled Ghost Message.");
			return new CommandsAPI.CommandResult("Disabled Ghost Message.", null, false);
		});

		StoreStream.getGatewaySocket().getMessageCreate().I().T(Utils.createActionSubscriber(message -> {
			if (message == null) return;
			// It's for readability ok don't judge me.
			if (this.sets.getBool("enabled", false)) {
				if (message.getEditedTimestamp() == 0 && message.getAuthor().f() == StoreStream.getUsers().getMe().getId()) {
					if (StoreStream.getChannelsSelected().getId() == message.getChannelId()) {
						StoreStream.getMessages().deleteMessage(message);
					}
				}
			}
		}));

		// Bad. Doesn't detect switch to DMs or guilds.
		patcher.patch("com.discord.utilities.channel.ChannelSelector$gotoChannel$1", "invoke", new Class[]{}, new PinePatchFn(callFrame -> {
			if (!this.sets.getBool("permanent", false) && this.sets.getBool("enabled", false)) {
				this.sets.setBool("enabled", false);
				this.logger.info(context, "Disabled Ghost Message automatically.");
			}
		}));
	}

	@Override
	public void stop(Context context) {
		patcher.unpatchAll();
		commands.unregisterAll();
	}

	public static Object nullish(Object obj, Object value) {
		return (obj != null) ? obj : value;
	}
}
