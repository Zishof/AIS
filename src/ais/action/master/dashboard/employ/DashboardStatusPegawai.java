package ais.action.master.dashboard.employ;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;

import ais.action.master.PegawaiAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.StatusKepegawaian;
import ais.database.model.StatusPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

import ais.ui.util.DashboardModernHtmlUtil;
/**
 * Komponen dashboard khusus untuk dashboard status pegawai. Kelas ini memilih variasi data atau
 * tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchStatusPegawai}, {@code
 * Center center}, {@code Grid grid}, {@code int width}, {@code int height}; inisialisasi/lifecycle ({@code
 * init()}); pembacaan/pencarian ({@code reload()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardStatusPegawai extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Combobox searchStatusPegawai;
	private Center center;
	protected Grid grid;
	private int width = 750;
	private int height = 100;

	public DashboardStatusPegawai() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardStatusPegawai(int width, int height) {
		super();
		try {
			this.width = width;
			this.height = height;
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardStatusPegawai(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		};

		searchStatusPegawai = new Combobox();
		row.appendChild(new MyLabelConfig("Status Pegawai"));
		row.appendChild(searchStatusPegawai);
		Common.insertComboDanSemua(searchStatusPegawai, "nama", StatusPegawai.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		searchStatusPegawai.setWidth("90%");

		Common.selectComboItem(searchStatusPegawai, ConstantValues.AKTIF_PEGAWAI);

		searchStatusPegawai.addEventListener("onChange", eventListener);

		center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		reload();

		row = new MyFormRow();
		row.setParent(rows);
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DashboardStatusPegawai.this.grid);
			}
		});

	}

	private void reload() {
		Common.clear(center);

		final StatusPegawai statusPegawai = (StatusPegawai) (searchStatusPegawai.getSelectedItem() == null
				|| searchStatusPegawai.getSelectedItem().getValue() == null ? null
						: searchStatusPegawai.getSelectedItem().getValue());

		final List<StatusKepegawaian> statusKepegawaians = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(Pegawai.class)
						.add(statusPegawai == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("statusPegawai", statusPegawai))
						.setProjection(Projections.groupProperty("statusKepegawaian.id"))
						.add(Restrictions.isNotNull("statusKepegawaian"))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.createAlias("statusPegawai", "statusPegawai").addOrder(Order.asc("statusPegawai.nama")),
				StatusKepegawaian.class, false);

		final List<SatuanKerja> satuanKerjas = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(Pegawai.class)
						.add(statusPegawai == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("statusPegawai", statusPegawai))
						.setProjection(Projections.groupProperty("satuanKerja.id"))
						.add(Restrictions.isNotNull("satuanKerja"))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.createAlias("satuanKerja", "satuanKerja").addOrder(Order.asc("satuanKerja.nama")),
				SatuanKerja.class, false);

		final Map<String, Integer> datas = new HashMap<String, Integer>();

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);

				grid = new Grid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig("Unit/Satuan Kerja");
				column.setParent(columns);
				column.setWidth("15%");

				for (StatusKepegawaian statusKepegawaian : statusKepegawaians) {
					column.setParent(columns);
					column = new MyColumnConfig(statusKepegawaian.getNama());
					column.setParent(columns);
					column.setWidth("10%");
				}

				Rows rows = new Rows();
				rows.setParent(grid);

				SimpleCategoryModel categoryModelRataRata = new SimpleCategoryModel();
				categoryModelRataRata.clear();

				SimpleCategoryModel categoryModelMin = new SimpleCategoryModel();
				categoryModelMin.clear();

				SimpleCategoryModel categoryModelMax = new SimpleCategoryModel();
				categoryModelMax.clear();

				for (final SatuanKerja satuanKerja : satuanKerjas) {
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBoldAja(satuanKerja.getNama()));

					for (final StatusKepegawaian statusKepegawaian : statusKepegawaians) {

						Integer jumlah = datas.get(statusKepegawaian.getId() + "_" + satuanKerja.getId());

						A a = new A(Common.numberFormat.get().format(jumlah));
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

															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.add(Restrictions.eq("statusKepegawaian",
																	statusKepegawaian))

															.add(Restrictions.eq("satuanKerja", satuanKerja))
															.addOrder(Order.asc("nim"));

													return new Object[] { criteria, PegawaiAction.columns };

												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
												}
												return null;
											}

										}, null, "Download Data", "/img/print.png", null, null, false, null,
												"DATA TAMBAHAN",
												new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
														"", "", "", "", "", "", "", "", "", "" })
										.getAttribute("eventListener");

								eventListener.onEvent(null);

							}
						});

						categoryModelRataRata.setValue(statusKepegawaian.getNama(), satuanKerja.getNama(), jumlah);

					}
				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(row, "3");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModelRataRata, String.valueOf("Rekap Data Pegawai"), "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
}

		});

		new Thread(new Runnable() {

			@Override
			public void run() {

				Session session = null;

				try {
					session = HibernateUtil.currentNativeSession();
					int i = 1;
					for (StatusKepegawaian statusKepegawaian : statusKepegawaians) {
						label.setValue("Sedang memproses data " + statusKepegawaian.getNama() + " ("
								+ Common.numberFormat.get().format((i * 100.0) / statusKepegawaians.size()) + ")");
						i++;

						for (SatuanKerja satuanKerja : satuanKerjas) {

							Integer dataIpk = ((Number) session.createCriteria(Pegawai.class)
									.setProjection(Projections.rowCount())
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("statusKepegawaian", statusKepegawaian))
									.add(Restrictions.eq("satuanKerja", satuanKerja)).uniqueResult()).intValue();

							datas.put(statusKepegawaian.getId() + "_" + satuanKerja.getId(), dataIpk);
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				} finally {
					if (session != null) {
						try {
							HibernateUtil.closeSession();
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
					try {
						label.setValue("");
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		}).start();
	}
}
