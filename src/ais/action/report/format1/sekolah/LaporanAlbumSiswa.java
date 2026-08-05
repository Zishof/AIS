package ais.action.report.format1.sekolah;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.util.ParallelTaskExecutor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanAlbumSiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Combobox yayasan;
	private Combobox sekolah;
	private Combobox kelas;

	private Center center;

	private Toolbar toolbar;

	private MyDatebox tanggal;

	@SuppressWarnings("rawtypes")
	private ArrayList<Map> maps;

	private KelasSiswa kelasSiswa = null;

	private Combobox tahunAjaran;

	private Desktop desktop;

	public LaporanAlbumSiswa() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Album Siswa", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAlbumSiswa(KelasSiswa kelasSiswa) {
		super();
		try {
			this.kelasSiswa = kelasSiswa;
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Album Siswa", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAlbumSiswa(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		Common.initYayasanDanSekolahDanSemua(yayasan = new Combobox(), sekolah = new Combobox(), null, null);

	}

	private void init() throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		if (kelasSiswa == null) {
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
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
			row.appendChild(yayasan);
			yayasan.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
			row.appendChild(sekolah);
			sekolah.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
			Common.selectComboItem(true, tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
					Common.getCurrentTahunAkademik());
			row.appendChild(tahunAjaran);
			tahunAjaran.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kelas *"));
			row.appendChild(kelas = new Combobox());
			kelas.setWidth("90%");

			EventListener kelasEvent = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null
							: sekolah.getSelectedItem().getValue());
					System.out.println("s => " + s);

					String ta = (String) tahunAjaran.getSelectedItem().getValue();
					Common.insertCombo(kelas, new String[] { "nama", "tahunAjaran", "ruang" }, "keterangan",
							KelasSiswa.class,
							Restrictions.and(Restrictions.eq("tahunAjaran", ta), (Restrictions.and(
									Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))));
				}
			};

			sekolah.addEventListener("onChange", kelasEvent);
			tahunAjaran.addEventListener("onChange", kelasEvent);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
			tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
			row.appendChild(tanggal);
			tanggal.setWidth("90%");

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

			Common.createDefaultTimer(kelasEvent);
		}

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				if (kelasSiswa == null) {
					if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
						MyMessageboxConfig.show("Pilih " + "Yayasan", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return null;
					}

					if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
						MyMessageboxConfig.show("Pilih " + Common.getBahasaConfig("Sekolah"), "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return null;
					}

					if (kelas.getSelectedItem() == null || kelas.getSelectedItem().getValue() == null) {
						MyMessageboxConfig.show("Pilih " + Common.getBahasaConfig("Kelas"), "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return null;
					}
				}
				Map parameters = generateParameter();
				return parameters;
			}
		}, "Album_Siswa", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		if (kelasSiswa != null) {
			onKHS(null);
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		if (kelasSiswa == null) {
			if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Pilih Yayasan", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return null;
			}

			if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Pilih Sekolah", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return null;
			}

			if (kelas.getSelectedItem() == null || kelas.getSelectedItem().getValue() == null) {
				return null;
			}

		}

		Map parameters = ais.common.HashMapGenerator.getRand();

		if (kelasSiswa != null) {
			Common.insertProperty(KelasSiswa.class, kelasSiswa, parameters, "kelasSiswa");
		} else {
			KelasSiswa kls = (KelasSiswa) kelas.getSelectedItem().getValue();
			Common.insertProperty(KelasSiswa.class, kls, parameters, "kelasSiswa");
		}

		if (maps != null) {
			parameters.put("maps", maps);
		}

		parameters.put("tanggal",
				tanggal == null || tanggal.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : tanggal.getValue());
		parameters.put("kaprodi", "(                                          )");
		parameters.put("nip", "");

		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generateDataDanImageAlbum(final Label label) throws Exception {

		final KelasSiswa kls;
		Session session = null;
		List<Siswa> siswasAsli = new ArrayList<Siswa>();

		// 1. MANAJEMEN SESSION UTAMA & PENGAMBILAN DATA
		try {
			session = ais.action.report.Report.openNativeSession();

			if (kelasSiswa == null) {
				if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
					return;
				}
				if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
					return;
				}
				if (kelas.getSelectedItem() == null || kelas.getSelectedItem().getValue() == null) {
					return;
				}

				kls = (KelasSiswa) kelas.getSelectedItem().getValue();

				siswasAsli = ConstantValues.simpleList(session.createCriteria(KelasSiswaPunyaSiswa.class)
						.createAlias("siswa", "siswa").setProjection(Projections.property("siswa.id"))
						.add(Restrictions.eq("siswa.aktif", true))
						.add(CommonSearchFilterHelper.eqSelectedWithId("siswa.sekolah", sekolah, false))
						.add(CommonSearchFilterHelper.eqSelectedWithId("siswa.yayasan", yayasan, false))
						.add(Restrictions.eq("kelasSiswa", kls)).addOrder(Order.asc("siswa.nomorIndukNasional")),
						Siswa.class, false);
			} else {
				kls = kelasSiswa;
				siswasAsli = ConstantValues.simpleList(session.createCriteria(KelasSiswaPunyaSiswa.class)
						.createAlias("siswa", "siswa").setProjection(Projections.property("siswa.id"))
						.add(Restrictions.eq("siswa.aktif", true)).add(Restrictions.eq("kelasSiswa", kls))
						.addOrder(Order.asc("siswa.nomorIndukNasional")), Siswa.class, false);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sekolah/LaporanAlbumSiswa.java:332");
			throw e;
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanAlbumSiswa.java:338");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Album Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
			ais.action.report.Report.closeCurrentSessionQuietly();
		}

		final int size = siswasAsli.size();
		if (size == 0) {
			this.maps = new ArrayList<Map>();
			if (label != null)
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			return;
		}

		// 2. EKSTRAKSI ID SISWA (Untuk keamanan Lazy Loading Lintas Thread) & ORDERING
		// ARRAY
		final List<Long> listIdSiswa = new ArrayList<Long>();
		for (Siswa s : siswasAsli) {
			if (s != null && s.getId() != null) {
				listIdSiswa.add(s.getId());
			}
		}

		final int actualSize = listIdSiswa.size();
		final Map[] orderedMaps = new Map[actualSize];
		List<Integer> listIndex = new ArrayList<Integer>();
		for (int i = 0; i < actualSize; i++) {
			listIndex.add(i);
		}

		final java.util.concurrent.atomic.AtomicInteger progressCounter = new java.util.concurrent.atomic.AtomicInteger(
				0);

		// 3. EKSEKUSI PARALEL (Max 100 Thread)
		ParallelTaskExecutor.process(listIndex, ParallelTaskExecutor.getDefaultReportMaxThreads(), new ParallelTaskExecutor.Task<Integer>() {
			@Override
			public void execute(final Integer idx) throws Exception {

				Long idSiswa = listIdSiswa.get(idx);
				Session threadSession = null;

				try {
					// Buka Sesi Lokal Khusus untuk Thread Ini (Mencegah
					// ConcurrentModificationException)
					threadSession = HibernateUtil.currentSession();
					Siswa siswa = (Siswa) threadSession.get(Siswa.class, idSiswa);
					if (siswa == null)
						return;

					// UI Update (Aman dari Freeze)
					int currentCount = progressCounter.incrementAndGet();
					if (label != null && desktop != null) {
						if (currentCount % 5 == 0 || currentCount == actualSize) {
							try {
								org.zkoss.zk.ui.Executions.activate(desktop);
								try {
									String progressStr;
									progressStr = Common.numberFormat.get().format((currentCount * 100.0) / actualSize);

									label.setValue("Memproses data " + siswa.getNama() + " (" + progressStr + "%)");
								} finally {
									org.zkoss.zk.ui.Executions.deactivate(desktop);
								}
							} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanAlbumSiswa.java:401");
							}
						}
					}

					Map map = new java.util.HashMap();

					Common.insertProperty(Siswa.class, siswa, map, "ssw");

					map.put("telp", siswa.getTeleponSiswa());
					map.put("alamat", siswa.getAlamatSiswa());
					map.put("nim", siswa.getNim());
					map.put("nama", siswa.getNama());
					map.put("sekolah", siswa.getSekolah() == null ? "" : siswa.getSekolah().getNama());
					map.put("yayasan", siswa.getSekolah() == null || siswa.getSekolah().getYayasan() == null ? ""
							: siswa.getSekolah().getYayasan().getNama());
					map.put("tahunAngkatan", siswa.getTahunLulus() + "");
					map.put("kelas", kls.getNama());

					siswa.putPhoto(map);

					// MENGAMANKAN URUTAN LAPORAN (ORDERING ARRAY)
					orderedMaps[idx] = map;

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Album Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				} finally {
					// Pastikan Sesi Lokal selalu ditutup bersih untuk mencegah kebocoran koneksi DB
					if (threadSession != null && threadSession.isOpen()) {
						try {
							threadSession.close();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanAlbumSiswa.java:432");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Album Siswa", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
						}
					}
				}
			}
		});

		// 4. MENGGABUNGKAN HASIL AKHIR DENGAN URUTAN YANG BENAR
		this.maps = new ArrayList<Map>();
		for (Map m : orderedMaps) {
			if (m != null) {
				this.maps.add(m);
			}
		}

		if (label != null) {
			ais.action.report.helper.LoadingReportUtil.selesai(label);
		}
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {
		if (kelasSiswa == null) {
			if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Pilih " + "Yayasan", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}

			if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Pilih " + Common.getBahasaConfig("Sekolah"), "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}
		}
		generateParameter();

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Album_Siswa",
						ais.ui.util.WaktuUtil.getDate(), null, toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

		desktop = org.zkoss.zk.ui.Executions.getCurrent().getDesktop();
		if (desktop != null && !desktop.isServerPushEnabled()) {
			desktop.enableServerPush(true);
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					generateDataDanImageAlbum(label);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sekolah/LaporanAlbumSiswa.java:492");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Album Siswa", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
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
