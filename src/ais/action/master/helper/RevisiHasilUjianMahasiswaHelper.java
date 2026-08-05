package ais.action.master.helper;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.HasilUjianMahasiswaDetail;
import org.zkoss.zk.ui.event.EventListener;
/**
 * Versi generic dari helper revisi lama.
 *
 * Semua proses baca/restore revisi dipusatkan di GenericRevisiHelper<T> agar:
 * - code lebih ringkas dan mudah dirawat;
 * - semua Hibernate Session memakai openSession();
 * - semua Session ditutup di finally melalui session.clear(), session.disconnect(), dan session.close();
 * - fitur restore satu revisi dan restore massal dari tanggal tertentu tetap tersedia.
 *
 * Kompatibel Java 1.7 / source 1.6.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiHasilUjianMahasiswaHelper extends GenericRevisiHelper<HasilUjianMahasiswaDetail> {

	private static final long serialVersionUID = 6589578552710016753L;
	private static final String[] SEARCH_PROPERTIES = new String[] { "nama", "kode", "keterangan" };

	private static QueryCustomizer[] buildFilters(HasilUjianMahasiswa hasilUjianMahasiswa) {
		java.util.List<QueryCustomizer> filters = new java.util.ArrayList<QueryCustomizer>();
		if (hasilUjianMahasiswa != null) {
			filters.add(new GenericRevisiHelper.FixedPropertyFilter("hasilUjianMahasiswa", hasilUjianMahasiswa));
		}
		return filters.toArray(new QueryCustomizer[filters.size()]);
	}

	public RevisiHasilUjianMahasiswaHelper(HasilUjianMahasiswa hasilUjianMahasiswa, EventListener eventListener) throws Exception {
		super(HasilUjianMahasiswaDetail.class, "Revisi Hasil Ujian Mahasiswa", eventListener, SEARCH_PROPERTIES, buildFilters(hasilUjianMahasiswa));
	}

}
