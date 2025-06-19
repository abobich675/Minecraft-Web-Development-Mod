package com.example;

import com.example.Main;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Site {

    public Site(PlayerEntity player, World world, BlockPos pos) {
        this.player = player;
        this.world = world;
        this.pos = pos;

        // Get style block
        Block block = world.getBlockState(pos).getBlock();
        this.style_block = Registries.BLOCK.getId(block);

        // Get outline block
        block = world.getBlockState(pos.down()).getBlock();
        this.outline_block = Registries.BLOCK.getId(block);
    }

    PlayerEntity player;
    World world;
    BlockPos pos;

    Identifier outline_block = null;
    Identifier style_block = null;
    Dictionary<Block, String> styles = new Hashtable<>();

    ServerSocket server = null;
    Thread serverThread = null;
    public int port = 0;
    BlockPos pos1 = null;
    BlockPos pos2 = null;

    private boolean CompareBlockID(Block block, Identifier id) {
        Identifier blockId = Registries.BLOCK.getId(block);
        return blockId.equals(id);
    }

    // Read text from a block at specified coordinates. Return "" if no text is found
    private String ReadBlock(BlockPos pos) {
        String text = "";
        Block block = world.getBlockState(pos).getBlock();
        //  Sign
        if (block instanceof SignBlock) {
            ServerWorld serverWorld = (ServerWorld) world; // cast only if you're sure it's server side
            final BlockPos finalPos = pos;
            CompletableFuture<String> future = new CompletableFuture<>();
            // Have the server deal with BlockEntities
            serverWorld.getServer().execute(() -> {
                String signHtml = "";
                BlockEntity be = world.getBlockEntity(finalPos);
                System.out.println(be);
                if (be instanceof SignBlockEntity signEntity) {
                    Text[] frontText = signEntity.getFrontText().getMessages(false);
                    Text[] backText = signEntity.getBackText().getMessages(false);

                    for (Text currText : frontText) {
                        System.out.println(currText.getString());
                        signHtml += currText.getString();
                    }
                    for (Text currText : backText) {
                        System.out.println(currText.getString());
                        signHtml += currText.getString();
                    }
                }
                future.complete(signHtml);
            });
            try {
                String signHtml = future.get();
                text += signHtml;
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Lectern
        } else if (block instanceof LecternBlock) {
            ServerWorld serverWorld = (ServerWorld) world; // cast only if you're sure it's server side
            final BlockPos finalPos = pos;
            CompletableFuture<String> future = new CompletableFuture<>();
            // Have the server deal with BlockEntities
            serverWorld.getServer().execute(() -> {
                BlockEntity be = world.getBlockEntity(finalPos);
                System.out.println(be);
                if (be instanceof LecternBlockEntity lecternEntity) {
                    if (!lecternEntity.hasBook()) {
                        future.complete("");
                        return;
                    }
                    ItemStack book = lecternEntity.getBook();
                    if (book.getItem() != Items.WRITABLE_BOOK && book.getItem() != Items.WRITTEN_BOOK) {
                        future.complete("");
                        return;
                    }

                    if (!book.contains(DataComponentTypes.WRITABLE_BOOK_CONTENT) && !book.contains(DataComponentTypes.WRITTEN_BOOK_CONTENT)) {
                        future.complete("");
                        return;
                    }

                    StringBuilder bookText = new StringBuilder();

                    if (book.contains(DataComponentTypes.WRITTEN_BOOK_CONTENT)) {
                        WrittenBookContentComponent content = book.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
                        if (content != null) {
                            for (int i = 0; i < content.pages().size(); i++) {
                                RawFilteredPair<Text> page = content.pages().get(i);
                                bookText.append(page.raw().getString());
                                if (i < content.pages().size() - 1) {
                                    bookText.append("\n");
                                }
                            }
                        }
                    }
                    else if (book.contains(DataComponentTypes.WRITABLE_BOOK_CONTENT)) {
                        WritableBookContentComponent content = book.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);
                        if (content != null) {
                            for (int i = 0; i < content.pages().size(); i++) {
                                RawFilteredPair<String> page = content.pages().get(i);
                                bookText.append(page.raw());
                                if (i < content.pages().size() - 1) {
                                    bookText.append("\n");
                                }
                            }
                        }
                    }
                    future.complete(bookText.toString());
                }
                future.complete("");
            });
            try {
                String bookHtml = future.get();

                text += bookHtml;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return text;
    }

    private String ReadStack(int x, int y, int z) {
        Stack<String> stack = new Stack<>();
        return ReadStack(x, y, z, stack);
    }

    private String ReadStack(int x, int y, int z, Stack<String> tagStack) {
        return ReadStack(x, y, z, tagStack, null);
    }

    private String ReadStack(int x, int y, int z, Stack<String> tagStack, Block currStyle) {
        BlockPos pos = new BlockPos(x, y, z);
        Block block = world.getBlockState(pos).getBlock();
        String html = "";

        Identifier blockId = Registries.BLOCK.getId(block);
        boolean isAir = blockId.toString().equals("minecraft:air");
        if (pos.getY() >= world.getHeight() || isAir) {
            while (!tagStack.isEmpty()) {
                html += tagStack.pop();
            }
            return html;
        }

        Identifier id = Registries.BLOCK.getId(block);
        System.out.println(id);

        if (CompareBlockID(block, style_block)) {
            // Define which style to edit
            if (currStyle == null) {
                currStyle = world.getBlockState(pos.up()).getBlock();
                styles.remove(currStyle);
                return ReadStack(x, y + 2, z, tagStack, currStyle);
            }

            return ReadStack(x, y + 1, z, tagStack, null);
        }

        if (currStyle != null) {

            // Update style
            String text = ReadBlock(pos);
            System.out.println("text: " + text);
            if (text.isEmpty()) {
                text = styles.get(block);
            }
            System.out.println("text after checking block: " + text);

            if (text == null) {
                text = "";
            }

            String curr = styles.get(currStyle);
            if (curr == null) {
                curr = "";
            }
            styles.put(currStyle, curr + text);

            // Move on
            return ReadStack(x, y + 1, z, tagStack, currStyle);
        }

        if (CompareBlockID(block, outline_block)) {
            if (tagStack.isEmpty())
                return ReadStack(x, y + 1, z, tagStack);

            html += tagStack.pop();
            html += ReadStack(x, y + 1, z, tagStack);
            return html;
        }

        String blockText = ReadBlock(pos);
        blockText = blockText.replace("<", "&lt;");
        blockText = blockText.replace(">", "&gt;");

        if (blockText.isEmpty()) {
            String blockStyle =  styles.get(block);
            boolean isTransparent = !block.getDefaultState().isOpaque();
            if (isTransparent) {
                if (blockStyle == null)
                    html += "\r\n<span>";
                else
                    html += "\r\n<span style='" + blockStyle + "'>";
                tagStack.push("\r\n</span>");
            } else {
                if (blockStyle == null)
                    html += "\r\n<div>";
                else
                    html += "\r\n<div style='" + blockStyle + "'>";
                tagStack.push("\r\n</div>");
            }
        } else {
            html += blockText;
        }
        html += ReadStack(x, y + 1, z, tagStack);
        return html;
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

    public String GetHTML() {

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

        return "<html>" + html + "<title>" + this.style_block + " - Minecraft Web Dev Mod</title></html>";
    }

    // TODO: REMOVE
//    private void StartServer(PlayerEntity player) {
//        serverThread = new Thread(() -> {
//            try (ServerSocket s = new ServerSocket(port)){
//                server = s;
//                System.out.println("Server listening on port " + port + "...");
//                while (!Thread.currentThread().isInterrupted()) {
//                    try (Socket client = s.accept()) {
//                        if (Thread.currentThread().isInterrupted()) {
//                            System.out.println("Is Interrupted!");
//                            break;
//                        }
//
//                        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
//                        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
//                        String message = in.readLine();
//                        System.out.println("Received: " + message);
//
//                        if (message.startsWith("GET / HTTP/1.1")) {
//                            SendHTTPResponse(out);
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        break;
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                player.sendMessage(net.minecraft.text.Text.literal("Server Error!"), true);
//            } finally {
//                try {
//                    if (server != null && !server.isClosed()) {
//                        player.sendMessage(net.minecraft.text.Text.literal("Server Closing."), true);
//                        server.close();
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        serverThread.start();
//    }

    public boolean IsValidServer(BlockPos pos) {
        int upperX = pos.getX();
        int lowerX = upperX;
        int lowerY = pos.getY() - 1;
        int upperZ = pos.getZ();
        int lowerZ = upperZ;

        // Check that style block and outline block are different
        if (outline_block == style_block) {
            System.out.println("Outline and Style cannot be the same: " + outline_block);
            return false;
        }

        // Check blocks around Command Block
        pos1 = pos.down();
        Block pos1Block = world.getBlockState(pos1).getBlock();
        if (!CompareBlockID(pos1Block, outline_block)) {
            System.out.println("Block below was invalid: " + pos1Block);
            return false;
        }

        boolean n = CompareBlockID(world.getBlockState(pos1.north()).getBlock(), outline_block);
        boolean s = CompareBlockID(world.getBlockState(pos1.south()).getBlock(), outline_block);
        boolean e = CompareBlockID(world.getBlockState(pos1.east()).getBlock(), outline_block);
        boolean w = CompareBlockID(world.getBlockState(pos1.west()).getBlock(), outline_block);

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
        while(CompareBlockID(world.getBlockState(curr.east()).getBlock(), outline_block)) {
            curr = curr.east();
            upperX = curr.getX();

            if (upperX - lowerX > Main.MAX_SIZE)
                return false;
        }

        // West
        curr = pos1;
        while(CompareBlockID(world.getBlockState(curr.west()).getBlock(), outline_block)) {
            curr = curr.west();
            lowerX = curr.getX();

            if (upperX - lowerX > Main.MAX_SIZE)
                return false;
        }

        // South
        curr = pos1;
        while(CompareBlockID(world.getBlockState(curr.south()).getBlock(), outline_block)) {
            curr = curr.south();
            upperZ = curr.getZ();

            if (upperZ - lowerZ > Main.MAX_SIZE)
                return false;
        }

        // North
        curr = pos1;
        while(CompareBlockID(world.getBlockState(curr.north()).getBlock(), outline_block)) {
            curr = curr.north();
            lowerZ = curr.getZ();

            if (upperZ - lowerZ > Main.MAX_SIZE)
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
                if (!CompareBlockID(block, outline_block)) {
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
}
