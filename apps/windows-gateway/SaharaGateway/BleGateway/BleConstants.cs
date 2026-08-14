using System;

namespace SaharaGateway.BleGateway
{
    public static class BleConstants
    {
        public static readonly Guid ServiceUuid =
            Guid.Parse(
                "12345678-1234-5678-1234-56789abcdef0"
            );

        public static readonly Guid MessageUuid =
            Guid.Parse(
                "12345678-1234-5678-1234-56789abcdef1"
            );
    }
}