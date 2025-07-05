package io.smilingface.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface IImageSrv {
    public CompletableFuture<List<Map<String, String>>> imageProcess(String topic);
}
