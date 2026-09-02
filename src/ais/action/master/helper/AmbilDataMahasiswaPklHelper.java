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
import ais.database.dao.MahasiswaDapatPklDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatPkl;
import ais.database.model.Pkl;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper "pilih dari daftar" untuk menugaskan mahasiswa ke satu kegiatan {@link Pkl} (Praktik
 * Kerja Lapangan), lewat relasi {@link MahasiswaDapatPkl}. Menampilkan jendela modal pencarian
 * mahasiswa (NIM, nama, tahun angkatan, fakultas, prodi) dengan checkbox per baris yang menandakan
 * apakah mahasiswa tersebut sudah tertaut ke {@link Pkl} yang bersangkutan; checkbox pada header
 * kolom mencentang/melepas-centang seluruh baris sekaligus.
 *
 * <p>
 * Berbeda dari pola sejenis di helper lain pada paket ini, {@link #save()} di sini hanya
 * menambahkan relasi baru untuk baris yang tercentang (dan belum tertaut) — melepas centang tidak
 * menghapus relasi yang sudah ada; hanya penambahan yang ditangani.
 * </p>
 */
public class AmbilDataMahasiswaPklHelper {

	private Pkl pkl;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private Textbox nim;
	private Textbox nama;
	private Decimalbox tahunangkatan;

	private Combobox searchfakultas;
	private Combobox searchjurusan;

	/** Membuat helper dan menginisialisasi combobox pencarian fakultas/jurusan (termasuk opsi "Semua"). */
	public AmbilDataMahasiswaPklHelper() {
	}

	/** Perender baris grid: checkbox status tertaut (dicentang bila relasi {@link MahasiswaDapatPkl} sudah ada) plus label NIM, nama, dan tahun angkatan mahasiswa. */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		private MahasiswaDapatPklDao mahasiswaDapatPklDao = DaoFactory.getInstance().getMahasiswaDapatPklDao();

		private Session session = mahasiswaDapatPklDao.getCurrentSession();

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;
			MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);

			Integer jml = ((Number) session.createCriteria(MahasiswaDapatPkl.class)
					.setProjection(Projections.rowCount()).add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("pkl", pkl)).uniqueResult()).intValue();

			checkbox.setChecked(!jml.equals(0));

			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);

		}

	}

	/**
	 * Menambahkan relasi {@link MahasiswaDapatPkl} baru (dengan {@code keterangan} kosong) untuk
	 * setiap baris grid yang saat ini tercentang dan belum memiliki relasi ke {@link #pkl}.
	 * Melepas centang tidak menghapus relasi yang sudah tersimpan. Kegagalan per baris ditelan.
	 */
	@SuppressWarnings("unchecked")
	public void save() {
		MahasiswaDapatPklDao mahasiswaDapatPklDao = DaoFactory.getInstance().getMahasiswaDapatPklDao();
		Session session = mahasiswaDapatPklDao.getCurrentSession();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			@SuppressWarnings("rawtypes")
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked()) {
					Mahasiswa mahasiswa = (Mahasiswa) checkbox.getAttribute("mahasiswa");

					Integer jml = ((Number) session.createCriteria(MahasiswaDapatPkl.class)
							.setProjection(Projections.rowCount()).add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(Restrictions.eq("pkl", pkl)).uniqueResult()).intValue();

					if (jml.equals(0)) {
						MahasiswaDapatPkl mahasiswaDapatPkl = new MahasiswaDapatPkl();
						mahasiswaDapatPkl.setPkl(pkl);
						mahasiswaDapatPkl.setKeterangan("");
						mahasiswaDapatPkl.setMahasiswa(mahasiswa);
						session.save(mahasiswaDapatPkl);
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaPklHelper.java:125");
				// TODO: handle exception
			}
		}

	}

	/**
	 * Membangun dan menampilkan jendela modal pemilihan mahasiswa untuk {@code pkl} yang diberikan:
	 * form pencarian (NIM/nama/tahun angkatan/fakultas/prodi), grid ber-paging dengan checkbox per
	 * baris, dan tombol Simpan/Cari/Batal.
	 *
	 * @param pkl        kegiatan PKL tujuan penugasan mahasiswa
	 * @param dataLoader callback penyegar tampilan pemanggil setelah simpan
	 * @param window     jendela modal yang akan dibangun isinya (dibersihkan lebih dulu)
	 */
	public void display(final Pkl pkl, final DataLoader dataLoader, final MyWindow window) throws Exception {
		this.pkl = pkl;
		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.clear(window);
		window.setTitle("Ambil Data Mahasiswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan Mahasiswa (kosong = semua)"));
		row.appendChild(tahunangkatan = new Decimalbox());
		tahunangkatan.setWidth("90%");
		tahunangkatan.setTooltiptext(
				"Isi dengan tahun masuk mahasiswa, bukan tahun pelaksanaan PKL. Kosongkan untuk menampilkan semua angkatan.");

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
		button.setTooltiptext("Cari kandidat mahasiswa sesuai filter");
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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaPklHelper.java:267");

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
		window.onModal();
	}

	/**
	 * Menjalankan pencarian mahasiswa aktif berdasarkan kombinasi filter NIM/nama (ilike), tahun
	 * angkatan (persis, opsional), jurusan, dan fakultas; hasil diurutkan menurun berdasarkan tahun
	 * angkatan lalu menaik berdasarkan NIM, dibatasi {@link Common#MAX_RESULT}. Memuat ulang grid
	 * dengan hasilnya.
	 *
	 * @param event event pemicu (tombol Cari), tidak dipakai langsung
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
		grid.setEmptyMessage(mahasiswa.isEmpty()
				? "Tidak ada kandidat mahasiswa yang sesuai. Kosongkan Angkatan Mahasiswa atau sesuaikan NIM, Nama, Fakultas, dan Prodi."
				: "Tidak ada kandidat mahasiswa yang cocok dengan filter.");
		grid.setModelCheckMobile(strset);

	}

}
