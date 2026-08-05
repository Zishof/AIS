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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.json.JSONObject;
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
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.maintenance.MainAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldMerah;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

/**
 * Menampilkan rincian piutang mahasiswa agar data tunggakan bisa diperiksa sampai level mahasiswa dan jenis biaya.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardPiutangRInci extends MyWindow {

	private static final long serialVersionUID = 3557603220165512688L;
	private Div center;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox jenisPembayaran = new Combobox();
	private MyTextbox nama;
	private Grid grid;
	private Combobox comboTampilkan;
	private Paging paging;
	private int jumlahDataDalamSatuHalamanElearning;

	private static final int POPUP_PAGE_SIZE = 10;
	private static final int SEGMENT_LIMIT = 15;

	public DasboardPiutangRInci() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DasboardPiutangRInci(String title, String border, boolean closable) {
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
				"Kartu Piutang Rinci",
				"Rincian piutang (tagihan belum terbayar) per mahasiswa, beserta grafiknya.");
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
				reload(true);
			}
		};

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "1,1,1,3");

		comboTampilkan = new Combobox();
		Integer[] dataCombo = new Integer[] { 10, 30, 50, 100, 300, 500, 1000 };
		for (Integer d : dataCombo) {
			comboitem = new MyComboitemConfig(d + " tampilan");
			comboitem.setValue(d);
			comboTampilkan.appendChild(comboitem);
		}
		comboTampilkan.setReadonly(true);
		Common.selectComboItem(comboTampilkan, 10);
		comboTampilkan.setParent(row);
		comboTampilkan.setCols(7);

		MyToolbarbuttonConfig tampilkan = new MyToolbarbuttonConfig("Tampilkan Dasbor", "/img/print.png");
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

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "6");

		jumlahDataDalamSatuHalamanElearning = 10;
		paging = new Paging();
		paging.setMold("os");
		paging.setParent(row);
		Common.initPagingCustom(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				reload(false);
			}
		}, jumlahDataDalamSatuHalamanElearning);

		Common.createDefaultTimer(eventListener);
	}

	@SuppressWarnings({ })
	private void reload(boolean hitungUlangPaging) {
		Common.clear(center);

		Vbox mainVbox = new Vbox();
		mainVbox.setParent(center);
		mainVbox.setWidth("100%");
		mainVbox.setSpacing("14px");
		mainVbox.setStyle("padding:12px; background:#f6f8fb; box-sizing:border-box;");

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			List<DashboardRow> allRows = loadDashboardRows(session, null, null, null);
			DashboardData data = buildDashboardData(allRows);

			if (hitungUlangPaging) {
				jumlahDataDalamSatuHalamanElearning = (Integer) comboTampilkan.getSelectedItem().getValue();
				paging.setActivePage(0);
				paging.setPageSize(jumlahDataDalamSatuHalamanElearning);
				paging.setMold("os");
				paging.setTotalSize(data.jumlahData);
				paging.setDetailed(false);
				paging.getParent().setVisible(data.jumlahData > jumlahDataDalamSatuHalamanElearning);
			} else {
				jumlahDataDalamSatuHalamanElearning = (Integer) comboTampilkan.getSelectedItem().getValue();
				paging.setPageSize(jumlahDataDalamSatuHalamanElearning);
				paging.setTotalSize(data.jumlahData);
				paging.getParent().setVisible(data.jumlahData > jumlahDataDalamSatuHalamanElearning);
			}

			renderHero(mainVbox, data);
			renderFilterSummary(mainVbox);
			renderMetricCards(mainVbox, data);
			ais.ui.util.DashboardJurnalPembayaranUtil.renderJurnalMahasiswaPanel(mainVbox, "Ringkasan Jurnal Pembayaran Mahasiswa",
					"Menunjukkan akun-akun yang muncul dari rincian piutang dan pembayaran mahasiswa. Data ini membantu memeriksa kesiapan jurnal sebelum ditindaklanjuti.");

			if (allRows == null || allRows.isEmpty()) {
				Div empty = new Div();
				empty.setParent(mainVbox);
				empty.setStyle("padding:16px; border:1px solid #fde68a; background:#fffbeb; color:#92400e; border-radius:14px; font-weight:bold;");
				empty.appendChild(new Label("Tidak ada rincian piutang/tagihan mahasiswa yang sesuai dengan filter."));
				return;
			}

			renderFunnelRincian(mainVbox, data);
			renderCollectionHealth(mainVbox, data);
			renderCharts(mainVbox, data);
			renderSegmentTables(mainVbox, data);
			renderKartuPiutangMahasiswa(mainVbox, data);
			renderAgingDanPrioritasPenagihan(mainVbox, data);
			renderHeatmapMahasiswaItem(mainVbox, data);
			renderRekomendasiTindakLanjut(mainVbox, data);

			int mulai = jumlahDataDalamSatuHalamanElearning * (paging == null ? 0 : paging.getActivePage());
			int sampai = Math.min(allRows.size(), mulai + jumlahDataDalamSatuHalamanElearning);
			List<DashboardRow> pageRows = new ArrayList<DashboardRow>();
			for (int i = mulai; i < sampai; i++) {
				pageRows.add(allRows.get(i));
			}
			renderMainGrid(mainVbox, pageRows, data);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/keuangan/DasboardPiutangRInci.java:332");
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
		sql.append(" a.tagihans as tagihans, a.bulans as bulans, ");
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

		sql.append(" order by d.nama_kegiatan, coalesce(c.nama, b.nama, ''), coalesce(c.nim, b.no_registrasi, '') ");

		Query query = session.createSQLQuery(sql.toString()).setString("buktilike", "%" + fs.keyword + "%");
		if (fs.tahunAkademik != null) {
			query.setString("tahunAkademik", fs.tahunAkademik);
		}

		List<Object[]> raw = query.list();
		List<DashboardRow> rows = new ArrayList<DashboardRow>();
		Map<Long, String> itemCache = new HashMap<Long, String>();
		if (raw != null) {
			for (int i = 0; i < raw.size(); i++) {
				parseKegiatanRow(raw.get(i), rows, itemCache, detailType, groupBy, groupValue);
			}
		}
		Collections.sort(rows, new Comparator<DashboardRow>() {
			@Override
			public int compare(DashboardRow o1, DashboardRow o2) {
				int c = safe(o1.jenis).compareToIgnoreCase(safe(o2.jenis));
				if (c != 0)
					return c;
				c = safe(o1.nama).compareToIgnoreCase(safe(o2.nama));
				if (c != 0)
					return c;
				c = safe(o1.itemBiaya).compareToIgnoreCase(safe(o2.itemBiaya));
				if (c != 0)
					return c;
				return safe(o1.bulanText).compareToIgnoreCase(safe(o2.bulanText));
			}
		});
		return rows;
	}

	private void parseKegiatanRow(Object[] o, List<DashboardRow> rows, Map<Long, String> itemCache, String detailType,
			String groupBy, String groupValue) {
		try {
			String kode = o[0] == null ? "" : o[0].toString();
			String namaMhs = o[1] == null ? "" : o[1].toString();
			String jenis = o[2] == null ? "Lain-lain" : o[2].toString();
			Number totalDibayar = o[3] == null ? 0.0 : (Number) o[3];
			Number totalTagihan = o[4] == null ? 0.0 : (Number) o[4];
			String tagihansD = o[5] == null ? "" : o[5].toString();
			String bulansD = o[6] == null ? "" : o[6].toString();
			String tahun = o[7] == null ? "" : o[7].toString();
			Number semester = o[8] == null ? 0.0 : (Number) o[8];
			String fakultas = o[9] == null ? "Tanpa Fakultas" : o[9].toString();
			String jurusan = o[10] == null ? "Tanpa Prodi" : o[10].toString();

			JSONObject tagihans = null;
			JSONObject dibayars = null;
			try {
				if (tagihansD != null && tagihansD.trim().length() > 0) {
					tagihans = new JSONObject(tagihansD);
				}
			} catch (Exception e) {
				tagihans = null;
			}
			try {
				if (bulansD != null && bulansD.trim().length() > 0) {
					dibayars = new JSONObject(bulansD);
				}
			} catch (Exception e) {
				dibayars = null;
			}

			boolean hasDetail = false;
			if (tagihans != null) {
				Iterator<String> iterator = tagihans.keys();
				while (iterator.hasNext()) {
					try {
						String key = iterator.next();
						Object val = tagihans.get(key);
						String[] keyParts = key.split("_");
						Long idItem = Long.parseLong(keyParts[0].trim());
						Integer bulan = keyParts.length < 2 ? null : Integer.valueOf(Integer.parseInt(keyParts[1].trim()));
						if (bulan != null && bulan.intValue() == 0) {
							bulan = null;
						}
						Double tagihan = Double.valueOf(Double.parseDouble(val + ""));
						Double dibayar = findDibayar(dibayars, idItem, bulan);

						if (Math.abs(tagihan.doubleValue()) > 0.001 || Math.abs(dibayar.doubleValue()) > 0.001) {
							hasDetail = true;
							DashboardRow r = buildRow(kode, namaMhs, fakultas, jurusan, jenis, getItemName(idItem, itemCache),
									bulan, tahun, semester.intValue(), tagihan.doubleValue(), dibayar.doubleValue());
							if (includeRow(r, detailType, groupBy, groupValue)) {
								rows.add(r);
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DasboardPiutangRInci.java:485");
					}
				}
			}

			if (!hasDetail) {
				DashboardRow r = buildRow(kode, namaMhs, fakultas, jurusan, jenis, "Total Tagihan", null, tahun,
						semester.intValue(), totalTagihan.doubleValue(), totalDibayar.doubleValue());
				if (includeRow(r, detailType, groupBy, groupValue)) {
					rows.add(r);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DasboardPiutangRInci.java:497");
		}
	}

	private Double findDibayar(JSONObject dibayars, Long idItem, Integer bulan) {
		Double hasil = Double.valueOf(0.0);
		if (dibayars == null || idItem == null) {
			return hasil;
		}
		try {
			Iterator<String> iteratorDibayar = dibayars.keys();
			while (iteratorDibayar.hasNext()) {
				try {
					String key = iteratorDibayar.next();
					Object val = dibayars.get(key);
					String[] parts = key.split("_");
					Long idItemV = Long.parseLong(parts[0].trim());
					Integer bulanV = parts.length < 2 ? null : Integer.valueOf(Integer.parseInt(parts[1].trim()));
					if (bulanV != null && bulanV.intValue() == 0) {
						bulanV = null;
					}
					if (idItem.equals(idItemV) && ((bulan == null && bulanV == null)
							|| (bulan != null && bulanV != null && bulan.equals(bulanV)))) {
						hasil = Double.valueOf(Double.parseDouble(val + ""));
						break;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DasboardPiutangRInci.java:523");
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DasboardPiutangRInci.java:526");
		}
		return hasil;
	}

	private String getItemName(Long idItem, Map<Long, String> itemCache) {
		if (idItem == null) {
			return "Lainnya";
		}
		if (itemCache.containsKey(idItem)) {
			return itemCache.get(idItem);
		}
		String name = "Item " + idItem;
		try {
			ItemBiaya itemBiaya = (ItemBiaya) ConstantValues.ambil(ItemBiaya.class.getName(), idItem);
			if (itemBiaya != null && itemBiaya.getNama() != null) {
				name = itemBiaya.getNama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DasboardPiutangRInci.java:544");
		}
		itemCache.put(idItem, name);
		return name;
	}

	private DashboardRow buildRow(String kode, String namaMhs, String fakultas, String jurusan, String jenis, String item,
			Integer bulan, String tahun, int semester, double tagihan, double dibayar) {
		DashboardRow r = new DashboardRow();
		r.kode = kode;
		r.nama = namaMhs;
		r.mahasiswaKey = buildMahasiswaKey(kode, namaMhs);
		r.fakultas = fakultas;
		r.jurusan = jurusan;
		r.jenis = jenis;
		r.itemBiaya = item;
		r.bulan = bulan;
		r.bulanText = bulan == null ? "-" : formatInt(bulan.intValue());
		r.tahunAkademik = tahun;
		r.semester = semester;
		r.tagihan = tagihan;
		r.dibayar = dibayar;
		r.sisa = tagihan - dibayar;
		r.status = getStatus(r);
		r.agingGroup = getAgingGroup(r);
		return r;
	}

	private boolean includeRow(DashboardRow r, String detailType, String groupBy, String groupValue) {
		if (r == null) {
			return false;
		}
		if (!matchDetailType(r, detailType)) {
			return false;
		}
		if (groupBy == null || groupBy.trim().length() == 0 || groupValue == null) {
			return true;
		}
		String key = "";
		if ("jenis".equalsIgnoreCase(groupBy)) {
			key = r.jenis;
		} else if ("item".equalsIgnoreCase(groupBy)) {
			key = r.itemBiaya;
		} else if ("bulan".equalsIgnoreCase(groupBy)) {
			key = r.bulanText;
		} else if ("prodi".equalsIgnoreCase(groupBy)) {
			key = r.jurusan;
		} else if ("fakultas".equalsIgnoreCase(groupBy)) {
			key = r.fakultas;
		} else if ("tahun".equalsIgnoreCase(groupBy)) {
			key = r.tahunAkademik;
		} else if ("semester".equalsIgnoreCase(groupBy)) {
			key = formatInt(r.semester);
		} else if ("status".equalsIgnoreCase(groupBy)) {
			key = r.status;
		} else if ("mahasiswa".equalsIgnoreCase(groupBy)) {
			key = r.mahasiswaKey;
		} else if ("aging".equalsIgnoreCase(groupBy)) {
			key = r.agingGroup;
		} else if ("mahasiswaItem".equalsIgnoreCase(groupBy)) {
			key = buildMahasiswaItemKey(r.mahasiswaKey, r.itemBiaya);
		}
		return safe(key).equalsIgnoreCase(safe(groupValue));
	}

	private boolean matchDetailType(DashboardRow r, String detailType) {
		if (detailType == null || detailType.trim().length() == 0 || "data".equalsIgnoreCase(detailType)) {
			return true;
		}
		if ("tagihan".equalsIgnoreCase(detailType)) {
			return r.tagihan > 0.1;
		}
		if ("dibayar".equalsIgnoreCase(detailType)) {
			return r.dibayar > 0.1;
		}
		if ("piutang".equalsIgnoreCase(detailType)) {
			return r.sisa > 0.1;
		}
		if ("lunas".equalsIgnoreCase(detailType)) {
			return r.tagihan > 0.1 && Math.abs(r.sisa) <= 0.1;
		}
		if ("belumbayar".equalsIgnoreCase(detailType)) {
			return r.tagihan > 0.1 && r.dibayar <= 0.1;
		}
		if ("parsial".equalsIgnoreCase(detailType)) {
			return r.tagihan > 0.1 && r.dibayar > 0.1 && r.sisa > 0.1;
		}
		if ("lebihbayar".equalsIgnoreCase(detailType)) {
			return r.sisa < -0.1;
		}
		return true;
	}

	private String getStatus(DashboardRow r) {
		if (r == null) {
			return "Tidak Diketahui";
		}
		if (r.sisa < -0.1) {
			return "Lebih Bayar";
		}
		if (r.tagihan > 0.1 && Math.abs(r.sisa) <= 0.1) {
			return "Lunas";
		}
		if (r.tagihan > 0.1 && r.dibayar <= 0.1) {
			return "Belum Bayar";
		}
		if (r.tagihan > 0.1 && r.dibayar > 0.1 && r.sisa > 0.1) {
			return "Parsial";
		}
		if (r.sisa > 0.1) {
			return "Piutang";
		}
		return "Data";
	}

	private DashboardData buildDashboardData(List<DashboardRow> rows) {
		DashboardData d = new DashboardData();
		if (rows == null) {
			return d;
		}
		for (int i = 0; i < rows.size(); i++) {
			DashboardRow r = rows.get(i);
			if (r == null) {
				continue;
			}
			d.jumlahData++;
			d.totalTagihan += r.tagihan;
			d.totalDibayar += r.dibayar;
			d.totalPiutang += Math.max(0.0, r.sisa);
			if (r.sisa < -0.1) {
				d.totalLebihBayar += Math.abs(r.sisa);
			}
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
			if (r.tagihan > 0.1 && r.dibayar > 0.1 && r.sisa > 0.1) {
				d.jumlahParsial++;
			}
			if (r.tagihan > 0.1 && Math.abs(r.sisa) <= 0.1) {
				d.jumlahLunas++;
			}
			if (r.sisa < -0.1) {
				d.jumlahLebihBayar++;
			}
			addBucket(d.byJenis, r.jenis, r);
			addBucket(d.byItem, r.itemBiaya, r);
			addBucket(d.byBulan, r.bulanText, r);
			addBucket(d.byProdi, r.jurusan, r);
			addBucket(d.byFakultas, r.fakultas, r);
			addBucket(d.byTahun, r.tahunAkademik, r);
			addBucket(d.bySemester, formatInt(r.semester), r);
			addBucket(d.byStatus, r.status, r);
			addBucket(d.byMahasiswa, r.mahasiswaKey, r);
			addBucket(d.byAging, r.agingGroup, r);
			addBucket(d.byMahasiswaItem, buildMahasiswaItemKey(r.mahasiswaKey, r.itemBiaya), r);
		}
		return d;
	}

	private void addBucket(Map<String, Bucket> map, String key, DashboardRow r) {
		key = safe(key).trim().length() == 0 ? "Tidak diketahui" : key;
		Bucket b = map.get(key);
		if (b == null) {
			b = new Bucket();
			b.nama = key;
			map.put(key, b);
		}
		b.count++;
		b.tagihan += r.tagihan;
		b.dibayar += r.dibayar;
		b.piutang += Math.max(0.0, r.sisa);
		b.sisa += r.sisa;
		if ("Belum Bayar".equalsIgnoreCase(r.status)) {
			b.belumBayar++;
		} else if ("Parsial".equalsIgnoreCase(r.status)) {
			b.parsial++;
		} else if ("Lunas".equalsIgnoreCase(r.status)) {
			b.lunas++;
		} else if ("Lebih Bayar".equalsIgnoreCase(r.status)) {
			b.lebihBayar++;
		}
	}


	private String buildMahasiswaKey(String kode, String namaMhs) {
		String k = safe(kode).trim();
		String n = safe(namaMhs).trim();
		if (k.length() == 0 && n.length() == 0) {
			return "Mahasiswa Tidak Diketahui";
		}
		if (k.length() == 0) {
			return n;
		}
		if (n.length() == 0) {
			return k;
		}
		return k + " - " + n;
	}

	private String buildMahasiswaItemKey(String mahasiswaKey, String itemBiaya) {
		return safe(mahasiswaKey) + "||" + safe(itemBiaya);
	}

	private String prettyGroupValue(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("||", " / ");
	}

	private String getAgingGroup(DashboardRow r) {
		if (r == null || r.sisa <= 0.1) {
			return "Tidak Ada Piutang";
		}
		if (r.bulan == null) {
			return "Non Bulanan / Sekali Bayar";
		}
		int b = r.bulan.intValue();
		if (b <= 2) {
			return "Bulan 1-2";
		}
		if (b <= 4) {
			return "Bulan 3-4";
		}
		if (b <= 6) {
			return "Bulan 5-6";
		}
		return "Bulan > 6";
	}

	private void renderHero(Component parent, DashboardData d) {
		Div hero = new Div();
		hero.setParent(parent);
		hero.setStyle("position:relative; overflow:hidden; border-radius:18px; padding:22px 24px; color:#ffffff;"
				+ "background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);"
				+ "box-shadow:0 14px 30px rgba(15,23,42,.18); box-sizing:border-box;");
		appendHtml(hero,
				"<div style='position:absolute; right:-60px; top:-70px; width:210px; height:210px; border-radius:999px; background:rgba(255,255,255,.12);'></div>"
						+ "<div style='position:absolute; right:90px; bottom:-70px; width:160px; height:160px; border-radius:999px; background:rgba(255,255,255,.10);'></div>");

		Hbox content = new Hbox();
		content.setParent(hero);
		content.setWidth("100%");
		content.setPack("justify");
		content.setAlign("center");
		content.setStyle("position:relative; z-index:1; gap:16px; flex-wrap:wrap;");

		Vbox titleBox = new Vbox();
		titleBox.setParent(content);
		titleBox.setStyle("max-width:760px;");
		appendHtml(titleBox,
				"<div style='font-size:12px; text-transform:uppercase; letter-spacing:.12em; opacity:.88;'>Receivable Detail Control Center</div>"
						+ "<div style='font-size:28px; line-height:1.15; font-weight:800; margin-top:6px;'>Dasbor Rincian Piutang Mahasiswa</div>"
						+ "<div style='font-size:13px; opacity:.90; margin-top:8px;'>Pantau tagihan, pembayaran, sisa piutang, rincian item biaya, bulan tagihan, dan status pelunasan dalam satu layar. Klik angka untuk melihat detail data.</div>");

		FilterState fs = getCurrentFilterState();
		appendHtml(titleBox,
				"<div style='margin-top:12px; display:flex; gap:8px; flex-wrap:wrap;'>" + badge("TA: "
						+ (fs.tahunAkademik == null ? "Semua" : fs.tahunAkademik))
						+ badge("Semester: " + (fs.semester == null ? "Semua" : fs.semester))
						+ badge("Fakultas: " + (fs.fakultas == null ? "Semua" : fs.fakultas.getNama()))
						+ badge("Prodi: " + (fs.jurusan == null ? "Semua" : fs.jurusan.getNama()))
						+ badge("Keyword: " + (fs.keyword == null || fs.keyword.length() == 0 ? "-" : fs.keyword))
						+ "</div>");

		Hbox numberBox = new Hbox();
		numberBox.setParent(content);
		numberBox.setStyle("gap:10px; flex-wrap:wrap;");
		createHeroNumber(numberBox, "Total Item", formatInt(d.jumlahData), "Klik untuk detail semua item",
				createPopupListener("data", null, null));
		createHeroNumber(numberBox, "Piutang Aktif", money(d.totalPiutang), "Klik untuk detail piutang aktif",
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
				"<div style='font-size:14px; font-weight:800; color:#0f172a;'>Overview dan Analitik Rincian Piutang</div>"
						+ "<div style='font-size:12px; color:#64748b; margin-top:4px;'>Dashboard memakai filter di bagian atas. Data rinci dibaca dari JSON tagihan/pembayaran per item biaya dan bulan. Semua angka utama, subtotal, dan nilai rincian dapat diklik untuk membuka popup detail.</div>");
	}

	private void renderMetricCards(Component parent, DashboardData d) {
		Div wrap = new Div();
		wrap.setParent(parent);
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		createMetricCard(wrap, "Total Tagihan", money(d.totalTagihan), d.jumlahTagihan + " item bertagihan", "#dbeafe",
				"#1e40af", "Rp", createPopupListener("tagihan", null, null));
		createMetricCard(wrap, "Total Dibayar", money(d.totalDibayar), d.jumlahDibayar + " item sudah dibayar", "#dcfce7",
				"#166534", "✓", createPopupListener("dibayar", null, null));
		createMetricCard(wrap, "Piutang Aktif", money(d.totalPiutang), d.jumlahPiutang + " item masih piutang", "#fee2e2",
				"#991b1b", "!", createPopupListener("piutang", null, null));
		createMetricCard(wrap, "Lunas", formatInt(d.jumlahLunas), "Item tagihan tertutup", "#fef3c7", "#92400e", "★",
				createPopupListener("lunas", null, null));
		createMetricCard(wrap, "Belum Bayar", formatInt(d.jumlahBelumBayar), "Item belum ada pembayaran", "#ede9fe",
				"#5b21b6", "0", createPopupListener("belumbayar", null, null));
		createMetricCard(wrap, "Lebih Bayar", money(d.totalLebihBayar), d.jumlahLebihBayar + " item lebih bayar", "#cffafe",
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

	private void renderFunnelRincian(Component parent, DashboardData d) {
		Vbox panel = createModernPanel(parent, " Status Rincian Tagihan",
				"Menunjukkan posisi item tagihan dari terbentuk, dibayar, parsial, piutang, sampai lunas. Klik angka untuk melihat detail item biaya.");
		int max = Math.max(1, max(new int[] { d.jumlahTagihan, d.jumlahDibayar, d.jumlahParsial, d.jumlahPiutang,
				d.jumlahLunas, d.jumlahBelumBayar }));
		renderFunnelRow(panel, "Item Tagihan Terbentuk", d.jumlahTagihan, max, "#2563eb", createPopupListener("tagihan", null, null));
		renderFunnelRow(panel, "Item Sudah Dibayar", d.jumlahDibayar, max, "#16a34a", createPopupListener("dibayar", null, null));
		renderFunnelRow(panel, "Item Pembayaran Parsial", d.jumlahParsial, max, "#f59e0b", createPopupListener("parsial", null, null));
		renderFunnelRow(panel, "Item Masih Piutang", d.jumlahPiutang, max, "#dc2626", createPopupListener("piutang", null, null));
		renderFunnelRow(panel, "Item Lunas", d.jumlahLunas, max, "#0891b2", createPopupListener("lunas", null, null));
		renderFunnelRow(panel, "Item Belum Bayar", d.jumlahBelumBayar, max, "#7c3aed", createPopupListener("belumbayar", null, null));
	}

	private void renderFunnelRow(Component parent, String label, int value, int max, String color, EventListener listener) {
		Hbox row = new Hbox();
		row.setParent(parent);
		row.setWidth("100%");
		row.setAlign("center");
		row.setStyle("gap:10px; margin-bottom:8px;");
		appendHtml(row, "<div style='width:190px; font-size:12px; color:#334155; font-weight:700;'>" + escape(label) + "</div>");
		int width = Math.max(4, (int) Math.round((value * 100.0) / Math.max(1, max)));
		appendHtml(row, "<div style='flex:1; height:13px; border-radius:999px; background:#e5e7eb; overflow:hidden;'>"
				+ "<div style='width:" + width + "%; height:13px; border-radius:999px; background:" + color + ";'></div></div>");
		A a = new A(formatInt(value));
		a.setParent(row);
		a.setStyle("min-width:70px; text-align:right; font-size:13px; font-weight:800; color:#0f172a; text-decoration:none; cursor:pointer;");
		a.addEventListener("onClick", listener);
	}

	private void renderCollectionHealth(Component parent, DashboardData d) {
		Vbox panel = createModernPanel(parent, "Kesehatan Koleksi Rincian Pembayaran",
				"Rasio ini membantu membaca realisasi pembayaran, tekanan piutang, dan risiko item yang belum terbayar.");
		renderGauge(panel, "Collection Ratio", percent(d.totalDibayar, d.totalTagihan), "Total dibayar dibanding total tagihan.",
				"#16a34a", createPopupListener("dibayar", null, null));
		renderGauge(panel, "Outstanding Ratio", percent(d.totalPiutang, d.totalTagihan), "Sisa piutang aktif dibanding total tagihan.",
				"#dc2626", createPopupListener("piutang", null, null));
		renderGauge(panel, "Item Lunas Ratio", percent(d.jumlahLunas, Math.max(1, d.jumlahTagihan)), "Proporsi item yang sudah lunas.",
				"#0891b2", createPopupListener("lunas", null, null));
		renderGauge(panel, "Unpaid Item Ratio", percent(d.jumlahBelumBayar, Math.max(1, d.jumlahTagihan)), "Proporsi item yang belum dibayar sama sekali.",
				"#7c3aed", createPopupListener("belumbayar", null, null));
	}

	private void renderGauge(Component parent, String label, int percent, String desc, String color, EventListener listener) {
		Div box = new Div();
		box.setParent(parent);
		box.setStyle("margin-bottom:10px; padding:10px; border-radius:12px; border:1px solid #e5e7eb; background:#f8fafc;");
		Hbox top = new Hbox();
		top.setParent(box);
		top.setWidth("100%");
		top.setPack("justify");
		appendHtml(top, "<div style='font-size:12px; color:#334155; font-weight:800;'>" + escape(label) + "</div>");
		A a = new A(percent + "%");
		a.setParent(top);
		a.setStyle("font-size:13px; font-weight:900; color:" + color + "; text-decoration:none; cursor:pointer;");
		a.addEventListener("onClick", listener);
		appendHtml(box, "<div style='height:12px; border-radius:999px; background:#e5e7eb; overflow:hidden; margin-top:8px;'>"
				+ "<div style='height:12px; width:" + Math.min(100, Math.max(0, percent)) + "%; background:" + color
				+ "; border-radius:999px;'></div></div>" + "<div style='font-size:11px; color:#64748b; margin-top:6px;'>"
				+ escape(desc) + "</div>");
	}

	private void renderCharts(Component parent, DashboardData d) {
		Div wrap = new Div();
		wrap.setParent(parent);
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		renderChartBox(wrap, "Distribusi Rincian per Item Biaya", d.byItem, "item");
		renderChartBox(wrap, "Distribusi Rincian per Jenis Pembayaran", d.byJenis, "jenis");
	}

	private void renderChartBox(Component parent, String title, Map<String, Bucket> data, String groupBy) {
		Vbox panel = createModernPanel(parent, title,
				"Grafik nilai tagihan, dibayar, dan piutang. Klik angka pada tabel di bawah untuk detail data.");
		panel.setStyle(panel.getStyle() + " min-width:320px; flex:1 1 420px;");
		HtmlCategoryModel categoryModel = new HtmlCategoryModel();
		List<Bucket> buckets = sortBuckets(data, "piutang");
		int limit = Math.min(10, buckets.size());
		for (int i = 0; i < limit; i++) {
			Bucket b = buckets.get(i);
			categoryModel.setValue("Tagihan", b.nama, Double.valueOf(b.tagihan));
			categoryModel.setValue("Dibayar", b.nama, Double.valueOf(b.dibayar));
			categoryModel.setValue("Piutang", b.nama, Double.valueOf(b.piutang));
		}
		panel.appendChild(new Html(buildModernChartHtml("Grafik Piutang Rinci Mahasiswa", categoryModel,
				"memperlihatkan perbandingan nominal tagihan, pembayaran, dan piutang berdasarkan filter yang dipilih. Gunakan grafik ini untuk mengetahui kelompok dengan piutang terbesar.")));
	}

	private void renderSegmentTables(Component parent, DashboardData d) {
		Div wrap = new Div();
		wrap.setParent(parent);
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		renderBucketTable(wrap, "Top Piutang per Item Biaya", d.byItem, "item", "piutang");
		renderBucketTable(wrap, "Top Piutang per Prodi", d.byProdi, "prodi", "piutang");
		renderBucketTable(wrap, "Sebaran per Jenis Pembayaran", d.byJenis, "jenis", "tagihan");
		renderBucketTable(wrap, "Sebaran per Bulan Tagihan", d.byBulan, "bulan", "tagihan");
		renderBucketTable(wrap, "Status Risiko Pembayaran", d.byStatus, "status", "piutang");
		renderBucketTable(wrap, "Rekap per Tahun Akademik", d.byTahun, "tahun", "tagihan");
		renderBucketTable(wrap, "Rekap per Semester", d.bySemester, "semester", "tagihan");
		renderBucketTable(wrap, "Sebaran per Fakultas", d.byFakultas, "fakultas", "piutang");
	}

	private void renderBucketTable(Component parent, String title, Map<String, Bucket> data, final String groupBy,
			String sortBy) {
		Vbox panel = createModernPanel(parent, title,
				"Klik angka tagihan, dibayar, atau piutang untuk membuka rincian mahasiswa pada kelompok tersebut.");
		panel.setStyle(panel.getStyle() + " min-width:360px; flex:1 1 460px;");
		Grid g = new Grid();
		g.setParent(panel);
		g.setWidth("100%");
		g.setSclass("dgrid");
		Columns cols = new Columns();
		cols.setParent(g);
		new MyColumnConfig("Kelompok").setParent(cols);
		MyColumnConfig col = new MyColumnConfig("Item");
		col.setAlign("right");
		col.setParent(cols);
		col = new MyColumnConfig("Tagihan");
		col.setAlign("right");
		col.setParent(cols);
		col = new MyColumnConfig("Dibayar");
		col.setAlign("right");
		col.setParent(cols);
		col = new MyColumnConfig("Piutang");
		col.setAlign("right");
		col.setParent(cols);
		Rows rows = new Rows();
		rows.setParent(g);
		List<Bucket> buckets = sortBuckets(data, sortBy);
		int limit = Math.min(SEGMENT_LIMIT, buckets.size());
		for (int i = 0; i < limit; i++) {
			Bucket b = buckets.get(i);
			MyFormRow r = new MyFormRow();
			r.setParent(rows);
			r.appendChild(new Label(b.nama));
			appendDetailLink(r, formatInt(b.count), "data", groupBy, b.nama);
			appendDetailLink(r, money(b.tagihan), "tagihan", groupBy, b.nama);
			appendDetailLink(r, money(b.dibayar), "dibayar", groupBy, b.nama);
			appendDetailLink(r, money(b.piutang), "piutang", groupBy, b.nama);
		}
	}


	private void renderKartuPiutangMahasiswa(Component parent, DashboardData d) {
		Div wrap = new Div();
		wrap.setParent(parent);
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		renderBucketTable(wrap, "Kartu Piutang Mahasiswa - Saldo Terbesar", d.byMahasiswa, "mahasiswa", "piutang");
		renderStudentRiskTable(wrap, "Prioritas Penagihan Mahasiswa", d.byMahasiswa);
	}

	private void renderStudentRiskTable(Component parent, String title, Map<String, Bucket> data) {
		Vbox panel = createModernPanel(parent, title,
				"Daftar mahasiswa dengan sisa piutang terbesar, item belum bayar, dan item parsial. Dipakai sebagai watchlist penagihan/klarifikasi kartu piutang.");
		panel.setStyle(panel.getStyle() + " min-width:420px; flex:1 1 560px;");
		Grid g = new Grid();
		g.setParent(panel);
		g.setWidth("100%");
		g.setSclass("dgrid");
		Columns cols = new Columns();
		cols.setParent(g);
		new MyColumnConfig("Mahasiswa").setParent(cols);
		MyColumnConfig col = new MyColumnConfig("Item");
		col.setAlign("right");
		col.setParent(cols);
		col = new MyColumnConfig("Belum");
		col.setAlign("right");
		col.setParent(cols);
		col = new MyColumnConfig("Parsial");
		col.setAlign("right");
		col.setParent(cols);
		col = new MyColumnConfig("Piutang");
		col.setAlign("right");
		col.setParent(cols);
		col = new MyColumnConfig("Tertagih");
		col.setAlign("right");
		col.setParent(cols);
		Rows rows = new Rows();
		rows.setParent(g);
		List<Bucket> buckets = sortBuckets(data, "piutang");
		int limit = Math.min(SEGMENT_LIMIT, buckets.size());
		for (int i = 0; i < limit; i++) {
			Bucket b = buckets.get(i);
			MyFormRow r = new MyFormRow();
			r.setParent(rows);
			r.appendChild(new Label(b.nama));
			appendDetailLink(r, formatInt(b.count), "data", "mahasiswa", b.nama);
			appendDetailLink(r, formatInt(b.belumBayar), "belumbayar", "mahasiswa", b.nama);
			appendDetailLink(r, formatInt(b.parsial), "parsial", "mahasiswa", b.nama);
			appendDetailLink(r, money(b.piutang), "piutang", "mahasiswa", b.nama);
			appendDetailLink(r, percent(b.dibayar, b.tagihan) + "%", "dibayar", "mahasiswa", b.nama);
		}
	}

	private void renderAgingDanPrioritasPenagihan(Component parent, DashboardData d) {
		Div wrap = new Div();
		wrap.setParent(parent);
		wrap.setStyle("display:flex; gap:12px; flex-wrap:wrap;");
		renderBucketTable(wrap, "Aging Piutang Berdasarkan Bulan Tagihan", d.byAging, "aging", "piutang");
		renderAgingNarrative(wrap, d);
	}

	private void renderAgingNarrative(Component parent, DashboardData d) {
		Vbox panel = createModernPanel(parent, "Ringkasan Aksi Kartu Piutang",
				"memberi arahan cepat untuk follow-up kartu piutang mahasiswa berdasarkan status pembayaran dan bulan tagihan.");
		panel.setStyle(panel.getStyle() + " min-width:360px; flex:1 1 460px;");
		Bucket belum = d.byStatus.get("Belum Bayar");
		Bucket parsial = d.byStatus.get("Parsial");
		Bucket lebih = d.byStatus.get("Lebih Bayar");
		renderActionRow(panel, "Tagihan belum dibayar", belum == null ? 0 : belum.count, belum == null ? 0.0 : belum.piutang,
				"Prioritaskan reminder, validasi invoice, dan cek status mahasiswa.", "belumbayar", "status", "Belum Bayar");
		renderActionRow(panel, "Pembayaran parsial", parsial == null ? 0 : parsial.count, parsial == null ? 0.0 : parsial.piutang,
				"Cek termin cicilan dan komunikasikan sisa yang harus diselesaikan.", "parsial", "status", "Parsial");
		renderActionRow(panel, "Potensi lebih bayar", lebih == null ? 0 : lebih.count, lebih == null ? Math.abs(d.totalLebihBayar) : Math.abs(lebih.sisa),
				"Perlu rekonsiliasi/refund/kompensasi ke tagihan lain bila sesuai kebijakan.", "lebihbayar", "status", "Lebih Bayar");
	}

	private void renderActionRow(Component parent, String label, int count, double amount, String desc, String type,
			String groupBy, String groupValue) {
		Div box = new Div();
		box.setParent(parent);
		box.setStyle("padding:10px; border-radius:12px; border:1px solid #e5e7eb; background:#f8fafc; margin-bottom:8px;");
		Hbox top = new Hbox();
		top.setParent(box);
		top.setWidth("100%");
		top.setPack("justify");
		appendHtml(top, "<div style='font-size:12px; color:#334155; font-weight:800;'>" + escape(label) + "</div>");
		A a = new A(formatInt(count) + " / " + money(amount));
		a.setParent(top);
		a.setStyle("font-size:12px; font-weight:900; color:#1d4ed8; text-decoration:none; cursor:pointer;");
		a.addEventListener("onClick", createPopupListener(type, groupBy, groupValue));
		appendHtml(box, "<div style='font-size:11px; color:#64748b; margin-top:5px; line-height:1.45;'>" + escape(desc) + "</div>");
	}

	private void renderHeatmapMahasiswaItem(Component parent, DashboardData d) {
		Vbox panel = createModernPanel(parent, "Heatmap Kartu Piutang Mahasiswa x Item Biaya",
				"Matriks ini menampilkan top mahasiswa dan top item biaya berdasarkan sisa piutang. Klik nilai pada cell untuk membuka detail mahasiswa dan item biaya terkait.");
		Grid g = new Grid();
		g.setParent(panel);
		g.setWidth("100%");
		g.setSclass("dgrid");
		Columns cols = new Columns();
		cols.setParent(g);
		new MyColumnConfig("Mahasiswa").setParent(cols);
		List<Bucket> topItems = sortBuckets(d.byItem, "piutang");
		List<Bucket> topMahasiswa = sortBuckets(d.byMahasiswa, "piutang");
		int itemLimit = Math.min(Common.isMobile() ? 3 : 6, topItems.size());
		int mahasiswaLimit = Math.min(10, topMahasiswa.size());
		for (int i = 0; i < itemLimit; i++) {
			MyColumnConfig col = new MyColumnConfig(shortText(topItems.get(i).nama, 18));
			col.setAlign("right");
			col.setParent(cols);
		}
		MyColumnConfig totalCol = new MyColumnConfig("Total");
		totalCol.setAlign("right");
		totalCol.setParent(cols);
		Rows rows = new Rows();
		rows.setParent(g);
		for (int r = 0; r < mahasiswaLimit; r++) {
			Bucket mahasiswa = topMahasiswa.get(r);
			MyFormRow row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new Label(shortText(mahasiswa.nama, 32)));
			for (int c = 0; c < itemLimit; c++) {
				Bucket item = topItems.get(c);
				String key = buildMahasiswaItemKey(mahasiswa.nama, item.nama);
				Bucket cell = d.byMahasiswaItem.get(key);
				double nilai = cell == null ? 0.0 : cell.piutang;
				appendDetailLink(row, nilai > 0.1 ? money(nilai) : "-", "piutang", "mahasiswaItem", key);
			}
			appendDetailLink(row, money(mahasiswa.piutang), "piutang", "mahasiswa", mahasiswa.nama);
		}
	}

	private void renderRekomendasiTindakLanjut(Component parent, DashboardData d) {
		Vbox panel = createModernPanel(parent, "Rekomendasi Tindak Lanjut Kartu Piutang Mahasiswa",
				"Checklist operasional untuk bagian keuangan agar kartu piutang mahasiswa lebih mudah ditindaklanjuti.");
		int outstanding = percent(d.totalPiutang, d.totalTagihan);
		String level = outstanding >= 50 ? "Tinggi" : (outstanding >= 25 ? "Sedang" : "Terkendali");
		appendHtml(panel, "<div style='display:flex; gap:10px; flex-wrap:wrap;'>"
				+ "<div style='flex:1 1 180px; padding:12px; border-radius:14px; background:#fff7ed; border:1px solid #fed7aa;'>"
				+ "<div style='font-size:11px; color:#9a3412; font-weight:800;'>Level Risiko Outstanding</div>"
				+ "<div style='font-size:22px; color:#7c2d12; font-weight:900; margin-top:4px;'>" + escape(level) + "</div>"
				+ "<div style='font-size:11px; color:#9a3412; margin-top:4px;'>Outstanding ratio " + outstanding + "% dari total tagihan.</div></div>"
				+ "<div style='flex:2 1 360px; padding:12px; border-radius:14px; background:#f8fafc; border:1px solid #e5e7eb; color:#334155; font-size:12px; line-height:1.65;'>"
				+ "<b>Prioritas:</b> 1) follow-up mahasiswa dengan saldo terbesar, 2) validasi item belum bayar, 3) rekonsiliasi lebih bayar, "
				+ "4) review item biaya yang paling sering menjadi piutang, 5) download popup detail untuk bahan penagihan.</div></div>");
	}

	private String shortText(String text, int max) {
		String s = safe(text);
		if (s.length() <= max) {
			return s;
		}
		return s.substring(0, Math.max(0, max - 3)) + "...";
	}

	private void renderMainGrid(Component parent, List<DashboardRow> rows, DashboardData data) {
		Vbox panel = createModernPanel(parent, "Rincian Laporan Piutang Mahasiswa per Item Biaya",
				"Grid utama mengikuti paging OS di bagian filter. Angka pada kolom pembayaran/tagihan/sisa dapat diklik untuk melihat popup detail dengan filter yang sama.");

		grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(panel);
		grid.setMold("default");

		Columns columns = new Columns();
		columns.setParent(grid);
		new MyColumnConfig("NIM/No.Reg").setParent(columns);
		new MyColumnConfig("Nama").setParent(columns);
		new MyColumnConfig("Fakultas").setParent(columns);
		new MyColumnConfig("Prodi").setParent(columns);
		new MyColumnConfig("Jenis Pembayaran").setParent(columns);
		new MyColumnConfig("Item Biaya").setParent(columns);
		new MyColumnConfig("Bulan").setParent(columns);
		new MyColumnConfig("TA").setParent(columns);
		new MyColumnConfig("Smt").setParent(columns);
		MyColumnConfig col = new MyColumnConfig("Dibayar");
		col.setAlign("right");
		col.setParent(columns);
		col = new MyColumnConfig("Tagihan");
		col.setAlign("right");
		col.setParent(columns);
		col = new MyColumnConfig("Sisa");
		col.setAlign("right");
		col.setParent(columns);
		new MyColumnConfig("Status").setParent(columns);

		Rows gridRows = new Rows();
		gridRows.setParent(grid);
		for (int i = 0; rows != null && i < rows.size(); i++) {
			DashboardRow r = rows.get(i);
			MyFormRow row = new MyFormRow();
			row.setParent(gridRows);
			row.appendChild(new Label(r.kode));
			row.appendChild(new Label(r.nama));
			row.appendChild(new Label(r.fakultas));
			row.appendChild(new Label(r.jurusan));
			row.appendChild(new Label(r.jenis));
			row.appendChild(new Label(r.itemBiaya));
			row.appendChild(new Label(r.bulanText));
			row.appendChild(new Label(r.tahunAkademik));
			row.appendChild(new Label(formatInt(r.semester)));
			appendDetailLink(row, money(r.dibayar), "dibayar", "item", r.itemBiaya);
			appendDetailLink(row, money(r.tagihan), "tagihan", "item", r.itemBiaya);
			appendDetailLink(row, money(Math.max(0.0, r.sisa)), "piutang", "item", r.itemBiaya);
			row.appendChild(new Label(r.status));
		}

		Foot foot = new Foot();
		foot.setParent(grid);
		Footer ft = new Footer();
		ft.setParent(foot);
		ft.appendChild(new MyLabelBold("GRAND TOTAL"));
		for (int i = 0; i < 8; i++) {
			new Footer().setParent(foot);
		}
		ft = new Footer();
		ft.setParent(foot);
		ft.setAlign("right");
		ft.appendChild(new MyLabelBoldMerah(money(data.totalDibayar)));
		ft = new Footer();
		ft.setParent(foot);
		ft.setAlign("right");
		ft.appendChild(new MyLabelBoldMerah(money(data.totalTagihan)));
		ft = new Footer();
		ft.setParent(foot);
		ft.setAlign("right");
		ft.appendChild(new MyLabelBoldMerah(money(data.totalPiutang)));
		new Footer().setParent(foot);
	}

	private void appendDetailLink(Component parent, String text, String type, String groupBy, String groupValue) {
		A a = new A(text);
		a.setParent(parent);
		a.setTooltiptext("Klik untuk melihat detail data");
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
		String title = "Detail Rincian Piutang Mahasiswa";
		if ("tagihan".equalsIgnoreCase(type)) {
			title = "Detail Item Tagihan Mahasiswa";
		} else if ("dibayar".equalsIgnoreCase(type)) {
			title = "Detail Item Pembayaran Mahasiswa";
		} else if ("piutang".equalsIgnoreCase(type)) {
			title = "Detail Item Piutang Aktif Mahasiswa";
		} else if ("lunas".equalsIgnoreCase(type)) {
			title = "Detail Item Tagihan Lunas";
		} else if ("belumbayar".equalsIgnoreCase(type)) {
			title = "Detail Item Belum Bayar";
		} else if ("parsial".equalsIgnoreCase(type)) {
			title = "Detail Item Pembayaran Parsial";
		} else if ("lebihbayar".equalsIgnoreCase(type)) {
			title = "Detail Item Lebih Bayar";
		}
		if (groupBy != null && groupValue != null) {
			title += " - " + prettyGroupValue(groupValue);
		}
		return title;
	}

	private void showDetailPopup(String title, String type, String groupBy, String groupValue) throws Exception {
		final Window win = new Window();
		win.setTitle(title);
		win.setBorder("normal");
		win.setWidth(Common.isMobile() ? "96%" : "92%");
		win.setHeight(Common.isMobile() ? "92%" : "82%");
		win.setClosable(true);
		win.setSizable(true);
		win.setPosition("center,center");
		win.setParent(this);

		Vbox body = new Vbox();
		body.setParent(win);
		body.setWidth("100%");
		body.setHeight("100%");
		body.setStyle("padding:12px; overflow:auto; box-sizing:border-box; background:#f8fafc;");

		appendHtml(body,
				"<div style='padding:10px 12px; margin-bottom:8px; border-radius:12px; background:#eff6ff; border:1px solid #bfdbfe; color:#1e3a8a; font-size:12px;'>"
						+ "Data detail mengikuti filter yang sedang dipakai. Klik <b>Download Excel</b> untuk mengambil data yang tampil di popup.</div>");

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			List<DashboardRow> rows = loadDashboardRows(session, type, groupBy, groupValue);
			DashboardData data = buildDashboardData(rows);

			Div summary = new Div();
			summary.setParent(body);
			summary.setStyle("display:flex; gap:10px; flex-wrap:wrap; margin-bottom:10px;");
			renderSmallSummary(summary, "Item", formatInt(data.jumlahData));
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
			new MyColumnConfig("Jenis Pembayaran").setParent(columns);
			new MyColumnConfig("Item Biaya").setParent(columns);
			new MyColumnConfig("Bulan").setParent(columns);
			new MyColumnConfig("TA").setParent(columns);
			new MyColumnConfig("Smt").setParent(columns);
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
				row.appendChild(new Label(r.jenis));
				row.appendChild(new Label(r.itemBiaya));
				row.appendChild(new Label(r.bulanText));
				row.appendChild(new Label(r.tahunAkademik));
				row.appendChild(new Label(formatInt(r.semester)));
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
			for (int i = 0; i < 8; i++) {
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

			MyToolbarbuttonConfig download = new MyToolbarbuttonConfig("Download Excel", "/img/excel.png");
			download.setParent(body);
			download.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					UIUtil.downloadGrid(detailGrid);
				}
			});
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/keuangan/DasboardPiutangRInci.java:1421");
			Common.tampilErrorJikaAdmin(e);
		} finally {
			cleanupSession(session);
		}
		win.doModal();
	}

	private void renderSmallSummary(Component parent, String label, String value) {
		Div box = new Div();
		box.setParent(parent);
		box.setStyle("min-width:120px; padding:10px 12px; border-radius:12px; background:#ffffff; border:1px solid #e5e7eb; box-shadow:0 8px 18px rgba(15,23,42,.04);");
		appendHtml(box, "<div style='font-size:11px; color:#64748b; font-weight:700;'>" + escape(label) + "</div>"
				+ "<div style='font-size:16px; color:#0f172a; font-weight:900; margin-top:3px;'>" + escape(value)
				+ "</div>");
	}

	private Vbox createModernPanel(Component parent, String title, String desc) {
		Vbox panel = new Vbox();
		panel.setParent(parent);
		panel.setWidth("100%");
		panel.setSpacing("8px");
		panel.setStyle("padding:14px; background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; box-shadow:0 12px 24px rgba(15,23,42,.07); box-sizing:border-box;");
		appendHtml(panel, "<div style='font-size:15px; font-weight:900; color:#0f172a;'>" + escape(title) + "</div>"
				+ "<div style='font-size:12px; color:#64748b; line-height:1.55;'>" + escape(desc) + "</div>");
		return panel;
	}

	private List<Bucket> sortBuckets(Map<String, Bucket> data, final String sortBy) {
		List<Bucket> list = new ArrayList<Bucket>();
		if (data != null) {
			list.addAll(data.values());
		}
		Collections.sort(list, new Comparator<Bucket>() {
			@Override
			public int compare(Bucket o1, Bucket o2) {
				double v1 = getBucketSortValue(o1, sortBy);
				double v2 = getBucketSortValue(o2, sortBy);
				if (v1 < v2)
					return 1;
				if (v1 > v2)
					return -1;
				return safe(o1.nama).compareToIgnoreCase(safe(o2.nama));
			}
		});
		return list;
	}

	private double getBucketSortValue(Bucket b, String sortBy) {
		if (b == null) {
			return 0.0;
		}
		if ("dibayar".equalsIgnoreCase(sortBy)) {
			return b.dibayar;
		}
		if ("tagihan".equalsIgnoreCase(sortBy)) {
			return b.tagihan;
		}
		if ("count".equalsIgnoreCase(sortBy)) {
			return b.count;
		}
		return b.piutang;
	}

	private int max(int[] values) {
		int max = 0;
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				if (values[i] > max) {
					max = values[i];
				}
			}
		}
		return max;
	}

	private int percent(double nilai, double total) {
		if (total <= 0.0) {
			return 0;
		}
		return (int) Math.round((nilai * 100.0) / total);
	}

	private String money(double value) {
		try {
			return value >= 0.0 ? Common.numberFormat.get().format(value)
					: "(" + Common.numberFormat.get().format(Math.abs(value)) + ")";
		} catch (Exception e) {
			return value + "";
		}
	}

	private String formatInt(int value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private void appendHtml(Component parent, String html) {
		Html h = new Html(html);
		h.setParent(parent);
	}

	private String safe(String s) {
		return s == null ? "" : s;
	}

	private String escape(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
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
		String mahasiswaKey;
		String fakultas;
		String jurusan;
		String jenis;
		String itemBiaya;
		Integer bulan;
		String bulanText;
		String tahunAkademik;
		int semester;
		double tagihan;
		double dibayar;
		double sisa;
		String status;
		String agingGroup;
	}

	private static class DashboardData {
		int jumlahData;
		int jumlahTagihan;
		int jumlahDibayar;
		int jumlahPiutang;
		int jumlahBelumBayar;
		int jumlahParsial;
		int jumlahLunas;
		int jumlahLebihBayar;
		double totalTagihan;
		double totalDibayar;
		double totalPiutang;
		double totalLebihBayar;
		Map<String, Bucket> byJenis = new HashMap<String, Bucket>();
		Map<String, Bucket> byItem = new HashMap<String, Bucket>();
		Map<String, Bucket> byBulan = new HashMap<String, Bucket>();
		Map<String, Bucket> byProdi = new HashMap<String, Bucket>();
		Map<String, Bucket> byFakultas = new HashMap<String, Bucket>();
		Map<String, Bucket> byTahun = new HashMap<String, Bucket>();
		Map<String, Bucket> bySemester = new HashMap<String, Bucket>();
		Map<String, Bucket> byStatus = new HashMap<String, Bucket>();
		Map<String, Bucket> byMahasiswa = new HashMap<String, Bucket>();
		Map<String, Bucket> byAging = new HashMap<String, Bucket>();
		Map<String, Bucket> byMahasiswaItem = new HashMap<String, Bucket>();
	}

	private static class Bucket {
		String nama;
		int count;
		double tagihan;
		double dibayar;
		double piutang;
		double sisa;
		int belumBayar;
		int parsial;
		int lunas;
		int lebihBayar;
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
