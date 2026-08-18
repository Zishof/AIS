package ais.action.report.format1.sekolah;
import ais.common.PesanFormalHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Window;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;

import ais.action.master.helper.AmbilDataAsramaBanbox;
import ais.action.master.sapto.util.SaptoUtil;
import ais.action.master.sekolah.helper.AmbilDataKelasSiswaBanbox;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.AsramaSiswa;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Dashboard/Grid version of LaporanRincianPembayaranSiswa.
 *
 * Tujuan:
 * - Mengonversi seluruh tab Excel lama menjadi dashboard, grafik, dan grid paging 10 baris.
 * - Tetap menyediakan preview/download Excel, tetapi baru diproses saat tombol Download Excel diklik.
 * - Menjaga filter laporan lama: yayasan, sekolah, tahun ajaran, siswa/calon siswa, jenis biaya,
 *   angkatan, cara pembayaran, nama cara pembayaran, bulan, tahun, kelas, asrama, dan rentang tanggal.
 * - Menambahkan global filter modern seperti DasboardPembayaranSekolah.
 *
 * Kompatibel Java 1.6/1.7 dan ZK 5.5: tanpa lambda/stream.
 */
public class LaporanRincianPembayaranSiswaGrid extends MyWindow {

	private static final long serialVersionUID = 2026052903L;
	private static final int GRID_PAGE_SIZE = 10;
	private static final int MAX_CHART_ITEMS = 12;
	private static final int MAX_DYNAMIC_ITEM_COLUMNS = 60;

	private Div rootContainer;
	private Div body;
	private Label processInfo;
	private Html loadingDashboardHtml;
	private int dashboardProgress = 0;

	private Combobox yayasan;
	private Combobox sekolah;
	private Combobox tahunAjaran;
	private Combobox jenisBiayaSekolah;
	private Combobox akunPembayaranSiswa;
	private Combobox tahun;
	private Combobox bulan;
	private Combobox angkatan;
	private Textbox siswa;
	private Siswa selectedSiswa = null;
	private Textbox itemBiayaKeyword;
	private Textbox keywordGlobal;
	private Textbox namaAkunPembayaranSiswa;
	private MyDatebox mulai;
	private MyDatebox sampai;
	private AmbilDataKelasSiswaBanbox kelas;
	private AmbilDataAsramaBanbox asrama;

	private String dashboardTahunAjaran = Common.getCurrentTahunAkademik();
	private String dashboardItemBiayaKeyword = "";
	private String dashboardKeyword = "";
	private Sekolah sk = null;

	public LaporanRincianPembayaranSiswaGrid() {
		super("Dashboard Rincian Pembayaran Siswa", "none", false);
		try {
			sk = SekolahUtil.getSekolah();
			loadSelectedSiswaFromRequest();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rincian Pembayaran Siswa Grid", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void loadSelectedSiswaFromRequest() {
		try {
			if (ExecutionsCtrl.getCurrent() != null && ExecutionsCtrl.getCurrent().getParameter("siswa") != null) {
				selectedSiswa = (Siswa) HibernateUtil.currentSession().createCriteria(Siswa.class)
						.add(Restrictions.isNotNull("namaSiswa"))
						.add(Restrictions.ne("namaSiswa", ""))
						.add(Restrictions.isNotNull("sekolah"))
						.add(Restrictions.idEq(Long.parseLong(ExecutionsCtrl.getCurrent().getParameter("siswa"))))
						.uniqueResult();
			}
		} catch (Exception e) {
			selectedSiswa = null;
		}
	}

	private void init() throws Exception {
		setWidth("100%");
		setHeight("100%");

		rootContainer = new Div();
		rootContainer.setParent(this);
		rootContainer.setWidth("100%");
		rootContainer.setHeight("100%");
		rootContainer.setStyle("overflow:auto; background:#f8fafc; padding:12px; box-sizing:border-box;");

		renderHeader(rootContainer);
		renderPembayaranGlobalFilter(rootContainer);

		body = new Div();
		body.setParent(rootContainer);
		body.setWidth("100%");
		body.setStyle("margin-top:12px; box-sizing:border-box;");

		renderAllDashboards();
	}

	private void renderHeader(Component parent) {
		Div hero = new Div();
		hero.setParent(parent);
		hero.setWidth("100%");
		hero.setStyle("position:relative; overflow:hidden; border-radius:18px; padding:22px 24px; color:#ffffff;"
				+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);"
				+ "box-shadow:0 14px 30px rgba(15,23,42,.16); box-sizing:border-box;");
		appendHtml(hero, "<div style='position:absolute;right:-60px;top:-70px;width:220px;height:220px;border-radius:999px;background:rgba(255,255,255,.12);'></div>"
				+ "<div style='position:relative;z-index:1;'>"
				+ "<div style='font-size:12px;letter-spacing:.12em;opacity:.86;text-transform:uppercase;'>School Payment Report Grid</div>"
				+ "<div style='font-size:27px;font-weight:900;line-height:1.15;margin-top:5px;'>Dashboard Rincian Pembayaran Siswa</div>"
				+ "<div style='font-size:12px;opacity:.92;margin-top:7px;'>Semua tab Excel pada LaporanRincianPembayaranSiswa dikonversi menjadi PortalLayout, grid paging 10 baris, grafik, rekap, dan popup Excel on-demand.</div>"
				+ "</div>");
	}

	/**
	 * Global filter utama mengikuti style DasboardPembayaranSekolah.
	 * Baris pertama dibuat sama: Tahun Ajaran, Sekolah, Item Biaya, Cari, Tampilkan Dasbor.
	 * Baris berikutnya tetap mempertahankan seluruh filter detail dari LaporanRincianPembayaranSiswa lama.
	 */
	private void renderPembayaranGlobalFilter(final Component parent) throws Exception {
		final Div filterContainer = new Div();
		filterContainer.setParent(parent);
		filterContainer.setStyle("margin-top:12px; padding:14px; background:#ffffff; border:1px solid #e8eef6; "
				+ "border-radius:16px; box-shadow:0 10px 26px rgba(15,23,42,0.04); box-sizing:border-box;");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(filterContainer);
		toolbar.setStyle("border:0; background:transparent; padding:0; display:flex; flex-wrap:wrap; align-items:center; gap:8px;");

		new MyLabelAgakKecil("Tahun Ajaran:").setParent(toolbar);
		final Combobox cmbTahun = Common.generateTahunAjaran(null);
		tahunAjaran = cmbTahun;
		cmbTahun.setCols(7);
		Common.selectComboItem(cmbTahun, dashboardTahunAjaran);
		cmbTahun.setParent(toolbar);

		new MyLabelAgakKecil("Sekolah:").setParent(toolbar);
		final Combobox cbSekolah = new Combobox();
		sekolah = cbSekolah;
		cbSekolah.setCols(8);
		cbSekolah.setReadonly(true);
		Yayasan selectedYayasan = SekolahUtil.getYayasan();
		Common.insertComboDanSemua(cbSekolah, new String[] { "nama", "jenisSekolah" }, "yayasan", Sekolah.class,
				"=" + Common.getBahasaConfig("sekolah") + "=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("yayasan", selectedYayasan));
		Common.selectComboItem(cbSekolah, sk);
		Sekolah selectedSekolah = SekolahUtil.getSekolah();
		if (selectedSekolah != null && selectedSekolah.getId() != null) {
			Common.selectComboItem(cbSekolah, selectedSekolah);
			cbSekolah.setDisabled(true);
		}
		cbSekolah.setParent(toolbar);

		new MyLabelAgakKecil("Item Biaya:").setParent(toolbar);
		final Textbox txtItemBiaya = new Textbox();
		itemBiayaKeyword = txtItemBiaya;
		txtItemBiaya.setCols(13);
		txtItemBiaya.setValue(dashboardItemBiayaKeyword == null ? "" : dashboardItemBiayaKeyword);
		txtItemBiaya.setTooltiptext("Ketik nama atau kode item biaya sekolah. Contoh: SPP, Daftar Ulang, Buku");
		txtItemBiaya.setParent(toolbar);

		new MyLabelAgakKecil("Cari:").setParent(toolbar);
		final Textbox txtKeyword = new Textbox();
		keywordGlobal = txtKeyword;
		txtKeyword.setCols(15);
		txtKeyword.setValue(dashboardKeyword == null ? "" : dashboardKeyword);
		txtKeyword.setTooltiptext("Cari nama siswa/calon siswa, NIS, sekolah, kelas, angkatan, item biaya, atau cara pembayaran");
		txtKeyword.setParent(toolbar);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Tampilkan Dasbor", "/img/svg/search.svg");
		refresh.setTooltiptext("Refresh dashboard rincian pembayaran berdasarkan filter global");
		refresh.setStyle("font-weight:bold; color:#ffffff; background:#2563eb; border-radius:10px; padding:6px 14px; margin-left:4px;");
		refresh.setParent(toolbar);

		processInfo = new Label(ais.common.Common.getBahasaConfig("Siap menampilkan dashboard."));
		processInfo.setStyle("font-size:11px; color:#64748b; margin-left:6px;");
		processInfo.setParent(toolbar);

		/* Filter lanjutan: mempertahankan semua filter yang tersedia di LaporanRincianPembayaranSiswa lama. */
		Toolbar toolbarLanjut = new Toolbar();
		toolbarLanjut.setParent(filterContainer);
		toolbarLanjut.setStyle("border:0; background:transparent; padding:10px 0 0 0; display:flex; flex-wrap:wrap; align-items:center; gap:8px;");

		new MyLabelAgakKecil("Yayasan:").setParent(toolbarLanjut);
		yayasan = new Combobox();
		yayasan.setCols(7);
		Common.initYayasanDanSekolahDanSemua(yayasan, cbSekolah, null, null);
		if (selectedYayasan != null && selectedYayasan.getId() != null) {
			Common.selectComboItem(yayasan, selectedYayasan);
		}
		yayasan.setParent(toolbarLanjut);
		/* Re-apply pilihan sekolah setelah init yayasan/sekolah agar pilihan user tidak hilang. */
		if (selectedSekolah != null && selectedSekolah.getId() != null) {
			Common.selectComboItem(cbSekolah, selectedSekolah);
		} else {
			Common.selectComboItem(cbSekolah, sk);
		}

		new MyLabelAgakKecil("Siswa/No.Reg:").setParent(toolbarLanjut);
		siswa = new Textbox();
		siswa.setCols(11);
		siswa.setTooltiptext("Filter khusus NIS/nama siswa/calon siswa");
		try {
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && tbmuser.getSiswa() != null) {
				siswa.setValue(tbmuser.getSiswa().getNomorInduk());
				siswa.setDisabled(true);
			}
			if (selectedSiswa != null) {
				siswa.setValue(selectedSiswa.getNomorInduk());
				siswa.setDisabled(true);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRincianPembayaranSiswaGrid.java:270");}
		siswa.setParent(toolbarLanjut);

		new MyLabelAgakKecil("Jenis Biaya:").setParent(toolbarLanjut);
		jenisBiayaSekolah = new Combobox();
		jenisBiayaSekolah.setCols(9);
		jenisBiayaSekolah.setReadonly(true);
		jenisBiayaSekolah.setParent(toolbarLanjut);

		new MyLabelAgakKecil("Cara Pembayaran:").setParent(toolbarLanjut);
		akunPembayaranSiswa = new Combobox();
		akunPembayaranSiswa.setCols(9);
		akunPembayaranSiswa.setReadonly(true);
		akunPembayaranSiswa.setParent(toolbarLanjut);

		new MyLabelAgakKecil("Nama Cara:").setParent(toolbarLanjut);
		namaAkunPembayaranSiswa = new Textbox();
		namaAkunPembayaranSiswa.setCols(11);
		namaAkunPembayaranSiswa.setTooltiptext("Nama cara pembayaran / akun pembayaran");
		namaAkunPembayaranSiswa.setParent(toolbarLanjut);

		new MyLabelAgakKecil("Mulai:").setParent(toolbarLanjut);
		Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
		cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - 3);
		mulai = new MyDatebox(cal.getTime());
		mulai.setCols(9);
		mulai.setReadonly(true);
		mulai.setParent(toolbarLanjut);

		new MyLabelAgakKecil("Sampai:").setParent(toolbarLanjut);
		sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		sampai.setCols(9);
		sampai.setReadonly(true);
		sampai.setParent(toolbarLanjut);

		initComboBulanTahunAngkatan(toolbarLanjut);

		new MyLabelAgakKecil("Kelas:").setParent(toolbarLanjut);
		kelas = new AmbilDataKelasSiswaBanbox();
		kelas.setCols(8);
		kelas.setParent(toolbarLanjut);

		new MyLabelAgakKecil("Asrama:").setParent(toolbarLanjut);
		asrama = new AmbilDataAsramaBanbox();
		asrama.setCols(8);
		asrama.setParent(toolbarLanjut);

		refreshDependentCombos();

		final EventListener refreshListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				updateDashboardBusy("Memproses filter dashboard rincian pembayaran...");
				dashboardTahunAjaran = normalizeDashboardTahunAjaran(cmbTahun.getValue());
				Object selectedSekolahObj = cbSekolah.getSelectedItem() == null ? null : cbSekolah.getSelectedItem().getValue();
				sk = selectedSekolahObj instanceof Sekolah && ((Sekolah) selectedSekolahObj).getId() != null ? (Sekolah) selectedSekolahObj : null;
				dashboardItemBiayaKeyword = txtItemBiaya.getValue() == null ? "" : txtItemBiaya.getValue().trim();
				dashboardKeyword = txtKeyword.getValue() == null ? "" : txtKeyword.getValue().trim();
				renderAllDashboards();
			}
		};

		refresh.addEventListener("onClick", refreshListener);
		txtKeyword.addEventListener("onOK", refreshListener);
		cmbTahun.addEventListener("onChange", refreshListener);
		cbSekolah.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				refreshDependentCombos();
				refreshListener.onEvent(event);
			}
		});
		txtItemBiaya.addEventListener("onOK", refreshListener);
		siswa.addEventListener("onOK", refreshListener);
		namaAkunPembayaranSiswa.addEventListener("onOK", refreshListener);
		bulan.addEventListener("onChange", refreshListener);
		tahun.addEventListener("onChange", refreshListener);
		angkatan.addEventListener("onChange", refreshListener);
		yayasan.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				refreshListener.onEvent(event);
			}
		});
		jenisBiayaSekolah.addEventListener("onChange", refreshListener);
		akunPembayaranSiswa.addEventListener("onChange", refreshListener);
	}

	private void initComboBulanTahunAngkatan(Toolbar toolbar) {
		Integer currTahun = new Integer(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));

		new MyLabelAgakKecil("Bulan:").setParent(toolbar);
		bulan = new Combobox();
		bulan.setReadonly(true);
		bulan.setCols(6);
		for (int i = 0; i < 12; i++) {
			Comboitem ci = new Comboitem(Common.BULAN[i]);
			ci.setValue(new Integer(i + 1));
			bulan.appendChild(ci);
		}
		Comboitem semuaBulan = new Comboitem("Semua");
		semuaBulan.setValue(null);
		bulan.appendChild(semuaBulan);
		bulan.setSelectedItem(semuaBulan);
		bulan.setParent(toolbar);

		new MyLabelAgakKecil("Tahun:").setParent(toolbar);
		tahun = new Combobox();
		tahun.setReadonly(true);
		tahun.setCols(6);
		for (int i = currTahun.intValue() - 10; i < currTahun.intValue() + 10; i++) {
			Comboitem ci = new Comboitem(String.valueOf(i));
			ci.setValue(new Integer(i));
			tahun.appendChild(ci);
		}
		Comboitem semuaTahun = new Comboitem("Semua");
		semuaTahun.setValue(null);
		tahun.appendChild(semuaTahun);
		tahun.setSelectedItem(semuaTahun);
		tahun.setParent(toolbar);

		new MyLabelAgakKecil("Angkatan:").setParent(toolbar);
		angkatan = new Combobox();
		angkatan.setReadonly(true);
		angkatan.setCols(6);
		for (int i = currTahun.intValue() - 20; i < currTahun.intValue() + 5; i++) {
			Comboitem ci = new Comboitem(String.valueOf(i));
			ci.setValue(new Integer(i));
			angkatan.appendChild(ci);
		}
		Comboitem semuaAngkatan = new Comboitem("Semua");
		semuaAngkatan.setValue(null);
		angkatan.appendChild(semuaAngkatan);
		angkatan.setSelectedItem(semuaAngkatan);
		angkatan.setParent(toolbar);
	}

	private void refreshDependentCombos() {
		try {
			Sekolah s = (Sekolah) selectedValue(sekolah);
			Common.insertComboDanSemua(jenisBiayaSekolah, new String[] { "kode", "nama", "periode" }, "sekolah",
					JenisBiayaSekolah.class,
					Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), s == null || s.getId() == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("sekolah", s)),
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
			Common.selectComboItem(jenisBiayaSekolah, null);

			Common.insertComboDanSemua(akunPembayaranSiswa, new String[] { "nama", "akun", "bank" }, "sekolah",
					AkunPembayaranSiswa.class,
					Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), s == null || s.getId() == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("sekolah", s)),
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
			Common.selectComboItem(akunPembayaranSiswa, null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rincian Pembayaran Siswa Grid", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void renderAllDashboards() {
		try {
			Common.clear(body);
			tampilkanLoadingDashboardRincian("Menyiapkan dashboard rincian pembayaran...", 5);
			updateDashboardBusy("Mengambil ringkasan pembayaran...", 12);
			renderOverview(body);
			ais.ui.util.DashboardJurnalPembayaranUtil.renderJurnalSiswaPanel(body, "Ringkasan Jurnal Pembayaran Siswa",
					"Menunjukkan akun pembayaran dan akun item biaya dari laporan rincian pembayaran siswa. Ringkasan ini membantu membaca arah jurnal tanpa membuka file Excel lebih dulu.");

			MyPortallayout layout = new MyPortallayout();
			layout.setWidth("100%");
			layout.setMaximizedMode("whole");
			layout.setStyle("background:transparent; padding:0; margin:0;");
			layout.setParent(body);

			updateDashboardBusy("Memproses Matrix Per Item Biaya...", 24);
			renderPerItemBiayaMatrix(layout);
			updateDashboardBusy("Memproses Matrix Per Virtual Account...", 36);
			renderPerVirtualAccountMatrix(layout);
			updateDashboardBusy("Memproses Per Item Detail...", 48);
			renderPerItemDetail(layout);
			updateDashboardBusy("Memproses Per Item Rinci...", 60);
			renderPerItemRinci(layout);
			updateDashboardBusy("Memproses Tagihan Item Detail...", 70);
			renderTagihanItemDetail(layout);
			updateDashboardBusy("Memproses Tagihan Per Siswa...", 80);
			renderTagihanPerSiswa(layout);
			updateDashboardBusy("Memproses Laporan Penerimaan...", 88);
			renderLaporanPenerimaan(layout);
			updateDashboardBusy("Memproses dashboard tambahan...", 95);
			renderDashboardTambahan(layout);
			updateDashboardBusy("Selesai memproses dashboard rincian pembayaran.", 100);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rincian Pembayaran Siswa Grid", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		} finally {
			sembunyikanLoadingDashboardRincian();
			try { ais.action.report.helper.LoadingReportUtil.clearBusy(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRincianPembayaranSiswaGrid.java:462");}
		}
	}

	private void renderOverview(Component parent) {
		try {
			Session session = HibernateUtil.currentSession();
			String sql = "SELECT COUNT(DISTINCT b.id) AS transaksi, COUNT(*) AS baris, "
					+ "COALESCE(SUM(a.nominal),0) AS realisasi, "
					+ "COUNT(DISTINCT COALESCE(CAST(c.id AS TEXT),'C'||CAST(cs.id AS TEXT))) AS peserta, "
					+ "COUNT(DISTINCT d.id) AS item "
					+ baseFromPembayaran() + buildWherePembayaran(true);
			List rows = queryMaps(session, sql);
			Map m = rows == null || rows.isEmpty() ? null : (Map) rows.get(0);

			String sqlTagihan = "SELECT COALESCE(SUM(t.nominal),0) AS tagihan, COALESCE(SUM(COALESCE(a.nominal,0)),0) AS bayar, "
					+ "COALESCE(SUM(COALESCE(t.nominal,0)-COALESCE(a.nominal,0)),0) AS sisa "
					+ baseFromTagihan() + buildWhereTagihan();
			List rowsTagihan = queryMaps(session, sqlTagihan);
			Map mt = rowsTagihan == null || rowsTagihan.isEmpty() ? null : (Map) rowsTagihan.get(0);

			Div wrap = new Div();
			wrap.setParent(parent);
			wrap.setStyle("border-radius:18px; padding:18px; color:#ffffff; background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); box-shadow:0 14px 30px rgba(15,23,42,.16); box-sizing:border-box;");
			appendHtml(wrap, "<div style='font-size:12px; letter-spacing:.12em; opacity:.85;'>PAYMENT ANALYTICS OVERVIEW</div>"
					+ "<div style='font-size:24px; font-weight:900; margin-top:4px;'>Ringkasan Rincian Pembayaran</div>"
					+ "<div style='font-size:12px; opacity:.90; margin-top:6px;'>Filter aktif: " + html(filterSummary()) + "</div>");

			Div cards = new Div();
			cards.setParent(wrap);
			cards.setStyle("display:flex; gap:10px; flex-wrap:wrap; margin-top:14px;");
			createOverviewMetric(cards, "Transaksi", formatInt(getDouble(m, "transaksi")), buildPembayaranDetailSql(""));
			createOverviewMetric(cards, "Baris Detail", formatInt(getDouble(m, "baris")), buildPembayaranDetailSql(""));
			createOverviewMetric(cards, "Realisasi", money(getDouble(m, "realisasi")), buildPembayaranDetailSql(""));
			createOverviewMetric(cards, "Peserta", formatInt(getDouble(m, "peserta")), buildPembayaranDetailSql(""));
			createOverviewMetric(cards, "Item Biaya", formatInt(getDouble(m, "item")), buildPembayaranDetailSql(""));
			createOverviewMetric(cards, "Total Tagihan", money(getDouble(mt, "tagihan")), buildTagihanDetailSql(""));
			createOverviewMetric(cards, "Dibayar", money(getDouble(mt, "bayar")), buildTagihanDetailSql(" AND COALESCE(a.nominal,0) > 0.1 "));
			createOverviewMetric(cards, "Sisa", money(getDouble(mt, "sisa")), buildTagihanDetailSql(" AND (COALESCE(t.nominal,0)-COALESCE(a.nominal,0)) > 0.1 "));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rincian Pembayaran Siswa Grid", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void createOverviewMetric(Component parent, final String title, String value, final String detailSql) {
		Div card = new Div();
		card.setParent(parent);
		card.setStyle("min-width:145px; padding:12px 14px; border-radius:14px; background:rgba(255,255,255,.16); border:1px solid rgba(255,255,255,.22);");
		A a = new A(value);
		a.setStyle("display:block; font-size:21px; font-weight:900; color:#ffffff; text-decoration:none; cursor:pointer;");
		a.setTooltiptext("Klik untuk melihat rincian " + title);
		a.setParent(card);
		a.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				openDetailPopup("Rincian " + title, detailSql);
			}
		});
		appendHtml(card, "<div style='font-size:11px; opacity:.88;'>" + html(title) + "</div>");
	}

	private String metricHtml(String title, String value) {
		return "<div style='min-width:145px; padding:12px 14px; border-radius:14px; background:rgba(255,255,255,.16); border:1px solid rgba(255,255,255,.22);'>"
				+ "<div style='font-size:21px; font-weight:900;'>" + html(value) + "</div>"
				+ "<div style='font-size:11px; opacity:.88;'>" + html(title) + "</div></div>";
	}

	private void renderPerItemBiayaMatrix(MyPortallayout layout) throws Exception {
		List itemBiayas = loadMatrixItems();
		if (itemBiayas.size() > MAX_DYNAMIC_ITEM_COLUMNS) {
			itemBiayas = itemBiayas.subList(0, MAX_DYNAMIC_ITEM_COLUMNS);
		}
		String[] headers = buildMatrixHeaders(itemBiayas, false);
		String[] keys = buildMatrixKeys(itemBiayas, false);
		boolean[] numeric = buildMatrixNumeric(itemBiayas, false);
		String sql = buildPerItemMatrixSql(itemBiayas);
		renderGridPanel(layout, "Per Item Biaya", "Matrix pembayaran per siswa/calon siswa dengan kolom dinamis per item biaya seperti Excel lama.", sql, headers, keys, numeric, true, "per_item_biaya_matrix");
	}

	private void renderPerVirtualAccountMatrix(MyPortallayout layout) throws Exception {
		List itemBiayas = loadMatrixItems();
		if (itemBiayas.size() > MAX_DYNAMIC_ITEM_COLUMNS) {
			itemBiayas = itemBiayas.subList(0, MAX_DYNAMIC_ITEM_COLUMNS);
		}
		String[] headers = buildMatrixHeaders(itemBiayas, true);
		String[] keys = buildMatrixKeys(itemBiayas, true);
		boolean[] numeric = buildMatrixNumeric(itemBiayas, true);
		String sql = buildPerVaMatrixSql(itemBiayas);
		renderGridPanel(layout, "Per Virtual Account", "Matrix pembayaran per virtual account/transaksi, siswa, kelas, item biaya, total, dan cara pembayaran.", sql, headers, keys, numeric, true, "per_virtual_account_matrix");
	}

	private void renderPerItemDetail(MyPortallayout layout) {
		String sql = "SELECT d.kode AS kode_item, d.nama AS nama_item, "
				+ pesertaKodeExpr() + " AS nis, " + pesertaNamaExpr() + " AS nama, "
				+ "MAX(CASE WHEN b1.nama IS NULL THEN '-' ELSE b1.nama END) AS kelas, "
				+ "COALESCE(SUM(a.nominal),0) AS nilai, " + caraExpr() + " AS cara "
				+ baseFromPembayaran() + buildWherePembayaran(true)
				+ " GROUP BY d.id, d.kode, d.nama, c.id, cs.id, " + pesertaKodeExpr() + ", " + pesertaNamaExpr() + ", " + caraExpr()
				+ " ORDER BY d.nama, " + pesertaKodeExpr();
		renderGridPanel(layout, "Per Item Detail", "Rincian total pembayaran per item, peserta, kelas, dan cara pembayaran.", sql,
				new String[] { "Kode Item", "Nama Item", "NIS", "Nama", "Kelas", "Nilai", "Cara Pembayaran" },
				new String[] { "kode_item", "nama_item", "nis", "nama", "kelas", "nilai", "cara" },
				new boolean[] { false, false, false, false, false, true, false }, true, "per_item_detail");
	}

	private void renderPerItemRinci(MyPortallayout layout) {
		String sql = "SELECT d.kode AS kode_item, d.nama AS nama_item, COALESCE(p.tahunajaran,'') AS tahunajaran, "
				+ pesertaKodeExpr() + " AS nis, " + pesertaNamaExpr() + " AS nama, "
				+ "CASE WHEN b1.nama IS NULL THEN '-' ELSE b1.nama END AS kelas, "
				+ "t.tahunbulan, t.bayarke, b.tanggal, a.nominal AS nilai, " + caraExpr() + " AS cara "
				+ baseFromPembayaran() + buildWherePembayaran(true)
				+ " ORDER BY b.tanggal DESC, d.nama, " + pesertaKodeExpr();
		renderGridPanel(layout, "Per Item Rinci", "Rincian pembayaran per baris transaksi item, termasuk tahun ajaran, periode, bayar ke, tanggal, dan cara pembayaran.", sql,
				new String[] { "Kode Item", "Nama Item", "TA", "NIS", "Nama", "Kelas", "Periode", "Ke", "Tanggal Bayar", "Nilai", "Cara Pembayaran" },
				new String[] { "kode_item", "nama_item", "tahunajaran", "nis", "nama", "kelas", "tahunbulan", "bayarke", "tanggal", "nilai", "cara" },
				new boolean[] { false, false, false, false, false, false, false, false, false, true, false }, false, "per_item_rinci");
	}

	private void renderTagihanItemDetail(MyPortallayout layout) {
		String porsi = "(COALESCE(b.daritabungan,0) / NULLIF(COUNT(*) OVER (PARTITION BY b.id),0))";
		String sql = "SELECT d.kode AS kode_item, d.nama AS nama_item, t.tahunbulan, t.bayarke, "
				+ pesertaKodeExpr() + " AS nis, " + pesertaNamaExpr() + " AS nama, "
				+ "CASE WHEN b1.nama IS NULL THEN '-' ELSE b1.nama END AS kelas, "
				+ "COALESCE(t.nominal,0) AS total_tagihan, "
				+ "(COALESCE(a.nominal,0)-COALESCE(" + porsi + ",0)) AS total_dibayar, "
				+ "COALESCE(" + porsi + ",0) AS porsi_tabungan, "
				+ "(COALESCE(t.nominal,0)-COALESCE(a.nominal,0)) AS sisa, "
				+ caraExpr() + " AS cara, b.tanggal "
				+ baseFromTagihan() + buildWhereTagihan()
				+ " ORDER BY b.tanggal DESC NULLS LAST, d.nama, " + pesertaKodeExpr() + ", t.tahunbulan";
		renderGridPanel(layout, "Tagihan Item Detail", "Rincian tagihan, nilai dibayar, porsi tabungan, sisa, cara, dan waktu pembayaran.", sql,
				new String[] { "Kode Item", "Nama Item", "Periode", "Ke", "NIS", "Nama", "Kelas", "Tagihan", "Dibayar", "Tabungan", "Sisa", "Cara", "Tanggal" },
				new String[] { "kode_item", "nama_item", "tahunbulan", "bayarke", "nis", "nama", "kelas", "total_tagihan", "total_dibayar", "porsi_tabungan", "sisa", "cara", "tanggal" },
				new boolean[] { false, false, false, false, false, false, false, true, true, true, true, false, false }, false, "tagihan_item_detail");
	}

	private void renderTagihanPerSiswa(MyPortallayout layout) {
		String sql = "SELECT MAX(" + pesertaKodeExpr() + ") AS nis, MAX(" + pesertaNamaExpr() + ") AS nama, "
				+ "MAX(CASE WHEN b1.nama IS NULL THEN '-' ELSE b1.nama END) AS kelas, "
				+ "COUNT(t.id) AS jumlah_tagihan, COALESCE(SUM(t.nominal),0) AS total_tagihan, "
				+ "COALESCE(SUM(CASE WHEN a.nominal IS NULL THEN 0 ELSE a.nominal END),0) AS dibayar, "
				+ "COALESCE(SUM(t.nominal),0)-COALESCE(SUM(CASE WHEN a.nominal IS NULL THEN 0 ELSE a.nominal END),0) AS belum_dibayar "
				+ baseFromTagihan() + buildWhereTagihan()
				+ " GROUP BY (CASE WHEN c.id IS NULL THEN cs.id ELSE c.id END) "
				+ " ORDER BY MAX(" + pesertaKodeExpr() + "), MAX(" + pesertaNamaExpr() + ")";
		renderGridPanel(layout, "Tagihan Per Siswa", "Rekap total tagihan, dibayar, dan belum dibayar per siswa/calon siswa.", sql,
				new String[] { "NIS", "Nama", "Kelas", "Jml Tagihan", "Total Tagihan", "Dibayar", "Belum Dibayar" },
				new String[] { "nis", "nama", "kelas", "jumlah_tagihan", "total_tagihan", "dibayar", "belum_dibayar" },
				new boolean[] { false, false, false, true, true, true, true }, true, "tagihan_per_siswa");
	}

	private void renderLaporanPenerimaan(MyPortallayout layout) {
		String sql = "SELECT d.kode AS kode_item, d.nama AS nama_item, COALESCE(SUM(a.nominal),0) AS nilai "
				+ baseFromPembayaran() + buildWherePembayaran(true)
				+ " GROUP BY d.id, d.kode, d.nama ORDER BY d.nama";
		renderGridPanel(layout, "Laporan Penerimaan", "Rekap penerimaan pembayaran per item biaya seperti tab Laporan Penerimaan pada laporan lama.", sql,
				new String[] { "Kode Item", "Nama Item", "Nilai" },
				new String[] { "kode_item", "nama_item", "nilai" },
				new boolean[] { false, false, true }, true, "laporan_penerimaan");
	}

	private void renderDashboardTambahan(MyPortallayout layout) {
		String sqlCara = "SELECT " + caraExpr() + " AS cara, COUNT(DISTINCT b.id) AS transaksi, COALESCE(SUM(a.nominal),0) AS nilai "
				+ baseFromPembayaran() + buildWherePembayaran(true)
				+ " GROUP BY " + caraExpr() + " ORDER BY COALESCE(SUM(a.nominal),0) DESC";
		renderGridPanel(layout, "Dashboard Tambahan - Cara Pembayaran", "Rekap realisasi pembayaran per bank/akun/cara pembayaran.", sqlCara,
				new String[] { "Cara Pembayaran", "Transaksi", "Nilai" },
				new String[] { "cara", "transaksi", "nilai" },
				new boolean[] { false, true, true }, true, "tambahan_cara_pembayaran");

		String sqlTanggal = "SELECT DATE(b.tanggal) AS tanggal, COUNT(DISTINCT b.id) AS transaksi, COUNT(*) AS baris, COALESCE(SUM(a.nominal),0) AS nilai "
				+ baseFromPembayaran() + buildWherePembayaran(true)
				+ " GROUP BY DATE(b.tanggal) ORDER BY DATE(b.tanggal) DESC";
		renderGridPanel(layout, "Dashboard Tambahan - Tren Harian", "Rekap transaksi dan penerimaan per tanggal bayar.", sqlTanggal,
				new String[] { "Tanggal", "Transaksi", "Baris", "Nilai" },
				new String[] { "tanggal", "transaksi", "baris", "nilai" },
				new boolean[] { false, true, true, true }, true, "tambahan_tren_harian");

		String sqlKelas = "SELECT CASE WHEN b1.nama IS NULL THEN '-' ELSE b1.nama END AS kelas, COUNT(DISTINCT b.id) AS transaksi, COALESCE(SUM(a.nominal),0) AS nilai "
				+ baseFromPembayaran() + buildWherePembayaran(true)
				+ " GROUP BY CASE WHEN b1.nama IS NULL THEN '-' ELSE b1.nama END ORDER BY COALESCE(SUM(a.nominal),0) DESC";
		renderGridPanel(layout, "Dashboard Tambahan - Per Kelas", "Rekap penerimaan berdasarkan kelas siswa pada tagihan.", sqlKelas,
				new String[] { "Kelas", "Transaksi", "Nilai" },
				new String[] { "kelas", "transaksi", "nilai" },
				new boolean[] { false, true, true }, true, "tambahan_per_kelas");
	}

	private List loadMatrixItems() throws Exception {
		String sql = "SELECT d.id AS id, d.nama AS nama " + baseFromPembayaran() + buildWherePembayaran(true)
				+ " GROUP BY d.id, d.nama ORDER BY d.nama";
		return queryMaps(HibernateUtil.currentSession(), sql);
	}

	private String[] buildMatrixHeaders(List itemBiayas, boolean va) {
		int fixed = va ? 6 : 3;
		String[] h = new String[fixed + itemBiayas.size() + 2];
		int i = 0;
		if (va) {
			h[i++] = "Kode Trx";
			h[i++] = "Nama Trx";
			h[i++] = "Waktu Byr";
		}
		h[i++] = "NIS";
		h[i++] = "Nama";
		h[i++] = "Kelas";
		for (int x = 0; x < itemBiayas.size(); x++) {
			Map m = (Map) itemBiayas.get(x);
			h[i++] = stringValue(m.get("nama"));
		}
		h[i++] = "Total";
		h[i++] = "Cara Pembayaran";
		return h;
	}

	private String[] buildMatrixKeys(List itemBiayas, boolean va) {
		int fixed = va ? 6 : 3;
		String[] k = new String[fixed + itemBiayas.size() + 2];
		int i = 0;
		if (va) {
			k[i++] = "kode_trx";
			k[i++] = "nama_trx";
			k[i++] = "waktu_byr";
		}
		k[i++] = "nis";
		k[i++] = "nama";
		k[i++] = "kelas";
		for (int x = 0; x < itemBiayas.size(); x++) {
			Map m = (Map) itemBiayas.get(x);
			k[i++] = itemAlias(m.get("id"));
		}
		k[i++] = "total";
		k[i++] = "cara";
		return k;
	}

	private boolean[] buildMatrixNumeric(List itemBiayas, boolean va) {
		int fixed = va ? 6 : 3;
		boolean[] n = new boolean[fixed + itemBiayas.size() + 2];
		for (int i = 0; i < n.length; i++) n[i] = false;
		int start = fixed;
		for (int i = start; i < start + itemBiayas.size() + 1; i++) n[i] = true;
		return n;
	}

	private String buildPerItemMatrixSql(List itemBiayas) {
		String sql = "SELECT " + pesertaKodeExpr() + " AS nis, " + pesertaNamaExpr() + " AS nama, "
				+ "MAX(CASE WHEN b1.nama IS NULL THEN '-' ELSE b1.nama END) AS kelas, ";
		for (int i = 0; i < itemBiayas.size(); i++) {
			Map m = (Map) itemBiayas.get(i);
			sql += "COALESCE(SUM(CASE WHEN a.item_biaya_id = " + numberValue(m.get("id")) + " THEN a.nominal ELSE 0 END),0) AS " + itemAlias(m.get("id")) + ", ";
		}
		sql += "COALESCE(SUM(a.nominal),0) AS total, " + caraExpr() + " AS cara "
				+ baseFromPembayaran() + buildWherePembayaran(true)
				+ " GROUP BY c.id, cs.id, " + pesertaKodeExpr() + ", " + pesertaNamaExpr() + ", " + caraExpr()
				+ " ORDER BY " + pesertaKodeExpr() + ", " + pesertaNamaExpr();
		return sql;
	}

	private String buildPerVaMatrixSql(List itemBiayas) {
		String sql = "SELECT MAX(" + kodeVaExpr() + ") AS kode_trx, MAX(" + namaVaExpr() + ") AS nama_trx, "
				+ "MAX(" + waktuVaExpr() + ") AS waktu_byr, "
				+ pesertaKodeExpr() + " AS nis, " + pesertaNamaExpr() + " AS nama, "
				+ "MAX(CASE WHEN b1.nama IS NULL THEN '-' ELSE b1.nama END) AS kelas, ";
		for (int i = 0; i < itemBiayas.size(); i++) {
			Map m = (Map) itemBiayas.get(i);
			sql += "COALESCE(SUM(CASE WHEN a.item_biaya_id = " + numberValue(m.get("id")) + " THEN a.nominal ELSE 0 END),0) AS " + itemAlias(m.get("id")) + ", ";
		}
		sql += "COALESCE(SUM(a.nominal),0) AS total, MAX(" + caraExpr() + ") AS cara "
				+ baseFromPembayaran() + buildWherePembayaran(true)
				+ " GROUP BY " + vaGroupExpr() + ", c.id, cs.id, " + pesertaKodeExpr() + ", " + pesertaNamaExpr()
				+ " ORDER BY MAX(" + waktuVaExpr() + ") DESC NULLS LAST, " + pesertaKodeExpr();
		return sql;
	}

	private void renderGridPanel(MyPortallayout layout, String title, String subtitle, final String sql, final String[] headers,
			final String[] keys, final boolean[] numeric, boolean chart, String code) {
		MyPortalchildren pc = new MyPortalchildren();
		pc.setParent(layout);
		pc.setWidth(Common.isMobile() || headers.length > 8 ? "100%" : "50%");
		pc.setStyle("padding:6px; box-sizing:border-box;");

		Panel panel = new Panel();
		panel.setParent(pc);
		panel.setTitle(title);
		panel.setBorder("none");
		panel.setCollapsible(true);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setStyle("border-radius:16px; border:1px solid #e5e7eb; background:#ffffff; box-shadow:0 10px 24px rgba(15,23,42,.05); overflow:hidden;");

		Panelchildren children = new Panelchildren();
		children.setParent(panel);
		children.setStyle("padding:12px; background:#ffffff;");

		Div intro = new Div();
		intro.setParent(children);
		intro.setStyle("display:flex; justify-content:space-between; align-items:center; gap:10px; flex-wrap:wrap; margin-bottom:8px;");
		appendHtml(intro, "<div><div style='font-size:13px; font-weight:900; color:#0f172a;'>" + html(title) + "</div>"
				+ "<div style='font-size:11px; color:#64748b; margin-top:3px;'>" + html(subtitle) + "</div></div>");
		MyToolbarbuttonConfig excel = new MyToolbarbuttonConfig("Download Excel", "/img/excel.png");
		excel.setStyle("font-weight:bold; border-radius:10px; padding:5px 10px;");
		excel.setParent(intro);
		final String excelTitle = title;
		final String excelSql = sql;
		final String[] excelHeaders = headers;
		final String[] excelKeys = keys;
		final boolean[] excelNumeric = numeric;
		excel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				openExcelPopup(excelTitle, excelSql, excelHeaders, excelKeys, excelNumeric);
			}
		});

		try {
			List rows = queryMaps(HibernateUtil.currentSession(), sql);
			appendHtml(children, "<div style='font-size:11px; color:#64748b; margin-bottom:6px;'>Jumlah baris: <b>" + rows.size() + "</b>. Grid paging per " + GRID_PAGE_SIZE + " baris.</div>");
			if (chart) renderChart(children, rows, keys, numeric);
			renderGrid(children, rows, headers, keys, numeric, title, code);
		} catch (Exception e) {
			appendHtml(children, "<div style='padding:10px; background:#fef2f2; color:#991b1b; border-radius:12px; font-size:12px;'>Gagal memuat panel " + html(title) + ": " + html(e.getMessage()) + "</div>");
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void renderChart(Component parent, List rowsData, String[] keys, boolean[] numeric) {
		if (rowsData == null || rowsData.isEmpty()) return;
		int labelIndex = -1;
		for (int i = 0; i < numeric.length; i++) {
			if (!numeric[i]) { labelIndex = i; break; }
		}
		int valueIndex = -1;
		for (int i = numeric.length - 1; i >= 0; i--) {
			if (numeric[i]) { valueIndex = i; break; }
		}
		if (labelIndex < 0 || valueIndex < 0) return;
		Div chartBox = new Div();
		chartBox.setParent(parent);
		chartBox.setWidth("100%");
		chartBox.setStyle("margin:8px 0 12px 0; padding:12px; border-radius:14px; background:#f8fafc; border:1px solid #e5e7eb; box-sizing:border-box;");
		double max = 0.0;
		int limit = Math.min(MAX_CHART_ITEMS, rowsData.size());
		for (int i = 0; i < limit; i++) {
			Map m = (Map) rowsData.get(i);
			double v = getDouble(m, keys[valueIndex]);
			if (v > max) max = v;
		}
		if (max <= 0.0) max = 1.0;
		for (int i = 0; i < limit; i++) {
			Map m = (Map) rowsData.get(i);
			String label = stringValue(m.get(keys[labelIndex]));
			if (label == null || label.length() == 0) label = "-";
			double val = getDouble(m, keys[valueIndex]);
			renderCssSingleBar(chartBox, label, val, max);
		}
	}

	private void renderCssSingleBar(Component parent, String label, double value, double max) {
		int pct = cssPercent(value, max);
		appendHtml(parent, "<div style='display:flex; align-items:center; gap:8px; margin:5px 0;'>"
				+ "<div style='width:170px; font-size:11px; color:#334155; font-weight:700; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;'>" + html(label) + "</div>"
				+ "<div style='flex:1; height:10px; border-radius:999px; background:#e5e7eb; overflow:hidden;'>"
				+ "<div style='width:" + pct + "%; height:10px; background:#2563eb; border-radius:999px;'></div></div>"
				+ "<div style='width:110px; text-align:right; font-size:11px; color:#0f172a; font-weight:800;'>" + html(money(value)) + "</div></div>");
	}

	private int cssPercent(double value, double max) {
		if (max <= 0.0 || value <= 0.0) return 1;
		int pct = (int) Math.round((value * 100.0) / max);
		return pct < 1 ? 1 : (pct > 100 ? 100 : pct);
	}

	private int calculateGridMinWidth(String[] headers, boolean[] numeric) {
		int width = 60;
		if (headers == null) {
			return 900;
		}
		for (int i = 0; i < headers.length; i++) {
			width += parsePxWidth(columnWidth(headers[i], numeric != null && i < numeric.length && numeric[i]));
		}
		return width < 980 ? 980 : width;
	}

	private String columnWidth(String header, boolean numeric) {
		String h = header == null ? "" : header.toLowerCase();
		if (numeric) return "105px";
		if (h.indexOf("nama") >= 0) return "190px";
		if (h.indexOf("nis") >= 0 || h.indexOf("nipd") >= 0 || h.indexOf("kode") >= 0 || h.indexOf("va") >= 0) return "120px";
		if (h.indexOf("kelas") >= 0 || h.indexOf("kel") >= 0) return "80px";
		if (h.indexOf("tanggal") >= 0 || h.indexOf("waktu") >= 0) return "120px";
		if (h.indexOf("cara") >= 0) return "140px";
		if (h.length() <= 4) return "90px";
		return "115px";
	}

	private int parsePxWidth(String width) {
		try {
			if (width == null) return 100;
			return Integer.parseInt(width.replace("px", "").trim());
		} catch (Exception e) {
			return 100;
		}
	}

	private void renderGrid(Component parent, List rowsData, String[] headers, String[] keys, boolean[] numeric, final String panelTitle, final String panelCode) {
		Div gridWrap = new Div();
		gridWrap.setParent(parent);
		gridWrap.setWidth("100%");
		gridWrap.setStyle("overflow-x:auto; overflow-y:hidden; border:1px solid #e5e7eb; border-radius:12px; background:#ffffff; box-sizing:border-box;");

		Grid grid = new Grid();
		grid.setParent(gridWrap);
		grid.setWidth(calculateGridMinWidth(headers, numeric) + "px");
		grid.setSclass("dgrid");
		grid.setMold("paging");
		grid.setPageSize(GRID_PAGE_SIZE);
		try { grid.getPagingChild().setMold("os"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRincianPembayaranSiswaGrid.java:878");}
		grid.setStyle("border:0; border-radius:12px; overflow:hidden; background:#ffffff; table-layout:fixed; min-height:150px;");

		Columns cols = new Columns();
		cols.setParent(grid);
		Column no = new MyColumnConfig("No.");
		no.setWidth("48px");
		no.setAlign("right");
		no.setParent(cols);
		for (int i = 0; i < headers.length; i++) {
			Column c = new MyColumnConfig(headers[i]);
			if (numeric[i]) c.setAlign("right");
			c.setWidth(columnWidth(headers[i], numeric[i]));
			c.setTooltiptext(headers[i]);
			c.setParent(cols);
		}
		Rows rows = new Rows();
		rows.setParent(grid);
		if (rowsData == null || rowsData.isEmpty()) {
			Row r = new Row();
			r.setParent(rows);
			r.appendChild(new Label(""));
			Label empty = new Label(ais.common.Common.getBahasaConfig("Tidak ada data berdasarkan filter saat ini."));
			empty.setStyle("font-size:12px; color:#64748b; font-weight:bold;");
			r.appendChild(empty);
			return;
		}
		double[] totals = new double[keys.length];
		for (int i = 0; i < rowsData.size(); i++) {
			final Map m = (Map) rowsData.get(i);
			Row r = new Row();
			r.setParent(rows);
			r.setStyle((i % 2 == 0 ? "background:#ffffff;" : "background:#f8fafc;") + " min-height:30px;");
			Label idx = new Label(String.valueOf(i + 1));
			idx.setStyle("display:block; font-size:11px; text-align:right; padding:4px 6px; white-space:nowrap;");
			r.appendChild(idx);
			for (int k = 0; k < keys.length; k++) {
				Object v = m.get(keys[k]);
				if (numeric[k]) {
					totals[k] += getDouble(m, keys[k]);
					final String clickedKey = keys[k];
					A a = new A(moneyOrInt(v));
					a.setStyle("display:block; font-size:11px; text-align:right; font-weight:bold; color:#1d4ed8; text-decoration:none; cursor:pointer; padding:4px 6px; white-space:nowrap;");
					a.setTooltiptext("Klik untuk melihat rincian angka ini");
					a.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							openDetailPopup("Rincian " + panelTitle + " - " + clickedKey, buildCellDetailSql(panelCode, clickedKey, m));
						}
					});
					r.appendChild(a);
				} else {
					Label l = new Label(displayValue(v));
					l.setStyle("display:block; font-size:11px; padding:4px 6px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;");
					l.setTooltiptext(displayValue(v));
					r.appendChild(l);
				}
			}
		}
		appendGridTotalRow(grid, keys, numeric, totals, panelTitle, panelCode);
	}

	private void appendGridTotalRow(Grid grid, final String[] keys, boolean[] numeric, double[] totals, final String panelTitle, final String panelCode) {
		try {
			Foot foot = new Foot();
			foot.setParent(grid);
			Footer fNo = new Footer();
			fNo.setParent(foot);
			fNo.appendChild(new Label(""));
			for (int i = 0; i < keys.length; i++) {
				Footer ft = new Footer();
				ft.setParent(foot);
				ft.setAlign(numeric[i] ? "right" : "left");
				if (i == 0) {
					Label l = new Label(ais.common.Common.getBahasaConfig("TOTAL"));
					l.setStyle("font-size:11px; font-weight:bold;");
					ft.appendChild(l);
				} else if (numeric[i]) {
					final String clickedKey = keys[i];
					A a = new A(money(totals[i]));
					a.setStyle("font-size:11px; font-weight:bold; color:#1d4ed8; text-decoration:none; cursor:pointer;");
					a.setTooltiptext("Klik untuk melihat rincian total kolom ini");
					a.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							openDetailPopup("Rincian Total " + panelTitle + " - " + clickedKey, buildCellDetailSql(panelCode, clickedKey, null));
						}
					});
					ft.appendChild(a);
				} else {
					ft.appendChild(new Label(""));
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRincianPembayaranSiswaGrid.java:971");}
	}

	private String buildCellDetailSql(String panelCode, String clickedKey, Map row) {
		String extra = buildExtraWhereForCell(panelCode, clickedKey, row);
		if (panelCode != null && panelCode.indexOf("tagihan") >= 0) {
			if ("dibayar".equals(clickedKey) || "total_dibayar".equals(clickedKey)) {
				extra += " AND COALESCE(a.nominal,0) > 0.1 ";
			} else if ("belum_dibayar".equals(clickedKey) || "sisa".equals(clickedKey)) {
				extra += " AND (COALESCE(t.nominal,0)-COALESCE(a.nominal,0)) > 0.1 ";
			}
			return buildTagihanDetailSql(extra);
		}
		if ("jumlah_tagihan".equals(clickedKey) || "total_tagihan".equals(clickedKey)) {
			return buildTagihanDetailSql(extra);
		}
		return buildPembayaranDetailSql(extra);
	}

	private String buildExtraWhereForCell(String panelCode, String clickedKey, Map row) {
		String w = "";
		if (clickedKey != null && clickedKey.startsWith("item_")) {
			w += " AND a.item_biaya_id = " + numberValue(clickedKey) + " ";
		}
		if (row == null) {
			return w;
		}
		String nis = stringValue(row.get("nis"));
		if (nis.length() > 0) w += " AND " + pesertaKodeExpr() + " = '" + sql(nis) + "' ";
		String kodeItem = stringValue(row.get("kode_item"));
		if (kodeItem.length() > 0) w += " AND d.kode = '" + sql(kodeItem) + "' ";
		String itemName = stringValue(row.get("nama_item"));
		if (kodeItem.length() == 0 && itemName.length() > 0) w += " AND d.nama = '" + sql(itemName) + "' ";
		String cara = stringValue(row.get("cara"));
		if (cara.length() > 0 && !"-".equals(cara)) w += " AND " + caraExpr() + " = '" + sql(cara) + "' ";
		String kelasVal = stringValue(row.get("kelas"));
		if (kelasVal.length() > 0 && !"-".equals(kelasVal) && (panelCode == null || panelCode.indexOf("kelas") >= 0)) w += " AND b1.nama = '" + sql(kelasVal) + "' ";
		Object periode = row.get("tahunbulan");
		if (periode != null && String.valueOf(periode).trim().length() > 0) w += " AND t.tahunbulan = " + numberValue(periode) + " ";
		Object bayarKe = row.get("bayarke");
		if (bayarKe != null && String.valueOf(bayarKe).trim().length() > 0) w += " AND t.bayarke = " + numberValue(bayarKe) + " ";
		Object tgl = row.get("tanggal");
		if (tgl != null && ("tambahan_tren_harian".equals(panelCode) || "per_item_rinci".equals(panelCode) || "tagihan_item_detail".equals(panelCode))) {
			w += " AND DATE(b.tanggal) = DATE('" + sql(dateFromObject(tgl)) + "') ";
		}
		String kodeVa = stringValue(row.get("kode_trx"));
		if (kodeVa.length() > 0) w += " AND " + kodeVaExpr() + " = '" + sql(kodeVa) + "' ";
		return w;
	}

	private String buildPembayaranDetailSql(String extraWhere) {
		return "SELECT b.id AS id, b.tanggal, "
				+ pesertaKodeExpr() + " AS nis, " + pesertaNamaExpr() + " AS nama, "
				+ "CASE WHEN b1.nama IS NULL THEN '-' ELSE b1.nama END AS kelas, "
				+ "d.kode AS kode_item, d.nama AS nama_item, a.nominal AS nilai, "
				+ "t.tahunbulan, t.bayarke, " + caraExpr() + " AS cara, "
				+ kodeVaExpr() + " AS kode_va, " + namaVaExpr() + " AS nama_va "
				+ baseFromPembayaran() + buildWherePembayaran(true) + (extraWhere == null ? "" : extraWhere)
				+ " ORDER BY b.tanggal DESC, " + pesertaKodeExpr() + ", d.nama";
	}

	private String buildTagihanDetailSql(String extraWhere) {
		return "SELECT t.id AS id, " + pesertaKodeExpr() + " AS nis, " + pesertaNamaExpr() + " AS nama, "
				+ "CASE WHEN b1.nama IS NULL THEN '-' ELSE b1.nama END AS kelas, "
				+ "d.kode AS kode_item, d.nama AS nama_item, t.tahunbulan, t.bayarke, "
				+ "COALESCE(t.nominal,0) AS total_tagihan, COALESCE(a.nominal,0) AS dibayar, "
				+ "(COALESCE(t.nominal,0)-COALESCE(a.nominal,0)) AS belum_dibayar, "
				+ "b.tanggal, " + caraExpr() + " AS cara "
				+ baseFromTagihan() + buildWhereTagihan() + (extraWhere == null ? "" : extraWhere)
				+ " ORDER BY " + pesertaKodeExpr() + ", d.nama, t.tahunbulan";
	}

	private String buildPesertaOverviewSql() {
		return "SELECT " + pesertaKodeExpr() + " AS nis, " + pesertaNamaExpr() + " AS nama, "
				+ "MAX(CASE WHEN b1.nama IS NULL THEN '-' ELSE b1.nama END) AS kelas, "
				+ "COUNT(DISTINCT b.id) AS transaksi, COUNT(*) AS baris, COALESCE(SUM(a.nominal),0) AS nilai "
				+ baseFromPembayaran() + buildWherePembayaran(true)
				+ " GROUP BY c.id, cs.id, " + pesertaKodeExpr() + ", " + pesertaNamaExpr()
				+ " ORDER BY COALESCE(SUM(a.nominal),0) DESC";
	}

	private String buildItemOverviewSql() {
		return "SELECT d.kode AS kode_item, d.nama AS nama_item, COUNT(*) AS baris, COALESCE(SUM(a.nominal),0) AS nilai "
				+ baseFromPembayaran() + buildWherePembayaran(true)
				+ " GROUP BY d.id, d.kode, d.nama ORDER BY d.nama";
	}

	private void openDetailPopup(String title, String sql) {
		try {
			final String[] headers = new String[] { "ID", "Tanggal", "NIS", "Nama", "Kelas", "Kode Item", "Nama Item", "Periode", "Ke", "Nilai/Tagihan", "Dibayar", "Sisa", "Cara", "Kode VA" };
			final String[] keys = new String[] { "id", "tanggal", "nis", "nama", "kelas", "kode_item", "nama_item", "tahunbulan", "bayarke", "nilai", "dibayar", "belum_dibayar", "cara", "kode_va" };
			final boolean[] numeric = new boolean[] { false, false, false, false, false, false, false, false, false, true, true, true, false, false };
			List rows = normalizeDetailRows(queryMaps(HibernateUtil.currentSession(), sql));
			Window win = new Window(title, "normal", true);
			win.setWidth(Common.isMobile() ? "98%" : "92%");
			win.setHeight(Common.isMobile() ? "92%" : "86%");
			win.setSizable(true);
			win.setMaximizable(true);
			win.setParent(this);
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(win);
			North north = new North();
			north.setParent(borderlayout);
			north.setHeight("52px");
			north.setBorder("none");
			Toolbar tb = new Toolbar();
			tb.setParent(north);
			tb.setStyle("padding:8px; background:#f8fafc; display:flex; gap:8px; align-items:center;");
			Label info = new Label("Jumlah data: " + rows.size() + " baris. Angka yang diklik ditampilkan sebagai data rinci.");
			info.setStyle("font-size:12px; font-weight:bold; color:#334155;");
			info.setParent(tb);
			MyToolbarbuttonConfig excel = new MyToolbarbuttonConfig("Download Excel", "/img/excel.png");
			excel.setParent(tb);
			final String detailExcelTitle = title;
			final String detailExcelSql = sql;
			final String[] detailExcelHeaders = headers;
			final String[] detailExcelKeys = keys;
			final boolean[] detailExcelNumeric = numeric;
			excel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					openExcelPopup(detailExcelTitle, detailExcelSql, detailExcelHeaders, detailExcelKeys, detailExcelNumeric);
				}
			});
			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);
			renderGrid(center, rows, headers, keys, numeric, title, "detail_popup");
			win.doModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rincian Pembayaran Siswa Grid", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private List normalizeDetailRows(List rows) {
		List out = new ArrayList();
		if (rows == null) return out;
		for (int i = 0; i < rows.size(); i++) {
			Map src = (Map) rows.get(i);
			java.util.HashMap m = new java.util.HashMap();
			m.put("id", src.get("id") != null ? src.get("id") : (src.get("pembayaran_id") != null ? src.get("pembayaran_id") : src.get("tagihan_id")));
			m.put("tanggal", src.get("tanggal"));
			m.put("nis", src.get("nis"));
			m.put("nama", src.get("nama"));
			m.put("kelas", src.get("kelas"));
			m.put("kode_item", src.get("kode_item"));
			m.put("nama_item", src.get("nama_item"));
			m.put("tahunbulan", src.get("tahunbulan"));
			m.put("bayarke", src.get("bayarke"));
			Object nilai = src.get("nilai") != null ? src.get("nilai") : src.get("total_tagihan");
			m.put("nilai", nilai);
			m.put("dibayar", src.get("dibayar"));
			m.put("belum_dibayar", src.get("belum_dibayar") != null ? src.get("belum_dibayar") : src.get("sisa"));
			m.put("cara", src.get("cara"));
			m.put("kode_va", src.get("kode_va"));
			out.add(m);
		}
		return out;
	}

	private String dateFromObject(Object o) {
		try {
			if (o instanceof Date) return Common.databaseDateFormat.get().format((Date) o);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRincianPembayaranSiswaGrid.java:1134");}
		String s = String.valueOf(o == null ? "" : o);
		if (s.length() >= 10) return s.substring(0, 10);
		return date(new Date());
	}

	private void openExcelPopup(String title, String sql, String[] headers, String[] keys, boolean[] numeric) {
		try {
			Window win = new Window("Preview Excel - " + title, "normal", true);
			win.setWidth(Common.isMobile() ? "98%" : "90%");
			win.setHeight(Common.isMobile() ? "92%" : "86%");
			win.setSizable(true);
			win.setMaximizable(true);
			win.setParent(this);

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(win);
			North north = new North();
			north.setParent(borderlayout);
			north.setHeight("45px");
			north.setBorder("none");
			Toolbar tb = new Toolbar();
			tb.setParent(north);
			tb.setStyle("padding:8px; background:#f8fafc;");
			Label info = new Label("Preview Excel: " + title + ". Gunakan tombol Excel/Download pada popup worksheet untuk menyimpan file.");
			info.setStyle("font-size:12px; font-weight:bold; color:#334155;");
			info.setParent(tb);

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);
			Label label = new Label(ais.common.Common.getBahasaConfig("Memproses data Excel..."));
			List datas = buildExcelDatas(title, sql, headers, keys, numeric);
			label.setAttribute("datas", datas);
			SaptoUtil.displayWorksheet(label, "data_umum", center, Math.max(40, datas.size() + 8));
			win.doModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rincian Pembayaran Siswa Grid", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private List buildExcelDatas(String title, String sql, String[] headers, String[] keys, boolean[] numeric) throws Exception {
		List datas = new ArrayList();
		ArrayList rowTitle = new ArrayList();
		rowTitle.add(title);
		datas.add(rowTitle);
		ArrayList header = new ArrayList();
		header.add("No.");
		for (int i = 0; i < headers.length; i++) header.add(headers[i]);
		datas.add(header);
		List rows = queryMaps(HibernateUtil.currentSession(), sql);
		double[] totals = new double[keys.length];
		for (int r = 0; r < rows.size(); r++) {
			Map m = (Map) rows.get(r);
			ArrayList dataRow = new ArrayList();
			dataRow.add(new Integer(r + 1));
			for (int i = 0; i < keys.length; i++) {
				Object v = m.get(keys[i]);
				if (numeric[i]) totals[i] += getDouble(m, keys[i]);
				dataRow.add(v == null ? "" : v);
			}
			datas.add(dataRow);
		}
		ArrayList total = new ArrayList();
		total.add("");
		for (int i = 0; i < keys.length; i++) {
			if (i == 0) total.add("TOTAL");
			else if (numeric[i]) total.add(Common.numberFormat.get().format(totals[i]));
			else total.add("");
		}
		datas.add(total);
		return datas;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List queryMaps(Session session, String sql) {
		return session.createSQLQuery(sql).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();
	}

	private String baseFromPembayaran() {
		return " FROM sekolah.pembayaran_siswa_detail a "
				+ " INNER JOIN sekolah.pembayaran_siswa b ON (a.pembayaran_siswa_id = b.id) "
				+ " LEFT JOIN virtual_account_bank v ON (v.id = b.virtual_account_bank) "
				+ " LEFT JOIN bni_request v1 ON (v1.id = b.bni_request_id) "
				+ " LEFT JOIN sekolah.siswa c ON (b.siswa_id = c.id) "
				+ " LEFT JOIN sekolah.calon_siswa cs ON (b.calon_siswa_id = cs.id) "
				+ " INNER JOIN sekolah.item_biaya_sekolah d ON (d.id = a.item_biaya_id) "
				+ " LEFT JOIN bank_host b2 ON (b2.id = b.bank_host_id) "
				+ " LEFT JOIN sekolah.akun_pembayaran_siswa b3 ON (b3.id = b.akun_pembayaran_siswa_id) "
				+ " LEFT JOIN sekolah.deposit_siswa e ON (e.pembayaran_siswa_id = b.id) "
				+ " INNER JOIN sekolah.tagihan t ON (t.pembayaran_siswa_detail_id = a.id) "
				+ " INNER JOIN sekolah.pengaturan_biaya p ON (t.pengaturan_biaya = p.id) "
				+ " LEFT JOIN sekolah.kelas b1 ON (b1.id = t.kelas_siswa_id) "
				+ " LEFT JOIN sekolah.sekolah sks ON (sks.id = c.sekolah_id) "
				+ " LEFT JOIN sekolah.sekolah skc ON (skc.id = cs.sekolah_id) ";
	}

	private String baseFromTagihan() {
		return " FROM sekolah.tagihan t "
				+ " INNER JOIN sekolah.pengaturan_biaya p ON (t.pengaturan_biaya = p.id AND (p.aktif OR p.aktif IS NULL)) "
				+ " LEFT JOIN sekolah.pembayaran_siswa_detail a ON (t.pembayaran_siswa_detail_id = a.id) "
				+ " LEFT JOIN sekolah.pembayaran_siswa b ON (a.pembayaran_siswa_id = b.id) "
				+ " LEFT JOIN virtual_account_bank v ON (v.id = b.virtual_account_bank) "
				+ " LEFT JOIN bni_request v1 ON (v1.id = b.bni_request_id) "
				+ " LEFT JOIN sekolah.siswa c ON (t.siswa_id = c.id) "
				+ " LEFT JOIN sekolah.calon_siswa cs ON (t.calon_siswa_id = cs.id) "
				+ " INNER JOIN sekolah.item_biaya_sekolah d ON (d.id = t.item_biaya_id) "
				+ " LEFT JOIN bank_host b2 ON (b2.id = b.bank_host_id) "
				+ " LEFT JOIN sekolah.akun_pembayaran_siswa b3 ON (b3.id = b.akun_pembayaran_siswa_id) "
				+ " LEFT JOIN sekolah.deposit_siswa e ON (e.pembayaran_siswa_id = b.id) "
				+ " LEFT JOIN sekolah.kelas b1 ON (b1.id = t.kelas_siswa_id) "
				+ " LEFT JOIN sekolah.sekolah sks ON (sks.id = c.sekolah_id) "
				+ " LEFT JOIN sekolah.sekolah skc ON (skc.id = cs.sekolah_id) ";
	}

	private String buildWherePembayaran(boolean withDate) {
		String where = " WHERE b.nominal > 0.1 ";
		if (withDate) {
			where += " AND DATE(b.tanggal) BETWEEN DATE('" + date(mulai.getValue()) + "') AND DATE('" + date(sampai.getValue()) + "') ";
		}
		where += commonFilters("b", "b", "c", "cs", "d", "p", "b1", "b2", "b3", "t");
		where += parentFilter("b");
		return where;
	}

	private String buildWhereTagihan() {
		String where = " WHERE (c.aktif OR c.aktif IS NULL) AND COALESCE(t.nominal,0) > 0.1 ";
		where += " AND (b.tanggal IS NULL OR DATE(b.tanggal) BETWEEN DATE('" + date(mulai.getValue()) + "') AND DATE('" + date(sampai.getValue()) + "')) ";
		where += commonFilters("t", "b", "c", "cs", "d", "p", "b1", "b2", "b3", "t");
		where += parentFilter("t");
		return where;
	}

	private String commonFilters(String periodAlias, String payAlias, String siswaAlias, String calonAlias, String itemAlias,
			String pengaturanAlias, String kelasAlias, String bankAlias, String akunAlias, String tagihanAlias) {
		String where = "";
		Object y = selectedValue(yayasan);
		if (y instanceof Yayasan && ((Yayasan) y).getId() != null) {
			Long id = ((Yayasan) y).getId();
			where += " AND (" + siswaAlias + ".yayasan_id = " + id + " OR " + calonAlias + ".yayasan_id = " + id + ") ";
		}
		Object s = selectedValue(sekolah);
		if (s instanceof Sekolah && ((Sekolah) s).getId() != null) {
			Long id = ((Sekolah) s).getId();
			where += " AND (" + siswaAlias + ".sekolah_id = " + id + " OR " + calonAlias + ".sekolah_id = " + id + ") ";
		}
		String ta = selectedString(tahunAjaran);
		if (ta != null && ta.trim().length() > 0) {
			where += " AND " + pengaturanAlias + ".tahunajaran = '" + sql(ta) + "' ";
		}
		Object jenis = selectedValue(jenisBiayaSekolah);
		if (jenis instanceof JenisBiayaSekolah && ((JenisBiayaSekolah) jenis).getId() != null) {
			where += " AND " + payAlias + ".jenis_biaya_id = " + ((JenisBiayaSekolah) jenis).getId() + " ";
		}
		Object akun = selectedValue(akunPembayaranSiswa);
		if (akun instanceof AkunPembayaranSiswa && ((AkunPembayaranSiswa) akun).getId() != null) {
			where += " AND " + payAlias + ".akun_pembayaran_siswa_id = " + ((AkunPembayaranSiswa) akun).getId() + " ";
		}
		String siswaCari = value(siswa);
		if (siswaCari.length() > 0) {
			where += " AND (" + siswaAlias + ".nomor_induk ILIKE '%" + sql(siswaCari) + "%' OR "
					+ siswaAlias + ".nama_siswa ILIKE '%" + sql(siswaCari) + "%' OR "
					+ calonAlias + ".nomor_induk ILIKE '%" + sql(siswaCari) + "%' OR "
					+ calonAlias + ".nama_siswa ILIKE '%" + sql(siswaCari) + "%') ";
		}
		String itemCari = value(itemBiayaKeyword);
		if (itemCari.length() > 0) {
			where += " AND (" + itemAlias + ".kode ILIKE '%" + sql(itemCari) + "%' OR "
					+ itemAlias + ".nama ILIKE '%" + sql(itemCari) + "%') ";
		}
		String namaAkun = value(namaAkunPembayaranSiswa);
		if (namaAkun.length() > 0) {
			where += " AND " + akunAlias + ".nama_pembayaran ILIKE '%" + sql(namaAkun) + "%' ";
		}
		String global = value(keywordGlobal);
		if (global.length() > 0) {
			where += " AND (" + siswaAlias + ".nomor_induk ILIKE '%" + sql(global) + "%' OR "
					+ siswaAlias + ".nama_siswa ILIKE '%" + sql(global) + "%' OR "
					+ calonAlias + ".nomor_induk ILIKE '%" + sql(global) + "%' OR "
					+ calonAlias + ".nama_siswa ILIKE '%" + sql(global) + "%' OR "
					+ itemAlias + ".kode ILIKE '%" + sql(global) + "%' OR "
					+ itemAlias + ".nama ILIKE '%" + sql(global) + "%' OR "
					+ kelasAlias + ".nama ILIKE '%" + sql(global) + "%' OR "
					+ bankAlias + ".nama ILIKE '%" + sql(global) + "%' OR "
					+ akunAlias + ".nama_pembayaran ILIKE '%" + sql(global) + "%' OR "
					+ "sks.nama ILIKE '%" + sql(global) + "%' OR skc.nama ILIKE '%" + sql(global) + "%') ";
		}
		String caraCari = value(namaAkunPembayaranSiswa);
		if (caraCari.length() > 0) {
			where += " AND (" + bankAlias + ".nama ILIKE '%" + sql(caraCari) + "%' OR "
					+ akunAlias + ".nama_pembayaran ILIKE '%" + sql(caraCari) + "%') ";
		}
		KelasSiswa k = kelas == null ? null : (KelasSiswa) kelas.getAttribute("kelas");
		if (k != null && k.getId() != null) {
			where += " AND " + tagihanAlias + ".kelas_siswa_id = " + k.getId() + " ";
		}
		AsramaSiswa a = asrama == null ? null : (AsramaSiswa) asrama.getAttribute("asrama");
		if (a != null && a.getId() != null) {
			where += " AND " + siswaAlias + ".asrama_id = " + a.getId() + " ";
		}
		Integer thn = selectedInteger(tahun);
		if (thn != null) where += " AND " + periodAlias + ".tahun = " + thn + " ";
		Integer bln = selectedInteger(bulan);
		if (bln != null) where += " AND " + periodAlias + ".bulan = " + bln + " ";
		Integer akt = selectedInteger(angkatan);
		if (akt != null) where += " AND (" + siswaAlias + ".tahun_masuk = " + akt + " OR " + calonAlias + ".tahun_masuk = " + akt + ") ";
		return where;
	}

	private String parentFilter(String alias) {
		try {
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && tbmuser.getSiswa() != null && tbmuser.getSiswa().getId() != null) {
				return " AND " + alias + ".siswa_id = " + tbmuser.getSiswa().getId() + " ";
			}
			if (selectedSiswa != null && selectedSiswa.getId() != null) {
				return " AND " + alias + ".siswa_id = " + selectedSiswa.getId() + " ";
			}
			if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
				String ids = "";
				for (Object o : tbmuser.getOrangTua().ambilAnakSiswa()) {
					ids += ids.length() == 0 ? String.valueOf(o) : "," + String.valueOf(o);
				}
				return " AND " + alias + ".siswa_id IN (" + ids + ") ";
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRincianPembayaranSiswaGrid.java:1359");}
		return "";
	}

	private String pesertaKodeExpr() {
		return "(CASE WHEN c.nomor_induk IS NULL THEN cs.nomor_induk ELSE c.nomor_induk END)";
	}

	private String pesertaNamaExpr() {
		return "(CASE WHEN c.nama_siswa IS NULL THEN cs.nama_siswa ELSE c.nama_siswa END)";
	}

	private String caraExpr() {
		return "(CASE WHEN b2.nama IS NULL THEN COALESCE(b3.nama_pembayaran,'-') ELSE b2.nama END)";
	}

	private String kodeVaExpr() {
		return "(CASE WHEN v.kode IS NULL THEN v1.va ELSE v.kode END)";
	}

	private String namaVaExpr() {
		return "(CASE WHEN v.nama IS NULL THEN v1.session_id ELSE v.nama END)";
	}

	private String waktuVaExpr() {
		return "(CASE WHEN v.waktubayar IS NULL THEN v1.tanggal_dirubah ELSE v.waktubayar END)";
	}

	private String vaGroupExpr() {
		return "(CASE WHEN v.id IS NULL THEN 'BNI'||CAST(v1.id AS TEXT) ELSE 'VAB'||CAST(v.id AS TEXT) END)";
	}

	private Object selectedValue(Combobox cb) {
		try { return cb == null || cb.getSelectedItem() == null ? null : cb.getSelectedItem().getValue(); } catch (Exception e) { return null; }
	}

	private String selectedString(Combobox cb) {
		Object v = selectedValue(cb);
		String s = null;
		if (v != null) {
			s = String.valueOf(v);
		} else {
			try { s = cb == null ? null : cb.getValue(); } catch (Exception e) { s = null; }
		}
		return normalizeDashboardTahunAjaran(s);
	}

	private String normalizeDashboardTahunAjaran(String value) {
		if (value == null) return null;
		String s = value.trim();
		if (s.length() == 0 || "Semua".equalsIgnoreCase(s) || "=Semua=".equalsIgnoreCase(s)) return null;
		return s;
	}

	private Integer selectedInteger(Combobox cb) {
		Object v = selectedValue(cb);
		if (v instanceof Integer) return (Integer) v;
		try { return v == null ? null : new Integer(String.valueOf(v)); } catch (Exception e) { return null; }
	}

	private String filterSummary() {
		String ta = selectedString(tahunAjaran);
		String s = sekolah == null ? "" : sekolah.getValue();
		String item = value(itemBiayaKeyword);
		String kw = value(keywordGlobal);
		return "TA " + (ta == null || ta.length() == 0 ? "Semua" : ta)
				+ ", Sekolah " + (s == null || s.length() == 0 ? "Semua" : s)
				+ ", Tanggal " + date(mulai.getValue()) + " s.d. " + date(sampai.getValue())
				+ (item.length() == 0 ? "" : ", Item: " + item)
				+ (kw.length() == 0 ? "" : ", Cari: " + kw);
	}

	private void tampilkanLoadingDashboardRincian(String text, int progress) {
		try {
			sembunyikanLoadingDashboardRincian();
			dashboardProgress = normalizeProgress(progress);
			if (body == null) return;
			loadingDashboardHtml = new Html(buildLoadingHtml(text, dashboardProgress));
			loadingDashboardHtml.setParent(body);
			try { ais.action.report.helper.LoadingReportUtil.showBusyText(dashboardProgress + "% - " + text); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRincianPembayaranSiswaGrid.java:1438");}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRincianPembayaranSiswaGrid.java:1439");}
	}

	private void updateDashboardBusy(String text) {
		updateDashboardBusy(text, dashboardProgress);
	}

	private void updateDashboardBusy(String text, int progress) {
		try {
			dashboardProgress = normalizeProgress(progress);
			if (processInfo != null) processInfo.setValue(dashboardProgress + "% - " + text);
			if (loadingDashboardHtml != null) {
				loadingDashboardHtml.setContent(buildLoadingHtml(text, dashboardProgress));
				loadingDashboardHtml.invalidate();
			}
			try { ais.action.report.helper.LoadingReportUtil.showBusyText(dashboardProgress + "% - " + text); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRincianPembayaranSiswaGrid.java:1454");}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRincianPembayaranSiswaGrid.java:1455");}
	}


	private void sembunyikanLoadingDashboardRincian() {
		try {
			if (loadingDashboardHtml != null && loadingDashboardHtml.getParent() != null) {
				loadingDashboardHtml.detach();
			}
			loadingDashboardHtml = null;
			if (processInfo != null) {
				processInfo.setValue("Selesai memproses dashboard rincian pembayaran.");
			}
			try { ais.action.report.helper.LoadingReportUtil.clearBusy(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRincianPembayaranSiswaGrid.java:1468");}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRincianPembayaranSiswaGrid.java:1469");}
	}

	private String buildLoadingHtml(String text, int progress) {
		return "<div style='margin:0 0 12px 0; padding:14px; background:#ffffff; border:1px solid #e8eef6; border-radius:16px; box-shadow:0 10px 26px rgba(15,23,42,0.04); box-sizing:border-box;'>"
				+ "<div style='font-size:13px; font-weight:900; color:#0f172a; margin-bottom:8px;'><i class=\"fa fa-spinner fa-spin\"></i> " + html(text) + "</div>"
				+ "<div style='height:10px; background:#e5e7eb; border-radius:999px; overflow:hidden;'><div style='height:10px; width:" + progress + "%; background:#2563eb; border-radius:999px;'></div></div>"
				+ "<div style='font-size:11px; color:#64748b; margin-top:6px;'>Progress " + progress + "%</div></div>";
	}

	private int normalizeProgress(int progress) {
		return progress < 0 ? 0 : (progress > 100 ? 100 : progress);
	}

	private String value(Textbox tb) {
		return tb == null || tb.getValue() == null ? "" : tb.getValue().trim();
	}

	private String date(Date d) {
		try { return Common.databaseDateFormat.get().format(d == null ? new Date() : d); } catch (Exception e) { return "1900-01-01"; }
	}

	private String sql(String s) {
		return s == null ? "" : s.replace("'", "''");
	}

	private String html(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private void appendHtml(Component parent, String html) {
		Html h = new Html(html);
		h.setParent(parent);
	}

	private String stringValue(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private String displayValue(Object o) {
		if (o == null) return "";
		try {
			if (o instanceof Date) return Common.dateFormat5.get().format((Date) o);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRincianPembayaranSiswaGrid.java:1513");}
		return String.valueOf(o);
	}

	private double getDouble(Map m, String key) {
		if (m == null) return 0.0;
		Object v = m.get(key);
		if (v instanceof Number) return ((Number) v).doubleValue();
		try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0.0; }
	}

	private String moneyOrInt(Object v) {
		if (!(v instanceof Number)) return stringValue(v);
		double d = ((Number) v).doubleValue();
		if (Math.abs(d) < 10000 && Math.floor(d) == d) return formatInt(d);
		return money(d);
	}

	private String money(double d) {
		try { return Common.numberFormat.get().format(d); } catch (Exception e) { return String.valueOf(d); }
	}

	private String formatInt(double d) {
		try { return Common.numberFormat.get().format(new Long(Math.round(d))); } catch (Exception e) { return String.valueOf(Math.round(d)); }
	}

	private String numberValue(Object id) {
		if (id instanceof Number) return String.valueOf(((Number) id).longValue());
		String s = String.valueOf(id == null ? "0" : id);
		return s.replaceAll("[^0-9]", "").length() == 0 ? "0" : s.replaceAll("[^0-9]", "");
	}

	private String itemAlias(Object id) {
		return "item_" + numberValue(id);
	}
}
