package io.smilingface.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import io.smilingface.utils.IExecutor;

@Service
public class ImageSrv implements IImageSrv {
    public IExecutor taskExecutor;

    public ImageSrv(IExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public CompletableFuture<List<Map<String, String>>> imageProcess(String topic) {
        return this.taskExecutor.submit(topic);
    }

}
