#!/usr/bin/env python3
"""Generates the bee roster's data and art files from one table (Phase 2 step 2).

  python3 tools/gen_bee_data.py [--art <BubbasBeesArt/work>]

Writes (overwriting only what it owns):
  data/bubbasbees/recipes/{combs,gems,stars}/...   every comb, gem and star recipe (from tools/recipe_snapshot.json,
                                                   with the balance-pass counts below and the colour families expanded)
  data/bubbasbees/tags/blocks/bee_flowers/<bee>.json, data/minecraft/tags/entity_types/beehive_inhabitors.json
  assets/bubbasbees/lang/en_us.json                 (bee/comb/gem/egg/new-item names merged in)
  assets/bubbasbees/models/item/...                 comb, gem, spawn egg, stardust, royal jelly, filled bee jar models
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
ART_WORK = os.path.join(os.path.dirname(ROOT), "BubbasBeesArt", "work")
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
# Ore recipes, 8 combs + 1 ore -> n (final balance, user 2026-11-07): coal net 4, iron/gold net 2,
# copper/redstone/quartz net 2; lapis, emerald, diamond and ancient debris stay at 2 (net 1)
ORE_COUNTS = {"coal": 5, "iron": 3, "gold": 3, "copper": 3, "redstone": 3, "quartz": 3}

# Colour families: the snapshot holds one example per family; (bee, label) -> (example seed, [(seed, output)])
FAMILIES = {
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
        if bee in SKIP_BEES or bee not in ROSTER or (bee, label) in SKIP_RECIPES:
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
            n = count
            if res != "minecraft:gilded_blackstone":   # not an ore recipe (5 combs + 4 blackstone -> 2)
                n = ORE_COUNTS.get(bee, n)
            made.append((f"combs/{bee}/{res.split(':')[1]}", shaped(grid, res, n)))
    for bee, g in snapshot["gems"].items():
        if bee in SKIP_BEES or bee not in ROSTER:
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
            # Original bees keep their committed looks; only matched angry replacements from angry/old_final come in
            for name in (f"{bee}_bee", f"{bee}_deepslate_bee"):
                angry = os.path.join(work, "angry", "old_final", name + "_angry.png")
                if os.path.exists(angry):
                    shutil.copyfile(angry, os.path.join(ent, name + "_angry.png"))
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
            # Angry look: the final from angry/new_final when there is one, else the normal look
            angry = os.path.join(work, "angry", "new_final", name + "_angry.png")
            shutil.copyfile(angry if os.path.exists(angry) else tex, os.path.join(ent, name + "_angry.png"))
        shutil.copyfile(model, os.path.join(geo, bee + ".geo.json"))
        copied.append(bee)
    # Starlight's angry eyes overlay (the calm overlay is committed by hand: never copy starsky/out's retired E2)
    overlay = os.path.join(work, "angry", "new_final", "starlight_bee_overlay_angry.png")
    if os.path.exists(overlay):
        shutil.copyfile(overlay, os.path.join(ent, "starlight_bee_overlay_angry.png"))
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
PEDESTALS = {  # id: (top, column, base, acc) textures (final art: BubbasBeesArt/work/pedestals/final, shape PB),
    # recipe column (top slab, middle, bottom block). The honeycomb pedestal was removed (user, 2026-11-02).
    "stone_pedestal": ("minecraft:block/smooth_stone", "minecraft:block/stone_bricks", "minecraft:block/stone",
                       "minecraft:block/chiseled_stone_bricks", ("stone_slab", "stone_brick_wall", "stone")),
    "deepslate_pedestal": ("minecraft:block/polished_deepslate", "minecraft:block/deepslate_bricks", "minecraft:block/deepslate",
                           "minecraft:block/chiseled_deepslate", ("polished_deepslate_slab", "polished_deepslate_wall", "deepslate")),
    "blackstone_pedestal": ("minecraft:block/polished_blackstone", "minecraft:block/polished_blackstone_bricks",
                            "minecraft:block/blackstone", "minecraft:block/chiseled_polished_blackstone",
                            ("blackstone_slab", "blackstone_wall", "polished_blackstone")),
    "quartz_pedestal": ("minecraft:block/quartz_block_top", "minecraft:block/quartz_pillar", "minecraft:block/quartz_block_side",
                        "minecraft:block/chiseled_quartz_block", ("quartz_slab", "quartz_pillar", "quartz_block")),
}
BLOCK_NAMES = {"stone_pedestal": "Stone Pedestal", "deepslate_pedestal": "Deepslate Pedestal",
               "blackstone_pedestal": "Blackstone Pedestal", "quartz_pedestal": "Quartz Pedestal",
               "celestial_altar": "Celestial Altar",
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
    fin = os.path.join(ART_WORK, "pedestals", "final")
    for model in ("pedestal.json", "celestial_altar.json"):   # final shapes (PB column, AE altar)
        write_json(os.path.join(bm, model), json.load(open(os.path.join(fin, model))))
    for pid, (top, col, base, acc, (slab, mid, bottom)) in PEDESTALS.items():
        write_json(os.path.join(bm, pid + ".json"), {"parent": "bubbasbees:block/pedestal",
                                                     "textures": {"top": top, "column": col, "base": base, "acc": acc}})
        write_json(os.path.join(DATA, "recipes", "blocks", pid + ".json"),
                   {"type": "minecraft:crafting_shaped", "pattern": ["S", "M", "B"],
                    "key": {"S": {"item": "minecraft:" + slab}, "M": {"item": "minecraft:" + mid}, "B": {"item": "minecraft:" + bottom}},
                    "result": {"item": "bubbasbees:" + pid, "count": 2}})
    for pid in list(PEDESTALS) + ["celestial_altar"]:
        write_json(os.path.join(bs, pid + ".json"), {"variants": {"": {"model": "bubbasbees:block/" + pid}}})
        write_json(os.path.join(im, pid + ".json"), {"parent": "bubbasbees:block/" + pid})
        write_json(os.path.join(loot, pid + ".json"), drop("bubbasbees:" + pid))
    write_json(os.path.join(DATA, "recipes", "blocks", "celestial_altar.json"),
               shaped(["v:crying_obsidian", "v:diamond", "v:crying_obsidian", "v:diamond", "x:quartz_pedestal",
                       "v:diamond", "v:crying_obsidian", "v:diamond", "v:crying_obsidian"], "bubbasbees:celestial_altar"))
    # comb chute: a block under the hive since 2026-11-02 (user); shears in the recipe
    write_json(os.path.join(DATA, "recipes", "blocks", "comb_chute.json"),
               shaped(["v:iron_ingot", "v:shears", "v:iron_ingot", "v:honeycomb", "v:hopper", "v:honeycomb",
                       "v:iron_ingot", "v:honeycomb", "v:iron_ingot"], "bubbasbees:comb_chute"))
    # block models: final art W1 (BubbasBeesArt/work/comb_chute/block/final), copied in step3_art
    write_json(os.path.join(bs, "comb_chute.json"), {"variants": {
        "honey=false": {"model": "bubbasbees:block/comb_chute"}, "honey=true": {"model": "bubbasbees:block/comb_chute_honey"}}})
    write_json(os.path.join(loot, "comb_chute.json"), drop("bubbasbees:comb_chute"))
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
                "blackstone_pedestal", "quartz_pedestal", "celestial_altar", "partial_lava_cauldron", "comb_chute")]})
    path = os.path.join(ASSETS, "lang", "en_us.json")
    lang = json.load(open(path))
    for b, n in BLOCK_NAMES.items():
        lang[f"block.bubbasbees.{b}"] = n
    lang.update({"item.bubbasbees.comb_chute": "Comb Chute",
                 "block.bubbasbees.comb_chute": "Comb Chute",
                 "message.bubbasbees.comb_chute.comb_mode": "Comb chute: combs",
                 "message.bubbasbees.comb_chute.honey_mode": "Comb chute: honey bottles (needs glass bottles)",
                 "message.bubbasbees.comb_chute.bottles": "Comb chute: %s glass bottles"})
    for old_key in ("message.bubbasbees.comb_chute.fitted", "message.bubbasbees.comb_chute.removed",
                    "gui.bubbasbees.hive_info.with_chute"):
        lang.pop(old_key, None)
    write_json(path, lang)


def step3_art(work):
    """Final stardust art (item + placed dust), final comb chute icon, PLACEHOLDER pile textures."""
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
    # comb chute icon: final art (BubbasBeesArt/work/comb_chute/final)
    shutil.copyfile(os.path.join(work, "comb_chute", "final", "comb_chute.png"), os.path.join(items, "comb_chute.png"))
    # comb chute block: final art W1 (user pick 2026-11-02): 8 textures + 2 models
    cfin = os.path.join(work, "comb_chute", "block", "final")
    for f in sorted(os.listdir(os.path.join(cfin, "textures", "block"))):
        shutil.copyfile(os.path.join(cfin, "textures", "block", f), os.path.join(blocks, f))
    for f in ("comb_chute.json", "comb_chute_honey.json"):
        shutil.copyfile(os.path.join(cfin, "models", "block", f), os.path.join(ASSETS, "models", "block", f))


# ------------------------------------------------------------------ step 4: traits, queen, jelly, pink piston
HOME_BIOMES = {  # bee: biomes where its hive stays 20% shorter (bee cards in finish-spec.md)
    "forest": ["forest", "birch_forest", "old_growth_birch_forest", "dark_forest"],
    "desert": ["desert"],
    "river": ["river"],
    "meadow": ["meadow", "flower_forest"],
    "jungle": ["jungle", "bamboo_jungle", "sparse_jungle"],
    "taiga": ["taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga", "snowy_taiga"],
    "savanna": ["savanna", "savanna_plateau", "windswept_savanna"],
    "deep_dark": ["deep_dark"],
    "lush_caves": ["lush_caves"],
    "frost": ["snowy_plains", "ice_spikes", "snowy_slopes", "frozen_peaks", "frozen_river", "frozen_ocean",
              "deep_frozen_ocean"],
    "swamp": ["swamp", "mangrove_swamp"],
    "mesa": ["badlands", "eroded_badlands", "wooded_badlands"],
    "mushroom_fields": ["mushroom_fields"],
    "sea": ["beach", "snowy_beach", "#minecraft:is_ocean"],
    "coral": ["warm_ocean"],
    "prismarine": ["deep_ocean", "deep_cold_ocean", "deep_lukewarm_ocean", "deep_frozen_ocean"],
    "corrupt_stone": ["nether_wastes", "basalt_deltas"],
    "soul": ["soul_sand_valley"],
    "corrupt_nature": ["crimson_forest", "warped_forest"],
    "magma": ["basalt_deltas", "nether_wastes"],
    "ender": ["#minecraft:is_end"],
}


def step4_data():
    bs, bm, im = (os.path.join(ASSETS, "blockstates"), os.path.join(ASSETS, "models", "block"),
                  os.path.join(ASSETS, "models", "item"))
    loot = os.path.join(DATA, "loot_tables", "blocks")
    for bee, biomes in HOME_BIOMES.items():
        write_json(os.path.join(DATA, "tags", "worldgen", "biome", "home_biomes", bee + ".json"),
                   {"replace": False, "values": [b if b.startswith("#") else "minecraft:" + b for b in biomes]})
    drop = lambda item: {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": item}],
                                                                "conditions": [{"condition": "minecraft:survives_explosion"}]}]}
    # royal jelly block: slime-block layout (inner core px 3-13 + translucent outer shell), final art B1
    faces = lambda uv, tex, cull: {f: dict({"uv": uv, "texture": tex}, **({"cullface": f} if cull else {}))
                                   for f in ("down", "up", "north", "south", "west", "east")}
    write_json(os.path.join(bm, "royal_jelly_block.json"), {"parent": "block/block", "render_type": "minecraft:translucent",
        "textures": {"particle": "bubbasbees:block/royal_jelly_block", "outer": "bubbasbees:block/royal_jelly_block",
                     "inner": "bubbasbees:block/royal_jelly_block_inner"},
        "elements": [{"from": [3, 3, 3], "to": [13, 13, 13], "faces": faces([3, 3, 13, 13], "#inner", False)},
                     {"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces([0, 0, 16, 16], "#outer", True)}]
                    # the shell again, facing inward, so the block still draws with the camera inside it
                    + [{"from": a, "to": b, "faces": {face: {"uv": [0, 0, 16, 16], "texture": "#outer", "cullface": side}}}
                       for side, face, a, b in (
                           ("north", "south", [0, 0, 0.02], [16, 16, 0.02]), ("south", "north", [0, 0, 15.98], [16, 16, 15.98]),
                           ("west", "east", [0.02, 0, 0], [0.02, 16, 16]), ("east", "west", [15.98, 0, 0], [15.98, 16, 16]),
                           ("down", "up", [0, 0.02, 0], [16, 0.02, 16]), ("up", "down", [0, 15.98, 0], [16, 15.98, 16]))]})
    write_json(os.path.join(bs, "royal_jelly_block.json"), {"variants": {"": {"model": "bubbasbees:block/royal_jelly_block"}}})
    write_json(os.path.join(im, "royal_jelly_block.json"), {"parent": "bubbasbees:block/royal_jelly_block"})
    write_json(os.path.join(loot, "royal_jelly_block.json"), drop("bubbasbees:royal_jelly_block"))
    rec = os.path.join(DATA, "recipes", "blocks")
    write_json(os.path.join(rec, "royal_jelly_block.json"), shaped(["x:royal_jelly"] * 9, "bubbasbees:royal_jelly_block"))
    write_json(os.path.join(rec, "royal_jelly_from_block.json"), {"type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": "bubbasbees:royal_jelly_block"}], "result": {"item": "bubbasbees:royal_jelly", "count": 9}})
    # pink sticky piston: vanilla sticky piston models with the pink face (final art V1); its own head block
    with zipfile.ZipFile(VANILLA_JAR) as jar:
        sticky = json.loads(jar.read("assets/minecraft/blockstates/sticky_piston.json"))
        head = json.loads(jar.read("assets/minecraft/blockstates/piston_head.json"))
    for v in sticky["variants"].values():
        if v["model"] == "minecraft:block/sticky_piston":
            v["model"] = "bubbasbees:block/pink_sticky_piston"
    write_json(os.path.join(bs, "pink_sticky_piston.json"), sticky)
    for key, v in head["variants"].items():   # the pink head is always sticky
        v["model"] = "bubbasbees:block/pink_piston_head" + ("_short" if "short=true" in key else "")
    write_json(os.path.join(bs, "pink_piston_head.json"), head)
    pink = "bubbasbees:block/pink_piston_top_sticky"
    write_json(os.path.join(bm, "pink_sticky_piston.json"), {"parent": "minecraft:block/template_piston",
        "textures": {"bottom": "minecraft:block/piston_bottom", "platform": pink, "side": "minecraft:block/piston_side"}})
    for short in ("", "_short"):
        write_json(os.path.join(bm, f"pink_piston_head{short}.json"), {"parent": f"minecraft:block/template_piston_head{short}",
            "textures": {"platform": pink, "side": "minecraft:block/piston_side", "unsticky": "minecraft:block/piston_top"}})
    write_json(os.path.join(bm, "pink_sticky_piston_inventory.json"), {"parent": "minecraft:block/cube_bottom_top",
        "textures": {"bottom": "minecraft:block/piston_bottom", "side": "minecraft:block/piston_side", "top": pink}})
    write_json(os.path.join(im, "pink_sticky_piston.json"), {"parent": "bubbasbees:block/pink_sticky_piston_inventory"})
    write_json(os.path.join(loot, "pink_sticky_piston.json"), drop("bubbasbees:pink_sticky_piston"))
    write_json(os.path.join(rec, "pink_sticky_piston.json"), {"type": "minecraft:crafting_shaped", "pattern": ["J", "P"],
        "key": {"J": {"item": "bubbasbees:royal_jelly"}, "P": {"item": "minecraft:piston"}},
        "result": {"item": "bubbasbees:pink_sticky_piston"}})
    write_json(os.path.join(im, "celestial_bubba_star.json"), {"parent": "item/generated",
        "textures": {"layer0": "bubbasbees:item/celestial_bubba_star"}})
    write_json(os.path.join(bs, "bee_light.json"), {"variants": {"": {"model": "minecraft:block/air"}}})
    # royal jelly counts as a slimeball for other mods' recipes
    write_json(os.path.join(RES, "data", "forge", "tags", "items", "slimeballs.json"),
               {"replace": False, "values": ["bubbasbees:royal_jelly"]})
    pick = os.path.join(RES, "data", "minecraft", "tags", "blocks", "mineable", "pickaxe.json")
    tag = json.load(open(pick))
    if "bubbasbees:pink_sticky_piston" not in tag["values"]:
        tag["values"].append("bubbasbees:pink_sticky_piston")
    write_json(pick, tag)
    path = os.path.join(ASSETS, "lang", "en_us.json")
    lang = json.load(open(path))
    lang.update({"block.bubbasbees.royal_jelly_block": "Royal Jelly Block",
                 "block.bubbasbees.pink_sticky_piston": "Pink Sticky Piston",
                 "block.bubbasbees.pink_piston_head": "Pink Piston Head",
                 "block.bubbasbees.bee_light": "Bee Light",
                 "entity.bubbasbees.dark_bee": "Dark Bee",
                 "message.bubbasbees.queen_fallen": "The Royal Queen has fallen"})
    write_json(path, lang)


# ------------------------------------------------------------------ step 5: rituals
def step5_data():
    """Ritual boss bar, chat and structure-hint text (RitualManager / RitualStructure)."""
    path = os.path.join(ASSETS, "lang", "en_us.json")
    lang = json.load(open(path))
    r = "ritual.bubbasbees."
    lang.update({
        r + "night": "Celestial Ritual", r + "queen": "Royal Summoning",
        r + "total": "Pollinations %s/%s", r + "starlight": "Starlight %s/%s", r + "guards": "Royal guards %s/%s",
        r + "types": "Types %s/%s", r + "missing_types": "Missing: %s", r + "time_left": "%s left",
        r + "paused": "Paused: relight the candle",
        r + "started.night": "The Celestial Ritual has begun. Finish before sunrise.",
        r + "started.queen": "The Royal Summoning has begun. You have 5 minutes.",
        r + "complete": "The ritual is complete",
        r + "complete.night": "The Bubba Star has become a Celestial Bubba Star!",
        r + "complete.queen": "The Royal Queen has arrived!",
        r + "failed": "The ritual has failed", r + "failed_because": "The ritual has failed. %s",
        r + "fail.unloaded": "Everyone left the circle.", r + "fail.item": "The altar's item was taken.",
        r + "fail.bee_killed": "A bee from the circle was killed.", r + "fail.sunrise": "The sun rose.",
        r + "fail.time": "Time ran out.",
        r + "problem.night": "The Celestial Ritual can only begin at night",
        r + "problem.sky": "The altar must be under open sky",
        r + "problem.day": "The Royal Summoning can only begin in daytime",
        r + "problem.queen_alive": "The Royal Queen is still alive",
        r + "problem.candles_lit": "Light every candle to begin",
        r + "problem.end_rod": "Needs an end rod in the circle, 8 blocks %s of the altar",
        r + "problem.corner_pedestal": "Needs a pedestal in the circle to the %s (6 out, 6 across)",
        r + "problem.echo_shard": "The %s pedestal needs an echo shard",
        r + "problem.ring_dust": "The stardust circle is missing a piece at %s, %s, %s",
        r + "problem.centre_candles": "Needs 4 candles next to the altar (%s found)",
        r + "problem.corner_candles": "Needs 2 candles within 2 blocks of the %s pedestal, inside the circle (%s found)",
        r + "problem.queen_base": "The altar must stand on 3x3 honeycomb blocks with gold block corners",
        r + "problem.flower_pot": "Needs a potted flower on the circle's edge to the %s",
        r + "problem.honey_block": "The %s flower pot must stand on a honey block",
    })
    for old in ("problem.rod_dust", "problem.circle"):
        lang.pop(r + old, None)
    for d in ("north", "east", "south", "west", "north_east", "south_east", "south_west", "north_west"):
        lang[r + "dir." + d] = d.replace("_", "-")
    write_json(path, lang)


def deepslate_ids():
    """Bees with a stone and a deepslate look (.deepslateLook() in BeeSpecs.java)."""
    java = open(SPECS_JAVA).read()
    return [m.group(1) for m in re.finditer(r'BeeSpec\.of\("([a-z_]+)"(.*?)\.build\(\)', java, re.S)
            if ".deepslateLook()" in m.group(2)]


def jar_names(ids):
    """(fill value, model/texture name) per filled jar, matching BeeJarItem.getFillState: 2 + roster index,
    +0.5 for a bee jarred in its deepslate look."""
    deep = set(deepslate_ids())
    out = []
    for i, bee in enumerate(ids):
        out.append((2.0 + i, "bee_jar_filled_" + bee))
        if bee in deep:
            out.append((2.5 + i, f"bee_jar_filled_{bee}_deepslate"))
    return out


def jar_models(ids):
    """Bee jar: one filled model per bee (and per deepslate look)."""
    models = os.path.join(ASSETS, "models", "item")
    overrides = [{"predicate": {"bubbasbees:filled": 1.0}, "model": "bubbasbees:item/bee_jar_filled"}]
    for value, name in jar_names(ids):
        write_json(os.path.join(models, name + ".json"),
                   {"parent": "item/generated", "textures": {"layer0": "bubbasbees:item/" + name}})
        overrides.append({"predicate": {"bubbasbees:filled": value}, "model": "bubbasbees:item/" + name})
    write_json(os.path.join(models, "bee_jar.json"),
               {"parent": "item/generated", "textures": {"layer0": "bubbasbees:item/bee_jar"}, "overrides": overrides})


def jar_art(work, ids):
    """Filled bee jars (art: eggs_jars/final_jars, one per roster bee; every jar shows the bee's real face)."""
    src = os.path.join(work, "eggs_jars", "final_jars")
    names = [n + ".png" for _, n in jar_names(ids)]
    missing = [n for n in names if not os.path.exists(os.path.join(src, n))]
    if missing:
        sys.exit(f"jar art missing: {missing}")
    for name in names:
        shutil.copyfile(os.path.join(src, name), os.path.join(ASSETS, "textures", "item", name))


def step4_art(work):
    """Final royal jelly art (item, block, pink piston face), the Celestial Bubba Star, and the dark bee (final art, else a placeholder)."""
    from PIL import Image
    fin = os.path.join(work, "royal_jelly", "final")
    items, blocks = os.path.join(ASSETS, "textures", "item"), os.path.join(ASSETS, "textures", "block")
    shutil.copyfile(os.path.join(fin, "royal_jelly.png"), os.path.join(items, "royal_jelly.png"))
    for name in ("royal_jelly_block", "royal_jelly_block_inner", "pink_piston_top_sticky"):
        shutil.copyfile(os.path.join(fin, name + ".png"), os.path.join(blocks, name + ".png"))
    # Conversion gems with final art (family batches; the rest keep their tinted placeholders)
    # gems/final = ore family, gems/biome_final = biome family (desert's sprout gem replaces the old sandy one,
    # which the user keeps as the crystalline template; it is still sandy_conversion_gem.png in the main repo)
    on_hold = ()   # gem names still under review by the user, if any (prefixes)
    # gems/batchA_final, batchB_final = the remaining families (recolours of the user's sandy, nature and stone gems)
    for family in ("final", "biome_final", "batchA_final", "batchB_final"):
        gems = os.path.join(work, "gems", family)
        if not os.path.isdir(gems):
            continue
        for name in sorted(os.listdir(gems)):
            if name.startswith(on_hold):
                continue
            if name.split("_conversion_gem")[0] not in ROSTER:
                continue    # gem of a bee no longer in the roster (concrete, deleted 2026-11-04)
            if name.endswith("_conversion_gem.png") or name.endswith("_conversion_gem.png.mcmeta"):
                shutil.copyfile(os.path.join(gems, name), os.path.join(items, name))
    # Celestial Bubba Star: final G3 (starlight recolour of the Bubba Star animation, 14 frames; glint from isFoil)
    star = os.path.join(work, "star", "celestial", "final")
    for name in ("celestial_bubba_star.png", "celestial_bubba_star.png.mcmeta"):
        shutil.copyfile(os.path.join(star, name), os.path.join(items, name))
    ent = os.path.join(ASSETS, "textures", "entity")
    dark = os.path.join(ent, "dark_bee.png")
    # Dark bee: final art (dark_bee/final, one texture, no angry look, no jar); placeholder only while it is missing
    dark_final = os.path.join(work, "dark_bee", "final", "dark_bee.png")
    if os.path.exists(dark_final):
        shutil.copyfile(dark_final, dark)
    elif not os.path.exists(dark):
        im = Image.open(os.path.join(ent, "stone_bee.png")).convert("RGBA")
        px = im.load()
        for y in range(im.height):
            for x in range(im.width):
                r, g, b, a = px[x, y]
                if a:
                    lum = (r + g + b) / 3
                    px[x, y] = (int(20 + lum * 0.10), int(14 + lum * 0.06), int(28 + lum * 0.14), 170)
        im.save(dark)


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
    step4_data()
    step5_data()
    jar_models(ids)
    if args.art:
        art(os.path.abspath(args.art), combs, ids)
        step3_art(os.path.abspath(args.art))
        step4_art(os.path.abspath(args.art))
        jar_art(os.path.abspath(args.art), ids)
    print(len(ids), "bees,", len(combs), "combs,", len(made), "recipes")


if __name__ == "__main__":
    main()
