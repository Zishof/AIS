package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.util.ParallelTaskExecutor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan album mahasiswa per prodi dan angkatan. Kelas ini
 * mengubah data domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa
 * memindahkan aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox fakultas}, {@code Combobox
 * jurusan}, {@code Combobox program}, {@code Intbox angkatan}, {@code Intbox angkatansd}, {@code Center center},
 * {@code Toolbar toolbar}, {@code MyDatebox tanggal}; inisialisasi/lifecycle ({@code initKHS()}, {@code
 * init()}); operasi domain lain ({@code generateParameter()}, {@code generateDataDanImageAlbum()}, {@code
 * onKHS()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanAlbumMahasiswaPerProdiDanAngkatan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox program;

//	private Combobox semester;
//	private Combobox tahunAkademik;
	private Intbox angkatan;
	private Intbox angkatansd;

	private Center center;

	private Toolbar toolbar;

//	private Label myTahunAngkatan;

	private MyDatebox tanggal;

	@SuppressWarnings("rawtypes")
	private ArrayList<Map> maps;

	private Perkuliahan perkuliahan = null;

	private Desktop desktop;

	public LaporanAlbumMahasiswaPerProdiDanAngkatan() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Album Mahasiswa Per Prodi Dan Angkatan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAlbumMahasiswaPerProdiDanAngkatan(Perkuliahan perkuliahan) {
		super();
		try {
			this.perkuliahan = perkuliahan;
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Album Mahasiswa Per Prodi Dan Angkatan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAlbumMahasiswaPerProdiDanAngkatan(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

	}

	private void init() throws Exception {

		program = Common.initPrograms(null);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		if (perkuliahan == null) {
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
			row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas *"));
			row.appendChild(fakultas);
			fakultas.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Prodi *"));
			row.appendChild(jurusan);
			jurusan.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
			row.appendChild(program);
			program.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
			Hbox hbox = new Hbox();

			angkatan = new Intbox(Calendar.getInstance().get(Calendar.YEAR) - 1);
			angkatan.setCols(2);

			angkatansd = new Intbox(Calendar.getInstance().get(Calendar.YEAR));
			angkatansd.setCols(2);

			hbox.appendChild(angkatan);
			hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
			hbox.appendChild(angkatansd);

			row.appendChild(hbox);

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
				if (perkuliahan == null) {
					if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
						MyMessageboxConfig.show("Mohon maaf, Fakultas belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Fakultas dari daftar dropdown; (2) Pastikan data Fakultas tersedia di sistem; (3) Ulangi proses cetak laporan album. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return null;
					}

					if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
						MyMessageboxConfig.show("Pilih " + Common.getBahasaConfig("Jurusan"), "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return null;
					}
				}
				Map parameters = generateParameter();
				return parameters;
			}
		}, "Album_Mahasiswa", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		if (perkuliahan != null) {
			onKHS(null);
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		if (perkuliahan == null) {
			if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Mohon maaf, Fakultas belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Fakultas dari daftar dropdown; (2) Pastikan data Fakultas tersedia di sistem; (3) Ulangi proses cetak laporan album. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return null;
			}

			if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Mohon maaf, Program Studi belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Program Studi dari dropdown setelah memilih Fakultas; (2) Pastikan data Prodi tersedia di sistem; (3) Ulangi proses cetak laporan album. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return null;
			}
		}

		Map parameters = ais.common.HashMapGenerator.getRand();

		if (perkuliahan != null) {
			Common.insertProperty(Perkuliahan.class, perkuliahan, parameters, "perkuliahan");
		}

		if (maps != null) {
			parameters.put("maps", maps);
		}
		parameters.put("program",
				program == null || program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
						? "-1"
						: program.getSelectedItem().getValue());

		parameters.put("tanggal",
				tanggal == null || tanggal.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : tanggal.getValue());
		parameters.put("kaprodi", "(                                          )");
		parameters.put("nip", "");

		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generateDataDanImageAlbum(final Label label) throws Exception {

		final String myprogram = (String) (program.getSelectedItem() == null
				|| program.getSelectedItem().getValue() == null ? "" : program.getSelectedItem().getValue());

		Session session = null;
		List<Mahasiswa> mahasiswasAsli = new ArrayList<Mahasiswa>();

		// 1. MANAJEMEN SESSION UTAMA & PENGAMBILAN DATA MAHASISWA
		try {
			session = ais.action.report.Report.openNativeSession();

			if (perkuliahan == null) {
				if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
					return;
				}
				if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
					return;
				}

				Integer a1 = angkatan.getValue() == null ? 0 : angkatan.getValue();
				Integer a2 = angkatansd.getValue() == null ? 4000 : angkatansd.getValue();

				mahasiswasAsli = ConstantValues
						.simpleList(
								session.createCriteria(Mahasiswa.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))
										.add(Restrictions.between("tahunangkatan", a1, a2))
										.add(myprogram.equals("") ? Restrictions.sqlRestriction("1=1")
												: Restrictions.eq("program", myprogram))
										.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim")),
								Mahasiswa.class);
			} else {
				mahasiswasAsli = ConstantValues.simpleList(
						session.createCriteria(Detailperkuliahan.class).createAlias("mahasiswa", "mahasiswa")
								.add(Restrictions.eq("mahasiswa.aktif", true))
								.setProjection(Projections.property("mahasiswa.id"))
								.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
								.add(Restrictions.eq("perkuliahan", perkuliahan))
								.addOrder(Order.desc("mahasiswa.tahunangkatan")).addOrder(Order.asc("mahasiswa.nim")),
						Mahasiswa.class, false);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanAlbumMahasiswaPerProdiDanAngkatan.java:329");
			throw e;
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAlbumMahasiswaPerProdiDanAngkatan.java:335");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Album Mahasiswa Per Prodi Dan Angkatan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
			ais.action.report.Report.closeCurrentSessionQuietly();
		}

		final int size = mahasiswasAsli.size();
		if (size == 0) {
			this.maps = new ArrayList<Map>();
			if (label != null)
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			return;
		}

		// 2. EKSTRAKSI ID (Untuk keamanan Lazy Load di Multi-Thread) & PENJAGAAN URUTAN
		final List<Long> listIdMahasiswa = new ArrayList<Long>();
		for (Mahasiswa m : mahasiswasAsli) {
			if (m != null && m.getId() != null) {
				listIdMahasiswa.add(m.getId());
			}
		}

		final int actualSize = listIdMahasiswa.size();
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

				Long idMhs = listIdMahasiswa.get(idx);
				Session threadSession = null;

				try {
					// Buka Session Lokal Khusus untuk Thread Ini
					threadSession = HibernateUtil.currentSession();
					Mahasiswa mahasiswa = (Mahasiswa) threadSession.get(Mahasiswa.class, idMhs);
					if (mahasiswa == null)
						return;

					// UI Update (Aman dari Thread Collision)
					int currentCount = progressCounter.incrementAndGet();
					if (label != null && desktop != null) {
						if (currentCount % 5 == 0 || currentCount == actualSize) {
							try {
								org.zkoss.zk.ui.Executions.activate(desktop);
								try {
									String progressStr;
									progressStr = Common.numberFormat.get().format((currentCount * 100.0) / actualSize);

									label.setValue("Memproses data " + mahasiswa.getNama() + " (" + progressStr + "%)");
								} finally {
									org.zkoss.zk.ui.Executions.deactivate(desktop);
								}
							} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAlbumMahasiswaPerProdiDanAngkatan.java:396");
							}
						}
					}

					Map map = new java.util.HashMap();
					map.put("telp", mahasiswa.getTelp());
					map.put("alamat", mahasiswa.getAlamat());
					map.put("nim", mahasiswa.getNim());
					map.put("nama", mahasiswa.getNama());
					map.put("jurusan", mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama());
					map.put("fakultas",
							mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
									: mahasiswa.getJurusan().getFakultas().getNama());
					map.put("tahunAngkatan", mahasiswa.getTahunangkatan() + "");

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);

					Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, map, "krs");
					Common.insertProperty(Mahasiswa.class, mahasiswa, map, "mhs");

					BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
					if (biodataMahasiswa != null) {
						Common.insertProperty(BiodataMahasiswa.class, biodataMahasiswa, map, "bio");
					}

					Double ipmhs = krsMahasiswa.getIps();
					Double ipkmhs = krsMahasiswa.getIpk();

					Integer sksmhss = krsMahasiswa.getSksYangDiambil();
					Integer sksmhs = krsMahasiswa.getSksk();

					if (krsMahasiswa.getSemester() > 1) {
						Double iplast = Common.ipTerakhir(mahasiswa, krsMahasiswa.getSemester());
						map.put("ip_sebelumnya", iplast);
					}

					map.put("ip", ipkmhs);
					map.put("ipk", ipkmhs);
					map.put("ips", ipmhs);
					map.put("sksk", sksmhs);
					map.put("sks", sksmhss);

					mahasiswa.putPhoto(map);

					// MENGAMANKAN URUTAN LAPORAN (ORDERING ARRAY)
					orderedMaps[idx] = map;

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Album Mahasiswa Per Prodi Dan Angkatan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				} finally {
					// Wajib menutup sesi lokal agar tidak terjadi kebocoran koneksi pool
					if (threadSession != null && threadSession.isOpen()) {
						try {
							threadSession.close();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAlbumMahasiswaPerProdiDanAngkatan.java:451");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Album Mahasiswa Per Prodi Dan Angkatan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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
		if (perkuliahan == null) {
			if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Pilih " + "Fakultas", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}

			if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Pilih " + Common.getBahasaConfig("Jurusan"), "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}
		}
		generateParameter();

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Album_Mahasiswa",
						ais.ui.util.WaktuUtil.getDate(), null, toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

		desktop = Executions.getCurrent().getDesktop();

		/* OPTIMASI FASE 5: server push dulu dinyalakan di sini tetapi TIDAK PERNAH dimatikan,
		 * sehingga browser terus polling (menahan thread Tomcat) selama tab terbuka walau
		 * laporan sudah selesai. Tugas juga dijalankan pada thread MENTAH tanpa batas.
		 * jalankanDenganPush() menyalakan push ber-reference-count, menjalankan tugas pada pool
		 * daemon berbatas milik AsyncTaskManager, lalu MELEPAS push di finally. */
		ais.common.AsyncTaskManager.jalankanDenganPush(desktop, new Runnable() {

			@Override
			public void run() {
				try {
					generateDataDanImageAlbum(label);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanAlbumMahasiswaPerProdiDanAngkatan.java:511");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Album Mahasiswa Per Prodi Dan Angkatan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		});

	}

}
