using Microsoft.UI.Xaml;
using SaharaGateway.BleGateway;
using SaharaGateway.Services;

namespace SaharaGateway
{
    public sealed partial class MainWindow : Window
    {
        private readonly BleServer _bleServer;

        private readonly ApiClient _apiClient;

        public MainWindow()
        {
            InitializeComponent();

            _bleServer =
                new BleServer();

            _apiClient =
                new ApiClient();

            _bleServer.StatusChanged +=
                BleServer_StatusChanged;

            _bleServer.MessageReceived +=
                BleServer_MessageReceived;
        }

        private async void StartGateway_Click(
            object sender,
            RoutedEventArgs e
        )
        {
            await _bleServer.StartAsync();
        }

        private void StopGateway_Click(
            object sender,
            RoutedEventArgs e
        )
        {
            _bleServer.Stop();
        }

        private void BleServer_StatusChanged(
            string status
        )
        {
            DispatcherQueue.TryEnqueue(
                () =>
                {
                    StatusText.Text =
                        status;
                }
            );
        }

        private void BleServer_MessageReceived(
            string message
        )
        {
            DispatcherQueue.TryEnqueue(
                async () =>
                {
                    StatusText.Text =
                        "SOS RECEIVED OVER BLE";

                    MessageTextBox.Text =
                        message;

                    ApiStatusText.Text =
                        "Sending SOS to FastAPI...";

                    var result =
                        await _apiClient
                            .SendSosAsync(
                                message
                            );

                    ApiStatusText.Text =
                        result;
                }
            );
        }
    }
}