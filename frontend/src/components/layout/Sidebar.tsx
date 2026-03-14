import { NavLink } from 'react-router-dom'
import styles from './Sidebar.module.css'

interface SidebarProps {
  collapsed: boolean
}

const menuItems = [
  { path: '/dashboard',  label: '대시보드',  icon: '📊' },
  { path: '/agents',     label: '업체관리',  icon: '🏢' },
  { path: '/inquiries',  label: '문의관리',  icon: '💬' },
  { path: '/notices',    label: '공지관리',  icon: '📢' },
  { path: '/board',      label: '게시판',    icon: '📋' },
  { path: '/settings',   label: '설정관리',  icon: '⚙️' },
]

function Sidebar({ collapsed }: SidebarProps) {
  return (
    <aside className={`${styles.sidebar} ${collapsed ? styles.collapsed : ''}`}>
      <div className={styles.logo}>
        {collapsed ? 'P3' : 'Park302'}
      </div>
      <nav className={styles.nav}>
        {menuItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `${styles.navItem} ${isActive ? styles.active : ''}`
            }
          >
            <span className={styles.icon}>{item.icon}</span>
            {!collapsed && <span className={styles.label}>{item.label}</span>}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}

export default Sidebar
