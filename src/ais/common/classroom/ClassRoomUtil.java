package ais.common.classroom;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.ClassroomScopes;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.Student;
import com.google.api.services.classroom.model.Teacher;
import com.google.api.services.classroom.model.Topic;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.GoogleCommon;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.FormulirKegiatan;
import ais.database.model.FormulirKegiatanPeserta;
import ais.database.model.GCalendarCode;
import ais.database.model.GeneralValueObject;
import ais.database.model.JadwalUjianPMB;
import ais.database.model.KrsMahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Ruang;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.FileFoto;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Adapter layanan eksternal/per-pengguna untuk class room util. Tipe ini membungkus autentikasi,
 * client API, dan mapping data layanan tersebut agar detail integrasi tidak disalin ke action
 * pemanggil.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code JsonFactory JSON_FACTORY}, {@code
 * String username}, {@code FileDataStoreFactory dataStoreFactory}, {@code Classroom service}, {@code
 * HttpTransport httpTransport}, {@code List SCOPES}; inisialisasi/lifecycle ({@code initService()});
 * pembacaan/pencarian ({@code getCredentials()}, {@code getOrganizer()}, {@code getAttendee()}); mutasi data
 * ({@code proses()}); operasi domain lain ({@code displayLink()}, {@code cretaeTimerWaiting()}, {@code
 * kirimEvent()}, {@code createButton()}); konfigurasi constructor: {@code dataStoreFactory}, {@code
 * httpTransport}, {@code lokasi}, {@code username}. Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> operasi dapat membaca kredensial per pengguna, melakukan I/O jaringan, menyegarkan
 * token, atau memetakan data remote. Jangan membagikan client/token antar pengguna; gunakan adapter ini sebagai
 * satu batas integrasi dan tangani kegagalan layanan luar.</p>
 */
public class ClassRoomUtil {
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private String username;
	private FileDataStoreFactory dataStoreFactory;
	private Classroom service = null;

	/** Global instance of the HTTP transport. */
	public HttpTransport httpTransport;

	public ClassRoomUtil(Tbmuser tbmuser) {

		try {
			username = tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId();
			String lokasi = ConstantValues.lokasiFileTemproraryTemp;

			if (lokasi.endsWith("/")) {
				lokasi += username + "_" + GoogleCommon.getGoogle_classroom_client_id();
			} else {
				lokasi += "/" + username + "_" + GoogleCommon.getGoogle_classroom_client_id();
			}

			System.out.println("Simpan ke lokasi -> " + lokasi);

			java.io.File file = new java.io.File(lokasi);
			if (!file.exists()) {
				file.mkdirs();
			}
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			dataStoreFactory = new FileDataStoreFactory(file);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		} catch (Throwable t) {
			t.printStackTrace(); ais.common.ErrorAuditUtil.record(t, "auto-audit src/ais/common/classroom/ClassRoomUtil.java:115");
		}
	}

	public static List<String> SCOPES = new ArrayList<String>();
	static {
		SCOPES.add(ClassroomScopes.CLASSROOM_COURSES);
		SCOPES.add(ClassroomScopes.CLASSROOM_TOPICS);
		SCOPES.add(ClassroomScopes.CLASSROOM_COURSEWORK_ME);
		SCOPES.add(ClassroomScopes.CLASSROOM_COURSEWORK_STUDENTS);
		SCOPES.add(ClassroomScopes.CLASSROOM_ROSTERS);
		SCOPES.add(ClassroomScopes.CLASSROOM_PROFILE_EMAILS);
		SCOPES.add(ClassroomScopes.CLASSROOM_PROFILE_PHOTOS);
	}

	public Credential getCredentials(FileDataStoreFactory dataStoreFactory, final String username) throws Exception {

		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
				new StringReader(GoogleCommon.getGoogle_classroom_key()));

		GoogleAuthorizationCodeFlow flow;

		flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
				.setDataStoreFactory(dataStoreFactory).setAccessType("offline").build();

		Credential credential = new AuthorizationCodeInstalledApp(flow, new VerificationCodeReceiver() {

			@Override
			public String waitForCode() throws IOException {
				// TODO Auto-generated method stub

				Session session = HibernateUtil.currentNativeSession();
				GCalendarCode gclassroomCode = (GCalendarCode) session.createCriteria(GCalendarCode.class)
						.add(Restrictions.eq("nama", username)).setMaxResults(1).uniqueResult();
				HibernateUtil.closeSession();

				return gclassroomCode != null ? gclassroomCode.getKeterangan()
						: "4/N-D27v1qgeomdHvvJdmgcCq6NfugLlRfXhTY3LRf_tc";
			}

			@Override
			public void stop() throws IOException {

			}

			@Override
			public String getRedirectUri() throws IOException {
				// TODO Auto-generated method stub
				return GoogleCommon.getRedirect_url_classroom();
			}
		})

				.authorize("user");

		// Authorization code SEKALI PAKAI sudah berhasil ditukar jadi token: hapus baris
		// gcalendar_code SEKARANG (bukan hanya menunggu otorisasi ulang berikutnya) agar kode
		// OAuth mentah tidak tersimpan lama di DB tanpa enkripsi (risiko data-at-rest). Pola sama
		// dengan ais.common.calendar.CalendarUtil#hapusGCalendarCode, kelas berbeda karena
		// ClassRoomUtil menyalin struktur CalendarUtil tanpa berbagi kode.
		hapusGCalendarCode(username);

		credential.setExpiresInSeconds(100000000L);

		return credential;
	}

	/**
	 * Hapus baris {@code gcalendar_code} milik {@code username} setelah authorization code
	 * berhasil ditukar menjadi token OAuth. Code Google bersifat SEKALI PAKAI sehingga baris lama
	 * yang tertinggal sudah tidak valid ditukar ulang, tapi tetap kredensial sensitif -- jangan
	 * dibiarkan tersimpan mentah di DB sampai user melakukan otorisasi ulang berikutnya.
	 */
	private static void hapusGCalendarCode(String username) {
		if (username == null) {
			return;
		}
		Session session = HibernateUtil.openSession();
		org.hibernate.Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List<?> daftar = session.createCriteria(GCalendarCode.class).add(Restrictions.eq("nama", username))
					.list();
			for (int i = 0; i < daftar.size(); i++) {
				session.delete(daftar.get(i));
			}
			tx.commit();
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/classroom/ClassRoomUtil.java:hapusGCalendarCode");
				}
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public void initService() throws Exception {
		if (service == null) {
			service = new Classroom.Builder(httpTransport, JSON_FACTORY, getCredentials(dataStoreFactory, username))
					.setApplicationName(GoogleCommon.APPLICATION_NAME).build();
		}
	}

	public void displayLink(final EventListener eventListener) throws Exception {
		GoogleCommon.codes.remove(username);
		final MyWindow window = new MyWindow("Calendar", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("300px");
		window.setWidth("400px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Common.initKeteranganSatuKolom(rows,
				"Anda belum terhubung ke google classroom, untuk menghubungkan, klik tombol berikut :");

		MyButtonConfig a = new MyButtonConfig("Hubungkan ke Google Classroom sekarang",
				FileFoto.icon("classroom.google"));
		a.setStyle("font-size:14px;font-weight: bolder;");
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
				GoogleCommon.codes.remove(username);
				String url = "https://accounts.google.com/o/oauth2/auth?access_type=offline&client_id="
						+ GoogleCommon.getGoogle_classroom_client_id() + "&redirect_uri="
						+ GoogleCommon.getRedirect_url_classroom() + "&response_type=code&scope="
						+ ClassroomScopes.CLASSROOM_COURSES + URLEncoder.encode(" ", "UTF-8")
						+ ClassroomScopes.CLASSROOM_TOPICS + URLEncoder.encode(" ", "UTF-8")
						+ ClassroomScopes.CLASSROOM_COURSEWORK_ME + URLEncoder.encode(" ", "UTF-8")
						+ ClassroomScopes.CLASSROOM_COURSEWORK_STUDENTS + URLEncoder.encode(" ", "UTF-8")
						+ ClassroomScopes.CLASSROOM_ROSTERS + URLEncoder.encode(" ", "UTF-8")
						+ ClassroomScopes.CLASSROOM_PROFILE_EMAILS + URLEncoder.encode(" ", "UTF-8")
						+ ClassroomScopes.CLASSROOM_PROFILE_PHOTOS + "&state="
						+ URLEncoder.encode(Common.getRequestHostWithProtocol() + "/accept.jsp?u=" + username, "UTF-8");
				Clients.evalJavaScript(
						"popupCenter({url: '" + url + "', title: 'Hubungkan ke Classroom', w: 500, h: 500});");
			}

		});

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(a);

		final Timer timer = new Timer(200);
		timer.setRepeats(true);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event event) throws Exception {
				if (GoogleCommon.codes.containsKey(username)) {
					String kode = GoogleCommon.codes.get(username);
					GCalendarCode gclassroomCode;
					try {

						Session session = HibernateUtil.currentNativeSession();
						gclassroomCode = (GCalendarCode) session.createCriteria(GCalendarCode.class)
								.add(Restrictions.eq("nama", username)).setMaxResults(1).uniqueResult();
						if (gclassroomCode == null) {
							gclassroomCode = new GCalendarCode();
						}
						gclassroomCode.setNama(username);
						gclassroomCode.setKeterangan(kode.trim());

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, (gclassroomCode));
						session.getTransaction().commit();

						HibernateUtil.closeSession();
					} catch (Exception e) {
						HibernateUtil.rollbackTransaction();
					}
					GoogleCommon.codes.remove(username);
					eventListener.onEvent(event);

					window.detach();
					timer.stop();
					timer.detach();
				}
			}
		});
		timer.start();

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
			public void onEvent(org.zkoss.zk.ui.event.Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		window.onModal();
	}

	public static void cretaeTimerWaiting(final List<Course> events, final EventListener eventListener) {
		final Timer timer = new Timer(500);
		timer.setRepeats(true);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
				if (!events.isEmpty()) {

					String info = "";
					if (events.size() == 1) {
						info += events.get(0).getDescription() + " berhasil tersingkron ke classroom";
					} else if (events.isEmpty()) {
						info = "Tidak ada data yang berhasil tersinkron ke classroom";
					} else {
						info += "Terdapat " + events.size()
								+ " pertemuan yang berhasil tersingkron ke classroom, yaitu sebagai berikut :\n";
						int index = 1;
						for (Course e : events) {
							info += (index + ". " + e.getDescription() + ".\n");
							index++;
						}
					}

					MyMessageboxConfig.show(info, "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
							new EventListener() {

								@Override
								public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
									eventListener.onEvent(null);
									String url = "https://classroom.google.com";
									if (events.size() > 0) {
										url = events.get(0).getAlternateLink();
									}
									if (Common.isMobile()) {
										ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
									} else {

										Clients.evalJavaScript("popupCenter({url: '" + url
												+ "', title: 'Classroom', w: 1200, h: 600});");

									}
								}
							});

					timer.detach();
				}
			}
		});
		timer.start();
	}

	public void kirimEvent(Label label, VOPembelajaran voPembelajaran, PerguruanTinggi perguruanTinggi,
			EventListener eventListener) {

		label.setValue("Mengirim data ke classroom..");
		try {
			initService();

			List<Course> courses = new ArrayList<Course>();

			String key = voPembelajaran.infoSimple();

			label.setValue("Mengirim \"" + key + "\" ke classroom..");

			try {
				String courseId = voPembelajaran.retreive("classroom_id");

				Course course = null;
				System.out.println("courseId -> " + courseId);
				if (courseId != null && !courseId.trim().isEmpty()) {
					try {
						course = service.courses().get(courseId).execute();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/classroom/ClassRoomUtil.java:370");
					}
				}
				boolean baru = course == null;
				if (course == null) {
					course = new Course();
				}

				Ruang ruang = null;
				String kelas = "";
				if (voPembelajaran instanceof Perkuliahan) {
					Perkuliahan perkuliahan = (Perkuliahan) voPembelajaran;
					ruang = perkuliahan.getRuang();
					kelas = perkuliahan.getKelas();
				}

				String lokasi = (perguruanTinggi != null && (!perguruanTinggi.getAlamat1().trim().isEmpty()
						|| !perguruanTinggi.getAlamat2().trim().isEmpty())
								? (perguruanTinggi.getAlamat1() + " " + perguruanTinggi.getAlamat2() + ", ")
								: "")
						+ voPembelajaran.infoSimple() + ",  di " + Common.getRequestHostWithProtocol();

				course.setName(voPembelajaran.ambilKeyword())
						.setRoom((ruang == null ? "" : ruang.getNama() + " - ") + (lokasi))
						.setDescription(voPembelajaran.infoSimple())
						.setDescriptionHeading(
								voPembelajaran.ambilTahunAkademik() + "/" + voPembelajaran.ambilJenisSemester()
										+ (kelas == null || kelas.trim().isEmpty() ? "" : "/" + kelas) + "/"
										+ voPembelajaran.ambilSemester())
						.setOwnerId("me").setEnrollmentCode(key)
						.setSection(voPembelajaran.ambilTahunAkademik() + "-" + voPembelajaran.ambilJenisSemester())
						.setCourseState("PROVISIONED");

				if (baru) {
					course = service.courses().create(course).execute();
				} else {
					service.courses().update(courseId, course).execute();
				}
				String link = course.getAlternateLink();
				String pretty = course.toPrettyString();
				System.out.println("pretty -> " + pretty);
				voPembelajaran.put(link, "ClasroomAlternateLink");
				voPembelajaran.put(course.getId(), "classroom_id");
				courses.add(course);

				String oldEventclassroom = voPembelajaran.getCourse();
				voPembelajaran.setCourse(pretty);
				if (!oldEventclassroom.equals(pretty)) {
					Session session = HibernateUtil.currentNativeSession();
					try {
						session.getTransaction().begin();
						session.update(voPembelajaran);
						session.getTransaction().commit();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/classroom/ClassRoomUtil.java:424");
					}
				}

				List<Pertemuan> pertemuans = voPembelajaran.ambilPertemuanList();
				for (Pertemuan pertemuan : pertemuans) {
					if (pertemuan.getAktif()) {
						String info = pertemuan.getPertemuanKe() + ". " + pertemuan.getTopik() + " ("
								+ pertemuan.getStatusPertemuan().getNama() + ") "
								+ Common.dateFormat4.get().format(pertemuan.getTanggal());
						label.setValue("Mengirim \"" + info + "\" ke classroom..");
						Topic topic = null;
						try {

							String topic_pertemuan_id = pertemuan.retreive("topic_pertemuan_id");
							if (topic_pertemuan_id != null && !topic_pertemuan_id.trim().isEmpty()) {
								// topic = service.courses().topics().get(courseId,
								// topic_pertemuan_id).execute();
								// topic.setName(info);
								// topic =
								// service.courses().topics().patch(courseId,
								// topic_pertemuan_id, topic).execute();
							} else {
								topic = new Topic().setCourseId(courseId).setName(info);
								topic = service.courses().topics().create(courseId, topic).execute();
							}
							pertemuan.put(topic.getTopicId(), "topic_pertemuan_id");
							System.out.printf("Topic '%s' in the course with ID '%s'.\n", topic.toPrettyString(),
									courseId);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/classroom/ClassRoomUtil.java:454");
						}
					}
				}

				List<String> emailsOrgnazer = getOrganizer(voPembelajaran);
				System.out.printf("emailsOrgnazer '%s'\n", emailsOrgnazer);
				for (String email : emailsOrgnazer) {
					label.setValue("Mengirim pengajar \"" + email + "\" ke classroom..");
					Teacher teacher = new Teacher().setUserId(email);
					try {
						teacher = service.courses().teachers().create(courseId, teacher).execute();
						System.out.printf("User '%s' was added as a teacher to the course with ID '%s'.\n",
								teacher.getProfile().getName().getFullName(), courseId);
					} catch (GoogleJsonResponseException e) {
						GoogleJsonError error = e.getDetails();
						if (error.getCode() == 403) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/classroom/ClassRoomUtil.java:471");
							break;
						} else if (error.getCode() == 409) {
							System.out.printf("User '%s' is already a member of this course.\n", email);
						} else {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/classroom/ClassRoomUtil.java:476");
						}
					}
				}

				Set<String> emailsAtendee = getAttendee(voPembelajaran);
				System.out.printf("emailsAtendee '%s'\n", emailsAtendee);
				for (String email : emailsAtendee) {
					label.setValue("Mengirim mahasiswa \"" + email + "\" ke classroom..");
					Student student = new Student().setUserId(email);
					try {
						student = service.courses().students().create(courseId, student)
								.setEnrollmentCode(course.getEnrollmentCode()).execute();
						System.out.printf("User '%s' was enrolled as a student in the course with ID '%s'.\n",
								student.getProfile().getName().getFullName(), courseId);
					} catch (GoogleJsonResponseException e) {
						GoogleJsonError error = e.getDetails();
						if (error.getCode() == 403) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/classroom/ClassRoomUtil.java:494");
							break;
						} else if (error.getCode() == 409) {
							System.out.println(email + " already a member of this course.");
						} else {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/classroom/ClassRoomUtil.java:499");
						}
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			if (eventListener != null) {
				eventListener.onEvent(new org.zkoss.zk.ui.event.Event("", null, courses));
			}
			label.setValue("");
		} catch (Exception e) {
			label.setValue("Error");
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private List<String> getOrganizer(VOPembelajaran voPembelajaran) {
		List<String> emails = new ArrayList<String>();
		List<Dosen> dosens = voPembelajaran.populateDosenBuNama();
		for (Dosen dosen : dosens) {
			if (dosen.getEmail() != null && !dosen.getEmail().trim().isEmpty()) {
				String email = dosen.getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		}
		dosens = null;
		String email = Common.getKonfigurasi("alamat_email_monitoring", "").getNilai();
		for (String e : email.split(",")) {
			if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
				emails.add(e.trim());
			}
		}
		return emails;
	}

	@SuppressWarnings("unchecked")
	private Set<String> getAttendee(VOPembelajaran voPembelajaran) {

		Set<String> emails = new HashSet<String>();
		if (voPembelajaran instanceof Perkuliahan) {
			for (Long detailperkuliahanid : ((Perkuliahan) voPembelajaran).ambilDetailperkuliahan()) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getMahasiswa().getEmail() != null
							&& !detailperkuliahan.getMahasiswa().getEmail().trim().isEmpty()) {

						String email = detailperkuliahan.getMahasiswa().getEmail();
						for (String e : email.split(",")) {
							if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
								emails.add(e.trim());
							}
						}
					}
				}
			}
		} else if (voPembelajaran instanceof JadwalUjianPMB) {
			JadwalUjianPMB jadwalUjianPMB = (JadwalUjianPMB) voPembelajaran;
			Session session = HibernateUtil.currentNativeSession();
			List<String> d = session.createCriteria(BiodataCalonMahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("gelombangPendaftaran",
							jadwalUjianPMB.getUjianPMB().getGelombangPendaftaran()))
					.add(jadwalUjianPMB.getPaket() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("paket", jadwalUjianPMB.getPaket()))
					.setProjection(Projections.groupProperty("email")).add(Restrictions.isNotNull("email"))
					.add(Restrictions.ne("email", "")).list();
			HibernateUtil.closeSession();
			for (String email : d) {
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		} else if (voPembelajaran instanceof FormulirKegiatan) {
			Session session = HibernateUtil.currentNativeSession();
			List<Object[]> d = session.createCriteria(FormulirKegiatanPeserta.class)
					.add(Restrictions.eq("formulirKegiatan", voPembelajaran))
					.createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
					.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
					.setProjection(Projections.projectionList().add(Projections.property("dosen.email"))
							.add(Projections.property("mahasiswa.email")))
					.add(Restrictions.or(Restrictions.isNotNull("dosen.email"),
							Restrictions.isNotNull("mahasiswa.email")))
					.add(Restrictions.or(Restrictions.ne("dosen.email", ""), Restrictions.ne("mahasiswa.email", "")))
					.list();
			HibernateUtil.closeSession();
			for (Object[] a : d) {
				try {
					String email = a[0] == null ? a[1].toString() : a[0].toString();
					for (String e : email.split(",")) {
						if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
							emails.add(e.trim());
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/classroom/ClassRoomUtil.java:603");
				}
			}
		} else if (voPembelajaran instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) voPembelajaran;
			if (mahasiswaRequestTugasAkhir.getMahasiswa().getEmail() != null
					&& !mahasiswaRequestTugasAkhir.getMahasiswa().getEmail().trim().isEmpty()) {

				String email = mahasiswaRequestTugasAkhir.getMahasiswa().getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		} else if (voPembelajaran instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) voPembelajaran;
			if (skripsi.getMahasiswa().getEmail() != null && !skripsi.getMahasiswa().getEmail().trim().isEmpty()) {

				String email = skripsi.getMahasiswa().getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		} else if (voPembelajaran instanceof KelompokKkn) {
			for (MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn : ((KelompokKkn) voPembelajaran)
					.ambilMahasiswaDapatKelompokKkn(false)) {
				if (mahasiswaDapatKelompokKkn.getMahasiswa().getEmail() != null
						&& !mahasiswaDapatKelompokKkn.getMahasiswa().getEmail().trim().isEmpty()) {

					String email = mahasiswaDapatKelompokKkn.getMahasiswa().getEmail();
					for (String e : email.split(",")) {
						if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
							emails.add(e.trim());
						}
					}

				}
			}
		} else if (voPembelajaran instanceof KelompokPkl) {
			for (MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl : ((KelompokPkl) voPembelajaran)
					.ambilMahasiswaDapatKelompokPkl(false)) {
				if (mahasiswaDapatKelompokPkl.getMahasiswa().getEmail() != null
						&& !mahasiswaDapatKelompokPkl.getMahasiswa().getEmail().trim().isEmpty()) {
					String email = mahasiswaDapatKelompokPkl.getMahasiswa().getEmail();
					for (String e : email.split(",")) {
						if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
							emails.add(e.trim());
						}
					}
				}
			}
		} else if (voPembelajaran instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) voPembelajaran;
			if (krsMahasiswa.getMahasiswa().getEmail() != null
					&& !krsMahasiswa.getMahasiswa().getEmail().trim().isEmpty()) {
				String email = krsMahasiswa.getMahasiswa().getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		}

		return emails;
	}

	public void proses(final VOPembelajaran voPembelajaran, final PerguruanTinggi perguruanTinggi,
			final EventListener eventListener) {
		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses kirim data ke classroom .."));
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					Clients.clearBusy();
					timer.detach();
				} else if (label.getValue().equals("Error")) {
					displayLink(new EventListener() {

						@Override
						public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
							proses(voPembelajaran, perguruanTinggi, eventListener);
						}
					});
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				kirimEvent(label, voPembelajaran, perguruanTinggi, eventListener);
			}
		}).start();
	}

	public static MyToolbarbuttonConfig createButton(final VOPembelajaran voPembelajaran, final DataLoader dataLoader) {
		MyToolbarbuttonConfig a = new MyToolbarbuttonConfig("Classroom", FileFoto.icon("classroom.google"));
		a.setVisible(false);
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Tbmuser tbmuser = Common.getCurrentUser();
				ClassRoomUtil classRoomUtil = new ClassRoomUtil(tbmuser);

				PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

				if (tbmuser != null && tbmuser.ambilDosen() != null
						&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
						&& tbmuser.ambilDosen().getPerguruanTinggi() != null) {
					selectedPerguruanTinggi = tbmuser.ambilDosen().getPerguruanTinggi();
				} else if (tbmuser != null && tbmuser.getMahasiswa() != null
						&& tbmuser.getMahasiswa().getJurusan() != null
						&& tbmuser.getMahasiswa().getJurusan().getFakultas() != null
						&& tbmuser.getMahasiswa().getJurusan().getFakultas().getPerguruanTinggi() != null) {
					selectedPerguruanTinggi = tbmuser.getMahasiswa().getJurusan().getFakultas().getPerguruanTinggi();
				} else if (tbmuser != null && tbmuser.ambilFakultas() != null
						&& tbmuser.ambilFakultas().getPerguruanTinggi() != null) {
					selectedPerguruanTinggi = tbmuser.ambilFakultas().getPerguruanTinggi();
				}
				final List<Course> events = new ArrayList<Course>();

				classRoomUtil.proses(voPembelajaran, selectedPerguruanTinggi, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Course> eventsa = (List<Course>) arg0.getData();
						events.addAll(eventsa);
					}
				});

				ClassRoomUtil.cretaeTimerWaiting(events, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dataLoader.loadData(null);
					}
				});
			}
		});
		return a;
	}
}
