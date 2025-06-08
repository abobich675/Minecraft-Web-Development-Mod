package com.example.blocks;

import com.mojang.serialization.MapCodec;
import com.nimbusds.openid.connect.sdk.claims.Address;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;


public class ServerCommandBlock extends BlockWithEntity {

    public ServerCommandBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(ServerCommandBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ServerCommandBlockEntity(pos, state);
    }

    public final int PORT = 3000;

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof ServerCommandBlockEntity serverBlockEntity) {
                int status = serverBlockEntity.tryStartServer(player, world, pos, PORT);
                if  (status == 1) {
                    player.sendMessage(net.minecraft.text.Text.literal("Server Created."), true);
                } else {
                    player.sendMessage(net.minecraft.text.Text.literal("Invalid Server."), true);
                }
            }
        }
        return ActionResult.SUCCESS;
    }
}