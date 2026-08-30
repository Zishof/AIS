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

	public static List<DetailBiaya> kosong() {
		return new PengecualianTagihanList();
	}

	public static boolean adalah(Collection detailBiayas) {
		return detailBiayas instanceof PengecualianTagihanList;
	}
}
