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
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.BJBSUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;
import ais.database.model.VirtualAccountBank;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class DownloadNoRegistrasiCalonMahasiswaBankBjb extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;
	public static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	static JenisKegiatan jenisKegiatan = pembayaranUtil.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);

	private Center center = new Center();
	private Intbox searchangkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
	private Textbox nims = new Textbox();
	private File file;

	public DownloadNoRegistrasiCalonMahasiswaBankBjb() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Unduh Nomor Registrasi Calon Mahasiswa (Bank BJB)",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Unduh Nomor Registrasi Calon Mahasiswa Bank BJB.",
							"Periksa apakah data Jenis Kegiatan Pendaftaran Calon Mahasiswa dan konfigurasi integrasi Bank BJB sudah benar.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
		}
	}

	public DownloadNoRegistrasiCalonMahasiswaBankBjb(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Unduh Nomor Registrasi Calon Mahasiswa (Bank BJB)",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Unduh Nomor Registrasi Calon Mahasiswa Bank BJB.",
							"Periksa apakah data Jenis Kegiatan Pendaftaran Calon Mahasiswa dan konfigurasi integrasi Bank BJB sudah benar.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
		}
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Penerimaan Mahasiswa Baru"));
		row.appendChild(searchangkatan);
		searchangkatan.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "1,5");
		row.appendChild(new ais.ui.util.MyLabelConfig("No Reg:"));
		row.appendChild(nims);
		nims.setWidth("90%");
		nims.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "6");
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Masukkan no reg jika hanya untuk ambil tagihan beberapa calon mahasiswa, pisahkan no reg dengan tanda koma"));

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
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"PEMBAYARAN_CALON_MAHASISWA_" + Common.dateFormat32.get().format(ais.ui.util.WaktuUtil.getDate())
									+ ".xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBjb.java:193");

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
			MyMessageboxConfig.show("Tahun harus dipilih", "Peringatan", MyMessageboxConfig.OK,
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

				XSSFSheet sheet = workbook.createSheet("TAGIHAN_NO_REG");
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

				List<BiodataCalonMahasiswa> biodataCalonMahasiswas = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(nimsMahasiswa.isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.in("noRegistrasi", nimsMahasiswa))
						.createAlias("gelombangPendaftaran", "gelombangPendaftaran")
						.add(Restrictions.eq("gelombangPendaftaran.bisaDipilihPendaftarOnline", true))
						.addOrder(Order.asc("id")).add(Restrictions.isNotNull("noRegistrasi"))
						.add(Restrictions.ne("noRegistrasi", ""))
						.add(searchangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahun", searchangkatan.getValue()))

						.list();

				int size = biodataCalonMahasiswas.size();

				int rowIndex = 1;
				int rowIndexProses = 1;
				for (BiodataCalonMahasiswa biodataCalonMahasiswa : biodataCalonMahasiswas) {
					label.setValue("Sedang memproses data " + biodataCalonMahasiswa.toString() + " ("
							+ Common.numberFormat.get().format(rowIndexProses * 100.0 / size) + " %)");
					if (createData(session, biodataCalonMahasiswa, sheet, rowIndex, lockedNumericStyle, notLocked)) {
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
							"Menyimpan berkas Excel hasil unduhan Nomor Registrasi Calon Mahasiswa (Bank BJB)",
							e,
							new String[] {
									"Pastikan berkas dengan nama yang sama tidak sedang dibuka oleh aplikasi lain (misalnya Microsoft Excel).",
									"Periksa ketersediaan ruang penyimpanan (disk space) pada server.",
									"Ulangi proses unduh data. Jika kegagalan berulang, hubungi Administrator/Developer disertai tangkapan layar (screenshot) pesan ini." });
				}

				MahasiswaVirtualAccountHelper.closeSessionQuietly(session);
			MahasiswaVirtualAccountHelper.closeHibernateContextQuietly();

				biodataCalonMahasiswas.clear();
				label.setValue("");
			} catch (Exception e) {
				// FIX "gagal diam-diam"/hang: sebelumnya try{...}finally{...} TANPA catch,
				// exception menembus keluar Runnable.run() tak tertangani, thread mati, dan
				// label.setValue("") TIDAK PERNAH dijalankan -> popup progres menggantung selamanya.
				Common.tampilErrorJikaAdmin(e);
				label.setValue("Error: " + PesanFormalHelper.pesanGagalException(
						"pengunduhan Nomor Registrasi Virtual Account Calon Mahasiswa (Bank BJB)", null, e,
						new String[] {
								"Periksa kembali data/filter (Tahun Angkatan/No Reg) yang dipilih dan coba ulangi.",
								"Periksa ketersediaan ruang penyimpanan (disk space) pada server untuk berkas Excel sementara.",
								"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
						.replace("\n", " "));
			} finally {
					// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
					// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
					if (session != null && session.isOpen()) {
						try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBjb.java:311");}
						try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBjb.java:312");}
					}
				}
			}
		}).start();

	}

	private boolean createData(Session session, BiodataCalonMahasiswa biodataCalonMahasiswa, XSSFSheet sheet,
			int rowIndex, XSSFCellStyle lockedNumericStyle, XSSFCellStyle notLocked) {

		Jurusan myjurusan1 = biodataCalonMahasiswa.getProdi1() == null ? biodataCalonMahasiswa.getProdi2()
				: biodataCalonMahasiswa.getProdi1();
		if (myjurusan1 == null) {
			myjurusan1 = biodataCalonMahasiswa.getProdi3();
		}
		if (myjurusan1 == null) {
			myjurusan1 = biodataCalonMahasiswa.getProdi4();
		}
		if (myjurusan1 == null) {
			myjurusan1 = biodataCalonMahasiswa.getProdi5();
		}
		java.util.Collection<DetailBiaya> detailBiayas = pembayaranUtil
				.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, myjurusan1, null, false);

		JadwalPembayaran myjadwalPembayaran = (JadwalPembayaran) session.createCriteria(JadwalPembayaran.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("jenjang"),
						Restrictions.eq("jenjang", biodataCalonMahasiswa.getJenjang())))
				.add(Restrictions.or(Restrictions.isNull("ganjil"),
						Restrictions.eq("ganjil",
								biodataCalonMahasiswa.getGelombangPendaftaran().getJenisSemester()
										.equals(Perkuliahan.GANJIL))))
				.add(Restrictions.or(Restrictions.isNull("tahunAkademik"),
						Restrictions.eq("tahunAkademik",
								biodataCalonMahasiswa.getGelombangPendaftaran().getTahunAkademik())))
				.add(Restrictions.eq("jenisKegiatan", jenisKegiatan)).addOrder(Order.desc("endDate"))
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

		if (myjadwalPembayaran != null) {

			String pemb = "";
			Double total = 0.0;

			String detailbiaya = "";
			for (DetailBiaya biaya : detailBiayas) {
				ItemBiaya itemBiaya = biaya.getItemBiaya();
				Double nilai = biaya.hitungTotal();
				if (nilai > 0.01) {
					detailbiaya += (detailbiaya.isEmpty() ? biaya.getId() : "," + biaya.getId());
					pemb += itemBiaya.getKode().trim() + "," + itemBiaya.getNama().trim() + "," + (nilai).longValue()
							+ ";";
					total += nilai;
				}
			}

			String kodeUnik = "CAL.MHS-" + biodataCalonMahasiswa.getId() + "-" + jenisKegiatan.getId();
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
			virtualAccountBankBjb.setBulanan("");
			virtualAccountBankBjb.setDetailbiaya(detailbiaya);

			virtualAccountBankBjb.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
			virtualAccountBankBjb.setJadwalPembayaran(myjadwalPembayaran);
			virtualAccountBankBjb.setSemester(0);
			virtualAccountBankBjb.setTahunAkademik(biodataCalonMahasiswa.getTahunAkademik());
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
			cell.setCellValue("[" + biodataCalonMahasiswa.getNoRegistrasi() + "] " + biodataCalonMahasiswa.getNama());

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
		}
		return false;

	}

	@SuppressWarnings("rawtypes")
	public static VirtualAccountBank downloadData(BiodataCalonMahasiswa biodataCalonMahasiswa,
			JadwalPembayaran myjadwalPembayaran, Collection detailBiayas) throws Exception {

		String detailbiaya = "";

		Session session = MahasiswaVirtualAccountHelper.openSession();
		try {

		String pemb = "";
		Double total = 0.0;

		for (Object o : detailBiayas) {
			if (o instanceof DetailBiaya) {
				DetailBiaya biaya = (DetailBiaya) o;
				ItemBiaya itemBiaya = biaya.getItemBiaya();
				detailbiaya += (detailbiaya.isEmpty() ? biaya.getId() : "," + biaya.getId());

				Double nilai = (biaya.getNilaiBiayaBaru() == null ? biaya.getNilaiBiaya() : biaya.getNilaiBiayaBaru());
				if (nilai > 0.01) {
					pemb += itemBiaya.getKode().trim() + "," + itemBiaya.getNama().trim() + "," + (nilai).longValue()
							+ ";";
					total += nilai;
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
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBjb.java:465");
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
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBjb.java:478");
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
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBjb.java:491");
					}
				}
			}
		}
		VirtualAccountBank virtualAccountBankBjb = (VirtualAccountBank) session.createCriteria(VirtualAccountBank.class)
				.add(Restrictions.eq("terjadiKendala", false)).add(Restrictions.eq("keterangan", pemb))
				.add(Restrictions.ge("kadaluarsaWaktu", WaktuUtil.getDate()))
				.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
				.add(Restrictions.eq("jenisKegiatan", myjadwalPembayaran.getJenisKegiatan()))
				.add(Restrictions.isNull("kegiatan")).setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
		if (virtualAccountBankBjb == null) {
			virtualAccountBankBjb = new VirtualAccountBank(PerguruanTinggiUtil.getPerguruanTinggi().getId());
			virtualAccountBankBjb.setKanalPembayaran(
					myjadwalPembayaran == null || myjadwalPembayaran.getJenisKegiatan() == null ? null
							: myjadwalPembayaran.getJenisKegiatan().getKanalPembayaran());

			JSONObject jsonObjectBilling = new JSONObject();

			// jsonObjectBilling.put("va_acc_no", nomorVa);
			jsonObjectBilling.put("company_id",
					Integer.parseInt(Common.getKonfigurasi("bjb_company_id", "456").getNilai()));

			if (biodataCalonMahasiswa.getNama().length() > 19) {
				String namaMhs = biodataCalonMahasiswa.getNama().substring(0, 19);
				jsonObjectBilling.put("name", namaMhs);
			} else {
				jsonObjectBilling.put("name", biodataCalonMahasiswa.getNama());
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
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBjb.java:540");
			}

			if (!nomorVa.isEmpty() && jsonObject != null) {

				virtualAccountBankBjb.setKadaluarsa(expired_date);
				virtualAccountBankBjb.setOtomatis(false);
				virtualAccountBankBjb.setKode(nomorVa);
				virtualAccountBankBjb.setRequest(postDataBilling);
				virtualAccountBankBjb.setResponse(jsonObject.toString());
				virtualAccountBankBjb.setJenisKegiatan(myjadwalPembayaran.getJenisKegiatan());
				virtualAccountBankBjb.setKeterangan(pemb);
				virtualAccountBankBjb.setTotal(total);
				virtualAccountBankBjb.setDetailbiaya(detailbiaya);

				virtualAccountBankBjb.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
				virtualAccountBankBjb.setJadwalPembayaran(myjadwalPembayaran);
				virtualAccountBankBjb.setSemester(1);
				virtualAccountBankBjb.setTahunAkademik(myjadwalPembayaran.getTahunAkademik());
				virtualAccountBankBjb.setBank("Bank BJBS");

				MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
				session.save(virtualAccountBankBjb);
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
				try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBjb.java:576");}
				try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBjb.java:577");}
			}
		}
	}
}
