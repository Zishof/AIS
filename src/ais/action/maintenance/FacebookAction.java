package ais.action.maintenance;

import java.net.URLEncoder;

import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;

import com.github.scribejava.apis.FacebookApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import ais.common.Common;
import ais.database.model.Tbmuser;

/**
 * Controller/action ZK untuk facebook. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String PROTECTED_RESOURCE_URL}, {@code
 * OAuth20Service service}, {@code Tbmuser currentUser}; inisialisasi/lifecycle ({@code doBeforeCompose()},
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
public class FacebookAction extends GenericAutowireComposer {

	/**
	 *  
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private static final String PROTECTED_RESOURCE_URL = "https://graph.facebook.com/v2.7/me?fields=id,name,email,picture,link,accounts";

	private OAuth20Service service;
	private Tbmuser currentUser;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {

		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		final String callback_url = execution.getParameter("callback_url") == null
				? Common.getRequestHostWithProtocol() + "/facebook.zul"
				: execution.getParameter("callback_url");
		if (execution.getParameter("callback_url") != null
				&& !execution.getParameter("callback_url").trim().isEmpty()) {
			session.setAttribute("callback_url", callback_url);
		} else {
			session.setAttribute("callback_url", null);
		}
		final String clientId = Common.getKonfigurasi("id_aplikasi_facebook", "1134829156583318").getNilai();
		final String clientSecret = Common
				.getKonfigurasi("kunci_rahasia_aplikasi_facebook", "454cfff40912a1a306bcf8edf36d0790").getNilai();

		service = new ServiceBuilder(clientId).apiSecret(clientSecret).callback(callback_url)
				.build(FacebookApi.instance());

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
		if (execution.getParameter("code") == null && execution.getParameter("state") == null) {
			session.setAttribute("tambah_akun", execution.getParameter("tambah_akun"));
			session.setAttribute("id", execution.getParameter("id"));
			final String authorizationUrl = service.getAuthorizationUrl();
			System.out.println("authorizationUrl = " + authorizationUrl);
			Execution exec = Executions.getCurrent();
			exec.sendRedirect(authorizationUrl);
		} else if (execution.getParameter("code") != null && !execution.getParameter("code").trim().isEmpty()
				&& execution.getParameter("state") != null && !execution.getParameter("state").trim().isEmpty()) {
			final String code = execution.getParameter("code").trim();
			final String callback_url = execution.getParameter("callback_url");

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					try {

						final OAuth2AccessToken accessToken = service.getAccessToken(code);
						System.out.println("Got the Access Token!");
						System.out.println("(if your curious it looks like this: " + (accessToken == null ? "(null)" : "(disamarkan)") + ", 'rawResponse'='"
								+ "(disamarkan)" + "')");

						OAuthRequest request = new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URL);
						service.signRequest(accessToken, request);
						Response response = service.execute(request);

						System.out.println("Got it! Lets see what we found...");

						System.out.println(response.getCode());

						String responseBody = response.getBody();
						JSONObject jsonObject = new JSONObject(responseBody);
						System.out.println("responseBody => " + jsonObject);

						final String facebookId = jsonObject.get("id").toString();
						System.out.println("facebookId => " + facebookId);

						if (facebookId == null || facebookId.trim().isEmpty()) {
							try {
								String target = callback_url == null ? Common.getRequestHostWithProtocol() : callback_url;
								String separator = target.indexOf("?") >= 0 ? "&" : "?";
								String pesan = "Proses login menggunakan akun Facebook tidak dapat diselesaikan "
										+ "karena sistem Facebook tidak mengembalikan data identitas akun (ID akun "
										+ "kosong). Kemungkinan penyebabnya adalah izin akses (permission) yang belum "
										+ "sepenuhnya disetujui oleh Bapak/Ibu pada saat proses otorisasi, atau "
										+ "terjadi gangguan sementara pada layanan Facebook. Silakan coba login "
										+ "kembali dan pastikan Bapak/Ibu menyetujui seluruh izin akses yang "
										+ "diminta; apabila kendala berulang, silakan login memakai username/kata "
										+ "sandi biasa, atau hubungi Administrator Sistem.";
								execution.sendRedirect(target + separator + "login_error="
										+ URLEncoder.encode(Common.getBahasaConfig(pesan), "UTF-8"));
							} catch (Exception ex) {
								Common.tampilErrorJikaAdmin(ex);
								execution.sendRedirect(
										callback_url == null ? Common.getRequestHostWithProtocol() : callback_url);
							}
						} else {

							final String linkProfile = jsonObject.isNull("link") ? "" : jsonObject.getString("link");
							String picture = "";
							try {
								picture = jsonObject.isNull("picture") ? ""
										: jsonObject.getJSONObject("picture").getJSONObject("data").getString("url");

							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							final String pictureUrl = picture;
							final String emailAddress = jsonObject.isNull("email") ? "" : jsonObject.getString("email");
							final String formattedName = jsonObject.isNull("name") ? "" : jsonObject.getString("name");

							Common.lanjutProcessSocialMedia(currentUser, "facebookid", facebookId, "facebookId",
									linkProfile, pictureUrl, emailAddress, formattedName, "/img/fblogin.png",
									callback_url);

						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						try {
							String target = callback_url == null ? Common.getRequestHostWithProtocol() : callback_url;
							String separator = target.indexOf("?") >= 0 ? "&" : "?";
							String pesan = "Proses login menggunakan akun Facebook tidak dapat diselesaikan. "
									+ "Kemungkinan penyebabnya adalah gangguan sementara pada layanan Facebook, sesi "
									+ "otorisasi yang sudah kadaluarsa, atau data akun Facebook yang dikembalikan tidak "
									+ "lengkap. Keterangan teknis dari sistem: \"" + e.getMessage() + "\". Silakan coba "
									+ "login kembali beberapa saat lagi; apabila kendala berulang, silakan login memakai "
									+ "username/kata sandi biasa, atau hubungi Administrator Sistem.";
							execution.sendRedirect(target + separator + "login_error="
									+ URLEncoder.encode(Common.getBahasaConfig(pesan), "UTF-8"));
						} catch (Exception ex) {
							Common.tampilErrorJikaAdmin(ex);
							execution.sendRedirect(
									callback_url == null ? Common.getRequestHostWithProtocol() : callback_url);
						}
					}

				}
			});

		}
	}

}
