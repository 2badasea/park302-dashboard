import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react'
import Grid from 'tui-grid'
import type { ColumnOptions, GridOptions, Row } from 'tui-grid'
import 'tui-grid/dist/tui-grid.css'

interface ToastGridWrapperProps {
  columns: ColumnOptions[]
  data: Row[]
  options?: Omit<GridOptions, 'el' | 'columns' | 'data'>
  /** 행 클릭 콜백. 체크박스(_checked) 클릭은 제외된다. */
  onRowClick?: (rowData: Row) => void
}

/** ToastGridWrapper에서 외부로 노출하는 명령형 핸들 */
export interface ToastGridHandle {
  /** 체크된 행 목록 반환 (삭제 등 벌크 작업에 활용) */
  getCheckedRows: () => Row[]
}

/**
 * tui-grid 코어를 감싼 React 래퍼 컴포넌트
 *
 * @toast-ui/react-grid는 React 18과 호환되지 않아(ReactDOM.render deprecated) 채택하지 않음.
 * 대신 tui-grid 코어를 useEffect에서 직접 초기화하여 사용한다.
 *
 * - 데이터 변경 시 gridInstance.resetData()로 갱신
 * - 컬럼/옵션 변경은 컴포넌트 재마운트로 처리
 * - forwardRef + useImperativeHandle로 getCheckedRows 노출
 * - onRowClick 제공 시 행 클릭 이벤트 바인딩 (체크박스 클릭 제외)
 */
const ToastGridWrapper = forwardRef<ToastGridHandle, ToastGridWrapperProps>(
  ({ columns, data, options = {}, onRowClick }, ref) => {
    const containerRef = useRef<HTMLDivElement>(null)
    const gridRef = useRef<Grid | null>(null)
    // rAF 시점에 최신 data를 참조하기 위한 ref
    // (rAF가 실행되기 전에 data prop이 바뀌는 경우를 대비)
    const dataRef = useRef<Row[]>(data)

    // 외부에서 getCheckedRows를 호출할 수 있도록 handle 노출
    useImperativeHandle(ref, () => ({
      getCheckedRows: () => gridRef.current?.getCheckedRows() ?? [],
    }))

    // 초기 마운트 시 Grid 인스턴스 생성
    // requestAnimationFrame으로 한 프레임 뒤에 초기화하여 부모 컨테이너의 레이아웃이
    // 완성된 이후에 TUI Grid가 clientHeight 등을 계산하도록 한다.
    useEffect(() => {
      let frameId: number

      frameId = requestAnimationFrame(() => {
        if (!containerRef.current) return

        gridRef.current = new Grid({
          el: containerRef.current,
          columns,
          data: dataRef.current,  // rAF 시점의 최신 데이터 사용
          bodyHeight: 500,
          rowHeight: 40,
          ...options,
        })

        // 행 클릭 이벤트: 체크박스(_checked) 클릭은 모달 오픈에서 제외
        // tui-grid click event 객체에 rowKey, columnName이 포함되나 타입 정의가 불완전하여 any 처리
        if (onRowClick) {
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          gridRef.current.on('click', (ev: any) => {
            if (ev.rowKey !== null && ev.rowKey !== undefined && ev.columnName !== '_checked') {
              const rowData = gridRef.current?.getRow(ev.rowKey)
              if (rowData) onRowClick(rowData)
            }
          })
        }
      })

      return () => {
        cancelAnimationFrame(frameId)
        gridRef.current?.destroy()
        gridRef.current = null
      }
      // columns, options, onRowClick은 마운트 시 1회만 적용 (변경 시 재마운트 필요)
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    // 데이터 변경 시 dataRef 동기화 및 그리드 갱신
    // 그리드가 아직 초기화되지 않은 경우(rAF 대기 중) dataRef만 업데이트하고,
    // 초기화 후에는 resetData로 반영된다.
    useEffect(() => {
      dataRef.current = data
      if (gridRef.current) {
        gridRef.current.resetData(data)
      }
    }, [data])

    // 컨테이너 크기 변화 감지 → refreshLayout() 자동 호출
    // 사이드바 토글, 윈도우 리사이즈 등 레이아웃 변경 시 그리드가 새 폭에 맞게 재계산된다.
    // gridRef가 아직 null(rAF 대기 중)이면 no-op으로 무시된다.
    useEffect(() => {
      if (!containerRef.current) return

      const observer = new ResizeObserver(() => {
        gridRef.current?.refreshLayout()
      })
      observer.observe(containerRef.current)

      return () => observer.disconnect()
    }, [])

    return (
      <div
        ref={containerRef}
        // onRowClick이 있으면 행에 pointer 커서 적용 (common.css .tui-grid-cursor-pointer 참고)
        className={onRowClick ? 'tui-grid-cursor-pointer' : undefined}
      />
    )
  }
)

ToastGridWrapper.displayName = 'ToastGridWrapper'

export default ToastGridWrapper
