#!/usr/bin/env python3
"""Before/after for block textures: python3 tools/compare_blocks.py <out.png> <rev> name1 name2 ..."""
import os, subprocess, sys
sys.path.insert(0, os.path.dirname(__file__))
from preview import read_png
from gen_assets import ASSETS, write_png
out, rev, names = sys.argv[1], sys.argv[2], sys.argv[3:]
Z = 8
sheet = [[(60, 64, 72, 255)] * (len(names) * 18 * Z) for _ in range(2 * 18 * Z)]
tmp = os.path.join(os.environ.get('TMPDIR', '/tmp'), '_old.png')
for k, n in enumerate(names):
    open(tmp, 'wb').write(subprocess.run(['git', 'show', f'{rev}:src/main/resources/assets/bromaxpestilence/textures/block/{n}.png'], capture_output=True).stdout)
    for r, img in enumerate((read_png(tmp), read_png(os.path.join(ASSETS, 'textures', 'block', f'{n}.png')))):
        for y in range(16):
            for x in range(16):
                p = img[y][x]
                if p[3]:
                    for dy in range(Z):
                        for dx in range(Z):
                            sheet[r * 18 * Z + Z + y * Z + dy][k * 18 * Z + Z + x * Z + dx] = p
write_png(out, sheet)
