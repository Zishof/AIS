package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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
import org.zkoss.zul.West;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK untuk menyalin ("copy") peserta {@link Detailperkuliahan} dari satu {@link
 * Perkuliahan} sumber ke satu atau beberapa jadwal perkuliahan paralel tujuan (kelas lain,
 * ruang/dosen berbeda, tetapi matakuliah/tahun ajaran/semester/jurusan sama, bukan kelas
 * paralel). Dialog terbagi dua panel: panel barat berisi daftar jadwal tujuan (checkbox per
 * jadwal), panel utama berisi daftar mahasiswa sumber (dengan filter NIM/nama/program) yang
 * belum mengikuti perkuliahan lain ({@code ikutiPerkuliahan} null) — setiap baris menampilkan
 * status persetujuan, total nilai, dan status mahasiswa saat ini (checkbox otomatis
 * dinonaktifkan bila mahasiswa tidak berstatus aktif).
 *
 * <p>
 * {@link #save()} melakukan penyalinan sesungguhnya: untuk tiap kombinasi (mahasiswa tercentang
 * &times; jadwal tujuan tercentang), bila mahasiswa tersebut belum terdaftar di jadwal tujuan,
 * baris {@link Detailperkuliahan} sumber di-clone (id dan feeder dikosongkan) lalu disimpan
 * sebagai baris baru pada jadwal tujuan — bila sudah terdaftar, penyalinan untuk kombinasi itu
 * dilewati dan dicatat sebagai peringatan yang ditampilkan setelah proses selesai. Tombol "Copy
 * data mahasiswa" pada UI memanggil {@link #save()} lalu menyinkronkan ulang (
 * {@code singkronkan}) baik jadwal tujuan maupun jadwal sumber.
 * </p>
 */
public class CopyDataMahasiswaHelper {

	/** Jadwal perkuliahan sumber tempat mahasiswa disalin dari. */
	private Perkuliahan perkuliahan;
	/** Grid mahasiswa sumber, dirender oleh {@link DetailperkuliahanRenderer}. */
	private MyGrid grid;
	/** Kotak filter pencarian: NIM mahasiswa (contains, ILIKE anywhere); dikunci bila {@link #selectedMahasiswa} diisi. */
	private Textbox nimmahasiswa;
	/** Kotak filter pencarian: nama mahasiswa (contains, ILIKE anywhere); dikunci bila {@link #selectedMahasiswa} diisi. */
	private Textbox namamahasiswa;

	/** Bila diisi, dialog dibatasi hanya untuk menyalin satu mahasiswa ini (lihat javadoc konstruktor). */
	private Mahasiswa selectedMahasiswa;
	/** Filter program studi pada pencarian mahasiswa sumber. */
	private Combobox program;
	/** Grid panel barat berisi daftar jadwal perkuliahan paralel tujuan beserta checkbox pemilihannya. */
	private MyGrid searchgridJadwal;

	/** Seperti {@link #CopyDataMahasiswaHelper(Perkuliahan, Mahasiswa)} tanpa membatasi ke satu mahasiswa tertentu (menampilkan seluruh mahasiswa sumber yang bisa disalin). */
	public CopyDataMahasiswaHelper(Perkuliahan perkuliahan) {
		this(perkuliahan, null);
	}

	/**
	 * @param perkuliahan       jadwal perkuliahan sumber tempat mahasiswa disalin dari
	 * @param selectedMahasiswa bila diisi, dialog dibatasi hanya untuk menyalin satu mahasiswa ini (field pencarian dinonaktifkan, checkbox mahasiswa ini sendiri juga dinonaktifkan karena sudah pasti terpilih)
	 */
	public CopyDataMahasiswaHelper(Perkuliahan perkuliahan, Mahasiswa selectedMahasiswa) {
		this.perkuliahan = perkuliahan;
		this.selectedMahasiswa = selectedMahasiswa;

	}

	/**
	 * Renderer baris mahasiswa sumber: checkbox seleksi (tercentang default, dinonaktifkan bila
	 * mahasiswa tidak berstatus aktif atau merupakan {@link #selectedMahasiswa} yang sudah pasti
	 * disalin), NIM/nama/angkatan/program, total nilai di jadwal sumber, status persetujuan, dan
	 * label kelas hasil sinkronisasi KRS (diisi tertunda via {@link Common#createDefaultTimer}).
	 */
	class DetailperkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris mahasiswa sumber (lihat javadoc kelas {@link DetailperkuliahanRenderer}
		 * untuk rincian kolom dan aturan checkbox terkunci).
		 *
		 * @param arg0 baris grid yang diisi
		 * @param arg1 instance {@link Detailperkuliahan} untuk baris ini
		 * @throws Exception diteruskan dari kegagalan pembangunan UI atau akses data
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Detailperkuliahan detailperkuliahan = (Detailperkuliahan) arg1;
			if (detailperkuliahan == null) {
				return;
			}
			arg0.setAttribute("value", detailperkuliahan);
			final Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);
			checkbox.setChecked(true);

			if (selectedMahasiswa != null && mahasiswa.getId().equals(selectedMahasiswa.getId())) {
				checkbox.setDisabled(true);
			}

			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(detailperkuliahan.getMahasiswa(),
					detailperkuliahan.getTahunAkademik(), detailperkuliahan.getSemester()).getStatusMahasiswa();

			checkbox.setDisabled(!statusMahasiswa.getId().equals(ConstantValues.AKTIF.getId()));

			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);
			new Label(mahasiswa.getProgram()).setParent(arg0);

			ais.ui.util.NilaiHurufAnalisisPopupHelper.buatLabel(detailperkuliahan.getTotalNilai() == null ? "0.0 (Belum dinilai)"
					: Common.numberFormat.get().format(detailperkuliahan.getTotalNilai()) + " ("
							+ (detailperkuliahan.getNilaiHuruf() == null
									|| detailperkuliahan.getNilaiHuruf().trim().equals("") ? "Belum dinilai"
											: detailperkuliahan.getNilaiHuruf())
							+ ")", detailperkuliahan)
					.setParent(arg0);

			Label label;
			(label = new Label(detailperkuliahan.getPersetujuan() == null
					|| detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.BELUM_DISETUJUI) ? "Tidak" : "Ya"))
					.setParent(arg0);
			label.setStyle(label.getValue().equals("Tidak") ? "color:red;" : "color:blue");

			final Label labelKelas = new Label("");
			labelKelas.setParent(arg0);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
					labelKelas.setValue(krsMahasiswa.getKelas());

				}
			});
		}

	}

	/**
	 * Menyalin setiap mahasiswa yang tercentang di grid utama ke setiap jadwal perkuliahan yang
	 * tercentang di panel jadwal tujuan. Melewati (dan mencatat sebagai peringatan yang
	 * ditampilkan di akhir) kombinasi yang mahasiswanya sudah terdaftar di jadwal tujuan. Setiap
	 * penyalinan dijalankan dalam transaksi Hibernate tersendiri per baris.
	 *
	 * @throws Exception bila tidak ada jadwal tujuan yang dipilih (ditangani dengan pesan peringatan, bukan exception dilempar keluar)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() throws Exception {
		Rows rows = searchgridJadwal.getRows();
		List<Row> list = rows.getChildren();
		List<Perkuliahan> perkuliahans = new ArrayList<Perkuliahan>();
		for (Row row : list) {
			List data = row.getChildren();
			if (data.get(0) instanceof Checkbox) {
				Checkbox checkbox = (Checkbox) data.get(0);
				if (checkbox.isChecked() && checkbox.getAttribute("perkuliahan") != null) {
					perkuliahans.add((Perkuliahan) checkbox.getAttribute("perkuliahan"));
				}
			}
		}

		if (perkuliahans.isEmpty()) {
			MyMessageboxConfig.show("Jadwal perkuliahan harus dipilih minimal satu", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);

			return;
		}

		String peringatan = "";
		for (Perkuliahan selectedPerkuliahan : perkuliahans) {
			rows = grid.getRows();
			list = rows.getChildren();

			for (Row row : list) {
				List data = row.getChildren();
				Session session = HibernateUtil.currentNativeSession();
				try {
					if (data.get(0) instanceof MyCheckboxConfig) {
						try {
							MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
							if (checkbox.isChecked()) {
								Detailperkuliahan detailperkuliahan = (Detailperkuliahan) row.getAttribute("value");

								Detailperkuliahan detailperkuliahanTujuan = (Detailperkuliahan) (session
										.createCriteria(Detailperkuliahan.class)
										.add(Restrictions.isNull("ikutiPerkuliahan"))
										.add(Restrictions.eq("mahasiswa", detailperkuliahan.getMahasiswa()))
										.add(Restrictions.eq("perkuliahan", selectedPerkuliahan)).uniqueResult());

								if (detailperkuliahanTujuan != null) {
									Mahasiswa m = detailperkuliahanTujuan.getMahasiswa();
									peringatan += "\nMahasiswa dengan NIM " + m.getNim() + " dan nama " + m.getNama()
											+ " sudah terdapat di Jadwal perkuliahan "
											+ selectedPerkuliahan.infoSimple() + ". Copy mahasiswa ini gagal dilakukan";
								} else {
									Detailperkuliahan detailperkuliahanCopy = (Detailperkuliahan) detailperkuliahan
											.clone();
									detailperkuliahanCopy.setPerkuliahan(selectedPerkuliahan);
									detailperkuliahanCopy.setFeeder(null);
									detailperkuliahanCopy.setId(null);
									session.getTransaction().begin();
									session.save(detailperkuliahanCopy);
									session.getTransaction().commit();
								}

							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CopyDataMahasiswaHelper.java:191");
							// TODO: handle exception
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				HibernateUtil.closeSession();
			}
		}

		Session session = HibernateUtil.currentNativeSession();
		for (Perkuliahan selectedPerkuliahan : perkuliahans) {
			selectedPerkuliahan.reInitDetailperkuliahan(session);
			selectedPerkuliahan.reInitPerkuliahanDosen();
		}
		HibernateUtil.closeSession();

		if (!peringatan.equals("")) {
			MyMessageboxConfig.show(peringatan, "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}
	}

	/**
	 * Titik masuk utama: membangun dialog dua panel (jadwal tujuan di barat, pencarian+grid
	 * mahasiswa sumber di tengah) di dalam {@code window}, lengkap dengan tombol Batal dan
	 * "Copy data mahasiswa" yang memanggil {@link #save()} lalu menyinkronkan ulang jadwal
	 * sumber/tujuan sebelum memuat ulang data pemanggil lewat {@code dataLoader}.
	 *
	 * @param dataLoader dipanggil untuk memuat ulang tampilan pemanggil setelah penyalinan selesai
	 * @param window     window pembungkus dialog (dibersihkan dan diisi ulang)
	 */
	@SuppressWarnings({ "unchecked" })
	public void display(final DataLoader dataLoader, final MyWindow window) {
		Common.clear(window);
		window.setTitle("Copy Data Mahasiswa yang mengikuti perkuliahan " + (perkuliahan.getMatakuliah() == null ? ""
				: perkuliahan.getMatakuliah().getNama() + " semester " + perkuliahan.getSemester() + " kelas "
						+ perkuliahan.getKelas() + " ruang "
						+ (perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama())));
		window.setWidth("90%");
		window.setHeight(selectedMahasiswa == null ? "90%" : "250px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		West west = new West();
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("50%");

		searchgridJadwal = new MyGrid();
		searchgridJadwal.setWidth("100%");
		searchgridJadwal.setParent(west);

		List<Perkuliahan> perkuliahans = HibernateUtil.currentSession().createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.eq("merupakan_paralel", false),
						Restrictions.isNull("merupakan_paralel")))
				.add(Restrictions.ne("id", perkuliahan.getId())).add(Restrictions.eq("kelas", perkuliahan.getKelas()))
				.add(Restrictions.eq("tahunAjaran", perkuliahan.getTahunAjaran()))
				.add(Restrictions.eq("semester", perkuliahan.getSemester()))
				.add(Restrictions.eq("jurusan", perkuliahan.getJurusan()))

				.addOrder(Order.desc("id")).list();

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		final MyCheckboxConfig checkboxAll = new MyCheckboxConfig("Jadwal");
		column.appendChild(checkboxAll);
		checkboxAll.addEventListener(Events.ON_CHECK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {
					try {
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkboxAll.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CopyDataMahasiswaHelper.java:263");

					}
				}
			}
		});

		ListModel strset = new SimpleListModel(perkuliahans);
		searchgridJadwal.setRowRenderer(new ais.ui.util.MyRowRenderer() {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				Perkuliahan perkuliahan = (Perkuliahan) arg1;
				final Checkbox checkbox = new Checkbox(perkuliahan.infoSimple());
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.setAttribute("perkuliahan", perkuliahan);

			}
		});
		searchgridJadwal.setModelCheckMobile(strset);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(north);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("NIM Mahasiswa"));
		row.appendChild(nimmahasiswa = new Textbox());
		nimmahasiswa.setWidth("90%");

		nimmahasiswa.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Mahasiswa"));
		row.appendChild(namamahasiswa = new Textbox());
		namamahasiswa.setWidth("90%");

		namamahasiswa.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program = new Combobox());
		program.setWidth("90%");
		Common.initPrograms(program);

		program.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (selectedMahasiswa != null) {
			nimmahasiswa.setValue(selectedMahasiswa.getNim());
			nimmahasiswa.setDisabled(true);
			namamahasiswa.setValue(selectedMahasiswa.getNama());
			namamahasiswa.setDisabled(true);
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/search.png");
		button.setVisible(selectedMahasiswa == null);
		button.setTooltiptext("Cari");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});
		button.setParent(row);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(200);
		grid.setParent(center);

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		column.appendChild(checkbox);
		checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {
					try {
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/CopyDataMahasiswaHelper.java:382");

					}
				}
			}
		});

		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Angkatan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Program");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total Nilai");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status Persetujuan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelas " + Common.getBahasa("label_mahasiswa"));

		onSearchDefault(null);

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Copy data mahasiswa", "/img/corner.gif");
		button.setTooltiptext("Copy data mahasiswa");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				save();

				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("rawtypes")
					@Override
					public void onEvent(Event arg0) throws Exception {

						Session session = HibernateUtil.currentNativeSession();

						Rows rows = searchgridJadwal.getRows();
						List<Row> list = rows.getChildren();
						for (Row row : list) {
							List data = row.getChildren();
							if (data.get(0) instanceof Checkbox) {
								Checkbox checkbox = (Checkbox) data.get(0);
								if (checkbox.isChecked() && checkbox.getAttribute("perkuliahan") != null) {
									((Perkuliahan) checkbox.getAttribute("perkuliahan")).singkronkan(session);
								}
							}
						}

						perkuliahan.singkronkan(session);
						HibernateUtil.closeSession();

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								dataLoader.loadData(null);
								window.setVisible(false);
							}
						});

					}
				});
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

	/** Memuat ulang grid mahasiswa sumber sesuai filter NIM/nama/program aktif, dibatasi ke mahasiswa yang belum mengikuti perkuliahan lain ({@code ikutiPerkuliahan} null) pada jadwal sumber ini. */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<Detailperkuliahan> detailperkuliahan = session.createCriteria(Detailperkuliahan.class)

				.add(program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("mahasiswa.program", program.getSelectedItem().getValue()))

				.add(Restrictions.isNull("ikutiPerkuliahan")).createAlias("mahasiswa", "mahasiswa")
				.add(nimmahasiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("mahasiswa.nim", nimmahasiswa.getValue().trim(), MatchMode.ANYWHERE))
				.add(namamahasiswa.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("mahasiswa.nama", namamahasiswa.getValue().trim(), MatchMode.ANYWHERE))

				.addOrder(Order.asc("mahasiswa.nim")).add(Restrictions.eq("perkuliahan", perkuliahan)).list();

		ListModel strset = new SimpleListModel(detailperkuliahan);
		grid.setRowRenderer(new DetailperkuliahanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
