## Submitting your work

As usual, submit all your changes to GitHub. Once you have pushed your final code, submit a link to your final commit on Canvas. A link will look like `https://github.com/CMU-17-214/<reponame>/commit/<commitid>`. You can get to this link easily when you click on the last commit (above the list of files) in the GitHub web interface.

## Evaluation

This assignment is worth 100 points. We will grade the assignment with the rubric below. Submissions that do not provide a link in the expected format can not be graded and will not receive credit.

Concurrency:

* [ ] [10 points] The backend code makes use of concurrency to speed up computations for getting Wikipedia requests
* [ ] [10 points] The backend code makes use of concurrency to speed up computations for analyzing images with the Google Cloud API.
* [ ] [10 points] The backend code can process multiple jobs concurrently.
* [ ] [10 points] The backend successfully completes computations and provides the results through the API.
* [ ] [5 points] The backend rejects additional new jobs when processing 5 jobs concurrently with HTTP error code 503
* [ ] [5 points] The backend correctly limits concurrency to at most 5 concurrent requests each to Wikipedia and Google.
* [ ] [5 points] The backend can process images after images are identified from a topic, independent of other topics also to be analyzed.

Error handling:

* [ ] [5 points] The backend implements a retry mechanism for failed Wikipedia and Google connections, that makes two more attempts with a one second wait after each failed attempt.
* [ ] [5 points] The backend recovers gracefully from failed Wikipedia and Google connections, reporting an error message to the client. Errors do not reduce the ability to perform work concurrently.

Frontend:

* [ ] [5 points] The frontend supports incremental loading of results
* [ ] [5 points] The frontend shows a progress bar while results are loaded
* [ ] [5 points] The frontend shows errors from the backend if computations fail

Others:

* [ ] [10 points] The frontend and backend code compiles on GitHub Actions.
* [ ] [5 points] Commit messages are reasonably cohesive and have reasonable messages.
* [ ] [5 points] The code is free of severe readability and style issues.

## Appendix: Hints and Tooling Tips

**How to start:** The code is functional as provided. Identify where the *sync* functions from `Connections` are called and understand how they are used. Incrementally turn synchronous functions in asynchronous ones by changing return type `T` to `Promise<T>` and updating the implementation of the function and its call sides accordingly.

It is difficult to make only local changes as incremental refactorings. It might be easier to change the entire WikipediaAPI or VisionAPI class at once. If necessary, comment out the vision API steps first and just get images from topics with and without neighboring topics. It might be useful to write simple tests for the calls to the Wikipedia API that can run outside the backend, e.g., with jest or with a simple main function just calling `newWikipediaAPI(new DefaultConnection()).getImageLinksFromTopic("David Tepper")`.

**Implementation suggestions:** We recommend to use promises or async/await. It might be a good starting point to turn the provided connection functions with callbacks into promises. Beyond that you might find `Promise.all` useful.

You can consider streams (`ReadableStream` of the Web Streams) for some of the work, but it is not required.

The homework changes can all be fully implementing with plain Node code. Consider using the Proxy pattern to modularize error handling. If you like, you can use external libraries, such as `p-limit` or one of the many retry libraries.

The library `express` works very similar to the NanoHTTP library in Java you have seen before. You will likely not need to modify the express code itself. The React code to handle multiple jobs and incremental updates is not trivial, but also not overly complex -- you will not need to substantially modify this.

The execution order of concurrent code may not always be intuitive. Consider using a debugger or using print statements in the code to follow the execution.

**Testing suggestions:** Writing tests may be useful, but is not required in this assignment. There are good opportunities for testing with stubs in the backend; the backend code is written in a way to make this testing easy by avoiding global functions. We do not suggest to test frontend code.

A basic testing infrastructure is already setup in directory `tests/`, but tests are commented out. The example tests use stubs, but you can also run tests against Wikipedia and Google Cloud by just using the `DefaultConnection` class. Of course you can also test parts of your implementation without starting express, for example by writing tests for the functions in `wikipediaapi.ts`.

While you are not required to automate tests, we will use tests in the style of the provided examples for grading.

**Tooling suggestions:** If you want to send requests to the backend without running the frontend, you can send requests on the command line, such as

```bash
curl -X POST -H "Content-Type: application/json" -d '{"name": "David Tepper", "withNeighbors": false}' http://localhost:8080/job
```

With `npx tsc --watch` you can keep the TypeScript compiler running in a terminal and it will automatically compile changes whenever you safe a file. In a second terminal,  `npx nodemon .` it will restart your application (the backend) after every change, that is, whenever the compiler has produced a new version. The test runner `jest` watches changes by default.

For the frontend `npx react-scripts start` provides a development environment that refreshes automatically whenever any React files are changed. This unfortunately does not connect to the backend automatically, but can be used for pure frontend development. You can test frontend code during development if you set the initial state (`const initialJobs: JobInfo[]`) to something more interesting that contains actual results, e.g., those received with the curl command above.
