import React from 'react'

/** Bootstrap 5 modal-{size} 클래스에 매핑되는 모달 크기 */
type ModalSize = 'sm' | 'lg' | 'xl' | 'xxl' | 'xxxl' | 'fullscreen'

interface AppModalProps {
  /** 모달 헤더 타이틀 */
  title: string
  /** 모달 크기 (기본: xl). Bootstrap modal-{size} 클래스에 매핑 */
  size?: ModalSize
  /** X 버튼 / 닫기 콜백 */
  onClose: () => void
  /**
   * 하단 푸터 영역. ReactNode로 버튼 등 자유롭게 구성.
   * 전달하지 않으면 푸터 영역 자체가 렌더링되지 않는다.
   */
  footer?: React.ReactNode
  children: React.ReactNode
  /**
   * 백드롭(어두운 배경) 클릭 시 닫힘 허용 여부 (기본: false)
   * false이면 백드롭 클릭으로 닫히지 않는 static 동작
   */
  closeOnBackdrop?: boolean
}

/**
 * 공통 모달 레이아웃 컴포넌트
 * Bootstrap 5 modal 구조를 React state로 제어한다.
 * title, size, footer를 props로 받아 일관된 모달 레이아웃을 제공한다.
 *
 * @example
 * <AppModal
 *   title="업체 등록"
 *   size="xl"
 *   onClose={onClose}
 *   footer={
 *     <>
 *       <button onClick={onClose}>취소</button>
 *       <button onClick={handleSubmit}>등록</button>
 *     </>
 *   }
 * >
 *   {body 내용}
 * </AppModal>
 */
function AppModal({
  title,
  size = 'xl',
  onClose,
  footer,
  children,
  closeOnBackdrop = false,
}: AppModalProps) {
  return (
    <>
      {/* 백드롭: closeOnBackdrop=true이면 클릭 시 닫힘 */}
      <div
        className="modal-backdrop-custom"
        onClick={closeOnBackdrop ? onClose : undefined}
      />

      {/* 모달 본체 */}
      <div className="modal show d-block" tabIndex={-1} style={{ zIndex: 1050 }}>
        <div className={`modal-dialog modal-${size} modal-dialog-scrollable`}>
          <div className="modal-content">

            {/* 헤더 */}
            <div className="modal-header">
              <h5 className="modal-title">{title}</h5>
              <button type="button" className="btn-close" onClick={onClose} />
            </div>

            {/* 바디 */}
            <div className="modal-body">
              {children}
            </div>

            {/* 푸터: footer prop이 있을 때만 렌더링 */}
            {footer && (
              <div className="modal-footer">
                {footer}
              </div>
            )}

          </div>
        </div>
      </div>
    </>
  )
}

export default AppModal
