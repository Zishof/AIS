package ais.action.master.obe;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataKurikulumBanbox;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.database.model.PerguruanTinggi;
import ais.database.model.obe.CapaianLulusan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class CapaianLulusanVsKurikulumMatakuliahAction extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Center center;
	private java.util.List<CapaianLulusan> loadedCpl = null;
	private java.util.List<KurikulumPunyaMatakuliah> loadedKpm = null;

	public CapaianLulusanVsKurikulumMatakuliahAction() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public CapaianLulusanVsKurikulumMatakuliahAction(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	private PerguruanTinggi perguruanTinggi;

	private Textbox nama;

	private Textbox nama1;

	private AmbilDataKurikulumBanbox searchkurikulum;

	private void init() throws Exception {
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North west = new North();
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setHeight("72px");

		/* Hindari transformasi otomatis MyGrid -> North berjudul "Menu". */
		Div filterContainer = new Div();
		filterContainer.setWidth("100%");
		filterContainer.setHeight("100%");
		filterContainer.setParent(west);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(filterContainer);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kurikulum" + " *"));
		row.appendChild(searchkurikulum = new AmbilDataKurikulumBanbox());
		searchkurikulum.setWidth("90%");
		searchkurikulum.setReadonly(true);
		searchkurikulum.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Capaian Lulusan"));
		row.appendChild(nama1 = new Textbox());
		nama1.setWidth("90%");

		nama1.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyButtonConfig button = new MyButtonConfig("Refresh");
		button.setParent(row);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

		MyButtonConfig matriksBtn = new MyButtonConfig("Lihat Matriks");
		matriksBtn.setParent(row);
		matriksBtn.setStyle("margin-left:6px;background:#1d4ed8;color:#fff;border-radius:4px;padding:2px 10px;border:0");
		matriksBtn.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				showMatriks();
			}
		});

	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings({ "deprecation", "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					Common.clear(center);
					Kurikulum kurikulum = (Kurikulum) searchkurikulum.getAttribute("kurikulum");
					if (kurikulum == null) {
						MyMessageboxConfig.show("Pilih " + "Kurikulum", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}

					Session session = HibernateUtil.currentSession();
					Jurusan jurusan = kurikulum.getJurusan();
					List<CapaianLulusan> capaianLulusans = ConstantValues.simpleList(session
							.createCriteria(CapaianLulusan.class)

							.add(nama1.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(
											Restrictions.ilike("kode", nama1.getValue().trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("nama", nama1.getValue().trim(), MatchMode.ANYWHERE)))

							.add(Restrictions.eq("perguruanTinggi", perguruanTinggi))
							.add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", jurusan)))
							.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							CapaianLulusan.class);

					List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = ConstantValues
							.simpleList(
									session.createCriteria(KurikulumPunyaMatakuliah.class)
											.createAlias("matakuliah", "matakuliah")
											.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
													: Restrictions.or(
															Restrictions.ilike("matakuliah.kode",
																	nama.getValue().trim(), MatchMode.ANYWHERE),
															Restrictions.ilike("matakuliah.nama",
																	nama.getValue().trim(), MatchMode.ANYWHERE)))

											.add(Restrictions.eq("kurikulum", kurikulum))
											.addOrder(Order.asc("matakuliah.kode"))
											.addOrder(Order.asc("matakuliah.nama"))
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.or(Restrictions.isNull("matakuliah.aktif"),
													Restrictions.eq("matakuliah.aktif", true))),
									KurikulumPunyaMatakuliah.class);

					MyGrid grid = new MyGrid();
					ais.ui.util.ZkCompat.setFixedLayout(grid, false);
					grid.setWidth("100%");
					grid.setParent(center);
					grid.setHeight("100%");

					Columns columns = new Columns();
					columns.setParent(grid);

					MyColumnConfig column = new MyColumnConfig("Capaian");
					column.setParent(columns);
					column.setWidth("120px");

					for (int smt = 1; smt <= jurusan.getJenjang().getJumlahSemester(); smt++) {
						column = new MyColumnConfig("Semester " + smt + "");
						column.setParent(columns);
						column.setWidth("120px");
						column.setTooltiptext("Semester " + smt);
					}

					Rows rows = new Rows();
					rows.setParent(grid);

					loadedCpl = capaianLulusans;
					loadedKpm = kurikulumPunyaMatakuliahs;

					for (CapaianLulusan capaianLulusan : capaianLulusans) {
						MyFormRow row = new MyFormRow();
						row.setTooltiptext(capaianLulusan.getKode() + " " + capaianLulusan.getNama());
						row.setValign("top");
						row.setParent(rows);
						row.appendChild(new Label(capaianLulusan.getKode()));

						for (int smt = 1; smt <= jurusan.getJenjang().getJumlahSemester(); smt++) {
							Vbox vbox = new Vbox();
							row.appendChild(vbox);
							for (final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
								if (kurikulumPunyaMatakuliah.getSemester() != null
										&& kurikulumPunyaMatakuliah.getSemester().equals(smt)) {
									Matakuliah matakuliah = kurikulumPunyaMatakuliah.getMatakuliah();
									if (matakuliah.getCapaianLulusan().contains("," + capaianLulusan.getId() + ",")) {
										Label lbl;
										vbox.appendChild(lbl = new Label(matakuliah.getKode()));
										lbl.setTooltiptext(matakuliah.getNama());
									}
								}
							}
						}
					}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

	}

	private void showMatriks() {
		if (loadedCpl == null || loadedKpm == null || loadedCpl.isEmpty()) {
			try {
				MyMessageboxConfig.show("Klik Refresh dahulu untuk memuat data.", "Info",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/obe/CapaianLulusanVsKurikulumMatakuliahAction.java:296"); /* ignore */ }
			return;
		}
		// Sort KPM by semester then MK kode
		java.util.List<KurikulumPunyaMatakuliah> kpmSorted = new java.util.ArrayList<KurikulumPunyaMatakuliah>(loadedKpm);
		java.util.Collections.sort(kpmSorted, new java.util.Comparator<KurikulumPunyaMatakuliah>() {
			public int compare(KurikulumPunyaMatakuliah a, KurikulumPunyaMatakuliah b) {
				int sa = a.getSemester() == null ? 0 : a.getSemester();
				int sb2 = b.getSemester() == null ? 0 : b.getSemester();
				if (sa != sb2) return sa - sb2;
				String ka = a.getMatakuliah() == null ? "" : (a.getMatakuliah().getKode() == null ? "" : a.getMatakuliah().getKode());
				String kb = b.getMatakuliah() == null ? "" : (b.getMatakuliah().getKode() == null ? "" : b.getMatakuliah().getKode());
				return ka.compareTo(kb);
			}
		});
		StringBuilder sb = new StringBuilder();
		sb.append("<style>");
		sb.append(".mx{font-family:Arial,sans-serif;padding:14px;color:#1e293b;overflow-x:auto}");
		sb.append(".mx h3{margin:0 0 10px;color:#1e40af;font-size:13pt}");
		sb.append(".mxt{border-collapse:collapse;font-size:9pt}");
		sb.append(".mxt th,.mxt td{border:1px solid #cbd5e1;padding:4px 6px;text-align:center}");
		sb.append(".lkode{background:#dbeafe;text-align:left;white-space:nowrap;font-weight:bold;position:sticky;left:0;z-index:1}");
		sb.append(".lnama{background:#f0f7ff;text-align:left;font-size:8pt;max-width:240px}");
		sb.append(".hcol{background:#dbeafe;font-weight:bold;writing-mode:vertical-rl;transform:rotate(180deg);height:80px;min-width:30px;cursor:default}");
		sb.append(".hsmt{background:#ede9fe;font-weight:bold;font-size:8pt;padding:2px 4px}");
		sb.append(".yes{background:#16a34a;color:#fff;font-weight:bold}");
		sb.append(".no{background:#f8fafc}");
		sb.append(".tot{background:#fef9c3;font-weight:bold}");
		sb.append(".corner{background:#e2e8f0;font-size:8pt;font-weight:bold}");
		sb.append(".summ{font-size:9pt;color:#64748b;margin-top:8px;padding:6px 0}");
		sb.append("</style>");
		sb.append("<div class='mx'>");
		sb.append("<h3>Matriks CPL &times; Matakuliah (per Semester)</h3>");
		sb.append("<table class='mxt'><thead>");
		// Semester header row
		sb.append("<tr><th class='corner lkode' rowspan='2'>CPL</th><th class='corner lnama' rowspan='2'>Deskripsi CPL</th>");
		int prevSmt = -1;
		int smtCount = 0;
		for (KurikulumPunyaMatakuliah kpm : kpmSorted) {
			int smt = kpm.getSemester() == null ? 0 : kpm.getSemester();
			if (smt != prevSmt) {
				if (prevSmt >= 0) sb.append("<th class='hsmt' colspan='").append(smtCount).append("'>Smt ").append(prevSmt).append("</th>");
				prevSmt = smt; smtCount = 1;
			} else { smtCount++; }
		}
		if (prevSmt >= 0) sb.append("<th class='hsmt' colspan='").append(smtCount).append("'>Smt ").append(prevSmt).append("</th>");
		sb.append("<th class='corner tot' rowspan='2'>#</th></tr>");
		// MK header row
		sb.append("<tr>");
		for (KurikulumPunyaMatakuliah kpm : kpmSorted) {
			String kode = kpm.getMatakuliah() == null ? "?" : (kpm.getMatakuliah().getKode() == null ? "?" : kpm.getMatakuliah().getKode());
			String nama = kpm.getMatakuliah() == null ? "" : (kpm.getMatakuliah().getNama() == null ? "" : kpm.getMatakuliah().getNama());
			sb.append("<th class='hcol' title='").append(esc(nama)).append("'>").append(esc(kode)).append("</th>");
		}
		sb.append("</tr></thead><tbody>");
		int[] colTotals = new int[kpmSorted.size()];
		int grand = 0;
		for (CapaianLulusan cpl : loadedCpl) {
			int rowTotal = 0;
			sb.append("<tr><td class='lkode'>").append(esc(cpl.getKode())).append("</td>");
			sb.append("<td class='lnama'>").append(esc(cpl.getNama())).append("</td>");
			for (int i = 0; i < kpmSorted.size(); i++) {
				Matakuliah mk = kpmSorted.get(i).getMatakuliah();
				boolean mapped = mk != null && mk.getCapaianLulusan().contains("," + cpl.getId() + ",");
				if (mapped) { rowTotal++; colTotals[i]++; grand++; }
				sb.append("<td class='").append(mapped ? "yes" : "no").append("'>")
				  .append(mapped ? "&#10003;" : "&nbsp;").append("</td>");
			}
			sb.append("<td class='tot'>").append(rowTotal).append("</td></tr>");
		}
		sb.append("<tr><td class='tot' colspan='2'>Total</td>");
		for (int t : colTotals) sb.append("<td class='tot'>").append(t).append("</td>");
		sb.append("<td class='tot'>").append(grand).append("</td></tr>");
		sb.append("</tbody></table>");
		int max = loadedCpl.size() * kpmSorted.size();
		int pct = max == 0 ? 0 : grand * 100 / max;
		sb.append("<div class='summ'>Cakupan: <b>").append(grand).append("/").append(max)
		  .append("</b> (").append(pct).append("%) &mdash; ")
		  .append(loadedCpl.size()).append(" CPL &times; ").append(kpmSorted.size()).append(" MK</div>");
		sb.append("</div>");
		MyWindow win = new MyWindow("Matriks CPL × Matakuliah", "normal", true);
		win.setWidth("95%");
		win.setHeight("88%");
		org.zkoss.zul.Div d = new org.zkoss.zul.Div();
		d.setStyle("overflow:auto;height:100%;padding:2px");
		d.setParent(win);
		new org.zkoss.zul.Html(sb.toString()).setParent(d);
		try { win.onModal(); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
	}

	private static String esc(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

}
