package ais.action.master.inventory;

import org.hibernate.Session;

import ais.database.model.Tbmuser;
import ais.database.model.inventory.Toko;

/**
 * <h3>Gerbang "siapa boleh mengubah harga" (kebijakan per toko).</h3>
 *
 * <p>Latar (permintaan 2026-08-20): harga jual/harga beli adalah angka finansial yang
 * berdampak ke laporan laba rugi dan penagihan, sehingga sebagian toko ingin
 * membatasi siapa saja yang boleh mengubahnya. Kebijakan disimpan pada
 * {@link Toko#getSemuaBolehUbahHarga()} (default {@code true} = perilaku lama, semua
 * pengguna boleh) dan daftar akun pada {@link Toko#getUserBolehUbahHarga()}.</p>
 *
 * <p><b>Kenapa di server.</b> Aturan ini ditegakkan pada titik SIMPAN di peladen supaya
 * keempat kanal (POS Desktop, Android, JSP, ZK) tunduk pada aturan yang sama; menonaktifkan
 * kolom di layar hanyalah kenyamanan, bukan pengaman.</p>
 *
 * <p><b>Yang TIDAK dibatasi.</b> Menyimpan perubahan non-harga (nama, stok minimum,
 * kategori, dst.) tetap bebas -- pemanggil hanya memeriksa ketika nilai harga benar-benar
 * BERUBAH, sehingga pengguna tanpa hak tetap dapat menyunting data lain.</p>
 */
public final class HargaAksesUtil {

	private HargaAksesUtil() {
	}

	/** Admin global (tanpa pedagang) selalu boleh -- selaras gerbang lain di aplikasi. */
	public static boolean bolehUbahHarga(Toko toko, Tbmuser tbmuser) {
		if (tbmuser == null) {
			return false;
		}
		if (ais.common.Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		if (toko == null || Boolean.TRUE.equals(toko.getSemuaBolehUbahHarga())) {
			return true;
		}
		String daftar = toko.getUserBolehUbahHarga();
		if (daftar == null || daftar.trim().length() == 0) {
			return false;
		}
		String userId = tbmuser.getUserId() == null ? "" : tbmuser.getUserId().trim();
		if (userId.length() == 0) {
			return false;
		}
		return normalkan(daftar).indexOf("," + userId.toLowerCase() + ",") >= 0;
	}

	/** Varian yang memuat toko dari session bila pemanggil hanya memegang id. */
	public static boolean bolehUbahHarga(Session session, Long tokoId, Tbmuser tbmuser) {
		if (tbmuser != null && ais.common.Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		Toko toko = (tokoId == null || session == null) ? null : (Toko) session.get(Toko.class, tokoId);
		return bolehUbahHarga(toko, tbmuser);
	}

	/** Pesan penolakan seragam supaya keempat kanal menampilkan alasan yang sama. */
	public static String pesanDitolak() {
		return "Anda tidak boleh mengubah harga karena tidak diberikan akses. "
				+ "Kebijakan toko membatasi perubahan harga hanya untuk pengguna tertentu; "
				+ "hubungi admin atau supervisor bila harga memang perlu diperbarui.";
	}

	/** Bandingkan dua nilai harga dgn toleransi pecahan supaya tidak memicu gerbang sia-sia. */
	public static boolean berubah(Double lama, double baru) {
		double a = lama == null ? 0.0 : lama.doubleValue();
		return Math.abs(a - baru) > 0.005;
	}

	/** CSV disimpan dgn koma pembungkus + huruf kecil supaya pencocokan konsisten. */
	public static String normalkan(String csv) {
		if (csv == null) {
			return ",";
		}
		StringBuilder sb = new StringBuilder(",");
		String[] bagian = csv.replace(";", ",").split(",");
		for (int i = 0; i < bagian.length; i++) {
			String v = bagian[i].trim().toLowerCase();
			if (v.length() > 0 && sb.indexOf("," + v + ",") < 0) {
				sb.append(v).append(",");
			}
		}
		return sb.toString();
	}
}
