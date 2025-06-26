import { BadgeDemo } from "@/components/BadgeList";
import { InputWithButton } from "@/components/InputWithButton";
import { TypographyH2 } from "@/components/Typographyh2";
import { useImage } from "@/hooks/useImage";

export default function SearchPage({imageState}) {
  const {setTopic, topic, loading, handleSubmit} = imageState;
  return (
    <div className="flex flex-col justify-center h-screen items-center">
      <TypographyH2 />
      <InputWithButton topic={topic} setTopic={setTopic} loading={loading} handleSubmit={handleSubmit}/>
      <BadgeDemo />
    </div>
  );
}
