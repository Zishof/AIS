package ais.action.master.helper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
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
import ais.database.dao.kedokteran.PHDHasMahasiswaDao;
import ais.database.dao.kedokteran.PertemuanHasDosenDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.kedokteran.PHDHasMahasiswa;
import ais.database.model.kedokteran.PertemuanHasDosen;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper jendela modal (modul Kedokteran) untuk mengatur keanggotaan mahasiswa pada satu
 * sesi kelompok kecil {@link PertemuanHasDosen} (pertemuan dengan dosen fasilitator
 * tertentu), disimpan sebagai baris {@link PHDHasMahasiswa}. Menampilkan grid mahasiswa
 * peserta {@link Perkuliahan} terkait dengan checkbox keanggotaan per baris (kolom
 * tambahan menampilkan dosen fasilitator yang sudah menaungi mahasiswa tersebut pada
 * pertemuan kedokteran yang sama, bila ada), filter pencarian Nama/Fakultas/Prodi dengan
 * paging server-side ({@link ais.ui.util.AmbilDataPagingHelper}), checkbox "pilih semua"
 * pada header, dan tombol Simpan yang menyelaraskan (tambah/hapus) baris
 * {@code PHDHasMahasiswa} sesuai status checkbox.
 */
public class AmbilDataMahasiswaKedokteranHelper {
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private Textbox nama;
	private Set<PHDHasMahasiswa> deletedMahasiswas = new HashSet<PHDHasMahasiswa>();
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private PertemuanHasDosen pertemuanHasDosen;
	private Perkuliahan perkuliahan;

	// private PertemuanHasDosen pertemuanHasDosen;

	/** Menyiapkan combobox filter Fakultas dan Prodi (dengan opsi "Semua") lewat {@link Common#initFakultasDanJurusanDanSemua}. */
	public AmbilDataMahasiswaKedokteranHelper() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
	}

	/** Perender baris grid mahasiswa: checkbox keanggotaan pada {@link #pertemuanHasDosen}, kolom identitas mahasiswa, dan nama dosen fasilitator yang sudah menaunginya (bila ada) pada pertemuan kedokteran yang sama. */
	class DosenRenderer extends ais.ui.util.MyRowRenderer {
		private PertemuanHasDosenDao pertemuanHasDosenDao = DaoFactory.getInstance().getPertemuanHasDosenDao();
		private Session session = pertemuanHasDosenDao.getCurrentSession();

		/**
		 * Merender satu baris {@link Mahasiswa}: checkbox yang status awalnya dicentang
		 * bila mahasiswa sudah terdaftar pada {@link #pertemuanHasDosen} saat ini,
		 * label NIM/Nama/Jurusan/Fakultas, serta nama dosen fasilitator lain yang sudah
		 * menaungi mahasiswa tersebut pada pertemuan kedokteran yang sama (bila ada,
		 * untuk membantu deteksi duplikasi penugasan).
		 *
		 * <p>
		 * Listener {@code onCheck} pada baris ini menambahkan entri
		 * {@link PHDHasMahasiswa} yang sudah ada ke {@link #deletedMahasiswas} saat
		 * checkbox DICENTANG, dan menghapusnya dari set tersebut saat checkbox
		 * DILEPAS — lihat catatan di {@link #save()} tentang bagaimana set ini dipakai.
		 * </p>
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);
			// checkbox.setId("" + dosen.getId());

			Integer jml = ((Number) session.createCriteria(PHDHasMahasiswa.class).setProjection(Projections.rowCount())
					.add(Restrictions.eq("pertemuanHasDosen", pertemuanHasDosen))
					.add(Restrictions.eq("mahasiswa", mahasiswa)).uniqueResult()).intValue();

			PHDHasMahasiswa phdHasMahasiswa = (PHDHasMahasiswa) session.createCriteria(PHDHasMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa)).createCriteria("pertemuanHasDosen")
					.add(Restrictions.eq("pertemuanKedokteran", pertemuanHasDosen.getPertemuanKedokteran()))

					.uniqueResult();

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					PHDHasMahasiswa pHasMahasiswa = (PHDHasMahasiswa) HibernateUtil.currentSession()
							.createCriteria(PHDHasMahasiswa.class)
							.add(Restrictions.eq("pertemuanHasDosen", pertemuanHasDosen))
							.add(Restrictions.eq("mahasiswa", mahasiswa)).uniqueResult();
					if (pHasMahasiswa != null) {
						if (!checkbox.isChecked()) {
							deletedMahasiswas.remove(pHasMahasiswa);
						} else {
							deletedMahasiswas.add(pHasMahasiswa);
						}
					}

				}
			});
			checkbox.setChecked(!jml.equals(0));
			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getFakultas().getNama())
					.setParent(arg0);

			new Label(phdHasMahasiswa == null ? "" : phdHasMahasiswa.getPertemuanHasDosen().getDosen().getNama())
					.setParent(arg0);

		}
	}

	/**
	 * Menyimpan hasil pemilihan mahasiswa untuk {@link #pertemuanHasDosen}: untuk setiap
	 * baris grid, bila checkbox tercentang maka entri {@link PHDHasMahasiswa} disimpan
	 * (dibuat baru atau diperbarui), bila tidak tercentang entri terkait dihapus.
	 * Setelahnya, seluruh entri pada {@link #deletedMahasiswas} juga dihapus.
	 *
	 * <p>
	 * <b>Catatan:</b> {@link #deletedMahasiswas} diisi dari listener {@code onCheck}
	 * pada {@link DosenRenderer} dengan arah yang berlawanan intuisi namanya — entri
	 * ditambahkan ke set ini saat checkbox DICENTANG oleh pengguna (bukan saat
	 * dilepas), dan dihapus dari set saat checkbox dilepas. Karena langkah pertama di
	 * atas sudah menyimpan baris untuk checkbox yang tercentang, penghapusan tambahan
	 * berdasarkan {@link #deletedMahasiswas} pada method ini dapat berakhir
	 * menghapus kembali entri yang baru saja disimpan untuk baris yang secara manual
	 * dicentang pengguna pada sesi tampilan yang sama.
	 * </p>
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() {
		PHDHasMahasiswaDao phdHasMahasiswaDao = DaoFactory.getInstance().getPhdHasMahasiswaDao();
		Session session = phdHasMahasiswaDao.getCurrentSession();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked()) {

					Mahasiswa mhs = (Mahasiswa) checkbox.getAttribute("mahasiswa");
					PHDHasMahasiswa mahasiswas = (PHDHasMahasiswa) session.createCriteria(PHDHasMahasiswa.class)
							.add(Restrictions.eq("pertemuanHasDosen", this.pertemuanHasDosen))
							.add(Restrictions.eq("mahasiswa", mhs)).setMaxResults(1).uniqueResult();

					if (mahasiswas == null) {
						mahasiswas = new PHDHasMahasiswa();
					}

					mahasiswas.setPertemuanHasDosen(this.pertemuanHasDosen);
					mahasiswas.setMahasiswa(mhs);
					session.saveOrUpdate(mahasiswas);

				} else {
					Mahasiswa mhs = (Mahasiswa) checkbox.getAttribute("mahasiswa");
					PHDHasMahasiswa mahasiswas = (PHDHasMahasiswa) session.createCriteria(PHDHasMahasiswa.class)
							.add(Restrictions.eq("pertemuanHasDosen", this.pertemuanHasDosen))
							.add(Restrictions.eq("mahasiswa", mhs)).setMaxResults(1).uniqueResult();

					if (mahasiswas == null) {
						mahasiswas = new PHDHasMahasiswa();
					}
					session.delete(mahasiswas);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaKedokteranHelper.java:162");
				// TODO: handle exception
			}
		}

		if (deletedMahasiswas != null) {

			for (PHDHasMahasiswa pertdosens : deletedMahasiswas) {
				session.delete(pertdosens);
			}

		}

	}

	/**
	 * Membangun dan menampilkan jendela modal "Data Mahasiswa" untuk mengatur
	 * keanggotaan mahasiswa pada {@code pertemuanHasDosen}: form filter
	 * (Nama/Fakultas/Prodi), grid berpaging server-side dengan checkbox "pilih semua",
	 * dan tombol Simpan/Batal. Jendela dibuat baru dan ditambahkan ke root halaman saat
	 * ini (bukan mengisi ulang jendela yang sudah ada).
	 *
	 * @param pertemuanHasDosen sesi kelompok kecil (pertemuan + dosen fasilitator) yang diatur keanggotaannya
	 * @param perkuliahan       perkuliahan induk, dipakai untuk membatasi kandidat mahasiswa pada pesertanya
	 * @param dataLoader        callback pemuatan ulang data pemanggil setelah Simpan
	 */
	public void display(PertemuanHasDosen pertemuanHasDosen, Perkuliahan perkuliahan, final DataLoader dataLoader) {
		this.perkuliahan = perkuliahan;
		this.pertemuanHasDosen = pertemuanHasDosen;
		final MyWindow window = new MyWindow();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		// Common.clear(window);
		window.setTitle("Data Mahasiswa");
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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
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
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getChildren().get(0);
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaKedokteranHelper.java:284");

					}
				}
			}
		});

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
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

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dosen Fasilitator");
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
	}

	/**
	 * Memuat ulang grid dengan mahasiswa peserta {@link #perkuliahan} (via
	 * {@link Detailperkuliahan}), menggunakan paging server-side
	 * ({@link ais.ui.util.AmbilDataPagingHelper}).
	 *
	 * @param event tidak digunakan; parameter kontrak listener/pemanggilan langsung
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Mahasiswa> mahasiswa = pagingHelper.cariDenganCriteria(session.createCriteria(Detailperkuliahan.class)
				.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.property("mahasiswa"))
				// .add(Restrictions
				// .eq("persetujuan", Detailperkuliahan.DISETUJUI))

				.addOrder(Order.asc("id")).setMaxResults(Common.MAX_RESULT), Mahasiswa.class);

		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new DosenRenderer());
		grid.setModelCheckMobile(strset);

	}

}
