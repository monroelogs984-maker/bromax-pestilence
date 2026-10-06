# BROMAX's Pestilence — design

A tame take on Scape and Run: Parasites, rebuilt around a medieval plague, for **Sovereign 202**
(NeoForge 1.21.1). Roughly 60–80% of SRP's scope. The plague should be a real problem a player has
to manage; generation is tuned so that leaving it alone makes things actively worse.

S202 rules apply: hard but fair, low RNG, every threat readable (telegraphed, announced, visible),
first-person friendly (no screen effects that hide combat).

## SRP mapping

| SRP | Pestilence | Tame twist |
|---|---|---|
| Evolution phases | Pestilence phases 0–8 from Plague Points | Phases go back down: curing, cleansing, burning corpses, bosses |
| Assimilation | The Afflicted: plague deaths leave corpses that rise | Players never convert; burned corpses never rise |
| Infested biome | The Blight: dead soil, withered crops, fetid water | Slow, capped per chunk, fully reversible, never player blocks |
| Nodes / nexus | Rat Warrens, Plague Pits, Miasma Wells (stages I–IV) | Visible landmarks, destroy them to relieve pressure |
| Call of the Hive | The Rot (late disease, converts mobs) | Mobs only |
| Living/Sentient gear | Consecrated gear, levels from cures and cleansing | |
| Status effects | Miasma, Dread, Delirium, Stench, Rotting | Annoying, never lethal, never blinding |

## Phases

| # | Name | Unlocks |
|---|---|---|
| 0 | Dormant | Plague rats (reduced), bubonic plague |
| 1 | Whispers | Full rat spawns |
| 2 | Murrain | Animals catch the plague and die of it; corpses; Afflicted animals rise |
| 3 | Rat Season | Afflicted animals spawn at night; Rat King; rat warrens |
| 4 | Outbreak | Villages get sick; villagers die of it; the Blight |
| 5 | Heralds | Herald mobs, plague pits |
| 6 | Black Death | Gravewarden; miasma wells |
| 7 | Danse Macabre | Everything denser, the Rot |
| 8 | The Pale Rider | The end boss |

## Systems (build order)

Status: 1 done (0.2.0), 2 done (0.3.0), 3 done (0.4.0), 4 done (0.5.0), 5 done (0.6.0). System 3 changes: no separate alembic (heated recipes need a fire, campfire or lit brazier within 2 blocks of the table); Sweating Sickness moves to 7 (seasons); The Rot moves to 5. System 4 change: plague villages are real villages converted the first time a player finds them (phase 5+), not a separate worldgen structure.

1. **Phases, corpses and pyres, Afflicted animals**: world Plague Points, announcements, `/pestilence`;
   corpse entity (flies, rises after a delay; burn it, haul it, or keep it near a lit pyre);
   Afflicted cow, pig, sheep, chicken, horse, wolf; miasma clouds; mobs die of late-stage plague.
2. **Nests and the Blight**: Rat Warren / Plague Pit / Miasma Well (staged), blight spread, quicklime,
   herb brazier, cleansing.
3. **Apothecary**: herbs (garlic, rue, sage, wormwood, rosemary) in worldgen, Apothecary Table,
   mortar and pestle, drying rack, alembic; disease roster (bubonic, pneumonic, septicemic,
   dysentery, sweating sickness, the Rot) with shared symptoms and a diagnosis kit; lancet, leeches,
   poultices, censer; full plague doctor outfit with set bonus.
4. **Villages**: house-to-house outbreaks, plague crosses (quarantine), Physician profession,
   villages that survive (rewards) or fall (plague village structure).
5. **Heralds**: Plague Bearer, Gravedigger, Flagellant, Censer Monk, Ratcatcher, Miasma Wraith,
   Mourner, Bone Collector, Pustule. Remaining vermin: Sewer Rat, Bloated Rat, Burrower, Rat Swarm,
   Flea Cloud, Carrion Crow, Fly Swarm.
6. **Bosses and dungeon**: Rat King, the Gravewarden (lazaretto dungeon), the Pale Rider.
   Structures: plague village, lazaretto, ossuary, warrens, pits.
7. **Gear and integration**: Consecrated gear, fire weapons, quicklime/fumigation bombs, rat-leather
   and bone tiers; Curios (mask, pomander), Serene Seasons, Lycanites coexistence.

Afflicted roster target: villager, wandering trader, pillager, vindicator, cow, pig, sheep, chicken,
horse, wolf, fox, Pestilent Dead. Only flesh-and-blood mobs can be Afflicted.

## Assets

`tools/gen_assets.py` builds every model, animation and texture from cube lists; edit there and
rerun. `tools/preview.py` renders a contact sheet of a model for review without the game;
`tools/preview_block.py` does the same for block models, reading vanilla textures from the MC client jar.
