package ais.action.master.epsbed;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.North;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk master perguruan tinggi. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Spreadsheet spreadsheet}, {@code Center
 * center}, {@code PembayaranUtil pembayaranUtil}; inisialisasi/lifecycle ({@code init()}, {@code
 * initSpreadsheet()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class MasterPerguruanTinggi extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	public MasterPerguruanTinggi() {
		super();
		try {
			init();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public MasterPerguruanTinggi(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
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

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		// toolbar.setHeight("25px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig search = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				System.out.println("search");
				initSpreadsheet();
			}
		});
		search.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Export Epsbed (MSPTI.xls)",
				"/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					spreadsheet.getBook().write(bout);
					bout.close();
					Filedownload.save(bout.toByteArray(),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "MSPTI.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/epsbed/MasterPerguruanTinggi.java:99");
				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		initSpreadsheet();
	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() {

		Common.clear(center);

		List<PerguruanTinggi> perguruanTinggis = HibernateUtil.currentSession()
				.createCriteria(PerguruanTinggi.class).setMaxResults(1).list();

		if (perguruanTinggis.size() == 0) {
			return;
		}

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(14);
		spreadsheet.setMaxrows(perguruanTinggis.size() + 1);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(30);

		int rowIndex = 0;
		int colIndex = 0;

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "KDYYSMSPT");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "KDPTIMSPT");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "NMPTIMSPT");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "ALMT1MSPT");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "ALMT2MSPT");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "KOTAAMSPT");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "KDPOSMSPT");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "TELPOMSPT");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "FAKSIMSPT");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, "NOMSKMSPT");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, "TGPTIMSPT");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, "EMAILMSPT");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12, "HPAGEMSPT");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, "TGAWLMSPT");

		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1,
						rowIndex), true);

		rowIndex++;
		for (PerguruanTinggi perguruanTinggi : perguruanTinggis) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0,
					perguruanTinggi.getKodeYayasan());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
					perguruanTinggi.getKodePerguruanTinggi());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, perguruanTinggi.getNama());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, perguruanTinggi.getAlamat1());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, perguruanTinggi.getAlamat2());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, perguruanTinggi.getKota());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, perguruanTinggi.getKodePos());

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, perguruanTinggi.getTelepon());

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8,
					perguruanTinggi.getFaksimili());

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9,
					perguruanTinggi.getNomorAkta());

			ais.ui.util.EcampusUtil.setCellValue(
					sheet,
					rowIndex,
					10,
					perguruanTinggi.getTanggalAkta() == null ? ""
							: Common.dateFormat2.get().format(perguruanTinggi
									.getTanggalAkta()));

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, perguruanTinggi.getEmail());

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12,
					perguruanTinggi.getWebsite());

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, Common.dateFormat2.get()
					.format(perguruanTinggi.getTanggalAwalPendirian()));

			rowIndex++;
		}

		Common.setStyled(sheet);spreadsheet.setMaxrows(rowIndex + 1);

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
	}
}
