"use client"

import { useImage } from "@/hooks/useImage";
import { InputWithButton } from "./InputWithButton";
import { TypographyH2 } from "./Typographyh2";
import { Separator } from "@/components/ui/separator";
import BoundingPolyViewer from "./BoundingPoly";

export default function DisplayPage({imageState}) {
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
            
            <Separator className="my-2"/>
            <div className="flex flex-row">
                <div className="flex-1/5"></div>
                <div className="flex-4/5">
                    <p className="text-muted-foreground text-xl">
                    AI Overview
                    </p>
                    <div>
                        {images?.map((img, imageIdx) => {
                            const {image, joy, surprise, anger, boundingPoly} = img;

                            return (
                            <div className="flex flex-row">
                            <div className="flex-1/2 my-4 mr-2">
                                {error ? <p style={{ color: "red" }}>{error}</p> : 
                                <div>
                                    <ul>
                                        <li>Joy Index: {joy}</li>
                                        <li>Surprise Index: {surprise}</li>
                                        <li>Anger Index: {anger}</li>
                                    </ul> 
                                    
                                </div>  
                                } 
                                    
                            </div>
                            <div className="flex-1/2">
                                {/* <img className="rounded-lg my-4 h-1/2" src={image} alt="image description" /> */}
                                <BoundingPolyViewer boundingPoly={boundingPoly} image={image}/>
                            </div>
                            </div>
                        )})}
                        
                    </div>
                </div>
            </div>
        </div>
    )
}