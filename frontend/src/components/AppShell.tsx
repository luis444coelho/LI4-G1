import type { CSSProperties, ReactNode } from 'react'

import { Link, NavLink, useLocation } from 'react-router-dom'

import type { RoleConfig } from '../data/mockData'
import { cn } from '../lib/cn'
import { Icon, LogoMark } from './icons'
import { TopChip } from './ui'

interface AppShellProps {
  role: RoleConfig
  title: string
  children: ReactNode
}

export function AppShell({ role, title, children }: AppShellProps) {
  const location = useLocation()
  const shellStyle = {
    '--accent': role.accent,
    '--accent-soft': role.accentSoft,
    '--accent-border': role.accentBorder,
    '--accent-muted': role.accentMuted,
    '--icon-bg': role.iconBg,
    '--icon-border': role.iconBorder,
  } as CSSProperties

  return (
    <div className="mf-screen">
      <div className="mf-shell" style={shellStyle}>
        <aside className="mf-sidebar">
          <div className="mf-brand-row">
            <div className="mf-brand-badge">
              <LogoMark className="mf-brand-mark" />
            </div>
            <span className="mf-brand-text">Mini-Formiga</span>
          </div>

          <div className="mf-sidebar-scroll">
            <span className="mf-menu-label">MENU</span>
            <nav className="mf-menu">
              {role.menu.map((item) => (
                <NavLink
                  key={item.path}
                  to={item.path}
                  className={({ isActive }) =>
                    cn(
                      'mf-menu-item',
                      (isActive || location.pathname === item.path) && 'is-active',
                    )
                  }
                >
                  <span className="mf-menu-icon-wrap">
                    <Icon name={item.icon} className="mf-menu-icon" />
                  </span>
                  <span>{item.label}</span>
                </NavLink>
              ))}
            </nav>
          </div>

          <div className="mf-sidebar-footer">
            <div className="mf-user-card">
              <span className="mf-user-badge">
                <LogoMark className="mf-user-mark" />
              </span>
              <div className="mf-user-text">
                <span className="mf-user-brand">Mini-Formiga</span>
                <span className="mf-user-role">{role.sidebarSubtitle}</span>
              </div>
            </div>
            <Link to="/" className="mf-logout">
              Terminar sessão
            </Link>
          </div>
        </aside>

        <div className="mf-main">
          <header className="mf-topbar">
            <div className="mf-topbar-left">
              <h1 className="mf-page-title">{title}</h1>
              <TopChip>{role.authLabel}</TopChip>
            </div>
            <div className="mf-online">
              <span className="mf-online-dot" />
              <span>Sistema online</span>
            </div>
          </header>

          <main className="mf-page-content">{children}</main>
        </div>
      </div>
    </div>
  )
}
