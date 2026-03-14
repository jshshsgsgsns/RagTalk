import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

export const THEMES = [
  {
    name: '粉蓝色',
    color: '#89CFF0',
    vars: {
      '--theme-primary': '#89CFF0',
      '--theme-on-primary': '#003355',
      '--theme-primary-container': '#D4F0FF',
      '--theme-on-primary-container': '#001F33',
      '--theme-primary-dim': '#5CB3E6',
    }
  },
  {
    name: '樱花粉',
    color: '#FFB6C1',
    vars: {
      '--theme-primary': '#FFB6C1',
      '--theme-on-primary': '#5C1A1A',
      '--theme-primary-container': '#FFE5E5',
      '--theme-on-primary-container': '#330000',
      '--theme-primary-dim': '#FF9999',
    }
  },
  {
    name: '薄荷绿',
    color: '#98FB98',
    vars: {
      '--theme-primary': '#98FB98',
      '--theme-on-primary': '#003300',
      '--theme-primary-container': '#E5FFE5',
      '--theme-on-primary-container': '#001A00',
      '--theme-primary-dim': '#66FF66',
    }
  },
  {
    name: '丁香紫',
    color: '#DDA0DD',
    vars: {
      '--theme-primary': '#DDA0DD',
      '--theme-on-primary': '#330066',
      '--theme-primary-container': '#F5F5FF',
      '--theme-on-primary-container': '#1A0033',
      '--theme-primary-dim': '#CC99CC',
    }
  }
];

export function SettingsPage() {
  const [activeTheme, setActiveTheme] = useState(() => {
    return localStorage.getItem('app-theme-name') || THEMES[0].name;
  });

  useEffect(() => {
    const savedThemeName = localStorage.getItem('app-theme-name');
    if (savedThemeName) {
      const theme = THEMES.find(t => t.name === savedThemeName);
      if (theme) {
        applyTheme(theme);
      }
    }
  }, []);

  const applyTheme = (theme: typeof THEMES[0]) => {
    Object.entries(theme.vars).forEach(([key, value]) => {
      document.documentElement.style.setProperty(key, value);
    });
  };

  const handleThemeChange = (theme: typeof THEMES[0]) => {
    setActiveTheme(theme.name);
    localStorage.setItem('app-theme-name', theme.name);
    applyTheme(theme);
  };

  return (
    <main className="flex-1 flex flex-col min-w-0 relative bg-surface overflow-y-auto">
      <header className="h-16 px-8 flex items-center justify-between sticky top-0 z-10 bg-surface/80 backdrop-blur-md border-b border-outline-variant/10">
        <div className="flex items-center gap-4">
          <h1 className="font-headline font-bold text-xl text-on-background">设置与视图</h1>
        </div>
      </header>

      <div className="max-w-4xl mx-auto w-full p-8">
        <h2 className="text-lg font-bold text-on-background mb-6">系统视图</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Link to="/memory" className="bg-surface-container-lowest p-6 rounded-2xl border border-outline-variant/10 hover:border-primary/30 hover:shadow-md transition-all group flex items-start gap-4">
            <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center shrink-0 group-hover:bg-primary group-hover:text-on-primary transition-colors text-primary">
              <span className="material-symbols-outlined">grid_view</span>
            </div>
            <div>
              <h3 className="font-bold text-on-background mb-1">记忆工作区</h3>
              <p className="text-sm text-on-surface-variant leading-relaxed">查看和管理系统提取的所有记忆片段、偏好和事实。</p>
            </div>
          </Link>

          <Link to="/timeline" className="bg-surface-container-lowest p-6 rounded-2xl border border-outline-variant/10 hover:border-primary/30 hover:shadow-md transition-all group flex items-start gap-4">
            <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center shrink-0 group-hover:bg-primary group-hover:text-on-primary transition-colors text-primary">
              <span className="material-symbols-outlined">timeline</span>
            </div>
            <div>
              <h3 className="font-bold text-on-background mb-1">时间轴视图</h3>
              <p className="text-sm text-on-surface-variant leading-relaxed">按时间顺序查看所有交互事件和记忆的演变过程。</p>
            </div>
          </Link>

          <Link to="/persona" className="bg-surface-container-lowest p-6 rounded-2xl border border-outline-variant/10 hover:border-primary/30 hover:shadow-md transition-all group flex items-start gap-4">
            <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center shrink-0 group-hover:bg-primary group-hover:text-on-primary transition-colors text-primary">
              <span className="material-symbols-outlined">fingerprint</span>
            </div>
            <div>
              <h3 className="font-bold text-on-background mb-1">智能画像</h3>
              <p className="text-sm text-on-surface-variant leading-relaxed">查看系统为您构建的个性化偏好和长期习惯画像。</p>
            </div>
          </Link>

          <Link to="/rag" className="bg-surface-container-lowest p-6 rounded-2xl border border-outline-variant/10 hover:border-primary/30 hover:shadow-md transition-all group flex items-start gap-4">
            <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center shrink-0 group-hover:bg-primary group-hover:text-on-primary transition-colors text-primary">
              <span className="material-symbols-outlined">data_object</span>
            </div>
            <div>
              <h3 className="font-bold text-on-background mb-1">RAG 检索</h3>
              <p className="text-sm text-on-surface-variant leading-relaxed">使用检索增强生成技术，向您的专属知识库提问。</p>
            </div>
          </Link>
        </div>

        <h2 className="text-lg font-bold text-on-background mb-6 mt-12">常规设置</h2>
        <div className="bg-surface-container-lowest rounded-2xl border border-outline-variant/10 overflow-hidden">
          <div className="p-6 border-b border-outline-variant/10 flex items-center justify-between">
            <div>
              <h3 className="font-bold text-on-background mb-1">辅助颜色</h3>
              <p className="text-sm text-on-surface-variant">选择您喜欢的界面辅助颜色</p>
            </div>
            <div className="flex items-center gap-3">
              {THEMES.map((theme) => (
                <button
                  key={theme.name}
                  onClick={() => handleThemeChange(theme)}
                  className={`w-8 h-8 rounded-full border-2 transition-all ${
                    activeTheme === theme.name ? 'border-on-background scale-110' : 'border-transparent hover:scale-105'
                  }`}
                  style={{ backgroundColor: theme.color }}
                  title={theme.name}
                />
              ))}
            </div>
          </div>
          <div className="p-6 border-b border-outline-variant/10 flex items-center justify-between">
            <div>
              <h3 className="font-bold text-on-background mb-1">主题模式</h3>
              <p className="text-sm text-on-surface-variant">切换亮色或暗色主题</p>
            </div>
            <button className="px-4 py-2 bg-surface-container-high rounded-lg text-sm font-medium hover:bg-surface-container-highest transition-colors">
              跟随系统
            </button>
          </div>
          <div className="p-6 flex items-center justify-between">
            <div>
              <h3 className="font-bold text-on-background mb-1">数据导出</h3>
              <p className="text-sm text-on-surface-variant">导出您的所有对话和记忆数据</p>
            </div>
            <button className="px-4 py-2 bg-primary text-on-primary rounded-lg text-sm font-medium hover:bg-primary-dim transition-colors">
              导出 JSON
            </button>
          </div>
        </div>
      </div>
    </main>
  );
}
