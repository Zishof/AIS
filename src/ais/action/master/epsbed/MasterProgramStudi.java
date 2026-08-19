package ais.action.master.epsbed;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyComboitemConfig;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.JenjangProgramStudi;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;

public class MasterProgramStudi extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();
	private Combobox searchfakultas = new Combobox();
	private Combobox tahunakademik = new Combobox();
	private Combobox jenisSemester = new Combobox();

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	public MasterProgramStudi() {
		super();
		try {

			Common.insertCombo(searchfakultas, new String[] { "nama", "kode" },
					Fakultas.class);
			init();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public MasterProgramStudi(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	private void init() throws Exception {

		Common.generateTahunAjaran(tahunakademik);
		tahunakademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				initSpreadsheet();
			}
		});
		jenisSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setValue(Perkuliahan.GANJIL);
		comboitem.setLabel(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setValue(Perkuliahan.GENAP);
		comboitem.setLabel(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		Common.selectComboItem(jenisSemester,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL
						: Perkuliahan.GENAP);
		jenisSemester.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				initSpreadsheet();
			}
		});

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		// FIX toolbar/tombol tidak tampil: pada ZK5 region North memakai tinggi bawaan
		// (+-100px); dengan flex=true isinya diregangkan ke tinggi tersebut sehingga
		// Toolbar yang diletakkan DI BAWAH grid filter ikut terpotong. Disamakan dengan
		// layar sejenis yang sudah benar (DownloadMahasiswa, DownloadKrs, DownloadNilai):
		// flex dimatikan + tinggi eksplisit. Autoscroll sebagai pengaman bila isi bertambah.
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid grid = new MyGrid();grid.setWidth("100%");
		grid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunakademik);tahunakademik.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		row.appendChild(jenisSemester);jenisSemester.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				initSpreadsheet();
			}
		});

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig search = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				initSpreadsheet();
			}
		});
		search.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Export Epsbed (MSPST.xls)",
				"/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					spreadsheet.getBook().write(bout);
					bout.close();
					Filedownload.save(bout.toByteArray(),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "MSPST.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/epsbed/MasterProgramStudi.java:179");

				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		initSpreadsheet();
	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() {

		Common.clear(center);

		final Label label = Common.displayLoadBar(this);

		final List<Jurusan> jenjangPS = HibernateUtil
				.currentSession()
				.createCriteria(Jurusan.class)
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? Restrictions
						.sqlRestriction("1=1") : CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)).list();

		final PerguruanTinggi perguruanTinggi = (PerguruanTinggi) HibernateUtil
				.currentSession().createCriteria(PerguruanTinggi.class)
				.setMaxResults(1).uniqueResult();

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(30);
		spreadsheet.setMaxrows(jenjangPS.size() + 1);

		final Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(30);

		int rowIndex = 0;
		int colIndex = 0;

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "KDPTIMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "KDFAKMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "KDJENMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "KDPSTMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "NMPSTMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "TGAWLMSPST");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "SMAWLMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "STATUMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "MLSEMMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, "SKSTTMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, "EMAILMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, "NOMSKMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12, "TGLSKMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, "TGLAKMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, "NOMBAMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15, "TGLBAMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, "TGLABMSPST");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, "KDSTAMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, "KDFREMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 19, "KDPELMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 20, "NOKPSMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 21, "TELPSMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 22, "TELPOMSPST");

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 23, "FAKSIMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 24, "NMOPRMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 25, "TELPRMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 26, "LISAHMSPST");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 27, "KDPS1MSPST");

		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1,
						rowIndex), true);

		rowIndex++;
		// System.out.println("epsbed jenjang prodi : " + jenjangPS.size());
		//
		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// try {
		// Thread.sleep(500);
		// } catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/master/epsbed/MasterProgramStudi.java:269");
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }
		rowIndex = 1;
		for (Jurusan jurusan : jenjangPS) {

			JenjangProgramStudi jenjangProdi = (JenjangProgramStudi) HibernateUtil
					.currentSession().createCriteria(JenjangProgramStudi.class)
					.add(Restrictions.eq("jurusan", jurusan)).setMaxResults(1)
					.uniqueResult();

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, perguruanTinggi == null || perguruanTinggi.getId() == null ? ""
					: perguruanTinggi.getKodePerguruanTinggi());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, jurusan.getFakultas()
					.getKode());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, jurusan.getJenjang()
					.getJenjangEpsbed());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, jurusan.getKodeEpsbed());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, jurusan.getNama());
			ais.ui.util.EcampusUtil.setCellValue(
					sheet,
					rowIndex,
					5,
					jenjangProdi == null ? "" : jenjangProdi
							.getTanggalBerdiri() == null ? ""
							: CommonEpsbed.dateFormatEpsbed.get().format(jenjangProdi
									.getTanggalBerdiri()));
			ais.ui.util.EcampusUtil.setCellValue(
					sheet,
					rowIndex,
					6,
					tahunakademik.getSelectedItem() == null ? "" : CommonEpsbed
							.getTahunSemesterPelaporan((String) tahunakademik
									.getSelectedItem().getValue(),
									(String) jenisSemester.getSelectedItem()
											.getValue()));

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, jenjangProdi == null ? ""
					: jenjangProdi.getEpsbedStatus() == null ? ""
							: jenjangProdi.getEpsbedStatus().getKode());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, jenjangProdi == null ? ""
					: jenjangProdi.getEpsbedTahunHapus());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, jenjangProdi == null ? ""
					: jenjangProdi.getSksLulus());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, jenjangProdi == null ? ""
					: jenjangProdi.getEmail());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, jenjangProdi == null ? ""
					: jenjangProdi.getNoSKDikti());
			ais.ui.util.EcampusUtil.setCellValue(
					sheet,
					rowIndex,
					12,
					jenjangProdi == null ? "" : jenjangProdi
							.getTglMulaiSKDikti() == null ? ""
							: CommonEpsbed.dateFormatEpsbed.get().format(jenjangProdi
									.getTglMulaiSKDikti()));
			ais.ui.util.EcampusUtil.setCellValue(
					sheet,
					rowIndex,
					13,
					jenjangProdi == null ? "" : jenjangProdi
							.getTglAkhirSKDikti() == null ? ""
							: CommonEpsbed.dateFormatEpsbed.get().format(jenjangProdi
									.getTglAkhirSKDikti()));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, jenjangProdi == null ? ""
					: jenjangProdi.getNoSKAkreditasi());
			ais.ui.util.EcampusUtil.setCellValue(
					sheet,
					rowIndex,
					15,
					jenjangProdi == null ? "" : jenjangProdi
							.getTglMulaiSKAkreditasi() == null ? ""
							: CommonEpsbed.dateFormatEpsbed.get().format(jenjangProdi
									.getTglMulaiSKAkreditasi()));
			ais.ui.util.EcampusUtil.setCellValue(
					sheet,
					rowIndex,
					16,
					jenjangProdi == null ? "" : jenjangProdi
							.getTglAkhirSKAkreditasi() == null ? ""
							: CommonEpsbed.dateFormatEpsbed.get().format(jenjangProdi
									.getTglAkhirSKAkreditasi()));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, jenjangProdi == null ? ""
					: jenjangProdi.getEpsbedStatusAkreditasi() == null ? ""
							: jenjangProdi.getEpsbedStatusAkreditasi()
									.getKode());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, jenjangProdi == null ? ""
					: jenjangProdi.getEpsbedFrekuensiKurikulum() == null ? ""
							: jenjangProdi.getEpsbedFrekuensiKurikulum()
									.getKode());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 19, jenjangProdi == null ? ""
					: jenjangProdi.getEpsbedPelaksanaanKurikulum() == null ? ""
							: jenjangProdi.getEpsbedPelaksanaanKurikulum()
									.getKode());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 20, jenjangProdi == null ? ""
					: jenjangProdi.getNidnKaPS());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 21, jenjangProdi == null ? ""
					: jenjangProdi.getTelpKaPS());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 22, jenjangProdi == null ? ""
					: jenjangProdi.getTelpPS());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 23, jenjangProdi == null ? ""
					: jenjangProdi.getFaxPS());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 24, jenjangProdi == null ? ""
					: jenjangProdi.getNamaOperator());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 25, jenjangProdi == null ? ""
					: jenjangProdi.getHpOperator());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 26, "");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 27, "");
			rowIndex++;
			// spreadsheet.setRowfreeze(rowIndex);
		}

		jenjangPS.clear();
		label.setValue("");
		// }
		// }).start();

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
	}
}
