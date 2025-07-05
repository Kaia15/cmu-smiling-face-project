export function parseBoundingPoly(rawText) {
  const lines = rawText?.split('\n').map(line => line.trim()).filter(Boolean);
  const vertices = [];

  for (let i = 0; i < lines.length; i++) {
    if (lines[i].startsWith('x:')) {
      const x = parseInt(lines[i].split(':')[1].trim(), 10);
      const yLine = lines[i + 1];
      const y = yLine && yLine.startsWith('y:') ? parseInt(yLine.split(':')[1].trim(), 10) : 0;
      vertices.push({ x, y });
      i++; // skip next line since we already used it
    }
  }

  return [{ vertices }];
}

export function getEmotionColor (level) {
    const colors = {
      'VERY_LIKELY': 'bg-emerald-500',
      'LIKELY': 'bg-blue-500',
      'POSSIBLE': 'bg-yellow-500',
      'UNLIKELY': 'bg-orange-500',
      'VERY_UNLIKELY': 'bg-red-500'
    };
    return colors[level] || 'bg-gray-500';
  };

export function getEmotionIcon (emotion) {
    const icons = {
      joy: '😊',
      surprise: '😲',
      anger: '😠',
      sorrow: '😢',
      fear: '😨'
    };
    return icons[emotion] || '😐';
  };