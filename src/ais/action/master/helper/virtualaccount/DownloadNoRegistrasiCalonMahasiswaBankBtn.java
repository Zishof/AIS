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

public class DownloadNoRegistrasiCalonMahasiswaBankBtn extends MyWindow {

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

	public DownloadNoRegistrasiCalonMahasiswaBankBtn() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Unduh Nomor Registrasi Calon Mahasiswa (Bank BTN)",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Unduh Nomor Registrasi Calon Mahasiswa Bank BTN.",
							"Periksa apakah data Jenis Kegiatan Pendaftaran Calon Mahasiswa dan konfigurasi integrasi Bank BTN sudah benar.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
		}
	}

	public DownloadNoRegistrasiCalonMahasiswaBankBtn(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Unduh Nomor Registrasi Calon Mahasiswa (Bank BTN)",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Unduh Nomor Registrasi Calon Mahasiswa Bank BTN.",
							"Periksa apakah data Jenis Kegiatan Pendaftaran Calon Mahasiswa dan konfigurasi integrasi Bank BTN sudah benar.",
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
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBtn.java:191");

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
							"Menyimpan berkas Excel hasil unduhan Nomor Registrasi Calon Mahasiswa (Bank BTN)",
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
						"pengunduhan Nomor Registrasi Virtual Account Calon Mahasiswa (Bank BTN)", null, e,
						new String[] {
								"Periksa kembali data/filter (Tahun Angkatan/No Reg) yang dipilih dan coba ulangi.",
								"Periksa ketersediaan ruang penyimpanan (disk space) pada server untuk berkas Excel sementara.",
								"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
						.replace("\n", " "));
			} finally {
					// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
					// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
					if (session != null && session.isOpen()) {
						try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBtn.java:309");}
						try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBtn.java:310");}
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
			VirtualAccountBank virtualAccountBankBtn = (VirtualAccountBank) session
					.createCriteria(VirtualAccountBank.class).add(Restrictions.eq("terjadiKendala", false))
					.add(Restrictions.eq("nama", kodeUnik)).uniqueResult();
			if (virtualAccountBankBtn == null) {
				virtualAccountBankBtn = new VirtualAccountBank(PerguruanTinggiUtil.getPerguruanTinggi().getId());
				virtualAccountBankBtn.setKanalPembayaran(
						myjadwalPembayaran == null || myjadwalPembayaran.getJenisKegiatan() == null ? null
								: myjadwalPembayaran.getJenisKegiatan().getKanalPembayaran());
			} else if (virtualAccountBankBtn.getKegiatan() != null) {
				return false;
			}

			virtualAccountBankBtn.setNama(kodeUnik);
			virtualAccountBankBtn.setJenisKegiatan(jenisKegiatan);
			virtualAccountBankBtn.setKeterangan(pemb);
			virtualAccountBankBtn.setTotal(total);
			virtualAccountBankBtn.setBulanan("");
			virtualAccountBankBtn.setDetailbiaya(detailbiaya);

			virtualAccountBankBtn.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
			virtualAccountBankBtn.setJadwalPembayaran(myjadwalPembayaran);
			virtualAccountBankBtn.setSemester(0);
			virtualAccountBankBtn.setTahunAkademik(biodataCalonMahasiswa.getTahunAkademik());
			virtualAccountBankBtn.setBank("Bank BTN");

			MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
			Common.refreshSaveOrUpdate(session, virtualAccountBankBtn);
			MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);

			XSSFRow row = sheet.createRow(rowIndex);

			XSSFCell cell = row.createCell(0);
			cell.setCellStyle(lockedNumericStyle);
			cell.setCellValue(virtualAccountBankBtn.getKode());

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

		String cicilan = "";
		for (Object o : detailBiayas) {
			if (o instanceof DetailBiaya) {
				DetailBiaya biaya = (DetailBiaya) o;
				ItemBiaya itemBiaya = biaya.getItemBiaya();
				detailbiaya += (detailbiaya.isEmpty() ? biaya.getId() : "," + biaya.getId());

				Double nilai = (biaya.getNilaiBiayaBaru() == null ? biaya.getNilaiBiaya() : biaya.getNilaiBiayaBaru());
				if (nilai > 0.01) {
					pemb += itemBiaya.getKode().trim() + "," + itemBiaya.getNama().trim() + "," + (nilai).longValue()
							+ ";";

					cicilan = MahasiswaVirtualAccountHelper.tambahTokenCicilan(cicilan, ("Item-" + itemBiaya.getId().toString() + "-" + nilai + "-" + biaya.getBayarKe() + "-"
									+ biaya.getId()));

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
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBtn.java:468");
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
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBtn.java:480");
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
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBtn.java:493");
					}
				}
			}
		}
		VirtualAccountBank virtualAccountBankBtn = (VirtualAccountBank) session.createCriteria(VirtualAccountBank.class)
				.add(Restrictions.eq("terjadiKendala", false))
				.add(Restrictions.ge("kadaluarsaWaktu", WaktuUtil.getDate())).add(Restrictions.eq("keterangan", pemb))
				.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
				.add(Restrictions.eq("jenisKegiatan", myjadwalPembayaran.getJenisKegiatan()))
				.add(Restrictions.isNull("kegiatan")).setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
		if (virtualAccountBankBtn == null) {

			JSONObject jsonObject = new JSONObject();

			String ref = Common.getGeneratedBarCode(12);
			String kodeInstitusi = Common.getKonfigurasi("btn_kode_institusi", "4463").getNilai();
			String kodePayment = Common.getKonfigurasi("btn_kode_payment", "001").getNilai();

			int digitgenerated = 10;
			try {
				digitgenerated = Integer.parseInt(Common.getKonfigurasi("btn_generated_payment", "10").getNilai());
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBtn.java:516");
			}
			String va = "9" + kodeInstitusi + kodePayment + Common.getGeneratedAngkaDigit(digitgenerated);

			jsonObject.put("ref", ref);
			jsonObject.put("va", va);
			jsonObject.put("nama", Common.maxPanjang(biodataCalonMahasiswa.getNama(), 40));
			jsonObject.put("layanan",
					Common.maxPanjang(biodataCalonMahasiswa.getProdiLulus() != null
							? biodataCalonMahasiswa.getProdiLulus().getFakultas().getPerguruanTinggi().getNama()
							: biodataCalonMahasiswa.getProdi1() != null
									? biodataCalonMahasiswa.getProdi1().getFakultas().getPerguruanTinggi().getNama()
									: "",
							40));
			jsonObject.put("kodelayanan",
					biodataCalonMahasiswa.getProdiLulus() != null
							? biodataCalonMahasiswa.getProdiLulus().getFakultas().getPerguruanTinggi()
									.getKodePerguruanTinggi()
							: biodataCalonMahasiswa.getProdi1() != null
									? biodataCalonMahasiswa.getProdi1().getFakultas().getPerguruanTinggi()
											.getKodePerguruanTinggi()
									: "");

			jsonObject.put("jenisbayar",
					Common.maxPanjangAkhir(myjadwalPembayaran.getJenisKegiatan().getNamaKegiatan(), 40));
			jsonObject.put("kodejenisbyr", myjadwalPembayaran.getJenisKegiatan().getKode());

			jsonObject.put("noid", biodataCalonMahasiswa.getNoRegistrasi());
			jsonObject.put("tagihan", total.intValue() + "");
			jsonObject.put("flag", "F");
			jsonObject.put("reserve", biodataCalonMahasiswa.getId() + "");

			jsonObject.put("angkatan", biodataCalonMahasiswa.getTahun() + "");
//			jsonObject.put("expired", DownloadTagihanMahasiswaBankBtn.expiredFormat.get().format(expired_date) ); 
			jsonObject.put("expired", "");
			jsonObject.put("description", "");
//			jsonObject.put("description", Common.maxPanjangAkhir(biodataCalonMahasiswa.getAlamat(),60));

			String postData = jsonObject.toString();

			System.out.println("Request body: ");
			System.out.println(postData);
			String strURL = (Common
					.getKonfigurasi("btn_gateway_url", "https://vabtn-dev.btn.co.id:9021/v1/bstimpr/createVA")
					.getNilai());
			String hasil = DownloadTagihanMahasiswaBankBtn.post(postData, postData, strURL);
			System.out.println("Response body: ");
			System.out.println(hasil);

			JSONObject response = new JSONObject(hasil);

			if (response.getString("rsp").equals("000")) {
				virtualAccountBankBtn = new VirtualAccountBank(PerguruanTinggiUtil.getPerguruanTinggi().getId());
				virtualAccountBankBtn.setKanalPembayaran(
						myjadwalPembayaran == null || myjadwalPembayaran.getJenisKegiatan() == null ? null
								: myjadwalPembayaran.getJenisKegiatan().getKanalPembayaran());
				virtualAccountBankBtn.setKadaluarsa(expired_date);
				virtualAccountBankBtn.setOtomatis(false);
				virtualAccountBankBtn.setKode(va);
				virtualAccountBankBtn.setRequest(postData);
				virtualAccountBankBtn.setResponse(response == null ? "" : response.toString());

				virtualAccountBankBtn.setCicilan(cicilan);
				virtualAccountBankBtn.setJenisKegiatan(myjadwalPembayaran.getJenisKegiatan());
				virtualAccountBankBtn.setKeterangan(pemb);
				virtualAccountBankBtn.setTotal(total);
				virtualAccountBankBtn.setBulanan("");
				virtualAccountBankBtn.setDetailbiaya(detailbiaya);

				virtualAccountBankBtn.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
				virtualAccountBankBtn.setJadwalPembayaran(myjadwalPembayaran);
				virtualAccountBankBtn.setSemester(0);
				virtualAccountBankBtn.setTahunAkademik(myjadwalPembayaran.getTahunAkademik());
				virtualAccountBankBtn.setBank("Bank BTN");

				MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
				session.saveOrUpdate(virtualAccountBankBtn);
				MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);
			}
		}

		MahasiswaVirtualAccountHelper.closeSessionQuietly(session);
			MahasiswaVirtualAccountHelper.closeHibernateContextQuietly();

		return virtualAccountBankBtn;
	} finally {
			// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
			// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBtn.java:605");}
				try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankBtn.java:606");}
			}
		}
	}
}
