package ais.action.master.helper;

import java.io.File;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

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
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.TampilanELearningAction;
import ais.action.master.helper.generic.AmbilDataVideoPertemuan;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.KurikulumPunyaMatakuliahDetail;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.file.FileFoto;
import ais.database.model.streaming.VideoPertemuan;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowStyled;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper tampilan pemutaran video pertemuan e-learning ({@link VideoPertemuan}): satu pertemuan
 * dapat memiliki beberapa video dari berbagai sumber (Google Drive, YouTube, Facebook, tautan
 * bebas, atau berkas terunggah), masing-masing dirender sebagai iframe embed atau daftar tautan
 * dengan pratinjau. Bila hanya ada satu video, ditampilkan langsung; bila lebih dari satu,
 * ditampilkan sebagai grup tombol tab ({@link ais.ui.util.MyButtonTabbox}) satu tab per video,
 * dimuat lazy.
 *
 * <p>
 * Untuk staf pengajar (bukan mahasiswa/siswa/calon mahasiswa/calon siswa) dengan hak
 * {@link #delete}, toolbar menyediakan: tambah/ambil video dari sumber lain (menyalin video
 * yang sudah ada — {@code copyDari} — sebagai baris baru yang ditautkan ke pertemuan/kurikulum
 * saat ini), tautan video eksternal, unggah dari Google Drive/Dropbox, scan foto/scan layar
 * (rekam kelas), keterangan tambahan yang dapat diedit inline, dan hapus video. Untuk mahasiswa/
 * peserta didik, syarat kehadiran/tugas terkait video ditampilkan read-only lewat
 * {@link Tugas#tampilanSyaratReadonly}, sedangkan staf melihat form syarat yang dapat diedit
 * lewat {@link Tugas#tampilanSyarat}.
 * </p>
 *
 * <p>
 * <b>Catatan implementasi</b> — tombol hapus video TIDAK menghapus baris {@link VideoPertemuan}
 * dari database, melainkan menjalankan {@code UPDATE} SQL native yang mengosongkan seluruh
 * kolom foreign key (pertemuan, kurikulum-punya-matakuliah, kurikulum-punya-matakuliah-detail,
 * grup pertemuan) dengan nilai sentinel {@code -11111111111111111} — pola "lepas keterkaitan"
 * ini membuat video tidak lagi muncul di halaman manapun tanpa benar-benar menghapus data
 * historisnya.
 * </p>
 */
public class VideoPertemuanHelper implements DataLoader {

	private Pertemuan pertemuan;
	private Boolean delete = false;

	private List<Number> pertemuans;
	private KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah;
	private KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail;
	private boolean tampilkanMk;
	private Center center;
	private VideoPertemuan selectedVideoPertemuan = null;

	/**
	 * @param delete       {@code true} bila pengguna berhak menambah/mengedit/menghapus video
	 *                     (toolbar aksi ditampilkan); {@code false} untuk mode tampil saja
	 * @param tampilkanMk  bila {@code true}, info mata kuliah pertemuan ikut ditampilkan di atas video
	 */
	public VideoPertemuanHelper(Boolean delete, boolean tampilkanMk) {
		this.delete = delete;
		this.tampilkanMk = tampilkanMk;
	}

	/**
	 * Membangun konten pemutar untuk satu {@code videoPertemuan}: iframe embed Google Drive/
	 * YouTube/Facebook bila salah satunya diisi; selain itu, daftar tautan (dari
	 * {@code keteranganTambahan}/{@code link}, atau URL media hasil salin berkas lokal ke
	 * direktori media publik bila video berupa berkas terunggah) dengan tautan otomatis
	 * dilinkifikasi di dalam teks keterangan. Menambahkan info revisi {@link Pertemuan} terkait
	 * di akhir (baik dari parameter {@code pertemuan} maupun, bila tidak diberikan, dari
	 * {@code videoPertemuan.getPertemuan()}). Menandai pertemuan sudah diakses lewat
	 * {@code pertemuan.masukkanData("video_"+id)}.
	 *
	 * @param videoPertemuan   video yang akan dirender
	 * @param pertemuan        pertemuan konteks untuk info revisi, boleh {@code null}
	 * @param tampilKeterangan tampilkan blok keterangan tambahan/isi teks di atas embed
	 * @return {@link Grid} berisi seluruh baris konten yang dibangun
	 */
	public static Grid createBoxVideo(final VideoPertemuan videoPertemuan, Pertemuan pertemuan,
			boolean tampilKeterangan) throws Exception {
		if (pertemuan != null)
			pertemuan.masukkanData("video_" + videoPertemuan.getId());
		HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();

		Row rowVideo = Common.tampilanScroll2(null);

		if (videoPertemuan.getGdrive() != null) {

			if (tampilKeterangan) {
				MyRowStyled vboxVideo = new MyRowStyled();
				vboxVideo.setParent(rowVideo.getParent());
				new Label(videoPertemuan.getKeteranganTambahan()).setParent(vboxVideo);
			}
			String contentVideo = "<iframe src=\"https://drive.google.com/file/d/" + videoPertemuan.getGdrive()
					+ "/preview\"  " + Common.getStyleContent() + "></iframe>";
			MyRowStyled vboxVideo = new MyRowStyled();
			vboxVideo.setParent(rowVideo.getParent());
			new ais.ui.util.MyHtml(contentVideo).setParent(vboxVideo);

		} else if (videoPertemuan.getYoutube() != null && !videoPertemuan.getYoutube().trim().isEmpty()) {
			if (tampilKeterangan) {
				MyRowStyled vboxVideo = new MyRowStyled();
				vboxVideo.setParent(rowVideo.getParent());
				new Label(videoPertemuan.getKeteranganTambahan()).setParent(vboxVideo);
			}
			String contentVideo = "<iframe " + Common.getStyleContent() + " src=\"https://www.youtube.com/embed/"
					+ videoPertemuan.getYoutube().trim()
					+ "\" frameborder=\"0\" allow=\"accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>";

			MyRowStyled vboxVideo = new MyRowStyled();
			vboxVideo.setParent(rowVideo.getParent());
			new ais.ui.util.MyHtml(contentVideo).setParent(vboxVideo);

		} else if (videoPertemuan.getFacebook() != null && !videoPertemuan.getFacebook().trim().isEmpty()) {
			if (tampilKeterangan) {
				MyRowStyled vboxVideo = new MyRowStyled();
				vboxVideo.setParent(rowVideo.getParent());
				new Label(videoPertemuan.getKeteranganTambahan()).setParent(vboxVideo);
			}
			String contentVideo = "<iframe src=\"https://web.facebook.com/plugins/video.php?href="
					+ URLEncoder.encode(videoPertemuan.getFacebook().trim(), "UTF-8") + "&show_text=0&height=315\" "
					+ (Common.isMobile() ? "style='width:100%'" : "width=\"560\"") + " "
					+ (Common.isMobile() ? "style=\"height:600px\"" : "height=\"460\"")
					+ " style=\"border:none;overflow:hidden\" scrolling=\"no\" frameborder=\"0\" allowTransparency=\"true\" allowFullScreen=\"true\"></iframe>";

			MyRowStyled vboxVideo = new MyRowStyled();
			vboxVideo.setParent(rowVideo.getParent());
			new ais.ui.util.MyHtml(contentVideo).setParent(vboxVideo);

		}

		else {
			String isi = videoPertemuan.getKeteranganTambahan();
			Set<String> urls = new HashSet<String>(Common.getUrls(isi));
			if (videoPertemuan.getLink() != null && !videoPertemuan.getLink().trim().isEmpty()) {
				urls.addAll(Common.getUrls(videoPertemuan.getLink()));
			} else {
				File file = new File(CommonMedia.getMediaDirectory().getAbsolutePath() + "/" + videoPertemuan.getId()
						+ "__"
						+ (videoPertemuan.getNama() == null ? "" : videoPertemuan.getNama().replaceAll(" ", "_")));

				if (file == null || !file.exists()) {
					file.getParentFile().mkdirs();
					Common.copy(videoPertemuan.ambilFile(), file);
				}
				System.out.println("Video -> " + file);
				if (file != null && file.exists()) {
					String url = "http" + (Common.isSecure(request) ? "s" : "") + "://" + request.getServerName() + ":"
							+ request.getServerPort() + "/media/" + URLEncoder.encode(file.getName(), "UTF-8");
					urls.add(url);
				}
			}

			for (String u : urls) {
				isi = org.apache.commons.lang3.StringUtils.replace(isi, u,
						"<a href='" + u + "' target='_blank'>" + u + "</a>");
			}

			if (tampilKeterangan) {
				MyRowStyled vboxVideo = new MyRowStyled();
				vboxVideo.setParent(rowVideo.getParent());
				new ais.ui.util.MyHtml("<div style='font-size:x-small'>" + isi + "</div>").setParent(vboxVideo);
			}

			for (String u : urls) {
				MyRowStyled vboxVideo = new MyRowStyled();
				vboxVideo.setParent(rowVideo.getParent());
				Common.displayUrlContent(u, vboxVideo);
			}
		}

		if (pertemuan != null) {
			try {
				MyRowStyled vboxVideo = new MyRowStyled();
				vboxVideo.setParent(rowVideo.getParent());
				RevisiHelper.createNewRevisi(Pertemuan.class, pertemuan,
						pertemuan.info() + " pertemuan ke " + pertemuan.getPertemuanKe() + " " + pertemuan.getTopik()
								+ " " + Common.dateFormat4.get().format(pertemuan.getTanggal()))
						.setParent(vboxVideo);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/VideoPertemuanHelper.java:187");
				// TODO: handle exception
			}
		} else if (videoPertemuan.getPertemuan() != null) {
			Session session = HibernateUtil.currentSession();
			pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class)
					.add(Restrictions.idEq(videoPertemuan.getPertemuan())).uniqueResult();
			if (pertemuan != null) {

				try {
					MyRowStyled vboxVideo = new MyRowStyled();
					vboxVideo.setParent(rowVideo.getParent());
					RevisiHelper.createNewRevisi(Pertemuan.class, pertemuan,
							pertemuan.info() + " pertemuan ke " + pertemuan.getPertemuanKe() + " "
									+ pertemuan.getTopik() + " " + Common.dateFormat4.get().format(pertemuan.getTanggal()))
							.setParent(vboxVideo);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/VideoPertemuanHelper.java:203");
					// TODO: handle exception
				}

			}
			pertemuan = null;
		}

		return rowVideo.getGrid();
	}

	/**
	 * Membangun tampilan lengkap satu video pertemuan ke dalam {@code tabpanelUtama}: blok
	 * syarat/tugas terkait (edit untuk staf, read-only untuk mahasiswa/peserta didik — lihat
	 * javadoc kelas), tombol unduh/putar video, keterangan tambahan (editable untuk staf
	 * ber-{@link #delete}, dengan auto-save dan pembaruan label tab tombol), tombol hapus (staf
	 * ber-{@link #delete}), penanda "dilihat" ({@link TampilanELearningAction#dilihat}), embed
	 * video ({@link #createBoxVideo}), dan blok syarat/info lain (hanya dirender bila benar-benar
	 * berisi data, menghindari grid kosong tampil sebagai garis-garis kosong).
	 */
	private void tampilkanKonten(final Component tabpanelUtama, final VideoPertemuan videoPertemuan) throws Exception {
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(tabpanelUtama);

		Tbmuser user = Common.getCurrentUser();
		Mahasiswa mahasiswa = user == null ? null : user.getMahasiswa();
		BiodataCalonMahasiswa biodataCalonMahasiswa = user == null ? null : user.getBiodataCalonMahasiswa();

		Rows myRows = new Rows();

		Set<String> syaratAlert = new HashSet<String>();
		Tbmuser tbmuser = Common.getCurrentUser();
		if (pertemuan != null) {

			MyToolbarbutton button = new MyToolbarbutton("fa-refresh", "Refresh Syarat");

			if (mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
					&& tbmuser.getSiswa() == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null) {
				Tugas.tampilanSyarat(pertemuan, null, null, null, null, videoPertemuan, myRows, syaratAlert, button);
			} else {
				Tugas.tampilanSyaratReadonly(pertemuan, null, null, null, null, videoPertemuan, myRows, syaratAlert,
						button);

				Tugas.tampilanLain(pertemuan, null, null, null, null, videoPertemuan, myRows, button);
			}
		}

		if (!syaratAlert.isEmpty()
				&& (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null
						|| tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null)) {

			Center center = new Center();
			center.setParent(borderlayout);
			center.setBorder("none");

			Row rowUtama = Common.tampilanScroll1(center);

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setParent(rowUtama);
			grid.setOddRowSclass("non-odd");

			Rows rows = new Rows();
			rows.setParent(grid);

			MyToolbarbutton button = new MyToolbarbutton("fa-refresh", "Refresh Syarat");

			Tugas.tampilanSyaratReadonly(pertemuan, null, null, null, null, videoPertemuan, rows, syaratAlert, button);

			Row baru = new Row();
			baru.setParent(rowUtama.getParent());

			grid = new Grid();
			grid.setSclass("dgrid");
			grid.setParent(baru);
			grid.setOddRowSclass("non-odd");

			rows = new Rows();
			rows.setParent(grid);

			button = new MyToolbarbutton("fa-refresh", "Refresh Info Lainya");

			Tugas.tampilanLain(pertemuan, null, null, null, null, videoPertemuan, rows, button);

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

			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);

			Pertemuan pertemuan = (Pertemuan) (videoPertemuan.getPertemuan() == null ? null
					: GeneralValueObject.ambilData(Pertemuan.class, videoPertemuan.getPertemuan().toString(), true));
			boolean edita = user != null && user.getMahasiswa() == null && user.getSiswa() == null
					&& user.getBiodataCalonMahasiswa() == null && user.getCalonSiswa() == null;

			Vbox vbox = new Vbox();
			vbox.setParent(row);

			if (tampilkanMk && pertemuan != null) {
				pertemuan.tampilMk(vbox);
			}

			Hbox myHbox = new Hbox();

			String n = videoPertemuan.getNama() != null && videoPertemuan.getNama().trim().equalsIgnoreCase("link")
					? videoPertemuan.getLink()
					: videoPertemuan.getNama();

			if (n == null) {
				n = videoPertemuan.getNama();
			}
			n = n.length() > 50 ? n.substring(0, 50) + "..." : n;

			Toolbarbutton toolbarbuttonDownload = new MyToolbarbuttonConfig(n, FileFoto.icon("mp4"));
			toolbarbuttonDownload.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (videoPertemuan.getGdrive() != null) {
						videoPertemuan.tampilGDrive(null);
					} else {

						String link = videoPertemuan == null ? null
								: (videoPertemuan.getLink() == null || videoPertemuan.getLink().isEmpty() ? null
										: videoPertemuan.getLink());

						if (videoPertemuan != null
								&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
							link = videoPertemuan.createLinkUri();
							if (link != null) {
								// link = link.replaceAll("download=false", "download=true");
							}
						}

						if (videoPertemuan != null && link != null && !link.trim().isEmpty()) {

							if (videoPertemuan.bisaPreview()) {
								Common.displayWindow(videoPertemuan.merupakanGambar(), link, true, "95%", "95%", true,
										videoPertemuan);
							} else {

								if (Common.isMobile()) {
									ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
								} else {
									Clients.evalJavaScript(
											"popupCenter({url: '" + Common.jsEscape(link) + "', title: 'data', w: 1200, h: 600});");
								}

							}
						} else {
							MyMessageboxConfig.show(
									"Mohon maaf, berkas yang Bapak/Ibu akses tidak dapat ditemukan. Langkah yang dapat dilakukan: (1) memuat ulang halaman kemudian mencoba kembali; (2) memastikan berkas masih tersedia dan belum dihapus; (3) apabila kendala tetap berlanjut, mohon menghubungi pihak kampus untuk dilakukan pengecekan lebih lanjut.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					}
				}

			});

			if (edita) {

					vbox.appendChild(new MyLabelBoldAja("Keterangan video :"));

					myHbox.setParent(vbox);
					myHbox.setWidth("100%");
					myHbox.setHflex("1");

					final Textbox keteranganTambahan = new Textbox(videoPertemuan.getKeteranganTambahan());
					keteranganTambahan.setReadonly(!delete);
					keteranganTambahan.setRows(4);
					keteranganTambahan.setParent(myHbox);
					keteranganTambahan.setWidth("100%");
					keteranganTambahan.setHflex("1");
					keteranganTambahan.setStyle("border:1px solid #9fb8bf;border-radius:8px;min-width:"
							+ (Common.isMobile() ? "280" : "520") + "px;max-width:760px;width:100%;"
							+ "min-height:90px;font-size:13px;line-height:1.45;padding:8px;box-sizing:border-box;");
				keteranganTambahan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							Session session = StreamingHibernateUtil.getInstance().currentSession();
							videoPertemuan.setKeteranganTambahan(keteranganTambahan.getValue());
							session.getTransaction().begin();
							Common.refreshUpdate(session, (videoPertemuan));
							session.getTransaction().commit();

							Object tabboxAttr = tabpanelUtama.getAttribute("myButtonTabbox");
							Object indexAttr = tabpanelUtama.getAttribute("myButtonTabboxIndex");
							if (tabboxAttr instanceof ais.ui.util.MyButtonTabbox && indexAttr instanceof Integer) {
								String n = keteranganTambahan.getValue().trim();
								if (!n.isEmpty()) {
									((ais.ui.util.MyButtonTabbox) tabboxAttr).setLabelTombol((Integer) indexAttr,
											n.length() > 30 ? n.substring(0, 30) + "..." : n);
								}
							}

						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						} finally {
							StreamingHibernateUtil.getInstance().closeSession();
						}

					}
				});

				myHbox = new Hbox();
				myHbox.setParent(vbox);

				toolbarbuttonDownload.setParent(myHbox);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus video ini", "/img/svg/trash.svg");
				button.setVisible(delete);
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
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

														String sql = "update video_pertemuan set pertemuan=-11111111111111111,kurikulumpunyamatakuliah=-11111111111111111,kurikulumpunyamatakuliahdetail=-11111111111111111,gruppertemuan=-11111111111111111 where id="
																+ videoPertemuan.getId() + " ";
														Statement stmt = null;

														try {
															stmt = connection.createStatement();
															System.out.println("proses hapus -> " + sql);
															int hasil = stmt.executeUpdate(sql);
															System.out.println("hapus -> " + sql + " hasil " + hasil);
														} catch (SQLException e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/VideoPertemuanHelper.java:444");
														} finally {
															if (stmt != null) {
																stmt.close();
															}
														}

													}
												});

												Pertemuan pertemuan = (Pertemuan) (videoPertemuan.getPertemuan() == null
														? null
														: GeneralValueObject.ambilData(Pertemuan.class,
																videoPertemuan.getPertemuan().toString(), true));
												if (pertemuan != null)
													pertemuan.belum("video_pertemuan");

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														loadData(null);
													}
												});

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												StreamingHibernateUtil.getInstance().rollbackTransaction();
												PesanFormalHelper.tampilkanGagalException(
														"menghapus video pertemuan ini",
														e,
														new String[] {
																"Periksa apakah data video ini masih berelasi dengan data lain sehingga tidak dapat dihapus.",
																"Hapus atau lepaskan terlebih dahulu data terkait yang masih berelasi, lalu ulangi proses penghapusan.",
																"Jika data tetap tidak dapat dihapus, konfirmasikan kebutuhan penghapusan ini kepada Administrator." });
											} finally {
												StreamingHibernateUtil.getInstance().closeSession();
											}

										}

									}
								});

					}

				});
				button.setParent(myHbox);
			} else {
				myHbox.setParent(vbox);
				toolbarbuttonDownload.setParent(myHbox);
			}

			TampilanELearningAction.dilihat(pertemuan, "video_" + videoPertemuan.getId(), "Akses Video")
					.setParent(myHbox);

			row = new Row();
			row.setParent(rows);

			Grid vboxVideo = VideoPertemuanHelper.createBoxVideo(videoPertemuan, pertemuan, !edita);
			vboxVideo.setParent(row);

			// Hanya tampilkan grid syarat/info bila benar-benar ada isinya. Tanpa guard
			// ini, grid "fgrid" kosong (mis. pertemuan tanpa syarat pada akun dosen/admin)
			// tetap dirender dan tampak sebagai deretan garis kosong di bawah video.
			if (myRows.getChildren() != null && !myRows.getChildren().isEmpty()) {
				row = new Row();
				row.setParent(rows);
				grid = new Grid();
				grid.setParent(row);
				grid.setSclass("fgrid");
//				grid.setOddRowSclass("non-odd");
				grid.appendChild(myRows);
			}
		}
	}

	/**
	 * Memuat/menyegarkan tampilan: mengambil daftar {@link VideoPertemuan} yang relevan —
	 * seluruh video pertemuan ({@link Pertemuan#ambilVideoPertemuanTotal()}) bila {@link #pertemuan}
	 * diset; selain itu, disaring berdasarkan {@link #kurikulumPunyaMatakuliahDetail}, daftar
	 * {@link #pertemuans}, atau {@link #kurikulumPunyaMatakuliah} (urutan prioritas filter),
	 * dibatasi {@link Common#MAX_RESULT_20}. Bila hanya satu video ditemukan, ditampilkan
	 * langsung lewat {@link #tampilkanKonten}; bila lebih, dibangun grup tombol tab satu per
	 * video (label diambil dari keterangan tambahan atau nama/link, dipotong 15 karakter), dan
	 * tab {@link #selectedVideoPertemuan} (bila diset) dipilih otomatis.
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.clear(center);
		List<VideoPertemuan> videoPertemuans = new ArrayList<VideoPertemuan>();

		if (pertemuan != null) {

			TreeMap<Long, VideoPertemuan> videoPertemuansa = pertemuan.ambilVideoPertemuanTotal();
			videoPertemuans = new ArrayList<VideoPertemuan>(videoPertemuansa.values());
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

				videoPertemuans = session.createCriteria(VideoPertemuan.class).setMaxResults(Common.MAX_RESULT_20)

						.addOrder(Order.desc("id")).add(
								kurikulumPunyaMatakuliahDetail != null
										&& kurikulumPunyaMatakuliahDetail.getId() != null
												? Restrictions.eq("kurikulumPunyaMatakuliahDetail",
														kurikulumPunyaMatakuliahDetail.getId())
												: pertemuan == null || pertemuan.getId() == null
														? (d == null || d.isEmpty()
																? (kurikulumPunyaMatakuliah == null
																		|| kurikulumPunyaMatakuliah.getId() == null
																				? Restrictions.sqlRestriction("true")
																				: Restrictions.eq(
																						"kurikulumPunyaMatakuliah",
																						kurikulumPunyaMatakuliah
																								.getId()))
																: Restrictions.in("pertemuan", d))
														: Restrictions.eq("pertemuan", pertemuan.getId()))
						.list();

			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			} finally {
				// Pastikan sesi streaming selalu ditutup, walau terjadi exception.
				StreamingHibernateUtil.getInstance().closeSession();
			}
		}

		if (videoPertemuans.size() == 1) {
			try {
				tampilkanKonten(center, videoPertemuans.get(0));
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/VideoPertemuanHelper.java:570");
			}
		} else {

			// GANTI TAB -> BUTTON GROUP (kelas reusable ais.ui.util.MyButtonTabbox): tab per
			// video pertemuan ini data-driven, sama seperti pola "Ke-1".."Ke-N" di
			// SetingBiayaAction yang sebelumnya bermasalah blank/scroll pakai Tab/Tabpanel
			// bawaan ZK.
			final ais.ui.util.MyButtonTabbox tabboxVideo = ais.ui.util.MyButtonTabbox.buat(center, "4000px", null);

			int index = 0;
			int indexTerpilih = 1;
			for (final VideoPertemuan videoPertemuan : videoPertemuans) {

				String n = videoPertemuan.getNama() != null && videoPertemuan.getNama().trim().equalsIgnoreCase("link")
						? videoPertemuan.getLink()
						: videoPertemuan.getNama();

				String na = !videoPertemuan.getKeteranganTambahan().isEmpty() ? videoPertemuan.getKeteranganTambahan()
						: n;

				if (videoPertemuan.getYoutube() != null && !videoPertemuan.getYoutube().trim().isEmpty()) {
					if (na == null || na.trim().isEmpty()) {
						na = videoPertemuan.getYoutube();
					}
					n = "https://www.youtube.com/watch?v=" + videoPertemuan.getYoutube();
				}
				if (videoPertemuan.getFacebook() != null && !videoPertemuan.getFacebook().trim().isEmpty()) {
					if (na == null || na.trim().isEmpty()) {
						na = videoPertemuan.getFacebook();
					}
					n = videoPertemuan.getFacebook();
				}

				if (na == null) {
					na = "";
				}

				final int tabIndex = index + 1;
				tabboxVideo.tambahTabLazy(tabIndex,
						na != null && na.length() > 15 ? na.substring(0, 15) + ".." + "..." : na,
						new ais.ui.util.MyButtonTabbox.PemuatTab() {
							@Override
							public void muat(org.zkoss.zul.Div panel) throws Exception {
								// Simpan referensi tabbox+index agar tampilkanKonten bisa perbarui label
								// tombol saat "keterangan tambahan" diedit (pengganti getLinkedTab()).
								panel.setAttribute("myButtonTabbox", tabboxVideo);
								panel.setAttribute("myButtonTabboxIndex", Integer.valueOf(tabIndex));
								tampilkanKonten(panel, videoPertemuan);
							}
						});

				if (selectedVideoPertemuan != null && selectedVideoPertemuan.getId() != null
						&& videoPertemuan.getId() != null
						&& selectedVideoPertemuan.getId().equals(videoPertemuan.getId())) {
					indexTerpilih = tabIndex;
				}

				index++;
			}
			tabboxVideo.pilih(indexTerpilih);
		}

	}

	/** Varian {@link #display} untuk menampilkan video dari beberapa {@link Pertemuan} sekaligus (mis. rekap video mingguan), disaring berdasarkan daftar id pertemuan. */
	public void display(final List<Number> pertemuans, final Component component) {
		this.pertemuans = pertemuans;
		display(pertemuan, null, null, component, null);
	}

	/** Varian {@link #display(Pertemuan, KurikulumPunyaMatakuliah, KurikulumPunyaMatakuliahDetail, Component, String, VideoPertemuan)} tanpa {@code rtmpLink} eksplisit. */
	public void display(final Pertemuan pertemuan, KurikulumPunyaMatakuliah kurikulumPunyaMatakuliahTemp,
			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail, final Component component,
			VideoPertemuan selectedVideoPertemuan) {
		display(pertemuan, kurikulumPunyaMatakuliahTemp, kurikulumPunyaMatakuliahDetail, component, null,
				selectedVideoPertemuan);
	}

	/**
	 * Membangun toolbar aksi video (scan foto/layar, tambah-ambil video, tautan eksternal,
	 * unggah Google Drive/Dropbox, refresh — masing-masing hanya untuk staf ber-{@link #delete}
	 * yang relevan, memicu {@code pertemuan.belum("video_pertemuan")} sebelum menyegarkan) di
	 * atas area konten video, lalu memuat data awal lewat {@link #loadData(Object)}.
	 *
	 * @param pertemuan                     pertemuan konteks, boleh {@code null}
	 * @param kurikulumPunyaMatakuliahTemp  konteks mata kuliah kurikulum, diabaikan bila {@code kurikulumPunyaMatakuliahDetail} diberikan (diturunkan dari situ)
	 * @param kurikulumPunyaMatakuliahDetail konteks detail kurikulum-matakuliah, boleh {@code null}
	 * @param component                     kontainer ZK yang akan diisi (isi sebelumnya dibersihkan)
	 * @param rtmpLink                      tidak dipakai langsung pada badan method ini
	 * @param selectedVideoPertemuan        video yang tab-nya otomatis dipilih saat data dimuat, boleh {@code null}
	 */
	public void display(final Pertemuan pertemuan, KurikulumPunyaMatakuliah kurikulumPunyaMatakuliahTemp,
			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail, final Component component,
			final String rtmpLink, VideoPertemuan selectedVideoPertemuan) {
		if (kurikulumPunyaMatakuliahDetail != null) {
			kurikulumPunyaMatakuliahTemp = kurikulumPunyaMatakuliahDetail.getKurikulumPunyaMatakuliah();
		}
		Tbmuser tbmuser = Common.getCurrentUser();
		this.pertemuan = pertemuan;
		this.kurikulumPunyaMatakuliah = kurikulumPunyaMatakuliahTemp;
		this.kurikulumPunyaMatakuliahDetail = kurikulumPunyaMatakuliahDetail;
		this.selectedVideoPertemuan = selectedVideoPertemuan;

		Common.clear(component);

		Toolbar toolbar = new Toolbar();

		AmbilDataVideoPertemuan.createScanFoto(pertemuan, kurikulumPunyaMatakuliahTemp, kurikulumPunyaMatakuliahDetail,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (pertemuan != null)
							pertemuan.belum("video_pertemuan");
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						});
					}
				}).setParent(toolbar);

		if (!Common.isMobile())
			AmbilDataVideoPertemuan.createScanLayar(pertemuan, kurikulumPunyaMatakuliahTemp,
					kurikulumPunyaMatakuliahDetail, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (pertemuan != null)
								pertemuan.belum("video_pertemuan");
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(null);
								}
							});
						}
					}).setParent(toolbar);

		toolbar.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null
				&& (pertemuan != null || kurikulumPunyaMatakuliahDetail != null));

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah / Ambil Video", "/img/new.gif");
		button.setVisible(delete);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				final AmbilDataVideoPertemuan ambilDataLampiranFileLain = new AmbilDataVideoPertemuan(pertemuan,
						kurikulumPunyaMatakuliah, kurikulumPunyaMatakuliahDetail, delete);

				ambilDataLampiranFileLain.setHeight("95%");
				ambilDataLampiranFileLain.setWidth("90%");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataLampiranFileLain);
				ambilDataLampiranFileLain.onModal();
				ambilDataLampiranFileLain.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub

						VideoPertemuan videoPertemuan = (VideoPertemuan) arg0.getData();
						if (videoPertemuan != null) {
							Session session = StreamingHibernateUtil.getInstance().currentSession();
							if (pertemuan != null)
								pertemuan.belum("video_pertemuan");
							try {

								if (!arg0.getName().equals("baru")) {

									final VideoPertemuan copy = (VideoPertemuan) videoPertemuan.clone();
									copy.setId(null);
									copy.setKurikulumPunyaMatakuliahDetail(
											kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
													: kurikulumPunyaMatakuliahDetail.getId());
									copy.setKurikulumPunyaMatakuliah(
											kurikulumPunyaMatakuliah == null ? -Common.randLong()
													: kurikulumPunyaMatakuliah.getId());
									copy.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
									copy.setCopyDari(videoPertemuan);

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

									videoPertemuan = copy;
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/VideoPertemuanHelper.java:792");
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

		AmbilDataVideoPertemuan.tampilkanTombolLinkVideo(pertemuan, kurikulumPunyaMatakuliahTemp,
				kurikulumPunyaMatakuliahDetail, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (pertemuan != null)
							pertemuan.belum("video_pertemuan");
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						});
					}
				}).setParent(toolbar);

		AmbilDataVideoPertemuan.tampilkanTombolUploadGDrive(pertemuan, kurikulumPunyaMatakuliahTemp,
				kurikulumPunyaMatakuliahDetail, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (pertemuan != null)
							pertemuan.belum("video_pertemuan");
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						});
					}
				}).setParent(toolbar);

		AmbilDataVideoPertemuan.tampilkanTombolUploadDropbox(pertemuan, kurikulumPunyaMatakuliahTemp,
				kurikulumPunyaMatakuliahDetail, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (pertemuan != null)
							pertemuan.belum("video_pertemuan");
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
					pertemuan.belum("video_pertemuan");
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
