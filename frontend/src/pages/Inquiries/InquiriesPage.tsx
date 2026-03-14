// 문의관리 페이지 — 1차: 목록 레이아웃 골격만 구성. 데이터 연동 및 상세 모달은 추후 구현

function InquiriesPage() {
  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">문의관리</h1>
        <button className="btn btn-primary">문의 등록</button>
      </div>
      <div className="card">
        <div className="card-body">
          {/* TODO: ToastGridWrapper로 문의 목록 그리드 구현 */}
          <p className="text-muted">문의 목록이 표시됩니다.</p>
        </div>
      </div>
    </div>
  )
}

export default InquiriesPage
