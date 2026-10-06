#!/usr/bin/env python3
"""Before/after for entity/armour models (3/4 view): python3 tools/compare_models.py <out.png> <rev> path/name ...
Each name is relative to assets (e.g. entity/plague_doctor or item/armor/plague_mask)."""
import json, os, subprocess, sys
sys.path.insert(0, os.path.dirname(__file__))
from preview import read_png, render
from gen_assets import ASSETS, write_png
out, rev, names = sys.argv[1], sys.argv[2], sys.argv[3:]
cell = 300
tmp = os.environ.get('TMPDIR', '/tmp')
sheet = [[(60, 64, 72, 255)] * (len(names) * cell) for _ in range(2 * cell)]
for k, n in enumerate(names):
    geo_rel = f'src/main/resources/assets/bromaxpestilence/geo/{n}.geo.json'
    tex_rel = f'src/main/resources/assets/bromaxpestilence/textures/{n}.png'
    old_geo = json.loads(subprocess.run(['git', 'show', f'{rev}:{geo_rel}'], capture_output=True).stdout)
    open(f'{tmp}/_ot.png', 'wb').write(subprocess.run(['git', 'show', f'{rev}:{tex_rel}'], capture_output=True).stdout)
    pairs = [(old_geo, read_png(f'{tmp}/_ot.png')),
             (json.load(open(os.path.join(ASSETS, 'geo', f'{n}.geo.json'))), read_png(os.path.join(ASSETS, 'textures', f'{n}.png')))]
    for r, (geo, tex) in enumerate(pairs):
        bones = geo['minecraft:geometry'][0]['bones']
        ys = [c['origin'][1] + d for b in bones for c in b.get('cubes', []) for d in (0, c['size'][1])]
        span = max(8, max(ys) - min(ys))
        img = render(geo, tex, 35, -15, min(7, 220 / span), (cell, cell))
        for y in range(cell):
            for x in range(cell):
                p = img[y][x]
                if p[3]:
                    sheet[r * cell + y][k * cell + x] = p
write_png(out, sheet)
