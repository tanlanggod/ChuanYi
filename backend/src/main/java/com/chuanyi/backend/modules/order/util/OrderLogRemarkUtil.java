package com.chuanyi.backend.modules.order.util;

import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public final class OrderLogRemarkUtil {

    private OrderLogRemarkUtil() {
    }

    public static String buildShippingRemark(String carrierCode, String trackingNo, String note) {
        StringBuilder sb = new StringBuilder();
        sb.append("carrierCode=").append(sanitize(carrierCode));
        sb.append(";trackingNo=").append(sanitize(trackingNo));
        if (StringUtils.hasText(note)) {
            sb.append(";note=").append(sanitize(note));
        }
        return sb.toString();
    }

    public static Map<String, String> parseKeyValueRemark(String text) {
        Map<String, String> map = new HashMap<String, String>();
        if (!StringUtils.hasText(text)) {
            return map;
        }
        String[] pairs = text.split(";");
        for (String pair : pairs) {
            if (!StringUtils.hasText(pair)) {
                continue;
            }
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = pair.substring(0, idx).trim();
            String value = pair.substring(idx + 1).trim();
            map.put(key, value);
        }
        return map;
    }

    private static String sanitize(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replace(';', ',').replace('=', ':').trim();
    }
}
