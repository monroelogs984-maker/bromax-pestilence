#!/usr/bin/env python3
"""Hand-remastered item textures (vanilla-style: tinted outlines, light from the top-left).

Remedies use vanilla's own glass bottle as their top model layer; only the liquid is ours, drawn
inside vanilla's potion-liquid mask. Everything written here is listed in tools/handmade.txt so
gen_assets.py never overwrites it.

    python3 tools/remaster.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_assets import ASSETS, write_json
from gen_assets import write_png as _write_png  # noqa: E402


def write_png(path, img):
    _write_png(path, img, force=True)
from vanilla_afflicted import jar_png  # noqa: E402

ITEMS = os.path.join(ASSETS, 'textures', 'item')
HANDMADE = os.path.join(os.path.dirname(__file__), 'handmade.txt')


def hexc(s):
    s = s.lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


def grid(rows, key):
    return [[hexc(key[c]) if c != '.' else (0, 0, 0, 0) for c in row] for row in rows]


def liquid(name, tones, details=()):
    """Fill vanilla's potion-liquid mask with our own shading: light top-left, dark bottom-right, a glint."""
    mask = jar_png('item/potion_overlay')
    pix = [(x, y) for y in range(16) for x in range(16) if mask[y][x][3]]
    xs = [p[0] for p in pix]; ys = [p[1] for p in pix]
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    img = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    light, mid, dark, deep = (hexc(t) for t in tones)
    for x, y in pix:
        t = ((x - x0) / max(1, x1 - x0) + (y - y0) / max(1, y1 - y0)) / 2
        edge = not all(0 <= x + dx < 16 and 0 <= y + dy < 16 and mask[y + dy][x + dx][3] for dx, dy in ((1, 0), (0, 1)))
        img[y][x] = deep if edge else light if t < 0.3 else mid if t < 0.6 else dark
    for x, y, c in details:
        if mask[y][x][3]:
            img[y][x] = hexc(c)
    write_png(os.path.join(ITEMS, f'{name}.png'), img)
    write_json(os.path.join(ASSETS, 'models', 'item', f'{name}.json'), {
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': f'bromaxpestilence:item/{name}', 'layer1': 'minecraft:item/potion'}})
    return f'textures/item/{name}.png'


def round1():
    made = []
    # Four Thieves Vinegar: clear amber, a sprig of rosemary and a garlic clove steeping in it.
    made.append(liquid('four_thieves_vinegar', ('#e8b04a', '#c88a2a', '#9a6418', '#6e4410'),
                       [(7, 9, '#4d7d52'), (8, 10, '#4d7d52'), (7, 11, '#355c3a'), (9, 11, '#f2ede2'), (9, 12, '#d8cfb8'), (6, 8, '#fff2c0')]))
    # Wormwood Tincture: murky bitter green with silt settled at the bottom.
    made.append(liquid('wormwood_tincture', ('#9aa84a', '#6f7d2e', '#4d581e', '#333a12'),
                       [(6, 8, '#d8e080'), (7, 13, '#5a4a2a'), (8, 13, '#5a4a2a'), (9, 12, '#5a4a2a')]))
    # Theriac: the old cure-all, near-black red with flecks of gold.
    made.append(liquid('theriac', ('#a8342a', '#7a1e18', '#541410', '#360c0a'),
                       [(6, 8, '#ff9a80'), (8, 10, '#e8c050'), (10, 12, '#e8c050'), (7, 12, '#c8a040')]))

    # The Plague Doctor's Mask, three-quarter view: brimmed hat, leather hood, brass lenses, long stitched beak.
    mask = grid([
        '................',
        '.....kkkkkk.....',
        '....kKKKKKKk....',
        '....kKKKKKKk....',
        '....bBBBBBBb....',
        '.kkkkkkkkkkkkkk.',
        '..kkhhhhhhhkkk..',
        '....hHrrHHHh....',
        '....hrlrhHHHh...',
        '..wwhrrrhHHHh...',
        'wWWWwhhhHHHHh...',
        '.wWsWWwhHHHh....',
        '..wwsWWwhHh.....',
        '....wwwwhh......',
        '.......hh.......',
        '................'], {
        'k': '#16141a', 'K': '#2a262e', 'b': '#5a3d26', 'B': '#7a5636',
        'h': '#2a211c', 'H': '#453830', 'r': '#8a6a26', 'l': '#bfe0d8',
        'w': '#a8946c', 'W': '#e0cfa4', 's': '#7a6644'})
    write_png(os.path.join(ITEMS, 'plague_mask.png'), mask)
    made.append('textures/item/plague_mask.png')

    # Consecrated Cleaver: broad hallowed blade with a gold rune, brass guard, wrapped grip.
    cleaver = grid([
        '................',
        '.........dddddd.',
        '........dSSSSSSd',
        '.......dSWWSSSSd',
        '......dSWSSyySd.',
        '.....dSWSSSySd..',
        '....dSWSSSSSd...',
        '....dSSSSSSd....',
        '....gdSSSSd.....',
        '...gGgddd.......',
        '....gGg.........',
        '...hHgg.........',
        '..hHh...........',
        '.hHh............',
        '.pp.............',
        '................'], {
        'd': '#3e444c', 'S': '#a8b4bc', 'W': '#e4ecf0', 'y': '#f0d060',
        'g': '#8a6a22', 'G': '#e0b848', 'h': '#3a2414', 'H': '#6a4a2a', 'p': '#c8a040'})
    write_png(os.path.join(ITEMS, 'consecrated_cleaver.png'), cleaver)
    made.append('textures/item/consecrated_cleaver.png')

    # Quicklime: a heap of chalky lumps, lit from the top-left.
    lime = grid([
        '................',
        '................',
        '................',
        '................',
        '......oooo......',
        '.....oWWWLo.....',
        '....oWWLLLLo....',
        '...ooWLLLLMo....',
        '..oWWLLMMoWWo...',
        '..oWLLMMoWWLLo..',
        '.oWWLLLMoWLLMMo.',
        '.oWLLMMMoLLMMMo.',
        '.oLLMMMDDoMMMDo.',
        '..ooDDDooooDDo..',
        '....oooo..ooo...',
        '................'], {'o': '#6e7078', 'W': '#ffffff', 'L': '#e8e8ea', 'M': '#c8c8d0', 'D': '#a0a2ac'})
    write_png(os.path.join(ITEMS, 'quicklime.png'), lime)
    made.append('textures/item/quicklime.png')

    return made


def item(name, rows, key):
    write_png(os.path.join(ITEMS, f'{name}.png'), grid(rows, key))
    return f'textures/item/{name}.png'


def round2():
    """The fresh herbs."""
    made = [item('garlic', [
        '................', '.......ss.......', '.......sS.......', '.......sS.......', '......oSSo......',
        '.....oWLLWo.....', '....oWLLpLLo....', '...oWLLLpLLLo...', '..oWWLLLpLLLLo..', '..oWLLpLLLpLMo..',
        '..oLLLpLLLpLMo..', '..oLLMpLLMpMMo..', '...oMMpMMMpMo...', '....oMMMMMMo....', '.....ooroo......', '....r..r..r.....'],
        {'o': '#8e8270', 'W': '#fffaf0', 'L': '#ece4d4', 'M': '#cfc3ad', 'p': '#b8a0bc', 's': '#9c8c6c', 'S': '#c4b48c', 'r': '#c8b898'})]
    made.append(item('rue', [
        '...........yY...', '..........yYYy..', '...........yy...', '........lL.s.yy.', '.......lhLs.yYy.',
        '....lL.lLs..yy..', '...lhLl.s.......', '...lLLLs.lL.....', '....ll.slhLl....', '.......sLLLl....',
        '...lL.s..ll.....', '..lhLls.........', '..lLLs..........', '...ls...........', '...s............', '................'],
        {'l': '#4a6e66', 'L': '#7fb0a0', 'h': '#a8d0c0', 's': '#5a7a4a', 'y': '#d8b830', 'Y': '#f8e070'}))
    made.append(item('sage', [
        '..........pP....', '.........pPp....', '..........pP....', '.........pPp....', '..........s.....',
        '....oooo..s.....', '...oLLhLo.s.....', '..oLhLLLLos.....', '..oLLLvLLLo.....', '..oLLvLLLos.oo..',
        '...oLvLLos.oLLo.', '....oooos.oLLhLo', '........soLLvLLo', '.......s.oLvLLo.', '......s...oooo..', '.....s..........'],
        {'o': '#4e5c48', 'L': '#8fa085', 'h': '#b4c4a8', 'v': '#6d7d64', 's': '#6a6a4a', 'p': '#7a5aa0', 'P': '#a888c8'}))
    made.append(item('wormwood', [
        '......y..y......', '.....yYy.Yy.....', '......s..s......', '....h.s.ls.h....', '...lh.sls..lh...',
        '..l.hss...lh.l..', '...lls.lhs.l....', '.h..s.lh.s.hl...', '.lhls.l..sll....', '..ls.lh..s..h...',
        '...s.l..s.lhl...', '...s...s..l.....', '....s.s.........', '.....s..........', '.....s..........', '................'],
        {'l': '#8e968a', 'h': '#c8d0c2', 's': '#6b7268', 'y': '#c8a830', 'Y': '#f0d860'}))
    made.append(item('rosemary', [
        '............bB..', '..........N.bb..', '.........N.sN.N.', '..........Ns.N..', '........N.snN.N.',
        '.........NsN.N..', '.......N.sn.N...', '........Nsn.N...', '......N.snN.....', '.......Nsn.N....',
        '.....N.snN......', '......Nsn.N.....', '....N.wn.N......', '.....ww.........', '....w...........', '................'],
        {'n': '#2f5236', 'N': '#5c8a5e', 's': '#4a6a3a', 'w': '#6a4e32', 'b': '#8aa8d8', 'B': '#c0d4f0'}))
    return made


ROUNDS = [round1, round2]

if __name__ == '__main__':
    made = []
    for r in ROUNDS:
        made += r()
    with open(HANDMADE, 'w') as f:
        f.write('# Textures painted by hand (tools/remaster.py or Glenn); gen_assets.py must not overwrite these.\n')
        f.write('\n'.join(made) + '\n')
    print('hand-made:', len(made))
