package net.bubba.bubbasbees.entity.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.bubba.bubbasbees.BubbasBees;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * The starlight bee's "window into the night sky" (user pick 2026-11-04, of three tested looks): the star pass draws
 * the bee's body with SKY, stars fixed to the screen (shaders/core/starlight_screen), so they slide across the bee as
 * it flies; StarlightOverlayLayer then draws the stripes, eyes, legs and wings on top from starlight_bee_overlay.png,
 * so its transparent pixels are the window. While an Oculus/Iris shader pack is in use (custom core shaders don't
 * survive a pack) or the shader failed to load, the bee falls back to its twinkling texture (16 frames).
 * Extends RenderType only to reach its protected state shards; it is never constructed.
 */
public final class StarlightSky extends RenderType {
    public static final ResourceLocation OVERLAY = tex("starlight_bee_overlay");
    private static final ResourceLocation SKY_TILE = tex("starlight_sky_tile");
    private static final ResourceLocation NEAR_STARS = tex("starlight_stars_near");

    /** Twinkle frames for the normal look: starlight_bee_twinkle_0..15, 3 ticks each. */
    private static final int TWINKLE_FRAMES = 16, TWINKLE_TICKS = 3;
    private static final ResourceLocation[] TWINKLE = new ResourceLocation[TWINKLE_FRAMES];

    static {
        for (int i = 0; i < TWINKLE_FRAMES; i++) TWINKLE[i] = tex("starlight_bee_twinkle_" + i);
    }

    @Nullable private static ShaderInstance screenShader;

    public static final RenderType SKY = create("bubbasbees_starlight_screen", DefaultVertexFormat.POSITION,
            VertexFormat.Mode.QUADS, 256, false, false, CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> screenShader))
                    .setTextureState(MultiTextureStateShard.builder().add(SKY_TILE, false, false).add(NEAR_STARS, false, false).build())
                    .createCompositeState(false));


    private StarlightSky() {
        super("unused", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 0, false, false, () -> {}, () -> {});
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/" + name + ".png");
    }

    /** Mod bus, client only (BubbasBees.ClientModEvents). */
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceManager(),
                new ResourceLocation(BubbasBees.MOD_ID, "starlight_screen"), DefaultVertexFormat.POSITION), s -> screenShader = s);
    }

    /** Whether the star look can be drawn now: the shader loaded and no shader pack is in use. */
    public static boolean available() {
        return screenShader != null && !shaderPackInUse();
    }

    public static ResourceLocation twinkleFrame(long gameTime) {
        return TWINKLE[(int) ((gameTime / TWINKLE_TICKS) % TWINKLE_FRAMES)];
    }

    // Oculus/Iris API looked up by reflection so the mod doesn't depend on it
    @Nullable private static Object irisApi;
    @Nullable private static Method packInUse;
    private static boolean irisLooked;

    private static boolean shaderPackInUse() {
        if (!irisLooked) {
            irisLooked = true;
            try {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApi = api.getMethod("getInstance").invoke(null);
                packInUse = api.getMethod("isShaderPackInUse");
            } catch (ReflectiveOperationException | LinkageError e) {
                irisApi = null;
            }
        }
        if (irisApi == null || packInUse == null) return false;
        try {
            return (boolean) packInUse.invoke(irisApi);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
