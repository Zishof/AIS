package ais.common.security;

import java.util.Date;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.model.sekolah.Siswa;

/**
 * <h3>Token akses publik "Catatan Orang Tua" per-siswa (hash tersimpan, kedaluwarsa opsional).</h3>
 *
 * <p>Menggantikan pemakaian id {@link Siswa} mentah pada endpoint publik tanpa login
 * {@code /AktiftasHarianSiswa?token=...} ({@code ais.action.servlet.CatatanOrangTuaServlet}).
 * Token 32-byte {@link java.security.SecureRandom} (via {@link PasswordHashService#tokenAcakHex})
 * dibangkitkan sekali per penerbitan; hanya SHA-256 hex-nya ({@link PasswordHashService#sha256Hex})
 * yang disimpan pada {@link Siswa#getTokenAksesCatatanOrtuHash()} &mdash; pola yang sama dengan
 * {@code ais.service.registration.EmailVerificationService}. Token MENTAH dikembalikan HANYA pada
 * saat penerbitan ({@link #terbitkanToken}) untuk langsung dirakit ke tautan yang dibagikan ke
 * orang tua/wali; tidak ada cara mengambilnya kembali dari basis data setelah itu &mdash; bila
 * tautan hilang/perlu dicabut, terbitkan ulang (token lama otomatis tidak berlaku lagi).</p>
 *
 * <p><b>Tidak berkaitan dengan dua mekanisme kredensial siswa lain yang sudah ada:</b>
 * {@link Siswa#getToken()} (token sesi/autentikasi mobile, sudah terdokumentasi rentan IDOR di
 * beberapa endpoint API) dan {@link Siswa#urlLogin()} (magic-link DES deterministik dengan
 * passphrase tetap, sudah terdokumentasi dapat dipalsukan). Ketiganya independen; jangan
 * disatukan.</p>
 */
public final class SiswaCatatanOrtuTokenService {

	/** Panjang token mentah dalam byte sebelum di-hex-kan (256-bit). */
	private static final int PANJANG_BYTE_TOKEN = 32;

	/** Batas panjang teks token yang diterima dari request publik, mencegah input berlebihan. */
	private static final int PANJANG_MAKS_INPUT = 128;

	private SiswaCatatanOrtuTokenService() {
	}

	/**
	 * Masa berlaku token dalam hari sejak diterbitkan; {@code 0} atau negatif berarti token TIDAK
	 * PERNAH kedaluwarsa (nilai bawaan). Dapat diatur lewat Konfigurasi
	 * {@code catatan_ortu_token_masa_berlaku_hari}.
	 *
	 * @return masa berlaku dalam hari, atau {@code 0} bila tidak dikonfigurasi/tidak valid
	 */
	public static int masaBerlakuHari() {
		try {
			return Integer.parseInt(Common.getKonfigurasi("catatan_ortu_token_masa_berlaku_hari", "0").getNilai().trim());
		} catch (Exception e) {
			return 0;
		}
	}

	/**
	 * Terbitkan token akses baru untuk {@code siswa}, MENGGANTIKAN token sebelumnya (bila ada) --
	 * tautan lama otomatis tidak berlaku lagi begitu method ini dipanggil. Perubahan disimpan DI
	 * DALAM transaction/session pemanggil (tidak membuka atau meng-commit transaction sendiri).
	 *
	 * @param session sesi Hibernate aktif milik pemanggil
	 * @param siswa   siswa yang akan diterbitkan tokennya; tidak boleh {@code null}
	 * @return token MENTAH (hex, 64 karakter) untuk langsung dirakit ke tautan; TIDAK disimpan di
	 *         mana pun -- pemanggil bertanggung jawab menyampaikannya (mis. ke kanal WA) sebelum
	 *         nilai ini hilang dari memori
	 * @throws IllegalArgumentException bila {@code siswa} {@code null}
	 */
	public static String terbitkanToken(Session session, Siswa siswa) {
		if (siswa == null) {
			throw new IllegalArgumentException("siswa tidak boleh null");
		}
		String tokenMentah = PasswordHashService.tokenAcakHex(PANJANG_BYTE_TOKEN);
		siswa.setTokenAksesCatatanOrtuHash(PasswordHashService.sha256Hex(tokenMentah));
		siswa.setTokenAksesCatatanOrtuDibuat(new Date());
		session.saveOrUpdate(siswa);
		return tokenMentah;
	}

	/**
	 * Cari {@link Siswa} pemilik token akses "Catatan Orang Tua" yang diberikan (dicocokkan lewat
	 * hash-nya, bukan token mentah dibandingkan langsung), dan tegakkan kedaluwarsa opsional
	 * ({@link #masaBerlakuHari()}). Dipakai satu-satunya oleh
	 * {@code ais.action.servlet.CatatanOrangTuaServlet} SEBELUM data siswa mana pun ditampilkan.
	 *
	 * @param session     sesi Hibernate aktif milik pemanggil
	 * @param tokenMentah token mentah dari parameter request publik; boleh {@code null}/kosong
	 * @return siswa pemilik token yang cocok dan belum kedaluwarsa, atau {@code null} bila token
	 *         kosong, berformat tidak valid, tidak ditemukan, atau sudah kedaluwarsa
	 */
	public static Siswa cariSiswaByToken(Session session, String tokenMentah) {
		if (tokenMentah == null) {
			return null;
		}
		String t = tokenMentah.trim();
		if (t.isEmpty() || t.length() > PANJANG_MAKS_INPUT || !t.matches("[0-9a-fA-F]+")) {
			return null;
		}

		Siswa siswa = (Siswa) session.createCriteria(Siswa.class)
				.add(Restrictions.eq("tokenAksesCatatanOrtuHash", PasswordHashService.sha256Hex(t)))
				.setMaxResults(1).uniqueResult();
		if (siswa == null) {
			return null;
		}

		int masaBerlakuHari = masaBerlakuHari();
		if (masaBerlakuHari > 0 && siswa.getTokenAksesCatatanOrtuDibuat() != null) {
			long batasMs = siswa.getTokenAksesCatatanOrtuDibuat().getTime() + masaBerlakuHari * 24L * 3600L * 1000L;
			if (System.currentTimeMillis() > batasMs) {
				return null;
			}
		}
		return siswa;
	}
}
