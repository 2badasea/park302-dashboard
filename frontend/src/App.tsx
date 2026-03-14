import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/layout/Layout'
import DashboardPage from './pages/Dashboard/DashboardPage'
import AgentsPage from './pages/Agents/AgentsPage'
import InquiriesPage from './pages/Inquiries/InquiriesPage'
import NoticesPage from './pages/Notices/NoticesPage'
import BoardPage from './pages/Board/BoardPage'
import SettingsPage from './pages/Settings/SettingsPage'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="agents" element={<AgentsPage />} />
          <Route path="inquiries" element={<InquiriesPage />} />
          <Route path="notices" element={<NoticesPage />} />
          <Route path="board" element={<BoardPage />} />
          <Route path="settings" element={<SettingsPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
