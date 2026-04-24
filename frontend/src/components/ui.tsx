import type { ButtonHTMLAttributes, CSSProperties, InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react'

import type { IconName, StatusTone } from '../data/mockData'
import { cn } from '../lib/cn'
import { Icon } from './icons'

export function TopChip({ children }: { children: ReactNode }) {
  return <span className="mf-top-chip">{children}</span>
}

export function StatusBadge({
  children,
  tone,
  compact = false,
}: {
  children: ReactNode
  tone: StatusTone
  compact?: boolean
}) {
  return <span className={cn('mf-status', `tone-${tone}`, compact && 'is-compact')}>{children}</span>
}

export function Panel({
  title,
  action,
  className,
  children,
}: {
  title?: ReactNode
  action?: ReactNode
  className?: string
  children: ReactNode
}) {
  return (
    <section className={cn('mf-panel', className)}>
      {(title || action) && (
        <header className="mf-panel-header">
          {title ? <h2 className="mf-panel-title">{title}</h2> : <span />}
          {action}
        </header>
      )}
      {children}
    </section>
  )
}

export function MetricCard({
  label,
  value,
  footnote,
  tone,
}: {
  label: string
  value: string
  footnote: string
  tone: 'success' | 'danger' | 'neutral'
}) {
  return (
    <article className="mf-metric-card">
      <span className="mf-metric-label">{label}</span>
      <strong className="mf-metric-value">{value}</strong>
      <span className={cn('mf-metric-footnote', tone !== 'neutral' && `tone-${tone}`)}>{footnote}</span>
    </article>
  )
}

export function Button({
  className,
  variant = 'primary',
  children,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'ghost'
}) {
  return (
    <button className={cn('mf-button', `variant-${variant}`, className)} {...props}>
      {children}
    </button>
  )
}

export function TextField({
  label,
  icon,
  className,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & {
  label?: string
  icon?: IconName
}) {
  return (
    <label className={cn('mf-field', className)}>
      {label ? <span className="mf-field-label">{label}</span> : null}
      <span className={cn('mf-input-wrap', icon && 'has-icon')}>
        {icon ? <Icon name={icon} className="mf-field-icon" /> : null}
        <input className="mf-input" {...props} />
      </span>
    </label>
  )
}

export function SelectField({
  label,
  options,
  className,
  ...props
}: SelectHTMLAttributes<HTMLSelectElement> & {
  label?: string
  options: string[]
}) {
  return (
    <label className={cn('mf-field', className)}>
      {label ? <span className="mf-field-label">{label}</span> : null}
      <span className="mf-select-wrap">
        <select className="mf-select" {...props}>
          {options.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
        <Icon name="chevron" className="mf-select-icon" />
      </span>
    </label>
  )
}

export function Callout({
  tone,
  children,
  className,
}: {
  tone: 'warning' | 'info'
  children: ReactNode
  className?: string
}) {
  return <div className={cn('mf-callout', `tone-${tone}`, className)}>{children}</div>
}

export function ProgressBar({
  value,
  tone,
}: {
  value: number
  tone: 'success' | 'danger'
}) {
  return (
    <div className="mf-progress">
      <span
        className={cn('mf-progress-fill', `tone-${tone}`)}
        style={{ width: `${Math.min(value, 100)}%` } as CSSProperties}
      />
    </div>
  )
}

export function InitialAvatar({ initials }: { initials: string }) {
  return <span className="mf-avatar">{initials}</span>
}
