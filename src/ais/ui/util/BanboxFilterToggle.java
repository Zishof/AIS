package ais.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Grid;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

/**
 * Penyederhana tampilan filter pada popup pencarian AmbilData*Banbox.
 *
 * <p>
 * Secara default hanya baris filter PERTAMA (berisi ±4 field paling umum, mis.
 * NIM/Nama/Kode/Telp) yang ditampilkan. Baris filter selebihnya disembunyikan di
 * balik tombol toggle <b>"Pencarian Lebih Lanjut"</b>; sekali klik dibuka, klik
 * lagi ditutup. Sekaligus tinggi region North dirapatkan supaya tidak ada ruang
 * kosong di bawah filter.
 * </p>
 *
 * <p>
 * Dipakai ulang oleh SEMUA banbox cukup dengan satu baris pemanggilan, supaya
 * tampilannya konsisten dan gampang dirawat di kemudian hari.
 * </p>
 */
public class BanboxFilterToggle {

	private BanboxFilterToggle() {
	}

	/**
	 * Tinggi North saat hanya baris filter umum yang tampil. Harus memuat 1 baris field
	 * + toolbar (tombol Pencarian Lebih Lanjut / Cari / Bersihkan yang bisa membungkus ke
	 * baris ke-2), sehingga tombol-tombol selalu tampil utuh dan tidak terpotong.
	 */
	private static final String TINGGI_TUTUP = "150px";

	/**
	 * Tinggi North untuk filter SATU baris (≈ ≤4 field): harus memuat 1 baris field + 1 baris
	 * toolbar (Cari/Bersihkan). Nilai lama 94px ternyata TERLALU PENDEK pada banyak popup
	 * (MyGrid memberi header + baris field ber-valign top ≈ 100px) sehingga toolbar terpotong /
	 * ter-scroll keluar pandangan → tombol "Cari" tampak HILANG dan filter "dempet ke atas"
	 * (mis. AmbilData*Banyak pemilih mahasiswa di KKN/PKL, dan banbox Ruang). Dinaikkan agar
	 * toolbar selalu tampil utuh.
	 */
	private static final String TINGGI_TUTUP_RAPAT = "130px";

	/**
	 * Sisakan baris filter pertama; sembunyikan sisanya di balik tombol
	 * "Pencarian Lebih Lanjut" yang ditaruh pada toolbar Cari/Bersihkan.
	 *
	 * @param north     region North tempat filter berada (tingginya akan dirapatkan)
	 * @param searchgrid grid berisi baris-baris field filter
	 * @param toolbar   toolbar tempat tombol toggle dipasang (boleh null)
	 */
	public static void pasang(final North north, Grid searchgrid, Toolbar toolbar) {
		pasang(north, searchgrid, toolbar, null);
	}

	/**
	 * Varian dengan tinggi North TERTUTUP kustom. Berguna untuk popup yang saat tertutup hanya
	 * menampilkan 1 baris (mis. instruksi) sehingga tinggi default {@code TINGGI_TUTUP} (150px)
	 * menyisakan ruang kosong besar. Pemanggil dapat memberi tinggi rapat (mis. {@code "64px"}).
	 * Bila {@code null}/kosong, dipakai heuristik tinggi default seperti varian 3-argumen,
	 * sehingga pemanggil lama TIDAK terpengaruh.
	 *
	 * @param tinggiTutupKustom tinggi North saat tertutup (mis. {@code "64px"}), atau {@code null}.
	 */
	public static void pasang(final North north, Grid searchgrid, Toolbar toolbar, String tinggiTutupKustom) {
		try {
			if (north == null || searchgrid == null || searchgrid.getRows() == null) {
				return;
			}

			// Hitung jumlah baris filter dulu untuk menentukan tinggi North yang RAPAT.
			List<?> anak = searchgrid.getRows().getChildren();
			int totalRow = 0;
			for (int i = 0; i < anak.size(); i++) {
				if (anak.get(i) instanceof Row) {
					totalRow++;
				}
			}

			// Satu baris filter (≈ ≤4 field) → tak ada toggle "Pencarian Lebih Lanjut", toolbar
			// muat 1 baris → pakai tinggi RAPAT supaya tidak ada ruang kosong (sesuai permintaan).
			// Lebih dari satu baris → ada toggle yang bisa membungkus toolbar → beri ruang penuh.
			final String tinggiTutup = (tinggiTutupKustom != null && !tinggiTutupKustom.trim().isEmpty())
					? tinggiTutupKustom
					: ((totalRow <= 1) ? TINGGI_TUTUP_RAPAT : TINGGI_TUTUP);

			// Rapatkan tinggi North dulu (berlaku Banbox & Banyak). Sebagian popup
			// (AmbilData*Banyak) memakai North flex=true tanpa tinggi tetap → matikan flex
			// agar tinggi yang kita set benar-benar diterapkan (filter rapat, tanpa ruang kosong).
			ZkCompat.setFlex(north, false);
			north.setAutoscroll(true);
			north.setHeight(tinggiTutup);

			// Popup Banbox (Bandpopup) SUDAH mendapat tombol "Pencarian Lebih Lanjut" generik
			// dari MainStyleHelper (patch 2026-06-12, injeksi JS pada .z-bandpopup). Di sana
			// CUKUP rapatkan tinggi North; JANGAN menambah toggle / menyembunyikan baris lagi
			// supaya TIDAK ada tombol dobel & tidak bentrok. Toggle helper ini khusus untuk
			// AmbilData*Banyak (popup MyWindow) yang TIDAK dijangkau JS MainStyleHelper.
			org.zkoss.zk.ui.Component induk = north;
			while (induk != null) {
				if (induk instanceof org.zkoss.zul.Bandpopup) {
					return;
				}
				induk = induk.getParent();
			}

			final List<Row> barisLanjutan = new ArrayList<Row>();
			int idx = 0;
			for (int i = 0; i < anak.size(); i++) {
				Object c = anak.get(i);
				if (c instanceof Row) {
					idx++;
					if (idx > 1) {
						((Row) c).setVisible(false);
						barisLanjutan.add((Row) c);
					}
				}
			}

			final String tinggiBuka = hitungTinggiBuka(totalRow);
			final String tinggiTutupFinal = tinggiTutup;

			// Tidak ada filter lanjutan (cukup satu baris) → tak perlu tombol toggle.
			if (barisLanjutan.isEmpty() || toolbar == null) {
				return;
			}

			final Toolbarbutton toggle = new Toolbarbutton();
			toggle.setLabel("Pencarian Lebih Lanjut");
			toggle.setSclass("ais-adv-search-toggle");
			toggle.setTooltiptext("Tampilkan / sembunyikan filter pencarian lainnya");
			toggle.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					boolean sedangTampil = !barisLanjutan.isEmpty() && barisLanjutan.get(0).isVisible();
					boolean buka = !sedangTampil;
					for (int i = 0; i < barisLanjutan.size(); i++) {
						barisLanjutan.get(i).setVisible(buka);
					}
					north.setHeight(buka ? tinggiBuka : tinggiTutupFinal);
					toggle.setLabel(buka ? "Sembunyikan Pencarian Lanjutan" : "Pencarian Lebih Lanjut");
					toggle.setSclass(buka ? "ais-adv-search-toggle ais-adv-search-open" : "ais-adv-search-toggle");
				}
			});
			toolbar.appendChild(toggle);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/BanboxFilterToggle.java:157");
		}
	}

	/**
	 * Tinggi North saat semua filter lanjutan dibuka: mengikuti jumlah baris field +
	 * cadangan untuk toolbar tombol (bisa membungkus 2 baris), dibatasi agar tetap rapi.
	 */
	private static String hitungTinggiBuka(int totalRow) {
		int t = totalRow * 38 + 110;
		if (t < 170) {
			t = 170;
		}
		if (t > 400) {
			t = 400;
		}
		return t + "px";
	}
}
