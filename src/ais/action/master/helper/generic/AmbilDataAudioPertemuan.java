package ais.action.master.helper.generic;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AudioPertemuanHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.dropbox.UploadDropboxUtil;
import ais.common.gdrive.GDriveUtilPerPengguna;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Konfigurasi;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.KurikulumPunyaMatakuliahDetail;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FileFotoLain;
import ais.database.model.streaming.AudioPertemuan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Tipe khusus untuk ambil data audio pertemuan. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code EventListener
 * eventListener}, {@code MyTextbox nama}, {@code Tbmuser tbmuser}, {@code Pertemuan pertemuan}, {@code
 * KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah}, {@code KurikulumPunyaMatakuliahDetail
 * kurikulumPunyaMatakuliahDetail}, {@code Paging paging}; inisialisasi/lifecycle ({@code initCriteria()});
 * pembacaan/pencarian ({@code tampilkanTombolUploadDropbox()}, {@code tampilkanTombolUpload()}, {@code
 * tampilkanTombolLinkAudio()}, {@code tampilkanTombolUploadGDrive()}, {@code onSearchDefault()}, {@code
 * setEventListener()}); operasi domain lain ({@code createScanFoto()}, {@code display()}); konfigurasi
 * constructor: {@code tbmuser}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class AmbilDataAudioPertemuan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	private EventListener eventListener;

	private MyTextbox nama;

	private Tbmuser tbmuser;
	private Pertemuan pertemuan;
	private KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah;
	private KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail;
	private Paging paging;

	public AmbilDataAudioPertemuan(final Pertemuan pertemuan, KurikulumPunyaMatakuliah kurikulumPunyaMatakuliahTemp,
			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail, Boolean delete) {
		super();
		this.pertemuan = pertemuan;
		this.kurikulumPunyaMatakuliah = kurikulumPunyaMatakuliahTemp;
		this.kurikulumPunyaMatakuliahDetail = kurikulumPunyaMatakuliahDetail;
		tbmuser = Common.getCurrentUser();
		display();
		onSearchDefault(null);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataAudioPertemuan}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataAudioPertemuan} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataAudioPertemuan
	 */
	class AudioPertemuanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			arg0.setValign("top");
			final AudioPertemuan audioPertemuan = (AudioPertemuan) arg1;
			arg0.setAttribute("audioPertemuan", audioPertemuan);
			final Radio checkbox = new Radio();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (eventListener != null) {
						Event myEvent = new Event("myEvent", arg0.getTarget(), audioPertemuan);
						eventListener.onEvent(myEvent);
					}
					AmbilDataAudioPertemuan.this.detach();
				}
			});

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setParent(arg0);
			grid.setSclass("fgrid");
			grid.setOddRowSclass("non-odd");

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);

			Grid gridAudio = AudioPertemuanHelper.createBoxAudio(audioPertemuan, pertemuan);
			gridAudio.setParent(row);

			row = new MyFormRow();
			row.setParent(rows);

			Vbox vboxaa = new Vbox();
			vboxaa.setParent(row);

			String olehId = audioPertemuan.getOlehId();
			String oleh = audioPertemuan.getOleh();
			vboxaa.appendChild(new MyLabelAgakKecil("Diupload oleh"));
			Common.infoDiuploadOleh(olehId, oleh, vboxaa);
		}

	}

	public static MyToolbarbuttonConfig tampilkanTombolUploadDropbox(final Pertemuan pertemuan,
			final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail, final EventListener eventListener) {

		final List<AudioPertemuan> audioPertemuans = new ArrayList<AudioPertemuan>();
		final Tbmuser tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig mybutton = new MyToolbarbuttonConfig("Upload video/mp3 ke Dropbox (maks 500 Mb)",
				FileFoto.icon("dropbox"));
		mybutton.setUpload("true,maxsize=" + (1024 * 500));
		mybutton.setVisible(false);
		mybutton.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final Media media = ((UploadEvent) event).getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (!media.getName().toLowerCase().endsWith(".mp3")) {
					MyMessageboxConfig.show("Mohon maaf, format file audio tidak sesuai. Langkah yang dapat dilakukan: (1) pastikan file audio berformat MP3; (2) konversi file ke format MP3 jika perlu; (3) ulangi proses upload. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				File folder = CommonMedia.getMediaDirectory();
				if (!folder.exists()) {
					folder.mkdirs();
				}
				File f = new File(folder.getAbsolutePath() + "/" + URLEncoder.encode(
						ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_" + media.getName(), "UTF-8"));

				f.createNewFile();
				FileOutputStream fileOutputStream = new FileOutputStream(f);
				try {
					IOUtils.copyLarge(media.getStreamData(), fileOutputStream);
				} catch (Exception e) {
					try {
						IOUtils.write(media.getStringData(), fileOutputStream);
					} catch (Exception ee) {
						IOUtils.write(media.getByteData(), fileOutputStream);
					}
				}

				fileOutputStream.close();

				UploadDropboxUtil.prosesBackup(f, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String urlLink = (String) arg0.getData();

						if (urlLink != null && !urlLink.trim().isEmpty()) {

							try {

								AudioPertemuan audioPertemuan = new AudioPertemuan();
								audioPertemuan.setLink(urlLink);
								audioPertemuan.setKurikulumPunyaMatakuliahDetail(
										kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
												: kurikulumPunyaMatakuliahDetail.getId());
								audioPertemuan.setKurikulumPunyaMatakuliah(
										kurikulumPunyaMatakuliah == null ? -Common.randLong()
												: kurikulumPunyaMatakuliah.getId());
								audioPertemuan.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
								try {
									audioPertemuan.setNama(media.getName());
									audioPertemuan.setKeterangan(media.getFormat());
									audioPertemuan.setType(media.getContentType());
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataAudioPertemuan.java:224");
								}
								audioPertemuan.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());

								Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
								Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
								Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
								String olehId = Common.generateOlehId(tbmuser);
								audioPertemuan.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
								audioPertemuan.setOlehId(olehId);
								audioPertemuan.setOleh(tbmuser == null ? "external_update"
										: mahasiswa != null ? mahasiswa.getNama()
												: dosen != null ? dosen.getNama()
														: pegawai != null ? pegawai.getNama()
																: (tbmuser.getUserNama()));
								Session session = StreamingHibernateUtil.getInstance().currentSession();
								session.getTransaction().begin();
								session.save(audioPertemuan);
								session.getTransaction().commit();

								audioPertemuans.add(audioPertemuan);

							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}
							StreamingHibernateUtil.getInstance().closeSession();
						}
					}
				}

				);

				final Timer timer = new Timer(1000);
				timer.setRepeats(true);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (!audioPertemuans.isEmpty()) {
							if (eventListener != null) {
								eventListener.onEvent(new Event("baru", null, audioPertemuans.get(0)));
							}
							timer.stop();
							timer.detach();
						}
					}
				});
				timer.start();

			}
		});

		return mybutton;
	}

	public static MyToolbarbuttonConfig createScanFoto(final Pertemuan pertemuan,
			final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail, final EventListener eventListener) {
		final Tbmuser tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Rekam Audio", "/img/Record-icon.png");
		HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		toolbarbutton.setVisible(Common.isSecure(request));
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event a) throws Exception {
				final Long rand = Common.randLong();
				final Timer timer = new Timer(1000);
				timer.setRepeats(true);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Map<String, Object> da = AmbilDataLampiranFileLain.fotoDrive.get(rand);
						if (da != null) {

							if (da.get("fileFotoLain") != null) {

								FileFotoLain a = (FileFotoLain) da.get("fileFotoLain");

								AudioPertemuan audioPertemuan = new AudioPertemuan();
								audioPertemuan.setKurikulumPunyaMatakuliahDetail(
										kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
												: kurikulumPunyaMatakuliahDetail.getId());
								audioPertemuan.setKurikulumPunyaMatakuliah(
										kurikulumPunyaMatakuliah == null ? -Common.randLong()
												: kurikulumPunyaMatakuliah.getId());
								audioPertemuan.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
								audioPertemuan.setNama(a.getNama());
								audioPertemuan.setKeterangan("");
								audioPertemuan.setFoto(a.getFoto());
								audioPertemuan.setType(a.getKeterangan());
								audioPertemuan.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());

								Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
								Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
								Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
								String olehId = Common.generateOlehId(tbmuser);
								audioPertemuan.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
								audioPertemuan.setOlehId(olehId);
								audioPertemuan.setOleh(tbmuser == null ? "external_update"
										: mahasiswa != null ? mahasiswa.getNama()
												: dosen != null ? dosen.getNama()
														: pegawai != null ? pegawai.getNama()
																: (tbmuser.getUserNama()));
								Session session = StreamingHibernateUtil.getInstance().currentSession();
								session.getTransaction().begin();
								session.save(audioPertemuan);
								session.getTransaction().commit();
								StreamingHibernateUtil.getInstance().closeSession();

								eventListener.onEvent(new Event("baru", null, audioPertemuan));

								timer.detach();

							} else if (da.get("drive") != null) {

								String d = (String) da.get("drive");
								String file_name = (String) da.get("file_name");
								if (d != null && !d.trim().isEmpty()) {

									AudioPertemuan audioPertemuan = new AudioPertemuan();
									audioPertemuan.setGdrive(d);
									audioPertemuan.setGdriveUsername(
											tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId());
									audioPertemuan.setKurikulumPunyaMatakuliahDetail(
											kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
													: kurikulumPunyaMatakuliahDetail.getId());
									audioPertemuan.setKurikulumPunyaMatakuliah(
											kurikulumPunyaMatakuliah == null ? -Common.randLong()
													: kurikulumPunyaMatakuliah.getId());
									audioPertemuan
											.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
									audioPertemuan.setNama(file_name);
									audioPertemuan.setKeterangan("");
									audioPertemuan.setType("audio/webm");
									audioPertemuan.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());

									Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
									Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
									Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
									String olehId = Common.generateOlehId(tbmuser);
									audioPertemuan.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
									audioPertemuan.setOlehId(olehId);
									audioPertemuan.setOleh(tbmuser == null ? "external_update"
											: mahasiswa != null ? mahasiswa.getNama()
													: dosen != null ? dosen.getNama()
															: pegawai != null ? pegawai.getNama()
																	: (tbmuser.getUserNama()));
									Session session = StreamingHibernateUtil.getInstance().currentSession();
									session.getTransaction().begin();
									session.save(audioPertemuan);
									session.getTransaction().commit();
									StreamingHibernateUtil.getInstance().closeSession();

									eventListener.onEvent(new Event("baru", null, audioPertemuan));

									timer.detach();
								}
							}
						}
					}
				});
				timer.start();

				AmbilDataLampiranFileLain.fotoDrive.put(rand, null);
				String q = "&rand=" + rand + "&clazz=" + AudioPertemuan.class.getName();

				try {

					final MyWindow window = new MyWindow("Ambil Audio", "none", true);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setBorder("none");
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					String src1 = Common.getRequestHostWithProtocol() + "/capture_audio.jsp?lokasi=false&mobile="
							+ Common.isMobile() + q;

					boolean mobile = Common.isMobile();
					String tinggi = mobile ? "650px" : "450px";

					Html html1 = new ais.ui.util.MyHtml("<iframe src=\"" + src1 + "\" style=\"width:100%;height:"
							+ tinggi + ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
					html1.setHeight(tinggi);
					Common.tampilanScroll(center).appendChild(html1);

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
						}
					});
					cancel.setParent(toolbar);
					window.setVisible(true);
					window.setHeight("97%");
					window.setWidth(mobile ? "97%" : "550px");
					window.onModal();

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataAudioPertemuan.java:440");
				}
			}
		});

		return toolbarbutton;
	}

	public MyToolbarbuttonConfig tampilkanTombolUpload(String tambahan) {

		MyToolbarbuttonConfig mybutton = new MyToolbarbuttonConfig("Upload " + tambahan + " (Maks 3 mb)",
				"/img/new.gif");
		mybutton.setUpload("true,maxsize=" + (1024 * 3));
		mybutton.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
				final Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
				final Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
				final String olehId = Common.generateOlehId(tbmuser);
				UploadEvent uploadEvent = (UploadEvent) event;

				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

				if (!media.getName().toLowerCase().endsWith(".mp3")) {
					MyMessageboxConfig.show("Mohon maaf, format file audio tidak sesuai. Langkah yang dapat dilakukan: (1) pastikan file audio berformat MP3; (2) konversi file ke format MP3 jika perlu; (3) ulangi proses upload. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				try {
					Session session = StreamingHibernateUtil.getInstance().currentSession();
					AudioPertemuan audioPertemuan = new AudioPertemuan();
					audioPertemuan.setFoto(Common.getBlobFromMedia(media));
					try {
						audioPertemuan.setNama(media.getName());
						audioPertemuan.setKeterangan(media.getFormat());
						audioPertemuan.setType(media.getContentType());
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataAudioPertemuan.java:481");
					}
					audioPertemuan.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
					audioPertemuan.setOleh(Common.getCurrentUser().getUserId());

					if (pertemuan != null) {
						audioPertemuan.setPertemuan(pertemuan.getId());
						if (pertemuan.getPerkuliahan() != null && pertemuan.getPerkuliahan().getJurusan() != null)
							audioPertemuan.setJurusan(pertemuan.getPerkuliahan().getJurusan().getId());
						if (pertemuan.getPerkuliahan() != null)
							audioPertemuan.setTahunAkademik(pertemuan.getPerkuliahan().getTahunAjaran());
					}
					if (kurikulumPunyaMatakuliah != null) {
						audioPertemuan.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah.getId());
						audioPertemuan.setJurusan(kurikulumPunyaMatakuliah.getMatakuliah().getJurusan().getId());
						audioPertemuan.setTahunAkademik(Common.getCurrentTahunAkademik());
					}

					if (kurikulumPunyaMatakuliahDetail != null) {
						audioPertemuan.setKurikulumPunyaMatakuliahDetail(kurikulumPunyaMatakuliahDetail.getId());
						audioPertemuan.setJurusan(kurikulumPunyaMatakuliahDetail.getKurikulumPunyaMatakuliah()
								.getMatakuliah().getJurusan().getId());
						audioPertemuan.setTahunAkademik(Common.getCurrentTahunAkademik());
					}

					audioPertemuan.setOlehId(olehId);
					audioPertemuan.setOleh(tbmuser == null ? "external_update"
							: mahasiswa != null ? mahasiswa.getNama()
									: dosen != null ? dosen.getNama()
											: pegawai != null ? pegawai.getNama() : (tbmuser.getUserNama()));

					session.getTransaction().begin();
					session.save(audioPertemuan);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();

					eventListener.onEvent(new Event("baru", null, audioPertemuan));
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});

		return mybutton;
	}

	public static MyToolbarbuttonConfig tampilkanTombolLinkAudio(final Pertemuan pertemuan,
			final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail, final EventListener eventListener) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Link Audio", FileFoto.icon("mp3"));
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				final MyWindow myWindow = new MyWindow("Tambah link Audio", "none", true);
				myWindow.setHeight("400px");
				myWindow.setWidth("550px");
				myWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);
				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				row.appendChild(new MyLabelBold("Link Audio"));

				row = new MyFormRow();
				row.setParent(rows);
				final Textbox isia;
				row.appendChild(isia = new Textbox());
				isia.setValue("");
				isia.setWidth("90%");
				isia.setRows(3);
				isia.select();

				row = new MyFormRow();
				row.setParent(rows);
				Common.initKeteranganSatuKolom(rows, "* Jika link lebih dari satu, pisahkan dengan spasi");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new Image(FileFoto.icon("mp3")));
				Common.initKeteranganSatuKolom(rows,
						"Link harus berupa MP3, contoh link : https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_700KB.mp3");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new Image(FileFoto.icon("drive.google")));
				Common.initKeteranganSatuKolom(rows,
						"Contoh link audio jika menggunakan dropbox : https://drive.google.com/file/d/1y0s_Px7pbtLRNnOQu-A3ehsjmcprHEIu/view");

				Common.initKeteranganSatuKolom(rows,
						"Contoh link di dalam folder drive : https://drive.google.com/drive/folders/0B1iqp0kGPjWsNDg5NWFlZjEtN2IwZC00NmZiLWE3MjktYTE2ZjZjNTZiMDY2");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new Image(FileFoto.icon("dropbox")));
				Common.initKeteranganSatuKolom(rows,
						"Contoh link audio jika menggunakan dropbox : https://www.dropbox.com/s/8gdbz7ecl8s7j86/1590806955788_kartolo%2Bdadi%2Bmanten%2Ba.mp3?dl=0");

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
						myWindow.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan Link Audio", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Tbmuser tbmuser = Common.getCurrentUser();
						final Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
						final Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
						final Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
						final String olehId = Common.generateOlehId(tbmuser);

						List<String> urls = Common.getUrls(isia.getValue().trim());
						if (urls.isEmpty()) {
							MyMessageboxConfig.show(
									"Masukkan link secara valid, perhatikan beberapa contoh link di bawah ini.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
						for (String u : urls) {

							try {
								AudioPertemuan audioPertemuan = new AudioPertemuan();
								audioPertemuan.setKeterangan("link");
								audioPertemuan.setType("link");
								audioPertemuan.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
								audioPertemuan.setOleh(Common.getCurrentUser().getUserId());
								audioPertemuan.setNama("link");
								audioPertemuan.setLink(u);

								if (pertemuan != null) {
									audioPertemuan.setPertemuan(pertemuan.getId());
									if (pertemuan.getPerkuliahan() != null
											&& pertemuan.getPerkuliahan().getJurusan() != null)
										audioPertemuan.setJurusan(pertemuan.getPerkuliahan().getJurusan().getId());
									if (pertemuan.getPerkuliahan() != null)
										audioPertemuan.setTahunAkademik(pertemuan.getPerkuliahan().getTahunAjaran());
								}
								if (kurikulumPunyaMatakuliah != null) {
									audioPertemuan.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah.getId());
									audioPertemuan
											.setJurusan(kurikulumPunyaMatakuliah.getMatakuliah().getJurusan().getId());
									audioPertemuan.setTahunAkademik(Common.getCurrentTahunAkademik());
								}

								if (kurikulumPunyaMatakuliahDetail != null) {
									audioPertemuan
											.setKurikulumPunyaMatakuliahDetail(kurikulumPunyaMatakuliahDetail.getId());
									audioPertemuan.setJurusan(kurikulumPunyaMatakuliahDetail
											.getKurikulumPunyaMatakuliah().getMatakuliah().getJurusan().getId());
									audioPertemuan.setTahunAkademik(Common.getCurrentTahunAkademik());
								}

								audioPertemuan.setOlehId(olehId);
								audioPertemuan.setOleh(tbmuser == null ? "external_update"
										: mahasiswa != null ? mahasiswa.getNama()
												: dosen != null ? dosen.getNama()
														: pegawai != null ? pegawai.getNama()
																: (tbmuser.getUserNama()));

								Session session = StreamingHibernateUtil.getInstance().currentSession();
								session.getTransaction().begin();
								session.save(audioPertemuan);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();

								eventListener.onEvent(new Event("baru", null, audioPertemuan));
							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}
						}
						myWindow.detach();
					}
				});
				save.setParent(toolbar);
				borderlayout.setParent(myWindow);
				myWindow.onModal();
			}

		});
		return button;
	}

	public static MyToolbarbuttonConfig tampilkanTombolUploadGDrive(final Pertemuan pertemuan,
			final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail, final EventListener eventListener) {
		int maxDrive = 300;
		try {
			maxDrive = Integer.parseInt(Common.getKonfigurasi("max_upload_via_drive_baru", "300").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataAudioPertemuan.java:700");
			// TODO: handle exception
		}
		final List<AudioPertemuan> audioPertemuans = new ArrayList<AudioPertemuan>();
		final Tbmuser tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig mybutton = new MyToolbarbuttonConfig(
				"Upload video/mp3 ke Drive (maks " + maxDrive + " Mb)", "/img/Google-Drive-icon.png");
		mybutton.setUpload("true,maxsize=" + (1024 * maxDrive));
		mybutton.setVisible(tbmuser != null && tbmuser.getUserId() != null);
		mybutton.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final Media media = ((UploadEvent) event).getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (!media.getName().toLowerCase().endsWith(".mp3")) {
					MyMessageboxConfig.show("Mohon maaf, format file audio tidak sesuai. Langkah yang dapat dilakukan: (1) pastikan file audio berformat MP3; (2) konversi file ke format MP3 jika perlu; (3) ulangi proses upload. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				File folder = CommonMedia.getMediaDirectory();
				if (!folder.exists()) {
					folder.mkdirs();
				}
				File f = new File(folder.getAbsolutePath() + "/" + URLEncoder.encode(
						ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_" + media.getName(), "UTF-8"));

				f.createNewFile();
				FileOutputStream fileOutputStream = new FileOutputStream(f);
				try {
					IOUtils.copyLarge(media.getStreamData(), fileOutputStream);
				} catch (Exception e) {
					try {
						IOUtils.write(media.getStringData(), fileOutputStream);
					} catch (Exception ee) {
						IOUtils.write(media.getByteData(), fileOutputStream);
					}
				}

				fileOutputStream.close();

				GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(tbmuser);
				VOPembelajaran voPembelajaran = pertemuan.ambilVOPembelajaran();
				String tugasName = "Audio pertemuan ke " + pertemuan.getPertemuanKe() + " " + pertemuan.info();

				driveUtilPerPengguna.prosesBackup(f, voPembelajaran.infoSimple(), tugasName, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
								.getData();

						if (fileUpload != null && fileUpload.getId() != null) {

							try {

								AudioPertemuan audioPertemuan = new AudioPertemuan();
								audioPertemuan.setGdrive(fileUpload.getId());
								audioPertemuan.setGdriveUsername(
										tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId());
								audioPertemuan.setKurikulumPunyaMatakuliahDetail(
										kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
												: kurikulumPunyaMatakuliahDetail.getId());
								audioPertemuan.setKurikulumPunyaMatakuliah(
										kurikulumPunyaMatakuliah == null ? -Common.randLong()
												: kurikulumPunyaMatakuliah.getId());
								audioPertemuan.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
								try {
									audioPertemuan.setNama(media.getName());
									audioPertemuan.setKeterangan(media.getFormat());
									audioPertemuan.setType(media.getContentType());
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataAudioPertemuan.java:774");
								}
								audioPertemuan.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());

								Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
								Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
								Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
								String olehId = Common.generateOlehId(tbmuser);
								audioPertemuan.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
								audioPertemuan.setOlehId(olehId);
								audioPertemuan.setOleh(tbmuser == null ? "external_update"
										: mahasiswa != null ? mahasiswa.getNama()
												: dosen != null ? dosen.getNama()
														: pegawai != null ? pegawai.getNama()
																: (tbmuser.getUserNama()));
								Session session = StreamingHibernateUtil.getInstance().currentSession();
								session.getTransaction().begin();
								session.save(audioPertemuan);
								session.getTransaction().commit();

								audioPertemuans.add(audioPertemuan);

							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}
							StreamingHibernateUtil.getInstance().closeSession();
						}
					}
				}

				);

				final Timer timer = new Timer(1000);
				timer.setRepeats(true);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (!audioPertemuans.isEmpty()) {
							if (eventListener != null) {
								eventListener.onEvent(new Event("baru", null, audioPertemuans.get(0)));
							}
							timer.stop();
							timer.detach();
						}
					}
				});
				timer.start();

			}
		});

		return mybutton;
	}

	public void display() {
		tbmuser = Common.getCurrentUser();
		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.isMobile();
		Radiogroup radiogroup = new Radiogroup();
		radiogroup.setParent(this);
		radiogroup.setHeight("100%");
		radiogroup.setWidth("100%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(radiogroup);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Borderlayout myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);

		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		South mySouth = new South();
		mySouth.setParent(myBorderlayout1);

		paging.setParent(mySouth);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);

		Grid searchgrid = new Grid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Columns columns = new Columns();

		columns.setParent(searchgrid);

		MyColumnConfig column = new MyColumnConfig("Daftar Audio");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig("Info Audio");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cari Deskripsi Audio"));
		row.appendChild(nama = new MyTextbox());
		nama.setStyle("border: 1px solid #9fb8bf;border-radius: 10px;");
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		createScanFoto(pertemuan, kurikulumPunyaMatakuliah, kurikulumPunyaMatakuliahDetail, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListener.onEvent(arg0);
			}
		}).setParent(toolbar);

		if (Common.bolehKonfigurasi("bisa_upload_audio_langsung_di_eleraning", Konfigurasi.TIDAK_AKTIF)) {
			toolbar.appendChild(tampilkanTombolUpload("audio baru "));
		}

		tampilkanTombolLinkAudio(pertemuan, kurikulumPunyaMatakuliah, kurikulumPunyaMatakuliahDetail,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						eventListener.onEvent(arg0);
					}
				}).setParent(toolbar);

		tampilkanTombolUploadGDrive(pertemuan, kurikulumPunyaMatakuliah, kurikulumPunyaMatakuliahDetail,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						eventListener.onEvent(arg0);
					}
				}).setParent(toolbar);

		tampilkanTombolUploadDropbox(pertemuan, kurikulumPunyaMatakuliah, kurikulumPunyaMatakuliahDetail,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						eventListener.onEvent(arg0);
					}
				}).setParent(toolbar);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		center.setTitle("Pilih audio yang sebelumnya pernah di-upload :");

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setParent(myCenter1);
		grid.setSclass("dgrid");

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataAudioPertemuan.this.detach();
			}
		});
		cancel.setParent(toolbar);

	}

	public Criteria initCriteria(boolean order, Session session) {
		BiodataCalonMahasiswa biodataCalonMahasiswa = tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa();
		Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
		String olehId = tbmuser == null ? "external_update;"
				: biodataCalonMahasiswa != null
						? biodataCalonMahasiswa.getNoRegistrasi() + ";" + BiodataCalonMahasiswa.class.getName()
						: mahasiswa != null ? mahasiswa.getNim() + ";" + Mahasiswa.class.getName()
								: dosen != null ? tbmuser.getUserId() + ";" + Dosen.class.getName()
										: pegawai != null ? tbmuser.getUserId() + ";" + Pegawai.class.getName()
												: (tbmuser.getUserId() + ";" + Tbmuser.class.getName());

		Criteria criteria = session.createCriteria(AudioPertemuan.class).add(Restrictions.isNull("copyDari"))
				.add(dosen != null || mahasiswa != null || biodataCalonMahasiswa != null
						|| tbmuser.getPesertaKursus() != null ? Restrictions.ilike("olehId", olehId, MatchMode.START)
								: Restrictions.sqlRestriction("true"));

		if (order) {
			criteria.addOrder(Order.desc("id"));
		}
		criteria.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.or(Restrictions.ilike("keterangan", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("keteranganTambahan", nama.getValue().trim(), MatchMode.ANYWHERE))));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = StreamingHibernateUtil.getInstance().currentSession();

		Common.initPaging(initCriteria(false, session), paging);

		List<Object[]> myAudioPertemuan = initCriteria(true, session).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(myAudioPertemuan);
		grid.setRowRenderer(new AudioPertemuanRenderer());
		grid.setModelCheckMobile(strset);
		StreamingHibernateUtil.getInstance().closeSession();
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
