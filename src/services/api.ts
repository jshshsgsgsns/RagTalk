export interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export interface HealthData {
  status: string;
  application: string;
  timestamp: string;
}

export interface ChatSendRequest {
  projectId: number;
  chatId: number;
  message: string;
}

export interface ChatSendResponse {
  projectId: number;
  chatId: number;
  userMessageId: number;
  assistantMessageId: number;
  assistantReply: string;
  usedContextMessageCount?: number;
  usedHistoryMessageCount?: number;
  usedMemoryCount?: number;
  userSequenceNo: number;
  assistantSequenceNo: number;
  provider: string;
  model: string;
  retrievedMemories?: {
    total: number;
    projectMemories: any[];
    chatMemories: any[];
    globalMemories: any[];
  };
}

export interface MemoryRecordRequest {
  userId: number;
  projectId: number;
  chatId?: number;
  sourceMessageEventId?: number;
  scopeType: 'global' | 'project' | 'chat';
  memoryType: 'preference' | 'profile' | 'habit' | 'requirement' | 'constraint' | 'decision' | 'summary' | 'fact';
  title?: string;
  summary: string;
  detailText?: string;
  tagsJson?: string;
  metadataJson?: string;
  importance?: number;
  confidence?: number;
}

export interface MemoryRecordVO {
  id: number;
  userId: number;
  projectId: number;
  chatId: number;
  sourceMessageEventId: number;
  scopeType: string;
  memoryType: string;
  title: string;
  summary: string;
  detailText: string;
  importance: number;
  confidence: number;
  createdAt: number;
  updatedAt: number;
}

export interface UserProfile {
  userId: number;
  preferences: string[];
  longTermHabits: string[];
  commonProjectDirections: string[];
  commonTechPreferences: string[];
}

export interface TimelineEventVO {
  eventType: string;
  eventTime: number;
  userId: number;
  projectId: number;
  chatId: number;
  sourceId: number;
  title: string;
  summary: string;
  detail: string;
}

export interface TimelineResponse {
  userId: number;
  totalEvents: number;
  events: TimelineEventVO[];
}

export interface ExtractMemoryResponse {
  projectId: number;
  chatId: number;
  scannedMessageCount: number;
  extractedCount: number;
  memories: MemoryRecordVO[];
}

export interface VectorSearchRequest {
  userId: number;
  projectId: number;
  chatId?: number;
  scopeTypes?: string[];
  queryText: string;
  topK?: number;
}

export interface VectorSearchResponse {
  queryText: string;
  topK: number;
  totalHits: number;
  projectHits: number;
  supplementalHits: number;
  hits: any[];
}

const BASE_URL = '/api';

async function fetchApi<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_URL}${url}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  });
  
  const result: Result<T> = await response.json();
  
  if (result.code !== 200) {
    throw new Error(result.message || 'API Error');
  }
  
  return result.data;
}

export const api = {
  health: () => fetchApi<HealthData>('/health'),
  
  chat: {
    send: (data: ChatSendRequest) => fetchApi<ChatSendResponse>('/chat/send', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    ragSend: (data: ChatSendRequest) => fetchApi<ChatSendResponse>('/chat/rag-send', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    extractMemory: (chatId: number, projectId: number) => fetchApi<ExtractMemoryResponse>(`/chat/${chatId}/extract-memory`, {
      method: 'POST',
      body: JSON.stringify({ projectId }),
    }),
  },
  
  memory: {
    create: (data: MemoryRecordRequest) => fetchApi<MemoryRecordVO>('/memory-records', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    get: (memoryId: number) => fetchApi<MemoryRecordVO>(`/memory-records/${memoryId}`),
    list: (params?: Record<string, any>) => {
      const query = new URLSearchParams(params as any).toString();
      return fetchApi<MemoryRecordVO[]>(`/memory-records${query ? `?${query}` : ''}`);
    },
  },
  
  vector: {
    upsert: (memoryId: number) => fetchApi<{ status: string }>(`/vector/memory/upsert/${memoryId}`, {
      method: 'POST',
    }),
    search: (data: VectorSearchRequest) => fetchApi<VectorSearchResponse>('/vector/memory/search', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  },
  
  user: {
    getProfile: (userId: number) => fetchApi<UserProfile>(`/profile/${userId}`),
    getTimeline: (userId: number) => fetchApi<TimelineResponse>(`/timeline/${userId}`),
  },
};
