package ais.action.master.dashboard.keuangan;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.maintenance.MainAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldMerah;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

/**
 * Merangkum piutang mahasiswa agar total tagihan, pembayaran, dan sisa kewajiban mudah dipantau.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardPiutang extends MyWindow {

	private static final long serialVersionUID = 3557603220165512688L;
	private Div center;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox jenisPembayaran = new Combobox();
	private MyTextbox nama;
	private Grid grid;

	private static final int POPUP_PAGE_SIZE = 10;
	private static final int MAIN_GRID_PAGE_SIZE = 10;
	private static final int SEGMENT_LIMIT = 12;

	public DasboardPiutang() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DasboardPiutang(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * PEMBERSIH SESSION TERPUSAT - PENCEGAHAN MEMORY LEAK (OOM)
	 */
	private void cleanupSession(Session session) {
		ais.ui.util.DashboardModernHtmlUtil.closeOpenedSession(session);
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		jenisPembayaran = Common.createComboJenisPembayaranDanSemua(jenisPembayaran);
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		org.zkoss.zk.ui.Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data piutang yang ditampilkan.",
				"Kartu Piutang",
				"Ringkasan piutang (tagihan belum terbayar) mahasiswa, beserta grafiknya.");
		org.zkoss.zk.ui.Component saringanHost = hostPortal[0];
		center = (org.zkoss.zul.Div) hostPortal[1];

		MyGrid formGrid = new MyGrid();
		formGrid.setWidth("100%");
		formGrid.setParent(saringanHost);
		formGrid.setWidth("100%");
		formGrid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(formGrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("150px");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setWidth("150px");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setWidth("150px");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(formGrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(nama = new MyTextbox());
		nama.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
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
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran"));
		row.appendChild(jenisPembayaran);
		jenisPembayaran.setWidth("90%");

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				reload();
			}
		};

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "1,1,4");

		MyToolbarbuttonConfig tampilkan = new MyToolbarbuttonConfig("Tampilkan Dasbor", "/img/search.png");
		row.appendChild(tampilkan);
		tampilkan.addEventListener("onClick", eventListener);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download Excel", "/img/excel.png");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (grid != null)
					UIUtil.downloadGrid(grid);
			}
		});
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		Common.createDefaultTimer(eventListener);
	}

	@SuppressWarnings({ })
	private void reload() {
		Common.clear(center);

		Vbox mainVbox = new Vbox();
		mainVbox.setParent(center);
		mainVbox.setWidth("100%");
		mainVbox.setSpacing("14px");
		mainVbox.setStyle("padding: 12px; background:#f6f8fb; box-sizing:border-box;");

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			List<DashboardRow> objects = loadDashboardRows(session, null, null, null);
			DashboardData data = buildDashboardData(objects);

			renderHero(mainVbox, data);
			renderFilterSummary(mainVbox);
			renderMetricCards(mainVbox, data);
			ais.ui.util.DashboardJurnalPembayaranUtil.renderJurnalMahasiswaPanel(mainVbox, "Ringkasan Jurnal Pembayaran Mahasiswa",
					"Menunjukkan hubungan pembayaran mahasiswa dengan akun kas/bank, piutang, dan pendapatan. Petugas bisa melihat lebih cepat apakah akun pembayaran sudah siap diposting.");

			if (objects == null || objects.isEmpty()) {
				Div empty = new Div();
				empty.setParent(mainVbox);
				empty.setStyle("padding:16px; border:1px solid #fde68a; background:#fffbeb; color:#92400e; border-radius:14px; font-weight:bold;");
				empty.appendChild(new Label("Tidak ada data piutang/pembayaran yang sesuai dengan filter."));
				return;
			}

			renderFunnelPiutang(mainVbox, data);
			renderCollectionHealth(mainVbox, data);
			renderCollectionRadar(mainVbox, data);
			renderCharts(mainVbox, data);
			renderSegmentTables(mainVbox, data);
			renderMainGrid(mainVbox, objects, data);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/keuangan/DasboardPiutang.java:276");
			Common.tampilErrorJikaAdmin(e);
		} finally {
			cleanupSession(session);
		}
	}

	private FilterState getCurrentFilterState() {
		FilterState fs = new FilterState();
		fs.tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());
		fs.semester = (String) (this.semesterAbsensi.getSelectedItem() == null
				|| this.semesterAbsensi.getSelectedItem().getValue() == null ? null
						: this.semesterAbsensi.getSelectedItem().getValue());
		fs.fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null : searchfakultas.getSelectedItem().getValue());
		fs.jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null : searchjurusan.getSelectedItem().getValue());
		fs.jenisPembayaran = (JenisKegiatan) (this.jenisPembayaran.getSelectedItem() == null ? null
				: this.jenisPembayaran.getSelectedItem().getValue());
		fs.keyword = nama == null || nama.getValue() == null ? "" : nama.getValue().trim();
		return fs;
	}

	@SuppressWarnings("unchecked")
	private List<DashboardRow> loadDashboardRows(Session session, String detailType, String groupBy, String groupValue)
			throws Exception {
		FilterState fs = getCurrentFilterState();
		StringBuffer sql = new StringBuffer();
		sql.append("select ");
		sql.append(" coalesce(c.nim, b.no_registrasi, '') as kode_transaksi, ");
		sql.append(" coalesce(c.nama, b.nama, '') as nama, ");
		sql.append(" coalesce(d.nama_kegiatan, 'Lain-lain') as nama_jenis_kegiatan, ");
		sql.append(" coalesce(a.dibayar,0) as dibayar, coalesce(a.tagihan,0) as tagihan, ");
		sql.append(" coalesce(a.tahun_akademik,'') as tahun_akademik, coalesce(a.semster,0) as semster, ");
		sql.append(" coalesce(f.nama,'Tanpa Fakultas') as fakultas, coalesce(x.nama,'Tanpa Prodi') as jurusan ");
		sql.append(" from kegiatan a ");
		sql.append(" left join biodata_calon_mahasiswa b on (a.calon_mahasiswa = b.id) ");
		sql.append(" left join mahasiswa c on (a.mahasiswa = c.id) ");
		sql.append(" inner join jenis_kegiatan d on (a.jenis_kegiatan=d.id) ");
		sql.append(" left join jurusan x on (a.jurusan = x.id) ");
		sql.append(" left join fakultas f on (x.fakultas = f.id) ");
		sql.append(" where a.aktif ");
		sql.append(" and (c.nama ilike :buktilike or b.nama ilike :buktilike or c.nim ilike :buktilike or b.no_registrasi ilike :buktilike) ");
		sql.append(" and (coalesce(a.dibayar,0) > 0.1 or coalesce(a.tagihan,0) > 0.1) ");

		if (fs.jurusan != null) {
			sql.append(" and a.jurusan = ").append(fs.jurusan.getId());
		}
		if (fs.fakultas != null) {
			sql.append(" and x.fakultas = ").append(fs.fakultas.getId());
		}
		if (fs.jenisPembayaran != null) {
			sql.append(" and a.jenis_kegiatan = ").append(fs.jenisPembayaran.getId());
		}
		if (fs.tahunAkademik != null) {
			sql.append(" and a.tahun_akademik = :tahunAkademik ");
		}
		if (fs.semester != null) {
			sql.append(fs.semester.equals(Perkuliahan.GENAP) ? " and a.semster % 2 = 0 " : " and a.semster % 2 = 1 ");
		}

		applyDetailTypeWhere(sql, detailType);
		applyGroupWhere(sql, groupBy, groupValue);

		sql.append(" order by d.nama_kegiatan, coalesce(c.nama, b.nama, ''), coalesce(c.nim, b.no_registrasi, '') ");

		Query query = session.createSQLQuery(sql.toString()).setString("buktilike", "%" + fs.keyword + "%");
		if (fs.tahunAkademik != null) {
			query.setString("tahunAkademik", fs.tahunAkademik);
		}
		if (groupBy != null && groupValue != null && !"status".equalsIgnoreCase(groupBy)) {
			query.setString("groupValue", groupValue);
		}

		List<Object[]> raw = query.list();
		List<DashboardRow> rows = new ArrayList<DashboardRow>();
		if (raw != null) {
			for (int i = 0; i < raw.size(); i++) {
				rows.add(toDashboardRow(raw.get(i)));
			}
		}
		return rows;
	}

	private void applyDetailTypeWhere(StringBuffer sql, String detailType) {
		if (detailType == null || detailType.trim().length() == 0 || "data".equalsIgnoreCase(detailType)) {
			return;
		}
		if ("tagihan".equalsIgnoreCase(detailType)) {
			sql.append(" and coalesce(a.tagihan,0) > 0.1 ");
		} else if ("dibayar".equalsIgnoreCase(detailType)) {
			sql.append(" and coalesce(a.dibayar,0) > 0.1 ");
		} else if ("piutang".equalsIgnoreCase(detailType)) {
			sql.append(" and (coalesce(a.tagihan,0)-coalesce(a.dibayar,0)) > 0.1 ");
		} else if ("lunas".equalsIgnoreCase(detailType)) {
			sql.append(" and coalesce(a.tagihan,0) > 0.1 and coalesce(a.dibayar,0) >= (coalesce(a.tagihan,0)-0.1) ");
		} else if ("belumbayar".equalsIgnoreCase(detailType)) {
			sql.append(" and coalesce(a.tagihan,0) > 0.1 and coalesce(a.dibayar,0) <= 0.1 ");
		} else if ("parsial".equalsIgnoreCase(detailType)) {
			sql.append(" and coalesce(a.dibayar,0) > 0.1 and (coalesce(a.tagihan,0)-coalesce(a.dibayar,0)) > 0.1 ");
		} else if ("lebihbayar".equalsIgnoreCase(detailType)) {
			sql.append(" and (coalesce(a.dibayar,0)-coalesce(a.tagihan,0)) > 0.1 ");
		} else if ("tanpatagihan".equalsIgnoreCase(detailType)) {
			sql.append(" and coalesce(a.tagihan,0) <= 0.1 and coalesce(a.dibayar,0) > 0.1 ");
		}
	}

	private void applyGroupWhere(StringBuffer sql, String groupBy, String groupValue) {
		if (groupBy == null || groupBy.trim().length() == 0) {
			return;
		}
		if ("jenis".equalsIgnoreCase(groupBy)) {
			sql.append(" and coalesce(d.nama_kegiatan, 'Lain-lain') = :groupValue ");
		} else if ("fakultas".equalsIgnoreCase(groupBy)) {
			sql.append(" and coalesce(f.nama, 'Tanpa Fakultas') = :groupValue ");
		} else if ("jurusan".equalsIgnoreCase(groupBy)) {
			sql.append(" and coalesce(x.nama, 'Tanpa Prodi') = :groupValue ");
		} else if ("tahun".equalsIgnoreCase(groupBy)) {
			sql.append(" and coalesce(a.tahun_akademik, '') = :groupValue ");
		} else if ("semester".equalsIgnoreCase(groupBy)) {
			sql.append(" and cast(coalesce(a.semster,0) as varchar) = :groupValue ");
		} else if ("status".equalsIgnoreCase(groupBy)) {
			if ("Lunas".equalsIgnoreCase(groupValue)) {
				sql.append(" and coalesce(a.tagihan,0) > 0.1 and coalesce(a.dibayar,0) >= (coalesce(a.tagihan,0)-0.1) ");
			} else if ("Belum Bayar".equalsIgnoreCase(groupValue)) {
				sql.append(" and coalesce(a.tagihan,0) > 0.1 and coalesce(a.dibayar,0) <= 0.1 ");
			} else if ("Parsial".equalsIgnoreCase(groupValue)) {
				sql.append(" and coalesce(a.dibayar,0) > 0.1 and (coalesce(a.tagihan,0)-coalesce(a.dibayar,0)) > 0.1 ");
			} else if ("Lebih Bayar".equalsIgnoreCase(groupValue)) {
				sql.append(" and (coalesce(a.dibayar,0)-coalesce(a.tagihan,0)) > 0.1 ");
			} else if ("Tanpa Tagihan".equalsIgnoreCase(groupValue)) {
				sql.append(" and coalesce(a.tagihan,0) <= 0.1 and coalesce(a.dibayar,0) > 0.1 ");
			}
		}
	}

	private DashboardRow toDashboardRow(Object[] o) {
		DashboardRow row = new DashboardRow();
		row.kode = safe(o, 0);
		row.nama = safe(o, 1);
		row.jenis = safe(o, 2);
		row.dibayar = toDouble(o, 3);
		row.tagihan = toDouble(o, 4);
		row.sisa = row.tagihan - row.dibayar;
		row.tahunAkademik = safe(o, 5);
		row.semester = toInt(o, 6);
		row.fakultas = safe(o, 7);
		row.jurusan = safe(o, 8);
		row.status = getStatusPembayaran(row.tagihan, row.dibayar);
		return row;
	}

	private DashboardData buildDashboardData(List<DashboardRow> rows) {
		DashboardData d = new DashboardData();
		d.rows = rows == null ? new ArrayList<DashboardRow>() : rows;
		d.perJenis = new HashMap<String, SegmentRow>();
		d.perJurusan = new HashMap<String, SegmentRow>();
		d.perFakultas = new HashMap<String, SegmentRow>();
		d.perTahun = new HashMap<String, SegmentRow>();
		d.perSemester = new HashMap<String, SegmentRow>();
		d.perStatus = new HashMap<String, SegmentRow>();

		for (int i = 0; i < d.rows.size(); i++) {
			DashboardRow r = d.rows.get(i);
			d.totalTagihan += r.tagihan;
			d.totalDibayar += r.dibayar;
			d.totalPiutang += Math.max(0.0, r.sisa);
			d.totalLebihBayar += Math.max(0.0, r.dibayar - r.tagihan);
			d.jumlahData++;
			if (r.tagihan > 0.1) {
				d.jumlahTagihan++;
			}
			if (r.dibayar > 0.1) {
				d.jumlahDibayar++;
			}
			if (r.sisa > 0.1) {
				d.jumlahPiutang++;
			}
			if (r.tagihan > 0.1 && r.dibayar <= 0.1) {
				d.jumlahBelumBayar++;
			}
			if (r.dibayar > 0.1 && r.sisa > 0.1) {
				d.jumlahParsial++;
			}
			if (r.tagihan > 0.1 && r.dibayar >= (r.tagihan - 0.1)) {
				d.jumlahLunas++;
			}
			if ((r.dibayar - r.tagihan) > 0.1) {
				d.jumlahLebihBayar++;
			}

			accumulate(d.perJenis, normalizeLabel(r.jenis, "Lain-lain"), r);
			accumulate(d.perJurusan, normalizeLabel(r.jurusan, "Tanpa Prodi"), r);
			accumulate(d.perFakultas, normalizeLabel(r.fakultas, "Tanpa Fakultas"), r);
			accumulate(d.perTahun, normalizeLabel(r.tahunAkademik, "Tanpa TA"), r);
			accumulate(d.perSemester, String.valueOf(r.semester), r);
			accumulate(d.perStatus, r.status, r);
		}
		return d;
	}

	private void accumulate(Map<String, SegmentRow> map, String key, DashboardRow r) {
		SegmentRow s = map.get(key);
		if (s == null) {
			s = new SegmentRow();
			s.nama = key;
			map.put(key, s);
		}
		s.count++;
		s.tagihan += r.tagihan;
		s.dibayar += r.dibayar;
		s.piutang += Math.max(0.0, r.sisa);
		s.lebihBayar += Math.max(0.0, r.dibayar - r.tagihan);
		if (r.tagihan > 0.1 && r.dibayar <= 0.1) {
			s.belumBayar++;
		}
		if (r.dibayar > 0.1 && r.sisa > 0.1) {
			s.parsial++;
		}
		if (r.tagihan > 0.1 && r.dibayar >= (r.tagihan - 0.1)) {
			s.lunas++;
		}
	}

	private void renderHero(Component parent, DashboardData d) {
		Div hero = new Div();
		hero.setParent(parent);
		hero.setStyle("position:relative; overflow:hidden; border-radius:18px; padding:22px 24px; color:#ffffff;"
				+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);"
				+ "box-shadow:0 14px 30px rgba(15,23,42,.18); box-sizing:border-box;");

		appendHtml(hero,
				"<div style='position:absolute; right:-70px; top:-80px; width:230px; height:230px; border-radius:999px; background:rgba(255,255,255,.13);'></div>"
						+ "<div style='position:absolute; right:110px; bottom:-70px; width:160px; height:160px; border-radius:999px; background:rgba(255,255,255,.10);'></div>");

		Hbox content = new Hbox();
		content.setWidth("100%");
		content.setPack("justify");
		content.setAlign("center");
		content.setStyle("position:relative; z-index:1; gap:16px; flex-wrap:wrap;");
		content.setParent(hero);

		Vbox titleBox = new Vbox();
		titleBox.setStyle("max-width:760px;");
		titleBox.setParent(content);
		appendHtml(titleBox,
				"<div style='font-size:12px; text-transform:uppercase; letter-spacing:.12em; opacity:.88;'>Receivable Control Center</div>"
						+ "<div style='font-size:28px; line-height:1.15; font-weight:800; margin-top:6px;'>Dasbor Piutang, Pembayaran & Tagihan Mahasiswa</div>"
						+ "<div style='font-size:13px; opacity:.90; margin-top:8px;'>Pantau tagihan, pembayaran, sisa piutang, status lunas, pembayaran parsial, dan potensi lebih bayar. Klik angka untuk membuka popup detail data.</div>");

		FilterState fs = getCurrentFilterState();
		String fakultasText = fs.fakultas == null ? "Semua Fakultas" : fs.fakultas.getNama();
		String prodiText = fs.jurusan == null ? "Semua Prodi" : fs.jurusan.getNama();
		String jenisText = fs.jenisPembayaran == null ? "Semua Jenis Pembayaran" : fs.jenisPembayaran.getNama();
		appendHtml(titleBox,
				"<div style='margin-top:12px; display:flex; gap:8px; flex-wrap:wrap;'>"
						+ badge("TA: " + (fs.tahunAkademik == null ? "Semua" : fs.tahunAkademik))
						+ badge("Semester: " + (fs.semester == null ? "Semua" : fs.semester)) + badge(fakultasText)
						+ badge(prodiText) + badge(jenisText) + badge("Cari: " + (fs.keyword.length() == 0 ? "-" : fs.keyword))
						+ "</div>");

		Hbox numberBox = new Hbox();
		numberBox.setStyle("gap:10px; flex-wrap:wrap;");
		numberBox.setParent(content);
		createHeroNumber(numberBox, "Collection Ratio", percentText(d.totalDibayar, d.totalTagihan), "Detail pembayaran",
				createPopupListener("dibayar", null, null));
		createHeroNumber(numberBox, "Piutang Aktif", money(d.totalPiutang), "Detail piutang aktif",
				createPopupListener("piutang", null, null));
	}

	private String badge(String text) {
		return "<span style='padding:6px 10px; border-radius:999px; background:rgba(255,255,255,.16); color:#fff; font-size:11px; font-weight:700;'>"
				+ escape(text) + "</span>";
	}

	private void createHeroNumber(Component parent, String title, String value, String tooltip, EventListener listener) {
		Div box = new Div();
		box.setParent(parent);
		box.setStyle("min-width:150px; padding:13px 16px; border-radius:16px; background:rgba(255,255,255,.15); border:1px solid rgba(255,255,255,.23); text-align:right;");
		appendHtml(box, "<div style='font-size:11px; opacity:.86; font-weight:700;'>" + escape(title) + "</div>");
		A a = new A(value);
		a.setParent(box);
		a.setTooltiptext(tooltip);
		a.setStyle("display:block; color:#ffffff; text-decoration:none; font-size:23px; font-weight:900; margin-top:4px; cursor:pointer;");
		a.addEventListener("onClick", listener);
	}

	private void renderFilterSummary(Component parent) {
		Div box = new Div();
		box.setParent(parent);
		box.setStyle("padding:12px 14px; border-radius:16px; background:#ffffff; border:1px solid #e5e7eb; box-shadow:0 10px 24px rgba(15,23,42,.04);");
		appendHtml(box,
				"<div style='font-size:14px; font-weight:800; color:#0f172a;'>Overview dan Analitik Piutang</div>"
						+ "<div style='font-size:12px; color:#64748b; margin-top:4px;'>Ringkasan ini memakai filter di bagian atas. Semua angka utama, subtotal, dan nilai rincian dapat diklik untuk membuka popup detail.</div>");
	}

	private void renderMetricCards(Component parent, DashboardData d) {
		Div wrap = new Div();
		wrap.setParent(parent);
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		createMetricCard(wrap, "Total Tagihan", money(d.totalTagihan), d.jumlahTagihan + " data tagihan", "#dbeafe",
				"#1e40af", "Rp", createPopupListener("tagihan", null, null));
		createMetricCard(wrap, "Total Dibayar", money(d.totalDibayar), d.jumlahDibayar + " data pembayaran", "#dcfce7",
				"#166534", "✓", createPopupListener("dibayar", null, null));
		createMetricCard(wrap, "Piutang Aktif", money(d.totalPiutang), d.jumlahPiutang + " data piutang", "#fee2e2",
				"#991b1b", "!", createPopupListener("piutang", null, null));
		createMetricCard(wrap, "Lunas", formatInt(d.jumlahLunas), "Tagihan tertutup", "#fef3c7", "#92400e", "★",
				createPopupListener("lunas", null, null));
		createMetricCard(wrap, "Belum Bayar", formatInt(d.jumlahBelumBayar), "Belum ada pembayaran", "#ede9fe",
				"#5b21b6", "0", createPopupListener("belumbayar", null, null));
		createMetricCard(wrap, "Lebih Bayar", money(d.totalLebihBayar), d.jumlahLebihBayar + " data lebih bayar", "#cffafe",
				"#155e75", "+", createPopupListener("lebihbayar", null, null));
	}

	private void createMetricCard(Component parent, String title, String value, String desc, String bg, String color,
			String icon, EventListener listener) {
		Div card = new Div();
		card.setParent(parent);
		card.setStyle("flex:1 1 155px; min-width:155px; background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; padding:14px;"
				+ "box-shadow:0 10px 22px rgba(15,23,42,.06); box-sizing:border-box;");
		Hbox top = new Hbox();
		top.setParent(card);
		top.setWidth("100%");
		top.setPack("justify");
		top.setAlign("center");
		appendHtml(top,
				"<div style='width:38px; height:38px; border-radius:12px; display:flex; align-items:center; justify-content:center; font-weight:800; background:"
						+ bg + "; color:" + color + ";'>" + escape(icon) + "</div>");
		A a = new A(value);
		a.setParent(top);
		a.setStyle("font-size:24px; font-weight:800; color:#0f172a; text-decoration:none; cursor:pointer;");
		a.addEventListener("onClick", listener);
		appendHtml(card, "<div style='font-size:12px; color:#64748b; margin-top:10px;'>" + escape(title) + "</div>"
				+ "<div style='font-size:11px; color:#94a3b8; margin-top:3px;'>" + escape(desc) + "</div>");
	}

	private void renderFunnelPiutang(Component parent, DashboardData d) {
		Vbox panel = createModernPanel(parent, " Status Tagihan Mahasiswa",
				"Menunjukkan posisi data dari tagihan terbentuk sampai lunas. Klik angka untuk melihat detail mahasiswa/tagihan.");
		int max = Math.max(1, max(new int[] { d.jumlahTagihan, d.jumlahDibayar, d.jumlahParsial, d.jumlahPiutang,
				d.jumlahLunas, d.jumlahBelumBayar }));
		renderFunnelRow(panel, "Tagihan Terbentuk", d.jumlahTagihan, max, "#2563eb", createPopupListener("tagihan", null, null));
		renderFunnelRow(panel, "Sudah Ada Pembayaran", d.jumlahDibayar, max, "#16a34a", createPopupListener("dibayar", null, null));
		renderFunnelRow(panel, "Pembayaran Parsial", d.jumlahParsial, max, "#f59e0b", createPopupListener("parsial", null, null));
		renderFunnelRow(panel, "Masih Piutang", d.jumlahPiutang, max, "#dc2626", createPopupListener("piutang", null, null));
		renderFunnelRow(panel, "Lunas", d.jumlahLunas, max, "#0891b2", createPopupListener("lunas", null, null));
		renderFunnelRow(panel, "Belum Bayar", d.jumlahBelumBayar, max, "#7c3aed", createPopupListener("belumbayar", null, null));
	}

	private void renderFunnelRow(Component parent, String label, int value, int max, String color, EventListener listener) {
		Hbox row = new Hbox();
		row.setParent(parent);
		row.setWidth("100%");
		row.setAlign("center");
		row.setStyle("gap:10px; margin-bottom:8px;");
		appendHtml(row, "<div style='width:175px; font-size:12px; color:#334155; font-weight:700;'>" + escape(label) + "</div>");
		int width = Math.max(4, (int) Math.round((value * 100.0) / Math.max(1, max)));
		appendHtml(row, "<div style='flex:1; height:13px; border-radius:999px; background:#e5e7eb; overflow:hidden;'>"
				+ "<div style='width:" + width + "%; height:13px; border-radius:999px; background:" + color + ";'></div></div>");
		A a = new A(formatInt(value));
		a.setParent(row);
		a.setStyle("min-width:70px; text-align:right; font-size:13px; font-weight:800; color:#0f172a; text-decoration:none; cursor:pointer;");
		a.addEventListener("onClick", listener);
	}

	private void renderCollectionHealth(Component parent, DashboardData d) {
		Vbox panel = createModernPanel(parent, "Kesehatan Koleksi Pembayaran",
				"Rasio ini membantu melihat realisasi pembayaran dan tekanan piutang pada periode/filter yang dipilih.");
		renderGauge(panel, "Collection Ratio", percent(d.totalDibayar, d.totalTagihan), "Total dibayar dibanding total tagihan.",
				"#16a34a", createPopupListener("dibayar", null, null));
		renderGauge(panel, "Outstanding Ratio", percent(d.totalPiutang, d.totalTagihan), "Piutang aktif dibanding total tagihan.",
				"#dc2626", createPopupListener("piutang", null, null));
		renderGauge(panel, "Paid Count Ratio", percent(d.jumlahLunas, Math.max(1, d.jumlahTagihan)),
				"Jumlah tagihan lunas dibanding jumlah tagihan.", "#0891b2", createPopupListener("lunas", null, null));
		renderGauge(panel, "Unpaid Count Ratio", percent(d.jumlahBelumBayar, Math.max(1, d.jumlahTagihan)),
				"Jumlah tagihan belum bayar dibanding jumlah tagihan.", "#7c3aed", createPopupListener("belumbayar", null, null));
	}

	/**
	 * Radar/spider "kesehatan penagihan" — merangkum beberapa rasio penagihan dalam
	 * satu jaring laba-laba (HTML/CSS murni via DashboardUiKit, tanpa JFreeChart).
	 * Memakai DashboardData yang SUDAH dihitung (tanpa query/iterasi tambahan).
	 * Semua sumbu 0–100 dan "makin besar makin sehat" (Outstanding & Belum Bayar
	 * dibalik agar konsisten arah).
	 */
	private void renderCollectionRadar(Component parent, DashboardData d) {
		if (parent == null || d == null) {
			return;
		}
		Vbox panel = createModernPanel(parent, "Radar Kesehatan Penagihan",
				"Jaring laba-laba ringkas: makin lebar dan seimbang, makin sehat penagihan pada filter terpilih.");
		String[] label = new String[] { "Collection", "Bebas Piutang", "Lunas", "Ada Bayar", "Tertangani" };
		int[] nilai = new int[] {
				clampPersen(percent(d.totalDibayar, d.totalTagihan)),
				clampPersen(100 - percent(d.totalPiutang, d.totalTagihan)),
				clampPersen(percent(d.jumlahLunas, Math.max(1, d.jumlahTagihan))),
				clampPersen(percent(d.jumlahDibayar, Math.max(1, d.jumlahTagihan))),
				clampPersen(100 - percent(d.jumlahBelumBayar, Math.max(1, d.jumlahTagihan)))
		};
		appendHtml(panel, ais.ui.util.DashboardUiKit.spider("Kesehatan Penagihan",
				"Rangkuman rasio penagihan (terbayar, bebas piutang, lunas, sudah membayar, tertangani) dalam satu pandangan.",
				label, nilai));
	}

	/** Jepit nilai ke rentang 0–100 (aman untuk kasus lebih bayar / rasio > 100). */
	private int clampPersen(int v) {
		return v < 0 ? 0 : (v > 100 ? 100 : v);
	}

	private void renderGauge(Component parent, String title, int percent, String desc, String color, EventListener listener) {
		Div box = new Div();
		box.setParent(parent);
		box.setStyle("margin-bottom:10px; padding:10px; border:1px solid #eef2f7; border-radius:14px; background:#f8fafc;");
		Hbox top = new Hbox();
		top.setParent(box);
		top.setWidth("100%");
		top.setPack("justify");
		top.setAlign("center");
		appendHtml(top, "<div style='font-size:12px; font-weight:800; color:#0f172a;'>" + escape(title) + "</div>");
		A a = new A(percent + "%");
		a.setParent(top);
		a.setStyle("font-size:18px; font-weight:900; color:#0f172a; text-decoration:none; cursor:pointer;");
		a.addEventListener("onClick", listener);
		appendHtml(box, "<div style='height:10px; border-radius:999px; background:#e5e7eb; overflow:hidden; margin-top:8px;'>"
				+ "<div style='width:" + Math.min(100, Math.max(0, percent)) + "%; height:10px; border-radius:999px; background:" + color + ";'></div></div>"
				+ "<div style='font-size:11px; color:#64748b; margin-top:6px;'>" + escape(desc) + "</div>");
	}

	private void renderCharts(Component parent, DashboardData d) {
		MyGroupboxStyled boxChart = new MyGroupboxStyled();
		boxChart.setWidth("100%");
		boxChart.setParent(parent);
		boxChart.appendChild(new MyCaptionStyled("Grafik Distribusi Tagihan, Pembayaran, dan Piutang Per Jenis Pembayaran"));
		Vbox wrap = new Vbox();
		wrap.setParent(boxChart);
		wrap.setWidth("100%");
		appendHtml(wrap,
				"<div style='font-size:12px; color:#64748b; margin:4px 0 10px 0;'>Grafik memperlihatkan perbandingan nominal tagihan, dibayar, dan piutang aktif pada setiap jenis pembayaran.</div>");
		HtmlCategoryModel categoryModel = new HtmlCategoryModel();
		List<SegmentRow> rows = sortSegments(d.perJenis, "piutang");
		for (int i = 0; i < rows.size(); i++) {
			SegmentRow s = rows.get(i);
			categoryModel.setValue("Tagihan", s.nama, s.tagihan);
			categoryModel.setValue("Dibayar", s.nama, s.dibayar);
			categoryModel.setValue("Piutang", s.nama, s.piutang);
		}
		wrap.appendChild(new Html(buildModernChartHtml("Grafik Piutang Mahasiswa", categoryModel,
				"memperlihatkan perbandingan tagihan, pembayaran, dan piutang mahasiswa. Grafik memudahkan pengguna melihat kelompok yang perlu diprioritaskan penagihannya.")));
	}

	private void renderSegmentTables(Component parent, DashboardData d) {
		Hbox row1 = new Hbox();
		row1.setParent(parent);
		row1.setWidth("100%");
		row1.setStyle("gap:12px; align-items:stretch;");
		renderSegmentTable(row1, "Top Piutang Per Jenis Pembayaran", d.perJenis, "jenis", "piutang", true);
		renderSegmentTable(row1, "Top Piutang Per Prodi", d.perJurusan, "jurusan", "piutang", true);

		Hbox row2 = new Hbox();
		row2.setParent(parent);
		row2.setWidth("100%");
		row2.setStyle("gap:12px; align-items:stretch;");
		renderSegmentTable(row2, "Sebaran Per Fakultas", d.perFakultas, "fakultas", "piutang", true);
		renderSegmentTable(row2, "Status Risiko Pembayaran", d.perStatus, "status", "piutang", false);

		Hbox row3 = new Hbox();
		row3.setParent(parent);
		row3.setWidth("100%");
		row3.setStyle("gap:12px; align-items:stretch;");
		renderSegmentTable(row3, "Rekap Tahun Akademik", d.perTahun, "tahun", "piutang", false);
		renderSegmentTable(row3, "Rekap Semester", d.perSemester, "semester", "piutang", false);
	}

	private void renderSegmentTable(Component parent, String title, Map<String, SegmentRow> map, final String groupBy,
			String sortBy, boolean limit) {
		Vbox panel = createModernPanel(parent, title,
				"Klik angka pada tagihan, dibayar, piutang, lunas, atau belum bayar untuk melihat detail data.");
		panel.setStyle(panel.getStyle() + " min-width:360px;");
		Grid g = new Grid();
		g.setParent(panel);
		g.setWidth("100%");
		g.setSclass("dgrid");
		g.setMold("paging");
		g.setPageSize(SEGMENT_LIMIT);
		g.getPagingChild().setMold("os");
		Columns columns = new Columns();
		columns.setParent(g);
		new MyColumnConfig("Segment").setParent(columns);
		MyColumnConfig col = new MyColumnConfig("Tagihan");
		col.setAlign("right");
		col.setParent(columns);
		col = new MyColumnConfig("Dibayar");
		col.setAlign("right");
		col.setParent(columns);
		col = new MyColumnConfig("Piutang");
		col.setAlign("right");
		col.setParent(columns);
		col = new MyColumnConfig("Lunas");
		col.setAlign("right");
		col.setWidth("70px");
		col.setParent(columns);
		col = new MyColumnConfig("Belum");
		col.setAlign("right");
		col.setWidth("70px");
		col.setParent(columns);
		Rows rows = new Rows();
		rows.setParent(g);
		List<SegmentRow> segments = sortSegments(map, sortBy);
		int max = limit ? Math.min(SEGMENT_LIMIT, segments.size()) : segments.size();
		for (int i = 0; i < max; i++) {
			SegmentRow s = segments.get(i);
			final String groupValue = s.nama;
			MyFormRow r = new MyFormRow();
			r.setParent(rows);
			r.appendChild(new Label(s.nama));
			createMoneyLink(r, s.tagihan, "tagihan", groupBy, groupValue);
			createMoneyLink(r, s.dibayar, "dibayar", groupBy, groupValue);
			createMoneyLink(r, s.piutang, "piutang", groupBy, groupValue);
			createCountLink(r, s.lunas, "lunas", groupBy, groupValue);
			createCountLink(r, s.belumBayar, "belumbayar", groupBy, groupValue);
		}
	}

	private void renderMainGrid(Component parent, List<DashboardRow> objects, DashboardData data) {
		MyGroupboxStyled boxTabel = new MyGroupboxStyled();
		boxTabel.setWidth("100%");
		boxTabel.setParent(parent);
		boxTabel.appendChild(new MyCaptionStyled("Rincian Laporan Piutang Mahasiswa"));

		grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(boxTabel);
		grid.setMold("paging");
		grid.setPageSize(MAIN_GRID_PAGE_SIZE);
		grid.getPagingChild().setMold("os");

		Columns columns = new Columns();
		columns.setParent(grid);
		new MyColumnConfig("NIM/No.Reg").setParent(columns);
		new MyColumnConfig("Nama").setParent(columns);
		new MyColumnConfig("Fakultas").setParent(columns);
		new MyColumnConfig("Prodi").setParent(columns);
		new MyColumnConfig("TA").setParent(columns);
		new MyColumnConfig("Smt").setParent(columns);
		new MyColumnConfig("Jenis Pembayaran").setParent(columns);
		MyColumnConfig colDibayar = new MyColumnConfig("Dibayar");
		colDibayar.setAlign("right");
		colDibayar.setParent(columns);
		MyColumnConfig colTagihan = new MyColumnConfig("Tagihan");
		colTagihan.setAlign("right");
		colTagihan.setParent(columns);
		MyColumnConfig colSisa = new MyColumnConfig("Sisa");
		colSisa.setAlign("right");
		colSisa.setParent(columns);
		new MyColumnConfig("Status").setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		String kodeAkun = "";
		Double totalDibayarGroup = 0.0;
		Double totalTagihanGroup = 0.0;
		Double totalSisaGroup = 0.0;

		for (int i = 0; i < objects.size(); i++) {
			DashboardRow o = objects.get(i);
			if (!kodeAkun.equalsIgnoreCase(o.jenis)) {
				if (!kodeAkun.isEmpty()) {
					renderSubTotalRow(rows, kodeAkun, totalDibayarGroup, totalTagihanGroup, totalSisaGroup);
					totalDibayarGroup = 0.0;
					totalTagihanGroup = 0.0;
					totalSisaGroup = 0.0;
				}
				MyFormRow rowGroupHead = new MyFormRow();
				rowGroupHead.setParent(rows);
				rowGroupHead.setStyle("background-color:#e0ebf5;");
				rowGroupHead.appendChild(new Label());
				rowGroupHead.appendChild(new MyLabelBoldMerah(o.jenis));
				for (int j = 0; j < 9; j++) {
					rowGroupHead.appendChild(new Label());
				}
				kodeAkun = o.jenis;
			}

			totalDibayarGroup += o.dibayar;
			totalTagihanGroup += o.tagihan;
			totalSisaGroup += o.sisa;

			MyFormRow rowData = new MyFormRow();
			rowData.setParent(rows);
			rowData.appendChild(new Label(o.kode));
			rowData.appendChild(new Label(o.nama));
			rowData.appendChild(new Label(o.fakultas));
			rowData.appendChild(new Label(o.jurusan));
			rowData.appendChild(new Label(o.tahunAkademik));
			rowData.appendChild(new Label(formatInt(o.semester)));
			rowData.appendChild(new Label(o.jenis));
			createMoneyLink(rowData, o.dibayar, "dibayar", "jenis", o.jenis);
			createMoneyLink(rowData, o.tagihan, "tagihan", "jenis", o.jenis);
			createMoneyLink(rowData, o.sisa, o.sisa < -0.1 ? "lebihbayar" : "piutang", "jenis", o.jenis);
			rowData.appendChild(new Label(o.status));
		}

		if (!kodeAkun.isEmpty()) {
			renderSubTotalRow(rows, kodeAkun, totalDibayarGroup, totalTagihanGroup, totalSisaGroup);
		}

		Foot foot = new Foot();
		foot.setParent(grid);
		Footer ft1 = new Footer();
		ft1.setParent(foot);
		ft1.appendChild(new MyLabelBold("GRAND TOTAL"));
		for (int i = 0; i < 6; i++) {
			new Footer().setParent(foot);
		}
		Footer ftDibayar = new Footer();
		ftDibayar.setParent(foot);
		ftDibayar.setAlign("right");
		createMoneyLink(ftDibayar, data.totalDibayar, "dibayar", null, null);
		Footer ftTagihan = new Footer();
		ftTagihan.setParent(foot);
		ftTagihan.setAlign("right");
		createMoneyLink(ftTagihan, data.totalTagihan, "tagihan", null, null);
		Footer ftSisa = new Footer();
		ftSisa.setParent(foot);
		ftSisa.setAlign("right");
		createMoneyLink(ftSisa, data.totalPiutang, "piutang", null, null);
		new Footer().setParent(foot);
	}

	private void renderSubTotalRow(Rows rows, final String kodeAkun, Double totalDibayarGroup, Double totalTagihanGroup,
			Double totalSisaGroup) {
		MyFormRow rowTotal = new MyFormRow();
		rowTotal.setParent(rows);
		rowTotal.setStyle("background-color:#f2f2f2;");
		rowTotal.appendChild(new MyLabelBoldMerah("Total Sub"));
		rowTotal.appendChild(new MyLabelBoldMerah(kodeAkun));
		for (int i = 0; i < 5; i++) {
			rowTotal.appendChild(new Label());
		}
		createMoneyLink(rowTotal, totalDibayarGroup, "dibayar", "jenis", kodeAkun);
		createMoneyLink(rowTotal, totalTagihanGroup, "tagihan", "jenis", kodeAkun);
		createMoneyLink(rowTotal, totalSisaGroup, totalSisaGroup < -0.1 ? "lebihbayar" : "piutang", "jenis", kodeAkun);
		rowTotal.appendChild(new Label());
	}

	private Vbox createModernPanel(Component parent, String title, String subtitle) {
		MyGroupboxStyled box = new MyGroupboxStyled();
		box.setParent(parent);
		box.setWidth(parent instanceof Hbox ? "50%" : "100%");
		box.appendChild(new MyCaptionStyled(title));
		Vbox body = new Vbox();
		body.setParent(box);
		body.setWidth("100%");
		body.setStyle("padding:10px; box-sizing:border-box;");
		if (subtitle != null && subtitle.length() > 0) {
			appendHtml(body, "<div style='font-size:12px; color:#64748b; margin-bottom:10px; line-height:1.55;'>"
					+ escape(subtitle) + "</div>");
		}
		return body;
	}

	private void createMoneyLink(Component parent, double value, String type, String groupBy, String groupValue) {
		A a = new A(money(value));
		a.setParent(parent);
		a.setStyle("font-size:11px; font-weight:700; text-decoration:none; cursor:pointer; color:#1d4ed8;");
		a.addEventListener("onClick", createPopupListener(type, groupBy, groupValue));
	}

	private void createCountLink(Component parent, int value, String type, String groupBy, String groupValue) {
		A a = new A(formatInt(value));
		a.setParent(parent);
		a.setStyle("font-size:11px; font-weight:700; text-decoration:none; cursor:pointer; color:#1d4ed8;");
		a.addEventListener("onClick", createPopupListener(type, groupBy, groupValue));
	}

	private EventListener createPopupListener(final String type, final String groupBy, final String groupValue) {
		return new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showDetailPopup(buildPopupTitle(type, groupBy, groupValue), type, groupBy, groupValue);
			}
		};
	}

	private String buildPopupTitle(String type, String groupBy, String groupValue) {
		String title = "Detail Piutang Mahasiswa";
		if ("tagihan".equalsIgnoreCase(type)) {
			title = "Detail Tagihan Mahasiswa";
		} else if ("dibayar".equalsIgnoreCase(type)) {
			title = "Detail Pembayaran Mahasiswa";
		} else if ("piutang".equalsIgnoreCase(type)) {
			title = "Detail Piutang Aktif Mahasiswa";
		} else if ("lunas".equalsIgnoreCase(type)) {
			title = "Detail Tagihan Lunas";
		} else if ("belumbayar".equalsIgnoreCase(type)) {
			title = "Detail Tagihan Belum Bayar";
		} else if ("parsial".equalsIgnoreCase(type)) {
			title = "Detail Pembayaran Parsial";
		} else if ("lebihbayar".equalsIgnoreCase(type)) {
			title = "Detail Lebih Bayar";
		}
		if (groupBy != null && groupValue != null) {
			title += " - " + groupValue;
		}
		return title;
	}

	private void showDetailPopup(String title, String type, String groupBy, String groupValue) throws Exception {
		final Window win = new Window(title, "normal", true);
		win.setWidth("92%");
		win.setHeight("82%");
		win.setClosable(true);
		win.setSizable(true);
		win.setPosition("center,center");
		win.setParent(this);

		Vbox body = new Vbox();
		body.setParent(win);
		body.setWidth("100%");
		body.setHeight("100%");
		body.setStyle("padding:12px; overflow:auto; box-sizing:border-box; background:#f8fafc;");

		appendHtml(body, "<div style='padding:10px 12px; margin-bottom:8px; border-radius:12px; background:#eff6ff; border:1px solid #bfdbfe; color:#1e3a8a; font-size:12px;'>"
				+ "Data detail mengikuti filter yang sedang dipakai. Klik <b>Download Excel</b> untuk mengambil data yang tampil di popup.</div>");

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			List<DashboardRow> rows = loadDashboardRows(session, type, groupBy, groupValue);
			DashboardData data = buildDashboardData(rows);

			Div summary = new Div();
			summary.setParent(body);
			summary.setStyle("display:flex; gap:10px; flex-wrap:wrap; margin-bottom:10px;");
			renderSmallSummary(summary, "Data", formatInt(data.jumlahData));
			renderSmallSummary(summary, "Tagihan", money(data.totalTagihan));
			renderSmallSummary(summary, "Dibayar", money(data.totalDibayar));
			renderSmallSummary(summary, "Piutang", money(data.totalPiutang));

			final Grid detailGrid = new Grid();
			detailGrid.setParent(body);
			detailGrid.setWidth("100%");
			detailGrid.setSclass("dgrid");
			detailGrid.setMold("paging");
			detailGrid.setPageSize(POPUP_PAGE_SIZE);
			detailGrid.getPagingChild().setMold("os");

			Columns columns = new Columns();
			columns.setParent(detailGrid);
			new MyColumnConfig("NIM/No.Reg").setParent(columns);
			new MyColumnConfig("Nama").setParent(columns);
			new MyColumnConfig("Fakultas").setParent(columns);
			new MyColumnConfig("Prodi").setParent(columns);
			new MyColumnConfig("TA").setParent(columns);
			new MyColumnConfig("Smt").setParent(columns);
			new MyColumnConfig("Jenis Pembayaran").setParent(columns);
			MyColumnConfig col = new MyColumnConfig("Tagihan");
			col.setAlign("right");
			col.setParent(columns);
			col = new MyColumnConfig("Dibayar");
			col.setAlign("right");
			col.setParent(columns);
			col = new MyColumnConfig("Sisa");
			col.setAlign("right");
			col.setParent(columns);
			new MyColumnConfig("Status").setParent(columns);

			Rows gridRows = new Rows();
			gridRows.setParent(detailGrid);
			for (int i = 0; i < rows.size(); i++) {
				DashboardRow r = rows.get(i);
				MyFormRow row = new MyFormRow();
				row.setParent(gridRows);
				row.appendChild(new Label(r.kode));
				row.appendChild(new Label(r.nama));
				row.appendChild(new Label(r.fakultas));
				row.appendChild(new Label(r.jurusan));
				row.appendChild(new Label(r.tahunAkademik));
				row.appendChild(new Label(formatInt(r.semester)));
				row.appendChild(new Label(r.jenis));
				row.appendChild(new Label(money(r.tagihan)));
				row.appendChild(new Label(money(r.dibayar)));
				row.appendChild(new Label(money(r.sisa)));
				row.appendChild(new Label(r.status));
			}

			Foot foot = new Foot();
			foot.setParent(detailGrid);
			Footer ft = new Footer();
			ft.setParent(foot);
			ft.appendChild(new MyLabelBold("TOTAL"));
			for (int i = 0; i < 6; i++) {
				new Footer().setParent(foot);
			}
			ft = new Footer();
			ft.setParent(foot);
			ft.setAlign("right");
			ft.appendChild(new MyLabelBoldMerah(money(data.totalTagihan)));
			ft = new Footer();
			ft.setParent(foot);
			ft.setAlign("right");
			ft.appendChild(new MyLabelBoldMerah(money(data.totalDibayar)));
			ft = new Footer();
			ft.setParent(foot);
			ft.setAlign("right");
			ft.appendChild(new MyLabelBoldMerah(money(data.totalPiutang)));
			new Footer().setParent(foot);

			Hbox toolbar = new Hbox();
			toolbar.setParent(body);
			toolbar.setStyle("margin-top:10px; gap:8px;");
			MyToolbarbuttonConfig download = new MyToolbarbuttonConfig("Download Excel", "/img/excel.png");
			download.setParent(toolbar);
			download.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					UIUtil.downloadGrid(detailGrid);
				}
			});
			MyToolbarbuttonConfig close = new MyToolbarbuttonConfig("Tutup", "/img/close.png");
			close.setParent(toolbar);
			close.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					win.detach();
				}
			});
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/keuangan/DasboardPiutang.java:1108");
			Common.tampilErrorJikaAdmin(e);
		} finally {
			cleanupSession(session);
		}
		win.doModal();
	}

	private void renderSmallSummary(Component parent, String title, String value) {
		Div card = new Div();
		card.setParent(parent);
		card.setStyle("min-width:135px; padding:10px 12px; border-radius:14px; border:1px solid #e5e7eb; background:#ffffff; box-shadow:0 8px 16px rgba(15,23,42,.05);");
		appendHtml(card, "<div style='font-size:11px; color:#64748b; font-weight:700;'>" + escape(title) + "</div>"
				+ "<div style='font-size:17px; color:#0f172a; font-weight:900; margin-top:3px;'>" + escape(value) + "</div>");
	}

	private List<SegmentRow> sortSegments(Map<String, SegmentRow> map, final String sortBy) {
		List<SegmentRow> list = new ArrayList<SegmentRow>();
		if (map != null) {
			list.addAll(map.values());
		}
		Collections.sort(list, new Comparator<SegmentRow>() {
			@Override
			public int compare(SegmentRow a, SegmentRow b) {
				double va = getSortValue(a, sortBy);
				double vb = getSortValue(b, sortBy);
				if (vb > va)
					return 1;
				if (vb < va)
					return -1;
				return a.nama.compareToIgnoreCase(b.nama);
			}
		});
		return list;
	}

	private double getSortValue(SegmentRow row, String sortBy) {
		if ("tagihan".equalsIgnoreCase(sortBy))
			return row.tagihan;
		if ("dibayar".equalsIgnoreCase(sortBy))
			return row.dibayar;
		if ("count".equalsIgnoreCase(sortBy))
			return row.count;
		return row.piutang;
	}

	private String getStatusPembayaran(double tagihan, double dibayar) {
		double sisa = tagihan - dibayar;
		if (tagihan <= 0.1 && dibayar > 0.1) {
			return "Tanpa Tagihan";
		}
		if ((dibayar - tagihan) > 0.1) {
			return "Lebih Bayar";
		}
		if (tagihan > 0.1 && dibayar <= 0.1) {
			return "Belum Bayar";
		}
		if (tagihan > 0.1 && sisa > 0.1) {
			return "Parsial";
		}
		if (tagihan > 0.1 && dibayar >= (tagihan - 0.1)) {
			return "Lunas";
		}
		return "Data Pembayaran";
	}

	private String normalizeLabel(String value, String def) {
		if (value == null || value.trim().length() == 0) {
			return def;
		}
		return value.trim();
	}

	private int max(int[] values) {
		int result = 0;
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				if (values[i] > result)
					result = values[i];
			}
		}
		return result;
	}

	private int percent(double value, double total) {
		if (total <= 0.0)
			return 0;
		return (int) Math.round((value * 100.0) / total);
	}

	private String percentText(double value, double total) {
		return percent(value, total) + "%";
	}

	private String safe(Object[] o, int idx) {
		if (o == null || idx >= o.length || o[idx] == null)
			return "";
		return String.valueOf(o[idx]);
	}

	private double toDouble(Object[] o, int idx) {
		if (o == null || idx >= o.length || o[idx] == null)
			return 0.0;
		if (o[idx] instanceof Number) {
			return ((Number) o[idx]).doubleValue();
		}
		try {
			return Double.parseDouble(String.valueOf(o[idx]));
		} catch (Exception e) {
			return 0.0;
		}
	}

	private int toInt(Object[] o, int idx) {
		if (o == null || idx >= o.length || o[idx] == null)
			return 0;
		if (o[idx] instanceof Number) {
			return ((Number) o[idx]).intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o[idx]));
		} catch (Exception e) {
			return 0;
		}
	}

	private String money(double value) {
		String text = Common.numberFormat.get().format(Math.abs(value));
		return value < 0 ? "(" + text + ")" : text;
	}

	private String formatInt(int value) {
		return Common.numberFormat.get().format(value);
	}

	private void appendHtml(Component parent, String html) {
		Html h = new Html(html);
		h.setParent(parent);
	}

	private String escape(String value) {
		if (value == null)
			return "";
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	private static class FilterState {
		String tahunAkademik;
		String semester;
		Fakultas fakultas;
		Jurusan jurusan;
		JenisKegiatan jenisPembayaran;
		String keyword;
	}

	private static class DashboardRow {
		String kode;
		String nama;
		String jenis;
		double dibayar;
		double tagihan;
		double sisa;
		String tahunAkademik;
		int semester;
		String fakultas;
		String jurusan;
		String status;
	}

	private static class DashboardData {
		List<DashboardRow> rows = new ArrayList<DashboardRow>();
		int jumlahData;
		int jumlahTagihan;
		int jumlahDibayar;
		int jumlahPiutang;
		int jumlahLunas;
		int jumlahBelumBayar;
		int jumlahParsial;
		int jumlahLebihBayar;
		double totalTagihan;
		double totalDibayar;
		double totalPiutang;
		double totalLebihBayar;
		Map<String, SegmentRow> perJenis;
		Map<String, SegmentRow> perJurusan;
		Map<String, SegmentRow> perFakultas;
		Map<String, SegmentRow> perTahun;
		Map<String, SegmentRow> perSemester;
		Map<String, SegmentRow> perStatus;
	}

	private static class SegmentRow {
		String nama;
		int count;
		int lunas;
		int belumBayar;
		int parsial;
		double tagihan;
		double dibayar;
		double piutang;
		double lebihBayar;
	}

	private static class HtmlCategoryModel {
		private List<HtmlCategoryRow> rows = new ArrayList<HtmlCategoryRow>();

		public void clear() {
			rows.clear();
		}

		public void setValue(String series, Object category, Object value) {
			HtmlCategoryRow row = new HtmlCategoryRow();
			row.series = series == null ? "" : series;
			row.category = category == null ? "" : String.valueOf(category);
			row.value = toDoubleDashboardValue(value);
			rows.add(row);
		}

		public List<HtmlCategoryRow> getRows() {
			return rows;
		}
	}

	private static class HtmlCategoryRow {
		String series;
		String category;
		double value;
	}

	private String buildModernChartHtml(String title, HtmlCategoryModel model, String description) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='width:100%;box-sizing:border-box;padding:14px;border:1px solid #e2e8f0;border-radius:16px;background:#ffffff;box-shadow:0 8px 18px rgba(15,23,42,.06);'>");
		sb.append("<div style='font-size:14px;font-weight:900;color:#0f172a;margin-bottom:6px;'>").append(escapeDashboardHtml(title)).append("</div>");
		if (description != null && description.trim().length() > 0) {
			sb.append("<div style='font-size:11px;color:#64748b;line-height:1.55;margin-bottom:10px;'>").append(escapeDashboardHtml(description)).append("</div>");
		}
		if (model == null || model.getRows() == null || model.getRows().isEmpty()) {
			sb.append("<div style='padding:12px;border-radius:12px;background:#f8fafc;color:#64748b;font-size:12px;'>Belum ada data yang dapat ditampilkan.</div></div>");
			return sb.toString();
		}
		double max = 0.0d;
		for (int i = 0; i < model.getRows().size(); i++) {
			HtmlCategoryRow r = (HtmlCategoryRow) model.getRows().get(i);
			if (r != null && r.value > max) {
				max = r.value;
			}
		}
		if (max <= 0.0d) {
			max = 1.0d;
		}
		sb.append("<div style='display:flex;flex-direction:column;gap:7px;'>");
		for (int i = 0; i < model.getRows().size(); i++) {
			HtmlCategoryRow r = (HtmlCategoryRow) model.getRows().get(i);
			if (r == null) {
				continue;
			}
			int width = (int) Math.round((r.value * 100.0d) / max);
			if (width < 2 && r.value > 0.0d) {
				width = 2;
			}
			sb.append("<div style='display:grid;grid-template-columns:minmax(95px,210px) 1fr minmax(70px,120px);gap:8px;align-items:center;'>");
			sb.append("<div style='font-size:11px;color:#334155;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;'>").append(escapeDashboardHtml(r.category)).append("</div>");
			sb.append("<div style='height:14px;border-radius:999px;background:#e2e8f0;overflow:hidden;'><div style='height:14px;width:").append(width)
					.append("%;border-radius:999px;background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));'></div></div>");
			sb.append("<div style='font-size:11px;color:#0f172a;font-weight:900;text-align:right;'>").append(formatDashboardNumber(r.value)).append("</div>");
			sb.append("</div>");
		}
		sb.append("</div></div>");
		return sb.toString();
	}

	private static double toDoubleDashboardValue(Object value) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		try {
			return value == null ? 0.0d : Double.parseDouble(String.valueOf(value));
		} catch (Exception e) {
			return 0.0d;
		}
	}

	private static String formatDashboardNumber(double value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(Math.round(value));
		}
	}

	private static String escapeDashboardHtml(Object value) {
		String text = value == null ? "" : String.valueOf(value);
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}


}
