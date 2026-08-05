package ais.ui.util;

import org.zkoss.zul.Detail;
import org.zkoss.zul.Row;

/**
 * Kembali memakai org.zkoss.zul.Detail bawaan ZK (13-06-2026) atas
 * permintaan user: varian jendela kustom masih bermasalah di lapangan.
 *
 * Catatan penting:
 * - Server class Detail ada di zul.jar; WIDGET client-nya (zkex.grid.Detail)
 *   ada di zkex.jar — zkex.jar sudah DIKEMBALIKAN ke WEB-INF/lib dari backup
 *   lib_dihapus_20260613. Tanpa zkex.jar muncul "Widget class required".
 * - Untuk cutover ZK CE 9 nanti, Detail perlu pengganti lagi (widget zkex
 *   tidak tersedia di CE); class ini menjadi satu-satunya titik ubah.
 *
 * Kompatibel Java 1.6/1.7.
 */
public class MyDetail extends Detail {

	private static final long serialVersionUID = 1L;

	/**
	 * Pengganti Row.getDetailChild(): mencari MyDetail di antara anak
	 * sebuah Row (dipakai PenilaianAction).
	 */
	public static MyDetail dari(Row row) {
		if (row == null) {
			return null;
		}
		for (int i = 0; i < row.getChildren().size(); i++) {
			Object anak = row.getChildren().get(i);
			if (anak instanceof MyDetail) {
				return (MyDetail) anak;
			}
		}
		return null;
	}
}
