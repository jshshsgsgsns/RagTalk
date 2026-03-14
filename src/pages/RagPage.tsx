import React, { useState } from 'react';
import { Composer } from '../components/Composer';
import { api, ChatSendResponse } from '../services/api';

export function RagPage() {
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<ChatSendResponse | null>(null);
  
  // Hardcoded for now
  const currentUserId = 1;

  const handleSend = async (text: string) => {
    if (!text.trim()) return;
    
    setQuery(text);
    setLoading(true);
    setResult(null);
    
    try {
      const response = await api.chat.ragSend({
        projectId: 1, // Hardcoded for now
        chatId: 1, // Hardcoded for now
        message: text
      });
      setResult(response);
    } catch (error) {
      console.error('Failed to send RAG query:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <main className="flex-1 flex flex-col h-screen overflow-y-auto bg-surface relative">
        <header className="sticky top-0 z-50 glass-nav h-16 px-8 flex items-center justify-between border-b border-surface-container bg-surface/80 backdrop-blur-md">
          <div className="flex items-center gap-4">
            <span className="material-symbols-outlined text-primary">data_object</span>
            <h2 className="font-headline font-bold text-on-surface tracking-tight">当前项目上下文</h2>
            <div className="px-3 py-1 bg-primary-container text-on-primary-container text-[10px] font-bold tracking-widest uppercase rounded-full border border-primary/20">知识库检索</div>
          </div>
          <div className="flex items-center gap-3">
            <button className="w-10 h-10 rounded-full flex items-center justify-center hover:bg-surface-container transition-colors"><span className="material-symbols-outlined text-on-surface-variant text-[20px]">info</span></button>
            <button className="w-10 h-10 rounded-full flex items-center justify-center hover:bg-surface-container transition-colors"><span className="material-symbols-outlined text-on-surface-variant text-[20px]">auto_awesome</span></button>
            <button className="w-10 h-10 rounded-full flex items-center justify-center hover:bg-surface-container transition-colors"><span className="material-symbols-outlined text-on-surface-variant text-[20px]">more_vert</span></button>
          </div>
        </header>
        <div className="flex-1 max-w-5xl mx-auto w-full py-12 px-8 pb-32">
          
          {!query && !loading && !result && (
            <div className="flex flex-col items-center justify-center h-full text-center space-y-6 opacity-60">
              <span className="material-symbols-outlined text-6xl text-primary">database</span>
              <h2 className="font-headline text-2xl font-bold">向您的知识库提问</h2>
              <p className="text-sm text-on-surface-variant max-w-md">通过 RAG (检索增强生成) 技术，您可以直接向您的文档、记忆和历史记录提问，获取基于事实的准确回答。</p>
            </div>
          )}

          {query && (
            <section className="mb-16">
              <div className="flex items-center gap-4 mb-6">
                <div className="w-10 h-10 rounded-full bg-surface-container-highest flex items-center justify-center shadow-sm">
                  <span className="material-symbols-outlined text-primary">person</span>
                </div>
                <span className="text-xs font-bold uppercase tracking-widest text-outline-variant">用户查询</span>
              </div>
              <div className="bg-surface-container-lowest p-8 rounded-2xl shadow-sm border border-outline-variant/5">
                <h1 className="font-headline text-2xl font-semibold text-on-background tracking-tight">{query}</h1>
              </div>
            </section>
          )}

          {loading && (
            <div className="flex items-center justify-center p-12">
              <div className="flex flex-col items-center gap-4">
                <div className="w-12 h-12 rounded-full border-4 border-primary border-t-transparent animate-spin"></div>
                <p className="text-sm font-bold text-on-surface-variant uppercase tracking-widest">正在检索知识库并生成回答...</p>
              </div>
            </div>
          )}

          {result && (
            <>
              {result.retrievedMemories && result.retrievedMemories.total > 0 && (
                <section className="mb-16">
                  <div className="flex items-center justify-between mb-8">
                    <div className="flex items-center gap-4">
                      <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
                        <span className="material-symbols-outlined text-primary">manage_search</span>
                      </div>
                      <div>
                        <span className="text-xs font-bold uppercase tracking-widest text-outline-variant">上下文检索</span>
                        <p className="text-sm text-outline font-medium">找到 {result.retrievedMemories.total} 个相关片段</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 px-4 py-2 bg-surface-container-low rounded-lg border border-outline-variant/10">
                      <span className="w-2 h-2 rounded-full bg-primary animate-pulse"></span>
                      <span className="text-[10px] font-bold uppercase text-primary">神经匹配已激活</span>
                    </div>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-start">
                    {[...(result.retrievedMemories.projectMemories || []), ...(result.retrievedMemories.globalMemories || []), ...(result.retrievedMemories.chatMemories || [])].slice(0, 4).map((source: any, idx) => (
                      <div key={idx} className={`bg-surface-container-lowest p-6 rounded-2xl border border-outline-variant/10 hover:border-primary/20 transition-all group ${idx === 2 ? 'md:col-span-2' : ''}`}>
                        <div className="flex items-center justify-between mb-4">
                          <div className="flex items-center gap-2">
                            <span className="material-symbols-outlined text-primary text-lg">description</span>
                            <span className="text-xs font-bold text-on-surface-variant">{source.title || source.memoryType || 'Memory'}</span>
                          </div>
                          <div className="text-[10px] font-bold px-2 py-0.5 bg-primary/5 text-primary rounded border border-primary/10">{(source.confidence || 0.9 * 100).toFixed(1)}% 相关度</div>
                        </div>
                        <p className="text-sm leading-relaxed text-on-background/80 mb-4">“{source.summary || source.detailText}”</p>
                      </div>
                    ))}
                  </div>
                </section>
              )}

              <section className="mb-24">
                <div className="flex items-center gap-4 mb-8">
                  <div className="w-10 h-10 rounded-full bg-primary flex items-center justify-center shadow-lg shadow-primary/20">
                    <span className="material-symbols-outlined text-on-primary">auto_awesome</span>
                  </div>
                  <span className="text-xs font-bold uppercase tracking-widest text-primary">综合分析</span>
                </div>
                <div className="space-y-6">
                  <div className="max-w-none bg-surface-container-lowest p-8 rounded-2xl shadow-sm border border-outline-variant/5">
                    <p className="text-lg leading-relaxed text-on-background/90 whitespace-pre-wrap">{result.assistantReply}</p>
                  </div>
                </div>
              </section>
            </>
          )}
        </div>
        <div className="fixed bottom-0 left-72 right-80 p-8 glass-nav border-t border-surface-container-high bg-surface/90">
          <div className="max-w-4xl mx-auto">
            <Composer placeholder="给知识库发消息..." onSend={handleSend} disabled={loading} />
            <p className="text-center text-[10px] text-outline-variant mt-4 font-medium uppercase tracking-widest">由 RAG 引擎 v4.2 提供支持 • 企业智能</p>
          </div>
        </div>
      </main>
      <aside className="w-80 bg-surface-container-low border-l border-surface-container-high flex flex-col h-screen shrink-0">
        <div className="p-6 border-b border-surface-container-high">
          <h3 className="font-headline font-bold text-on-background tracking-tight">上下文助手</h3>
          <p className="text-xs text-outline-variant uppercase tracking-widest font-medium">知识库</p>
        </div>
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          <div>
            <span className="text-[10px] font-bold uppercase tracking-wider text-outline-variant/60 block mb-3">项目筛选</span>
            <div className="bg-surface-container-lowest p-3 rounded-xl border border-primary/20 flex items-center gap-3">
              <div className="w-8 h-8 rounded-lg bg-primary-container flex items-center justify-center">
                <span className="material-symbols-outlined text-primary text-sm">finance</span>
              </div>
              <div>
                <p className="text-xs font-bold text-on-surface">全局搜索</p>
                <p className="text-[10px] text-outline-variant">所有文档</p>
              </div>
              <button className="ml-auto text-outline-variant hover:text-error"><span className="material-symbols-outlined text-sm">close</span></button>
            </div>
          </div>
          <div className="space-y-4">
            <div className="flex items-center gap-3 p-3 rounded-xl bg-primary/10 border border-primary/10">
              <span className="material-symbols-outlined text-primary filled">psychology</span>
              <span className="text-sm font-bold text-primary">记忆</span>
            </div>
            <div className="flex items-center gap-3 p-3 rounded-xl hover:bg-surface-container transition-colors cursor-pointer group">
              <span className="material-symbols-outlined text-outline-variant group-hover:text-on-surface">database</span>
              <span className="text-sm font-medium text-on-surface-variant group-hover:text-on-surface">RAG 结果</span>
            </div>
            <div className="flex items-center gap-3 p-3 rounded-xl hover:bg-surface-container transition-colors cursor-pointer group">
              <span className="material-symbols-outlined text-outline-variant group-hover:text-on-surface">summarize</span>
              <span className="text-sm font-medium text-on-surface-variant group-hover:text-on-surface">摘要</span>
            </div>
          </div>
          <div className="pt-6 border-t border-surface-container-high">
            <span className="text-[10px] font-bold uppercase tracking-wider text-outline-variant/60 block mb-4">检索统计</span>
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <span className="text-xs text-on-surface-variant">搜索延迟</span>
                <span className="text-xs font-bold text-primary">120ms</span>
              </div>
              <div className="w-full bg-surface-container-highest h-1.5 rounded-full overflow-hidden">
                <div className="bg-primary h-full w-[85%]"></div>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-xs text-on-surface-variant">Vector Precision</span>
                <span className="text-xs font-bold text-primary">96%</span>
              </div>
              <div className="w-full bg-surface-container-highest h-1.5 rounded-full overflow-hidden">
                <div className="bg-primary h-full w-[96%]"></div>
              </div>
            </div>
          </div>
        </div>
        <div className="p-6">
          <div className="bg-inverse-surface rounded-xl p-4 text-center">
            <p className="text-[10px] text-surface-variant/70 uppercase tracking-widest mb-2">工作区洞察</p>
            <p className="text-xs text-surface-container-low font-medium leading-relaxed">“您的数据源目前已 100% 同步。”</p>
          </div>
        </div>
      </aside>
    </>
  );
}
