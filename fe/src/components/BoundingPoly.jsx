import { parseBoundingPoly } from "@/lib/helpers";

import React, { useRef, useEffect, useState } from 'react';

const BoundingPolyViewer = ({ image, boundingPoly }) => {
  const imgRef = useRef(null);
  const canvasRef = useRef(null);
  const [imageLoaded, setImageLoaded] = useState(false);
  const vertices = parseBoundingPoly(boundingPoly);

  useEffect(() => {
    if (!imageLoaded || !imgRef.current || !canvasRef.current) return;

    const canvas = canvasRef.current;
    const img = imgRef.current;
    canvas.width = img.offsetWidth;
    canvas.height = img.offsetHeight;

    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.strokeStyle = 'red';
    ctx.lineWidth = 2;

    vertices.forEach((poly) => {
      ctx.beginPath();
      poly.vertices.forEach((v, i) => {
        // Handle normalized vertices if needed
        const x = v.x ?? 0;
        const y = v.y ?? 0;
        i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y);
      });
      ctx.closePath();
      ctx.stroke();
    });
  }, [imageLoaded, vertices]);

  return (
    <div className="relative inline-block">
      <img
        ref={imgRef}
        src={image}
        alt="Analyzed"
        onLoad={() => setImageLoaded(true)}
        className="block max-w-full h-auto"
      />
      <canvas
        ref={canvasRef}
        className="absolute top-0 left-0 pointer-events-none"
      />
    </div>
  );
};

export default BoundingPolyViewer;
