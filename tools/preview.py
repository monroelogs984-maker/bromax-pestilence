#!/usr/bin/env python3
"""Renders geo.json models to PNG contact sheets (rest pose, no animation) for quick review.

    python3 tools/preview.py <model.geo.json> <texture.png> <out.png>

Orthographic views: front, right side, back, and a 3/4 view. Bone rotations are ignored (the
generated models don't use any). Face texel orientation is approximate.
"""
import json
import math
import struct
import sys
import zlib

sys.path.insert(0, __import__('os').path.dirname(__file__))
from gen_assets import write_png  # noqa: E402


def read_png(path):
    data = open(path, 'rb').read()
    pos, idat, w = 8, b'', 0
    while pos < len(data):
        n, tag = struct.unpack('>I4s', data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + n]
        if tag == b'IHDR':
            w, h = struct.unpack('>II', body[:8])
        elif tag == b'IDAT':
            idat += body
        pos += 12 + n
    raw = zlib.decompress(idat)
    stride = w * 4
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(h):
        f = raw[i]
        line = bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - 4] if x >= 4 else 0
            b = prev[x]
            c = prev[x - 4] if x >= 4 else 0
            if f == 1:
                line[x] = (line[x] + a) & 255
            elif f == 2:
                line[x] = (line[x] + b) & 255
            elif f == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append([tuple(line[j:j + 4]) for j in range(0, stride, 4)])
        prev = line
    return rows


def faces_of(cube):
    (x0, y0, z0), (sx, sy, sz) = cube['origin'], cube['size']
    inf = cube.get('inflate', 0)
    x0, y0, z0 = x0 - inf, y0 - inf, z0 - inf
    x1, y1, z1 = x0 + sx + 2 * inf, y0 + sy + 2 * inf, z0 + sz + 2 * inf
    # corner order: top-left, top-right, bottom-right, bottom-left as seen from outside
    return {
        'north': [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        'south': [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        'east': [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        'west': [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        'up': [(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)],
        'down': [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
    }


def render(geo, tex, yaw, pitch, scale, size):
    w, h = size
    img = [[(0, 0, 0, 0)] * w for _ in range(h)]
    zbuf = [[1e9] * w for _ in range(h)]
    cy, sy_ = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    bones = geo['minecraft:geometry'][0]['bones']
    allc = [c for b in bones for c in b.get('cubes', [])]
    ys = [c['origin'][1] for c in allc] + [c['origin'][1] + c['size'][1] for c in allc]
    mid = (min(ys) + max(ys)) / 2

    def proj(p):
        x, y, z = p[0], p[1] - mid, p[2]
        x, z = x * cy - z * sy_, x * sy_ + z * cy
        y, z = y * cp - z * sp, y * sp + z * cp
        return (w / 2 - x * scale, h / 2 - y * scale, z)

    for c in allc:
        for face, quad in faces_of(c).items():
            u0, v0 = c['uv'][face]['uv']
            uw, vh = c['uv'][face]['uv_size']
            pts = [proj(p) for p in quad]
            for tv in range(vh):
                for tu in range(uw):
                    col = tex[v0 + tv][u0 + tu]
                    if col[3] == 0:
                        continue
                    def lerp(a, b, t):
                        return tuple(a[k] + (b[k] - a[k]) * t for k in range(3))
                    def at(s, t):
                        top = lerp(pts[0], pts[1], s)
                        bot = lerp(pts[3], pts[2], s)
                        return lerp(top, bot, t)
                    q = [at(tu / uw, tv / vh), at((tu + 1) / uw, tv / vh),
                         at((tu + 1) / uw, (tv + 1) / vh), at(tu / uw, (tv + 1) / vh)]
                    fill_quad(img, zbuf, q, col)
    return img


def fill_quad(img, zbuf, q, col):
    h, w = len(img), len(img[0])
    xs, ys = [p[0] for p in q], [p[1] for p in q]
    z = sum(p[2] for p in q) / 4
    for py in range(max(0, int(min(ys))), min(h, int(max(ys)) + 1)):
        for px in range(max(0, int(min(xs))), min(w, int(max(xs)) + 1)):
            x, y = px + 0.5, py + 0.5
            inside = True
            sign = 0
            for i in range(4):
                a, b = q[i], q[(i + 1) % 4]
                cr = (b[0] - a[0]) * (y - a[1]) - (b[1] - a[1]) * (x - a[0])
                if abs(cr) < 1e-9:
                    continue
                s = 1 if cr > 0 else -1
                if sign == 0:
                    sign = s
                elif s != sign:
                    inside = False
                    break
            if inside and z < zbuf[py][px]:
                zbuf[py][px] = z
                img[py][px] = col


def main():
    geo = json.load(open(sys.argv[1]))
    tex = read_png(sys.argv[2])
    scale = float(sys.argv[4]) if len(sys.argv) > 4 else 8
    cell = int(48 * scale)
    views = [(0, 0), (90, 0), (180, 0), (35, -20)]
    sheet = [[(40, 44, 52, 255)] * (cell * len(views)) for _ in range(cell)]
    for i, (yaw, pitch) in enumerate(views):
        img = render(geo, tex, yaw, pitch, scale, (cell, cell))
        for y in range(cell):
            for x in range(cell):
                if img[y][x][3]:
                    sheet[y][i * cell + x] = img[y][x]
    write_png(sys.argv[3], sheet)


if __name__ == '__main__':
    main()
