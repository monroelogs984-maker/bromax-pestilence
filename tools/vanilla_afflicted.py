#!/usr/bin/env python3
"""Afflicted animals built from the real vanilla models and textures (Minecraft 1.21.1).

Geometry is transcribed from the vanilla Java model classes (CowModel, PigModel, ...) and
converted to GeckoLib/Bedrock geometry with vanilla box UVs:

    pivot  = (px, 24 - py, pz)                  (Java part offsets accumulated, y-down -> y-up)
    origin = (px + x, 24 - (py + y + h), pz + z)
    rotation (degrees) = Java rotation in degrees, same signs

(Derived from GeckoLib's bake step: it negates bone X/Y rotation and mirrors X, which is exactly
what vanilla's scale(-1, -1, 1) does to Java parts.)

GeckoLib's head-turn *replaces* the "head" bone's rotation, so every model gets a bare "head"
pivot bone with the actual head parts in a "skull" child, which keeps vanilla's base angles and
carries the idle/bite animations.

Textures are the vanilla ones read from the client jar, sickened: drained colour, sores, exposed
ribs, yellow-green eyes, and a strip appended at the bottom for buboes and tusks.
"""
import math
import os
import random
import sys
import zipfile

sys.path.insert(0, os.path.dirname(__file__))
from gen_assets import ASSETS, write_json, write_png  # noqa: E402
from preview import read_png  # noqa: E402

JAR = os.path.expanduser('~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar')
SCRATCH = os.environ.get('TMPDIR', '/tmp')

SICK_TINT = (107, 115, 48)
BUBO = [(42, 28, 34, 255), (58, 40, 48, 255), (30, 20, 26, 255)]
BONE = (216, 207, 184, 255)
GLOW = (212, 217, 74, 255)
SORE = [(140, 138, 58, 255), (110, 42, 36, 255)]
EXTRA_ROWS = 16


def jar_png(path):
    data = zipfile.ZipFile(JAR).read(f'assets/minecraft/textures/{path}.png')
    tmp = os.path.join(SCRATCH, '_vanilla.png')
    open(tmp, 'wb').write(data)
    return read_png(tmp)


# ------------------------------------------------------------------------------------------------
# Geometry
# ------------------------------------------------------------------------------------------------

def part(name, offset, boxes=(), rot=(0, 0, 0), parent=None, rename=None):
    """A Java model part. boxes: (u, v, x, y, z, w, h, d[, inflate[, mirror]]). rot in radians."""
    return dict(name=name, offset=offset, boxes=list(boxes), rot=rot, parent=parent, rename=rename or name)


def convert(parts, extra_cubes):
    """Java parts -> GeckoLib bones. extra_cubes: {bone: [(origin, size, (u, v))]} in final model space."""
    by_name = {p['name']: p for p in parts}

    def absolute(p):
        x, y, z = p['offset']
        if p['parent']:
            ax, ay, az = absolute(by_name[p['parent']])
            return ax + x, ay + y, az + z
        return x, y, z

    bones = [{'name': 'root', 'pivot': [0, 0, 0]}]
    for p in parts:
        px, py, pz = absolute(p)
        bone = {'name': p['rename'], 'parent': by_name[p['parent']]['rename'] if p['parent'] else 'root',
                'pivot': [px, 24 - py, pz]}
        if any(p['rot']):
            bone['rotation'] = [round(math.degrees(r), 4) for r in p['rot']]
        cubes = []
        for b in p['boxes']:
            u, v, x, y, z, w, h, d = b[:8]
            inflate = b[8] if len(b) > 8 else 0
            mirror = b[9] if len(b) > 9 else False
            cube = {'origin': [px + x, 24 - (py + y + h), pz + z], 'size': [w, h, d], 'uv': [u, v]}
            if inflate:
                cube['inflate'] = inflate
            if mirror:
                cube['mirror'] = True
            cubes.append(cube)
        for origin, size, uv in extra_cubes.get(p['rename'], []):
            cubes.append({'origin': list(origin), 'size': list(size), 'uv': list(uv)})
        if cubes:
            bone['cubes'] = cubes
        bones.append(bone)
    return bones


def with_skull(parts, head='head'):
    """Split the head part into a bare 'head' pivot (for look-turning) and a 'skull' holding everything else."""
    out = []
    for p in parts:
        if p['name'] == head:
            out.append(part(head, p['offset'], parent=p['parent']))
            out.append(part('skull', (0, 0, 0), p['boxes'], p['rot'], parent=head))
        else:
            out.append(dict(p, parent='skull' if p['parent'] == head else p['parent']))
    return out


def sores_bone(body_name, body_pivot, body_rot_deg):
    """A child of the (rotated) body that cancels its rotation, so buboes can be placed in plain model space."""
    bone = {'name': 'sores', 'parent': body_name, 'pivot': body_pivot}
    if body_rot_deg:
        bone['rotation'] = [-body_rot_deg, 0, 0]
    return bone


# ------------------------------------------------------------------------------------------------
# Texture work
# ------------------------------------------------------------------------------------------------

def faces(u, v, w, h, d):
    """Box UV rects (x, y, w, h) per face, Bedrock/Java layout."""
    return {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d),
            'east': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
            'west': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h)}


def sicken(img, rng, strength=1.0):
    out = []
    for row in img:
        new = []
        for r, g, b, a in row:
            if a == 0:
                new.append((r, g, b, a))
                continue
            lum = 0.3 * r + 0.59 * g + 0.11 * b
            k = 0.45 * strength
            c = [v * (1 - k) + lum * k for v in (r, g, b)]
            c = [v * 0.82 for v in c]
            c = [v * (1 - 0.14 * strength) + t * 0.14 * strength for v, t in zip(c, SICK_TINT)]
            n = 1 + rng.uniform(-0.06, 0.06)
            new.append(tuple(max(0, min(255, int(v * n))) for v in c) + (a,))
        out.append(new)
    return out


def darken(img, x, y, f):
    r, g, b, a = img[y][x]
    if a:
        img[y][x] = (int(r * f), int(g * f), int(b * f), a)


def ribs(img, rect, along='rows'):
    """Exposed ribs across a flank rect: dark lines every 2px over the middle of the body."""
    x0, y0, w, h = rect
    if along == 'rows':     # body length runs down the rect (rotated vanilla bodies)
        for y in range(y0 + int(h * 0.25), y0 + int(h * 0.72), 2):
            for x in range(x0 + int(w * 0.2), x0 + int(w * 0.8)):
                darken(img, x, y, 0.55)
    else:                   # body length runs across the rect
        for x in range(x0 + int(w * 0.25), x0 + int(w * 0.72), 2):
            for y in range(y0 + int(h * 0.2), y0 + int(h * 0.8)):
                darken(img, x, y, 0.55)


def sores(img, rect, rng, n):
    x0, y0, w, h = rect
    for _ in range(n):
        x, y = x0 + rng.randrange(w), y0 + rng.randrange(h)
        if img[y][x][3]:
            img[y][x] = SORE[0]
            if x + 1 < x0 + w and img[y][x + 1][3]:
                img[y][x + 1] = SORE[1]


def glow_eyes(img, rect, upper=0.6, per_half=True, count=1):
    """Turn the darkest pixel(s) in the top part of a face (the eyes) sick yellow-green.
    per_half: one eye in each half of a front face; otherwise the darkest `count` in the whole rect (a side face)."""
    x0, y0, w, h = rect
    rows = range(y0, y0 + max(1, int(h * upper)))
    spans = [(x0, x0 + w // 2), (x0 + w - w // 2, x0 + w)] if per_half else [(x0, x0 + w)]
    for a, b in spans:
        px = sorted(((0.3 * img[y][x][0] + 0.59 * img[y][x][1] + 0.11 * img[y][x][2], x, y)
                     for y in rows for x in range(a, b) if img[y][x][3]))
        for _, x, y in px[:count]:
            img[y][x] = GLOW


def patches(img, rect, rng, n, color=None, alpha_hole=False, size=2):
    x0, y0, w, h = rect
    for _ in range(n):
        x, y = x0 + rng.randrange(w), y0 + rng.randrange(h)
        for dx in range(size):
            for dy in range(size):
                if x + dx < x0 + w and y + dy < y0 + h:
                    img[y + dy][x + dx] = (0, 0, 0, 0) if alpha_hole else color


def extend(img, rng):
    """Append EXTRA_ROWS rows: bubo skin at x 0-15, bone (tusks) at x 16-31."""
    w = len(img[0])
    extra = [[(0, 0, 0, 0)] * w for _ in range(EXTRA_ROWS)]
    for y in range(EXTRA_ROWS):
        for x in range(16):
            extra[y][x] = rng.choice(BUBO)
            extra[y][16 + x] = BONE
    return img + extra


def write(name, bones, img):
    h = len(img)
    write_json(os.path.join(ASSETS, 'geo', 'entity', f'{name}.geo.json'), {
        'format_version': '1.12.0',
        'minecraft:geometry': [{
            'description': {'identifier': f'geometry.{name}', 'texture_width': len(img[0]), 'texture_height': h,
                            'visible_bounds_width': 3, 'visible_bounds_height': 3, 'visible_bounds_offset': [0, 1, 0]},
            'bones': bones}]})
    write_png(os.path.join(ASSETS, 'textures', 'entity', f'{name}.png'), img)
    print(f'{name}: vanilla geometry, texture {len(img[0])}x{h}')


# ------------------------------------------------------------------------------------------------
# The animals (part definitions transcribed from the 1.21.1 vanilla model classes)
# ------------------------------------------------------------------------------------------------

HALF_PI = math.pi / 2


def cow():
    rng = random.Random(1)
    base = 32  # vanilla texture height; extras start here
    parts = with_skull([
        part('head', (0, 4, -8), [(0, 0, -4, -4, -6, 8, 8, 6), (22, 0, -5, -5, -4, 1, 3, 1),
                                   (22, 0, 4, -3, -4, 1, 1, 1)]),            # left horn snapped to a stub
        part('body', (0, 5, 2), [(18, 4, -6, -10, -7, 12, 18, 10), (52, 0, -2, 2, -8, 4, 6, 1)], rot=(HALF_PI, 0, 0)),
        part('right_hind_leg', (-4, 12, 7), [(0, 16, -2, 0, -2, 4, 12, 4)]),
        part('left_hind_leg', (4, 12, 7), [(0, 16, -2, 0, -2, 4, 12, 4)]),
        part('right_front_leg', (-4, 12, -6), [(0, 16, -2, 0, -2, 4, 12, 4)]),
        part('left_front_leg', (4, 12, -6), [(0, 16, -2, 0, -2, 4, 12, 4)]),
    ])
    bones = convert(parts, {})
    bones.append(dict(sores_bone('body', [0, 19, 2], 90), cubes=[
        {'origin': [6, 15, -3], 'size': [1, 2, 2], 'uv': [0, base]},
        {'origin': [-7, 17, 3], 'size': [1, 2, 2], 'uv': [0, base]},
        {'origin': [-1, 11, -7], 'size': [2, 1, 2], 'uv': [0, base]}]))
    img = sicken(jar_png('entity/cow/cow'), rng)
    body = faces(18, 4, 12, 18, 10)
    ribs(img, body['east']); ribs(img, body['west'])
    sores(img, body['north'], rng, 5); sores(img, body['east'], rng, 3); sores(img, body['west'], rng, 3)
    glow_eyes(img, faces(0, 0, 8, 8, 6)['north'], upper=0.55)
    write('afflicted_cow', bones, extend(img, rng))


def pig():
    rng = random.Random(2)
    base = 32
    parts = with_skull([
        part('head', (0, 12, -6), [(0, 0, -4, -4, -8, 8, 8, 8), (16, 16, -2, 0, -9, 4, 3, 1)]),
        part('body', (0, 11, 2), [(28, 8, -5, -10, -7, 10, 16, 8)], rot=(HALF_PI, 0, 0)),
        part('right_hind_leg', (-3, 18, 7), [(0, 16, -2, 0, -2, 4, 6, 4)]),
        part('left_hind_leg', (3, 18, 7), [(0, 16, -2, 0, -2, 4, 6, 4)]),
        part('right_front_leg', (-3, 18, -5), [(0, 16, -2, 0, -2, 4, 6, 4)]),
        part('left_front_leg', (3, 18, -5), [(0, 16, -2, 0, -2, 4, 6, 4)]),
    ])
    tusks = [((-3, 9, -15), (1, 2, 1), (16, base)), ((2, 9, -15), (1, 2, 1), (16, base))]
    bones = convert(parts, {'skull': tusks})
    bones.append(dict(sores_bone('body', [0, 13, 2], 90), cubes=[
        {'origin': [5, 9, -2], 'size': [1, 2, 2], 'uv': [0, base]},
        {'origin': [-6, 10, 3], 'size': [1, 2, 2], 'uv': [0, base]},
        {'origin': [1, 14, 0], 'size': [2, 1, 2], 'uv': [0, base]}]))
    img = sicken(jar_png('entity/pig/pig'), rng)
    body = faces(28, 8, 10, 16, 8)
    for f in ('east', 'west', 'north'):
        patches(img, body[f], rng, 4, color=(110, 72, 70, 255))
        sores(img, body[f], rng, 3)
    glow_eyes(img, faces(0, 0, 8, 8, 8)['north'], upper=0.6)
    write('afflicted_pig', bones, extend(img, rng))


def sheep():
    rng = random.Random(3)
    skin = sicken(jar_png('entity/sheep/sheep'), rng)
    fur = jar_png('entity/sheep/sheep_fur')
    # The fleece is vanilla's white fur layer, dyed a filthy grey-brown and torn through in places.
    dirty = (156, 149, 127)
    fur = [[(int(r * dirty[0] / 255), int(g * dirty[1] / 255), int(b * dirty[2] / 255), a) if a else (r, g, b, a)
            for r, g, b, a in row] for row in fur]
    fur = sicken(fur, rng, 0.5)
    fbody = faces(28, 8, 8, 16, 6)
    for f in ('east', 'west', 'north', 'south', 'up'):
        patches(fur, fbody[f], rng, 9, alpha_hole=True, size=3)
        patches(fur, fbody[f], rng, 6, color=(92, 84, 66, 255))       # matted, filthy clumps
    img = skin + fur                               # fur sits 32 rows below the skin
    base = 64
    parts = with_skull([
        part('head', (0, 6, -8), [(0, 0, -3, -4, -6, 6, 6, 8), (0, 32, -3, -4, -4, 6, 6, 6, 0.6)]),
        part('body', (0, 5, 2), [(28, 8, -4, -10, -7, 8, 16, 6), (28, 40, -4, -10, -7, 8, 16, 6, 1.75)], rot=(HALF_PI, 0, 0)),
        part('right_hind_leg', (-3, 12, 7), [(0, 16, -2, 0, -2, 4, 12, 4), (0, 48, -2, 0, -2, 4, 6, 4, 0.5)]),
        part('left_hind_leg', (3, 12, 7), [(0, 16, -2, 0, -2, 4, 12, 4), (0, 48, -2, 0, -2, 4, 6, 4, 0.5)]),
        part('right_front_leg', (-3, 12, -5), [(0, 16, -2, 0, -2, 4, 12, 4), (0, 48, -2, 0, -2, 4, 6, 4, 0.5)]),
        part('left_front_leg', (3, 12, -5), [(0, 16, -2, 0, -2, 4, 12, 4), (0, 48, -2, 0, -2, 4, 6, 4, 0.5)]),
    ])
    bones = convert(parts, {})
    bones.append(dict(sores_bone('body', [0, 19, 2], 90), cubes=[
        {'origin': [5.5, 14, 0], 'size': [1, 2, 2], 'uv': [0, base]},
        {'origin': [-6.5, 15, 2], 'size': [1, 2, 2], 'uv': [0, base]}]))
    sbody = faces(28, 8, 8, 16, 6)
    ribs(img, sbody['east']); ribs(img, sbody['west'])
    glow_eyes(img, faces(0, 0, 6, 6, 8)['north'], upper=0.6)
    write('afflicted_sheep', bones, extend(img, rng))


def chicken():
    rng = random.Random(4)
    base = 32
    parts = with_skull([
        part('head', (0, 15, -4), [(0, 0, -2, -6, -2, 4, 6, 3)]),
        part('beak', (0, 0, 0), [(14, 0, -2, -4, -4, 4, 2, 2)], parent='head'),
        part('red_thing', (0, 0, 0), [(14, 4, -1, -2, -3, 2, 2, 2)], parent='head'),
        part('body', (0, 16, 0), [(0, 9, -3, -4, -3, 6, 8, 6)], rot=(HALF_PI, 0, 0)),
        part('right_leg', (-2, 19, 1), [(26, 0, -1, 0, -3, 3, 5, 3)]),
        part('left_leg', (1, 19, 1), [(26, 0, -1, 0, -3, 3, 5, 3)]),
        part('right_wing', (-4, 13, 0), [(24, 13, 0, 0, -3, 1, 4, 6)]),
        part('left_wing', (4, 13, 0), [(24, 13, -1, 0, -3, 1, 4, 6)]),
    ])
    bones = convert(parts, {})
    bones.append(dict(sores_bone('body', [0, 8, 0], 90), cubes=[{'origin': [-1, 6, -5], 'size': [2, 2, 1], 'uv': [0, base]}]))
    img = sicken(jar_png('entity/chicken'), rng, 0.8)
    body = faces(0, 9, 6, 8, 6)
    for f in ('east', 'west', 'north', 'south'):
        patches(img, body[f], rng, 3, color=(196, 139, 134, 255))     # plucked raw skin
    wing = faces(24, 13, 1, 4, 6)
    patches(img, wing['east'], rng, 2, color=(196, 139, 134, 255))
    glow_eyes(img, faces(0, 0, 4, 6, 3)['north'], upper=0.6)
    write('afflicted_chicken', bones, extend(img, rng))


def wolf():
    rng = random.Random(5)
    base = 32
    parts = [
        part('head', (-1, 13.5, -7)),
        part('skull', (0, 0, 0), [(0, 0, -2, -3, -2, 6, 6, 4), (16, 14, -2, -5, 0, 2, 2, 1), (16, 14, 2, -5, 0, 2, 2, 1),
                                  (0, 10, -0.5, -0.001, -5, 3, 3, 4)], parent='head'),
        part('body', (0, 14, 2), [(18, 14, -3, -2, -3, 6, 9, 6)], rot=(HALF_PI, 0, 0)),
        part('upper_body', (-1, 14, -3), [(21, 0, -3, -3, -3, 8, 6, 7)], rot=(HALF_PI, 0, 0)),
        part('right_hind_leg', (-2.5, 16, 7), [(0, 18, 0, 0, -1, 2, 8, 2)]),
        part('left_hind_leg', (0.5, 16, 7), [(0, 18, 0, 0, -1, 2, 8, 2)]),
        part('right_front_leg', (-2.5, 16, -4), [(0, 18, 0, 0, -1, 2, 8, 2)]),
        part('left_front_leg', (0.5, 16, -4), [(0, 18, 0, 0, -1, 2, 8, 2)]),
        part('tail', (-1, 12, 8), rot=(math.pi / 5, 0, 0)),
        part('real_tail', (0, 0, 0), [(9, 18, 0, 0, -1, 2, 8, 2)], parent='tail'),
    ]
    bones = convert(parts, {})
    bones.append(dict(sores_bone('body', [0, 10, 2], 90), cubes=[
        {'origin': [3, 9, 3], 'size': [1, 2, 2], 'uv': [0, base]},
        {'origin': [-4, 10, 5], 'size': [1, 1, 1], 'uv': [0, base]}]))
    img = sicken(jar_png('entity/wolf/wolf'), rng)
    body = faces(18, 14, 6, 9, 6)
    ribs(img, body['east']); ribs(img, body['west'])
    mane = faces(21, 0, 8, 6, 7)
    for f in ('east', 'west', 'up'):
        patches(img, mane[f], rng, 3, color=(122, 90, 82, 255))     # mange
    glow_eyes(img, faces(0, 0, 6, 6, 4)['north'], upper=0.6)
    write('afflicted_wolf', bones, extend(img, rng))


def horse():
    rng = random.Random(6)
    base = 64
    parts = [
        part('body', (0, 11, 5), [(0, 32, -5, -8, -17, 10, 10, 22, 0.05)]),
        part('tail', (0, -5, 2), [(42, 36, -1.5, 0, 0, 3, 14, 4)], rot=(math.pi / 6, 0, 0), parent='body'),
        # vanilla's head_parts, split: bare 'head' for look-turning, 'skull' keeps the 30 degree neck angle
        part('head', (0, 4, -12)),
        part('skull', (0, 0, 0), [(0, 35, -2.05, -6, -2, 4, 12, 7)], rot=(math.pi / 6, 0, 0), parent='head'),
        part('face', (0, 0, 0), [(0, 13, -3, -11, -2, 6, 5, 7)], parent='skull'),
        part('mane', (0, 0, 0), [(56, 36, -1, -11, 5.01, 2, 16, 2)], parent='skull'),
        part('upper_mouth', (0, 0, 0), [(0, 25, -2, -11, -7, 4, 5, 5)], parent='skull'),
        part('left_ear', (0, 0, 0), [(19, 16, 0.55, -13, 4, 2, 3, 1, -0.001)], parent='face'),
        part('right_ear', (0, 0, 0), [(19, 16, -2.55, -13, 4, 2, 3, 1, -0.001)], parent='face'),
        part('left_hind_leg', (4, 14, 7), [(48, 21, -3, -1.01, -1, 4, 11, 4, 0, True)]),
        part('right_hind_leg', (-4, 14, 7), [(48, 21, -1, -1.01, -1, 4, 11, 4)]),
        part('left_front_leg', (4, 14, -12), [(48, 21, -3, -1.01, -1.9, 4, 11, 4, 0, True)]),
        part('right_front_leg', (-4, 14, -12), [(48, 21, -1, -1.01, -1.9, 4, 11, 4)]),
    ]
    bones = convert(parts, {'body': [((5, 15, -5), (1, 2, 2), (0, base)), ((-6, 16, 3), (1, 2, 2), (0, base))]})
    img = sicken(jar_png('entity/horse/horse_brown'), rng)
    body = faces(0, 32, 10, 10, 22)
    ribs(img, body['east'], along='cols'); ribs(img, body['west'], along='cols')
    sores(img, body['up'], rng, 6)
    face = faces(0, 13, 6, 5, 7)
    glow_eyes(img, face['east'], upper=0.7, per_half=False, count=2); glow_eyes(img, face['west'], upper=0.7, per_half=False, count=2)
    write('afflicted_horse', bones, extend(img, rng))


# ------------------------------------------------------------------------------------------------
# Animations (vanilla bone names; head look-turning is GeckoLib's, so motion goes on 'skull')
# ------------------------------------------------------------------------------------------------

def swing(amount, speed, phase=0):
    return f'math.sin(query.anim_time * {speed} + {phase}) * {amount}'


def quad(stride, period, head_drop=0, extra_walk=None, extra_idle=None, bite=None):
    speed = 360 / period
    walk = {
        # vanilla gait: diagonal pairs. The right hind leg limps (shorter swing).
        'right_hind_leg': {'rotation': [swing(stride * 0.55, speed), 0, 0]},
        'left_front_leg': {'rotation': [swing(stride, speed), 0, 0]},
        'left_hind_leg': {'rotation': [swing(stride, speed, 180), 0, 0]},
        'right_front_leg': {'rotation': [swing(stride, speed, 180), 0, 0]},
        'body': {'position': [0, f'math.abs({swing(0.4, speed)})', 0]},
        'skull': {'rotation': [f'{head_drop} + {swing(4, speed * 2)}', 0, 0]},
    }
    walk.update(extra_walk or {})
    idle = {'skull': {'rotation': [f'{head_drop} + {swing(3, 90)}', swing(5, 40), swing(1.5, 400)]}}
    idle.update(extra_idle or {})
    return {
        'misc.idle': {'loop': True, 'animation_length': 4.0, 'bones': idle},
        'move.walk': {'loop': True, 'animation_length': period, 'bones': walk},
        'attack.bite': {'loop': False, 'animation_length': 0.4, 'bones': bite or {
            'skull': {'rotation': {'0.0': [0, 0, 0], '0.12': [-20, 0, 0], '0.25': [18, 0, 0], '0.4': [0, 0, 0]}}}},
    }


def animations():
    kick = {'0.0': [0, 0, 0], '0.15': [-45, 0, 0], '0.3': [10, 0, 0], '0.4': [0, 0, 0]}
    anims = {
        'afflicted_cow': quad(22, 1.4, head_drop=8),
        'afflicted_pig': quad(30, 0.8),
        'afflicted_sheep': quad(24, 1.1, head_drop=6),
        'afflicted_wolf': quad(35, 0.7, extra_walk={'real_tail': {'rotation': [0, 0, swing(15, 514)]}},
                               extra_idle={'real_tail': {'rotation': [0, 0, swing(8, 120)]}}),
        'afflicted_horse': quad(28, 1.2, extra_walk={'tail': {'rotation': [swing(8, 300), 0, 0]}}, bite={
            'left_front_leg': {'rotation': kick}, 'right_front_leg': {'rotation': kick},
            'body': {'rotation': {'0.0': [0, 0, 0], '0.15': [-12, 0, 0], '0.3': [3, 0, 0], '0.4': [0, 0, 0]}}}),
        'afflicted_chicken': {
            'misc.idle': {'loop': True, 'animation_length': 2.0, 'bones': {'skull': {'rotation': [swing(10, 360), swing(8, 90), 0]}}},
            'move.walk': {'loop': True, 'animation_length': 0.4, 'bones': {
                'right_leg': {'rotation': [swing(35, 900), 0, 0]},
                'left_leg': {'rotation': [swing(35, 900, 180), 0, 0]},
                'right_wing': {'rotation': [0, 0, f'math.abs({swing(25, 900)})']},
                'left_wing': {'rotation': [0, 0, f'-math.abs({swing(25, 900)})']},
                'skull': {'position': [0, 0, swing(0.6, 900)]}}},
            'attack.bite': {'loop': False, 'animation_length': 0.3, 'bones': {
                'skull': {'rotation': {'0.0': [0, 0, 0], '0.1': [35, 0, 0], '0.2': [-5, 0, 0], '0.3': [0, 0, 0]}}}},
        },
    }
    for name, a in anims.items():
        write_json(os.path.join(ASSETS, 'animations', 'entity', f'{name}.animation.json'),
                   {'format_version': '1.8.0', 'animations': a, 'geckolib_format_version': 2})


def build_all():
    cow()
    pig()
    sheep()
    chicken()
    wolf()
    horse()
    animations()


if __name__ == '__main__':
    build_all()
