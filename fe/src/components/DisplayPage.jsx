"use client"

import { useImage } from "@/hooks/useImage";
import { InputWithButton } from "./InputWithButton";
import { TypographyH2 } from "./Typographyh2";
import { Separator } from "@/components/ui/separator";
import BoundingPolyViewer from "./BoundingPoly";
import { getEmotionColor, getEmotionIcon } from "@/lib/helpers";

export default function DisplayPage({ imageState }) {
    const { images, topic, setTopic, loading, handleSubmit, error } = imageState;
    console.log(images);

    return (
        <div>
            <div className="flex flex-row">
                <div className="flex-1/5">
                    <TypographyH2 />
                </div>
                <div className="flex-4/5">
                    <InputWithButton
                        topic={topic}
                        setTopic={setTopic}
                        loading={loading}
                        handleSubmit={handleSubmit}
                    />
                </div>
            </div>

            <Separator className="my-2" />
            <div className="flex flex-row">
                <div className="flex-1/5"></div>
                <div className="flex-4/5">
                    <p className="text-muted-foreground text-xl">
                        AI Overview
                    </p>
                    <div>
                        {images?.map((img, imageIdx) => {
                            const { image, joy, surprise, anger, boundingPoly, note, error } = img;
                            if (note || error) return (
                                <div>
                                    <p> {note} </p>
                                    <img
                                        src={image}
                                        alt="Analyzed"
                                        className="block max-w-full h-auto"
                                    />
                                </div>
                            )
                            // return (
                            //     <img
                            //             src={image}
                            //             alt="Analyzed"
                            //             className="block max-w-full h-auto"
                            //     />
                            // )

                            // Helper function to convert likelihood to confidence percentage
                            const getConfidenceFromLikelihood = (likelihood) => {
                                const confidenceMap = {
                                    'VERY_LIKELY': 90,
                                    'LIKELY': 70,
                                    'POSSIBLE': 50,
                                    'UNLIKELY': 30,
                                    'VERY_UNLIKELY': 10
                                };
                                return confidenceMap[likelihood] || 0;
                            };

                            // Create emotions array for easy mapping
                            const emotions = [
                                { name: 'joy', value: joy, icon: '😊' },
                                { name: 'surprise', value: surprise, icon: '😲' },
                                { name: 'anger', value: anger, icon: '😠' }
                            ];

                            return (
                                <div key={imageIdx} className="flex flex-col space-y-4">
                                    {/* Emotions Row */}
                                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                        {emotions.map((emotion) => (
                                            <div key={emotion.name} className="bg-white/50 rounded-xl p-3 border border-gray-200/50">
                                                <div className="flex items-center justify-between mb-2">
                                                    <div className="flex items-center space-x-2">
                                                        <span className="text-xl">{emotion.icon}</span>
                                                        <div>
                                                            <h3 className="font-semibold text-gray-800 capitalize text-sm">{emotion.name}</h3>
                                                            <p className="text-xs text-gray-600">
                                                                {emotion.value.replace('_', ' ')}
                                                            </p>
                                                        </div>
                                                    </div>
                                                    <div className="text-right">
                                                        <div className="text-sm font-bold text-gray-800">
                                                            {getConfidenceFromLikelihood(emotion.value)}%
                                                        </div>
                                                    </div>
                                                </div>

                                                <div className="w-full bg-gray-200 rounded-full h-2 overflow-hidden">
                                                    <div
                                                        className={`h-full ${getEmotionColor(emotion.value)} transition-all duration-1000 ease-out rounded-full`}
                                                        style={{ width: `${getConfidenceFromLikelihood(emotion.value)}%` }}
                                                    ></div>
                                                </div>
                                            </div>
                                        ))}
                                    </div>

                                    {/* Image with Bounding Box */}
                                    {error ? (
                                        <p style={{ color: "red" }}>{error}</p>
                                    ) : (
                                        <BoundingPolyViewer boundingPoly={boundingPoly} image={image} />
                                    )}
                                </div>
                            );
                        })}

                    </div>
                </div>
            </div>
        </div>
    )
}