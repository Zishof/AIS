package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataDosen;
import ais.database.model.Dosen;
import ais.database.model.Perkuliahan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanFormatEMISRiwayatDosen extends MyWindow implements DataCriteria {

	private Center center;

	/**
	 *
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Combobox fakultas;
	private Combobox jurusan;

	private Combobox tahunAjaran;
	private Combobox ganjilGenap;

	private Spreadsheet excelku;

	private MyToolbarbuttonConfig search;

	private Combobox program;

	public LaporanFormatEMISRiwayatDosen() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Format EMIS Riwayat Dosen", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanFormatEMISRiwayatDosen(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

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

		program = Common.initPrograms(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");

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
					Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "riwayat_dosen.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanFormatEMISRiwayatDosen.java:208");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Format EMIS Riwayat Dosen", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});

				}
			}
		});
		print.setParent(toolbar);

		String[] contents = new String[] { "id", "code", "nidn", "nama", "lockId" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				Criteria criteria = LaporanFormatEMISRiwayatDosen.this.initCriteria(true);
				return criteria;
			}
		}, contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = Common.uploadData(new DataSearchDefault() {

			@Override
			public void onSearchDefault(Event event) {
				onCetak(null);
			}
		}, Dosen.class, contents);
		toolbar.appendChild(upload);

		// onCetak(null);
	}

	@SuppressWarnings("rawtypes")
	private List<List> datas = null;

	private List<Dosen> dosens;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetak(Event event) {

		try {

			Common.clear(center);

			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

			dosens = initCriteria(true).list();

			new Thread(new Runnable() {

				@Override
				public void run() {

					System.out.println("Ukuran data =>" + dosens.size());
					Session session = ais.action.report.Report.openNativeSession();
					datas = new ArrayList<List>();
					for (int rowIndex = 3; rowIndex < dosens.size() + 3; rowIndex++) {

						try {

							Dosen dosen = dosens.get(rowIndex - 3);
							BiodataDosen biodataDosen = dosen.ambilBiodata();

							label.setValue("Sedang memproses data " + dosen.toString() + " ("
									+ Common.numberFormat.get().format((rowIndex - 1) * 100.0 / dosens.size()) + " %)");

							Criterion criterion = dosen == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.eq("dosen1", dosen),
											Restrictions.eq("dosen2", dosen));

							criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
							criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));

							List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(criterion)

									.add(ganjilGenap.getSelectedItem() == null
											|| ganjilGenap.getSelectedItem().getValue() == null
													? Restrictions.sqlRestriction("1=1")
													: Restrictions.sqlRestriction("this_.semester % 2 = "
															+ (ganjilGenap.getSelectedItem().getValue()
																	.equals(Perkuliahan.GANJIL) ? "1" : "0")))

									.add(program.getSelectedItem() == null
											|| program.getSelectedItem().getValue() == null
													? Restrictions.sqlRestriction("1=1")
													: Restrictions.eq("program", program.getSelectedItem().getValue()))

									.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))

									.add(tahunAjaran.getSelectedItem() == null
											|| tahunAjaran.getSelectedItem().getValue() == null
													? Restrictions.sqlRestriction("1=1")
													: Restrictions.eq("tahunAjaran",
															tahunAjaran.getSelectedItem().getValue()))

									.createCriteria("jurusan", Criteria.INNER_JOIN)

									.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false))

									.list();

							for (Perkuliahan perkuliahan : perkuliahans) {
								List sub = new ArrayList();
								sub.add(dosen.getLockId());
								sub.add(dosen.getCode());
								sub.add(dosen.getNidn());
								sub.add(biodataDosen == null ? "" : biodataDosen.getNoIdentitas());
								sub.add(dosen.getNama());

								sub.add(dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama());
								sub.add(dosen.getJurusan() == null ? ""
										: dosen.getJurusan().getId().equals(perkuliahan.getJurusan().getId()) ? "1"
												: "2");
								sub.add(dosen.getJurusan() == null ? ""
										: perkuliahan.getJurusan().getJenjang().getKode());

								sub.add(1);

								sub.add(perkuliahan.getMatakuliah().getNama());
								sub.add(perkuliahan.getMatakuliah().getSks());
								try {
									sub.add((perkuliahan.getWaktuSelesaiD() - perkuliahan.getWaktuMulaiD()) * 60.0);
								} catch (Exception e) {
									sub.add("");
								}

								try {
									for (int i = 1; i <= Common.haris.length; i++) {
										if (perkuliahan.getHari().equalsIgnoreCase(Common.haris[i - 1])) {
											sub.add(i);
											break;
										}
									}
								} catch (Exception e) {
									sub.add("");
								}

								sub.add(perkuliahan.getWaktuMulai() == null ? ""
										: org.apache.commons.lang3.StringUtils.replace(perkuliahan.getWaktuMulai(), ".", ":"));
								System.out.println("sub =>" + sub);
								datas.add(sub);
							}

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e); 
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Format EMIS Riwayat Dosen", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
									new String[] {
										"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
										"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
										"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
									});
						}

					}
					System.out.println("Ukuran datas =>" + datas.size());
					ais.action.report.Report.closeCurrentSessionQuietly();

					ais.action.report.helper.LoadingReportUtil.selesai(label);

				}
			}).start();

			final Timer timer = new Timer(1000);
			timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			timer.setRepeats(true);
			ais.action.report.helper.LoadingReportUtil.showBusy(label);
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					System.out.println("dosen = " + dosens.size() + ", data = " + datas.size());
					ais.action.report.helper.LoadingReportUtil.showBusy(label);
					if (ais.action.report.helper.LoadingReportUtil.isSelesai(label)) {
						ais.action.report.helper.LoadingReportUtil.clearBusy();
						excelku = new ais.ui.util.MySpreadsheet();
						center.appendChild(excelku);
						excelku.setSrc("../../WEB-INF/riwayat_dosen.xlsx");
						excelku.setStyle("border:1px solid #8AA3C1");
						excelku.setHeight("100%");
						excelku.setWidth("100%");
						excelku.setMaxrows(datas.size() + 3);
						excelku.setMaxcolumns(20);

						Worksheet sheet = excelku.getSheet(0);

						for (int rowIndex = 2; rowIndex < datas.size() + 2; rowIndex++) {
							List sub = datas.get(rowIndex - 2);
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
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Format EMIS Riwayat Dosen", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Dosen.class)

				.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false))

				.add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false));



		if (order)
			criteria.addOrder(Order.asc("nidn"));
		return criteria;
	}
}
