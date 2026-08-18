package ais.action.master.feeder.util;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;

/**
 * {@code FeederUtil} — kumpulan utilitas pencarian &amp; penyalinan entitas berbasis <b>kode Feeder</b>
 * yang dipakai bersama oleh seluruh importer/exporter Neo Feeder. Tujuannya memusatkan dua pola
 * berulang di modul feeder sehingga tidak diduplikasi di banyak tempat (maksimalkan reuse):
 *
 * <ol>
 *   <li><b>Resolusi entitas lokal dari kode Feeder.</b> Saat mengimpor data dari Feeder, tiap
 *       record membawa identifier Feeder (mis. {@code id_matkul}, {@code id_prodi},
 *       {@code id_kelas_kuliah}). Aplikasi perlu menautkannya ke entitas lokal yang sudah punya
 *       kolom pemetaan. Dua skema pemetaan didukung:
 *       <ul>
 *         <li>Kolom tunggal {@code feeder} (satu entitas = satu kode Feeder) — lihat
 *             {@link #getDataByFeeder(Session, Object, Class)} dan overload ber-{@link Criterion}.</li>
 *         <li>Kolom gabungan {@code feeders} (satu entitas dapat memetakan beberapa kode Feeder yang
 *             dipisah, dicocokkan secara {@code ilike ANYWHERE}) — lihat
 *             {@link #getDataByFeeders(Session, String, Class)} dan overload ber-{@link Criterion}.</li>
 *       </ul>
 *       Semua varian mengembalikan objek ter-cache (lewat {@link ConstantValues#simpleObject}) sehingga
 *       hemat query berulang dan konsisten dengan L1/L2 cache aplikasi.</li>
 *   <li><b>Penyalinan nilai "isi bila kosong".</b> {@link #copyDataJikaKosong} menyalin properti dari
 *       entitas sumber ke entitas tujuan hanya untuk properti yang masih kosong/nol — berguna saat
 *       memperkaya entitas lokal dengan data Feeder tanpa menimpa data yang sudah diisi manual.</li>
 * </ol>
 *
 * <h3>Manajemen session</h3>
 * Method pencarian di kelas ini <b>menerima {@link Session} dari pemanggil</b> dan tidak pernah
 * membuka maupun menutup session sendiri — sehingga tidak ada resource yang perlu ditutup di sini.
 * Tanggung jawab siklus hidup session tetap pada pemanggil (yang membuka via
 * {@code openSession()}/{@code currentNativeSession()} wajib menutup di {@code finally};
 * {@code currentSession()} tidak boleh ditutup manual).
 *
 * <h3>Kompatibilitas</h3>
 * Ditulis agar kompatibel dengan <b>Java 1.7</b>.
 *
 * @author Tim AIS
 */
public class FeederUtil {

	/** Kelas utilitas statis — tidak untuk diinstansiasi. */
	private FeederUtil() {
	}

	/**
	 * Inti bersama pencarian entitas: membangun {@link Criteria} dari kriteria dasar (wajib) dan
	 * kriteria tambahan (opsional), mengambil <b>satu</b> hasil teratas, lalu mengembalikan objek
	 * ter-cache via {@link ConstantValues#simpleObject}. Menjadi tumpuan seluruh overload publik
	 * agar logika query tidak diduplikasi.
	 *
	 * @param session session Hibernate aktif milik pemanggil
	 * @param clazz   kelas entitas target
	 * @param base    kriteria dasar (mis. {@code eq("feeder", ...)} atau {@code ilike("feeders", ...)})
	 * @param extra   kriteria tambahan opsional; boleh {@code null}
	 * @param <T>     tipe entitas
	 * @return entitas yang cocok, atau {@code null} bila tak ada
	 */
	@SuppressWarnings("unchecked")
	private static <T> T cariSatu(Session session, Class<T> clazz, Criterion base, Criterion extra) {
		Criteria criteria = session.createCriteria(clazz).add(base);
		if (extra != null) {
			criteria.add(extra);
		}
		return (T) ConstantValues.simpleObject(criteria.setMaxResults(1), clazz);
	}

	/**
	 * Mencari entitas lokal berdasarkan kolom tunggal {@code feeder} dengan kriteria tambahan.
	 *
	 * @param session   session Hibernate aktif
	 * @param feeder    nilai kode Feeder yang dicocokkan persis pada kolom {@code feeder}
	 * @param clazz     kelas entitas target
	 * @param criterion kriteria tambahan (mis. filter {@code aktif})
	 * @param <T>       tipe entitas
	 * @return entitas yang cocok, atau {@code null}
	 */
	public static <T> T getDataByFeeder(Session session, Object feeder, Class<T> clazz, Criterion criterion) {
		return cariSatu(session, clazz, Restrictions.eq("feeder", feeder), criterion);
	}

	/**
	 * Mencari entitas lokal berdasarkan kolom tunggal {@code feeder}.
	 *
	 * @param session session Hibernate aktif
	 * @param feeder  nilai kode Feeder yang dicocokkan persis pada kolom {@code feeder}
	 * @param clazz   kelas entitas target
	 * @param <T>     tipe entitas
	 * @return entitas yang cocok, atau {@code null}
	 */
	public static <T> T getDataByFeeder(Session session, Object feeder, Class<T> clazz) {
		return cariSatu(session, clazz, Restrictions.eq("feeder", feeder), null);
	}

	/**
	 * Mencari entitas lokal berdasarkan kolom gabungan {@code feeders} (cocok {@code ilike ANYWHERE}).
	 *
	 * @param session session Hibernate aktif
	 * @param feeder  potongan kode Feeder yang dicari di dalam kolom {@code feeders}
	 * @param clazz   kelas entitas target
	 * @param <T>     tipe entitas
	 * @return entitas yang cocok, atau {@code null}
	 */
	public static <T> T getDataByFeeders(Session session, String feeder, Class<T> clazz) {
		return cariSatu(session, clazz, Restrictions.ilike("feeders", feeder, MatchMode.ANYWHERE), null);
	}

	/**
	 * Mencari entitas lokal berdasarkan kolom gabungan {@code feeders} (cocok {@code ilike ANYWHERE})
	 * dengan kriteria tambahan.
	 *
	 * @param session   session Hibernate aktif
	 * @param feeder    potongan kode Feeder yang dicari di dalam kolom {@code feeders}
	 * @param clazz     kelas entitas target
	 * @param criterion kriteria tambahan
	 * @param <T>       tipe entitas
	 * @return entitas yang cocok, atau {@code null}
	 */
	public static <T> T getDataByFeeders(Session session, String feeder, Class<T> clazz, Criterion criterion) {
		return cariSatu(session, clazz, Restrictions.ilike("feeders", feeder, MatchMode.ANYWHERE), criterion);
	}

	/**
	 * Menyalin properti dari {@code copyFrom} ke {@code copyTo} hanya untuk properti yang masih
	 * dianggap "kosong" pada {@code copyTo}. Lihat {@link #copyDataJikaKosong(Object, Object, Class, org.hibernate.criterion.Criterion)}.
	 *
	 * @param copyFrom entitas sumber
	 * @param copyTo   entitas tujuan
	 * @param clazz    kelas kedua entitas
	 * @param <T>      tipe entitas
	 * @return {@code copyTo} setelah diperkaya
	 */
	public static <T> T copyDataJikaKosong(T copyFrom, T copyTo, Class<T> clazz) {
		return copyDataJikaKosong(copyFrom, copyTo, clazz, null);
	}

	/**
	 * Menyalin properti dari {@code copyFrom} ke {@code copyTo} hanya untuk properti yang masih
	 * "kosong" pada {@code copyTo} — yaitu {@code null}, {@link String} kosong (via
	 * {@code Common.checkIsStringNull}), {@link Double} {@code < 0.01}, atau {@link Integer}
	 * {@code < 1}. Berguna untuk "isi dari Feeder tanpa menimpa data manual". Setiap akses properti
	 * dibungkus try/catch agar properti yang tidak kompatibel tidak menggagalkan keseluruhan proses.
	 *
	 * @param copyFrom  entitas sumber
	 * @param copyTo    entitas tujuan
	 * @param clazz     kelas kedua entitas
	 * @param criterion parameter dipertahankan untuk kompatibilitas tanda tangan; tidak dipakai
	 * @param <T>       tipe entitas
	 * @return {@code copyTo} setelah diperkaya
	 */
	public static <T> T copyDataJikaKosong(T copyFrom, T copyTo, Class<T> clazz, Criterion criterion) {
		ClassMetadata classMetadata = HibernateUtil.getClassMetadata(clazz);
		String[] properties = classMetadata.getPropertyNames();
		for (String p : properties) {
			Object dataLama = null;
			try {
				dataLama = classMetadata.getPropertyValue(copyTo, p, EntityMode.POJO);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederUtil.java:169");
			}
			Object dataBaru = null;
			try {
				dataBaru = classMetadata.getPropertyValue(copyFrom, p, EntityMode.POJO);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederUtil.java:174");
			}
			try {
				if (dataLama == null || (dataLama instanceof String && Common.checkIsStringNull(dataLama))
						|| (dataLama instanceof Double && ((Double) dataLama) < 0.01)
						|| (dataLama instanceof Integer && ((Integer) dataLama) < 1)) {
					classMetadata.setPropertyValue(copyTo, p, dataBaru, EntityMode.POJO);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederUtil.java:182");
			}
		}
		return copyTo;
	}
}
