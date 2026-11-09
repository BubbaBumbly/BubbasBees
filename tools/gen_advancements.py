#!/usr/bin/env python3
"""Writes Bubba's Bees' advancements (user 2026-11-08) and the tags they use, from the mod's own data.

  python3 tools/gen_advancements.py

Writes:
  data/bubbasbees/advancements/*.json                       the 11 advancements (tab background: honeycomb block)
  data/bubbasbees/tags/items/conversion_gems.json, ore_combs.json
  data/bubbasbees/tags/blocks/advanced_beehives.json        vanilla-wood hives (from the plain/reinforced item tags)
  compat/<mod>/data/bubbasbees/tags/blocks/advanced_beehives.json   each wood mod's hives
  assets/bubbasbees/lang/en_us.json                         advancements.bubbasbees.* keys merged in
Only vanilla triggers; the queen's uses minecraft:summoned_entity, fired by RitualManager.win.
"""
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'src', 'main', 'resources')
DATA = os.path.join(RES, 'data', 'bubbasbees')
NS = 'bubbasbees'


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def tag_values(path):
    return json.load(open(path))['values'] if os.path.exists(path) else []


# ---- roster: gem bees in BeeSpecs order, from the Java source (the single source of truth) ----
specs = open(os.path.join(ROOT, 'src/main/java/net/bubba/bubbasbees/bees/BeeSpecs.java')).read()
bees = re.findall(r'BeeSpec\.of\("([a-z_]+)", (LESSER|BUBBA|CELESTIAL|NONE)\)', specs)
gem_bees = [b for b, tier in bees if tier != 'NONE']
ORE_BEES = ['coal', 'copper', 'iron', 'gold', 'redstone', 'lapis', 'diamond', 'emerald', 'quartz', 'ancient_debris']

# ---- tags ----
write(os.path.join(DATA, 'tags/items/conversion_gems.json'),
      {'replace': False, 'values': [f'{NS}:{b}_conversion_gem' for b in gem_bees]})
write(os.path.join(DATA, 'tags/items/ore_combs.json'),
      {'replace': False, 'values': [f'{NS}:{b}_honeycomb' for b in ORE_BEES]})
hives = tag_values(os.path.join(DATA, 'tags/items/plain_hives.json')) + tag_values(os.path.join(DATA, 'tags/items/reinforced_hives.json'))
write(os.path.join(DATA, 'tags/blocks/advanced_beehives.json'), {'replace': False, 'values': hives})
for mod in sorted(os.listdir(os.path.join(RES, 'compat'))):
    t = os.path.join(RES, 'compat', mod, 'data', NS, 'tags', 'items')
    vals = tag_values(os.path.join(t, 'plain_hives.json')) + tag_values(os.path.join(t, 'reinforced_hives.json'))
    if vals:
        write(os.path.join(RES, 'compat', mod, 'data', NS, 'tags/blocks/advanced_beehives.json'), {'replace': False, 'values': vals})

# ---- advancements ----
LANG = {}


def adv(name, parent, icon, title, desc, criteria, frame='task', requirements=None, background=None):
    display = {'icon': {'item': icon}, 'title': {'translate': f'advancements.{NS}.{name}.title'},
               'description': {'translate': f'advancements.{NS}.{name}.description'}, 'frame': frame,
               'show_toast': True, 'announce_to_chat': True, 'hidden': False}
    if background:
        display['background'] = background
    out = {'display': display, 'criteria': criteria}
    if parent:
        out['parent'] = f'{NS}:{parent}'
    if requirements:
        out['requirements'] = requirements
    write(os.path.join(DATA, 'advancements', name + '.json'), out)
    LANG[f'advancements.{NS}.{name}.title'] = title
    LANG[f'advancements.{NS}.{name}.description'] = desc


def has(*items, tag=None):
    pred = {'tag': tag} if tag else {'items': list(items)}
    return {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [pred]}}


def used_on_bee(item, entity='minecraft:bee'):
    cond = {'item': {'items': [item]}}
    if entity:
        cond['entity'] = {'type': entity}
    return {'trigger': 'minecraft:player_interacted_with_entity', 'conditions': cond}


adv('root', None, f'{NS}:encyclobeedia', "Bubba's Bees", 'Get an Advanced Beehive or the Encyclobeedia',
    {'book': has(f'{NS}:encyclobeedia'), 'plain': has(tag=f'{NS}:plain_hives'), 'reinforced': has(tag=f'{NS}:reinforced_hives')},
    requirements=[['book', 'plain', 'reinforced']], background='minecraft:textures/block/honeycomb_block.png')
adv('place_hive', 'root', f'{NS}:advanced_beehive', 'Home Improvement', 'Place an Advanced Beehive',
    {'plain': {'trigger': 'minecraft:placed_block', 'conditions': {'item': {'tag': f'{NS}:plain_hives'}}},
     'reinforced': {'trigger': 'minecraft:placed_block', 'conditions': {'item': {'tag': f'{NS}:reinforced_hives'}}}},
    requirements=[['plain', 'reinforced']])
adv('comb_chute', 'place_hive', f'{NS}:comb_chute', 'Hands Free', 'Put a Comb Chute under a hive to harvest it for you',
    {'chute': {'trigger': 'minecraft:placed_block', 'conditions': {'item': {'items': [f'{NS}:comb_chute']}}}})
adv('bee_jar', 'place_hive', f'{NS}:bee_jar', 'Bee in a Bottle', 'Catch a bee in a Bee Jar',
    {'jar': used_on_bee(f'{NS}:bee_jar', entity=None)})
adv('lesser_star', 'place_hive', f'{NS}:lesser_bubba_star', 'Seeing Stars', 'Craft a Lesser Bubba Star',
    {'star': has(f'{NS}:lesser_bubba_star')})
adv('first_gem', 'lesser_star', f'{NS}:stone_conversion_gem', 'A Gem of a Bee', 'Turn a vanilla bee with a conversion gem',
    {'gem': {'trigger': 'minecraft:player_interacted_with_entity',
             'conditions': {'item': {'tag': f'{NS}:conversion_gems'}, 'entity': {'type': 'minecraft:bee'}}}})
adv('busy_hive', 'first_gem', f'{NS}:bubba_star', 'Busy Bees', 'Switch a hive between calm and busy with any Bubba Star',
    {'switch': {'trigger': 'minecraft:item_used_on_block',
                'conditions': {'location': {'block': {'tag': f'{NS}:advanced_beehives'}}, 'item': {'items': [f'{NS}:lesser_bubba_star', f'{NS}:bubba_star', f'{NS}:celestial_bubba_star']}}}})
adv('celestial_ritual', 'busy_hive', f'{NS}:celestial_bubba_star', 'Written in the Stars', 'Win the Celestial Ritual',
    {'star': has(f'{NS}:celestial_bubba_star')}, frame='goal')
adv('ore_comb', 'celestial_ritual', f'{NS}:iron_honeycomb', 'Honey, I Struck Ore', 'Get your first ore comb',
    {'comb': has(tag=f'{NS}:ore_combs')})
adv('diamond_comb', 'ore_comb', f'{NS}:diamond_honeycomb', 'Diamonds From the Sky', 'Get a diamond comb',
    {'comb': has(f'{NS}:diamond_honeycomb')}, frame='goal')
adv('royal_queen', 'first_gem', f'{NS}:royal_jelly', 'Long Live the Queen', 'Summon the Royal Queen',
    {'queen': {'trigger': 'minecraft:summoned_entity', 'conditions': {'entity': {'type': f'{NS}:royal_queen_bee'}}}},
    frame='challenge')
adv('collector', 'first_gem', f'{NS}:starlight_conversion_gem', 'Collector', 'Turn a vanilla bee into every kind of bee',
    {b: used_on_bee(f'{NS}:{b}_conversion_gem') for b in gem_bees}, frame='challenge')

lang_path = os.path.join(RES, 'assets', NS, 'lang', 'en_us.json')
raw = open(lang_path).read()
lang = json.loads(raw)
lang.update(LANG)
with open(lang_path, 'w') as f:
    f.write(json.dumps(lang, indent=2, ensure_ascii=False) + ('\n' if raw.endswith('\n') else ''))
print(f'{len(LANG) // 2} advancements, {len(gem_bees)} gems in Collector, {len(hives)} vanilla hives in the block tag')
