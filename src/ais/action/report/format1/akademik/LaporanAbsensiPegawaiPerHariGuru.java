package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.CommonPayroll;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.rab.SatuanKerja;
import ais.database.util.ParallelTaskExecutor;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanAbsensiPegawaiPerHariGuru extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private MyDatebox mulai;
	private MyDatebox sampai;

	private Center center;

	private AmbilDataSatuanKerjaBanbox searchSatker;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Toolbar toolbar;

	// private Combobox tahun;

	private Date date = null;

	@SuppressWarnings("rawtypes")
	private List maps = null;

	private MyCheckboxConfig[] haris;

	private Pegawai pegawai;

	private Desktop desktop;

	public LaporanAbsensiPegawaiPerHariGuru() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Absensi Pegawai Per Hari Guru", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAbsensiPegawaiPerHariGuru(Pegawai pegawai) {
		super();
		try {
			this.pegawai = pegawai;
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Absensi Pegawai Per Hari Guru", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAbsensiPegawaiPerHariGuru(Date date) {
		super();
		this.date = date;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Absensi Pegawai Per Hari Guru", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAbsensiPegawaiPerHariGuru(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() throws Exception {

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setVisible(pegawai == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("Satuan Kerja")));
		row.appendChild(searchSatker = new AmbilDataSatuanKerjaBanbox());
		searchSatker.setWidth("90%");
		searchSatker.setReadonly(true);

		Calendar calendarUtama = ais.ui.util.WaktuUtil.getCalendar();
		if (date != null) {
			calendarUtama.setTime(date);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
			row.appendChild(mulai = new MyDatebox(calendarUtama.getTime()));
			mulai.setReadonly(true);

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
			row.appendChild(sampai = new MyDatebox(calendar.getTime()));
			sampai.setReadonly(true);
		} else {

			int tanggalMulaiAbsensi = 1;
			try {
				tanggalMulaiAbsensi = Integer.parseInt(Common.getKonfigurasi("tanggal_mulai_absensi", "1").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAbsensiPegawaiPerHariGuru.java:179");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Absensi Pegawai Per Hari Guru", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
			calendarUtama = ais.ui.util.WaktuUtil.getCalendar();
			calendarUtama.set(Calendar.MONTH, calendarUtama.get(Calendar.MONTH) - 1);
			calendarUtama.set(Calendar.DATE, tanggalMulaiAbsensi);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
			row.appendChild(mulai = new MyDatebox(calendarUtama.getTime()));
			mulai.setReadonly(true);

			calendarUtama.set(Calendar.MONTH, calendarUtama.get(Calendar.MONTH) + 1);
			calendarUtama.set(Calendar.DATE, calendarUtama.get(Calendar.DATE) - 1);
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
			row.appendChild(sampai = new MyDatebox(calendarUtama.getTime()));
			sampai.setReadonly(true);

		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari Aktif"));
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		String hariDefaultTidakAktif = Common.getKonfigurasi("hari_default_tidak_aktif", ",1,7,").getNilai();

		haris = new MyCheckboxConfig[Common.haris.length];
		int hari = 1;
		for (String h : Common.haris) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(haris[hari - 1] = new MyCheckboxConfig(h));
			haris[hari - 1].setChecked(!hariDefaultTidakAktif.contains("," + hari + ","));
			haris[hari - 1].setValue(h);
			haris[hari - 1].setAttribute("hari", hari);
			hari++;
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onKHS(event);
			}
		});
		print.setParent(row);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				Map parameters = generateParameter();
				return parameters;
			}
		}, "Laporan_Absensi_Per_Hari_Guru", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		Map parameters = ais.common.HashMapGenerator.getRand();
		if (maps != null) {
			parameters.put("maps", maps);
		}
		parameters.put("mulai", Common.dateFormat1.get().format(mulai.getValue()));
		parameters.put("sampai", Common.dateFormat1.get().format(sampai.getValue()));

		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generateDataDanImageAlbum(final Label label) throws Exception {

		SatuanKerja parent = (SatuanKerja) searchSatker.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null && pegawai == null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = null;
		List<Pegawai> pegawais = new ArrayList<Pegawai>();
		List<CutiDanIzin> cutiDanIzinsSemua = new ArrayList<CutiDanIzin>();
		Map<String, StatuskehadiranKaryawanHarian> statusHarianMap = new HashMap<String, StatuskehadiranKaryawanHarian>();

		// 1. BLOK PENGAMBILAN DATA (Aman dengan Try-Finally)
		try {
			session = ais.action.report.Report.openNativeSession();
			pegawais = ConstantValues.simpleList(session.createCriteria(Pegawai.class)
					.add(Restrictions.eq("statusPegawai", ConstantValues.AKTIF_PEGAWAI))
					.createAlias("tipePegawai", "tipePegawai", Criteria.LEFT_JOIN)
					.add(Restrictions.or(Restrictions.isNull("tipePegawai.masukPresensi"),
							Restrictions.eq("tipePegawai.masukPresensi", true)))
					.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(
									parent == null ? Restrictions.isNull("satuanKerja")
											: Restrictions.sqlRestriction("false"),
									Restrictions.in("satuanKerja", satuanKerjas)))
					.add(Restrictions.isNotNull("guru"))
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.addOrder(Order.asc("dosen")).addOrder(Order.asc("guru")).addOrder(Order.asc("satuanKerja"))
					.addOrder(Order.asc("nama")), Pegawai.class);

			if (!pegawais.isEmpty()) {
				cutiDanIzinsSemua = session.createCriteria(CutiDanIzin.class)
						.add(Restrictions.or(Restrictions.between("mulai", mulai.getValue(), sampai.getValue()),
								Restrictions.between("sampai", mulai.getValue(), sampai.getValue())))
						.addOrder(Order.asc("mulai")).add(Restrictions.in("pegawai", pegawais))
						.add(Restrictions.eq("setujui", true)).list();

				statusHarianMap = CommonPayroll.getDefaultStatuskehadiranKaryawanHarian(cutiDanIzinsSemua,
						mulai.getValue(), sampai.getValue(), pegawais, session, true);
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanAbsensiPegawaiPerHariGuru.java:318");
			throw e;
		} finally {
			ais.action.report.Report.closeNativeSession(session);
			ais.action.report.Report.closeCurrentSessionQuietly();
		}

		// 2. PERSIAPAN VARIABEL FINAL UNTUK THREADING
		final Date dateMulai = mulai.getValue();
		final Date dateSampai = sampai.getValue();
		final Date sekarang = WaktuUtil.getDate();
		final Map<String, StatuskehadiranKaryawanHarian> statuskehadiranKaryawanHarians = statusHarianMap;

		// Hitung perkiraan size untuk progres bar UI
		int tempSize = 0;
		if (!pegawais.isEmpty()) {
			Calendar c = ais.ui.util.WaktuUtil.getCalendar();
			c.setTime(dateMulai);
			ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(c);
			Calendar s = ais.ui.util.WaktuUtil.getCalendar();
			s.setTime(dateSampai);
			ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(s);
			s.add(Calendar.DATE, 1);
			while (c.getTime().before(s.getTime())) {
				c.add(Calendar.DATE, 1);
				tempSize++;
			}
		}

		final int totalDays = tempSize == 0 ? 1 : tempSize;
		final int totalTasks = pegawais.size() * totalDays;
		final java.util.concurrent.atomic.AtomicInteger currentIndex = new java.util.concurrent.atomic.AtomicInteger(0);

		// Synchronized List untuk menampung hasil eksekusi dari multithread
		final List<Map> finalMaps = java.util.Collections.synchronizedList(new ArrayList<Map>());

		// 3. EKSEKUSI PARALEL (Maksimal 100 Thread)
		ParallelTaskExecutor.process(pegawais, ParallelTaskExecutor.getDefaultReportMaxThreads(), new ParallelTaskExecutor.Task<Pegawai>() {
			@Override
			public void execute(final Pegawai pegawai) throws Exception {

				if (pegawai != null && pegawai.getTipePegawai() != null
						&& !pegawai.getTipePegawai().getMasukPresensi()) {
					// Lompati proses jika presensi tidak dihitung
					currentIndex.addAndGet(totalDays);
					return;
				}

				// Variabel diletakkan di dalam Thread agar aman (Thread-Safe)
				long tidakHadir = 0L;
				long tidakHadirTanpaHoliday = 0L;

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(dateMulai);
				ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(calendar);

				Calendar s = ais.ui.util.WaktuUtil.getCalendar();
				s.setTime(dateSampai);
				ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(s);
				s.add(Calendar.DATE, 1);

				// Perulangan per-hari untuk pegawai bersangkutan
				while (calendar.getTime().before(s.getTime())) {
					Date tanggal = calendar.getTime();
					calendar.add(Calendar.DATE, 1); // Majukan tanggal di awal agar aman dari "continue"

					int currIdx = currentIndex.incrementAndGet();

					// [ZK 5.5] Update Progress Bar UI (Update setiap beberapa iterasi agar tidak
					// lag)
					if (currIdx % 15 == 0 || currIdx == totalTasks) {
						try {
							org.zkoss.zk.ui.Executions.activate(desktop);
							try {
								// Gunakan blok Synchronized pada SimpleDateFormat bawaan Common yang tidak
								// thread-safe
								String tglProgress;

								tglProgress = Common.dateFormat6.get().format(tanggal);

								label.setValue("Memproses data " + pegawai.getNama() + " tanggal " + tglProgress + " ("
										+ Common.numberFormat.get().format((currIdx * 100.0) / totalTasks) + "%)");
							} finally {
								org.zkoss.zk.ui.Executions.deactivate(desktop);
							}
						} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAbsensiPegawaiPerHariGuru.java:403");
							// Abaikan jika interruptedException
						}
					}

					Integer hari = calendar.get(Calendar.DAY_OF_WEEK) - 1; // Konversi ke index base-0 karena sudah
																			// ditambah 1 di atas
					if (hari == 0)
						hari = 7; // Sesuaikan mapping hari

					// Pengamanan akses SimpleDateFormat Common
					String keyMap;
					keyMap = Common.dateFormat83.get().format(tanggal) + "_" + pegawai.getId();

					StatuskehadiranKaryawanHarian skh = statuskehadiranKaryawanHarians.get(keyMap);
					boolean adaHadir = (skh != null && skh.getStatusabsensi() != null
							&& skh.getStatusabsensi().getId().equals(1L));

					if (!adaHadir) {
						// Fallback mapping array hari
						int indexHari = hari - 1;
						if (indexHari >= 0 && indexHari < haris.length && !haris[indexHari].isChecked()) {
							continue;
						}
					}

					boolean holiday = Common.isHoliday(tanggal);

					if (skh == null) {
						skh = new StatuskehadiranKaryawanHarian();
						skh.setTanggal(tanggal);
						skh.setPegawai(pegawai);
						skh.setKeterangan("");
						skh.setMasukjam(null);
						skh.setPulangJam(null);
						skh.setMinggu(hari);
						skh.setStatusabsensi(tanggal.before(sekarang) ? ConstantValues.TIDAK_ADA_ALASAN
								: ConstantValues.BELUM_ABSEN);
					}

					if (skh.isTidakHadirTanpaHoliday(adaHadir, skh.getCutiDanIzin(), skh.getLiburNasional())) {
						tidakHadirTanpaHoliday++;
					}

					if (skh.isTidakHadirEfektif(adaHadir, holiday, skh.getCutiDanIzin(), skh.getLiburNasional())) {
						tidakHadir++;
					}

					Map map = new java.util.HashMap();
					map.put("foto_datang", skh.getFotoAbsenDatang());
					map.put("foto_pulang", skh.getFotoAbsenPulang());
					map.put("lokasi_datang", skh.getLokasiAbsenDatang());
					map.put("lokasi_pulang", skh.getLokasiAbsenPulang());
					map.put("apakah_dosen", pegawai.getDosen() != null);
					map.put("nama_satuan_kerja",
							pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama());
					map.put("pegawai", pegawai.getId());
					map.put("nama", pegawai.getNama());
					map.put("nip", pegawai.getMycode());

					Statusabsensi statusabsensi = skh.getStatusabsensi();
					if (ConstantValues.kehadiranHarusMulaiDanSampai) {
						if (skh.getMasukjam() == null || skh.getPulangJam() == null) {
							statusabsensi = ConstantValues.BELUM_ABSEN;
						}
					}

					map.put("keterangan",
							(statusabsensi == null ? "" : statusabsensi.getNama()) + " " + skh.getKeterangan());
					map.put("jumlahTerlambat", skh.getJumlahTerlambat());
					map.put("jumlahCepatKeluar", skh.getJumlahCepatKeluar());

					Double lemburDa = skh.getJumlahLemburMasuk();
					Date lemburMulai = skh.getLamburMulai();
					Date lemburSampai = skh.getLamburSampai();

					map.put("jumlahLemburMasuk", lemburDa);
					map.put("tidakHadir", tidakHadir);
					map.put("tidakHadirTanpaHoliday", tidakHadirTanpaHoliday);

					map.put("masuk",
							skh.ambilMasukjam() == null ? "" : Common.timeFormat.get().format(skh.ambilMasukjam()));
					map.put("pulang",
							skh.ambilPulangjam() == null ? "" : Common.timeFormat.get().format(skh.ambilPulangjam()));
					map.put("lemburMulai", lemburMulai == null ? "" : Common.timeFormat.get().format(lemburMulai));
					map.put("lemburSampai", lemburSampai == null ? "" : Common.timeFormat.get().format(lemburSampai));
					map.put("hari", Common.dateFormat6.get().format(tanggal));
					map.put("tanggal", Common.dateFormat1.get().format(tanggal));
					map.put("jam_kerja", skh.getDetailJenisShiftPegawai() == null ? ""
							: Common.timeFormat.get().format(skh.getDetailJenisShiftPegawai().getMulai()) + " - "
									+ Common.timeFormat.get().format(skh.getDetailJenisShiftPegawai().getSampai()));

					finalMaps.add(map);
				}
			}
		});

		// 4. BERSIHKAN MEMORI
		this.maps = new ArrayList<Map>(finalMaps);
		statusHarianMap.clear();
		cutiDanIzinsSemua.clear();

		// Kembalikan label UI ke posisi akhir
		ais.action.report.helper.LoadingReportUtil.selesai(label);
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				Map parameters = generateParameter();
				parameters.put("maps", maps);
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Laporan_Absensi_Per_Hari_Guru",
						ais.ui.util.WaktuUtil.getDate(), null, toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

		desktop = Executions.getCurrent().getDesktop();
		if (!desktop.isServerPushEnabled()) {
			desktop.enableServerPush(true);
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					generateDataDanImageAlbum(label);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanAbsensiPegawaiPerHariGuru.java:537");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Absensi Pegawai Per Hari Guru", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}).start();

	}

}
