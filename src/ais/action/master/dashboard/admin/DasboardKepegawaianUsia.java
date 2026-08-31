package ais.action.master.dashboard.admin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.TipeMasaKerja;
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
/**
 * Komponen dashboard khusus untuk dasboard kepegawaian usia. Kelas ini memilih variasi data atau
 * tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Div center}, {@code
 * AmbilDataSatuanKerjaBanbox searchparent}, {@code int width}, {@code int height}, {@code SatuanKerjaTreeModel
 * satuanKerjaTreeModel}, {@code Grid grid}, {@code Combobox tipeMasaKerja}; inisialisasi/lifecycle ({@code
 * init()}); pembacaan/pencarian ({@code reload()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DasboardKepegawaianUsia extends MyWindow {

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

	public DasboardKepegawaianUsia() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DasboardKepegawaianUsia(String title, String border, boolean closable) {
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
				"Sebaran Pegawai per Kelompok Usia",
				"Jumlah pegawai menurut kelompok usia, beserta grafiknya.");
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
				UIUtil.downloadGrid(DasboardKepegawaianUsia.this.grid);
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

		List<CommonVO> usias = new ArrayList<CommonVO>();
		usias.add(new CommonVO("1", "<30"));
		usias.add(new CommonVO("2", ">=30"));
		usias.add(new CommonVO("3", ">=40"));
		usias.add(new CommonVO("4", ">=50"));
		usias.add(new CommonVO("5", "Tidak Ditentukan"));
		Map<String, MyColumnConfig> listCols = new HashMap<String, MyColumnConfig>();
		for (CommonVO usia : usias) {
			column = new MyColumnConfig(usia.getName());
			column.setWidth("5%");
			column.setAlign("right");
			listCols.put(usia.getId(), column);
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
		final String satker = inSatker.isEmpty() ? "true" : "(this_.satuan_kerja in (" + inSatker + ") or this_.satuan_kerja is null)";

		satuanKerjas.add(null);

		Rows rows = new Rows();
		rows.setParent(grid);

		SimpleCategoryModel categoryModel = new SimpleCategoryModel();
		categoryModel.clear();
		Map<String, Integer> listTotals = new HashMap<String, Integer>();
		Map<Long, List<Integer>> mapData = new HashMap<Long, List<Integer>>();

		for (final SatuanKerja satuanKerja : satuanKerjas) {
			List<Integer> data = mapData.get(satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId());
			if (data == null) {
				data = new ArrayList<Integer>();
				mapData.put(satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId(), data);
			}
			for (CommonVO usia : usias) {

				Calendar mulai = Calendar.getInstance();
				Calendar sampai = Calendar.getInstance();
				if (usia.getId().equals("1")) {
					mulai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 30);
				} else if (usia.getId().equals("2")) {
					sampai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 30);
					mulai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 40);
				} else if (usia.getId().equals("3")) {
					sampai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 40);
					mulai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 50);
				} else if (usia.getId().equals("4")) {
					sampai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 50);
					mulai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 250);
				}

				Integer count = ((Number) HibernateUtil.currentSession().createCriteria(Pegawai.class)
						.add(Restrictions.sqlRestriction(satker))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.rowCount())
						.add(satuanKerja == null ? Restrictions.isNull("satuanKerja")
								: Restrictions.eq("satuanKerja", satuanKerja))
						.add(tipeMasaKerja.getSelectedItem() == null
								|| tipeMasaKerja.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("tipeMasaKerja", tipeMasaKerja.getSelectedItem().getValue()))
						.add(usia.getId().equals("5") ? Restrictions.isNull("tanggallahir")
								: Restrictions.between("tanggallahir", mulai.getTime(), sampai.getTime()))
						.uniqueResult()).intValue();
				data.add(count);
				Integer colCount = listTotals.get(usia.getId());
				if (colCount == null) {
					colCount = 0;
				}
				colCount += count;
				listTotals.put(usia.getId(), colCount);

			}
		}

		for (final SatuanKerja satuanKerja : satuanKerjas) {
			List<Integer> data = mapData.get(satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId());
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.appendChild(new MyLabelBoldAja(satuanKerja == null ? "Tidak Ditentukan" : satuanKerja.getNama()));
			int jml = 0;
			int i = 0;
			for (final CommonVO usia : usias) {
				Integer colCount = listTotals.get(usia.getId());
				if (colCount == null) {
					colCount = 0;
				}
				if (colCount > 0) {
					int count = data.get(i);
					jml += count;

					if (count > 0) {
						categoryModel.setValue(satuanKerja == null ? "Tidak Ditentukan" : satuanKerja.getNama(),
								usia.getName(), count);
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

												Calendar mulai = Calendar.getInstance();
												Calendar sampai = Calendar.getInstance();
												if (usia.getId().equals("1")) {
													mulai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 30);
												} else if (usia.getId().equals("2")) {
													sampai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 30);
													mulai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 40);
												} else if (usia.getId().equals("3")) {
													sampai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 40);
													mulai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 50);
												} else if (usia.getId().equals("4")) {
													sampai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 50);
													mulai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 250);
												}

												Criteria criteria = HibernateUtil.currentSession()
														.createCriteria(Pegawai.class)
														.add(Restrictions.sqlRestriction(satker))
														.add(Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true)))
														.add(satuanKerja == null ? Restrictions.isNull("satuanKerja")
																: Restrictions.eq("satuanKerja", satuanKerja))
														.add(tipeMasaKerja.getSelectedItem() == null
																|| tipeMasaKerja.getSelectedItem().getValue() == null
																		? Restrictions.sqlRestriction("true")
																		: Restrictions.eq("tipeMasaKerja",
																				tipeMasaKerja.getSelectedItem()
																						.getValue()))
														.add(usia.getId().equals("5")
																? Restrictions.isNull("tanggallahir")
																: Restrictions.between("tanggallahir", mulai.getTime(),
																		sampai.getTime()));

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
												.add(tipeMasaKerja.getSelectedItem() == null
														|| tipeMasaKerja.getSelectedItem().getValue() == null
																? Restrictions.sqlRestriction("true")
																: Restrictions.eq("tipeMasaKerja",
																		tipeMasaKerja.getSelectedItem().getValue()))
												.add(satuanKerja == null ? Restrictions.isNull("satuanKerja")
														: Restrictions.eq("satuanKerja", satuanKerja));

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
		for (final CommonVO usia : usias) {
			Integer colCount = listTotals.get(usia.getId());
			if (colCount == null) {
				colCount = 0;
			}
			totalCol += colCount;
			if (colCount > 0) {
				countCol++;
				listCols.get(usia.getId()).setParent(columns);

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

										Calendar mulai = Calendar.getInstance();
										Calendar sampai = Calendar.getInstance();
										if (usia.getId().equals("1")) {
											mulai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 30);
										} else if (usia.getId().equals("2")) {
											sampai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 30);
											mulai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 40);
										} else if (usia.getId().equals("3")) {
											sampai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 40);
											mulai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 50);
										} else if (usia.getId().equals("4")) {
											sampai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 50);
											mulai.set(Calendar.YEAR, mulai.get(Calendar.YEAR) - 250);
										}

										try {

											Criteria criteria = HibernateUtil.currentSession()
													.createCriteria(Pegawai.class)
													.add(Restrictions.sqlRestriction(satker))
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))
													.add(tipeMasaKerja.getSelectedItem() == null
															|| tipeMasaKerja.getSelectedItem().getValue() == null
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.eq("tipeMasaKerja",
																			tipeMasaKerja.getSelectedItem().getValue()))
													.add(usia.getId().equals("5") ? Restrictions.isNull("tanggallahir")
															: Restrictions.between("tanggallahir", mulai.getTime(),
																	sampai.getTime()));

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
		for (CommonVO usia : usias) {
			Integer colCount = listTotals.get(usia.getId());
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

		row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, "Dasbor Kepegawaian Usia", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
}
}
