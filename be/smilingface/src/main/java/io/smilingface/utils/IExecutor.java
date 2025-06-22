package io.smilingface.utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public interface IExecutor {
    public Future<List<Map<String, String>>> submit(String topic);
}
