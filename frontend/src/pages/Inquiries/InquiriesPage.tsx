import { useEffect, useRef, useState } from 'react'
import type { ColumnOptions, Row } from 'tui-grid'
import ToastGridWrapper, { ToastGridHandle } from '../../lib/ToastGridWrapper'

// -------------------------------------------------------------------------
// 상수
// -------------------------------------------------------------------------

const PER_PAGE = 20

const AGENT_OPTIONS = [
  { value: '1', label: '칼리개발' },
  { value: '2', label: '알파테크' },
  { value: '3', label: '베타솔루션' },
]

const CATEGORY_OPTIONS = [
  { value: 'BUG',         label: '버그' },
  { value: 'ERROR',       label: '오류' },
  { value: 'NEW_FEATURE', label: '기능 요청' },
  { value: 'INQUIRY',     label: '문의' },
]

const STATUS_OPTIONS = [
  { value: 'READY',       label: '대기' },
  { value: 'IN_PROGRESS', label: '진행중' },
  { value: 'ON_HOLD',     label: '보류' },
  { value: 'DONE',        label: '완료' },
  { value: 'CANCELLED',   label: '취소' },
]

const DATE_TYPES = [
  { value: 'CREATED_AT',       label: '작성일자' },
  { value: 'EXPECT_START_DAY', label: '예상시작일' },
  { value: 'EXPECT_FINISH_DAY',label: '예상종료일' },
  { value: 'START_DAY',        label: '시작일' },
  { value: 'FINISH_DAY',       label: '종료일' },
]

const SEARCH_TYPES = [
  { value: 'ALL',     label: '전체' },
  { value: 'TITLE',   label: '제목' },
  { value: 'CREATOR', label: '작성자' },
  { value: 'CONTENT', label: '내용' },
]

// 그리드 셀 표시용 한글 레이블 맵 — enum 값을 그대로 노출하지 않도록 formatter에서 사용
const CATEGORY_LABEL: Record<string, string> = {
  BUG: '버그', ERROR: '오류', NEW_FEATURE: '기능 요청', INQUIRY: '문의',
}
const PRIORITY_LABEL: Record<string, string> = {
  NORMAL: '일반', EMERGENCY: '긴급',
}
const STATUS_LABEL: Record<string, string> = {
  READY: '대기', IN_PROGRESS: '진행중', ON_HOLD: '보류', DONE: '완료', CANCELLED: '취소',
}

// whiteSpace: 'normal' — 제목이 길 경우 잘리지 않고 자동 개행. rowHeight: 'auto'와 함께 동작
const COLUMNS: ColumnOptions[] = [
  { name: 'id',               header: '번호',     width: 65,  align: 'center', sortable: true },
  { name: 'agentName',        header: '업체명',    width: 120, align: 'center' },
  { name: 'category',         header: '유형',      width: 90,  align: 'center',
    formatter: ({ value }) => CATEGORY_LABEL[value as string] ?? String(value) },
  { name: 'title',            header: '제목',      minWidth: 200, align: 'left', whiteSpace: 'normal' },
  { name: 'priorityByAgent',  header: '업체중요도', width: 95,  align: 'center',
    formatter: ({ value }) => PRIORITY_LABEL[value as string] ?? String(value) },
  { name: 'priorityByDev',    header: '내부중요도', width: 95,  align: 'center',
    formatter: ({ value }) => PRIORITY_LABEL[value as string] ?? String(value) },
  { name: 'workStatus',       header: '상태',      width: 90,  align: 'center',
    formatter: ({ value }) => STATUS_LABEL[value as string] ?? String(value) },
  { name: 'createMemberName', header: '작성자',    width: 85,  align: 'center' },
  // 날짜 컬럼: yyyy-mm-dd 최대 10자 기준으로 잘리지 않을 width 설정
  { name: 'createdAt',        header: '작성일자',  width: 105, align: 'center' },
  { name: 'expectStartDay',   header: '예상시작일', width: 105, align: 'center' },
  { name: 'expectFinishDay',  header: '예상종료일', width: 105, align: 'center' },
  { name: 'startDay',         header: '시작일',    width: 105, align: 'center' },
  { name: 'finishDay',        header: '종료일',    width: 105, align: 'center' },
]

// -------------------------------------------------------------------------
// 더미 데이터 (API 연동 전 UI 확인용)
// -------------------------------------------------------------------------

const DUMMY_DATA: Row[] = [
  {
    id: 1, agentName: '칼리개발', category: 'BUG',
    title: '로그인 화면에서 오류가 발생합니다. 특정 브라우저 환경에서만 반복적으로 나타나는 현상입니다.',
    priorityByAgent: 'EMERGENCY', priorityByDev: 'NORMAL', workStatus: 'READY',
    createMemberName: '홍길동', createdAt: '2026-03-20',
    expectStartDay: '2026-03-25', expectFinishDay: '2026-03-31', startDay: '', finishDay: '',
  },
  {
    id: 2, agentName: '알파테크', category: 'INQUIRY',
    title: '정산 내역 조회 기능 문의',
    priorityByAgent: 'NORMAL', priorityByDev: 'NORMAL', workStatus: 'IN_PROGRESS',
    createMemberName: '김철수', createdAt: '2026-03-18',
    expectStartDay: '2026-03-19', expectFinishDay: '2026-03-22', startDay: '2026-03-19', finishDay: '',
  },
  {
    id: 3, agentName: '칼리개발', category: 'NEW_FEATURE',
    title: '엑셀 다운로드 기능 요청',
    priorityByAgent: 'NORMAL', priorityByDev: 'NORMAL', workStatus: 'ON_HOLD',
    createMemberName: '이영희', createdAt: '2026-03-15',
    expectStartDay: '', expectFinishDay: '', startDay: '', finishDay: '',
  },
  {
    id: 4, agentName: '베타솔루션', category: 'ERROR',
    title: '서버 오류 500 간헐적 발생',
    priorityByAgent: 'EMERGENCY', priorityByDev: 'EMERGENCY', workStatus: 'DONE',
    createMemberName: '박민준', createdAt: '2026-03-10',
    expectStartDay: '2026-03-11', expectFinishDay: '2026-03-13', startDay: '2026-03-11', finishDay: '2026-03-13',
  },
  {
    id: 5, agentName: '알파테크', category: 'BUG',
    title: '날짜 필터 적용 시 데이터 누락 현상',
    priorityByAgent: 'NORMAL', priorityByDev: 'NORMAL', workStatus: 'CANCELLED',
    createMemberName: '최수진', createdAt: '2026-03-08',
    expectStartDay: '2026-03-09', expectFinishDay: '2026-03-12', startDay: '', finishDay: '',
  },
]

// -------------------------------------------------------------------------
// MultiSelectDropdown 컴포넌트
// -------------------------------------------------------------------------

interface SelectOption {
  value: string
  label: string
}

interface MultiSelectDropdownProps {
  /** 버튼에 표시할 기본 레이블 */
  label: string
  options: SelectOption[]
  selected: string[]
  onChange: (selected: string[]) => void
}

/**
 * 다중 선택 드롭다운 컴포넌트
 * Bootstrap 드롭다운 스타일 + 내부 체크박스 리스트로 구현.
 * 선택 항목이 일부만 있으면 버튼에 "(n)" 카운트를 표시하고, 외부 클릭 시 닫힌다.
 */
function MultiSelectDropdown({ label, options, selected, onChange }: MultiSelectDropdownProps) {
  const [open, setOpen] = useState(false)
  const wrapperRef = useRef<HTMLDivElement>(null)

  // 드롭다운 외부 클릭 감지 → 닫기
  useEffect(() => {
    const handleOutsideClick = (e: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleOutsideClick)
    return () => document.removeEventListener('mousedown', handleOutsideClick)
  }, [])

  // 선택 항목이 없거나 전체인 경우를 '전체' 상태로 간주
  const isAllSelected = selected.length === 0 || selected.length === options.length

  // 전체 토글: 전체 선택 상태이면 전체 해제, 아니면 전체 선택
  const handleAllToggle = () => {
    onChange(isAllSelected ? options.map((o) => o.value) : [])
  }

  const handleOptionToggle = (value: string) => {
    onChange(
      selected.includes(value)
        ? selected.filter((v) => v !== value)
        : [...selected, value]
    )
  }

  // 일부만 선택된 경우 버튼에 "(n)" 표시
  const buttonLabel =
    selected.length > 0 && selected.length < options.length
      ? `${label} (${selected.length})`
      : label

  return (
    <div ref={wrapperRef} className="position-relative">
      <button
        type="button"
        className="btn btn-outline-secondary btn-sm dropdown-toggle"
        onClick={() => setOpen((v) => !v)}
      >
        {buttonLabel}
      </button>
      {open && (
        <ul
          className="dropdown-menu show"
          style={{ minWidth: '130px', padding: '4px 0', zIndex: 1050 }}
        >
          <li>
            <label className="dropdown-item d-flex align-items-center gap-2" style={{ cursor: 'pointer' }}>
              <input type="checkbox" checked={isAllSelected} onChange={handleAllToggle} />
              전체
            </label>
          </li>
          <li><hr className="dropdown-divider my-1" /></li>
          {options.map((opt) => (
            <li key={opt.value}>
              <label className="dropdown-item d-flex align-items-center gap-2" style={{ cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={selected.includes(opt.value)}
                  onChange={() => handleOptionToggle(opt.value)}
                />
                {opt.label}
              </label>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

// -------------------------------------------------------------------------
// InquiriesPage 컴포넌트
// -------------------------------------------------------------------------

function InquiriesPage() {
  // 다중 선택 필터 상태 (빈 배열 = 전체)
  const [selectedAgents,     setSelectedAgents]     = useState<string[]>([])
  const [selectedCategories, setSelectedCategories] = useState<string[]>([])
  const [selectedStatuses,   setSelectedStatuses]   = useState<string[]>([])

  // 날짜 필터 상태
  const [dateType, setDateType] = useState('CREATED_AT')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo,   setDateTo]   = useState('')

  // 검색 상태
  const [searchType, setSearchType] = useState('ALL')
  const [keyword,    setKeyword]    = useState('')

  // 페이지네이션 (더미 데이터 기준)
  const [currentPage, setCurrentPage] = useState(1)
  const totalCount = DUMMY_DATA.length
  const totalPages = Math.max(1, Math.ceil(totalCount / PER_PAGE))

  const gridRef = useRef<ToastGridHandle>(null)

  const handleSearch = () => {
    // TODO: API 연동 시 fetchList(1, filters) 호출
    setCurrentPage(1)
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') handleSearch()
  }

  /** 행 클릭 시 상세 모달 오픈 (TODO: InquiryModal 구현 후 연결) */
  const handleRowClick = (rowData: Row) => {
    console.log('문의 상세 오픈:', rowData.id)
  }

  const handlePageChange = (page: number) => {
    // TODO: API 연동 시 fetchList(page, filters) 호출
    setCurrentPage(page)
  }

  return (
    <div>
      {/* 페이지 헤더 */}
      <div className="page-header">
        <h1 className="page-title">문의관리</h1>
      </div>

      <div className="card">
        <div className="card-body">

          {/* 필터 영역: 그리드 위, 우측 정렬 */}
          <div className="d-flex flex-wrap justify-content-end align-items-center gap-2 mb-2">

            {/* 업체 / 유형 / 상태 다중 선택 */}
            <MultiSelectDropdown
              label="업체"
              options={AGENT_OPTIONS}
              selected={selectedAgents}
              onChange={setSelectedAgents}
            />
            <MultiSelectDropdown
              label="유형"
              options={CATEGORY_OPTIONS}
              selected={selectedCategories}
              onChange={setSelectedCategories}
            />
            <MultiSelectDropdown
              label="상태"
              options={STATUS_OPTIONS}
              selected={selectedStatuses}
              onChange={setSelectedStatuses}
            />

            {/* 날짜 필터: 날짜 타입 선택 + 기간 범위 */}
            <select
              className="form-select form-select-sm"
              style={{ width: '115px' }}
              value={dateType}
              onChange={(e) => setDateType(e.target.value)}
            >
              {DATE_TYPES.map((t) => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
            <input
              type="date"
              className="form-control form-control-sm"
              style={{ width: '135px' }}
              value={dateFrom}
              onChange={(e) => setDateFrom(e.target.value)}
            />
            <span className="text-muted" style={{ fontSize: '0.8rem' }}>~</span>
            <input
              type="date"
              className="form-control form-control-sm"
              style={{ width: '135px' }}
              value={dateTo}
              onChange={(e) => setDateTo(e.target.value)}
            />

            {/* 검색 타입 + 키워드 */}
            <select
              className="form-select form-select-sm"
              style={{ width: '100px' }}
              value={searchType}
              onChange={(e) => setSearchType(e.target.value)}
            >
              {SEARCH_TYPES.map((t) => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
            <input
              type="text"
              className="form-control form-control-sm"
              style={{ width: '180px' }}
              placeholder="검색어 입력"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={handleKeyDown}
            />
            <button className="btn btn-outline-secondary btn-sm" onClick={handleSearch}>
              검색
            </button>
          </div>

          {/* TUI Grid: rowHeight auto로 제목 개행에 따라 행 높이 자동 확장 */}
          <ToastGridWrapper
            ref={gridRef}
            columns={COLUMNS}
            data={DUMMY_DATA}
            onRowClick={handleRowClick}
            options={{
              rowHeight: 'auto' as unknown as number,
              minRowHeight: 40,
              bodyHeight: 500,
            }}
          />

          {/* 페이지네이션 + 총 건수 */}
          <div className="d-flex justify-content-between align-items-center mt-3">
            <small className="text-muted">총 {totalCount.toLocaleString()}건</small>

            {totalPages > 1 && (
              <nav>
                <ul className="pagination pagination-sm mb-0">
                  <li className={`page-item ${currentPage === 1 ? 'disabled' : ''}`}>
                    <button
                      className="page-link"
                      onClick={() => handlePageChange(currentPage - 1)}
                      disabled={currentPage === 1}
                    >
                      &laquo;
                    </button>
                  </li>

                  {Array.from({ length: totalPages }, (_, i) => i + 1)
                    .filter((p) => p === 1 || p === totalPages || Math.abs(p - currentPage) <= 2)
                    .map((p) => (
                      <li key={p} className={`page-item ${p === currentPage ? 'active' : ''}`}>
                        <button className="page-link" onClick={() => handlePageChange(p)}>
                          {p}
                        </button>
                      </li>
                    ))}

                  <li className={`page-item ${currentPage === totalPages ? 'disabled' : ''}`}>
                    <button
                      className="page-link"
                      onClick={() => handlePageChange(currentPage + 1)}
                      disabled={currentPage === totalPages}
                    >
                      &raquo;
                    </button>
                  </li>
                </ul>
              </nav>
            )}
          </div>

        </div>
      </div>
    </div>
  )
}

export default InquiriesPage
