// 공지관리 페이지 — 1차: 목록 레이아웃 골격만 구성. 등록/수정 모달(에디터 포함)은 추후 구현

function NoticesPage() {
  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">공지관리</h1>
        <button className="btn btn-primary">공지 등록</button>
      </div>
      <div className="card">
        <div className="card-body">
          {/* TODO: ToastGridWrapper로 공지 목록 그리드 구현 */}
          {/* TODO: 등록/수정 모달에 Toast UI Editor 적용 */}
          <p className="text-muted">공지 목록이 표시됩니다.</p>
        </div>
      </div>
    </div>
  )
}

export default NoticesPage
