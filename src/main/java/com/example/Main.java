package com.example;

import com.example.blocks.ModBlockEntities;
import com.example.blocks.ModBlocks;
import com.example.items.ModItems;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraft.item.Item;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ModInitializer {


	public static final String MOD_ID = "webdevmod";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ModItems.initialize();
		ModBlocks.initialize();
		ModBlockEntities.initialize();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("test_command").executes(context -> {
				context.getSource().sendFeedback(() -> Text.literal("Called /test_command."), false);
				return 1;
			}));
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			if (environment.dedicated) {
				dispatcher.register(CommandManager.literal("dedicated_command")
						.executes(WebDevCommands::executeDedicatedCommand));
			}
		});

		LOGGER.info("Hello from WebDevMod!");
	}
}