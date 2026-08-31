package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.MatakuliahPrasyaratAction;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.MatakuliahDao;
import ais.database.dao.MatakuliahPrasyaratDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jenjang;
import ais.database.model.Matakuliah;
import ais.database.model.MatakuliahPrasyarat;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK berbentuk dialog pencarian-dan-pilih untuk menetapkan mata kuliah prasyarat
 * ({@link MatakuliahPrasyarat}) bagi satu {@link Matakuliah} target. Dialog menampilkan grid
 * berpaging server-side (mold "paging") seluruh mata kuliah aktif dengan filter kode/nama,
 * fakultas, prodi, dan jenjang; checkbox tiap baris tercentang otomatis bila relasi prasyarat
 * ke {@link #matakuliah} target sudah ada, dan kolom "Prasyarat" menampilkan daftar prasyarat
 * yang sudah ditetapkan sebelumnya (via {@link
 * ais.action.master.MatakuliahPrasyaratAction#tampilPrasyarat}).
 *
 * <p>
 * {@link #save()} hanya MENAMBAH/memperbarui relasi untuk baris yang tercentang saat disimpan —
 * tidak menghapus relasi yang tidak lagi tercentang (baris yang checkbox-nya dilepas tetap
 * dibiarkan apa adanya, hanya baris tercentang yang diproses {@code saveOrUpdate}).
 * </p>
 */
public class AmbilDataMatakuliahPrasyaratHelper {

	private Matakuliah matakuliah;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private Textbox kodeMk;
	private Textbox namaMk;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox searchjenjang = new Combobox();

	/** Menyiapkan combobox filter jenjang, fakultas, dan jurusan (masing-masing dengan opsi "Semua"). */
	public AmbilDataMatakuliahPrasyaratHelper() {

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
	}

	/** Renderer baris kandidat prasyarat: checkbox (tercentang bila relasi prasyarat ke {@link #matakuliah} sudah ada), nama, daftar prasyarat yang sudah ditetapkan, SKS, status, jenis, fakultas/jurusan/jenjang. */
	class MatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		private MatakuliahDao matakuliahPrasyaratDao = DaoFactory.getInstance().getMatakuliahDao();

		private Session session = matakuliahPrasyaratDao.getCurrentSession();

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Matakuliah matakuliah = (Matakuliah) arg1;
			Checkbox checkbox = new Checkbox(matakuliah.getKode() + " (" + matakuliah.getId() + ") ");
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.setAttribute("matakuliah", matakuliah);

			Integer jml = ((Number) session.createCriteria(MatakuliahPrasyarat.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("matakuliah", AmbilDataMatakuliahPrasyaratHelper.this.matakuliah))
					.add(Restrictions.eq("matakuliahPrasyarat", matakuliah)).uniqueResult()).intValue();

			checkbox.setChecked(!jml.equals(0));

			new Label(matakuliah.getNama()).setParent(arg0);
			MatakuliahPrasyaratAction.tampilPrasyarat(arg0, matakuliah);
			new Label(matakuliah.getSks() + "").setParent(arg0);
			new Label(matakuliah.getStatus() == null ? "" : matakuliah.getStatus()).setParent(arg0);
			new Label(matakuliah.getJenisMatakuliah()).setParent(arg0);

			new Label(matakuliah.getJurusan() == null || matakuliah.getJurusan().getFakultas() == null ? ""
					: matakuliah.getJurusan().getFakultas().getNama()).setParent(arg0);
			new Label(matakuliah.getJurusan() == null ? "" : matakuliah.getJurusan().getNama()).setParent(arg0);
			new Label(matakuliah.getJurusan() == null || matakuliah.getJurusan().getJenjang() == null ? ""
					: matakuliah.getJurusan().getJenjang().getNama()).setParent(arg0);

		}

	}

	/**
	 * Menyimpan relasi {@link MatakuliahPrasyarat} untuk setiap baris grid yang checkbox-nya
	 * tercentang (mencari relasi existing lebih dulu agar tidak duplikat). Baris yang tidak
	 * tercentang TIDAK memicu penghapusan relasi apa pun — method ini hanya bersifat aditif.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() {
		MatakuliahPrasyaratDao matakuliahPrasyaratDao = DaoFactory.getInstance().getMatakuliahPrasyaratDao();
		Session session = matakuliahPrasyaratDao.getCurrentSession();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				Checkbox checkbox = (Checkbox) data.get(0);
				if (checkbox.isChecked()) {
					Matakuliah matakuliah = (Matakuliah) checkbox.getAttribute("matakuliah");
					MatakuliahPrasyarat matakuliahPrasyarat = (MatakuliahPrasyarat) session
							.createCriteria(MatakuliahPrasyarat.class)
							.add(Restrictions.eq("matakuliah", this.matakuliah))
							.add(Restrictions.eq("matakuliahPrasyarat", matakuliah)).setMaxResults(1).uniqueResult();
					if (matakuliahPrasyarat == null) {
						matakuliahPrasyarat = new MatakuliahPrasyarat();
					}

					matakuliahPrasyarat.setMatakuliah(this.matakuliah);
					matakuliahPrasyarat.setMatakuliahPrasyarat(matakuliah);
					session.saveOrUpdate(matakuliahPrasyarat);

				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AmbilDataMatakuliahPrasyaratHelper.java:139");
			}
		}

	}

	/**
	 * Titik masuk utama: membangun dialog pencarian (filter fakultas/kode/prodi/jenjang/nama) dan
	 * grid berpaging kandidat prasyarat untuk {@code matakuliah} target, dengan tombol Simpan
	 * (memanggil {@link #save()}) / Batal.
	 *
	 * @param matakuliah mata kuliah target yang prasyaratnya ditetapkan
	 * @param dataLoader dipanggil untuk memuat ulang tampilan pemanggil setelah simpan
	 * @param window     window pembungkus dialog (dibersihkan dan diisi ulang)
	 */
	public void display(final Matakuliah matakuliah, final DataLoader dataLoader, final MyWindow window) {

		this.matakuliah = matakuliah;
		Common.clear(window);
		window.setTitle("Ambil Data Matakuliah");
		window.setWidth("750px");
		window.setHeight("540px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.Grid gridUtama = new org.zkoss.zul.Grid();
		gridUtama.setWidth("100%");
		ais.ui.util.ZkCompat.setFlex(gridUtama, true);
		gridUtama.setParent(center);
		Rows rowsUtama = new Rows();
		rowsUtama.setParent(gridUtama);

		Row rowUtama = new Row();
		rowUtama.setParent(rowsUtama);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Matakuliah"));
		row.appendChild(kodeMk = new Textbox());
		kodeMk.setWidth("90%");
		kodeMk.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		row.appendChild(searchjenjang);
		searchjenjang.setWidth("90%");
		searchjenjang.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Matakuliah"));
		row.appendChild(namaMk = new Textbox());
		namaMk.setWidth("90%");
		namaMk.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMatakuliahPrasyaratHelper.java:286");

					}
				}
			}
		});
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Prasyarat");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keberadaan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenjang");

		onSearchDefault(null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

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

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Memuat ulang grid kandidat mata kuliah (maks {@link Common#MAX_RESULT} baris) sesuai filter aktif. */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Matakuliah> matakuliah = session.createCriteria(Matakuliah.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama"))
				.add(Restrictions.ilike("kode", kodeMk.getText().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("nama", namaMk.getText().trim(), MatchMode.ANYWHERE))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(searchjenjang.getSelectedItem() == null || searchjenjang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenjang", searchjenjang.getSelectedItem().getValue()))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(matakuliah);
		grid.setRowRenderer(new MatakuliahRenderer());
		grid.setModelCheckMobile(strset);

	}

}
