"use client"

import DisplayPage from "@/components/DisplayPage";
import SearchPage from "@/components/SearchPage";
import { useImage } from "@/hooks/useImage";
import { useEffect } from "react";

export default function Home() {
  const imageState = useImage();

  return (
    <div>
      {imageState.images.length > 0 ? <DisplayPage imageState={imageState}/> : <SearchPage imageState={imageState}/>}
    </div>
  );
}
