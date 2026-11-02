package net.bubba.bubbasbees.entity.custom;

import net.bubba.bubbasbees.bees.BeeSpec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/** Every roster bee: one class, told apart by its BeeSpec (see BeeSpecs). */
public class ModBee extends BubbasBee {
    private final BeeSpec spec;

    public ModBee(EntityType<? extends Bee> type, Level level, BeeSpec spec) {
        super(type, level, spec);
        this.spec = spec;
    }

    @Override
    public @NotNull BeeSpec getSpec() {
        return spec;
    }
}
