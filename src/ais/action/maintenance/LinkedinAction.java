package ais.action.maintenance;

import java.net.URLEncoder;

import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;

import com.github.scribejava.apis.LinkedInApi20;
import com.github.scribejava.core.builder.ScopeBuilder;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import ais.common.Common;
import ais.database.model.Tbmuser;

public class LinkedinAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private OAuth20Service service;
	private Tbmuser currentUser;
	private static final String PROTECTED_EMAIL_RESOURCE_URL = "https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))";

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		final String clientId = Common.getKonfigurasi("id_aplikasi_linkedin", "75l33zqsb4wucn").getNilai();
		final String clientSecret = Common.getKonfigurasi("kunci_rahasia_aplikasi_linkedin", "O2qbKAgQ9haUlgzd")
				.getNilai();
		service = new ServiceBuilder(clientId).apiSecret(clientSecret)
				.defaultScope(new ScopeBuilder("r_liteprofile", "r_emailaddress"))
				.callback(Common.getRequestHostWithProtocol() + "/linkedin.zul").build(LinkedInApi20.instance());
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
		if (execution.getParameter("code") == null || execution.getParameter("code").trim().isEmpty()) {
			session.setAttribute("tambah_akun", execution.getParameter("tambah_akun"));
			session.setAttribute("id", execution.getParameter("id"));
			System.out.println("Fetching the Authorization URL...");
			final String authorizationUrl = service.getAuthorizationUrl();
			System.out.println("Got the Authorization URL!");
			System.out.println("Now go and authorize ScribeJava here:");
			System.out.println(authorizationUrl);

			Execution exec = Executions.getCurrent();
			exec.sendRedirect(authorizationUrl);
		} else {
			final String code = execution.getParameter("code");
			final String callback_url = execution.getParameter("callback_url");
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					try {

						// Trade the Request Token and Verfier for the Access
						// Token
						System.out.println("Trading the Request Token for an Access Token...");
						final OAuth2AccessToken accessToken = service.getAccessToken(code);
						System.out.println("Got the Access Token!");
						System.out.println("(if your curious it looks like this: " + accessToken + ", 'rawResponse'='"
								+ accessToken.getRawResponse() + "')");

						final OAuthRequest request = new OAuthRequest(Verb.GET, PROTECTED_EMAIL_RESOURCE_URL);
						request.addHeader("x-li-format", "json");
						request.addHeader("Accept-Language", "ru-RU");
						service.signRequest(accessToken, request);
						final Response response = service.execute(request);

						String responseBody = response.getBody();
						JSONObject jsonObject = new JSONObject(responseBody);
						System.out.println("responseBody => " + jsonObject);

						final String linkedinId = jsonObject.isNull("id") ? "" : jsonObject.get("id").toString();

						if (linkedinId == null || linkedinId.trim().isEmpty()) {
							try {
								String target = callback_url == null ? Common.getRequestHostWithProtocol() : callback_url;
								String separator = target.indexOf("?") >= 0 ? "&" : "?";
								String pesan = "Proses login menggunakan akun LinkedIn tidak dapat diselesaikan "
										+ "karena sistem LinkedIn tidak mengembalikan data identitas akun (ID akun "
										+ "kosong). Kemungkinan penyebabnya adalah izin akses (permission) yang belum "
										+ "sepenuhnya disetujui oleh Bapak/Ibu pada saat proses otorisasi, atau "
										+ "terjadi gangguan sementara pada layanan LinkedIn. Silakan coba login "
										+ "kembali dan pastikan Bapak/Ibu menyetujui seluruh izin akses yang "
										+ "diminta; apabila kendala berulang, silakan login memakai username/kata "
										+ "sandi biasa, atau hubungi Administrator Sistem.";
								execution.sendRedirect(target + separator + "login_error="
										+ URLEncoder.encode(Common.getBahasaConfig(pesan), "UTF-8"));
							} catch (Exception ex) {
								Common.tampilErrorJikaAdmin(ex);
								execution.sendRedirect(Common.getRequestHostWithProtocol());
							}
						} else {
							final String linkProfile = jsonObject.isNull("publicProfileUrl") ? ""
									: jsonObject.getString("publicProfileUrl");
							final String pictureUrl = jsonObject.isNull("pictureUrl") ? ""
									: jsonObject.getString("pictureUrl");
							final String emailAddress = jsonObject.isNull("emailAddress") ? ""
									: jsonObject.getString("emailAddress");
							final String formattedName = jsonObject.isNull("formattedName") ? ""
									: jsonObject.getString("formattedName");

							Common.lanjutProcessSocialMedia(currentUser, "linkedinid", linkedinId, "linkedinId",
									linkProfile, pictureUrl, emailAddress, formattedName, "/img/linkedin.png",
									callback_url);
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						try {
							String target = callback_url == null ? Common.getRequestHostWithProtocol() : callback_url;
							String separator = target.indexOf("?") >= 0 ? "&" : "?";
							String pesan = "Proses login menggunakan akun LinkedIn tidak dapat diselesaikan. "
									+ "Kemungkinan penyebabnya adalah gangguan sementara pada layanan LinkedIn, sesi "
									+ "otorisasi yang sudah kadaluarsa, atau data akun LinkedIn yang dikembalikan tidak "
									+ "lengkap. Keterangan teknis dari sistem: \"" + e.getMessage() + "\". Silakan coba "
									+ "login kembali beberapa saat lagi; apabila kendala berulang, silakan login memakai "
									+ "username/kata sandi biasa, atau hubungi Administrator Sistem.";
							execution.sendRedirect(target + separator + "login_error="
									+ URLEncoder.encode(Common.getBahasaConfig(pesan), "UTF-8"));
						} catch (Exception ex) {
							Common.tampilErrorJikaAdmin(ex);
							execution.sendRedirect(Common.getRequestHostWithProtocol());
						}
					}
				}
			});

		}

	}
}
