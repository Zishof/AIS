package ais.action.master.helper.keuangan;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class UploadCicilanCalonMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();

	private boolean edit = false;
	private boolean delete = false;
	private boolean create = false;

	private boolean harusPerBulan = false;

	private File file;

	public UploadCicilanCalonMahasiswa() {
		super();
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Upload Cicilan Calon Mahasiswa",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Upload Cicilan Calon Mahasiswa.",
							"Periksa apakah konfigurasi 'aktifkan_rencana_pembayaran_bulanan' pada Konfigurasi sudah benar.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
		}
	}

	public UploadCicilanCalonMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Upload Cicilan Calon Mahasiswa",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Upload Cicilan Calon Mahasiswa.",
							"Periksa apakah konfigurasi 'aktifkan_rencana_pembayaran_bulanan' pada Konfigurasi sudah benar.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
		}
	}

	private void init() throws Exception {

		harusPerBulan = Common.getKonfigurasi("aktifkan_rencana_pembayaran_bulanan", Konfigurasi.AKTIF).getNilai()
				.equals(Konfigurasi.TIDAK_AKTIF);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		create = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Upload Pembayaran" + Common.ukuranLabelFileUpload(),
				"/img/upload.png");
		button.setParent(toolbar);
		button.setVisible(create && edit && delete);
		button.setUpload(Common.ukuranFileUpload());
		button.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub

				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {
					InputStream inputStream = media.getStreamData();
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();
					initSpreadsheet(file);

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Ambil Data", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"PEMBAYARAN_MAHASISWA.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/keuangan/UploadCicilanCalonMahasiswa.java:169");
					PesanFormalHelper.tampilkanGagalException(
							"Mengambil (download) kembali berkas Excel yang telah di-upload",
							e,
							new String[] {
									"Pastikan berkas Excel (xlsx) sudah pernah berhasil di-upload sebelumnya melalui tombol upload.",
									"Ulangi proses upload berkas terlebih dahulu, kemudian coba tombol Ambil Data kembali.",
									"Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." });
				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
	}

	private CicilanPembayaran createCicilanPembayaran(Session session, Long id, BiodataCalonMahasiswa calonMahasiswa,
			Integer semester, ItemBiaya itemBiaya, Integer bayarKe,
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan, Double nilai, Kegiatan kegiatan, Date tanggal,
			String keterangan, Tbmuser tbmuser, List<String> errorStatus) {

		if (nilai > 0.01) {
			CicilanPembayaran cicilanPembayaran = id == null ? null
					: (CicilanPembayaran) session.createCriteria(CicilanPembayaran.class).add(Restrictions.idEq(id))
							.setMaxResults(1).uniqueResult();

			if (cicilanPembayaran == null) {
				cicilanPembayaran = (CicilanPembayaran) session.createCriteria(CicilanPembayaran.class)
						.add(Restrictions.eq("bayarKe", bayarKe))
						.add(Restrictions.sqlRestriction(
								"DATE(this_.tanggal)=DATE('" + Common.databaseDateFormat.get().format(tanggal) + "')"))
						.add(Restrictions.eq("kegiatan", kegiatan)).createAlias("kegiatan", "kegiatan")
						.createAlias("pengaturanPembayaranBulanan", "pengaturanPembayaranBulanan", Criteria.LEFT_JOIN)
						.createAlias("pengaturanPembayaranBulanan.detailBiaya", "detailBiaya", Criteria.LEFT_JOIN)
						.add(Restrictions.eq("kegiatan.calonMahasiswa", calonMahasiswa))
						.add(Restrictions.eq("kegiatan.semster", semester))

						.add(itemBiaya == null ? Restrictions.isNull("itemBiaya")
								: Restrictions.eq("itemBiaya", itemBiaya))

						.add(pengaturanPembayaranBulanan == null ? Restrictions.isNull("pengaturanPembayaranBulanan")
								: Restrictions.and(
										Restrictions.eq("pengaturanPembayaranBulanan.bulan",
												pengaturanPembayaranBulanan.getBulan()),
										Restrictions.eq("detailBiaya.itemBiaya",
												pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya())))
						.setMaxResults(1).uniqueResult();
			}

			System.out.println("cicilanPembayaran=>" + cicilanPembayaran);

			if (cicilanPembayaran == null) {

				Number jumlah = (Number) session.createCriteria(CicilanPembayaran.class)
						.add(Restrictions.eq("kegiatan", kegiatan)).setProjection(Projections.rowCount())
						.setMaxResults(1).uniqueResult();
				cicilanPembayaran = new CicilanPembayaran();
				cicilanPembayaran.setKe(jumlah.intValue() + 1);
				errorStatus.add("Baru");
			} else {
				errorStatus.add("Update");
			}

			cicilanPembayaran.setKegiatan(kegiatan);
			cicilanPembayaran.setItemBiaya(itemBiaya);
			cicilanPembayaran.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
			cicilanPembayaran.setTanggal(tanggal);
			cicilanPembayaran.setNilai(nilai);
			cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
			cicilanPembayaran.setKeterangan(keterangan);
			cicilanPembayaran.setValidator(tbmuser.getUserNama());

			session.getTransaction().begin();
			if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
			session.getTransaction().commit();

			return cicilanPembayaran;
		} else {
			errorStatus.add("Nilai tidak boleh 0");
		}
		return null;
	}

	@SuppressWarnings({})
	private void initSpreadsheet(final File fileUpload) throws Exception {

		Common.clear(center);

		final Tbmuser tbmuser = Common.getCurrentUser();

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Cicilan Calon Mahasiswa");
		final Label downloadPath = new Label("");

		final Timer timerReport = new Timer(300);
		timerReport.setParent(UploadCicilanCalonMahasiswa.this);
		timerReport.setRepeats(true);
		timerReport.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				if (label.getValue().isEmpty()) {
					timerReport.stop();
					timerReport.detach();
					if (!downloadPath.getValue().isEmpty()) {
						try {
							Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain");
						} catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) UploadCicilanCalonMahasiswa download laporan"); }
					}
					MyMessageboxConfig.show(report.getRingkasan(), "Laporan Upload Cicilan Calon", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				}
			}
		});
		timerReport.start();

		new Thread(new Runnable() {

			@SuppressWarnings({ "rawtypes" })
			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
				lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

				XSSFCellStyle lockedNumericStyleUpdate = workbook.createCellStyle();
				lockedNumericStyleUpdate.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				lockedNumericStyleUpdate.setFillForegroundColor(new XSSFColor(Color.YELLOW));

				XSSFCellStyle lockedNumericStyleGagal = workbook.createCellStyle();
				lockedNumericStyleGagal.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				lockedNumericStyleGagal.setFillForegroundColor(new XSSFColor(Color.RED));

				XSSFSheet sheet = workbook.createSheet("PEMBAYARAN");
				// sheet.protectSheet("passwordrahasia");
				sheet.setDefaultColumnWidth(20);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("ID");
				rowhead.createCell(1).setCellValue("JENIS PEMBAYARAN");
				rowhead.createCell(2).setCellValue("MAHASISWA");
				rowhead.createCell(3).setCellValue("SEMESTER");
				rowhead.createCell(4).setCellValue("ITEM BIAYA");
				rowhead.createCell(5).setCellValue("BULAN");
				rowhead.createCell(6).setCellValue("WAKTU");
				rowhead.createCell(7).setCellValue("JUMLAH BAYAR");
				rowhead.createCell(8).setCellValue("KETERANGAN");
				rowhead.createCell(9).setCellValue("SMT");
				rowhead.createCell(10).setCellValue("TA");
				rowhead.createCell(11).setCellValue("Bayar ke");
				rowhead.createCell(12).setCellValue("Status Upload");

				XSSFWorkbook workbookUpload;
				try {
					workbookUpload = new XSSFWorkbook(fileUpload.getAbsolutePath());

					XSSFSheet sheetUpload = workbookUpload.getSheetAt(0);
					int size = sheetUpload.getLastRowNum() + 1;

					int i = 1;
					for (; i < size; i++) {
						XSSFRow row = sheet.createRow(i);
						CicilanPembayaran cicilanPembayaran = null;
						String data = "";
						List<String> errorStatus = new ArrayList<String>();
						Session session = HibernateUtil.currentNativeSession();
						try {

							Long id = Common.getSheetContentAsLong(sheetUpload, 0, i);
							data = "id: " + id;

							JenisKegiatan jenisKegiatan = (JenisKegiatan) Common.getSheetContentAsObject(sheetUpload, 1,
									i, JenisKegiatan.class,
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

							data += ", jenisPembayaran: " + jenisKegiatan;

							String noRegistrasi = Common.getCellContent(Common.getCell(sheetUpload, 2, i));

							BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) Common
									.getSheetContentAsObject(sheetUpload, 2, i, BiodataCalonMahasiswa.class);
							if (calonMahasiswa == null) {
								calonMahasiswa = (BiodataCalonMahasiswa) (noRegistrasi == null
										|| noRegistrasi.trim().isEmpty()
												? null
												: session
														.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions
																.ilike("noRegistrasi", noRegistrasi, MatchMode.EXACT))
														.setMaxResults(1).uniqueResult());
							}
							if (calonMahasiswa == null) {
								calonMahasiswa = (BiodataCalonMahasiswa) (noRegistrasi == null
										|| noRegistrasi.trim().isEmpty()
												? null
												: session
														.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions
																.ilike("noUjian", noRegistrasi, MatchMode.EXACT))
														.setMaxResults(1).uniqueResult());
							}
							if (calonMahasiswa == null) {
								calonMahasiswa = (BiodataCalonMahasiswa) (noRegistrasi == null
										|| noRegistrasi.trim().isEmpty()
												? null
												: session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
														.add(Restrictions.ilike("nim", noRegistrasi, MatchMode.EXACT))
														.setMaxResults(1).uniqueResult());
							}

							data += ", calonMahasiswa: " + calonMahasiswa;

							Integer semester = Common.getSheetContentAsInteger(sheetUpload, 3, i);

							data += ", semester: " + semester;

							ItemBiaya itemBiaya = (ItemBiaya) Common.getSheetContentAsObject(sheetUpload, 4, i,
									ItemBiaya.class);

							data += ", itemBiaya: " + itemBiaya;

							Integer bulan = Common.getSheetContentAsInteger(sheetUpload, 5, i);

							data += ", bulan: " + bulan;

							String waktu = Common.getSheetContentAsString(sheetUpload, 6, i);

							data += ", waktu: " + waktu;

							Double nilai = Common.getSheetContentAsDouble(sheetUpload, 7, i);

							data += ", nilai: " + nilai;

							String keterangan = Common.getSheetContentAsString(sheetUpload, 8, i);

							data += ", keterangan: " + keterangan;

							Integer bayarKe = Common.getSheetContentAsInteger(sheetUpload, 11, i);

							if (bayarKe == null || bayarKe < 1) {
								bayarKe = 1;
							}

							data += ", bayarKe: " + bayarKe;
							System.out.println("data -> " + data);

							if (calonMahasiswa != null && jenisKegiatan != null && semester != null && nilai != null) {

								String tahunAkademik = calonMahasiswa.getTahunAkademik();

								label.setValue("Upload data " + jenisKegiatan + " - " + tahunAkademik + " - "
										+ calonMahasiswa + " - " + semester + " - " + nilai + " ("
										+ Common.numberFormat.get().format(i * 100.0 / size) + " %)");

								PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
								Collection detailBiayas = PembayaranUtil.getInstance().getDetailBiayaCalonMahasiswa(
										calonMahasiswa, jenisKegiatan, null, semester, false);
								Map<Long, DetailBiaya> maps = new HashMap<Long, DetailBiaya>();
								for (Object o : detailBiayas) {
									if (o instanceof DetailBiaya) {
										DetailBiaya detailBiaya = (DetailBiaya) o;
										if (bayarKe.equals(detailBiaya.getBayarKe())) {
											maps.put(detailBiaya.getId(), detailBiaya);
										}
									} else if (o instanceof PengaturanPembayaranBulanan) {
										PengaturanPembayaranBulanan p = (PengaturanPembayaranBulanan) o;
										DetailBiaya detailBiaya = p.getDetailBiaya();
										if (bayarKe.equals(detailBiaya.getBayarKe())) {
											maps.put(detailBiaya.getId(), detailBiaya);
										}
									}
								}

//								System.out.println("detailBiayas = " + maps + " - " + label.getValue());

								if (bulan != null && itemBiaya != null && !maps.isEmpty()) {
									pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
											.createCriteria(PengaturanPembayaranBulanan.class)
											.createAlias("detailBiaya", "detailBiaya")
											.add(Restrictions.eq("realBulan", bulan))
											.add(Restrictions.in("detailBiaya", maps.values()))
											.add(Restrictions.eq("detailBiaya.itemBiaya", itemBiaya))
											.add(Restrictions.gt("nominal", 0.01)).setMaxResults(1).uniqueResult();
								}

								boolean lewatiBaris = false;
								if (harusPerBulan && pengaturanPembayaranBulanan == null) {
									// dulu: continue; -> baris di-skip DIAM-DIAM tanpa status apa pun di hasil.
									// Sekarang dilaporkan eksplisit agar operator tahu sebab baris tidak diproses.
									errorStatus.add("Rencana pembayaran BULANAN aktif, tetapi pengaturan bulanan yang cocok untuk baris ini tidak ditemukan -- periksa kolom BULAN (kolom F) dan ITEM BIAYA (kolom E)");
									lewatiBaris = true;
								}

								JadwalPembayaran jadwalPembayaran = lewatiBaris ? null
										: PembayaranUtil.getInstance()
												.getJadwalPembayaranTanpaDibatasiWaktu(jenisKegiatan, tahunAkademik,
														semester % 2 == 1, calonMahasiswa.getJenisSeleksi(),
														calonMahasiswa.getProgram(), calonMahasiswa.getNoRegistrasi());

								// GERBANG (opt-in, default TIDAK AKTIF): cegah pembuatan pembayaran bila Jadwal
								// Pembayaran tidak ada sama sekali untuk jenis pembayaran/tahun akademik ini.
								if (!lewatiBaris && jadwalPembayaran == null && Common.bolehKonfigurasi(
										"cegah_pembayaran_tanpa_jadwal_pembayaran", Konfigurasi.TIDAK_AKTIF)) {
									errorStatus.add("DICEGAH: Jadwal Pembayaran untuk jenis pembayaran & tahun akademik ini TIDAK DITEMUKAN, dan konfigurasi 'cegah_pembayaran_tanpa_jadwal_pembayaran' sedang AKTIF -- buat jadwalnya terlebih dahulu di menu Pengaturan Jadwal Pembayaran, atau nonaktifkan konfigurasi tersebut");
									lewatiBaris = true;
								}

								Kegiatan kegiatan = lewatiBaris ? null : calonMahasiswa.ambilKegiatans(semester, jenisKegiatan);

								if (!lewatiBaris && (kegiatan == null || kegiatan.getId() == null)) {

									kegiatan = new Kegiatan();
									kegiatan.setJenisKegiatan(jenisKegiatan);
									kegiatan.setJadwalPembayaran(jadwalPembayaran);
									kegiatan.setCalonMahasiswa(calonMahasiswa);
									kegiatan.setSemster(semester);
									kegiatan.setTahunAkademik(tahunAkademik);
									kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
									kegiatan.setValidated(1);
									kegiatan.setJenisKegiatan(jenisKegiatan);
									kegiatan.setValidator(tbmuser.getUserNama());
									kegiatan.setPengurangan(0.0);
									kegiatan.setKeterangan("");
								}

								if (kegiatan != null) {

									Date tanggal = null;
									try {
										tanggal = Common.dateFormat3.get().parse(waktu);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/keuangan/UploadCicilanCalonMahasiswa.java:469");

									}

									session.getTransaction().begin();
									Common.refreshSaveOrUpdate(session, kegiatan);
									session.getTransaction().commit();

									cicilanPembayaran = createCicilanPembayaran(session, id, calonMahasiswa, semester,
											itemBiaya, bayarKe, pengaturanPembayaranBulanan, nilai, kegiatan, tanggal,
											keterangan, tbmuser, errorStatus);

									Double amountTotal = 0.0;
									Double denda = 0.0;
									Number jumlahYangSudahDibayar = null;
									if (kegiatan.getId() != null) {
										jumlahYangSudahDibayar = (Number) session
												.createCriteria(CicilanPembayaran.class)
												.add(Restrictions.eq("kegiatan", kegiatan))
												.setProjection(Projections.sum("nilai")).uniqueResult();
										if (jumlahYangSudahDibayar != null) {
											amountTotal += jumlahYangSudahDibayar.doubleValue();
										}

										Number jumlahDenda = (Number) session.createCriteria(CicilanPembayaran.class)
												.add(Restrictions.eq("kegiatan", kegiatan))
												.setProjection(Projections.sum("denda")).uniqueResult();
										denda = jumlahDenda == null ? 0.0 : jumlahDenda.doubleValue();
									}
									kegiatan.setAmount(amountTotal);
									kegiatan.setJumlahTelahDibayar(amountTotal);
									kegiatan.setDenda(denda);
									kegiatan.setTanggal(cicilanPembayaran.getTanggal());
									kegiatan.setValidator(tbmuser.getUserNama());

									try {

										Map<Long, DetailBiaya> map = new java.util.HashMap<Long, DetailBiaya>();

										for (Object o : detailBiayas) {
											if (o instanceof DetailBiaya) {
												DetailBiaya detailBiaya = (DetailBiaya) o;
												map.put(detailBiaya.getId(), detailBiaya);
											} else if (o instanceof PengaturanPembayaranBulanan) {
												PengaturanPembayaranBulanan p = (PengaturanPembayaranBulanan) o;
												DetailBiaya detailBiaya = p.getDetailBiaya();
												map.put(detailBiaya.getId(), detailBiaya);
											}
										}

										Double nilaiBiayaHarusDiBayars = 0.0;
										for (DetailBiaya detailBiaya : map.values()) {
											Double n = detailBiaya.hitungTotalKegiatan(kegiatan, session);
											nilaiBiayaHarusDiBayars += (n);
										}

										kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (amountTotal - denda));
										kegiatan.setAmount(amountTotal);
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									session.getTransaction().begin();
									Common.refreshUpdate(session, kegiatan);
									session.getTransaction().commit();

									Collection<DetailKegiatan> detailKegiatans = kegiatan == null
											|| kegiatan.getId() == null ? null : kegiatan.ambilDetailKegiatan(true);

									for (Object o : detailBiayas) {
										DetailBiaya detailBiaya = null;
										if (o instanceof DetailBiaya) {
											detailBiaya = (DetailBiaya) o;
										} else {
											PengaturanPembayaranBulanan p = (PengaturanPembayaranBulanan) o;
											detailBiaya = p.getDetailBiaya();
										}

										Double nilaia = detailBiaya.hitungTotalKegiatan(kegiatan, session);

										DetailKegiatan detailKegiatan = pengaturanPembayaranBulanan != null
												? kegiatan.ambilSatuDetailKegiatan(pengaturanPembayaranBulanan,
														detailKegiatans)
												: kegiatan.ambilSatuDetailKegiatan(detailBiaya, true);
										if (detailKegiatan == null) {
											detailKegiatan = new DetailKegiatan();
											detailKegiatan.setBiaya(nilaia);
											detailKegiatan.setDetailBiaya(detailBiaya);
											detailKegiatan.setKeterangan(detailBiaya.getKeterangan());
											detailKegiatan.setKegiatan(kegiatan);

											// detailKegiatan.setAkunKredit(detailBiaya.getItemBiaya()
											// == null ? null
											// :
											// detailBiaya.getItemBiaya().getAkunKredit());

											System.out.println(
													"================================== simpan Detail Pembayaran "
															+ (nilai) + " untuk calonMahasiswa " + calonMahasiswa
															+ " ===================================");

											session.getTransaction().begin();
											session.save(detailKegiatan);
											session.getTransaction().commit();
										}

									}

									if (cicilanPembayaran != null && cicilanPembayaran.getId() != null) {

										XSSFCell cell = row.createCell(0);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(cicilanPembayaran.getId());

										cell = row.createCell(1);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(cicilanPembayaran.getKegiatan().getJenisKegiatan() == null
												? ""
												: cicilanPembayaran.getKegiatan().getJenisKegiatan().toString());

										cell = row.createCell(2);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(
												cicilanPembayaran.getKegiatan().getCalonMahasiswa().toString());

										cell = row.createCell(3);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(cicilanPembayaran.getKegiatan().getSemster());

										cell = row.createCell(4);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(cicilanPembayaran.getItemBiaya() == null ? ""
												: cicilanPembayaran.getItemBiaya().toString());

										cell = row.createCell(5);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(cicilanPembayaran.getPengaturanPembayaranBulanan() == null
												|| cicilanPembayaran.getPengaturanPembayaranBulanan()
														.getRealBulan() == null ? ""
																: cicilanPembayaran.getPengaturanPembayaranBulanan()
																		.getRealBulan().toString());

										cell = row.createCell(6);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(cicilanPembayaran.getTanggal() == null ? ""
												: Common.dateFormat3.get().format(cicilanPembayaran.getTanggal()));

										cell = row.createCell(7);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(cicilanPembayaran.getNilai());

										cell = row.createCell(8);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(cicilanPembayaran.getKeterangan());

										cell = row.createCell(9);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(kegiatan.getSemster() % 2 == 0 ? Perkuliahan.GENAP
												: Perkuliahan.GANJIL);

										cell = row.createCell(10);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(kegiatan.getTahunAkademik());

										cell = row.createCell(11);
										cell.setCellStyle(lockedNumericStyle);
										cell.setCellValue(bayarKe);

									}

								}
							} else {
								// DIAGNOSTIK RINCI: jelaskan PERSIS field mana yang membuat baris gagal,
								// supaya operator tahu harus memperbaiki apa (bukan cuma "Gagal" mentah).
								if (jenisKegiatan == null) {
									errorStatus.add("JENIS PEMBAYARAN (kolom B) tidak ditemukan atau tidak aktif -- periksa penulisan kode/nama jenis pembayaran");
								}
								if (calonMahasiswa == null) {
									errorStatus.add("CALON MAHASISWA (kolom C) tidak ditemukan -- periksa nomor registrasi/NIM; bila yang bersangkutan SUDAH menjadi mahasiswa, gunakan menu Upload Pembayaran Mahasiswa");
								}
								if (semester == null) {
									errorStatus.add("SEMESTER (kolom D) kosong dan tidak bisa diturunkan dari SMT+TA (kolom J/K) -- isi kolom SEMESTER, atau pastikan SMT & TA terisi benar");
								}
								if (nilai == null) {
									errorStatus.add("JUMLAH BAYAR (kolom H) kosong atau bukan angka");
								}
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							// LAPORKAN DETAIL: kendala sekecil apa pun harus terlihat jelas oleh operator
							// di kolom Status Upload baris ybs, bukan tertelan diam-diam.
							errorStatus.add("ERROR SISTEM saat memproses baris ini: " + e.getClass().getSimpleName()
									+ (e.getMessage() == null ? "" : (": " + e.getMessage()))
									+ " -- ulangi upload; bila berulang, hubungi Administrator/Developer dengan melampirkan screenshot pesan ini");
							ais.common.ErrorAuditUtil.record(e, "auto-audit(upload-cicilan-calon-mahasiswa) baris upload gagal");
						}

						if (cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
							report.gagal(i, data, "Cicilan gagal disimpan: " + errorStatus, "Pastikan data mahasiswa aktif dan tagihan belum lunas.");

							for (int col = 0; col < 12; col++) {
								try {
									String content = Common.getSheetContentAsString(sheetUpload, col, i);

									XSSFCell cellBaru = row.createCell(col);
									cellBaru.setCellStyle(lockedNumericStyleGagal);
									cellBaru.setCellValue(content);
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/keuangan/UploadCicilanCalonMahasiswa.java:655");
								}
							}

							XSSFCell cell = row.createCell(12);
							cell.setCellStyle(lockedNumericStyleGagal);
							cell.setCellValue("Gagal, " + data + ", " + errorStatus);
						} else {
							report.sukses(i, data, null);

							XSSFCell cell = row.createCell(12);
							cell.setCellStyle(
									errorStatus.contains("Update") ? lockedNumericStyleUpdate : lockedNumericStyle);
							cell.setCellValue("Sukses, " + data + ", " + errorStatus);
						}

						HibernateUtil.closeSession();
					}

					sizedata.setValue(i + 1);

					try {
						FileOutputStream fileOut = new FileOutputStream(filename);
						workbook.write(fileOut);
						fileOut.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						Common.tampilErrorJikaAdmin(e);
						PesanFormalHelper.tampilkanGagalException(
								"Menyimpan berkas Excel hasil proses Upload Cicilan Calon Mahasiswa",
								e,
								new String[] {
										"Pastikan berkas dengan nama yang sama tidak sedang dibuka oleh aplikasi lain (misalnya Microsoft Excel).",
										"Periksa ketersediaan ruang penyimpanan (disk space) pada server.",
										"Ulangi proses upload data. Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." });
					}

				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/keuangan/UploadCicilanCalonMahasiswa.java:686");
					PesanFormalHelper.tampilkanGagalException(
							"Memproses berkas Excel Upload Cicilan Calon Mahasiswa",
							e1,
							new String[] {
									"Pastikan berkas Excel yang di-upload sesuai format template yang telah ditentukan (kolom dan urutan data).",
									"Periksa isi data pada setiap baris, pastikan tidak ada sel yang kosong atau bertipe data yang salah.",
									"Ulangi proses upload data. Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." });
					// FIX "gagal diam-diam": sebelumnya setelah dialog error ini ditampilkan,
					// eksekusi tetap lanjut ke label.setValue("") di bawah (di luar catch ini)
					// tanpa syarat, membuat progress bar tetap melaporkan SUKSES palsu meski
					// proses baru saja gagal total. Set label ke status Error dan hentikan di sini.
					label.setValue("Error: " + PesanFormalHelper.pesanGagalException(
							"Memproses berkas Excel Upload Cicilan Calon Mahasiswa", null, e1,
							new String[] {
									"Pastikan berkas Excel yang di-upload sesuai format template yang telah ditentukan (kolom dan urutan data).",
									"Periksa isi data pada setiap baris, pastikan tidak ada sel yang kosong atau bertipe data yang salah.",
									"Ulangi proses upload data. Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." })
							.replace("\n", " "));
					return;
				}

				System.out.println("Your excel file has been generated! ");

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) UploadCicilanCalonMahasiswa laporan"); }

				HibernateUtil.closeSession();
				label.setValue("");

			} catch (Exception eOuter) {
					// FIX "gagal diam-diam"/hang selamanya: try terluar ini sebelumnya TIDAK
					// punya catch (hanya finally), jadi exception di luar blok try dalam akan
					// lolos tak tertangani keluar dari run() thread ini; label.setValue("")
					// tidak pernah tereksekusi dan popup progres tidak akan pernah tertutup.
					Common.tampilErrorJikaAdmin(eOuter);
					label.setValue("Error: " + PesanFormalHelper.pesanGagalException(
							"pemrosesan Upload Cicilan Calon Mahasiswa", null, eOuter,
							new String[] {
									"Pastikan berkas Excel yang di-upload sesuai format template yang telah ditentukan.",
									"Periksa koneksi ke database server dan ketersediaan ruang penyimpanan (disk space) pada server.",
									"Ulangi proses upload data. Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." })
							.replace("\n", " "));
				} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}
}
