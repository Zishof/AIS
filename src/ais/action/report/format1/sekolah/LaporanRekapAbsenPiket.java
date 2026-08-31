package ais.action.report.format1.sekolah;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
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

import ais.action.master.sekolah.helper.AmbilDataKelasSiswaBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Perkuliahan;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.AbsenGuruPiket;
import ais.database.model.sekolah.AbsenPiket;
import ais.database.model.sekolah.AbsenPiketDetail;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.EcampusUtil;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Penyusun/penyaji laporan untuk laporan rekap absen piket. Kelas ini mengubah data domain menjadi
 * bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke
 * lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Spreadsheet excelku}, {@code
 * AmbilDataKelasSiswaBanbox searchnama}, {@code MyDatebox mulai}, {@code MyDatebox sampai}, {@code Center
 * center}, {@code MyToolbarbuttonConfig printAmbil}, {@code Combobox yayasan}, {@code Combobox sekolah};
 * inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor ({@code onCetak()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapAbsenPiket extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Spreadsheet excelku;

	private AmbilDataKelasSiswaBanbox searchnama = new AmbilDataKelasSiswaBanbox();

	private MyDatebox mulai = new MyDatebox();
	private MyDatebox sampai = new MyDatebox(WaktuUtil.getDate());

	private Center center;

	private MyToolbarbuttonConfig printAmbil;

	private Combobox yayasan;

	private Combobox sekolah;

	private Combobox searchta;

	private Combobox searchsmt;

	private Tbmuser tbmuser;

	public LaporanRekapAbsenPiket() throws Exception {
		super();
		init();
	}

	public LaporanRekapAbsenPiket(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() throws Exception {

		tbmuser = Common.getCurrentUser();

		yayasan = new Combobox();
		sekolah = new Combobox();

		searchsmt = new Combobox();
		searchta = new Combobox();

		Common.generateTahunAjaran(searchta);

		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		comboitem.setValue(1);
		searchsmt.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		comboitem.setValue(2);
		searchsmt.appendChild(comboitem);
		searchsmt.setCols(2);

		Common.selectComboItem(searchsmt, Common.isNowSemensterGanjil() ? 1 : 2);
		searchsmt.setReadonly(true);

		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		Vbox vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		vbox.appendChild(yayasan);
		yayasan.setCols(6);

		row = new MyFormRow();
		row.setParent(rows);

		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		vbox.appendChild(sekolah);
		sekolah.setCols(6);

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

		if (tbmuser == null || tbmuser.getSiswa() == null) {
			row = new MyFormRow();
			row.setParent(rows);
			vbox = new Vbox();
			vbox.setParent(row);
			vbox.setWidth("100%");
			vbox.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
			vbox.appendChild(searchnama);
			searchnama.setCols(6);

			searchta.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					searchnama.ta = (String) (searchta.getSelectedItem() == null ? Common.getCurrentTahunAkademik()
							: searchta.getSelectedItem().getValue());
					Common.selectComboItem(searchnama.tahunAkademik,
							searchta.getSelectedItem() == null ? null : searchta.getSelectedItem().getValue());
					searchnama.onSearchDefault(arg0);
				}
			});

			searchnama.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					KelasSiswa kelas = (KelasSiswa) searchnama.getAttribute("kelasSiswa");

					if (kelas != null && kelas.getSekolah() != null) {
						Common.pilihSekolah(sekolah, kelas.getSekolah());
					}
				}
			});
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		vbox.appendChild(mulai);
		mulai.setCols(6);
		mulai.setValue(calendar.getTime());
		mulai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		vbox.appendChild(sampai);
		sampai.setCols(6);
		sampai.setReadonly(true);

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
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "absen_piket.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRekapAbsenPiket.java:272");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Absen Piket", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
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

			Sekolah seko = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
			if (seko == null) {
				MyMessageboxConfig.show("Sekolah harus dipilih", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}

			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

			new Thread(new Runnable() {

				@Override
				public void run() {

					KelasSiswa kelas = (KelasSiswa) searchnama.getAttribute("kelasSiswa");

					Date m = mulai.getValue();
					Date s = sampai.getValue();

					datas = new ArrayList<List>();

					Sekolah seko = (Sekolah) (sekolah.getSelectedItem() == null ? null
							: sekolah.getSelectedItem().getValue());

					if (kelas != null && kelas.getSekolah() != null) {
						seko = kelas.getSekolah();
					}

					List<Integer> jamKes = AbsenGuruPiket.jamKes(seko == null || seko.getId() == null ? 0L : seko.getId());

					Session session = HibernateUtil.currentSession();
					List<AbsenPiket> absenPikets = session.createCriteria(AbsenPiket.class)

							.add(Restrictions.sqlRestriction(
									"date(this_.tanggal) between date('" + Common.databaseDateFormat.get().format(m)
											+ "') and  date('" + Common.databaseDateFormat.get().format(s) + "')"))

							.add(searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null
									|| searchsmt.getSelectedItem().getValue() == null
											? Restrictions.sqlRestriction("1=1")
											: Restrictions.eq("semester", searchsmt.getSelectedItem().getValue()))

							.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
									|| searchta.getSelectedItem().getValue() == null
											? Restrictions.sqlRestriction("1=1")
											: Restrictions.eq("tahunAjaran", searchta.getSelectedItem().getValue()))

							.add(sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null
									|| sekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
											: CommonSearchFilterHelper.eqSelectedWithId("sekolah", sekolah, false))

							.add(yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null
									|| yayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
											: CommonSearchFilterHelper.eqSelectedWithId("yayasan", yayasan, false))

							.addOrder(Order.asc("tanggal"))

							.add(kelas == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("kelas", kelas))

							.list();

					TreeMap<String, AbsenPiket> myItems = new TreeMap<String, AbsenPiket>();
					TreeMap<String, KelasSiswa> kelases = new TreeMap<String, KelasSiswa>();
					for (AbsenPiket absenPiket : absenPikets) {
						myItems.put(Common.dateFormat1.get().format(absenPiket.getTanggal()) + "_"
								+ absenPiket.getKelas().getId(), absenPiket);
						kelases.put(absenPiket.getKelas().getNama() + "_" + absenPiket.getKelas().getId(),
								absenPiket.getKelas());

					}

					List<Long> anak = tbmuser != null && tbmuser.getOrangTua() != null
							? tbmuser.getOrangTua().ambilAnakSiswa()
							: new ArrayList<Long>();

					Map<String, Integer[]> mapsTotalPerJenis = null;
					int nomor = 1;
					for (KelasSiswa kelasSiswa : kelases.values()) {

						/* Callee di iterasi sebelumnya bisa menutup session
						 * thread-local milik thread ini ("Session is closed!");
						 * ambil ulang bila sudah tertutup. */
						if (session == null || !session.isOpen()) {
							session = HibernateUtil.currentSession();
						}

						List<KelasSiswaPunyaSiswa> kelasSiswaPunyaSiswas = ConstantValues.simpleList(
								session.createCriteria(KelasSiswaPunyaSiswa.class)

										.add(tbmuser != null && tbmuser.getSiswa() != null
												? Restrictions.eq("siswa", tbmuser.getSiswa())
												: Restrictions.sqlRestriction("true"))

										.add(Restrictions.eq("kelasSiswa", kelasSiswa)).createAlias("siswa", "siswa")

										.add(anak.isEmpty() ? Restrictions.sqlRestriction("true")
												: Restrictions.in("siswa.id", anak))

										.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.nama")),
								KelasSiswaPunyaSiswa.class);

						if (!kelasSiswaPunyaSiswas.isEmpty()) {
							label.setValue("Sedang memproses kelas " + kelasSiswa.toString());
							String n = kelasSiswa.getNama() + " " + (kelasSiswa.getGuruPembina() == null ? ""
									: kelasSiswa.getGuruPembina().getNama());

							TreeMap<String, Date> dates = new TreeMap<String, Date>();
							for (AbsenPiket absenPiket : absenPikets) {
								if (absenPiket.getKelas().getId().equals(kelasSiswa.getId())) {
									dates.put(Common.dateFormat8.get().format(absenPiket.getTanggal()),
											absenPiket.getTanggal());
								}
							}

							try {

								List sub = new ArrayList();

								sub.add("");
								sub.add("");
								sub.add("");
								sub.add("");
								for (@SuppressWarnings("unused")
								Date date : dates.values()) {
									if (jamKes.isEmpty()) {
										sub.add("");
									} else {
										for (@SuppressWarnings("unused")
										Integer jam : jamKes) {
											sub.add("");
										}
									}
								}
								sub.add("");
								sub.add("");
								sub.add("");
								sub.add("");

								sub.add("");
								sub.add("");
								sub.add("");
								sub.add("");
								sub.add("");
								datas.add(sub);

								sub = new ArrayList();

								sub.add("");
								sub.add(n);
								sub.add("");
								sub.add("");
								for (@SuppressWarnings("unused")
								Date date : dates.values()) {
									if (jamKes.isEmpty()) {
										sub.add("");
									} else {
										for (@SuppressWarnings("unused")
										Integer jam : jamKes) {
											sub.add("");
										}
									}
								}
								sub.add("");
								sub.add("");
								sub.add("");
								sub.add("");

								sub.add("");
								sub.add("");
								sub.add("");
								sub.add("");
								sub.add("");

								datas.add(sub);

								sub = new ArrayList();
								sub.add("**No.");
								sub.add("**NIS");
								sub.add("**Nama");
								sub.add("**NISN");

								for (Date date : dates.values()) {
									if (jamKes.isEmpty()) {
										sub.add("**" + Common.simpleDateFormat2.get().format(date));
									} else {
										for (Integer jam : jamKes) {
											sub.add("**" + Common.simpleDateFormat2.get().format(date) + "-" + jam);
										}
									}
								}
								sub.add("**Total Hadir");
								sub.add("**Total Sakit");
								sub.add("**Total Izin");
								sub.add("**Total Alpa");
								sub.add("**Total Absen");

								sub.add("**(%)Hadir");
								sub.add("**(%)Sakit");
								sub.add("**(%)Izin");
								sub.add("**(%)Alpa");

								datas.add(sub);

							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRekapAbsenPiket.java:502");
								// TODO: handle exception
							}

							mapsTotalPerJenis = new HashMap<String, Integer[]>();
							nomor = 1;

							try {

								for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : kelasSiswaPunyaSiswas) {
									Siswa siswa = kelasSiswaPunyaSiswa.getSiswa();
									label.setValue("Sedang memproses data " + siswa.toString());

									List sub = new ArrayList();
									sub.add(nomor);
									sub.add(siswa.getNomorInduk());
									sub.add(siswa.getNama());
									sub.add(siswa.getNomorIndukNasional());
									int hadir = 0;
									int sakit = 0;
									int izin = 0;
									int alpa = 0;
									int semua = 0;
									for (Date date : dates.values()) {

										AbsenPiket absenPiket = myItems
												.get(Common.dateFormat1.get().format(date) + "_" + kelasSiswa.getId());

										if (jamKes.isEmpty()) {
											Integer jamke = 0;
											AbsenPiketDetail absenPiketDetail = absenPiket == null ? null
													: AbsenPiketDetail.ambil(null, siswa, absenPiket,
															absenPiket.getKelas().getAbsensi(), jamke);

											Statusabsensi statusabsensi = (Statusabsensi) (absenPiketDetail == null
													? ConstantValues.BELUM_ABSEN
													: ConstantValues.ambil(Statusabsensi.class.getName(),
															absenPiketDetail.retreiveAbsensiId(
																	siswa.getId() + "_" + absenPiket.getId())));
											if (statusabsensi == null) {
												statusabsensi = ConstantValues.BELUM_ABSEN;
											}

											sub.add(statusabsensi.getKode());
											Integer h = ConstantValues.MASUK != null
													&& statusabsensi.getId().equals(ConstantValues.MASUK.getId()) ? 1
															: 0;
											Integer ss = ConstantValues.SAKIT != null
													&& statusabsensi.getId().equals(ConstantValues.SAKIT.getId()) ? 1
															: 0;
											Integer ii = ConstantValues.IZIN != null
													&& statusabsensi.getId().equals(ConstantValues.IZIN.getId()) ? 1
															: 0;
											Integer a = ConstantValues.TIDAK_ADA_ALASAN != null && statusabsensi.getId()
													.equals(ConstantValues.TIDAK_ADA_ALASAN.getId()) ? 1 : 0;

											String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;

											Integer[] totalSemuaPerJenis = mapsTotalPerJenis.get(key);
											if (totalSemuaPerJenis == null) {
												totalSemuaPerJenis = new Integer[] { 0, 0, 0, 0 };
											}

											totalSemuaPerJenis[0] += h;
											totalSemuaPerJenis[1] += ss;
											totalSemuaPerJenis[2] += ii;
											totalSemuaPerJenis[3] += a;
											mapsTotalPerJenis.put(key, totalSemuaPerJenis);

											hadir += h;
											sakit += ss;
											izin += ii;
											alpa += a;

											semua++;
										} else {
											for (Integer jamke : jamKes) {
												AbsenPiketDetail absenPiketDetail = absenPiket == null ? null
														: AbsenPiketDetail.ambil(null, siswa, absenPiket,
																absenPiket.getKelas().getAbsensi(), jamke);

												Statusabsensi statusabsensi = (Statusabsensi) (absenPiketDetail == null
														? ConstantValues.BELUM_ABSEN
														: ConstantValues.ambil(Statusabsensi.class.getName(),
																absenPiketDetail.retreiveAbsensiId(
																		siswa.getId() + "_" + absenPiket.getId())));
												if (statusabsensi == null) {
													statusabsensi = ConstantValues.BELUM_ABSEN;
												}

												sub.add(statusabsensi.getKode());
												Integer h = ConstantValues.MASUK != null
														&& statusabsensi.getId().equals(ConstantValues.MASUK.getId())
																? 1
																: 0;
												Integer ss = ConstantValues.SAKIT != null
														&& statusabsensi.getId().equals(ConstantValues.SAKIT.getId())
																? 1
																: 0;
												Integer ii = ConstantValues.IZIN != null
														&& statusabsensi.getId().equals(ConstantValues.IZIN.getId()) ? 1
																: 0;
												Integer a = ConstantValues.TIDAK_ADA_ALASAN != null && statusabsensi
														.getId().equals(ConstantValues.TIDAK_ADA_ALASAN.getId()) ? 1
																: 0;

												String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;

												Integer[] totalSemuaPerJenis = mapsTotalPerJenis.get(key);
												if (totalSemuaPerJenis == null) {
													totalSemuaPerJenis = new Integer[] { 0, 0, 0, 0 };
												}

												totalSemuaPerJenis[0] += h;
												totalSemuaPerJenis[1] += ss;
												totalSemuaPerJenis[2] += ii;
												totalSemuaPerJenis[3] += a;
												mapsTotalPerJenis.put(key, totalSemuaPerJenis);

												hadir += h;
												sakit += ss;
												izin += ii;
												alpa += a;

												semua++;
											}
										}
									}

									sub.add("**" + Common.numberFormat.get().format(hadir));
									sub.add("**" + Common.numberFormat.get().format(sakit));
									sub.add("**" + Common.numberFormat.get().format(izin));
									sub.add("**" + Common.numberFormat.get().format(alpa));
									sub.add("**" + Common.numberFormat.get().format(semua));

									sub.add("**"
											+ Common.numberFormat.get().format(semua == 0 ? 0.0 : (hadir * 100.0 / semua))
											+ "%");
									sub.add("**"
											+ Common.numberFormat.get().format(semua == 0 ? 0.0 : (sakit * 100.0 / semua))
											+ "%");
									sub.add("**" + Common.numberFormat.get().format(semua == 0 ? 0.0 : (izin * 100.0 / semua))
											+ "%");
									sub.add("**" + Common.numberFormat.get().format(semua == 0 ? 0.0 : (alpa * 100.0 / semua))
											+ "%");

									System.out.println("sub =>" + sub);
									datas.add(sub);

									nomor++;
								}

								try {

									ArrayList sub = new ArrayList();
									sub.add("");
									sub.add("**Total");
									sub.add("**");
									sub.add("**");
									int hadir = 0;
									int sakit = 0;
									int izin = 0;
									int alpa = 0;
									int semua = 0;
									for (Date date : dates.values()) {
										if (jamKes.isEmpty()) {
											Integer jamke = 0;
											String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
											Integer[] totalSemua = mapsTotalPerJenis.get(key);
											if (totalSemua == null) {
												totalSemua = new Integer[] { 0, 0, 0, 0 };
											}
											hadir += totalSemua[0];
											sakit += totalSemua[1];
											izin += totalSemua[2];
											alpa += totalSemua[3];

											int t = totalSemua[0] + totalSemua[1] + totalSemua[2] + totalSemua[3];
											semua += t;

											sub.add("**" + Common.numberFormat.get().format(t));
										} else {

											for (Integer jamke : jamKes) {
												String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
												Integer[] totalSemua = mapsTotalPerJenis.get(key);
												if (totalSemua == null) {
													totalSemua = new Integer[] { 0, 0, 0, 0 };
												}
												hadir += totalSemua[0];
												sakit += totalSemua[1];
												izin += totalSemua[2];
												alpa += totalSemua[3];

												int t = totalSemua[0] + totalSemua[1] + totalSemua[2] + totalSemua[3];
												semua += t;

												sub.add("**" + Common.numberFormat.get().format(t));
											}

										}
									}
									sub.add("**" + Common.numberFormat.get().format(hadir));
									sub.add("**" + Common.numberFormat.get().format(sakit));
									sub.add("**" + Common.numberFormat.get().format(izin));
									sub.add("**" + Common.numberFormat.get().format(alpa));
									sub.add("**" + Common.numberFormat.get().format(semua));

									sub.add("**"
											+ Common.numberFormat.get().format(semua == 0 ? 0.0 : (hadir * 100.0 / semua))
											+ "%");
									sub.add("**"
											+ Common.numberFormat.get().format(semua == 0 ? 0.0 : (sakit * 100.0 / semua))
											+ "%");
									sub.add("**" + Common.numberFormat.get().format(semua == 0 ? 0.0 : (izin * 100.0 / semua))
											+ "%");
									sub.add("**" + Common.numberFormat.get().format(semua == 0 ? 0.0 : (alpa * 100.0 / semua))
											+ "%");

									datas.add(sub);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Absen Piket", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
								}

								try {

									ArrayList sub = new ArrayList();
									sub.add("");
									sub.add("**Total Hadir");
									sub.add("**");
									sub.add("**");
									for (Date date : dates.values()) {
										if (jamKes.isEmpty()) {
											Integer jamke = 0;
											String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
											Integer[] totalSemua = mapsTotalPerJenis.get(key);
											if (totalSemua == null) {
												totalSemua = new Integer[] { 0, 0, 0, 0 };
											}

											sub.add("**" + Common.numberFormat.get().format(totalSemua[0]));
										} else {

											for (Integer jamke : jamKes) {
												String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
												Integer[] totalSemua = mapsTotalPerJenis.get(key);
												if (totalSemua == null) {
													totalSemua = new Integer[] { 0, 0, 0, 0 };
												}

												sub.add("**" + Common.numberFormat.get().format(totalSemua[0]));
											}
										}
									}
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");

									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");

									datas.add(sub);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Absen Piket", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
								}

								try {

									ArrayList sub = new ArrayList();
									sub.add("");
									sub.add("**Total Sakit");
									sub.add("**");
									sub.add("**");
									for (Date date : dates.values()) {
										if (jamKes.isEmpty()) {
											Integer jamke = 0;
											String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
											Integer[] totalSemua = mapsTotalPerJenis.get(key);
											if (totalSemua == null) {
												totalSemua = new Integer[] { 0, 0, 0, 0 };
											}

											sub.add("**" + Common.numberFormat.get().format(totalSemua[1]));
										} else {

											for (Integer jamke : jamKes) {
												String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
												Integer[] totalSemua = mapsTotalPerJenis.get(key);
												if (totalSemua == null) {
													totalSemua = new Integer[] { 0, 0, 0, 0 };
												}

												sub.add("**" + Common.numberFormat.get().format(totalSemua[1]));
											}
										}
									}
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");

									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");

									datas.add(sub);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Absen Piket", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
								}

								try {

									ArrayList sub = new ArrayList();
									sub.add("");
									sub.add("**Total Izin");
									sub.add("**");
									sub.add("**");
									for (Date date : dates.values()) {
										if (jamKes.isEmpty()) {
											Integer jamke = 0;
											String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
											Integer[] totalSemua = mapsTotalPerJenis.get(key);
											if (totalSemua == null) {
												totalSemua = new Integer[] { 0, 0, 0, 0 };
											}

											sub.add("**" + Common.numberFormat.get().format(totalSemua[2]));
										} else {

											for (Integer jamke : jamKes) {
												String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
												Integer[] totalSemua = mapsTotalPerJenis.get(key);
												if (totalSemua == null) {
													totalSemua = new Integer[] { 0, 0, 0, 0 };
												}

												sub.add("**" + Common.numberFormat.get().format(totalSemua[2]));
											}
										}
									}
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");

									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");

									datas.add(sub);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Absen Piket", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
								}

								try {

									ArrayList sub = new ArrayList();
									sub.add("");
									sub.add("**Total Alpa");
									sub.add("**");
									sub.add("**");
									for (Date date : dates.values()) {
										if (jamKes.isEmpty()) {
											Integer jamke = 0;
											String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
											Integer[] totalSemua = mapsTotalPerJenis.get(key);
											if (totalSemua == null) {
												totalSemua = new Integer[] { 0, 0, 0, 0 };
											}

											sub.add("**" + Common.numberFormat.get().format(totalSemua[3]));
										} else {

											for (Integer jamke : jamKes) {
												String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
												Integer[] totalSemua = mapsTotalPerJenis.get(key);
												if (totalSemua == null) {
													totalSemua = new Integer[] { 0, 0, 0, 0 };
												}

												sub.add("**" + Common.numberFormat.get().format(totalSemua[3]));
											}
										}
									}
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");

									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");

									datas.add(sub);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Absen Piket", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
								}

								try {

									ArrayList sub = new ArrayList();
									sub.add("");
									sub.add("**% Hadir");
									sub.add("**");
									sub.add("**");
									for (Date date : dates.values()) {
										if (jamKes.isEmpty()) {
											Integer jamke = 0;
											String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
											Integer[] totalSemua = mapsTotalPerJenis.get(key);
											if (totalSemua == null) {
												totalSemua = new Integer[] { 0, 0, 0, 0 };
											}

											int t = totalSemua[0] + totalSemua[1] + totalSemua[2] + totalSemua[3];
											sub.add("**" + Common.numberFormat.get()
													.format(t == 0 ? 0 : ((totalSemua[0] * 100) / t)) + "%");
										} else {

											for (Integer jamke : jamKes) {
												String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
												Integer[] totalSemua = mapsTotalPerJenis.get(key);
												if (totalSemua == null) {
													totalSemua = new Integer[] { 0, 0, 0, 0 };
												}

												int t = totalSemua[0] + totalSemua[1] + totalSemua[2] + totalSemua[3];
												sub.add("**" + Common.numberFormat.get()
														.format(t == 0 ? 0 : ((totalSemua[0] * 100) / t)) + "%");
											}
										}
									}
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");

									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");

									datas.add(sub);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Absen Piket", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
								}

								try {

									ArrayList sub = new ArrayList();
									sub.add("");
									sub.add("**% Sakit");
									sub.add("**");
									sub.add("**");
									for (Date date : dates.values()) {
										if (jamKes.isEmpty()) {
											Integer jamke = 0;
											String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
											Integer[] totalSemua = mapsTotalPerJenis.get(key);
											if (totalSemua == null) {
												totalSemua = new Integer[] { 0, 0, 0, 0 };
											}

											int t = totalSemua[0] + totalSemua[1] + totalSemua[2] + totalSemua[3];
											sub.add("**" + Common.numberFormat.get()
													.format(t == 0 ? 0 : ((totalSemua[1] * 100) / t)) + "%");
										} else {

											for (Integer jamke : jamKes) {
												String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
												Integer[] totalSemua = mapsTotalPerJenis.get(key);
												if (totalSemua == null) {
													totalSemua = new Integer[] { 0, 0, 0, 0 };
												}

												int t = totalSemua[0] + totalSemua[1] + totalSemua[2] + totalSemua[3];
												sub.add("**" + Common.numberFormat.get()
														.format(t == 0 ? 0 : ((totalSemua[1] * 100) / t)) + "%");
											}
										}
									}
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");

									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");

									datas.add(sub);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Absen Piket", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
								}

								try {

									ArrayList sub = new ArrayList();
									sub.add("");
									sub.add("**% Izin");
									sub.add("**");
									sub.add("**");
									for (Date date : dates.values()) {
										if (jamKes.isEmpty()) {
											Integer jamke = 0;
											String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
											Integer[] totalSemua = mapsTotalPerJenis.get(key);
											if (totalSemua == null) {
												totalSemua = new Integer[] { 0, 0, 0, 0 };
											}

											int t = totalSemua[0] + totalSemua[1] + totalSemua[2] + totalSemua[3];
											sub.add("**" + Common.numberFormat.get()
													.format(t == 0 ? 0 : ((totalSemua[2] * 100) / t)) + "%");
										} else {

											for (Integer jamke : jamKes) {
												String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
												Integer[] totalSemua = mapsTotalPerJenis.get(key);
												if (totalSemua == null) {
													totalSemua = new Integer[] { 0, 0, 0, 0 };
												}

												int t = totalSemua[0] + totalSemua[1] + totalSemua[2] + totalSemua[3];
												sub.add("**" + Common.numberFormat.get()
														.format(t == 0 ? 0 : ((totalSemua[2] * 100) / t)) + "%");
											}
										}
									}
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");

									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");

									datas.add(sub);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Absen Piket", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
								}

								try {

									ArrayList sub = new ArrayList();
									sub.add("");
									sub.add("**% Alpa");
									sub.add("**");
									sub.add("**");
									for (Date date : dates.values()) {
										if (jamKes.isEmpty()) {
											Integer jamke = 0;
											String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
											Integer[] totalSemua = mapsTotalPerJenis.get(key);
											if (totalSemua == null) {
												totalSemua = new Integer[] { 0, 0, 0, 0 };
											}

											int t = totalSemua[0] + totalSemua[1] + totalSemua[2] + totalSemua[3];
											sub.add("**" + Common.numberFormat.get()
													.format(t == 0 ? 0 : ((totalSemua[3] * 100) / t)) + "%");
										} else {

											for (Integer jamke : jamKes) {
												String key = Common.simpleDateFormat2.get().format(date) + "_" + jamke;
												Integer[] totalSemua = mapsTotalPerJenis.get(key);
												if (totalSemua == null) {
													totalSemua = new Integer[] { 0, 0, 0, 0 };
												}

												int t = totalSemua[0] + totalSemua[1] + totalSemua[2] + totalSemua[3];
												sub.add("**" + Common.numberFormat.get()
														.format(t == 0 ? 0 : ((totalSemua[3] * 100) / t)) + "%");
											}
										}
									}
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");

									sub.add("**");
									sub.add("**");
									sub.add("**");
									sub.add("**");

									datas.add(sub);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Absen Piket", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
								}

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Absen Piket", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
							}

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

						ais.action.report.helper.LoadingReportUtil.clearBusy();
						excelku = new ais.ui.util.MySpreadsheet();
						Common.clear(center);
						center.appendChild(excelku);
						EcampusUtil.tampilkan(datas, excelku);
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
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Absen Piket", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
