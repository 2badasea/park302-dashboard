import { gFetch } from '../utils/gFetch'

const BASE = '/api/agents'

// -------------------------------------------------------------------------
// Request / Response 타입
// -------------------------------------------------------------------------

export interface AgentListRequest {
  page: number
  perPage: number
  searchType: string
  keyword: string
}

/** 목록 항목 — TUI Grid 행 데이터로 사용 */
export interface AgentListItem {
  id: number
  name: string
  clientCode: string | null
  address: string | null
  contactTel: string | null
  contactEmail: string | null
}

/** 상세 조회 응답 — 수정 모달 폼 초기화에 사용 */
export interface AgentDetail {
  id: number
  name: string
  clientCode: string | null
  businessNumber: string | null
  contactTel: string | null
  contactEmail: string | null
  address: string | null
  memo: string | null
  createdAt: string
  updatedAt: string
}

export interface AgentSaveRequest {
  name: string
  clientCode?: string
  businessNumber?: string
  contactTel?: string
  contactEmail?: string
  address?: string
  memo?: string
}

// -------------------------------------------------------------------------
// API 호출 함수
// -------------------------------------------------------------------------

export const agentService = {
  /** 목록 조회 (서버사이드 페이지네이션) */
  getList: (params: AgentListRequest) => {
    const qs = new URLSearchParams({
      page: String(params.page),
      perPage: String(params.perPage),
      searchType: params.searchType,
      keyword: params.keyword,
    })
    return gFetch(`${BASE}?${qs}`)
  },

  /** 상세 조회 (수정 모달 진입 시) */
  getDetail: (id: number) => gFetch(`${BASE}/${id}`),

  /** 등록 */
  create: (data: AgentSaveRequest) =>
    gFetch(BASE, { method: 'POST', body: JSON.stringify(data) }),

  /** 수정 */
  update: (id: number, data: AgentSaveRequest) =>
    gFetch(`${BASE}/${id}`, { method: 'PATCH', body: JSON.stringify(data) }),

  /** 삭제 (soft delete, 복수) */
  delete: (ids: number[]) =>
    gFetch(BASE, { method: 'DELETE', body: JSON.stringify({ ids }) }),
}
