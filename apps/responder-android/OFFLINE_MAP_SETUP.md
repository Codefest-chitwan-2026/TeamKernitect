# SAHARA Responder offline map pack

The responder supports an osmdroid-compatible raster MBTiles archive named `chitwan_offline.mbtiles`.

Obtain a legitimate archive with appropriate offline-use rights covering the intended Chitwan/Bagmati demonstration area. The application supports zoom levels 10–17. Respect the tile source license and attribution. No map archive is included in this repository.

## Production asset

Name the file exactly `chitwan_offline.mbtiles` and place it at:

`app/src/main/assets/offline_maps/chitwan_offline.mbtiles`

Then rebuild and install the responder APK. On first use, the application copies the asset on a background dispatcher to:

`files/offline_maps/chitwan_offline.mbtiles`

An existing valid private archive is reused. To update a deployed pack, replace the private file or reinstall after clearing application data.

## Development installation with ADB

For a debuggable build:

```text
adb shell run-as com.kernitect.sahararesponder mkdir -p files/offline_maps
adb push chitwan_offline.mbtiles /data/local/tmp/chitwan_offline.mbtiles
adb shell run-as com.kernitect.sahararesponder cp /data/local/tmp/chitwan_offline.mbtiles files/offline_maps/chitwan_offline.mbtiles
adb shell rm /data/local/tmp/chitwan_offline.mbtiles
```

Force-stop and reopen the app after replacing a runtime pack.

- Missing archive: `Offline map pack is not installed.`
- Archive installed but no matching initial zoom tile: `Offline map tiles are unavailable for this area.`
- Matching offline tile available: `Offline Map` renders normally.

Coordinates, GPS, markers, distance, and bearing remain available when background tiles are missing. Archive presence does not guarantee every coordinate or zoom is covered. The app checks the standard MBTiles `tiles` table for current map points at the initial zoom; full coverage still requires physical verification.
