#!/usr/bin/env python3
"""Converts the mod's data files from 1.20.1 to the 1.21.1 layout (one-off helper for the 1.21.1 port).

  python3 tools/convert_data_121.py

Moves/rewrites the hand-written and generated data the generators do not own:
  recipes -> recipe, loot_tables -> loot_table, advancements -> advancement,
  tags/blocks -> tags/block, tags/items -> tags/item, tags/entity_types -> tags/entity_type,
  recipe results "item" -> "id", loot copy_nbt -> copy_components, silk-touch predicate -> predicates,
  forge:slimeballs -> c:slimeballs, and removes pack.mcmeta + META-INF/mods.toml.
Idempotent: a second run finds nothing left to move.
"""
import json
import os
import shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'src', 'main', 'resources')

COPY_NBT_COMPONENTS = {
    'Bees': 'minecraft:bees',
    'ModBeehiveCombList': 'bubbasbees:beehive_combs',
    'BubbasBusy': 'bubbasbees:hive_busy',
}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def convert_recipe(obj):
    result = obj.get('result')
    if isinstance(result, dict) and 'item' in result:
        result['id'] = result.pop('item')
    _convert_forge_types(obj)
    return obj


def _convert_forge_types(obj):
    """1.21 NeoForge registers its ingredient types under the neoforge: namespace."""
    if isinstance(obj, dict):
        if isinstance(obj.get('type'), str) and obj['type'].startswith('forge:'):
            obj['type'] = 'neoforge:' + obj['type'][len('forge:'):]
        for value in obj.values():
            _convert_forge_types(value)
    elif isinstance(obj, list):
        for value in obj:
            _convert_forge_types(value)


def convert_advancement(obj):
    """1.21 advancement display icons are ItemStacks, so "item" becomes "id"."""
    display = obj.get('display')
    if isinstance(display, dict):
        icon = display.get('icon')
        if isinstance(icon, dict) and 'item' in icon:
            icon['id'] = icon.pop('item')
    return obj


def convert_asset(path):
    """NeoForge 1.21 model loaders live under the neoforge: namespace."""
    changed = 0
    for dirpath, _dirnames, filenames in os.walk(path):
        for name in filenames:
            if not name.endswith('.json'):
                continue
            full = os.path.join(dirpath, name)
            with open(full) as f:
                data = json.load(f)
            loader = data.get('loader') if isinstance(data, dict) else None
            if isinstance(loader, str) and loader.startswith('forge:'):
                data['loader'] = 'neoforge:' + loader[len('forge:'):]
                write_json(full, data)
                changed += 1
    return changed


def convert_loot(obj):
    if isinstance(obj, dict):
        if obj.get('function') == 'minecraft:copy_nbt':
            include = []
            for op in obj.get('ops', []):
                mapped = COPY_NBT_COMPONENTS.get(op.get('source'))
                if mapped and mapped not in include:
                    include.append(mapped)
            obj.clear()
            obj['function'] = 'minecraft:copy_components'
            obj['source'] = 'block_entity'
            obj['include'] = include
        if obj.get('condition') == 'minecraft:match_tool' and isinstance(obj.get('predicate'), dict):
            predicate = obj['predicate']
            if 'enchantments' in predicate and 'predicates' not in predicate:
                entries = predicate.pop('enchantments')
                obj['predicate'] = {'predicates': {'minecraft:enchantments': entries}}
                predicate = obj['predicate']
            for entries in predicate.get('predicates', {}).values():
                if isinstance(entries, list):
                    for entry in entries:
                        if isinstance(entry, dict) and 'enchantment' in entry:
                            entry['enchantments'] = entry.pop('enchantment')
        for value in list(obj.values()):
            convert_loot(value)
    elif isinstance(obj, list):
        for value in obj:
            convert_loot(value)
    return obj


def move_tree(src, dst, convert):
    if not os.path.isdir(src):
        return 0
    moved = 0
    for dirpath, _dirnames, filenames in os.walk(src):
        rel = os.path.relpath(dirpath, src)
        for name in filenames:
            if not name.endswith('.json'):
                continue
            target = os.path.join(dst, rel, name)
            if os.path.exists(target):
                continue
            with open(os.path.join(dirpath, name)) as f:
                data = json.load(f)
            write_json(target, convert(data))
            moved += 1
    return moved


def move_dir(src, dst):
    if not os.path.isdir(src):
        return 0
    moved = 0
    for dirpath, _dirnames, filenames in os.walk(src):
        rel = os.path.relpath(dirpath, src)
        for name in filenames:
            target = os.path.join(dst, rel, name)
            if os.path.exists(target):
                continue
            os.makedirs(os.path.dirname(target), exist_ok=True)
            shutil.copyfile(os.path.join(dirpath, name), target)
            moved += 1
    return moved


def remove(path):
    if os.path.isdir(path):
        shutil.rmtree(path)
    elif os.path.exists(path):
        os.remove(path)


def main():
    data = os.path.join(RES, 'data', 'bubbasbees')
    converted = 0

    converted += move_tree(os.path.join(data, 'recipes'), os.path.join(data, 'recipe'), convert_recipe)
    converted += move_tree(os.path.join(data, 'loot_tables'), os.path.join(data, 'loot_table'), convert_loot)
    converted += move_tree(os.path.join(data, 'advancements'), os.path.join(data, 'advancement'), convert_advancement)
    converted += move_tree(os.path.join(data, 'tags', 'blocks'), os.path.join(data, 'tags', 'block'), lambda obj: obj)
    converted += move_tree(os.path.join(data, 'tags', 'items'), os.path.join(data, 'tags', 'item'), lambda obj: obj)
    converted += move_tree(os.path.join(data, 'tags', 'entity_types'), os.path.join(data, 'tags', 'entity_type'), lambda obj: obj)

    minecraft = os.path.join(RES, 'data', 'minecraft', 'tags')
    converted += move_tree(os.path.join(minecraft, 'blocks'), os.path.join(minecraft, 'block'), lambda obj: obj)
    converted += move_tree(os.path.join(minecraft, 'entity_types'), os.path.join(minecraft, 'entity_type'), lambda obj: obj)

    forge_slime = os.path.join(RES, 'data', 'forge', 'tags', 'items', 'slimeballs.json')
    if os.path.exists(forge_slime):
        write_json(os.path.join(RES, 'data', 'c', 'tags', 'item', 'slimeballs.json'),
                   json.load(open(forge_slime)))
        converted += 1

    # In-place pass for files already generated in the 1.21 folder layout
    recipe_root = os.path.join(data, 'recipe')
    if os.path.isdir(recipe_root):
        for dirpath, _dirnames, filenames in os.walk(recipe_root):
            for name in filenames:
                if name.endswith('.json'):
                    full = os.path.join(dirpath, name)
                    data_obj = json.load(open(full))
                    before = json.dumps(data_obj, sort_keys=True)
                    convert_recipe(data_obj)
                    if json.dumps(data_obj, sort_keys=True) != before:
                        write_json(full, data_obj)
                        converted += 1

    converted += convert_asset(os.path.join(RES, 'assets'))

    # In-place pass for loot tables already generated in the 1.21 folder layout
    loot_root = os.path.join(data, 'loot_table')
    if os.path.isdir(loot_root):
        for dirpath, _dirnames, filenames in os.walk(loot_root):
            for name in filenames:
                if name.endswith('.json'):
                    full = os.path.join(dirpath, name)
                    data_obj = json.load(open(full))
                    before = json.dumps(data_obj, sort_keys=True)
                    convert_loot(data_obj)
                    if json.dumps(data_obj, sort_keys=True) != before:
                        write_json(full, data_obj)
                        converted += 1

    # In-place pass for advancements already generated in the 1.21 folder layout
    advancement_root = os.path.join(data, 'advancement')
    if os.path.isdir(advancement_root):
        for dirpath, _dirnames, filenames in os.walk(advancement_root):
            for name in filenames:
                if name.endswith('.json'):
                    full = os.path.join(dirpath, name)
                    data_obj = json.load(open(full))
                    before = json.dumps(data_obj, sort_keys=True)
                    convert_advancement(data_obj)
                    if json.dumps(data_obj, sort_keys=True) != before:
                        write_json(full, data_obj)
                        converted += 1

    # The old plural folders are empty after the moves (or only hold files already written in the new layout)
    for old in ('recipes', 'loot_tables', 'advancements'):
        remove(os.path.join(data, old))
    for old in ('blocks', 'items', 'entity_types'):
        remove(os.path.join(data, 'tags', old))
    remove(os.path.join(minecraft, 'blocks'))
    remove(os.path.join(minecraft, 'entity_types'))
    remove(os.path.join(RES, 'data', 'forge'))

    # NeoForge synthesizes pack.mcmeta and reads neoforge.mods.toml instead of mods.toml
    remove(os.path.join(RES, 'pack.mcmeta'))
    remove(os.path.join(RES, 'META-INF', 'mods.toml'))
    print(f'converted {converted} data files to the 1.21.1 layout')


if __name__ == '__main__':
    main()
