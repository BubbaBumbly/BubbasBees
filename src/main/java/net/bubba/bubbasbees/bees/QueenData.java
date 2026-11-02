package net.bubba.bubbasbees.bees;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Where the royal queen is (one per world), so hives can check her distance once when a bee enters instead of
 * searching for her (finish-spec.md "no-lag build rule"). Kept in the Overworld's data storage.
 * Position = her hive while she is inside one; unknown (null) while she sits in a bee jar.
 */
public class QueenData extends SavedData {
    private static final String NAME = "bubbasbees_queen";
    public static final int BOOST_RANGE = 16;

    private boolean alive;
    private @Nullable UUID queen;
    private @Nullable ResourceKey<Level> dimension;
    private @Nullable BlockPos pos;

    public static @Nullable QueenData get(@Nullable MinecraftServer server) {
        if (server == null) return null;
        return server.overworld().getDataStorage().computeIfAbsent(QueenData::load, QueenData::new, NAME);
    }

    public static @Nullable QueenData get(Level level) {
        return level.isClientSide ? null : get(level.getServer());
    }

    private static QueenData load(CompoundTag tag) {
        QueenData data = new QueenData();
        data.alive = tag.getBoolean("Alive");
        if (tag.hasUUID("Queen")) data.queen = tag.getUUID("Queen");
        if (tag.contains("Dimension")) {
            data.dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(tag.getString("Dimension")));
        }
        if (tag.contains("Pos")) data.pos = NbtUtils.readBlockPos(tag.getCompound("Pos"));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Alive", alive);
        if (queen != null) tag.putUUID("Queen", queen);
        if (dimension != null) tag.putString("Dimension", dimension.location().toString());
        if (pos != null) tag.put("Pos", NbtUtils.writeBlockPos(pos));
        return tag;
    }

    public boolean isAlive() {
        return alive;
    }

    /** Called by a living queen every second, and by a hive when she goes in. */
    public void setPosition(UUID uuid, ResourceKey<Level> dim, BlockPos at) {
        if (alive && uuid.equals(queen) && dim.equals(dimension) && at.equals(pos)) return;
        alive = true;
        queen = uuid;
        dimension = dim;
        pos = at.immutable();
        setDirty();
    }

    /** She left the world without dying (put in a bee jar, or into a hive, which sets her position right after). */
    public void clearPosition(UUID uuid) {
        if (!uuid.equals(queen) || pos == null) return;
        pos = null;
        setDirty();
    }

    public void died(UUID uuid) {
        if (queen != null && !uuid.equals(queen)) return;
        alive = false;
        pos = null;
        dimension = null;
        setDirty();
    }

    /** Whether a hive at hivePos is within her boost range. */
    public boolean boosts(Level level, BlockPos hivePos) {
        return alive && pos != null && level.dimension().equals(dimension)
                && pos.distSqr(hivePos) <= (double) BOOST_RANGE * BOOST_RANGE;
    }
}
