import React from 'react';

interface RightSidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export function RightSidebar({ isOpen, onClose }: RightSidebarProps) {
  if (!isOpen) return null;

  return (
    <aside className="w-80 bg-surface-container-low border-l border-outline-variant/10 h-screen flex flex-col shrink-0">
      <div className="p-6 border-b border-outline-variant/10 flex items-center justify-between">
        <div>
          <h3 className="font-headline font-bold text-lg mb-1">上下文助手</h3>
          <p className="text-xs text-on-surface-variant">知识库洞察</p>
        </div>
        <button 
          onClick={onClose}
          className="p-2 rounded-full hover:bg-surface-container-high transition-colors text-on-surface-variant"
        >
          <span className="material-symbols-outlined text-sm">close</span>
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-6 space-y-6">
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-on-surface-variant uppercase tracking-widest">当前激活层</span>
            <span className="material-symbols-outlined text-sm text-primary">info</span>
          </div>

          <div className="space-y-3">
            {/* Active Layer */}
            <div className="p-4 bg-surface-container-lowest rounded-xl border border-primary/20 flex items-center gap-3">
              <span className="material-symbols-outlined text-primary">psychology</span>
              <div className="flex-1">
                <p className="text-sm font-semibold">记忆</p>
                <p className="text-[10px] text-on-surface-variant">当前会话活跃</p>
              </div>
              <div className="w-2 h-2 rounded-full bg-primary"></div>
            </div>

            {/* Inactive Layer */}
            <div className="p-4 bg-surface-container-lowest rounded-xl border border-outline-variant/10 flex items-center gap-3 opacity-60">
              <span className="material-symbols-outlined text-on-surface-variant">database</span>
              <div className="flex-1">
                <p className="text-sm font-semibold">RAG 检索结果</p>
                <p className="text-[10px] text-on-surface-variant">未关联来源</p>
              </div>
            </div>

            {/* Another Layer */}
            <div className="p-4 bg-surface-container-lowest rounded-xl border border-outline-variant/10 flex items-center gap-3">
              <span className="material-symbols-outlined text-tertiary">summarize</span>
              <div className="flex-1">
                <p className="text-sm font-semibold">摘要</p>
                <p className="text-[10px] text-on-surface-variant">最近 3 条摘要</p>
              </div>
            </div>
          </div>
        </div>

        {/* Insight Box */}
        <div className="p-6 bg-primary-container/30 rounded-2xl border border-primary/10">
          <h4 className="text-sm font-bold text-on-primary-container mb-2">记忆洞察</h4>
          <p className="text-xs leading-relaxed text-on-primary-container/80">
            本周您的记忆使用量增加了 12%。建议检查并归档陈旧的项目特定规则。
          </p>
        </div>
      </div>

      <div className="p-6 border-t border-outline-variant/10">
        <div className="flex items-center gap-2 mb-4">
          <div className="flex -space-x-2">
            <img
              src="https://lh3.googleusercontent.com/aida-public/AB6AXuC2fBbByjfdDwrtpJ44_ClgCqZBTy8z5wpRREopdmVihQm87rEUuAWFrVsnaI_xA7MNLvX0GlKWAEnSBjGwwKH_uoYwxzszbywdv8Ge6IMDrvOa__s0l8M4tmI0EE3-YH2_RDoOwgGopqyaaV98K0k5u0w1igBtgFZaGrtUlN2V64L3duIF0JLMkGhHKyNOjG33TeO5Q0HAHWqTmPil_kg74rDD3PFFI2XLXglNnKu8Fc9NmoOeWZppJOAl9ZR1fTc__-xDOcfa1ZPT"
              alt="Collaborator"
              className="w-6 h-6 rounded-full border-2 border-surface-container-low object-cover"
            />
            <img
              src="https://lh3.googleusercontent.com/aida-public/AB6AXuB27QhPRVDWAi1b9j5z3s4hJ18RmMVmd96jG7Kca4sQRQL8wXWW-2Xh-o2TZZI_r7BvYiiGlCUmwRXGSUQtiUbLXlxzaPwfNtiR0Bl1vSfCMh0AsISfuTGjR5rd5FxXZCb9D7UT01wVfXj7lOYONj4VEHhPP7pRgkvqM9ILAS1bPIi0k6NDVUe69WgPs_X3mBf_DP-eiroCgKF6dfYjwLRd6SX9und3piHUQAbNavVh35J9lZUh-eF_EpH4QYits_D1vj7ds3dGVicG"
              alt="Collaborator"
              className="w-6 h-6 rounded-full border-2 border-surface-container-low object-cover"
            />
            <div className="w-6 h-6 rounded-full bg-surface-container-high border-2 border-surface-container-low flex items-center justify-center text-[8px] font-bold text-on-surface">
              +4
            </div>
          </div>
          <span className="text-[10px] font-semibold text-on-surface-variant">已与工作区共享</span>
        </div>
      </div>
    </aside>
  );
}
