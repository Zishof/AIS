package ais.action.master.epsbed;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyComboitemConfig;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.temporary.IPKMahasiswa;

public class TransaksiAktivitasKuliahMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private Combobox tahunakademik = new Combobox();
	private Combobox jenisSemester = new Combobox();
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private File file;

	public TransaksiAktivitasKuliahMahasiswa() {
		super();
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public TransaksiAktivitasKuliahMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	private void init() throws Exception {

		Common.generateTahunAjaran(tahunakademik);
		// tahunakademik.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// // TODO Auto-generated method stub
		// initSpreadsheet();
		// }
		// });
		jenisSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setValue(Perkuliahan.GANJIL);
		comboitem.setLabel(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setValue(Perkuliahan.GENAP);
		comboitem.setLabel(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		Common.selectComboItem(jenisSemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		// jenisSemester.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// // TODO Auto-generated method stub
		// initSpreadsheet();
		// }
		// });

		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub

				Common.clear(searchjurusan);
				Common.selectComboItem(searchjurusan, null);
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
				// initSpreadsheet();
			}
		});
		// searchjurusan.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// // TODO Auto-generated method stub
		// initSpreadsheet();
		// }
		// });

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
			if (tbmuser.ambilJurusan() != null) {
				Common.selectComboItem(searchjurusan, tbmuser.ambilJurusan());
			}
		}
		// else {
		// Fakultas fakultas = (Fakultas) HibernateUtil.currentSession()
		// .createCriteria(Fakultas.class).add(Restrictions.idEq(1L))
		// .uniqueResult();
		// Jurusan jurusan = (Jurusan) HibernateUtil.currentSession()
		// .createCriteria(Jurusan.class).add(Restrictions.idEq(1L))
		// .uniqueResult();
		// Common.selectComboItem(searchfakultas, fakultas);
		// Common.insertCombo(searchjurusan, new String[]{"nama", "kodeEpsbed"},
		// "jenjang", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
		// Restrictions.eq("fakultas", searchfakultas
		// .getSelectedItem().getValue()));
		// Common.selectComboItem(searchjurusan, jurusan);
		// }

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

		Div div = new Div();
		div.setParent(north);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunakademik);
		tahunakademik.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		row.appendChild(new ais.ui.util.MyLabelConfig("Program Studi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");

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

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Export Epsbed (TRAKM.xls)", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "TRAKM.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/epsbed/TransaksiAktivitasKuliahMahasiswa.java:224");

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
	private void initSpreadsheet() throws Exception {

		if (tahunakademik.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Akademik harus diisi");
			return;
		}

		if (jenisSemester.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Semester harus diisi");
			return;
		}

		Common.clear(center);
		final Jurusan jurusan = searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null || searchjurusan.getSelectedItem().getValue()==null ? null
				: (Jurusan) searchjurusan.getSelectedItem().getValue();

		final PerguruanTinggi perguruanTinggi = (PerguruanTinggi) HibernateUtil.currentSession()
				.createCriteria(PerguruanTinggi.class).setMaxResults(1).uniqueResult();

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/data_" + URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		(file = new File(filename)).createNewFile();
		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				XSSFWorkbook workbook = new XSSFWorkbook();
				XSSFSheet sheet = workbook.createSheet("DATA");
				sheet.setDefaultColumnWidth(20);
				int rowIndex = 0;

				XSSFRow rowhead = sheet.createRow((short) 0);
				rowhead.createCell(0).setCellValue("THSMSTRAK");
				rowhead.createCell(1).setCellValue("KDPTITRAK");
				rowhead.createCell(2).setCellValue("KDJENTRAK");
				rowhead.createCell(3).setCellValue("KDPSTTRAK");
				rowhead.createCell(4).setCellValue("NIMHSTRAK");
				rowhead.createCell(5).setCellValue("SMAWLTRAK");

				rowhead.createCell(6).setCellValue("STMHSTRAK");
				rowhead.createCell(7).setCellValue("MLSEMTRAK");
				rowhead.createCell(8).setCellValue("NLIPSTRAK");

				rowhead.createCell(9).setCellValue("SKSEMTRAK");
				rowhead.createCell(10).setCellValue("NLIPKTRAK");
				rowhead.createCell(11).setCellValue("SKSTTTRAK");

				Session session = HibernateUtil.currentNativeSession();
				List<IPKMahasiswa> mahasiswa = session.createCriteria(IPKMahasiswa.class)
						.add(Restrictions.eq("tahunAkademik", tahunakademik.getSelectedItem().getValue()))

				.add(Restrictions.sqlRestriction("semester % 2 = "
						+ (jenisSemester.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? "1" : "0")))
						.createCriteria("mahasiswa")

				.add(jurusan == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", jurusan))

				.createAlias("jurusan", "jurusan")

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false)).list();

				rowIndex = 1;
				for (IPKMahasiswa ipkmahasiswa : mahasiswa) {

					if (ipkmahasiswa.getMahasiswa() != null) {
						System.out.println("epsbed status mhs : " + ipkmahasiswa.getMahasiswa().getNim());
						label.setValue("Sedang memproses data " + ipkmahasiswa.getMahasiswa().toString() + " ("
								+ Common.numberFormat.get().format(rowIndex * 100.0 / mahasiswa.size()) + " %)");
					}
					XSSFRow row = sheet.createRow(rowIndex);
					row.createCell(0)
							.setCellValue(CommonEpsbed.getTahunSemesterPelaporan(
									(String) tahunakademik.getSelectedItem().getValue(),
									(String) jenisSemester.getSelectedItem().getValue()));
					row.createCell(1)
							.setCellValue(perguruanTinggi == null || perguruanTinggi.getId() == null ? "" : perguruanTinggi.getKodePerguruanTinggi());

					row.createCell(2).setCellValue(ipkmahasiswa.getMahasiswa().getJenjang().getJenjangEpsbed());
					// jurusan tidak ditampilkan untuk matakuliah konversi
					row.createCell(3)
							.setCellValue(ipkmahasiswa.getMahasiswa().getJurusan() == null
									? ipkmahasiswa.getMahasiswa().getJurusan().getKodeEpsbed()
									: ipkmahasiswa.getMahasiswa().getJurusan()

					.getKodeEpsbed());
					row.createCell(4).setCellValue(ipkmahasiswa.getMahasiswa().getNim());
					row.createCell(5).setCellValue("");
					row.createCell(6).setCellValue("");
					row.createCell(7).setCellValue("");
					@SuppressWarnings("resource")
					Formatter format = new Formatter();
					row.createCell(8).setCellValue(
							ipkmahasiswa == null ? "0" : format.format("%.2f", ipkmahasiswa.getIp()).toString());
					row.createCell(9)
							.setCellValue(ipkmahasiswa == null ? "0" : ipkmahasiswa.getSksCurrent().toString());
					format = new Formatter();
					row.createCell(10).setCellValue(
							ipkmahasiswa == null ? "0" : format.format("%.2f", ipkmahasiswa.getIpk()).toString());

					row.createCell(11).setCellValue(ipkmahasiswa == null ? "0" : ipkmahasiswa.getSksTotal().toString());

					rowIndex++;
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

				System.out.println("Your excel file has been generated! " );

				HibernateUtil.closeSession();

				mahasiswa.clear();
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}
}
