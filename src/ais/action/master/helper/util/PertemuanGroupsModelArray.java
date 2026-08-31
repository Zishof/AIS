package ais.action.master.helper.util;

import java.util.Comparator;

import org.zkoss.zul.GroupsModelArray;

import ais.database.model.Pertemuan;

/**
 * Tipe khusus untuk pertemuan groups model array. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GroupsModelArray}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code createGroupHead()}, {@code
 * createGroupFoot}(). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 *
 * @see GroupsModelArray
 */
public class PertemuanGroupsModelArray extends GroupsModelArray {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9178565020194034560L;

	@SuppressWarnings("rawtypes")
	public PertemuanGroupsModelArray(Pertemuan[] data, Comparator cmpr) {
		super(data, cmpr);

	}

	protected Object createGroupHead(Pertemuan[] groupdata, int index, int col) {
		return new Object[] { groupdata[0], index, col };
	}

	// Create GroupFoot Data
	protected Object createGroupFoot(Pertemuan[] groupdata, int index, int col) {
		return groupdata.length;
	}
}
