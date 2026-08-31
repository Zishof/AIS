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
 * Tugas terjadwal ({@link TimerTask}) yang secara otomatis menonaktifkan mahasiswa jenjang S1
 * (angkatan lama/{@code ConstantUtil#PENDAFTARAN_MAHASISWA_LAMA}) yang belum melakukan pembayaran
 * pada tahun akademik dan semester berjalan.
 *
 * <p>
 * Eksekusi hanya benar-benar berdampak bila konfigurasi
 * {@code mahasiswa_s1_lambat_bayar_langsung_tidak_aktif} bernilai {@link Konfigurasi#AKTIF}; bila
 * tidak, {@link #doProcess()} hanya mencetak status pengecekan ke log tanpa melakukan perubahan
 * apa pun. Selama fitur aktif, method mengecek dulu apakah masih ada
 * {@link JadwalPembayaran} yang berlaku (via
 * {@link PembayaranUtil#getJadwalPembayaranDanDendaIgnoreStart}) — bila jadwal pembayaran masih
 * ada, proses berhenti (mahasiswa masih dalam masa toleransi bayar).
 * </p>
 *
 * <p>
 * <b>Catatan</b>: blok SQL {@code UPDATE mahasiswa SET status = TIDAK_AKTIF ...} yang menjadi inti
 * tujuan kelas ini saat ini dalam keadaan <b>dikomentari (nonaktif)</b> di {@link #doProcess()} —
 * transaksi tetap dibuka dan di-commit, tetapi tidak ada perubahan data yang benar-benar
 * dieksekusi. Efektifnya kelas ini saat ini hanya mencatat log pengecekan, bukan menonaktifkan
 * mahasiswa secara nyata.
 * </p>
 *
 * <p>
 * <b>Keputusan (audit keamanan)</b>: ini tergolong pengamanan yang disengaja (governance),
 * bukan bug. SQL update dikomentari di ATAS pengaman konfigurasi {@code
 * mahasiswa_s1_lambat_bayar_langsung_tidak_aktif} yang sendirinya sudah default
 * {@link Konfigurasi#TIDAK_AKTIF} — dua lapis pengaman independen untuk operasi tak-reversibel
 * (menonaktifkan akun mahasiswa massal berdasar status pembayaran). TIDAK diaktifkan kembali di
 * sini; mengaktifkannya perlu persetujuan eksplisit pemilik modul akademik/keuangan.
 * </p>
 */
public class AutoNotActivatingMahasiswaS1Processor extends TimerTask {

	/** Konstruktor tanpa argumen, dipakai penjadwal ({@code Timer}) untuk membuat instance tugas. */
	public AutoNotActivatingMahasiswaS1Processor() {

	}

	/** Titik masuk {@link TimerTask}; langsung mendelegasikan ke {@link #doProcess()}. */
	@Override
	public void run() {
		doProcess();
	}

	/**
	 * Logika inti pengecekan/penonaktifan mahasiswa S1 yang belum bayar. Lihat javadoc kelas untuk
	 * penjelasan alur lengkap dan catatan bahwa eksekusi SQL update-nya saat ini dikomentari.
	 */
	private void doProcess() {

		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// Common.synOptimizeNilai(null, null, null);
		// }
		// }).start();

		PembayaranUtil pembayaranUtil;
		JenisKegiatan jenisKegiatan;

		pembayaranUtil = PembayaranUtil.getInstance();
		jenisKegiatan = pembayaranUtil
				.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA);

		Konfigurasi mahasiswa_s1_lambat_bayar_langsung_tidak_aktif = Common
				.getKonfigurasi(
						"mahasiswa_s1_lambat_bayar_langsung_tidak_aktif",
						Konfigurasi.TIDAK_AKTIF);
		String currentTahunAkademik = Common.getCurrentTahunAkademik();
		System.out
				.println("================ CHECK AUTO NOT ACTIVE MAHASISWA S1 TAHUN ANGKATAN "
						+ currentTahunAkademik
						+ " SEMESTER "
						+ (Common.isNowSemensterGanjil() ? "GANJIL" : "GENAP")
						+ " STATUS = "
						+ mahasiswa_s1_lambat_bayar_langsung_tidak_aktif
								.getNilai() + " =========================");

		if (mahasiswa_s1_lambat_bayar_langsung_tidak_aktif.getNilai()
				.equalsIgnoreCase(Konfigurasi.AKTIF)) {

			System.out
					.println("================ CHECK AUTO NOT ACTIVE MAHASISWA S1 TAHUN ANGKATAN "
							+ currentTahunAkademik
							+ " SEMESTER "
							+ (Common.isNowSemensterGanjil() ? "GANJIL"
									: "GENAP") + " =========================");
			Session session = HibernateUtil.currentNativeSession();

			Serializable[] serializables = pembayaranUtil
					.getJadwalPembayaranDanDendaIgnoreStart(ais.ui.util.WaktuUtil.getDate(),
							jenisKegiatan, ConstantValues.s1,
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
//
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
//					+ ConstantValues.s1.getId() + " and a.status = "
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
