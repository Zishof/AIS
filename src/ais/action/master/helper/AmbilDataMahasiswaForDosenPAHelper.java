package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
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
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Kelas;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper jendela modal untuk menetapkan (atau MEMINDAHKAN) satu {@link Dosen} sebagai
 * Dosen PA (Pembimbing Akademik) bagi banyak {@link Mahasiswa} sekaligus. Pencarian
 * SENGAJA tidak difilter berdasarkan Dosen PA yang sudah dimiliki mahasiswa — mahasiswa
 * yang masih menunjuk PA lama tetap dapat ditemukan (kolom "Dosen PA" menampilkan PA
 * saat ini) agar dapat dialihkan ke dosen tujuan lewat menu yang sama. Bila
 * Lingkup kandidat ditentukan oleh filter Fakultas/Prodi yang terlihat pada dialog, bukan
 * diam-diam mengikuti homebase dosen tujuan. Dengan begitu pencarian NIM tetap deterministik
 * dan dosen lintas prodi tetap dapat dipakai bila memang dipilih operator.
 * Menyimpan mengubah baik {@code mahasiswa.dosen} (referensi langsung) maupun
 * {@link KrsMahasiswa#getDosenPa()} (via {@link Common#singkronkanKrsMahasiswa}).
 */
public class AmbilDataMahasiswaForDosenPAHelper {

	private Dosen dosen;
	private MyGrid grid;


	/* Penetapan PA lazim dilakukan massal; lima baris per halaman terlalu sedikit. */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper =
			new ais.ui.util.AmbilDataPagingHelper(Common.ROWS_COUNT_ON_PAGE_100);
	private Textbox nim;
	private Textbox nama;
	private Decimalbox tahunangkatan;
	private Textbox dariNim;
	private Textbox sampaiNim;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private AmbilDataKelasBanbox searchkelas;

	/** Menyiapkan combobox filter Fakultas dan Prodi (dengan opsi "Semua") lewat {@link Common#initFakultasDanJurusanDanSemua}. */
	public AmbilDataMahasiswaForDosenPAHelper() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	/** Perender baris grid mahasiswa: checkbox pemilihan, lalu label NIM/Nama/Tahun Angkatan/Dosen PA saat ini/Kelas. */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris {@link Mahasiswa}: checkbox pemilihan (tidak
		 * pra-dicentang), lalu label NIM/Nama/Tahun Angkatan, serta nama Dosen PA saat
		 * ini dan Kelas yang diambil dari {@link KrsMahasiswa} terkait (via
		 * {@link Common#ambilKrsMahasiswaTanpaSinkronisasi}) — sehingga operator dapat
		 * melihat apakah mahasiswa sudah punya PA sebelum memindahkannya.
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;

			KrsMahasiswa krsMahasiswa = Common.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa);

			MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);

			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);
			new Label(krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? ""
					: krsMahasiswa.getDosenPa().getNama()).setParent(arg0);
			new Label(krsMahasiswa == null || krsMahasiswa.getKelas() == null
					? (mahasiswa.getKelas() == null ? "" : mahasiswa.getKelas())
					: krsMahasiswa.getKelas()).setParent(arg0);
		}

	}

	/**
	 * Untuk setiap mahasiswa tercentang di grid: mengeset {@code mahasiswa.dosen} ke id
	 * {@link #dosen} terpilih dan menyimpannya, lalu menyinkronkan
	 * {@link KrsMahasiswa} (via {@link Common#singkronkanKrsMahasiswa}) dan memperbarui
	 * {@code dosenPa}-nya bila belum sesuai. Efeknya berlaku sebagai
	 * penetapan/pemindahan PA — mahasiswa yang sebelumnya punya PA lain akan
	 * dialihkan ke {@link #dosen} ini.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() {

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked()) {
					Mahasiswa mahasiswa = (Mahasiswa) checkbox.getAttribute("mahasiswa");

					mahasiswa.setDosen(dosen.getId());
					Common.refreshSaveOrUpdate(mahasiswa);

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);

					if (krsMahasiswa.getDosenPa() == null || !krsMahasiswa.getDosenPa().getId().equals(dosen.getId())) {
						krsMahasiswa.setDosenPa(dosen);
						Common.refreshSaveOrUpdate(krsMahasiswa);

					}

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaForDosenPAHelper.java:129");
				// TODO: handle exception
			}
		}

	}

	/**
	 * Membangun dan menampilkan jendela modal "Ambil Data Mahasiswa" untuk penetapan
	 * Dosen PA: form filter (NIM/rentang NIM/Nama/Fakultas/Tahun Angkatan/Prodi/Kelas),
	 * grid berpaging server-side dengan checkbox "pilih semua", peringatan bahwa
	 * mahasiswa dengan PA lama tetap dapat dipilih untuk dipindahkan, dan tombol
	 * Simpan/Batal.
	 *
	 * @param dosen      dosen yang akan ditetapkan sebagai PA bagi mahasiswa terpilih
	 * @param dataLoader callback pemuatan ulang data pemanggil setelah Simpan (dipanggil dengan {@code dosen})
	 * @param window     jendela ZK yang akan diisi dan ditampilkan sebagai modal
	 */
	public void display(final Dosen dosen, final DataLoader dataLoader, final MyWindow window) {
		this.dosen = dosen;
		Common.clear(window);
		window.setTitle("Ambil Data Mahasiswa");
		window.setWidth("750px");
		window.setHeight("540px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Borderlayout myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);

		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		// Pager tunggal via AmbilDataPagingHelper; pager manual dihapus (double paging).

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("240px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
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
		tahunangkatan.setTooltiptext("Isi dengan tahun masuk mahasiswa. Kosongkan untuk menampilkan semua angkatan.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(searchkelas = new AmbilDataKelasBanbox());
		searchkelas.setWidth("90%");

		final EventListener eventCariDariAwal = pagingHelper.buatEventCari(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		searchkelas.setEventListener(eventCariDariAwal);
		nim.addEventListener(Events.ON_OK, eventCariDariAwal);
		nama.addEventListener(Events.ON_OK, eventCariDariAwal);
		dariNim.addEventListener(Events.ON_OK, eventCariDariAwal);
		sampaiNim.addEventListener(Events.ON_OK, eventCariDariAwal);
		tahunangkatan.addEventListener(Events.ON_OK, eventCariDariAwal);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);
		Label petunjuk = new Label(
				"Mahasiswa yang sudah memiliki Dosen PA tetap dapat dicari dan dipilih untuk dipindahkan. "
						+ "Periksa kolom Dosen PA sebelum menyimpan.");
		petunjuk.setStyle("display:block;color:#475569;background:#f8fafc;border:1px solid #cbd5e1;"
				+ "border-radius:6px;padding:7px 10px;margin:4px 0;");
		petunjuk.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.setTooltiptext("Cari kandidat mahasiswa sesuai filter");
		button.addEventListener("onClick", eventCariDariAwal);
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT_100. */
		pagingHelper.pasangOnPaging(new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		pagingHelper.pasangGridDanPaging(myCenter1, grid);

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaForDosenPAHelper.java:291");

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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Angkatan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dosen PA");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelas");

		onSearchDefault(null);

		South south = new South();
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
				dataLoader.loadData(dosen);
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);

		// button = new MyToolbarbuttonConfig("Ambil Semua", "/img/save.gif");
		// button.setTooltiptext("Simpan");
		// button.addEventListener("onClick", new EventListener() {
		// @Override
		// public void onEvent(Event event) throws Exception {
		// saveSemua();
		// dataLoader.loadData(null);
		// window.setVisible(false);
		// }
		// });
		// button.setParent(toolbar);

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

	// @SuppressWarnings({ "unchecked" })
	// public void saveSemua() throws InterruptedException {
	//
	// List<Mahasiswa> mahasiswas =
	// initCriteria(true).setMaxResults(1000).list();
	//
	// Session session = HibernateUtil.currentSession();
	// for (Mahasiswa mahasiswa : mahasiswas) {
	// DosenPembimbingAkademik dosenPembimbingAkademik =
	// (DosenPembimbingAkademik) session
	// .createCriteria(DosenPembimbingAkademik.class).add(Restrictions.eq("mahasiswa",
	// mahasiswa))
	// .setMaxResults(1).uniqueResult();
	// if (dosenPembimbingAkademik == null) {
	// dosenPembimbingAkademik = new DosenPembimbingAkademik();
	// }
	// dosenPembimbingAkademik.setDosen(dosen);
	// dosenPembimbingAkademik.setMahasiswa(mahasiswa);
	// Common.refreshSaveOrUpdate(session, dosenPembimbingAkademik);
	//
	// }
	//
	// }

	/**
	 * Membangun kriteria pencarian mahasiswa aktif sesuai seluruh filter form
	 * (kelas/nama/NIM/rentang NIM/tahun angkatan/prodi/fakultas). Tidak ada pembatas
	 * tersembunyi dari homebase dosen; pembatas organisasi hanya berasal dari filter
	 * Fakultas dan Prodi yang dapat dilihat serta diubah operator.
	 *
	 * @param order tambahkan pengurutan (tahun angkatan menurun, lalu NIM menaik) bila {@code true}
	 * @return kriteria Hibernate atas {@link Mahasiswa}
	 */
	public Criteria initCriteria(boolean order) {
		Kelas kelas = (Kelas) (searchkelas.getAttribute("kelas"));
		String nimDari = dariNim.getValue() == null ? "" : dariNim.getValue().trim();
		String nimSampai = sampaiNim.getValue() == null ? "" : sampaiNim.getValue().trim();
		if (!nimDari.isEmpty() && !nimSampai.isEmpty() && nimDari.compareToIgnoreCase(nimSampai) > 0) {
			String temp = nimDari;
			nimDari = nimSampai;
			nimSampai = temp;
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				/*
				 * Jangan menyaring berdasarkan mahasiswa.dosen. Menu ini juga merupakan
				 * sarana PEMINDAHAN PA: mahasiswa yang masih menunjuk PA lama harus tetap
				 * dapat ditemukan, diperlihatkan PA-nya pada kolom Dosen PA, lalu dipilih
				 * untuk dialihkan ke dosen tujuan. Filter lama (dosen null/0 saja) membuat
				 * pencarian NIM yang benar menghasilkan nol baris.
				 */

				.add(kelas != null && !kelas.getNama().trim().isEmpty()
						? Restrictions.ilike("kelas", kelas.getNama().trim(), MatchMode.EXACT)
						: Restrictions.sqlRestriction("true"))

				.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("nim", nim.getValue().trim(), MatchMode.ANYWHERE))
				.add(tahunangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", tahunangkatan.getValue().intValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.add(nimDari.isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ge("nim", nimDari))
				.add(nimSampai.isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.le("nim", nimSampai))

				.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false));

		if (order)
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));

		return criteria;
	}

	/**
	 * Memuat ulang grid mahasiswa sesuai {@link #initCriteria(boolean)} memakai paging
	 * server-side.
	 *
	 * @param event tidak digunakan; parameter kontrak listener/pemanggilan langsung
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		List<Mahasiswa> mahasiswa = pagingHelper.cariDenganCriteria(initCriteria(true), Mahasiswa.class);

		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setEmptyMessage(mahasiswa.isEmpty()
				? "Tidak ada kandidat mahasiswa yang sesuai. Kosongkan Angkatan Mahasiswa atau sesuaikan filter pencarian."
				: "Tidak ada kandidat mahasiswa yang cocok dengan filter.");
		grid.setModelCheckMobile(strset);

	}
}
