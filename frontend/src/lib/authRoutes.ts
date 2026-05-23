import { roles, type RoleId } from '../data/mockData'

export function perfilToRoleId(perfil: string): RoleId {
  switch (perfil) {
    case 'GESTOR':
      return 'gestor'
    case 'GERENTE':
      return 'gerente'
    case 'FUNCIONARIO':
      return 'funcionario'
    case 'RESPONSAVEL_ARMAZEM':
      return 'armazem'
    default:
      return 'funcionario'
  }
}

export function roleDefaultPath(roleId: RoleId) {
  return roles[roleId].defaultPath
}
