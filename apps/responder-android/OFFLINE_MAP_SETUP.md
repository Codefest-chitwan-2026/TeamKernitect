# SAHARA Responder offline map pack

The responder supports an osmdroid-compatible MBTiles archive named:

`chitwan_offline.mbtiles`

Use a legitimate OpenStreetMap-derived archive covering Chitwan and the required surrounding Bagmati area. Recommended operational zoom levels are 10–17. Respect the tile source's license and attribution; no map archive is included in this repository.

## Production asset

Place the archive at:

`app/src/main/assets/offline_maps/chitwan_offline.mbtiles`

On first use the app copies it, on a background dispatcher, to its private location:

`files/offline_maps/chitwan_offline.mbtiles`

An existing non-empty private archive is reused and is not recopied on every start. To update a deployed pack, replace/delete the private file before opening the map, or install a new app build after clearing application data.

## Development installation with ADB

For a debuggable build:

```text
adb shell run-as com.kernitect.sahararesponder mkdir -p files/offline_maps
adb push chitwan_offline.mbtiles /data/local/tmp/chitwan_offline.mbtiles
adb shell run-as com.kernitect.sahararesponder cp /data/local/tmp/chitwan_offline.mbtiles files/offline_maps/chitwan_offline.mbtiles
adb shell rm /data/local/tmp/chitwan_offline.mbtiles
```

Force-stop and reopen the app after replacing a pack. With internet disabled, the Map screen should show `Offline Map`. If the archive is missing, the app displays an offline-coverage message while keeping incident coordinates, GPS, markers, distance, and bearing logic available.

Archive presence does not guarantee that every coordinate is covered. The current architecture cannot reliably extract a geographic bounding box from every archive format, so coverage outside the generated pack must be verified during map-pack production and physical testing.
