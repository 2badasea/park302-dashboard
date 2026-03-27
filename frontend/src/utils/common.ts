import { toast } from 'react-toastify'
import Swal from 'sweetalert2'

// =============================================================================
// UI — Toast (입력 검증·간단 안내, 우측 상단)
// =============================================================================

/** 우측 상단 토스트 (입력값 검증 오류·간단 안내용). 앱에 <ToastContainer>가 마운트되어 있어야 함 */
export const gToast = (msg: string, type: 'info' | 'success' | 'error' | 'warning' = 'info') =>
  toast[type](msg)

// =============================================================================
// UI — 로딩
// =============================================================================

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

// =============================================================================
// UI — 성공 메시지
// =============================================================================

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

// =============================================================================
// UI — 오류/안내 메시지
// =============================================================================

/** 오류·안내 메시지 (중앙, 확인 클릭 시 닫힘) */
export const gAlert = (title: string, html = '', icon: 'error' | 'warning' | 'info' = 'error') =>
  Swal.fire({
    title,
    html: html || undefined,
    icon,
    confirmButtonText: '확인',
  })

// =============================================================================
// UI — Confirm
// =============================================================================

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

// =============================================================================
// 검증 유틸
// =============================================================================

/**
 * 단일 값 빈값·공백 검증
 * @returns true: 유효한 값 있음 / false: null·undefined·빈 문자열·공백만 있음
 */
export const checkInput = (value: string | null | undefined): boolean =>
  typeof value === 'string' && value.trim().length > 0

/**
 * 여러 필드 일괄 필수 검증
 * 배열 순서대로 검사하여 첫 번째 실패 항목의 안내 메시지를 반환한다.
 * 모두 통과하면 null을 반환한다.
 *
 * @param fields - { value: 검증할 값, label: 필드 표시명 } 배열
 * @returns 첫 번째 실패 메시지 | null
 *
 * @example
 * const err = checkRequired([
 *   { value: form.name, label: '업체명' },
 *   { value: form.contactTel, label: '연락처' },
 * ])
 * if (err) { gToast(err, 'warning'); return }
 */
export const checkRequired = (
  fields: { value: string | null | undefined; label: string }[]
): string | null => {
  for (const { value, label } of fields) {
    if (!checkInput(value)) return `${label}을(를) 입력해 주세요.`
  }
  return null
}

/**
 * 이메일 형식 검증
 * @returns true: 유효한 이메일 형식 / false: 형식 불일치
 */
export const validateEmail = (value: string): boolean =>
  /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim())

// =============================================================================
// 입력값 포맷팅 유틸
// =============================================================================

/**
 * 사업자등록번호 자동 포맷팅 (000-00-00000)
 * 숫자 이외의 문자는 제거하고, 자릿수에 맞춰 하이픈을 자동 삽입한다.
 * onChange 핸들러에서 호출하여 controlled input에 적용한다.
 *
 * @example
 * onChange={(e) => setForm(prev => ({ ...prev, businessNumber: formatBusinessNumber(e.target.value) }))}
 */
export const formatBusinessNumber = (value: string): string => {
  const digits = value.replace(/\D/g, '').slice(0, 10)
  if (digits.length < 4) return digits
  if (digits.length < 6) return `${digits.slice(0, 3)}-${digits.slice(3)}`
  return `${digits.slice(0, 3)}-${digits.slice(3, 5)}-${digits.slice(5)}`
}

/**
 * 전화번호 자동 포맷팅
 * 서울(02): 02-XXXX-XXXX 또는 02-XXX-XXXX
 * 그 외 (010, 031 등): 000-XXXX-XXXX
 * 숫자 이외의 문자는 제거하고, 최대 11자리까지 허용한다.
 * onChange 핸들러에서 호출하여 controlled input에 적용한다.
 *
 * @example
 * onChange={(e) => setForm(prev => ({ ...prev, contactTel: formatTel(e.target.value) }))}
 */
export const formatTel = (value: string): string => {
  const digits = value.replace(/\D/g, '').slice(0, 11)
  if (digits.startsWith('02')) {
    // 서울 지역번호: 2자리 국번
    if (digits.length < 3) return digits
    if (digits.length < 7) return `${digits.slice(0, 2)}-${digits.slice(2)}`
    return `${digits.slice(0, 2)}-${digits.slice(2, -4)}-${digits.slice(-4)}`
  }
  // 010, 031 등 3자리 국번
  if (digits.length < 4) return digits
  if (digits.length < 8) return `${digits.slice(0, 3)}-${digits.slice(3)}`
  return `${digits.slice(0, 3)}-${digits.slice(3, -4)}-${digits.slice(-4)}`
}
