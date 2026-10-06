#!/usr/bin/env python3
"""Generates BROMAX's Pestilence models, animations and textures.

Each model is a list of cubes (bone, origin, size, colour, optional face decor). The script packs
every cube face into its own UV rect, paints the rect (base colour, per-face light, noise, darker
rim, then decor), and writes GeckoLib geo.json + texture. Edit PALETTE or the cube lists and rerun:

    python3 tools/gen_assets.py

Coordinates are Blockbench/Bedrock pixels: feet at y=0, model faces north (-Z), 16px = 1 block.
"""
import json
import os
import random
import struct
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'bromaxpestilence')

PALETTE = {
    'hood': '#2b2420', 'lens': '#9fc7c0', 'brass': '#b08d3c', 'beak': '#c9b48a',
    'hat': '#1c1a1a', 'band': '#4a3526', 'coat': '#2e2a28', 'cape': '#232020',
    'belt': '#5a3d22', 'glove': '#3b2a1c', 'boot': '#1f1b18', 'cane': '#6b4a2b',
    'fur': '#4a3f38', 'fur_dark': '#382f2a', 'skin': '#c48b86', 'eye': '#a01818',
    'sore': '#8c8a3a',
    # Afflicted
    'bubo': '#2a1c22', 'glow': '#d4d94a', 'hoof': '#2e2620', 'horn': '#c9bfa5', 'bone': '#d8cfb8',
    'cow': '#5a4b3f', 'cow_pale': '#a99f8a', 'pig': '#b48580', 'pig_dark': '#8a5f5a', 'snout': '#9c6a66',
    'wool': '#9c957f', 'sheep_skin': '#8f7f78', 'sheep_face': '#4a423c', 'feather': '#a59e8f',
    'comb': '#6e2a2a', 'beak_y': '#a8863a', 'horse': '#51463d', 'mane': '#2a2420', 'muzzle': '#3d342d',
    'wolf': '#625e58', 'wolf_dark': '#46423d',
}


def rgba(name_or_hex, a=255):
    s = PALETTE.get(name_or_hex, name_or_hex).lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def scale(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c[:3]) + (c[3],)


def write_png(path, img):
    h, w = len(img), len(img[0])
    raw = b''.join(b'\x00' + bytes(v for px in row for v in px) for row in img)

    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
                + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


# ---------------------------------------------------------------------------------------------
# Model building
# ---------------------------------------------------------------------------------------------

FACE_LIGHT = {'up': 1.15, 'down': 0.7, 'north': 1.0, 'south': 0.92, 'east': 0.85, 'west': 0.85}


def cube(bone, origin, size, color, decor=None, inflate=0.0, noise=5, rim=True):
    return dict(bone=bone, origin=origin, size=size, color=color, decor=decor or {}, inflate=inflate,
                noise=noise, rim=rim)


def face_dims(size):
    sx, sy, sz = size
    return {'north': (sx, sy), 'south': (sx, sy), 'east': (sz, sy), 'west': (sz, sy),
            'up': (sx, sz), 'down': (sx, sz)}


def pack(rects, width):
    """Shelf-pack (key, w, h) rects; returns {key: (u, v)} and used height."""
    placed, x, y, shelf = {}, 0, 0, 0
    for key, w, h in sorted(rects, key=lambda r: (-r[2], -r[1])):
        if x + w > width:
            x, y, shelf = 0, y + shelf, 0
        placed[key] = (x, y)
        x += w
        shelf = max(shelf, h)
    return placed, y + shelf


def build(name, bones, cubes, seed, out_geo, out_tex, identifier, bounds=(2, 3, 1.25)):
    rects = []
    for i, c in enumerate(cubes):
        for face, (w, h) in face_dims(c['size']).items():
            rects.append(((i, face), int(w), int(h)))
    width = 32
    while True:
        placed, used = pack(rects, width)
        height = 16
        while height < used:
            height *= 2
        if height <= width * 2:
            break
        width *= 2
    img = [[(0, 0, 0, 0)] * width for _ in range(height)]
    rng = random.Random(seed)

    for i, c in enumerate(cubes):
        base = rgba(c['color'])
        for face, (w, h) in face_dims(c['size']).items():
            u, v = placed[(i, face)]
            w, h = int(w), int(h)
            for yy in range(h):
                for xx in range(w):
                    col = scale(base, FACE_LIGHT[face] * (1 + rng.uniform(-c['noise'], c['noise']) / 100))
                    if c['rim'] and w >= 3 and h >= 3 and (xx in (0, w - 1) or yy in (0, h - 1)):
                        col = scale(col, 0.85)
                    img[v + yy][u + xx] = col
            decor = c['decor'].get(face) or c['decor'].get('all')
            if decor:
                def put(x, y, color, f=FACE_LIGHT[face], u=u, v=v, w=w, h=h):
                    if 0 <= x < w and 0 <= y < h:
                        img[v + y][u + x] = scale(rgba(color), f)
                decor(put, w, h)

    geo_bones = []
    for b in bones:
        entry = {'name': b['name'], 'pivot': b['pivot']}
        if b.get('parent'):
            entry['parent'] = b['parent']
        cs = []
        for i, c in enumerate(cubes):
            if c['bone'] != b['name']:
                continue
            uv = {}
            for face, (w, h) in face_dims(c['size']).items():
                u, v = placed[(i, face)]
                uv[face] = {'uv': [u, v], 'uv_size': [int(w), int(h)]}
            cj = {'origin': c['origin'], 'size': c['size'], 'uv': uv}
            if c['inflate']:
                cj['inflate'] = c['inflate']
            cs.append(cj)
        if cs:
            entry['cubes'] = cs
        geo_bones.append(entry)

    write_json(out_geo, {
        'format_version': '1.12.0',
        'minecraft:geometry': [{
            'description': {
                'identifier': identifier, 'texture_width': width, 'texture_height': height,
                'visible_bounds_width': bounds[0], 'visible_bounds_height': bounds[1],
                'visible_bounds_offset': [0, bounds[2], 0],
            },
            'bones': geo_bones,
        }],
    })
    write_png(out_tex, img)
    print(f'{name}: {len(cubes)} cubes, texture {width}x{height}')


# ---------------------------------------------------------------------------------------------
# Decor helpers (face-local pixel coords, y=0 is the top of the face)
# ---------------------------------------------------------------------------------------------

def fill(color):
    def d(put, w, h):
        for y in range(h):
            for x in range(w):
                put(x, y, color)
    return d


def lens_face(put, w, h):
    for y in range(h):
        for x in range(w):
            put(x, y, 'lens')
    put(0, 0, '#d8ece8')


def beak_top(put, w, h):
    # A stitched seam running down the length of the beak.
    for y in range(0, h, 2):
        put(w // 2, y, '#8f7d58')


def coat_front(put, w, h):
    for y in range(h):
        put(w // 2 - 1, y, '#1d1a19')        # overlap seam
    for y in (2, 5, 8):
        put(1, y, 'brass')
        put(w - 2, y, 'brass')


def skirt_front(put, w, h):
    for y in range(h):
        put(w // 2, y, '#161413')            # coat split
    for x in range(w):
        if x % 3 == 0:
            put(x, h - 1, '#161413')         # ragged hem


def belt_front(put, w, h):
    for x in range(w // 2 - 1, w // 2 + 2):
        for y in range(h):
            put(x, y, 'brass')
    put(w // 2, h // 2, 'belt')


def rat_face(put, w, h):
    for y in range(h):
        for x in range(w):
            put(x, y, 'fur')
    put(0, 0, 'eye')
    put(w - 1, 0, 'eye')


def snout_face(put, w, h):
    for y in range(h):
        for x in range(w):
            put(x, y, 'skin')
    put(w // 2, 0, '#7a4a48')


def rat_back(put, w, h):
    rng = random.Random(66)
    for _ in range(4):
        x, y = rng.randrange(w), rng.randrange(h)
        put(x, y, 'sore')
        put(min(w - 1, x + 1), y, '#6e2a24')
    for y in range(h):
        put(w // 2, y, 'fur_dark')           # darker spine stripe


# ---------------------------------------------------------------------------------------------
# Plague doctor head (shared by the mob and the mask armour)
# ---------------------------------------------------------------------------------------------

def doctor_head(bone, inflate):
    t = inflate  # cubes in front of / above the head move out with the inflated hood
    return [
        cube(bone, [-4, 24, -4], [8, 8, 8], 'hood', inflate=inflate),
        cube(bone, [-4, 26, -4], [8, 1, 8], 'belt', inflate=inflate + 0.15),   # strap holding the beak
        cube(bone, [-3, 28.5, -4.6 - t], [2, 2, 1], 'brass', decor={'north': lens_face}),
        cube(bone, [1, 28.5, -4.6 - t], [2, 2, 1], 'brass', decor={'north': lens_face}),
        # The beak curves downward in steps rather than by rotation.
        cube(bone, [-2, 24, -10 - t], [4, 4, 6], 'beak', decor={'up': beak_top}),
        cube(bone, [-1.5, 23.5, -13 - t], [3, 3, 3], 'beak', decor={'up': beak_top}),
        cube(bone, [-1, 23, -15 - t], [2, 2, 2], '#b39f76'),
        cube(bone, [-0.5, 22.5, -16 - t], [1, 1, 1], '#9c8a64', rim=False),
        cube(bone, [-7, 32 + t, -7], [14, 1, 14], 'hat'),
        cube(bone, [-4.5, 33 + t, -4.5], [9, 1, 9], 'band', inflate=0.1),
        cube(bone, [-4.5, 34 + t, -4.5], [9, 3, 9], 'hat'),
    ]


def plague_doctor():
    bones = [
        {'name': 'root', 'pivot': [0, 0, 0]},
        {'name': 'torso', 'parent': 'root', 'pivot': [0, 12, 0]},
        {'name': 'head', 'parent': 'torso', 'pivot': [0, 24, 0]},
        {'name': 'skull', 'parent': 'head', 'pivot': [0, 24, 0]},
        {'name': 'right_arm', 'parent': 'torso', 'pivot': [-6, 22, 0]},
        {'name': 'left_arm', 'parent': 'torso', 'pivot': [6, 22, 0]},
        {'name': 'right_leg', 'parent': 'root', 'pivot': [-2, 12, 0]},
        {'name': 'left_leg', 'parent': 'root', 'pivot': [2, 12, 0]},
    ]
    cubes = doctor_head('skull', 0) + [
        cube('torso', [-4, 12, -2], [8, 12, 4], 'coat', decor={'north': coat_front}),
        cube('torso', [-5, 20, -3], [10, 4, 6], 'cape'),                     # shoulder capelet
        cube('torso', [-4.5, 2, -3], [9, 10, 6], 'coat', decor={'north': skirt_front}),
        cube('torso', [-4.5, 11, -3], [9, 2, 6], 'belt', inflate=0.1, decor={'north': belt_front}),
        cube('right_arm', [-8, 14, -2], [4, 10, 4], 'coat'),
        cube('right_arm', [-7.5, 11, -1.5], [3, 3, 3], 'glove'),
        cube('left_arm', [4, 14, -2], [4, 10, 4], 'coat'),
        cube('left_arm', [4.5, 11, -1.5], [3, 3, 3], 'glove'),
        cube('left_arm', [5.5, 0, -3], [1, 14, 1], 'cane', noise=8),        # cane hangs from the left hand
        cube('left_arm', [5, 13.5, -3.5], [2, 1, 2], 'brass'),
        cube('right_leg', [-4, 0, -2], [4, 12, 4], 'boot'),
        cube('left_leg', [0, 0, -2], [4, 12, 4], 'boot'),
    ]
    build('plague_doctor', bones, cubes, 202,
          os.path.join(ASSETS, 'geo', 'entity', 'plague_doctor.geo.json'),
          os.path.join(ASSETS, 'textures', 'entity', 'plague_doctor.png'),
          'geometry.plague_doctor', bounds=(3, 3.5, 1.5))


def plague_mask():
    names = ['armorHead', 'armorBody', 'armorRightArm', 'armorLeftArm',
             'armorRightLeg', 'armorLeftLeg', 'armorRightBoot', 'armorLeftBoot']
    pivots = {'armorHead': [0, 24, 0], 'armorBody': [0, 24, 0], 'armorRightArm': [-5, 22, 0],
              'armorLeftArm': [5, 22, 0], 'armorRightLeg': [-1.9, 12, 0], 'armorLeftLeg': [1.9, 12, 0],
              'armorRightBoot': [-1.9, 12, 0], 'armorLeftBoot': [1.9, 12, 0]}
    bones = [{'name': n, 'pivot': pivots[n]} for n in names]
    build('plague_mask', bones, doctor_head('armorHead', 0.6), 202,
          os.path.join(ASSETS, 'geo', 'item', 'armor', 'plague_mask.geo.json'),
          os.path.join(ASSETS, 'textures', 'item', 'armor', 'plague_mask.png'),
          'geometry.plague_mask', bounds=(2, 2.5, 1.5))


def plague_rat():
    bones = [
        {'name': 'root', 'pivot': [0, 0, 0]},
        {'name': 'body', 'parent': 'root', 'pivot': [0, 4, 0]},
        {'name': 'head', 'parent': 'body', 'pivot': [0, 4, -4]},
        {'name': 'skull', 'parent': 'head', 'pivot': [0, 4, -4]},
        {'name': 'tail', 'parent': 'body', 'pivot': [0, 3.5, 5]},
        {'name': 'tail_tip', 'parent': 'tail', 'pivot': [0, 3.5, 11]},
        {'name': 'leg_front_left', 'parent': 'root', 'pivot': [1.5, 2, -3]},
        {'name': 'leg_front_right', 'parent': 'root', 'pivot': [-1.5, 2, -3]},
        {'name': 'leg_back_left', 'parent': 'root', 'pivot': [1.5, 2, 3.5]},
        {'name': 'leg_back_right', 'parent': 'root', 'pivot': [-1.5, 2, 3.5]},
    ]
    cubes = [
        cube('body', [-2.5, 2, -4], [5, 4, 9], 'fur', noise=10, decor={'up': rat_back}),
        cube('body', [-3, 2, 0], [6, 5, 5], 'fur', noise=10, decor={'up': rat_back}),   # haunches
        cube('skull', [-2, 2.5, -8], [4, 3, 4], 'fur', noise=10, decor={'north': rat_face}),
        cube('skull', [-1, 2.5, -10], [2, 2, 2], 'skin', decor={'north': snout_face}, rim=False),
        cube('skull', [-2.5, 5.5, -6], [1, 2, 1], 'skin', rim=False),
        cube('skull', [1.5, 5.5, -6], [1, 2, 1], 'skin', rim=False),
        cube('tail', [-0.5, 3, 5], [1, 1, 6], 'skin', rim=False),
        cube('tail_tip', [-0.5, 3, 11], [1, 1, 6], '#b07a75', rim=False),
        cube('leg_front_left', [1, 0, -3.5], [1, 2, 1], 'skin', rim=False),
        cube('leg_front_right', [-2, 0, -3.5], [1, 2, 1], 'skin', rim=False),
        cube('leg_back_left', [1, 0, 3], [1, 2, 1], 'skin', rim=False),
        cube('leg_back_right', [-2, 0, 3], [1, 2, 1], 'skin', rim=False),
    ]
    build('plague_rat', bones, cubes, 66,
          os.path.join(ASSETS, 'geo', 'entity', 'plague_rat.geo.json'),
          os.path.join(ASSETS, 'textures', 'entity', 'plague_rat.png'),
          'geometry.plague_rat', bounds=(2, 1, 0.25))



def ribs(put, w, h):
    """Exposed ribs: dark vertical lines across the middle of a flank."""
    for x in range(2, w - 2, 2):
        for y in range(max(1, h // 5), h - max(1, h // 4)):
            put(x, y, '#2b221d')


def sores(seed, n=4):
    def d(put, w, h):
        rng = random.Random(seed)
        for _ in range(n):
            x, y = rng.randrange(w), rng.randrange(h)
            put(x, y, 'sore')
            put(min(w - 1, x + 1), y, '#6e2a24')
    return d


def both(*decors):
    def d(put, w, h):
        for f in decors:
            f(put, w, h)
    return d


def glow_eyes(base):
    """Head front: base colour with two sick yellow-green eyes in the upper corners."""
    def d(put, w, h):
        for y in range(h):
            for x in range(w):
                put(x, y, base)
        ey = max(1, h // 3)
        put(1, ey, 'glow')
        put(w - 2, ey, 'glow')
    return d


def patches(color, seed, n=6):
    def d(put, w, h):
        rng = random.Random(seed)
        for _ in range(n):
            x, y = rng.randrange(w), rng.randrange(h)
            for dx in range(2):
                for dy in range(2):
                    put(x + dx, y + dy, color)
    return d


def teeth(put, w, h):
    for x in range(w):
        put(x, h - 1, 'bone' if x % 2 == 0 else 'muzzle')


def bubo(bone, origin, size=(1, 2, 2)):
    return cube(bone, list(origin), list(size), 'bubo', noise=8, rim=False)


def quad_bones(body_pivot, head_pivot, legs, extra=()):
    bones = [{'name': 'root', 'pivot': [0, 0, 0]},
             {'name': 'body', 'parent': 'root', 'pivot': body_pivot},
             {'name': 'head', 'parent': 'body', 'pivot': head_pivot}]
    for name, pivot in legs.items():
        bones.append({'name': name, 'parent': 'root', 'pivot': pivot})
    bones.extend(extra)
    return bones


def leg(name, x, z, w, h, color, hoof=None, hoof_h=2):
    cs = [cube(name, [x, hoof_h if hoof else 0, z], [w, h - (hoof_h if hoof else 0), w], color)]
    if hoof:
        cs.append(cube(name, [x, 0, z], [w, hoof_h, w], hoof, rim=False))
    return cs


def entity(name, bones, cubes, seed, bounds):
    build(name, bones, cubes, seed,
          os.path.join(ASSETS, 'geo', 'entity', f'{name}.geo.json'),
          os.path.join(ASSETS, 'textures', 'entity', f'{name}.png'),
          f'geometry.{name}', bounds=bounds)


# ---------------------------------------------------------------------------------------------
# Animations (Molang; math.sin takes degrees)
# ---------------------------------------------------------------------------------------------

def swing(amount, speed, phase=0):
    return f'math.sin(query.anim_time * {speed} + {phase}) * {amount}'


def animations():
    doctor = {
        'misc.idle': {'loop': True, 'animation_length': 4.0, 'bones': {
            'torso': {'scale': [1, f'1 + {swing(0.01, 90)}', 1]},
            'skull': {'rotation': [swing(3, 90, 90), swing(4, 45), 0]},
            'right_arm': {'rotation': [swing(2, 90), 0, swing(1.5, 90)]},
        }},
        'move.walk': {'loop': True, 'animation_length': 1.2, 'bones': {
            'right_leg': {'rotation': [swing(22, 300), 0, 0]},
            'left_leg': {'rotation': [swing(22, 300, 180), 0, 0]},
            'right_arm': {'rotation': [swing(18, 300, 180), 0, 0]},
            'left_arm': {'rotation': [swing(8, 300), 0, 0]},
            'torso': {'position': [0, f'math.abs({swing(0.5, 300)})', 0]},
            'skull': {'rotation': [swing(2, 600), 0, 0]},
        }},
    }
    rat = {
        'misc.idle': {'loop': True, 'animation_length': 2.0, 'bones': {
            'skull': {'rotation': [swing(4, 720), swing(6, 180), 0]},
            'tail': {'rotation': [0, swing(12, 180), 0]},
            'tail_tip': {'rotation': [0, swing(15, 180, 60), 0]},
        }},
        'move.walk': {'loop': True, 'animation_length': 0.4, 'bones': {
            'leg_front_left': {'rotation': [swing(35, 900), 0, 0]},
            'leg_back_right': {'rotation': [swing(35, 900), 0, 0]},
            'leg_front_right': {'rotation': [swing(35, 900, 180), 0, 0]},
            'leg_back_left': {'rotation': [swing(35, 900, 180), 0, 0]},
            'body': {'position': [0, f'math.abs({swing(0.4, 900)})', 0]},
            'tail': {'rotation': [0, swing(18, 900), 0]},
            'tail_tip': {'rotation': [0, swing(22, 900, 90), 0]},
        }},
        'attack.bite': {'loop': False, 'animation_length': 0.3, 'bones': {
            'skull': {'rotation': {'0.0': [0, 0, 0], '0.1': [-25, 0, 0], '0.2': [15, 0, 0], '0.3': [0, 0, 0]}},
        }},
    }

    for name, anims in [('plague_doctor', doctor), ('plague_rat', rat)]:
        write_json(os.path.join(ASSETS, 'animations', 'entity', f'{name}.animation.json'),
                   {'format_version': '1.8.0', 'animations': anims, 'geckolib_format_version': 2})


# ---------------------------------------------------------------------------------------------
# 16x16 item icons and 18x18 effect icons (hand pixel art)
# ---------------------------------------------------------------------------------------------

def sprite(path, rows, key):
    img = [[rgba(key[ch]) if ch != '.' else (0, 0, 0, 0) for ch in row] for row in rows]
    write_png(path, img)


def icons():
    sprite(os.path.join(ASSETS, 'textures', 'item', 'plague_mask.png'), [
        '................',
        '.....kkkkkk.....',
        '.....kkkkkk.....',
        '.....bbbbbb.....',
        '..kkkkkkkkkkkk..',
        '.....hhhhhhh....',
        '....hrlrhhhhh...',
        '....hrrrhhhhh...',
        '..wwwhhhhhhhh...',
        'wwwwwwhhhhhhh...',
        '.WWwwwhhhhhhh...',
        '...WWwhhhhhh....',
        '.....hhhhhhh....',
        '.....hhhhhh.....',
        '......hhhh......',
        '................',
    ], {'k': 'hat', 'b': 'band', 'h': 'hood', 'r': 'brass', 'l': 'lens', 'w': 'beak', 'W': '#9c8a64'})
    sprite(os.path.join(ASSETS, 'textures', 'item', 'four_thieves_vinegar.png'), [
        '................',
        '.......cc.......',
        '.......cc.......',
        '......gnng..e...',
        '.......gg..ee...',
        '......gvvg.e....',
        '.....gvvvvg.....',
        '....gvvVvvvg....',
        '....gvVvvvvg....',
        '....gLLLLLLg....',
        '....gLLLLLLg....',
        '....gvvvvvDg....',
        '....gvvvvDDg....',
        '.....gDDDDg.....',
        '......gggg......',
        '................',
    ], {'c': '#8a6a43', 'g': '#c8dfe0', 'n': '#a9c4c6', 'v': '#9a6b24', 'V': '#c99a4b', 'D': '#6e4a17',
        'L': '#d9c9a0', 'e': '#5f7f3a'})
    sprite(os.path.join(ASSETS, 'textures', 'item', 'rat_tail.png'), [
        '................',
        '................',
        '...........fff..',
        '..........ffff..',
        '..........fff...',
        '.........pP.....',
        '........pP......',
        '.......pP.......',
        '......pP........',
        '.....pP.........',
        '....pP..........',
        '...pP...........',
        '...p............',
        '..pP............',
        '..p.............',
        '................',
    ], {'f': 'fur', 'p': 'skin', 'P': '#9c6763'})

    sprite(os.path.join(ASSETS, 'textures', 'item', 'shrouded_corpse.png'), [
        '................',
        '................',
        '................',
        '.....llll.......',
        '...llLLLLll.....',
        '..lLLLLrLLLll...',
        '.lLLLLLrLLLLLl..',
        '.lLLLLLrLLLLLLl.',
        '.lddLLLrLLLLddl.',
        '.lLLddLrLLddLLl.',
        '..lLLLdrddLLLl..',
        '...llLLrLLLll...',
        '.....lllll......',
        '................',
        '................',
        '................',
    ], {'l': '#6e6656', 'L': '#cfc6ad', 'd': '#9c937c', 'r': '#7a5a3a'})
    write_png(os.path.join(ASSETS, 'textures', 'particle', 'fly.png'),
              [[(20, 18, 16, 255) if 2 <= x <= 5 and 3 <= y <= 4 or x in (2, 5) and y == 2 else (0, 0, 0, 0)
                for x in range(8)] for y in range(8)])

    def circle_icon(path, ring, inner, spots):
        img = [[(0, 0, 0, 0)] * 18 for _ in range(18)]
        for y in range(18):
            for x in range(18):
                d = ((x - 8.5) ** 2 + (y - 8.5) ** 2) ** 0.5
                if d <= 7.5:
                    img[y][x] = rgba(ring if d > 6.2 else inner)
        for x, y, c in spots:
            img[y][x] = rgba(c)
        write_png(path, img)

    effects = os.path.join(ASSETS, 'textures', 'mob_effect')
    circle_icon(os.path.join(effects, 'plague.png'), '#2a2f16', '#6b7330',
                [(6, 6, '#1a1a10'), (7, 6, '#1a1a10'), (6, 7, '#1a1a10'), (11, 9, '#1a1a10'),
                 (11, 10, '#1a1a10'), (10, 10, '#1a1a10'), (7, 12, '#3a1a14'), (8, 12, '#3a1a14')])
    cross = [(x, y, '#ffffff') for x in range(7, 11) for y in range(4, 14)] + \
            [(x, y, '#ffffff') for x in range(4, 14) for y in range(7, 11)]
    circle_icon(os.path.join(effects, 'plague_immunity.png'), '#2f5a24', '#5f9a4a', cross)


# ---------------------------------------------------------------------------------------------
# Block textures and block models (system 2: nests and the Blight)
# ---------------------------------------------------------------------------------------------

def noise_tex(size, base, var, seed):
    rng = random.Random(seed)
    return [[scale(rgba(base), 1 + rng.uniform(-var, var) / 100) for _ in range(size)] for _ in range(size)]


def block_textures():
    tex = os.path.join(ASSETS, 'textures', 'block')
    rng = random.Random(404)
    top = noise_tex(16, '#4a4038', 12, 1)
    for _ in range(3):                                   # rot veins
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(9):
            top[y % 16][x % 16] = rgba('#5a3a52')
            x += rng.choice((-1, 0, 1)); y += rng.choice((0, 1))
    for _ in range(7):
        top[rng.randrange(16)][rng.randrange(16)] = rgba('#8c8a3a')
    write_png(os.path.join(tex, 'blighted_soil_top.png'), top)
    side = noise_tex(16, '#6b4f35', 10, 2)
    for x in range(16):
        depth = 3 + rng.randrange(3)
        for y in range(depth):
            side[y][x] = scale(rgba('#4a4038'), 1 + rng.uniform(-0.1, 0.1))
        if rng.random() < 0.4:
            side[depth][x] = rgba('#5a3a52')
    write_png(os.path.join(tex, 'blighted_soil_side.png'), side)
    leaves = []
    for y in range(16):
        row = []
        for x in range(16):
            if rng.random() < 0.28:
                row.append((0, 0, 0, 0))
            else:
                row.append(scale(rgba(rng.choice(['#6e5a3e', '#5a4a34', '#8a7550', '#4f4030'])), 1 + rng.uniform(-0.08, 0.08)))
        leaves.append(row)
    write_png(os.path.join(tex, 'withered_leaves.png'), leaves)
    sprite(os.path.join(ASSETS, 'textures', 'item', 'quicklime.png'), [
        '................',
        '................',
        '................',
        '................',
        '................',
        '.......ww.......',
        '......wWWw......',
        '.....wWWWWw.....',
        '....wWWwWWWw....',
        '...wWWWWWwWWw...',
        '..wWWwWWWWWWWw..',
        '..gwWWWWWwWWwg..',
        '...ggggggggggg..',
        '................',
        '................',
        '................',
    ], {'w': '#d9d6cc', 'W': '#f2f0ea', 'g': '#a8a49a'})


def box(fr, to, tex, faces=('north', 'south', 'east', 'west', 'up', 'down'), top=None, rotation=None):
    el = {'from': fr, 'to': to, 'faces': {f: {'texture': top if f == 'up' and top else tex} for f in faces}}
    if rotation:
        el['rotation'] = rotation
    return el


def block_model(name, elements, textures, render_type=None, parent='minecraft:block/block'):
    model = {'parent': parent, 'textures': textures, 'elements': elements}
    if render_type:
        model['render_type'] = render_type
    write_json(os.path.join(ASSETS, 'models', 'block', f'{name}.json'), model)


def nest_models():
    heights = [8, 10, 13, 16]
    for stage in range(1, 5):
        h = heights[stage - 1]
        # Rat Warren: an earth mound riddled with burrow holes (one more per stage).
        # stepped tiers so it reads as a heaped mound, not a cube
        t1, t2 = round(h * 0.45), round(h * 0.75)
        els = [box([0, 0, 0], [16, t1, 16], '#dirt', top='#top'), box([2, t1, 2], [14, t2, 14], '#dirt', top='#top'),
               box([4, t2, 4], [12, h, 12], '#top')]
        holes = [([6, 0.5, -0.01], [10, min(4, t1 - 0.5), -0.01], 'north'), ([16.01, 0.5, 6], [16.01, min(4, t1 - 0.5), 10], 'east'),
                 ([6, 0.5, 16.01], [10, min(4, t1 - 0.5), 16.01], 'south'), ([-0.01, 0.5, 6], [-0.01, min(4, t1 - 0.5), 10], 'west')]
        for fr, to, face in holes[:stage]:
            els.append(box(fr, to, '#hole', faces=(face,)))
        if stage >= 2:
            els.append(box([1, t1, 10], [4, t1 + 2, 14], '#gravel'))
        if stage >= 3:
            els.append(box([11, t1, 1], [15, t1 + 1, 4], '#gravel'))
        if stage >= 4:
            els.append(box([7, h, 4], [8, h + 3, 5], '#bone', rotation={'origin': [7.5, h, 4.5], 'axis': 'x', 'angle': 22.5}))
        block_model(f'rat_warren_stage{stage}', els, {'particle': 'minecraft:block/coarse_dirt', 'dirt': 'minecraft:block/coarse_dirt',
                    'top': 'minecraft:block/rooted_dirt', 'gravel': 'minecraft:block/gravel', 'hole': 'minecraft:block/black_concrete',
                    'bone': 'minecraft:block/bone_block_side'})

        # Plague Pit: churned mud and soul soil with bones jutting out (more each stage).
        els = [box([0, 0, 0], [16, 15, 16], '#mud', top='#soil')]
        bones = [([3, 15, 4], 5, 22.5, 'x'), ([11, 15, 10], 4, -22.5, 'z'), ([7, 15, 11], 6, 22.5, 'z'),
                 ([12, 15, 3], 3, -22.5, 'x'), ([4, 15, 12], 4, 45, 'x')]
        for (x, y, z), length, angle, axis in bones[:1 + stage]:
            els.append(box([x, y, z], [x + 1, y + length, z + 1], '#bone',
                           rotation={'origin': [x + 0.5, y, z + 0.5], 'axis': axis, 'angle': angle}))
        if stage >= 3:
            els.append(box([5, 15, 5], [9, 18, 9], '#skull', top='#skull_top'))
        block_model(f'plague_pit_stage{stage}', els, {'particle': 'minecraft:block/mud', 'mud': 'minecraft:block/mud',
                    'soil': 'minecraft:block/soul_soil', 'bone': 'minecraft:block/bone_block_side',
                    'skull': 'minecraft:block/bone_block_side', 'skull_top': 'minecraft:block/bone_block_top'})

        # Miasma Well: a mossy stone ring brimming with green sludge; taller each stage.
        els = [box([0, 0, 0], [16, h, 3], '#stone'), box([0, 0, 13], [16, h, 16], '#stone'),
               box([0, 0, 3], [3, h, 13], '#stone'), box([13, 0, 3], [16, h, 13], '#stone'),
               box([3, 0, 3], [13, h - 1, 13], '#sludge')]
        if stage >= 3:
            els.append(box([0, h, 0], [16, h + 0.5, 3], '#moss'))
        block_model(f'miasma_well_stage{stage}', els, {'particle': 'minecraft:block/mossy_cobblestone',
                    'stone': 'minecraft:block/mossy_cobblestone', 'sludge': 'minecraft:block/slime_block', 'moss': 'minecraft:block/moss_block'},
                    render_type='minecraft:translucent')

    # Herb Brazier: an iron bowl on four legs, smouldering herbs when lit.
    legs = [box([x, 0, z], [x + 1, 6, z + 1], '#iron') for x, z in ((3, 3), (12, 3), (3, 12), (12, 12))]
    bowl = [box([2, 6, 2], [14, 7, 14], '#iron'), box([2, 7, 2], [14, 11, 3], '#iron'), box([2, 7, 13], [14, 11, 14], '#iron'),
            box([2, 7, 3], [3, 11, 13], '#iron'), box([13, 7, 3], [14, 11, 13], '#iron')]
    tex = {'particle': 'minecraft:block/anvil', 'iron': 'minecraft:block/anvil', 'coal': 'minecraft:block/coal_block',
           'herbs': 'minecraft:block/moss_block', 'embers': 'minecraft:block/magma', 'fire': 'minecraft:block/fire_0'}
    block_model('herb_brazier', legs + bowl + [box([3, 7, 3], [13, 9, 13], '#coal', top='#herbs')], tex)
    fire = [{'from': [3, 9, 8], 'to': [13, 19, 8], 'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': a}, 'shade': False,
             'faces': {'north': {'texture': '#fire'}, 'south': {'texture': '#fire'}}} for a in (45, -45)]
    block_model('herb_brazier_lit', legs + bowl + [box([3, 7, 3], [13, 9, 13], '#coal', top='#embers')] + fire, tex,
                render_type='minecraft:cutout')


if __name__ == '__main__':
    plague_doctor()
    plague_mask()
    plague_rat()
    animations()
    icons()
    block_textures()
    nest_models()
    import vanilla_afflicted
    vanilla_afflicted.build_all()
