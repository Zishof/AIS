package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.TimDosenDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Perkuliahan;
import ais.database.model.TimDosen;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper "pilih dari daftar" untuk mengatur tim dosen pengampu satu {@link Perkuliahan}, lewat
 * relasi {@link TimDosen}. Menampilkan jendela modal pencarian dosen aktif (nama, fakultas, prodi)
 * dengan checkbox per baris yang menandakan apakah dosen tersebut sudah menjadi anggota tim
 * pengampu perkuliahan yang bersangkutan.
 *
 * <p>
 * Sama seperti {@link AmbilDataItemBiayaHelper}, perubahan checkbox tidak langsung disimpan;
 * {@link #save()} baru dijalankan saat tombol Simpan ditekan, menyinkronkan seluruh baris grid
 * yang sedang ditampilkan (checked → buat/pertahankan {@link TimDosen}, unchecked → hapus) plus
 * entri tambahan pada {@link #deletedDosens} (dosen yang di-uncheck lewat listener {@code onCheck}
 * pada baris yang mungkin sudah di luar halaman grid saat ini).
 * </p>
 */
public class AmbilDataDosenHelper {
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private Perkuliahan perkuliahan;
	private Textbox nama;
	private Set<TimDosen> deletedDosens = new HashSet<TimDosen>();
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	/** Membuat helper dan menginisialisasi combobox pencarian fakultas/jurusan (termasuk opsi "Semua"). */
	public AmbilDataDosenHelper() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
	}

	/** Perender baris grid: checkbox status keanggotaan tim dosen (dengan listener yang menandai/melepas entri {@link #deletedDosens}), plus label NIP, nama, jurusan, dan fakultas dosen. */
	class DosenRenderer extends ais.ui.util.MyRowRenderer {
		private TimDosenDao timDosenDao = DaoFactory.getInstance().gettTimDosenDao();
		private Session session = timDosenDao.getCurrentSession();

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Dosen dosen = (Dosen) arg1;
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("dosen", dosen);
			// checkbox.setId("" + dosen.getId());

			Integer jml = ((Number) session.createCriteria(TimDosen.class).setProjection(Projections.rowCount())
					.add(Restrictions.eq("perkuliahan", perkuliahan)).add(Restrictions.eq("dosen", dosen))
					.uniqueResult()).intValue();

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					TimDosen timDosen = (TimDosen) HibernateUtil.currentSession().createCriteria(TimDosen.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan)).add(Restrictions.eq("dosen", dosen))
							.uniqueResult();
					if (timDosen != null) {
						if (!checkbox.isChecked()) {
							deletedDosens.remove(timDosen);
						} else {
							deletedDosens.add(timDosen);
						}
					}

				}
			});
			checkbox.setChecked(!jml.equals(0));

			new Label(dosen.getCode()).setParent(arg0);
			new Label(dosen.getNama()).setParent(arg0);
			new Label(dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama()).setParent(arg0);
			new Label(dosen.getFakultas() == null ? "" : dosen.getFakultas().getNama()).setParent(arg0);
		}
	}

	/**
	 * Menyinkronkan status checkbox seluruh baris grid yang saat ini ditampilkan ke tabel relasi
	 * {@link TimDosen}: baris tercentang membuat/mempertahankan keanggotaan tim, baris tak
	 * tercentang menghapusnya (bila ada). Setelah itu, seluruh entri pada {@link #deletedDosens}
	 * juga dihapus. Kegagalan per baris ditelan.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() {
		TimDosenDao timDosenDao = DaoFactory.getInstance().gettTimDosenDao();
		Session session = timDosenDao.getCurrentSession();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked()) {

					Dosen dosen = (Dosen) checkbox.getAttribute("dosen");
					TimDosen timDosen = (TimDosen) session.createCriteria(TimDosen.class)
							.add(Restrictions.eq("perkuliahan", this.perkuliahan)).add(Restrictions.eq("dosen", dosen))
							.setMaxResults(1).uniqueResult();

					if (timDosen == null) {
						timDosen = new TimDosen();
					}
					timDosen.setPerkuliahan(this.perkuliahan);
					timDosen.setDosen(dosen);
					session.saveOrUpdate(timDosen);

				} else {
					Dosen dosen = (Dosen) checkbox.getAttribute("dosen");
					TimDosen timDosen = (TimDosen) session.createCriteria(TimDosen.class)
							.add(Restrictions.eq("perkuliahan", this.perkuliahan)).add(Restrictions.eq("dosen", dosen))
							.setMaxResults(1).uniqueResult();

					if (timDosen == null) {
						timDosen = new TimDosen();
					}
					session.delete(timDosen);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataDosenHelper.java:145");
				// TODO: handle exception
			}
		}

		if (deletedDosens != null) {

			for (TimDosen timDosen : deletedDosens) {
				session.delete(timDosen);
			}

		}

	}

	/**
	 * Membangun dan menampilkan jendela modal pengaturan tim dosen untuk {@code perkuliahan} yang
	 * diberikan: form pencarian nama/fakultas/prodi, grid ber-paging server-side dengan checkbox
	 * per baris, dan tombol Cari/Simpan/Batal. Combobox fakultas/prodi sengaja dinonaktifkan
	 * sesaat lalu diaktifkan kembali lewat {@link Common#createDefaultTimer} (pola workaround UI
	 * ZK, bukan kesalahan).
	 *
	 * @param perkuliahan perkuliahan yang tim dosennya akan diatur
	 * @param dataLoader  callback penyegar tampilan pemanggil setelah simpan
	 * @param window      jendela modal yang akan dibangun isinya (dibersihkan lebih dulu)
	 */
	public void display(Perkuliahan perkuliahan, final DataLoader dataLoader, final MyWindow window) {
		this.perkuliahan = perkuliahan;
		Common.clear(window);
		window.setTitle("Data Tim Dosen");
		window.setWidth("750px");
		window.setHeight("540px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(window);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Dosen");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("200px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.setTooltiptext("Cari");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
		pagingHelper.pasangOnPaging(new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		pagingHelper.pasangGridDanPaging(center, grid);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("50px");
		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		column.appendChild(checkbox);
		checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {
					try {
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataDosenHelper.java:264");

					}
				}
			}
		});

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIP");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");
		column.setWidth("20%");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				save();
				dataLoader.loadData(null);
				window.setVisible(false);
			}
		});

		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);

		onSearchDefault(null);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				searchfakultas.setDisabled(false);
				searchjurusan.setDisabled(false);

			}
		});
	}

	/**
	 * Menjalankan pencarian dosen aktif berdasarkan nama (ilike) dan filter jurusan/fakultas —
	 * dosen dengan {@code milikUniversitas = true} selalu lolos filter jurusan/fakultas (dianggap
	 * "milik semua unit"). Memuat ulang grid dengan hasilnya.
	 *
	 * @param event event pemicu (tombol Cari/paging), tidak dipakai langsung
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Dosen> dosen = pagingHelper.cariDenganCriteria(session.createCriteria(Dosen.class)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.or(
						searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false),
						Restrictions.eq("milikUniversitas", true)))

				.add(Restrictions.or(Restrictions.eq("milikUniversitas", true),
						searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

				.addOrder(Order.asc("id")).setMaxResults(Common.MAX_RESULT), Dosen.class);

		ListModel strset = new SimpleListModel(dosen);
		grid.setRowRenderer(new DosenRenderer());
		grid.setModelCheckMobile(strset);

	}

}
