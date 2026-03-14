import { toast } from 'react-toastify'
import Swal from 'sweetalert2'

// ─────────────────────────────────────────────
// UI — Toast (입력 검증·간단 안내, 우측 상단)
// ─────────────────────────────────────────────

/** 우측 상단 토스트 (입력값 검증 오류·간단 안내용). 앱에 <ToastContainer>가 마운트되어 있어야 함 */
export const gToast = (msg: string, type: 'info' | 'success' | 'error' | 'warning' = 'info') =>
  toast[type](msg)

// ─────────────────────────────────────────────
// UI — 로딩
// ─────────────────────────────────────────────

/** 로딩 다이얼로그 표시. 반드시 gCloseLoading()으로 닫을 것 */
export const gLoading = (title = '처리 중...') => {
  Swal.fire({
    title,
    allowOutsideClick: false,
    showConfirmButton: false,
    didOpen: () => Swal.showLoading(),
  })
}

/** 로딩 다이얼로그 닫기 */
export const gCloseLoading = () => Swal.close()

// ─────────────────────────────────────────────
// UI — 성공 메시지
// ─────────────────────────────────────────────

/** 성공 메시지 (중앙, 확인 버튼 + 3초 타이머 자동 닫힘) */
export const gSuccess = (title: string, html = '') =>
  Swal.fire({
    title,
    html: html || undefined,
    icon: 'success',
    timer: 3000,
    timerProgressBar: true,
    showConfirmButton: true,
    confirmButtonText: '확인',
  })

// ─────────────────────────────────────────────
// UI — 오류/안내 메시지
// ─────────────────────────────────────────────

/** 오류·안내 메시지 (중앙, 확인 클릭 시 닫힘) */
export const gAlert = (title: string, html = '', icon: 'error' | 'warning' | 'info' = 'error') =>
  Swal.fire({
    title,
    html: html || undefined,
    icon,
    confirmButtonText: '확인',
  })

// ─────────────────────────────────────────────
// UI — Confirm
// ─────────────────────────────────────────────

/** Confirm 다이얼로그. 확인 → true, 취소/외부클릭 → false */
export const gConfirm = (title: string, html = ''): Promise<boolean> =>
  Swal.fire({
    title,
    html: html || undefined,
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: '확인',
    cancelButtonText: '취소',
  }).then((r) => r.isConfirmed)
