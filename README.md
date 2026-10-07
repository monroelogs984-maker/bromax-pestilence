# BROMAX's Pestilence

A plague mod for Minecraft **1.21.1 NeoForge**: a tamer take on Scape and Run: Parasites, about
two-thirds its size, built for the *Sovereign 202* modpack. MIT licensed.

The Pestilence is a world-wide tide. Sickness, plague deaths and rising corpses feed it, and it
also creeps up a little each day. Cures, burned bodies and slain Afflicted push it back. As it
grows it passes through phases, each announced by a tolling bell:

| Phase | Name | What changes |
|---|---|---|
| 0 | Dormant | A few rats |
| 1 | Whispers | Rats in earnest; farms draw them |
| 2 | Murrain | Animals sicken, die and rise as Afflicted |
| 3 | Rat Season | Afflicted walk at night; nests seed near players |
| 4 | Outbreak | The Blight spreads over grass and leaves |
| 5 | Heralds | Plague pits; the Heralds and pustules appear |
| 6 | Black Death | Miasma wells |
| 7 | Danse Macabre | The Rot turns the living Afflicted in place |
| 8 | The Pale Rider | Pestilence rides |
| 9 | *The Blackblight* | Secret, opt-in only (see below) |

## Features

- **Diseases**: bubonic, pneumonic, septicemic, dysentery, the Rot and the Sweating Sickness. Each
  has its own symptoms and its own remedy; Theriac cures them all. People recover; animals die.
- **The physician's craft**: five herbs (plantable, found wild), a drying rack, the Apothecary
  Table (heated recipes), lancet, leeches, the Physician's Kit for diagnosis, a censer of burning
  herbs, the beaked mask and the full Doctor's Garb.
- **Corpses**: the dead rise unless burned on a pyre, limed, buried or carried away in a shroud.
- **Nests**: Rat Warrens, Plague Pits and Miasma Wells grow through four stages and seed more.
  The Blight spreads from them. Quicklime and Herb Braziers cleanse it.
- **Villages**: outbreaks are tracked per village and end in survival or a fallen village. You
  can quarantine with Plague Crosses, and the Physician villager trades remedies and treats the sick.
- **Creatures**: Afflicted cows, pigs, sheep, chickens, horses, wolves and villagers (vanilla models
  made sick). Also 9 Heralds, plague rats and vermin (swarms, crows, flea and fly clouds).
- **Bosses**: the Rat King, the Gravewarden (beneath the Lazaretto structure) and the Pale Rider.
- **Arms**:
  - Pyre Torch, quicklime and fumigation bombs.
  - The Consecrated Cleaver, which grows holier with good deeds.
  - Bone Saw (fast; saws corpses for bone and ichor).
  - Censer Flail (slow; clears miasma).
  - Gravedigger's Spade (long reach; buries the dead).
- **Charms** (Curios, or the offhand without it), each drawn on the player when worn:
  - Pomander.
  - Plaguebane Amulet.
  - Rat-tail Bracelet: no vermin bite can infect you.
  - Physician's Monocle: shows you who is sick.
  - A lit censer on the belt.
- **Farming feeds the plague**: harvests add to the Pestilence, and fields and granaries draw rats.
- **A lighter plague if you want one**:
  - At every phase rise, ops (or the singleplayer host) can click *Halt the Pestilence here*.
  - `/pestilence halt` and `/pestilence resume` do the same by command.
  - Config presets: *Vanilla+* (stops at phase 4, half pace), *Standard* and *Grim*.
- **The Blackblight**: burn the Heart of the Pestilence on a lit pyre, at night, during phase 8,
  twice. The Black Bloom takes root and spreads a ten-stage sickness whose tenth stage kills.
  Killing the Bloom cures everyone and breaks the Pestilence entirely. It is disabled under Vanilla+.
- **The Treatise**, a Patchouli guide (craft a book with rotten flesh), and 18 advancements.

## Dependencies

- **Required:** NeoForge 21.1+, [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) 4.9+
- **Optional:** Patchouli (the Treatise), Curios API (charm slots), Cold Sweat (fevers and the
  Sweating Sickness follow body temperature)

## Building

The GeckoLib, Curios, Cold Sweat and Patchouli jars are compiled against from `libs/`, which is not
in the repository. Download those versions (see `build.gradle`) into `libs/`, then run
`./gradlew build`.

`DESIGN.md` has the full design and the notes on how each system was built. `tools/` holds the
Python scripts that generate the models, textures and the Lazaretto structure.

## Commands

- `/pestilence`: phase and Plague Points.
- `/pestilence halt [phase]` and `/pestilence resume`: ops, or the singleplayer host.
- Ops only:
  - `points set|add`
  - `phase <0-8>`
  - `infect <targets> [stage] [disease]`
  - `blackblight unleash|end|infect`

## License

MIT, by BROMAX.
