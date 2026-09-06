package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.RencanaTahunAkademikAction;
import ais.action.master.TampilanELearningAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.sekolah.helper.AmbilDataKelasLesSiswaBanbox;
import ais.action.master.sekolah.helper.AmbilDataKelasSiswaBanbox;
import ais.action.master.sekolah.helper.PenjadwalanSiswaHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.RencanaTahunAkademik;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.TugasKelompok;
import ais.database.model.Ujian;
import ais.database.model.VOPembelajaran;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.SmartDateTimeUtil;
import ais.ui.util.WaktuUtil;

/**
 * Helper widget "Rekapitulasi Ujian" — daftar {@link PertemuanPunyaUjian} (ujian yang
 * ditautkan ke satu {@link Pertemuan}) untuk satu pengguna atau satu
 * {@link VOPembelajaran} (perkuliahan/jadwal pelajaran) tertentu, sekaligus penyedia
 * wizard {@link #buatbaru} yang DIPAKAI BERSAMA oleh helper rekap lain
 * ({@code RekapitulasiTugasHelper}) untuk membuat entri baru (Ujian/Tugas/Tugas
 * Kelompok) yang ditautkan ke sebuah {@link Pertemuan}.
 *
 * <p>
 * Berbeda dari {@code RekapitulasiTugasHelper} yang memakai SQL native
 * {@code UNION ALL} untuk performa, {@link #reload(List, Paging, MyGrid, Mahasiswa, boolean, Date, Date, String, VOPembelajaran)}
 * di sini memakai Hibernate {@link Criteria} langsung atas {@link PertemuanPunyaUjian}
 * (join {@code ujian}), dengan filter tanggal disuntikkan sebagai literal SQL
 * ter-format (bukan parameter binding, tapi nilai berasal dari objek {@link Date}
 * ter-format lewat {@link Common#databaseDateFormat}, bukan input pengguna langsung).
 * Untuk mahasiswa/siswa/calon, baris grid menampilkan kontrol "boleh ikut ujian atau
 * tidak" lewat {@link PertemuanPunyaUjianHelper#tampilBolekIkutUjianAtauTidak}
 * (termasuk info kuota dan hasil ujian mereka sendiri); untuk staf, baris hanya
 * membuka detail pertemuan (tab Ujian, index 6) lewat {@link PertemuanHelper}.
 * </p>
 */
public class RekapitulasiUjianHelper {

	/**
	 * Membuat tombol "Buat &lt;nama&gt;" (mis. "Buat Ujian", "Buat Tugas", "Buat Tugas
	 * Kelompok" — dipakai lintas helper rekap) yang membuka jendela wizard: memilih
	 * {@link Pertemuan} target (langsung dari daftar pertemuan {@code perkuliahan} bila
	 * terisi, dengan tombol tambah pertemuan baru bila belum ada; atau memilih dulu
	 * Perkuliahan/Kelas Siswa/Kelas Les Siswa lalu Jadwal Pelajaran lalu Pertemuan bila
	 * {@code perkuliahan} {@code null}), lalu tombol "Lanjut" membuka
	 * {@link PertemuanHelper} pada tab {@code nomor} (untuk "Tugas Kelompok", memakai
	 * {@link TugasKelompokHelper#onAddExternal} sebagai gantinya). Tombol hanya
	 * terlihat untuk pengguna staf (bukan mahasiswa/siswa/calon).
	 *
	 * @param tbmuser     pengguna yang membuat entri baru
	 * @param perkuliahan konteks perkuliahan/jadwal pelajaran; {@code null} untuk memilih bebas lintas kelas
	 * @param dataLoader  callback pemuatan ulang data pemanggil setelah entri dibuat
	 * @param nomor       indeks tab {@link PertemuanHelper} yang dibuka setelah "Lanjut"
	 * @param nama        label jenis entri (mis. "Ujian", "Tugas", "Tugas Kelompok"), dipakai pada judul tombol/jendela dan menentukan jalur khusus "Tugas Kelompok"
	 * @return tombol toolbar siap pakai
	 */
	public static MyToolbarbuttonConfig buatbaru(final Tbmuser tbmuser, final VOPembelajaran perkuliahan,
			final DataLoader dataLoader, final int nomor, final String nama) {
		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Buat " + nama, "/img/svg/addthis.svg");
		refresh.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null);
		refresh.setTooltiptext("Refresh");
		refresh.addEventListener("onClick", new EventListener() {

			/**
			 * Pertemuan TERAKHIR yang dimasukkan ke combobox oleh {@code PertemuanEvent} — bukan
			 * pertemuan yang dipilih pengguna. Hanya dipakai untuk kasus daftar berisi tepat satu
			 * pertemuan, yaitu agar item tunggal itu dapat langsung diaktifkan lewat
			 * {@code Common.selectComboItem}. Direset ke {@code null} setiap pengisian ulang.
			 */
			private Pertemuan pdata;

			@Override
			public void onEvent(Event event) throws Exception {

				final MyWindow window = new MyWindow("Buat " + nama + " Baru", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("350px");
				window.setWidth("750px");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center a = new Center();
				a.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(a);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("30%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();

				rows.setParent(grid);

				final Combobox jadwal = new Combobox();
				final Combobox pertemuan = new Combobox();
				final MyFormRow rowTambah = new MyFormRow();

				/**
				 * Kelas lokal (method-local) pengisi ulang {@link Combobox} "Pilih Pertemuan" pada
				 * wizard {@link RekapitulasiUjianHelper#buatbaru}. Dipakai sebagai {@link EventListener}
				 * yang dipanggil ulang setiap kali konteks pembelajaran berubah (Perkuliahan/Jadwal
				 * Pelajaran baru dipilih, atau satu pertemuan baru selesai dibuat lewat
				 * {@link PenjadwalanHelper}/{@link PenjadwalanSiswaHelper}).
				 *
				 * <p>
				 * Perilaku pengisian: mengosongkan combobox, mengambil daftar {@link Pertemuan} dari
				 * {@code vo.ambilPertemuanList()}, lalu menyesuaikan mode pilih menurut jumlahnya —
				 * tepat satu pertemuan berarti langsung terpilih dan combobox dikunci; lebih dari satu
				 * memunculkan item pembuka "= Pilih Pertemuan ="; nol pertemuan memunculkan item
				 * "= Pertemuan / Agenda belum dibuat =" dan combobox dinonaktifkan. Baris tombol tambah
				 * pertemuan ({@code rowTambah}) hanya ditampilkan bila {@code vo} terisi.
				 * </p>
				 *
				 * <p>
				 * <b>Scope &amp; efek samping:</b> ini adalah kelas LOKAL di dalam {@code onEvent} tombol
				 * "Buat &lt;nama&gt;", sehingga menutup (capture) variabel {@code pertemuan},
				 * {@code rowTambah}, dan field {@code pdata} milik listener induk — instance-nya hanya
				 * valid selama jendela wizard itu hidup dan TIDAK boleh disimpan atau dibagikan lintas
				 * desktop/session. Operasinya murni baca-data + mutasi komponen ZK; tidak ada penulisan
				 * ke database di kelas ini.
				 * </p>
				 *
				 * @see RekapitulasiUjianHelper#buatbaru(Tbmuser, VOPembelajaran, DataLoader, int, String)
				 */
				class PertemuanEvent implements EventListener {

					/**
					 * Konteks pembelajaran yang daftar pertemuannya diisikan ke combobox. Bila
					 * {@code null}, combobox dikosongkan dan baris tombol tambah pertemuan
					 * disembunyikan (belum ada Perkuliahan/Jadwal Pelajaran terpilih).
					 */
					private VOPembelajaran vo;
					/**
					 * Pertemuan yang harus langsung terpilih setelah combobox diisi ulang — dipakai
					 * ketika pengguna baru saja membuat satu pertemuan, agar hasilnya otomatis aktif
					 * dan combobox dikunci. {@code null} berarti tanpa pra-pilihan.
					 */
					private Pertemuan pertemuanPilih = null;

					/**
					 * Membuat listener pengisi combobox tanpa pra-pilihan pertemuan.
					 *
					 * @param vo konteks pembelajaran sumber daftar pertemuan; boleh {@code null}
					 */
					public PertemuanEvent(VOPembelajaran vo) {
						this.vo = vo;
					}

					/**
					 * Membuat listener pengisi combobox yang sekaligus mengunci pilihan ke satu
					 * pertemuan tertentu.
					 *
					 * @param vo             konteks pembelajaran sumber daftar pertemuan; boleh {@code null}
					 * @param pertemuanPilih pertemuan yang langsung dipilih dan dikunci setelah pengisian
					 */
					public PertemuanEvent(VOPembelajaran vo, Pertemuan pertemuanPilih) {
						this.vo = vo;
						this.pertemuanPilih = pertemuanPilih;
					}

					/**
					 * Mengisi ulang combobox "Pilih Pertemuan" sesuai {@code vo} dan
					 * {@code pertemuanPilih}, serta mereset {@code pdata} milik listener induk. Aman
					 * dipanggil dengan {@code arg0} bernilai {@code null} (kelas ini kerap dipanggil
					 * langsung, bukan lewat dispatch event ZK) karena isi event tidak pernah dibaca.
					 *
					 * @param arg0 event pemicu; tidak dipakai, boleh {@code null}
					 * @throws Exception diteruskan dari operasi komponen ZK
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						pdata = null;
						rowTambah.setVisible(vo != null);
						if (pertemuan != null) {
							Common.clear(pertemuan);
						}
						List<Pertemuan> pertemuans = vo == null ? new ArrayList<Pertemuan>() : vo.ambilPertemuanList();
						for (Pertemuan p : pertemuans) {
							Comboitem comboitem = new Comboitem("Pertemuan ke-" + p.getPertemuanKe() + " "
									+ p.getTopik() + " "
									+ (p.getStatusPertemuan() == null ? "" : p.getStatusPertemuan().getNama()) + " "
									+ (p.getTanggal() == null ? "" : Common.dateFormat6.get().format(p.getTanggal())));
							comboitem.setValue(p);
							pertemuan.appendChild(comboitem);
							pdata = p;
						}
						pertemuan.setReadonly(true);
						pertemuan.setDisabled(false);

						if (pertemuans.size() == 1) {
							pertemuan.setSelectedIndex(0);
							pertemuan.setDisabled(true);
							Common.selectComboItem(pertemuan, pdata);
						} else if (!pertemuans.isEmpty()) {
							Comboitem comboitem = new Comboitem("= Pilih Pertemuan =");
							comboitem.setValue(null);
							pertemuan.appendChild(comboitem);
							pertemuan.setSelectedItem(comboitem);
						} else {
							Comboitem comboitem = new Comboitem("= Pertemuan / Agenda belum dibuat =");
							comboitem.setValue(null);
							pertemuan.appendChild(comboitem);
							pertemuan.setSelectedItem(comboitem);
							pertemuan.setDisabled(true);
						}

						if (pertemuanPilih != null) {
							Common.selectComboItem(true, pertemuan, pertemuanPilih);
							pertemuan.setDisabled(true);
						}
					}

				}

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				if (perkuliahan != null) {
					row.appendChild(new ais.ui.util.MyLabelConfig("Info"));
					row.appendChild(new Label(perkuliahan.infoSimple()));

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Pertemuan"));
					row.appendChild(pertemuan);
					pertemuan.setWidth("90%");

					final EventListener eventListenerPertemuan = new PertemuanEvent(perkuliahan);
					eventListenerPertemuan.onEvent(null);

					rowTambah.appendChild(new ais.ui.util.MyLabelConfig(""));
					rowTambah.setParent(rows);

					Hbox hbox = new Hbox();
					hbox.setParent(rowTambah);

					if (perkuliahan instanceof JadwalPelajaran) {

						MyToolbarbuttonConfig button = PenjadwalanSiswaHelper
								.buatSatuPertemuan((JadwalPelajaran) perkuliahan, tbmuser, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										Pertemuan pertemuan = (Pertemuan) arg0.getData();
										perkuliahan.belum();
										Common.createDefaultTimer(new PertemuanEvent(perkuliahan, pertemuan));
									}
								});
						button.setLabel("Tambahkan Pertemuan/Agenda Baru");
						button.setParent(hbox);

					} else if (perkuliahan instanceof Perkuliahan) {

						MyToolbarbuttonConfig button = PenjadwalanHelper.buatSatuPertemuan((Perkuliahan) perkuliahan,
								tbmuser, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										Pertemuan pertemuan = (Pertemuan) arg0.getData();
										perkuliahan.belum();
										Common.createDefaultTimer(new PertemuanEvent(perkuliahan, pertemuan));
									}
								});
						button.setLabel("Tambahkan Pertemuan Baru");
						button.setParent(hbox);

						PenjadwalanHelper.tampilTombolBuatPertemuan(hbox, (Perkuliahan) perkuliahan,
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										perkuliahan.belum();
										Common.createDefaultTimer(eventListenerPertemuan);
									}
								});

						PenjadwalanHelper.tampilTombolAmbil(hbox, (Perkuliahan) perkuliahan, null, null, null, null,
								null, null, new DataLoader() {

									@Override
									public void loadData(Object value) {
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												perkuliahan.belum();
												Common.createDefaultTimer(eventListenerPertemuan);
											}
										});
									}
								});

					}

				} else {

					Sekolah sk = SekolahUtil.getSekolah();

					row = new MyFormRow();
					row.setVisible((sk == null || sk.getId() == null) && tbmuser != null && tbmuser.getGuru() == null);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Perkuliahan *"));
					final AmbilDataPerkuliahanBandbox pilihPerkuliahan;
					row.appendChild(pilihPerkuliahan = new AmbilDataPerkuliahanBandbox());
					pilihPerkuliahan.setReadonly(true);
					pilihPerkuliahan.setWidth("90%");

					row = new MyFormRow();
					row.setVisible(sk != null && sk.getId() != null);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Kelas Siswa *"));
					final AmbilDataKelasSiswaBanbox pilihKelasSiswa;
					row.appendChild(pilihKelasSiswa = new AmbilDataKelasSiswaBanbox(false, false));
					pilihKelasSiswa.setReadonly(true);
					pilihKelasSiswa.setWidth("90%");

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Kelas Les Siswa *"));
					final AmbilDataKelasLesSiswaBanbox pilihKelasLesSiswa;
					row.appendChild(pilihKelasLesSiswa = new AmbilDataKelasLesSiswaBanbox());
					pilihKelasLesSiswa.setWidth("90%");

					row = new MyFormRow();
					row.setVisible(sk != null && sk.getId() != null);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
					final Combobox semester;
					row.appendChild(semester = new Combobox());
					Comboitem comboitem = new Comboitem(JadwalPelajaran.GANJIL);
					comboitem.setValue(1);
					semester.appendChild(comboitem);
					comboitem = new Comboitem(JadwalPelajaran.GENAP);
					comboitem.setValue(2);
					semester.appendChild(comboitem);
					Common.selectComboItem(true, semester, Common.isNowSemensterGanjil() ? 1 : 2);
					semester.setWidth("90%");
					semester.setReadonly(true);

					row = new MyFormRow();
					row.setVisible(sk != null && sk.getId() != null);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Mata Pelajaran *"));
					final Combobox matapelajaran;
					row.appendChild(matapelajaran = new Combobox());
					matapelajaran.setWidth("90%");
					matapelajaran.setReadonly(true);

					row = new MyFormRow();
					row.setVisible(sk != null && sk.getId() != null);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Jadwal Pelajaran *"));
					row.appendChild(jadwal);
					jadwal.setWidth("90%");
					jadwal.setReadonly(true);

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Pertemuan *"));
					row.appendChild(pertemuan);
					pertemuan.setWidth("90%");

					final EventListener eventListenerPilihKelas = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							pilihKelasSiswa.getParent()
									.setVisible(pilihKelasLesSiswa.getAttribute("kelasLesSiswa") == null);
							semester.getParent().setVisible(pilihKelasLesSiswa.getAttribute("kelasLesSiswa") == null);
							matapelajaran.getParent()
									.setVisible(pilihKelasLesSiswa.getAttribute("kelasLesSiswa") == null);
							pilihKelasLesSiswa.getParent()
									.setVisible(pilihKelasSiswa.getAttribute("kelasSiswa") == null);
						}
					};

					pilihKelasLesSiswa.setEventListener(eventListenerPilihKelas);

					try {
						eventListenerPilihKelas.onEvent(null);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RekapitulasiUjianHelper.java:355");
					}

					pilihKelasSiswa.setEventListener(new EventListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event arg0) throws Exception {
							eventListenerPilihKelas.onEvent(arg0);
							KelasSiswa kelasSiswa = (KelasSiswa) pilihKelasSiswa.getAttribute("kelasSiswa");
							if (kelasSiswa != null) {
								pilihPerkuliahan.getParent().setVisible(false);

								Sekolah s = kelasSiswa.getSekolah();
								System.out.println("s => " + s);

								List<Long> longs = kelasSiswa == null ? new ArrayList<Long>() : kelasSiswa.ambilMk();

								List<Long> ids = new ArrayList<Long>();
								Tbmuser tbmuser = Common.getCurrentUser();
								Guru guru = tbmuser.ambilGuru();
								if (guru != null) {
									Session session = HibernateUtil.currentSession();
									ids = session.createCriteria(JadwalPelajaran.class)

											.add(semester.getSelectedItem() == null
													|| semester.getSelectedItem().getValue() == null
													|| semester.getSelectedItem().getValue() == null
															? Restrictions.sqlRestriction("1=1")
															: Restrictions.eq("semester",
																	semester.getSelectedItem().getValue()))

											.add(Restrictions.eq("tahunAjaran", kelasSiswa.getTahunAjaran()))

											.add(Restrictions.isNotNull("matapelajaran"))
											.setProjection(Projections.groupProperty("matapelajaran.id"))

											.add(guru == null ? Restrictions.sqlRestriction("1=1") :

													Restrictions.or(Restrictions.eq("guru12", guru), Restrictions.or(
															Restrictions.eq("guru11", guru),
															Restrictions.or(Restrictions.eq("guru10", guru),
																	Restrictions.or(Restrictions.eq("guru9", guru),
																			Restrictions.or(
																					Restrictions.eq("guru8", guru),
																					Restrictions.or(
																							Restrictions.eq("guru7",
																									guru),
																							Restrictions.or(Restrictions
																									.eq("guru6", guru),
																									Restrictions.or(
																											Restrictions
																													.eq("guru5",
																															guru),
																											Restrictions
																													.or(Restrictions
																															.eq("guru4",
																																	guru),
																															Restrictions
																																	.or(Restrictions
																																			.eq("guru3",
																																					guru),
																																			Restrictions
																																					.or(Restrictions
																																							.eq("guru",
																																									guru),
																																							Restrictions
																																									.eq("guru2",
																																											guru))))))))))))

											).list();
								}

								Common.insertCombo(matapelajaran, new String[] { "nama", "jenisPenilaian", "sekolah" },
										Matapelajaran.class,

										Restrictions.and(
												guru != null && ids.isEmpty() ? Restrictions.sqlRestriction("false")
														: ids.isEmpty() ? Restrictions.sqlRestriction("true")
																: Restrictions.in("id", ids),

												Restrictions.and(
														longs.isEmpty() ? Restrictions.sqlRestriction("true")
																: Restrictions.not(Restrictions.in("id", longs)),

														Restrictions.and(
																Restrictions.or(Restrictions.isNull("sekolah"),
																		Restrictions.eq("sekolah", s)),
																Restrictions.or(Restrictions.isNull("aktif"),
																		Restrictions.eq("aktif", true))))));

								matapelajaran.setReadonly(true);
							}
						}
					});

					final Hbox hbox = new Hbox();
					final EventListener jadwalEventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (hbox != null) {
								Common.clear(hbox);
							}
							final JadwalPelajaran mk = (JadwalPelajaran) (jadwal.getSelectedItem() == null ? null
									: jadwal.getSelectedItem().getValue());
							if (mk != null) {
								Common.createDefaultTimer(new PertemuanEvent(mk));

								MyToolbarbuttonConfig button = PenjadwalanSiswaHelper.buatSatuPertemuan(mk, tbmuser,
										new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												Pertemuan pertemuan = (Pertemuan) arg0.getData();
												mk.belum();
												Common.createDefaultTimer(new PertemuanEvent(mk, pertemuan));
											}
										});
								button.setLabel("Tambahkan Pertemuan/Agenda Baru");
								button.setParent(hbox);

							}

						}
					};
					final EventListener pilihPerkuliahanEventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (hbox != null) {
								Common.clear(hbox);
							}
							final Perkuliahan mk = (Perkuliahan) pilihPerkuliahan.getAttribute("perkuliahan");
							if (mk != null) {

								Common.createDefaultTimer(new PertemuanEvent(mk));

								MyToolbarbuttonConfig button = PenjadwalanHelper.buatSatuPertemuan(mk, tbmuser,
										new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												Pertemuan pertemuan = (Pertemuan) arg0.getData();
												mk.belum();
												Common.createDefaultTimer(new PertemuanEvent(mk, pertemuan));
											}
										});
								button.setLabel("Tambahkan Pertemuan Baru");
								button.setParent(hbox);

								PenjadwalanHelper.tampilTombolBuatPertemuan(hbox, mk, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										mk.belum();
										Common.createDefaultTimer(new PertemuanEvent(mk));
									}
								});

								PenjadwalanHelper.tampilTombolAmbil(hbox, mk, null, null, null, null, null, null,
										new DataLoader() {

											@Override
											public void loadData(Object value) {
												mk.belum();
												Common.createDefaultTimer(new PertemuanEvent(mk));
											}
										});
							}

						}
					};

					EventListener eventListener = new EventListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (jadwal != null) {
								Common.clear(jadwal);
							}
							KelasSiswa kelasSiswa = (KelasSiswa) pilihKelasSiswa.getAttribute("kelasSiswa");

							KelasLesSiswa kelasLesSiswa = (KelasLesSiswa) pilihKelasLesSiswa
									.getAttribute("kelasLesSiswa");

							Matapelajaran mk = (Matapelajaran) (kelasLesSiswa != null ? kelasLesSiswa.getMatapelajaran()
									: (matapelajaran.getSelectedItem() == null ? null
											: matapelajaran.getSelectedItem().getValue()));
							if (kelasLesSiswa != null || (kelasSiswa != null && mk != null)) {

								Tbmuser tbmuser = Common.getCurrentUser();
								Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();

								List<JadwalPelajaran> jadwalPelajarans = ConstantValues.simpleList(kelasLesSiswa != null
										? HibernateUtil.currentSession().createCriteria(JadwalPelajaran.class)
												.add(Restrictions.eq("kelasLesSiswa", kelasLesSiswa))
												.add(guru == null ? Restrictions.sqlRestriction("1=1") :

														Restrictions.or(Restrictions.eq("guru12", guru), Restrictions
																.or(Restrictions.eq("guru11", guru), Restrictions.or(
																		Restrictions.eq("guru10", guru),
																		Restrictions.or(Restrictions.eq("guru9", guru),
																				Restrictions.or(
																						Restrictions.eq("guru8", guru),
																						Restrictions.or(
																								Restrictions.eq("guru7",
																										guru),
																								Restrictions.or(
																										Restrictions.eq(
																												"guru6",
																												guru),
																										Restrictions.or(
																												Restrictions
																														.eq("guru5",
																																guru),
																												Restrictions
																														.or(Restrictions
																																.eq("guru4",
																																		guru),
																																Restrictions
																																		.or(Restrictions
																																				.eq("guru3",
																																						guru),
																																				Restrictions
																																						.or(Restrictions
																																								.eq("guru",
																																										guru),
																																								Restrictions
																																										.eq("guru2",
																																												guru)))))))))))))
										:

										HibernateUtil.currentSession().createCriteria(JadwalPelajaran.class)
												.add(Restrictions.eq("matapelajaran", mk))
												.add(Restrictions.eq("kelas", kelasSiswa))

												.add(Restrictions.and(
														Restrictions.eq("tahunAjaran", kelasSiswa.getTahunAjaran()),

														semester.getSelectedItem() == null
																|| semester.getSelectedItem().getValue() == null
																|| semester.getSelectedItem().getValue() == null
																		? Restrictions.sqlRestriction("1=1")
																		: Restrictions.eq("semester",
																				semester.getSelectedItem().getValue())))

												.add(guru == null ? Restrictions.sqlRestriction("1=1") :

														Restrictions.or(Restrictions.eq("guru12", guru), Restrictions
																.or(Restrictions.eq("guru11", guru), Restrictions.or(
																		Restrictions.eq("guru10", guru),
																		Restrictions.or(Restrictions.eq("guru9", guru),
																				Restrictions.or(
																						Restrictions.eq("guru8", guru),
																						Restrictions.or(
																								Restrictions.eq("guru7",
																										guru),
																								Restrictions.or(
																										Restrictions.eq(
																												"guru6",
																												guru),
																										Restrictions.or(
																												Restrictions
																														.eq("guru5",
																																guru),
																												Restrictions
																														.or(Restrictions
																																.eq("guru4",
																																		guru),
																																Restrictions
																																		.or(Restrictions
																																				.eq("guru3",
																																						guru),
																																				Restrictions
																																						.or(Restrictions
																																								.eq("guru",
																																										guru),
																																								Restrictions
																																										.eq("guru2",
																																												guru))))))))))))

												)

										, JadwalPelajaran.class);

								for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {
									Comboitem comboitem = new Comboitem(jadwalPelajaran.infoSimple());
									comboitem.setValue(jadwalPelajaran);
									jadwal.appendChild(comboitem);
								}
								jadwal.setReadonly(true);

								if (jadwalPelajarans.size() == 1) {
									jadwal.setSelectedIndex(0);

									Common.createDefaultTimer(jadwalEventListener);

								} else if (!jadwalPelajarans.isEmpty()) {
									Comboitem comboitem = new Comboitem("= Pilih Jadwal =");
									comboitem.setValue(null);
									jadwal.appendChild(comboitem);
									jadwal.setSelectedItem(comboitem);
								}
							}

						}
					};

					semester.addEventListener("onChange", eventListener);
					matapelajaran.addEventListener("onChange", eventListener);

					jadwal.addEventListener("onChange", jadwalEventListener);

					pilihPerkuliahan.setEventListener(pilihPerkuliahanEventListener);

					rowTambah.appendChild(new ais.ui.util.MyLabelConfig(""));
					rowTambah.setParent(rows);

					ais.ui.util.MenuAksiBaris.pasang(hbox);
					hbox.setParent(rowTambah);

				}

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Lanjut", "/img/save.gif");
				save.setTooltiptext("Lanjut Tambah Data");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Pertemuan pert = (Pertemuan) (pertemuan.getSelectedItem() == null ? null
								: pertemuan.getSelectedItem().getValue());

						if (pert == null) {
							MyMessageboxConfig.show("Mohon maaf, pertemuan belum dipilih. Langkah yang dapat dilakukan: (1) pilih pertemuan dari daftar yang tersedia; (2) pastikan data pertemuan sudah ada; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							// Hentikan: tanpa return, eksekusi lanjut ke pert.getId() → NullPointerException.
							return;
						}
						window.detach();

						if (nama.equalsIgnoreCase("Tugas Kelompok")) {
							TugasKelompok tugasKelompok = new TugasKelompok();
							tugasKelompok.setPertemuan(pert.getId());
							tugasKelompok.setPerkuliahan(pert.getPerkuliahan());
							tugasKelompok.setJadwalPelajaran(pert.getJadwalPelajaran());
							TugasKelompokHelper.onAddExternal(event, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									// TODO Auto-generated method stub

								}
							}, tugasKelompok, tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa());

						} else {

							new PertemuanHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())
									.display(pert, dataLoader, nomor);
						}
					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});
		return refresh;
	}

	/** Seperti {@link #display(Component, Tbmuser, VOPembelajaran)} tanpa membatasi ke satu {@link VOPembelajaran} — menampilkan rekap ujian milik {@code tbmuser} secara umum. */
	public static void display(Component parent, final Tbmuser tbmuser) {
		display(parent, tbmuser, null);
	}

	/**
	 * Membangun panel "Rekapitulasi Ujian" ke dalam {@code parent}: toolbar pencarian
	 * (dan filter rentang tanggal bila {@code perkuliahan} {@code null}), tombol "Buat
	 * Ujian" ({@link #buatbaru}, tab 6) dan Refresh. Rentang tanggal default: satu
	 * bulan sebelum s.d. satu bulan setelah rencana tahun akademik berjalan. Grid
	 * dimuat lewat {@link #reload(Tbmuser, Component, Date, Date, String, boolean, boolean, VOPembelajaran)}.
	 *
	 * @param parent      kontainer ZK tempat panel dibangun
	 * @param tbmuser     pengguna yang rekap ujiannya ditampilkan (menentukan cakupan pertemuan: mahasiswa/dosen/guru/siswa/admin)
	 * @param perkuliahan bila terisi, membatasi rekap hanya ke pertemuan milik satu {@link Perkuliahan}/{@link JadwalPelajaran} ini; {@code null} untuk rekap umum milik {@code tbmuser}
	 */
	public static void display(Component parent, final Tbmuser tbmuser, final VOPembelajaran perkuliahan) {

		org.zkoss.zul.Vbox subVboxUtama = new org.zkoss.zul.Vbox();
		subVboxUtama.setWidth("100%");
		subVboxUtama.setParent(parent);

		final org.zkoss.zul.Div center = new org.zkoss.zul.Div();
		center.setWidth("100%");
		Toolbar hbox = new Toolbar();
		hbox.setParent(subVboxUtama);

		final Textbox cari = new Textbox();
		hbox.appendChild(new MyLabelConfig("Cari:"));
		hbox.appendChild(cari);
		cari.setCols(10);

		RencanaTahunAkademik rencanaTahunAkademik = RencanaTahunAkademikAction
				.getCurrentRencanaTahunAkademik(WaktuUtil.getDate());

		Calendar calendarMulai = Calendar.getInstance();
		calendarMulai
				.setTime(rencanaTahunAkademik == null ? WaktuUtil.getDate() : rencanaTahunAkademik.getTanggalMulai());
		calendarMulai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) - 1);

		Calendar calendarSampai = Calendar.getInstance();
		calendarSampai
				.setTime(rencanaTahunAkademik == null ? WaktuUtil.getDate() : rencanaTahunAkademik.getTanggalSampai());
		calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 1);

		final MyDatebox mulai = new MyDatebox(calendarMulai.getTime());
		mulai.setReadonly(true);

		final MyDatebox sampai = new MyDatebox(calendarSampai.getTime());
		sampai.setReadonly(true);

		if (perkuliahan == null) {
			hbox.appendChild(new MyLabelConfig("Tanggal"));
			hbox.appendChild(mulai);
			hbox.appendChild(new MyLabelConfig("sd"));
			hbox.appendChild(sampai);
		}

		RekapitulasiUjianHelper.buatbaru(tbmuser, perkuliahan, new DataLoader() {

			@Override
			public void loadData(Object value) {
				try {
					reload(tbmuser, center, mulai.getValue(), sampai.getValue(), cari.getValue().trim(), true, true,
							perkuliahan);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RekapitulasiUjianHelper.java:795");
					// TODO: handle exception
				}
			}
		}, 6, "Ujian").setParent(hbox);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/refresh.png");
		refresh.setTooltiptext("Refresh");
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				reload(tbmuser, center, mulai.getValue(), sampai.getValue(), cari.getValue().trim(), true, true,
						perkuliahan);
			}
		});
		refresh.setParent(hbox);

		if (RecoveryAktivitasPembelajaranHelper.bolehTampil(tbmuser)) {
			MyToolbarbuttonConfig recovery = new MyToolbarbuttonConfig("Recovery", "/img/jadwal.png");
			recovery.setTooltiptext("Kembalikan ujian yang terhapus, termasuk relasi soal-soalnya.");
			recovery.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					RecoveryAktivitasPembelajaranHelper.bukaRecoveryUjian(perkuliahan, new EventListener() {
						@Override
						public void onEvent(Event callbackEvent) throws Exception {
							reload(tbmuser, center, mulai.getValue(), sampai.getValue(), cari.getValue().trim(),
									true, true, perkuliahan);
						}
					});
				}
			});
			recovery.setParent(hbox);
		}

		center.setParent(subVboxUtama);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload(tbmuser, center, mulai.getValue(), sampai.getValue(), cari.getValue().trim(), false, true,
						perkuliahan);
			}
		};

		Common.createDefaultTimer(eventListener);
		cari.addEventListener("onOK", eventListener);
		mulai.addEventListener("onChange", eventListener);
		sampai.addEventListener("onChange", eventListener);
	}

	/**
	 * Menentukan daftar id {@link Pertemuan} yang relevan bagi konteks tampilan
	 * (langsung dari {@code perkuliahan} bila terisi; via
	 * {@code Mahasiswa#ambilPertemuan}/{@code Dosen#ambilPertemuan} bila pengguna
	 * mahasiswa/dosen — dengan opsi muat ulang cache 6 bulan sebelum-sesudah rentang
	 * tanggal saat {@code refreh}; via kriteria timeline umum
	 * {@link DashboardTimelinePertemuan#initStaticCriteria} untuk guru/siswa sekolah;
	 * atau kosong untuk admin karena filter dilewati di query data), lalu membangun
	 * grid berpaging dan memuat halaman pertama lewat
	 * {@link #reload(List, Paging, MyGrid, Mahasiswa, boolean, Date, Date, String, VOPembelajaran)}.
	 *
	 * @param tbmuser    pengguna yang menentukan cakupan pertemuan
	 * @param center     kontainer ZK yang akan diisi ulang (dibersihkan lebih dulu)
	 * @param mulai      awal rentang tanggal filter
	 * @param sampai     akhir rentang tanggal filter
	 * @param cari       kata kunci pencarian kode/nama ujian
	 * @param refreh     paksa muat ulang cache pertemuan mahasiswa/dosen dari database
	 * @param awal       arahkan halaman aktif ke baris pertama yang tanggal ujiannya sudah lewat
	 * @param perkuliahan bila terisi, membatasi ke satu {@link VOPembelajaran} tertentu
	 */
	@SuppressWarnings("unchecked")
	private static void reload(final Tbmuser tbmuser, final Component center, final Date mulai, final Date sampai,
			final String cari, boolean refreh, boolean awal, final VOPembelajaran perkuliahan) {
		if (center != null) {
			Common.clear(center);
		}

		final Mahasiswa mahasiswa = tbmuser.getMahasiswa();
		Dosen dosen = tbmuser.ambilDosen();

		final Paging paging = new Paging();

		final List<Long> pertemuans = new ArrayList<Long>();

		Session session = HibernateUtil.currentSession();

		if (perkuliahan != null) {
			Criteria criteria = session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(perkuliahan instanceof Perkuliahan ? Restrictions.eq("perkuliahan", perkuliahan)
							: perkuliahan instanceof JadwalPelajaran ? Restrictions.eq("jadwalPelajaran", perkuliahan)
									: Restrictions.sqlRestriction("false"));
			pertemuans.addAll(criteria.setProjection(Projections.property("id")).setMaxResults(32766).list());
		}

		else if (mahasiswa != null) {

			if (refreh) {

				Calendar calendarMulai = Calendar.getInstance();
				calendarMulai.setTime(mulai);
				calendarMulai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) - 6);

				Calendar calendarSampai = Calendar.getInstance();
				calendarSampai.setTime(sampai);
				calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 6);

				mahasiswa.reInitPertemuan(session, new Label(), calendarMulai.getTime(), calendarSampai.getTime());
			}

			pertemuans.addAll(mahasiswa.ambilPertemuan(session).values());

		} else if (dosen != null) {

			if (refreh) {

				Calendar calendarMulai = Calendar.getInstance();
				calendarMulai.setTime(mulai);
				calendarMulai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) - 6);

				Calendar calendarSampai = Calendar.getInstance();
				calendarSampai.setTime(sampai);
				calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 6);

				dosen.reInitPertemuan(session, new Label(), calendarMulai.getTime(), calendarSampai.getTime());
			}

			pertemuans.addAll(dosen.ambilPertemuan(session).values());
		} else if (!Common.getApakahAdmin()) {

			Sekolah sk = SekolahUtil.getSekolah();
			Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();
			Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();

			if (guru != null) {
				sk = guru.getSekolah();
			}
			if (siswa != null) {
				sk = siswa.getSekolah();
			}

			Calendar calendarMulai = Calendar.getInstance();
			calendarMulai.setTime(mulai);
			calendarMulai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) - 6);

			Calendar calendarSampai = Calendar.getInstance();
			calendarSampai.setTime(sampai);
			calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 6);

			String mul = null;
			String sam = null;

			Integer bln = 1;
			Integer ke = null;

			boolean jadwalPerkuliahan = !(sk != null && sk.getId() != null);

			boolean jadwalPelajaran = (sk != null && sk.getId() != null);

			boolean jadwalKkn = jadwalPerkuliahan;

			boolean jadwalPkl = jadwalPerkuliahan;

			boolean jadwalKegiatan = jadwalPerkuliahan;

			boolean jadwalRevisi = jadwalPerkuliahan;
			boolean jadwalKonsultasi = jadwalPerkuliahan;

			boolean jadwalBimbingan = jadwalPerkuliahan;

			boolean jadwalKonsultasiLain = jadwalPerkuliahan;

			boolean tdpDiskusi = false;

			boolean tdpUjian = false;

			boolean tdpMateri = false;
			boolean tdpTugas = false;
			boolean tdpCatatan = false;
			boolean tdpAudio = false;
			boolean tdpVideo = false;
			boolean tdpDosenPengganti = false;

			String cariMk = "";
			String cariDosen = "";
			String cariTopik = "";
			String cariCatatan = "";
			String cariMahasiswa = "";
			String cariKelas = "";
			String cariRuang = "";

			Integer ekstra = null;

			boolean remedial = false;
			boolean paralel = false;
			boolean pra = false;
			StatusPertemuan statusPertemuan = null;
			boolean ujian = false;

			String day = null;

			Criteria criteria = DashboardTimelinePertemuan.initStaticCriteria(true, calendarMulai.getTime(),
					calendarSampai.getTime(), tbmuser, cari, mul, sam, bln, statusPertemuan, ke, day, tdpVideo,
					tdpAudio, tdpMateri, jadwalPerkuliahan, jadwalPelajaran, jadwalKkn, jadwalPkl, jadwalRevisi,
					jadwalKonsultasi, jadwalBimbingan, jadwalKonsultasiLain, jadwalKegiatan, tdpUjian, tdpDiskusi,
					tdpTugas, tdpCatatan, tdpDosenPengganti, cariTopik, cariCatatan, paralel, pra, remedial, cariMk,
					ekstra != null && ekstra.equals(Perkuliahan.EKSTRA), cariKelas, cariRuang, cariDosen, cariMahasiswa,
					ujian, sk, session);

			pertemuans.addAll(criteria.setProjection(Projections.property("id")).setMaxResults(32766).list());
		}

		paging.setDetailed(true);
		paging.setPageSize(Common.ROWS_COUNT_ON_PAGE);
		paging.setPageIncrement(Common.isMobile() ? 5 : 10);
		paging.setMold("os");

		final MyGrid grid = new MyGrid();
		paging.addEventListener("onPaging", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				reload(pertemuans, paging, grid, mahasiswa, false, mulai, sampai, cari, perkuliahan);
			}
		});

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(center);

		groupbox.appendChild(paging);

		grid.setParent(groupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ujian");

		try {

			if (tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null
					|| tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null)) {
				column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth((Common.isMobile() ? "40%" : "30%"));
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RekapitulasiUjianHelper.java:1011");
		}

		reload(pertemuans, paging, grid, mahasiswa, awal, mulai, sampai, cari, perkuliahan);

	}

	/**
	 * Implementasi inti pemuatan data grid rekap ujian lewat Hibernate {@link Criteria}
	 * atas {@link PertemuanPunyaUjian} (join {@code ujian}). Menghitung total baris dan
	 * (bila {@code awal}) menentukan halaman aktif berdasarkan jumlah baris yang
	 * tanggal ujiannya sebelum hari ini, lalu mengambil satu halaman data terurut
	 * berdasarkan tanggal mulai ujian. Filter kepemilikan memakai {@code pertemuans}
	 * kecuali untuk admin di luar konteks {@code perkuliahan} spesifik (filter
	 * dilewati sepenuhnya). Filter rentang tanggal (bila {@code perkuliahan}
	 * {@code null}) disuntikkan sebagai literal SQL ter-format dari objek {@link Date}
	 * (bukan input pengguna langsung).
	 *
	 * @param pertemuans  daftar id {@link Pertemuan} yang menjadi cakupan (diabaikan untuk admin non-konteks)
	 * @param paging      komponen paging yang total/halaman aktifnya diperbarui
	 * @param grid        grid target yang modelnya diisi ulang
	 * @param mahasiswa   mahasiswa terkait (diteruskan ke {@link DetailPertemuanRenderer})
	 * @param awal        hitung ulang dan set halaman aktif ke baris pertama yang sudah lewat tanggal ujian
	 * @param mulai       awal rentang tanggal filter (diabaikan bila {@code perkuliahan} terisi)
	 * @param sampai      akhir rentang tanggal filter (diabaikan bila {@code perkuliahan} terisi)
	 * @param cari        kata kunci pencarian kode/nama ujian
	 * @param perkuliahan bila terisi, filter tanggal tidak berlaku (semua ujian pada pertemuan cakupan ditampilkan)
	 */
	@SuppressWarnings("unchecked")
	private static void reload(final List<Long> pertemuans, final Paging paging, final MyGrid grid,
			final Mahasiswa mahasiswa, boolean awal, final Date mulai, final Date sampai, final String cari,
			final VOPembelajaran perkuliahan) {
		Session session = HibernateUtil.currentSession();

		Criterion criterion = Common.getApakahAdmin() && perkuliahan == null ? Restrictions.sqlRestriction("true")
				: (pertemuans.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("pertemuan.id", pertemuans));
		int size = 0;
		try {
			size = ((Number) session.createCriteria(PertemuanPunyaUjian.class)

					.createAlias("ujian", "ujian")

					.add(cari.isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("ujian.kode", cari, MatchMode.ANYWHERE),
									Restrictions.ilike("ujian.nama", cari, MatchMode.ANYWHERE)))

					.add(perkuliahan != null ? Restrictions.sqlRestriction("true")
							: Restrictions.sqlRestriction(
									"date(this_.mulai_ujian) between date('" + Common.databaseDateFormat.get().format(mulai)
											+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"))
					.add(criterion).setProjection(Projections.rowCount()).uniqueResult()).intValue();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RekapitulasiUjianHelper.java:1042");
			// TODO: handle exception
		}
		paging.setTotalSize(size);
		paging.setVisible(size > Common.ROWS_COUNT_ON_PAGE);

		if (awal) {

			int page = ((Number) session.createCriteria(PertemuanPunyaUjian.class).createAlias("ujian", "ujian")

					.add(cari.isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("ujian.kode", cari, MatchMode.ANYWHERE),
									Restrictions.ilike("ujian.nama", cari, MatchMode.ANYWHERE)))
					.add(criterion).add(Restrictions.sqlRestriction("date(mulai_ujian)<CURRENT_DATE"))

					.add(perkuliahan != null ? Restrictions.sqlRestriction("true")
							: Restrictions.sqlRestriction(
									"date(this_.mulai_ujian) between date('" + Common.databaseDateFormat.get().format(mulai)
											+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"))

					.setProjection(Projections.rowCount()).uniqueResult()).intValue();

			try {
				System.out.println("page -> " + page + ", size -> " + size + ", pertemuans -> " + pertemuans.size());
				paging.setActivePage((int) (page / Common.ROWS_COUNT_ON_PAGE));
			} catch (Exception e) {
				try {

					paging.setActivePage(((int) (page / Common.ROWS_COUNT_ON_PAGE)) - 1);
				} catch (Exception ae) { ais.common.ErrorAuditUtil.record(ae, "auto-audit(empty-catch) src/ais/action/master/helper/RekapitulasiUjianHelper.java:1071");
					// TODO: handle exception
				}
			}
		}

		List<PertemuanPunyaUjian> pertemuanPunyaUjians = session.createCriteria(PertemuanPunyaUjian.class)

				.createAlias("ujian", "ujian")

				.add(cari.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("ujian.kode", cari, MatchMode.ANYWHERE),
								Restrictions.ilike("ujian.nama", cari, MatchMode.ANYWHERE)))

				.add(perkuliahan != null ? Restrictions.sqlRestriction("true")
						: Restrictions.sqlRestriction(
								"date(this_.mulai_ujian) between date('" + Common.databaseDateFormat.get().format(mulai)
										+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"))
				.add(criterion)

				.addOrder(Order.asc("mulaiUjian")).addOrder(Order.asc("id")).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pertemuanPunyaUjians);
		grid.setModelCheckMobile(strset);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reload(pertemuans, paging, grid, mahasiswa, false, mulai, sampai, cari, perkuliahan);
			}
		};

		grid.setRowRenderer(new DetailPertemuanRenderer(mahasiswa, null, eventListener));

	}

	/**
	 * Perender baris grid rekap ujian. Menampilkan judul ujian, jumlah peserta yang
	 * telah mengikuti ujian, indikator "dilihat", dan rentang tanggal+topik pertemuan.
	 * Perilaku klik berbeda menurut jenis pengguna: untuk mahasiswa/calon
	 * mahasiswa/siswa/calon siswa, delegasi ke
	 * {@link PertemuanPunyaUjianHelper#tampilBolekIkutUjianAtauTidak} yang menampilkan
	 * kontrol ikut-ujian sesuai kuota dan hasil ujian mereka sendiri (menggantikan
	 * listener klik default); untuk staf, klik judul/tombol membuka
	 * {@link PertemuanHelper} pada tab Ujian (index 6).
	 */
	public static class DetailPertemuanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Pengguna yang sedang login, di-resolve SEKALI saat renderer dibuat (bukan per
		 * baris). Menentukan cabang perilaku klik baris: mahasiswa/calon mahasiswa/siswa/
		 * calon siswa mendapat kontrol ikut-ujian, selain itu baris membuka detail
		 * pertemuan. Karena diambil dari konteks request saat konstruksi, instance
		 * renderer tidak boleh dipakai ulang lintas session.
		 */
		private Tbmuser tbmuser = Common.getCurrentUser();
		/**
		 * Callback muat-ulang grid milik pemanggil, dipicu setelah jendela detail
		 * pertemuan ditutup atau setelah aksi ikut-ujian selesai, sehingga statistik
		 * peserta dan status baris ikut ter-refresh.
		 */
		private EventListener eventListener;

		/**
		 * Membuat renderer baris rekap ujian. Hanya {@code eventListener} yang disimpan;
		 * identitas peserta yang dibutuhkan saat render diambil ulang dari
		 * {@link #tbmuser} (pengguna login), bukan dari kedua parameter pertama.
		 *
		 * @param mahasiswa              tidak digunakan langsung (parameter dipertahankan untuk kompatibilitas dengan pola renderer serupa)
		 * @param biodataCalonMahasiswa  tidak digunakan langsung (parameter dipertahankan untuk kompatibilitas dengan pola renderer serupa)
		 * @param eventListener          callback yang dipanggil setelah jendela detail/aksi ikut-ujian selesai, untuk memuat ulang grid
		 */
		public DetailPertemuanRenderer(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
				EventListener eventListener) {
			this.eventListener = eventListener;
		}

		/**
		 * Merender satu baris {@link PertemuanPunyaUjian}: menyembunyikan baris bila
		 * pertemuan terkait sudah tidak ada, menyegarkan cache ujian
		 * ({@link HasilUjianHelper#reinitUjian}), lalu menampilkan judul, statistik
		 * peserta, dan link yang perilakunya bergantung jenis pengguna (lihat javadoc
		 * kelas).
		 */
		@Override
		public void render(final Row arg0, Object data) throws Exception {
			final PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) data;
			final Pertemuan pertemuan = pertemuanPunyaUjian.getPertemuan();
			if (pertemuan == null) {
				arg0.setVisible(false);
				return;
			}

			Ujian ujian = pertemuanPunyaUjian == null ? null : pertemuanPunyaUjian.getUjian();
			if (ujian != null) {
				HasilUjianHelper.reinitUjian(ujian, pertemuan);
			}

			String n = pertemuanPunyaUjian.getNama();
			if (n == null) {
				n = "";
			}
			Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
			vbox.setWidth("90%");
			vbox.setParent(arg0);

			Toolbarbutton downloadButton = new ais.ui.util.MyToolbarbuttonConfig(n.length() > 150 ? n.substring(0, 150) + "..." : n,
					"/img/svg/card-checklist.svg");

			downloadButton.setAttribute("janganDisabled", true);
			vbox.appendChild(downloadButton);

			Hbox myHbox = new Hbox();
			myHbox.setParent(vbox);
			Number tg = pertemuanPunyaUjian.ambilJumlahHasilUjianMahasiswaTelahIkut(false);
			MyLabelKecil labelKecil = new MyLabelKecil(
					"Ikut Ujian : " + Common.numberFormat.get().format(tg.intValue()) + " peserta");
			labelKecil.setStyle("font-size:8px;color:blue;");

			myHbox.appendChild(labelKecil);
			TampilanELearningAction.dilihat(pertemuan, "ujian_" + pertemuanPunyaUjian.getId(), "Akses", false)
					.setParent(myHbox);

			Date tgl = pertemuanPunyaUjian.getMulaiUjian();
			Date tgl1 = pertemuanPunyaUjian.getSampaiUjian();

			A a;
			vbox.appendChild(a = new A("Ujian pertemuan ke: " + pertemuan.getPertemuanKe() + ", " + pertemuan.info()
					+ ", "
					+ (tgl == null ? "" : (SmartDateTimeUtil.getDayString(tgl, null) + Common.dateFormat51.get().format(tgl)))
					+ (tgl1 == null ? ""
							: " sampai dengan "
									+ (SmartDateTimeUtil.getDayString(tgl1, null) + Common.dateFormat51.get().format(tgl1)))
					+ ", " + pertemuan.getTopik()));

			a.setStyle("font-size:12px;");

			List<EventListener> eventListeners = new ArrayList<EventListener>();

			if (tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null
					|| tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null)) {

				try {
					HasilUjianMahasiswa hasilUjianMahasiswa = HasilUjianMahasiswa.ambilByKey(pertemuanPunyaUjian,
							tbmuser == null ? null : tbmuser.getMahasiswa(),
							tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa(),
							tbmuser == null ? null : tbmuser.getSiswa(),
							tbmuser == null ? null : tbmuser.getCalonSiswa());
					if (hasilUjianMahasiswa != null) {
						if (pertemuanPunyaUjian.getUjian() == null && pertemuanPunyaUjian.getId() != null) {
							HibernateUtil.currentSession().refresh(pertemuanPunyaUjian);
							ProsesUjianHelper.kuotaUjian.remove(hasilUjianMahasiswa.getKeyhasil());
						}
					}

					PertemuanPunyaUjianHelper.tampilBolekIkutUjianAtauTidak(arg0, pertemuanPunyaUjian,
							tbmuser == null ? null : tbmuser.getMahasiswa(),
							tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa(), hasilUjianMahasiswa,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									Common.createDefaultTimer(eventListener);

								}
							}, eventListeners);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/RekapitulasiUjianHelper.java:1203");
					arg0.appendChild(new Label());
				}
			}

			if (eventListeners.isEmpty()) {
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						new PertemuanHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())
								.display(pertemuan, new DataLoader() {

									@Override
									public void loadData(Object value) {
										Common.createDefaultTimer(eventListener);

									}
								}, 6);

					}
				});

				downloadButton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						new PertemuanHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())
								.display(pertemuan, new DataLoader() {

									@Override
									public void loadData(Object value) {
										Common.createDefaultTimer(eventListener);
									}
								}, 6);

					}
				});
			} else {
				downloadButton.addEventListener("onClick", eventListeners.get(0));
				a.addEventListener("onClick", eventListeners.get(0));
			}

		}

	}

}
