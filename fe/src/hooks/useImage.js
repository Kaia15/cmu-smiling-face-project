import react, {useState} from "react";

export function useImage() {
    const [images, setImages] = useState([]);
    const [topic, setTopic] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const handleSubmit = async () => {
        if (!topic.trim()) return;
        console.log(topic);

        setLoading(true);
        setError(null);

        try {
            const response = await fetch(`http://localhost:8080/api/v1?topic=${encodeURIComponent(topic)}`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                }
            });

            if (!response.ok) {
                throw new Error(`Error: ${response.status}`);
            }

            const data = await response.json();
            setImages(data); // assuming `data.images` is the array
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    return {
        images,
        topic,
        setTopic,
        loading, 
        error, 
        handleSubmit
    };
}