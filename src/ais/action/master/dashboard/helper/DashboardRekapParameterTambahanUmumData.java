package ais.action.master.dashboard.helper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
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
import ais.database.model.CommonVO;
import ais.database.model.Fakultas;
import ais.database.model.GrupChecklistPenilaianUmum;
import ais.database.model.IsiAngketParameterUmum;
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

public class DashboardRekapParameterTambahanUmumData extends MyWindow {

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

	public DashboardRekapParameterTambahanUmumData() {
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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

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
						Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "data.xlsx");
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
							.add(Restrictions.eq("grupChecklistPenilaianUmum", grupChecklistPenilaianUmum))

							.list();
					Collections.sort(parameterTambahanAngketUmums);

					row = new MyFormRow();
					row.setParent(rowsParams);
					row.appendChild(new MyLabelBold(grupChecklistPenilaianUmum.getIsi()));

					for (ParameterTambahanAngketUmum parameterTambahanAngketUmum : parameterTambahanAngketUmums) {
						row = new MyFormRow();
						row.setParent(rowsParams);
						MyCheckboxConfig checkboxConfig = new MyCheckboxConfig(
								parameterTambahanAngketUmum.getParameterTambahan().getLabelInputan());
						checkboxConfig.setChecked(true);
						checkboxConfig.setAttribute("parameterTambahan",
								parameterTambahanAngketUmum.getParameterTambahan());
						checkboxConfig.setAttribute("grupChecklistPenilaianUmum", grupChecklistPenilaianUmum);
						checkboxConfig.setParent(row);
						kolom.add(checkboxConfig);
					}
				}
			}
		};

		eventListener.onEvent(null);

		searchtahunakademik.addEventListener("onChange", eventListener);
		searchsemester.addEventListener("onChange", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		initSpreadsheet();

	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() {

		final Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		final Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		final List<List<Object>> jurusansSemua = new ArrayList<List<Object>>();

		final Label label = Common.displayLoadBar(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);
				if (!jurusansSemua.isEmpty()) {
					spreadsheet = new ais.ui.util.MySpreadsheet();
	Common.clear(center);spreadsheet.setParent(center);
					spreadsheet.setWidth("100%");
					spreadsheet.setHeight("100%");
					spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
					spreadsheet.setMaxcolumns(jurusansSemua.get(0).size() + 1);
					spreadsheet.setMaxrows(jurusansSemua.size() + 1);

					Worksheet sheet = spreadsheet.getSelectedSheet();
					sheet.setDefaultColumnWidth(40);
					Utils.setColumnWidth(sheet, 0, 20);
					Utils.setColumnWidth(sheet, 1, 60);
					Utils.setColumnWidth(sheet, 2, 100);
					Utils.setColumnWidth(sheet, 3, 80);

					int rowIndex = 0;
					for (List<Object> objects : jurusansSemua) {
						int rowIndexData = 0;
						for (Object object : objects) {
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, rowIndexData, object);
							rowIndexData++;
						}
						rowIndex++;
					}

					// Excel mentah -> grid ringan (Book tetap hidup untuk tombol Download). Pola B PratinjauXlsxHelper.
					ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
				}

			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				List<Object> namadata = new ArrayList<Object>();
				namadata.add("No.");
				namadata.add("NIM/Kode");
				namadata.add("Nama");
				namadata.add("Prodi");

				for (MyCheckboxConfig checkboxConfig : kolom) {
					if (checkboxConfig.isChecked()) {
						namadata.add(checkboxConfig.getLabel());
					}
				}
				jurusansSemua.add(namadata);

				Session session = HibernateUtil.currentNativeSession();
				List<Long> isiAngketParameterUmumsid = session.createCriteria(IsiAngketParameterUmum.class)
						.setProjection(Projections.property("id"))
						.createAlias("jadwalChecklistPenilaianUmum", "jadwalChecklistPenilaianUmum")
						.add(Restrictions.eq("jadwalChecklistPenilaianUmum.tahunAkademik",
								searchtahunakademik.getSelectedItem().getValue()))
						.add(Restrictions.eq("jadwalChecklistPenilaianUmum.semester",
								searchsemester.getSelectedItem().getValue()))
						.addOrder(Order.asc("id")).list();

				int size = isiAngketParameterUmumsid.size();
				int index = 0;
				for (Long idP : isiAngketParameterUmumsid) {
					IsiAngketParameterUmum isiAngketParameterUmum = (IsiAngketParameterUmum) session
							.createCriteria(IsiAngketParameterUmum.class).add(Restrictions.idEq(idP)).uniqueResult();
					if (isiAngketParameterUmum != null) {
						index++;

						if (jurusan != null) {
							if (isiAngketParameterUmum.getMahasiswa() != null
									&& (isiAngketParameterUmum.getMahasiswa().getJurusan() == null
											|| !isiAngketParameterUmum.getMahasiswa().getJurusan().getId()
													.equals(jurusan.getId()))) {
								continue;
							}
							if (isiAngketParameterUmum.getDosen() != null
									&& (isiAngketParameterUmum.getDosen().getJurusan() == null
											|| !isiAngketParameterUmum.getDosen().getJurusan().getId()
													.equals(jurusan.getId()))) {
								continue;
							}
							if (isiAngketParameterUmum.getTbmuser() != null
									&& (isiAngketParameterUmum.getTbmuser().ambilJurusan() == null
											|| !isiAngketParameterUmum.getTbmuser().ambilJurusan().getId()
													.equals(jurusan.getId()))) {
								continue;
							}
						}

						if (fakultas != null) {
							if (isiAngketParameterUmum.getMahasiswa() != null
									&& (isiAngketParameterUmum.getMahasiswa().getJurusan() == null
											|| !isiAngketParameterUmum.getMahasiswa().getJurusan().getFakultas().getId()
													.equals(fakultas.getId()))) {
								continue;
							}
							if (isiAngketParameterUmum.getDosen() != null
									&& (isiAngketParameterUmum.getDosen().getFakultas() == null
											|| !isiAngketParameterUmum.getDosen().getFakultas().getId()
													.equals(fakultas.getId()))) {
								continue;
							}
							if (isiAngketParameterUmum.getTbmuser() != null
									&& (isiAngketParameterUmum.getTbmuser().ambilFakultas() == null
											|| !isiAngketParameterUmum.getTbmuser().ambilFakultas().getId()
													.equals(fakultas.getId()))) {
								continue;
							}
						}

						List<Object> objects = new ArrayList<Object>();

						objects.add(index);

						if (isiAngketParameterUmum.getMahasiswa() != null) {
							objects.add(isiAngketParameterUmum.getMahasiswa().getNim());
							objects.add(isiAngketParameterUmum.getMahasiswa().getNama());
							objects.add(isiAngketParameterUmum.getMahasiswa().getJurusan() == null ? ""
									: isiAngketParameterUmum.getMahasiswa().getJurusan().getNama());
						} else if (isiAngketParameterUmum.getDosen() != null) {
							objects.add(isiAngketParameterUmum.getDosen().getNidn());
							objects.add(isiAngketParameterUmum.getDosen().getNama());
							objects.add(isiAngketParameterUmum.getDosen().getJurusan() == null ? ""
									: isiAngketParameterUmum.getDosen().getJurusan().getNama());
						} else if (isiAngketParameterUmum.getTbmuser() != null) {
							objects.add(isiAngketParameterUmum.getTbmuser().getUserId());
							objects.add(isiAngketParameterUmum.getTbmuser().getUserNama());
							objects.add(isiAngketParameterUmum.getTbmuser().ambilJurusan() == null ? ""
									: isiAngketParameterUmum.getTbmuser().ambilJurusan().getNama());
						} else {
							objects.add("");
							objects.add("");
							objects.add("");
						}

						label.setValue("Memproses data \"" + objects.toString() + "\" ("
								+ Common.numberFormat.get().format((index * 100.0) / size) + " %)");

						boolean adaSemua = false;
						List<CommonVO> hasil = isiAngketParameterUmum.ambilDataParameterTambahan();
						for (MyCheckboxConfig checkboxConfig : kolom) {
							if (checkboxConfig.isChecked()) {
								ParameterTambahan parameterTambahan = (ParameterTambahan) checkboxConfig
										.getAttribute("parameterTambahan");
								boolean ada = false;
								for (CommonVO vo : hasil) {
									if (vo.getId() != null && vo.getId().equals(parameterTambahan.getId().toString())) {
										objects.add(vo.getName1()
												+ (vo.getName2() != null && !vo.getName2().trim().isEmpty()
														? ", link : " + vo.getName2()
														: ""));
										adaSemua = true;
										ada = true;
										break;
									}
								}
								if (!ada) {
									objects.add("");
								}
							}
						}
						hasil = null;
						if (adaSemua) {
							jurusansSemua.add(objects);
						}

					}
					isiAngketParameterUmum = null;
				}
				isiAngketParameterUmumsid = null;
				label.setValue("");
				HibernateUtil.closeSession();
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}
}
