package ais.action.master.helper.util;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Label;

import ais.common.Common;
import ais.database.model.Detailperkuliahan;
import ais.database.model.FormatNilai;
import ais.database.model.FormatNilaiTambahan;

/**
 * Utilitas statis kecil untuk mengisi satu komponen input/tampilan nilai ({@link Doublebox} atau
 * {@link Label}) dengan nilai belum-terverifikasi ({@code retreiveDetailNilaiBelumVerify}) dari satu
 * {@link FormatNilai}, atau nilai tambahan ({@code retreiveDetailNilaiTambahan}) dari satu
 * {@link FormatNilaiTambahan}, milik satu {@link Detailperkuliahan}. Untuk {@link Doublebox}, komponen
 * disembunyikan+dikunci selama pengambilan data lalu ditampilkan kembali setelah nilai terisi
 * (mencegah pengguna mengedit nilai lama sebelum nilai baru selesai dimuat); untuk {@link Label},
 * nilai langsung diformat dan ditulis. Kegagalan pengambilan nilai ditangkap dan dilaporkan lewat
 * {@code Common.tampilErrorJikaAdmin}, dengan nilai default {@code 0.0} tetap ditampilkan.
 */
public class NilaiLoader {

	/** Varian {@link #startLoad(Detailperkuliahan, FormatNilai, FormatNilaiTambahan, Component)} khusus {@link FormatNilai} (nilai komponen penilaian biasa). */
	public static void startLoad(final Detailperkuliahan detailperkuliahan, final FormatNilai formatNilai,
			final Component component) {
		startLoad(detailperkuliahan, formatNilai, null, component);
	}

	/** Varian {@link #startLoad(Detailperkuliahan, FormatNilai, FormatNilaiTambahan, Component)} khusus {@link FormatNilaiTambahan} (nilai tambahan/ekstra). */
	public static void startLoad(final Detailperkuliahan detailperkuliahan,
			final FormatNilaiTambahan formatNilaiTambahan, final Component component) {
		startLoad(detailperkuliahan, null, formatNilaiTambahan, component);
	}

	/**
	 * Implementasi kanonik: mengambil nilai sesuai {@code formatNilai} atau {@code formatNilaiTambahan}
	 * (tepat satu yang diisi) dari {@code detailperkuliahan}, lalu menuliskannya ke {@code component}
	 * ({@link Doublebox} atau {@link Label}; tipe lain diabaikan).
	 *
	 * @param detailperkuliahan   sumber data nilai
	 * @param formatNilai         format nilai biasa, atau {@code null} bila memakai {@code formatNilaiTambahan}
	 * @param formatNilaiTambahan format nilai tambahan, atau {@code null} bila memakai {@code formatNilai}
	 * @param component           komponen ZK tujuan penulisan nilai
	 */
	public static void startLoad(final Detailperkuliahan detailperkuliahan, final FormatNilai formatNilai,
			final FormatNilaiTambahan formatNilaiTambahan, final Component component) {
		if (component instanceof Doublebox) {
			((Doublebox) component).setReadonly(true);
			((Doublebox) component).setVisible(false);
		}

		Double nilai = 0.0;

		try {
			if (formatNilai != null) {
				nilai = detailperkuliahan.retreiveDetailNilaiBelumVerify(formatNilai);
			} else if (formatNilaiTambahan != null) {
				nilai = detailperkuliahan.retreiveDetailNilaiTambahan(formatNilaiTambahan);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
		Double n = nilai == null ? 0.0 : nilai.doubleValue();

		if (component instanceof Doublebox) {
			((Doublebox) component).setVisible(true);
			((Doublebox) component).setReadonly(false);
			((Doublebox) component).setValue(n);
		} else if (component instanceof Label) {
			((Label) component).setValue(Common.numberFormat.get().format(n));
		}

	}

}
