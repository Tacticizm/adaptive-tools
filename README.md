# Adaptive Tools

NeoForge mod for Minecraft Java Edition. Tiered tools, armor, and a wireless power network. The current build has the five alloys, tools, armor, smithing upgrades, cables, and modules.

- Minecraft `26.1.2`
- Java `25`
- NeoForge `26.1.2.126`
- Mod id `adaptivetools`

## Build

```
./gradlew build
```

The mod jar is written to `build/libs/adaptivetools-0.6.0.jar`.

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

Item, armor, block, cable, and screen textures in `assets/adaptivetools/textures` are the approved finals. Storage blocks reuse the ingot texture. Generator, transmitter, and receiver faces are still art only. Baby armor sheets are wired in each tier's equipment JSON. Sprite positions for the unbuilt screens are in `docs/gui_uv.json`.
