import React, { useState } from 'react';
import { RightSidebar } from '../components/RightSidebar';
import { MemoryGrid } from '../components/MemoryGrid';

export function WorkspacePage() {
  const [isRightSidebarOpen, setIsRightSidebarOpen] = useState(false);

  return (
    <>
      <main className="flex-1 flex flex-col min-w-0 relative">
        <header className="h-16 px-8 flex items-center justify-between sticky top-0 z-10 bg-surface/80 backdrop-blur-md border-b border-outline-variant/10">
          <div className="flex items-center gap-4">
            <h1 className="font-headline font-bold text-xl text-on-background">当前项目上下文</h1>
            <div className="h-4 w-[1px] bg-outline-variant/30"></div>
            <div className="flex items-center gap-2 text-sm text-on-surface-variant font-medium">
              <span className="material-symbols-outlined text-sm">database</span>
              记忆工作区
            </div>
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

        <MemoryGrid />

        <div className="absolute bottom-0 left-0 right-0 p-8 flex justify-center bg-gradient-to-t from-surface via-surface to-transparent pointer-events-none">
          <div className="w-full max-w-3xl bg-surface-container-lowest rounded-2xl shadow-lg border border-outline-variant/10 p-2 pointer-events-auto">
            <div className="flex items-end gap-2">
              <button className="p-3 text-on-surface-variant hover:text-primary transition-colors rounded-xl hover:bg-surface-container-low">
                <span className="material-symbols-outlined">attach_file</span>
              </button>
              <textarea
                className="flex-1 bg-transparent border-none focus:ring-0 resize-none py-3 px-2 text-on-background placeholder:text-on-surface-variant/50 focus:outline-none"
                placeholder="向 ChatGPT 发送消息..."
                rows={1}
              ></textarea>
              <div className="flex items-center gap-1 pb-1 pr-1">
                <button className="p-2 text-on-surface-variant hover:text-primary transition-colors rounded-xl hover:bg-surface-container-low">
                  <span className="material-symbols-outlined">mic</span>
                </button>
                <button className="p-2 text-on-surface-variant hover:text-primary transition-colors rounded-xl hover:bg-surface-container-low">
                  <span className="material-symbols-outlined">image</span>
                </button>
                <button className="ml-1 bg-inverse-surface text-on-secondary w-10 h-10 rounded-xl flex items-center justify-center hover:bg-on-background transition-colors shadow-md">
                  <span className="material-symbols-outlined filled">send</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>

      <RightSidebar isOpen={isRightSidebarOpen} onClose={() => setIsRightSidebarOpen(false)} />
    </>
  );
}
