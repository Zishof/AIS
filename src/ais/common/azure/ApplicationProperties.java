package ais.common.azure;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import ais.common.Common;
import ais.database.model.Konfigurasi;

/**
 * Kumpulan getter statis untuk parameter integrasi <b>Microsoft Azure AD / SharePoint (Microsoft
 * Graph API)</b> yang dipakai modul {@code ais.common.azure} untuk otentikasi OAuth ke SharePoint
 * Online. Setiap method membaca satu nilai konfigurasi lewat
 * {@link Common#getKonfigurasi(String, String)} — yang berarti nilai sesungguhnya SEHARUSNYA
 * berasal dari tabel konfigurasi runtime AIS ({@link ais.database.model.Konfigurasi}, dapat diatur
 * per instalasi/tenant tanpa deploy ulang) — namun setiap pemanggilan {@code getKonfigurasi}
 * di kelas ini disertai <b>nilai default</b> yang dipakai apabila kunci konfigurasi terkait belum
 * pernah diisi di database.
 *
 * <h2>Riwayat keamanan (DIPERBAIKI 2026-09-02) — kredensial Azure AD sebelumnya tertanam sebagai
 * nilai default</h2>
 * <p>
 * Nilai default yang sebelumnya ditanam di kode ini BUKAN sekadar placeholder kosong seperti
 * biasa: tiga di antaranya berbentuk seperti kredensial nyata yang berfungsi (bukan contoh
 * generik), yaitu:
 * </p>
 * <ul>
 * <li>{@link #getClientId()} — default lama {@code "2ba2456b-5877-42b3-a15c-be27d98798b2"},
 * berformat GUID persis seperti Application (client) ID Azure AD App Registration sungguhan.</li>
 * <li>{@link #getClientSecret()} — default lama {@code "BMLz8TNsbVVl1sdwgIUY7GUO3Yu@z:.:"},
 * berbentuk string acak bercampur simbol khas nilai <i>client secret</i> Azure AD, BUKAN teks
 * penjelas seperti {@code "isi secret di sini"}.</li>
 * <li>{@link #getTenantId()} — default lama {@code "cc1522dd-6b7f-653f-8546-2228663419d6"}, juga
 * berformat GUID khas Directory (tenant) ID Azure AD.</li>
 * </ul>
 * <p>
 * Ketiganya bersama-sama adalah triplet kredensial OAuth2 client-credentials Azure AD yang lazim
 * dipakai untuk memperoleh access token Microsoft Graph tanpa interaksi pengguna — bila ketiganya
 * valid dan masih aktif di sisi Azure, siapa pun yang membaca kode sumber ini (termasuk lewat
 * riwayat kontrol versi) berpotensi memperoleh akses ke resource SharePoint/Graph yang diizinkan
 * App Registration tersebut. Ketiga default itu sudah dihapus (kini string kosong); instalasi
 * AIS WAJIB mengisi kunci konfigurasi {@code sharepoint.client.id},
 * {@code sharepoint.client.secret}, dan {@code sharepoint.tenant.id} secara eksplisit di
 * database, tanpa itu integrasi SharePoint/Graph akan gagal otentikasi (bukan lagi diam-diam
 * memakai kredensial tertanam sebagai fallback produksi). Dua method lain
 * ({@link #getUsername()} dan {@link #getPassword()}) tetap memakai default berupa teks
 * placeholder yang jelas bukan kredensial nyata ({@code "your microsoft account username"} dan
 * {@code "your password"}), sehingga tidak diubah.
 * </p>
 * <p>
 * <b>TINDAK LANJUT DI LUAR PERUBAHAN KODE INI</b>: triplet client id/secret/tenant id yang
 * sebelumnya tertanam sudah lama berada di riwayat SVN dan WAJIB dianggap bocor — tim yang
 * memegang akses Azure AD terkait perlu meninjau apakah App Registration ini masih aktif, dan
 * bila ya, MEROTASI client secret tersebut di Azure Portal.
 * </p>
 *
 * <p>
 * Seluruh method di kelas ini mengikuti pola yang identik: membaca satu kunci konfigurasi lewat
 * {@link Common#getKonfigurasi(String, String)} dengan nilai default masing-masing, lalu
 * mengembalikan {@link Konfigurasi#getNilai()}; bila terjadi kegagalan apa pun saat membaca
 * konfigurasi (mis. kegagalan akses database), exception aslinya DITELAN dan dibungkus ulang
 * menjadi {@link IOException} dengan pesan generik yang menyebutkan nama parameter yang gagal
 * diambil — pemanggil tidak dapat membedakan "kegagalan database" dari "nilai memang tidak
 * ditemukan" hanya dari jenis exception ini.
 * </p>
 */
public class ApplicationProperties {

	/**
	 * Mengambil Application (client) ID Azure AD App Registration untuk integrasi SharePoint, dari
	 * kunci konfigurasi {@code sharepoint.client.id}. Lihat riwayat keamanan pada javadoc kelas —
	 * method ini sebelumnya memakai nilai default berbentuk kredensial nyata, sudah dihapus.
	 *
	 * @return nilai client id dari konfigurasi (string kosong bila kunci belum diisi)
	 * @throws IOException dibungkus dari kegagalan apa pun saat membaca konfigurasi, dengan pesan
	 *                      {@code "client id not found"}
	 */
	public static String getClientId() throws IOException {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi("sharepoint.client.id", "");
			return konfigurasi.getNilai();
		} catch (Exception ex) {
			throw new IOException("client id not found");
		}
	}

	/**
	 * Mengambil client secret Azure AD App Registration untuk integrasi SharePoint, dari kunci
	 * konfigurasi {@code sharepoint.client.secret}. Lihat riwayat keamanan pada javadoc kelas —
	 * method ini sebelumnya memakai nilai default berbentuk string acak khas client secret Azure
	 * AD sungguhan (bukan teks placeholder), sudah dihapus.
	 *
	 * @return nilai client secret dari konfigurasi (string kosong bila kunci belum diisi)
	 * @throws IOException dibungkus dari kegagalan apa pun saat membaca konfigurasi, dengan pesan
	 *                      {@code "client secret value not found"}
	 */
	public static String getClientSecret() throws IOException {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi("sharepoint.client.secret", "");
			return konfigurasi.getNilai();
		} catch (Exception ex) {
			throw new IOException("client secret value not found");
		}
	}

	/**
	 * Mengambil Directory (tenant) ID Azure AD untuk integrasi SharePoint, dari kunci konfigurasi
	 * {@code sharepoint.tenant.id}. Lihat riwayat keamanan pada javadoc kelas — method ini
	 * sebelumnya memakai nilai default berformat GUID tenant Azure AD sungguhan, sudah dihapus.
	 *
	 * @return nilai tenant id dari konfigurasi (string kosong bila kunci belum diisi)
	 * @throws IOException dibungkus dari kegagalan apa pun saat membaca konfigurasi, dengan pesan
	 *                      {@code "tenant id value not found"}
	 */
	public static String getTenantId() throws IOException {
		try {

			Konfigurasi konfigurasi = Common.getKonfigurasi("sharepoint.tenant.id", "");
			return konfigurasi.getNilai();
		} catch (Exception ex) {
			throw new IOException("tenant id value not found");
		}
	}

	/**
	 * Mengambil daftar scope OAuth2 Microsoft Graph yang diminta saat otentikasi, dari kunci
	 * konfigurasi {@code sharepoint.scope} (nilai berupa string dipisah koma, default
	 * {@code "https://graph.microsoft.com/.default"} — scope bawaan Microsoft Graph yang
	 * mewakili seluruh izin yang sudah di-{@code consent}-kan pada App Registration terkait).
	 *
	 * @return daftar scope hasil pemisahan nilai konfigurasi berdasarkan koma
	 * @throws IOException dibungkus dari kegagalan apa pun saat membaca konfigurasi, dengan pesan
	 *                      {@code "scopes value not found"}
	 */
	public static List<String> getScopeList() throws IOException {
		try {

			Konfigurasi konfigurasi = Common.getKonfigurasi("sharepoint.scope", "https://graph.microsoft.com/.default");
			String[] scopeList = konfigurasi.getNilai().split(",");
			return Arrays.asList(scopeList);
		} catch (Exception ex) {
			throw new IOException("scopes value not found");
		}
	}

	/**
	 * Mengambil username akun Microsoft yang dipakai untuk alur otentikasi Resource Owner Password
	 * Credentials (ROPC) ke SharePoint/Graph, dari kunci konfigurasi {@code sharepoint.user.name}
	 * (default berupa teks placeholder {@code "your microsoft account username"}, bukan kredensial
	 * nyata — berbeda dari default {@link #getClientId()}/{@link #getClientSecret()}/
	 * {@link #getTenantId()}, lihat javadoc kelas).
	 *
	 * @return nilai username dari konfigurasi (atau placeholder bila kunci belum diisi)
	 * @throws IOException dibungkus dari kegagalan apa pun saat membaca konfigurasi, dengan pesan
	 *                      {@code "user name value not found"}
	 */
	public static String getUsername() throws IOException {
		try {

			Konfigurasi konfigurasi = Common.getKonfigurasi("sharepoint.user.name", "your microsoft account username");
			return konfigurasi.getNilai();
		} catch (Exception ex) {
			throw new IOException("user name value not found");
		}
	}

	/**
	 * Mengambil password akun Microsoft yang dipakai untuk alur otentikasi Resource Owner Password
	 * Credentials (ROPC) ke SharePoint/Graph, dari kunci konfigurasi
	 * {@code sharepoint.user.password} (default berupa teks placeholder {@code "your password"},
	 * bukan kredensial nyata).
	 *
	 * @return nilai password dari konfigurasi (atau placeholder bila kunci belum diisi)
	 * @throws IOException dibungkus dari kegagalan apa pun saat membaca konfigurasi, dengan pesan
	 *                      {@code "user name value not found"} (perhatikan: pesan galat method ini
	 *                      identik dengan pesan galat {@link #getUsername()} walau menyangkut
	 *                      parameter password — kemungkinan salin-tempel yang belum diperbarui,
	 *                      dicatat di sini sebagai observasi tanpa mengubah kode)
	 */
	public static String getPassword() throws IOException {
		try {

			Konfigurasi konfigurasi = Common.getKonfigurasi("sharepoint.user.password", "your password");
			return konfigurasi.getNilai();
		} catch (Exception ex) {
			throw new IOException("user name value not found");
		}
	}
}