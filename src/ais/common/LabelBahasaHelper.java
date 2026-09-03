package ais.common;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.LabelBahasa;

/**
 * {@code LabelBahasaHelper} — lapisan data untuk <b>menyunting label/teks multi-bahasa</b>
 * langsung dari layar, tanpa perlu masuk ke menu Konfigurasi atau redeploy.
 *
 * <h3>Untuk apa</h3>
 * <p>Setiap teks yang dilewatkan {@link Common#getBahasaConfig(String)} — termasuk SELURUH
 * pesan alert aplikasi ini — disimpan pada satu baris {@link LabelBahasa} dengan kolom per
 * bahasa: {@code indonesia}, {@code english}, {@code arab}, {@code mandarin}. Kelas ini
 * membaca dan menulis baris tersebut, sehingga administrator dapat memperbaiki kalimat yang
 * janggal atau salah terjemah tepat di tempat kalimat itu muncul.</p>
 *
 * <h3>Kenapa terpisah dari CommonComboLanguageHelper</h3>
 * <p>Kelas itu berisi jalur baca yang dipanggil ribuan kali per render dan punya worker
 * terjemah latar sendiri. Penyuntingan manual oleh admin adalah operasi yang jarang, sinkron,
 * dan menulis SEMUA bahasa sekaligus — perilakunya berbeda, jadi dipisahkan agar jalur baca
 * yang panas tidak ikut berubah.</p>
 *
 * <h3>Perbedaan penting dengan terjemah otomatis latar</h3>
 * <p>Worker latar sengaja hanya mengisi kolom yang MASIH KOSONG (atau masih sama dengan teks
 * Indonesia) supaya tidak menimpa hasil kerja manusia. Kelas ini sebaliknya: yang diketik
 * administrator adalah kebenaran terakhir, jadi kolomnya ditimpa apa adanya.</p>
 *
 * <p>Kompatibilitas: Java 1.6 (tanpa lambda, diamond, try-with-resources, atau Stream).</p>
 */
public final class LabelBahasaHelper {

	private LabelBahasaHelper() {
	}

	/** Urutan tetap isi array pada {@link #ambilTerjemahan(String)}. */
	public static final int INDONESIA = 0;
	public static final int ENGLISH = 1;
	public static final int ARAB = 2;
	public static final int MANDARIN = 3;

	/**
	 * Kunci kamus untuk sebuah teks default — sama persis dengan yang dipakai jalur baca,
	 * sehingga hasil suntingan langsung terpakai oleh {@link Common#getBahasaConfig(String)}.
	 */
	public static String kunci(String teksDefault) {
		return CommonComboLanguageHelper.kunciBahasa(teksDefault);
	}

	/**
	 * Ambil seluruh terjemahan untuk satu kunci.
	 *
	 * @return array 4 elemen berurutan {@link #INDONESIA}, {@link #ENGLISH}, {@link #ARAB},
	 *         {@link #MANDARIN}; elemen yang belum terisi bernilai string kosong. Tidak
	 *         pernah {@code null}.
	 */
	public static String[] ambilTerjemahan(String kunci) {
		String[] hasil = new String[] { "", "", "", "" };
		if (kunci == null || kunci.trim().length() == 0) {
			return hasil;
		}
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			LabelBahasa label = (LabelBahasa) session
					.createQuery("from ais.database.model.LabelBahasa where nama = :n")
					.setString("n", kunci.trim()).setMaxResults(1).uniqueResult();
			if (label != null) {
				hasil[INDONESIA] = kosongKeString(label.getIndonesia());
				hasil[ENGLISH] = kosongKeString(label.getEnglish());
				hasil[ARAB] = kosongKeString(label.getArab());
				hasil[MANDARIN] = kosongKeString(label.getMandarin());
			}
		} catch (Throwable t) {
			ErrorAuditUtil.record(t, "LabelBahasaHelper.ambilTerjemahan kunci=" + kunci);
		} finally {
			tutupSesi(session);
		}
		return hasil;
	}

	/**
	 * Simpan terjemahan hasil suntingan administrator dan segarkan cache memori supaya
	 * perubahannya langsung terlihat tanpa restart.
	 *
	 * <p>Baris dibuat bila belum ada. Kolom yang dikirim kosong TIDAK menghapus isi lama —
	 * mengosongkan satu bahasa hampir selalu tidak disengaja, dan akibatnya teks bahasa itu
	 * jatuh kembali ke bahasa Indonesia di seluruh aplikasi.</p>
	 *
	 * @return {@code true} bila tersimpan
	 */
	public static boolean simpan(String kunci, String indonesia, String english, String arab, String mandarin) {
		if (kunci == null || kunci.trim().length() == 0) {
			return false;
		}
		String k = kunci.trim();
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			LabelBahasa label = (LabelBahasa) session
					.createQuery("from ais.database.model.LabelBahasa where nama = :n")
					.setString("n", k).setMaxResults(1).uniqueResult();
			boolean baru = false;
			if (label == null) {
				label = new LabelBahasa();
				label.setNama(k);
				baru = true;
			}
			if (adaIsi(indonesia)) {
				label.setIndonesia(indonesia.trim());
			}
			if (adaIsi(english)) {
				label.setEnglish(english.trim());
			}
			if (adaIsi(arab)) {
				label.setArab(arab.trim());
			}
			if (adaIsi(mandarin)) {
				label.setMandarin(mandarin.trim());
			}
			if (baru) {
				session.save(label);
			} else {
				session.update(label);
			}
			tx.commit();
			tx = null;
			segarkanMemori(k, indonesia, english, arab, mandarin);
			return true;
		} catch (Throwable t) {
			ErrorAuditUtil.record(t, "LabelBahasaHelper.simpan kunci=" + k);
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Throwable abaikan) {
					// transaksi sudah tidak dapat dipulihkan; kegagalan simpan sudah dicatat
				}
			}
			return false;
		} finally {
			tutupSesi(session);
		}
	}

	/**
	 * Terjemahkan otomatis dari bahasa Indonesia.
	 *
	 * <p>Memakai {@link AiTerjemah} yang sama dengan worker latar: bila server AI siap ia
	 * dipakai, bila tidak otomatis jatuh ke kamus internal. Dipanggil dari thread UI, jadi
	 * kegagalan apa pun dikembalikan sebagai string kosong — bukan exception yang menutup
	 * layar admin.</p>
	 *
	 * @param langKode {@code "english"}, {@code "arab"}, atau {@code "mandarin"}
	 */
	public static String terjemahOtomatis(String indonesia, String langKode) {
		if (indonesia == null || indonesia.trim().length() == 0) {
			return "";
		}
		try {
			String hasil = AiTerjemah.terjemah(indonesia.trim(), langKode);
			return hasil == null ? "" : hasil.trim();
		} catch (Throwable t) {
			ErrorAuditUtil.record(t, "LabelBahasaHelper.terjemahOtomatis lang=" + langKode);
			return "";
		}
	}

	// =========================================================
	// Util internal
	// =========================================================

	private static void segarkanMemori(String kunci, String indonesia, String english, String arab, String mandarin) {
		try {
			if (adaIsi(indonesia)) {
				MemoryDbUtil.getBahasaIndonesias().put(kunci, indonesia.trim());
			}
			if (adaIsi(english)) {
				MemoryDbUtil.getBahasaEnglishs().put(kunci, english.trim());
			}
			if (adaIsi(arab)) {
				MemoryDbUtil.getBahasaArabs().put(kunci, arab.trim());
			}
			if (adaIsi(mandarin)) {
				MemoryDbUtil.getBahasaMandarins().put(kunci, mandarin.trim());
			}
		} catch (Throwable t) {
			// Data sudah tersimpan di basis data; cache akan menyusul pada muat ulang berikutnya.
			ErrorAuditUtil.record(t, "LabelBahasaHelper.segarkanMemori kunci=" + kunci);
		}
	}

	/** Sesi dibuka sendiri lewat openSession(), jadi WAJIB ditutup tuntas di sini. */
	private static void tutupSesi(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				session.clear();
			}
		} catch (Throwable t) {
			ErrorAuditUtil.record(t, "LabelBahasaHelper.tutupSesi-clear");
		}
		try {
			if (session.isOpen()) {
				session.disconnect();
			}
		} catch (Throwable t) {
			ErrorAuditUtil.record(t, "LabelBahasaHelper.tutupSesi-disconnect");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Throwable t) {
			ErrorAuditUtil.record(t, "LabelBahasaHelper.tutupSesi-close");
		}
	}

	private static boolean adaIsi(String s) {
		return s != null && s.trim().length() > 0;
	}

	private static String kosongKeString(String s) {
		return s == null ? "" : s;
	}
}
