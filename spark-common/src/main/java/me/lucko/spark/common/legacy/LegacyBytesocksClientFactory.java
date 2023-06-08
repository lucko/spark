package me.lucko.spark.common.legacy;

import me.lucko.bytesocks.client.BytesocksClient;

public class LegacyBytesocksClientFactory {
    public static BytesocksClient newClient(String host, String userAgent) {
        return new LegacyBytesocksClient(host, userAgent);
    }
}
