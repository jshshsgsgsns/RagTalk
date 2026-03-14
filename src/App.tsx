import React, { useEffect, useState } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { LeftSidebar } from './components/LeftSidebar';
import { RightSidebar } from './components/RightSidebar';
import { MemoryGrid } from './components/MemoryGrid';
import { TimelinePage } from './pages/TimelinePage';
import { PersonaPage } from './pages/PersonaPage';
import { RagPage } from './pages/RagPage';
import { ChatPage } from './pages/ChatPage';
import { SettingsPage, THEMES } from './pages/SettingsPage';
import { Composer } from './components/Composer';

export default function App() {
  const [isRightSidebarOpen, setIsRightSidebarOpen] = useState(false);

  useEffect(() => {
    const savedThemeName = localStorage.getItem('app-theme-name');
    if (savedThemeName) {
      const theme = THEMES.find(t => t.name === savedThemeName);
      if (theme) {
        Object.entries(theme.vars).forEach(([key, value]) => {
          document.documentElement.style.setProperty(key, value);
        });
      }
    }
  }, []);

  return (
    <Router>
      <div className="flex h-screen overflow-hidden bg-surface text-on-background">
        <LeftSidebar />
        
        <Routes>
          <Route path="/" element={<ChatPage />} />
          <Route path="/memory" element={
            <>
              <main className="flex-1 flex flex-col min-w-0 relative">
                {/* TopNavBar */}
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

                {/* Main Scrollable Content */}
                <MemoryGrid />

                {/* Composer (Fixed at bottom of main area) */}
                <div className="absolute bottom-0 left-0 right-0 p-8 flex justify-center bg-gradient-to-t from-surface via-surface to-transparent pointer-events-none">
                  <div className="w-full max-w-3xl pointer-events-auto">
                    <Composer />
                  </div>
                </div>
              </main>

              <RightSidebar isOpen={isRightSidebarOpen} onClose={() => setIsRightSidebarOpen(false)} />
            </>
          } />
          <Route path="/timeline" element={<TimelinePage />} />
          <Route path="/persona" element={<PersonaPage />} />
          <Route path="/rag" element={<RagPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Routes>
      </div>
    </Router>
  );
}
