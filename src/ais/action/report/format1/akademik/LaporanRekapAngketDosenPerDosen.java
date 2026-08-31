package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ChecklistBaruPenilaianDosenOlehMahasiswa;
import ais.database.model.ChecklistPenilaianDosen;
import ais.database.model.Dosen;
import ais.database.model.GrupChecklistPenilaianDosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.EcampusUtil;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan rekap angket dosen per dosen. Kelas ini mengubah data
 * domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Spreadsheet excelku}, {@code Center
 * center}, {@code MyToolbarbuttonConfig printAmbil}, {@code Combobox fakultas}, {@code Combobox jurusan}, {@code
 * Combobox searchta}, {@code Combobox searchsmt}, {@code Tbmuser tbmuser}; inisialisasi/lifecycle ({@code
 * init()}); pelaporan/ekspor ({@code onCetak()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapAngketDosenPerDosen extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Spreadsheet excelku;

	private Center center;

	private MyToolbarbuttonConfig printAmbil;

	private Combobox fakultas;

	private Combobox jurusan;

	private Combobox searchta;

	private Combobox searchsmt;

	private Tbmuser tbmuser;

	public LaporanRekapAngketDosenPerDosen() throws Exception {
		super();
		init();
	}

	public LaporanRekapAngketDosenPerDosen(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() throws Exception {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		tbmuser = Common.getCurrentUser();

		fakultas = new Combobox();
		jurusan = new Combobox();

		searchsmt = new Combobox();
		searchta = new Combobox();

		Common.generateTahunAjaran(searchta);

		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchsmt.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchsmt.appendChild(comboitem);
		searchsmt.setCols(2);

		comboitem = new Comboitem(Perkuliahan.SP);
		comboitem.setValue(Perkuliahan.SP);
		searchsmt.appendChild(comboitem);
		searchsmt.setCols(2);

		Common.selectComboItem(searchsmt, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		searchsmt.setReadonly(true);

		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		LayoutRegion west = Common.isMobile() ? new North() : new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		if (Common.isMobile()) {
			west.setHeight("250px");
			west.setOpen(false);
		} else {
			west.setWidth("150px");
		}

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		Vbox vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		vbox.appendChild(fakultas);
		fakultas.setCols(6);

		row = new MyFormRow();
		row.setParent(rows);

		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Jurusan"));
		vbox.appendChild(jurusan);
		jurusan.setCols(6);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("TA"));
		vbox.appendChild(searchta);
		searchta.setCols(6);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		vbox.appendChild(searchsmt);
		searchsmt.setCols(6);

		row = new MyFormRow();
		row.setParent(rows);

		Vbox toolbar = new Vbox();
		toolbar.setParent(row);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(null);
			}
		});
		print.setParent(toolbar);

		printAmbil = new MyToolbarbuttonConfig("Ambil File", "/img/excel.png");
		printAmbil.setVisible(false);
		printAmbil.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					excelku.getBook().write(bout);
					bout.close();
					Filedownload.save(bout.toByteArray(),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "data.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanRekapAngketDosenPerDosen.java:207");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Angket Dosen Per Dosen", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});

				}
			}
		});
		printAmbil.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		if (tbmuser != null && tbmuser.getSiswa() != null) {
			onCetak(null);
		}

	}

	@SuppressWarnings("rawtypes")
	private List<List> datas = null;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetak(Event event) {

		try {

			Common.clear(center);

			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

			new Thread(new Runnable() {

				@SuppressWarnings("unused")
				@Override
				public void run() {

					datas = new ArrayList<List>();

					Session session = HibernateUtil.currentSession();
					List<ChecklistBaruPenilaianDosenOlehMahasiswa> checklistBaruPenilaianDosenOlehMahasiswas = session
							.createCriteria(ChecklistBaruPenilaianDosenOlehMahasiswa.class)

							.createAlias("perkuliahan", "perkuliahan")

							.add(searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null
									|| searchsmt.getSelectedItem().getValue() == null
											? Restrictions.sqlRestriction("1=1")
											: Restrictions.eq("perkuliahan.ganjilGenap",
													searchsmt.getSelectedItem().getValue()))

							.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
									|| searchta.getSelectedItem().getValue() == null
											? Restrictions.sqlRestriction("1=1")
											: Restrictions.eq("perkuliahan.tahunAjaran",
													searchta.getSelectedItem().getValue()))

							.add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
									|| jurusan.getSelectedItem().getValue() == null
											? Restrictions.sqlRestriction("1=1")
											: CommonSearchFilterHelper.eqSelectedWithId("perkuliahan.jurusan", jurusan, false))

							.createAlias("perkuliahan.jurusan", "jurusan")

							.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
									|| fakultas.getSelectedItem().getValue() == null
											? Restrictions.sqlRestriction("1=1")
											: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", fakultas, false))

							.list();

					TreeMap<Long, List<ChecklistBaruPenilaianDosenOlehMahasiswa>> myItems = new TreeMap<Long, List<ChecklistBaruPenilaianDosenOlehMahasiswa>>();

					TreeMap<Long, TreeMap<Long, ChecklistPenilaianDosen>> checklistPenilaianDosens = new TreeMap<Long, TreeMap<Long, ChecklistPenilaianDosen>>();
					for (ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa : checklistBaruPenilaianDosenOlehMahasiswas) {
						try {
							List<Dosen> dosens = checklistBaruPenilaianDosenOlehMahasiswa.getPerkuliahan()
									.populateDosenBuNama();
							for (Dosen dosen : dosens) {

								List<ChecklistBaruPenilaianDosenOlehMahasiswa> baruPenilaianDosenOlehMahasiswas = myItems
										.get(dosen.getId());
								if (baruPenilaianDosenOlehMahasiswas == null) {
									baruPenilaianDosenOlehMahasiswas = new ArrayList<ChecklistBaruPenilaianDosenOlehMahasiswa>();
									myItems.put(dosen.getId(), baruPenilaianDosenOlehMahasiswas);
								}
								baruPenilaianDosenOlehMahasiswas.add(checklistBaruPenilaianDosenOlehMahasiswa);

							}

							for (String d : StringUtils.split(checklistBaruPenilaianDosenOlehMahasiswa.getKeterangan(),
									"<>___DATA")) {
								try {
									if (StringUtils.contains(d, ";")) {
										Long sd = Long.parseLong(StringUtils.split(d, ";")[0].trim());
										ChecklistPenilaianDosen checklistPenilaianDosen = (ChecklistPenilaianDosen) ConstantValues
												.ambil(ChecklistPenilaianDosen.class.getName(), sd);
										if (checklistPenilaianDosen != null) {
											GrupChecklistPenilaianDosen grupChecklistPenilaianDosen = checklistPenilaianDosen
													.getGrupChecklistPenilaianDosen();
											if (grupChecklistPenilaianDosen != null) {
												TreeMap<Long, ChecklistPenilaianDosen> subchecklistPenilaianDosens = checklistPenilaianDosens
														.get(grupChecklistPenilaianDosen.getId());
												if (subchecklistPenilaianDosens == null) {
													subchecklistPenilaianDosens = new TreeMap<Long, ChecklistPenilaianDosen>();
													checklistPenilaianDosens.put(grupChecklistPenilaianDosen.getId(),
															subchecklistPenilaianDosens);
												}
												subchecklistPenilaianDosens.put(checklistPenilaianDosen.getId(),
														checklistPenilaianDosen);
											}
										}
									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapAngketDosenPerDosen.java:319");
									PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Angket Dosen Per Dosen", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapAngketDosenPerDosen.java:323");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Angket Dosen Per Dosen", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
						}

					}

					System.out.println("myItems => " + myItems.size());
					System.out.println("checklistPenilaianDosens => " + checklistPenilaianDosens.size());

					List sub = new ArrayList();
					sub.add("**No.");
					sub.add("**NIDN");
					sub.add("**Nama");
					sub.add("**Prodi");
					sub.add("**Fakultas");
					sub.add("**Status");
					sub.add("**Pertanyaan/Kuesioner");
					datas.add(sub);

					int nomor = 1;
					for (Long dosenId : myItems.keySet()) {
						Dosen dosen = (Dosen) ConstantValues.ambil(Dosen.class.getName(), dosenId);
						List<ChecklistBaruPenilaianDosenOlehMahasiswa> aa = myItems.get(dosenId);

						try {

							label.setValue("Sedang memproses dosen " + dosen.toString());

							Map<String, Mahasiswa> mahasiswas = new HashMap<String, Mahasiswa>();
							for (ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa : aa) {
								mahasiswas.put(checklistBaruPenilaianDosenOlehMahasiswa.getMahasiswa().getNim(),
										checklistBaruPenilaianDosenOlehMahasiswa.getMahasiswa());
							}

							Map<Long, Perkuliahan> perkuliahans = new HashMap<Long, Perkuliahan>();
							for (ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa : aa) {
								perkuliahans.put(checklistBaruPenilaianDosenOlehMahasiswa.getPerkuliahan().getId(),
										checklistBaruPenilaianDosenOlehMahasiswa.getPerkuliahan());
							}

							try {

								sub = new ArrayList();
								sub.add("**");
								sub.add("**");
								sub.add("**");
								sub.add("**");
								sub.add("**");
								sub.add("**");
								sub.add("**");

								for (Mahasiswa mahasiswa : mahasiswas.values()) {
									sub.add("**" + mahasiswa.getNim() + "-" + mahasiswa.getNama());
								}

								sub.add("**Total");
								sub.add("**Rata-Rata");

								datas.add(sub);

							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanRekapAngketDosenPerDosen.java:382");
								// TODO: handle exception
							}

							sub = new ArrayList();
							sub.add(nomor);
							sub.add(dosen.getNidn());
							sub.add(dosen.getNama());
							sub.add(dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama());
							sub.add(dosen.getFakultas() == null ? "" : dosen.getFakultas().getNama());
							sub.add(dosen.getStatusKepegawaian() == null ? "" : dosen.getStatusKepegawaian().getNama());
							sub.add("**Pertanyaan/Kuesioner");
							datas.add(sub);
							Map<String, Integer> jumlahMasingMasing = new HashMap<String, Integer>();
							Integer jumlahSemua = 0;
							Integer totalSemua = 0;
							for (Perkuliahan perkuliahan : perkuliahans.values()) {

								sub = new ArrayList();
								sub.add("**");
								sub.add("**" + perkuliahan.getMatakuliah().getKode());
								sub.add("**" + perkuliahan.getMatakuliah().getNama());
								sub.add("**" + perkuliahan.getJurusan().getNama());
								sub.add("**" + perkuliahan.getProgram());
								sub.add("**" + perkuliahan.getKelas());
								sub.add("**" + perkuliahan.getSemester());

								for (Mahasiswa mahasiswa : mahasiswas.values()) {
									sub.add("**" + mahasiswa.getNim() + "-" + mahasiswa.getNama());
								}

								sub.add("**");
								sub.add("**");

								datas.add(sub);

								for (Long idGrup : checklistPenilaianDosens.keySet()) {

									GrupChecklistPenilaianDosen grupChecklistPenilaianDosen = (GrupChecklistPenilaianDosen) ConstantValues
											.ambil(GrupChecklistPenilaianDosen.class.getName(), idGrup);

									TreeMap<Long, ChecklistPenilaianDosen> treeMap = checklistPenilaianDosens
											.get(idGrup);

									System.out.println("keyGrup -> " + idGrup + " " + treeMap.size() + " dosen -> "
											+ dosen.toString());

									sub = new ArrayList();
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**" + grupChecklistPenilaianDosen.getIsi());
									for (Mahasiswa mahasiswa : mahasiswas.values()) {
										sub.add("**");
									}

									sub.add("**");
									sub.add("**");
									datas.add(sub);

									for (ChecklistPenilaianDosen checklistPenilaianDosen : treeMap.values()) {
										sub = new ArrayList();
										sub.add("");
										sub.add("");
										sub.add("");
										sub.add("");
										sub.add("");
										sub.add("");
										sub.add(checklistPenilaianDosen.getIsi());

										System.out.println(
												"isi -> " + checklistPenilaianDosen.getIsi() + " " + treeMap.size());
										Integer jumlah = 0;
										Integer total = 0;

										for (Mahasiswa mahasiswa : mahasiswas.values()) {

											ChecklistBaruPenilaianDosenOlehMahasiswa baruPenilaianDosenOlehMahasiswa = null;
											for (ChecklistBaruPenilaianDosenOlehMahasiswa c : aa) {
												if (c.getPerkuliahan().getId().equals(perkuliahan.getId())
														&& dosenId.equals(c.getDosen().getId())) {
													baruPenilaianDosenOlehMahasiswa = c;
													break;
												}
											}

											if (baruPenilaianDosenOlehMahasiswa != null) {
												Integer val = baruPenilaianDosenOlehMahasiswa
														.getValue(checklistPenilaianDosen);
												sub.add(val);
												total += val;
												jumlah++;

												totalSemua += val;
												jumlahSemua++;

												String key = mahasiswa.getId() + "_" + perkuliahan.getId();
												Integer sebelumnya = jumlahMasingMasing.get(key);
												if (sebelumnya == null) {
													sebelumnya = 0;
												}
												sebelumnya += val;
												jumlahMasingMasing.put(key, sebelumnya);
											} else {
												sub.add("");
											}

										}
										sub.add(total);
										sub.add(total.doubleValue() / jumlah.doubleValue());
										datas.add(sub);
									}

								}

								sub = new ArrayList();
								sub.add("**");
								sub.add("**Total");
								sub.add("**");
								sub.add("**");
								sub.add("**");
								sub.add("**");
								sub.add("**");
								for (Mahasiswa mahasiswa : mahasiswas.values()) {
									String key = mahasiswa.getId() + "_" + perkuliahan.getId();
									Integer sebelumnya = jumlahMasingMasing.get(key);
									if (sebelumnya == null) {
										sebelumnya = 0;
									}
									sub.add("**" + Common.numberFormat.get().format(sebelumnya));
								}

								sub.add("**" + Common.numberFormat.get().format(totalSemua));
								sub.add("**" + Common.numberFormat.get()
										.format(totalSemua.doubleValue() / jumlahSemua.doubleValue()));
								datas.add(sub);

							}

							nomor++;

						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapAngketDosenPerDosen.java:527");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Angket Dosen Per Dosen", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
						}

					}

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

//						System.out.println("Tampilkan data --> " + datas);

						// FIX race "Only one child is allowed: <Center>": hentikan & lepas Timer SEGERA
						// (bukan di akhir) sebelum membangun/memasang spreadsheet. Membangun spreadsheet +
						// grid pratinjau bisa memakan waktu > interval Timer (1000ms); bila Timer baru
						// dihentikan di akhir, klien bisa sudah mengirim tick "onTimer" berikutnya sebelum
						// menerima sinyal stop, sehingga blok ini terpanggil DUA KALI untuk 'center' yang
						// sama (percobaan attach kedua menabrak anak dari percobaan pertama).
						ais.action.report.helper.LoadingReportUtil.stopAndDetach(timer);

						ais.action.report.helper.LoadingReportUtil.clearBusy();
						excelku = new ais.ui.util.MySpreadsheet();
						Common.clear(center);
						excelku.setParent(center);
						EcampusUtil.tampilkan(datas, excelku, false);
						// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
						ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(excelku);

						printAmbil.setVisible(true);
					}

				}
			});
			timer.start();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Angket Dosen Per Dosen", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
