package com.example.blocks;

import com.example.Main;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.beans.Expression;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class ServerCommandBlockEntity extends BlockEntity {
    public ServerCommandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SERVER_COMMAND_ENTITY, pos, state);
    }

//    private int clicks = 0;
//    public int getClicks() {
//        return clicks;
//    }
//
//    public void incrementClicks() {
//        clicks++;
//        markDirty();
//    }

    final int MAX_SIZE = 32;
    public int port = 3000;
    public BlockPos pos1 = null;
    public BlockPos pos2 = null;
    World world = null;

    private String ReadStack(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        Block block = world.getBlockState(pos).getBlock();

        final StringBuilder html = new StringBuilder();

        while (pos.getY() < world.getHeight() && !CompareBlockID(block, "minecraft", "air")) {
            Identifier id = Registries.BLOCK.getId(block);
            System.out.println(id);

            ServerWorld serverWorld = (ServerWorld) world; // cast only if you're sure it's server side
            final BlockPos finalPos = pos;
            // Have the server deal with BlockEntities
            serverWorld.getServer().execute(() -> {
                        BlockEntity be = world.getBlockEntity(finalPos);
                        System.out.println(be);
                        if (be instanceof SignBlockEntity signEntity) {
                            Text[] frontText = signEntity.getFrontText().getMessages(false);
                            Text[] backText = signEntity.getBackText().getMessages(false);

                            for (Text text : frontText) {
                                System.out.println(text.getString());
                                html.append(text.getString());
                            }
                            for (Text text : backText) {
                                System.out.println(text.getString());
                                html.append(text.getString());
                            }
                        }
                    });

            pos = pos.up();
            block = world.getBlockState(pos).getBlock();
        }

        return html.toString();
    }

    private String ParseXZ(int xDir, int zDir) {
        String html = "";
        for (int x = pos1.getX(); (xDir == 1) ? (x <= pos2.getX() ) : (x >= pos2.getX() ); x += xDir) {
            for (int z = pos1.getZ(); (zDir == 1) ? (z <= pos2.getZ() ) : (z >= pos2.getZ() ); z += zDir) {
                html += ReadStack(x, pos1.getY() + 1, z);
            }
        }

        return html;
    }

    private String ParseZX(int xDir, int zDir) {
        String html = "";
        for (int z = pos1.getZ(); (zDir == 1) ? (z <= pos2.getZ() ) : (z >= pos2.getZ() ); z += zDir) {
            for (int x = pos1.getX(); (xDir == 1) ? (x <= pos2.getX() ) : (x >= pos2.getX() ); x += xDir) {
                html += ReadStack(x, pos1.getY() + 1, z);
            }
        }
        return html;
    }

    private String GetHTML() {

        int xDir = (pos1.getX() > pos2.getX()) ? -1 : 1;
        int zDir = (pos1.getZ() > pos2.getZ()) ? -1 : 1;

        // -X, +Z -> Z first, then X
        // -X, -Z -> X first, then Z
        // +X, -Z -> Z first, then X
        // +X, +Z -> X first, then Z

        String html = "";

        boolean xFirst = (xDir == zDir);
        if  (xFirst)
            html = ParseZX(xDir, zDir);
        else
            html = ParseXZ(xDir, zDir);

        return "<html>" + html + "</html>";
    }

    private void SendHTTPResponse(PrintWriter out) {
        String html = GetHTML();
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
            try (ServerSocket server = new ServerSocket(port)){
                System.out.println("Server listening on port " + port + "...");
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

    private boolean CompareBlockID(Block block, String name) {
        Identifier id = Registries.BLOCK.getId(block);
        return id.toString().equals(Main.MOD_ID + ":" + name);
    }

    private boolean CompareBlockID(Block block, String modId, String name) {
        Identifier id = Registries.BLOCK.getId(block);
        return id.toString().equals(modId + ":" + name);
    }

    private boolean IsValidServer(BlockPos pos) {
        int upperX = pos.getX();
        int lowerX = upperX;
        int lowerY = pos.getY() - 1;
        int upperZ = pos.getZ();
        int lowerZ = upperZ;

        // Check blocks around Command Block
        pos1 = pos.down();
        Block pos1Block = world.getBlockState(pos1).getBlock();
        if (!CompareBlockID(pos1Block, "server_outline")) {
            return false;
        }

        boolean n = CompareBlockID(world.getBlockState(pos1.north()).getBlock(), "server_outline");
        boolean s = CompareBlockID(world.getBlockState(pos1.south()).getBlock(), "server_outline");
        boolean e = CompareBlockID(world.getBlockState(pos1.east()).getBlock(), "server_outline");
        boolean w = CompareBlockID(world.getBlockState(pos1.west()).getBlock(), "server_outline");

        if (!(n || s || e || w)) {
            System.out.println("No structure found");
            return false;
        }

        if ((n && s) || (e && w)) {
            System.out.println("Server Command not at corner");
            return false;
        }

        // Find edges of structure
        // East
        BlockPos curr = pos1;
        while(CompareBlockID(world.getBlockState(curr.east()).getBlock(), "server_outline")) {
            curr = curr.east();
            upperX = curr.getX();

            if (upperX - lowerX > MAX_SIZE)
                return false;
        }

        // West
        curr = pos1;
        while(CompareBlockID(world.getBlockState(curr.west()).getBlock(), "server_outline")) {
            curr = curr.west();
            lowerX = curr.getX();

            if (upperX - lowerX > MAX_SIZE)
                return false;
        }

        // South
        curr = pos1;
        while(CompareBlockID(world.getBlockState(curr.south()).getBlock(), "server_outline")) {
            curr = curr.south();
            upperZ = curr.getZ();

            if (upperZ - lowerZ > MAX_SIZE)
                return false;
        }

        // North
        curr = pos1;
        while(CompareBlockID(world.getBlockState(curr.north()).getBlock(), "server_outline")) {
            curr = curr.north();
            lowerZ = curr.getZ();

            if (upperZ - lowerZ > MAX_SIZE)
                return false;
        }

        System.out.println("UpperX: " + upperX);
        System.out.println("LowerX: " + lowerX);
        System.out.println("UpperZ: " + upperZ);
        System.out.println("LowerZ: " + lowerZ);

        // Ensure Structure is Filled
        for (int x = lowerX; x <= upperX; x++) {
            for (int z = lowerZ; z <= upperZ; z++) {
                Block block = world.getBlockState(new BlockPos(x, lowerY, z)).getBlock();
                if (!CompareBlockID(block, "server_outline")) {
                    System.out.println("Bad block at: (" + x + ", " + lowerY + ", " + z + ")" );
                    return false;
                }
            }
        }

        int x = 0;
        int z = 0;

        if (lowerX == pos1.getX())
            x = upperX;
        else
            x = lowerX;

        if (lowerZ == pos1.getZ())
            z = upperZ;
        else
            z = lowerZ;

        pos2 = new BlockPos(x, lowerY, z);

        return true;
    }

    public int tryStartServer(PlayerEntity player, World world, BlockPos pos, int port) {
        this.world = world;
        if (IsValidServer(pos)) {
            this.port = port;
            StartServer(player);
            return 1;
        }
        else
            return 0;
    }

}