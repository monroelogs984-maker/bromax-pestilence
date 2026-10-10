"""Writes the GameTest arena: a 16x10x16 structure with a stone floor and air above.

    python3 tools/gametest_arena.py
"""
import gzip
import os
import struct

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'data', 'bromaxpestilence',
                   'structure', 'gametest', 'arena.nbt')
W, H, D = 16, 10, 16


def s(text):
    b = text.encode()
    return struct.pack('>H', len(b)) + b


def tag(t, name, payload):
    return bytes([t]) + s(name) + payload


def int_(name, v):
    return tag(3, name, struct.pack('>i', v))


def string(name, v):
    return tag(8, name, s(v))


def compound(name, *children):
    return tag(10, name, b''.join(children) + b'\x00')


def list_(name, elem_type, items):
    return tag(9, name, bytes([elem_type]) + struct.pack('>i', len(items)) + b''.join(items))


def int_list(name, values):
    return list_(name, 3, [struct.pack('>i', v) for v in values])


blocks = []
for x in range(W):
    for y in range(H):
        for z in range(D):
            state = 0 if y == 0 else 1
            blocks.append(b''.join([int_('state', state), int_list('pos', [x, y, z])]) + b'\x00')
palette = [
    b''.join([string('Name', 'minecraft:stone')]) + b'\x00',
    b''.join([string('Name', 'minecraft:air')]) + b'\x00',
]
root = compound('',
                int_list('size', [W, H, D]),
                list_('palette', 10, palette),
                list_('blocks', 10, blocks),
                list_('entities', 10, []),
                int_('DataVersion', 3955))
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with gzip.open(OUT, 'wb') as f:
    f.write(root)
print('wrote', OUT)
