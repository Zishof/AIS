package ais.action.master.dashboard.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;

import ais.action.maintenance.MainAction;
import ais.action.master.PegawaiAction;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.TipeMasaKerja;
import ais.database.model.employ.UnitKerja;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

import ais.ui.util.DashboardModernHtmlUtil;
public class DasboardKepegawaianUnitKerja extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Div center;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private int width = 750;
	private int height = 100;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Grid grid;
	private Combobox tipeMasaKerja;

	public DasboardKepegawaianUnitKerja() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DasboardKepegawaianUnitKerja(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		org.zkoss.zk.ui.Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih satuan kerja dan jenis kerja untuk menyaring data yang ditampilkan.",
				"Sebaran Pegawai per Unit Kerja",
				"Jumlah pegawai menurut unit kerja, beserta grafiknya.");
		org.zkoss.zk.ui.Component saringanHost = hostPortal[0];
		center = (org.zkoss.zul.Div) hostPortal[1];

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(saringanHost);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}

		};

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		searchparent = new AmbilDataSatuanKerjaBanbox();
		searchparent.setEventListener(eventListener);

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		row.appendChild(new MyLabelConfig("Satuan Kerja"));
		row.appendChild(searchparent);

		row.appendChild(new MyLabelConfig("Jenis Kerja"));
		tipeMasaKerja = new Combobox();
		row.appendChild(tipeMasaKerja);
		Common.insertComboDanSemua(tipeMasaKerja, "nama", "keterangan", TipeMasaKerja.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		tipeMasaKerja.setReadonly(true);
		tipeMasaKerja.addEventListener("onChange", eventListener);

		row.appendChild(new Label());
		row.appendChild(new Label());



		Common.createDefaultTimer(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "6");
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DasboardKepegawaianUnitKerja.this.grid);
			}
		});
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void reload() {
		Common.clear(center);
		grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Satuan Kerja");
		column.setParent(columns);

		List<UnitKerja> unitKerjas = ConstantValues.simpleList(HibernateUtil.currentSession()
				.createCriteria(UnitKerja.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")),
				UnitKerja.class);

		unitKerjas.add(null);
		Map<Long, MyColumnConfig> listCols = new HashMap<Long, MyColumnConfig>();
		for (UnitKerja unitKerja : unitKerjas) {
			column = new MyColumnConfig(unitKerja == null ? "Tidak ditentukan" : unitKerja.getNama());
			column.setWidth("5%");
			column.setAlign("right");
			listCols.put(unitKerja == null || unitKerja.getId() == null ? -1L : unitKerja.getId(), column);
		}

		List<SatuanKerja> satuanKerjas;

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		if (parent != null) {
			Set<SatuanKerja> temp = new HashSet<SatuanKerja>();
			if (parent != null) {
				temp.add(parent);
				satuanKerjaTreeModel.getChildsSet(parent, temp);
			}
			satuanKerjas = new ArrayList<SatuanKerja>(temp);
			Collections.sort(satuanKerjas);
		} else {
			satuanKerjas = new ArrayList<SatuanKerja>(ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas());
			Collections.sort(satuanKerjas);
		}

		String inSatker = "";
		for (SatuanKerja satuanKerja : satuanKerjas) {
			inSatker += inSatker.isEmpty() ? satuanKerja.getId().toString() : "," + satuanKerja.getId();
		}
		final String satker = inSatker.isEmpty() ? "true"
				: "(this_.satuan_kerja in (" + inSatker + ") or this_.satuan_kerja is null)";

		satuanKerjas.add(null);

		Rows rows = new Rows();
		rows.setParent(grid);

		SimpleCategoryModel categoryModel = new SimpleCategoryModel();
		categoryModel.clear();
		Map<Long, Integer> listTotals = new HashMap<Long, Integer>();
		Map<Long, List<Integer>> mapData = new HashMap<Long, List<Integer>>();
		for (final SatuanKerja satuanKerja : satuanKerjas) {
			List<Integer> data = mapData.get(satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId());
			if (data == null) {
				data = new ArrayList<Integer>();
				mapData.put(satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId(), data);
			}
			for (UnitKerja unitKerja : unitKerjas) {
				Integer count = ((Number) HibernateUtil.currentSession().createCriteria(Pegawai.class)
						.add(Restrictions.sqlRestriction(satker))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.rowCount())
						.add(satuanKerja == null ? Restrictions.isNull("satuanKerja")
								: Restrictions.eq("satuanKerja", satuanKerja))
						.add(unitKerja == null ? Restrictions.isNull("unitKerja")
								: Restrictions.eq("unitKerja", unitKerja))
						.add(tipeMasaKerja.getSelectedItem() == null
								|| tipeMasaKerja.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("tipeMasaKerja", tipeMasaKerja.getSelectedItem().getValue()))
						.uniqueResult()).intValue();
				data.add(count);
				Integer colCount = listTotals.get(unitKerja == null || unitKerja.getId() == null ? -1L : unitKerja.getId());
				if (colCount == null) {
					colCount = 0;
				}
				colCount += count;
				listTotals.put(unitKerja == null || unitKerja.getId() == null ? -1L : unitKerja.getId(), colCount);

			}
		}

		for (final SatuanKerja satuanKerja : satuanKerjas) {
			List<Integer> data = mapData.get(satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId());
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.appendChild(new MyLabelBoldAja(satuanKerja == null ? "Tidak Ditentukan" : satuanKerja.getNama()));
			int jml = 0;
			int i = 0;
			for (final UnitKerja unitKerja : unitKerjas) {
				Integer colCount = listTotals.get(unitKerja == null || unitKerja.getId() == null ? -1L : unitKerja.getId());
				if (colCount == null) {
					colCount = 0;
				}
				if (colCount > 0) {
					int count = data.get(i);
					jml += count;

					if (count > 0) {
						categoryModel.setValue(satuanKerja == null ? "Tidak Ditentukan" : satuanKerja.getNama(),
								unitKerja == null ? "Tidak ditentukan" : unitKerja.getNama(), count);
					}
					A a = new A(count + "");
					a.setStyle("font-size:12px;");
					a.setParent(row);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							EventListener eventListener = (EventListener) Common
									.cetakDataCustomButton(Pegawai.class, new DataCriteriaWithColumn() {

										@Override
										public Object[] initCriteria(boolean order) {

											try {

												Criteria criteria = HibernateUtil.currentSession()
														.createCriteria(Pegawai.class)
														.add(Restrictions.sqlRestriction(satker))
														.add(Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true)))
														.add(satuanKerja == null ? Restrictions.isNull("satuanKerja")
																: Restrictions.eq("satuanKerja", satuanKerja))
														.add(unitKerja == null ? Restrictions.isNull("unitKerja")
																: Restrictions.eq("unitKerja", unitKerja))
														.add(tipeMasaKerja.getSelectedItem() == null
																|| tipeMasaKerja.getSelectedItem().getValue() == null
																		? Restrictions.sqlRestriction("true")
																		: Restrictions.eq("tipeMasaKerja", tipeMasaKerja
																				.getSelectedItem().getValue()));

												return new Object[] { criteria, PegawaiAction.columns };

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
											return null;
										}

									}, null, "Download Data", "/img/print.png", null, null, false, null,
											"DATA TAMBAHAN",
											new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"" })
									.getAttribute("eventListener");

							eventListener.onEvent(null);
						}
					});
				}
				i++;
			}

			A a = new A(jml + "");
			a.setStyle("font-size:12px;");
			a.setParent(row);
			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					EventListener eventListener = (EventListener) Common
							.cetakDataCustomButton(Pegawai.class, new DataCriteriaWithColumn() {

								@Override
								public Object[] initCriteria(boolean order) {

									try {

										Criteria criteria = HibernateUtil.currentSession().createCriteria(Pegawai.class)
												.add(Restrictions.sqlRestriction(satker))
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))
												.add(satuanKerja == null ? Restrictions.isNull("satuanKerja")
														: Restrictions.eq("satuanKerja", satuanKerja))
												.add(tipeMasaKerja.getSelectedItem() == null
														|| tipeMasaKerja.getSelectedItem().getValue() == null
																? Restrictions.sqlRestriction("true")
																: Restrictions.eq("tipeMasaKerja",
																		tipeMasaKerja.getSelectedItem().getValue()));

										return new Object[] { criteria, PegawaiAction.columns };

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
									return null;
								}

							}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
									new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" })
							.getAttribute("eventListener");

					eventListener.onEvent(null);
				}
			});

			if (jml > 0) {
				row.setParent(rows);
			}
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelBolder("Total"));
		int countCol = 0;
		int totalCol = 0;
		for (final UnitKerja unitKerja : unitKerjas) {
			Integer colCount = listTotals.get(unitKerja == null || unitKerja.getId() == null ? -1L : unitKerja.getId());
			if (colCount == null) {
				colCount = 0;
			}
			totalCol += colCount;
			if (colCount > 0) {
				countCol++;
				listCols.get(unitKerja == null || unitKerja.getId() == null ? -1L : unitKerja.getId()).setParent(columns);

				A a = new A(colCount + "");
				a.setStyle("font-size:16px;font-weight: bolder;");
				a.setParent(row);
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						EventListener eventListener = (EventListener) Common
								.cetakDataCustomButton(Pegawai.class, new DataCriteriaWithColumn() {

									@Override
									public Object[] initCriteria(boolean order) {

										try {

											Criteria criteria = HibernateUtil.currentSession()
													.createCriteria(Pegawai.class)
													.add(Restrictions.sqlRestriction(satker))
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))
													.add(unitKerja == null ? Restrictions.isNull("unitKerja")
															: Restrictions.eq("unitKerja", unitKerja))
													.add(tipeMasaKerja.getSelectedItem() == null
															|| tipeMasaKerja.getSelectedItem().getValue() == null
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("tipeMasaKerja", tipeMasaKerja
																			.getSelectedItem().getValue()));

											return new Object[] { criteria, PegawaiAction.columns };

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										}
										return null;
									}

								}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
										new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "" })
								.getAttribute("eventListener");

						eventListener.onEvent(null);
					}
				});
			}

		}

		A a = new A(totalCol + "");
		a.setStyle("font-size:16px;font-weight: bolder;");
		a.setParent(row);
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				EventListener eventListener = (EventListener) Common
						.cetakDataCustomButton(Pegawai.class, new DataCriteriaWithColumn() {

							@Override
							public Object[] initCriteria(boolean order) {

								try {

									Criteria criteria = HibernateUtil.currentSession().createCriteria(Pegawai.class)
											.add(Restrictions.sqlRestriction(satker))
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(tipeMasaKerja.getSelectedItem() == null
													|| tipeMasaKerja.getSelectedItem().getValue() == null
															? Restrictions.sqlRestriction("true")
															: Restrictions.eq("tipeMasaKerja",
																	tipeMasaKerja.getSelectedItem().getValue()));

									return new Object[] { criteria, PegawaiAction.columns };

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
								return null;
							}

						}, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
								new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
										"", "", "", "", "", "", "", "", "", "" })
						.getAttribute("eventListener");

				eventListener.onEvent(null);
			}
		});

		column = new MyColumnConfig("Total");
		column.setWidth("5%");
		column.setAlign("right");
		column.setParent(columns);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelBolder("Persen"));
		for (UnitKerja unitKerja : unitKerjas) {
			Integer colCount = listTotals.get(unitKerja == null || unitKerja.getId() == null ? -1L : unitKerja.getId());
			if (colCount == null) {
				colCount = 0;
			}
			if (colCount > 0) {
				new MyLabelBolder(Common.numberFormat.get().format((colCount.doubleValue() * 100.0) / totalCol) + "%")
						.setParent(row);
			}
		}
		new MyLabelBolder("100%").setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.setSpans((2 + countCol) + "");
		row.setAlign("center");

		row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, "Dasbor Kepegawaian Unit Kerja", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
}
}
