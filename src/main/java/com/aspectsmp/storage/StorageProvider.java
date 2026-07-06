package com.aspectsmp.storage;

import com.aspectsmp.AspectSMP;
import com.aspectsmp.core.Heart;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageProvider {
    void initialize(AspectSMP plugin);
    CompletableFuture<Void> saveHeart(Heart heart);
    Heart loadHeart(UUID playerId);
    void shutdown();
}