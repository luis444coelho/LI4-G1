import type { CSSProperties, FormEvent } from 'react'
import { useMemo, useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'

import { getRole, roleList } from '../data/mockData'
import { Icon, LogoMark } from '../components/icons'
import { Button, TextField } from '../components/ui'

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

        <p className="auth-note">Demonstração · escolha o perfil para explorar</p>
      </div>
    </div>
  )
}

export function AuthenticationPage() {
  const { roleId } = useParams()
  const navigate = useNavigate()
  const role = useMemo(() => getRole(roleId), [roleId])
  const [password, setPassword] = useState('miniformiga')

  if (!role) {
    return <Navigate to="/" replace />
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    navigate(role.defaultPath)
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
          label="PALAVRA-PASSE"
          type="password"
          icon="lock"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />

        <Button type="submit" className="auth-submit">
          Entrar
        </Button>

        <p className="auth-note">Demonstração · qualquer palavra-passe funciona</p>
      </form>
    </div>
  )
}
