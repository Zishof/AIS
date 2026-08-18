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
import ais.database.model.Matakuliah;
import ais.database.model.PerguruanTinggi;
import ais.database.model.obe.BahanKajian;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class MatakuliahVsBahanKajianAction extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Combobox fakultas;
	private Combobox jurusan;

	private Center center;

	public MatakuliahVsBahanKajianAction() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public MatakuliahVsBahanKajianAction(String title, String border, boolean closable) throws Exception {
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

		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
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

					Jurusan jurusan = (Jurusan) MatakuliahVsBahanKajianAction.this.jurusan.getSelectedItem().getValue();
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

					List<Matakuliah> matakuliahs = ConstantValues.simpleList(HibernateUtil.currentSession()
							.createCriteria(Matakuliah.class)

							.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(
											Restrictions.ilike("kode", nama.getValue().trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE)))

							.add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", jurusan)))
							.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							Matakuliah.class);

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

					column = new MyColumnConfig("Matakuliah");
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

					for (final Matakuliah matakuliah : matakuliahs) {
						MyFormRow row = new MyFormRow();row.setValign("top");
						row.setParent(rows);
						row.appendChild(new Label(matakuliah.getKode()));
						row.appendChild(ObeBaseAction.ringkasanKeterangan(matakuliah.getNama()));
						for (final BahanKajian bahanKajian : bahanKajians) {
							final Checkbox checkbox = new Checkbox();
							checkbox.setTooltiptext(bahanKajian.getKode() + " " + bahanKajian.getNama());
							checkbox.setChecked(matakuliah.getBahanKajian().contains("," + bahanKajian.getId() + ","));
							row.appendChild(checkbox);
							checkbox.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									String p = matakuliah.getBahanKajian();
									if (checkbox.isChecked()) {
										p += p.isEmpty() ? bahanKajian.getId() + "" : "," + bahanKajian.getId();
									} else {
										p = org.apache.commons.lang3.StringUtils.replace(p, "," + bahanKajian.getId(), "");
									}
									matakuliah.setBahanKajian(p);
									Common.refreshUpdate(matakuliah);
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

}
