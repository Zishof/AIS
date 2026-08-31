package ais.action.master.pmb;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Tabpanel;

import ais.action.master.dashboard.admin.DashboardRekapAgamaMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardRekapAsalSmaMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardRekapGelombangMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardRekapJenisInfoPertanyaanMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardRekapJenisSekolahMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardRekapJenisSeleksiCalonMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardRekapJenisSeleksiMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardRekapJurusanMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardRekapKecamatanMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardRekapKotaMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardRekapPekerjaanMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardRekapPendapatanMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardRekapPendidikanMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardRekapProgramMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardRekapSkorMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardRekapStatusAwalMahasiswaMahasiswaBaru;
import ais.action.master.dashboard.admin.DashboardRekapTahunKelulusanMahasiswaBaru;
import ais.common.Common;

/**
 * Controller/action ZK untuk rekap rekap. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Tabpanel rekapitulasiGelombang}, {@code
 * Tabpanel rekapitulasi}, {@code Tabpanel rekapitulasiJenisSeleksi}, {@code Tabpanel rekapitulasiKota}, {@code
 * Tabpanel rekapitulasiKecamatan}, {@code Tabpanel rekapitulasiAgama}, {@code Tabpanel rekapitulasiAsalSma},
 * {@code Tabpanel rekapitulasiTahunKelulusan}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}); operasi domain lain ({@code onRekapitulasiGelombang()}, {@code onRekapitulasi()}, {@code
 * onRekapitulasiJenisSeleksi()}, {@code onRekapitulasiKota()}, {@code onRekapitulasiKecamatan()}, {@code
 * onRekapitulasiAgama()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class RekapRekapAction extends GenericAutowireComposer {

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
		super.doAfterCompose(comp);
		Common.initLaguage();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRekapitulasiCalonJenisSeleksi(arg0);
			}
		});
	}

	private Tabpanel rekapitulasiGelombang;

	public void onRekapitulasiGelombang(Event event) {
		if (rekapitulasiGelombang.getChildren().isEmpty()) {
			DashboardRekapGelombangMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapGelombangMahasiswaBaru();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanRekapitulasiPA, rekapitulasiGelombang,
				"Rekap per Gelombang", "Jumlah calon mahasiswa baru yang mendaftar pada setiap gelombang penerimaan.");
		}
	}

	private Tabpanel rekapitulasi;

	public void onRekapitulasi(Event event) {
		if (rekapitulasi.getChildren().isEmpty()) {
			DashboardRekapJenisSekolahMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapJenisSekolahMahasiswaBaru();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanRekapitulasiPA, rekapitulasi,
				"Rekap Jenis Sekolah", "Asal jenis sekolah mahasiswa baru (SMA, SMK, MA, dll.).");
		}
	}

	private Tabpanel rekapitulasiJenisSeleksi;

	public void onRekapitulasiJenisSeleksi(Event event) {
		if (rekapitulasiJenisSeleksi.getChildren().isEmpty()) {
			DashboardRekapJenisSeleksiMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapJenisSeleksiMahasiswaBaru();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanRekapitulasiPA, rekapitulasiJenisSeleksi,
				"Rekap Jenis Seleksi", "Jalur seleksi yang digunakan mahasiswa baru untuk masuk (reguler, undangan, dll.).");
		}
	}

	private Tabpanel rekapitulasiKota;

	public void onRekapitulasiKota(Event event) {
		if (rekapitulasiKota.getChildren().isEmpty()) {
			DashboardRekapKotaMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapKotaMahasiswaBaru();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanRekapitulasiPA, rekapitulasiKota,
				"Rekap per Kota", "Sebaran asal kota mahasiswa baru yang mendaftar.");
		}
	}

	private Tabpanel rekapitulasiKecamatan;

	public void onRekapitulasiKecamatan(Event event) {
		if (rekapitulasiKecamatan.getChildren().isEmpty()) {
			DashboardRekapKecamatanMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapKecamatanMahasiswaBaru();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanRekapitulasiPA, rekapitulasiKecamatan,
				"Rekap per Kecamatan", "Sebaran asal kecamatan mahasiswa baru yang mendaftar.");
		}
	}

	private Tabpanel rekapitulasiAgama;

	public void onRekapitulasiAgama(Event event) {
		if (rekapitulasiAgama.getChildren().isEmpty()) {
			DashboardRekapAgamaMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapAgamaMahasiswaBaru();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanRekapitulasiPA, rekapitulasiAgama,
				"Rekap Agama", "Komposisi agama mahasiswa baru yang mendaftar.");
		}
	}


	private Tabpanel rekapitulasiAsalSma;

	public void onRekapitulasiAsalSma(Event event) {
		if (rekapitulasiAsalSma.getChildren().isEmpty()) {
			DashboardRekapAsalSmaMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapAsalSmaMahasiswaBaru();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanRekapitulasiPA, rekapitulasiAsalSma,
				"Rekap Asal SMA", "Sekolah menengah atas asal mahasiswa baru terbanyak mendaftar.");
		}
	}

	private Tabpanel rekapitulasiTahunKelulusan;

	public void onRekapitulasiTahunKelulusan(Event event) {
		if (rekapitulasiTahunKelulusan.getChildren().isEmpty()) {
			DashboardRekapTahunKelulusanMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapTahunKelulusanMahasiswaBaru();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanRekapitulasiPA, rekapitulasiTahunKelulusan,
				"Rekap Tahun Kelulusan", "Sebaran tahun kelulusan SMA mahasiswa baru yang mendaftar.");
		}
	}

	private Tabpanel rekapitulasiProgram;

	public void onRekapitulasiProgram(Event event) {
		if (rekapitulasiProgram.getChildren().isEmpty()) {
			DashboardRekapProgramMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapProgramMahasiswaBaru();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanRekapitulasiPA, rekapitulasiProgram,
				"Rekap Program Studi", "Jumlah peminat per program studi pada penerimaan mahasiswa baru.");
		}
	}

	private Tabpanel rekapitulasiStatusAwalMahasiswa;

	public void onRekapitulasiStatusAwalMahasiswa(Event event) {
		if (rekapitulasiStatusAwalMahasiswa.getChildren().isEmpty()) {
			DashboardRekapStatusAwalMahasiswaMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapStatusAwalMahasiswaMahasiswaBaru();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanRekapitulasiPA, rekapitulasiStatusAwalMahasiswa,
				"Rekap Status Awal", "Status awal mahasiswa baru (aktif, cuti, mengundurkan diri, dll.).");
		}
	}


	private Tabpanel rekapitulasiSkor;

	public void onRekapitulasiSkor(Event event) {
		if (rekapitulasiSkor.getChildren().isEmpty()) {
			DashboardRekapSkorMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapSkorMahasiswaBaru();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanRekapitulasiPA, rekapitulasiSkor,
				"Rekap Skor Seleksi", "Distribusi skor hasil seleksi calon mahasiswa baru.");
		}
	}

	private Tabpanel rekapitulasiInfoKampusDariMana;

	public void onRekapitulasiInfoKampusDariMana(Event event) {
		if (rekapitulasiInfoKampusDariMana.getChildren().isEmpty()) {
			DashboardRekapJenisInfoPertanyaanMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapJenisInfoPertanyaanMahasiswaBaru();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanRekapitulasiPA, rekapitulasiInfoKampusDariMana,
				"Rekap Info Kampus", "Sumber informasi yang membuat calon mahasiswa mengenal kampus ini.");
		}
	}

	private Tabpanel rekapitulasiPekerjaan;

	public void onRekapitulasiPekerjaan(Event event) {
		if (rekapitulasiPekerjaan.getChildren().isEmpty()) {
			DashboardRekapPekerjaanMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapPekerjaanMahasiswaBaru();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanRekapitulasiPA, rekapitulasiPekerjaan,
				"Rekap Pekerjaan Orang Tua", "Latar belakang pekerjaan orang tua mahasiswa baru.");
		}
	}

	private Tabpanel rekapitulasiPendidikan;

	public void onRekapitulasiPendidikan(Event event) {
		if (rekapitulasiPendidikan.getChildren().isEmpty()) {
			DashboardRekapPendidikanMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapPendidikanMahasiswaBaru();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanRekapitulasiPA, rekapitulasiPendidikan,
				"Rekap Pendidikan Orang Tua", "Tingkat pendidikan orang tua mahasiswa baru.");
		}
	}

	private Tabpanel rekapitulasiPendapatan;

	public void onRekapitulasiPendapatan(Event event) {
		if (rekapitulasiPendapatan.getChildren().isEmpty()) {
			DashboardRekapPendapatanMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapPendapatanMahasiswaBaru();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanRekapitulasiPA, rekapitulasiPendapatan,
				"Rekap Pendapatan Orang Tua", "Kelompok pendapatan orang tua mahasiswa baru.");
		}
	}

	private Tabpanel rekapitulasiJurusan;

	public void onRekapitulasiJurusan(Event event) {
		if (rekapitulasiJurusan.getChildren().isEmpty()) {
			DashboardRekapJurusanMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapJurusanMahasiswaBaru();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanRekapitulasiPA, rekapitulasiJurusan,
				"Rekap per Jurusan", "Jumlah mahasiswa baru yang diterima di setiap program studi.");
		}
	}

	private Tabpanel rekapitulasiCalonJenisSeleksi;

	public void onRekapitulasiCalonJenisSeleksi(Event event) {
		if (rekapitulasiCalonJenisSeleksi.getChildren().isEmpty()) {
			DashboardRekapJenisSeleksiCalonMahasiswaBaru laporanRekapitulasiPA = new DashboardRekapJenisSeleksiCalonMahasiswaBaru();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporanRekapitulasiPA, rekapitulasiCalonJenisSeleksi,
				"Rekap Seleksi Calon", "Jalur seleksi yang digunakan calon mahasiswa baru dalam pendaftaran.");
		}
	}

}
