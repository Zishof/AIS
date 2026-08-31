package ais.action.report.format1.sekolah;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.sekolah.helper.AmbilDataKelasSiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
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
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Penyusun/penyaji laporan untuk laporan rekap absen piket harian. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyDatebox mulai}, {@code MyDatebox
 * sampai}, {@code AmbilDataKelasSiswaBanbox searchnama}, {@code Center center}, {@code Combobox yayasan}, {@code
 * Combobox sekolah}, {@code Combobox searchta}, {@code Combobox searchsmt}; inisialisasi/lifecycle ({@code
 * init()}); pelaporan/ekspor ({@code onCetak()}); operasi domain lain ({@code generateParameter()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapAbsenPiketHarian extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private MyDatebox mulai = new MyDatebox();
	private MyDatebox sampai = new MyDatebox(WaktuUtil.getDate());
	private AmbilDataKelasSiswaBanbox searchnama = new AmbilDataKelasSiswaBanbox();
	private Center center;
	private Combobox yayasan;

	private Combobox sekolah;

	private Combobox searchta;

	private Combobox searchsmt;

	private Toolbar toolbar;

	private Tbmuser tbmuser;

	public LaporanRekapAbsenPiketHarian() throws Exception {
		super();
		init();
	}

	public LaporanRekapAbsenPiketHarian(String title, String border, boolean closable) throws Exception {
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

		Vbox v = new Vbox();
		v.setParent(row);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(null);
			}
		});
		print.setParent(v);

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
		}, "Laporan_Absensi_Per_Hari_Siswa", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(null);
			}
		}));

	}

	@SuppressWarnings("rawtypes")
	private List<Map> maps = null;

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

					Date m = mulai.getValue();
					Date s = sampai.getValue();

					maps = new ArrayList<Map>();

					Sekolah seko = (Sekolah) (sekolah.getSelectedItem() == null ? null
							: sekolah.getSelectedItem().getValue());
					KelasSiswa kelas = (KelasSiswa) searchnama.getAttribute("kelasSiswa");

					if (kelas != null && kelas.getSekolah() != null) {
						seko = kelas.getSekolah();
					}
					List<Integer> jamKes = AbsenGuruPiket.jamKes(seko == null || seko.getId() == null ? 0L : seko.getId());

					Session session = HibernateUtil.getSessionFactory().openSession();
					try {
					List<AbsenPiket> absenPikets = session.createCriteria(AbsenPiket.class)

							.add(kelas == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("kelas", kelas))

							.add(Restrictions.isNotNull("kelas"))

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

							.list();

					TreeMap<String, AbsenPiket> myItems = new TreeMap<String, AbsenPiket>();
					TreeMap<String, KelasSiswa> kelases = new TreeMap<String, KelasSiswa>();
					for (AbsenPiket absenPiket : absenPikets) {
						myItems.put(Common.dateFormat1.get().format(absenPiket.getTanggal()), absenPiket);
						kelases.put(absenPiket.getKelas().getNama() + "_" + absenPiket.getSekolah().getId(),
								absenPiket.getKelas());

					}

					for (KelasSiswa kelasSiswa : kelases.values()) {
						label.setValue("Sedang memproses kelas " + kelasSiswa.getNama());

						TreeMap<String, Date> dates = new TreeMap<String, Date>();
						for (AbsenPiket absenPiket : absenPikets) {
							if (absenPiket.getKelas().getId().equals(kelasSiswa.getId())) {
								dates.put(Common.dateFormat8.get().format(absenPiket.getTanggal()), absenPiket.getTanggal());
							}
						}

						try {

							List<Siswa> siswas = ConstantValues.simpleList(session
									.createCriteria(KelasSiswaPunyaSiswa.class)
									.setProjection(Projections.property("siswa.id")).createAlias("siswa", "siswa")
									.add(Restrictions.isNotNull("siswa"))
									.addOrder(Order.asc("siswa.nomorIndukNasional"))
									.add(Restrictions.eq("kelasSiswa", kelasSiswa))
									.add(Restrictions.eq("siswa.aktif", true)).addOrder(Order.asc("siswa.nama")),
									Siswa.class, false);

							for (Date date : dates.values()) {

								for (Siswa siswa : siswas) {

									label.setValue("Sedang memproses data " + siswa.toString());

									AbsenPiket absenPiket = myItems.get(Common.dateFormat1.get().format(date));

									if (jamKes.isEmpty()) {
										Integer jamke = 0;

										AbsenPiketDetail absenPiketDetail = absenPiket == null ? null
												: AbsenPiketDetail.ambil(null, siswa, absenPiket,
														absenPiket.getKelas().getAbsensi(), jamke);

										Statusabsensi statusabsensi = (Statusabsensi) (absenPiketDetail == null
												? ConstantValues.BELUM_ABSEN
												: ConstantValues.ambil(Statusabsensi.class.getName(), absenPiketDetail
														.retreiveAbsensiId(siswa.getId() + "_" + absenPiket.getId())));
										if (statusabsensi == null) {
											statusabsensi = ConstantValues.BELUM_ABSEN;
										}

										String ket = absenPiketDetail.retreiveAbsensiKeterangan(siswa.getId() + "");
										String mulai = absenPiketDetail.retreiveAbsensiMulai(siswa.getId() + "");
										String sampai = absenPiketDetail.retreiveAbsensiSampai(siswa.getId() + "");

										Map map = new java.util.HashMap();

										map.put("apakah_dosen", kelasSiswa.getNama());
										map.put("nama_satuan_kerja", "Siswa");
										map.put("pegawai", siswa.getId());
										map.put("nama", siswa.getNama());
										map.put("nip", siswa.getNomorIndukNasional());
										map.put("keterangan", ket);
										map.put("masuk", mulai);
										map.put("statusabsensi", statusabsensi.getNama());
										map.put("pulang", sampai);
										map.put("jamke", jamke);
										map.put("hari", Common.dateFormat6.get().format(date));
										map.put("tanggal", Common.dateFormat1.get().format(date));

										maps.add(map);
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

											String ket = absenPiketDetail.retreiveAbsensiKeterangan(siswa.getId() + "");
											String mulai = absenPiketDetail.retreiveAbsensiMulai(siswa.getId() + "");
											String sampai = absenPiketDetail.retreiveAbsensiSampai(siswa.getId() + "");

											Map map = new java.util.HashMap();

											map.put("apakah_dosen", kelasSiswa.getNama());
											map.put("nama_satuan_kerja", "Siswa");
											map.put("pegawai", siswa.getId());
											map.put("nama", siswa.getNama());
											map.put("nip", siswa.getNomorIndukNasional());
											map.put("keterangan", ket);
											map.put("masuk", mulai);
											map.put("statusabsensi", statusabsensi.getNama());
											map.put("pulang", sampai);
											map.put("jamke", jamke);
											map.put("hari", Common.dateFormat6.get().format(date));
											map.put("tanggal", Common.dateFormat1.get().format(date));

											maps.add(map);
										}

									}
								}

							}

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Absen Piket Harian", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
									new String[] {
										"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
										"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
										"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
									});
						}

					}

					} finally {
						// Tutup sesi dedicated laporan latar (openSession) agar tidak menggantung.
						if (session != null && session.isOpen()) {
							try { session.clear(); } catch (Exception eClose) { ais.common.ErrorAuditUtil.record(eClose, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRekapAbsenPiketHarian.java:467");}
							try { session.disconnect(); } catch (Exception eClose) { ais.common.ErrorAuditUtil.record(eClose, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRekapAbsenPiketHarian.java:468");}
							try { session.close(); } catch (Exception eClose) { ais.common.ErrorAuditUtil.record(eClose, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanRekapAbsenPiketHarian.java:469");}
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

						Map parameters = generateParameter();
						parameters.put("maps", maps);
						File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
								"Laporan_Absensi_Per_Hari_Siswa", ais.ui.util.WaktuUtil.getDate(), null, toolbar);
						CommonReport.tampilkanReportPDF(center, file);

						ais.action.report.helper.LoadingReportUtil.stopAndDetach(timer);
					}

				}
			});
			timer.start();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekap Absen Piket Harian", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
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
}
