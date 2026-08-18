package ais.action.master.obe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonDashboardHtmlHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Akreditasi;
import ais.database.model.CapaianJurusan;
import ais.database.model.DokumenAkreditasi;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.KelompokMatakuliah;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.database.model.obe.BahanKajian;
import ais.database.model.obe.IndikatorKinerja;
import ais.database.model.obe.ReferensiLulusan;
import ais.database.model.obe.CapaianLulusan;
import ais.database.model.obe.CapaianPembelajaranLulusan;
import ais.database.model.obe.ProfesiLulusan;
import ais.database.model.obe.ProfilLulusan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>PikobeAction — Pusat Penyusunan Kurikulum OBE dalam SATU halaman</h3>
 *
 * <p>Halaman ini menggabungkan seluruh komponen penyusunan kurikulum berbasis luaran
 * (OBE): Profesi Lulusan, Profil Lulusan, Capaian Pembelajaran Lulusan (CPL), Bahan
 * Kajian, Capaian Pembelajaran Mata Kuliah (CPMK), Mata Kuliah, hingga matriks
 * keterkaitannya — sehingga pengguna tidak perlu berpindah-pindah halaman.</p>
 *
 * <p><b>Susunan.</b> Sebuah penyaring (Fakultas &amp; Program Studi) di atas, lalu sebuah
 * {@code Tabbox} berisi: (1) tab "Ringkasan OBE" yang memvisualkan data lewat grafik
 * HTML/CSS modern (radar/jaring laba-laba, donat, batang, tren) — TANPA JFreeChart; dan
 * (2) satu tab untuk tiap komponen, masing-masing menampilkan data sebagai grid ZK,
 * lengkap dengan tombol "Unduh Excel" dan tombol "Tambah / Kelola" yang membuka halaman
 * CRUD OBE yang sudah ada dalam jendela melayang (reuse — entri tetap di halaman ini).</p>
 *
 * <p><b>Reuse.</b> Memakai {@link PikobeChartHelper} (grafik), {@link PikobeExportHelper}
 * (unduh Excel), {@link CommonDashboardHtmlHelper} (kartu/panel), serta halaman OBE
 * lama via {@link MyInclude}. Gaya Java 1.6/1.7.</p>
 */
public class PikobeAction extends MyWindow {

	private static final long serialVersionUID = 7711201926060619001L;

	private static final int TAB_RINGKASAN = 0;
	private static final int TAB_PEO = 1;
	private static final int TAB_PROFESI = 2;
	private static final int TAB_PROFIL = 3;
	private static final int TAB_CPL = 4;
	private static final int TAB_IK = 5;
	private static final int TAB_KERANGKA = 6;
	private static final int TAB_BK = 7;
	private static final int TAB_CPMK = 8;
	private static final int TAB_MK = 9;
	private static final int TAB_KELOMPOK = 10;
	private static final int TAB_M_CPL_PROFIL = 11;
	private static final int TAB_M_BK_MK = 12;
	private static final int TAB_M_MK_CPL = 13;
	private static final int TAB_PETA_CPL = 14;
	private static final int TAB_DOKUMEN = 15;
	private static final int TAB_ORGANISASI_MK = 16;

	private static final String[] JUDUL = { "Ringkasan OBE", "Tujuan / PEO", "Profesi Lulusan", "Profil Lulusan",
			"CPL", "Indikator Kinerja (IK/PI)", "Pemetaan Kerangka", "Bahan Kajian", "CPMK", "Mata Kuliah",
			"Kelompok MK", "Matriks CPL × Profil", "Matriks BK × MK", "Matriks MK × CPL", "Peta Pemenuhan CPL",
			"Dokumen Akreditasi", "Organisasi MK per Semester" };

	/** Batas kolom pada tab matriks agar tetap nyaman dibaca (Excel tetap lengkap). */
	private static final int MAKS_KOLOM = 20;

	private static final String ZUL_PEO = "/pages/master/capaian_jurusan.zul";
	private static final String ZUL_PROFESI = "/pages/master/obe/profesi_lulusan.zul";
	private static final String ZUL_PROFIL = "/pages/master/obe/profil_lulusan.zul";
	private static final String ZUL_CPL = "/pages/master/obe/capaian_lulusan.zul";
	private static final String ZUL_BK = "/pages/master/obe/bahan_kajian.zul";
	private static final String ZUL_CPMK = "/pages/master/obe/capaian_pembelajaran_lulusan.zul";
	private static final String ZUL_KERANGKA = "/pages/master/obe/referensi_lulusan.zul";
	private static final String ZUL_KELOMPOK = "/pages/master/kelompok_matakuliah.zul";
	private static final String ZUL_AKREDITASI = "/pages/master/akreditasi.zul";

	private Combobox cbFakultas;
	private Combobox cbJurusan;
	private Tabbox tabbox;
	private Div[] hosts;
	private boolean[] loaded;

	public PikobeAction() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public PikobeAction(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	// ════════════════════════════════════════════════════════════════════════
	// BANGUN UI
	// ════════════════════════════════════════════════════════════════════════

	private void init() throws Exception {
		setWidth("100%");
		setHeight("100%");
		setBorder("none");
		setClosable(false);
		setContentStyle("overflow:auto;");

		appendHtml(this, gaya());

		// — Penyaring —
		Div filter = new Div();
		filter.setSclass("pikobe-filter");
		filter.setParent(this);
		appendHtml(filter,
				"<div class='pikobe-judul'>Penyusunan Kurikulum OBE</div>"
						+ "<div class='pikobe-sub'>Isi dan pantau seluruh komponen OBE program studi dalam satu halaman.</div>");

		Hbox bar = new Hbox();
		bar.setAlign("center");
		bar.setSclass("pikobe-filter-bar");
		bar.setParent(filter);
		bar.appendChild(label("Fakultas"));
		cbFakultas = new Combobox();
		cbFakultas.setReadonly(true);
		cbFakultas.setSclass("pikobe-combo");
		bar.appendChild(cbFakultas);
		bar.appendChild(label("Program Studi"));
		cbJurusan = new Combobox();
		cbJurusan.setReadonly(true);
		cbJurusan.setSclass("pikobe-combo");
		bar.appendChild(cbJurusan);
		Common.initFakultasDanJurusanDanSemua(null, null, cbFakultas, cbJurusan);

		EventListener onFilter = new EventListener() {
			public void onEvent(Event e) throws Exception {
				resetLoaded();
				renderSelected();
			}
		};
		cbFakultas.addEventListener("onChange", onFilter);
		cbJurusan.addEventListener("onChange", onFilter);

		// — Tabbox —
		tabbox = new Tabbox();
		tabbox.setWidth("100%");
		tabbox.setHeight("100%");
		tabbox.setSclass("pikobe-tabs");
		tabbox.setParent(this);

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);
		Tabpanels panels = new Tabpanels();
		panels.setParent(tabbox);

		hosts = new Div[JUDUL.length];
		loaded = new boolean[JUDUL.length];
		for (int i = 0; i < JUDUL.length; i++) {
			Tab t = new Tab(JUDUL[i]);
			t.setParent(tabs);
			Tabpanel tp = new ais.ui.util.MyTabpanel();
			tp.setParent(panels);
			Div host = new Div();
			host.setWidth("100%");
			host.setSclass("pikobe-panelhost");
			host.setParent(tp);
			hosts[i] = host;
		}

		tabbox.addEventListener("onSelect", new EventListener() {
			public void onEvent(Event e) throws Exception {
				renderSelected();
			}
		});

		renderSelected();
	}

	private Label label(String t) {
		Label l = new Label(t);
		l.setSclass("pikobe-lbl");
		return l;
	}

	private void resetLoaded() {
		for (int i = 0; i < loaded.length; i++) {
			loaded[i] = false;
		}
	}

	private void renderSelected() {
		int idx = tabbox == null ? 0 : tabbox.getSelectedIndex();
		if (idx < 0) {
			idx = 0;
		}
		renderTab(idx);
	}

	private void renderTab(int idx) {
		if (hosts == null || idx < 0 || idx >= hosts.length) {
			return;
		}
		if (loaded[idx]) {
			return;
		}
		final Div host = hosts[idx];
		Common.clear(host);
		try {
			Jurusan jur = jurusanTerpilih();
			if (jur == null) {
				appendHtml(host, CommonDashboardHtmlHelper.emptyState(
						"Silakan pilih Fakultas dan Program Studi terlebih dahulu untuk menampilkan data."));
				return; // sengaja tidak ditandai 'loaded' agar dirender ulang setelah prodi dipilih
			}
			switch (idx) {
			case TAB_RINGKASAN:
				renderRingkasan(host, jur);
				break;
			case TAB_PEO:
				renderTujuanPeo(idx, host, jur);
				break;
			case TAB_PROFESI:
				renderProfesi(idx, host, jur);
				break;
			case TAB_PROFIL:
				renderProfil(idx, host, jur);
				break;
			case TAB_CPL:
				renderCpl(idx, host, jur);
				break;
			case TAB_IK:
				renderIndikatorKinerja(idx, host, jur);
				break;
			case TAB_KERANGKA:
				renderMatriksKerangka(idx, host, jur);
				break;
			case TAB_BK:
				renderBahanKajian(idx, host, jur);
				break;
			case TAB_CPMK:
				renderCpmk(idx, host, jur);
				break;
			case TAB_MK:
				renderMataKuliah(idx, host, jur);
				break;
			case TAB_KELOMPOK:
				renderKelompokMk(idx, host, jur);
				break;
			case TAB_M_CPL_PROFIL:
				renderMatriksCplProfil(idx, host, jur);
				break;
			case TAB_M_BK_MK:
				renderMatriksBkMk(idx, host, jur);
				break;
			case TAB_M_MK_CPL:
				renderMatriksMkCpl(idx, host, jur);
				break;
			case TAB_PETA_CPL:
				renderPetaPemenuhanCpl(idx, host, jur);
				break;
			case TAB_DOKUMEN:
				renderDokumenAkreditasi(idx, host, jur);
				break;
			case TAB_ORGANISASI_MK:
				renderOrganisasiMK(idx, host, jur);
				break;
			default:
				break;
			}
			loaded[idx] = true;
		} catch (Exception e) {
			Common.clear(host);
			appendHtml(host, CommonDashboardHtmlHelper.errorState(JUDUL[idx], e));
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private Jurusan jurusanTerpilih() {
		try {
			if (cbJurusan == null || cbJurusan.getSelectedItem() == null) {
				return null;
			}
			Object v = cbJurusan.getSelectedItem().getValue();
			return (v instanceof Jurusan) ? (Jurusan) v : null;
		} catch (Exception e) {
			return null;
		}
	}

	// ════════════════════════════════════════════════════════════════════════
	// TAB KOMPONEN (grid + unduh excel + kelola) — REUSE
	// ════════════════════════════════════════════════════════════════════════

	private void buatTabKomponen(final int idx, Div host, String judul, String deskripsi, final String[] headers,
			final List<String[]> baris, final String namaFile, final String judulKelola, final String zulKelola) {
		appendHtml(host, panelHead(judul, deskripsi));

		Toolbar tb = new Toolbar();
		tb.setStyle("border:none; background:transparent; padding:2px 0 8px 0;");
		tb.setParent(host);

		if (zulKelola != null) {
			MyToolbarbuttonConfig btnKelola = new MyToolbarbuttonConfig("Tambah / Kelola", "/img/new.gif");
			btnKelola.setTooltiptext("Buka halaman input " + judul + " (tetap di halaman ini)");
			final String jk = judulKelola;
			final String zk = zulKelola;
			btnKelola.addEventListener("onClick", new EventListener() {
				public void onEvent(Event e) throws Exception {
					bukaKelola(jk, zk);
				}
			});
			btnKelola.setParent(tb);
		}

		MyToolbarbuttonConfig btnUnduh = new MyToolbarbuttonConfig("Unduh Excel", "/img/save.gif");
		btnUnduh.setTooltiptext("Unduh data tabel ini sebagai berkas Excel (.xlsx)");
		btnUnduh.addEventListener("onClick", new EventListener() {
			public void onEvent(Event e) throws Exception {
				PikobeExportHelper.unduhExcel(namaFile, namaFile, headers, baris);
			}
		});
		btnUnduh.setParent(tb);

		MyToolbarbuttonConfig btnSegar = new MyToolbarbuttonConfig("Muat Ulang", "/img/Button-Refresh-icon.png");
		btnSegar.setTooltiptext("Ambil ulang data terbaru");
		btnSegar.addEventListener("onClick", new EventListener() {
			public void onEvent(Event e) throws Exception {
				loaded[idx] = false;
				renderTab(idx);
			}
		});
		btnSegar.setParent(tb);

		Label jml = new Label("Jumlah: " + baris.size() + " baris");
		jml.setStyle("font-size:11px; color:#64748b; margin-left:8px;");
		tb.appendChild(jml);

		buatGrid(host, headers, baris);
	}

	private void buatGrid(Div host, String[] headers, List<String[]> baris) {
		Grid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setSclass("dgrid");
		grid.setParent(host);

		Columns cols = new Columns();
		cols.setSizable(true);
		cols.setParent(grid);
		MyColumnConfig no = new MyColumnConfig("No");
		no.setWidth("48px");
		no.setParent(cols);
		for (int i = 0; i < headers.length; i++) {
			MyColumnConfig c = new MyColumnConfig(headers[i]);
			c.setParent(cols);
		}

		Rows rows = new Rows();
		rows.setParent(grid);
		if (baris.isEmpty()) {
			Row r = new Row();
			r.setParent(rows);
			Label l = new Label(ais.common.Common.getBahasaConfig("Belum ada data untuk program studi ini."));
			l.setStyle("color:#94a3b8; font-style:italic;");
			l.setParent(r);
			return;
		}
		for (int i = 0; i < baris.size(); i++) {
			String[] data = baris.get(i);
			Row r = new Row();
			r.setParent(rows);
			new Label(String.valueOf(i + 1)).setParent(r);
			for (int c = 0; c < data.length; c++) {
				Label cell = new Label(data[c] == null ? "" : data[c]);
				cell.setStyle("font-size:12px;");
				cell.setParent(r);
			}
		}
	}

	/** Buka halaman CRUD OBE yang sudah ada di dalam jendela melayang (reuse penuh). */
	private void bukaKelola(String judul, String zulPath) {
		try {
			final MyWindow w = new MyWindow(judul, "normal", true);
			w.setWidth("94%");
			w.setHeight("90%");
			w.setSclass("pikobe-modal");
			w.setParent(this);
			MyInclude inc = new MyInclude(zulPath);
			inc.setWidth("100%");
			inc.setHeight("100%");
			inc.setParent(w);
			w.addEventListener("onClose", new EventListener() {
				public void onEvent(Event e) throws Exception {
					resetLoaded();
					renderSelected();
				}
			});
			w.doHighlighted();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	// ── tiap komponen ───────────────────────────────────────────────────────

	private void renderProfesi(int idx, Div host, Jurusan jur) {
		List list = list(ProfesiLulusan.class, jur, true);
		List<String[]> baris = new ArrayList<String[]>();
		for (int i = 0; i < list.size(); i++) {
			ProfesiLulusan x = (ProfesiLulusan) list.get(i);
			baris.add(new String[] { s(x.getKode()), s(x.getNama()), s(x.getKeterangan()), yaTidak(x.getAktif()) });
		}
		buatTabKomponen(idx, host, "Profesi Lulusan",
				"Daftar pekerjaan atau peran yang ditargetkan untuk lulusan program studi.",
				new String[] { "Kode", "Nama Profesi", "Keterangan", "Aktif" }, baris,
				"OBE_Profesi_" + kodeJur(jur), "Profesi Lulusan", "/pages/master/obe/profesi_lulusan.zul");
	}

	private void renderProfil(int idx, Div host, Jurusan jur) {
		List list = list(ProfilLulusan.class, jur, true);
		List<String[]> baris = new ArrayList<String[]>();
		for (int i = 0; i < list.size(); i++) {
			ProfilLulusan x = (ProfilLulusan) list.get(i);
			String profesi = "";
			try {
				profesi = x.getProfesiLulusan() == null ? "" : s(x.getProfesiLulusan().getNama());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/obe/PikobeAction.java:468");
			}
			baris.add(new String[] { s(x.getKode()), s(x.getNama()), profesi, yaTidak(x.getAktif()) });
		}
		buatTabKomponen(idx, host, "Profil Lulusan",
				"Gambaran kemampuan utama yang diharapkan dimiliki lulusan setelah menyelesaikan studi.",
				new String[] { "Kode", "Deskripsi Profil", "Profesi Terkait", "Aktif" }, baris,
				"OBE_ProfilLulusan_" + kodeJur(jur), "Profil Lulusan", "/pages/master/obe/profil_lulusan.zul");
	}

	private void renderCpl(int idx, Div host, Jurusan jur) {
		List list = list(CapaianLulusan.class, jur, true);
		List<String[]> baris = new ArrayList<String[]>();
		for (int i = 0; i < list.size(); i++) {
			CapaianLulusan x = (CapaianLulusan) list.get(i);
			baris.add(new String[] { s(x.getKode()), s(x.getNama()), s(x.getKategori()), yaTidak(x.getAktif()) });
		}
		buatTabKomponen(idx, host, "Capaian Pembelajaran Lulusan (CPL)",
				"Kemampuan yang wajib dikuasai lulusan — menjadi acuan utama seluruh kurikulum.",
				new String[] { "Kode", "Nama CPL", "Kelompok", "Aktif" }, baris,
				"OBE_CPL_" + kodeJur(jur), "Capaian Pembelajaran Lulusan (CPL)",
				"/pages/master/obe/capaian_lulusan.zul");
	}

	private void renderBahanKajian(int idx, Div host, Jurusan jur) {
		List list = list(BahanKajian.class, jur, true);
		List<String[]> baris = new ArrayList<String[]>();
		for (int i = 0; i < list.size(); i++) {
			BahanKajian x = (BahanKajian) list.get(i);
			baris.add(new String[] { s(x.getKode()), s(x.getNama()), s(x.getKeterangan()), yaTidak(x.getAktif()) });
		}
		buatTabKomponen(idx, host, "Bahan Kajian",
				"Pokok-pokok keilmuan atau materi yang menjadi bahan penyusun mata kuliah.",
				new String[] { "Kode", "Nama Bahan Kajian", "Keterangan", "Aktif" }, baris,
				"OBE_BahanKajian_" + kodeJur(jur), "Bahan Kajian", "/pages/master/obe/bahan_kajian.zul");
	}

	private void renderCpmk(int idx, Div host, Jurusan jur) {
		List list = list(CapaianPembelajaranLulusan.class, jur, true);
		List<String[]> baris = new ArrayList<String[]>();
		for (int i = 0; i < list.size(); i++) {
			CapaianPembelajaranLulusan x = (CapaianPembelajaranLulusan) list.get(i);
			baris.add(new String[] { s(x.getKode()), s(x.getNama()), angka(x.getBobot()), angka(x.getMinimal()),
					yaTidak(x.getAktif()) });
		}
		buatTabKomponen(idx, host, "Capaian Pembelajaran Mata Kuliah (CPMK)",
				"Target kemampuan pada tiap mata kuliah — penjabaran lebih rinci dari CPL.",
				new String[] { "Kode", "Nama CPMK", "Bobot", "Nilai Minimal", "Aktif" }, baris,
				"OBE_CPMK_" + kodeJur(jur), "Capaian Pembelajaran Mata Kuliah (CPMK)",
				"/pages/master/obe/capaian_pembelajaran_lulusan.zul");
	}

	private void renderMataKuliah(int idx, Div host, Jurusan jur) {
		List list = list(Matakuliah.class, jur, false);
		List<String[]> baris = new ArrayList<String[]>();
		for (int i = 0; i < list.size(); i++) {
			Matakuliah x = (Matakuliah) list.get(i);
			baris.add(new String[] { s(x.getKode()), s(x.getNama()), angka(x.getSks()) });
		}
		buatTabKomponen(idx, host, "Mata Kuliah",
				"Daftar mata kuliah beserta bobot SKS pada program studi.",
				new String[] { "Kode", "Nama Mata Kuliah", "SKS" }, baris,
				"OBE_MataKuliah_" + kodeJur(jur), null, null);
	}

	private void renderMatriksCplProfil(int idx, Div host, Jurusan jur) {
		List cpls = list(CapaianLulusan.class, jur, true, "kode");
		List profils = list(ProfilLulusan.class, jur, true, "kode");
		String[] rowLabels = new String[cpls.size()];
		for (int i = 0; i < cpls.size(); i++) {
			rowLabels[i] = s(((CapaianLulusan) cpls.get(i)).getKode());
		}
		int kol = Math.min(MAKS_KOLOM, profils.size());
		String[] colLabels = new String[kol];
		Long[] colIds = new Long[kol];
		for (int j = 0; j < kol; j++) {
			ProfilLulusan p = (ProfilLulusan) profils.get(j);
			colLabels[j] = s(p.getKode());
			colIds[j] = p.getId();
		}
		Korelasi akses = new Korelasi() {
			public String ambil(Object o) {
				return ((CapaianLulusan) o).getProfil();
			}

			public void simpan(Object o, String v) {
				((CapaianLulusan) o).setProfil(v);
			}
		};
		buatTabMatriksEditable(idx, host, "Matriks CPL × Profil Lulusan",
				"Centang bila CPL (baris) mendukung profil lulusan (kolom). Perubahan langsung tersimpan.", "CPL",
				cpls, rowLabels, CapaianLulusan.class, colLabels, colIds, akses,
				"OBE_Matriks_CPL_Profil_" + kodeJur(jur), "Capaian Pembelajaran Lulusan (CPL)", ZUL_CPL);
	}

	// ── Tujuan / PEO (CapaianJurusan) ─────────────────────────────────────────
	private void renderTujuanPeo(int idx, Div host, Jurusan jur) {
		List l = list(CapaianJurusan.class, jur, true, "nomorUrut");
		List<String[]> baris = new ArrayList<String[]>();
		for (int i = 0; i < l.size(); i++) {
			CapaianJurusan x = (CapaianJurusan) l.get(i);
			String jenis = "";
			try {
				jenis = x.getJenisCapaianJurusan() == null ? "" : s(x.getJenisCapaianJurusan().getNama());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/obe/PikobeAction.java:572");
			}
			baris.add(new String[] { s(x.getNomorUrut()), s(x.getNama()), jenis, s(x.getTahunLulus()),
					yaTidak(x.getAktif()) });
		}
		buatTabKomponen(idx, host, "Tujuan Pendidikan Program Studi (PEO)",
				"Tujuan jangka panjang yang ingin dicapai lulusan beberapa tahun setelah lulus.",
				new String[] { "Urutan", "Tujuan / PEO", "Jenis", "Tahun Setelah Lulus", "Aktif" }, baris,
				"OBE_Tujuan_PEO_" + kodeJur(jur), "Tujuan Pendidikan / PEO", ZUL_PEO);
	}

	// ── Pengelompokan Mata Kuliah (KelompokMatakuliah) ────────────────────────
	private void renderKelompokMk(int idx, Div host, Jurusan jur) {
		List mks = list(Matakuliah.class, jur, false, "kode");
		Map<String, int[]> agg = new LinkedHashMap<String, int[]>(); // nama -> [jmlMk, totalSks]
		Map<String, String> kodeMap = new LinkedHashMap<String, String>();
		for (int i = 0; i < mks.size(); i++) {
			Matakuliah mk = (Matakuliah) mks.get(i);
			String nama = "Tanpa Kelompok";
			String kode = "-";
			try {
				if (mk.getKelompokMatakuliah() != null) {
					nama = s(mk.getKelompokMatakuliah().getNama());
					kode = s(mk.getKelompokMatakuliah().getKode());
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/obe/PikobeAction.java:597");
			}
			int sks = 0;
			try {
				sks = mk.getSks() == null ? 0 : mk.getSks().intValue();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/obe/PikobeAction.java:602");
			}
			int[] a = agg.get(nama);
			if (a == null) {
				a = new int[2];
				agg.put(nama, a);
				kodeMap.put(nama, kode);
			}
			a[0]++;
			a[1] += sks;
		}
		List<String[]> baris = new ArrayList<String[]>();
		for (Map.Entry<String, int[]> e : agg.entrySet()) {
			baris.add(new String[] { s(kodeMap.get(e.getKey())), e.getKey(), String.valueOf(e.getValue()[0]),
					String.valueOf(e.getValue()[1]) });
		}
		buatTabKomponen(idx, host, "Pengelompokan Mata Kuliah",
				"Pengelompokan mata kuliah menurut rumpunnya (mis. Matematika, Sains Dasar, Rekayasa, Desain, Umum) sesuai standar akreditasi.",
				new String[] { "Kode", "Kelompok", "Jumlah MK", "Total SKS" }, baris,
				"OBE_KelompokMK_" + kodeJur(jur), "Kelompok Mata Kuliah", ZUL_KELOMPOK);
	}

	// ── Pemetaan CPL × Kerangka acuan (ReferensiLulusan) — editable ───────────
	private void renderMatriksKerangka(int idx, Div host, Jurusan jur) {
		List cpls = list(CapaianLulusan.class, jur, true, "kode");
		List refs = listSemua(ReferensiLulusan.class, true, "kode");
		String[] rowLabels = new String[cpls.size()];
		for (int i = 0; i < cpls.size(); i++) {
			rowLabels[i] = s(((CapaianLulusan) cpls.get(i)).getKode());
		}
		int kol = Math.min(MAKS_KOLOM, refs.size());
		String[] colLabels = new String[kol];
		Long[] colIds = new Long[kol];
		for (int j = 0; j < kol; j++) {
			ReferensiLulusan rf = (ReferensiLulusan) refs.get(j);
			colLabels[j] = s(rf.getKode());
			colIds[j] = rf.getId();
		}
		Korelasi akses = new Korelasi() {
			public String ambil(Object o) {
				return ((CapaianLulusan) o).getReferensi();
			}

			public void simpan(Object o, String v) {
				((CapaianLulusan) o).setReferensi(v);
			}
		};
		buatTabMatriksEditable(idx, host, "Pemetaan CPL terhadap Kerangka Acuan",
				"Centang bila CPL (baris) memenuhi standar/kerangka acuan (mis. SN-Dikti, KKNI, IABEE, ABET). Perubahan langsung tersimpan.",
				"CPL", cpls, rowLabels, CapaianLulusan.class, colLabels, colIds, akses,
				"OBE_Pemetaan_Kerangka_" + kodeJur(jur), "Referensi / Kerangka Acuan", ZUL_KERANGKA);
	}

	// ── Matriks Bahan Kajian × Mata Kuliah — editable ─────────────────────────
	private void renderMatriksBkMk(int idx, Div host, Jurusan jur) {
		List mks = list(Matakuliah.class, jur, false, "kode");
		List bks = list(BahanKajian.class, jur, true, "kode");
		String[] rowLabels = new String[mks.size()];
		for (int i = 0; i < mks.size(); i++) {
			rowLabels[i] = s(((Matakuliah) mks.get(i)).getKode());
		}
		int kol = Math.min(MAKS_KOLOM, bks.size());
		String[] colLabels = new String[kol];
		Long[] colIds = new Long[kol];
		for (int j = 0; j < kol; j++) {
			BahanKajian b = (BahanKajian) bks.get(j);
			colLabels[j] = s(b.getKode());
			colIds[j] = b.getId();
		}
		Korelasi akses = new Korelasi() {
			public String ambil(Object o) {
				return ((Matakuliah) o).getBahanKajian();
			}

			public void simpan(Object o, String v) {
				((Matakuliah) o).setBahanKajian(v);
			}
		};
		buatTabMatriksEditable(idx, host, "Matriks Bahan Kajian × Mata Kuliah",
				"Centang bila bahan kajian (kolom) dibahas pada mata kuliah (baris). Perubahan langsung tersimpan.",
				"Mata Kuliah", mks, rowLabels, Matakuliah.class, colLabels, colIds, akses,
				"OBE_Matriks_BK_MK_" + kodeJur(jur), null, null);
	}

	// ── Matriks Mata Kuliah × CPL — editable ──────────────────────────────────
	private void renderMatriksMkCpl(int idx, Div host, Jurusan jur) {
		List mks = list(Matakuliah.class, jur, false, "kode");
		List cpls = list(CapaianLulusan.class, jur, true, "kode");
		String[] rowLabels = new String[mks.size()];
		for (int i = 0; i < mks.size(); i++) {
			rowLabels[i] = s(((Matakuliah) mks.get(i)).getKode());
		}
		int kol = Math.min(MAKS_KOLOM, cpls.size());
		String[] colLabels = new String[kol];
		Long[] colIds = new Long[kol];
		for (int j = 0; j < kol; j++) {
			CapaianLulusan c = (CapaianLulusan) cpls.get(j);
			colLabels[j] = s(c.getKode());
			colIds[j] = c.getId();
		}
		Korelasi akses = new Korelasi() {
			public String ambil(Object o) {
				return ((Matakuliah) o).getCapaianLulusan();
			}

			public void simpan(Object o, String v) {
				((Matakuliah) o).setCapaianLulusan(v);
			}
		};
		buatTabMatriksEditable(idx, host, "Matriks Mata Kuliah × CPL",
				"Centang bila mata kuliah (baris) mendukung CPL (kolom). Perubahan langsung tersimpan.",
				"Mata Kuliah", mks, rowLabels, Matakuliah.class, colLabels, colIds, akses,
				"OBE_Matriks_MK_CPL_" + kodeJur(jur), null, null);
	}

	// ════════════════════════════════════════════════════════════════════════
	// MATRIKS EDITABLE (checkbox simpan langsung) + INDIKATOR KINERJA inline
	// ════════════════════════════════════════════════════════════════════════

	/** Akses baca/tulis field korelasi (",id,id,") pada entitas baris. */
	private interface Korelasi {
		String ambil(Object baris);

		void simpan(Object baris, String nilai);
	}

	/**
	 * Bangun tab matriks yang bisa diisi LANGSUNG di grid (checkbox per sel). Mencentang/
	 * melepas sel langsung menyimpan ke DB (tanpa membuka modal). Tombol "Unduh Excel"
	 * mengekspor snapshot saat ini.
	 */
	private void buatTabMatriksEditable(final int idx, Div host, String judul, String deskripsi, String kolom0Label,
			List rowEntities, String[] rowLabels, final Class rowClass, String[] colLabels, final Long[] colIds,
			final Korelasi akses, final String namaFile, String judulKelola, String zulKelola) {

		appendHtml(host, panelHead(judul, deskripsi));

		// snapshot untuk ekspor Excel
		final String[] headersX = new String[colLabels.length + 1];
		headersX[0] = kolom0Label;
		for (int j = 0; j < colLabels.length; j++) {
			headersX[j + 1] = colLabels[j];
		}
		final List<String[]> barisX = new ArrayList<String[]>();
		for (int i = 0; i < rowEntities.size(); i++) {
			Object e = rowEntities.get(i);
			String cur = akses.ambil(e);
			String[] r = new String[headersX.length];
			r[0] = rowLabels[i];
			for (int j = 0; j < colIds.length; j++) {
				r[j + 1] = korelasiMengandung(cur, colIds[j]) ? "✔" : "";
			}
			barisX.add(r);
		}

		Toolbar tb = new Toolbar();
		tb.setStyle("border:none; background:transparent; padding:2px 0 8px 0;");
		tb.setParent(host);
		if (zulKelola != null) {
			final String jk = judulKelola;
			final String zk = zulKelola;
			MyToolbarbuttonConfig bk = new MyToolbarbuttonConfig("Tambah / Kelola", "/img/new.gif");
			bk.addEventListener("onClick", new EventListener() {
				public void onEvent(Event e) throws Exception {
					bukaKelola(jk, zk);
				}
			});
			bk.setParent(tb);
		}
		MyToolbarbuttonConfig bu = new MyToolbarbuttonConfig("Unduh Excel", "/img/save.gif");
		bu.addEventListener("onClick", new EventListener() {
			public void onEvent(Event e) throws Exception {
				PikobeExportHelper.unduhExcel(namaFile, namaFile, headersX, barisX);
			}
		});
		bu.setParent(tb);
		MyToolbarbuttonConfig br = new MyToolbarbuttonConfig("Muat Ulang", "/img/Button-Refresh-icon.png");
		br.addEventListener("onClick", new EventListener() {
			public void onEvent(Event e) throws Exception {
				loaded[idx] = false;
				renderTab(idx);
			}
		});
		br.setParent(tb);
		appendHtml(host, "<div style='font-size:11px;color:#16a34a;margin:0 0 6px 2px;'>"
				+ "✏️ Klik kotak centang untuk mengisi — otomatis tersimpan.</div>");

		Grid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setSclass("dgrid");
		grid.setParent(host);
		Columns cols = new Columns();
		cols.setSizable(true);
		cols.setParent(grid);
		MyColumnConfig c0 = new MyColumnConfig(kolom0Label);
		c0.setWidth("160px");
		c0.setParent(cols);
		for (int j = 0; j < colLabels.length; j++) {
			MyColumnConfig c = new MyColumnConfig(colLabels[j]);
			c.setAlign("center");
			c.setParent(cols);
		}
		Rows rows = new Rows();
		rows.setParent(grid);
		if (rowEntities.isEmpty() || colLabels.length == 0) {
			Row r = new Row();
			r.setParent(rows);
			Label l = new Label("Data baris/kolom belum ada. Lengkapi dulu komponennya.");
			l.setStyle("color:#94a3b8; font-style:italic;");
			l.setParent(r);
			return;
		}
		for (int i = 0; i < rowEntities.size(); i++) {
			Object e = rowEntities.get(i);
			final Long rowId = ((GeneralValueObject) e).getId();
			String cur = akses.ambil(e);
			Row r = new Row();
			r.setParent(rows);
			Label lbl = new Label(rowLabels[i]);
			lbl.setStyle("font-size:12px; font-weight:600;");
			lbl.setParent(r);
			for (int j = 0; j < colIds.length; j++) {
				final Long colId = colIds[j];
				final Checkbox cb = new Checkbox();
				cb.setChecked(korelasiMengandung(cur, colId));
				cb.setParent(r);
				cb.addEventListener("onCheck", new EventListener() {
					public void onEvent(Event ev) throws Exception {
						simpanKorelasi(rowClass, rowId, colId, cb, akses);
					}
				});
			}
		}
	}

	/** Toggle satu sel korelasi lalu simpan ke DB (fail-safe; kembalikan centang bila gagal). */
	private void simpanKorelasi(Class rowClass, Long rowId, Long colId, Checkbox cb, Korelasi akses) {
		try {
			Session ss = HibernateUtil.currentSession();
			Object managed = ss.get(rowClass, rowId);
			if (managed == null) {
				return;
			}
			java.util.LinkedHashSet<String> set = keSet(akses.ambil(managed));
			String id = String.valueOf(colId);
			if (cb.isChecked()) {
				set.add(id);
			} else {
				set.remove(id);
			}
			akses.simpan(managed, gabung(set));
			Common.refreshSaveOrUpdate(ss, (GeneralValueObject) managed);
		} catch (Exception ex) {
			try {
				cb.setChecked(!cb.isChecked());
			} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/obe/PikobeAction.java:857");
			}
			Common.tampilErrorJikaAdmin(ex);
		}
	}

	private java.util.LinkedHashSet<String> keSet(String korelasi) {
		java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<String>();
		if (korelasi != null) {
			String[] bag = korelasi.split(",");
			for (int i = 0; i < bag.length; i++) {
				String t = bag[i].trim();
				if (t.length() > 0) {
					set.add(t);
				}
			}
		}
		return set;
	}

	private String gabung(java.util.Set<String> set) {
		StringBuilder sb = new StringBuilder();
		for (java.util.Iterator<String> it = set.iterator(); it.hasNext();) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(it.next());
		}
		return sb.toString();
	}

	// ── Indikator Kinerja (IK/PI) — entri inline langsung di halaman ──────────
	private void renderIndikatorKinerja(final int idx, Div host, final Jurusan jur) {
		appendHtml(host, panelHead("Indikator Kinerja (IK) / Performance Indicator (PI)",
				"Penjabaran terukur dari tiap CPL untuk menilai ketercapaiannya. Tambah baris langsung di sini."));

		final List cpls = list(CapaianLulusan.class, jur, true, "kode");

		// — baris entri cepat (tanpa modal) —
		Div addbar = new Div();
		addbar.setStyle("background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:10px;margin-bottom:10px;");
		addbar.setParent(host);
		Hbox h = new Hbox();
		h.setAlign("end");
		h.setStyle("flex-wrap:wrap;gap:8px;");
		h.setParent(addbar);

		final Combobox cbCpl = new Combobox();
		cbCpl.setReadonly(true);
		cbCpl.setWidth("220px");
		for (int i = 0; i < cpls.size(); i++) {
			CapaianLulusan c = (CapaianLulusan) cpls.get(i);
			Comboitem it = new Comboitem(s(c.getKode()) + " - " + potongTeks(s(c.getNama()), 40));
			it.setValue(c);
			it.setParent(cbCpl);
		}
		final Textbox tKode = new Textbox();
		tKode.setWidth("90px");
		final Textbox tNama = new Textbox();
		tNama.setWidth("280px");
		final Decimalbox dBobot = new Decimalbox();
		dBobot.setWidth("70px");
		h.appendChild(kolom("CPL", cbCpl));
		h.appendChild(kolom("Kode IK", tKode));
		h.appendChild(kolom("Uraian Indikator", tNama));
		h.appendChild(kolom("Bobot", dBobot));
		Button btn = new Button("Tambah");
		btn.setStyle("background:#16a34a;color:#fff;border:none;border-radius:6px;padding:6px 14px;font-weight:700;");
		btn.addEventListener("onClick", new EventListener() {
			public void onEvent(Event e) throws Exception {
				CapaianLulusan cpl = (cbCpl.getSelectedItem() == null) ? null
						: (CapaianLulusan) cbCpl.getSelectedItem().getValue();
				String nama = tNama.getValue() == null ? "" : tNama.getValue().trim();
				if (cpl == null || nama.length() == 0) {
					ais.ui.util.MyMessageboxConfig.show("Mohon maaf, data belum lengkap. Langkah yang dapat dilakukan: (1) pilih CPL terlebih dahulu; (2) isi Uraian Indikator; (3) ulangi penyimpanan.", "Lengkapi Data",
							ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.EXCLAMATION);
					return;
				}
				try {
					IndikatorKinerja ik = new IndikatorKinerja();
					ik.setJurusan(jur);
					ik.setCapaianLulusan(cpl);
					ik.setKode(tKode.getValue());
					ik.setNama(nama);
					ik.setBobot(dBobot.getValue() == null ? Double.valueOf(0) : Double.valueOf(dBobot.getValue().doubleValue()));
					ik.setAktif(Boolean.TRUE);
					Common.refreshSaveOrUpdate(HibernateUtil.currentSession(), ik);
					loaded[idx] = false;
					renderTab(idx);
				} catch (Exception ex) {
					Common.tampilErrorJikaAdmin(ex);
				}
			}
		});
		Div bw = new Div();
		bw.setStyle("display:flex;align-items:flex-end;");
		bw.appendChild(btn);
		h.appendChild(bw);

		// — toolbar + daftar IK —
		List iks = list(IndikatorKinerja.class, jur, true, "id");
		final List<String[]> barisX = new ArrayList<String[]>();
		for (int i = 0; i < iks.size(); i++) {
			IndikatorKinerja ik = (IndikatorKinerja) iks.get(i);
			String cpl = "";
			try {
				cpl = ik.getCapaianLulusan() == null ? "" : s(ik.getCapaianLulusan().getKode());
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/obe/PikobeAction.java:964");
			}
			barisX.add(new String[] { cpl, s(ik.getKode()), s(ik.getNama()), angka(ik.getBobot()) });
		}
		final String namaFile = "OBE_IndikatorKinerja_" + kodeJur(jur);
		Toolbar tb = new Toolbar();
		tb.setStyle("border:none; background:transparent; padding:2px 0 8px 0;");
		tb.setParent(host);
		MyToolbarbuttonConfig bu = new MyToolbarbuttonConfig("Unduh Excel", "/img/save.gif");
		bu.addEventListener("onClick", new EventListener() {
			public void onEvent(Event e) throws Exception {
				PikobeExportHelper.unduhExcel(namaFile, namaFile,
						new String[] { "CPL", "Kode IK", "Uraian Indikator (IK/PI)", "Bobot" }, barisX);
			}
		});
		bu.setParent(tb);
		MyToolbarbuttonConfig br = new MyToolbarbuttonConfig("Muat Ulang", "/img/Button-Refresh-icon.png");
		br.addEventListener("onClick", new EventListener() {
			public void onEvent(Event e) throws Exception {
				loaded[idx] = false;
				renderTab(idx);
			}
		});
		br.setParent(tb);
		Label jml = new Label("Jumlah: " + iks.size() + " indikator");
		jml.setStyle("font-size:11px; color:#64748b; margin-left:8px;");
		tb.appendChild(jml);

		Grid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setSclass("dgrid");
		grid.setParent(host);
		Columns cols = new Columns();
		cols.setSizable(true);
		cols.setParent(grid);
		new MyColumnConfig("No").setParent(cols);
		new MyColumnConfig("CPL").setParent(cols);
		new MyColumnConfig("Kode IK").setParent(cols);
		new MyColumnConfig("Uraian Indikator (IK/PI)").setParent(cols);
		new MyColumnConfig("Bobot").setParent(cols);
		new MyColumnConfig("Aksi").setParent(cols);
		Rows rows = new Rows();
		rows.setParent(grid);
		if (iks.isEmpty()) {
			Row r = new Row();
			r.setParent(rows);
			Label l = new Label(ais.common.Common.getBahasaConfig("Belum ada indikator. Tambahkan di atas."));
			l.setStyle("color:#94a3b8; font-style:italic;");
			l.setParent(r);
			return;
		}
		for (int i = 0; i < iks.size(); i++) {
			IndikatorKinerja ik = (IndikatorKinerja) iks.get(i);
			final Long ikId = ik.getId();
			String cpl = "";
			try {
				cpl = ik.getCapaianLulusan() == null ? "" : s(ik.getCapaianLulusan().getKode());
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/obe/PikobeAction.java:1021");
			}
			Row r = new Row();
			r.setParent(rows);
			new Label(String.valueOf(i + 1)).setParent(r);
			new Label(cpl).setParent(r);
			new Label(s(ik.getKode())).setParent(r);
			Label u = new Label(s(ik.getNama()));
			u.setStyle("font-size:12px;");
			u.setParent(r);
			new Label(angka(ik.getBobot())).setParent(r);
			Button hapus = new Button("Hapus");
			hapus.setStyle("background:#dc2626;color:#fff;border:none;border-radius:5px;padding:3px 10px;");
			hapus.addEventListener("onClick", new EventListener() {
				public void onEvent(Event e) throws Exception {
					try {
						Session ss = HibernateUtil.currentSession();
						Object m = ss.get(IndikatorKinerja.class, ikId);
						if (m != null) {
							ss.delete(m);
						}
						loaded[idx] = false;
						renderTab(idx);
					} catch (Exception ex) {
						Common.tampilErrorJikaAdmin(ex);
					}
				}
			});
			hapus.setParent(r);
		}
	}

	private Div kolom(String label, org.zkoss.zk.ui.Component field) {
		Div d = new Div();
		d.setStyle("display:flex;flex-direction:column;gap:2px;");
		Label l = new Label(label);
		l.setStyle("font-size:11px;color:#475569;font-weight:600;");
		l.setParent(d);
		field.setParent(d);
		return d;
	}

	private String potongTeks(String v, int maks) {
		if (v == null) {
			return "";
		}
		v = v.trim();
		return v.length() > maks ? v.substring(0, maks - 1) + "…" : v;
	}

	// ── Peta Pemenuhan CPL ────────────────────────────────────────────────────
	private void renderPetaPemenuhanCpl(int idx, Div host, Jurusan jur) {
		List cpls = list(CapaianLulusan.class, jur, true, "kode");
		List mks = list(Matakuliah.class, jur, false, "kode");
		List<String[]> baris = new ArrayList<String[]>();
		for (int i = 0; i < cpls.size(); i++) {
			CapaianLulusan c = (CapaianLulusan) cpls.get(i);
			int jmlMk = 0;
			StringBuilder daftar = new StringBuilder();
			for (int m = 0; m < mks.size(); m++) {
				Matakuliah mk = (Matakuliah) mks.get(m);
				if (korelasiMengandung(mk.getCapaianLulusan(), c.getId())) {
					jmlMk++;
					if (daftar.length() > 0) {
						daftar.append(", ");
					}
					daftar.append(s(mk.getKode()));
				}
			}
			int jmlCpmk = jumlahKorelasi(c.getCapaianPembelajaranLulusan());
			baris.add(new String[] { s(c.getKode()), s(c.getNama()), String.valueOf(jmlMk), String.valueOf(jmlCpmk),
					daftar.toString() });
		}
		buatTabKomponen(idx, host, "Peta Pemenuhan CPL",
				"Memastikan setiap CPL benar-benar dipenuhi: berapa mata kuliah & CPMK yang mendukungnya. CPL dengan 0 pendukung perlu diperbaiki.",
				new String[] { "CPL", "Nama CPL", "Jml MK", "Jml CPMK", "Mata Kuliah Pendukung" }, baris,
				"OBE_Peta_Pemenuhan_CPL_" + kodeJur(jur), null, null);
	}

	// ── Dokumen Akreditasi (Akreditasi + DokumenAkreditasi) ───────────────────
	@SuppressWarnings("unchecked")
	private void renderDokumenAkreditasi(int idx, Div host, Jurusan jur) {
		List<String[]> baris = new ArrayList<String[]>();
		try {
			List docs = HibernateUtil.currentSession().createCriteria(DokumenAkreditasi.class, "d")
					.createAlias("d.akreditasi", "ak").add(Restrictions.eq("ak.jurusan", jur))
					.addOrder(Order.asc("d.nomorUrut")).list();
			for (int i = 0; i < docs.size(); i++) {
				DokumenAkreditasi d = (DokumenAkreditasi) docs.get(i);
				String akr = "";
				String lemb = "";
				try {
					if (d.getAkreditasi() != null) {
						akr = s(d.getAkreditasi().getNama());
						lemb = s(d.getAkreditasi().getLembaga());
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/obe/PikobeAction.java:1117");
				}
				String tgl = "";
				try {
					tgl = d.getTanggalDokumen() == null ? "" : Common.dateFormat.get().format(d.getTanggalDokumen());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/obe/PikobeAction.java:1122");
				}
				baris.add(new String[] { akr, lemb, s(d.getKode()), s(d.getNama()), tgl, yaTidak(d.getAktif()) });
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/obe/PikobeAction.java:1126");
		}
		buatTabKomponen(idx, host, "Dokumen Akreditasi Pendukung",
				"Daftar dokumen akreditasi prodi (termasuk dokumen kurikulum OBE ini) yang tersimpan sebagai berkas pendukung.",
				new String[] { "Akreditasi", "Lembaga", "Kode", "Dokumen", "Tanggal", "Aktif" }, baris,
				"Dokumen_Akreditasi_" + kodeJur(jur), "Akreditasi & Dokumen", ZUL_AKREDITASI);
	}

	private void renderOrganisasiMK(int idx, Div host, Jurusan jur) throws Exception {
		List<String[]> baris = new ArrayList<String[]>();
		String kurikulumNama = "-";

		Session sess = HibernateUtil.currentSession();
		Criteria critK = sess.createCriteria(Kurikulum.class);
		critK.add(Restrictions.eq("jurusan.id", jur.getId()));
		critK.add(Restrictions.eq("obe", true));
		critK.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		critK.addOrder(Order.desc("id"));
		List kList = critK.list();

		if (!kList.isEmpty()) {
			Kurikulum kur = (Kurikulum) kList.get(0);
			kurikulumNama = kur.getNama() != null ? kur.getNama() : "-";

			Criteria critKpm = sess.createCriteria(KurikulumPunyaMatakuliah.class);
			critKpm.createAlias("matakuliah", "mk");
			critKpm.add(Restrictions.eq("kurikulum.id", kur.getId()));
			critKpm.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			critKpm.add(Restrictions.or(Restrictions.isNull("mk.aktif"), Restrictions.eq("mk.aktif", true)));
			critKpm.addOrder(Order.asc("semester")).addOrder(Order.asc("mk.kode"));
			List listKpm = critKpm.list();

			String[] smtLabel = { "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII" };

			for (int i = 0; i < listKpm.size(); i++) {
				KurikulumPunyaMatakuliah kpm = (KurikulumPunyaMatakuliah) listKpm.get(i);
				Matakuliah mk = kpm.getMatakuliah();
				if (mk == null) continue;

				Integer smt = kpm.getSemester();
				if (smt == null || smt < 1) smt = 1;
				String smtStr = (smt >= 1 && smt <= smtLabel.length) ? "Smt " + smtLabel[smt - 1] : "Smt " + smt;

				String kodeMk = s(mk.getKode());
				String namaMk = s(mk.getNama());
				String sksStr = mk.getSks() != null ? String.valueOf(mk.getSks()) : "0";

				String kelompokStr = "-";
				try {
					KelompokMatakuliah kel = mk.getKelompokMatakuliah();
					if (kel != null) {
						String kk = kel.getKode() != null ? kel.getKode().trim() : "";
						String kn = kel.getNama() != null ? kel.getNama().trim() : "";
						kelompokStr = (kk.isEmpty() ? "" : kk + " - ") + kn;
						if (kelompokStr.trim().isEmpty() || kelompokStr.trim().equals("-")) kelompokStr = "-";
					}
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/obe/PikobeAction.java:renderOrganisasiMK-kelompok");
				}

				baris.add(new String[] { smtStr, kodeMk, namaMk, sksStr, kelompokStr });
			}
		}

		buatTabKomponen(idx, host, "Tabel 13. Organisasi Matakuliah per Semester",
				"Distribusi matakuliah dalam kurikulum OBE per semester. Kurikulum: " + kurikulumNama,
				new String[] { "Semester", "Kode MK", "Nama Matakuliah", "SKS", "Kelompok MK" }, baris,
				"Organisasi_MK_" + kodeJur(jur), null, null);
	}

	// ════════════════════════════════════════════════════════════════════════
	// TAB RINGKASAN (dashboard HTML/CSS)
	// ════════════════════════════════════════════════════════════════════════

	private void renderRingkasan(Div host, Jurusan jur) {
		int nProfesi = cnt(ProfesiLulusan.class, jur, true);
		int nProfil = cnt(ProfilLulusan.class, jur, true);
		int nCpl = cnt(CapaianLulusan.class, jur, true);
		int nBk = cnt(BahanKajian.class, jur, true);
		int nCpmk = cnt(CapaianPembelajaranLulusan.class, jur, true);
		int nMk = cnt(Matakuliah.class, jur, false);

		StringBuilder sb = new StringBuilder();
		appendHtml(host, panelHead("Ringkasan OBE — " + s(jur.getNama()),
				"Gambaran cepat seluruh isi kurikulum OBE program studi Anda dalam bentuk angka dan grafik."));

		// kartu angka
		String[] kartu = new String[] {
				CommonDashboardHtmlHelper.metricCard("Profesi Lulusan", String.valueOf(nProfesi), "Target peran lulusan"),
				CommonDashboardHtmlHelper.metricCard("Profil Lulusan", String.valueOf(nProfil), "Kemampuan utama lulusan"),
				CommonDashboardHtmlHelper.metricCard("CPL", String.valueOf(nCpl), "Capaian wajib lulusan"),
				CommonDashboardHtmlHelper.metricCard("Bahan Kajian", String.valueOf(nBk), "Pokok materi keilmuan"),
				CommonDashboardHtmlHelper.metricCard("CPMK", String.valueOf(nCpmk), "Target tiap mata kuliah"),
				CommonDashboardHtmlHelper.metricCard("Mata Kuliah", String.valueOf(nMk), "Mata kuliah prodi") };
		sb.append(CommonDashboardHtmlHelper.panel("Jumlah Komponen",
				"Banyaknya data yang sudah Anda isi pada tiap bagian kurikulum OBE.",
				CommonDashboardHtmlHelper.cards(kartu)));

		// kelengkapan
		int adaKomponen = 0;
		if (nProfesi > 0) adaKomponen++;
		if (nProfil > 0) adaKomponen++;
		if (nCpl > 0) adaKomponen++;
		if (nBk > 0) adaKomponen++;
		if (nCpmk > 0) adaKomponen++;
		if (nMk > 0) adaKomponen++;
		int persen = CommonDashboardHtmlHelper.percent(adaKomponen, 6);
		StringBuilder prog = new StringBuilder();
		prog.append(CommonDashboardHtmlHelper.progressBar(persen, "Kelengkapan penyusunan",
				adaKomponen + " dari 6 komponen sudah terisi"));
		prog.append(CommonDashboardHtmlHelper.barRow("Profesi Lulusan", nProfesi, Math.max(1, nProfesi)));
		sb.append(CommonDashboardHtmlHelper.panel("Kelengkapan OBE",
				"Seberapa lengkap komponen OBE sudah diisi, dari Profesi sampai Mata Kuliah.",
				prog.toString()));

		// radar: cakupan CPL oleh MK
		List cpls = list(CapaianLulusan.class, jur, true);
		List mks = list(Matakuliah.class, jur, false);
		int batas = Math.min(8, cpls.size());
		String[] axCpl = new String[batas];
		double[] valCover = new double[batas];
		double[] valCpmk = new double[batas];
		String[] axCpl2 = new String[batas];
		double maxCover = 1;
		for (int i = 0; i < batas; i++) {
			CapaianLulusan c = (CapaianLulusan) cpls.get(i);
			axCpl[i] = s(c.getKode());
			axCpl2[i] = s(c.getKode());
			int cover = 0;
			for (int m = 0; m < mks.size(); m++) {
				Matakuliah mk = (Matakuliah) mks.get(m);
				if (korelasiMengandung(mk.getCapaianLulusan(), c.getId())) {
					cover++;
				}
			}
			valCover[i] = cover;
			if (cover > maxCover) {
				maxCover = cover;
			}
			valCpmk[i] = jumlahKorelasi(c.getCapaianPembelajaranLulusan());
		}
		if (batas >= 3) {
			sb.append(CommonDashboardHtmlHelper.panel("Cakupan CPL oleh Mata Kuliah",
					"Melihat apakah setiap CPL sudah didukung mata kuliah. Makin lebar dan merata jaringnya, makin baik.",
					PikobeChartHelper.radar(axCpl, valCover, maxCover)));
		}

		// donut: CPL per kelompok
		Map<String, Double> perKategori = new LinkedHashMap<String, Double>();
		for (int i = 0; i < cpls.size(); i++) {
			CapaianLulusan c = (CapaianLulusan) cpls.get(i);
			String k = (c.getKategori() == null || c.getKategori().trim().length() == 0) ? "Lainnya"
					: c.getKategori().trim();
			Double v = perKategori.get(k);
			perKategori.put(k, Double.valueOf((v == null ? 0 : v.doubleValue()) + 1));
		}
		if (!perKategori.isEmpty()) {
			String[] lk = new String[perKategori.size()];
			double[] vk = new double[perKategori.size()];
			int z = 0;
			for (Map.Entry<String, Double> e : perKategori.entrySet()) {
				lk[z] = e.getKey();
				vk[z] = e.getValue().doubleValue();
				z++;
			}
			sb.append(CommonDashboardHtmlHelper.panel("CPL per Kelompok",
					"Pembagian CPL menurut kelompoknya (misalnya Sikap, Pengetahuan, Keterampilan).",
					PikobeChartHelper.donut(lk, vk)));
		}

		// tren: jumlah CPMK per CPL
		if (batas >= 2) {
			sb.append(CommonDashboardHtmlHelper.panel("Penjabaran CPL menjadi CPMK",
					"Banyaknya CPMK yang menjabarkan tiap CPL — membantu melihat CPL mana yang masih kurang diuraikan.",
					PikobeChartHelper.trend(axCpl2, valCpmk)));
		}

		// batang: banyak data tiap komponen
		String[] labKomp = new String[] { "Profesi", "Profil", "CPL", "Bahan Kajian", "CPMK", "Mata Kuliah" };
		double[] valKomp = new double[] { nProfesi, nProfil, nCpl, nBk, nCpmk, nMk };
		sb.append(CommonDashboardHtmlHelper.panel("Perbandingan Jumlah Komponen",
				"Membandingkan banyaknya data pada tiap komponen OBE secara sekilas.",
				PikobeChartHelper.bars(labKomp, valKomp, "#2563eb")));

		appendHtml(host, "<div class='pikobe-dash'>" + sb.toString() + "</div>");
	}

	// ════════════════════════════════════════════════════════════════════════
	// QUERY & UTIL
	// ════════════════════════════════════════════════════════════════════════

	private int cnt(Class cls, Jurusan jur, boolean useAktif) {
		try {
			Criteria c = HibernateUtil.currentSession().createCriteria(cls).add(Restrictions.eq("jurusan", jur));
			if (useAktif) {
				c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			}
			Number n = (Number) c.setProjection(Projections.rowCount()).uniqueResult();
			return n == null ? 0 : n.intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private List list(Class cls, Jurusan jur, boolean useAktif) {
		return list(cls, jur, useAktif, "kode");
	}

	@SuppressWarnings("unchecked")
	private List list(Class cls, Jurusan jur, boolean useAktif, String orderProp) {
		try {
			Criteria c = HibernateUtil.currentSession().createCriteria(cls).add(Restrictions.eq("jurusan", jur));
			if (useAktif) {
				c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			}
			if (orderProp != null) {
				c.addOrder(Order.asc(orderProp));
			}
			List l = c.list();
			return l == null ? new ArrayList() : l;
		} catch (Exception e) {
			return new ArrayList();
		}
	}

	/** Daftar tanpa filter jurusan (untuk entitas tingkat-PT seperti ReferensiLulusan). */
	@SuppressWarnings("unchecked")
	private List listSemua(Class cls, boolean useAktif, String orderProp) {
		try {
			Criteria c = HibernateUtil.currentSession().createCriteria(cls);
			if (useAktif) {
				c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			}
			if (orderProp != null) {
				c.addOrder(Order.asc(orderProp));
			}
			List l = c.list();
			return l == null ? new ArrayList() : l;
		} catch (Exception e) {
			return new ArrayList();
		}
	}

	/** Apakah string korelasi (format ",id,id,") memuat id tertentu. */
	private boolean korelasiMengandung(String korelasi, Long id) {
		if (korelasi == null || id == null) {
			return false;
		}
		String[] bag = korelasi.split(",");
		for (int i = 0; i < bag.length; i++) {
			String t = bag[i].trim();
			if (t.length() > 0 && t.equals(String.valueOf(id))) {
				return true;
			}
		}
		return false;
	}

	/** Hitung banyaknya id dalam string korelasi. */
	private int jumlahKorelasi(String korelasi) {
		if (korelasi == null) {
			return 0;
		}
		int n = 0;
		String[] bag = korelasi.split(",");
		for (int i = 0; i < bag.length; i++) {
			if (bag[i].trim().length() > 0) {
				n++;
			}
		}
		return n;
	}

	private String kodeJur(Jurusan jur) {
		try {
			String k = jur.getNama();
			return k == null ? "Prodi" : k.replaceAll("[^A-Za-z0-9]+", "");
		} catch (Exception e) {
			return "Prodi";
		}
	}

	private String s(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private String yaTidak(Boolean b) {
		return Boolean.TRUE.equals(b) ? "Ya" : "Tidak";
	}

	private String angka(Object o) {
		if (o == null) {
			return "";
		}
		if (o instanceof Number) {
			double d = ((Number) o).doubleValue();
			if (d == Math.floor(d)) {
				return String.valueOf((long) d);
			}
			return String.valueOf(d);
		}
		return String.valueOf(o);
	}

	private void appendHtml(org.zkoss.zk.ui.Component parent, String html) {
		Html h = new Html(html);
		h.setParent(parent);
	}

	private String panelHead(String judul, String deskripsi) {
		return "<div class='pikobe-head'><div class='pikobe-head-judul'>" + CommonDashboardHtmlHelper.escape(judul)
				+ "</div><div class='pikobe-head-desk'>" + CommonDashboardHtmlHelper.escape(deskripsi)
				+ "</div></div>";
	}

	/** CSS responsif & modern, disuntik sekali. */
	private String gaya() {
		return "<style>"
				+ ".pikobe-filter{background:linear-gradient(135deg,#1e3a8a,#2563eb);color:#fff;border-radius:12px;"
				+ "padding:14px 18px;margin:4px 4px 12px 4px;box-shadow:0 10px 24px rgba(37,99,235,.18);}"
				+ ".pikobe-judul{font-size:17px;font-weight:800;}"
				+ ".pikobe-sub{font-size:12px;opacity:.85;margin-top:2px;margin-bottom:8px;}"
				+ ".pikobe-filter-bar{flex-wrap:wrap;gap:8px;}"
				+ ".pikobe-lbl{color:#e0ecff;font-size:12px;font-weight:600;margin:0 4px 0 6px;}"
				+ ".pikobe-tabs .z-tabs{overflow-x:auto;white-space:nowrap;}"
				+ ".pikobe-panelhost{padding:6px 4px 14px 4px;box-sizing:border-box;}"
				+ ".pikobe-head{margin:4px 2px 10px 2px;}"
				+ ".pikobe-head-judul{font-size:16px;font-weight:800;color:#0f172a;}"
				+ ".pikobe-head-desk{font-size:12px;color:#64748b;margin-top:2px;line-height:1.45;}"
				+ ".pikobe-dash{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:12px;}"
				+ "@media(max-width:640px){.pikobe-dash{grid-template-columns:1fr;}.pikobe-combo{width:130px;}}"
				+ "</style>";
	}
}
