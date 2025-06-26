import { parseBoundingPoly } from "@/lib/helpers";

export default function BoundingPolyViewer({ boundingPoly, image }) {
  const vertices = parseBoundingPoly(boundingPoly);
  const points = vertices.map(v => `${v.x},${v.y}`).join(" ");

  return (
    <div className="relative inline-block">
      <img src={image} alt="Annotated" className="block" width={600} height={400} />
      <svg
        className="absolute top-0 left-0 pointer-events-none w-full h-full"
        width={600}
        height={400}
      >
        <polygon
          points={points}
          fill="rgba(255, 0, 0, 0.3)"
          stroke="red"
          strokeWidth={2}
        />
      </svg>
    </div>
  );
}
