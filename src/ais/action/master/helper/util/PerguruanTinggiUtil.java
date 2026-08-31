package ais.action.master.helper.util;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.zkoss.zk.ui.sys.ExecutionsCtrl;

import ais.action.master.PerguruanTinggiAction;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.RequestContext;
import ais.database.model.Konfigurasi;
import ais.database.model.Pendaftar;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Sekolah;

/**
 * Utilitas resolusi <b>tenant</b> (multi-institusi) untuk permintaan HTTP yang sedang berjalan:
 * menentukan {@link PerguruanTinggi} (perguruan tinggi), {@link Pendaftar} (penerimaan
 * mahasiswa baru/PMB), dan aset media (logo/banner/background) yang berlaku untuk request
 * tersebut. AIS dapat melayani beberapa institusi dari satu deployment, sehingga resolusi ini
 * penting untuk branding dan pemisahan data per institusi (mis. laporan, tampilan PMB).
 *
 * <p>
 * Strategi resolusi mengikuti urutan prioritas yang konsisten di seluruh method publik:
 * </p>
 * <ol>
 * <li>Relasi langsung pada {@link Tbmuser} yang sedang login (mis. {@code tbmuser.getPerguruanTinggi()}
 * atau relasi lewat {@link Tbmrole} → jurusan/fakultas), bila pengguna sudah login.</li>
 * <li>Cache pada {@code HttpSession} (atribut {@code perguruanTinggi_data}/{@code pendaftar_data}/
 * {@code media_data_*}) — hasil resolusi sebelumnya untuk sesi yang sama dipakai ulang agar
 * tidak menghitung ulang di setiap request.</li>
 * <li>Pencocokan nama domain server request ({@code request.getServerName()}) terhadap peta
 * {@code PerguruanTinggiAction.perguruanTinggiByDomain}/{@code pendaftarByDomain} (dimuat lewat
 * {@code PerguruanTinggiAction.reInitByDomain()} bila peta masih kosong) — dicoba dulu dengan
 * {@code startsWith}, lalu (bila belum ketemu) dengan {@code contains} sebagai fallback yang
 * lebih longgar.</li>
 * <li>Relasi lewat {@link Sekolah} (modul sekolah) bila request berasal dari domain sekolah.</li>
 * <li>Default statis ({@code PerguruanTinggiAction.perguruanTinggiDefault}) bila semua langkah
 * di atas gagal.</li>
 * </ol>
 *
 * <p>
 * Sumber {@link HttpServletRequest} sendiri, ketika tidak diberikan eksplisit, diambil dari
 * konteks eksekusi ZK ({@link ExecutionsCtrl#getCurrent()}) atau, sebagai fallback, dari
 * {@link RequestContext#get()} (thread-local request untuk konteks non-ZK, mis. servlet murni).
 * </p>
 */
public class PerguruanTinggiUtil {

	/**
	 * Menentukan {@link PerguruanTinggi} untuk request saat ini, diambil otomatis dari konteks
	 * eksekusi ZK atau {@link RequestContext}. Lihat javadoc kelas untuk urutan prioritas
	 * resolusi. Mengembalikan {@code PerguruanTinggiAction.perguruanTinggiDefault} bila terjadi
	 * kegagalan apa pun (mis. tidak ada request aktif).
	 */
	public static PerguruanTinggi getPerguruanTinggi() {

		try {

			if (PerguruanTinggiAction.perguruanTinggiByDomain.isEmpty()) {
				PerguruanTinggiAction.reInitByDomain();
			}
			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}
			if (request == null) {
				request = RequestContext.get();
			}
			return getPerguruanTinggi(request);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/PerguruanTinggiUtil.java:38");

		}
		return PerguruanTinggiAction.perguruanTinggiDefault;
	}

	/**
	 * Menentukan {@link PerguruanTinggi} untuk {@code request} yang diberikan: relasi langsung
	 * pengguna login → cache sesi → resolusi domain/sekolah (lewat
	 * {@link #getPerguruanTinggiData(HttpServletRequest)}, hasilnya disimpan ke cache sesi) →
	 * default. Lihat javadoc kelas untuk detail lengkap urutan prioritas.
	 *
	 * @param request permintaan HTTP saat ini; boleh {@code null} (langsung mengembalikan hasil
	 *                resolusi domain/default tanpa cache sesi)
	 * @return perguruan tinggi yang berlaku, tidak pernah {@code null}
	 *         (jatuh ke {@code perguruanTinggiDefault} bila tidak ditemukan)
	 */
	public static PerguruanTinggi getPerguruanTinggi(HttpServletRequest request) {

		Tbmuser tbmuser = Common.getCurrentUser(request);
		try {
			PerguruanTinggi ptUser = tbmuser == null ? null : tbmuser.getPerguruanTinggi();
			if (ptUser != null && ptUser.getId() != null) {
				return ptUser;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/PerguruanTinggiUtil.java:52");
			// User mahasiswa/PMB tertentu bisa tidak memiliki relasi PT langsung.
		}

		PerguruanTinggi perguruanTinggi = (PerguruanTinggi) (request == null ? null
				: request.getSession().getAttribute("perguruanTinggi_data"));
		if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
			return perguruanTinggi;
		}
		perguruanTinggi = getPerguruanTinggiData(request);
		if (request != null && perguruanTinggi != null && perguruanTinggi.getId() != null) {
			request.getSession().setAttribute("perguruanTinggi_data", perguruanTinggi);
		}

//		System.out.println("perguruanTinggi -> " + perguruanTinggi);

		return perguruanTinggi == null ? PerguruanTinggiAction.perguruanTinggiDefault : perguruanTinggi;
	}

	/** Resolusi tanpa-cache berbasis hak akses pengguna, lalu domain server, lalu relasi sekolah; lihat javadoc kelas untuk urutan lengkap. Dipanggil hanya saat cache sesi kosong. */
	private static PerguruanTinggi getPerguruanTinggiData(HttpServletRequest request) {

		try {
			if (request == null) {
				return null;
			}

			Tbmuser tbmuser = Common.getCurrentUser(request);
			Tbmrole hakAkses = tbmuser == null ? null : tbmuser.hakAkses();
			if (tbmuser != null && hakAkses != null && hakAkses.getJurusan() != null
					&& hakAkses.getJurusan().getFakultas() != null
					&& hakAkses.getJurusan().getFakultas().getPerguruanTinggi() != null) {
				return hakAkses.getJurusan().getFakultas().getPerguruanTinggi();
			} else if (tbmuser != null && hakAkses != null && hakAkses.getFakultas() != null
					&& hakAkses.getFakultas().getPerguruanTinggi() != null) {
				return hakAkses.getFakultas().getPerguruanTinggi();
			}

			String sn = request.getServerName().toLowerCase().trim();
//			System.out.println("server name -> " + sn);

			for (String s : PerguruanTinggiAction.perguruanTinggiByDomain.keySet()) {
//				System.out.println("server s -> " + s);
				if (s != null && !s.trim().isEmpty() && sn.startsWith(s.trim().toLowerCase())) {
					return PerguruanTinggiAction.perguruanTinggiByDomain.get(s);
				}

			}

			Sekolah sekolah = SekolahUtil.getSekolah(request);
			if (sekolah != null && sekolah.getPerguruanTinggi() != null
					&& sekolah.getPerguruanTinggi().getId() != null) {
				return sekolah.getPerguruanTinggi();
			}

			if (PerguruanTinggiAction.perguruanTinggiByDomain.isEmpty()) {
				PerguruanTinggiAction.reInitByDomain();
			}

			for (String s : PerguruanTinggiAction.perguruanTinggiByDomain.keySet()) {

				if (s != null && !s.trim().isEmpty() && sn.contains(s.trim().toLowerCase())) {
					return PerguruanTinggiAction.perguruanTinggiByDomain.get(s);
				}
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/PerguruanTinggiUtil.java:117");

		}
		return PerguruanTinggiAction.perguruanTinggiDefault;
	}

	/** Seperti {@link #getPerguruanTinggi()} tetapi untuk entitas {@link Pendaftar} (konfigurasi jalur penerimaan/PMB); mengembalikan {@code null} bila tidak ditemukan (tidak ada default statis untuk Pendaftar). */
	public static Pendaftar getPendaftar() {

		try {

			if (PerguruanTinggiAction.pendaftarByDomain.isEmpty()) {
				PerguruanTinggiAction.reInitByDomain();
			}
			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}
			if (request == null) {
				request = RequestContext.get();
			}
			return getPendaftar(request);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/PerguruanTinggiUtil.java:138");

		}
		return null;
	}

	/** Seperti {@link #getPerguruanTinggi(HttpServletRequest)} tetapi untuk {@link Pendaftar}, dengan cache sesi {@code pendaftar_data}. */
	public static Pendaftar getPendaftar(HttpServletRequest request) {
		Pendaftar pendaftar = (Pendaftar) (request == null ? null
				: request.getSession().getAttribute("pendaftar_data"));
		if (pendaftar != null && pendaftar.getId() != null) {
			return pendaftar;
		}
		pendaftar = getPendaftarData(request);
		if (request != null && pendaftar != null && pendaftar.getId() != null) {
			request.getSession().setAttribute("pendaftar_data", pendaftar);
		}
		return pendaftar;
	}

	/** Resolusi {@link Pendaftar} tanpa-cache berbasis domain server lalu relasi sekolah; dipanggil hanya saat cache sesi kosong. */
	private static Pendaftar getPendaftarData(HttpServletRequest request) {

		try {
			if (request == null) {
				return null;
			}
			String sn = request.getServerName().toLowerCase().trim();
//			System.out.println("server name -> " + sn);

			for (String s : PerguruanTinggiAction.pendaftarByDomain.keySet()) {
//				System.out.println("server s -> " + s);
				if (s != null && !s.trim().isEmpty() && sn.startsWith(s.trim().toLowerCase())) {
					return PerguruanTinggiAction.pendaftarByDomain.get(s);
				}

			}

			Sekolah sekolah = SekolahUtil.getSekolah(request);
			if (sekolah != null && sekolah.getPendaftar() != null && sekolah.getPendaftar().getId() != null) {
				return sekolah.getPendaftar();
			}

			if (PerguruanTinggiAction.pendaftarByDomain.isEmpty()) {
				PerguruanTinggiAction.reInitByDomain();
			}

			for (String s : PerguruanTinggiAction.pendaftarByDomain.keySet()) {

				if (s != null && !s.trim().isEmpty() && sn.contains(s.trim().toLowerCase())) {
					return PerguruanTinggiAction.pendaftarByDomain.get(s);
				}
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/PerguruanTinggiUtil.java:190");

		}
		return null;
	}

	/**
	 * Mengambil path lokal (bukan URL) file lampiran bertipe {@code jenis} (mis. logo) milik
	 * {@link PerguruanTinggi} untuk request saat ini, lewat {@link LampiranLain#ambil}. Bila
	 * tidak ditemukan/gagal, jatuh ke {@code logo.png} default di direktori laporan
	 * ({@link Common#ambilREAL_PATH_REPORT()}).
	 *
	 * @param jenis kunci jenis lampiran (mis. {@code "logo"})
	 * @return path absolut file media
	 */
	public static String getMedia(String jenis) {

		try {

			PerguruanTinggi perguruanTinggi = getPerguruanTinggi();
			if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
				return LampiranLain.ambil(perguruanTinggi.getId(), jenis).ambilFile().getAbsolutePath();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/PerguruanTinggiUtil.java:204");

		}
		return new File(Common.ambilREAL_PATH_REPORT() + "/logo.png").getAbsolutePath();
	}

	/**
	 * Seperti {@link #getPerguruanTinggiMedia(HttpServletRequest, String)}, mengambil
	 * {@link HttpServletRequest} otomatis dari konteks eksekusi ZK/{@link RequestContext}. Bila
	 * request tidak tersedia atau resolusi gagal, mengembalikan URL gambar generik bawaan
	 * aplikasi (dipilih berdasarkan kata kunci pada {@code jenis}: {@code background},
	 * {@code banner}, atau default logo).
	 *
	 * @param jenis kunci jenis media (mis. {@code "logo"}, {@code "backgroundPMB"}, {@code "banner"})
	 * @return URL lengkap (dengan host+protokol) media yang berlaku
	 */
	public static String getPerguruanTinggiMedia(String jenis) {
		try {
			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}
			if (request == null) {
				request = RequestContext.get();
			}
			return getPerguruanTinggiMedia(request, jenis);
		} catch (Exception e) {
			if (jenis.contains("background")) {
				return Common.getRequestHostWithProtocol() + "/img/main.jpg";
			} else if (jenis.contains("banner")) {
				return Common.getRequestHostWithProtocol() + "/img/header.jpg";
			} else {
				return Common.getRequestHostWithProtocol() + "/img/logo.png";
			}
		}
	}

	/** Seperti {@link #getPerguruanTinggiMedia(HttpServletRequest, String, PerguruanTinggi)} tanpa perguruan tinggi eksplisit (diresolusi otomatis); hasil di-cache di sesi HTTP dengan kunci {@code "media_data_"+jenis}. */
	public static String getPerguruanTinggiMedia(HttpServletRequest request, String jenis) {
		String media = (String) (request == null ? null : request.getSession().getAttribute("media_data_" + jenis));
		if (media != null && !media.trim().isEmpty()) {
			return media;
		}
		media = getPerguruanTinggiMediaData(request, jenis);
		if (request != null && media != null && !media.trim().isEmpty()) {
			request.getSession().setAttribute("media_data_" + jenis, media);
		}
		return media;
	}

	/**
	 * Mengembalikan URL lengkap file media bertipe {@code jenis} (mis. {@code "logo"},
	 * {@code "banner"}, {@code "background"}) untuk {@code perguruanTinggi} yang diberikan,
	 * dengan hasil di-cache di sesi HTTP. Bila cache kosong, meneruskan ke
	 * {@link #getPerguruanTinggiMediaData(HttpServletRequest, String, PerguruanTinggi)} lalu
	 * menyimpan hasilnya ke sesi.
	 *
	 * @param request         permintaan HTTP saat ini, dipakai untuk membangun URL dan cache sesi
	 * @param jenis           kunci jenis media
	 * @param perguruanTinggi perguruan tinggi eksplisit (mengesampingkan resolusi otomatis
	 *                        bila tidak {@code null}/id kosong)
	 * @return URL lengkap media yang berlaku
	 */
	public static String getPerguruanTinggiMedia(HttpServletRequest request, String jenis,
			PerguruanTinggi perguruanTinggi) {
		String media = (String) (request == null ? null : request.getSession().getAttribute("media_data_" + jenis));
		if (media != null && !media.trim().isEmpty()) {
			return media;
		}
		media = getPerguruanTinggiMediaData(request, jenis, perguruanTinggi);
		if (request != null && media != null && !media.trim().isEmpty()) {
			request.getSession().setAttribute("media_data_" + jenis, media);
		}
		return media;
	}

	/** Delegasi ke {@link #getPerguruanTinggiMediaData(HttpServletRequest, String, PerguruanTinggi)} tanpa perguruan tinggi eksplisit (diresolusi dari pengguna login/domain). */
	private static String getPerguruanTinggiMediaData(HttpServletRequest request, String jenis) {
		return getPerguruanTinggiMediaData(request, jenis, null);
	}

	/**
	 * Membangun URL media tanpa-cache: dimulai dari {@code CURRENT_URL} dasar (protokol+host+
	 * context path, atau nilai konfigurasi {@code CURRENT_URL} bila
	 * {@code dapatkan_code_via_url_custom} aktif), lalu menambahkan segmen path berbasis
	 * {@code jenis} dan id institusi yang teridentifikasi. Id institusi diresolusi menurut
	 * urutan: {@link PerguruanTinggi} pengguna login → domain pendaftar ({@code startsWith}) →
	 * domain perguruan tinggi ({@code startsWith}) → relasi sekolah → domain perguruan tinggi
	 * ({@code contains}, fallback longgar) → gambar generik bawaan aplikasi. Ekstensi file
	 * ditentukan otomatis ({@code .jpg} untuk {@code background}/{@code banner}, selain itu
	 * {@code .png}). Bila konfigurasi {@code wajib_https} aktif dan URL belum HTTPS, protokol
	 * dipaksa menjadi {@code https}. Kegagalan apa pun jatuh ke gambar generik memakai
	 * {@code CURRENT_URL} yang sempat terbentuk sebelum galat terjadi.
	 */
	private static String getPerguruanTinggiMediaData(HttpServletRequest request, String jenis,
			PerguruanTinggi perguruanTinggi) {

		String CURRENT_URL = (Common.isSecure(request) ? "https://" : "http://") + request.getServerName()
				+ (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort())
				+ request.getContextPath();

		if (Common.bolehKonfigurasi("dapatkan_code_via_url_custom", Konfigurasi.TIDAK_AKTIF)) {
			CURRENT_URL = Common.getKonfigurasi("CURRENT_URL", Common.getRequestHostWithProtocol()).getNilai();
		}

		try {

			Tbmuser tbmuser = Common.getCurrentUser(request);
			if (tbmuser != null && tbmuser.getPerguruanTinggi() != null
					&& tbmuser.getPerguruanTinggi().getId() != null) {
				perguruanTinggi = tbmuser.getPerguruanTinggi();
			}

			if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
				CURRENT_URL = CURRENT_URL + "/img/" + jenis + perguruanTinggi.getId()
						+ (jenis.contains("background") || jenis.contains("banner") ? ".jpg" : ".png");
			} else {

				if (PerguruanTinggiAction.perguruanTinggiByDomain.isEmpty()) {
					PerguruanTinggiAction.reInitByDomain();
				}

				boolean a = false;

				String sn = request.getServerName().toLowerCase().trim();
//			System.out.println("server name -> " + sn);

				for (String s : PerguruanTinggiAction.pendaftarByDomain.keySet()) {

					if (sn.startsWith(s.trim().toLowerCase())) {
						CURRENT_URL = CURRENT_URL + "/img/" + jenis + "Pendaftar_"
								+ PerguruanTinggiAction.pendaftarByDomain.get(s).getId()
								+ (jenis.contains("background") || jenis.contains("banner") ? ".jpg" : ".png");
						a = true;
						break;
					}
				}
				if (!a) {
					for (String s : PerguruanTinggiAction.perguruanTinggiByDomain.keySet()) {

						if (sn.startsWith(s.trim().toLowerCase())) {
							CURRENT_URL = CURRENT_URL + "/img/" + jenis
									+ PerguruanTinggiAction.perguruanTinggiByDomain.get(s).getId()
									+ (jenis.contains("background") || jenis.contains("banner") ? ".jpg" : ".png");
							a = true;
							break;
						}
					}
				}

				if (!a) {
					Sekolah sekolah = SekolahUtil.getSekolah(request);
					if (sekolah != null && sekolah.getPerguruanTinggi() != null
							&& sekolah.getPerguruanTinggi().getId() != null) {

						CURRENT_URL = CURRENT_URL + "/img/" + jenis + sekolah.getPerguruanTinggi().getId()
								+ (jenis.contains("background") || jenis.contains("banner") ? ".jpg" : ".png");
						a = true;
					}
				}
				if (!a) {
					for (String s : PerguruanTinggiAction.perguruanTinggiByDomain.keySet()) {

						if (sn.contains(s.trim().toLowerCase())) {
							CURRENT_URL = CURRENT_URL + "/img/" + jenis
									+ PerguruanTinggiAction.perguruanTinggiByDomain.get(s).getId()
									+ (jenis.contains("background") || jenis.contains("banner") ? ".jpg" : ".png");
							a = true;
							break;
						}
					}
				}
				if (!a) {
					if (jenis.contains("background")) {
						CURRENT_URL = CURRENT_URL + "/img/main.jpg";
					} else if (jenis.contains("banner")) {
						CURRENT_URL = CURRENT_URL + "/img/header.jpg";
					} else {
						CURRENT_URL = CURRENT_URL + "/img/logo.png";
					}
				}
			}
			if (!CURRENT_URL.contains("https")) {
				if (Common.bolehKonfigurasi("wajib_https", Konfigurasi.TIDAK_AKTIF)) {
					CURRENT_URL = CURRENT_URL.replaceAll("http", "https");
				}
			}
			return CURRENT_URL;
		} catch (Exception e) {
			if (jenis.contains("background")) {
				return CURRENT_URL + "/img/main.jpg";
			} else if (jenis.contains("banner")) {
				return CURRENT_URL + "/img/header.jpg";
			} else {
				return CURRENT_URL + "/img/logo.png";
			}
		}
	}
}
