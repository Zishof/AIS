package ais.action.master;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.database.model.Konfigurasi;

public class KonfigurasiDashboardAction extends KonfigurasiNewAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterComposeOri(comp);
		Common.initLaguage();

		onTampil();

	}

	public static final String KALENDER = "dashboard_kalender_waktu";
	public static final String PENGUMUMAN = "dashboard_pengumuman";
	public static final String DISPOSISI_MASUK = "dashboard_disposisi_masuk";
	public static final String KEHADIRAN_PEGAWAI = "dashboard_kehadiran_pegawai";
	public static final String PENGAJUAN_SURAT_KELUAR = "dashboard_pengajuan_surat_keluar";
	public static final String DISPOSISI_KELUAR = "dashboard_disposisi_keluar";
	public static final String KRS_DAN_NILAI = "dashboard_krs_dan_nilai";
	public static final String TUGAS_MAHASISWA = "dashboard_tugas_mahasiswa";
	public static final String AKSES = "dashboard_akses_pengguna";
	public static final String AKREDITASI = "dashboard_akreditasi";
	public static final String STATUS_MHS_TIAP_PRODI = "dashboard_status_mhs_tiap_prodi";
	public static final String STATUS_DSN_TIAP_PRODI = "dashboard_status_dsn_tiap_prodi";

	public static final String STATUS_MHS = "dashboard_status_mhs";
	public static final String STATUS_DSN = "dashboard_status_dsn";

	public static final String STATUS_PERKULIAHAN = "dashboard_perkuliahan_baru";
	public static final String STATUS_PENILAIAN = "dashboard_penilaian";
	public static final String STATUS_SIDANG = "dashboard_sidang";
	public static final String STATUS_WISUDA = "dashboard_wisuda";
	public static final String STATUS_BIMBINGAN = "dashboard_bimbingan";
	public static final String STATUS_KKN = "dashboard_kkn";
	public static final String STATUS_PKL = "dashboard_pkl";
	public static final String STATUS_PEMBAYARAN = "dashboard_pembayaran";
	public static final String STATUS_KRS = "dashboard_krs";
	public static final String STATUS_KRS_BELUM_DISETUJUI = "dashboard_krs_belum_disetuji";

	public static final String STATUS_MHS_BARU = "dashboard_mhs_baru";
	public static final String STATUS_ALUMNI = "dashboard_alumni";

	public static final String STATUS_KUNJUNGAN_PUSTAKA = "dashboard_kunjungan_pustaka";
	public static final String STATUS_PEMINJAMAN_PUSTAKA = "dashboard_peminjaman_pustaka";
	public static final String STATUS_PENGEMBALIAN_PUSTAKA = "dashboard_pengembalian_pustaka";

	public void onTampil() throws Exception {

		Rows rows = (createSpan("Pengaturan Tampilan di Dashboard Utama"));

		rows.appendChild(createRowActiveDefault("Dashboard Kalender / Waktu", KALENDER, Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Dashboard Pengumuman", PENGUMUMAN, Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Dashboard Disposisi Surat Masuk", DISPOSISI_MASUK, Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Dashboard Kehadiran Pegawai", KEHADIRAN_PEGAWAI, Konfigurasi.AKTIF));

		rows.appendChild(
				createRowActiveDefault("Dashboard Disposisi Surat Keluar", DISPOSISI_KELUAR, Konfigurasi.AKTIF));

		rows.appendChild(
				createRowActiveDefault("Dashboard KRS dan Nilai (untuk mahasiswa)", KRS_DAN_NILAI, Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Dashboard Pengajuan Surat (untuk dosen dan mahasiswa)",
				PENGAJUAN_SURAT_KELUAR, Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Dashboard Tugas Mahasiswa (untuk mahasiswa)", TUGAS_MAHASISWA,
				Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Dashboard Akses Pengguna", AKSES, Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Dashboard Dokumen Akreditasi", AKREDITASI, Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Dashboard Status Mahasiswa Tiap Prodi", STATUS_MHS_TIAP_PRODI,
				Konfigurasi.AKTIF));

		rows.appendChild(
				createRowActiveDefault("Dashboard Dosen Tiap Prodi", STATUS_DSN_TIAP_PRODI, Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Dashboard Status Mahasiswa", STATUS_MHS, Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Dashboard Status Dosen", STATUS_DSN, Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Dashboard Perkuliahan", STATUS_PERKULIAHAN, Konfigurasi.TIDAK_AKTIF));

		rows.appendChild(createRowActiveDefault("Dashboard Penilaian", STATUS_PENILAIAN, Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("KKN", KonfigurasiDashboardAction.STATUS_KKN, Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("PKL", KonfigurasiDashboardAction.STATUS_PKL, Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Pengajuan Bimbingan", KonfigurasiDashboardAction.STATUS_BIMBINGAN,
				Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Pengajuan Sidang", KonfigurasiDashboardAction.STATUS_SIDANG,
				Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Pengajuan Wisuda", KonfigurasiDashboardAction.STATUS_WISUDA,
				Konfigurasi.AKTIF));

		rows.appendChild(
				createRowActiveDefault("Dashboard Pembayaran Mahasiswa", STATUS_PEMBAYARAN, Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Dashboard KRS", STATUS_KRS, Konfigurasi.AKTIF));
		rows.appendChild(
				createRowActiveDefault("Dashboard KRS belum disetujui", STATUS_KRS_BELUM_DISETUJUI, Konfigurasi.AKTIF));
		rows.appendChild(createRowActiveDefault("Dashboard Status Mahasiswa Baru", STATUS_MHS_BARU, Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Dashboard Status Alumni", STATUS_ALUMNI, Konfigurasi.AKTIF));

		rows.appendChild(
				createRowActiveDefault("Dashboard Kunjungan Perpustakan", STATUS_KUNJUNGAN_PUSTAKA, Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Dashboard Peminjaman Perpustakan", STATUS_PEMINJAMAN_PUSTAKA,
				Konfigurasi.AKTIF));

		rows.appendChild(createRowActiveDefault("Dashboard Pengembalian Perpustakan", STATUS_PENGEMBALIAN_PUSTAKA,
				Konfigurasi.AKTIF));

	}
}
