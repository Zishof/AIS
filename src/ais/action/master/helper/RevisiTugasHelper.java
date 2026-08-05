package ais.action.master.helper;

import org.zkoss.zk.ui.event.EventListener;

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

	private static QueryCustomizer[] buildFilters(String property, Object value) {
		java.util.List<QueryCustomizer> filters = new java.util.ArrayList<QueryCustomizer>();
		if (property != null && property.trim().length() > 0 && value != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter(property, value));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
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
}
