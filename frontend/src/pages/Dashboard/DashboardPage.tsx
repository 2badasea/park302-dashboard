import { useState } from 'react'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import interactionPlugin from '@fullcalendar/interaction'
import listPlugin from '@fullcalendar/list'
import koLocale from '@fullcalendar/core/locales/ko'
import styles from './DashboardPage.module.css'

type ViewMode = 'calendar' | 'list'

// 정적 샘플 이벤트 (API 연동 전 임시)
const sampleEvents = [
  { id: '1', title: '[업체A] 유지보수 요청', date: '2026-03-15', color: '#4f86f7' },
  { id: '2', title: '[업체B] 추가개발 미팅', date: '2026-03-18', color: '#f7a24f' },
  { id: '3', title: '서버 점검 예정', date: '2026-03-20', color: '#e05c5c' },
]

function DashboardPage() {
  const [viewMode, setViewMode] = useState<ViewMode>('calendar')

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">대시보드</h1>
        <div className={styles.viewToggle}>
          <button
            className={`btn ${viewMode === 'calendar' ? 'btn-primary' : 'btn-outline-secondary'}`}
            onClick={() => setViewMode('calendar')}
          >
            캘린더
          </button>
          <button
            className={`btn ${viewMode === 'list' ? 'btn-primary' : 'btn-outline-secondary'}`}
            onClick={() => setViewMode('list')}
          >
            리스트
          </button>
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          {viewMode === 'calendar' ? (
            <FullCalendar
              plugins={[dayGridPlugin, interactionPlugin]}
              initialView="dayGridMonth"
              locale={koLocale}
              events={sampleEvents}
              headerToolbar={{
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,dayGridWeek',
              }}
              height="auto"
            />
          ) : (
            <FullCalendar
              plugins={[listPlugin]}
              initialView="listMonth"
              locale={koLocale}
              events={sampleEvents}
              headerToolbar={{
                left: 'prev,next today',
                center: 'title',
                right: '',
              }}
              height="auto"
            />
          )}
        </div>
      </div>
    </div>
  )
}

export default DashboardPage
