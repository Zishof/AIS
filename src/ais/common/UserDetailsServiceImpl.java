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

/**
 * Implementasi {@link UserDetailsService} Spring Security untuk AIS: menerjemahkan satu
 * {@code username} yang login menjadi objek {@link UserDetails} (username, password, status
 * aktif/enabled, daftar {@link GrantedAuthority}) yang dipahami kerangka kerja Spring Security
 * untuk proses autentikasi/otorisasi. Kelas ini didaftarkan sebagai bean Spring bernama
 * {@code userDetailsService} lewat anotasi {@link Service @Service}.
 *
 * <h2>Sumber data pengguna — lima jenis akun</h2>
 * <p>
 * AIS melayani lima jenis entitas yang dapat login sebagai satu "pengguna": {@link Tbmuser} (staf/
 * admin/dosen/guru — akun utama sistem, dengan hak akses lewat {@link Tbmrole}),
 * {@link Mahasiswa} (mahasiswa, termasuk akun turunan milik orang tua lewat {@code userOrtu}),
 * {@link Siswa} (siswa sekolah), {@link Penduduk} (modul Sistem Informasi Desa/{@code sisdes}), dan
 * {@link BiodataCalonMahasiswa} (calon mahasiswa pada proses PMB/penerimaan mahasiswa baru).
 * Method {@link #getUserDetails(String)} <b>tidak melakukan pencarian ke database sama sekali</b> —
 * ia hanya membaca entitas yang sudah ditemukan dan divalidasi sebelumnya (mis. lewat pencocokan
 * password saat submit form login) dan disimpan sementara di peta statis
 * {@link SecurityFilter#dataLogin}, dikunci oleh {@code username}. Ini berarti kelas ini berperan
 * sebagai <b>jembatan/adapter</b> antara mekanisme login kustom AIS (di {@code SecurityFilter}) dan
 * kontrak {@link UserDetailsService} yang dibutuhkan Spring Security, bukan sebagai titik
 * autentikasi itu sendiri.
 * </p>
 *
 * <h2>Otorisasi</h2>
 * <p>
 * Semua jenis akun diberi {@link GrantedAuthority} {@code ROLE_USER}. Khusus {@link Tbmuser} yang
 * kolom {@code root} bernilai {@code true}, ditambahkan pula {@code ROLE_SUPERVISOR}. Status
 * {@code enabled} ditentukan dari kombinasi flag aktif entitas dan (untuk {@link Tbmuser}) status
 * aktif peran/hak aksesnya ({@code hakAkses().getAktif()}); untuk {@link BiodataCalonMahasiswa},
 * {@code enabled} selalu {@code true} tanpa pengecekan tambahan.
 * </p>
 *
 * <h2>Peringatan keamanan — penanganan password</h2>
 * <p>
 * <b>Password pengguna TIDAK disimpan sebagai hash satu-arah (mis. BCrypt/Argon2/PBKDF2)
 * sebagaimana lazimnya kontrak {@link UserDetailsService} dipakai di ekosistem Spring Security.</b>
 * Sebaliknya, seluruh jalur ({@link Tbmuser}, {@link Mahasiswa} — termasuk akun turunan orang tua,
 * {@link Siswa}, {@link Penduduk}) memanggil {@code Common.desEncrypter.get().decrypt(...)} untuk
 * <b>mendekripsi</b> kolom password yang tersimpan di database menjadi teks polos, lalu
 * menyerahkan teks polos tersebut sebagai password pada objek {@link User} yang dikembalikan.
 * Ini menyiratkan password pengguna disimpan dengan <b>enkripsi simetris yang dapat dibalik</b>
 * (DES, dari nama field {@code desEncrypter}), bukan hash satu-arah — konsekuensinya, siapa pun
 * yang menguasai kunci enkripsi dan basis data (mis. lewat kebocoran/akses admin) dapat memulihkan
 * password asli seluruh pengguna dalam bentuk teks polos, bukan hanya memverifikasi kecocokan
 * password. Komentar kode di beberapa jalur ({@link Mahasiswa}, {@link Siswa}, {@link Penduduk})
 * secara eksplisit mencatat "KEAMANAN: JANGAN cetak password plaintext ke log" — menunjukkan
 * kesadaran tim terhadap risiko ini, namun mitigasi yang diterapkan hanya membatasi agar password
 * hasil dekripsi tidak tercetak ke log, TIDAK mengubah skema penyimpanan menjadi hash satu-arah.
 * Untuk jalur {@link BiodataCalonMahasiswa} (calon mahasiswa), password bahkan tidak didekripsi
 * sama sekali — nilainya langsung diambil dari {@code getNoRegistrasi()} (nomor registrasi calon
 * mahasiswa), yang berarti password akun ini pada dasarnya adalah <b>identifier yang dapat ditebak/
 * diketahui pihak lain</b>, bukan rahasia yang dipilih pengguna. Sesuai instruksi tugas dokumentasi
 * ini, seluruh temuan tersebut TIDAK diubah di sini — dilaporkan agar dapat ditindaklanjuti terpisah
 * oleh tim yang berwenang (migrasi ke skema hash satu-arah memerlukan strategi reset password massal
 * karena password lama tidak dapat "di-hash mundur" dari bentuk terenkripsi tanpa didekripsi lebih
 * dahulu).
 * </p>
 */
@SuppressWarnings("deprecation")
@Service("userDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {

	/**
	 * Implementasi kontrak {@link UserDetailsService#loadUserByUsername(String)}; seluruh logika
	 * didelegasikan ke {@link #getUserDetails(String)} (dipertahankan sebagai method statis
	 * terpisah agar dapat dipanggil langsung dari kode lain tanpa melalui instance Spring bean).
	 *
	 * @param username username yang sedang login
	 * @return {@link UserDetails} pengguna yang bersangkutan
	 * @throws UsernameNotFoundException bila {@code username} tidak ditemukan di
	 *                                    {@link SecurityFilter#dataLogin} atau entitas yang
	 *                                    tersimpan tidak dikenali
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// TODO Auto-generated method stub
		return getUserDetails(username);
	}

	/**
	 * Membangun objek {@link UserDetails} Spring Security untuk {@code username} yang sudah
	 * tervalidasi sebelumnya dan tersimpan di {@link SecurityFilter#dataLogin}. Entitas asal
	 * ditentukan lewat pengecekan tipe ({@code instanceof}) atas nilai pertama pasangan data login:
	 * {@link BiodataCalonMahasiswa} (calon mahasiswa), {@link Mahasiswa}, {@link Siswa},
	 * {@link Penduduk}, atau {@link Tbmuser} — masing-masing menghasilkan objek {@link User} dengan
	 * password (didekripsi atau, khusus calon mahasiswa, berupa nomor registrasi — lihat peringatan
	 * keamanan pada Javadoc kelas), status enabled, dan daftar {@link GrantedAuthority} yang
	 * berbeda sesuai jenis akun. Untuk {@link Mahasiswa}, terdapat percabangan tambahan: bila
	 * {@code username} cocok dengan {@code userOrtu} (akun turunan orang tua) milik mahasiswa
	 * tersebut, password yang dipakai adalah {@code passOrtu} (bukan {@code pass} milik mahasiswa
	 * sendiri).
	 *
	 * @param username username yang sedang login, dipakai sebagai kunci pencarian di
	 *                 {@link SecurityFilter#dataLogin}
	 * @return {@link UserDetails} siap pakai untuk proses autentikasi/otorisasi Spring Security
	 * @throws UsernameNotFoundException bila {@code username} tidak ditemukan di
	 *                                    {@link SecurityFilter#dataLogin}, atau nilai yang
	 *                                    tersimpan bukan salah satu dari lima tipe entitas yang
	 *                                    didukung
	 */
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