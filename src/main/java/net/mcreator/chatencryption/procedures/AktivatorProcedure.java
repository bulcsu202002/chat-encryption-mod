package net.mcreator.chatencryption.procedures;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class AktivatorProcedure {
	public static boolean eventResult = true;

	public AktivatorProcedure() {
		ClientLifecycleEvents.CLIENT_STARTED.register((client) -> {
			execute();
		});
	}

	public static void execute() {
		net.mcreator.chatencryption.ChatDecrypter.init();
	}
}