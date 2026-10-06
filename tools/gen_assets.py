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
    'hood': '#3e3028', 'lens': '#a8d8cc', 'brass': '#c09a48', 'beak': '#d8c8a0',
    'hat': '#2c2622', 'band': '#6a4a30', 'coat': '#3d3530', 'cape': '#2e2826',
    'belt': '#6a4628', 'glove': '#5a3e28', 'boot': '#2a2420', 'cane': '#7a5432',
    'fur': '#5a4a40', 'fur_dark': '#3e322c', 'skin': '#c48b86', 'eye': '#a01818',
    'sore': '#8c8a3a',
    # Afflicted
    'bubo': '#2a1c22', 'glow': '#d4d94a', 'hoof': '#2e2620', 'horn': '#c9bfa5', 'bone': '#d8cfb8',
    'cow': '#5a4b3f', 'cow_pale': '#a99f8a', 'pig': '#b48580', 'pig_dark': '#8a5f5a', 'snout': '#9c6a66',
    'wool': '#9c957f', 'sheep_skin': '#8f7f78', 'sheep_face': '#4a423c', 'feather': '#a59e8f',
    'comb': '#6e2a2a', 'beak_y': '#a8863a', 'horse': '#51463d', 'mane': '#2a2420', 'muzzle': '#3d342d',
    'wolf': '#625e58', 'wolf_dark': '#46423d',
}


def rgba(name_or_hex, a=255):
    if '@' in name_or_hex:
        name_or_hex, a = name_or_hex.split('@')[0], int(name_or_hex.split('@')[1])
    s = PALETTE.get(name_or_hex, name_or_hex).lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def scale(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c[:3]) + (c[3],)


def _handmade():
    path = os.path.join(HERE, 'handmade.txt')
    if not os.path.exists(path):
        return set()
    return {os.path.normpath(os.path.join(ASSETS, l.strip())) for l in open(path) if l.strip() and not l.startswith('#')}


def write_png(path, img, force=False):
    """Write an RGBA PNG. Files listed in tools/handmade.txt are hand-painted and left alone unless forced."""
    if not force and os.path.normpath(path) in _handmade() and os.path.exists(path):
        return
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


def cube(bone, origin, size, color, decor=None, inflate=0.0, noise=5, rim=True, mat=None):
    return dict(bone=bone, origin=origin, size=size, color=color, decor=decor or {}, inflate=inflate,
                noise=noise, rim=rim, mat=mat)


# ---- materials: how a surface is shaded (remastered models) ----------------------------------
FACE_T = {'up': 0.22, 'down': -0.45, 'north': 0.0, 'south': -0.12, 'east': -0.18, 'west': -0.18}


def tone(c, t):
    """Pixel-art shading: darker shades drift cool and richer, lighter ones warm and paler."""
    import colorsys
    h, l, s_ = colorsys.rgb_to_hls(c[0] / 255, c[1] / 255, c[2] / 255)
    if t < 0:
        l *= 1 + t * 0.75
        h = h + (0.62 - h) * min(0.25, -t * 0.12) if s_ > 0.05 else h
        s_ = min(1, s_ * (1 - t * 0.25))
    else:
        l = l + (1 - l) * t * 0.55
        h = h + (0.11 - h) * min(0.25, t * 0.12) if s_ > 0.05 else h
        s_ *= 1 - t * 0.15
    r, g, b = colorsys.hls_to_rgb(h % 1, max(0, min(1, l)), max(0, min(1, s_)))
    return (int(r * 255), int(g * 255), int(b * 255), c[3])


def material_t(mat, xx, yy, w, h, face, rng, cell):
    side = face not in ('up', 'down')
    t = FACE_T[face] + ((0.12 - 0.26 * yy / max(1, h - 1)) if side and h > 2 else 0)
    if mat == 'cloth':
        t += (-0.15 if (xx + cell(xx, 0) % 2) % 4 == 0 else 0) + rng.uniform(-0.04, 0.04)
        if side and yy == h - 1:
            t -= 0.12
    elif mat == 'leather':
        t += (cell(xx // 2, yy // 2) % 5 - 2) * 0.035 + rng.uniform(-0.03, 0.03)
        if side and xx + yy == 1:
            t += 0.3
    elif mat == 'fur':
        t += (cell(xx, yy // 2) % 5 - 2) * 0.06 + (0.08 if yy == 0 and side else 0)
    elif mat == 'metal':
        band = max(1, int(h * 0.3))
        t += 0.4 if yy == band else (-0.18 if yy > h * 0.65 else 0.05) + rng.uniform(-0.02, 0.02)
    elif mat == 'wood':
        t += (-0.14 if xx % 3 == 0 else 0) + (cell(xx, yy // 3) % 3 - 1) * 0.04
    elif mat == 'bone':
        t += rng.uniform(-0.05, 0.05) + (-0.45 if cell(xx, yy) % 23 == 0 else 0)
    elif mat == 'flesh':
        t += (cell(xx // 2, yy // 2) % 5 - 2) * 0.06 + rng.uniform(-0.04, 0.04)
    elif mat == 'stone':
        t += (cell(xx // 2, yy // 2) % 5 - 2) * 0.07
    elif mat == 'glow':
        t = 0.1
    if mat != 'glow' and w >= 3 and h >= 3 and (xx == w - 1 or yy == h - 1):
        t -= 0.1
    return t


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


def build(name, bones, cubes, seed, out_geo, out_tex, identifier, bounds=(2, 3, 1.25), mats=None):
    """mats: {colour key: material} for cubes that don't name their own material (remastered models)."""
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
        mat = c.get('mat') or (mats or {}).get(c['color'])
        for face, (w, h) in face_dims(c['size']).items():
            u, v = placed[(i, face)]
            w, h = int(w), int(h)
            salt = zlib.crc32(f'{name}{i}{face}'.encode())
            cell = lambda a, b, salt=salt: zlib.crc32(f'{salt},{a},{b}'.encode()) % 997
            for yy in range(h):
                for xx in range(w):
                    if mat:
                        col = tone(base, material_t(mat, xx, yy, w, h, face, rng, cell))
                    else:
                        col = scale(base, FACE_LIGHT[face] * (1 + rng.uniform(-c['noise'], c['noise']) / 100))
                        if c['rim'] and w >= 3 and h >= 3 and (xx in (0, w - 1) or yy in (0, h - 1)):
                            col = scale(col, 0.85)
                    img[v + yy][u + xx] = col
            decor = c['decor'].get(face) or c['decor'].get('all')
            if decor:
                def put(x, y, color, face=face, u=u, v=v, w=w, h=h, mat=mat):
                    if 0 <= x < w and 0 <= y < h:
                        img[v + y][u + x] = tone(rgba(color), FACE_T[face] * 0.6) if mat else scale(rgba(color), FACE_LIGHT[face])
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

DOCTOR_MATS = {'bone': 'bone', 'hood': 'leather', 'brass': 'metal', 'beak': 'leather', '#b39f76': 'leather', '#9c8a64': 'leather', 'hat': 'cloth', 'band': 'leather', 'belt': 'leather', 'coat': 'leather', 'cape': 'leather', 'glove': 'leather', 'boot': 'leather', 'cane': 'wood', '#2a2522': 'cloth'}
RAT_MATS = {'fur': 'fur', 'skin': 'flesh', '#b07a75': 'flesh', '#8a7a70': 'fur'}
CROW_MATS = {'#2a2c38': 'fur', '#24242c': 'fur', '#1a1a22': 'fur', '#33333a': 'metal', '#3a3a40': 'leather'}


def rimmed_lens(put, w, h):
    """Brass-rimmed glass: rim all round, pale glass inside with a glint."""
    for y in range(h):
        for x in range(w):
            put(x, y, 'brass' if x in (0, w - 1) or y in (0, h - 1) else 'lens')
    if w > 2 and h > 2:
        put(1, 1, '#e8fff8')


def beak_side(put, w, h):
    """Breathing holes near the root of the beak."""
    put(w - 2, h - 2, '#2a1c14'); put(w - 3, h - 2, '#2a1c14')


def fingers(put, w, h):
    for x in range(0, w, 2):
        put(x, h - 1, '#3a2818')


def doctor_head(bone, inflate):
    t = inflate  # cubes in front of / above the head move out with the inflated hood
    return [
        cube(bone, [-4, 24, -4], [8, 8, 8], 'hood', inflate=inflate),
        cube(bone, [-4, 26, -4], [8, 1, 8], 'belt', inflate=inflate + 0.15),   # strap holding the beak
        cube(bone, [-3.5, 27.5, -4.6 - t], [3, 3, 1], 'brass', decor={'north': rimmed_lens}),
        cube(bone, [0.5, 27.5, -4.6 - t], [3, 3, 1], 'brass', decor={'north': rimmed_lens}),
        # The beak curves downward in steps rather than by rotation.
        cube(bone, [-2, 24, -10 - t], [4, 4, 6], 'beak', decor={'up': beak_top, 'east': beak_side, 'west': beak_side}),
        cube(bone, [-1.5, 23.5, -13 - t], [3, 3, 3], 'beak', decor={'up': beak_top}),
        cube(bone, [-1, 23, -15 - t], [2, 2, 2], '#b39f76'),
        cube(bone, [-0.5, 22.5, -16 - t], [1, 1, 1], '#9c8a64', rim=False),
        cube(bone, [-7, 32 + t, -7], [14, 1, 14], 'hat'),
        cube(bone, [-4.5, 33 + t, -4.5], [9, 1, 9], 'band', inflate=0.1),
        cube(bone, [-4.5, 34 + t, -4.5], [9, 4, 9], 'hat'),
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
        cube('torso', [-4.5, 22, -3], [9, 3, 6], 'cape'),                     # high collar
        cube('torso', [-6, 19, -3.5], [12, 4, 7], 'cape'),                    # capelet over the shoulders
        cube('torso', [-4.5, 6, -3], [9, 6, 6], 'coat', decor={'north': skirt_front}),
        cube('torso', [-5, 2, -3.5], [10, 4, 7], 'coat', decor={'north': skirt_front}),   # skirts flaring at the hem
        cube('torso', [-4.5, 11, -3], [9, 2, 6], 'belt', inflate=0.1, decor={'north': belt_front}),
        cube('right_arm', [-8, 14, -2], [4, 10, 4], 'coat'),
        cube('right_arm', [-8.5, 13, -2.5], [5, 2, 5], 'cape'),              # turned-back cuff
        cube('right_arm', [-7.5, 10, -1.5], [3, 3, 3], 'glove', decor={'all': fingers}),
        cube('left_arm', [4, 14, -2], [4, 10, 4], 'coat'),
        cube('left_arm', [3.5, 13, -2.5], [5, 2, 5], 'cape'),
        cube('left_arm', [4.5, 10, -1.5], [3, 3, 3], 'glove', decor={'all': fingers}),
        cube('left_arm', [5.5, 0, -3], [1, 13, 1], 'cane'),                  # cane hangs from the left hand
        cube('left_arm', [5, 13, -3.5], [2, 2, 2], 'bone', decor={'north': sockets}),     # skull-topped
        cube('right_leg', [-4, 0, -2], [4, 12, 4], 'boot'),
        cube('left_leg', [0, 0, -2], [4, 12, 4], 'boot'),
    ]
    build('plague_doctor', bones, cubes, 202,
          os.path.join(ASSETS, 'geo', 'entity', 'plague_doctor.geo.json'),
          os.path.join(ASSETS, 'textures', 'entity', 'plague_doctor.png'),
          'geometry.plague_doctor', bounds=(3, 3.5, 1.5), mats=DOCTOR_MATS)


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
          'geometry.plague_mask', bounds=(2, 2.5, 1.5), mats=DOCTOR_MATS)


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
    def ear(put, w, h):
        for y in range(h):
            for x in range(w):
                put(x, y, '#c48b86' if (x, y) != (0, 0) else '#e8b0a8')
        put(w - 1, h - 1, '#8a5a56')

    cubes = [
        cube('body', [-2.5, 2, -4], [5, 4, 9], 'fur', decor={'up': rat_back}),
        cube('body', [-3, 2, 0], [6, 5, 5], 'fur', decor={'up': rat_back}),          # haunches
        cube('body', [-2, 1.5, -3], [4, 1, 7], '#8a7a70'),                            # paler belly
        cube('skull', [-2, 2, -7], [4, 4, 3], 'fur', decor={'north': rat_face}),
        cube('skull', [-1.5, 2, -9], [3, 3, 2], 'fur'),                               # the snout tapers
        cube('skull', [-1, 2, -10], [2, 2, 1], 'skin', decor={'north': snout_face}, rim=False),
        cube('skull', [-3, 5.5, -6], [2, 2, 1], 'skin', decor={'north': ear}, rim=False),
        cube('skull', [1, 5.5, -6], [2, 2, 1], 'skin', decor={'north': ear}, rim=False),
        cube('tail', [-1, 2.5, 5], [2, 2, 3], 'skin'),                                # thick at the root
        cube('tail', [-0.5, 3, 8], [1, 1, 3], 'skin', rim=False),
        cube('tail_tip', [-0.5, 3, 11], [1, 1, 6], '#b07a75', rim=False),
        cube('leg_front_left', [1, 0, -3.5], [1, 2, 1], 'skin', rim=False),
        cube('leg_front_right', [-2, 0, -3.5], [1, 2, 1], 'skin', rim=False),
        cube('leg_back_left', [1, 0, 3], [2, 2, 2], 'skin', rim=False),               # big hind feet
        cube('leg_back_right', [-3, 0, 3], [2, 2, 2], 'skin', rim=False),
    ]
    build('plague_rat', bones, cubes, 66,
          os.path.join(ASSETS, 'geo', 'entity', 'plague_rat.geo.json'),
          os.path.join(ASSETS, 'textures', 'entity', 'plague_rat.png'),
          'geometry.plague_rat', bounds=(2, 1, 0.25), mats=RAT_MATS)



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


# ---------------------------------------------------------------------------------------------
# System 3: herbs, apothecary icons, Doctor's Garb
# ---------------------------------------------------------------------------------------------

HERB_STYLE = {
    # stem, leaf colours, accent (flower/bulb), leaf shape
    'garlic': ('#5d7a3a', ['#6f9a45', '#5c8538', '#87b05a'], '#e8e2d4', 'blade'),
    'rue': ('#4d6b4a', ['#6f9a8a', '#5d8878', '#85b0a0'], '#e3c94a', 'round'),
    'sage': ('#5a6650', ['#8fa085', '#7d8f74', '#a3b398'], '#8a6ab0', 'broad'),
    'wormwood': ('#6b7268', ['#b3bcae', '#9fa89a', '#c8d0c2'], '#d8c24a', 'feather'),
    'rosemary': ('#5a4a32', ['#3f6b45', '#355c3a', '#4d7d52'], '#9fb8e0', 'needle'),
}
STAGE_HEIGHT = [5, 8, 11, 15]


def herb_sprite(name, stage):
    stem, leaves, accent, shape = HERB_STYLE[name]
    rng = random.Random(zlib.crc32(f"{name}{stage}".encode()))  # stable across runs (hash() is salted)
    img = [[(0, 0, 0, 0)] * 16 for _ in range(16)]

    def put(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            img[y][x] = rgba(c)
    h = STAGE_HEIGHT[stage]
    stems = [3, 7, 11, 13] if stage >= 2 else [5, 10]
    for sx in stems:
        top = 16 - h + rng.randrange(0, 3)
        x = sx
        for y in range(15, top - 1, -1):
            put(x, y, stem if shape in ('needle', 'feather') or y > 13 else leaves[1])
            if shape == 'blade':
                continue
            if y % 2 == 0 and y < 14:
                side = 1 if (y // 2) % 2 else -1
                if shape == 'round':
                    put(x + side, y, rng.choice(leaves)); put(x + side, y - 1, rng.choice(leaves))
                elif shape == 'broad':
                    for dx in (1, 2):
                        put(x + side * dx, y, rng.choice(leaves)); put(x + side * dx, y - 1, rng.choice(leaves))
                elif shape == 'feather':
                    for dx in (1, 2, 3):
                        if rng.random() < 0.7: put(x + side * dx, y - (dx // 2), rng.choice(leaves))
                elif shape == 'needle':
                    put(x - 1, y, rng.choice(leaves)); put(x + 1, y, rng.choice(leaves))
            if shape == 'blade' and rng.random() < 0.3:
                x += rng.choice((-1, 1))
        if shape == 'blade':                      # garlic: a second blade leaning off each stem
            for y in range(15, top + 2, -1):
                put(sx + (15 - y) // 4 + 1, y, rng.choice(leaves))
        if stage == 3:
            if shape == 'blade':                  # flower head on the scape
                for dx, dy in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)):
                    put(sx + dx, top + dy, accent)
            else:
                put(sx, top, accent); put(sx + rng.choice((-1, 1)), top + 1, accent)
    return img


def dried(img):
    out = []
    for row in img:
        new = []
        for r, g, b, a in row:
            if a == 0:
                new.append((r, g, b, a)); continue
            lum = 0.3 * r + 0.59 * g + 0.11 * b
            mix = [v * 0.35 + lum * 0.65 for v in (r, g, b)]
            new.append((int(mix[0] * 0.9 + 22), int(mix[1] * 0.8 + 10), int(mix[2] * 0.55), a))
        out.append(new)
    return out


HERB_ICONS = {
    'garlic': ['................', '.......g........', '......gg........', '.......g........', '.......g........',
               '......www.......', '.....wWWWw......', '....wWWwWWw.....', '....wWWwWWw.....', '....wWwWwWw.....',
               '....wWwWwWw.....', '.....wWWWw......', '......ddd.......', '................', '................', '................'],
    'rue': ['................', '.....y..y.......', '....yyyyyy......', '.....lyyl.......', '....lLs.Ll......',
            '...lL.s.lLl.....', '....l.s..l......', '...lLls.Ll......', '....lLs.lLl.....', '.....l.sLl......',
            '.......s........', '.......s........', '......s.........', '................', '................', '................'],
    'sage': ['................', '........LL......', '.......LlLL.....', '......LlLLL.....', '......LlLLL.....',
             '...LL.LlLL......', '..LlLL.LL.......', '..LlLLLs........', '...LLLls........', '.......s........',
             '......s.........', '......s.........', '.....s..........', '................', '................', '................'],
    'wormwood': ['................', '.......y........', '....l.lsl.l.....', '...lLl.s.lLl....', '....l.lsl.l.....',
                 '..lLl..s..lLl...', '...l.l.s.l.l....', '..lLl.lsl.lLl...', '....l..s..l.....', '.......s........',
                 '.......s........', '......s.........', '................', '................', '................', '................'],
    'rosemary': ['................', '.........b......', '........lsl.....', '.......lsl......', '........sl......',
                 '.......lsl......', '......lsl.......', '.......sl.......', '......lsl.......', '.....lsl........',
                 '......s.........', '.....s..........', '....s...........', '................', '................', '................'],
}


def herbs():
    blocks = os.path.join(ASSETS, 'textures', 'block')
    items = os.path.join(ASSETS, 'textures', 'item')
    for name, (stem, leaves, accent, _) in HERB_STYLE.items():
        for stage in range(4):
            write_png(os.path.join(blocks, f'{name}_stage{stage}.png'), herb_sprite(name, stage))
        key = {'g': leaves[0], 'w': '#ddd6c6', 'W': '#f2ede2', 'd': '#9c8f75', 'y': accent, 'b': accent,
               'l': leaves[0], 'L': leaves[1], 's': stem}
        icon = [[rgba(key[c]) if c != '.' else (0, 0, 0, 0) for c in row] for row in HERB_ICONS[name]]
        write_png(os.path.join(items, f'{name}.png'), icon)
        write_png(os.path.join(items, f'dried_{name}.png'), dried(icon))


def bottle(liquid, light, dark, cork='#8a6a43', label=None):
    rows = ['................', '.......cc.......', '.......cc.......', '......gnng......', '.......gg.......',
            '......gvvg......', '.....gvvvvg.....', '....gvvVvvvg....', '....gvVvvvvg....', '....gvvvvvvg....',
            '....gvvvvvvg....', '....gvvvvvDg....', '....gvvvvDDg....', '.....gDDDDg.....', '......gggg......', '................']
    if label:
        rows[9] = '....gLLLLLLg....'
    return rows, {'c': cork, 'g': '#c8dfe0', 'n': '#a9c4c6', 'v': liquid, 'V': light, 'D': dark, 'L': label or '#000000'}


def apothecary_icons():
    items = os.path.join(ASSETS, 'textures', 'item')
    for name, args in {'wormwood_tincture': ('#5b6b2a', '#8a9a48', '#3e4a1c', '#7a5a3a', '#d9c9a0'),
                       'theriac': ('#6e2416', '#a8442a', '#45140c', '#3a2a1a', '#e0c070')}.items():
        rows, key = bottle(*args[:4], label=args[4])
        sprite(os.path.join(items, f'{name}.png'), rows, key)
    icons = {
        'herbal_broth': (['................', '................', '................', '................', '................',
                          '................', '...g..s..g......', '..bbbbbbbbbbb...', '..bBBgBBBgBBb...', '..wbBBBBBsBbw...',
                          '...wbbbbbbbw....', '....wwwwwww.....', '.....ddddd......', '................', '................', '................'],
                         {'b': '#b8a050', 'B': '#c9b260', 'g': '#5c8538', 's': '#8fa085', 'w': '#8a6a43', 'd': '#6b5233'}),
        'plague_poultice': (['................', '................', '................', '...cccccccccc...', '...cCCCgCCCCc...',
                             '...cCgggCCCCc...', '...cCCgCCsCCc...', '...rrrrrrrrrr...', '...cCCCCsCCCc...', '...cCCgCCCgCc...',
                             '...cCCCCCCCCc...', '...cccccccccc...', '................', '................', '................', '................'],
                            {'c': '#a89f84', 'C': '#d9cfb0', 'g': '#5c8538', 's': '#8fa085', 'r': '#7a5a3a'}),
        'mortar_and_pestle': (['................', '...........ww...', '..........wW....', '.........wW.....', '........wW......',
                               '.......wW.......', '..ssssswssssss..', '..sSSSSwSSSSSs..', '..sSSSSSSSSSSs..', '...sSSSSSSSSs...',
                               '....sSSSSSSs....', '.....ssssss.....', '....dddddddd....', '................', '................', '................'],
                              {'s': '#6f6f6f', 'S': '#9a9a9a', 'w': '#6b4a2b', 'W': '#8f6a42', 'd': '#4a4a4a'}),
        'physicians_kit': (['................', '................', '.....hhhhhh.....', '.....h....h.....', '..llllllllllll..',
                            '..lLLLLLLLLLLl..', '..lLLLLbbLLLLl..', '..lllllbBlllll..', '..lLLLLLLLLLLl..', '..lLLLLLLLLLLl..',
                            '..lLLLLLLLLLLl..', '..lLLLLLLLLLLl..', '..llllllllllll..', '................', '................', '................'],
                           {'h': '#4a3424', 'l': '#4a3424', 'L': '#7a5638', 'b': '#b08d3c', 'B': '#e0c070'}),
        'lancet': (['................', '................', '................', '............ss..', '...........sSs..',
                    '..........sSs...', '.........sSs....', '........sSs.....', '.......gg.......', '......whg.......',
                    '.....whw........', '....whw.........', '...whw..........', '...ww...........', '................', '................'],
                   {'s': '#8f9ba3', 'S': '#dfe8ee', 'g': '#b08d3c', 'w': '#5a3d22', 'h': '#8f6a42'}),
        'leech_jar': (['................', '.....cccccc.....', '.....cccccc.....', '....gggggggg....', '....gwwwwwwg....',
                       '....gwkwwwwg....', '....gwkkwkwg....', '....gwwwwkwg....', '....gwkwwkwg....', '....gwkkwwwg....',
                       '....gwwwwkkg....', '....gwwkwwwg....', '....gggggggg....', '................', '................', '................'],
                      {'c': '#8a6a43', 'g': '#c8dfe0', 'w': '#4f6a4a', 'k': '#1a1612'}),
        'censer': (['.......c........', '.......c........', '.......c........', '......ccc.......', '.....bBBBb......',
                    '....bBhBhBb.....', '....bBBBBBb.....', '...bhBBhBBhb....', '...bBBBBBBBb....', '....bBhBhBb.....',
                    '....bBBBBBb.....', '.....bbbbb......', '......bbb.......', '................', '................', '................'],
                   {'c': '#6e6e6e', 'b': '#7a5f22', 'B': '#c8a048', 'h': '#2a2018'}),
        'doctors_coat': (['................', '...cc.....cc....', '..cCCcccccCCc...', '..cCCCCCCCCCc...', '..cCCCbCbCCCc...',
                          '..cCCCCCCCCCc...', '...cCCbCbCCc....', '....cCCCCCc.....', '....cCCbCbCc....', '....rrrBrrrr....',
                          '....cCCCCCCc....', '...cCCCCCCCCc...', '...cCCCCCCCCc...', '...cccccccccc...', '................', '................'],
                         {'c': '#1a1716', 'C': '#2e2a28', 'b': '#b08d3c', 'r': '#5a3d22', 'B': '#b08d3c'}),
        'doctors_trousers': (['................', '................', '...cccccccccc...', '...cCCCCCCCCc...', '...cCCCCCCCCc...',
                              '...cCCCccCCCc...', '...cCCCc.cCCc...', '...cCCc..cCCc...', '...cCCc..cCCc...', '...cCCc..cCCc...',
                              '...cCCc..cCCc...', '...cCCc..cCCc...', '...cccc..cccc...', '................', '................', '................'],
                             {'c': '#1a1716', 'C': '#2a2522'}),
        'doctors_boots': (['................', '................', '................', '................', '...cccc..cccc...',
                           '...cCCc..cCCc...', '...cCCc..cCCc...', '...cCCc..cCCc...', '...cCCc..cCCc...', '..cCCCc.cCCCc...',
                           '.cCCCCc.cCCCCc..', '.cCCCCc.cCCCCc..', '.cccccc.cccccc..', '................', '................', '................'],
                          {'c': '#0f0d0c', 'C': '#1f1b18'}),
    }
    for name, (rows, key) in icons.items():
        sprite(os.path.join(items, f'{name}.png'), rows, key)


def doctors_garb():
    names = ['armorHead', 'armorBody', 'armorRightArm', 'armorLeftArm', 'armorRightLeg', 'armorLeftLeg', 'armorRightBoot', 'armorLeftBoot']
    pivots = {'armorHead': [0, 24, 0], 'armorBody': [0, 24, 0], 'armorRightArm': [-5, 22, 0], 'armorLeftArm': [5, 22, 0],
              'armorRightLeg': [-1.9, 12, 0], 'armorLeftLeg': [1.9, 12, 0], 'armorRightBoot': [-1.9, 12, 0], 'armorLeftBoot': [1.9, 12, 0]}
    bones = [{'name': n, 'pivot': pivots[n]} for n in names]
    cubes = [
        cube('armorBody', [-4, 12, -2], [8, 12, 4], 'coat', inflate=0.6, decor={'north': coat_front}),
        cube('armorBody', [-4, 20, -2], [8, 4, 4], 'cape', inflate=1.0),                      # shoulder capelet
        cube('armorBody', [-4, 4, -2], [8, 8, 4], 'coat', inflate=1.0, decor={'north': skirt_front}),   # long skirts
        cube('armorBody', [-4, 11, -2], [8, 2, 4], 'belt', inflate=0.75, decor={'north': belt_front}),
        cube('armorBody', [-4.5, 22, -2.5], [9, 3, 5], 'cape', inflate=0.6),                     # high collar
        cube('armorRightArm', [-8, 12, -2], [4, 12, 4], 'coat', inflate=0.55),
        cube('armorRightArm', [-8, 13, -2], [4, 2, 4], 'cape', inflate=0.85),
        cube('armorLeftArm', [4, 13, -2], [4, 2, 4], 'cape', inflate=0.85),
        cube('armorRightArm', [-8, 12, -2], [4, 3, 4], 'glove', inflate=0.7),
        cube('armorLeftArm', [4, 12, -2], [4, 12, 4], 'coat', inflate=0.55),
        cube('armorLeftArm', [4, 12, -2], [4, 3, 4], 'glove', inflate=0.7),
        cube('armorRightLeg', [-3.9, 3, -2], [4, 9, 4], '#2a2522', inflate=0.45),
        cube('armorLeftLeg', [-0.1, 3, -2], [4, 9, 4], '#2a2522', inflate=0.45),
        cube('armorRightBoot', [-3.9, 0, -2], [4, 5, 4], 'boot', inflate=0.7),
        cube('armorLeftBoot', [-0.1, 0, -2], [4, 5, 4], 'boot', inflate=0.7),
    ]
    build('doctors_garb', bones, cubes, 303,
          os.path.join(ASSETS, 'geo', 'item', 'armor', 'doctors_garb.geo.json'),
          os.path.join(ASSETS, 'textures', 'item', 'armor', 'doctors_garb.png'),
          'geometry.doctors_garb', bounds=(2, 2.5, 1), mats=DOCTOR_MATS)


def plague_cross():
    rng = random.Random(55)
    img = []
    for y in range(16):
        row = []
        for x in range(16):
            board = (y // 4) % 2
            c = scale(rgba('#7a5a3a' if board else '#6b4f33'), 1 + rng.uniform(-0.08, 0.08))
            if y % 4 == 0:
                c = scale(c, 0.75)                              # gaps between boards
            row.append(c)
        img.append(row)
    for i in range(1, 15):                                      # the red cross, daubed thick
        for t in (0, 1):
            for x, y in ((i + t, i), (15 - i - t, i)):
                if 0 <= x < 16:
                    img[y][x] = scale(rgba('#9a1c14'), 1 + rng.uniform(-0.12, 0.08))
    for x, y0 in ((4, 12), (11, 11), (8, 9)):                   # drips
        for y in range(y0, min(16, y0 + 3)):
            img[y][x] = scale(rgba('#7a140e'), 1)
    write_png(os.path.join(ASSETS, 'textures', 'block', 'plague_cross.png'), img)
    write_json(os.path.join(ASSETS, 'models', 'block', 'plague_cross.json'), {
        'parent': 'minecraft:block/block', 'textures': {'particle': 'bromaxpestilence:block/plague_cross', 'cross': 'bromaxpestilence:block/plague_cross',
                                                        'edge': 'minecraft:block/stripped_oak_log'},
        'elements': [{'from': [1, 1, 15], 'to': [15, 15, 16], 'faces': {
            'north': {'texture': '#cross', 'uv': [1, 1, 15, 15]}, 'south': {'texture': '#edge'},
            'east': {'texture': '#edge'}, 'west': {'texture': '#edge'}, 'up': {'texture': '#edge'}, 'down': {'texture': '#edge'}}}]})


def apothecary_models():
    def box(fr, to, tex, top=None):
        return {'from': fr, 'to': to, 'faces': {f: {'texture': top if f in ('up', 'down') and top else tex}
                                                for f in ('north', 'south', 'east', 'west', 'up', 'down')}}
    legs = [box([x, 0, z], [x + 2, 14, z + 2], '#leg', top='#leg_top') for x, z in ((1, 1), (13, 1), (1, 13), (13, 13))]
    table = legs + [box([0, 14, 0], [16, 16, 16], '#top'), box([2, 4, 2], [14, 5, 14], '#shelf'),
                    box([2, 5, 10], [6, 9, 14], '#jar', top='#jar_top'), box([10, 5, 3], [13, 8, 6], '#jar', top='#jar_top')]
    write_json(os.path.join(ASSETS, 'models', 'block', 'apothecary_table.json'), {
        'parent': 'minecraft:block/block', 'textures': {
            'particle': 'minecraft:block/spruce_planks', 'top': 'minecraft:block/spruce_planks', 'shelf': 'minecraft:block/oak_planks',
            'leg': 'minecraft:block/stripped_spruce_log', 'leg_top': 'minecraft:block/stripped_spruce_log_top',
            'jar': 'minecraft:block/glass', 'jar_top': 'minecraft:block/barrel_top'},
        'render_type': 'minecraft:cutout', 'elements': table})
    rack = [box([1, 0, 7], [3, 16, 9], '#post', top='#post_top'), box([13, 0, 7], [15, 16, 9], '#post', top='#post_top'),
            box([0, 13, 7.5], [16, 15, 8.5], '#bar'), box([3, 2, 7.5], [13, 3, 8.5], '#bar'),
            box([0, 0, 5], [4, 1, 11], '#post', top='#post_top'), box([12, 0, 5], [16, 1, 11], '#post', top='#post_top')]
    write_json(os.path.join(ASSETS, 'models', 'block', 'drying_rack.json'), {
        'parent': 'minecraft:block/block', 'textures': {
            'particle': 'minecraft:block/stripped_oak_log', 'post': 'minecraft:block/stripped_oak_log',
            'post_top': 'minecraft:block/stripped_oak_log_top', 'bar': 'minecraft:block/oak_planks'}, 'elements': rack})


# ---------------------------------------------------------------------------------------------
# System 5: Heralds and vermin
# ---------------------------------------------------------------------------------------------

def hooded_face(hood, eye='glow', shadow='#14110f'):
    """A hood seen from the front: dark hollow with two eyes in it."""
    def d(put, w, h):
        for y in range(h):
            for x in range(w):
                put(x, y, hood)
        for y in range(2, h - 1):
            for x in range(1, w - 1):
                put(x, y, shadow)
        if eye:
            put(2, 4, eye); put(w - 3, 4, eye)
    return d


def welts(put, w, h):
    rng = random.Random(77)
    for _ in range(6):
        y = rng.randrange(1, h - 1)
        for x in range(rng.randrange(0, 2), w - rng.randrange(0, 2)):
            if (x + y) % 3:
                put(x, y, '#8a2a20')


def stitched(put, w, h):
    for y in range(1, h - 1, 3):
        put(w // 2, y, '#4a3c2c')
    for x in range(1, w - 1, 3):
        put(x, h // 2, '#4a3c2c')


def sockets(put, w, h):
    """A bone-white skull face: hollow eyes (2x2 on big faces), a nose hole, a row of teeth."""
    for y in range(h):
        for x in range(w):
            put(x, y, 'bone')
    big = w >= 6 and h >= 6
    ey = 2 if big else 1
    for dx in (range(2) if big else range(1)):
        for dy in (range(2) if big else range(1)):
            put(1 + dx, ey + dy, '#1a1612'); put(w - 2 - dx, ey + dy, '#1a1612')
    if h > 3:
        put(w // 2, ey + (2 if big else 1), '#1a1612')
    if big:
        for x in range(2, w - 2):
            put(x, h - 2, '#1a1612' if x % 2 else '#b8ae98')


def bars(put, w, h):
    for y in range(h):
        for x in range(w):
            put(x, y, '#6e6e6e' if x % 2 == 0 or y in (0, h - 1) else '#1a1612')


def censer_holes(put, w, h):
    for y in range(h):
        for x in range(w):
            put(x, y, '#7a5f22')
    put(1, 1, '#b8c84a')


def herald(name, robe, hood, extras, skirt=True, torso=None, sleeves=None, legs='#2a2522', face=None, belt='#5a3d22', seed=0):
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
    cubes = [
        cube('skull', [-4, 24, -4], [8, 8, 8], hood, decor={'north': face or hooded_face(hood)}),
        cube('torso', [-4, 12, -2], [8, 12, 4], torso or robe, decor={'north': welts} if torso else {}),
        cube('torso', [-4, 11, -2], [8, 1, 4], belt, inflate=0.3),
        cube('right_arm', [-8, 12, -2], [4, 12, 4], sleeves or robe),
        cube('left_arm', [4, 12, -2], [4, 12, 4], sleeves or robe),
        cube('right_arm', [-7.5, 10, -1.5], [3, 2, 3], '#8a7a6a'),
        cube('left_arm', [4.5, 10, -1.5], [3, 2, 3], '#8a7a6a'),
        cube('right_leg', [-4, 0, -2], [4, 12, 4], legs),
        cube('left_leg', [0, 0, -2], [4, 12, 4], legs),
    ]
    if skirt:
        cubes.append(cube('torso', [-4.5, 2, -3], [9, 10, 6], robe, decor={'north': skirt_front}))
    build(name, bones, cubes + extras, seed,
          os.path.join(ASSETS, 'geo', 'entity', f'{name}.geo.json'),
          os.path.join(ASSETS, 'textures', 'entity', f'{name}.png'),
          f'geometry.{name}', bounds=(3, 3.5, 1.5))


def heralds():
    herald('plague_bearer', '#4a3c2c', '#3a2e22', [
        cube('torso', [-5, 13, 2], [10, 11, 6], '#7a6a4a', noise=9, decor={'all': stitched}),     # the sack of bodies
        cube('torso', [-3, 24, 4], [1, 3, 1], 'bone', rim=False),
        cube('torso', [1, 23, 3], [1, 4, 1], 'bone', rim=False),
        cube('torso', [-1, 23, 5], [3, 3, 3], 'bone', decor={'north': sockets, 'south': sockets}),
        cube('right_arm', [-7, 7, -1], [2, 3, 2], 'brass'),                                    # handbell
        cube('right_arm', [-6.5, 10, -0.5], [1, 1, 1], 'cane', rim=False),
    ], seed=501)
    herald('gravedigger', '#3a3028', '#4a3a2c', [
        cube('torso', [-4, 7, -2.6], [8, 15, 1], '#5a3d22'),                                        # leather apron
        cube('skull', [-5, 32, -5], [10, 1, 10], '#3a3028'),                                         # flat cap brim
        cube('right_arm', [-6.5, 2, -0.5], [1, 11, 1], 'cane', rim=False),                           # shovel
        cube('right_arm', [-7.5, 0, -1], [3, 3, 1], '#8f9ba3'),
    ], seed=502)
    herald('flagellant', '#6b5a44', '#4a3c2c', [
        cube('skull', [-3, 32, -3], [6, 3, 6], '#4a3c2c'),                                           # pointed hood
        cube('skull', [-1.5, 35, -1.5], [3, 3, 3], '#4a3c2c'),
        cube('left_arm', [5.5, 5, -0.5], [1, 6, 1], '#5a3d22', rim=False),                           # the whip
        cube('left_arm', [6, 1, -0.5], [1, 4, 1], '#3a2a1a', rim=False),
    ], torso='#b08870', sleeves='#b08870', belt='#8a7a5a', seed=503)
    herald('censer_monk', '#4a5040', '#3e4436', [
        cube('skull', [-4, 24, -4], [8, 8, 8], '#3e4436', inflate=0.45, decor={'north': hooded_face('#3e4436', eye=None)}),
        cube('left_arm', [5.5, 5, -0.5], [1, 5, 1], '#6e6e6e', rim=False),                           # chain
        cube('left_arm', [4.5, 2, -1.5], [3, 3, 3], '#7a5f22', decor={'all': censer_holes}),        # the foul censer
    ], seed=504)
    herald('ratcatcher', '#5a4632', '#4a3a2c', [
        cube('skull', [-6, 32, -6], [12, 1, 12], '#3a2e22'),                                         # wide hat
        cube('skull', [-4, 33, -4], [8, 3, 8], '#3a2e22'),
        cube('torso', [-0.5, 8, 2.5], [1, 24, 1], 'cane', rim=False),                                # pole
        cube('torso', [-2.5, 30, 1], [5, 5, 4], '#6e6e6e', decor={'all': bars}),                     # cage
        cube('torso', [3, 24, -1], [2, 2, 4], 'fur', noise=10),                                     # rat on the shoulder
        cube('torso', [3.5, 24.5, 3], [1, 1, 3], 'skin', rim=False),
    ], seed=505)
    herald('mourner', '#151214', '#151214', [
        cube('skull', [-4.5, 16, -4.5], [9, 9, 9], '#1d181b@235', noise=4),                         # the veil, to the waist
        cube('torso', [-2, 14, -3], [4, 2, 1], '#9a948e', rim=False),                                # clasped hands
    ], face=hooded_face('#151214', eye='#bdb6ae'), legs='#151214', belt='#151214', seed=506)
    herald('bone_collector', '#5a5248', '#4a4238', [
        cube('torso', [-4, 14, 2], [8, 2, 2], 'bone', rim=False),                                    # bundle of bones on the back
        cube('torso', [-1, 10, 2.5], [2, 14, 2], 'bone', rim=False),
        cube('torso', [-2, 18, 4], [4, 4, 3], 'bone', decor={'south': sockets}),
    ], face=sockets, seed=507)


def miasma_wraith():
    bones = [{'name': 'root', 'pivot': [0, 0, 0]},
             {'name': 'body', 'parent': 'root', 'pivot': [0, 10, 0]},
             {'name': 'head', 'parent': 'body', 'pivot': [0, 20, 0]},
             {'name': 'skull', 'parent': 'head', 'pivot': [0, 20, 0]},
             {'name': 'wisp_left', 'parent': 'body', 'pivot': [4.5, 19, 0]},
             {'name': 'wisp_right', 'parent': 'body', 'pivot': [-4.5, 19, 0]},
             {'name': 'tail', 'parent': 'body', 'pivot': [0, 10, 0]}]
    def hollow(put, w, h):
        for y in range(h):
            for x in range(w):
                put(x, y, '#a8b070@170')
        for x, y in ((1, 2), (w - 2, 2), (2, 2), (w - 3, 2)):
            put(x, y, '#e8f080')
        for x in range(2, w - 2):
            put(x, h - 2, '#2a3010@200')
    cubes = [
        cube('skull', [-3, 20, -3], [6, 6, 6], '#a8b070@170', noise=12, decor={'north': hollow}),
        cube('body', [-3.5, 10, -2.5], [7, 10, 5], '#6b7330@150', noise=14),
        cube('tail', [-2.5, 5, -2], [5, 5, 4], '#6b7330@120', noise=14),
        cube('tail', [-1.5, 1, -1], [3, 4, 3], '#6b7330@90', noise=14),
        cube('wisp_left', [3.5, 10, -1], [2, 9, 2], '#7a8240@140', noise=14),
        cube('wisp_right', [-5.5, 10, -1], [2, 9, 2], '#7a8240@140', noise=14),
    ]
    build('miasma_wraith', bones, cubes, 601, os.path.join(ASSETS, 'geo', 'entity', 'miasma_wraith.geo.json'),
          os.path.join(ASSETS, 'textures', 'entity', 'miasma_wraith.png'), 'geometry.miasma_wraith', bounds=(2, 2.5, 1))


def pustule():
    def veins(put, w, h):
        rng = random.Random(91)
        x, y = rng.randrange(w), rng.randrange(h)
        for _ in range(w):
            put(x % w, y % h, '#8a4a44'); x += 1; y += rng.choice((-1, 0, 1))
        put(rng.randrange(w), rng.randrange(h), '#d4d94a')
    bones = [{'name': 'root', 'pivot': [0, 0, 0]}, {'name': 'sac', 'parent': 'root', 'pivot': [0, 0, 0]}]
    cubes = [
        cube('sac', [-6, 0, -6], [12, 6, 12], '#a89a5a', noise=10, decor={'all': veins}),
        cube('sac', [-5, 6, -5], [10, 5, 10], '#b8a86a', noise=10, decor={'all': veins}),
        cube('sac', [-3, 11, -3], [6, 3, 6], '#c8b87a', noise=10, decor={'up': veins}),
        cube('sac', [-7, 2, -2], [1, 3, 3], '#8a7a4a'), cube('sac', [5, 3, 1], [1, 2, 2], '#8a7a4a'),
        cube('sac', [-2, 4, -7], [3, 3, 1], '#d4c88a'),
    ]
    build('pustule', bones, cubes, 602, os.path.join(ASSETS, 'geo', 'entity', 'pustule.geo.json'),
          os.path.join(ASSETS, 'textures', 'entity', 'pustule.png'), 'geometry.pustule', bounds=(1.5, 1.2, 0.5))


def rat_like(name, fur, fur_dark, skin, extra=(), belly=None, seed=66):
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
    if belly:
        bones.append({'name': 'belly', 'parent': 'body', 'pivot': [0, 3, 1]})
    def face(put, w, h):
        for y in range(h):
            for x in range(w):
                put(x, y, fur)
        put(0, 0, 'eye'); put(w - 1, 0, 'eye')
    cubes = [
        cube('body', [-2.5, 2, -4], [5, 4, 9], fur, noise=10, decor={'up': rat_back}),
        cube('body', [-3, 2, 0], [6, 5, 5], fur, noise=10),
        cube('skull', [-2, 2.5, -8], [4, 3, 4], fur, noise=10, decor={'north': face}),
        cube('skull', [-1, 2.5, -10], [2, 2, 2], skin, decor={'north': snout_face}, rim=False),
        cube('skull', [-2.5, 5.5, -6], [1, 2, 1], skin, rim=False),
        cube('skull', [1.5, 5.5, -6], [1, 2, 1], skin, rim=False),
        cube('tail', [-0.5, 3, 5], [1, 1, 6], skin, rim=False),
        cube('tail_tip', [-0.5, 3, 11], [1, 1, 6], skin, rim=False),
        cube('leg_front_left', [1, 0, -3.5], [1, 2, 1], skin, rim=False),
        cube('leg_front_right', [-2, 0, -3.5], [1, 2, 1], skin, rim=False),
        cube('leg_back_left', [1, 0, 3], [1, 2, 1], skin, rim=False),
        cube('leg_back_right', [-2, 0, 3], [1, 2, 1], skin, rim=False),
    ] + list(extra)
    if belly:
        def stretched(put, w, h):
            rng = random.Random(5)
            for _ in range(4):
                x, y = rng.randrange(w), rng.randrange(h)
                for _ in range(4):
                    put(x % w, y % h, '#7a3a44'); x += 1; y += rng.choice((-1, 0, 1))
        cubes.append(cube('belly', [-3.5, 0.5, -3], [7, 6, 8], belly, noise=8, decor={'all': stretched}))
    build(name, bones, cubes, seed, os.path.join(ASSETS, 'geo', 'entity', f'{name}.geo.json'),
          os.path.join(ASSETS, 'textures', 'entity', f'{name}.png'), f'geometry.{name}', bounds=(2, 1, 0.25))


def vermin():
    rat_like('sewer_rat', '#2f2b26', '#1f1c19', '#8a6a64', seed=67)
    rat_like('bloated_rat', '#6a5e56', '#4a423c', '#b88a84', belly='#9a8a80', seed=68)
    rat_like('burrower_rat', '#5a4632', '#3a2e22', '#a88070', extra=[
        cube('leg_front_left', [1, 0, -4.5], [1, 1, 1], 'bone', rim=False),
        cube('leg_front_right', [-2, 0, -4.5], [1, 1, 1], 'bone', rim=False),
        cube('skull', [-0.5, 2.5, -11], [1, 1, 1], '#7a5a54', rim=False),
    ], seed=69)
    # the swarm: seven small rats in a loose pack, each on its own bone so they scurry out of step
    spots = [(-5, -4), (0, -5), (5, -3), (-3, 1), (3, 1), (-6, 4), (1, 5)]
    bones = [{'name': 'root', 'pivot': [0, 0, 0]}] + [{'name': f'r{i}', 'parent': 'root', 'pivot': [x, 0, z]} for i, (x, z) in enumerate(spots)]
    cubes = []
    for i, (x, z) in enumerate(spots):
        cubes += [cube(f'r{i}', [x - 1, 0, z - 2], [2, 2, 4], 'fur', noise=10, rim=False),
                  cube(f'r{i}', [x - 0.5, 0.5, z - 3], [1, 1, 1], 'skin', rim=False),
                  cube(f'r{i}', [x - 0.5, 0.5, z + 2], [1, 1, 3], 'skin', rim=False)]
    build('rat_swarm', bones, cubes, 70, os.path.join(ASSETS, 'geo', 'entity', 'rat_swarm.geo.json'),
          os.path.join(ASSETS, 'textures', 'entity', 'rat_swarm.png'), 'geometry.rat_swarm', bounds=(2, 1, 0.25))
    # carrion crow
    def crow_face(put, w, h):
        for y in range(h):
            for x in range(w):
                put(x, y, '#1a1a20')
        put(0, 1, '#c8a040'); put(w - 1, 1, '#c8a040')
    bones = [{'name': 'root', 'pivot': [0, 0, 0]},
             {'name': 'body', 'parent': 'root', 'pivot': [0, 4, 0]},
             {'name': 'head', 'parent': 'body', 'pivot': [0, 6, -3]},
             {'name': 'skull', 'parent': 'head', 'pivot': [0, 6, -3]},
             {'name': 'wing_left', 'parent': 'body', 'pivot': [2, 7, 0]},
             {'name': 'wing_right', 'parent': 'body', 'pivot': [-2, 7, 0]},
             {'name': 'tail', 'parent': 'body', 'pivot': [0, 5, 4]},
             {'name': 'leg_left', 'parent': 'root', 'pivot': [1, 3, 0]},
             {'name': 'leg_right', 'parent': 'root', 'pivot': [-1, 3, 0]}]
    cubes = [cube('body', [-2, 3, -3], [4, 4, 4], '#2a2c38'),                                  # breast
             cube('body', [-1.5, 3.5, 1], [3, 3, 3], '#2a2c38'),                                 # back, narrowing
             cube('skull', [-1.5, 6, -5], [3, 3, 3], '#2a2c38', decor={'north': crow_face}),
             cube('skull', [-1, 6, -7], [2, 2, 2], '#33333a'),                                   # heavy beak
             cube('skull', [-0.5, 6, -8], [1, 1, 1], '#44444c', rim=False),
             cube('wing_left', [2, 4, -2], [1, 3, 5], '#30323e'),
             cube('wing_left', [2, 4, 3], [1, 2, 3], '#22242e'),                                 # long primaries
             cube('wing_right', [-3, 4, -2], [1, 3, 5], '#30323e'),
             cube('wing_right', [-3, 4, 3], [1, 2, 3], '#22242e'),
             cube('tail', [-1.5, 4, 4], [3, 1, 3], '#22242e'),
             cube('tail', [-2, 4, 7], [4, 1, 2], '#22242e'),                                     # fanned tail
             cube('leg_left', [0.5, 0, 0], [1, 3, 1], '#3a3a40', rim=False),
             cube('leg_right', [-1.5, 0, 0], [1, 3, 1], '#3a3a40', rim=False)]
    build('carrion_crow', bones, cubes, 71, os.path.join(ASSETS, 'geo', 'entity', 'carrion_crow.geo.json'),
          os.path.join(ASSETS, 'textures', 'entity', 'carrion_crow.png'), 'geometry.carrion_crow', bounds=(1, 1, 0.25), mats=CROW_MATS)


def system5_animations():
    def write(name, anims):
        write_json(os.path.join(ASSETS, 'animations', 'entity', f'{name}.animation.json'),
                   {'format_version': '1.8.0', 'animations': anims, 'geckolib_format_version': 2})
    humanoid = {
        'misc.idle': {'loop': True, 'animation_length': 4.0, 'bones': {
            'skull': {'rotation': [swing(3, 90, 90), swing(5, 45), 0]},
            'right_arm': {'rotation': [swing(2, 90), 0, swing(1.5, 90)]},
            'left_arm': {'rotation': [swing(2, 90, 180), 0, swing(-1.5, 90)]}}},
        'move.walk': {'loop': True, 'animation_length': 1.2, 'bones': {
            'right_leg': {'rotation': [swing(22, 300), 0, 0]},
            'left_leg': {'rotation': [swing(22, 300, 180), 0, 0]},
            'right_arm': {'rotation': [swing(16, 300, 180), 0, 0]},
            'left_arm': {'rotation': [swing(16, 300), 0, 0]},
            'torso': {'position': [0, f'math.abs({swing(0.5, 300)})', 0]},
            'skull': {'rotation': [swing(2, 600), 0, 0]}}},
        'attack.bite': {'loop': False, 'animation_length': 0.5, 'bones': {
            'right_arm': {'rotation': {'0.0': [0, 0, 0], '0.15': [-110, 0, 10], '0.3': [20, 0, 0], '0.5': [0, 0, 0]}},
            'torso': {'rotation': {'0.0': [0, 0, 0], '0.15': [0, -10, 0], '0.3': [0, 15, 0], '0.5': [0, 0, 0]}}}},
    }
    for n in ('plague_bearer', 'gravedigger', 'censer_monk', 'ratcatcher', 'mourner', 'bone_collector'):
        write(n, humanoid)
    flag = json.loads(json.dumps(humanoid))
    flag['attack.bite']['bones']['left_arm'] = {'rotation': {'0.0': [0, 0, 0], '0.2': [-140, 0, -20], '0.35': [30, 0, 0], '0.5': [0, 0, 0]}}
    write('flagellant', flag)
    float_bob = {'body': {'position': [0, f'{swing(0.8, 120)} + 2', 0]},
                 'wisp_left': {'rotation': [swing(10, 160), 0, swing(8, 120)]},
                 'wisp_right': {'rotation': [swing(10, 160, 180), 0, swing(-8, 120)]},
                 'tail': {'rotation': [swing(6, 90), 0, swing(10, 140)]}}
    write('miasma_wraith', {
        'misc.idle': {'loop': True, 'animation_length': 3.0, 'bones': float_bob},
        'move.walk': {'loop': True, 'animation_length': 3.0, 'bones': dict(float_bob, body={'position': [0, f'{swing(0.8, 240)} + 2', 0], 'rotation': [12, 0, 0]})},
        'attack.bite': {'loop': False, 'animation_length': 0.5, 'bones': {
            'wisp_left': {'rotation': {'0.0': [0, 0, 0], '0.2': [-90, 0, 0], '0.5': [0, 0, 0]}},
            'wisp_right': {'rotation': {'0.0': [0, 0, 0], '0.2': [-90, 0, 0], '0.5': [0, 0, 0]}}}}})
    pulse = {'sac': {'scale': [f'1 + {swing(0.05, 180)}', f'1 + {swing(0.09, 180)}', f'1 + {swing(0.05, 180)}']}}
    write('pustule', {'misc.idle': {'loop': True, 'animation_length': 2.0, 'bones': pulse},
                      'move.walk': {'loop': True, 'animation_length': 2.0, 'bones': pulse},
                      'attack.bite': {'loop': False, 'animation_length': 0.3, 'bones': {'sac': {'scale': {'0.0': [1, 1, 1], '0.15': [1.15, 1.25, 1.15], '0.3': [1, 1, 1]}}}}})
    rat = json.load(open(os.path.join(ASSETS, 'animations', 'entity', 'plague_rat.animation.json')))['animations']
    for n in ('sewer_rat', 'burrower_rat'):
        write(n, rat)
    bloated = json.loads(json.dumps(rat))
    bloated['misc.swell'] = {'loop': True, 'animation_length': 0.5, 'bones': {
        'belly': {'scale': [f'1.2 + {swing(0.15, 720)}', f'1.25 + {swing(0.15, 720)}', f'1.15 + {swing(0.1, 720)}']}}}
    write('bloated_rat', bloated)
    scurry = {f'r{i}': {'position': [swing(0.6, 900, i * 50), f'math.abs({swing(0.4, 900, i * 70)})', swing(0.8, 600, i * 40)]} for i in range(7)}
    idle_sw = {f'r{i}': {'position': [swing(0.3, 200, i * 50), 0, swing(0.3, 150, i * 40)]} for i in range(7)}
    write('rat_swarm', {'misc.idle': {'loop': True, 'animation_length': 2.0, 'bones': idle_sw},
                        'move.walk': {'loop': True, 'animation_length': 0.4, 'bones': scurry}})
    write('carrion_crow', {
        'misc.idle': {'loop': True, 'animation_length': 2.0, 'bones': {'skull': {'rotation': [swing(8, 360), swing(20, 90), 0]}}},
        'move.fly': {'loop': True, 'animation_length': 0.4, 'bones': {
            'wing_left': {'rotation': [0, 0, f'-20 + {swing(50, 900)}']},
            'wing_right': {'rotation': [0, 0, f'20 - {swing(50, 900)}']},
            'leg_left': {'rotation': [60, 0, 0]}, 'leg_right': {'rotation': [60, 0, 0]},
            'body': {'position': [0, swing(0.5, 900), 0]}}},
        'attack.bite': {'loop': False, 'animation_length': 0.3, 'bones': {
            'skull': {'rotation': {'0.0': [0, 0, 0], '0.1': [35, 0, 0], '0.3': [0, 0, 0]}}}}})


def system5_icons():
    items = os.path.join(ASSETS, 'textures', 'item')
    sprite(os.path.join(items, 'pestilent_ichor.png'), [
        '................', '................', '.......g........', '......gGg.......', '......gGg.......', '.....gGGGg......',
        '....gGGhGGg.....', '....gGhGGGg.....', '...gGGGGGGGg....', '...gGGGGGgGg....', '...gGGGGGGGg....', '....gGGGGGg.....',
        '.....ggggg......', '................', '................', '................'], {'g': '#4a5020', 'G': '#7a8a30', 'h': '#c8d870'})
    def effect(name, draw):
        img = [[(0, 0, 0, 0)] * 18 for _ in range(18)]
        draw(img)
        write_png(os.path.join(ASSETS, 'textures', 'mob_effect', f'{name}.png'), img)
    def eye(img):
        for y in range(18):
            for x in range(18):
                d = ((x - 8.5) / 7.5) ** 2 + ((y - 8.5) / 4.5) ** 2
                if d <= 1: img[y][x] = rgba('#3a3040' if d > 0.6 else '#d8d0c8')
                if (x - 8.5) ** 2 + (y - 8.5) ** 2 <= 5: img[y][x] = rgba('#151214')
    def swirl(img):
        import math
        for t in range(120):
            a = t * 0.18; r = t * 0.065
            x, y = int(8.5 + math.cos(a) * r), int(8.5 + math.sin(a) * r)
            if 0 <= x < 18 and 0 <= y < 18: img[y][x] = rgba('#9aa84a' if t % 2 else '#5a6a2a')
    def meat(img):
        for y in range(4, 14):
            for x in range(3, 15):
                if ((x - 8.5) / 6) ** 2 + ((y - 8.5) / 5) ** 2 <= 1:
                    img[y][x] = rgba('#7a8a3a' if (x * 7 + y * 3) % 5 == 0 else '#8a5a3a')
        for x, y in ((6, 6), (10, 8), (8, 11)):
            img[y][x] = rgba('#1a1612')
    effect('dread', eye); effect('delirium', swirl); effect('rotting', meat)


# ---------------------------------------------------------------------------------------------
# System 6: bosses, crown, crypt seal
# ---------------------------------------------------------------------------------------------

def rat_king():
    # eight big rats on the spokes of a knot of tails, alternately facing along x and z
    spokes = [(0, -9, 'z-'), (0, 9, 'z+'), (-9, 0, 'x-'), (9, 0, 'x+'), (-7, -7, 'z-'), (7, 7, 'z+'), (-7, 7, 'x-'), (7, -7, 'x+')]
    bones = [{'name': 'root', 'pivot': [0, 0, 0]}, {'name': 'knot', 'parent': 'root', 'pivot': [0, 3, 0]},
             {'name': 'head', 'parent': 'root', 'pivot': [0, 8, 0]}, {'name': 'skull', 'parent': 'head', 'pivot': [0, 8, 0]}]
    def face(put, w, h):
        for y in range(h):
            for x in range(w):
                put(x, y, '#2f2b26')
        put(0, 0, 'eye'); put(w - 1, 0, 'eye')
    cubes = [cube('knot', [-3, 0, -3], [6, 5, 6], '#b07a75', noise=12),           # the tangle of tails
             cube('knot', [-4, 1, -2], [8, 2, 4], '#c48b86', noise=12),
             cube('knot', [-2, 1, -4], [4, 3, 8], '#9c6763', noise=12),
             cube('skull', [-3, 5, -3], [6, 1, 6], 'bone'),                       # crown of bone
             cube('skull', [-3, 6, -3], [1, 3, 1], 'bone', rim=False), cube('skull', [2, 6, -3], [1, 3, 1], 'bone', rim=False),
             cube('skull', [-3, 6, 2], [1, 3, 1], 'bone', rim=False), cube('skull', [2, 6, 2], [1, 3, 1], 'bone', rim=False),
             cube('skull', [-0.5, 6, -3.5], [1, 4, 1], 'bone', rim=False)]
    for i, (x, z, d) in enumerate(spokes):
        b = f'rat{i}'
        bones.append({'name': b, 'parent': 'root', 'pivot': [x, 2, z]})
        if d[0] == 'z':
            sgn = -1 if d == 'z-' else 1
            cubes += [cube(b, [x - 2, 0, z - 3 if sgn < 0 else z - 3], [4, 4, 7], '#3a302a', noise=12, decor={'up': rat_back}),
                      cube(b, [x - 1.5, 1, z - 6 if sgn < 0 else z + 4], [3, 3, 3], '#3a302a', decor={'north' if sgn < 0 else 'south': face}),
                      cube(b, [x - 0.5, 1, z - 7 if sgn < 0 else z + 7], [1, 1, 1], 'skin', rim=False)]
        else:
            sgn = -1 if d == 'x-' else 1
            cubes += [cube(b, [x - 3, 0, z - 2], [7, 4, 4], '#3a302a', noise=12, decor={'up': rat_back}),
                      cube(b, [x - 6 if sgn < 0 else x + 4, 1, z - 1.5], [3, 3, 3], '#3a302a', decor={'east' if sgn < 0 else 'west': face}),
                      cube(b, [x - 7 if sgn < 0 else x + 7, 1, z - 0.5], [1, 1, 1], 'skin', rim=False)]
    build('rat_king', bones, cubes, 801, os.path.join(ASSETS, 'geo', 'entity', 'rat_king.geo.json'),
          os.path.join(ASSETS, 'textures', 'entity', 'rat_king.png'), 'geometry.rat_king', bounds=(3, 1.5, 0.5))


def gravewarden():
    def iron_mask(put, w, h):
        for y in range(h):
            for x in range(w):
                put(x, y, '#4a4a50')
        for ex in (1, w - 3):
            put(ex, 2, '#b8c84a'); put(ex + 1, 2, '#b8c84a')
            put(ex, 3, '#2a2a30'); put(ex + 1, 3, '#2a2a30')
    herald('gravewarden', '#2a2420', '#3a2e28', [
        cube('skull', [-1.5, 25, -9], [3, 3, 5], '#5a5a62', decor={'up': beak_top}),       # iron beak
        cube('skull', [-1, 24.5, -12], [2, 2, 3], '#5a5a62'),
        cube('skull', [-4, 24, -4], [8, 8, 8], '#3a2e28', inflate=0.4, decor={'north': iron_mask}),
        cube('torso', [-4, 4, -2.7], [8, 19, 1], '#5a3d22', decor={'north': lambda put, w, h: [put(x, y, '#3a2010') for y in range(h) for x in range(w) if (x * 3 + y * 7) % 11 == 0]}),  # bloodied apron
        cube('right_arm', [-6.5, 2, -0.5], [1, 9, 1], 'cane', rim=False),                   # the cleaver
        cube('right_arm', [-8, -6, -3], [4, 9, 1], '#8f9ba3', decor={'north': lambda put, w, h: [put(x, h - 1 - x, '#5a2a20') for x in range(min(w, h))]}),
        cube('left_arm', [5.5, 6, -0.5], [1, 4, 1], '#6e6e6e', rim=False),                  # the lantern
        cube('left_arm', [4.5, 2, -1.5], [3, 4, 3], '#2a2a30', decor={'all': lambda put, w, h: [put(x, y, '#c8d070') for y in range(1, h - 1) for x in range(1, w - 1)]}),
    ], seed=802)


def rat_crown():
    names = ['armorHead', 'armorBody', 'armorRightArm', 'armorLeftArm', 'armorRightLeg', 'armorLeftLeg', 'armorRightBoot', 'armorLeftBoot']
    pivots = {'armorHead': [0, 24, 0], 'armorBody': [0, 24, 0], 'armorRightArm': [-5, 22, 0], 'armorLeftArm': [5, 22, 0],
              'armorRightLeg': [-1.9, 12, 0], 'armorLeftLeg': [1.9, 12, 0], 'armorRightBoot': [-1.9, 12, 0], 'armorLeftBoot': [1.9, 12, 0]}
    bones = [{'name': n, 'pivot': pivots[n]} for n in names]
    cubes = [cube('armorHead', [-4, 32, -4], [8, 1, 8], 'bone', inflate=0.5),
             cube('armorHead', [-4.5, 33, -4.5], [1, 3, 1], 'bone', rim=False), cube('armorHead', [3.5, 33, -4.5], [1, 3, 1], 'bone', rim=False),
             cube('armorHead', [-4.5, 33, 3.5], [1, 3, 1], 'bone', rim=False), cube('armorHead', [3.5, 33, 3.5], [1, 3, 1], 'bone', rim=False),
             cube('armorHead', [-0.5, 33, -4.5], [1, 4, 1], 'bone', rim=False),
             cube('armorHead', [-1, 31, -5.5], [2, 2, 2], 'bone', decor={'north': sockets})]
    build('rat_crown', bones, cubes, 803, os.path.join(ASSETS, 'geo', 'item', 'armor', 'rat_crown.geo.json'),
          os.path.join(ASSETS, 'textures', 'item', 'armor', 'rat_crown.png'), 'geometry.rat_crown', bounds=(2, 2.5, 1.5))


def system6_textures():
    items = os.path.join(ASSETS, 'textures', 'item')
    sprite(os.path.join(items, 'rat_crown.png'), [
        '................', '................', '................', '..b....b....b...', '..b....b....b...', '..bb..bbb..bb...',
        '..bbbbbbbbbbb...', '..bBBBBBBBBBb...', '..bBkBBBBkBBb...', '..bBBBBBBBBBb...', '..bbbbbbbbbbb...', '................',
        '................', '................', '................', '................'], {'b': '#b8ae98', 'B': '#d8cfb8', 'k': '#1a1612'})
    sprite(os.path.join(items, 'surgeons_steel.png'), [
        '................', '................', '................', '................', '.....ssssssss...', '....sSSSSSSSs...',
        '...sSSSrSSSSs...', '..sSSSSSrSSs....', '..sSSSSSSSs.....', '..ssssssss......', '................', '................',
        '................', '................', '................', '................'], {'s': '#5a6068', 'S': '#a8b4bc', 'r': '#7a2a20'})
    sprite(os.path.join(items, 'heart_of_pestilence.png'), [
        '................', '................', '...ggg...ggg....', '..gGGGg.gGGGg...', '.gGhGGGgGGGGGg..', '.gGGGGGGGGkGGg..',
        '.gGGkGGGGGGGGg..', '.gGGGGGGGGGGGg..', '..gGGGGkGGGGg...', '...gGGGGGGGg....', '....gGGGGGg.....', '.....gGGGg......',
        '......gGg.......', '.......g........', '................', '................'], {'g': '#2a3010', 'G': '#5a6a20', 'h': '#c8d870', 'k': '#1a1a10'})
    tex = os.path.join(ASSETS, 'textures', 'block')
    for name, broken in (('crypt_seal_top', False), ('crypt_seal_broken', True)):
        rng = random.Random(9 if broken else 8)
        img = [[scale(rgba('#3a3a42'), 1 + rng.uniform(-0.1, 0.1)) for _ in range(16)] for _ in range(16)]
        for i in range(16):                                         # iron bands
            img[i][3] = img[i][12] = img[3][i] = img[12][i] = rgba('#5a5a62')
        for y in range(5, 11):
            for x in range(5, 11):
                if (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 9:
                    img[y][x] = rgba('#7a1c14' if not broken else '#2a1a18')
        if not broken:
            img[7][7] = img[8][8] = img[7][8] = img[8][7] = rgba('#b8c84a')
        else:
            for k in range(16):
                img[k][(k * 7) % 16] = rgba('#141418')
        write_png(os.path.join(tex, f'{name}.png'), img)


def system6_animations():
    def write(name, anims):
        write_json(os.path.join(ASSETS, 'animations', 'entity', f'{name}.animation.json'),
                   {'format_version': '1.8.0', 'animations': anims, 'geckolib_format_version': 2})
    writhe = {f'rat{i}': {'rotation': [0, swing(8, 200, i * 45), 0], 'position': [0, f'math.abs({swing(0.3, 400, i * 60)})', 0]} for i in range(8)}
    writhe['knot'] = {'scale': [f'1 + {swing(0.06, 180)}', f'1 + {swing(0.08, 180)}', f'1 + {swing(0.06, 180)}']}
    scurry = {f'rat{i}': {'rotation': [0, swing(14, 700, i * 45), 0], 'position': [0, f'math.abs({swing(0.6, 900, i * 60)})', 0]} for i in range(8)}
    scurry['knot'] = writhe['knot']
    write('rat_king', {'misc.idle': {'loop': True, 'animation_length': 2.0, 'bones': writhe},
                       'move.walk': {'loop': True, 'animation_length': 0.4, 'bones': scurry},
                       'attack.bite': {'loop': False, 'animation_length': 0.4, 'bones': {
                           'knot': {'scale': {'0.0': [1, 1, 1], '0.15': [1.25, 1.3, 1.25], '0.4': [1, 1, 1]}},
                           'skull': {'position': {'0.0': [0, 0, 0], '0.15': [0, 1.5, 0], '0.4': [0, 0, 0]}}}}})
    heavy = json.load(open(os.path.join(ASSETS, 'animations', 'entity', 'gravedigger.animation.json')))['animations']
    heavy['move.walk']['animation_length'] = 1.6
    heavy['attack.bite'] = {'loop': False, 'animation_length': 0.8, 'bones': {
        'right_arm': {'rotation': {'0.0': [0, 0, 0], '0.35': [-160, 0, 15], '0.5': [30, 0, -5], '0.8': [0, 0, 0]}},
        'torso': {'rotation': {'0.0': [0, 0, 0], '0.35': [-10, -15, 0], '0.5': [12, 20, 0], '0.8': [0, 0, 0]}}}}
    write('gravewarden', heavy)


def system7_icons():
    items = os.path.join(ASSETS, 'textures', 'item')
    icons = {
        'pyre_torch': (['................', '..........yy....', '.........yOOy...', '.........OrrO...', '........yOrrO...', '.........ccc....',
                        '........cbc.....', '.......cbc......', '......sbs.......', '.....ss.........', '....ss..........', '...ss...........',
                        '..ss............', '..s.............', '................', '................'],
                       {'y': '#f8d040', 'O': '#f08020', 'r': '#c02010', 'c': '#3a2a1a', 'b': '#5a4632', 's': '#6b4a2b'}),
        'quicklime_bomb': (['................', '.......ss.......', '......s..s......', '.......ss.......', '.....wwwwww.....', '....wWWWWWWw....',
                            '...wWWWwWWWWw...', '...wWWWWWWWWw...', '...wWWwWWWWWw...', '...wWWWWWWwWw...', '...wWWWWWWWWw...', '....wWWWWWWw....',
                            '.....wwwwww.....', '................', '................', '................'],
                           {'s': '#8a7a5a', 'w': '#a8a49a', 'W': '#f2f0ea'}),
        'fumigation_bomb': (['.....g...g......', '......g.g.......', '.......ss.......', '.......ss.......', '.....hhhhhh.....', '....hHHHHHHh....',
                             '...hHHeHHHHHh...', '...hHHHHHeHHh...', '...hHeHHHHHHh...', '...hHHHHeHHHh...', '...hHHHHHHHHh...', '....hHHHHHHh....',
                             '.....hhhhhh.....', '................', '................', '................'],
                            {'g': '#c8c8c0', 's': '#8a7a5a', 'h': '#4a5040', 'H': '#7d8a6a', 'e': '#5c8538'}),
        'consecrated_cleaver': (['................', '.......ssssss...', '......sSSSSSSs..', '......sSSSSSSs..', '......sSSySSSs..', '......sSSSSSs...',
                                 '......sSSSSs....', '......ssssg.....', '.........g......', '........bg......', '.......bg.......', '......bg........',
                                 '.....bg.........', '....bb..........', '................', '................'],
                                {'s': '#5a6068', 'S': '#c8d0d8', 'y': '#f0d060', 'g': '#c8a040', 'b': '#5a3d22'}),
        'pomander': (['................', '.......cc.......', '.......c........', '......gGg.......', '.....gGhGg......', '....gGhGGhg.....',
                      '....gGGGGGg.....', '....ghGGhGg.....', '....gGGGGGg.....', '.....gGhGg......', '......ggg.......', '................',
                      '................', '................', '................', '................'],
                     {'c': '#6e6e6e', 'g': '#8a6a22', 'G': '#d8b048', 'h': '#3a5a2a'}),
        'plaguebane_amulet': (['....c......c....', '.....c....c.....', '......c..c......', '.......cc.......', '......gggg......', '.....gGGGGg.....',
                               '....gGeeeeGg....', '....gGeEEeGg....', '....gGeEEeGg....', '....gGeeeeGg....', '.....gGGGGg.....', '......gggg......',
                               '................', '................', '................', '................'],
                              {'c': '#c8a040', 'g': '#8a6a22', 'G': '#e8c050', 'e': '#3a6a20', 'E': '#a8e060'}),
    }
    for name, (rows, key) in icons.items():
        sprite(os.path.join(items, f'{name}.png'), rows, key)


if __name__ == '__main__':
    plague_doctor()
    plague_mask()
    plague_rat()
    animations()
    icons()
    block_textures()
    nest_models()
    herbs()
    apothecary_icons()
    doctors_garb()
    apothecary_models()
    plague_cross()
    heralds()
    miasma_wraith()
    pustule()
    vermin()
    system5_icons()
    rat_king()
    gravewarden()
    rat_crown()
    system6_textures()
    system7_icons()
    import vanilla_afflicted
    vanilla_afflicted.build_all()
    system5_animations()
    system6_animations()
