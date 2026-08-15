using System;
using System.Text;
using System.Threading.Tasks;
using Windows.Devices.Bluetooth;
using Windows.Devices.Bluetooth.GenericAttributeProfile;
using Windows.Storage.Streams;

namespace SaharaGateway.BleGateway
{
    public sealed class BleServer
    {
        private GattServiceProvider? _serviceProvider;

        private GattLocalCharacteristic? _messageCharacteristic;

        public event Action<string>? StatusChanged;

        public event Action<string>? MessageReceived;

        public bool IsRunning { get; private set; }

        public async Task StartAsync()
        {
            if (IsRunning)
            {
                StatusChanged?.Invoke(
                    "Gateway already running"
                );

                return;
            }

            try
            {
                StatusChanged?.Invoke(
                    "Checking Bluetooth adapter..."
                );

                var adapter =
                    await BluetoothAdapter.GetDefaultAsync();

                if (adapter == null)
                {
                    StatusChanged?.Invoke(
                        "ERROR: No Bluetooth adapter found"
                    );

                    return;
                }

                if (!adapter.IsLowEnergySupported)
                {
                    StatusChanged?.Invoke(
                        "ERROR: Bluetooth LE is unsupported"
                    );

                    return;
                }

                if (!adapter.IsPeripheralRoleSupported)
                {
                    StatusChanged?.Invoke(
                        "ERROR: Peripheral role unsupported"
                    );

                    return;
                }

                /*
                 * 1. Create service.
                 */
                StatusChanged?.Invoke(
                    "Creating RESCUEMESH service..."
                );

                var serviceResult =
                    await GattServiceProvider.CreateAsync(
                        BleConstants.ServiceUuid
                    );

                if (
                    serviceResult.Error !=
                    BluetoothError.Success
                )
                {
                    StatusChanged?.Invoke(
                        $"SERVICE FAILED: {serviceResult.Error}"
                    );

                    return;
                }

                _serviceProvider =
                    serviceResult.ServiceProvider;

                /*
                 * 2. Create characteristic BEFORE
                 * advertising.
                 *
                 * Notify is important because Windows
                 * automatically generates the CCCD for
                 * a notifiable characteristic.
                 */
                var characteristicParameters =
                    new GattLocalCharacteristicParameters
                    {
                        CharacteristicProperties =
                            GattCharacteristicProperties.Write |
                            GattCharacteristicProperties.WriteWithoutResponse |
                            GattCharacteristicProperties.Notify,

                        WriteProtectionLevel =
                            GattProtectionLevel.Plain
                    };

                var characteristicResult =
                    await _serviceProvider
                        .Service
                        .CreateCharacteristicAsync(
                            BleConstants.MessageUuid,
                            characteristicParameters
                        );

                if (
                    characteristicResult.Error !=
                    BluetoothError.Success
                )
                {
                    StatusChanged?.Invoke(
                        $"CHARACTERISTIC FAILED: " +
                        $"{characteristicResult.Error}"
                    );

                    _serviceProvider =
                        null;

                    return;
                }

                _messageCharacteristic =
                    characteristicResult.Characteristic;

                /*
                 * 3. Register handler.
                 */
                _messageCharacteristic.WriteRequested +=
                    MessageCharacteristic_WriteRequested;

                /*
                 * 4. Advertise only after service +
                 * characteristic exist.
                 */
                var advertisingParameters =
                    new GattServiceProviderAdvertisingParameters
                    {
                        IsConnectable = true,
                        IsDiscoverable = true
                    };

                _serviceProvider.StartAdvertising(
                    advertisingParameters
                );

                IsRunning =
                    true;

                StatusChanged?.Invoke(
                    "RESCUEMESH ADVERTISING"
                );
            }
            catch (Exception ex)
            {
                StatusChanged?.Invoke(
                    $"ERROR: {ex.Message}"
                );

                Stop();
            }
        }

        private async void MessageCharacteristic_WriteRequested(
            GattLocalCharacteristic sender,
            GattWriteRequestedEventArgs args
        )
        {
            var deferral =
                args.GetDeferral();

            try
            {
                var request =
                    await args.GetRequestAsync();

                if (request == null)
                {
                    return;
                }

                using var reader =
                    DataReader.FromBuffer(
                        request.Value
                    );

                var bytes =
                    new byte[
                        reader.UnconsumedBufferLength
                    ];

                reader.ReadBytes(
                    bytes
                );

                var message =
                    Encoding.UTF8.GetString(
                        bytes
                    );

                MessageReceived?.Invoke(
                    message
                );

                /*
                 * Android uses WRITE_TYPE_DEFAULT,
                 * so this is normally WriteWithResponse.
                 */
                if (
                    request.Option ==
                    GattWriteOption.WriteWithResponse
                )
                {
                    request.Respond();
                }
            }
            catch (Exception ex)
            {
                StatusChanged?.Invoke(
                    $"WRITE ERROR: {ex.Message}"
                );
            }
            finally
            {
                deferral.Complete();
            }
        }

        public void Stop()
        {
            try
            {
                if (
                    _messageCharacteristic != null
                )
                {
                    _messageCharacteristic.WriteRequested -=
                        MessageCharacteristic_WriteRequested;
                }

                _serviceProvider
                    ?.StopAdvertising();
            }
            catch
            {
                // Cleanup only.
            }

            _messageCharacteristic =
                null;

            _serviceProvider =
                null;

            IsRunning =
                false;

            StatusChanged?.Invoke(
                "Gateway stopped"
            );
        }
    }
}