#!/usr/bin/env python3
"""Renders a Java block model (elements + vanilla/mod textures) with preview.py's renderer.

    python3 tools/preview_block.py <model.json> <out.png> [scale]

Vanilla textures are read from the Minecraft client jar. Element rotations are ignored.
"""
import io
import json
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(__file__))
from preview import read_png, render  # noqa: E402
from gen_assets import write_png, ASSETS  # noqa: E402

JAR = os.path.expanduser('~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar')


def load_texture(ref, jar):
    ns, path = ref.split(':') if ':' in ref else ('minecraft', ref)
    if ns == 'minecraft':
        data = jar.read(f'assets/minecraft/textures/{path}.png')
        tmp = '/tmp/_tex.png' if not os.environ.get('TMPDIR') else os.path.join(os.environ['TMPDIR'], '_tex.png')
        open(tmp, 'wb').write(data)
        img = read_png(tmp)
    else:
        img = read_png(os.path.join(ASSETS, 'textures', f'{path}.png'))
    return [row[:16] for row in img[:16]]   # first animation frame


def resolve(model, key):
    while key.startswith('#'):
        key = model['textures'][key[1:]]
    return key


def main():
    model = json.load(open(sys.argv[1]))
    jar = zipfile.ZipFile(JAR)
    cubes, atlas_rows, cache = [], [], {}
    # every distinct texture gets a 16px tall strip in a vertical atlas
    def strip(ref):
        if ref not in cache:
            cache[ref] = len(atlas_rows)
            atlas_rows.extend(load_texture(ref, jar))
        return cache[ref]
    for el in model['elements']:
        fr, to = el['from'], el['to']
        size = [max(0.01, to[i] - fr[i]) for i in range(3)]
        uv = {}
        for face in ('north', 'south', 'east', 'west', 'up', 'down'):
            if face not in el['faces']:
                uv[face] = {'uv': [0, 0], 'uv_size': [0, 0]}
                continue
            v0 = strip(resolve(model, el['faces'][face]['texture']))
            w = size[0] if face in ('north', 'south', 'up', 'down') else size[2]
            h = size[1] if face not in ('up', 'down') else size[2]
            u = int(fr[0] if face in ('north', 'south', 'up', 'down') else fr[2]) % 16
            v = int(16 - to[1]) % 16 if face not in ('up', 'down') else int(fr[2]) % 16
            uv[face] = {'uv': [u, v0 + v], 'uv_size': [max(1, min(16 - u, round(w))), max(1, min(16 - v, round(h)))]}
        # block space (0..16) to model space centred on x/z, like an entity model
        cubes.append({'origin': [fr[0] - 8, fr[1], fr[2] - 8], 'size': size, 'uv': uv})
    geo = {'minecraft:geometry': [{'bones': [{'name': 'b', 'pivot': [0, 0, 0], 'cubes': cubes}]}]}
    tex = [row + [(0, 0, 0, 0)] * (16 - len(row)) for row in atlas_rows]
    scale = float(sys.argv[3]) if len(sys.argv) > 3 else 8
    cell = int(30 * scale)
    views = [(0, 0), (35, -25), (90, 0)]
    sheet = [[(40, 44, 52, 255)] * (cell * len(views)) for _ in range(cell)]
    for i, (yaw, pitch) in enumerate(views):
        img = render(geo, tex, yaw, pitch, scale, (cell, cell))
        for y in range(cell):
            for x in range(cell):
                if img[y][x][3]:
                    sheet[y][i * cell + x] = img[y][x]
    write_png(sys.argv[2], sheet)


if __name__ == '__main__':
    main()
