package com.example.blocks;

import com.example.Main;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModBlocks {
    public static Block registerBlock(String name, Function<AbstractBlock.Settings, Block> blockFactory, AbstractBlock.Settings settings, boolean shouldRegisterItem) {
        // Create the block key.
        RegistryKey<Block> blockKey = keyOfBlock(name); // RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Main.MOD_ID, name));

        // Create the block instance.
        Block block = blockFactory.apply(settings.registryKey(blockKey));

        // Register the block.
        if (shouldRegisterItem) {
            // Items need to be registered with a different type of registry key, but the ID
            // can be the same.
            RegistryKey<Item> itemKey = keyOfItem(name);

            BlockItem blockItem = new BlockItem(block, new Item.Settings().registryKey(itemKey));
            Registry.register(Registries.ITEM, itemKey, blockItem);
        }

        return Registry.register(Registries.BLOCK, blockKey, block);
    }

    private static RegistryKey<Block> keyOfBlock(String name) {
        return RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(Main.MOD_ID, name));
    }

    private static RegistryKey<Item> keyOfItem(String name) {
        return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Main.MOD_ID, name));
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register((itemGroup) -> itemGroup.add(ModBlocks.SERVER_COMMAND.asItem()));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register((itemGroup) -> itemGroup.add(ModBlocks.SERVER_OUTLINE.asItem()));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register((itemGroup) -> itemGroup.add(ModBlocks.STYLE_COMMAND.asItem()));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register((itemGroup) -> itemGroup.add(ModBlocks.STYLE_OUTLINE.asItem()));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register((itemGroup) -> itemGroup.add(ModBlocks.COUNTER_BLOCK.asItem()));
    }

    public static final Block SERVER_COMMAND = registerBlock("server_command", ServerCommandBlock::new, AbstractBlock.Settings.create().sounds(BlockSoundGroup.AMETHYST_BLOCK), true);
    public static final Block SERVER_OUTLINE = registerBlock("server_outline", Block::new, AbstractBlock.Settings.create().sounds(BlockSoundGroup.AMETHYST_BLOCK), true);

    public static final Block STYLE_COMMAND = registerBlock("style_command", StyleCommandBlock::new, AbstractBlock.Settings.create().sounds(BlockSoundGroup.AMETHYST_BLOCK), true);
    public static final Block STYLE_OUTLINE = registerBlock("style_outline", Block::new, AbstractBlock.Settings.create().sounds(BlockSoundGroup.AMETHYST_BLOCK), true);

    public static final Block COUNTER_BLOCK = registerBlock("counter", CounterBlock::new, AbstractBlock.Settings.create(), true);
}
