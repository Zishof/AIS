package ais.action.master.helper.virtualaccount;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.BJBSUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.VirtualAccountBank;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class DownloadTagihanMahasiswaBankBjb extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;
	public static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	private Center center = new Center();
	private Intbox searchangkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
	private Combobox jenisPembayaran;
	private Intbox searchbulan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1);
	protected Combobox searchTahunAjaran = new Combobox();
	protected Combobox searchJenisSemester = new Combobox();

	private Textbox nims = new Textbox();

	private File file;

	public DownloadTagihanMahasiswaBankBjb() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Unduh Tagihan Mahasiswa (Bank BJB)",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Unduh Tagihan Mahasiswa Bank BJB.",
							"Periksa apakah data Tahun Ajaran, Semester, dan konfigurasi integrasi Bank BJB sudah benar.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
		}
	}

	public DownloadTagihanMahasiswaBankBjb(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Unduh Tagihan Mahasiswa (Bank BJB)",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Unduh Tagihan Mahasiswa Bank BJB.",
							"Periksa apakah data Tahun Ajaran, Semester, dan konfigurasi integrasi Bank BJB sudah benar.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
		}
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		searchJenisSemester.setReadonly(true);
		searchTahunAjaran.setReadonly(true);

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchJenisSemester.appendChild(comboitem);

		Common.selectComboItem(searchJenisSemester,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		Common.generateTahunAjaran(searchTahunAjaran);

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("200px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa Tahun Angkatan"));
		row.appendChild(searchangkatan);
		searchangkatan.setWidth("90%");

		jenisPembayaran = Common.initJenisPembayaranMahasiswa(jenisPembayaran = new Combobox());
		jenisPembayaran.setReadonly(true);

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran"));
		row.appendChild(jenisPembayaran);
		jenisPembayaran.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(searchTahunAjaran);
		searchTahunAjaran.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(searchJenisSemester);
		searchJenisSemester.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Tagihan Bulan"));
		row.appendChild(searchbulan);
		searchbulan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "1,9");
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM:"));
		row.appendChild(nims);
		nims.setWidth("90%");
		nims.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "10");
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Masukkan nim mahasiswa jika hanya untuk ambil tagihan beberapa mahasiswa, pisahkan NIM dengan tanda koma"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig search = new MyToolbarbuttonConfig("Tampilkan Data", "/img/svg/search.svg");
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		search.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Ambil Data", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (searchangkatan.getValue() == null) {
					MyMessageboxConfig.show("Tahun Angkatan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				if (file == null) {
					MyMessageboxConfig.show("Click \"Tampilkan Data\" terlebih dahulu", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "PEMBAYARAN_MAHASISWA_"
									+ Common.dateFormat32.get().format(ais.ui.util.WaktuUtil.getDate()) + ".xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBjb.java:232");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		Common.clear(center);

		if (searchangkatan.getValue() == null) {
			MyMessageboxConfig.show("Tahun Angkatan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		if (searchbulan.getValue() == null) {
			MyMessageboxConfig.show("Tagihan bulan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		final JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
				: jenisPembayaran.getSelectedItem().getValue());

		if (jenisKegiatan == null) {
			MyMessageboxConfig.show("Jenis pembayaran harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		System.out.println("init spreadsheet running");

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		final Integer bulan = searchbulan.getValue();

		final String ta = (String) searchTahunAjaran.getSelectedItem().getValue();
		final String semesterMulai = (String) searchJenisSemester.getSelectedItem().getValue();

		new Thread(new Runnable() {

			@Override
			public void run() {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
				lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				lockedNumericStyle.setLocked(true);

				XSSFCellStyle notLocked = workbook.createCellStyle();
				notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				// notLocked.setLocked(false);

				XSSFSheet sheet = workbook.createSheet("TAGIHAN_MAHASISWA");
				// sheet.protectSheet("passwordrahasia");
				sheet.setDefaultColumnWidth(25);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("nopel");
				rowhead.createCell(1).setCellValue("nama");
				rowhead.createCell(2).setCellValue("ket");
				rowhead.createCell(3).setCellValue("tagihan");
				rowhead.createCell(4).setCellValue("expired");

				Session session = MahasiswaVirtualAccountHelper.openSession();
				try {
				List<String> nimsMahasiswa = new ArrayList<String>();
				for (String nim : StringUtils.split(nims.getValue().trim(), ",")) {
					nimsMahasiswa.add(nim.trim());
				}

				List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("jurusan")).addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))
						.add(nimsMahasiswa.isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.in("nim", nimsMahasiswa))
						.add(searchangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunangkatan", searchangkatan.getValue()))
						.list();

				int size = mahasiswas.size();

				int rowIndex = 1;
				int rowIndexProses = 1;
				for (Mahasiswa mahasiswa : mahasiswas) {
					label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
							+ Common.numberFormat.get().format(rowIndexProses * 100.0 / size) + " %)");
					if (createData(session, mahasiswa, sheet, rowIndex, lockedNumericStyle, notLocked, jenisKegiatan,
							bulan, ta, semesterMulai)) {
						rowIndex++;
					}
					rowIndexProses++;
				}

				Common.setStyled(sheet);
				sizedata.setValue(rowIndex + 1);

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException(
							"Menyimpan berkas Excel hasil unduhan Tagihan Mahasiswa (Bank BJB)",
							e,
							new String[] {
									"Pastikan berkas dengan nama yang sama tidak sedang dibuka oleh aplikasi lain (misalnya Microsoft Excel).",
									"Periksa ketersediaan ruang penyimpanan (disk space) pada server.",
									"Ulangi proses unduh data. Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." });
				}

				MahasiswaVirtualAccountHelper.closeSessionQuietly(session);
			MahasiswaVirtualAccountHelper.closeHibernateContextQuietly();

				mahasiswas.clear();
				label.setValue("");
			} catch (Exception e) {
				// FIX "gagal diam-diam"/hang: sebelumnya try{...}finally{...} TANPA catch,
				// exception menembus keluar Runnable.run() tak tertangani, thread mati, dan
				// label.setValue("") TIDAK PERNAH dijalankan -> popup progres menggantung selamanya.
				Common.tampilErrorJikaAdmin(e);
				label.setValue("Error: " + PesanFormalHelper.pesanGagalException(
						"pengunduhan Tagihan Virtual Account Mahasiswa (Bank BJB)", null, e,
						new String[] {
								"Periksa kembali data/filter (Tahun Angkatan/Jenis Pembayaran/Tahun Akademik/Semester/Tagihan Bulan) yang dipilih dan coba ulangi.",
								"Periksa ketersediaan ruang penyimpanan (disk space) pada server untuk berkas Excel sementara.",
								"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
						.replace("\n", " "));
			} finally {
					// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
					// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
					if (session != null && session.isOpen()) {
						try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBjb.java:366");}
						try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBjb.java:367");}
					}
				}
			}
		}).start();

	}

	@SuppressWarnings("unchecked")
	public static boolean createData(Session session, Mahasiswa mahasiswa, XSSFSheet sheet, int rowIndex,
			XSSFCellStyle lockedNumericStyle, XSSFCellStyle notLocked, JenisKegiatan jenisKegiatan, Integer bulan,
			String ta, String semesterMulai) {

		final Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
		Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);

		Integer smt = Common.getSemester(tahunAngkatanMhs, semesterMulai, mahasiswa.getPindahKeKampusIniMasukSemester(),
				tahun, mahasiswa.getSemesterMulai());

		JadwalPembayaran myjadwalPembayaran = (JadwalPembayaran) session.createCriteria(JadwalPembayaran.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("jenjang"),
						Restrictions.eq("jenjang", mahasiswa.getJenjang())))
				.add(Restrictions.or(Restrictions.isNull("ganjil"),
						Restrictions.eq("ganjil", semesterMulai.equals(Perkuliahan.GANJIL))))
				.add(Restrictions.or(Restrictions.isNull("tahunAkademik"), Restrictions.eq("tahunAkademik", ta)))
				.add(Restrictions.eq("jenisKegiatan", jenisKegiatan)).addOrder(Order.desc("endDate"))
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

		if (myjadwalPembayaran != null) {

			Collection<DetailBiaya> detailBiayas = pembayaranUtil.getDetailBiayaMahasiswa(mahasiswa, smt, jenisKegiatan,
					false);

			String detailbiaya = "";
			for (DetailBiaya biaya : detailBiayas) {
				detailbiaya += (detailbiaya.isEmpty() ? biaya.getId() : "," + biaya.getId());
			}

			Collection<PengaturanPembayaranBulanan> pengaturanPembayaranBulanans = pembayaranUtil
					.getPengaturanPembayaranSemua(mahasiswa, null, smt, session, myjadwalPembayaran.getJenisKegiatan(),
							detailBiayas, bulan, false, true);
			if (!pengaturanPembayaranBulanans.isEmpty()) {

				String pemb = "";
				Double total = 0.0;
				JadwalPembayaran jdw = myjadwalPembayaran != null && myjadwalPembayaran.getKhususUntukNim() != null
						&& myjadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
								? myjadwalPembayaran
								: null;
				String bulanan = "";
				for (PengaturanPembayaranBulanan biaya : pengaturanPembayaranBulanans) {
					ItemBiaya itemBiaya = biaya.getDetailBiaya().getItemBiaya();
					Double nilai = biaya.ambilNominalModifikasi(mahasiswa, mahasiswa.currentSemester());
					if (nilai > 0.01) {
						bulanan += (bulanan.isEmpty() ? biaya.getId() : "," + biaya.getId());

						Double hasilDenda = biaya.checkDenda(nilai, ais.ui.util.WaktuUtil.getDate(), jdw,
								myjadwalPembayaran == null ? null : myjadwalPembayaran.getJenisKegiatan());

						String desc = biaya.getKeterangan();
						desc = (desc.isEmpty() ? (biaya.getDetailBiaya().getItemBiaya().getNama()) : desc) + ", Rp. "
								+ Common.numberFormat.get().format(nilai)
								+ (hasilDenda.intValue() > nilai.intValue() ? biaya.getInfoDenda() : "");

						pemb += itemBiaya.getKode().trim() + "," + desc + ";";

						total += nilai;
					}
				}

				String kodeUnik = "MHS-" + mahasiswa.getId() + "-" + bulan + "-" + jenisKegiatan.getId();
				VirtualAccountBank virtualAccountBankBjb = (VirtualAccountBank) session
						.createCriteria(VirtualAccountBank.class).add(Restrictions.eq("terjadiKendala", false))
						.add(Restrictions.eq("nama", kodeUnik)).uniqueResult();
				if (virtualAccountBankBjb == null) {
					virtualAccountBankBjb = new VirtualAccountBank(PerguruanTinggiUtil.getPerguruanTinggi().getId());

					virtualAccountBankBjb.setKanalPembayaran(
							myjadwalPembayaran == null || myjadwalPembayaran.getJenisKegiatan() == null ? null
									: myjadwalPembayaran.getJenisKegiatan().getKanalPembayaran());

				} else if (virtualAccountBankBjb.getKegiatan() != null) {
					return false;
				}

				virtualAccountBankBjb.setNama(kodeUnik);
				virtualAccountBankBjb.setJenisKegiatan(jenisKegiatan);
				virtualAccountBankBjb.setKeterangan(pemb);
				virtualAccountBankBjb.setTotal(total);
				virtualAccountBankBjb.setBulanan(bulanan);
				virtualAccountBankBjb.setDetailbiaya(detailbiaya);

				virtualAccountBankBjb.setMahasiswa(mahasiswa);
				virtualAccountBankBjb.setJadwalPembayaran(myjadwalPembayaran);
				virtualAccountBankBjb.setSemester(smt);
				virtualAccountBankBjb.setTahunAkademik(ta);
				virtualAccountBankBjb.setBank("Bank BJBS");

				MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
				Common.refreshSaveOrUpdate(session, virtualAccountBankBjb);
				MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);

				XSSFRow row = sheet.createRow(rowIndex);

				XSSFCell cell = row.createCell(0);
				cell.setCellStyle(lockedNumericStyle);
				cell.setCellValue(virtualAccountBankBjb.getKode());

				cell = row.createCell(1);
				cell.setCellStyle(notLocked);
				cell.setCellValue("[" + mahasiswa.getNim() + "] " + mahasiswa.getNama());

				cell = row.createCell(2);
				cell.setCellStyle(notLocked);
				cell.setCellValue(pemb);

				cell = row.createCell(3);
				cell.setCellStyle(lockedNumericStyle);
				cell.setCellValue(total);

				cell = row.createCell(4);
				cell.setCellStyle(notLocked);
				cell.setCellValue(
						myjadwalPembayaran == null ? "" : Common.dateFormat11.get().format(myjadwalPembayaran.getEndDate()));
				return true;

			} else {

				int countPengaturanBulanan = PembayaranUtil.getInstance().countBulanan(session, mahasiswa,
						myjadwalPembayaran.getJenisKegiatan(), smt, detailBiayas, false, true);

				if (countPengaturanBulanan == 0) {
					String pemb = "";
					Double total = 0.0;
					for (DetailBiaya detailBiaya : detailBiayas) {
						pemb += detailBiaya.getItemBiaya().getKode().trim() + "," + detailBiaya.getItemBiaya().getNama()
								+ "," + Common.numberFormat.get().format(detailBiaya.getNilaiBiaya()) + ";";
						total += detailBiaya.getNilaiBiaya();
					}
					String kodeUnik = "MHS-" + mahasiswa.getId() + "-" + +jenisKegiatan.getId();
					VirtualAccountBank virtualAccountBankBjb = (VirtualAccountBank) session
							.createCriteria(VirtualAccountBank.class).add(Restrictions.eq("terjadiKendala", false))
							.add(Restrictions.eq("nama", kodeUnik)).uniqueResult();
					if (virtualAccountBankBjb == null) {
						virtualAccountBankBjb = new VirtualAccountBank(
								PerguruanTinggiUtil.getPerguruanTinggi().getId());

						virtualAccountBankBjb.setKanalPembayaran(
								myjadwalPembayaran == null || myjadwalPembayaran.getJenisKegiatan() == null ? null
										: myjadwalPembayaran.getJenisKegiatan().getKanalPembayaran());

					} else if (virtualAccountBankBjb.getKegiatan() != null) {
						return false;
					}

					virtualAccountBankBjb.setNama(kodeUnik);
					virtualAccountBankBjb.setJenisKegiatan(jenisKegiatan);
					virtualAccountBankBjb.setKeterangan("");
					virtualAccountBankBjb.setTotal(total);
					virtualAccountBankBjb.setBulanan("");
					virtualAccountBankBjb.setDetailbiaya(detailbiaya);

					virtualAccountBankBjb.setMahasiswa(mahasiswa);
					virtualAccountBankBjb.setJadwalPembayaran(myjadwalPembayaran);
					virtualAccountBankBjb.setSemester(smt);
					virtualAccountBankBjb.setTahunAkademik(ta);
					virtualAccountBankBjb.setBank("Bank BJB");

					MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
					Common.refreshSaveOrUpdate(session, virtualAccountBankBjb);
					MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);

					XSSFRow row = sheet.createRow(rowIndex);

					XSSFCell cell = row.createCell(0);
					cell.setCellStyle(lockedNumericStyle);
					cell.setCellValue(virtualAccountBankBjb.getKode());

					cell = row.createCell(1);
					cell.setCellStyle(notLocked);
					cell.setCellValue("[" + mahasiswa.getNim() + "] " + mahasiswa.getNama());

					cell = row.createCell(2);
					cell.setCellStyle(notLocked);
					cell.setCellValue(pemb);

					cell = row.createCell(3);
					cell.setCellStyle(lockedNumericStyle);
					cell.setCellValue(total);

					cell = row.createCell(4);
					cell.setCellStyle(notLocked);
					cell.setCellValue(myjadwalPembayaran == null ? ""
							: Common.dateFormat11.get().format(myjadwalPembayaran.getEndDate()));

					if (!detailBiayas.isEmpty()) {
						return true;
					}
				}

			}
		}
		return false;

	}

//	public static String post(String postData, String strURL) throws Exception {
//		String hasil = "";
//
//		CloseableHttpClient httpclient = HttpClients.createDefault();
//		try {
//
//			HttpPost httpPost = new HttpPost(strURL);
//
//			StringEntity entity = new StringEntity(postData);
//			httpPost.setEntity(entity);
//			httpPost.setHeader("Accept", "application/json");
//			httpPost.setHeader("Content-type", "application/json");
//
//			CloseableHttpResponse response = httpclient.execute(httpPost);
//			hasil = EntityUtils.toString(response.getEntity());
//
//			return hasil;
//		} finally {
//			httpclient.close();
//		}
//	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static VirtualAccountBank downloadData(Mahasiswa mahasiswa, Integer smt, JadwalPembayaran myjadwalPembayaran,
			Collection detailBiayas, Grid gridCicilan) throws Exception {

		Session session = MahasiswaVirtualAccountHelper.openSession();
		try {

		String detailbiaya = "";
		for (Object o : detailBiayas) {
			if (o instanceof DetailBiaya) {
				DetailBiaya biaya = (DetailBiaya) o;
				detailbiaya += (detailbiaya.isEmpty() ? biaya.getId() : "," + biaya.getId());
			}
		}

		String pemb = "";
		String cicilan = "";
		Double total = 0.0;
		JadwalPembayaran jdw = myjadwalPembayaran != null && myjadwalPembayaran.getKhususUntukNim() != null
				&& myjadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",") ? myjadwalPembayaran
						: null;
		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		for (Row row : mycicilanrows) {
			MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");

			if (jumlahCicilan.getValue() != null && jumlahCicilan.getValue().intValue() != 0) {
				CicilanPembayaran cicilanPembayaranSebelumnya = (CicilanPembayaran) row
						.getAttribute("cicilanPembayaran");
				if (cicilanPembayaranSebelumnya.getId() == null) {
					try {
						PengaturanPembayaranBulanan biaya = cicilanPembayaranSebelumnya
								.getPengaturanPembayaranBulanan();
						if (biaya != null) {

							Double nilai = jumlahCicilan.getValue();
							try {
								cicilan = MahasiswaVirtualAccountHelper.tambahTokenCicilan(cicilan, ("Bulanan-" + biaya.getId().toString() + "-" + nilai));

								Double hasilDenda = biaya.checkDenda(nilai, ais.ui.util.WaktuUtil.getDate(), jdw,
										myjadwalPembayaran == null ? null : myjadwalPembayaran.getJenisKegiatan());

								String desc = biaya.getKeterangan();
								desc = (desc.isEmpty() ? (biaya.getDetailBiaya().getItemBiaya().getNama()) : desc)
										+ ", Rp. " + Common.numberFormat.get().format(nilai)
										+ (hasilDenda.intValue() > nilai.intValue() ? biaya.getInfoDenda() : "");

								pemb += biaya.getDetailBiaya().getItemBiaya().getKode().trim() + "," + desc + ";";
								total += nilai;
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBjb.java:644");
								// TODO: handle exception
							}
						} else {

							Double nilai = jumlahCicilan.getValue();

							try {
								Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
								ItemBiaya itemBiaya;
								DetailBiaya detailBiaya = (DetailBiaya) (myItemBiaya.getSelectedItem() == null ? null
										: myItemBiaya.getSelectedItem().getValue());
								if (cicilanPembayaranSebelumnya != null
										&& cicilanPembayaranSebelumnya.getItemBiaya() != null
										&& cicilanPembayaranSebelumnya.getItemBiaya().getId() != null) {
									itemBiaya = cicilanPembayaranSebelumnya.getItemBiaya();

								} else {
									itemBiaya = detailBiaya.getItemBiaya();
								}
								cicilan = MahasiswaVirtualAccountHelper.tambahTokenCicilan(cicilan, ("Item-" + itemBiaya.getId().toString() + "-" + nilai + "-"
												+ detailBiaya.getBayarKe() + "-" + detailBiaya.getId()));

								String desc = itemBiaya.getNama() + ", Rp. " + Common.numberFormat.get().format(nilai);

								pemb += itemBiaya.getKode().trim() + "," + desc + ";";
								total += nilai;
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBjb.java:671");
								// TODO: handle exception
							}
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBjb.java:676");
					}
				}
			}
		}

		boolean tagihan_expired_akhir_hari = Common
				.getKonfigurasi("tagihan_expired_akhir_hari", Konfigurasi.TIDAK_AKTIF).getNilai().trim()
				.equals(Konfigurasi.AKTIF);
		Date expired_date = myjadwalPembayaran.getEndDate();
		if (tagihan_expired_akhir_hari) {
			try {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.HOUR_OF_DAY, 23);
				calendar.set(Calendar.MINUTE, 59);
				calendar.set(Calendar.SECOND, 59);
				expired_date = calendar.getTime();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBjb.java:694");
			}
		} else {
			String tagihan_expired_jam = Common.getKonfigurasi("tagihan_expired_jam", "").getNilai();
			if (!tagihan_expired_jam.isEmpty()) {
				if (!tagihan_expired_jam.isEmpty() && !tagihan_expired_jam.equalsIgnoreCase("0")) {
					try {
						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.set(Calendar.HOUR_OF_DAY,
								calendar.get(Calendar.HOUR_OF_DAY) + Integer.parseInt(tagihan_expired_jam));
						expired_date = calendar.getTime();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBjb.java:706");
					}
				}
			} else {
				String tagihan_expired_day = Common.getKonfigurasi("tagihan_expired_day", "0").getNilai();

				if (!tagihan_expired_day.isEmpty() && !tagihan_expired_day.equalsIgnoreCase("0")) {
					try {
						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.set(Calendar.DATE,
								calendar.get(Calendar.DATE) + Integer.parseInt(tagihan_expired_day));
						expired_date = calendar.getTime();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBjb.java:719");
					}
				}
			}
		}
		VirtualAccountBank virtualAccountBankBjb = (VirtualAccountBank) session.createCriteria(VirtualAccountBank.class)
				.add(Restrictions.eq("terjadiKendala", false))
				.add(Restrictions.ge("kadaluarsaWaktu", WaktuUtil.getDate())).add(Restrictions.eq("keterangan", pemb))
				.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("semester", smt))
				.add(Restrictions.eq("jenisKegiatan", myjadwalPembayaran.getJenisKegiatan()))
				.add(Restrictions.isNull("kegiatan")).setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
		if (virtualAccountBankBjb == null) {
			virtualAccountBankBjb = new VirtualAccountBank(PerguruanTinggiUtil.getPerguruanTinggi().getId());

			virtualAccountBankBjb.setKanalPembayaran(
					myjadwalPembayaran == null || myjadwalPembayaran.getJenisKegiatan() == null ? null
							: myjadwalPembayaran.getJenisKegiatan().getKanalPembayaran());

			JSONObject jsonObjectBilling = new JSONObject();
			if (mahasiswa.getNama().length() > 19) {
				String namaMhs = mahasiswa.getNama().substring(0, 19);
				jsonObjectBilling.put("name", namaMhs);
			} else {
				jsonObjectBilling.put("name", mahasiswa.getNama());
			}

			Integer product_id = BJBSUtil.buatProdukBJB(pemb, Common.getKonfigurasi("user_bjbs", "s1627").getNilai());

			JSONObject jsonObjectBillingItem = new JSONObject();
			jsonObjectBillingItem.put("product_id", product_id);
			jsonObjectBillingItem.put("amount", total.intValue());
			JSONArray jAr = new JSONArray();
			jAr.put(jsonObjectBillingItem);
			jsonObjectBilling.put("data", jAr);

			String postDataBilling = jsonObjectBilling.toString();

			String nomorVa = "";
			JSONObject jsonObject = null;
			try {
				jsonObject = BJBSUtil.billingBJB(postDataBilling, true);
				nomorVa = jsonObject.getJSONObject("data").getString("va_acc_no");
				System.out.println("Response body billing: ");
				System.out.println(nomorVa);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBjb.java:764");
			}

			if (!nomorVa.isEmpty() && jsonObject != null) {

				virtualAccountBankBjb.setKadaluarsa(expired_date);
				virtualAccountBankBjb.setOtomatis(false);
				virtualAccountBankBjb.setKode(nomorVa);
				virtualAccountBankBjb.setRequest(postDataBilling);
				virtualAccountBankBjb.setResponse(jsonObject.toString());
				virtualAccountBankBjb.setCicilan(cicilan);
				virtualAccountBankBjb.setJenisKegiatan(myjadwalPembayaran.getJenisKegiatan());
				virtualAccountBankBjb.setKeterangan(pemb);
				virtualAccountBankBjb.setTotal(total);
				virtualAccountBankBjb.setBulanan("");
				virtualAccountBankBjb.setDetailbiaya(detailbiaya);

				virtualAccountBankBjb.setMahasiswa(mahasiswa);
				virtualAccountBankBjb.setJadwalPembayaran(myjadwalPembayaran);
				virtualAccountBankBjb.setSemester(smt);
				virtualAccountBankBjb.setTahunAkademik(myjadwalPembayaran.getTahunAkademik());
				virtualAccountBankBjb.setBank("Bank BJBS");

				MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
				session.saveOrUpdate(virtualAccountBankBjb);
				MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);
			}
		}
		MahasiswaVirtualAccountHelper.closeSessionQuietly(session);
			MahasiswaVirtualAccountHelper.closeHibernateContextQuietly();

		return virtualAccountBankBjb;
	} finally {
			// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
			// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBjb.java:800");}
				try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBjb.java:801");}
			}
		}
	}

	public static void downloadData(Mahasiswa mahasiswa, String ta, Integer smt, JenisKegiatan jenisKegiatan,
			Map<Long, Object[]> biayas) {

		String detailbiaya = "";

		Session session = MahasiswaVirtualAccountHelper.openSession();
		try {

		String pemb = "";
		Double total = 0.0;

		for (Object[] o : biayas.values()) {
			DetailBiaya biaya = (DetailBiaya) o[0];
			Double nilai = (Double) o[1];
			ItemBiaya itemBiaya = biaya.getItemBiaya();
			detailbiaya += (detailbiaya.isEmpty() ? biaya.getId() : "," + biaya.getId());
			if (nilai > 0.01) {
				pemb += itemBiaya.getKode().trim() + "," + itemBiaya.getNama().trim() + "," + (nilai).longValue() + ";";
				total += nilai;
			}

		}

		String bulanan = "";

		String kodeUnik = Common.getGeneratedBarCode();
		VirtualAccountBank virtualAccountBankBjb = (VirtualAccountBank) session.createCriteria(VirtualAccountBank.class)
				.add(Restrictions.eq("terjadiKendala", false)).add(Restrictions.eq("nama", kodeUnik)).uniqueResult();
		if (virtualAccountBankBjb == null) {
			virtualAccountBankBjb = new VirtualAccountBank(PerguruanTinggiUtil.getPerguruanTinggi().getId());

			virtualAccountBankBjb.setKanalPembayaran(jenisKegiatan == null ? null : jenisKegiatan.getKanalPembayaran());

		} else if (virtualAccountBankBjb.getKegiatan() != null) {
			return;
		}

		virtualAccountBankBjb.setKodeUnikLain(true);
		virtualAccountBankBjb.setNama(kodeUnik);
		virtualAccountBankBjb.setJenisKegiatan(jenisKegiatan);
		virtualAccountBankBjb.setKeterangan(pemb);
		virtualAccountBankBjb.setTotal(total);
		virtualAccountBankBjb.setBulanan(bulanan);
		virtualAccountBankBjb.setDetailbiaya(detailbiaya);

		virtualAccountBankBjb.setMahasiswa(mahasiswa);
		virtualAccountBankBjb.setSemester(smt);
		virtualAccountBankBjb.setTahunAkademik(ta);
		virtualAccountBankBjb.setBank("Bank BJB");

		MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
		Common.refreshSaveOrUpdate(session, virtualAccountBankBjb);
		MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);

		MahasiswaVirtualAccountHelper.closeSessionQuietly(session);
			MahasiswaVirtualAccountHelper.closeHibernateContextQuietly();

	} finally {
			// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
			// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBjb.java:867");}
				try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBjb.java:868");}
			}
		}
	}
}
