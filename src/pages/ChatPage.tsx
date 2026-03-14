import React, { useState } from 'react';
import { Composer } from '../components/Composer';
import { RightSidebar } from '../components/RightSidebar';
import { api, ChatSendResponse } from '../services/api';

export function ChatPage() {
  const [messages, setMessages] = useState<{ role: 'user' | 'assistant', content: string }[]>([]);
  const [loading, setLoading] = useState(false);
  const [isRightSidebarOpen, setIsRightSidebarOpen] = useState(false);
  
  // Hardcoded for now
  const currentUserId = 1;

  const handleSend = async (text: string) => {
    if (!text.trim()) return;
    
    // Add user message
    setMessages(prev => [...prev, { role: 'user', content: text }]);
    setLoading(true);
    
    try {
      const response = await api.chat.send({
        projectId: 1, // Hardcoded for now
        chatId: 1, // Hardcoded for now
        message: text
      });
      
      // Add assistant message
      setMessages(prev => [...prev, { role: 'assistant', content: response.assistantReply }]);
    } catch (error) {
      console.error('Failed to send message:', error);
      setMessages(prev => [...prev, { role: 'assistant', content: '抱歉，发送消息时出错，请稍后重试。' }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <main className="flex-1 flex flex-col min-w-0 relative bg-surface">
        {/* TopNavBar */}
        <header className="h-16 px-8 flex items-center justify-between sticky top-0 z-10 bg-surface/80 backdrop-blur-md border-b border-outline-variant/10">
          <div className="flex items-center gap-4">
            <h1 className="font-headline font-bold text-xl text-on-background">新对话</h1>
          </div>
          <div className="flex items-center gap-2">
            <button 
              onClick={() => setIsRightSidebarOpen(!isRightSidebarOpen)}
              className={`p-2 rounded-full transition-colors ${isRightSidebarOpen ? 'bg-primary/10 text-primary' : 'hover:bg-surface-container text-on-surface-variant'}`}
              title="上下文助手"
            >
              <span className="material-symbols-outlined">info</span>
            </button>
            <button className="p-2 rounded-full hover:bg-surface-container transition-colors text-on-surface-variant">
              <span className="material-symbols-outlined">auto_awesome</span>
            </button>
            <button className="p-2 rounded-full hover:bg-surface-container transition-colors text-on-surface-variant">
              <span className="material-symbols-outlined">more_vert</span>
            </button>
            <div className="ml-2">
              <img
                src="https://lh3.googleusercontent.com/aida-public/AB6AXuAvJoQ1SBuZO2fAWB265KU4CM8M9XiHS-4mkwwZBFtH2LRRfOlJhTG4G5r2wTOaTjGZOulovsT43VPHkehkDI2bKdQZVUGQmKOjb9ws5ftO-GPTL5tbRffsVu8xw_Rn8HTczXYze45P9B6kXuXAH85uMjkQLOLlVP5bxAQjjsg1VLNp8LFDFBqf0JUdAuMgTNCM7tFingeIKfBVAKN43Kx6NFY6MzWXOG58WA1HdKbFgsqCAVK7t-esPqO97BkcOyy8L9huh_85AZya"
                alt="User Profile"
                className="w-8 h-8 rounded-full border border-outline-variant/20 object-cover"
              />
            </div>
          </div>
        </header>

        {/* Chat Content */}
        <div className="flex-1 overflow-y-auto p-8 flex flex-col">
          {messages.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center">
              <div className="w-16 h-16 rounded-2xl bg-primary-container/50 flex items-center justify-center mb-6">
                <span className="material-symbols-outlined text-primary text-3xl">chat_bubble</span>
              </div>
              <h2 className="font-headline text-2xl font-bold text-on-background mb-2">今天想聊点什么？</h2>
              <p className="text-on-surface-variant text-sm max-w-md text-center">
                我已准备好协助您。您可以询问关于项目上下文的问题，或者开始一个新的话题。
              </p>
            </div>
          ) : (
            <div className="max-w-3xl mx-auto w-full space-y-8 pb-8">
              {messages.map((msg, idx) => (
                <div key={idx} className={`flex gap-4 ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                  {msg.role === 'assistant' && (
                    <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center shrink-0 mt-1">
                      <span className="material-symbols-outlined text-primary text-sm">auto_awesome</span>
                    </div>
                  )}
                  <div className={`max-w-[80%] p-4 rounded-2xl ${
                    msg.role === 'user' 
                      ? 'bg-primary text-on-primary rounded-tr-sm' 
                      : 'bg-surface-container-lowest border border-outline-variant/10 rounded-tl-sm text-on-surface'
                  }`}>
                    <p className="whitespace-pre-wrap leading-relaxed">{msg.content}</p>
                  </div>
                  {msg.role === 'user' && (
                    <div className="w-8 h-8 rounded-full bg-surface-container-highest flex items-center justify-center shrink-0 mt-1">
                      <span className="material-symbols-outlined text-on-surface-variant text-sm">person</span>
                    </div>
                  )}
                </div>
              ))}
              {loading && (
                <div className="flex gap-4 justify-start">
                  <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center shrink-0 mt-1">
                    <span className="material-symbols-outlined text-primary text-sm">auto_awesome</span>
                  </div>
                  <div className="p-4 rounded-2xl bg-surface-container-lowest border border-outline-variant/10 rounded-tl-sm flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-primary/50 animate-bounce"></span>
                    <span className="w-2 h-2 rounded-full bg-primary/50 animate-bounce" style={{ animationDelay: '0.2s' }}></span>
                    <span className="w-2 h-2 rounded-full bg-primary/50 animate-bounce" style={{ animationDelay: '0.4s' }}></span>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Composer */}
        <div className="p-8 pt-0 flex justify-center bg-gradient-to-t from-surface via-surface to-transparent">
          <div className="w-full max-w-3xl">
            <Composer placeholder="发送消息给 ChatGPT..." onSend={handleSend} disabled={loading} />
          </div>
        </div>
      </main>

      <RightSidebar isOpen={isRightSidebarOpen} onClose={() => setIsRightSidebarOpen(false)} />
    </>
  );
}
