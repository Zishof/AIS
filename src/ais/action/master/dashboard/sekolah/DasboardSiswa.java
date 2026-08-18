package ais.action.master.dashboard.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.maintenance.MainAction;
import ais.action.master.sekolah.SiswaAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

import ais.ui.util.DashboardModernHtmlUtil;
public class DasboardSiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Center center;
	private int width = 750;
	private int height = 100;
	private Grid grid;

	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();

	private MyIntbox mulai = new MyIntbox();
	private MyIntbox sampai = new MyIntbox();
	private Calendar calendar;

	public DasboardSiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DasboardSiswa(String title, String border, boolean closable) {
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

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabDasbor = new MyTabConfig("Dasbor");
		tabDasbor.setParent(tabs);

		MyTabConfig tab1 = new MyTabConfig("Data");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Pendidikan");
		tab2.setParent(tabs);

//		MyTabConfig tab3 = new MyTabConfig("Status");
//		tab3.setParent(tabs);
//
//		MyTabConfig tab4 = new MyTabConfig("Agama");
//		tab4.setParent(tabs);
//
//		MyTabConfig tab5 = new MyTabConfig("Usia");
//		tab5.setParent(tabs);
//
//		MyTabConfig tab6 = new MyTabConfig("Tipe");
//		tab6.setParent(tabs);
//
//		MyTabConfig tab7 = new MyTabConfig("Unit Kerja");
//		tab7.setParent(tabs);
//
//		MyTabConfig tab8 = new MyTabConfig("PTKP");
//		tab8.setParent(tabs);
//
//		MyTabConfig tab9 = new MyTabConfig("Ikatan");
//		tab9.setParent(tabs);
//
//		MyTabConfig tab10 = new MyTabConfig("Atasan");
//		tab10.setParent(tabs);
//
//		MyTabConfig tab11 = new MyTabConfig("Masa Kerja");
//		tab11.setParent(tabs);
//
//		MyTabConfig tab12 = new MyTabConfig("Asuransi");
//		tab12.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelDasbor = new ais.ui.util.MyTabpanel();
		tabpanelDasbor.setParent(tabpanels);
		// Tab "Dasbor": pakai setHeight (properti tinggi ZK) PERSIS pola tab "Data"
		// (tabpanel1.setHeight("30330px")) — inilah yang benar-benar dihormati mold tabbox ZK
		// sehingga panel jadi 20000px & konten tampil penuh. (setStyle("min-height:..") TIDAK cukup
		// karena ZK menyetel tinggi div-konten internal ke tinggi viewport, mengabaikan min-height.)
		tabpanelDasbor.setHeight("20000px");
		tabDasbor.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelDasbor.getChildren().size() == 0) {
					DashboardRingkasanSiswa d = new DashboardRingkasanSiswa();
					d.setWidth("100%");
					d.setParent(tabpanelDasbor);
				}
			}
		});
		// Auto-load: Dasbor adalah tab pertama (default terpilih).
		DashboardRingkasanSiswa dRingkasanSiswa = new DashboardRingkasanSiswa();
		dRingkasanSiswa.setWidth("100%");
		dRingkasanSiswa.setParent(tabpanelDasbor);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);
		tabpanel1.setHeight("30330px");

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().size() == 0) {
//					DasboardSiswaPendidikan dasboardSiswaPendidikan = new DasboardSiswaPendidikan();
//					dasboardSiswaPendidikan.setHeight("100%");
//					dasboardSiswaPendidikan.setWidth("100%");
//					dasboardSiswaPendidikan.setParent(tabpanel2);
				}
			}
		});

//		final Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
//		tabpanel3.setParent(tabpanels);
//		tab3.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel3.getChildren().size() == 0) {
//					DasboardSiswaStatusSiswa dasboardSiswaStatusSiswa = new DasboardSiswaStatusSiswa();
//					dasboardSiswaStatusSiswa.setHeight("100%");
//					dasboardSiswaStatusSiswa.setWidth("100%");
//					dasboardSiswaStatusSiswa.setParent(tabpanel3);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel4 = new ais.ui.util.MyTabpanel();
//		tabpanel4.setParent(tabpanels);
//		tab4.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel4.getChildren().size() == 0) {
//					DasboardSiswaAgama dasboardSiswaAgama = new DasboardSiswaAgama();
//					dasboardSiswaAgama.setHeight("100%");
//					dasboardSiswaAgama.setWidth("100%");
//					dasboardSiswaAgama.setParent(tabpanel4);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel5 = new ais.ui.util.MyTabpanel();
//		tabpanel5.setParent(tabpanels);
//		tab5.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel5.getChildren().size() == 0) {
//					DasboardSiswaUsia dasboardSiswaUsia = new DasboardSiswaUsia();
//					dasboardSiswaUsia.setHeight("100%");
//					dasboardSiswaUsia.setWidth("100%");
//					dasboardSiswaUsia.setParent(tabpanel5);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel6 = new ais.ui.util.MyTabpanel();
//		tabpanel6.setParent(tabpanels);
//		tab6.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel6.getChildren().size() == 0) {
//					DasboardSiswaTipeSiswa dasboardSiswaTipeSiswa = new DasboardSiswaTipeSiswa();
//					dasboardSiswaTipeSiswa.setHeight("100%");
//					dasboardSiswaTipeSiswa.setWidth("100%");
//					dasboardSiswaTipeSiswa.setParent(tabpanel6);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel7 = new ais.ui.util.MyTabpanel();
//		tabpanel7.setParent(tabpanels);
//		tab7.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel7.getChildren().size() == 0) {
//					DasboardSiswaUnitKerja dasboardSiswaUnitKerja = new DasboardSiswaUnitKerja();
//					dasboardSiswaUnitKerja.setHeight("100%");
//					dasboardSiswaUnitKerja.setWidth("100%");
//					dasboardSiswaUnitKerja.setParent(tabpanel7);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel8 = new ais.ui.util.MyTabpanel();
//		tabpanel8.setParent(tabpanels);
//		tab8.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel8.getChildren().size() == 0) {
//					DasboardSiswaPtkpSiswa dasboardSiswaPtkpSiswa = new DasboardSiswaPtkpSiswa();
//					dasboardSiswaPtkpSiswa.setHeight("100%");
//					dasboardSiswaPtkpSiswa.setWidth("100%");
//					dasboardSiswaPtkpSiswa.setParent(tabpanel8);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel9 = new ais.ui.util.MyTabpanel();
//		tabpanel9.setParent(tabpanels);
//		tab9.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel9.getChildren().size() == 0) {
//					DasboardSiswaIkatanKerja dasboardSiswaIkatanKerja = new DasboardSiswaIkatanKerja();
//					dasboardSiswaIkatanKerja.setHeight("100%");
//					dasboardSiswaIkatanKerja.setWidth("100%");
//					dasboardSiswaIkatanKerja.setParent(tabpanel9);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel10 = new ais.ui.util.MyTabpanel();
//		tabpanel10.setParent(tabpanels);
//		tab10.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel10.getChildren().size() == 0) {
//					DasboardSiswaJenisJabatan dasboardSiswaJenisJabatan = new DasboardSiswaJenisJabatan();
//					dasboardSiswaJenisJabatan.setHeight("100%");
//					dasboardSiswaJenisJabatan.setWidth("100%");
//					dasboardSiswaJenisJabatan.setParent(tabpanel10);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel11 = new ais.ui.util.MyTabpanel();
//		tabpanel11.setParent(tabpanels);
//		tab11.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel11.getChildren().size() == 0) {
//					DasboardSiswaMasaKerja dasboardSiswaMasaKerja = new DasboardSiswaMasaKerja();
//					dasboardSiswaMasaKerja.setHeight("100%");
//					dasboardSiswaMasaKerja.setWidth("100%");
//					dasboardSiswaMasaKerja.setParent(tabpanel11);
//				}
//			}
//		});
//
//		final Tabpanel tabpanel12 = new ais.ui.util.MyTabpanel();
//		tabpanel12.setParent(tabpanels);
//		tab12.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabpanel12.getChildren().size() == 0) {
//					DasboardSiswaAsuransi dasboardSiswaAsuransi = new DasboardSiswaAsuransi();
//					dasboardSiswaAsuransi.setHeight("100%");
//					dasboardSiswaAsuransi.setWidth("100%");
//					dasboardSiswaAsuransi.setParent(tabpanel12);
//				}
//			}
//		});

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(tabpanel1);
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
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(searchyayasan);
		searchyayasan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(searchsekolah);
		searchsekolah.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Masuk"));
		Hbox hbox = new Hbox();
		hbox.appendChild(mulai);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		hbox.appendChild(sampai);

		mulai.setCols(5);
		sampai.setCols(5);

		calendar = ais.ui.util.WaktuUtil.getCalendar();

		mulai.setValue(calendar.get(Calendar.YEAR) - 4);
		sampai.setValue(calendar.get(Calendar.YEAR));

		searchyayasan.addEventListener("onChange", eventListener);
		searchsekolah.addEventListener("onChange", eventListener);
		mulai.addEventListener("onChange", eventListener);
		sampai.addEventListener("onChange", eventListener);

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
				UIUtil.downloadGrid(DasboardSiswa.this.grid);
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

		int mul = mulai.getValue() == null ? (calendar.get(Calendar.YEAR) - 4) : mulai.getValue();
		int sam = sampai.getValue() == null ? (calendar.get(Calendar.YEAR)) : sampai.getValue();

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Sekolah");
		column.setParent(columns);

		column.setParent(columns);
		column = new MyColumnConfig("Tahun Masuk");
		column.setAlign("center");
		column.setParent(columns);
		column.setWidth("10%");

		column.setParent(columns);
		column = new MyColumnConfig("Jumlah");
		column.setAlign("center");
		column.setParent(columns);
		column.setWidth("5%");

		final List<Sekolah> sekolahs = ConstantValues.simpleList(HibernateUtil.currentSession()
				.createCriteria(Sekolah.class)
				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						? CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false)
						: Restrictions.sqlRestriction("true"))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						? Restrictions.eq("id", ((Sekolah) searchsekolah.getSelectedItem().getValue()).getId())
						: Restrictions.sqlRestriction("true"))

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("nama")), Sekolah.class);

		Rows rows = new Rows();
		rows.setParent(grid);

		SimpleCategoryModel categoryModel = new SimpleCategoryModel();
		categoryModel.clear();
		int total = 0;
		for (final Sekolah sekolah : sekolahs) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.appendChild(new MyLabelBoldAja(sekolah.getNama()));

			for (int i = mul; i <= sam; i++) {
				final int thn = i;
				Integer count = ((Number) HibernateUtil.currentSession().createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa",""))
						.add(Restrictions.eq("tahunMasuk", thn))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.rowCount())
						.add(sekolah == null ? Restrictions.isNull("sekolah") : Restrictions.eq("sekolah", sekolah))

						.uniqueResult()).intValue();
				if (count > 0) {
					categoryModel.setValue(sekolah == null ? "Tidak Ditentukan" : sekolah.getNama(), "", count);
				}

				total += count;

				A a = new A(count + "");
				a.setStyle("font-size:12px;");
				a.setParent(row);
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						EventListener eventListener = (EventListener) Common
								.cetakDataCustomButton(Siswa.class, new DataCriteriaWithColumn() {

									@Override
									public Object[] initCriteria(boolean order) {

										try {

											Criteria criteria = HibernateUtil.currentSession()
													.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.eq("tahunMasuk", thn))
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))
													.add(sekolah == null ? Restrictions.isNull("sekolah")
															: Restrictions.eq("sekolah", sekolah));

											return new Object[] { criteria, SiswaAction.contents };

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

				if (count > 0) {
					row.setParent(rows);
				}
			}
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelBolder("Total"));

		A a = new A(total + "");
		a.setStyle("font-size:16px;font-weight: bolder;");
		a.setParent(row);
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				EventListener eventListener = (EventListener) Common
						.cetakDataCustomButton(Siswa.class, new DataCriteriaWithColumn() {

							@Override
							public Object[] initCriteria(boolean order) {

								try {

									Criteria criteria = HibernateUtil.currentSession().createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa",""))
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(sekolahs.isEmpty() ? Restrictions.sqlRestriction("false")
													: Restrictions.in("sekolah", sekolahs));

									return new Object[] { criteria, SiswaAction.contents };

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

		row = new MyFormRow();
		row.setParent(rows);
		row.setSpans((4) + "");
		row.setAlign("center");

		row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, "Dasbor Siswa", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
}
}
