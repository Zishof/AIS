package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.EcampusUtil;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan format ptkkn. Kelas ini mengubah data domain menjadi
 * bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke
 * lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Combobox
 * fakultas}, {@code Combobox jurusan}, {@code Combobox program}, {@code Combobox tahunAjaran}, {@code Combobox
 * ganjilGenap}, {@code Combobox angkatan}, {@code Combobox angkatanSampai}; inisialisasi/lifecycle ({@code
 * init()}); pelaporan/ekspor ({@code onCetak()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanFormatPTKKN extends MyWindow {

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

	public LaporanFormatPTKKN() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Format PTKKN", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanFormatPTKKN(String title, String border, boolean closable) throws Exception {
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
		tahunAjaran = Common.generateTahunAjaran(tahunAjaran);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan Mulai"));
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan Sampai"));
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
		Common.insertComboDanSemua(status, "nama", "kodeEpsbed", StatusMahasiswa.class);
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
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "PTKKN.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanFormatPTKKN.java:256");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Format PTKKN", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
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

	// private List<HistoryStatusMahasiswa> mahasiswas = null;
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

					String tahunAkademik = (String) (tahunAjaran.getSelectedItem() == null
							? Common.getCurrentTahunAkademik()
							: tahunAjaran.getSelectedItem().getValue());

					String semestera = (String) (ganjilGenap.getSelectedItem() == null ? Common.getSemesterString()
							: ganjilGenap.getSelectedItem().getValue());

					String programA = (String) (program.getSelectedItem() == null
							|| program.getSelectedItem().getValue() == null ? null
									: program.getSelectedItem().getValue());

					Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null
							|| jurusan.getSelectedItem().getValue() == null ? null
									: jurusan.getSelectedItem().getValue());
					Fakultas myFakultas = (Fakultas) (fakultas.getSelectedItem() == null
							|| fakultas.getSelectedItem().getValue() == null ? null
									: fakultas.getSelectedItem().getValue());

					Integer tahunangkatan = (Integer) (angkatan.getValue() == null
							|| angkatan.getSelectedItem().getValue() == null ? null
									: angkatan.getSelectedItem().getValue());
					Integer tahunangkatanSd = (Integer) (angkatanSampai.getValue() == null
							|| angkatanSampai.getSelectedItem().getValue() == null ? null
									: angkatanSampai.getSelectedItem().getValue());

					StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (status.getSelectedItem() == null
							|| status.getSelectedItem().getValue() == null ? null
									: status.getSelectedItem().getValue());
					List<Mahasiswa> dataMhs = ConstantValues.simpleList(
							HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
									.add(Restrictions.between("tahunangkatan", tahunangkatan, tahunangkatanSd))
									.add(myJurusan == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("jurusan", myJurusan))
									.add(programA == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("program", programA))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							Mahasiswa.class);

					int size = dataMhs.size();

					int rowIndex = 0;
					datas = new ArrayList<List>();

					List sub = new ArrayList();
					sub.add("**No");
					sub.add("**NIK");
					sub.add("**NIM");
					sub.add("**Nama Lengkap");
					sub.add("**Jenis Kelamin");
					sub.add("**Tempat Lahir");
					sub.add("**Tanggal Lahir (dd/mm/yyyy)");
					sub.add("**Alamat (KTP)");
					sub.add("**Provinsi (KTP)");
					sub.add("**Kabupaten/Kota (KTP)");
					sub.add("**Kecamatan (KTP)");
					sub.add("**Kelurahan (KTP)");
					sub.add("**Tahun Angkatan");
					sub.add("**Prodi");
					sub.add("**Jenjang Pendidikan");
					sub.add("**Status Mahasiswa");
					sub.add("**PTKK");
					sub.add("**Provinsi PTKK");
					sub.add("**Kabupaten/Kota PTKK");
					sub.add("**Alamat");
					sub.add("**Status PTKK (Swasta/Negeri)");
					sub.add("**Status KIP");
					sub.add("**Nomor KIP");
					sub.add("**Tahun Angkatan KIP");
					sub.add("**Tahun Anggaran KIP");
					sub.add("**Status Penerima KIP");
					datas.add(sub);

					for (Mahasiswa mahasiswa : dataMhs) {

						try {

							if (myFakultas == null
									|| myFakultas.getId().equals(mahasiswa.getJurusan().getFakultas().getId())) {

								if (myJurusan == null || myJurusan.getId().equals(mahasiswa.getJurusan().getId())) {

									if (programA == null || programA.equals(mahasiswa.getProgram())) {

										if (tahunangkatan == null || tahunangkatan <= mahasiswa.getTahunangkatan()) {

											if (tahunangkatanSd == null
													|| tahunangkatanSd >= mahasiswa.getTahunangkatan()) {

												sub = new ArrayList();

												try {

													Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(),
															tahunAkademik, semestera,
															mahasiswa.getPindahKeKampusIniMasukSemester(),
															mahasiswa.getSemesterMulai());
													BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
													HistoryStatusMahasiswa historyStatusMahasiswa = Common
															.currentStatus(mahasiswa, tahunAkademik, smt);

													if (statusMahasiswa == null || (historyStatusMahasiswa != null
															&& historyStatusMahasiswa.getStatusMahasiswa() != null
															&& statusMahasiswa.getId().equals(historyStatusMahasiswa
																	.getStatusMahasiswa().getId()))) {

														rowIndex++;
														label.setValue("Sedang memproses data " + mahasiswa.toString()
																+ " ("
																+ Common.numberFormat.get().format(rowIndex * 100.0 / size)
																+ " %)");

														sub.add(rowIndex);

														sub.add(biodataMahasiswa.getNoIdentitas());
														sub.add(mahasiswa.getNim());

														sub.add(mahasiswa.getNama());

														sub.add(mahasiswa.getKelamin());

														sub.add(mahasiswa.getTempatlahir());

														sub.add(mahasiswa.getTanggallahir() == null ? ""
																: Common.dateFormat112.get()
																		.format(mahasiswa.getTanggallahir()));

														sub.add(biodataMahasiswa.getAlamat());

														sub.add(biodataMahasiswa.getPropinsi() == null ? ""
																: biodataMahasiswa.getPropinsi().getNama());
														sub.add(biodataMahasiswa.getKota() == null ? ""
																: biodataMahasiswa.getKota().getNama());
														sub.add(biodataMahasiswa.getKecamatan() == null ? ""
																: biodataMahasiswa.getKecamatan().getNama());
														sub.add(biodataMahasiswa.getKelurahan());

														sub.add(mahasiswa.getTahunangkatan());
														sub.add(mahasiswa.getJurusan() == null ? ""
																: mahasiswa.getJurusan().getNama());
														sub.add(mahasiswa.getJenjang() == null ? ""
																: mahasiswa.getJenjang().getNama());

														sub.add(historyStatusMahasiswa.getStatusMahasiswa() == null ? ""
																: historyStatusMahasiswa.getStatusMahasiswa()
																		.getNama());

														sub.add(mahasiswa.getJurusan() == null
																|| mahasiswa.getJurusan().getFakultas() == null
																|| mahasiswa.getJurusan().getFakultas()
																		.getPerguruanTinggi() == null
																				? ""
																				: mahasiswa.getJurusan().getFakultas()
																						.getPerguruanTinggi()
																						.getNama());

														sub.add(mahasiswa.getJurusan() == null
																|| mahasiswa.getJurusan().getFakultas() == null
																|| mahasiswa.getJurusan().getFakultas()
																		.getPerguruanTinggi() == null
																				? ""
																				: mahasiswa.getJurusan().getFakultas()
																						.getPerguruanTinggi()
																						.getPropinsi());

														sub.add(mahasiswa.getJurusan() == null
																|| mahasiswa.getJurusan().getFakultas() == null
																|| mahasiswa.getJurusan().getFakultas()
																		.getPerguruanTinggi() == null
																				? ""
																				: mahasiswa.getJurusan().getFakultas()
																						.getPerguruanTinggi()
																						.getKota());

														sub.add(mahasiswa.getJurusan() == null
																|| mahasiswa.getJurusan().getFakultas() == null
																|| mahasiswa.getJurusan().getFakultas()
																		.getPerguruanTinggi() == null
																				? ""
																				: mahasiswa.getJurusan().getFakultas()
																						.getPerguruanTinggi()
																						.getAlamat1());

														sub.add(mahasiswa.getJurusan() == null
																|| mahasiswa.getJurusan().getFakultas() == null
																|| mahasiswa.getJurusan().getFakultas()
																		.getPerguruanTinggi() == null
																				? ""
																				: mahasiswa.getJurusan().getFakultas()
																						.getPerguruanTinggi()
																						.getStatus());

														sub.add(mahasiswa.getStatusAwalMahasiswa() == null ? ""
																: mahasiswa.getStatusAwalMahasiswa().getNama()
																		.toLowerCase().contains("kip")
																		|| mahasiswa.getStatusAwalMahasiswa().getNama()
																				.toLowerCase()
																				.contains("kartu indonesia pintar")
																						? "Ya"
																						: "Tidak");

														sub.add(mahasiswa.getStatusKrs());

														sub.add("");
														sub.add("");
														sub.add("");

														System.out.println("sub =>" + sub);
														datas.add(sub);
													}

												} catch (Exception e) {
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanFormatPTKKN.java:495");
													PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Format PTKKN", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanFormatPTKKN.java:505");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Format PTKKN", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
						}
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
						try {
							ais.action.report.helper.LoadingReportUtil.clearBusy();
							excelku = new ais.ui.util.MySpreadsheet();
							Common.clear(center);
							center.appendChild(excelku);
							EcampusUtil.tampilkan(datas, excelku, false);
							// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
							ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(excelku);

							ais.action.report.helper.LoadingReportUtil.stopAndDetach(timer);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanFormatPTKKN.java:536");
							PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Format PTKKN", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
								new String[] {
									"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
									"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
						}
					}

				}
			});
			timer.start();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Format PTKKN", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}
}
