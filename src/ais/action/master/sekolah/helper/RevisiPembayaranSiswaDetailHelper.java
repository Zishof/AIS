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
 * Subclass dari {@link ais.action.master.helper.GenericRevisiHelper} untuk entity
 * {@link PembayaranSiswaDetail}, dibatasi pada siswa/calon siswa yang sedang dipilih di layar
 * Pembayaran Siswa — lihat Javadoc class induk untuk penjelasan lengkap arsitektur window, alur
 * Envers, dan fitur restore (satu revisi maupun massal "Restore Terbaru mulai tanggal"); class ini
 * hanya menyuplai scope filternya lewat {@link MilikSiswaFilter}.
 *
 * <p>Mengikuti pola {@code RevisiCicilanPembayaranHelper} (kampus, lihat contoh di Javadoc
 * {@link ais.action.master.helper.GenericRevisiHelper}).</p>
 *
 * <p><b>Scoping dua tingkat:</b> {@link PembayaranSiswaDetail} tidak punya FK langsung ke
 * {@link Siswa}/{@link CalonSiswa} (hanya lewat {@code pembayaranSiswa}), sedangkan query Envers
 * ({@code AuditQuery}) tidak bisa melakukan join dua tingkat layaknya HQL biasa. Maka
 * {@link MilikSiswaFilter} mengambil dulu SELURUH id {@link PembayaranSiswa} milik siswa/calon
 * siswa dari tabel LIVE (bukan riwayat) lewat {@code Criteria} biasa, lalu revisi
 * {@code PembayaranSiswaDetail} disaring dengan {@code AuditEntity.relatedId("pembayaranSiswa")}
 * per-id memakai disjunction (OR) — pola workaround ini penting dipahami karena berbeda dari
 * {@link ais.action.master.helper.GenericRevisiHelper.FixedPropertyFilter} satu-tingkat biasa yang
 * dipakai kebanyakan subclass lain.</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiPembayaranSiswaDetailHelper extends GenericRevisiHelper<PembayaranSiswaDetail> {

	private static final long serialVersionUID = 4127880021764301117L;

	private static final String[] SEARCH_PROPERTIES = new String[] { "keterangan", "ref", "oleh" };

	/**
	 * Implementasi {@link QueryCustomizer} yang membatasi riwayat {@link PembayaranSiswaDetail}
	 * hanya pada baris milik {@link PembayaranSiswa} kepunyaan satu {@link Siswa} dan/atau
	 * {@link CalonSiswa} tertentu. Lihat penjelasan workaround "scoping dua tingkat" pada Javadoc
	 * class {@link RevisiPembayaranSiswaDetailHelper} — filter inilah yang mengimplementasikan
	 * workaround tersebut: query id {@code PembayaranSiswa} milik siswa/calon siswa lewat
	 * {@code Criteria} pada tabel live, lalu menambahkan disjungsi {@code relatedId} per-id ke
	 * {@code AuditQuery} Envers.
	 *
	 * <p>Tipe ini {@code static} dan tidak menangkap instance {@link RevisiPembayaranSiswaDetailHelper}
	 * — seluruh dependensi ({@code siswa}, {@code calonSiswa}) diberikan eksplisit lewat
	 * konstruktor, sehingga aman dipakai berulang oleh {@link GenericRevisiHelper#buildAuditQuery}
	 * tanpa keterikatan pada state window.</p>
	 *
	 * @see RevisiPembayaranSiswaDetailHelper
	 */
	private static class MilikSiswaFilter implements QueryCustomizer {
		private final Siswa siswa;
		private final CalonSiswa calonSiswa;

		/**
		 * @param siswa      siswa pemilik transaksi pembayaran yang riwayatnya ingin ditampilkan;
		 *                   boleh {@code null}.
		 * @param calonSiswa calon siswa pemilik transaksi pembayaran; boleh {@code null}. Bila
		 *                   {@code siswa} dan {@code calonSiswa} keduanya {@code null}, filter
		 *                   tidak melakukan apa-apa ({@link #apply} kembali tanpa menyaring apa
		 *                   pun — seluruh riwayat ditampilkan).
		 */
		MilikSiswaFilter(Siswa siswa, CalonSiswa calonSiswa) {
			this.siswa = siswa;
			this.calonSiswa = calonSiswa;
		}

		/**
		 * Menambahkan kriteria pembatas kepemilikan ke {@code query} Envers: (1) cari seluruh ID
		 * {@link PembayaranSiswa} milik {@code siswa} dan/atau {@code calonSiswa} dari tabel LIVE
		 * (transaksi bisa tercatat atas nama siswa ATAU calon siswa — mis. calon yang kemudian
		 * dikonversi menjadi siswa — sehingga keduanya dicakup sekaligus lewat OR bila kedua
		 * parameter diisi); (2) bila tidak ada ID yang cocok, paksa hasil kosong (bukan tampil
		 * semua) dengan filter ID yang mustahil ({@code -1}); (3) bila ada, tambahkan disjungsi
		 * {@code AuditEntity.relatedId("pembayaranSiswa")} untuk tiap ID ke {@code query}.
		 *
		 * @param session Session Hibernate lokal (dipakai untuk query {@code Criteria} tabel live).
		 * @param query   AuditQuery Envers yang sedang dibangun oleh kelas induk; dimodifikasi
		 *                in-place lewat {@code query.add(...)}.
		 * @throws Exception diteruskan bila query database gagal.
		 */
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

	/** Membungkus satu {@link MilikSiswaFilter} sebagai array {@link QueryCustomizer} tunggal untuk konstruktor induk. */
	private static QueryCustomizer[] buildFilters(Siswa siswa, CalonSiswa calonSiswa) {
		return new QueryCustomizer[] { new MilikSiswaFilter(siswa, calonSiswa) };
	}

	/**
	 * Membuka window riwayat revisi Pembayaran Siswa Detail, dibatasi pada satu siswa dan/atau
	 * calon siswa (lihat {@link MilikSiswaFilter}). Judul window disisipi identitas siswa/calon
	 * siswa bila salah satunya diberikan.
	 *
	 * @param eventListener callback yang diteruskan ke {@link ais.action.master.helper.GenericRevisiHelper}.
	 * @param siswa         siswa pemilik transaksi yang riwayatnya ingin dilihat; boleh {@code null}.
	 * @param calonSiswa    calon siswa pemilik transaksi; boleh {@code null}.
	 */
	public RevisiPembayaranSiswaDetailHelper(EventListener eventListener, Siswa siswa, CalonSiswa calonSiswa)
			throws Exception {
		super(PembayaranSiswaDetail.class,
				"Revisi Pembayaran Siswa"
						+ (siswa != null ? " - " + siswa : (calonSiswa != null ? " - " + calonSiswa : "")),
				eventListener, SEARCH_PROPERTIES, buildFilters(siswa, calonSiswa));
	}

	/**
	 * Buka langsung tab "Seluruh Data Revisi" (tempat tombol Restore per-revisi dan
	 * "Restore Terbaru mulai tanggal" berada) — dipakai tombol Restore di toolbar. Kegagalan
	 * (mis. {@code mainTabbox} belum terbentuk) dicatat lewat {@code ErrorAuditUtil} dan tidak
	 * dilempar ulang, karena ini hanya kenyamanan UI, bukan operasi kritikal.
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
