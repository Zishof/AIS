package ais.action.master.dashboard.sekolah;

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
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleCategoryModel;

import ais.action.maintenance.MainAction;
import ais.action.master.sekolah.KurikulumSekolahAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.JenisPenilaian;
import ais.database.model.sekolah.KurikulumSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyWindow;

import ais.ui.util.DashboardModernHtmlUtil;
/**
 * Komponen dashboard khusus untuk dashboard kurikulum sekolah. Kelas ini memilih variasi data atau
 * tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox jenisPenilaian}, {@code Center
 * center}, {@code Combobox searchyayasan}, {@code Combobox searchsekolah}, {@code int width}, {@code int
 * height}; inisialisasi/lifecycle ({@code init()}); pembacaan/pencarian ({@code reload()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DashboardKurikulumSekolah extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Combobox jenisPenilaian;
	private Center center;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private int width = 750;
	private int height = 100;

	public DashboardKurikulumSekolah() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardKurikulumSekolah(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings({})
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Jenis Kurikulum"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}

		};

		jenisPenilaian = new Combobox();
		Common.insertComboDanSemua(jenisPenilaian, "nama", JenisPenilaian.class, Restrictions.eq("aktif", true));
		jenisPenilaian.setParent(row);
		jenisPenilaian.setWidth("95%");

		searchyayasan = new Combobox();
		searchsekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);
		row.appendChild(new MyLabelConfig("Yayasan / Sekolah"));
		hbox = new Hbox();
		hbox.setParent(row);
		hbox.appendChild(searchyayasan);
		hbox.appendChild(searchsekolah);
		searchyayasan.addEventListener("onChange", eventListener);
		searchsekolah.addEventListener("onChange", eventListener);
		searchyayasan.setCols(2);
		searchsekolah.setCols(2);

		center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		reload();
	}

	@SuppressWarnings({ "deprecation" })
	private void reload() {
		Common.clear(center);
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Grid grid = new Grid();
				grid.setSclass("dgrid");
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig("Yayasan");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Sekolah");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Jenis");
				column.setParent(columns);
				column.setWidth("15%");

				column.setParent(columns);
				column = new MyColumnConfig("Jumlah");
				column.setParent(columns);

				Yayasan fak = (Yayasan) (searchyayasan.getSelectedItem() == null ? null
						: searchyayasan.getSelectedItem().getValue());
				Sekolah jur = (Sekolah) (searchsekolah.getSelectedItem() == null ? null
						: searchsekolah.getSelectedItem().getValue());

				JenisPenilaian jpe = (JenisPenilaian) (jenisPenilaian.getSelectedItem() == null ? null
						: jenisPenilaian.getSelectedItem().getValue());

				List<Sekolah> sekolahs = ConstantValues.simpleList(
						HibernateUtil.currentSession().createCriteria(Sekolah.class)
								.add(jur == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("id", jur.getId()))
								.add(fak == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("yayasan", fak))
								.addOrder(Order.asc("yayasan"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						Sekolah.class);

				Rows rows = new Rows();
				rows.setParent(grid);

				SimpleCategoryModel categoryModel = new SimpleCategoryModel();
				categoryModel.clear();

				for (final Sekolah sekolah : sekolahs) {

					List<JenisPenilaian> jenisPenilaians = ConstantValues.simpleList(
							HibernateUtil.currentSession().createCriteria(JenisPenilaian.class)
									.add(Restrictions.eq("sekolah", sekolah))
									.add(jpe == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("id", jpe.getId()))
									.addOrder(Order.asc("nama")),
							JenisPenilaian.class);

					jenisPenilaians.add(null);

					for (final JenisPenilaian jp : jenisPenilaians) {

						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);
						row.appendChild(new MyLabelBoldAja(sekolah.getYayasan().getNama()));
						row.appendChild(new MyLabelBoldAja(sekolah.getNama()));
						row.appendChild(new MyLabelBoldAja(jp == null ? "Tanpa Jenis Penilaian" : jp.getNama()));

						Integer count = ((Number) HibernateUtil.currentSession().createCriteria(KurikulumSekolah.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setProjection(Projections.rowCount()).add(Restrictions.eq("sekolah", sekolah))
								.add(jp == null ? Restrictions.isNull("jenisPenilaian")
										: Restrictions.eq("jenisPenilaian", jp))
								.uniqueResult()).intValue();

						categoryModel.setValue(sekolah.getNama(),
								(jp == null ? "" : jp.getNama() + " ") + sekolah.getNama(), count);
						A a = new A(count + "");
						a.setStyle("font-size:12px;");
						a.setParent(row);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								EventListener eventListener = (EventListener) Common
										.cetakDataCustomButton(KurikulumSekolah.class, new DataCriteriaWithColumn() {

											@Override
											public Object[] initCriteria(boolean order) {

												try {

													Criteria criteria = HibernateUtil.currentSession()
															.createCriteria(KurikulumSekolah.class)
															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.setProjection(Projections.rowCount())
															.add(Restrictions.eq("sekolah", sekolah))
															.add(jp == null ? Restrictions.isNull("jenisPenilaian")
																	: Restrictions.eq("jenisPenilaian", jp));

													return new Object[] { criteria, KurikulumSekolahAction.contents };

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
					}

				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");

				row.setParent(rows);
				row.setSpans((5) + "");
				row.setAlign("center");

				row.appendChild(DashboardModernHtmlUtil.createAnyChart(categoryModel, "Dasbor Kurikulum Sekolah", "Perbandingan data dibuat ringkas agar kelompok terbesar dan terkecil mudah terlihat.", String.valueOf("bar")));
}
		});
	}
}
