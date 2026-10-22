# Aspect SMP resource pack (MC 1.21.4+ item-model format)

## custom_model_data (float index 0 / legacy int)
catalyst=1 resonator=2 restoration=3 reforging=4 stability_bottle=5 bottled_essence=6
cloudheart(mace)=7 frostheart=1002 shadowheart=1004 stormheart=1005 (nether_star)
heart_of_the_sea: inferno=100 tide=101 tempest=102 rift=103 vitality=104 war=105 cosmos=106
fortune=107 cloud=108 winter=109 shadow=110 storm=111 base=112

## Boss bars
assets/aspectsmp/textures/gui/bossbar_<aspect>.png (all 13, 182x5) are source art.
Vanilla only has 7 bar colors, so these aspects are mapped onto vanilla sprites:
red=inferno blue=tide green=vitality yellow=fortune purple=tempest white=cloud pink=cosmos
(assets/minecraft/textures/gui/sprites/boss_bar/<color>_progress.png). Edit the mapping in build.py.

## Overlays (assets/aspectsmp/textures/overlay)
Not vanilla paths: use with a shader / font glyph / display entity. Strips are horizontal, 512x512 per frame.
heartbeat 8f, critical 4f, awakened 6f, shift 8f (horizontal 50/100, vertical 50/100, radial 50/100, diagonal 50/100).
chromatic.png: R=left weight, G=center weight, B=right weight, alpha=radial strength.
