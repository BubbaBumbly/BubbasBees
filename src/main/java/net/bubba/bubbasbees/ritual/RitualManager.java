package net.bubba.bubbasbees.ritual;

import com.mojang.math.Vector3f;
import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.bees.QueenData;
import net.bubba.bubbasbees.blockentities.PedestalBlockEntity;
import net.bubba.bubbasbees.blocks.custom.PedestalBlock;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.entity.custom.DarkBee;
import net.bubba.bubbasbees.entity.custom.ModBee;
import net.bubba.bubbasbees.registries.ModEntityTypes;
import net.bubba.bubbasbees.registries.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Runs the two rituals at the Celestial Altar (finish-spec.md "Rituals").
 * Start: night ritual = Bubba Star on the altar, structure complete, every candle lit, at night under open sky (the
 * last of these to happen starts it); queen ritual = golden helmet on the altar in daytime with the structure complete
 * and no living queen. Pollinations count when a bee finishes a flower inside the circle, its hive is inside the
 * circle, and no candle is out. Hives inside the circle send their bees out at night and make no combs while it runs.
 * Fails on: structure broken or item taken (F1), altar unloaded or no player near (F2), a bee from a hive inside the
 * circle killed (F3), sunrise (night) or 5 minutes up (queen). The altar's item is never consumed.
 */
@Mod.EventBusSubscriber(modid = BubbasBees.MOD_ID)
public final class RitualManager {
    private RitualManager() {}

    private static final Map<ResourceKey<Level>, Map<BlockPos, Ritual>> ACTIVE = new HashMap<>();
    /** Candles lit by a player: the altars near them are checked a couple of ticks later. */
    private static final List<PendingCheck> CANDLE_CHECKS = new ArrayList<>();
    private record PendingCheck(ResourceKey<Level> dimension, BlockPos candle, long at, Player player) {}

    private static final int PLAYER_RANGE = 64;
    private static final int BAR_RANGE = 48;
    private static final int RESULT_TICKS = 100;
    private static final int DARK_BEE_MIN = 600, DARK_BEE_MAX = 1200;   // every 30-60 s
    private static final int LAST_MINUTE = 1200;
    private static final long SUNRISE = 23000;                         // isNight() turns false about here
    private static final DustParticleOptions EDGE = new DustParticleOptions(new Vector3f(1.0F, 0.75F, 0.2F), 1.0F);

    // ---------------------------------------------------------------- queries from bees and hives

    /** Whether a ritual is running whose circle holds this hive (its bees go out at night; it makes no combs). */
    public static boolean isRitualHive(@Nullable Level level, @Nullable BlockPos hive) {
        if (level == null || level.isClientSide || hive == null) return false;
        Map<BlockPos, Ritual> rituals = ACTIVE.get(level.dimension());
        if (rituals == null) return false;
        for (Ritual ritual : rituals.values()) {
            if (ritual.isRunning() && RitualStructure.inArea(ritual.altar, hive, ritual.kind == Ritual.Kind.NIGHT)) return true;
        }
        return false;
    }

    /** A bee finished a flower visit. */
    public static void onPollinated(BubbasBee bee, @Nullable BlockPos flower) {
        if (flower == null || !(bee.level instanceof ServerLevel level)) return;
        Map<BlockPos, Ritual> rituals = ACTIVE.get(level.dimension());
        if (rituals == null) return;
        BlockPos hive = bee.getHivePos();
        for (Ritual ritual : rituals.values()) {
            if (!ritual.isRunning() || ritual.paused || hive == null) continue;
            boolean night = ritual.kind == Ritual.Kind.NIGHT;
            if (!RitualStructure.inArea(ritual.altar, flower, night) || !RitualStructure.inArea(ritual.altar, hive, night)) continue;
            ritual.total++;
            String type = bee.getVariant();
            if (ritual.kind == Ritual.Kind.NIGHT ? "starlight".equals(type) : "royal_guard".equals(type)) ritual.special++;
            ritual.types.add(type);
            if (ritual.isWon()) win(level, ritual);
            else ritual.refreshBar(ticksLeft(level, ritual));
            return;
        }
    }

    // ---------------------------------------------------------------- starting

    /** Called when an item goes on an altar or a candle near one is lit; tells the player what's missing, if anything. */
    public static void tryStart(ServerLevel level, BlockPos altar, @Nullable Player player) {
        Map<BlockPos, Ritual> rituals = ACTIVE.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        Ritual old = rituals.get(altar);
        if (old != null && old.isRunning()) return;
        if (!(level.getBlockEntity(altar) instanceof PedestalBlockEntity pedestal)) return;
        ItemStack item = pedestal.getItem();
        Ritual.Kind kind = item.is(ModItems.BUBBA_STAR.get()) ? Ritual.Kind.NIGHT
                : item.is(Items.GOLDEN_HELMET) ? Ritual.Kind.QUEEN : null;
        if (kind == null) return;
        List<BlockPos> candles = new ArrayList<>();
        Component problem = startProblem(level, altar, kind, candles);
        if (problem != null) {
            if (player != null) player.displayClientMessage(problem.copy().withStyle(ChatFormatting.YELLOW), true);
            if (kind == Ritual.Kind.QUEEN) showEdge(level, altar);
            return;
        }
        if (old != null) {   // the last one's result is still showing
            old.bar.removeAllPlayers();
            rituals.remove(altar);
        }
        long now = level.getGameTime();
        Ritual ritual = new Ritual(kind, altar, now);
        ritual.candles.addAll(candles);
        ritual.nextDarkBee = now + DARK_BEE_MIN + level.random.nextInt(DARK_BEE_MAX - DARK_BEE_MIN + 1);
        rituals.put(altar, ritual);
        pedestal.setRitualActive(true);
        updatePlayers(level, ritual);
        ritual.refreshBar(ticksLeft(level, ritual));
        broadcast(level, ritual, Component.translatable("ritual.bubbasbees.started." + kind.key()).withStyle(kind.colour));
        level.playSound(null, altar, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, kind == Ritual.Kind.NIGHT ? 0.8F : 1.2F);
    }

    private static @Nullable Component startProblem(ServerLevel level, BlockPos altar, Ritual.Kind kind, List<BlockPos> candles) {
        if (kind == Ritual.Kind.NIGHT) {
            if (!level.isNight()) return Component.translatable("ritual.bubbasbees.problem.night");
            if (!level.canSeeSky(altar.above())) return Component.translatable("ritual.bubbasbees.problem.sky");
            Component structure = RitualStructure.checkNight(level, altar, candles);
            if (structure != null) return structure;
            for (BlockPos candle : candles) {
                if (!RitualStructure.isLitCandle(level.getBlockState(candle))) {
                    return Component.translatable("ritual.bubbasbees.problem.candles_lit");
                }
            }
            return null;
        }
        if (level.isNight()) return Component.translatable("ritual.bubbasbees.problem.day");
        QueenData queen = QueenData.get(level);
        if (queen != null && queen.isAlive()) return Component.translatable("ritual.bubbasbees.problem.queen_alive");
        return RitualStructure.checkQueen(level, altar);
    }

    // ---------------------------------------------------------------- running

    private static long ticksLeft(ServerLevel level, Ritual ritual) {
        if (ritual.kind == Ritual.Kind.QUEEN) return ritual.startTime + Ritual.QUEEN_TICKS - level.getGameTime();
        long time = level.getDayTime() % 24000L;
        return time < 12000L ? 0 : SUNRISE - time;
    }

    private static void tickLevel(ServerLevel level) {
        long now = level.getGameTime();
        if (!CANDLE_CHECKS.isEmpty()) {
            Iterator<PendingCheck> it = CANDLE_CHECKS.iterator();
            while (it.hasNext()) {
                PendingCheck check = it.next();
                if (!check.dimension().equals(level.dimension()) || now < check.at()) continue;
                it.remove();
                if (RitualStructure.isLitCandle(level.getBlockState(check.candle()))) startNear(level, check.candle(), check.player());
            }
        }
        Map<BlockPos, Ritual> rituals = ACTIVE.get(level.dimension());
        if (rituals == null || rituals.isEmpty()) return;
        Iterator<Ritual> it = rituals.values().iterator();
        while (it.hasNext()) {
            Ritual ritual = it.next();
            if (!ritual.isRunning()) {
                if (now >= ritual.removeAt) {
                    ritual.bar.removeAllPlayers();
                    it.remove();
                }
                continue;
            }
            long age = now - ritual.startTime;
            if (age % 10 == 0) updatePause(level, ritual);
            if (age % 20 == 0) everySecond(level, ritual);
            if (ritual.isRunning() && ritual.kind == Ritual.Kind.NIGHT && !ritual.paused && now >= ritual.nextDarkBee) {
                spawnDarkBees(level, ritual);
                ritual.nextDarkBee = now + DARK_BEE_MIN + level.random.nextInt(DARK_BEE_MAX - DARK_BEE_MIN + 1);
            }
        }
    }

    /** Any counted candle out (a dark bee's work) pauses the ritual until it is relit. */
    private static void updatePause(ServerLevel level, Ritual ritual) {
        if (ritual.kind != Ritual.Kind.NIGHT) return;
        boolean paused = false;
        for (BlockPos candle : ritual.candles) {
            BlockState state = level.getBlockState(candle);
            if (state.getBlock() instanceof AbstractCandleBlock && !state.getValue(AbstractCandleBlock.LIT)) {
                paused = true;
                break;
            }
        }
        if (paused != ritual.paused) {
            ritual.paused = paused;
            // The 30-60 s between dark bees counts only unpaused time: no new one the moment a candle is relit
            if (!paused) {
                ritual.nextDarkBee = level.getGameTime() + DARK_BEE_MIN + level.random.nextInt(DARK_BEE_MAX - DARK_BEE_MIN + 1);
            }
            ritual.refreshBar(ticksLeft(level, ritual));
        }
    }

    private static void everySecond(ServerLevel level, Ritual ritual) {
        BlockPos altar = ritual.altar;
        if (!level.isLoaded(altar) || !anyPlayerNear(level, altar, PLAYER_RANGE)) {
            fail(level, ritual, "unloaded");
            return;
        }
        if (!(level.getBlockEntity(altar) instanceof PedestalBlockEntity pedestal)
                || !(level.getBlockState(altar).getBlock() instanceof PedestalBlock block) || !block.isAltar()
                || !pedestal.getItem().is(ritual.kind == Ritual.Kind.NIGHT ? ModItems.BUBBA_STAR.get() : Items.GOLDEN_HELMET)) {
            fail(level, ritual, "item");
            return;
        }
        Component broken = ritual.kind == Ritual.Kind.NIGHT
                ? RitualStructure.checkNight(level, altar, ritual.candles) : RitualStructure.checkQueen(level, altar);
        if (broken != null) {
            fail(level, ritual, broken);
            return;
        }
        if (ritual.kind == Ritual.Kind.NIGHT && !level.isNight()) {
            fail(level, ritual, "sunrise");
            return;
        }
        long left = ticksLeft(level, ritual);
        if (ritual.kind == Ritual.Kind.QUEEN && left <= 0) {
            fail(level, ritual, "time");
            return;
        }
        updatePlayers(level, ritual);
        ritual.refreshBar(left);
        // The altar glows while it runs; the queen circle's edge is shown with particles
        level.sendParticles(ParticleTypes.END_ROD, altar.getX() + 0.5, altar.getY() + 1.4, altar.getZ() + 0.5,
                3, 0.3, 0.3, 0.3, 0.01);
        if (ritual.kind == Ritual.Kind.QUEEN) showEdge(level, altar);
    }

    private static void spawnDarkBees(ServerLevel level, Ritual ritual) {
        ritual.darkBees.removeIf(id -> !(level.getEntity(id) instanceof DarkBee bee) || !bee.isAlive());
        int allowed = ticksLeft(level, ritual) <= LAST_MINUTE ? 2 : 1;
        List<BlockPos> lit = new ArrayList<>();
        for (BlockPos candle : ritual.candles) if (RitualStructure.isLitCandle(level.getBlockState(candle))) lit.add(candle);
        while (ritual.darkBees.size() < allowed && !lit.isEmpty()) {
            DarkBee bee = ModEntityTypes.DARK_BEE.get().create(level);
            if (bee == null) return;
            boolean placed = false;
            for (int attempt = 0; attempt < 8 && !placed; attempt++) {   // a random point on the ring, in open air
                double angle = level.random.nextDouble() * Math.PI * 2;
                double x = ritual.altar.getX() + 0.5 + Math.cos(angle) * RitualStructure.NIGHT_RADIUS;
                double z = ritual.altar.getZ() + 0.5 + Math.sin(angle) * RitualStructure.NIGHT_RADIUS;
                bee.moveTo(x, ritual.altar.getY() + 1.5, z, level.random.nextFloat() * 360.0F, 0.0F);
                placed = level.noCollision(bee);
            }
            if (!placed) return;
            bee.setCandle(lit.remove(level.random.nextInt(lit.size())));
            bee.finalizeSpawn(level, level.getCurrentDifficultyAt(bee.blockPosition()), MobSpawnType.EVENT, null, null);
            level.addFreshEntity(bee);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, bee.getX(), bee.getY() + 0.3, bee.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
            ritual.darkBees.add(bee.getUUID());
        }
    }

    // ---------------------------------------------------------------- ending

    private static void win(ServerLevel level, Ritual ritual) {
        BlockPos altar = ritual.altar;
        if (level.getBlockEntity(altar) instanceof PedestalBlockEntity pedestal) {
            pedestal.setRitualActive(false);
            if (ritual.kind == Ritual.Kind.NIGHT) pedestal.setItem(new ItemStack(ModItems.CELESTIAL_BUBBA_STAR.get()));
        }
        if (ritual.kind == Ritual.Kind.QUEEN) {
            ModBee queen = ModEntityTypes.bees().get("royal_queen").get().create(level);
            if (queen != null) {
                queen.moveTo(altar.getX() + 0.5, altar.getY() + 1.6, altar.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(queen);
                QueenData data = QueenData.get(level);
                if (data != null) data.setPosition(queen.getUUID(), level.dimension(), queen.blockPosition());
                // "Long Live the Queen" advancement (minecraft:summoned_entity) for everyone at the ritual
                for (net.minecraft.server.level.ServerPlayer player : level.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class,
                        new net.minecraft.world.phys.AABB(altar).inflate(64.0D))) {
                    net.minecraft.advancements.CriteriaTriggers.SUMMONED_ENTITY.trigger(player, queen);
                }
            }
        }
        level.sendParticles(ParticleTypes.END_ROD, altar.getX() + 0.5, altar.getY() + 1.5, altar.getZ() + 0.5, 60, 0.4, 0.6, 0.4, 0.15);
        level.playSound(null, altar, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        ritual.bar.setName(Component.translatable("ritual.bubbasbees.complete").withStyle(ChatFormatting.GREEN));
        ritual.bar.setColor(BossEvent.BossBarColor.GREEN);
        ritual.bar.setProgress(1.0F);
        broadcast(level, ritual, Component.translatable("ritual.bubbasbees.complete." + ritual.kind.key()).withStyle(ChatFormatting.GREEN));
        end(level, ritual);
    }

    private static void fail(ServerLevel level, Ritual ritual, String reason) {
        fail(level, ritual, Component.translatable("ritual.bubbasbees.fail." + reason));
    }

    private static void fail(ServerLevel level, Ritual ritual, Component reason) {
        if (!ritual.isRunning()) return;
        if (level.getBlockEntity(ritual.altar) instanceof PedestalBlockEntity pedestal) pedestal.setRitualActive(false);
        ritual.bar.setName(Component.translatable("ritual.bubbasbees.failed"));
        ritual.bar.setColor(BossEvent.BossBarColor.RED);
        broadcast(level, ritual, Component.translatable("ritual.bubbasbees.failed_because", reason).withStyle(ChatFormatting.RED));
        level.playSound(null, ritual.altar, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.8F);
        end(level, ritual);
    }

    private static void end(ServerLevel level, Ritual ritual) {
        ritual.removeAt = level.getGameTime() + RESULT_TICKS;
        for (java.util.UUID id : ritual.darkBees) {
            if (level.getEntity(id) instanceof DarkBee bee) bee.poof();
        }
        ritual.darkBees.clear();
    }

    // ---------------------------------------------------------------- helpers

    private static boolean anyPlayerNear(ServerLevel level, BlockPos pos, double range) {
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) <= range * range) return true;
        }
        return false;
    }

    private static void updatePlayers(ServerLevel level, Ritual ritual) {
        double max = (double) BAR_RANGE * BAR_RANGE;
        BlockPos altar = ritual.altar;
        for (ServerPlayer player : List.copyOf(ritual.bar.getPlayers())) {
            if (player.level != level || player.distanceToSqr(altar.getX() + 0.5, altar.getY(), altar.getZ() + 0.5) > max) {
                ritual.bar.removePlayer(player);
            }
        }
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(altar.getX() + 0.5, altar.getY(), altar.getZ() + 0.5) <= max) ritual.bar.addPlayer(player);
        }
    }

    /** To everyone near the altar and everyone who had its bar (so a player who walked off hears why it failed). */
    private static void broadcast(ServerLevel level, Ritual ritual, Component message) {
        BlockPos altar = ritual.altar;
        java.util.Set<ServerPlayer> told = new java.util.HashSet<>(ritual.bar.getPlayers());
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(altar.getX() + 0.5, altar.getY(), altar.getZ() + 0.5) <= (double) BAR_RANGE * BAR_RANGE) {
                told.add(player);
            }
        }
        for (ServerPlayer player : told) player.sendSystemMessage(message);
    }

    /** Gold dust around the radius-6 edge (the queen circle has no dust ring to show it). */
    private static void showEdge(ServerLevel level, BlockPos altar) {
        for (int i = 0; i < 48; i++) {
            double angle = i * Math.PI * 2 / 48;
            level.sendParticles(EDGE, altar.getX() + 0.5 + Math.cos(angle) * RitualStructure.QUEEN_RADIUS, altar.getY() + 0.3,
                    altar.getZ() + 0.5 + Math.sin(angle) * RitualStructure.QUEEN_RADIUS, 1, 0, 0, 0, 0);
        }
    }

    /** A candle was lit: any altar near it holding a ritual item may start. */
    private static void startNear(ServerLevel level, BlockPos candle, Player player) {
        for (BlockPos pos : BlockPos.betweenClosed(candle.offset(-8, -2, -8), candle.offset(8, 2, 8))) {
            if (level.getBlockState(pos).getBlock() instanceof PedestalBlock block && block.isAltar()) {
                tryStart(level, pos.immutable(), player);
            }
        }
    }

    // ---------------------------------------------------------------- events

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) tickLevel(level);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide || !(level.getBlockState(event.getPos()).getBlock() instanceof AbstractCandleBlock)) return;
        CANDLE_CHECKS.add(new PendingCheck(level.dimension(), event.getPos().immutable(), level.getGameTime() + 2, event.getEntity()));
    }

    /** F3: a bee from a hive inside a running circle was killed. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Bee bee) || !(entity.level instanceof ServerLevel level)) return;
        Map<BlockPos, Ritual> rituals = ACTIVE.get(level.dimension());
        if (rituals == null || bee.getHivePos() == null) return;
        for (Ritual ritual : rituals.values()) {
            if (ritual.isRunning() && RitualStructure.inArea(ritual.altar, bee.getHivePos(), ritual.kind == Ritual.Kind.NIGHT)) fail(level, ritual, "bee_killed");
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            Map<BlockPos, Ritual> rituals = ACTIVE.remove(level.dimension());
            if (rituals != null) rituals.values().forEach(r -> r.bar.removeAllPlayers());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
        CANDLE_CHECKS.clear();
    }
}
