package ais.action.master.pmb;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.DiskonCalonMahasiswaAction;
import ais.action.master.InterviewCalonMahasiswaAction;
import ais.action.master.dashboard.helper.DashboardRekapMahasiswaBaruSemua;
import ais.action.master.helper.FilterLanjutHelper;
import ais.action.master.helper.HasilUjianHelper;
import ais.action.master.helper.KegiatanHelper;
import ais.action.master.helper.RevisiBiodataCalonMahasiswaHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.pmb.nim.NimGenerator;
import ais.action.report.CommonReportHelper;
import ais.action.report.format1.akademik.LaporanKartuMahasiswa;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonMedia;
import ais.common.CommonPMB;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.PmbArkatama;
import ais.common.LaporanUpload;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.AfiliasiCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiBerkas;
import ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran;
import ais.database.model.BuktiPembayaran;
import ais.database.model.CekKesehatan;
import ais.database.model.CommonVO;
import ais.database.model.DiskonMahasiswa;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jenjang;
import ais.database.model.JenisSekolahMahasiswaBaru;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jurusan;
import ais.database.model.JurusanSekolahMahasiswaBaru;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.MatapelajaranSekolah;
import ais.database.model.Paket;
import ais.database.model.PaketPunyaMatapelajaran;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.RuangPaketPMB;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.VerifikasiKelengkapanCalonMahasiswa;
import ais.database.model.file.FotoBiodataCalonMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.LampiranLainBiodataCalonMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk cetak registrasi. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Paging paging},
 * {@code Tabbox tabboxDataCalonMahasiswa}, {@code int tabAktifDataCalonMahasiswa}, {@code Textbox searchnama},
 * {@code Textbox searchnoreg}, {@code Textbox searchsekolah}, {@code MyToolbarbuttonConfig uploadUKT};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code buatSaranUrutanNim()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code onDownloadLampiran()}, {@code onUploadUKT()}, {@code
 * onUploadNIM()}, {@code onDownloadFoto()}, {@code tampilkanAnalisisUrutanNim()}, {@code
 * getJumlahDigitUrutNim()}); operasi domain lain ({@code onPilihanCalonMahasiswa()}, {@code onSpanPtkin()},
 * {@code onInterview()}, {@code onKelompok()}, {@code onRekapRekap()}, {@code onStatistik()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class CetakRegistrasiAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	private static final long serialVersionUID = 1155733365712985677L;

	private MyGrid grid;
	private Paging paging;
	private Tabbox tabboxDataCalonMahasiswa;
	private final int[] tabAktifDataCalonMahasiswa = new int[] { 1 };

	private Textbox searchnama;
	private Textbox searchnoreg;
	private Textbox searchsekolah;
	private MyToolbarbuttonConfig uploadUKT;
	private MyToolbarbuttonConfig uploadNIM;
	private Textbox searchujian;
	private Textbox searchkabkota;
	private MyDatebox searchTanggalLahirDari;
	private MyDatebox searchTanggalLahirSampai;
	private Textbox searchnim;
	private Checkbox searchaktif;
	private Combobox searchTahunAjaran;
	private Combobox searchGelombang;
	private Combobox searchJenisSeleksi;
	private Combobox searchSemester;
	private Combobox searchJenjang;
	private Combobox searchJenisSekolahMahasiswaBaru;
	private Combobox searchJurusanSekolahMahasiswaBaru;
	private Combobox searchPaket;
	private Combobox searchProgram;
	private Textbox searchAfiliasi;
	private Combobox searchProdiPilihan1;
	private Combobox searchProdiPilihan2;
	private Combobox searchProdiPilihan3;
	private Combobox searchProdiLulus;
	private Combobox searchSyarat;

	private MyCheckboxConfig tampilkanTidakAdaTagihanReg;
	private MyCheckboxConfig tampilkanTidakAdaTagihanDaftarUlang;

	private MyCheckboxConfig tampilkanYgSudahBayarDaftarUlang;
	private MyCheckboxConfig tampilkanYgSudahLunasDaftarUlang;
	private MyCheckboxConfig tampilkanYgBelumLunasDaftarUlang;
	private MyCheckboxConfig tampilkanYgSudahdapatNIM;
	private Combobox searchStatusAwalMahasiswa;

	private MyCheckboxConfig tampilkanYgSudahBayar;
	private MyCheckboxConfig tampilkanYgBelumBayar;

	private MyCheckboxConfig tampilkanYgBelumBayarDaftarUlang;
	private MyCheckboxConfig tampilkanYgBelumdapatNIM;
	private MyCheckboxConfig mengisiFormTambahan;
	private MyCheckboxConfig diterima;
	private MyCheckboxConfig mundur;
	private MyCheckboxConfig ditolak;
	private MyCheckboxConfig dptNoUjian;
	private MyCheckboxConfig blmNoUjian;
	private MyCheckboxConfig blmditerima;
	private PerguruanTinggi selectedPerguruanTinggi;
	private MyCheckboxConfig telahLogin;
	private MyCheckboxConfig blmVerifBekas;

	private MyToolbarbuttonConfig find;

	private String tahunAkademikPenerimaanMahasiswaBaru;

	public static String[] contents = new String[] { "id", "noRegistrasi", "noUjian", "nama", "totalSkor", "alamat",
			"rt", "rw", "kelurahanCalon", "kecamatanCalon", "kotaCalon", "propinsiCalon", "namaSekolahAsal",
			"namaSekolahAsal.kode", "pembayaranRegistrasi", "pembayaranDaftarUlang", "kodePos", "tempatLahir",
			"tanggalLahir", "jenisKelamin", "asalNegara", "kewarganegaraan", "jenisKartuIdentitas", "noIdentitas",
			"email", "nisn", "jenisSekolah", "akreditasiSekolah", "kodePosSekolah", "kecamatanSekolah", "kotaSekolah",
			"propinsiSekolah", "tahunKelulusan", "jurusanSekolah", "jurusanSekolahLain", "namaWali", "noTelpOrtu",
			"pendapatanOrtu", "pendidikanOrtu", "alamatOrtu", "rtOrtu", "rwOrtu", "kodePosOrtu", "kecamatanOrtu",
			"kelurahanOrtu", "propinsiOrtu", "kotaOrtu", "paket", "prodi1", "prodi2", "prodi3", "prodi4", "prodi5",
			"jenjang", "statusLulus", "prodiLulus", "nimGenerated", "cetakKartu", "program", "jenisSeleksi",
			"tanggalDaftar", "tahun", "semesterMulai", "tahunAkademik", "gelombangPendaftaran", "tanggalPendaftaran",
			"agama", "semesterMulai", "program", "hp", "namaAyah", "pendidikanAyah", "pekerjaanAyah", "namaIbu",
			"pendidikanIbu", "pekerjaanIbu", "namaUntukIjazah", "noIjazah", "ukuranJaket", "tinggiBadan",
			"pernahMenetapDiLuarNegeri", "beratBadan", "teleponRumah", "suratIzinMengemudi", "kendaraanKuliah",
			"pernahMemimpinOrganisasi", "namaOrganisasi", "hobi", "minatSeni", "kemampuanBahasa1", "kemampuanBahasa2",
			"kemampuanBahasa3", "asalSma", "alamatAsalSma", "asalSmp", "alamatAsalSmp", "asalSd", "alamatAsalSd",
			// CATATAN: "jenisKuliah" BUKAN properti Hibernate BiodataCalonMahasiswa
			// (lihat DaftarUlangMahasiswaBaruAction: label "Jenis Kuliah" diisi dari
			// getProgram()) -- sebelumnya memicu QueryException "could not resolve
			// property: jenisKuliah" tiap upload (tertangkap & dilewati, tapi jadi
			// noise di audit). Dialihkan ke "program" (properti asli) yang SUDAH ada
			// lebih dulu di array ini, jadi otomatis dilewati aman lewat mekanisme
			// dedup colomSudahMasuk di ObjectHelper.setObjectValues -- posisi kolom
			// Excel TIDAK berubah.
			"golonganDarah", "statusNikah", "program", "statusPembayaran", "nim", "mahasiswa", "merupakanPindahan",
			"pindahanDariKampus", "pindahanDariProdi", "nimLamaSebelumPindah", "pindahDariKampusLamaDiSemester",
			"tanggalPindah", "keteranganPindah", "infoKampusDariMana", "namaTemanInfoKampusDariMana",
			"keteranganInfoKampusDariMana", "dariNamaDosenKaryawan", "pinPassword", "parameterTambahan",
			"parameterTambahanInds", "tanggal_dirubah", "oleh", "keterangan", "telahLogin", "waktuLogin",
			"afiliasiCalonMahasiswa", "afiliasiPegawai", "afiliasiMahasiswa" };

	private boolean edit = false;
	private boolean delete = false;
	private boolean create = false;

	private Tabpanel pilihanCalonMahasiswa;

	public void onPilihanCalonMahasiswa(Event event) throws Exception {

		if (pilihanCalonMahasiswa.getChildren().isEmpty()) {

			MyButtonTabbox tabbox = MyButtonTabbox.buat(pilihanCalonMahasiswa, "100%", new int[] { 1 });
			Div tabpanelUtama = tabbox.tambahTab(1, "Data Peserta");

			MyWindow window = CommonReportHelper.onCetakDataPMBFoto();
			window.setClosable(false);
			window.setTitle("");
			window.setHeight("100%");
			window.setWidth("100%");
			window.setBorder("none");
			tabpanelUtama.appendChild(window);

			tabbox.tambahTabLazy(2, "Daftar Hadir Ujian", new MyButtonTabbox.PemuatTab() {
				@Override
				public void muat(Div panel) throws Exception {
					MyWindow window = CommonReportHelper.onCetakAbsensiPMBFoto();
					window.setClosable(false);
					window.setTitle("");
					window.setHeight("100%");
					window.setWidth("100%");
					window.setBorder("none");
					panel.appendChild(window);
				}
			});
			tabbox.pilih(1);

		}
	}

	private Tabpanel laporanSpanPtkin;

	public void onSpanPtkin(Event event) {
		if (laporanSpanPtkin.getChildren().isEmpty()) {
			MyInclude halaman = new MyInclude("/pages/master/upload_biodata_calon_mahasiswa_span_ptkin.zul");
			halaman.setHeight("100%");
			halaman.setWidth("100%");
			laporanSpanPtkin.appendChild(halaman);
		}
	}

	private Tabpanel interviewCalonMahasiswa;

	public void onInterview(Event event) {
		if (interviewCalonMahasiswa.getChildren().isEmpty()) {
			MyInclude halaman = new MyInclude("/pages/master/interview_calon_mahasiswa.zul");
			halaman.setHeight("100%");
			halaman.setWidth("100%");
			interviewCalonMahasiswa.appendChild(halaman);
		}
	}

	private Tabpanel kelompokCalonMahasiswa;

	public void onKelompok(Event event) {
		if (kelompokCalonMahasiswa.getChildren().isEmpty()) {
			MyInclude halaman = new MyInclude("/pages/master/kelompok_calon_mahasiswa.zul");
			halaman.setHeight("100%");
			halaman.setWidth("100%");
			kelompokCalonMahasiswa.appendChild(halaman);
		}
	}

	private Tabpanel rekaprekap;

	public void onRekapRekap(Event event) {
		if (rekaprekap.getChildren().isEmpty()) {
			DashboardRekapMahasiswaBaruSemua rekapMahasiswaBaruSemua = new DashboardRekapMahasiswaBaruSemua();
			ais.ui.util.BaseDasbordPortal.mountWrapped(rekapMahasiswaBaruSemua, rekaprekap,
				"Rekap Mahasiswa Baru", "Ringkasan lengkap jumlah dan komposisi mahasiswa baru yang terdaftar.");
		}
	}

	private Tabpanel statistik;

	public void onStatistik(Event event) {
		if (statistik.getChildren().isEmpty()) {
			MyInclude halaman = new MyInclude("/pages/pmb/statistik.zul");
			halaman.setHeight("100%");
			halaman.setWidth("100%");
			statistik.appendChild(halaman);
		}
	}

	private Tabpanel dasborKeuangan;

	public void onDasborKeuangan(Event event) {
		if (dasborKeuangan.getChildren().isEmpty()) {
			MyInclude halaman = new MyInclude("/pages/pmb/dasbor_keuangan_pmb.zul");
			halaman.setHeight("100%");
			halaman.setWidth("100%");
			dasborKeuangan.appendChild(halaman);
		}
	}

	@SuppressWarnings("unchecked")
	public void onDownloadLampiran(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Long> calonMahasiswa = initCriteria(true).list();
				File fileFolderLampiran = new File(
						"/opt/ecampus/lampiran_" + ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
				fileFolderLampiran.mkdirs();
				System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());

				/**
				 * Helper implementasi bersarang milik {@link CetakRegistrasiAction} untuk file download helper. Kelas ini
				 * mengemas langkah lokal yang dipakai kelas induk dan bukan service domain alternatif.
				 *
				 * <p><b>Scope:</b> setiap instance terikat pada instance {@link CetakRegistrasiAction} dan dapat mengakses
				 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
				 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code download}(). Aturan bisnis bersama
				 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
				 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
				 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
				 * tambahkan perilaku lintas domain pada service bersama.</p>
				 *
				 * @see CetakRegistrasiAction
				 */
				class FileDownloadHelper {
					public File download(String jenis, BiodataCalonMahasiswa biodataCalonMahasiswa,
							File fileFolderCalon) {
						File fileCopy = null;
						Session streamingSession = null;
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();
							int jumlah = ((Number) streamingSession
									.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
									.setProjection(Projections.rowCount()).add(Restrictions.eq("jenis", jenis))
									.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId()))
									.setMaxResults(1).uniqueResult()).intValue();
							if (jumlah > 0) {
								LampiranLainBiodataCalonMahasiswa lampiranLainBiodataCalonMahasiswa = (LampiranLainBiodataCalonMahasiswa) streamingSession
										.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
										.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId()))
										.add(Restrictions.eq("jenis", jenis)).setMaxResults(1).uniqueResult();

								if (lampiranLainBiodataCalonMahasiswa != null
										&& lampiranLainBiodataCalonMahasiswa.getGdrive() != null) {
									fileCopy = new File(fileFolderCalon.getAbsolutePath() + "/" + jenis + "_"
											+ URLEncoder.encode(biodataCalonMahasiswa.getNoRegistrasi() + "_"
													+ biodataCalonMahasiswa.getNama(), "UTF-8")
											+ ".txt");
									FileUtils.writeStringToFile(fileCopy,
											lampiranLainBiodataCalonMahasiswa.forwardGDriveUrl());
								} else if (lampiranLainBiodataCalonMahasiswa != null
										&& lampiranLainBiodataCalonMahasiswa.getFoto() != null) {

									File file = lampiranLainBiodataCalonMahasiswa.ambilFile();
									fileCopy = new File(
											fileFolderCalon.getAbsolutePath() + "/" + jenis + "_" + file.getName());
									System.out.println("fileCopy => " + fileCopy.getAbsolutePath());

									FileOutputStream fileOutputStream = null;
									FileInputStream fileInputStream = null;
									try {
										fileOutputStream = new FileOutputStream(fileCopy);
										fileInputStream = new FileInputStream(file);
										IOUtils.copyLarge(fileInputStream, fileOutputStream);
									} finally {
										if (fileInputStream != null) {
											try {
												fileInputStream.close();
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										}
										if (fileOutputStream != null) {
											try {
												fileOutputStream.close();
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										}
									}
								}
							}

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						} finally {
							try {
								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						}
						return fileCopy;
					}
				}

				FileDownloadHelper downloadHelper = new FileDownloadHelper();

				File folderOut = new File(Common.REAL_PATH + "/media/");
				try {
					folderOut.mkdirs();
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

				for (Long biodataCalonMahasiswaid : calonMahasiswa) {
					BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
							.ambil(BiodataCalonMahasiswa.class.getName(), biodataCalonMahasiswaid);
					if (biodataCalonMahasiswa != null) {
						File fileFolderCalon = new File(fileFolderLampiran.getAbsolutePath() + "/"
								+ URLEncoder.encode(
										biodataCalonMahasiswa.getNoRegistrasi() + "_" + biodataCalonMahasiswa.getNama(),
										"UTF-8"));
						fileFolderCalon.mkdirs();
						System.out.println("fileFolderCalon => " + fileFolderCalon.getAbsolutePath());

						Session streamingSession = null;
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							FotoBiodataCalonMahasiswa fotobiodataCalonMahasiswa = (FotoBiodataCalonMahasiswa) streamingSession
									.createCriteria(FotoBiodataCalonMahasiswa.class)
									.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId()))
									.setMaxResults(1).uniqueResult();

							if (fotobiodataCalonMahasiswa != null && fotobiodataCalonMahasiswa.getLink() != null
									&& fotobiodataCalonMahasiswa.getLink().toLowerCase().contains("dropbox")) {
								File fileCopy = new File(fileFolderCalon.getAbsolutePath() + "/FOTO_"
										+ URLEncoder.encode(biodataCalonMahasiswa.getNoRegistrasi() + "_"
												+ biodataCalonMahasiswa.getNama(), "UTF-8")
										+ ".txt");
								FileUtils.writeStringToFile(fileCopy, fotobiodataCalonMahasiswa.dropboxLinkRaw());
							} else if (fotobiodataCalonMahasiswa != null
									&& fotobiodataCalonMahasiswa.getGdrive() != null) {
								File fileCopy = new File(fileFolderCalon.getAbsolutePath() + "/FOTO_"
										+ URLEncoder.encode(biodataCalonMahasiswa.getNoRegistrasi() + "_"
												+ biodataCalonMahasiswa.getNama(), "UTF-8")
										+ ".txt");
								FileUtils.writeStringToFile(fileCopy, fotobiodataCalonMahasiswa.forwardGDriveUrl());
							} else if (fotobiodataCalonMahasiswa != null
									&& fotobiodataCalonMahasiswa.getFoto() != null) {
								File fileFoto = fotobiodataCalonMahasiswa.ambilFile();
								File fileCopy = new File(
										fileFolderCalon.getAbsolutePath() + "/FOTO_" + fileFoto.getName());
								System.out.println("fileCopy => " + fileCopy.getAbsolutePath());

								FileOutputStream fileOutputStream = null;
								FileInputStream fileInputStream = null;
								try {
									fileOutputStream = new FileOutputStream(fileCopy);
									fileInputStream = new FileInputStream(fileFoto);
									IOUtils.copyLarge(fileInputStream, fileOutputStream);
								} finally {
									if (fileInputStream != null) {
										try {
											fileInputStream.close();
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									}
									if (fileOutputStream != null) {
										try {
											fileOutputStream.close();
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									}
								}
							}

						} catch (Exception e1) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/pmb/CetakRegistrasiAction.java:509");
						} finally {
							try {
								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						}

						downloadHelper.download(LampiranLainBiodataCalonMahasiswa.IJAZAH, biodataCalonMahasiswa,
								fileFolderCalon);
						downloadHelper.download(LampiranLainBiodataCalonMahasiswa.TRANSKRIP_NILAI,
								biodataCalonMahasiswa, fileFolderCalon);
						downloadHelper.download(LampiranLainBiodataCalonMahasiswa.KTP, biodataCalonMahasiswa,
								fileFolderCalon);
						downloadHelper.download(LampiranLainBiodataCalonMahasiswa.LAMPIRAN_1, biodataCalonMahasiswa,
								fileFolderCalon);
						downloadHelper.download(LampiranLainBiodataCalonMahasiswa.LAMPIRAN_2, biodataCalonMahasiswa,
								fileFolderCalon);
						downloadHelper.download(LampiranLainBiodataCalonMahasiswa.LAMPIRAN_3, biodataCalonMahasiswa,
								fileFolderCalon);
						downloadHelper.download(LampiranLainBiodataCalonMahasiswa.LAMPIRAN_4, biodataCalonMahasiswa,
								fileFolderCalon);
						downloadHelper.download(LampiranLainBiodataCalonMahasiswa.LAMPIRAN_5, biodataCalonMahasiswa,
								fileFolderCalon);

						downloadHelper.download(LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_PENDAFTARAN,
								biodataCalonMahasiswa, fileFolderCalon);
						downloadHelper.download(LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_DAFTAR_ULANG,
								biodataCalonMahasiswa, fileFolderCalon);

						String[] spl = biodataCalonMahasiswa.getParameterTambahanInds().split("\n");
						for (String d : spl) {
							String[] value = d.split("<=>");
							String jenis = value.length > 0 ? value[0].trim() : "";
							String val = value.length > 1 ? value[1].trim() : "";
							String url = value.length > 2 ? value[2].trim() : "";
							if (!url.trim().isEmpty()) {
								File fileCopy = null;
								LampiranLain lam = LampiranLain.ambil(biodataCalonMahasiswa.getId(), jenis);

								if (lam != null && lam.getGdrive() != null) {
									fileCopy = new File(
											fileFolderCalon.getAbsolutePath() + "/" + URLEncoder.encode(val, "UTF-8")
													+ "_" + URLEncoder.encode(biodataCalonMahasiswa.getNoRegistrasi()
															+ "_" + biodataCalonMahasiswa.getNama(), "UTF-8")
													+ ".txt");
									FileUtils.writeStringToFile(fileCopy, lam.forwardGDriveUrl());
								} else if (lam != null) {

									File file;
									if (lam.getGdrive() != null && !lam.getGdrive().trim().isEmpty()) {
										file = new File(folderOut.getAbsolutePath() + "/"
												+ URLEncoder.encode(lam.getNama(), "UTF-8") + ".txt");
										FileUtils.writeStringToFile(file, lam.forwardGDriveUrl());
									} else if (lam.getLink() != null && !lam.getLink().trim().isEmpty()) {
										file = new File(folderOut.getAbsolutePath() + "/"
												+ URLEncoder.encode(lam.getNama(), "UTF-8") + ".txt");
										FileUtils.writeStringToFile(file, lam.getLink().trim());
									} else {
										file = lam.ambilFile();
									}
									fileCopy = new File(fileFolderCalon.getAbsolutePath() + "/"
											+ URLEncoder.encode(val, "UTF-8") + "_" + file.getName());
									System.out.println("fileCopy => " + fileCopy.getAbsolutePath());

									FileOutputStream fileOutputStream = null;
									FileInputStream fileInputStream = null;
									try {
										fileOutputStream = new FileOutputStream(fileCopy);
										fileInputStream = new FileInputStream(file);
										IOUtils.copyLarge(fileInputStream, fileOutputStream);
									} finally {
										if (fileInputStream != null) {
											try {
												fileInputStream.close();
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										}
										if (fileOutputStream != null) {
											try {
												fileOutputStream.close();
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
										}
									}
								}
							}
						}

						Session session = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();
							GelombangPendaftaran gel = biodataCalonMahasiswa.getGelombangPendaftaran();
							if (gel != null) {
								session.refresh(gel);
								Set<VerifikasiKelengkapanCalonMahasiswa> verifikasiKelengkapanCalonMahasiswasTemp = gel
										.getVerifikasiKelengkapanCalonMahasiswas();

								JenisSeleksi jenisSeleksi = biodataCalonMahasiswa.getJenisSeleksi();
								if (jenisSeleksi != null) {
									session.refresh(jenisSeleksi);
									if (!jenisSeleksi.getVerifikasiKelengkapanCalonMahasiswas().isEmpty()) {
										verifikasiKelengkapanCalonMahasiswasTemp = jenisSeleksi
												.getVerifikasiKelengkapanCalonMahasiswas();
									}
								}

								List<VerifikasiKelengkapanCalonMahasiswa> verifikasiKelengkapanCalonMahasiswas = new ArrayList<VerifikasiKelengkapanCalonMahasiswa>(
										verifikasiKelengkapanCalonMahasiswasTemp);

								try {
									Collections.sort(verifikasiKelengkapanCalonMahasiswas);
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

								for (VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa : verifikasiKelengkapanCalonMahasiswas) {

									BiodataCalonMahasiswaPunyaVerifikasiBerkas biodataCalonMahasiswaPunyaVerifikasiBerkas = (BiodataCalonMahasiswaPunyaVerifikasiBerkas) session
											.createCriteria(BiodataCalonMahasiswaPunyaVerifikasiBerkas.class)
											.add(Restrictions.eq("verifikasiKelengkapanCalonMahasiswa",
													verifikasiKelengkapanCalonMahasiswa))
											.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
											.setMaxResults(1).uniqueResult();

									if (biodataCalonMahasiswaPunyaVerifikasiBerkas == null) {
										biodataCalonMahasiswaPunyaVerifikasiBerkas = new BiodataCalonMahasiswaPunyaVerifikasiBerkas();
										biodataCalonMahasiswaPunyaVerifikasiBerkas
												.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
										biodataCalonMahasiswaPunyaVerifikasiBerkas
												.setVerifikasiKelengkapanCalonMahasiswa(
														verifikasiKelengkapanCalonMahasiswa);

										Transaction tx = session.beginTransaction();
										session.saveOrUpdate(biodataCalonMahasiswaPunyaVerifikasiBerkas);
										tx.commit();
									}

									String jenis = BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName();
									LampiranLain lam = LampiranLain
											.ambil(biodataCalonMahasiswaPunyaVerifikasiBerkas.getId(), jenis);

									System.out.println("jenis => " + jenis + ", lam => " + lam);
									if (lam != null && lam.getGdrive() != null) {
										File fileCopy = new File(
												fileFolderCalon.getAbsolutePath() + "/"
														+ URLEncoder.encode(biodataCalonMahasiswa.getNoRegistrasi()
																+ "_" + biodataCalonMahasiswa.getNama(), "UTF-8")
														+ ".txt");
										FileUtils.writeStringToFile(fileCopy, lam.forwardGDriveUrl());
									} else if (lam != null) {
										File file;
										if (lam.getGdrive() != null && !lam.getGdrive().trim().isEmpty()) {
											file = new File(folderOut.getAbsolutePath() + "/"
													+ URLEncoder.encode(lam.getNama(), "UTF-8") + ".txt");
											FileUtils.writeStringToFile(file, lam.forwardGDriveUrl());
										} else if (lam.getLink() != null && !lam.getLink().trim().isEmpty()) {
											file = new File(folderOut.getAbsolutePath() + "/"
													+ URLEncoder.encode(lam.getNama(), "UTF-8") + ".txt");
											FileUtils.writeStringToFile(file, lam.getLink().trim());
										} else {
											file = lam.ambilFile();
										}

										File fileCopy = new File(fileFolderCalon.getAbsolutePath() + "/"
												+ URLEncoder.encode(biodataCalonMahasiswa.getNoRegistrasi() + "_"
														+ biodataCalonMahasiswa.getNama(), "UTF-8")
												+ "_" + file.getName());
										System.out.println("fileCopy => " + fileCopy.getAbsolutePath());

										FileOutputStream fileOutputStream = null;
										FileInputStream fileInputStream = null;
										try {
											fileOutputStream = new FileOutputStream(fileCopy);
											fileInputStream = new FileInputStream(file);
											IOUtils.copyLarge(fileInputStream, fileOutputStream);
										} finally {
											if (fileInputStream != null) {
												try {
													fileInputStream.close();
												} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
											}
											if (fileOutputStream != null) {
												try {
													fileOutputStream.close();
												} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
											}
										}
									}
								}
							}
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						} finally {
							if (session != null && session.isOpen()) {
								try {
									session.close();
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							}
						}
					}
				}
				File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
				Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
				Filedownload.save(fileFolderLampiranZip, "application/zip");

			}
		}, "Harap tunggu.. sedang melakukan proses download lampiran..");

	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void onAdd(Event event) throws Exception {

		BiodataCalonMahasiswaAction biodataCalonMahasiswaAction = new BiodataCalonMahasiswaAction("Calon Mahasiswa",
				"none", true, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						final Timer timer = new Timer(500);
						page.getFirstRoot().appendChild(timer);
						timer.addEventListener("onTimer", new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
								timer.detach();
							}
						});
						timer.start();
					}
				});

		page.getFirstRoot().appendChild(biodataCalonMahasiswaAction);
		biodataCalonMahasiswaAction.setWidth("900px");
		biodataCalonMahasiswaAction.setHeight("95%");
		biodataCalonMahasiswaAction.onModal();

	}

	public static void onEdit(BiodataCalonMahasiswa biodataCalonMahasiswa, final DataSearchDefault dataSearchDefault)
			throws Exception {

		if (biodataCalonMahasiswa.getId() != null) {
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				session.refresh(biodataCalonMahasiswa);
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
				if (session != null && session.isOpen()) {
					try {
						session.close();
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				}
			}
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		if (tbmuser == null || (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null)) {
			boolean harusBayarSebelumLogin = biodataCalonMahasiswa.getGelombangPendaftaran()
					.getHarusBayarSebelumBisaLogin();

			if (harusBayarSebelumLogin) {
				if (!CommonPMB.isPembayaranRegistrasiTerpenuhi(
						biodataCalonMahasiswa.getPembayaranRegistrasi())) {

					MyMessageboxConfig.show(
							"Calon mahasiswa harus melakukan pembayaran registrasi sebelum dapat melengkapi biodata dan berkas.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}
			}

			if (biodataCalonMahasiswa.getGelombangPendaftaran().getTanggalDaftarUlangBerakhir() != null
					&& biodataCalonMahasiswa.getGelombangPendaftaran().getTanggalDaftarUlangBerakhir()
							.before(WaktuUtil.getDate())) {

				if (!Common.dateFormat1.get()
						.format(biodataCalonMahasiswa.getGelombangPendaftaran().getTanggalDaftarUlangBerakhir())
						.equals(Common.dateFormat1.get().format(WaktuUtil.getDate()))) {
					MyMessageboxConfig.show(
							"Tanggal melengkapi berkas belum mulai/telah terlewat. Periksa kembali jadwal.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}
			}
		}

		BiodataCalonMahasiswaAction biodataCalonMahasiswaAction = new BiodataCalonMahasiswaAction("Calon Mahasiswa",
				"none", true, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								dataSearchDefault.onSearchDefault(arg0);
							}
						});
					}
				}, biodataCalonMahasiswa);

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(biodataCalonMahasiswaAction);
		biodataCalonMahasiswaAction.setWidth("850px");
		biodataCalonMahasiswaAction.setHeight("95%");
		biodataCalonMahasiswaAction.onModal();
	}

	private boolean harusBayarSebelumLogin = true;
	private boolean integrasi_pmb_arkatama = false;
	private MyCheckboxConfig cariBlmMasukFeeder;
	private boolean tampilkanInformasiUjianDiPMB = true;

	public void onUploadUKT(Event event) throws Exception {

		ForwardEvent forwardEvent = (ForwardEvent) event;
		final Media media = ((UploadEvent) forwardEvent.getOrigin()).getMedia();
		if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
			return;
		if (media.getName().toLowerCase().endsWith("xlsx")) {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					InputStream inputStream = null;
					FileOutputStream fileOutputStream = null;
					File file = null;

					try {
						inputStream = media.getStreamData();
						file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
						file.getParentFile().mkdirs();
						fileOutputStream = new FileOutputStream(file);

						// OPTIMASI: Gunakan IOUtils.copyLarge untuk upload lebih cepat
						IOUtils.copyLarge(inputStream, fileOutputStream);

					} finally {
						if (fileOutputStream != null) {
							try {
								fileOutputStream.close();
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						}
						if (inputStream != null) {
							try {
								inputStream.close();
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						}
					}

					if (file == null || !file.exists())
						return;

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
						Session session = null;
						Transaction tx = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();
							tx = session.beginTransaction();

							String noRegistrasi = Common.getCellContent(Common.getCell(sheet, 0, i));
							String noUjian = Common.getCellContent(Common.getCell(sheet, 1, i));
							String nim = Common.getCellContent(Common.getCell(sheet, 2, i));

							BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) (noRegistrasi == null
									|| noRegistrasi.trim().isEmpty()
											? null
											: session.createCriteria(BiodataCalonMahasiswa.class)
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))
													.add(Restrictions.ilike("noRegistrasi", noRegistrasi,
															MatchMode.EXACT))
													.setMaxResults(1).uniqueResult());
							if (biodataCalonMahasiswa == null) {
								biodataCalonMahasiswa = (BiodataCalonMahasiswa) (noUjian == null
										|| noUjian.trim().isEmpty()
												? null
												: session.createCriteria(BiodataCalonMahasiswa.class)
														.add(Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true)))
														.add(Restrictions.ilike("noUjian", noUjian, MatchMode.EXACT))
														.setMaxResults(1).uniqueResult());
							}
							if (biodataCalonMahasiswa == null) {
								biodataCalonMahasiswa = (BiodataCalonMahasiswa) (nim == null || nim.trim().isEmpty()
										? null
										: session.createCriteria(BiodataCalonMahasiswa.class)
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))
												.add(Restrictions.ilike("nim", nim, MatchMode.EXACT)).setMaxResults(1)
												.uniqueResult());
							}
							StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) Common
									.getSheetContentAsObject(sheet, 4, i, StatusAwalMahasiswa.class);

							System.out.println(" biodataCalonMahasiswa = " + biodataCalonMahasiswa
									+ ", statusAwalMahasiswa = " + statusAwalMahasiswa + " nim " + nim);

							if (biodataCalonMahasiswa == null || statusAwalMahasiswa == null) {
								continue;
							}

							if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getMahasiswa() != null
									&& nim != null && !nim.trim().isEmpty()) {
								Mahasiswa mahasiswa = biodataCalonMahasiswa.getMahasiswa();
								session.refresh(mahasiswa);
								mahasiswa.setNim(nim);
								session.update(mahasiswa);
							}

							if (biodataCalonMahasiswa != null) {
								biodataCalonMahasiswa.setStatusAwalMahasiswa(statusAwalMahasiswa);
								session.update(biodataCalonMahasiswa);
							}

							tx.commit();

						} catch (Exception e) {
							if (tx != null && tx.isActive()) {
								try {
									tx.rollback();
								} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/CetakRegistrasiAction.java:955");
								}
							}
							Common.tampilErrorJikaAdmin(e);
						} finally {
							if (session != null && session.isOpen()) {
								try {
									session.close();
								} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/CetakRegistrasiAction.java:963");
								}
							}
						}
					}

					MyMessageboxConfig.show("Update NIM berhasil dilakukan", "Pemberitahuan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
				}
			});

		} else {
			MyMessageboxConfig.show(
					"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
							+ media,
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
		}
	}

	public void onUploadNIM(Event event) throws Exception {

		ForwardEvent forwardEvent = (ForwardEvent) event;
		final Media media = ((UploadEvent) forwardEvent.getOrigin()).getMedia();
		if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
			return;
		if (media.getName().toLowerCase().endsWith("xlsx")) {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					InputStream inputStream = null;
					FileOutputStream fileOutputStream = null;
					File file = null;
					try {
						inputStream = media.getStreamData();
						file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
						file.getParentFile().mkdirs();
						fileOutputStream = new FileOutputStream(file);

						// OPTIMASI: Gunakan IOUtils.copyLarge untuk upload lebih cepat
						IOUtils.copyLarge(inputStream, fileOutputStream);

					} finally {
						if (fileOutputStream != null) {
							try {
								fileOutputStream.close();
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						}
						if (inputStream != null) {
							try {
								inputStream.close();
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						}
					}

					if (file == null || !file.exists())
						return;

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
						Session session = null;
						Transaction tx = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();
							tx = session.beginTransaction();

							String noRegistrasi = Common.getCellContent(Common.getCell(sheet, 0, i));
							String noUjian = Common.getCellContent(Common.getCell(sheet, 1, i));

							BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) (noRegistrasi == null
									|| noRegistrasi.trim().isEmpty()
											? null
											: session.createCriteria(BiodataCalonMahasiswa.class)
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))
													.add(Restrictions.ilike("noRegistrasi", noRegistrasi,
															MatchMode.EXACT))
													.setMaxResults(1).uniqueResult());
							if (biodataCalonMahasiswa == null) {
								biodataCalonMahasiswa = (BiodataCalonMahasiswa) (noUjian == null
										|| noUjian.trim().isEmpty()
												? null
												: session.createCriteria(BiodataCalonMahasiswa.class)
														.add(Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true)))
														.add(Restrictions.ilike("noUjian", noUjian, MatchMode.EXACT))
														.setMaxResults(1).uniqueResult());
							}

							String nim = Common.getCellContent(Common.getCell(sheet, 2, i));

							System.out.println(
									" biodataCalonMahasiswa = " + biodataCalonMahasiswa + ", nim mahasiswa = " + nim);

							if (biodataCalonMahasiswa == null || nim == null || nim.trim().isEmpty()) {
								continue;
							}

							if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getMahasiswa() != null
									&& nim != null && !nim.trim().isEmpty()) {
								Mahasiswa mahasiswa = biodataCalonMahasiswa.getMahasiswa();
								session.refresh(mahasiswa);
								mahasiswa.setNim(nim);
								session.update(mahasiswa);
							} else {
								/*
								 * Transaksi baris ini sudah dimulai dan akan di-commit oleh pemanggil.
								 * Mode commit manual pada saveMahasiswa memulai/commit transaksi yang sama
								 * beberapa kali, sehingga tx.commit() di bawah menerima transaksi yang sudah
								 * selesai ("Transaction not successfully started").
								 */
								Mahasiswa mahasiswa = CommonPMB.saveMahasiswa(session, biodataCalonMahasiswa, nim,
										false);
								if (mahasiswa != null) {
									CommonPMB.copyLampiran(biodataCalonMahasiswa, mahasiswa);
								}
								System.out.println(" biodataCalonMahasiswa = " + biodataCalonMahasiswa
										+ ", new mahasiswa = " + mahasiswa);
							}

							/*
							 * saveMahasiswa dapat memulihkan benturan request paralel dengan rollback
							 * lalu memulai transaksi pengganti pada session yang sama. Variabel tx di
							 * atas masih menunjuk transaksi lama yang sudah selesai. Ambil kembali
							 * transaksi aktif dari session sebelum commit agar tidak muncul
							 * "Transaction not successfully started".
							 */
							tx = session.getTransaction();
							if (tx != null && tx.isActive()) {
								tx.commit();
							}

						} catch (Exception e) {
							if (tx != null && tx.isActive()) {
								try {
									tx.rollback();
								} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/CetakRegistrasiAction.java:1090");
								}
							}
							Common.tampilErrorJikaAdmin(e);
						} finally {
							/* openSession(): rollback aktif + clear/disconnect/close wajib di finally. */
							HibernateUtil.closeSessionQuietly(session);
						}
					}

					MyMessageboxConfig.show("Update NIM berhasil dilakukan", "Pemberitahuan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
				}
			});

		} else {
			MyMessageboxConfig.show(
					"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
							+ media,
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
		}
	}

	private AfiliasiCalonMahasiswa afiliasiCalonMahasiswaData = null;
	private Tbmuser tbmuser;

	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		tbmuser = Common.getCurrentUser();

		tampilkanInformasiUjianDiPMB = Common.bolehKonfigurasi("tampilkan_informasi_ujian_di_pmb");

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		create = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchSemester.appendChild(comboitem);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null) {
			try {
				for (Object aa : ConstantValues.ambilBerdasarClass(AfiliasiCalonMahasiswa.class).values()) {
					AfiliasiCalonMahasiswa afiliasiCalonMahasiswa = (AfiliasiCalonMahasiswa) aa;
					if (afiliasiCalonMahasiswa.getAktif()) {
						String khususUser = afiliasiCalonMahasiswa.getKhususUsername();
						for (String username : khususUser.split(",")) {
							if (username.equalsIgnoreCase(tbmuser.getUserId())) {
								afiliasiCalonMahasiswaData = afiliasiCalonMahasiswa;
							}
						}
					}
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}

		tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		Common.insertComboDanSemua(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(searchJenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertComboDanSemua(searchJenisSeleksi, "nama", "deskripsi", JenisSeleksi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(searchJenisSekolahMahasiswaBaru, "nama", "keterangan",
				JenisSekolahMahasiswaBaru.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(searchJurusanSekolahMahasiswaBaru, "nama", "keterangan",
				JurusanSekolahMahasiswaBaru.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(searchPaket, "nama", "keterangan", Paket.class,
				Restrictions.and(
						selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi),
										Restrictions.isNull("perguruanTinggi")),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

		Common.initPrograms(searchProgram);

		Common.insertComboDanSemua(searchProdiPilihan1, "nama", "fakultas", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(searchProdiPilihan2, "nama", "fakultas", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(searchProdiPilihan3, "nama", "fakultas", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(searchProdiLulus, "nama", "fakultas", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);

		Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);

		EventListener gelombangEventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.insertComboDanSemua(searchGelombang, "nama", GelombangPendaftaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								searchTahunAjaran.getSelectedItem() == null
										|| searchTahunAjaran.getSelectedItem().getValue() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("tahunAkademik",
														searchTahunAjaran.getSelectedItem().getValue())));
				searchGelombang.setReadonly(true);
				searchGelombang.setSelectedIndex(searchGelombang.getChildren().size() - 1);
			}
		};

		gelombangEventListener.onEvent(null);
		searchTahunAjaran.addEventListener("onChange", gelombangEventListener);

		if (create && edit) {

			String[] contentsUkt = new String[] { "noRegistrasi", "noUjian", "nim", "nama", "statusAwalMahasiswa",
					"prodiLulus" };

			MyToolbarbuttonConfig ukt = Common.cetakDataCustomButton(BiodataCalonMahasiswa.class, this, "Download UKT",
					"/img/print.png", contentsUkt);
			ukt.setVisible(Common.bolehKonfigurasi("tampilkan_form_download_dan_upload_ukt", Konfigurasi.TIDAK_AKTIF));
			Common.appendKeToolbar(ukt, find, comp);

			String[] contentsNim = new String[] { "noRegistrasi", "noUjian", "nim", "nama", "prodiLulus" };
			MyToolbarbuttonConfig nim = Common.cetakDataCustomButton(BiodataCalonMahasiswa.class, this, "Download NIM",
					"/img/print.png", contentsNim);
			nim.setVisible(Common.bolehKonfigurasi("tampilkan_form_download_dan_upload_nim_di_calon_mhs", Konfigurasi.TIDAK_AKTIF));
			Common.appendKeToolbar(nim, find, comp);

			if (uploadUKT != null) {
				uploadUKT.setVisible(ukt.isVisible());
			}
			if (uploadNIM != null) {
				uploadNIM.setVisible(nim.isVisible());
			}

			List<String> columnHeadersAdding = new ArrayList<String>();
			columnHeadersAdding.add("Foto");
			columnHeadersAdding.add(LampiranLainBiodataCalonMahasiswa.IJAZAH);
			columnHeadersAdding.add(LampiranLainBiodataCalonMahasiswa.TRANSKRIP_NILAI);
			columnHeadersAdding.add(LampiranLainBiodataCalonMahasiswa.KTP);
			columnHeadersAdding.add(LampiranLainBiodataCalonMahasiswa.LAMPIRAN_1);
			columnHeadersAdding.add(LampiranLainBiodataCalonMahasiswa.LAMPIRAN_2);
			columnHeadersAdding.add(LampiranLainBiodataCalonMahasiswa.LAMPIRAN_3);
			columnHeadersAdding.add(LampiranLainBiodataCalonMahasiswa.LAMPIRAN_4);
			columnHeadersAdding.add(LampiranLainBiodataCalonMahasiswa.LAMPIRAN_5);
			columnHeadersAdding.add(LampiranLainBiodataCalonMahasiswa.LAMPIRAN_6);
			columnHeadersAdding.add(LampiranLainBiodataCalonMahasiswa.LAMPIRAN_7);
			columnHeadersAdding.add(LampiranLainBiodataCalonMahasiswa.LAMPIRAN_8);
			columnHeadersAdding.add(LampiranLainBiodataCalonMahasiswa.LAMPIRAN_9);
			columnHeadersAdding.add(LampiranLainBiodataCalonMahasiswa.LAMPIRAN_10);
			columnHeadersAdding.add(LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_PENDAFTARAN);
			columnHeadersAdding.add(LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_DAFTAR_ULANG);

			EventListener dataAdding = new EventListener() {

				private Map<String, Integer> lbs = null;
				private int urutanTerakhir = 0;

				@Override
				public void onEvent(Event arg0) throws Exception {

					try {
						Object[] objects = (Object[]) arg0.getData();
						BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) objects[0];
						XSSFRow row = (XSSFRow) objects[2];
						XSSFWorkbook workbook = (XSSFWorkbook) objects[3];
						XSSFRow rowTambahan = (XSSFRow) objects[4];
						XSSFRow rowheadTambahan = (XSSFRow) objects[5];
						XSSFFont hlink_font = workbook.createFont();
						hlink_font.setUnderline(XSSFFont.U_SINGLE);
						hlink_font.setColor(new XSSFColor(Color.BLUE));

						final XSSFCellStyle hlink_style = workbook.createCellStyle();
						hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
						hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
						hlink_style.setFont(hlink_font);

						/**
						 * Helper implementasi bersarang milik {@link CetakRegistrasiAction} untuk data adding helper. Kelas ini
						 * mengemas langkah lokal yang dipakai kelas induk dan bukan service domain alternatif.
						 *
						 * <p><b>Scope:</b> setiap instance terikat pada instance {@link CetakRegistrasiAction} dan dapat mengakses
						 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
						 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code process}(). Aturan bisnis bersama
						 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
						 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
						 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
						 * tambahkan perilaku lintas domain pada service bersama.</p>
						 *
						 * @see CetakRegistrasiAction
						 */
						class DataAddingHelper {
							public void process(XSSFRow row, int index, BiodataCalonMahasiswa biodataCalonMahasiswa,
									String jenis) throws Exception {
								Session streamingSession = null;
								try {
									streamingSession = StreamingHibernateUtil.getInstance().currentSession();
									Long ids = (Long) (streamingSession
											.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
											.setProjection(Projections.property("id"))
											.add(Restrictions.eq("jenis", jenis)).add(Restrictions
													.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId()))
											.setMaxResults(1).uniqueResult());

									XSSFCell cell = row.createCell(index);

									if (ids != null) {
										LampiranLainBiodataCalonMahasiswa nama = (LampiranLainBiodataCalonMahasiswa) (streamingSession
												.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
												.add(Restrictions.eq("jenis", jenis))
												.add(Restrictions.eq("biodataCalonMahasiswa",
														biodataCalonMahasiswa.getId()))
												.setMaxResults(1).uniqueResult());

										cell.setCellStyle(hlink_style);
										cell.setCellValue(nama.getNama());
										String url = nama.createLinkUri(false);

										XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
												.createHyperlink(Hyperlink.LINK_URL);
										link.setAddress(url);
										cell.setHyperlink(link);
									}
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								} finally {
									try {
										StreamingHibernateUtil.getInstance().closeSession();
									} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
								}
							}
						}

						XSSFCell cell = row.createCell(contents.length);

						Session streamingSession = null;
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							FotoBiodataCalonMahasiswa fotobiodataCalonMahasiswa = (FotoBiodataCalonMahasiswa) streamingSession
									.createCriteria(FotoBiodataCalonMahasiswa.class)
									.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId()))
									.setMaxResults(1).uniqueResult();

							if (fotobiodataCalonMahasiswa != null && fotobiodataCalonMahasiswa.getGdrive() != null) {
								cell.setCellStyle(hlink_style);
								cell.setCellValue("Foto");
								String url = fotobiodataCalonMahasiswa.createLinkUri(false);

								XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
										.createHyperlink(Hyperlink.LINK_URL);
								link.setAddress(url);
								cell.setHyperlink(link);
							} else if (fotobiodataCalonMahasiswa != null
									&& fotobiodataCalonMahasiswa.getFoto() != null) {
								cell.setCellStyle(hlink_style);
								cell.setCellValue("Foto");
								String url = fotobiodataCalonMahasiswa.createLinkUri(false);

								XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
										.createHyperlink(Hyperlink.LINK_URL);
								link.setAddress(url);
								cell.setHyperlink(link);
							}
						} catch (Exception e1) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/pmb/CetakRegistrasiAction.java:1370");
						} finally {
							try {
								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						}

						DataAddingHelper dataAddingHelper = new DataAddingHelper();
						dataAddingHelper.process(row, contents.length + 1, biodataCalonMahasiswa,
								LampiranLainBiodataCalonMahasiswa.IJAZAH);
						dataAddingHelper.process(row, contents.length + 2, biodataCalonMahasiswa,
								LampiranLainBiodataCalonMahasiswa.TRANSKRIP_NILAI);
						dataAddingHelper.process(row, contents.length + 3, biodataCalonMahasiswa,
								LampiranLainBiodataCalonMahasiswa.KTP);
						dataAddingHelper.process(row, contents.length + 4, biodataCalonMahasiswa,
								LampiranLainBiodataCalonMahasiswa.LAMPIRAN_1);
						dataAddingHelper.process(row, contents.length + 5, biodataCalonMahasiswa,
								LampiranLainBiodataCalonMahasiswa.LAMPIRAN_2);
						dataAddingHelper.process(row, contents.length + 6, biodataCalonMahasiswa,
								LampiranLainBiodataCalonMahasiswa.LAMPIRAN_3);
						dataAddingHelper.process(row, contents.length + 7, biodataCalonMahasiswa,
								LampiranLainBiodataCalonMahasiswa.LAMPIRAN_4);
						dataAddingHelper.process(row, contents.length + 8, biodataCalonMahasiswa,
								LampiranLainBiodataCalonMahasiswa.LAMPIRAN_5);
						dataAddingHelper.process(row, contents.length + 9, biodataCalonMahasiswa,
								LampiranLainBiodataCalonMahasiswa.LAMPIRAN_6);
						dataAddingHelper.process(row, contents.length + 10, biodataCalonMahasiswa,
								LampiranLainBiodataCalonMahasiswa.LAMPIRAN_7);
						dataAddingHelper.process(row, contents.length + 11, biodataCalonMahasiswa,
								LampiranLainBiodataCalonMahasiswa.LAMPIRAN_8);
						dataAddingHelper.process(row, contents.length + 12, biodataCalonMahasiswa,
								LampiranLainBiodataCalonMahasiswa.LAMPIRAN_9);
						dataAddingHelper.process(row, contents.length + 13, biodataCalonMahasiswa,
								LampiranLainBiodataCalonMahasiswa.LAMPIRAN_10);
						dataAddingHelper.process(row, contents.length + 14, biodataCalonMahasiswa,
								LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_PENDAFTARAN);
						dataAddingHelper.process(row, contents.length + 15, biodataCalonMahasiswa,
								LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_DAFTAR_ULANG);

						if (rowTambahan != null) {
							rowTambahan.createCell(0).setCellValue(biodataCalonMahasiswa.getId());
							rowTambahan.createCell(1).setCellValue(biodataCalonMahasiswa.getNoRegistrasi());
							rowTambahan.createCell(2).setCellValue(biodataCalonMahasiswa.getNoUjian());
							rowTambahan.createCell(3).setCellValue(biodataCalonMahasiswa.getNama());

							Session session = null;
							try {
								session = HibernateUtil.getSessionFactory().openSession();
								int j = 0;
								GelombangPendaftaran gel = biodataCalonMahasiswa.getGelombangPendaftaran();
								if (gel != null && gel.getId() != null) {
									session.refresh(gel);
									Set<VerifikasiKelengkapanCalonMahasiswa> verifikasiKelengkapanCalonMahasiswasTemp = gel
											.getVerifikasiKelengkapanCalonMahasiswas();

									JenisSeleksi jenisSeleksi = biodataCalonMahasiswa.getJenisSeleksi();
									if (jenisSeleksi != null) {
										session.refresh(jenisSeleksi);
										if (!jenisSeleksi.getVerifikasiKelengkapanCalonMahasiswas().isEmpty()) {
											verifikasiKelengkapanCalonMahasiswasTemp = jenisSeleksi
													.getVerifikasiKelengkapanCalonMahasiswas();
										}
									}

									List<VerifikasiKelengkapanCalonMahasiswa> verifikasiKelengkapanCalonMahasiswas = new ArrayList<VerifikasiKelengkapanCalonMahasiswa>(
											verifikasiKelengkapanCalonMahasiswasTemp);

									try {
										Collections.sort(verifikasiKelengkapanCalonMahasiswas);
									} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

									for (VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa : verifikasiKelengkapanCalonMahasiswas) {

										BiodataCalonMahasiswaPunyaVerifikasiBerkas berkas = (BiodataCalonMahasiswaPunyaVerifikasiBerkas) session
												.createCriteria(BiodataCalonMahasiswaPunyaVerifikasiBerkas.class)
												.add(Restrictions.eq("verifikasiKelengkapanCalonMahasiswa",
														verifikasiKelengkapanCalonMahasiswa))
												.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
												.setMaxResults(1).uniqueResult();

										if (berkas == null) {
											berkas = new BiodataCalonMahasiswaPunyaVerifikasiBerkas();
											berkas.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
											berkas.setVerifikasiKelengkapanCalonMahasiswa(
													verifikasiKelengkapanCalonMahasiswa);
											Transaction tx = session.beginTransaction();
											session.saveOrUpdate(berkas);
											tx.commit();
										}

										int indexCol = j + 4;
										if (rowheadTambahan != null) {
											XSSFCell hssfCell = rowheadTambahan.getCell(indexCol);
											if (hssfCell == null) {
												rowheadTambahan.createCell(indexCol).setCellValue(berkas
														.getVerifikasiKelengkapanCalonMahasiswa().getId() + "-"
														+ berkas.getVerifikasiKelengkapanCalonMahasiswa().getNama());
											}
										}

										XSSFCell cellTambahan = rowTambahan.createCell(indexCol);
										if (berkas.getKeterangan().isEmpty()) {
											cellTambahan.setCellValue(berkas.getVerified());
										} else {
											cellTambahan
													.setCellValue(berkas.getVerified() + ";" + berkas.getKeterangan());
										}

										LampiranLain lampiranLain = LampiranLain.ambil(berkas.getId(),
												BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName());
										if (lampiranLain != null) {
											cellTambahan.setCellStyle(hlink_style);
											String url = lampiranLain.getGdrive() != null
													? lampiranLain.forwardGDriveUrl()
													: lampiranLain.createLinkUri(false);

											XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
													.createHyperlink(Hyperlink.LINK_URL);
											link.setAddress(url);
											cellTambahan.setHyperlink(link);
										}
										j++;
									}
								}

								List<MatapelajaranSekolah> matapelajaranSekolahs = session
										.createCriteria(PaketPunyaMatapelajaran.class)
										.setProjection(Projections.property("matapelajaranSekolah"))
										.createAlias("matapelajaranSekolah", "matapelajaranSekolah")
										.add(Restrictions.or(Restrictions.isNull("paket"),
												Restrictions.eq("paket", biodataCalonMahasiswa.getPaket())))
										.add(Restrictions.eq("matapelajaranSekolah.aktif", true))
										.addOrder(Order.asc("matapelajaranSekolah.nama")).list();

								for (MatapelajaranSekolah matapelajaranSekolah : matapelajaranSekolahs) {

									BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran berkas = (BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran) session
											.createCriteria(BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran.class)
											.add(Restrictions.eq("matapelajaranSekolah", matapelajaranSekolah))
											.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
											.setMaxResults(1).uniqueResult();

									if (berkas == null) {
										berkas = new BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran();
										berkas.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
										berkas.setMatapelajaranSekolah(matapelajaranSekolah);
										Transaction tx = session.beginTransaction();
										session.saveOrUpdate(berkas);
										tx.commit();
									}

									if (rowheadTambahan != null && biodataCalonMahasiswa.getPaket() != null
											&& biodataCalonMahasiswa.getPaket().getKelasVerifikasiRapor() != null) {

										for (String nilaikelas : biodataCalonMahasiswa.getPaket()
												.getKelasVerifikasiRapor().split(";")) {
											if (!nilaikelas.trim().isEmpty()) {
												int indexCol = j + 4;
												j++;
												XSSFCell hssfCell = rowheadTambahan.getCell(indexCol);
												if (hssfCell == null) {
													String[] ca = StringUtils.split(nilaikelas, ":");
													String kel = ca.length > 0 ? ca[0] : "";
													String sem = ca.length > 1 ? ca[1] : "";
													String s = matapelajaranSekolah.getNama() + ", Kls:" + kel
															+ (sem.isEmpty() ? "" : ", Smt:" + sem);

													rowheadTambahan.createCell(indexCol).setCellValue(s);
												}

												XSSFCell cellTambahan = rowTambahan.createCell(indexCol);
												cellTambahan.setCellValue(berkas.ambilNilai(nilaikelas.trim()));
											}
										}
									}
								}
							} catch (Exception ex) {
								ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/pmb/CetakRegistrasiAction.java:1551");
							} finally {
								if (session != null && session.isOpen()) {
									try {
										session.close();
									} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
								}
							}

							List<CommonVO> d = biodataCalonMahasiswa.ambilDataParameterTambahan();
							if (lbs == null) {
								lbs = new HashMap<String, Integer>();
								// int o = j; // Gunakan j terakhir dari loop verifikasi di atas jika diperlukan
								// ... karena j dideklarasikan di dalam blok try, kita harus hati-hati
								int o = contents.length + 16;
								for (CommonVO commonVO : d) {
									int indexCol = o + 4;
									o++;
									String lbl = commonVO.getName();
									if (rowheadTambahan != null) {
										XSSFCell hssfCell = rowheadTambahan.getCell(indexCol);
										if (hssfCell == null) {
											rowheadTambahan.createCell(indexCol).setCellValue(lbl);
										}
									}
									lbs.put(lbl, indexCol);
									urutanTerakhir = indexCol;
								}
							} else {
								for (CommonVO commonVO : d) {
									String lbl = commonVO.getName();
									if (!lbs.containsKey(lbl)) {
										int indexCol = urutanTerakhir + 1;
										if (rowheadTambahan != null) {
											XSSFCell hssfCell = rowheadTambahan.getCell(indexCol);
											if (hssfCell == null) {
												rowheadTambahan.createCell(indexCol).setCellValue(lbl);
											}
										}
										lbs.put(lbl, indexCol);
										urutanTerakhir = indexCol;
									}
								}
							}

							for (String lblData : lbs.keySet()) {
								for (CommonVO commonVO : d) {
									Integer indexCol = lbs.get(lblData);
									String lbl = commonVO.getName();

									if (lblData.equalsIgnoreCase(lbl)) {
										String url = commonVO.getName2();
										String val = commonVO.getName1();

										XSSFCell cellTambahan = rowTambahan.createCell(indexCol);
										cellTambahan.setCellValue(val);
										if (url != null && !url.trim().isEmpty()) {
											cellTambahan.setCellStyle(hlink_style);
											XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
													.createHyperlink(Hyperlink.LINK_URL);
											link.setAddress(url);
											cellTambahan.setHyperlink(link);
										}
										break;
									}
								}
							}
							d = null;
						}
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
			};

			List<String> columnHeadersAddingTambahan = new ArrayList<String>();
			columnHeadersAddingTambahan.add("ID");
			columnHeadersAddingTambahan.add("No. Reg");
			columnHeadersAddingTambahan.add("No. Ujian");
			columnHeadersAddingTambahan.add("Nama");

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(BiodataCalonMahasiswa.class, this,
					"Download", "/img/excel.png", columnHeadersAdding, dataAdding, true, columnHeadersAddingTambahan,
					contents);
			Common.appendKeToolbar(cetakToolbarbutton, find, comp);

			MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
					"/img/excel.png");
			upload.setUpload(Common.ukuranFileUpload());
			upload.addEventListener("onUpload", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					UploadEvent uploadEvent = (UploadEvent) event;
					Media media = uploadEvent.getMedia();
					if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
						return;
					if (media.getName().toLowerCase().endsWith("xlsx")) {

						InputStream inputStream = null;
						FileOutputStream fileOutputStream = null;
						final File file = new File(
								Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
						try {
							inputStream = media.getStreamData();
							file.getParentFile().mkdirs();
							fileOutputStream = new FileOutputStream(file);
							IOUtils.copyLarge(inputStream, fileOutputStream);
						} finally {
							if (fileOutputStream != null) {
								try {
									fileOutputStream.close();
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							}
							if (inputStream != null) {
								try {
									inputStream.close();
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							}
						}

						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								CommonPMB.uploadDataCalonMahasiswa(file, new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										onSearchDefault(arg0);
										Clients.clearBusy();
									}
								}, contents);
							}
						}, "Harap tunggu.. sedang melakukan proses upload data..");

					} else {
						MyMessageboxConfig.show(
								"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
										+ media,
								"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
					}
				}
			});
			if (Common.bolehKonfigurasi("aktifkan_tombol_upload_data_calon_mahasiswa_baru", Konfigurasi.TIDAK_AKTIF)
					&& Common.bolehUploadDataKonfigurasi("hak_akses_upload_data_registrasi")) {
				Common.appendKeToolbar(upload, find, comp);
			}

			{
				final org.zkoss.zk.ui.Component rootCompTA = comp;
				MyToolbarbuttonConfig btnUbahTA = new MyToolbarbuttonConfig("Ubah Default TA", "/img/svg/calendar3.svg");
				btnUbahTA.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						final org.zkoss.zul.Window winTA = new org.zkoss.zul.Window();
						winTA.setTitle("Ubah Default Tahun Akademik PMB");
						winTA.setClosable(true);
						winTA.setWidth("380px");
						winTA.setBorder("normal");
						winTA.setParent(rootCompTA);

						Vbox vlTA = new Vbox();
						vlTA.setSpacing("6px");
						vlTA.setStyle("padding:12px;");
						vlTA.setParent(winTA);

						new Label(ais.common.Common.getBahasaConfig("Pilih Tahun Akademik:")).setParent(vlTA);

						final Combobox cboTaPopup = Common.generateTahunAjaran(null);
						cboTaPopup.setReadonly(true);
						cboTaPopup.setWidth("100%");
						Common.selectComboItem(cboTaPopup, tahunAkademikPenerimaanMahasiswaBaru);
						cboTaPopup.setParent(vlTA);

						Hbox hbBtnTA = new Hbox();
						hbBtnTA.setStyle("margin-top:8px;");
						hbBtnTA.setParent(vlTA);

						MyToolbarbuttonConfig btnSimpanTA = new MyToolbarbuttonConfig("Simpan");
						btnSimpanTA.setParent(hbBtnTA);
						btnSimpanTA.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								if (cboTaPopup.getSelectedItem() == null) {
									// Lewat MyMessageboxConfig, bukan Clients.alert: agar ikut memperoleh
									// dialog baku (ringkasan bermakna, Detail teknis, dan tombol Ubah Teks
									// bagi administrator) seperti alert lain di aplikasi ini.
									MyMessageboxConfig.show("Pilih tahun akademik terlebih dahulu.");
									return;
								}
								String newTA = cboTaPopup.getSelectedItem().getLabel();
								Konfigurasi cfg = Common.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", newTA);
								cfg.setNilai(newTA);
								Session sSaveTA = HibernateUtil.openSession();
								try {
									sSaveTA.beginTransaction();
									sSaveTA.saveOrUpdate(cfg);
									sSaveTA.getTransaction().commit();
									ais.common.MemoryDbUtil.getKonfigurasi().put(cfg.getNama(), cfg);
								} catch (Exception eSaveTA) {
									if (sSaveTA.getTransaction() != null && sSaveTA.getTransaction().isActive()) {
										try { sSaveTA.getTransaction().rollback(); } catch (Exception eRb) { ais.common.ErrorAuditUtil.record(eRb, "auto-audit(empty-catch) src/ais/action/master/pmb/CetakRegistrasiAction.java:1753");}
									}
									throw eSaveTA;
								} finally {
									try { sSaveTA.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/CetakRegistrasiAction.java:1757");}
									try { sSaveTA.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/CetakRegistrasiAction.java:1758");}
								}
								tahunAkademikPenerimaanMahasiswaBaru = newTA;
								Common.selectComboItem(searchTahunAjaran, newTA);
								winTA.detach();
								org.zkoss.zk.ui.event.Events.sendEvent(searchTahunAjaran,
										new org.zkoss.zk.ui.event.Event("onChange", searchTahunAjaran, null));
							}
						});

						MyToolbarbuttonConfig btnBatalTA = new MyToolbarbuttonConfig("Batal");
						btnBatalTA.setParent(hbBtnTA);
						btnBatalTA.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								winTA.detach();
							}
						});

						winTA.doModal();
					}
				});
				Common.appendKeToolbar(btnUbahTA, find, comp);
			}

			cetakToolbarbutton = Common.cetakDataCustomButton(BiodataCalonMahasiswa.class, this, "Download Gen. NIM",
					"/img/excel.png",
					new String[] { "id", "noRegistrasi", "noUjian", "nim", "nama", "prodiLulus", "generateNimOtomatis",
							"prodi1", "prodi2", "prodi3", "prodi4", "prodi5", "pembayaranRegistrasi",
							"pembayaranDaftarUlang" });
			Common.appendKeToolbar(cetakToolbarbutton, find, comp);

			upload = new MyToolbarbuttonConfig("Upload Gen. NIM", "/img/excel.png");
			upload.setUpload(Common.ukuranFileUpload());
			upload.addEventListener("onUpload", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					UploadEvent uploadEvent = (UploadEvent) event;
					Media media = uploadEvent.getMedia();
					if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
						return;
					if (media.getName().toLowerCase().endsWith("xlsx")) {

						InputStream inputStream = null;
						FileOutputStream fileOutputStream = null;
						final File file = new File(
								Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
						try {
							inputStream = media.getStreamData();
							file.getParentFile().mkdirs();
							fileOutputStream = new FileOutputStream(file);
							IOUtils.copyLarge(inputStream, fileOutputStream);
						} finally {
							if (fileOutputStream != null) {
								try {
									fileOutputStream.close();
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							}
							if (inputStream != null) {
								try {
									inputStream.close();
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							}
						}

						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								CommonPMB.uploadKelulusan(file, new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										onSearchDefault(arg0);
										Clients.clearBusy();
									}
								});
							}
						}, "Harap tunggu.. sedang melakukan proses upload data..");

					} else {
						MyMessageboxConfig.show(
								"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
										+ media,
								"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
					}
				}
			});

			upload.setVisible(
					Common.bolehKonfigurasi("aktifkan_tombol_upload_gen_nim_data_calon_mahasiswa"));

			Common.appendKeToolbar(upload, find, comp);

			MyToolbarbuttonConfig downloadLampiran = new MyToolbarbuttonConfig("Lampiran", "/img/attachment-icon.png");
			downloadLampiran.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					onDownloadLampiran(arg0);
				}
			});
			Common.appendKeToolbar(downloadLampiran, find, comp);

			MyToolbarbuttonConfig singkronDenganMhs = new MyToolbarbuttonConfig("Singkron dg mhs",
					"/img/svg/check2.svg");
			singkronDenganMhs.setVisible(Common.bolehKonfigurasi("tampilkan_tombol_singkronkan_calon_dengan_mahasiswa", Konfigurasi.TIDAK_AKTIF));
			singkronDenganMhs.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(final Event arg0) throws Exception {
							String tahun = searchTahunAjaran.getSelectedItem().getValue().toString().split("/")[0];

							StringBuilder sql1 = new StringBuilder();
							sql1.append("update biodata_calon_mahasiswa aa set mahasiswa=\r\n").append(
									" (select max(bb.id) from mahasiswa bb where bb.tahunangkatan=aa.tahun and bb.nama=aa.nama and date(bb.tanggallahir)=date(aa.tanggal_lahir) \r\n")
									.append(" and tahunangkatan=").append(tahun)
									.append(" and bb.jurusan=aa.prodi_lulus) where aa.tahun=").append(tahun)
									.append(" and aa.prodi_lulus is not null;");

							StringBuilder sql2 = new StringBuilder();
							sql2.append(
									"update biodata_calon_mahasiswa set mahasiswa = null where pembayaran_daftar_ulang is null and tahun=")
									.append(tahun).append(" and prodi_lulus is null;");

							StringBuilder sql3 = new StringBuilder();
							sql3.append(
									"update biodata_calon_mahasiswa aa set mahasiswa = (select id from mahasiswa where biodata_calon_mahasiswa_long=aa.id) where mahasiswa is null;");

							// Bulk Update SQL sudah paling efisien, kita hanya perbaiki keamanan
							// Session-nya
							Session session = null;
							Transaction tx = null;
							try {
								session = HibernateUtil.getSessionFactory().openSession();
								tx = session.beginTransaction();

								session.createSQLQuery(sql1.toString()).executeUpdate();
								session.createSQLQuery(sql2.toString()).executeUpdate();
								session.createSQLQuery(sql3.toString()).executeUpdate();

								tx.commit();
							} catch (Exception e) {
								if (tx != null) {
									try {
										tx.rollback();
									} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/CetakRegistrasiAction.java:1907");
									}
								}
								ais.common.Common.tampilErrorJikaAdmin(e);
							} finally {
								cleanupSession(session);
							}

							// Refresh halaman setelah proses bulk selesai
							Common.createDefaultTimer(new EventListener() {
								@Override
								public void onEvent(Event a) throws Exception {
									onSearchDefault(arg0);
								}
							});
						}
					});
				}
			});
			Common.appendKeToolbar(singkronDenganMhs, find, comp);

			// -----------------------------------------------------------------------------------

			MyToolbarbuttonConfig singkronPemb = new MyToolbarbuttonConfig("Singkron dg pemb.", "/img/svg/check2.svg");
			singkronPemb.setVisible(
					Common.bolehKonfigurasi("tampilkan_tombol_singkronkan_calon_dengan_pembayaran"));
			singkronPemb.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {

					List<Long> dataIds = initCriteria(true).setProjection(Projections.property("id")).list();

					// Laporan rinci per calon (berhasil/gagal+penyebab teknis lengkap+langkah
					// mengatasi) - otomatis diunduh sbg berkas teks sesudah proses selesai.
					final LaporanUpload laporan = new LaporanUpload("Sinkronisasi Pembayaran Calon Mahasiswa");

					// Lempar ke Proses Multithreading Paralel 100 Antrean
					eksekusiParalel("Sinkronisasi Pembayaran", dataIds, new ParalelAction() {
						@Override
						public void execute(Long id, Session session) throws Exception {
							// Melempar null pada parameter 'label' karena progress bar sekarang di-handle
							// oleh arsitektur ZK Timer agar tidak memicu error "UiException".
							CetakRegistrasiAction.singkronkanDenganPembayaran(id, null, 0, 0);
						}
					}, laporan);
				}
			});
			Common.appendKeToolbar(singkronPemb, find, comp);

			MyToolbarbuttonConfig singkronNim = new MyToolbarbuttonConfig("Singkron dg NIM.", "/img/svg/check2.svg");
			singkronNim.setVisible(
					Common.bolehKonfigurasi("tampilkan_tombol_singkronkan_calon_dengan_nim"));
			singkronNim.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {

					List<Long> dataIds = initCriteria(true).setProjection(Projections.property("id")).list();
					final LaporanUpload laporan = new LaporanUpload("Sinkronisasi NIM Calon Mahasiswa");

					// Lempar ke Proses Multithreading Paralel 100 Antrean
					eksekusiParalel("Sinkronisasi NIM", dataIds, new ParalelAction() {
						@Override
						public void execute(Long id, Session session) throws Exception {
							// Melempar null pada parameter 'label' karena progress bar sekarang di-handle
							// oleh arsitektur ZK Timer agar tidak memicu error "UiException".
							CetakRegistrasiAction.singkronkanDenganNim(id, null, 0, 0);
						}
					}, laporan);
				}
			});
			Common.appendKeToolbar(singkronNim, find, comp);

			// -----------------------------------------------------------------------------------

			MyToolbarbuttonConfig perbaikiNimGanda = new MyToolbarbuttonConfig(
					"Perbaiki NIM ganda", "/img/svg/check2.svg");
			perbaikiNimGanda.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					// Cari semua NIM yang muncul lebih dari sekali di biodata_calon_mahasiswa
					Session s0 = HibernateUtil.currentNativeSession();
					@SuppressWarnings("unchecked")
					List<String> nimGanda = s0.createSQLQuery(
							"SELECT nim FROM biodata_calon_mahasiswa "
							+ "WHERE nim IS NOT NULL AND trim(nim) <> '' "
							+ "GROUP BY nim HAVING COUNT(*) > 1").list();

					if (nimGanda.isEmpty()) {
						MyMessageboxConfig.show("Tidak ada NIM ganda ditemukan.", "Info",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					// Kumpulkan ID calon mhs yang bukan pemilik sah NIM tersebut
					// (pemilik sah = ada mahasiswa yang NIM-nya cocok dengan kolom nim di biodata)
					final List<Long> idsToClear = new ArrayList<Long>();
					for (String nim : nimGanda) {
						@SuppressWarnings("unchecked")
						List<BiodataCalonMahasiswa> camaList = s0
								.createCriteria(BiodataCalonMahasiswa.class)
								.add(Restrictions.eq("nim", nim)).list();
						for (BiodataCalonMahasiswa cama : camaList) {
							boolean pemilikSah = cama.getMahasiswa() != null
									&& nim.equals(cama.getMahasiswa().getNim());
							if (!pemilikSah) {
								idsToClear.add(cama.getId());
							}
						}
					}

					final LaporanUpload laporan = new LaporanUpload(
							"Perbaiki NIM Ganda: " + nimGanda.size() + " NIM ganda, "
							+ idsToClear.size() + " data akan dibersihkan");

					eksekusiParalel("Perbaiki NIM Ganda", idsToClear, new ParalelAction() {
						@Override
						public void execute(Long id, Session session) throws Exception {
							// Hapus NIM duplikat agar dapat di-generate ulang oleh admin
							Transaction tx = session.beginTransaction();
							session.createSQLQuery(
									"UPDATE biodata_calon_mahasiswa SET nim = NULL WHERE id = :id")
									.setParameter("id", id).executeUpdate();
							tx.commit();
						}
					}, laporan);
				}
			});
			Common.appendKeToolbar(perbaikiNimGanda, find, comp);

			// -----------------------------------------------------------------------------------

			MyToolbarbuttonConfig analisisNimDouble = new MyToolbarbuttonConfig(
					"Analisis NIM Double", "/img/svg/search_menu.svg");
			analisisNimDouble.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session s0 = HibernateUtil.currentNativeSession();
					@SuppressWarnings("unchecked")
					List<String> nimGandaList = s0.createSQLQuery(
							"SELECT nim FROM biodata_calon_mahasiswa "
							+ "WHERE nim IS NOT NULL AND trim(nim) <> '' "
							+ "GROUP BY nim HAVING COUNT(*) > 1 ORDER BY nim").list();

					final org.zkoss.zul.Window win = new org.zkoss.zul.Window();
					win.setTitle("Analisis NIM Ganda");
					win.setClosable(true);
					win.setBorder("normal");
					win.setWidth("820px");

					Vbox vbMain = new Vbox();
					vbMain.setWidth("100%");
					vbMain.setParent(win);

					if (nimGandaList.isEmpty()) {
						new org.zkoss.zul.Html(
								"<div style='padding:24px;text-align:center;color:#198754;"
								+ "font-size:14px;font-weight:bold'>"
								+ "&#10003;&nbsp; Tidak ada NIM ganda &mdash; data bersih.</div>")
								.setParent(vbMain);
					} else {
						new org.zkoss.zul.Html(
								"<div style='margin:10px 14px 6px;padding:10px 14px;"
								+ "background:#fff5f5;border-left:4px solid #dc3545;"
								+ "border-radius:0 6px 6px 0'><b style='color:#dc3545'>"
								+ "&#9888;&nbsp;" + nimGandaList.size()
								+ " NIM ganda ditemukan</b>"
								+ "<span style='color:#664d03;font-size:11px;margin-left:12px'>"
								+ "ID terendah = Pemilik Utama &bull; ID lebih tinggi = Konflik</span>"
								+ "</div>").setParent(vbMain);

						org.zkoss.zul.Div divScroll = new org.zkoss.zul.Div();
						divScroll.setStyle("max-height:460px;overflow-y:auto;padding:0 14px 10px;");
						divScroll.setParent(vbMain);

						for (final String nimItem : nimGandaList) {
							@SuppressWarnings("unchecked")
							List<BiodataCalonMahasiswa> camaList = s0
									.createCriteria(BiodataCalonMahasiswa.class)
									.add(Restrictions.eq("nim", nimItem))
									.addOrder(Order.asc("id")).list();

							// Kartu per NIM
							org.zkoss.zul.Div divCard = new org.zkoss.zul.Div();
							divCard.setStyle("margin-bottom:12px;border:1px solid #dee2e6;"
									+ "border-radius:6px;overflow:hidden;");
							divCard.setParent(divScroll);

							new org.zkoss.zul.Html(
									"<div style='background:#f8f9fa;padding:8px 12px;"
									+ "font-weight:bold;border-bottom:1px solid #dee2e6;"
									+ "font-family:Arial,sans-serif;font-size:12px'>"
									+ "NIM: <span style='color:#0d6efd'>" + nimItem + "</span>"
									+ "&nbsp;<span style='font-weight:normal;color:#6c757d'>["
									+ camaList.size() + " data]</span></div>")
									.setParent(divCard);

							org.zkoss.zul.Grid grd = new org.zkoss.zul.Grid();
							grd.setStyle("border:none;");
							grd.setParent(divCard);

							org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
							cols.setParent(grd);
							org.zkoss.zul.Column cNama = new org.zkoss.zul.Column("Nama");
							cNama.setWidth("200px"); cNama.setParent(cols);
							org.zkoss.zul.Column cReg = new org.zkoss.zul.Column("No. Registrasi");
							cReg.setWidth("145px"); cReg.setParent(cols);
							org.zkoss.zul.Column cProdi = new org.zkoss.zul.Column("Prodi Diterima");
							cProdi.setParent(cols);
							org.zkoss.zul.Column cStatus = new org.zkoss.zul.Column("Status");
							cStatus.setWidth("90px"); cStatus.setParent(cols);
							org.zkoss.zul.Column cAksi = new org.zkoss.zul.Column("");
							cAksi.setWidth("200px"); cAksi.setParent(cols);

							Rows rows = new Rows();
							rows.setParent(grd);

							for (int ri = 0; ri < camaList.size(); ri++) {
								final BiodataCalonMahasiswa cama = camaList.get(ri);
								boolean isPemilik = (ri == 0); // ID terendah = pemilik utama

								Row row = new Row();
								if (!isPemilik) row.setStyle("background:#fff5f5;");
								row.setParent(rows);

								new Label(cama.getNama() != null ? cama.getNama() : "-")
										.setParent(row);
								new Label(cama.getNoRegistrasi() != null
										? cama.getNoRegistrasi() : "-").setParent(row);
								new Label(cama.getProdiLulus() != null
										? cama.getProdiLulus().getNama() : "-").setParent(row);

								if (isPemilik) {
									new org.zkoss.zul.Html(
											"<span style='color:#198754;font-size:11px'>"
											+ "&#10003; Pemilik</span>").setParent(row);
									new Label("").setParent(row);
								} else {
									new org.zkoss.zul.Html(
											"<span style='color:#dc3545;font-size:11px'>"
											+ "&#9888; Konflik</span>").setParent(row);

									final Long conflictId = cama.getId();
									final String namaCama = cama.getNama() != null
											? cama.getNama() : "-";
									final String noRegCama = cama.getNoRegistrasi() != null
											? cama.getNoRegistrasi() : "-";

									Hbox hbAksi = new Hbox();
									hbAksi.setSpacing("4px");
									hbAksi.setParent(row);

									// Tombol Generate Ulang NIM
									MyToolbarbuttonConfig btnGen = new MyToolbarbuttonConfig(
											"Generate Ulang", "/img/svg/refresh.svg");
									btnGen.setParent(hbAksi);
									btnGen.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event e) throws Exception {
											win.detach();
											final List<Long> ids = new ArrayList<Long>();
											ids.add(conflictId);
											final LaporanUpload laporan = new LaporanUpload(
													"Generate Ulang NIM: " + namaCama
													+ " [" + conflictId + "]");
											eksekusiParalel("Generate Ulang NIM", ids,
													new ParalelAction() {
												@Override
												public void execute(Long id, Session session)
														throws Exception {
													BiodataCalonMahasiswa cf =
															(BiodataCalonMahasiswa) session.get(
															BiodataCalonMahasiswa.class, id);
													if (cf == null) return;
													Long mahId = cf.getMahasiswa() != null
															? cf.getMahasiswa().getId() : null;
													// DefaultNimGenerator memakai & menutup
													// currentNativeSession (thread-local terpisah)
													String newNim = new ais.action.master.pmb.nim
															.DefaultNimGenerator().generateNim(cf);
													Transaction tx = session.beginTransaction();
													session.createSQLQuery(
															"UPDATE biodata_calon_mahasiswa"
															+ " SET nim = :nim WHERE id = :id")
															.setParameter("nim", newNim)
															.setParameter("id", id)
															.executeUpdate();
													if (mahId != null)
														session.createSQLQuery(
																"UPDATE mahasiswa SET nim = :nim"
																+ " WHERE id = :mid")
																.setParameter("nim", newNim)
																.setParameter("mid", mahId)
																.executeUpdate();
													tx.commit();
												}
											}, laporan);
										}
									});

									// Tombol Edit Manual NIM
									MyToolbarbuttonConfig btnEdit = new MyToolbarbuttonConfig(
											"Edit NIM", "/img/svg/pencil-square.svg");
									btnEdit.setParent(hbAksi);
									btnEdit.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event e) throws Exception {
											final org.zkoss.zul.Window winEdit =
													new org.zkoss.zul.Window();
											winEdit.setTitle("Edit Manual NIM");
											winEdit.setClosable(true);
											winEdit.setBorder("normal");
											winEdit.setWidth("400px");

											Vbox vbEdit = new Vbox();
											vbEdit.setStyle("padding:14px;");
											vbEdit.setParent(winEdit);

											new org.zkoss.zul.Html(
													"<div style='margin-bottom:10px;padding:8px 12px;"
													+ "background:#fff3cd;border-left:3px solid #ffc107;"
													+ "border-radius:0 4px 4px 0;font-size:12px'>"
													+ "NIM saat ini: <b>" + nimItem + "</b><br>"
													+ "Mahasiswa: <b>" + namaCama + "</b>"
													+ " (" + noRegCama + ")"
													+ "</div>").setParent(vbEdit);

											new Label("NIM Baru:").setParent(vbEdit);
											final Textbox txtNim = new Textbox();
											txtNim.setWidth("100%");
											txtNim.setParent(vbEdit);

											Hbox hbEditBtn = new Hbox();
											hbEditBtn.setStyle("margin-top:10px;");
											hbEditBtn.setParent(vbEdit);

											MyToolbarbuttonConfig btnSimpan =
													new MyToolbarbuttonConfig("Simpan",
													"/img/svg/check2.svg");
											btnSimpan.setParent(hbEditBtn);
											btnSimpan.addEventListener("onClick",
													new EventListener() {
												@Override
												public void onEvent(Event ev) throws Exception {
													String nimBaru = txtNim.getValue() != null
															? txtNim.getValue().trim() : "";
													if (nimBaru.isEmpty()) {
														MyMessageboxConfig.show(
																"NIM baru tidak boleh kosong.",
																"Peringatan",
																MyMessageboxConfig.OK,
																MyMessageboxConfig.EXCLAMATION);
														return;
													}
													Session sSave = HibernateUtil.openSession();
													try {
														sSave.beginTransaction();
														sSave.createSQLQuery(
																"UPDATE biodata_calon_mahasiswa"
																+ " SET nim = :nim WHERE id = :id")
																.setParameter("nim", nimBaru)
																.setParameter("id", conflictId)
																.executeUpdate();
														// Update mahasiswa yang NIM-nya = nimItem
														// dan terhubung ke calon ini saja
														sSave.createSQLQuery(
																"UPDATE mahasiswa SET nim = :nim"
																+ " WHERE nim = :old AND id = ("
																+ "SELECT mahasiswa_id"
																+ " FROM biodata_calon_mahasiswa"
																+ " WHERE id = :bid)")
																.setParameter("nim", nimBaru)
																.setParameter("old", nimItem)
																.setParameter("bid", conflictId)
																.executeUpdate();
														sSave.getTransaction().commit();
													} catch (Exception exSv) {
														if (sSave.getTransaction() != null
																&& sSave.getTransaction().isActive())
															try { sSave.getTransaction().rollback();
															} catch (Exception rb) {
																ais.common.ErrorAuditUtil.record(rb,
																"CetakRegistrasiAction editNIM rollback");
															}
														throw exSv;
													} finally {
														try { sSave.close();
														} catch (Exception exc) {
															ais.common.ErrorAuditUtil.record(exc,
															"CetakRegistrasiAction editNIM close");
														}
													}
													winEdit.detach();
													win.detach();
													MyMessageboxConfig.show(
															"NIM berhasil diubah menjadi: "
															+ nimBaru, "Berhasil",
															MyMessageboxConfig.OK,
															MyMessageboxConfig.INFORMATION);
												}
											});

											MyToolbarbuttonConfig btnBatal =
													new MyToolbarbuttonConfig("Batal");
											btnBatal.setParent(hbEditBtn);
											btnBatal.addEventListener("onClick",
													new EventListener() {
												@Override
												public void onEvent(Event ev) throws Exception {
													winEdit.detach();
												}
											});

											winEdit.setParent(win.getPage().getFirstRoot());
											winEdit.doModal();
										}
									});
								}
							}
						}
					}

					// Footer
					Hbox hbFooter = new Hbox();
					hbFooter.setStyle("padding:10px 16px;border-top:1px solid #dee2e6;");
					hbFooter.setParent(vbMain);

					MyToolbarbuttonConfig btnTutup = new MyToolbarbuttonConfig("Tutup");
					btnTutup.setParent(hbFooter);
					btnTutup.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event e) throws Exception {
							win.detach();
						}
					});

					win.setParent(arg0.getTarget().getPage().getFirstRoot());
					win.doHighlighted();
				}
			});
			Common.appendKeToolbar(analisisNimDouble, find, comp);

			// -----------------------------------------------------------------------------------

			MyToolbarbuttonConfig analisisUrutanNim = new MyToolbarbuttonConfig(
					"Analisis Urutan NIM", "/img/svg/search_menu.svg");
			analisisUrutanNim.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					tampilkanAnalisisUrutanNim(arg0);
				}
			});
			Common.appendKeToolbar(analisisUrutanNim, find, comp);

			// -----------------------------------------------------------------------------------

			MyToolbarbuttonConfig singkronDiskon = new MyToolbarbuttonConfig("Singkron dg diskon",
					"/img/svg/check2.svg");
			singkronDiskon.setVisible(
					Common.bolehKonfigurasi("tampilkan_tombol_singkronkan_calon_dengan_diskon"));
			singkronDiskon.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {

					List<Long> dataIds = initCriteria(true).setProjection(Projections.property("id")).list();
					final LaporanUpload laporan = new LaporanUpload("Sinkronisasi Diskon Calon Mahasiswa");

					// Lempar ke Proses Multithreading Paralel 100 Antrean
					eksekusiParalel("Sinkronisasi Diskon Maba", dataIds, new ParalelAction() {
						@Override
						public void execute(Long id, Session session) throws Exception {
							// Ambil data menggunakan session yang terisolasi dari dalam Thread
							BiodataCalonMahasiswa bio = (BiodataCalonMahasiswa) session.get(BiodataCalonMahasiswa.class,
									id);
							if (bio != null) {
								DiskonCalonMahasiswaAction.ambilDiskon(bio);
							}
						}
					}, laporan);
				}
			});
			Common.appendKeToolbar(singkronDiskon, find, comp);
		}

		Common.initLaguage();

		MyToolbarbuttonConfig downloadFoto = new MyToolbarbuttonConfig("Download Foto", "/img/attachment-icon.png");
		downloadFoto.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onDownloadFoto(arg0);
			}
		});
		Common.appendKeToolbar(downloadFoto, find, comp);

		Session sessionInit = null;
		try {
			sessionInit = HibernateUtil.getSessionFactory().openSession();
			List<VerifikasiKelengkapanCalonMahasiswa> verifikasiKelengkapanCalonMahasiswas = sessionInit
					.createCriteria(VerifikasiKelengkapanCalonMahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.asc("nama")).list();

			Comboitem semua = new Comboitem("= Semua =");
			searchSyarat.appendChild(semua);

			for (VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa : verifikasiKelengkapanCalonMahasiswas) {

				Comboitem telah = new Comboitem("Upload \"" + verifikasiKelengkapanCalonMahasiswa.getNama() + " \"");
				telah.setAttribute("nilai", verifikasiKelengkapanCalonMahasiswa);
				telah.setValue(1);
				searchSyarat.appendChild(telah);

				Comboitem belum = new Comboitem(
						"Belum Upload \"" + verifikasiKelengkapanCalonMahasiswa.getNama() + " \"");
				belum.setAttribute("nilai", verifikasiKelengkapanCalonMahasiswa);
				belum.setValue(2);
				searchSyarat.appendChild(belum);

				Comboitem verify = new Comboitem(
						"Verifikasi \"" + verifikasiKelengkapanCalonMahasiswa.getNama() + " \"");
				verify.setAttribute("nilai", verifikasiKelengkapanCalonMahasiswa);
				verify.setValue(3);
				searchSyarat.appendChild(verify);

				Comboitem belumVerifikasi = new Comboitem(
						"Belum Verifikasi \"" + verifikasiKelengkapanCalonMahasiswa.getNama() + " \"");
				belumVerifikasi.setAttribute("nilai", verifikasiKelengkapanCalonMahasiswa);
				belumVerifikasi.setValue(4);
				searchSyarat.appendChild(belumVerifikasi);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (sessionInit != null && sessionInit.isOpen()) {
				try {
					sessionInit.close();
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}

		if (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null) {
			Comboitem telah = new Comboitem(
					"Upload \"" + ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getNamaKegiatan() + " \"");
			telah.setAttribute("nilai", ConstantValues.PENDAFTARAN_CALON_MAHASISWA);
			telah.setValue(1);
			searchSyarat.appendChild(telah);
		}
		if (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null) {
			Comboitem telah = new Comboitem(
					"Upload \"" + ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getNamaKegiatan() + " \"");
			telah.setAttribute("nilai", ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
			telah.setValue(1);
			searchSyarat.appendChild(telah);
		}
		Comboitem semua = new Comboitem("= Semua =");
		searchSyarat.appendChild(semua);
		if (searchSyarat != null) { searchSyarat.setSelectedItem(semua); }
		if (searchSyarat != null) { searchSyarat.setReadonly(true); }
		searchSyarat.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		cariBlmMasukFeeder = new MyCheckboxConfig("Blm terkirim");

		if (integrasi_pmb_arkatama = Common.bolehKonfigurasi("integrasi_pmb_arkatama", Konfigurasi.TIDAK_AKTIF)) {
			MyToolbarbuttonConfig singkronDenganMhs = new MyToolbarbuttonConfig("Krm ke Feeder PMB",
					"/img/svg/check2.svg");
			singkronDenganMhs.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					final List<String> hasils = new ArrayList<String>();
					final Label label = Common.displayLoadBar(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							StringBuilder h = new StringBuilder();
							for (String s : hasils) {
								if (h.length() > 0)
									h.append("\n");
								h.append(s);
							}
							MyMessageboxConfig.show("Hasil :\n" + h.toString(), "Hasil", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
						}
					});

					new Thread(new Runnable() {
						@Override
						public void run() {
							List<Long> longs = initCriteria(true).list();
							int size = longs.size();
							int index = 0;
							for (Long biodataCalonMahasiswaid : longs) {
								BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
										.ambil(BiodataCalonMahasiswa.class.getName(), biodataCalonMahasiswaid);
								index++;

								Session session = null;
								Transaction tx = null;
								try {
									label.setValue("Kirimkan ke Feeder PMB " + biodataCalonMahasiswa.getNoRegistrasi()
											+ "-" + biodataCalonMahasiswa.getNama() + " ("
											+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

									PmbArkatama.doPost(biodataCalonMahasiswa, hasils);

									session = HibernateUtil.getSessionFactory().openSession();
									tx = session.beginTransaction();
									session.update(biodataCalonMahasiswa);
									tx.commit();
								} catch (Exception e) {
									if (tx != null && tx.isActive()) {
										try {
											tx.rollback();
										} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/CetakRegistrasiAction.java:2140");
										}
									}
									ais.common.Common.tampilErrorJikaAdmin(e);
								} finally {
									if (session != null && session.isOpen()) {
										try {
											session.close();
										} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/CetakRegistrasiAction.java:2148");
										}
									}
								}
							}
							longs.clear();
							label.setValue("");
						}
					}).start();
				}
			});
			Common.appendKeToolbar(singkronDenganMhs, find, comp);
			Common.appendKeToolbar(cariBlmMasukFeeder, find, comp);

			cariBlmMasukFeeder.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});
		}

		MyButtonTabbox.gantiTabboxNative(tabboxDataCalonMahasiswa, tabAktifDataCalonMahasiswa);
		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		if (button != null) { button.setDisabled(!edit); }
		if (button != null) { button.setVisible(edit); }
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				RevisiBiodataCalonMahasiswaHelper revisiHelper = new RevisiBiodataCalonMahasiswaHelper(
						new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										onSearchDefault(arg0);
									}
								});
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();
			}
		});
		if (button != null) { button.setParent(find.getParent()); }

		FilterLanjutHelper.setup(comp, 2);
	}

	/**
	 * Antarmuka kustom untuk eksekusi aksi di dalam Thread.
	 */
	public interface ParalelAction {
		void execute(Long id, Session session) throws Exception;
	}

	/**
	 * Memproses List ID menggunakan 100 Executor Thread secara paralel. Indikator
	 * dijamin update secara asinkron tanpa memblokir layar ZK.
	 *
	 * <p>Kompatibilitas: overload lama TANPA {@link LaporanUpload} — perilaku PERSIS seperti
	 * sebelumnya (error per item ditelan diam-diam, cuma dicatat ke ErrorAuditUtil, popup akhir
	 * generik "... telah selesai 100%."). Dipakai pemanggil yang belum butuh laporan rinci.</p>
	 */
	private void eksekusiParalel(final String namaProses, final List<Long> dataIds, final ParalelAction action) {
		eksekusiParalel(namaProses, dataIds, action, null);
	}

	/**
	 * Varian {@link #eksekusiParalel(String, List, ParalelAction)} yang MENCATAT hasil TIAP
	 * item (berhasil/gagal) ke {@code laporan} — dipakai proses yang perlu memberi tahu admin
	 * PERSIS mana yang berhasil/gagal disinkron beserta penyebab TEKNIS rinci (lihat
	 * {@link LaporanUpload#catatGagalDetail(int, String, Throwable)}), bukan cuma popup
	 * generik "selesai 100%" yang menyembunyikan kegagalan per baris.
	 *
	 * <p>"kunci" per baris pada laporan = NIM bila sudah ada, jika tidak No. Registrasi, jika
	 * tidak nama calon mahasiswa — supaya admin bisa langsung kenali baris mana di layar.</p>
	 *
	 * @param laporan boleh null (perilaku sama seperti overload tanpa laporan); bila diisi,
	 *                WAJIB dipanggil {@code laporan.selesaikan(...)} oleh pemanggil setelah
	 *                proses ini betul-betul selesai (lihat listener Timer di bawah).
	 */
	private void eksekusiParalel(final String namaProses, final List<Long> dataIds, final ParalelAction action,
			final LaporanUpload laporan) {
		if (dataIds == null || dataIds.isEmpty())
			return;

		final int totalData = dataIds.size();
		final AtomicInteger dataTerproses = new AtomicInteger(0);

		// Gunakan AtomicReference agar variabel dapat diubah & dibaca lintas-Thread
		// dengan aman (Thread-Safe)
		final AtomicReference<String> statusMessage = new AtomicReference<String>("Menyiapkan " + namaProses + "...");

		// Hitung paralelisme AMAN di thread request (yang punya session), lalu dipakai oleh
		// thread latar. Membatasi jumlah koneksi DB simultan agar pool c3p0 tidak habis -
		// penyebab Tomcat tidak bisa diakses (semua thread AJP/HTTP ikut menunggu koneksi).
		// safe(dataIds.size()) = sejumlah data tapi tak melebihi plafon aman (config).
		final int parallelThreads = ais.common.DbThreadPool.safe(dataIds.size());
		final AtomicInteger nomorBarisLaporan = new AtomicInteger(0);

		new Thread(new Runnable() {
			@Override
			public void run() {
				// Batasi eksekusi paralel (kecil) agar CPU server & koneksi DB stabil.
				// SEBELUMNYA: Executors.newFixedThreadPool(100) -> membuka s/d 100 Session/
				// koneksi DB SEKALIGUS untuk SATU proses -> c3p0 pool habis -> seluruh thread
				// AJP/HTTP ikut menunggu (BasicResourcePool.checkoutResource) -> aplikasi hang.
				ExecutorService executor = Executors.newFixedThreadPool(parallelThreads);

				try {
				for (final Long id : dataIds) {
					executor.submit(new Runnable() {
						@Override
						public void run() {
							Session session = null;
							// "kunci" (NIM/No.Registrasi/Nama) utk laporan — diambil SEBELUM
							// action.execute() supaya tetap tersedia walau action-nya gagal.
							String kunci = "ID:" + id;
							int nomorBaris = nomorBarisLaporan.getAndIncrement();
							try {
								// Buka session terisolasi spesifik untuk Thread ini
								session = HibernateUtil.getSessionFactory().openSession();

								if (laporan != null) {
									try {
										BiodataCalonMahasiswa bcm = (BiodataCalonMahasiswa) session
												.get(BiodataCalonMahasiswa.class, id);
										if (bcm != null) {
											kunci = bcm.getNim() != null && !bcm.getNim().trim().isEmpty() ? bcm.getNim()
													: bcm.getNoRegistrasi() != null && !bcm.getNoRegistrasi().trim().isEmpty()
															? bcm.getNoRegistrasi()
															: (bcm.getNama() != null ? bcm.getNama() : kunci);
										}
									} catch (Exception eKunci) { ais.common.ErrorAuditUtil.record(eKunci, "auto-audit(empty-catch) src/ais/action/master/pmb/CetakRegistrasiAction.java:eksekusiParalelKunci");
										// kunci tetap "ID:<id>" bila gagal diambil - jangan sampai gagalkan proses utama
									}
								}

								action.execute(id, session);

								if (laporan != null) {
									laporan.catatBerhasil(nomorBaris, kunci, "Sinkronisasi berhasil");
								}

							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/CetakRegistrasiAction.java:2256");
								// Abaikan agar jika 1 error, yang lain tetap berlanjut
								if (laporan != null) {
									laporan.catatGagalDetail(nomorBaris, kunci, e);
								}
							} finally {
								cleanupSession(session);
							}

							// Update nilai progres bar otomatis
							int current = dataTerproses.incrementAndGet();
							double persentase = (current * 100.0) / totalData;
							statusMessage.set(namaProses + " | " + current + " dari " + totalData + " data ("
									+ Common.numberFormat.get().format(persentase) + "%) diproses...");
						}
					});
				}
				} finally {
					// Selalu tutup executor walau perulangan submit gagal (cegah thread bocor)
					executor.shutdown();
					try {
						// Tunggu sampai seluruh antrean selesai
						executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						// Bantu Garbage Collector membebaskan memori
						dataIds.clear();
					}
				}

				// Beri tanda bahwa proses telah selesai 100%
				statusMessage.set("");
			}
		}).start();

		// ZK TIMER: Timer ini bertugas membaca progres dari Thread Background
		// lalu menampilkannya ke layar UI dengan cara yang sangat aman (Anti UI
		// Exception).
		final Timer timer = new Timer(500);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				String currentStatus = statusMessage.get();

				if (currentStatus == null || currentStatus.isEmpty()) {
					Clients.clearBusy();
					timer.detach();

					if (laporan != null) {
						// Laporan rinci per baris (berhasil/gagal+penyebab teknis+langkah mengatasi)
						// otomatis diunduh sebagai berkas teks, lalu refresh grid setelah OK.
						laporan.selesaikan(new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								onSearchDefault(null);
							}
						});
					} else {
						MyMessageboxConfig.show(namaProses + " telah selesai 100%.", "Pemberitahuan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

						// Refresh otomatis grid pencarian setelah sukses
						onSearchDefault(null);
					}
				} else {
					Clients.showBusy(currentStatus); // Muncul layar abu-abu: Sinkronisasi Pembayaran | 25 dari 100
														// (25%) diproses...
				}
			}
		});
		timer.start();
	}

	/**
	 * Pembersihan session paling ketat untuk mencegah kebocoran (Connection Leak)
	 */
	private void cleanupSession(Session session) {
		if (session != null) {
			try {
				if (session.isOpen())
					session.clear();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			try {
				session.disconnect();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			try {
				if (session.isOpen())
					session.close();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}
	}

	public static void singkronkanDenganNim(Long id, Label label, int index, int size) {
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
					.ambil(BiodataCalonMahasiswa.class.getName(), id);

			JenisKegiatan jenisKegiatan = ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU;
			Kegiatan kegiatanDaftarUlang = KegiatanHelper.checkKegiatanCalonMahasiswa(jenisKegiatan,
					biodataCalonMahasiswa, 1, biodataCalonMahasiswa.getTahunAkademik(), true, false, null, session);
			boolean berhasil = false;
			if (kegiatanDaftarUlang != null && kegiatanDaftarUlang.getId() != null) {
				session.refresh(kegiatanDaftarUlang);
				berhasil = CommonReportHelper.checkGenNim(kegiatanDaftarUlang);
			}

			if (berhasil) {
				tx = session.beginTransaction();
				kegiatanDaftarUlang = KegiatanHelper.checkKegiatanCalonMahasiswa(jenisKegiatan, biodataCalonMahasiswa,
						1, biodataCalonMahasiswa.getTahunAkademik(), true, false, null, session);

				biodataCalonMahasiswa.setPembayaranDaftarUlang(kegiatanDaftarUlang);

				StringBuilder sql2 = new StringBuilder();
				sql2.append("update biodata_calon_mahasiswa set pembayaran_daftar_ulang=")
						.append(kegiatanDaftarUlang == null ? "null" : kegiatanDaftarUlang.getId());
				if (kegiatanDaftarUlang != null && kegiatanDaftarUlang.getTanggalBayarAwal() != null) {
					sql2.append(", tanggalpembayarandaftarulang='")
							.append(Common.databaseDateFormat1.get().format(kegiatanDaftarUlang.getTanggalBayarAwal()))
							.append("'");
				}
				sql2.append(" where id=").append(id);

				session.createSQLQuery(sql2.toString()).executeUpdate();
				tx.commit();
				tx = null;
			}

		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception rollbackError) {
					ais.common.ErrorAuditUtil.record(rollbackError,
							"CetakRegistrasiAction.singkronkanDenganNim rollback");
				}
			}
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void singkronkanDenganPembayaran(Long id, Label label, int index, int size) {
		// KE-9: update native SQL biodata_calon_mahasiswa (akhir method) bisa kena deadlock
		// PostgreSQL bila proses sinkron lain menyentuh baris yang sama bersamaan. PostgreSQL
		// meng-abort SELURUH transaksi saat itu terjadi -- tak bisa retry sebagian, jadi
		// SELURUH method (idempotent: sinkronisasi ulang aman) diulang dgn backoff singkat.
		int maksimalPercobaanDeadlock = 3;
		for (int percobaanDeadlock = 1; percobaanDeadlock <= maksimalPercobaanDeadlock; percobaanDeadlock++) {
		boolean berhasilKomit = false;
		boolean konflikKunciDeadlock = false;
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();

			BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
					.ambil(BiodataCalonMahasiswa.class.getName(), id);

			// Error E: ConstantValues.ambil() dapat mengembalikan null bila baris sudah
			// dihapus/tidak ditemukan (data berubah di antara pengambilan daftar ID awal
			// dan eksekusi task paralel ini) -> NPE di getProdi1() dst. Lewati task ini
			// dengan aman (idempotent, id lain di batch tetap diproses).
			if (biodataCalonMahasiswa == null) {
				if (tx != null && tx.isActive()) {
					try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/CetakRegistrasiAction.java:2410-null-guard"); }
				}
				return;
			}

			if (biodataCalonMahasiswa.getProdi1() != null && !biodataCalonMahasiswa.getProdi1().getAktif()) {
				Jurusan juruanSama = null;
				Map<Long, Jurusan> maps = ConstantValues.ambilBerdasarClass(Jurusan.class);
				for (Jurusan jurusan : maps.values()) {
					if (jurusan.getAktif()
							&& jurusan.getKodeEpsbed().equals(biodataCalonMahasiswa.getProdi1().getKodeEpsbed())
							&& jurusan.getNama().equalsIgnoreCase(biodataCalonMahasiswa.getProdi1().getNama())) {
						juruanSama = jurusan;
						break;
					}
				}

				if (juruanSama != null) {
					biodataCalonMahasiswa.setProdi1(juruanSama);
					String sql1 = "update biodata_calon_mahasiswa set prodi_1=" + (juruanSama.getId()) + " where id="
							+ id;
					session.createSQLQuery(sql1).executeUpdate();
				}
			}

			if (biodataCalonMahasiswa.getProdi2() != null && !biodataCalonMahasiswa.getProdi2().getAktif()) {
				Jurusan juruanSama = null;
				Map<Long, Jurusan> maps = ConstantValues.ambilBerdasarClass(Jurusan.class);
				for (Jurusan jurusan : maps.values()) {
					if (jurusan.getAktif()
							&& jurusan.getKodeEpsbed().equals(biodataCalonMahasiswa.getProdi2().getKodeEpsbed())
							&& jurusan.getNama().equalsIgnoreCase(biodataCalonMahasiswa.getProdi2().getNama())) {
						juruanSama = jurusan;
						break;
					}
				}

				if (juruanSama != null) {
					biodataCalonMahasiswa.setProdi2(juruanSama);
					String sql1 = "update biodata_calon_mahasiswa set prodi_2=" + (juruanSama.getId()) + " where id="
							+ id;
					session.createSQLQuery(sql1).executeUpdate();
				}
			}

			if (biodataCalonMahasiswa.getProdiLulus() != null && !biodataCalonMahasiswa.getProdiLulus().getAktif()) {
				Jurusan juruanSama = null;
				Map<Long, Jurusan> maps = ConstantValues.ambilBerdasarClass(Jurusan.class);
				for (Jurusan jurusan : maps.values()) {
					if (jurusan.getAktif()
							&& jurusan.getKodeEpsbed().equals(biodataCalonMahasiswa.getProdiLulus().getKodeEpsbed())
							&& jurusan.getNama().equalsIgnoreCase(biodataCalonMahasiswa.getProdiLulus().getNama())) {
						juruanSama = jurusan;
						break;
					}
				}

				if (juruanSama != null) {
					biodataCalonMahasiswa.setProdiLulus(juruanSama);
					String sql1 = "update biodata_calon_mahasiswa set prodi_lulus=" + (juruanSama.getId())
							+ " where id=" + id;
					session.createSQLQuery(sql1).executeUpdate();
				}
			}

			if (label != null) {
				label.setValue("Singkronkan pembayaran " + biodataCalonMahasiswa.getNoRegistrasi() + "-"
						+ biodataCalonMahasiswa.getNama() + " ("
						+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");
			}
			biodataCalonMahasiswa.reInitKegiatan(session);
			// Evict BiodataCalonMahasiswa agar tidak memicu auto-flush 1664-kolom
			try { session.evict(biodataCalonMahasiswa); } catch (Exception evEx) { ais.common.ErrorAuditUtil.record(evEx, "auto-audit(empty-catch) src/ais/action/master/pmb/CetakRegistrasiAction.java:2477");}

			// Load salinan fresh dari DB (session-managed) agar asosiasi LAZY seperti
			// jenisSeleksi, gelombangPendaftaran, prodiLulus, prodi1 bisa diakses di
			// dalam checkKegiatanCalonMahasiswa tanpa LazyInitializationException.
			final String taForKegiatan = biodataCalonMahasiswa.getTahunAkademik();
			BiodataCalonMahasiswa biodataForKegiatan = (BiodataCalonMahasiswa) session.get(
					BiodataCalonMahasiswa.class, id);
			if (biodataForKegiatan == null) biodataForKegiatan = biodataCalonMahasiswa;

			JenisKegiatan jenisKegiatan = ConstantValues.PENDAFTARAN_CALON_MAHASISWA;
			Kegiatan pembayaranRegistrasi = null;
			try {
				pembayaranRegistrasi = KegiatanHelper.checkKegiatanCalonMahasiswa(jenisKegiatan,
						biodataForKegiatan, 0, taForKegiatan, true, false, null, session);
				sinkronkanNominalKegiatanDariLogHostToHost(session, biodataForKegiatan, pembayaranRegistrasi, false);
			} catch (Exception kegEx) {
				// KE-10: bila koneksi/statement JDBC yang dipegang `session` sudah tertutup (c3p0
				// mengembalikan koneksi basi / diputus admin-maintenance), session.clear()+
				// beginTransaction() DI BAWAH tidak bisa memperbaiki koneksi fisik yang mati --
				// hanya membuat panggilan berikutnya (checkKegiatanCalonMahasiswa kedua & sql
				// update di akhir method) ikut gagal dgn error yang sama (berantai). Untuk kasus
				// ini, lempar ulang agar loop retry di pemanggil (percobaanDeadlock) membuka
				// SESSION/KONEKSI BARU sepenuhnya -- itulah satu-satunya perbaikan yang benar.
				if (ais.common.Common.isTransientKoneksiError(kegEx)) {
					if (kegEx instanceof RuntimeException) {
						throw (RuntimeException) kegEx;
					}
					throw new RuntimeException(kegEx);
				}
				ais.common.Common.tampilErrorJikaAdmin(kegEx);
				// FIX akar masalah "Transaction not successfully started" pada tx.commit() di akhir
				// method (KE-2/KE-3): session.clear() saja TIDAK cukup memulihkan transaksi yang sudah
				// gagal -- itu cuma membersihkan cache Hibernate, BUKAN mengirim ROLLBACK ke PostgreSQL.
				// Bila statement di checkKegiatanCalonMahasiswa gagal krn constraint/data (bukan koneksi
				// transient, sudah ditangani di atas), transaksi PostgreSQL yang mendasarinya tetap
				// "aborted" (SQLState 25P02) -- beginTransaction() berikutnya bisa jadi hanya
				// mengembalikan wrapper Hibernate yang sama tanpa benar-benar memulai transaksi baru,
				// sehingga SEMUA statement berikutnya (termasuk tx.commit() di akhir) ikut gagal
				// berantai. Rollback eksplisit dulu supaya PostgreSQL benar-benar menutup transaksi lama
				// sebelum session.clear()+beginTransaction() memulai yang baru.
				try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception rbEx) { ais.common.ErrorAuditUtil.record(rbEx, "auto-audit(empty-catch) src/ais/action/master/pmb/CetakRegistrasiAction.java:2494-rollback");}
				// Jangan lanjut menulis FK null/hasil parsial lalu melaporkan sinkronisasi
				// sebagai berhasil. Biarkan eksekutor mencatat calon ini sebagai gagal.
				throw kegEx instanceof RuntimeException ? (RuntimeException) kegEx : new RuntimeException(kegEx);
			}

			jenisKegiatan = ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU;

			Kegiatan kegiatanDaftarUlang = null;
			try {
				kegiatanDaftarUlang = KegiatanHelper.checkKegiatanCalonMahasiswa(jenisKegiatan,
						biodataForKegiatan, 1, taForKegiatan, true, false, null, session);
				sinkronkanNominalKegiatanDariLogHostToHost(session, biodataForKegiatan, kegiatanDaftarUlang, true);
			} catch (Exception kegEx) {
				// KE-10: sama seperti panggilan checkKegiatanCalonMahasiswa pertama di atas -- error
				// koneksi transient tidak bisa "dipulihkan" dgn clear()+beginTransaction() pada
				// session yang koneksi fisiknya sudah mati. Lempar ulang agar loop retry di
				// pemanggil membuka session/koneksi baru sepenuhnya.
				if (ais.common.Common.isTransientKoneksiError(kegEx)) {
					if (kegEx instanceof RuntimeException) {
						throw (RuntimeException) kegEx;
					}
					throw new RuntimeException(kegEx);
				}
				ais.common.Common.tampilErrorJikaAdmin(kegEx);
				// FIX akar masalah "Transaction not successfully started" (KE-2/KE-4/KE-5): sama
				// seperti panggilan checkKegiatanCalonMahasiswa pertama di atas -- session.clear() saja
				// tidak mengirim ROLLBACK ke PostgreSQL, jadi transaksi yang sudah "aborted" (25P02)
				// tetap rusak sampai beginTransaction() berikutnya, membuat tx.commit() di akhir method
				// ikut gagal berantai. Rollback eksplisit dulu sebelum memulai transaksi baru.
				try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception rbEx) { ais.common.ErrorAuditUtil.record(rbEx, "auto-audit(empty-catch) src/ais/action/master/pmb/CetakRegistrasiAction.java:2505-rollback");}
				// Jangan menimpa pembayaran_daftar_ulang menjadi null bila kalkulasi gagal.
				throw kegEx instanceof RuntimeException ? (RuntimeException) kegEx : new RuntimeException(kegEx);
			}

			if (tx == null || !tx.isActive()) {
				tx = session.beginTransaction();
			}

			StringBuilder sql1 = new StringBuilder();
			sql1.append("update biodata_calon_mahasiswa set pembayaran_registrasi=")
					.append(pembayaranRegistrasi == null ? "null" : pembayaranRegistrasi.getId());
			if (pembayaranRegistrasi != null && pembayaranRegistrasi.getTanggalBayarAwal() != null) {
				sql1.append(", tanggalpembayaranregistrasi='")
						.append(Common.databaseDateFormat1.get().format(pembayaranRegistrasi.getTanggalBayarAwal()))
						.append("'");
			}
			if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getMahasiswa() != null) {
				sql1.append(", tanggal_masuk='").append(
						Common.databaseDateFormat1.get().format(biodataCalonMahasiswa.getMahasiswa().getTanggalMasuk()))
						.append("'");
			}
			sql1.append(" where id=").append(id);

			StringBuilder sql2 = new StringBuilder();
			sql2.append("update biodata_calon_mahasiswa set pembayaran_daftar_ulang=")
					.append(kegiatanDaftarUlang == null ? "null" : kegiatanDaftarUlang.getId());
			if (kegiatanDaftarUlang != null && kegiatanDaftarUlang.getTanggalBayarAwal() != null) {
				sql2.append(", tanggalpembayarandaftarulang='")
						.append(Common.databaseDateFormat1.get().format(kegiatanDaftarUlang.getTanggalBayarAwal()))
						.append("'");
			}
			sql2.append(" where id=").append(id);

			session.createSQLQuery(sql1.toString()).executeUpdate();
			session.createSQLQuery(sql2.toString()).executeUpdate();

			if (tx == null || !tx.isActive()) {
				tx = session.beginTransaction();
			}
			tx.commit();
			berhasilKomit = true;

			// Data calon ditampilkan melalui ConstantValues (cache JVM). Tanpa invalidasi,
			// Refresh/grid tetap membaca relasi Kegiatan dan angka tagihan sebelum sinkron.
			// Hapus hanya entitas yang baru diubah; request berikutnya memuat nilai fresh DB.
			ConstantValues.hapus(BiodataCalonMahasiswa.class.getName(), id);
			if (pembayaranRegistrasi != null && pembayaranRegistrasi.getId() != null) {
				ConstantValues.hapus(Kegiatan.class.getName(), pembayaranRegistrasi.getId());
			}
			if (kegiatanDaftarUlang != null && kegiatanDaftarUlang.getId() != null) {
				ConstantValues.hapus(Kegiatan.class.getName(), kegiatanDaftarUlang.getId());
			}

		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/pmb/CetakRegistrasiAction.java:2543");
				}
			}
			// KE-10: "connection has been closed"/"statement has been closed" (c3p0 mengembalikan
			// koneksi basi, atau koneksi terputus admin/maintenance) diperlakukan SAMA seperti
			// konflik kunci deadlock di atas -- keduanya transient & method ini idempotent. Retry
			// aman karena iterasi berikutnya membuka session/koneksi BARU (baris session =
			// HibernateUtil.getSessionFactory().openSession() di atas), bukan memakai koneksi basi
			// yang sama.
			boolean koneksiTransient = ais.common.Common.isTransientKoneksiError(e);
			konflikKunciDeadlock = isKonflikKunciSinkronisasi(e) || koneksiTransient;
			if (!(konflikKunciDeadlock && percobaanDeadlock < maksimalPercobaanDeadlock)) {
				if (koneksiTransient) {
					ais.common.ErrorAuditUtil.record(e,
							"Koneksi database terputus saat proses cetak (singkronkanDenganPembayaran, id="
									+ id + ", percobaan=" + percobaanDeadlock + "/" + maksimalPercobaanDeadlock + ")");
				} else {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.clear();
					session.disconnect();
					session.close();
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		}
		if (berhasilKomit || !konflikKunciDeadlock) {
			return;
		}
		try {
			Thread.sleep(Math.min(200L * percobaanDeadlock, 1500L) + (long) (Math.random() * 150));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			return;
		}
		}
	}

	private static void sinkronkanNominalKegiatanDariLogHostToHost(Session session,
			BiodataCalonMahasiswa biodataCalonMahasiswa, Kegiatan kegiatan, boolean daftarUlang) {
		try {
			if (session == null || biodataCalonMahasiswa == null || kegiatan == null || kegiatan.getId() == null) {
				return;
			}
			double totalKegiatan = (kegiatan.getAmount() == null ? 0.0 : kegiatan.getAmount().doubleValue())
					+ (kegiatan.getAmountTerhutang() == null ? 0.0 : kegiatan.getAmountTerhutang().doubleValue());
			if (totalKegiatan > 0.01) {
				return;
			}
			String noReg = biodataCalonMahasiswa.getNoRegistrasi() == null ? ""
					: biodataCalonMahasiswa.getNoRegistrasi().trim();
			String noUjian = biodataCalonMahasiswa.getNoUjian() == null ? ""
					: biodataCalonMahasiswa.getNoUjian().trim();
			if (noReg.length() == 0 && noUjian.length() == 0) {
				return;
			}
			String kolomCari = "lower(coalesce(item,'') || ' ' || coalesce(response,'') || ' ' "
					+ "|| coalesce(nama,'') || ' ' || coalesce(keterangan,''))";
			String sql = "select nominal, tanggal from log_host_to_host "
					+ "where nominal is not null and nominal > 0.01 "
					+ "and (trim(coalesce(nim,'')) = :noReg or trim(coalesce(nim,'')) = :noUjian "
					+ "or trim(coalesce(kode,'')) = :noReg or trim(coalesce(kode,'')) = :noUjian) "
					+ "and (lower(coalesce(response_code,'')) in ('ok','00','success','sukses') "
					+ "or lower(coalesce(response_description,'')) like '%success%' "
					+ "or lower(coalesce(response_description,'')) like '%sukses%' "
					+ "or lower(coalesce(response,'')) like '%succeeded%' "
					+ "or lower(coalesce(response,'')) like '%\"rc\":\"ok\"%') "
					+ "and (" + kolomCari + " like :k1 or " + kolomCari + " like :k2 or "
					+ kolomCari + " like :k3) "
					+ "order by tanggal desc, id desc limit 1";
			org.hibernate.SQLQuery query = session.createSQLQuery(sql);
			query.setParameter("noReg", noReg);
			query.setParameter("noUjian", noUjian);
			if (daftarUlang) {
				query.setParameter("k1", "%spp%");
				query.setParameter("k2", "%daftar ulang%");
				query.setParameter("k3", "%semester%");
			} else {
				query.setParameter("k1", "%uang pendaftaran%");
				query.setParameter("k2", "%pendaftaran calon mahasiswa%");
				query.setParameter("k3", "%registrasi%");
			}
			Object rowObj = query.uniqueResult();
			if (!(rowObj instanceof Object[])) {
				return;
			}
			Object[] row = (Object[]) rowObj;
			if (!(row[0] instanceof Number)) {
				return;
			}
			double nominalHostToHost = ((Number) row[0]).doubleValue();
			if (nominalHostToHost <= 0.01) {
				return;
			}
			kegiatan.setTagihan(nominalHostToHost);
			kegiatan.setDibayar(nominalHostToHost);
			kegiatan.setPersentaseLunas(100.0);
			kegiatan.setLunas(true);
			if (row.length > 1 && row[1] instanceof java.util.Date) {
				kegiatan.setTanggalBayarAwal((java.util.Date) row[1]);
			}
			Common.refreshSaveOrUpdate(session, kegiatan);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit CetakRegistrasiAction.sinkronkanNominalKegiatanDariLogHostToHost");
		}
	}

	/**
	 * Deteksi apakah exception dari {@link #singkronkanDenganPembayaran(Long, Label, int, int)}
	 * disebabkan konflik kunci sementara (deadlock/serialization failure/lock timeout PostgreSQL)
	 * yang aman diulang, bukan masalah data permanen. Pola sama dgn
	 * {@code TagihanUtil.isKonflikKunci} (SQL state 40P01/40001/55P03/57014/25P02 + pesan terkait).
	 */
	private static boolean isKonflikKunciSinkronisasi(Throwable e) {
		Throwable c = e;
		while (c != null) {
			String state = (c instanceof java.sql.SQLException) ? ((java.sql.SQLException) c).getSQLState() : null;
			if ("40P01".equals(state) || "40001".equals(state) || "55P03".equals(state) || "57014".equals(state)
					|| "25P02".equals(state)) {
				return true;
			}
			String msg = c.getMessage();
			if (msg != null) {
				String m = msg.toLowerCase();
				if (m.indexOf("deadlock detected") >= 0 || m.indexOf("could not serialize") >= 0
						|| m.indexOf("lock timeout") >= 0 || m.indexOf("current transaction is aborted") >= 0) {
					return true;
				}
			}
			c = c.getCause();
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public void onDownloadFoto(Event event) {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Long> calonBiodataCalonMahasiswa = initCriteria(true).list();
				File fileFolderLampiran = new File(
						"/opt/ecampus/foto_" + ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
				fileFolderLampiran.mkdirs();
				System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());

				for (Long biodataCalonMahasiswaid : calonBiodataCalonMahasiswa) {
					BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
							.ambil(BiodataCalonMahasiswa.class.getName(), biodataCalonMahasiswaid);
					if (biodataCalonMahasiswa != null) {
						Session streamingSession = null;
						try {
							streamingSession = StreamingHibernateUtil.getInstance().currentSession();

							FotoBiodataCalonMahasiswa fotobiodataCalonMahasiswa = (FotoBiodataCalonMahasiswa) streamingSession
									.createCriteria(FotoBiodataCalonMahasiswa.class)
									.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId()))
									.setMaxResults(1).uniqueResult();

							if (fotobiodataCalonMahasiswa != null && fotobiodataCalonMahasiswa.getLink() != null
									&& fotobiodataCalonMahasiswa.getLink().toLowerCase().contains("dropbox")) {
								File fileCopy = new File(fileFolderLampiran.getAbsolutePath() + "/FOTO_"
										+ URLEncoder.encode(biodataCalonMahasiswa.getNoRegistrasi() + "_"
												+ biodataCalonMahasiswa.getNama(), "UTF-8")
										+ ".txt");
								ais.common.BacaTulisUtil.tulis(fileCopy, fotobiodataCalonMahasiswa.dropboxLinkRaw());
							} else if (fotobiodataCalonMahasiswa != null
									&& fotobiodataCalonMahasiswa.getGdrive() != null) {
								File fileCopy = new File(fileFolderLampiran.getAbsolutePath() + "/FOTO_"
										+ URLEncoder.encode(biodataCalonMahasiswa.getNoRegistrasi() + "_"
												+ biodataCalonMahasiswa.getNama(), "UTF-8")
										+ ".txt");
								ais.common.BacaTulisUtil.tulis(fileCopy, fotobiodataCalonMahasiswa.forwardGDriveUrl());
							} else if (fotobiodataCalonMahasiswa != null
									&& fotobiodataCalonMahasiswa.getFoto() != null) {
								File fileFoto = fotobiodataCalonMahasiswa.ambilFile();
								File fileCopy = new File(fileFolderLampiran.getAbsolutePath() + "/"
										+ biodataCalonMahasiswa.getNoRegistrasi() + "_"
										+ biodataCalonMahasiswa.getNama() + "_" + fileFoto.getName());

								FileOutputStream fileOutputStream = null;
								FileInputStream fileInputStream = null;
								try {
									fileOutputStream = new FileOutputStream(fileCopy);
									fileInputStream = new FileInputStream(fileFoto);
									IOUtils.copyLarge(fileInputStream, fileOutputStream);
								} finally {
									if (fileInputStream != null) {
										try {
											fileInputStream.close();
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									}
									if (fileOutputStream != null) {
										try {
											fileOutputStream.close();
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									}
								}
							}
						} catch (Exception e1) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/pmb/CetakRegistrasiAction.java:2670");
						} finally {
							try {
								StreamingHibernateUtil.getInstance().closeSession();
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						}
					}
				}
				File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
				Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
				Filedownload.save(fileFolderLampiranZip, "application/zip");
			}
		}, "Harap tunggu.. sedang melakukan proses download foto..");
	}

	public static void bukaRinci(final MyDetail detail, final BiodataCalonMahasiswa calonMahasiswa) {
		if (detail.isOpen()) {
			Common.clear(detail);

			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {

					Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
					groupbox.setParent(detail);
					groupbox.setHeight("4750px");
					groupbox.appendChild(new MyCaptionStyled("Verifikasi dan Hasil Ujian"));

					Tabbox tabbox = new Tabbox();
					tabbox.setHeight("4750px");
					tabbox.setParent(groupbox);
					Tabs tabs = new Tabs();
					tabs.setParent(tabbox);

					final MyTabConfig tabData = new MyTabConfig("Verifikasi");
					tabData.setParent(tabs);

					Tabpanels tabpanels = new Tabpanels();
					tabpanels.setParent(tabbox);

					Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
					tabpanel.setParent(tabpanels);

					Borderlayout myborderlayout = new Borderlayout();
					myborderlayout.setParent(Common.tampilanScroll(tabpanel));
					myborderlayout.setWidth("100%");
					myborderlayout.setHeight("4700px");

					Center mycenter = new Center();
					mycenter.setParent(myborderlayout);
					ais.ui.util.ZkCompat.setFlex(mycenter, true);

					Session session = null;
					int jumlah = 0;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						jumlah = calonMahasiswa.ambilHasilUjianMahasiswa(session, false).size();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					} finally {
						if (session != null && session.isOpen()) {
							try {
								session.close();
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						}
					}

					final MyTabConfig tabHasilUjian = new MyTabConfig(Common.getBahasaConfig("Hasil Ujian") + " "
							+ (jumlah == 0 ? "" : "(" + jumlah + " " + Common.getBahasaConfig("ujian") + ")"));
					tabHasilUjian.setParent(tabs);
					final Tabpanel tabpanelHasilUjian = new ais.ui.util.MyTabpanel();
					tabpanelHasilUjian.setParent(tabpanels);

					tabHasilUjian.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabpanelHasilUjian.getChildren().isEmpty()) {
								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										new HasilUjianHelper(null, calonMahasiswa, null).display(tabpanelHasilUjian);
									}
								});
							}
						}
					});

					MyGrid grid = new MyGrid();
					grid.setWidth("100%");
					grid.setParent(mycenter);

					Columns columns = new Columns();
					columns.setParent(grid);
					MyColumnConfig column = new MyColumnConfig();
					column.setParent(columns);
					column.setWidth("40%");

					column = new MyColumnConfig();
					column.setParent(columns);

					final Rows rows = new Rows();
					rows.setParent(grid);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);

					VerifikasiPMBHelper.tampilkanVerifikasi(calonMahasiswa, rows, null, null, null);
					VerifikasiMatapelajaranPMBHelper.tampilkanVerifikasi(calonMahasiswa, rows, null, null);
					VerifikasiParameterPMBHelper.tampilkanVerifikasi(calonMahasiswa, rows, null, null);
				}
			});
		}
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link CetakRegistrasiAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link CetakRegistrasiAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render()}, {@code
	 * pasangCellTagihan}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see CetakRegistrasiAction
	 */
	class CalonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");

			final BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
					.ambil(BiodataCalonMahasiswa.class.getName(), (Serializable) arg1);
			if (calonMahasiswa == null) {
				arg0.setVisible(false);
				return;
			}

			if (calonMahasiswa.getNoRegistrasi() == null || calonMahasiswa.getNoRegistrasi().trim().isEmpty()) {
				calonMahasiswa.setNoRegistrasi(CommonPMB.generateNoRegistrasi(calonMahasiswa));
				// Guard: hanya flush jika entity sudah tersimpan (id tidak null).
				// Flush pada entity transient (id null) menyebabkan AssertionFailure Hibernate.
				if (calonMahasiswa.getId() != null) {
					try {
						Common.refreshUpdate(calonMahasiswa);
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
			}

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CetakRegistrasiAction.bukaRinci(detail, calonMahasiswa);
				}
			});

			CommonMedia.tampilkanGambarKecil(calonMahasiswa).setParent(arg0);

			Vbox aa;
			(aa = RevisiHelper.createNewRevisi(BiodataCalonMahasiswa.class, calonMahasiswa, calonMahasiswa.getNama()))
					.setParent(arg0);
			aa.appendChild(new MyLabelKecilBold(calonMahasiswa.getKeterangan()));

			if (integrasi_pmb_arkatama) {
				aa.appendChild(new MyLabelKecil(calonMahasiswa.getPinPassword()));
			}

			calonMahasiswa.tampilkanHp(aa);
			calonMahasiswa.tampilkanEmail(aa);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelKecil(calonMahasiswa.getTempatLahir()).setParent(vbox);
			new MyLabelKecil(calonMahasiswa.getTanggalLahir() == null
					? Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate())
					: Common.dateFormat2.get().format(calonMahasiswa.getTanggalLahir())).setParent(vbox);

			StringBuilder dd = new StringBuilder();
			if (calonMahasiswa.getInfoKampusDariMana() != null) {
				for (String s : calonMahasiswa.getInfoKampusDariMana().split(";")) {
					if (dd.length() > 0)
						dd.append(" dan ");
					dd.append(s);
				}
			}

			new MyLabelKecil(dd.toString()).setParent(vbox);
			new MyLabelKecil(calonMahasiswa.getNamaTemanInfoKampusDariMana()).setParent(vbox);
			new MyLabelKecil(calonMahasiswa.getKeteranganInfoKampusDariMana()).setParent(vbox);

			new MyLabelKecil(calonMahasiswa.getAfiliasiMahasiswa() != null
					? "Afiliasi:" + calonMahasiswa.getAfiliasiMahasiswa().getNama()
					: calonMahasiswa.getAfiliasiPegawai() != null
							? "Afiliasi:" + calonMahasiswa.getAfiliasiPegawai().getNama()
							: calonMahasiswa.getAfiliasiCalonMahasiswa() != null
									? "Afiliasi:" + calonMahasiswa.getAfiliasiCalonMahasiswa().getNama()
									: "")
					.setParent(vbox);

			vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelKecil(calonMahasiswa.getAsalSma() == null ? "" : calonMahasiswa.getAsalSma()).setParent(vbox);
			new MyLabelKecil(calonMahasiswa.getJenisSekolah() == null ? "" : calonMahasiswa.getJenisSekolah().getNama())
					.setParent(vbox);
			new MyLabelKecil(
					calonMahasiswa.getJurusanSekolah() == null ? "" : calonMahasiswa.getJurusanSekolah().getNama())
					.setParent(vbox);
			new MyLabelKecil("thn kelulusan:" + calonMahasiswa.getTahunKelulusan()).setParent(vbox);
			new MyLabelKecil(
					calonMahasiswa.getJurusanSekolahLain() == null ? "" : calonMahasiswa.getJurusanSekolahLain())
					.setParent(vbox);
			new MyLabelKecil(calonMahasiswa.getAlamatAsalSma() == null ? "" : calonMahasiswa.getAlamatAsalSma())
					.setParent(vbox);

			vbox = new Vbox();
			if (calonMahasiswa.getNoRegistrasi() != null && !calonMahasiswa.getNoRegistrasi().trim().isEmpty())
				vbox.appendChild(new Label("No. Reg.:" + (calonMahasiswa.getNoRegistrasi())));
			if (calonMahasiswa.getNoUjian() != null && !calonMahasiswa.getNoUjian().trim().isEmpty())
				vbox.appendChild(new Label("No. Ujian:" + (calonMahasiswa.getNoUjian())));
			if (calonMahasiswa.getTotalSkor() > 0)
				vbox.appendChild(
						new Label("Skor :" + Common.numberFormat.get().format((calonMahasiswa.getTotalSkor()))));
			vbox.appendChild(new Label("Login :" + (calonMahasiswa.getTelahLogin() ? "Ya" : "Tidak")));
			if (calonMahasiswa.getWaktuLogin() != null)
				vbox.appendChild(
						new Label("Terakhir Login :" + Common.dateFormat.get().format(calonMahasiswa.getWaktuLogin())));
			if (calonMahasiswa.getNim() != null && !calonMahasiswa.getNim().trim().isEmpty())
				vbox.appendChild(new Label("NIM :" + (calonMahasiswa.getNim())));
			if (calonMahasiswa.getMerupakanPindahan()) {
				vbox.appendChild(new Label("Pindahan dari :" + (calonMahasiswa.getPindahanDariKampus())));
				vbox.appendChild(new Label("Prodi :" + (calonMahasiswa.getPindahanDariProdi())));
				vbox.appendChild(
						new Label("Pindah di semester :" + (calonMahasiswa.getPindahDariKampusLamaDiSemester())));
				vbox.appendChild(new Label("NIM lama :" + (calonMahasiswa.getNimLamaSebelumPindah())));
				vbox.appendChild(new Label("Alasan pindah:" + (calonMahasiswa.getKeteranganPindah())));
			}
			vbox.setParent(arg0);

			vbox = new Vbox();
			vbox.setParent(arg0);

			new Label(calonMahasiswa.getGelombangPendaftaran() == null ? ""
					: calonMahasiswa.getGelombangPendaftaran().getNama()).setParent(vbox);
			new Label(calonMahasiswa.getPaket() == null ? "" : calonMahasiswa.getPaket().getNama()).setParent(vbox);
			new Label(calonMahasiswa.getJenisSeleksi() == null ? "" : calonMahasiswa.getJenisSeleksi().getNama())
					.setParent(vbox);

			DiskonMahasiswa diskonMahasiswa = DiskonCalonMahasiswaAction.ambilDiskon(calonMahasiswa);
			if (diskonMahasiswa != null && diskonMahasiswa.getJenisDiskonMahasiswa() != null) {
				new Label("Diskon: " + diskonMahasiswa.getJenisDiskonMahasiswa().getNama()).setParent(vbox);
			}

			vbox = new Vbox();
			if (calonMahasiswa.getProdi1() != null)
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi1())));
			if (calonMahasiswa.getProdi2() != null)
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi2())));
			if (calonMahasiswa.getProdi3() != null)
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi3())));
			if (calonMahasiswa.getProdi4() != null)
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi4())));
			if (calonMahasiswa.getProdi5() != null)
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi5())));
			if (calonMahasiswa.getKonsentrasi() != null)
				vbox.appendChild(new Label("" + (calonMahasiswa.getKonsentrasi().getNama())));

			if (calonMahasiswa.getMundur()) {
				vbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Mengundurkan diri")));
			} else if (calonMahasiswa.getDitolak()) {
				vbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak diterima (ditolak)")));
			} else if (calonMahasiswa.getProdiLulus() != null) {
				vbox.appendChild(new Label("Lulus di prodi : " + (calonMahasiswa.getProdiLulus())));
			} else {
				vbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Belum lulus")));
			}

			int sehat = 0;
			Session sessionRow = null;
			RuangPaketPMB ruangPaketPMB = null;
			try {
				sessionRow = HibernateUtil.getSessionFactory().openSession();
				// Guard: criteria query dengan entity sebagai parameter membutuhkan id non-null.
				// Entity transient (id==null) akan memicu TransientObjectException di Hibernate.
				if (calonMahasiswa.getId() != null) {
					sehat = ((Number) sessionRow.createCriteria(CekKesehatan.class)
							.setProjection(Projections.rowCount())
							.add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa))
							.uniqueResult()).intValue();

					ruangPaketPMB = (RuangPaketPMB) sessionRow.createCriteria(RuangPaketPMB.class)
							.add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa))
							.setMaxResults(1).uniqueResult();
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} finally {
				if (sessionRow != null && sessionRow.isOpen()) {
					try {
						sessionRow.close();
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				}
			}

			new Label(sehat == 0 ? "Belum cek kesehatan" : "Telah chek kesehatan").setParent(vbox);

			new Label(ruangPaketPMB == null || ruangPaketPMB.getRuangPMB() == null ? ""
					: "R. Ujian : " + ruangPaketPMB.getRuangPMB().getNama()).setParent(vbox);

			vbox.setParent(arg0);

			if (harusBayarSebelumLogin) {
				Kegiatan kegiatan = calonMahasiswa.chekPembayaranRegistrasi();
				pasangCellTagihan(arg0, kegiatan, calonMahasiswa, true);
			} else {
				new Label("").setParent(arg0);
			}

			Kegiatan kegiatanDaftarUlang = calonMahasiswa.chekPembayaranDaftarUlang();
			if (calonMahasiswa == null || calonMahasiswa.getProdiLulus() == null) {
				new Label("Belum dinyatakan Lulus/Diterima").setParent(arg0);
			} else {
				pasangCellTagihan(arg0, kegiatanDaftarUlang, calonMahasiswa, false);
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(calonMahasiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					calonMahasiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(calonMahasiswa);
				}
			});

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dikumpulkan lalu dibungkus
			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Edit", "/img/svg/edit-box-line.svg");
			button.setOrient("vertical");
			button.setStyle("font-size:9px;");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CetakRegistrasiAction.onEdit(calonMahasiswa, CetakRegistrasiAction.this);
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Reg dan Email", "/img/print.png");
			button.setStyle("font-size:9px;");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					BiodataCalonMahasiswaAction.onCetakKartu(calonMahasiswa, true);
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Biodata", "/img/online-icon_access.png");
			button.setStyle("font-size:9px;");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportHelper.onCetakBiodataCalonMahasiswa(calonMahasiswa, true);
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Kartu Ujian", "/img/print.png");
			button.setVisible(tampilkanInformasiUjianDiPMB);
			button.setStyle("font-size:9px;");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportHelper.onCetakKartuUjianPMB(Common.getCurrentUser(), calonMahasiswa,
							calonMahasiswa.getNoUjian());
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Interview", "/img/svg/user-business.svg");
			try {
				button.setVisible(calonMahasiswa.getGelombangPendaftaran() != null
						&& calonMahasiswa.getGelombangPendaftaran().getTerdapatInterview());
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			button.setStyle("font-size:9px;");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					InterviewCalonMahasiswaAction.tampilkanInterview(calonMahasiswa);
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Copy", "/img/svg/edit-copy.svg");
			button.setOrient("vertical");
			button.setStyle("font-size:9px;");
			button.setTooltiptext("Copy Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					calonMahasiswa.setId(null);
					calonMahasiswa.setNoRegistrasi(null);
					calonMahasiswa.setNoUjian(null);
					calonMahasiswa.setMahasiswa(null);
					CetakRegistrasiAction.onEdit(calonMahasiswa, CetakRegistrasiAction.this);
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("No. Ujian", "/img/Configure.gif");
			button.setStyle("font-size:9px;");
			button.setOrient("vertical");
			button.setVisible(edit && tampilkanInformasiUjianDiPMB);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (calonMahasiswa.getNoUjian() != null && !calonMahasiswa.getNoUjian().trim().isEmpty()) {
						MyMessageboxConfig.show(
								"Calon Mahasiswa ini sudah memiliki " + "nomor ujian, yaitu : "
										+ calonMahasiswa.getNoUjian(),
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										CommonReportHelper.onCetakKartuUjianPMB(Common.getCurrentUser(), calonMahasiswa,
												calonMahasiswa.getNoUjian());
									}
								});
						return;
					}

					final String noUjianGenerated = CommonPMB.generateNoUjian(Common.getCurrentUser(), calonMahasiswa);

					if (!noUjianGenerated.trim().isEmpty()) {
						MyMessageboxConfig.show("Generated no ujian : " + noUjianGenerated, "Informasi",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										CommonReportHelper.onCetakKartuUjianPMB(Common.getCurrentUser(), calonMahasiswa,
												noUjianGenerated);
									}
								});
						onSearchDefault(null);
					}
					return;
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Bukti Diterima", "/img/svg/check2.svg");
			button.setStyle("font-size:9px;");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportHelper.onCetakSuratKeteranganLulus(calonMahasiswa, false);
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("KTM", "/img/svg/file.svg");
			button.setStyle("font-size:9px;");
			button.setOrient("vertical");
			button.setVisible(edit && calonMahasiswa.getMahasiswa() != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					LaporanKartuMahasiswa kartuMahasiswa = new LaporanKartuMahasiswa(calonMahasiswa.getMahasiswa());
					kartuMahasiswa.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					kartuMahasiswa.setBorder(false);
					kartuMahasiswa.setBorder("none");
					kartuMahasiswa.setClosable(true);
					kartuMahasiswa.setTitle("Kartu Tanda Mahasiswa");
					kartuMahasiswa.setHeight("95%");
					kartuMahasiswa.setWidth("800px");
					kartuMahasiswa.onModal();
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("NIM", "/img/svg/file.svg");
			button.setStyle("font-size:9px;");
			button.setOrient("vertical");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					try {
						NimGenerator nimGenerator = (NimGenerator) Class.forName(Common
								.getKonfigurasi("class_untuk_generate_nim", "ais.action.master.pmb.nim.DefaultNimGenerator")
								.getNilai().trim()).newInstance();
						String nimHasil = CommonPMB.onGenerateNim(calonMahasiswa, nimGenerator);
						if (nimHasil == null || nimHasil.trim().isEmpty()) {
							return;
						}
						onSearchDefault(null);
						MyMessageboxConfig.showFormat(
								"NIM berhasil diproses untuk {V1}. NIM: {V2}. Daftar calon mahasiswa telah diperbarui.",
								"Berhasil", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								calonMahasiswa.getNama(), nimHasil);
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e,
								"CetakRegistrasiAction tombol NIM calon=" + calonMahasiswa.getId());
						Common.tampilErrorJikaAdmin(e);
						MyMessageboxConfig.showFormat(
								"NIM belum berhasil dibuat untuk {V1}. Penyebab: {V2}. Periksa kelengkapan prodi lulus dan konfigurasi format NIM, lalu ulangi proses.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR,
								calonMahasiswa.getNama(),
								e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
					}
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setStyle("font-size:9px;");
			button.setOrient("vertical");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && calonMahasiswa.getDikunci() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDeleteFlush(calonMahasiswa);
											Common.createDefaultTimer(new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});
										} catch (org.hibernate.StaleStateException staleEx) {
											// KE-6: Envers audit gagal saat flush krn baris yg dihapus sudah
											// diubah/dihapus proses/pengguna LAIN lbh dulu (bukan relasi data,
											// murni konflik konkurensi) -> pesan spesifik, bukan teks teknis Hibernate.
											Common.tampilErrorJikaAdmin(staleEx);
											MyMessageboxConfig.show(
													"Data ini sudah diubah atau dihapus oleh proses/pengguna lain sesaat sebelum penghapusan selesai. "
															+ "Silakan muat ulang (refresh) halaman lalu periksa kembali datanya.");
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
			aksiButtons.add(button);

			// Tombol Kunci / Buka Kunci via container sementara lalu pindah ke daftar aksi
			// (pola sama dengan MahasiswaAction).
			Hbox tempKunci = new Hbox();
			GeneralValueObject.tampilKunci(tempKunci, calonMahasiswa, tbmuser, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			}, true);
			aksiButtons.addAll(new java.util.ArrayList<org.zkoss.zk.ui.Component>(tempKunci.getChildren()));

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

		private void pasangCellTagihan(final Component parent, Kegiatan kegiatan,
				final BiodataCalonMahasiswa calon, final boolean isRegistrasi) {

			final boolean tidakAdaTagihan = kegiatan != null
					&& (kegiatan.getAmount() + kegiatan.getAmountTerhutang()) < 0.01;

			String teks;
			if (tidakAdaTagihan) {
				teks = "Tidak ada tagihan";
			} else if (kegiatan == null
					|| (kegiatan.getAmount() < 0.01 && kegiatan.getPersentaseLunas() < 0.01)) {
				teks = "Belum Bayar " + (kegiatan == null ? ""
						: Common.numberFormat.get()
								.format(kegiatan.getAmount() + kegiatan.getAmountTerhutang()));
			} else if (kegiatan.getPersentaseLunas().intValue() == 100) {
				teks = "Lunas " + Common.numberFormat.get().format(kegiatan.getAmount());
			} else {
				teks = "Bayar " + Common.numberFormat.get().format(kegiatan.getAmount())
						+ " dari tagihan "
						+ Common.numberFormat.get()
								.format(kegiatan.getAmount() + kegiatan.getAmountTerhutang())
						+ " atau "
						+ Common.numberFormat.get().format(kegiatan.getPersentaseLunas()) + "%";
			}

			if (!tidakAdaTagihan) {
				new Label(teks).setParent(parent);
				return;
			}

			// "Tidak ada tagihan" — label merah + tombol (?) dengan penjelasan kontekstual
			Hbox hb = new Hbox();
			hb.setAlign("center");
			hb.setParent(parent);

			Label lbl = new Label(teks);
			lbl.setStyle("color:#dc3545;font-weight:600;");
			lbl.setParent(hb);

			final Jurusan prodiHelp = isRegistrasi ? calon.getProdi1() : calon.getProdiLulus();
			final String namaProdi = prodiHelp != null ? prodiHelp.getNama() : "(prodi tidak diketahui)";
			final String namaGelombang = calon.getGelombangPendaftaran() != null
					? calon.getGelombangPendaftaran().getNama() : "-";
			final String namaJenisSeleksi = calon.getJenisSeleksi() != null
					? calon.getJenisSeleksi().getNama() : "-";
			final String jenisKeg = isRegistrasi ? "Pendaftaran Calon Mahasiswa"
					: "Pendaftaran Ulang Mahasiswa Baru";
			final String namaCalon = calon.getNama() != null ? calon.getNama() : "-";

			Toolbarbutton btnHelp = new Toolbarbutton("?");
			btnHelp.setStyle("min-width:18px;padding:1px 5px;font-size:10px;color:#fff;"
					+ "background:#dc3545;border-radius:10px;border:none;margin-left:4px;");
			btnHelp.setTooltiptext("Klik untuk lihat penyebab & solusi");
			btnHelp.setParent(hb);

			btnHelp.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					org.zkoss.zul.Window win = new org.zkoss.zul.Window();
					win.setTitle("Penyebab & Solusi — Tidak Ada Tagihan");
					win.setClosable(true);
					win.setBorder("normal");
					win.setWidth("480px");

					String html = "<div style='padding:12px;font-size:12px;line-height:1.7'>"
							+ "<div style='margin-bottom:10px;padding:8px 12px;background:#fff5f5;"
							+ "border-left:4px solid #dc3545;border-radius:0 6px 6px 0'>"
							+ "<b style='color:#dc3545'>Penyebab</b><br>"
							+ "Setting Biaya belum dikonfigurasi untuk calon <b>" + namaCalon + "</b>:<br>"
							+ "<ul style='margin:5px 0 0 14px'>"
							+ "<li>Prodi " + (isRegistrasi ? "pilihan 1" : "diterima") + ": <b>" + namaProdi + "</b></li>"
							+ "<li>Gelombang: <b>" + namaGelombang + "</b></li>"
							+ "<li>Jenis Seleksi: <b>" + namaJenisSeleksi + "</b></li>"
							+ "<li>Jenis Kegiatan: <b>" + jenisKeg + "</b></li>"
							+ "</ul></div>"
							+ "<div style='padding:8px 12px;background:#f0faf4;"
							+ "border-left:4px solid #198754;border-radius:0 6px 6px 0'>"
							+ "<b style='color:#198754'>Solusi</b>"
							+ "<ol style='margin:5px 0 0 14px'>"
							+ "<li>Buka <b>Pengaturan &rarr; Setting Biaya</b></li>"
							+ "<li>Filter jenis kegiatan <b>&quot;" + jenisKeg + "&quot;</b>"
							+ " dan gelombang <b>&quot;" + namaGelombang + "&quot;</b></li>"
							+ "<li>Pastikan prodi <b>&quot;" + namaProdi + "&quot;</b> sudah ada &mdash;<br>"
							+ "jika pakai mode <i>Per Prodi</i>, tambahkan prodi ini ke daftar biaya</li>"
							+ "<li>Setelah diperbaiki, gunakan <b>Hitung Ulang</b><br>"
							+ "atau minta calon mahasiswa login ulang</li>"
							+ "</ol></div></div>";

					new org.zkoss.zul.Html(html).setParent(win);
					win.setParent(parent.getPage().getFirstRoot());
					win.doHighlighted();
				}
			});
		}
	}

	/**
	 * Tipe implementasi bersarang {@link NimUrutanSuggestion} milik {@link CetakRegistrasiAction}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * CetakRegistrasiAction}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan
	 * diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code Long mahasiswaId},
	 * {@code String nama}, {@code String noRegistrasi}, {@code String prodi}, {@code String nimLama}, {@code
	 * String nimSaran}, {@code String status}. Aturan bisnis bersama tetap berada pada kelas induk atau service
	 * yang dipanggilnya.</p>
	 *
	 * @see CetakRegistrasiAction
	 */
	private static class NimUrutanSuggestion {
		private Long id;
		private Long mahasiswaId;
		private String nama;
		private String noRegistrasi;
		private String prodi;
		private String nimLama;
		private String nimSaran;
		private String status;
		private boolean aman;
		private Checkbox checkbox;
	}

	private void tampilkanAnalisisUrutanNim(final Event event) throws Exception {
		final org.zkoss.zul.Window win = new org.zkoss.zul.Window();
		win.setTitle("Analisis Urutan NIM");
		win.setClosable(true);
		win.setBorder("normal");
		win.setWidth("980px");

		final Vbox vbMain = new Vbox();
		vbMain.setWidth("100%");
		vbMain.setParent(win);

		new org.zkoss.zul.Html(
				"<div style='margin:14px;padding:14px 16px;background:#eef7ff;"
				+ "border-left:4px solid #0d6efd;border-radius:0 8px 8px 0;"
				+ "font-size:13px;line-height:1.6'>"
				+ "<b>Menyiapkan analisis urutan NIM...</b><br>"
				+ "Data sedang diproses berdasarkan filter calon mahasiswa yang aktif."
				+ "<br><span style='color:#495057'>Filter aktif: " + filterAnalisisNimRingkas() + "</span>"
				+ "</div>").setParent(vbMain);

		final org.zkoss.zul.Progressmeter progress = new org.zkoss.zul.Progressmeter();
		progress.setWidth("94%");
		progress.setValue(15);
		progress.setStyle("margin:4px 14px 6px;height:14px;");
		progress.setParent(vbMain);

		final Label lblProgress = new Label("Mengambil data sesuai filter...");
		lblProgress.setStyle("display:block;margin:0 14px 18px;color:#495057;font-size:12px;");
		lblProgress.setParent(vbMain);

		win.setParent(event.getTarget().getPage().getFirstRoot());
		win.doHighlighted();

		final Timer timer = new Timer(250);
		timer.setRepeats(false);
		timer.setParent(win);
		timer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				try {
					progress.setValue(35);
					lblProgress.setValue("Membaca NIM existing dan menyusun urutan suggestion...");
					List<NimUrutanSuggestion> suggestions = buatSaranUrutanNim();
					progress.setValue(85);
					lblProgress.setValue("Menyiapkan tabel perbandingan NIM...");
					renderAnalisisUrutanNim(win, vbMain, suggestions);
				} catch (Exception e) {
					vbMain.getChildren().clear();
					new org.zkoss.zul.Html(
							"<div style='margin:14px;padding:14px 16px;background:#fff5f5;"
							+ "border-left:4px solid #dc3545;border-radius:0 8px 8px 0;"
							+ "font-size:13px;line-height:1.6'>"
							+ "<b style='color:#dc3545'>Analisis urutan NIM gagal diproses.</b><br>"
							+ "Silakan coba kembali. Jika masih gagal, buka detail error sistem untuk melihat stack trace teknis."
							+ "<br><span style='color:#6c757d'>" + bersihkan(e.getMessage()) + "</span>"
							+ "</div>").setParent(vbMain);
					Hbox hbFooter = new Hbox();
					hbFooter.setStyle("padding:10px 16px;border-top:1px solid #dee2e6;");
					hbFooter.setParent(vbMain);
					MyToolbarbuttonConfig btnTutup = new MyToolbarbuttonConfig("Tutup");
					btnTutup.setParent(hbFooter);
					btnTutup.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event e) throws Exception {
							win.detach();
						}
					});
					Common.tampilErrorJikaAdmin(e);
				} finally {
					timer.detach();
				}
			}
		});
		timer.start();
	}

	private void renderAnalisisUrutanNim(final org.zkoss.zul.Window win, Vbox vbMain,
			List<NimUrutanSuggestion> suggestions) throws Exception {
		vbMain.getChildren().clear();

		int perluDirapikan = 0;
		for (int i = 0; i < suggestions.size(); i++) {
			NimUrutanSuggestion s = suggestions.get(i);
			if (s.aman && s.nimSaran != null && !s.nimSaran.equals(s.nimLama)) {
				perluDirapikan++;
			}
		}

		new org.zkoss.zul.Html(
				"<div style='margin:10px 14px 6px;padding:10px 14px;"
				+ "background:#eef7ff;border-left:4px solid #0d6efd;border-radius:0 6px 6px 0;"
				+ "font-size:12px;line-height:1.5'>"
				+ "<b>Analisis mengikuti filter calon mahasiswa yang sedang aktif.</b><br>"
				+ "Sistem membaca prefix NIM lalu membandingkan NIM existing dengan NIM urut/suggestion. "
				+ "Perubahan hanya dilakukan setelah operator menekan tombol Apply."
				+ "<br><b>" + perluDirapikan + "</b> data memiliki saran NIM baru."
				+ "<br><span style='color:#495057'>Filter aktif: " + filterAnalisisNimRingkas() + "</span>"
				+ "</div>").setParent(vbMain);

		if (suggestions.isEmpty()) {
			new org.zkoss.zul.Html(
					"<div style='padding:24px;text-align:center;color:#198754;"
					+ "font-size:14px;font-weight:bold'>Data NIM sesuai filter belum ada untuk dianalisis.</div>")
					.setParent(vbMain);
		} else {
			org.zkoss.zul.Div divScroll = new org.zkoss.zul.Div();
			divScroll.setStyle("max-height:480px;overflow:auto;padding:0 14px 10px;");
			divScroll.setParent(vbMain);

			org.zkoss.zul.Grid grd = new org.zkoss.zul.Grid();
			grd.setWidth("100%");
			grd.setParent(divScroll);

			Columns cols = new Columns();
			cols.setParent(grd);
			org.zkoss.zul.Column cPilih = new org.zkoss.zul.Column("");
			cPilih.setWidth("42px");
			cPilih.setParent(cols);
			org.zkoss.zul.Column cNama = new org.zkoss.zul.Column("Nama");
			cNama.setWidth("210px");
			cNama.setParent(cols);
			org.zkoss.zul.Column cReg = new org.zkoss.zul.Column("No. Registrasi");
			cReg.setWidth("140px");
			cReg.setParent(cols);
			org.zkoss.zul.Column cProdi = new org.zkoss.zul.Column("Prodi");
			cProdi.setWidth("190px");
			cProdi.setParent(cols);
			org.zkoss.zul.Column cLama = new org.zkoss.zul.Column("NIM Existing");
			cLama.setWidth("135px");
			cLama.setParent(cols);
			org.zkoss.zul.Column cSaran = new org.zkoss.zul.Column("NIM Urut/Suggestion");
			cSaran.setWidth("135px");
			cSaran.setParent(cols);
			org.zkoss.zul.Column cStatus = new org.zkoss.zul.Column("Status");
			cStatus.setParent(cols);
			org.zkoss.zul.Column cAksi = new org.zkoss.zul.Column("");
			cAksi.setWidth("110px");
			cAksi.setParent(cols);

			Rows rows = new Rows();
			rows.setParent(grd);

			final List<NimUrutanSuggestion> finalSuggestions = suggestions;
			for (int i = 0; i < finalSuggestions.size(); i++) {
				final NimUrutanSuggestion s = finalSuggestions.get(i);
				Row row = new Row();
				if (!s.aman) {
					row.setStyle("background:#fff5f5;");
				} else if (s.nimSaran != null && !s.nimSaran.equals(s.nimLama)) {
					row.setStyle("background:#fffbe6;");
				}
				row.setParent(rows);

				Checkbox cb = new Checkbox();
				cb.setChecked(s.aman && s.nimSaran != null && !s.nimSaran.equals(s.nimLama));
				cb.setDisabled(!cb.isChecked());
				cb.setParent(row);
				s.checkbox = cb;

				new Label(s.nama == null ? "-" : s.nama).setParent(row);
				new Label(s.noRegistrasi == null ? "-" : s.noRegistrasi).setParent(row);
				new Label(s.prodi == null ? "-" : s.prodi).setParent(row);
				new Label(s.nimLama == null ? "-" : s.nimLama).setParent(row);
				new Label(s.nimSaran == null ? "-" : s.nimSaran).setParent(row);
				new Label(s.status == null ? "-" : s.status).setParent(row);

				MyToolbarbuttonConfig btnReplace = new MyToolbarbuttonConfig("Apply", "/img/svg/check2.svg");
				btnReplace.setDisabled(!s.aman || s.nimSaran == null || s.nimSaran.equals(s.nimLama));
				btnReplace.setParent(row);
				btnReplace.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						List<NimUrutanSuggestion> pilihan = new ArrayList<NimUrutanSuggestion>();
						pilihan.add(s);
						replaceSaranUrutanNimDenganKonfirmasi(pilihan, win);
					}
				});
			}
		}

		Hbox hbFooter = new Hbox();
		hbFooter.setSpacing("6px");
		hbFooter.setStyle("padding:10px 16px;border-top:1px solid #dee2e6;");
		hbFooter.setParent(vbMain);

		final List<NimUrutanSuggestion> finalSuggestions = suggestions;
		MyToolbarbuttonConfig btnTerpilih = new MyToolbarbuttonConfig("Apply Terpilih", "/img/svg/check2.svg");
		btnTerpilih.setParent(hbFooter);
		btnTerpilih.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				List<NimUrutanSuggestion> pilihan = new ArrayList<NimUrutanSuggestion>();
				for (int i = 0; i < finalSuggestions.size(); i++) {
					NimUrutanSuggestion s = finalSuggestions.get(i);
					if (s.checkbox != null && s.checkbox.isChecked()) {
						pilihan.add(s);
					}
				}
				replaceSaranUrutanNimDenganKonfirmasi(pilihan, win);
			}
		});

		MyToolbarbuttonConfig btnSemua = new MyToolbarbuttonConfig("Apply Semua Aman", "/img/svg/check2.svg");
		btnSemua.setParent(hbFooter);
		btnSemua.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				List<NimUrutanSuggestion> pilihan = new ArrayList<NimUrutanSuggestion>();
				for (int i = 0; i < finalSuggestions.size(); i++) {
					NimUrutanSuggestion s = finalSuggestions.get(i);
					if (s.aman && s.nimSaran != null && !s.nimSaran.equals(s.nimLama)) {
						pilihan.add(s);
					}
				}
				replaceSaranUrutanNimDenganKonfirmasi(pilihan, win);
			}
		});

		MyToolbarbuttonConfig btnTutup = new MyToolbarbuttonConfig("Tutup");
		btnTutup.setParent(hbFooter);
		btnTutup.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				win.detach();
			}
		});

	}

	@SuppressWarnings("unchecked")
	private List<NimUrutanSuggestion> buatSaranUrutanNim() {
		List<Long> ids = initCriteria(true).setProjection(Projections.property("id")).list();
		List<Long> uniqueIds = new ArrayList<Long>(new LinkedHashSet<Long>(ids));
		List<NimUrutanSuggestion> hasil = new ArrayList<NimUrutanSuggestion>();
		if (uniqueIds.isEmpty()) {
			return hasil;
		}

		int jumlahDigit = getJumlahDigitUrutNim();
		Map<String, List<NimUrutanSuggestion>> perPrefix = new HashMap<String, List<NimUrutanSuggestion>>();
		Session session = HibernateUtil.currentSession();
		for (int i = 0; i < uniqueIds.size(); i++) {
			BiodataCalonMahasiswa calon = (BiodataCalonMahasiswa) session.get(BiodataCalonMahasiswa.class,
					uniqueIds.get(i));
			if (calon == null) {
				continue;
			}
			String nim = bersihkan(calon.getNim());
			if (nim == null) {
				continue;
			}

			NimUrutanSuggestion s = new NimUrutanSuggestion();
			s.id = calon.getId();
			s.mahasiswaId = calon.getMahasiswa() == null ? null : calon.getMahasiswa().getId();
			s.nama = calon.getNama();
			s.noRegistrasi = calon.getNoRegistrasi();
			s.prodi = calon.getProdiLulus() == null ? null : calon.getProdiLulus().getNama();
			s.nimLama = nim;

			String prefix = ambilPrefixNim(nim, jumlahDigit);
			Long nomor = ambilSuffixNim(nim, jumlahDigit);
			if (prefix == null || nomor == null) {
				s.aman = false;
				s.status = "Format NIM tidak terbaca untuk " + jumlahDigit + " digit urut terakhir.";
				hasil.add(s);
				continue;
			}

			List<NimUrutanSuggestion> list = perPrefix.get(prefix);
			if (list == null) {
				list = new ArrayList<NimUrutanSuggestion>();
				perPrefix.put(prefix, list);
			}
			list.add(s);
			hasil.add(s);
		}

		for (Map.Entry<String, List<NimUrutanSuggestion>> entry : perPrefix.entrySet()) {
			final int digit = jumlahDigit;
			Collections.sort(entry.getValue(), new Comparator<NimUrutanSuggestion>() {
				@Override
				public int compare(NimUrutanSuggestion o1, NimUrutanSuggestion o2) {
					Long n1 = ambilSuffixNim(o1.nimLama, digit);
					Long n2 = ambilSuffixNim(o2.nimLama, digit);
					int c = n1.compareTo(n2);
					if (c != 0) {
						return c;
					}
					return o1.id.compareTo(o2.id);
				}
			});

			List<NimUrutanSuggestion> list = entry.getValue();
			for (int i = 0; i < list.size(); i++) {
				NimUrutanSuggestion s = list.get(i);
				s.nimSaran = entry.getKey() + padNomor(i + 1, jumlahDigit);
				if (s.nimSaran.equals(s.nimLama)) {
					s.aman = true;
					s.status = "Sudah sesuai urutan.";
				} else if (nimSaranBentrokDenganDataLain(session, s.nimSaran, uniqueIds)) {
					s.aman = false;
					s.status = "NIM saran sudah dipakai data lain di luar filter.";
				} else {
					s.aman = true;
					s.status = "Perlu dirapikan.";
				}
			}
		}

		Collections.sort(hasil, new Comparator<NimUrutanSuggestion>() {
			@Override
			public int compare(NimUrutanSuggestion o1, NimUrutanSuggestion o2) {
				if (o1.nimLama == null && o2.nimLama == null) {
					return 0;
				}
				if (o1.nimLama == null) {
					return 1;
				}
				if (o2.nimLama == null) {
					return -1;
				}
				return o1.nimLama.compareTo(o2.nimLama);
			}
		});
		return hasil;
	}

	private void replaceSaranUrutanNimDenganKonfirmasi(final List<NimUrutanSuggestion> pilihan,
			final org.zkoss.zul.Window win) throws Exception {
		if (pilihan == null || pilihan.isEmpty()) {
			MyMessageboxConfig.show("Belum ada NIM yang dipilih untuk di-apply.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		StringBuilder ringkas = new StringBuilder();
		int limit = Math.min(8, pilihan.size());
		for (int i = 0; i < limit; i++) {
			NimUrutanSuggestion s = pilihan.get(i);
			ringkas.append(s.nama == null ? "-" : s.nama).append(": ")
					.append(s.nimLama).append(" -> ").append(s.nimSaran).append("\n");
		}
		if (pilihan.size() > limit) {
			ringkas.append("... dan ").append(pilihan.size() - limit).append(" data lainnya.\n");
		}

		MyMessageboxConfig.show("Apply perubahan NIM berikut?\n\n" + ringkas.toString(),
				"Konfirmasi Apply NIM",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
				MyMessageboxConfig.QUESTION, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				int jawab = Integer.parseInt(event.getData().toString());
				if (jawab != MyMessageboxConfig.OK) {
					return;
				}
				replaceSaranUrutanNim(pilihan);
				win.detach();
				onSearchDefault(null);
				MyMessageboxConfig.show("Apply NIM selesai untuk " + pilihan.size() + " data.",
						"Berhasil", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			}
		});
	}

	private void replaceSaranUrutanNim(List<NimUrutanSuggestion> pilihan) throws Exception {
		Session session = HibernateUtil.openSession();
		try {
			session.beginTransaction();
			for (int i = 0; i < pilihan.size(); i++) {
				NimUrutanSuggestion s = pilihan.get(i);
				if (!s.aman || s.nimSaran == null || s.nimSaran.equals(s.nimLama)) {
					continue;
				}
				session.createSQLQuery("UPDATE biodata_calon_mahasiswa SET nim = :nim WHERE id = :id")
						.setParameter("nim", s.nimSaran)
						.setParameter("id", s.id)
						.executeUpdate();
				if (s.mahasiswaId != null) {
					session.createSQLQuery("UPDATE mahasiswa SET nim = :nim WHERE id = :id")
							.setParameter("nim", s.nimSaran)
							.setParameter("id", s.mahasiswaId)
							.executeUpdate();
				}
				ConstantValues.hapus(BiodataCalonMahasiswa.class.getName(), s.id);
				if (s.mahasiswaId != null) {
					ConstantValues.hapus(Mahasiswa.class.getName(), s.mahasiswaId);
				}
			}
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session.getTransaction() != null && session.getTransaction().isActive()) {
				try {
					session.getTransaction().rollback();
				} catch (Exception rb) {
					ais.common.ErrorAuditUtil.record(rb, "CetakRegistrasiAction replaceSaranUrutanNim rollback");
				}
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private boolean nimSaranBentrokDenganDataLain(Session session, String nimSaran, List<Long> idsFilter) {
		@SuppressWarnings("unchecked")
		List<Number> ids = session.createSQLQuery(
				"SELECT id FROM biodata_calon_mahasiswa WHERE nim = :nim")
				.setParameter("nim", nimSaran).list();
		for (int i = 0; i < ids.size(); i++) {
			Long id = Long.valueOf(ids.get(i).longValue());
			if (!idsFilter.contains(id)) {
				return true;
			}
		}
		return false;
	}

	private int getJumlahDigitUrutNim() {
		try {
			return Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4")
					.getNilai().trim());
		} catch (Exception e) {
			return 4;
		}
	}

	private String ambilPrefixNim(String nim, int jumlahDigit) {
		if (nim == null || nim.length() <= jumlahDigit) {
			return null;
		}
		String suffix = nim.substring(nim.length() - jumlahDigit);
		if (!isAngka(suffix)) {
			return null;
		}
		return nim.substring(0, nim.length() - jumlahDigit);
	}

	private static Long ambilSuffixNim(String nim, int jumlahDigit) {
		if (nim == null || nim.length() <= jumlahDigit) {
			return null;
		}
		String suffix = nim.substring(nim.length() - jumlahDigit);
		if (!isAngka(suffix)) {
			return null;
		}
		return Long.valueOf(suffix);
	}

	private static boolean isAngka(String nilai) {
		if (nilai == null || nilai.length() == 0) {
			return false;
		}
		for (int i = 0; i < nilai.length(); i++) {
			if (!Character.isDigit(nilai.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static String padNomor(int nomor, int jumlahDigit) {
		String nilai = String.valueOf(nomor);
		while (nilai.length() < jumlahDigit) {
			nilai = "0" + nilai;
		}
		return nilai;
	}

	private static String bersihkan(String nilai) {
		if (nilai == null) {
			return null;
		}
		nilai = nilai.trim();
		return nilai.length() == 0 ? null : nilai;
	}

	private String filterAnalisisNimRingkas() {
		StringBuilder sb = new StringBuilder();
		tambahFilterRingkas(sb, "Nama", searchnama == null ? null : searchnama.getValue());
		tambahFilterRingkas(sb, "No.Reg", searchnoreg == null ? null : searchnoreg.getValue());
		tambahFilterRingkas(sb, "No.Ujian", searchujian == null ? null : searchujian.getValue());
		tambahFilterRingkas(sb, "NIM", searchnim == null ? null : searchnim.getValue());
		tambahFilterRingkas(sb, "Tahun", nilaiComboRingkas(searchTahunAjaran));
		tambahFilterRingkas(sb, "Semester", nilaiComboRingkas(searchSemester));
		tambahFilterRingkas(sb, "Gelombang", nilaiComboRingkas(searchGelombang));
		tambahFilterRingkas(sb, "Jenjang", nilaiComboRingkas(searchJenjang));
		tambahFilterRingkas(sb, "Program", nilaiComboRingkas(searchProgram));
		tambahFilterRingkas(sb, "Prodi Lulus", nilaiComboRingkas(searchProdiLulus));
		tambahFilterRingkas(sb, "Status Awal", nilaiComboRingkas(searchStatusAwalMahasiswa));
		return sb.length() == 0 ? "semua data sesuai hak akses halaman" : sb.toString();
	}

	private static void tambahFilterRingkas(StringBuilder sb, String label, String nilai) {
		nilai = bersihkan(nilai);
		if (nilai == null || "Semua".equalsIgnoreCase(nilai) || "= Semua =".equalsIgnoreCase(nilai)
				|| "=Program=".equalsIgnoreCase(nilai)) {
			return;
		}
		if (sb.length() > 0) {
			sb.append("; ");
		}
		sb.append(label).append("=").append(nilai);
	}

	private static String nilaiComboRingkas(Combobox combo) {
		if (combo == null || combo.getSelectedItem() == null) {
			return null;
		}
		if (combo.getSelectedItem().getLabel() != null) {
			return combo.getSelectedItem().getLabel();
		}
		return combo.getValue();
	}

	public Criteria initCriteria(boolean order) {
		VerifikasiKelengkapanCalonMahasiswa verifikasiKelengkapanCalonMahasiswa = null;
		JenisKegiatan jenisKegiatan = null;
		Integer pilihan = null;

		if (searchSyarat != null) {
			Comboitem comboitem = searchSyarat.getSelectedItem();
			if (comboitem != null) {
				pilihan = (Integer) comboitem.getValue();
				if (comboitem.getAttribute("nilai") != null
						&& comboitem.getAttribute("nilai") instanceof JenisKegiatan) {
					jenisKegiatan = (JenisKegiatan) comboitem.getAttribute("nilai");
				} else {
					verifikasiKelengkapanCalonMahasiswa = (VerifikasiKelengkapanCalonMahasiswa) comboitem
							.getAttribute("nilai");
				}
			}
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(BiodataCalonMahasiswa.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(searchSemester.getSelectedItem() == null || searchSemester.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("semesterMulai", searchSemester.getSelectedItem().getValue()))
				.add(cariBlmMasukFeeder != null && cariBlmMasukFeeder.isChecked()
						? Restrictions.or(Restrictions.isNull("pinPassword"), Restrictions.eq("pinPassword", ""))
						: Restrictions.sqlRestriction("true"));

		if (searchkabkota != null && !searchkabkota.getValue().trim().isEmpty()) {
			criteria.createAlias("kotaCalon", "kotaCalon")
					.add(Restrictions.ilike("kotaCalon.nama", searchkabkota.getValue().trim(), MatchMode.ANYWHERE));
		}

		if (searchTanggalLahirDari != null && searchTanggalLahirDari.getValue() != null) {
			criteria.add(Restrictions.ge("tanggalLahir", searchTanggalLahirDari.getValue()));
		}
		if (searchTanggalLahirSampai != null && searchTanggalLahirSampai.getValue() != null) {
			criteria.add(Restrictions.le("tanggalLahir", searchTanggalLahirSampai.getValue()));
		}

		if (jenisKegiatan != null) {
			Criterion criterion = Restrictions.eq("jenisKegiatan", jenisKegiatan);
			if (order) {
				criteria = session.createCriteria(BuktiPembayaran.class).addOrder(Order.desc("biodataCalonMahasiswa"))
						.add(criterion).setProjection(Projections.groupProperty("biodataCalonMahasiswa.id"))
						.createCriteria("biodataCalonMahasiswa");
			} else {
				criteria = session.createCriteria(BuktiPembayaran.class).add(criterion)
						.setProjection(Projections.groupProperty("biodataCalonMahasiswa.id"))
						.createCriteria("biodataCalonMahasiswa");
			}
		} else if (verifikasiKelengkapanCalonMahasiswa != null && pilihan != null) {
			Criterion criterion = Restrictions.sqlRestriction("true");
			if (pilihan.equals(1)) {
				criterion = Restrictions.and(
						Restrictions.eq("verifikasiKelengkapanCalonMahasiswa", verifikasiKelengkapanCalonMahasiswa),
						Restrictions.eq("uploaded", true));
			} else if (pilihan.equals(2)) {
				criterion = Restrictions.and(
						Restrictions.eq("verifikasiKelengkapanCalonMahasiswa", verifikasiKelengkapanCalonMahasiswa),
						Restrictions.or(Restrictions.eq("uploaded", false), Restrictions.isNull("uploaded")));
			} else if (pilihan.equals(3)) {
				criterion = Restrictions.and(
						Restrictions.eq("verifikasiKelengkapanCalonMahasiswa", verifikasiKelengkapanCalonMahasiswa),
						Restrictions.eq("verified", true));
			} else if (pilihan.equals(4)) {
				criterion = Restrictions.and(
						Restrictions.eq("verifikasiKelengkapanCalonMahasiswa", verifikasiKelengkapanCalonMahasiswa),
						Restrictions.eq("verified", false));
			}

			if (order) {
				criteria = session.createCriteria(BiodataCalonMahasiswaPunyaVerifikasiBerkas.class)
						.addOrder(Order.desc("biodataCalonMahasiswa")).add(criterion)
						.setProjection(Projections.groupProperty("biodataCalonMahasiswa.id"))
						.createCriteria("biodataCalonMahasiswa");
			} else {
				criteria = session.createCriteria(BiodataCalonMahasiswaPunyaVerifikasiBerkas.class).add(criterion)
						.setProjection(Projections.groupProperty("biodataCalonMahasiswa.id"))
						.createCriteria("biodataCalonMahasiswa");
			}
		} else {
			criteria.setProjection(Projections.property("id"));
		}

		Jurusan prodiPilihan1 = (Jurusan) (searchProdiPilihan1.getSelectedItem() == null ? null
				: searchProdiPilihan1.getSelectedItem().getValue());
		Jurusan prodiPilihan2 = (Jurusan) (searchProdiPilihan2.getSelectedItem() == null ? null
				: searchProdiPilihan2.getSelectedItem().getValue());
		Jurusan prodiPilihan3 = (Jurusan) (searchProdiPilihan3.getSelectedItem() == null ? null
				: searchProdiPilihan3.getSelectedItem().getValue());

		Jurusan prodiLulus = (Jurusan) (searchProdiLulus.getSelectedItem() == null ? null
				: searchProdiLulus.getSelectedItem().getValue());
		Jenjang jenjang = (Jenjang) (searchJenjang.getSelectedItem() == null ? null
				: searchJenjang.getSelectedItem().getValue());

		criteria.add(searchProgram.getSelectedItem() == null || searchProgram.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("program", searchProgram.getSelectedItem().getValue()))
				.createAlias("gelombangPendaftaran", "gelombangPendaftaran", Criteria.LEFT_JOIN)
				.add(selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.eq("gelombangPendaftaran.perguruanTinggi", selectedPerguruanTinggi),
								Restrictions.isNull("gelombangPendaftaran.perguruanTinggi")))
				.add(prodiLulus == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("prodiLulus", prodiLulus))
				.add(jenjang == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jenjang", jenjang))
				.add(mengisiFormTambahan.isChecked() ? Restrictions.ne("parameterTambahanInds", "")
						: Restrictions.sqlRestriction("true"))
				.add(mundur.isChecked() ? Restrictions.eq("mundur", true) : Restrictions.sqlRestriction("true"))
				.add(ditolak.isChecked() ? Restrictions.eq("ditolak", true) : Restrictions.sqlRestriction("true"))
				.add(diterima.isChecked() ? Restrictions.isNotNull("prodiLulus") : Restrictions.sqlRestriction("true"))
				.add(blmditerima.isChecked() ? Restrictions.isNull("prodiLulus") : Restrictions.sqlRestriction("true"))
				.add(dptNoUjian.isChecked() ? Restrictions.isNotNull("noUjian") : Restrictions.sqlRestriction("true"))
				.add(blmNoUjian.isChecked() ? Restrictions.isNull("noUjian") : Restrictions.sqlRestriction("true"))
				.add(telahLogin.isChecked() ? Restrictions.eq("telahLogin", true) : Restrictions.sqlRestriction("true"))
				.add(tampilkanYgSudahdapatNIM.isChecked() ? Restrictions.isNotNull("mahasiswa")
						: Restrictions.sqlRestriction("true"))
				.add(tampilkanYgBelumdapatNIM.isChecked() ? Restrictions.isNull("mahasiswa")
						: Restrictions.sqlRestriction("true"))
				.add(searchJenisSekolahMahasiswaBaru.getSelectedItem() == null
						|| searchJenisSekolahMahasiswaBaru.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jenisSekolah",
										searchJenisSekolahMahasiswaBaru.getSelectedItem().getValue()))
				.add(searchJurusanSekolahMahasiswaBaru.getSelectedItem() == null
						|| searchJurusanSekolahMahasiswaBaru.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusanSekolah", searchJurusanSekolahMahasiswaBaru, false))
				.add(searchPaket.getSelectedItem() == null || searchPaket.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("paket", searchPaket.getSelectedItem().getValue()));

		Criterion criterion = (prodiPilihan1 == null ? Restrictions.sqlRestriction("true")
				: Restrictions.eq("prodi1", prodiPilihan1));
		criterion = Restrictions.and(criterion,
				prodiPilihan2 == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("prodi2", prodiPilihan2));
		criterion = Restrictions.and(criterion,
				prodiPilihan3 == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("prodi3", prodiPilihan3));

		criteria.add(criterion)
				.add(searchStatusAwalMahasiswa.getSelectedItem() == null
						|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("statusAwalMahasiswa",
										searchStatusAwalMahasiswa.getSelectedItem().getValue()))
				.add(searchJenisSeleksi.getSelectedItem() == null
						|| searchJenisSeleksi.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jenisSeleksi", searchJenisSeleksi.getSelectedItem().getValue()))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchsekolah.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("asalSma", searchsekolah.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
				.add(searchGelombang.getSelectedItem() == null || searchGelombang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("gelombangPendaftaran", searchGelombang.getSelectedItem().getValue()))
				.add(searchnoreg.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("noRegistrasi", searchnoreg.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchujian.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("noUjian", searchujian.getValue().trim(), MatchMode.ANYWHERE));

		if (afiliasiCalonMahasiswaData != null) {
			criteria.add(Restrictions.eq("afiliasiCalonMahasiswa", afiliasiCalonMahasiswaData));
			if (searchAfiliasi != null) {
				searchAfiliasi.setValue(afiliasiCalonMahasiswaData.getNama());
				searchAfiliasi.setDisabled(true);
			}
		} else if (searchAfiliasi != null && !searchAfiliasi.getValue().trim().isEmpty()) {
			criteria.createAlias("afiliasiCalonMahasiswa", "afiliasiCalonMahasiswa", Criteria.LEFT_JOIN)
					.createAlias("afiliasiMahasiswa", "afiliasiMahasiswa", Criteria.LEFT_JOIN)
					.createAlias("afiliasiPegawai", "afiliasiPegawai", Criteria.LEFT_JOIN)
					.add(Restrictions.or(
							Restrictions.ilike("afiliasiCalonMahasiswa.nama", searchAfiliasi.getValue().trim(),
									MatchMode.ANYWHERE),
							Restrictions.or(
									Restrictions.ilike("afiliasiPegawai.nama", searchAfiliasi.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.ilike("afiliasiMahasiswa.nama", searchAfiliasi.getValue().trim(),
											MatchMode.ANYWHERE))));
		}

		if (searchnim != null && !searchnim.getValue().trim().isEmpty()) {
			criteria.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
					.add(Restrictions.or(
							Restrictions.ilike("nim", searchnim.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.ilike("mahasiswa.nim", searchnim.getValue().trim(), MatchMode.ANYWHERE)));
		}

		if (order && verifikasiKelengkapanCalonMahasiswa == null && jenisKegiatan == null) {
			criteria.addOrder(Order.desc("id"));
		}

		if (tampilkanYgSudahLunasDaftarUlang.isChecked() || tampilkanYgBelumLunasDaftarUlang.isChecked()
				|| tampilkanTidakAdaTagihanDaftarUlang.isChecked() || tampilkanYgSudahBayarDaftarUlang.isChecked()
				|| tampilkanYgBelumBayarDaftarUlang.isChecked()) {

			criteria

					.createAlias("pembayaranDaftarUlang", "pembayaranDaftarUlang", Criteria.LEFT_JOIN)

					.add(tampilkanYgSudahLunasDaftarUlang.isChecked()
							? Restrictions.eq("pembayaranDaftarUlang.lunas", true)
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgBelumLunasDaftarUlang.isChecked()
							? Restrictions.eq("pembayaranDaftarUlang.lunas", false)
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgSudahBayarDaftarUlang.isChecked()
							? Restrictions.gt("pembayaranDaftarUlang.dibayar", 0.1)
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgBelumBayarDaftarUlang.isChecked()
							? Restrictions.and(Restrictions.isNotNull("pembayaranDaftarUlang"),
									Restrictions.lt("pembayaranDaftarUlang.dibayar", 0.1))
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanTidakAdaTagihanDaftarUlang.isChecked()
							? Restrictions.or(Restrictions.isNull("pembayaranDaftarUlang"),
									Restrictions.and(Restrictions.lt("pembayaranDaftarUlang.tagihan", 0.1),
											Restrictions.lt("pembayaranDaftarUlang.dibayar", 0.1)))
							: Restrictions.sqlRestriction("true"));

		}

		if (tampilkanYgSudahBayar.isChecked() || tampilkanYgBelumBayar.isChecked()
				|| tampilkanTidakAdaTagihanReg.isChecked()) {

			criteria

					.createAlias("pembayaranRegistrasi", "pembayaranRegistrasi", Criteria.LEFT_JOIN)

					.add(tampilkanYgSudahBayar.isChecked() ? Restrictions.gt("pembayaranRegistrasi.dibayar", 0.1)
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanYgBelumBayar.isChecked()
							? Restrictions.and(Restrictions.isNotNull("pembayaranRegistrasi"),
									Restrictions.lt("pembayaranRegistrasi.dibayar", 0.1))
							: Restrictions.sqlRestriction("true"))

					.add(tampilkanTidakAdaTagihanReg.isChecked()
							? Restrictions.or(Restrictions.isNull("pembayaranRegistrasi"),
									Restrictions.and(Restrictions.lt("pembayaranRegistrasi.tagihan", 0.1),
											Restrictions.lt("pembayaranRegistrasi.dibayar", 0.1)))
							: Restrictions.sqlRestriction("true"));
		}

		if (selectedPerguruanTinggi != null) {
			criteria.add(Restrictions.or(Restrictions.isNull("gelombangPendaftaran.perguruanTinggi"),
					Restrictions.eq("gelombangPendaftaran.perguruanTinggi", selectedPerguruanTinggi)));
		}

		if (blmVerifBekas != null && blmVerifBekas.isChecked()) {
			criteria.add(Restrictions.sqlRestriction(
				"EXISTS (SELECT 1 FROM public.biodata_calon_mahasiswa_punya_verifikasi_berkas pvb"
				+ " WHERE pvb.biodata_calon_mahasiswa = {alias}.id"
				+ " AND (pvb.verified = false OR pvb.verified IS NULL))"));
		}

		return criteria;
	}

	@SuppressWarnings({ "unchecked" })
	public void onSearchDefault(Event event) {
		System.out.println(" ==== 1 onSearchDefault ===");
		Common.initPaging(initCriteria(false), paging);

		List<Long> calonMahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		// OPTIMASI: Menghapus duplikasi dengan mempertahankan urutan tanpa menggunakan
		// stream() Java 8
		// Cara ini murni kompatibel dengan Java 1.6 & 1.7 dan sangat efisien secara
		// memori.
		List<Long> newcalonMahasiswa = null;
		if (calonMahasiswa != null) {
			newcalonMahasiswa = new ArrayList<Long>(new java.util.LinkedHashSet<Long>(calonMahasiswa));
			// Tombol Refresh harus benar-benar merefresh data, bukan sekadar merender ulang
			// object lama dari MemoryCacheUtil. Batasi invalidasi pada baris halaman aktif.
			for (Long calonId : newcalonMahasiswa) {
				ConstantValues.hapus(BiodataCalonMahasiswa.class.getName(), calonId);
			}
		}

		ListModel strset = new SimpleListModel(newcalonMahasiswa);
		grid.setRowRenderer(new CalonMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

		System.out.println(" ==== 2 onSearchDefault ===");
		calonMahasiswa = null;
	}

}
