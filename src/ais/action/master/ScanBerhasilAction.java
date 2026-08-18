package ais.action.master;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Row;
import org.zkoss.zul.Timer;

import ais.action.master.helper.generic.AmbilDataLampiranFileLain;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPayroll;
import ais.common.ConstantValues;
import ais.common.gdrive.GDriveUtilPerPengguna;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Pertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.payroll.DetailJenisShiftPegawai;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.SmartDateTimeUtil;
import ais.ui.util.WaktuUtil;

public class ScanBerhasilAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private MyWindow window;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private String messageVideo = "<br><span>*) Jika video / audio Anda belum tampil, artinya video / audio tersebut sedang dalam pemrosesan, Anda bisa menunggu beberapa menit agar video / audio Anda bisa ditampilkan. Atau juga, Anda bisa menutup halaman ini, meskipun halaman ini ditutup, proses video / audio tetap berjalan.</span><hr>";

	@SuppressWarnings({ "unchecked" })
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		HttpServletRequest request = (HttpServletRequest) execution.getNativeRequest();
		final boolean http = !Common.isSecure(request);

		final Pertemuan pertemuan = request.getParameter("pert") == null
				|| !Common.isNumber(request.getParameter("pert"))
						? null
						: (Pertemuan) HibernateUtil.currentSession().createCriteria(Pertemuan.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.idEq(Long.parseLong(request.getParameter("pert").trim())))
								.uniqueResult();

		final Mahasiswa mahasiswa = request.getParameter("mahasiswa") == null
				|| !Common.isNumber(request.getParameter("mahasiswa")) ? null
						: (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(),
								Long.parseLong(request.getParameter("mahasiswa").trim()));

		final Siswa siswa = request.getParameter("siswa") == null || !Common.isNumber(request.getParameter("siswa"))
				? null
				: (Siswa) ConstantValues.ambil(Siswa.class.getName(),
						Long.parseLong(request.getParameter("siswa").trim()));

		final Dosen dosen = request.getParameter("dosen") == null || !Common.isNumber(request.getParameter("dosen"))
				? null
				: (Dosen) ConstantValues.ambil(Dosen.class.getName(),
						Long.parseLong(request.getParameter("dosen").trim()));

		final Guru guru = request.getParameter("guru") == null || !Common.isNumber(request.getParameter("guru")) ? null
				: (Guru) ConstantValues.ambil(Guru.class.getName(),
						Long.parseLong(request.getParameter("guru").trim()));

		final BiodataCalonMahasiswa biodataCalonMahasiswa = request.getParameter("calon_mahasiswa") == null
				|| !Common.isNumber(request.getParameter("calon_mahasiswa")) ? null
						: (BiodataCalonMahasiswa) ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(),
								Long.parseLong(request.getParameter("calon_mahasiswa").trim()));

		final CalonSiswa calonSiswa = request.getParameter("calon_siswa") == null
				|| !Common.isNumber(request.getParameter("calon_siswa")) ? null
						: (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(),
								Long.parseLong(request.getParameter("calon_siswa").trim()));

		final Pegawai pegawai = request.getParameter("pegawai") == null
				|| !Common.isNumber(request.getParameter("pegawai")) ? null
						: (Pegawai) ConstantValues.ambil(Pegawai.class.getName(),
								Long.parseLong(request.getParameter("pegawai").trim()));

		final String pert = request.getParameter("pert");
		final String lat = request.getParameter("lat");
		final String lng = request.getParameter("lng");
		final String userid = request.getParameter("userid");

		final String data = request.getParameter("image");
		final String keterangan = request.getParameter("keterangan");

		final String clazz = request.getParameter("clazz");
		final String rand = request.getParameter("rand");
		final String jenis = request.getParameter("jenis");
		// Penanda arah absensi dari aplikasi/scanner: "0"=KEDATANGAN, "1"=KEPULANGAN. Bila dikirim, HORMATI
		// (jangan pakai heuristik 'scan pertama = masuk' yang salah untuk pegawai yang hanya absen pulang).
		final String stateAbsen = request.getParameter("state");

		final Tbmuser asli = Common.getCurrentUser();
		Tbmuser u = Common.getCurrentUser();
		if (u == null && userid != null && !userid.trim().isEmpty()) {
			Map<Serializable, GeneralValueObject> map = ConstantValues.ambilBerdasarClass(Tbmuser.class);
			for (GeneralValueObject generalValueObject : map.values()) {
				Tbmuser usr = (Tbmuser) generalValueObject;
				if (usr != null && usr.getUserId() != null && usr.getUserId().equalsIgnoreCase(userid)) {
					u = usr;
					break;
				}
			}
		}

		if (u == null && mahasiswa != null) {
			u = new Tbmuser(mahasiswa);
		} else if (u == null && siswa != null) {
			u = new Tbmuser(siswa);
		} else if (u == null && biodataCalonMahasiswa != null) {
			u = new Tbmuser(biodataCalonMahasiswa);
		} else if (u == null && calonSiswa != null) {
			u = new Tbmuser(calonSiswa);
		}

		final Tbmuser tbmuser = u;

		String lokasi = request.getParameter("lokasi");

		System.out.println("lokasi -> " + lokasi + ", lat -> " + lat + ", lng -> " + lng);

		if (lokasi != null && lokasi.equalsIgnoreCase("true")) {
			if (lat == null || lat.trim().isEmpty() || lat.trim().equalsIgnoreCase("0.0")
					|| lat.trim().equalsIgnoreCase("0") || lng == null || lng.trim().isEmpty()
					|| lng.trim().equalsIgnoreCase("0.0") || lng.trim().equalsIgnoreCase("0")) {

				String img = "";
				tampilanError(pertemuan, mahasiswa, dosen, lat, lng, img, http, asli,
						"Absensi online gagal dilakukan.. Lokasi Anda tidak ditemukan, pastikan pengaturan Lokasi Anda aktif dan sistem telah diizinkan (Allow) untuk mengakses lokasi.<br><br>Coba ulangi lagi dan pastikan lokasi telah diaktifkan dan sistem telah dizinkan.<br><br>Pastikan tampilan peta telah tampil dibawah tampilan Gambar Kamera sebelum Anda klik tombol \"Ambil Gambar\"");
				return;
			}
		}

		if ((clazz == null || clazz.trim().isEmpty()) && rand != null && !rand.trim().isEmpty() && data != null
				&& !data.trim().isEmpty()) {

			final File outputfile;

			if (data.endsWith(".webm") || data.endsWith(".wav")) {
				outputfile = new File(Common.ambilREAL_PATH_REPORT() + "/" + data);
				System.out.println("outputfile video -> " + outputfile.getAbsolutePath());

				GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(tbmuser);

				driveUtilPerPengguna.prosesBackup(outputfile, "Gunakan Kamera", "Foto", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
								.getData();

						if (fileUpload != null && fileUpload.getId() != null) {
							Map<String, Object> map = new HashMap<String, Object>();
							map.put("drive", fileUpload.getId());
							map.put("file_name", outputfile.getName());
							AmbilDataLampiranFileLain.fotoDrive.put(Long.parseLong(rand), map);
							outputfile.delete();
						}
					}
				});

				final Timer timer = new Timer(1000);
				timer.setRepeats(true);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Map<String, Object> da = AmbilDataLampiranFileLain.fotoDrive.get(Long.parseLong(rand));
						if (da != null && da.get("drive") != null) {
							String d = (String) da.get("drive");
							if (d != null && !d.trim().isEmpty()) {
								outputfile.delete();

								Borderlayout borderlayout = new Borderlayout();
								borderlayout.setParent(window);

								Center center = new Center();
								center.setBorder("none");
								center.setParent(borderlayout);
								ais.ui.util.ZkCompat.setFlex(center, true);

								String img = "<iframe src=\"https://drive.google.com/file/d/" + d + "/preview\" "
										+ "style=\"width:100%;height:200px\"" + "></iframe>" + messageVideo;
//								System.out.println("img -> " + img);

								String urlFoto = "https://drive.google.com/uc?download=view&id=" + d;
								System.out.println("urlFoto -> " + urlFoto);

								if (!data.endsWith(".webm") && !data.endsWith(".wav")) {
									Image image = new Image(urlFoto);
									image.setWidth("97%");
									Common.tampilanScroll1(center).appendChild(image);
								} else {
									Html html = new Html(img);
									center.appendChild(html);
								}

								timer.detach();
							}
						}
					}
				});
				timer.start();

			} else {
				String[] imageParts = data == null ? null : data.split(",", 2);
				if (imageParts == null || imageParts.length < 2) {
					MyMessageboxConfig.show("Format gambar/scan tidak valid. Silakan ulangi proses scan.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				String imageString = imageParts[1];
				BufferedImage image = null;

				byte[] imageByte = org.apache.commons.codec.binary.Base64.decodeBase64(imageString.getBytes("UTF-8"));

				ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
				image = ais.common.CommonFileMediaHelper.bacaGambarAman(bis);

				bis.close();

				if (image == null) {
					MyMessageboxConfig.show("Gambar tidak dapat dibaca. Silakan ulangi proses scan.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				outputfile = new File(Common.ambilREAL_PATH_REPORT() + "/foto_" + Common.getGeneratedBarCode() + ".jpg");
				System.out.println("outputfile foto -> " + outputfile.getAbsolutePath());
				// write the image to a file
				ImageIO.write(image, "jpg", outputfile);

				File target = CommonMedia.resize(outputfile, image.getWidth() * 0.2, image.getHeight() * 0.2);

				Session session = StreamingHibernateUtil.getInstance().currentSession();
				try {

					System.out.println("outputfile foto kecil -> " + target);

					Boolean usingId = false;
					Long ref = Math.abs(Common.randLong());
					FileFotoLain a = FileFotoLain.createFileFotoLain(tbmuser, session, LampiranLain.class, usingId, ref,
							LampiranLain.ABSEN_ONLINE, null, target, null);

					String urlFoto = a.createLinkUri(true);
					proses(pertemuan, mahasiswa, dosen, siswa, guru, biodataCalonMahasiswa, pegawai, pert, urlFoto,
							urlFoto, lat, lng, http, asli, a.getId(), rand, keterangan, a, stateAbsen);

				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
				StreamingHibernateUtil.getInstance().closeSession();
			}

		} else if (clazz != null && !clazz.trim().isEmpty() && rand != null && !rand.trim().isEmpty() && data != null
				&& !data.trim().isEmpty()) {
			final File outputfile;

			if (data.endsWith(".webm") || data.endsWith(".wav")) {
				outputfile = new File(Common.ambilREAL_PATH_REPORT() + "/" + data);
				System.out.println("outputfile video -> " + outputfile.getAbsolutePath());

				if (!data.endsWith(".webm") && !data.endsWith(".wav")
						&& !AmbilDataLampiranFileLain.bolehDriveAtauLink(Class.forName(clazz), jenis, "")) {
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("drive", outputfile.getAbsolutePath());
					map.put("file_name", outputfile.getName());
					AmbilDataLampiranFileLain.fotoDrive.put(Long.parseLong(rand), map);

					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setBorder("none");
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					Image i = new Image(Common.getRequestHostWithProtocol() + "/report/" + outputfile.getName());
					i.setWidth("90%");
					center.appendChild(i);

				} else {

					GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(tbmuser);

					driveUtilPerPengguna.prosesBackup(outputfile, "Gunakan Kamera", "Foto", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
									.getData();

							if (fileUpload != null && fileUpload.getId() != null) {

								Map<String, Object> map = new HashMap<String, Object>();
								map.put("drive", fileUpload.getId());
								map.put("file_name", outputfile.getName());

								AmbilDataLampiranFileLain.fotoDrive.put(Long.parseLong(rand), map);
								outputfile.delete();
							}
						}
					});

					final Timer timer = new Timer(1000);
					timer.setRepeats(true);
					timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					timer.addEventListener("onTimer", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Map<String, Object> da = AmbilDataLampiranFileLain.fotoDrive.get(Long.parseLong(rand));
							if (da != null && da.get("drive") != null) {
								String d = (String) da.get("drive");
								if (d != null && !d.trim().isEmpty()) {
									outputfile.delete();

									Borderlayout borderlayout = new Borderlayout();
									borderlayout.setParent(window);

									Center center = new Center();
									center.setBorder("none");
									center.setParent(borderlayout);
									ais.ui.util.ZkCompat.setFlex(center, true);

									String img = "<iframe src=\"https://drive.google.com/file/d/" + d + "/preview\" "
											+ "style=\"width:100%;height:200px\"" + "></iframe>" + messageVideo;

									String urlFoto = "https://drive.google.com/uc?download=view&id=" + d;
									System.out.println("urlFoto -> " + urlFoto);

									if (!data.endsWith(".webm") && !data.endsWith(".wav")) {
										Image image = new Image(urlFoto);
										image.setWidth("97%");
										Common.tampilanScroll1(center).appendChild(image);
									} else {
										Html html = new Html(img);
										center.appendChild(html);
									}

									timer.detach();
								}
							}
						}
					});
					timer.start();
				}

			} else {
				String[] imageParts = data == null ? null : data.split(",", 2);
				if (imageParts == null || imageParts.length < 2) {
					MyMessageboxConfig.show("Format gambar/scan tidak valid. Silakan ulangi proses scan.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				String imageString = imageParts[1];
				BufferedImage image = null;

				byte[] imageByte = org.apache.commons.codec.binary.Base64.decodeBase64(imageString.getBytes("UTF-8"));

				ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
				image = ais.common.CommonFileMediaHelper.bacaGambarAman(bis);

				bis.close();

				if (image == null) {
					MyMessageboxConfig.show("Gambar tidak dapat dibaca. Silakan ulangi proses scan.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				outputfile = new File(Common.ambilREAL_PATH_REPORT() + "/foto_" + Common.getGeneratedBarCode() + ".jpg");
				System.out.println("outputfile foto -> " + outputfile.getAbsolutePath());
				// write the image to a file
				ImageIO.write(image, "jpg", outputfile);

				File target = CommonMedia.resize(outputfile, image.getWidth() * 0.2, image.getHeight() * 0.2);

				Session session = StreamingHibernateUtil.getInstance().currentSession();
				try {

					System.out.println("outputfile foto kecil -> " + target.getAbsolutePath());

					Boolean usingId = false;
					Long ref = Math.abs(Common.randLong());
					FileFotoLain a = FileFotoLain.createFileFotoLain(tbmuser, session, LampiranLain.class, usingId, ref,
							LampiranLain.ABSEN_ONLINE, null, target, null);

					String urlFoto = a.createLinkUri(true);
					proses(pertemuan, mahasiswa, dosen, siswa, guru, biodataCalonMahasiswa, pegawai, pert, urlFoto,
							urlFoto, lat, lng, http, asli, a.getId(), rand, keterangan, a, stateAbsen);

				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
				StreamingHibernateUtil.getInstance().closeSession();

			}

		} else if (data == null || data.trim().isEmpty()) {
			proses(pertemuan, mahasiswa, dosen, siswa, guru, biodataCalonMahasiswa, pegawai, pert, null, null, lat, lng,
					http, asli, null, rand, keterangan, null, stateAbsen);
		} else {
			try {
				final File outputfile;

				if (data.endsWith(".webm") || data.endsWith(".wav")) {
					outputfile = new File(Common.ambilREAL_PATH_REPORT() + "/" + data);
					System.out.println("outputfile video -> " + outputfile.getAbsolutePath());

					if (tbmuser != null) {
						GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(tbmuser);
						final List<String> s = new ArrayList<String>();
						driveUtilPerPengguna.prosesBackup(outputfile, "Absensi Kehadiran", "Foto", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
										.getData();

								if (fileUpload != null && fileUpload.getId() != null) {
									s.add(fileUpload.getId());
								}
							}
						});

						final Timer timer = new Timer(1000);
						timer.setRepeats(true);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.addEventListener("onTimer", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (!s.isEmpty()) {
									outputfile.delete();
									String img = "<iframe src=\"https://drive.google.com/file/d/" + s.get(0)
											+ "/preview\" " + "style=\"width:100%;height:200px\"" + "></iframe>"
											+ messageVideo;
//									System.out.println("img -> " + img);

									String urlFoto = "https://drive.google.com/uc?download=view&id=" + s.get(0);
									System.out.println("urlFoto -> " + urlFoto);

									if (!data.endsWith(".webm") && !data.endsWith(".wav")) {
										img = urlFoto;
									}

									proses(pertemuan, mahasiswa, dosen, siswa, guru, biodataCalonMahasiswa, pegawai,
											pert, img, urlFoto, lat, lng, http, asli, null, rand, keterangan, null, stateAbsen);
									timer.detach();
								}
							}
						});
						timer.start();
					}

				} else {
					String imageString = data.split(",")[1];
					BufferedImage image = null;

					byte[] imageByte = org.apache.commons.codec.binary.Base64.decodeBase64(imageString.getBytes("UTF-8"));

					ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
					image = ais.common.CommonFileMediaHelper.bacaGambarAman(bis);

					bis.close();

					if (image == null) {
						MyMessageboxConfig.show("Gambar tidak dapat dibaca. Silakan ulangi proses scan.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}
					outputfile = new File(Common.ambilREAL_PATH_REPORT() + "/foto_" + Common.getGeneratedBarCode() + ".jpg");

					System.out.println("outputfile foto -> " + outputfile.getAbsolutePath());

					// write the image to a file
					ImageIO.write(image, "jpg", outputfile);

					File target = CommonMedia.resize(outputfile, image.getWidth() * 0.2, image.getHeight() * 0.2);
					Session session = StreamingHibernateUtil.getInstance().currentSession();
					try {

						System.out.println("outputfile foto kecil -> " + target.getAbsolutePath());

						Boolean usingId = false;
						Long ref = Math.abs(Common.randLong());
						FileFotoLain a = FileFotoLain.createFileFotoLain(tbmuser, session, LampiranLain.class, usingId,
								ref, LampiranLain.ABSEN_ONLINE, null, target, null);

						String urlFoto = a.createLinkUri(true);
						proses(pertemuan, mahasiswa, dosen, siswa, guru, biodataCalonMahasiswa, pegawai, pert, urlFoto,
								urlFoto, lat, lng, http, asli, a.getId(), rand, keterangan, a, stateAbsen);

					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
					StreamingHibernateUtil.getInstance().closeSession();
				}

			} catch (Exception e) {

				tampilanError(pertemuan, mahasiswa, dosen, lat, lng, "", http, asli,
						"Absensi online gagal dilakukan.. Gambar / foto / video tidak ditemukan, pastikan kamera Anda aktif dan sistem telah diizinkan (Allow) untuk mengakses kamera.<br><br>Coba ulangi lagi dan pastikan kamera telah diaktifkan dan sistem telah dizinkan menggunakan kamera.<br><br>Pastikan Gambar dari kamera telah tampil sebelum Anda klik tombol \"Ambil Gambar\"");
//				ais.common.Common.tampilErrorJikaAdmin(e);
			}

		}
	}

	private void tampilanError(Pertemuan pertemuan, Mahasiswa mahasiswa, Dosen dosen, String lat, String lng,
			String image, boolean http, Tbmuser asli, String error) {
		try {
			String pre = "<div id=\"camBox\">\r\n"
					+ "		<div class=\"revdivshowimg\"\r\n"
					+ ">";

			if (pertemuan != null) {

				pre += "<h3>"
						+ pertemuan.ambilVOPembelajaran().infoSimple(pertemuan.getDosenPengganti() == null ? null
								: (Dosen) ConstantValues.ambil(Dosen.class.getName(), pertemuan.getDosenPengganti()))
						+ "</h3><hr>";
			}

			String pos = "<br>Perhatikan apakah opsi di browser Anda seperti gambar di bawah ini sudah diizinkan (Allow) ?<br><img src='"
					+ Common.getRequestHostWithProtocol() + "/img/allow.jpg'/></div>\r\n" + "	</div>";

			Borderlayout borderlayout = new Borderlayout();
			borderlayout.setParent(window);

			Center center = new Center();
			center.setBorder("none");
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);
			Html html = new ais.ui.util.MyHtml();
			html.setContent(pre + "<strong><font>" + error
					+ "</font></strong><br><img src=\"" + Common.getRequestHostWithProtocol()
					+ "/img/oh.gif\" alt=\"WebP rules.\" />" + pos);

			if (pertemuan != null) {

				String map = "https://maps.google.com/maps?q=" + lat + "," + lng + "&hl=id&z=14";

				JSONObject jsonObject = new JSONObject();
				try {
					String sebelumnya = pertemuan.retreive("sejarah");
					if (!sebelumnya.isEmpty()) {
						jsonObject = new JSONObject(sebelumnya);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/ScanBerhasilAction.java:600");
					// TODO: handle exception
				}
				Date tanggal = WaktuUtil.getDate();
				String keyTgl = "";
				if (mahasiswa != null && mahasiswa.getId() != null) {
					keyTgl = Common.dateFormat9.get().format(tanggal) + ":mhs" + mahasiswa.getId();
					jsonObject.put(keyTgl + "_foto", image);
					jsonObject.put(keyTgl + "_lokasi", map);
				} else if (dosen != null && dosen.getId() != null) {
					keyTgl = Common.dateFormat9.get().format(tanggal) + ":dsn" + dosen.getId();
					jsonObject.put(keyTgl + "_foto", image);
					jsonObject.put(keyTgl + "_lokasi", map);
				}
				jsonObject.put(keyTgl + "_info", error);
				pertemuan.put(jsonObject.toString(), "sejarah");
			}
			center.appendChild(html);
		} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/ScanBerhasilAction.java:618");
			// TODO: handle exception
		}
	}

	private void proses(Pertemuan pertemuan, Mahasiswa mahasiswa, Dosen dosen, Siswa siswa, Guru guru,
			BiodataCalonMahasiswa biodataCalonMahasiswa, Pegawai pegawai, String pert, String img, String urlFoto,
			String lat, String lng, boolean http, Tbmuser asli, Long idFile, String rand, String keterangan,
			FileFotoLain fileFotoLain, String stateAbsen) throws Exception {

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(window);

		Center center1 = new Center();
		center1.setBorder("none");
		center1.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center1, true);

		Row rowUtama = Common.tampilanScroll1(center1);

		if (Common.bolehKonfigurasi("siswa_tidak_boleh_absen_online", Konfigurasi.TIDAK_AKTIF) && siswa != null) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("urlFoto", urlFoto);
			map.put("idFile", idFile + "");
			map.put("fileFotoLain", fileFotoLain);
			AmbilDataLampiranFileLain.fotoDrive.put(Long.parseLong(rand), map);

			Html html = new ais.ui.util.MyHtml();
			html.setContent("<strong><font>Siswa tidak boleh absen</font></strong>");

			rowUtama.appendChild(html);
			return;
		}

		if (pertemuan == null && (pert == null || pert.trim().isEmpty()) && rand != null && !rand.trim().isEmpty()
				&& fileFotoLain != null) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("urlFoto", urlFoto);
			map.put("idFile", idFile + "");
			map.put("fileFotoLain", fileFotoLain);
			AmbilDataLampiranFileLain.fotoDrive.put(Long.parseLong(rand), map);

			Html html = new ais.ui.util.MyHtml();
			html.setContent("<strong><font>Media berhasil diambil"
					+ "</font></strong><br><img src=\"" + urlFoto + "\" alt=\"WebP rules.\" />");

			rowUtama.appendChild(html);

		} else {

			System.out.println("absen -> " + pertemuan + " pert " + pert);

			String map = "";
			if (lat != null && lng != null) {
				map = "https://maps.google.com/maps?q=" + lat + "," + lng + "&hl=id&z=14";
			}

			String pre = "<div id=\"camBox\">\r\n"
					+ "		<div class=\"revdivshowimg\"\r\n"
					+ ">";

			if (pertemuan != null) {

				pre += "<h3>"
						+ pertemuan.ambilVOPembelajaran().infoSimple(pertemuan.getDosenPengganti() == null ? null
								: (Dosen) ConstantValues.ambil(Dosen.class.getName(), pertemuan.getDosenPengganti()))
						+ "</h3><hr>";
			}

			String pos = "</div>\r\n" + "	</div>";

			String imgeHtml = img == null || img.trim().isEmpty() ? ""
					: img.trim().contains("iframe") ? img
							: "<img src='" + img + "'/><br>";
			boolean video = img != null && img.trim().contains("iframe");
			if ((pegawai != null || mahasiswa != null || siswa != null) && pert != null && pert.startsWith("P")) {
				try {
					String code = pert.split("-", 2)[1];
					System.out.println("Absensi pegawai code = " + code);
					String tgl = Common.desEncrypter.get().decrypt(code);
					System.out.println("Absensi pegawai tgl = " + tgl);
					Date tanggal = Common.dateFormat8.get().parse(tgl);
					System.out.println("Absensi pegawai tanggal = " + tanggal);
					if (!Common.dateFormat8.get().format(WaktuUtil.getDate()).equals(tgl)) {
						Html html = new ais.ui.util.MyHtml();

						if (mahasiswa != null) {
							html.setContent(pre + imgeHtml
									+ "<strong><font>Mahasiswa dengan nama \""
									+ mahasiswa.getNama()
									+ "\" tidak bisa melakukan absen karena tanggal QR-Code tidak sesuai, tanggal di QR-Code \""
									+ Common.dateFormat6.get().format(tanggal) + "\" sedangkan sekarang  \""
									+ Common.dateFormat6.get().format(WaktuUtil.getDate())
									+ "\". Absensi online gagal dilakukan</font></strong><br><img src=\""
									+ Common.getRequestHostWithProtocol() + "/img/oh.gif\" alt=\"WebP rules.\" />"
									+ pos);
						}

						else if (siswa != null) {
							html.setContent(pre + imgeHtml
									+ "<strong><font>Siswa dengan nama \""
									+ siswa.getNama()
									+ "\" tidak bisa melakukan absen karena tanggal QR-Code tidak sesuai, tanggal di QR-Code \""
									+ Common.dateFormat6.get().format(tanggal) + "\" sedangkan sekarang  \""
									+ Common.dateFormat6.get().format(WaktuUtil.getDate())
									+ "\". Absensi online gagal dilakukan</font></strong><br><img src=\""
									+ Common.getRequestHostWithProtocol() + "/img/oh.gif\" alt=\"WebP rules.\" />"
									+ pos);
						}

						else if (pegawai != null) {
							html.setContent(pre + imgeHtml
									+ "<strong><font>Pegawai dengan nama \""
									+ pegawai.getNama()
									+ "\" tidak bisa melakukan absen karena tanggal QR-Code tidak sesuai, tanggal di QR-Code \""
									+ Common.dateFormat6.get().format(tanggal) + "\" sedangkan sekarang  \""
									+ Common.dateFormat6.get().format(WaktuUtil.getDate())
									+ "\". Absensi online gagal dilakukan</font></strong><br><img src=\""
									+ Common.getRequestHostWithProtocol() + "/img/oh.gif\" alt=\"WebP rules.\" />"
									+ pos);
						}

						rowUtama.appendChild(html);
					} else {

					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (pertemuan == null) {
				DetailJenisShiftPegawai jenis = null;
				if (pegawai != null || mahasiswa != null || siswa != null) {

					Date tanggal = WaktuUtil.getDate();
					String keyTgl = Common.dateFormat9.get().format(tanggal);
					Session session = HibernateUtil.openSession();
					try {

						StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = CommonPayroll
								.getDefaultStatuskehadiranKaryawanHarian(tanggal, pegawai, mahasiswa, siswa, lat, lng,
										session, true);

						JSONObject jsonObject = new JSONObject();
						try {
							String sebelumnya = statuskehadiranKaryawanHarian.retreive("sejarah");
							if (!sebelumnya.isEmpty()) {
								jsonObject = new JSONObject(sebelumnya);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/ScanBerhasilAction.java:768");
							// TODO: handle exception
						}

						jsonObject.put(keyTgl + "_foto", urlFoto);
						jsonObject.put(keyTgl + "_lokasi", map);

						boolean datang = true;
						CutiDanIzin cutiDanIzin = statuskehadiranKaryawanHarian.getCutiDanIzin();
						// Bila aplikasi mengirim penanda arah (state), HORMATI. Jika tidak, pakai heuristik lama
						// (scan pertama hari itu = kedatangan). "1"=KEPULANGAN, "0"=KEDATANGAN.
						boolean stateKepulangan = stateAbsen != null && stateAbsen.trim().equals("1");
						boolean stateKedatangan = stateAbsen != null && stateAbsen.trim().equals("0");
						boolean sebagaiKedatangan;
						if (stateKepulangan) {
							sebagaiKedatangan = false;
						} else if (stateKedatangan) {
							sebagaiKedatangan = true;
						} else {
							// Heuristik lama: scan pertama (belum ada masuk) = kedatangan.
							sebagaiKedatangan = statuskehadiranKaryawanHarian.getMasukjam() == null
									&& (cutiDanIzin == null || !cutiDanIzin.getSetujui());
						}

						if (sebagaiKedatangan) {
							statuskehadiranKaryawanHarian.setMasukjam(tanggal);

							statuskehadiranKaryawanHarian.setFotoAbsenDatang(urlFoto);
							statuskehadiranKaryawanHarian.setLokasiAbsenDatang(map);

							if (img != null && img.trim().contains("iframe")) {
								statuskehadiranKaryawanHarian.setFotoAbsenDatang(img);
							}

						} else {

							statuskehadiranKaryawanHarian.setFotoAbsenPulang(urlFoto);
							statuskehadiranKaryawanHarian.setLokasiAbsenPulang(map);

							if (img != null && img.trim().contains("iframe")) {
								statuskehadiranKaryawanHarian.setFotoAbsenDatang(img);
							}

							datang = false;
							// KEPULANGAN eksplisit (state="1") TIDAK mengeset kedatangan sama sekali — sehingga
							// pegawai yang hanya absen pulang tidak lagi salah tercatat "datang".
							statuskehadiranKaryawanHarian.setPulangJam(tanggal);
							// Cegah SHADOW: bila State/Manual sudah terisi lebih awal (scan lain/duplikat),
							// majukan ke scan pulang ini agar kolom Jam Pulang tidak menampilkan jam lama.
							statuskehadiranKaryawanHarian.majukanPulangStateManualJikaLebihAwal(tanggal);
						}

						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.setTime(tanggal);
						String hari = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];
						jenis = CommonPayroll.getDetailJenisShiftPegawai(pegawai, mahasiswa, siswa,
								statuskehadiranKaryawanHarian.ambilMasukjam() == null ? tanggal
										: statuskehadiranKaryawanHarian.ambilMasukjam(),
								statuskehadiranKaryawanHarian.getTanggal(), hari,
								statuskehadiranKaryawanHarian.getLiburNasional() != null);

						System.out.println("statuskehadiranKaryawanHarian -> " + statuskehadiranKaryawanHarian
								+ ", jenis -> " + jenis + " datang -> " + datang);

						if (jenis == null && pegawai != null) {
							keterangan = (keterangan == null || keterangan.trim().isEmpty() ? "" : keterangan + " ")
									+ statuskehadiranKaryawanHarian.getKeterangan();
							String s = "Hai "
									+ (mahasiswa != null ? mahasiswa.getNama()
											: siswa != null ? siswa.getNama() : pegawai.getNama())
									+ ". Absensi foto GAGAL dilakukan, karena pengaturan waktu absensi Anda tidak ditemukan.";
							keterangan = (keterangan.isEmpty() ? s : s + ";\n") + keterangan;

							Html html = new ais.ui.util.MyHtml();
							html.setContent(pre + imgeHtml + "<strong><font>" + s
									+ "</font></strong><br><img src=\""
									+ Common.getRequestHostWithProtocol() + "/img/oh.gif\" alt=\"WebP rules.\" />"
									+ pos);

							jsonObject.put(keyTgl + "_info", s);

							statuskehadiranKaryawanHarian.put(jsonObject.toString(), "sejarah");

							rowUtama.appendChild(html);
							HibernateUtil.closeSession();
							return;
						}
						Lokasi lokasi = null;
						Double jarakKm = null;
						if (jenis != null) {

							if (jenis.getJenisShiftPegawai() != null && !statuskehadiranKaryawanHarian.getAbaikanJarak()
									&& (statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai() == null
											|| !statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai()
													.getAbaikanJarak())) {

								Entry<Double, Lokasi> entry = jenis.ambilJarakDanLokasiTerdekat(lat, lng);

								if (entry != null) {
									try {
										jarakKm = entry.getKey();
										lokasi = entry.getValue();

										try {
											statuskehadiranKaryawanHarian.setJarak(jarakKm);
											statuskehadiranKaryawanHarian
													.setJarakMaks(jenis.getJenisShiftPegawai().getJarak());
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

										if (jarakKm > jenis.getJenisShiftPegawai().getJarak()
												&& !statuskehadiranKaryawanHarian.getAbaikanJarak()
												&& (statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai() == null
														|| !statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai()
																.getAbaikanJarak())) {

											String s = "Absensi gagal dilakukan pada "
													+ Common.dateFormat5.get().format(WaktuUtil.getDate())
													+ ", karena jarak lokasi Anda berada "
													+ Common.numberFormat.get().format(jarakKm) + "km dari lokasi/koordinat "
													+ lokasi.getNama();
											if (lat != null && lng != null) {
												s += ", lokasi absen " + map + " ";
											}

											if (urlFoto != null && !urlFoto.trim().isEmpty()) {
												s += ", " + (video ? "video" : "foto") + " absen " + urlFoto + " ";
											}

											keterangan = (keterangan == null || keterangan.trim().isEmpty() ? ""
													: keterangan + " ") + statuskehadiranKaryawanHarian.getKeterangan();

											keterangan = (keterangan.isEmpty() ? s : s + ";") + keterangan;

											Html html = new ais.ui.util.MyHtml();
											html.setContent(
													pre + imgeHtml + "<strong><font>"
															+ s + "</font></strong><br><img src=\""
															+ Common.getRequestHostWithProtocol()
															+ "/img/oh.gif\" alt=\"WebP rules.\" />" + pos);

											jsonObject.put(keyTgl + "_info", s);

											statuskehadiranKaryawanHarian.put(jsonObject.toString(), "sejarah");

											rowUtama.appendChild(html);
											HibernateUtil.closeSession();
											return;
										}

									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}
								}
							}

							statuskehadiranKaryawanHarian.setDetailJenisShiftPegawai(jenis);

							System.out.println("urlFoto -> " + urlFoto);

							if (jenis.getAktifkanAbsenFoto()) {
								if (datang) {
									Double menitDatangFoto = statuskehadiranKaryawanHarian
											.getJumlahMenitAbsenFotoSaatHadir();
									Double mulaiAbsenFoto = Math.abs(jenis.getMenitSebelumJamMulai());
									Double mulaiAbsenFotoSetelah = Math.abs(jenis.getMenitSetelahJamMulai());

									boolean belumMulaiToleransi = Math.abs(menitDatangFoto) > mulaiAbsenFoto;
									boolean setelahMulaiToleransi = Math.abs(menitDatangFoto) > mulaiAbsenFotoSetelah;
									System.out.println(jenis + " : menitDatangFoto -> " + menitDatangFoto
											+ ", mulaiAbsenFoto -> " + mulaiAbsenFoto + ", mulaiAbsenFotoSetelah -> "
											+ mulaiAbsenFotoSetelah + ", belumMulaiToleransi -> " + belumMulaiToleransi
											+ ", setelahMulaiToleransi -> " + setelahMulaiToleransi);

									if (menitDatangFoto < 0 && belumMulaiToleransi) {
										keterangan = (keterangan == null || keterangan.trim().isEmpty() ? ""
												: keterangan + " ") + statuskehadiranKaryawanHarian.getKeterangan();

										String s = "Hai "
												+ (mahasiswa != null ? mahasiswa.getNama()
														: siswa != null ? siswa.getNama() : pegawai.getNama())
												+ ". Absensi online kedatangan GAGAL dilakukan, karena di pengaturan jadwal absensi, absen menggunakan foto atau video bisa dilakukan maksimal "
												+ ((int) Math.abs(mulaiAbsenFoto)) + " menit sebelum pukul "
												+ Common.timeFormat.get().format(jenis.getMulai()) + ", sedangkan saat ini "
												+ ((int) Math.abs(menitDatangFoto)) + " menit sebelum pukul "
												+ Common.timeFormat.get().format(jenis.getMulai()) + ".";
										keterangan = (keterangan.isEmpty() ? s : s + ";\n") + keterangan;

										Html html = new ais.ui.util.MyHtml();
										html.setContent(
												pre + imgeHtml + "<strong><font>" + s
														+ "</font></strong><br><img src=\""
														+ Common.getRequestHostWithProtocol()
														+ "/img/oh.gif\" alt=\"WebP rules.\" />" + pos);

										rowUtama.appendChild(html);

										jsonObject.put(keyTgl + "_info", s);

										statuskehadiranKaryawanHarian.put(jsonObject.toString(), "sejarah");

										HibernateUtil.closeSession();
										return;
									} else if (menitDatangFoto > 0 && setelahMulaiToleransi) {
										keterangan = (keterangan == null || keterangan.trim().isEmpty() ? ""
												: keterangan + " ") + statuskehadiranKaryawanHarian.getKeterangan();

										String s = "Hai "
												+ (mahasiswa != null ? mahasiswa.getNama()
														: siswa != null ? siswa.getNama() : pegawai.getNama())
												+ ". Absensi online kedatangan GAGAL dilakukan, karena di pengaturan jadwal absensi, absen menggunakan foto atau video bisa dilakukan maksimal "
												+ ((int) Math.abs(mulaiAbsenFotoSetelah)) + " menit setelah pukul "
												+ Common.timeFormat.get().format(jenis.getMulai()) + ", sedangkan saat ini "
												+ ((int) Math.abs(menitDatangFoto)) + " menit setelah pukul "
												+ Common.timeFormat.get().format(jenis.getMulai()) + ".";
										keterangan = (keterangan.isEmpty() ? s : s + ";\n") + keterangan;

										Html html = new ais.ui.util.MyHtml();
										html.setContent(
												pre + imgeHtml + "<strong><font>" + s
														+ "</font></strong><br><img src=\""
														+ Common.getRequestHostWithProtocol()
														+ "/img/oh.gif\" alt=\"WebP rules.\" />" + pos);

										rowUtama.appendChild(html);

										jsonObject.put(keyTgl + "_info", s);

										statuskehadiranKaryawanHarian.put(jsonObject.toString(), "sejarah");

										HibernateUtil.closeSession();
										return;
									} else {
										statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.MASUK);
									}

								} else {

									Double menitPulangFoto = statuskehadiranKaryawanHarian
											.getJumlahMenitAbsenFotoSaatPulang();
									Double mulaiAbsenFoto = Math.abs(jenis.getMenitSebelumJamSampai());
									Double mulaiAbsenFotoSetelah = Math.abs(jenis.getMenitSetelahJamSampai());

									boolean belumMulaiToleransi = Math.abs(menitPulangFoto) > mulaiAbsenFoto;
									boolean setelahMulaiToleransi = Math.abs(menitPulangFoto) > mulaiAbsenFotoSetelah;
									System.out.println(jenis + " : menitPulangFoto -> " + menitPulangFoto
											+ ", mulaiAbsenFoto -> " + mulaiAbsenFoto + ", mulaiAbsenFotoSetelah -> "
											+ mulaiAbsenFotoSetelah + ", belumMulaiToleransi -> " + belumMulaiToleransi
											+ ", setelahMulaiToleransi -> " + setelahMulaiToleransi);

									if (menitPulangFoto < 0 && belumMulaiToleransi) {
										keterangan = (keterangan == null || keterangan.trim().isEmpty() ? ""
												: keterangan + " ") + statuskehadiranKaryawanHarian.getKeterangan();

										String s = "Hai "
												+ (mahasiswa != null ? mahasiswa.getNama()
														: siswa != null ? siswa.getNama() : pegawai.getNama())
												+ ". Absensi online kepulangan GAGAL dilakukan, karena di pengaturan jadwal absensi, absen menggunakan foto atau video bisa dilakukan maksimal "
												+ ((int) Math.abs(mulaiAbsenFoto)) + " menit sebelum pukul "
												+ Common.timeFormat.get().format(jenis.getSampai()) + ", sedangkan saat ini "
												+ ((int) Math.abs(menitPulangFoto)) + " menit sebelum pukul "
												+ Common.timeFormat.get().format(jenis.getSampai()) + ".";
										keterangan = (keterangan.isEmpty() ? s : s + ";\n") + keterangan;

										Html html = new ais.ui.util.MyHtml();
										html.setContent(
												pre + imgeHtml + "<strong><font>" + s
														+ "</font></strong><br><img src=\""
														+ Common.getRequestHostWithProtocol()
														+ "/img/oh.gif\" alt=\"WebP rules.\" />" + pos);

										rowUtama.appendChild(html);

										jsonObject.put(keyTgl + "_info", s);

										statuskehadiranKaryawanHarian.put(jsonObject.toString(), "sejarah");

										HibernateUtil.closeSession();
										return;
									} else if (menitPulangFoto > 0 && setelahMulaiToleransi) {
										keterangan = (keterangan == null || keterangan.trim().isEmpty() ? ""
												: keterangan + " ") + statuskehadiranKaryawanHarian.getKeterangan();

										String s = "Hai "
												+ (mahasiswa != null ? mahasiswa.getNama()
														: siswa != null ? siswa.getNama() : pegawai.getNama())
												+ ". Absensi online kepulangan GAGAL dilakukan, karena di pengaturan jadwal absensi, absen menggunakan foto atau video bisa dilakukan maksimal "
												+ ((int) Math.abs(mulaiAbsenFotoSetelah)) + " menit setelah pukul "
												+ Common.timeFormat.get().format(jenis.getSampai()) + ", sedangkan saat ini "
												+ ((int) Math.abs(menitPulangFoto)) + " menit setelah pukul "
												+ Common.timeFormat.get().format(jenis.getSampai()) + ".";
										keterangan = (keterangan.isEmpty() ? s : s + ";\n") + keterangan;

										Html html = new ais.ui.util.MyHtml();
										html.setContent(
												pre + imgeHtml + "<strong><font>" + s
														+ "</font></strong><br><img src=\""
														+ Common.getRequestHostWithProtocol()
														+ "/img/oh.gif\" alt=\"WebP rules.\" />" + pos);

										rowUtama.appendChild(html);

										jsonObject.put(keyTgl + "_info", s);

										statuskehadiranKaryawanHarian.put(jsonObject.toString(), "sejarah");

										HibernateUtil.closeSession();
										return;
									} else {
										statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.MASUK);
									}
								}
							} else {
								statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.MASUK);
							}

							if (statuskehadiranKaryawanHarian.getJarak() != null
									&& statuskehadiranKaryawanHarian.getJarakMaks() != null
									&& statuskehadiranKaryawanHarian
											.getJarak() > statuskehadiranKaryawanHarian.getJarakMaks()
									&& !statuskehadiranKaryawanHarian.getAbaikanJarak()
									&& (statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai() == null
											|| !statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai()
													.getAbaikanJarak())) {
								String s = "Absensi gagal dilakukan pada "
										+ Common.dateFormat5.get().format(WaktuUtil.getDate())
										+ ", karena jarak lokasi Anda berada "
										+ Common.numberFormat.get().format(statuskehadiranKaryawanHarian.getJarak())
										+ "km dari lokasi/koordinat " + (lokasi == null ? "" : lokasi.getNama());
								if (lat != null && lng != null) {
									s += ", lokasi absen " + map + " ";
								}

								if (urlFoto != null && !urlFoto.trim().isEmpty()) {
									s += ", " + (video ? "video" : "foto") + " absen " + urlFoto + " ";
								}

								keterangan = (keterangan == null || keterangan.trim().isEmpty() ? "" : keterangan + " ")
										+ statuskehadiranKaryawanHarian.getKeterangan();

								keterangan = (keterangan.isEmpty() ? s : s + ";") + keterangan;

								Html html = new ais.ui.util.MyHtml();
								html.setContent(pre + imgeHtml + "<strong><font>" + s
										+ "</font></strong><br><img src=\""
										+ Common.getRequestHostWithProtocol() + "/img/oh.gif\" alt=\"WebP rules.\" />"
										+ pos);

								jsonObject.put(keyTgl + "_info", s);

								statuskehadiranKaryawanHarian.put(jsonObject.toString(), "sejarah");

								rowUtama.appendChild(html);
							}

							if (pertemuan == null) {
								String s = ((urlFoto != null && !urlFoto.trim().isEmpty())
										? "ABSEN FOTO " + (datang ? "DATANG " : "PULANG ")
										: "QR-CODE " + (datang ? "DATANG " : "PULANG "))
										+ Common.dateFormat5.get().format(tanggal);
								if (lat != null && lng != null) {
									s += " " + map + " ";
								}
								if (urlFoto != null && !urlFoto.trim().isEmpty()) {
									s += ", " + (video ? "video" : "foto") + " absen " + urlFoto + " ";
								}

								statuskehadiranKaryawanHarian.setKeterangan(s);
								if (idFile != null) {
									if (datang) {
										statuskehadiranKaryawanHarian.setIdFile(idFile);
									} else {
										statuskehadiranKaryawanHarian.setIdFilePulang(idFile);
									}
								}
								simpanStatusKehadiran(session, statuskehadiranKaryawanHarian);

								CommonPayroll.simpanDetail(session, statuskehadiranKaryawanHarian, true);

								Html html = new ais.ui.util.MyHtml();
								html.setContent(pre + imgeHtml
										+ "<strong><font>Selamat \""
										+ (mahasiswa != null ? mahasiswa.getNama()
												: siswa != null ? siswa.getNama() : pegawai.getNama())
										+ "\", absensi pada \"" + Common.dateFormat6.get().format(tanggal)
										+ "\" berhasil dilakukan.</font></strong><br><img src=\""
										+ Common.getRequestHostWithProtocol()
										+ "/img/success-icon_big.png\" alt=\"WebP rules.\" />" + pos);

								jsonObject.put(
										"Absensi " + (datang ? "kedatangan " : "kepulangan") + " sukses dilakukan",
										keyTgl + "_info");

								statuskehadiranKaryawanHarian.put(jsonObject.toString(), "sejarah");

								rowUtama.appendChild(html);
							}
						}

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}

				}
			}
			if (pertemuan == null && pegawai == null && mahasiswa == null && dosen == null
					&& biodataCalonMahasiswa == null && siswa == null && guru == null) {

				Html html = new ais.ui.util.MyHtml();
				html.setContent(pre + imgeHtml
						+ "<strong><font>Terdapat kesalahan data.. Absensi online gagal dilakukan</font></strong><br><img src=\""
						+ Common.getRequestHostWithProtocol() + "/img/oh.gif\" alt=\"WebP rules.\" />" + pos);

				rowUtama.appendChild(html);
				return;

			}

			try {
				if (pegawai != null) {
					CutiDanIzin cutiDanIzin = CutiDanIzin.apakahSedangCuti(pegawai, WaktuUtil.getDate());
					if (cutiDanIzin != null && cutiDanIzin.getId() != null) {
						Html html = new ais.ui.util.MyHtml();
						html.setContent(pre + imgeHtml
								+ "<strong><font>Status Anda sedang cuti dengan alasan \""
								+ cutiDanIzin.getKeterangan() + "\" mulai tanggal "
								+ Common.dateFormat6.get().format(cutiDanIzin.getMulai()) + " sd "
								+ Common.dateFormat6.get().format(cutiDanIzin.getSampai())
								+ " .. Absensi online gagal dilakukan</font></strong><br><img src=\""
								+ Common.getRequestHostWithProtocol() + "/img/oh.gif\" alt=\"WebP rules.\" />" + pos);

						rowUtama.appendChild(html);
						return;
					}
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

			if (pertemuan != null) {

				JSONObject jsonObject = new JSONObject();
				try {
					String sebelumnya = pertemuan.retreive("sejarah");
					if (!sebelumnya.isEmpty()) {
						jsonObject = new JSONObject(sebelumnya);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/ScanBerhasilAction.java:1199");
					// TODO: handle exception
				}

				Date tanggal = WaktuUtil.getDate();
				String keyTgl = "";
				if (mahasiswa != null && mahasiswa.getId() != null) {

					keyTgl = Common.dateFormat9.get().format(tanggal) + ":mhs" + mahasiswa.getId();
					if (video) {
						jsonObject.put(keyTgl + "_img", img);
					}
					jsonObject.put(keyTgl + "_foto", urlFoto);
					jsonObject.put(keyTgl + "_lokasi", map);

					// Validasi peserta harus membaca DB langsung. Cache peserta perkuliahan dapat
					// tertinggal sesaat setelah KRS disetujui dan menyebabkan penolakan palsu.
					boolean ada = pertemuan.apakahMahasiswaPesertaDisetujuiLangsung(mahasiswa);
					if (!ada) {
						String s = "Mahasiswa dengan NIM \"" + mahasiswa.getNim() + "\" dan nama \""
								+ mahasiswa.getNama()
								+ "\" tidak terdaftar di absensi pertemuan ini. Absensi online gagal dilakukan.";
						Html html = new ais.ui.util.MyHtml();
						html.setContent(pre + imgeHtml + "<strong><font>" + s
								+ "</font></strong><br><img src=\""
								+ Common.getRequestHostWithProtocol() + "/img/oh.gif\" alt=\"WebP rules.\" />" + pos);
						jsonObject.put(keyTgl + "_info", s);
						pertemuan.put(jsonObject.toString(), "sejarah");

						rowUtama.appendChild(html);
						return;
					}
				} else if (dosen != null && dosen.getId() != null) {

					keyTgl = Common.dateFormat9.get().format(tanggal) + ":dsn" + dosen.getId();
					if (img != null && img.trim().contains("iframe")) {
						jsonObject.put(keyTgl + "_img", img);
					}
					jsonObject.put(keyTgl + "_foto", urlFoto);
					jsonObject.put(keyTgl + "_lokasi", map);

					List<Dosen> dosens = pertemuan.ambilDosen();
					boolean ada = false;
					for (Dosen mhs : dosens) {
						if (mhs != null && mhs.getId() != null && mhs.getId().equals(dosen.getId())) {
							ada = true;
							break;
						}
					}
					dosens = null;
					if (!ada) {
						String s = "Dosen dengan nama \"" + dosen.getNama()
								+ "\" tidak terdaftar di absensi pertemuan ini. Absensi online gagal dilakukan.";
						Html html = new ais.ui.util.MyHtml();
						html.setContent(pre + imgeHtml + "<strong><font>" + s
								+ "</font></strong><br><img src=\""
								+ Common.getRequestHostWithProtocol() + "/img/oh.gif\" alt=\"WebP rules.\" />" + pos);
						jsonObject.put(keyTgl + "_info", s);
						pertemuan.put(jsonObject.toString(), "sejarah");
						rowUtama.appendChild(html);
						return;
					}
				}

				if (pertemuan != null && pertemuan.getLokasi() != null) {

					try {
						if (lat == null || lat.trim().isEmpty() || lng == null || lng.trim().isEmpty()) {
							throw new IllegalArgumentException("Koordinat lokasi tidak tersedia");
						}
						double latitude1 = pertemuan.getLokasi().getLat();
						double longitude1 = pertemuan.getLokasi().getLng();
						double latitude2 = Double.parseDouble(lat.trim());
						double longitude2 = Double.parseDouble(lng.trim());

						Double jarakKm = Common.getDistanceBetweenPointsNew(latitude1, longitude1, latitude2,
								longitude2);

						if (jarakKm > pertemuan.getJarak()) {

							keterangan = (keterangan == null || keterangan.trim().isEmpty() ? "" : keterangan + " ")
									+ "";

							String s = "Absensi gagal dilakukan pada " + Common.dateFormat5.get().format(WaktuUtil.getDate())
									+ ", karena jarak lokasi Anda berada " + Common.numberFormat.get().format(jarakKm)
									+ "km dari lokasi/koordinat " + pertemuan.getLokasi().getNama();
							if (lat != null && lng != null) {
								s += ", lokasi absen " + map + " ";
							}

							if (urlFoto != null && !urlFoto.trim().isEmpty()) {
								s += ", " + (video ? "video" : "foto") + " absen " + urlFoto + " ";
							}

							keterangan = (keterangan.isEmpty() ? s : s + ";") + keterangan;

							Html html = new ais.ui.util.MyHtml();
							html.setContent(pre + imgeHtml + "<strong><font>" + s
									+ "</font></strong><br><img src=\""
									+ Common.getRequestHostWithProtocol() + "/img/oh.gif\" alt=\"WebP rules.\" />"
									+ pos);

							jsonObject.put(keyTgl + "_info", s);
							pertemuan.put(jsonObject.toString(), "sejarah");

							rowUtama.appendChild(html);
							return;
						}

					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
				
				
				if (mahasiswa != null && pertemuan != null && pertemuan.getPerkuliahan() != null
						&& pertemuan.getPerkuliahan().getMahasiswaHanyaBolehAbsenSetelahAdaDosenYangAbsen()) {

					try {

						boolean dosenTelahAbsen = false;
						for (Long dosenId : pertemuan.getPerkuliahan().populateDosenBuId()) {
							Statusabsensi statusabsensi = (Statusabsensi) ConstantValues
									.ambil(Statusabsensi.class.getName(), pertemuan.retreiveAbsensiId(dosenId));
							if (statusabsensi != null && ConstantValues.ABSEN != null
									&& statusabsensi.getId().equals(ConstantValues.ABSEN.getId())) {
								dosenTelahAbsen = true;
								break;
							}
						}

						if (!dosenTelahAbsen) {

							keterangan = (keterangan == null || keterangan.trim().isEmpty() ? "" : keterangan + " ")
									+ "";

							String s = "Absensi gagal dilakukan pada "
									+ Common.dateFormat5.get().format(WaktuUtil.getDate())
									+ ", karena belum ada dosen yang belum melakukan absensi";
							if (lat != null && lng != null) {
								s += ", lokasi absen " + map + " ";
							}

							if (urlFoto != null && !urlFoto.trim().isEmpty()) {
								s += ", " + (video ? "video" : "foto") + " absen " + urlFoto + " ";
							}

							keterangan = (keterangan.isEmpty() ? s : s + ";") + keterangan;

							Html html = new ais.ui.util.MyHtml();
							html.setContent(pre + imgeHtml + "<strong><font>" + s
									+ "</font></strong><br><img src=\""
									+ Common.getRequestHostWithProtocol() + "/img/oh.gif\" alt=\"WebP rules.\" />"
									+ pos);

							jsonObject.put(keyTgl + "_info", s);
							pertemuan.put(jsonObject.toString(), "sejarah");

							rowUtama.appendChild(html);
							return;
						}

					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}

				Calendar calendarMulai = Calendar.getInstance();

				if (pertemuan.getTanggalRealisasi() != null) {
					calendarMulai.setTime(pertemuan.getTanggalRealisasi());
				} else if (pertemuan.getTanggal() != null) {
					calendarMulai
							.setTime(pertemuan.getTanggal() == null ? WaktuUtil.getDate() : pertemuan.getTanggal());
				}

				try {
					if (pertemuan.getWaktuMulai() != null) {
						Integer jamMulai = Integer.parseInt(pertemuan.getWaktuMulai().split("\\.")[0]);
						Integer menitMulai = Integer.parseInt(pertemuan.getWaktuMulai().split("\\.")[1]);
						calendarMulai.set(Calendar.HOUR_OF_DAY, jamMulai);
						calendarMulai.set(Calendar.MINUTE,
								menitMulai - pertemuan.getBolehAbsenSebelumWaktuMulaiDalamMenit());
						calendarMulai.set(Calendar.SECOND, 1);

					}
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

				Calendar calendarSelesai = Calendar.getInstance();

				if (pertemuan.getTanggalRealisasi() != null) {
					calendarSelesai.setTime(pertemuan.getTanggalRealisasi());
				} else {
					calendarSelesai
							.setTime(pertemuan.getTanggal() == null ? WaktuUtil.getDate() : pertemuan.getTanggal());
				}

				try {
					if (pertemuan.getWaktuMulai() != null) {
						Integer jamMulai = Integer.parseInt(pertemuan.getWaktuSelesai().split("\\.")[0]);
						Integer menitMulai = Integer.parseInt(pertemuan.getWaktuSelesai().split("\\.")[1]);
						calendarSelesai.set(Calendar.HOUR_OF_DAY, jamMulai);
						calendarSelesai.set(Calendar.MINUTE,
								menitMulai + pertemuan.getBolehAbsenSetelahWaktuMulaiDalamMenit());
						calendarSelesai.set(Calendar.SECOND, 1);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/ScanBerhasilAction.java:1413");
//					ais.common.Common.tampilErrorJikaAdmin(e);
				}

				Statusabsensi statusabsensi = (Statusabsensi) (mahasiswa != null
						? ConstantValues.ambil(Statusabsensi.class.getName(),
								pertemuan.retreiveAbsensiId(mahasiswa.getId()))
						: dosen != null
								? ConstantValues.ambil(Statusabsensi.class.getName(),
										pertemuan.retreiveAbsensiId(dosen.getId()))
								: biodataCalonMahasiswa != null ? ConstantValues.ambil(Statusabsensi.class.getName(),
										pertemuan.retreiveAbsensiId(biodataCalonMahasiswa.getId())) : null);
				if (statusabsensi == null) {
					statusabsensi = ConstantValues.BELUM_ABSEN;
				}

				if (ConstantValues.BELUM_ABSEN != null && ConstantValues.MASUK != null
						&& (statusabsensi.getId().equals(ConstantValues.BELUM_ABSEN.getId())
								|| statusabsensi.getId().equals(ConstantValues.MASUK.getId()))) {

					keterangan = (keterangan == null || keterangan.trim().isEmpty() ? "" : keterangan + " ") + "";
					int selisih = 0;
					int toleransiHari = 0;
					Date currentDate = WaktuUtil.getDate();
					boolean harusSesuai = Common.bolehKonfigurasi("absen_harus_sesuai_waktu");
					if (harusSesuai) {

						selisih = pertemuan.getTanggal() == null ? 0
								: Math.abs(Common.getBetweenTwoDates(currentDate, pertemuan.getTanggal())) - 1;

						toleransiHari = pertemuan.getPerkuliahan() == null ? 1000
								: pertemuan.getPerkuliahan().getBatasWaktuBolehAbsenKehadiran();

						if (Common.bolehKonfigurasi("jumlah_hari_batas_waktu_pakai_default", Konfigurasi.TIDAK_AKTIF)) {
							try {
								toleransiHari = Integer.parseInt(Common
										.getKonfigurasi("jumlah_hari_batas_waktu_dalam_hari", "0").getNilai().trim());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/ScanBerhasilAction.java:1450");
								// TODO: handle exception
							}
						}
					}

					if (harusSesuai && pertemuan.getPerkulaiahnOnlineHarusSesuaiJadwal() && selisih > toleransiHari) {

						String s = "Absensi gagal pada " + Common.dateFormat5.get().format(currentDate)
								+ ", karena absensi online telah terlewat/belum mulai " + selisih
								+ " hari, batas maksimal boleh absen adalah " + toleransiHari
								+ " hari setelah/sebelum pertemuan dilaksanakan";
						if (lat != null && lng != null) {
							s += ", lokasi absen " + map + " ";
						}

						if (urlFoto != null && !urlFoto.trim().isEmpty()) {
							s += ", " + (video ? "video" : "foto") + " absen " + urlFoto + " ";
						}

						keterangan = (keterangan.isEmpty() ? s : s + ";") + keterangan;

						Html html = new ais.ui.util.MyHtml();
						html.setContent(pre + imgeHtml + "<strong><font>" + s
								+ " Absensi online gagal dilakukan</font></strong><br><img src=\""
								+ Common.getRequestHostWithProtocol() + "/img/oh.gif\" alt=\"WebP rules.\" />" + pos);

						jsonObject.put(keyTgl + "_info", s);
						pertemuan.put(jsonObject.toString(), "sejarah");

						rowUtama.appendChild(html);

					}

					else if (pertemuan.getPerkulaiahnOnlineHarusSesuaiJadwal()
							&& currentDate.before(calendarMulai.getTime())) {
						String d = (SmartDateTimeUtil.getDayString(calendarMulai.getTime(), null)
								+ Common.dateFormat5.get().format(calendarMulai.getTime()));

						String s = "Absensi gagal pada " + Common.dateFormat5.get().format(WaktuUtil.getDate())
								+ ", karena absensi online belum dimulai " + d;
						if (lat != null && lng != null) {
							s += ", lokasi absen " + map + " ";
						}

						if (urlFoto != null && !urlFoto.trim().isEmpty()) {
							s += ", " + (video ? "video" : "foto") + " absen " + urlFoto + " ";
						}

						keterangan = (keterangan.isEmpty() ? s : s + ";") + keterangan;

						Html html = new ais.ui.util.MyHtml();
						html.setContent(pre + imgeHtml
								+ "<strong><font>Absensi online akan di-mulai " + d
								+ " (toleransi absen online " + pertemuan.getBolehAbsenSebelumWaktuMulaiDalamMenit()
								+ " menit sebelum dan " + pertemuan.getBolehAbsenSetelahWaktuMulaiDalamMenit()
								+ " setelah pertemuan)."
								+ " Absensi online gagal dilakukan</font></strong><br><img src=\""
								+ Common.getRequestHostWithProtocol() + "/img/oh.gif\" alt=\"WebP rules.\" />" + pos);

						jsonObject.put(keyTgl + "_info", s);
						pertemuan.put(jsonObject.toString(), "sejarah");

						rowUtama.appendChild(html);
					} else if (pertemuan.getPerkulaiahnOnlineHarusSesuaiJadwal()
							&& currentDate.after(calendarSelesai.getTime())) {
						String d = (SmartDateTimeUtil.getDayString(calendarSelesai.getTime(), null)
								+ Common.dateFormat5.get().format(calendarSelesai.getTime()));

						String s = "Absensi gagal pada " + Common.dateFormat5.get().format(WaktuUtil.getDate())
								+ ", karena absensi online telah terlewat " + d;
						if (lat != null && lng != null) {
							s += ", lokasi absen " + map + " ";
						}
						if (urlFoto != null && !urlFoto.trim().isEmpty()) {
							s += ", " + (video ? "video" : "foto") + " absen " + urlFoto + " ";
						}
						keterangan = (keterangan.isEmpty() ? s : s + ";") + keterangan;

						Html html = new ais.ui.util.MyHtml();
						html.setContent(pre + imgeHtml
								+ "<strong><font>Absensi online telah terlewat, dan telah berakhir "
								+ d + " (toleransi absen online " + pertemuan.getBolehAbsenSebelumWaktuMulaiDalamMenit()
								+ " menit sebelum dan " + pertemuan.getBolehAbsenSetelahWaktuMulaiDalamMenit()
								+ " setelah pertemuan)."
								+ " Absensi online gagal dilakukan</font></strong><br><img src=\""
								+ Common.getRequestHostWithProtocol() + "/img/oh.gif\" alt=\"WebP rules.\" />" + pos);

						jsonObject.put(keyTgl + "_info", s);
						pertemuan.put(jsonObject.toString(), "sejarah");

						rowUtama.appendChild(html);
					} else {

						String s = "Absensi sukses pada " + Common.dateFormat5.get().format(WaktuUtil.getDate());
						if (lat != null && lng != null) {
							s += ", lokasi absen " + map + " ";
						}
						if (urlFoto != null && !urlFoto.trim().isEmpty()) {
							s += ", " + (video ? "video" : "foto") + " absen " + urlFoto + " ";
						}
						keterangan = (keterangan.isEmpty() ? s : s + ";") + keterangan;

						statusabsensi = ConstantValues.MASUK;

						Html html = new ais.ui.util.MyHtml();
						html.setContent(pre + imgeHtml
								+ "<strong><font>Selamat, absensi berhasil dilakukan. "
								+ " (toleransi absen online " + pertemuan.getBolehAbsenSebelumWaktuMulaiDalamMenit()
								+ " menit sebelum dan " + pertemuan.getBolehAbsenSetelahWaktuMulaiDalamMenit()
								+ " setelah pertemuan)." + "</font></strong><br><img src=\""
								+ Common.getRequestHostWithProtocol()
								+ "/img/success-icon_big.png\" alt=\"WebP rules.\" />" + pos);

						rowUtama.appendChild(html);

						jsonObject.put(keyTgl + "_info", s);
						pertemuan.put(jsonObject.toString(), "sejarah");

					}

					pertemuan.populate(
							siswa != null ? siswa.getId()
									: guru != null ? guru.getId()
											: mahasiswa != null ? mahasiswa.getId()
													: dosen != null ? dosen.getId()
															: biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null ? biodataCalonMahasiswa.getId() : -1L,
							statusabsensi, keterangan, null, Common.timeFormat2.get().format(WaktuUtil.getDate()),
							pertemuan.getWaktuSelesai(),
							siswa != null ? "Siswa"
									: guru != null ? "Guru"
											: mahasiswa != null ? "Mahasiswa"
													: dosen != null ? "Dosen"
															: biodataCalonMahasiswa != null ? "Calon Mahasiswa" : "");
					simpanPertemuanAbsensi(pertemuan);
				} else if (statusabsensi != null) {

					String s = "Status kehadiran Anda telah dinyatakan \"" + statusabsensi.getNama()
							+ "\", sehingga Anda tidak bisa melakukan absen ulang.";

					jsonObject.put(keyTgl + "_info", s);
					pertemuan.put(jsonObject.toString(), "sejarah");

					Html html = new ais.ui.util.MyHtml();
					html.setContent(pre + imgeHtml + "<strong><font>" + s
							+ "</font></strong><br><img src=\"" + Common.getRequestHostWithProtocol()
							+ "/img/oh.gif\" alt=\"WebP rules.\" />" + pos);

					rowUtama.appendChild(html);

				}
			}
		}
	}

	private void simpanStatusKehadiran(Session session, StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian) throws Exception {
		Transaction transaction = null;
		try {
			HibernateUtil.assertSessionOpen(session, "Session absensi harian");
			transaction = session.beginTransaction();
			if (statuskehadiranKaryawanHarian.getId() == null) {
				session.save(statuskehadiranKaryawanHarian);
			} else {
				session.saveOrUpdate(statuskehadiranKaryawanHarian);
			}
			transaction.commit();
		} catch (Exception e) {
			rollbackQuietly(transaction);
			throw e;
		}
	}

	private void simpanPertemuanAbsensi(Pertemuan pertemuan) throws Exception {
		Session session = null;
		Transaction transaction = null;
		try {
			session = HibernateUtil.openSession();
			HibernateUtil.assertSessionOpen(session, "Session update absensi pertemuan");
			transaction = session.beginTransaction();
			Common.refreshUpdate(session, pertemuan, false);
			session.flush();
			transaction.commit();
		} catch (Exception e) {
			rollbackQuietly(transaction);
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private void rollbackQuietly(Transaction transaction) {
		try {
			if (transaction != null && !transaction.wasCommitted() && !transaction.wasRolledBack()) {
				transaction.rollback();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

}
