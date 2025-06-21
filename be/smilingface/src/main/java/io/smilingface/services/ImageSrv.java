package io.smilingface.services;

import io.smilingface.utils.IExecutor;

public class ImageSrv {
    public IExecutor taskExecutor;

    public ImageSrv(IExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void imageProcess(String topic) {
        this.taskExecutor.submit(topic);
    }

}
