package ais.action.master.helper;

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

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Ruang;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper jendela modal untuk mentransfer mahasiswa peserta satu {@link Perkuliahan} (kelas) ke
 * kelas paralel lain dari matakuliah yang sama — mis. memindahkan mahasiswa dari kelas A ke kelas
 * B karena bentrok jadwal atau penyeimbangan jumlah peserta.
 *
 * <p>
 * Daftar kelas tujuan yang ditawarkan pada combobox {@link #perkuliahanTujuan} dibangun di
 * konstruktor: seluruh {@link Perkuliahan} AKTIF, BUKAN kelas paralel itu sendiri
 * ({@code merupakan_paralel=false/null}), dengan kode matakuliah PERSIS SAMA (exact match) dengan
 * {@code perkuliahan} asal, selain {@code perkuliahan} itu sendiri — diurutkan tahun ajaran/
 * semester/id menurun (kelas terbaru muncul lebih dulu). Bila {@code perkuliahan.getMatakuliah()}
 * bernilai {@code null} (data tidak konsisten), daftar tujuan sengaja dikosongkan alih-alih
 * melempar {@link NullPointerException}.
 * </p>
 *
 * <p>
 * Grid peserta menampilkan mahasiswa dengan {@code Detailperkuliahan.ikutiPerkuliahan == null}
 * (peserta aktif kelas asal) pada {@code perkuliahan} tersebut, disaring NIM/nama/program.
 * Checkbox baris otomatis terkunci (tercentang, tidak bisa diubah) untuk mahasiswa yang statusnya
 * BUKAN {@link ConstantValues#AKTIF}, mencegah transfer mahasiswa yang statusnya tidak aktif.
 * Bila konstruktor dipanggil dengan {@code selectedMahasiswa} tertentu, filter NIM/nama dikunci ke
 * mahasiswa tersebut (field disabled) dan baris mahasiswa itu otomatis tercentang+terkunci —
 * dipakai saat transfer dipicu dari konteks satu mahasiswa spesifik (bukan transfer massal).
 * </p>
 *
 * <p>
 * {@link #save()} menegakkan kapasitas ruangan kelas tujuan ({@link Ruang#getKapasitasRuangan()},
 * atau {@link Ruang#getDefaultKapasitas()} bila tidak diset) sebelum benar-benar memindahkan baris
 * mana pun — bila jumlah peserta kelas tujuan (yang sudah ada + yang akan ditransfer) melebihi
 * kapasitas, seluruh transfer dibatalkan dengan pesan peringatan. Mahasiswa yang KEBETULAN sudah
 * terdaftar di kelas tujuan (duplikat) dilewati per-baris dengan pesan informasi, tanpa
 * menggagalkan transfer mahasiswa lain dalam batch yang sama.
 * </p>
 */
public class TransferDataMahasiswaHelper {

	/** Kelas (perkuliahan) asal yang pesertanya akan ditransfer. */
	private Perkuliahan perkuliahan;
	/** Grid peserta kelas asal, dirender oleh {@link DetailperkuliahanRenderer}. */
	private MyGrid grid;
	/** Kotak filter pencarian: NIM mahasiswa (contains, ILIKE anywhere); dikunci bila {@link #selectedMahasiswa} diisi. */
	private Textbox nimmahasiswa;
	/** Kotak filter pencarian: nama mahasiswa (contains, ILIKE anywhere); dikunci bila {@link #selectedMahasiswa} diisi. */
	private Textbox namamahasiswa;

	/** Combobox pilihan kelas paralel tujuan, diisi di konstruktor (lihat javadoc kelas untuk kriteria kandidatnya). */
	private Combobox perkuliahanTujuan;
	/** Bila tidak {@code null}, membatasi/mengunci tampilan pada satu mahasiswa spesifik (lihat javadoc kelas). */
	private Mahasiswa selectedMahasiswa;
	/** Filter program studi pada pencarian peserta. */
	private Combobox program;

	/** Seperti {@link #TransferDataMahasiswaHelper(Perkuliahan, Mahasiswa)} tanpa mahasiswa terpilih (mode transfer massal, seluruh peserta kelas dapat dipilih). */
	public TransferDataMahasiswaHelper(Perkuliahan perkuliahan) {
		this(perkuliahan, null);
	}

	/**
	 * @param perkuliahan      kelas asal yang pesertanya akan ditransfer
	 * @param selectedMahasiswa bila tidak {@code null}, membatasi/mengunci tampilan pada satu
	 *                          mahasiswa spesifik yang otomatis terpilih (lihat javadoc kelas)
	 */
	@SuppressWarnings("unchecked")
	public TransferDataMahasiswaHelper(Perkuliahan perkuliahan, Mahasiswa selectedMahasiswa) {
		this.perkuliahan = perkuliahan;
		this.selectedMahasiswa = selectedMahasiswa;
		// Guard: perkuliahan.getMatakuliah() bisa null (data tak konsisten) -- daripada NPE
		// langsung di constructor, perlakukan sebagai "tak ada perkuliahan paralel tujuan".
		List<Perkuliahan> perkuliahans = perkuliahan.getMatakuliah() == null ? new java.util.ArrayList<Perkuliahan>()
				: HibernateUtil.currentSession().createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.eq("merupakan_paralel", false),
						Restrictions.isNull("merupakan_paralel")))
				.add(Restrictions.ne("id", perkuliahan.getId())).createAlias("matakuliah", "matakuliah")
				.add(Restrictions.ilike("matakuliah.kode", perkuliahan.getMatakuliah().getKode(), MatchMode.EXACT))
				.addOrder(Order.desc("tahunAjaran")).addOrder(Order.desc("semester")).addOrder(Order.desc("id")).list();
		perkuliahanTujuan = new Combobox();
		for (Perkuliahan o : perkuliahans) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel((o.getDosen1() == null ? "" : o.getDosen1().getNama()) + " - "
					+ (o.getMatakuliah() == null ? "" : o.getMatakuliah().getNama()) + " (" + o.getId() + ")");
			comboitem.setValue(o);

			String deskripsi = "TA: " + o.getTahunAjaran() + ", Smt: "
					+ (o.getSemester() + (o.getKelas() == null || o.getKelas().equals("") ? "" : " " + o.getKelas()))
					+ ", Ruang: " + (o.getRuang() == null ? "" : o.getRuang().getKodeRuangan()) + ", Hari: "
					+ o.getHari() + ", Waktu: " + o.getWaktuMulai() + "-" + o.getWaktuSelesai() + ", Program : "
					+ o.getProgram() + ", Prodi : " + (o.getJurusan() == null ? "" : o.getJurusan().getNama());

			comboitem.setDescription(deskripsi);
			perkuliahanTujuan.appendChild(comboitem);
		}
	}

	/** Perender baris grid: checkbox pemilihan (terkunci untuk {@link #selectedMahasiswa} atau mahasiswa berstatus non-aktif), NIM/nama/tahun angkatan/program mahasiswa, total nilai & status persetujuan pada {@code perkuliahan} asal, dan kelas KRS mahasiswa (disinkronkan lewat {@link Common#singkronkanKrsMahasiswa}). */
	class DetailperkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris peserta kelas asal (lihat javadoc kelas
		 * {@link DetailperkuliahanRenderer} untuk rincian kolom dan aturan checkbox terkunci).
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

			if (selectedMahasiswa != null && mahasiswa.getId().equals(selectedMahasiswa.getId())) {
				checkbox.setChecked(true);
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

			Label labelKelas = new Label("");
			labelKelas.setParent(arg0);

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, detailperkuliahan.getSemester(), null,
					null);
			labelKelas.setValue(krsMahasiswa.getKelas());
		}

	}

	/**
	 * Memvalidasi lalu menjalankan transfer mahasiswa yang tercentang pada grid ke kelas yang
	 * dipilih di {@link #perkuliahanTujuan}. Menggunakan sesi Hibernate native mandiri
	 * ({@link HibernateUtil#currentNativeSession()}), ditutup tuntas di {@code finally} apa pun
	 * hasilnya. Lihat javadoc kelas untuk aturan validasi kapasitas ruangan dan penanganan
	 * duplikat.
	 *
	 * @return {@code true} bila proses (termasuk kemungkinan sebagian gagal karena duplikat)
	 *         selesai dijalankan; {@code false} bila dibatalkan (kelas tujuan belum dipilih,
	 *         kapasitas penuh, atau terjadi galat tak terduga)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean save() throws Exception {

		Perkuliahan selectedPerkuliahan = (Perkuliahan) (perkuliahanTujuan.getSelectedItem() == null ? null
				: perkuliahanTujuan.getSelectedItem().getValue());
		if (selectedPerkuliahan == null) {
			MyMessageboxConfig.show("Jadwal perkuliahan tujuan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			perkuliahanTujuan.focus();
			return false;
		}

		Session session = HibernateUtil.currentNativeSession();
		String peringatan = "";
		try {
			Rows rows = grid.getRows();
			List<Row> list = rows.getChildren();
			Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(session, selectedPerkuliahan, false);
			for (Row row : list) {
				List data = row.getChildren();
				if (data.get(0) instanceof MyCheckboxConfig) {
					try {
						MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
						if (checkbox.isChecked()) {
							jumlahUdahMasuk++;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TransferDataMahasiswaHelper.java:170");
						// TODO: handle exception
					}
				}
			}

			final Ruang ruang = selectedPerkuliahan.getRuang();
			if (ruang != null && (ruang == null ? Ruang.getDefaultKapasitas() : ruang.getKapasitasRuangan()) != null
					&& jumlahUdahMasuk > (ruang == null ? Ruang.getDefaultKapasitas() : ruang.getKapasitasRuangan())) {
				MyMessageboxConfig.show(
						"Kapasitas ruangan " + ruang.getKodeRuangan()
								+ " sudah penuh. Maksimal kapasitas ruangan tesebut adalah "
								+ (ruang == null ? Ruang.getDefaultKapasitas() : ruang.getKapasitasRuangan())
								+ ", sedangkan anda mencoba memasukkan mahasiswa berjumlah " + jumlahUdahMasuk,
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				perkuliahanTujuan.focus();
					return false;
			}

			for (Row row : list) {
				List data = row.getChildren();
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
											+ " sudah terdapat di Jadwal perkuliahan tujuan. Transfer mahasiswa ini gagal dilakukan";
								} else {

									session.refresh(detailperkuliahan);
									detailperkuliahan.setPerkuliahan(selectedPerkuliahan);

									session.getTransaction().begin();
									Common.refreshUpdate(session, detailperkuliahan);
									session.getTransaction().commit();

									Mahasiswa m = detailperkuliahan.getMahasiswa();
									peringatan += "\nMahasiswa dengan NIM " + m.getNim() + " dan nama " + m.getNama()
											+ " berhasil di-transfer";

									m.reInitDetailperkuliahan(session);
								}

							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TransferDataMahasiswaHelper.java:225");
							// TODO: handle exception
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"mentransfer data mahasiswa ke jadwal perkuliahan tujuan",
					e, new String[] {
							"Periksa data mahasiswa mana saja yang sudah berhasil ditransfer sebelum proses ini gagal.",
							"Muat ulang (refresh) halaman ini lalu coba proses transfer kembali.",
							"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
			return false;
		} finally {
			// currentNativeSession() adalah sesi mandiri untuk proses transfer ini.
			// Tutup tuntas walaupun validasi, query, atau commit gagal.
			HibernateUtil.closeSession();
		}
		if (!peringatan.equals("")) {
			MyMessageboxConfig.show(peringatan, "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}
		return true;
	}

	/**
	 * Membangun dan menampilkan jendela modal transfer (form filter + pilihan kelas tujuan, grid
	 * peserta berpaging, tombol Transfer/Batal). Tombol Transfer memanggil {@link #save()}, lalu —
	 * bila berhasil — menyinkronkan ulang ({@code singkronkan}) baik kelas asal maupun kelas tujuan
	 * lewat timer bertahap sebelum menyegarkan {@code dataLoader} dan menutup jendela.
	 */
	@SuppressWarnings("deprecation")
	public void display(final DataLoader dataLoader, final MyWindow window) {
		Common.clear(window);
		window.setTitle(
				"Transfer Data Mahasiswa yang mengikuti perkuliahan " + (perkuliahan.getMatakuliah() == null ? ""
						: perkuliahan.getMatakuliah().getNama() + " semester " + perkuliahan.getSemester() + " kelas "
								+ perkuliahan.getKelas() + " ruang "
								+ (perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama())));
		window.setWidth("90%");
		window.setHeight(selectedMahasiswa == null ? "90%" : "250px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

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

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "1,7");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih pindah ke jadwal perkuliahan :"));
		row.appendChild(perkuliahanTujuan);
		perkuliahanTujuan.setWidth("90%");
		perkuliahanTujuan.setReadonly(true);
		// perkuliahanTujuan.setConstraint("no empty");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(200);
		grid.setParent(center);

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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TransferDataMahasiswaHelper.java:365");

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

		button = new MyToolbarbuttonConfig("Transfer", "/img/corner.gif");
		button.setTooltiptext("Transfer");
		button.setTooltiptext("Transfer");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (!save()) {
					return;
				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Perkuliahan selectedPerkuliahan = (Perkuliahan) (perkuliahanTujuan.getSelectedItem() == null
								? null
								: perkuliahanTujuan.getSelectedItem().getValue());
						if (selectedPerkuliahan == null || perkuliahan == null) {
							return;
						}
						Session session = null;
						try {
							session = HibernateUtil.currentNativeSession();
							selectedPerkuliahan.singkronkan(session);
							perkuliahan.singkronkan(session);
						} finally {
							HibernateUtil.closeSession();
						}

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

	/** Memuat ulang grid dengan peserta aktif {@link #perkuliahan} yang cocok dengan filter NIM/nama/program aktif, terurut NIM. {@code event} tidak dipakai. */
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
