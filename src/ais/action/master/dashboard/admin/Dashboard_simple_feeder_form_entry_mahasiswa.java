package ais.action.master.dashboard.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class Dashboard_simple_feeder_form_entry_mahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox searchstatus;
	private Combobox searchStatusAwalMahasiswa;
	private Label angkatan = new Label();
	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();
	private Center center = new Center();

	private File file;

	// private MyCheckboxConfig BelumDinilai;
	// private MyCheckboxConfig TelahDinilai;

	public Dashboard_simple_feeder_form_entry_mahasiswa() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public Dashboard_simple_feeder_form_entry_mahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		initFakultas();

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("200px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		// searchfakultas.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initSpreadsheet();
		// }
		// });

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		// searchjurusan.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initSpreadsheet();
		// }
		// });

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);
		// tahunAkademik.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initSpreadsheet();
		// }
		// });

		Common.insertCombo(searchstatus = new Combobox(), new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		Common.insertCombo(searchStatusAwalMahasiswa = new Combobox(), "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		row.appendChild(searchstatus);
		searchstatus.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen PA"));
		row.appendChild(searchDosen);
		searchDosen.setWidth("90%");
		searchDosen.setWidth("90%");
		// searchDosen.setEventListener(new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initSpreadsheet();
		// }
		// });

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(1);
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");
		semesterAbsensi.setReadonly(true);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester ke"));
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatan);
		angkatan.setValue("(tahun angkatan : semua)");
		angkatan.setWidth("90%");
		Common.initPrograms(searchprogram);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal"));
		row.appendChild(searchStatusAwalMahasiswa);
		searchStatusAwalMahasiswa.setWidth("90%");

		Common.checkProgramString(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		// searchprogram.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// initSpreadsheet();
		// }
		// });

		final EventListener semesterEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final String tahunAkademik = (String) (Dashboard_simple_feeder_form_entry_mahasiswa.this.tahunAkademik
						.getSelectedItem() == null ? null
								: Dashboard_simple_feeder_form_entry_mahasiswa.this.tahunAkademik.getSelectedItem()
										.getValue());
				final String semester = (String) (Dashboard_simple_feeder_form_entry_mahasiswa.this.semesterAbsensi
						.getSelectedItem() == null ? Perkuliahan.GANJIL
								: Dashboard_simple_feeder_form_entry_mahasiswa.this.semesterAbsensi.getSelectedItem()
										.getValue());

				final Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? -1
						: searchsemester.getSelectedItem().getValue());

				if (tahunAkademik == null) {
					return;
				}
				final Integer tahunAngkatan = Common.getTahunAngkatan(semesterKe, semester, tahunAkademik);
				final Boolean semuasemester = searchsemester.getSelectedItem() == null;
				if (!semuasemester) {
					angkatan.setValue("(tahun angkatan : " + tahunAngkatan + ")");
				}
			}
		};

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchsemester);
				searchsemester.setSelectedItem(null);
				angkatan.setValue("(tahun angkatan : semua)");
				if (semesterAbsensi.getSelectedItem() == null) {
					return;
				}
				Boolean genap = semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
				if (genap) {
					for (int i : Common.genap) {
						if (i == 0)
							continue;
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				} else {
					for (int i : Common.ganjil) {
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				}

				semesterEventListener.onEvent(null);
			}
		};
		// semesterAbsensi.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// eventListener.onEvent(arg0);
		// initSpreadsheet();
		// }
		// });
		//
		searchsemester.addEventListener("onChange", semesterEventListener);
		tahunAkademik.addEventListener("onChange", semesterEventListener);

		semesterAbsensi.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(row);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		
		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Data Mahasiswa.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/Dashboard_simple_feeder_form_entry_mahasiswa.java:377");

				}
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() throws Exception {

		Common.clear(center);
		final String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());

		final String semester = (String) (this.semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? Perkuliahan.GANJIL
						: this.semesterAbsensi.getSelectedItem().getValue());

		final Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		final Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		final Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? -1
				: searchsemester.getSelectedItem().getValue());
		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final Dosen dosen = (Dosen) searchDosen.getAttribute("dosen");

		if (tahunAkademik == null) {
			return;
		}
		final Integer tahunAngkatan = Common.getTahunAngkatan(semesterKe, semester, tahunAkademik);
		final Boolean semuasemester = searchsemester.getSelectedItem() == null;
		if (!semuasemester) {
			angkatan.setValue("(tahun angkatan : " + tahunAngkatan + ")");
		}
		final int tahun = Integer.parseInt(StringUtils.split(tahunAkademik, "/")[0]);

		System.out.println("init spreadsheet running => tahun = " + tahun);

		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		(file = new File(filename)).createNewFile();

		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);
		// final boolean telahDinilai = TelahDinilai.isChecked();
		// final boolean belumDinilai = BelumDinilai.isChecked();

		new Thread(new Runnable() {

			@Override
			public void run() {
				// Session KHUSUS thread ini (openSession). Dideklarasi di LUAR try agar bisa
				// ditutup di blok finally. Tidak memakai currentNativeSession() (thread-local)
				// karena util lain di dalam loop menutup native thread-local -> "Session is closed!".
				Session session = null;
				try {

				System.out.println("tahunAkademik = " + tahunAkademik);

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFSheet sheet = workbook.createSheet("AKM");
				sheet.setDefaultColumnWidth(25);

				XSSFRow rowhead = sheet.createRow((short) 0);

				rowhead.createCell(0).setCellValue("id_sp");
				rowhead.createCell(1).setCellValue("regpd_id_sms");
				rowhead.createCell(2).setCellValue("regpd_id_sp");
				rowhead.createCell(3).setCellValue("nm_pd");
				rowhead.createCell(4).setCellValue("regpd_nipd");
				rowhead.createCell(5).setCellValue("jk");
				rowhead.createCell(6).setCellValue("tmpt_lahir");
				rowhead.createCell(7).setCellValue("tgl_lahir");
				rowhead.createCell(8).setCellValue("id_agama");
				rowhead.createCell(9).setCellValue("id_kk");
				rowhead.createCell(10).setCellValue("jln");
				rowhead.createCell(11).setCellValue("ds_kel");
				rowhead.createCell(12).setCellValue("id_wil");
				rowhead.createCell(13).setCellValue("a_terima_kps");
				rowhead.createCell(14).setCellValue("stat_pd");
				rowhead.createCell(15).setCellValue("nm_ayah");
				rowhead.createCell(16).setCellValue("id_kebutuhan_khusus_ayah");
				rowhead.createCell(17).setCellValue("nm_ibu_kandung");
				rowhead.createCell(18).setCellValue("id_kebutuhan_khusus_ibu");
				rowhead.createCell(19).setCellValue("kewarganegaraan");
				rowhead.createCell(20).setCellValue("regpd_id_jns_daftar");
				rowhead.createCell(21).setCellValue("regpd_tgl_masuk_sp");
				rowhead.createCell(22).setCellValue("regpd_mulai_smt");
				rowhead.createCell(23).setCellValue("nisn");

				StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
						|| searchstatus.getSelectedItem().getValue() == null ? null
								: searchstatus.getSelectedItem().getValue());

				// Buka session KHUSUS (bukan currentNativeSession thread-local): TIDAK disimpan di
				// ThreadLocal sehingga KEBAL dari HibernateUtil.closeSession() yang dipanggil util lain
				// (mis. Common.singkronkanKrsMahasiswa) di tengah loop. Ditutup di blok finally.
				session = HibernateUtil.openSession();

				List<Mahasiswa> biodataMahasiswas = ConstantValues.simpleList(session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(searchStatusAwalMahasiswa.getSelectedItem() == null
								|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
								|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("statusAwalMahasiswa",
												searchStatusAwalMahasiswa.getSelectedItem().getValue()))

						.add(dosen != null ? Restrictions.eq("dosen", dosen.getId())
								: Restrictions.sqlRestriction("1=1"))
						.createAlias("jurusan", "jurusan")

						.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))

						.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jurusan.fakultas", fakultas))

						.add(jurusan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", jurusan))

						.add(program == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("program", program))

						.add(semuasemester ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunangkatan", tahunAngkatan))

						, Mahasiswa.class);

				int size = biodataMahasiswas.size();

				int rowIndex = 1;
				for (Mahasiswa mahasiswa : biodataMahasiswas) {

					Integer currentSemester = Common.getSemester(mahasiswa.getTahunangkatan(), semester,
							mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());
					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, currentSemester,
							null, null);
					HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krsMahasiswa);

					if (statusMahasiswa == null || (historyStatusMahasiswa != null
							&& historyStatusMahasiswa.getStatusMahasiswa() != null
							&& historyStatusMahasiswa.getStatusMahasiswa().getId().equals(statusMahasiswa.getId()))) {

						// Query pakai session KHUSUS (openSession di awal thread) yang TETAP TERBUKA
						// selama loop. Common.singkronkanKrsMahasiswa di atas hanya menutup native
						// session thread-local, TIDAK menyentuh session khusus ini -> aman dari
						// "Session is closed!". Objek Mahasiswa berasal dari cache ConstantValues
						// (tidak terikat session ini), jadi tak ada konflik antar-session.
						BiodataMahasiswa biodataMahasiswa = (BiodataMahasiswa) session
								.createCriteria(BiodataMahasiswa.class).add(Restrictions.eq("mahasiswa", mahasiswa))
								.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
						// Mahasiswa mahasiswa = biodataMahasiswa.getMahasiswa();
						label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
								+ Common.numberFormat.get().format(rowIndex * 100.0 / size) + " %)");

						XSSFRow row = sheet.createRow(rowIndex);
						XSSFCell cell = row.createCell(0);
						cell.setCellValue(mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getFeeder());

						cell = row.createCell(1);
						cell.setCellValue(mahasiswa.getJurusan().getFeeder());

						cell = row.createCell(2);
						cell.setCellValue(mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
								: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getFeeder());

						cell = row.createCell(3);
						cell.setCellValue(Common.maxPanjang(mahasiswa.getNama(), 50));

						cell = row.createCell(4);
						cell.setCellValue(Common.maxPanjang(mahasiswa.getNim(), 18));

						cell = row.createCell(5);
						cell.setCellValue(mahasiswa.getKelamin() == null ? "*"
								: mahasiswa.getKelamin().equalsIgnoreCase("Laki-laki") ? "L" : "P");

						cell = row.createCell(6);
						cell.setCellValue(Common.maxPanjang(mahasiswa.getTempatlahir(), 20));

						cell = row.createCell(7);
						if (mahasiswa.getTanggallahir() != null) {
							cell.setCellValue(Common.databaseDateFormat.get().format(mahasiswa.getTanggallahir()));
						}

						cell = row.createCell(8);
						if (mahasiswa.getAgama() != null) {
							cell.setCellValue(mahasiswa.getAgama().getFeeder());
						} else {
							cell.setCellValue(98);
						}

						cell = row.createCell(9);
						cell.setCellValue(0);

						cell = row.createCell(10);
						cell.setCellValue(Common.maxPanjang(mahasiswa.getAlamat(), 80));

						cell = row.createCell(11);
						cell.setCellValue(
								Common.maxPanjang(biodataMahasiswa == null ? "" : biodataMahasiswa.getKelurahan(), 40));

						cell = row.createCell(12);
						if (biodataMahasiswa != null && biodataMahasiswa.getKecamatan() != null
								&& biodataMahasiswa.getKecamatan().getFeeder() != null
								&& !biodataMahasiswa.getKecamatan().getFeeder().trim().isEmpty()) {
							cell.setCellValue(biodataMahasiswa.getKecamatan().getFeeder());
						} else {
							cell.setCellValue("000000");
						}

						cell = row.createCell(13);
						cell.setCellValue(0);

						cell = row.createCell(14);

						if (historyStatusMahasiswa != null
								&& historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed() != null
								&& (historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
										.equalsIgnoreCase("A")
										|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
												.equalsIgnoreCase("C")
										|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
												.equalsIgnoreCase("D")
										|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
												.equalsIgnoreCase("L")
										|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
												.equalsIgnoreCase("P")
										|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
												.equalsIgnoreCase("N")
										|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
												.equalsIgnoreCase("G")
										|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
												.equalsIgnoreCase("X")
										|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim()
												.equalsIgnoreCase("K"))

						) {
							cell.setCellValue(historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed());
						} else {
							cell.setCellValue("X");
						}

						cell = row.createCell(15);
						cell.setCellValue(
								Common.maxPanjang(biodataMahasiswa == null ? "" : biodataMahasiswa.getNamaAyah(), 50));

						cell = row.createCell(16);
						cell.setCellValue(0);

						cell = row.createCell(17);
						cell.setCellValue(
								Common.maxPanjang(biodataMahasiswa == null ? "" : biodataMahasiswa.getNamaIbu(), 50));

						cell = row.createCell(18);
						cell.setCellValue(0);

						cell = row.createCell(19);
						cell.setCellValue("ID");

						cell = row.createCell(20);
						if (mahasiswa.getStatusAwalMahasiswa() != null) {
							cell.setCellValue(mahasiswa.getStatusAwalMahasiswa().getFeeder());
						}

						cell = row.createCell(21);
						if (mahasiswa.getTanggalMasuk() != null) {
							cell.setCellValue(Common.databaseDateFormat.get().format(mahasiswa.getTanggalMasuk()));
						}

						cell = row.createCell(22);
						if (mahasiswa.getTahunangkatan() != null) {
							cell.setCellValue(mahasiswa.getTahunangkatan()
									+ (mahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL) ? "1" : "2"));
						}

						cell = row.createCell(23);
						cell.setCellValue(biodataMahasiswa == null ? "" : biodataMahasiswa.getNisn());

						rowIndex++;
					}
				}

				Common.setStyled(sheet);sizedata.setValue(rowIndex + 1);

				try {
					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

				HibernateUtil.closeSession();

				biodataMahasiswas.clear();
				label.setValue("");
							} finally {
					// Tutup session KHUSUS thread di finally: clear + disconnect + close (null-safe).
					if (session != null) {
						try { session.clear(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/Dashboard_simple_feeder_form_entry_mahasiswa.java:690");}
						try { session.disconnect(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/Dashboard_simple_feeder_form_entry_mahasiswa.java:691");}
						try { session.close(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/Dashboard_simple_feeder_form_entry_mahasiswa.java:692");}
					}
					// Bersihkan juga native session thread-local sisa util (mis. singkronkanKrsMahasiswa).
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}
}
