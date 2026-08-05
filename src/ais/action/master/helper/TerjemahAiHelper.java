package ais.action.master.helper;

import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.KamusTerjemah;

/**
 * Cache <b>pembelajaran terjemahan</b> yang persisten untuk hasil AI (Ollama).
 *
 * <p>Ketika sebuah konten (mis. panduan) diterjemahkan oleh AI, hasilnya disimpan ke tabel
 * {@link KamusTerjemah} dengan kunci = MD5 teks sumber + kode bahasa. Dengan begitu:
 * (1) terjemahan yang sama tidak perlu memanggil AI lagi (hemat & instan), (2) hasil tetap
 * ada setelah aplikasi di-restart, dan (3) "kamus" terjemahan tumbuh seiring pemakaian.</p>
 *
 * <p>Bila konten sumber berubah (mis. panduan disunting), MD5 ikut berubah sehingga cache lama
 * otomatis tidak terpakai (tak perlu invalidasi manual).</p>
 */
public final class TerjemahAiHelper {

	private TerjemahAiHelper() {
	}

	private static final ConcurrentHashMap<String, String> MEM = new ConcurrentHashMap<String, String>();
	private static final int MEM_MAKS = 20000;

	/** Ambil terjemahan tersimpan untuk {@code sumber} pada bahasa {@code lang}; null bila belum ada. */
	public static String ambilDoc(String sumber, String lang) {
		if (sumber == null || sumber.length() == 0) {
			return null;
		}
		String kode = normalLang(lang);
		String hash = md5(sumber);
		String key = kode + "|" + hash;

		String m = MEM.get(key);
		if (m != null) {
			return m;
		}
		Session s = null;
		try {
			s = HibernateUtil.openSession();
			List<?> l = s.createCriteria(KamusTerjemah.class)
					.add(Restrictions.eq("sumberHash", hash))
					.add(Restrictions.eq("lang", kode))
					.setMaxResults(1)
					.list();
			if (l != null && !l.isEmpty()) {
				String hasil = ((KamusTerjemah) l.get(0)).getHasil();
				memPut(key, hasil);
				return hasil;
			}
		} catch (Throwable t) {
			// abaikan → anggap belum ada
		} finally {
			HibernateUtil.closeSessionQuietly(s);
		}
		return null;
	}

	/** Simpan/pelajari terjemahan AI untuk {@code sumber} → {@code hasil} pada bahasa {@code lang}. */
	public static void simpanDoc(String sumber, String lang, String hasil) {
		if (sumber == null || sumber.length() == 0 || hasil == null || hasil.trim().length() == 0) {
			return;
		}
		String kode = normalLang(lang);
		String hash = md5(sumber);
		Session s = null;
		Transaction tx = null;
		try {
			s = HibernateUtil.openSession();
			tx = s.beginTransaction();
			List<?> l = s.createCriteria(KamusTerjemah.class)
					.add(Restrictions.eq("sumberHash", hash))
					.add(Restrictions.eq("lang", kode))
					.setMaxResults(1)
					.list();
			KamusTerjemah kt = (l != null && !l.isEmpty()) ? (KamusTerjemah) l.get(0) : new KamusTerjemah();
			if (kt.getId() == null) {
				kt.setSumberHash(hash);
				kt.setLang(kode);
			}
			// Simpan cuplikan sumber (untuk keterbacaan/telusur) — cukup awalannya bila sangat panjang.
			kt.setSumber(sumber.length() > 4000 ? sumber.substring(0, 4000) : sumber);
			kt.setHasil(hasil);
			kt.setWaktu(new Date());
			s.saveOrUpdate(kt);
			tx.commit();
			memPut(kode + "|" + hash, hasil);
		} catch (Throwable t) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Throwable ignore) {
				}
			}
		} finally {
			HibernateUtil.closeSessionQuietly(s);
		}
	}

	// ---------------- util ----------------

	private static void memPut(String key, String val) {
		try {
			if (val != null && MEM.size() < MEM_MAKS) {
				MEM.put(key, val);
			}
		} catch (Throwable ignore) {
		}
	}

	private static String normalLang(String lang) {
		String tl = lang == null ? "" : lang.trim().toLowerCase();
		if (tl.startsWith("ar") || tl.contains("arab")) {
			return "arab";
		}
		if (tl.startsWith("zh") || tl.contains("mandarin") || tl.contains("china") || tl.contains("chinese")) {
			return "mandarin";
		}
		return "english";
	}

	private static String md5(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] d = md.digest(s.getBytes("UTF-8"));
			StringBuilder sb = new StringBuilder(32);
			for (int i = 0; i < d.length; i++) {
				int v = d[i] & 0xff;
				if (v < 16) {
					sb.append('0');
				}
				sb.append(Integer.toHexString(v));
			}
			return sb.toString();
		} catch (Throwable t) {
			return Integer.toHexString(s.hashCode());
		}
	}
}
