package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ChecklistBaruPenilaianDosenOlehMahasiswa;
import ais.database.model.ChecklistPenilaianDosen;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.ui.util.EcampusUtil;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan rekap angket dosen. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Spreadsheet excelku}, {@code Combobox
 * tahunAkademik}, {@code Combobox fakultas}, {@code Combobox jurusan}, {@code Combobox semesterAbsensi}, {@code
 * AmbilDataDosenBanbox dosen}, {@code Center center}, {@code MyToolbarbuttonConfig printAmbil};
 * inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor ({@code onCetak()}); operasi domain lain ({@code
 * populateTotal()}, {@code populateTotalPerkuliahan()}). Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapAngketDosen extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Spreadsheet excelku;

	private Combobox tahunAkademik;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox semesterAbsensi;
	private AmbilDataDosenBanbox dosen;

	private Center center;

	private MyToolbarbuttonConfig printAmbil;

	private Combobox program;

	private AmbilDataMasaPerkuliahanBanbox masaPerkuliahan;

	private MyCheckboxConfig semesterPendek;

	private Dosen dsn;

	public LaporanRekapAngketDosen() throws Exception {
		this(null);
	}

	public LaporanRekapAngketDosen(Dosen dsn) {
		super();
		this.dsn = dsn;
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Angket Dosen", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanRekapAngketDosen(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {
		setHeight("100%");
		setWidth("100%");
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
			west.setWidth("250px");
		}

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		program = Common.initPrograms(null);
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaran(tahunAkademik);
		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterAbsensi = new Combobox());
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setWidth("90%");
//		comboitem = new MyComboitemConfig();
//		comboitem.setLabel("Semua");
//		comboitem.setValue(null);
//		semesterAbsensi.appendChild(comboitem);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		semesterAbsensi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Perkuliahan"));
		row.appendChild(masaPerkuliahan = new AmbilDataMasaPerkuliahanBanbox());
		masaPerkuliahan.setWidth("90%");
		jurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (jurusan.getSelectedItem() != null) {
					masaPerkuliahan.setJurusanSelected((Jurusan) jurusan.getSelectedItem().getValue());
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(this.semesterPendek = new MyCheckboxConfig("Semester Pendek"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_dosen")));
		row.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setWidth("90%");
		dosen.setReadonly(true);

		if (dsn != null) {
			dosen.setAttribute("dosen", dsn);
			dosen.setAttribute("myValue", dsn);
			dosen.setValue(dsn.getNama());
		}

		Common.initKeterangan(rows, "Jika dosen tidak dipilih, maka akan tampil data semua dosen");

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
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
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "rekap_angket.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanRekapAngketDosen.java:259");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Angket Dosen", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
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

	}

	@SuppressWarnings("rawtypes")
	private List<List> datas = null;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetak(Event event) {

		final String genapGanjil = (String) (semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? "Semua"
						: semesterAbsensi.getSelectedItem().getValue());

		final String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? "Semua"
						: this.tahunAkademik.getSelectedItem().getValue());

		final Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());

		final Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());

		final String program = (String) (this.program.getSelectedItem() == null
				|| this.program.getSelectedItem().getValue() == null ? null
						: this.program.getSelectedItem().getValue());

		final Dosen dosen = (Dosen) this.dosen.getAttribute("dosen");
		final MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) this.masaPerkuliahan.getAttribute("masaPerkuliahan");

		final Integer sp = semesterPendek.isChecked() ? 1 : null;

		try {

			Common.clear(center);

			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

			new Thread(new Runnable() {

				@SuppressWarnings("unused")
				@Override
				public void run() {

					datas = new ArrayList<List>();

					Session session = ais.action.report.Report.openNativeSession();
					List<Long> ids = session.createCriteria(ChecklistBaruPenilaianDosenOlehMahasiswa.class)

							.setProjection(Projections.property("id"))

							.add(dosen == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("dosen", dosen))

							.createAlias("perkuliahan", "perkuliahan").createAlias("perkuliahan.jurusan", "jurusan")

							.add(sp == null ? Restrictions.isNull("perkuliahan.statusSemesterPendek")
									: Restrictions.eq("perkuliahan.statusSemesterPendek", sp))

							.add(fakultas == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("jurusan.fakultas", fakultas))

							.add(program == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("perkuliahan.program", program))

							.add(tahunAkademik == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("perkuliahan.tahunAjaran", tahunAkademik))

							.add(masaPerkuliahan == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("perkuliahan.masaPerkuliahan", masaPerkuliahan))

							.add(jurusan == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("perkuliahan.jurusan", jurusan))

							.add(genapGanjil == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("perkuliahan.ganjilGenap", genapGanjil))

							.addOrder(Order.asc("perkuliahan.id")).addOrder(Order.asc("mahasiswa.id"))

							.list();
					// session.disconnect();
					if (session.isOpen()) {session.disconnect();session.close();}
					ais.action.report.Report.closeCurrentSessionQuietly();

					int size = ids.size();
					int index = 0;

					Map<Long, TreeMap<String, Object[]>> myItems = new HashMap<Long, TreeMap<String, Object[]>>();

					Map<String, TreeMap<String, Object[]>> myItemsPerkuliahan = new HashMap<String, TreeMap<String, Object[]>>();

					TreeMap<String, ChecklistPenilaianDosen> all = new TreeMap<String, ChecklistPenilaianDosen>();

					TreeMap<Long, Perkuliahan> allPerkuliahan = new TreeMap<Long, Perkuliahan>();

					for (Long id : ids) {
						index++;
						session = ais.action.report.Report.openNativeSession();
						try {
							ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa = (ChecklistBaruPenilaianDosenOlehMahasiswa) session
									.createCriteria(ChecklistBaruPenilaianDosenOlehMahasiswa.class)
									.add(Restrictions.idEq(id)).uniqueResult();
							if (checklistBaruPenilaianDosenOlehMahasiswa != null) {
								label.setValue("Sedang memproses data "
										+ checklistBaruPenilaianDosenOlehMahasiswa.getPerkuliahan() + " ("
										+ Common.numberFormat.get().format((index * 100.0) / size) + ")");

								List<Object[]> objectss = checklistBaruPenilaianDosenOlehMahasiswa.ambilValue();
								for (Object[] object : objectss) {
									Long checklistPenilaianDosenId = (Long) (object.length > 0 ? object[0] : -1L);
									ChecklistPenilaianDosen checklistPenilaianDosen = (ChecklistPenilaianDosen) session
											.createCriteria(ChecklistPenilaianDosen.class)
											.add(Restrictions.idEq(checklistPenilaianDosenId)).uniqueResult();
									if (checklistPenilaianDosen != null) {
										try {
											Integer nilai = (Integer) (object.length > 1 ? object[1] : 0);
											String keterangan = (String) (object.length > 2 ? object[2] : "");

											TreeMap<String, Object[]> treeMap = myItems.get(
													checklistBaruPenilaianDosenOlehMahasiswa.getMahasiswa().getId());
											if (treeMap == null) {
												treeMap = new TreeMap<String, Object[]>();
												myItems.put(
														checklistBaruPenilaianDosenOlehMahasiswa.getMahasiswa().getId(),
														treeMap);
											}

											TreeMap<String, Object[]> treeMapPerkuliahan = myItemsPerkuliahan
													.get(checklistBaruPenilaianDosenOlehMahasiswa.getMahasiswa().getId()
															+ "_"
															+ checklistBaruPenilaianDosenOlehMahasiswa.getPerkuliahan()
																	.getId()
															+ "_" + checklistBaruPenilaianDosenOlehMahasiswa.getDosen()
																	.getId());
											if (treeMapPerkuliahan == null) {
												treeMapPerkuliahan = new TreeMap<String, Object[]>();
												myItemsPerkuliahan.put(checklistBaruPenilaianDosenOlehMahasiswa
														.getMahasiswa().getId()
														+ "_"
														+ checklistBaruPenilaianDosenOlehMahasiswa.getPerkuliahan()
																.getId()
														+ "_"
														+ checklistBaruPenilaianDosenOlehMahasiswa.getDosen().getId(),
														treeMapPerkuliahan);
											}

											String key = checklistPenilaianDosen.ambilkey();

											Object[] o = treeMap.get(key);
											if (o == null) {
												o = new Object[] { checklistPenilaianDosen, nilai };
											} else {
												Integer n = (Integer) o[1];
												n += nilai;
												o = new Object[] { checklistPenilaianDosen, n };
											}
											treeMap.put(key, o);

											o = treeMapPerkuliahan.get(key);
											if (o == null) {
												o = new Object[] { checklistPenilaianDosen, nilai };
											} else {
												Integer n = (Integer) o[1];
												n += nilai;
												o = new Object[] { checklistPenilaianDosen, n };
											}
											treeMapPerkuliahan.put(key, o);

											all.put(key, checklistPenilaianDosen);

											allPerkuliahan.put(
													checklistBaruPenilaianDosenOlehMahasiswa.getPerkuliahan().getId(),
													checklistBaruPenilaianDosenOlehMahasiswa.getPerkuliahan());

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapAngketDosen.java:445");
											PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Angket Dosen", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
												new String[] {
													"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
													"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
													"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
												});
										}
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapAngketDosen.java:451");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Angket Dosen", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
						}
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
						ais.action.report.Report.closeCurrentSessionQuietly();
					}

					populateTotal(myItems, all, datas, label);

					populateTotalPerkuliahan(allPerkuliahan, myItemsPerkuliahan, all, datas, label);

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

						Common.clear(center);

						ais.action.report.helper.LoadingReportUtil.clearBusy();
						excelku = new ais.ui.util.MySpreadsheet();
						center.appendChild(excelku);
						EcampusUtil.tampilkan(datas, excelku, false);
						// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
						ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(excelku);
						printAmbil.setVisible(true);
						ais.action.report.helper.LoadingReportUtil.stopAndDetach(timer);
					}

				}
			});
			timer.start();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Angket Dosen", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	private void populateTotal(Map<Long, TreeMap<String, Object[]>> myItems,
			TreeMap<String, ChecklistPenilaianDosen> all, List<List> datas, Label label) {

		ArrayList sub;

		Long jdl = null;

		Map<Long, Integer> mapsTotal = new HashMap<Long, Integer>();
		int nomor = 1;
		for (ChecklistPenilaianDosen checklistPenilaianDosen : all.values()) {

			if (jdl == null || (checklistPenilaianDosen.getGrupChecklistPenilaianDosen() != null
					&& !jdl.equals(checklistPenilaianDosen.getGrupChecklistPenilaianDosen().getId()))) {

				sub = new ArrayList();
				sub.add("");
				sub.add("Semua Perkuliahan");
				for (Long mhs : myItems.keySet()) {
					sub.add("");
				}
				datas.add(sub);

				sub = new ArrayList();
				sub.add("");
				sub.add(checklistPenilaianDosen.getGrupChecklistPenilaianDosen().getIsi());
				for (Long mhs : myItems.keySet()) {
					sub.add("");
				}
				datas.add(sub);

				sub = new ArrayList();
				sub.add("No.");
				sub.add("Judul");

				for (Long mhs : myItems.keySet()) {
					Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), mhs);
					sub.add(mahasiswa.getNim() + "-" + mahasiswa.getNama());
				}

				datas.add(sub);

				jdl = checklistPenilaianDosen.getGrupChecklistPenilaianDosen().getId();
				nomor = 1;
			}

			sub = new ArrayList();

			try {
				String key = checklistPenilaianDosen.ambilkey();
				label.setValue("Sedang memproses data " + key);

				sub.add(nomor);
				sub.add(checklistPenilaianDosen.getIsi());

				for (Long mhs : myItems.keySet()) {

					TreeMap<String, Object[]> treeMap = myItems.get(mhs);

					if (treeMap != null) {
						Object[] objects = treeMap.get(key);
						if (objects != null) {
							Integer nilai = (Integer) objects[1];
							sub.add(nilai);

							Integer totalSemua = mapsTotal.get(mhs);
							if (totalSemua == null) {
								totalSemua = 0;
							}

							totalSemua += nilai;
							mapsTotal.put(mhs, totalSemua);

						} else {
							sub.add(0.0);
						}
					}
				}

				System.out.println("sub =>" + sub);
				datas.add(sub);

				nomor++;

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Angket Dosen", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
			}

		}

		try {

			sub = new ArrayList();
			sub.add("");
			sub.add("Total");

			Integer semua = 0;

			for (Long mhs : myItems.keySet()) {
				Integer totalSemua = mapsTotal.get(mhs);
				if (totalSemua == null) {
					totalSemua = 0;
				}

				sub.add(totalSemua);

				semua += totalSemua;
			}

			datas.add(sub);

			sub = new ArrayList();
			sub.add("");
			sub.add("Total Semua");
			sub.add(semua);

			datas.add(sub);

			sub = new ArrayList();
			sub.add("");
			sub.add("Rata-Rata Total");
			sub.add((1.0 * semua) / (myItems.size() * 1.0));

			datas.add(sub);

			sub = new ArrayList();
			sub.add("");
			sub.add("Rata-Rata");
			sub.add(((1.0 * semua) / (myItems.size() * 1.0)) / (all.size() * 1.0));

			datas.add(sub);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Angket Dosen", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	private void populateTotalPerkuliahan(TreeMap<Long, Perkuliahan> allPerkuliahan,

			Map<String, TreeMap<String, Object[]>> myItemsPerkuliahaan, TreeMap<String, ChecklistPenilaianDosen> all,
			List<List> datas, Label label) {

		for (Perkuliahan perkuliahan : allPerkuliahan.values()) {

			ArrayList sub = new ArrayList();
			sub.add("");
			sub.add("");

			datas.add(sub);

			sub = new ArrayList();
			sub.add("");
			sub.add(perkuliahan.infoSimple());

			datas.add(sub);

			for (Dosen dosen : perkuliahan.populateDosenBuNama()) {

				Set<Long> myItems = new TreeSet<Long>();

				for (String s : myItemsPerkuliahaan.keySet()) {
					if (s.endsWith("_" + perkuliahan.getId() + "_" + dosen.getId())) {
						myItems.add(Long.parseLong(s.split("_")[0]));
					}
				}

				sub = new ArrayList();
				sub.add("");
				sub.add("");
				for (Long mhs : myItems) {
					sub.add("");
				}
				datas.add(sub);

				sub = new ArrayList();
				sub.add("");
				sub.add(dosen.getNama());
				for (Long mhs : myItems) {
					sub.add("");
				}
				datas.add(sub);

				Long jdl = null;

				Map<Long, Integer> mapsTotal = new HashMap<Long, Integer>();
				int nomor = 1;
				for (ChecklistPenilaianDosen checklistPenilaianDosen : all.values()) {

					if (jdl == null || (checklistPenilaianDosen.getGrupChecklistPenilaianDosen() != null
							&& !jdl.equals(checklistPenilaianDosen.getGrupChecklistPenilaianDosen().getId()))) {

						sub = new ArrayList();
						sub.add("");
						sub.add(checklistPenilaianDosen.getGrupChecklistPenilaianDosen().getIsi());
						for (Long mhs : myItems) {
							sub.add("");
						}
						datas.add(sub);

						sub = new ArrayList();
						sub.add("No.");
						sub.add("Judul");

						for (Long mhs : myItems) {
							Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), mhs);
							sub.add(mahasiswa.getNim() + "-" + mahasiswa.getNama());
						}

						datas.add(sub);

						jdl = checklistPenilaianDosen.getGrupChecklistPenilaianDosen().getId();
						nomor = 1;
					}

					sub = new ArrayList();

					try {
						String key = checklistPenilaianDosen.ambilkey();
						label.setValue("Sedang memproses data " + key);

						sub.add(nomor);
						sub.add(checklistPenilaianDosen.getIsi());

						for (Long mhs : myItems) {

							TreeMap<String, Object[]> treeMap = myItemsPerkuliahaan
									.get(mhs + "_" + perkuliahan.getId() + "_" + dosen.getId());

							if (treeMap != null) {
								Object[] objects = treeMap.get(key);
								if (objects != null) {

									Integer nilai = (Integer) objects[1];
									sub.add(nilai);

									Integer totalSemua = mapsTotal.get(mhs);
									if (totalSemua == null) {
										totalSemua = 0;
									}

									totalSemua += nilai;
									mapsTotal.put(mhs, totalSemua);

								} else {
									sub.add(0.0);
								}
							} else {
								sub.add(0.0);
							}
						}

						System.out.println("sub =>" + sub);
						datas.add(sub);

						nomor++;

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Angket Dosen", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
					}

				}

				try {
					sub = new ArrayList();
					sub.add("");
					sub.add("Total");

					Integer semua = 0;

					for (Long mhs : myItems) {
						Integer totalSemua = mapsTotal.get(mhs);
						if (totalSemua == null) {
							totalSemua = 0;
						}

						sub.add(totalSemua);

						semua += totalSemua;
					}

					datas.add(sub);

					sub = new ArrayList();
					sub.add("");
					sub.add("Total Semua");
					sub.add(semua);

					datas.add(sub);

					sub = new ArrayList();
					sub.add("");
					sub.add("Rata-Rata Total");
					sub.add((1.0 * semua) / (myItems.size() * 1.0));

					datas.add(sub);

					sub = new ArrayList();
					sub.add("");
					sub.add("Rata-Rata");
					sub.add(((1.0 * semua) / (myItems.size() * 1.0)) / (all.size() * 1.0));

					datas.add(sub);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanRekapAngketDosen.java:801");
					// TODO: handle exception
				}

			}
		}
	}
}
