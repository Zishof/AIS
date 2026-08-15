package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.KonfigurasiTampilanSiswaAction;
import ais.action.master.SertifikatAction;
import ais.action.master.dashboard.admin.DashboardKegiatanKesiswaan;
import ais.action.master.helper.AmbilDataKecamatanBanbox;
import ais.action.master.helper.AmbilDataNegaraBanbox;
import ais.action.master.helper.MainHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.RevisiSiswaHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.AmbilDataKelasSiswaBanbox;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.Report;
import ais.action.report.format1.sekolah.LaporanKartuSiswa;
import ais.action.report.format1.sekolah.LaporanRaporSiswa;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Agama;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jenjang;
import ais.database.model.Konfigurasi;
import ais.database.model.LogLogin;
import ais.database.model.Mahasiswa;
import ais.database.model.Negara;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.Wilayah;
import ais.database.model.file.FotoSiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.AlatTransportasiSiswa;
import ais.database.model.sekolah.AsramaSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JenisSekolah;
import ais.database.model.sekolah.JenisTinggalSiswa;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.PekerjaanOrtuSiswa;
import ais.database.model.sekolah.PendidikanOrangTuaSiswa;
import ais.database.model.sekolah.PenghasilanOrangTuaSiswa;
import ais.database.model.sekolah.PenjurusanSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.StatusAwalSiswa;
import ais.database.model.sekolah.StatusKeluarSiswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupConfig;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecilBoldHijau;
import ais.ui.util.MyMessageboxConfig;

import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class SiswaAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, DataInitDefault {

	protected boolean alumni = false;

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Textbox searchkode;
	private Textbox searchnama;
	private AmbilDataKelasSiswaBanbox searchkelas;
	private Decimalbox searchtahunMulai;
	private Decimalbox searchtahunSampai;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Combobox searchstatuskeluar;
	private Combobox searchSudahKeluar;
	private Combobox searchPunyaKelas;
	private Combobox searchPenjurusan;
	private Combobox searchstatusawal;
	protected AmbilDataGuruBanbox searchguruPembina;
	protected AmbilDataGuruBanbox searchguruBk;
	private Checkbox searchaktif;

	private Combobox searchkelamin;
	private Combobox searchagama;
	private Textbox searchnamaortu;
	private Textbox searchnamatelp;
	private Textbox searchnamaemail;

	private Textbox namaSiswa;
	private Textbox nomorInduk;
	private Textbox nomorIndukSantri;
	private Textbox nomorIndukNasional;
	private Combobox sekolah;
	private Combobox agama;
	private Combobox statusAwalSiswa;
	private Textbox alamatEmail;
//	private Textbox alamatOrangTua;
	private Textbox alamatSiswa;
	private Textbox alamatWali;
	private Intbox anakKe;
	private Intbox dariAnakKe;
	private Intbox diterimaDiSekolahIniDiKelas;
	private Combobox jenisKelamin;
	private Textbox namaAyah;
	private Textbox namaIbu;
	private Textbox namaWali;
	private MyDatebox padaTanggal;
	private Combobox pekerjaanAyah;
	private Combobox pekerjaanIbu;
	private Combobox pekerjaanWali;
	private Textbox sekolahAsal;
	private Combobox statusDalamKeluarga;
	private Intbox tahunMasuk;
	private Intbox bulanMasuk;
	private MyDatebox tanggalLahir;
//	private Textbox teleponOrangTua;
	private Textbox teleponSiswa;
	private Textbox teleponWali;
	private Textbox tempatLahir;
	private Combobox bahasa;
	private MyDoublebox berat;
	private Textbox golonganDarah;
	private Textbox hobby;
	private Textbox hp1ayah;
	private Textbox hp1ibu;
	private Textbox hp2ayah;
	private Textbox hp2ibu;
	private Textbox hp3ayah;
	private Textbox hp3ibu;
	private Intbox jumlahSaudaraKandung;
	private Intbox jumlahSaudaraTiri;
	private Combobox kewarganegaraan;
	private Combobox kondisiSiswa;
	private Textbox panggilan;
	private Combobox pendidikanAyah;
	private Combobox pendidikanIbu;
	private Combobox penghasilanAyah;
	private Combobox penghasilanIbu;
	private Textbox riwayatPenyakit;
	private Combobox statusSiswa;
	private MyDatebox tanggalLahirAyah;
	private MyDatebox tanggalLahirIbu;
	private Textbox tempatLahirAyah;
	private Textbox tempatLahirIbu;
	private MyDoublebox tinggi;
	private Combobox pendidikanWali;
	private Combobox penghasilanWali;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Siswa siswa;
	private MyToolbarbuttonConfig add;
	private AmbilDataNegaraBanbox negara;
	private Textbox password;
	private Combobox yayasan;
	private Textbox tempatLahirWali;
	private MyDatebox tanggalLahirWali;
	private Textbox hp1wali;
	private Textbox hp2wali;
	private Textbox hp3wali;
	protected FotoSiswa fotoSiswa;

	private MyToolbarbuttonConfig downloadRfid;
	private MyToolbarbuttonConfig uploadRfid;

	public void onUploadRfid(Event event) throws Exception {

		ForwardEvent forwardEvent = (ForwardEvent) event;
		Media media = ((UploadEvent) forwardEvent.getOrigin()).getMedia();
		if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
			return;
		if (media.getName().toLowerCase().endsWith("xlsx")) {

			InputStream inputStream = media.getStreamData();
			// System.out.println("media = " + media);
			File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
			// System.out.println("file = " + file.getAbsolutePath());
			file.getParentFile().mkdirs();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			int c;
			while ((c = inputStream.read()) != -1) {
				fileOutputStream.write(c);
			}
			fileOutputStream.close();
			inputStream.close();
			XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
			XSSFSheet sheet = workbook.getSheetAt(0);
			final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload RFID Siswa");

			for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
				Session session = HibernateUtil.currentNativeSession();
				String id = null;
				String rfid = null;
				try {

					id = Common.getCellContent(Common.getCell(sheet, 0, i));
					rfid = Common.getCellContent(Common.getCell(sheet, 1, i));

					if (id == null || id.trim().isEmpty()) {
						continue;
					}

					Siswa siswa = (Siswa) ConstantValues.ambil(Siswa.class.getName(), Long.parseLong(rfid));
					System.out.println("id = " + id + " rfid = " + rfid + ", siswa = " + siswa);
					if (siswa != null) {
						session.refresh(siswa);
						siswa.setIdfinger(rfid);
						session.getTransaction().begin();
						session.update(siswa);
						session.getTransaction().commit();
						report.sukses(i, id + " | RFID=" + rfid, "idfinger diperbarui");
					} else {
						report.gagal(i, id + " | RFID=" + rfid, "Siswa tidak ditemukan", "Pastikan kolom ID mengacu pada ID Siswa yang valid");
					}

				} catch (Exception e) {
					report.gagal(i, (id != null ? id : "baris-" + i) + " | RFID=" + (rfid != null ? rfid : ""), e, "Periksa format NIS dan nilai RFID");
					session.getTransaction().rollback();
					Common.tampilErrorJikaAdmin(e);
					// Common.tampilErrorJikaAdmin(e);
				}

				HibernateUtil.closeSession();
			}

			try {
				Filedownload.save(report.simpanLaporan(), "text/plain");
			} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) SiswaAction laporan rfid"); }
			MyMessageboxConfig.show(report.getRingkasan(), "Pemberitahuan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);

		} else {
			MyMessageboxConfig.show(
					"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
							+ media,
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
		}
	}

	@SuppressWarnings("unchecked")
	public void onDownloadRfid(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				ProjectionList projectionList = Projections.projectionList();
				projectionList.add(Projections.property("id"));
				projectionList.add(Projections.property("idfinger"));
				projectionList.add(Projections.property("nama"));

				List<Object[]> objects = initCriteria(true).setProjection(projectionList).list();

				final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/idfinger_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

				File file;
				(file = new File(filename)).createNewFile();

				XSSFWorkbook workbook = new XSSFWorkbook();
				XSSFSheet sheet = workbook.createSheet("DATA");
				sheet.setDefaultColumnWidth(20);
				int rowIndex = 0;

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("ID");
				rowhead.createCell(1).setCellValue("ID Finger/RFID");
				rowhead.createCell(2).setCellValue("Nama");
				rowIndex = 1;
				for (Object[] siswa : objects) {
					try {
						XSSFRow row = sheet.createRow(rowIndex);
						row.createCell(0).setCellValue(siswa[0] + "");
						row.createCell(1).setCellValue(siswa[1] + "");
						row.createCell(2).setCellValue(siswa[2] + "");
						rowIndex++;
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

				System.out.println("Your excel file has been generated! ");

				Filedownload.save(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

			}
		}, "Download ...");

	}

	private MyToolbarbuttonConfig uploadPassword;

	private Textbox alamatAyah, alamatIbu, prestasiSiswa1, prestasiSiswa2, prestasiSiswa3, nik, kk, waAyah, waIbu,
			waWali;

	private Tabpanel manajemenKartuSiswa;

	public void onKartuSiswa(Event event) {
		if (manajemenKartuSiswa.getChildren().size() == 0) {
			LaporanKartuSiswa laporan = new LaporanKartuSiswa();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(manajemenKartuSiswa);
		}
	}

	private Tabpanel manajemenFormTambahan;

	public void onFormTambahan(Event event) {
		if (manajemenFormTambahan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenFormTambahan);
			MyInclude iframe = new MyInclude("/pages/master/konfigurasi_siswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel manajemenKelas;

	public void onManajemenKelas(Event event) {
		if (manajemenKelas.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenKelas);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/kelas_siswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel kegiatanSiswaan;

	public void onKegiatanKesiswaan(Event event) {
		if (kegiatanSiswaan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kegiatanSiswaan);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/kegiatan_siswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel transkripAkademik;

	public void onTampilTranskripAkademik(Event event) {
		if (transkripAkademik.getChildren().size() == 0) {
			LaporanRaporSiswa window = new LaporanRaporSiswa();
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(transkripAkademik);

		}
	}

	private Tabpanel statistik;

	public void onStatistik(Event event) {
		if (statistik.getChildren().size() == 0) {
			ais.action.master.dashboard.sekolah.DasboardSiswa dasboard =
					new ais.action.master.dashboard.sekolah.DasboardSiswa();
			ais.ui.util.BaseDasbordPortal.mountWrapped(
					dasboard, statistik,
					"Statistik Siswa",
					"Gambaran sebaran siswa per sekolah, angkatan, jenis kelamin, dan status keaktifan.");
		}
	}

	private Tabpanel manajemenAsrama;
	private Combobox penjurusanSekolah;
	private EventListener eventListener = null;
	private Combobox statusKeluar;
//	private AmbilDataGuruBanbox guruPembina;
	private Decimalbox tahunLulus;
	private Textbox rt;
	private Textbox rw;
	private Textbox dusun;
	private Textbox kelurahan;
	private AmbilDataKecamatanBanbox kecamatan;
	private Textbox kodePos;
	private Combobox jenisTinggal;
	private Combobox alatTransportasi;
	private Textbox hp;
	private Textbox skhun;
	private MyCheckboxConfig penerimaKps;
	private Textbox noKps;
	private Textbox noPesertaUjianNasional;
	private Textbox noSeriIjazah;
	private MyCheckboxConfig penerimaKip;
	private Textbox nomorKip;
	private Textbox namaDiKip;
	private Textbox nomorKks;
	private Textbox noRegistrasiAktaLahir;
	private Textbox bank;
	private Textbox nomorRekeningBank;
	private Textbox rekeningAtasNama;
	private MyCheckboxConfig layakPip;
	private Textbox alasanLayakPip;
	private Textbox kebutuhanKhusus;
	private Textbox lintang;
	private Textbox bujur;
	private Textbox lingkarKepala;
	private Textbox jarakRumahKeSekolah;
	private Combobox asrama;
	private Textbox nikAyah;
	private Textbox nikIbu;
	private Textbox nikWali;
//	private AmbilDataGuruBanbox guruBk;
	protected LampiranLain ttd;
	private Textbox idfinger;
	private Textbox namaAr;
	private Textbox namaCh;

	private PerguruanTinggi perguruanTinggi;

	protected TreeSet<Long> selectedKelasLesSiswa = null;
	protected TreeSet<Long> hapusKelasLesSiswa = null;

	private Combobox gelombangPendaftaran;

	private Textbox noSeriTranskrip;

	private MyDatebox tanggalLulus;

	public void onManajemenAsrama(Event event) {
		if (manajemenAsrama.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenAsrama);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/asrama_siswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel manajemenKelompokStatusKeluarSiswa;

	public void onManajemenKelompokStatusKeluarSiswa(Event event) {
		if (manajemenKelompokStatusKeluarSiswa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenKelompokStatusKeluarSiswa);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/kelompok_status_keluar_siswa.zul");
			iframe.setParent(window);
		}
	}

	public void onUploadPasswordSiswa(Event event) throws Exception {

		ForwardEvent forwardEvent = (ForwardEvent) event;
		Media media = ((UploadEvent) forwardEvent.getOrigin()).getMedia();
		if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
			return;
		if (media.getName().toLowerCase().endsWith("xlsx")) {

			InputStream inputStream = media.getStreamData();
			// System.out.println("media = " + media);
			File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
			// System.out.println("file = " + file.getAbsolutePath());
			file.getParentFile().mkdirs();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			int c;
			while ((c = inputStream.read()) != -1) {
				fileOutputStream.write(c);
			}
			fileOutputStream.close();
			inputStream.close();
			XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
			XSSFSheet sheet = workbook.getSheetAt(0);
			final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Password Siswa");

			for (int i = 1; i < (sheet.getLastRowNum() + 1); i++) {
				Session session = HibernateUtil.currentNativeSession();
				String nim = null;
				try {

					nim = Common.getCellContent(Common.getCell(sheet, 0, i));
					String password = Common.getCellContent(Common.getCell(sheet, 1, i));

					if (nim == null) {
						continue;
					}

					Siswa siswa = (Siswa)

					ConstantValues.simpleObject(session.createCriteria(Siswa.class)
							.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
							.add(Restrictions.isNotNull("sekolah")).add(Restrictions
									.or(Restrictions.eq("nomorIndukNasional", nim), Restrictions.eq("nomorInduk", nim)))
							.setMaxResults(1), Siswa.class);
					System.out.println("nim = " + nim + ", siswa = " + siswa); // KEAMANAN: password TIDAK dicetak ke log
					if (siswa != null) {
						siswa.setPass(Common.desEncrypter.get().encrypt(password));
						session.getTransaction().begin();
						session.update(siswa);
						session.getTransaction().commit();
						report.sukses(i, nim, "password diperbarui");
					} else {
						report.gagal(i, nim, "Siswa tidak ditemukan", "Pastikan NIS/NISN di kolom A sesuai data siswa");
					}

				} catch (Exception e) {
					report.gagal(i, nim != null ? nim : "baris-" + i, e, "Periksa format NIS/NISN");
					session.getTransaction().rollback();
					Common.tampilErrorJikaAdmin(e);
					// Common.tampilErrorJikaAdmin(e);
				}

				HibernateUtil.closeSession();
			}

			try {
				Filedownload.save(report.simpanLaporan(), "text/plain");
			} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) SiswaAction laporan password"); }
			MyMessageboxConfig.show(report.getRingkasan(), "Pemberitahuan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);

		} else {
			MyMessageboxConfig.show(
					"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
							+ media,
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
		}
	}

	public static String[] contents = new String[] { "id", "nomorInduk", "namaSiswa", "namaAr", "namaCh", "panggilan",
			"jenisKelamin", "nomorIndukNasional", "nomorIndukSantri", "idfinger", "tahunMasuk", "bulanMasuk",
			"diterimaDiSekolahIniDiKelas", "padaTanggal", "sekolah", "kelas", "penjurusanSekolah", "tempatLahir",
			"tanggalLahir", "nik", "agama", "bahasa", "statusSiswa", "statusAwalSiswa", "statusKeluar", "alamatSiswa",
			"rt", "rw", "dusun", "kelurahan", "kecamatan", "kodePos", "kewarganegaraan", "negara", "jenisTinggal",
			"asrama", "alatTransportasi", "teleponSiswa", "hp", "alamatEmail", "skhun", "penerimaKps", "noKps",
			"namaAyah", "tempatLahirAyah", "tanggalLahirAyah", "alamatAyah", "pendidikanAyah", "pekerjaanAyah",
			"penghasilanAyah", "nikAyah", "hp1ayah", "hp2ayah", "hp3ayah", "waAyah", "namaIbu", "tempatLahirIbu",
			"tanggalLahirIbu", "alamatIbu", "pendidikanIbu", "pekerjaanIbu", "penghasilanIbu", "nikIbu", "hp1ibu",
			"hp2ibu", "hp3ibu", "waIbu", "namaWali", "tempatLahirWali", "tanggalLahirWali", "alamatWali",
			"pendidikanWali", "pekerjaanWali", "penghasilanWali", "nikWali", "teleponWali", "waWali",
			"noPesertaUjianNasional", "noSeriIjazah", "noSeriTranskrip", "penerimaKip", "nomorKip", "namaDiKip",
			"nomorKks", "noRegistrasiAktaLahir", "bank", "nomorRekeningBank", "rekeningAtasNama", "layakPip",
			"alasanLayakPip", "kebutuhanKhusus", "kondisiSiswa", "sekolahAsal", "anakKe", "dariAnakKe", "lintang",
			"bujur", "kk", "berat", "tinggi", "lingkarKepala", "golonganDarah", "riwayatPenyakit",
			"jumlahSaudaraKandung", "jumlahSaudaraTiri", "statusDalamKeluarga", "jarakRumahKeSekolah", "hobby",
			"prestasiSiswa1", "prestasiSiswa2", "prestasiSiswa3", "keterangan", "tahunLulus", "tanggalLulus",
			"gelombangPendaftaranPsb", "ubahPasword" };

	public final static String[] DATA = contents;

	public final static String[] DEFAULT_TIDAK_AKTIF = new String[] { "nomorIndukSantri", "ubahPasword" };

	public final static String[] DEFAULT_TIDAK_WAJIB = new String[] { "panggilan", "kelas", "namaAr", "namaCh",
			"sekolah", "kelas", "asrama", "guruPembina", "alamatOrangTua", "penjurusanSekolah", "agama", "tahunMasuk",
			"anakKe", "dariAnakKe", "nomorInduk", "nomorIndukNasional", "idfinger", "jumlahSaudaraKandung",
			"jumlahSaudaraTiri", "kewarganegaraan", "negara", "padaTanggal", "alamatEmail", "teleponSiswa",
			"pekerjaanAyah", "pendidikanAyah", "penghasilanAyah", "tempatLahirAyah", "tanggalLahirAyah", "hp1ayah",
			"hp2ayah", "hp3ayah", "pekerjaanIbu", "pendidikanIbu", "penghasilanIbu", "tempatLahirIbu",
			"tanggalLahirIbu", "hp1ibu", "hp2ibu", "hp3ibu", "namaWali", "pekerjaanWali", "pendidikanWali",
			"penghasilanWali", "tempatLahirWali", "tanggalLahirWali", "hp1wali", "hp2wali", "hp3wali", "alamatSiswa",
			"dusunCalon", "rt", "rw", "kodePos", "kelurahanCalon", "kecamatanCalon", "propinsiCalon", "kotaCalon",
			"alamatWali", "teleponWali", "sekolahAsal", "alamatSekolahAsal", "statusDalamKeluarga", "bahasa", "berat",
			"tinggi", "riwayatPenyakit", "golonganDarah", "hobby", "keterangan", "statusSiswa", "alamatAyah",
			"alamatIbu", "prestasiSiswa1", "prestasiSiswa2", "prestasiSiswa3", "nik", "kk", "waAyah", "waIbu", "waWali",
			"guruPembina",

			"rt", "rw", "dusun", "kelurahan", "kecamatan", "kodePos", "jenisTinggal", "alatTransportasi", "hp", "skhun",
			"penerimaKps", "noKps", "noPesertaUjianNasional", "noSeriIjazah", "noSeriTranskrip", "penerimaKip",
			"nomorKip", "namaDiKip", "nomorKks", "noRegistrasiAktaLahir", "bank", "nomorRekeningBank",
			"rekeningAtasNama", "layakPip", "alasanLayakPip", "kebutuhanKhusus", "lintang", "bujur", "lingkarKepala",
			"jarakRumahKeSekolah", "statusKeluar", "nikAyah", "nikIbu", "nikWali", "teleponOrangTua", "guruBk",
			"tahunLulus", "tanggalLulus", "gelombangPendaftaranPsb" };

	public final static String[] DATA_DESC = new String[] { "id", "Nomor Induk Peserta Didik (NIPD)", "Nama Lengkap",
			"Nama (Aksara Arab)", "Nama (Aksara Tionghoa)", "Nama Panggilan", "Jenis Kelamin",
			"Nomor Induk Siswa Nasional", "Nomor Induk Santri", "Kode Finger", "Tahun Masuk", "Bulan Masuk",
			"Diterima di kelas/tingkat", "Pada Tanggal", "Sekolah", "kelas", "Penjurusan Sekolah", "Tempat Lahir",
			"Tanggal Lahir", "NIK", "Agama", "Bahasa", "Status Siswa", "Status Awal Siswa", "Status Keluar",
			"Alamat Siswa", "RT", "RW", "Dusun", "Kelurahan", "Kecamatan", "Kode Pos", "Kewarganegaraan", "Negara",
			"Jenis Tinggal", "Asrama", "Alat Transportasi", "Telepon (atau HP) / No. WA", "HP", "Alamat Email", "SKHUN",
			"Penerima KPS", "No. KPS", "Nama Ayah", "Tempat Lahir Ayah", "Tanggal Lahir Ayah", "Alamat Ayah",
			"Pendidikan Ayah", "Pekerjaan Ayah", "Penghasilan Ayah", "NIK Ayah", "HP Ayah", "HP Ayah", "HP Ayah",
			"WA Ayah", "Nama Ibu", "Tempat Lahir Ibu", "Tanggal Lahir Ibu", "Alamat Ibu", "Pendidikan Ibu",
			"Pekerjaan Ibu", "Penghasilan Ibu", "NIK Ibu", "HP Ibu", "HP Ibu", "HP Ibu", "WA Ibu", "Nama Wali",
			"Tempat Lahir Wali", "Tanggal Lahir Wali", "Alamat Wali", "Pendidikan Wali", "Pekerjaan Wali",
			"Penghasilan Wali", "NIK Wali", "Telepon Wali", "WA Wali", "No Peserta Ujian Nasional", "No Seri Ijazah",
			"No Seri Transkrip", "Penerima KIP", "Nomor KIP", "Nama di KIP", "Nomor KKS", "No Registrasi Akta Lahir",
			"Bank", "Nomor Rekening Bank", "Rekening Atas Nama", "Layak PIP (usulan dari sekolah)", "Alasan Layak PIP",
			"Kebutuhan Khusus", "Kondisi Siswa", "Asal Pendidikan Sebelumnya", "Anak Ke", "Dari Ke", "Lintang", "Bujur",
			"KK", "Berat", "Tinggi", "Lingkar Kepala", "Golongan Darah", "Riwayat Penyakit", "Jumlah Saudara Kandung",
			"Jumlah Saudara Tiri", "Status Dalam Keluarga", "Jarak Rumah ke Sekolah (KM)", "Hobby", "Prestasi Siswa I",
			"Prestasi Siswa II", "Prestasi Siswa III", "Keterangan", "Tahun Lulus", "Tanggal Lulus", "Jalur Masuk",
			"Ubah Pasword" };

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

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		if (alumni) {
			add.setVisible(false);
		} else {
			if (add != null) {
			add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
			add.setTooltiptext("Tambah");
			}
		}
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		// Sembunyikan tombol "Upload Foto (NIM)" bila hak akses (baca+ubah+hapus) tak lengkap.
		ais.common.helper.UploadFotoMassalHelper.terapkanGateTombolUpload(self);
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Tbmuser tbmuser = Common.getCurrentUser();
		boolean merupakanAdmin = (tbmuser != null && tbmuser != null && tbmuser.hakAkses() != null
				&& tbmuser.hakAkses() != null && tbmuser != null && tbmuser.hakAkses() != null
				&& tbmuser.hakAkses().getRoleId() != null && (tbmuser != null && tbmuser.hakAkses() != null
						&& Common.getCurrentUser().hakAkses().getRoleId().equalsIgnoreCase(Tbmrole.ADMINISTRATOR)));
		if (uploadRfid != null) {
			uploadRfid.setVisible(Common.bolehKonfigurasi("aktifkan_upload_rfid_siswa", Konfigurasi.TIDAK_AKTIF) && (add != null && add.isVisible()) && edit && merupakanAdmin);
			downloadRfid.setVisible(Common.bolehKonfigurasi("aktifkan_download_rfid_siswa", Konfigurasi.TIDAK_AKTIF) && (add != null && add.isVisible()) && edit && merupakanAdmin);
		}

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		if (searchkelas != null) searchkelas.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		if (searchguruPembina != null)
			searchguruPembina.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);

				}
			});

		if (searchguruBk != null)
			searchguruBk.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(null);

				}
			});

		if (execution.getParameter("guruPembina") != null) {
			Guru g = (Guru) ConstantValues.ambil(Guru.class.getName(),
					Long.parseLong(execution.getParameter("guruPembina")));
			if (g != null && searchguruPembina != null) {
				searchguruPembina.setAttribute("guru", g);
				searchguruPembina.setValue(g.getNama());
				searchguruPembina.setReadonly(true);
				searchguruPembina.setDisabled(true);
			}
		}

		if (execution.getParameter("guruBk") != null) {
			Guru g = (Guru) ConstantValues.ambil(Guru.class.getName(),
					Long.parseLong(execution.getParameter("guruBk")));
			if (g != null && searchguruBk != null) {
				searchguruBk.setAttribute("guru", g);
				searchguruBk.setValue(g.getNama());
				searchguruBk.setReadonly(true);
				searchguruBk.setDisabled(true);
			}
		}

		MyComboitemConfig comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Laki-laki"); }
		if (comboitem != null) { comboitem.setValue("Laki-laki"); }
		if (searchkelamin != null) { searchkelamin.appendChild(comboitem); }
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Perempuan"); }
		if (comboitem != null) { comboitem.setValue("Perempuan"); }
		if (searchkelamin != null) { searchkelamin.appendChild(comboitem); }
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		if (searchkelamin != null) { searchkelamin.appendChild(comboitem); }
		if (searchkelamin != null) { searchkelamin.setSelectedItem(comboitem); }

		Common.insertComboDanSemua(searchagama, new String[] { "nama" }, "keterangan", Agama.class, "Semua",
				Restrictions.eq("aktif", true));

		Common.insertComboDanSemua(searchstatuskeluar, new String[] { "nama" }, "keterangan", StatusKeluarSiswa.class,
				"Semua", Restrictions.eq("aktif", true));

		MyComboitemConfig ciSemua = new MyComboitemConfig();
		ciSemua.setLabel("Semua"); ciSemua.setValue(null);
		if (searchSudahKeluar != null) { searchSudahKeluar.appendChild(ciSemua); searchSudahKeluar.setSelectedItem(ciSemua); }
		MyComboitemConfig ciSudah = new MyComboitemConfig();
		ciSudah.setLabel("Sudah lulus/keluar"); ciSudah.setValue("sudah");
		if (searchSudahKeluar != null) { searchSudahKeluar.appendChild(ciSudah); }
		MyComboitemConfig ciBelum = new MyComboitemConfig();
		ciBelum.setLabel("Belum lulus/keluar"); ciBelum.setValue("belum");
		if (searchSudahKeluar != null) { searchSudahKeluar.appendChild(ciBelum); }

		MyComboitemConfig ckSemua = new MyComboitemConfig();
		ckSemua.setLabel("Semua"); ckSemua.setValue(null);
		if (searchPunyaKelas != null) { searchPunyaKelas.appendChild(ckSemua); searchPunyaKelas.setSelectedItem(ckSemua); }
		MyComboitemConfig ckAda = new MyComboitemConfig();
		ckAda.setLabel("Punya kelas"); ckAda.setValue("ada");
		if (searchPunyaKelas != null) { searchPunyaKelas.appendChild(ckAda); }
		MyComboitemConfig ckTidak = new MyComboitemConfig();
		ckTidak.setLabel("Tidak punya kelas"); ckTidak.setValue("tidak");
		if (searchPunyaKelas != null) { searchPunyaKelas.appendChild(ckTidak); }

		// FIX NPE: StatusAwalSiswa TIDAK punya properti Hibernate-mapped
		// "keterangan" (lihat perbaikan sama di CalonSiswaAction.java) --
		// deskripsi "" aman (fallback ke toString() entity).
		Common.insertComboDanSemua(searchstatusawal, new String[] { "nama" }, "", StatusAwalSiswa.class,
				"Semua", Restrictions.eq("aktif", true));

		MyToolbarbuttonConfig generatePasswordSiswa = new MyToolbarbuttonConfig("Download Password", "/img/print.png");
		if (generatePasswordSiswa != null) { generatePasswordSiswa.setVisible((add != null && add.isVisible()) && edit && delete); }
		generatePasswordSiswa.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (searchtahunMulai.getValue() == null || searchtahunMulai.getValue().intValue() < 1970) {
					MyMessageboxConfig.show("Tahun Masuk harus diisi dengan benar", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									searchtahunMulai.focus();
									searchtahunMulai.select();
								}
							});
					return;
				}

				MyMessageboxConfig.show("Anda akan mendapatkan username dan password siswa.", "Informasi",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									String strURL = Common
											.getKonfigurasi("ambil_kode_url", "https://dev.ecampus.id/ecampus/Api")
											.getNilai();

									String link = Common.getRequestHostWithProtocol() + "/Api";
									String nama_pt = perguruanTinggi.getNama();

									HttpServletRequest request = (HttpServletRequest) Executions.getCurrent()
											.getNativeRequest();

									String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
											.getPerguruanTinggiMedia(request, "background_perguruanTinggi_");
									String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
											.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
									String banner_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
											.getPerguruanTinggiMedia(request, "banner_perguruanTinggi_");
									String background_login_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
											.getPerguruanTinggiMedia(request, "background_login_perguruanTinggi_");

									Session session = HibernateUtil.currentSession();
									int count = ((Number) session.createCriteria(Siswa.class)
											.add(Restrictions.isNotNull("namaSiswa"))
											.add(Restrictions.ne("namaSiswa", ""))
											.add(Restrictions.isNotNull("sekolah"))
											.add(Restrictions.or(Restrictions.isNull("is_encripted"),
													Restrictions.eq("is_encripted", false)))
											.setProjection(Projections.rowCount()).uniqueResult()).intValue();
									if (count > 0) {
										List<Siswa> siswas = ConstantValues
												.simpleList(
														session.createCriteria(Siswa.class)
																.add(Restrictions.isNotNull("namaSiswa"))
																.add(Restrictions.ne("namaSiswa", ""))
																.add(Restrictions.isNotNull("sekolah"))
																.add(Restrictions.or(
																		Restrictions.isNull("is_encripted"),
																		Restrictions.eq("is_encripted", false))),
														Siswa.class);
										for (Siswa siswa : siswas) {
											siswa.setIs_encripted(true);
											siswa.setPass(Common.desEncrypter.get().encrypt(siswa.getPass()));
											session.update(siswa);
										}
									}

									String filename = Sessions.getCurrent().getWebApp()
											.getRealPath("/tmp/user_password_siswa_"
													+ URLEncoder.encode(Common.datetimeFormat2s.get()
															.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
													+ ".xlsx");

									List<Siswa> siswas = ConstantValues.simpleList(
											initCriteria(true).add(Restrictions.isNotNull("nim"))
													.add(Restrictions.ne("nim", "")).setMaxResults(1048576),
											Siswa.class);

									XSSFWorkbook workbook = new XSSFWorkbook();
									XSSFSheet sheet = workbook.createSheet("SISWA");
									sheet.setDefaultColumnWidth(20);
									int rowIndex = 0;

									XSSFRow rowhead = sheet.createRow((short) 0);
									rowhead.createCell(0).setCellValue("Username");
									rowhead.createCell(1).setCellValue("Password");
									rowhead.createCell(2).setCellValue("Sekolah");
									rowhead.createCell(3).setCellValue("Yayasan");
									rowhead.createCell(4).setCellValue("Nama Lengkap");
									rowhead.createCell(5).setCellValue("Kode Install Mobile");

									for (Siswa siswa : siswas) {
										if (siswa.getNama() != null && !siswa.getNama().trim().isEmpty()) {
											rowIndex++;
											XSSFRow row = sheet.createRow(rowIndex);
											boolean sudahdiubah = false;

											try {
												if (siswa.getPass() == null || siswa.getPass().trim().isEmpty()
														|| siswa.getPass().trim().equals("uRywMowySCU=")) {
													siswa.setIs_encripted(true);
													siswa.setPass(Common.desEncrypter.get().encrypt(siswa.getNim()));
													Common.refreshUpdate(siswa);
													sudahdiubah = true;
												}
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

											if (!sudahdiubah) {
												try {

													String desc2x = Common.desEncrypter.get()
															.decrypt(Common.desEncrypter.get().decrypt(siswa.getPass()));
													System.out.println("desc2x = " + desc2x);
													if (desc2x != null && !desc2x.trim().isEmpty()) {
														siswa.setIs_encripted(true);
														siswa.setPass(Common.desEncrypter.get().encrypt(siswa.getNim()));
														Common.refreshUpdate(siswa);
														sudahdiubah = true;
													}
												} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
											}

											if (!sudahdiubah) {
												try {

													String desc1x = Common.desEncrypter.get().decrypt(siswa.getPass());
													System.out.println("desc1x = " + desc1x + " " + siswa.getPass());
													if (desc1x != null && desc1x.trim().isEmpty()) {
														siswa.setIs_encripted(true);
														siswa.setPass(Common.desEncrypter.get().encrypt(siswa.getNim()));
														Common.refreshUpdate(siswa);
														sudahdiubah = true;
													}
												} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
											}

											row.createCell(0)
													.setCellValue(siswa.getNomorInduk() != null
															&& !siswa.getNomorInduk().isEmpty() ? siswa.getNomorInduk()
																	: siswa.getNim());

											try {
												row.createCell(1)
														.setCellValue(Common.desEncrypter.get().decrypt(siswa.getPass()));
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

											row.createCell(2)
													.setCellValue(siswa.getSekolah() == null
															|| siswa.getSekolah().getYayasan() == null ? ""
																	: siswa.getSekolah().getYayasan().getNama());
											row.createCell(3).setCellValue(
													siswa.getSekolah() == null ? "" : siswa.getSekolah().getNama());
											row.createCell(4).setCellValue(siswa.getNama());

											String hasil = "";
											try {

												String username = siswa.getNomorInduk() != null
														&& !siswa.getNomorInduk().isEmpty() ? siswa.getNomorInduk()
																: siswa.getNim() + ";"
																		+ Common.getRequestHostWithProtocol();

												if (!siswa.getNomorIndukNasional().isEmpty()) {
													username = siswa.getNomorIndukNasional() + ";"
															+ Common.getRequestHostWithProtocol();
												}

												JSONObject postData = new JSONObject();
												postData.put("username", username);
												postData.put("link", link);
												postData.put("nama_pt", nama_pt);
												postData.put("login_bg_pt", background_login_PerguruanTinggi);
												postData.put("bg_pt", background_PerguruanTinggi);
												postData.put("logo_pt", logo_PerguruanTinggi);
												postData.put("banner_pt", banner_PerguruanTinggi);

												postData.put("motto_pt", perguruanTinggi.getMotto());
												postData.put("alamat_pt", perguruanTinggi.getAlamat1());
												postData.put("telp_pt", perguruanTinggi.getTelepon());
												postData.put("email_pt", perguruanTinggi.getEmail());
												postData.put("action", "code");

												System.out.println("linkPost -> " + strURL);
												System.out.println("postData -> " + postData);

												String[] command = { "curl", "-d", postData.toString(), "-H",
														"Content-Type: application/json", strURL };

												ProcessBuilder process = new ProcessBuilder(command);
												Process p;
												p = process.start();
												BufferedReader reader = new BufferedReader(
														new InputStreamReader(p.getInputStream()));
												StringBuilder builder = new StringBuilder();
												String line = null;
												while ((line = reader.readLine()) != null) {
													builder.append(line);
													builder.append(System.getProperty("line.separator"));
												}
												hasil = builder.toString();

												System.out.println(hasil);

												JSONObject jsonObject = new JSONObject(hasil);

												row.createCell(5).setCellValue(
														jsonObject.isNull("code") ? "" : jsonObject.get("code") + "");
											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
											}
										}

									}

									try {
										FileOutputStream fileOut = new FileOutputStream(filename);
										workbook.write(fileOut);
										fileOut.close();
									} catch (IOException e) {
										// TODO Auto-generated
										// catch
										// block
										Common.tampilErrorJikaAdmin(e);
									}

									try {
										File file = new File(filename);
										Filedownload.save(new FileInputStream(file),
												"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
												file.getName());
									} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
								}

							}
						});

			}
		});
		Common.appendKeToolbar(generatePasswordSiswa, add, comp);

		// Tombol "Upload Password" (unggah Excel utk set password siswa massal). Dulu HANYA berupa
		// field + handler onUploadPasswordSiswa + setVisible, TANPA tombol yang pernah dibuat ->
		// selalu null -> tak pernah muncul. Kini tombolnya dibuat, di-forward ke handler, dan
		// ditampilkan mengikuti KONFIG + hak akses yang sama dengan "Download Password" (tidak lagi
		// bergantung getApakahAdmin, agar admin sekolah pun dapat memakainya).
		uploadPassword = new MyToolbarbuttonConfig("Upload Password", "/img/upload.gif");
		uploadPassword.setUpload(Common.ukuranFileUpload());
		uploadPassword.setTooltiptext("Unggah berkas Excel (.xlsx) untuk mengatur password siswa secara massal");
		uploadPassword.addForward("onUpload", self, "onUploadPasswordSiswa");
		Common.appendKeToolbar(uploadPassword, add, comp);
		uploadPassword.setVisible(Common.bolehKonfigurasi("aktifkan_upload_password_siswa")
				&& (add != null && add.isVisible()) && edit && delete);

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Jumlah Login");
		columnHeadersAdding.add("Sejarah Login");

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				Siswa siswa = (Siswa) objects[0];

				XSSFRow row = (XSSFRow) objects[2];

				Session session = HibernateUtil.currentNativeSession();

				Object[] logLogins = (Object[]) session.createCriteria(LogLogin.class)
						.add(Restrictions.eq("siswa", siswa)).add(Restrictions.isNotNull("login"))
						.setProjection(
								Projections.projectionList().add(Projections.max("login")).add(Projections.rowCount()))
						.uniqueResult();

				Date maxLogin = (Date) (logLogins == null || logLogins.length == 0 || logLogins[0] == null ? null
						: logLogins[0]);
				Integer countLogin = ((Number) (logLogins == null || logLogins.length == 0 || logLogins[1] == null ? 0
						: logLogins[1])).intValue();

				row.createCell(contents.length + 0).setCellValue(countLogin);

				row.createCell(contents.length + 1)
						.setCellValue(maxLogin == null ? "" : Common.dateFormat3.get().format(maxLogin));

				HibernateUtil.closeSession();
			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Siswa.class, this, "Download Siswa",
				"/img/print.png", columnHeadersAdding, dataAdding, false, null, "DATA TAMBAHAN", contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		final Sekolah currSekolah = tbmuser == null || tbmuser.ambilSekolah() == null ? SekolahUtil.getSekolah()
				: tbmuser.ambilSekolah();
		final Yayasan yayasan = tbmuser == null ? null : tbmuser.ambilYayasan();
//		final String ta = Common.getCurrentTahunAkademik();
		MyToolbarbuttonConfig upload = Common.uploadData(this, Siswa.class, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (yayasan != null) {

					Object[] data = (Object[]) arg0.getData();
					Siswa siswa = (Siswa) data[0];

					if (siswa != null && (siswa.getSekolah() == null || siswa.getSekolah().getId() == null)) {

						siswa.setYayasan(yayasan);
						Session session = (Session) data[1];
						@SuppressWarnings("rawtypes")
						Map datum = (Map) data[2];
						@SuppressWarnings("unchecked")
						List<String> apakahSimpan = (List<String>) data[3];

						Sekolah sekolah = currSekolah == null || currSekolah.getId() == null ? siswa.getSekolah()
								: currSekolah;

						Sekolah sekolahFilter = (Sekolah) (searchsekolah.getSelectedItem() == null ? null
								: searchsekolah.getSelectedItem().getValue());

						if ((sekolah == null || sekolah.getId() == null) && sekolahFilter != null) {
							sekolah = sekolahFilter;
							if (sekolah != null) {
								siswa.setSekolah(sekolah);
								apakahSimpan.add("ya");
							}
							if (searchguruPembina.getAttribute("guru") != null) {
								siswa.setGuruPembina((Guru) searchguruPembina.getAttribute("guru"));
								apakahSimpan.add("ya");
							}

							if (searchguruBk.getAttribute("guru") != null) {
								siswa.setGuruBk((Guru) searchguruBk.getAttribute("guru"));
								apakahSimpan.add("ya");
							}
						} else {

							String skl = datum.get("sekolah") != null ? datum.get("sekolah").toString().trim() : "";
							System.out.println("skl => " + skl);
							if ((sekolah == null || sekolah.getId() == null) && !skl.isEmpty()) {
								sekolah = (Sekolah) session.createCriteria(Sekolah.class)
										.createAlias("jenisSekolah", "jenisSekolah")
										.createAlias("jenisSekolah.jenjang", "jenjang")
										.add(Restrictions.ilike("jenjang.nama", skl))
										.add(yayasan == null || yayasan.getId() == null
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("yayasan", yayasan))
										.setMaxResults(1).uniqueResult();

								if (sekolah == null) {
									sekolah = (Sekolah) session.createCriteria(Sekolah.class)
											.createAlias("jenisSekolah", "jenisSekolah")
											.createAlias("jenisSekolah.jenjang", "jenjang")
											.add(Restrictions.ilike("jenjang.nama", skl, MatchMode.START))
											.add(yayasan == null || yayasan.getId() == null
													? Restrictions.sqlRestriction("true")
													: Restrictions.eq("yayasan", yayasan))
											.setMaxResults(1).uniqueResult();
								}
								if (sekolah == null) {
									sekolah = (Sekolah) session.createCriteria(Sekolah.class)
											.createAlias("jenisSekolah", "jenisSekolah")
											.add(Restrictions.ilike("jenisSekolah.nama", skl))
											.add(yayasan == null || yayasan.getId() == null
													? Restrictions.sqlRestriction("true")
													: Restrictions.eq("yayasan", yayasan))
											.setMaxResults(1).uniqueResult();
								}
								if (sekolah == null) {
									sekolah = (Sekolah) session.createCriteria(Sekolah.class)
											.add(Restrictions.ilike("nama", skl))
											.add(yayasan == null || yayasan.getId() == null
													? Restrictions.sqlRestriction("true")
													: Restrictions.eq("yayasan", yayasan))
											.setMaxResults(1).uniqueResult();
								}

								if (sekolah == null) {
									Jenjang jenjang = (Jenjang) session.createCriteria(Jenjang.class)
											.add(Restrictions.ilike("nama", skl)).setMaxResults(1).uniqueResult();
									if (jenjang == null) {
										jenjang = (Jenjang) session.createCriteria(Jenjang.class)
												.add(Restrictions.ilike("nama", skl, MatchMode.START)).setMaxResults(1)
												.uniqueResult();
									}

									if (jenjang != null) {
										JenisSekolah jenisSekolah = (JenisSekolah) session
												.createCriteria(JenisSekolah.class)
												.add(Restrictions.eq("jenjang", jenjang))
												.add(Restrictions.ilike("nama", skl)).setMaxResults(1).uniqueResult();
										if (jenisSekolah == null) {
											jenisSekolah = new JenisSekolah();
											jenisSekolah.setNama(skl);
											jenisSekolah.setJenjang(jenjang);
											session.getTransaction().begin();
											session.save(jenisSekolah);
											session.getTransaction().commit();
										}

										sekolah = new Sekolah();
										sekolah.setJenisSekolah(jenisSekolah);
										sekolah.setNama(skl);
										sekolah.setYayasan(yayasan);
										sekolah.setAlamat("Alamat sekolah");
										sekolah.setNss("Nomor Sekolah nasional");

										session.getTransaction().begin();
										session.save(sekolah);
										session.getTransaction().commit();
									}
								}

								if (searchguruPembina.getAttribute("guru") != null) {
									siswa.setGuruPembina((Guru) searchguruPembina.getAttribute("guru"));
									apakahSimpan.add("ya");
								}

								if (searchguruBk.getAttribute("guru") != null) {
									siswa.setGuruBk((Guru) searchguruBk.getAttribute("guru"));
									apakahSimpan.add("ya");
								}

								if (sekolah != null) {
									siswa.setSekolah(sekolah);
									apakahSimpan.add("ya");
								}
							}
						}
					}
				}
			}

		}, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] data = (Object[]) arg0.getData();
				Siswa siswa = (Siswa) data[0];
				siswa.setYayasan(yayasan);

			}
		}, contents);
		if (upload != null && (add != null && add.isVisible()) && edit && delete
				&& Common.bolehUploadDataKonfigurasi("hak_akses_upload_data_siswa", "*")) {
			Common.appendKeToolbar(upload, add, comp);
		}

		// Tombol "Download Foto" lama DIHAPUS — digantikan "Download Foto Massal" (bulk ZIP,
		// multi-thread + bar progres) agar seragam. Handler onDownloadFoto lama dibiarkan (tak dipakai).

		EventListener eventListenerSekolah = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (searchPenjurusan != null) {
					Common.clear(searchPenjurusan);
					Sekolah s = (Sekolah) (searchsekolah.getSelectedItem() == null ? null
							: searchsekolah.getSelectedItem().getValue());
					System.out.println("s => " + s);

					searchPenjurusan.setReadonly(true);

					if (s != null && s.getId() != null) {
						try {
							HibernateUtil.currentSession().refresh(s);
							Set<PenjurusanSekolah> selectedPenjurusanSekolah = s.getPenjurusanSekolahs();
							for (PenjurusanSekolah o : selectedPenjurusanSekolah) {
								if (o.getAktif()) {
									Comboitem comboitem = new Comboitem();
									comboitem.setLabel(o.getNama());
									comboitem.setDescription(o.getKeterangan());
									comboitem.setValue(o);
									searchPenjurusan.appendChild(comboitem);
								}
							}

							Comboitem comboitem = new Comboitem();
							comboitem.setLabel("Semua Penjurusan");
							comboitem.setValue(null);
							searchPenjurusan.appendChild(comboitem);
							searchPenjurusan.setSelectedItem(comboitem);

							comboitem = new Comboitem();
							comboitem.setLabel("Belum Ditentukan Penjurusan");
							comboitem.setValue(new PenjurusanSekolah());
							searchPenjurusan.appendChild(comboitem);

							comboitem = new Comboitem();
							comboitem.setLabel("Sudah Ditentukan Penjurusan");
							comboitem.setValue(new PenjurusanSekolah(-1L, ""));
							searchPenjurusan.appendChild(comboitem);
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
					}
				}

			}
		};

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("QR-Login", "/img/svg/qrcode-scan.svg");
		if (button != null) { button.setTooltiptext("Hapus Data"); }
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (searchtahunMulai.getValue() == null || searchtahunMulai.getValue().intValue() < 1970) {
					MyMessageboxConfig.show("Tahun Masuk harus diisi dengan benar", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									searchtahunMulai.focus();
									searchtahunMulai.select();
								}
							});
					return;
				}

				HttpServletRequest requesta = (HttpServletRequest) Executions.getCurrent().getNativeRequest();

				final String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
						.getPerguruanTinggiMedia(requesta, "background_perguruanTinggi_");
				final String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
						.getPerguruanTinggiMedia(requesta, "logo_perguruanTinggi_");
				final String banner_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
						.getPerguruanTinggiMedia(requesta, "banner_perguruanTinggi_");
				final String background_login_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
						.getPerguruanTinggiMedia(requesta, "background_login_perguruanTinggi_");

				final PDFMergerUtility ut = new PDFMergerUtility();
				final File filePdfBaru = new File(
						Common.ambilREAL_PATH_REPORT() + "/" + Common.getGeneratedBarCode() + ".pdf");
				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ut.setDestinationStream(new FileOutputStream(filePdfBaru));
						ut.mergeDocuments();

						Report.tampil(filePdfBaru);
					}
				});

				new Thread(new Runnable() {

					@SuppressWarnings({ "rawtypes", "unchecked" })
					@Override
					public void run() {

						String strURL = Common.getKonfigurasi("ambil_kode_url", "https://dev.ecampus.id/ecampus/Api")
								.getNilai();

						String link = Common.getRequestHostWithProtocol() + "/Api";
						String nama_pt = perguruanTinggi.getNama();

						Session session = HibernateUtil.currentSession();
						int count = ((Number) session.createCriteria(Siswa.class)
								.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
								.add(Restrictions.isNotNull("sekolah"))
								.add(Restrictions.or(Restrictions.isNull("is_encripted"),
										Restrictions.eq("is_encripted", false)))
								.setProjection(Projections.rowCount()).uniqueResult()).intValue();
						if (count > 0) {
							List<Siswa> siswas = ConstantValues.simpleList(session.createCriteria(Siswa.class)
									.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
									.add(Restrictions.isNotNull("sekolah"))
									.add(Restrictions.or(Restrictions.isNull("is_encripted"),
											Restrictions.eq("is_encripted", false))),
									Siswa.class);
							for (Siswa siswa : siswas) {
								siswa.setIs_encripted(true);
								siswa.setPass(Common.desEncrypter.get().encrypt(siswa.getPass()));
								session.update(siswa);
							}
						}

						try {

							List<Siswa> siswas = ConstantValues
									.simpleList(
											initCriteria(true).add(Restrictions.isNotNull("nim"))
													.add(Restrictions.ne("nim", "")).setMaxResults(1048576),
											Siswa.class);

							int index = 0;
							int size = siswas.size();
							for (Siswa siswa : siswas) {
								index++;
								label.setValue("Memperoses data " + siswa.getNama() + " ("
										+ Common.numberFormat.get().format(((index * 1.0) / (size * 1.0)) * 100.0) + "%)");
								try {

									String username = siswa.getNomorInduk() != null && !siswa.getNomorInduk().isEmpty()
											? siswa.getNomorInduk()
											: siswa.getNim() + ";" + Common.getRequestHostWithProtocol();

									if (!siswa.getNomorIndukNasional().isEmpty()) {
										username = siswa.getNomorIndukNasional() + ";"
												+ Common.getRequestHostWithProtocol();
									}

									JSONObject postData = new JSONObject();
									postData.put("username", username);
									postData.put("link", link);
									postData.put("nama_pt", nama_pt);
									postData.put("login_bg_pt", background_login_PerguruanTinggi);
									postData.put("bg_pt", background_PerguruanTinggi);
									postData.put("logo_pt", logo_PerguruanTinggi);
									postData.put("banner_pt", banner_PerguruanTinggi);

									postData.put("motto_pt", perguruanTinggi.getMotto());
									postData.put("alamat_pt", perguruanTinggi.getAlamat1());
									postData.put("telp_pt", perguruanTinggi.getTelepon());
									postData.put("email_pt", perguruanTinggi.getEmail());
									postData.put("action", "code");

									System.out.println("linkPost -> " + strURL);
									System.out.println("postData -> " + postData);

									String[] command = { "curl", "-d", postData.toString(), "-H",
											"Content-Type: application/json", strURL };

									ProcessBuilder process = new ProcessBuilder(command);
									Process p;
									p = process.start();
									BufferedReader reader = new BufferedReader(
											new InputStreamReader(p.getInputStream()));
									StringBuilder builder = new StringBuilder();
									String line = null;
									while ((line = reader.readLine()) != null) {
										builder.append(line);
										builder.append(System.getProperty("line.separator"));
									}
									String hasil = builder.toString();

									System.out.println(hasil);

									JSONObject jsonObject = new JSONObject(hasil);

									String code = jsonObject.isNull("code") ? "" : jsonObject.get("code") + "";
									File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + code + ".png");
									if (!myfilebarcode.exists()) {
										BarcodeCommon.generateCRCode(code, myfilebarcode);
									}
									Map parameters = ais.common.HashMapGenerator.getRand();
									Common.insertProperty(Siswa.class, siswa, parameters, "");
									parameters.put("code", myfilebarcode.getAbsolutePath());
									File file = Report.generateFileReport(Report.PDF, parameters, "sekolah/login_siswa",
											ais.ui.util.WaktuUtil.getDate(), new Toolbar());
									ut.addSource(file);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									ais.common.Common.tampilErrorJikaAdmin(e);
								}

							}

						} catch (Exception e) {
							// TODO Auto-generated catch block
							ais.common.Common.tampilErrorJikaAdmin(e);
						}

						label.setValue("");
					}
				}).start();

			}
		});
		if (button != null && add != null && add.getParent() != null) { button.setParent(add.getParent()); }

		searchsekolah.addEventListener("onChange", eventListenerSekolah);
		Common.createDefaultTimer(eventListenerSekolah);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		if (button != null) { button.setDisabled(!edit); }
		if (button != null) { button.setVisible((add != null && add.isVisible()) && edit); }
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiSiswaHelper revisiHelper = new RevisiSiswaHelper(new EventListener() {

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
		if (button != null && add != null && add.getParent() != null) { button.setParent(add.getParent()); }

	        FilterLanjutHelper.setup(comp);
}

	@SuppressWarnings("unchecked")
	public void onDownloadFoto(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Siswa> calonSiswa = initCriteria(true).list();
				File fileFolderLampiran = new File(
						"/opt/ecampus/foto_" + ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis());
				fileFolderLampiran.mkdirs();
				System.out.println("fileFolderLampiran => " + fileFolderLampiran.getAbsolutePath());

				for (Siswa siswa : calonSiswa) {

					try {
						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

						FotoSiswa fotosiswa = (FotoSiswa) streamingSession.createCriteria(FotoSiswa.class)
								.add(Restrictions.eq("siswa", siswa.getId())).setMaxResults(1).uniqueResult();

						if (fotosiswa != null) {
							File fileFoto = fotosiswa.ambilFile();
							File fileCopy = new File(fileFolderLampiran.getAbsolutePath() + "/" + siswa.getNomorInduk()
									+ "_" + siswa.getNamaSiswa() + "_" + fileFoto.getName());
							System.out.println("fileCopy => " + fileCopy.getAbsolutePath());
							FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
							FileInputStream fileInputStream = new FileInputStream(fileFoto);
							IOUtils.copyLarge(fileInputStream, fileOutputStream);
							fileInputStream.close();
							fileOutputStream.close();
						}

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e1) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/SiswaAction.java:1563");
					}

				}

				File fileFolderLampiranZip = new File(fileFolderLampiran.getAbsolutePath() + ".zip");
				Common.zipDir(fileFolderLampiranZip.getAbsolutePath(), fileFolderLampiran.getAbsolutePath());
				Filedownload.save(fileFolderLampiranZip, "application/zip");

			}
		}, "Harap tunggu.. sedang melakukan proses download foto..");

	}

	class SiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Siswa siswa = (Siswa) arg1;
			CommonMedia.tampilkanGambarKecil(siswa).setParent(arg0);

			Vbox aa = new Vbox();
			aa.setParent(arg0);
			new Label(siswa.getNomorIndukNasional()).setParent(aa);
			new Label(siswa.getNomorInduk()).setParent(aa);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(Siswa.class, siswa, siswa.getNama())).setParent(arg0);

			if (!siswa.getNamaSiswa().equalsIgnoreCase(siswa.getNamaAr())) {
				new Label(siswa.getNamaAr()).setParent(a);
			}
			if (!siswa.getNamaSiswa().equalsIgnoreCase(siswa.getNamaCh())) {
				new Label(siswa.getNamaCh()).setParent(a);
			}

//			TbmuserAction.tampilkanSocialMediaProfile(a, siswa.getSocialMediaProfile());

			new Label(siswa.getTahunMasuk().toString()).setParent(arg0);
			try {
				KelasSiswa kelasSiswa = siswa.getKelas();
				new Label(kelasSiswa == null ? "" : kelasSiswa.getNama()).setParent(arg0);
			} catch (Exception e) {
				new Label().setParent(a);
			}

			new Label(siswa.getYayasan() == null ? "" : siswa.getYayasan().getNama()).setParent(arg0);
			new Label((siswa.getSekolah() == null ? "" : siswa.getSekolah().getNama())
					+ (siswa.getPenjurusanSekolah() == null ? "" : " (" + siswa.getPenjurusanSekolah().getNama() + ")"))
					.setParent(arg0);
			new Label((siswa.getStatusAwalSiswa() == null ? "" : siswa.getStatusAwalSiswa().getNama())
					+ (siswa.getStatusKeluar() == null ? ""
							: " (" + siswa.getStatusKeluar().getNama()
									+ (siswa.getTahunLulus() == null ? "" : " / " + siswa.getTahunLulus()) + ")"))
					.setParent(arg0);
			try {
				Guru guru = siswa.getGuruPembina();
				new Label(guru == null ? "" : guru.getNamaGuru()).setParent(arg0);
			} catch (Exception e) {
				new Label().setParent(arg0);
			}
			try {
				Guru guru = siswa.getGuruBk();
				new Label(guru == null ? "" : guru.getNamaGuru()).setParent(arg0);
			} catch (Exception e) {
				new Label().setParent(arg0);
			}

			new Label(siswa.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(siswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					siswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(siswa);
				}
			});

			// Aksi baris: tombol Ubah/Hapus bawaan + tombol "Dasbor Studi Siswa" (analog dasbor mahasiswa).
			// sclass disamakan dgn Common.copyEditDeleteButtons (ais-row-action-btn) supaya ukuran/
			// padding/hover tombol ke-4 ini konsisten dgn 3 tombol lain -- sebelumnya tombol ini polos
			// tanpa class sehingga tampil beda ukuran & merusak kerapian kolom aksi.
			Hbox aksiSiswa = Common.copyEditDeleteButtons(edit, delete, siswa, SiswaAction.this);
			MyToolbarbuttonConfig dasborSiswaBtn = new MyToolbarbuttonConfig("", "/img/upload.gif");
			dasborSiswaBtn.setSclass("ais-row-action-btn ais-row-action-dasbor");
			dasborSiswaBtn.setTooltiptext("Dasbor Studi Siswa (nilai, kehadiran, riwayat pembelajaran per semester)");
			dasborSiswaBtn.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0b) throws Exception {
							ais.action.master.sekolah.helper.TampilStudiSiswaHelper.tampil(siswa, null);
						}
					});
				}
			});
			dasborSiswaBtn.setParent(aksiSiswa);
			aksiSiswa.setParent(arg0);

		}

	}

	public static void onAddExternal(MyWindow addWindow, Event event, EventListener eventListener, Siswa siswa)
			throws Exception {

		// if(siswa !=null && siswa.getId() != null){
		// HibernateUtil.currentSession().refresh(siswa);
		// }

		SiswaAction siswaAction = new SiswaAction();
		siswaAction.eventListener = eventListener;
		siswaAction.addWindow = addWindow;
		siswaAction.init(siswa);
	}

	public static void onAddExternal(Event event, EventListener eventListener, Siswa siswa) throws Exception {

		// if(siswa !=null && siswa.getId() != null){
		// HibernateUtil.currentSession().refresh(siswa);
		// }

		SiswaAction siswaAction = new SiswaAction();
		siswaAction.eventListener = eventListener;
		siswaAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(siswaAction.addWindow);
		siswaAction.addWindow.setHeight("95%");
		siswaAction.addWindow.setWidth("750px");

		siswaAction.init(siswa);

		siswaAction.addWindow.setVisible(true);
		siswaAction.addWindow.onModal();
	}

	/**
	 * Unggah FOTO MASSAL siswa: admin memilih banyak berkas foto sekaligus yang NAMA BERKAS-nya
	 * adalah No. Induk/NIS (mis. {@code 12345.jpg}, {@code 45666.png}). Setiap foto langsung dipasang
	 * sebagai foto profil siswa yang No. Induk-nya cocok, lalu ringkasan hasil ditampilkan. Dipicu
	 * tombol toolbar "Upload Foto (No. Induk)" (upload multiple).
	 */
	/** Unduh SEMUA foto siswa sebagai satu ZIP (baca BLOB paralel maks 50 thread). Tombol "Download Foto Massal". */
	public void onDownloadFotoMassal(Event event) throws Exception {
		ais.common.helper.DownloadFotoMassalHelper.downloadFotoSiswaMassal();
	}

	public void onUploadFotoMassal(Event event) throws Exception {
		if (!ais.common.helper.UploadFotoMassalHelper.bolehUploadMassal()) {
			ais.ui.util.MyMessageboxConfig.show(
					"Anda tidak memiliki hak akses (baca, ubah, dan hapus) untuk mengunggah foto.", "Akses Ditolak",
					ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.EXCLAMATION);
			return;
		}
		ForwardEvent forwardEvent = (ForwardEvent) event;
		UploadEvent uploadEvent = (UploadEvent) forwardEvent.getOrigin();
		Media[] medias = uploadEvent.getMedias();
		java.util.List<Media> daftar = new java.util.ArrayList<Media>();
		if (medias != null) {
			for (Media m : medias) {
				if (m != null) {
					daftar.add(m);
				}
			}
		} else if (uploadEvent.getMedia() != null) {
			daftar.add(uploadEvent.getMedia());
		}
		if (daftar.isEmpty()) {
			MyMessageboxConfig.show("Tidak ada berkas foto yang dipilih.", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Foto Massal Siswa");
		for (int i = 0; i < daftar.size(); i++) {
			Media m = daftar.get(i);
			String nama = (m != null && m.getName() != null && !m.getName().trim().isEmpty()) ? m.getName() : ("berkas-" + (i + 1));
			if (m != null && m.isBinary() && m.getName() != null && !m.getName().trim().isEmpty()) {
				report.sukses(i + 1, nama, "diajukan untuk upload");
			} else {
				report.gagal(i + 1, nama, "Berkas tidak valid (bukan biner atau nama berkas kosong)", "Pastikan berkas berupa gambar jpg/png dengan nama = Nomor Induk siswa");
			}
		}
		int[] hasil = ais.common.helper.UploadFotoMassalHelper.uploadFotoSiswaByNim(daftar);
		try {
			Filedownload.save(report.simpanLaporan(), "text/plain");
		} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) SiswaAction laporan foto massal"); }
		MyMessageboxConfig.show(ais.common.helper.UploadFotoMassalHelper.ringkasan(hasil), "Upload Foto Massal",
				MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
	}

	public void onAdd(Event event) throws Exception {
		init(new Siswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		siswa = (Siswa) obj;
		init(siswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("unchecked")
	public static void initKelasLes(Row row, final Set<Long> selectedKelasLesSiswa, final Set<Long> hapusKelasLesSiswa,
			final Siswa siswa) {
		Common.clear(row);
		Sekolah sekolah = siswa.getSekolah();
		List<KelasLesSiswa> kelasLesSiswas = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(KelasLesSiswa.class).addOrder(Order.asc("tingkat"))
						.addOrder(Order.asc("nama")).add(Restrictions.eq("sekolah", sekolah))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				KelasLesSiswa.class);

		if (kelasLesSiswas != null && !kelasLesSiswas.isEmpty()) {
			final MyGrid subGridKelasLesSiswa = new MyGrid();
			row.appendChild(subGridKelasLesSiswa);

			Columns subColumns = new Columns();
			subColumns.setParent(subGridKelasLesSiswa);
			Column c = new Column("Pilih Kelas Les");
			c.setWidth("70%");
			subColumns.appendChild(c);

			c = new Column("Sertifikat");
			subColumns.appendChild(c);

			Rows subRows = new Rows();
			subRows.setParent(subGridKelasLesSiswa);

			selectedKelasLesSiswa.addAll(siswa.ambilKelasLesSiswaId());

			Set<Long> ids = new HashSet<Long>();
			for (Long v : selectedKelasLesSiswa) {
				ids.add(v);
			}

			System.out.println("ids ->" + ids);

			Session session = HibernateUtil.currentSession();
			List<Long> idsKelasMasuk = siswa == null || siswa.getId() == null ? new ArrayList<Long>()
					: session.createCriteria(KelasLesSiswaPunyaSiswa.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setProjection(Projections.groupProperty("kelasLesSiswa.id"))
							.add(Restrictions.eq("siswa", siswa)).list();

			for (final KelasLesSiswa kelasLesSiswa : kelasLesSiswas) {

				MyFormRow subRow = new MyFormRow();
				subRow.setParent(subRows);
				subRow.setValign("top");

				if (idsKelasMasuk.contains(kelasLesSiswa.getId())) {
					new MyLabelAgakKecilBoldHijau(kelasLesSiswa.getNama()).setParent(subRow);
					final KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa = (KelasLesSiswaPunyaSiswa) session
							.createCriteria(KelasLesSiswaPunyaSiswa.class).add(Restrictions.eq("siswa", siswa))
							.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa)).setMaxResults(1).uniqueResult();

					if (kelasLesSiswaPunyaSiswa != null && kelasLesSiswaPunyaSiswa.getAcc()
							&& kelasLesSiswa.getSertifikat() != null) {
						MyToolbarbuttonConfig cetakToolbarbuttonSertifikat = new MyToolbarbuttonConfig("Sertifikat",
								"/img/certificate-icon.png");
						cetakToolbarbuttonSertifikat.setParent(subRow);
						cetakToolbarbuttonSertifikat.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								SertifikatAction.cetakSertifikat(kelasLesSiswaPunyaSiswa);
							}
						});
					} else {
						new Label().setParent(subRow);
					}
				} else {

					final Checkbox checkbox = new Checkbox(kelasLesSiswa.getNama());
					checkbox.setParent(subRow);
					checkbox.setChecked(ids.contains(kelasLesSiswa.getId()));
					checkbox.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (checkbox.isChecked()) {
								selectedKelasLesSiswa.add(kelasLesSiswa.getId());
								hapusKelasLesSiswa.remove(kelasLesSiswa.getId());
							} else {
								for (Long a : selectedKelasLesSiswa) {
									if (a.equals(kelasLesSiswa.getId())) {
										selectedKelasLesSiswa.remove(a);
										hapusKelasLesSiswa.add(a);
										break;
									}
								}
							}

							String jenisS = "";
							for (Long kelasLesSiswa : selectedKelasLesSiswa) {
								jenisS += jenisS.isEmpty() ? kelasLesSiswa.toString() : "," + kelasLesSiswa;
							}
							siswa.setKelasLesDipilih(jenisS);

							System.out.println(
									"selectedKelasLesSiswa => " + selectedKelasLesSiswa + ", jenisS " + jenisS);
						}
					});

					new Label().setParent(subRow);
				}

			}

		}
	}

	@SuppressWarnings("deprecation")
	private void initKelasLes(org.zkoss.zul.Div tabpanel) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel);
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		final MyGroupConfig myRowStyledKelas = new MyGroupConfig("KELAS YANG PILIH");
		myRowStyledKelas.setParent(rows);

		final MyFormRow rowDataG = new MyFormRow();
		rowDataG.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowDataG, "2");
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				selectedKelasLesSiswa = new TreeSet<Long>();
				hapusKelasLesSiswa = new TreeSet<Long>();
				SiswaAction.initKelasLes(rowDataG, selectedKelasLesSiswa, hapusKelasLesSiswa, siswa);
			}

		};

		Common.createDefaultTimer(eventListener);
	}

	private void init(final Siswa siswa) throws Exception {
		this.siswa = siswa;
		addWindow.setTitle(siswa.getId() == null ? "Tambah Siswa" : "Ubah Siswa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		West west = new West();
		west.setStyle("border:0px;");
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("270px");
		west.setParent(borderlayout);

		fotoSiswa = null;

		Common.createDownloadUploadFoto(west, siswa, FotoSiswa.class, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				fotoSiswa = (FotoSiswa) arg0.getData();
			}
		}, true);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		if (siswa.getId() != null) {
			final ais.ui.util.MyButtonTabbox btnTab = ais.ui.util.MyButtonTabbox.buat(center, "100%", new int[] { 0 });
			final int[] idx2 = {0};

			final org.zkoss.zul.Div panelSiswa = btnTab.tambahTab(idx2[0]++, "Data Siswa", "/img/svg/user-graduate.svg");

			{
				final int myIdx = idx2[0]++;
				final org.zkoss.zul.Div panelMobile = btnTab.tambahTab(myIdx, "Mobile", "/img/svg/phone.svg");
				boolean mobileVisible = Common.bolehKonfigurasi("tampilkan_mobile_di_profile_siswa");
				btnTab.setVisibleTombol(myIdx, mobileVisible);
				btnTab.onSetiapPilih(myIdx, new EventListener() {
					@Override public void onEvent(Event arg0) throws Exception {
						if (panelMobile.getChildren().isEmpty()) {
							MainHelper.onDapatkanKode(new Tbmuser(siswa), panelMobile, false);
						}
					}
				});
			}

			btnTab.tambahTabLazy(idx2[0]++, "Dokumen dan Lampiran", "/img/svg/folder-open-thin.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(final org.zkoss.zul.Div panelDok) throws Exception {
					final Runnable[] reloaderRef = {null};
					reloaderRef[0] = new Runnable() {
						@SuppressWarnings({ "deprecation", "unchecked" })
						@Override public void run() {
							try {

					Common.clear(panelDok);

					MyGrid grid = new MyGrid();
					grid.setWidth("100%");
					grid.setParent(Common.tampilanScroll(panelDok));
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
					row.appendChild(
							tambahDokumen = new MyToolbarbuttonConfig("Tambah Dokumen", "/img/svg/addthis.svg"));
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
									.getKonfigurasi("lampiran_pegawai",
											"Akta Kelahiran;BPJS;Kartu Keluarga;KTP;NPWP;Ijazah;Prestasi")
									.getNilai().split(";");

							final Combobox dokumen = new Combobox();
							for (String s : lampiran_Pegawai) {
								Comboitem comboitem = new Comboitem(s);
								dokumen.appendChild(comboitem);
							}
							row.appendChild(dokumen);
							dokumen.setWidth("95%");

							Common.initKeterangan(rowsTambah,
									"Ketikkan nama dokumen jika tidak tercantum dalam pilihan");

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

							LampiranLain.createDownloadUploadFileLain(hboxGambar, siswa.getId(),
									"Dokumen_Siswa_" + Common.getGeneratedBarCode(), "Dokumen", false,
									new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											LampiranLain lainMahasiswaCover = (LampiranLain) arg0.getData();

											Session streamingSession = StreamingHibernateUtil.getInstance()
													.currentSession();
											streamingSession.refresh(lainMahasiswaCover);
											lainMahasiswaCover.setRef(siswa.getId());
											lainMahasiswaCover.setJenis("Dokumen_Siswa_" + dokumen.getValue().trim());

											streamingSession.getTransaction().begin();
											streamingSession.update(lainMahasiswaCover);
											streamingSession.getTransaction().commit();
											StreamingHibernateUtil.getInstance().closeSession();

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													addWindow.detach();
													reloaderRef[0].run();
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
							.addOrder(Order.asc("id")).add(Restrictions.eq("ref", siswa.getId()))
							.add(Restrictions.ilike("jenis", "Dokumen_Siswa_", MatchMode.START)).list();
					StreamingHibernateUtil.getInstance().closeSession();
					String doksId = "";
					for (final LampiranLain lampiranLain : lampiranLains) {

						doksId += doksId.isEmpty() ? lampiranLain.getId().toString() : "," + lampiranLain.getId();

						row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);
						row.appendChild(new Label(lampiranLain.getJenis().replaceAll("Dokumen_Siswa_", "")));

						Hbox hbox = new Hbox();
						row.appendChild(hbox);
						hbox.appendChild(
								tambahDokumen = new MyToolbarbuttonConfig("Lihat Dokumen", "/img/svg/eye.svg"));
						tambahDokumen.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.display(lampiranLain);
							}
						});

						MyToolbarbuttonConfig hapusDokumen;
						hbox.appendChild(
								hapusDokumen = new MyToolbarbuttonConfig("Hapus Dokumen", "/img/svg/trash.svg"));
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

														reloaderRef[0].run();
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

					if (!siswa.getKarpeg().equalsIgnoreCase(doksId)) {
						siswa.setKarpeg(doksId);
						Common.refreshSaveOrUpdate(siswa);
					}

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				};
				reloaderRef[0].run();
				}
			});

			Borderlayout myborderlayout = new ais.ui.util.MyBorderlayout();
			myborderlayout.setParent(panelSiswa);
			myborderlayout.setWidth("100%");
			myborderlayout.setHeight("100%");

			Center mycenter = new Center();
			mycenter.setParent(myborderlayout);
			ais.ui.util.ZkCompat.setFlex(mycenter, true);

			mycenter.appendChild(grid);
			grid.setHeight("1500px");

			btnTab.tambahTabLazy(idx2[0]++, "Kegiatan Kesiswaan", "/img/svg/calendar-check.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					DashboardKegiatanKesiswaan window = new DashboardKegiatanKesiswaan(siswa);
					ais.ui.util.BaseDasbordPortal.mountWrapped(window, panel,
						"Kegiatan Kesiswaan", "Rekap kegiatan ekstrakurikuler dan prestasi siswa ini.");
				}
			});

			{
				org.zkoss.zul.Div panelKelasLes = btnTab.tambahTab(idx2[0]++, "Kelas Les", "/img/svg/chalkboard-teacher-light.svg");
				try {
					initKelasLes(panelKelasLes);
				} catch (Exception exKelasLes) {
					panelKelasLes.getChildren().clear();
					Label infoKelasLes = new Label("Panel Kelas Les belum dapat dimuat. Silakan tutup form dan buka kembali.");
					infoKelasLes.setStyle("display:block;padding:10px;color:#b45309;");
					infoKelasLes.setParent(panelKelasLes);
				}
			}

			if (Common.bolehKonfigurasi("tampilRiwayatMediaSosial")) {
				final int myIdxSosial = idx2[0]++;
				final org.zkoss.zul.Div panelSosial = btnTab.tambahTab(myIdxSosial, "Media Sosial", "/img/svg/comment-2-text-line.svg");
				final ais.ui.util.MyTabConfig dummyTabSosial = new ais.ui.util.MyTabConfig();
				Common.displaySocialMedia(dummyTabSosial, panelSosial, siswa);
				btnTab.onSetiapPilih(myIdxSosial, new EventListener() {
					@Override public void onEvent(Event arg0) throws Exception {
						org.zkoss.zk.ui.event.Events.sendEvent(new Event("onClick", dummyTabSosial));
					}
				});
			}
		} else {
			grid.setParent(center);
		}

		grid.setWidth("100%");
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

		Tbmuser tbmuser = Common.getCurrentUser();

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		String statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("namaSiswa");
		row.setVisible(
				statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nama Lengkap " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.setParent(rows);
		namaSiswa = new Textbox(siswa.getNamaSiswa());
		if (tbmuser != null && tbmuser.getSiswa() != null) {
			row.appendChild(new Label(siswa.getNamaSiswa()));
		} else {
			row.appendChild(namaSiswa);
		}
		namaSiswa.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("panggilan");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Panggilan " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		panggilan = new Textbox(siswa.getPanggilan());
		if (tbmuser != null && tbmuser.getSiswa() != null) {
			row.appendChild(new Label(siswa.getPanggilan()));
		} else {
			row.appendChild(panggilan);
		}
		panggilan.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("namaAr");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nama (Aksara Arab) " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		namaAr = new Textbox(siswa.getNamaAr());
		if (tbmuser != null && tbmuser.getSiswa() != null) {
			row.appendChild(new Label(siswa.getNamaAr()));
		} else {
			row.appendChild(namaAr);
		}
		namaAr.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("namaCh");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nama (Aksara Tionghoa) " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		namaCh = new Textbox(siswa.getNamaCh());
		if (tbmuser != null && tbmuser.getSiswa() != null) {
			row.appendChild(new Label(siswa.getNamaCh()));
		} else {
			row.appendChild(namaCh);
		}
		namaCh.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("nomorInduk");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor Induk Peserta Didik (NIPD) " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		nomorInduk = new Textbox(siswa.getNomorInduk());

		if (tbmuser != null && tbmuser.getSiswa() != null) {
			row.appendChild(new Label(siswa.getNomorInduk()));
		} else {
			row.appendChild(nomorInduk);
		}
		nomorInduk.setWidth("90%");

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("nomorIndukNasional");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("NISN " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		nomorIndukNasional = new Textbox(siswa.getNomorIndukNasional());
		if (tbmuser != null && tbmuser.getSiswa() != null) {
			row.appendChild(new Label(siswa.getNomorIndukNasional()));
		} else {
			row.appendChild(nomorIndukNasional);
		}
		nomorIndukNasional.setWidth("90%");

		if (row.isVisible()) {
			Common.initKeterangan(rows,
					"* Nomor Induk Siswa Nasional (NISN) digunakan usebagai username untuk login siswa, jika belum memiliki NISN, bisa disamakan dengan NIS, untuk mencari NISN, bisa di lihat di link berikut :");

			String l = Common.getKonfigurasi("link_mencari_nisn", "https://nisn.data.kemdikbud.go.id/page/data")
					.getNilai();

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new Label());
			A link = new A(l);
			link.setHref(l);
			link.setTarget("_blank");
			row.appendChild(link);

		}

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("nomorIndukSantri");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor Induk Santri " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		nomorIndukSantri = new Textbox(siswa.getNomorIndukSantri());

		if (tbmuser != null && tbmuser.getSiswa() != null) {
			row.appendChild(new Label(siswa.getNomorIndukSantri()));
		} else {
			row.appendChild(nomorIndukSantri);
		}

		nomorIndukSantri.setWidth("90%");

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("idfinger");
		row = new MyFormRow();
		row.setVisible(
				statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Kode Finger Print " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		idfinger = new Textbox(siswa.getIdfinger());

		if (tbmuser != null && tbmuser.getSiswa() != null) {
			row.appendChild(new Label(siswa.getIdfinger()));
		} else {
			row.appendChild(idfinger);
		}

		idfinger.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("nik");
		row.setVisible(
				statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor Induk Kependudukan (NIK) " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.setParent(rows);
		row.appendChild(nik = new Textbox(siswa.getNik()));
		nik.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("kk");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor Kartu Keluarga (KK) " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(kk = new Textbox(siswa.getKk()));
		kk.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("password");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Password (Hanya Super Admin) " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		password = new Textbox(siswa.getPass() == null || siswa.getPass().trim().equals("") ? ""
				: Common.desEncrypter.get().decrypt(siswa.getPass()));
		if (tbmuser != null && tbmuser.getSiswa() != null) {
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("* Untuk mengubah password, klik menu Ganti Password")));
		} else {
			row.appendChild(password);
			password.setDisabled(!Common.getApakahAdmin());
		}
		password.setWidth("90%");
		password.setType("password");
		if (Common.bolehKonfigurasi("tampilkan_link_login_oleh_admin_di_data_siswa")) {
			if (siswa.getId() != null) {

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Link"));

				final A a = new A("Tampilkan Link");
				a.setHref("");
				row.appendChild(a);

				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String code = siswa.urlLogin();
						a.setLabel(code);
						a.setHref(Common.getRequestHostWithProtocol() + "/logoff?param="
								+ URLEncoder.encode(code, "UTF-8"));
					}
				});

				Common.initKeterangan(rows, "Link ini bisa digunakan untuk login tanpa menggunakan password");
			}
		}
		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("tahunMasuk");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tahun Masuk " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(tahunMasuk = new Intbox(siswa.getTahunMasuk()));

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("bulanMasuk");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Bulan Masuk " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(bulanMasuk = new Intbox(siswa.getBulanMasuk()));

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("diterimaDiSekolahIniDiKelas");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Diterima di kelas/tingkat " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		Hbox hbox = new Hbox();
		hbox.appendChild(diterimaDiSekolahIniDiKelas = new Intbox(siswa.getDiterimaDiSekolahIniDiKelas()));
		hbox.appendChild(new ais.ui.util.MyLabelConfig(" pada tanggal "));
		hbox.appendChild(padaTanggal = new MyDatebox(siswa.getPadaTanggal()));
		row.appendChild(hbox);
		diterimaDiSekolahIniDiKelas.setCols(3);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("sekolahAsal");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Asal Pendidikan Sebelumnya " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(sekolahAsal = new Textbox(siswa.getSekolahAsal()));
		sekolahAsal.setWidth("90%");

		jenisKelamin = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig();
		comboitem.setLabel("Laki-laki");
		comboitem.setValue("Laki-laki");
		jenisKelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Perempuan");
		comboitem.setValue("Perempuan");
		jenisKelamin.appendChild(comboitem);

		ttd = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanda Tangan (PNG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, siswa.getId(), LampiranLain.TTD_SISWA, "Tanda Tangan", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ttd = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true, null, false, false);

		hbox.setParent(row);

		CalonSiswa calonSiswa = siswa.ambilCalonSiswa();
		GelombangPendaftaranPsb myGelombangPendaftaranPsb = calonSiswa != null ? calonSiswa.getGelombangPendaftaranPsb()
				: null;

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("gelombangPendaftaranPsb");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Jalur Masuk " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		gelombangPendaftaran = new Combobox();
		if (myGelombangPendaftaranPsb != null) {
			row.appendChild(new Label(myGelombangPendaftaranPsb.getNama()));
		} else {
			row.appendChild(gelombangPendaftaran);
		}
		gelombangPendaftaran.setWidth("90%");
		gelombangPendaftaran.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("jenisKelamin");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Jenis Kelamin " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		Common.selectComboItem(jenisKelamin, siswa.getJenisKelamin());
		row.appendChild(jenisKelamin);
		jenisKelamin.setWidth("90%");
		jenisKelamin.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("tempatLahir");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tempat / Tanggal Lahir " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		Box hboxa = Common.isMobile() ? new Vbox() : new Hbox();
		hboxa.appendChild(tempatLahir = new Textbox(siswa.getTempatLahir() == null ? "" : siswa.getTempatLahir()));
		hboxa.appendChild(tanggalLahir = new MyDatebox(siswa.getTanggalLahir()));
		row.appendChild(hboxa);
		tempatLahir.setCols(10);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		Sekolah selectedSekolah = SekolahUtil.getSekolah();
		Yayasan selectedYayasan = SekolahUtil.getYayasan();

		if (siswa.getId() == null) {
			siswa.setYayasan(selectedYayasan);
			siswa.setSekolah(selectedSekolah);
		}
		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("yayasan");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Yayasan " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, siswa.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("sekolah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Sekolah " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, siswa.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(false);

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("penjurusanSekolah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Penjurusan " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(penjurusanSekolah = new Combobox());
		penjurusanSekolah.setWidth("90%");
		penjurusanSekolah.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(false);

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("asrama");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Asrama Siswa " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(asrama = new Combobox());
		asrama.setWidth("90%");
		asrama.setReadonly(true);
		Common.insertComboDanSemua(asrama, new String[] { "nama" }, "keterangan", AsramaSiswa.class,
				Restrictions.eq("aktif", true));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Rombel Saat Ini (Kelas)"));

		KelasSiswa kelasSiswa = null;

		try {
			kelasSiswa = siswa.getKelas();
			row.appendChild(new Label(kelasSiswa == null ? "{Belum ada kelas}" : kelasSiswa.getNama()));
		} catch (Exception e) {
			row.appendChild(new Label("{Belum ada kelas}"));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Wali kelas Saat Ini"));

		try {
			row.appendChild(
					new Label(kelasSiswa == null || kelasSiswa.getGuruPembina() == null ? "{Belum ada wali kelas}"
							: kelasSiswa.getGuruPembina().getNama()));
		} catch (Exception e) {
			row.appendChild(new Label("{Belum ada wali kelas}"));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru BK kelas Saat Ini"));

		try {
			row.appendChild(new Label(kelasSiswa == null || kelasSiswa.getGuruBk() == null ? "{Belum ada Guru BK}"
					: kelasSiswa.getGuruBk().getNama()));
		} catch (Exception e) {
			row.appendChild(new Label("{Belum ada Guru BK}"));
		}

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("agama");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Agama " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(agama = new Combobox());
		Common.insertCombo(agama, "nama", "keterangan", Agama.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(agama, siswa.getAgama());
		agama.setWidth("90%");
		agama.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("statusAwalSiswa");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Status Awal Siswa " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(statusAwalSiswa = new Combobox());
		Common.insertCombo(statusAwalSiswa, "nama", "kode", StatusAwalSiswa.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(statusAwalSiswa, siswa.getStatusAwalSiswa());
		statusAwalSiswa.setWidth("90%");
		statusAwalSiswa.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("statusKeluar");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Status Keluar dari sekolah " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		statusKeluar = new Combobox();
		row.appendChild(statusKeluar);
		Common.insertComboDanSemua(statusKeluar, new String[] { "nama" }, "keterangan", StatusKeluarSiswa.class,
				"== Siswa ini Belum Keluar / Masih Aktif ==", Restrictions.eq("aktif", true));
		Common.selectComboItem(statusKeluar, siswa.getStatusKeluar());
		statusKeluar.setWidth("90%");
		statusKeluar.setReadonly(true);

		tahunLulus = new Decimalbox(siswa.getTahunLulus() == null ? null : new BigDecimal(siswa.getTahunLulus()));

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("tahunLulus");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tahun Lulus/Keluar " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(tahunLulus);
		tahunLulus.setCols(3);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("tanggalLulus");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tanggal Lulus/Keluar " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
		row.appendChild(tanggalLulus = new MyDatebox(siswa.getTanggalLulus()));

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("anakKe");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Anak ke " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		hbox = new Hbox();
		hbox.appendChild(anakKe = new Intbox(siswa.getAnakKe()));
		hbox.appendChild(new ais.ui.util.MyLabelConfig(" dari "));
		hbox.appendChild(dariAnakKe = new Intbox(siswa.getDariAnakKe()));
		hbox.appendChild(new ais.ui.util.MyLabelConfig(" bersaudara "));
		row.appendChild(hbox);
		anakKe.setCols(2);
		dariAnakKe.setCols(2);

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("jumlahSaudaraKandung");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Jml. saudara kandung " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(jumlahSaudaraKandung = new Intbox(siswa.getJumlahSaudaraKandung()));

		row = new MyFormRow();
		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("jumlahSaudaraTiri");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Jml. saudara tiri " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(jumlahSaudaraTiri = new Intbox(siswa.getJumlahSaudaraTiri()));

		kewarganegaraan = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Mahasiswa.WNI);
		comboitem.setValue(Mahasiswa.WNI);
		kewarganegaraan.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Mahasiswa.WNA);
		comboitem.setValue(Mahasiswa.WNA);
		kewarganegaraan.appendChild(comboitem);
		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("kewarganegaraan");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Kewarganegaraan " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		Common.selectComboItem(kewarganegaraan, siswa.getKewarganegaraan());
		row.appendChild(kewarganegaraan);
		kewarganegaraan.setWidth("90%");
		kewarganegaraan.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("negara");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Asal Negara " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(negara = new AmbilDataNegaraBanbox());

		try {
			negara.setAttribute("negara", siswa.getNegara() == null ? ConstantValues.INDONESIA : siswa.getNegara());
			negara.setValue((siswa.getNegara() == null ? ConstantValues.INDONESIA : siswa.getNegara()).getNamaNegara());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/SiswaAction.java:2825");
			// TODO: handle exception
		}

		negara.setReadonly(true);
		negara.setWidth("90%");

		kondisiSiswa = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Berkecukupan");
		comboitem.setValue("Berkecukupan");
		kondisiSiswa.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Kurang Mampu");
		comboitem.setValue("Kurang Mampu");
		kondisiSiswa.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Lain-nya");
		comboitem.setValue("Lain-nya");
		kondisiSiswa.appendChild(comboitem);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("kondisiSiswa");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Kondisi Siswa " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		Common.selectComboItem(kondisiSiswa, siswa.getKondisiSiswa());
		row.appendChild(kondisiSiswa);
		kondisiSiswa.setWidth("90%");
		kondisiSiswa.setReadonly(true);

		statusSiswa = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Reguler");
		comboitem.setValue("Reguler");
		statusSiswa.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Ekslusif");
		comboitem.setValue("Ekslusif");
		statusSiswa.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Lain-nya");
		comboitem.setValue("Lain-nya");
		statusSiswa.appendChild(comboitem);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("statusSiswa");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Status Siswa " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		Common.selectComboItem(statusSiswa, siswa.getStatusSiswa());
		row.appendChild(statusSiswa);
		statusSiswa.setWidth("90%");
		statusSiswa.setReadonly(true);

		statusDalamKeluarga = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Anak Kandung");
		comboitem.setValue("Anak Kandung");
		statusDalamKeluarga.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Bukan Anak Kandung");
		// FIX GenericJDBCException "value too long for type character varying(12)": kolom DB
		// status_dalam_keluarga dibatasi varchar(12), sedangkan label "Bukan Anak Kandung" 19
		// karakter. Simpan value singkat yang tetap muat (label tampilan ke user tidak berubah).
		comboitem.setValue("Bukan Anak");
		statusDalamKeluarga.appendChild(comboitem);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("statusDalamKeluarga");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Status Dalam Keluarga " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		Common.selectComboItem(statusDalamKeluarga, siswa.getStatusDalamKeluarga());
		row.appendChild(statusDalamKeluarga);
		statusDalamKeluarga.setWidth("90%");
		statusDalamKeluarga.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("berat");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Berat Badan " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(berat = new MyDoublebox(siswa.getBerat()));

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("tinggi");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tinggi Badan " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(tinggi = new MyDoublebox(siswa.getTinggi()));

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("golonganDarah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Golongan Darah " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(golonganDarah = new Textbox(siswa.getGolonganDarah()));
		golonganDarah.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ConstantValues.penggunaanLabelBahasa);

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("bahasa");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Bahasa " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

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

		Common.selectComboItem(bahasa, siswa.getBahasa());

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("teleponSiswa");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Telp. " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(teleponSiswa = new Textbox(siswa.getTeleponSiswa()));
		teleponSiswa.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("alamatEmail");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Email " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(alamatEmail = new Textbox(siswa.getAlamatEmail()));
		alamatEmail.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("alamatSiswa");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Alamat Siswa " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(alamatSiswa = new Textbox(siswa.getAlamatSiswa()));
		alamatSiswa.setWidth("90%");
		alamatSiswa.setRows(2);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("rt");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("RT " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(rt = new Textbox(siswa.getRt()));
		rt.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("rw");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("RW " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(rw = new Textbox(siswa.getRw()));
		rw.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("dusun");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Dusun " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(dusun = new Textbox(siswa.getDusun()));
		dusun.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("kelurahan");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Kelurahan " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(kelurahan = new Textbox(siswa.getKelurahan()));
		kelurahan.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("kecamatan");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Kecamatan " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(kecamatan = new AmbilDataKecamatanBanbox());
		kecamatan.setWidth("90%");
		kecamatan.setAttribute("wilayah", siswa.getKecamatan());
		kecamatan.setValue(siswa.getKecamatan() == null ? "" : siswa.getKecamatan().getNama());
		kecamatan.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("kodePos");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Kode Pos " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(kodePos = new Textbox(siswa.getKodePos()));
		kodePos.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("jenisTinggal");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Jenis Tinggal " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(jenisTinggal = new Combobox());
		jenisTinggal.setWidth("90%");
		Common.insertComboDanSemua(jenisTinggal, new String[] { "nama" }, "keterangan", JenisTinggalSiswa.class,
				" = Jenis Tinggal Siswa = ", Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisTinggal, siswa.getJenisTinggal());

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("alatTransportasi");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Alat Transportasi " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(alatTransportasi = new Combobox());
		alatTransportasi.setWidth("90%");
		Common.insertComboDanSemua(alatTransportasi, new String[] { "nama" }, "keterangan", AlatTransportasiSiswa.class,
				" = Alat Transportasi Siswa = ", Restrictions.eq("aktif", true));
		Common.selectComboItem(alatTransportasi, siswa.getAlatTransportasi());

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("hp");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("HP " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(hp = new Textbox(siswa.getHp()));
		hp.setWidth("90%");

//		"hp",
//		"skhun",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("skhun");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig("Surat Keterangan Hasil Ujian Nasional (SKHUN) "
				+ (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(skhun = new Textbox(siswa.getSkhun()));
		skhun.setWidth("90%");

//		"penerimaKps",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("penerimaKps");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Penerima KPS " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(penerimaKps = new MyCheckboxConfig("Penerima Keluarga Pra Sejahtera (KPS)"));
		penerimaKps.setChecked(siswa.getPenerimaKps());

//		"noKps",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("noKps");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor Keluarga Pra Sejahtera (KPS) " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(noKps = new Textbox(siswa.getNoKps()));
		noKps.setWidth("90%");

		EventListener penerimaKpsListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				noKps.setDisabled(!penerimaKps.isChecked());
			}
		};
		penerimaKps.addEventListener("onClick", penerimaKpsListener);
		penerimaKpsListener.onEvent(null);

//		"noPesertaUjianNasional",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("noPesertaUjianNasional");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor Peserta Ujian Nasional " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(noPesertaUjianNasional = new Textbox(siswa.getNoPesertaUjianNasional()));
		noPesertaUjianNasional.setWidth("90%");

//		"noSeriIjazah",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("noSeriIjazah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor Seri Ijazah " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(noSeriIjazah = new Textbox(siswa.getNoSeriIjazah()));
		noSeriIjazah.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("noSeriTranskrip");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor Seri Transkrip " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(noSeriTranskrip = new Textbox(siswa.getNoSeriTranskrip()));
		noSeriTranskrip.setWidth("90%");

//		"penerimaKip",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("penerimaKip");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Penerima KPI " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(penerimaKip = new MyCheckboxConfig("Penerima Kartu Indonesia Pintar (KIP)"));
		penerimaKip.setChecked(siswa.getPenerimaKip());

//		"nomorKip",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("nomorKip");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor Kartu Indonesia Pintar (KIP) " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(nomorKip = new Textbox(siswa.getNomorKip()));
		nomorKip.setWidth("90%");

//		"namaDiKip",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("namaDiKip");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nama Kartu Indonesia Pintar (KIP) " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(namaDiKip = new Textbox(siswa.getNamaDiKip()));
		namaDiKip.setWidth("90%");

		EventListener penerimaKipListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				nomorKip.setDisabled(!penerimaKip.isChecked());
				namaDiKip.setDisabled(!penerimaKip.isChecked());
			}
		};
		penerimaKip.addEventListener("onClick", penerimaKipListener);
		penerimaKipListener.onEvent(null);

//		"nomorKks",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("nomorKks");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nama Kartu Keluarga Sejahtera (KKS) " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(nomorKks = new Textbox(siswa.getNomorKks()));
		nomorKks.setWidth("90%");

//		"noRegistrasiAktaLahir",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("noRegistrasiAktaLahir");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"No. Registrasi Akta Lahir " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(noRegistrasiAktaLahir = new Textbox(siswa.getNoRegistrasiAktaLahir()));
		noRegistrasiAktaLahir.setWidth("90%");

//		"bank",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("bank");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Bank " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(bank = new Textbox(siswa.getBank()));
		bank.setWidth("90%");

//		"nomorRekeningBank",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("nomorRekeningBank");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor Rekening Bank " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(nomorRekeningBank = new Textbox(siswa.getNomorRekeningBank()));
		nomorRekeningBank.setWidth("90%");

//		"rekeningAtasNama",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("rekeningAtasNama");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Rekening Atas Nama " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(rekeningAtasNama = new Textbox(siswa.getRekeningAtasNama()));
		rekeningAtasNama.setWidth("90%");

//		"layakPip",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("layakPip");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Layak PIP " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(layakPip = new MyCheckboxConfig("Layak Program Indonesia Pintar (PIP)"));
		layakPip.setChecked(siswa.getLayakPip());

//		"alasanLayakPip",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("alasanLayakPip");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alasan Layak Program Indonesia Pintar (PIP) "
				+ (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(alasanLayakPip = new Textbox(siswa.getAlasanLayakPip()));
		alasanLayakPip.setWidth("90%");

		EventListener penerimaPipListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				alasanLayakPip.setDisabled(!layakPip.isChecked());
			}
		};
		layakPip.addEventListener("onClick", penerimaPipListener);
		penerimaPipListener.onEvent(null);

//		"kebutuhanKhusus",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("kebutuhanKhusus");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Kebutuhan Khusus " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(kebutuhanKhusus = new Textbox(siswa.getKebutuhanKhusus()));
		kebutuhanKhusus.setWidth("90%");
		kebutuhanKhusus.setRows(2);

//		"lintang",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("lintang");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Lintang " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(lintang = new Textbox(siswa.getLintang()));
		lintang.setWidth("90%");

//		"bujur",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("bujur");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Bujur " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(bujur = new Textbox(siswa.getBujur()));
		bujur.setWidth("90%");

//		"lingkarKepala",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("lingkarKepala");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Lingkar Kepala " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(lingkarKepala = new Textbox(siswa.getLingkarKepala()));
		lingkarKepala.setWidth("90%");

//		"jarakRumahKeSekolah",

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("jarakRumahKeSekolah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Jarak Rumah Ke Sekolah (dalam KM)" + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(jarakRumahKeSekolah = new Textbox(siswa.getJarakRumahKeSekolah()));
		jarakRumahKeSekolah.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("hobby");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Hobbi Siswa " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(hobby = new Textbox(siswa.getHobby()));
		hobby.setWidth("90%");
		hobby.setRows(2);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("riwayatPenyakit");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Riwayat Penyakit Siswa " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(riwayatPenyakit = new Textbox(siswa.getRiwayatPenyakit()));
		riwayatPenyakit.setWidth("90%");
		riwayatPenyakit.setRows(2);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("prestasiSiswa1");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Prestasi Siswa I " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(prestasiSiswa1 = new Textbox(siswa.getPrestasiSiswa1()));
		prestasiSiswa1.setWidth("90%");
		prestasiSiswa1.setRows(2);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("prestasiSiswa2");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Prestasi Siswa II " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(prestasiSiswa2 = new Textbox(siswa.getPrestasiSiswa2()));
		prestasiSiswa2.setWidth("90%");
		prestasiSiswa2.setRows(2);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("prestasiSiswa3");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Prestasi Siswa III " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(prestasiSiswa3 = new Textbox(siswa.getPrestasiSiswa3()));
		prestasiSiswa3.setWidth("90%");
		prestasiSiswa3.setRows(2);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("namaAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Nama Ayah " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(namaAyah = new Textbox(siswa.getNamaAyah()));
		namaAyah.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("nikAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("NIK Ayah " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(nikAyah = new Textbox(siswa.getNikAyah()));
		nikAyah.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("alamatAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Alamat Ayah " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(alamatAyah = new Textbox(siswa.getAlamatAyah()));
		alamatAyah.setWidth("90%");
		alamatAyah.setRows(2);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("tempatLahirAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tempat / Tanggal Lahir Ayah " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		hboxa = Common.isMobile() ? new Vbox() : new Hbox();
		hboxa.appendChild(tempatLahirAyah = new Textbox(siswa.getTempatLahirAyah()));
		hboxa.appendChild(tanggalLahirAyah = new MyDatebox(siswa.getTanggalLahirAyah()));
		row.appendChild(hboxa);
		tempatLahirAyah.setCols(10);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("pekerjaanAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Pekerjaan Ayah " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(pekerjaanAyah = new Combobox());
		Common.insertCombo(pekerjaanAyah, "nama", PekerjaanOrtuSiswa.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(pekerjaanAyah, siswa.getPekerjaanAyah());
		pekerjaanAyah.setWidth("90%");
		pekerjaanAyah.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("pendidikanAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Pendidikan Ayah " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(pendidikanAyah = new Combobox());
		Common.insertCombo(pendidikanAyah, "nama", PendidikanOrangTuaSiswa.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(pendidikanAyah, siswa.getPendidikanAyah());
		pendidikanAyah.setWidth("90%");
		pendidikanAyah.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("penghasilanAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Rata-rata penghasilan Ayah " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(penghasilanAyah = new Combobox());
		Common.insertCombo(penghasilanAyah, "nama", PenghasilanOrangTuaSiswa.class);
		Common.selectComboItem(penghasilanAyah, siswa.getPenghasilanAyah());
		penghasilanAyah.setWidth("90%");
		penghasilanAyah.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("hp1ayah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"No. HP Ayah " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		hbox = new Hbox();
		hbox.appendChild(hp1ayah = new Textbox(siswa.getHp1ayah()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" atau ")));
		hbox.appendChild(hp2ayah = new Textbox(siswa.getHp2ayah()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" atau ")));
		hbox.appendChild(hp3ayah = new Textbox(siswa.getHp3ayah()));
		row.appendChild(hbox);
		hp1ayah.setCols(10);
		hp2ayah.setCols(10);
		hp3ayah.setCols(10);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("waAyah");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor WA Ayah " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(waAyah = new Textbox(siswa.getWaAyah()));
		waAyah.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("namaIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Nama Ibu " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(namaIbu = new Textbox(siswa.getNamaIbu()));
		namaIbu.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("nikIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("NIK Ibu " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(nikIbu = new Textbox(siswa.getNikIbu()));
		nikIbu.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("alamatIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Alamat Ibu " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(alamatIbu = new Textbox(siswa.getAlamatIbu()));
		alamatIbu.setWidth("90%");
		alamatIbu.setRows(2);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("tempatLahirIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tempat / Tanggal Lahir Ibu " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		hboxa = Common.isMobile() ? new Vbox() : new Hbox();
		hboxa.appendChild(tempatLahirIbu = new Textbox(siswa.getTempatLahirIbu()));
		hboxa.appendChild(tanggalLahirIbu = new MyDatebox(siswa.getTanggalLahirIbu()));
		row.appendChild(hboxa);
		tempatLahirIbu.setCols(10);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("pekerjaanIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Pekerjaan Ibu " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(pekerjaanIbu = new Combobox());
		Common.insertCombo(pekerjaanIbu, "nama", PekerjaanOrtuSiswa.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(pekerjaanIbu, siswa.getPekerjaanIbu());
		pekerjaanIbu.setWidth("90%");
		pekerjaanIbu.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("pendidikanIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Pendidikan Ibu " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(pendidikanIbu = new Combobox());
		Common.insertCombo(pendidikanIbu, "nama", PendidikanOrangTuaSiswa.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(pendidikanIbu, siswa.getPendidikanIbu());
		pendidikanIbu.setWidth("90%");
		pendidikanIbu.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("penghasilanIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Rata-rata penghasilan Ibu " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(penghasilanIbu = new Combobox());
		Common.insertCombo(penghasilanIbu, "nama", PenghasilanOrangTuaSiswa.class);
		Common.selectComboItem(penghasilanIbu, siswa.getPenghasilanIbu());
		penghasilanIbu.setWidth("90%");
		penghasilanIbu.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("hp1ibu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"No. HP Ibu " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		hbox = new Hbox();
		hbox.appendChild(hp1ibu = new Textbox(siswa.getHp1ibu()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" atau ")));
		hbox.appendChild(hp2ibu = new Textbox(siswa.getHp2ibu()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" atau ")));
		hbox.appendChild(hp3ibu = new Textbox(siswa.getHp3ibu()));
		row.appendChild(hbox);
		hp1ibu.setCols(10);
		hp2ibu.setCols(10);
		hp3ibu.setCols(10);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("waIbu");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor WA Ibu " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(waIbu = new Textbox(siswa.getWaIbu()));
		waIbu.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("namaWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Nama Wali " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(namaWali = new Textbox(siswa.getNamaWali()));
		namaWali.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("nikWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("NIK Wali " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(nikWali = new Textbox(siswa.getNikWali()));
		nikWali.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("tempatLahirWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Tempat / Tanggal Lahir Wali " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		hboxa = Common.isMobile() ? new Vbox() : new Hbox();
		hboxa.appendChild(tempatLahirWali = new Textbox(siswa.getTempatLahirWali()));
		hboxa.appendChild(tanggalLahirWali = new MyDatebox(siswa.getTanggalLahirWali()));
		row.appendChild(hboxa);
		tempatLahirWali.setCols(10);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("pekerjaanWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Pekerjaan Wali " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(pekerjaanWali = new Combobox());
		Common.insertCombo(pekerjaanWali, "nama", PekerjaanOrtuSiswa.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(pekerjaanWali, siswa.getPekerjaanWali());
		pekerjaanWali.setWidth("90%");
		pekerjaanWali.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("pendidikanWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Pendidikan Wali " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(pendidikanWali = new Combobox());
		Common.insertCombo(pendidikanWali, "nama", PendidikanOrangTuaSiswa.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(pendidikanWali, siswa.getPendidikanWali());
		pendidikanWali.setWidth("90%");
		pendidikanWali.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("penghasilanWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Rata-rata penghasilan Wali " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(penghasilanWali = new Combobox());
		Common.insertCombo(penghasilanWali, "nama", PenghasilanOrangTuaSiswa.class);
		Common.selectComboItem(penghasilanWali, siswa.getPenghasilanWali());
		penghasilanWali.setWidth("90%");
		penghasilanWali.setReadonly(true);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("hp1wali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"No. HP Wali " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		hbox = new Hbox();
		hbox.appendChild(hp1wali = new Textbox(siswa.getHp1wali()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" atau ")));
		hbox.appendChild(hp2wali = new Textbox(siswa.getHp2wali()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" atau ")));
		hbox.appendChild(hp3wali = new Textbox(siswa.getHp3wali()));
		row.appendChild(hbox);
		hp1wali.setCols(10);
		hp2wali.setCols(10);
		hp3wali.setCols(10);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("waWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Nomor WA Wali " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(waWali = new Textbox(siswa.getWaWali()));
		waWali.setWidth("90%");

//		row = new MyFormRow();
////		row.setParent(rows);
//
//		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("alamatOrangTua");
//		row.setVisible(
//				statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB));
//		row.appendChild(new ais.ui.util.MyLabelConfig(
//				"Alamat Orang Tua " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
//
//		row.appendChild(alamatOrangTua = new Textbox(siswa.getAlamatOrangTua()));
//		alamatOrangTua.setWidth("90%");
//		alamatOrangTua.setRows(2);

//		row = new MyFormRow();
////		row.setParent(rows);
//
//		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("teleponOrangTua");
//		row.setVisible(
//				statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB));
//		row.appendChild(new ais.ui.util.MyLabelConfig(
//				"Telepon Orang Tua " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));
//
//		row.appendChild(teleponOrangTua = new Textbox(siswa.getTeleponOrangTua()));
//		teleponOrangTua.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("alamatWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Alamat Wali " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(alamatWali = new Textbox(siswa.getAlamatWali()));
		alamatWali.setWidth("90%");
		alamatWali.setRows(2);

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("teleponWali");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Telepon Wali " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(teleponWali = new Textbox(siswa.getTeleponWali()));
		teleponWali.setWidth("90%");

		row = new MyFormRow();

		statusWajibIsi = KonfigurasiTampilanSiswaAction.statusWajibIsi("keterangan");
		row.setParent((statusWajibIsi.equals(Konfigurasi.AKTIF) || statusWajibIsi.equals(Konfigurasi.AKTIF_TIDAK_WAJIB)) ? rows : null);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Keterangan " + (statusWajibIsi.equals(Konfigurasi.AKTIF) ? " (*)" : "")));

		row.appendChild(keterangan = new Textbox(siswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);

					if (SiswaAction.this.eventListener != null) {
						SiswaAction.this.eventListener.onEvent(event);
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

		EventListener eventListenerSekolah = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				penjurusanSekolah.getParent().setVisible(false);
				Common.clear(penjurusanSekolah);
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				System.out.println("s => " + s);

				if (s != null && s.getId() != null) {
					try {
						HibernateUtil.currentSession().refresh(s);
						Set<PenjurusanSekolah> selectedPenjurusanSekolah = s.getPenjurusanSekolahs();
						for (PenjurusanSekolah o : selectedPenjurusanSekolah) {
							if (o.getAktif()) {
								Comboitem comboitem = new Comboitem();
								comboitem.setLabel(o.getNama());
								comboitem.setDescription(o.getKeterangan());
								comboitem.setValue(o);
								penjurusanSekolah.appendChild(comboitem);
							}
						}
						penjurusanSekolah.getParent().setVisible(!selectedPenjurusanSekolah.isEmpty());
						Common.selectComboItem(penjurusanSekolah, siswa.getPenjurusanSekolah());

						Common.insertComboDanSemua(gelombangPendaftaran, new String[] { "nama", "tahunAjaran" },
								"informasi", GelombangPendaftaranPsb.class, "Pilih Gelombang Pendaftaran",
								Restrictions.and(Restrictions.eq("tahunMasuk", tahunMasuk.getValue()),
										Restrictions.and(
												Restrictions.or(Restrictions.isNull("sekolah"),
														Restrictions.eq("sekolah", s)),
												Restrictions.eq("aktif", true))));
						Common.selectComboItem(true, gelombangPendaftaran,
								SiswaAction.this.siswa.getGelombangPendaftaranPsb());

					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}

			}
		};

		sekolah.addEventListener("onChange", eventListenerSekolah);
		Common.createDefaultTimer(eventListenerSekolah);

	}

	public boolean onSave(Event event) throws Exception {
		if (namaSiswa.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Siswa belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Siswa dan ketik nama lengkap siswa; (2) pastikan nama tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
//		if (nomorIndukNasional.getValue().trim().equals("")) {
//			MyMessageboxConfig.show("NISN Siswa harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}

		if (tahunMasuk.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tahun Masuk belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Tahun Masuk dan pilih atau ketik tahun masuk siswa; (2) pastikan format tahun benar (misal: 2024); (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (bulanMasuk.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Bulan Masuk belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Bulan Masuk dan pilih bulan dari daftar; (2) pastikan bulan sudah terpilih; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
//		if (jenisKelamin.getSelectedItem() == null) {
//			MyMessageboxConfig.show("Jenis kelamin harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}
		if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Yayasan belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Yayasan dan pilih yayasan yang sesuai; (2) pastikan yayasan terpilih sebelum menyimpan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Sekolah belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Sekolah dan pilih sekolah dari daftar; (2) pastikan yayasan sudah dipilih agar daftar sekolah tersedia; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		Sekolah s = (Sekolah) sekolah.getSelectedItem().getValue();
		if (s.getPenjurusanWajibDipilih()) {
			if (penjurusanSekolah.getParent() != null && penjurusanSekolah.getParent().isVisible()
					&& (penjurusanSekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null)) {
				MyMessageboxConfig.show("Mohon maaf, Penjurusan belum dipilih padahal wajib untuk sekolah ini. Langkah yang dapat dilakukan: (1) klik kolom Penjurusan dan pilih jurusan yang sesuai; (2) pastikan sekolah sudah dipilih agar daftar penjurusan tersedia; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
		}
		if ((statusKeluar.getSelectedItem() != null && statusKeluar.getSelectedItem().getValue() != null
				&& tahunLulus.getValue() == null)) {
			MyMessageboxConfig.show("Mohon maaf, Tahun Lulus/Keluar belum diisi. Jika Status Keluar telah dipilih maka Tahun Lulus/Keluar wajib diisi. Langkah yang dapat dilakukan: (1) isi kolom Tahun Lulus/Keluar dengan tahun yang sesuai; (2) atau kosongkan pilihan Status Keluar jika tidak relevan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (siswa.getId() != null && siswa.getId() > 0L) {
			siswa = (Siswa) session.load(Siswa.class, siswa.getId());

		}

		if (siswa.getId() != null && siswa.getId() < 0L) {
			siswa.setId(null);
		}

		siswa.setPadaTanggal(padaTanggal.getValue());
		siswa.setNamaSiswa(namaSiswa.getValue());

		siswa.setNamaAr(namaAr.getValue());
		siswa.setNamaCh(namaCh.getValue());

		siswa.setPanggilan(panggilan.getValue());
		siswa.setNomorInduk(nomorInduk.getValue());
		siswa.setNomorIndukNasional(nomorIndukNasional.getValue());
		siswa.setTahunMasuk(tahunMasuk.getValue());
		siswa.setDiterimaDiSekolahIniDiKelas(diterimaDiSekolahIniDiKelas.getValue());
		siswa.setJenisKelamin(jenisKelamin.getValue());
		siswa.setTempatLahir(tempatLahir.getValue());
		siswa.setTanggalLahir(tanggalLahir.getValue());
		siswa.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		siswa.setAgama((Agama) (agama.getSelectedItem() == null ? null : agama.getSelectedItem().getValue()));
		siswa.setAnakKe(anakKe.getValue());
		siswa.setDariAnakKe(dariAnakKe.getValue());
		siswa.setJumlahSaudaraKandung(jumlahSaudaraKandung.getValue());
		siswa.setJumlahSaudaraTiri(jumlahSaudaraTiri.getValue());
		siswa.setKewarganegaraan(kewarganegaraan.getValue());
		siswa.setNegara((Negara) negara.getAttribute("negara"));
		siswa.setKondisiSiswa(kondisiSiswa.getValue());
		siswa.setStatusSiswa(statusSiswa.getValue());
		siswa.setTeleponSiswa(teleponSiswa.getValue());
		siswa.setAlamatEmail(alamatEmail.getValue());
		siswa.setNamaAyah(namaAyah.getValue());
		siswa.setNamaIbu(namaIbu.getValue());
		siswa.setNamaWali(namaWali.getValue());
		siswa.setKeterangan(keterangan.getValue());
		siswa.setNoSeriTranskrip(noSeriTranskrip.getValue().trim());
		siswa.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		siswa.setPekerjaanAyah((PekerjaanOrtuSiswa) (pekerjaanAyah.getSelectedItem() == null ? null
				: pekerjaanAyah.getSelectedItem().getValue()));
		siswa.setPekerjaanIbu((PekerjaanOrtuSiswa) (pekerjaanIbu.getSelectedItem() == null ? null
				: pekerjaanIbu.getSelectedItem().getValue()));
		siswa.setPekerjaanWali((PekerjaanOrtuSiswa) (pekerjaanWali.getSelectedItem() == null ? null
				: pekerjaanWali.getSelectedItem().getValue()));

		siswa.setPendidikanAyah((PendidikanOrangTuaSiswa) (pendidikanAyah.getSelectedItem() == null ? null
				: pendidikanAyah.getSelectedItem().getValue()));
		siswa.setPendidikanIbu((PendidikanOrangTuaSiswa) (pendidikanIbu.getSelectedItem() == null ? null
				: pendidikanIbu.getSelectedItem().getValue()));
		siswa.setPendidikanWali((PendidikanOrangTuaSiswa) (pendidikanWali.getSelectedItem() == null ? null
				: pendidikanWali.getSelectedItem().getValue()));

		siswa.setPenghasilanAyah((PenghasilanOrangTuaSiswa) (penghasilanAyah.getSelectedItem() == null ? null
				: penghasilanAyah.getSelectedItem().getValue()));
		siswa.setPenghasilanIbu((PenghasilanOrangTuaSiswa) (penghasilanIbu.getSelectedItem() == null ? null
				: penghasilanIbu.getSelectedItem().getValue()));
		siswa.setPenghasilanWali((PenghasilanOrangTuaSiswa) (penghasilanWali.getSelectedItem() == null ? null
				: penghasilanWali.getSelectedItem().getValue()));

//		siswa.setAlamatOrangTua(alamatOrangTua.getValue());
		siswa.setAlamatSiswa(alamatSiswa.getValue());
		siswa.setAlamatWali(alamatWali.getValue());
//		siswa.setTeleponOrangTua(teleponOrangTua.getValue());
		siswa.setTeleponWali(teleponWali.getValue());
		siswa.setSekolahAsal(sekolahAsal.getValue());
		siswa.setBerat(berat.getValue());
		siswa.setTinggi(tinggi.getValue());
		siswa.setGolonganDarah(golonganDarah.getValue());
		siswa.setStatusDalamKeluarga((String) (statusDalamKeluarga.getSelectedItem() == null ? null
				: statusDalamKeluarga.getSelectedItem().getValue()));

		siswa.setHobby(hobby.getValue());
		siswa.setTempatLahirAyah(tempatLahirAyah.getValue());
		siswa.setTempatLahirIbu(tempatLahirIbu.getValue());
		siswa.setTanggalLahirAyah(tanggalLahirAyah.getValue());
		siswa.setTanggalLahirIbu(tanggalLahirIbu.getValue());

		siswa.setTempatLahirIbu(tempatLahirIbu.getValue());
		siswa.setTanggalLahirWali(tanggalLahirWali.getValue());

		siswa.setRiwayatPenyakit(riwayatPenyakit.getValue());

		siswa.setHp1ayah(hp1ayah.getValue());
		siswa.setHp2ayah(hp2ayah.getValue());
		siswa.setHp3ayah(hp3ayah.getValue());

		siswa.setHp1ibu(hp1ibu.getValue());
		siswa.setHp2ibu(hp2ibu.getValue());
		siswa.setHp3ibu(hp3ibu.getValue());

		siswa.setHp1wali(hp1wali.getValue());
		siswa.setHp2wali(hp2wali.getValue());
		siswa.setHp3wali(hp3wali.getValue());
		siswa.setBulanMasuk(bulanMasuk.getValue());
		siswa.setStatusAwalSiswa((StatusAwalSiswa) (statusAwalSiswa.getSelectedItem() == null ? null
				: statusAwalSiswa.getSelectedItem().getValue()));
		siswa.setPenjurusanSekolah((PenjurusanSekolah) (penjurusanSekolah.getSelectedItem() == null ? null
				: penjurusanSekolah.getSelectedItem().getValue()));
		siswa.setNomorIndukSantri(nomorIndukSantri.getValue().trim());

		siswa.setAlamatAyah(alamatAyah.getValue());
		siswa.setAlamatIbu(alamatIbu.getValue());
		siswa.setPrestasiSiswa1(prestasiSiswa1.getValue());
		siswa.setPrestasiSiswa2(prestasiSiswa2.getValue());
		siswa.setPrestasiSiswa3(prestasiSiswa3.getValue());
		siswa.setNik(nik.getValue());
		siswa.setKk(kk.getValue());
		siswa.setWaAyah(waAyah.getValue());
		siswa.setWaIbu(waIbu.getValue());
		siswa.setWaWali(waWali.getValue());

		siswa.setPekerjaanAyah((PekerjaanOrtuSiswa) (pekerjaanAyah.getSelectedItem() == null ? null
				: pekerjaanAyah.getSelectedItem().getValue()));
		siswa.setPekerjaanIbu((PekerjaanOrtuSiswa) (pekerjaanIbu.getSelectedItem() == null ? null
				: pekerjaanIbu.getSelectedItem().getValue()));
		siswa.setPekerjaanWali((PekerjaanOrtuSiswa) (pekerjaanWali.getSelectedItem() == null ? null
				: pekerjaanWali.getSelectedItem().getValue()));

		siswa.setPendidikanAyah((PendidikanOrangTuaSiswa) (pendidikanAyah.getSelectedItem() == null ? null
				: pendidikanAyah.getSelectedItem().getValue()));
		siswa.setPendidikanIbu((PendidikanOrangTuaSiswa) (pendidikanIbu.getSelectedItem() == null ? null
				: pendidikanIbu.getSelectedItem().getValue()));
		siswa.setPendidikanWali((PendidikanOrangTuaSiswa) (pendidikanWali.getSelectedItem() == null ? null
				: pendidikanWali.getSelectedItem().getValue()));

		siswa.setStatusKeluar((StatusKeluarSiswa) (statusKeluar.getSelectedItem() == null ? null
				: statusKeluar.getSelectedItem().getValue()));

//		siswa.setGuruPembina((Guru) guruPembina.getAttribute("guru"));
//		siswa.setGuruBk((Guru) guruBk.getAttribute("guru"));

		siswa.setTahunLulus(tahunLulus.getValue() == null ? null : tahunLulus.getValue().intValue());

		siswa.setBahasa((String) (bahasa.getSelectedItem() == null ? null : bahasa.getSelectedItem().getValue()));

//		"rt",
		siswa.setRt(rt.getValue());
//		"rw",
		siswa.setRw(rw.getValue());
//		"dusun",
		siswa.setDusun(dusun.getValue());
//		"kelurahan",
		siswa.setKelurahan(kelurahan.getValue());
//		"kecamatan",
		siswa.setKecamatan((Wilayah) kecamatan.getAttribute("wilayah"));
//		"kodePos",
		siswa.setKodePos(kodePos.getValue());
//		"jenisTinggal",
		siswa.setJenisTinggal((JenisTinggalSiswa) (jenisTinggal.getSelectedItem() == null ? null
				: jenisTinggal.getSelectedItem().getValue()));
//		"alatTransportasi",
		siswa.setAlatTransportasi((AlatTransportasiSiswa) (alatTransportasi.getSelectedItem() == null ? null
				: alatTransportasi.getSelectedItem().getValue()));
//
//
//
//		"hp",
		siswa.setHp(hp.getValue());
//		"skhun",
		siswa.setSkhun(skhun.getValue());
//		"penerimaKps",
		siswa.setPenerimaKps(penerimaKps.isChecked());
//		"noKps",
		siswa.setNoKps(noKps.getValue());
//		"noPesertaUjianNasional",
		siswa.setNoPesertaUjianNasional(noPesertaUjianNasional.getValue());
//		"noSeriIjazah",
		siswa.setNoSeriIjazah(noSeriIjazah.getValue());
//		"penerimaKip",
		siswa.setPenerimaKip(penerimaKip.isChecked());
//		"nomorKip",
		siswa.setNomorKip(nomorKip.getValue());
//		"namaDiKip",
		siswa.setNamaDiKip(namaDiKip.getValue());
//		"nomorKks",
		siswa.setNomorKks(nomorKks.getValue());
//		"noRegistrasiAktaLahir",
		siswa.setNoRegistrasiAktaLahir(noRegistrasiAktaLahir.getValue());
//		"bank",
		siswa.setBank(bank.getValue());
//		"nomorRekeningBank",
		siswa.setNomorRekeningBank(nomorRekeningBank.getValue());
//		"rekeningAtasNama",
		siswa.setRekeningAtasNama(rekeningAtasNama.getValue());
//		"layakPip",
		siswa.setLayakPip(layakPip.isChecked());
//		"alasanLayakPip",
		siswa.setAlasanLayakPip(alasanLayakPip.getValue());
//		"kebutuhanKhusus",
		siswa.setKebutuhanKhusus(kebutuhanKhusus.getValue());
//		"lintang",
		siswa.setLintang(lintang.getValue());
//		"bujur",
		siswa.setBujur(bujur.getValue());
//		"lingkarKepala",
		siswa.setLingkarKepala(lingkarKepala.getValue());
//		"jarakRumahKeSekolah",
		siswa.setJarakRumahKeSekolah(jarakRumahKeSekolah.getValue());

		siswa.setAsrama((AsramaSiswa) (asrama.getSelectedItem() == null ? null : asrama.getSelectedItem().getValue()));

		siswa.setIdfinger(idfinger.getValue().trim());

		siswa.setGelombangPendaftaranPsb(gelombangPendaftaran.getSelectedItem() == null ? null
				: (GelombangPendaftaranPsb) gelombangPendaftaran.getSelectedItem().getValue());

		siswa.setTanggalLulus(tanggalLulus.getValue());

		try {
			siswa.setPass(Common.desEncrypter.get().encrypt(password.getValue().trim()));
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		List<String> daftarWajibDiisi = KonfigurasiTampilanSiswaAction.dataYangWajibDiisi();
		for (String key : daftarWajibDiisi) {
			if (Common.checkIsNull(Siswa.class, siswa, key)) {

				MyMessageboxConfig.show(
						"Biodata Siswa harus dilengkapi. Data \"" + KonfigurasiTampilanSiswaAction.keyDesc(key)
								+ "\" masih belum terisi dengan benar",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		String jenisS = "";
		if (this.selectedKelasLesSiswa != null) {
			for (Long kelasLesSiswa : this.selectedKelasLesSiswa) {
				jenisS += jenisS.isEmpty() ? kelasLesSiswa.toString() : "," + kelasLesSiswa;

				int count = ((Number) session.createCriteria(KelasLesSiswaPunyaSiswa.class)
						.setProjection(Projections.rowCount()).add(Restrictions.eq("kelasLesSiswa.id", kelasLesSiswa))
						.add(Restrictions.eq("siswa", siswa)).uniqueResult()).intValue();

				if (count == 0) {
					KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa = new KelasLesSiswaPunyaSiswa();
					kelasLesSiswaPunyaSiswa.setCalonSiswa(siswa.ambilCalonSiswa());
					kelasLesSiswaPunyaSiswa.setSiswa(siswa);
					kelasLesSiswaPunyaSiswa.setKelasLesSiswa(new KelasLesSiswa(kelasLesSiswa));
					kelasLesSiswaPunyaSiswa.setAktif(false);
					session.getTransaction().begin();
					session.save(kelasLesSiswaPunyaSiswa);
					session.getTransaction().commit();
				}
			}
		}
		siswa.setKelasLesDipilih(jenisS);

		if (this.hapusKelasLesSiswa != null) {
			for (Long kelasLesSiswa : this.hapusKelasLesSiswa) {
				session.createSQLQuery("delete from sekolah.kelas_les_punya_siswa where kelas_id=" + kelasLesSiswa
						+ " and siswa_id=" + siswa.getId()).executeUpdate();
			}
		}

		Common.refreshSaveOrUpdate(session, siswa);

		if (fotoSiswa != null) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.refresh(fotoSiswa);
			fotoSiswa.setSiswa(siswa.getId());
			streamingSession.getTransaction().begin();
			streamingSession.update(fotoSiswa);
			streamingSession.getTransaction().commit();

			StreamingHibernateUtil.getInstance().closeSession();
		}

		if (ttd != null && ttd.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(ttd);
				ttd.setRef(siswa.getId());

				session.getTransaction().begin();
				session.update(ttd);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.getSiswa() != null && tbmuser.getSiswa().getId().equals(siswa.getId())) {
			tbmuser.setSiswa(siswa);

			Sessions.getCurrent().setAttribute("mytbmuser", tbmuser);
			Sessions.getCurrent().setAttribute("usersTemp", tbmuser);
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> longs = null;
		if (searchguruPembina != null && searchguruPembina.getAttribute("guru") != null) {
			longs = session.createCriteria(KelasSiswaPunyaSiswa.class).setProjection(Projections.property("siswa.id"))
					.createAlias("kelasSiswa", "kelasSiswa")
					.add((searchguruPembina == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.eq("kelasSiswa.guruPembina", searchguruPembina.getAttribute("guru")))).list();
		}

		List<Long> longsBk = null;
		if (searchguruBk != null && searchguruBk.getAttribute("guru") != null) {
			longsBk = session.createCriteria(KelasSiswaPunyaSiswa.class).setProjection(Projections.property("siswa.id"))
					.createAlias("kelasSiswa", "kelasSiswa")
					.add((searchguruBk == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.eq("kelasSiswa.guruBk", searchguruBk.getAttribute("guru")))).list();
		}

		List<Long> longsKls = null;
		if (searchkelas != null && searchkelas.getAttribute("kelas") != null) {
			longsKls = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.setProjection(Projections.property("siswa.id"))
					.add((searchkelas == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.eq("kelasSiswa", searchkelas.getAttribute("kelas")))).list();

		}

		final String searchPunyaKelasVal = (searchPunyaKelas == null || searchPunyaKelas.getSelectedItem() == null)
				? null : (String) searchPunyaKelas.getSelectedItem().getValue();
		List<Long> longsKlsSemua = null;
		if (searchPunyaKelasVal != null) {
			longsKlsSemua = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.setProjection(Projections.property("siswa.id")).list();
		}

		PenjurusanSekolah penjurusanSekolah = (PenjurusanSekolah) (searchPenjurusan == null
				|| searchPenjurusan.getSelectedItem() == null ? null : searchPenjurusan.getSelectedItem().getValue());

		Criteria criteria = session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
				.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))

				.add(penjurusanSekolah == null ? Restrictions.sqlRestriction("true")
						: penjurusanSekolah.getId() == null ? Restrictions.isNull("penjurusanSekolah")
								: penjurusanSekolah.getId().equals(-1L) ? Restrictions.isNotNull("penjurusanSekolah")
										: Restrictions.eq("penjurusanSekolah", penjurusanSekolah))

				.add(longs == null || longs.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.in("id", longs))

				.add(longsBk == null || longsBk.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.in("id", longsBk))

				.add(longsKls == null || longsKls.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.in("id", longsKls))

				.add(alumni ? Restrictions.eq("statusKeluar.id", 1L)
						: (searchaktif != null && searchaktif.isChecked()
								? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
								: Restrictions.sqlRestriction("true")));

		if (order)
			criteria.addOrder(Order.desc("tahunMasuk")).addOrder(Order.asc("nomorInduk"));

		if (!searchnama.getValue().trim().isEmpty()) {
			criteria.createAlias("gelombangPendaftaranPsb", "gelombangPendaftaranPsb").add(Restrictions
					.ilike("gelombangPendaftaranPsb.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		}

		criteria.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") :

				Restrictions.or(Restrictions.ilike("namaSiswa", searchkode.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.or(
								Restrictions.ilike("nomorIndukSantri", searchkode.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("nomorInduk", searchkode.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("nomorIndukNasional", searchkode.getValue().trim(),
												MatchMode.ANYWHERE)))))

				.add(searchnamaemail.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("alamatEmail", searchnamaemail.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchnamaortu.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("namaWali", searchnamaortu.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("namaAyah", searchnamaortu.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("namaIbu", searchnamaortu.getValue().trim(),
												MatchMode.ANYWHERE))))

				.add(searchnamatelp.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") :

						Restrictions.or(
								Restrictions.ilike("hp1ibu", searchnamatelp.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("hp1ayah", searchnamatelp.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("teleponSiswa", searchnamatelp.getValue().trim(),
														MatchMode.ANYWHERE),
												Restrictions.or(
														Restrictions.ilike("teleponOrangTua",
																searchnamatelp.getValue().trim(), MatchMode.ANYWHERE),
														Restrictions.ilike("teleponWali",
																searchnamatelp.getValue().trim(),
																MatchMode.ANYWHERE)))))

				)

				.add(searchkelamin == null || searchkelamin.getSelectedItem() == null
						|| searchkelamin.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisKelamin", searchkelamin.getSelectedItem().getValue()))

				.add(searchagama.getSelectedItem() == null || searchagama.getSelectedItem().getValue() == null
						|| searchagama.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("agama", searchagama.getSelectedItem().getValue()))

				.add(searchtahunMulai == null || searchtahunMulai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.ge("tahunMasuk", searchtahunMulai.getValue().intValue()))

				.add(searchtahunSampai == null || searchtahunSampai.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.le("tahunMasuk", searchtahunSampai.getValue().intValue()))

				.add(searchSudahKeluar == null || searchSudahKeluar.getSelectedItem() == null
						|| searchSudahKeluar.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
						: "sudah".equals(searchSudahKeluar.getSelectedItem().getValue())
								? Restrictions.isNotNull("statusKeluar")
								: Restrictions.isNull("statusKeluar"))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchstatuskeluar.getSelectedItem() == null
						|| searchstatuskeluar.getSelectedItem().getValue() == null
						|| searchstatuskeluar.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("statusKeluar", searchstatuskeluar.getSelectedItem().getValue()))

				.add(searchstatusawal.getSelectedItem() == null || searchstatusawal.getSelectedItem().getValue() == null
						|| searchstatusawal.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("statusAwalSiswa", searchstatusawal.getSelectedItem().getValue()))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

				.add(searchPunyaKelasVal == null ? Restrictions.sqlRestriction("1=1")
						: "ada".equals(searchPunyaKelasVal)
								? (longsKlsSemua == null || longsKlsSemua.isEmpty()
										? Restrictions.sqlRestriction("1=0")
										: Restrictions.in("id", longsKlsSemua))
								: (longsKlsSemua == null || longsKlsSemua.isEmpty()
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.not(Restrictions.in("id", longsKlsSemua))));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchnama == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		List<Siswa> siswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(siswa);
		grid.setRowRenderer(new SiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
