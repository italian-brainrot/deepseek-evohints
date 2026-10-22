# Evolution Hints

A small client-side Cobblemon addon for Minecraft 1.21.1 / NeoForge.

Evolution Hints adds a green evolution marker to Pokémon stored in Cobblemon's
PC while Shift is held. Hovering a marked Pokémon explains how it evolves
without revealing level or friendship thresholds, and previews the evolution
target only after that species has been encountered in the Pokédex.

## Features

- Hold Shift in the PC to reveal evolution markers on party and box slots.
- Tooltip with one row per evolution path, shown while Shift is held.
- Level and friendship evolutions are described without their exact numbers.
- Item, trade, location, and other requirements use Cobblemon's own requirement
  descriptions, so addon-defined evolutions keep working.
- Compatible with Chiselmon's shift tooltip: the evolution summary is appended to
  Chiselmon's tooltip instead of opening a second, overlapping panel.
- The target's name and 3D preview are replaced with `???` / `?` until the
  species is registered in the Pokédex.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.262 or newer
- Cobblemon 1.8.2 or newer
- Kotlin for Forge is supplied by Cobblemon at runtime

## Building

```bash
./gradlew build
```

The mod jar is written to `build/libs/evohints-1.0.0.jar`.

## Installing

Drop the jar into the `mods` folder alongside Cobblemon and Kotlin for Forge.
The mod is client-side only; it can be installed on a client connecting to a
server that does not have it.
