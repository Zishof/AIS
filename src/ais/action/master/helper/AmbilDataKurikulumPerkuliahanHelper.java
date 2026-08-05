package ais.action.master.helper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
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
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Jurusan;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PaketPerkuliahan;
import ais.database.model.Program;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataKurikulumPerkuliahanHelper {

	private String tahunAjaran;
	private Integer semester;
	private Mahasiswa mahasiswa;
	private MyGrid grid;

	private Combobox searchfakultas = new Combobox();
	private Combobox jurusanCombobox = new Combobox();
	private Combobox programCombobox = new Combobox();
	private Combobox semesterPerkuliahan;
	private List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs;
	private Integer semesterPendek;
	private PaketPerkuliahan paketPerkuliahan;
	private MyToolbarbuttonConfig ambil;
	private String kelas;

	public AmbilDataKurikulumPerkuliahanHelper(Integer semesterPendek) {
		this.semesterPendek = semesterPendek;
	}

	class MatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row row, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) arg1;
			Matakuliah matakuliah = kurikulumPunyaMatakuliah.getMatakuliah();

			new Label(matakuliah.getKode()).setParent(row);
			new Label(matakuliah.getNama()).setParent(row);
			new Label(matakuliah.getSks() + "").setParent(row);

		}

	}

	@SuppressWarnings({})
	public boolean save() throws Exception {

		Tbmuser tbmuser = Common.getCurrentUser();

		Session sessionDelete = HibernateUtil.currentNativeSession();
		String sql = "delete from detailperkuliahan where mahasiswa = " + mahasiswa.getId()
				+ " and persetujuan = 0 and semester = " + semesterPerkuliahan.getSelectedItem().getValue();
		sessionDelete.createSQLQuery(sql).executeUpdate();

		HibernateUtil.closeSession();

		Set<Long> matakuliahs = new HashSet<Long>();
		String peringatanKapasitasRuangan = "";
		for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
			if (matakuliahs.contains(kurikulumPunyaMatakuliah.getMatakuliah().getId())) {
				continue;
			}
			matakuliahs.add(kurikulumPunyaMatakuliah.getMatakuliah().getId());

			if (!Common.checkMatakuliahPrasyarat(kurikulumPunyaMatakuliah.getMatakuliah(), mahasiswa,
					AmbilDataKurikulumPerkuliahanHelper.this.semester)) {
				continue;
			}

			Session mySession = HibernateUtil.currentNativeSession();
			try {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) (mySession
						.createCriteria(Detailperkuliahan.class).add(Restrictions.isNull("ikutiPerkuliahan"))
						.add(Restrictions.eq("matakuliahKonversi", kurikulumPunyaMatakuliah.getMatakuliah()))
						.add(Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.eq("semester", AmbilDataKurikulumPerkuliahanHelper.this.semester))
						.setMaxResults(1).uniqueResult());
				if (detailperkuliahan == null) {
					detailperkuliahan = new Detailperkuliahan(tbmuser, AmbilDataKurikulumPerkuliahanHelper.class);
					detailperkuliahan.setNilaiHuruf("");
					detailperkuliahan.setTotalNilai(0.0);
					detailperkuliahan.setMahasiswa(mahasiswa);
					detailperkuliahan.setPerkuliahan(null);
					detailperkuliahan.setMatakuliahKonversi(kurikulumPunyaMatakuliah.getMatakuliah());
					detailperkuliahan.setSemester(AmbilDataKurikulumPerkuliahanHelper.this.semester);
					mySession.getTransaction().begin();
					Common.refreshSaveOrUpdate(mySession, detailperkuliahan);
					mySession.getTransaction().commit();
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			HibernateUtil.closeSession();

		}
		if (!peringatanKapasitasRuangan.trim().equals("")) {
			MyMessageboxConfig.show(peringatanKapasitasRuangan, "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
		}
		return true;
	}

	public void display(final Mahasiswa mahasiswa, final String tahunAjaran, final Integer semester,
			final DataLoader dataLoader) throws Exception {
		this.mahasiswa = mahasiswa;
		this.tahunAjaran = tahunAjaran;
		this.semester = semester;
		Session session = HibernateUtil.currentSession();
		kelas = mahasiswa.getKelas();
		kelas = kelas == null ? "" : kelas.trim();

		final MyWindow window = new MyWindow();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setTitle("Ambil Data Perkuliahan Konversi");
		window.setWidth("97%");
		window.setHeight("97%");

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

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, jurusanCombobox);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(searchfakultas, this.mahasiswa.getJurusan().getFakultas());
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		searchfakultas.setDisabled(true);
		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.insertCombo(jurusanCombobox, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", this.mahasiswa.getJurusan().getFakultas()));
		Common.selectComboItem(jurusanCombobox, this.mahasiswa.getJurusan());
		row.appendChild(jurusanCombobox);
		jurusanCombobox.setWidth("90%");
		jurusanCombobox.setDisabled(true);
		jurusanCombobox.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.insertCombo(programCombobox, "namaBaru", Program.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Program programselected = (Program) session.createCriteria(Program.class)
				.add(Restrictions.eq("nama", this.mahasiswa.getProgram())).uniqueResult();
		Common.selectComboItem(programCombobox, programselected);
		row.appendChild(programCombobox);
		programCombobox.setWidth("90%");
		programCombobox.setDisabled(true);
		programCombobox.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterPerkuliahan = new Combobox());

		int maxSemesterPilihan = 25;
		try {
			maxSemesterPilihan = Integer
					.parseInt(Common.getKonfigurasi("max_semester_pilihan", "25").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataKurikulumPerkuliahanHelper.java:245");

		}

		for (int i = 1; i < maxSemesterPilihan; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semesterPerkuliahan.appendChild(comboitem);
		}
		Common.selectComboItem(semesterPerkuliahan, this.semester);
		semesterPerkuliahan.setWidth("90%");
		semesterPerkuliahan.setDisabled(true);
		semesterPerkuliahan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(new ais.ui.util.MyLabelConfig(this.tahunAjaran));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
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
		column.setLabel("Kode MK");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mata Kuliah");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("5%");

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

		ambil = new MyToolbarbuttonConfig("Ambil Perkuliahan Konversi", "/img/save.gif");
		ambil.setTooltiptext("Ambil");
		ambil.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				save();
				dataLoader.loadData(true);
				window.detach();

			}
		});
		ambil.setParent(toolbar);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		onSearchDefault(null);
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) throws Exception {

		Program program = (Program) (programCombobox.getSelectedItem() == null ? null
				: programCombobox.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();

		paketPerkuliahan = (PaketPerkuliahan) session.createCriteria(PaketPerkuliahan.class)

				.add(Restrictions.sqlRestriction(
						semesterPerkuliahan.getSelectedItem().getValue() + " between minsmt and maxsmt"))

				.add(Restrictions.sqlRestriction(mahasiswa.getTahunangkatan() + " between mulai and sampai"))

				.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
						: Restrictions.eq("statusSemesterPendek", semesterPendek))

				.createAlias("kurikulum", "kurikulum").createAlias("kurikulum.program", "program")
				.createAlias("kurikulum.jurusan", "jurusan")

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))
				.add(jurusanCombobox.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("kurikulum.jurusan", jurusanCombobox, false))

				.add(program == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", program.getNama()))

				.add(Restrictions.eq("tahunAkademik", tahunAjaran)).addOrder(Order.desc("angkatanMulai"))
				.addOrder(Order.desc("angkatanSampai")).addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

		if (paketPerkuliahan == null) {
			MyMessageboxConfig.show("Paket perkuliahan tidak ditemukan", "Pemberitahuan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			ambil.setDisabled(true);
			return;
		}

		System.out.println("paketPerkuliahan = " + paketPerkuliahan);

		kurikulumPunyaMatakuliahs = session.createCriteria(KurikulumPunyaMatakuliah.class)
				.add(Restrictions.eq("kurikulum", paketPerkuliahan.getKurikulum()))
				.add(Restrictions.eq("semester", semesterPerkuliahan.getSelectedItem().getValue())).list();

		if (kurikulumPunyaMatakuliahs.size() == 0) {
			MyMessageboxConfig.show(
					"Paket perkuliahan untuk semester " + semesterPerkuliahan.getSelectedItem().getValue()
							+ " tidak ditemukan",
					"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			ambil.setDisabled(true);
			return;
		}

		ListModel strset = new SimpleListModel(kurikulumPunyaMatakuliahs);
		grid.setRowRenderer(new MatakuliahRenderer());
		grid.setModelCheckMobile(strset);

	}
}
