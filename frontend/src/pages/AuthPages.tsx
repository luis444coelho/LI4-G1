import type { CSSProperties, FormEvent } from 'react'
import { useMemo, useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'

import { getRole, roleList, type RoleId } from '../data/mockData'
import { Icon, LogoMark } from '../components/icons'
import { Button, TextField } from '../components/ui'
import { ApiError } from '../lib/api'
import { useAuth } from '../lib/auth'
import { roleDefaultPath } from '../lib/authRoutes'

const demoUsers: Record<RoleId, string> = {
  gestor: 'gestor.formiga',
  gerente: 'gerente.braga',
  funcionario: 'operador.braga',
  armazem: 'armazem.braga',
}

export function ProfileSelectionPage() {
  return (
    <div className="auth-screen">
      <div className="auth-card selection">
        <div className="auth-logo">
          <LogoMark className="auth-logo-mark" />
        </div>

        <div className="auth-head">
          <h1>Mini-Formiga</h1>
          <p>Seleccione o seu perfil para continuar</p>
        </div>

        <div className="profile-grid">
          {roleList.map((role) => (
            <Link
              key={role.id}
              to={`/auth/${role.id}`}
              className="profile-card"
              style={
                {
                  '--profile-icon-bg': role.iconBg,
                  '--profile-icon-border': role.iconBorder,
                  '--profile-icon-color': role.accent,
                } as CSSProperties
              }
            >
              <span className="profile-icon">
                <Icon name={role.profileIcon} className="profile-icon-svg" />
              </span>
              <span className="profile-copy">
                <span className="profile-title">{role.selectionTitle}</span>
                <span className="profile-subtitle">{role.selectionSubtitle}</span>
              </span>
              <Icon name="chevron" className="profile-chevron" />
            </Link>
          ))}
        </div>

        <p className="auth-note">Demonstração · autenticação real por perfil</p>
      </div>
    </div>
  )
}

export function AuthenticationPage() {
  const { roleId } = useParams()
  const navigate = useNavigate()
  const { login, session } = useAuth()
  const role = useMemo(() => getRole(roleId), [roleId])
  const [username, setUsername] = useState(role ? demoUsers[role.id] : '')
  const [password, setPassword] = useState('MiniFormiga2026!')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  if (!role) {
    return <Navigate to="/" replace />
  }
  if (session) {
    return <Navigate to={roleDefaultPath(session.roleId)} replace />
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const nextSession = await login(username, password)
      navigate(roleDefaultPath(nextSession.roleId), { replace: true })
    } catch (caught) {
      const message = caught instanceof ApiError ? caught.message : 'Não foi possível autenticar.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-screen">
      <form className="auth-card login" onSubmit={handleSubmit}>
        <div className="auth-logo">
          <LogoMark className="auth-logo-mark" />
        </div>

        <div className="auth-head compact">
          <h1>Mini-Formiga</h1>
          <p>Autenticação</p>
        </div>

        <div
          className="selected-profile"
          style={
            {
              '--profile-icon-bg': role.iconBg,
              '--profile-icon-border': role.iconBorder,
              '--profile-icon-color': role.accent,
              '--selected-border': role.accentBorder,
            } as CSSProperties
          }
        >
          <span className="profile-icon">
            <Icon name={role.profileIcon} className="profile-icon-svg" />
          </span>
          <span className="profile-copy">
            <span className="profile-title single">{role.label}</span>
            <span className="profile-subtitle">{role.selectionSubtitle}</span>
          </span>
          <Link to="/" className="change-role">
            Alterar
          </Link>
        </div>

        <TextField
          label="UTILIZADOR"
          icon="users"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
        />

        <TextField
          label="PALAVRA-PASSE"
          type="password"
          icon="lock"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />

        {error ? <p className="auth-error">{error}</p> : null}

        <Button type="submit" className="auth-submit" disabled={loading}>
          {loading ? 'A entrar...' : 'Entrar'}
        </Button>

        <p className="auth-note">Demonstração · credenciais reais preenchidas automaticamente</p>
      </form>
    </div>
  )
}
