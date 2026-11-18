package net.bubba.bubbasbees.util;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/** A registered object held by id, so the code can keep calling {@code .get()} like the old deferred holders. */
public final class Reg<T> implements Supplier<T> {
    private final ResourceLocation id;
    private T value;

    private Reg(ResourceLocation id) {
        this.id = id;
    }

    public static <T> Reg<T> of(ResourceLocation id) {
        return new Reg<>(id);
    }

    public void set(T value) {
        this.value = value;
    }

    public ResourceLocation id() {
        return id;
    }

    @Override
    public T get() {
        return value;
    }
}
