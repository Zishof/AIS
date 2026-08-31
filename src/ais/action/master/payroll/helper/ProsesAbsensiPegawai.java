package ais.action.master.payroll.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.West;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.report.Report;
import ais.action.report.format1.payroll.LaporanCutiPegawai;
import ais.common.Common;
import ais.common.CommonPayroll;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CutiBersama;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.KehadiranPegawaiBulanan;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.PengajuanPegawai;
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

/**
 * Layar "Proses Absensi Pegawai" modul payroll: menghitung rekap kehadiran bulanan pegawai
 * (aktif/masuk/alpa/sakit/izin/belum/tidak-hadir/terlambat/pulang-cepat/lembur/masuk-di-hari-libur,
 * digabung dengan cuti dan pengajuan pegawai yang disetujui) untuk periode terpilih — perhitungan
 * memakai pola konkurensi yang sama dengan {@link ais.action.report.format1.payroll.LaporanAbsensiPegawai}
 * (cache libur nasional, pengelompokan cuti/pengajuan per pegawai, eksekusi paralel lewat
 * {@link ParallelTaskExecutor}, server push dikelola lewat
 * {@link ais.common.AsyncTaskManager#jalankanDenganPush}) — lalu menampilkan hasilnya sebagai grid
 * rekap yang dapat dicetak PDF.
 *
 * <p>
 * <b>Berbeda dari layar laporan sejenis</b>: kelas ini menyediakan tombol "Proses Sebagai
 * Kehadiran Pegawai bulan ..." yang <b>menulis hasil perhitungan ke database</b> — setiap baris
 * rekap disimpan/dimutakhirkan sebagai satu baris {@link KehadiranPegawaiBulanan} (kunci
 * pegawai+bulan+tahun) dalam satu transaksi Hibernate, menjadikan rekap bulanan ini sumber data
 * resmi untuk proses payroll selanjutnya (mis. perhitungan potongan/tunjangan kehadiran). Bulan
 * dan tahun yang dipakai sebagai kunci penyimpanan diambil dari tanggal akhir rentang filter
 * ({@code sampai}), BUKAN dari rentang keseluruhan — penting diperhatikan bila rentang filter
 * melewati batas bulan.
 * </p>
 */
public class ProsesAbsensiPegawai extends MyWindow {

	private static final long serialVersionUID = -397946194166101691L;

	private Checkbox[] haris;
	private Center centerUtama;

	private MyDatebox mulai;
	private MyDatebox sampai;

	@SuppressWarnings("rawtypes")
	private List<Map> maps = null;

	private AmbilDataPegawaiBanbox searchparent;
	private AmbilDataSatuanKerjaBanbox searchSatker;

	private MyCheckboxConfig hanyaDosen;
	private MyCheckboxConfig hanyaPegawai;
	private Combobox ikatanDinasDosen;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Pegawai pegawai;
	private MyCheckboxConfig hanyaGuru;

	private MyCheckboxConfig abaikanKehadiranJikaHariTidakTerpilih;

	/** Membuat layar proses untuk seluruh pegawai (tanpa pra-filter satu pegawai tertentu). */
	public ProsesAbsensiPegawai() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Membuat layar proses dibatasi ke satu {@code pegawai} tertentu (filter satuan kerja/pegawai pada panel disembunyikan). */
	public ProsesAbsensiPegawai(Pegawai pegawai) {
		super();
		this.pegawai = pegawai;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Konstruktor varian dengan judul/border/closable eksplisit, dipakai saat jendela dibuat sebagai komponen tersemat. */
	public ProsesAbsensiPegawai(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	/** Membangun panel filter (satuan kerja, pegawai, jenis pegawai, ikatan kerja, rentang tanggal default sebulan penuh, checkbox hari aktif) dan area hasil di sisi barat/tengah layar. */
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
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setWidth("20%");
		column.setParent(columns);
		column = new Column();
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

		row = new MyFormRow();
		row.setVisible(pegawai == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("Pegawai")));
		row.appendChild(searchparent = new AmbilDataPegawaiBanbox(true));
		searchparent.setWidth("90%");
		searchparent.setReadonly(true);

		if (pegawai == null) {
			Common.initKeterangan(rows, "Kosongkan data pegawai untuk mencetak data semua pegawai");
		}

		boolean tampilanPilihanHanyaDosenDanGuruSaja = Common.bolehKonfigurasi("tampilan_pilihan_hanya_dosen_dan_guru_saja");

		row = new MyFormRow();
		row.setVisible(pegawai == null && tampilanPilihanHanyaDosenDanGuruSaja);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hanyaDosen = new MyCheckboxConfig("Hanya dosen saja"));

		row = new MyFormRow();
		row.setVisible(pegawai == null && tampilanPilihanHanyaDosenDanGuruSaja);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hanyaGuru = new MyCheckboxConfig("Hanya guru saja"));

		row = new MyFormRow();
		row.setVisible(pegawai == null && tampilanPilihanHanyaDosenDanGuruSaja);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hanyaPegawai = new MyCheckboxConfig("Hanya pegawai, bukan dosen dan guru"));

		row = new MyFormRow();
		row.setVisible(pegawai == null && tampilanPilihanHanyaDosenDanGuruSaja);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ikatan Kerja"));
		row.appendChild(ikatanDinasDosen = new Combobox());
		Common.insertComboDanSemua(ikatanDinasDosen, "nama", IkatanKerjaDosen.class, Restrictions.eq("aktif", true));

		int tanggalMulaiAbsensi = 1;
		try {
			tanggalMulaiAbsensi = Integer.parseInt(Common.getKonfigurasi("tanggal_mulai_absensi", "1").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/ProsesAbsensiPegawai.java:195");
		}

		Calendar calendarUtama = ais.ui.util.WaktuUtil.getCalendar();
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

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Hari Aktif")));
		row.appendChild(new Label(""));

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

		abaikanKehadiranJikaHariTidakTerpilih = ais.action.master.helper.KehadiranPresensiUtil
				.buatCheckboxAbaikanKehadiranHariTidakTerpilih(rows);

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

		centerUtama = new Center();
		centerUtama.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(centerUtama, true);
	}

	/** Menyusun peta parameter laporan (indeks hari aktif, rentang tanggal, dan {@link #maps} hasil {@link #generateDataDanImageAlbum} bila sudah tersedia) untuk mesin laporan {@link Report}. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		Map parameters = ais.common.HashMapGenerator.getRand();
		int i = 0;
		for (Checkbox checkbox : haris) {
			Integer hari = (Integer) checkbox.getAttribute("hari");
			parameters.put("hari" + i, checkbox.isChecked() ? hari : -1);
			i++;
		}
		if (maps != null) {
			parameters.put("maps", maps);
		}
		parameters.put("mulai", Common.dateFormat1.get().format(mulai.getValue()));
		parameters.put("sampai", Common.dateFormat1.get().format(sampai.getValue()));

		return parameters;
	}

	
	/**
	 * Menghitung rekap kehadiran seluruh pegawai yang lolos filter untuk rentang tanggal terpilih
	 * dan menyimpan hasilnya ke {@link #maps} (read-only sampai pengguna menekan tombol proses).
	 * Alur: (1) memuat pegawai aktif, cuti-bersama tahun berjalan, cuti/izin, status kehadiran
	 * harian, dan pengajuan pegawai (lembur/dinas dll.) yang disetujui dan relevan dalam satu sesi
	 * native yang selalu ditutup di {@code finally}; (2) mengelompokkan cuti dan pengajuan per
	 * pegawai untuk akses O(1); (3) memproses setiap pegawai paralel — untuk setiap hari, menentukan
	 * status kehadiran, ketepatan waktu, jam lembur (hanya dihitung bila ada pengajuan lembur
	 * disetujui pada tanggal tersebut), dan masuk di hari libur; (4) menggabungkan hasil ke
	 * {@link #maps} dan memperbarui {@code label} progres lewat server push.
	 *
	 * @param label komponen label UI untuk menampilkan progres
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generateDataDanImageAlbum(final Label label) throws Exception {

		SatuanKerja parent = (SatuanKerja) searchSatker.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null && pegawai == null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Pegawai peg = pegawai == null ? (Pegawai) searchparent.getAttribute("pegawai") : pegawai;
		IkatanKerjaDosen ikatanKerjaDosenData = (IkatanKerjaDosen) (ikatanDinasDosen.getSelectedItem() == null ? null
				: ikatanDinasDosen.getSelectedItem().getValue());

		final Date[] rangeTanggal = ais.action.master.helper.KehadiranPresensiUtil.normalisasiRentangTanggal(mulai.getValue(), sampai.getValue());
		final Date dateMulai = rangeTanggal[0];
		final Date dateSampai = rangeTanggal[1];

		Session session = null;
		List<Pegawai> pegawais = new ArrayList<Pegawai>();
		CutiBersama cutiBersamaData = null;
		List<CutiDanIzin> cutiDanIzinsSemua = new ArrayList<CutiDanIzin>();
		List<PengajuanPegawai> pengajuanPegawais = new ArrayList<PengajuanPegawai>();
		Map<String, StatuskehadiranKaryawanHarian> statusHarianMap = new HashMap<String, StatuskehadiranKaryawanHarian>();

		

		// 1. BLOK PENGAMBILAN DATA UTAMA (Aman dengan Finally)
		try {
			session = HibernateUtil.currentNativeSession();

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
					.add(hanyaGuru.isChecked() ? Restrictions.isNotNull("guru") : Restrictions.sqlRestriction("true"))
					.add(hanyaDosen.isChecked() ? Restrictions.isNotNull("dosen") : Restrictions.sqlRestriction("true"))
					.add(hanyaPegawai.isChecked()
							? Restrictions.and(Restrictions.isNull("dosen"), Restrictions.isNull("guru"))
							: Restrictions.sqlRestriction("true"))
					.add(ikatanKerjaDosenData == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("ikatanKerjaDosen", ikatanKerjaDosenData))
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.add(peg == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("id", peg.getId()))
					.addOrder(Order.asc("dosen")).addOrder(Order.asc("guru")).addOrder(Order.asc("departemen"))
					.addOrder(Order.asc("nama")), Pegawai.class);

			Calendar calTemp = ais.ui.util.WaktuUtil.getCalendar();
			calTemp.setTime(dateMulai);
			int tahun = calTemp.get(Calendar.YEAR);

			Calendar calTemp1 = ais.ui.util.WaktuUtil.getCalendar();
			calTemp1.setTime(dateMulai);
			calTemp1.set(Calendar.MONTH, calTemp1.get(Calendar.MONTH) - 1);

			cutiBersamaData = (CutiBersama) session.createCriteria(CutiBersama.class).add(Restrictions.eq("tahun", tahun))
					.setMaxResults(1).uniqueResult();
			if (cutiBersamaData == null) {
				cutiBersamaData = new CutiBersama();
			}

			if (!pegawais.isEmpty()) {
				cutiDanIzinsSemua = session.createCriteria(CutiDanIzin.class)
						.add(Restrictions.or(Restrictions.between("mulai", dateMulai, dateSampai),
								Restrictions.between("sampai", dateMulai, dateSampai)))
						.addOrder(Order.asc("mulai")).add(Restrictions.in("pegawai", pegawais))
						.add(Restrictions.eq("setujui", true)).list();

				statusHarianMap = CommonPayroll.getDefaultStatuskehadiranKaryawanHarian(
						cutiDanIzinsSemua, dateMulai, dateSampai, pegawais, session, true);

				pengajuanPegawais = session.createCriteria(PengajuanPegawai.class)
						.createAlias("jenisPengajuanPegawai", "jenisPengajuanPegawai")
						.add(Restrictions.or(Restrictions.isNull("jenisPengajuanPegawai.masukPresensi"),
								Restrictions.eq("jenisPengajuanPegawai.masukPresensi", true)))
						.add(Restrictions.or(
								Restrictions.sqlRestriction(
										"date('" + Common.databaseDateFormat.get().format(WaktuUtil.getDate())
												+ "') between date(this_.waktu) and date(this_.waktusampai)"),
								Restrictions.or(
										Restrictions.between("waktuSampai", calTemp1.getTime(), dateSampai),
										Restrictions.between("waktu", calTemp1.getTime(), dateSampai))))
						.addOrder(Order.asc("waktu")).add(Restrictions.in("pegawai", pegawais))
						.add(Restrictions.eq("setujui", true)).list();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/helper/ProsesAbsensiPegawai.java:369");
			throw e;
		} finally {
			ais.action.master.helper.KehadiranPresensiUtil.closeNativeSession(session);
			HibernateUtil.closeSession();
		}

		// 2. OPTIMASI MEMORI: Kelompokkan Data Cuti & Pengajuan berdasar ID Pegawai
		final Map<Long, List<CutiDanIzin>> mapCutiByPegawai = new HashMap<Long, List<CutiDanIzin>>();
		for (CutiDanIzin c : cutiDanIzinsSemua) {
			if (c.getPegawai() != null) {
				Long pId = c.getPegawai().getId();
				if (!mapCutiByPegawai.containsKey(pId)) {
					mapCutiByPegawai.put(pId, new ArrayList<CutiDanIzin>());
				}
				mapCutiByPegawai.get(pId).add(c);
			}
		}

		final Map<Long, List<PengajuanPegawai>> mapPengajuanByPegawai = new HashMap<Long, List<PengajuanPegawai>>();
		for (PengajuanPegawai p : pengajuanPegawais) {
			if (p.getPegawai() != null) {
				Long pId = p.getPegawai().getId();
				if (!mapPengajuanByPegawai.containsKey(pId)) {
					mapPengajuanByPegawai.put(pId, new ArrayList<PengajuanPegawai>());
				}
				mapPengajuanByPegawai.get(pId).add(p);
			}
		}

		// 3. PERSIAPAN VARIABEL FINAL UNTUK MULTI-THREAD
		final int size = pegawais.size();
		final Date sekarang = WaktuUtil.getDate();
		
		Calendar calFinalTahun = ais.ui.util.WaktuUtil.getCalendar();
		calFinalTahun.setTime(dateMulai);
		final int tahunFinal = calFinalTahun.get(Calendar.YEAR);
		
		final CutiBersama cutiBersama = cutiBersamaData;
		final Map<String, StatuskehadiranKaryawanHarian> statuskehadiranKaryawanHarians = statusHarianMap;

		final List<Map> finalMaps = java.util.Collections.synchronizedList(new ArrayList<Map>());
		final java.util.concurrent.atomic.AtomicInteger currentIndex = new java.util.concurrent.atomic.AtomicInteger(0);

		// 4. EKSEKUSI PARALEL TERKONTROL
		ParallelTaskExecutor.process(pegawais, ais.action.master.helper.KehadiranPresensiUtil.DEFAULT_PARALLEL_BATCH_SIZE, new ParallelTaskExecutor.Task<Pegawai>() {
			@Override
			public void execute(final Pegawai pegw) throws Exception {

				if (pegw != null && pegw.getTipePegawai() != null && !pegw.getTipePegawai().getMasukPresensi()) {
					return;
				}

				// Update UI (Server Push Aman)
				int currIdx = currentIndex.incrementAndGet();
				if (currIdx % 5 == 0 || currIdx == size) { // Update tiap 5 iterasi agar ringan
					try {
						org.zkoss.zk.ui.Executions.activate(desktop);
						try {
							label.setValue("Memproses data " + pegw.getNama() + " ("
									+ Common.numberFormat.get().format((currIdx * 100.0) / size) + "%)");
						} finally {
							org.zkoss.zk.ui.Executions.deactivate(desktop);
						}
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/ProsesAbsensiPegawai.java:433");
						// Abaikan jika interruptedException
					}
				}

				// Ambil Cuti secara Instan (O(1))
				List<CutiDanIzin> cutiDanIzins = mapCutiByPegawai.get(pegw.getId());
				if (cutiDanIzins == null) {
					cutiDanIzins = new ArrayList<CutiDanIzin>();
				}

				// Ambil Pengajuan secara Instan (O(1)) dan mapping per tanggal
				List<PengajuanPegawai> myPengajuans = mapPengajuanByPegawai.get(pegw.getId());
				Map<String, PengajuanPegawai> pengajuanPegawaisData = new HashMap<String, PengajuanPegawai>();

				if (myPengajuans != null) {
					for (PengajuanPegawai p : myPengajuans) {
						Calendar calendarSub = ais.ui.util.WaktuUtil.getCalendar();
						calendarSub.setTime(p.getWaktu());

						int ind = 0;
						while (Common.dateFormat8.get().format(calendarSub.getTime()).equals(Common.dateFormat8.get().format(p.getWaktuSampai()))
								|| calendarSub.getTime().before(p.getWaktuSampai())) {
							ind++;
							if (ind > 5000) break;

							pengajuanPegawaisData.put(Common.dateFormat83.get().format(calendarSub.getTime()), p);
							calendarSub.add(Calendar.DATE, 1);
						}
					}
				}

				Map map = new java.util.HashMap();
				map.put("apakah_dosen", pegw.getDosen() != null);
				map.put("nama_satuan_kerja", pegw.getSatuanKerja() == null ? "" : pegw.getSatuanKerja().getNama());
				map.put("pegawai", pegw.getId());
				map.put("nama", pegw.getNama());
				map.put("nip", pegw.getMycode());
				map.put("nama_dept", pegw.getDepartemen() == null ? "" : pegw.getDepartemen().getNama());
				map.put("dept", pegw.getDepartemen() == null ? 0L : pegw.getDepartemen().getId());

				long masuk = 0L;
				long tidakHadir = 0L;
				long tidakHadirTanpaHoliday = 0L;
				long alpa = 0L;
				long sakit = 0L;
				long izin = 0L;
				long belum = 0L;
				long lain = 0L;
				long tidakAbsenPulang = 0L;
				long jumlahHariEfektif = 0L;
				double lembur = 0.0;
				long tepatWaktu = 0L;
				long tepatWaktuBanget = 0L;
				long masukDihariLibur = 0L;
				long terlambat = 0L;
				long pulangcepat = 0L;
				Map<String, Long> cutis = new HashMap<String, Long>();
				long aktif = 0L;

				// PENTING: Inisialisasi Calendar HARUS di dalam Task execute()
				// agar tidak saling menimpa data antar thread (Thread-Safety).
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(dateMulai);
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);

				Calendar s = ais.ui.util.WaktuUtil.getCalendar();
				s.setTime(dateSampai);
				s.set(Calendar.HOUR_OF_DAY, 0);
				s.set(Calendar.MINUTE, 0);
				s.set(Calendar.SECOND, 0);
				s.set(Calendar.MILLISECOND, 0);

				while (calendar.compareTo(s) <= 0) {

					Date tanggal = calendar.getTime();
					Integer hari = calendar.get(Calendar.DAY_OF_WEEK);

					StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = statuskehadiranKaryawanHarians
							.get(Common.dateFormat83.get().format(tanggal) + "_" + pegw.getId());
					boolean adaHadir = (statuskehadiranKaryawanHarian != null
							&& statuskehadiranKaryawanHarian.getStatusabsensi() != null
							&& statuskehadiranKaryawanHarian.getStatusabsensi().getId().equals(1L));

					boolean holiday = Common.isHoliday(tanggal);
					if ((!holiday && haris[hari - 1].isChecked())) {
						aktif++;
					}

					if (ais.action.master.helper.KehadiranPresensiUtil
							.harusLewatiTanggalKarenaHariTidakDipilih(haris, hari, adaHadir, abaikanKehadiranJikaHariTidakTerpilih)) {
						calendar.add(Calendar.DATE, 1);
						continue;
					}

					if (statuskehadiranKaryawanHarian == null) {
						statuskehadiranKaryawanHarian = new StatuskehadiranKaryawanHarian();
						statuskehadiranKaryawanHarian.setTanggal(tanggal);
						statuskehadiranKaryawanHarian.setPegawai(pegw); // Perbaikan: Gunakan pegw (dari local thread), bukan pegawai dari luar
						statuskehadiranKaryawanHarian.setKeterangan("");
						statuskehadiranKaryawanHarian.setMasukjam(null);
						statuskehadiranKaryawanHarian.setPulangJam(null);

						statuskehadiranKaryawanHarian.setMinggu(hari);
						if (tanggal.before(sekarang)) {
							statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.TIDAK_ADA_ALASAN);
						} else {
							statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.BELUM_ABSEN);
						}
					}

					Statusabsensi statusabsensi = statuskehadiranKaryawanHarian.getStatusabsensi();
					if (ConstantValues.kehadiranHarusMulaiDanSampai) {
						if (statuskehadiranKaryawanHarian.getMasukjam() == null || statuskehadiranKaryawanHarian.getPulangJam() == null) {
							statusabsensi = ConstantValues.BELUM_ABSEN;
						}
					}

					if (statuskehadiranKaryawanHarian.ambilMasukjam() != null && statuskehadiranKaryawanHarian.ambilPulangjam() == null) {
						tidakAbsenPulang++;
					}

					if (statuskehadiranKaryawanHarian.getLiburNasional() == null) {
						jumlahHariEfektif++;
					}

					if (holiday && statuskehadiranKaryawanHarian.getMasukjam() != null) {
						masukDihariLibur++;
					}

					if (statuskehadiranKaryawanHarian.isTidakHadirTanpaHoliday(adaHadir,
							statuskehadiranKaryawanHarian.getCutiDanIzin(),
							statuskehadiranKaryawanHarian.getLiburNasional())) {
						tidakHadirTanpaHoliday++;
					}

					if (statuskehadiranKaryawanHarian.isTidakHadirEfektif(adaHadir, holiday,
							statuskehadiranKaryawanHarian.getCutiDanIzin(),
							statuskehadiranKaryawanHarian.getLiburNasional())) {
						tidakHadir++;
					}

					CutiDanIzin cutiDanIzin = statuskehadiranKaryawanHarian.getCutiDanIzin();

					if (cutiDanIzin != null && cutiDanIzin.getStatusabsensi() != null && cutiDanIzin.getSetujui()) {
						Long c = cutis.get(cutiDanIzin.getStatusabsensi().getNama());
						if (c == null) c = 0L;
						cutis.put(cutiDanIzin.getStatusabsensi().getNama(), c + 1);
					}

					if (adaHadir || cutiDanIzin == null || !cutiDanIzin.getSetujui()) {

						if (statusabsensi.getId().equals(1L)) {
							masuk++;

							if (statuskehadiranKaryawanHarian.getDatangTerlambat()) {
								terlambat++;
							} else if (statuskehadiranKaryawanHarian.getPulangCepat()) {
								pulangcepat++;
							} else {
								tepatWaktu++;
							}

							if (statuskehadiranKaryawanHarian.getMasukjam() != null
									&& statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai() != null
									&& statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai().getMulai() != null
									&& Double.parseDouble(Common.timeFormat2.get().format(statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai().getMulai())) 
										>= Double.parseDouble(Common.timeFormat2.get().format(statuskehadiranKaryawanHarian.getMasukjam()))) {
								tepatWaktuBanget++;
							}

							PengajuanPegawai pengajuanPegawai = pengajuanPegawaisData.get(Common.dateFormat83.get().format(tanggal));
							if (pengajuanPegawai != null && pengajuanPegawai.getSatuanKerjaPengaju() != null) {
								Double lemburDa = statuskehadiranKaryawanHarian.getJumlahLemburMasuk();
								lembur += (lemburDa != null ? lemburDa : 0.0);
							}

						} else if (!holiday && statusabsensi.getId().equals(2L)) {
							alpa++;
						} else if (statusabsensi.getId().equals(3L)) {
							sakit++;
						} else if (statusabsensi.getId().equals(4L)) {
							izin++;
						} else if (statusabsensi.getId().equals(5L)) {
							belum++;
						} else {
							lain++;
						}
					}

					calendar.add(Calendar.DATE, 1);
				}

				for (Object o : ConstantValues.ambilBerdasarClass(Statusabsensi.class).values()) {
					try {
						Statusabsensi sa = (Statusabsensi) o;
						map.put("jml_" + sa.getNama(), 0L);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/ProsesAbsensiPegawai.java:633");}
				}

				for (String skey : cutis.keySet()) {
					map.put("jml_" + skey, cutis.get(skey));
				}

				map.put("aktif", aktif);
				map.put("terlambat", terlambat);
				map.put("pulangcepat", pulangcepat);
				map.put("tepatWaktu", tepatWaktu);
				map.put("tepatWaktuBanget", tepatWaktuBanget);
				map.put("masukDihariLibur", masukDihariLibur);

				map.put("jumlahHariEfektif", jumlahHariEfektif);
				map.put("tidakAbsenPulang", tidakAbsenPulang);
				map.put("masuk", masuk);
				map.put("alpa", alpa);
				map.put("sakit", sakit);
				map.put("izin", izin);
				map.put("belum", belum);
				map.put("lain", lain);
				map.put("lembur", lembur);
				map.put("tidakHadir", tidakHadir);
				map.put("tidakHadirTanpaHoliday", tidakHadirTanpaHoliday);

				int jumlahCuti = pegw.getJatahCutiTahunan() == null ? cutiBersama.getJumlahCuti() : pegw.getJatahCutiTahunan();
				int jumlahCutiYangBisaDiambil = jumlahCuti - cutiBersama.getJumlahCutiBersama();

				LaporanCutiPegawai.generateCutiDanIzinParameter(map, cutiDanIzins, tahunFinal, haris, cutiBersama, jumlahCutiYangBisaDiambil);

				finalMaps.add(map);

			} 
		});

		// 5. ASIGN HASIL AKHIR & BERSIHKAN MEMORI
		this.maps = new ArrayList<Map>(finalMaps);
		
		statusHarianMap.clear();
		cutiDanIzinsSemua.clear();
		mapCutiByPegawai.clear();
		mapPengajuanByPegawai.clear();

		// Update UI di Main Thread
		label.setValue("Proses Selesai.");
	}
	
	
	@SuppressWarnings("rawtypes")
	private Map<Long, Map> dataHadir = new TreeMap<Long, Map>();

	private Desktop desktop;

	/**
	 * Menangani klik "Tampilkan": menghitung rekap kehadiran ({@link #generateDataDanImageAlbum})
	 * di bawah pengelolaan server push, lalu menampilkan grid rekap beserta tombol "Proses Sebagai
	 * Kehadiran Pegawai" untuk bulan/tahun dari tanggal akhir filter. Menekan tombol proses tersebut
	 * <b>menulis</b> setiap baris rekap ke database sebagai {@link KehadiranPegawaiBulanan}
	 * (membuat baru atau memutakhirkan yang sudah ada, kunci pegawai+bulan+tahun) dalam satu
	 * transaksi, lalu mencetak PDF laporan absensi hasil proses tersebut. Lihat javadoc kelas
	 * untuk implikasi penulisan data ini.
	 *
	 * @param event event pemicu tombol "Tampilkan"
	 */
	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({ "rawtypes" })
			@Override
			public void onEvent(Event arg0) throws Exception {

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(sampai.getValue());
				final int bln = calendar.get(Calendar.MONTH);
				final int tahun = calendar.get(Calendar.YEAR);
				final String bulan = Common.BULAN[bln];

				Common.clear(centerUtama);
				dataHadir.clear();
				Borderlayout borderlayout = new Borderlayout();
				borderlayout.setParent(centerUtama);

				North north = new North();
				north.setParent(borderlayout);
				Toolbar toolbar = new Toolbar();
				toolbar.setParent(north);

				Center center = new Center();
				center.setTitle("Rekap Kehadiran Pegawai");
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig(
						"Proses Sebagai Kehadiran Pegawai bulan " + bulan + " tahun " + tahun,
						"/img/svg/check-circled-outline.svg");
				toolbar.appendChild(toolbarbutton);
				toolbarbutton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Session session = null;
						Transaction tx = null;

						try {
							session = HibernateUtil.currentSession();
							tx = session.beginTransaction();

							for (Map map : maps) {
								Long pegawaiId = (Long) map.get("pegawai");
								Long aktif = (Long) map.get("aktif");
								Long masuk = (Long) map.get("masuk");
								Long alpa = (Long) map.get("alpa");
								Long sakit = (Long) map.get("sakit");
								Long izin = (Long) map.get("izin");
								Long belum = (Long) map.get("belum");
								Integer cuti = (Integer) map.get("cuti_bln_" + (bln + 1));

								Long tepatWaktu = (Long) map.get("tepatWaktu");
								Long terlambat = (Long) map.get("terlambat");
								Long pulangcepat = (Long) map.get("pulangcepat");
								Long tidakHadir = (Long) map.get("tidakHadir");
								Long tidakHadirTanpaHoliday = (Long) map.get("tidakHadirTanpaHoliday");

								Double lembur = (Double) map.get("lembur");
								Long masukDihariLibur = (Long) map.get("masukDihariLibur");

								if (pegawaiId == null)
									continue;

								KehadiranPegawaiBulanan kehadiranPegawaiBulanan = (KehadiranPegawaiBulanan) session
										.createCriteria(KehadiranPegawaiBulanan.class)
										.add(Restrictions.eq("bulan", (bln + 1))).add(Restrictions.eq("tahun", tahun))
										.add(Restrictions.eq("pegawai.id", pegawaiId)).setMaxResults(1).uniqueResult();

								if (kehadiranPegawaiBulanan == null) {
									kehadiranPegawaiBulanan = new KehadiranPegawaiBulanan();
								}

								kehadiranPegawaiBulanan.setTidakHadirTanpaHoliday(
										tidakHadirTanpaHoliday == null ? 0 : tidakHadirTanpaHoliday.intValue());
								kehadiranPegawaiBulanan.setTidakHadir(tidakHadir == null ? 0 : tidakHadir.intValue());
								kehadiranPegawaiBulanan.setMasukDihariLibur(
										masukDihariLibur == null ? 0 : masukDihariLibur.intValue());
								kehadiranPegawaiBulanan.setTepatWaktu(tepatWaktu == null ? 0 : tepatWaktu.intValue());
								kehadiranPegawaiBulanan.setTerlambat(terlambat == null ? 0 : terlambat.intValue());
								kehadiranPegawaiBulanan
										.setPulangcepat(pulangcepat == null ? 0 : pulangcepat.intValue());
								kehadiranPegawaiBulanan.setAktif(aktif == null ? 0 : aktif.intValue());
								kehadiranPegawaiBulanan.setBulan((bln + 1));
								kehadiranPegawaiBulanan.setTahun(tahun);
								kehadiranPegawaiBulanan.setPegawai(new Pegawai(pegawaiId));
								kehadiranPegawaiBulanan.setNama(bulan + " " + tahun);
								kehadiranPegawaiBulanan.setKeterangan("kehadiran bulan " + bulan + " " + tahun);

								kehadiranPegawaiBulanan.setMasuk(masuk == null ? 0 : masuk.intValue());
								kehadiranPegawaiBulanan.setAlpa(alpa == null ? 0 : alpa.intValue());
								kehadiranPegawaiBulanan.setSakit(sakit == null ? 0 : sakit.intValue());
								kehadiranPegawaiBulanan.setIzin(izin == null ? 0 : izin.intValue());
								kehadiranPegawaiBulanan.setBelum(belum == null ? 0 : belum.intValue());
								kehadiranPegawaiBulanan.setCuti(cuti == null ? 0 : cuti.intValue());
								kehadiranPegawaiBulanan.setLembur(lembur == null ? 0.0 : lembur);

								Common.refreshSaveOrUpdate(session, kehadiranPegawaiBulanan);
							}
							session.flush();
							tx.commit();
						} catch (Exception e) {
							if (tx != null && tx.isActive()) {
								tx.rollback();
							}
							throw e;
						} finally {
							// currentSession dikelola oleh lifecycle Hibernate/request, sehingga tidak ditutup manual.
						}

						Report.generatePDFReport(Report.PDF, generateParameter(), "payroll/Laporan_Absensi_Pegawai",
								ais.ui.util.WaktuUtil.getDate(), maps);
					}
				});

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");
				grid.setMold("paging");
				grid.setPageSize(20);
				grid.getPagingChild().setMold("os");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig("Unit");
				column.setParent(columns);
				column.setWidth("8%");

				column = new MyColumnConfig("Pegawai");
				column.setWidth("12%");
				column.setParent(columns);

				column = new MyColumnConfig("Aktif {V_AKTIF}");
				column.setParent(columns);
				column.setAlign("right");

				column = new MyColumnConfig("Hadir {V_HDR}");
				column.setParent(columns);
				column.setAlign("right");

				column = new MyColumnConfig("Tdk.Hadir {V_THDR}");
				column.setParent(columns);
				column.setAlign("right");

				column = new MyColumnConfig("Sakit {V_SKT}");
				column.setParent(columns);
				column.setAlign("right");

				column = new MyColumnConfig("Izin {V_IZIN}");
				column.setParent(columns);
				column.setAlign("right");

				column = new MyColumnConfig("Alpa {V_ALPA}");
				column.setParent(columns);
				column.setAlign("right");

				column = new MyColumnConfig("Blm/Tdk Absen {V_BLM}");
				column.setParent(columns);
				column.setAlign("right");

				column = new MyColumnConfig("Cuti " + bulan + " {V_CUTI}");
				column.setParent(columns);
				column.setAlign("right");

				column = new MyColumnConfig("Lembur {V_LEM}");
				column.setParent(columns);
				column.setAlign("right");

				column = new MyColumnConfig("Hadir {V_TPT}");
				column.setParent(columns);
				column.setAlign("right");

				column = new MyColumnConfig("Msk.Dihari Libur {V_MSK_LIBUR}");
				column.setParent(columns);
				column.setAlign("right");

				Rows rows = new Rows();
				rows.setParent(grid);

				for (Map map : maps) {
					Long pegawaiId = (Long) map.get("pegawai");
					if (pegawaiId == null)
						continue;

					Long aktif = (Long) map.get("aktif");
					Long masuk = (Long) map.get("masuk");
					Long alpa = (Long) map.get("alpa");
					Long sakit = (Long) map.get("sakit");
					Long izin = (Long) map.get("izin");
					Long belum = (Long) map.get("belum");
					Long tidakHadir = (Long) map.get("tidakHadir");
					Long tidakHadirTanpaHoliday = (Long) map.get("tidakHadirTanpaHoliday");
					Integer cuti = (Integer) map.get("cuti_bln_" + (bln + 1));
					Double lembur = (Double) map.get("lembur");
					Long tepatWaktu = (Long) map.get("tepatWaktu");
					Long masukDihariLibur = (Long) map.get("masukDihariLibur");

					Pegawai pegawaiData = (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), pegawaiId);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new Label(pegawaiData == null || pegawaiData.getUnitKerja() == null ? ""
							: pegawaiData.getUnitKerja().getNama()));
					row.appendChild(new Label(pegawaiData == null ? "" : pegawaiData.getNama()));
					row.appendChild(new Label(aktif == null ? "" : Common.numberFormat.get().format(aktif)));
					row.appendChild(new Label(masuk == null ? "" : Common.numberFormat.get().format(masuk)));
					row.appendChild(new Label((tidakHadir == null ? "" : Common.numberFormat.get().format(tidakHadir)) + " / "
							+ (tidakHadirTanpaHoliday == null ? ""
									: Common.numberFormat.get().format(tidakHadirTanpaHoliday))));

					row.appendChild(new Label(sakit == null ? "" : Common.numberFormat.get().format(sakit)));
					row.appendChild(new Label(izin == null ? "" : Common.numberFormat.get().format(izin)));
					row.appendChild(new Label(alpa == null ? "" : Common.numberFormat.get().format(alpa)));
					row.appendChild(new Label(belum == null ? "" : Common.numberFormat.get().format(belum)));
					row.appendChild(new Label(cuti == null ? "" : Common.numberFormat.get().format(cuti)));
					row.appendChild(new Label(lembur == null ? "" : Common.numberFormat.get().format(lembur)));
					row.appendChild(new Label(tepatWaktu == null ? "" : Common.numberFormat.get().format(tepatWaktu)));
					row.appendChild(
							new Label(masukDihariLibur == null ? "" : Common.numberFormat.get().format(masukDihariLibur)));
				}
				Clients.clearBusy();
			}
		});
		
		desktop = Executions.getCurrent().getDesktop();

		/* OPTIMASI FASE 5: server push dulu dinyalakan di sini tetapi TIDAK PERNAH dimatikan,
		 * sehingga browser terus polling (menahan thread Tomcat) selama tab terbuka walau proses
		 * sudah selesai. Tugas juga dijalankan pada thread MENTAH tanpa batas.
		 * jalankanDenganPush() menyalakan push ber-reference-count, menjalankan tugas pada pool
		 * daemon berbatas milik AsyncTaskManager, lalu MELEPAS push di finally. */
		ais.common.AsyncTaskManager.jalankanDenganPush(desktop, new Runnable() {
			@Override
			public void run() {
				try {
					generateDataDanImageAlbum(label);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/payroll/helper/ProsesAbsensiPegawai.java:929");
				}
			}
		});

	}

}