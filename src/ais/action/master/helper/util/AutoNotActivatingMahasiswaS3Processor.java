package ais.action.master.helper.util;

import java.io.Serializable;
import java.util.TimerTask;

import org.hibernate.Session;

import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Konfigurasi;

/**
 * Tugas terjadwal ({@link TimerTask}) yang seharusnya menonaktifkan status mahasiswa
 * jenjang S3 secara otomatis apabila jadwal pembayaran (termasuk denda) untuk tahun
 * akademik/semester berjalan sudah lewat dan mahasiswa belum melakukan pembayaran.
 * Pengaktifan proses ini digantungkan pada konfigurasi
 * {@code mahasiswa_s3_lambat_bayar_langsung_tidak_aktif}.
 *
 * <p>
 * <b>Catatan penting:</b> query {@code UPDATE} yang benar-benar mengubah status mahasiswa
 * menjadi tidak aktif saat ini DIKOMENTARI (nonaktif) di {@link #doProcess()}. Selama
 * baris SQL tersebut tidak diaktifkan kembali, method ini hanya mencatat log dan membuka
 * lalu langsung meng-commit transaksi kosong — tidak ada perubahan data yang benar-benar
 * terjadi walau kondisi terpenuhi.
 * </p>
 */
public class AutoNotActivatingMahasiswaS3Processor extends TimerTask {

	/** Konstruktor tanpa argumen; tidak ada inisialisasi state khusus. */
	public AutoNotActivatingMahasiswaS3Processor() {

	}

	/** Dipanggil oleh {@link java.util.Timer} sesuai jadwal; mendelegasikan ke {@link #doProcess()}. */
	@Override
	public void run() {
		doProcess();
	}

	/**
	 * Memeriksa apakah masih ada jadwal pembayaran (termasuk denda) yang berlaku untuk
	 * jenjang S3 pada tahun akademik/semester berjalan. Bila konfigurasi
	 * {@code mahasiswa_s3_lambat_bayar_langsung_tidak_aktif} aktif dan tidak ada lagi
	 * jadwal pembayaran yang berlaku, method ini seharusnya menandai mahasiswa S3 yang
	 * belum membayar pada periode tersebut menjadi tidak aktif — namun query
	 * penonaktifan sesungguhnya saat ini dikomentari, sehingga efeknya hanya logging.
	 */
	private void doProcess() {

		PembayaranUtil pembayaranUtil;
		JenisKegiatan jenisKegiatan;

		pembayaranUtil = PembayaranUtil.getInstance();
		jenisKegiatan = pembayaranUtil
				.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA);

		Konfigurasi mahasiswa_s3_lambat_bayar_langsung_tidak_aktif = Common
				.getKonfigurasi(
						"mahasiswa_s3_lambat_bayar_langsung_tidak_aktif",
						Konfigurasi.TIDAK_AKTIF);
		String currentTahunAkademik = Common.getCurrentTahunAkademik();
		System.out
				.println("================ CHECK AUTO NOT ACTIVE MAHASISWA S3 TAHUN ANGKATAN "
						+ currentTahunAkademik
						+ " SEMESTER "
						+ (Common.isNowSemensterGanjil() ? "GANJIL" : "GENAP")
						+ " STATUS = "
						+ mahasiswa_s3_lambat_bayar_langsung_tidak_aktif
								.getNilai() + " =========================");

		if (mahasiswa_s3_lambat_bayar_langsung_tidak_aktif.getNilai()
				.equalsIgnoreCase(Konfigurasi.AKTIF)) {

			System.out
					.println("================ CHECK AUTO NOT ACTIVE MAHASISWA S3 TAHUN ANGKATAN "
							+ currentTahunAkademik
							+ " SEMESTER "
							+ (Common.isNowSemensterGanjil() ? "GANJIL"
									: "GENAP") + " =========================");
			Session session = HibernateUtil.currentNativeSession();

			// List<Jenjang> jenjangs =
			// session.createCriteria(Jenjang.class).list();

			// for (Jenjang jenjang : jenjangs) {
			Serializable[] serializables = pembayaranUtil
					.getJadwalPembayaranDanDendaIgnoreStart(ais.ui.util.WaktuUtil.getDate(), jenisKegiatan,
							ConstantValues.s3,
							Common.getCurrentTahunAkademik(),
							Common.isNowSemensterGanjil(), null, null, null);

			JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];

			if (jadwalPembayaran != null) {
				System.out.println("Jadwal pembayaran masih ada "
						+ jadwalPembayaran + ", " + jadwalPembayaran.getId());
				return;
			} else {
				System.out.println("Jadwal sudah tidak ada ");
			}

			session.getTransaction().begin();

//			String sql = "update mahasiswa set status = "
//					+ ConstantValues.TIDAK_AKTIF.getId()
//					+ ", keterangan = 'Mahasiswa ini statusnya menjadi tidak aktif karena belum melakukan pembayaran di tahun akademik "
//					+ currentTahunAkademik
//					+ " semester "
//					+ (Common.isNowSemensterGanjil() ? "Ganjil" : "Genap")
//					+ "' where id in (select a.id "
//					+ "from mahasiswa a "
//					+ "left join (select aa.* from kegiatan aa where aa.jenis_kegiatan = "
//					+ jenisKegiatan.getId() + " and aa.tahun_akademik = '"
//					+ currentTahunAkademik + "' and aa.semster % 2 = "
//					+ (Common.isNowSemensterGanjil() ? "1" : "0")
//					+ ") b on (b.mahasiswa = a.id) "
//					+ " where b.id is null and a.jenjang = "
//					+ ConstantValues.s3.getId() + " and a.status = "
//					+ ConstantValues.AKTIF.getId() + ") ";
//
//			System.out.println("SQL update =  " + sql);
//
//			session.createSQLQuery(sql).executeUpdate();

			session.getTransaction().commit();
			// }

			HibernateUtil.closeSession();
		}
	}

}
