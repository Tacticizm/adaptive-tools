package com.tacticizm.adaptivetools;

import java.util.function.UnaryOperator;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import com.tacticizm.adaptivetools.wireless.FrozenStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AdaptiveTools.MOD_ID);

    /** FE stored in a Flux or Ender tool. Absent means empty. Capacity comes from the tier, not this value. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = COMPONENTS.register("energy",
            () -> component(builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolSettings>> TOOL_SETTINGS = COMPONENTS.register("tool_settings",
            () -> component(builder -> builder.persistent(ToolSettings.CODEC).networkSynchronized(ToolSettings.STREAM_CODEC)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MOMENTUM_STACKS = COMPONENTS.register("momentum_stacks",
            () -> component(builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> MOMENTUM_TIME = COMPONENTS.register("momentum_time",
            () -> component(builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> GUARD_READY = COMPONENTS.register("guard_ready",
            () -> component(builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> SIPHON_READY = COMPONENTS.register("siphon_ready",
            () -> component(builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG)));

    /** Channel id on a carried receiver. Absent means Unassigned. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CHANNEL = COMPONENTS.register("channel",
            () -> component(builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8)));

    /** Fuel stack inside a field generator. Absent means the slot is empty. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FrozenStack>> FIELD_FUEL = COMPONENTS.register("field_fuel",
            () -> component(builder -> builder.persistent(FrozenStack.CODEC).networkSynchronized(FrozenStack.STREAM_CODEC)));

    /** Item sitting in a field generator's charge slot. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FrozenStack>> FIELD_CHARGE = COMPONENTS.register("field_charge",
            () -> component(builder -> builder.persistent(FrozenStack.CODEC).networkSynchronized(FrozenStack.STREAM_CODEC)));

    /** Furnace burn ticks left on a field generator. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FIELD_BURN = COMPONENTS.register("field_burn",
            () -> component(builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT)));

    private ModComponents() {}

    private static <T> DataComponentType<T> component(UnaryOperator<DataComponentType.Builder<T>> operator) {
        return operator.apply(DataComponentType.builder()).build();
    }
}
