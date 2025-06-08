package com.example.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CounterBlock extends BlockWithEntity {
    public CounterBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(CounterBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CounterBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof CounterBlockEntity counterBlockEntity)) {
            return super.onUse(state, world, pos, player, hit);
        }

        counterBlockEntity.incrementClicks();
        player.sendMessage(Text.literal("You've clicked the block for the " + counterBlockEntity.getClicks() + "th time."), true);

        return ActionResult.SUCCESS;
    }

}