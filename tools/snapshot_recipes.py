#!/usr/bin/env python3
"""Copies the final recipe data out of the art/recipe review scripts into tools/recipe_snapshot.json.

  python3 tools/snapshot_recipes.py <path to BubbasBeesArt/work/drafts>

The review scripts (owned by the art/recipes sessions) are the source of truth for grids; this snapshot pins what the
mod was built from. gen_bee_data.py turns it into recipe JSON (with the balance-pass counts applied there).
Tokens: "c:<comb>", "v:<vanilla id>", "x:<new mod item>", "s:lesser|bubba|ritual", "g:<bee>", null = empty slot.
"""
import json
import os
import sys

drafts = os.path.abspath(sys.argv[1])
sys.path.insert(0, drafts)
os.chdir(drafts)
import final_recipes as F   # noqa: E402
import recipe_gallery as R  # noqa: E402

combs = [{"bee": b, "label": l, "grid": g, "out": o, "count": n} for b, l, g, o, n in F.collect()]

# Gem grids as round3_finals() builds them (first page picks + round 1 + round 2 picks + OG1 ore gems)
gems = R.first_page_gems()
for bee, title, note, opts in R.ROUND1:
    if bee in R.R1_PICKS:
        gems[bee] = (R.R1_PICKS[bee], dict((k, g) for k, g, _ in opts)[R.R1_PICKS[bee]])
r2 = {"jungle": "JU2", "taiga": "TA2", "swamp": "SW2", "corrupt_nature": "CN2", "royal_guard": "RG4"}
for bee, title, note, opts in R.ROUND2:
    gems[bee] = (r2[bee], dict((k, g) for k, g, _ in opts)[r2[bee]])
for o in R.ORES:
    gems[o[0]] = ("OG1", R.ore_gem(*o))

out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "recipe_snapshot.json")
json.dump({"source": drafts, "combs": combs,
           "gems": {b: {"pick": k, "grid": g} for b, (k, g) in sorted(gems.items())}},
          open(out, "w"), indent=1)
print(out, len(combs), "comb recipes,", len(gems), "gems")
