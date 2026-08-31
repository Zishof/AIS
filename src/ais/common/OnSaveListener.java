package ais.common;

import org.zkoss.zk.ui.event.Event;

/**
 * Kontrak callback generik untuk komponen ZK (biasanya composer/controller layar entri data) yang
 * perlu diberi tahu saat tombol "Simpan" (atau aksi setara) ditekan pengguna, tanpa komponen
 * pemanggil perlu mengetahui detail implementasi penyimpanan di balik layar.
 *
 * <p>
 * Pola pemakaian umum: sebuah komponen UI reusable (mis. dialog pemilih/editor anak, band-box,
 * atau composer bersama) menyimpan referensi {@code OnSaveListener} yang di-<i>set</i> oleh
 * pemanggilnya, lalu memanggil {@link #onSave(Event)} tepat sebelum atau sesudah data disimpan ke
 * database, sehingga pemanggil dapat menyisipkan logika tambahan (validasi lanjutan, refresh
 * tampilan, penutupan dialog, dsb.) tanpa mengubah kode komponen reusable tersebut. Ini adalah
 * varian sederhana dari {@link ais.common.listener.GetTransaksi} yang dipakai pada konteks
 * transaksi medis (SIRS) — {@code OnSaveListener} sendiri tidak terikat modul tertentu dan dapat
 * dipakai di mana saja pola "beri tahu saat simpan" dibutuhkan.
 * </p>
 */
public interface OnSaveListener {

	/**
	 * Dipanggil oleh komponen pemilik saat operasi simpan terjadi.
	 *
	 * @param event event ZK yang memicu aksi simpan (mis. klik tombol)
	 * @return {@code true} bila proses simpan dianggap berhasil/boleh dilanjutkan oleh pemanggil,
	 *         {@code false} bila implementasi ingin membatalkan/menghentikan alur lanjutan
	 * @throws Exception diteruskan apa adanya dari logika penyimpanan implementasi
	 */
	public boolean onSave(Event event) throws Exception;
}
