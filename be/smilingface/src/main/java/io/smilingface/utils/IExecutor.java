package io.smilingface.utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface IExecutor {
    public CompletableFuture<List<Map<String, String>>> submit(String topic);
}
