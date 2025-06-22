package io.smilingface.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public interface IImageSrv {
    public Future<List<Map<String, String>>> imageProcess(String topic);
}
