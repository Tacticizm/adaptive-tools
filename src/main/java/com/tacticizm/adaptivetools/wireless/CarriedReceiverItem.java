package com.tacticizm.adaptivetools.wireless;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.tacticizm.adaptivetools.ModComponents;

/** Charges FE items in the player's inventory from a power channel. Starts Unassigned. */
public class CarriedReceiverItem extends Item {
    public CarriedReceiverItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static UUID channel(ItemStack stack) {
        String id = stack.get(ModComponents.CHANNEL.get());
        if (id == null || id.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static void assign(ItemStack stack, UUID channel) {
        if (channel == null) {
            stack.remove(ModComponents.CHANNEL.get());
        } else {
            stack.set(ModComponents.CHANNEL.get(), channel.toString());
        }
    }

    public static int assignedCount(MinecraftServer server, UUID channel) {
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            count += count(player, channel);
        }
        return count;
    }

    public static int unassign(MinecraftServer server, UUID channel) {
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.getItem() instanceof CarriedReceiverItem && channel.equals(channel(stack))) {
                    assign(stack, null);
                    count++;
                }
            }
        }
        return count;
    }

    private static int count(ServerPlayer player, UUID channel) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof CarriedReceiverItem && channel.equals(channel(stack))) {
                count++;
            }
        }
        return count;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) {
            WirelessMessages.openChannels(server, hand);
        }
        return InteractionResult.SUCCESS;
    }
}
