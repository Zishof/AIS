package ais.common.listener;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Button;

import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.TransaksiMedis;

/**
 * Kontrak yang harus dipenuhi composer/controller ZK pada modul SIRS (Sistem Informasi Rumah
 * Sakit) yang menangani entri <b>transaksi medis</b> (mis. layar tindakan/pemberian resep di
 * suatu kelas perawatan/lokasi), agar komponen bersama (dialog pemilih resep, band-box, dsb.)
 * dapat berinteraksi dengan layar induknya melalui satu antarmuka seragam tanpa perlu mengetahui
 * implementasi konkret layar tersebut.
 *
 * <p>
 * Method-method getter ({@link #getTransaksi()}, {@link #getLokasi()},
 * {@link #getKelasPerawatan()}, {@link #getSumber()}) menyediakan konteks data yang sedang
 * dikerjakan pengguna pada layar induk, sedangkan getter komponen UI ({@link #getAdd()},
 * {@link #getSimpan()}, {@link #getResep()}) memberi akses ke tombol/band-box tertentu agar
 * komponen anak dapat mengaktifkan/menonaktifkan atau memicu aksi pada elemen tersebut secara
 * terprogram. {@link #onSave(Event)} berfungsi sama seperti {@link ais.common.OnSaveListener},
 * yaitu dipanggil balik saat operasi simpan terjadi pada layar yang mengimplementasikan
 * antarmuka ini.
 * </p>
 */
public interface GetTransaksi {
	/** @return entitas {@link TransaksiMedis} yang sedang dikerjakan pada layar induk. */
	public TransaksiMedis getTransaksi();

	/** @return {@link Lokasi} (aset/ruangan) tempat transaksi medis berlangsung. */
	public Lokasi getLokasi();

	/** @return {@link KelasPerawatan} yang berlaku untuk transaksi medis saat ini. */
	public KelasPerawatan getKelasPerawatan();

	/**
	 * Dipanggil balik oleh komponen anak saat operasi simpan terjadi pada layar induk.
	 *
	 * @param event event ZK yang memicu aksi simpan
	 * @return {@code true} bila simpan berhasil/boleh dilanjutkan, {@code false} bila dibatalkan
	 * @throws Exception diteruskan apa adanya dari logika penyimpanan implementasi
	 */
	public boolean onSave(Event event) throws Exception;

	/** @return penanda sumber/asal entri transaksi (mis. kode ruangan/instalasi pemanggil). */
	public String getSumber();

	/** @return tombol "Tambah" pada layar induk. */
	public Button getAdd();

	/** @return tombol "Simpan" pada layar induk. */
	public Button getSimpan();

	/** @return band-box pemilihan resep pada layar induk. */
	public Bandbox getResep();
}
