import { useEffect, useState } from 'react'
import { agentService, AgentDetail, AgentSaveRequest } from '../../services/agentService'
import { gAlert, gCloseLoading, gLoading, gToast } from '../../utils/gUI'

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
 * agentId가 있으면 수정 모드 — 마운트 시 상세 API를 호출해 폼을 채운다.
 */
function AgentModal({ agentId, onClose, onSaved }: AgentModalProps) {
  const isEdit = agentId !== undefined
  const [form, setForm] = useState<AgentSaveRequest>(EMPTY_FORM)
  const [loading, setLoading] = useState(false)

  // 수정 모드: 마운트 시 상세 데이터 조회 후 폼 초기화
  useEffect(() => {
    if (!isEdit) return

    setLoading(true)
    agentService
      .getDetail(agentId)
      .then((res: { data: AgentDetail }) => {
        const d = res.data
        setForm({
          name: d.name ?? '',
          clientCode: d.clientCode ?? '',
          businessNumber: d.businessNumber ?? '',
          contactTel: d.contactTel ?? '',
          contactEmail: d.contactEmail ?? '',
          address: d.address ?? '',
          memo: d.memo ?? '',
        })
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
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async () => {
    // 필수값 검증
    if (!form.name.trim()) {
      gToast('업체명을 입력해 주세요.', 'warning')
      return
    }

    gLoading()
    try {
      if (isEdit) {
        await agentService.update(agentId, form)
      } else {
        await agentService.create(form)
      }
      gCloseLoading()
      onSaved()
    } catch (err: unknown) {
      gCloseLoading()
      const msg =
        err instanceof Error && (err as { data?: { msg?: string } }).data?.msg
          ? (err as { data: { msg: string } }).data.msg
          : '저장 중 오류가 발생했습니다.'
      gAlert('저장 실패', msg)
    }
  }

  return (
    <>
      {/* Backdrop */}
      <div className="modal-backdrop-custom" onClick={onClose} />

      {/* Modal */}
      <div className="modal show d-block" tabIndex={-1} style={{ zIndex: 1050 }}>
        <div className="modal-dialog modal-lg">
          <div className="modal-content">
            {/* Header */}
            <div className="modal-header">
              <h5 className="modal-title">{isEdit ? '업체 수정' : '업체 등록'}</h5>
              <button type="button" className="btn-close" onClick={onClose} />
            </div>

            {/* Body */}
            <div className="modal-body">
              {loading ? (
                <div className="text-center py-4">
                  <div className="spinner-border spinner-border-sm" role="status" />
                  <span className="ms-2">불러오는 중...</span>
                </div>
              ) : (
                <div className="row g-3">
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

                  {/* 사업자등록번호 */}
                  <div className="col-md-6">
                    <label className="form-label">사업자등록번호</label>
                    <input
                      type="text"
                      className="form-control"
                      name="businessNumber"
                      value={form.businessNumber ?? ''}
                      onChange={handleChange}
                      placeholder="000-00-00000"
                      maxLength={20}
                    />
                  </div>

                  {/* 연락처 */}
                  <div className="col-md-6">
                    <label className="form-label">연락처</label>
                    <input
                      type="text"
                      className="form-control"
                      name="contactTel"
                      value={form.contactTel ?? ''}
                      onChange={handleChange}
                      placeholder="000-0000-0000"
                      maxLength={20}
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
              )}
            </div>

            {/* Footer */}
            <div className="modal-footer">
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
            </div>
          </div>
        </div>
      </div>
    </>
  )
}

export default AgentModal
