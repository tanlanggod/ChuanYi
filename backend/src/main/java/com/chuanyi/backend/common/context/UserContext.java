package com.chuanyi.backend.common.context;

public final class UserContext {

    private static final Long DEFAULT_USER_ID = 1L;
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<Long>();

    private UserContext() {
    }

    public static void bindUserId(Long userId) {
        if (userId == null) {
            USER_ID_HOLDER.remove();
            return;
        }
        USER_ID_HOLDER.set(userId);
    }

    public static Long currentUserId() {
        Long userId = USER_ID_HOLDER.get();
        return userId == null ? DEFAULT_USER_ID : userId;
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
