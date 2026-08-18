package ais.action.master.obe;

import java.util.List;

import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.obe.BahanKajian;
import ais.database.model.obe.CapaianLulusan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class CapaianLulusanVsBahanKajianAction extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Combobox fakultas;
	private Combobox jurusan;

	private Center center;
	private java.util.List<CapaianLulusan> loadedCpl = null;
	private java.util.List<BahanKajian> loadedBk = null;

	public CapaianLulusanVsBahanKajianAction() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public CapaianLulusanVsBahanKajianAction(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

	}

	private PerguruanTinggi perguruanTinggi;

	private Textbox nama;

	private Textbox nama1;

	private void init() throws Exception {
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North west = new North();
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setHeight("40px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
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

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas" + " *"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Jurusan") + " *"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		jurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Capaian"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Bahan Kajian"));
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
					if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
						MyMessageboxConfig.show("Pilih " + "Fakultas", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}

					if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
						MyMessageboxConfig.show("Pilih " + Common.getBahasaConfig("Jurusan"), "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					Jurusan jurusan = (Jurusan) CapaianLulusanVsBahanKajianAction.this.jurusan.getSelectedItem()
							.getValue();
					List<BahanKajian> bahanKajians = ConstantValues.simpleList(HibernateUtil.currentSession()
							.createCriteria(BahanKajian.class)

							.add(nama1.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(
											Restrictions.ilike("kode", nama1.getValue().trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("nama", nama1.getValue().trim(), MatchMode.ANYWHERE)))

							.add(Restrictions.eq("perguruanTinggi", perguruanTinggi))
							.add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", jurusan)))
							.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							BahanKajian.class);

					List<CapaianLulusan> capaianLulusans = ConstantValues.simpleList(HibernateUtil.currentSession()
							.createCriteria(CapaianLulusan.class)

							.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(
											Restrictions.ilike("kode", nama.getValue().trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE)))

							.add(Restrictions.eq("perguruanTinggi", perguruanTinggi))
							.add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", jurusan)))
							.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							CapaianLulusan.class);

					MyGrid grid = new MyGrid();
					ais.ui.util.ZkCompat.setFixedLayout(grid, false);
					grid.setWidth("100%");
					grid.setParent(center);
					grid.setHeight("100%");

					Columns columns = new Columns();
					columns.setParent(grid);

					MyColumnConfig column = new MyColumnConfig("Kode");
					column.setParent(columns);
					column.setWidth("80px");

					column = new MyColumnConfig("Capaian");
					column.setParent(columns);
					column.setWidth("300px");

					for (BahanKajian bahanKajian : bahanKajians) {
						column = new MyColumnConfig(bahanKajian.getKode());
						column.setParent(columns);
						column.setWidth("40px");
						column.setTooltiptext(bahanKajian.getKode() + " " + bahanKajian.getNama());
					}

					Rows rows = new Rows();
					rows.setParent(grid);

					loadedCpl = capaianLulusans;
					loadedBk = bahanKajians;

					for (final CapaianLulusan capaianLulusan : capaianLulusans) {
						MyFormRow row = new MyFormRow();row.setValign("top");
						row.setParent(rows);
						row.appendChild(new Label(capaianLulusan.getKode()));
						row.appendChild(ObeBaseAction.ringkasanKeterangan(capaianLulusan.getNama()));
						for (final BahanKajian bahanKajian : bahanKajians) {
							final Checkbox checkbox = new Checkbox();
							checkbox.setTooltiptext(bahanKajian.getKode() + " " + bahanKajian.getNama());
							checkbox.setChecked(
									capaianLulusan.getBahanKajian().contains("," + bahanKajian.getId() + ","));
							row.appendChild(checkbox);
							checkbox.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									String p = capaianLulusan.getBahanKajian();
									if (checkbox.isChecked()) {
										p += p.isEmpty() ? bahanKajian.getId() + "" : "," + bahanKajian.getId();
									} else {
										p = org.apache.commons.lang3.StringUtils.replace(p, "," + bahanKajian.getId(), "");
									}
									capaianLulusan.setBahanKajian(p);
									Common.refreshUpdate(capaianLulusan);
								}
							});
						}
					}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

	}

	private void showMatriks() {
		if (loadedCpl == null || loadedBk == null || loadedCpl.isEmpty()) {
			try {
				MyMessageboxConfig.show("Klik Refresh dahulu untuk memuat data.", "Info",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/obe/CapaianLulusanVsBahanKajianAction.java:312"); /* ignore */ }
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<style>");
		sb.append(".mx{font-family:Arial,sans-serif;padding:14px;color:#1e293b;overflow-x:auto}");
		sb.append(".mx h3{margin:0 0 10px;color:#1e40af;font-size:13pt}");
		sb.append(".mxt{border-collapse:collapse;font-size:9pt}");
		sb.append(".mxt th,.mxt td{border:1px solid #cbd5e1;padding:4px 6px;text-align:center}");
		sb.append(".lkode{background:#dbeafe;text-align:left;white-space:nowrap;font-weight:bold;position:sticky;left:0;z-index:1}");
		sb.append(".lnama{background:#f0f7ff;text-align:left;font-size:8pt;max-width:240px}");
		sb.append(".hcol{background:#dbeafe;font-weight:bold;writing-mode:vertical-rl;transform:rotate(180deg);height:80px;min-width:30px;cursor:default}");
		sb.append(".yes{background:#16a34a;color:#fff;font-weight:bold}");
		sb.append(".no{background:#f8fafc}");
		sb.append(".tot{background:#fef9c3;font-weight:bold}");
		sb.append(".corner{background:#e2e8f0;font-size:8pt;font-weight:bold}");
		sb.append(".summ{font-size:9pt;color:#64748b;margin-top:8px;padding:6px 0}");
		sb.append("</style>");
		sb.append("<div class='mx'>");
		sb.append("<h3>Matriks CPL &times; Bahan Kajian (BK)</h3>");
		sb.append("<table class='mxt'><thead><tr>");
		sb.append("<th class='corner lkode'>CPL</th><th class='corner lnama'>Deskripsi CPL</th>");
		for (BahanKajian bk : loadedBk) {
			sb.append("<th class='hcol' title='").append(esc(bk.getNama())).append("'>")
			  .append(esc(bk.getKode())).append("</th>");
		}
		sb.append("<th class='corner tot'>#</th></tr></thead><tbody>");
		int[] colTotals = new int[loadedBk.size()];
		int grand = 0;
		for (CapaianLulusan cpl : loadedCpl) {
			int rowTotal = 0;
			sb.append("<tr><td class='lkode'>").append(esc(cpl.getKode())).append("</td>");
			sb.append("<td class='lnama'>").append(esc(cpl.getNama())).append("</td>");
			for (int i = 0; i < loadedBk.size(); i++) {
				boolean mapped = cpl.getBahanKajian().contains("," + loadedBk.get(i).getId() + ",");
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
		int max = loadedCpl.size() * loadedBk.size();
		int pct = max == 0 ? 0 : grand * 100 / max;
		sb.append("<div class='summ'>Cakupan: <b>").append(grand).append("/").append(max)
		  .append("</b> (").append(pct).append("%) &mdash; ")
		  .append(loadedCpl.size()).append(" CPL &times; ").append(loadedBk.size()).append(" BK</div>");
		sb.append("</div>");
		MyWindow win = new MyWindow("Matriks CPL × Bahan Kajian", "normal", true);
		win.setWidth("90%");
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
