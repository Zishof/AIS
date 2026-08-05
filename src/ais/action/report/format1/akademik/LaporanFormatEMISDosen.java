package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataDosen;
import ais.database.model.Dosen;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanFormatEMISDosen extends MyWindow {

	private Center center;

	/**
	 *
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Combobox fakultas;
	private Combobox jurusan;

	private Spreadsheet excelku;

	private MyToolbarbuttonConfig search;

	public LaporanFormatEMISDosen() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Format EMIS Dosen", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanFormatEMISDosen(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Data Dosen");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Tugas Dosen");
		tab2.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().size() == 0) {
					LaporanFormatEMISRiwayatDosen laporanKHS = new LaporanFormatEMISRiwayatDosen();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel2);
				}
			}
		});

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel1);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("300px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// EventListener eventListener = new EventListener() {
		//
		// @Override
		// public void onEvent(Event event) throws Exception {
		// onCetak(event);
		//
		// }
		// };

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		// fakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		// jurusan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(row);

		search = new MyToolbarbuttonConfig("Proses Data", "/img/svg/search.svg");
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(null);
			}
		});
		search.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Cetak", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					excelku.getBook().write(bout);
					bout.close();
					Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "EMIS.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanFormatEMISDosen.java:199");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Format EMIS Dosen", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});

				}
			}
		});
		print.setParent(toolbar);

		// onCetak(null);
	}

	private List<BiodataDosen> biodataDosens = null;
	@SuppressWarnings("rawtypes")
	private List<List> datas = null;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetak(Event event) {

		try {

			Common.clear(center);

			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

			new Thread(new Runnable() {

				@Override
				public void run() {

					Session session = ais.action.report.Report.openNativeSession();
					biodataDosens = session.createCriteria(BiodataDosen.class)

							.createAlias("dosen", "dosen").createAlias("dosen.jurusan", "jurusan")

							.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", fakultas, false))

							.add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: CommonSearchFilterHelper.eqSelectedWithId("dosen.jurusan", jurusan, false))

							.addOrder(Order.asc("dosen.mycode")).addOrder(Order.asc("dosen.nidn"))

							.list();
					System.out.println("Ukuran data =>" + biodataDosens.size());

					datas = new ArrayList<List>();
					for (int rowIndex = 5; rowIndex < biodataDosens.size() + 5; rowIndex++) {
						List sub = new ArrayList();
						try {

							BiodataDosen biodataDosen = biodataDosens.get(rowIndex - 5);
							Dosen dosen = biodataDosen.getDosen();

							label.setValue("Sedang memproses data " + dosen.toString() + " ("
									+ Common.numberFormat.get().format((rowIndex - 1) * 100.0 / biodataDosens.size())
									+ " %)");

							sub.add(dosen.getMycode());
							sub.add(dosen.getNidn());
							sub.add(dosen.getNama());
							sub.add(dosen.getGelarDepan());
							sub.add(dosen.getGelarBelakang());

							sub.add(dosen.getKelamin() == null ? null
									: dosen.getKelamin().equalsIgnoreCase("Laki-Laki") ? 1 : 0);

							sub.add(dosen.getTempatlahir());

							if (dosen.getTanggallahir() != null) {
								Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
								calendar.setTime(dosen.getTanggallahir());
								sub.add(calendar.get(Calendar.DATE));
								sub.add(calendar.get(Calendar.MONTH) + 1);
								sub.add(calendar.get(Calendar.YEAR));
							} else {
								sub.add("");
								sub.add("");
								sub.add("");
							}

							sub.add(dosen.getKtp());
							sub.add(biodataDosen.getNamaIbu());
							sub.add(dosen.getStatusKepegawaian() != null
									&& dosen.getStatusKepegawaian().getFeeder() != null
									&& (dosen.getStatusKepegawaian().getFeeder().equalsIgnoreCase("1")
											|| dosen.getStatusKepegawaian().getFeeder().equalsIgnoreCase("2")
											|| dosen.getStatusKepegawaian().getFeeder().equalsIgnoreCase("3")) ? "1"
													: "2");

							if (dosen.getGolonganPegawai() != null) {

								if (dosen.getGolonganPegawai().getNama().toLowerCase().startsWith("I/")
										|| dosen.getGolonganPegawai().getNama().toLowerCase().startsWith("II/")) {
									sub.add("01");
								} else if (dosen.getGolonganPegawai().getNama().equalsIgnoreCase("III/a")) {
									sub.add("02");
								} else if (dosen.getGolonganPegawai().getNama().equalsIgnoreCase("III/b")) {
									sub.add("03");
								} else if (dosen.getGolonganPegawai().getNama().equalsIgnoreCase("III/c")) {
									sub.add("04");
								} else if (dosen.getGolonganPegawai().getNama().equalsIgnoreCase("III/d")) {
									sub.add("05");
								} else if (dosen.getGolonganPegawai().getNama().equalsIgnoreCase("IV/a")) {
									sub.add("06");
								} else if (dosen.getGolonganPegawai().getNama().equalsIgnoreCase("IV/b")) {
									sub.add("07");
								} else if (dosen.getGolonganPegawai().getNama().equalsIgnoreCase("IV/c")) {
									sub.add("08");
								} else if (dosen.getGolonganPegawai().getNama().equalsIgnoreCase("IV/d")) {
									sub.add("09");
								} else if (dosen.getGolonganPegawai().getNama().equalsIgnoreCase("IV/e")) {
									sub.add("10");
								}

							} else {
								sub.add("");
							}

							if (dosen.getJabatanFungsionalDosen() != null) {
								sub.add(dosen.getJabatanFungsionalDosen().getFeeder());
							} else {
								sub.add("");

							}

							sub.add(dosen.getSkAngkat());

							if (dosen.getTmtSkAngkat() != null) {
								Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
								calendar.setTime(dosen.getTmtSkAngkat());
								sub.add(calendar.get(Calendar.DATE));
								sub.add(calendar.get(Calendar.MONTH) + 1);
								sub.add(calendar.get(Calendar.YEAR));
							} else {
								sub.add("");
								sub.add("");
								sub.add("");
							}

							sub.add(dosen.getSkCpns());

							if (dosen.getTmtPns() != null) {
								Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
								calendar.setTime(dosen.getTmtPns());
								sub.add(calendar.get(Calendar.DATE));
								sub.add(calendar.get(Calendar.MONTH) + 1);
								sub.add(calendar.get(Calendar.YEAR));
							} else {
								sub.add("");
								sub.add("");
								sub.add("");
							}

							// sub.add(biodataDosen.getStatusDosen() == null ?
							// null
							// : (biodataDosen.getStatusDosen().getId()
							// .equals(ConstantValues.AKTIF
							// .getId())) ? 1
							// : (biodataDosen
							// .getStatusDosen()
							// .getId()
							// .equals(ConstantValues.CUTI
							// .getId()) ? 2 : 3));
							//
							// if (biodataDosen.getTanggalStatus() != null) {
							// Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							// calendar.setTime(biodataDosen
							// .getTanggalStatus());
							// sub.add(calendar.get(Calendar.DATE));
							// sub.add(calendar.get(Calendar.MONTH) + 1);
							// sub.add(calendar.get(Calendar.YEAR));
							// } else {
							// sub.add("");
							// sub.add("");
							// sub.add("");
							// }
							//
							// if (dosen.getJurusan() != null
							// && dosen.getJurusan().getJenjang() != null) {
							//
							// sub.add(dosen.getJurusan().getJenjang()
							// .getKode());
							//
							// } else {
							// sub.add("");
							// }
							//
							// if (dosen.getJurusan() != null
							// && dosen.getJurusan().getFakultas() != null) {
							// sub.add(dosen.getJurusan().getFakultas()
							// .getNama());
							// } else {
							// sub.add("");
							// }
							//
							// if (dosen.getJurusan() != null) {
							// sub.add(dosen.getJurusan().getNama());
							// } else {
							// sub.add("");
							// }
							//
							// sub.add(biodataDosen.getSemester());
							//
							// if (biodataDosen != null
							// && biodataDosen.getJenisSekolah() != null) {
							// sub.add(biodataDosen.getJenisSekolah()
							// .getKode());
							// } else {
							// sub.add("");
							// }
							//
							// sub.add(Common.hitungIPK(dosen));
							//
							// if (biodataDosen != null
							// && biodataDosen.getPendapatanOrtu() != null) {
							// sub.add(biodataDosen.getPendapatanOrtu()
							// .getKode());
							// } else {
							// sub.add("");
							// }
							//
							// if (biodataDosen != null
							// && biodataDosen.getPekerjaanAyah() != null) {
							// sub.add(biodataDosen.getPekerjaanAyah()
							// .getKode());
							// } else {
							// sub.add("");
							// }
							//
							// if (biodataDosen != null
							// && biodataDosen.getPekerjaanIbu() != null) {
							// sub.add(biodataDosen.getPekerjaanIbu()
							// .getKode());
							// } else {
							// sub.add("");
							// }
							//
							// if (biodataDosen != null
							// && biodataDosen.getPendidikanAyah() != null) {
							// sub.add(biodataDosen.getPendidikanAyah()
							// .getKode());
							// } else {
							// sub.add("");
							// }
							//
							// if (biodataDosen != null
							// && biodataDosen.getPendidikanIbu() != null) {
							// sub.add(biodataDosen.getPendidikanIbu()
							// .getKode());
							// } else {
							// sub.add("");
							// }
							//
							// sub.add(dosen.getBeasiswaDosenMiskin() == null ?
							// "0"
							// : dosen.getBeasiswaDosenMiskin().getKode());
							//
							// sub.add(dosen.getBeasiswaBidikMisi() == null ?
							// "0"
							// : dosen.getBeasiswaBidikMisi().getKode());
							//
							// sub.add(dosen.getBeasiswaLain() == null ? "0"
							// : dosen.getBeasiswaLain().getKode());
							//
							// sub.add(dosen.getAlamat());
							// if (biodataDosen != null
							// && biodataDosen.getKota() != null) {
							// sub.add(biodataDosen.getKota().getNama());
							// } else {
							// sub.add("");
							// }

							if (biodataDosen != null && biodataDosen.getPropinsi() != null) {
								sub.add(biodataDosen.getPropinsi().getKode());
								sub.add(biodataDosen.getPropinsi().getNama());
							} else {
								sub.add("");
								sub.add("");
							}

							System.out.println("sub =>" + sub);

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e); 
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Format EMIS Dosen", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
									new String[] {
										"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
										"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
										"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
									});
						}
						datas.add(sub);
					}
					System.out.println("Ukuran datas =>" + datas.size());

				}
			}).start();

			final Timer timer = new Timer(1000);
			timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			timer.setRepeats(true);
			ais.action.report.helper.LoadingReportUtil.showBusy(label);
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					System.out.println("dosen = " + biodataDosens.size() + ", data = " + datas.size());
					ais.action.report.helper.LoadingReportUtil.showBusy(label);
					if (datas.size() == biodataDosens.size()) {
						ais.action.report.helper.LoadingReportUtil.clearBusy();
						excelku = new ais.ui.util.MySpreadsheet();
						center.appendChild(excelku);
						excelku.setSrc("../../WEB-INF/emis_dosen.xlsx");
						excelku.setStyle("border:1px solid #8AA3C1");
						excelku.setHeight("100%");
						excelku.setWidth("100%");
						excelku.setMaxrows(datas.size() + 5);
						excelku.setMaxcolumns(33);
						excelku.setColumnfreeze(excelku.getMaxcolumns());

						Worksheet sheet = excelku.getSheet(0);

						for (int rowIndex = 4; rowIndex < datas.size() + 4; rowIndex++) {
							List sub = datas.get(rowIndex - 4);
							for (int colIndex = 0; colIndex < sub.size(); colIndex++) {
								Object d = sub.get(colIndex);
								if (d != null && d instanceof Integer) {
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, (Integer) d);
								} else if (d != null && d instanceof Double) {
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, (Double) d);
								} else {
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex,
											d == null ? "" : d.toString());
								}
							}

						}
						// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Cetak diklik.
						ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(excelku);
						ais.action.report.helper.LoadingReportUtil.stopAndDetach(timer);
					}

				}
			});
			timer.start();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Format EMIS Dosen", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}
}
