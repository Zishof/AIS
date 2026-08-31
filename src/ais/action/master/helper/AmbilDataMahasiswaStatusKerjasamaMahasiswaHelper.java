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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.MahasiswaDapatStatusKerjasamaMahasiswaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatStatusKerjasamaMahasiswa;
import ais.database.model.StatusKerjasamaMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper jendela modal untuk memberikan (assign) satu {@link StatusKerjasamaMahasiswa}
 * kepada banyak {@link Mahasiswa} sekaligus. Menampilkan grid mahasiswa aktif dengan
 * checkbox per baris (dicentang otomatis bila mahasiswa sudah memiliki status kerjasama
 * tersebut, dan dinonaktifkan bila mahasiswa berstatus tidak aktif), filter pencarian
 * NIM/Nama/Tahun Angkatan/Fakultas/Prodi, checkbox "pilih semua" pada header, dan tombol
 * Simpan yang membuat baris {@link MahasiswaDapatStatusKerjasamaMahasiswa} baru untuk
 * setiap mahasiswa tercentang yang belum memilikinya (tidak menghapus baris yang
 * dicentang-lepas — murni operasi tambah).
 */
public class AmbilDataMahasiswaStatusKerjasamaMahasiswaHelper {

	private StatusKerjasamaMahasiswa statusKerjasamaMahasiswa;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private Textbox nim;
	private Textbox nama;
	private Decimalbox tahunangkatan;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	/** Menyiapkan combobox filter Fakultas dan Prodi (dengan opsi "Semua") lewat {@link Common#initFakultasDanJurusanDanSemua}. */
	public AmbilDataMahasiswaStatusKerjasamaMahasiswaHelper() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	/** Perender baris grid mahasiswa: checkbox (status awal & enable/disable mengikuti status kerjasama & keaktifan mahasiswa), lalu label NIM/Nama/Tahun Angkatan. */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		private MahasiswaDapatStatusKerjasamaMahasiswaDao mahasiswaDapatStatusKerjasamaMahasiswaDao = DaoFactory
				.getInstance().getMahasiswaDapatStatusKerjasamaMahasiswaDao();

		private Session session = mahasiswaDapatStatusKerjasamaMahasiswaDao.getCurrentSession();

		/**
		 * Merender satu baris {@link Mahasiswa}: checkbox yang dinonaktifkan bila
		 * status mahasiswa saat ini bukan {@link ConstantValues#AKTIF} dan dicentang
		 * otomatis bila mahasiswa sudah memiliki {@link #statusKerjasamaMahasiswa}
		 * terkait, lalu label NIM/Nama/Tahun Angkatan.
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;
			MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);
			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();
			checkbox.setDisabled(!statusMahasiswa.getId().equals(ConstantValues.AKTIF.getId()));

			Integer jml = ((Number) session.createCriteria(MahasiswaDapatStatusKerjasamaMahasiswa.class)
					.setProjection(Projections.rowCount()).add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("statusKerjasamaMahasiswa", statusKerjasamaMahasiswa)).uniqueResult())
					.intValue();

			checkbox.setChecked(!jml.equals(0));

			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);

		}

	}

	/**
	 * Membuat baris {@link MahasiswaDapatStatusKerjasamaMahasiswa} baru untuk setiap
	 * mahasiswa yang checkbox-nya tercentang di grid dan belum memiliki
	 * {@link #statusKerjasamaMahasiswa} tersebut. Mahasiswa yang sudah memiliki status
	 * ini tidak diduplikasi; melepas centang pada mahasiswa yang sudah punya status TIDAK
	 * menghapus baris yang ada (operasi ini murni penambahan).
	 */
	@SuppressWarnings("unchecked")
	public void save() {
		MahasiswaDapatStatusKerjasamaMahasiswaDao mahasiswaDapatStatusKerjasamaMahasiswaDao = DaoFactory.getInstance()
				.getMahasiswaDapatStatusKerjasamaMahasiswaDao();
		Session session = mahasiswaDapatStatusKerjasamaMahasiswaDao.getCurrentSession();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			@SuppressWarnings("rawtypes")
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked()) {
					Mahasiswa mahasiswa = (Mahasiswa) checkbox.getAttribute("mahasiswa");
					Integer jml = ((Number) session.createCriteria(MahasiswaDapatStatusKerjasamaMahasiswa.class)
							.setProjection(Projections.rowCount()).add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(Restrictions.eq("statusKerjasamaMahasiswa", statusKerjasamaMahasiswa)).uniqueResult())
							.intValue();

					if (jml.equals(0)) {
						MahasiswaDapatStatusKerjasamaMahasiswa mahasiswaDapatStatusKerjasamaMahasiswa = new MahasiswaDapatStatusKerjasamaMahasiswa();
						mahasiswaDapatStatusKerjasamaMahasiswa.setStatusKerjasamaMahasiswa(statusKerjasamaMahasiswa);
						mahasiswaDapatStatusKerjasamaMahasiswa.setKeterangan("");
						mahasiswaDapatStatusKerjasamaMahasiswa.setMahasiswa(mahasiswa);
						session.save(mahasiswaDapatStatusKerjasamaMahasiswa);
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaStatusKerjasamaMahasiswaHelper.java:133");
				// TODO: handle exception
			}
		}

	}

	/**
	 * Membangun dan menampilkan jendela modal "Ambil Data Mahasiswa": form filter
	 * (NIM/Nama/Tahun Angkatan/Fakultas/Prodi), toolbar (Simpan/Cari/Batal), dan grid
	 * berpaging dengan checkbox "pilih semua" pada header, lalu memuat data awal via
	 * {@link #onSearchDefault(Event)} dan menampilkan jendela sebagai modal.
	 *
	 * @param statusKerjasamaMahasiswa status kerjasama yang akan diberikan ke mahasiswa terpilih
	 * @param dataLoader                callback pemuatan ulang data pemanggil setelah Simpan
	 * @param window                    jendela ZK yang akan diisi dan ditampilkan sebagai modal
	 */
	public void display(final StatusKerjasamaMahasiswa statusKerjasamaMahasiswa, final DataLoader dataLoader,
			final MyWindow window) {
		this.statusKerjasamaMahasiswa = statusKerjasamaMahasiswa;
		Common.clear(window);
		window.setTitle("Ambil Data Mahasiswa");
		window.setWidth("750px");
		window.setHeight("540px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(window);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Mahasiswa");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(nim = new Textbox());
		nim.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Mahasiswa"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(tahunangkatan = new Decimalbox());
		tahunangkatan.setWidth("90%");

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

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// South south = new South();
		// ais.ui.util.ZkCompat.setFlex(south, true);
		// south.setParent(div);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
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
		button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT_100. */
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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaStatusKerjasamaMahasiswaHelper.java:287");

					}
				}
			}
		});
		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("65%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Angkatan");
		column.setWidth("25%");

		onSearchDefault(null);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Memuat ulang grid mahasiswa aktif sesuai filter NIM/Nama/Tahun Angkatan/
	 * Fakultas/Prodi yang sedang terisi (dibatasi {@link Common#MAX_RESULT} baris),
	 * diurutkan tahun angkatan terbaru lalu NIM menaik.
	 *
	 * @param event tidak digunakan; parameter kontrak listener/pemanggilan langsung
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Mahasiswa> mahasiswa = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))

				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("nim", nim.getValue().trim(), MatchMode.ANYWHERE))
				.add(tahunangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", tahunangkatan.getValue().intValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
