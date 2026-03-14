import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import Header from './Header'
import styles from './Layout.module.css'

function Layout() {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)

  return (
    <div className={styles.wrapper}>
      <Sidebar collapsed={sidebarCollapsed} />
      <div className={`${styles.main} ${sidebarCollapsed ? styles.mainExpanded : ''}`}>
        <Header onToggleSidebar={() => setSidebarCollapsed((prev) => !prev)} />
        <div className={styles.content}>
          <Outlet />
        </div>
      </div>
    </div>
  )
}

export default Layout
