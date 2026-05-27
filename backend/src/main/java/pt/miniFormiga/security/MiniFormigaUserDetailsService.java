package pt.miniFormiga.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.subsistemas.utilizadores.repository.UtilizadorRepository;

import java.util.ArrayList;

@Service
public class MiniFormigaUserDetailsService implements UserDetailsService {

    private final UtilizadorRepository utilizadorRepository;

    public MiniFormigaUserDetailsService(UtilizadorRepository utilizadorRepository) {
        this.utilizadorRepository = utilizadorRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilizador utilizador = utilizadorRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador nao encontrado"));
        ArrayList<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + utilizador.getPerfil().getNome()));
        utilizador.getPerfil().getPermissoes().stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return User.builder()
                .username(utilizador.getUsername())
                .password(utilizador.getPasswordHash())
                .authorities(authorities)
                .disabled(!utilizador.isAtivo())
                .build();
    }
}
