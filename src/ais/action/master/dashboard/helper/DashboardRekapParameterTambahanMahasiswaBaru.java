package ais.action.master.dashboard.helper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.poi.ss.util.CellRangeAddress;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.model.impl.BookHelper;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.West;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.KelompokParameterTambahanCalonMahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanPaket;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardRekapParameterTambahanMahasiswaBaru extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	protected Combobox searchJenisSemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Label angkatan = new Label();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	private Combobox searchpilihan;

	private List<MyCheckboxConfig> kolom = new ArrayList<MyCheckboxConfig>();

	public DashboardRekapParameterTambahanMahasiswaBaru() {
		super();
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("200px");

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		Common.selectComboItem(tahunAkademik, tahunAkademikPenerimaanMahasiswaBaru);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		searchJenisSemester.appendChild(comboitem);

		searchJenisSemester.setSelectedItem(comboitem);
		searchJenisSemester.setReadonly(true);
		row.appendChild(searchJenisSemester);
		searchJenisSemester.setWidth("90%");
		searchJenisSemester.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		searchpilihan = new Combobox();
		comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Prodi Lulus");
		comboitem.setValue("prodi_lulus");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi I");
		comboitem.setValue("prodi_1");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi II");
		comboitem.setValue("prodi_2");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi III");
		comboitem.setValue("prodi3");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi IV");
		comboitem.setValue("prodi4");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi V");
		comboitem.setValue("prodi5");
		searchpilihan.appendChild(comboitem);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilihan"));
		row.appendChild(searchpilihan);
		searchpilihan.setWidth("90%");
		searchpilihan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		searchpilihan.setReadonly(true);
		searchpilihan.setSelectedIndex(1);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatan);
		angkatan.setValue("(tahun angkatan : semua)");

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		final Rows rowsParams = new Rows();
		rowsParams.setParent(grid);

		EventListener eventListener = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsParams);
				kolom.clear();

				MyFormRow row = new MyFormRow();
		row.setValign("top");
				row.setParent(rowsParams);

				Hbox hbox = new Hbox();
				hbox.setParent(row);

				MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
				refresh.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						initSpreadsheet();
					}
				});

				MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
				print.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						ByteArrayOutputStream bout = new ByteArrayOutputStream();
						spreadsheet.getBook().write(bout);
						bout.close();
						Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "data.xlsx");
					}
				});

				refresh.setParent(hbox);
				print.setParent(hbox);

				Session session = HibernateUtil.currentSession();
				List<KelompokParameterTambahanCalonMahasiswa> kelompokParameterTambahanCalonMahasiswas = session
						.createCriteria(ParameterTambahanPaket.class)
						.createAlias("parameterTambahan", "parameterTambahan")
						.createAlias("kelompokParameterTambahanCalonMahasiswa",
								"kelompokParameterTambahanCalonMahasiswa")
						.add(Restrictions.eq("parameterTambahan.aktif", true))
						.add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa.aktif", true))
						.setProjection(Projections.groupProperty("kelompokParameterTambahanCalonMahasiswa")).list();
				Collections.sort(kelompokParameterTambahanCalonMahasiswas);
				for (KelompokParameterTambahanCalonMahasiswa kelompokParameterTambahanCalonMahasiswa : kelompokParameterTambahanCalonMahasiswas) {
					List<ParameterTambahan> parameterTambahans = session.createCriteria(ParameterTambahanPaket.class)
							.add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa",
									kelompokParameterTambahanCalonMahasiswa))
							.createAlias("parameterTambahan", "parameterTambahan")
							.createAlias("kelompokParameterTambahanCalonMahasiswa",
									"kelompokParameterTambahanCalonMahasiswa")
							.add(Restrictions.eq("parameterTambahan.aktif", true))
							.add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa.aktif", true))
							.add(Restrictions.eq("parameterTambahan.tipeDataInputan", ParameterTambahan.PILIHAN_CUSTOM))
							.setProjection(Projections.groupProperty("parameterTambahan")).list();
					Collections.sort(parameterTambahans);

					row = new MyFormRow();
					row.setParent(rowsParams);
					row.appendChild(new MyLabelBold(kelompokParameterTambahanCalonMahasiswa.getNama()));

					for (final ParameterTambahan parameterTambahan : parameterTambahans) {
						row = new MyFormRow();
						row.setParent(rowsParams);
						MyCheckboxConfig checkboxConfig = new MyCheckboxConfig(parameterTambahan.getLabelInputan());
						checkboxConfig.setAttribute("parameterTambahan", parameterTambahan);
						checkboxConfig.setAttribute("kelompokParameterTambahanCalonMahasiswa",
								kelompokParameterTambahanCalonMahasiswa);
						checkboxConfig.setParent(row);
						kolom.add(checkboxConfig);
					}
				}
			}
		};

		eventListener.onEvent(null);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		

		initSpreadsheet();

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initSpreadsheet() {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);
				String tahunAkademik = (String) (DashboardRekapParameterTambahanMahasiswaBaru.this.tahunAkademik
						.getSelectedItem() == null ? null
								: DashboardRekapParameterTambahanMahasiswaBaru.this.tahunAkademik.getSelectedItem()
										.getValue());
				Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? null
						: searchfakultas.getSelectedItem().getValue());
				Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null || searchjurusan.getSelectedItem().getValue()==null ? null
						: searchjurusan.getSelectedItem().getValue());

				String program = (String) (searchprogram.getSelectedItem() == null||searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

				if (tahunAkademik == null) {
					return;
				}

				String tahun = tahunAkademik.substring(0, 4);
				DashboardRekapParameterTambahanMahasiswaBaru.this.angkatan.setValue(tahun);

				Session session = HibernateUtil.currentSession();

				List<List<Object[]>> jurusansSemua = new ArrayList<List<Object[]>>();
				List<List> generalValueObjectsSemua = new ArrayList<List>();
				List<String> namadata = new ArrayList<String>();

				int maxColoumn = 0;
				int totalData = 0;
				for (MyCheckboxConfig checkboxConfig : kolom) {
					if (checkboxConfig.isChecked()) {
						ParameterTambahan parameterTambahan = (ParameterTambahan) checkboxConfig
								.getAttribute("parameterTambahan");
						KelompokParameterTambahanCalonMahasiswa kelompokParameterTambahanCalonMahasiswa = (KelompokParameterTambahanCalonMahasiswa) checkboxConfig
								.getAttribute("kelompokParameterTambahanCalonMahasiswa");
						namadata.add(checkboxConfig.getLabel());

						List<Object[]> jurusans = new ArrayList<Object[]>();

						List generalValueObjects = new ArrayList();
						String[] ss = StringUtils.split(parameterTambahan.getNilaiDataInputan(), ";");
						Arrays.sort(ss);

						for (String s : ss) {

							String[] kol = StringUtils.split(s, ":");
							String a = kol[0];
							generalValueObjects.add(a);
						}

						Collections.sort(generalValueObjects);
						generalValueObjects.add("Tidak di-pilih");

						if (generalValueObjects.size() > maxColoumn) {
							maxColoumn = generalValueObjects.size();
						}

						String sql = "select max(y.nama) as fakultas, max(x.nama) as jurusan, ";

						List<String> likes = new ArrayList<String>();
						for (Object obj : generalValueObjects) {
							if (obj != null && !obj.toString().isEmpty() && !obj.toString().equals("Tidak di-pilih")) {
								String like = kelompokParameterTambahanCalonMahasiswa.getId() + "->"
										+ parameterTambahan.getId() + "<=>" + StringUtils
												.replace(org.apache.commons.lang3.StringUtils.replace(obj.toString(), "\'", "''"), "\"", "");
								likes.add(like);
								sql += "sum(case when aaa.parametertambahaninds ilike '%" + like
										+ "%' and aaa.jenis_kelamin='Laki-laki' then 1 else 0 end) as \"" + obj
										+ " Laki-laki\", ";
								sql += "sum(case when aaa.parametertambahaninds ilike '%" + like
										+ "%' and aaa.jenis_kelamin='Perempuan' then 1 else 0 end) as \"" + obj
										+ " Perempuan\", ";
								sql += "sum(case when aaa.parametertambahaninds ilike '%" + like
										+ "%' then 1 else 0 end) as \"" + obj + "\", ";

							}
						}

						String orIlike = "";
						for (String like : likes) {
							orIlike += orIlike.isEmpty() ? "(aaa.parametertambahaninds not ilike '%" + like + "%')"
									: " and (aaa.parametertambahaninds not ilike '%" + like + "%')";
						}

						sql += "sum(case when " + (orIlike.isEmpty() ? "true" : "(" + orIlike + ")")
								+ " and aaa.jenis_kelamin='Laki-laki' then 1 else 0 end) as \"Tidak di-pilih Laki-laki\", ";
						sql += "sum(case when " + (orIlike.isEmpty() ? "true" : "(" + orIlike + ")")
								+ " and aaa.jenis_kelamin='Perempuan' then 1 else 0 end) as \"Tidak di-pilih Perempuan\", ";
						sql += "sum(case when " + (orIlike.isEmpty() ? "true" : "(" + orIlike + ")")
								+ " then 1 else 0 end) as \"Tidak di-pilih\", ";

						sql += "sum(case when aaa.parametertambahaninds is not null and aaa.jenis_kelamin='Laki-laki' then 1 else 0 end) as \"Laki-laki\", ";
						sql += "sum(case when aaa.parametertambahaninds is not null and aaa.jenis_kelamin='Perempuan' then 1 else 0 end) as \"Perempuan\", ";

						String pilihan = (String) searchpilihan.getSelectedItem().getValue();

						sql += " sum(case when aaa.parametertambahaninds is not null then 1 else 0 end) as total from biodata_calon_mahasiswa aaa  "
								+ " inner join jurusan x on (aaa." + pilihan + " = x.id  )    "
								+ " inner join fakultas y on (y.id = x.fakultas)      where 1=1 "
								+ (searchJenisSemester.getSelectedItem().getValue() == null ? ""
										: "and aaa.semester_mulai='" + searchJenisSemester.getSelectedItem().getValue()
												+ "' ")
								+ (program == null ? "" : " and aaa.program = '" + program + "'")
								+ (jurusan == null ? "" : " and aaa." + pilihan + " = " + jurusan.getId())
								+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId())
								+ " and aaa.tahun = " + tahun + " group by x.fakultas,aaa." + pilihan
								+ " order by max(y.nama), max(x.nama) ";

						System.out.println(sql);
						jurusans = Common.ambilSql(sql);
						jurusansSemua.add(jurusans);
						generalValueObjectsSemua.add(generalValueObjects);

						totalData += jurusans.size();
						totalData += 3;
					}
				}

				spreadsheet = new ais.ui.util.MySpreadsheet();
Common.clear(center);spreadsheet.setParent(center);
				spreadsheet.setWidth("100%");
				spreadsheet.setHeight("100%");
				spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
				spreadsheet.setMaxcolumns((maxColoumn * 3) + 5);
				spreadsheet.setMaxrows(totalData + 5);

				Worksheet sheet = spreadsheet.getSelectedSheet();
				sheet.setDefaultColumnWidth(40);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

				ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
						"REKAPITULASI MAHASISWA BARU " + searchpilihan.getValue().toUpperCase() + "\n"
								+ (fakultas == null ? "" : fakultas.getNama().toUpperCase() + "\n")
								+ (jurusan == null ? "" : jurusan.getNama().toUpperCase() + "\n") + " TAHUN AKADEMIK "
								+ tahunAkademik + " "
								+ (searchJenisSemester.getSelectedItem().getValue() == null ? ""
										: searchJenisSemester.getSelectedItem().getValue())
								+ "\nPROGRAM " + (program == null ? "SEMUA" : program.toUpperCase()));
				final String color = "#000000";

				int rowIndex = 0;

				Utils.setRowHeight(sheet, 1, 150);
				ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
				Cell cell = Utils.getCell(sheet, 1, 0);
				cell.getCellStyle().setWrapText(true);
				cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

				sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, spreadsheet.getMaxcolumns() - 1));

				for (int indexData = 0; indexData < generalValueObjectsSemua.size(); indexData++) {

					rowIndex += 3;

					List generalValueObjects = generalValueObjectsSemua.get(indexData);
					List<Object[]> jurusans = jurusansSemua.get(indexData);

					String namaData = namadata.get(indexData);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 0, namaData.toUpperCase());

					ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex - 1, 0, rowIndex - 1,
							(generalValueObjects.size() * 3) + 4, true);

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Fakultas");
					Utils.setColumnWidth(sheet, 0, 200);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Jurusan");
					Utils.setColumnWidth(sheet, 1, 200);

					int colIndex = 2;
					for (Object obj : generalValueObjects) {
						if (colIndex > 250) {
							break;
						}
						if (obj != null && !obj.toString().isEmpty()) {
							if (obj instanceof GeneralValueObject) {
								GeneralValueObject generalValueObject = (GeneralValueObject) obj;
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex,
										generalValueObject.getNama());
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1,
										generalValueObject.getNama());
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2,
										generalValueObject.getNama());
							} else {
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, obj);
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, obj);
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, obj);
							}

							ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, colIndex, rowIndex, colIndex + 2, true);

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex, "Laki-laki");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 1, "Perempuan");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 2, "Total");
							Utils.setColumnWidth(sheet, colIndex, 70);
							Utils.setColumnWidth(sheet, colIndex + 1, 70);
							Utils.setColumnWidth(sheet, colIndex + 2, 70);
							colIndex += 3;
						}
					}

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, "Total");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, "Total");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, "Total");

					ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, colIndex, rowIndex, colIndex + 2, true);

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex, "Laki-laki");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 1, "Perempuan");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 2, "Total");

					Utils.setColumnWidth(sheet, colIndex, 70);
					Utils.setColumnWidth(sheet, colIndex + 1, 70);
					Utils.setColumnWidth(sheet, colIndex + 2, 70);

					try {
						ais.ui.util.EcampusUtil.setBorder(sheet,
								new Rect(0, rowIndex, (generalValueObjects.size() * 3) + 4, rowIndex + 1),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
						ais.ui.util.EcampusUtil.setBold(sheet,
								new Rect(0, rowIndex, (generalValueObjects.size() * 3) + 4, rowIndex + 1), true);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapParameterTambahanMahasiswaBaru.java:582");

					}

					rowIndex += 2;
					colIndex = 0;

					String namaFakultas = "";
					String namaProdi = "";
					Integer[] nilaisLaki = new Integer[generalValueObjects.size()];
					Integer[] nilaisPerempuan = new Integer[generalValueObjects.size()];
					Integer[] nilais = new Integer[generalValueObjects.size()];

					Integer totalLaki = 0;
					Integer totalPerempuan = 0;
					Integer total = 0;
					for (Object[] objects : jurusans) {
						if (objects[0] != null) {
							if (!namaFakultas.equals(objects[0].toString())) {
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[0].toString());
								namaFakultas = objects[0].toString();
							} else {
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "");
							}

							if (!namaProdi.equals(objects[1].toString())) {
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, objects[1].toString());
								namaProdi = objects[1].toString();
							} else {
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "");
							}
						} else {
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0,
									"Tidak pilih " + "Fakultas");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
									"Tidak pilih " + Common.getBahasaConfig("Jurusan"));
						}

						colIndex = 2;
						int index = 0;
						for (Object generalValueObject : generalValueObjects) {
							if (colIndex > 250) {
								break;
							}
							if (generalValueObject != null && !generalValueObject.toString().isEmpty()) {
								if (nilaisLaki[index] == null) {
									nilaisLaki[index] = 0;
								}
								Integer nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex]))
										.intValue();
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
								nilaisLaki[index] += nilai0;
								colIndex++;

								if (nilaisPerempuan[index] == null) {
									nilaisPerempuan[index] = 0;
								}
								nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
								nilaisPerempuan[index] += nilai0;
								colIndex++;

								if (nilais[index] == null) {
									nilais[index] = 0;
								}
								nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
								nilais[index] += nilai0;
								colIndex++;

								index++;
							}
						}

						Integer td = ((Number) (objects[objects.length - 3] == null ? 0 : objects[objects.length - 3]))
								.intValue();
						totalLaki += td;
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, td);
						colIndex++;

						td = ((Number) (objects[objects.length - 2] == null ? 0 : objects[objects.length - 2]))
								.intValue();
						totalPerempuan += td;
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, td);
						colIndex++;

						td = ((Number) (objects[objects.length - 1] == null ? 0 : objects[objects.length - 1]))
								.intValue();
						total += td;
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, td);

						rowIndex++;
					}

					try {
						ais.ui.util.EcampusUtil.setBorder(sheet,
								new Rect(0, rowIndex - jurusans.size(), (generalValueObjects.size() * 3) + 4, rowIndex),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapParameterTambahanMahasiswaBaru.java:680");
					}

					try {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
						colIndex = 2;
						for (int i = 0; i < nilais.length; i++) {
							int jum = nilaisLaki[i];
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jum);
							colIndex++;

							jum = nilaisPerempuan[i];
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jum);
							colIndex++;

							jum = nilais[i];
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jum);
							colIndex++;
						}

						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, totalLaki);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, totalPerempuan);
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, total);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapParameterTambahanMahasiswaBaru.java:703");

					}

					try {

						ais.ui.util.EcampusUtil.setBold(sheet,
								new Rect(colIndex, rowIndex, (generalValueObjects.size() * 3) + 4, rowIndex), true);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapParameterTambahanMahasiswaBaru.java:711");

					}

				}

				Common.setStyled(sheet);spreadsheet.setMaxrows(rowIndex + 1);
				// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

				// try {
				// ais.ui.util.EcampusUtil.setBold(sheet,
				// new Rect(spreadsheet.getMaxcolumns() - 1, 3,
				// spreadsheet.getMaxcolumns() - 1, rowIndex),
				// true);
				// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapParameterTambahanMahasiswaBaru.java:726");
				// }
			}
		});

	}
}
