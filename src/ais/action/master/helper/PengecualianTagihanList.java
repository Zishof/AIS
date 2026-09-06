package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ais.database.model.DetailBiaya;

/**
 * Penanda internal bahwa hasil kosong disebabkan NIM masuk daftar pengecualian.
 * Bagi pemanggil kelas ini tetap merupakan List kosong biasa, tetapi utilitas
 * pembayaran dapat membedakannya dari kondisi "setting tidak ditemukan" agar
 * tidak melanjutkan pencarian ke setting umum atau tagihan bulanan.
 */
public final class PengecualianTagihanList extends ArrayList<DetailBiaya> {

	private static final long serialVersionUID = 1L;

	/**
	 * Membuat instance kosong dari penanda ini. Dipakai pemanggil yang perlu
	 * mengembalikan "tidak ada tagihan" namun tetap memberi tahu kode di
	 * atasnya bahwa kekosongan tersebut BUKAN karena setting umum/tagihan
	 * bulanan tidak ditemukan, melainkan karena NIM masuk daftar pengecualian.
	 *
	 * @return list kosong bertipe {@link PengecualianTagihanList}.
	 */
	public static List<DetailBiaya> kosong() {
		return new PengecualianTagihanList();
	}

	/**
	 * Memeriksa apakah suatu koleksi hasil pencarian tagihan adalah penanda
	 * pengecualian ini (bukan sekadar {@code List} kosong biasa).
	 *
	 * @param detailBiayas koleksi yang hendak diperiksa; boleh {@code null}
	 *                     (akan mengembalikan {@code false}).
	 * @return {@code true} bila {@code detailBiayas} adalah instance
	 *         {@link PengecualianTagihanList}.
	 */
	public static boolean adalah(Collection detailBiayas) {
		return detailBiayas instanceof PengecualianTagihanList;
	}
}
