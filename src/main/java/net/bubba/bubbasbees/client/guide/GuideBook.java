package net.bubba.bubbasbees.client.guide;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bubba.bubbasbees.BubbasBees;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Encyclobeedia's content, read from assets/bubbasbees/encyclobeedia/ (written by BubbasBeesGuide/gen_book.py):
 * book.json, categories/&lt;id&gt;.json and entries/&lt;category&gt;/&lt;id&gt;.json. Client resources, so a resource pack can
 * replace any of it. Reloaded with the other resources (F3+T).
 */
public final class GuideBook {
    public static final String ROOT = "encyclobeedia";
    private static final Gson GSON = new Gson();
    private static GuideBook current = new GuideBook();

    public String name = "Encyclobeedia";
    public String subtitle = "";
    public String landingText = "";
    public final List<Category> categories = new ArrayList<>();
    /** Entry id ("category/name", as used by $(l:...) links) to entry. */
    public final Map<String, Entry> entries = new LinkedHashMap<>();

    public static GuideBook get() {
        return current;
    }

    public record Category(String id, String name, String description, ItemStack icon, int sortnum, List<Entry> entries) {
    }

    public record Entry(String id, String name, ItemStack icon, String category, int sortnum, List<JsonObject> pages) {
    }

    public @Nullable Category category(String id) {
        for (Category c : categories) if (c.id().equals(id)) return c;
        return null;
    }

    public static ItemStack icon(@Nullable String id) {
        if (id == null || id.isEmpty()) return new ItemStack(Items.BOOK);
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return item == null || item == Items.AIR ? new ItemStack(Items.BARRIER) : new ItemStack(item);
    }

    /** "bubbasbees:rituals" or "rituals" -> "rituals". */
    private static String localId(String id) {
        int i = id.indexOf(':');
        return i < 0 ? id : id.substring(i + 1);
    }

    public static final class Loader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonObject>> {
        @Override
        protected Map<ResourceLocation, JsonObject> prepare(ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, JsonObject> out = new LinkedHashMap<>();
            Map<ResourceLocation, Resource> found = manager.listResources(ROOT, rl -> rl.getPath().endsWith(".json"));
            for (Map.Entry<ResourceLocation, Resource> e : found.entrySet()) {
                if (!e.getKey().getNamespace().equals(BubbasBees.MOD_ID)) continue;
                try (Reader reader = e.getValue().openAsReader()) {
                    JsonElement json = GSON.fromJson(reader, JsonElement.class);
                    if (json != null && json.isJsonObject()) out.put(e.getKey(), json.getAsJsonObject());
                } catch (Exception ex) {
                    BubbasBees.LOGGER.error("Encyclobeedia: can't read {}", e.getKey(), ex);
                }
            }
            return out;
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonObject> files, ResourceManager manager, ProfilerFiller profiler) {
            GuideBook book = new GuideBook();
            Map<String, List<Entry>> byCategory = new LinkedHashMap<>();
            List<JsonObject> categoryJson = new ArrayList<>();
            List<String> categoryIds = new ArrayList<>();
            for (Map.Entry<ResourceLocation, JsonObject> f : files.entrySet()) {
                String path = f.getKey().getPath().substring(ROOT.length() + 1);   // book.json, categories/x.json, entries/c/x.json
                path = path.substring(0, path.length() - ".json".length());
                JsonObject json = f.getValue();
                try {
                    if (path.equals("book")) {
                        book.name = GsonHelper.getAsString(json, "name", book.name);
                        book.subtitle = GsonHelper.getAsString(json, "subtitle", "");
                        book.landingText = GsonHelper.getAsString(json, "landing_text", "");
                    } else if (path.startsWith("categories/")) {
                        categoryIds.add(path.substring("categories/".length()));
                        categoryJson.add(json);
                    } else if (path.startsWith("entries/")) {
                        String id = path.substring("entries/".length());
                        List<JsonObject> pages = new ArrayList<>();
                        for (JsonElement p : GsonHelper.getAsJsonArray(json, "pages")) pages.add(p.getAsJsonObject());
                        Entry entry = new Entry(id, GsonHelper.getAsString(json, "name"),
                                icon(GsonHelper.getAsString(json, "icon", null)),
                                localId(GsonHelper.getAsString(json, "category")), GsonHelper.getAsInt(json, "sortnum", 0), pages);
                        book.entries.put(id, entry);
                        byCategory.computeIfAbsent(entry.category(), k -> new ArrayList<>()).add(entry);
                    }
                } catch (Exception ex) {
                    BubbasBees.LOGGER.error("Encyclobeedia: bad file {}", f.getKey(), ex);
                }
            }
            for (int i = 0; i < categoryIds.size(); i++) {
                JsonObject json = categoryJson.get(i);
                List<Entry> entries = byCategory.getOrDefault(categoryIds.get(i), new ArrayList<>());
                entries.sort(Comparator.comparingInt(Entry::sortnum).thenComparing(Entry::name));
                book.categories.add(new Category(categoryIds.get(i), GsonHelper.getAsString(json, "name"),
                        GsonHelper.getAsString(json, "description", ""), icon(GsonHelper.getAsString(json, "icon", null)),
                        GsonHelper.getAsInt(json, "sortnum", 0), entries));
            }
            book.categories.sort(Comparator.comparingInt(Category::sortnum));
            current = book;
            GuideScreen.forgetPlace();
        }
    }
}
