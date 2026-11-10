# Adaptive Tools

NeoForge mod for Minecraft Java Edition. Tiered tools, armor, and a wireless power network. The current build has alloy ingots, pickaxes, area mining, and Light, Medium, and Heavy cables.

- Minecraft `26.1.2`
- Java `25`
- NeoForge `26.1.2.126`
- Mod id `adaptivetools`

## Build

```
./gradlew build
```

The mod jar is written to `build/libs/adaptivetools-0.2.0.jar`.

Game tests:

```
./gradlew runGameTestServer
```

Refresh generated models, tags, and recipes:

```
./gradlew runData
```

Client and dedicated server:

```
./gradlew runClient
./gradlew runServer
```

Ingot, pickaxe, and cable block textures are placeholders. Module, wrench, and GUI textures are the approved finals. Cable block art lives at `assets/adaptivetools/textures/block/light_cable.png`, `medium_cable.png`, and `heavy_cable.png`.
