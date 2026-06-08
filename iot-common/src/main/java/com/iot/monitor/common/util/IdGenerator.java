package com.iot.monitor.common.util;

import java.util.UUID;

public class IdGenerator {

    public static String generateAlertCode() {
        return "ALT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public static String generateDeviceCode() {
        return "DEV" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
