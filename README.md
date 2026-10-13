# Custom Jukebox

A Fabric mod for Minecraft 1.21.1 that adds a jukebox with a track-selection screen and playback synchronized between players on the server.

## Features
- Track list screen with albums and cover art
- Play / stop requests synchronized between all players (client–server networking)
- Tracks configured through `tracks.json`
- Russian and English localization

## Requirements
- Minecraft 1.21.1
- Fabric Loader and Fabric API
- Java 21

## Audio files are not included
The music and cover images are not distributed with this repository for copyright reasons. To use the mod, add your own files:

- audio: `src/main/resources/assets/customjukebox/sounds/tracks/disc_N.ogg`
- covers: `src/main/resources/assets/customjukebox/textures/covers/disc_N.png`

Then edit `tracks.json` so the titles match your files and rebuild the mod.

## Build
```
./gradlew build
```
The built `.jar` appears in `build/libs`. The project uses the Gradle wrapper (8.10.2) and JDK 21.

## Status
Work in progress. [Write here what works and what is still being fixed.]

## License
MIT, see [LICENSE](LICENSE).