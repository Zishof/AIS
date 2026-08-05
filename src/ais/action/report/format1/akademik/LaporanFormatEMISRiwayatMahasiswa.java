package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
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
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanFormatEMISRiwayatMahasiswa extends MyWindow {

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

	private Decimalbox tahunAngkatan;
	private Decimalbox tahunAngkatanSd;

	private Combobox status;

	private Spreadsheet excelku;

	private MyToolbarbuttonConfig search;

	private Combobox statusMahasiswaAwal;

	public LaporanFormatEMISRiwayatMahasiswa() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Format EMIS Riwayat Mahasiswa", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanFormatEMISRiwayatMahasiswa(String title, String border, boolean closable) throws Exception {
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal"));
		row.appendChild(statusMahasiswaAwal = new Combobox());
		statusMahasiswaAwal.setWidth("90%");
		Common.insertComboDanSemua(statusMahasiswaAwal, "nama", StatusAwalMahasiswa.class,
				Restrictions.eq("aktif", true));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));

		int tahun = Calendar.getInstance().get(Calendar.YEAR);

		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(tahunAngkatan = new Decimalbox(new BigDecimal(tahun)));
		tahunAngkatan.setCols(4);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		hbox.appendChild(tahunAngkatanSd = new Decimalbox(new BigDecimal(tahun)));
		tahunAngkatanSd.setCols(4);

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
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"riwayat_mahasiswa.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanFormatEMISRiwayatMahasiswa.java:255");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Format EMIS Riwayat Mahasiswa", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});

				}
			}
		});
		print.setParent(toolbar);

		String[] contents = new String[] { "id", "nim", "nama", "lockId" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Mahasiswa.class, new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {

				if (datasMhsId.isEmpty()) {
					try {
						MyMessageboxConfig.show("Tombol \"Proses Data\" harus di klik dulu", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanFormatEMISRiwayatMahasiswa.java:274");
					}
					return null;
				}

				Criteria criteria = HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.in("id", datasMhsId)).addOrder(Order.asc("nim"));
				return criteria;
			}
		}, contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = Common.uploadData(new DataSearchDefault() {

			@Override
			public void onSearchDefault(Event event) {
				onCetak(null);
			}
		}, Mahasiswa.class, contents);
		toolbar.appendChild(upload);

		// onCetak(null);
	}

	@SuppressWarnings("rawtypes")
	private List<List> datas = null;
	private List<Long> datasMhsId = new ArrayList<Long>();

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetak(Event event) {

		try {

			Common.clear(center);

			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

			final String tahunAkademik = (String) (tahunAjaran.getSelectedItem() == null
					? Common.getCurrentTahunAkademik()
					: tahunAjaran.getSelectedItem().getValue());

			final String semestera = (String) (ganjilGenap.getSelectedItem() == null ? Common.getSemesterString()
					: ganjilGenap.getSelectedItem().getValue());

			final String programA = (String) (program.getSelectedItem() == null
					|| program.getSelectedItem().getValue() == null ? null : program.getSelectedItem().getValue());

			final Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null
					|| jurusan.getSelectedItem().getValue() == null ? null : jurusan.getSelectedItem().getValue());
			final Fakultas myFakultas = (Fakultas) (fakultas.getSelectedItem() == null
					|| fakultas.getSelectedItem().getValue() == null ? null : fakultas.getSelectedItem().getValue());

			final StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) (statusMahasiswaAwal
					.getSelectedItem() == null || statusMahasiswaAwal.getSelectedItem().getValue() == null ? null
							: statusMahasiswaAwal.getSelectedItem().getValue());

			final Integer tahunangkatan = tahunAngkatan.getValue() == null ? null : tahunAngkatan.getValue().intValue();
			final Integer tahunangkatanSd = tahunAngkatanSd.getValue() == null ? null
					: tahunAngkatanSd.getValue().intValue();

			final StatusMahasiswa selectedStatusMahasiswa = (StatusMahasiswa) (status.getSelectedItem() == null
					|| status.getSelectedItem().getValue() == null ? null : status.getSelectedItem().getValue());

			new Thread(new Runnable() {

				@Override
				public void run() {

					int size = 0;
					List<Mahasiswa> dataMhs = ConstantValues.simpleList(
							HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
									.add(Restrictions.between("tahunangkatan", tahunangkatan, tahunangkatanSd))
									.add(myJurusan == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("jurusan", myJurusan))
									.add(programA == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("program", programA))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							Mahasiswa.class);
					size += dataMhs.size();

					datas = new ArrayList<List>();
					datasMhsId = new ArrayList<Long>();

					int rowIndex = 2;

					for (Mahasiswa mahasiswa : dataMhs) {

						rowIndex++;
						label.setValue("Sedang memproses data " + mahasiswa.toString() + " ("
								+ Common.numberFormat.get().format((rowIndex - 1) * 100.0 / size) + " %)");

						if (myFakultas == null
								|| myFakultas.getId().equals(mahasiswa.getJurusan().getFakultas().getId())) {

							if (myJurusan == null || myJurusan.getId().equals(mahasiswa.getJurusan().getId())) {

								if (programA == null || programA.equals(mahasiswa.getProgram())) {

									if (statusAwalMahasiswa == null || statusAwalMahasiswa.getId()
											.equals(mahasiswa.getStatusAwalMahasiswa().getId())) {

										if (tahunangkatan == null || tahunangkatan <= mahasiswa.getTahunangkatan()) {

											if (tahunangkatanSd == null
													|| tahunangkatanSd >= mahasiswa.getTahunangkatan()) {

												List sub = new ArrayList();
												try {

													Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(),
															tahunAkademik, semestera,
															mahasiswa.getPindahKeKampusIniMasukSemester(),
															mahasiswa.getSemesterMulai());

													HistoryStatusMahasiswa historyStatusMahasiswa = Common
															.currentStatus(mahasiswa, tahunAkademik, smt);

													if (selectedStatusMahasiswa == null
															|| (historyStatusMahasiswa != null
																	&& historyStatusMahasiswa
																			.getStatusMahasiswa() != null
																	&& historyStatusMahasiswa.getStatusMahasiswa()
																			.getId()
																			.equals(selectedStatusMahasiswa.getId()))) {

														BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
														KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(
																mahasiswa, historyStatusMahasiswa.getSemester(), null,
																null);

														sub.add(mahasiswa.getLockId());
														sub.add(mahasiswa.getJurusan() == null ? ""
																: mahasiswa.getJurusan().getNama());
														sub.add(mahasiswa.getNim());
														sub.add(biodataMahasiswa == null ? ""
																: biodataMahasiswa.getNirm());
														sub.add(biodataMahasiswa == null ? ""
																: biodataMahasiswa.getNisn());
														sub.add(biodataMahasiswa == null ? ""
																: biodataMahasiswa.getNoIdentitas());
														sub.add(mahasiswa.getNama());
														sub.add(mahasiswa.getKelamin() == null ? null
																: mahasiswa.getKelamin().equalsIgnoreCase("Laki-Laki")
																		? 1
																		: 0);

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

														sub.add(biodataMahasiswa == null ? ""
																: biodataMahasiswa.getHp());
														sub.add("1");

														sub.add(krsMahasiswa.getSemester());

														sub.add(krsMahasiswa.getIpk());

														sub.add(mahasiswa.getBeasiswaMahasiswaMiskin() == null ? ""
																: mahasiswa.getBeasiswaMahasiswaMiskin().getNama());
														sub.add(mahasiswa.getBeasiswaBidikMisi() == null ? ""
																: mahasiswa.getBeasiswaBidikMisi().getNama());
														sub.add(mahasiswa.getBeasiswaLain() == null ? ""
																: mahasiswa.getBeasiswaLain().getNama());

														sub.add(historyStatusMahasiswa.getStatusMahasiswa() == null
																? null
																: (historyStatusMahasiswa.getStatusMahasiswa().getId()
																		.equals(ConstantValues.AKTIF.getId()))
																				? 1
																				: (historyStatusMahasiswa
																						.getStatusMahasiswa().getId()
																						.equals(ConstantValues.CUTI
																								.getId()) ? 2 : 3));

														if (historyStatusMahasiswa.getTanggalStatus() != null) {
															Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
															calendar.setTime(historyStatusMahasiswa.getTanggalStatus());
															sub.add(calendar.get(Calendar.DATE));
															sub.add(calendar.get(Calendar.MONTH) + 1);
															sub.add(calendar.get(Calendar.YEAR) + "");
														} else {
															sub.add("");
															sub.add("");
															sub.add("");
														}

														sub.add(mahasiswa.getSkDo());

														sub.add(biodataMahasiswa == null ? ""
																: biodataMahasiswa.getNo_rek_bri());
														sub.add(biodataMahasiswa == null ? ""
																: biodataMahasiswa.getCabangBri());
														sub.add(biodataMahasiswa == null ? ""
																: biodataMahasiswa.getKodeKerjaan());

														System.out.println("sub =>" + sub);
														datas.add(sub);
														datasMhsId.add(mahasiswa.getId());
													}

												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
													PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Format EMIS Riwayat Mahasiswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
															new String[] {
																"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
																"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
																"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
															});
												}

											}
										}
									}
								}
							}
						}
						System.out.println("Ukuran datas =>" + datas.size());
					}
					dataMhs.clear();
					dataMhs = null;
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

					ais.action.report.helper.LoadingReportUtil.showBusy(label);
					if (ais.action.report.helper.LoadingReportUtil.isSelesai(label)) {
						ais.action.report.helper.LoadingReportUtil.clearBusy();
						excelku = new ais.ui.util.MySpreadsheet();
						center.appendChild(excelku);
						excelku.setSrc("../../WEB-INF/riwayat_mahasiswa.xlsx");
						excelku.setStyle("border:1px solid #8AA3C1");
						excelku.setHeight("100%");
						excelku.setWidth("100%");
						excelku.setMaxrows(datas.size() + 3);
						excelku.setMaxcolumns(30);

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
						// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
						ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(excelku);
						ais.action.report.helper.LoadingReportUtil.stopAndDetach(timer);
					}

				}
			});
			timer.start();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Format EMIS Riwayat Mahasiswa", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
