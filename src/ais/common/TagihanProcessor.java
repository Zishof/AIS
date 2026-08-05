package ais.common;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.master.helper.KegiatanHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;

public class TagihanProcessor {

	private static boolean sedangProses = false;

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
