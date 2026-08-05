package ais.common.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;

import ais.common.Common;

/**
 * Menghitung ringkasan seleksi penerima (Belum diproses / Diterima / Ditolak / Total) untuk
 * layar <b>Seleksi Penerima KKN, PKL, dan Beasiswa</b> secara efisien.
 *
 * <p><b>Masalah yang diselesaikan.</b> Ketiga layar itu dulu menjalankan <b>4 query COUNT
 * terpisah PER BARIS</b> (satu per status + satu total) di dalam renderer grid. Dengan 10 baris
 * per halaman itu = 40 query COUNT bolak-balik ke basis data hanya untuk mengisi satu kolom
 * "Informasi" — inilah penyebab layar terasa sangat lambat saat memuat.</p>
 *
 * <p><b>Solusi.</b> Satu query HQL <code>GROUP BY</code> untuk SELURUH baris pada halaman: hasil
 * dikelompokkan per induk (kkn/pkl/beasiswa) dan per nilai <code>terima</code>, lalu diringkas ke
 * dalam peta <code>id-induk → {belum, diterima, ditolak, total}</code>. Dari 4×N query menjadi
 * <b>1 query per halaman</b>.</p>
 *
 * <p><b>Setia pada perilaku lama.</b> Bucket dihitung dari nilai <code>terima</code> mentah
 * persis seperti kueri lama (<code>eq 0</code>/<code>1</code>/<code>2</code>): baris dengan
 * <code>terima</code> NULL atau nilai lain hanya masuk ke <b>Total</b>, tidak ke tiga bucket
 * status — sehingga angka yang ditampilkan tidak berubah, hanya jauh lebih cepat.</p>
 *
 * <p>Kompatibel Java 1.6/1.7 dan ZK 5.5: tanpa lambda, tanpa diamond operator.</p>
 */
public final class HitungSeleksiPenerimaHelper {

	private HitungSeleksiPenerimaHelper() {
	}

	/** Indeks pada {@code int[]} hasil. */
	public static final int BELUM = 0;
	public static final int DITERIMA = 1;
	public static final int DITOLAK = 2;
	public static final int TOTAL = 3;

	/**
	 * Menghitung ringkasan untuk sekumpulan induk dalam SATU query.
	 *
	 * @param session          sesi Hibernate aktif.
	 * @param entitasPendaftar nama entitas pendaftar, mis. {@code "MahasiswaDaftarKkn"}.
	 * @param propertiInduk    nama properti relasi ke induk pada entitas pendaftar, mis.
	 *                         {@code "kkn"} / {@code "pkl"} / {@code "beasiswa"}.
	 * @param idInduk          daftar id induk yang tampil pada halaman.
	 * @return peta {@code id-induk → int[]{belum, diterima, ditolak, total}}. Induk tanpa
	 *         pendaftar tidak muncul di peta (pemanggil memperlakukannya sebagai nol).
	 */
	public static Map<Long, int[]> hitung(Session session, String entitasPendaftar, String propertiInduk,
			Collection<Long> idInduk) {
		Map<Long, int[]> hasil = new HashMap<Long, int[]>();
		if (idInduk == null || idInduk.isEmpty()) {
			return hasil;
		}
		try {
			String hql = "select induk.id, p.terima, count(p.id) from " + entitasPendaftar + " p join p."
					+ propertiInduk + " induk where induk.id in (:ids) group by induk.id, p.terima";
			List<?> baris = session.createQuery(hql).setParameterList("ids", idInduk).list();
			for (int b = 0; b < baris.size(); b++) {
				Object[] r = (Object[]) baris.get(b);
				if (r[0] == null) {
					continue;
				}
				Long id = ((Number) r[0]).longValue();
				Integer terima = r[1] == null ? null : Integer.valueOf(((Number) r[1]).intValue());
				int jml = ((Number) r[2]).intValue();

				int[] c = hasil.get(id);
				if (c == null) {
					c = new int[4];
					hasil.put(id, c);
				}
				c[TOTAL] += jml;
				if (terima != null) {
					if (terima.intValue() == 0) {
						c[BELUM] += jml;
					} else if (terima.intValue() == 1) {
						c[DITERIMA] += jml;
					} else if (terima.intValue() == 2) {
						c[DITOLAK] += jml;
					}
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "hitung-seleksi-penerima src/ais/common/helper/HitungSeleksiPenerimaHelper.java");
		}
		return hasil;
	}

	/** Mengumpulkan id (bukan-null) dari daftar induk apa pun yang punya getId(). */
	public static List<Long> kumpulkanId(List<?> daftarInduk) {
		List<Long> ids = new ArrayList<Long>();
		if (daftarInduk == null) {
			return ids;
		}
		for (int i = 0; i < daftarInduk.size(); i++) {
			Object o = daftarInduk.get(i);
			if (o == null) {
				continue;
			}
			try {
				java.lang.reflect.Method m = o.getClass().getMethod("getId");
				Object id = m.invoke(o);
				if (id instanceof Number) {
					ids.add(Long.valueOf(((Number) id).longValue()));
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e,
						"kumpulkanId src/ais/common/helper/HitungSeleksiPenerimaHelper.java");
			}
		}
		return ids;
	}

	/**
	 * Membangun HTML kolom "Informasi" yang sama seperti sebelumnya.
	 *
	 * @param c        array {@code {belum, diterima, ditolak, total}} (boleh null = semua nol).
	 * @param fontKecil true untuk memakai {@code font-size:8px} (KKN/PKL); false polos (Beasiswa).
	 */
	public static String htmlInformasi(int[] c, boolean fontKecil) {
		int belum = c == null ? 0 : c[BELUM];
		int diterima = c == null ? 0 : c[DITERIMA];
		int ditolak = c == null ? 0 : c[DITOLAK];
		int total = c == null ? 0 : c[TOTAL];
		String buka = fontKecil ? "<font style='font-size:8px;'>" : "<font>";
		String content = buka + "<ol>";
		content += "<li>Belum diproses : " + Common.numberFormat.get().format(belum) + "</li>";
		content += "<li>Diterima : " + Common.numberFormat.get().format(diterima) + "</li>";
		content += "<li>Ditolak : " + Common.numberFormat.get().format(ditolak) + "</li>";
		content += "<li>Total : " + Common.numberFormat.get().format(total) + "</li>";
		content += "</ol>" + "</font>";
		return content;
	}
}
