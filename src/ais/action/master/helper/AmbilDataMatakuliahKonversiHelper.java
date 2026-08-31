package ais.action.master.helper;

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
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
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

import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper "pilih dari daftar" untuk mencatat matakuliah konversi (dari kurikulum lain/luar) yang
 * diakui sebagai pengganti matakuliah reguler bagi satu {@link Mahasiswa} pada satu semester
 * tertentu, ditulis sebagai baris {@link Detailperkuliahan} baru dengan
 * {@code matakuliahKonversi} terisi (dan {@code ikutiPerkuliahan} kosong — bukan mengikuti kelas
 * perkuliahan reguler). Menampilkan jendela modal pencarian matakuliah dari {@link Kurikulum}
 * tertentu (kurikulum wajib dipilih sebelum pencarian dapat dijalankan) berdasarkan kode/nama,
 * dengan checkbox per baris.
 *
 * <p>
 * Kecuali konfigurasi {@code boleh_ambil_matakuliah_konversi_lebih_dari_satu_kali} aktif,
 * matakuliah yang sudah pernah dikonversi oleh mahasiswa yang sama pada semester yang sama
 * ditampilkan tercentang sekaligus dinonaktifkan (mencegah duplikasi); {@link #save()} akan
 * memperbarui baris {@link Detailperkuliahan} yang sudah ada (bukan membuat baru) untuk kasus ini.
 * Fakultas dan prodi pencarian dikunci ke fakultas/prodi milik mahasiswa; dropdown kurikulum
 * otomatis terisi dan mengambil kurikulum terbaru milik prodi tersebut sebagai default.
 * </p>
 */
public class AmbilDataMatakuliahKonversiHelper {

	private Mahasiswa mahasiswa;
	private Integer semester;
	private MyGrid grid;

	private Textbox kodeMk;
	private Textbox namaMk;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox searchkurikulum = new Combobox();
	private Combobox searchsemester = new Combobox();

	private Boolean bolehAmbilMatakuliahLebihDariSatuKali = false;

	/** Membuat helper dan membaca konfigurasi {@code boleh_ambil_matakuliah_konversi_lebih_dari_satu_kali}, serta menginisialisasi combobox fakultas/jurusan. */
	public AmbilDataMatakuliahKonversiHelper() {

		bolehAmbilMatakuliahLebihDariSatuKali = Common.bolehKonfigurasi("boleh_ambil_matakuliah_konversi_lebih_dari_satu_kali", Konfigurasi.TIDAK_AKTIF);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
	}

	/** Perender baris grid: checkbox pemilihan (tercentang+nonaktif bila mahasiswa sudah punya konversi matakuliah ini pada semester yang sama dan pengambilan ganda tidak diizinkan) plus label kode+id, nama, SKS, semester kurikulum, status, jenis, fakultas, dan jurusan matakuliah. */
	class MatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) arg1;
			final Matakuliah matakuliah = kurikulumPunyaMatakuliah.getMatakuliah();
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setAttribute("value", matakuliah);
			arg0.setAttribute("value", matakuliah);
			checkbox.setAttribute("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah);
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// if (apakahMelebihiSks()) {
					// checkbox.setChecked(false);
					// }
				}
			});

			if (!bolehAmbilMatakuliahLebihDariSatuKali) {
				Session session = HibernateUtil.currentSession();
				Integer jml = ((Number) session.createCriteria(Detailperkuliahan.class)
						.setProjection(Projections.rowCount()).add(Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.eq("semester", semester))
						.add(Restrictions.eq("matakuliahKonversi", matakuliah)).setMaxResults(1).uniqueResult())
						.intValue();

				checkbox.setChecked(!jml.equals(0));
				checkbox.setDisabled(!jml.equals(0));
			}

			new Label(matakuliah.getKode() + " (" + matakuliah.getId() + ") ").setParent(arg0);
			new Label(matakuliah.getNama()).setParent(arg0);
			new Label(matakuliah.getSks() + "").setParent(arg0);
			new Label(kurikulumPunyaMatakuliah.getSemester() + "").setParent(arg0);
			new Label(matakuliah.getStatus() == null ? "" : matakuliah.getStatus()).setParent(arg0);
			new Label(matakuliah.getJenisMatakuliah() == null ? "" : matakuliah.getJenisMatakuliah()).setParent(arg0);
			new Label(matakuliah.getJurusan() == null || matakuliah.getJurusan().getFakultas() == null ? ""
					: matakuliah.getJurusan().getFakultas().getNama()).setParent(arg0);
			new Label(matakuliah.getJurusan() == null ? "" : matakuliah.getJurusan().getNama()).setParent(arg0);

		}

	}

	/**
	 * Untuk setiap baris grid yang tercentang, membuat (atau, bila belum boleh ambil ganda dan baris
	 * sudah ada, memperbarui) satu {@link Detailperkuliahan} dengan {@code matakuliahKonversi} dan
	 * {@code semester} sesuai matakuliah yang dipilih. Menolak menyimpan (dengan pesan peringatan)
	 * bila kurikulum belum dipilih. Kegagalan per baris ditangkap dan dicatat.
	 */
	@SuppressWarnings({ "unchecked" })
	public void save() throws Exception {

		if (searchkurikulum.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, kurikulum belum dipilih. Langkah yang dapat dilakukan: (1) pilih kurikulum dari daftar yang tersedia; (2) pastikan data kurikulum sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {

			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");

				if (checkbox.isChecked()) {

					try {
						KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) checkbox
								.getAttribute("kurikulumPunyaMatakuliah");
						Matakuliah matakuliah = (Matakuliah) checkbox.getAttribute("value");

						Session session = HibernateUtil.currentNativeSession();
						Detailperkuliahan detailPerkuliahan = null;
						if (!bolehAmbilMatakuliahLebihDariSatuKali) {
							detailPerkuliahan = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
									.add(Restrictions.isNull("ikutiPerkuliahan"))
									.add(Restrictions.eq("mahasiswa", this.mahasiswa))
									.add(Restrictions.eq("semester", semester))
									.add(Restrictions.eq("matakuliahKonversi", matakuliah)).setMaxResults(1)
									.uniqueResult();

							if (detailPerkuliahan == null) {
								detailPerkuliahan = new Detailperkuliahan(tbmuser,
										AmbilDataMatakuliahKonversiHelper.class);
							}
						} else {
							detailPerkuliahan = new Detailperkuliahan(tbmuser, AmbilDataMatakuliahKonversiHelper.class);
						}

						detailPerkuliahan.setMahasiswa(mahasiswa);
						detailPerkuliahan.setMatakuliahKonversi(matakuliah);
						detailPerkuliahan.setSemester(kurikulumPunyaMatakuliah.getSemester());

						session.getTransaction().begin();
						session.saveOrUpdate(detailPerkuliahan);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AmbilDataMatakuliahKonversiHelper.java:177");
					}

					HibernateUtil.closeSession();
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AmbilDataMatakuliahKonversiHelper.java:183");
			}
		}
	}

	/**
	 * Membangun dan menampilkan jendela modal pemilihan matakuliah konversi untuk {@code mahasiswa}
	 * pada {@code semester} yang diberikan: form pencarian (fakultas/prodi dikunci ke milik
	 * mahasiswa, kode, nama, kurikulum — wajib dipilih, otomatis terisi kurikulum terbaru prodi
	 * — dan semester), grid ber-paging client-side dengan checkbox per baris, dan tombol
	 * Cari/Simpan/Batal.
	 *
	 * @param mahasiswa   mahasiswa yang akan diberi matakuliah konversi
	 * @param semester    semester default pencarian dan nilai yang dipakai bila kurikulum tidak
	 *                    memberi semester spesifik per matakuliah
	 * @param tahunAjaran tidak dipakai langsung di badan method; ada untuk kompatibilitas signature
	 *                    pemanggil
	 * @param dataLoader  callback penyegar tampilan pemanggil setelah simpan
	 * @param window      jendela modal yang akan dibangun isinya (dibersihkan lebih dulu)
	 */
	public void display(final Mahasiswa mahasiswa, final Integer semester, final String tahunAjaran,
			final DataLoader dataLoader, final MyWindow window) throws Exception {

		this.mahasiswa = mahasiswa;
		this.semester = semester;
		Common.clear(window);
		window.setTitle("Ambil Data Matakuliah Konversi");
		window.setWidth("90%");
		window.setHeight("95%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		/*
		 * Sebelumnya North memakai setFlex(true) TANPA tinggi → wilayah North menciut
		 * sehingga hanya baris filter pertama (Fakultas | Kode) yang tampak; baris di
		 * bawahnya (Prodi | Nama, dan Kurikulum | Semester) TERPOTONG. Akibatnya field
		 * Kurikulum yang WAJIB dipilih (lihat onSearchDefault/save: "Kurikulum harus diisi")
		 * tak bisa diisi sehingga pencarian selalu gagal. Perbaikan: ikuti pola popup
		 * AmbilData lain — flex=false + tinggi eksplisit + autoscroll. Tinggi dilebihkan
		 * (190px) agar muat 3 baris filter + tombol Cari; autoscroll sebagai pengaman di
		 * layar sempit/mobile.
		 */
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("190px");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		Common.selectComboItem(searchfakultas, mahasiswa.getJurusan().getFakultas());
		searchfakultas.setReadonly(true);


		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
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
		searchjurusan.setReadonly(true);

		Common.insertCombo(searchjurusan, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", mahasiswa.getJurusan().getFakultas()));
		Common.selectComboItem(searchjurusan, mahasiswa.getJurusan());


		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(namaMk = new Textbox());
		namaMk.setWidth("90%");
		namaMk.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kurikulum"));
		row.appendChild(searchkurikulum);
		searchkurikulum.setWidth("90%");
		searchkurikulum.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});
		searchkurikulum.setReadonly(true);

		class KurikulumEventListener implements EventListener {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(searchkurikulum);
				searchkurikulum.setSelectedItem(null);
				if (searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null) {
					return;
				}

				Jurusan myJurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
						|| searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? null
								: searchjurusan.getSelectedItem().getValue());

				List<Kurikulum> kurikulums = HibernateUtil.currentSession().createCriteria(Kurikulum.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.desc("tahun")).add(Restrictions.eq("jurusan", myJurusan)).list();

				for (Kurikulum kurikulum : kurikulums) {
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setLabel(kurikulum.getId() + "-" + kurikulum.getNama());
					comboitem.setValue(kurikulum);
					comboitem.setDescription(kurikulum.getNamaAsli() + " " + kurikulum.getTahun() + " "
							+ kurikulum.getTahunAkademik() + " " + kurikulum.getJenisSemester());
					searchkurikulum.appendChild(comboitem);
				}

				if (myJurusan != null) {
					Kurikulum mykurikulum = (Kurikulum) HibernateUtil.currentSession().createCriteria(Kurikulum.class)
							.addOrder(Order.desc("tahun")).add(Restrictions.eq("jurusan", myJurusan)).setMaxResults(1)
							.uniqueResult();
					Common.selectComboItem(searchkurikulum, mykurikulum);
				}

			}

		}

		KurikulumEventListener kurikulumEventListener = new KurikulumEventListener();

		searchjurusan.addEventListener("onChange", kurikulumEventListener);


		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		Common.clear(searchsemester);
		for (int i = 0; i < 30; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			comboitem.setDescription("Semester " + i);
			searchsemester.appendChild(comboitem);
		}
		row.appendChild(searchsemester);
		searchsemester.setReadonly(true);
		searchsemester.setWidth("90%");
		searchsemester.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		comboitem.setDescription("Semua Semester");
		searchsemester.appendChild(comboitem);

		Common.selectComboItem(searchsemester, mahasiswa.currentSemester());

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
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

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(100);
		grid.setParent(myCenter1);

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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMatakuliahKonversiHelper.java:413");

					}
				}
			}
		});
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Semester");
		column.setWidth("10%");

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

		kurikulumEventListener.onEvent(null);
		onSearchDefault(null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				save();
				dataLoader.loadData(null);
				window.detach();
			}
		});
		button.setParent(toolbar);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun kriteria pencarian {@link KurikulumPunyaMatakuliah} sesuai kurikulum, semester
	 * (bila diisi), kode/nama matakuliah (ilike), dan fakultas/jurusan — matakuliah dengan
	 * {@code milikUniversitas = true} selalu lolos filter fakultas/jurusan.
	 *
	 * @param order bila {@code true}, tambahkan pengurutan menaik berdasarkan semester lalu nama matakuliah
	 * @return kriteria Hibernate siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KurikulumPunyaMatakuliah.class);
		criteria.add(searchkurikulum.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("kurikulum", searchkurikulum.getSelectedItem().getValue()))

				.add(semester == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("semester", semester))

				.createAlias("matakuliah", "matakuliah")
				.createAlias("matakuliah.jurusan", "jurusan", Criteria.LEFT_JOIN)
				.add(namaMk.getValue().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("matakuliah.nama", namaMk.getValue(), MatchMode.ANYWHERE))
				.add(kodeMk.getValue().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("matakuliah.kode", kodeMk.getValue(), MatchMode.ANYWHERE))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false),
										Restrictions.eq("matakuliah.milikUniversitas", true)))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										CommonSearchFilterHelper.eqSelectedWithId("matakuliah.jurusan", searchjurusan, false),
										Restrictions.eq("matakuliah.milikUniversitas", true)));

		if (order) {
			criteria.addOrder(Order.asc("semester")).addOrder(Order.asc("matakuliah.nama"));
		}

		return criteria;
	}

	/**
	 * Menjalankan pencarian {@link KurikulumPunyaMatakuliah} sesuai filter saat ini (menolak dengan
	 * pesan peringatan bila kurikulum belum dipilih), lalu memuat ulang grid dengan hasilnya secara
	 * asinkron (dibungkus {@link Common#createDefaultTimer} dengan indikator busy) agar UI tetap
	 * responsif selama query berjalan.
	 *
	 * @param event event pemicu (tombol Cari, atau perubahan salah satu filter), tidak dipakai langsung
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) throws Exception {
		try {
			semester = (Integer) (searchsemester.getSelectedItem() == null ? null
					: searchsemester.getSelectedItem().getValue());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMatakuliahKonversiHelper.java:540");

		}

		if (searchkurikulum.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, kurikulum belum dipilih. Langkah yang dapat dilakukan: (1) pilih kurikulum dari daftar yang tersedia; (2) pastikan data kurikulum sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		Clients.showBusy("Loading kurikulum..");
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				List<KurikulumPunyaMatakuliah> matakuliah = initCriteria(true).list();

				ListModel strset = new SimpleListModel(matakuliah);
				grid.setRowRenderer(new MatakuliahRenderer());
				grid.setModelCheckMobile(strset);
				Clients.clearBusy();
			}
		});

	}

}
