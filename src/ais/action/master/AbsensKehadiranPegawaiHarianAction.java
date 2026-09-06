package ais.action.master;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
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
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AbsensiKehadiranPegawaiHarianHelper;
import ais.action.master.helper.AbsensiKehadiranPegawaiPerHariHelper;
import ais.action.master.helper.ProsesKehadiranDosen;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.payroll.helper.ProsesAbsensiPegawai;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.report.format1.akademik.LaporanAbsensiPegawai;
import ais.action.report.format1.akademik.LaporanAbsensiPegawaiPerHari;
import ais.action.report.format1.akademik.LaporanAbsensiPegawaiPerOrang;
import ais.action.report.format1.akademik.LaporanQrCodeAbsensiPegawai;
import ais.action.report.format1.payroll.LaporanLembur;
import ais.action.report.format1.payroll.LaporanRekapitulasiAbsen;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonOnSearchdefault;
import ais.common.CommonPayroll;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jabatan;
import ais.database.model.Pegawai;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusPegawai;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JabatanFungsional;
import ais.database.model.employ.JabatanStruktural;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk absens kehadiran pegawai harian. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Paging paging},
 * {@code Textbox searchcode}, {@code Textbox searchnama}, {@code Combobox searchstatus}, {@code Combobox
 * searchPernahAbsen}, {@code AmbilDataSatuanKerjaBanbox searchparent}, {@code Tabpanel laporanAbsensiPegawai};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code uploadDataMahasiswa()}, {@code onSearchDefault()}); mutasi data ({@code
 * onRiwayatProsesKehadiranPegawai()}, {@code onProsesKehadiran()}, {@code onProsesKehadiranPegawai()});
 * pelaporan/ekspor ({@code cetakDataCustomButton()}); operasi domain lain ({@code onLaporanTidakHadir()}, {@code
 * onLaporanLembur()}, {@code onPerHari()}, {@code onLaporanAbsensiPegawai()}, {@code
 * onLaporanAbsensiPerPegawai()}, {@code onLaporanAbsensiPerTanggal()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
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
public class AbsensKehadiranPegawaiHarianAction extends GenericAutowireComposer implements CommonOnSearchdefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyGrid grid;
	private Paging paging;
	private Textbox searchcode;
	private Textbox searchnama;
	private Combobox searchstatus;
	private Combobox searchPernahAbsen;
	private AmbilDataSatuanKerjaBanbox searchparent;

	protected Tabpanel laporanAbsensiPegawai;
	protected Tabpanel laporanAbsensiPerPegawai;
	protected Tabpanel harianAbsensiPegawai;
	protected Tabpanel laporanAbsensiPerTanggal;
	protected Tabpanel laporanAbsensiPerUnit;
	protected Tabpanel laporanAbsensiPerRinci;
	protected Tabpanel prosesKehadiranTab;

	protected Tabpanel riwayatProsesKehadiranPegawaiTab;

	public void onRiwayatProsesKehadiranPegawai(Event event) {

		if (riwayatProsesKehadiranPegawaiTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(riwayatProsesKehadiranPegawaiTab);
			MyInclude iframe = new MyInclude("/pages/master/kehadiran_pegawai_bulanan.zul");
			iframe.setParent(window);
		}
	}

	protected Tabpanel laporanTidakHadir;

	public void onLaporanTidakHadir(Event event) {

		if (laporanTidakHadir.getChildren().size() == 0) {
			LaporanRekapitulasiAbsen laporan = new LaporanRekapitulasiAbsen();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanTidakHadir);
		}
	}

	protected Tabpanel laporanLembur;

	public void onLaporanLembur(Event event) {

		if (laporanLembur.getChildren().size() == 0) {
			LaporanLembur laporan = new LaporanLembur();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanLembur);
		}
	}

	public void onProsesKehadiran(Event event) {

		if (prosesKehadiranTab.getChildren().size() == 0) {
			ProsesKehadiranDosen laporan = new ProsesKehadiranDosen();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.prosesKehadiranTab);
		}
	}

	protected Tabpanel prosesKehadiranPegawaiTab;

	public void onProsesKehadiranPegawai(Event event) {

		if (prosesKehadiranPegawaiTab.getChildren().size() == 0) {
			ProsesAbsensiPegawai laporan = new ProsesAbsensiPegawai();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.prosesKehadiranPegawaiTab);
		}
	}

	protected Tabpanel laporanQrPerTanggal;

	public void onPerHari(Event event) {

		if (harianAbsensiPegawai.getChildren().size() == 0) {
			AbsensiKehadiranPegawaiPerHariHelper laporan = new AbsensiKehadiranPegawaiPerHariHelper();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.harianAbsensiPegawai);
		}
	}

	private MyToolbarbuttonConfig add;

	public void onLaporanAbsensiPegawai(Event event) {

		if (laporanAbsensiPegawai.getChildren().size() == 0) {
			LaporanAbsensiPegawai laporan = new LaporanAbsensiPegawai();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanAbsensiPegawai);
		}
	}

	public void onLaporanAbsensiPerPegawai(Event event) {

		if (laporanAbsensiPerPegawai.getChildren().size() == 0) {
			LaporanAbsensiPegawaiPerOrang laporan = new LaporanAbsensiPegawaiPerOrang();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanAbsensiPerPegawai);
		}
	}

	public void onLaporanAbsensiPerTanggal(Event event) {

		if (laporanAbsensiPerTanggal.getChildren().size() == 0) {
			LaporanAbsensiPegawaiPerHari laporan = new LaporanAbsensiPegawaiPerHari();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanAbsensiPerTanggal);
		}
	}

	public void onLaporanAbsensiPerUnit(Event event) {

		if (laporanAbsensiPerUnit.getChildren().size() == 0) {
			ais.action.report.format1.payroll.LaporanAbsensiPegawaiPerOrang laporan = new ais.action.report.format1.payroll.LaporanAbsensiPegawaiPerOrang();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanAbsensiPerUnit);
		}
	}

	public void onLaporanAbsensiPerRinci(Event event) {

		if (laporanAbsensiPerRinci.getChildren().size() == 0) {
			ais.action.report.format1.payroll.LaporanAbsensiPegawaiPerPegawai laporan = new ais.action.report.format1.payroll.LaporanAbsensiPegawaiPerPegawai();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanAbsensiPerRinci);
		}
	}

	public void onQrPerTanggal(Event event) {

		if (laporanQrPerTanggal.getChildren().size() == 0) {
			LaporanQrCodeAbsensiPegawai laporan = new LaporanQrCodeAbsensiPegawai();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanQrPerTanggal);
		}
	}

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Common.insertComboDanSemua(searchstatus, "nama", StatusPegawai.class, Restrictions.eq("aktif", true));

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
		if (add != null) { add.setVisible(false); }
		Tbmuser tbmuser = Common.getCurrentUser();
		// Guard: getCurrentUser() bisa null → hindari NPE saat tbmuser.ambilDosen()/getMahasiswa().
		boolean bukanDosenMhs = tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null;
		MyToolbarbuttonConfig cetakToolbarbutton = cetakDataCustomButton("Download Kehadiran Pegawai",
				"/img/print.png");
		if (cetakToolbarbutton != null) { cetakToolbarbutton.setVisible(bukanDosenMhs); }
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig(
				"Upload Kehadiran Pegawai" + Common.ukuranLabelFileUpload(), "/img/excel.png");
		if (upload != null) { upload.setVisible(bukanDosenMhs); }
		if (upload != null) { upload.setUpload(Common.ukuranFileUpload()); }

		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							uploadDataMahasiswa(file, new EventListener() {

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
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	public static void uploadDataMahasiswa(final File file, final EventListener eventListener) throws Exception {

		final Label peringatan = new Label("");
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Absensi Harian Pegawai");
		final Label downloadPath = new Label("");

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					System.out.println("loading file " + file.getAbsolutePath());
					if (!downloadPath.getValue().isEmpty()) {
						try { Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); }
						catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) download laporan AbsensKehadiran"); }
					}
					MyMessageboxConfig.show(
							report.getRingkasan()
									+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()),
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						Session session = HibernateUtil.currentNativeSession();
						String identBaris = "Baris-" + i;
						try {

							String kode = Common.getSheetContentAsString(sheet, 0, i);
							Pegawai pegawai = null;
							if (kode != null && !kode.trim().isEmpty()) {
								pegawai = (Pegawai) session.createCriteria(Pegawai.class)
										.add(Restrictions.or(Restrictions.eq("aktif", true),
												Restrictions.isNull("aktif")))
										.add(Restrictions.eq("idfinger", kode)).setMaxResults(1).uniqueResult();
							}

							if (pegawai == null) {
								kode = Common.getSheetContentAsString(sheet, 1, i);
								if (kode != null && !kode.trim().isEmpty()) {
									pegawai = (Pegawai) session.createCriteria(Pegawai.class)
											.add(Restrictions.or(Restrictions.eq("aktif", true),
													Restrictions.isNull("aktif")))
											.add(Restrictions.eq("mycode", kode)).setMaxResults(1).uniqueResult();
								}
							}

							if (pegawai == null) {
								String nama = Common.getSheetContentAsString(sheet, 2, i);
								if (nama != null && !nama.trim().isEmpty()) {
									pegawai = (Pegawai) session.createCriteria(Pegawai.class)
											.add(Restrictions.or(Restrictions.eq("aktif", true),
													Restrictions.isNull("aktif")))
											.add(Restrictions.ilike("nama", nama)).setMaxResults(1).uniqueResult();
								}
							}
							if (pegawai != null) {
								identBaris = pegawai.getNama() != null ? pegawai.getNama() : "Baris-" + i;

								Date taggal = Common.getSheetContentAsDate(sheet, 6, i);

								if (taggal != null) {
									String mulai = Common.getSheetContentAsString(sheet, 7, i);
									String pulang = Common.getSheetContentAsString(sheet, 8, i);
									Statusabsensi statusabsensi = (Statusabsensi) Common.getSheetContentAsObject(sheet,
											9, i, Statusabsensi.class);

									Boolean am = null;
									if (mulai != null && mulai.trim().toLowerCase().contains("am")) {
										am = true;
									} else if (mulai != null && mulai.trim().toLowerCase().contains("pm")) {
										am = false;
									}

									Date masukjam = null;
									try {
										masukjam = am != null ? Common.parseFormatTime.get().parse(mulai)
												: Common.dateFormat1.get().parse(mulai.trim().split(" ")[0]);
									} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
			report.gagal(i, identBaris, e, "Periksa format jam masuk (kolom 7) pada baris ini.");
		}

									if (pulang == null || pulang.trim().isEmpty()) {
										pulang = mulai;
									}

									am = null;
									if (pulang != null && pulang.trim().toLowerCase().contains("am")) {
										am = true;
									} else if (pulang != null && pulang.trim().toLowerCase().contains("pm")) {
										am = false;
									}

									Date pulangJam = null;
									try {
										pulangJam = am != null ? Common.parseFormatTime.get().parse(pulang)
												: Common.dateFormat1.get().parse(pulang.trim().split(" ")[0]);
									} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
			report.gagal(i, identBaris, e, "Periksa format jam pulang (kolom 8) pada baris ini.");
		}

									if (masukjam != null) {
										statusabsensi = ConstantValues.MASUK;
									}

									StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = CommonPayroll
											.getDefaultStatuskehadiranKaryawanHarian(taggal, pegawai, null, null, "",
													"", session, true);

									statuskehadiranKaryawanHarian.setStatusabsensi(statusabsensi);
									statuskehadiranKaryawanHarian.setMasukjam(masukjam);
									statuskehadiranKaryawanHarian.setPulangJam(pulangJam);
									session.getTransaction().begin();
									session.update(statuskehadiranKaryawanHarian);
									session.getTransaction().commit();

									CommonPayroll.simpanDetail(session, statuskehadiranKaryawanHarian, true);

									report.sukses(i, identBaris, "Absensi berhasil dicatat");
									System.out
											.println("pegawai -> " + pegawai + ", " + Common.dateFormat1.get().format(taggal)
													+ " " + mulai + " " + pulang + " " + statusabsensi);
								}

								label.setValue("Upload data \"" + pegawai + "\" " + Common.dateFormat1.get().format(taggal)
										+ " (" + Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							} else {
								report.gagal(i, identBaris, "Pegawai tidak ditemukan pada baris ini", "Pastikan ID finger/kode/nama pada kolom 0-2 valid.");
							}

							// session.disconnect();
							if (session.isOpen()) {
								session.disconnect();
								session.close();
							}

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							report.gagal(i, identBaris, e, "Periksa format tanggal/data pada baris ini.");
						}
						HibernateUtil.closeSession();
					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/AbsensKehadiranPegawaiHarianAction.java:518");
				}

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) AbsensKehadiran laporan"); }
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}

	public MyToolbarbuttonConfig cetakDataCustomButton(String buttonLabel, String buttonImage) {

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(buttonLabel, buttonImage);

		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tahun Dan Bulan", "none", true);
				window.setParent(page.getFirstRoot());
				window.setHeight("300px");
				window.setWidth("600px");
				final Combobox tahunAkademik = new Combobox();
				Common.generateTahunAjaranDanSemua(tahunAkademik);
				final Combobox genapGanjil = new Combobox();
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(Perkuliahan.GENAP);
				comboitem.setValue(Perkuliahan.GENAP);
				genapGanjil.appendChild(comboitem);
				comboitem = new MyComboitemConfig();
				comboitem.setLabel(Perkuliahan.GANJIL);
				comboitem.setValue(Perkuliahan.GANJIL);
				genapGanjil.appendChild(comboitem);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("20%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Bulan")));
				final Combobox bulan;
				row.appendChild(bulan = new Combobox());
				bulan.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun")));
				final Combobox tahun;
				row.appendChild(tahun = new Combobox());
				tahun.setReadonly(true);

				for (int i = 0; i < 12; i++) {
					comboitem = new Comboitem(Common.BULAN[i]);
					comboitem.setValue(i + 1);
					bulan.appendChild(comboitem);
				}

				Common.selectComboItem(bulan, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1);

				Integer currTahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
				for (int i = currTahun - 10; i < currTahun + 10; i++) {
					comboitem = new Comboitem(i + "");
					comboitem.setValue(i);
					tahun.appendChild(comboitem);
				}

				Common.selectComboItem(tahun, currTahun);

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

					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Download", "/img/save.gif");
				save.setTooltiptext("Download");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();

						final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
						final Intbox intbox = new Intbox(10);
						Clients.showBusy(label.getValue());

						final String filename = Sessions.getCurrent().getWebApp()
								.getRealPath("/tmp/cetak_data_" + URLEncoder.encode(
										Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
										+ ".xlsx");
						final File file;
						(file = new File(filename)).createNewFile();

						final Timer timer = new Timer(200);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.setRepeats(true);
						timer.addEventListener("onTimer", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								try {

									Clients.showBusy(label.getValue());
									System.out.println("label " + label.getValue());

									if (label.getValue().trim().equalsIgnoreCase("-")) {
										Clients.clearBusy();
										timer.detach();
									} else if (label.getValue().isEmpty()) {

										Center center = new Center();
										final MyWindow window = new MyWindow("Cetak Data", "none", true);
										window.setParent(
												ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
										window.setHeight("97%");
										window.setWidth("90%");

										Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
										borderlayout.setParent(window);

										ais.ui.util.ZkCompat.setFlex(center, true);
										center.setParent(borderlayout);

										System.out.println("loading file " + file.getAbsolutePath());
										Common.clear(center);
										Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
										Common.clear(center);
										spreadsheet.setParent(center);
										spreadsheet.setWidth("100%");
										spreadsheet.setHeight("100%");
										spreadsheet.setSrc("../../tmp/" + file.getName());

										spreadsheet.setMaxrows(intbox.getValue() + 1);
										spreadsheet.setMaxcolumns(9);
										ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

										South south = new South();
										south.setParent(borderlayout);

										Toolbar toolbar = new Toolbar();
										// toolbar.setHeight("25px");
										toolbar.setParent(south);
										MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup",
												"/img/cancel.gif");
										cancel.setTooltiptext("Tutup");
										cancel.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {
												window.detach();
											}
										});
										cancel.setParent(toolbar);

										MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
												"/img/excel.png");
										print.addEventListener("onClick", new EventListener() {
											@Override
											public void onEvent(Event event) throws Exception {

												try {
													Filedownload.save(new FileInputStream(file),
															"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
															file.getName());
												} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
											}
										});
										print.setParent(toolbar);

										window.setVisible(true);
										window.onModal();

										Clients.clearBusy();
										timer.detach();
									}

								} catch (Exception e) {
									Clients.clearBusy();
								}

							}
						});
						timer.start();

						try {

							Clients.showBusy(label.getValue());

							new Thread(new Runnable() {

								@SuppressWarnings("unchecked")
								@Override
								public void run() {
									try {

									try {
										Integer thn = (Integer) tahun.getSelectedItem().getValue();
										Integer bln = (Integer) bulan.getSelectedItem().getValue();

										Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
										calendar.set(Calendar.MONTH, bln - 1);
										calendar.set(Calendar.YEAR, thn);
										calendar.set(Calendar.DATE, 1);

										int jumlahHari = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

										List<Pegawai> pegawais = initCriteria(true).list();
										int banyak = pegawais.size() * jumlahHari;
										intbox.setValue(banyak);

										XSSFWorkbook workbook = new XSSFWorkbook();
										XSSFSheet sheet = workbook.createSheet("CETAK DATA");
										sheet.setDefaultColumnWidth(20);
										int rowIndex = 0;

										XSSFRow rowhead = sheet.createRow((short) 0);
										String[] columns = new String[] { "PIN", "NIP", "Nama", "Jabatan", "Departemen",
												"Golongan", "Tanggal", "Masuk", "Keluar", "Status" };
										for (int i = 0; i < columns.length; i++) {
											rowhead.createCell(i).setCellValue(columns[i].toUpperCase());
										}

										for (Pegawai o : pegawais) {
											try {
												if (o == null) {
													continue;
												}
												for (int i = 1; i <= jumlahHari; i++) {

													Session session = HibernateUtil.currentNativeSession();
													try {
														calendar.set(Calendar.DATE, i);

														Date tanggal = calendar.getTime();

														StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = CommonPayroll
																.getDefaultStatuskehadiranKaryawanHarian(tanggal, o,
																		null, null, "", "", session, true);

														rowIndex++;

														label.setValue("Sedang memproses data " + o.toString() + " ("
																+ Common.numberFormat.get().format(rowIndex * 100.0 / banyak)
																+ " %)");

														XSSFRow row = sheet.createRow(rowIndex);

														row.createCell(0).setCellValue(o.getIdfinger());
														row.createCell(1).setCellValue(o.getMycode());
														row.createCell(2).setCellValue(o.getNama());

														String jenis = "";
														if (o.getTipePegawai() != null) {
															jenis = o.getTipePegawai().getNama();
														} else if (o.getGuru() != null) {
															jenis = "Guru";
														} else if (o.getDosen() != null) {
															jenis = "Dosen";
														} else if (o != null) {

															for (Object oo : ConstantValues
																	.ambilBerdasarClass(Tbmuser.class).values()) {
																Tbmuser tbmuser = (Tbmuser) oo;
																if (tbmuser.getAktif() && tbmuser.getUserRole() != null
																		&& tbmuser.getPegawai() != null
																		&& tbmuser.getPegawai().getId()
																				.equals(o.getId())) {
																	jenis = tbmuser.getUserRole().getRoleName();
																}
															}

														}

														row.createCell(3).setCellValue(jenis);
														row.createCell(4).setCellValue(o.getSatuanKerja() == null ? ""
																: o.getSatuanKerja().getNama());

														List<KenaikanPangkat> kenaikanPangkats = o
																.ambilKenaikanPangkat(tanggal);
														KenaikanPangkat kenaikanPangkat = kenaikanPangkats.isEmpty()
																? null
																: kenaikanPangkats.get(0);

														row.createCell(5).setCellValue(kenaikanPangkat == null
																|| kenaikanPangkat.getGolongan() == null ? ""
																		: kenaikanPangkat.getGolongan().getNama());

														row.createCell(6).setCellValue(tanggal == null ? ""
																: Common.dateFormat1.get().format(tanggal));

														row.createCell(7).setCellValue(
																statuskehadiranKaryawanHarian.ambilMasukjam() == null
																		? ""
																		: Common.timeFormat1.get()
																				.format(statuskehadiranKaryawanHarian
																						.ambilMasukjam()));
														row.createCell(8).setCellValue(
																statuskehadiranKaryawanHarian.ambilPulangjam() == null
																		? ""
																		: Common.timeFormat1.get()
																				.format(statuskehadiranKaryawanHarian
																						.ambilPulangjam()));
														row.createCell(9).setCellValue(
																statuskehadiranKaryawanHarian.getStatusabsensi() == null
																		? ""
																		: statuskehadiranKaryawanHarian
																				.getStatusabsensi().getNama());

														// session.disconnect();
														if (session.isOpen()) {
															session.disconnect();
															session.close();
														}

													} catch (Exception e) {
														ais.common.Common.tampilErrorJikaAdmin(e);
													}

													HibernateUtil.closeSession();

												}

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
										pegawais.clear();
										pegawais = null;
										label.setValue("");
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										label.setValue("-");
									}

																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

						} catch (Exception e) {
							// TODO Auto-generated catch block
							Common.tampilErrorJikaAdmin(e);
						}

					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});

		return toolbarbutton;
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AbsensKehadiranPegawaiHarianAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AbsensKehadiranPegawaiHarianAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Collection pangkats}; operasi lokal:
	 * {@code render}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AbsensKehadiranPegawaiHarianAction
	 */
	class PegawaiRenderer extends ais.ui.util.MyRowRenderer {
		@SuppressWarnings("rawtypes")
		Collection pangkats = ConstantValues.ambilBerdasarClass(KenaikanPangkat.class).values();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pegawai pegawai = (Pegawai) arg1;

			final AbsensiKehadiranPegawaiHarianHelper detail = new AbsensiKehadiranPegawaiHarianHelper(pegawai);
			detail.setParent(arg0);

			CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);

			Date sekarang = WaktuUtil.getDate();

			new Label(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(arg0);

			RevisiHelper.createNewRevisi(Pegawai.class, pegawai, pegawai.getNama()).setParent(arg0);
			new Label(pegawai.getStatusPegawai() == null ? "" : pegawai.getStatusPegawai().getNama()).setParent(arg0);

			List<KenaikanPangkat> kenaikanPangkats = pegawai.ambilKenaikanPangkat(sekarang, pangkats);
			KenaikanPangkat kenaikanPangkat = kenaikanPangkats.isEmpty() ? null : kenaikanPangkats.get(0);
			String gaji = (kenaikanPangkat == null || kenaikanPangkat.getGolongan() == null ? ""
					: kenaikanPangkat.getGolongan().getNama());

			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">" + gaji + "</font>").setParent(arg0);

			new Label(pegawai.getEmail()).setParent(arg0);
			new Label(pegawai.getTanggalmasuk() == null ? "" : Common.dateFormat1.get().format(pegawai.getTanggalmasuk()))
					.setParent(arg0);

			JabatanFungsional jabatanFungsional = pegawai.ambilJabatanFungsional(kenaikanPangkats);
			JabatanStruktural jabatanStruktural = pegawai.ambilJabatanStruktural(kenaikanPangkats);
			Jabatan jabatan = pegawai.ambilJabatan(kenaikanPangkats);

			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">"
					+ (jabatanFungsional == null ? "" : jabatanFungsional.getNama() + "<br>")
					+ (jabatanStruktural == null ? "" : jabatanStruktural.getNama() + "<br>")
					+ (jabatan == null ? "" : jabatan.getNama()) + "</font>").setParent(arg0);
			kenaikanPangkats = null;

			new Label(pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama()).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setOrient("vertical");
			button.setLabel("Cetak Absensi");
			button.setTooltiptext("Cetak Absensi Pegawai");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					LaporanAbsensiPegawaiPerOrang laporan = new LaporanAbsensiPegawaiPerOrang(pegawai);
					laporan.setTitle("Absensi Pegawai");
					laporan.setClosable(true);
					laporan.setHeight("97%");
					laporan.setWidth("97%");
					laporan.setParent(page.getFirstRoot());
					laporan.onModal();
				}

			});
			button.setParent(toolbar);

		}

	}

	public Criteria initCriteria(boolean order) {
		Tbmuser tbmuser = Common.getCurrentUser();
		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pegawai.class)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

		if (order)
			criteria.addOrder(Order.asc("nama"));

		// Gerbang cakupan satuan kerja: SekolahUtil.ambilSatuanKerjas() dapat mengembalikan himpunan
		// kosong bukan hanya untuk superadmin, tetapi juga saat konteks Yayasan/SatuanKerja tidak dapat
		// diresolusi sama sekali -- fail-open lama ("1=1") diam-diam menampilkan SELURUH pegawai lintas
		// satuan kerja pada kondisi tersebut. Perbaikan: tetap "lihat semua" HANYA bila pengguna memang
		// diberi hak eksplisit getMelihatDataSatkerLain(); selain itu tutup total (fail-closed), mengikuti
		// pola yang sama seperti RekeningDosenAction.terapkanCakupanSatuanKerja.
		boolean bolehLihatSatkerLain = tbmuser != null && tbmuser.hakAkses() != null
				&& Boolean.TRUE.equals(tbmuser.hakAkses().getMelihatDataSatkerLain());
		criteria.add(satuanKerjas.size() == 0
				? (bolehLihatSatkerLain ? Restrictions.sqlRestriction("1=1") : Restrictions.sqlRestriction("1=0"))
				: Restrictions.or(
						parent == null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"),
						Restrictions.in("satuanKerja", satuanKerjas)))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchcode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("mycode", searchcode.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("code", searchcode.getValue().trim(), MatchMode.ANYWHERE)))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("statusPegawai", searchstatus.getSelectedItem().getValue()));
		// .add(tbmuser == null || tbmuser.ambilPegawai() == null ||
		// tbmuser.getPegawai().getId() == null
		// ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("id",
		// tbmuser.getPegawai().getId()));

		// Filter "Absensi": Pernah Absen / Belum Pernah Absen / Semua (default).
		// "Pernah Absen" = pegawai punya minimal satu catatan kehadiran harian
		// (status_kehadiran_karyawan_harian). Memakai subquery id pegawai.
		String pernahAbsen = searchPernahAbsen == null || searchPernahAbsen.getSelectedItem() == null
				|| searchPernahAbsen.getSelectedItem().getValue() == null ? ""
						: searchPernahAbsen.getSelectedItem().getValue().toString();
		if ("pernah".equals(pernahAbsen) || "belum".equals(pernahAbsen)) {
			DetachedCriteria subAbsen = DetachedCriteria.forClass(StatuskehadiranKaryawanHarian.class)
					.add(Restrictions.isNotNull("pegawai"))
					.setProjection(Projections.distinct(Projections.property("pegawai")));
			criteria.add("pernah".equals(pernahAbsen) ? Subqueries.propertyIn("id", subAbsen)
					: Subqueries.propertyNotIn("id", subAbsen));
		}
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pegawai> pegawai = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						Pegawai.class);
		ListModel strset = new SimpleListModel(pegawai);
		grid.setRowRenderer(new PegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
