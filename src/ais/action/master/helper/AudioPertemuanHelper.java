package ais.action.master.helper;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.TampilanELearningAction;
import ais.action.master.helper.generic.AmbilDataAudioPertemuan;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.KurikulumPunyaMatakuliahDetail;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.file.FileFoto;
import ais.database.model.streaming.AudioPertemuan;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowStyled;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper composer ZK untuk menampilkan dan mengelola lampiran audio ({@link AudioPertemuan})
 * terkait satu {@link Pertemuan} (atau, lebih luas, satu {@link KurikulumPunyaMatakuliah}/
 * {@link KurikulumPunyaMatakuliahDetail}, atau kumpulan banyak pertemuan sekaligus). Digunakan pada
 * tampilan e-learning untuk memutar/mengunduh materi audio, menampilkan syarat akses (bila ada),
 * dan — bagi user berwenang (bukan mahasiswa/siswa/calon) — menambah, mengubah keterangan, atau
 * menghapus audio.
 *
 * <p>
 * Bila hanya ada satu audio yang cocok, kontennya ditampilkan langsung; bila lebih dari satu,
 * ditampilkan sebagai tab (vertikal di mobile) yang dimuat malas saat tab dibuka — dengan tab yang
 * cocok dengan {@code selectedAudioPertemuan} (bila diberikan) dipilih otomatis, atau tab pertama
 * bila tidak ada yang cocok. Penghapusan bersifat "soft" — bukan {@code DELETE} baris, melainkan
 * SQL {@code UPDATE} yang menimpa kolom relasi ({@code pertemuan}, {@code kurikulumpunyamatakuliah},
 * dst.) dengan id sentinel negatif agar audio lepas dari semua tautan tapi baris tetap ada. Audio
 * dapat ditambah lewat pemilihan dari sumber lain (menyalin/{@code clone} lewat
 * {@link AmbilDataAudioPertemuan}), tautan langsung, unggah Google Drive, atau Dropbox — masing-
 * masing didelegasikan ke method statis {@link AmbilDataAudioPertemuan}.
 * </p>
 */
public class AudioPertemuanHelper implements DataLoader {

	private Pertemuan pertemuan;
	private Boolean delete = false;
	private List<Number> pertemuans;
	private KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah;
	private KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail;

	private boolean tampilkanMk;
	private Center center;
	private AudioPertemuan selectedAudioPertemuan = null;

	/**
	 * @param delete       bila {@code true}, tombol tambah/hapus dan edit keterangan audio
	 *                     ditampilkan bagi user berwenang
	 * @param tampilkanMk  bila {@code true}, info matakuliah pertemuan ikut ditampilkan di bawah
	 *                     konten audio (hanya berlaku saat menampilkan satu audio, lewat
	 *                     {@link Pertemuan#tampilMk(Vbox)})
	 */
	public AudioPertemuanHelper(Boolean delete, boolean tampilkanMk) {
		this.delete = delete;
		this.tampilkanMk = tampilkanMk;
	}

	/**
	 * Membangun grid berisi konten pemutar satu {@link AudioPertemuan}: pratinjau embed Google
	 * Drive bila {@code audioPertemuan.getGdrive()} terisi, atau tautan eksternal lain dari
	 * {@code getLink()}; diikuti keterangan tambahan (dengan URL di dalamnya diubah jadi tautan
	 * klik), dan info revisi pertemuan terkait (dicari dari {@code audioPertemuan.getPertemuan()}
	 * bila parameter {@code pertemuan} tidak diberikan). Menandai pertemuan sudah "mengakses" audio
	 * ini lewat {@link Pertemuan#masukkanData(String)}.
	 *
	 * @param audioPertemuan audio yang akan ditampilkan
	 * @param pertemuan      pertemuan terkait untuk info revisi; boleh {@code null} (akan dicoba
	 *                       dicari otomatis dari {@code audioPertemuan.getPertemuan()})
	 * @return grid siap dipasang ke parent
	 * @throws Exception diteruskan dari kegagalan pembangunan UI
	 */
	public static Grid createBoxAudio(final AudioPertemuan audioPertemuan, Pertemuan pertemuan) throws Exception {
		if (pertemuan != null)
			pertemuan.masukkanData("audio_" + audioPertemuan.getId());

		Row rowAudio = Common.tampilanScroll2(null);

		if (audioPertemuan.getGdrive() != null) {
			String contentAudio = "<iframe src=\"https://drive.google.com/file/d/" + audioPertemuan.getGdrive()
					+ "/preview\"  " + Common.getStyleContentAudio() + "></iframe>";
			MyRowStyled vboxAudio = new MyRowStyled();
			vboxAudio.setParent(rowAudio.getParent());
			new ais.ui.util.MyHtml(contentAudio).setParent(vboxAudio);
		} else {
			String url = audioPertemuan.getLink();
			for (String u : Common.getUrls(url)) {
				MyRowStyled vboxAudio = new MyRowStyled();
				vboxAudio.setParent(rowAudio.getParent());
				Common.displayUrlContent(u, vboxAudio);
			}
		}

		String isi = audioPertemuan.getKeteranganTambahan();

		List<String> urls = Common.getUrls(isi);

		for (String u : urls) {
			isi = org.apache.commons.lang3.StringUtils.replace(isi, u, "<a href='" + u + "' target='_blank'>" + u + "</a>");
		}
		MyRowStyled vboxAudio = new MyRowStyled();
		vboxAudio.setParent(rowAudio.getParent());
		new ais.ui.util.MyHtml("<div style='font-size:x-small'>" + isi + "</div>").setParent(vboxAudio);

		for (String u : urls) {
			vboxAudio = new MyRowStyled();
			vboxAudio.setParent(rowAudio.getParent());
			Common.displayUrlContent(u, vboxAudio);
		}

		if (pertemuan != null) {
			try {
				vboxAudio = new MyRowStyled();
				vboxAudio.setParent(rowAudio.getParent());
				RevisiHelper.createNewRevisi(Pertemuan.class, pertemuan,
						pertemuan.info() + " pertemuan ke " + pertemuan.getPertemuanKe() + " " + pertemuan.getTopik()
								+ " " + Common.dateFormat4.get().format(pertemuan.getTanggal()))
						.setParent(vboxAudio);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AudioPertemuanHelper.java:125");
				// TODO: handle exception
			}
		} else if (audioPertemuan.getPertemuan() != null) {
			Session session = HibernateUtil.currentSession();
			pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class)
					.add(Restrictions.idEq(audioPertemuan.getPertemuan())).uniqueResult();
			if (pertemuan != null) {

				try {
					vboxAudio = new MyRowStyled();
					vboxAudio.setParent(rowAudio.getParent());
					RevisiHelper.createNewRevisi(Pertemuan.class, pertemuan,
							pertemuan.info() + " pertemuan ke " + pertemuan.getPertemuanKe() + " "
									+ pertemuan.getTopik() + " " + Common.dateFormat4.get().format(pertemuan.getTanggal()))
							.setParent(vboxAudio);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AudioPertemuanHelper.java:141");
					// TODO: handle exception
				}

			}
			pertemuan = null;
		}

		return rowAudio.getGrid();
	}

	/**
	 * Menampilkan satu {@link AudioPertemuan} secara lengkap di dalam {@code tabpanelUtama}: syarat
	 * akses (bila pertemuan terkait punya syarat, ditampilkan editable untuk staf atau read-only
	 * untuk mahasiswa/peserta), tombol putar/unduh audio, keterangan (dapat diedit oleh non-peserta
	 * bila {@link #delete} aktif) dengan tombol hapus, indikator "dilihat"
	 * ({@link TampilanELearningAction#dilihat}), konten pemutar (lewat {@link #createBoxAudio}), dan
	 * opsional info matakuliah bila {@link #tampilkanMk}.
	 *
	 * @param tabpanelUtama  komponen induk tempat konten ditampilkan
	 * @param audioPertemuan audio yang ditampilkan
	 * @throws Exception diteruskan dari kegagalan pembangunan UI atau akses data
	 */
	private void tampilkanKonten(final Component tabpanelUtama, final AudioPertemuan audioPertemuan) throws Exception {
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(tabpanelUtama);
		Tbmuser user = Common.getCurrentUser();
		Mahasiswa mahasiswa = user == null ? null : user.getMahasiswa();
		BiodataCalonMahasiswa biodataCalonMahasiswa = user == null ? null : user.getBiodataCalonMahasiswa();

		Rows myRows = new Rows();

		Set<String> syaratAlert = new HashSet<String>();

		if (pertemuan != null) {

			MyToolbarbutton button = new MyToolbarbutton("fa-refresh", "Refresh Syarat");

			Tbmuser tbmuser = Common.getCurrentUser();
			if (mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null && tbmuser.getSiswa()==null
					&& tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null) {
				Tugas.tampilanSyarat(pertemuan, null, null, null, audioPertemuan, null, myRows, syaratAlert, button);
			} else {
				Tugas.tampilanSyaratReadonly(pertemuan, null, null, null, audioPertemuan, null, myRows, syaratAlert,
						button);

				Tugas.tampilanLain(pertemuan, null, null, null, audioPertemuan, null, myRows, button);
			}
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		if (!syaratAlert.isEmpty() && (mahasiswa != null || biodataCalonMahasiswa != null
				|| (tbmuser != null && tbmuser.getPesertaKursus() != null) || tbmuser.getSiswa() != null
				|| tbmuser.getCalonSiswa() != null)) {

			Center center = new Center();
			center.setParent(borderlayout);
			center.setBorder("none");

			Row rowUtama = Common.tampilanScroll1(center);

			Grid grid = new Grid();
			grid.setSclass("fgrid");
			grid.setParent(rowUtama);
			grid.setOddRowSclass("non-odd");

			Rows rows = new Rows();
			rows.setParent(grid);

			MyToolbarbutton button = new MyToolbarbutton("fa-refresh", "Refresh Syarat");

			Tugas.tampilanSyaratReadonly(pertemuan, null, null, null, audioPertemuan, null, rows, syaratAlert, button);

			Row baru = new Row();
			baru.setParent(rowUtama.getParent());

			grid = new Grid();
			grid.setSclass("fgrid");
			grid.setParent(baru);
			grid.setOddRowSclass("non-odd");

			rows = new Rows();
			rows.setParent(grid);

			button = new MyToolbarbutton("fa-refresh", "Refresh Info Lainya");

			Tugas.tampilanLain(pertemuan, null, null, null, audioPertemuan, null, rows, button);

		} else {

			Center center = new Center();
			center.setParent(borderlayout);
			center.setBorder("none");

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setParent(center);
			grid.setSclass("fgrid");
			grid.setOddRowSclass("non-odd");

			Rows rows = new Rows();
			rows.setParent(grid);

			Row row = new Row();row.setValign("top");
			row.setParent(rows);

			Vbox myVbox = new Vbox();
			myVbox.setParent(row);

			Hbox myHbox = new Hbox();

			String n = audioPertemuan.getNama() != null && audioPertemuan.getNama().trim().equalsIgnoreCase("link")
					? audioPertemuan.getLink()
					: audioPertemuan.getNama();

			if (n == null) {
				n = audioPertemuan.getNama();
			}
			n = n.length() > 50 ? n.substring(0, 50) + "..." : n;

			Toolbarbutton toolbarbuttonDownload = new MyToolbarbuttonConfig(n, FileFoto.icon("mp3"));
			toolbarbuttonDownload.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (audioPertemuan.getGdrive() != null) {
						audioPertemuan.tampilGDrive(null);
					} else {

						String link = audioPertemuan == null ? null
								: (audioPertemuan.getLink() == null || audioPertemuan.getLink().isEmpty() ? null
										: audioPertemuan.getLink());

						if (audioPertemuan != null
								&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
							link = audioPertemuan.createLinkUri();
							if (link != null) {
								// link = link.replaceAll("download=false", "download=true");
							}
						}

						if (audioPertemuan != null && link != null && !link.trim().isEmpty()) {

							if (audioPertemuan.bisaPreview()) {
								Common.displayWindow(audioPertemuan.merupakanGambar(), link, true, "95%", "95%", true,
										audioPertemuan);
							} else {

								if (Common.isMobile()) {
									ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
								} else {
									Clients.evalJavaScript(
											"popupCenter({url: '" + link + "', title: 'data', w: 1200, h: 600});");
								}

							}
						} else {
							MyMessageboxConfig.show(
									"Mohon maaf, berkas audio yang Anda akses tidak ditemukan. Langkah yang dapat dilakukan: (1) muat ulang halaman lalu coba kembali; (2) pastikan berkas belum dihapus atau dipindahkan; (3) hubungi pengajar atau administrator apabila berkas seharusnya tersedia.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					}
				}

			});

			if (user != null && user.getMahasiswa() == null && user.getSiswa() == null
					&& user.getBiodataCalonMahasiswa() == null && user.getCalonSiswa() == null) {
				myVbox.appendChild(new MyLabelBoldAja("Keterangan audio :"));

				myHbox.setParent(myVbox);

				final Textbox keteranganTambahan = new Textbox(audioPertemuan.getKeteranganTambahan());
				keteranganTambahan.setReadonly(!delete);
				keteranganTambahan.setRows(3);
				keteranganTambahan.setParent(myHbox);
				keteranganTambahan.setWidth("90%");
				keteranganTambahan.setStyle("min-height: 95%;");
				keteranganTambahan.setStyle("border: 1px solid #9fb8bf;border-radius: 10px;min-width: "
						+ (Common.isMobile() ? "240" : "460") + "px;width:100%;");
				keteranganTambahan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							Session session = StreamingHibernateUtil.getInstance().currentSession();
							audioPertemuan.setKeteranganTambahan(keteranganTambahan.getValue());
							session.getTransaction().begin();
							Common.refreshUpdate(session, (audioPertemuan));
							session.getTransaction().commit();
							StreamingHibernateUtil.getInstance().closeSession();

							if (tabpanelUtama instanceof Tabpanel) {
								String n = keteranganTambahan.getValue().trim();
								if (!n.isEmpty()) {
									((Tabpanel) tabpanelUtama).getLinkedTab()
											.setLabel(n.length() > 30 ? n.substring(0, 30) + "..." : n);
								}
							}

						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}
				});

				myHbox = new Hbox();
				myHbox.setParent(myVbox);

				toolbarbuttonDownload.setParent(myHbox);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus audio ini", "/img/svg/trash.svg");
				button.setVisible(delete);
				button.setParent(myHbox);
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah Anda yakin ingin menghapus data ini? Perlu diperhatikan, tindakan ini bersifat permanen dan data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Session session = StreamingHibernateUtil.getInstance().currentSession();
												session.doWork(new Work() {

													@Override
													public void execute(Connection connection) throws SQLException {

														String sql = "update audio_pertemuan set pertemuan=-11111111111111111,kurikulumpunyamatakuliah=-11111111111111111,kurikulumpunyamatakuliahdetail=-11111111111111111,gruppertemuan=-11111111111111111 where id="
																+ audioPertemuan.getId() + " ";
														Statement stmt = null;

														try {
															stmt = connection.createStatement();
															System.out.println("proses hapus -> " + sql);
															int hasil = stmt.executeUpdate(sql);
															System.out.println("hapus -> " + sql + " hasil " + hasil);
														} catch (SQLException e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AudioPertemuanHelper.java:375");
														} finally {
															if (stmt != null) {
																stmt.close();
															}
														}

													}
												});

												if (pertemuan != null)
													pertemuan.belum("audio_pertemuan");

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														loadData(null);
													}
												});

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												StreamingHibernateUtil.getInstance().rollbackTransaction();
												MyMessageboxConfig.show(Common.pesan(
														"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan terlebih dahulu data lain yang terkait; (2) periksa kembali keterkaitan data; (3) hubungi administrator apabila kendala berlanjut.",
														e.getMessage()));
											}
											StreamingHibernateUtil.getInstance().closeSession();
										}

									}
								});

					}

				});
			} else {
				myHbox.setParent(myVbox);
				toolbarbuttonDownload.setParent(myHbox);
			}

			TampilanELearningAction.dilihat(pertemuan, "audio_" + audioPertemuan.getId(), "Akses Audio")
					.setParent(myHbox);

			row = new Row();
			row.setParent(rows);

			Grid gridAudio = AudioPertemuanHelper.createBoxAudio(audioPertemuan, pertemuan);
			gridAudio.setParent(row);

			if (tampilkanMk) {

				row = new Row();
				row.setParent(rows);

				Vbox vbox = new Vbox();
				vbox.setParent(row);

				pertemuan.tampilMk(vbox);
			}

			row = new Row();
			row.setParent(rows);
			grid = new Grid();
			grid.setParent(row);
			grid.setSclass("fgrid");
//			grid.setOddRowSclass("non-odd");
			grid.appendChild(myRows);
		}

	}

	/**
	 * Memuat ulang area konten dengan seluruh {@link AudioPertemuan} yang cocok: bila
	 * {@link #pertemuan} diberikan, memakai {@link Pertemuan#ambilAudioPertemuanTotal()}; selain
	 * itu, query langsung ke {@link AudioPertemuan} difilter berdasarkan
	 * {@link #kurikulumPunyaMatakuliahDetail}, daftar {@link #pertemuans}, atau
	 * {@link #kurikulumPunyaMatakuliah} (urutan prioritas filter sesuai kondisi yang tersedia),
	 * dibatasi {@link Common#MAX_RESULT_50}. Hasil satu audio ditampilkan langsung; lebih dari satu
	 * ditampilkan sebagai tab (lihat javadoc kelas).
	 *
	 * @param value tidak dipakai; parameter standar {@link DataLoader}
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.clear(center);

		List<AudioPertemuan> audioPertemuans = new ArrayList<AudioPertemuan>();

		if (pertemuan != null) {

			TreeMap<Long, AudioPertemuan> audioPertemuansa = pertemuan.ambilAudioPertemuanTotal();
			audioPertemuans = new ArrayList<AudioPertemuan>(audioPertemuansa.values());
		} else {
			try {
				Session session = StreamingHibernateUtil.getInstance().currentSession();
				List<Long> d = null;
				if (pertemuans != null && !pertemuans.isEmpty()) {
					d = new ArrayList<Long>();
					for (Number n : pertemuans) {
						d.add(n.longValue());
					}
				}
				audioPertemuans = session.createCriteria(AudioPertemuan.class).setMaxResults(Common.MAX_RESULT_50)

						.addOrder(Order.desc("id"))
						.add(kurikulumPunyaMatakuliahDetail != null
								? Restrictions.eq("kurikulumPunyaMatakuliahDetail",
										kurikulumPunyaMatakuliahDetail.getId())
								: pertemuan == null || pertemuan.getId() == null
										? (d == null || d.isEmpty()
												? (kurikulumPunyaMatakuliah == null
														? Restrictions.sqlRestriction("true")
														: Restrictions.eq("kurikulumPunyaMatakuliah",
																kurikulumPunyaMatakuliah.getId()))
												: Restrictions.in("pertemuan", d))
										: Restrictions.eq("pertemuan", pertemuan.getId()))
						.list();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (audioPertemuans.size() == 1) {
			try {
				tampilkanKonten(center, audioPertemuans.get(0));
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AudioPertemuanHelper.java:496");
			}
		} else {

			Tabbox tabbox = new Tabbox();
			boolean mobile = Common.isMobile();
			if (mobile) {
				tabbox.setOrient("vertical");
			}
			tabbox.setParent(center);
			tabbox.setHeight("4000px");
			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			int index = 0;
			for (final AudioPertemuan audioPertemuan : audioPertemuans) {

				String n = audioPertemuan.getNama() != null && audioPertemuan.getNama().trim().equalsIgnoreCase("link")
						? audioPertemuan.getLink()
						: audioPertemuan.getNama();

				String na = !audioPertemuan.getKeteranganTambahan().isEmpty() ? audioPertemuan.getKeteranganTambahan()
						: n;

				if (na == null) {
					na = "";
				}

				Tab tab;
				tabs.appendChild(
						tab = new Tab(na.length() > 15 ? na.substring(0, 15) + ".." + "..." : na, FileFoto.icon(n)));

				if (mobile) {
					tab.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
				}

				final Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
				tabpanels.appendChild(tabpanelUtama);
				tabpanelUtama.setHeight("4000px");
				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tabpanelUtama.getChildren().isEmpty()) {
							tampilkanKonten(tabpanelUtama, audioPertemuan);
						}
					}
				};

				tab.addEventListener("onClick", eventListener);

				if (selectedAudioPertemuan != null && selectedAudioPertemuan.getId() != null
						&& audioPertemuan.getId() != null
						&& selectedAudioPertemuan.getId().equals(audioPertemuan.getId())) {
					try {
						tab.setSelected(true);
						eventListener.onEvent(null);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AudioPertemuanHelper.java:558");
					}
				}

				else if (index == 0) {
					try {
						tab.setSelected(true);
						eventListener.onEvent(null);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AudioPertemuanHelper.java:568");
					}
				}

				index++;
			}
		}

	}

	/**
	 * Varian {@link #display(Pertemuan, KurikulumPunyaMatakuliah, KurikulumPunyaMatakuliahDetail,
	 * Component, AudioPertemuan)} untuk menampilkan audio dari BANYAK pertemuan sekaligus
	 * (mis. rekap audio satu minggu), sesuai daftar id {@code pertemuans}.
	 *
	 * @param pertemuans daftar id {@link Pertemuan} yang audionya digabung ditampilkan
	 * @param component  komponen induk ZK tempat UI dibangun
	 */
	public void display(final List<Number> pertemuans, final Component component) {
		this.pertemuans = pertemuans;
		display(pertemuan, null, null, component, null);
	}

	/**
	 * Membangun UI lengkap: toolbar aksi (scan foto/tambah/link/upload GDrive/upload Dropbox/refresh
	 * — visibilitas tombol tambah bergantung {@link #delete}, toolbar keseluruhan hanya untuk user
	 * non-mahasiswa/siswa/calon) dan area konten. Lalu memuat datanya.
	 *
	 * @param pertemuan                     pertemuan yang audionya ditampilkan; boleh {@code null}
	 * @param kurikulumPunyaMatakuliahTemp   konteks kurikulum-matakuliah bila tidak spesifik ke satu
	 *                                       pertemuan; diabaikan (ditimpa) bila
	 *                                       {@code kurikulumPunyaMatakuliahDetail} diberikan
	 * @param kurikulumPunyaMatakuliahDetail konteks detail kurikulum-matakuliah; bila diberikan,
	 *                                       {@code kurikulumPunyaMatakuliahTemp} diturunkan darinya
	 * @param component                     komponen induk ZK; isinya dibersihkan lebih dulu
	 * @param selectedAudioPertemuan        audio yang tab-nya dipilih otomatis saat dibuka, boleh
	 *                                       {@code null}
	 */
	public void display(final Pertemuan pertemuan, KurikulumPunyaMatakuliah kurikulumPunyaMatakuliahTemp,
			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail, final Component component,
			AudioPertemuan selectedAudioPertemuan) {
		if (kurikulumPunyaMatakuliahDetail != null) {
			kurikulumPunyaMatakuliahTemp = kurikulumPunyaMatakuliahDetail.getKurikulumPunyaMatakuliah();
		}
		Tbmuser tbmuser = Common.getCurrentUser();
		this.pertemuan = pertemuan;
		this.kurikulumPunyaMatakuliah = kurikulumPunyaMatakuliahTemp;
		this.kurikulumPunyaMatakuliahDetail = kurikulumPunyaMatakuliahDetail;
		this.selectedAudioPertemuan = selectedAudioPertemuan;

		Common.clear(component);

		Toolbar toolbar = new Toolbar();

		AmbilDataAudioPertemuan.createScanFoto(pertemuan, kurikulumPunyaMatakuliah, kurikulumPunyaMatakuliahDetail,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (pertemuan != null)
							pertemuan.belum("audio_pertemuan");
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						});
					}
				}).setParent(toolbar);

		toolbar.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getSiswa() == null && (pertemuan != null || kurikulumPunyaMatakuliahDetail != null));
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah / Ambil Audio", "/img/new.gif");
		button.setVisible(delete);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final AmbilDataAudioPertemuan ambilDataLampiranFileLain = new AmbilDataAudioPertemuan(pertemuan,
						kurikulumPunyaMatakuliah, kurikulumPunyaMatakuliahDetail, delete);

				ambilDataLampiranFileLain.setHeight("95%");
				ambilDataLampiranFileLain.setWidth("90%");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataLampiranFileLain);
				ambilDataLampiranFileLain.onModal();
				ambilDataLampiranFileLain.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub

						AudioPertemuan audioPertemuan = (AudioPertemuan) arg0.getData();
						if (audioPertemuan != null) {
							Session session = StreamingHibernateUtil.getInstance().currentSession();
							if (pertemuan != null)
								pertemuan.belum("audio_pertemuan");
							try {

								if (!arg0.getName().equals("baru")) {

									final AudioPertemuan copy = (AudioPertemuan) audioPertemuan.clone();
									copy.setId(null);
									copy.setKurikulumPunyaMatakuliahDetail(
											kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
													: kurikulumPunyaMatakuliahDetail.getId());
									copy.setKurikulumPunyaMatakuliah(
											kurikulumPunyaMatakuliah == null ? -Common.randLong()
													: kurikulumPunyaMatakuliah.getId());
									copy.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
									copy.setCopyDari(audioPertemuan);

									Tbmuser tbmuser = Common.getCurrentUser();
									Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
									Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
									Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
									String olehId = Common.generateOlehId(tbmuser);
									copy.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
									copy.setOlehId(olehId);
									copy.setOleh(tbmuser == null ? "external_update"
											: mahasiswa != null ? mahasiswa.getNama()
													: dosen != null ? dosen.getNama()
															: pegawai != null ? pegawai.getNama()
																	: (tbmuser.getUserNama()));

									session.getTransaction().begin();
									session.save(copy);
									session.getTransaction().commit();

									audioPertemuan = copy;
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AudioPertemuanHelper.java:679");
							}

							StreamingHibernateUtil.getInstance().closeSession();

							loadData(null);

							ambilDataLampiranFileLain.detach();
						}
					}
				});

			}

		});
		button.setParent(toolbar);

		AmbilDataAudioPertemuan.tampilkanTombolLinkAudio(pertemuan, kurikulumPunyaMatakuliah,
				kurikulumPunyaMatakuliahDetail, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (pertemuan != null)
							pertemuan.belum("audio_pertemuan");
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						});
					}
				}).setParent(toolbar);

		AmbilDataAudioPertemuan.tampilkanTombolUploadGDrive(pertemuan, kurikulumPunyaMatakuliah,
				kurikulumPunyaMatakuliahDetail, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (pertemuan != null)
							pertemuan.belum("audio_pertemuan");
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						});
					}
				}).setParent(toolbar);

		AmbilDataAudioPertemuan.tampilkanTombolUploadDropbox(pertemuan, kurikulumPunyaMatakuliah,
				kurikulumPunyaMatakuliahDetail, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (pertemuan != null)
							pertemuan.belum("audio_pertemuan");
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						});
					}
				}).setParent(toolbar);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		refresh.setParent(toolbar);
		refresh.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (pertemuan != null)
					pertemuan.belum("audio_pertemuan");
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadData(null);
					}
				});
			}
		});

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setHeight("4000px");
		borderlayout.setParent(component);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setBorder("none");
		north.appendChild(toolbar);

		center = new Center();
		center.setBorder("none");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		loadData(null);

	}

}
