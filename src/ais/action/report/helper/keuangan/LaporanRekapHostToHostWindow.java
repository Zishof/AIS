package ais.action.report.helper.keuangan;
import ais.common.PesanFormalHelper;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.poi.xssf.usermodel.extensions.XSSFCellBorder.BorderSide;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.keuangan.DashboardPembayaranMahasiswaPerBulan;
import ais.action.master.dashboard.keuangan.DashboardTunggakanMahasiswaPerBulan;
import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailSettingBiaya;
import ais.database.model.Fakultas;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan rekap host to host window. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchfakultas}, {@code
 * Combobox searchjurusan}, {@code Decimalbox angkatan}, {@code Combobox tahunAkademik}, {@code Combobox
 * semesterAbsensi}, {@code Combobox jenisPembayaran}, {@code Combobox jenisSeleksi}, {@code Combobox jenjang};
 * inisialisasi/lifecycle ({@code init()}, {@code initSpreadsheet()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapHostToHostWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Decimalbox angkatan;
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox jenisPembayaran = new Combobox();
	private Combobox jenisSeleksi = new Combobox();
	private Combobox jenjang = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private MyDatebox start = new MyDatebox();
	private MyDatebox end = new MyDatebox();

	private List<Checkbox> mapItemBiaya = new ArrayList<Checkbox>();

	private Center center = new Center();

	private Combobox semester;

	private Combobox statusAwal;

	private Textbox keterangan;

	private MyCheckboxConfig format;

	public LaporanRekapHostToHostWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Host To Host Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapHostToHostWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void init() {

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabUtama = new MyTabConfig("Rekap Pembayaran");
		tabUtama.setParent(tabs);

		MyTabConfig tab21 = new MyTabConfig("Rekap Biaya Administrasi");
		tab21.setVisible(Common.bolehKonfigurasi("Tampilkan_Rekap_Biaya_Administrasi"));
		tab21.setParent(tabs);

		MyTabConfig tab22 = new MyTabConfig("Rekap Biaya payment Gateway");
		tab22.setVisible(Common.bolehKonfigurasi("Tampilkan_Rekap_payment_Gateway"));
		tab22.setParent(tabs);

		MyTabConfig tab23 = new MyTabConfig("Rekap Biaya Ecampus");
		tab23.setVisible(Common.bolehKonfigurasi("Tampilkan_Rekap_Biaya_Ecampus"));
		tab23.setParent(tabs);

		MyTabConfig tab3 = new MyTabConfig("Info Pembayaran Per-mhs");
		tab3.setParent(tabs);

		MyTabConfig tab31 = new MyTabConfig("Info Pembayaran");
		tab31.setParent(tabs);

		MyTabConfig tab4 = new MyTabConfig("Tagihan Virtual Account");
		tab4.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);

		final Tabpanel tabpanel21 = new ais.ui.util.MyTabpanel();
		tabpanel21.setParent(tabpanels);
		tab21.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel21.getChildren().size() == 0) {
					LaporanRekapBiayaAdministrasiWindow laporanKHS = new LaporanRekapBiayaAdministrasiWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel21);
				}
			}
		});

		final Tabpanel tabpanel22 = new ais.ui.util.MyTabpanel();
		tabpanel22.setParent(tabpanels);
		tab22.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel22.getChildren().size() == 0) {
					LaporanRekapBiayaPaymentGatewayWindow laporanKHS = new LaporanRekapBiayaPaymentGatewayWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel22);
				}
			}
		});

		final Tabpanel tabpanel23 = new ais.ui.util.MyTabpanel();
		tabpanel23.setParent(tabpanels);
		tab23.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel23.getChildren().size() == 0) {
					LaporanRekapBiayaEcampusWindow laporanKHS = new LaporanRekapBiayaEcampusWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel23);
				}
			}
		});

		final Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
		tabpanel3.setParent(tabpanels);
		tab3.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel3.getChildren().size() == 0) {
					DashboardPembayaranMahasiswaPerBulan laporanKHS = new DashboardPembayaranMahasiswaPerBulan();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel3);
				}
			}
		});

		final Tabpanel tabpanel31 = new ais.ui.util.MyTabpanel();
		tabpanel31.setParent(tabpanels);
		tab31.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel31.getChildren().size() == 0) {
					DashboardTunggakanMahasiswaPerBulan laporanKHS = new DashboardTunggakanMahasiswaPerBulan();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel31);
				}
			}
		});

		final Tabpanel tabpanel4 = new ais.ui.util.MyTabpanel();
		tabpanel4.setParent(tabpanels);
		tab4.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel4.getChildren().size() == 0) {
					String url = "/pages/master/virtual_account_bank.zul";
					MyWindow window = new MyWindow("", "none", false);
					window.setHeight("100%");
					window.setWidth("100%");
					window.setParent(tabpanel4);
					MyInclude iframe = new MyInclude(url);
					iframe.setParent(window);
				}
			}
		});

		Common.insertComboDanSemua(jenisPembayaran, "namaKegiatan", JenisKegiatan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(jenisSeleksi, "nama", JenisSeleksi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(jenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		// setTitle("Rekap Pembayaran Host to Host");
		setPosition("center");

		Borderlayout borderlayoutBaru = new Borderlayout();
		borderlayoutBaru.setParent(tabpanelUtama);

		Center centerBaru = new Center();
		ais.ui.util.ZkCompat.setFlex(centerBaru, true);
		centerBaru.setParent(borderlayoutBaru);

		Tabbox tabboxBaru = new Tabbox();
		tabboxBaru.setParent(centerBaru);
		tabboxBaru.setHeight("100%");
		tabboxBaru.setWidth("100%");

		Tabs tabsBaru = new Tabs();
		tabsBaru.setParent(tabboxBaru);

		MyTabConfig tab1 = new MyTabConfig("Rekap Tanggal");
		tab1.setParent(tabsBaru);

		MyTabConfig tab2 = new MyTabConfig("Rekap Mahasiswa");
		tab2.setParent(tabsBaru);

		MyTabConfig tab20 = new MyTabConfig("Rekap Item");
		tab20.setParent(tabsBaru);

		MyTabConfig tab211 = new MyTabConfig("Rekap Item Bulanan");
		tab211.setParent(tabsBaru);

		MyTabConfig tab201 = new MyTabConfig("Per Item");
		tab201.setParent(tabsBaru);

		MyTabConfig tab202 = new MyTabConfig("Per Item dan Mahasiswa");
		tab202.setParent(tabsBaru);

//		MyTabConfig tab203 = new MyTabConfig("Belum Bayar Rekap Item");
//		tab203.setParent(tabsBaru);

		Tabpanels tabpanelsBaru = new Tabpanels();
		tabpanelsBaru.setParent(tabboxBaru);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanelsBaru);

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanelsBaru);
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().size() == 0) {
					LaporanRekapHostToHostCicilanWindow laporanKHS = new LaporanRekapHostToHostCicilanWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel2);
				}
			}
		});

		final Tabpanel tabpanel20 = new ais.ui.util.MyTabpanel();
		tabpanel20.setParent(tabpanelsBaru);
		tab20.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel20.getChildren().size() == 0) {
					LaporanRekapHostToHostCicilanItemWindow laporanKHS = new LaporanRekapHostToHostCicilanItemWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel20);
				}
			}
		});

		final Tabpanel tabpanel211 = new ais.ui.util.MyTabpanel();
		tabpanel211.setParent(tabpanelsBaru);
		tab211.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel211.getChildren().size() == 0) {
					LaporanRekapHostToHostCicilanPerItemBulananWindow laporanKHS = new LaporanRekapHostToHostCicilanPerItemBulananWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel211);
				}
			}
		});

		final Tabpanel tabpanel201 = new ais.ui.util.MyTabpanel();
		tabpanel201.setParent(tabpanelsBaru);
		tab201.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel201.getChildren().size() == 0) {
					LaporanRekapHostToHostCicilanPerItemWindow laporanKHS = new LaporanRekapHostToHostCicilanPerItemWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel201);
				}
			}
		});

		final Tabpanel tabpanel202 = new ais.ui.util.MyTabpanel();
		tabpanel202.setParent(tabpanelsBaru);
		tab202.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel202.getChildren().size() == 0) {
					LaporanRekapHostToHostCicilanPerItemDanMahasiswaWindow laporanKHS = new LaporanRekapHostToHostCicilanPerItemDanMahasiswaWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel202);
				}
			}
		});

//		final Tabpanel tabpanel203 = new ais.ui.util.MyTabpanel();
//		tabpanel203.setParent(tabpanelsBaru);
//		tab203.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel203.getChildren().size() == 0) {
//					LaporanRekapHostToHostCicilanPerItemBelumbayarWindow laporanKHS = new LaporanRekapHostToHostCicilanPerItemBelumbayarWindow();
//					laporanKHS.setHeight("100%");
//					laporanKHS.setWidth("100%");
//					laporanKHS.setParent(tabpanel203);
//				}
//			}
//		});
//
//		tab203.setVisible(false);
//		tabpanel203.setVisible(false);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(tabpanel1);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("130px");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("130px");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("130px");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("130px");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas/Prodi"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(searchfakultas);
		searchfakultas.setCols(2);
		hbox.appendChild(searchjurusan);
		searchjurusan.setCols(2);
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("TA/Smt"));
		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(tahunAkademik);
		tahunAkademik.setCols(5);
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(2);
		hbox.appendChild(semesterAbsensi);
		semesterAbsensi.setCols(5);
		semesterAbsensi.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis/Status Awal"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(jenisPembayaran);
		jenisPembayaran.setCols(5);
		jenisPembayaran.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
						: jenisPembayaran.getSelectedItem().getValue());

				jenisSeleksi.setDisabled(jenisKegiatan == null
						|| !jenisKegiatan.getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA));

				if (jenisSeleksi.isDisabled()) {
					jenisSeleksi.setSelectedItem(null);
				}

			}
		});

		statusAwal = new Combobox();
		Common.insertComboDanSemua(statusAwal, "nama", StatusAwalMahasiswa.class, Restrictions.eq("aktif", true));
		statusAwal.setReadonly(true);
		hbox.appendChild(statusAwal);
		statusAwal.setCols(5);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Smt/Angkt"));
		hbox = new Hbox();
		row.appendChild(hbox);
		semester = new Combobox();
		comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		semester.appendChild(comboitem);
		for (int i = 1; i < 20; i++) {
			Comboitem itemSmt = new Comboitem();
			itemSmt.setValue(i);
			itemSmt.setLabel(i + "");
			semester.appendChild(itemSmt);
		}
		semester.setReadonly(true);
		semester.setCols(5);
		semester.setSelectedIndex(0);
		hbox.appendChild(semester);

		angkatan = new Decimalbox();
		angkatan.setCols(5);
		hbox.appendChild(angkatan);

		row = new MyFormRow();
		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Seleksi"));
		row.appendChild(jenisSeleksi);
		jenisSeleksi.setDisabled(true);
		jenisSeleksi.setSelectedItem(null);
		jenisSeleksi.setWidth("90%");

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) - 1);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		hbox = new Hbox();
		row.appendChild(hbox);
		if (start != null) start.setValue(calendar.getTime());
		hbox.appendChild(start);
		start.setCols(5);
		if (start != null) start.setReadonly(true);

		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);

		if (end != null) end.setValue(calendar.getTime());
		if (end != null) end.setReadonly(true);
		hbox.appendChild(end);
		end.setCols(5);

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang / Ket."));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(jenjang);
		jenjang.setCols(5);
		jenjang.setReadonly(true);
		keterangan = new Textbox();
		hbox.appendChild(keterangan);
		keterangan.setCols(5);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);

		final Vbox vbox = new Vbox();
		vbox.setParent(row);

		EventListener listener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(vbox);
				mapItemBiaya.clear();

				JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
						: jenisPembayaran.getSelectedItem().getValue());

				List<ItemBiaya> itemBiayas = HibernateUtil.currentSession().createCriteria(DetailSettingBiaya.class)
						.createAlias("itemBiaya", "itemBiaya").createAlias("settingBiaya", "settingBiaya")
						.add(jenisKegiatan == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("settingBiaya.jenisKegiatan", jenisKegiatan))
						.add(Restrictions.or(Restrictions.eq("itemBiaya.aktif", true),
								Restrictions.isNull("itemBiaya.aktif")))
						.setProjection(Projections.groupProperty("itemBiaya")).addOrder(Order.asc("itemBiaya")).list();
				Hbox hbox1 = new Hbox();
				vbox.appendChild(hbox1);
				int index = 0;
				for (ItemBiaya itemBiaya : itemBiayas) {
					if (index % 15 == 0) {
						hbox1 = new Hbox();
						vbox.appendChild(hbox1);
					}
					index++;
					Checkbox checkbox = new Checkbox(itemBiaya.getNama());
					checkbox.setAttribute("itemBiaya", itemBiaya);
					mapItemBiaya.add(checkbox);
					checkbox.setChecked(true);
					checkbox.setStyle("font-size:8px");
					checkbox.setParent(hbox1);
				}
				itemBiayas = null;

			}
		};

		jenisPembayaran.addEventListener("onChange", listener);
		try {
			listener.onEvent(null);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/helper/keuangan/LaporanRekapHostToHostWindow.java:640");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Host To Host Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		Hbox toolbar = new Hbox();
		toolbar.setParent(row);
		row.setParent(rows);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initSpreadsheet();
			}
		});
		print.setParent(toolbar);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (spreadsheet == null || spreadsheet.getBook() == null) {
					MyMessageboxConfig.show("Belum ada data untuk diunduh. Silakan tekan Tampilkan terlebih dahulu.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				spreadsheet.getBook().write(bout);
				bout.close();
				Filedownload.save(bout.toByteArray(),
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "REKAP_PEMBAYARAN.xlsx");
			}
		});
		print.setParent(toolbar);

		format = new MyCheckboxConfig("Format Nilai");
		format.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initSpreadsheet();
			}
		});
		format.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

	}

	// private void

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() throws Exception {

//		int tanggal = Common.getBetweenTwoDates(start.getValue(), end.getValue());
//		if (tanggal > 370) {
//			MyMessageboxConfig.show("Tanggal mulai dan sampai pengambilan data tidak boleh lebih dari 370 hari",
//					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
//			return;
//		}

		final Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());

		final Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		final StatusAwalMahasiswa statusAwal = (StatusAwalMahasiswa) (this.statusAwal.getSelectedItem() == null
				|| this.statusAwal.getSelectedItem().getValue() == null ? null
						: this.statusAwal.getSelectedItem().getValue());

		final JenisKegiatan jenisPembayaran = (JenisKegiatan) (LaporanRekapHostToHostWindow.this.jenisPembayaran
				.getSelectedItem() == null ? null
						: LaporanRekapHostToHostWindow.this.jenisPembayaran.getSelectedItem().getValue());
		final JenisSeleksi jenisSeleksi = (JenisSeleksi) (LaporanRekapHostToHostWindow.this.jenisSeleksi
				.getSelectedItem() == null ? null
						: LaporanRekapHostToHostWindow.this.jenisSeleksi.getSelectedItem().getValue());

		final String tahunAkademik = (String) (LaporanRekapHostToHostWindow.this.tahunAkademik.getSelectedItem() == null
				? null
				: LaporanRekapHostToHostWindow.this.tahunAkademik.getSelectedItem().getValue());
		final String semester = (String) (LaporanRekapHostToHostWindow.this.semesterAbsensi.getSelectedItem() == null
				? null
				: LaporanRekapHostToHostWindow.this.semesterAbsensi.getSelectedItem().getValue());

		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final Integer tahunAngkatan = angkatan.getValue() == null ? null : angkatan.getValue().intValue();

		final Jenjang jenjang = (Jenjang) (LaporanRekapHostToHostWindow.this.jenjang.getSelectedItem() == null ? null
				: LaporanRekapHostToHostWindow.this.jenjang.getSelectedItem().getValue());

		final Integer smt = (Integer) (LaporanRekapHostToHostWindow.this.semester.getValue() == null
				|| LaporanRekapHostToHostWindow.this.semester.getSelectedItem() == null ? null
						: LaporanRekapHostToHostWindow.this.semester.getSelectedItem().getValue());

		final TreeMap<String, Object[]> jurusans = new TreeMap<String, Object[]>();
		final TreeMap<String, Double> perValidator = new TreeMap<String, Double>();

		final String fn = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/rekap_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

		final Label label = Common.displayLoadBar(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFFont hlink_font = workbook.createFont();
				hlink_font.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
				hlink_font.setColor(new XSSFColor(Color.BLACK));

				XSSFCellStyle hlink_style = workbook.createCellStyle();
				hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				hlink_style.setFont(hlink_font);

				hlink_style.setBorderLeft(BorderStyle.THIN);
				hlink_style.setBorderTop(BorderStyle.THIN);
				hlink_style.setBorderRight(BorderStyle.THIN);
				hlink_style.setBorderBottom(BorderStyle.DOUBLE);

				hlink_style.setBorderColor(BorderSide.TOP, new XSSFColor(new Color(0, 0, 0)));
				hlink_style.setBorderColor(BorderSide.RIGHT, new XSSFColor(new Color(0, 0, 0)));
				hlink_style.setBorderColor(BorderSide.BOTTOM, new XSSFColor(new Color(0, 0, 0)));
				hlink_style.setBorderColor(BorderSide.LEFT, new XSSFColor(new Color(0, 0, 0)));

				XSSFFont bodyfont = workbook.createFont();
				bodyfont.setBoldweight(XSSFFont.BOLDWEIGHT_NORMAL);
				bodyfont.setColor(new XSSFColor(Color.BLACK));

				XSSFCellStyle bodystyle = workbook.createCellStyle();
				bodystyle.setFont(bodyfont);

				bodystyle.setBorderLeft(BorderStyle.THIN);
				bodystyle.setBorderTop(BorderStyle.THIN);
				bodystyle.setBorderRight(BorderStyle.THIN);
				bodystyle.setBorderBottom(BorderStyle.THIN);

				bodystyle.setBorderColor(BorderSide.TOP, new XSSFColor(new Color(0, 0, 0)));
				bodystyle.setBorderColor(BorderSide.RIGHT, new XSSFColor(new Color(0, 0, 0)));
				bodystyle.setBorderColor(BorderSide.BOTTOM, new XSSFColor(new Color(0, 0, 0)));
				bodystyle.setBorderColor(BorderSide.LEFT, new XSSFColor(new Color(0, 0, 0)));

				XSSFSheet sheet = workbook.createSheet("Data");
				sheet.setDefaultColumnWidth(20);
				int rowIndex = 1;
				XSSFRow rowhead = sheet.createRow(rowIndex);
				XSSFCell cell = rowhead.createCell(0);
				cell.setCellStyle(hlink_style);

				cell.setCellValue("REKAPITULASI PEMBAYARAN "
						+ (jenisPembayaran == null ? "SEMUA JENIS PEMBAYARAN"
								: jenisPembayaran.getNamaKegiatan().toUpperCase())
						+ "\n  "
						+ (fakultas == null || fakultas.getId().equals(-1L) ? "SEMUA " + "Fakultas"
								: "Fakultas" + " " + fakultas.getNama().toUpperCase())
						+ "\n " + (tahunAkademik == null ? "SEMUA TAHUN AKADEMIK" : "TAHUN AKADEMIK " + tahunAkademik)
						+ "\n  " + (semester == null ? "SEMUA SEMESTER" : "SEMESTER " + semester.toUpperCase())
						+ (jenisSeleksi == null || jenisSeleksi.getId() == null ? ""
								: "\nJENIS SELEKSI " + jenisSeleksi.getNama().toUpperCase()));

				for (int k = 1; k <= 5; k++) {
					cell = rowhead.createCell(k);
					cell.setCellStyle(hlink_style);
				}

				rowIndex = 2;
				int k = 0;
				XSSFRow row = sheet.createRow(rowIndex);
				for (String col : new String[] { "Tanggal", "Validator", "Jumlah Mahasiswa", "Total Pembayaran",
						"Jumlah Mahasiswa\nPer Tanggal", "Total Pembayaran\nPer Tanggal" }) {
					cell = row.createCell(k++);
					cell.setCellStyle(hlink_style);
					cell.setCellValue(col);

				}

				rowIndex = 3;
				row = sheet.createRow(rowIndex);

				String tanggal = "";
				Double jumlahTotal = 0.0;
				Double jumlahTotalPertanggal = 0.0;
				Integer jumlah = 0;
				Integer jumlahPertanggal = 0;
				for (Object[] objects : jurusans.values()) {
					if (objects[0] == null) {
						continue;
					}

					if (!tanggal.equals(objects[0].toString())) {

						if (rowIndex != 3) {

							cell = row.createCell(4);
							cell.setCellStyle(hlink_style);
							if (format.isChecked()) {
								cell.setCellValue(Common.numberFormat.get().format(jumlahPertanggal));
							} else {
								cell.setCellValue(jumlahPertanggal);
							}

							cell = row.createCell(5);
							cell.setCellStyle(hlink_style);
							if (format.isChecked()) {
								cell.setCellValue(Common.numberFormat.get().format(jumlahTotalPertanggal));
							} else {
								cell.setCellValue(jumlahTotalPertanggal);
							}

						}

						row = sheet.createRow(rowIndex);
						cell = row.createCell(0);
						cell.setCellStyle(bodystyle);
						cell.setCellValue(objects[0].toString());
						tanggal = objects[0].toString();

						cell = row.createCell(4);
						cell.setCellStyle(hlink_style);

						cell = row.createCell(5);
						cell.setCellStyle(hlink_style);

						jumlahTotalPertanggal = 0.0;
						jumlahPertanggal = 0;
					} else {
						row = sheet.createRow(rowIndex);
						cell = row.createCell(0);
						cell.setCellStyle(bodystyle);

						cell = row.createCell(4);
						cell.setCellStyle(hlink_style);

						cell = row.createCell(5);
						cell.setCellStyle(hlink_style);

					}
					cell = row.createCell(1);
					cell.setCellStyle(bodystyle);
					cell.setCellValue(objects[1] == null ? "" : objects[1].toString());

					Set<String> countMhs = (Set<String>) objects[2];
					Integer c = countMhs == null ? 0 : countMhs.size();
					countMhs.clear();

					cell = row.createCell(2);
					cell.setCellStyle(bodystyle);

					if (format.isChecked()) {
						cell.setCellValue(Common.numberFormat.get().format(c));
					} else {
						cell.setCellValue(c);
					}

					jumlah += c;
					jumlahPertanggal += c;
					Double total = new Double(objects[3] == null ? "0.0" : objects[3].toString());

					cell = row.createCell(3);
					cell.setCellStyle(bodystyle);
					
					if (format.isChecked()) {
						cell.setCellValue(Common.numberFormat.get().format(total));
					} else {
						cell.setCellValue(total);
					}
					
					jumlahTotal += total;
					jumlahTotalPertanggal += total;

					rowIndex++;
				}

				if (rowIndex != 3) {

					cell = row.createCell(4);
					cell.setCellStyle(hlink_style);
					if (format.isChecked()) {
						cell.setCellValue(Common.numberFormat.get().format(jumlahPertanggal));
					} else {
						cell.setCellValue(jumlahPertanggal);
					}

					cell = row.createCell(5);
					cell.setCellStyle(hlink_style);
					if (format.isChecked()) {
						cell.setCellValue(Common.numberFormat.get().format(jumlahTotalPertanggal));
					} else {
						cell.setCellValue(jumlahTotalPertanggal);
					}
				}

				row = sheet.createRow(rowIndex);
				cell = row.createCell(0);
				cell.setCellStyle(hlink_style);
				cell.setCellValue("TOTAL");

				cell = row.createCell(1);
				cell.setCellStyle(hlink_style);
				cell.setCellValue("");

				cell = row.createCell(2);
				cell.setCellStyle(hlink_style);
				cell.setCellValue("");

				cell = row.createCell(3);
				cell.setCellStyle(hlink_style);
				cell.setCellValue("");

				cell = row.createCell(4);
				cell.setCellStyle(hlink_style);
				if (format.isChecked()) {
					cell.setCellValue(Common.numberFormat.get().format(jumlah));
				} else {
					cell.setCellValue(jumlah);
				}

				cell = row.createCell(5);
				cell.setCellStyle(hlink_style);
				if (format.isChecked()) {
					cell.setCellValue(Common.numberFormat.get().format(jumlahTotal));
				} else {
					cell.setCellValue(jumlahTotal);
				}

				rowIndex++;
				rowIndex++;

				row = sheet.createRow(rowIndex);
				cell = row.createCell(0);
				cell.setCellStyle(hlink_style);
				cell.setCellValue("Validator");

				cell = row.createCell(1);
				cell.setCellStyle(hlink_style);
				cell.setCellValue("Nilai Total");

				Double totalSemua = 0.0;
				for (String key : perValidator.keySet()) {
					rowIndex++;
					Double tot = perValidator.get(key);
					totalSemua += tot;

					row = sheet.createRow(rowIndex);
					cell = row.createCell(0);
					cell.setCellStyle(hlink_style);
					cell.setCellValue(key);

					cell = row.createCell(1);
					cell.setCellStyle(hlink_style);
					if (format.isChecked()) {
						cell.setCellValue(Common.numberFormat.get().format(tot));
					} else {
						cell.setCellValue(tot);
					}

				}

				rowIndex++;

				row = sheet.createRow(rowIndex);
				cell = row.createCell(0);
				cell.setCellStyle(hlink_style);
				cell.setCellValue("TOTAL");

				cell = row.createCell(1);
				cell.setCellStyle(hlink_style);
				if (format.isChecked()) {
					cell.setCellValue(Common.numberFormat.get().format(jumlahTotal));
				} else {
					cell.setCellValue(jumlahTotal);
				}

				for (int i = 0; i < 6; i++) {
					try {
						sheet.autoSizeColumn(i);
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e,
								"autosize laporan rekap host-to-host kolom " + i);
					}
				}

				File file = new File(fn);

				try {
					FileOutputStream fileOut = new FileOutputStream(file);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

				spreadsheet = new ais.ui.util.MySpreadsheet();
				Common.clear(center);
				spreadsheet.setParent(center);
				spreadsheet.setWidth("100%");
				spreadsheet.setHeight("100%");
				spreadsheet.setSrc("../../tmp/" + file.getName());
				spreadsheet.setMaxcolumns(6);
				spreadsheet.setMaxrows(jurusans.size() + 25);

				jurusans.clear();

				// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {

				try {

					List<Long> ids = new ArrayList<Long>();
					for (Checkbox checkbox : mapItemBiaya) {
						if (checkbox.isChecked()) {
							ItemBiaya itemBiaya = (ItemBiaya) checkbox.getAttribute("itemBiaya");
							ids.add(itemBiaya.getId());
						}
					}

					List<Long> cicilanPembayarans = new ArrayList<Long>();

					try {
						Session session1 = ais.action.report.Report.openNativeSession();

						cicilanPembayarans = session1.createCriteria(CicilanPembayaran.class)

								.add(keterangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("keterangan", keterangan.getValue().trim(),
												MatchMode.ANYWHERE))

								.setProjection(Projections.property("id"))

								.add(ids.isEmpty() ? Restrictions.sqlRestriction("false")
										: Restrictions.in("itemBiaya.id", ids))

								.add(Restrictions.sqlRestriction("date(this_.tanggal) between date('"
										+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
										+ Common.databaseDateFormat.get().format(end.getValue()) + "')"))
								.list();
						session1.disconnect();
						session1.close();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/helper/keuangan/LaporanRekapHostToHostWindow.java:1093");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Host To Host Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}

					ais.action.report.Report.closeCurrentSessionQuietly();

					int size = cicilanPembayarans.size();
					int index = 0;
					for (Long cicilanPembayaranId : cicilanPembayarans) {
						try {
							index++;
							Session session1 = ais.action.report.Report.openNativeSession();
							CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session1
									.createCriteria(CicilanPembayaran.class).add(Restrictions.idEq(cicilanPembayaranId))
									.uniqueResult();
							session1.disconnect();
							session1.close();
							ais.action.report.Report.closeCurrentSessionQuietly();
							label.setValue("Ambil data " + cicilanPembayaran.toString() + " ("
									+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

							if ((cicilanPembayaran.getItemBiaya() != null
									&& ids.contains(cicilanPembayaran.getItemBiaya().getId()))) {

								Kegiatan kegiatan = cicilanPembayaran.getKegiatan();

								if (kegiatan != null) {

									if (smt == null || smt.equals(kegiatan.getSemster())) {

										if (semester == null
												|| (semester.equals(Perkuliahan.GENAP) ? kegiatan.getSemster() % 2 == 0
														: kegiatan.getSemster() % 2 == 1)) {

											if (tahunAkademik == null
													|| tahunAkademik.equals(kegiatan.getTahunAkademik())) {

												if (jenisPembayaran == null || jenisPembayaran.getId()
														.equals(kegiatan.getJenisKegiatan().getId())) {

													Mahasiswa mahasiswa = kegiatan.getMahasiswa();
													BiodataCalonMahasiswa biodataCalonMahasiswa = kegiatan
															.getCalonMahasiswa();

													if (tahunAngkatan == null
															|| (biodataCalonMahasiswa != null
																	&& biodataCalonMahasiswa.getTahun() != null
																	&& tahunAngkatan
																			.equals(biodataCalonMahasiswa.getTahun()))

															|| (mahasiswa != null
																	&& mahasiswa.getTahunangkatan() != null
																	&& tahunAngkatan
																			.equals(mahasiswa.getTahunangkatan()))

													) {

														if (program == null
																|| (biodataCalonMahasiswa != null
																		&& biodataCalonMahasiswa.getProgram() != null
																		&& program.equals(
																				biodataCalonMahasiswa.getProgram()))

																|| (mahasiswa != null && mahasiswa.getProgram() != null
																		&& program.equals(mahasiswa.getProgram()))

														) {

															if (statusAwal == null
																	|| (biodataCalonMahasiswa != null
																			&& biodataCalonMahasiswa
																					.getStatusAwalMahasiswa() != null
																			&& statusAwal.getId()
																					.equals(biodataCalonMahasiswa
																							.getStatusAwalMahasiswa()
																							.getId()))

																	|| (mahasiswa != null
																			&& mahasiswa
																					.getStatusAwalMahasiswa() != null
																			&& statusAwal.getId().equals(mahasiswa
																					.getStatusAwalMahasiswa().getId()))

															) {

																if (jenisSeleksi == null
																		|| (biodataCalonMahasiswa != null
																				&& biodataCalonMahasiswa
																						.getJenisSeleksi() != null
																				&& jenisSeleksi.getId()
																						.equals(biodataCalonMahasiswa
																								.getJenisSeleksi()
																								.getId()))

																		|| (mahasiswa != null
																				&& mahasiswa.getJenisSeleksi() != null
																				&& jenisSeleksi.getId().equals(mahasiswa
																						.getJenisSeleksi().getId()))

																) {

																	if (fakultas == null || fakultas.getId().equals(-1L)

																			|| (mahasiswa != null
																					&& mahasiswa.getJurusan() != null
																					&& mahasiswa.getJurusan()
																							.getFakultas().getId()
																							.equals(fakultas.getId()))

																			|| (biodataCalonMahasiswa != null
																					&& biodataCalonMahasiswa
																							.getProdi1() != null
																					&& biodataCalonMahasiswa.getProdi1()
																							.getFakultas().getId()
																							.equals(fakultas.getId()))

																			|| (biodataCalonMahasiswa != null
																					&& biodataCalonMahasiswa
																							.getProdiLulus() != null
																					&& biodataCalonMahasiswa
																							.getProdiLulus()
																							.getFakultas().getId()
																							.equals(fakultas
																									.getId()))) {

																		if (jurusan == null
																				|| jurusan.getId().equals(-1L)

																				|| (mahasiswa != null
																						&& mahasiswa
																								.getJurusan() != null
																						&& mahasiswa.getJurusan()
																								.getId()
																								.equals(jurusan
																										.getId()))

																				|| (biodataCalonMahasiswa != null
																						&& biodataCalonMahasiswa
																								.getProdi1() != null
																						&& biodataCalonMahasiswa
																								.getProdi1().getId()
																								.equals(jurusan
																										.getId()))

																				|| (biodataCalonMahasiswa != null
																						&& biodataCalonMahasiswa
																								.getProdiLulus() != null
																						&& biodataCalonMahasiswa
																								.getProdiLulus().getId()
																								.equals(jurusan
																										.getId()))) {

																			if (jenjang == null
																					|| jenjang.getId().equals(-1L)

																					|| (mahasiswa != null && mahasiswa
																							.getJurusan() != null
																							&& mahasiswa.getJurusan()
																									.getJenjang()
																									.getId()
																									.equals(jenjang
																											.getId()))

																					|| (biodataCalonMahasiswa != null
																							&& biodataCalonMahasiswa
																									.getProdi1() != null
																							&& biodataCalonMahasiswa
																									.getProdi1()
																									.getJenjang()
																									.getId()
																									.equals(jenjang
																											.getId()))

																					|| (biodataCalonMahasiswa != null
																							&& biodataCalonMahasiswa
																									.getProdiLulus() != null
																							&& biodataCalonMahasiswa
																									.getProdiLulus()
																									.getJenjang()
																									.getId()
																									.equals(jenjang
																											.getId()))) {

																				String key = Common.dateFormat8.get().format(
																						cicilanPembayaran.getTanggal())
																						+ "_"
																						+ (cicilanPembayaran
																								.getValidator() == null
																										? "Tidak ada validator"
																										: cicilanPembayaran
																												.getValidator());

																				Object[] objSbm = jurusans.get(key);
																				Set<String> countMhs;
																				Double total;
																				if (objSbm == null) {
																					countMhs = new HashSet<String>();
																					total = 0.0;
																				} else {
																					countMhs = (Set<String>) objSbm[2];
																					total = (Double) objSbm[3];
																				}

																				total += cicilanPembayaran.getNilai();

																				if (mahasiswa != null) {
																					countMhs.add(kegiatan.getMahasiswa()
																							.getId() + "_mhs");
																				} else if (biodataCalonMahasiswa != null) {
																					countMhs.add(kegiatan
																							.getCalonMahasiswa().getId()
																							+ "_calon_mhs");
																				}

																				String val = (cicilanPembayaran
																						.getValidator() == null
																								? "Tidak ada validator"
																								: cicilanPembayaran
																										.getValidator());

																				Object[] objects = new Object[] {
																						Common.dateFormat4.get().format(
																								cicilanPembayaran
																										.getTanggal()),
																						val, countMhs, total };
																				jurusans.put(key, objects);

																				Double nilai = perValidator.get(val);
																				if (nilai == null) {
																					nilai = 0.0;
																				}
																				nilai += cicilanPembayaran.getNilai();
																				perValidator.put(val, nilai);
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/helper/keuangan/LaporanRekapHostToHostWindow.java:1339");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Host To Host Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
						}
					}
					cicilanPembayarans = null;

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/helper/keuangan/LaporanRekapHostToHostWindow.java:1345");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Host To Host Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
				ais.action.report.helper.LoadingReportUtil.selesai(label);

			}
		}).start();

	}
}
