package ais.action.master.dashboard.akunting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;

import ais.action.maintenance.MainAction;
import ais.action.master.akunting.KasKecilAction;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.KasKecil;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

import ais.ui.util.DashboardModernHtmlUtil;
public class DasboardKasKecil extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Center center;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private MyDatebox start;
	private MyDatebox end;
	private int width = 750;
	private int height = 100;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Grid grid;

	public DasboardKasKecil() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DasboardKasKecil(String title, String border, boolean closable) {
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

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);
		borderlayout.setStyle("min-height:25000px");
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getUserId() != null) {
			Integer desktopHeight = MainAction.desktopHeights.get(tbmuser.getUserId());
			if (desktopHeight != null) {
				borderlayout.setStyle("min-height:" + (desktopHeight * 0.9) + "px");
			}
		}

		North north = new North();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(borderlayout);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(north);
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

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal")));
		row.appendChild(start = new MyDatebox());
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("sd")));
		row.appendChild(end = new MyDatebox());

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 12);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		start.addEventListener("onChange", eventListener);
		end.addEventListener("onChange", eventListener);

		center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		Common.createDefaultTimer(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "6");
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DasboardKasKecil.this.grid);
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

		TreeMap<Integer, String> treeMap = new TreeMap<Integer, String>();
		treeMap.put(1, "Pengajuan");
		treeMap.put(2, "Disetujui");
		treeMap.put(3, "Ditolak");
		treeMap.put(4, "Penggantian");
		treeMap.put(5, "Ditransfer");

		final TreeMap<Integer, Criterion> treeMapCriterion = new TreeMap<Integer, Criterion>();

		Criterion criterion = Restrictions.and(Restrictions.isNull("penggantianKasKecil.disetujuiOleh"),
				Restrictions.and(Restrictions.isNull("prosesTransfer.realisasikanOleh"),
						Restrictions.or(Restrictions.eq("penggantianKasKecil.status", KasKecil.PENGAJUAN),
								Restrictions.eq("status", KasKecil.PENGAJUAN))));
		treeMapCriterion.put(1, criterion);
		criterion = Restrictions.and(Restrictions.isNull("penggantianKasKecil.disetujuiOleh"),
				Restrictions.and(Restrictions.isNull("prosesTransfer.realisasikanOleh"),
						Restrictions.or(Restrictions.eq("penggantianKasKecil.status", KasKecil.DISETUJU),
								Restrictions.eq("status", KasKecil.DISETUJU))));
		treeMapCriterion.put(2, criterion);
		criterion = Restrictions.and(Restrictions.isNull("penggantianKasKecil.disetujuiOleh"),
				Restrictions.and(Restrictions.isNull("prosesTransfer.realisasikanOleh"),
						Restrictions.or(Restrictions.eq("penggantianKasKecil.status", KasKecil.DITOLAK),
								Restrictions.eq("status", KasKecil.DITOLAK))));
		treeMapCriterion.put(3, criterion);
		criterion = Restrictions.and(Restrictions.isNotNull("penggantianKasKecil.disetujuiOleh"),
				Restrictions.isNull("prosesTransfer.realisasikanOleh"));
		treeMapCriterion.put(4, criterion);
		criterion = Restrictions.and(Restrictions.isNotNull("prosesTransfer.realisasikanOleh"),
				Restrictions.isNotNull("penggantianKasKecil.disetujuiOleh"));
		treeMapCriterion.put(5, criterion);

		Map<Integer, MyColumnConfig> listCols = new HashMap<Integer, MyColumnConfig>();
		for (Integer status : treeMap.keySet()) {
			String nama = treeMap.get(status);
			column = new MyColumnConfig(nama);
			column.setWidth("5%");
			column.setAlign("right");
			listCols.put(status, column);
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
		Map<Integer, Integer> listTotals = new HashMap<Integer, Integer>();
		Map<Long, List<Integer>> mapData = new HashMap<Long, List<Integer>>();
		for (final SatuanKerja satuanKerja : satuanKerjas) {
			List<Integer> data = mapData.get(satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId());
			if (data == null) {
				data = new ArrayList<Integer>();
				mapData.put(satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId(), data);
			}
			for (Integer status : treeMap.keySet()) {

				Criteria criteria = HibernateUtil.currentSession().createCriteria(KasKecil.class)
						.add(Restrictions.sqlRestriction(satker))
						.add(Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
								+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
								+ Common.databaseDateFormat.get().format(end.getValue()) + "')"))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.rowCount())
						.add(satuanKerja == null ? Restrictions.isNull("satuanKerja")
								: Restrictions.eq("satuanKerja", satuanKerja))

						.createAlias("penggantianKasKecil", "penggantianKasKecil", Criteria.LEFT_JOIN)
						.createAlias("penggantianKasKecil.daftarPengajuanTransfer", "daftarPengajuanTransfer",
								Criteria.LEFT_JOIN)
						.createAlias("daftarPengajuanTransfer.prosesTransfer", "prosesTransfer", Criteria.LEFT_JOIN);

				Integer count = ((Number) criteria.add(treeMapCriterion.get(status)).uniqueResult()).intValue();

				data.add(count);
				Integer colCount = listTotals.get(status);
				if (colCount == null) {
					colCount = 0;
				}
				colCount += count;
				listTotals.put(status, colCount);

			}
		}

		for (final SatuanKerja satuanKerja : satuanKerjas) {
			List<Integer> data = mapData.get(satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId());
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.appendChild(new MyLabelBoldAja(satuanKerja == null ? "Tidak Ditentukan" : satuanKerja.getNama()));
			int jml = 0;
			int i = 0;
			for (final Integer status : treeMap.keySet()) {
				Integer colCount = listTotals.get(status);
				if (colCount == null) {
					colCount = 0;
				}
				if (colCount > 0) {
					int count = data.get(i);
					jml += count;

					if (count > 0) {
						categoryModel.setValue(satuanKerja == null ? "Tidak Ditentukan" : satuanKerja.getNama(),
								treeMap.get(status), count);
					}
					A a = new A(count + "");
					a.setStyle("font-size:12px;");
					a.setParent(row);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							EventListener eventListener = (EventListener) Common
									.cetakDataCustomButton(KasKecil.class, new DataCriteriaWithColumn() {

										@Override
										public Object[] initCriteria(boolean order) {

											try {

												Criteria criteria = HibernateUtil.currentSession()
														.createCriteria(KasKecil.class)
														.add(Restrictions.sqlRestriction(
																"date(this_.tanggal_persetujuan) between date('"
																		+ Common.databaseDateFormat.get()
																				.format(start.getValue())
																		+ "') and date('"
																		+ Common.databaseDateFormat.get()
																				.format(end.getValue())
																		+ "')"))
														.add(Restrictions.sqlRestriction(satker))
														.add(Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true)))
														.add(satuanKerja == null ? Restrictions.isNull("satuanKerja")
																: Restrictions.eq("satuanKerja", satuanKerja))

														.createAlias("penggantianKasKecil", "penggantianKasKecil",
																Criteria.LEFT_JOIN)
														.createAlias("penggantianKasKecil.daftarPengajuanTransfer",
																"daftarPengajuanTransfer", Criteria.LEFT_JOIN)
														.createAlias("daftarPengajuanTransfer.prosesTransfer",
																"prosesTransfer", Criteria.LEFT_JOIN);

												criteria.add(treeMapCriterion.get(status));

												return new Object[] { criteria, KasKecilAction.contents };

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
							.cetakDataCustomButton(KasKecil.class, new DataCriteriaWithColumn() {

								@Override
								public Object[] initCriteria(boolean order) {

									try {

										Criteria criteria = HibernateUtil.currentSession()
												.createCriteria(KasKecil.class)
												.add(Restrictions
														.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
																+ Common.databaseDateFormat.get().format(start.getValue())
																+ "') and date('"
																+ Common.databaseDateFormat.get().format(end.getValue())
																+ "')"))
												.add(Restrictions.sqlRestriction(satker))
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))
												.add(satuanKerja == null ? Restrictions.isNull("satuanKerja")
														: Restrictions.eq("satuanKerja", satuanKerja))

												.createAlias("penggantianKasKecil", "penggantianKasKecil",
														Criteria.LEFT_JOIN)
												.createAlias("penggantianKasKecil.daftarPengajuanTransfer",
														"daftarPengajuanTransfer", Criteria.LEFT_JOIN)
												.createAlias("daftarPengajuanTransfer.prosesTransfer", "prosesTransfer",
														Criteria.LEFT_JOIN);

										Criterion criterion = Restrictions.sqlRestriction("false");
										for (Criterion c : treeMapCriterion.values()) {
											criterion = Restrictions.or(criterion, c);
										}
										criteria.add(criterion);

										return new Object[] { criteria, KasKecilAction.contents };

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
		for (final Integer status : treeMap.keySet()) {
			Integer colCount = listTotals.get(status);
			if (colCount == null) {
				colCount = 0;
			}
			totalCol += colCount;
			if (colCount > 0) {
				countCol++;
				listCols.get(status).setParent(columns);

				A a = new A(colCount + "");
				a.setStyle("font-size:16px;font-weight: bolder;");
				a.setParent(row);
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						EventListener eventListener = (EventListener) Common
								.cetakDataCustomButton(KasKecil.class, new DataCriteriaWithColumn() {

									@Override
									public Object[] initCriteria(boolean order) {

										try {

											Criteria criteria = HibernateUtil.currentSession()
													.createCriteria(KasKecil.class)
													.add(Restrictions.sqlRestriction(
															"date(this_.tanggal_persetujuan) between date('"
																	+ Common.databaseDateFormat.get().format(start.getValue())
																	+ "') and date('"
																	+ Common.databaseDateFormat.get().format(end.getValue())
																	+ "')"))
													.add(Restrictions.sqlRestriction(satker))
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))

													.createAlias("penggantianKasKecil", "penggantianKasKecil",
															Criteria.LEFT_JOIN)
													.createAlias("penggantianKasKecil.daftarPengajuanTransfer",
															"daftarPengajuanTransfer", Criteria.LEFT_JOIN)
													.createAlias("daftarPengajuanTransfer.prosesTransfer",
															"prosesTransfer", Criteria.LEFT_JOIN);

											criteria.add(treeMapCriterion.get(status));

											return new Object[] { criteria, KasKecilAction.contents };

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
						.cetakDataCustomButton(KasKecil.class, new DataCriteriaWithColumn() {

							@Override
							public Object[] initCriteria(boolean order) {

								try {

									Criteria criteria = HibernateUtil.currentSession().createCriteria(KasKecil.class)
											.add(Restrictions
													.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
															+ Common.databaseDateFormat.get().format(start.getValue())
															+ "') and date('"
															+ Common.databaseDateFormat.get().format(end.getValue()) + "')"))
											.add(Restrictions.sqlRestriction(satker))
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))

											.createAlias("penggantianKasKecil", "penggantianKasKecil",
													Criteria.LEFT_JOIN)
											.createAlias("penggantianKasKecil.daftarPengajuanTransfer",
													"daftarPengajuanTransfer", Criteria.LEFT_JOIN)
											.createAlias("daftarPengajuanTransfer.prosesTransfer", "prosesTransfer",
													Criteria.LEFT_JOIN);

									Criterion criterion = Restrictions.sqlRestriction("false");
									for (Criterion c : treeMapCriterion.values()) {
										criterion = Restrictions.or(criterion, c);
									}
									criteria.add(criterion);

									return new Object[] { criteria, KasKecilAction.contents };

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
		for (Integer status : treeMap.keySet()) {
			Integer colCount = listTotals.get(status);
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

		row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, "Dasbor Kas Kecil", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
}
}
