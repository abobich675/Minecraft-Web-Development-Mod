package com.example.blocks;

import com.example.Main;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
//    Register Block Entities
    private static <T extends BlockEntity> BlockEntityType<T> registerBlock(
            String name,
            FabricBlockEntityTypeBuilder.Factory<? extends T> entityFactory,
            Block... blocks
    ) {
        Identifier id = Identifier.of(Main.MOD_ID, name);
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, id, FabricBlockEntityTypeBuilder.<T>create(entityFactory, blocks).build());
    }

    public static void initialize() {}

    public static final BlockEntityType<CounterBlockEntity> COUNTER_BLOCK_ENTITY = registerBlock("counter_block", CounterBlockEntity::new, ModBlocks.COUNTER_BLOCK);
}
