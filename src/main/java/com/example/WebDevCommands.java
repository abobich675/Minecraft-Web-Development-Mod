package com.example;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class WebDevCommands {
    public static int executeDedicatedCommand(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() -> Text.literal("Called /dedicated_command."), false);
        return 1;
    }
}
