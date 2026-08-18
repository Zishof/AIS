package ais.common;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sisdes.Penduduk;

@SuppressWarnings("deprecation")
@Service("userDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// TODO Auto-generated method stub
		return getUserDetails(username);
	}

	public static UserDetails getUserDetails(String username) throws UsernameNotFoundException {

		Object[] obj = SecurityFilter.dataLogin.get(username);
		if (obj == null) {
			throw new UsernameNotFoundException("login failed");
		}

		Tbmuser users = null;
		Mahasiswa mahasiswa = null;
		BiodataCalonMahasiswa biodataCalonMahasiswa = null;
		Siswa siswa = null;
		Penduduk penduduk = null;
		if (obj[0] instanceof BiodataCalonMahasiswa) {
			biodataCalonMahasiswa = (BiodataCalonMahasiswa) obj[0];
		} else if (obj[0] instanceof Mahasiswa) {
			mahasiswa = (Mahasiswa) obj[0];
		} else if (obj[0] instanceof Siswa) {
			siswa = (Siswa) obj[0];
		} else if (obj[0] instanceof Penduduk) {
			penduduk = (Penduduk) obj[0];
		} else if (obj[0] instanceof Tbmuser) {
			users = (Tbmuser) obj[0];
		}

		if (users == null && mahasiswa == null && siswa == null && biodataCalonMahasiswa == null && penduduk == null) {
			throw new UsernameNotFoundException("user not found");
		} else if (users != null) {

			String password = null;
			try {
				password = Common.desEncrypter.get().decrypt(users.getUserPassword().trim());
				//System.out.println("password user = " + password);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/UserDetailsServiceImpl.java:64");
			}
			boolean enabled = users.getAktif() && users.hakAkses() != null && users.hakAkses().getAktif();
			boolean accountNonExpired = true;
			boolean credentialsNonExpired = true;
			boolean accountNonLocked = true;

			Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
			Tbmrole role = users.hakAkses();
			if (role != null) {
				authorities.add(new GrantedAuthorityImpl("ROLE_USER"));
			}

			if (users != null && users.getRoot() != null && users.getRoot()) {
				authorities.add(new GrantedAuthorityImpl("ROLE_SUPERVISOR"));
			}

			User user = new User(username, password, enabled, accountNonExpired, credentialsNonExpired,
					accountNonLocked, authorities);

			return user;
		} else {

			if (mahasiswa != null) {
				if (mahasiswa.getUserOrtu() != null && !mahasiswa.getUserOrtu().trim().equals("")
						&& mahasiswa.getUserOrtu().trim().equals(username.trim())) {

					String password = null;
					try {
						password = Common.desEncrypter.get().decrypt(mahasiswa.getPassOrtu().trim());
						// KEAMANAN: JANGAN cetak password plaintext ke log (kebocoran data pribadi).
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
					boolean enabled = mahasiswa.getAktif();
					boolean accountNonExpired = true;
					boolean credentialsNonExpired = true;
					boolean accountNonLocked = true;

					Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

					authorities.add(new GrantedAuthorityImpl("ROLE_USER"));

					User user = new User(username, password, enabled, accountNonExpired, credentialsNonExpired,
							accountNonLocked, authorities);

					return user;

				} else {

					String password = null;
					try {
						password = Common.desEncrypter.get().decrypt(mahasiswa.getPass().trim());
						// KEAMANAN: JANGAN cetak password plaintext ke log (kebocoran data pribadi).
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
					boolean enabled = mahasiswa.getAktif();
					boolean accountNonExpired = true;
					boolean credentialsNonExpired = true;
					boolean accountNonLocked = true;

					Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

					authorities.add(new GrantedAuthorityImpl("ROLE_USER"));

					User user = new User(username, password, enabled, accountNonExpired, credentialsNonExpired,
							accountNonLocked, authorities);

					return user;
				}
			} else if (siswa != null) {

				String password = null;
				try {
					password = Common.desEncrypter.get().decrypt(siswa.getPass().trim());
					// KEAMANAN: JANGAN cetak password plaintext ke log (kebocoran data pribadi).
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				boolean enabled = siswa.getAktif();
				boolean accountNonExpired = true;
				boolean credentialsNonExpired = true;
				boolean accountNonLocked = true;

				Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

				authorities.add(new GrantedAuthorityImpl("ROLE_USER"));

				User user = new User(username, password, enabled, accountNonExpired, credentialsNonExpired,
						accountNonLocked, authorities);

				return user;
			} else if (penduduk != null) {

				String password = null;
				try {
					password = Common.desEncrypter.get().decrypt(penduduk.getPass().trim());
					// KEAMANAN: JANGAN cetak password plaintext ke log (kebocoran data pribadi).
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				boolean enabled = penduduk.getAktif();
				boolean accountNonExpired = true;
				boolean credentialsNonExpired = true;
				boolean accountNonLocked = true;

				Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

				authorities.add(new GrantedAuthorityImpl("ROLE_USER"));

				User user = new User(username, password, enabled, accountNonExpired, credentialsNonExpired,
						accountNonLocked, authorities);

				return user;
			} else if (biodataCalonMahasiswa != null) {

				String password = null;
				try {
					password = biodataCalonMahasiswa.getNoRegistrasi();
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				boolean enabled = true;
				boolean accountNonExpired = true;
				boolean credentialsNonExpired = true;
				boolean accountNonLocked = true;

				Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

				authorities.add(new GrantedAuthorityImpl("ROLE_USER"));

				User user = new User(username, password, enabled, accountNonExpired, credentialsNonExpired,
						accountNonLocked, authorities);

				return user;
			} else {
				throw new UsernameNotFoundException("user not found");
			}
		}

	}

}