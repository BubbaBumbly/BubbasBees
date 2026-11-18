package net.bubba.bubbasbees.registries;

import com.mojang.serialization.Codec;
import net.bubba.bubbasbees.BubbasBees;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/** Item data components: the bee stored in a bee jar, and the honeycombs carried by a picked-up advanced hive. */
public class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(BubbasBees.MOD_ID);

    // Full entity NBT of the captured bee plus an "EntityType" string, kept as CustomData
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> BEE_JAR_CONTENTS =
            COMPONENTS.registerComponentType("bee_jar_contents",
                    builder -> builder.persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ItemStack>>> BEEHIVE_COMBS =
            COMPONENTS.registerComponentType("beehive_combs",
                    builder -> builder.persistent(ItemStack.CODEC.listOf())
                            .networkSynchronized(ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list())));

    /** True on a picked-up hive that was in busy mode; absent means calm. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> HIVE_BUSY =
            COMPONENTS.registerComponentType("hive_busy",
                    builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}
