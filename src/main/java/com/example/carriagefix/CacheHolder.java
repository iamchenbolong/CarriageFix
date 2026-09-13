package com.example.carriagefix;

import java.util.Collection;

public class CacheHolder {
    public static final String CACHE_FIELD = "carriageFixCache";
    public static final String VERSION_FIELD = "carriageFixVersion";

    private CacheHolder() {}

    @SuppressWarnings("rawtypes")
    public static Collection getCachedIfValid(
            Object self,
            int portalCutoffMin,
            int portalCutoffMax,
            Collection currentCache,
            int currentVersion,
            int cachedMin,
            int cachedMax,
            int version) {
        if (currentCache != null && currentVersion == version) {
            return currentCache;
        }
        return null;
    }
}