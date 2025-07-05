package io.smilingface.utils;

import java.util.concurrent.atomic.AtomicInteger;


public class Limiter implements  ILimiter{
    /* 
    Good choice for concurrency limiter since CPU uses special instructions such as CAS (`compareAndSet()`)
    to ensure thread-safety: only one thread accesss this var to modify at a time
    */ 

    // since we cannot parse from `int` to `AtomicInteger`, we use this class's constructor directly
    private AtomicInteger currentReq = new AtomicInteger(0);
    private final int MAX_CONCURRENT_REQS;
    public Limiter(int maxConcurrentRequests) {
        if (maxConcurrentRequests <= 0) {
            throw new IllegalArgumentException("Max concurrent requests must be positive.");
        }
        this.MAX_CONCURRENT_REQS = maxConcurrentRequests;
    }

    /*
     * successful acquisition means we can process the job and when it's done, 
     * we return `True` and handle the number of requests thread-safely & correctly
     */
    public boolean tryAcquire(){
        while (true) {
            int numReq = this.currentReq.get();

            if (numReq >= MAX_CONCURRENT_REQS) {
                return false;
            }

            /* 
            `.compareAndSet()` is very important, since it ensures thread-safety, 
            and we need to retry if it fails **silently** somewhere under CAS operations
            */ 
            if (currentReq.compareAndSet(numReq, numReq + 1)) {
                return true;
            }
            
        }
    }

    public void release() {
        currentReq.decrementAndGet();
    }
}
