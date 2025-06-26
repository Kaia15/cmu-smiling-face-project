export function parseBoundingPoly(polyString) {
  const vertexRegex = /x:\s*(\d+)\s*y:\s*(\d+)/g;
  const vertices = [];
  let match;
  while ((match = vertexRegex.exec(polyString)) !== null) {
    vertices.push({ x: Number(match[1]), y: Number(match[2]) });
  }
  return vertices;
}