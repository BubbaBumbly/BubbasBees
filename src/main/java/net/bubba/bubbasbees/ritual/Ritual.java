package net.bubba.bubbasbees.ritual;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One running ritual at one Celestial Altar: its counts, candles, dark bees and boss bar. Lives only in memory
 * (RitualManager); a server restart, like the players leaving, ends it.
 */
public class Ritual {
    public enum Kind {
        NIGHT(100, 50, 8, ChatFormatting.LIGHT_PURPLE, BossEvent.BossBarColor.PURPLE),
        QUEEN(40, 2, 0, ChatFormatting.GOLD, BossEvent.BossBarColor.YELLOW);

        /** Total pollinations; night: starlight pollinations, queen: royal guard pollinations; night: types. */
        final int total, special, types;
        final ChatFormatting colour;
        final BossEvent.BossBarColor barColour;

        Kind(int total, int special, int types, ChatFormatting colour, BossEvent.BossBarColor barColour) {
            this.total = total;
            this.special = special;
            this.types = types;
            this.colour = colour;
            this.barColour = barColour;
        }

        String key() {
            return name().toLowerCase();
        }
    }

    /** The night ritual's 8 types; each must pollinate once (round 4). */
    static final List<String> NIGHT_TYPES = List.of("glow", "amethyst", "deep_dark", "obsidian", "soul", "prismarine",
            "frost", "corrupt_nature");
    static final int QUEEN_TICKS = 5 * 60 * 20;

    final Kind kind;
    final BlockPos altar;
    final long startTime;
    final ServerBossEvent bar;
    final List<BlockPos> candles = new ArrayList<>();
    final List<UUID> darkBees = new ArrayList<>();
    final Set<String> types = new HashSet<>();
    int total, special;
    boolean paused;
    long nextDarkBee;
    /** Set when it has ended (won or failed): the bar stays up until then, showing the result. */
    long removeAt = -1;

    Ritual(Kind kind, BlockPos altar, long startTime) {
        this.kind = kind;
        this.altar = altar.immutable();
        this.startTime = startTime;
        this.bar = new ServerBossEvent(Component.translatable("ritual.bubbasbees." + kind.key()), kind.barColour,
                BossEvent.BossBarOverlay.PROGRESS);
        this.bar.setProgress(0.0F);
    }

    boolean isRunning() {
        return removeAt < 0;
    }

    boolean isWon() {
        if (total < kind.total || special < kind.special) return false;
        return kind != Kind.NIGHT || types.containsAll(NIGHT_TYPES);
    }

    int typesDone() {
        int done = 0;
        for (String type : NIGHT_TYPES) if (types.contains(type)) done++;
        return done;
    }

    float progress() {
        float total = Math.min(this.total, kind.total) / (float) kind.total;
        float special = Math.min(this.special, kind.special) / (float) kind.special;
        if (kind == Kind.QUEEN) return 0.8F * total + 0.2F * special;
        return 0.5F * total + 0.3F * special + 0.2F * typesDone() / NIGHT_TYPES.size();
    }

    /** The bar title: the ritual's name, then only what's still missing, the time left, and a pause note. */
    void refreshBar(long ticksLeft) {
        if (!isRunning()) return;
        MutableComponent title = Component.translatable("ritual.bubbasbees." + kind.key()).withStyle(kind.colour);
        List<Component> parts = new ArrayList<>();
        if (total < kind.total) parts.add(Component.translatable("ritual.bubbasbees.total", total, kind.total));
        if (special < kind.special) {
            parts.add(Component.translatable(kind == Kind.NIGHT ? "ritual.bubbasbees.starlight" : "ritual.bubbasbees.guards",
                    special, kind.special));
        }
        if (kind == Kind.NIGHT && typesDone() < NIGHT_TYPES.size()) {
            parts.add(Component.translatable("ritual.bubbasbees.types", typesDone(), NIGHT_TYPES.size()));
            if (NIGHT_TYPES.size() - typesDone() <= 3) {
                MutableComponent names = Component.empty();
                boolean first = true;
                for (String type : NIGHT_TYPES) {
                    if (types.contains(type)) continue;
                    if (!first) names.append(", ");
                    names.append(Component.translatable("entity.bubbasbees." + type + "_bee"));
                    first = false;
                }
                parts.add(Component.translatable("ritual.bubbasbees.missing_types", names));
            }
        }
        long seconds = Math.max(0, ticksLeft) / 20;
        parts.add(Component.translatable("ritual.bubbasbees.time_left", String.format("%d:%02d", seconds / 60, seconds % 60)));
        if (paused) parts.add(Component.translatable("ritual.bubbasbees.paused").withStyle(ChatFormatting.RED));
        for (Component part : parts) title.append(Component.literal(" · ").withStyle(ChatFormatting.GRAY)).append(part);
        bar.setName(title);
        bar.setProgress(Math.min(1.0F, progress()));
    }
}
