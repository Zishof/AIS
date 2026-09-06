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
import org.zkoss.zul.Messagebox;
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
import ais.database.dao.MahasiswaDapatBeasiswaDao;
import ais.database.dao.beasiswa.MahasiswaDaftarBeasiswaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Beasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.beasiswa.MahasiswaDaftarBeasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Composer ZK untuk dialog "Ambil Data Mahasiswa" pada seleksi penerima {@link Beasiswa}: menampilkan
 * grid pencarian mahasiswa (NIM/rentang NIM/nama/fakultas/jurusan/tahun angkatan) dengan checkbox
 * pilih yang tercentang otomatis bagi mahasiswa yang sudah diterima ({@code terima=1}) pada beasiswa
 * tersebut. {@link #save()} mendaftarkan setiap mahasiswa yang dicentang sebagai baris
 * {@link MahasiswaDaftarBeasiswa} baru (hanya bila belum terdaftar), memvalidasi kelayakan lewat
 * {@code Common.checkApakahMemenuhiSyaratBeasiswa} — mahasiswa yang tidak memenuhi syarat tidak
 * disimpan dan namanya dikumpulkan ke satu pesan peringatan yang ditampilkan di akhir proses.
 */
public class AmbilDataMahasiswaSeleksiBeasiswaHelper {

	/** Beasiswa tujuan seleksi, ditetapkan di {@link #display(Beasiswa, DataLoader, MyWindow)}. */
	private Beasiswa beasiswa;
	/** Grid kandidat mahasiswa hasil pencarian, diisi ulang oleh {@link #onSearchDefault(Event)}. */
	private MyGrid grid;


	/** Paging server-side per 5 baris (pola {@code AmbilDataPagingHelper}); saat ini disiapkan tapi belum dipakai langsung oleh {@link #onSearchDefault(Event)}. */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	/** Textbox filter NIM mahasiswa (cocok anywhere, case-insensitive). */
	private Textbox nim;
	/** Textbox filter nama mahasiswa (cocok anywhere, case-insensitive). */
	private Textbox nama;
	/** Filter tahun angkatan mahasiswa; kosong berarti semua angkatan ditampilkan. */
	private Decimalbox tahunangkatan;

	/** Combobox filter fakultas pada form pencarian, diinisialisasi ulang tiap {@link #display(Beasiswa, DataLoader, MyWindow)}. */
	private Combobox searchfakultas;
	/** Combobox filter jurusan/prodi pada form pencarian, mengikuti pilihan {@link #searchfakultas}. */
	private Combobox searchjurusan;

	/** Batas bawah rentang NIM (inklusif, {@code >=}); kosong berarti tidak dibatasi. */
	private Textbox dariNim;
	/** Batas atas rentang NIM (inklusif, {@code <=}); kosong berarti tidak dibatasi. */
	private Textbox sampaiNim;

	/** Konstruktor tanpa argumen; state (beasiswa, grid, filter combobox) baru ditetapkan saat {@link #display(Beasiswa, DataLoader, MyWindow)} dipanggil. */
	public AmbilDataMahasiswaSeleksiBeasiswaHelper() {
	}

	/** Row renderer grid pencarian: checkbox pilih (tercentang bila mahasiswa sudah diterima pada beasiswa ini), NIM, nama, dan tahun angkatan. */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		/** DAO {@link MahasiswaDapatBeasiswaDao} dipakai untuk memperoleh {@link #session} Hibernate aktif. */
		private MahasiswaDapatBeasiswaDao mahasiswaDapatBeasiswaDao = DaoFactory.getInstance()
				.getMahasiswaDapatBeasiswaDao();

		/** Sesi Hibernate aktif, dipakai untuk mengecek status pendaftaran-diterima tiap kandidat mahasiswa. */
		private Session session = mahasiswaDapatBeasiswaDao.getCurrentSession();

		/**
		 * Merender satu baris kandidat mahasiswa ({@code arg1}, harus {@link Mahasiswa}): checkbox
		 * (tercentang bila mahasiswa sudah terdaftar dan diterima, {@code terima=1}, pada
		 * {@link #beasiswa} ini), NIM, nama, dan tahun angkatan.
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

			Integer jml = ((Number) session.createCriteria(MahasiswaDaftarBeasiswa.class)
					.setProjection(Projections.rowCount()).add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("beasiswa", beasiswa)).add(Restrictions.eq("terima", 1)).uniqueResult())
					.intValue();

			checkbox.setChecked(!jml.equals(0));

			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);

		}

	}

	/**
	 * Mendaftarkan setiap mahasiswa yang dicentang di grid dan belum memiliki baris
	 * {@link MahasiswaDaftarBeasiswa} sebagai pendaftar baru beasiswa ini, memvalidasi kelayakan lewat
	 * {@code Common.checkApakahMemenuhiSyaratBeasiswa} sebelum disimpan. Mahasiswa yang tidak memenuhi
	 * syarat dilewati dan namanya dikumpulkan untuk satu dialog peringatan di akhir.
	 *
	 * @throws Exception diteruskan dari kegagalan Hibernate saat menyimpan
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() throws Exception {
		MahasiswaDaftarBeasiswaDao mahasiswaDaftarBeasiswaDao = DaoFactory.getInstance()
				.getMahasiswaDaftarBeasiswaDao();
		Session session = mahasiswaDaftarBeasiswaDao.getCurrentSession();

		String warning = "";
		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked()) {
					Mahasiswa mahasiswa = (Mahasiswa) checkbox.getAttribute("mahasiswa");

					MahasiswaDaftarBeasiswa mahasiswaDaftarBeasiswa = (MahasiswaDaftarBeasiswa) (session
							.createCriteria(MahasiswaDaftarBeasiswa.class).add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(Restrictions.eq("beasiswa", beasiswa)).uniqueResult());

					if (mahasiswaDaftarBeasiswa == null) {
						mahasiswaDaftarBeasiswa = new MahasiswaDaftarBeasiswa();
						mahasiswaDaftarBeasiswa.setBeasiswa(beasiswa);
						mahasiswaDaftarBeasiswa.setKeterangan("");
						mahasiswaDaftarBeasiswa.setNama("");
						mahasiswaDaftarBeasiswa.setMahasiswa(mahasiswa);
						mahasiswaDaftarBeasiswa
								.setMemenuhiSyarat(Common.checkApakahMemenuhiSyaratBeasiswa(mahasiswaDaftarBeasiswa));
						if (mahasiswaDaftarBeasiswa.getMemenuhiSyarat()) {
							session.save(mahasiswaDaftarBeasiswa);
						} else {
							warning += Common.pesan(
									"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} belum memenuhi syarat untuk beasiswa \"{V3}\". Langkah yang dapat dilakukan: (1) periksa kembali kriteria dan persyaratan beasiswa tersebut; (2) lengkapi atau perbarui data mahasiswa yang belum sesuai; (3) ulangi proses penyeleksian setelah syarat terpenuhi.\n\n",
									mahasiswa.getNim(), mahasiswa.getNama(), beasiswa.getNama());
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaSeleksiBeasiswaHelper.java:143");
				// TODO: handle exception
			}
		}

		if (!warning.isEmpty()) {
			MyMessageboxConfig.show(warning, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}

	}

	/**
	 * Membangun window modal pencarian+pilih mahasiswa (filter, grid checkbox, tombol Simpan/Cari/
	 * Batal) untuk beasiswa yang diberikan dan memuat data awal.
	 *
	 * @param beasiswa   beasiswa yang penerimanya dipilih
	 * @param dataLoader callback yang dipanggil ulang setelah "Simpan" berhasil
	 * @param window     window yang dipakai sebagai kanvas dialog (dibersihkan lebih dulu)
	 */
	public void display(final Beasiswa beasiswa, final DataLoader dataLoader, final MyWindow window) throws Exception {
		this.beasiswa = beasiswa;
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

		Columns columns = new Columns();
		columns.setParent(searchgrid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(nim = new Textbox());
		nim.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dari Nim"));
		row.appendChild(dariNim = new Textbox());
		dariNim.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Mahasiswa"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai Nim"));
		row.appendChild(sampaiNim = new Textbox());
		sampaiNim.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan Mahasiswa (kosong = semua)"));
		row.appendChild(tahunangkatan = new Decimalbox());
		tahunangkatan.setWidth("90%");
		tahunangkatan.setTooltiptext(
				"Isi dengan tahun masuk mahasiswa. Kosongkan untuk menampilkan semua angkatan.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

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

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaSeleksiBeasiswaHelper.java:307");

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

	/** Mencari mahasiswa aktif sesuai filter (dibatasi {@link Common#MAX_RESULT} baris) dan me-render ulang grid. */
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
				.add(dariNim.getValue().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ge("nim", dariNim.getValue()))
				.add(sampaiNim.getValue().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.le("nim", sampaiNim.getValue()))
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
