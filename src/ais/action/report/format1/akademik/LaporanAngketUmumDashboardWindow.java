package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;
import ais.ui.util.DashboardGridExportHelper;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.GrupChecklistPenilaianUmumAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanAngketUmumDashboardWindow extends MyWindow {

	private static final long serialVersionUID = 3820619879461224201L;

	private Combobox tahunAkademik;
	private Combobox semesterAbsensi;
	private Combobox searchdiperuntukkan;
	private Center center;
	private Div dashboardContent;

	public LaporanAngketUmumDashboardWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Angket Umum Dashboard Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAngketUmumDashboardWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() throws Exception {
		DashboardGridExportHelper.pasang(this, "Laporan Angket Umum Dashboard Window");
		setHeight("100%");
		setWidth("100%");

		final EventListener reloadListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				reloadDashboard();
			}
		};

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setAutoscroll(true);

		Div mainWrapper = new Div();
		mainWrapper.setStyle("width:100%;height:100%;overflow:auto;box-sizing:border-box;background:#f6f8fb;");
		mainWrapper.setParent(center);

		Vbox northBox = new Vbox();
		northBox.setWidth("100%");
		northBox.setStyle("box-sizing:border-box;padding:10px 12px;background:#f8fafc;border-bottom:1px solid #e5e7eb;");
		northBox.setParent(mainWrapper);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(northBox);
		toolbar.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = new Combobox();
		Common.generateTahunAjaran(tahunAkademik);
		tahunAkademik.setReadonly(true);
		tahunAkademik.setWidth("145px");
		tahunAkademik.addEventListener("onChange", reloadListener);
		tahunAkademik.setParent(toolbar);

		toolbar.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		semesterAbsensi = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		semesterAbsensi.setReadonly(true);
		semesterAbsensi.setWidth("100px");
		semesterAbsensi.addEventListener("onChange", reloadListener);
		semesterAbsensi.setParent(toolbar);

		toolbar.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengguna"));
		searchdiperuntukkan = new Combobox();
		GrupChecklistPenilaianUmumAction.diperuntukkan(searchdiperuntukkan);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		searchdiperuntukkan.appendChild(comboitem);
		searchdiperuntukkan.setSelectedItem(comboitem);
		searchdiperuntukkan.setReadonly(true);
		searchdiperuntukkan.setWidth("160px");
		searchdiperuntukkan.addEventListener("onChange", reloadListener);
		searchdiperuntukkan.setParent(toolbar);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh.svg");
		refresh.addEventListener("onClick", reloadListener);
		refresh.setParent(toolbar);

		Html info = new Html("<div style='padding:8px 12px;line-height:1.5;color:#374151;background:#f8fafc;border-top:1px solid #e5e7eb;'>"
				+ "<b>Dasbor Angket Umum</b> - "
				+ "Menampilkan ringkasan jawaban, rata-rata nilai, peserta, kelompok angket, distribusi nilai, dan masukan terbaru. "
				+ "Filter di atas akan memuat ulang seluruh kartu dan tabel dasbor."
				+ "</div>");
		info.setParent(northBox);

		dashboardContent = new Div();
		dashboardContent.setStyle("width:100%;box-sizing:border-box;");
		dashboardContent.setParent(mainWrapper);

		reloadDashboard();
	}

	private void reloadDashboard() throws Exception {
		Common.clear(dashboardContent);
		Html loading = new Html("<div style='padding:14px;color:#555;'><i class='fa fa-spinner fa-spin'></i> Memuat dasbor angket umum...</div>");
		loading.setParent(dashboardContent);
		try {
			final Filter filter = readFilter();
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					renderDashboard(filter);
				}
			});
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Umum Dashboard Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private Filter readFilter() {
		Filter filter = new Filter();
		filter.tahunAkademik = selectedString(tahunAkademik, "Semua");
		filter.semester = selectedString(semesterAbsensi, "Semua");
		filter.diperuntukkan = selectedString(searchdiperuntukkan, "Semua");
		return filter;
	}

	private String selectedString(Combobox combo, String defaultValue) {
		try {
			if (combo == null || combo.getSelectedItem() == null || combo.getSelectedItem().getValue() == null) {
				return defaultValue;
			}
			Object value = combo.getSelectedItem().getValue();
			return value == null ? defaultValue : String.valueOf(value);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	private void renderDashboard(Filter filter) {
		Session session = null;
		try {
			session = ais.action.report.Report.openNativeSession();
			DashboardData data = loadDashboardData(session, filter);
			Common.clear(dashboardContent);

			Div wrapper = new Div();
			wrapper.setStyle("padding:14px;overflow:auto;box-sizing:border-box;");
			wrapper.setParent(dashboardContent);

			Html title = new Html("<div style='margin-bottom:12px;'>"
					+ "<div style='font-size:18px;font-weight:bold;color:#163d7a;'>Dasbor Angket Umum</div>"
					+ "<div style='color:#666;font-size:12px;'>Tahun Akademik: <b>" + html(filter.tahunAkademik)
					+ "</b> &nbsp; Semester: <b>" + html(filter.semester) + "</b> &nbsp; Jenis Pengguna: <b>"
					+ html(filter.diperuntukkan) + "</b></div></div>");
			title.setParent(wrapper);

			renderCards(wrapper, data);
			renderCssVisualizations(wrapper, data);
			renderDistribution(wrapper, data);
			renderGroupSummary(wrapper, data.groupRows);
			renderJenisSummary(wrapper, data.jenisRows);
			renderKeteranganTerbaru(wrapper, data.keteranganRows);
		} catch (Exception e) {
			Common.clear(dashboardContent);
			Html error = new Html("<div style='padding:14px;color:#b91c1c;background:#fee2e2;border:1px solid #fecaca;'>"
					+ "Gagal memuat dasbor angket umum. Silakan cek struktur tabel atau query laporan. Detail error: "
					+ html(e.getMessage()) + "</div>");
			error.setParent(dashboardContent);
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSession(session);
		}
	}

	private DashboardData loadDashboardData(Session session, Filter filter) {
		DashboardData data = new DashboardData();
		data.totalJawaban = longValue(uniqueResult(session, baseCountSql("count(1)"), filter));
		data.totalPeserta = longValue(uniqueResult(session, baseCountSql(
				"count(distinct coalesce('M' || h.mahasiswa::text, 'S' || h.siswa::text, 'D' || h.dosen::text, 'G' || h.guru::text, 'U' || h.tbmuser::text, 'TD' || h.tbmuser_dinilai::text, '0'))"), filter));
		data.totalPertanyaan = longValue(uniqueResult(session, baseCountSql("count(distinct c.id)"), filter));
		data.totalKelompok = longValue(uniqueResult(session, baseCountSql("count(distinct g.id)"), filter));
		data.totalJadwal = longValue(uniqueResult(session, baseCountSql("count(distinct j.id)"), filter));
		data.rataRata = doubleValue(uniqueResult(session, baseCountSql("avg(h.nilai)"), filter));

		List nilaiRows = list(session, baseSelectSql("h.nilai, count(1)") + " group by h.nilai order by h.nilai", filter);
		for (int i = 0; i < nilaiRows.size(); i++) {
			Object[] row = toArray(nilaiRows.get(i));
			Integer nilai = integerValue(row.length > 0 ? row[0] : null);
			Long jumlah = Long.valueOf(longValue(row.length > 1 ? row[1] : null));
			if (nilai != null) {
				data.nilaiCount.put(nilai, jumlah);
			}
		}

		data.groupRows = list(session, baseSelectSql("coalesce(g.isi, 'Tanpa Kelompok'), coalesce(g.diperuntukkan, 'Umum'), count(1), "
				+ "count(distinct coalesce('M' || h.mahasiswa::text, 'S' || h.siswa::text, 'D' || h.dosen::text, 'G' || h.guru::text, 'U' || h.tbmuser::text, 'TD' || h.tbmuser_dinilai::text, '0')), avg(h.nilai)")
				+ " group by g.id, g.isi, g.diperuntukkan order by count(1) desc, coalesce(g.isi, 'Tanpa Kelompok') asc limit 20", filter);

		data.jenisRows = list(session, baseSelectSql("case when h.mahasiswa is not null then 'Mahasiswa' "
				+ "when h.siswa is not null then 'Siswa' when h.dosen is not null then 'Dosen' when h.guru is not null then 'Guru' "
				+ "when h.tbmuser is not null then 'User/Admin' when h.tbmuser_dinilai is not null then 'User Dinilai' else 'Tidak Teridentifikasi' end, "
				+ "count(distinct coalesce('M' || h.mahasiswa::text, 'S' || h.siswa::text, 'D' || h.dosen::text, 'G' || h.guru::text, 'U' || h.tbmuser::text, 'TD' || h.tbmuser_dinilai::text, '0')), count(1), avg(h.nilai)")
				+ " group by 1 order by count(1) desc", filter);

		String ketSql = baseSelectSql("coalesce(g.isi, ''), coalesce(c.isi, ''), coalesce(h.keterangan, ''), h.tahun_akademik, h.semester")
				+ " and h.keterangan is not null and trim(h.keterangan) <> '' order by h.id desc limit 10";
		data.keteranganRows = list(session, ketSql, filter);
		return data;
	}

	private String baseCountSql(String selectExpression) {
		return baseSelectSql(selectExpression);
	}

	private String baseSelectSql(String selectExpression) {
		return "select " + selectExpression
				+ " from checklist_hasil_penilaian_umum h "
				+ " left join checklist_penilaian_umum c on c.id = h.checklist_penilaian_umum "
				+ " left join grup_checklist_penilaian_umum g on g.id = c.grup_checklist_penilaian_umum "
				+ " left join jadwal_checklist_penilaian_umum j on cast(j.grup_penilaian_umum as text) = cast(g.id as text) "
				+ " where 1=1 "
				+ " and (:tahunAkademik = 'Semua' or cast(h.tahun_akademik as text) = :tahunAkademik) "
				+ " and (:semester = 'Semua' or cast(h.semester as text) = :semester) "
				+ " and (:diperuntukkan = 'Semua' or cast(coalesce(g.diperuntukkan, 'Umum') as text) = :diperuntukkan) ";
	}

	private Object uniqueResult(Session session, String sql, Filter filter) {
		try {
			Query query = session.createSQLQuery(sql);
			bind(query, filter);
			return query.uniqueResult();
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("rawtypes")
	private List list(Session session, String sql, Filter filter) {
		try {
			Query query = session.createSQLQuery(sql);
			bind(query, filter);
			return query.list();
		} catch (Exception e) {
			return new java.util.ArrayList();
		}
	}

	private void bind(Query query, Filter filter) {
		query.setParameter("tahunAkademik", filter == null || filter.tahunAkademik == null ? "Semua" : filter.tahunAkademik);
		query.setParameter("semester", filter == null || filter.semester == null ? "Semua" : filter.semester);
		query.setParameter("diperuntukkan", filter == null || filter.diperuntukkan == null ? "Semua" : filter.diperuntukkan);
	}

	private void renderCards(Div parent, DashboardData data) {
		Div cards = new Div();
		cards.setStyle("display:flex;flex-wrap:wrap;gap:10px;margin-bottom:12px;");
		cards.setParent(parent);
		appendCard(cards, "Total Jawaban", format(data.totalJawaban), "Jumlah detail jawaban angket yang terekam",
				popupRows(new Object[] { "Total Jawaban", format(data.totalJawaban) }));
		appendCard(cards, "Peserta Mengisi", format(data.totalPeserta), "Jumlah responden unik berdasarkan pemilik jawaban",
				popupRows(new Object[] { "Peserta Mengisi", format(data.totalPeserta) }));
		appendCard(cards, "Rata-rata Nilai", format(data.rataRata), "Rata-rata nilai dari seluruh jawaban",
				popupRows(new Object[] { "Rata-rata Nilai", format(data.rataRata) }));
		appendCard(cards, "Kelompok Angket", format(data.totalKelompok), "Jumlah kelompok angket yang memiliki jawaban",
				data.groupRows);
		appendCard(cards, "Pertanyaan", format(data.totalPertanyaan), "Jumlah pertanyaan yang sudah memiliki jawaban",
				popupRows(new Object[] { "Pertanyaan", format(data.totalPertanyaan) }));
		appendCard(cards, "Jadwal", format(data.totalJadwal), "Jumlah jadwal terkait kelompok angket",
				popupRows(new Object[] { "Jadwal", format(data.totalJadwal) }));
	}

	private void appendCard(Div parent, final String title, String value, String desc, final List rowsData) {
		Div card = new Div();
		card.setStyle("flex:1;min-width:160px;background:#fff;border:1px solid #dbeafe;border-radius:10px;"
				+ "box-shadow:0 2px 8px rgba(15,23,42,.07);padding:12px;cursor:pointer;");
		card.setTooltiptext("Klik untuk melihat detail " + title);
		card.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showDataPopup(title, new String[] { "Data", "Nilai" }, rowsData);
			}
		});
		card.setParent(parent);
		new Html("<div style='font-size:12px;color:#64748b;'>" + html(title) + "</div>"
				+ "<div style='font-size:24px;font-weight:bold;color:#163d7a;line-height:1.3;'>" + html(value) + "</div>"
				+ "<div style='font-size:11px;color:#64748b;'>" + html(desc) + "</div>").setParent(card);
	}

	private void renderDistribution(Div parent, DashboardData data) {
		Div box = new Div();
		box.setStyle("background:#fff;border:1px solid #e5e7eb;border-radius:10px;padding:12px;margin-bottom:12px;");
		box.setParent(parent);
		new Html("<div style='font-weight:bold;color:#163d7a;margin-bottom:8px;'>Distribusi Nilai Jawaban</div>").setParent(box);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.setParent(box);
		Columns columns = new Columns();
		columns.setParent(grid);
		addColumn(columns, "Nilai", "40%");
		addColumn(columns, "Jumlah", "30%");
		addColumn(columns, "Persentase Relatif", "30%");
		Rows rows = new Rows();
		rows.setParent(grid);
		long max = 1;
		for (int i = 1; i <= 5; i++) {
			Long value = data.nilaiCount.get(Integer.valueOf(i));
			if (value != null && value.longValue() > max) {
				max = value.longValue();
			}
		}
		for (int i = 1; i <= 5; i++) {
			long count = data.nilaiCount.get(Integer.valueOf(i)) == null ? 0L : data.nilaiCount.get(Integer.valueOf(i)).longValue();
			int percent = (int) Math.round((count * 100.0) / max);
			List detail = popupRows(new Object[] { "Nilai " + i, format(count), String.valueOf(percent) + "%" });
			Row row = new Row();
			row.setParent(rows);
			clickableLabel("Nilai " + i, "Distribusi Nilai " + i, new String[] { "Nilai", "Jumlah", "Persentase" }, detail).setParent(row);
			clickableLabel(format(count), "Distribusi Nilai " + i, new String[] { "Nilai", "Jumlah", "Persentase" }, detail).setParent(row);
			clickableLabel(String.valueOf(percent) + "%", "Distribusi Nilai " + i, new String[] { "Nilai", "Jumlah", "Persentase" }, detail).setParent(row);
		}
	}

	@SuppressWarnings("rawtypes")
	private void renderGroupSummary(Div parent, List rowsData) {
		new Html("<div style='font-weight:bold;color:#163d7a;margin:10px 0 4px 0;'>Ringkasan per Kelompok Angket</div>"
				+ "<div style='font-size:11px;color:#64748b;line-height:1.45;margin-bottom:8px;'>Panel ini merangkum jawaban berdasarkan kelompok angket. Pengguna dapat melihat kelompok mana yang paling banyak diisi dan bagaimana rata-rata nilainya.</div>").setParent(parent);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		try {
			grid.getPagingChild().setMold("os");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketUmumDashboardWindow.java:380");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Umum Dashboard Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		grid.setParent(parent);
		Columns columns = new Columns();
		columns.setParent(grid);
		addColumn(columns, "Kelompok", "38%");
		addColumn(columns, "Jenis Pengguna", "18%");
		addColumn(columns, "Jawaban", "14%");
		addColumn(columns, "Peserta", "14%");
		addColumn(columns, "Rata-rata", "14%");
		Rows rows = new Rows();
		rows.setParent(grid);
		if (rowsData == null || rowsData.isEmpty()) {
			Row row = new Row();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "5");
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Belum ada data angket pada filter ini.")));
			return;
		}
		for (int i = 0; i < rowsData.size(); i++) {
			Object[] data = toArray(rowsData.get(i));
			Row row = new Row();
			row.setParent(rows);
			List detail = popupRows(data);
			clickableLabel(string(data, 0), "Detail Kelompok Angket", new String[] { "Kelompok", "Jenis Pengguna", "Jawaban", "Peserta", "Rata-rata" }, detail).setParent(row);
			clickableLabel(string(data, 1), "Detail Kelompok Angket", new String[] { "Kelompok", "Jenis Pengguna", "Jawaban", "Peserta", "Rata-rata" }, detail).setParent(row);
			clickableLabel(format(longValue(value(data, 2))), "Detail Kelompok Angket", new String[] { "Kelompok", "Jenis Pengguna", "Jawaban", "Peserta", "Rata-rata" }, detail).setParent(row);
			clickableLabel(format(longValue(value(data, 3))), "Detail Kelompok Angket", new String[] { "Kelompok", "Jenis Pengguna", "Jawaban", "Peserta", "Rata-rata" }, detail).setParent(row);
			clickableLabel(format(doubleValue(value(data, 4))), "Detail Kelompok Angket", new String[] { "Kelompok", "Jenis Pengguna", "Jawaban", "Peserta", "Rata-rata" }, detail).setParent(row);
		}
	}

	@SuppressWarnings("rawtypes")
	private void renderJenisSummary(Div parent, List rowsData) {
		new Html("<div style='font-weight:bold;color:#163d7a;margin:14px 0 4px 0;'>Responden per Jenis Pengguna</div>"
				+ "<div style='font-size:11px;color:#64748b;line-height:1.45;margin-bottom:8px;'>Panel ini menunjukkan jenis pengguna yang mengisi angket. Data ini membantu memastikan angket sudah menjangkau kelompok pengguna yang tepat.</div>").setParent(parent);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.setParent(parent);
		Columns columns = new Columns();
		columns.setParent(grid);
		addColumn(columns, "Jenis", "35%");
		addColumn(columns, "Peserta", "20%");
		addColumn(columns, "Jawaban", "20%");
		addColumn(columns, "Rata-rata", "20%");
		Rows rows = new Rows();
		rows.setParent(grid);
		if (rowsData == null || rowsData.isEmpty()) {
			Row row = new Row();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "4");
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Belum ada data responden pada filter ini.")));
			return;
		}
		for (int i = 0; i < rowsData.size(); i++) {
			Object[] data = toArray(rowsData.get(i));
			Row row = new Row();
			row.setParent(rows);
			List detail = popupRows(data);
			clickableLabel(string(data, 0), "Detail Responden", new String[] { "Jenis", "Peserta", "Jawaban", "Rata-rata" }, detail).setParent(row);
			clickableLabel(format(longValue(value(data, 1))), "Detail Responden", new String[] { "Jenis", "Peserta", "Jawaban", "Rata-rata" }, detail).setParent(row);
			clickableLabel(format(longValue(value(data, 2))), "Detail Responden", new String[] { "Jenis", "Peserta", "Jawaban", "Rata-rata" }, detail).setParent(row);
			clickableLabel(format(doubleValue(value(data, 3))), "Detail Responden", new String[] { "Jenis", "Peserta", "Jawaban", "Rata-rata" }, detail).setParent(row);
		}
	}

	@SuppressWarnings("rawtypes")
	private void renderKeteranganTerbaru(Div parent, List rowsData) {
		new Html("<div style='font-weight:bold;color:#163d7a;margin:14px 0 4px 0;'>Masukan / Keterangan Terbaru</div>"
				+ "<div style='font-size:11px;color:#64748b;line-height:1.45;margin-bottom:8px;'>Panel ini menampilkan komentar terbaru dari pengisi angket. Masukan ini membantu membaca alasan di balik nilai yang diberikan.</div>").setParent(parent);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.setParent(parent);
		Columns columns = new Columns();
		columns.setParent(grid);
		addColumn(columns, "Kelompok", "22%");
		addColumn(columns, "Pertanyaan", "28%");
		addColumn(columns, "Keterangan", "36%");
		addColumn(columns, "Periode", "14%");
		Rows rows = new Rows();
		rows.setParent(grid);
		if (rowsData == null || rowsData.isEmpty()) {
			Row row = new Row();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "4");
			row.appendChild(new Label("Belum ada keterangan/masukan pada filter ini."));
			return;
		}
		for (int i = 0; i < rowsData.size(); i++) {
			Object[] data = toArray(rowsData.get(i));
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			List detail = popupRows(data);
			clickableLabel(limit(string(data, 0), 120), "Detail Keterangan", new String[] { "Kelompok", "Pertanyaan", "Keterangan", "Tahun", "Semester" }, detail).setParent(row);
			clickableLabel(limit(string(data, 1), 150), "Detail Keterangan", new String[] { "Kelompok", "Pertanyaan", "Keterangan", "Tahun", "Semester" }, detail).setParent(row);
			clickableLabel(limit(string(data, 2), 300), "Detail Keterangan", new String[] { "Kelompok", "Pertanyaan", "Keterangan", "Tahun", "Semester" }, detail).setParent(row);
			clickableLabel(string(data, 3) + " / " + string(data, 4), "Detail Keterangan", new String[] { "Kelompok", "Pertanyaan", "Keterangan", "Tahun", "Semester" }, detail).setParent(row);
		}
	}


	private void renderCssVisualizations(Div parent, DashboardData data) {
		Div charts = new Div();
		charts.setStyle("display:flex;flex-wrap:wrap;gap:12px;margin-bottom:12px;");
		charts.setParent(parent);
		renderCssBarVisual(charts, data);
		renderCssTrendVisual(charts, data);
		renderCssSpiderVisual(charts, data);
	}

	private void renderCssBarVisual(Div parent, DashboardData data) {
		Div box = visualBox(parent, "Grafik Distribusi Nilai", "Bar CSS berdasarkan jumlah jawaban per nilai. Klik batang untuk melihat detail.");
		long max = 1L;
		for (int i = 1; i <= 5; i++) {
			Long value = (Long) data.nilaiCount.get(Integer.valueOf(i));
			if (value != null && value.longValue() > max) {
				max = value.longValue();
			}
		}
		for (int i = 1; i <= 5; i++) {
			long jumlah = data.nilaiCount.get(Integer.valueOf(i)) == null ? 0L : ((Long) data.nilaiCount.get(Integer.valueOf(i))).longValue();
			int percent = clampPercent(max <= 0 ? 0 : (jumlah * 100.0D / max));
			List detail = popupRows(new Object[] { "Nilai " + i, format(jumlah), String.valueOf(percent) + "%" });
			appendCssBar(box, "Nilai " + i, jumlah, percent, "Detail Grafik Nilai " + i,
					new String[] { "Nilai", "Jumlah", "Persentase" }, detail);
		}
	}

	private void renderCssTrendVisual(Div parent, DashboardData data) {
		Div box = visualBox(parent, "Trend Rata-rata per Kelompok", "Grafik trend CSS dari rata-rata kelompok/aspek angket umum.");
		int added = 0;
		for (int i = 0; data.groupRows != null && i < data.groupRows.size() && added < 8; i++) {
			Object[] arr = toArray(data.groupRows.get(i));
			String label = string(arr, 0);
			double avg = doubleValue(value(arr, 4));
			int percent = clampPercent(avg * 20.0D);
			appendCssBar(box, label, avg, percent, "Detail Trend " + label,
					new String[] { "Kelompok", "Jenis Pengguna", "Jawaban", "Peserta", "Rata-rata" }, popupRows(arr));
			added++;
		}
		if (added == 0) {
			new Html("<div style='color:#64748b;font-size:12px;'>Belum ada data trend pada filter ini.</div>").setParent(box);
		}
	}

	private void renderCssSpiderVisual(Div parent, DashboardData data) {
		Div box = visualBox(parent, "Spider Web Aspek Angket", "Radar/spider web ringan berbasis CSS untuk ringkasan lima aspek teratas.");
		double total = 0D;
		int count = 0;
		StringBuffer labels = new StringBuffer();
		for (int i = 0; data.groupRows != null && i < data.groupRows.size() && count < 5; i++) {
			Object[] arr = toArray(data.groupRows.get(i));
			double avg = doubleValue(value(arr, 4));
			total += avg;
			count++;
			labels.append("<div style='display:flex;justify-content:space-between;gap:8px;border-bottom:1px dashed #e5e7eb;padding:3px 0;'>")
					.append("<span>").append(html(limit(string(arr, 0), 34))).append("</span>")
					.append("<b>").append(html(format(avg))).append("</b></div>");
		}
		double avgAll = count <= 0 ? 0D : total / count;
		int radar = clampPercent(avgAll * 20.0D);
		String html = "<div style='display:flex;align-items:center;gap:12px;'>"
				+ "<div style='position:relative;width:150px;height:150px;border-radius:50%;"
				+ "background:radial-gradient(circle,transparent 0 22%,#e5e7eb 23% 24%,transparent 25% 45%,#e5e7eb 46% 47%,transparent 48% 68%,#e5e7eb 69% 70%,transparent 71% 88%,#bfdbfe 89% 90%),"
				+ "conic-gradient(#2563eb 0 " + radar + "%,#e0f2fe " + radar + "% 100%);border:1px solid #bfdbfe;'>"
				+ "<div style='position:absolute;left:31px;top:52px;width:88px;text-align:center;background:rgba(255,255,255,.88);border-radius:12px;padding:6px 0;'>"
				+ "<div style='font-size:11px;color:#64748b;'>Rata-rata</div><div style='font-size:20px;font-weight:bold;color:#1d4ed8;'>" + html(format(avgAll)) + "</div></div></div>"
				+ "<div style='flex:1;font-size:12px;color:#334155;'>" + labels.toString() + "</div></div>";
		new Html(html).setParent(box);
	}

	private Div visualBox(Div parent, String title, String subtitle) {
		Div box = new Div();
		box.setStyle("flex:1;min-width:280px;background:#fff;border:1px solid #dbeafe;border-radius:12px;padding:12px;box-shadow:0 2px 8px rgba(15,23,42,.06);box-sizing:border-box;");
		box.setParent(parent);
		new Html("<div style='font-weight:bold;color:#163d7a;margin-bottom:2px;'>" + html(title) + "</div>"
				+ "<div style='font-size:11px;color:#64748b;margin-bottom:8px;'>" + html(subtitle) + "</div>").setParent(box);
		return box;
	}

	private void appendCssBar(Div parent, String label, double value, int percent, final String popupTitle,
			final String[] headers, final List detail) {
		Div row = new Div();
		row.setStyle("margin:7px 0;cursor:pointer;");
		row.setTooltiptext("Klik untuk melihat detail");
		row.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showDataPopup(popupTitle, headers, detail);
			}
		});
		row.setParent(parent);
		String nilai = Math.abs(value - Math.round(value)) < 0.0001D ? format(Math.round(value)) : format(value);
		new Html("<div style='display:flex;justify-content:space-between;font-size:12px;color:#334155;margin-bottom:3px;'>"
				+ "<span>" + html(limit(label, 36)) + "</span><b>" + html(nilai) + "</b></div>"
				+ "<div style='height:10px;background:#e0f2fe;border-radius:999px;overflow:hidden;'>"
				+ "<div style='height:10px;width:" + percent + "%;background:linear-gradient(90deg,#2563eb,#22c55e);border-radius:999px;'></div></div>").setParent(row);
	}

	private int clampPercent(double value) {
		if (value < 0D) {
			return 0;
		}
		if (value > 100D) {
			return 100;
		}
		return (int) Math.round(value);
	}

	private void addColumn(Columns columns, String label, String width) {
		MyColumnConfig column = new MyColumnConfig();
		column.setLabel(label);
		if (width != null) {
			column.setWidth(width);
		}
		column.setParent(columns);
	}

	private Object[] toArray(Object data) {
		if (data instanceof Object[]) {
			return (Object[]) data;
		}
		return new Object[] { data };
	}

	private Object value(Object[] data, int index) {
		return data == null || index < 0 || index >= data.length ? null : data[index];
	}

	private String string(Object[] data, int index) {
		Object value = value(data, index);
		return value == null ? "" : String.valueOf(value);
	}

	private Integer integerValue(Object value) {
		try {
			if (value == null) {
				return null;
			}
			if (value instanceof Number) {
				return Integer.valueOf(((Number) value).intValue());
			}
			return Integer.valueOf(String.valueOf(value));
		} catch (Exception e) {
			return null;
		}
	}

	private long longValue(Object value) {
		try {
			if (value == null) {
				return 0L;
			}
			if (value instanceof BigInteger) {
				return ((BigInteger) value).longValue();
			}
			if (value instanceof BigDecimal) {
				return ((BigDecimal) value).longValue();
			}
			if (value instanceof Number) {
				return ((Number) value).longValue();
			}
			return Long.parseLong(String.valueOf(value));
		} catch (Exception e) {
			return 0L;
		}
	}

	private double doubleValue(Object value) {
		try {
			if (value == null) {
				return 0D;
			}
			if (value instanceof BigDecimal) {
				return ((BigDecimal) value).doubleValue();
			}
			if (value instanceof Number) {
				return ((Number) value).doubleValue();
			}
			return Double.parseDouble(String.valueOf(value));
		} catch (Exception e) {
			return 0D;
		}
	}

	private String format(long value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String format(double value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String limit(String value, int max) {
		if (value == null) {
			return "";
		}
		String text = value.replace('\n', ' ').replace('\r', ' ').trim();
		if (text.length() <= max) {
			return text;
		}
		return text.substring(0, max) + "...";
	}

	private String html(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}


	private Label clickableLabel(String text, final String title, final String[] headers, final List rowsData) {
		Label label = new Label(text == null ? "" : text);
		label.setStyle("cursor:pointer;color:#1d4ed8;text-decoration:underline;");
		label.setTooltiptext("Klik untuk melihat detail");
		label.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				showDataPopup(title, headers, rowsData);
			}
		});
		return label;
	}

	private List popupRows(Object data) {
		List rows = new java.util.ArrayList();
		rows.add(data);
		return rows;
	}

	private void showDataPopup(String title, String[] headers, List rowsData) {
		try {
			MyWindow window = new MyWindow();
			window.setTitle(title == null ? "Detail Data" : title);
			window.setWidth("82%");
			window.setHeight("78%");
			window.setClosable(true);
			window.setParent(this);
			Div wrap = new Div();
			wrap.setStyle("height:100%;overflow:auto;padding:10px;box-sizing:border-box;");
			wrap.setParent(window);
			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(10);
			grid.setParent(wrap);
			Columns columns = new Columns();
			columns.setParent(grid);
			if (headers == null || headers.length == 0) {
				headers = new String[] { "Data" };
			}
			for (int i = 0; i < headers.length; i++) {
				addColumn(columns, headers[i], null);
			}
			Rows rows = new Rows();
			rows.setParent(grid);
			if (rowsData == null || rowsData.isEmpty()) {
				Row row = new Row();
				row.setParent(rows);
				row.setSpans(String.valueOf(headers.length));
				new Label(ais.common.Common.getBahasaConfig("Tidak ada data detail.")).setParent(row);
			} else {
				for (int i = 0; i < rowsData.size(); i++) {
					Object[] arr = toArray(rowsData.get(i));
					Row row = new Row();
					row.setValign("top");
					row.setParent(rows);
					for (int j = 0; j < headers.length; j++) {
						new Label(j < arr.length && arr[j] != null ? String.valueOf(arr[j]) : "").setParent(row);
					}
				}
			}
			window.setVisible(true);
			window.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Umum Dashboard Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void closeSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketUmumDashboardWindow.java:779");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Umum Dashboard Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketUmumDashboardWindow.java:783");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Umum Dashboard Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		try {
			session.close();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketUmumDashboardWindow.java:787");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Angket Umum Dashboard Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	private static class Filter implements Serializable {
		private static final long serialVersionUID = -4545988118961519518L;
		private String tahunAkademik;
		private String semester;
		private String diperuntukkan;
	}

	private static class DashboardData implements Serializable {
		private static final long serialVersionUID = 4875809889216694153L;
		private long totalJawaban;
		private long totalPeserta;
		private long totalPertanyaan;
		private long totalKelompok;
		private long totalJadwal;
		private double rataRata;
		private Map<Integer, Long> nilaiCount = new HashMap<Integer, Long>();
		private List groupRows;
		private List jenisRows;
		private List keteranganRows;
	}
}
