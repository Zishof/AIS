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
 * Tugas terjadwal ({@link TimerTask}) yang secara berkala mengecek apakah mahasiswa jenjang S2
 * perlu dinonaktifkan otomatis karena belum melakukan pembayaran pada tahun akademik/semester
 * berjalan. Hanya berjalan bila konfigurasi
 * {@code mahasiswa_s2_lambat_bayar_langsung_tidak_aktif} bernilai {@link Konfigurasi#AKTIF}.
 *
 * <p>
 * <b>Catatan implementasi</b> — logika inti {@link #doProcess()} mengecek keberadaan jadwal
 * pembayaran (denda inklusif, {@code ignoreStart}) untuk kegiatan pendaftaran mahasiswa lama
 * jenjang S2; bila jadwal tersebut masih ada, proses berhenti tanpa mengubah apa pun. Bila
 * jadwal sudah tidak ada, blok transaksi dibuka dan ditutup, namun pernyataan SQL
 * {@code UPDATE mahasiswa SET status = ...} yang menonaktifkan mahasiswa masih
 * <b>dikomentari (nonaktif)</b> di kode sumber — sehingga saat ini method tidak benar-benar
 * mengubah status mahasiswa mana pun walaupun kondisinya terpenuhi. Kemungkinan sengaja
 * dimatikan sementara untuk keperluan pengujian/keamanan, atau fitur belum selesai diaktifkan.
 * </p>
 */
public class AutoNotActivatingMahasiswaS2Processor extends TimerTask {

	/** Konstruktor kosong; tidak ada state yang perlu diinisialisasi. */
	public AutoNotActivatingMahasiswaS2Processor() {

	}

	/** Dipanggil oleh {@link java.util.Timer} sesuai jadwal; mendelegasikan ke {@link #doProcess()}. */
	@Override
	public void run() {
		doProcess();
	}

	/**
	 * Mengecek jadwal pembayaran mahasiswa S2 untuk tahun akademik/semester berjalan dan,
	 * bila konfigurasi terkait aktif serta tidak ada jadwal pembayaran tersisa, seharusnya
	 * menonaktifkan mahasiswa yang belum membayar — lihat catatan kelas di atas mengenai
	 * pernyataan SQL update yang saat ini masih dikomentari (tidak aktif).
	 */
	private void doProcess() {

		PembayaranUtil pembayaranUtil;
		JenisKegiatan jenisKegiatan;

		pembayaranUtil = PembayaranUtil.getInstance();
		jenisKegiatan = pembayaranUtil
				.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA);

		Konfigurasi mahasiswa_s2_lambat_bayar_langsung_tidak_aktif = Common
				.getKonfigurasi(
						"mahasiswa_s2_lambat_bayar_langsung_tidak_aktif",
						Konfigurasi.TIDAK_AKTIF);
		String currentTahunAkademik = Common.getCurrentTahunAkademik();
		System.out
				.println("================ CHECK AUTO NOT ACTIVE MAHASISWA S2 TAHUN ANGKATAN "
						+ currentTahunAkademik
						+ " SEMESTER "
						+ (Common.isNowSemensterGanjil() ? "GANJIL" : "GENAP")
						+ " STATUS = "
						+ mahasiswa_s2_lambat_bayar_langsung_tidak_aktif
								.getNilai() + " =========================");

		if (mahasiswa_s2_lambat_bayar_langsung_tidak_aktif.getNilai()
				.equalsIgnoreCase(Konfigurasi.AKTIF)) {

			System.out
					.println("================ CHECK AUTO NOT ACTIVE MAHASISWA S2 TAHUN ANGKATAN "
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
							ConstantValues.s2,
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
//					+ ConstantValues.s2.getId() + " and a.status = "
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
