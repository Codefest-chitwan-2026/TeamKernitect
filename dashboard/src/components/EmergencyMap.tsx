import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
} from "react-leaflet";

import L from "leaflet";
import "leaflet/dist/leaflet.css";

// Fix Leaflet's default marker icon paths when using Vite
const createMarkerIcon = (color: string, symbol: string) =>
  L.divIcon({
    className: "",
    html: `
      <div style="
        width: 34px;
        height: 34px;
        border-radius: 50%;
        background: ${color};
        border: 3px solid white;
        box-shadow: 0 3px 10px rgba(0,0,0,0.25);
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        font-weight: 800;
        font-size: 15px;
        font-family: Arial, sans-serif;
      ">
        ${symbol}
      </div>
    `,
    iconSize: [34, 34],
    iconAnchor: [17, 17],
    popupAnchor: [0, -20],
  });

const victimIcon = createMarkerIcon("#ef4444", "!");
const rescueIcon = createMarkerIcon("#2563eb", "+");
const rescuedIcon = createMarkerIcon("#22c55e", "✓");

export default function EmergencyMap() {
  return (
    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
      
      {/* Map Header */}
      <div className="flex items-center justify-between border-b border-gray-100 px-5 py-4">
        <div>
          <h2 className="font-semibold text-gray-900">
            Live Emergency Map
          </h2>

          <p className="mt-1 text-xs text-gray-500">
            Real-time emergency locations across Nepal
          </p>
        </div>

        {/* Legend */}
        <div className="hidden items-center gap-4 sm:flex">

          <div className="flex items-center gap-2">
            <span className="h-3 w-3 rounded-full bg-red-500" />
            <span className="text-xs text-gray-600">
              Victim
            </span>
          </div>

          <div className="flex items-center gap-2">
            <span className="h-3 w-3 rounded-full bg-blue-600" />
            <span className="text-xs text-gray-600">
              Rescue Team
            </span>
          </div>

          <div className="flex items-center gap-2">
            <span className="h-3 w-3 rounded-full bg-green-500" />
            <span className="text-xs text-gray-600">
              Rescued
            </span>
          </div>

        </div>
      </div>

      {/* Real Map */}
      <div className="h-[500px] w-full">
        <MapContainer
          center={[28.3949, 84.124]}
          zoom={7}
          scrollWheelZoom={true}
          className="h-full w-full"
        >

          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          {/* ========================= */}
          {/* VICTIMS */}
          {/* ========================= */}

          <Marker
            position={[27.7172, 85.324]}
            icon={victimIcon}
          >
            <Popup>
              <div className="min-w-[180px]">
                <strong className="text-red-600">
                  Emergency Report
                </strong>

                <p className="mt-1 text-sm">
                  Kathmandu
                </p>

                <p className="mt-1 text-xs text-gray-500">
                  Status: Awaiting rescue
                </p>
              </div>
            </Popup>
          </Marker>

          <Marker
            position={[27.6588, 85.3247]}
            icon={victimIcon}
          >
            <Popup>
              <strong className="text-red-600">
                Emergency Report
              </strong>

              <p className="mt-1 text-sm">
                Lalitpur
              </p>

              <p className="mt-1 text-xs text-gray-500">
                Status: Awaiting rescue
              </p>
            </Popup>
          </Marker>


          {/* ========================= */}
          {/* RESCUE TEAMS */}
          {/* ========================= */}

          <Marker
            position={[27.671, 85.4298]}
            icon={rescueIcon}
          >
            <Popup>
              <strong className="text-blue-600">
                Rescue Team K-01
              </strong>

              <p className="mt-1 text-sm">
                Bhaktapur Response Unit
              </p>

              <p className="mt-1 text-xs text-gray-500">
                Status: On mission
              </p>
            </Popup>
          </Marker>

          <Marker
            position={[27.7172, 85.28]}
            icon={rescueIcon}
          >
            <Popup>
              <strong className="text-blue-600">
                Rescue Team K-02
              </strong>

              <p className="mt-1 text-sm">
                Kathmandu Response Unit
              </p>

              <p className="mt-1 text-xs text-gray-500">
                Status: Available
              </p>
            </Popup>
          </Marker>


          {/* ========================= */}
          {/* RESCUED */}
          {/* ========================= */}

          <Marker
            position={[27.7, 85.31]}
            icon={rescuedIcon}
          >
            <Popup>
              <strong className="text-green-600">
                Rescue Completed
              </strong>

              <p className="mt-1 text-sm">
                Kathmandu
              </p>

              <p className="mt-1 text-xs text-gray-500">
                Status: Victim rescued
              </p>
            </Popup>
          </Marker>

        </MapContainer>
      </div>

    </div>
  );
}