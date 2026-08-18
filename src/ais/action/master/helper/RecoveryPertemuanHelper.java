package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.AuditCriterion;

public class RecoveryPertemuanHelper extends MyWindow {

	private static final long serialVersionUID = 1L;

	private VOPembelajaran voPembelajaran;
	private MyGrid grid;
	private Datebox tglMulai;
	private Datebox tglSelesai;
	private Label labelLoading;
	private EventListener onSuccessListener;

	public static MyToolbarbuttonConfig button(final VOPembelajaran voPembelajaran, final EventListener eventListener) {
		Tbmuser tbmuser = Common.getCurrentUser();

		ais.ui.util.MyToolbarbuttonConfig buttonRecovery = new ais.ui.util.MyToolbarbuttonConfig("Recovery Data",
				"/img/Configure.png");
		buttonRecovery.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null);
		buttonRecovery.setTooltiptext("Pulihkan data pertemuan yang terhapus atau hilang dari histori");
		buttonRecovery.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK,
				new org.zkoss.zk.ui.event.EventListener() {

					@Override
					public void onEvent(org.zkoss.zk.ui.event.Event event) throws Exception {

						// Pastikan parent (VOPembelajaran) tidak null sebelum membuka recovery
						if (voPembelajaran == null || voPembelajaran.getId() == null) {
							ais.ui.util.MyMessageboxConfig.show(
									"Silakan pilih data pembelajaran (Perkuliahan/Jadwal) terlebih dahulu.",
									"Peringatan", ais.ui.util.MyMessageboxConfig.OK,
									ais.ui.util.MyMessageboxConfig.EXCLAMATION);
							return;
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								// Panggil Window RecoveryPertemuanHelper
								ais.action.master.helper.RecoveryPertemuanHelper recoveryWindow = new ais.action.master.helper.RecoveryPertemuanHelper(
										voPembelajaran, eventListener);

								// Tempelkan window pop-up ke root window saat ini
								recoveryWindow
										.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()); // Ganti
																														// 'window'
																														// dengan
																														// ID
																														// komponen
																														// root
																														// Anda
																														// jika
																														// berbeda

								// Tampilkan secara melayang (Modal Pop-up) agar fokus pengguna hanya ke form
								// recovery
								recoveryWindow.doModal();

							}
						});
					}
				});

		return buttonRecovery;
	}

	// Menampung data terbaru yang disaring berdasarkan rentang tanggal
	private List<Pertemuan> dataTerbaruToRecover = new ArrayList<Pertemuan>();

	/**
	 * Constructor
	 * 
	 * @param voPembelajaran    Parameter induk (Bisa Perkuliahan, JadwalPelajaran,
	 *                          Skripsi, dll)
	 * @param onSuccessListener Event yang akan dipanggil ketika recovery selesai
	 *                          (untuk refresh tabel utama)
	 */
	public RecoveryPertemuanHelper(VOPembelajaran voPembelajaran, EventListener onSuccessListener) throws Exception {
		super();
		this.voPembelajaran = voPembelajaran;
		this.onSuccessListener = onSuccessListener;
		initUI();
	}

	/**
	 * Mendapatkan nama field relasi di tabel Pertemuan secara dinamis berdasarkan
	 * class turunan dari VOPembelajaran.
	 */
	private String getPropertyRelasiName(VOPembelajaran vo) {
		// PENTING: Gunakan Hibernate.getClass() untuk mengatasi objek Proxy / Lazy
		// Loading
		// Jika menggunakan vo.getClass(), bisa menghasilkan nama aneh seperti
		// "Perkuliahan_$$_javassist_1"
		Class<?> clazz = org.hibernate.Hibernate.getClass(vo);
		String className = clazz.getSimpleName();

		// Mapping Eksplisit sesuai dengan daftar relasi
		if (className.equals("Perkuliahan"))
			return "perkuliahan";
		if (className.equals("JadwalUjianPMB"))
			return "jadwalUjianPMB";
		if (className.equals("MahasiswaRequestTugasAkhir"))
			return "mahasiswaRequestTugasAkhir";
		if (className.equals("KelompokKkn"))
			return "kelompokKkn";
		if (className.equals("KelompokPkl"))
			return "kelompokPkl";
		if (className.equals("Skripsi"))
			return "skripsi"; // Diubah dari "bimbingan" menjadi "skripsi"
		if (className.equals("KrsMahasiswa"))
			return "krsMahasiswa";
		if (className.equals("JadwalUjianPSB"))
			return "jadwalUjianPSB";
		if (className.equals("JadwalPertemuanPSB"))
			return "jadwalPertemuanPSB";
		if (className.equals("JadwalUjianPegawai"))
			return "jadwalUjianPegawai";
		if (className.equals("JadwalPelajaran"))
			return "jadwalPelajaran";
		if (className.equals("PertemuanPunyaGrupPertemuan"))
			return "pertemuanPunyaGrupPertemuan";
		if (className.equals("FormulirKegiatan"))
			return "formulirKegiatan";
		if (className.equals("KomponenDataProdukKursus"))
			return "komponenDataProdukKursus";
		if (className.equals("KelasLesSiswa"))
			return "kelasLesSiswa";

		// Fallback Cerdas untuk class baru di masa depan:
		// Huruf pertama dikecilkan (Contoh: "NamaClassBaru" -> "namaClassBaru")
		return className.substring(0, 1).toLowerCase() + className.substring(1);
	}

	@SuppressWarnings("deprecation")
	private void initUI() throws Exception {
		setTitle("Recovery Data Pertemuan - " + voPembelajaran.getClass().getSimpleName());
		setWidth("95%");
		setHeight("90%");
		setClosable(true);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		// ================= NORTH (Filter Pencarian) =================
		North north = new North();
		north.setParent(borderlayout);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setParent(north);
		searchgrid.setWidth("100%");

		Columns searchCols = new Columns();
		searchCols.setParent(searchgrid);
		new MyColumnConfig().setParent(searchCols);
		new MyColumnConfig().setParent(searchCols);
		new MyColumnConfig().setParent(searchCols);
		new MyColumnConfig().setParent(searchCols);

		Rows searchRows = new Rows();
		searchRows.setParent(searchgrid);
		Row r1 = new Row();
		r1.setParent(searchRows);

		// Menghitung default 5 Tahun ke belakang untuk tglMulai
		Calendar cal5TahunLalu = Calendar.getInstance();
		cal5TahunLalu.setTime(ais.ui.util.WaktuUtil.getDate());
		cal5TahunLalu.add(Calendar.YEAR, -5);

		r1.appendChild(new MyLabelConfig("Tanggal Perubahan Mulai :"));
		tglMulai = new Datebox(cal5TahunLalu.getTime());
		tglMulai.setFormat("dd-MM-yyyy");
		r1.appendChild(tglMulai);

		r1.appendChild(new MyLabelConfig("Tanggal Perubahan Selesai :"));
		tglSelesai = new Datebox(ais.ui.util.WaktuUtil.getDate());
		tglSelesai.setFormat("dd-MM-yyyy");
		r1.appendChild(tglSelesai);

		Row r2 = new Row();
		r2.setParent(searchRows);
		ais.ui.util.ZkCompat.setSpans(r2, "4");
		Toolbar toolNorth = new Toolbar();
		toolNorth.setParent(r2);

		MyToolbarbuttonConfig btnCari = new MyToolbarbuttonConfig("Tampilkan Data Audit", "/img/svg/search.svg");
		btnCari.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDataAudit();
			}
		});
		btnCari.setParent(toolNorth);

		// ================= CENTER (Grid Data Audit) =================
		Center center = new Center();
		center.setParent(borderlayout);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);

		// Kolom Sesuai Instruksi
		createColumn(columns, "ID", "50px");
		createColumn(columns, "Ke", "40px");
		createColumn(columns, "Tanggal", "100px");
		createColumn(columns, "Topik", "150px");
		createColumn(columns, "Catatan", "150px");
		createColumn(columns, "Indikator", "150px");
		createColumn(columns, "Waktu Pembelajaran", "120px");
		createColumn(columns, "Pengalaman Belajar", "150px");
		createColumn(columns, "Metode Pembelajaran", "150px");
		createColumn(columns, "Mulai", "60px");
		createColumn(columns, "Selesai", "60px");
		createColumn(columns, "Ruang", "100px");

		// ================= SOUTH (Tombol Recovery) =================
		South south = new South();
		south.setParent(borderlayout);

		Toolbar toolSouth = new Toolbar();
		toolSouth.setParent(south);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null) {

			MyToolbarbuttonConfig btnRecovery = new MyToolbarbuttonConfig("Recovery Data", "/img/Configure.png");
			btnRecovery.addEventListener(Events.ON_CLICK, getRecoveryEventListener());
			btnRecovery.setParent(toolSouth);
		}

		MyToolbarbuttonConfig btnTutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		btnTutup.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				detach();
			}
		});
		btnTutup.setParent(toolSouth);

		// Status Loading Bar
		labelLoading = new Label("");
		labelLoading.setStyle("font-weight:bold; color:blue; margin-left:20px;");
		labelLoading.setParent(toolSouth);

		// Load data otomatis saat form dibuka
		onSearchDataAudit();
	}

	private void createColumn(Columns parent, String label, String width) {
		MyColumnConfig col = new MyColumnConfig();
		col.setLabel(label);
		col.setWidth(width);
		col.setParent(parent);
	}

	@SuppressWarnings("rawtypes")
	private void onSearchDataAudit() {
		Session session = null;
		try {
			if (tglMulai.getValue() == null || tglSelesai.getValue() == null) {
				MyMessageboxConfig.show("Rentang tanggal tidak boleh kosong.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return;
			}

			// Adjust Tanggal Selesai ke jam 23:59:59
			Calendar cal = Calendar.getInstance();
			cal.setTime(tglSelesai.getValue());
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			Date dateEnd = cal.getTime();

			session = HibernateUtil.currentSession();
			AuditReader reader = AuditReaderFactory.get(session);

			String propertyRelasi = getPropertyRelasiName(voPembelajaran);

			// Ambil History Pertemuan (false, true = return Object[] {Entity, RevEntity,
			// RevType})
			AuditQuery query = reader.createQuery().forRevisionsOfEntity(Pertemuan.class, false, true);

			// Filter berdasarkan Rentang Tanggal Revisi
			query.add(AuditEntity.revisionProperty("timestamp").between(tglMulai.getValue().getTime(),
					dateEnd.getTime()));

			// Filter relasi induk (Perkuliahan / JadwalPelajaran / dll)
			query.add(AuditEntity.property(propertyRelasi).eq(voPembelajaran));

			// Urutkan DESC agar data yang paling awal dibaca adalah data paling UPDATE di
			// rentang tersebut
			query.addOrder(AuditEntity.revisionNumber().desc());

			List results = query.getResultList();

			dataTerbaruToRecover.clear();

			// Menggunakan String sebagai Key untuk menampung gabungan ID dan PertemuanKe
			Map<Integer, Pertemuan> mapFilterTerbaru = new HashMap<Integer, Pertemuan>();

			// Ekstrak data (Hanya ambil revisi paling atas/terbaru dari masing-masing
			// kombinasi ID dan PertemuanKe)
			for (Object obj : results) {
				Object[] tuple = (Object[]) obj;
				Pertemuan p = (Pertemuan) tuple[0];

				// ==========================================================
				// DYNAMIC GROUP BY: Menggabungkan ID dan Pertemuan Ke
				// Contoh Output Key: "15542_1", "15542_2", dst.
				// ==========================================================
				Integer groupKey = p.getPertemuanKe();

				if (!mapFilterTerbaru.containsKey(groupKey)) {
					mapFilterTerbaru.put(groupKey, p);
					dataTerbaruToRecover.add(p);
				}
			}

			// Render ke Grid
			ListModel model = new SimpleListModel(dataTerbaruToRecover);
			grid.setRowRenderer(new DataRenderer());
			grid.setModelCheckMobile(model);

			if (dataTerbaruToRecover.isEmpty()) {
				MyMessageboxConfig.show("Tidak ada histori perubahan pada rentang tanggal tersebut.", "Info",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"mencari histori perubahan pertemuan untuk proses recovery",
					e, new String[] {
							"Periksa kembali rentang tanggal yang dipilih.",
							"Muat ulang (refresh) halaman ini lalu coba pencarian kembali.",
							"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	class DataRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(Row row, Object data) throws Exception {
			row.setValign("top");
			Pertemuan p = (Pertemuan) data;

			new Label(p.getId() + "").setParent(row);
			new Label(p.getPertemuanKe() + "").setParent(row);

			// Format Tanggal Aman (Tanggal tetap menggunakan object Date)
			String strTanggal = "";
			if (p.getTanggal() != null) {
				try {
					strTanggal = Common.dateFormat5.get().format((Date) p.getTanggal());
				} catch (Exception e) {
					strTanggal = p.getTanggal().toString(); // Fallback jika tiba-tiba format gagal
				}
			}
			new Label(strTanggal).setParent(row);

			new Label(p.getTopik() != null ? p.getTopik() : "").setParent(row);
			new Label(p.getCatatan() != null ? p.getCatatan() : "").setParent(row);
			new Label(p.getIndikator() != null ? p.getIndikator() : "").setParent(row);
			new Label(p.getWaktupembelajaran() != null ? p.getWaktupembelajaran() : "").setParent(row);
			new Label(p.getPengalamanBelajar() != null ? p.getPengalamanBelajar() : "").setParent(row);
			new Label(p.getMetodePembelajaran() != null ? p.getMetodePembelajaran() : "").setParent(row);

			// Waktu Mulai & Selesai langsung dicetak karena sudah bertipe String
			new Label(p.getWaktuMulai() != null ? p.getWaktuMulai() : "").setParent(row);
			new Label(p.getWaktuSelesai() != null ? p.getWaktuSelesai() : "").setParent(row);

			// Proteksi kolom Ruang (Mengantisipasi proxy putus akibat master Ruang sudah
			// dihapus)
			String nmRuang = "";
			try {
				nmRuang = p.getRuang() != null ? p.getRuang().getNama() : "";
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RecoveryPertemuanHelper.java:429");
				// Abaikan error Hibernate Proxy
			}
			new Label(nmRuang).setParent(row);
		}
	}

	// =========================================================================
	// ENGINE RECOVERY LATAR BELAKANG (THREADED & SERVER PUSH)
	// =========================================================================
	private EventListener getRecoveryEventListener() {
		return new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (dataTerbaruToRecover.isEmpty()) {
					MyMessageboxConfig.show("Tidak ada data untuk di-recover. Silakan cari terlebih dahulu.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show("Apakah Anda yakin ingin melakukan Recovery sebanyak "
						+ dataTerbaruToRecover.size() + " data dari tabel audit?\n"
						+ "Tindakan ini akan memulihkan / menimpa data utama dengan data di histori ini secara utuh.",
						"Konfirmasi Recovery", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int response = Integer.parseInt(event.getData().toString());
								if (response == MyMessageboxConfig.OK) {

									final List<String> warnings = java.util.Collections
											.synchronizedList(new ArrayList<String>());
									final Desktop desktop = Executions.getCurrent().getDesktop();

									// Enable Server Push (Wajib untuk modifikasi UI dari Thread)
									if (!desktop.isServerPushEnabled()) {
										desktop.enableServerPush(true);
									}

									// Kunci tombol agar tidak di-klik ganda
									labelLoading.setValue("Memulai proses...");

									new Thread(new Runnable() {
										@Override
										public void run() {
											Session session = null;
											Transaction tx = null;
											try {
												session = HibernateUtil.currentNativeSession();
												tx = session.beginTransaction();
												AuditReader reader = AuditReaderFactory.get(session);

												int total = dataTerbaruToRecover.size();
												int count = 0;

												Set<String> processedIds = new HashSet<String>();

												for (Pertemuan p : dataTerbaruToRecover) {
													// 1. Eksekusi Deep Dependencies Restore (Jika ada relasi yang
													// hilang)
													restoreDependenciesRecursively(session, reader, p, processedIds);

													// 2. Replicate / Force Save data Pertemuan ke Tabel Utama
													session.replicate(p, ReplicationMode.OVERWRITE);

													count++;
													final int percent = (count * 100) / total;
													final String msg = "Menyimpan Pertemuan ke-" + p.getPertemuanKe();

													// Update Loading Bar UI
													Executions.schedule(desktop, new EventListener() {
														@Override
														public void onEvent(Event ev) throws Exception {
															if (labelLoading != null) {
																labelLoading.setValue(
																		"Loading... " + percent + "% (" + msg + ")");
															}
														}
													}, null);
												}

												tx.commit();

											} catch (Exception e) {
												if (tx != null && tx.isActive())
													tx.rollback();
												warnings.add("Gagal memulihkan data: " + e.getMessage());
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RecoveryPertemuanHelper.java:517");
											} finally {
												if (session != null && session.isOpen()) {
													try {
														session.disconnect();
														session.close();
													} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/RecoveryPertemuanHelper.java:523");
													}
												}

												// Proses Finalisasi di UI Thread
												try {
													Executions.schedule(desktop, new EventListener() {
														@Override
														public void onEvent(Event event) throws Exception {
															labelLoading.setValue(""); // Sembunyikan Loading

															if (!warnings.isEmpty()) {
																StringBuilder sb = new StringBuilder();
																for (String w : warnings)
																	sb.append(w).append("\n");
																MyMessageboxConfig.show(sb.toString(), "Peringatan",
																		MyMessageboxConfig.OK,
																		MyMessageboxConfig.ERROR);
															} else {
																MyMessageboxConfig.show(
																		"Seluruh data pertemuan berhasil dipulihkan!",
																		"Sukses", MyMessageboxConfig.OK,
																		MyMessageboxConfig.INFORMATION);
																if (onSuccessListener != null) {
																	Common.createDefaultTimer(onSuccessListener);
																}
																detach(); // Tutup window setelah sukses
															}
														}
													}, null);
												} catch (Exception e) {
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RecoveryPertemuanHelper.java:554");
												}
											}
										}
									}).start();
								}
							}
						});
			}
		};
	}

	/**
	 * DEEP DEPENDENCY RESTORE Berjaga-jaga jika ada relasi di dalam object
	 * Pertemuan (seperti Ruang, Dosen, dll) yang terhapus secara fisik di master
	 * database.
	 */
	@SuppressWarnings("rawtypes")
	private void restoreDependenciesRecursively(Session session, AuditReader reader, Object entityObj,
			Set<String> processedIds) {
		if (entityObj == null)
			return;
		java.lang.reflect.Method[] methods = entityObj.getClass().getMethods();
		for (java.lang.reflect.Method method : methods) {
			if (method.getName().startsWith("get") && GeneralValueObject.class.isAssignableFrom(method.getReturnType())
					&& !method.getName().equals("getClass")) {
				try {
					GeneralValueObject relation = (GeneralValueObject) method.invoke(entityObj);
					if (relation != null && relation.getId() != null) {
						String key = relation.getClass().getName() + "-" + relation.getId();
						if (!processedIds.contains(key)) {
							processedIds.add(key);
							Class<?> realClass = org.hibernate.Hibernate.getClass(relation);
						Object checkDb = session.get(realClass, relation.getId());
							if (checkDb == null) {
								List results = reader.createQuery()
										.forRevisionsOfEntity(realClass, true, false)
										.add(AuditEntity.id().eq(relation.getId()))
										.addOrder(AuditEntity.revisionNumber().desc()).setMaxResults(1).getResultList();
								if (results != null && !results.isEmpty()) {
									Object auditRel = results.get(0);
									restoreDependenciesRecursively(session, reader, auditRel, processedIds);
									session.replicate(auditRel, ReplicationMode.OVERWRITE);
									session.flush();
								}
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RecoveryPertemuanHelper.java:601");
				}
			}
		}
	}
}
