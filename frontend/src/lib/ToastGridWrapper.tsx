import { useEffect, useRef } from 'react'
import Grid from 'tui-grid'
import 'tui-grid/dist/tui-grid.css'

interface ToastGridWrapperProps {
  columns: Grid.ColumnOptions[]
  data: Grid.RowData[]
  options?: Omit<Grid.GridOptions, 'el' | 'columns' | 'data'>
}

/**
 * tui-grid 코어를 감싼 React 래퍼 컴포넌트
 *
 * @toast-ui/react-grid는 React 18과 호환되지 않아(ReactDOM.render deprecated) 채택하지 않음.
 * 대신 tui-grid 코어를 useEffect에서 직접 초기화하여 사용한다.
 *
 * - 데이터 변경 시 gridInstance.resetData()로 갱신
 * - 컬럼/옵션 변경은 컴포넌트 재마운트로 처리
 */
function ToastGridWrapper({ columns, data, options = {} }: ToastGridWrapperProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const gridRef = useRef<Grid | null>(null)

  // 초기 마운트 시 Grid 인스턴스 생성
  useEffect(() => {
    if (!containerRef.current) return

    gridRef.current = new Grid({
      el: containerRef.current,
      columns,
      data,
      bodyHeight: 400,
      rowHeight: 40,
      ...options,
    })

    return () => {
      gridRef.current?.destroy()
      gridRef.current = null
    }
    // columns, options는 마운트 시 1회만 적용 (변경 시 재마운트 필요)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // 데이터 변경 시 resetData로 갱신
  useEffect(() => {
    if (gridRef.current) {
      gridRef.current.resetData(data)
    }
  }, [data])

  return <div ref={containerRef} />
}

export default ToastGridWrapper
