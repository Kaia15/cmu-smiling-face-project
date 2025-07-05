"use client"

import { BadgeDemo } from "@/components/BadgeList";
import { InputWithButton } from "@/components/InputWithButton";
import { TypographyH2 } from "@/components/Typographyh2";
import { useImage } from "@/hooks/useImage";

export default function SearchPage({imageState}) {
  const {setTopics, topics, loading, handleSubmit, input, handleEnter, setInput, handleClear} = imageState;
  return (
    <div className="flex flex-col justify-center h-screen items-center">
      <TypographyH2 />
      <InputWithButton topics={topics} setTopics={setTopics} 
      loading={loading} handleSubmit={handleSubmit}
      input={input}
      handleEnter={handleEnter}
      setInput={setInput}
      handleClear={handleClear}
      />
      <BadgeDemo />
    </div>
  );
}
