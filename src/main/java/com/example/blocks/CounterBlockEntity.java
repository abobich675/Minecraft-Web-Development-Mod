package com.example.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class CounterBlockEntity extends BlockEntity {
    public CounterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COUNTER_BLOCK_ENTITY, pos, state);
    }

    private int clicks = 0;
    public int getClicks() {
        return clicks;
    }

    public void incrementClicks() {
        clicks++;
        markDirty();
    }

}