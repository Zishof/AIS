package ais.action.master.dashboard.surat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
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
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.action.maintenance.MainAction;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.surat.SuratMasukAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.LokerSurat;
import ais.database.model.surat.SuratMasuk;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

public class DasboardSuratMasukLokerSurat extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Center center;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private int width = 750;
	private int height = 100;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Grid grid;
	private MyDatebox start;
	private MyDatebox end;
	private String tipe;

	public DasboardSuratMasukLokerSurat(String tipe) {
		super();
		this.tipe = tipe;
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DasboardSuratMasukLokerSurat(String tipe, String title, String border, boolean closable) {
		super(title, border, closable);
		this.tipe = tipe;
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
		searchparent.setWidth("95%");
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
		ais.ui.util.ZkCompat.setSpans(row, "8");
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DasboardSuratMasukLokerSurat.this.grid);
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
		ais.ui.util.ZkCompat.setFixedLayout(grid, true);

		Columns contents = new Columns();
		contents.setParent(grid);

		List<LokerSurat> lokers = ConstantValues.simpleList(HibernateUtil.currentSession()
				.createCriteria(LokerSurat.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")),
				LokerSurat.class);

		MyColumnConfig columnUtama = new MyColumnConfig("Satuan Kerja");
		columnUtama.setParent(contents);

		new Thread(new Runnable() {

			@Override
			public void run() {
				Session session = null;
				List<SuratMasuk> suratMasuks = null;
				try {
					session = HibernateUtil.currentNativeSession();
					suratMasuks = session.createCriteria(SuratMasuk.class)
							.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))
							.add(Restrictions.isNull("loker")).list();
					session.getTransaction().begin();
					for (SuratMasuk suratMasuk : suratMasuks) {
						session.update(suratMasuk);
					}
					session.getTransaction().commit();
				} catch (Exception e) {
					try { if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) session.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/dashboard/surat/DasboardSuratMasukLokerSurat.java:220");}
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/surat/DasboardSuratMasukLokerSurat.java:221");
				} finally {
					try { if (suratMasuks != null) suratMasuks.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/surat/DasboardSuratMasukLokerSurat.java:223");}
					try { if (session != null) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/surat/DasboardSuratMasukLokerSurat.java:224");}
					try { if (session != null) session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/surat/DasboardSuratMasukLokerSurat.java:225");}
					try { if (session != null && session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/surat/DasboardSuratMasukLokerSurat.java:226");}
				}
			}
		}).start();

		lokers.add(null);
		Map<Long, MyColumnConfig> listCols = new HashMap<Long, MyColumnConfig>();
		for (LokerSurat loker : lokers) {
			MyColumnConfig column = new MyColumnConfig(loker == null ? "Tidak ditentukan" : loker.getNama());
			column.setTooltiptext(loker == null ? "Tidak ditentukan" : loker.getNama());
			column.setWidth("5%");
			column.setAlign("right");
			listCols.put(loker == null || loker.getId() == null ? -1L : loker.getId(), column);
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

		HtmlCategoryModel categoryModel = new HtmlCategoryModel();
		categoryModel.clear();
		Map<Long, Integer> listTotals = new HashMap<Long, Integer>();
		Map<Long, List<Integer>> mapData = new HashMap<Long, List<Integer>>();
		for (final SatuanKerja satuanKerja : satuanKerjas) {
			List<Integer> data = mapData.get(satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId());
			if (data == null) {
				data = new ArrayList<Integer>();
				mapData.put(satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId(), data);
			}
			for (LokerSurat loker : lokers) {
				Integer count = ((Number) HibernateUtil.currentSession().createCriteria(SuratMasuk.class)
						.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))
						.add(Restrictions.sqlRestriction("date(this_.tanggalsurat) between date('"
								+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
								+ Common.databaseDateFormat.get().format(end.getValue()) + "')"))

						.add(Restrictions.sqlRestriction(satker))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.rowCount())
						.add(satuanKerja == null ? Restrictions.isNull("satuanKerja")
								: Restrictions.eq("satuanKerja", satuanKerja))
						.add(loker == null ? Restrictions.isNull("loker") : Restrictions.eq("loker", loker))

						.uniqueResult()).intValue();
				data.add(count);
				Integer colCount = listTotals.get(loker == null || loker.getId() == null ? -1L : loker.getId());
				if (colCount == null) {
					colCount = 0;
				}
				colCount += count;
				listTotals.put(loker == null || loker.getId() == null ? -1L : loker.getId(), colCount);

			}
		}
		Set<String> countData = new HashSet<String>();
		for (final SatuanKerja satuanKerja : satuanKerjas) {
			List<Integer> data = mapData.get(satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId());
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.appendChild(new MyLabelBoldAja(satuanKerja == null ? "Tidak Ditentukan" : satuanKerja.getNama()));
			int jml = 0;
			int i = 0;
			for (final LokerSurat loker : lokers) {
				Integer colCount = listTotals.get(loker == null || loker.getId() == null ? -1L : loker.getId());
				if (colCount == null) {
					colCount = 0;
				}
				if (colCount > 0) {

					countData.add((loker == null ? "" : "E" + loker.getId()));
					int count = data.get(i);
					jml += count;

					if (count > 0) {
						countData.add((satuanKerja == null ? "" : "S" + satuanKerja.getId()));
						categoryModel.setValue(satuanKerja == null ? "Tidak Ditentukan" : satuanKerja.getNama(),
								loker == null ? "Tidak ditentukan" : loker.getNama(), count);
					}
					A a = new A(count + "");
					a.setStyle("font-size:12px;");
					a.setParent(row);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							EventListener eventListener = (EventListener) Common
									.cetakDataCustomButton(SuratMasuk.class, new DataCriteriaWithColumn() {

										@Override
										public Object[] initCriteria(boolean order) {

											try {

												Criteria criteria = HibernateUtil.currentSession()
														.createCriteria(SuratMasuk.class)
														.add(Restrictions.or(Restrictions.isNull("tipe"),
																Restrictions.eq("tipe", tipe)))
														.add(Restrictions.sqlRestriction(
																"date(this_.tanggalsurat) between date('"
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

														.add(loker == null ? Restrictions.isNull("loker")
																: Restrictions.eq("loker", loker));

												return new Object[] { criteria, SuratMasukAction.contents };

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
							.cetakDataCustomButton(SuratMasuk.class, new DataCriteriaWithColumn() {

								@Override
								public Object[] initCriteria(boolean order) {

									try {

										Criteria criteria = HibernateUtil.currentSession()
												.createCriteria(SuratMasuk.class)
												.add(Restrictions.or(Restrictions.isNull("tipe"),
														Restrictions.eq("tipe", tipe)))
												.add(Restrictions
														.sqlRestriction("date(this_.tanggalsurat) between date('"
																+ Common.databaseDateFormat.get().format(start.getValue())
																+ "') and date('"
																+ Common.databaseDateFormat.get().format(end.getValue())
																+ "')"))

												.add(Restrictions.sqlRestriction(satker))
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))
												.add(satuanKerja == null ? Restrictions.isNull("satuanKerja")
														: Restrictions.eq("satuanKerja", satuanKerja));

										return new Object[] { criteria, SuratMasukAction.contents };

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
		for (final LokerSurat loker : lokers) {
			Integer colCount = listTotals.get(loker == null || loker.getId() == null ? -1L : loker.getId());
			if (colCount == null) {
				colCount = 0;
			}
			totalCol += colCount;
			if (colCount > 0) {
				countCol++;
				listCols.get(loker == null || loker.getId() == null ? -1L : loker.getId()).setParent(contents);

				A a = new A(colCount + "");
				a.setStyle("font-size:16px;font-weight: bolder;");
				a.setParent(row);
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						EventListener eventListener = (EventListener) Common
								.cetakDataCustomButton(SuratMasuk.class, new DataCriteriaWithColumn() {

									@Override
									public Object[] initCriteria(boolean order) {

										try {

											Criteria criteria = HibernateUtil.currentSession()
													.createCriteria(SuratMasuk.class)
													.add(Restrictions.or(Restrictions.isNull("tipe"),
															Restrictions.eq("tipe", tipe)))

													.add(Restrictions
															.sqlRestriction("date(this_.tanggalsurat) between date('"
																	+ Common.databaseDateFormat.get().format(start.getValue())
																	+ "') and date('"
																	+ Common.databaseDateFormat.get().format(end.getValue())
																	+ "')"))

													.add(Restrictions.sqlRestriction(satker))
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))
													.add(loker == null ? Restrictions.isNull("loker")
															: Restrictions.eq("loker", loker));

											return new Object[] { criteria, SuratMasukAction.contents };

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

		if (countCol > 15) {
			columnUtama.setWidth("15%");
		}

		A a = new A(totalCol + "");
		a.setStyle("font-size:16px;font-weight: bolder;");
		a.setParent(row);
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				EventListener eventListener = (EventListener) Common
						.cetakDataCustomButton(SuratMasuk.class, new DataCriteriaWithColumn() {

							@Override
							public Object[] initCriteria(boolean order) {

								try {

									Criteria criteria = HibernateUtil.currentSession().createCriteria(SuratMasuk.class)
											.add(Restrictions.or(Restrictions.isNull("tipe"),
													Restrictions.eq("tipe", tipe)))
											.add(Restrictions.sqlRestriction("date(this_.tanggalsurat) between date('"
													+ Common.databaseDateFormat.get().format(start.getValue())
													+ "') and date('" + Common.databaseDateFormat.get().format(end.getValue())
													+ "')"))

											.add(Restrictions.sqlRestriction(satker)).add(Restrictions
													.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

									return new Object[] { criteria, SuratMasukAction.contents };

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

		MyColumnConfig column = new MyColumnConfig("Total");
		column.setWidth("5%");
		column.setTooltiptext("Total");
		column.setAlign("right");
		column.setParent(contents);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelBolder("Persen"));
		for (LokerSurat loker : lokers) {
			Integer colCount = listTotals.get(loker == null || loker.getId() == null ? -1L : loker.getId());
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

		Html mychart = new Html(buildModernDashboardSuratChartHtml("Ringkasan Grafik Surat",
				"Grafik ini memakai HTML/CSS modern yang ringan. Data tetap menampilkan perbandingan jumlah surat berdasarkan satuan kerja dan kategori yang dipilih.",
				categoryModel));
		row.appendChild(mychart);
		mychart.setStyle("display:block;width:100%;");

	}

	private static class HtmlCategoryModel {
		private Map<String, Map<String, Number>> values = new LinkedHashMap<String, Map<String, Number>>();

		public void clear() {
			values.clear();
		}

		public void setValue(String series, String category, Number value) {
			if (series == null || series.trim().length() == 0) {
				series = "Tidak Ditentukan";
			}
			if (category == null || category.trim().length() == 0) {
				category = "Tidak Ditentukan";
			}
			Map<String, Number> row = values.get(series);
			if (row == null) {
				row = new LinkedHashMap<String, Number>();
				values.put(series, row);
			}
			row.put(category, value == null ? Integer.valueOf(0) : value);
		}

		public Map<String, Map<String, Number>> getValues() {
			return values;
		}

		public int getTotal() {
			int total = 0;
			for (Map<String, Number> row : values.values()) {
				for (Number number : row.values()) {
					total += number == null ? 0 : number.intValue();
				}
			}
			return total;
		}

		public int getMaxRowTotal() {
			int max = 0;
			for (Map<String, Number> row : values.values()) {
				int total = 0;
				for (Number number : row.values()) {
					total += number == null ? 0 : number.intValue();
				}
				if (total > max) {
					max = total;
				}
			}
			return max <= 0 ? 1 : max;
		}
	}

	private String buildModernDashboardSuratChartHtml(String title, String description, HtmlCategoryModel model) {
		StringBuilder sb = new StringBuilder();
		if (model == null || model.getValues().isEmpty()) {
			sb.append("<div style='padding:18px;border-radius:18px;background:#fff;border:1px solid #e5e7eb;")
					.append("box-shadow:0 12px 26px rgba(15,23,42,.08);font-family:Arial,sans-serif;color:#334155;'>")
					.append("<div style='font-size:16px;font-weight:900;color:#0f172a;'>").append(safeHtml(title))
					.append("</div>")
					.append("<div style='font-size:12px;line-height:1.55;color:#64748b;margin-top:6px;'>")
					.append(safeHtml(description)).append("</div>")
					.append("<div style='margin-top:14px;padding:12px;border-radius:14px;background:#f8fafc;border:1px dashed #cbd5e1;'>")
					.append("Belum ada data yang cocok dengan filter saat ini.</div></div>");
			return sb.toString();
		}

		int total = model.getTotal();
		int maxRow = model.getMaxRowTotal();
		String[] colors = new String[] { "#2563eb", "#16a34a", "#f59e0b", "#dc2626", "#7c3aed", "#0891b2",
				"#db2777", "#4f46e5", "#65a30d", "#9333ea" };

		sb.append("<div style='padding:18px;border-radius:18px;background:#fff;border:1px solid #e5e7eb;")
				.append("box-shadow:0 12px 26px rgba(15,23,42,.08);font-family:Arial,sans-serif;color:#334155;'>");
		sb.append("<div style='display:flex;align-items:flex-start;justify-content:space-between;gap:12px;flex-wrap:wrap;'>")
				.append("<div style='min-width:240px;flex:1;'>")
				.append("<div style='font-size:11px;letter-spacing:.12em;text-transform:uppercase;color:#0f766e;font-weight:900;'>")
				.append("Grafik HTML/CSS</div>")
				.append("<div style='font-size:18px;font-weight:900;color:#0f172a;margin-top:4px;'>")
				.append(safeHtml(title)).append("</div>")
				.append("<div style='font-size:12px;line-height:1.55;color:#64748b;margin-top:7px;'>")
				.append(safeHtml(description)).append("</div>")
				.append("</div>")
				.append("<div style='padding:10px 14px;border-radius:16px;background:#ecfdf5;border:1px solid #bbf7d0;text-align:right;'>")
				.append("<div style='font-size:11px;color:#166534;font-weight:800;'>Total Dokumen</div>")
				.append("<div style='font-size:24px;color:#14532d;font-weight:900;'>").append(formatNumber(total))
				.append("</div></div></div>");

		sb.append("<div style='margin-top:16px;display:flex;gap:8px;flex-wrap:wrap;'>");
		int categoryIndex = 0;
		Map<String, String> categoryColors = new LinkedHashMap<String, String>();
		for (Map<String, Number> row : model.getValues().values()) {
			for (String category : row.keySet()) {
				if (!categoryColors.containsKey(category)) {
					categoryColors.put(category, colors[categoryIndex % colors.length]);
					categoryIndex++;
				}
			}
		}
		for (Map.Entry<String, String> entry : categoryColors.entrySet()) {
			sb.append("<span style='display:inline-flex;align-items:center;gap:6px;font-size:11px;color:#334155;")
					.append("background:#f8fafc;border:1px solid #e2e8f0;border-radius:999px;padding:5px 9px;'>")
					.append("<i style='display:inline-block;width:10px;height:10px;border-radius:999px;background:")
					.append(entry.getValue()).append(";'></i>")
					.append(safeHtml(entry.getKey())).append("</span>");
		}
		sb.append("</div>");

		sb.append("<div style='margin-top:16px;display:flex;flex-direction:column;gap:12px;'>");
		for (Map.Entry<String, Map<String, Number>> rowEntry : model.getValues().entrySet()) {
			int rowTotal = 0;
			for (Number number : rowEntry.getValue().values()) {
				rowTotal += number == null ? 0 : number.intValue();
			}
			double rowPercent = maxRow <= 0 ? 0.0 : (rowTotal * 100.0 / maxRow);
			if (rowPercent < 3.0 && rowTotal > 0) {
				rowPercent = 3.0;
			}
			sb.append("<div style='padding:12px;border:1px solid #e5e7eb;border-radius:15px;background:#f8fafc;'>")
					.append("<div style='display:flex;align-items:center;justify-content:space-between;gap:10px;'>")
					.append("<div style='font-size:12px;font-weight:900;color:#0f172a;'>")
					.append(safeHtml(rowEntry.getKey())).append("</div>")
					.append("<div style='font-size:12px;font-weight:900;color:#0f766e;'>")
					.append(formatNumber(rowTotal)).append("</div></div>")
					.append("<div style='height:16px;margin-top:8px;background:#e2e8f0;border-radius:999px;overflow:hidden;display:flex;'>");

			for (Map.Entry<String, Number> valueEntry : rowEntry.getValue().entrySet()) {
				int value = valueEntry.getValue() == null ? 0 : valueEntry.getValue().intValue();
				if (value <= 0 || rowTotal <= 0) {
					continue;
				}
				double width = value * 100.0 / rowTotal;
				if (width < 2.0) {
					width = 2.0;
				}
				String color = categoryColors.get(valueEntry.getKey());
				sb.append("<div title='").append(safeHtml(valueEntry.getKey())).append(": ").append(formatNumber(value))
						.append("' style='height:16px;width:").append(formatDecimal(width))
						.append("%;background:").append(color).append(";'></div>");
			}
			sb.append("</div>")
					.append("<div style='height:5px;margin-top:8px;background:#e2e8f0;border-radius:999px;overflow:hidden;'>")
					.append("<div style='height:5px;width:").append(formatDecimal(rowPercent))
					.append("%;background:linear-gradient(90deg,#0f766e,#22c55e);'></div></div>");

			sb.append("<div style='display:flex;gap:6px;flex-wrap:wrap;margin-top:8px;'>");
			for (Map.Entry<String, Number> valueEntry : rowEntry.getValue().entrySet()) {
				int value = valueEntry.getValue() == null ? 0 : valueEntry.getValue().intValue();
				if (value <= 0) {
					continue;
				}
				String color = categoryColors.get(valueEntry.getKey());
				sb.append("<span style='font-size:10px;border-radius:999px;padding:4px 7px;background:#fff;border:1px solid #e2e8f0;color:#475569;'>")
						.append("<b style='color:").append(color).append(";'>").append(formatNumber(value))
						.append("</b> ").append(safeHtml(valueEntry.getKey())).append("</span>");
			}
			sb.append("</div></div>");
		}
		sb.append("</div>");

		sb.append("<div style='margin-top:14px;padding:10px 12px;border-radius:14px;background:#eff6ff;border:1px solid #bfdbfe;")
				.append("font-size:11px;line-height:1.5;color:#1e3a8a;'>")
				.append("<b>Manfaat panel:</b> membantu membaca penyebaran surat antar satuan kerja dan kategori tanpa harus menghitung tabel secara manual. ")
				.append("Bar yang lebih panjang menunjukkan jumlah dokumen yang lebih banyak sehingga prioritas pemeriksaan lebih mudah terlihat.")
				.append("</div>");
		sb.append("</div>");
		return sb.toString();
	}

	private String safeHtml(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	private String formatNumber(int value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String formatDecimal(double value) {
		try {
			return Common.numberFormat.get().format(value).replace(",", ".");
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

}
