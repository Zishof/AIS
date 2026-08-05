package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.zkoss.zul.Vbox;

import ais.action.master.MatakuliahPrasyaratAction;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.PerkuliahanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Kurikulum;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Program;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataPerkuliahanNonPaketHelper {

	private String tahunAjaran;
	private Integer semester;
	private Mahasiswa mahasiswa;
	private MyGrid grid;

	private Textbox kodeMk;
	private Textbox namaMk;
	private Combobox searchfakultas = new Combobox();
	private Combobox jurusanCombobox = new Combobox();
	private Combobox programCombobox = new Combobox();
	private Combobox semesterPerkuliahan;
	private Label sksYangdiambil;
	private Integer semesterPendek;
	private Paging paging;
	private Integer tahapan;

	public AmbilDataPerkuliahanNonPaketHelper(Integer semesterPendek) {
		this.semesterPendek = semesterPendek;

		paging = new Paging();
		Common.initPaging15(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class MatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		private PerkuliahanDao perkuliahanDao = DaoFactory.getInstance().getPerkuliahanDao();

		private Session session = perkuliahanDao.getCurrentSession();

		private Set<Long> longs = new HashSet<Long>();
		private List<Perkuliahan> perkuliahans = new ArrayList<Perkuliahan>();

		@Override
		public void render(Row row, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Perkuliahan perkuliahan = (Perkuliahan) arg1;

			final Matakuliah matakuliah = perkuliahan.getMatakuliah();
			if (perkuliahan == null || matakuliah == null) {
				row.setVisible(false);
				return;
			}
			boolean kirikulumBolehAmbil = (perkuliahan != null && perkuliahan.getKurikulum() != null
					&& perkuliahan.getKurikulum().bolehAmbil(mahasiswa));

			String genapOrGanjil = semester.intValue() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
			Integer jmlMk = perkuliahan.getMerupakanRemedial() ? 0
					: ((Number) session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.isNull("ikutiPerkuliahan")).setProjection(Projections.rowCount())

							.add(Restrictions.sqlRestriction("(this_.semester % 2) = (CASE '" + genapOrGanjil
									+ "' when '" + Perkuliahan.GENAP + "' then 0 else 1 end)"))

							.add(Restrictions.eq("mahasiswa", mahasiswa))
							// .add(Restrictions.ne("perkuliahan", perkuliahan))
							.createCriteria("perkuliahan", Criteria.LEFT_JOIN)

							.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
									: Restrictions.eq("statusSemesterPendek", semesterPendek))

							.add(Restrictions.eq("tahunAjaran", tahunAjaran))
							.add(Restrictions.eq("matakuliah", perkuliahan.getMatakuliah())).uniqueResult()).intValue();

			row.setValign("top");
			row.setAttribute("myValue", perkuliahan);
			final Checkbox checkbox = new Checkbox(
					perkuliahan.getMatakuliah().getKode() + " - " + perkuliahan.getMatakuliah().getNama()
							+ (perkuliahan.getMerupakan_paralel() != null && perkuliahan.getMerupakan_paralel()
									? " (Paralel) "
									: ""));
			checkbox.setVisible(jmlMk.equals(0));

			boolean tampil = jmlMk.equals(0) || !kirikulumBolehAmbil;

			checkbox.setVisible(tampil);
			if (!tampil) {
				new Label(perkuliahan.getMatakuliah().getKode() + " - " + perkuliahan.getMatakuliah().getNama()
						+ (perkuliahan.getMerupakan_paralel() != null && perkuliahan.getMerupakan_paralel()
								? " (Paralel) "
								: ""))
						.setParent(row);
			} else {
				checkbox.setParent(row);
			}

			row.setValign("top");
			row.setAttribute("checkbox", checkbox);

			final Integer jml = ((Number) session.createCriteria(Detailperkuliahan.class)
					.add(Restrictions.isNull("ikutiPerkuliahan")).setProjection(Projections.rowCount())

					.add(Restrictions.sqlRestriction("(this_.semester % 2) = (CASE '" + genapOrGanjil + "' when '"
							+ Perkuliahan.GENAP + "' then 0 else 1 end)"))

					.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("perkuliahan", perkuliahan))
					.createCriteria("perkuliahan", Criteria.LEFT_JOIN)

					.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
							: Restrictions.eq("statusSemesterPendek", semesterPendek))

					.add(Restrictions.eq("tahunAjaran", tahunAjaran)).uniqueResult()).intValue();

			checkbox.setChecked(!jml.equals(0) || !kirikulumBolehAmbil);
			// cek ruang

			Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(session, perkuliahan, false);
			if (!checkbox.isChecked()) {
				if (jumlahUdahMasuk < (perkuliahan.getKapasitasKelas() == null ? Ruang.getDefaultKapasitas()
						: perkuliahan.getKapasitasKelas())) {
					checkbox.setDisabled(false);
				} else {
					checkbox.setDisabled(true);
				}
			}

			checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (!Common.checkMatakuliahPrasyarat(perkuliahan.getMatakuliah(), mahasiswa, semester)) {
						checkbox.setChecked(false);
						return;
					}
					if (!checkbox.isChecked() && !jml.equals(0)) {
						MyMessageboxConfig.show(
								"Anda tidak bisa meng-uncheck matakuliah yang telah anda pilih sebelumnya. Solusinya anda harus menghapus terlibih dahulu matakuliah yang belum disetujui di papan pengambilan "
										+ Common.getBahasa("label_krs"),
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						checkbox.setChecked(true);
						return;
					}
					updateStatus(checkbox, matakuliah);
				}
			});

			Vbox vbox = new Vbox();
			vbox.setParent(row);

			new Label(perkuliahan.getMatakuliah().getSks() + " SKS").setParent(vbox);

			MatakuliahPrasyaratAction.tampilPrasyarat(vbox, perkuliahan.getMatakuliah());
			Kurikulum kurikulum = perkuliahan.getKurikulum();
			new Label(kurikulum == null ? "" : "Kurikulum : " + kurikulum.getNama()).setParent(vbox);

			Label label;
			label = new Label(
					!jml.equals(0) ? "Terpilih"
							: !jmlMk.equals(0)
									? "Anda telah mengambil matkul ini tapi mungkin di jadwal dan kelas yang berbeda"
									: (jumlahUdahMasuk < (perkuliahan.getKapasitasKelas() == null
											? Ruang.getDefaultKapasitas()
											: perkuliahan.getKapasitasKelas())) ? "Tersedia" : "Penuh");
			if (!kirikulumBolehAmbil) {
				label.setValue("Kurikulum ini Anda tidak boleh ambil");
				label.setStyle("font-weight:bold;color:red");
			} else if (!jml.equals(0)) {
				label.setStyle("font-weight:bold;color:brown");
			} else if (!jmlMk.equals(0)) {
				label.setStyle("font-weight:bold;color:green");
			} else if (jumlahUdahMasuk < (perkuliahan.getKapasitasKelas() == null ? Ruang.getDefaultKapasitas()
					: perkuliahan.getKapasitasKelas())) {
				label.setStyle("font-weight:bold;color:blue");
			} else {
				label.setStyle("font-weight:bold;color:red");
			}

			row.setValign("top");
			row.setAttribute("label_status", label);

			row.setVisible(!label.getValue().equals("Penuh") && !longs.contains(matakuliah.getId())
					&& !Common.checkJamBentrok(perkuliahans, perkuliahan));

			if (row.isVisible()) {
				longs.add(matakuliah.getId());
				perkuliahans.add(perkuliahan);
			}
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void updateStatus(Checkbox checkbox, Matakuliah matakuliah) throws Exception {
		if (apakahMelebihiKetentuan()) {
			checkbox.setChecked(false);
			hitungSksYangTelahDiambil();
			return;
		}

		// Session session = HibernateUtil.currentSession();
		Rows rows = grid.getRows();
		rows = grid.getRows();
		List<Row> list = rows.getChildren();
		Set<Long> longs = new HashSet<Long>();
		// longs.add(matakuliah.getId());
		for (Row row : list) {
			List child = row.getChildren();
			Checkbox c = (Checkbox) child.get(0);
			if (c.isChecked()) {
				Perkuliahan perkuliahan = (Perkuliahan) row.getAttribute("myValue");
				longs.add(perkuliahan.getMatakuliah().getId());
			}
		}
		for (Row row : list) {
			List child = row.getChildren();
			Checkbox c = (Checkbox) child.get(0);

			if (c == checkbox || c.isChecked())
				continue;
			Perkuliahan perkuliahan = (Perkuliahan) row.getAttribute("myValue");
			if (perkuliahan == null) {
				continue;
			}

		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Integer hitungSksYangTelahDiambil() {
		Map<Long, Perkuliahan> map = new java.util.HashMap<Long, Perkuliahan>();
		if (grid != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
			Rows rows = grid.getRows();

			List<Row> list = rows.getChildren();

			for (Row row : list) {
				List child = row.getChildren();
				Checkbox checkbox = (Checkbox) child.get(0);

				if (checkbox.isChecked()) {
					final Perkuliahan perkuliahan = (Perkuliahan) row.getAttribute("myValue");
					map.put(perkuliahan.getMatakuliah().getId(), perkuliahan);
				}
			}
		}

		Integer jumlah = KrsUtilHelper.hitungSksYangTelahDiambil(map, mahasiswa, tahapan, semester, semesterPendek);
		if (this.sksYangdiambil != null) {
			this.sksYangdiambil.setValue(jumlah + " SKS");
		}

		return jumlah;
	}

	@SuppressWarnings({})
	private boolean apakahMelebihiKetentuan() throws Exception {
		return semesterPendek != null && semesterPendek.equals(Perkuliahan.SEMESTER_PENDEK) ? false
				: Common.checkPembatasanSKSBerdasarkanIP(mahasiswa, AmbilDataPerkuliahanNonPaketHelper.this.semester,
						hitungSksYangTelahDiambil(), semesterPendek);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean save() throws Exception {

		if (apakahMelebihiKetentuan()) {
			return false;
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		Rows rows = grid.getRows();
		Session session = HibernateUtil.currentSession();
		rows = grid.getRows();
		List<Row> list = rows.getChildren();

		Set<Long> matakuliahs = new HashSet<Long>();
		String peringatanKapasitasRuangan = "";
		for (Row row : list) {
			List child = row.getChildren();
			try {
				Checkbox checkbox = (Checkbox) child.get(0);

				if (checkbox.isChecked()) {
					final Long id;
					final Perkuliahan perkuliahan = (Perkuliahan) row.getAttribute("myValue");
					if (matakuliahs.contains(perkuliahan.getMatakuliah().getId())) {
						continue;
					}
					matakuliahs.add(perkuliahan.getMatakuliah().getId());

					try {
						id = (Long) (session.createCriteria(Detailperkuliahan.class)
								.add(Restrictions.isNull("ikutiPerkuliahan")).setProjection(Projections.property("id"))
								.add(Restrictions.eq("perkuliahan", perkuliahan))
								.add(Restrictions.eq("mahasiswa", mahasiswa))
								.add(Restrictions.eq("semester", AmbilDataPerkuliahanNonPaketHelper.this.semester))

								.createCriteria("perkuliahan", Criteria.LEFT_JOIN)
								.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
										: Restrictions.eq("statusSemesterPendek", semesterPendek))

								.uniqueResult());
					} catch (Exception e) {
						continue;
					}

					if (!Common.checkMatakuliahPrasyarat(perkuliahan.getMatakuliah(), mahasiswa, semester)) {
						continue;
					}

					Detailperkuliahan detailperkuliahan = new Detailperkuliahan(tbmuser,
							AmbilDataPerkuliahanNonPaketHelper.class);
					if (id != null) {
						detailperkuliahan = (Detailperkuliahan) session.load(Detailperkuliahan.class, id);
					} else {

						Session mySession = HibernateUtil.currentNativeSession();
						Integer jumlahUdahMasuk = ((Number) mySession.createCriteria(Detailperkuliahan.class)
								.add(Restrictions.isNull("ikutiPerkuliahan")).setProjection(Projections.rowCount())
								.add(Restrictions.eq("perkuliahan", perkuliahan))

								.createCriteria("perkuliahan", Criteria.LEFT_JOIN)
								.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
										: Restrictions.eq("statusSemesterPendek", semesterPendek))

								.uniqueResult()).intValue();

						HibernateUtil.closeSession();

						jumlahUdahMasuk++;
						if (jumlahUdahMasuk > (perkuliahan.getKapasitasKelas() == null ? Ruang.getDefaultKapasitas()
								: perkuliahan.getKapasitasKelas())) {
							peringatanKapasitasRuangan += "Kapasitas kelas sudah penuh. Maksimal kapasitas kelas tesebut adalah "
									+ (perkuliahan.getKapasitasKelas() == null ? Ruang.getDefaultKapasitas()
											: perkuliahan.getKapasitasKelas())
									+ ", sedangkan anda mencoba masuk ke perkuliahan ini menjadi berjumlah "
									+ jumlahUdahMasuk + ". Pilihlah jadwal perkuliahan lainnya.\n";
							continue;
						}
					}

					detailperkuliahan.setNilaiHuruf("");
					detailperkuliahan.setTotalNilai(0.0);
					detailperkuliahan.setMahasiswa(mahasiswa);
					detailperkuliahan.setPerkuliahan(perkuliahan);
					detailperkuliahan.setSemester(AmbilDataPerkuliahanNonPaketHelper.this.semester);
					session.saveOrUpdate(detailperkuliahan);

				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AmbilDataPerkuliahanNonPaketHelper.java:409");
			}
		}
		if (!peringatanKapasitasRuangan.trim().equals("")) {
			MyMessageboxConfig.show(peringatanKapasitasRuangan, "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
		}
		return true;
	}

	public void display(final Mahasiswa mahasiswa, final String tahunAjaran, final Integer semester,
			final Integer tahapan, final DataLoader dataLoader) {
		this.mahasiswa = mahasiswa;
		this.tahunAjaran = tahunAjaran;
		this.semester = semester;
		this.tahapan = tahapan;

		Double[] batas = Common.getMinDanMaxIPK(mahasiswa, semester, semesterPendek);
		final Integer maxsks = batas[0].intValue();

		final MyWindow window = new MyWindow();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setTitle("Ambil Data Matakuliah");
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
		north.setHeight("280px");
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

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class,
				Restrictions.eq("aktif", true));

		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(jurusanCombobox);
				jurusanCombobox.setSelectedItem(null);
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(jurusanCombobox, "nama", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}

		}

		searchfakultas.addEventListener("onChange", new SearchFakultasEventListener());

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas" + " Studi"));
		Common.selectComboItem(searchfakultas, this.mahasiswa.getJurusan().getFakultas());
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Matakuliah"));
		row.appendChild(kodeMk = new Textbox());
		kodeMk.setWidth("90%");

		kodeMk.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.insertCombo(jurusanCombobox, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", this.mahasiswa.getJurusan().getFakultas()));
		Common.selectComboItem(jurusanCombobox, this.mahasiswa.getJurusan());
		row.appendChild(jurusanCombobox);
		jurusanCombobox.setWidth("90%");

		jurusanCombobox.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.insertCombo(programCombobox, "namaBaru", Program.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Session session = HibernateUtil.currentSession();
		Program programselected = (Program) session.createCriteria(Program.class)
				.add(Restrictions.eq("nama", this.mahasiswa.getProgram())).uniqueResult();
		Common.selectComboItem(programCombobox, programselected);
		row.appendChild(programCombobox);
		programCombobox.setWidth("90%");

		programCombobox.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Matakuliah"));
		row.appendChild(namaMk = new Textbox());
		namaMk.setWidth("90%");

		namaMk.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterPerkuliahan = new Combobox());

		int maxSemesterPilihan = 25;
		try {
			maxSemesterPilihan = Integer
					.parseInt(Common.getKonfigurasi("max_semester_pilihan", "25").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataPerkuliahanNonPaketHelper.java:589");

		}

		for (int i = 1; i < maxSemesterPilihan; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semesterPerkuliahan.appendChild(comboitem);
		}
		Common.selectComboItem(semesterPerkuliahan, this.semester);
		semesterPerkuliahan.setWidth("90%");

		semesterPerkuliahan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(new ais.ui.util.MyLabelConfig(this.tahunAjaran));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Anda telah mengambil"));
		row.appendChild(sksYangdiambil = new Label(hitungSksYangTelahDiambil() + " SKS"));

		// KrsMahasiswa krsMahasiswa =
		// Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan,
		// semesterPendek);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah maksimal SKS yang boleh anda diambil"));
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get().format(maxsks) + " SKS"));

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
		column.setLabel("Mata Kuliah");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS / Syarat / Kurikulum");
		column.setWidth("35%");

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

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dataLoader.loadData(true);
						window.detach();
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

		onSearchDefault(null);
	}

	public Criteria initCriteria(boolean order) {

		Program program = (Program) (programCombobox.getSelectedItem() == null ? null
				: programCombobox.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("tampilkanSaatPengambilanKrs"),
						Restrictions.eq("tampilkanSaatPengambilanKrs", true)));

		criteria.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
				: Restrictions.eq("statusSemesterPendek", semesterPendek))

				.createAlias("jurusan", "jurusan")
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))
				.add(jurusanCombobox.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusanCombobox, false))

				.add(program == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", program.getNama()))

				.add(semesterPerkuliahan.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("semester", semesterPerkuliahan.getSelectedItem().getValue()))

				.add(Restrictions.eq("tahunAjaran", tahunAjaran))
				.add(Restrictions.or(Restrictions.eq("merupakan_paralel", false),
						Restrictions.isNull("merupakan_paralel")))

				.createAlias("matakuliah", "matakuliah")
				.add(kodeMk.getText().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("matakuliah.kode", kodeMk.getText().trim(), MatchMode.ANYWHERE))
				.add(namaMk.getText().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("matakuliah.nama", namaMk.getText().trim(), MatchMode.ANYWHERE));

		if (order)
			criteria.addOrder(Order.asc("matakuliah.nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging15(initCriteria(false), paging);

		List<Perkuliahan> matakuliah = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_50)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_50 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(matakuliah);

		grid.setRowRenderer(new MatakuliahRenderer());
		grid.setModelCheckMobile(strset);

	}
}
