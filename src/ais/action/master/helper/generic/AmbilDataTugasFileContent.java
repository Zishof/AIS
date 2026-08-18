package ais.action.master.helper.generic;

import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.dropbox.UploadDropboxUtil;
import ais.common.gdrive.GDriveUtilPerPengguna;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.TugasFileContent;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataTugasFileContent extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	private EventListener eventListener;

	private MyTextbox nama;

	private Tbmuser tbmuser;
	private Tugas tugas;
	private Siswa siswa;
	private Mahasiswa mahasiswa;
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private Paging paging;
	private CalonSiswa calonSiswa;
	private Pertemuan pertemuan;

	public AmbilDataTugasFileContent(Tugas tugas, Pertemuan pertemuan, Siswa siswa, CalonSiswa calonSiswa,
			Mahasiswa mahasiswaTemp, final BiodataCalonMahasiswa biodataCalonMahasiswa) {
		super();
		this.tugas = tugas;
		this.siswa = siswa;
		this.calonSiswa = calonSiswa;
		this.mahasiswa = mahasiswaTemp;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		this.pertemuan = pertemuan;
		tbmuser = Common.getCurrentUser();
		display();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class TugasFileContentRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			arg0.setValign("top");
			final TugasFileContent tugasFileContent = (TugasFileContent) arg1;
			arg0.setAttribute("tugasFileContent", tugasFileContent);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			final Radio checkbox = new Radio();
			checkbox.setParent(hbox);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
						Event myEvent = new Event("myEvent", event.getTarget(), tugasFileContent);
						eventListener.onEvent(myEvent);

					}
					AmbilDataTugasFileContent.this.detach();
				}
			});

			Toolbarbutton downloadButton = new MyToolbarbuttonConfig(
					tugasFileContent.getNama() != null && tugasFileContent.getNama().trim().equalsIgnoreCase("link")
							? tugasFileContent.getLink()
							: tugasFileContent.getNama(),
					tugasFileContent.iconDonwload());
			downloadButton.setTooltiptext("Lihat / Download \"" + tugasFileContent.getNama() + "\"");
			downloadButton.setAttribute("janganDisabled", true);
			hbox.appendChild(downloadButton);
			downloadButton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (tugasFileContent.getGdrive() != null) {
						tugasFileContent.tampilGDrive(null);
					} else {

						String link = tugasFileContent == null ? null
								: (tugasFileContent.getLink() == null || tugasFileContent.getLink().isEmpty() ? null
										: tugasFileContent.getLink());

						if (tugasFileContent != null
								&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
							link = tugasFileContent.createLinkUri();
							if (link != null) {
								// link = link.replaceAll("download=false", "download=true");
							}
						}

						if (tugasFileContent != null && link != null && !link.trim().isEmpty()) {

							if (tugasFileContent.bisaPreview()) {
								Common.displayWindow(tugasFileContent.merupakanGambar(), link, true, "95%", "95%", true,
										tugasFileContent);
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
									"Mohon maaf, berkas yang Anda akses tidak ditemukan. Langkah yang dapat dilakukan: (1) muat ulang halaman lalu coba kembali; (2) pastikan berkas belum dihapus atau dipindahkan; (3) hubungi pengajar atau administrator apabila berkas seharusnya tersedia.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					}
				}
			});

			new Label(tugasFileContent.getTanggal_dirubah() == null ? ""
					: Common.dateFormat5.get().format(tugasFileContent.getTanggal_dirubah())).setParent(arg0);

			String olehId = tugasFileContent.getOlehId();
			String oleh = tugasFileContent.getOleh();

			if (tugasFileContent.getMahasiswa() != null) {
				Vbox vbox = new Vbox();
				vbox.setPack("center");
				vbox.setAlign("center");
				vbox.setParent(arg0);
				Mahasiswa tbmuser = (Mahasiswa) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.idEq(tugasFileContent.getMahasiswa())).setMaxResults(1).uniqueResult();
				if (tbmuser != null) {
					CommonMedia.tampilkanGambarKecil(tbmuser).setParent(vbox);
					new Label(tbmuser.getNim()).setParent(vbox);
					new Label(tbmuser.getNama()).setParent(vbox);
				}
			}

			else {
				Common.infoDiuploadOleh(olehId, oleh, arg0);
			}

		}

	}

	public static boolean checkFile(Media media) throws Exception {
		// FIX (ERROR NullPointerException): UploadEvent.getMedia() bisa mengembalikan null
		// bila proses upload di sisi browser gagal/dibatalkan (mis. koneksi terputus,
		// berkas kosong 0 byte pada beberapa browser) -- dipakai di 170+ titik upload di
		// seluruh aplikasi, jadi digerbangi di sini sekali agar seluruh pemanggil aman.
		if (media == null || media.getName() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, berkas gagal terbaca saat diunggah. Silakan coba unggah ulang berkas tersebut.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		String name = media.getName();
		if (name.toLowerCase().endsWith(".jsp") || name.toLowerCase().endsWith(".jspx")
				|| name.toLowerCase().endsWith(".zul") || name.toLowerCase().endsWith(".html")
				|| name.toLowerCase().endsWith(".exe") || name.toLowerCase().endsWith(".sh")
				|| name.toLowerCase().endsWith(".php") || name.toLowerCase().endsWith(".htm")) {
			MyMessageboxConfig.show(
					"Mohon maaf, unggahan file dengan jenis ini tidak diizinkan. Solusinya, silakan masukkan file tersebut ke dalam format kompresi ∗.zip atau ∗.rar sebelum mengunggah.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		return true;
	}

	/**
	 * Memvalidasi bahwa file yang diunggah adalah file gambar.
	 *
	 * <p>Validasi dilakukan dengan dua lapis:</p>
	 * <ol>
	 *   <li><b>Ekstensi file</b> — nama file harus diakhiri dengan salah satu dari:
	 *       .jpg, .jpeg, .png, .gif, .svg, atau .webp (tidak peka huruf besar/kecil).</li>
	 *   <li><b>MIME/Content-Type</b> — jenis konten yang dilaporkan browser harus diawali
	 *       dengan "image/" (mis. image/jpeg, image/png, image/gif).</li>
	 * </ol>
	 *
	 * <p>File diterima jika <em>salah satu</em> dari dua lapis tersebut valid.
	 * Ini mengakomodasi browser atau klien yang tidak selalu melaporkan MIME type
	 * dengan benar. File ditolak hanya jika <em>kedua</em> lapis gagal — yaitu
	 * ekstensi bukan gambar DAN MIME type bukan "image/*".</p>
	 *
	 * <p>Jika file ditolak, pesan kesalahan ditampilkan langsung kepada pengguna
	 * via {@link ais.ui.util.MyMessageboxConfig} dan method mengembalikan
	 * {@code false}. Pemanggil wajib menghentikan proses upload jika method
	 * ini mengembalikan {@code false}.</p>
	 *
	 * <p>Contoh pemakaian di event listener upload:</p>
	 * <pre>
	 * UploadEvent uploadEvent = (UploadEvent) event;
	 * if (uploadEvent != null) {
	 *     if (!AmbilDataTugasFileContent.validasiFoto(uploadEvent.getMedia())) return;
	 *     // lanjutkan proses simpan foto...
	 * }
	 * </pre>
	 *
	 * @param media objek {@link org.zkoss.util.media.Media} dari {@link org.zkoss.zk.ui.event.UploadEvent}
	 * @return {@code true} jika file valid sebagai gambar; {@code false} jika ditolak
	 * @throws Exception jika terjadi kesalahan saat menampilkan pesan
	 */
	public static boolean validasiFoto(org.zkoss.util.media.Media media) throws Exception {
		String nama = media.getName() == null ? "" : media.getName().toLowerCase();
		String mime = media.getContentType() == null ? "" : media.getContentType().toLowerCase();

		boolean extOk = nama.endsWith(".jpg") || nama.endsWith(".jpeg")
				|| nama.endsWith(".png") || nama.endsWith(".gif")
				|| nama.endsWith(".svg") || nama.endsWith(".webp");
		boolean mimeOk = mime.startsWith("image/");

		if (!extOk && !mimeOk) {
			MyMessageboxConfig.show(
					"File yang Anda upload bukan gambar.\n\n"
					+ "Format yang diizinkan: JPG, JPEG, PNG, GIF, SVG, atau WEBP.\n"
					+ "File yang Anda pilih: " + media.getName(),
					"Format Tidak Sesuai", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		return true;
	}

	public MyToolbarbuttonConfig tampilkanTombolUpload(String tambahan) {
		MyToolbarbuttonConfig mybutton = new MyToolbarbuttonConfig(
				"Upload " + tambahan + " " + Common.ukuranLabelFileUpload(), "/img/new.gif");
		mybutton.setUpload(Common.ukuranFileUpload());
		mybutton.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Media media = ((UploadEvent) event).getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
				// hapus();
				Session session = null;
				try {

					session = StreamingHibernateUtil.getInstance().currentSession();
					TugasFileContent tugasFileContent = new TugasFileContent(tugas.getClass().getName());
					Blob blob = Common.getBlobFromMedia(media, session);
					tugasFileContent.setFoto(blob);
					tugasFileContent.setNama(media.getName());
					tugasFileContent.setFileMimeType(media.getContentType());
					tugasFileContent.setBiodataCalonMahasiswa(
							biodataCalonMahasiswa == null ? -Common.randLong() : biodataCalonMahasiswa.getId());
					tugasFileContent.setCalonSiswa(calonSiswa == null ? -Common.randLong() : calonSiswa.getId());
					tugasFileContent.setSiswa(siswa == null ? -Common.randLong() : siswa.getId());
					tugasFileContent.setMahasiswa(mahasiswa == null ? -Common.randLong() : mahasiswa.getId());
					tugasFileContent.setPertemuan(tugas == null ? -Common.randLong() : tugas.getId());
					tugasFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());

					if (tugas != null) {
						tugasFileContent.setPertemuan(tugas.getId());
					}
					if (siswa != null) {
						String nama = siswa.getNim().trim() + "_" + siswa.getNama() + "_" + media.getName();
						tugasFileContent.setNama(nama);
						tugasFileContent.setSiswa(siswa.getId());
					}
					if (mahasiswa != null) {
						String nama = mahasiswa.getNim().trim() + "_" + mahasiswa.getNama() + "_" + media.getName();
						tugasFileContent.setNama(nama);
						tugasFileContent.setMahasiswa(mahasiswa.getId());
					}

					if (biodataCalonMahasiswa != null) {
						String nama = biodataCalonMahasiswa.getNoRegistrasi().trim() + "_"
								+ biodataCalonMahasiswa.getNama() + "_" + media.getName();
						tugasFileContent.setNama(nama);
						tugasFileContent.setBiodataCalonMahasiswa(biodataCalonMahasiswa.getId());
					}

					if (calonSiswa != null) {
						String nama = calonSiswa.getNoRegistrasi().trim() + "_" + calonSiswa.getNama() + "_"
								+ media.getName();
						tugasFileContent.setNama(nama);
						tugasFileContent.setCalonSiswa(calonSiswa.getId());
					}

					Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
					Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
					Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
					String olehId = Common.generateOlehId(tbmuser);
					tugasFileContent.setOlehId(olehId);
					tugasFileContent.setOleh(tbmuser == null ? "external_update"
							: mahasiswa != null ? mahasiswa.getNama()
									: dosen != null ? dosen.getNama()
											: pegawai != null ? pegawai.getNama() : (tbmuser.getUserNama()));

					session.getTransaction().begin();
					session.save(tugasFileContent);
					session.getTransaction().commit();

					eventListener.onEvent(new Event("baru", null, tugasFileContent));

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataTugasFileContent.java:380");
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				} finally {
					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
					StreamingHibernateUtil.getInstance().closeSession();
				}
			}
		});

		return mybutton;
	}

	private TugasFileContent tugasFileContent = null;
	private MyCheckboxConfig tampilkan;
	private Toolbar toolbar;
	private MyBorderlayout myBorderlayout1;

	public MyToolbarbuttonConfig tampilkanTombolUploadGDrive(String tambahan) {

		int maxDrive = 300;
		try {
			maxDrive = Integer.parseInt(Common.getKonfigurasi("max_upload_via_drive_baru", "300").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataTugasFileContent.java:407");
			// TODO: handle exception
		}

		final Tbmuser tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig mybutton = new MyToolbarbuttonConfig(
				"Upload " + tambahan + " ke Drive (maks " + maxDrive + " Mb)", "/img/Google-Drive-icon.png");
		mybutton.setUpload("true,maxsize=" + (1024 * maxDrive));
		mybutton.setVisible(tbmuser != null && tbmuser.getUserId() != null);
		mybutton.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				tugasFileContent = null;
				final Media media = ((UploadEvent) event).getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;

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
				String tugasName = tugas.getJudultugas() + "-Pertemuan ke " + pertemuan.getPertemuanKe() + " "
						+ pertemuan.info();

				driveUtilPerPengguna.prosesBackup(f, voPembelajaran.infoSimple(), tugasName, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
								.getData();

						if (fileUpload != null && fileUpload.getId() != null) {

							try {

								Session session = StreamingHibernateUtil.getInstance().currentSession();
								tugasFileContent = new TugasFileContent(tugas.getClass().getName());
								tugasFileContent.setGdrive(fileUpload.getId());
								tugasFileContent.setGdriveUsername(
										tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId());
								tugasFileContent.setNama(media.getName());
								tugasFileContent.setFileMimeType(media.getContentType());
								tugasFileContent
										.setBiodataCalonMahasiswa(biodataCalonMahasiswa == null ? -Common.randLong()
												: biodataCalonMahasiswa.getId());
								tugasFileContent
										.setCalonSiswa(calonSiswa == null ? -Common.randLong() : calonSiswa.getId());
								tugasFileContent.setSiswa(siswa == null ? -Common.randLong() : siswa.getId());
								tugasFileContent
										.setMahasiswa(mahasiswa == null ? -Common.randLong() : mahasiswa.getId());
								tugasFileContent.setPertemuan(tugas == null ? -Common.randLong() : tugas.getId());
								tugasFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());

								if (tugas != null) {
									tugasFileContent.setPertemuan(tugas.getId());
								}
								if (siswa != null) {
									String nama = siswa.getNim().trim() + "_" + siswa.getNama() + "_" + media.getName();
									tugasFileContent.setNama(nama);
									tugasFileContent.setMahasiswa(mahasiswa.getId());
								}
								if (mahasiswa != null) {
									String nama = mahasiswa.getNim().trim() + "_" + mahasiswa.getNama() + "_"
											+ media.getName();
									tugasFileContent.setNama(nama);
									tugasFileContent.setMahasiswa(mahasiswa.getId());
								}

								if (biodataCalonMahasiswa != null) {
									String nama = biodataCalonMahasiswa.getNoRegistrasi().trim() + "_"
											+ biodataCalonMahasiswa.getNama() + "_" + media.getName();
									tugasFileContent.setNama(nama);
									tugasFileContent.setBiodataCalonMahasiswa(biodataCalonMahasiswa.getId());
								}

								if (calonSiswa != null) {
									String nama = calonSiswa.getNoRegistrasi().trim() + "_" + calonSiswa.getNama() + "_"
											+ media.getName();
									tugasFileContent.setNama(nama);
									tugasFileContent.setCalonSiswa(calonSiswa.getId());
								}

								Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
								Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
								Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
								String olehId = Common.generateOlehId(tbmuser);
								tugasFileContent.setOlehId(olehId);
								tugasFileContent.setOleh(tbmuser == null ? "external_update"
										: mahasiswa != null ? mahasiswa.getNama()
												: dosen != null ? dosen.getNama()
														: pegawai != null ? pegawai.getNama()
																: (tbmuser.getUserNama()));

								session.getTransaction().begin();
								session.save(tugasFileContent);
								session.getTransaction().commit();
								// session.disconnect();
								if (session.isOpen()) {
									session.disconnect();
									session.close();
								}
								StreamingHibernateUtil.getInstance().closeSession();

							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}

						}
					}
				}

				);

				final Timer timer = new Timer(1000);
				timer.setRepeats(true);
				timer.setParent(AmbilDataTugasFileContent.this);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tugasFileContent != null && tugasFileContent.getId() != null) {
							eventListener.onEvent(new Event("baru", null, tugasFileContent));
							AmbilDataTugasFileContent.this.detach();
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

	public MyToolbarbuttonConfig tampilkanTombolUploadDropbox(String tambahan) {
		final Tbmuser tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig mybutton = new MyToolbarbuttonConfig("Upload " + tambahan + " ke Dropbox (maks 500 Mb)",
				FileFoto.icon("dropbox"));
		mybutton.setUpload("true,maxsize=" + (1024 * 500));
		mybutton.setVisible(false);
		mybutton.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				tugasFileContent = null;
				final Media media = ((UploadEvent) event).getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;

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

								Session session = StreamingHibernateUtil.getInstance().currentSession();
								tugasFileContent = new TugasFileContent(tugas.getClass().getName());
								tugasFileContent.setNama(media.getName());
								tugasFileContent.setFileMimeType(media.getContentType());
								tugasFileContent
										.setBiodataCalonMahasiswa(biodataCalonMahasiswa == null ? -Common.randLong()
												: biodataCalonMahasiswa.getId());
								tugasFileContent
										.setCalonSiswa(calonSiswa == null ? -Common.randLong() : calonSiswa.getId());
								tugasFileContent.setSiswa(siswa == null ? -Common.randLong() : siswa.getId());
								tugasFileContent
										.setMahasiswa(mahasiswa == null ? -Common.randLong() : mahasiswa.getId());
								tugasFileContent.setPertemuan(tugas == null ? -Common.randLong() : tugas.getId());
								tugasFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());
								tugasFileContent.setLink(urlLink);
								if (tugas != null) {
									tugasFileContent.setPertemuan(tugas.getId());
								}
								if (siswa != null) {
									String nama = siswa.getNim().trim() + "_" + siswa.getNama() + "_" + media.getName();
									tugasFileContent.setNama(nama);
									tugasFileContent.setMahasiswa(mahasiswa.getId());
								}
								if (mahasiswa != null) {
									String nama = mahasiswa.getNim().trim() + "_" + mahasiswa.getNama() + "_"
											+ media.getName();
									tugasFileContent.setNama(nama);
									tugasFileContent.setMahasiswa(mahasiswa.getId());
								}

								if (biodataCalonMahasiswa != null) {
									String nama = biodataCalonMahasiswa.getNoRegistrasi().trim() + "_"
											+ biodataCalonMahasiswa.getNama() + "_" + media.getName();
									tugasFileContent.setNama(nama);
									tugasFileContent.setBiodataCalonMahasiswa(biodataCalonMahasiswa.getId());
								}

								if (calonSiswa != null) {
									String nama = calonSiswa.getNoRegistrasi().trim() + "_" + calonSiswa.getNama() + "_"
											+ media.getName();
									tugasFileContent.setNama(nama);
									tugasFileContent.setCalonSiswa(calonSiswa.getId());
								}

								Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
								Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
								Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
								String olehId = Common.generateOlehId(tbmuser);
								tugasFileContent.setOlehId(olehId);
								tugasFileContent.setOleh(tbmuser == null ? "external_update"
										: mahasiswa != null ? mahasiswa.getNama()
												: dosen != null ? dosen.getNama()
														: pegawai != null ? pegawai.getNama()
																: (tbmuser.getUserNama()));

								session.getTransaction().begin();
								session.save(tugasFileContent);
								session.getTransaction().commit();
								// session.disconnect();
								if (session.isOpen()) {
									session.disconnect();
									session.close();
								}
								StreamingHibernateUtil.getInstance().closeSession();

							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}

						}
					}
				}

				);

				final Timer timer = new Timer(1000);
				timer.setRepeats(true);
				timer.setParent(AmbilDataTugasFileContent.this);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tugasFileContent != null && tugasFileContent.getId() != null) {
							eventListener.onEvent(new Event("baru", null, tugasFileContent));
							AmbilDataTugasFileContent.this.detach();
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

	public MyToolbarbuttonConfig createScanLayar() {
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Rekam Layar", "/img/Monitor-3-icon.png");
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

								Session session = StreamingHibernateUtil.getInstance().currentSession();
								tugasFileContent = new TugasFileContent(tugas.getClass().getName());
								tugasFileContent.setFoto(a.getFoto());
								tugasFileContent.setNama(a.getNama());
								tugasFileContent.setFileMimeType(a.getKeterangan());
								tugasFileContent
										.setBiodataCalonMahasiswa(biodataCalonMahasiswa == null ? -Common.randLong()
												: biodataCalonMahasiswa.getId());
								tugasFileContent
										.setCalonSiswa(calonSiswa == null ? -Common.randLong() : calonSiswa.getId());
								tugasFileContent.setSiswa(siswa == null ? -Common.randLong() : siswa.getId());
								tugasFileContent
										.setMahasiswa(mahasiswa == null ? -Common.randLong() : mahasiswa.getId());
								tugasFileContent.setPertemuan(tugas == null ? -Common.randLong() : tugas.getId());
								tugasFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());

								if (tugas != null) {
									tugasFileContent.setPertemuan(tugas.getId());
								}
								if (siswa != null) {
									String nama = siswa.getNim().trim() + "_" + siswa.getNama() + "_" + "gambar.jpg";
									tugasFileContent.setNama(nama);
									tugasFileContent.setMahasiswa(mahasiswa.getId());
								}
								if (mahasiswa != null) {
									String nama = mahasiswa.getNim().trim() + "_" + mahasiswa.getNama() + "_"
											+ "gambar.jpg";
									tugasFileContent.setNama(nama);
									tugasFileContent.setMahasiswa(mahasiswa.getId());
								}

								if (biodataCalonMahasiswa != null) {
									String nama = biodataCalonMahasiswa.getNoRegistrasi().trim() + "_"
											+ biodataCalonMahasiswa.getNama() + "_" + "gambar.jpg";
									tugasFileContent.setNama(nama);
									tugasFileContent.setBiodataCalonMahasiswa(biodataCalonMahasiswa.getId());
								}

								if (calonSiswa != null) {
									String nama = calonSiswa.getNoRegistrasi().trim() + "_" + calonSiswa.getNama() + "_"
											+ "gambar.jpg";
									tugasFileContent.setNama(nama);
									tugasFileContent.setCalonSiswa(calonSiswa.getId());
								}

								Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
								Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
								Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
								String olehId = Common.generateOlehId(tbmuser);
								tugasFileContent.setOlehId(olehId);
								tugasFileContent.setOleh(tbmuser == null ? "external_update"
										: mahasiswa != null ? mahasiswa.getNama()
												: dosen != null ? dosen.getNama()
														: pegawai != null ? pegawai.getNama()
																: (tbmuser.getUserNama()));

								session.getTransaction().begin();
								session.save(tugasFileContent);
								session.getTransaction().commit();
								// session.disconnect();
								if (session.isOpen()) {
									session.disconnect();
									session.close();
								}
								StreamingHibernateUtil.getInstance().closeSession();

								eventListener.onEvent(new Event("baru", null, tugasFileContent));

								AmbilDataTugasFileContent.this.detach();

								timer.detach();

							} else if (da.get("drive") != null) {

								String d = (String) da.get("drive");
								String file_name = (String) da.get("file_name");
								if (d != null && !d.trim().isEmpty()) {
									Session session = StreamingHibernateUtil.getInstance().currentSession();
									tugasFileContent = new TugasFileContent(tugas.getClass().getName());
									tugasFileContent.setGdrive(d);
									tugasFileContent.setGdriveUsername(
											tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId());
									tugasFileContent.setNama(file_name);
									tugasFileContent.setFileMimeType("image/jpg");
									tugasFileContent
											.setBiodataCalonMahasiswa(biodataCalonMahasiswa == null ? -Common.randLong()
													: biodataCalonMahasiswa.getId());
									tugasFileContent.setCalonSiswa(
											calonSiswa == null ? -Common.randLong() : calonSiswa.getId());
									tugasFileContent.setSiswa(siswa == null ? -Common.randLong() : siswa.getId());
									tugasFileContent
											.setMahasiswa(mahasiswa == null ? -Common.randLong() : mahasiswa.getId());
									tugasFileContent.setPertemuan(tugas == null ? -Common.randLong() : tugas.getId());
									tugasFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());

									if (tugas != null) {
										tugasFileContent.setPertemuan(tugas.getId());
									}
									if (siswa != null) {
										String nama = siswa.getNim().trim() + "_" + siswa.getNama() + "_"
												+ "gambar.jpg";
										tugasFileContent.setNama(nama);
										tugasFileContent.setMahasiswa(mahasiswa.getId());
									}
									if (mahasiswa != null) {
										String nama = mahasiswa.getNim().trim() + "_" + mahasiswa.getNama() + "_"
												+ "gambar.jpg";
										tugasFileContent.setNama(nama);
										tugasFileContent.setMahasiswa(mahasiswa.getId());
									}

									if (biodataCalonMahasiswa != null) {
										String nama = biodataCalonMahasiswa.getNoRegistrasi().trim() + "_"
												+ biodataCalonMahasiswa.getNama() + "_" + "gambar.jpg";
										tugasFileContent.setNama(nama);
										tugasFileContent.setBiodataCalonMahasiswa(biodataCalonMahasiswa.getId());
									}

									if (calonSiswa != null) {
										String nama = calonSiswa.getNoRegistrasi().trim() + "_" + calonSiswa.getNama()
												+ "_" + "gambar.jpg";
										tugasFileContent.setNama(nama);
										tugasFileContent.setCalonSiswa(calonSiswa.getId());
									}

									Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
									Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
									Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
									String olehId = Common.generateOlehId(tbmuser);
									tugasFileContent.setOlehId(olehId);
									tugasFileContent.setOleh(tbmuser == null ? "external_update"
											: mahasiswa != null ? mahasiswa.getNama()
													: dosen != null ? dosen.getNama()
															: pegawai != null ? pegawai.getNama()
																	: (tbmuser.getUserNama()));

									session.getTransaction().begin();
									session.save(tugasFileContent);
									session.getTransaction().commit();
									// session.disconnect();
									if (session.isOpen()) {
										session.disconnect();
										session.close();
									}
									StreamingHibernateUtil.getInstance().closeSession();

									eventListener.onEvent(new Event("baru", null, tugasFileContent));

									AmbilDataTugasFileContent.this.detach();

									timer.detach();
								}
							}
						}
					}
				});
				timer.start();

				AmbilDataLampiranFileLain.fotoDrive.put(rand, null);
				String clazzs = StringUtils.split(tugas.getClass().getName(), "_")[0];
				String q = "&rand=" + rand + "&clazz=" + clazzs;

				try {

					final MyWindow window = new MyWindow("Rekam Layar", "none", true);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setBorder("none");
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);
					String src = Common.getRequestHostWithProtocol() + "/capture_screen.jsp?lokasi=false&mobile="
							+ Common.isMobile() + q;

					boolean mobile = Common.isMobile();
					String tinggi = mobile ? "850px" : "550px";

					Html html = new ais.ui.util.MyHtml("<iframe src=\"" + src + "\" style=\"width:100%;height:" + tinggi
							+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
					html.setHeight(tinggi);
					Common.tampilanScroll(center).appendChild(html);

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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataTugasFileContent.java:942");
				}
			}
		});

		return toolbarbutton;
	}

	public MyToolbarbuttonConfig createScanFoto() {
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Gunakan Kamera", "/img/camera-icon.png");
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

								Session session = StreamingHibernateUtil.getInstance().currentSession();
								tugasFileContent = new TugasFileContent(tugas.getClass().getName());
								tugasFileContent.setFoto(a.getFoto());
								tugasFileContent.setNama(a.getNama());
								tugasFileContent.setFileMimeType(a.getKeterangan());
								tugasFileContent
										.setBiodataCalonMahasiswa(biodataCalonMahasiswa == null ? -Common.randLong()
												: biodataCalonMahasiswa.getId());
								tugasFileContent
										.setCalonSiswa(calonSiswa == null ? -Common.randLong() : calonSiswa.getId());
								tugasFileContent.setSiswa(siswa == null ? -Common.randLong() : siswa.getId());
								tugasFileContent
										.setMahasiswa(mahasiswa == null ? -Common.randLong() : mahasiswa.getId());
								tugasFileContent.setPertemuan(tugas == null ? -Common.randLong() : tugas.getId());
								tugasFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());

								if (tugas != null) {
									tugasFileContent.setPertemuan(tugas.getId());
								}
								if (siswa != null) {
									String nama = siswa.getNim().trim() + "_" + siswa.getNama() + "_" + "gambar.jpg";
									tugasFileContent.setNama(nama);
									tugasFileContent.setMahasiswa(mahasiswa.getId());
								}
								if (mahasiswa != null) {
									String nama = mahasiswa.getNim().trim() + "_" + mahasiswa.getNama() + "_"
											+ "gambar.jpg";
									tugasFileContent.setNama(nama);
									tugasFileContent.setMahasiswa(mahasiswa.getId());
								}

								if (biodataCalonMahasiswa != null) {
									String nama = biodataCalonMahasiswa.getNoRegistrasi().trim() + "_"
											+ biodataCalonMahasiswa.getNama() + "_" + "gambar.jpg";
									tugasFileContent.setNama(nama);
									tugasFileContent.setBiodataCalonMahasiswa(biodataCalonMahasiswa.getId());
								}

								if (calonSiswa != null) {
									String nama = calonSiswa.getNoRegistrasi().trim() + "_" + calonSiswa.getNama() + "_"
											+ "gambar.jpg";
									tugasFileContent.setNama(nama);
									tugasFileContent.setCalonSiswa(calonSiswa.getId());
								}

								Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
								Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
								Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
								String olehId = Common.generateOlehId(tbmuser);
								tugasFileContent.setOlehId(olehId);
								tugasFileContent.setOleh(tbmuser == null ? "external_update"
										: mahasiswa != null ? mahasiswa.getNama()
												: dosen != null ? dosen.getNama()
														: pegawai != null ? pegawai.getNama()
																: (tbmuser.getUserNama()));

								session.getTransaction().begin();
								session.save(tugasFileContent);
								session.getTransaction().commit();
								// session.disconnect();
								if (session.isOpen()) {
									session.disconnect();
									session.close();
								}
								StreamingHibernateUtil.getInstance().closeSession();

								eventListener.onEvent(new Event("baru", null, tugasFileContent));

								AmbilDataTugasFileContent.this.detach();

								timer.detach();

							} else if (da.get("drive") != null) {

								String d = (String) da.get("drive");
								String file_name = (String) da.get("file_name");
								if (d != null && !d.trim().isEmpty()) {
									Session session = StreamingHibernateUtil.getInstance().currentSession();
									tugasFileContent = new TugasFileContent(tugas.getClass().getName());
									tugasFileContent.setGdrive(d);
									tugasFileContent.setGdriveUsername(
											tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId());
									tugasFileContent.setNama(file_name);
									tugasFileContent.setFileMimeType("image/jpg");
									tugasFileContent
											.setBiodataCalonMahasiswa(biodataCalonMahasiswa == null ? -Common.randLong()
													: biodataCalonMahasiswa.getId());
									tugasFileContent.setCalonSiswa(
											calonSiswa == null ? -Common.randLong() : calonSiswa.getId());
									tugasFileContent.setSiswa(siswa == null ? -Common.randLong() : siswa.getId());
									tugasFileContent
											.setMahasiswa(mahasiswa == null ? -Common.randLong() : mahasiswa.getId());
									tugasFileContent.setPertemuan(tugas == null ? -Common.randLong() : tugas.getId());
									tugasFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());

									if (tugas != null) {
										tugasFileContent.setPertemuan(tugas.getId());
									}
									if (siswa != null) {
										String nama = siswa.getNim().trim() + "_" + siswa.getNama() + "_"
												+ "gambar.jpg";
										tugasFileContent.setNama(nama);
										tugasFileContent.setMahasiswa(mahasiswa.getId());
									}
									if (mahasiswa != null) {
										String nama = mahasiswa.getNim().trim() + "_" + mahasiswa.getNama() + "_"
												+ "gambar.jpg";
										tugasFileContent.setNama(nama);
										tugasFileContent.setMahasiswa(mahasiswa.getId());
									}

									if (biodataCalonMahasiswa != null) {
										String nama = biodataCalonMahasiswa.getNoRegistrasi().trim() + "_"
												+ biodataCalonMahasiswa.getNama() + "_" + "gambar.jpg";
										tugasFileContent.setNama(nama);
										tugasFileContent.setBiodataCalonMahasiswa(biodataCalonMahasiswa.getId());
									}

									if (calonSiswa != null) {
										String nama = calonSiswa.getNoRegistrasi().trim() + "_" + calonSiswa.getNama()
												+ "_" + "gambar.jpg";
										tugasFileContent.setNama(nama);
										tugasFileContent.setCalonSiswa(calonSiswa.getId());
									}

									Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
									Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
									Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
									String olehId = Common.generateOlehId(tbmuser);
									tugasFileContent.setOlehId(olehId);
									tugasFileContent.setOleh(tbmuser == null ? "external_update"
											: mahasiswa != null ? mahasiswa.getNama()
													: dosen != null ? dosen.getNama()
															: pegawai != null ? pegawai.getNama()
																	: (tbmuser.getUserNama()));

									session.getTransaction().begin();
									session.save(tugasFileContent);
									session.getTransaction().commit();
									// session.disconnect();
									if (session.isOpen()) {
										session.disconnect();
										session.close();
									}
									StreamingHibernateUtil.getInstance().closeSession();

									eventListener.onEvent(new Event("baru", null, tugasFileContent));

									AmbilDataTugasFileContent.this.detach();

									timer.detach();
								}
							}
						}
					}
				});
				timer.start();

				AmbilDataLampiranFileLain.fotoDrive.put(rand, null);
				String clazzs = StringUtils.split(tugas.getClass().getName(), "_")[0];
				String q = "&rand=" + rand + "&clazz=" + clazzs;

				try {

					final MyWindow window = new MyWindow("Ambil Foto / Video", "none", true);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setBorder("none");
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);
					String src = Common.getRequestHostWithProtocol() + "/capture.jsp?lokasi=false&mobile="
							+ Common.isMobile() + q;
					String src1 = Common.getRequestHostWithProtocol() + "/capture_video.jsp?lokasi=false&mobile="
							+ Common.isMobile() + q;

					Tabbox tabbox = new Tabbox();
					tabbox.setHeight("100%");
					tabbox.setWidth("100%");
					tabbox.setParent(center);
					Tabs myTabs = new Tabs();
					myTabs.setParent(tabbox);

					Tabpanels mytabpanels = new Tabpanels();
					mytabpanels.setParent(tabbox);

					Tab tabUtama = new Tab("Foto");
					myTabs.appendChild(tabUtama);

					boolean mobile = Common.isMobile();
					String tinggi = mobile ? "850px" : "550px";

					Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
					tabpanelUtama.setHeight(tinggi);
					tabpanelUtama.setWidth("100%");
					tabpanelUtama.setParent(mytabpanels);

					Html html = new ais.ui.util.MyHtml("<iframe src=\"" + src + "\" style=\"width:100%;height:" + tinggi
							+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
					html.setHeight(tinggi);
					Common.tampilanScroll(tabpanelUtama).appendChild(html);

					Tab tabUtama1 = new Tab("Video");
					myTabs.appendChild(tabUtama1);

					Tabpanel tabpanelUtama1 = new ais.ui.util.MyTabpanel();
					tabpanelUtama1.setHeight(tinggi);
					tabpanelUtama1.setWidth("100%");
					tabpanelUtama1.setParent(mytabpanels);

					Html html1 = new ais.ui.util.MyHtml("<iframe src=\"" + src1 + "\" style=\"width:100%;height:"
							+ tinggi + ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
					html1.setHeight(tinggi);
					Common.tampilanScroll(tabpanelUtama1).appendChild(html1);

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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataTugasFileContent.java:1213");
				}
			}
		});

		return toolbarbutton;
	}

	public void display() {

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Radiogroup radiogroup = new Radiogroup();
		radiogroup.setParent(this);
		radiogroup.setHeight("100%");
		radiogroup.setWidth("100%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(radiogroup);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);

		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		North myNorth = new North();
		ais.ui.util.ZkCompat.setFlex(myNorth, true);
		myNorth.setParent(myBorderlayout1);

		Grid searchgrid = new Grid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(myNorth);

		Columns columns = new Columns();

		columns.setParent(searchgrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("20%");

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama File Tugas"));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		MyToolbarbuttonConfig buttonCari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		buttonCari.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		buttonCari.setParent(row);

		South mySouth = new South();
		mySouth.setParent(myBorderlayout1);

		paging.setParent(mySouth);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setHeight("320px");
		north.setStyle("overflow:auto; background:#f8fafc;");

		toolbar = new Toolbar();
		toolbar.setHeight("200px");
		toolbar.setOrient("vertical");
		toolbar.setStyle("display:flex; flex-direction:column; gap:10px; width:100%; "
				+ "box-sizing:border-box; padding:14px 12px;");
		toolbar.setParent(north);

		MyToolbarbuttonConfig kameraBtn = createScanFoto();
		kameraBtn.setSclass("ais-upload-opt-kamera");
		toolbar.appendChild(kameraBtn);

		if (!Common.isMobile()) {
			MyToolbarbuttonConfig layarBtn = createScanLayar();
			layarBtn.setSclass("ais-upload-opt-layar");
			toolbar.appendChild(layarBtn);
		}

		if (Common.bolehKonfigurasi("boleh_upload_file_langsung")) {
			MyToolbarbuttonConfig fileBtn = tampilkanTombolUpload("file tugas ");
			fileBtn.setSclass("ais-upload-opt-file");
			toolbar.appendChild(fileBtn);
		}

		MyToolbarbuttonConfig driveBtn = tampilkanTombolUploadGDrive("tugas ");
		driveBtn.setSclass("ais-upload-opt-drive");
		toolbar.appendChild(driveBtn);

		MyToolbarbuttonConfig dropboxBtn = tampilkanTombolUploadDropbox("tugas ");
		dropboxBtn.setSclass("ais-upload-opt-dropbox");
		toolbar.appendChild(dropboxBtn);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Link Tugas", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				final Window myWindow = new Window("Tambah link tugas", "none", true);
				myWindow.setHeight("95%");
				myWindow.setWidth("850px");
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

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new MyLabelBold("Link Tugas"));

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

				row.appendChild(new Image(FileFoto.icon("drive.google")));

				Common.initKeteranganSatuKolom(rows,
						"Contoh link jika menggunakan google drive : https://drive.google.com/file/d/1jqqlH3bqCE9IcShsooF_RqOYpLehKiRV/view?usp=sharing");
				Common.initKeteranganSatuKolom(rows,
						"atau contoh di google drive : atau juga bisa https://drive.google.com/open?id=1jqqlH3bqCE9IcShsooF_RqOYpLehKiRV");

				Common.initKeteranganSatuKolom(rows,
						"Contoh link di dalam folder drive : https://drive.google.com/drive/folders/0B1iqp0kGPjWsNDg5NWFlZjEtN2IwZC00NmZiLWE3MjktYTE2ZjZjNTZiMDY2");

				row = new MyFormRow();
				row.setParent(rows);

				row.appendChild(new Image(FileFoto.icon("youtube")));
				Common.initKeteranganSatuKolom(rows,
						"Contoh link video jika menggunakan youtube : https://www.youtube.com/watch?v=Ed8Uw9b_jyk");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new Image(FileFoto.icon("instagram")));
				Common.initKeteranganSatuKolom(rows,
						"Contoh link jika menggunakan Instagram : https://www.instagram.com/p/fA9uwTtkSN");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new Image(FileFoto.icon("dropbox")));
				Common.initKeteranganSatuKolom(rows,
						"Contoh link jika menggunakan dropbox : https://www.dropbox.com/s/fshcbd82hnj0f60/1590735986510_Funny%2BCat%2BFaces%2BCompilation%2B2014%2B%255BNEW%255D.mp4?dl=0");

				row = new MyFormRow();
				row.setParent(rows);

				row.appendChild(new Image(FileFoto.icon("mp3")));
				Common.initKeteranganSatuKolom(rows,
						"Contoh link MP3 : https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_700KB.mp3");

				row = new MyFormRow();
				row.setParent(rows);

				row.appendChild(new Image(FileFoto.icon("pdf")));
				Common.initKeteranganSatuKolom(rows,
						"Contoh link pdf : https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf");

				row = new MyFormRow();
				row.setParent(rows);

				row.appendChild(new Image(FileFoto.icon("facebook")));

				Common.initKeteranganSatuKolom(rows,
						"Contoh post facebook : https://www.facebook.com/20531316728/posts/10154009990506729/");

				row = new MyFormRow();
				row.setParent(rows);

				row.appendChild(new Image(FileFoto.icon("twitter")));

				Common.initKeteranganSatuKolom(rows,
						"Contoh post twitter : https://twitter.com/Interior/status/463440424141459456");

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
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan Link Tugas", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						List<String> urls = Common.getUrls(isia.getValue().trim());
						if (urls.isEmpty()) {
							MyMessageboxConfig.show(
									"Masukkan link secara valid, perhatikan beberapa contoh link di bawah ini.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						String type = "";

						try {
							URL url = new URL(urls.get(0));
							HttpURLConnection huc = (HttpURLConnection) url.openConnection();
							type = huc.getHeaderField("Content-Type");
//							System.out.println("type => " + type);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataTugasFileContent.java:1460");
						}

						try {
							TugasFileContent tugasFileContent = new TugasFileContent(tugas.getClass().getName());
							tugasFileContent.setKeterangan("link");
							tugasFileContent.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
							tugasFileContent.setOleh(Common.getCurrentUser().getUserId());
							tugasFileContent.setNama("link");
							tugasFileContent.setLink(urls.get(0));
							tugasFileContent.setFileMimeType(type);

							if (tugas != null) {
								tugasFileContent.setPertemuan(tugas.getId());
							}
							if (siswa != null) {
								String nama = siswa.getNim().trim() + "_" + siswa.getNama() + "_tugas_berupa_link.txt";
								tugasFileContent.setNama(nama);
								tugasFileContent.setMahasiswa(mahasiswa.getId());
							}
							if (mahasiswa != null) {
								String nama = mahasiswa.getNim().trim() + "_" + mahasiswa.getNama()
										+ "_tugas_berupa_link.txt";
								tugasFileContent.setNama(nama);
								tugasFileContent.setMahasiswa(mahasiswa.getId());
							}

							if (biodataCalonMahasiswa != null) {
								String nama = biodataCalonMahasiswa.getNoRegistrasi().trim() + "_"
										+ biodataCalonMahasiswa.getNama() + "_tugas_berupa_link.txt";
								tugasFileContent.setNama(nama);
								tugasFileContent.setBiodataCalonMahasiswa(biodataCalonMahasiswa.getId());
							}

							if (calonSiswa != null) {
								String nama = calonSiswa.getNoRegistrasi().trim() + "_" + calonSiswa.getNama()
										+ "_tugas_berupa_link.txt";
								tugasFileContent.setNama(nama);
								tugasFileContent.setCalonSiswa(calonSiswa.getId());
							}

							Session session = StreamingHibernateUtil.getInstance().currentSession();
							session.getTransaction().begin();
							session.save(tugasFileContent);
							session.getTransaction().commit();
							// session.disconnect();
							if (session.isOpen()) {
								session.disconnect();
								session.close();
							}
							StreamingHibernateUtil.getInstance().closeSession();

							eventListener.onEvent(new Event("baru", null, tugasFileContent));

						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}
						myWindow.detach();
					}
				});
				save.setParent(toolbar);
				borderlayout.setParent(myWindow);
				myWindow.onModal();
			}

		});
		button.setSclass("ais-upload-opt-link");
		button.setParent(toolbar);

		tampilkan = new MyCheckboxConfig("Tampilkan tugas-tugas yg telah diupload sebelumnya");
		toolbar.appendChild(tampilkan);
		tampilkan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				onSearchDefault(arg0);
			}
		});

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataTugasFileContent.this.detach();
			}
		});
		cancel.setParent(toolbar);

		center.setTitle("Pilih daftar tugas yang sebelumnya pernah di-upload, jika file tugas lebih besar dari "
				+ Common.ukuranLabelFileUpload()
				+ ", maka tugas harus di-upload di tempat lain dan klik tambahkan link di atas, kemudian masukkan link dari file yang baru saja di-upload.");

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setParent(myCenter1);

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("File Tugas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu Upload");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Diupload oleh");
		column.setWidth("15%");

	}

	public Criteria initCriteria(boolean order, Session session) {
		Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		String olehId = Common.generateOlehId(tbmuser);

		boolean hanyaBolehMelihatlampirannyaSendiri = Common.bolehKonfigurasi("hanya_boleh_melihat_lampirannya_sendiri", Konfigurasi.TIDAK_AKTIF);

		Criteria criteria = session.createCriteria(TugasFileContent.class).add(Restrictions.isNull("copyDari"))

				.add(calonSiswa != null && calonSiswa.getId() != null
						? Restrictions.eq("calonSiswa", calonSiswa.getId())
						: siswa != null && siswa.getId() != null ? Restrictions.eq("siswa", siswa.getId())
								: biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null
										? Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId())
										: mahasiswa != null && mahasiswa.getId() != null
												? Restrictions.eq("mahasiswa", mahasiswa.getId())
												: dosen != null || hanyaBolehMelihatlampirannyaSendiri
														? Restrictions.ilike("olehId", olehId, MatchMode.START)
														: Restrictions.sqlRestriction("true"))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE));
		if (order) {
			criteria.addOrder(Order.desc("id"));
		}
		return criteria;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onSearchDefault(Event event) {
		boolean mobile = Common.isMobile();
		if (tampilkan.isChecked()) {
			toolbar.setOrient("vertical");
			myBorderlayout1.setVisible(true);
			AmbilDataTugasFileContent.this.setWidth("90%");
			AmbilDataTugasFileContent.this.setHeight("95%");
			AmbilDataTugasFileContent.this.setPosition("left,top");

			Session session = StreamingHibernateUtil.getInstance().currentSession();
			Common.initPaging(initCriteria(false, session), paging);

			List<TugasFileContent> myTugasFileContent = initCriteria(true, session)
					.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

			ListModel strset = new SimpleListModel(myTugasFileContent);
			grid.setRowRenderer(new TugasFileContentRenderer());
			grid.setModelCheckMobile(strset);
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			StreamingHibernateUtil.getInstance().closeSession();
		} else {

			myBorderlayout1.setVisible(false);
			toolbar.setOrient("vertical");
			AmbilDataTugasFileContent.this.setHeight("220px");
			AmbilDataTugasFileContent.this.setWidth(mobile ? "95%" : "850px");

			ListModel strset = new SimpleListModel(new ArrayList());
			grid.setRowRenderer(new TugasFileContentRenderer());
			grid.setModelCheckMobile(strset);

		}
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
