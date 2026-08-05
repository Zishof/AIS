package ais.action.master.helper.util;

import java.io.Serializable;
import java.util.List;
import java.util.TimerTask;

import org.hibernate.Session;

import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jenjang;
import ais.database.model.Konfigurasi;

public class AutoNotActivatingMahasiswaSemuaJenjangProcessor extends TimerTask {

	public AutoNotActivatingMahasiswaSemuaJenjangProcessor() {

	}

	@Override
	public void run() {
		doProcess();
	}

	@SuppressWarnings("unchecked")
	private void doProcess() {

		PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();
		JenisKegiatan jenisKegiatan = pembayaranUtil.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA);

		Konfigurasi mhs_all_lambat_bayar_langsung_tidak_aktif = Common
				.getKonfigurasi("mhs_all_lambat_bayar_langsung_tidak_aktif", Konfigurasi.TIDAK_AKTIF);
		String currentTahunAkademik = Common.getCurrentTahunAkademik();
		System.out.println("================ CHECK AUTO NOT ACTIVE MAHASISWA SEMUA TAHUN ANGKATAN "
				+ currentTahunAkademik + " SEMESTER " + (Common.isNowSemensterGanjil() ? "GANJIL" : "GENAP")
				+ " STATUS = " + mhs_all_lambat_bayar_langsung_tidak_aktif.getNilai() + " =========================");

		if (mhs_all_lambat_bayar_langsung_tidak_aktif.getNilai().equalsIgnoreCase(Konfigurasi.AKTIF)) {

			System.out.println("================ CHECK AUTO NOT ACTIVE MAHASISWA SEMUA TAHUN ANGKATAN "
					+ currentTahunAkademik + " SEMESTER " + (Common.isNowSemensterGanjil() ? "GANJIL" : "GENAP")
					+ " =========================");
			Session session = HibernateUtil.getSessionFactory().openSession();
			try {

				List<Jenjang> jenjangs = session.createCriteria(Jenjang.class).list();

				for (Jenjang jenjang : jenjangs) {
					Serializable[] serializables = pembayaranUtil.getJadwalPembayaranDanDendaIgnoreStart(
							ais.ui.util.WaktuUtil.getDate(), jenisKegiatan, jenjang, Common.getCurrentTahunAkademik(),
							Common.isNowSemensterGanjil(), null, null, null);

					JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];

					if (jadwalPembayaran != null) {
						System.out.println(
								"Jadwal pembayaran masih ada " + jadwalPembayaran + ", " + jadwalPembayaran.getId());
						return;
					} else {
						System.out.println("Jadwal sudah tidak ada ");
					}

					org.hibernate.Transaction tx = session.beginTransaction();
					try {

//					String sql = "update mahasiswa set status = " + ConstantValues.TIDAK_AKTIF.getId()
//							+ ", keterangan = 'Mahasiswa ini statusnya menjadi tidak aktif karena belum melakukan pembayaran di tahun akademik "
//							+ currentTahunAkademik + " semester " + (Common.isNowSemensterGanjil() ? "Ganjil" : "Genap")
//							+ "' where id in (select a.id " + "from mahasiswa a "
//							+ "left join (select aa.* from kegiatan aa where aa.jenis_kegiatan = " + jenisKegiatan.getId()
//							+ " and aa.tahun_akademik = '" + currentTahunAkademik + "' and aa.semster % 2 = "
//							+ (Common.isNowSemensterGanjil() ? "1" : "0") + ") b on (b.mahasiswa = a.id) "
//							+ " where b.id is null and a.jenjang = " + jenjang.getId() + " and a.status = "
//							+ ConstantValues.AKTIF.getId() + ") ";
//
//					System.out.println("SQL update =  " + sql);
//
//					session.createSQLQuery(sql).executeUpdate();

						tx.commit();
					} catch (Exception eUpdate) {
						if (tx != null && tx.isActive())
							tx.rollback();
						Common.tampilErrorJikaAdmin(eUpdate);
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null) {
					try {
						session.clear();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/util/AutoNotActivatingMahasiswaSemuaJenjangProcessor.java:98");
					}
					try {
						session.disconnect();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/util/AutoNotActivatingMahasiswaSemuaJenjangProcessor.java:102");
					}
					try {
						if (session.isOpen())
							session.close();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/util/AutoNotActivatingMahasiswaSemuaJenjangProcessor.java:107");
					}
				}
			}

		}

	}

}
