import { useEffect } from 'react';

interface Props {
  imageUrl: string;
  filename: string;
  onClose: () => void;
}

export default function ImageModal({ imageUrl, filename, onClose }: Props) {
  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [onClose]);

  const handleDownload = () => {
    const a = document.createElement('a');
    a.href = imageUrl;
    a.download = filename;
    a.target = '_blank';
    a.click();
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center"
      onClick={onClose}
    >
      <div
        className="relative max-w-[90vw] max-h-[90vh]"
        onClick={(e) => e.stopPropagation()}
      >
        <img
          src={imageUrl}
          alt={filename}
          className="max-w-full max-h-[85vh] object-contain rounded-lg"
        />
        <div className="absolute top-3 right-3 flex gap-2">
          <button
            onClick={handleDownload}
            className="w-9 h-9 bg-white/90 rounded-full flex items-center justify-center hover:bg-white shadow text-sm"
            title="다운로드"
          >
            ⬇
          </button>
          <button
            onClick={onClose}
            className="w-9 h-9 bg-white/90 rounded-full flex items-center justify-center hover:bg-white shadow text-sm"
            title="닫기"
          >
            ✕
          </button>
        </div>
        <p className="text-center text-white/80 text-sm mt-2">{filename}</p>
      </div>
    </div>
  );
}
