import { useCallback, useEffect, useRef, useState } from 'react'
import type { ColumnOptions, Row } from 'tui-grid'
import ToastGridWrapper, { ToastGridHandle } from '../../lib/ToastGridWrapper'
import { agentService, AgentListItem } from '../../services/agentService'
import { gAlert, gCloseLoading, gConfirm, gLoading, gSuccess } from '../../utils/gUI'
import AgentModal from './AgentModal'

// -------------------------------------------------------------------------
// 상수
// -------------------------------------------------------------------------

const PER_PAGE = 20

const SEARCH_TYPES = [
  { value: 'ALL', label: '전체' },
  { value: 'NAME', label: '업체명' },
  { value: 'CLIENT_CODE', label: 'Client Code' },
  { value: 'ADDRESS', label: '주소' },
  { value: 'TEL', label: '연락처' },
  { value: 'EMAIL', label: '이메일' },
]

const COLUMNS: ColumnOptions[] = [
  { name: 'name', header: '업체명', minWidth: 150, sortable: true },
  { name: 'clientCode', header: 'Client Code', width: 150 },
  { name: 'address', header: '주소', minWidth: 200 },
  { name: 'contactTel', header: '연락처', width: 140 },
  { name: 'contactEmail', header: '이메일', minWidth: 180 },
]

// -------------------------------------------------------------------------
// 컴포넌트
// -------------------------------------------------------------------------

function AgentsPage() {
  // 목록 데이터
  const [data, setData] = useState<AgentListItem[]>([])
  const [totalCount, setTotalCount] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [currentPage, setCurrentPage] = useState(1)

  // 검색 상태
  const [searchType, setSearchType] = useState('ALL')
  const [keyword, setKeyword] = useState('')

  // 모달 상태: agentId가 없으면 등록, 있으면 수정
  const [modalState, setModalState] = useState<{ open: boolean; agentId?: number }>({
    open: false,
  })

  // TUI Grid handle — 체크된 행 조회(삭제용)에 사용
  const gridRef = useRef<ToastGridHandle>(null)

  // -------------------------------------------------------------------------
  // 데이터 조회
  // -------------------------------------------------------------------------

  const fetchList = useCallback(async (page: number, type: string, kw: string) => {
    try {
      const res = await agentService.getList({ page, perPage: PER_PAGE, searchType: type, keyword: kw })
      setData(res.data.content)
      setTotalCount(res.data.totalCount)
      setTotalPages(res.data.totalPages)
      setCurrentPage(page)
    } catch {
      gAlert('조회 실패', '업체 목록을 불러오는 데 실패했습니다.')
    }
  }, [])

  useEffect(() => {
    fetchList(1, 'ALL', '')
  }, [fetchList])

  // -------------------------------------------------------------------------
  // 이벤트 핸들러
  // -------------------------------------------------------------------------

  const handleSearch = () => {
    fetchList(1, searchType, keyword)
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') handleSearch()
  }

  /** 행 클릭 시 수정 모달 오픈 */
  const handleRowClick = (rowData: Row) => {
    setModalState({ open: true, agentId: rowData.id as number })
  }

  /** 체크된 행을 soft delete 처리 */
  const handleDelete = async () => {
    const checkedRows = gridRef.current?.getCheckedRows() ?? []
    if (checkedRows.length === 0) {
      gAlert('선택된 업체 없음', '삭제할 업체를 선택해 주세요.', 'warning')
      return
    }

    const confirmed = await gConfirm(
      `${checkedRows.length}개 업체를 삭제하시겠습니까?`,
      '삭제된 업체는 목록에서 숨김 처리됩니다.'
    )
    if (!confirmed) return

    const ids = checkedRows.map((row) => row.id as number)
    gLoading()
    try {
      await agentService.delete(ids)
      gCloseLoading()
      gSuccess('삭제 완료')
      fetchList(currentPage, searchType, keyword)
    } catch {
      gCloseLoading()
      gAlert('삭제 실패', '삭제 중 오류가 발생했습니다.')
    }
  }

  const handleModalClose = () => setModalState({ open: false })

  /** 저장 성공: 모달 닫고 목록 갱신 */
  const handleModalSaved = () => {
    setModalState({ open: false })
    gSuccess(modalState.agentId ? '업체 정보가 수정되었습니다.' : '업체가 등록되었습니다.')
    fetchList(currentPage, searchType, keyword)
  }

  const handlePageChange = (page: number) => {
    fetchList(page, searchType, keyword)
  }

  // -------------------------------------------------------------------------
  // 렌더링
  // -------------------------------------------------------------------------

  return (
    <div>
      {/* 페이지 헤더 */}
      <div className="page-header">
        <h1 className="page-title">업체관리</h1>
      </div>

      <div className="card">
        <div className="card-body">
          {/* 그리드 툴바 */}
          <div className="d-flex justify-content-between align-items-center mb-2">
            {/* 좌측: 삭제 / 등록 */}
            <div className="d-flex gap-2">
              <button className="btn btn-danger btn-sm" onClick={handleDelete}>
                삭제
              </button>
              <button
                className="btn btn-primary btn-sm"
                onClick={() => setModalState({ open: true })}
              >
                등록
              </button>
            </div>

            {/* 우측: 검색 */}
            <div className="d-flex gap-2 align-items-center">
              <select
                className="form-select form-select-sm"
                style={{ width: '120px' }}
                value={searchType}
                onChange={(e) => setSearchType(e.target.value)}
              >
                {SEARCH_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
              <input
                type="text"
                className="form-control form-control-sm"
                style={{ width: '200px' }}
                placeholder="검색어 입력"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                onKeyDown={handleKeyDown}
              />
              <button className="btn btn-outline-secondary btn-sm" onClick={handleSearch}>
                검색
              </button>
            </div>
          </div>

          {/* TUI Grid */}
          <ToastGridWrapper
            ref={gridRef}
            columns={COLUMNS}
            data={data as Row[]}
            onRowClick={handleRowClick}
            options={{
              rowHeaders: ['checkbox'],
              bodyHeight: 500,
            }}
          />

          {/* 페이지네이션 + 총 건수 */}
          <div className="d-flex justify-content-between align-items-center mt-3">
            <small className="text-muted">총 {totalCount.toLocaleString()}건</small>

            {totalPages > 1 && (
              <nav>
                <ul className="pagination pagination-sm mb-0">
                  {/* 이전 */}
                  <li className={`page-item ${currentPage === 1 ? 'disabled' : ''}`}>
                    <button
                      className="page-link"
                      onClick={() => handlePageChange(currentPage - 1)}
                      disabled={currentPage === 1}
                    >
                      &laquo;
                    </button>
                  </li>

                  {/* 페이지 번호 (최대 5개 표시) */}
                  {Array.from({ length: totalPages }, (_, i) => i + 1)
                    .filter(
                      (p) =>
                        p === 1 ||
                        p === totalPages ||
                        Math.abs(p - currentPage) <= 2
                    )
                    .map((p) => (
                      <li
                        key={p}
                        className={`page-item ${p === currentPage ? 'active' : ''}`}
                      >
                        <button className="page-link" onClick={() => handlePageChange(p)}>
                          {p}
                        </button>
                      </li>
                    ))}

                  {/* 다음 */}
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

      {/* 등록 / 수정 모달 */}
      {modalState.open && (
        <AgentModal
          agentId={modalState.agentId}
          onClose={handleModalClose}
          onSaved={handleModalSaved}
        />
      )}
    </div>
  )
}

export default AgentsPage
