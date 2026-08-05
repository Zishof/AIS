package ais.action.master.helper.util;

import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.MasaPerkuliahan;

public class MasaPerkuliahanSyncrhonizerProcessor extends TimerTask {

	@Override
	public void run() {
		doProcess();
	}

	@SuppressWarnings("unchecked")
	private void doProcess() {
		try {
			Konfigurasi cfgCicilan = Common.getKonfigurasi(
					"masa_perkuliahan_di_dibuat_berdasar_jadwal_perkuliahan", Konfigurasi.AKTIF);
			if (cfgCicilan != null && Konfigurasi.TIDAK_AKTIF.equals(cfgCicilan.getNilai())) {
				Session session = HibernateUtil.currentNativeSession();
				try {
					int jumlah = ((Number) session.createCriteria(CicilanPembayaran.class)
							.setProjection(Projections.rowCount())
							.add(Restrictions.isNotNull("pengaturanPembayaranBulanan"))
							.add(Restrictions.isNull("tahap")).uniqueResult()).intValue();
					System.out.println("jml cicilan tanpa tahap => " + jumlah);
					if (jumlah > 0) {
						List<Long> ids = session.createCriteria(CicilanPembayaran.class)
								.setProjection(Projections.property("id"))
								.add(Restrictions.isNotNull("pengaturanPembayaranBulanan"))
								.add(Restrictions.isNull("tahap")).list();
						for (Long id : ids) {
							CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
									.createCriteria(CicilanPembayaran.class).add(Restrictions.idEq(id))
									.uniqueResult();
							session.getTransaction().begin();
							try {
								Common.refreshSaveOrUpdate(session, cicilanPembayaran);
								session.getTransaction().commit();
							} catch (Exception exTx) {
								try { if (session.getTransaction() != null && session.getTransaction().isActive()) session.getTransaction().rollback(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/util/MasaPerkuliahanSyncrhonizerProcessor.java:52");}
							}
						}
					}
				} finally {
					HibernateUtil.closeSession();
				}
			}

			Konfigurasi masa_perkuliahan_syncrhonizer = Common.getKonfigurasi("masa_perkuliahan_synchronizer",
					Konfigurasi.TIDAK_AKTIF);

			if (masa_perkuliahan_syncrhonizer != null
					&& Konfigurasi.AKTIF.equals(masa_perkuliahan_syncrhonizer.getNilai())) {

				Session session = HibernateUtil.currentNativeSession();
				try {
					List<Jurusan> jurusans = session.createCriteria(Jurusan.class).list();

					for (Jurusan jurusan : jurusans) {
						String sql = "select perkuliahandimulai, perkuliahansampai,tahun_ajaran "
								+ "from perkuliahan a where a.jurusan = " + jurusan.getId()
								+ " and perkuliahandimulai is not null and perkuliahansampai is not null "
								+ "group by perkuliahandimulai,perkuliahansampai,tahun_ajaran "
								+ "order by tahun_ajaran,perkuliahandimulai,perkuliahansampai;";
						System.out.println(sql);
						List<Object[]> dates = session.createSQLQuery(sql).list();
						session.getTransaction().begin();
						try {
							for (Object[] myDates : dates) {
								MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) session
										.createCriteria(MasaPerkuliahan.class)
										.add(Restrictions.eq("mulai", myDates[0]))
										.add(Restrictions.eq("sampai", myDates[1]))
										.add(Restrictions.eq("jurusan", jurusan))
										.add(Restrictions.eq("tahunAkademik", myDates[2]))
										.setMaxResults(1).uniqueResult();

								if (masaPerkuliahan == null) {
									masaPerkuliahan = new MasaPerkuliahan();
									masaPerkuliahan.setFakultas(jurusan.getFakultas());
									masaPerkuliahan.setJurusan(jurusan);
									masaPerkuliahan.setKeterangan("");
									masaPerkuliahan.setMulai((Date) myDates[0]);
									masaPerkuliahan.setTahunAkademik((String) myDates[2]);
									masaPerkuliahan.setSampai((Date) myDates[1]);
									session.save(masaPerkuliahan);
								}
							}
							session.getTransaction().commit();
						} catch (Exception exTx) {
							try { if (session.getTransaction() != null && session.getTransaction().isActive()) session.getTransaction().rollback(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/util/MasaPerkuliahanSyncrhonizerProcessor.java:103");}
						}
					}

					String sql = "update perkuliahan a set masa_perkuliahan = (select max(id) from masa_perkuliahan where jurusan = a.jurusan and mulai = a.perkuliahandimulai and sampai = a.perkuliahansampai);";
					session.getTransaction().begin();
					try {
						System.out.println(sql);
						session.createSQLQuery(sql).executeUpdate();
						session.getTransaction().commit();
					} catch (Exception exTx) {
						try { if (session.getTransaction() != null && session.getTransaction().isActive()) session.getTransaction().rollback(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/util/MasaPerkuliahanSyncrhonizerProcessor.java:114");}
					}
				} finally {
					HibernateUtil.closeSession();
				}
			}
		} catch (Exception e) {
			// Tangkap semua (termasuk NPE dari getKonfigurasi null saat DB overload)
			// agar TimerTask tidak mati permanen
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/util/MasaPerkuliahanSyncrhonizerProcessor.java:123");
			HibernateUtil.closeSession();
		}
	}
}
