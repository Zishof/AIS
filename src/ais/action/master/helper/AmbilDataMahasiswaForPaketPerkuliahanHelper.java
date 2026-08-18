package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Panelchildren;
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
import ais.database.dao.DaoFactory;
import ais.database.dao.DetailperkuliahanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.PaketPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Ruang;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataMahasiswaForPaketPerkuliahanHelper {

	private PaketPerkuliahan paketPerkuliahan;
	private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private Textbox nim;
	private Textbox nama;
	private Decimalbox tahunangkatan;
	private Textbox dariNim;
	private Textbox sampaiNim;

	private Combobox searchstatusmahasiswa = new Combobox();
	private List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private Integer semesterPendek;
	private Integer semester;

	public AmbilDataMahasiswaForPaketPerkuliahanHelper(PaketPerkuliahan paketPerkuliahan, Integer semesterPendek) {
		this.paketPerkuliahan = paketPerkuliahan;
		this.semesterPendek = semesterPendek;
		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class,
				Restrictions.eq("aktif", true));

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

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu

		if (paketPerkuliahan.getKurikulum().getJurusan().getFakultas() != null) {
			Common.selectComboItem(searchfakultas, paketPerkuliahan.getKurikulum().getJurusan().getFakultas());
			Common.clear(searchjurusan);
			Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", paketPerkuliahan.getKurikulum().getJurusan().getFakultas()));
			searchfakultas.setDisabled(true);
		} else {
			searchfakultas.setDisabled(false);
			Common.selectComboItem(searchfakultas, paketPerkuliahan.getKurikulum().getJurusan().getFakultas());
		}

		if (paketPerkuliahan.getKurikulum().getJurusan() != null) {
			Common.selectComboItem(searchjurusan, paketPerkuliahan.getKurikulum().getJurusan());
			searchjurusan.setDisabled(true);
		} else {
			searchjurusan.setDisabled(false);
			Common.selectComboItem(searchjurusan, paketPerkuliahan.getKurikulum().getJurusan());
		}

	}

	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		private DetailperkuliahanDao detailperkuliahanDao = DaoFactory.getInstance().getDetailperkuliahanDao();

		private Session session = detailperkuliahanDao.getCurrentSession();

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);
			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();
			checkbox.setDisabled(!statusMahasiswa.getId().equals(ConstantValues.AKTIF.getId()));

			Integer jml = ((Number) session.createCriteria(Detailperkuliahan.class)
					.add(Restrictions.isNull("ikutiPerkuliahan")).setProjection(Projections.rowCount())
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("paketPerkuliahan", paketPerkuliahan)).uniqueResult()).intValue();

			arg0.setVisible(jml.equals(0));
			checkbox.setChecked(!jml.equals(0));

			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);

		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() throws Exception {

		Session session = HibernateUtil.currentSession();
		kurikulumPunyaMatakuliahs = session.createCriteria(KurikulumPunyaMatakuliah.class)
				.add(Restrictions.eq("kurikulum", paketPerkuliahan.getKurikulum()))
				.add(Restrictions.eq("semester", semester)).list();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked() && row.isVisible()) {
					Mahasiswa mahasiswa = (Mahasiswa) checkbox.getAttribute("mahasiswa");
					Boolean hasil = save(mahasiswa);
					if (!hasil) {
						break;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaForPaketPerkuliahanHelper.java:194");
				// TODO: handle exception
			}
		}

	}

	@SuppressWarnings({ "unchecked" })
	public boolean save(Mahasiswa mahasiswa) throws Exception {

		Tbmuser tbmuser = Common.getCurrentUser();

		Session session = HibernateUtil.currentSession();

		String sql = "delete from detailperkuliahan where mahasiswa = " + mahasiswa.getId()
				+ " and persetujuan = 0 and semester = " + semester;
		session.createSQLQuery(sql).executeUpdate();

		List<Perkuliahan> selectedPerkuliahans = new ArrayList<Perkuliahan>();
		for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {

			List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("matakuliah", kurikulumPunyaMatakuliah.getMatakuliah()))
					.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
							: Restrictions.eq("statusSemesterPendek", semesterPendek))

					.createAlias("jurusan", "jurusan")

					.add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))

					.add(Restrictions.eq("program", mahasiswa.getProgram()))

					.add(Restrictions.eq("semester", semester))

					.add(Restrictions.eq("tahunAjaran", paketPerkuliahan.getTahunAkademik())).add(Restrictions
							.or(Restrictions.eq("merupakan_paralel", false), Restrictions.isNull("merupakan_paralel")))
					.setMaxResults(Common.MAX_RESULT).list();

			for (Perkuliahan perkuliahan : perkuliahans) {
				Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(session, perkuliahan, false);
				if (jumlahUdahMasuk < perkuliahan.getKapasitasKelas()
						&& !Common.checkJamBentrok(selectedPerkuliahans, perkuliahan)) {
					selectedPerkuliahans.add(perkuliahan);
					break;
				}
			}
			perkuliahans = null;
		}

		if (selectedPerkuliahans.size() == 0) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, jadwal perkuliahan untuk paket perkuliahan \"{V1}\" pada semester {V2} belum ditemukan. Langkah yang dapat dilakukan: (1) pastikan jadwal perkuliahan untuk paket dan semester tersebut telah dibuat; (2) periksa kesesuaian program studi, program/kelas, serta tahun akademik; (3) ulangi proses setelah jadwal tersedia.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
					paketPerkuliahan.getNama(), semester);
			return false;
		}

		if (AmbilDataMahasiswaForPaketPerkuliahanHelper.this.semester > paketPerkuliahan.getMaxSmt()
				|| AmbilDataMahasiswaForPaketPerkuliahanHelper.this.semester < paketPerkuliahan.getMinSmt()) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, semester {V1} tidak diizinkan untuk mengikuti paket perkuliahan \"{V2}\". Langkah yang dapat dilakukan: (1) periksa rentang semester yang diizinkan pada paket perkuliahan tersebut; (2) pilih mahasiswa dengan semester yang sesuai; (3) atau sesuaikan pengaturan batas semester paket bila diperlukan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
					semester, paketPerkuliahan.getNama());
			return false;
		}

		Set<Long> matakuliahs = new HashSet<Long>();
		String peringatanKapasitasRuangan = "";
		for (final Perkuliahan perkuliahan : selectedPerkuliahans) {
			if (matakuliahs.contains(perkuliahan.getMatakuliah().getId())) {
				continue;
			}
			matakuliahs.add(perkuliahan.getMatakuliah().getId());

			Long id;
			try {
				id = (Long) (session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.isNull("ikutiPerkuliahan")).setProjection(Projections.property("id"))
						.add(Restrictions.eq("perkuliahan", perkuliahan)).add(Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.eq("semester", AmbilDataMahasiswaForPaketPerkuliahanHelper.this.semester))

						.createCriteria("perkuliahan", Criteria.LEFT_JOIN)
						.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
								: Restrictions.eq("statusSemesterPendek", semesterPendek))

						.uniqueResult());
			} catch (Exception e) {
				continue;
			}

			if (!Common.checkMatakuliahPrasyarat(perkuliahan.getMatakuliah(), mahasiswa,
					AmbilDataMahasiswaForPaketPerkuliahanHelper.this.semester)) {
				continue;
			}

			Detailperkuliahan detailperkuliahan = new Detailperkuliahan(tbmuser,
					AmbilDataMahasiswaForPaketPerkuliahanHelper.class);
			if (id != null) {
				detailperkuliahan = (Detailperkuliahan) session.load(Detailperkuliahan.class, id);
			} else {

				Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(session, perkuliahan, false);

				jumlahUdahMasuk++;
				if (jumlahUdahMasuk > (perkuliahan.getKapasitasKelas() == null ? Ruang.getDefaultKapasitas()
						: perkuliahan.getKapasitasKelas())) {
					peringatanKapasitasRuangan += "Kapasitas kelas sudah penuh. Maksimal kapasitas kelas tesebut adalah "
							+ (perkuliahan.getKapasitasKelas() == null ? Ruang.getDefaultKapasitas()
									: perkuliahan.getKapasitasKelas())
							+ ", sedangkan anda mencoba masuk ke perkuliahan ini menjadi berjumlah " + jumlahUdahMasuk
							+ ". Pilihlah jadwal perkuliahan lainnya.\n";
					continue;
				}
			}

			detailperkuliahan.setPaketPerkuliahan(paketPerkuliahan);
			detailperkuliahan.setNilaiHuruf("");
			detailperkuliahan.setTotalNilai(0.0);
			detailperkuliahan.setMahasiswa(mahasiswa);
			detailperkuliahan.setPerkuliahan(perkuliahan);
			detailperkuliahan.setSemester(AmbilDataMahasiswaForPaketPerkuliahanHelper.this.semester);
			session.saveOrUpdate(detailperkuliahan);

		}
		System.out.println("peringatanKapasitasRuangan = " + peringatanKapasitasRuangan);
		// if (!peringatanKapasitasRuangan.trim().equals("")) {
		// MyMessageboxConfig.show(peringatanKapasitasRuangan, "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		// }
		return true;
	}

	public void display(final DataLoader dataLoader, final Integer semester, final MyWindow window) {

		this.semester = semester;
		Common.clear(window);
		window.setTitle("Ambil Data Mahasiswa");
		window.setWidth("90%");
		window.setHeight("90%");

		MyPanel panel = new MyPanel();
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(tahunangkatan = new Decimalbox());
		tahunangkatan.setWidth("90%");
		Integer angkatan = Common.getTahunAngkatan(semester,
				semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
		tahunangkatan.setValue(new BigDecimal(angkatan));
		// tahunangkatan.setDisabled(true);

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
						if (myCheckbox.isDisabled()) {
							continue;
						}

						myCheckbox.setChecked(checkbox.isChecked());
						if (!checkbox.isChecked()) {
							continue;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaForPaketPerkuliahanHelper.java:486");

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

		onSearchDefault(null);
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

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

		List<Mahasiswa> mahasiswa = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(criteriaStatus).addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))
				//
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
		grid.setModelCheckMobile(strset);

	}

}
