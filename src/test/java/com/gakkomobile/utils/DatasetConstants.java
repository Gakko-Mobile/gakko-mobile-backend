package com.gakkomobile.utils;

import java.util.UUID;

public class DatasetConstants {

    private static final String uuid = "123e4567-e89b-12d3-a456-42665544000";

    public static UUID uuid(int variantNumber) {

        return UUID.fromString(uuid + variantNumber);
    }
}
