#!/usr/bin/env python3
"""Before/after sheet for a remaster round: python3 tools/compare.py <out.png> <git-rev> name1 name2 ...
Remedies drawn as liquid layers are shown under vanilla's bottle, as in game."""
import json, os, subprocess, sys, tempfile
sys.path.insert(0, os.path.dirname(__file__))
from preview import read_png
from gen_assets import ASSETS, write_png
from vanilla_afflicted import jar_png

out, rev, names = sys.argv[1], sys.argv[2], sys.argv[3:]
Z = 10
sheet = [[(60, 64, 72, 255)] * (len(names) * 18 * Z) for _ in range(2 * 18 * Z)]
for k, n in enumerate(names):
    rel = f'src/main/resources/assets/bromaxpestilence/textures/item/{n}.png'
    tmp = os.path.join(os.environ.get('TMPDIR', '/tmp'), '_old.png')
    with open(tmp, 'wb') as f:
        f.write(subprocess.run(['git', 'show', f'{rev}:{rel}'], capture_output=True).stdout)
    old = read_png(tmp)
    new = read_png(os.path.join(ASSETS, 'textures', 'item', f'{n}.png'))
    model = json.load(open(os.path.join(ASSETS, 'models', 'item', f'{n}.json')))
    for layer in ('layer1',):
        ref = model.get('textures', {}).get(layer, '')
        if ref.startswith('minecraft:'):
            top = jar_png(ref.split(':')[1])
            new = [[t if t[3] else b for b, t in zip(rb, rt)] for rb, rt in zip(new, top)]
    for r, img in enumerate((old, new)):
        for y in range(16):
            for x in range(16):
                p = img[y][x]
                if p[3]:
                    for dy in range(Z):
                        for dx in range(Z):
                            sheet[r * 18 * Z + Z + y * Z + dy][k * 18 * Z + Z + x * Z + dx] = p
write_png(out, sheet)
