import React, { useState, useRef } from 'react';

interface ComposerProps {
  placeholder?: string;
  onSend?: (text: string) => void;
  disabled?: boolean;
}

export function Composer({ placeholder = "向 ChatGPT 发送消息...", onSend, disabled = false }: ComposerProps) {
  const [message, setMessage] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const handleInput = () => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 200)}px`;
    }
  };

  const handleSendClick = () => {
    if (message.trim() && !disabled) {
      if (onSend) {
        onSend(message);
      }
      setMessage('');
      if (textareaRef.current) {
        textareaRef.current.style.height = 'auto';
      }
    }
  };

  return (
    <div className={`w-full bg-surface-container-lowest rounded-2xl shadow-lg border border-outline-variant/10 p-2 pointer-events-auto transition-shadow focus-within:shadow-xl focus-within:border-primary/30 ${disabled ? 'opacity-70 pointer-events-none' : ''}`}>
      <div className="flex items-end gap-2">
        <button className="p-3 text-on-surface-variant hover:text-primary transition-colors rounded-xl hover:bg-surface-container-low" title="附加文件">
          <span className="material-symbols-outlined">attach_file</span>
        </button>
        <textarea
          ref={textareaRef}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          onInput={handleInput}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              handleSendClick();
            }
          }}
          className="flex-1 bg-transparent border-none focus:ring-0 resize-none py-3 px-2 text-on-background placeholder:text-on-surface-variant/50 focus:outline-none max-h-[200px] overflow-y-auto"
          placeholder={placeholder}
          rows={1}
          disabled={disabled}
        ></textarea>
        <div className="flex items-center gap-1 pb-1 pr-1">
          <button className="p-2 text-on-surface-variant hover:text-primary transition-colors rounded-xl hover:bg-surface-container-low" title="语音输入">
            <span className="material-symbols-outlined">mic</span>
          </button>
          <button className="p-2 text-on-surface-variant hover:text-primary transition-colors rounded-xl hover:bg-surface-container-low" title="上传图片">
            <span className="material-symbols-outlined">image</span>
          </button>
          <button 
            onClick={handleSendClick}
            disabled={!message.trim() || disabled}
            className="ml-1 bg-primary text-on-primary w-10 h-10 rounded-xl flex items-center justify-center hover:bg-primary-dim transition-colors shadow-md disabled:opacity-50 disabled:cursor-not-allowed"
            title="发送"
          >
            <span className="material-symbols-outlined filled">send</span>
          </button>
        </div>
      </div>
    </div>
  );
}
