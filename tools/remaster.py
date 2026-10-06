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
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
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


def dry(name, twine):
    """A dried bundle: greens to olive and straw, flowers faded, outlines browned, tied with twine at `twine`."""
    import colorsys
    fresh = __import__('preview').read_png(os.path.join(ITEMS, f'{name}.png'))
    out = []
    for row in fresh:
        new = []
        for r, g, b, a in row:
            if not a:
                new.append((0, 0, 0, 0)); continue
            h, l, s_ = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            if l > 0.75:                        # pale skins (garlic): papery tan
                h, s_, l = 0.09, 0.38, l * 0.84
            elif 0.15 < h < 0.55:               # greens and blue-greens: warm olive-straw
                h, s_, l = 0.10 + (h - 0.15) * 0.08, 0.36 + 0.1 * (l > 0.45), l * 0.9
            else:                               # flowers and stems: faded, browned
                s_, l = s_ * 0.6, l * 0.88
            rr, gg, bb = colorsys.hls_to_rgb(h, max(0, min(1, l)), max(0, min(1, s_)))
            new.append((int(rr * 255), int(gg * 255), int(bb * 255), a))
        out.append(new)
    for x, y in twine:
        out[y][x] = hexc('#c8a86a') if (x + y) % 2 else hexc('#8a6a3a')
    write_png(os.path.join(ITEMS, f'dried_{name}.png'), out)
    return f'textures/item/dried_{name}.png'


def round3():
    """The dried herbs, from the fresh ones (run after round 2)."""
    return [dry('garlic', [(6, 4), (7, 4), (8, 4), (9, 4)]),
            dry('rue', [(5, 11), (6, 11), (6, 10), (7, 10)]),
            dry('sage', [(8, 11), (9, 11), (8, 12)]),
            dry('wormwood', [(4, 11), (5, 11), (6, 11), (7, 11)]),
            dry('rosemary', [(5, 11), (6, 11), (7, 11)])]


def round4():
    """Remedies and tools."""
    return [
        item('herbal_broth', [
            '................', '................', '................', '......s...s.....', '.......s.s......',
            '......s...s.....', '................', '...oyyYYgYYyo...', '..oyYgYYYwYYyo..', '.oBbyyyyyyyybBo.',
            '.oBBbbbbbbbbBBo.', '..oBBbbbbbbBBo..', '...oBBBBBBBBo...', '....oooooooo....', '................', '................'],
            {'o': '#4a3018', 'B': '#9a6a3a', 'b': '#7a5028', 'y': '#c8a040', 'Y': '#e8c868', 'g': '#5c8538', 'w': '#f2ede2', 's': '#d0d0d4'}),
        item('plague_poultice', [
            '................', '................', '...oooooooooo...', '..oCCCCtCCCCco..', '..oCCgCtCCCcco..',
            '..oCGgGtCCccco..', '..otttttttttto..', '..oCCCgtCCgcco..', '..oCCCCtCGgcco..', '..ocCCCtCCccco..',
            '..occcctcccco...', '...oooooooooo...', '................', '................', '................', '................'],
            {'o': '#8a7e60', 'C': '#e8dfc4', 'c': '#c8bc9c', 't': '#a07848', 'g': '#5c8538', 'G': '#7aa850'}),
        item('mortar_and_pestle', [
            '................', '............wo..', '...........wWo..', '..........wWo...', '.........wWo....',
            '........wWo.....', '...oooowWoooo...', '..oLLgwWgggLLo..', '.oLSSggggggSSLo.', '.oSSSSSSSSSSSSo.',
            '.oSSSSSSSSSSSDo.', '..oSSSSSSSSSDo..', '...oSSSSSSSDo...', '....oDDDDDDo....', '.....oooooo.....', '................'],
            {'o': '#3e3e44', 'L': '#c8c8cc', 'S': '#8a8a90', 'D': '#5e5e66', 'g': '#6a8a3a', 'w': '#6b4a2b', 'W': '#9a7048'}),
        item('physicians_kit', [
            '................', '................', '.....hhhhhh.....', '....hh....hh....', '..oooooooooooo..',
            '.oFFFFFFFFFFFFo.', '.oFFFFFFFFFFFFo.', '.ofFFFFbbFFFFfo.', '.oLLLLLbBLLLLLo.', '.oLLLLLLLLLLLDo.',
            '.oLLLLLLLLLLLDo.', '.oDDDDDDDDDDDDo.', '..oooooooooooo..', '................', '................', '................'],
            {'o': '#2a1c12', 'F': '#8a5e3a', 'f': '#6a442a', 'L': '#7a5034', 'D': '#5a3a22', 'b': '#b08d3c', 'B': '#f0c860', 'h': '#4a3424'}),
        item('lancet', [
            '................', '................', '.............oW.', '............oWs.', '...........oWso.',
            '..........oWso..', '.........oWso...', '........oWso....', '.......oWso.....', '......ggso......',
            '.....gGg........', '....hHg.........', '...hHh..........', '..hHh...........', '..hh............', '................'],
            {'o': '#4a525a', 'W': '#f0f4f8', 's': '#a8b4bc', 'g': '#8a6a22', 'G': '#e0b848', 'h': '#b8ae98', 'H': '#e8e0cc'}),
    ]


def pomander():
    """A gold filigree lattice ball over dark dried herbs, hung from a red ribbon loop."""
    img = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    for x, y in ((7, 1), (8, 1), (6, 2), (9, 2), (7, 3), (8, 3)):
        img[y][x] = hexc('#8a2a2a')
    cx, cy, r = 7.5, 8.5, 4.6
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d > r:
                continue
            if d > r - 1:
                img[y][x] = hexc('#4a3410')
            elif (x + y) % 3 == 0 or (x - y) % 3 == 0:
                lit = (x - cx) + (y - cy) < 0
                img[y][x] = hexc('#f0d070' if lit else '#b08a30')
            else:
                img[y][x] = hexc('#4a5a2a' if (x * 7 + y) % 4 else '#6a4a2a')
    write_png(os.path.join(ITEMS, 'pomander.png'), img)
    return 'textures/item/pomander.png'


def round5():
    """Charms and the Heart."""
    return [
        item('leech_jar', [
            '................', '......cCCc......', '.....cCCCCc.....', '....gggggggg....', '...gWwwwwwwwg...',
            '...gWwkwwwwwg...', '...gwwkkwwkwg...', '...gwwwkwkkwg...', '...gwkwwwwkwg...', '...gwkkwwwwwg...',
            '...gwwkwwkkwg...', '...gwwwwwkwwg...', '...gdddddddDg...', '....gggggggg....', '................', '................'],
            {'c': '#8a6a43', 'C': '#b08a5a', 'g': '#7ca4c4', 'W': '#e8f4ff', 'w': '#4f6a4a', 'k': '#1a1612', 'd': '#3a3020', 'D': '#2a2418'}),
        item('censer', [
            '.......c...s....', '.......c..s.....', '.......c...s....', '......oyo.......', '.....oHYYo......',
            '....oHYYYYo.....', '...oyyyyyyyo....', '...oYkYkYkYo....', '...oYYYYYYyo....', '...oYkYkYkyo....',
            '....oyyyyyo.....', '.....ooyoo......', '......ooo.......', '................', '................', '................'],
            {'c': '#6e6e76', 'o': '#3a2a0c', 'Y': '#c89838', 'H': '#f0d070', 'y': '#8a6420', 'k': '#d8501c', 's': '#c8c8cc'}),
        pomander(),
        item('plaguebane_amulet', [
            '..c..........c..', '...c........c...', '....c......c....', '.....c....c.....', '......cooc......',
            '.....oGGGGo.....', '....oGeeeeGo....', '...oGeEhEeeGo...', '...oGeEEEEeGo...', '...oGeeEEeeGo...',
            '....oGeeeeGo....', '.....oGGGGo.....', '......oooo......', '................', '................', '................'],
            {'c': '#c8a040', 'o': '#6a4a14', 'G': '#e8c050', 'e': '#2a4a14', 'E': '#6aa030', 'h': '#d8ff90'}),
        item('heart_of_pestilence', [
            '................', '................', '...ooo...ooo....', '..oHHHo.oHHHo...', '.oHhHHHoHHHHVo..',
            '.oHHHVHHHHVHHo..', '.oHHHHVHHHVHHo..', '.oHVHHHHHHHHHo..', '..oHHVHHHHVHo...', '...oHHHVHHHo....',
            '....oHHHHHo.....', '.....oHHHo......', '......oHo.......', '.......d........', '.......d........', '................'],
            {'o': '#141a08', 'H': '#3a5a14', 'h': '#9ad040', 'V': '#1e2a0a', 'd': '#6a8a20'}),
    ]


def round6():
    """Weapons, bombs and the plague's materials."""
    E = '................'
    return [
        item('pyre_torch', [
            '...........y....', '..........yY.y..', '.........yYOYy..', '.........yOrOy..', '........yOrrOy..',
            '.........crrc...', '........cCCc....', '.......cCCc.....', '......wwc.......', '.....wWw........',
            '....wWw.........', '...wWw..........', '..wWw...........', '..ww............', E, E],
            {'y': '#f8d040', 'Y': '#fff4a0', 'O': '#f08020', 'r': '#c83010', 'c': '#3a2a1a', 'C': '#6a5040', 'w': '#5a3d22', 'W': '#8a6040'}),
        item('quicklime_bomb', [
            E, '.......tt.......', '......tTTt......', '.......ss.......', '.....pPPPPp.....',
            '....pPWPPPPp....', '...pPWPPPPPPp...', '...pPPPPPPPPp...', '...pPPPPPPPdp...', '...pPPPPPPddp...',
            '....pPPPPddp....', '.....pddddp.....', '......pppp......', E, E, E],
            {'P': '#ece6d4', 'W': '#ffffff', 'd': '#c8c0a8', 'p': '#8a8270', 't': '#d8d0bc', 'T': '#f4f0e4', 's': '#8a6a3a'}),
        item('fumigation_bomb', [
            '......s..s......', '.......ff.......', '......fFFf......', '.......ss.......', '.....pPPPPp.....',
            '....pPhPPhPp....', '...pPPPPhPPPp...', '...pPhPPPPPhp...', '...pPPPhPPPdp...', '...pPhPPPPddp...',
            '....pPPPhddp....', '.....pddddp.....', '......pppp......', E, E, E],
            {'P': '#7d8a6a', 'd': '#5a6650', 'p': '#3a4232', 'h': '#a8b878', 'f': '#6a5040', 'F': '#f08020', 's': '#c8c8cc'}),
        item('pestilent_ichor', [
            E, '.......o........', '......oGo.......', '......oGo.......', '.....oGGGo......',
            '.....oGhGo......', '....oGhGGGo.....', '....oGGGGGo.....', '...oGGGGGGdo....', '...oGGGGGddo....',
            '...oGGGGdddo....', '....oGddddo.....', '.....oooo.o.....', '..........o.....', E, E],
            {'o': '#1e2a0a', 'G': '#7a9a28', 'h': '#d8f080', 'd': '#4a6018'}),
        item('surgeons_steel', [
            E, E, E, E, '......oooooooo..',
            '.....oWWWWWWSo..', '....oWSSSSSSDo..', '...oWSSSrSSDo...', '..oSSSSSrrDo....', '..oDDDDDDDo.....',
            '..ooooooooo.....', E, E, E, E, E],
            {'o': '#2a3038', 'W': '#dce4ea', 'S': '#8e9aa4', 'D': '#5a646e', 'r': '#7a2a20'}),
    ]


def round7():
    """Rat spoils, the shrouded dead, the coat and trousers."""
    E = '................'
    return [
        item('rat_tail', [
            E, '.........ffff...', '........fFFFFf..', '........fFFFf...', '.........sss....',
            '........sRs.....', '.......sSs......', '......sRs.......', '.....sSs........', '....sRs.........',
            '...sSs..........', '..sRs...........', '..sS............', '.sR.............', '.s..............', E],
            {'f': '#3a302a', 'F': '#5a4a40', 's': '#9c6763', 'S': '#d8a8a0', 'R': '#b88078'}),
        item('rat_crown', [
            E, E, '..b....b....b...', '..B...bBb...B...', '.bBb..bBb..bBb..',
            '.bBBbbBBBbbBBb..', '.oBBBBBBBBBBBo..', '.otTtTtTtTtTto..', '.oBBBBkBkBBBBo..', '.obbbbbbbbbbbo..',
            '..ooooooooooo...', E, E, E, E, E],
            {'b': '#a89e88', 'B': '#e0d8c4', 'o': '#5a5040', 't': '#b07a75', 'T': '#d8a8a0', 'k': '#1a1612'}),
        item('shrouded_corpse', [
            E, E, E, E, '...oooooooooo...',
            '..oLLHLLLLLLLo..', '.oHLrLLLrLLLrLo.', '.oLLrLxLrLLLrLLo', '.oLLrLLLrLxLrLLo', '.oLdrLLdrLLdrLo.',
            '..oddddddddddo..', '...oooooooooo...', E, E, E, E],
            {'o': '#6e6656', 'L': '#d8cfb4', 'H': '#f0ead8', 'd': '#a89e84', 'r': '#7a5a3a', 'x': '#8a6a5a'}),
        item('doctors_coat', [
            E, '..ooo......ooo..', '.oHCCo....oCCCo.', '.oCCCCooooCCCCo.', '.oCCCCKKKKCCCCo.',
            '..ooCCCbbCCCoo..', '....oHCCCCCo....', '....oCbCCbCo....', '....oHCCCCCo....', '....rrrBBrrr....',
            '....oHCCCCCo....', '...oHCCCCCCCo...', '...oCCCCCCCCo...', '...oooooooooo...', E, E],
            {'o': '#0e0c0c', 'C': '#2e2a28', 'H': '#4a4440', 'K': '#1e1a1a', 'b': '#c8a040', 'B': '#e0c050', 'r': '#5a3d22'}),
        item('doctors_trousers', [
            E, E, '...oooooooooo...', '...oHTTTTTTTo...', '...oHTTTTTTTo...',
            '...oHTTooTTTo...', '...oHTo..oTTo...', '...oHTo..oTTo...', '...oHTo..oTTo...', '...oHTo..oTTo...',
            '...oHTo..oTTo...', '...oHTo..oTTo...', '...oDDo..oDDo...', '...oooo..oooo...', E, E],
            {'o': '#0e0c0c', 'T': '#2a2522', 'H': '#4a423c', 'D': '#1a1614'}),
    ]


ROUNDS = [round1, round2, round3, round4, round5, round6, round7]

if __name__ == '__main__':
    made = []
    for r in ROUNDS:
        made += r()
    with open(HANDMADE, 'w') as f:
        f.write('# Textures painted by hand (tools/remaster.py or Glenn); gen_assets.py must not overwrite these.\n')
        f.write('\n'.join(made) + '\n')
    print('hand-made:', len(made))
