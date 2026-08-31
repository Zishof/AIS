package ais.common;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.master.helper.KegiatanHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;

/**
 * Prosesor batch untuk menyinkronkan ulang tagihan {@link Kegiatan} yang nilainya tidak lagi
 * konsisten antara kolom {@code dibayar} dan {@code amount} (biasanya akibat perubahan data lain
 * yang tidak melewati alur normal penghitungan ulang tagihan). Dipanggil secara berkala (mis. dari
 * scheduler/cron job aplikasi) lewat {@link #proses()} untuk menemukan baris-baris
 * {@link Kegiatan} yang "tidak singkron" tersebut dan memaksa penghitungan ulang tagihannya lewat
 * {@link KegiatanHelper}.
 *
 * <p>
 * Kelas ini murni statis dan menyimpan satu bit state bersama ({@link #sedangProses}) sebagai
 * penanda mutex sederhana agar dua eksekusi {@link #proses()} tidak berjalan tumpang tindih
 * (mis. bila scheduler memicu proses baru sebelum proses sebelumnya selesai). Seluruh pekerjaan
 * dilakukan lewat sesi Hibernate native yang dibuka-tutup manual per baris data agar transaksi
 * tetap kecil dan tidak menahan koneksi lama saat memproses banyak tagihan.
 * </p>
 */
public class TagihanProcessor {

	/**
	 * Penanda mutex proses-hidup: {@code true} selama {@link #proses()} sedang berjalan, dipakai
	 * untuk mencegah dua eksekusi {@link #proses()} berjalan bersamaan (mis. dipicu scheduler
	 * dua kali sebelum eksekusi pertama selesai). Bukan penanda thread-safe murni (tidak
	 * {@code volatile}/{@code synchronized}), sehingga hanya efektif sebagai pengaman ringan,
	 * bukan jaminan eksklusi mutual yang ketat.
	 */
	private static boolean sedangProses = false;

	/**
	 * Menjalankan satu putaran sinkronisasi ulang tagihan. Hanya berjalan bila fitur
	 * {@code proses_tagihan_otomatis} aktif di konfigurasi DAN tidak ada eksekusi lain yang
	 * sedang berjalan ({@link #sedangProses} bernilai {@code false}).
	 *
	 * <p>
	 * Langkah kerja: (1) mencari seluruh id {@link Kegiatan} yang kolom {@code dibayar}-nya
	 * {@code null} atau tidak sama dengan {@code amount} (dibandingkan sebagai integer via
	 * {@code cast(... AS INTEGER)}) — inilah definisi "tagihan tidak singkron" pada method ini;
	 * (2) untuk setiap id yang ditemukan, memuat ulang entity {@link Kegiatan} dalam sesi baru
	 * dan memanggil {@link KegiatanHelper#checkKegiatanMahasiswa} (bila kegiatan terkait
	 * mahasiswa aktif) atau {@link KegiatanHelper#checkKegiatanCalonMahasiswa} (bila terkait
	 * calon mahasiswa) untuk memaksa penghitungan ulang tagihan; (3) setiap kegagalan per baris
	 * ditangkap dan dicatat ke {@link ErrorAuditUtil} tanpa menghentikan proses baris lainnya,
	 * sehingga satu data bermasalah tidak menggagalkan seluruh batch.
	 * </p>
	 *
	 * <p>
	 * Sesi Hibernate dibuka dan ditutup terpisah untuk query pencarian id dan untuk setiap baris
	 * yang diproses, agar transaksi tetap ringan. Penanda {@link #sedangProses} selalu
	 * dikembalikan ke {@code false} di akhir eksekusi (baik sukses maupun terjadi galat yang
	 * tertangkap), sehingga putaran berikutnya tetap dapat berjalan.
	 * </p>
	 */
	@SuppressWarnings("unchecked")
	public static void proses() {

		if (!sedangProses && Common.bolehKonfigurasi("proses_tagihan_otomatis", Konfigurasi.TIDAK_AKTIF)) {
			sedangProses = true;

			try {
				Session session = HibernateUtil.currentNativeSession();
//				List<Long> tagihansTidakSingkron = session.createCriteria(Kegiatan.class)
//						.setProjection(Projections.property("id"))
//						.add(Restrictions.or(
//								Restrictions.sqlRestriction(
//										"cast(tagihan AS INTEGER) != cast((amount+amount_terhutang) AS INTEGER)"),
//								Restrictions.sqlRestriction("cast(dibayar AS INTEGER) != cast(amount AS INTEGER)")))
//						.list();

				List<Long> tagihansTidakSingkron = session.createCriteria(Kegiatan.class)
						.setProjection(Projections.property("id")).add(Restrictions.sqlRestriction(
								"dibayar is null or (cast(dibayar AS INTEGER) != cast(amount AS INTEGER))"))
						.list();

				System.out.println("jumlah tagihan tidak singkron -> " + tagihansTidakSingkron.size());
				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}
				HibernateUtil.closeSession();

				if (!tagihansTidakSingkron.isEmpty()) {
//					KegiatanHelper.prosestagihan = true;

					int size = tagihansTidakSingkron.size();
					int index = 1;
					try {
						for (Long keg : tagihansTidakSingkron) {
							System.out
									.println("tagihan -> " + Common.numberFormat.get().format((index * 100.0) / size) + "%");
							index++;
							try {
								session = HibernateUtil.currentNativeSession();
								Kegiatan kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class)
										.add(Restrictions.idEq(keg)).uniqueResult();
								if (kegiatan != null) {
									if (kegiatan.getMahasiswa() != null) {
										KegiatanHelper.checkKegiatanMahasiswa(kegiatan, kegiatan.getJenisKegiatan(),
												kegiatan.getMahasiswa(), kegiatan.getSemster(),
												kegiatan.getTahunAkademik(), true, kegiatan.getJadwalPembayaran(), false,
												true, null, session);
									} else if (kegiatan.getCalonMahasiswa() != null) {
										KegiatanHelper.checkKegiatanCalonMahasiswa(kegiatan,
												kegiatan.getJenisKegiatan(), kegiatan.getCalonMahasiswa(),
												kegiatan.getSemster(), kegiatan.getTahunAkademik(), true,
												kegiatan.getJadwalPembayaran(), false, true, null, session);
									}
								}
								// session.disconnect();
								if (session.isOpen()) {session.disconnect();session.close();}

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/TagihanProcessor.java:75");
							}
							HibernateUtil.closeSession();

						}

					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/TagihanProcessor.java:82");
					}

//					KegiatanHelper.prosestagihan = false;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/TagihanProcessor.java:88");
			}

			sedangProses = false;

		}

	}

}
