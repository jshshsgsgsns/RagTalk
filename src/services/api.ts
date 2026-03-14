export interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export class ApiError extends Error {
  status: number;
  code?: number;
  payload?: unknown;

  constructor(message: string, options?: { status?: number; code?: number; payload?: unknown }) {
    super(message);
    this.name = 'ApiError';
    this.status = options?.status ?? 0;
    this.code = options?.code;
    this.payload = options?.payload;
  }
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
    projectMemories: RetrievedMemoryHit[];
    chatMemories: RetrievedMemoryHit[];
    globalMemories: RetrievedMemoryHit[];
  };
}

export interface RetrievedMemoryHit {
  id: number;
  projectId: number;
  chatId?: number | null;
  scopeType: 'global' | 'project' | 'chat';
  memoryType: 'preference' | 'profile' | 'habit' | 'requirement' | 'constraint' | 'decision' | 'summary' | 'fact';
  title?: string | null;
  summary: string;
  detailText?: string | null;
  importance?: number | null;
  confidence?: number | null;
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
  chatId?: number | null;
  sourceMessageEventId?: number | null;
  scopeType: 'global' | 'project' | 'chat';
  memoryType: 'preference' | 'profile' | 'habit' | 'requirement' | 'constraint' | 'decision' | 'summary' | 'fact';
  title?: string | null;
  summary: string;
  detailText?: string | null;
  importance?: number | null;
  confidence?: number | null;
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
  chatId?: number | null;
  sourceId: number;
  title: string;
  summary: string;
  detail?: string | null;
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
  scopeTypes?: Array<'global' | 'project' | 'chat'>;
  queryText: string;
  topK?: number;
}

export interface VectorSearchHit {
  memoryId: number;
  score: number;
  scopeType: 'global' | 'project' | 'chat';
  memoryType: 'preference' | 'profile' | 'habit' | 'requirement' | 'constraint' | 'decision' | 'summary' | 'fact';
  title?: string | null;
  summary: string;
  detailText?: string | null;
}

export interface VectorSearchResponse {
  queryText: string;
  topK: number;
  totalHits: number;
  projectHits: number;
  supplementalHits: number;
  hits: VectorSearchHit[];
}

const BASE_URL = '/api';

function isWrappedResult<T>(value: unknown): value is Result<T> {
  return typeof value === 'object' && value !== null && 'code' in value && 'message' in value && 'data' in value;
}

function buildQueryString(params?: Record<string, unknown>): string {
  if (!params) {
    return '';
  }

  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }

    if (Array.isArray(value)) {
      value.forEach((item) => {
        if (item !== undefined && item !== null && item !== '') {
          searchParams.append(key, String(item));
        }
      });
      return;
    }

    searchParams.append(key, String(value));
  });

  return searchParams.toString();
}

async function readResponseBody<T>(response: Response): Promise<unknown> {
  const contentType = response.headers.get('content-type') ?? '';

  if (contentType.includes('application/json')) {
    return response.json() as Promise<T>;
  }

  const text = await response.text();
  return text ? { message: text } : null;
}

async function fetchApi<T>(url: string, options?: RequestInit): Promise<T> {
  const headers = new Headers(options?.headers);
  const hasBody = options?.body !== undefined && options.body !== null;
  const isFormData = typeof FormData !== 'undefined' && options?.body instanceof FormData;

  if (hasBody && !isFormData && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  let response: Response;

  try {
    response = await fetch(`${BASE_URL}${url}`, {
      ...options,
      headers,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Network request failed';
    throw new ApiError(message, { payload: error });
  }

  const payload = await readResponseBody<Result<T>>(response);

  if (!response.ok) {
    const message =
      (isWrappedResult(payload) && payload.message) ||
      (typeof payload === 'object' && payload !== null && 'message' in payload && typeof payload.message === 'string' && payload.message) ||
      `HTTP ${response.status} ${response.statusText}`.trim();

    throw new ApiError(message, {
      status: response.status,
      code: isWrappedResult(payload) ? payload.code : undefined,
      payload,
    });
  }

  if (isWrappedResult<T>(payload)) {
    if (payload.code !== 200) {
      throw new ApiError(payload.message || 'API Error', {
        status: response.status,
        code: payload.code,
        payload,
      });
    }

    return payload.data;
  }

  return payload as T;
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
      const query = buildQueryString(params);
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
