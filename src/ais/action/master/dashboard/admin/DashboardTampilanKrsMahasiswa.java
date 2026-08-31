package ais.action.master.dashboard.admin;
import ais.ui.util.DashboardGridExportHelper;

import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyWindow;

/**
 * Komponen dashboard khusus untuk dashboard tampilan krs mahasiswa. Kelas ini memilih variasi data
 * atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Div center}, {@code Mahasiswa
 * mahasiswa}, {@code Integer semesterPendek}; inisialisasi/lifecycle ({@code init()}); pembacaan/pencarian
 * ({@code reload()}); konfigurasi constructor: {@code mahasiswa}. Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardTampilanKrsMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;

	private Div center;
	private Mahasiswa mahasiswa = null;

	private Integer semesterPendek;

	public DashboardTampilanKrsMahasiswa(Integer semesterPendek) {
		super();
		try {
			this.semesterPendek = semesterPendek;
			mahasiswa = Common.getCurrentUser().getMahasiswa();
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardTampilanKrsMahasiswa(Integer semesterPendek, String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			this.semesterPendek = semesterPendek;
			mahasiswa = Common.getCurrentUser().getMahasiswa();
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Tampilan Krs Mahasiswa");
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		org.zkoss.zk.ui.Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Aksi & Saringan",
				"Tombol data KRS dan saringan untuk menyesuaikan data yang ditampilkan.",
				"Tampilan KRS Mahasiswa",
				"Rekap tampilan KRS mahasiswa, beserta grafiknya.");
		org.zkoss.zk.ui.Component saringanHost = hostPortal[0];
		center = (org.zkoss.zul.Div) hostPortal[1];

		Toolbar hbox = new Toolbar();
		hbox.setParent(saringanHost);

		MyButtonConfig myButtonConfig = new MyButtonConfig("Data KRS", "/img/12123-eyes-icon.png");
		myButtonConfig.setParent(hbox);
		myButtonConfig.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				MyWindow laporan = new MyWindow();
				laporan.setHeight("99%");
				laporan.setWidth("99%");
				laporan.setTitle("Data KRS");
				laporan.setClosable(true);
				laporan.setBorder("none");

				Borderlayout borderlayout = new Borderlayout();
				laporan.appendChild(borderlayout);

				Center center = new Center();
				ais.ui.util.ZkCompat.setFlex(center, true);
				center.setParent(borderlayout);

				center.appendChild(new Iframe("/pages/master/krs.zul"));
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporan);
				laporan.onModal();
			}
		});

		myButtonConfig = new MyButtonConfig("Data Nilai", "/img/12123-eyes-icon.png");
		myButtonConfig.setParent(hbox);
		myButtonConfig.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				MyWindow laporan = new MyWindow();
				laporan.setHeight("99%");
				laporan.setWidth("99%");
				laporan.setTitle("Data KRS");
				laporan.setClosable(true);
				laporan.setBorder("none");

				Borderlayout borderlayout = new Borderlayout();
				laporan.appendChild(borderlayout);

				Center center = new Center();
				ais.ui.util.ZkCompat.setFlex(center, true);
				center.setParent(borderlayout);

				center.appendChild(new Iframe("/pages/master/nilai_mahasiswa.zul"));
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporan);
				laporan.onModal();
			}
		});

		myButtonConfig = new MyButtonConfig("Refresh", "/img/Button-Refresh-icon.png");
		myButtonConfig.setParent(hbox);
		myButtonConfig.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						reload(true);
					}
				});
			}
		});



		reload(false);

	}

	private void reload(boolean refresh) {
		Common.clear(center);
		if (mahasiswa == null) {
			return;
		}

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Integer mulai = 1;
		Integer sampai = mahasiswa.currentSemester();

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("");
		column.setParent(columns);
		column.setWidth("0px");

		column = new MyColumnConfig("TA/SMT");
		column.setParent(columns);
		column.setWidth("34%");

		column = new MyColumnConfig("IP/IPK");
		column.setParent(columns);
		column.setWidth("33%");

		column = new MyColumnConfig("SKS/SKSK");
		column.setParent(columns);
		column.setWidth("33%");

		Rows rows = new Rows();
		rows.setParent(grid);

		List<String[]> datas = Common.generateSemestersForGrid(mahasiswa, mulai, sampai, semesterPendek);
		Integer semester = 0;
		for (String[] data : datas) {

			Integer smt;
			try {
				smt = Integer.parseInt(data[1].split(",")[0]);
			} catch (Exception e) {
				smt = 0;
			}
			Integer tahap;
			try {
				tahap = Integer.parseInt(data[3]);
			} catch (Exception e) {
				tahap = 0;
			}
			Integer tahapan = tahap;
			semester = smt;
			final KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan,
					semesterPendek, refresh);
			if (krsMahasiswa.getSemester() > 0) {
				Row row = new Row();
		row.setValign("top");
				row.setParent(rows);

				MyDetail detail = new MyDetail();
				detail.setParent(row);
				detail.setOpen(true);

				new Label(krsMahasiswa.getTahunAkademik() + "/" + krsMahasiswa.getSemester()).setParent(row);

				new Label(Common.numberFormat.get().format(krsMahasiswa.getIps()) + "/"
						+ Common.numberFormat.get().format(krsMahasiswa.getIpk())).setParent(row);

				new Label(Common.numberFormat.get().format(krsMahasiswa.getSksYangDiambil()) + "/"
						+ Common.numberFormat.get().format(krsMahasiswa.getSksk())).setParent(row);

				Vbox vbox = new Vbox();
				vbox.setParent(detail);
				Html html = new ais.ui.util.MyHtml(mahasiswa.rubahKeteranganPengambilanKRS(semester, tahapan,
						semesterPendek, krsMahasiswa, false));
				html.setParent(vbox);
				MyLabelAgakKecil catatan = new MyLabelAgakKecil(krsMahasiswa.getCatatan());
				MyLabelAgakKecil catatanKhs = new MyLabelAgakKecil(krsMahasiswa.getCatatanKhs());
				catatan.setParent(vbox);
				catatanKhs.setParent(vbox);

				Hbox hbox = new Hbox();
				hbox.setParent(vbox);

				MyButtonConfig button = new MyButtonConfig("Cetak " + Common.getBahasa("label_krs"), "/img/print.png");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						CommonReportHelper.cetakKRS(krsMahasiswa.getMahasiswa(), krsMahasiswa.getSemester(),
								krsMahasiswa.getTahapan(), krsMahasiswa.getSemesterPendek(), false);
					}

				});
				button.setParent(hbox);

				button = new MyButtonConfig("Cetak Kartu UTS", "/img/print.png");
				button.setVisible(Common.bolehKonfigurasi("tampilkan_tombol_cetak_kartu_uts"));
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						CommonReportHelper.cetakUTS(krsMahasiswa.getMahasiswa(), krsMahasiswa.getSemester(),
								krsMahasiswa.getTahapan(), krsMahasiswa.getTahunAkademik(),
								krsMahasiswa.getSemesterPendek(), false, false);
					}

				});
				button.setParent(hbox);

				button = new MyButtonConfig("Cetak Kartu UAS", "/img/print.png");
				button.setVisible(Common.bolehKonfigurasi("tampilkan_tombol_cetak_kartu_uas"));
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						CommonReportHelper.cetakUAS(krsMahasiswa.getMahasiswa(), krsMahasiswa.getSemester(),
								krsMahasiswa.getTahapan(), krsMahasiswa.getTahunAkademik(),
								krsMahasiswa.getSemesterPendek(), false, false);

					}

				});
				button.setParent(hbox);
			}

		}
		setStyle("min-height:" + (120 + (50 * semester)) + "px");
	}
}
