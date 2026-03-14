import React, { useState, useEffect } from 'react';
import { Composer } from '../components/Composer';
import { api, TimelineEventVO } from '../services/api';

export function TimelinePage() {
  const [events, setEvents] = useState<TimelineEventVO[]>([]);
  const [loading, setLoading] = useState(true);
  
  // Hardcoded for now
  const currentUserId = 1;

  useEffect(() => {
    const fetchTimeline = async () => {
      try {
        setLoading(true);
        const data = await api.user.getTimeline(currentUserId);
        setEvents(data.events || []);
      } catch (error) {
        console.error('Failed to fetch timeline:', error);
      } finally {
        setLoading(false);
      }
    };
    
    fetchTimeline();
  }, []);

  const formatDate = (timestamp: number) => {
    return new Date(timestamp).toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  const getEventIcon = (type: string) => {
    switch (type) {
      case 'PROJECT_SPACE': return 'folder';
      case 'CHAT_SESSION': return 'chat';
      case 'MESSAGE_EVENT': return 'forum';
      case 'MEMORY_RECORD': return 'psychology';
      case 'CONVERSATION_SUMMARY': return 'summarize';
      default: return 'flag';
    }
  };

  return (
    <>
      <main className="flex-1 flex flex-col min-w-0 relative bg-surface">
        <header className="h-16 px-8 flex items-center justify-between sticky top-0 z-10 bg-surface/80 backdrop-blur-md border-b border-outline-variant/10">
          <div className="flex items-center gap-4">
            <h1 className="font-headline font-bold text-xl text-on-background">当前项目语境</h1>
            <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-primary-container text-on-primary-container tracking-tighter">记忆时间轴视图</span>
          </div>
          <div className="flex items-center gap-2">
            <button className="p-2 rounded-full hover:bg-surface-container transition-colors text-on-surface-variant"><span className="material-symbols-outlined">info</span></button>
            <button className="p-2 rounded-full hover:bg-surface-container transition-colors text-on-surface-variant"><span className="material-symbols-outlined">auto_awesome</span></button>
            <button className="p-2 rounded-full hover:bg-surface-container transition-colors text-on-surface-variant"><span className="material-symbols-outlined">more_vert</span></button>
            <div className="ml-2 h-8 w-[1px] bg-outline-variant/30"></div>
            <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuAvJoQ1SBuZO2fAWB265KU4CM8M9XiHS-4mkwwZBFtH2LRRfOlJhTG4G5r2wTOaTjGZOulovsT43VPHkehkDI2bKdQZVUGQmKOjb9ws5ftO-GPTL5tbRffsVu8xw_Rn8HTczXYze45P9B6kXuXAH85uMjkQLOLlVP5bxAQjjsg1VLNp8LFDFBqf0JUdAuMgTNCM7tFingeIKfBVAKN43Kx6NFY6MzWXOG58WA1HdKbFgsqCAVK7t-esPqO97BkcOyy8L9huh_85AZya" className="w-8 h-8 rounded-full border border-outline-variant/20 object-cover ml-2" alt="Profile" />
          </div>
        </header>
        <div className="flex-1 overflow-y-auto p-10 lg:p-20">
          <div className="max-w-4xl mx-auto pb-24">
            <div className="mb-20 text-center lg:text-left">
              <h2 className="font-headline text-5xl font-extrabold text-on-surface tracking-tighter mb-4 leading-none">记忆里程碑</h2>
              <p className="text-outline text-lg max-w-2xl leading-relaxed">一份关于我们协作进化和收集的关键智能的高保真按时间顺序排列的图谱。</p>
            </div>
            <div className="relative">
              <div className="absolute left-8 top-4 bottom-4 w-[2px] bg-surface-container-highest rounded-full"></div>
              <div className="space-y-16">
                
                {loading ? (
                  <div className="relative flex items-start group">
                    <div className="absolute left-8 -translate-x-1/2 w-4 h-4 rounded-full bg-primary/40 border-4 border-surface z-10"></div>
                    <div className="ml-20 w-full">
                      <div className="flex items-center gap-3 mb-4">
                        <span className="px-3 py-1 bg-surface-container-low text-outline text-xs font-bold rounded-full">加载中</span>
                      </div>
                      <div className="p-8 rounded-xl border border-dashed border-outline-variant/30 flex items-center justify-center">
                        <div className="flex flex-col items-center gap-2">
                          <div className="w-10 h-10 rounded-full border-2 border-primary border-t-transparent animate-spin"></div>
                          <p className="text-xs font-headline font-bold text-outline uppercase tracking-widest mt-2">正在获取时间轴数据</p>
                        </div>
                      </div>
                    </div>
                  </div>
                ) : events.length > 0 ? (
                  events.map((event, index) => (
                    <div key={`${event.eventType}-${event.sourceId}-${index}`} className="relative flex items-start group">
                      <div className="absolute left-8 -translate-x-1/2 w-4 h-4 rounded-full bg-primary border-4 border-surface z-10 group-hover:scale-125 transition-transform"></div>
                      <div className="ml-20 w-full">
                        <div className="flex items-center gap-3 mb-4">
                          <span className="px-3 py-1 bg-surface-container-lowest text-primary text-xs font-bold rounded-full border border-outline-variant/15">{formatDate(event.eventTime)}</span>
                          <span className="material-symbols-outlined text-outline text-lg">{getEventIcon(event.eventType)}</span>
                        </div>
                        <div className="bg-surface-container-lowest p-8 rounded-xl border border-outline-variant/10 shadow-sm hover:shadow-md transition-shadow">
                          <h3 className="font-headline text-2xl font-bold mb-3 text-on-surface">{event.title}</h3>
                          <p className="text-on-surface-variant leading-relaxed">{event.summary}</p>
                          {event.detail && (
                            <div className="mt-4 p-4 bg-surface-container-lowest rounded-lg border border-outline-variant/10 text-sm text-on-surface-variant">
                              {event.detail}
                            </div>
                          )}
                          <div className="mt-6 flex flex-wrap gap-2">
                            <span className="text-[10px] font-bold tracking-widest uppercase py-1 px-3 bg-surface-container rounded-full text-on-surface-variant">{event.eventType}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="relative flex items-start group">
                    <div className="absolute left-8 -translate-x-1/2 w-4 h-4 rounded-full bg-primary/40 border-4 border-surface z-10"></div>
                    <div className="ml-20 w-full">
                      <div className="p-8 rounded-xl border border-dashed border-outline-variant/30 text-center text-on-surface-variant">
                        暂无时间轴数据
                      </div>
                    </div>
                  </div>
                )}
                
              </div>
            </div>
          </div>
        </div>
        {/* Composer */}
        <div className="absolute bottom-8 left-1/2 -translate-x-1/2 w-full max-w-3xl px-6">
          <Composer />
        </div>
      </main>
      <aside className="w-80 border-l border-outline-variant/15 bg-surface-container-low/30 hidden xl:flex flex-col shrink-0">
        <div className="p-6 border-b border-outline-variant/15">
          <div className="flex items-center justify-between mb-6">
            <h3 className="font-headline font-bold text-sm text-on-surface">记忆统计</h3>
            <span className="material-symbols-outlined text-primary">psychology</span>
          </div>
          <div className="space-y-4">
            <div className="p-4 rounded-lg bg-surface-container-lowest border border-outline-variant/10">
              <p className="text-[10px] text-outline font-bold uppercase tracking-widest mb-1">事件总数</p>
              <p className="text-2xl font-headline font-black text-on-surface">{events.length}</p>
            </div>
          </div>
        </div>
      </aside>
    </>
  );
}
