"""Write the bb_art review display into the test instance's world (datapack bb_art: /function bb_art:show, bb_art:cleanup).

One column per bee look in roster order (deepslate looks right after their stone look), 10 columns per lane:
filled jar in an item frame at the back, angry bee in the middle, named calm bee in front. Dark bee to the left.
Usage: python3 tools/gen_art_display.py <world save folder of the test instance>   (then /reload in game)
The world folder can also be given in the BB_TEST_WORLD environment variable.
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPECS_JAVA = os.path.join(ROOT, "src/main/java/net/bubba/bubbasbees/bees/BeeSpecs.java")
WORLD = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("BB_TEST_WORLD")
if not WORLD:
    sys.exit("Give the test instance's world save folder as the first argument, or set BB_TEST_WORLD.")
PACK = os.path.join(os.path.expanduser(WORLD), "datapacks", "bb_art")
BASE = 'NoAI:1b,NoGravity:1b,PersistenceRequired:1b,Invulnerable:1b,Silent:1b,Tags:["bbart"],Rotation:[0f,0f]'
CLEAR = ["kill @e[tag=bbart]", "fill 98 -60 20 135 -50 72 air"]


def entries():
    java = open(SPECS_JAVA).read()
    out = []
    for m in re.finditer(r'BeeSpec\.of\("([a-z_]+)"(.*?)\.build\(\)', java, re.S):
        out.append((m.group(1), False))
        if ".deepslateLook()" in m.group(2):
            out.append((m.group(1), True))
    return out


def main():
    looks = entries()
    show = CLEAR + ["time set 6000"]
    for i, (bee, deep) in enumerate(looks):
        lane, col = divmod(i, 10)
        x, zb = 102 + 3 * col, 61 - 9 * lane
        ds = "1b" if deep else "0b"
        name = bee + (" deepslate" if deep else "")
        jar = f'{{id:"bubbasbees:bee_jar",Count:1b,tag:{{BeeData:{{EntityType:"bubbasbees:{bee}_bee",BubbasDeepslate:{ds}}}}}}}'
        show += [f"setblock {x} -58 {zb} smooth_quartz",
                 f'summon item_frame {x} -58 {zb + 1} {{Facing:3b,Fixed:1b,Invulnerable:1b,Tags:["bbart"],Item:{jar}}}',
                 f"summon bubbasbees:{bee}_bee {x + .5} -58.5 {zb + 3.5} {{{BASE},BubbasDeepslate:{ds},AngerTime:999999}}",
                 f"summon bubbasbees:{bee}_bee {x + .5} -58.5 {zb + 5.5} "
                 f"{{{BASE},BubbasDeepslate:{ds},CustomName:'\"{name}\"',CustomNameVisible:1b}}"]
    show.append(f"summon bubbasbees:dark_bee 99.5 -58.5 66.5 {{{BASE},CustomName:'\"dark bee\"',CustomNameVisible:1b}}")
    show.append("tp @s 116 -59 72 180 15")
    funcs = os.path.join(PACK, "data", "bb_art", "functions")
    os.makedirs(funcs, exist_ok=True)
    json.dump({"pack": {"pack_format": 10, "description": "BubbasBees art review display (builder)"}},
              open(os.path.join(PACK, "pack.mcmeta"), "w"))
    open(os.path.join(funcs, "show.mcfunction"), "w").write("\n".join(show) + "\n")
    open(os.path.join(funcs, "cleanup.mcfunction"), "w").write("\n".join(CLEAR) + "\n")
    print(len(looks), "looks,", len(show), "commands")


if __name__ == "__main__":
    main()
