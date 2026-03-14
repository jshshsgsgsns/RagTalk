import React from 'react';
import { NavLink, Link } from 'react-router-dom';

export function LeftSidebar() {
  const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-3 px-4 py-3 rounded-lg font-medium transition-all ${
      isActive
        ? 'bg-primary/10 text-primary'
        : 'hover:bg-surface-container-high text-on-surface-variant'
    }`;

  return (
    <aside className="w-72 bg-surface-container-low text-on-surface flex flex-col h-screen shrink-0 border-r border-outline-variant/20">
      <div className="p-6 flex-1 overflow-y-auto">
        <div className="flex items-center gap-3 mb-8">
          <div className="w-10 h-10 rounded-lg bg-primary/20 flex items-center justify-center">
            <span className="material-symbols-outlined text-primary">psychology</span>
          </div>
          <div>
            <h2 className="font-headline font-bold text-lg leading-tight">项目切换器</h2>
            <p className="text-xs text-on-surface-variant">当前工作区</p>
          </div>
        </div>

        <Link to="/" className="w-full bg-primary hover:bg-primary-dim text-on-primary py-3 px-4 rounded-xl font-medium flex items-center justify-center gap-2 mb-8 transition-colors shadow-sm">
          <span className="material-symbols-outlined text-xl">add</span>
          新对话
        </Link>

        <div>
          <h3 className="text-xs font-bold text-on-surface-variant/70 uppercase tracking-widest mb-3 px-4">历史记录</h3>
          <nav className="space-y-1">
            <a href="#" className="flex items-center gap-3 px-4 py-3 rounded-lg hover:bg-surface-container-high text-on-surface-variant transition-all">
              <span className="material-symbols-outlined text-sm">chat_bubble</span>
              <span className="truncate text-sm">React 性能优化方案</span>
            </a>
            <a href="#" className="flex items-center gap-3 px-4 py-3 rounded-lg hover:bg-surface-container-high text-on-surface-variant transition-all">
              <span className="material-symbols-outlined text-sm">chat_bubble</span>
              <span className="truncate text-sm">Spring Boot 向量数据库接入</span>
            </a>
            <a href="#" className="flex items-center gap-3 px-4 py-3 rounded-lg hover:bg-surface-container-high text-on-surface-variant transition-all">
              <span className="material-symbols-outlined text-sm">chat_bubble</span>
              <span className="truncate text-sm">RAG 架构设计讨论</span>
            </a>
            <NavLink to="/settings" className={navLinkClass}>
              <span className="material-symbols-outlined">settings</span>
              设置与视图
            </NavLink>
          </nav>
        </div>
      </div>
    </aside>
  );
}
