package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ais.common.Common;
import ais.database.model.Detailperkuliahan;
import ais.database.model.GeneralValueObject;
import ais.database.model.Matakuliah;

/**
 * Utilitas penyaring duplikasi matakuliah EKIVALEN pada tampilan-tampilan yang
 * menampilkan <b>nilai akhir</b> mahasiswa (Transkrip, KHS, Penilaian).
 *
 * <p><b>Latar:</b> Bila dua matakuliah dinyatakan saling ekivalen (mis.
 * <i>PEMROGRAMAN DASAR</i> ekivalen dengan <i>PEMROGRAMAN DESKTOP</i>) dan
 * seorang mahasiswa pernah mengambil keduanya, maka pada tampilan nilai keduanya
 * akan muncul "dua-duanya". Sesuai ketentuan akademik, yang seharusnya tampil
 * hanyalah <b>satu</b> — yaitu yang <b>nilainya tertinggi</b>.</p>
 *
 * <p><b>Cara kerja:</b> setiap {@link Detailperkuliahan} dipetakan ke
 * "matakuliah kanonik" lewat {@link Common#getMatakuliahApakahEkivalen}. Karena
 * {@code Matakuliah.reInitEkivalen} membangun relasi secara <i>simetris</i>
 * (mencari {@code matakuliah == this OR matakuliahEkivalen == this}), dua
 * matakuliah yang berpasangan ekivalen akan menghasilkan kanonik yang SAMA,
 * sehingga dapat dikelompokkan. Untuk tiap kelompok kanonik hanya dipertahankan
 * Detailperkuliahan dengan {@code getTotalNilai()} terbesar; sisanya disembunyikan.
 * Urutan kemunculan dipertahankan (slot kelompok memakai posisi kemunculan
 * pertamanya).</p>
 *
 * <p><b>Catatan pemakaian:</b> hanya dipakai pada tampilan <i>hasil/nilai</i>.
 * JANGAN dipakai pada perencanaan KRS / Rencana Studi, karena di sana mahasiswa
 * justru perlu melihat seluruh pengambilan matakuliah.</p>
 */
public class EkivalenNilaiUtil {

	private EkivalenNilaiUtil() {
	}

	/**
	 * Saring daftar id {@link Detailperkuliahan} sehingga matakuliah yang saling
	 * ekivalen hanya tersisa satu — yang nilainya tertinggi.
	 *
	 * @param detailperkuliahanIds daftar id Detailperkuliahan (boleh null/kosong)
	 * @param nim                  NIM mahasiswa (untuk ekivalen khusus-NIM); boleh null
	 * @return daftar baru yang sudah disaring; bila terjadi kegagalan, daftar asli
	 *         dikembalikan apa adanya (aman — tidak menyembunyikan data secara keliru)
	 */
	public static List<Long> saringNilaiTertinggi(Collection<Long> detailperkuliahanIds, String nim) {
		if (detailperkuliahanIds == null) {
			return new ArrayList<Long>();
		}
		if (detailperkuliahanIds.size() < 2) {
			return new ArrayList<Long>(detailperkuliahanIds);
		}
		try {
			List<Long> hasil = new ArrayList<Long>();
			Map<Long, Integer> posisiKanonik = new LinkedHashMap<Long, Integer>();
			Map<Long, Double> nilaiTerbaik = new LinkedHashMap<Long, Double>();

			for (Long id : detailperkuliahanIds) {
				if (id == null) {
					continue;
				}
				Detailperkuliahan dp = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						id.toString());
				if (dp == null) {
					hasil.add(id);
					continue;
				}
				Matakuliah mk = dp.getPerkuliahan() == null ? dp.getMatakuliahKonversi()
						: dp.getPerkuliahan().getMatakuliah();

				Long kanonik;
				try {
					Matakuliah[] mks = Common.getMatakuliahApakahEkivalen(mk, nim, false);
					kanonik = (mks != null && mks[0] != null) ? mks[0].getId() : (mk == null ? id : mk.getId());
				} catch (Throwable t) {
					kanonik = (mk == null ? id : mk.getId());
				}
				if (kanonik == null) {
					kanonik = id;
				}

				double nilai = dp.getTotalNilai() == null ? 0.0 : dp.getTotalNilai().doubleValue();

				if (!posisiKanonik.containsKey(kanonik)) {
					// kemunculan pertama kelompok ini → ambil slot baru
					posisiKanonik.put(kanonik, hasil.size());
					nilaiTerbaik.put(kanonik, nilai);
					hasil.add(id);
				} else if (nilai > nilaiTerbaik.get(kanonik)) {
					// nilai lebih tinggi → gantikan isi slot kelompok (urutan tetap)
					hasil.set(posisiKanonik.get(kanonik).intValue(), id);
					nilaiTerbaik.put(kanonik, nilai);
				}
				// nilai lebih rendah/sama → diabaikan (disembunyikan)
			}
			return hasil;
		} catch (Throwable t) {
			Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
			return new ArrayList<Long>(detailperkuliahanIds);
		}
	}
}
