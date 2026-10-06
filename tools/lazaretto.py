#!/usr/bin/env python3
"""Builds the Lazaretto structure (data/bromaxpestilence/structure/lazaretto.nbt) and its worldgen JSON.

A ruined plague hospital: a stone-brick ward of beds (bodies on some of them), a dispensary, a
barred-window shell under a flat roof, and a stairwell down to the deepslate crypt where the
Crypt Seal holds the Gravewarden. Template y 0-6 is the crypt (buried); y 7 is the ground floor.

    python3 tools/lazaretto.py
"""
import gzip
import io
import json
import os
import random
import struct

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, '..', 'src', 'main', 'resources', 'data', 'bromaxpestilence')
DATA_VERSION = 3955  # 1.21.1

# ---------------------------------------------------------------------------------------------
# Minimal NBT writer
# ---------------------------------------------------------------------------------------------

class Int(int): pass
class Byte(int): pass
class Double(float): pass
class Float(float): pass


def _tag_id(v):
    if isinstance(v, Byte): return 1
    if isinstance(v, Int) or isinstance(v, bool): return 3
    if isinstance(v, int): return 3
    if isinstance(v, Float): return 5
    if isinstance(v, (Double, float)): return 6
    if isinstance(v, str): return 8
    if isinstance(v, list): return 9
    if isinstance(v, dict): return 10
    raise TypeError(type(v))


def _write(out, v):
    t = _tag_id(v)
    if t == 1: out.write(struct.pack('>b', int(v)))
    elif t == 3: out.write(struct.pack('>i', int(v)))
    elif t == 5: out.write(struct.pack('>f', float(v)))
    elif t == 6: out.write(struct.pack('>d', float(v)))
    elif t == 8:
        b = v.encode('utf-8'); out.write(struct.pack('>H', len(b))); out.write(b)
    elif t == 9:
        inner = _tag_id(v[0]) if v else 0
        out.write(struct.pack('>bi', inner, len(v)))
        for x in v: _write(out, x)
    elif t == 10:
        for k, x in v.items():
            out.write(struct.pack('>b', _tag_id(x)))
            kb = k.encode('utf-8'); out.write(struct.pack('>H', len(kb))); out.write(kb)
            _write(out, x)
        out.write(b'\x00')


def write_nbt(path, root):
    buf = io.BytesIO()
    buf.write(b'\x0a\x00\x00')     # unnamed root compound
    _write(buf, root)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with gzip.open(path, 'wb') as f:
        f.write(buf.getvalue())


# ---------------------------------------------------------------------------------------------
# The building
# ---------------------------------------------------------------------------------------------

SX, SY, SZ = 25, 16, 22
FLOOR = 7


class Template:
    def __init__(self):
        self.blocks = {}
        self.entities = []

    def set(self, x, y, z, name, props=None, nbt=None):
        if 0 <= x < SX and 0 <= y < SY and 0 <= z < SZ:
            self.blocks[(x, y, z)] = (name, tuple(sorted((props or {}).items())), nbt)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, props)

    def corpse(self, x, y, z, source='minecraft:villager', yaw=0.0):
        self.entities.append({'pos': [Double(x), Double(y), Double(z)], 'blockPos': [Int(int(x)), Int(int(y)), Int(int(z))],
                              'nbt': {'id': 'bromaxpestilence:corpse', 'source': source, 'age': Int(0), 'Rotation': [Float(yaw), Float(0)],
                                      'Pos': [Double(x), Double(y), Double(z)]}})

    def save(self, path):
        palette, index = [], {}
        blocks = []
        for (x, y, z), (name, props, nbt) in sorted(self.blocks.items()):
            key = (name, props)
            if key not in index:
                index[key] = len(palette)
                entry = {'Name': name}
                if props:
                    entry['Properties'] = {k: str(v) for k, v in props}
                palette.append(entry)
            b = {'pos': [Int(x), Int(y), Int(z)], 'state': Int(index[key])}
            if nbt:
                b['nbt'] = nbt
            blocks.append(b)
        write_nbt(path, {'DataVersion': Int(DATA_VERSION), 'size': [Int(SX), Int(SY), Int(SZ)],
                         'palette': palette, 'blocks': blocks, 'entities': self.entities})


def build():
    rng = random.Random(1347)
    t = Template()

    def brick():
        r = rng.random()
        return 'minecraft:mossy_stone_bricks' if r < 0.2 else 'minecraft:cracked_stone_bricks' if r < 0.35 else 'minecraft:stone_bricks'

    # Everything outside the walls is left to the terrain (structure void); inside is cleared to air.
    for x in range(SX):
        for y in range(SY):
            for z in range(SZ):
                t.set(x, y, z, 'minecraft:structure_void')

    # ---- crypt (y 0-6): a 13x13 deepslate vault under the ward
    cx0, cx1, cz0, cz1 = 6, 18, 4, 16
    t.fill(cx0 - 1, 0, cz0 - 1, cx1 + 1, 6, cz1 + 1, 'minecraft:deepslate_tiles')
    t.fill(cx0, 1, cz0, cx1, 5, cz1, 'minecraft:air')
    t.fill(cx0, 0, cz0, cx1, 0, cz1, 'minecraft:polished_deepslate')
    for x, z in ((cx0 + 1, cz0 + 1), (cx1 - 1, cz0 + 1), (cx0 + 1, cz1 - 1), (cx1 - 1, cz1 - 1)):
        t.fill(x, 1, z, x, 5, z, 'minecraft:chiseled_deepslate')
    for x, z in ((cx0 + 3, cz0 + 3), (cx1 - 3, cz0 + 3), (cx0 + 3, cz1 - 3), (cx1 - 3, cz1 - 3), (12, 10)):
        t.set(x, 5, z, 'minecraft:soul_lantern', {'hanging': 'true', 'waterlogged': 'false'})
    t.set(12, 1, 10, 'bromaxpestilence:crypt_seal', {'broken': 'false'})
    t.set(12, 1, cz1, 'minecraft:chest', {'facing': 'north', 'type': 'single', 'waterlogged': 'false'},
          {'LootTable': 'bromaxpestilence:chests/lazaretto_crypt'})
    for x in (cx0 + 1, cx1 - 1):
        for z in range(cz0 + 3, cz1 - 2, 3):
            t.set(x, 1, z, 'minecraft:skeleton_skull', {'rotation': str(rng.randrange(16))})
    # stairwell: from the ward's north-west corner down a 1-wide flight along z, then a passage east
    t.fill(1, 0, 1, 5, 7, 11, 'minecraft:deepslate_bricks')
    for i in range(7):
        z = 3 + i
        y = 6 - i
        t.fill(3, y, z, 3, y + 3, z, 'minecraft:air')
        t.set(3, y, z, 'minecraft:deepslate_brick_stairs', {'facing': 'north', 'half': 'bottom', 'shape': 'straight', 'waterlogged': 'false'})
    t.fill(3, 0, 10, 3, 0, 10, 'minecraft:polished_deepslate')
    t.fill(3, 1, 10, 5, 3, 10, 'minecraft:air')
    t.fill(4, 0, 10, 5, 0, 10, 'minecraft:polished_deepslate')
    t.fill(cx0 - 1, 1, 10, cx0 - 1, 3, 10, 'minecraft:air')
    t.set(4, 3, 10, 'minecraft:soul_lantern', {'hanging': 'true', 'waterlogged': 'false'})

    # ---- the ward (y 7 floor, walls y 8-12, roof y 13, parapet y 14)
    x0, x1, z0, z1 = 0, 24, 0, 20
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            t.set(x, FLOOR, z, 'minecraft:stone_bricks' if (x + z) % 5 else 'minecraft:cobblestone')
            t.set(x, 13, z, brick())
            edge = x in (x0, x1) or z in (z0, z1)
            for y in range(8, 13):
                t.set(x, y, z, brick() if edge else 'minecraft:air')
            if edge and (x + z) % 2 == 0:
                t.set(x, 14, z, brick())
    t.set(3, FLOOR, 3, 'minecraft:air')                       # stairwell mouth
    t.fill(3, 8, 2, 3, 10, 3, 'minecraft:air')
    # barred windows every four blocks
    for x in range(4, x1, 4):
        for z in (z0, z1):
            t.fill(x, 9, z, x, 10, z, 'minecraft:iron_bars')
    for z in range(4, z1, 4):
        for x in (x0, x1):
            t.fill(x, 9, z, x, 10, z, 'minecraft:iron_bars')
    # door in the middle of the south wall, crossed out
    t.set(12, 8, z1, 'minecraft:oak_door', {'facing': 'north', 'half': 'lower', 'hinge': 'left', 'open': 'false', 'powered': 'false'})
    t.set(12, 9, z1, 'minecraft:oak_door', {'facing': 'north', 'half': 'upper', 'hinge': 'left', 'open': 'false', 'powered': 'false'})
    t.set(12, 10, z1 + 1, 'bromaxpestilence:plague_cross', {'facing': 'south'})
    t.set(11, 9, z1 + 1, 'bromaxpestilence:plague_cross', {'facing': 'south'})
    t.set(13, 9, z1 + 1, 'bromaxpestilence:plague_cross', {'facing': 'south'})
    # two rows of beds against the long walls, heads to the wall
    for i, x in enumerate(range(7, 23, 3)):
        for row, (head, foot, facing) in enumerate(((1, 2, 'north'), (19, 18, 'south'))):
            t.set(x, 8, head, 'minecraft:white_bed', {'facing': facing, 'part': 'head', 'occupied': 'false'})
            t.set(x, 8, foot, 'minecraft:white_bed', {'facing': facing, 'part': 'foot', 'occupied': 'false'})
            if (i + row) % 2 == 0:
                t.corpse(x + 0.5, 8.6, foot + 0.5, yaw=rng.uniform(0, 360))
            if rng.random() < 0.5:
                t.set(x + 1, 8, head, 'minecraft:candle', {'candles': str(rng.randint(1, 3)), 'lit': 'false', 'waterlogged': 'false'})
    # the dispensary, in the north-east corner
    t.set(22, 8, 9, 'bromaxpestilence:apothecary_table', {'facing': 'west'})
    t.set(22, 8, 11, 'bromaxpestilence:drying_rack', {'facing': 'west'})
    t.set(23, 8, 10, 'minecraft:chest', {'facing': 'west', 'type': 'single', 'waterlogged': 'false'},
          {'LootTable': 'bromaxpestilence:chests/lazaretto'})
    t.fill(23, 9, 8, 23, 10, 12, 'minecraft:bookshelf')
    # the sick were brought in here, and some got up again
    t.set(12, 8, 10, 'minecraft:spawner', None, {'SpawnData': {'entity': {'id': 'bromaxpestilence:afflicted_villager'}},
                                                 'Delay': Int(20), 'MinSpawnDelay': Int(400), 'MaxSpawnDelay': Int(900),
                                                 'SpawnCount': Int(1), 'MaxNearbyEntities': Int(3), 'RequiredPlayerRange': Int(12)})
    t.set(12, 7, 10, 'minecraft:stone_bricks')
    # cobwebs in the upper corners, rubble on the floor
    for _ in range(40):
        x, z = rng.randrange(1, x1), rng.randrange(1, z1)
        if t.blocks.get((x, 12, z), ('',))[0] == 'minecraft:air':
            t.set(x, 12, z, 'minecraft:cobweb')
    for _ in range(12):
        x, z = rng.randrange(1, x1), rng.randrange(4, z1 - 3)
        if t.blocks.get((x, 8, z), ('',))[0] == 'minecraft:air':
            t.set(x, 8, z, 'minecraft:cobblestone_slab', {'type': 'bottom', 'waterlogged': 'false'})
    t.save(os.path.join(DATA, 'structure', 'lazaretto.nbt'))
    print(f'lazaretto: {len(t.blocks)} blocks, {len(t.entities)} corpses')


def worldgen():
    def w(path, data):
        os.makedirs(os.path.dirname(os.path.join(DATA, path)), exist_ok=True)
        json.dump(data, open(os.path.join(DATA, path), 'w'), indent=2)
    w('worldgen/structure/lazaretto.json', {
        'type': 'minecraft:jigsaw', 'biomes': '#bromaxpestilence:has_lazaretto', 'step': 'surface_structures',
        'spawn_overrides': {}, 'terrain_adaptation': 'beard_thin', 'start_pool': 'bromaxpestilence:lazaretto',
        'size': 1, 'start_height': {'absolute': -FLOOR}, 'project_start_to_heightmap': 'WORLD_SURFACE_WG',
        'max_distance_from_center': 80, 'use_expansion_hack': False})
    w('worldgen/template_pool/lazaretto.json', {'fallback': 'minecraft:empty', 'elements': [{'weight': 1, 'element': {
        'element_type': 'minecraft:single_pool_element', 'location': 'bromaxpestilence:lazaretto',
        'projection': 'rigid', 'processors': 'minecraft:empty'}}]})
    w('worldgen/structure_set/lazaretto.json', {'structures': [{'structure': 'bromaxpestilence:lazaretto', 'weight': 1}],
        'placement': {'type': 'minecraft:random_spread', 'spacing': 48, 'separation': 18, 'salt': 20261006}})
    w('tags/worldgen/biome/has_lazaretto.json', {'values': ['#c:is_plains', '#minecraft:is_forest', '#minecraft:is_taiga', '#minecraft:is_savanna']})


if __name__ == '__main__':
    build()
    worldgen()
