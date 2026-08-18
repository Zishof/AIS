package ais.action.master;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.joda.time.Years;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.dashboard.admin.DashboardKegiatanKemahasiswaan;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataKecamatanBanbox;
import ais.action.master.helper.AmbilDataNamaSekolahBanbox;
import ais.action.master.helper.DetailArtikelHelper;
import ais.action.master.helper.HasilUjianHelper;
import ais.action.master.helper.MainHelper;
import ais.action.master.helper.ParameterTambahanAlumniListener;
import ais.action.master.helper.ParameterTambahanMahasiswaListener;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.penelitiandanpengabdian.helper.PengajuanPenelitianDanPengabdianHelper;
import ais.action.report.CommonReportHelper;
import ais.action.report.format1.akademik.LaporanKartuMahasiswa;
import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Agama;
import ais.database.model.AlatTransportasiMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.JenisSekolahMahasiswaBaru;
import ais.database.model.JenisTinggalMahasiswa;
import ais.database.model.Jenjang;
import ais.database.model.Konfigurasi;
import ais.database.model.Kota;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.NamaSekolahAsal;
import ais.database.model.NilaiToeflToaflMahasiswa;
import ais.database.model.OperatorSeluler;
import ais.database.model.Pekerjaan;
import ais.database.model.PekerjaanOrangTua;
import ais.database.model.PendapatanOrangTua;
import ais.database.model.PendidikanOrangTua;
import ais.database.model.Penghasilan;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Propinsi;
import ais.database.model.Skripsi;
import ais.database.model.StatusDomisiliSetelahLulus;
import ais.database.model.StatusPekerjaanSetelahLulus;
import ais.database.model.StatusSetelahLulus;
import ais.database.model.Tbmuser;
import ais.database.model.Wilayah;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.LampiranLainMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class BiodataMahasiswaAction extends GenericAutowireComposer {

	public final static String[] DEFAULT_TIDAK_WAJIB = new String[] { "namaArab", "namaTionghoa", "npsn",
			"apakahPernahPaud", "apakahPernahTk", "jenisTinggalMahasiswa", "ukuranJaket", "pernahMenetapDiLuarNegeri",
			"tinggiBadan", "beratBadan", "hp", "operatorSeluler", "alatTransportasiMahasiswa", "suratIzinMengemudi",
			"pernahMemimpinOrganisasi", "namaOrganisasi", "hobi", "minatSeni", "kemampuanBahasa1", "kemampuanBahasa2",
			"kemampuanBahasa3", "golonganDarah", "noKK", "nisn", "nirm", "npwp", "nikAyah", "nikIbu", "telpAyah",
			"telpIbu", "telpWali", "kodeKerjaan", "no_rek_bri", "cabangBri" };

	public final static String[] DATA = new String[] { "namaArab", "namaTionghoa", "namaUntukIjazah", "noIjazah", "hp",
			"operatorSeluler", "jenisSekolah", "asalSma", "npsn", "alamatAsalSma", "asalSmp", "alamatAsalSmp", "asalSd",
			"alamatAsalSd",

			"apakahPernahPaud", "apakahPernahTk", "jenisTinggalMahasiswa", "ukuranJaket", "pernahMenetapDiLuarNegeri",
			"tinggiBadan", "beratBadan", "hp", "alatTransportasiMahasiswa", "suratIzinMengemudi",
			"pernahMemimpinOrganisasi", "namaOrganisasi", "hobi", "minatSeni", "kemampuanBahasa1", "kemampuanBahasa2",
			"kemampuanBahasa3", "golonganDarah", "statusNikah", "agama",

			"noIdentitas", "alamat", "dusun", "rt", "rw", "kodepos", "kelurahan", "kecamatan", "propinsi", "kota",
			"teleponRumah",

			"noKK", "namaAyah", "telpAyah", "tanggalLahirAyah", "jenisPekerjaanAyah", "jenisPenghasilanAyah",
			"jenjangPendidikanAyah", "namaIbu", "telpIbu", "tanggalLahirIbu", "jenisPekerjaanIbu",
			"jenisPenghasilanIbu", "jenjangPendidikanIbu", "bersaudara",

			"namaWali", "telpWali", "tanggalLahirWali", "jenisPekerjaanWali", "jenisPenghasilanWali",
			"jenjangPendidikanWali", "nisn", "nirm", "npwp", "nikAyah", "nikIbu", "jenisSekolah", "kodeKerjaan",
			"no_rek_bri", "cabangBri" };

	public final static String[] DATA_DESC = new String[] { "Nama Arab", "Nama Tionghoa",
			"Nama Ijazah Pendidikan Sebelumya", "Nomor Ijazah Pendidikan Sebelumya", "Nomor HP", "Nama Operator HP",
			"Jenis Pendidikan sebelumya", "Asal Pendidikan sebelumya", "NPSN Pendidikan sebelumya",
			"Alamat Asal Pendidikan sebelumya", "Asal Smp", "Alamat Asal SMP", "Asal SD", "Alamat Asal SD",

			"Pernah PAUD (Pendidikan Anak Usia Dini)", "Pernah TK (Taman kanak-Kanak)",
			"Jenis Tempat Tinggal Mahasiswa (Bersama orang tua, Wali, Kost, Asrama)", "Ukuran Jaket",
			"Pernah Menetap di Luar Negeri", "Tinggi Badan (Cm)", "Berat Badan (Kg)", "HP",
			"Alat Transportasi Mahasiswa Saat Kuliah", "Nomor Surat Ijin Mengemudi",
			"Organisasi Intra Kampus yang diikuti", "Jabatan dalam organisasi mahasiswa", "Hobi", "Minat Seni",
			"Kemampuan Bahasa 1", "Kemampuan Bahasa 2", "Kemampuan Bahasa 3", "Golongan Darah", "Status Pernikahan",
			"Agama",

			"Nomor Identitas Diri (KTP)", "Alamat", "Dusun / Kampung", "RT", "RW", "Kode Pos", "Kelurahan", "Kecamatan",
			"Propinsi", "Kota/Kabupaten", "Telepon Rumah, jika tidak ada, nomor yang bisa dihubungi",

			"No. Kartu Keluarga (KK)", "Nama Ayah", "Telpon/HP Ayah", "Tanggal Lahir Ayah", "Jenis Pekerjaan Ayah",
			"Rata-rata Penghasilan Ayah", "Jenjang Pendidikan Ayah", "Nama Ibu", "Telpon/HP Ibu", "Tanggal Lahir Ibu",
			"Jenis Pekerjaan Ibu", "Rata-rata Penghasilan Ibu", "Jenjang Pendidikan Ibu",
			"Jumlah anggota keluarga (termasuk ayah dan ibu jika masih ada)",

			"Nama Wali", "Telpon/HP Wali", "Tanggal Lahir Wali", "Jenis Pekerjaan Wali", "Rata-rata penghasilan Wali ",
			"Jenjang Pendidikan Wali ", "Nomor Induk Siswa Nasional (NISN)", "Nomor Induk Registrasi Mahasiswa (NIRM)",
			"NPWP", "No. KTP Ayah", "No. KTP Ibu", "Asal pendidikan sebelumnya", "Nama Bank", "No. Rekening Bank",
			"Atas Nama Rekening Bank" };

	public static void displayBiodataWindow() throws Exception {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final MyWindow window = new MyWindow();
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("95%");

				window.setWidth("99%");
				window.setClosable(false);

				MyInclude include = new MyInclude("/pages/master/biodata_mahasiswa.zul?refresh=true");
				include.setParent(window);
				include.setWidth("100%");
				include.setHeight("100%");

				window.onModal();
			}
		});
	}

	public static boolean checkEmailMahasiswa(Mahasiswa mahasiswa) throws Exception {
		if (mahasiswa.getEmail() == null || mahasiswa.getEmail().trim().isEmpty()
				|| !Common.isValidEmailAddress(mahasiswa.getEmail().trim())) {
			MyMessageboxConfig.show("Email harus diisi dan format email harus benar", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							// TODO Auto-generated method stub
							displayBiodataWindow();
						}
					});
			return false;
		}
		return true;
	}

	public static boolean checkBiodataMahasiswa(Mahasiswa mahasiswa) throws Exception {

		try {

//			if (!LampiranLainMahasiswa.checkLampiran(mahasiswa, new EventListener() {
//
//				@Override
//				public void onEvent(Event arg0) throws Exception {
//					displayBiodataWindow();
//				}
//			})) {
//
//				return false;
//			}

			BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
			if (biodataMahasiswa == null) {
				MyMessageboxConfig.show("Biodata Anda harus dilengkapi", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								// TODO Auto-generated method stub
								displayBiodataWindow();
							}
						});
				return false;
			}

			if (mahasiswa.getStatusKeluar() != null && mahasiswa.getStatusSetelahLulus() == null) {
				MyMessageboxConfig.show(
						"Anda telah dinyatakan " + mahasiswa.getStatusKeluar().getNama()
								+ ", Informasi Kelulusan harus dilengkapi pada Status Setelah Lulus",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								// TODO Auto-generated method stub
								displayBiodataWindow();
							}
						});
				return false;
			}

			if (mahasiswa.getStatusKeluar() != null && mahasiswa.getStatusPekerjaanSetelahLulus() == null) {
				MyMessageboxConfig.show(
						"Anda telah dinyatakan " + mahasiswa.getStatusKeluar().getNama()
								+ ", Informasi Kelulusan harus dilengkapi pada Status Pekerjaan Setelah Lulus",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								// TODO Auto-generated method stub
								displayBiodataWindow();
							}
						});
				return false;
			}

			if (mahasiswa.getStatusKeluar() != null && mahasiswa.getStatusDomisiliSetelahLulus() == null) {
				MyMessageboxConfig.show(
						"Anda telah dinyatakan " + mahasiswa.getStatusKeluar().getNama()
								+ ", Informasi Kelulusan harus dilengkapi pada Status Domisili Setelah Lulus",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								// TODO Auto-generated method stub
								displayBiodataWindow();
							}
						});
				return false;
			}

			if (Common.bolehKonfigurasi("ktp_mahasiswa_harus_16_karakter", Konfigurasi.TIDAK_AKTIF)) {
				if (biodataMahasiswa.getNoIdentitas().length() != 16
						&& mahasiswa.getWarganegara().equals(Mahasiswa.WNI)) {
					MyMessageboxConfig.show("Nomor KTP harus diisi tepat 16 digit tanpa tanda baca.", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									displayBiodataWindow();
								}
							});
					return false;
				}
			}

			if (Common.bolehKonfigurasi("nisn_mahasiswa_harus_10_karakter", Konfigurasi.TIDAK_AKTIF)) {
				if (biodataMahasiswa.getNisn().length() != 10 && mahasiswa.getWarganegara().equals(Mahasiswa.WNI)) {
					MyMessageboxConfig.show("NISN harus diisi tepat 10 digit tanpa tanda baca.", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									displayBiodataWindow();
								}
							});
					return false;
				}
			}

			if (Common.bolehKonfigurasi("mahasiswa_bisa_memilih_dosen_pa_sendiri", Konfigurasi.TIDAK_AKTIF)) {

				if (mahasiswa.getDosen() == null) {
					MyMessageboxConfig.show(
							"Sistem memperbolehkan Anda memilih dosen pembimbing akademik. Pilihlah dosen pembimbing Akademik Anda.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									displayBiodataWindow();
								}
							});
					return false;
				}
			}

			List<String> daftarWajibDiisi = KonfigurasiTampilanBiodataMahasiswaAction.dataYangWajibDiisi();
			for (String key : daftarWajibDiisi) {
				if (Common.checkIsNull(BiodataMahasiswa.class, biodataMahasiswa, key)) {

					MyMessageboxConfig.show(
							"Biodata Anda harus dilengkapi. Data \""
									+ KonfigurasiTampilanBiodataMahasiswaAction.keyDesc(key)
									+ "\" masih belum terisi dengan benar.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									displayBiodataWindow();
								}
							});
					return false;
				}
			}

			String[] DATA_MHS = new String[] { "tanggallahir", "tempatlahir" };
			String[] DATA_MHS_DESC = new String[] { "Tanggal lahir", "Tempat lahir" };

			for (int i = 0; i < DATA_MHS.length; i++) {
				if (Common.checkIsNull(Mahasiswa.class, mahasiswa, DATA_MHS[i])) {
					MyMessageboxConfig.show(
							"Biodata Anda harus dilengkapi. Data \"" + DATA_MHS_DESC[i] + "\" masih belum terisi",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									displayBiodataWindow();
								}
							});
					return false;
				}
			}

			Date tanggalLahir = mahasiswa.getTanggallahir();
			if (tanggalLahir != null) {
				int selisih = Math.abs(
						Years.yearsBetween(new DateTime(tanggalLahir), new DateTime(ais.ui.util.WaktuUtil.getDate()))
								.getYears());
				if (selisih <= 10) {
					MyMessageboxConfig.show(
							"Tanggal lahir Anda \"" + Common.dateFormat1.get().format(tanggalLahir)
									+ "\" tidak sesuai. Tanggal lahir harus disesuaikan dengan waktu Anda lahir",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									displayBiodataWindow();
								}
							});
					return false;
				}
			}

			if (Common.bolehKonfigurasi("foto_mahasiswa_wajib_diisi")) {
				FileFotoLain fileFotoLain = FileFotoLain.ambil(false, mahasiswa.getId(), FotoMahasiswa.DEFAULT_JENIS,
						FotoMahasiswa.class);
				if (fileFotoLain == null) {
					MyMessageboxConfig.show("Foto Anda harus dilengkapi. Ukuran maksimal 1 Mb.", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									displayBiodataWindow();
								}
							});
					return false;
				}
			}

			if (Common.bolehKonfigurasi("upload_ktp_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF)) {
				FileFotoLain fileFotoLain = FileFotoLain.ambil(false, mahasiswa.getId(), LampiranLainMahasiswa.KTP,
						LampiranLainMahasiswa.class);
				if (fileFotoLain == null) {
					MyMessageboxConfig.show(
							"Upload KTP/NIK wajib dilakukan, pilihlah di tab Alamat dan klik Upload KTP/NIK. Ukuran maksimal 1 Mb.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									displayBiodataWindow();
								}
							});
					return false;
				}
			}

			if (Common.bolehKonfigurasi("upload_akte_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF)) {
				FileFotoLain fileFotoLain = FileFotoLain.ambil(false, mahasiswa.getId(), LampiranLainMahasiswa.AKTE,
						LampiranLainMahasiswa.class);
				if (fileFotoLain == null) {
					MyMessageboxConfig.show(
							"Upload akte kelahiran wajib dilakukan, pilihlah di tab Alamat dan klik Upload Akte Kelahiran. Ukuran maksimal 1 Mb.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									displayBiodataWindow();
								}
							});
					return false;
				}
			}

			if (Common.bolehKonfigurasi("upload_npwp_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF)) {
				FileFotoLain fileFotoLain = FileFotoLain.ambil(false, mahasiswa.getId(), LampiranLainMahasiswa.NPWP,
						LampiranLainMahasiswa.class);
				if (fileFotoLain == null) {
					MyMessageboxConfig.show("Upload NPWP wajib dilakukan. Ukuran maksimal 1 Mb.", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									displayBiodataWindow();
								}
							});
					return false;
				}
			}

			if (Common.bolehKonfigurasi("upload_ijazah_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF)) {
				FileFotoLain fileFotoLain = FileFotoLain.ambil(false, mahasiswa.getId(), LampiranLainMahasiswa.IJAZAH,
						LampiranLainMahasiswa.class);
				if (fileFotoLain == null) {
					MyMessageboxConfig.show(
							"Upload ijazah pendidikan sebelumnya wajib dilakukan, Ukuran maksimal 1 Mb.", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									displayBiodataWindow();
								}
							});
					return false;
				}
			}

			if (Common.bolehKonfigurasi("upload_transkrip_nilai_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF)) {
				FileFotoLain fileFotoLain = FileFotoLain.ambil(false, mahasiswa.getId(),
						LampiranLainMahasiswa.TRANSKRIP_NILAI, LampiranLainMahasiswa.class);
				if (fileFotoLain == null) {
					MyMessageboxConfig.show(
							"Upload Transkrip Nilai Pendidikan Sebelumnya wajib dilakukan, Ukuran maksimal 1 Mb.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									displayBiodataWindow();
								}
							});
					return false;
				}
			}

			if (Common.bolehKonfigurasi("upload_kk_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF)) {
				FileFotoLain fileFotoLain = FileFotoLain.ambil(false, mahasiswa.getId(), LampiranLainMahasiswa.KK,
						LampiranLainMahasiswa.class);
				if (fileFotoLain == null) {
					MyMessageboxConfig.show(
							"Upload Kartu Keluarga (KK) wajib dilakukan, pilihlah di tab Keluarga dan klik Upload Kartu Keluarga (KK). Ukuran maksimal 1 Mb.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									displayBiodataWindow();
								}
							});
					return false;
				}
			}

			if (Common.bolehKonfigurasi("upload_ktp_ayah_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF)) {
				FileFotoLain fileFotoLain = FileFotoLain.ambil(false, mahasiswa.getId(), LampiranLainMahasiswa.KTP_AYAH,
						LampiranLainMahasiswa.class);
				if (fileFotoLain == null) {
					MyMessageboxConfig.show(
							"Upload KTP Ayah wajib dilakukan, pilihlah di tab Keluarga dan klik Upload KTP Ayah. Ukuran maksimal 1 Mb.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									displayBiodataWindow();
								}
							});
					return false;
				}
			}

			if (Common.bolehKonfigurasi("upload_ktp_ibu_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF)) {
				FileFotoLain fileFotoLain = FileFotoLain.ambil(false, mahasiswa.getId(), LampiranLainMahasiswa.KTP_IBU,
						LampiranLainMahasiswa.class);
				if (fileFotoLain == null) {
					MyMessageboxConfig.show(
							"Upload KTP Ibu wajib dilakukan, pilihlah di tab Keluarga dan klik Upload KTP Ibu. Ukuran maksimal 1 Mb.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									displayBiodataWindow();
								}
							});
					return false;
				}
			}

			if (Common.bolehKonfigurasi("upload_ktp_wali_mahasiswa_wajib_diisi", Konfigurasi.TIDAK_AKTIF)) {
				FileFotoLain fileFotoLain = FileFotoLain.ambil(false, mahasiswa.getId(), LampiranLainMahasiswa.KTP_WALI,
						LampiranLainMahasiswa.class);
				if (fileFotoLain == null) {
					MyMessageboxConfig.show(
							"Upload KTP Wali wajib dilakukan, pilihlah di tab Wali dan klik Upload KTP Wali. Ukuran maksimal 1 Mb.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub
									displayBiodataWindow();
								}
							});
					return false;
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		return true;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 4155860737880329036L;
	MyWindow win;
	private Textbox tempatlahir;
	private MyDatebox tanggallahir;
	private Combobox kelamin;
	private Decimalbox pin;
	private Textbox alamat;
	private Textbox namaAyah;
	private Textbox telpAyah;
	private Textbox telpIbu;
	private Textbox telpWali;
	private Combobox pekerjaanAyah;
	private Combobox pendidikanAyah;
	private Combobox jenjangPendidikanAyah;
	private Combobox jenjangPendidikanIbu;

	private Textbox nisn;
	private Textbox npwp;
	private Textbox namaIbu;
	private Combobox pekerjaanIbu;
	private Combobox pendidikanIbu;
	private Textbox namaUntukIjazah;
	private Textbox noIjazah;
	private Textbox ukuranJaket;
	private Combobox pernahMenetapDiLuarNegeri;
	private Decimalbox tinggiBadan;
	private Decimalbox beratBadan;
	private Textbox teleponRumah;
	private Textbox hp;
	private Textbox suratIzinMengemudi;
	private Textbox kendaraanKuliah;
	private Combobox pernahMemimpinOrganisasi;
	private Textbox namaOrganisasi;
	private Textbox hobi;
	private Textbox minatSeni;
	private Textbox kemampuanBahasa1;
	private Textbox kemampuanBahasa2;
	private Textbox kemampuanBahasa3;

	private Combobox jenisSekolah;
	private Textbox npsn;
	private AmbilDataNamaSekolahBanbox asalSma;
	private Textbox alamatAsalSma;
	private AmbilDataNamaSekolahBanbox asalSmp;
	private Textbox alamatAsalSmp;
	private AmbilDataNamaSekolahBanbox asalSd;
	private Textbox alamatAsalSd;
	private Textbox golonganDarah;
	private Combobox statusNikah;

	private Combobox agama;
	private Textbox email;
	private Intbox bersaudara;
//	private Textbox noRekBri;
//	private Combobox kancaBri;
	private Textbox noKK;

	private Mahasiswa mahasiswa;

	private EventListener fotoEventListener;

	private boolean tampilFotoBiodata = true;

	private Textbox rt;
	private Textbox rw;
	private Textbox kodepos;
	private Textbox kelurahan;
	private AmbilDataKecamatanBanbox kecamatan;
	private Label propinsi;
	private Label kota;

	private Textbox noIdentitas;
	private Textbox dusun;
	private Combobox pendapatanOrtu;
	private Combobox pendapatanOrtuIbu;
	private Textbox namaWali;
	private Combobox pekerjaanWali;
	private Combobox pendapatanWali;
	private Combobox pendidikanWali;
	private MyDatebox tanggalLahirWali;
	private MyDatebox tanggalLahirAyah;
	private MyDatebox tanggalLahirIbu;
	private Boolean refresh = null;
	private Combobox jenisTinggalMahasiswa;
	private Combobox alatTransportasiMahasiswa;
	private Combobox jenisPekerjaanAyah;
	private Combobox jenisPekerjaanIbu;
	private Combobox jenisPenghasilanAyah;
	private Combobox jenisPenghasilanIbu;
	private Combobox jenisPenghasilanWali;
	private Combobox jenjangPendidikanWali;
	private Combobox jenisPekerjaanWali;
	private MyCheckboxConfig apakahPernahPaud;
	private MyCheckboxConfig apakahPernahTk;
	private Tabpanel tabpanelDataLampiran;
	private AmbilDataDosenBanbox dosen;
	private boolean terintegrasiDenganEmis;

	private Map<String, LampiranLain> lampiranLains = new HashMap<String, LampiranLain>();
	private ParameterTambahanMahasiswaListener parameterTambahanMahasiswaListener;

	// private ParameterTambahanAlumniListener parameterTambahanAlumniListener;

	private Textbox nikAyah;

	private Textbox nikIbu;

	private MyTabConfig tabDataTambahan;

	private MyTabConfig tabDataAlumni;

	private Combobox bahasa;

	private MyTextbox tanggallahirManual;

	private Textbox nirm;

	private Combobox hpProvider;

	protected FotoMahasiswa fotoMahasiswa;

	private Tbmuser tbmuser;

	private boolean bolehgantiFoto;

	private Textbox kodeKerjaan;

	private Textbox cabangBri;

	private Textbox no_rek_bri;

	private boolean mahasiswa_boleh_mengubah_ttl_dan_ibu_profile;

	protected LampiranLain ttd;

	private Textbox namaArab;

	private Textbox namaTionghoa;

	private Combobox statusSetelahLulus;

	private Combobox statusPekerjaanSetelahLulus;

	private Textbox emailAtasan;

	private Combobox statusDomisiliSetelahLulus;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		mahasiswa_boleh_mengubah_ttl_dan_ibu_profile = Common.bolehKonfigurasi("mahasiswa_boleh_mengubah_ttl_dan_ibu_profile");
		tbmuser = Common.getCurrentUser();
		if (tbmuser == null || tbmuser.getMahasiswa() == null) {
			alert("Anda harus login sebagai mahasiswa");
			return;
		}

		if (session.getAttribute("fotoEventListener") != null) {
			fotoEventListener = (EventListener) session.getAttribute("fotoEventListener");
			session.removeAttribute("fotoEventListener");
		}

		if (execution.getParameter("refresh") != null) {
			refresh = Boolean.parseBoolean(execution.getParameter("refresh"));
		}

		win = (MyWindow) comp;
		bolehgantiFoto = true;
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			bolehgantiFoto = Common.bolehKonfigurasi("mahasiswa_boleh_mengubah_foto_profile");
		}
		mahasiswa = tbmuser.getMahasiswa();

		init();

	}

	public Borderlayout initData(BiodataMahasiswa biodataMahasiswa) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setStyle("border:0px;");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");


		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(RevisiHelper.createNewRevisi(BiodataMahasiswa.class, biodataMahasiswa, mahasiswa.getNim()));

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(new ais.ui.util.MyLabelConfig(mahasiswa.getJurusan().getNama()));

		row = new MyFormRow();


		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(new ais.ui.util.MyLabelConfig(mahasiswa.getNama()));

		row = new MyFormRow();

		String statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("namaArab");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nama Dalam Karakter Arab " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(namaArab = new Textbox(mahasiswa.getNamaArab()));
		namaArab.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("namaTionghoa");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nama Dalam Karakter Tionghoa " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(namaTionghoa = new Textbox(mahasiswa.getNamaTionghoa()));
		namaTionghoa.setWidth("90%");

		row = new MyFormRow();

				row.setVisible(
				tbmuser != null && tbmuser.getMahasiswa() != null && mahasiswa_boleh_mengubah_ttl_dan_ibu_profile);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat / Tanggal Lahir"));
		Box hbox = Common.isMobile() ? new Vbox() : new Hbox();
		hbox.appendChild(
				tempatlahir = new Textbox(mahasiswa.getTempatlahir() == null ? "" : mahasiswa.getTempatlahir()));
		hbox.appendChild(tanggallahir = new MyDatebox(
				mahasiswa.getTanggallahir() == null ? ais.ui.util.WaktuUtil.getDate() : mahasiswa.getTanggallahir()));
		row.appendChild(hbox);
		tempatlahir.setCols(10);
		tanggallahir.setCols(10);

		row = new MyFormRow();

				row.setVisible(
				!(tbmuser != null && tbmuser.getMahasiswa() != null) || !mahasiswa_boleh_mengubah_ttl_dan_ibu_profile);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat / Tanggal Lahir"));
		hbox = Common.isMobile() ? new Vbox() : new Hbox();
		hbox.appendChild(new Label(mahasiswa.getTempatlahir() == null ? "" : mahasiswa.getTempatlahir()));
		hbox.appendChild(new Label(
				mahasiswa.getTanggallahir() == null ? "" : Common.dateFormat2.get().format(mahasiswa.getTanggallahir())));
		row.appendChild(hbox);

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
		Dosen dosenPa = krsMahasiswa.getDosenPa();

		if (Common.bolehKonfigurasi("mahasiswa_bisa_memilih_dosen_pa_sendiri", Konfigurasi.TIDAK_AKTIF)) {

			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembimbing Akademik"));
			row.appendChild(dosen = new AmbilDataDosenBanbox());
			dosen.setValue(dosenPa == null || dosenPa.getId() == null ? "" : (dosenPa.getNama()));
			dosen.setAttribute("dosen", dosenPa);
			dosen.setAttribute("myValue", dosenPa);
			dosen.setWidth("90%");
		} else {
			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembimbing Akademik"));
			if (dosenPa != null) {
				Vbox vbox = new Vbox();
				row.appendChild(vbox);
				CommonMedia.tampilkanGambarKecil(dosenPa).setParent(vbox);
				vbox.appendChild(new Label(dosenPa == null ? "T/A" : dosenPa.getNama()));
			} else {
				row.appendChild(new Label(dosenPa == null ? "T/A" : dosenPa.getNama()));
			}
		}

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kelamin"));
		kelamin = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Laki-laki");
		comboitem.setValue("Laki-laki");
		kelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Perempuan");
		comboitem.setValue("Perempuan");
		kelamin.appendChild(comboitem);
		Common.selectComboItem(kelamin, mahasiswa.getKelamin());
		row.appendChild(kelamin);
		kelamin.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ConstantValues.penggunaanLabelBahasa);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bahasa"));
		row.appendChild(bahasa = new Combobox());
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Tbmuser.INDONESIA);
		comboitem.setValue(Tbmuser.INDONESIA);
		bahasa.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Tbmuser.ENGLISH);
		comboitem.setValue(Tbmuser.ENGLISH);
		bahasa.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Tbmuser.ARAB);
		comboitem.setValue(Tbmuser.ARAB);
		bahasa.appendChild(comboitem);
		bahasa.setWidth("90%");
		bahasa.setReadonly(true);

		Common.selectComboItem(bahasa, mahasiswa.getBahasa());

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("nisn");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor Induk Siswa Nasional (NISN) " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(nisn = new Textbox(biodataMahasiswa.getNisn()));
		nisn.setWidth("90%");

		Common.initKeterangan(rows, "Jika Anda tidak memiliki NISN, boleh diisi dengan 0000000000 (0 sepuluh kali)");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("nirm");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor Induk Registrasi Mahasiswa (NIRM) " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(nirm = new Textbox(biodataMahasiswa.getNirm()));
		nirm.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("npwp");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("NPWP " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(npwp = new Textbox(biodataMahasiswa.getNpwp()));
		npwp.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(
				statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB));
		row.setParent(rows);
		row.appendChild(new Label());

		MyFormRow parentPreview = new MyFormRow();
		parentPreview.setVisible(
				statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB));
		parentPreview.setParent(rows);
		parentPreview.appendChild(new Label());

		Hbox ahbox = new Hbox();
		ahbox.setParent(parentPreview);
		Common.createDownloadUploadFileLampiran(row, ahbox, mahasiswa, LampiranLainMahasiswa.NPWP, "NPWP");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(new ais.ui.util.MyLabelConfig(mahasiswa.getJurusan().getFakultas().getNama()));

		row = new MyFormRow();


		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				mahasiswa.getTahunangkatan() + " (" + mahasiswa.getSemesterMulai() + ")"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Masuk"));
		Vbox vboxTglMasuk = new Vbox();
		row.appendChild(vboxTglMasuk);
		vboxTglMasuk.appendChild(new Label(
				mahasiswa.getTanggalMasuk() == null ? "" : Common.dateFormat2.get().format(mahasiswa.getTanggalMasuk())));

		final Label labelWaktuBelajar;
		vboxTglMasuk.appendChild(labelWaktuBelajar = new Label());
		EventListener listener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				labelWaktuBelajar.setValue(mahasiswa.ambilMasaStudi());
			}
		};

		listener.onEvent(null);

		row = new MyFormRow();


		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		try {
			row.appendChild(
					new ais.ui.util.MyLabelConfig(ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa().getNama()));
		} catch (Exception e) {
			row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		}
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Email"));
		row.appendChild(email = new Textbox(biodataMahasiswa.getEmail()));
		// email.setConstraint("/.+@.+\\.[a-z]+/: Format email harus sesuai");
		email.setWidth("90%");
		Common.initKeterangan(rows,
				"Jika email lebih dari satu, gunakan tanda koma (,) sebagai pemisah, misal-nya :  anda@mail.com,anda1@oke.com,anda3@mail.com. Sedangkan untuk email utama, tempatkan di urutan paling depan.");

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("aktifkan_pin_smartcard_mhs", Konfigurasi.TIDAK_AKTIF));

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("PIN (Hanya Data Numerik)"));
		row.appendChild(pin = new Decimalbox(new BigDecimal(mahasiswa.getPin() == null ? 0L : mahasiswa.getPin())));
		pin.setWidth("90%");
		// pin.setDisabled(Common.getCurrentUser().getRoot() == null ? true
		// : !Common.getCurrentUser().getRoot());

		ttd = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanda Tangan (PNG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, mahasiswa.getId(), LampiranLain.TTD_MAHASISWA, "Tanda Tangan",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ttd = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true, null, false, false);

		hbox.setParent(row);

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("jenisSekolah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Jenis Pendidikan sebelumya " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		Common.insertCombo(jenisSekolah = new Combobox(), "nama", JenisSekolahMahasiswaBaru.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		jenisSekolah.setReadonly(true);
		Common.selectComboItem(jenisSekolah, biodataMahasiswa.getJenisSekolah());
		row.appendChild(jenisSekolah);
		jenisSekolah.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("asalSma");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Asal Pendidikan Sebelumnya " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(asalSma = new AmbilDataNamaSekolahBanbox(
				biodataMahasiswa.getAsalSma() == null ? "" : biodataMahasiswa.getAsalSma()));
		asalSma.setAttribute("namaSekolahAsal", biodataMahasiswa.getNamaSekolahAsal());
		asalSma.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("npsn");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"NPSN Pendidikan Sebelumnya " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(npsn = new Textbox(biodataMahasiswa.getNpsn()));
		npsn.setWidth("90%");

		asalSma.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				NamaSekolahAsal namaSekolahAsal = (NamaSekolahAsal) asalSma.getAttribute("namaSekolahAsal");
				npsn.setValue(namaSekolahAsal == null ? "" : namaSekolahAsal.getKode());

			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());

		parentPreview = new MyFormRow();
		parentPreview.setParent(rows);
		parentPreview.appendChild(new Label());
		ahbox = new Hbox();
		ahbox.setParent(parentPreview);
		Common.createDownloadUploadFileLampiran(row, ahbox, mahasiswa, LampiranLainMahasiswa.IJAZAH,
				"Ijazah Pendidikan Sebelumnya");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());

		parentPreview = new MyFormRow();
		parentPreview.setParent(rows);
		parentPreview.appendChild(new Label());
		ahbox = new Hbox();
		ahbox.setParent(parentPreview);
		Common.createDownloadUploadFileLampiran(row, ahbox, mahasiswa, LampiranLainMahasiswa.TRANSKRIP_NILAI,
				"Transkrip Nilai Pendidikan Sebelumnya");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("kodeKerjaan");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new Label("Nama Bank" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(kodeKerjaan = new Textbox(
				biodataMahasiswa.getKodeKerjaan() == null ? "" : biodataMahasiswa.getKodeKerjaan()));
		kodeKerjaan.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("cabangBri");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Atas Nama Rekening Bank" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(cabangBri = new Textbox(
				biodataMahasiswa.getCabangBri() == null ? "" : biodataMahasiswa.getCabangBri()));
		cabangBri.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("no_rek_bri");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new Label("Nomor Rekening Bank" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(no_rek_bri = new Textbox(
				biodataMahasiswa.getNo_rek_bri() == null ? "" : biodataMahasiswa.getNo_rek_bri()));
		no_rek_bri.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("namaUntukIjazah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama yang tertera di Ijazah SMA / Sederajat "
				+ (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(
				namaUntukIjazah = new Textbox(biodataMahasiswa.getNamaUntukIjazah() == null ? mahasiswa.getNama()
						: biodataMahasiswa.getNamaUntukIjazah()));
		namaUntukIjazah.setWidth("90%");

		tanggallahirManual = null;
		if (Common.bolehKonfigurasi("tanggal_lahir_manual_tampil_di_mahasiswa", Konfigurasi.TIDAK_AKTIF)) {
			row = new MyFormRow();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tgl lahir yang tertera di Ijazah SMA / Sederajat"));
			row.appendChild(tanggallahirManual = new MyTextbox(mahasiswa.getTanggallahirManual()));
		}

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("noIjazah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new Label(
				"No Ijazah Pendidikan Sebelumnya" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(
				noIjazah = new Textbox(biodataMahasiswa.getNoIjazah() == null ? "" : biodataMahasiswa.getNoIjazah()));
		noIjazah.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("alamatAsalSma");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Alamat Pendidikan Sebelumnya" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(alamatAsalSma = new Textbox(
				biodataMahasiswa.getAlamatAsalSma() == null ? "" : biodataMahasiswa.getAlamatAsalSma()));
		alamatAsalSma.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("asalSmp");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Asal SMP / Sederajat" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(asalSmp = new AmbilDataNamaSekolahBanbox(
				biodataMahasiswa.getAsalSmp() == null ? "" : biodataMahasiswa.getAsalSmp()));
		asalSmp.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("alamatAsalSmp");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Alamat Asal SMP / Sederajat" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(alamatAsalSmp = new Textbox(
				biodataMahasiswa.getAlamatAsalSmp() == null ? "" : biodataMahasiswa.getAlamatAsalSmp()));
		alamatAsalSmp.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("asalSd");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Asal SD / Sederajat" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(asalSd = new AmbilDataNamaSekolahBanbox(
				biodataMahasiswa.getAsalSd() == null ? "" : biodataMahasiswa.getAsalSd()));
		asalSd.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("alamatAsalSd");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Alamat Asal SD" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(alamatAsalSd = new Textbox(
				biodataMahasiswa.getAlamatAsalSd() == null ? "" : biodataMahasiswa.getAlamatAsalSd()));
		alamatAsalSd.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("apakahPernahPaud");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Pernah PAUD (Pendidikan Anak Usia Dini)" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(apakahPernahPaud = new MyCheckboxConfig());
		apakahPernahPaud.setChecked(biodataMahasiswa.getApakahPernahPaud());

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("apakahPernahTk");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Pernah TK (Taman kanak-Kanak)" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(apakahPernahTk = new MyCheckboxConfig());
		apakahPernahTk.setChecked(biodataMahasiswa.getApakahPernahTk());

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("jenisTinggalMahasiswa");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Tempat Tinggal Saat Kuliah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(jenisTinggalMahasiswa = new Combobox());
		Common.insertCombo(jenisTinggalMahasiswa, "nama", JenisTinggalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jenisTinggalMahasiswa, biodataMahasiswa.getJenisTinggalMahasiswa());
		jenisTinggalMahasiswa.setWidth("90%");
		jenisTinggalMahasiswa.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("ukuranJaket");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Ukuran Jaket" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(ukuranJaket = new Textbox(
				biodataMahasiswa.getUkuranJaket() == null ? "" : biodataMahasiswa.getUkuranJaket()));
		ukuranJaket.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("pernahMenetapDiLuarNegeri");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Pernah Menetap di Luar Negeri" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		Common.selectComboItem(pernahMenetapDiLuarNegeri, biodataMahasiswa.getPernahMenetapDiLuarNegeri());
		row.appendChild(pernahMenetapDiLuarNegeri);
		pernahMenetapDiLuarNegeri.setWidth("90%");
		pernahMenetapDiLuarNegeri.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("tinggiBadan");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tinggi Badan (Cm)" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(tinggiBadan = new Decimalbox(
				new BigDecimal(biodataMahasiswa.getTinggiBadan() == null ? 0 : biodataMahasiswa.getTinggiBadan())));
		tinggiBadan.setWidth("90%");
		// row.setVisible(false);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("beratBadan");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Berat Badan (Kg)" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(beratBadan = new Decimalbox(
				new BigDecimal(biodataMahasiswa.getBeratBadan() == null ? 0 : biodataMahasiswa.getBeratBadan())));
		beratBadan.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("hp");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("No. HP" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(hp = new Textbox(biodataMahasiswa.getHp() == null ? "" : biodataMahasiswa.getHp()));
		hp.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("operatorSeluler");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Operator HP" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(hpProvider = new Combobox());
		Common.insertComboDanSemua(hpProvider, new String[] { "nama" }, "keterangan", OperatorSeluler.class,
				"== Pilih Salah Satu Operator Seluler ==", Restrictions.eq("aktif", true));
		Common.selectComboItem(hpProvider, biodataMahasiswa.getOperatorSeluler());
		hpProvider.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("suratIzinMengemudi");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Nomor Surat Ijin Mengemudi" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(suratIzinMengemudi = new Textbox(
				biodataMahasiswa.getSuratIzinMengemudi() == null ? "" : biodataMahasiswa.getSuratIzinMengemudi()));
		suratIzinMengemudi.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("alatTransportasiMahasiswa");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Transportasi Mahasiswa Saat Kuliah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(alatTransportasiMahasiswa = new Combobox());
		Common.insertCombo(alatTransportasiMahasiswa, "nama", AlatTransportasiMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(alatTransportasiMahasiswa, biodataMahasiswa.getAlatTransportasiMahasiswa());
		alatTransportasiMahasiswa.setWidth("90%");
		alatTransportasiMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(false);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kendaraan Kuliah"));
		row.appendChild(kendaraanKuliah = new Textbox(
				biodataMahasiswa.getKendaraanKuliah() == null ? "" : biodataMahasiswa.getKendaraanKuliah()));
		kendaraanKuliah.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("pernahMemimpinOrganisasi");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Organisasi Intra Kampus yang diikuti" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		Common.selectComboItem(pernahMemimpinOrganisasi, biodataMahasiswa.getPernahMemimpinOrganisasi());
		row.appendChild(pernahMemimpinOrganisasi);
		pernahMemimpinOrganisasi.setWidth("90%");
		pernahMemimpinOrganisasi.setReadonly(true);

		row = new MyFormRow();

				row.setVisible(false);
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("namaOrganisasi");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nama organisasi mahasiswa" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(namaOrganisasi = new Textbox(
				biodataMahasiswa.getNamaOrganisasi() == null ? "" : biodataMahasiswa.getNamaOrganisasi()));
		namaOrganisasi.setWidth("90%");
		namaOrganisasi.setMaxlength(49);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Surat penunjukan sebagai pengurus organisasi (jika ada)"));

		parentPreview = new MyFormRow();
		parentPreview.setParent(rows);
		parentPreview.appendChild(new Label());

		ahbox = new Hbox();
		ahbox.setParent(parentPreview);
		Common.createDownloadUploadFileLampiran(row, ahbox, mahasiswa,
				LampiranLainMahasiswa.SURAT_PENUNJUKAN_PENGURUS_ORGANISASI, "Surat Penunjukan Pengurus Organisasi");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("hobi");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Hobi" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(hobi = new Textbox(biodataMahasiswa.getHobi() == null ? "" : biodataMahasiswa.getHobi()));
		hobi.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("minatSeni");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Minat Seni" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(minatSeni = new Textbox(
				biodataMahasiswa.getMinatSeni() == null ? "" : biodataMahasiswa.getMinatSeni()));
		minatSeni.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("kemampuanBahasa1");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Kemampuan Bahasa 1 / Utama" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(kemampuanBahasa1 = new Textbox(
				biodataMahasiswa.getKemampuanBahasa1() == null ? "" : biodataMahasiswa.getKemampuanBahasa1()));
		kemampuanBahasa1.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("kemampuanBahasa2");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Kemampuan Bahasa 2" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(kemampuanBahasa2 = new Textbox(
				biodataMahasiswa.getKemampuanBahasa2() == null ? "" : biodataMahasiswa.getKemampuanBahasa2()));
		kemampuanBahasa2.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("kemampuanBahasa3");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Kemampuan Bahasa 3" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(kemampuanBahasa3 = new Textbox(
				biodataMahasiswa.getKemampuanBahasa3() == null ? "" : biodataMahasiswa.getKemampuanBahasa3()));
		kemampuanBahasa3.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("golonganDarah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Golongan Darah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(golonganDarah = new Textbox(
				biodataMahasiswa.getGolonganDarah() == null ? "" : biodataMahasiswa.getGolonganDarah()));
		golonganDarah.setWidth("90%");
		golonganDarah.setMaxlength(10);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("statusNikah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Status Nikah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		Common.selectComboItem(statusNikah, biodataMahasiswa.getStatusNikah());
		row.appendChild(statusNikah);
		statusNikah.setWidth("90%");
		statusNikah.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("agama");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Agama" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		Common.selectComboItem(agama, biodataMahasiswa.getAgama());
		row.appendChild(agama);
		agama.setWidth("90%");
		agama.setReadonly(true);

		return borderlayout;
	}

	private Borderlayout initKelulusan(final Mahasiswa mahasiswa) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Keluar dari Kampus"));

		row.appendChild(new Label(mahasiswa.getStatusKeluar() == null ? "Mahasiswa Belum Lulus / Masih Aktif"
				: mahasiswa.getStatusKeluar().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Predikat Kelulusan"));
		row.appendChild(
				new Label(mahasiswa.getPredikatKelulusan() == null ? "-" : mahasiswa.getPredikatKelulusan().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Setelah Lulus"));

		statusSetelahLulus = new Combobox();
		row.appendChild(statusSetelahLulus);

		Common.insertComboDanSemua(statusSetelahLulus, new String[] { "nama", "kode" }, "keterangan",
				StatusSetelahLulus.class, "== Mahasiswa Belum Lulus / Masih Aktif ==",
				Restrictions.sqlRestriction("true"));
		Common.selectComboItem(statusSetelahLulus, mahasiswa.getStatusSetelahLulus());
		statusSetelahLulus.setWidth("90%");
		statusSetelahLulus.setReadonly(true);

		Common.initKeterangan(rows, "* Wajib diisi ketika mahasiswa dinyatakan lulus");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Pekerjaan Setelah Lulus"));

		statusPekerjaanSetelahLulus = new Combobox();
		row.appendChild(statusPekerjaanSetelahLulus);
		Common.insertComboDanSemua(statusPekerjaanSetelahLulus, new String[] { "nama", "kode" }, "keterangan",
				StatusPekerjaanSetelahLulus.class, "== Mahasiswa Belum Lulus / Masih Aktif ==",
				Restrictions.sqlRestriction("true"));
		Common.selectComboItem(statusPekerjaanSetelahLulus, mahasiswa.getStatusPekerjaanSetelahLulus());
		statusPekerjaanSetelahLulus.setWidth("90%");
		statusPekerjaanSetelahLulus.setReadonly(true);

		Common.initKeterangan(rows, "* Wajib diisi ketika mahasiswa dinyatakan lulus");

		BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Email Pimpinan / Atasan Setelah Bekerja"));
		row.appendChild(emailAtasan = new Textbox(biodataMahasiswa == null ? null : biodataMahasiswa.getEmailAtasan()));
		// email.setConstraint("/.+@.+\\.[a-z]+/: Format email harus sesuai");
		emailAtasan.setWidth("90%");
		Common.initKeterangan(rows,
				"Jika email lebih dari satu, gunakan tanda koma (,) sebagai pemisah, misal-nya :  atasan@mail.com,atasan1@oke.com,atasan3@mail.com. Sedangkan untuk email utama, tempatkan di urutan paling depan.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Domisili Setelah Lulus"));
		statusDomisiliSetelahLulus = new Combobox();
		row.appendChild(statusDomisiliSetelahLulus);

		Common.insertComboDanSemua(statusDomisiliSetelahLulus, new String[] { "nama", "kode" }, "keterangan",
				StatusDomisiliSetelahLulus.class, "== Mahasiswa Belum Lulus / Masih Aktif ==",
				Restrictions.sqlRestriction("true"));
		Common.selectComboItem(statusDomisiliSetelahLulus, mahasiswa.getStatusDomisiliSetelahLulus());
		statusDomisiliSetelahLulus.setWidth("90%");
		statusDomisiliSetelahLulus.setReadonly(true);

		Common.initKeterangan(rows, "* Wajib diisi ketika mahasiswa dinyatakan lulus");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Ijazah I"));
		row.appendChild(new Label(mahasiswa.getNoIjazah1()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Ijazah II"));
		row.appendChild(new Label(mahasiswa.getNoIjazah2()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Akta"));
		row.appendChild(new Label(mahasiswa.getNoAkta1()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Surat Pendamping Ijazah"));
		row.appendChild(new Label(mahasiswa.getNomorSkpi()));

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Lulus"));
		row.appendChild(new Label(mahasiswa.getTahunLulus() == null || mahasiswa.getTahunLulus().equals(0) ? ""
				: mahasiswa.getTahunLulus().toString()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Lulus / Keluar / Mengundurkan Diri / DO"));
		row.appendChild(new Label(
				mahasiswa.getTanggalLulus() == null ? "" : Common.dateFormat2.get().format(mahasiswa.getTanggalLulus())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester Lulus / Keluar / Mengundurkan Diri / DO"));
		row.appendChild(new Label(mahasiswa.getSemesterLulus() == null || mahasiswa.getSemesterLulus().equals(0) ? ""
				: mahasiswa.getSemesterLulus().toString()));
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Memaksa Non Aktif di semester mana saja ?"));
		row.appendChild(new Label(mahasiswa.getBatasStudi()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Memaksa Aktif di semester mana saja ?"));
		row.appendChild(new Label(mahasiswa.getPaksaAktifSemester()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Wisuda"));
		row.appendChild(new Label(mahasiswa.getTahunWisuda() == null || mahasiswa.getTahunWisuda().equals(0) ? ""
				: mahasiswa.getTahunWisuda().toString()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Wisuda"));
		row.appendChild(new Label(
				mahasiswa.getTanggalWisuda() == null ? "" : Common.dateFormat2.get().format(mahasiswa.getTanggalWisuda())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Yudisium"));
		row.appendChild(new Label(mahasiswa.getTanggalYudisium() == null ? ""
				: Common.dateFormat2.get().format(mahasiswa.getTanggalYudisium())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. SK"));
		row.appendChild(new Label(mahasiswa.getNoAkta2()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal SK"));
		row.appendChild(new Label(mahasiswa.getTanggalSkRektor() == null ? ""
				: Common.dateFormat2.get().format(mahasiswa.getTanggalSkRektor())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor SK Drop Out (jika DO)"));
		row.appendChild(new Label(mahasiswa.getSkDo()));

		Object[] juduls = (Object[]) (mahasiswa.getId() != null ? HibernateUtil.currentSession()
				.createCriteria(Skripsi.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.setProjection(Projections.projectionList().add(Projections.property("judul"))

						.add(Projections.property("awalBimbingan")).add(Projections.property("akhirBimbingan"))
						.add(Projections.property("judulen"))

				).addOrder(Order.desc("id")).setMaxResults(1).uniqueResult() : null);
		String judul = juduls == null || juduls.length == 0 ? null : juduls[0].toString();
		Date awal = (Date) (juduls == null || juduls.length == 0 ? null : juduls[1]);
		Date akhir = (Date) (juduls == null || juduls.length == 0 ? null : juduls[2]);
		String judulen = juduls == null || juduls.length == 0 ? null : juduls[3].toString();
		if (judul != null && !judul.trim().isEmpty()) {
			mahasiswa.setJudulSkripsi(judul);
		}
		if (judulen != null && !judulen.trim().isEmpty()) {
			mahasiswa.setJudulSkripsiEn(judulen);
		}
		if (awal != null) {
			mahasiswa.setBlnAwalBimbingan(awal);
		}
		if (akhir != null) {
			mahasiswa.setBlnAkhirBimbingan(akhir);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Awal Bimbingan"));
		row.appendChild(new Label(mahasiswa.getBlnAwalBimbingan() == null ? ""
				: Common.dateFormat2.get().format(mahasiswa.getBlnAwalBimbingan())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Akhir Bimbingan"));
		row.appendChild(new Label(mahasiswa.getBlnAkhirBimbingan() == null ? ""
				: Common.dateFormat2.get().format(mahasiswa.getBlnAkhirBimbingan())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Judul " + Common.getKonfigurasi("label_skripsi", "skripsi").getNilai()));
		row.appendChild(new Label(mahasiswa.getJudulSkripsi()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Judul " + Common.getKonfigurasi("label_skripsi", "skripsi").getNilai() + " (English)"));
		row.appendChild(new Label(mahasiswa.getJudulSkripsiEn()));

		return borderlayout;
	}

	public Borderlayout initAlamat(final BiodataMahasiswa biodataMahasiswa) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setStyle("border:0px;");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		columns.appendChild(column);
		column = new MyColumnConfig();
		columns.appendChild(column);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");

		String statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("noIdentitas");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Nomor KTP/NIK tanpa tanda baca" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(noIdentitas = new Textbox(biodataMahasiswa.getNoIdentitas()));
		noIdentitas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());

		MyFormRow parentPreview = new MyFormRow();
		parentPreview.setParent(rows);
		parentPreview.appendChild(new Label());

		Hbox ahbox = new Hbox();
		ahbox.setParent(parentPreview);

		Common.createDownloadUploadFileLampiran(row, ahbox, mahasiswa, LampiranLainMahasiswa.KTP, "KTP/NIK");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Akte Kelahiran")));

		parentPreview = new MyFormRow();
		parentPreview.setParent(rows);
		parentPreview.appendChild(new Label());

		ahbox = new Hbox();
		ahbox.setParent(parentPreview);

		Common.createDownloadUploadFileLampiran(row, ahbox, mahasiswa, LampiranLainMahasiswa.AKTE, "Akte Kelahiran");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("alamat");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Alamat / Jalan / Gang" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(alamat = new Textbox(biodataMahasiswa.getAlamat() == null ? "" : biodataMahasiswa.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(3);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("dusun");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Dusun / Kampung" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(dusun = new Textbox(biodataMahasiswa.getDusun()));
		dusun.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("rt");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig("RT" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(rt = new Textbox(biodataMahasiswa.getRt() == null ? "" : biodataMahasiswa.getRt()));
		rt.setWidth("90%");
		rt.setMaxlength(3);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("rw");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig("RW" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(rw = new Textbox(biodataMahasiswa.getRw() == null ? "" : biodataMahasiswa.getRw()));
		rw.setWidth("90%");
		rw.setMaxlength(3);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("kodepos");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Kode Pos" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(
				kodepos = new Textbox(biodataMahasiswa.getKodepos() == null ? "" : biodataMahasiswa.getKodepos()));
		kodepos.setWidth("90%");
		kodepos.setMaxlength(8);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("kelurahan");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Kelurahan / Desa" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(kelurahan = new Textbox(
				biodataMahasiswa.getKelurahan() == null ? "" : biodataMahasiswa.getKelurahan()));
		kelurahan.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("kecamatan");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Kecamatan" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(kecamatan = new AmbilDataKecamatanBanbox());
		kecamatan.setValue(biodataMahasiswa.getKecamatan() == null ? "" : biodataMahasiswa.getKecamatan().getNama());
		kecamatan.setAttribute("wilayah", biodataMahasiswa.getKecamatan());
		kecamatan.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("propinsi");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Propinsi" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
//		Common.selectComboItem(propinsi, biodataMahasiswa.getPropinsi());
		row.appendChild(propinsi);
		propinsi.setWidth("90%");
		propinsi.setAttribute("wilayah", biodataMahasiswa.getPropinsi());

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("kota");
		Common.createFieldKota(rows, "Kota/Kabupaten" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : ""), kota,
				propinsi, biodataMahasiswa.getKota(),
				statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB));

		Common.createKotaPropinsiListenerBerdasarkanKecamatan(propinsi, kota, kecamatan);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("teleponRumah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telepon Rumah, jika tidak ada, nomor yang bisa dihubungi"
				+ (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(teleponRumah = new Textbox(
				biodataMahasiswa.getTeleponRumah() == null ? "" : biodataMahasiswa.getTeleponRumah()));
		teleponRumah.setWidth("90%");

		return borderlayout;
	}

	public Borderlayout initOrangTua(final BiodataMahasiswa biodataMahasiswa) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setStyle("border:0px;");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		columns.appendChild(column);
		column = new MyColumnConfig();
		columns.appendChild(column);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");

		String statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("noKK");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("No. Kartu Keluarga (KK)" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(noKK = new Textbox(biodataMahasiswa.getNoKK() == null ? "" : biodataMahasiswa.getNoKK()));
		noKK.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());

		MyFormRow parentPreview = new MyFormRow();
		parentPreview.setParent(rows);
		parentPreview.appendChild(new Label());

		Hbox ahbox = new Hbox();
		ahbox.setParent(parentPreview);

		Common.createDownloadUploadFileLampiran(row, ahbox, mahasiswa, LampiranLainMahasiswa.KK, "Kartu Keluarga (KK)");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("nikAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"No. KTP Ayah " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(nikAyah = new Textbox(biodataMahasiswa.getNikAyah()));
		nikAyah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());

		parentPreview = new MyFormRow();
		parentPreview.setParent(rows);
		parentPreview.appendChild(new Label());

		ahbox = new Hbox();
		ahbox.setParent(parentPreview);

		Common.createDownloadUploadFileLampiran(row, ahbox, mahasiswa, LampiranLainMahasiswa.KTP_AYAH, "KTP Ayah");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("namaAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nama Ayah Kandung" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(
				namaAyah = new Textbox(biodataMahasiswa.getNamaAyah() == null ? "" : biodataMahasiswa.getNamaAyah()));
		namaAyah.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("telpAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Telpon / HP Ayah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(
				telpAyah = new Textbox(biodataMahasiswa.getTelpAyah() == null ? "" : biodataMahasiswa.getTelpAyah()));
		telpAyah.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("tanggalLahirAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tanggal Lahir Ayah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(tanggalLahirAyah = new MyDatebox(biodataMahasiswa.getTanggalLahirAyah()));

		row = new MyFormRow();

				row.setVisible(terintegrasiDenganEmis);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pekerjaan Ayah"));
		row.appendChild(pekerjaanAyah = new Combobox());
		Common.insertCombo(pekerjaanAyah, "nama", PekerjaanOrangTua.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(pekerjaanAyah, biodataMahasiswa.getPekerjaanAyah());
		pekerjaanAyah.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("jenisPekerjaanAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Jenis Pekerjaan Ayah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(jenisPekerjaanAyah = new Combobox());
		Common.insertCombo(jenisPekerjaanAyah, "nama", Pekerjaan.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(jenisPekerjaanAyah, biodataMahasiswa.getJenisPekerjaanAyah());
		jenisPekerjaanAyah.setWidth("90%");
		row.setVisible(!jenisPekerjaanAyah.getChildren().isEmpty());
		jenisPekerjaanAyah.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(terintegrasiDenganEmis);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Rata-rata penghasilan ayah perbulan"));
		row.appendChild(pendapatanOrtu = new Combobox());
		for (PendapatanOrangTua o : DaoFactory.getInstance().getPendapatanOrangTuaDao().findAll()) {
			MyComboitemConfig comboitems = new MyComboitemConfig();
			comboitems.setLabel("Rp. " + Common.numberFormat.get().format(o.getMulaiDari()) + " - Rp. "
					+ Common.numberFormat.get().format(o.getSampai()));
			comboitems.setValue(o);
			pendapatanOrtu.appendChild(comboitems);
		}
		Common.selectComboItem(pendapatanOrtu, biodataMahasiswa.getPendapatanOrtu());
		pendapatanOrtu.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("jenisPenghasilanAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Rata-rata penghasilan ayah" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(jenisPenghasilanAyah = new Combobox());
		Common.insertCombo(jenisPenghasilanAyah, "nama", Penghasilan.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(jenisPenghasilanAyah, biodataMahasiswa.getJenisPenghasilanAyah());
		jenisPenghasilanAyah.setWidth("90%");
		row.setVisible(!jenisPenghasilanAyah.getChildren().isEmpty());
		jenisPenghasilanAyah.setReadonly(true);

		row = new MyFormRow();

		row.setVisible(terintegrasiDenganEmis);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pendidikan Ayah"));
		row.appendChild(pendidikanAyah = new Combobox());
		Common.insertCombo(pendidikanAyah, "nama", PendidikanOrangTua.class);
		Common.selectComboItem(pendidikanAyah, biodataMahasiswa.getPendidikanAyah());
		pendidikanAyah.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("jenjangPendidikanAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Jenjang Pendidikan Ayah " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(jenjangPendidikanAyah = new Combobox());
		Common.insertCombo(jenjangPendidikanAyah, "nama", Jenjang.class,
				Restrictions.or(Restrictions.eq("aktifDipilih", true), Restrictions.isNull("aktifDipilih")));
		Common.selectComboItem(jenjangPendidikanAyah, biodataMahasiswa.getJenjangPendidikanAyah());
		jenjangPendidikanAyah.setWidth("90%");
		jenjangPendidikanAyah.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("nikIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"No. KTP Ibu " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(nikIbu = new Textbox(biodataMahasiswa.getNikIbu()));
		nikIbu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());

		parentPreview = new MyFormRow();
		parentPreview.setParent(rows);
		parentPreview.appendChild(new Label());
		ahbox = new Hbox();
		ahbox.setParent(parentPreview);
		Common.createDownloadUploadFileLampiran(row, ahbox, mahasiswa, LampiranLainMahasiswa.KTP_IBU, "KTP Ibu");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("namaIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nama Ibu Kandung" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		namaIbu = new Textbox(biodataMahasiswa.getNamaIbu() == null ? "" : biodataMahasiswa.getNamaIbu());

//		if (mahasiswa.getIdRegPd() != null && !mahasiswa.getIdRegPd().trim().isEmpty()) {
//			row.appendChild(new Label(biodataMahasiswa.getNamaIbu()));
//		} else 

		if (tbmuser == null || tbmuser.getMahasiswa() == null) {
			row.appendChild(namaIbu);
		} else {
			if (tbmuser != null && tbmuser.getMahasiswa() != null && mahasiswa_boleh_mengubah_ttl_dan_ibu_profile) {
				row.appendChild(namaIbu);
			} else {
				row.appendChild(new Label(biodataMahasiswa.getNamaIbu()));
			}
		}
		namaIbu.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("telpIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Telpon / HP Ibu" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(
				telpIbu = new Textbox(biodataMahasiswa.getTelpIbu() == null ? "" : biodataMahasiswa.getTelpIbu()));
		telpIbu.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("tanggalLahirIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tanggal Lahir Ibu" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(tanggalLahirIbu = new MyDatebox(biodataMahasiswa.getTanggalLahirIbu()));

		row = new MyFormRow();

		row.setVisible(terintegrasiDenganEmis);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pekerjaan Ibu"));
		row.appendChild(pekerjaanIbu = new Combobox());
		Common.insertCombo(pekerjaanIbu, "nama", PekerjaanOrangTua.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(pekerjaanIbu, biodataMahasiswa.getPekerjaanIbu());
		pekerjaanIbu.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("jenisPekerjaanIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Jenis Pekerjaan Ibu" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(jenisPekerjaanIbu = new Combobox());
		Common.insertCombo(jenisPekerjaanIbu, "nama", Pekerjaan.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(jenisPekerjaanIbu, biodataMahasiswa.getJenisPekerjaanIbu());
		jenisPekerjaanIbu.setWidth("90%");
		row.setVisible(!jenisPekerjaanIbu.getChildren().isEmpty());
		jenisPekerjaanIbu.setReadonly(true);

		row = new MyFormRow();

		row.setVisible(terintegrasiDenganEmis);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Rata-rata penghasilan ibu perbulan"));
		row.appendChild(pendapatanOrtuIbu = new Combobox());
		for (PendapatanOrangTua o : DaoFactory.getInstance().getPendapatanOrangTuaDao().findAll()) {
			MyComboitemConfig comboitems = new MyComboitemConfig();
			comboitems.setLabel("Rp. " + Common.numberFormat.get().format(o.getMulaiDari()) + " - Rp. "
					+ Common.numberFormat.get().format(o.getSampai()));
			comboitems.setValue(o);

			pendapatanOrtuIbu.appendChild(comboitems);
		}
		Common.selectComboItem(pendapatanOrtuIbu, biodataMahasiswa.getPendapatanOrtuIbu());
		pendapatanOrtuIbu.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("jenisPenghasilanIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Rata-rata penghasilan ibu" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(jenisPenghasilanIbu = new Combobox());
		Common.insertCombo(jenisPenghasilanIbu, "nama", Penghasilan.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(jenisPenghasilanIbu, biodataMahasiswa.getJenisPenghasilanIbu());
		jenisPenghasilanIbu.setWidth("90%");
		row.setVisible(!jenisPenghasilanIbu.getChildren().isEmpty());
		jenisPenghasilanIbu.setReadonly(true);

		row = new MyFormRow();

		row.setVisible(terintegrasiDenganEmis);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pendidikan Ibu"));
		row.appendChild(pendidikanIbu = new Combobox());
		Common.insertCombo(pendidikanIbu, "nama", PendidikanOrangTua.class);
		Common.selectComboItem(pendidikanIbu, biodataMahasiswa.getPendidikanIbu());
		pendidikanIbu.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("jenjangPendidikanIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Jenjang Pendidikan Ibu" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(jenjangPendidikanIbu = new Combobox());
		Common.insertCombo(jenjangPendidikanIbu, "nama", Jenjang.class,
				Restrictions.or(Restrictions.eq("aktifDipilih", true), Restrictions.isNull("aktifDipilih")));
		Common.selectComboItem(jenjangPendidikanIbu, biodataMahasiswa.getJenjangPendidikanIbu());
		jenjangPendidikanIbu.setWidth("90%");
		jenjangPendidikanIbu.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("bersaudara");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah anggota keluarga (termasuk ayah dan ibu jika masih ada)"
				+ (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(bersaudara = new Intbox(
				biodataMahasiswa.getBersaudara() == null ? 0 : biodataMahasiswa.getBersaudara()));
		bersaudara.setWidth("90%");

		return borderlayout;
	}

	public Borderlayout initLampiran(final BiodataMahasiswa biodataMahasiswa) throws Exception {

		Mahasiswa mahasiswa = biodataMahasiswa.getMahasiswa();

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setStyle("border:0px;");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		fotoMahasiswa = null;
		Common.createDownloadUploadFoto(row, mahasiswa, FotoMahasiswa.class, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getData() instanceof FotoMahasiswa) {
					fotoMahasiswa = (FotoMahasiswa) arg0.getData();
				}
			}
		}, bolehgantiFoto);

		Konfigurasi uploadKonfigurasi = Common.getKonfigurasi("mahasiswa_tidak_perlu_upload_ijazah", Konfigurasi.AKTIF);
		if (uploadKonfigurasi.checkTidakAktifDanTidakWajib()) {
			Common.createDownloadUploadFileLampiran(rows, mahasiswa, LampiranLainMahasiswa.IJAZAH,
					"Ijazah / Surat Keterangan Lulus (pendidikan sebelumnya)"
							+ (uploadKonfigurasi.getNilai().equals(Konfigurasi.TIDAK_AKTIF) ? " (*) " : ""));
		}
		uploadKonfigurasi = Common.getKonfigurasi("mahasiswa_tidak_perlu_upload_nilai", Konfigurasi.AKTIF);

		if (uploadKonfigurasi.checkTidakAktifDanTidakWajib()) {
			Common.createDownloadUploadFileLampiran(rows, mahasiswa, LampiranLainMahasiswa.TRANSKRIP_NILAI,
					"Transkrip Nilai Lulus (pendidikan sebelumnya)"
							+ (uploadKonfigurasi.getNilai().equals(Konfigurasi.TIDAK_AKTIF) ? " (*) " : ""));
		}

		uploadKonfigurasi = Common.getKonfigurasi("mahasiswa_tidak_perlu_upload_ktp", Konfigurasi.AKTIF);
		if (uploadKonfigurasi.checkTidakAktifDanTidakWajib()) {
			Common.createDownloadUploadFileLampiran(rows, mahasiswa, LampiranLainMahasiswa.KTP,
					"KTP / Kartu Pelajar (pendidikan sebelumnya) / Kartu Identitas lain"
							+ (uploadKonfigurasi.getNilai().equals(Konfigurasi.TIDAK_AKTIF) ? " (*) " : ""));
		}

		Konfigurasi konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_1", Konfigurasi.TIDAK_AKTIF);
		if (konfigurasiUpload.checkAktifDanTidakWajib()) {
			Common.createDownloadUploadFileLampiran(rows, mahasiswa, LampiranLainMahasiswa.LAMPIRAN_1,
					konfigurasiUpload.info1AndCheckWajibDanTidakWajib());
		}

		konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_2", Konfigurasi.TIDAK_AKTIF);
		if (konfigurasiUpload.checkAktifDanTidakWajib()) {
			Common.createDownloadUploadFileLampiran(rows, mahasiswa, LampiranLainMahasiswa.LAMPIRAN_2,
					konfigurasiUpload.info1AndCheckWajibDanTidakWajib());
		}

		konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_3", Konfigurasi.TIDAK_AKTIF);
		if (konfigurasiUpload.checkAktifDanTidakWajib()) {
			Common.createDownloadUploadFileLampiran(rows, mahasiswa, LampiranLainMahasiswa.LAMPIRAN_3,
					konfigurasiUpload.info1AndCheckWajibDanTidakWajib());
		}

		konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_4", Konfigurasi.TIDAK_AKTIF);
		if (konfigurasiUpload.checkAktifDanTidakWajib()) {
			Common.createDownloadUploadFileLampiran(rows, mahasiswa, LampiranLainMahasiswa.LAMPIRAN_4,
					konfigurasiUpload.info1AndCheckWajibDanTidakWajib());
		}

		konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_5", Konfigurasi.TIDAK_AKTIF);
		if (konfigurasiUpload.checkAktifDanTidakWajib()) {
			Common.createDownloadUploadFileLampiran(rows, mahasiswa, LampiranLainMahasiswa.LAMPIRAN_5,
					konfigurasiUpload.info1AndCheckWajibDanTidakWajib());
		}

		konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_6", Konfigurasi.TIDAK_AKTIF);
		if (konfigurasiUpload.checkAktifDanTidakWajib()) {
			Common.createDownloadUploadFileLampiran(rows, mahasiswa, LampiranLainMahasiswa.LAMPIRAN_6,
					konfigurasiUpload.info1AndCheckWajibDanTidakWajib());
		}

		konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_7", Konfigurasi.TIDAK_AKTIF);
		if (konfigurasiUpload.checkAktifDanTidakWajib()) {
			Common.createDownloadUploadFileLampiran(rows, mahasiswa, LampiranLainMahasiswa.LAMPIRAN_7,
					konfigurasiUpload.info1AndCheckWajibDanTidakWajib());
		}

		konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_8", Konfigurasi.TIDAK_AKTIF);
		if (konfigurasiUpload.checkAktifDanTidakWajib()) {
			Common.createDownloadUploadFileLampiran(rows, mahasiswa, LampiranLainMahasiswa.LAMPIRAN_8,
					konfigurasiUpload.info1AndCheckWajibDanTidakWajib());
		}

		konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_9", Konfigurasi.TIDAK_AKTIF);
		if (konfigurasiUpload.checkAktifDanTidakWajib()) {
			Common.createDownloadUploadFileLampiran(rows, mahasiswa, LampiranLainMahasiswa.LAMPIRAN_9,
					konfigurasiUpload.info1AndCheckWajibDanTidakWajib());
		}

		konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_10", Konfigurasi.TIDAK_AKTIF);
		if (konfigurasiUpload.checkAktifDanTidakWajib()) {
			Common.createDownloadUploadFileLampiran(rows, mahasiswa, LampiranLainMahasiswa.LAMPIRAN_10,
					konfigurasiUpload.info1AndCheckWajibDanTidakWajib());
		}

		return borderlayout;
	}

	public Borderlayout initWali(final BiodataMahasiswa biodataMahasiswa) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setStyle("border:0px;");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		columns.appendChild(column);
		column = new MyColumnConfig();
		columns.appendChild(column);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");

		String statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("namaWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Nama Wali" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(
				namaWali = new Textbox(biodataMahasiswa.getNamaWali() == null ? "" : biodataMahasiswa.getNamaWali()));
		namaWali.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());

		MyFormRow parentPreview = new MyFormRow();
		parentPreview.setParent(rows);
		parentPreview.appendChild(new Label());
		Hbox ahbox = new Hbox();
		ahbox.setParent(parentPreview);
		Common.createDownloadUploadFileLampiran(row, ahbox, mahasiswa, LampiranLainMahasiswa.KTP_WALI, "KTP Wali");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("telpWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Telpon / HP Wali" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(
				telpWali = new Textbox(biodataMahasiswa.getTelpWali() == null ? "" : biodataMahasiswa.getTelpWali()));
		telpWali.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("tanggalLahirWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tanggal Lahir Wali" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(tanggalLahirWali = new MyDatebox(biodataMahasiswa.getTanggalLahirWali()));

		row = new MyFormRow();

				row.setVisible(terintegrasiDenganEmis);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pekerjaan Wali"));
		row.appendChild(pekerjaanWali = new Combobox());
		Common.insertCombo(pekerjaanWali, "nama", PekerjaanOrangTua.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(pekerjaanWali, biodataMahasiswa.getPekerjaanWali());
		pekerjaanWali.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("jenisPekerjaanWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Jenis Pekerjaan Wali" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(jenisPekerjaanWali = new Combobox());
		Common.insertCombo(jenisPekerjaanWali, "nama", Pekerjaan.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(jenisPekerjaanWali, biodataMahasiswa.getJenisPekerjaanWali());
		jenisPekerjaanWali.setWidth("90%");
		row.setVisible(!jenisPekerjaanWali.getChildren().isEmpty());
		jenisPekerjaanWali.setReadonly(true);

		row = new MyFormRow();

		row.setVisible(terintegrasiDenganEmis);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Rata-rata penghasilan wali perbulan"));
		row.appendChild(pendapatanWali = new Combobox());
		for (PendapatanOrangTua o : DaoFactory.getInstance().getPendapatanOrangTuaDao().findAll()) {
			MyComboitemConfig comboitems = new MyComboitemConfig();
			comboitems.setLabel("Rp. " + Common.numberFormat.get().format(o.getMulaiDari()) + " - Rp. "
					+ Common.numberFormat.get().format(o.getSampai()));
			comboitems.setValue(o);
			pendapatanWali.appendChild(comboitems);
		}
		Common.selectComboItem(pendapatanWali, biodataMahasiswa.getPendapatanWali());
		pendapatanWali.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("jenisPenghasilanWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Rata-rata penghasilan wali" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(jenisPenghasilanWali = new Combobox());
		Common.insertCombo(jenisPenghasilanWali, "nama", Penghasilan.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(jenisPenghasilanWali, biodataMahasiswa.getJenisPenghasilanWali());
		jenisPenghasilanWali.setWidth("90%");
		row.setVisible(!jenisPenghasilanWali.getChildren().isEmpty());
		jenisPenghasilanWali.setReadonly(true);

		row = new MyFormRow();

		row.setVisible(terintegrasiDenganEmis);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pendidikan Wali"));
		row.appendChild(pendidikanWali = new Combobox());
		Common.insertCombo(pendidikanWali, "nama", PendidikanOrangTua.class);
		Common.selectComboItem(pendidikanWali, biodataMahasiswa.getPendidikanWali());
		pendidikanWali.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanBiodataMahasiswaAction.statusWajibIsi("jenjangPendidikanWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new Label("Jenjang Pendidikan Wali" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(jenjangPendidikanWali = new Combobox());
		Common.insertCombo(jenjangPendidikanWali, "nama", Jenjang.class,
				Restrictions.or(Restrictions.eq("aktifDipilih", true), Restrictions.isNull("aktifDipilih")));
		Common.selectComboItem(jenjangPendidikanWali, biodataMahasiswa.getJenjangPendidikanWali());
		jenjangPendidikanWali.setWidth("90%");
		jenjangPendidikanWali.setReadonly(true);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private Borderlayout initToeflToafl(final Mahasiswa mahasiswa) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row;

		Session session = HibernateUtil.currentSession();
		List<NilaiToeflToaflMahasiswa> nilaiToeflToaflMahasiswas = mahasiswa.getId() == null
				? new ArrayList<NilaiToeflToaflMahasiswa>()
				: session.createCriteria(NilaiToeflToaflMahasiswa.class).add(Restrictions.eq("mahasiswa", mahasiswa))
						.list();

		for (NilaiToeflToaflMahasiswa nilaiToeflToaflMahasiswa : nilaiToeflToaflMahasiswas) {
			row = new MyFormRow();
			row.setStyle("border:1px;solid;background: transparent;");
			row.setParent(rows);
			MyGrid gridData = new MyGrid();
			gridData.setParent(row);
			Rows rowsData = new Rows();
			rowsData.setParent(gridData);

			Columns columns = new Columns();
			columns.setParent(gridData);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("20%");
			column.setStyle("background: #E1F1FC;");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("90%");

			MyFormRow rowData = new MyFormRow();
			rowData.setValign("top");
			rowData.setParent(rowsData);
			rowData.setStyle("background: #E1F1FC;");
			rowData.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Test")));
			rowData.appendChild(new Label(nilaiToeflToaflMahasiswa.getJenisTest()));

			rowData = new MyFormRow();
			rowData.setParent(rowsData);
			rowData.setStyle("background: #E1F1FC;");
			rowData.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Tes")));
			rowData.appendChild(new Label(Common.dateFormat2.get().format(nilaiToeflToaflMahasiswa.getTanggalTest())));

			rowData = new MyFormRow();
			rowData.setParent(rowsData);
			rowData.setStyle("background: #E1F1FC;");
			rowData.appendChild(new Label(ais.common.Common.getBahasaConfig("Berlaku sampai dengan")));
			rowData.appendChild(new Label(nilaiToeflToaflMahasiswa.getMasaBerlaku() == null ? ""
					: Common.dateFormat2.get().format(nilaiToeflToaflMahasiswa.getMasaBerlaku())));

			rowData = new MyFormRow();
			rowData.setParent(rowsData);
			rowData.setStyle("background: #E1F1FC;");
			rowData.appendChild(new Label(ais.common.Common.getBahasaConfig("Section 1")));
			rowData.appendChild(new Label(Common.numberFormat.get().format(nilaiToeflToaflMahasiswa.getSkor1())));

			rowData = new MyFormRow();
			rowData.setParent(rowsData);
			rowData.setStyle("background: #E1F1FC;");
			rowData.appendChild(new Label(ais.common.Common.getBahasaConfig("Section 2")));
			rowData.appendChild(new Label(Common.numberFormat.get().format(nilaiToeflToaflMahasiswa.getSkor2())));

			rowData = new MyFormRow();
			rowData.setParent(rowsData);
			rowData.setStyle("background: #E1F1FC;");
			rowData.appendChild(new Label(ais.common.Common.getBahasaConfig("Section 3")));
			rowData.appendChild(new Label(Common.numberFormat.get().format(nilaiToeflToaflMahasiswa.getSkor3())));

			rowData = new MyFormRow();
			rowData.setParent(rowsData);
			rowData.setStyle("background: #E1F1FC;");
			rowData.appendChild(new Label(ais.common.Common.getBahasaConfig("Section 4")));
			rowData.appendChild(new Label(Common.numberFormat.get().format(nilaiToeflToaflMahasiswa.getSkor4())));

			rowData = new MyFormRow();
			rowData.setParent(rowsData);
			rowData.setStyle("background: #E1F1FC;");
			rowData.appendChild(new Label(ais.common.Common.getBahasaConfig("Total")));
			rowData.appendChild(new Label(Common.numberFormat.get().format(nilaiToeflToaflMahasiswa.getTotal())));

		}

		if (nilaiToeflToaflMahasiswas.isEmpty()) {
			row = new MyFormRow();
			row.setStyle("border:1px;solid;background: transparent;");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tidak ada informasi"));
		}

		return borderlayout;
	}

	public Borderlayout initMain(final Mahasiswa mahasiswa) throws Exception {
		this.mahasiswa = mahasiswa;
		final BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();

		terintegrasiDenganEmis = Common.bolehKonfigurasi("terintegrasi_dengan_emis", Konfigurasi.TIDAK_AKTIF);

		propinsi = new Label();
		kota = new Label();

		Common.insertCombo(agama = new Combobox(), "nama", Agama.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		pernahMenetapDiLuarNegeri = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Ya");
		comboitem.setValue(1);
		pernahMenetapDiLuarNegeri.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak");
		comboitem.setValue(0);
		pernahMenetapDiLuarNegeri.appendChild(comboitem);

		pernahMemimpinOrganisasi = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Ya");
		comboitem.setValue(1);
		pernahMemimpinOrganisasi.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Tidak");
		comboitem.setValue(0);
		pernahMemimpinOrganisasi.appendChild(comboitem);

		statusNikah = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Belum Nikah");
		comboitem.setValue(0);
		statusNikah.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Nikah");
		comboitem.setValue(1);
		statusNikah.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Janda");
		comboitem.setValue(2);
		statusNikah.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Duda");
		comboitem.setValue(3);
		statusNikah.appendChild(comboitem);

//		kancaBri = new Combobox();
//		comboitem = new MyComboitemConfig("BRI Kanca Ciputat");
//		comboitem.setValue("0382-01-");
//		kancaBri.appendChild(comboitem);
//		comboitem = new MyComboitemConfig("BRI KK Ciputat");
//		comboitem.setValue("1511-01-");
//		kancaBri.appendChild(comboitem);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setStyle("border:0px;");
		fotoMahasiswa = null;
		if (tampilFotoBiodata) {
			West west = new West();
			west.setStyle("border:0px;");
			ais.ui.util.ZkCompat.setFlex(west, true);
			west.setWidth("270px");
			west.setParent(borderlayout);

			Common.createDownloadUploadFoto(west, mahasiswa, FotoMahasiswa.class, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (arg0 != null && arg0.getData() instanceof FotoMahasiswa) {
						fotoMahasiswa = (FotoMahasiswa) arg0.getData();
					}
				}
			}, bolehgantiFoto);
		}

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabData = new MyTabConfig("Biodata");
		tabData.setParent(tabs);

		MyTabConfig tabAlamat = new MyTabConfig("Alamat");
		tabAlamat.setParent(tabs);

		MyTabConfig tabOrangTua = new MyTabConfig("Keluarga");
		tabOrangTua.setParent(tabs);

		MyTabConfig tabWali = new MyTabConfig("Wali");
		tabWali.setParent(tabs);

		MyTabConfig tabKTM = new MyTabConfig("KTM");
		tabKTM.setParent(tabs);

		MyTabConfig tabMobile = new MyTabConfig("Mobile");
		tabMobile.setParent(tabs);

		tabMobile.setVisible(Common.bolehKonfigurasi("tampilkan_mobile_di_profile_mhs"));

		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			MyTabConfig tabKelulusan = new MyTabConfig("Info Kelulusan");
			tabKelulusan.setParent(tabs);
		}

		tabKTM.setVisible(Common.bolehKonfigurasi("tampilkan_ktm_di_profile_mhs"));

		MyTabConfig tabToeflToafl = new MyTabConfig("NILAI TOEFL");
		tabToeflToafl.setParent(tabs);

		final MyTabConfig tabLampiran = new MyTabConfig("Upload dan Download Lampiran");
		tabLampiran.setParent(tabs);

		int jumlah = ((Number) HibernateUtil.currentSession().createCriteria(HasilUjianMahasiswa.class)
				.add(Restrictions.isNotNull("keyhasil")).createAlias("pertemuanPunyaUjian", "pertemuanPunyaUjian")
				.add(Restrictions.eq("mahasiswa", mahasiswa)).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

		final MyTabConfig tabHasilUjian = new MyTabConfig(Common.getBahasaConfig("Hasil Ujian") + " "
				+ (jumlah == 0 ? "" : "(" + jumlah + " " + Common.getBahasaConfig("ujian") + ")"));
		tabHasilUjian.setParent(tabs);

		boolean tampilkan_hasil_ujian_mahasiswa = Common.bolehKonfigurasi("tampilkan_hasil_ujian_mahasiswa", Konfigurasi.TIDAK_AKTIF);

		boolean terhubung_ke_ojs = Common.bolehKonfigurasi("tampilkan_penelitian_dan_pengabdian_di_mahasiswa", Konfigurasi.TIDAK_AKTIF);

		final MyTabConfig tabPenelitian = new MyTabConfig("Penelitian dan Pengabdian");
		tabPenelitian.setVisible(terhubung_ke_ojs);
		tabPenelitian.setParent(tabs);

		final MyTabConfig tabPublikasi = new MyTabConfig("Publikasi Ilmiah");
		tabPublikasi.setVisible(terhubung_ke_ojs);
		tabPublikasi.setParent(tabs);

		MyTabConfig tab1e;
		tabs.appendChild(tab1e = new MyTabConfig("Dokumen dan Lampiran"));

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelData = new ais.ui.util.MyTabpanel();
		tabpanelData.appendChild(initData(biodataMahasiswa));
		tabpanelData.setParent(tabpanels);

		tabpanelData = new ais.ui.util.MyTabpanel();
		tabpanelData.appendChild(initAlamat(biodataMahasiswa));
		tabpanelData.setParent(tabpanels);

		tabpanelData = new ais.ui.util.MyTabpanel();
		tabpanelData.appendChild(initOrangTua(biodataMahasiswa));
		tabpanelData.setParent(tabpanels);

		tabpanelData = new ais.ui.util.MyTabpanel();
		tabpanelData.appendChild(initWali(biodataMahasiswa));
		tabpanelData.setParent(tabpanels);

		final Tabpanel tabpanelKtm = new ais.ui.util.MyTabpanel();
		tabpanelKtm.setParent(tabpanels);
		tabpanelKtm.setVisible(tabKTM.isVisible());
		tabKTM.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelKtm.getChildren().isEmpty()) {
					LaporanKartuMahasiswa laporanKartuMahasiswa = new LaporanKartuMahasiswa(mahasiswa);
					laporanKartuMahasiswa.setHeight("500px");
					laporanKartuMahasiswa.setWidth("100%");
					laporanKartuMahasiswa.setParent(tabpanelKtm);
				}
			}

		});

		final Tabpanel tabpanelMobile = new ais.ui.util.MyTabpanel();
		tabpanelMobile.setParent(tabpanels);
		tabpanelMobile.setVisible(tabMobile.isVisible());
		tabMobile.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelMobile.getChildren().isEmpty()) {
					MainHelper.onDapatkanKode(new Tbmuser(mahasiswa), tabpanelMobile, false);
				}
			}

		});

		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			tabpanelData = new ais.ui.util.MyTabpanel();
			tabpanelData.appendChild(initKelulusan(mahasiswa));
			tabpanelData.setParent(tabpanels);
		}

		tabpanelData = new ais.ui.util.MyTabpanel();
		tabpanelData.setParent(tabpanels);
		tabpanelData.appendChild(initToeflToafl(mahasiswa));

		tabpanelDataLampiran = new ais.ui.util.MyTabpanel();
		tabpanelDataLampiran.setParent(tabpanels);

		tabLampiran.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelDataLampiran.getChildren().isEmpty()) {
					tabpanelDataLampiran.appendChild(initLampiran(biodataMahasiswa));
				}
			}

		});

		final Tabpanel tabpanelHasilUjian = new ais.ui.util.MyTabpanel();
		tabpanelHasilUjian.setParent(tabpanels);

		tabHasilUjian.setVisible(tampilkan_hasil_ujian_mahasiswa);
		tabpanelHasilUjian.setVisible(tampilkan_hasil_ujian_mahasiswa);

		tabHasilUjian.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelHasilUjian.getChildren().isEmpty()) {
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							new HasilUjianHelper(mahasiswa, null, null).display(tabpanelHasilUjian);
						}
					});
				}
			}

		});

		final Tabpanel tabpanelPublikasi = new ais.ui.util.MyTabpanel();
		tabpanelPublikasi.setParent(tabpanels);
		tabpanelPublikasi.setVisible(terhubung_ke_ojs);

		tabPenelitian.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (tabpanelPublikasi.getChildren().isEmpty()) {
					PengajuanPenelitianDanPengabdianHelper pengajuanPenelitianDanPengabdianHelper = new PengajuanPenelitianDanPengabdianHelper();
					MyWindow addWindowPengajuan = new MyWindow();
					addWindowPengajuan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					pengajuanPenelitianDanPengabdianHelper.displayPengajuan(false, mahasiswa.getNim(),
							PengumumanAkademis.UNTUK_MAHASISWA, null, tabpanelPublikasi, addWindowPengajuan,
							ConstantValues.PENELITIAN_LAINNYA, "500px");
				}
			}
		});

		final Tabpanel tabpanelPublikasiIlmiah = new ais.ui.util.MyTabpanel();
		tabpanelPublikasiIlmiah.setParent(tabpanels);
		tabpanelPublikasiIlmiah.setVisible(terhubung_ke_ojs);

		tabPublikasi.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (tabpanelPublikasiIlmiah.getChildren().isEmpty()) {
					DetailArtikelHelper artikelHelper = new DetailArtikelHelper(null);
					MyWindow addWindowPengajuan = new MyWindow();
					addWindowPengajuan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					artikelHelper.displayPengajuan(false, mahasiswa.getNim(), PengumumanAkademis.UNTUK_MAHASISWA, null,
							tabpanelPublikasiIlmiah, addWindowPengajuan, "500px");
				}
			}
		});

		final Tabpanel tabpanelUtamaE = new ais.ui.util.MyTabpanel();
		tabpanelUtamaE.setParent(tabpanels);
		tab1e.addEventListener(Events.ON_CLICK, new EventListener() {

			@SuppressWarnings({ "deprecation", "unchecked" })
			private void reload() {
				Common.clear(tabpanelUtamaE);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(Common.tampilanScroll(tabpanelUtamaE));
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("60%");
				columns.appendChild(column);
				column = new MyColumnConfig();
				columns.appendChild(column);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(row, "2");
				MyToolbarbuttonConfig tambahDokumen;
				row.appendChild(tambahDokumen = new MyToolbarbuttonConfig("Tambah Dokumen", "/img/svg/addthis.svg"));
				tambahDokumen.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						final MyWindow addWindow = new MyWindow("Tambah Dokumen", "none", false);
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
						addWindow.setHeight("300px");
						addWindow.setWidth("450px");

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setParent(addWindow);
						Center center = new Center();
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);
						MyGrid grid = new MyGrid();
						grid.setWidth("100%");
						grid.setParent(center);
						grid.setWidth("100%");
						grid.setHeight("100%");

						Columns columns = new Columns();
						columns.setParent(grid);

						MyColumnConfig column = new MyColumnConfig();
						column.setParent(columns);
						column.setWidth("40%");

						column = new MyColumnConfig();
						column.setParent(columns);

						Rows rowsTambah = new Rows();
						rowsTambah.setParent(grid);

						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setParent(rowsTambah);
						row.appendChild(new ais.ui.util.MyLabelConfig("Nama Dokumen"));
						String[] lampiran_Pegawai = Common
								.getKonfigurasi("lampiran_mahasiswa",
										"Akta Kelahiran;BPJS;Kartu Keluarga;KTP;NPWP;Ijazah;Prestasi")
								.getNilai().split(";");

						final Combobox dokumen = new Combobox();
						for (String s : lampiran_Pegawai) {
							Comboitem comboitem = new Comboitem(s);
							dokumen.appendChild(comboitem);
						}
						row.appendChild(dokumen);
						dokumen.setWidth("95%");

						Common.initKeterangan(rowsTambah, "Ketikkan nama dokumen jika tidak tercantum dalam pilihan");

						final MyFormRow rowDokumen = new MyFormRow();
						rowDokumen.setVisible(false);
						rowDokumen.setValign("top");
						rowDokumen.setParent(rowsTambah);
						rowDokumen.appendChild(new ais.ui.util.MyLabelConfig("File Dokumen"));

						dokumen.addEventListener("onOK", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								rowDokumen.setVisible(!dokumen.getValue().trim().isEmpty());
							}
						});
						dokumen.addEventListener("onChange", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								rowDokumen.setVisible(!dokumen.getValue().trim().isEmpty());
							}
						});

						Hbox myHbox = new Hbox();
						myHbox.setParent(rowDokumen);
						myHbox.setHeight("30px");

						Hbox hboxGambar = new Hbox();
						hboxGambar.setParent(myHbox);

						LampiranLain.createDownloadUploadFileLain(hboxGambar, mahasiswa.getId(),
								"Dokumen_Mahasiswa_" + Common.getGeneratedBarCode(), "Dokumen", false,
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										LampiranLain lainMahasiswaCover = (LampiranLain) arg0.getData();

										Session streamingSession = StreamingHibernateUtil.getInstance()
												.currentSession();
										streamingSession.refresh(lainMahasiswaCover);
										lainMahasiswaCover.setRef(mahasiswa.getId());
										lainMahasiswaCover.setJenis("Dokumen_Mahasiswa_" + dokumen.getValue().trim());

										streamingSession.getTransaction().begin();
										streamingSession.update(lainMahasiswaCover);
										streamingSession.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												addWindow.detach();
												reload();
											}
										});
									}
								});

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						// toolbar.setHeight("25px");
						toolbar.setParent(south);
						MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
						cancel.setTooltiptext("Tutup");
						cancel.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								addWindow.detach();
							}
						});
						cancel.setParent(toolbar);

						addWindow.setVisible(true);
						addWindow.onModal();
					}
				});

				Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
				List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
						.addOrder(Order.asc("id")).add(Restrictions.eq("ref", mahasiswa.getId()))
						.add(Restrictions.ilike("jenis", "Dokumen_Mahasiswa_", MatchMode.START)).list();
				StreamingHibernateUtil.getInstance().closeSession();
				String doksId = "";
				for (final LampiranLain lampiranLain : lampiranLains) {

					doksId += doksId.isEmpty() ? lampiranLain.getId().toString() : "," + lampiranLain.getId();

					row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new Label(lampiranLain.getJenis().replaceAll("Dokumen_Mahasiswa_", "")));

					Hbox hbox = new Hbox();
					row.appendChild(hbox);
					hbox.appendChild(tambahDokumen = new MyToolbarbuttonConfig("Lihat Dokumen", "/img/svg/eye.svg"));
					tambahDokumen.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.display(lampiranLain);
						}
					});

					MyToolbarbuttonConfig hapusDokumen;
					hbox.appendChild(hapusDokumen = new MyToolbarbuttonConfig("Hapus Dokumen", "/img/svg/trash.svg"));
					hapusDokumen.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							MyMessageboxConfig.show("Apakah yakin ingin menghapus dokumen ini ?", "Pertanyaan",
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {

													try {
														Session session = StreamingHibernateUtil.getInstance()
																.currentSession();

														session.getTransaction().begin();
														session.delete(lampiranLain);
														session.getTransaction().commit();

														StreamingHibernateUtil.getInstance().closeSession();
													} catch (Exception e) {
														StreamingHibernateUtil.getInstance().rollbackTransaction();
														Common.tampilErrorJikaAdmin(e);
													}

													reload();
												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
													MyMessageboxConfig.show(
															"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
																	+ e.getMessage());
												}

											}
										}
									});
						}
					});
				}

				if (!mahasiswa.getKarpeg().equalsIgnoreCase(doksId)) {
					mahasiswa.setKarpeg(doksId);
					Common.refreshSaveOrUpdate(mahasiswa);
				}
			}

			@Override
			public void onEvent(Event event) throws Exception {

				if (tabpanelUtamaE.getChildren().isEmpty()) {

					reload();

				}
			}
		});

		// try {
		// if
		// (ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa().getId().equals(ConstantValues.LULUS.getId()))
		// {
		// tabDataAlumni = new MyTabConfig("Data Alumni");
		// tabDataAlumni.setParent(tabs);
		//
		// Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		// tabpanel.setParent(tabpanels);
		//
		// initDataAlumni(tabpanel);
		// }
		// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataMahasiswaAction.java:3098");
		// Common.tampilErrorJikaAdmin(e);
		// }

		tabDataTambahan = new MyTabConfig("Data Lain");
		tabDataTambahan.setParent(tabs);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);

		initDataLain(tabpanel, biodataMahasiswa);

		// final MyTabConfig tabOrganisasi = new MyTabConfig("Organisasi
		// Mahasiswa");
		// tabOrganisasi.setParent(tabs);
		//
		// final Tabpanel tabpanelOrganisasi = new ais.ui.util.MyTabpanel();
		// tabpanelOrganisasi.setParent(tabpanels);
		//
		// tabOrganisasi.addEventListener(Events.ON_CLICK, new EventListener() {
		//
		// @Override
		// public void onEvent(Event event) throws Exception {
		// if (tabpanelOrganisasi.getChildren().isEmpty()) {
		// MahasiswaPunyaOrganisasiIntraKampusHelper detailperkuliahanHelper =
		// new MahasiswaPunyaOrganisasiIntraKampusHelper();
		// detailperkuliahanHelper.display(mahasiswa, tabpanelOrganisasi);
		// }
		// }
		// });

		final MyTabConfig tabKegiatanKemahasiswaan = new MyTabConfig("Kegiatan Kemahasiswaan");
		tabKegiatanKemahasiswaan.setParent(tabs);

		final Tabpanel tabpanelKegiatanKemahasiswaan = new ais.ui.util.MyTabpanel();
		tabpanelKegiatanKemahasiswaan.setParent(tabpanels);

		tabKegiatanKemahasiswaan.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (tabpanelKegiatanKemahasiswaan.getChildren().isEmpty()) {

					DashboardKegiatanKemahasiswaan window = new DashboardKegiatanKemahasiswaan(mahasiswa);
					ais.ui.util.BaseDasbordPortal.mountWrapped(window, tabpanelKegiatanKemahasiswaan,
						"Kegiatan Kemahasiswaan", "Rekap kegiatan organisasi dan prestasi kemahasiswaan mahasiswa ini.");

				}
			}
		});

		final MyTabConfig tabAngket = new MyTabConfig("Angket Dosen");
		tabAngket.setParent(tabs);

		final Tabpanel tabpanelAngket = new ais.ui.util.MyTabpanel();
		tabpanelAngket.setParent(tabpanels);

		tabAngket.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (tabpanelAngket.getChildren().isEmpty()) {
					MyWindow window = new MyWindow("", "none", false);
					window.setHeight("97%");
					window.setWidth("97%");
					window.setParent(tabpanelAngket);
					MyInclude iframe = new MyInclude(
							"/common/checklist_penilaian_dosen_oleh_mhs.zul?mahasiswa=" + mahasiswa.getId());
					iframe.setParent(window);
				}
			}
		});

		final MyTabConfig tabAngketUmum = new MyTabConfig("Angket Umum");
		tabAngketUmum.setParent(tabs);

		final Tabpanel tabpanelAngketUmum = new ais.ui.util.MyTabpanel();
		tabpanelAngketUmum.setParent(tabpanels);

		tabAngketUmum.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (tabpanelAngketUmum.getChildren().isEmpty()) {
					MyWindow window = new MyWindow("", "none", false);
					window.setHeight("97%");
					window.setWidth("97%");
					window.setParent(tabpanelAngketUmum);
					MyInclude iframe = new MyInclude(
							"/common/checklist_penilaian_umum.zul?mahasiswa=" + mahasiswa.getId());
					iframe.setParent(window);
				}
			}
		});
		boolean tampilRiwayatMediaSosial = Common.bolehKonfigurasi("tampilRiwayatMediaSosial");
		if (tampilRiwayatMediaSosial) {
			final MyTabConfig tabSosial = new MyTabConfig("Media Sosial");
			tabSosial.setParent(tabs);
			tabpanel = new ais.ui.util.MyTabpanel();
			tabpanel.setParent(tabpanels);

			Common.displaySocialMedia(tabSosial, tabpanel, mahasiswa);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabDataTambahan != null
						&& !ParameterTambahanMahasiswaListener.validate(biodataMahasiswa, null, false)) {
					tabDataTambahan.setVisible(true);
					tabDataTambahan.getLinkedPanel().setVisible(true);
					tabDataTambahan.setSelected(true);
				}

				if (tabDataAlumni != null
						&& !ParameterTambahanAlumniListener.validate(biodataMahasiswa, null, false, false)) {
					tabDataAlumni.setVisible(true);
					tabDataAlumni.getLinkedPanel().setVisible(true);
					tabDataAlumni.setSelected(true);
				}

			}
		});

		if (mahasiswa.getDikunci() != null) {
			Common.freezeGanti(center, true);
		}

		ais.ui.util.MyButtonTabbox.gantiTabboxNative(tabbox, new int[] { 1 });

		return borderlayout;
	}

	private void initDataLain(Tabpanel tabpanel, BiodataMahasiswa biodataMahasiswa) throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid gridEast = new MyGrid();
		gridEast.setWidth("100%");
		gridEast.setParent(center);
		gridEast.setWidth("100%");
		gridEast.setHeight("100%");

		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.setParent(gridEast);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("40%");
		columns.appendChild(column);
		column = new MyColumnConfig();
		columns.appendChild(column);

		Rows rowsEast = new Rows();
		rowsEast.setParent(gridEast);

		List<Row> parameterRows = new ArrayList<Row>();
		parameterTambahanMahasiswaListener = new ParameterTambahanMahasiswaListener(biodataMahasiswa, parameterRows,
				lampiranLains, rowsEast);
		boolean visible = parameterTambahanMahasiswaListener.check();
		// System.out.println("parameterTambahanMahasiswaListener visible => " +
		// visible);
		tabpanel.setVisible(visible);
		tabpanel.getLinkedTab().setVisible(visible);

		parameterTambahanMahasiswaListener.onEvent(null);
	}

	private void init() throws Exception {
		win.getFellowIfAny("window");

		win.setTitle("Biodata Mahasiswa");

		Borderlayout borderlayout = initMain(mahasiswa);
		borderlayout.setParent(win);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Selesai dan Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(mahasiswa, true, emailAtasan == null ? null : emailAtasan.getValue(), null)) {
					MyMessageboxConfig.show("Data berhasil disimpan!", "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
				}
			}
		});
		save.setParent(toolbar);

		MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak Biodata", "/img/svg/printer.svg");
		cetak.setVisible(mahasiswa != null && mahasiswa.getId() != null);
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
				File bio = CommonReportHelper.onCetakBiodataMahasiswa(biodataMahasiswa);
				CommonEmail.infoLengkapMahasiswa(biodataMahasiswa, bio);
			}
		});
		cetak.setParent(toolbar);

	}

	private BiodataMahasiswa biodataMahasiswadata = null;

	/**
	 * Membaca nilai integer dari sebuah {@link org.zkoss.zul.Decimalbox} dengan AMAN. Bila pengguna
	 * mengetik teks bersatuan (mis. "170 Cm", "60 Kg"), {@code Decimalbox.getValue()} melempar
	 * {@link org.zkoss.zk.ui.WrongValueException} dan MENGGAGALKAN simpan. Di sini error itu ditangani:
	 * ambil blok angka pertama dari teks mentah, bersihkan pesan error UI agar form tetap bisa
	 * disubmit, lalu kembalikan angka (atau {@code nilaiDefault} bila tak ada angka).
	 */
	private static int ambilIntDecimalboxAman(org.zkoss.zul.Decimalbox box, int nilaiDefault) {
		if (box == null) {
			return nilaiDefault;
		}
		try {
			java.math.BigDecimal v = box.getValue();
			return v == null ? nilaiDefault : v.intValue();
		} catch (org.zkoss.zk.ui.WrongValueException wve) {
			String raw = null;
			try { box.clearErrorMessage(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataMahasiswaAction.java:3327"); }
			try { raw = box.getText(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataMahasiswaAction.java:3328"); }
			Integer angka = ekstrakAngkaPertama(raw);
			return angka == null ? nilaiDefault : angka.intValue();
		} catch (Exception e) {
			return nilaiDefault;
		}
	}

	private static java.util.Date ambilTanggalAman(org.zkoss.zul.Datebox box) {
		if (box == null) {
			return null;
		}
		try {
			return box.getValue();
		} catch (org.zkoss.zk.ui.WrongValueException wve) {
			try { box.clearErrorMessage(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataMahasiswaAction.java:tanggalAmanClear"); }
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	/** Ambil blok digit pertama dari string (mis. "170 Cm" -&gt; 170); null bila tak ada digit. */
	private static Integer ekstrakAngkaPertama(String raw) {
		if (raw == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (c >= '0' && c <= '9') {
				sb.append(c);
			} else if (sb.length() > 0) {
				break;
			}
		}
		if (sb.length() == 0) {
			return null;
		}
		try {
			return Integer.valueOf(Integer.parseInt(sb.toString()));
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(final Mahasiswa mahasiswa, boolean simpanUlangMahasiswa, String emailAtasan,
			ParameterTambahanAlumniListener parameterTambahanAlumniListener) throws InterruptedException {
		try {

			if (apakahPernahPaud == null) {
				return false;
			}

			Session session = HibernateUtil.currentNativeSession();
			try {
				List<BiodataMahasiswa> biodataMahasiswas = ConstantValues
						.simpleList(
								session.createCriteria(BiodataMahasiswa.class)
										.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.asc("id")),
								BiodataMahasiswa.class);

				if (biodataMahasiswas.isEmpty()) {
					BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata(true);
					biodataMahasiswas.add(biodataMahasiswa);
				}

				for (BiodataMahasiswa biodataMahasiswa : biodataMahasiswas) {

					biodataMahasiswa.setOperatorSeluler((OperatorSeluler) (hpProvider.getSelectedItem() == null ? null
							: hpProvider.getSelectedItem().getValue()));
					biodataMahasiswa.setHpProvider(hpProvider.getValue());
					biodataMahasiswa.setNpsn(npsn.getValue());
					biodataMahasiswa.setApakahPernahPaud(apakahPernahPaud.isChecked());
					biodataMahasiswa.setApakahPernahTk(apakahPernahTk.isChecked());
					if (emailAtasan != null) {
						biodataMahasiswa.setEmailAtasan(emailAtasan.trim());
					}
					biodataMahasiswa.setJenisPenghasilanWali(
							(Penghasilan) (jenisPenghasilanWali.getSelectedItem() == null ? null
									: jenisPenghasilanWali.getSelectedItem().getValue()));
					biodataMahasiswa
							.setJenisPekerjaanWali((Pekerjaan) (jenisPekerjaanWali.getSelectedItem() == null ? null
									: jenisPekerjaanWali.getSelectedItem().getValue()));
					biodataMahasiswa
							.setJenjangPendidikanWali((Jenjang) (jenjangPendidikanWali.getSelectedItem() == null ? null
									: jenjangPendidikanWali.getSelectedItem().getValue()));

					biodataMahasiswa.setJenisPenghasilanAyah(
							(Penghasilan) (jenisPenghasilanAyah.getSelectedItem() == null ? null
									: jenisPenghasilanAyah.getSelectedItem().getValue()));

					biodataMahasiswa
							.setJenisPenghasilanIbu((Penghasilan) (jenisPenghasilanIbu.getSelectedItem() == null ? null
									: jenisPenghasilanIbu.getSelectedItem().getValue()));

					biodataMahasiswa
							.setJenisPekerjaanAyah((Pekerjaan) (jenisPekerjaanAyah.getSelectedItem() == null ? null
									: jenisPekerjaanAyah.getSelectedItem().getValue()));
					biodataMahasiswa
							.setJenisPekerjaanIbu((Pekerjaan) (jenisPekerjaanIbu.getSelectedItem() == null ? null
									: jenisPekerjaanIbu.getSelectedItem().getValue()));

					biodataMahasiswa
							.setJenjangPendidikanAyah((Jenjang) (jenjangPendidikanAyah.getSelectedItem() == null ? null
									: jenjangPendidikanAyah.getSelectedItem().getValue()));
					biodataMahasiswa
							.setJenjangPendidikanIbu((Jenjang) (jenjangPendidikanIbu.getSelectedItem() == null ? null
									: jenjangPendidikanIbu.getSelectedItem().getValue()));

					biodataMahasiswa.setAlatTransportasiMahasiswa(
							(AlatTransportasiMahasiswa) (alatTransportasiMahasiswa.getSelectedItem() == null ? null
									: alatTransportasiMahasiswa.getSelectedItem().getValue()));
					biodataMahasiswa.setJenisTinggalMahasiswa(
							(JenisTinggalMahasiswa) (jenisTinggalMahasiswa.getSelectedItem() == null ? null
									: jenisTinggalMahasiswa.getSelectedItem().getValue()));

					biodataMahasiswa.setEmail(email.getValue());
					biodataMahasiswa
							.setPendidikanAyah((PendidikanOrangTua) (pendidikanAyah.getSelectedItem() == null ? null
									: pendidikanAyah.getSelectedItem().getValue()));

					biodataMahasiswa.setNamaWali(namaWali.getValue());
					biodataMahasiswa.setTanggalLahirWali(ambilTanggalAman(tanggalLahirWali));
					biodataMahasiswa
							.setPekerjaanWali((PekerjaanOrangTua) (pekerjaanWali.getSelectedItem() == null ? null
									: pekerjaanWali.getSelectedItem().getValue()));
					biodataMahasiswa
							.setPendidikanWali((PendidikanOrangTua) (pendidikanWali.getSelectedItem() == null ? null
									: pendidikanWali.getSelectedItem().getValue()));
					biodataMahasiswa
							.setPendapatanWali((PendapatanOrangTua) (pendapatanWali.getSelectedItem() == null ? null
									: pendapatanWali.getSelectedItem().getValue()));

					biodataMahasiswa.setTanggalLahirIbu(ambilTanggalAman(tanggalLahirIbu));
					biodataMahasiswa.setTanggalLahirAyah(ambilTanggalAman(tanggalLahirAyah));

					biodataMahasiswa
							.setPendidikanIbu((PendidikanOrangTua) (pendidikanIbu.getSelectedItem() == null ? null
									: pendidikanIbu.getSelectedItem().getValue()));

					biodataMahasiswa
							.setJenisSekolah((JenisSekolahMahasiswaBaru) (jenisSekolah.getSelectedItem() == null ? null
									: jenisSekolah.getSelectedItem().getValue()));
					biodataMahasiswa.setPendapatanOrtu(pendapatanOrtu.getSelectedItem() == null ? null
							: (PendapatanOrangTua) pendapatanOrtu.getSelectedItem().getValue());

					biodataMahasiswa.setPendapatanOrtuIbu(pendapatanOrtuIbu.getSelectedItem() == null ? null
							: (PendapatanOrangTua) pendapatanOrtuIbu.getSelectedItem().getValue());

					biodataMahasiswa.setDusun(dusun.getValue());
					biodataMahasiswa.setNoIdentitas(noIdentitas.getValue());

					biodataMahasiswa.setMahasiswa(mahasiswa);
					biodataMahasiswa.setAlamat(alamat.getValue());
					biodataMahasiswa.setNamaAyah(namaAyah.getValue());
					biodataMahasiswa
							.setPekerjaanAyah((PekerjaanOrangTua) (pekerjaanAyah.getSelectedItem() == null ? null
									: pekerjaanAyah.getSelectedItem().getValue()));
					biodataMahasiswa.setNamaIbu(namaIbu.getValue());
					biodataMahasiswa.setPekerjaanIbu((PekerjaanOrangTua) (pekerjaanIbu.getSelectedItem() == null ? null
							: pekerjaanIbu.getSelectedItem().getValue()));
					biodataMahasiswa.setNamaUntukIjazah(namaUntukIjazah.getValue());
					biodataMahasiswa.setNoIjazah(noIjazah.getValue());
					biodataMahasiswa.setUkuranJaket(ukuranJaket.getValue());
					biodataMahasiswa.setNo_rek_bri(no_rek_bri.getValue());
					biodataMahasiswa.setCabangBri(cabangBri.getValue());
					biodataMahasiswa.setKodeKerjaan(kodeKerjaan.getValue());

					biodataMahasiswa.setNoKK(noKK.getValue());

					biodataMahasiswa.setPernahMenetapDiLuarNegeri(
							(Integer) (pernahMenetapDiLuarNegeri.getSelectedItem() == null ? null
									: pernahMenetapDiLuarNegeri.getSelectedItem().getValue()));

					biodataMahasiswa.setTinggiBadan(ambilIntDecimalboxAman(beratBadan, 0));
					biodataMahasiswa.setTinggiBadan(ambilIntDecimalboxAman(tinggiBadan, 0));
					biodataMahasiswa.setTeleponRumah(teleponRumah.getValue().trim());
					biodataMahasiswa.setHp(hp.getValue().trim());
					biodataMahasiswa.setSuratIzinMengemudi(suratIzinMengemudi.getValue());
					biodataMahasiswa.setKendaraanKuliah(kendaraanKuliah.getValue());
					biodataMahasiswa.setPernahMemimpinOrganisasi(
							(Integer) (pernahMemimpinOrganisasi.getSelectedItem() == null ? null
									: pernahMemimpinOrganisasi.getSelectedItem().getValue()));
					biodataMahasiswa.setNamaOrganisasi(namaOrganisasi.getValue());
					biodataMahasiswa.setHobi(hobi.getValue());
					biodataMahasiswa.setMinatSeni(minatSeni.getValue());
					biodataMahasiswa.setKemampuanBahasa1(kemampuanBahasa1.getValue());
					biodataMahasiswa.setKemampuanBahasa2(kemampuanBahasa2.getValue());
					biodataMahasiswa.setKemampuanBahasa3(kemampuanBahasa3.getValue());
					biodataMahasiswa.setAsalSma(asalSma.getValue());
					biodataMahasiswa.setNamaSekolahAsal((NamaSekolahAsal) asalSma.getAttribute("namaSekolahAsal"));
					biodataMahasiswa.setAlamatAsalSma(alamatAsalSma.getValue());
					biodataMahasiswa.setAsalSmp(asalSmp.getValue());
					biodataMahasiswa.setAlamatAsalSmp(alamatAsalSmp.getValue());
					biodataMahasiswa.setAsalSd(asalSd.getValue());
					biodataMahasiswa.setAlamatAsalSd(alamatAsalSd.getValue());
					biodataMahasiswa.setGolonganDarah(golonganDarah.getValue());
					biodataMahasiswa.setStatusNikah((Integer) (statusNikah.getSelectedItem() == null ? null
							: statusNikah.getSelectedItem().getValue()));
					biodataMahasiswa.setKewarganegaraan("");
					biodataMahasiswa.setAgama(
							(Agama) (agama.getSelectedItem() == null ? null : agama.getSelectedItem().getValue()));
					try {
						biodataMahasiswa.setBersaudara(bersaudara.getValue());
					} catch (WrongValueException e) {
						// Intbox "jumlah bersaudara" -- user mengetik teks bersatuan (mis. "10 orang")
						// bukan angka murni. Intbox.getValue() melempar WrongValueException "You must
						// specify an integer, rather than 10 orang." Tangani KHUSUS di sini (jangan
						// biarkan tertangkap oleh catch(WrongValueException) umum di bawah yang
						// pesannya tentang format tanggal -- akan menyesatkan user) dan hentikan simpan
						// agar data tidak valid tidak ikut tersimpan.
						try { bersaudara.clearErrorMessage(); } catch (Exception ce) { ais.common.ErrorAuditUtil.record(ce, "auto-audit(empty-catch) src/ais/action/master/BiodataMahasiswaAction.java:bersaudara"); }
						ais.common.Common.tampilErrorJikaAdmin(e);
						MyMessageboxConfig.show(
								"Format tidak valid, harap isi hanya angka pada jumlah bersaudara. (" + e.getMessage() + ")",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
						return false;
					}

					biodataMahasiswa.setKelurahan(kelurahan.getValue());
					biodataMahasiswa.setKecamatan((Wilayah) kecamatan.getAttribute("wilayah"));
					biodataMahasiswa.setPropinsi((Propinsi) (propinsi.getAttribute("wilayah")));
					biodataMahasiswa.setKota((Kota) (kota.getAttribute("wilayah")));
					biodataMahasiswa.setRt(rt.getValue());
					biodataMahasiswa.setRw(rw.getValue());
					biodataMahasiswa.setKodepos(kodepos.getValue());
					biodataMahasiswa
							.setBeratBadan(beratBadan.getValue() == null ? 0 : beratBadan.getValue().intValue());
					biodataMahasiswa
							.setTinggiBadan(tinggiBadan.getValue() == null ? 0 : tinggiBadan.getValue().intValue());

					biodataMahasiswa.setGolonganDarah(golonganDarah.getValue());

					biodataMahasiswa.setNisn(nisn.getValue().trim());
					biodataMahasiswa.setNirm(nirm.getValue().trim());
					biodataMahasiswa.setNpwp(npwp.getValue().trim());
					biodataMahasiswa.setNikAyah(nikAyah.getValue().trim());
					biodataMahasiswa.setNikIbu(nikIbu.getValue().trim());

					biodataMahasiswa.setTelpAyah(telpAyah.getValue().trim());
					biodataMahasiswa.setTelpIbu(telpIbu.getValue().trim());
					biodataMahasiswa.setTelpWali(telpWali.getValue().trim());

					if (parameterTambahanMahasiswaListener != null) {
						parameterTambahanMahasiswaListener.onSave(biodataMahasiswa);
					}

					if (parameterTambahanAlumniListener != null) {
						parameterTambahanAlumniListener.onSave(biodataMahasiswa);
					}

					// refreshUpdate mandiri (kelola sesi + transaksi sendiri). Pola begin/commit manual
					// pada native session rawan "Transaction not successfully started" bila sesi ditutup
					// helper bersarang di tengah proses.
					Common.refreshUpdate(biodataMahasiswa);

					biodataMahasiswadata = biodataMahasiswa;
				}
				mahasiswa.biodataMahasiswa = biodataMahasiswadata;
			} catch (WrongValueException e) {
				// Validasi field tanggal (mis. tanggalLahirWali/tanggalLahirIbu/tanggalLahirAyah)
				// gagal parse -- terjadi kalau user mengetik teks tanggal manual yang tidak valid
				// di Datebox (bukan lewat date-picker) sebelum submit; Datebox.getValue() melempar
				// WrongValueException "You must specify a date. Format: dd-MM-yyyy".
				// Sebelumnya tertangkap generic catch di bawah & hanya dicatat admin (silent
				// bagi user biasa) -> tampilkan pesan jelas spt pola BiodataCalonMahasiswaAction.
				ais.common.Common.tampilErrorJikaAdmin(e);
				MyMessageboxConfig.show("Format tanggal tidak valid, gunakan dd-MM-yyyy. (" + e.getMessage() + ")",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				return false;
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} finally {
				// session adalah currentNativeSession() -- jangan ditutup manual
				// (dikelola oleh framework ZK/Hibernate; menutup manual menyebabkan double-close)
				try { session.clear(); } catch (Exception ignored) {}
			}

			HibernateUtil.closeSession();

			// Guard: bila penyimpanan gagal (mis. nilai melebihi panjang kolom), biodataMahasiswadata
			// bisa null sehingga println debug ini melempar NullPointerException dan menutupi pesan asli.
			if (biodataMahasiswadata != null) {
				System.out.println("Simpan -> biodataMahasiswa " + biodataMahasiswadata + ", npwp " + npwp.getValue()
						+ " " + biodataMahasiswadata.getNpwp());
			}

			if (fotoEventListener != null) {
				fotoEventListener.onEvent(null);
			}

			if (simpanUlangMahasiswa) {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						session.refresh(mahasiswa);

						if (statusSetelahLulus != null) {
							mahasiswa.setStatusDomisiliSetelahLulus(
									(StatusDomisiliSetelahLulus) (statusDomisiliSetelahLulus.getSelectedItem() == null
											? null
											: statusDomisiliSetelahLulus.getSelectedItem().getValue()));

							mahasiswa.setStatusSetelahLulus(
									(StatusSetelahLulus) (statusSetelahLulus.getSelectedItem() == null ? null
											: statusSetelahLulus.getSelectedItem().getValue()));

							mahasiswa.setStatusPekerjaanSetelahLulus(
									(StatusPekerjaanSetelahLulus) (statusPekerjaanSetelahLulus.getSelectedItem() == null
											? null
											: statusPekerjaanSetelahLulus.getSelectedItem().getValue()));

						}

						mahasiswa.setPin(pin.getValue() == null ? null : pin.getValue().longValue());
						mahasiswa.setEmail(email.getValue());
						mahasiswa.setAlamat(alamat.getValue());
						mahasiswa.setTinggi_badan(ambilIntDecimalboxAman(tinggiBadan, 0));
						mahasiswa.setBerat_badan(ambilIntDecimalboxAman(beratBadan, 0));
						mahasiswa.setGolongan_darah(golonganDarah.getValue());
						mahasiswa.setKelamin(kelamin.getSelectedItem() == null ? null
								: kelamin.getSelectedItem().getValue().toString());
						java.util.Date tanggallahirAman = ambilTanggalAman(tanggallahir);
						if (tanggallahirAman != null) {
							mahasiswa.setTanggallahir(tanggallahirAman);
						}
						mahasiswa.setTempatlahir(tempatlahir.getValue());
						mahasiswa.setAgama(
								(Agama) (agama.getSelectedItem() == null ? null : agama.getSelectedItem().getValue()));
						// Guard sama seperti baris ~3578: biodataMahasiswadata bisa null bila
						// penyimpanan biodata di try sinkron sebelumnya gagal (exception-nya
						// ditelan, tidak di-rethrow), sementara callback timer ini tetap jalan.
						if (biodataMahasiswadata != null) {
							mahasiswa.setTelp(biodataMahasiswadata.getHp());
							mahasiswa.setKtp(biodataMahasiswadata.getNoIdentitas());
						}
						mahasiswa.setBahasa((String) bahasa.getSelectedItem().getValue());

						mahasiswa.setNamaArab(namaArab.getValue());
						mahasiswa.setNamaTionghoa(namaTionghoa.getValue());

						if (tanggallahirManual != null) {
							mahasiswa.setTanggallahirManual(tanggallahirManual.getValue());
						}

						if (BiodataMahasiswaAction.this.dosen != null
								&& BiodataMahasiswaAction.this.dosen.getAttribute("myValue") != null) {
							mahasiswa.setDosen(
									((Dosen) BiodataMahasiswaAction.this.dosen.getAttribute("myValue")).getId());

							try {
								KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
								if ((mahasiswa.getDosen() != null && krsMahasiswa.getDosenPa() == null)
										|| (mahasiswa.getDosen() != null
												&& !krsMahasiswa.getDosenPa().getId().equals(mahasiswa.getDosen()))) {
									krsMahasiswa.setDosenPa(
											(Dosen) BiodataMahasiswaAction.this.dosen.getAttribute("myValue"));
									Common.refreshSaveOrUpdate(session, krsMahasiswa);
								}
								Integer tahapanTemp = krsMahasiswa.getTahapan();
								if (tahapanTemp != null && tahapanTemp == 0) {
									tahapanTemp = null;
								}
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}

						Common.refreshUpdate(session, mahasiswa);

						if (ttd != null && ttd.getId() != null) {
							try {
								session = StreamingHibernateUtil.getInstance().currentSession();

								session.refresh(ttd);
								ttd.setRef(mahasiswa.getId());

								session.getTransaction().begin();
								session.update(ttd);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}

						}

						if (fotoMahasiswa != null && fotoMahasiswa.getId() != null) {
							try {
								Session sessionS = StreamingHibernateUtil.getInstance().currentSession();

								sessionS.refresh(fotoMahasiswa);
								fotoMahasiswa.setMahasiswa(mahasiswa.getId());

								sessionS.getTransaction().begin();
								sessionS.update(fotoMahasiswa);
								sessionS.getTransaction().commit();

							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}
							StreamingHibernateUtil.getInstance().closeSession();
						}

						Tbmuser tbmuser = Common.getCurrentUser();
						if (tbmuser.getMahasiswa() != null
								&& tbmuser.getMahasiswa().getId().equals(mahasiswa.getId())) {
							tbmuser.setMahasiswa(mahasiswa);

							Sessions.getCurrent().setAttribute("mytbmuser", tbmuser);
							Sessions.getCurrent().setAttribute("usersTemp", tbmuser);

						}

						if (refresh != null && refresh) {
							Clients.confirmClose(null);
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Executions.sendRedirect("/main");
								}
							});
						}

					}
				});
			}

			return true;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return false;
	}

	public boolean isTampilFotoBiodata() {
		return tampilFotoBiodata;
	}

	public void setTampilFotoBiodata(boolean tampilFotoBiodata) {
		this.tampilFotoBiodata = tampilFotoBiodata;
	}

}
