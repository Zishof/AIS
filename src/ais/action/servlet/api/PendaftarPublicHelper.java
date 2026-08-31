package ais.action.servlet.api;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pendaftar;
import ais.ui.util.WaktuUtil;

/**
 * <b>PendaftarPublicHelper</b> -- logika registrasi mandiri & login pelanggan baru
 * ebisnis.id (landing page {@code ebisnis.jsp}). SENGAJA method statis dgn sesi
 * Hibernate sendiri (POLA sama dgn {@link KantinHelper}), dipanggil dari
 * {@code EbisnisPublicServlet} (bukan logika langsung di servlet/JSP -- lihat
 * catatan arsitektur pada dokumen master prompt ebisnis.id: "Tidak ada business
 * logic di servlet/JSP").
 *
 * <h3>Kenapa TIDAK memakai jalur Tbmuser/{@code Common.desEncrypter} yang lama</h3>
 * <p>Jalur login existing (Tbmuser, Pedagang, dst.) memakai enkripsi DES yang
 * REVERSIBLE (lihat {@code CommonSecurityLoginHelper.checkLogin}) -- cocok utk
 * akun internal yang dibuat staf, tapi TIDAK ideal utk permukaan publik baru ini
 * yang siapa saja di internet boleh mendaftar sendiri. Di sini dipakai hash
 * SATU ARAH (PBKDF2WithHmacSHA256, bawaan JDK -- tanpa dependency baru) + salt
 * acak per akun, konsisten dgn semangat "jangan simpan password reversibel"
 * yang sudah lebih dulu diterapkan utk token perangkat POS ({@code PosDeviceToken}
 * hanya menyimpan hash SHA-256 token, bukan token mentah).</p>
 *
 * <h3>Kenapa field wilayah disimpan sbg teks bebas</h3>
 * <p>Permintaan awal hanya perlu Negara(default Indonesia)/Provinsi/Kota/Kecamatan
 * sbg data pendukung pendaftar -- BELUM perlu tabel referensi wilayah penuh
 * (itu pekerjaan terpisah, bisa menyusul kalau memang dibutuhkan validasi/dropdown
 * bertingkat). Menyimpan sbg teks bebas SEKARANG tidak menutup jalan migrasi ke
 * FK nanti (kolom teks tetap bisa dipetakan ke ID master wilayah belakangan).</p>
 *
 * <h3>Cakupan yang SENGAJA belum ada di sini (out of scope giliran ini)</h3>
 * <p>Verifikasi email/OTP, multi-tenant formal (tenant_id), Brand/Toko/mesin-POS,
 * modul Manajemen, Investor/bagi-hasil -- semua itu iterasi berikutnya (lihat
 * task list). Method di sini murni: (1) catat pendaftar baru ke tabel
 * {@code Pendaftar} yang SUDAH ADA, (2) izinkan pendaftar itu login kembali.</p>
 */
public class PendaftarPublicHelper {

	/** Panjang salt dalam byte SEBELUM di-hex-encode (32 byte -> 64 karakter hex). */
	private static final int PANJANG_SALT_BYTE = 32;
	private static final int PBKDF2_ITERASI = 120000;
	private static final int PBKDF2_PANJANG_KEY_BIT = 256;

	/**
	 * Tipe implementasi bersarang {@link HasilProses} milik {@link PendaftarPublicHelper}. Kelas ini memberi nama
	 * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * PendaftarPublicHelper}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan
	 * diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code boolean sukses}, {@code String
	 * pesan}, {@code Long pendaftarId}, {@code String namaBisnis}, {@code Pendaftar pendaftar}. Aturan bisnis
	 * bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see PendaftarPublicHelper
	 */
	public static class HasilProses {
		public boolean sukses;
		public String pesan;
		public Long pendaftarId;
		public String namaBisnis;
		/** Entity penuh, diisi HANYA saat sukses -- dipakai servlet utk mengisi sesi (lihat EbisnisPublicServlet). */
		public Pendaftar pendaftar;
		public HasilProses(boolean sukses, String pesan) {
			this.sukses = sukses;
			this.pesan = pesan;
		}
	}

	/**
	 * Registrasi pendaftar baru. Validasi minimal namun eksplisit (bukan asal
	 * simpan) -- nama bisnis, email, & password wajib; email TIDAK boleh sudah
	 * dipakai pendaftar lain yang sudah punya password (akun Pendaftar lama hasil
	 * onboarding staf eCampus/eSchool, yang emailnya mungkin kosong/duplikat scr
	 * historis, TIDAK diperiksa di sini -- hanya email yang SUDAH pernah dipakai
	 * mendaftar mandiri lewat jalur ini yang dicegah dobel).
	 */
	public static HasilProses daftar(String namaBisnis, String email, String telp, String password,
			String konfirmasiPassword, String negara, String provinsi, String kotaKabupaten, String kecamatan,
			String jenisBisnis, String alamat, String kontakPerson, String telpKontakPerson) {
		namaBisnis = trim(namaBisnis);
		email = trim(email);
		password = password == null ? "" : password;

		if (namaBisnis.isEmpty()) {
			return new HasilProses(false, "Nama bisnis/toko/perusahaan wajib diisi.");
		}
		if (email.isEmpty() || !email.contains("@")) {
			return new HasilProses(false, "Alamat email yang valid wajib diisi.");
		}
		if (password.length() < 6) {
			return new HasilProses(false, "Password minimal 6 karakter.");
		}
		if (!password.equals(konfirmasiPassword)) {
			return new HasilProses(false, "Konfirmasi password tidak sama.");
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Criteria cekEmail = session.createCriteria(Pendaftar.class)
					.add(Restrictions.eq("email", email))
					.add(Restrictions.isNotNull("passwordHash"));
			if (cekEmail.uniqueResult() != null) {
				return new HasilProses(false, "Email ini sudah terdaftar. Silakan masuk (login) langsung.");
			}

			String[] hashSalt = hashPassword(password);

			Pendaftar p = new Pendaftar();
			p.setNama(namaBisnis);
			p.setKode(buatSlugUnik(session, namaBisnis));
			p.setDomain(p.getKode());
			p.setEmail(email);
			p.setTelp(trim(telp));
			p.setAlamat(trim(alamat));
			p.setKontakperson(trim(kontakPerson));
			p.setTelpkontakperson(trim(telpKontakPerson));
			p.setNegara(negara == null || trim(negara).isEmpty() ? "Indonesia" : trim(negara));
			p.setProvinsi(trim(provinsi));
			p.setKotaKabupaten(trim(kotaKabupaten));
			p.setKecamatan(trim(kecamatan));
			p.setJenisBisnis(trim(jenisBisnis));
			p.setPasswordHash(hashSalt[0]);
			p.setPasswordSalt(hashSalt[1]);
			p.setDibuatPada(WaktuUtil.getDate());
			p.setAktif(true);
			// Pendaftar existing default-nya dianggap Sekolah bila field ini null
			// (lihat Pendaftar.getMerupakanSekolah()) -- pendaftar ebisnis.id BUKAN
			// institusi pendidikan, jadi WAJIB di-set eksplisit false di sini supaya
			// tidak salah muncul di layar/laporan yang menyaring berdasar flag ini.
			p.setMerupakanSekolah(false);

			session.beginTransaction();
			session.save(p);
			session.getTransaction().commit();

			HasilProses hasil = new HasilProses(true, "Pendaftaran berhasil. Selamat datang, " + namaBisnis + "!");
			hasil.pendaftarId = p.getId();
			hasil.namaBisnis = p.getNama();
			hasil.pendaftar = p;
			return hasil;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PendaftarPublicHelper.daftar");
			return new HasilProses(false, "Terjadi kesalahan pada sistem. Silakan coba lagi.");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Login pendaftar yang sudah mendaftar mandiri (punya {@code passwordHash}).
	 * Pendaftar LAMA hasil onboarding staf (belum pernah set password lewat jalur
	 * ini) akan selalu gagal login di sini -- itu memang benar, mereka belum
	 * "mengaktifkan" akun mandirinya.
	 */
	public static HasilProses login(String email, String password) {
		email = trim(email);
		if (email.isEmpty() || password == null || password.isEmpty()) {
			return new HasilProses(false, "Email dan password wajib diisi.");
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Pendaftar p = (Pendaftar) session.createCriteria(Pendaftar.class)
					.add(Restrictions.eq("email", email))
					.add(Restrictions.isNotNull("passwordHash"))
					.setMaxResults(1).uniqueResult();
			if (p == null || !cocokkanPassword(password, p.getPasswordHash(), p.getPasswordSalt())) {
				return new HasilProses(false, "Email atau password salah.");
			}
			if (!Boolean.TRUE.equals(p.getAktif())) {
				return new HasilProses(false, "Akun ini sedang tidak aktif. Hubungi dukungan pelanggan.");
			}

			// Catat lastLoginAt pada extension profile (bila akun jalur pendaftaran tenant) --
			// best-effort: kegagalan pencatatan TIDAK boleh menggagalkan login.
			try {
				ais.database.model.tenant.PendaftarTenantProfile profile =
						(ais.database.model.tenant.PendaftarTenantProfile) session
								.createCriteria(ais.database.model.tenant.PendaftarTenantProfile.class)
								.add(Restrictions.eq("pendaftar.id", p.getId()))
								.setMaxResults(1).uniqueResult();
				if (profile != null) {
					session.beginTransaction();
					profile.setLastLoginAt(WaktuUtil.getDate());
					session.saveOrUpdate(profile);
					session.getTransaction().commit();
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit PendaftarPublicHelper.login.lastLoginAt");
			}

			HasilProses hasil = new HasilProses(true, "Berhasil masuk.");
			hasil.pendaftarId = p.getId();
			hasil.namaBisnis = p.getNama();
			hasil.pendaftar = p;
			return hasil;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PendaftarPublicHelper.login");
			return new HasilProses(false, "Terjadi kesalahan pada sistem. Silakan coba lagi.");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static String trim(String s) {
		return s == null ? "" : s.trim();
	}

	/**
	 * Slug URL-safe dari nama bisnis (huruf kecil, spasi->dash, non-alfanumerik
	 * dibuang) + suffix angka bila sudah dipakai -- WAJIB unik krn dipakai juga
	 * sbg {@code domain} (kolom existing {@code unique=true, nullable=false}
	 * pada {@link Pendaftar}).
	 */
	private static String buatSlugUnik(Session session, String namaBisnis) {
		String basis = namaBisnis.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
		if (basis.isEmpty()) {
			basis = "bisnis";
		}
		if (basis.length() > 40) {
			basis = basis.substring(0, 40);
		}
		String kandidat = basis;
		int suffix = 1;
		while (session.createCriteria(Pendaftar.class).add(Restrictions.eq("domain", kandidat))
				.setMaxResults(1).uniqueResult() != null) {
			suffix++;
			kandidat = basis + "-" + suffix;
		}
		return kandidat;
	}

	private static String[] hashPassword(String password) {
		byte[] salt = new byte[PANJANG_SALT_BYTE];
		new SecureRandom().nextBytes(salt);
		String hashHex = pbkdf2(password, salt);
		return new String[] { hashHex, byteToHex(salt) };
	}

	private static boolean cocokkanPassword(String password, String hashHexTersimpan, String saltHexTersimpan) {
		if (hashHexTersimpan == null || saltHexTersimpan == null) {
			return false;
		}
		byte[] salt = hexToByte(saltHexTersimpan);
		String hashHex = pbkdf2(password, salt);
		// Constant-time (§12.3 program pendaftaran tenant) -- semantik identik equalsIgnoreCase
		// lama (kedua sisi hex lowercase-kan dulu), hanya menutup timing side-channel.
		return java.security.MessageDigest.isEqual(hashHex.toLowerCase().getBytes(),
				hashHexTersimpan.toLowerCase().getBytes());
	}

	private static String pbkdf2(String password, byte[] salt) {
		try {
			PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERASI, PBKDF2_PANJANG_KEY_BIT);
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			byte[] hash = skf.generateSecret(spec).getEncoded();
			return byteToHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Gagal menghitung hash password", e);
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException("Gagal menghitung hash password", e);
		}
	}

	private static String byteToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b & 0xff));
		}
		return sb.toString();
	}

	private static byte[] hexToByte(String hex) {
		int len = hex.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
		}
		return data;
	}
}
