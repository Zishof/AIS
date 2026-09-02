package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import ais.ui.util.MyMessageboxConfig;

import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
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
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.FormulirKegiatan;
import ais.database.model.FormulirKegiatanPeserta;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper jendela modal untuk mendaftarkan banyak {@link Mahasiswa} sekaligus sebagai
 * peserta ({@link FormulirKegiatanPeserta}) pada satu {@link FormulirKegiatan}. Filter
 * Fakultas/Prodi dikunci otomatis ke fakultas/jurusan kegiatan bila kegiatan sudah
 * membatasi keduanya. Mahasiswa yang sudah terdaftar ditampilkan dengan checkbox
 * tercentang dan dinonaktifkan (tidak dapat dibatalkan dari sini). Bila kegiatan
 * tergabung dalam {@code grupFormulirKegiatan}, penyimpanan mencegah pendaftaran ganda
 * lintas kegiatan dalam grup yang sama — mahasiswa yang sudah terdaftar di kegiatan lain
 * pada grup tersebut ditolak dengan pesan yang menyebutkan nama kegiatan terkait.
 * Setiap peserta baru diberi {@code kode} berurutan 5 digit unik per kegiatan.
 */
public class AmbilDataMahasiswaForFormulirKegiatanHelper {

	private FormulirKegiatan formulirKegiatan;
	private MyGrid grid;

	private Textbox nim;
	private Textbox nama;
	private Decimalbox tahunangkatan;
	private Textbox dariNim;
	private Textbox sampaiNim;

	private Combobox searchstatusmahasiswa = new Combobox();

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private Paging paging;

	/**
	 * Menyiapkan combobox filter Fakultas/Prodi/Status Mahasiswa dan paging (50
	 * baris/halaman). Bila {@code formulirKegiatan} sudah menetapkan fakultas/jurusan
	 * tertentu, combobox terkait langsung dipilih dan dikunci ({@code setDisabled(true)})
	 * agar pencarian tidak dapat keluar dari lingkup kegiatan.
	 *
	 * @param formulirKegiatan kegiatan yang pesertanya akan ditambahkan
	 */
	public AmbilDataMahasiswaForFormulirKegiatanHelper(FormulirKegiatan formulirKegiatan) {
		this.formulirKegiatan = formulirKegiatan;
		Fakultas fakultas = formulirKegiatan.getFakultas();
		Jurusan jurusan = formulirKegiatan.getJurusan();
		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class,
				Restrictions.eq("aktif", true));

		/**
		 * Event listener lokal milik {@link AmbilDataMahasiswaForFormulirKegiatanHelper}. Kelas ini menangani event
		 * untuk komponen induk dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataMahasiswaForFormulirKegiatanHelper}
		 * dan dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see AmbilDataMahasiswaForFormulirKegiatanHelper
		 */
		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchjurusan);
				searchjurusan.setSelectedItem(null);
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}

		}

		searchfakultas.addEventListener("onChange", new SearchFakultasEventListener());

		Common.insertCombo(searchstatusmahasiswa, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);

		if (fakultas != null) {
			Common.selectComboItem(searchfakultas, fakultas);
			Common.clear(searchjurusan);
			Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", fakultas));
			searchfakultas.setDisabled(true);
		} else {
			searchfakultas.setDisabled(false);
		}

		if (jurusan != null) {
			Common.selectComboItem(searchjurusan, jurusan);
			searchjurusan.setDisabled(true);
		} else {
			searchjurusan.setDisabled(false);
		}

		paging = new Paging();
		Common.initPaging50(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	/** Perender baris grid mahasiswa: checkbox (dikunci tercentang bila sudah terdaftar sebagai peserta) dan label identitas. */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris {@link Mahasiswa}: checkbox yang tercentang DAN
		 * dinonaktifkan bila mahasiswa sudah terdaftar sebagai
		 * {@link FormulirKegiatanPeserta} pada {@link #formulirKegiatan}, lalu label
		 * NIM/Nama/Tahun Angkatan.
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;
			Session session = HibernateUtil.currentSession();
			int count = ((Number) session.createCriteria(FormulirKegiatanPeserta.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("formulirKegiatan", formulirKegiatan)).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);
			checkbox.setChecked(count != 0);
			checkbox.setDisabled(count != 0);

			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);

		}
	}

	/**
	 * Mendaftarkan setiap mahasiswa yang checkbox-nya tercentang dan tidak dinonaktifkan
	 * sebagai peserta {@link #formulirKegiatan}. Bila kegiatan tergabung dalam
	 * {@code grupFormulirKegiatan}, terlebih dahulu dicek apakah mahasiswa sudah
	 * terdaftar pada kegiatan lain (siswa/guru/mahasiswa/dosen) dalam grup yang sama —
	 * bila ya, PROSES SELURUH PENYIMPANAN DIHENTIKAN (return) dengan pesan peringatan
	 * yang menyebutkan nama kegiatan konflik, sehingga baris-baris lain yang belum
	 * sempat diproses pada iterasi tersebut tidak ikut tersimpan. Peserta baru diberi
	 * {@code kode} 5 digit berurutan berdasarkan jumlah peserta yang sudah ada.
	 *
	 * @throws InterruptedException tidak pernah dilempar secara nyata pada implementasi saat ini
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() throws InterruptedException {
		Session session = HibernateUtil.currentSession();
		final Tbmuser tbmuser = Common.getCurrentUser();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				Object checkboxObject = row.getAttribute("checkbox");
				if (!(checkboxObject instanceof MyCheckboxConfig)) {
					checkboxObject = data == null || data.isEmpty() ? null : data.get(0);
				}
				if (!(checkboxObject instanceof MyCheckboxConfig)) {
					continue;
				}
				MyCheckboxConfig checkbox = (MyCheckboxConfig) checkboxObject;
				if (checkbox.isChecked() && !checkbox.isDisabled()) {
					Mahasiswa mahasiswa = (Mahasiswa) checkbox.getAttribute("mahasiswa");

					if (formulirKegiatan.getGrupFormulirKegiatan() != null) {
						FormulirKegiatanPeserta kegiatanLainSatuGrup = ((FormulirKegiatanPeserta) session
								.createCriteria(FormulirKegiatanPeserta.class)
								.createAlias("formulirKegiatan", "formulirKegiatan")
								.add(Restrictions.eq("formulirKegiatan.grupFormulirKegiatan",
										formulirKegiatan.getGrupFormulirKegiatan()))
								.add(Restrictions.or(Restrictions.isNotNull("siswa"),
										Restrictions.or(Restrictions.isNotNull("guru"),
												Restrictions.or(Restrictions.isNotNull("mahasiswa"),
														Restrictions.isNotNull("dosen")))))
								.add(Restrictions.ne("formulirKegiatan", formulirKegiatan))

								.add(Restrictions.eq("mahasiswa", mahasiswa))

								.setMaxResults(1).uniqueResult());
						if (mahasiswa != null && kegiatanLainSatuGrup != null) {
							MyMessageboxConfig.showFormat(
									"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} tidak dapat didaftarkan karena yang bersangkutan telah terdaftar pada kegiatan \"{V3}\". Langkah yang dapat dilakukan: (1) periksa kembali data pendaftaran mahasiswa tersebut; (2) apabila mahasiswa perlu dipindahkan, batalkan terlebih dahulu pendaftaran pada kegiatan sebelumnya; (3) untuk informasi lebih lanjut, mohon menghubungi administrator.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
									mahasiswa.getNim(), mahasiswa.getNama(),
									kegiatanLainSatuGrup.getFormulirKegiatan().getNama());

							return;
						}
					}

					FormulirKegiatanPeserta formulirKegiatanPeserta = (FormulirKegiatanPeserta) session
							.createCriteria(FormulirKegiatanPeserta.class).add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(Restrictions.eq("formulirKegiatan", formulirKegiatan)).setMaxResults(1).uniqueResult();
					if (formulirKegiatanPeserta == null) {
						formulirKegiatanPeserta = new FormulirKegiatanPeserta();
						int count = ((Number) session.createCriteria(FormulirKegiatanPeserta.class)
								.setProjection(Projections.rowCount())
								.add(Restrictions.eq("formulirKegiatan", formulirKegiatan)).uniqueResult()).intValue();
						count++;
						String kode = "0000000000000" + count;
						kode = kode.substring(kode.length() - 5);
						formulirKegiatanPeserta.setKode(kode);
					}

					formulirKegiatanPeserta.setFormulirKegiatan(formulirKegiatan);
					formulirKegiatanPeserta.setOleh(tbmuser.getUserId());
					formulirKegiatanPeserta.setMahasiswa(mahasiswa);
					Common.refreshSaveOrUpdate(session, formulirKegiatanPeserta);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaForFormulirKegiatanHelper.java:215");
				// TODO: handle exception
			}
		}

	}

	/**
	 * Membangun dan menampilkan jendela modal "Ambil Data Mahasiswa": form filter
	 * (NIM/rentang NIM/Nama/Fakultas/Tahun Angkatan/Prodi/Status Mahasiswa), grid
	 * berpaging dengan checkbox "pilih semua" pada header, dan tombol Simpan/Batal.
	 *
	 * @param dataLoader callback pemuatan ulang data pemanggil setelah Simpan
	 * @param window     jendela ZK yang akan diisi dan ditampilkan sebagai modal
	 */
	public void display(final DataLoader dataLoader, final MyWindow window) {

		Common.clear(window);
		window.setTitle("Ambil Data Mahasiswa");
		window.setWidth("90%");
		window.setHeight("90%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("240px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);
		//
		//
		//
		//

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
		Common.insertComboDanSemua(searchstatusmahasiswa, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		row.appendChild(searchstatusmahasiswa);
		searchstatusmahasiswa.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.setTooltiptext("Cari kandidat mahasiswa sesuai filter");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		Borderlayout myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);

		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		South mySouth = new South();
		mySouth.setParent(myBorderlayout1);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(myCenter1);

		paging.setParent(mySouth);

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

						if (myCheckbox.isDisabled()) {
							continue;
						}

						myCheckbox.setChecked(checkbox.isChecked());
						if (!checkbox.isChecked()) {
							continue;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaForFormulirKegiatanHelper.java:377");

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

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Angkatan");
		column.setWidth("25%");

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
				dataLoader.loadData(null);
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

	/**
	 * Membangun kriteria pencarian mahasiswa aktif sesuai seluruh filter form. Filter
	 * status mahasiswa dievaluasi lewat sub-query SQL native ke
	 * {@code history_status_mahasiswa} untuk tahun akademik dan paritas semester
	 * (ganjil/genap) yang sedang berjalan saat ini.
	 *
	 * @param order tambahkan pengurutan (tahun angkatan menurun, lalu NIM menaik) bila {@code true}
	 * @return kriteria Hibernate atas {@link Mahasiswa}
	 */
	public Criteria initCriteria(boolean order) {

		StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatusmahasiswa.getSelectedItem() == null ? null
				: searchstatusmahasiswa.getSelectedItem().getValue());

		Criterion criteriaStatus = Restrictions.sqlRestriction("true");
		if (statusMahasiswa != null) {
			String sql = "this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ statusMahasiswa.getId() + " and tahunakademik = '" + Common.getCurrentTahunAkademik()
					+ "' and semester%2=" + (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
			System.out.println("sql=>" + sql);
			criteriaStatus = Restrictions.sqlRestriction(sql);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (order)
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));

		criteria.add(criteriaStatus).add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
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
				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));

		return criteria;
	}

	/**
	 * Memuat ulang grid mahasiswa sesuai {@link #initCriteria(boolean)}, menggunakan
	 * paging 50 baris/halaman ({@link Common#initPaging50}).
	 *
	 * @param event tidak digunakan; parameter kontrak listener/pemanggilan langsung
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging50(initCriteria(false), paging);

		List<Mahasiswa> mahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_50)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_50 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setEmptyMessage(mahasiswa.isEmpty()
				? "Tidak ada kandidat mahasiswa yang sesuai. Kosongkan Angkatan Mahasiswa atau sesuaikan filter pencarian."
				: "Tidak ada kandidat mahasiswa yang cocok dengan filter.");
		grid.setModelCheckMobile(strset);

	}

}
