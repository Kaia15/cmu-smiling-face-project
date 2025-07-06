import { useImage } from "@/hooks/useImage";
import { InputWithButton } from "./InputWithButton";
import { TypographyH2 } from "./Typographyh2";
import { Separator } from "@/components/ui/separator";
import BoundingPolyViewer from "./BoundingPoly";
import { getEmotionColor, getEmotionIcon } from "@/lib/helpers";

export default function DisplayPage({ imageState }) {
  const {
    images,
    topics,
    setTopics,
    loading,
    handleSubmit,
    error,
    handleClear,
    handleEnter,
    setInput,
    input,
  } = imageState;
  console.log(images);

  return (
    <div>
      <div className="flex flex-row">
        <div className="flex-1/5">
          <TypographyH2 />
        </div>
        <div className="flex-4/5">
          <InputWithButton
            topics={topics}
            setTopics={setTopics}
            loading={loading}
            handleSubmit={handleSubmit}
            input={input}
            handleEnter={handleEnter}
            setInput={setInput}
            handleClear={handleClear}
          />
        </div>
      </div>

      <div className="flex flex-row">
        <div className="flex-1/5"></div>
        <div className="flex-4/5">
          <div>
            <p className="text-muted-foreground text-xl">AI Overview</p>
            <div>
              There are totally{" "}
              <span className="font-bold"> {images?.length} </span> topics:
              <>
                {images?.map((imagesData, dataId) => (
                  <span key={dataId} className="mx-1">
                    {imagesData.topic},
                    {dataId < images.length - 1 ? ", " : ""}
                  </span>
                ))}
              </>
            </div>
          </div>

          <div className="mt-6 flex flex-row">
                        {images?.map((topicData, topicIndex) => (
                            <div key={topicIndex} className="border rounded-lg p-4 basis-1/2">
                                <h3 className="text-lg font-semibold mb-4 capitalize">
                                    {topicData.topic}
                                </h3>
                                
                                {/* Grid of images for this topic */}
                                <div className="grid grid-cols-2 gap-4">
                                    {topicData.data?.map((imageData, imageIndex) => (
                                        <div key={imageIndex} className="relative">
                                            <div className="relative inline-block">
                                                <img 
                                                    src={imageData.image} 
                                                    alt={`${topicData.topic} image ${imageIndex + 1}`}
                                                    className="w-full h-auto rounded-lg shadow-md"
                                                />
                                                
                                                
                                                {/* {imageData.boundingPoly && (
                                                    <BoundingPolyViewer 
                                                        imageData={imageData}
                                                        className="absolute inset-0"
                                                    />
                                                )} */}
                                            </div>
                                            {(imageData.error || imageData.status) && (
                                                <div className="mt-2 text-sm text-red-600 bg-red-50 p-2 rounded">
                                                    Status: {imageData.error || imageData.status}
                                                </div>
                                            )}
                                            
      
                                            {/* Face detected - show emotions */}
                                            {imageData.boundingPoly && (
                                                <div className="mt-2">
                                                    <div className="text-sm text-green-600 mb-2">
                                                        Face detected
                                                    </div>
                                                    <div className="flex flex-wrap gap-1">
                                                        {(() => {
                                                            const emotions = [];
                                                            
                                                            // Create emotions array from image data
                                                            if (imageData.anger && imageData.anger !== 'UNKNOWN') {
                                                                emotions.push({ name: 'anger', level: imageData.anger });
                                                            }
                                                            if (imageData.joy && imageData.joy !== 'UNKNOWN') {
                                                                emotions.push({ name: 'joy', level: imageData.joy });
                                                            }
                                                            if (imageData.surprise && imageData.surprise !== 'UNKNOWN') {
                                                                emotions.push({ name: 'surprise', level: imageData.surprise });
                                                            }
                                                            
                                                            return emotions.map((emotion, emotionIndex) => (
                                                                <span 
                                                                    key={emotionIndex}
                                                                    className={`px-2 py-1 rounded-full text-xs text-white ${getEmotionColor(emotion.level)}`}
                                                                >
                                                                    {getEmotionIcon(emotion.name)} {emotion.name.charAt(0).toUpperCase() + emotion.name.slice(1)}: {emotion.level}
                                                                </span>
                                                            ));
                                                        })()}
                                                    </div>
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                                
                                {/* Topic summary */}
                                <div className="mt-4 text-sm text-gray-500">
                                    {topicData.data?.length || 0} images in this topic
                                </div>
                            </div>
                        ))}
                    </div>
        </div>
      </div>
    </div>
  );
}
