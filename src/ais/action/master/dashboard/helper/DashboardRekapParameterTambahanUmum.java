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
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.West;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.GrupChecklistPenilaianUmum;
import ais.database.model.JadwalChecklistPenilaianUmum;
import ais.database.model.Jurusan;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanAngketUmum;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardRekapParameterTambahanUmum extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchtahunakademik = new Combobox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	private List<MyCheckboxConfig> kolom = new ArrayList<MyCheckboxConfig>();

	public DashboardRekapParameterTambahanUmum() {
		super();

		try {
			initFakultas();
			init();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initFakultas() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchsemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchsemester.appendChild(comboitem);

		Common.generateTahunAjaran(searchtahunakademik);
		searchtahunakademik.setReadonly(true);
		searchsemester.setReadonly(true);

		Common.selectComboItem(searchsemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

	}

	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(searchtahunakademik);
		searchtahunakademik.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");

		West west = new West();
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("250px");

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
						Filedownload.save(bout.toByteArray(),
								"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "data.xlsx");
					}
				});

				refresh.setParent(hbox);
				print.setParent(hbox);

				Session session = HibernateUtil.currentSession();
				List<GrupChecklistPenilaianUmum> grupChecklistPenilaianUmums = session
						.createCriteria(JadwalChecklistPenilaianUmum.class)
						.add(Restrictions.eq("tahunAkademik", searchtahunakademik.getSelectedItem().getValue()))
						.add(Restrictions.eq("semester", searchsemester.getSelectedItem().getValue()))
						.createAlias("grupChecklistPenilaianUmum", "grupChecklistPenilaianUmum")
						.add(Restrictions.eq("grupChecklistPenilaianUmum.aktif", true))
						.setProjection(Projections.groupProperty("grupChecklistPenilaianUmum")).list();
				Collections.sort(grupChecklistPenilaianUmums);
				for (GrupChecklistPenilaianUmum grupChecklistPenilaianUmum : grupChecklistPenilaianUmums) {
					List<ParameterTambahanAngketUmum> parameterTambahanAngketUmums = session
							.createCriteria(ParameterTambahanAngketUmum.class)
							.createAlias("parameterTambahan", "parameterTambahan")
							.add(Restrictions.eq("grupChecklistPenilaianUmum", grupChecklistPenilaianUmum))
							.add(Restrictions.eq("parameterTambahan.tipeDataInputan", ParameterTambahan.PILIHAN_CUSTOM))
							.list();
					Collections.sort(parameterTambahanAngketUmums);

					row = new MyFormRow();
					row.setParent(rowsParams);
					row.appendChild(new MyLabelBold(grupChecklistPenilaianUmum.getIsi()));

					for (final ParameterTambahanAngketUmum parameterTambahanAngketUmum : parameterTambahanAngketUmums) {
						row = new MyFormRow();
						row.setParent(rowsParams);
						MyCheckboxConfig checkboxConfig = new MyCheckboxConfig(
								parameterTambahanAngketUmum.getParameterTambahan().getLabelInputan());
						checkboxConfig.setAttribute("parameterTambahan",
								parameterTambahanAngketUmum.getParameterTambahan());
						checkboxConfig.setAttribute("grupChecklistPenilaianUmum", grupChecklistPenilaianUmum);
						checkboxConfig.setParent(row);
						kolom.add(checkboxConfig);
					}
				}
			}
		};

		searchtahunakademik.addEventListener("onChange", eventListener);
		searchsemester.addEventListener("onChange", eventListener);

		eventListener.onEvent(null);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		initSpreadsheet();

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initSpreadsheet() {

		Common.clear(center);

		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		List<List<Object[]>> jurusansSemua = new ArrayList<List<Object[]>>();
		List<List> generalValueObjectsSemua = new ArrayList<List>();
		List<String> namadata = new ArrayList<String>();

		int maxColoumn = 0;
		int totalData = 0;
		for (MyCheckboxConfig checkboxConfig : kolom) {
			if (checkboxConfig.isChecked()) {

				namadata.add(checkboxConfig.getLabel());
				ParameterTambahan parameterTambahan = (ParameterTambahan) checkboxConfig
						.getAttribute("parameterTambahan");
				GrupChecklistPenilaianUmum grupChecklistPenilaianUmum = (GrupChecklistPenilaianUmum) checkboxConfig
						.getAttribute("grupChecklistPenilaianUmum");

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
						String like = grupChecklistPenilaianUmum.getId() + "->" + parameterTambahan.getId() + "<=>"
								+ org.apache.commons.lang3.StringUtils.replace(org.apache.commons.lang3.StringUtils.replace(obj.toString(), "\'", "''"), "\"", "");
						likes.add(like);
						sql += "sum(case when aaa.parametertambahan ilike '%" + like
								+ "%' and (m.kelamin='Laki-laki' or m1.kelamin='Laki-laki' or m2.kelamin='Laki-laki') then 1 else 0 end) as \""
								+ obj + " Laki-laki\", ";
						sql += "sum(case when aaa.parametertambahan ilike '%" + like
								+ "%' and (m.kelamin='Perempuan' or m1.kelamin='Perempuan' or m2.kelamin='Perempuan') then 1 else 0 end) as \""
								+ obj + " Perempuan\", ";
						sql += "sum(case when aaa.parametertambahan ilike '%" + like + "%' then 1 else 0 end) as \""
								+ obj + "\", ";

					}
				}

				String orIlike = "";
				for (String like : likes) {
					orIlike += orIlike.isEmpty() ? "(aaa.parametertambahan not ilike '%" + like + "%')"
							: " and (aaa.parametertambahan not ilike '%" + like + "%')";
				}

				sql += "sum(case when " + (orIlike.isEmpty() ? "true" : "(" + orIlike + ")")
						+ " and (m.kelamin='Laki-laki' or m1.kelamin='Laki-laki' or m2.kelamin='Laki-laki') then 1 else 0 end) as \"Tidak di-pilih Laki-laki\", ";
				sql += "sum(case when " + (orIlike.isEmpty() ? "true" : "(" + orIlike + ")")
						+ " and (m.kelamin='Perempuan' or m1.kelamin='Perempuan' or m2.kelamin='Perempuan') then 1 else 0 end) as \"Tidak di-pilih Perempuan\", ";
				sql += "sum(case when " + (orIlike.isEmpty() ? "true" : "(" + orIlike + ")")
						+ " then 1 else 0 end) as \"Tidak di-pilih\", ";

				sql += "sum(case when aaa.parametertambahan is not null and (m.kelamin='Laki-laki' or m1.kelamin='Laki-laki' or m2.kelamin='Laki-laki') then 1 else 0 end) as \"Laki-laki\", ";
				sql += "sum(case when aaa.parametertambahan is not null and (m.kelamin='Perempuan' or m1.kelamin='Perempuan' or m2.kelamin='Perempuan') then 1 else 0 end) as \"Perempuan\", ";

				sql += " sum(case when aaa.parametertambahan is not null then 1 else 0 end) as total "
						+ " from isi_angket_parameter_umum aaa"
						+ " inner join jadwal_checklist_penilaian_umum bbb on (aaa.jadwal_checklist_penilaian_umum=bbb.id)  "
						+ " left join mahasiswa m on (aaa.mahasiswa = m.id  )    "
						+ " left join dosen m1 on (aaa.dosen = m1.id  )    "
						+ " left join tbmuser m2 on (aaa.tbmuser = m2.userid  )    "
						+ " inner join jurusan x on (m.jurusan = x.id or m1.jurusan = x.id or m2.jurusan = x.id)    "
						+ " inner join fakultas y on (y.id = x.fakultas)      where 1=1 and bbb.tahunakademik='"
						+ searchtahunakademik.getSelectedItem().getValue() + "' and bbb.semester='"
						+ searchsemester.getSelectedItem().getValue() + "' "
						+ (jurusan == null ? "" : " and m.jurusan = " + jurusan.getId())
						+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId())
						+ " group by x.fakultas,m.jurusan order by max(y.nama), max(x.nama) ";

				System.out.println(sql);
				jurusans = Common.ambilSql(sql);
				jurusansSemua.add(jurusans);
				generalValueObjectsSemua.add(generalValueObjects);

				totalData += jurusans.size();
				totalData += 3;

			}
		}

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns((maxColoumn * 3) + 5);
		spreadsheet.setMaxrows(totalData + 5);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0, "REKAPITULASI PARAMETER");
		final String color = "#000000";
		int rowIndex = 0;

		Utils.setRowHeight(sheet, 1, 150);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);

		for (int indexData = 0; indexData < generalValueObjectsSemua.size(); indexData++) {

			rowIndex += 3;

			List generalValueObjects = generalValueObjectsSemua.get(indexData);
			List<Object[]> jurusans = jurusansSemua.get(indexData);

			String namaData = namadata.get(indexData);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 0, namaData.toUpperCase());

			ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex - 1, 0, rowIndex - 1,
					(generalValueObjects.size() * 3) + 4, true);

			ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
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
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, generalValueObject.getNama());
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

			ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, colIndex, rowIndex, colIndex + 2, false);

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex, "Laki-laki");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 1, "Perempuan");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 2, "Total");
			// Utils.setColumnWidth(sheet, colIndex + 1, 100);
			Utils.setColumnWidth(sheet, colIndex, 70);
			Utils.setColumnWidth(sheet, colIndex + 1, 70);
			Utils.setColumnWidth(sheet, colIndex + 2, 70);

			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(0, rowIndex, (generalValueObjects.size() * 3) + 4, rowIndex + 1),
						BookHelper.BORDER_FULL, BorderStyle.THIN, color);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(0, rowIndex, (generalValueObjects.size() * 3) + 4, rowIndex + 1), true);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapParameterTambahanUmum.java:465");

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
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Tidak pilih " + "Fakultas");
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
						Integer nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
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

				td = ((Number) (objects[objects.length - 2] == null ? 0 : objects[objects.length - 2])).intValue();
				totalPerempuan += td;
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, td);
				colIndex++;

				td = ((Number) (objects[objects.length - 1] == null ? 0 : objects[objects.length - 1])).intValue();
				total += td;
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, td);

				rowIndex++;

			}

			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(0, rowIndex - jurusans.size(), (generalValueObjects.size() * 3) + 4, rowIndex),
						BookHelper.BORDER_FULL, BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapParameterTambahanUmum.java:561");
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapParameterTambahanUmum.java:584");

			}

			try {

				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(colIndex, rowIndex, (generalValueObjects.size() * 3) + 4, rowIndex), true);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapParameterTambahanUmum.java:592");

			}

		}

		Common.setStyled(sheet);
		spreadsheet.setMaxrows(rowIndex + 1);
		// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

	}
}
