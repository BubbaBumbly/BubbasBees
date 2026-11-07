package net.bubba.bubbasbees.client.guide;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The book's small markup, laid out in Minecraft's uniform (unicode) font, 9-px lines:
 * $(l) bold on, $() plain again, $(l:category/entry) ... $(/l) a link, $(br) new line, $(br2) blank line,
 * $(li) a bullet item. Wrapping matches BubbasBeesGuide/gen_book.py's layout() so its page-fit checks hold.
 */
public final class GuideText {
    public static final ResourceLocation FONT = new ResourceLocation("minecraft", "uniform");
    public static final Style BASE = Style.EMPTY.withFont(FONT);
    public static final int LINE = 9;
    private static final int LI_INDENT = 10;
    public static final int COLOUR = 0x000000;
    public static final int LINK = 0x0D4FA8;
    public static final int LINK_HOVER = 0x8A3FC0;
    private static final Pattern COMMAND = Pattern.compile("\\$\\(([^)]*)\\)");

    private GuideText() {
    }

    /** One run of text on one line; link = the entry it opens, or null. */
    public record Word(Component text, int x, int line, int width, @Nullable String link) {
    }

    public record Layout(List<Word> words, List<int[]> bullets, int lines) {
        /** The link under (mx, my) when the layout is drawn at (x, y), or null. */
        public @Nullable String linkAt(int x, int y, double mx, double my) {
            for (Word w : words) {
                if (w.link() == null) continue;
                int wx = x + w.x(), wy = y + w.line() * LINE;
                if (mx >= wx && mx < wx + w.width() && my >= wy - 1 && my < wy + LINE - 1) return w.link();
            }
            return null;
        }
    }

    public static Component styled(String text, boolean bold) {
        return Component.literal(text).withStyle(BASE.withBold(bold));
    }

    public static Layout layout(Font font, String markup, int width) {
        List<Word> words = new ArrayList<>();
        List<int[]> bullets = new ArrayList<>();
        int line = 0, x = 0, indent = 0;
        boolean bold = false;
        String link = null;
        Matcher m = COMMAND.matcher(markup);
        int at = 0;
        List<Object> tokens = new ArrayList<>();    // String text or String[]{command}
        while (m.find()) {
            if (m.start() > at) tokens.add(markup.substring(at, m.start()));
            tokens.add(new String[]{m.group(1)});
            at = m.end();
        }
        if (at < markup.length()) tokens.add(markup.substring(at));
        for (Object t : tokens) {
            if (t instanceof String[] cmd) {
                String c = cmd[0];
                switch (c) {
                    case "br" -> { line++; x = indent = 0; }
                    case "br2" -> { line += 2; x = indent = 0; }
                    case "li" -> {
                        if (x > 0) line++;
                        x = indent = LI_INDENT;
                        bullets.add(new int[]{line});
                    }
                    case "l" -> bold = true;
                    case "" -> bold = false;
                    case "/l" -> link = null;
                    default -> {
                        if (c.startsWith("l:")) link = c.substring(2);
                    }
                }
                continue;
            }
            String text = (String) t;
            int i = 0;
            while (i < text.length()) {     // words and single spaces
                int j = text.charAt(i) == ' ' ? i + 1 : nextSpace(text, i);
                String word = text.substring(i, j);
                i = j;
                Component comp = styled(word, bold);
                int w = font.width(comp);
                if (!word.equals(" ") && x + w > width && x > indent) {
                    line++;
                    x = indent;
                }
                if (word.equals(" ") && x == indent) continue;
                words.add(new Word(comp, x, line, w, link));
                x += w;
            }
        }
        return new Layout(words, bullets, line + 1);
    }

    private static int nextSpace(String s, int from) {
        int k = s.indexOf(' ', from);
        return k < 0 ? s.length() : k;
    }

    public static void draw(PoseStack pose, Font font, Layout layout, int x, int y, double mx, double my) {
        String hovered = layout.linkAt(x, y, mx, my);
        for (Word w : layout.words()) {
            int colour = w.link() == null ? COLOUR : (w.link().equals(hovered) ? LINK_HOVER : LINK);
            font.draw(pose, w.text(), x + w.x(), y + w.line() * LINE, colour);
        }
        for (int[] b : layout.bullets()) {
            font.draw(pose, styled("•", false), x + 3, y + b[0] * LINE, COLOUR);
        }
    }

    /** Draws one line centred on [x, x + width), shrunk to fit if needed. */
    public static void drawCentred(PoseStack pose, Font font, Component text, int x, int y, int width, int colour) {
        int w = font.width(text);
        if (w <= width) {
            font.draw(pose, text, x + (width - w) / 2f, y, colour);
            return;
        }
        float s = width / (float) w;
        pose.pushPose();
        pose.translate(x, y + (1 - s) * 4, 0);
        pose.scale(s, s, 1);
        font.draw(pose, text, 0, 0, colour);
        pose.popPose();
    }
}
