package ais.action.master.obe;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataMatakuliahBanyak;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Matakuliah;
import ais.database.model.PerguruanTinggi;
import ais.database.model.obe.BahanKajian;
import ais.database.model.obe.CapaianLulusan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class BahanKajianVsCapaianLulusanVsMatakuliahAction extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Combobox fakultas;
	private Combobox jurusan;

	private Center center;

	public BahanKajianVsCapaianLulusanVsMatakuliahAction() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public BahanKajianVsCapaianLulusanVsMatakuliahAction(String title, String border, boolean closable)
			throws Exception {
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
		west.setHeight("72px");

		// Hindari transformasi otomatis MyGrid ketika menjadi anak langsung North.
		Div filterContainer = new Div();
		filterContainer.setWidth("100%");
		filterContainer.setHeight("100%");
		filterContainer.setParent(west);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(filterContainer);
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
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

		row.appendChild(new ais.ui.util.MyLabelConfig("Bahan Kajian"));
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
					Session session = HibernateUtil.currentSession();
					Jurusan jurusan = (Jurusan) BahanKajianVsCapaianLulusanVsMatakuliahAction.this.jurusan
							.getSelectedItem().getValue();
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

					List<BahanKajian> bahanKajians = ConstantValues.simpleList(session.createCriteria(BahanKajian.class)

							.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(
											Restrictions.ilike("kode", nama.getValue().trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE)))

							.add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", jurusan)))
							.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							BahanKajian.class);

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

					column = new MyColumnConfig("Bahan Kajian");
					column.setParent(columns);
					column.setWidth("300px");

					for (CapaianLulusan capaianLulusan : capaianLulusans) {
						column = new MyColumnConfig();

						column.appendChild(RevisiHelper.createNewRevisi(CapaianLulusan.class, capaianLulusan,
								capaianLulusan.getKode()));

						column.setParent(columns);
						column.setWidth("100px");
						column.setTooltiptext(capaianLulusan.getKode() + " " + capaianLulusan.getNama());

					}

					Rows rows = new Rows();
					rows.setParent(grid);

					for (final BahanKajian bahanKajian : bahanKajians) {

						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);
						row.appendChild(new Label(bahanKajian.getKode()));

						row.appendChild(
								RevisiHelper.createNewRevisi(BahanKajian.class, bahanKajian, bahanKajian.getNama()));

						for (final CapaianLulusan capaianLulusan : capaianLulusans) {

							final String kodeFinal = capaianLulusan.getId() + "|" + bahanKajian.getId();

							Criterion criterion = Restrictions.ilike("capaianLulusan", "," + kodeFinal + ",",
									MatchMode.ANYWHERE);

							Criterion criterion1 = Restrictions.ilike("bahanKajian", "," + kodeFinal + ",",
									MatchMode.ANYWHERE);

							final List<Matakuliah> matakuliahsData = ConstantValues.simpleList(
									session.createCriteria(Matakuliah.class).add(criterion).add(criterion1)
											.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")).add(Restrictions
													.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
									Matakuliah.class);

							MyGrid subGrid = new MyGrid();
							subGrid.setWidth("100%");
							subGrid.setParent(row);
							subGrid.setHeight("100%");

							subGrid.setTooltip(capaianLulusan.getKode() + " " + capaianLulusan.getNama());

							Columns subcolumns = new Columns();
							subcolumns.setParent(subGrid);

							MyColumnConfig subcolumn = new MyColumnConfig();
							subcolumn.setParent(subcolumns);
							subcolumn.setWidth("70%");

							subcolumn = new MyColumnConfig();
							subcolumn.setParent(subcolumns);

							Rows subrows = new Rows();
							subrows.setParent(subGrid);

							for (final Matakuliah matakuliah : matakuliahsData) {
								MyFormRow subrow = new MyFormRow();
								subrow.setParent(subrows);
								Vbox a = RevisiHelper.createNewRevisi(Matakuliah.class, matakuliah,
										matakuliah.getKode());
								subrow.setTooltip(matakuliah.getKode() + " " + matakuliah.getNama());
								a.setParent(subrow);

								MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
								button.setTooltiptext("Hapus Data");
								button.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										MyMessageboxConfig.show("Apakah yakin ingin menghapus matakuliah ini ini ?",
												"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
												MyMessageboxConfig.QUESTION, new EventListener() {

													@Override
													public void onEvent(Event event) throws Exception {
														int i = Integer.parseInt(event.getData().toString());
														if (i == MyMessageboxConfig.OK) {
															try {

																Session session = HibernateUtil.currentSession();
																session.refresh(matakuliah);

																String p = matakuliah.getCapaianLulusan();

																p = org.apache.commons.lang3.StringUtils.replace(p,
																		"," + kodeFinal + ",", "");

																matakuliah.setCapaianLulusan(p);

																System.out.println("setCapaianLulusan -> " + p);

																p = matakuliah.getBahanKajian();

																p = org.apache.commons.lang3.StringUtils.replace(p,
																		"," + kodeFinal + ",", "");

																matakuliah.setBahanKajian(p);

																System.out.println("setBahanKajian -> " + p);

																Common.refreshUpdate(session, matakuliah);
																session.flush();

																onKHS(event);

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
																MyMessageboxConfig.show(
																		"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
																				+ e.getMessage());
															}

														}

													}
												});

									}
								});
								button.setParent(subrow);
							}

							MyFormRow subrow = new MyFormRow();
							ais.ui.util.ZkCompat.setSpans(subrow, "2");
							subrow.setParent(subrows);

							MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah", "/img/svg/addthis.svg");
							button.setTooltiptext("Hapus Data");
							button.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {

									Jurusan jurusan = (Jurusan) BahanKajianVsCapaianLulusanVsMatakuliahAction.this.jurusan
											.getSelectedItem().getValue();

									AmbilDataMatakuliahBanyak ambilDataMatakuliahBanyak = new AmbilDataMatakuliahBanyak(
											matakuliahsData, jurusan);
									ambilDataMatakuliahBanyak
											.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
									ambilDataMatakuliahBanyak.setHeight("95%");
									ambilDataMatakuliahBanyak.setWidth("700px");

									ambilDataMatakuliahBanyak.setEventListener(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											List<Matakuliah> matakuliahes = (List<Matakuliah>) arg0.getData();
											Session session = HibernateUtil.currentSession();
											for (Matakuliah matakuliah : matakuliahes) {

												session.refresh(matakuliah);

												String p = matakuliah.getCapaianLulusan();
												p += p.isEmpty() ? kodeFinal + "" : "," + kodeFinal;
												matakuliah.setCapaianLulusan(p);

												System.out.println("setCapaianLulusan -> " + p);

												p = matakuliah.getBahanKajian();

												p += p.isEmpty() ? kodeFinal + "" : "," + kodeFinal;
												System.out.println("setBahanKajian -> " + p);
												matakuliah.setBahanKajian(p);

												Common.refreshUpdate(session, matakuliah);
												session.flush();
											}
											onKHS(arg0);
										}
									});

									ambilDataMatakuliahBanyak.onModal();

								}
							});
							button.setParent(subrow);

						}
					}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

	}

}
