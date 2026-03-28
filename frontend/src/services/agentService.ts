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
  /** ERP 연동 API 키 (외부 API 인증에 사용) */
  apiKey: string | null
  /** ERP 측 webhook 수신 URL */
  callbackUrl: string | null
  /** ERP 측 webhook 인증 키 */
  callbackKey: string | null
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
  /** ERP 연동 API 키 */
  apiKey?: string
  /** ERP 측 webhook 수신 URL */
  callbackUrl?: string
  /** ERP 측 webhook 인증 키 */
  callbackKey?: string
}

/** 업체담당자 단건 항목 (목록 조회 응답 + 저장 요청에 공용) */
export interface AgentManagerItem {
  id?: number
  name: string
  department: string | null
  position: string | null
  tel: string | null
  email: string | null
}

/** 담당자 일괄 저장 요청 */
export interface AgentManagerSaveRequest {
  /** upsert 대상 (id 있으면 update, id 없으면 insert) */
  managers: AgentManagerItem[]
  /** soft delete 대상 id 목록 */
  deleteIds: number[]
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

  /** 업체담당자 목록 조회 */
  getManagers: (agentId: number) =>
    gFetch(`${BASE}/${agentId}/managers`),

  /** 업체담당자 일괄 저장 (upsert + soft delete) */
  saveManagers: (agentId: number, data: AgentManagerSaveRequest) =>
    gFetch(`${BASE}/${agentId}/managers`, { method: 'PUT', body: JSON.stringify(data) }),
}
