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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.pkl.MahasiswaDaftarPkl;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper jendela "Ambil Data" (picker) untuk memasukkan mahasiswa ke dalam satu
 * {@link KelompokPkl} (kelompok PKL). Kandidat yang ditampilkan dibatasi hanya mahasiswa yang
 * sudah terdaftar dan DITERIMA pada {@link KelompokPkl#getPkl()} terkait (lewat
 * {@link MahasiswaDaftarPkl}), disaring lebih lanjut dengan NIM/nama/tahun angkatan/fakultas/
 * prodi. Ada blok kondisi pengecualian mahasiswa yang sudah tergabung di kelompok PKL lain (SQL
 * mentah) yang dikomentari (nonaktif) — dibiarkan apa adanya sesuai instruksi.
 *
 * <p>
 * Setiap baris menampilkan checkbox yang otomatis tercentang bila mahasiswa tersebut SUDAH
 * diterima ({@code diterima=true}) di {@code kelompokPkl} yang sedang diisi (lihat
 * {@link MahasiswaDapatKelompokPkl}). {@link #save()} menegakkan batas kuota kelompok
 * ({@link KelompokPkl#getKuota()}) — bila jumlah total (yang sudah ada + yang baru dicentang)
 * melebihi kuota, penyimpanan ditolak dengan pesan peringatan dan tidak ada baris yang disimpan.
 * </p>
 */
public class AmbilDataMahasiswaKelompokPklHelper {

	/** Kelompok PKL tujuan pemasukan mahasiswa, ditetapkan di {@link #display(KelompokPkl, DataLoader)}. */
	private KelompokPkl kelompokPkl;
	/** Grid kandidat mahasiswa hasil pencarian, diisi ulang oleh {@link #onSearchDefault(Event)}. */
	private MyGrid grid;

	/** Textbox filter NIM mahasiswa (cocok anywhere, case-insensitive). */
	private Textbox nim;
	/** Textbox filter nama mahasiswa (cocok anywhere, case-insensitive). */
	private Textbox nama;
	/** Filter tahun angkatan mahasiswa; kosong berarti semua angkatan ditampilkan. */
	private Decimalbox tahunangkatan;

	/** Combobox filter fakultas pada form pencarian, diinisialisasi ulang tiap {@link #display(KelompokPkl, DataLoader)}. */
	private Combobox searchfakultas;
	/** Combobox filter jurusan/prodi pada form pencarian, mengikuti pilihan {@link #searchfakultas}. */
	private Combobox searchjurusan;

	/** Konstruktor tanpa argumen; state (kelompokPkl, grid, filter combobox) baru ditetapkan saat {@link #display(KelompokPkl, DataLoader)} dipanggil. */
	public AmbilDataMahasiswaKelompokPklHelper() {
	}

	/** Perender baris grid: checkbox (label NIM, tercentang bila mahasiswa sudah diterima di {@link #kelompokPkl}), nama, tahun angkatan, dan jurusan mahasiswa. */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris kandidat mahasiswa ({@code arg1}, harus {@link Mahasiswa}): checkbox
		 * berlabel NIM (tercentang bila mahasiswa sudah diterima, {@code diterima=true}, di
		 * {@link #kelompokPkl}), nama, tahun angkatan, dan jurusan mahasiswa.
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;
			Checkbox checkbox = new Checkbox(mahasiswa.getNim());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);

			Session session = HibernateUtil.currentSession();
			Integer jml = ((Number) session.createCriteria(MahasiswaDapatKelompokPkl.class)
					.add(Restrictions.eq("diterima", true)).setProjection(Projections.rowCount())
					.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("kelompokPkl", kelompokPkl))
					.uniqueResult()).intValue();

			checkbox.setChecked(!jml.equals(0));

			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);
		}

	}

	/**
	 * Menghitung total anggota kelompok (yang sudah ada + baris tercentang pada grid) dan menolak
	 * penyimpanan (menampilkan peringatan, tanpa menyimpan apa pun) bila melebihi
	 * {@link KelompokPkl#getKuota()}. Bila dalam batas kuota, menyimpan baris
	 * {@link MahasiswaDapatKelompokPkl} baru (diterima=true) untuk setiap mahasiswa tercentang yang
	 * belum terdaftar sebelumnya di {@code kelompokPkl} — idempoten terhadap baris yang sudah ada.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean save() throws Exception {

		Session session = HibernateUtil.currentSession();

		int count = ((Number) HibernateUtil.currentSession().createCriteria(MahasiswaDapatKelompokPkl.class)
				.add(Restrictions.eq("kelompokPkl", kelompokPkl)).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			Object checkboxObject = row.getAttribute("checkbox");
			if (!(checkboxObject instanceof Checkbox)) {
				continue;
			}
			Checkbox checkbox = (Checkbox) checkboxObject;
			if (checkbox.isChecked()) {
				Mahasiswa mahasiswa = (Mahasiswa) checkbox.getAttribute("mahasiswa");
				Integer sudahAda = ((Number) session.createCriteria(MahasiswaDapatKelompokPkl.class)
						.setProjection(Projections.rowCount()).add(Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.eq("kelompokPkl", kelompokPkl)).uniqueResult()).intValue();
				if (sudahAda.equals(0)) {
					count++;
				}
			}
		}

		if (kelompokPkl.getKuota() != null && count > kelompokPkl.getKuota().intValue()) {
			MyMessageboxConfig.show("Jumlah mahasiswa tidak boleh melebihi kuota yang ditentukan", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		for (Row row : list) {

			Object checkboxObject = row.getAttribute("checkbox");
			if (!(checkboxObject instanceof Checkbox)) {
				continue;
			}
			Checkbox checkbox = (Checkbox) checkboxObject;
			if (checkbox.isChecked()) {
				Mahasiswa mahasiswa = (Mahasiswa) checkbox.getAttribute("mahasiswa");
				MahasiswaDapatKelompokPkl dataLama = (MahasiswaDapatKelompokPkl) session
						.createCriteria(MahasiswaDapatKelompokPkl.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.eq("kelompokPkl", kelompokPkl)).setMaxResults(1).uniqueResult();
				if (dataLama == null) {
					dataLama = new MahasiswaDapatKelompokPkl();
					dataLama.setKelompokPkl(kelompokPkl);
					dataLama.setKeterangan("");
					dataLama.setMahasiswa(mahasiswa);
				}
				dataLama.setDiterima(true);
				session.saveOrUpdate(dataLama);
			}
		}
		return true;
	}

	/**
	 * Membangun dan menampilkan jendela modal "Ambil Data Mahasiswa" (form filter, grid berpaging
	 * dengan checkbox pemilihan) untuk memasukkan mahasiswa ke {@code kelompokPkl}. Tombol Simpan
	 * memanggil {@link #save()} lalu {@code dataLoader.loadData(null)} untuk menyegarkan tampilan
	 * pemanggil.
	 *
	 * @param kelompokPkl kelompok PKL tujuan pemasukan mahasiswa
	 * @param dataLoader  komponen pemanggil yang disegarkan setelah data berhasil disimpan
	 */
	public void display(final KelompokPkl kelompokPkl, final DataLoader dataLoader) throws Exception {
		this.kelompokPkl = kelompokPkl;
		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		final MyWindow window = new MyWindow();
		window.setTitle("Ambil Data Mahasiswa");
		window.setWidth("750px");
		window.setHeight("540px");
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Mahasiswa"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

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
				if (save()) {
					dataLoader.loadData(null);
					window.detach();
				}
			}
		});
		button.setParent(toolbar);
		button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.setTooltiptext("Cari mahasiswa diterima sesuai filter");
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
				window.detach();
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(100);
		grid.getPagingChild().setMold("os");
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
		final Checkbox checkbox = new Checkbox("NIM");
		column.appendChild(checkbox);
		checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {
					try {
						Object checkboxObject = row.getAttribute("checkbox");
						if (!(checkboxObject instanceof Checkbox)) {
							continue;
						}
						Checkbox myCheckbox = (Checkbox) checkboxObject;
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaKelompokPklHelper.java:284");

					}
				}
			}
		});
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Angkatan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		onSearchDefault(null);

		window.setVisible(true);
		window.onModal();
	}

	/** Memuat ulang grid kandidat mahasiswa (terdaftar & diterima pada PKL terkait, sesuai filter aktif), dibatasi {@link Common#MAX_RESULT_1000} baris. {@code event} tidak dipakai. */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		Number jumlahDiterima = (Number) session.createCriteria(MahasiswaDaftarPkl.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("pkl", kelompokPkl.getPkl()))
				.add(Restrictions.eq("terima", MahasiswaDaftarPkl.DITERIMA)).uniqueResult();
		List<Mahasiswa> mahasiswa = ConstantValues.simpleList(session.createCriteria(MahasiswaDaftarPkl.class)

//				.add(Restrictions.sqlRestriction(
//						"this_.mahasiswa not in (select a.mahasiswa from mahasiswa_dapat_kelompok_kelompok_pkl a inner join kelompok_pkl b on (a.kelompok_pkl = b.id) where b.pkl="
//								+ kelompokPkl.getPkl().getId() + ")"))

				.setProjection(Projections.property("mahasiswa.id"))

				.add(Restrictions.eq("pkl", kelompokPkl.getPkl()))

				.add(Restrictions.eq("terima", MahasiswaDaftarPkl.DITERIMA)).createCriteria("mahasiswa")

				.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nim", nim.getValue().trim(), MatchMode.ANYWHERE))

				.add(tahunangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", tahunangkatan.getValue().intValue()))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.setMaxResults(Common.MAX_RESULT_1000), Mahasiswa.class, false);
		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new MahasiswaRenderer());
		if (mahasiswa.isEmpty()) {
			if (jumlahDiterima == null || jumlahDiterima.intValue() == 0) {
				grid.setEmptyMessage(
						"Belum ada pendaftar yang berstatus DITERIMA pada kegiatan PKL ini. Terima mahasiswa terlebih dahulu melalui menu Seleksi Penerima PKL.");
			} else {
				grid.setEmptyMessage(
						"Ada mahasiswa yang sudah diterima, tetapi tidak cocok dengan filter. Kosongkan Angkatan Mahasiswa atau pilih Fakultas/Prodi yang sesuai.");
			}
		} else {
			grid.setEmptyMessage("Tidak ada mahasiswa yang cocok dengan filter.");
		}
		grid.setModelCheckMobile(strset);

	}

}
