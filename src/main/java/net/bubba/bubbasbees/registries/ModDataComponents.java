package net.bubba.bubbasbees.registries;

import com.mojang.serialization.Codec;
import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.util.Reg;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** Item data components: the bee stored in a bee jar, and the honeycombs carried by a picked-up advanced hive. */
public class ModDataComponents {
    private static ResourceLocation rl(String name) {
        return ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, name);
    }

    // Full entity NBT of the captured bee plus an "EntityType" string, kept as CustomData
    public static final Reg<DataComponentType<CustomData>> BEE_JAR_CONTENTS = Reg.of(rl("bee_jar_contents"));
    public static final Reg<DataComponentType<List<ItemStack>>> BEEHIVE_COMBS = Reg.of(rl("beehive_combs"));
    /** True on a picked-up hive that was in busy mode; absent means calm. */
    public static final Reg<DataComponentType<Boolean>> HIVE_BUSY = Reg.of(rl("hive_busy"));

    public static void register() {
        BEE_JAR_CONTENTS.set(Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, BEE_JAR_CONTENTS.id(),
                DataComponentType.<CustomData>builder()
                        .persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC).build()));
        BEEHIVE_COMBS.set(Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, BEEHIVE_COMBS.id(),
                DataComponentType.<List<ItemStack>>builder()
                        .persistent(ItemStack.CODEC.listOf())
                        .networkSynchronized(ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list())).build()));
        HIVE_BUSY.set(Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, HIVE_BUSY.id(),
                DataComponentType.<Boolean>builder()
                        .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build()));
    }
}
