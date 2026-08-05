package ais.action.master.dashboard;

import java.util.TreeMap;

import ais.action.master.dashboard.admin.DashboardLogAktifitasPengguna;
import ais.action.master.dashboard.admin.DashboardRekapJenisSekolahMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardRekapKunjunganPengguna;
import ais.action.master.dashboard.admin.DashboardRekapMenuPengguna;
import ais.action.master.dashboard.admin.DashboardRekapPengambilanKRSMahasiswa;
import ais.action.master.dashboard.admin.DashboardRekapPerkuliahan;
import ais.action.master.dashboard.admin.DashboardRekapStatusMahasiswa;
import ais.action.master.dashboard.admin.DashboardStatistikJenisSekolahMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardStatistikJumlahMahasiswa;
import ais.action.master.dashboard.admin.DashboardStatistikKunjunganPengguna;
import ais.action.master.dashboard.admin.DashboardStatistikPengambilanKRSMahasiswa;
import ais.action.master.dashboard.admin.DashboardStatistikPenilaian;
import ais.action.master.dashboard.admin.DashboardStatistikPerkuliahan;
import ais.action.master.dashboard.admin.DashboardStatistikStatusMahasiswa;
import ais.action.master.dashboard.keuangan.DashboardPerbandinganSudahMembayarDanBelum;
import ais.action.master.dashboard.keuangan.DashboardRekapPembayaranMahasiswa;
import ais.action.report.helper.akademik.LaporanDaftarHadirDosen;
import ais.action.report.helper.akademik.LaporanRekapPenilaianMahasiswaWindow;
import ais.action.report.helper.keuangan.LaporanRekapHostToHostWindow;
import ais.action.report.helper.keuangan.LaporanRekapMahasiswaSudahBayarWindow;
import ais.action.report.helper.nilai.LaporanDaftarNilaiWindow;
import ais.action.report.helper.nilai.LaporanDaftarPrestasiBelajarWindow;
import ais.common.Common;

public class CommonDashboard {

	@SuppressWarnings("rawtypes")
	public static final TreeMap<String, Class> masterDashboard = new TreeMap<String, Class>();

	static {
		/**
		 * Buat Admin
		 */
		masterDashboard.put("Aktifitas Pengguna",
				DashboardLogAktifitasPengguna.class);
		masterDashboard.put("Statistik Jenis Asal Sekolah Mahasiswa Baru",
				DashboardStatistikJenisSekolahMahasiswaBaru.class);
		masterDashboard.put("Rekap Jenis Asal Sekolah Mahasiswa Baru",
				DashboardRekapJenisSekolahMahasiswaBaru.class);

		masterDashboard.put("Rekap Kunjungan Pengguna",
				DashboardRekapKunjunganPengguna.class);
		masterDashboard.put("Rekap Menu Pengguna",
				DashboardRekapMenuPengguna.class);

		masterDashboard.put(
				"Rekap Pengambilan " + Common.getBahasa("label_krs"),
				DashboardRekapPengambilanKRSMahasiswa.class);
		masterDashboard.put("Rekap Perkuliahan",
				DashboardRekapPerkuliahan.class);
		masterDashboard.put("Rekap Status Mahasiswa",
				DashboardRekapStatusMahasiswa.class);
		masterDashboard.put("Statistik Jumlah Mahasiswa",
				DashboardStatistikJumlahMahasiswa.class);
		masterDashboard.put("Statistik Kunjungan Pengguna",
				DashboardStatistikKunjunganPengguna.class);
		masterDashboard.put(
				"Statistik Pengambilan " + Common.getBahasa("label_krs"),
				DashboardStatistikPengambilanKRSMahasiswa.class);
		masterDashboard.put("Statistik Penilaian",
				DashboardStatistikPenilaian.class);
		masterDashboard.put("Statistik Perkuliahan",
				DashboardStatistikPerkuliahan.class);
		masterDashboard.put("Statistik Status Mahasiswa",
				DashboardStatistikStatusMahasiswa.class);

		masterDashboard
				.put("Jadwal Hadir Dosen", LaporanDaftarHadirDosen.class);
		masterDashboard.put("Rekap Penilaian Mahasiswa",
				LaporanRekapPenilaianMahasiswaWindow.class);
		masterDashboard.put("Nilai Rata-Rata", LaporanDaftarNilaiWindow.class);
		masterDashboard.put("Prestasi Belajar",
				LaporanDaftarPrestasiBelajarWindow.class);

		/**
		 * Buat Keuangan
		 */
		masterDashboard.put("Perbandingan Sudah Dan Belum Membayar",
				DashboardPerbandinganSudahMembayarDanBelum.class);
		masterDashboard.put("Rekap Pembayaran Mahasiswa",
				DashboardRekapPembayaranMahasiswa.class);
		masterDashboard.put("Rekapitulasi Pembayaran",
				LaporanRekapHostToHostWindow.class);
		masterDashboard.put("Rekap Mahasiswa Belum Bayar",
				LaporanRekapHostToHostWindow.class);
		masterDashboard.put("Rekap Mahasiswa Sudah Bayar",
				LaporanRekapMahasiswaSudahBayarWindow.class);
	}


	public static String dashboardDescription(String text) {
		return ais.ui.util.DashboardModernHtmlUtil.description(text);
	}

	public static String dashboardCard(String title, String value, String note) {
		return ais.ui.util.DashboardModernHtmlUtil.card(title, value, note);
	}

	public static void closeOpenedSession(org.hibernate.Session session) {
		ais.ui.util.DashboardModernHtmlUtil.closeOpenedSession(session);
	}
}
