package ais.action.master.sekolah.helper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.helper.AktifitasPerkuliahanHelper;
import ais.action.master.helper.CalendarPerkuliahanMingguIniComposer;
import ais.action.master.helper.PenjadwalanHelper;
import ais.action.master.helper.generic.AmbilDataTemplatePembelajaran;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.PertemuanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JadwalPelajaranPunyaItem;
import ais.database.model.sekolah.JamPelajaran;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyLabelEdit;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Helper layar pengelolaan <b>jadwal pertemuan</b> (sesi pembelajaran/{@link Pertemuan}) untuk
 * satu {@link JadwalPelajaran} (mata pelajaran+kelas) modul sekolah — analog dengan pengelolaan
 * perkuliahan pada modul akademik, tetapi khusus jenjang sekolah (SD/SMP/SMA). Tampilan utama
 * ({@link #display(JadwalPelajaran, Component)}) berupa tab: daftar pertemuan (dengan status
 * kehadiran, materi, tugas, ujian per pertemuan), deskripsi pembelajaran, dan capaian/kompetensi.
 *
 * <p>
 * Fungsi utama yang disediakan lewat tombol toolbar statis:
 * </p>
 * <ul>
 * <li>{@link #tampilTombolBuatPertemuan} — membangkitkan seluruh pertemuan satu semester
 * sekaligus berdasarkan pola jadwal (hari + jam pelajaran, hingga 5 slot bergantian) dan masa
 * pembelajaran yang dikonfigurasi pada {@code jadwalPelajaran}; menolak dijalankan bila hari/jam/
 * masa belum lengkap diisi di menu jadwal pelajaran.</li>
 * <li>{@link #buatSatuPertemuan} — menambah satu pertemuan tambahan (ad-hoc) pada tanggal/jam
 * pilihan bebas; disembunyikan bagi siswa/mahasiswa, dan bagi guru bila
 * {@code jadwalPelajaran.guruBisaMerubahTanggalJadwalPelajaran} dimatikan.</li>
 * <li>{@link #tampilTombolAturUlangWaktu}/{@link #prosesTampilTombolAturUlangWaktu} — menyusun
 * ulang tanggal seluruh pertemuan dari satu tanggal mulai baru, dengan opsi melewati hari libur
 * nasional.</li>
 * <li>{@link #tampilTombolAmbil} — menyalin isi pembelajaran (materi/catatan, berkas, video,
 * audio, ujian, tugas kelompok/mandiri, deskripsi, capaian) dari {@link JadwalPelajaran} lain
 * (agenda template) ke jadwal ini lewat {@link #copyLampiranPertemuan}; peserta, kehadiran, dan
 * nilai per-siswa SENGAJA TIDAK ikut disalin. Menghasilkan laporan ringkas jumlah item tersalin
 * dan daftar kendala per pertemuan.</li>
 * <li>{@link #tampilTombolHapus} — menghapus seluruh baris {@link Pertemuan} milik jadwal ini.</li>
 * </ul>
 */
public class PenjadwalanSiswaHelper {

	private JadwalPelajaran jadwalPelajaran;
	private MyGrid grid;

	private MyCheckboxConfig hanyaYangAktif;
	private MyCheckboxConfig urutkanManual;

	/** Mem-parsing waktu dari string dengan mencoba dua pola berurutan ({@code HH.mm} lalu {@code HH:mm}), melempar exception parsing pola terakhir bila keduanya gagal. */
	private static Date parseWaktu(String nilai) throws ParseException {
		ParseException terakhir = null;
		String[] pola = new String[] { "HH.mm", "HH:mm" };
		for (int i = 0; i < pola.length; i++) {
			try {
				SimpleDateFormat format = new SimpleDateFormat(pola[i]);
				format.setLenient(false);
				return format.parse(nilai.trim());
			} catch (ParseException e) {
				terakhir = e;
			}
		}
		throw terakhir;
	}

	/** Mengambil status pertemuan default "Tatap Muka" dari cache {@link ConstantValues#TATAP_MUKA} bila tersedia, atau memuat langsung dari database (id 236) sebagai fallback. */
	private static StatusPertemuan ambilStatusPertemuanDefault(Session session) {
		if (ConstantValues.TATAP_MUKA != null) {
			return ConstantValues.TATAP_MUKA;
		}
		return session == null ? null : (StatusPertemuan) session.get(StatusPertemuan.class, Long.valueOf(236L));
	}

	/**
	 * Perender baris grid daftar pertemuan: menampilkan tanggal, jam, status pertemuan, materi/
	 * catatan pembelajaran (dapat disunting inline), dan indikator kelengkapan (berkas, video,
	 * audio, ujian, tugas) per pertemuan, dengan aksi sesuai peran pengguna saat ini
	 * ({@link #tbmuser}).
	 */
	class PertemuanRenderer extends ais.ui.util.MyRowRenderer {

		private Tbmuser tbmuser = Common.getCurrentUser();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, arg1.toString());
			if (pertemuan == null) {
				arg0.setVisible(false);
				return;
			}
			arg0.setAttribute("myValue", pertemuan);

			MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.setOpen(true);

			MyGroupboxStyled tools = new MyGroupboxStyled();
			tools.setWidth("100%");
			tools.setParent(detail);
			tools.setStyleLangsung(
					"text-align:center;border: 1px solid #bdbbbb;padding: 1px 2px 2px 0px;background-color: rgba(255,255,255,0.5);border-radius: 5px 5px 5px 5px;overflow: hidden;box-shadow: 1px 1px 2px #c0c0c0;max-width: 97%;margin:auto;border-width: 1px;");

			Component aa = DashboardTimelinePertemuan.createVideoConrefrence(pertemuan, null, false, new EventListener() {

				@Override
				public void onEvent(Event a) throws Exception {
					try {
						HibernateUtil.currentSession().refresh(pertemuan);
						Common.clear(arg0);
						render(arg0, pertemuan);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						Common.tampilErrorJikaAdmin(e);
					}
				}
			});

			Component bb = AbsensiSiswaHelper.createTombolAbsen(pertemuan, new DataLoader() {

				@Override
				public void loadData(Object value) {
					try {
						HibernateUtil.currentSession().refresh(pertemuan);
						Common.clear(arg0);
						render(arg0, pertemuan);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						Common.tampilErrorJikaAdmin(e);
					}

				}
			});

			MyToolbarbutton a = new MyToolbarbutton("fa-calendar-o", "Agenda");

			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event a) throws Exception {

					CalendarPerkuliahanMingguIniComposer.init(pertemuan, new EventListener() {

						@Override
						public void onEvent(Event a) throws Exception {
							try {
								HibernateUtil.currentSession().refresh(pertemuan);
								Common.clear(arg0);
								render(arg0, pertemuan);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								Common.tampilErrorJikaAdmin(e);
							}
						}
					});
				}
			});

			AktifitasPerkuliahanHelper.createKeteranganData(pertemuan, tbmuser, tbmuser.getMahasiswa(),
					tbmuser.getBiodataCalonMahasiswa(), new DataLoader() {

						@Override
						public void loadData(Object value) {
							try {
								HibernateUtil.currentSession().refresh(pertemuan);
								Common.clear(arg0);
								render(arg0, pertemuan);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								Common.tampilErrorJikaAdmin(e);
							}

						}
					}, true, false, aa, a, bb, DashboardTimelinePertemuan.createScanFoto(tbmuser, pertemuan))
					.setParent(tools);

			VOPembelajaran pembelajaran = pertemuan == null ? null : pertemuan.ambilVOPembelajaran();
			if (pembelajaran != null && !pembelajaran.getUrutkanotomatis()) {
				final Intbox pertemuanManual = new Intbox(pertemuan.getPertemuanManual());
				pertemuanManual.setParent(arg0);
				pertemuanManual.setWidth("90%");
				pertemuanManual.setStyle("font-size:16px;font-weight: bolder;");
				pertemuanManual.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pertemuan.setPertemuanManual(pertemuanManual.getValue());
						Common.refreshSaveOrUpdate(pertemuan);
					}
				});
			} else {
				new MyLabelBolder(pertemuan.getPertemuanKe() + "").setParent(arg0);
			}

			final MyLabelEdit topik = new MyLabelEdit(pertemuan.getTopik());
			topik.setWidth("90%");
			topik.setRows(4);
			topik.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					pertemuan.setTopik(topik.getValue());
					Common.refreshUpdate(pertemuan);
				}
			});
			topik.setParent(arg0);

			final MyLabelEdit indikator = new MyLabelEdit(pertemuan.getIndikator());
			indikator.setWidth("90%");
			indikator.setRows(4);
			indikator.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					pertemuan.setIndikator(indikator.getValue());
					Common.refreshUpdate(pertemuan);
				}
			});
			indikator.setParent(arg0);

			final MyLabelEdit waktupembelajaran = new MyLabelEdit(pertemuan.getWaktupembelajaran());
			waktupembelajaran.setWidth("90%");
			waktupembelajaran.setRows(4);
			waktupembelajaran.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					pertemuan.setWaktupembelajaran(waktupembelajaran.getValue());
					Common.refreshUpdate(pertemuan);
				}
			});
			waktupembelajaran.setParent(arg0);

			final MyLabelEdit pengalamanBelajar = new MyLabelEdit(pertemuan.getPengalamanBelajar());
			pengalamanBelajar.setWidth("90%");
			pengalamanBelajar.setRows(4);
			pengalamanBelajar.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					pertemuan.setPengalamanBelajar(pengalamanBelajar.getValue());
					Common.refreshUpdate(pertemuan);
				}
			});
			pengalamanBelajar.setParent(arg0);

			final MyLabelEdit tugasDanPenilaian = new MyLabelEdit(pertemuan.getTugasDanPenilaian());
			tugasDanPenilaian.setWidth("90%");
			tugasDanPenilaian.setRows(4);
			tugasDanPenilaian.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					pertemuan.setTugasDanPenilaian(tugasDanPenilaian.getValue());
					Common.refreshUpdate(pertemuan);
				}
			});
			tugasDanPenilaian.setParent(arg0);

			final MyLabelEdit bukuRujukan1 = new MyLabelEdit(pertemuan.getBukuRujukan1());
			bukuRujukan1.setWidth("90%");
			bukuRujukan1.setRows(4);
			bukuRujukan1.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					pertemuan.setBukuRujukan1(bukuRujukan1.getValue());
					Common.refreshUpdate(pertemuan);
				}
			});
			bukuRujukan1.setParent(arg0);

			final MyLabelEdit bukuRujukan2 = new MyLabelEdit(pertemuan.getBukuRujukan2());
			bukuRujukan2.setWidth("90%");
			bukuRujukan2.setRows(4);
			bukuRujukan2.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					pertemuan.setBukuRujukan2(bukuRujukan2.getValue());
					Common.refreshUpdate(pertemuan);
				}
			});
			bukuRujukan2.setParent(arg0);

			final MyLabelEdit metodePembelajaran = new MyLabelEdit(pertemuan.getMetodePembelajaran());
			metodePembelajaran.setWidth("90%");
			metodePembelajaran.setRows(4);
			metodePembelajaran.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					pertemuan.setMetodePembelajaran(metodePembelajaran.getValue());
					Common.refreshUpdate(pertemuan);
				}
			});
			metodePembelajaran.setParent(arg0);

			final Combobox combobox = new Combobox();
			Common.insertCombo(combobox, "nama", StatusPertemuan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(combobox, pertemuan.getStatusPertemuan());
			combobox.setWidth("90%");
			combobox.setParent(arg0);
			combobox.setReadonly(true);

			combobox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pertemuan.setStatusPertemuan((StatusPertemuan) combobox.getSelectedItem().getValue());
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (pertemuan));
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setChecked(pertemuan.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pertemuan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(pertemuan);
				}
			});

			final MyDatebox mulai = new MyDatebox();
			mulai.setValue(pertemuan.getTanggal());

			if (tbmuser.getMahasiswa() == null && (tbmuser.ambilGuru() == null
					|| (jadwalPelajaran != null && jadwalPelajaran.getGuruBisaMerubahTanggalJadwalPelajaran()))) {

				Vbox vbox = new Vbox();
				vbox.setParent(arg0);

				mulai.setWidth("90%");
				mulai.setParent(vbox);
				mulai.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pertemuan.setTanggal(mulai.getValue());pertemuan.setTanggalEdit(mulai.getValue());
						Session session = HibernateUtil.currentSession();
						Common.refreshUpdate(session, (pertemuan));
					}
				});

				final Timebox waktuMulai;
				final Timebox waktuSelesai;
				waktuMulai = new ais.ui.util.MyTimebox();
				waktuSelesai = new ais.ui.util.MyTimebox();
				waktuMulai.setFormat(Common.timeFormat.get().toPattern());
				waktuSelesai.setFormat(Common.timeFormat.get().toPattern());

				try {
					waktuMulai.setValue(
							pertemuan.getWaktuMulai() == null || pertemuan.getWaktuMulai().trim().isEmpty() ? null
									: Common.timeFormat2.get().parse(pertemuan.getWaktuMulai()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PenjadwalanSiswaHelper.java:391");

				}
				try {
					waktuSelesai.setValue(
							pertemuan.getWaktuSelesai() == null || pertemuan.getWaktuSelesai().trim().isEmpty() ? null
									: Common.timeFormat2.get().parse(pertemuan.getWaktuSelesai()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PenjadwalanSiswaHelper.java:398");

				}

				EventListener updateLocal = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pertemuan.setWaktuMulai(waktuMulai.getValue() == null ? null
								: Common.timeFormat2.get().format(waktuMulai.getValue()));
						pertemuan.setWaktuSelesai(waktuSelesai.getValue() == null ? null
								: Common.timeFormat2.get().format(waktuSelesai.getValue()));

						Session session = HibernateUtil.currentSession();
						Common.refreshUpdate(session, (pertemuan));
					}
				};

				waktuMulai.setCols(1);
				waktuSelesai.setCols(1);

				waktuMulai.addEventListener("onChange", updateLocal);
				waktuSelesai.addEventListener("onChange", updateLocal);

				Hbox hbox = new Hbox();
				hbox.setParent(vbox);
				waktuMulai.setParent(hbox);
				waktuSelesai.setParent(hbox);

				Hbox toolbar = new Hbox();
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						if (!PenjadwalanHelper.checkBolehHapus(pertemuan)) {
							return;
						}

						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												PertemuanDao pertemuanDao = DaoFactory.getInstance().getPertemuanDao();
												// pertemuanDao.beginTransaction();

												pertemuanDao.delete((pertemuan));

												jadwalPelajaran.belum();

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														onSearchDefault(arg0);
													}
												});
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(
														"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
																+ e.getMessage());
											}

										}

									}
								});

					}
				});
				button.setParent(toolbar);
				toolbar.setParent(arg0);

			} else {
				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				new Label(pertemuan.getTanggal() == null ? "" : Common.dateFormat1.get().format(pertemuan.getTanggal()))
						.setParent(vbox);

				Hbox hbox = new Hbox();
				hbox.setParent(vbox);

				new MyLabelKecil(pertemuan.getWaktuMulai() == null ? "" : pertemuan.getWaktuMulai()).setParent(hbox);
				new MyLabelKecil(pertemuan.getWaktuSelesai() == null ? "" : "s.d " + pertemuan.getWaktuSelesai())
						.setParent(hbox);

				new Label().setParent(arg0);
			}

			arg0.setAttribute("topik", topik);
			arg0.setAttribute("indikator", indikator);
			arg0.setAttribute("waktupembelajaran", waktupembelajaran);
			arg0.setAttribute("pengalamanBelajar", pengalamanBelajar);
			arg0.setAttribute("tugasDanPenilaian", tugasDanPenilaian);
			arg0.setAttribute("buku", bukuRujukan1);
			arg0.setAttribute("ref", bukuRujukan2);
			arg0.setAttribute("metodePembelajaran", metodePembelajaran);
			arg0.setAttribute("statuspertemuan", combobox);
			arg0.setAttribute("date", mulai);
		}

	}

	/** Menambahkan tombol "Hapus" ke {@code toolbar} yang, setelah konfirmasi, menghapus seluruh baris {@link Pertemuan} milik {@code jadwalPelajaran} lewat SQL native lalu memanggil {@code eventListener}. */
	public static void tampilTombolHapus(Toolbar toolbar, final JadwalPelajaran jadwalPelajaran,
			final EventListener eventListener) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus semua pertemuan ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {
										Session session = HibernateUtil.currentSession();

										if (jadwalPelajaran != null) {
											session.createSQLQuery("delete from pertemuan where jadwal_pelajaran="
													+ jadwalPelajaran.getId()).executeUpdate();
										}

										eventListener.onEvent(event);
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig.show(
												"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
														+ e.getMessage());
									}

								}

							}
						});

			}
		});
		button.setParent(toolbar);
	}

	/** Menambahkan tombol "Ubah Tanggal Mulai" ke {@code toolbar} yang membuka dialog {@link #prosesTampilTombolAturUlangWaktu}. */
	public static void tampilTombolAturUlangWaktu(Toolbar toolbar, final JadwalPelajaran jadwalPelajaran,
			final EventListener eventListener) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ubah Tanggal Mulai", "/img/svg/edit-box-line.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				prosesTampilTombolAturUlangWaktu(jadwalPelajaran, eventListener);

			}
		});
		button.setParent(toolbar);
	}

	/**
	 * Menambahkan tombol "Buat Pertemuan" ke {@code toolbar} yang membangkitkan seluruh pertemuan
	 * satu semester untuk {@code jadwalPelajaran} sekaligus, berdasarkan pola jadwal (hari + jam
	 * pelajaran, mendukung hingga 5 slot bergantian) dan masa pembelajaran yang sudah dikonfigurasi.
	 * Menolak dijalankan (menampilkan peringatan) bila jam pelajaran, hari, atau masa jadwal
	 * pelajaran belum lengkap diisi.
	 */
	public static void tampilTombolBuatPertemuan(Component toolbar, final JadwalPelajaran jadwalPelajaran,
			final EventListener eventListener) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Buat Pertemuan", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (jadwalPelajaran.getJamPelajaran() == null && jadwalPelajaran.getJamPelajaran2() != null
						&& jadwalPelajaran.getJamPelajaran3() != null && jadwalPelajaran.getJamPelajaran4() != null
						&& jadwalPelajaran.getJamPelajaran5() != null) {
					MyMessageboxConfig.show("Jam pelajaran belum dipilih di menu jadwal pelajaran", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				if (jadwalPelajaran.getHari() == null && jadwalPelajaran.getHari2() != null
						&& jadwalPelajaran.getHari3() != null && jadwalPelajaran.getHari4() != null
						&& jadwalPelajaran.getHari5() != null) {
					MyMessageboxConfig.show("Hari pelajaran belum dipilih di menu jadwal pelajaran", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				if (jadwalPelajaran.getMasaJadwalPelajaran() == null) {
					MyMessageboxConfig.show("Masa pembelajaran belum dipilih di menu jadwal pelajaran", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				final Window window = new Window();
				window.setHeight("95%");
				window.setWidth("90%");
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);
				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("15%");

				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jadwal Pelajaran"));
				row.appendChild(new ais.ui.util.MyLabelBoldAja(jadwalPelajaran.info()));

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Pendahuluan"));
				final MyCkEditor pendahuluan;
				row.appendChild(pendahuluan = new MyCkEditor());
				pendahuluan.setValue(jadwalPelajaran.getPendahuluan());
				pendahuluan.setHeight("200px");
				pendahuluan.setWidth("90%");

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi Pembelajaran"));
				final MyTextbox deskripsiPembelajaran;
				row.appendChild(deskripsiPembelajaran = new MyTextbox(jadwalPelajaran.getDeskripsiPembelajaran()));
				deskripsiPembelajaran.setRows(3);
				deskripsiPembelajaran.setWidth("90%");

				Common.initKeterangan(rows,
						"Contoh untuk mata pelajaran Matematika : Tujuan utama dari mata pelajaran ini adalah membekali siswa dengan berbagai kemampuan ilmu berhitung ........");

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Capaian / Kompetensi"));
				final MyTextbox kompetensi;
				row.appendChild(kompetensi = new MyTextbox(jadwalPelajaran.getCapaianPembelajaranProdi()));
				kompetensi.setRows(2);
				kompetensi.setWidth("90%");

				Common.initKeterangan(rows,
						"Contoh untuk mata pelajaran Matematika : Siswa memiliki pemahaman mengenai konsep dasar menghitung ........");

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran"));
				Hbox hbox = new Hbox();
				hbox.setParent(row);
				Hbox hbox1 = new Hbox();
				hbox1.setParent(hbox);
				LampiranLain.createDownloadUploadFileLain(hbox1, jadwalPelajaran.getId(), LampiranLain.PEMBELAJARAN,
						"Rencana Pembelajaran", false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						}, null, false, false, false, true);

				Common.initKeterangan(rows,
						"Berupa file rencana pembelajaran, file ini tidak harus diupload, namun sangat dianjurkan diupload, sehingga semua siswa yang mengikuti pelajaran dapat melihat rencana pembelajaran selama satu semester");

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai *"));
				final MyDatebox tanggalMulai;
				row.appendChild(tanggalMulai = new MyDatebox(jadwalPelajaran.getTanggalMulaiJadwalPelajaran()));
				if (tanggalMulai != null) tanggalMulai.setDisabled(true);

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				final MyCheckboxConfig lewatiTanggalMerahNasional = new MyCheckboxConfig(
						"Lewati tanggal merah / hari libur");
				lewatiTanggalMerahNasional.setChecked(jadwalPelajaran.getLewatiTanggalMerahNasional());
				row.appendChild(lewatiTanggalMerahNasional);

				row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Hapus Pertemuan"));
				final MyCheckboxConfig hapus;
				row.appendChild(hapus = new MyCheckboxConfig("Hapus pertemuan yang sebelumnya sudah ada"));

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
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						if (tanggalMulai.getValue() == null) {
							MyMessageboxConfig.show("Tanggal Mulai harus diisi", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return;
						}

						Session session = HibernateUtil.currentSession();
						jadwalPelajaran.setLewatiTanggalMerahNasional(lewatiTanggalMerahNasional.isChecked());
						jadwalPelajaran.setTanggalMulaiJadwalPelajaran(tanggalMulai.getValue());
						jadwalPelajaran.setDeskripsiPembelajaran(deskripsiPembelajaran.getValue());
						jadwalPelajaran.setPendahuluan(pendahuluan.getValue());
						jadwalPelajaran.setCapaianPembelajaranProdi(kompetensi.getValue());
						Common.refreshUpdate(jadwalPelajaran);

						if (hapus.isChecked()) {
							session.createSQLQuery(
									"delete from pertemuan where jadwal_pelajaran=" + jadwalPelajaran.getId())
									.executeUpdate();
						}

						Date currDate = tanggalMulai.getValue();

						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.setTime(currDate);

						Calendar s = ais.ui.util.WaktuUtil.getCalendar();
						s.setTime(jadwalPelajaran.getMasaJadwalPelajaran().getSampai());

						List<String> haris = jadwalPelajaran.populateHari();
//						List<JamPelajaran> waktus = jadwalPelajaran.populateWaktu();

						int i = 0;
						while (currDate.before(s.getTime())) {

							if (!lewatiTanggalMerahNasional.isChecked()
									|| !Common.isHolidayMerahDanAtauHariLibur(currDate)) {
								Integer hari = calendar.get(Calendar.DAY_OF_WEEK);

								if (haris.contains(Common.haris[hari - 1])) {

									i++;
									Pertemuan pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.eq("tanggal", currDate))
											.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran)).setMaxResults(1)
											.uniqueResult();
									if (pertemuan == null) {

										pertemuan = new Pertemuan();
										pertemuan.setPertemuanKe(i);

										StatusPertemuan statusDefault = ambilStatusPertemuanDefault(session);
										if (statusDefault == null) {
											throw new IllegalStateException("Status pertemuan Tatap Muka belum tersedia.");
										}
										pertemuan.setStatusPertemuan(statusDefault);

										pertemuan.setTanggal(currDate);
										pertemuan.setJadwalPelajaran(jadwalPelajaran);
										pertemuan.setRuang(jadwalPelajaran.getRuang());
										if (jadwalPelajaran.getJamPelajaran() != null) {
											pertemuan.setWaktuMulai(jadwalPelajaran.getJamPelajaran().getMulaiS());
											pertemuan.setWaktuSelesai(jadwalPelajaran.getJamPelajaran().getSampaiS());
										}

										List<JamPelajaran> waktus = jadwalPelajaran
												.populateWaktu(Common.haris[hari - 1]);

										Date m = null;
										Date sa = null;

										for (JamPelajaran waktua : waktus) {
											if (m == null || m.after(waktua.getMulai())) {
												m = waktua.getMulai();
											}
											if (waktua.getSelesai() != null
													&& (sa == null || sa.before(waktua.getSelesai()))) {
												sa = waktua.getSelesai();
											}
										}

										String waktu = Common.timeFormat2.get().format(m);
										String sampai = sa == null ? null : Common.timeFormat2.get().format(sa);
										pertemuan.setWaktuMulai(waktu);
										pertemuan.setWaktuSelesai(sampai);

										pertemuan.setTanggal(currDate);
										pertemuan.setMulai(currDate);
										pertemuan.setSelesai(null);

										Common.refreshSaveOrUpdate(session, pertemuan);

									}
								}
							}
							calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
							currDate = calendar.getTime();

						}
						window.detach();

						Common.createDefaultTimer(eventListener);
					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});
		button.setParent(toolbar);
	}

	/**
	 * Menambahkan tombol "Ambil / Copy dari agenda lain" ke {@code toolbar}: membuka dialog
	 * pemilihan {@link JadwalPelajaran} template ({@link AmbilDataTemplatePembelajaran}), lalu
	 * menyalin deskripsi pembelajaran, pendahuluan, capaian pembelajaran, dan seluruh isi
	 * pertemuan (materi/catatan, berkas, video, audio, ujian, tugas — lewat
	 * {@link #copyLampiranPertemuan(Pertemuan, Pertemuan)}) dari jadwal sumber ke
	 * {@code jadwalPelajaran} saat ini. Peserta, kehadiran, dan nilai per-siswa TIDAK ikut disalin.
	 * Menyusun dan menawarkan unduhan laporan teks ringkas berisi jumlah item tersalin per
	 * kategori dan daftar kendala yang terjadi per pertemuan.
	 */
	public static void tampilTombolAmbil(Component toolbar, final JadwalPelajaran jadwalPelajaran,
			final DataLoader dataLoader) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil / Copy dari agenda lain", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				AmbilDataTemplatePembelajaran window = new AmbilDataTemplatePembelajaran(jadwalPelajaran);

				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("90%");
				window.setHeight("97%");

				window.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						List<GeneralValueObject> templatePembelajarans = (List<GeneralValueObject>) arg0.getData();

						if (templatePembelajarans != null) {
							Session session = HibernateUtil.currentSession();

							// --- Laporan hasil salin agenda (diunduh .txt setelah selesai) ---
							StringBuilder laporan = new StringBuilder();
							laporan.append("========================================\n");
							laporan.append("   LAPORAN SALIN AGENDA PEMBELAJARAN (SISWA)\n");
							laporan.append("========================================\n");
							laporan.append("Waktu proses : ")
									.append(Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate())).append("\n");
							if (jadwalPelajaran != null) {
								laporan.append("Jadwal Pel.  : ID ").append(jadwalPelajaran.getId()).append("\n");
							}
							laporan.append("Catatan      : Peserta (siswa/guru), kehadiran, dan nilai per-peserta "
									+ "TIDAK ikut disalin.\n");
							laporan.append("----------------------------------------\n\n");
							int totPertemuan = 0;
							int totDilewati = 0;
							int totMateri = 0;
							int totFile = 0;
							int totVideo = 0;
							int totAudio = 0;
							int totUjian = 0;
							int totTgsKel = 0;
							int totTgsMandiri = 0;
							int totKendala = 0;

							for (GeneralValueObject templatePembelajaran : templatePembelajarans) {
								JadwalPelajaran copyJadwalPelajaran = (JadwalPelajaran) templatePembelajaran;
								if (jadwalPelajaran != null) {
									jadwalPelajaran
											.setDeskripsiPembelajaran(copyJadwalPelajaran.getDeskripsiPembelajaran());
									jadwalPelajaran.setPendahuluan(copyJadwalPelajaran.getPendahuluan());
									jadwalPelajaran.setCapaianPembelajaranProdi(
											copyJadwalPelajaran.getCapaianPembelajaranProdi());
									Common.refreshSaveOrUpdate(session, jadwalPelajaran);
								}

								List<Pertemuan> pertemuans = session.createCriteria(Pertemuan.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))

										.add(copyJadwalPelajaran == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("jadwalPelajaran", copyJadwalPelajaran))

										.addOrder(Order.asc("tanggal")).addOrder(Order.asc("id")).list();
								for (Pertemuan pertemuan : pertemuans) {
									Pertemuan pertemuanBaru = (Pertemuan) session.createCriteria(Pertemuan.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.eq("copyDariPertemuan", pertemuan.getId()))

											.add(jadwalPelajaran == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("jadwalPelajaran", jadwalPelajaran))

											.setMaxResults(1).uniqueResult();
									if (pertemuanBaru == null) {
										pertemuanBaru = new Pertemuan();
										pertemuanBaru.setIndikator(pertemuan.getIndikator());
										pertemuanBaru.setWaktupembelajaran(pertemuan.getWaktupembelajaran());
										pertemuanBaru.setPengalamanBelajar(pertemuan.getPengalamanBelajar());
										pertemuanBaru.setTugasDanPenilaian(pertemuan.getTugasDanPenilaian());
										pertemuanBaru.setMandiri(pertemuan.getMandiri());
										pertemuanBaru.setCopyDariPertemuan(pertemuan.getId());
										pertemuanBaru.setStatusPertemuan(pertemuan.getStatusPertemuan());
										pertemuanBaru.setTanggal(pertemuan.getTanggal());
										pertemuanBaru.setJadwalPelajaran(jadwalPelajaran);

										pertemuanBaru.setTopik(pertemuan.getTopik());
										pertemuanBaru.setRuang(pertemuan.getRuang());
										pertemuanBaru.setWaktuMulai(pertemuan.getWaktuMulai());
										pertemuanBaru.setWaktuSelesai(pertemuan.getWaktuSelesai());
										pertemuanBaru.setBukuRujukan1(pertemuan.getBukuRujukan1());
										pertemuanBaru.setBukuRujukan2(pertemuan.getBukuRujukan2());
										pertemuanBaru.setCatatan(pertemuan.getCatatan());
										pertemuanBaru.setGuruTamu(pertemuan.getGuruTamu());
										pertemuanBaru.setGuruTamu2(pertemuan.getGuruTamu2());
										pertemuanBaru.setIsitugas(pertemuan.getIsitugas());
										pertemuanBaru.setMetodePembelajaran(pertemuan.getMetodePembelajaran());
										pertemuanBaru.setMulai(pertemuan.getMulai());
										pertemuanBaru.setSelesai(pertemuan.getSelesai());
										pertemuanBaru.setPertemuanKe(pertemuan.getPertemuanKe());

										try {
											Common.refreshSaveOrUpdate(session, pertemuanBaru);

											ais.action.master.helper.PenjadwalanHelper.HasilSalinPertemuan hsl = copyLampiranPertemuan(
													pertemuan, pertemuanBaru);
											totPertemuan++;
											totMateri += hsl.materi;
											totFile += hsl.file;
											totVideo += hsl.video;
											totAudio += hsl.audio;
											totUjian += hsl.ujian;
											totTgsKel += hsl.tugasKelompok;
											totTgsMandiri += hsl.tugasMandiri;
											laporan.append("Pertemuan ke-").append(pertemuan.getPertemuanKe())
													.append(" (agenda lama #").append(pertemuan.getId())
													.append(" → baru #").append(pertemuanBaru.getId()).append(")\n");
											laporan.append("   materi=").append(hsl.materi).append(", file=")
													.append(hsl.file).append(", video=").append(hsl.video)
													.append(", audio=").append(hsl.audio).append(", ujian=")
													.append(hsl.ujian).append(", tugas kelompok=").append(hsl.tugasKelompok)
													.append(", tugas mandiri=").append(hsl.tugasMandiri).append("\n");
											for (String k : hsl.kendala) {
												laporan.append("   [KENDALA] ").append(k).append("\n");
												totKendala++;
											}
											laporan.append("\n");
										} catch (Exception ePert) {
											totKendala++;
											laporan.append("Pertemuan ke-").append(pertemuan.getPertemuanKe())
													.append(" (agenda lama #").append(pertemuan.getId())
													.append("): GAGAL dibuat — ")
													.append(ais.action.master.helper.PenjadwalanHelper.pesanError(ePert))
													.append("\n\n");
											ais.common.ErrorAuditUtil.record(ePert, "salin-agenda-siswa buat-pertemuan");
										}

									} else {
										totDilewati++;
										laporan.append("Pertemuan ke-").append(pertemuan.getPertemuanKe())
												.append(" (agenda lama #").append(pertemuan.getId())
												.append("): sudah pernah disalin — DILEWATI.\n\n");
									}

								}

								if (copyJadwalPelajaran != null) {
									List<JadwalPelajaranPunyaItem> jadwalPelajaranPunyaItems = session
											.createCriteria(JadwalPelajaranPunyaItem.class).addOrder(Order.asc("id"))

											.add(copyJadwalPelajaran == null ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("jadwalPelajaran", copyJadwalPelajaran))

											.list();
									for (JadwalPelajaranPunyaItem c : jadwalPelajaranPunyaItems) {
										JadwalPelajaranPunyaItem jadwalPelajaranPunyaItem = (JadwalPelajaranPunyaItem) session
												.createCriteria(JadwalPelajaranPunyaItem.class)
												.add(Restrictions.eq("item", c.getItem()))
												.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran))
												.setMaxResults(1).uniqueResult();
										if (jadwalPelajaranPunyaItem == null) {
											jadwalPelajaranPunyaItem = new JadwalPelajaranPunyaItem();
											jadwalPelajaranPunyaItem.setItem(c.getItem());
											jadwalPelajaranPunyaItem.setJadwalPelajaran(jadwalPelajaran);
											Common.refreshSaveOrUpdate(session, jadwalPelajaranPunyaItem);
										}

									}
								}

							}

							// --- Ringkasan + unduh laporan .txt ---
							laporan.append("========================================\n");
							laporan.append("   RINGKASAN\n");
							laporan.append("========================================\n");
							laporan.append("Pertemuan baru dibuat : ").append(totPertemuan).append("\n");
							laporan.append("Pertemuan dilewati    : ").append(totDilewati)
									.append(" (sudah pernah disalin)\n");
							laporan.append("Materi/catatan         : ").append(totMateri).append("\n");
							laporan.append("File materi            : ").append(totFile).append("\n");
							laporan.append("Video materi           : ").append(totVideo).append("\n");
							laporan.append("Audio materi           : ").append(totAudio).append("\n");
							laporan.append("Ujian                  : ").append(totUjian).append("\n");
							laporan.append("Tugas kelompok         : ").append(totTgsKel).append("\n");
							laporan.append("Tugas mandiri          : ").append(totTgsMandiri).append("\n");
							laporan.append("Total kendala          : ").append(totKendala).append("\n");
							if (totKendala == 0) {
								laporan.append("\nSemua item berhasil disalin tanpa kendala.\n");
							} else {
								laporan.append("\nTerdapat ").append(totKendala)
										.append(" kendala — lihat rincian [KENDALA] per pertemuan di atas.\n");
							}

							try {
								String namaFile = "laporan_salin_agenda_siswa_"
										+ Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()) + ".txt";
								org.zkoss.zul.Filedownload.save(laporan.toString().getBytes("UTF-8"), "text/plain",
										namaFile);
							} catch (Exception eDl) {
								ais.common.ErrorAuditUtil.record(eDl, "salin-agenda-siswa unduh-laporan");
							}

							try {
								ais.ui.util.MyMessageboxConfig.show(
										"Salin agenda selesai: " + totPertemuan + " pertemuan dibuat"
												+ (totDilewati > 0 ? ", " + totDilewati + " dilewati" : "")
												+ (totKendala > 0 ? ". Terdapat " + totKendala
														+ " kendala — lihat berkas laporan yang terunduh."
														: " tanpa kendala.")
												+ "\n\nBerikutnya: atur tanggal mulai & interval pertemuan.",
										"Selesai", ais.ui.util.MyMessageboxConfig.OK,
										ais.ui.util.MyMessageboxConfig.INFORMATION);
							} catch (Exception eMsg) {
								ais.common.ErrorAuditUtil.record(eMsg, "salin-agenda-siswa ringkasan");
							}

							try {
								prosesTampilTombolAturUlangWaktu(jadwalPelajaran, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										dataLoader.loadData(null);
									}
								});
							} catch (Exception e) {
								dataLoader.loadData(null);
							}

						}

					}
				});

				window.onModal();
			}

		});
		button.setParent(toolbar);
	}

	/**
	 * Membangun dan menampilkan dialog "Atur Tanggal & Interval Pertemuan": tanggal mulai baru,
	 * opsi melewati tanggal merah/hari libur nasional, dan jenis pengaturan ulang interval, lalu
	 * menyusun ulang tanggal seluruh pertemuan {@code jadwalPelajaran} sesuai pilihan tersebut.
	 */
	public static void prosesTampilTombolAturUlangWaktu(final JadwalPelajaran jadwalPelajaran,
			final EventListener eventListener) throws Exception {

		final Window window = new Window();
		window.setTitle("Atur Tanggal & Interval Pertemuan");
		window.setBorder("normal");
		window.setClosable(true);
		window.setHeight("600px");
		window.setWidth("92%");
		window.setContentStyle("overflow:auto;");
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		center.setAutoscroll(true);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		final MyDatebox tanggalMulai;
		row.appendChild(tanggalMulai = new MyDatebox(
				jadwalPelajaran == null ? null : jadwalPelajaran.getTanggalMulaiJadwalPelajaran()));

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyCheckboxConfig lewatiTanggalMerahNasional = new MyCheckboxConfig("Lewati tanggal merah / hari libur");
		lewatiTanggalMerahNasional
				.setChecked(jadwalPelajaran == null ? true : jadwalPelajaran.getLewatiTanggalMerahNasional());
		row.appendChild(lewatiTanggalMerahNasional);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis"));
		final Radiogroup jenis;
		row.appendChild(jenis = new Radiogroup());

		jenis.appendChild(new MyRadioConfig("Harian"));
		jenis.appendChild(new MyRadioConfig("Tgl Ganjil"));
		jenis.appendChild(new MyRadioConfig("Tgl Genap"));
		jenis.appendChild(new MyRadioConfig("2 Harian"));
		jenis.appendChild(new MyRadioConfig("3 Harian"));
		jenis.appendChild(new MyRadioConfig("4 Harian"));
		jenis.appendChild(new MyRadioConfig("5 Harian"));
		jenis.appendChild(new MyRadioConfig("6 Harian"));
		MyRadioConfig minggu;
		jenis.appendChild(minggu = new MyRadioConfig("Mingguan"));
		minggu.setChecked(true);
		jenis.appendChild(new MyRadioConfig("2 Mingguan"));
		jenis.appendChild(new MyRadioConfig("3 Mingguan"));
		jenis.appendChild(new MyRadioConfig("4 Mingguan"));
		jenis.appendChild(new MyRadioConfig("Bulanan"));

		@SuppressWarnings("unchecked")
		List<MyRadioConfig> myRadioConfigs = jenis.getChildren();
		for (MyRadioConfig s : myRadioConfigs) {
			if (s.getLabel().equalsIgnoreCase(jadwalPelajaran.getJenis())) {
				s.setChecked(true);
				break;
			}
		}

		// --- Pratinjau daftar tanggal (live) ---
		MyFormRow rowPratinjau = new MyFormRow();
		rowPratinjau.setValign("top");
		rowPratinjau.setParent(rows);
		rowPratinjau.appendChild(new ais.ui.util.MyLabelConfig("Pratinjau Tanggal"));
		final org.zkoss.zul.Div pratinjauBox = new org.zkoss.zul.Div();
		pratinjauBox.setStyle("max-height:300px;overflow:auto;border:1px solid #e2e8f0;border-radius:8px;"
				+ "padding:6px 10px;background:#f8fafc;");
		rowPratinjau.appendChild(pratinjauBox);

		final EventListener refreshPratinjau = new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				try {
					Common.clear(pratinjauBox);
					if (tanggalMulai.getValue() == null || jenis.getSelectedItem() == null) {
						pratinjauBox.appendChild(new ais.ui.util.MyHtml(
								"<div style='color:#94a3b8;'>Pilih Tanggal Mulai &amp; Jenis interval untuk melihat pratinjau.</div>"));
						return;
					}
					Session s = HibernateUtil.currentSession();
					java.util.List<Pertemuan> daftar = s.createCriteria(Pertemuan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(jadwalPelajaran == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("jadwalPelajaran", jadwalPelajaran))
							.addOrder(Order.asc("tanggal")).addOrder(Order.asc("id")).list();
					if (daftar == null || daftar.isEmpty()) {
						pratinjauBox.appendChild(new ais.ui.util.MyHtml(
								"<div style='color:#94a3b8;'>Belum ada pertemuan untuk dijadwalkan.</div>"));
						return;
					}
					java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE, dd-MM-yyyy",
							new java.util.Locale("id"));
					boolean skip = lewatiTanggalMerahNasional.isChecked();
					// Replikasi logika Simpan siswa: pertemuan pertama = Tanggal Mulai (tanpa skip);
					// berikutnya = maju sesuai interval lalu lewati hari libur (bila dicentang).
					java.util.Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
					java.util.Date cur = tanggalMulai.getValue();
					StringBuilder sb = new StringBuilder();
					sb.append("<div style='font-size:11px;color:#334155;margin-bottom:4px;'>").append(daftar.size())
							.append(" pertemuan &middot; ")
							.append(ais.ui.util.DashboardUiKit.esc(jenis.getSelectedItem().getLabel()))
							.append(skip ? " &middot; lewati hari libur" : " &middot; TANPA lewati libur").append("</div>");
					int no = 0;
					for (Pertemuan p : daftar) {
						no++;
						boolean libur = false;
						try {
							libur = Common.isHolidayMerahDanAtauHariLibur(cur);
						} catch (Exception e) {
							libur = false;
						}
						int ke = p.getPertemuanKe() == null ? no : p.getPertemuanKe();
						String topik = p.getTopik() == null ? "" : p.getTopik().trim();
						if (topik.length() > 40) {
							topik = topik.substring(0, 40) + "…";
						}
						sb.append("<div style='padding:3px 0;border-bottom:1px dashed #e2e8f0;'>")
								.append("<b>Pertemuan ke-").append(ke).append("</b> : ")
								.append("<span style='color:").append(libur ? "#dc2626" : "#0f172a").append(";'>")
								.append(sdf.format(cur)).append(libur ? " (libur)" : "").append("</span>");
						if (topik.length() > 0) {
							sb.append(" <span style='color:#64748b;font-size:11px;'>— ")
									.append(ais.ui.util.DashboardUiKit.esc(topik)).append("</span>");
						}
						sb.append("</div>");
						// hitung tanggal pertemuan berikutnya
						cal.setTime(cur);
						cal = Common.curreDate(jenis, cal);
						if (skip) {
							int guard = 0;
							while (Common.isHolidayMerahDanAtauHariLibur(cal.getTime()) && guard++ < 400) {
								cal = Common.curreDate(jenis, cal);
							}
						}
						cur = cal.getTime();
					}
					pratinjauBox.appendChild(new ais.ui.util.MyHtml(sb.toString()));
				} catch (Exception e) {
					pratinjauBox.appendChild(new ais.ui.util.MyHtml(
							"<div style='color:#dc2626;'>Gagal membuat pratinjau: "
									+ ais.ui.util.DashboardUiKit.esc(ais.action.master.helper.PenjadwalanHelper.pesanError(e))
									+ "</div>"));
					ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PenjadwalanSiswaHelper.pratinjauTanggal");
				}
			}
		};
		tanggalMulai.addEventListener("onChange", refreshPratinjau);
		jenis.addEventListener("onCheck", refreshPratinjau);
		lewatiTanggalMerahNasional.addEventListener("onCheck", refreshPratinjau);
		try {
			refreshPratinjau.onEvent(null);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PenjadwalanSiswaHelper.pratinjau-init");
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
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				if (tanggalMulai.getValue() == null) {
					MyMessageboxConfig.show("Tanggal Mulai harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				Session session = HibernateUtil.currentSession();

				if (jadwalPelajaran != null) {
					jadwalPelajaran.setLewatiTanggalMerahNasional(lewatiTanggalMerahNasional.isChecked());
					jadwalPelajaran.setTanggalMulaiJadwalPelajaran(tanggalMulai.getValue());
					jadwalPelajaran.setJenis(jenis.getSelectedItem().getLabel());
					Common.refreshUpdate(jadwalPelajaran);
				}

				List<Pertemuan> pertemuans = session.createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("tanggal")).addOrder(Order.asc("id"))

						.add(jadwalPelajaran == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jadwalPelajaran", jadwalPelajaran))

						.list();

				Date currDate = tanggalMulai.getValue();

				for (final Pertemuan pertemuan : pertemuans) {
					pertemuan.setTanggal(currDate);

					pertemuan.setMulai(currDate);
					pertemuan.setSelesai(null);
					if (jadwalPelajaran != null && jadwalPelajaran.getJamPelajaran() != null) {
						pertemuan.setWaktuMulai(jadwalPelajaran.getJamPelajaran().getMulaiS());
						pertemuan.setWaktuSelesai(jadwalPelajaran.getJamPelajaran().getSampaiS());
					}
					Common.refreshSaveOrUpdate(session, pertemuan);
					Calendar myCalendar = ais.ui.util.WaktuUtil.getCalendar();
					myCalendar.setTime(currDate);

					if (jenis.getSelectedItem().getLabel().equals("Tgl Ganjil")) {
						myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);

						int date = myCalendar.get(Calendar.DATE);
						if (date % 2 == 0) {
							myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
							date = myCalendar.get(Calendar.DATE);
							if (date % 2 == 0) {
								myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
								date = myCalendar.get(Calendar.DATE);
							}
						}

					} else if (jenis.getSelectedItem().getLabel().equals("Tgl Genap")) {
						myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);

						int date = myCalendar.get(Calendar.DATE);
						if (date % 2 == 1) {
							myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
							date = myCalendar.get(Calendar.DATE);
							if (date % 2 == 1) {
								myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
								date = myCalendar.get(Calendar.DATE);
							}
						}

					} else if (jenis.getSelectedItem().getLabel().equals("Harian")) {
						myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
					} else if (jenis.getSelectedItem().getLabel().equals("2 Harian")) {
						myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 2);
					} else if (jenis.getSelectedItem().getLabel().equals("3 Harian")) {
						myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 3);
					} else if (jenis.getSelectedItem().getLabel().equals("4 Harian")) {
						myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 4);
					} else if (jenis.getSelectedItem().getLabel().equals("5 Harian")) {
						myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 5);
					} else if (jenis.getSelectedItem().getLabel().equals("6 Harian")) {
						myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 6);
					} else if (jenis.getSelectedItem().getLabel().equals("Mingguan")) {
						myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 7);
					} else if (jenis.getSelectedItem().getLabel().equals("2 Mingguan")) {
						myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 14);
					} else if (jenis.getSelectedItem().getLabel().equals("3 Mingguan")) {
						myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 21);
					} else if (jenis.getSelectedItem().getLabel().equals("4 Mingguan")) {
						myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 28);
					} else if (jenis.getSelectedItem().getLabel().equals("Bulanan")) {
						myCalendar.set(Calendar.MONTH, myCalendar.get(Calendar.MONTH) + 1);
					}
					currDate = myCalendar.getTime();

					if (lewatiTanggalMerahNasional.isChecked()) {
						while (Common.isHolidayMerahDanAtauHariLibur(currDate)) {
							if (jenis.getSelectedItem().getLabel().equals("Tgl Ganjil")) {
								myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);

								int date = myCalendar.get(Calendar.DATE);
								if (date % 2 == 0) {
									myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
									date = myCalendar.get(Calendar.DATE);
									if (date % 2 == 0) {
										myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
										date = myCalendar.get(Calendar.DATE);
									}
								}

							} else if (jenis.getSelectedItem().getLabel().equals("Tgl Genap")) {
								myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);

								int date = myCalendar.get(Calendar.DATE);
								if (date % 2 == 1) {
									myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
									date = myCalendar.get(Calendar.DATE);
									if (date % 2 == 1) {
										myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
										date = myCalendar.get(Calendar.DATE);
									}
								}

							} else if (jenis.getSelectedItem().getLabel().equals("Harian")) {
								myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 1);
							} else if (jenis.getSelectedItem().getLabel().equals("2 Harian")) {
								myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 2);
							} else if (jenis.getSelectedItem().getLabel().equals("3 Harian")) {
								myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 3);
							} else if (jenis.getSelectedItem().getLabel().equals("4 Harian")) {
								myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 4);
							} else if (jenis.getSelectedItem().getLabel().equals("5 Harian")) {
								myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 5);
							} else if (jenis.getSelectedItem().getLabel().equals("6 Harian")) {
								myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 6);
							} else if (jenis.getSelectedItem().getLabel().equals("Mingguan")) {
								myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 7);
							} else if (jenis.getSelectedItem().getLabel().equals("2 Mingguan")) {
								myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 14);
							} else if (jenis.getSelectedItem().getLabel().equals("3 Mingguan")) {
								myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 21);
							} else if (jenis.getSelectedItem().getLabel().equals("4 Mingguan")) {
								myCalendar.set(Calendar.DATE, myCalendar.get(Calendar.DATE) + 28);
							} else if (jenis.getSelectedItem().getLabel().equals("Bulanan")) {
								myCalendar.set(Calendar.MONTH, myCalendar.get(Calendar.MONTH) + 1);
							}
							currDate = myCalendar.getTime();
						}
					}
				}
				window.detach();

				Common.createDefaultTimer(eventListener);
			}
		});
		save.setParent(toolbar);

		window.onModal();

	}

	/**
	 * Menyalin satu lampiran bertipe {@code jenis} (mis. {@link LampiranLain#CATATAN_PERKULIAHAN})
	 * dari {@code pertemuan} ke {@code pertemuanBaru}, hanya bila lampiran sumber ada dan
	 * {@code pertemuanBaru} belum memiliki lampiran jenis tersebut (menghindari duplikasi bila
	 * dipanggil berulang). Lampiran baru dibuat sebagai referensi salinan ({@code copyDari}), bukan
	 * duplikasi fisik data.
	 */
	public static void copyLampiranPertemuan(Pertemuan pertemuan, Pertemuan pertemuanBaru, String jenis) {
		ais.action.master.helper.PenjadwalanHelper.copyLampiranPertemuan(pertemuan, pertemuanBaru, jenis);
	}
	/**
	 * Menyalin seluruh isi pembelajaran satu {@code pertemuan} ke {@code pertemuanBaru}: materi/
	 * catatan pembelajaran (lewat {@link #copyLampiranPertemuan(Pertemuan, Pertemuan, String)}),
	 * berkas/video/audio materi ({@link PertemuanFileContent}, {@link VideoPertemuan},
	 * {@link AudioPertemuan} — masing-masing item disalin dalam transaksi sendiri agar satu
	 * kegagalan tidak menggagalkan item lain), serta ujian dan tugas terkait pertemuan tersebut.
	 * Peserta, kehadiran, dan nilai per-siswa TIDAK ikut disalin.
	 *
	 * @param pertemuan    pertemuan sumber
	 * @param pertemuanBaru pertemuan tujuan
	 * @return ringkasan {@link ais.action.master.helper.PenjadwalanHelper.HasilSalinPertemuan} berisi jumlah item tersalin per kategori dan daftar pesan kendala
	 */
	@SuppressWarnings("unchecked")
	public static ais.action.master.helper.PenjadwalanHelper.HasilSalinPertemuan copyLampiranPertemuan(
			Pertemuan pertemuan, Pertemuan pertemuanBaru) {
		final ais.action.master.helper.PenjadwalanHelper.HasilSalinPertemuan hasil = new ais.action.master.helper.PenjadwalanHelper.HasilSalinPertemuan();

		// 1) Materi / catatan pembelajaran.
		try {
			LampiranLain sebelum = LampiranLain.ambil(pertemuanBaru.getId(), LampiranLain.CATATAN_PERKULIAHAN);
			copyLampiranPertemuan(pertemuan, pertemuanBaru, LampiranLain.CATATAN_PERKULIAHAN);
			LampiranLain sesudah = LampiranLain.ambil(pertemuanBaru.getId(), LampiranLain.CATATAN_PERKULIAHAN);
			if (sebelum == null && sesudah != null) {
				hasil.materi++;
			}
		} catch (Exception e) {
			hasil.kendala.add("Materi/catatan pembelajaran: " + ais.action.master.helper.PenjadwalanHelper.pesanError(e));
			ais.common.ErrorAuditUtil.record(e, "salin-agenda-siswa materi p" + pertemuanBaru.getId());
		}

		// 2) File / Video / Audio materi (transaksi per item).
		Session lampiranSession = null;
		try {
			lampiranSession = StreamingHibernateUtil.getInstance().openSession();
			lampiranSession.beginTransaction();
			Session session = lampiranSession;

			List<PertemuanFileContent> pertemuanFileContents = session.createCriteria(PertemuanFileContent.class)
					.addOrder(Order.desc("id")).add(Restrictions.eq("pertemuan", pertemuan.getId())).list();
			for (PertemuanFileContent c : pertemuanFileContents) {
				Session itemSession = null;
				try {
					itemSession = StreamingHibernateUtil.getInstance().openSession();
					itemSession.beginTransaction();
					c = (PertemuanFileContent) itemSession.get(PertemuanFileContent.class, c.getId());
					PertemuanFileContent pertemuanFileContent = new PertemuanFileContent();
					pertemuanFileContent.setFoto(c.getFoto());
					pertemuanFileContent.setNama(c.getNama());
					pertemuanFileContent.setFileMimeType(c.getFileMimeType());
					pertemuanFileContent.setCopyDari(c);
					pertemuanFileContent.setGdrive(c.getGdrive());
					pertemuanFileContent.setLokasiFisik(c.getLokasiFisik());
					pertemuanFileContent.setPertemuan(pertemuanBaru.getId());
					pertemuanFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());
					itemSession.save(pertemuanFileContent);
					itemSession.getTransaction().commit();
					hasil.file++;
				} catch (Exception e) {
					hasil.kendala.add("File materi \"" + c.getNama() + "\": "
							+ ais.action.master.helper.PenjadwalanHelper.pesanError(e));
					ais.common.ErrorAuditUtil.record(e, "salin-agenda-siswa file p" + pertemuanBaru.getId());
				} finally {
					HibernateUtil.closeSessionQuietly(itemSession);
				}
			}

			List<VideoPertemuan> videoPertemuans = session.createCriteria(VideoPertemuan.class)
					.addOrder(Order.desc("id")).add(Restrictions.eq("pertemuan", pertemuan.getId())).list();
			for (VideoPertemuan c : videoPertemuans) {
				Session itemSession = null;
				try {
					itemSession = StreamingHibernateUtil.getInstance().openSession();
					itemSession.beginTransaction();
					c = (VideoPertemuan) itemSession.get(VideoPertemuan.class, c.getId());
					VideoPertemuan videoPertemuan = new VideoPertemuan();
					videoPertemuan.setFoto(c.getFoto());
					videoPertemuan.setNama(c.getNama());
					videoPertemuan.setJurusan(c.getJurusan());
					videoPertemuan.setKeterangan(c.getKeterangan());
					videoPertemuan.setKeteranganTambahan(c.getKeteranganTambahan());
					videoPertemuan.setTahunAkademik(c.getTahunAkademik());
					videoPertemuan.setType(c.getType());
					videoPertemuan.setUkuran(c.getUkuran());
					videoPertemuan.setCopyDari(c);
					videoPertemuan.setGdrive(c.getGdrive());
					videoPertemuan.setPertemuan(pertemuanBaru.getId());
					itemSession.save(videoPertemuan);
					itemSession.getTransaction().commit();
					hasil.video++;
				} catch (Exception e) {
					hasil.kendala.add("Video materi \"" + c.getNama() + "\": "
							+ ais.action.master.helper.PenjadwalanHelper.pesanError(e));
					ais.common.ErrorAuditUtil.record(e, "salin-agenda-siswa video p" + pertemuanBaru.getId());
				} finally {
					HibernateUtil.closeSessionQuietly(itemSession);
				}
			}

			List<AudioPertemuan> audioPertemuans = session.createCriteria(AudioPertemuan.class)
					.addOrder(Order.desc("id")).add(Restrictions.eq("pertemuan", pertemuan.getId())).list();
			for (AudioPertemuan c : audioPertemuans) {
				Session itemSession = null;
				try {
					itemSession = StreamingHibernateUtil.getInstance().openSession();
					itemSession.beginTransaction();
					c = (AudioPertemuan) itemSession.get(AudioPertemuan.class, c.getId());
					AudioPertemuan audioPertemuan = new AudioPertemuan();
					audioPertemuan.setFoto(c.getFoto());
					audioPertemuan.setNama(c.getNama());
					audioPertemuan.setJurusan(c.getJurusan());
					audioPertemuan.setKeterangan(c.getKeterangan());
					audioPertemuan.setKeteranganTambahan(c.getKeteranganTambahan());
					audioPertemuan.setTahunAkademik(c.getTahunAkademik());
					audioPertemuan.setType(c.getType());
					audioPertemuan.setUkuran(c.getUkuran());
					audioPertemuan.setCopyDari(c);
					audioPertemuan.setGdrive(c.getGdrive());
					audioPertemuan.setPertemuan(pertemuanBaru.getId());
					itemSession.save(audioPertemuan);
					itemSession.getTransaction().commit();
					hasil.audio++;
				} catch (Exception e) {
					hasil.kendala.add("Audio materi \"" + c.getNama() + "\": "
							+ ais.action.master.helper.PenjadwalanHelper.pesanError(e));
					ais.common.ErrorAuditUtil.record(e, "salin-agenda-siswa audio p" + pertemuanBaru.getId());
				} finally {
					HibernateUtil.closeSessionQuietly(itemSession);
				}
			}

		} catch (Exception e1) {
			hasil.kendala.add(
					"Lampiran file/video/audio (umum): " + ais.action.master.helper.PenjadwalanHelper.pesanError(e1));
			ais.common.ErrorAuditUtil.record(e1, "salin-agenda-siswa lampiran-streaming p" + pertemuanBaru.getId());
		} finally {
			HibernateUtil.closeSessionQuietly(lampiranSession);
		}

		Session session = HibernateUtil.currentSession();

		// 3) Ujian.
		try {
			List<PertemuanPunyaUjian> pertemuanPunyaUjians = session.createCriteria(PertemuanPunyaUjian.class)
					.addOrder(Order.asc("id")).add(Restrictions.eq("pertemuan", pertemuan)).list();
			for (PertemuanPunyaUjian punyaUjian : pertemuanPunyaUjians) {
				try {
					PertemuanPunyaUjian pertemuanPunyaUjian = new PertemuanPunyaUjian();
					pertemuanPunyaUjian.setDibatasiWaktu(punyaUjian.getDibatasiWaktu());
					pertemuanPunyaUjian.setFormatNilai(punyaUjian.getFormatNilai());
					pertemuanPunyaUjian.setJmlDitampilkan(punyaUjian.getJmlDitampilkan());
					pertemuanPunyaUjian.setKeterangan(punyaUjian.getKeterangan());
					pertemuanPunyaUjian.setLama(punyaUjian.getLama());
					pertemuanPunyaUjian.setMulaiUjian(punyaUjian.getMulaiUjian());
					pertemuanPunyaUjian.setNama(punyaUjian.getNama());
					pertemuanPunyaUjian.setPertemuan(pertemuanBaru);
					pertemuanPunyaUjian.setSampaiUjian(punyaUjian.getSampaiUjian());
					pertemuanPunyaUjian.setUjian(punyaUjian.getUjian());
					Common.refreshSaveOrUpdate(session, pertemuanPunyaUjian);
					hasil.ujian++;
				} catch (Exception e) {
					hasil.kendala.add("Ujian \"" + punyaUjian.getNama() + "\": "
							+ ais.action.master.helper.PenjadwalanHelper.pesanError(e));
					ais.common.ErrorAuditUtil.record(e, "salin-agenda-siswa ujian p" + pertemuanBaru.getId());
				}
			}
		} catch (Exception e) {
			hasil.kendala.add("Ujian (umum): " + ais.action.master.helper.PenjadwalanHelper.pesanError(e));
			ais.common.ErrorAuditUtil.record(e, "salin-agenda-siswa ujian-umum p" + pertemuanBaru.getId());
		}

		// 4a) Tugas kelompok (dedup by nama).
		try {
			List<ais.database.model.TugasKelompok> tugasKelompoks = session
					.createCriteria(ais.database.model.TugasKelompok.class)
					.add(Restrictions.eq("pertemuan", pertemuan.getId())).addOrder(Order.asc("id")).list();
			for (ais.database.model.TugasKelompok c : tugasKelompoks) {
				try {
					ais.database.model.TugasKelompok ada = (ais.database.model.TugasKelompok) session
							.createCriteria(ais.database.model.TugasKelompok.class)
							.add(Restrictions.eq("nama", c.getNama()))
							.add(Restrictions.eq("pertemuan", pertemuanBaru.getId())).setMaxResults(1).uniqueResult();
					if (ada == null) {
						ais.database.model.TugasKelompok baru = new ais.database.model.TugasKelompok();
						baru.setPertemuan(pertemuanBaru.getId());
						baru.setNama(c.getNama());
						Common.refreshSaveOrUpdate(session, baru);
						hasil.tugasKelompok++;
					}
				} catch (Exception e) {
					hasil.kendala.add("Tugas kelompok \"" + c.getNama() + "\": "
							+ ais.action.master.helper.PenjadwalanHelper.pesanError(e));
					ais.common.ErrorAuditUtil.record(e, "salin-agenda-siswa tugas-kelompok p" + pertemuanBaru.getId());
				}
			}
		} catch (Exception e) {
			hasil.kendala.add("Tugas kelompok (umum): " + ais.action.master.helper.PenjadwalanHelper.pesanError(e));
			ais.common.ErrorAuditUtil.record(e, "salin-agenda-siswa tugas-kelompok-umum p" + pertemuanBaru.getId());
		}

		// 4b) Tugas mandiri / individu (TugasPertemuan) — dedup by judul. Field per-peserta TIDAK disalin.
		try {
			List<ais.database.model.TugasPertemuan> tugasMandiris = session
					.createCriteria(ais.database.model.TugasPertemuan.class)
					.add(Restrictions.eq("pertemuan", pertemuan.getId())).addOrder(Order.asc("id")).list();
			for (ais.database.model.TugasPertemuan c : tugasMandiris) {
				try {
					String judul = c.getJudultugas();
					if (judul == null || judul.trim().isEmpty()) {
						continue;
					}
					ais.database.model.TugasPertemuan ada = (ais.database.model.TugasPertemuan) session
							.createCriteria(ais.database.model.TugasPertemuan.class)
							.add(Restrictions.eq("judultugas", judul))
							.add(Restrictions.eq("pertemuan", pertemuanBaru.getId())).setMaxResults(1).uniqueResult();
					if (ada == null) {
						ais.database.model.TugasPertemuan baru = new ais.database.model.TugasPertemuan();
						baru.setJudultugas(c.getJudultugas());
						baru.setIsitugas(c.getIsitugas());
						baru.setMulai(c.getMulai());
						baru.setSelesai(c.getSelesai());
						baru.setFormatNilai(c.getFormatNilai());
						baru.setFormatNilais(c.getFormatNilais());
						baru.setProsentase(c.getProsentase());
						baru.setSyaratMengumpulkanTugas(c.getSyaratMengumpulkanTugas());
						baru.setSyaratAkses(c.getSyaratAkses());
						baru.setJenisItemPenilaianSiswa(c.getJenisItemPenilaianSiswa());
						baru.setGrupPenilaian(c.getGrupPenilaian());
						baru.setGrupKategoriItemPenilaianSiswa(c.getGrupKategoriItemPenilaianSiswa());
						baru.setAktif(c.getAktif() == null ? Boolean.TRUE : c.getAktif());
						baru.setPertemuan(pertemuanBaru.getId());
						Common.refreshSaveOrUpdate(session, baru);
						hasil.tugasMandiri++;
					}
				} catch (Exception e) {
					hasil.kendala.add("Tugas mandiri \"" + c.getJudultugas() + "\": "
							+ ais.action.master.helper.PenjadwalanHelper.pesanError(e));
					ais.common.ErrorAuditUtil.record(e, "salin-agenda-siswa tugas-mandiri p" + pertemuanBaru.getId());
				}
			}
		} catch (Exception e) {
			hasil.kendala.add("Tugas mandiri (umum): " + ais.action.master.helper.PenjadwalanHelper.pesanError(e));
			ais.common.ErrorAuditUtil.record(e, "salin-agenda-siswa tugas-mandiri-umum p" + pertemuanBaru.getId());
		}

		return hasil;
	}

	/**
	 * Membuat tombol "Tambah Satu Pertemuan" yang membuka dialog pemilihan tanggal+jam untuk
	 * menambah satu pertemuan ad-hoc (di luar pola jadwal reguler) ke {@code jadwalPelajaran}.
	 * Tombol disembunyikan bagi siswa/mahasiswa, dan bagi guru bila
	 * {@code jadwalPelajaran.guruBisaMerubahTanggalJadwalPelajaran} tidak diaktifkan.
	 *
	 * @param jadwalPelajaran     jadwal pelajaran tujuan
	 * @param tbmuser             pengguna saat ini, menentukan visibilitas tombol
	 * @param eventListenerData   dipanggil setelah pertemuan baru dibuat, untuk memuat ulang data pemanggil
	 * @return tombol toolbar siap ditempel ke komponen induk
	 */
	public static MyToolbarbuttonConfig buatSatuPertemuan(final JadwalPelajaran jadwalPelajaran, final Tbmuser tbmuser,
			final EventListener eventListenerData) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Satu Pertemuan", "/img/new.gif");
		if (tbmuser.getSiswa() != null || tbmuser.getMahasiswa() != null || (tbmuser.ambilGuru() != null
				&& jadwalPelajaran != null && !jadwalPelajaran.getGuruBisaMerubahTanggalJadwalPelajaran())) {
			button.setVisible(false);
		}
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final MyWindow window = new MyWindow("Pilih Tanggal Pertemuan", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("95%");
				window.setWidth("600px");
				final Combobox tahunAkademik = new Combobox();
				Common.generateTahunAjaran(tahunAkademik);

				final Combobox tahunAkademikSampai = new Combobox();
				Common.generateTahunAjaran(tahunAkademikSampai);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
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

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pertemuan"));
				final MyDatebox tanggalPertemuan;
				row.appendChild(tanggalPertemuan = new MyDatebox(WaktuUtil.getDate()));
				tanggalPertemuan.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Pertemuan"));

				final Timebox waktuMulai;
				final Timebox waktuSelesai;
				waktuMulai = new ais.ui.util.MyTimebox();
				waktuSelesai = new ais.ui.util.MyTimebox();
				waktuMulai.setFormat(Common.timeFormat.get().toPattern());
				waktuSelesai.setFormat(Common.timeFormat.get().toPattern());

				try {
					waktuMulai.setValue(
							jadwalPelajaran.getWaktuMulai() == null || jadwalPelajaran.getWaktuMulai().trim().isEmpty()
									? null
									: parseWaktu(jadwalPelajaran.getWaktuMulai()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PenjadwalanSiswaHelper.java:1412");

				}
				try {
					waktuSelesai.setValue(jadwalPelajaran.getWaktuSelesai() == null
							|| jadwalPelajaran.getWaktuSelesai().trim().isEmpty() ? null
									: parseWaktu(jadwalPelajaran.getWaktuSelesai()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/PenjadwalanSiswaHelper.java:1419");

				}

				waktuMulai.setCols(1);
				waktuSelesai.setCols(1);

				Hbox hbox = new Hbox();
				hbox.setParent(row);
				waktuMulai.setParent(hbox);
				waktuSelesai.setParent(hbox);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pertemuan"));
				final Combobox combobox = new Combobox();
				Common.insertCombo(combobox, "nama", StatusPertemuan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
				Common.selectComboItem(combobox, ConstantValues.TATAP_MUKA);
				combobox.setWidth("95%");
				combobox.setParent(row);
				combobox.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Kemampuan / kompetensi yang ingin dicapai"));
				final MyTextbox topik;
				row.appendChild(topik = new MyTextbox());
				topik.setWidth("95%");
				topik.setRows(5);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Bahan Kajian"));
				final MyTextbox bukuRujukan1;
				row.appendChild(bukuRujukan1 = new MyTextbox());
				bukuRujukan1.setWidth("95%");
				bukuRujukan1.setRows(4);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Metode Pembelajaran"));
				final MyTextbox metodePembelajaran;
				row.appendChild(metodePembelajaran = new MyTextbox());
				metodePembelajaran.setWidth("95%");
				metodePembelajaran.setRows(2);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Referensi"));
				final MyTextbox bukuRujukan2;
				row.appendChild(bukuRujukan2 = new MyTextbox());
				bukuRujukan2.setWidth("95%");
				bukuRujukan2.setRows(5);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Catatan"));
				final MyTextbox catatan;
				row.appendChild(catatan = new MyTextbox());
				catatan.setWidth("95%");
				catatan.setRows(5);

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
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Tambahkan Pertemuan", "/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						Session session = HibernateUtil.currentSession();
						StatusPertemuan statusPertemuan = combobox.getSelectedItem() == null ? null
								: (StatusPertemuan) combobox.getSelectedItem().getValue();
						if (statusPertemuan == null) {
							statusPertemuan = ambilStatusPertemuanDefault(session);
						}
						if (statusPertemuan == null) {
							MyMessageboxConfig.show("Jenis pertemuan belum tersedia.", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							return;
						}
						window.detach();

						final Pertemuan pertemuan = new Pertemuan();
						pertemuan.setCatatan(catatan.getValue());
						pertemuan.setBukuRujukan1(bukuRujukan1.getValue());
						pertemuan.setBukuRujukan2(bukuRujukan2.getValue());
						pertemuan.setStatusPertemuan(statusPertemuan);
						pertemuan.setTanggal(tanggalPertemuan.getValue());
						pertemuan.setJadwalPelajaran(jadwalPelajaran);
						pertemuan.setRuang(jadwalPelajaran.getRuang());
						pertemuan.setTopik(topik.getValue());
						pertemuan.setWaktuMulai(waktuMulai.getValue() == null ? null
								: Common.timeFormat2.get().format(waktuMulai.getValue()));
						pertemuan.setWaktuSelesai(waktuSelesai.getValue() == null ? null
								: Common.timeFormat2.get().format(waktuSelesai.getValue()));
						pertemuan.setMetodePembelajaran(metodePembelajaran.getValue());
						if (jadwalPelajaran.getJamPelajaran() != null) {
							pertemuan.setWaktuMulai(jadwalPelajaran.getJamPelajaran().getMulaiS());
							pertemuan.setWaktuSelesai(jadwalPelajaran.getJamPelajaran().getSampaiS());
						}
						pertemuan.setJadwalPelajaran(jadwalPelajaran);

						session.save(pertemuan);
						session.flush();
						jadwalPelajaran.belum();

						List<Pertemuan> pertemuansTemp = session.createCriteria(Pertemuan.class)
								.addOrder(!jadwalPelajaran.getUrutkanotomatis() ? Order.asc("pertemuanKe")
										: Order.asc("tanggal"))
								.add(Restrictions.isNotNull("tanggal")).addOrder(Order.asc("id"))
								.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran)).list();
						jadwalPelajaran.reInitPertemuan(pertemuansTemp, session);
						pertemuansTemp.clear();
						pertemuansTemp = null;
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								eventListenerData.onEvent(new Event("", null, pertemuan));
							}
						});

					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});

		return button;
	}

	/**
	 * Membangun tampilan bertab untuk {@code jadwalPelajaran} di dalam {@code component}: tab
	 * "Jadwal Pertemuan" (grid daftar pertemuan beserta toolbar aksi), "Deskripsi Pembelajaran",
	 * dan "Capaian / Kompetensi" — dua tab terakhir tersimpan otomatis saat isian berubah.
	 *
	 * @param jadwalPelajaran jadwal pelajaran yang ditampilkan/dikelola
	 * @param component       komponen induk tempat tampilan dibangun (dibersihkan lebih dulu)
	 */
	public void display(final JadwalPelajaran jadwalPelajaran, Component component) {
		Common.clear(component);
		this.jadwalPelajaran = jadwalPelajaran;

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tab1 = new MyTabConfig("Jadwal Pertemuan");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Deskripsi Pembelajaran");
		tab2.setParent(tabs);

		MyTabConfig tab3 = new MyTabConfig("Capaian / Kompetensi");
		tab3.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);

		Tabpanel tabpanelLain = new ais.ui.util.MyTabpanel();
		tabpanelLain.setParent(tabpanels);

		final Textbox deskripsiPembelajaran = new Textbox();
		deskripsiPembelajaran.setValue(jadwalPelajaran.getDeskripsiPembelajaran());
		deskripsiPembelajaran.setRows(25);
		deskripsiPembelajaran.setWidth("100%");
		deskripsiPembelajaran.setParent(tabpanelLain);

		deskripsiPembelajaran.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jadwalPelajaran.setDeskripsiPembelajaran(deskripsiPembelajaran.getValue());
				Common.refreshSaveOrUpdate(jadwalPelajaran);

				if (jadwalPelajaran.getMatapelajaran().getDeskripsiPembelajaran().isEmpty()) {
					Matapelajaran matapelajaran = jadwalPelajaran.getMatapelajaran();
					matapelajaran.setDeskripsiPembelajaran(jadwalPelajaran.getDeskripsiPembelajaran());
					Common.refreshSaveOrUpdate(matapelajaran);
				}
			}
		});

		tabpanelLain = new ais.ui.util.MyTabpanel();
		tabpanelLain.setParent(tabpanels);

		final Textbox capaianPembelajaranProdi = new Textbox();
		capaianPembelajaranProdi.setValue(jadwalPelajaran.getCapaianPembelajaranProdi());
		capaianPembelajaranProdi.setRows(25);
		capaianPembelajaranProdi.setWidth("100%");
		capaianPembelajaranProdi.setParent(tabpanelLain);

		capaianPembelajaranProdi.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jadwalPelajaran.setCapaianPembelajaranProdi(capaianPembelajaranProdi.getValue());
				Common.refreshSaveOrUpdate(jadwalPelajaran);

				if (jadwalPelajaran.getMatapelajaran().getCapaianPembelajaranProdi().isEmpty()) {
					Matapelajaran matapelajaran = jadwalPelajaran.getMatapelajaran();
					matapelajaran.setCapaianPembelajaranProdi(jadwalPelajaran.getCapaianPembelajaranProdi());
					Common.refreshSaveOrUpdate(matapelajaran);
				}
			}
		});

		tabpanelLain = new ais.ui.util.MyTabpanel();
		tabpanelLain.setParent(tabpanels);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanelUtama);
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(north);

		Tbmuser tbmuser = Common.getCurrentUser();

		if (jadwalPelajaran != null && tbmuser.getSiswa() == null) {

			if (tbmuser.ambilGuru() == null || jadwalPelajaran.getGuruBisaMerubahTanggalJadwalPelajaran()) {
				PenjadwalanSiswaHelper.tampilTombolBuatPertemuan(toolbar, jadwalPelajaran, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});
			}
		}

		MyToolbarbuttonConfig button = PenjadwalanSiswaHelper.buatSatuPertemuan(jadwalPelajaran, tbmuser,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
					}
				});
		button.setParent(toolbar);

		if (tbmuser.getSiswa() == null && (tbmuser.ambilGuru() == null
				|| (jadwalPelajaran != null && jadwalPelajaran.getGuruBisaMerubahTanggalJadwalPelajaran()))) {

			PenjadwalanSiswaHelper.tampilTombolAmbil(toolbar, jadwalPelajaran, new DataLoader() {

				@Override
				public void loadData(Object value) {
					onSearchDefault(null);
				}
			});
			PenjadwalanSiswaHelper.tampilTombolAturUlangWaktu(toolbar, jadwalPelajaran, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});
		}

		String[] contents = new String[] { "id", "indikator", "topik", "metodePembelajaran", "pengalamanBelajar",
				"waktupembelajaran", "tugasDanPenilaian", "catatan", "bukuRujukan1", "bukuRujukan2", "guruTamu",
				"guruTamu", "tanggal", "statusPertemuan", "ruang", "waktuMulai", "waktuSelesai" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Pertemuan.class, new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				Session session = HibernateUtil.currentSession();

				return session.createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("tanggal")).addOrder(Order.asc("id"))
						.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran));
			}
		}, "Download", "/img/print.png", contents);
		toolbar.appendChild(cetakToolbarbutton);

		if (tbmuser.getSiswa() == null && (tbmuser.ambilGuru() == null
				|| (jadwalPelajaran != null && jadwalPelajaran.getGuruBisaMerubahTanggalJadwalPelajaran()))) {

			Criterion idCrit = null;
			HashMap<String, Object> nilai = null;
			if (jadwalPelajaran != null) {
				idCrit = Restrictions.eq("jadwalPelajaran", jadwalPelajaran);
				nilai = new HashMap<String, Object>();
				nilai.put("jadwalPelajaran", jadwalPelajaran);
			}

			MyToolbarbuttonConfig upload = Common.uploadData(new DataSearchDefault() {

				@Override
				public void onSearchDefault(Event event) {
					PenjadwalanSiswaHelper.this.onSearchDefault(null);
				}
			}, Pertemuan.class, null, idCrit, nilai, contents);
			toolbar.appendChild(upload);

			PenjadwalanSiswaHelper.tampilTombolHapus(toolbar, jadwalPelajaran, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});
		}

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jadwalPelajaran.belum();
				onSearchDefault(arg0);
			}
		});

		hanyaYangAktif = new MyCheckboxConfig("hanya yg aktif");
		hanyaYangAktif.setChecked(true);
		hanyaYangAktif.setParent(toolbar);
		hanyaYangAktif.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jadwalPelajaran.belum();
				onSearchDefault(arg0);
			}
		});

		urutkanManual = new MyCheckboxConfig("Urutkan Manual");
		urutkanManual.setChecked(!jadwalPelajaran.getUrutkanotomatis());
		urutkanManual.setParent(toolbar);
		urutkanManual.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jadwalPelajaran.setUrutkanotomatis(!urutkanManual.isChecked());
				Common.refreshUpdate(jadwalPelajaran);
				jadwalPelajaran.belum();
				onSearchDefault(arg0);
			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.");
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kemampuan akhir pembelajaran");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kriteria,Indikator&Bobot penilaian");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pengalaman Belajar");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tugas Dan Penilaian");
		column.setWidth("18%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Bahan Kajian");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Referensi");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Metode Pembelajaran");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis Pert.");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Aktif");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal/Waktu");
		column.setWidth("18%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		onSearchDefault(null);
	}

	/**
	 * Membuka {@link #display(JadwalPelajaran, Component)} sebagai jendela modal penuh (98%
	 * layar) dengan tombol "Selesai" yang menandai jadwal pelajaran sebagai belum lengkap
	 * ({@code jadwalPelajaran.belum()}) dan memuat ulang data pemanggil ({@code dataLoader}) saat
	 * ditutup.
	 *
	 * @param jadwalPelajaran jadwal pelajaran yang ditampilkan
	 * @param dataLoader      callback pemuatan ulang data layar pemanggil setelah jendela ditutup
	 */
	public void display(final JadwalPelajaran jadwalPelajaran, final DataLoader dataLoader) {
		final Window window = new Window();
		window.setClosable(true);
		window.setBorder("none");
		window.setTitle(jadwalPelajaran.info());

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		Common.clear(window);
		window.setWidth("98%");
		window.setHeight("98%");

		Borderlayout borderlayoutUtama = new ais.ui.util.MyBorderlayout();
		borderlayoutUtama.setParent(window);
		borderlayoutUtama.setWidth("100%");
		borderlayoutUtama.setHeight("100%");

		Center centerUtama = new Center();
		centerUtama.setParent(borderlayoutUtama);
		ais.ui.util.ZkCompat.setFlex(centerUtama, true);

		display(jadwalPelajaran, centerUtama);

		South south = new South();
		south.setParent(borderlayoutUtama);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				jadwalPelajaran.belum();
				dataLoader.loadData(null);
				window.detach();
			}
		});
		button.setParent(toolbar);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Memuat ulang daftar pertemuan {@link #jadwalPelajaran} ke grid, diurutkan berdasarkan tanggal atau nomor pertemuan (mengikuti {@code urutkanotomatis}), disaring status aktif sesuai checkbox {@link #hanyaYangAktif}. */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		if (!hanyaYangAktif.isChecked()) {
			List<Long> pertemuanss = session.createCriteria(Pertemuan.class).setProjection(Projections.property("id"))
					.addOrder(!jadwalPelajaran.getUrutkanotomatis() ? Order.asc("pertemuanKe") : Order.asc("tanggal"))
					.add(Restrictions.isNotNull("tanggal")).addOrder(Order.asc("id"))
					.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran)).list();

			ListModel strset = new SimpleListModel(pertemuanss);
			grid.setRowRenderer(new PertemuanRenderer());
			grid.setModelCheckMobile(strset);
		} else {

			final List<Long> pertemuanss;
			if (jadwalPelajaran.udah()) {
				Object[] a = jadwalPelajaran.ambilPertemuan(0, 1000, false);
				pertemuanss = (List<Long>) a[0];
			} else {
				List<Pertemuan> pertemuansTemp = session.createCriteria(Pertemuan.class)
						.add(hanyaYangAktif.isChecked()
								? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
								: Restrictions.sqlRestriction("true"))
						.addOrder(
								!jadwalPelajaran.getUrutkanotomatis() ? Order.asc("pertemuanKe") : Order.asc("tanggal"))
						.add(Restrictions.isNotNull("tanggal")).addOrder(Order.asc("id"))
						.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran)).list();
				jadwalPelajaran.reInitPertemuan(pertemuansTemp, session);
				pertemuansTemp.clear();
				pertemuansTemp = null;

				Object[] a = jadwalPelajaran.ambilPertemuan(0, 1000, false);
				pertemuanss = (List<Long>) a[0];
			}

			ListModel strset = new SimpleListModel(pertemuanss);
			grid.setRowRenderer(new PertemuanRenderer());
			grid.setModelCheckMobile(strset);
		}

	}

}
