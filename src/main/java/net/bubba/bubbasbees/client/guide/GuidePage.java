package net.bubba.bubbasbees.client.guide;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * One page of an entry, drawn in a 116 x 156 area. Page kinds (the "type" in the entry JSON): text, crafting,
 * entity, image, spotlight, multiblock. Text lines below the picture follow gen_book.py's line budgets.
 */
public abstract class GuidePage {
    public static final int W = 116, H = 156;
    /** Where the text starts under a page's title and separator. */
    protected static final int TITLE_TEXT_Y = 22;

    protected final JsonObject json;
    protected final String entryName;
    protected final boolean first;
    private GuideText.Layout layout;
    private int layoutY;

    protected GuidePage(JsonObject json, String entryName, boolean first) {
        this.json = json;
        this.entryName = entryName;
        this.first = first;
    }

    public static GuidePage of(JsonObject json, String entryName, boolean first) {
        String type = GsonHelper.getAsString(json, "type", "text");
        type = type.substring(type.indexOf(':') + 1);
        return switch (type) {
            case "crafting" -> new Crafting(json, entryName, first);
            case "entity" -> new EntityPage(json, entryName, first);
            case "image" -> new Image(json, entryName, first);
            case "spotlight" -> new Spotlight(json, entryName, first);
            case "multiblock" -> new Multiblock(json, entryName, first);
            default -> new Text(json, entryName, first);
        };
    }

    public abstract void render(GuideScreen screen, PoseStack pose, int x, int y, int mx, int my);

    /** A click on this page; true if it did something. */
    public boolean click(GuideScreen screen, int x, int y, double mx, double my) {
        String link = layout == null ? null : layout.linkAt(x, y + layoutY, mx, my);
        if (link != null) {
            screen.openLink(link);
            return true;
        }
        return false;
    }

    /** The page's "text" field at (x, y + dy). */
    protected void text(PoseStack pose, Font font, int x, int y, int dy, int mx, int my) {
        String text = GsonHelper.getAsString(json, "text", "");
        if (text.isEmpty()) return;
        if (layout == null) layout = GuideText.layout(font, text, W - 2);
        layoutY = dy;
        GuideText.draw(pose, font, layout, x, y + dy, mx, my);
    }

    /** A centred title with the honey separator under it. */
    protected static void header(PoseStack pose, Font font, String title, int x, int y) {
        GuideText.drawCentred(pose, font, GuideText.styled(title, true), x, y, W, GuideText.COLOUR);
        RenderSystem.setShaderTexture(0, GuideScreen.TEXTURE);
        GuiComponent.blit(pose, x + 3, y + 11, 140, 180, 110, 3, 512, 256);
    }

    /** The wood frame from the GUI sheet (106 x 106), centred on the page. */
    protected static void frame(PoseStack pose, int x, int y) {
        RenderSystem.setShaderTexture(0, GuideScreen.TEXTURE);
        GuiComponent.blit(pose, x + (W - 106) / 2, y, 405, 149, 106, 106, 512, 256);
    }

    // ------------------------------------------------------------------ kinds
    static final class Text extends GuidePage {
        Text(JsonObject json, String entryName, boolean first) {
            super(json, entryName, first);
        }

        @Override
        public void render(GuideScreen screen, PoseStack pose, int x, int y, int mx, int my) {
            String title = first ? entryName : GsonHelper.getAsString(json, "title", null);
            if (title != null) header(pose, screen.font(), title, x, y);
            text(pose, screen.font(), x, y, title != null ? TITLE_TEXT_Y : 0, mx, my);
        }
    }

    static final class Crafting extends GuidePage {
        private static final int SLOT = 18;

        Crafting(JsonObject json, String entryName, boolean first) {
            super(json, entryName, first);
        }

        @Override
        public void render(GuideScreen screen, PoseStack pose, int x, int y, int mx, int my) {
            String id1 = GsonHelper.getAsString(json, "recipe", null);
            String id2 = GsonHelper.getAsString(json, "recipe2", null);
            recipe(screen, pose, id1, GsonHelper.getAsString(json, "title", null), x, y, mx, my);
            if (id2 != null) recipe(screen, pose, id2, null, x, y + 78, mx, my);
            else text(pose, screen.font(), x, y, 76, mx, my);
        }

        private void recipe(GuideScreen screen, PoseStack pose, @Nullable String id, @Nullable String title,
                            int x, int y, int mx, int my) {
            Minecraft mc = Minecraft.getInstance();
            Recipe<?> recipe = id == null || mc.level == null ? null
                    : mc.level.getRecipeManager().byKey(new ResourceLocation(id)).orElse(null);
            if (recipe == null) {
                header(pose, screen.font(), "Missing recipe", x, y);
                return;
            }
            ItemStack out = recipe.getResultItem();
            header(pose, screen.font(), title != null ? title : out.getHoverName().getString(), x, y);
            int gx = x + 3, gy = y + 16;
            int w = 3, h = 3;
            if (recipe instanceof ShapedRecipe shaped) { w = shaped.getWidth(); h = shaped.getHeight(); }
            var ingredients = recipe.getIngredients();
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    int sx = gx + c * SLOT, sy = gy + r * SLOT;
                    slot(pose, sx, sy, SLOT);
                    int i = recipe instanceof ShapedRecipe ? (c < w && r < h ? r * w + c : -1) : r * 3 + c;
                    if (i < 0 || i >= ingredients.size()) continue;
                    ItemStack stack = cycle(ingredients.get(i));
                    if (stack.isEmpty()) continue;
                    GuideRender.item(stack, sx + 1, sy + 1, 1, false);
                    if (mx >= sx && mx < sx + SLOT && my >= sy && my < sy + SLOT) screen.tooltip(stack);
                }
            }
            screen.font().draw(pose, GuideText.styled("→", true), gx + 3 * SLOT + 6, gy + SLOT + 5, 0x5A3A1A);
            int ox = gx + 3 * SLOT + 22, oy = gy + SLOT - 4;
            slot(pose, ox, oy, 26);
            GuideRender.item(out, ox + 5, oy + 5, 1, true);
            if (mx >= ox && mx < ox + 26 && my >= oy && my < oy + 26) screen.tooltip(out);
        }

        /** A recessed slot in the page colours. */
        private static void slot(PoseStack pose, int x, int y, int size) {
            GuiComponent.fill(pose, x, y, x + size, y + size, 0xFF8A6238);
            GuiComponent.fill(pose, x + 1, y + 1, x + size, y + size, 0xFFFFF6DE);
            GuiComponent.fill(pose, x + 1, y + 1, x + size - 1, y + size - 1, 0xFFE4CF9E);
        }

        private static ItemStack cycle(Ingredient ingredient) {
            ItemStack[] items = ingredient.getItems();
            if (items.length == 0) return ItemStack.EMPTY;
            return items[(int) (System.currentTimeMillis() / 1000 % items.length)];
        }
    }

    static final class EntityPage extends GuidePage {
        private static final Map<String, Entity> CACHE = new HashMap<>();

        EntityPage(JsonObject json, String entryName, boolean first) {
            super(json, entryName, first);
        }

        @Override
        public void render(GuideScreen screen, PoseStack pose, int x, int y, int mx, int my) {
            header(pose, screen.font(), GsonHelper.getAsString(json, "name", entryName), x, y);
            frame(pose, x, y + 13);
            Entity entity = entity(GsonHelper.getAsString(json, "entity"));
            if (entity != null) {
                float size = 42F / Math.max(entity.getBbWidth(), entity.getBbHeight());
                float yaw = (System.currentTimeMillis() % 12000L) / 12000F * 360F;
                GuideRender.entity(entity, x + W / 2, y + 13 + 60 + (int) (entity.getBbHeight() * size / 2), size, yaw);
            }
            text(pose, screen.font(), x, y, 121, mx, my);
        }

        static @Nullable Entity entity(String id) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return null;
            Entity cached = CACHE.get(id);
            if (cached != null && cached.level == mc.level) return cached;
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
            Entity e = type == null ? null : type.create(mc.level);
            if (e != null) CACHE.put(id, e);
            return e;
        }
    }

    static final class Image extends GuidePage {
        Image(JsonObject json, String entryName, boolean first) {
            super(json, entryName, first);
        }

        @Override
        public void render(GuideScreen screen, PoseStack pose, int x, int y, int mx, int my) {
            String title = GsonHelper.getAsString(json, "title", null);
            int top = 6;
            if (title != null) { header(pose, screen.font(), title, x, y); top = TITLE_TEXT_Y; }
            var images = GsonHelper.getAsJsonArray(json, "images");
            if (!images.isEmpty()) {
                int i = (int) (System.currentTimeMillis() / 2000 % images.size());
                RenderSystem.setShaderTexture(0, new ResourceLocation(images.get(i).getAsString()));
                RenderSystem.enableBlend();
                // gen_book.py draws the art inside the top-left 200 x 200 of a 256 canvas
                GuiComponent.blit(pose, x + 8, y + top, 100, 100, 0, 0, 200, 200, 256, 256);
                RenderSystem.disableBlend();
            }
            text(pose, screen.font(), x, y, top + 104, mx, my);
        }
    }

    static final class Spotlight extends GuidePage {
        Spotlight(JsonObject json, String entryName, boolean first) {
            super(json, entryName, first);
        }

        @Override
        public void render(GuideScreen screen, PoseStack pose, int x, int y, int mx, int my) {
            JsonObject itemJson = json.has("item") && json.get("item").isJsonObject() ? json.getAsJsonObject("item") : null;
            String id = itemJson != null ? GsonHelper.getAsString(itemJson, "item") : GsonHelper.getAsString(json, "item", "");
            ItemStack stack = GuideBook.icon(id.contains("{") ? id.substring(0, id.indexOf('{')) : id);
            String title = GsonHelper.getAsString(json, "title", first ? entryName : stack.getHoverName().getString());
            header(pose, screen.font(), title, x, y);
            int ix = x + W / 2 - 16, iy = y + 18;
            GuideRender.item(stack, ix, iy, 2, false);
            if (mx >= ix && mx < ix + 32 && my >= iy && my < iy + 32) screen.tooltip(stack);
            text(pose, screen.font(), x, y, 57, mx, my);
        }
    }

    static final class Multiblock extends GuidePage {
        private GuideMultiblock multiblock;

        Multiblock(JsonObject json, String entryName, boolean first) {
            super(json, entryName, first);
        }

        String name() {
            return GsonHelper.getAsString(json, "name", entryName);
        }

        GuideMultiblock multiblock() {
            if (multiblock == null) multiblock = new GuideMultiblock(json.getAsJsonObject("multiblock"));
            return multiblock;
        }

        private int[] eye(int x, int y) {
            return new int[]{x + (W - 106) / 2 + 106 - 15, y + 13 + 106 - 15};
        }

        @Override
        public void render(GuideScreen screen, PoseStack pose, int x, int y, int mx, int my) {
            header(pose, screen.font(), name(), x, y);
            frame(pose, x, y + 13);
            float yaw = (System.currentTimeMillis() % 20000L) / 20000F * 360F;
            GuideRender.multiblock(multiblock(), x + (W - 106) / 2 + 4, y + 17, 98, 98, yaw);
            if (GsonHelper.getAsBoolean(json, "enable_visualize", true)) {
                int[] e = eye(x, y);
                boolean hover = mx >= e[0] && mx < e[0] + 11 && my >= e[1] && my < e[1] + 11;
                boolean on = GuideVisualizer.isShowing(name());
                RenderSystem.setShaderTexture(0, GuideScreen.TEXTURE);
                GuiComponent.blit(pose, e[0], e[1], hover || on ? 319 : 308, 31, 11, 11, 512, 256);
                if (hover) screen.tooltipText(Component.translatable(on
                        ? "gui.bubbasbees.guide.visualize_off" : "gui.bubbasbees.guide.visualize"));
            }
            text(pose, screen.font(), x, y, 121, mx, my);
        }

        @Override
        public boolean click(GuideScreen screen, int x, int y, double mx, double my) {
            if (GsonHelper.getAsBoolean(json, "enable_visualize", true)) {
                int[] e = eye(x, y);
                if (mx >= e[0] && mx < e[0] + 11 && my >= e[1] && my < e[1] + 11) {
                    if (GuideVisualizer.isShowing(name())) GuideVisualizer.clear();
                    else {
                        GuideVisualizer.show(name(), multiblock());
                        screen.onClose();
                    }
                    return true;
                }
            }
            return super.click(screen, x, y, mx, my);
        }
    }
}
