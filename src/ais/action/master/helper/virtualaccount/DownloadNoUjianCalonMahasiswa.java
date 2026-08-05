package ais.action.master.helper.virtualaccount;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;

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
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonPMB;
import ais.common.GetFile;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DownloadNoUjianCalonMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;
	public static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	static JenisKegiatan jenisKegiatan = pembayaranUtil
			.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);

	private Center center = new Center();
	private Intbox searchangkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(
			Calendar.YEAR));

	private File file;

	public DownloadNoUjianCalonMahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Unduh Nomor Ujian Calon Mahasiswa",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Unduh Nomor Ujian Calon Mahasiswa.",
							"Periksa apakah data Jenis Kegiatan Pendaftaran Ulang Mahasiswa Baru sudah dikonfigurasi dengan benar.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
		}
	}

	public DownloadNoUjianCalonMahasiswa(String title, String border,
			boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"Membuka jendela Unduh Nomor Ujian Calon Mahasiswa",
					e,
					new String[] {
							"Muat ulang (refresh) halaman ini, lalu buka kembali jendela Unduh Nomor Ujian Calon Mahasiswa.",
							"Periksa apakah data Jenis Kegiatan Pendaftaran Ulang Mahasiswa Baru sudah dikonfigurasi dengan benar.",
							"Jika jendela tetap gagal terbuka setelah beberapa kali percobaan, hubungi Administrator." });
		}
	}

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
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);

		MyGrid grid = new MyGrid();grid.setWidth("100%");
		grid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Penerimaan Mahasiswa Baru"));
		row.appendChild(searchangkatan);
		searchangkatan.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig search = new MyToolbarbuttonConfig("Tampilkan Data",
				"/img/svg/search.svg");
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
					MyMessageboxConfig
							.show("Tahun Angkatan harus dipilih", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}
				if (file == null) {
					MyMessageboxConfig
							.show("Click \"Tampilkan Data\" terlebih dahulu",
									"Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
					return;
				}

				try {
					Filedownload.save(new FileInputStream(file),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"TAGIHAN_NO_UJIAN_CALON_MAHASISWA_TAHUN_"
									+ searchangkatan.getValue() + ".xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoUjianCalonMahasiswa.java:168");

				}
			}
		});
		print.setParent(toolbar);

		CommonPMB.createDownloadBRIFormat(new GetFile() {

			@Override
			public File getFile() {
				return file;
			}
		}).setParent(toolbar);
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

		final String filename = Sessions
				.getCurrent()
				.getWebApp()
				.getRealPath(
						"/tmp/data_"
								+ URLEncoder.encode(
										Common.dateFormat7.get().format(ais.ui.util.WaktuUtil.getDate()),
										"UTF-8") + ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		new Thread(new Runnable() {

			@Override
			public void run() {

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFCellStyle lockedNumericStyle = workbook.createCellStyle();
				lockedNumericStyle
						.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				lockedNumericStyle
						.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				lockedNumericStyle.setLocked(true);

				XSSFSheet sheet = workbook.createSheet("TAGIHAN_NO_UJIAN");
				sheet.protectSheet("passwordrahasia");
				sheet.setDefaultColumnWidth(25);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("ID DATA");
				rowhead.createCell(1).setCellValue("NO. UJIAN CALON MAHASISWA");
				rowhead.createCell(2).setCellValue("NAMA CALON MAHASISWA");
				rowhead.createCell(3).setCellValue("PROGRAM STUDI");
				rowhead.createCell(4).setCellValue("TAGIHAN");
				rowhead.createCell(5).setCellValue("RINCIAN TAGIHAN");

				Session session = MahasiswaVirtualAccountHelper.openSession();
				try {

				List<BiodataCalonMahasiswa> biodataCalonMahasiswas = session
						.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("id"))
						.add(Restrictions.isNotNull("prodiLulus"))
						.add(Restrictions.isNotNull("noUjian"))
						.add(Restrictions.ne("noUjian", ""))
						.add(searchangkatan.getValue() == null ? Restrictions
								.sqlRestriction("1=1") : Restrictions.eq(
								"tahun", searchangkatan.getValue())).list();

				int size = biodataCalonMahasiswas.size();

				int rowIndex = 1;
				for (BiodataCalonMahasiswa biodataCalonMahasiswa : biodataCalonMahasiswas) {
					label.setValue("Sedang memproses data "
							+ biodataCalonMahasiswa.toString()
							+ " ("
							+ Common.numberFormat.get().format(rowIndex * 100.0
									/ size) + " %)");
					createData(session, biodataCalonMahasiswa, sheet, rowIndex,
							lockedNumericStyle);
					rowIndex++;
				}

				Common.setStyled(sheet);sizedata.setValue(rowIndex + 1);

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException(
							"Menyimpan berkas Excel hasil unduhan Nomor Ujian Calon Mahasiswa",
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
						"pengunduhan Nomor Ujian Virtual Account Calon Mahasiswa", null, e,
						new String[] {
								"Periksa kembali data/filter (Tahun Angkatan) yang dipilih dan coba ulangi.",
								"Periksa ketersediaan ruang penyimpanan (disk space) pada server untuk berkas Excel sementara.",
								"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
						.replace("\n", " "));
			} finally {
					// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
					// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
					if (session != null && session.isOpen()) {
						try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoUjianCalonMahasiswa.java:288");}
						try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoUjianCalonMahasiswa.java:289");}
					}
				}
			}
		}).start();

	}

	private void createData(Session session,
			BiodataCalonMahasiswa biodataCalonMahasiswa, XSSFSheet sheet,
			int rowIndex, XSSFCellStyle lockedNumericStyle) {

		Jurusan myjurusan1 = biodataCalonMahasiswa.getProdiLulus();
		java.util.Collection<DetailBiaya> detailBiayas = pembayaranUtil
				.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa,
						jenisKegiatan, myjurusan1, null, false);
		String pemb = "|";
		Double total = 0.0;

		for (DetailBiaya biaya : detailBiayas) {
			ItemBiaya itemBiaya = biaya.getItemBiaya();
			Double nilai = biaya.hitungTotal();
			if (nilai > 0.01) {
				pemb += itemBiaya.getKode().trim() + "\\"
						+ itemBiaya.getNama().trim() + "\\"
						+ itemBiaya.getDeskripsi().trim() + "\\"
						+ (nilai).longValue() + "|";
				total += nilai;
			}
		}

		XSSFRow row = sheet.createRow(rowIndex);

		XSSFCell cell = row.createCell(0);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(biodataCalonMahasiswa == null || biodataCalonMahasiswa.getId() == null ? -1L : biodataCalonMahasiswa.getId());

		cell = row.createCell(1);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(biodataCalonMahasiswa.getNoUjian());

		cell = row.createCell(2);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(biodataCalonMahasiswa.getNama());

		String prodi = "";
		if (biodataCalonMahasiswa.getProdi1() != null) {
			prodi += biodataCalonMahasiswa.getProdi1().getNama() + ",";
		}
		if (biodataCalonMahasiswa.getProdi2() != null) {
			prodi += biodataCalonMahasiswa.getProdi2().getNama() + ",";
		}
		if (biodataCalonMahasiswa.getProdi3() != null) {
			prodi += biodataCalonMahasiswa.getProdi3().getNama() + ",";
		}
		if (biodataCalonMahasiswa.getProdi4() != null) {
			prodi += biodataCalonMahasiswa.getProdi4().getNama() + ",";
		}
		if (biodataCalonMahasiswa.getProdi5() != null) {
			prodi += biodataCalonMahasiswa.getProdi5().getNama() + ",";
		}

		cell = row.createCell(3);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(prodi);

		cell = row.createCell(4);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(total);

		cell = row.createCell(5);
		cell.setCellStyle(lockedNumericStyle);
		cell.setCellValue(pemb);

	}
}
