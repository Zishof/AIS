package ais.common.listener;

import org.zkoss.zk.ui.event.Event;

/**
 * Kontrak callback yang dipanggil pada saat operasi "simpan" (save) terjadi di layar ZK,
 * memungkinkan kode pemanggil menyisipkan logika kustom sebelum/selama proses penyimpanan tanpa
 * harus mewarisi atau memodifikasi kelas controller ZK yang menanganinya secara langsung.
 *
 * <p>
 * Pola ini umum dipakai di AIS untuk memisahkan logika penyimpanan generik (di controller/handler
 * bersama) dari logika spesifik per layar/entitas — implementasi konkret {@code OnSave} disuntikkan
 * ke komponen bersama tersebut sebagai hook.
 * </p>
 */
public interface OnSave {
	/**
	 * Dipanggil saat aksi simpan dijalankan, dengan event ZK yang memicunya.
	 *
	 * @param event event ZK yang memicu aksi simpan (mis. klik tombol simpan)
	 * @return {@code true} bila proses simpan dianggap berhasil/boleh dilanjutkan oleh pemanggil,
	 *         {@code false} bila implementasi ingin membatalkan/menghentikan alur simpan
	 * @throws Exception diteruskan apa adanya bila terjadi kegagalan selama proses simpan
	 *                    kustom (mis. validasi gagal, error database)
	 */
	public boolean onSave(Event event) throws Exception;
}
