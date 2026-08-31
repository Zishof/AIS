package ais.common.inventory.controltower;

import java.util.List;

import ais.common.inventory.controltower.ControlTowerTypes.Alert;
import ais.common.inventory.controltower.ControlTowerTypes.Filter;
import ais.common.inventory.controltower.ControlTowerTypes.Metric;
import ais.common.inventory.controltower.ControlTowerTypes.Snapshot;

/**
 * Port (dalam gaya arsitektur hexagonal/ports-and-adapters) untuk model baca (read model) fitur
 * "Control Tower" inventory — titik abstraksi yang memisahkan logika bisnis/tampilan Control Tower
 * dari mekanisme penyimpanan/pengambilan data aktualnya (database, cache, layanan eksternal, dsb).
 * Kode pemanggil bergantung hanya pada interface ini; implementasi konkretnya (adapter) yang
 * memutuskan bagaimana data {@link Snapshot}, {@link Metric}, dan {@link Alert} sesungguhnya
 * diambil atau disimpan.
 *
 * <p>
 * Seluruh tipe data yang dipertukarkan (({@link Filter}, {@link Snapshot}, {@link Metric},
 * {@link Alert}) didefinisikan sebagai tipe bersarang pada {@link ControlTowerTypes}, sehingga
 * port ini murni mendeklarasikan kontrak operasi tanpa membawa detail struktur data — lihat
 * {@link ControlTowerTypes} untuk definisi field masing-masing tipe.
 * </p>
 */
public interface ControlTowerReadModelPort {
	/**
	 * Mengambil snapshot data Control Tower paling akhir/terbaru yang cocok dengan kriteria
	 * {@code filter} yang diberikan.
	 *
	 * @param filter kriteria penyaringan (mis. rentang waktu, lokasi/gudang, kategori)
	 * @return snapshot terbaru yang sesuai filter, atau {@code null} bila tidak ada data
	 */
	Snapshot findLatest(Filter filter);

	/**
	 * Mengambil satu snapshot spesifik berdasarkan id-nya.
	 *
	 * @param snapshotId id unik snapshot yang dicari
	 * @return snapshot dengan id tersebut, atau {@code null} bila tidak ditemukan
	 */
	Snapshot findById(String snapshotId);

	/**
	 * Menghitung/mengumpulkan daftar metrik agregat Control Tower sesuai kriteria {@code filter}.
	 *
	 * @param filter kriteria penyaringan untuk agregasi metrik
	 * @return daftar {@link Metric} hasil agregasi; tidak pernah {@code null}, boleh kosong
	 */
	List<Metric> aggregateMetrics(Filter filter);

	/**
	 * Menghitung/mengumpulkan daftar peringatan (alert) Control Tower sesuai kriteria
	 * {@code filter}.
	 *
	 * @param filter kriteria penyaringan untuk agregasi alert
	 * @return daftar {@link Alert} hasil agregasi; tidak pernah {@code null}, boleh kosong
	 */
	List<Alert> aggregateAlerts(Filter filter);

	/**
	 * Menyimpan (membuat atau memperbarui) satu snapshot data Control Tower ke penyimpanan yang
	 * mendasarinya.
	 *
	 * @param snapshot snapshot yang akan disimpan
	 */
	void save(Snapshot snapshot);
}
