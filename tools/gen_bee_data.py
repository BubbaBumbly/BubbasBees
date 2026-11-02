#!/usr/bin/env python3
"""Generates the bee roster's data and art files from one table (Phase 2 step 2).

  python3 tools/gen_bee_data.py [--art <BubbasBeesArt/work>]

Writes (overwriting only what it owns):
  data/bubbasbees/recipes/{combs,gems,stars}/...   every comb, gem and star recipe (from tools/recipe_snapshot.json,
                                                   with the balance-pass counts below and the colour families expanded)
  data/bubbasbees/tags/blocks/bee_flowers/<bee>.json, data/minecraft/tags/entity_types/beehive_inhabitors.json
  assets/bubbasbees/lang/en_us.json                 (bee/comb/gem/egg/new-item names merged in)
  assets/bubbasbees/models/item/...                 comb, gem, spawn egg, stardust, royal jelly models
With --art, also copies the final bee textures + models and comb textures, makes placeholder art for items that have
none yet (gems, stardust, royal jelly), and writes each bee's spawn egg colours into BeeSpecs.java.

The Java side (BeeSpecs.java) must list the same bee ids; the script checks.
"""
import argparse
import json
import os
import re
import shutil
import sys
import zipfile
from io import BytesIO

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "bubbasbees")
DATA = os.path.join(RES, "data", "bubbasbees")
SPECS_JAVA = os.path.join(ROOT, "src", "main", "java", "net", "bubba", "bubbasbees", "bees", "BeeSpecs.java")
VANILLA_JAR = os.path.expanduser("~/.gradle/caches/forge_gradle/minecraft_repo/versions/1.19.2/client-extra.jar")
MC_VERSION = "1.19.2"

# ---------------------------------------------------------------------------------------------------------------
# Roster: id -> (display name, flowers, art). Flowers from the bee cards in finish-spec.md ("#tag" = block tag).
# art: "existing" = already in the mod; otherwise "<dir>/<key>" under BubbasBeesArt/work/drafts
# (a "+deepslate" suffix = <key>_stone / <key>_deepslate variants).
# ---------------------------------------------------------------------------------------------------------------
COPPER_BLOCKS = ["copper_block", "exposed_copper", "weathered_copper", "oxidized_copper", "waxed_copper_block",
                 "waxed_exposed_copper", "waxed_weathered_copper", "waxed_oxidized_copper"]
COLOURS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
           "purple", "blue", "brown", "green", "red", "black"]
CORALS = ["tube", "brain", "bubble", "fire", "horn"]
FLOWERS = ["dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip", "white_tulip",
           "pink_tulip", "oxeye_daisy", "cornflower", "lily_of_the_valley", "sunflower", "lilac", "rose_bush", "peony"]

ROSTER = {
    "stone": ("Stone Bee", ["stone", "deepslate", "tuff", "andesite", "diorite", "granite", "calcite", "dripstone_block"], "existing"),
    "dirt": ("Dirt Bee", ["dirt", "coarse_dirt", "podzol", "rooted_dirt", "grass_block"], "bees/dirt/A"),
    "concrete": ("Concrete Bee", [f"{c}_concrete" for c in COLOURS] + [f"{c}_concrete_powder" for c in COLOURS], "bees/concrete/A"),
    "nature": ("Nature Bee", ["#minecraft:flowers", "#minecraft:leaves", "#minecraft:saplings", "#minecraft:crops", "grass",
                              "fern", "tall_grass", "large_fern", "moss_block", "moss_carpet", "vine", "cactus", "sugar_cane",
                              "bamboo", "lily_pad", "brown_mushroom", "red_mushroom", "big_dripleaf", "small_dripleaf",
                              "azalea", "flowering_azalea"], "existing"),
    "forest": ("Forest Bee", ["oak_leaves", "birch_leaves", "dark_oak_leaves", "oak_log", "birch_log", "dark_oak_log"], "rework2/forest/C3a"),
    "desert": ("Desert Bee", ["sand"], "existing"),
    "river": ("River Bee", ["clay", "gravel"], "bees/river/C"),
    "meadow": ("Meadow Bee", ["#minecraft:flowers"], "bees/meadow/C"),
    "jungle": ("Jungle Bee", ["jungle_leaves", "jungle_log", "bamboo", "melon"], "bees/jungle/C"),
    "taiga": ("Taiga Bee", ["spruce_leaves", "spruce_log", "sweet_berry_bush"], "rework11/taiga/C2o"),
    "savanna": ("Savanna Bee", ["acacia_leaves", "acacia_log", "coarse_dirt"], "rework2/savanna/B3a"),
    "amethyst": ("Amethyst Bee", ["amethyst_block", "budding_amethyst", "small_amethyst_bud", "medium_amethyst_bud",
                                  "large_amethyst_bud", "amethyst_cluster"], "bees/amethyst/B"),
    "obsidian": ("Obsidian Bee", ["obsidian", "crying_obsidian"], "rework/obsidian/A3"),
    "glow": ("Glow Bee", ["glowstone", "glow_lichen", "shroomlight"], "rework/glow/M1"),
    "mesa": ("Mesa Bee", ["red_sand", "terracotta"] + [f"{c}_terracotta" for c in COLOURS], "rework7/mesa/A2e"),
    "swamp": ("Swamp Bee", ["mud", "lily_pad", "mangrove_leaves"], "rework2/swamp/B2a"),
    "frost": ("Frost Bee", ["snow_block", "ice", "packed_ice", "blue_ice"], "rework/frost/N2"),
    "lush_caves": ("Lush Caves Bee", ["azalea", "flowering_azalea", "moss_block", "spore_blossom", "cave_vines",
                                      "cave_vines_plant"], "rework4/lush_caves/L3e"),
    "deep_dark": ("Deep Dark Bee", ["sculk", "sculk_catalyst", "sculk_sensor"], "rework7/deep_dark/D3l"),
    "mushroom_fields": ("Mushroom Fields Bee", ["mycelium", "red_mushroom", "brown_mushroom", "red_mushroom_block",
                                                "brown_mushroom_block", "mushroom_stem"], "rework/mushroom_fields/A4"),
    "sea": ("Sea Bee", ["sea_pickle", "kelp", "kelp_plant", "seagrass", "tall_seagrass"], "bees/sea/A"),
    "coral": ("Coral Bee", [f"{d}{c}_{p}" for c in CORALS for p in ("coral_block", "coral_fan", "coral_wall_fan", "coral")
                            for d in ("", "dead_")], "bees/coral/B"),
    "prismarine": ("Prismarine Bee", ["prismarine", "prismarine_bricks", "dark_prismarine", "sea_lantern"], "bees/prismarine/B"),
    "corrupt_stone": ("Corrupt Stone Bee", ["netherrack", "basalt", "blackstone"], "bees/corrupt_stone/A"),
    "soul": ("Soul Bee", ["soul_sand", "soul_soil"], "soul"),
    "corrupt_nature": ("Corrupt Nature Bee", ["crimson_fungus", "warped_fungus", "crimson_nylium", "warped_nylium",
                                              "nether_wart", "nether_wart_block", "warped_wart_block", "weeping_vines",
                                              "weeping_vines_plant", "twisting_vines", "twisting_vines_plant",
                                              "crimson_stem", "warped_stem"], "rework3/corrupt_nature/C3b"),
    "magma": ("Magma Bee", ["magma_block"], "rework2/magma/C2a"),
    "ender": ("Ender Bee", ["end_stone", "purpur_block", "purpur_pillar", "chorus_plant", "chorus_flower"], "existing"),
    "royal_guard": ("Royal Guard Bee", ["#minecraft:flowers"], "rework5/royal_guard/B4i"),
    "starlight": ("Starlight Bee", ["end_rod", "enchanting_table"], "rework/starlight/A3"),
    "coal": ("Coal Bee", ["coal_ore", "deepslate_coal_ore", "coal_block"], "rework/coal/A2+deepslate"),
    "copper": ("Copper Bee", ["copper_ore", "deepslate_copper_ore", "raw_copper_block"] + COPPER_BLOCKS, "existing"),
    "iron": ("Iron Bee", ["iron_ore", "deepslate_iron_ore", "raw_iron_block", "iron_block"], "existing"),
    "gold": ("Gold Bee", ["gold_ore", "deepslate_gold_ore", "nether_gold_ore", "raw_gold_block", "gold_block"], "existing"),
    "redstone": ("Redstone Bee", ["redstone_ore", "deepslate_redstone_ore", "redstone_block"], "existing"),
    "lapis": ("Lapis Bee", ["lapis_ore", "deepslate_lapis_ore", "lapis_block"], "rework/lapis/A2+deepslate"),
    "diamond": ("Diamond Bee", ["diamond_ore", "deepslate_diamond_ore", "diamond_block"], "existing"),
    "emerald": ("Emerald Bee", ["emerald_ore", "deepslate_emerald_ore", "emerald_block"], "rework/emerald/A2+deepslate"),
    "quartz": ("Quartz Bee", ["nether_quartz_ore", "quartz_block"], "rework/quartz/B3"),
    "ancient_debris": ("Ancient Debris Bee", ["ancient_debris", "netherite_block"], "bees/ancient_debris/C"),
    "royal_queen": ("Royal Queen", ["#minecraft:flowers", "honey_block"], "bees/royal_queen/A"),
}
SKIP_BEES = {"cherry_grove"}                       # 1.20+ only
COMB_NAMES = {"royal_queen": "Royal Honeycomb", "budding_amethyst": "Budding Amethyst Honeycomb"}

# Balance pass (finish-spec.md "Balance numbers", 2026-11-02): (bee, label) -> count
COUNTS = {("dirt", "dirt"): 32, ("desert", "bone blocks (bone comb)"): 8, ("sea", "nautilus shell (nautilus comb)"): 4,
          ("soul", "ghast tear (sad comb)"): 5, ("starlight", "stardust"): 8, ("royal_queen", "royal jelly"): 8}
SKIP_RECIPES = {("desert", "suspicious sand (1.20+)")}

# Colour families: the snapshot holds one example per family; (bee, label) -> (example seed, [(seed, output)])
FAMILIES = {
    ("concrete", "red"): ("v:red_dye", [(f"v:{c}_dye", f"v:{c}_concrete_powder") for c in COLOURS]),
    ("concrete", "blue"): None,
    ("mesa", "orange terracotta"): ("v:orange_terracotta", [(f"v:{c}_terracotta",) * 2 for c in
                                                             ("orange", "yellow", "red", "brown", "white", "light_gray")]),
    ("coral", "tube coral block"): ("v:tube_coral_block", [(f"v:{c}_{p}",) * 2 for c in CORALS
                                                           for p in ("coral_block", "coral_fan", "coral")]),
    ("coral", "fire coral fan"): None,
    ("meadow", "poppy"): ("v:poppy", [(f"v:{f}",) * 2 for f in FLOWERS]),
    ("meadow", "cornflower"): None,
    ("swamp", "froglight (+ glow)"): ("v:ochre_froglight", [(f"v:{c}_froglight",) * 2 for c in ("ochre", "verdant", "pearlescent")]),
}

STARS = {"s:lesser": "bubbasbees:lesser_bubba_star", "s:bubba": "bubbasbees:bubba_star",
         "s:ritual": "bubbasbees:celestial_bubba_star"}
GEM_STAR_FIX = {"ender": "s:bubba"}                # ender moved to the Bubba Star tier (2026-11-02)


def item_id(tok, bee=None):
    kind, n = tok.split(":", 1)
    if kind == "v":
        return "minecraft:" + n
    if kind == "c":
        return f"bubbasbees:{'desert' if n == 'sandy' else n}_honeycomb"
    if kind == "x":
        return "bubbasbees:" + n
    if kind == "g":
        return f"bubbasbees:{n}_conversion_gem"
    if kind == "s":
        return STARS[tok]
    raise ValueError(tok)


def shaped(grid, result, count=1, rtype="minecraft:crafting_shaped"):
    keys, pattern = {}, []
    letters = iter("ABCDEFGHIJ")
    for row in range(3):
        line = ""
        for tok in grid[row * 3:row * 3 + 3]:
            if tok is None:
                line += " "
                continue
            iid = item_id(tok)
            if iid not in keys:
                keys[iid] = next(letters)
            line += keys[iid]
        pattern.append(line)
    out = {"type": rtype, "pattern": pattern, "key": {k: {"item": i} for i, k in keys.items()},
           "result": {"item": result}}
    if count != 1:
        out["result"]["count"] = count
    return out


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def spec_ids():
    return re.findall(r'BeeSpec\.of\("([a-z_]+)"', open(SPECS_JAVA).read())


def vanilla_items():
    with zipfile.ZipFile(VANILLA_JAR) as jar:
        return {"minecraft:" + os.path.basename(n)[:-5] for n in jar.namelist()
                if n.startswith("assets/minecraft/models/item/") and n.endswith(".json")}


def recipes(snapshot, known):
    root = os.path.join(DATA, "recipes")
    for d in ("combs", "gems", "stars", "blocks"):
        shutil.rmtree(os.path.join(root, d), ignore_errors=True)
    made = []
    for r in snapshot["combs"]:
        bee, label = r["bee"], r["label"]
        if bee in SKIP_BEES or (bee, label) in SKIP_RECIPES:
            continue
        count = COUNTS.get((bee, label), r["count"])
        fam = FAMILIES.get((bee, label), ())
        if fam is None:
            continue
        variants = [(r["grid"], r["out"])]
        if fam:
            seed, members = fam
            variants = [([m[0] if t == seed else t for t in r["grid"]], m[-1]) for m in members]
        for grid, out in variants:
            res = item_id(out)
            made.append((f"combs/{bee}/{res.split(':')[1]}", shaped(grid, res, count)))
    for bee, g in snapshot["gems"].items():
        if bee in SKIP_BEES:
            continue
        grid = [GEM_STAR_FIX.get(bee, t) if t and t.startswith("s:") else t for t in g["grid"]]
        made.append((f"gems/{bee}_conversion_gem", shaped(grid, f"bubbasbees:{bee}_conversion_gem")))
    made.append(("stars/lesser_bubba_star", shaped(
        ["v:honey_block", "v:amethyst_shard", "v:honey_block", "v:amethyst_shard", "v:ender_eye", "v:amethyst_shard",
         "v:honey_block", "v:amethyst_shard", "v:honey_block"], "bubbasbees:lesser_bubba_star")))
    made.append(("stars/bubba_star", shaped(
        ["v:echo_shard", "v:nether_star", "v:ghast_tear", "v:goat_horn", "s:lesser", "v:phantom_membrane",
         "v:end_rod", "v:heart_of_the_sea", "v:spore_blossom"], "bubbasbees:bubba_star",
        rtype="bubbasbees:consuming_shaped")))
    bad = set()
    for name, obj in made:
        for k in obj["key"].values():
            if k["item"].startswith("minecraft:") and k["item"] not in known:
                bad.add((name, k["item"]))
        res = obj["result"]["item"]
        if res.startswith("minecraft:") and res not in known:
            bad.add((name, res))
        write_json(os.path.join(root, name + ".json"), obj)
    if bad:
        sys.exit("Unknown vanilla items in " + MC_VERSION + ": " + str(sorted(bad)))
    return made


def comb_ids():
    """Every mod comb (BeeSpecs: own comb + rare combs + extra normal combs)."""
    java = open(SPECS_JAVA).read()
    ids = []
    for m in re.finditer(r'BeeSpec\.of\("([a-z_]+)"(.*?)\.build\(\)', java, re.S):
        bee, body = m.group(1), m.group(2)
        own = re.findall(r'\.comb\("([a-z_]+)"', body)
        rare = re.findall(r'\.rare\("([a-z_]+)"', body)
        if "VANILLA_HONEYCOMB" in body:
            own = own or ["-"]
        ids += [c for c in (own or [bee]) + rare if c != "-" and c not in ids]
    return ids


def names(combs, ids):
    path = os.path.join(ASSETS, "lang", "en_us.json")
    lang = json.load(open(path))
    for bee in ids:
        n = ROSTER[bee][0]
        lang[f"entity.bubbasbees.{bee}_bee"] = n
        lang[f"item.bubbasbees.{bee}_bee_spawn_egg"] = n + " Spawn Egg"
        if bee != "royal_queen":
            lang[f"item.bubbasbees.{bee}_conversion_gem"] = n.replace(" Bee", "") + " Conversion Gem"
    for c in combs:
        lang[f"item.bubbasbees.{c}_honeycomb"] = COMB_NAMES.get(c, c.replace("_", " ").title() + " Honeycomb")
    lang["item.bubbasbees.stardust"] = "Stardust"
    lang["item.bubbasbees.royal_jelly"] = "Royal Jelly"
    lang["item.bubbasbees.celestial_bubba_star"] = "Celestial Bubba Star"
    write_json(path, lang)


def item_models(combs, ids):
    m = os.path.join(ASSETS, "models", "item")
    gen = lambda tex: {"parent": "item/generated", "textures": {"layer0": "bubbasbees:item/" + tex}}
    for c in combs:
        write_json(os.path.join(m, f"{c}_honeycomb.json"), gen(f"{c}_honeycomb"))
    for bee in ids:
        write_json(os.path.join(m, f"{bee}_bee_spawn_egg.json"), {"parent": "minecraft:item/template_spawn_egg"})
        if bee != "royal_queen":
            write_json(os.path.join(m, f"{bee}_conversion_gem.json"), gen(f"{bee}_conversion_gem"))
    for n in ("stardust", "royal_jelly"):
        write_json(os.path.join(m, n + ".json"), gen(n))


def tags(ids):
    for bee in ids:
        vals = [v if v.startswith("#") else "minecraft:" + v for v in ROSTER[bee][1]]
        write_json(os.path.join(DATA, "tags", "blocks", "bee_flowers", bee + ".json"), {"replace": False, "values": vals})
    write_json(os.path.join(RES, "data", "minecraft", "tags", "entity_types", "beehive_inhabitors.json"),
               {"replace": False, "values": ["bubbasbees:bubbas_bee"] + [f"bubbasbees:{b}_bee" for b in ids]})


# ------------------------------------------------------------------ art (--art)
def art(work, combs, ids):
    from PIL import Image
    drafts = os.path.join(work, "drafts")
    ent, geo, items = (os.path.join(ASSETS, "textures", "entity"), os.path.join(ASSETS, "geo"),
                       os.path.join(ASSETS, "textures", "item"))
    copied = []
    for bee in ids:
        src = ROSTER[bee][2]
        if src == "existing":
            continue
        if src == "soul":
            tex, model = os.path.join(drafts, "soul_translucent", "soul_final.png"), os.path.join(drafts, "bees", "soul", "B.geo.json")
            pairs = [(tex, f"{bee}_bee")]
        elif src.endswith("+deepslate"):
            base = os.path.join(drafts, src[:-len("+deepslate")])
            pairs = [(base + "_stone.png", f"{bee}_bee"), (base + "_deepslate.png", f"{bee}_deepslate_bee")]
            model = base + "_stone.geo.json"
        else:
            pairs, model = [(os.path.join(drafts, src) + ".png", f"{bee}_bee")], os.path.join(drafts, src) + ".geo.json"
        for tex, name in pairs:
            shutil.copyfile(tex, os.path.join(ent, name + ".png"))
            shutil.copyfile(tex, os.path.join(ent, name + "_angry.png"))   # angry looks = normal, as for every bee so far
        shutil.copyfile(model, os.path.join(geo, bee + ".geo.json"))
        copied.append(bee)
    for c in combs:
        src = os.path.join(drafts, "comb_finals", f"{c}_honeycomb.png")
        dst = os.path.join(items, f"{c}_honeycomb.png")
        if os.path.exists(src) and not os.path.exists(dst):
            shutil.copyfile(src, dst)
    eggs = egg_colours(ids, ent)
    placeholder_gems(ids, items, eggs)
    placeholder_items(items)
    set_egg_colours(eggs)
    print("art: bee looks copied for", len(copied), "bees")


def egg_colours(ids, ent):
    """Two most common opaque colours of each bee's texture (quantized), as the spawn egg base and spots."""
    from PIL import Image
    out = {}
    for bee in ids:
        im = Image.open(os.path.join(ent, f"{bee}_bee.png")).convert("RGBA")
        px = [p[:3] for p in im.getdata() if p[3] > 200]
        q = Image.new("RGB", (len(px), 1))
        q.putdata(px)
        pal = q.quantize(colors=4)
        counts = sorted(pal.getcolors(), reverse=True)
        palette = pal.getpalette()
        cols = [tuple(palette[i * 3:i * 3 + 3]) for _, i in counts]
        out[bee] = [cols[0], cols[1] if len(cols) > 1 else cols[0]]
    return out


def set_egg_colours(eggs):
    java = open(SPECS_JAVA).read()
    for bee, (a, b) in eggs.items():
        if ROSTER[bee][2] == "existing":
            continue    # keep the hand-picked eggs of the original bees
        hexes = "0x%02x%02x%02x, 0x%02x%02x%02x" % (a + b)
        java = re.sub(r'(BeeSpec\.of\("%s".*?\.egg\()[^)]*(\))' % bee, r"\g<1>%s\g<2>" % hexes, java, count=1, flags=re.S)
    open(SPECS_JAVA, "w").write(java)


def tint(im, col):
    im = im.copy()
    px = im.load()
    for x in range(im.width):
        for y in range(im.height):
            r, g, b, a = px[x, y]
            if a:
                k = min(1.4, (r * .3 + g * .59 + b * .11) / 150)
                px[x, y] = tuple(min(255, int(c * k)) for c in col) + (a,)
    return im


def placeholder_gems(ids, items, eggs):
    """PLACEHOLDER gem art (the real gems are made later in families): the stone gem tinted with the bee's colour."""
    from PIL import Image
    base = Image.open(os.path.join(items, "stone_conversion_gem.png")).convert("RGBA")
    meta = os.path.join(items, "stone_conversion_gem.png.mcmeta")
    for bee in ids:
        if bee == "royal_queen" or os.path.exists(os.path.join(items, f"{bee}_conversion_gem.png")):
            continue
        tint(base, eggs[bee][0]).save(os.path.join(items, f"{bee}_conversion_gem.png"))
        shutil.copyfile(meta, os.path.join(items, f"{bee}_conversion_gem.png.mcmeta"))


def placeholder_items(items):
    """PLACEHOLDER icons (same recolours as the recipe review pages): stardust, royal jelly."""
    from PIL import Image
    with zipfile.ZipFile(VANILLA_JAR) as jar:
        for name, (src, col) in {"stardust": ("glowstone_dust", (190, 200, 255)),
                                 "royal_jelly": ("slime_ball", (236, 120, 190))}.items():
            dst = os.path.join(items, name + ".png")
            if not os.path.exists(dst):
                im = Image.open(BytesIO(jar.read(f"assets/minecraft/textures/item/{src}.png"))).convert("RGBA")
                tint(im, col).save(dst)


# ------------------------------------------------------------------ step 3 blocks
PEDESTALS = {  # id: (top, column, base) textures, recipe column (top slab, middle, bottom block)
    "stone_pedestal": ("minecraft:block/smooth_stone", "minecraft:block/stone_bricks", "minecraft:block/stone",
                       ("stone_slab", "stone_brick_wall", "stone")),
    "deepslate_pedestal": ("minecraft:block/polished_deepslate", "minecraft:block/polished_deepslate", "minecraft:block/deepslate",
                           ("polished_deepslate_slab", "polished_deepslate_wall", "deepslate")),
    "blackstone_pedestal": ("minecraft:block/polished_blackstone", "minecraft:block/blackstone", "minecraft:block/polished_blackstone",
                            ("blackstone_slab", "blackstone_wall", "polished_blackstone")),
    "quartz_pedestal": ("minecraft:block/quartz_block_top", "minecraft:block/quartz_pillar", "minecraft:block/quartz_block_side",
                        ("quartz_slab", "quartz_pillar", "quartz_block")),
    "honeycomb_pedestal": ("minecraft:block/honeycomb_block", "minecraft:block/honeycomb_block", "minecraft:block/honeycomb_block",
                           ("honeycomb", "honeycomb_block", "honeycomb_block")),
}
BLOCK_NAMES = {"stone_pedestal": "Stone Pedestal", "deepslate_pedestal": "Deepslate Pedestal",
               "blackstone_pedestal": "Blackstone Pedestal", "quartz_pedestal": "Quartz Pedestal",
               "honeycomb_pedestal": "Honeycomb Pedestal", "celestial_altar": "Celestial Altar",
               "stardust": "Stardust", "partial_lava_cauldron": "Lava Cauldron"}


def box(frm, to, tex, faces=("north", "east", "south", "west", "up", "down")):
    return {"from": frm, "to": to, "faces": {f: {"texture": tex} for f in faces}}


def step3_blocks():
    bs, bm, im = (os.path.join(ASSETS, "blockstates"), os.path.join(ASSETS, "models", "block"),
                  os.path.join(ASSETS, "models", "item"))
    loot = os.path.join(DATA, "loot_tables", "blocks")
    drop = lambda item: {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": item}],
                                                                "conditions": [{"condition": "minecraft:survives_explosion"}]}]}
    # pedestals: one shared shape, textures per material
    write_json(os.path.join(bm, "pedestal.json"), {"render_type": "minecraft:cutout", "textures": {"particle": "#column"},
        "elements": [box([2, 0, 2], [14, 3, 14], "#base"), box([4, 3, 4], [12, 11, 12], "#column", ("north", "east", "south", "west")),
                     box([2, 11, 2], [14, 13, 14], "#top")]})
    write_json(os.path.join(bm, "celestial_altar.json"), {"render_type": "minecraft:cutout",   # PLACEHOLDER look
        "textures": {"particle": "minecraft:block/crying_obsidian", "base": "minecraft:block/polished_blackstone",
                     "trim": "minecraft:block/gold_block", "column": "minecraft:block/crying_obsidian",
                     "top": "minecraft:block/diamond_block"},
        "elements": [box([0, 0, 0], [16, 3, 16], "#base"), box([0, 3, 0], [16, 4, 16], "#trim"),
                     box([3, 4, 3], [13, 13, 13], "#column", ("north", "east", "south", "west")),
                     box([1, 13, 1], [15, 14, 15], "#trim"), box([1, 14, 1], [15, 16, 15], "#top")]})
    for pid, (top, col, base, (slab, mid, bottom)) in PEDESTALS.items():
        write_json(os.path.join(bm, pid + ".json"), {"parent": "bubbasbees:block/pedestal",
                                                     "textures": {"top": top, "column": col, "base": base}})
        write_json(os.path.join(DATA, "recipes", "blocks", pid + ".json"),
                   {"type": "minecraft:crafting_shaped", "pattern": ["S", "M", "B"],
                    "key": {"S": {"item": "minecraft:" + slab}, "M": {"item": "minecraft:" + mid}, "B": {"item": "minecraft:" + bottom}},
                    "result": {"item": "bubbasbees:" + pid, "count": 2}})
    for pid in list(PEDESTALS) + ["celestial_altar"]:
        write_json(os.path.join(bs, pid + ".json"), {"variants": {"": {"model": "bubbasbees:block/" + pid}}})
        write_json(os.path.join(im, pid + ".json"), {"parent": "bubbasbees:block/" + pid})
        write_json(os.path.join(loot, pid + ".json"), drop("bubbasbees:" + pid))
    write_json(os.path.join(DATA, "recipes", "blocks", "celestial_altar.json"),
               shaped(["v:crying_obsidian", "v:diamond", "v:crying_obsidian", "v:diamond", "x:honeycomb_pedestal",
                       "v:diamond", "v:crying_obsidian", "v:diamond", "v:crying_obsidian"], "bubbasbees:celestial_altar"))
    write_json(os.path.join(DATA, "recipes", "blocks", "comb_chute.json"),
               shaped(["v:iron_ingot", "v:honeycomb", "v:iron_ingot", "v:honeycomb", "v:hopper", "v:honeycomb",
                       "v:iron_ingot", "v:honeycomb", "v:iron_ingot"], "bubbasbees:comb_chute"))
    # stardust: redstone-style dot + arms (art final: BubbasBeesArt/work/stardust/final), pile drawn by a renderer
    write_json(os.path.join(bm, "stardust_dot.json"), {"render_type": "minecraft:cutout", "ambientocclusion": False,
        "textures": {"particle": "bubbasbees:block/stardust_dot", "dot": "bubbasbees:block/stardust_dot"},
        "elements": [{"from": [0, 0.25, 0], "to": [16, 0.25, 16], "shade": False,
                      "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#dot"}, "down": {"uv": [0, 16, 16, 0], "texture": "#dot"}}}]})
    write_json(os.path.join(bm, "stardust_arm.json"), {"render_type": "minecraft:cutout", "ambientocclusion": False,
        "textures": {"particle": "bubbasbees:block/stardust_dot", "line": "bubbasbees:block/stardust_line"},
        "elements": [{"from": [8, 0.25, 0], "to": [16, 0.25, 16], "shade": False,
                      "faces": {"up": {"uv": [8, 0, 16, 16], "texture": "#line"}, "down": {"uv": [8, 16, 16, 0], "texture": "#line"}}}]})
    parts = [{"apply": {"model": "bubbasbees:block/stardust_dot"}}]
    for d, y in (("east", 0), ("south", 90), ("west", 180), ("north", 270)):
        part = {"when": {d: "side|rod"}, "apply": {"model": "bubbasbees:block/stardust_arm"}}
        if y: part["apply"]["y"] = y
        parts.append(part)
    write_json(os.path.join(bs, "stardust.json"), {"multipart": parts})
    write_json(os.path.join(loot, "stardust.json"), drop("bubbasbees:stardust"))
    # partial lava cauldron: the vanilla level-1 template with the lava surface at each quarter
    with zipfile.ZipFile(VANILLA_JAR) as jar:
        tmpl = json.loads(jar.read("assets/minecraft/models/block/template_cauldron_level1.json"))
    for lvl in (1, 2, 3):
        model = json.loads(json.dumps(tmpl))
        y = 4 + 11 * lvl / 4
        for el in model["elements"]:
            if any(f.get("texture") == "#content" for f in el["faces"].values()):
                el["from"][1] = el["to"][1] = y
        model["textures"].update({"content": "minecraft:block/lava_still", "particle": "minecraft:block/cauldron_side"})
        write_json(os.path.join(bm, f"partial_lava_cauldron_level{lvl}.json"), model)
    write_json(os.path.join(bs, "partial_lava_cauldron.json"),
               {"variants": {f"level={l}": {"model": f"bubbasbees:block/partial_lava_cauldron_level{l}"} for l in (1, 2, 3)}})
    write_json(os.path.join(loot, "partial_lava_cauldron.json"), drop("minecraft:cauldron"))
    write_json(os.path.join(im, "comb_chute.json"), {"parent": "item/generated", "textures": {"layer0": "bubbasbees:item/comb_chute"}})
    write_json(os.path.join(RES, "data", "minecraft", "tags", "blocks", "mineable", "pickaxe.json"),
               {"replace": False, "values": ["bubbasbees:" + p for p in ("stone_pedestal", "deepslate_pedestal",
                "blackstone_pedestal", "quartz_pedestal", "celestial_altar", "partial_lava_cauldron")]})
    path = os.path.join(ASSETS, "lang", "en_us.json")
    lang = json.load(open(path))
    for b, n in BLOCK_NAMES.items():
        lang[f"block.bubbasbees.{b}"] = n
    lang.update({"item.bubbasbees.comb_chute": "Comb Chute",
                 "message.bubbasbees.comb_chute.fitted": "Comb chute fitted: hoppers can pull combs out",
                 "message.bubbasbees.comb_chute.removed": "Comb chute removed",
                 "gui.bubbasbees.hive_info.with_chute": "%s · comb chute"})
    write_json(path, lang)


def step3_art(work):
    """Final stardust art (item + placed dust) and PLACEHOLDER pile textures / comb chute icon."""
    from PIL import Image
    import random
    fin = os.path.join(work, "stardust", "final")
    items, blocks = os.path.join(ASSETS, "textures", "item"), os.path.join(ASSETS, "textures", "block")
    os.makedirs(blocks, exist_ok=True)
    shutil.copyfile(os.path.join(fin, "stardust.png"), os.path.join(items, "stardust.png"))
    shutil.copyfile(os.path.join(fin, "stardust_dot.png"), os.path.join(blocks, "stardust_dot.png"))
    shutil.copyfile(os.path.join(fin, "stardust_line.png"), os.path.join(blocks, "stardust_line.png"))
    # pile colours from pile_pass4.py: tops LP/PU/BM + dimmed star, sides PU/PD/BM/BD + dimmed star
    LP, PU, PD, BM, BD = (113, 99, 170), (90, 74, 156), (62, 50, 116), (30, 50, 124), (21, 42, 104)
    STAR, STAR2 = (206, 208, 228), (190, 192, 218)
    rnd = random.Random(5)
    for name, cols, wts in (("stardust_pile_top", [LP, PU, BM, STAR], [4, 5, 3, 1]),
                            ("stardust_pile_side", [PU, PD, BM, BD, STAR2], [4, 3, 3, 2, 1])):
        im = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                im.putpixel((x, y), rnd.choices(cols, wts)[0] + (255,))
        im.save(os.path.join(blocks, name + ".png"))
    chute = os.path.join(items, "comb_chute.png")
    if not os.path.exists(chute):
        with zipfile.ZipFile(VANILLA_JAR) as jar:
            hopper = Image.open(BytesIO(jar.read("assets/minecraft/textures/item/hopper.png"))).convert("RGBA")
        tint(hopper, (232, 170, 60)).save(chute)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--art", help="BubbasBeesArt/work: copy final bee/comb art and make placeholders")
    args = ap.parse_args()
    ids = spec_ids()
    missing = [b for b in ids if b not in ROSTER]
    extra = [b for b in ROSTER if b not in ids]
    if missing or extra:
        sys.exit(f"BeeSpecs.java and ROSTER differ: missing {missing}, extra {extra}")
    snapshot = json.load(open(os.path.join(ROOT, "tools", "recipe_snapshot.json")))
    combs = comb_ids()
    made = recipes(snapshot, vanilla_items())
    names(combs, ids)
    item_models(combs, ids)
    tags(ids)
    step3_blocks()
    if args.art:
        art(os.path.abspath(args.art), combs, ids)
        step3_art(os.path.abspath(args.art))
    print(len(ids), "bees,", len(combs), "combs,", len(made), "recipes")


if __name__ == "__main__":
    main()
