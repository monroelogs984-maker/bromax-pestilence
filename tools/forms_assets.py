"""Models, textures and animations for the Forms (Miasma Clot, Gnawing Mass, Choking Shade,
Blackened, Mouldhusk). Uses gen_assets' cube painter; run on its own so nothing else is rebuilt:

    python3 tools/forms_assets.py

Tone rule (Glenn): plague concentrated, not body horror. Silhouettes of rats, rags, soot, fog and
mould; no exposed flesh, wounds or extra eyes.
"""
import os
import random

import gen_assets as g
from gen_assets import cube, swing, build, ASSETS, write_json, hooded_face


def out(name):
    return (os.path.join(ASSETS, 'geo', 'entity', f'{name}.geo.json'),
            os.path.join(ASSETS, 'textures', 'entity', f'{name}.png'), f'geometry.{name}')


def anim(name, anims):
    write_json(os.path.join(ASSETS, 'animations', 'entity', f'{name}.animation.json'),
               {'format_version': '1.8.0', 'geckolib_format_version': 2, 'animations': anims})


def specks(color, seed, n):
    def d(put, w, h):
        rng = random.Random(seed + w * 7 + h)
        for _ in range(n):
            put(rng.randrange(w), rng.randrange(h), color)
    return d


def cracks(color, seed):
    """A few short ember cracks on a face (the Blackened's soot skin); narrow faces often get none."""
    def d(put, w, h):
        rng = random.Random(seed + w * 13 + h)
        if w < 4 and rng.random() < 0.5:
            return
        x, y = rng.randrange(w), rng.randrange(max(1, h // 2))
        for _ in range(max(2, h // 3)):
            put(x % w, y % h, color)
            y += 1
            x += rng.choice((-1, 0, 0, 1))
    return d


def eyes(color, y=2, inset=1):
    def d(put, w, h):
        put(inset, min(h - 1, y), color)
        put(w - 1 - inset, min(h - 1, y), color)
    return d


def both(*decors):
    def d(put, w, h):
        for f in decors:
            f(put, w, h)
    return d


# ---------------------------------------------------------------------------------------------

def miasma_clot():
    """A knot of plague fog with a few dim motes inside; drawn translucent and scaled by mass."""
    bones = [{'name': 'root', 'pivot': [0, 0, 0]},
             {'name': 'body', 'parent': 'root', 'pivot': [0, 6, 0]},
             {'name': 'puff_a', 'parent': 'body', 'pivot': [5, 9, 0]},
             {'name': 'puff_b', 'parent': 'body', 'pivot': [-5, 7, 2]},
             {'name': 'puff_c', 'parent': 'body', 'pivot': [0, 12, -3]}]
    fog, fog2, fog3 = '#6b7340@170', '#8a9258@130', '#a8b070@100'
    cubes = [
        cube('body', [-5, 1, -5], [10, 9, 10], fog, decor={'all': specks('#d4d94a@200', 3, 3)}),
        cube('body', [-6, 3, -4], [12, 5, 8], fog2),
        cube('body', [-4, 2, -6], [8, 6, 12], fog2),
        cube('puff_a', [4, 6, -2], [5, 5, 5], fog3),
        cube('puff_b', [-9, 4, 0], [5, 5, 5], fog3),
        cube('puff_c', [-2, 9, -5], [5, 4, 5], fog3),
    ]
    geo, tex, ident = out('miasma_clot')
    build('miasma_clot', bones, cubes, 701, geo, tex, ident, bounds=(2, 1.5, 0.5),
          mats={fog: 'stone', fog2: 'stone', fog3: 'stone'})
    drift = {'body': {'position': [0, f'{swing(0.6, 120)} + 0.6', 0], 'rotation': [0, f'query.anim_time * 20', 0],
                      'scale': [f'1 + {swing(0.05, 150)}', f'1 + {swing(0.08, 150)}', f'1 + {swing(0.05, 150)}']},
             'puff_a': {'position': [swing(0.8, 100), swing(0.6, 140), 0]},
             'puff_b': {'position': [swing(0.8, 110, 90), swing(0.6, 130, 45), 0]},
             'puff_c': {'position': [0, swing(0.8, 120, 30), swing(0.6, 90)]}}
    anim('miasma_clot', {'misc.idle': {'loop': True, 'animation_length': 6.0, 'bones': drift},
                         'move.walk': {'loop': True, 'animation_length': 6.0, 'bones': drift}})


def gnawing_mass():
    """Bubonic Form: a hunched heap of rats holding one shape. Rat snouts and tails poke out of the
    pile; one big rat head at the front gives it a face; two piled 'arms' do the hitting."""
    fur, dark, skin, eye = '#4a3c32', '#33281f', '#b8908a', '#d4d94a'
    bones = [{'name': 'root', 'pivot': [0, 0, 0]},
             {'name': 'body', 'parent': 'root', 'pivot': [0, 8, 0]},
             {'name': 'head', 'parent': 'body', 'pivot': [0, 18, -7]},
             {'name': 'skull', 'parent': 'head', 'pivot': [0, 18, -7]},
             {'name': 'left_arm', 'parent': 'body', 'pivot': [12, 17, -3]},
             {'name': 'right_arm', 'parent': 'body', 'pivot': [-12, 17, -3]},
             {'name': 'tails', 'parent': 'body', 'pivot': [0, 6, 8]}]

    def snout(x, y, z, facing):
        """A small rat head poking out of the heap (facing: -1 north, +1 south, 'e', 'w')."""
        if facing in (-1, 1):
            zz = z if facing == -1 else z
            return [cube('body', [x, y, zz], [2, 2, 3], fur), cube('body', [x + 0.5, y, zz - 1 if facing == -1 else zz + 3], [1, 1, 1], skin)]
        xx = x
        return [cube('body', [xx, y, z], [3, 2, 2], fur), cube('body', [xx + (3 if facing == 'e' else -1), y, z + 0.5], [1, 1, 1], skin)]

    cubes = [
        # the heap, stepped to read as a rounded mound
        cube('body', [-10, 0, -8], [20, 7, 17], dark, mat='fur'),
        cube('body', [-9, 7, -7], [18, 6, 15], fur, mat='fur'),
        cube('body', [-7, 13, -6], [14, 5, 12], fur, mat='fur'),
        cube('body', [-5, 18, -4], [10, 3, 8], dark, mat='fur'),
        # the face of it: one big rat head
        cube('skull', [-3.5, 15, -12], [7, 6, 6], fur, decor={'north': eyes(eye, 2, 1)}, mat='fur'),
        cube('skull', [-2, 15, -15], [4, 4, 3], fur, mat='fur'),
        cube('skull', [-1, 16, -16], [2, 2, 1], skin),
        cube('skull', [-3.5, 21, -10], [2, 2, 1], skin), cube('skull', [1.5, 21, -10], [2, 2, 1], skin),   # ears
        # piled arms
        cube('left_arm', [10, 3, -5.5], [5, 15, 5], fur, mat='fur'),
        cube('left_arm', [10.5, 0, -6.5], [4, 3, 4], dark, mat='fur'),
        cube('right_arm', [-15, 3, -5.5], [5, 15, 5], fur, mat='fur'),
        cube('right_arm', [-14.5, 0, -6.5], [4, 3, 4], dark, mat='fur'),
        # tails trailing behind
        cube('tails', [-6, 1, 8], [1, 1, 7], skin), cube('tails', [-2, 3, 8], [1, 1, 9], skin),
        cube('tails', [3, 2, 8], [1, 1, 6], skin), cube('tails', [6, 5, 7], [1, 1, 7], skin),
    ]
    for args in ((-8, 9, -8, -1), (4, 11, -7, -1), (6, 3, -9, -1), (-6, 4, 9, 1), (2, 12, 7, 1),
                 (10, 8, -2, 'e'), (9, 13, 2, 'e'), (-13, 6, 0, 'w'), (-11, 14, -3, 'w')):
        cubes += snout(*args)
    geo, tex, ident = out('gnawing_mass')
    build('gnawing_mass', bones, cubes, 702, geo, tex, ident, bounds=(2.5, 2.2, 1),
          mats={skin: 'leather'})
    seethe = {f'body': {'scale': [f'1 + {swing(0.02, 300)}', f'1 + {swing(0.03, 360)}', f'1 + {swing(0.02, 330)}']},
              'tails': {'rotation': [swing(8, 200), swing(12, 160), 0]}}
    walk = dict(seethe)
    walk['body'] = {'position': [0, f'math.abs({swing(0.6, 300)})', 0], 'rotation': [0, 0, swing(3, 150)]}
    walk['left_arm'] = {'rotation': [swing(20, 300), 0, 0]}
    walk['right_arm'] = {'rotation': [swing(20, 300, 180), 0, 0]}
    anim('gnawing_mass', {
        'misc.idle': {'loop': True, 'animation_length': 2.0, 'bones': dict(seethe, skull={'rotation': [swing(4, 180), swing(8, 90), 0]})},
        'move.walk': {'loop': True, 'animation_length': 1.2, 'bones': walk},
        'attack.bite': {'loop': False, 'animation_length': 0.6, 'bones': {
            'left_arm': {'rotation': {'0.0': [0, 0, 0], '0.25': [-120, 0, -10], '0.4': [10, 0, 0], '0.6': [0, 0, 0]}},
            'right_arm': {'rotation': {'0.0': [0, 0, 0], '0.25': [-120, 0, 10], '0.4': [10, 0, 0], '0.6': [0, 0, 0]}},
            'skull': {'rotation': {'0.0': [0, 0, 0], '0.25': [-15, 0, 0], '0.4': [20, 0, 0], '0.6': [0, 0, 0]}}}}})


def choking_shade():
    """Pneumonic Form: a tall hooded column of foul air in rags, its lower half trailing off into fog."""
    robe, rag, fog, fog2, eye = '#3a4030', '#2c3024', '#6b7340@150', '#8a9258@100', '#e8f080'
    bones = [{'name': 'root', 'pivot': [0, 0, 0]},
             {'name': 'body', 'parent': 'root', 'pivot': [0, 14, 0]},
             {'name': 'head', 'parent': 'body', 'pivot': [0, 32, -1]},
             {'name': 'skull', 'parent': 'head', 'pivot': [0, 32, -1]},
             {'name': 'left_arm', 'parent': 'body', 'pivot': [6, 30, 0]},
             {'name': 'right_arm', 'parent': 'body', 'pivot': [-6, 30, 0]},
             {'name': 'skirt', 'parent': 'body', 'pivot': [0, 14, 0]}]
    cubes = [
        cube('body', [-5, 14, -3.5], [10, 16, 7], robe, mat='cloth'),
        cube('body', [-6, 27, -4], [12, 4, 8], rag, mat='cloth'),                    # hunched shoulders
        cube('skull', [-4, 31, -5], [8, 9, 8], robe, decor={'north': hooded_face(robe, eye)}, mat='cloth'),
        cube('skull', [-3, 39, -3], [6, 2, 6], rag, mat='cloth'),
        cube('skull', [-2, 31, -6], [4, 3, 1], fog2),                               # breath at the mouth of the hood
        cube('left_arm', [5, 12, -1.5], [3, 18, 3], rag, mat='cloth'),
        cube('right_arm', [-8, 12, -1.5], [3, 18, 3], rag, mat='cloth'),
        cube('left_arm', [5.5, 10, -1], [2, 2, 2], '#4a4a3a'),
        cube('right_arm', [-7.5, 10, -1], [2, 2, 2], '#4a4a3a'),
        # the lower half is fog, frayed rags hanging into it
        cube('skirt', [-5.5, 6, -4], [11, 8, 8], fog),
        cube('skirt', [-4.5, 1, -3], [9, 5, 6], fog2),
        cube('skirt', [-5, 8, -4.5], [2, 6, 1], rag), cube('skirt', [-1, 7, -4.5], [2, 7, 1], rag), cube('skirt', [3, 9, -4.5], [2, 5, 1], rag),
    ]
    geo, tex, ident = out('choking_shade')
    build('choking_shade', bones, cubes, 703, geo, tex, ident, bounds=(2, 3, 1.4),
          mats={fog: 'stone', fog2: 'stone'})
    sway = {'body': {'position': [0, f'{swing(0.5, 100)}', 0], 'rotation': [5, 0, swing(2, 80)]},
            'skirt': {'rotation': [swing(5, 120), 0, swing(6, 90)], 'scale': [f'1 + {swing(0.05, 140)}', 1, f'1 + {swing(0.05, 140)}']},
            'left_arm': {'rotation': [swing(6, 90), 0, swing(4, 70)]},
            'right_arm': {'rotation': [swing(6, 90, 180), 0, swing(-4, 70)]}}
    walk = dict(sway, body={'position': [0, f'{swing(0.5, 200)}', 0], 'rotation': [12, 0, 0]})
    anim('choking_shade', {
        'misc.idle': {'loop': True, 'animation_length': 4.0, 'bones': sway},
        'move.walk': {'loop': True, 'animation_length': 2.0, 'bones': walk},
        'attack.bite': {'loop': False, 'animation_length': 0.6, 'bones': {
            'left_arm': {'rotation': {'0.0': [0, 0, 0], '0.25': [-100, 0, -15], '0.6': [0, 0, 0]}},
            'right_arm': {'rotation': {'0.0': [0, 0, 0], '0.25': [-100, 0, 15], '0.6': [0, 0, 0]}}}}})


def blackened():
    """Septicemic Form: a lean, hunched figure of soot, faint embers in the cracks of it."""
    soot, soot2, ember, eye = '#1d181a', '#2a2326', '#6a2612', '#d86a28'
    bones = [{'name': 'root', 'pivot': [0, 0, 0]},
             {'name': 'body', 'parent': 'root', 'pivot': [0, 14, 0]},
             {'name': 'head', 'parent': 'body', 'pivot': [0, 26, -2]},
             {'name': 'skull', 'parent': 'head', 'pivot': [0, 26, -2]},
             {'name': 'left_arm', 'parent': 'body', 'pivot': [4.5, 25, -1]},
             {'name': 'right_arm', 'parent': 'body', 'pivot': [-4.5, 25, -1]},
             {'name': 'left_leg', 'parent': 'root', 'pivot': [2, 14, 0]},
             {'name': 'right_leg', 'parent': 'root', 'pivot': [-2, 14, 0]}]
    cubes = [
        cube('body', [-3.5, 14, -2.5], [7, 12, 4], soot, decor={'all': cracks(ember, 11)}, mat='stone'),
        cube('body', [-4, 23, -3.5], [8, 3, 5], soot2, mat='stone'),
        cube('skull', [-2.5, 25, -6], [5, 6, 5], soot, decor={'north': both(cracks(ember, 12), eyes(eye, 2, 1))}, mat='stone'),
        cube('left_arm', [3.5, 9, -2], [2, 16, 2], soot2, decor={'all': cracks(ember, 13)}, mat='stone'),
        cube('right_arm', [-5.5, 9, -2], [2, 16, 2], soot2, decor={'all': cracks(ember, 14)}, mat='stone'),
        cube('left_leg', [0.5, 0, -1], [3, 14, 3], soot, decor={'all': cracks(ember, 15)}, mat='stone'),
        cube('right_leg', [-3.5, 0, -1], [3, 14, 3], soot, decor={'all': cracks(ember, 16)}, mat='stone'),
    ]
    geo, tex, ident = out('blackened')
    build('blackened', bones, cubes, 704, geo, tex, ident, bounds=(1.5, 2.6, 1.2),
          mats={ember: 'glow', eye: 'glow'})
    idle = {'skull': {'rotation': [swing(4, 120), swing(10, 60), 0]},
            'body': {'rotation': [10 , 0, swing(2, 90)]},
            'left_arm': {'rotation': [swing(5, 90), 0, 0]}, 'right_arm': {'rotation': [swing(5, 90, 180), 0, 0]}}
    walk = {'left_leg': {'rotation': [swing(30, 400), 0, 0]}, 'right_leg': {'rotation': [swing(30, 400, 180), 0, 0]},
            'left_arm': {'rotation': [swing(25, 400, 180), 0, 0]}, 'right_arm': {'rotation': [swing(25, 400), 0, 0]},
            'body': {'rotation': [18, 0, 0], 'position': [0, f'math.abs({swing(0.6, 400)})', 0]}}
    anim('blackened', {
        'misc.idle': {'loop': True, 'animation_length': 3.0, 'bones': idle},
        'move.walk': {'loop': True, 'animation_length': 0.9, 'bones': walk},
        'attack.bite': {'loop': False, 'animation_length': 0.4, 'bones': {
            'right_arm': {'rotation': {'0.0': [0, 0, 0], '0.15': [-130, 0, 20], '0.3': [20, 0, 0], '0.4': [0, 0, 0]}},
            'left_arm': {'rotation': {'0.0': [0, 0, 0], '0.2': [-110, 0, -20], '0.35': [20, 0, 0], '0.4': [0, 0, 0]}}}}})


def mouldhusk():
    """Form of the Rot: a hunched, barrel-like husk of old bark and moss, shelf fungus growing out of it."""
    bark, bark2, moss, shelf, shelf2, eye = '#5a4632', '#463627', '#5a7a3a', '#c8a870', '#a8805a', '#9ad040'
    bones = [{'name': 'root', 'pivot': [0, 0, 0]},
             {'name': 'body', 'parent': 'root', 'pivot': [0, 8, 0]},
             {'name': 'head', 'parent': 'body', 'pivot': [0, 22, -5]},
             {'name': 'skull', 'parent': 'head', 'pivot': [0, 22, -5]},
             {'name': 'left_arm', 'parent': 'body', 'pivot': [9, 20, 0]},
             {'name': 'right_arm', 'parent': 'body', 'pivot': [-9, 20, 0]},
             {'name': 'left_leg', 'parent': 'root', 'pivot': [4, 8, 0]},
             {'name': 'right_leg', 'parent': 'root', 'pivot': [-4, 8, 0]}]
    cubes = [
        cube('body', [-8, 8, -6], [16, 14, 12], bark, decor={'all': g.patches(moss, 21, 7)}, mat='wood'),
        cube('body', [-7, 22, -5], [14, 3, 10], moss, mat='fur'),
        cube('skull', [-3.5, 20, -9], [7, 6, 4], bark2, decor={'north': eyes(eye, 2, 1)}, mat='wood'),
        # shelf fungus
        cube('body', [8, 16, -3], [3, 1, 5], shelf), cube('body', [8, 12, 0], [2, 1, 4], shelf2),
        cube('body', [-11, 18, -1], [3, 1, 5], shelf), cube('body', [-10, 13, -4], [2, 1, 4], shelf2),
        cube('body', [-4, 15, 6], [6, 1, 3], shelf), cube('body', [1, 11, 6], [4, 1, 2], shelf2),
        cube('body', [-5, 25, -3], [4, 2, 4], shelf), cube('body', [1, 25, -1], [3, 3, 3], shelf2),
        cube('left_arm', [8, 6, -2.5], [5, 15, 5], bark2, decor={'all': g.patches(moss, 22, 3)}, mat='wood'),
        cube('right_arm', [-13, 6, -2.5], [5, 15, 5], bark2, decor={'all': g.patches(moss, 23, 3)}, mat='wood'),
        cube('left_leg', [1, 0, -3], [6, 8, 6], bark2, mat='wood'),
        cube('right_leg', [-7, 0, -3], [6, 8, 6], bark2, mat='wood'),
    ]
    geo, tex, ident = out('mouldhusk')
    build('mouldhusk', bones, cubes, 705, geo, tex, ident, bounds=(2.5, 2.4, 1.1),
          mats={shelf: 'bone', shelf2: 'bone', eye: 'glow'})
    breathe = {'body': {'scale': [f'1 + {swing(0.02, 60)}', f'1 + {swing(0.03, 60)}', f'1 + {swing(0.02, 60)}']},
               'skull': {'rotation': [swing(3, 45), swing(6, 30), 0]}}
    walk = {'left_leg': {'rotation': [swing(15, 200), 0, 0]}, 'right_leg': {'rotation': [swing(15, 200, 180), 0, 0]},
            'left_arm': {'rotation': [swing(10, 200, 180), 0, 0]}, 'right_arm': {'rotation': [swing(10, 200), 0, 0]},
            'body': {'rotation': [0, 0, swing(4, 100)], 'position': [0, f'math.abs({swing(0.5, 200)})', 0]}}
    anim('mouldhusk', {
        'misc.idle': {'loop': True, 'animation_length': 6.0, 'bones': breathe},
        'move.walk': {'loop': True, 'animation_length': 1.8, 'bones': walk},
        'attack.bite': {'loop': False, 'animation_length': 0.9, 'bones': {
            'left_arm': {'rotation': {'0.0': [0, 0, 0], '0.4': [-150, 0, -10], '0.6': [15, 0, 0], '0.9': [0, 0, 0]}},
            'right_arm': {'rotation': {'0.0': [0, 0, 0], '0.4': [-150, 0, 10], '0.6': [15, 0, 0], '0.9': [0, 0, 0]}},
            'body': {'rotation': {'0.0': [0, 0, 0], '0.4': [-8, 0, 0], '0.6': [10, 0, 0], '0.9': [0, 0, 0]}}}}})


if __name__ == '__main__':
    miasma_clot()
    gnawing_mass()
    choking_shade()
    blackened()
    mouldhusk()
