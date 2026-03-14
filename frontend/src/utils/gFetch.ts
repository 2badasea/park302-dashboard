/**
 * fetch 래퍼
 * - JSON Content-Type 기본 설정 (multipart 전송 시 headers에서 Content-Type 제외할 것)
 * - HTTP 에러(response.ok=false) 시 Error throw
 * - 응답 JSON 자동 파싱 (204 No Content는 null 반환)
 *
 * TODO: JWT 인증 방식 확정 후 Authorization: Bearer <token> 헤더 자동 추가 로직 구현 필요
 */
export const gFetch = async (url: string, options: RequestInit = {}): Promise<unknown> => {
  const { headers, ...rest } = options

  const res = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...(headers as Record<string, string>),
    },
    ...rest,
  })

  if (!res.ok) {
    const err = new Error(`HTTP ${res.status}`) as Error & { status: number; data: unknown }
    err.status = res.status
    try {
      err.data = await res.json()
    } catch {
      err.data = null
    }
    throw err
  }

  if (res.status === 204) return null
  return res.json()
}
