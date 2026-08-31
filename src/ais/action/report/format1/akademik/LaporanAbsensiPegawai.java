package ais.action.report.format1.akademik;

/**
 * Alias paket {@code akademik} untuk laporan absensi pegawai: seluruhnya mewarisi implementasi
 * dari {@link ais.action.report.format1.payroll.LaporanAbsensiPegawai} tanpa menambah perilaku
 * baru. Kelas ini hanya membuka kembali kedua konstruktor induk agar laporan yang sama dapat
 * didaftarkan/dirujuk dari lokasi paket akademik (mis. menu/registrasi laporan yang mengharapkan
 * kelas berada di bawah {@code report.format1.akademik}).
 */
@SuppressWarnings("serial")
public class LaporanAbsensiPegawai extends ais.action.report.format1.payroll.LaporanAbsensiPegawai {

	/** Membuat instans dengan konfigurasi bawaan; lihat konstruktor induk. */
	public LaporanAbsensiPegawai() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Membuat instans dengan judul, gaya border, dan status dapat-ditutup jendela laporan.
	 *
	 * @param title    judul jendela laporan
	 * @param border   gaya border jendela
	 * @param closable apakah jendela dapat ditutup pengguna
	 */
	public LaporanAbsensiPegawai(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		// TODO Auto-generated constructor stub
	}

}
