## Backend
### Requirements
**1. Concurrency in the backend.** Rewrite the backend to perform computations concurrently. We have the following requirements of the final implementation:

* 1.1 The server makes requests to Wikipedia and the Google Cloud API concurrently, speeding up responses significantly.
* 1.2 The server can answer multiple requests concurrently (i.e., multiple calls to `/job` and `/job/:id`).
* 1.3 The server reports an error when more than 5 jobs are processed concurrently asking users to try again later. Reject additional requests with HTTP error code 503.
* 1.4 The server never makes more than 5 concurrent requests to Wikipedia and never more than 5 concurrent requests to the Google Cloud API in order to not overload those servers (this limit is shared by all jobs).
* 1.5 If multiple topics are analyzed, the server does not wait until all images are collected from all topics, but starts analyzing images as soon as the images from each topic are identified.

**2. Error handling.** Make the implementation robust to errors. Specifically we expect you to handle the following kind of errors:

* When connections to Wikipedia or the Google Cloud API fail (error, timeout, or invalid results) retry two more times after a short wait of one second.
* When connections to Wikipedia or the Google Cloud API fail and cannot be recovered or any other computations fail, report an error message to the frontend gracefully. Your server should still be able to handle 5 concurrent jobs and up to 5 concurrent backend requests afterward.
* The backend validates inputs received from the frontend. Reject empty and invalid inputs with HTTP error code 400.

**3. Solution** 

* 1.1-1.3: Counter Limiter
* Fundamental Idea: There are multiple threads in ThreadPool, but there are maximum 5 permits allowed to take on the jobs/requests received from the frontend. As one request comes in, one of threads in the pool will pick it up. This thread tries to acquire the permit before running the job => if successful, the job can be processed and rejected otherwise. Once the job completes, the thread releases the permit. Another job, if waiting, acquires the freed permit and starts. **At most 5 jobs** are processed **concurrently at any time**. 