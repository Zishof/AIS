package ais.action.maintenance;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;

import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.core.builder.ScopeBuilder;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;

public class GoogleAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private static final String PROTECTED_RESOURCE_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

	// Kunci session utk menyimpan contextPath/base-URL ASAL user (sebelum redirect ke Google).
	// Dipakai sbg cadangan bila parameter OAuth "state" (yg jadi sumber utama) tidak terbaca
	// -- lihat resolveOriginCallbackUrl().
	private static final String SESSION_ORIGIN_CALLBACK_URL = "google_oauth_origin_callback_url";

	private OAuth20Service service;
	private Tbmuser currentUser;

	/**
	 * <p><b>Tujuan.</b> Tentukan contextPath/base-URL ASAL yang dipakai user saat MEMULAI proses
	 * login Google (sebelum diarahkan ke accounts.google.com), supaya redirect akhir setelah
	 * login sukses/gagal tetap konsisten pada contentPath yang sama -- bukan ikut contentPath
	 * milik request callback saat ini (yang bisa berbeda bila server production memetakan lebih
	 * dari satu contentPath Apache ke webapp yang sama).</p>
	 *
	 * <p><b>Sumber, berurutan dari yang paling dipercaya:</b>
	 * <ol>
	 *   <li>Parameter OAuth {@code state} -- dikirim ke Google saat inisiasi (lihat proses()),
	 *       lalu dikembalikan APA ADANYA oleh Google saat callback (mekanisme standar OAuth2,
	 *       tidak tergantung request.getContextPath() milik request callback).</li>
	 *   <li>HttpSession, sbg cadangan bila parameter state kosong/hilang (mis. browser lama atau
	 *       provider yg memangkas parameter tak dikenal).</li>
	 * </ol>
	 * </p>
	 *
	 * @return base-URL asal (mis. "https://host/contentPathAsal/main"), atau null bila tak ada
	 *         sumber yg bisa dipakai (fallback terakhir ditangani oleh pemanggil).
	 */
	private String resolveOriginCallbackUrl(Execution exec) {
		try {
			String state = exec.getParameter("state");
			if (state != null && !state.trim().isEmpty()) {
				String decoded = safeUrlDecode(state);
				if (decoded != null && !decoded.trim().isEmpty()) {
					return decoded;
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/maintenance/GoogleAction.java:resolveOriginCallbackUrl");
		}
		try {
			Object fromSession = session.getAttribute(SESSION_ORIGIN_CALLBACK_URL);
			if (fromSession != null && !fromSession.toString().trim().isEmpty()) {
				return fromSession.toString();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return null;
	}

	private String safeUrlDecode(String value) {
		if (value == null) {
			return null;
		}
		try {
			return URLDecoder.decode(value, "UTF-8");
		} catch (IllegalArgumentException e) {
			ais.common.ErrorAuditUtil.record(e,
					"oauth-state-invalid-escape src/ais/action/maintenance/GoogleAction.java:safeUrlDecode value="
							+ value);
			return value;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/maintenance/GoogleAction.java:safeUrlDecode");
			return value;
		}
	}

	/**
	 * <p><b>Tujuan.</b> Tentukan contextPath EKSTERNAL yang SEBENARNYA diketik/diklik user di
	 * browser (mis. "/idaqupyk" atau "/staidapyk"), BUKAN contextPath INTERNAL Tomcat.</p>
	 *
	 * <p><b>Kenapa perlu.</b> Konfigurasi Apache produksi melakukan REVERSE PROXY PATH-MASKING,
	 * misalnya:
	 * <pre>ProxyPass /idaqupyk ajp://38.47.178.34:8009/staidapyk</pre>
	 * Akibatnya {@code request.getContextPath()} di sisi Java SELALU mengembalikan contextPath
	 * INTERNAL ("/staidapyk"), TIDAK PEDULI path eksternal apa yang sebenarnya dipakai user
	 * ("/idaqupyk" ataupun "/staidapyk"). Ini membuat origin-tracking OAuth (lihat
	 * {@link #resolveOriginCallbackUrl(Execution)}) selalu salah menyimpan contextPath internal.</p>
	 *
	 * <p><b>Solusi.</b> Admin sudah menambahkan header {@code X-Forwarded-Prefix} di Apache per
	 * blok {@code <Location>} (mis. {@code RequestHeader set X-Forwarded-Prefix "/idaqupyk"} di
	 * blok {@code <Location /idaqupyk>}, dan {@code "/staidapyk"} di blok {@code <Location
	 * /staidapyk>}), diteruskan lewat AJP ke Tomcat. Header inilah yang berisi path eksternal
	 * asli. Kalau header tidak ada (mis. akses langsung tanpa proxy, atau environment lain yang
	 * belum diset), jatuh ke {@code request.getContextPath()} spt biasa.</p>
	 */
	private static String resolveExternalContextPath(HttpServletRequest request) {
		if (request == null) {
			return "";
		}
		try {
			String forwardedPrefix = request.getHeader("X-Forwarded-Prefix");
			if (forwardedPrefix != null && !forwardedPrefix.trim().isEmpty()) {
				return forwardedPrefix.trim();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return request.getContextPath();
	}

	/**
	 * <p><b>Tujuan.</b> Bangun base-URL (protocol://host:port/contextPath) memakai contextPath
	 * EKSTERNAL yang benar (lihat {@link #resolveExternalContextPath(HttpServletRequest)}),
	 * sbg pengganti {@code Common.getRequestHostWithProtocol()} yang menggabungkan
	 * {@code request.getContextPath()} INTERNAL apa adanya (tidak sadar path-masking proxy).</p>
	 *
	 * <p>Sengaja TIDAK mengubah {@code CommonCurrentSessionHelper.getRequestHostWithProtocol()}
	 * krn dipakai banyak komponen lain di luar alur login Google -- risiko regresi lbh besar drpd
	 * manfaatnya. Di sini cukup pakai varian {@code getRequestHostWithProtocolSimple()} (protocol
	 * + host + port SAJA, tanpa contextPath) lalu tempelkan contextPath eksternal secara terpisah.</p>
	 */
	private static String resolveExternalHostWithContextPath(HttpServletRequest request) {
		return Common.getRequestHostWithProtocolSimple(request) + resolveExternalContextPath(request);
	}

	private void redirectLoginError(String message, String callbackUrl) {
		try {
			String target = (callbackUrl != null && callbackUrl.trim().length() > 0) ? callbackUrl : ConstantValues.recapchaHome;
			String separator = target.indexOf("?") >= 0 ? "&" : "?";
			execution.sendRedirect(target + separator + "login_error="
					+ URLEncoder.encode(Common.getBahasaConfig(message), "UTF-8"));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		final String clientId = Common.getKonfigurasi("id_aplikasi_google",
				"659282761898-am1cl4kjbnupq3gjehantrfr77qmdp8u.apps.googleusercontent.com").getNilai();
		final String clientSecret = Common.getKonfigurasi("kunci_rahasia_aplikasi_google", "wSJ8-Sb4rx3LseH0k8HIiUqr")
				.getNilai();
		// Pakai contextPath EKSTERNAL (sadar proxy path-masking X-Forwarded-Prefix), BUKAN
		// Common.getRequestHostWithProtocol() yg menempel request.getContextPath() INTERNAL apa
		// adanya -- lihat resolveExternalContextPath(HttpServletRequest).
		HttpServletRequest requestSaatInisiasi = null;
		try {
			org.zkoss.zk.ui.Execution execSaatInisiasi = Executions.getCurrent();
			if (execSaatInisiasi != null) {
				requestSaatInisiasi = (HttpServletRequest) execSaatInisiasi.getNativeRequest();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		service = new ServiceBuilder(clientId).apiSecret(clientSecret)
				.defaultScope(new ScopeBuilder("profile", "email"))
				.callback(resolveExternalHostWithContextPath(requestSaatInisiasi) + "/google.zul")
				.build(GoogleApi20.instance());
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		currentUser = Common.getCurrentUser();

		if (currentUser != null) {
			if (execution.getParameter("tambah_akun") != null && execution.getParameter("tambah_akun").equals("true")
					&& execution.getParameter("id") != null && !execution.getParameter("id").trim().isEmpty()) {
				proses();
			} else if (session.getAttribute("tambah_akun") != null && session.getAttribute("tambah_akun").equals("true")
					&& session.getAttribute("id") != null && !session.getAttribute("id").toString().trim().isEmpty()) {
				session.removeAttribute("tambah_akun");
				session.removeAttribute("id");
				proses();
			} else {
				execution.sendRedirect("main");
			}
			return;
		} else {
			proses();
		}

	}

	public void proses() throws Exception {
		// Request native dipakai utk resolveExternalContextPath() -- sadar header
		// X-Forwarded-Prefix yg diteruskan Apache lwt AJP, shg contextPath yg dipakai utk
		// origin-tracking adalah path EKSTERNAL asli (idaqupyk/staidapyk), bukan contextPath
		// INTERNAL Tomcat hasil proxy path-masking.
		HttpServletRequest request = null;
		try {
			if (execution != null) {
				request = (HttpServletRequest) execution.getNativeRequest();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		if (execution.getParameter("code") == null && execution.getParameter("state") == null) {
			session.setAttribute("tambah_akun", execution.getParameter("tambah_akun"));
			session.setAttribute("id", execution.getParameter("id"));

			// Tangkap contextPath/base-URL ASAL user SEBELUM diarahkan ke Google. Ini titik
			// SATU-SATUNYA yg bisa dipercaya merepresentasikan contentPath yg sedang dipakai
			// user (mis. jika Apache memetakan >1 contentPath ke webapp yg sama, request
			// callback dari Google bisa saja mendarat di contentPath yg berbeda).
			String originCallbackUrl = execution.getParameter("callback_url");
			if (originCallbackUrl == null || originCallbackUrl.trim().isEmpty()) {
				originCallbackUrl = resolveExternalHostWithContextPath(request) + "/main";
			}
			session.setAttribute(SESSION_ORIGIN_CALLBACK_URL, originCallbackUrl);

			Map<String, String> additionalParams = new HashMap<String, String>();
			additionalParams.put("approval_prompt", "auto");
			additionalParams.put("access_type", "online");
			// Titipkan contentPath asal via parameter "state" OAuth standar. Google akan
			// mengembalikan nilai ini APA ADANYA saat callback (lihat resolveOriginCallbackUrl()),
			// sehingga penentuan contentPath tujuan akhir TIDAK tergantung pada
			// request.getContextPath() milik request callback itu sendiri.
			additionalParams.put("state", URLEncoder.encode(originCallbackUrl, "UTF-8"));
			String authorizationUrl = service.getAuthorizationUrl(additionalParams);
			System.out.println("Got the Authorization URL!");
			System.out.println("Now go and authorize ScribeJava here:");
			System.out.println(authorizationUrl);
			Execution exec = Executions.getCurrent();
			exec.sendRedirect(authorizationUrl);
		} else if (execution.getParameter("error") != null && !execution.getParameter("error").trim().isEmpty()) {
			String originCallbackUrl = resolveOriginCallbackUrl(execution);
			redirectLoginError("Login Google dibatalkan atau tidak diizinkan. Silakan coba kembali.",
					(originCallbackUrl != null && !originCallbackUrl.trim().isEmpty()) ? originCallbackUrl
							: execution.getParameter("callback_url"));
		} else if (execution.getParameter("code") != null && !execution.getParameter("code").trim().isEmpty()
				&& execution.getParameter("scope") != null && !execution.getParameter("scope").trim().isEmpty()) {
			final String code = execution.getParameter("code").trim();
			// Utamakan contentPath ASAL (dari state OAuth / session) drpd parameter
			// "callback_url" mentah pd request callback ini -- lihat resolveOriginCallbackUrl().
			String originCallbackUrl = resolveOriginCallbackUrl(execution);
			final String callback_url = (originCallbackUrl != null && !originCallbackUrl.trim().isEmpty())
					? originCallbackUrl : execution.getParameter("callback_url");

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					try {

						// Trade the Request Token and Verfier for the Access
						// Token
						System.out.println("Trading the Request Token for an Access Token...");
						OAuth2AccessToken accessToken = null;
						try {
							accessToken = service.getAccessToken(code);
						} catch (Exception tokenError) {
							System.out.println("Google OAuth token ditolak: " + tokenError.getMessage());
							redirectLoginError(
									"Sesi login Google tidak valid atau sudah kadaluarsa. Silakan login kembali.",
									callback_url);
							return;
						}
						System.out.println("Got the Access Token!");
						System.out.println("(if your curious it looks like this: " + accessToken + ", 'rawResponse'='"
								+ accessToken.getRawResponse() + "')");

						OAuthRequest request = new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URL);
						service.signRequest(accessToken, request);
						Response response = service.execute(request);

//						System.out.println("response.getHeaders() => " + response.getHeaders());

						String responseBody = response.getBody();
						JSONObject jsonObject = new JSONObject(responseBody);
						System.out.println("responseBody => " + jsonObject);

						String linkProfile = jsonObject.isNull("url") ? "" : jsonObject.getString("url");

						if (Common.bolehKonfigurasi("login_menggunakan_email_terdaftar", Konfigurasi.TIDAK_AKTIF)) {

							String emails = "";
							try {
								if (!jsonObject.isNull("emails")) {
									emails = jsonObject.getJSONArray("emails").getJSONObject(0).getString("value");
								}
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							String emailAddress = emails;

							if (!jsonObject.isNull("email")) {
								emailAddress = jsonObject.getString("email");
							}

							Session session = HibernateUtil.currentSession();
							if (emailAddress != null && !emailAddress.isEmpty()
									&& Common.isValidEmailAddress(emailAddress)) {
//								Tbmuser tbmuser = (Tbmuser) ConstantValues.simpleObject(session
//										.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
//										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
//										.createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
//										.createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN)
//										.createAlias("guru", "guru", Criteria.LEFT_JOIN)
//										.add(Restrictions.or(
//												Restrictions.ilike("email", emailAddress, MatchMode.ANYWHERE),
//												Restrictions.or(
//														Restrictions.ilike("guru.alamatEmail", emailAddress,
//																MatchMode.ANYWHERE),
//														Restrictions.or(
//																Restrictions.ilike("pegawai.email", emailAddress,
//																		MatchMode.ANYWHERE),
//																Restrictions.ilike("dosen.email", emailAddress,
//																		MatchMode.ANYWHERE)))))
//										.setMaxResults(1), Tbmuser.class);

								Tbmuser tbmuser = Tbmuser.ambilBerdasarEmail(emailAddress);

								if (tbmuser != null) {
									Common.doLogin(tbmuser, linkProfile, callback_url);
									return;
								} else {

									Mahasiswa mahasiswa = (Mahasiswa) ConstantValues
											.simpleObject(
													session.createCriteria(Mahasiswa.class)
															.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
															.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
															.add(Restrictions.ilike("email", emailAddress,
																	MatchMode.ANYWHERE))
															.setMaxResults(1),
													Mahasiswa.class);
									if (mahasiswa != null) {
										Common.doLogin(mahasiswa, linkProfile, callback_url);
										return;
									} else {
										Siswa siswa = (Siswa) ConstantValues
												.simpleObject(
														session.createCriteria(Siswa.class)
																.add(Restrictions.isNotNull("namaSiswa"))
																.add(Restrictions.ne("namaSiswa", ""))
																.add(Restrictions.isNotNull("sekolah"))
																.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																.add(Restrictions.ilike("alamatEmail", emailAddress,
																		MatchMode.ANYWHERE))
																.setMaxResults(1),
														Siswa.class);
										if (siswa != null) {
											Common.doLogin(siswa, linkProfile, callback_url);
											return;
										}
									}

								}

							}

							try {

								String error = Common
										.getBahasaConfig("Email Anda belum terdaftar atau tidak ditemukan");

								execution.sendRedirect(ConstantValues.recapchaHome + "?login_error="
										+ URLEncoder.encode(error + (emailAddress == null || emailAddress.isEmpty() ? ""
												: " (" + emailAddress + ")"), "UTF-8"));

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						} else {

							String googleId = jsonObject.isNull("id") ? null : jsonObject.get("id").toString();
							if (!jsonObject.isNull("sub")) {
								googleId = jsonObject.get("sub").toString();
							}
							System.out.println("googleId => " + googleId);

							if (googleId == null || googleId.trim().isEmpty()) {
								redirectLoginError(
										"Proses login menggunakan akun Google tidak dapat diselesaikan karena "
												+ "sistem Google tidak mengembalikan data identitas akun (ID akun kosong). "
												+ "Kemungkinan penyebabnya adalah izin akses (permission) yang belum "
												+ "sepenuhnya disetujui oleh Bapak/Ibu pada saat proses otorisasi, atau "
												+ "terjadi gangguan sementara pada layanan Google. Silakan coba login "
												+ "kembali dan pastikan Bapak/Ibu menyetujui seluruh izin akses yang "
												+ "diminta; apabila kendala berulang, silakan login memakai username/kata "
												+ "sandi biasa, atau hubungi Administrator Sistem.",
										callback_url);
							} else {

								String picture = "";
								try {
									picture = jsonObject.isNull("image") ? ""
											: jsonObject.getJSONObject("image").getString("url");

								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
								String pictureUrl = picture;
								String emails = "";
								try {
									if (!jsonObject.isNull("emails")) {
										emails = jsonObject.getJSONArray("emails").getJSONObject(0).getString("value");
									}
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
								String emailAddress = emails;

								if (!jsonObject.isNull("email")) {
									emailAddress = jsonObject.getString("email");
								}

								String formattedName = jsonObject.isNull("name") ? "" : jsonObject.getString("name");

								Common.lanjutProcessSocialMedia(currentUser, "googleid", googleId, "googleId",
										linkProfile, pictureUrl, emailAddress, formattedName, "/img/sign_in_google.png",
										callback_url);
							}
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						redirectLoginError(
								"Proses login menggunakan akun Google tidak dapat diselesaikan. Kemungkinan penyebabnya "
										+ "adalah gangguan sementara pada layanan Google, sesi otorisasi yang sudah "
										+ "kadaluarsa, atau data akun Google yang dikembalikan tidak lengkap. "
										+ "Keterangan teknis dari sistem: \"" + e.getMessage() + "\". Silakan coba login "
										+ "kembali beberapa saat lagi; apabila kendala berulang, silakan login memakai "
										+ "username/kata sandi biasa, atau hubungi Administrator Sistem.",
								callback_url);
					}
				}
			});

		}
	}

}
