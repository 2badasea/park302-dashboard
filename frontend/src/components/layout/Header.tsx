import styles from './Header.module.css'

interface HeaderProps {
  onToggleSidebar: () => void
}

function Header({ onToggleSidebar }: HeaderProps) {
  return (
    <header className={styles.header}>
      <button className={styles.toggleBtn} onClick={onToggleSidebar} aria-label="사이드바 토글">
        ☰
      </button>
      <div className={styles.right}>
        <span className={styles.adminLabel}>관리자</span>
      </div>
    </header>
  )
}

export default Header
