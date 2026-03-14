import React, { useState, useEffect } from 'react';
import { Composer } from '../components/Composer';
import { api, UserProfile } from '../services/api';

export function PersonaPage() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  
  // Hardcoded for now
  const currentUserId = 1;

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        setLoading(true);
        const data = await api.user.getProfile(currentUserId);
        setProfile(data);
      } catch (error) {
        console.error('Failed to fetch profile:', error);
      } finally {
        setLoading(false);
      }
    };
    
    fetchProfile();
  }, []);

  return (
    <main className="flex-1 overflow-y-auto flex flex-col relative bg-surface">
      <header className="glass-nav sticky top-0 z-50 px-10 py-5 border-b border-outline-variant/10 flex items-center justify-between bg-surface/80 backdrop-blur-md">
        <div className="flex items-center gap-4">
          <span className="material-symbols-outlined text-primary">fingerprint</span>
          <h1 className="font-headline text-lg font-bold text-on-background tracking-tight">当前项目情境</h1>
        </div>
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-4 text-on-surface-variant">
            <span className="material-symbols-outlined cursor-pointer hover:text-primary transition-colors">info</span>
            <span className="material-symbols-outlined cursor-pointer hover:text-primary transition-colors">auto_awesome</span>
            <span className="material-symbols-outlined cursor-pointer hover:text-primary transition-colors">more_vert</span>
          </div>
          <div className="h-8 w-[1px] bg-outline-variant/20"></div>
          <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuBdA-BjgnD0GKskJ4ad-fHvLcB-4jXVw1kHMyzt4PoCIwD9kcCCtXcWSIpVYXJ8rYzDwRv2d7i6oxrQm70ViXOQZq2gwLAx2YMoTi6iSXBlz6dWhJb48Aw4xz-h1-vSLLC4NxnbJwJWK3ByH9Qqa5acFL_kOyNUhaCi8z8IN-yo9tnC3WYpoVu1pHlcR1M8YEI9OQr1MsElVoAA_yXJ-j0_xWigl2AwibR9m_YigPLW8sFJCn5zEmQCxF7VB-vyJdy1T7Lo3guWogfV" className="w-9 h-9 rounded-full border border-outline-variant/20 object-cover" alt="Profile" />
        </div>
      </header>
      <div className="max-w-6xl mx-auto w-full p-12 flex flex-col gap-12 pb-32">
        <section className="flex flex-col md:flex-row items-end justify-between gap-8 border-b border-outline-variant/10 pb-12">
          <div className="space-y-4 max-w-2xl">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary-container/30 text-primary text-[10px] font-bold tracking-[0.2em] uppercase">
              <span className="w-2 h-2 rounded-full bg-primary animate-pulse"></span> 神经核心身份
            </div>
            <h2 className="font-headline text-5xl font-extrabold text-on-background leading-tight">智能画像摘要</h2>
            <p className="font-body text-on-surface-variant leading-relaxed text-lg">这一数字足迹由428个交互点合成。我已经细化了关于您的工作流程、风格细微差别和战略重点的模型，以作为更精确的智能延伸。</p>
          </div>
          <div className="flex flex-col items-end">
            <div className="text-right">
              <span className="block text-[40px] font-headline font-bold text-primary leading-none">89%</span>
              <span className="block text-[10px] font-bold uppercase tracking-widest text-outline-variant mt-1">匹配得分</span>
            </div>
          </div>
        </section>
        
        {loading ? (
          <div className="flex items-center justify-center p-20">
            <div className="flex flex-col items-center gap-4">
              <div className="w-12 h-12 rounded-full border-4 border-primary border-t-transparent animate-spin"></div>
              <p className="text-sm font-bold text-on-surface-variant uppercase tracking-widest">正在加载画像数据...</p>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-12 gap-6">
            {/* Card 1 */}
            <div className="col-span-12 lg:col-span-4 flex flex-col gap-6">
              <div className="bg-surface-container-lowest p-8 rounded-xl border border-outline-variant/10 flex flex-col gap-6 h-full shadow-sm">
                <div className="flex items-center justify-between">
                  <span className="material-symbols-outlined text-primary text-3xl">stylus_note</span>
                  <span className="material-symbols-outlined text-outline-variant/40">north_east</span>
                </div>
                <div>
                  <h3 className="font-headline text-xl font-bold mb-2">偏好风格</h3>
                  <p className="text-sm text-on-surface-variant font-body mb-6">您处理和请求信息的方式。</p>
                </div>
                <div className="flex flex-wrap gap-2 mt-auto">
                  {profile?.preferences?.length ? (
                    profile.preferences.map((pref, idx) => (
                      <span key={idx} className="px-4 py-2 rounded-lg bg-surface-container-high font-label text-xs font-semibold text-primary">{pref}</span>
                    ))
                  ) : (
                    <>
                      <span className="px-4 py-2 rounded-lg bg-surface-container-high font-label text-xs font-semibold text-primary">技术型</span>
                      <span className="px-4 py-2 rounded-lg bg-surface-container-high font-label text-xs font-semibold text-primary">简洁</span>
                      <span className="px-4 py-2 rounded-lg border border-outline-variant/20 font-label text-xs font-medium text-on-surface-variant">分析性</span>
                      <span className="px-4 py-2 rounded-lg border border-outline-variant/20 font-label text-xs font-medium text-on-surface-variant">客观</span>
                    </>
                  )}
                </div>
              </div>
            </div>
            {/* Card 2 */}
            <div className="col-span-12 lg:col-span-8">
              <div className="bg-inverse-surface p-8 rounded-xl flex flex-col h-full text-surface shadow-2xl shadow-primary/5">
                <div className="flex items-center gap-4 mb-8">
                  <div className="w-12 h-12 rounded-full bg-primary/20 flex items-center justify-center">
                    <span className="material-symbols-outlined text-primary-fixed">auto_graph</span>
                  </div>
                  <div>
                    <h3 className="font-headline text-xl font-bold">高频话题 & 习惯</h3>
                    <p className="text-xs text-outline-variant">合成交互集群</p>
                  </div>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                  <div className="space-y-4">
                    <div className="flex items-center justify-between border-b border-surface-container-highest/10 pb-2">
                      <span className="font-label text-sm font-medium">常见项目方向</span>
                    </div>
                    <ul className="text-xs text-outline-variant leading-relaxed list-disc pl-4 space-y-2">
                      {profile?.commonProjectDirections?.length ? (
                        profile.commonProjectDirections.map((dir, idx) => <li key={idx}>{dir}</li>)
                      ) : (
                        <>
                          <li>专注于以人为本的系统、可访问性框架和迭代原型设计工作流程。</li>
                          <li>对定位、叙事驱动的增长和高端品牌识别系统表现出浓厚兴趣。</li>
                        </>
                      )}
                    </ul>
                  </div>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between border-b border-surface-container-highest/10 pb-2">
                      <span className="font-label text-sm font-medium">长期习惯</span>
                    </div>
                    <ul className="text-xs text-outline-variant leading-relaxed list-disc pl-4 space-y-2">
                      {profile?.longTermHabits?.length ? (
                        profile.longTermHabits.map((habit, idx) => <li key={idx}>{habit}</li>)
                      ) : (
                        <li>倾向于在深夜进行深入的技术探讨。</li>
                      )}
                    </ul>
                  </div>
                </div>
                <div className="mt-8 flex gap-3">
                  <div className="flex -space-x-3 overflow-hidden">
                    <div className="inline-block h-8 w-8 rounded-full ring-2 ring-inverse-surface bg-surface-container-highest"></div>
                    <div className="inline-block h-8 w-8 rounded-full ring-2 ring-inverse-surface bg-primary"></div>
                    <div className="inline-block h-8 w-8 rounded-full ring-2 ring-inverse-surface bg-tertiary"></div>
                  </div>
                  <div className="flex flex-col justify-center">
                    <span className="text-[10px] font-bold text-outline-variant uppercase">已与3个工作区共享</span>
                  </div>
                </div>
              </div>
            </div>
            {/* Card 3 */}
            <div className="col-span-12 lg:col-span-7">
              <div className="bg-surface-container-low p-8 rounded-xl border border-outline-variant/10 min-h-[300px] flex flex-col relative overflow-hidden">
                <div className="relative z-10">
                  <h3 className="font-headline text-xl font-bold mb-2">技术偏好</h3>
                  <p className="text-sm text-on-surface-variant font-body mb-8 max-w-sm">我根据您在全局情境中的身份关联的特定技术栈和工具。</p>
                </div>
                <div className="flex-1 relative min-h-[200px]">
                  {profile?.commonTechPreferences?.length ? (
                    <div className="flex flex-wrap gap-4 relative z-10">
                      {profile.commonTechPreferences.map((tech, idx) => (
                        <div key={idx} className="p-4 bg-surface-container-lowest rounded-xl shadow-sm border border-outline-variant/10 flex items-center gap-3">
                          <span className="material-symbols-outlined text-primary">code</span>
                          <span className="font-label text-sm font-semibold">{tech}</span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <>
                      <div className="absolute top-0 left-4 p-4 bg-surface-container-lowest rounded-xl shadow-sm border border-outline-variant/10 flex items-center gap-3">
                        <span className="material-symbols-outlined text-primary">token</span>
                        <span className="font-label text-sm font-semibold">React & Tailwind</span>
                      </div>
                      <div className="absolute bottom-4 right-4 p-4 bg-surface-container-lowest rounded-xl shadow-sm border border-outline-variant/10 flex items-center gap-3">
                        <span className="material-symbols-outlined text-primary">architecture</span>
                        <span className="font-label text-sm font-semibold">Spring Boot</span>
                      </div>
                      <div className="absolute top-1/2 left-1/3 p-4 bg-primary text-on-primary rounded-xl shadow-xl flex items-center gap-3">
                        <span className="material-symbols-outlined">cognition</span>
                        <span className="font-label text-sm font-semibold">SQLite</span>
                      </div>
                    </>
                  )}
                </div>
              </div>
            </div>
            {/* Card 4 */}
            <div className="col-span-12 lg:col-span-5">
              <div className="bg-surface p-1 border-2 border-primary-container rounded-xl h-full">
                <div className="bg-surface-container-lowest rounded-lg p-8 h-full flex flex-col">
                  <div className="mb-6 flex items-center gap-3">
                    <div className="w-2 h-2 rounded-full bg-primary relative">
                      <div className="absolute inset-0 rounded-full bg-primary animate-ping opacity-75"></div>
                    </div>
                    <span className="text-[10px] font-bold uppercase tracking-widest text-primary">实时学习流</span>
                  </div>
                  <h4 className="font-headline text-2xl font-bold mb-6 italic text-on-background">“用户表现出对结构清晰度优于装饰复杂性的明显偏好。”</h4>
                  <div className="space-y-6 mt-auto">
                    <div className="flex items-start gap-4">
                      <span className="material-symbols-outlined text-primary-dim mt-1">history_edu</span>
                      <p className="text-sm leading-relaxed text-on-surface-variant">最近检测到在请求的摘要中，用户的偏好转向了 <span className="font-bold text-on-surface">认知负荷最小化</span>。</p>
                    </div>
                    <button className="w-full py-4 bg-surface-container-high hover:bg-surface-container-highest transition-colors rounded-lg font-label text-sm font-bold text-on-secondary-fixed">查看完整记忆审计</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
      <footer className="fixed bottom-0 left-72 right-0 px-10 pb-8 pt-4 bg-gradient-to-t from-surface via-surface to-transparent pointer-events-none">
        <div className="max-w-4xl mx-auto pointer-events-auto">
          <Composer placeholder="发送消息给 ChatGPT..." />
          <p className="text-[10px] text-center mt-3 text-outline-variant uppercase tracking-widest font-bold">正在根据当前画像视图优化情境</p>
        </div>
      </footer>
    </main>
  );
}
