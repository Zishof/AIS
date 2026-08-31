package ais.ui.util;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Initiator;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.RequestContext;
import ais.database.model.PerguruanTinggi;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * {@link Initiator} ZK yang dipasang pada berkas {@code .zul} halaman-halaman utama AIS untuk
 * menyesuaikan judul tab browser (page title) dan ikon favicon secara dinamis berdasarkan
 * institusi/tenant yang sedang aktif (Perguruan Tinggi, Sekolah, atau Yayasan) sebelum halaman
 * ditampilkan ke pengguna. Ini memungkinkan satu basis kode AIS yang sama melayani banyak
 * instansi (multi-tenant) dengan identitas visual (judul + logo) yang berbeda-beda tanpa
 * konfigurasi statis per instalasi.
 *
 * <p>
 * Logika utama berada pada {@link #doInit(Page, Map)}, yang dijalankan ZK sebelum komponen
 * halaman dikomposisi. Method tersebut:
 * </p>
 * <ol>
 * <li>Mengambil {@link HttpServletRequest} aktif (dari {@link ExecutionsCtrl} bila tersedia,
 * jika tidak jatuh ke {@link RequestContext#get()}).</li>
 * <li>Menentukan entitas institusi yang relevan untuk request tersebut: {@code
 * PerguruanTinggi}, {@code Yayasan}, dan {@code Sekolah} lewat {@code PerguruanTinggiUtil} dan
 * {@code SekolahUtil}, serta memakai {@code Common#chekPtAtauSekolah()} untuk menentukan apakah
 * instalasi ini berjenis sekolah ({@code ptYa[1]}).</li>
 * <li>Memilih judul halaman dan logo sesuai prioritas: Sekolah aktif → Yayasan aktif →
 * Perguruan Tinggi aktif, dengan judul default diambil dari konfigurasi {@code judul_header}
 * (atau {@code judul_header_sekolah} untuk jalur sekolah) dan logo default berupa
 * {@code /img/logo.png} bila tidak ada logo kustom yang diunggah untuk institusi terkait.</li>
 * <li>Menuliskan judul terpilih ke {@code PageDefinition} halaman dan menyimpan URL logo
 * terpilih sebagai atribut halaman {@code myFavicon} (dibaca oleh template/layout untuk
 * merender tag favicon).</li>
 * </ol>
 * <p>
 * Seluruh proses dibungkus satu blok try-catch tunggal: kegagalan apa pun (mis. request tidak
 * tersedia, entitas institusi tidak ditemukan) dicatat lewat {@code ErrorAuditUtil.record} dan
 * TIDAK menghentikan pemuatan halaman — halaman tetap tampil dengan judul/favicon default bila
 * penentuan dinamis gagal.
 * </p>
 */
public class MyInit implements Initiator {

	/** Tidak melakukan apa pun; hook siklus hidup {@link Initiator} yang tidak dipakai kelas ini. */
	public void doAfterCompose(Page arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	/** Tidak menangani exception apa pun (selalu mengembalikan {@code false}, meneruskan exception ke penanganan default ZK). */
	public boolean doCatch(Throwable arg0) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	/** Tidak melakukan apa pun; hook siklus hidup {@link Initiator} yang tidak dipakai kelas ini. */
	public void doFinally() throws Exception {
		// TODO Auto-generated method stub

	}

	/**
	 * Menentukan dan menerapkan judul halaman serta favicon dinamis sesuai institusi/tenant
	 * aktif untuk request saat ini — lihat penjelasan alur lengkap pada javadoc kelas
	 * {@link MyInit}. Dipanggil ZK sebelum komponen halaman dikomposisi; seluruh kegagalan
	 * ditangkap dan dicatat, tidak dilempar ulang.
	 *
	 * @param arg0 halaman ZK yang sedang diinisialisasi
	 * @param arg1 peta argumen inisialisasi dari deklarasi {@code <?init class="..."?>} di
	 *             berkas {@code .zul}, tidak dipakai implementasi ini
	 * @throws Exception tidak pernah dilempar keluar; seluruh kegagalan ditangkap secara
	 *                    internal dan dicatat lewat audit error
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void doInit(Page arg0, Map arg1) throws Exception {
		// TODO Auto-generated method stub
		try {
			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}

			if (request == null) {
				request = RequestContext.get();
			}
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);
			Yayasan yayasan = SekolahUtil.getYayasan(request);
			Sekolah sekolah = SekolahUtil.getSekolah(request);
			boolean[] ptYa = Common.chekPtAtauSekolah();
			boolean ya = ptYa[1];
			String judul = Common.getKonfigurasi("judul_header", "eCampus").getNilai();
			String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
					.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_", perguruanTinggi);
			if (logo_PerguruanTinggi == null || logo_PerguruanTinggi.trim().isEmpty()) {
				logo_PerguruanTinggi = "/img/logo.png";
			}
			if (ya && (sekolah != null && sekolah.getId() != null)) {
				judul = Common.getKonfigurasi("judul_header_sekolah", "eSchool").getNilai();
				ExecutionsCtrl.getCurrentCtrl().getCurrentPageDefinition()
						.setTitle((judul.isEmpty() ? "" : judul + " | ") + sekolah.getNama());

				String logo_PerguruanTinggi_local = ais.action.master.sekolah.util.SekolahUtil.getSekolahMedia(request,
						"logo_sekolah_", sekolah);
				if (logo_PerguruanTinggi_local != null && !logo_PerguruanTinggi_local.endsWith("logo.png")) {
					logo_PerguruanTinggi = logo_PerguruanTinggi_local;
				}

			} else if (ya && (yayasan != null && yayasan.getId() != null)) {
				ExecutionsCtrl.getCurrentCtrl().getCurrentPageDefinition().setTitle(yayasan.getNama());

				String logo_PerguruanTinggi_local = ais.action.master.sekolah.util.SekolahUtil.getYayasanMedia(request,
						"logo_yayasan_", yayasan);
				if (logo_PerguruanTinggi_local != null && !logo_PerguruanTinggi_local.endsWith("logo.png")) {
					logo_PerguruanTinggi = logo_PerguruanTinggi_local;
				}

			} else if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
				ExecutionsCtrl.getCurrentCtrl().getCurrentPageDefinition()
						.setTitle((judul.isEmpty() ? "" : judul + " | ") + perguruanTinggi.getNama());
			}

			ExecutionsCtrl.getCurrentCtrl().getCurrentPage().setAttribute("myFavicon", logo_PerguruanTinggi);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/MyInit.java:88");
		}

	}
}
