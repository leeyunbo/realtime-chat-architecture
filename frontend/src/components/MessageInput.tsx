import { useState, useRef, type FormEvent } from 'react';

interface Props {
  roomId: number;
  onSend?: (content: string) => void;
  onSendFile?: (file: File) => void;
  uploading?: boolean;
}

export default function MessageInput({ roomId: _roomId, onSend, onSendFile, uploading }: Props) {
  const [content, setContent] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (uploading) return;

    if (selectedFile && onSendFile) {
      onSendFile(selectedFile);
      clearFile();
      return;
    }

    const trimmed = content.trim();
    if (!trimmed || !onSend) return;
    onSend(trimmed);
    setContent('');
  };

  const MAX_FILE_SIZE = 20 * 1024 * 1024;

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > MAX_FILE_SIZE) {
      alert(`파일 크기가 ${formatSize(MAX_FILE_SIZE)}를 초과합니다. (${formatSize(file.size)})`);
      if (fileInputRef.current) fileInputRef.current.value = '';
      return;
    }

    setSelectedFile(file);

    if (file.type.startsWith('image/')) {
      const url = URL.createObjectURL(file);
      setPreview(url);
    } else {
      setPreview(null);
    }
  };

  const clearFile = () => {
    if (preview) URL.revokeObjectURL(preview);
    setSelectedFile(null);
    setPreview(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes}B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
  };

  const canSend = onSend && (selectedFile || content.trim());

  return (
    <div className="border-t border-gray-200">
      {selectedFile && (
        <div className="px-4 pt-3 flex items-center gap-3">
          <div className="relative">
            {preview ? (
              <img src={preview} alt="preview" className="w-16 h-16 object-cover rounded-lg" />
            ) : (
              <div className="w-16 h-16 bg-gray-100 rounded-lg flex items-center justify-center">
                <span className="text-2xl">📄</span>
              </div>
            )}
            <button
              type="button"
              onClick={clearFile}
              className="absolute -top-2 -right-2 w-5 h-5 bg-gray-500 text-white rounded-full text-xs flex items-center justify-center hover:bg-gray-700"
            >
              ✕
            </button>
          </div>
          <div className="text-sm text-gray-600 truncate max-w-xs">
            <p className="font-medium truncate">{selectedFile.name}</p>
            <p className="text-xs text-gray-400">{formatSize(selectedFile.size)}</p>
          </div>
          {uploading && <span className="text-xs text-blue-500 animate-pulse">업로드 중...</span>}
        </div>
      )}
      <form onSubmit={handleSubmit} className="p-4 flex gap-2">
        <input
          ref={fileInputRef}
          type="file"
          onChange={handleFileSelect}
          className="hidden"
          accept="image/jpeg,image/png,image/gif,image/webp,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        />
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          disabled={!onSend || uploading}
          className="px-3 py-2 text-gray-500 hover:text-gray-700 disabled:opacity-50"
          title="파일 첨부"
        >
          📎
        </button>
        <input
          type="text"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder={onSend ? (selectedFile ? '파일을 전송합니다' : 'Type a message...') : 'Connecting...'}
          disabled={!onSend || !!selectedFile || uploading}
          className="flex-1 px-4 py-2 border border-gray-300 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
        />
        <button
          type="submit"
          disabled={!canSend || uploading}
          className="px-4 py-2 bg-blue-600 text-white rounded-full hover:bg-blue-700 disabled:opacity-50"
        >
          Send
        </button>
      </form>
    </div>
  );
}
