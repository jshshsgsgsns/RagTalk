import React, { useState, useEffect } from 'react';
import { api, MemoryRecordVO } from '../services/api';

interface Memory {
  id: number;
  confidence: number;
  priority: string;
  content: string;
  colorClass: string;
  bgClass: string;
  textClass: string;
  hasStar: boolean;
  type: '全局' | '项目' | '个人';
}

const mapScopeToType = (scope: string): '全局' | '项目' | '个人' => {
  switch (scope) {
    case 'global': return '全局';
    case 'project': return '项目';
    case 'chat': return '个人';
    default: return '项目';
  }
};

const mapTypeToScope = (type: '全局' | '项目' | '个人'): 'global' | 'project' | 'chat' => {
  switch (type) {
    case '全局': return 'global';
    case '项目': return 'project';
    case '个人': return 'chat';
  }
};

export function MemoryGrid() {
  const [memories, setMemories] = useState<Memory[]>([]);
  const [loading, setLoading] = useState(true);

  const [filter, setFilter] = useState<'全部' | '全局' | '项目' | '个人'>('全部');
  const [searchQuery, setSearchQuery] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newMemoryContent, setNewMemoryContent] = useState('');
  const [newMemoryType, setNewMemoryType] = useState<'全局' | '项目' | '个人'>('项目');
  
  // Hardcoded for now, should come from context/auth
  const currentUserId = 1;
  const currentProjectId = 1;

  useEffect(() => {
    fetchMemories();
  }, []);

  const fetchMemories = async () => {
    try {
      setLoading(true);
      const data = await api.memory.list({ userId: currentUserId, projectId: currentProjectId });
      
      const mappedMemories: Memory[] = data.map(record => ({
        id: record.id,
        confidence: Math.round((record.confidence || 0.8) * 100),
        priority: (record.importance || 0.7) > 0.8 ? '高' : (record.importance || 0.7) > 0.5 ? '中' : '低',
        content: record.summary,
        colorClass: 'bg-primary',
        bgClass: 'bg-primary/10',
        textClass: 'text-primary-dim',
        hasStar: (record.importance || 0) > 0.8,
        type: mapScopeToType(record.scopeType),
      }));
      
      setMemories(mappedMemories);
    } catch (error) {
      console.error('Failed to fetch memories:', error);
      // Fallback to empty or mock data if API fails
    } finally {
      setLoading(false);
    }
  };

  const filteredMemories = memories.filter(memory => {
    const matchesFilter = filter === '全部' || memory.type === filter;
    const matchesSearch = memory.content.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesFilter && matchesSearch;
  });

  const handleAddMemory = async () => {
    if (!newMemoryContent.trim()) return;
    
    try {
      await api.memory.create({
        userId: currentUserId,
        projectId: currentProjectId,
        scopeType: mapTypeToScope(newMemoryType),
        memoryType: 'fact', // Defaulting to fact
        summary: newMemoryContent,
        importance: 0.8,
        confidence: 0.9,
      });
      
      setNewMemoryContent('');
      setIsModalOpen(false);
      fetchMemories(); // Refresh list
    } catch (error) {
      console.error('Failed to create memory:', error);
      alert('保存记忆失败，请重试');
    }
  };

  return (
    <div className="p-8 max-w-6xl mx-auto w-full flex-1 overflow-y-auto relative">
      {/* Header Section */}
      <div className="mb-10">
        <h2 className="font-headline font-extrabold text-4xl mb-3 tracking-tight">记忆工作区</h2>
        <p className="text-on-surface-variant max-w-2xl leading-relaxed text-lg">
          管理 AI 记住的关于您的偏好、技术要求和项目约束的上下文及特定指令。
        </p>
      </div>

      {/* Filter Bar */}
      <div className="flex flex-col md:flex-row gap-4 mb-8 items-center justify-between bg-surface-container-low p-4 rounded-xl border border-outline-variant/10">
        <div className="flex items-center gap-3 overflow-x-auto pb-2 md:pb-0 w-full md:w-auto">
          <span className="text-sm font-semibold text-on-surface-variant px-2 whitespace-nowrap">筛选：</span>
          {(['全部', '全局', '项目', '个人'] as const).map((f) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={`px-4 py-1.5 rounded-full text-sm transition-colors whitespace-nowrap ${
                filter === f
                  ? 'bg-surface-container-lowest text-primary font-medium border border-primary/20 shadow-sm'
                  : 'bg-transparent text-on-surface-variant border border-outline-variant/30 hover:border-outline-variant'
              }`}
            >
              {f === '全部' ? '全部记忆' : f}
            </button>
          ))}
        </div>

        <div className="relative w-full md:w-64 shrink-0">
          <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant/60">
            search
          </span>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索知识..."
            className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg pl-10 pr-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 text-on-surface shadow-sm"
          />
        </div>
      </div>

      {/* Memory Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 pb-24">
        {filteredMemories.map((memory) => (
          <div
            key={memory.id}
            className="bg-surface-container-lowest p-6 rounded-xl group hover:shadow-xl hover:shadow-primary/5 transition-all flex flex-col border border-outline-variant/10 relative overflow-hidden"
          >
            <div className="absolute top-0 right-0 px-3 py-1 bg-surface-container-low text-[10px] font-bold text-on-surface-variant rounded-bl-lg border-b border-l border-outline-variant/10">
              {memory.type}
            </div>
            <div className="flex items-start justify-between mb-4 mt-2">
              <span className={`${memory.bgClass} ${memory.textClass} text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded`}>
                {memory.confidence}% 置信度
              </span>
              <div className="flex items-center gap-1 text-on-surface-variant">
                {memory.hasStar && (
                  <span className="material-symbols-outlined text-sm filled">star</span>
                )}
                <span className="text-xs font-semibold">{memory.priority}</span>
              </div>
            </div>
            
            <p className="text-on-background leading-relaxed mb-6 font-medium">
              {memory.content}
            </p>
            
            <div className="mt-auto pt-4 border-t border-outline-variant/10 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="w-8 h-1.5 bg-surface-container rounded-full overflow-hidden">
                  <div 
                    className={`${memory.colorClass} h-full`} 
                    style={{ width: `${memory.confidence}%` }}
                  ></div>
                </div>
                <span className="text-[10px] font-bold text-on-surface-variant">
                  {memory.confidence}% 置信度
                </span>
              </div>
              <button 
                onClick={() => setMemories(memories.filter(m => m.id !== memory.id))}
                className="text-on-surface-variant/40 hover:text-error transition-colors"
                title="删除记忆"
              >
                <span className="material-symbols-outlined text-sm">delete</span>
              </button>
            </div>
          </div>
        ))}

        {/* Add Memory Card */}
        <button 
          onClick={() => setIsModalOpen(true)}
          className="bg-primary/5 border-2 border-dashed border-primary/20 p-6 rounded-xl flex flex-col items-center justify-center text-center group hover:bg-primary/10 transition-colors min-h-[200px]"
        >
          <span className="material-symbols-outlined text-3xl text-primary mb-3">add_circle</span>
          <h3 className="font-headline font-bold text-primary mb-1">手动添加记忆</h3>
          <p className="text-xs text-on-surface-variant px-6">直接影响 AI 的永久知识库。</p>
        </button>
      </div>

      {/* Add Memory Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm p-4">
          <div className="bg-surface-container-lowest rounded-2xl shadow-2xl border border-outline-variant/20 w-full max-w-lg overflow-hidden">
            <div className="p-6 border-b border-outline-variant/10 flex items-center justify-between">
              <h3 className="font-headline font-bold text-xl text-on-surface">添加新记忆</h3>
              <button 
                onClick={() => setIsModalOpen(false)}
                className="text-on-surface-variant hover:text-on-surface transition-colors"
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-semibold text-on-surface-variant mb-2">记忆内容</label>
                <textarea
                  value={newMemoryContent}
                  onChange={(e) => setNewMemoryContent(e.target.value)}
                  className="w-full bg-surface-container-low border border-outline-variant/20 rounded-xl p-3 text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 resize-none h-32"
                  placeholder="例如：在所有前端代码中优先使用 Tailwind CSS 进行样式设计..."
                  autoFocus
                ></textarea>
              </div>
              <div>
                <label className="block text-sm font-semibold text-on-surface-variant mb-2">作用域</label>
                <div className="flex gap-3">
                  {(['全局', '项目', '个人'] as const).map((type) => (
                    <button
                      key={type}
                      onClick={() => setNewMemoryType(type)}
                      className={`flex-1 py-2 rounded-lg text-sm font-medium transition-colors border ${
                        newMemoryType === type
                          ? 'bg-primary-container text-on-primary-container border-primary/30'
                          : 'bg-surface-container-low text-on-surface-variant border-outline-variant/20 hover:bg-surface-container'
                      }`}
                    >
                      {type}
                    </button>
                  ))}
                </div>
              </div>
            </div>
            <div className="p-6 border-t border-outline-variant/10 bg-surface-container-low flex justify-end gap-3">
              <button 
                onClick={() => setIsModalOpen(false)}
                className="px-4 py-2 rounded-lg text-sm font-medium text-on-surface-variant hover:bg-surface-container transition-colors"
              >
                取消
              </button>
              <button 
                onClick={handleAddMemory}
                disabled={!newMemoryContent.trim()}
                className="px-4 py-2 rounded-lg text-sm font-medium bg-primary text-on-primary hover:bg-primary-dim transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                保存记忆
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
