package ais.action.master.sekolah.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.criteria.AuditDisjunction;
import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.helper.GenericRevisiHelper;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.Siswa;

/**
 * History + Restore data audit (Envers) untuk PembayaranSiswaDetail, dibatasi
 * pada siswa/calon siswa yang sedang dipilih di layar Pembayaran Siswa.
 *
 * Mengikuti pola RevisiCicilanPembayaranHelper (kampus): seluruh tampilan riwayat,
 * restore satu revisi, dan restore massal "Restore Terbaru mulai tanggal" sudah
 * disediakan GenericRevisiHelper — class ini hanya menyuplai scope filternya.
 *
 * Scoping: PembayaranSiswaDetail tidak punya FK langsung ke siswa (hanya lewat
 * pembayaranSiswa), sedangkan query Envers tidak bisa join dua tingkat. Maka id
 * seluruh PembayaranSiswa milik siswa/calon siswa diambil dulu dari tabel live,
 * lalu revisi difilter dengan relatedId("pembayaranSiswa") per-id (disjunction).
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiPembayaranSiswaDetailHelper extends GenericRevisiHelper<PembayaranSiswaDetail> {

	private static final long serialVersionUID = 4127880021764301117L;

	private static final String[] SEARCH_PROPERTIES = new String[] { "keterangan", "ref", "oleh" };

	/**
	 * Tipe implementasi bersarang {@link MilikSiswaFilter} milik {@link RevisiPembayaranSiswaDetailHelper}. Kelas
	 * ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * RevisiPembayaranSiswaDetailHelper}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Siswa siswa}, {@code CalonSiswa
	 * calonSiswa}; operasi lokal: {@code apply}(). Aturan bisnis bersama tetap berada pada kelas induk atau
	 * service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see RevisiPembayaranSiswaDetailHelper
	 */
	private static class MilikSiswaFilter implements QueryCustomizer {
		private final Siswa siswa;
		private final CalonSiswa calonSiswa;

		MilikSiswaFilter(Siswa siswa, CalonSiswa calonSiswa) {
			this.siswa = siswa;
			this.calonSiswa = calonSiswa;
		}

		public void apply(Session session, AuditQuery query) throws Exception {
			if (siswa == null && calonSiswa == null) {
				return;
			}
			// Transaksi bisa tercatat atas nama siswa ATAU calon siswa (mis. calon yang
			// kemudian dikonversi menjadi siswa) -> keduanya dicakup sekaligus.
			Criterion pemilik;
			if (siswa != null && calonSiswa != null) {
				pemilik = Restrictions.or(Restrictions.eq("siswa", siswa),
						Restrictions.eq("calonSiswa", calonSiswa));
			} else if (siswa != null) {
				pemilik = Restrictions.eq("siswa", siswa);
			} else {
				pemilik = Restrictions.eq("calonSiswa", calonSiswa);
			}
			List ids = session.createCriteria(PembayaranSiswa.class).add(pemilik)
					.setProjection(Projections.id()).list();
			if (ids == null || ids.isEmpty()) {
				// Tidak ada transaksi utk siswa ini -> paksa hasil kosong (bukan tampil semua).
				query.add(AuditEntity.id().eq(Long.valueOf(-1)));
				return;
			}
			AuditDisjunction salahSatu = AuditEntity.disjunction();
			for (int i = 0; i < ids.size(); i++) {
				salahSatu.add(AuditEntity.relatedId("pembayaranSiswa").eq((Long) ids.get(i)));
			}
			query.add(salahSatu);
		}
	}

	private static QueryCustomizer[] buildFilters(Siswa siswa, CalonSiswa calonSiswa) {
		return new QueryCustomizer[] { new MilikSiswaFilter(siswa, calonSiswa) };
	}

	public RevisiPembayaranSiswaDetailHelper(EventListener eventListener, Siswa siswa, CalonSiswa calonSiswa)
			throws Exception {
		super(PembayaranSiswaDetail.class,
				"Revisi Pembayaran Siswa"
						+ (siswa != null ? " - " + siswa : (calonSiswa != null ? " - " + calonSiswa : "")),
				eventListener, SEARCH_PROPERTIES, buildFilters(siswa, calonSiswa));
	}

	/**
	 * Buka langsung tab "Seluruh Data Revisi" (tempat tombol Restore per-revisi dan
	 * "Restore Terbaru mulai tanggal" berada) — dipakai tombol Restore di toolbar.
	 */
	public void bukaTabSeluruhData() {
		try {
			if (mainTabbox != null) {
				mainTabbox.setSelectedIndex(2);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit RevisiPembayaranSiswaDetailHelper.bukaTabSeluruhData");
		}
	}
}
