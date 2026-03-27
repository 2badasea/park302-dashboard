import { useEffect, useRef, useState } from 'react'
import Grid from 'tui-grid'
import 'tui-grid/dist/tui-grid.css'
import {
  agentService,
  AgentDetail,
  AgentManagerItem,
  AgentSaveRequest,
} from '../../services/agentService'
import {
  gAlert,
  gCloseLoading,
  gConfirm,
  gLoading,
  gToast,
  formatBusinessNumber,
  formatTel,
  checkRequired,
} from '../../utils/common'
import AppModal from '../../components/common/AppModal'

interface AgentModalProps {
  /** undefined = 등록 모드, number = 수정 모드 */
  agentId?: number
  onClose: () => void
  /** 저장 성공 후 목록 갱신을 위해 부모에서 콜백 */
  onSaved: () => void
}

const EMPTY_FORM: AgentSaveRequest = {
  name: '',
  clientCode: '',
  businessNumber: '',
  contactTel: '',
  contactEmail: '',
  address: '',
  memo: '',
}

/**
 * 업체 등록 / 수정 모달
 * agentId가 있으면 수정 모드 — 마운트 시 상세 API + 담당자 목록 API를 호출해 폼을 채운다.
 *
 * 담당자 그리드는 tui-grid 코어를 직접 사용한다.
 * ToastGridWrapper는 조회 전용 그리드에 적합하며, 인라인 편집·행 추가/삭제 같은
 * 명령형 제어가 필요한 이 케이스에서는 그리드 인스턴스를 직접 관리한다.
 */
function AgentModal({ agentId, onClose, onSaved }: AgentModalProps) {
  const isEdit = agentId !== undefined
  const [form, setForm] = useState<AgentSaveRequest>(EMPTY_FORM)
  const [loading, setLoading] = useState(false)

  // 삭제된 기존 담당자 ID 목록 (저장 시 soft delete 처리)
  const [deletedManagerIds, setDeletedManagerIds] = useState<number[]>([])

  // 담당자 tui-grid 인스턴스 및 DOM 컨테이너 ref
  const managerGridContainerRef = useRef<HTMLDivElement>(null)
  const managerGridRef = useRef<Grid | null>(null)

  // 담당자 그리드 초기화 — 마운트 시 1회 실행
  useEffect(() => {
    if (!managerGridContainerRef.current) return

    managerGridRef.current = new Grid({
      el: managerGridContainerRef.current,
      bodyHeight: 180,
      rowHeight: 36,
      scrollX: false,
      // 체크박스는 rowHeaders로 추가 (tui-grid 내장 방식)
      rowHeaders: ['checkbox'],
      columns: [
        { name: 'name',       header: '이름 *', width: 130, editor: 'text' },
        { name: 'department', header: '부서',    width: 120, editor: 'text' },
        { name: 'position',   header: '직책',    width: 100, editor: 'text' },
        // 이메일은 width 미지정 — 남은 공간을 자동으로 채움
        { name: 'email',      header: '이메일',  minWidth: 160, editor: 'text' },
        { name: 'tel',        header: '연락처',  width: 150, editor: 'text' },
      ],
    })

    return () => {
      managerGridRef.current?.destroy()
      managerGridRef.current = null
    }
  }, [])

  // 수정 모드: 마운트 시 업체 상세 + 담당자 목록 병렬 조회
  useEffect(() => {
    if (!isEdit) return

    setLoading(true)
    Promise.all([
      agentService.getDetail(agentId),
      agentService.getManagers(agentId),
    ])
      .then(([agentRes, managersRes]) => {
        const d = (agentRes as { data: AgentDetail }).data
        setForm({
          name: d.name ?? '',
          clientCode: d.clientCode ?? '',
          businessNumber: d.businessNumber ?? '',
          contactTel: d.contactTel ?? '',
          contactEmail: d.contactEmail ?? '',
          address: d.address ?? '',
          memo: d.memo ?? '',
        })
        // 담당자 데이터를 그리드에 주입
        // id 필드는 columns에 없어도 tui-grid 내부에 보존되어 getData()로 회수 가능
        if (managerGridRef.current) {
          // tui-grid resetData는 내부 OptRow[] 타입을 요구하므로 any[] 캐스팅
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          managerGridRef.current.resetData((managersRes as { data: any[] }).data)
        }
      })
      .catch(() => {
        gAlert('조회 실패', '업체 정보를 불러오는 데 실패했습니다.')
        onClose()
      })
      .finally(() => setLoading(false))
  }, [agentId, isEdit, onClose])

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target
    // 사업자번호·연락처는 포맷팅 함수를 거쳐 숫자+하이픈 형태로 저장
    let formatted = value
    if (name === 'businessNumber') formatted = formatBusinessNumber(value)
    else if (name === 'contactTel') formatted = formatTel(value)
    setForm((prev) => ({ ...prev, [name]: formatted }))
  }

  /** 행추가: 빈 편집 가능 행을 그리드 맨 아래에 추가 */
  const handleManagerAdd = () => {
    managerGridRef.current?.appendRow({
      name: '',
      department: '',
      position: '',
      email: '',
      tel: '',
    })
  }

  /**
   * 행삭제: 체크된 행 제거
   * 기존 DB 담당자(id 있음)가 포함된 경우 deletedManagerIds에 누적하고 gToast로 안내
   * 신규 추가 행(id 없음)은 조용히 제거
   */
  const handleManagerDelete = () => {
    const grid = managerGridRef.current
    if (!grid) return

    const checked = grid.getCheckedRows()
    if (checked.length === 0) {
      gToast('삭제할 행을 선택해 주세요.', 'warning')
      return
    }

    // 기존 DB 담당자 ID 수집 (id 필드가 있는 행만)
    const existingIds = checked
      .filter((r) => r.id != null)
      .map((r) => Number(r.id))

    // 그리드에서 선택 행 제거
    checked.forEach((r) => grid.removeRow(r.rowKey as number))

    // 기존 데이터가 한 개라도 있으면 저장 대기 목록에 추가 후 안내
    if (existingIds.length > 0) {
      setDeletedManagerIds((prev) => [...prev, ...existingIds])
      gToast('삭제된 담당자는 저장 시 반영됩니다.', 'info')
    }
  }

  const handleSubmit = async () => {
    // 업체명 필수 검증
    const formErr = checkRequired([{ value: form.name, label: '업체명' }])
    if (formErr) { gToast(formErr, 'warning'); return }

    // 담당자 그리드 데이터 수집 및 이름 필수 검증
    // (검증 먼저 완료 후 confirm을 띄워 불필요한 다이얼로그 방지)
    // tui-grid getData() 반환 타입과 AgentManagerItem 간 타입 불일치 — unknown 경유 캐스팅
    const managers = (managerGridRef.current?.getData() ?? []) as unknown as AgentManagerItem[]
    const hasEmptyName = managers.some((m) => !String(m.name ?? '').trim())
    if (hasEmptyName) {
      gToast('담당자 이름을 입력해 주세요.', 'warning')
      return
    }

    // 검증 통과 후 저장 확인
    const confirmed = await gConfirm(
      isEdit ? '업체 정보를 수정하시겠습니까?' : '업체를 등록하시겠습니까?'
    )
    if (!confirmed) return

    gLoading()
    try {
      // 업체 저장 (등록 또는 수정) — 응답에서 agentId 확보
      let savedAgentId: number
      if (isEdit) {
        await agentService.update(agentId, form)
        savedAgentId = agentId
      } else {
        const res = await agentService.create(form) as { data: AgentDetail }
        savedAgentId = res.data.id
      }

      // 담당자 일괄 저장 (신규/수정 upsert + soft delete)
      await agentService.saveManagers(savedAgentId, {
        managers,
        deleteIds: deletedManagerIds,
      })

      gCloseLoading()
      onSaved()
    } catch (err: unknown) {
      gCloseLoading()
      const msg =
        (err as { data?: { msg?: string } })?.data?.msg ?? '저장 중 오류가 발생했습니다.'
      gAlert('저장 실패', msg)
    }
  }

  return (
    <AppModal
      title={isEdit ? '업체 수정' : '업체 등록'}
      size="xl"
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            취소
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={handleSubmit}
            disabled={loading}
          >
            {isEdit ? '수정' : '등록'}
          </button>
        </>
      }
    >
      {/* 로딩 스피너: 폼과 별도 렌더링.
          loading 조건으로 폼 전체를 언마운트하면 managerGridContainerRef div도 사라져
          TUI Grid 인스턴스가 null 요소를 참조하며 clientHeight 오류가 발생한다.
          display:none으로 숨기는 방식을 사용해 DOM을 항상 유지한다. */}
      {loading && (
        <div className="text-center py-4">
          <div className="spinner-border spinner-border-sm" role="status" />
          <span className="ms-2">불러오는 중...</span>
        </div>
      )}
      <div style={{ display: loading ? 'none' : undefined }} className="row g-3">
          {/* 업체명 (필수) */}
          <div className="col-md-6">
            <label className="form-label">
              업체명 <span className="text-danger">*</span>
            </label>
            <input
              type="text"
              className="form-control"
              name="name"
              value={form.name}
              onChange={handleChange}
              placeholder="업체명 입력"
              maxLength={100}
            />
          </div>

          {/* Client Code */}
          <div className="col-md-6">
            <label className="form-label">Client Code</label>
            <input
              type="text"
              className="form-control"
              name="clientCode"
              value={form.clientCode ?? ''}
              onChange={handleChange}
              placeholder="ERP 연동 식별 코드 (예: cali-dev)"
              maxLength={50}
            />
          </div>

          {/* 사업자등록번호 — agentNum 클래스로 CSS 구분, 포맷팅은 handleChange에서 처리 */}
          <div className="col-md-6">
            <label className="form-label">사업자등록번호</label>
            <input
              type="text"
              className="form-control agentNum"
              name="businessNumber"
              value={form.businessNumber ?? ''}
              onChange={handleChange}
              placeholder="000-00-00000"
              maxLength={12}
            />
          </div>

          {/* 연락처 — 포맷팅은 handleChange에서 처리 */}
          <div className="col-md-6">
            <label className="form-label">연락처</label>
            <input
              type="text"
              className="form-control"
              name="contactTel"
              value={form.contactTel ?? ''}
              onChange={handleChange}
              placeholder="000-0000-0000"
              maxLength={13}
            />
          </div>

          {/* 이메일 */}
          <div className="col-md-6">
            <label className="form-label">이메일</label>
            <input
              type="email"
              className="form-control"
              name="contactEmail"
              value={form.contactEmail ?? ''}
              onChange={handleChange}
              placeholder="example@domain.com"
              maxLength={100}
            />
          </div>

          {/* 주소 */}
          <div className="col-md-6">
            <label className="form-label">주소</label>
            <input
              type="text"
              className="form-control"
              name="address"
              value={form.address ?? ''}
              onChange={handleChange}
              placeholder="주소 입력"
              maxLength={200}
            />
          </div>

          {/* 업체담당자 그리드 */}
          <div className="col-12">
            <div className="d-flex justify-content-between align-items-center mb-1">
              <label className="form-label mb-0">업체담당자</label>
              <div className="btn-group btn-group-sm">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={handleManagerAdd}
                >
                  행추가
                </button>
                <button
                  type="button"
                  className="btn btn-outline-danger"
                  onClick={handleManagerDelete}
                >
                  행삭제
                </button>
              </div>
            </div>
            <div ref={managerGridContainerRef} />
          </div>

          {/* 메모 */}
          <div className="col-12">
            <label className="form-label">메모 (내부용)</label>
            <textarea
              className="form-control"
              name="memo"
              value={form.memo ?? ''}
              onChange={handleChange}
              placeholder="내부 메모 입력"
              rows={3}
            />
          </div>
        </div>
    </AppModal>
  )
}

export default AgentModal
