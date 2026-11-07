package net.bubba.bubbasbees.client.guide;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.bubba.bubbasbees.BubbasBees;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The Encyclobeedia: a two-page book on the user's GUI sheet (textures/gui/encyclobeedia.png, 512 x 256: book 272 x 180
 * at 0,0, page arrows 272,0, back arrow 308,0, eye 308,31, banner 0,180, separator 140,180, frame 405,149).
 * Landing (title + welcome | categories), category (description | entries), entry (two pages per spread).
 */
public class GuideScreen extends Screen {
    public static final ResourceLocation TEXTURE = new ResourceLocation(BubbasBees.MOD_ID, "textures/gui/encyclobeedia.png");
    private static final int BOOK_W = 272, BOOK_H = 180, LEFT = 15, RIGHT = 141, TOP = 12, ROW = 13;

    private enum View { LANDING, CATEGORY, ENTRY }

    private record Place(View view, String id, int spread) {
    }

    private static @Nullable Place remembered;
    private static final Deque<Place> history = new ArrayDeque<>();

    private Place place;
    private List<GuidePage> pages = List.of();
    private int bx, by;
    private ItemStack tooltipStack = ItemStack.EMPTY;
    private Component tooltipText;

    public GuideScreen() {
        super(Component.literal(GuideBook.get().name));
        place = remembered != null ? remembered : new Place(View.LANDING, "", 0);
        loadPages();
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new GuideScreen());
    }

    /** Called when the book's files reload: the remembered place may no longer exist. */
    static void forgetPlace() {
        remembered = null;
        history.clear();
    }

    public Font font() {
        return font;
    }

    public void tooltip(ItemStack stack) {
        tooltipStack = stack;
    }

    public void tooltipText(Component text) {
        tooltipText = text;
    }

    @Override
    public boolean isPauseScreen() {
        return false;   // bees keep flapping on their pages
    }

    @Override
    protected void init() {
        bx = (width - BOOK_W) / 2;
        by = (height - BOOK_H) / 2;
    }

    // ------------------------------------------------------------------ navigation
    private void loadPages() {
        pages = new ArrayList<>();
        if (place.view() == View.ENTRY) {
            GuideBook.Entry entry = GuideBook.get().entries.get(place.id());
            if (entry == null) { place = new Place(View.LANDING, "", 0); return; }
            for (int i = 0; i < entry.pages().size(); i++) pages.add(GuidePage.of(entry.pages().get(i), entry.name(), i == 0));
        }
    }

    private void go(Place to, boolean remember) {
        if (remember) history.push(place);
        place = to;
        remembered = to;
        loadPages();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    public void openLink(String target) {
        int hash = target.indexOf('#');
        String id = hash < 0 ? target : target.substring(0, hash);
        if (GuideBook.get().entries.containsKey(id)) go(new Place(View.ENTRY, id, 0), true);
    }

    private void back() {
        if (!history.isEmpty()) go(history.pop(), false);
        else if (place.view() == View.ENTRY) {
            GuideBook.Entry e = GuideBook.get().entries.get(place.id());
            go(e != null ? new Place(View.CATEGORY, e.category(), 0) : new Place(View.LANDING, "", 0), false);
        } else if (place.view() == View.CATEGORY) go(new Place(View.LANDING, "", 0), false);
    }

    /** Number of spreads in the current view. */
    private int spreads() {
        return switch (place.view()) {
            case ENTRY -> Math.max(1, (pages.size() + 1) / 2);
            case CATEGORY -> {
                GuideBook.Category c = GuideBook.get().category(place.id());
                int n = c == null ? 0 : c.entries().size();
                yield n <= rowsFirst() ? 1 : 1 + (n - rowsFirst() + 2 * rowsFull() - 1) / (2 * rowsFull());
            }
            case LANDING -> 1;
        };
    }

    private static int rowsFirst() {
        return (GuidePage.H - 22) / ROW;
    }

    private static int rowsFull() {
        return GuidePage.H / ROW;
    }

    private void turn(int by) {
        int s = Math.max(0, Math.min(spreads() - 1, place.spread() + by));
        if (s != place.spread()) go(new Place(place.view(), place.id(), s), false);
    }

    // ------------------------------------------------------------------ drawing
    @Override
    public void render(PoseStack pose, int mx, int my, float partialTick) {
        tooltipStack = ItemStack.EMPTY;
        tooltipText = null;
        renderBackground(pose);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableBlend();
        GuiComponent.blit(pose, bx, by, 0, 0, BOOK_W, BOOK_H, 512, 256);
        int lx = bx + LEFT, rx = bx + RIGHT, ty = by + TOP;
        switch (place.view()) {
            case LANDING -> renderLanding(pose, lx, rx, ty, mx, my);
            case CATEGORY -> renderCategory(pose, lx, rx, ty, mx, my);
            case ENTRY -> {
                int i = place.spread() * 2;
                if (i < pages.size()) pages.get(i).render(this, pose, lx, ty, mx, my);
                if (i + 1 < pages.size()) pages.get(i + 1).render(this, pose, rx, ty, mx, my);
            }
        }
        renderButtons(pose, mx, my);
        if (!tooltipStack.isEmpty()) renderTooltip(pose, tooltipStack, mx, my);
        else if (tooltipText != null) renderTooltip(pose, tooltipText, mx, my);
        super.render(pose, mx, my, partialTick);
    }

    private void renderLanding(PoseStack pose, int lx, int rx, int ty, int mx, int my) {
        GuideBook book = GuideBook.get();
        RenderSystem.setShaderTexture(0, TEXTURE);
        // the banner's body cut short + its notched tail (sheet 126..138), so it ends before the spine (user 2026-11-07)
        GuiComponent.blit(pose, lx - 8, ty - 2, 0, 180, 113, 31, 512, 256);
        GuiComponent.blit(pose, lx - 8 + 113, ty - 2, 126, 180, 12, 31, 512, 256);
        font.draw(pose, GuideText.styled(book.name, true), lx + 2, ty + 3, 0xFFE9A8);
        font.draw(pose, GuideText.styled(book.subtitle, false), lx + 2, ty + 15, 0xF3DDB4);
        GuideText.Layout l = GuideText.layout(font, book.landingText, GuidePage.W - 2);
        GuideText.draw(pose, font, l, lx, ty + 36, mx, my);
        GuidePage.header(pose, font, Component.translatable("gui.bubbasbees.guide.categories").getString(), rx, ty);
        int y = ty + 22;
        for (GuideBook.Category c : book.categories) {
            row(pose, c.icon(), c.name(), rx, y, mx, my);
            y += ROW;
        }
    }

    private void renderCategory(PoseStack pose, int lx, int rx, int ty, int mx, int my) {
        GuideBook.Category c = GuideBook.get().category(place.id());
        if (c == null) return;
        if (place.spread() == 0) {
            GuidePage.header(pose, font, c.name(), lx, ty);
            GuideText.draw(pose, font, GuideText.layout(font, c.description(), GuidePage.W - 2), lx, ty + 22, mx, my);
        }
        for (Row r : rows(c)) row(pose, r.entry().icon(), r.entry().name(), r.x(), r.y(), mx, my);
    }

    private record Row(GuideBook.Entry entry, int x, int y) {
    }

    /** The entry rows shown on this spread of a category. */
    private List<Row> rows(GuideBook.Category c) {
        List<Row> out = new ArrayList<>();
        int lx = bx + LEFT, rx = bx + RIGHT, ty = by + TOP;
        List<GuideBook.Entry> es = c.entries();
        if (place.spread() == 0) {
            for (int i = 0; i < Math.min(es.size(), rowsFirst()); i++) out.add(new Row(es.get(i), rx, ty + 22 + i * ROW));
        } else {
            int start = rowsFirst() + (place.spread() - 1) * 2 * rowsFull();
            for (int i = start; i < Math.min(es.size(), start + 2 * rowsFull()); i++) {
                int k = i - start;
                out.add(new Row(es.get(i), k < rowsFull() ? lx : rx, ty + (k % rowsFull()) * ROW));
            }
        }
        return out;
    }

    private void row(PoseStack pose, ItemStack icon, String name, int x, int y, int mx, int my) {
        boolean hover = mx >= x && mx < x + GuidePage.W && my >= y && my < y + ROW;
        if (hover) GuiComponent.fill(pose, x - 1, y - 1, x + GuidePage.W, y + ROW - 1, 0x30A0661E);
        GuideRender.item(icon, x, y - 1, 0.75F, false);
        font.draw(pose, GuideText.styled(name, false), x + 15, y + 2, hover ? GuideText.LINK_HOVER : GuideText.COLOUR);
    }

    private boolean over(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void renderButtons(PoseStack pose, int mx, int my) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        int ay = by + BOOK_H - 12;
        if (place.spread() > 0) {
            int x = bx + 8;
            GuiComponent.blit(pose, x, ay, over(x, ay, 18, 10, mx, my) ? 290 : 272, 10, 18, 10, 512, 256);
        }
        if (place.spread() < spreads() - 1) {
            int x = bx + BOOK_W - 26;
            GuiComponent.blit(pose, x, ay, over(x, ay, 18, 10, mx, my) ? 290 : 272, 0, 18, 10, 512, 256);
        }
        if (place.view() != View.LANDING) {
            int x = bx + BOOK_W / 2 - 9, y = by + BOOK_H - 12;
            boolean hover = over(x, y, 18, 10, mx, my);
            GuiComponent.blit(pose, x, y, hover ? 326 : 308, 0, 18, 10, 512, 256);
            if (hover) tooltipText(Component.translatable("gui.bubbasbees.guide.back"));
        }
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 3) { back(); return true; }   // mouse "back" button
        if (button != 0) return super.mouseClicked(mx, my, button);
        int ay = by + BOOK_H - 12;
        if (place.spread() > 0 && over(bx + 8, ay, 18, 10, mx, my)) { turn(-1); return true; }
        if (place.spread() < spreads() - 1 && over(bx + BOOK_W - 26, ay, 18, 10, mx, my)) { turn(1); return true; }
        if (place.view() != View.LANDING && over(bx + BOOK_W / 2 - 9, ay, 18, 10, mx, my)) { back(); return true; }
        int lx = bx + LEFT, rx = bx + RIGHT, ty = by + TOP;
        switch (place.view()) {
            case LANDING -> {
                int y = ty + 22;
                for (GuideBook.Category c : GuideBook.get().categories) {
                    if (over(rx, y, GuidePage.W, ROW, mx, my)) { go(new Place(View.CATEGORY, c.id(), 0), true); return true; }
                    y += ROW;
                }
                String link = GuideText.layout(font, GuideBook.get().landingText, GuidePage.W - 2).linkAt(lx, ty + 36, mx, my);
                if (link != null) { openLink(link); return true; }
            }
            case CATEGORY -> {
                GuideBook.Category c = GuideBook.get().category(place.id());
                if (c != null) for (Row r : rows(c)) {
                    if (over(r.x(), r.y(), GuidePage.W, ROW, mx, my)) { go(new Place(View.ENTRY, r.entry().id(), 0), true); return true; }
                }
            }
            case ENTRY -> {
                int i = place.spread() * 2;
                if (i < pages.size() && pages.get(i).click(this, lx, ty, mx, my)) return true;
                if (i + 1 < pages.size() && pages.get(i + 1).click(this, rx, ty, mx, my)) return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        turn(delta > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        switch (key) {
            case 259 -> { back(); return true; }          // backspace
            case 263, 266 -> { turn(-1); return true; }   // left, page up
            case 262, 267 -> { turn(1); return true; }    // right, page down
            default -> { return super.keyPressed(key, scan, modifiers); }
        }
    }
}
