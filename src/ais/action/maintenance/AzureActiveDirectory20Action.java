package ais.action.maintenance;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

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

import com.github.scribejava.apis.MicrosoftAzureActiveDirectory20Api;
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

public class AzureActiveDirectory20Action extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

//	private static final String NETWORK_NAME = "Microsoft Azure Active Directory";
	private static final String PROTECTED_RESOURCE_URL = "https://graph.microsoft.com/v1.0/me";

	private OAuth20Service service;
	private Tbmuser currentUser;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		String clientId = Common.getKonfigurasi("id_aplikasi_azure", "id_aplikasi_azure").getNilai();
		String clientSecret = Common.getKonfigurasi("kunci_rahasia_aplikasi_azure", "kunci_rahasia_aplikasi_azure")
				.getNilai();
		service = new ServiceBuilder(clientId).apiSecret(clientSecret).defaultScope("openid User.Read")
				.callback(Common.getRequestHostWithProtocol().replaceAll("http:", "https:") + "/azure.zul")
				.build(MicrosoftAzureActiveDirectory20Api.instance());
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
		if (execution.getParameter("code") == null) {
			session.setAttribute("tambah_akun", execution.getParameter("tambah_akun"));
			session.setAttribute("id", execution.getParameter("id"));
			Map<String, String> additionalParams = new HashMap<String, String>();
			additionalParams.put("approval_prompt", "auto");
			additionalParams.put("access_type", "online");
			String authorizationUrl = service.getAuthorizationUrl(additionalParams);
			System.out.println("Got the Authorization URL!");
			System.out.println("Now go and authorize ScribeJava here:");
			System.out.println(authorizationUrl);
			Execution exec = Executions.getCurrent();
			exec.sendRedirect(authorizationUrl);
		} else if (execution.getParameter("code") != null && !execution.getParameter("code").trim().isEmpty()) {
			final String code = execution.getParameter("code");
			final String callback_url = null;

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					try {

						OAuth2AccessToken accessToken = service.getAccessToken(code);

						OAuthRequest request = new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URL);
						service.signRequest(accessToken, request);
						Response response = service.execute(request);
						
						

						String responseBody = response.getBody();
						JSONObject jsonObject = new JSONObject(responseBody);
						System.out.println("responseBody => " + jsonObject);
						
						

						String linkProfile = jsonObject.isNull("url") ? "" : jsonObject.getString("url");

						if (Common.bolehKonfigurasi("login_menggunakan_email_terdaftar", Konfigurasi.TIDAK_AKTIF)) {
							String value = jsonObject.isNull("mail") ? null : jsonObject.getString("mail");

							if (value != null && !value.isEmpty() && Common.isValidEmailAddress(value)) {
								Session session = HibernateUtil.currentSession();
								Tbmuser tbmuser = (Tbmuser) ConstantValues.simpleObject(session
										.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.add(Restrictions.ilike("email", value, MatchMode.ANYWHERE)).setMaxResults(1),
										Tbmuser.class);

								if (tbmuser != null) {
									Common.doLogin(tbmuser, linkProfile, callback_url);
									return;
								} else {

									Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.simpleObject(
											session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
													.add(Restrictions.ilike("email", value, MatchMode.ANYWHERE))
													.setMaxResults(1),
											Mahasiswa.class);
									if (mahasiswa != null) {
										Common.doLogin(mahasiswa, linkProfile, callback_url);
										return;
									} else {
										Siswa siswa = (Siswa) ConstantValues
												.simpleObject(
														session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
																.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
																.add(Restrictions.ilike("alamatEmail", value,
																		MatchMode.ANYWHERE))
																.setMaxResults(1),
														Siswa.class);
										if (siswa != null) {
											Common.doLogin(siswa, linkProfile, callback_url);
											return;
										}
									}

								}

								try {

									String error = Common
											.getBahasaConfig("Email Anda belum terdaftar atau tidak ditemukan");

									execution.sendRedirect(ConstantValues.recapchaHome + "?login_error="
											+ URLEncoder.encode(error
													+ (value == null || value.isEmpty() ? "" : " (" + value + ")"),
													"UTF-8"));

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}

							} else {

								try {

									String error = Common
											.getBahasaConfig("Email Anda belum terdaftar atau tidak ditemukan");

									execution.sendRedirect(ConstantValues.recapchaHome + "?login_error="
											+ URLEncoder.encode(error
													+ (value == null || value.isEmpty() ? "" : " (" + value + ")"),
													"UTF-8"));

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
							}
						} else {

							String azureId = jsonObject.isNull("id") ? null : jsonObject.get("id").toString();
//							System.out.println("azureId => " + azureId);

							if (azureId == null || azureId.trim().isEmpty()) {
								try {
									String pesan = "Proses login menggunakan akun Microsoft/Azure Active Directory "
											+ "tidak dapat diselesaikan karena sistem Microsoft/Azure tidak "
											+ "mengembalikan data identitas akun (ID akun kosong). Kemungkinan "
											+ "penyebabnya adalah izin akses (permission) yang belum sepenuhnya "
											+ "disetujui oleh Bapak/Ibu pada saat proses otorisasi, atau terjadi "
											+ "gangguan sementara pada layanan Microsoft/Azure. Silakan coba login "
											+ "kembali dan pastikan Bapak/Ibu menyetujui seluruh izin akses yang "
											+ "diminta; apabila kendala berulang, silakan login memakai username/kata "
											+ "sandi biasa, atau hubungi Administrator Sistem.";
									execution.sendRedirect(ConstantValues.recapchaHome + "?login_error="
											+ URLEncoder.encode(Common.getBahasaConfig(pesan), "UTF-8"));
								} catch (Exception ex) {
									Common.tampilErrorJikaAdmin(ex);
									execution.sendRedirect(Common.getRequestHostWithProtocol());
								}
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
									emails = jsonObject.getJSONArray("emails").getJSONObject(0).getString("value");
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
								String emailAddress = emails;
								String formattedName = jsonObject.isNull("displayName") ? ""
										: jsonObject.getString("displayName");

								Common.lanjutProcessSocialMedia(currentUser, "azureid", azureId, "azureId", linkProfile,
										pictureUrl, emailAddress, formattedName, "/img/sign_in_azure.png",
										callback_url);
							}
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						try {
							String pesan = "Proses login menggunakan akun Microsoft/Azure Active Directory tidak dapat "
									+ "diselesaikan. Kemungkinan penyebabnya adalah gangguan sementara pada layanan "
									+ "Microsoft/Azure, sesi otorisasi yang sudah kadaluarsa, atau data akun yang "
									+ "dikembalikan tidak lengkap. Keterangan teknis dari sistem: \"" + e.getMessage()
									+ "\". Silakan coba login kembali beberapa saat lagi; apabila kendala berulang, "
									+ "silakan login memakai username/kata sandi biasa, atau hubungi Administrator Sistem.";
							execution.sendRedirect(ConstantValues.recapchaHome + "?login_error="
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
