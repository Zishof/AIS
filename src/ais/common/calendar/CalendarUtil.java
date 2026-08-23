package ais.common.calendar;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
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
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.GoogleCommon;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GCalendarCode;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.InterviewCalonMahasiswa;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.FileFoto;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class CalendarUtil {

	private static boolean gangguanJaringanAtauBelumOtorisasi(Throwable error) {
		Throwable t = error;
		while (t != null) {
			if (t instanceof java.net.UnknownHostException || t instanceof java.net.ConnectException
					|| t instanceof java.net.SocketTimeoutException) return true;
			String pesan = t.getMessage();
			if (pesan != null && (pesan.indexOf("belum dihubungkan") >= 0
					|| pesan.indexOf("timed out") >= 0)) return true;
			t = t.getCause();
		}
		return false;
	}
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private String username;
	private FileDataStoreFactory dataStoreFactory;
	private Calendar service = null;

	/** Global instance of the HTTP transport. */
	public HttpTransport httpTransport;
	private Tbmuser tbmuser;

	public CalendarUtil(Tbmuser tbmuser) {

		try {
			this.tbmuser = tbmuser;
			username = tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId();
			String lokasi = ConstantValues.lokasiFileTemproraryTemp;

			if (lokasi.endsWith("/")) {
				lokasi += username + "_" + GoogleCommon.getGoogle_calendar_client_id();
			} else {
				lokasi += "/" + username + "_" + GoogleCommon.getGoogle_calendar_client_id();
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
			t.printStackTrace(); ais.common.ErrorAuditUtil.record(t, "auto-audit src/ais/common/calendar/CalendarUtil.java:107");
		}
	}

	public static boolean chekSudahAda(Pertemuan pertemuan, Tbmuser tbmuser) throws Exception {
		if (tbmuser != null) {
			List<String> emails = new ArrayList<String>();
			JSONObject jsonObject = new JSONObject(pertemuan.getCalendarEvent());
			if (!jsonObject.isNull("attendees")) {
				JSONArray attendees = jsonObject.getJSONArray("attendees");
				for (int i = 0; i < attendees.length(); i++) {
					try {
						JSONObject attendee = attendees.getJSONObject(i);
						emails.add(attendee.getString("email").trim().toLowerCase());
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/calendar/CalendarUtil.java:122");
					}
				}
			}
			if (!jsonObject.isNull("organizer")) {
				try {
					JSONObject organizer = jsonObject.getJSONObject("organizer");
					emails.add(organizer.getString("email").trim().toLowerCase());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/calendar/CalendarUtil.java:131");
				}
			}

//			System.out.println("emails -> " + emails);

			if (tbmuser != null && tbmuser.getMahasiswa() != null) {
				String email = tbmuser.getMahasiswa().getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						if (emails.contains(e.trim().toLowerCase())) {
							return true;
						}
					}
				}
			} else if (tbmuser != null && tbmuser.ambilDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
				String email = tbmuser.ambilDosen().getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						if (emails.contains(e.trim().toLowerCase())) {
							return true;
						}
					}
				}
			} else if (tbmuser != null && tbmuser.ambilGuru() != null) {
				String email = tbmuser.ambilGuru().getAlamatEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						if (emails.contains(e.trim().toLowerCase())) {
							return true;
						}
					}
				}
			} else if (tbmuser != null && tbmuser.getSiswa() != null) {
				String email = tbmuser.getSiswa().getAlamatEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						if (emails.contains(e.trim().toLowerCase())) {
							return true;
						}
					}
				}
			} else if (tbmuser != null && tbmuser.ambilPegawai() != null) {
				String email = tbmuser.ambilPegawai().getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						if (emails.contains(e.trim().toLowerCase())) {
							return true;
						}
					}
				}
			} else if (tbmuser != null) {
				String email = tbmuser.getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						if (emails.contains(e.trim().toLowerCase())) {
							return true;
						}
					}
				}
			}

		}
		return false;

	}

	public Credential getCredentials(FileDataStoreFactory dataStoreFactory, final String username) throws Exception {

		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
				new StringReader(GoogleCommon.getGoogle_calendar_key()));

		GoogleAuthorizationCodeFlow flow;

		flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets,
				Collections.singleton(CalendarScopes.CALENDAR)).setDataStoreFactory(dataStoreFactory)
				.setAccessType("offline").build();

		// Pakai kredensial TERSIMPAN (refresh token) bila masih ada → hindari menukar ulang
		// authorization code yang SEKALI PAKAI; penukaran ulang inilah yang memicu
		// "invalid_grant" (400 Bad Request) berulang tiap sinkronisasi kalender.
		try {
			Credential tersimpan = flow.loadCredential("user");
			if (tersimpan != null && tersimpan.getRefreshToken() != null
					&& !tersimpan.getRefreshToken().isEmpty()) {
				tersimpan.setExpiresInSeconds(100000000L);
				return tersimpan;
			}
		} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/calendar/CalendarUtil.java:220");
		}

		Credential credential;
		try {
			credential = new AuthorizationCodeInstalledApp(flow, new VerificationCodeReceiver() {

				@Override
				public String waitForCode() throws IOException {
					GCalendarCode gcalendarCode = null;
					try {
						Session session = HibernateUtil.currentNativeSession();
						gcalendarCode = (GCalendarCode) session.createCriteria(GCalendarCode.class)
								.add(Restrictions.eq("nama", username)).setMaxResults(1).uniqueResult();
					} finally {
						// currentNativeSession adalah ThreadLocal native dan wajib ditutup tuntas.
						HibernateUtil.closeSession();
					}

					if (gcalendarCode == null || gcalendarCode.getKeterangan() == null
							|| gcalendarCode.getKeterangan().trim().length() == 0) {
						// Jangan pernah memakai authorization-code contoh/hardcoded: code OAuth
						// bersifat sekali pakai dan pasti menghasilkan invalid_grant.
						throw new IOException("Google Calendar belum dihubungkan untuk user '" + username
								+ "'. Silakan lakukan otorisasi Calendar terlebih dahulu.");
					}
					return gcalendarCode.getKeterangan().trim();
				}

				@Override
				public void stop() throws IOException {

				}

				@Override
				public String getRedirectUri() throws IOException {
					// TODO Auto-generated method stub
					return GoogleCommon.getRedirect_url_calendar();
				}
			})

					.authorize("user");
		} catch (com.google.api.client.auth.oauth2.TokenResponseException e) {
			// invalid_grant: authorization code / refresh token sudah tak berlaku (kadaluarsa
			// atau dicabut). Hapus kredensial tersimpan yang rusak agar admin bisa OTORISASI
			// ULANG dari awal (Hubungkan Calendar), bukan terus gagal memakai token basi.
			try {
				if (flow.getCredentialDataStore() != null) {
					flow.getCredentialDataStore().delete("user");
				}
			} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/calendar/CalendarUtil.java:261");
			}
			throw e;
		}

		credential.setExpiresInSeconds(100000000L);

		return credential;
	}

	public void initService() throws Exception {
		if (service == null) {
			service = new Calendar.Builder(httpTransport, JSON_FACTORY, getCredentials(dataStoreFactory, username))
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
				"Anda belum terhubung ke google calendar, untuk menghubungkan, klik tombol berikut :");

		MyButtonConfig a = new MyButtonConfig("Hubungkan ke Google Calendar sekarang",
				FileFoto.icon("calendar.google"));
		a.setStyle("font-size:14px;font-weight: bolder;");
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
				GoogleCommon.codes.remove(username);
				String url = "https://accounts.google.com/o/oauth2/auth?access_type=offline&client_id="
						+ GoogleCommon.getGoogle_calendar_client_id() + "&redirect_uri="
						+ GoogleCommon.getRedirect_url_calendar() + "&response_type=code&scope="
						+ CalendarScopes.CALENDAR + "&state="
						+ URLEncoder.encode(Common.getRequestHostWithProtocol() + "/accept.jsp?u=" + username, "UTF-8");
				Clients.evalJavaScript(
						"popupCenter({url: '" + url + "', title: 'Hubungkan ke Calendar', w: 500, h: 500});");
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
					GCalendarCode gcalendarCode;
					try {

						Session session = HibernateUtil.currentNativeSession();
						gcalendarCode = (GCalendarCode) session.createCriteria(GCalendarCode.class)
								.add(Restrictions.eq("nama", username)).setMaxResults(1).uniqueResult();
						if (gcalendarCode == null) {
							gcalendarCode = new GCalendarCode();
						}
						gcalendarCode.setNama(username);
						gcalendarCode.setKeterangan(kode.trim());

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, (gcalendarCode));
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

	public static void cretaeTimerWaiting(final List<Event> events, final Date tanggal,
			final EventListener eventListener) {
		final Timer timer = new Timer(500);
		timer.setRepeats(true);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
				if (!events.isEmpty()) {

					String info = "";
					if (events.size() == 1) {
						info += events.get(0).getSummary() + " berhasil tersingkron ke kalender";
					} else if (events.isEmpty()) {
						info = "Tidak ada pertemuan yang berhasil tersinkron ke kalender";
					} else {
						info += "Terdapat " + events.size()
								+ " pertemuan yang berhasil tersingkron ke kalender, yaitu sebagai berikut :\n";
						int index = 1;
						for (com.google.api.services.calendar.model.Event e : events) {
							info += (index + ". " + e.getSummary() + ".\n");
							index++;
						}
					}

					MyMessageboxConfig.show(info, "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
							new EventListener() {

								@Override
								public void onEvent(org.zkoss.zk.ui.event.Event arg0) throws Exception {
									eventListener.onEvent(null);
									// String url =
									// "https://calendar.google.com";
									java.util.Calendar calendar = WaktuUtil.getCalendar();
									calendar.setTime(tanggal);
									String url = "https://calendar.google.com/calendar/r/day/"
											+ calendar.get(java.util.Calendar.YEAR) + "/"
											+ (calendar.get(java.util.Calendar.MONTH) + 1) + "/"
											+ calendar.get(java.util.Calendar.DATE);
									if (Common.isMobile()) {
										ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
									} else {

										Clients.evalJavaScript("popupCenter({url: '" + url
												+ "', title: 'Kalender', w: 1200, h: 600});");

									}
								}
							});

					timer.detach();
				}
			}
		});
		timer.start();
	}

	public void kirimEvent(Label label, TreeMap<String, Long> pertemuanss, PerguruanTinggi perguruanTinggi,
			EventListener eventListener) {
		// List the next 10 events from the primary calendar.
		label.setValue("Mengirim data pertemuan ke kalender..");
		try {
			initService();

			List<Pertemuan> pertemuans = new ArrayList<Pertemuan>();
			for (Long pertemuanid : pertemuanss.values()) {
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
				if (pertemuan != null) {
					pertemuans.add(pertemuan);
				}
			}

			Date mulai = null;
			Date sampai = null;
			for (Pertemuan pertemuan : pertemuans) {
				if (pertemuan.getAktif()) {
					if (mulai == null || pertemuan.getTanggal().before(mulai)) {
						mulai = pertemuan.getTanggal();
					}
					if (sampai == null || pertemuan.getTanggal().after(sampai)) {
						sampai = pertemuan.getTanggal();
					}
				}
			}

			String tglMulai = Common.dateFormat1.get().format(mulai == null ? WaktuUtil.getDate() : mulai) + " 00:00:00";
			String tglSampai = Common.dateFormat1.get().format(sampai == null ? WaktuUtil.getDate() : sampai) + " 23:59:59";

			System.out.println("Cari event yg sudah ada -> " + tglMulai + " sd " + tglSampai);
			String calendarId = "primary";
			DateTime start = new DateTime(Common.dateFormat3.get().parse(tglMulai));
			DateTime end = new DateTime(Common.dateFormat3.get().parse(tglSampai));
			com.google.api.services.calendar.model.Events eventsA = service.events().list(calendarId)
					.setMaxResults(10000).setTimeMin(start).setTimeMax(end).setOrderBy("startTime")
					.setSingleEvents(true).execute();
			List<Event> items = eventsA.getItems();
			if (items.isEmpty()) {
				System.out.println("No upcoming events found.");
			} else {
				System.out.println("Upcoming events");
				for (Event event : items) {

					start = event.getStart().getDateTime();
					if (start == null) {
						start = event.getStart().getDate();
					}
					System.out.printf("%s (%s)\n", event.getSummary(), start);
				}
			}

			List<Event> events = new ArrayList<Event>();
			int size = pertemuans.size();
			int index = 0;

			for (Pertemuan pertemuan : pertemuans) {
				if (pertemuan.getAktif()) {
					index++;

					String info = VOPembelajaran.infoSimple(pertemuan);

					label.setValue("Mengirim \"" + info + "\" ke kalender ("
							+ Common.numberFormat.get().format((index * 100.0) / size) + "%)..");

					try {
						String eventId = pertemuan.retreive("calendar_id");

						Event event = null;
						System.out.println("eventId -> " + eventId);
						if (eventId != null && !eventId.trim().isEmpty()) {
							for (Event evt : items) {
								boolean sama = evt.getId().split("_")[0].equals(eventId);
								System.out.println("evt.getId() -> " + evt.getId() + ", eventId -> " + eventId
										+ ", sama -> " + sama);
								if (sama) {
									eventId = evt.getId();
									event = evt;
									break;
								}
							}
						}
						boolean baru = event == null;
						if (event == null) {
							event = new Event();
						}

						event.setSummary(pertemuan.info() + ", " + info)
								.setLocation((perguruanTinggi != null && (!perguruanTinggi.getAlamat1().trim().isEmpty()
										|| !perguruanTinggi.getAlamat2().trim().isEmpty())
												? (perguruanTinggi.getAlamat1() + " " + perguruanTinggi.getAlamat2()
														+ ", ")
												: "")
										+ pertemuan.info() + " di " + Common.getRequestHostWithProtocol())
								.setDescription(pertemuan.getCatatan());

						String tgl = Common.dateFormat1.get().format(pertemuan.getTanggal());

						String mul = pertemuan.getWaktuMulai() == null ? "07:00"
								: pertemuan.getWaktuMulai().replaceAll("\\.", ":");
						String selesai = pertemuan.getWaktuSelesai() == null ? "18:00"
								: pertemuan.getWaktuSelesai().replaceAll("\\.", ":");

						Date mula = Common.dateFormat.get().parse(tgl + " " + mul);
						Date sampa = Common.dateFormat.get().parse(tgl + " " + selesai);

						java.util.Calendar calendarMulai = java.util.Calendar.getInstance(TimeZone.getDefault());
						calendarMulai.setTime(mula);

						java.util.Calendar calendarSampai = java.util.Calendar.getInstance(TimeZone.getDefault());
						calendarSampai.setTime(sampa);

						System.out.println("mulai -> " + Common.dateFormat.get().format(mula) + " s.d "
								+ Common.dateFormat.get().format(sampa));

						EventDateTime startA = new EventDateTime()
								.setDateTime(new DateTime(calendarMulai.getTimeInMillis()))
								.setTimeZone(WaktuUtil.gteTimezoneName());
						event.setStart(startA);

						EventDateTime endA = new EventDateTime()
								.setDateTime(new DateTime(calendarSampai.getTimeInMillis()))
								.setTimeZone(WaktuUtil.gteTimezoneName());
						event.setEnd(endA);

						String[] recurrence = new String[] { "RRULE:FREQ=DAILY;COUNT=1" };
						event.setRecurrence(Arrays.asList(recurrence));
						List<String> emailsOrgnazer = VOPembelajaran.getOrganizer(pertemuan);
						Set<String> emails = VOPembelajaran.getAttendee(pertemuan);
						emails.addAll(emailsOrgnazer);
						System.out.println("EventAttendee -> " + emails);

						List<EventAttendee> eventAttendee = new ArrayList<EventAttendee>();
						for (String email : emails) {
							eventAttendee.add(
									new EventAttendee().setEmail(email).setOrganizer(emailsOrgnazer.contains(email)));
						}

						event.setAttendees(eventAttendee);

						EventReminder[] reminderOverrides = new EventReminder[] {
								new EventReminder().setMethod("email").setMinutes(24 * 60),
								new EventReminder().setMethod("popup").setMinutes(30), };
						Event.Reminders reminders = new Event.Reminders().setUseDefault(false)
								.setOverrides(Arrays.asList(reminderOverrides));
						event.setReminders(reminders);

						event.setVisibility("public");
						event.setAnyoneCanAddSelf(true);
						if (baru) {
							event = service.events().insert(calendarId, event).execute();
						} else {
							service.events().update(calendarId, eventId, event).execute();
						}
						String link = event.getHtmlLink();
						String pretty = event.toPrettyString();
						System.out.println("event -> " + pretty);
						pertemuan.put(link, "html_link");
						pertemuan.put(event.getHangoutLink(), "hangoutLink");
						pertemuan.put(event.getId(), "calendar_id");
						events.add(event);

						String oldEventcalendar = pertemuan.getCalendarEvent();
						pertemuan.setCalendarEvent(pretty);
						if (!oldEventcalendar.equals(pretty)) {
							Session session = HibernateUtil.currentNativeSession();
							try {
								pertemuan.setOnlineMenggunakan(Pertemuan.GOOGLE_MEET);
								session.getTransaction().begin();
								session.update(pertemuan);
								session.getTransaction().commit();
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/calendar/CalendarUtil.java:617");
							}
						}

						tbmuser.checkEmailSudahAdaApaBelum(event.getCreator().getEmail());

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}

			if (eventListener != null) {
				eventListener.onEvent(new org.zkoss.zk.ui.event.Event("", null, events));
			}
			label.setValue("");
		} catch (com.google.api.client.auth.oauth2.TokenResponseException e) {
			service = null;
			// Label HARUS tepat "Error" agar timer onTimer memicu displayLink re-otorisasi Google.
			// Label deskriptif lain (mis. "Error: sesi...") tidak cocok dengan equals("Error").
			try { label.setValue("Error"); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/calendar/CalendarUtil.java:637");}
			System.err.println("Google Calendar meminta otorisasi ulang untuk user '" + username
					+ "': " + e.getMessage());
		} catch (Exception e) {
			try { label.setValue("Error"); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/calendar/CalendarUtil.java:640");}
			if (gangguanJaringanAtauBelumOtorisasi(e)) {
				System.err.println("Google Calendar sementara belum dapat dipakai untuk user '" + username
						+ "': " + e.getMessage());
			} else {
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	public void proses(final TreeMap<String, Long> pertemuans, final PerguruanTinggi perguruanTinggi,
			final EventListener eventListener) {
		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses kirim data ke kalender .."));
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
							proses(pertemuans, perguruanTinggi, eventListener);
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
				try {
					kirimEvent(label, pertemuans, perguruanTinggi, eventListener);
				} catch (Exception eKirim) {
					System.err.println("[CalendarUtil] kirimEvent gagal (OAuth/network): " + eKirim.getMessage());
				} finally {
					// Thread latar TANPA konteks ZK: kirimEvent memakai currentSession() yang jatuh ke
					// native ThreadLocal session. WAJIB ditutup di finally (clear/disconnect/close) lewat
					// HibernateUtil.closeSession() agar koneksi c3p0 TIDAK bocor ketika OAuth invalid_grant
					// (atau error lain) melempar keluar dari kirimEvent.
					try {
						ais.database.hibernate.HibernateUtil.closeSession();
					} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/calendar/CalendarUtil.java:691");
					}
				}
			}
		}).start();
	}

	public void proses(final PengumumanAkademis pengumumanAkademis, final EventListener eventListener) {
		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses kirim data ke kalender .."));
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
							proses(pengumumanAkademis, eventListener);
						}
					});
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();

		final PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		new Thread(new Runnable() {

			@Override
			public void run() {
				// Tutup native session di finally (thread latar tanpa konteks ZK) agar koneksi tak bocor.
				try {
					kirimEvent(label, pengumumanAkademis, perguruanTinggi, eventListener);
				} finally {
					try {
						ais.database.hibernate.HibernateUtil.closeSession();
					} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/calendar/CalendarUtil.java:740");
					}
				}
			}
		}).start();
	}

	public void proses(final GelombangPendaftaran gelombangPendaftaran, final EventListener eventListener) {
		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses kirim data ke kalender .."));
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
							proses(gelombangPendaftaran, eventListener);
						}
					});
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();

		final PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		new Thread(new Runnable() {

			@Override
			public void run() {
				// Tutup native session di finally (thread latar tanpa konteks ZK) agar koneksi tak bocor.
				try {
					kirimEvent(label, gelombangPendaftaran, perguruanTinggi, eventListener);
				} finally {
					try {
						ais.database.hibernate.HibernateUtil.closeSession();
					} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/calendar/CalendarUtil.java:789");
					}
				}
			}
		}).start();
	}

	public void proses(final InterviewCalonMahasiswa interviewCalonMahasiswa, final EventListener eventListener) {
		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses kirim data ke kalender .."));
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
							proses(interviewCalonMahasiswa, eventListener);
						}
					});
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();

		final PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		new Thread(new Runnable() {

			@Override
			public void run() {
				// Tutup native session di finally (thread latar tanpa konteks ZK) agar koneksi tak bocor.
				try {
					kirimEvent(label, interviewCalonMahasiswa, perguruanTinggi, eventListener);
				} finally {
					try {
						ais.database.hibernate.HibernateUtil.closeSession();
					} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/calendar/CalendarUtil.java:838");
					}
				}
			}
		}).start();
	}

	public void kirimEvent(Label label, PengumumanAkademis pengumumanAkademis, PerguruanTinggi perguruanTinggi,
			EventListener eventListener) {
		// List the next 10 events from the primary calendar.
		label.setValue("Mengirim data pertemuan ke kalender..");
		try {
			initService();

			List<Event> events = new ArrayList<Event>();

			String info = pengumumanAkademis.getJudul();

			label.setValue("Mengirim \"" + info + "\" ke kalender ...");
			String calendarId = "primary";
			try {
				String eventId = pengumumanAkademis.retreive("calendar_id");

				Event event = null;
				System.out.println("eventId -> " + eventId);
				if (eventId != null && !eventId.trim().isEmpty()) {
					try {
						service.events().get(calendarId, eventId);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/calendar/CalendarUtil.java:867");
					}
				}
				boolean baru = event == null;
				if (event == null) {
					event = new Event();
				}

				event.setSummary(pengumumanAkademis.getJudul())
						.setLocation((perguruanTinggi != null && (!perguruanTinggi.getAlamat1().trim().isEmpty()
								|| !perguruanTinggi.getAlamat2().trim().isEmpty())
										? (perguruanTinggi.getAlamat1() + " " + perguruanTinggi.getAlamat2() + ", ")
										: "")
								+ " atau online di " + Common.getRequestHostWithProtocol())
						.setDescription(pengumumanAkademis.getCatatan());

				String tgl = Common.dateFormat1.get().format(pengumumanAkademis.getTanggal());

				String mul = "07:00";
				String selesai = "18:00";

				Date mula = Common.dateFormat.get().parse(tgl + " " + mul);
				Date sampa = Common.dateFormat.get().parse(tgl + " " + selesai);

				java.util.Calendar calendarMulai = java.util.Calendar.getInstance(TimeZone.getDefault());
				calendarMulai.setTime(mula);

				java.util.Calendar calendarSampai = java.util.Calendar.getInstance(TimeZone.getDefault());
				calendarSampai.setTime(sampa);

				System.out.println(
						"mulai -> " + Common.dateFormat.get().format(mula) + " s.d " + Common.dateFormat.get().format(sampa));

				EventDateTime startA = new EventDateTime().setDateTime(new DateTime(calendarMulai.getTimeInMillis()))
						.setTimeZone(WaktuUtil.gteTimezoneName());
				event.setStart(startA);

				EventDateTime endA = new EventDateTime().setDateTime(new DateTime(calendarSampai.getTimeInMillis()))
						.setTimeZone(WaktuUtil.gteTimezoneName());
				event.setEnd(endA);

				String[] recurrence = new String[] { "RRULE:FREQ=DAILY;COUNT=1" };
				event.setRecurrence(Arrays.asList(recurrence));
				List<String> emailsOrgnazer = pengumumanAkademis.ambilOrganizer();
				Set<String> emails = pengumumanAkademis.ambilAttendee();
				emails.addAll(emailsOrgnazer);
				System.out.println("EventAttendee -> " + emails);

				List<EventAttendee> eventAttendee = new ArrayList<EventAttendee>();
				for (String email : emails) {
					eventAttendee.add(new EventAttendee().setEmail(email).setOrganizer(emailsOrgnazer.contains(email)));
				}

				event.setAttendees(eventAttendee);

				EventReminder[] reminderOverrides = new EventReminder[] {
						new EventReminder().setMethod("email").setMinutes(24 * 60),
						new EventReminder().setMethod("popup").setMinutes(30), };
				Event.Reminders reminders = new Event.Reminders().setUseDefault(false)
						.setOverrides(Arrays.asList(reminderOverrides));
				event.setReminders(reminders);

				event.setVisibility("public");
				event.setAnyoneCanAddSelf(true);
				if (baru) {
					event = service.events().insert(calendarId, event).execute();
				} else {
					service.events().update(calendarId, eventId, event).execute();
				}
				String link = event.getHtmlLink();
				String pretty = event.toPrettyString();
				System.out.println("event -> " + pretty);
				pengumumanAkademis.put(link, "html_link");
				pengumumanAkademis.put(event.getHangoutLink(), "hangoutLink");
				pengumumanAkademis.put(event.getId(), "calendar_id");
				events.add(event);

				String oldEventcalendar = pengumumanAkademis.getCalendarEvent();
				pengumumanAkademis.setCalendarEvent(pretty);
				if (!oldEventcalendar.equals(pretty)) {
					Session session = HibernateUtil.currentNativeSession();
					try {
						session.getTransaction().begin();
						session.update(pengumumanAkademis);
						session.getTransaction().commit();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/calendar/CalendarUtil.java:953");
					}
				}

				tbmuser.checkEmailSudahAdaApaBelum(event.getCreator().getEmail());

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			if (eventListener != null) {
				eventListener.onEvent(new org.zkoss.zk.ui.event.Event("", null, events));
			}
			label.setValue("");
		} catch (Exception e) {
			label.setValue("Error");
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public void kirimEvent(Label label, GelombangPendaftaran gelombangPendaftaran, PerguruanTinggi perguruanTinggi,
			EventListener eventListener) {
		// List the next 10 events from the primary calendar.
		label.setValue("Mengirim data pertemuan ke kalender..");
		try {
			initService();

			List<Event> events = new ArrayList<Event>();

			String info = gelombangPendaftaran.getNama();

			label.setValue("Mengirim \"" + info + "\" ke kalender ...");
			String calendarId = "primary";
			try {
				String eventId = gelombangPendaftaran.retreive("calendar_id");

				Event event = null;
				System.out.println("eventId -> " + eventId);
				if (eventId != null && !eventId.trim().isEmpty()) {
					try {
						service.events().get(calendarId, eventId);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/calendar/CalendarUtil.java:995");
					}
				}
				boolean baru = event == null;
				if (event == null) {
					event = new Event();
				}

				event.setSummary(gelombangPendaftaran.getNama())
						.setLocation((perguruanTinggi != null && (!perguruanTinggi.getAlamat1().trim().isEmpty()
								|| !perguruanTinggi.getAlamat2().trim().isEmpty())
										? (perguruanTinggi.getAlamat1() + " " + perguruanTinggi.getAlamat2() + ", ")
										: "")
								+ " atau online di " + Common.getRequestHostWithProtocol())
						.setDescription(gelombangPendaftaran.getKeterangan());

				String tgl = Common.dateFormat1.get().format(gelombangPendaftaran.getMulai());

				String mul = "07:00";
				String selesai = "18:00";

				Date mula = Common.dateFormat.get().parse(tgl + " " + mul);
				Date sampa = Common.dateFormat.get().parse(tgl + " " + selesai);

				java.util.Calendar calendarMulai = java.util.Calendar.getInstance(TimeZone.getDefault());
				calendarMulai.setTime(mula);

				java.util.Calendar calendarSampai = java.util.Calendar.getInstance(TimeZone.getDefault());
				calendarSampai.setTime(sampa);

				System.out.println(
						"mulai -> " + Common.dateFormat.get().format(mula) + " s.d " + Common.dateFormat.get().format(sampa));

				EventDateTime startA = new EventDateTime().setDateTime(new DateTime(calendarMulai.getTimeInMillis()))
						.setTimeZone(WaktuUtil.gteTimezoneName());
				event.setStart(startA);

				EventDateTime endA = new EventDateTime().setDateTime(new DateTime(calendarSampai.getTimeInMillis()))
						.setTimeZone(WaktuUtil.gteTimezoneName());
				event.setEnd(endA);

				String[] recurrence = new String[] { "RRULE:FREQ=DAILY;COUNT=1" };
				event.setRecurrence(Arrays.asList(recurrence));

				EventReminder[] reminderOverrides = new EventReminder[] {
						new EventReminder().setMethod("email").setMinutes(24 * 60),
						new EventReminder().setMethod("popup").setMinutes(30), };
				Event.Reminders reminders = new Event.Reminders().setUseDefault(false)
						.setOverrides(Arrays.asList(reminderOverrides));
				event.setReminders(reminders);

				List<EventAttendee> eventAttendee = new ArrayList<EventAttendee>();
				Set<String> emails = gelombangPendaftaran.ambilAttendee();
				for (String email : emails) {
					eventAttendee.add(new EventAttendee().setEmail(email));
				}

				event.setAttendees(eventAttendee);

				event.setVisibility("public");
				event.setAnyoneCanAddSelf(true);
				if (baru) {
					event = service.events().insert(calendarId, event).execute();
				} else {
					service.events().update(calendarId, eventId, event).execute();
				}
				String link = event.getHtmlLink();
				String pretty = event.toPrettyString();
				System.out.println("event -> " + pretty);
				gelombangPendaftaran.put(link, "html_link");
				gelombangPendaftaran.put(event.getHangoutLink(), "hangoutLink");
				gelombangPendaftaran.put(event.getId(), "calendar_id");
				events.add(event);

				String oldEventcalendar = gelombangPendaftaran.getCalendarEvent();
				gelombangPendaftaran.setCalendarEvent(pretty);
				if (!oldEventcalendar.equals(pretty)) {
					Session session = HibernateUtil.currentNativeSession();
					try {
						session.getTransaction().begin();
						session.update(gelombangPendaftaran);
						session.getTransaction().commit();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/calendar/CalendarUtil.java:1078");
					}
				}

				tbmuser.checkEmailSudahAdaApaBelum(event.getCreator().getEmail());

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			if (eventListener != null) {
				eventListener.onEvent(new org.zkoss.zk.ui.event.Event("", null, events));
			}
			label.setValue("");
		} catch (Exception e) {
			label.setValue("Error");
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public void kirimEvent(Label label, InterviewCalonMahasiswa interviewCalonMahasiswa,
			PerguruanTinggi perguruanTinggi, EventListener eventListener) {
		// List the next 10 events from the primary calendar.
		label.setValue("Mengirim data pertemuan ke kalender..");
		try {
			initService();

			List<Event> events = new ArrayList<Event>();

			String info = interviewCalonMahasiswa.getNama();

			label.setValue("Mengirim \"" + info + "\" ke kalender ...");
			String calendarId = "primary";
			try {
				String eventId = interviewCalonMahasiswa.retreive("calendar_id");

				Event event = null;
				System.out.println("eventId -> " + eventId);
				if (eventId != null && !eventId.trim().isEmpty()) {
					try {
						service.events().get(calendarId, eventId);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/calendar/CalendarUtil.java:1120");
					}
				}
				boolean baru = event == null;
				if (event == null) {
					event = new Event();
				}

				event.setSummary(interviewCalonMahasiswa.getNama())
						.setLocation((perguruanTinggi != null && (!perguruanTinggi.getAlamat1().trim().isEmpty()
								|| !perguruanTinggi.getAlamat2().trim().isEmpty())
										? (perguruanTinggi.getAlamat1() + " " + perguruanTinggi.getAlamat2() + ", ")
										: "")
								+ " atau online di " + Common.getRequestHostWithProtocol())
						.setDescription(interviewCalonMahasiswa.getKeterangan());

				String tgl = Common.dateFormat1.get().format(interviewCalonMahasiswa.getMulai());

				String mul = "07:00";
				String selesai = "18:00";

				Date mula = Common.dateFormat.get().parse(tgl + " " + mul);
				Date sampa = Common.dateFormat.get().parse(tgl + " " + selesai);

				java.util.Calendar calendarMulai = java.util.Calendar.getInstance(TimeZone.getDefault());
				calendarMulai.setTime(mula);

				java.util.Calendar calendarSampai = java.util.Calendar.getInstance(TimeZone.getDefault());
				calendarSampai.setTime(sampa);

				System.out.println(
						"mulai -> " + Common.dateFormat.get().format(mula) + " s.d " + Common.dateFormat.get().format(sampa));

				EventDateTime startA = new EventDateTime().setDateTime(new DateTime(calendarMulai.getTimeInMillis()))
						.setTimeZone(WaktuUtil.gteTimezoneName());
				event.setStart(startA);

				EventDateTime endA = new EventDateTime().setDateTime(new DateTime(calendarSampai.getTimeInMillis()))
						.setTimeZone(WaktuUtil.gteTimezoneName());
				event.setEnd(endA);

				String[] recurrence = new String[] { "RRULE:FREQ=DAILY;COUNT=1" };
				event.setRecurrence(Arrays.asList(recurrence));

				EventReminder[] reminderOverrides = new EventReminder[] {
						new EventReminder().setMethod("email").setMinutes(24 * 60),
						new EventReminder().setMethod("popup").setMinutes(30), };
				Event.Reminders reminders = new Event.Reminders().setUseDefault(false)
						.setOverrides(Arrays.asList(reminderOverrides));
				event.setReminders(reminders);

				List<EventAttendee> eventAttendee = new ArrayList<EventAttendee>();
//				Set<String> emails = interviewCalonMahasiswa.ambilAttendee();
//				for (String email : emails) {
//					eventAttendee.add(new EventAttendee().setEmail(email));
//				}

				event.setAttendees(eventAttendee);

				event.setVisibility("public");
				event.setAnyoneCanAddSelf(true);
				if (baru) {
					event = service.events().insert(calendarId, event).execute();
				} else {
					service.events().update(calendarId, eventId, event).execute();
				}
				String link = event.getHtmlLink();
				String pretty = event.toPrettyString();
				System.out.println("event -> " + pretty);
				interviewCalonMahasiswa.put(link, "html_link");
				interviewCalonMahasiswa.put(event.getHangoutLink(), "hangoutLink");
				interviewCalonMahasiswa.put(event.getId(), "calendar_id");
				events.add(event);

				String oldEventcalendar = interviewCalonMahasiswa.getCalendarEvent();
				interviewCalonMahasiswa.setCalendarEvent(pretty);
				if (!oldEventcalendar.equals(pretty)) {
					Session session = HibernateUtil.currentNativeSession();
					try {
						session.getTransaction().begin();
						session.update(interviewCalonMahasiswa);
						session.getTransaction().commit();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/calendar/CalendarUtil.java:1203");
					}
				}

				tbmuser.checkEmailSudahAdaApaBelum(event.getCreator().getEmail());

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			if (eventListener != null) {
				eventListener.onEvent(new org.zkoss.zk.ui.event.Event("", null, events));
			}
			label.setValue("");
		} catch (Exception e) {
			label.setValue("Error");
			Common.tampilErrorJikaAdmin(e);
		}
	}
}
