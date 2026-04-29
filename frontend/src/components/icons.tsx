import type { IconName } from '../data/mockData'

interface IconProps {
  name: IconName
  className?: string
}

const commonProps = {
  fill: 'none',
  stroke: 'currentColor',
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
  strokeWidth: 1.8,
}

export function LogoMark({ className = '' }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
      <circle cx="12" cy="12" r="1.8" fill="currentColor" />
      <circle cx="8" cy="8" r="1.8" fill="currentColor" />
      <circle cx="16" cy="8" r="1.8" fill="currentColor" />
      <circle cx="8" cy="16" r="1.8" fill="currentColor" />
      <circle cx="16" cy="16" r="1.8" fill="currentColor" />
    </svg>
  )
}

export function Icon({ name, className = '' }: IconProps) {
  switch (name) {
    case 'dashboard':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <rect x="4.5" y="4.5" width="6" height="6" rx="1.3" />
          <rect x="13.5" y="4.5" width="6" height="6" rx="1.3" />
          <rect x="4.5" y="13.5" width="6" height="6" rx="1.3" />
          <rect x="13.5" y="13.5" width="6" height="6" rx="1.3" />
        </svg>
      )
    case 'reports':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <rect x="6" y="4.5" width="12" height="15" rx="2" />
          <path d="M9 9.5h6M9 13h6M9 16.5h4" />
        </svg>
      )
    case 'suppliers':
    case 'staff':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <circle cx="12" cy="8" r="3.2" />
          <path d="M6.5 19c.9-3 3-4.5 5.5-4.5S16.6 16 17.5 19" />
        </svg>
      )
    case 'users':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <circle cx="8" cy="8.5" r="2.6" />
          <circle cx="16.5" cy="9.3" r="2.2" />
          <path d="M3.8 18.2c.7-2.6 2.5-4 4.8-4s4.1 1.4 4.8 4" />
          <path d="M14 17.6c.5-1.9 1.9-3 3.7-3 1.1 0 2.1.4 2.8 1.2" />
        </svg>
      )
    case 'orders':
    case 'warehouse':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <path d="M6 9.5h12v8.5a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2Z" />
          <path d="M9 9.5V8a3 3 0 1 1 6 0v1.5" />
        </svg>
      )
    case 'sync':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <path d="M6.5 9.2A6.5 6.5 0 0 1 17 7.7" />
          <path d="m17 7.7 1.8 1.1L20 6.7" />
          <path d="M17.5 14.8A6.5 6.5 0 0 1 7 16.3" />
          <path d="m7 16.3-1.8-1.1L4 17.3" />
        </svg>
      )
    case 'stock':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <path d="M5 18.5h14" />
          <path d="M7.5 18V11M12 18V7.5M16.5 18v-4.5" />
          <rect x="6" y="11" width="3" height="7" rx="1" />
          <rect x="10.5" y="7.5" width="3" height="10.5" rx="1" />
          <rect x="15" y="13.5" width="3" height="4.5" rx="1" />
        </svg>
      )
    case 'cash':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <rect x="4.5" y="7" width="15" height="10" rx="2" />
          <path d="M8 12h8" />
          <path d="M6.5 10.2h1.2M16.3 13.8h1.2" />
        </svg>
      )
    case 'adjustment':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <circle cx="12" cy="12" r="6.8" />
          <path d="M12 8.2v3.8l2.8 2.2" />
        </svg>
      )
    case 'sale':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <rect x="5" y="5" width="14" height="14" rx="2" />
          <path d="M8.5 8.5h7M8.5 12h7M8.5 15.5h7" />
          <path d="M12 8.5v7" />
        </svg>
      )
    case 'return':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <path d="M9.2 8H5.5v3.7" />
          <path d="M6 11a6 6 0 1 0 1.8-4.2L5.5 8" />
        </svg>
      )
    case 'goods':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <path d="M6.2 8.5 12 5l5.8 3.5v7L12 19l-5.8-3.5Z" />
          <path d="M12 5v7M6.2 8.5 12 12l5.8-3.5" />
        </svg>
      )
    case 'inventory':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <rect x="6" y="4.5" width="12" height="15" rx="2" />
          <path d="M9 9.5h2.5M9 13.5h6.5" />
          <path d="m14.5 9.8 1.3 1.4 2.2-2.4" />
        </svg>
      )
    case 'chain':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <circle cx="12" cy="8" r="3.1" />
          <path d="M6.3 18.5c.8-3 2.9-4.5 5.7-4.5s4.9 1.5 5.7 4.5" />
        </svg>
      )
    case 'store':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <path d="M6 9.5h12v8.8a1.7 1.7 0 0 1-1.7 1.7H7.7A1.7 1.7 0 0 1 6 18.3Z" />
          <path d="M7.5 9.5V7.8h9v1.7" />
        </svg>
      )
    case 'employee':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <rect x="7" y="5.5" width="10" height="13" rx="2" />
          <path d="M9.5 9.5h5M9.5 13h5" />
        </svg>
      )
    case 'lock':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <rect x="6.5" y="10.2" width="11" height="8.3" rx="2" />
          <path d="M9 10V8.5a3 3 0 1 1 6 0V10" />
        </svg>
      )
    case 'chevron':
      return (
        <svg viewBox="0 0 24 24" className={className} aria-hidden="true" {...commonProps}>
          <path d="m10 7 5 5-5 5" />
        </svg>
      )
    default:
      return null
  }
}
