package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.zkoss.zk.ui.event.EventListener;

import ais.database.model.Pertemuan;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.VOPembelajaran;

/**
 * Recovery / Riwayat Revisi Tugas (Envers) — reuse {@link GenericRevisiHelper} yang sudah menyediakan
 * grid riwayat (ADD/MOD/DEL), pencarian, perbandingan, dan <b>restore satu revisi</b> maupun restore
 * massal. Dipakai tombol "Recovery" di menu Tugas.
 *
 * <p>Kelas Tugas memiliki dua turunan konkret yang di-audit sendiri-sendiri: {@code Pertemuan} (tugas
 * yang melekat pada pertemuan) dan {@code TugasPertemuan} (tugas tambahan). Karena Envers bekerja
 * per-entity konkret, konstruktor menerima {@code kelas} konkret dari objek tugas yang dibuka
 * ({@code tugas.getClass()}). Untuk membatasi riwayat ke <b>pembelajaran yang sama</b> (VoPembelajaran),
 * pemanggil memberi properti asosiasi + nilainya (mis. "perkuliahan"/"jadwalPelajaran") yang tersedia
 * pada kelas tersebut; bila null, seluruh riwayat kelas itu ditampilkan.</p>
 *
 * <p>Kompatibel Java 1.7.</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiTugasHelper extends GenericRevisiHelper {

	private static final long serialVersionUID = 6589578552710016754L;

	/** Properti yang bisa dicari dari kotak pencarian modal (semuanya field milik Tugas). */
	private static final String[] SEARCH_PROPERTIES = new String[] { "judultugas" };

	/**
	 * Membangun daftar filter (0 atau 1 elemen) dari satu pasangan properti-nilai bebas. Dipakai
	 * konstruktor generik {@link #RevisiTugasHelper(Class, String, Object, EventListener)} yang
	 * menerima nama properti asosiasi apa saja dari pemanggil.
	 *
	 * @param property nama properti asosiasi pembatas (mis. {@code "perkuliahan"}); bila
	 *                 {@code null}/kosong, tidak ada filter yang ditambahkan.
	 * @param value    nilai pembatas untuk {@code property}; bila {@code null}, tidak ada filter
	 *                 yang ditambahkan meski {@code property} terisi.
	 * @return array berisi satu {@link GenericRevisiHelper.FixedPropertyFilter}, atau array kosong
	 *         bila {@code property}/{@code value} tidak lengkap (seluruh riwayat kelas ditampilkan).
	 */
	private static QueryCustomizer[] buildFilters(String property, Object value) {
		List<QueryCustomizer> filters = new ArrayList<QueryCustomizer>();
		if (property != null && property.trim().length() > 0 && value != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter(property, value));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	/**
	 * Membangun filter yang membatasi riwayat ke satu {@link VOPembelajaran} (mata kuliah/kelas yang
	 * sedang dibuka), disesuaikan dengan kelas entitas tugas konkret yang sedang dibuka karena setiap
	 * turunan Tugas menyimpan asosiasi pembelajaran lewat properti yang berbeda. Dipakai konstruktor
	 * {@link #RevisiTugasHelper(Class, VOPembelajaran, EventListener)} (jalur "Recovery").
	 *
	 * <ul>
	 *   <li>{@link Pertemuan} dan {@link TugasKelompok} — asosiasi langsung ke pembelajaran lewat
	 *       properti {@code "perkuliahan"} (bila {@code pembelajaran} berupa {@code Perkuliahan}) atau
	 *       {@code "jadwalPelajaran"} (kursus non-perkuliahan).</li>
	 *   <li>{@link TugasPertemuan} — tidak punya asosiasi langsung ke pembelajaran, sehingga dibatasi
	 *       tidak langsung lewat kumpulan ID {@link Pertemuan} milik {@code pembelajaran} (lihat
	 *       {@link #ambilIdPertemuan(VOPembelajaran)}), fail-closed ke ID {@code -1} bila kosong agar
	 *       tidak menampilkan seluruh riwayat kelas.</li>
	 * </ul>
	 *
	 * @param kelas        kelas entitas tugas konkret yang sedang dibuka.
	 * @param pembelajaran konteks mata kuliah/kelas pembatas; bila {@code null}, tidak ada filter yang
	 *                     ditambahkan (seluruh riwayat kelas ditampilkan).
	 * @return array filter yang sesuai kombinasi {@code kelas}/{@code pembelajaran}; bisa kosong.
	 */
	private static QueryCustomizer[] buildFilters(final Class kelas, final VOPembelajaran pembelajaran) {
		List<QueryCustomizer> filters = new ArrayList<QueryCustomizer>();
		if (pembelajaran == null) {
			return filters.toArray(new QueryCustomizer[filters.size()]);
		}

		if (Pertemuan.class.equals(kelas)) {
			String property = pembelajaran instanceof ais.database.model.Perkuliahan ? "perkuliahan"
					: "jadwalPelajaran";
			filters.add(new GenericRevisiHelper.FixedPropertyFilter(property, pembelajaran));
		} else if (TugasKelompok.class.equals(kelas)) {
			String property = pembelajaran instanceof ais.database.model.Perkuliahan ? "perkuliahan"
					: "jadwalPelajaran";
			filters.add(new GenericRevisiHelper.FixedPropertyFilter(property, pembelajaran));
		} else if (TugasPertemuan.class.equals(kelas)) {
			filters.add(new QueryCustomizer() {
				@Override
				public void apply(Session session, AuditQuery query) throws Exception {
					Long[] ids = ambilIdPertemuan(pembelajaran);
					if (ids.length == 0) {
						query.add(AuditEntity.property("pertemuan").eq(Long.valueOf(-1L)));
					} else {
						query.add(AuditEntity.property("pertemuan").in(ids));
					}
				}
			});
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	/**
	 * Mengekstrak seluruh ID {@link Pertemuan} milik satu {@link VOPembelajaran} (dari
	 * {@link VOPembelajaran#ambilPertemuan()}), membuang entri {@code null}. Dipakai
	 * {@link #buildFilters(Class, VOPembelajaran)} untuk membatasi riwayat {@link TugasPertemuan}
	 * secara tidak langsung lewat pertemuan induknya.
	 *
	 * @param pembelajaran konteks mata kuliah/kelas; boleh {@code null}.
	 * @return array ID pertemuan (bisa kosong, tidak pernah {@code null}).
	 */
	private static Long[] ambilIdPertemuan(VOPembelajaran pembelajaran) {
		TreeMap<String, Long> data = pembelajaran == null ? null : pembelajaran.ambilPertemuan();
		List<Long> ids = new ArrayList<Long>();
		if (data != null) {
			for (Long id : data.values()) {
				if (id != null) {
					ids.add(id);
				}
			}
		}
		return ids.toArray(new Long[ids.size()]);
	}

	/**
	 * @param kelas         kelas konkret entitas tugas (mis. Pertemuan.class / TugasPertemuan.class).
	 * @param filterProperty properti asosiasi pembatas pembelajaran (mis. "perkuliahan"); boleh null.
	 * @param filterValue    nilai (entitas) pembatas; boleh null.
	 * @param eventListener  callback saat ada restore (boleh null).
	 */
	public RevisiTugasHelper(Class kelas, String filterProperty, Object filterValue, EventListener eventListener)
			throws Exception {
		super(kelas, "Recovery Tugas — riwayat & kembalikan", eventListener, SEARCH_PROPERTIES,
				buildFilters(filterProperty, filterValue));
	}

	/** Membatasi recovery ke seluruh pertemuan milik satu pembelajaran. */
	public RevisiTugasHelper(Class kelas, VOPembelajaran pembelajaran, EventListener eventListener)
			throws Exception {
		super(kelas, "Recovery Tugas — riwayat & kembalikan", eventListener, SEARCH_PROPERTIES,
				buildFilters(kelas, pembelajaran));
	}
}
