package com.example;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Enumeration;
import java.util.Hashtable;


public class Main implements ModInitializer {


	public static final String MOD_ID = "webdevmod";
	public static final int MAX_SIZE = 32;

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("host")
				.then (CommandManager.argument("pos", BlockPosArgumentType.blockPos())
					.executes(Main::hostCommand)));
		});

		LOGGER.info("Hello from WebDevMod!");
	}

	private static final Hashtable<Block, Site> sites = new Hashtable<>();

	private static int hostCommand(CommandContext<ServerCommandSource> context) {
		BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
		ServerWorld world = context.getSource().getWorld();
		Block block = world.getBlockState(pos).getBlock();

		Enumeration<Block> keys = sites.keys();

		boolean matchingBlock = false;
		while (keys.hasMoreElements()) {
			Block key = keys.nextElement();
			if (key.equals(block)) {
				matchingBlock = true;
				break;
			}
		}

		if (matchingBlock) {
			context.getSource().sendFeedback(() -> Text.literal("Could not host. Block already in use."), false);
			return 0;
		}

		PlayerEntity player;
		try {
			player = context.getSource().getPlayerOrThrow();
		}  catch (Exception e) {
			player = null;
		}

		Site newSite = new Site(player, world, pos);

		int result = newSite.tryStartServer();

		Identifier id = Registries.BLOCK.getId(block);
		if (result == 0) {
			context.getSource().sendFeedback(() -> Text.literal("Failed to host " + id + ". Invalid Setup."), false);
			return 0;
		}

		sites.put(block, newSite);
		context.getSource().sendFeedback(() -> Text.literal("Hosting " + id), false);
		return 1;
	}
}