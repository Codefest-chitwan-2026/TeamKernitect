const API_BASE_URL = "http://127.0.0.1:8000/api";


export type EmergencyType =
  | "EMERGENCY"
  | "EARTHQUAKE"
  | "FLOOD"
  | "LANDSLIDE"
  | "MEDICAL"
  | "TRAPPED"
  | "FIRE"
  | "OTHER";


export type SosPriority =
  | "NORMAL"
  | "HIGH"
  | "CRITICAL";


export type RescueStatus =
  | "NEW"
  | "RESPONDING"
  | "RESOLVED";


export interface Sos {
  id: string;
  revision: number;

  type: EmergencyType;

  latitude: number;
  longitude: number;

  message: string | null;

  timestamp: number;

  hopCount: number;
  maxHops: number;

  priority: SosPriority;

  status: RescueStatus;

  receivedAt?: string;
  updatedAt?: string;
  statusUpdatedAt?: string;
}


export interface SosListResponse {
  count: number;
  items: Sos[];
}


export interface SosStats {
  total: number;
  new: number;
  responding: number;
  resolved: number;
  criticalActive: number;
}


export async function getSosList(): Promise<SosListResponse> {
  const response = await fetch(
    `${API_BASE_URL}/sos`
  );

  if (!response.ok) {
    throw new Error(
      `Failed to fetch SOS list: ${response.status}`
    );
  }

  return response.json();
}


export async function getSosById(
  id: string
): Promise<Sos> {
  const response = await fetch(
    `${API_BASE_URL}/sos/${encodeURIComponent(id)}`
  );

  if (!response.ok) {
    throw new Error(
      `Failed to fetch SOS: ${response.status}`
    );
  }

  return response.json();
}


export async function getSosStats(): Promise<SosStats> {
  const response = await fetch(
    `${API_BASE_URL}/sos/stats`
  );

  if (!response.ok) {
    throw new Error(
      `Failed to fetch SOS statistics: ${response.status}`
    );
  }

  return response.json();
}


export async function updateSosStatus(
  id: string,
  status: RescueStatus
): Promise<Sos> {
  const response = await fetch(
    `${API_BASE_URL}/sos/${encodeURIComponent(id)}/status`,
    {
      method: "PATCH",

      headers: {
        "Content-Type": "application/json",
      },

      body: JSON.stringify({
        status,
      }),
    }
  );

  if (!response.ok) {
    throw new Error(
      `Failed to update SOS status: ${response.status}`
    );
  }

  return response.json();
}