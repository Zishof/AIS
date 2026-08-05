package ais.action.master.asset.helper;

import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PerjanjianKerjasamaMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;

/**
 * <h1>SinkronPersetujuanSopPengadaanHelper — self-heal kolom {@code disetujui_oleh} pengadaan</h1>
 *
 * <p><b>Masalah yang diselesaikan.</b> Pengajuan pengadaan (PR/PO/PKS) yang persetujuannya sudah
 * diproses lewat alur SOP <b>tidak langsung tampil di Dasbor Pengadaan</b>; data baru muncul setelah
 * setiap pengajuan dibuka/di-refresh satu per satu.</p>
 *
 * <p><b>Akar masalah.</b> {@code getDisetujuiOleh()} pada entitas pengadaan adalah getter
 * <i>terkomputasi</i>: ia MENURUNKAN pemberi persetujuan dari graf disposisi SOP
 * ({@code disposisiSop.getDisposisiSetuju().getDiajukanOleh()}) dan hanya <b>mengubah field di
 * memori</b> — TIDAK menyimpannya. Kolom mentah {@code disetujui_oleh} baru terisi ketika entitas
 * kebetulan tersimpan (mis. auto-save saat baris di-render). Sementara itu Dasbor Pengadaan menyaring
 * dengan {@code Restrictions.isNotNull("disetujuiOleh")} yang membaca <b>KOLOM MENTAH</b> — sehingga
 * pengajuan yang sudah disetujui via SOP belum terhitung sampai dibuka satu per satu.</p>
 *
 * <p><b>Cara kerja perbaikan.</b> Ambil id entitas yang <i>berpotensi basi</i> ({@code disposisiSop}
 * terisi TAPI {@code disetujuiOleh} mentah masih null), muat entitasnya, panggil getter terkomputasi
 * untuk mengetahui pemberi persetujuan sebenarnya, lalu tulis balik <b>HANYA satu kolom</b> memakai
 * <b>UPDATE HQL terarah</b>.</p>
 *
 * <p><b>Kenapa UPDATE HQL, bukan {@code save()} entitas.</b> Entitas ini penuh getter terkomputasi yang
 * memutasi field saat dibaca; menyimpan entitas utuh akan ikut mem-flush nilai turunan lain yang tidak
 * kita kehendaki. Sesi khusus dibuka dengan {@link FlushMode#MANUAL} agar entitas kotor tidak pernah
 * ter-flush diam-diam (pelajaran yang sama seperti backfill satuan kerja).</p>
 *
 * <p><b>Sifat.</b> Idempoten &amp; menyembuhkan diri: baris yang sudah benar tidak akan terambil lagi
 * (filternya {@code disetujuiOleh is null}), jadi setelah tersalin, biaya pemanggilan berikutnya hanya
 * beberapa query id yang kosong. Dibatasi {@value #MAKS_PER_TIPE} baris per tipe per pemanggilan agar
 * pemuatan dasbor tidak pernah lama; sisa tumpukan lama sembuh bertahap pada pemuatan berikutnya.</p>
 */
public final class SinkronPersetujuanSopPengadaanHelper {

	/** Batas baris yang disembuhkan per tipe per pemanggilan (menjaga dasbor tetap responsif). */
	public static final int MAKS_PER_TIPE = 300;

	private SinkronPersetujuanSopPengadaanHelper() {
	}

	/**
	 * Sinkronkan kolom mentah {@code disetujui_oleh} untuk PR, PO, dan PKS.
	 *
	 * @return jumlah baris yang berhasil disembuhkan (0 bila semua sudah sinkron)
	 */
	public static int backfillPersetujuanPengadaan() {
		int total = 0;
		total += backfillSatuTipe(PermintaanPengadaanMasterAsset.class);
		total += backfillSatuTipe(PemesananPengadaanMasterAsset.class);
		total += backfillSatuTipe(PerjanjianKerjasamaMasterAsset.class);
		return total;
	}

	/**
	 * Sembuhkan satu tipe entitas. Aman dipanggil berulang: hanya menyentuh baris yang kolom
	 * mentahnya masih null padahal SOP-nya sudah menyetujui.
	 */
	@SuppressWarnings("unchecked")
	private static int backfillSatuTipe(Class<?> kelas) {
		String fqn = kelas.getName();
		int sembuh = 0;
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			// MANUAL: entitas yang getter-nya memutasi field TIDAK boleh ter-flush diam-diam.
			session.setFlushMode(FlushMode.MANUAL);

			List<Long> ids = session
					.createQuery("select e.id from " + fqn + " e "
							+ "where e.disposisiSop is not null and e.disetujuiOleh is null")
					.setMaxResults(MAKS_PER_TIPE).list();

			for (Long id : ids) {
				if (id == null) {
					continue;
				}
				try {
					Object entitas = session.get(kelas, id);
					if (entitas == null) {
						continue;
					}

					// Getter TERKOMPUTASI: menurunkan pemberi persetujuan dari graf disposisi SOP.
					Tbmuser penyetuju = ambilDisetujuiOleh(entitas);
					if (penyetuju == null || penyetuju.getUserId() == null) {
						continue; // memang belum disetujui — biarkan apa adanya
					}

					// Tulis balik HANYA kolom disetujui_oleh (jangan simpan entitas utuh).
					session.getTransaction().begin();
					int n = session.createQuery("update " + fqn + " e set e.disetujuiOleh = :penyetuju where e.id = :id")
							.setParameter("penyetuju", penyetuju).setLong("id", id.longValue()).executeUpdate();
					session.getTransaction().commit();
					if (n > 0) {
						sembuh++;
					}
				} catch (Exception perBaris) {
					try {
						if (session.getTransaction() != null && session.getTransaction().isActive()) {
							session.getTransaction().rollback();
						}
					} catch (Exception ig) {
						ais.common.ErrorAuditUtil.record(ig,
								"auto-audit src/ais/action/master/asset/helper/SinkronPersetujuanSopPengadaanHelper.java:rollback");
					}
					ais.common.ErrorAuditUtil.record(perBaris,
							"auto-audit src/ais/action/master/asset/helper/SinkronPersetujuanSopPengadaanHelper.java:perBaris " + fqn);
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/action/master/asset/helper/SinkronPersetujuanSopPengadaanHelper.java:backfill " + fqn);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return sembuh;
	}

	/** Panggil {@code getDisetujuiOleh()} sesuai tipe konkret (hindari refleksi pada proxy Hibernate). */
	private static Tbmuser ambilDisetujuiOleh(Object entitas) {
		if (entitas instanceof PermintaanPengadaanMasterAsset) {
			return ((PermintaanPengadaanMasterAsset) entitas).getDisetujuiOleh();
		}
		if (entitas instanceof PemesananPengadaanMasterAsset) {
			return ((PemesananPengadaanMasterAsset) entitas).getDisetujuiOleh();
		}
		if (entitas instanceof PerjanjianKerjasamaMasterAsset) {
			return ((PerjanjianKerjasamaMasterAsset) entitas).getDisetujuiOleh();
		}
		return null;
	}
}
