package ais.action.master.dashboard.lkp;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.maintenance.MainAction;
import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPayroll;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.Tbmuser;
import ais.database.model.lkp.RealisasiKerjaPegawai;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteriaWithColumn;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;

/**
 * Komponen dashboard khusus untuk dasboard rekap kegiatan pegawai. Kelas ini memilih variasi data
 * atau tampilan dashboard sambil memakai lifecycle dan mekanisme pemuatan dari kelas induknya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code
 * AmbilDataSatuanKerjaBanbox searchparent}, {@code AmbilDataPegawaiBanbox searchpegawai}, {@code MyDatebox
 * start}, {@code MyDatebox end}, {@code SatuanKerjaTreeModel satuanKerjaTreeModel}, {@code Grid grid}, {@code
 * String contentsRealisasi}; inisialisasi/lifecycle ({@code init()}); pembacaan/pencarian ({@code reload()},
 * {@code getTotal()}, {@code getValue()}); pelaporan/ekspor ({@code renderSummaryDanGrafik()}, {@code
 * renderJudulTabel()}); operasi domain lain ({@code buildDashboardHeader()}, {@code buildSummaryCards()}, {@code
 * appendMetricCard()}, {@code buildChartsHtml()}, {@code buildPanelHtml()}, {@code
 * buildDistribusiAktivitasChart()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class DasboardRekapKegiatanPegawai extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3557603220165512688L;
	private Center center;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private AmbilDataPegawaiBanbox searchpegawai;
	private MyDatebox start;
	private MyDatebox end;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Grid grid;

	public static String[] contentsRealisasi = new String[] { "id", "pegawai.nama", "targetKerjaPegawai.tahun",
			"targetKerjaPegawai.bulan", "targetKerjaPegawai.kegiatanTugasJabatan.nama", "targetKerjaPegawai.kuantitas",
			"targetKerjaPegawai.kualitas", "targetKerjaPegawai.waktu", "targetKerjaPegawai.biaya", "kuantitas", "waktu",
			"biaya", "tanggalWaktu", "tanggalWaktuSampai", "keterangan", "catatan", "verifikasi" };

	public static String[] catatanRealisasi = new String[] { "id", "pegawai.nama", "targetKerjaPegawai.tahun",
			"targetKerjaPegawai.bulan", "tanggalWaktu", "tanggalWaktuSampai", "keterangan", "catatan", "verifikasi" };

	public static String[] kehadiran = new String[] { "id", "pegawai.nama", "tanggal", "statusabsensi.nama",
			"detailJenisShiftPegawai.mulai", "detailJenisShiftPegawai.sampai", "masukjam", "pulangJam", "lamburMulai",
			"lamburSampai", "jumlahJamMasuk", "jumlahTerlambat", "jumlahCepat", "jumlahLemburMasuk",
			"jumlahMasukSebelumWaktunya", "jumlahPulangSetelahWaktunya", "jumlahCepatKeluar", "datangCepat",
			"datangTerlambat", "pulangCepat", "pulangTerlambat", "jumlahMenitAbsenFotoSaatHadir",
			"jumlahMenitAbsenFotoSaatPulang", "waktuJamMasuk", "waktuTerlambat", "waktuLemburMasuk", "waktuCepatKeluar",
			"secondJamMasuk", "secondTerlambat", "secondLemburMasuk", "secondCepatKeluar", "liburNasional",
			"cutiDanIzin", "liburRutin", "fotoAbsenDatang", "fotoAbsenPulang", "lokasiAbsenDatang", "lokasiAbsenPulang",
			"jarak", "jarakMaks", "lng", "lat", "satuanKerja.nama" };

	public DasboardRekapKegiatanPegawai() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DasboardRekapKegiatanPegawai(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);
		borderlayout.setStyle("min-height:25000px");
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getUserId() != null) {
			Integer desktopHeight = MainAction.desktopHeights.get(tbmuser.getUserId());
			if (desktopHeight != null) {
				borderlayout.setStyle("min-height:" + (desktopHeight * 0.9) + "px");
			}
		}

		North north = new North();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(borderlayout);
		north.setStyle("border:0px; background:#f6f8fb; padding:10px;");

		Grid grid = new Grid();
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setStyle(
				"background:#ffffff; border:1px solid #e9ecef; border-radius:14px; padding:10px; box-shadow:0 4px 18px rgba(0,0,0,0.04);");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}

		};

		Row row = new Row();
		row.setValign("middle");
		row.setStyle("background:#ffffff; border:0px;");
		row.setParent(rows);
		searchparent = new AmbilDataSatuanKerjaBanbox();
		searchparent.setEventListener(eventListener);

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		row.appendChild(new MyLabelConfig("Satuan Kerja"));
		row.appendChild(searchparent);

		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("Pegawai")));
		row.appendChild(searchpegawai = new AmbilDataPegawaiBanbox(true));
		searchpegawai.setWidth("90%");
		searchpegawai.setReadonly(true);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal")));
		row.appendChild(start = new MyDatebox());
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("sd")));
		row.appendChild(end = new MyDatebox());

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, 1);
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
		if (start != null) start.setValue(calendar.getTime());
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
		if (end != null) end.setValue(calendar.getTime());

		start.addEventListener("onChange", eventListener);
		end.addEventListener("onChange", eventListener);

		center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		Common.createDefaultTimer(eventListener);

		row = new Row();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "8");
		MyToolbarbuttonConfig refreshButton = new MyToolbarbuttonConfig("Refresh", "/img/refresh.png");
		refreshButton.setStyle(
				"font-weight:bold; background:#0d6efd; color:#ffffff; border-radius:8px; padding:6px 14px; margin-right:8px;");
		refreshButton.setParent(row);
		refreshButton.addEventListener("onClick", eventListener);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download Tabel", "/img/print.png");
		toolbarbutton
				.setStyle("font-weight:bold; background:#198754; color:#ffffff; border-radius:8px; padding:6px 14px;");
		toolbarbutton.setParent(row);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				UIUtil.downloadGrid(DasboardRekapKegiatanPegawai.this.grid);
			}
		});
	}

	private List<RealisasiKerjaPegawai> realisasiKerjaPegawaisSemua;
	private Map<String, StatuskehadiranKaryawanHarian> statuskehadiranKaryawanHarians;
	private List<Pegawai> pegawais;

	@SuppressWarnings({ "unchecked" })
	private void reload() {
		Common.clear(center);

		org.zkoss.zul.Vbox wrapperDasbor = new org.zkoss.zul.Vbox();
		wrapperDasbor.setWidth("100%");
		wrapperDasbor.setSpacing("12px");
		wrapperDasbor.setStyle("padding:12px; background:#f6f8fb; box-sizing:border-box; min-height:100%;");

		org.zkoss.zul.Div tableWrapper = new org.zkoss.zul.Div();
		tableWrapper.setWidth("100%");
		tableWrapper.setStyle("background:#ffffff; border:1px solid #e9ecef; border-radius:14px; padding:12px; "
				+ "box-shadow:0 4px 18px rgba(0,0,0,0.04); box-sizing:border-box;");

		wrapperDasbor.setParent(center);

		grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(tableWrapper);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("border:0px; background:#ffffff;");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Pegawai");
		column.setParent(columns);

		column = new MyColumnConfig("Jenis");
		column.setParent(columns);
		column.setWidth("10%");

		TreeMap<Integer, String> treeMap = new TreeMap<Integer, String>();
		treeMap.put(0, "Catatan");
		treeMap.put(1, "Kegiatan");
		treeMap.put(2, "Datang");
		treeMap.put(3, "Pulang");
		treeMap.put(4, "Tanpa Kegiatan");
		treeMap.put(5, "Rata-Rata");

		Map<Integer, MyColumnConfig> listCols = new HashMap<Integer, MyColumnConfig>();
		for (Integer status : treeMap.keySet()) {
			String nama = treeMap.get(status);
			column = new MyColumnConfig(nama);
			column.setWidth("5%");
			column.setAlign("right");
			listCols.put(status, column);
		}

		List<SatuanKerja> satuanKerjas;
		Session session = HibernateUtil.currentSession();
		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		if (parent != null) {
			Set<SatuanKerja> temp = new HashSet<SatuanKerja>();
			if (parent != null) {
				temp.add(parent);
				satuanKerjaTreeModel.getChildsSet(parent, temp);
			}
			satuanKerjas = new ArrayList<SatuanKerja>(temp);
			Collections.sort(satuanKerjas);
		} else {
			satuanKerjas = ConstantValues.simpleList(session.createCriteria(SatuanKerja.class)
					.add(Restrictions.eq("defaultItem", true)).addOrder(Order.asc("kode")).addOrder(Order.asc("nama")),
					SatuanKerja.class);
		}

		String inSatker = "";
		for (SatuanKerja satuanKerja : satuanKerjas) {
			inSatker += inSatker.isEmpty() ? satuanKerja.getId().toString() : "," + satuanKerja.getId();
		}
		final String satker = inSatker.isEmpty() ? "true"
				: "(this_.satuan_kerja in (" + inSatker + ") or this_.satuan_kerja is null)";
		final Pegawai peg = (Pegawai) searchpegawai.getAttribute("pegawai");
		pegawais = ConstantValues
				.simpleList(
						session.createCriteria(Pegawai.class)
								.add(peg == null || peg.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("id", peg.getId()))
								.add(Restrictions.eq("statusPegawai", ConstantValues.AKTIF_PEGAWAI))

								.createAlias("tipePegawai", "tipePegawai", Criteria.LEFT_JOIN)
								.add(Restrictions.or(Restrictions.isNull("tipePegawai.masukPresensi"),
										Restrictions.eq("tipePegawai.masukPresensi", true)))

								.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))

								.add(peg == null ? Restrictions.sqlRestriction(satker)
										: Restrictions.sqlRestriction("true"))
								.addOrder(Order.asc("satuanKerja")).addOrder(Order.asc("dosen"))
								.addOrder(Order.asc("guru")).addOrder(Order.asc("nama")),
						Pegawai.class);

		List<CutiDanIzin> cutiDanIzinsSemua = pegawais.isEmpty() ? new ArrayList<CutiDanIzin>()
				: session.createCriteria(CutiDanIzin.class)
						.add(Restrictions.or(Restrictions.between("mulai", start.getValue(), end.getValue()),
								Restrictions.between("sampai", start.getValue(), end.getValue())))
						.addOrder(Order.asc("mulai")).add(Restrictions.in("pegawai", pegawais))
						.add(Restrictions.eq("setujui", true)).list();

		realisasiKerjaPegawaisSemua = pegawais.isEmpty() ? new ArrayList<RealisasiKerjaPegawai>()
				: session.createCriteria(RealisasiKerjaPegawai.class)
						.createAlias("targetKerjaPegawai", "targetKerjaPegawai")
						.add(Restrictions.or(Restrictions.between("tanggalWaktu", start.getValue(), end.getValue()),
								Restrictions.between("tanggalWaktuSampai", start.getValue(), end.getValue())))
						.addOrder(Order.asc("tanggalWaktu"))
						.add(Restrictions.or(Restrictions.in("targetKerjaPegawai.pegawai", pegawais),
								Restrictions.in("pegawai", pegawais)))
						.list();

		statuskehadiranKaryawanHarians = CommonPayroll.getDefaultStatuskehadiranKaryawanHarian(cutiDanIzinsSemua,
				start.getValue(), end.getValue(), pegawais, session, true);

		Rows rows = new Rows();
		rows.setParent(grid);

		Map<Integer, Double> listTotals = new HashMap<Integer, Double>();
		Map<Long, List<Double>> mapData = new HashMap<Long, List<Double>>();

		for (final Pegawai pegawai : pegawais) {
			List<Double> data = mapData.get(pegawai == null || pegawai.getId() == null ? -1L : pegawai.getId());
			if (data == null) {
				data = new ArrayList<Double>();
				mapData.put(pegawai == null || pegawai.getId() == null ? -1L : pegawai.getId(), data);
			}
			for (Integer status : treeMap.keySet()) {

				Double count = 0.0;
				if (status.equals(0)) {
					for (RealisasiKerjaPegawai realisasiKerjaPegawai : realisasiKerjaPegawaisSemua) {
						if (realisasiKerjaPegawai.getPegawai() != null
								&& realisasiKerjaPegawai.getPegawai().getId().equals(pegawai.getId())) {
							count++;
						}
					}
				} else if (status.equals(1)) {
					List<String> s = new ArrayList<String>();
					for (RealisasiKerjaPegawai realisasiKerjaPegawai : realisasiKerjaPegawaisSemua) {
						if (realisasiKerjaPegawai.getPegawai() != null
								&& realisasiKerjaPegawai.getPegawai().getId().equals(pegawai.getId())) {
							String d = Common.dateFormat83.get().format(realisasiKerjaPegawai.getTanggalWaktu());
							if (!s.contains(d)) {
								s.add(d);
								count++;
							}
						}
					}
				} else if (status.equals(2)) {
					for (StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian : statuskehadiranKaryawanHarians
							.values()) {
						if (statuskehadiranKaryawanHarian.getPegawai() != null
								&& statuskehadiranKaryawanHarian.getPegawai().getId().equals(pegawai.getId())
								&& statuskehadiranKaryawanHarian.ambilMasukjam() != null) {
							count++;
						}
					}
				} else if (status.equals(3)) {
					for (StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian : statuskehadiranKaryawanHarians
							.values()) {
						if (statuskehadiranKaryawanHarian.getPegawai() != null
								&& statuskehadiranKaryawanHarian.getPegawai().getId().equals(pegawai.getId())
								&& statuskehadiranKaryawanHarian.ambilMasukjam() != null
								&& statuskehadiranKaryawanHarian.ambilPulangjam() != null) {
							count++;
						}
					}
				} else if (status.equals(4)) {

					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.setTime(start.getValue());

					Calendar s = ais.ui.util.WaktuUtil.getCalendar();
					s.setTime(end.getValue());

					while (calendar.getTime().before(s.getTime())) {
						Date tanggal = calendar.getTime();
						calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
						boolean ada = false;
						for (RealisasiKerjaPegawai realisasiKerjaPegawai : realisasiKerjaPegawaisSemua) {
							if (realisasiKerjaPegawai.getPegawai() != null
									&& realisasiKerjaPegawai.getPegawai().getId().equals(pegawai.getId())
									&& Common.dateFormat83.get().format(tanggal).equals(Common.dateFormat83.get()
											.format(realisasiKerjaPegawai.getTanggalWaktu()))) {
								ada = true;
								break;
							}
						}
						if (!ada) {
							count++;
						}
					}
				} else if (status.equals(5)) {
					Double kegiatan = listTotals.get(1);
					if (kegiatan == null) {
						kegiatan = 0.0;
					}
					Double datang = listTotals.get(2);
					if (datang == null) {
						datang = 0.0;
					}
					count = datang < 0.1 ? 0.0 : kegiatan / datang;
				}

				data.add(count);
				Double colCount = listTotals.get(status);
				if (colCount == null) {
					colCount = 0.0;
				}
				colCount += count;
				listTotals.put(status, colCount);

			}
		}

		renderSummaryDanGrafik(wrapperDasbor, listTotals, mapData);
		renderJudulTabel(tableWrapper);
		tableWrapper.setParent(wrapperDasbor);

		for (final Pegawai pegawai : pegawais) {
			List<Double> data = mapData.get(pegawai == null || pegawai.getId() == null ? -1L : pegawai.getId());
			Row row = new Row();
			row.setValign("top");
			row.appendChild(new MyLabelBoldAja(pegawai == null ? "" : pegawai.getNama()));
			row.appendChild(new MyLabelBoldAja(pegawai == null || pegawai.getStatusKepegawaian() == null ? ""
					: pegawai.getStatusKepegawaian().getNama()));
			int jml = 0;
			int i = 0;
			for (final Integer status : treeMap.keySet()) {
				Double colCount = listTotals.get(status);
				if (colCount == null) {
					colCount = 0.0;
				}
				if (colCount > 0.1) {
					Double count = data.get(i);
					jml += count;

					A a = new A(Common.numberFormat.get().format(count) + "");
					a.setStyle(
							"font-size:12px; font-weight:bold; color:#0d6efd; background:#eef6ff; border-radius:999px; padding:3px 8px; text-decoration:none;");
					a.setParent(row);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (status.equals(1) || status.equals(0)) {
								EventListener eventListener = (EventListener) Common.cetakDataCustomButton(
										RealisasiKerjaPegawai.class, new DataCriteriaWithColumn() {

											@Override
											public Object[] initCriteria(boolean order) {

												try {

													if (status.equals(0)) {
														List<RealisasiKerjaPegawai> realisasiKerjaPegawais = new ArrayList<RealisasiKerjaPegawai>();
														for (RealisasiKerjaPegawai realisasiKerjaPegawai : realisasiKerjaPegawaisSemua) {
															if (realisasiKerjaPegawai.getPegawai() != null
																	&& realisasiKerjaPegawai.getPegawai().getId()
																			.equals(pegawai.getId())) {
																realisasiKerjaPegawais.add(realisasiKerjaPegawai);
															}
														}
														return new Object[] { realisasiKerjaPegawais,
																catatanRealisasi };
													} else if (status.equals(1)) {
														List<String> s = new ArrayList<String>();
														List<RealisasiKerjaPegawai> realisasiKerjaPegawais = new ArrayList<RealisasiKerjaPegawai>();
														for (RealisasiKerjaPegawai realisasiKerjaPegawai : realisasiKerjaPegawaisSemua) {
															if (realisasiKerjaPegawai.getPegawai() != null
																	&& realisasiKerjaPegawai.getPegawai().getId()
																			.equals(pegawai.getId())) {
																String d = Common.dateFormat83.get().format(
																		realisasiKerjaPegawai.getTanggalWaktu());
																if (!s.contains(d)) {
																	s.add(d);
																	realisasiKerjaPegawais.add(realisasiKerjaPegawai);
																}
															}
														}
														return new Object[] { realisasiKerjaPegawais,
																contentsRealisasi };
													}

												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
												}
												return null;
											}

										}, null, "Download Data", "/img/print.png", null, null, false, null,
										"DATA TAMBAHAN",
										new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "" })
										.getAttribute("eventListener");

								eventListener.onEvent(null);
							} else if (status.equals(2) || status.equals(3)) {
								EventListener eventListener = (EventListener) Common.cetakDataCustomButton(
										StatuskehadiranKaryawanHarian.class, new DataCriteriaWithColumn() {

											@Override
											public Object[] initCriteria(boolean order) {

												try {

													if (status.equals(2)) {
														List<StatuskehadiranKaryawanHarian> statuskehadirans = new ArrayList<StatuskehadiranKaryawanHarian>();
														for (StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian : statuskehadiranKaryawanHarians
																.values()) {
															if (statuskehadiranKaryawanHarian.getPegawai() != null
																	&& statuskehadiranKaryawanHarian
																			.ambilMasukjam() != null
																	&& statuskehadiranKaryawanHarian.getPegawai()
																			.getId().equals(pegawai.getId())) {
																statuskehadirans.add(statuskehadiranKaryawanHarian);
															}
														}
														return new Object[] { statuskehadirans, kehadiran };
													} else if (status.equals(3)) {
														List<StatuskehadiranKaryawanHarian> statuskehadirans = new ArrayList<StatuskehadiranKaryawanHarian>();
														for (StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian : statuskehadiranKaryawanHarians
																.values()) {
															if (statuskehadiranKaryawanHarian.getPegawai() != null
																	&& statuskehadiranKaryawanHarian
																			.ambilMasukjam() != null
																	&& statuskehadiranKaryawanHarian
																			.ambilPulangjam() != null
																	&& statuskehadiranKaryawanHarian.getPegawai()
																			.getId().equals(pegawai.getId())) {
																statuskehadirans.add(statuskehadiranKaryawanHarian);
															}
														}
														return new Object[] { statuskehadirans, kehadiran };
													}

												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
												}
												return null;
											}

										}, null, "Download Data", "/img/print.png", null, null, false, null,
										"DATA TAMBAHAN",
										new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
												"", "", "", "", "", "", "", "" })
										.getAttribute("eventListener");

								eventListener.onEvent(null);
							} else if (status.equals(4)) {

								Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
								calendar.setTime(start.getValue());

								Calendar s = ais.ui.util.WaktuUtil.getCalendar();
								s.setTime(end.getValue());

								List<Date> tidakAdaKegiatan = new ArrayList<Date>();
								while (calendar.getTime().before(s.getTime())) {
									Date tanggal = calendar.getTime();
									calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
									boolean ada = false;
									for (RealisasiKerjaPegawai realisasiKerjaPegawai : realisasiKerjaPegawaisSemua) {
										if (realisasiKerjaPegawai.getPegawai() != null
												&& realisasiKerjaPegawai.getPegawai().getId().equals(pegawai.getId())
												&& Common.dateFormat83.get().format(tanggal).equals(Common.dateFormat83
														.get().format(realisasiKerjaPegawai.getTanggalWaktu()))) {
											ada = true;
											break;
										}
									}
									if (!ada) {
										tidakAdaKegiatan.add(tanggal);
									}
								}

								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
								spreadsheet.setMaxcolumns(9);
								spreadsheet.setMaxrows(tidakAdaKegiatan.size() + 2);
								Worksheet sheet = spreadsheet.getSelectedSheet();

								int rowIndex = 0;
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "PEGAWAI");
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "TANGGAL");
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "STATUS");

								rowIndex = 1;
								for (Date date : tidakAdaKegiatan) {
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, pegawai.getNama());
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
											Common.dateFormat1.get().format(date));
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Tidak ada kegiatan");
									rowIndex++;
								}

								ByteArrayOutputStream bout = new ByteArrayOutputStream();
								spreadsheet.getBook().write(bout);
								bout.close();

								String fn = Sessions.getCurrent().getWebApp()
										.getRealPath(
												"/tmp/tidak_hadir_pegawai_" + pegawai.getId() + "_"
														+ URLEncoder.encode(Common.dateFormat62.get()
																.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
														+ ".xlsx");

								try {
									FileOutputStream fileOut = new FileOutputStream(fn);
									fileOut.write(bout.toByteArray());
									fileOut.close();
								} catch (IOException e) {
									Common.tampilErrorJikaAdmin(e);
								}

								Common.displayXlsx(fn, new Intbox(rowIndex), 20);

							} else if (status.equals(5)) {

								List<String> s = new ArrayList<String>();
								Map<String, Object[]> realisasiKerjaPegawais = new HashMap<String, Object[]>();
								for (RealisasiKerjaPegawai realisasiKerjaPegawai : realisasiKerjaPegawaisSemua) {
									if (realisasiKerjaPegawai.getPegawai() != null
											&& realisasiKerjaPegawai.getPegawai().getId().equals(pegawai.getId())) {
										String d = Common.dateFormat83.get()
												.format(realisasiKerjaPegawai.getTanggalWaktu());
										if (!s.contains(d)) {
											s.add(d);
											StatuskehadiranKaryawanHarian hadir = null;
											for (StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian : statuskehadiranKaryawanHarians
													.values()) {
												if (statuskehadiranKaryawanHarian.getPegawai() != null
														&& statuskehadiranKaryawanHarian.ambilMasukjam() != null
														&& d.equalsIgnoreCase(Common.dateFormat83.get()
																.format(statuskehadiranKaryawanHarian.getTanggal()))
														&& statuskehadiranKaryawanHarian.getPegawai().getId()
																.equals(pegawai.getId())) {
													hadir = statuskehadiranKaryawanHarian;
													break;
												}
											}

											if (hadir != null) {
												realisasiKerjaPegawais.put(d,
														new Object[] { realisasiKerjaPegawai, hadir });
											}
										}
									}
								}

								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
								spreadsheet.setMaxcolumns(9);
								spreadsheet.setMaxrows(realisasiKerjaPegawais.size() + 2);
								Worksheet sheet = spreadsheet.getSelectedSheet();

								int rowIndex = 0;
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "PEGAWAI");
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "TANGGAL");
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "HADIR JAM");
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "PULANG JAM");
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "TARGET");
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "CATATAN");

								rowIndex = 1;
								for (Object[] d : realisasiKerjaPegawais.values()) {
									RealisasiKerjaPegawai realisasiKerjaPegawai = (RealisasiKerjaPegawai) d[0];
									StatuskehadiranKaryawanHarian hadir = (StatuskehadiranKaryawanHarian) d[1];
									Date date = hadir.getTanggal();
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, pegawai.getNama());
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
											Common.dateFormat1.get().format(date));
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2,
											hadir.ambilMasukjam() == null ? ""
													: Common.timeFormat.get().format(hadir.ambilMasukjam()));
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3,
											hadir.ambilPulangjam() == null ? ""
													: Common.timeFormat.get().format(hadir.ambilPulangjam()));
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4,
											realisasiKerjaPegawai == null
													|| realisasiKerjaPegawai.getTargetKerjaPegawai() == null
													|| realisasiKerjaPegawai.getTargetKerjaPegawai()
															.getKegiatanTugasJabatan() == null ? ""
																	: realisasiKerjaPegawai.getTargetKerjaPegawai()
																			.getKegiatanTugasJabatan().getNama());
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5,
											realisasiKerjaPegawai == null ? "" : realisasiKerjaPegawai.getCatatan());
									rowIndex++;
								}

								ByteArrayOutputStream bout = new ByteArrayOutputStream();
								spreadsheet.getBook().write(bout);
								bout.close();

								String fn = Sessions.getCurrent().getWebApp()
										.getRealPath(
												"/tmp/catatan_pegawai_" + pegawai.getId() + "_"
														+ URLEncoder.encode(Common.dateFormat62.get()
																.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
														+ ".xlsx");

								try {
									FileOutputStream fileOut = new FileOutputStream(fn);
									fileOut.write(bout.toByteArray());
									fileOut.close();
								} catch (IOException e) {
									Common.tampilErrorJikaAdmin(e);
								}

								Common.displayXlsx(fn, new Intbox(rowIndex), 20);
							}
						}
					});
				}
				i++;
			}

			if (jml > 0) {
				row.setParent(rows);
			}
		}

		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelBolder("Total"));
		row.appendChild(new MyLabelBolder(""));
		Double countCol = 0.0;
		Double totalCol = 0.0;
		for (final Integer status : treeMap.keySet()) {
			Double colCount = listTotals.get(status);
			if (colCount == null) {
				colCount = 0.0;
			}
			totalCol += colCount;
			if (colCount > 0) {
				countCol++;
				listCols.get(status).setParent(columns);

				A a = new A(Common.numberFormat.get().format(colCount) + "");
				a.setStyle(
						"font-size:16px; font-weight:bolder; color:#ffffff; background:#0d6efd; border-radius:999px; padding:4px 10px; text-decoration:none;");
				a.setParent(row);
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (status.equals(1) || status.equals(0)) {
							EventListener eventListener = (EventListener) Common
									.cetakDataCustomButton(RealisasiKerjaPegawai.class, new DataCriteriaWithColumn() {

										@Override
										public Object[] initCriteria(boolean order) {

											try {

												if (status.equals(0)) {
													return new Object[] { realisasiKerjaPegawaisSemua,
															catatanRealisasi };
												} else if (status.equals(1)) {
													List<String> s = new ArrayList<String>();
													List<RealisasiKerjaPegawai> realisasiKerjaPegawais = new ArrayList<RealisasiKerjaPegawai>();
													for (RealisasiKerjaPegawai realisasiKerjaPegawai : realisasiKerjaPegawaisSemua) {

														String d = Common.dateFormat83.get()
																.format(realisasiKerjaPegawai.getTanggalWaktu());
														if (!s.contains(d)) {
															s.add(d);
															realisasiKerjaPegawais.add(realisasiKerjaPegawai);
														}

													}
													return new Object[] { realisasiKerjaPegawais, contentsRealisasi };
												}

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
											return null;
										}

									}, null, "Download Data", "/img/print.png", null, null, false, null,
											"DATA TAMBAHAN",
											new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
													"" })
									.getAttribute("eventListener");

							eventListener.onEvent(null);
						} else if (status.equals(2) || status.equals(3)) {
							EventListener eventListener = (EventListener) Common.cetakDataCustomButton(
									StatuskehadiranKaryawanHarian.class, new DataCriteriaWithColumn() {

										@Override
										public Object[] initCriteria(boolean order) {

											try {

												if (status.equals(2)) {
													List<StatuskehadiranKaryawanHarian> statuskehadirans = new ArrayList<StatuskehadiranKaryawanHarian>(
															statuskehadiranKaryawanHarians.values());
													return new Object[] { statuskehadirans, kehadiran };
												} else if (status.equals(3)) {
													List<StatuskehadiranKaryawanHarian> statuskehadirans = new ArrayList<StatuskehadiranKaryawanHarian>();
													for (StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian : statuskehadiranKaryawanHarians
															.values()) {
														if (statuskehadiranKaryawanHarian.getPegawai() != null
																&& statuskehadiranKaryawanHarian.ambilMasukjam() != null
																&& statuskehadiranKaryawanHarian
																		.ambilPulangjam() != null) {
															statuskehadirans.add(statuskehadiranKaryawanHarian);
														}
													}
													return new Object[] { statuskehadirans, kehadiran };
												}

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
											return null;
										}

									}, null, "Download Data", "/img/print.png", null, null, false, null,
									"DATA TAMBAHAN",
									new String[] { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
											"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "" })
									.getAttribute("eventListener");

							eventListener.onEvent(null);
						} else if (status.equals(4)) {

							int jumlah = 1;
							Map<Pegawai, List<Date>> maps = new HashMap<Pegawai, List<Date>>();
							for (Pegawai pegawai : pegawais) {

								Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
								calendar.setTime(start.getValue());

								Calendar s = ais.ui.util.WaktuUtil.getCalendar();
								s.setTime(end.getValue());

								List<Date> tidakAdaKegiatan = new ArrayList<Date>();
								while (calendar.getTime().before(s.getTime())) {
									Date tanggal = calendar.getTime();
									calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
									boolean ada = false;
									for (RealisasiKerjaPegawai realisasiKerjaPegawai : realisasiKerjaPegawaisSemua) {
										if (realisasiKerjaPegawai.getPegawai() != null
												&& realisasiKerjaPegawai.getPegawai().getId().equals(pegawai.getId())
												&& Common.dateFormat83.get().format(tanggal).equals(Common.dateFormat83
														.get().format(realisasiKerjaPegawai.getTanggalWaktu()))) {
											jumlah++;
											ada = true;
											break;
										}
									}
									if (!ada) {
										tidakAdaKegiatan.add(tanggal);
									}
								}

								maps.put(pegawai, tidakAdaKegiatan);
							}
							Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
							spreadsheet.setWidth("100%");
							spreadsheet.setHeight("100%");
							spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
							spreadsheet.setMaxcolumns(9);
							spreadsheet.setMaxrows(jumlah + 2);
							Worksheet sheet = spreadsheet.getSelectedSheet();

							int rowIndex = 0;
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "PEGAWAI");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "TANGGAL");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "STATUS");

							rowIndex = 1;
							for (Pegawai pegawai : pegawais) {
								List<Date> tidakAdaKegiatan = maps.get(pegawai);
								for (Date date : tidakAdaKegiatan) {
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, pegawai.getNama());
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
											Common.dateFormat1.get().format(date));
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Tidak ada kegiatan");
									rowIndex++;
								}
							}

							ByteArrayOutputStream bout = new ByteArrayOutputStream();
							spreadsheet.getBook().write(bout);
							bout.close();

							String fn = Sessions.getCurrent().getWebApp().getRealPath("/tmp/tidak_hadir_pegawai_semua_"
									+ URLEncoder.encode(
											Common.dateFormat62.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
									+ ".xlsx");

							try {
								FileOutputStream fileOut = new FileOutputStream(fn);
								fileOut.write(bout.toByteArray());
								fileOut.close();
							} catch (IOException e) {
								Common.tampilErrorJikaAdmin(e);
							}

							Common.displayXlsx(fn, new Intbox(rowIndex), 20);

						} else if (status.equals(5)) {

							int jumlah = 1;
							Map<Pegawai, Map<String, Object[]>> maps = new HashMap<Pegawai, Map<String, Object[]>>();
							for (Pegawai pegawai : pegawais) {
								List<String> s = new ArrayList<String>();
								Map<String, Object[]> realisasiKerjaPegawais = new HashMap<String, Object[]>();
								for (RealisasiKerjaPegawai realisasiKerjaPegawai : realisasiKerjaPegawaisSemua) {
									if (realisasiKerjaPegawai.getPegawai() != null
											&& realisasiKerjaPegawai.getPegawai().getId().equals(pegawai.getId())) {
										String d = Common.dateFormat83.get()
												.format(realisasiKerjaPegawai.getTanggalWaktu());
										if (!s.contains(d)) {
											s.add(d);
											StatuskehadiranKaryawanHarian hadir = null;
											for (StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian : statuskehadiranKaryawanHarians
													.values()) {
												if (statuskehadiranKaryawanHarian.getPegawai() != null
														&& statuskehadiranKaryawanHarian.ambilMasukjam() != null
														&& d.equalsIgnoreCase(Common.dateFormat83.get()
																.format(statuskehadiranKaryawanHarian.getTanggal()))
														&& statuskehadiranKaryawanHarian.getPegawai().getId()
																.equals(pegawai.getId())) {
													hadir = statuskehadiranKaryawanHarian;
													jumlah++;
													break;
												}
											}

											if (hadir != null) {
												realisasiKerjaPegawais.put(d,
														new Object[] { realisasiKerjaPegawai, hadir });
											}
										}
									}
								}

								maps.put(pegawai, realisasiKerjaPegawais);
							}
							Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
							spreadsheet.setWidth("100%");
							spreadsheet.setHeight("100%");
							spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
							spreadsheet.setMaxcolumns(6);
							spreadsheet.setMaxrows(jumlah + 2);
							Worksheet sheet = spreadsheet.getSelectedSheet();

							int rowIndex = 0;
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "PEGAWAI");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "TANGGAL");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "HADIR JAM");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "PULANG JAM");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "TARGET");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "CATATAN");

							rowIndex = 1;
							for (Pegawai pegawai : pegawais) {
								Map<String, Object[]> realisasiKerjaPegawais = maps.get(pegawai);
								for (Object[] d : realisasiKerjaPegawais.values()) {
									RealisasiKerjaPegawai realisasiKerjaPegawai = (RealisasiKerjaPegawai) d[0];
									StatuskehadiranKaryawanHarian hadir = (StatuskehadiranKaryawanHarian) d[1];
									Date date = hadir.getTanggal();
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, pegawai.getNama());
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
											Common.dateFormat1.get().format(date));
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2,
											hadir.ambilMasukjam() == null ? ""
													: Common.timeFormat.get().format(hadir.ambilMasukjam()));
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3,
											hadir.ambilPulangjam() == null ? ""
													: Common.timeFormat.get().format(hadir.ambilPulangjam()));
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4,
											realisasiKerjaPegawai == null
													|| realisasiKerjaPegawai.getTargetKerjaPegawai() == null
													|| realisasiKerjaPegawai.getTargetKerjaPegawai()
															.getKegiatanTugasJabatan() == null ? ""
																	: realisasiKerjaPegawai.getTargetKerjaPegawai()
																			.getKegiatanTugasJabatan().getNama());
									ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5,
											realisasiKerjaPegawai == null ? "" : realisasiKerjaPegawai.getCatatan());
									rowIndex++;
								}
							}
							ByteArrayOutputStream bout = new ByteArrayOutputStream();
							spreadsheet.getBook().write(bout);
							bout.close();

							String fn = Sessions.getCurrent().getWebApp().getRealPath("/tmp/catatan_pegawai_semua_"
									+ URLEncoder.encode(
											Common.dateFormat62.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
									+ ".xlsx");

							try {
								FileOutputStream fileOut = new FileOutputStream(fn);
								fileOut.write(bout.toByteArray());
								fileOut.close();
							} catch (IOException e) {
								Common.tampilErrorJikaAdmin(e);
							}

							Common.displayXlsx(fn, new Intbox(rowIndex), 20);
						}
					}
				});
			}

		}

	}

	private void renderSummaryDanGrafik(org.zkoss.zul.Vbox wrapperDasbor, Map<Integer, Double> listTotals,
			Map<Long, List<Double>> mapData) {
		List<PegawaiDashboardSummary> summaries = buildPegawaiDashboardSummaries(mapData);

		double totalCatatan = getTotal(listTotals, 0);
		double totalKegiatan = getTotal(listTotals, 1);
		double totalDatang = getTotal(listTotals, 2);
		double totalPulang = getTotal(listTotals, 3);
		double totalTanpaKegiatan = getTotal(listTotals, 4);
		double rataKegiatanPerHadir = totalDatang < 0.1 ? 0.0 : totalKegiatan / totalDatang;
		double rasioKegiatan = totalDatang < 0.1 ? 0.0 : totalKegiatan * 100.0 / totalDatang;
		double rasioPulangLengkap = totalDatang < 0.1 ? 0.0 : totalPulang * 100.0 / totalDatang;
		double rasioTanpaKegiatan = (totalKegiatan + totalTanpaKegiatan) < 0.1 ? 0.0
				: totalTanpaKegiatan * 100.0 / (totalKegiatan + totalTanpaKegiatan);

		org.zkoss.zul.Html header = new org.zkoss.zul.Html(buildDashboardHeader(totalCatatan, totalKegiatan));
		header.setParent(wrapperDasbor);

		org.zkoss.zul.Html cards = new org.zkoss.zul.Html(
				buildSummaryCards(totalCatatan, totalKegiatan, totalDatang, totalPulang, totalTanpaKegiatan,
						rataKegiatanPerHadir, rasioKegiatan, rasioPulangLengkap, rasioTanpaKegiatan));
		cards.setParent(wrapperDasbor);

		org.zkoss.zul.Html charts = new org.zkoss.zul.Html(buildChartsHtml(summaries, listTotals));
		charts.setParent(wrapperDasbor);

		org.zkoss.zul.Html insight = new org.zkoss.zul.Html(
				buildInsightHtml(summaries, rasioKegiatan, rasioPulangLengkap, rasioTanpaKegiatan));
		insight.setParent(wrapperDasbor);
	}

	private void renderJudulTabel(org.zkoss.zul.Div tableWrapper) {
		org.zkoss.zul.Html title = new org.zkoss.zul.Html(
				"<div style='display:flex; justify-content:space-between; align-items:center; gap:12px; margin-bottom:10px;'>"
						+ "<div>"
						+ "<div style='font-size:18px; font-weight:800; color:#212529;'>Rekap Detail Kegiatan Pegawai</div>"
						+ "<div style='font-size:12px; color:#6c757d;'>Klik angka pada tabel untuk melihat atau mengunduh rincian data.</div>"
						+ "</div>"
						+ "<div style='font-size:12px; color:#6c757d; background:#f8f9fa; border-radius:999px; padding:6px 12px;'>"
						+ escapeHtml(formatPeriode()) + "</div>" + "</div>");
		title.setParent(tableWrapper);
	}

	private String buildDashboardHeader(double totalCatatan, double totalKegiatan) {
		String periode = formatPeriode();
		String satker = "-";
		try {
			SatuanKerja satuanKerja = (SatuanKerja) searchparent.getAttribute("satuanKerja");
			if (satuanKerja != null && satuanKerja.getNama() != null) {
				satker = satuanKerja.getNama();
			} else {
				satker = "Semua satuan kerja default";
			}
		} catch (Exception e) {
			satker = "Semua satuan kerja";
		}

		String pegawaiFilter = "Semua pegawai";
		try {
			Pegawai pegawai = (Pegawai) searchpegawai.getAttribute("pegawai");
			if (pegawai != null && pegawai.getNama() != null) {
				pegawaiFilter = pegawai.getNama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/lkp/DasboardRekapKegiatanPegawai.java:1141");
		}

		return "<div style='background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); color:white; border-radius:18px; "
				+ "padding:18px 20px; box-shadow:0 8px 24px rgba(13,110,253,0.18);'>"
				+ "<div style='display:flex; justify-content:space-between; align-items:flex-start; gap:15px; flex-wrap:wrap;'>"
				+ "<div>"
				+ "<div style='font-size:13px; opacity:.9; letter-spacing:.4px; text-transform:uppercase;'>Dasbor Rekap Kegiatan Pegawai</div>"
				+ "<div style='font-size:25px; font-weight:800; margin-top:4px;'>Monitoring Catatan, Kehadiran, dan Produktivitas Harian</div>"
				+ "<div style='font-size:13px; opacity:.92; margin-top:8px;'>Periode: " + escapeHtml(periode)
				+ " &nbsp; | &nbsp; Satuan Kerja: " + escapeHtml(satker) + " &nbsp; | &nbsp; Pegawai: "
				+ escapeHtml(pegawaiFilter) + "</div>" + "</div>" + "<div style='text-align:right; min-width:190px;'>"
				+ "<div style='font-size:12px; opacity:.85;'>Total Catatan / Hari Kegiatan</div>"
				+ "<div style='font-size:30px; font-weight:900; line-height:1.1;'>" + formatAngka(totalCatatan) + " / "
				+ formatAngka(totalKegiatan) + "</div>" + "</div>" + "</div>" + "</div>";
	}

	private String buildSummaryCards(double totalCatatan, double totalKegiatan, double totalDatang, double totalPulang,
			double totalTanpaKegiatan, double rataKegiatanPerHadir, double rasioKegiatan, double rasioPulangLengkap,
			double rasioTanpaKegiatan) {
		StringBuffer sb = new StringBuffer();
		sb.append("<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(185px,1fr)); gap:12px;'>");
		appendMetricCard(sb, "Pegawai Dianalisis", formatAngka(pegawais == null ? 0 : pegawais.size()),
				"pegawai aktif sesuai filter", "#0d6efd");
		appendMetricCard(sb, "Total Catatan", formatAngka(totalCatatan), "seluruh catatan harian", "#6f42c1");
		appendMetricCard(sb, "Hari Berkegiatan", formatAngka(totalKegiatan), "hari unik dengan kegiatan", "#20c997");
		appendMetricCard(sb, "Hadir / Datang", formatAngka(totalDatang), "rekap presensi masuk", "#198754");
		appendMetricCard(sb, "Pulang Lengkap", formatAngka(totalPulang) + " (" + formatAngka(rasioPulangLengkap) + "%)",
				"hadir dengan absen pulang", "#0dcaf0");
		appendMetricCard(sb, "Tanpa Kegiatan",
				formatAngka(totalTanpaKegiatan) + " (" + formatAngka(rasioTanpaKegiatan) + "%)",
				"hari belum ada catatan", "#dc3545");
		appendMetricCard(sb, "Rasio Kegiatan", formatAngka(rasioKegiatan) + "%", "kegiatan dibanding hadir",
				warnaPersen(rasioKegiatan));
		appendMetricCard(sb, "Rata-rata", formatAngka(rataKegiatanPerHadir), "hari kegiatan per hadir", "#fd7e14");
		sb.append("</div>");
		return sb.toString();
	}

	private void appendMetricCard(StringBuffer sb, String title, String value, String desc, String color) {
		sb.append("<div style='background:#ffffff; border:1px solid #e9ecef; border-radius:16px; padding:14px; "
				+ "box-shadow:0 4px 16px rgba(0,0,0,0.04); position:relative; overflow:hidden;'>");
		sb.append(
				"<div style='position:absolute; right:-18px; top:-18px; width:72px; height:72px; border-radius:50%; background:")
				.append(color).append("; opacity:.12;'></div>");
		sb.append("<div style='font-size:12px; color:#6c757d; font-weight:700; text-transform:uppercase;'>")
				.append(escapeHtml(title)).append("</div>");
		sb.append("<div style='font-size:24px; font-weight:900; color:").append(color).append("; margin-top:4px;'>")
				.append(escapeHtml(value)).append("</div>");
		sb.append("<div style='font-size:12px; color:#6c757d; margin-top:2px;'>").append(escapeHtml(desc))
				.append("</div>");
		sb.append("</div>");
	}

	private String buildChartsHtml(List<PegawaiDashboardSummary> summaries, Map<Integer, Double> listTotals) {
		StringBuffer sb = new StringBuffer();
		sb.append("<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(360px,1fr)); gap:12px;'>");

		sb.append(buildPanelHtml("Grafik Distribusi Rekap Aktivitas", buildDistribusiAktivitasChart(listTotals),
				"Perbandingan total Catatan, Hari Berkegiatan, Datang, Pulang, dan Tanpa Kegiatan."));

		sb.append(buildPanelHtml("Top 10 Produktivitas Kegiatan", buildTopProduktifChart(summaries),
				"Pegawai dengan catatan kegiatan terbanyak pada periode terpilih."));

		sb.append(buildPanelHtml("Kelengkapan Presensi vs Kegiatan", buildKelengkapanChart(summaries),
				"Melihat keseimbangan hari hadir, hari berkegiatan, dan absen pulang."));

		sb.append(buildPanelHtml("Pegawai dengan Risiko Tanpa Kegiatan", buildTanpaKegiatanChart(summaries),
				"Prioritas monitoring pegawai yang banyak hari tanpa catatan kegiatan."));

		sb.append("</div>");
		return sb.toString();
	}

	private String buildPanelHtml(String title, String body, String caption) {
		return "<div style='background:#ffffff; border:1px solid #e9ecef; border-radius:16px; padding:14px; "
				+ "box-shadow:0 4px 16px rgba(0,0,0,0.04);'>"
				+ "<div style='font-size:16px; font-weight:800; color:#212529; margin-bottom:3px;'>" + escapeHtml(title)
				+ "</div>" + "<div style='font-size:12px; color:#6c757d; margin-bottom:12px;'>" + escapeHtml(caption)
				+ "</div>" + body + "</div>";
	}

	private String buildDistribusiAktivitasChart(Map<Integer, Double> listTotals) {
		double catatan = getTotal(listTotals, 0);
		double kegiatan = getTotal(listTotals, 1);
		double datang = getTotal(listTotals, 2);
		double pulang = getTotal(listTotals, 3);
		double tanpa = getTotal(listTotals, 4);
		double max = Math.max(1.0, Math.max(Math.max(catatan, kegiatan), Math.max(Math.max(datang, pulang), tanpa)));

		StringBuffer sb = new StringBuffer();
		sb.append("<div>");
		appendBar(sb, "Catatan", catatan, max, "#6f42c1", "Total entri catatan harian");
		appendBar(sb, "Kegiatan", kegiatan, max, "#20c997", "Jumlah hari unik berkegiatan");
		appendBar(sb, "Datang", datang, max, "#198754", "Jumlah presensi masuk");
		appendBar(sb, "Pulang", pulang, max, "#0dcaf0", "Jumlah presensi pulang lengkap");
		appendBar(sb, "Tanpa Kegiatan", tanpa, max, "#dc3545", "Hari tanpa catatan kegiatan");
		sb.append("</div>");
		return sb.toString();
	}

	private String buildTopProduktifChart(List<PegawaiDashboardSummary> summaries) {
		List<PegawaiDashboardSummary> list = new ArrayList<PegawaiDashboardSummary>(summaries);
		Collections.sort(list, new java.util.Comparator<PegawaiDashboardSummary>() {
			public int compare(PegawaiDashboardSummary a, PegawaiDashboardSummary b) {
				if (b.catatan > a.catatan)
					return 1;
				if (b.catatan < a.catatan)
					return -1;
				if (b.kegiatan > a.kegiatan)
					return 1;
				if (b.kegiatan < a.kegiatan)
					return -1;
				return a.nama.compareToIgnoreCase(b.nama);
			}
		});

		double max = 1.0;
		for (PegawaiDashboardSummary h : list) {
			if (h.catatan > max) {
				max = h.catatan;
			}
		}

		StringBuffer sb = new StringBuffer();
		int no = 0;
		for (PegawaiDashboardSummary h : list) {
			if (h.catatan <= 0.0) {
				continue;
			}
			no++;
			appendRankBar(sb, no, h.nama, h.catatan, max, "#6f42c1",
					formatAngka(h.catatan) + " catatan, " + formatAngka(h.kegiatan) + " hari kegiatan");
			if (no >= 10) {
				break;
			}
		}
		if (no == 0) {
			sb.append(emptyChartMessage("Belum ada catatan kegiatan pada periode ini."));
		}
		return sb.toString();
	}

	private String buildKelengkapanChart(List<PegawaiDashboardSummary> summaries) {
		List<PegawaiDashboardSummary> list = new ArrayList<PegawaiDashboardSummary>(summaries);
		Collections.sort(list, new java.util.Comparator<PegawaiDashboardSummary>() {
			public int compare(PegawaiDashboardSummary a, PegawaiDashboardSummary b) {
				if (b.rasioKegiatan > a.rasioKegiatan)
					return 1;
				if (b.rasioKegiatan < a.rasioKegiatan)
					return -1;
				return a.nama.compareToIgnoreCase(b.nama);
			}
		});

		StringBuffer sb = new StringBuffer();
		int no = 0;
		for (PegawaiDashboardSummary h : list) {
			if (h.datang <= 0.0) {
				continue;
			}
			no++;
			double max = 100.0;
			sb.append("<div style='margin-bottom:10px;'>");
			sb.append("<div style='display:flex; justify-content:space-between; font-size:12px; margin-bottom:4px;'>");
			sb.append("<span style='font-weight:700; color:#212529;'>").append(no).append(". ")
					.append(escapeHtml(h.nama)).append("</span>");
			sb.append("<span style='color:#6c757d;'>Kegiatan ").append(formatAngka(h.rasioKegiatan))
					.append("% | Pulang ").append(formatAngka(h.rasioPulang)).append("%</span>");
			sb.append("</div>");
			sb.append("<div style='height:9px; background:#e9ecef; border-radius:999px; overflow:hidden;'>");
			sb.append("<div style='height:9px; width:").append(barWidth(h.rasioKegiatan, max))
					.append("%; background:#20c997; border-radius:999px;'></div>");
			sb.append("</div>");
			sb.append("</div>");
			if (no >= 10) {
				break;
			}
		}
		if (no == 0) {
			sb.append(emptyChartMessage("Belum ada data presensi masuk pada periode ini."));
		}
		return sb.toString();
	}

	private String buildTanpaKegiatanChart(List<PegawaiDashboardSummary> summaries) {
		List<PegawaiDashboardSummary> list = new ArrayList<PegawaiDashboardSummary>(summaries);
		Collections.sort(list, new java.util.Comparator<PegawaiDashboardSummary>() {
			public int compare(PegawaiDashboardSummary a, PegawaiDashboardSummary b) {
				if (b.tanpaKegiatan > a.tanpaKegiatan)
					return 1;
				if (b.tanpaKegiatan < a.tanpaKegiatan)
					return -1;
				return a.nama.compareToIgnoreCase(b.nama);
			}
		});

		double max = 1.0;
		for (PegawaiDashboardSummary h : list) {
			if (h.tanpaKegiatan > max) {
				max = h.tanpaKegiatan;
			}
		}

		StringBuffer sb = new StringBuffer();
		int no = 0;
		for (PegawaiDashboardSummary h : list) {
			if (h.tanpaKegiatan <= 0.0) {
				continue;
			}
			no++;
			appendRankBar(sb, no, h.nama, h.tanpaKegiatan, max, warnaPersen(100.0 - h.rasioTanpaKegiatan),
					formatAngka(h.tanpaKegiatan) + " hari tanpa kegiatan, risiko " + formatAngka(h.rasioTanpaKegiatan)
							+ "%");
			if (no >= 10) {
				break;
			}
		}
		if (no == 0) {
			sb.append(emptyChartMessage("Tidak ada pegawai dengan hari tanpa kegiatan pada periode ini."));
		}
		return sb.toString();
	}

	private String buildInsightHtml(List<PegawaiDashboardSummary> summaries, double rasioKegiatan,
			double rasioPulangLengkap, double rasioTanpaKegiatan) {
		PegawaiDashboardSummary topProduktif = null;
		PegawaiDashboardSummary topRisiko = null;
		for (PegawaiDashboardSummary h : summaries) {
			if (topProduktif == null || h.catatan > topProduktif.catatan) {
				topProduktif = h;
			}
			if (topRisiko == null || h.tanpaKegiatan > topRisiko.tanpaKegiatan) {
				topRisiko = h;
			}
		}

		String statusUmum;
		String warnaStatus;
		if (rasioKegiatan >= 90.0 && rasioPulangLengkap >= 90.0 && rasioTanpaKegiatan <= 10.0) {
			statusUmum = "Sangat Baik";
			warnaStatus = "#198754";
		} else if (rasioKegiatan >= 75.0 && rasioPulangLengkap >= 80.0) {
			statusUmum = "Baik, tetap perlu monitoring";
			warnaStatus = "#0d6efd";
		} else if (rasioKegiatan >= 60.0) {
			statusUmum = "Cukup, perlu peningkatan konsistensi catatan";
			warnaStatus = "#fd7e14";
		} else {
			statusUmum = "Perlu perhatian, banyak hari hadir belum diikuti catatan kegiatan";
			warnaStatus = "#dc3545";
		}

		StringBuffer sb = new StringBuffer();
		sb.append("<div style='background:#ffffff; border:1px solid #e9ecef; border-left:6px solid ")
				.append(warnaStatus)
				.append("; border-radius:16px; padding:14px; box-shadow:0 4px 16px rgba(0,0,0,0.04);'>");
		sb.append(
				"<div style='font-size:16px; font-weight:800; color:#212529; margin-bottom:8px;'>Rekap Summary & Insight Analitik</div>");
		sb.append("<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:10px;'>");
		sb.append(buildInsightItem("Status Umum", statusUmum, warnaStatus));
		sb.append(buildInsightItem("Rasio Kegiatan", formatAngka(rasioKegiatan) + "%", warnaPersen(rasioKegiatan)));
		sb.append(buildInsightItem("Kelengkapan Pulang", formatAngka(rasioPulangLengkap) + "%",
				warnaPersen(rasioPulangLengkap)));
		sb.append(buildInsightItem("Risiko Tanpa Kegiatan", formatAngka(rasioTanpaKegiatan) + "%",
				warnaPersen(100.0 - rasioTanpaKegiatan)));
		if (topProduktif != null) {
			sb.append(buildInsightItem("Paling Produktif",
					topProduktif.nama + " (" + formatAngka(topProduktif.catatan) + " catatan)", "#6f42c1"));
		}
		if (topRisiko != null && topRisiko.tanpaKegiatan > 0.0) {
			sb.append(buildInsightItem("Prioritas Monitoring",
					topRisiko.nama + " (" + formatAngka(topRisiko.tanpaKegiatan) + " hari)", "#dc3545"));
		}
		sb.append("</div>");
		sb.append("<div style='font-size:12px; color:#6c757d; margin-top:10px;'>"
				+ "Catatan: angka pada tabel detail tetap dapat diklik untuk menelusuri data Realisasi Kerja atau Presensi pegawai.</div>");
		sb.append("</div>");
		return sb.toString();
	}

	private String buildInsightItem(String title, String value, String color) {
		return "<div style='background:#f8f9fa; border-radius:12px; padding:10px; border:1px solid #eef1f4;'>"
				+ "<div style='font-size:11px; color:#6c757d; text-transform:uppercase; font-weight:800;'>"
				+ escapeHtml(title) + "</div>" + "<div style='font-size:15px; color:" + color
				+ "; font-weight:900; margin-top:3px;'>" + escapeHtml(value) + "</div>" + "</div>";
	}

	private void appendBar(StringBuffer sb, String label, double value, double max, String color, String caption) {
		sb.append("<div style='margin-bottom:10px;'>");
		sb.append(
				"<div style='display:flex; justify-content:space-between; gap:10px; font-size:12px; margin-bottom:4px;'>");
		sb.append("<span style='font-weight:700; color:#212529;'>").append(escapeHtml(label)).append("</span>");
		sb.append("<span style='color:#6c757d;'>").append(formatAngka(value)).append("</span>");
		sb.append("</div>");
		sb.append("<div style='height:12px; background:#eef1f4; border-radius:999px; overflow:hidden;'>");
		sb.append("<div style='height:12px; width:").append(barWidth(value, max)).append("%; background:").append(color)
				.append("; border-radius:999px;'></div>");
		sb.append("</div>");
		sb.append("<div style='font-size:11px; color:#8a8f94; margin-top:2px;'>").append(escapeHtml(caption))
				.append("</div>");
		sb.append("</div>");
	}

	private void appendRankBar(StringBuffer sb, int no, String label, double value, double max, String color,
			String caption) {
		sb.append("<div style='margin-bottom:10px;'>");
		sb.append(
				"<div style='display:flex; justify-content:space-between; gap:10px; font-size:12px; margin-bottom:4px;'>");
		sb.append(
				"<span style='font-weight:700; color:#212529; max-width:62%; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;'>")
				.append(no).append(". ").append(escapeHtml(label)).append("</span>");
		sb.append("<span style='color:#6c757d;'>").append(escapeHtml(caption)).append("</span>");
		sb.append("</div>");
		sb.append("<div style='height:10px; background:#eef1f4; border-radius:999px; overflow:hidden;'>");
		sb.append("<div style='height:10px; width:").append(barWidth(value, max)).append("%; background:").append(color)
				.append("; border-radius:999px;'></div>");
		sb.append("</div>");
		sb.append("</div>");
	}

	private String emptyChartMessage(String message) {
		return "<div style='padding:20px; border-radius:12px; background:#f8f9fa; color:#6c757d; text-align:center; font-size:12px;'>"
				+ escapeHtml(message) + "</div>";
	}

	private List<PegawaiDashboardSummary> buildPegawaiDashboardSummaries(Map<Long, List<Double>> mapData) {
		List<PegawaiDashboardSummary> summaries = new ArrayList<PegawaiDashboardSummary>();
		if (pegawais == null) {
			return summaries;
		}
		for (Pegawai pegawai : pegawais) {
			List<Double> data = mapData.get(pegawai == null || pegawai.getId() == null ? -1L : pegawai.getId());
			PegawaiDashboardSummary h = new PegawaiDashboardSummary();
			h.pegawai = pegawai;
			h.nama = pegawai == null || pegawai.getNama() == null ? "-" : pegawai.getNama();
			h.satuanKerja = pegawai == null || pegawai.getSatuanKerja() == null ? "-"
					: pegawai.getSatuanKerja().getNama();
			h.catatan = getValue(data, 0);
			h.kegiatan = getValue(data, 1);
			h.datang = getValue(data, 2);
			h.pulang = getValue(data, 3);
			h.tanpaKegiatan = getValue(data, 4);
			h.rataRata = h.datang < 0.1 ? 0.0 : h.kegiatan / h.datang;
			h.rasioKegiatan = h.datang < 0.1 ? 0.0 : h.kegiatan * 100.0 / h.datang;
			h.rasioPulang = h.datang < 0.1 ? 0.0 : h.pulang * 100.0 / h.datang;
			h.rasioTanpaKegiatan = (h.kegiatan + h.tanpaKegiatan) < 0.1 ? 0.0
					: h.tanpaKegiatan * 100.0 / (h.kegiatan + h.tanpaKegiatan);
			summaries.add(h);
		}
		return summaries;
	}

	private double getTotal(Map<Integer, Double> listTotals, int key) {
		Double value = listTotals == null ? null : listTotals.get(key);
		return value == null ? 0.0 : value.doubleValue();
	}

	private double getValue(List<Double> data, int index) {
		if (data == null || data.size() <= index || data.get(index) == null) {
			return 0.0;
		}
		return data.get(index).doubleValue();
	}

	private double barWidth(double value, double max) {
		if (max <= 0.0 || value <= 0.0) {
			return 0.0;
		}
		double width = value * 100.0 / max;
		if (width > 100.0) {
			width = 100.0;
		}
		if (width > 0.0 && width < 3.0) {
			width = 3.0;
		}
		return width;
	}

	private String warnaPersen(double persen) {
		if (persen >= 90.0) {
			return "#198754";
		}
		if (persen >= 75.0) {
			return "#0d6efd";
		}
		if (persen >= 60.0) {
			return "#fd7e14";
		}
		return "#dc3545";
	}

	private String formatAngka(double value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String formatPeriode() {
		return formatTanggal(start == null ? null : start.getValue()) + " s/d "
				+ formatTanggal(end == null ? null : end.getValue());
	}

	private String formatTanggal(Date date) {
		if (date == null) {
			return "-";
		}
		try {
			return Common.dateFormat1.get().format(date);
		} catch (Exception e) {
			return String.valueOf(date);
		}
	}

	private String escapeHtml(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'",
				"&#39;");
	}

	private static class PegawaiDashboardSummary {
		Pegawai pegawai;
		String nama;
		String satuanKerja;
		double catatan;
		double kegiatan;
		double datang;
		double pulang;
		double tanpaKegiatan;
		double rataRata;
		double rasioKegiatan;
		double rasioPulang;
		double rasioTanpaKegiatan;
	}

}
