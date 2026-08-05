package ais.action.master.helper.util;

import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;

/**
 * Gerbang (gate) pembayaran SEMESTER PENDEK (SP): mahasiswa yang mengambil mata kuliah SP tidak boleh
 * di-ABSEN atau di-ENTRY NILAI sebelum melunasi pembayaran SP. Util ini menyediakan satu titik keputusan
 * agar aturan sama dipakai di alur absensi maupun penilaian.
 *
 * <p><b>Aman &amp; opt-in.</b> Penentuan lunas memakai kembali {@link Common#checkStatusPembayaranMahasiswa}
 * dengan {@code sp=true}, yang otomatis mengembalikan "boleh" bila konfigurasi
 * {@code mahasiswa_harus_bayar_sebelum_isi_krs_sp} TIDAK aktif, item biaya belum diset, atau mahasiswa
 * masuk daftar pengecualian ({@code BaypassPembayaranMahasiswa}). Jadi bila institusi belum mengaktifkan
 * gerbang ini, TIDAK ada yang diblok. Selain itu, hanya perkuliahan berstatus SP yang diperiksa; mata
 * kuliah reguler selalu lolos. Bila terjadi error, util mengembalikan "boleh" (fail-open) agar tidak
 * menghalangi operasi akademik secara tak sengaja.</p>
 *
 * <p>Method mengembalikan <b>alasan blokir</b> berupa {@code String} (untuk ditampilkan ke pengguna), atau
 * {@code null} bila operasi BOLEH dilanjutkan.</p>
 */
public final class GateBayarSpUtil {

	private GateBayarSpUtil() {
	}

	private static boolean isSp(Perkuliahan perkuliahan) {
		return perkuliahan != null && perkuliahan.getStatusSemesterPendek() != null
				&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK);
	}

	/**
	 * Apakah gerbang bayar-SP AKTIF (konfigurasi in-memory, TANPA query DB). Dipakai sebagai pintu murah
	 * agar bila gerbang dimatikan admin, util keluar SEBELUM query Detailperkuliahan/pembayaran apa pun.
	 * Default mengikuti nilai konfigurasi {@code mahasiswa_harus_bayar_sebelum_isi_krs_sp}.
	 */
	private static boolean gerbangSpAktif() {
		try {
			return Common.bolehKonfigurasi("mahasiswa_harus_bayar_sebelum_isi_krs_sp");
		} catch (Exception e) {
			return true; // bila ragu, serahkan keputusan ke checkStatusPembayaranMahasiswa.
		}
	}

	private static String pesan(Mahasiswa mahasiswa, Integer semester) {
		return "Mahasiswa \"" + (mahasiswa == null ? "" : mahasiswa.toString())
				+ "\" belum melunasi pembayaran Semester Pendek (SP)"
				+ (semester == null ? "" : " untuk semester " + semester)
				+ ". Absensi dan entry nilai tidak dapat dilakukan sebelum pembayaran SP lunas.";
	}

	/**
	 * Alasan blokir untuk operasi pada {@link Detailperkuliahan} (dipakai di ENTRY NILAI). Mengembalikan
	 * {@code null} bila boleh (bukan SP, sudah lunas/bypass, gerbang non-aktif, atau data tak lengkap).
	 */
	public static String alasanBlokir(Detailperkuliahan detailperkuliahan) {
		try {
			if (detailperkuliahan == null) {
				return null;
			}
			Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
			if (!isSp(perkuliahan)) {
				return null; // bukan SP -> tanpa query, boleh.
			}
			if (!gerbangSpAktif()) {
				return null; // gerbang non-aktif -> tanpa query pembayaran, boleh.
			}
			Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
			if (mahasiswa == null) {
				return null;
			}
			boolean lunas = Common.checkStatusPembayaranMahasiswa(detailperkuliahan.getSemester(),
					detailperkuliahan.getTahap(), mahasiswa, false, true);
			return lunas ? null : pesan(mahasiswa, detailperkuliahan.getSemester());
		} catch (Exception e) {
			return null; // fail-open: jangan blok bila terjadi error
		}
	}

	/**
	 * Alasan blokir untuk operasi pada pasangan (Perkuliahan, Mahasiswa) (dipakai di ABSENSI). Mencari
	 * {@link Detailperkuliahan} mahasiswa pada perkuliahan tsb untuk memperoleh semester/tahap, lalu
	 * mendelegasikan ke {@link #alasanBlokir(Detailperkuliahan)}. Mengembalikan {@code null} bila boleh.
	 */
	public static String alasanBlokir(Perkuliahan perkuliahan, Mahasiswa mahasiswa) {
		try {
			if (!isSp(perkuliahan) || mahasiswa == null) {
				return null;
			}
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) HibernateUtil.currentSession()
					.createCriteria(Detailperkuliahan.class).add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("perkuliahan", perkuliahan)).setMaxResults(1).uniqueResult();
			return detailperkuliahan == null ? null : alasanBlokir(detailperkuliahan);
		} catch (Exception e) {
			return null;
		}
	}

	/** Sama seperti {@link #alasanBlokir(Perkuliahan, Mahasiswa)} tetapi menerima ID mahasiswa (dipakai di absensi). */
	public static String alasanBlokir(Perkuliahan perkuliahan, Long mahasiswaId) {
		try {
			if (!isSp(perkuliahan) || mahasiswaId == null) {
				return null; // bukan SP -> tanpa query, boleh.
			}
			if (!gerbangSpAktif()) {
				return null; // gerbang non-aktif -> TANPA query Detailperkuliahan, boleh.
			}
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) HibernateUtil.currentSession()
					.createCriteria(Detailperkuliahan.class).createAlias("mahasiswa", "m")
					.add(Restrictions.eq("m.id", mahasiswaId)).add(Restrictions.eq("perkuliahan", perkuliahan))
					.setMaxResults(1).uniqueResult();
			return detailperkuliahan == null ? null : alasanBlokir(detailperkuliahan);
		} catch (Exception e) {
			return null;
		}
	}
}
