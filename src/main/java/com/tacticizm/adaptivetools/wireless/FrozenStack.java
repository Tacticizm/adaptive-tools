package com.tacticizm.adaptivetools.wireless;

import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** Immutable item snapshot safe to store as a data component. */
public record FrozenStack(ItemStack stack) {
    public FrozenStack {
        stack = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    public static final Codec<FrozenStack> CODEC = ItemStack.CODEC.xmap(FrozenStack::new, FrozenStack::stack);
    public static final StreamCodec<RegistryFriendlyByteBuf, FrozenStack> STREAM_CODEC = ItemStack.STREAM_CODEC.map(FrozenStack::new, FrozenStack::stack);

    public ItemStack copy() {
        return stack.copy();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FrozenStack frozen && ItemStack.matches(stack, frozen.stack);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(stack);
    }
}
