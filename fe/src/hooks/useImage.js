import react, { useState } from "react";

export function useImage() {
    const [images, setImages] = useState([]);
    const [topics, setTopics] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [input, setInput] = useState("");
    console.log(images);


    const handleSubmit = async () => {
        setLoading(true);
        setError(null);
        setImages([]);

        const requestTimeoutMs = 30000; // 30 seconds timeout for each individual fetch request

        const mappedFetchRequests = topics.map((topic) => {
            const controller = new AbortController();
            const id = setTimeout(() => controller.abort(), requestTimeoutMs); // Set timeout for each request

            const url = `http://localhost:8080/api/v1?topic=${encodeURIComponent(topic)}`;
            console.log(`[Frontend] Initiating fetch for topic: ${topic} to URL: ${url}`);

            return fetch(url, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                signal: controller.signal, // Link the AbortController to the fetch request
            })
                .then(response => {
                    clearTimeout(id); // Clear the timeout if the request completes
                    console.log(`[Frontend] Received response for topic: ${topic}, status: ${response.status}`);

                    return response;
                })
                .catch(fetchError => {
                    clearTimeout(id); // Clear the timeout even if there's an error
                    if (fetchError.name === 'AbortError') {
                        console.error(`[Frontend] Fetch for topic: ${topic} was aborted (timeout or cancelled).`);

                        return Promise.reject(new Error(`Timeout/Abort for topic: ${topic}`));
                    } else {
                        console.error(`[Frontend] Network or other fetch error for topic: ${topic}:`, fetchError);
                        return Promise.reject(fetchError); // Re-throw to make this promise reject
                    }
                });
        });

        try {

            const responses = await Promise.allSettled(mappedFetchRequests);

            const allImages = [];
            const failedRequests = [];

            for (let i = 0; i < responses.length; i++) {
                const result = responses[i];
                const topic = topics[i];

                if (result.status === 'fulfilled') {
                    const res = result.value; // This is the actual Response object from fetch
                    if (res.ok) {
                        console.log(`[Frontend] Successfully processed and parsing JSON for topic: ${topic}`);
                        const data = await res.json();
                        allImages.push({ topic: topic, data: data });
                    } else {
                        // Handle HTTP errors like 503, 404, etc.
                        const errorText = await res.text(); // Get error body for more info
                        console.error(`[Frontend] Request for topic: ${topic} failed with HTTP status ${res.status}: ${errorText}`);
                        failedRequests.push(`Topic: ${topic}, Status: ${res.status}, Error: ${errorText.substring(0, 100)}...`);
                    }
                } else {
                    // Handle promise rejections (network errors, timeouts, etc.)
                    console.error(`[Frontend] Request for topic: ${topic} rejected:`, result.reason);
                    failedRequests.push(`Topic: ${topic}, Rejected: ${result.reason.message || result.reason}`);
                }
            }

            setImages(allImages);

            if (failedRequests.length > 0) {
                setError(`Some requests failed: \n${failedRequests.join('\n')}`);
            } else if (allImages.length === 0 && topics.length > 0) {
                setError("No images processed successfully.");
            }

        } catch (err) {
            // This catch block would only be hit if Promise.allSettled itself failed (very rare)
            console.error("[Frontend] Unexpected error during Promise.allSettled execution:", err);
            setError(`An unexpected frontend error occurred: ${err.message}`);
        } finally {
            setLoading(false);
        }
    };

    const handleEnter = (e) => {
        if (e.key === 'Enter' && input.trim() !== '' && topics.length < 5) {
            e.preventDefault();
            setTopics(previousTopics => [...previousTopics, input.trim()]);
            setInput("");
        }
    }

    const handleClear = (inputId) => setTopics(currentTopics => currentTopics.filter((topic, topicId) => topicId !== inputId));

    return {
        images,
        topics,
        setTopics,
        loading,
        error,
        handleSubmit,
        input,
        handleEnter,
        setInput,
        handleClear
    };
}
