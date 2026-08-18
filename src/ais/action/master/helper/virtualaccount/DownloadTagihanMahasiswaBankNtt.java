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

public class DownloadTagihanMahasiswaBankNtt extends MyWindow {

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

	public DownloadTagihanMahasiswaBankNtt() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Unduh Tagihan Mahasiswa (Bank NTT)",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Unduh Tagihan Mahasiswa Bank NTT.",
							"Periksa apakah data Tahun Ajaran, Semester, dan konfigurasi integrasi Bank NTT sudah benar.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
		}
	}

	public DownloadTagihanMahasiswaBankNtt(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Unduh Tagihan Mahasiswa (Bank NTT)",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Unduh Tagihan Mahasiswa Bank NTT.",
							"Periksa apakah data Tahun Ajaran, Semester, dan konfigurasi integrasi Bank NTT sudah benar.",
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
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankNtt.java:229");

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
							"Menyimpan berkas Excel hasil unduhan Tagihan Mahasiswa (Bank NTT)",
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
						"pengunduhan Tagihan Virtual Account Mahasiswa (Bank NTT)", null, e,
						new String[] {
								"Periksa kembali data/filter (Tahun Angkatan/Jenis Pembayaran/Tahun Akademik/Semester/Tagihan Bulan) yang dipilih dan coba ulangi.",
								"Periksa ketersediaan ruang penyimpanan (disk space) pada server untuk berkas Excel sementara.",
								"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
						.replace("\n", " "));
			} finally {
					// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
					// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
					if (session != null && session.isOpen()) {
						try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankNtt.java:363");}
						try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankNtt.java:364");}
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
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankNtt.java:447");
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
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankNtt.java:459");
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
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankNtt.java:472");
							}
						}
					}
				}
				String kodeUnik = "MHS-" + mahasiswa.getId() + "-" + bulan + "-" + jenisKegiatan.getId();
				VirtualAccountBank virtualAccountBankNtt = (VirtualAccountBank) session
						.createCriteria(VirtualAccountBank.class).add(Restrictions.eq("terjadiKendala", false))
						.add(Restrictions.ge("kadaluarsaWaktu", WaktuUtil.getDate()))
						.add(Restrictions.eq("nama", kodeUnik)).uniqueResult();
				if (virtualAccountBankNtt == null) {
					virtualAccountBankNtt = new VirtualAccountBank(PerguruanTinggiUtil.getPerguruanTinggi().getId());
					virtualAccountBankNtt.setKanalPembayaran(
							myjadwalPembayaran == null || myjadwalPembayaran.getJenisKegiatan() == null ? null
									: myjadwalPembayaran.getJenisKegiatan().getKanalPembayaran());
				} else if ((virtualAccountBankNtt.getKegiatan() != null || virtualAccountBankNtt.getPembayaran() != null)) {
					return false;
				}
				String va = Common.getGeneratedAngkaDigit(10);

				virtualAccountBankNtt.setKadaluarsa(expired_date);
				virtualAccountBankNtt.setOtomatis(false);
				virtualAccountBankNtt.setKode(va);
				virtualAccountBankNtt.setNama(kodeUnik);
				virtualAccountBankNtt.setJenisKegiatan(jenisKegiatan);
				virtualAccountBankNtt.setKeterangan(pemb);
				virtualAccountBankNtt.setTotal(total);
				virtualAccountBankNtt.setBulanan(bulanan);
				virtualAccountBankNtt.setDetailbiaya(detailbiaya);

				virtualAccountBankNtt.setMahasiswa(mahasiswa);
				virtualAccountBankNtt.setJadwalPembayaran(myjadwalPembayaran);
				virtualAccountBankNtt.setSemester(smt);
				virtualAccountBankNtt.setTahunAkademik(ta);
				virtualAccountBankNtt.setBank("Bank NTT");

				MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
				Common.refreshSaveOrUpdate(session, virtualAccountBankNtt);
				MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);

				XSSFRow row = sheet.createRow(rowIndex);

				XSSFCell cell = row.createCell(0);
				cell.setCellStyle(lockedNumericStyle);
				cell.setCellValue(virtualAccountBankNtt.getKode());

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
					VirtualAccountBank virtualAccountBankNtt = (VirtualAccountBank) session
							.createCriteria(VirtualAccountBank.class).add(Restrictions.eq("terjadiKendala", false))
							.add(Restrictions.eq("nama", kodeUnik)).uniqueResult();
					if (virtualAccountBankNtt == null) {
						virtualAccountBankNtt = new VirtualAccountBank(
								PerguruanTinggiUtil.getPerguruanTinggi().getId());
						virtualAccountBankNtt.setKanalPembayaran(
								myjadwalPembayaran == null || myjadwalPembayaran.getJenisKegiatan() == null ? null
										: myjadwalPembayaran.getJenisKegiatan().getKanalPembayaran());
					} else if ((virtualAccountBankNtt.getKegiatan() != null || virtualAccountBankNtt.getPembayaran() != null)) {
						return false;
					}
					String va = Common.getGeneratedAngkaDigit(10);
					virtualAccountBankNtt.setOtomatis(false);
					virtualAccountBankNtt.setKode(va);
					virtualAccountBankNtt.setNama(kodeUnik);
					virtualAccountBankNtt.setJenisKegiatan(jenisKegiatan);
					virtualAccountBankNtt.setKeterangan("");
					virtualAccountBankNtt.setTotal(total);
					virtualAccountBankNtt.setBulanan("");
					virtualAccountBankNtt.setDetailbiaya(detailbiaya);

					virtualAccountBankNtt.setMahasiswa(mahasiswa);
					virtualAccountBankNtt.setJadwalPembayaran(myjadwalPembayaran);
					virtualAccountBankNtt.setSemester(smt);
					virtualAccountBankNtt.setTahunAkademik(ta);
					virtualAccountBankNtt.setBank("Bank NTT");

					MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
					Common.refreshSaveOrUpdate(session, virtualAccountBankNtt);
					MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);

					XSSFRow row = sheet.createRow(rowIndex);

					XSSFCell cell = row.createCell(0);
					cell.setCellStyle(lockedNumericStyle);
					cell.setCellValue(virtualAccountBankNtt.getKode());

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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static VirtualAccountBank downloadData(Mahasiswa mahasiswa, Integer smt, JadwalPembayaran myjadwalPembayaran,
			Collection detailBiayas, Grid gridCicilan) {

		String detailbiaya = "";
		for (Object o : detailBiayas) {
			if (o instanceof DetailBiaya) {
				DetailBiaya biaya = (DetailBiaya) o;
				detailbiaya += (detailbiaya.isEmpty() ? biaya.getId() : "," + biaya.getId());
			}
		}

		Session session = MahasiswaVirtualAccountHelper.openSession();
		try {

		String pemb = "";
		String cicilan = "";
		Double total = 0.0;

		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		for (Row row : mycicilanrows) {
			MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
			JadwalPembayaran jdw = myjadwalPembayaran != null && myjadwalPembayaran.getKhususUntukNim() != null
					&& myjadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
							? myjadwalPembayaran
							: null;
			if (jumlahCicilan.getValue() != null && jumlahCicilan.getValue().intValue() != 0) {
				CicilanPembayaran cicilanPembayaranSebelumnya = (CicilanPembayaran) row
						.getAttribute("cicilanPembayaran");
				Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
				if (cicilanPembayaranSebelumnya != null) {
					if (cicilanPembayaranSebelumnya.getId() == null) {
						try {
							Object jenisBiaya = myItemBiaya.getSelectedItem() == null ? null
									: myItemBiaya.getSelectedItem().getValue();
							PengaturanPembayaranBulanan biaya = cicilanPembayaranSebelumnya
									.getPengaturanPembayaranBulanan();
							ItemBiaya itemBiaya = cicilanPembayaranSebelumnya.getItemBiaya();
							DetailBiaya detailBiaya = null;
							if (jenisBiaya != null && jenisBiaya instanceof PengaturanPembayaranBulanan) {
								biaya = (PengaturanPembayaranBulanan) jenisBiaya;
								detailBiaya = biaya.getDetailBiaya();
								itemBiaya = detailBiaya.getItemBiaya();
							} else if (jenisBiaya != null && jenisBiaya instanceof DetailBiaya) {
								detailBiaya = (DetailBiaya) jenisBiaya;
								itemBiaya = detailBiaya.getItemBiaya();
							}

							if (biaya != null) {
								try {
									Double nilai = jumlahCicilan.getValue();

									cicilan = MahasiswaVirtualAccountHelper.tambahTokenCicilan(cicilan, ("Bulanan-" + biaya.getId().toString() + "-" + nilai));

									Double hasilDenda = biaya.checkDenda(nilai, ais.ui.util.WaktuUtil.getDate(), jdw,
											myjadwalPembayaran == null ? null : myjadwalPembayaran.getJenisKegiatan());

									String desc = biaya.getKeterangan();
									desc = (desc.isEmpty() ? (biaya.getDetailBiaya().getItemBiaya().getNama()) : desc)
											+ ", Rp. " + Common.numberFormat.get().format(nilai)
											+ (hasilDenda.intValue() > nilai.intValue() ? biaya.getInfoDenda() : "");

									pemb += biaya.getDetailBiaya().getItemBiaya().getKode().trim() + "," + desc + ";";
									total += nilai;
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankNtt.java:681");
								}
							} else {

								try {
									Double nilai = jumlahCicilan.getValue();

									System.out.println("itemBiaya -> " + itemBiaya);

									cicilan = MahasiswaVirtualAccountHelper.tambahTokenCicilan(cicilan, ("Item-" + itemBiaya.getId() + "-" + nilai + "-"
													+ (detailBiaya == null ? 1 : detailBiaya.getBayarKe())
													+ (detailBiaya == null ? "" : "-" + detailBiaya.getId())));

									String desc = itemBiaya.getNama() + ", Rp. " + Common.numberFormat.get().format(nilai);

									pemb += itemBiaya.getKode().trim() + "," + desc + ";";
									total += nilai;
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankNtt.java:699");
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankNtt.java:703");
						}
					}
				}
			}
		}

		String tagihan_expired_day = Common.getKonfigurasi("tagihan_expired_day", "0").getNilai();
		Date expired_date = myjadwalPembayaran.getEndDate();
		if (!tagihan_expired_day.isEmpty() && !tagihan_expired_day.equalsIgnoreCase("0")) {
			try {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + Integer.parseInt(tagihan_expired_day));
				expired_date = calendar.getTime();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankNtt.java:718");
			}
		}

		VirtualAccountBank virtualAccountBankNtt = (VirtualAccountBank) session.createCriteria(VirtualAccountBank.class)
				.add(Restrictions.eq("terjadiKendala", false))
				.add(Restrictions.ge("kadaluarsaWaktu", WaktuUtil.getDate())).add(Restrictions.eq("keterangan", pemb))
				.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("semester", smt))
				.add(Restrictions.eq("jenisKegiatan", myjadwalPembayaran.getJenisKegiatan()))
				.add(Restrictions.isNull("kegiatan")).setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
		if (virtualAccountBankNtt == null) {
			virtualAccountBankNtt = new VirtualAccountBank(PerguruanTinggiUtil.getPerguruanTinggi().getId());
			virtualAccountBankNtt.setKanalPembayaran(
					myjadwalPembayaran == null || myjadwalPembayaran.getJenisKegiatan() == null ? null
							: myjadwalPembayaran.getJenisKegiatan().getKanalPembayaran());

			String va = Common.getGeneratedAngkaDigit(10);
			virtualAccountBankNtt.setKadaluarsa(expired_date);
			virtualAccountBankNtt.setOtomatis(false);
			virtualAccountBankNtt.setKode(va);
			virtualAccountBankNtt.setCicilan(cicilan);
			virtualAccountBankNtt.setJenisKegiatan(myjadwalPembayaran.getJenisKegiatan());
			virtualAccountBankNtt.setKeterangan(pemb);
			virtualAccountBankNtt.setTotal(total);
			virtualAccountBankNtt.setBulanan("");
			virtualAccountBankNtt.setDetailbiaya(detailbiaya);

			virtualAccountBankNtt.setMahasiswa(mahasiswa);
			virtualAccountBankNtt.setJadwalPembayaran(myjadwalPembayaran);
			virtualAccountBankNtt.setSemester(smt);
			virtualAccountBankNtt.setTahunAkademik(myjadwalPembayaran.getTahunAkademik());
			virtualAccountBankNtt.setBank("Bank NTT");

			MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
			session.saveOrUpdate(virtualAccountBankNtt);
			MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);

		}

		MahasiswaVirtualAccountHelper.closeSessionQuietly(session);
			MahasiswaVirtualAccountHelper.closeHibernateContextQuietly();

		return virtualAccountBankNtt;
	} finally {
			// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
			// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankNtt.java:765");}
				try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankNtt.java:766");}
			}
		}
	}

	public static void downloadData(Mahasiswa mahasiswa, String ta, Integer smt, JenisKegiatan jenisKegiatan,
			Map<Long, Object[]> biayas) {

		XSSFWorkbook workbook = new XSSFWorkbook();

		XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
		lockedNumericStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		lockedNumericStyle.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
		lockedNumericStyle.setLocked(true);

		XSSFCellStyle notLocked = workbook.createCellStyle();
		notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
		notLocked.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
		// notLocked.setLocked(false);

		XSSFSheet sheet = workbook.createSheet("PEMBAYARAN_MAHASISWA");
		// sheet.protectSheet("passwordrahasia");
		sheet.setDefaultColumnWidth(25);

		XSSFRow rowhead = sheet.createRow((short) 0);

		rowhead.createCell(0).setCellValue("nopel");
		rowhead.createCell(1).setCellValue("nama");
		rowhead.createCell(2).setCellValue("ket");
		rowhead.createCell(3).setCellValue("tagihan");
		rowhead.createCell(4).setCellValue("expired");

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
		VirtualAccountBank virtualAccountBankNtt = (VirtualAccountBank) session.createCriteria(VirtualAccountBank.class)
				.add(Restrictions.eq("terjadiKendala", false)).add(Restrictions.eq("nama", kodeUnik)).uniqueResult();
		if (virtualAccountBankNtt == null) {
			virtualAccountBankNtt = new VirtualAccountBank(PerguruanTinggiUtil.getPerguruanTinggi().getId());
			virtualAccountBankNtt.setKanalPembayaran(jenisKegiatan == null ? null : jenisKegiatan.getKanalPembayaran());

		} else if ((virtualAccountBankNtt.getKegiatan() != null || virtualAccountBankNtt.getPembayaran() != null)) {
			return;
		}
		String va = Common.getGeneratedAngkaDigit(10);
		virtualAccountBankNtt.setOtomatis(false);
		virtualAccountBankNtt.setKode(va);
		virtualAccountBankNtt.setKodeUnikLain(true);
		virtualAccountBankNtt.setNama(kodeUnik);
		virtualAccountBankNtt.setJenisKegiatan(jenisKegiatan);
		virtualAccountBankNtt.setKeterangan(pemb);
		virtualAccountBankNtt.setTotal(total);
		virtualAccountBankNtt.setBulanan(bulanan);
		virtualAccountBankNtt.setDetailbiaya(detailbiaya);

		virtualAccountBankNtt.setMahasiswa(mahasiswa);
		virtualAccountBankNtt.setSemester(smt);
		virtualAccountBankNtt.setTahunAkademik(ta);
		virtualAccountBankNtt.setBank("Bank NTT");

		MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
		Common.refreshSaveOrUpdate(session, virtualAccountBankNtt);
		MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);

		XSSFRow row = sheet.createRow(1);

		XSSFCell cell = row.createCell(0);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(virtualAccountBankNtt.getKode());

		cell = row.createCell(1);
		cell.setCellStyle(notLocked);
		cell.setCellValue("[" + mahasiswa.getNim() + "] " + mahasiswa.getNama());

		cell = row.createCell(2);
		cell.setCellStyle(notLocked);
		cell.setCellValue(pemb);

		cell = row.createCell(3);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(total);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) + 1);

		cell = row.createCell(4);
		cell.setCellStyle(notLocked);
		cell.setCellValue(Common.dateFormat11.get().format(calendar.getTime()));

		MahasiswaVirtualAccountHelper.closeSessionQuietly(session);
			MahasiswaVirtualAccountHelper.closeHibernateContextQuietly();

		try {

			final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_"
					+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
					+ ".xlsx");

			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
			fileOut.close();

			try {
				Filedownload.save(new FileInputStream(filename),
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "PEMBAYARAN_MAHASISWA_"
								+ Common.dateFormat32.get().format(ais.ui.util.WaktuUtil.getDate()) + ".xlsx");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankNtt.java:892");
				PesanFormalHelper.tampilkanGagalException(
						"Mengunduh (download) berkas Excel Tagihan Mahasiswa (Bank NTT)",
						e,
						new String[] {
								"Ulangi proses unduh data.",
								"Pastikan browser tidak memblokir unduhan (download) untuk halaman ini.",
								"Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." });
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Menyimpan berkas Excel hasil unduhan Tagihan Mahasiswa (Bank NTT)",
					e,
					new String[] {
							"Pastikan berkas dengan nama yang sama tidak sedang dibuka oleh aplikasi lain (misalnya Microsoft Excel).",
							"Periksa ketersediaan ruang penyimpanan (disk space) pada server.",
							"Ulangi proses unduh data. Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." });
		}
	} finally {
			// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
			// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankNtt.java:904");}
				try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankNtt.java:905");}
			}
		}
	}
}
