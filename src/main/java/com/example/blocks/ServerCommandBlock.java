package com.example.blocks;

import com.nimbusds.openid.connect.sdk.claims.Address;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;


public class ServerCommandBlock extends Block {

    public final int PORT = 3000;
    public ServerCommandBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    private void SendHTTPResponse(PrintWriter out) {
        String html = "<html> It works! </html>";
        final String http =
                "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + html.length() + "\r\n" +
                "Content-Type: text/html\r\n" +
                "\r\n" +
                html;
        out.println(http);
    }

    private void StartServer(PlayerEntity player) {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(PORT)){
                System.out.println("Server listening on port " + PORT + "...");
                while (true) {
                    try (Socket client = server.accept()) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                        String message = in.readLine();
                        System.out.println("Received: " + message);

                        String getRequest = "GET / HTTP/1.1";
                        if (message.startsWith(getRequest)) {
                            SendHTTPResponse(out);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                player.sendMessage(net.minecraft.text.Text.literal("Server Error!"), true);
            }
        }).start();
    }

    private boolean IsValidServer(World world, BlockPos pos) {
        BlockPos commandPos = pos;

        System.out.println(commandPos.getX() + " " + commandPos.getY() + " " + commandPos.getZ());
        System.out.println(Registries.BLOCK.getId(world.getBlockState(commandPos).getBlock()));

        if (world.getBlockState(commandPos).getBlock() instanceof ServerCommandBlock) {
            System.out.println("Server Block is valid!");
            BlockPos newpos = pos.down();
            Block nextBlock = world.getBlockState(newpos).getBlock();
            Identifier id = Registries.BLOCK.getId(nextBlock);
            System.out.println(id);
        }

        return true;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            if (IsValidServer(world, pos)) {
                StartServer(player);
                player.sendMessage(net.minecraft.text.Text.literal("Server Created."), true);
            } else {
                player.sendMessage(net.minecraft.text.Text.literal("Invalid Server."), true);
            }
        }
        return ActionResult.SUCCESS;
    }
}