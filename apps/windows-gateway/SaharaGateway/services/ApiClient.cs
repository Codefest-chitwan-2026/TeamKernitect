using System;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace SaharaGateway.Services
{
    public sealed class ApiClient
    {
        private readonly HttpClient _httpClient;

        private const string SosUrl =
            "http://127.0.0.1:8000/api/sos";

        public ApiClient()
        {
            _httpClient = new HttpClient
            {
                Timeout = TimeSpan.FromSeconds(10)
            };
        }

        public async Task<string> SendSosAsync(
            string rawBleJson
        )
        {
            try
            {
                /*
                 * Parse the packet received
                 * from Android over BLE.
                 */
                using JsonDocument document =
                    JsonDocument.Parse(
                        rawBleJson
                    );

                JsonElement root =
                    document.RootElement;

                string id =
                    root.GetProperty("id")
                        .GetString()
                    ?? "";

                double latitude =
                    root.GetProperty("latitude")
                        .GetDouble();

                double longitude =
                    root.GetProperty("longitude")
                        .GetDouble();

                long timestamp =
                    root.GetProperty("timestamp")
                        .GetInt64();

                int hopCount =
                    root.GetProperty("hopCount")
                        .GetInt32();

                int ttl =
                    root.GetProperty("ttl")
                        .GetInt32();

                /*
                 * New fields coming from Android.
                 *
                 * Fallback values make Windows
                 * compatible with older packets too.
                 */
                string priority =
                    root.TryGetProperty(
                        "priority",
                        out JsonElement priorityElement
                    )
                        ? priorityElement.GetString()
                            ?? "CRITICAL"
                        : "CRITICAL";

                string message =
                    root.TryGetProperty(
                        "message",
                        out JsonElement messageElement
                    )
                        ? messageElement.GetString()
                            ?? "SOS received via RESCUEMESH"
                        : "SOS received via RESCUEMESH";

                /*
                 * Convert BLE packet into
                 * FastAPI's expected model.
                 */
                var apiPacket = new
                {
                    id = id,

                    revision = 1,

                    type = "EMERGENCY",

                    latitude = latitude,

                    longitude = longitude,

                    message = message,

                    timestamp = timestamp,

                    hopCount = hopCount,

                    maxHops = ttl,

                    priority = priority
                };

                string apiJson =
                    JsonSerializer.Serialize(
                        apiPacket
                    );

                using var content =
                    new StringContent(
                        apiJson,
                        Encoding.UTF8,
                        "application/json"
                    );

                HttpResponseMessage response =
                    await _httpClient.PostAsync(
                        SosUrl,
                        content
                    );

                string responseBody =
                    await response.Content
                        .ReadAsStringAsync();

                if (response.IsSuccessStatusCode)
                {
                    return
                        $"SUCCESS\n" +
                        $"Priority: {priority}\n" +
                        $"HTTP {(int)response.StatusCode}\n" +
                        responseBody;
                }

                return
                    $"FAILED\n" +
                    $"Priority: {priority}\n" +
                    $"HTTP {(int)response.StatusCode}\n" +
                    responseBody;
            }
            catch (Exception ex)
            {
                return
                    $"ERROR\n{ex.Message}";
            }
        }
    }
}