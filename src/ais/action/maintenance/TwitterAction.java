package ais.action.maintenance;

import java.net.URLEncoder;

import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;

import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;

import ais.common.Common;
import ais.database.model.Tbmuser;

/**
 * Controller/action ZK untuk twitter. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String PROTECTED_RESOURCE_URL}, {@code
 * OAuth10aService service}, {@code Tbmuser currentUser}; inisialisasi/lifecycle ({@code doBeforeCompose()},
 * {@code doAfterCompose()}); mutasi data ({@code proses()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class TwitterAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private static final String PROTECTED_RESOURCE_URL = "https://api.twitter.com/1.1/account/verify_credentials.json";

	private OAuth10aService service;
	private Tbmuser currentUser;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		final String clientId = Common.getKonfigurasi("id_aplikasi_twitter", "31JDGYztaFvIXDuZyhpfSIh80").getNilai();
		final String clientSecret = Common
				.getKonfigurasi("kunci_rahasia_aplikasi_twitter", "mqPwaSByVkda3OvTCZckpi2tbUu7olkp7eCQOGpkCD07cWJEoD")
				.getNilai();
		service = new ServiceBuilder(clientId).apiSecret(clientSecret)
				.callback(Common.getRequestHostWithProtocol() + "/twitter.zul").build(TwitterApi.instance());
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
		if (execution.getParameter("oauth_token") == null && execution.getParameter("oauth_verifier") == null) {

			session.setAttribute("tambah_akun", execution.getParameter("tambah_akun"));
			session.setAttribute("id", execution.getParameter("id"));
			// Obtain the Request Token
			System.out.println("Fetching the Request Token...");
			final OAuth1RequestToken requestToken = service.getRequestToken();
			System.out.println("Got the Request Token!");

			String url = service.getAuthorizationUrl(requestToken);

			System.out.println("Now go and authorize ScribeJava here:");
			System.out.println("url => " + url);

			Execution exec = Executions.getCurrent();
			exec.sendRedirect(url);
		} else {

			final String oauthToken = execution.getParameter("oauth_token");
			final String oauthVerifier = execution.getParameter("oauth_verifier");
			final String callback_url = execution.getParameter("callback_url");

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					try {

						System.out.println("Trading the Request Token for an Access Token...");

						OAuth1RequestToken requestToken = new OAuth1RequestToken(oauthToken, oauthVerifier);

						final OAuth1AccessToken accessToken = service.getAccessToken(requestToken, oauthVerifier);
						System.out.println("Got the Access Token!");
						System.out.println("(if your curious it looks like this: " + (accessToken == null ? "(null)" : "(disamarkan)") + ", 'rawResponse'='"
								+ "(disamarkan)" + "')");

						// Now let's go and ask for a protected
						// resource!
						System.out.println("Now we're going to access a protected resource...");
						final OAuthRequest request = new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URL);
						service.signRequest(accessToken, request);
						final Response response = service.execute(request);
						System.out.println("Got it! Lets see what we found...");

						String responseBody = response.getBody();
						JSONObject jsonObject = new JSONObject(responseBody);
						System.out.println("responseBody => " + jsonObject);

						final String twitterId = jsonObject.get("id").toString();
						System.out.println("twitterId => " + twitterId);

						if (twitterId == null || twitterId.trim().isEmpty()) {
							try {
								String target = callback_url == null ? Common.getRequestHostWithProtocol() : callback_url;
								String separator = target.indexOf("?") >= 0 ? "&" : "?";
								String pesan = "Proses login menggunakan akun Twitter/X tidak dapat diselesaikan "
										+ "karena sistem Twitter/X tidak mengembalikan data identitas akun (ID akun "
										+ "kosong). Kemungkinan penyebabnya adalah izin akses (permission) yang belum "
										+ "sepenuhnya disetujui oleh Bapak/Ibu pada saat proses otorisasi, atau "
										+ "terjadi gangguan sementara pada layanan Twitter/X. Silakan coba login "
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

							final String linkProfile = "https://twitter.com/"
									+ (jsonObject.isNull("screen_name") ? "" : jsonObject.getString("screen_name"));
							final String pictureUrl = jsonObject.isNull("profile_image_url") ? ""
									: jsonObject.getString("profile_image_url");
							final String emailAddress = jsonObject.isNull("screen_name") ? ""
									: "@" + jsonObject.getString("screen_name");
							final String formattedName = jsonObject.isNull("name") ? "" : jsonObject.getString("name");

							Common.lanjutProcessSocialMedia(currentUser, "twitterid", twitterId, "twitterId",
									linkProfile, pictureUrl, emailAddress, formattedName, "/img/twitter.png",
									callback_url);

						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						try {
							String target = callback_url == null ? Common.getRequestHostWithProtocol() : callback_url;
							String separator = target.indexOf("?") >= 0 ? "&" : "?";
							String pesan = "Proses login menggunakan akun Twitter tidak dapat diselesaikan. "
									+ "Kemungkinan penyebabnya adalah gangguan sementara pada layanan Twitter, sesi "
									+ "otorisasi yang sudah kadaluarsa, atau data akun Twitter yang dikembalikan tidak "
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
