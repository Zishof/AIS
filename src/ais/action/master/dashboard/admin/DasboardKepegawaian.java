package ais.action.master.dashboard.admin;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
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
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.maintenance.MainAction;
import ais.action.master.PegawaiAction;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.TipeMasaKerja;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

import org.zkoss.zul.Html;

/**
 * Melihat komposisi pegawai berdasarkan unit dan masa kerja agar kebutuhan SDM lebih mudah dianalisis.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardKepegawaian extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Center center;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private Combobox tipeMasaKerja;
	private int width = 750;
	private int height = 100;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Grid grid;

	public DasboardKepegawaian() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DasboardKepegawaian(String title, String border, boolean closable) {
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
		tabbox.setHeight("3330px");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabDasbor = new MyTabConfig("Dasbor");
		tabDasbor.setParent(tabs);
		tabDasbor.setSelected(true);

		MyTabConfig tab1 = new MyTabConfig("Data");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Pendidikan");
		tab2.setParent(tabs);

		MyTabConfig tab3 = new MyTabConfig("Status");
		tab3.setParent(tabs);

		MyTabConfig tab4 = new MyTabConfig("Agama");
		tab4.setParent(tabs);

		MyTabConfig tab5 = new MyTabConfig("Usia");
		tab5.setParent(tabs);

		MyTabConfig tab6 = new MyTabConfig("Tipe");
		tab6.setParent(tabs);

		MyTabConfig tab7 = new MyTabConfig("Unit Kerja");
		tab7.setParent(tabs);

		MyTabConfig tab8 = new MyTabConfig("PTKP");
		tab8.setParent(tabs);

		MyTabConfig tab9 = new MyTabConfig("Ikatan");
		tab9.setParent(tabs);

		MyTabConfig tab10 = new MyTabConfig("Atasan");
		tab10.setParent(tabs);

		MyTabConfig tab11 = new MyTabConfig("Masa Kerja");
		tab11.setParent(tabs);

		MyTabConfig tab12 = new MyTabConfig("Asuransi");
		tab12.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelDasbor = new ais.ui.util.MyTabpanel();
		tabpanelDasbor.setParent(tabpanels);
		DasboardUtamaKepegawaian dasboardUtamaKepegawaian = new DasboardUtamaKepegawaian();
		dasboardUtamaKepegawaian.setHeight("100%");
		dasboardUtamaKepegawaian.setWidth("100%");
		dasboardUtamaKepegawaian.setParent(tabpanelDasbor);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().size() == 0) {
					DasboardKepegawaianPendidikan dasboardKepegawaianPendidikan = new DasboardKepegawaianPendidikan();
					dasboardKepegawaianPendidikan.setHeight("100%");
					dasboardKepegawaianPendidikan.setWidth("100%");
					dasboardKepegawaianPendidikan.setParent(tabpanel2);
				}
			}
		});

		final Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
		tabpanel3.setParent(tabpanels);
		tab3.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel3.getChildren().size() == 0) {
					DasboardKepegawaianStatusKepegawaian dasboardKepegawaianStatusKepegawaian = new DasboardKepegawaianStatusKepegawaian();
					dasboardKepegawaianStatusKepegawaian.setHeight("100%");
					dasboardKepegawaianStatusKepegawaian.setWidth("100%");
					dasboardKepegawaianStatusKepegawaian.setParent(tabpanel3);
				}
			}
		});

		final Tabpanel tabpanel4 = new ais.ui.util.MyTabpanel();
		tabpanel4.setParent(tabpanels);
		tab4.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel4.getChildren().size() == 0) {
					DasboardKepegawaianAgama dasboardKepegawaianAgama = new DasboardKepegawaianAgama();
					dasboardKepegawaianAgama.setHeight("100%");
					dasboardKepegawaianAgama.setWidth("100%");
					dasboardKepegawaianAgama.setParent(tabpanel4);
				}
			}
		});

		final Tabpanel tabpanel5 = new ais.ui.util.MyTabpanel();
		tabpanel5.setParent(tabpanels);
		tab5.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel5.getChildren().size() == 0) {
					DasboardKepegawaianUsia dasboardKepegawaianUsia = new DasboardKepegawaianUsia();
					dasboardKepegawaianUsia.setHeight("100%");
					dasboardKepegawaianUsia.setWidth("100%");
					dasboardKepegawaianUsia.setParent(tabpanel5);
				}
			}
		});

		final Tabpanel tabpanel6 = new ais.ui.util.MyTabpanel();
		tabpanel6.setParent(tabpanels);
		tab6.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel6.getChildren().size() == 0) {
					DasboardKepegawaianTipePegawai dasboardKepegawaianTipePegawai = new DasboardKepegawaianTipePegawai();
					dasboardKepegawaianTipePegawai.setHeight("100%");
					dasboardKepegawaianTipePegawai.setWidth("100%");
					dasboardKepegawaianTipePegawai.setParent(tabpanel6);
				}
			}
		});

		final Tabpanel tabpanel7 = new ais.ui.util.MyTabpanel();
		tabpanel7.setParent(tabpanels);
		tab7.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel7.getChildren().size() == 0) {
					DasboardKepegawaianUnitKerja dasboardKepegawaianUnitKerja = new DasboardKepegawaianUnitKerja();
					dasboardKepegawaianUnitKerja.setHeight("100%");
					dasboardKepegawaianUnitKerja.setWidth("100%");
					dasboardKepegawaianUnitKerja.setParent(tabpanel7);
				}
			}
		});

		final Tabpanel tabpanel8 = new ais.ui.util.MyTabpanel();
		tabpanel8.setParent(tabpanels);
		tab8.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel8.getChildren().size() == 0) {
					DasboardKepegawaianPtkpPegawai dasboardKepegawaianPtkpPegawai = new DasboardKepegawaianPtkpPegawai();
					dasboardKepegawaianPtkpPegawai.setHeight("100%");
					dasboardKepegawaianPtkpPegawai.setWidth("100%");
					dasboardKepegawaianPtkpPegawai.setParent(tabpanel8);
				}
			}
		});

		final Tabpanel tabpanel9 = new ais.ui.util.MyTabpanel();
		tabpanel9.setParent(tabpanels);
		tab9.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel9.getChildren().size() == 0) {
					DasboardKepegawaianIkatanKerja dasboardKepegawaianIkatanKerja = new DasboardKepegawaianIkatanKerja();
					dasboardKepegawaianIkatanKerja.setHeight("100%");
					dasboardKepegawaianIkatanKerja.setWidth("100%");
					dasboardKepegawaianIkatanKerja.setParent(tabpanel9);
				}
			}
		});

		final Tabpanel tabpanel10 = new ais.ui.util.MyTabpanel();
		tabpanel10.setParent(tabpanels);
		tab10.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel10.getChildren().size() == 0) {
					DasboardKepegawaianJenisJabatan dasboardKepegawaianJenisJabatan = new DasboardKepegawaianJenisJabatan();
					dasboardKepegawaianJenisJabatan.setHeight("100%");
					dasboardKepegawaianJenisJabatan.setWidth("100%");
					dasboardKepegawaianJenisJabatan.setParent(tabpanel10);
				}
			}
		});

		final Tabpanel tabpanel11 = new ais.ui.util.MyTabpanel();
		tabpanel11.setParent(tabpanels);
		tab11.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel11.getChildren().size() == 0) {
					DasboardKepegawaianMasaKerja dasboardKepegawaianMasaKerja = new DasboardKepegawaianMasaKerja();
					dasboardKepegawaianMasaKerja.setHeight("100%");
					dasboardKepegawaianMasaKerja.setWidth("100%");
					dasboardKepegawaianMasaKerja.setParent(tabpanel11);
				}
			}
		});

		final Tabpanel tabpanel12 = new ais.ui.util.MyTabpanel();
		tabpanel12.setParent(tabpanels);
		tab12.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel12.getChildren().size() == 0) {
					DasboardKepegawaianAsuransi dasboardKepegawaianAsuransi = new DasboardKepegawaianAsuransi();
					dasboardKepegawaianAsuransi.setHeight("100%");
					dasboardKepegawaianAsuransi.setWidth("100%");
					dasboardKepegawaianAsuransi.setParent(tabpanel12);
				}
			}
		});

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
				UIUtil.downloadGrid(DasboardKepegawaian.this.grid);
			}
		});

	}

	@SuppressWarnings({ "deprecation" })
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

		column.setParent(columns);
		column = new MyColumnConfig("Jumlah Pegawai");
		column.setAlign("center");
		column.setParent(columns);

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
		int total = 0;
		for (final SatuanKerja satuanKerja : satuanKerjas) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.appendChild(new MyLabelBoldAja(satuanKerja == null ? "Tidak Ditentukan" : satuanKerja.getNama()));

			Integer count = ((Number) HibernateUtil.currentSession().createCriteria(Pegawai.class)
					.add(Restrictions.sqlRestriction(satker))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount())
					.add(satuanKerja == null ? Restrictions.isNull("satuanKerja")
							: Restrictions.eq("satuanKerja", satuanKerja))

					.add(tipeMasaKerja.getSelectedItem() == null || tipeMasaKerja.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("true")
							: Restrictions.eq("tipeMasaKerja", tipeMasaKerja.getSelectedItem().getValue()))

					.uniqueResult()).intValue();
			if (count > 0) {
				categoryModel.setValue(satuanKerja == null ? "Tidak Ditentukan" : satuanKerja.getNama(), "", count);
			}

			total += count;

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

			if (count > 0) {
				row.setParent(rows);
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

		row = new MyFormRow();
		row.setParent(rows);
		row.setSpans((3) + "");
		row.setAlign("center");

		row.appendChild(new Html(buildModernChartHtml("Sebaran Pegawai per Satuan Kerja", categoryModel,
				"membantu melihat jumlah pegawai pada setiap satuan kerja. Grafik ini memudahkan pimpinan mengetahui unit yang paling banyak atau paling sedikit pegawainya.")));

	}

	/**
	 * Pembawa data/helper lokal milik {@link DasboardKepegawaian} untuk html category model. Tipe ini
	 * mengelompokkan nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang
	 * jelas.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardKepegawaian}.
	 * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code List rows}; operasi lokal: {@code
	 * clear()}, {@code setValue()}, {@code getRows}(). Aturan bisnis bersama tetap berada pada kelas induk atau
	 * service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see DasboardKepegawaian
	 */
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

	/**
	 * Pembawa data/helper lokal milik {@link DasboardKepegawaian} untuk html category row. Tipe ini mengelompokkan
	 * nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DasboardKepegawaian}.
	 * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String series}, {@code String
	 * category}, {@code double value}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 *
	 * @see DasboardKepegawaian
	 */
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
