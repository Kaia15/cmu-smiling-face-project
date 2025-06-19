package io.smilingface.utils;

public interface ILimiter {
    public boolean tryAcquire();
    public void release();
}
