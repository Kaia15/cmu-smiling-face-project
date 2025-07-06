# Facemini

Inspired by **CMU-17-214** course, in this project, you will work with concurrency in a Java/Spring Boot backend and a React frontend from scratch. You will get experience with writing asynchronous code, with error handling, and with handling state in React, and Java/Spring.

https://github.com/user-attachments/assets/cac95d01-49ad-4bb1-b062-7ec3b8a18d8b

## Introduction

You will work on a semi-completed program *SmilingFaces* to analyze faces in Wikipedia pages -- for example are more people smiling in pictures of the Carnegie Mellon wikipedia page or in the University of Pittsburgh wikipedia page? In the web application, you can enter a *topic* for which a Wikipedia page exists and the program will identify all images in the page and determine with an ML model whether the picture contains smiling faces. It can also collect the pictures from other Wikipedia pages linked heavily from the target page ("include top neighbor topics").

The code will handle all the communication with Wikipedia and the Google Cloud Vision API, but the backend code is currently written synchronously (which is actually quite difficult to do and very unusual for Node code). As a consequence the backend can only respond to a single request at a time and it is very slow. The provided implementation is also bad at error handling.

The code consists of two related projects: The backend *java/spring boot* implementation in the root directory and the frontend *React* implementation in directory `frontend`.  Compile and run the code as follows:

* First, build the frontend
  * run `npm install`  in the `frontend/` directory
  * `npm run build` in `frontend/` to build the frontend, which will result in static pages in `frontend/build/`
* Second, build and run the backend
  * Make sure that you are signed into the Google Cloud API using `gcloud` (same as Lab 8)
  * `javac YourApplication.java` runs the server which you can then access at `http://localhost:8080`

The backend serves the frontend code in the root of the web server but also provides API endpoints for starting a task (`POST ?topic=${your_search_topic}`) both communicate in JSON format (using the [Long Running Operation with Polling Pattern](http://restalk-patterns.org/long-running-operation-polling.html) /*still on research*) . The frontend will make requests to the APIs to update the state within the page. If a job is not completed on the first request, it will check every second for updates.

In the user interface in the web browser you can enter a topic and start the analysis. Note that the response will take a very long time if you analyze any nontrivial pages. Analyzing the topic "Carnegie Mellon University" gathers and analyzes 30 images without neighboring pages (and many more with neighbors), which easily takes 30 seconds to respond. A good test page might be "David Tepper" which has only a single image and takes about 2 seconds to analyze.

## Tasks

**Concurrency in the backend.** Rewrite the backend to perform computations concurrently. We have the following requirements of the final implementation:

* The server makes requests to Wikipedia and the Google Cloud API concurrently, speeding up responses significantly.
* The server can answer multiple requests concurrently (i.e., multiple calls to `POST ?topic=${search_param}`).
* The server reports an error when more than 5 jobs are processed concurrently asking users to try again later. Reject additional requests with HTTP error code 503.
* The server never makes more than 5 concurrent requests to Wikipedia and never more than 5 concurrent requests to the Google Cloud API in order to not overload those servers (this limit is shared by all jobs).
* If multiple topics are analyzed, the server does not wait until all images are collected from all topics, but starts analyzing images as soon as the images from each topic are identified.

**Error handling.** Make the implementation robust to errors. Specifically we expect you to handle the following kind of errors:

* When connections to Wikipedia or the Google Cloud API fail (error, timeout, or invalid results) retry two more times after a short wait of one second.
* When connections to Wikipedia or the Google Cloud API fail and cannot be recovered or any other computations fail, report an error message to the frontend gracefully. Your server should still be able to handle 5 concurrent jobs and up to 5 concurrent backend requests afterward.
* The backend validates inputs received from the frontend. Reject empty and invalid inputs with HTTP error code 400.

**Frontend improvements.** Improve the React frontend with some minor extensions

* Allow incremental loading in the frontend, by polling regularly for updates from the backend. (This should work out of the box if the backend responds correctly)
* Show a progress bar while data is loaded.
* Show errors from the backend in the frontend, ideally with meaningful error messages.

**What not to change:** We plan to automate some testing of your code and ask you to NOT change the `Connections` interface and the signature of the `smilingFacesBackend` function. Make all external calls through the APIs in `Connections` and do not make web calls with any other API. You may, and probably should, develop your own abstractions on top of the functions in `Connections`.

