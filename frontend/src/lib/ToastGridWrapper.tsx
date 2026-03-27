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

    // 외부에서 getCheckedRows를 호출할 수 있도록 handle 노출
    useImperativeHandle(ref, () => ({
      getCheckedRows: () => gridRef.current?.getCheckedRows() ?? [],
    }))

    // 초기 마운트 시 Grid 인스턴스 생성
    useEffect(() => {
      if (!containerRef.current) return

      gridRef.current = new Grid({
        el: containerRef.current,
        columns,
        data,
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

      return () => {
        gridRef.current?.destroy()
        gridRef.current = null
      }
      // columns, options, onRowClick은 마운트 시 1회만 적용 (변경 시 재마운트 필요)
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    // 데이터 변경 시 resetData로 갱신
    useEffect(() => {
      if (gridRef.current) {
        gridRef.current.resetData(data)
      }
    }, [data])

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
