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
   **DONE**: phase 9 (`Phase.BLACKBLIGHT`, a flag in PestilenceData; points stop mattering). Trigger:
   use the Heart on a lit pyre at night at phase 8, twice within 10s (first use only warns); refused
   under VANILLA_PLUS or `allowBlackblight=false`. The Black Bloom (500 HP, armour 6, stationary,
   fire x2) replaces the pyre: every 5s pulses (35% Blackblight to players within 16), raises up to 4
   pustules, blackens soil/leaves out to radius 24 over time. Blackblight effect: stages 1-9 of
   `blackblightStageTicks` (1800) each, -0.5 heart per stage, darkness 3+, weakness 5+, slowness and
   wither 7+; stage 10 kills (bromaxpestilence:blackblight damage, bypasses armour/protection).
   Spreads from stage 4+ (3 blocks, mask/censer halve it), 8% per Afflicted bite in phase 9.
   Theriac: back 2 stages (cures at 1-2). Amulet useless. Bloom's death cures all and resets to 0.
   `/pestilence blackblight unleash|end|infect <targets> [stage]`. Book: The Blackblight.
2. **Warnings and opt-outs**: at every phase change, offer a clickable prompt to stop the Pestilence
   advancing (caps the phase). Supports a lighter, "vanilla+-ish" plague. Host/op only in multiplayer.
   Pair with config presets: Vanilla+, Standard, Grim.
   **DONE**: `difficulty.preset` (VANILLA_PLUS caps at phase 4, x0.5 growth, x0.6 contagion/spawns, no
   fallen villages; GRIM x1.6 growth, x1.4 contagion, no natural recovery), `difficulty.maxPhase`,
   `/pestilence halt [phase]` and `/pestilence resume` (op or singleplayer host), a clickable halt prompt
   and a "next phase" warning on every rise, persisted `halt` in the phase data. Book: Staying the Tide.
3. **More weapons**: 3 more, spread through the progression, each with its own attack speed
   (candidates: fast Bone Saw, slow smoke-clearing Censer Flail, long-reach Gravedigger's Spade).
   **DONE**: Bone Saw (iron, 4 dmg / 2.6 speed; saws corpses into 2-4 bones + ichor, 25% contact
   risk scaled by garb), Censer Flail (10 dmg / 0.7 speed; hits clear miasma + vermin clouds within 3,
   x2 vs Miasma Wraiths), Gravedigger's Spade (shovel, 7.5 dmg / 1.0 speed, +1.5 entity / +1 block
   reach; buries corpses for half a burn's points and a deed; replaces the Gravedigger's iron shovel
   drop). Icons: remaster round 12 (also the bracelet and monocle charms). Book: Tools of the Trade.
4. **Curios first**: render the mask and charms on the player (ICurioRenderer), add more charm-slot
   items, and design gear around slots.
   **DONE**: slots head (mask), necklace (amulet, pomander), charm (pomander, monocle), bracelet
   (Rat-tail Bracelet: vermin bites/swarms/fleas/flies can't infect), belt (censer: smokes while lit,
   ticked from the player tick). Physician's Monocle sends stage-tinted motes over sick mobs in sight to
   the wearer only. ICurioRenderers (client/CurioRenderers, loaded only with Curios): mask via its
   GeoArmorRenderer, the rest as item sprites pinned to head/body/arm. Offhand fallback without Curios.
5. **Farming feeds the plague**: growing and harvesting crops speeds the Pestilence up (granaries draw
   rats), so farming carries a cost.
   **DONE**: from phase 1, each mature crop a player harvests adds `farming.pointsHarvest` (0.25); every
   `ratInterval` a farm of 16+ (farmland, hay/composter x4) near a player draws 1-3 Plague Rats at night
   (any hour with 4+ stores, a granary), each eating the crop it emerges from. Book: The Harvest.
6. **Texture pass**: **DONE** (items, effects, blocks and mob models remastered).

Claude's additions:
- A playtest before any of the above.
- Custom sounds (cough, squeak, bell, Rider).
- Advancements and S202 questbook hooks. **Advancements DONE** (0.9.0): 18 in a `pestilence/` tree,
  vanilla triggers where possible; burn/lime/bury, full consecration, saved village, halt, Blackblight
  unleashed and Last Breath are `minecraft:impossible` granted via ModAdvancements. Quest hooks: Glenn,
  in-game.
- BMM/BHB weapon-profile integration for the new weapons.

## Forms (0.10.0, 2026-10-10)

Glenn: take SRP's "concentrated, evolved clumps of disease" idea without SRP's body horror. Forms read as
rats, rags, soot, fog and mould: no exposed flesh, wounds or extra eyes. Everything is telegraphed.

| SRP | Pestilence |
|---|---|
| Merge: infected melt into Moving Flesh, which fuses and grows into a Primitive | **Coalescence**: Afflicted (not Heralds) with `coalesceKills` kills (half beside a nest), `coalesceGroup` of them within 4 blocks, stop and smoke for 3 s, then become a **Miasma Clot**. Fire breaks the telegraph. Unburned corpses lying 3+ together may coalesce instead of rising (`corpseCoalesceChance`). From `coalescePhase` (4). |
| Moving Flesh | **Miasma Clot**: slow, weak, foul to breathe near; drifts to other clots and ready Afflicted and absorbs them (mass). At `clotMassForForm` it churns for 5 s (announced) and condenses into a Lesser Form. Fire x2, brazier/censer smoke hurts it. |
| Primitive -> Adapted -> Pure by kills | **Lesser -> Greater -> Pestilent** by kills (`greaterKills` 5 from phase 6, `pestilentKills` 12 from phase 7); 3 s churn and an announcement first. x1/1.6/2.4 health, x1/1.3/1.7 damage, x1/1.2/1.4 size; Pestilent gets a boss bar. |
| Damage adaptation (fire/lightning reset) | **Hardening**: per kind of harm (melee, projectile, magic, explosion) every 2 hits add a stack, `adaptPerStack` (12%) each, up to `adaptMaxStacks` (5). Clank + grey dust on each new stack. Fire, lightning, the Consecrated Cleaver and brazier/censer smoke strip it all; fire also does x1.5. |
| Reinforcements (Beckons) | A Form hurt below half, and again on death, makes the nearest nest within 32 send 1+tier of its breed. No nest, no help. |
| Carriers / things inside victims | The Gnawing Mass sheds rats at each quarter of health lost. |
| Call of the Hive | The Mouldhusk's spore burst spreads the Rot. |

Lines (the nearest nest picks; the Rot takes 40% from phase 7):
- **Gnawing Mass** (bubonic, warren): heap of rats in one shape; sheds 1+tier rats per quarter health.
- **Choking Shade** (pneumonic, well): hooded column of fog; 1 s loud inhale, then a line of 2+tier miasma clouds.
- **Blackened** (septicemic, pit): quick soot figure; Weakness (Slowness when Pestilent) on hit; blights the ground it walks.
- **Mouldhusk** (the Rot, phase 7+): slow fungal husk; 1.5 s swell, then a spore burst (Rot chance, Rotting on players).

Points: coalesce +5, Form born +15, grows +20, killed by a player -10 x tier. Art: `tools/forms_assets.py`
(separate from gen_assets so nothing else is rebuilt). Book: The Forms (Enemies).
