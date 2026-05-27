import { roles, type RoleId } from '../config/appConfig'

const profileRoleMap: Record<string, RoleId> = {
  GESTOR: 'gestor',
  GERENTE: 'gerente',
  FUNCIONARIO: 'funcionario',
  RESPONSAVEL_ARMAZEM: 'armazem',
  ARMAZEM: 'armazem',
}

export function perfilToRoleId(perfil: string): RoleId {
  return profileRoleMap[perfil] ?? 'funcionario'
}

export function roleDefaultPath(roleId: RoleId) {
  return roles[roleId].defaultPath
}
