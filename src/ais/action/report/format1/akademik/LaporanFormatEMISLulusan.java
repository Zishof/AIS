package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeMap;

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
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanFormatEMISLulusan extends MyWindow {

	private Center center;

	/**
	 *
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox program;

	private Combobox tahunAjaran;
	private Combobox ganjilGenap;

	private Combobox angkatan;
	private Combobox angkatanSampai;
	private Combobox status;

	private Spreadsheet excelku;

	private MyToolbarbuttonConfig search;

	public LaporanFormatEMISLulusan() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Format EMIS Lulusan", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanFormatEMISLulusan(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		program = Common.initPrograms(null);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

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
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");
		// program.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAjaran = Common.generateTahunAjaranDanSemua(tahunAjaran);
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");
		tahunAjaran.setReadonly(true);
		// tahunAjaran.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		ganjilGenap = new Combobox();
		row.appendChild(ganjilGenap);
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		ganjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		ganjilGenap.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		ganjilGenap.appendChild(comboitem);

		Common.selectComboItem(ganjilGenap, Common.getSemesterString());
		// ganjilGenap.addEventListener("onChange", eventListener);
		ganjilGenap.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Lulusan Mulai"));
		angkatan = new Combobox();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = tahun - 20; i <= tahun; i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			angkatan.appendChild(comboitem);
		}

		comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		angkatan.appendChild(comboitem);
		Common.selectComboItem(angkatan, tahun);
		row.appendChild(angkatan);
		angkatan.setWidth("90%");
		angkatan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Lulusan Sampai"));
		angkatanSampai = new Combobox();
		tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = tahun - 20; i <= tahun; i++) {
			comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			angkatanSampai.appendChild(comboitem);
		}

		comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		angkatanSampai.appendChild(comboitem);
		Common.selectComboItem(angkatanSampai, tahun);
		row.appendChild(angkatanSampai);
		angkatanSampai.setWidth("90%");
		angkatanSampai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
		row.appendChild(status = new Combobox());
		Common.insertCombo(status, "nama", "kodeEpsbed", StatusMahasiswa.class);
		status.setSelectedIndex(-1);
		status.setWidth("90%");

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
					Filedownload.save(bout.toByteArray(),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "lulusan.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanFormatEMISLulusan.java:259");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Format EMIS Lulusan", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
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

	private TreeMap<String, HistoryStatusMahasiswa> mahasiswas = null;
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
					mahasiswas = new TreeMap<String, HistoryStatusMahasiswa>();
					List<HistoryStatusMahasiswa> history = session.createCriteria(HistoryStatusMahasiswa.class)

							.add(status.getSelectedItem() == null || status.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: Restrictions.eq("statusMahasiswa", status.getSelectedItem().getValue()))

							.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")

							.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", fakultas, false))

							.add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", jurusan, false))

							.add(program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: Restrictions.eq("mahasiswa.program", program.getSelectedItem().getValue()))

							.add(tahunAjaran.getSelectedItem() == null
									|| tahunAjaran.getSelectedItem().getValue() == null
											? Restrictions.sqlRestriction("true")
											: Restrictions.eq("tahunAkademik",
													tahunAjaran.getSelectedItem().getValue()))

							.add(ganjilGenap.getSelectedItem() == null
									|| ganjilGenap.getSelectedItem().getValue() == null
											? Restrictions.sqlRestriction("true")
											: Restrictions
													.in("semester",
															ganjilGenap.getSelectedItem().getValue().equals(
																	Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))

							.add(Restrictions.in("statusMahasiswa.id", new Long[] { ConstantValues.LULUS.getId() }))

							.add(angkatan.getSelectedItem() == null || angkatan.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("true")
									: Restrictions.ge("mahasiswa.tahunLulus", angkatan.getSelectedItem().getValue()))

							.add(angkatanSampai.getSelectedItem() == null
									|| angkatanSampai.getSelectedItem().getValue() == null
											? Restrictions.sqlRestriction("true")
											: Restrictions.le("mahasiswa.tahunLulus",
													angkatanSampai.getSelectedItem().getValue()))

							.add(Restrictions.isNotNull("mahasiswa.tahunLulus"))

							.addOrder(Order.asc("jurusan.nama")).addOrder(Order.asc("mahasiswa.nim"))

							.list();

					for (HistoryStatusMahasiswa a : history) {
						mahasiswas.put(a.getMahasiswa().getNim(), a);
					}

					System.out.println("Ukuran data =>" + mahasiswas.size());

					datas = new ArrayList<List>();
					int rowIndex = 4;
					for (String nim : mahasiswas.keySet()) {
						rowIndex++;
						List sub = new ArrayList();
						try {

							HistoryStatusMahasiswa historyStatusMahasiswa = mahasiswas.get(nim);
							Mahasiswa mahasiswa = historyStatusMahasiswa.getMahasiswa();
							BiodataMahasiswa biodataMahasiswa = (BiodataMahasiswa) session
									.createCriteria(BiodataMahasiswa.class).add(Restrictions.eq("mahasiswa", mahasiswa))
									.setMaxResults(1).uniqueResult();
							KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa,
									historyStatusMahasiswa.getSemester(), null, null);

							label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
									+ Common.numberFormat.get().format((rowIndex - 1) * 100.0 / mahasiswas.size()) + " %)");

							sub.add(mahasiswa.getNama());
							sub.add(mahasiswa.getNim());
							sub.add(biodataMahasiswa == null ? "" : biodataMahasiswa.getNirm());
							sub.add(biodataMahasiswa == null ? "" : biodataMahasiswa.getNisn());
							sub.add(mahasiswa.getTempatlahir());

							if (mahasiswa.getTanggallahir() != null) {
								Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
								calendar.setTime(mahasiswa.getTanggallahir());
								sub.add(calendar.get(Calendar.DATE));
								sub.add(calendar.get(Calendar.MONTH) + 1);
								sub.add(calendar.get(Calendar.YEAR) + "");
							} else {
								sub.add("");
								sub.add("");
								sub.add("");
							}

							sub.add(mahasiswa.getKelamin() == null ? null
									: mahasiswa.getKelamin().equalsIgnoreCase("Laki-Laki") ? 1 : 0);

							if (mahasiswa.getNegara() != null) {
								sub.add(mahasiswa.getNegara().getKode().equals("ID") ? "1" : "0");
							} else {
								sub.add("");
							}

							if (biodataMahasiswa != null && biodataMahasiswa.getJenisSekolah() != null) {
								sub.add(biodataMahasiswa.getJenisSekolah().getKode());
							} else {
								sub.add("");
							}

							if (mahasiswa.getTanggalMasuk() != null) {
								Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
								calendar.setTime(mahasiswa.getTanggalMasuk());
								sub.add(calendar.get(Calendar.DATE));
								sub.add(calendar.get(Calendar.MONTH) + 1);
								sub.add(calendar.get(Calendar.YEAR) + "");
							} else {
								sub.add("");
								sub.add("");
								sub.add("");
							}

							if (mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getJenjang() != null) {

								sub.add(mahasiswa.getJurusan().getJenjang().getKode());

							} else {
								sub.add("");
							}

							if (mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getFakultas() != null) {
								sub.add(mahasiswa.getJurusan().getFakultas().getNama());
							} else {
								sub.add("");
							}

							if (mahasiswa.getJurusan() != null) {
								sub.add(mahasiswa.getJurusan().getNama());
							} else {
								sub.add("");
							}

							sub.add(krsMahasiswa.getSksk());

							sub.add(mahasiswa.getSemesterLulus());

							sub.add(krsMahasiswa.getIpk());

							sub.add(mahasiswa.getNoIjazah1());

							if (mahasiswa.getTanggalLulus() != null) {
								Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
								calendar.setTime(mahasiswa.getTanggalLulus());
								sub.add(calendar.get(Calendar.DATE));
								sub.add(calendar.get(Calendar.MONTH) + 1);
								sub.add(calendar.get(Calendar.YEAR) + "");
							} else {
								sub.add("");
								sub.add("");
								sub.add("");
							}

							if (biodataMahasiswa != null && biodataMahasiswa.getPendapatanOrtu() != null) {
								sub.add(biodataMahasiswa.getPendapatanOrtu().getKode());
							} else {
								sub.add("");
							}

							if (biodataMahasiswa != null && biodataMahasiswa.getPekerjaanAyah() != null) {
								sub.add(biodataMahasiswa.getPekerjaanAyah().getKode());
							} else {
								sub.add("");
							}

							if (biodataMahasiswa != null && biodataMahasiswa.getPekerjaanIbu() != null) {
								sub.add(biodataMahasiswa.getPekerjaanIbu().getKode());
							} else {
								sub.add("");
							}

							if (biodataMahasiswa != null && biodataMahasiswa.getPendidikanAyah() != null) {
								sub.add(biodataMahasiswa.getPendidikanAyah().getKode());
							} else {
								sub.add("");
							}

							if (biodataMahasiswa != null && biodataMahasiswa.getPendidikanIbu() != null) {
								sub.add(biodataMahasiswa.getPendidikanIbu().getKode());
							} else {
								sub.add("");
							}

							sub.add(mahasiswa.getAlamat());

							if (mahasiswa.getStatusSetelahLulus() != null) {
								sub.add(mahasiswa.getStatusSetelahLulus().getKode());
							} else {
								sub.add("");
							}

							if (mahasiswa.getStatusPekerjaanSetelahLulus() != null) {
								sub.add(mahasiswa.getStatusPekerjaanSetelahLulus().getKode());
							} else {
								sub.add("");
							}

							if (mahasiswa.getStatusDomisiliSetelahLulus() != null) {
								sub.add(mahasiswa.getStatusDomisiliSetelahLulus().getKode());
							} else {
								sub.add("");
							}

							sub.add(biodataMahasiswa == null ? 0 : biodataMahasiswa.getPunyaSkpi() ? 1 : 0);
							sub.add(biodataMahasiswa == null ? 0
									: biodataMahasiswa.getPunyaSertifikatBahasaInggris() ? 1 : 0);
							sub.add(biodataMahasiswa == null ? 0
									: biodataMahasiswa.getPunyaSertifikatBahasaArab() ? 1 : 0);

							System.out.println("sub =>" + sub);

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Format EMIS Lulusan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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
					if (mahasiswas == null || datas == null) {
						return;
					}
					System.out.println("mahasiswa = " + mahasiswas.size() + ", data = " + datas.size());
					ais.action.report.helper.LoadingReportUtil.showBusy(label);
					if (datas.size() == mahasiswas.size()) {
						ais.action.report.helper.LoadingReportUtil.clearBusy();
						excelku = new ais.ui.util.MySpreadsheet();
						center.appendChild(excelku);
						excelku.setSrc("../../WEB-INF/lulusan.xlsx");
						excelku.setStyle("border:1px solid #8AA3C1");
						excelku.setHeight("100%");
						excelku.setWidth("100%");
						excelku.setMaxrows(datas.size() + 5);
						excelku.setMaxcolumns(37);

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
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Format EMIS Lulusan", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}
}
