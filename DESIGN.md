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

Status: 1 done (0.2.0), 2 done (0.3.0), 3 done (0.4.0), 4 done (0.5.0), 5 done (0.6.0), 6 done (0.7.0), 7 done (0.8.0): all systems built, nothing playtested yet. System 6 change: the Lazaretto is the one built structure (tools/lazaretto.py writes its NBT); ossuary dropped, warrens/pits/plague villages are dynamic. System 3 changes: no separate alembic (heated recipes need a fire, campfire or lit brazier within 2 blocks of the table); Sweating Sickness moves to 7 (seasons); The Rot moves to 5. System 4 change: plague villages are real villages converted the first time a player finds them (phase 5+), not a separate worldgen structure.

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

## System 7 as built

Gear: Pyre Torch, Quicklime Bomb, Fumigation Bomb (thrown brazier), Consecrated Cleaver (levels from
deeds: cures 2, corpses burned 1, nests 3, villages saved 5, bosses 10; thresholds 10/30/60/100),
Pomander (halves airborne exposure), Plaguebane Amulet (from the Heart of the Pestilence: full immunity).
Integrations: Curios (mask in head slot; charms in necklace/charm), Cold Sweat (fevers raise core
temperature; overheating brings Sweating Sickness, cured by sleep), Lycanites (no plague hosts among
its mobs; `naturalSpawnRate` config to thin our spawns). S202 has no Serene Seasons, so seasons
were swapped for Cold Sweat. Rat-leather and bone tiers were cut as filler.

## Backlog (Glenn, 2026-10-06, after 0.8.0)

1. **Blackblight**: a secret phase 9 with its own boss and the deadliest infection: a Blackblight effect
   with 10 stages that kills outright at stage 10. Keep it opt-in and heavily telegraphed.
2. **Warnings and opt-outs**: at every phase change, offer a clickable prompt to stop the Pestilence
   advancing (caps the phase). Supports a lighter, "vanilla+-ish" plague. Host/op only in multiplayer.
   Pair with config presets: Vanilla+, Standard, Grim.
3. **More weapons**: 3 more, spread through the progression, each with its own attack speed
   (candidates: fast Bone Saw, slow smoke-clearing Censer Flail, long-reach Gravedigger's Spade).
4. **Curios first**: render the mask and charms on the player (ICurioRenderer), add more charm-slot
   items, and design gear around slots.
5. **Farming feeds the plague**: growing and harvesting crops speeds the Pestilence up (granaries draw
   rats), so farming carries a cost.
6. **Texture pass**: the current art is placeholder quality and needs a real pass.

Claude's additions:
- A playtest before any of the above.
- Custom sounds (cough, squeak, bell, Rider).
- Advancements and S202 questbook hooks.
- BMM/BHB weapon-profile integration for the new weapons.
