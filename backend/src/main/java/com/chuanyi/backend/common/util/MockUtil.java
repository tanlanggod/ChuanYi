package com.chuanyi.backend.common.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public final class MockUtil {

    private static final Random RANDOM = new Random();

    private MockUtil() {
    }

    public static String nextNo(String prefix) {
        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        int suffix = 1000 + RANDOM.nextInt(9000);
        return prefix + time + suffix;
    }
}
