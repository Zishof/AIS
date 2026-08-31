package ais.common.inventory.controltower;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ais.common.inventory.controltower.ControlTowerTypes.Alert;
import ais.common.inventory.controltower.ControlTowerTypes.Filter;
import ais.common.inventory.controltower.ControlTowerTypes.Metric;
import ais.common.inventory.controltower.ControlTowerTypes.Snapshot;

/**
 * Lapisan layanan (service layer) untuk fitur "Control Tower" modul inventori: dasbor pemantauan
 * berbasis snapshot yang menampilkan metrik (KPI) dan peringatan (alert) inventori terkini, dengan
 * dukungan ekspor laporan. Kelas ini bertindak sebagai orkestrator murni yang tidak mengakses
 * penyimpanan data secara langsung — seluruh akses data didelegasikan ke abstraksi
 * {@link ControlTowerReadModelPort} yang disuntikkan lewat konstruktor (pola <i>ports and
 * adapters</i>/hexagonal architecture), sehingga kelas ini dapat diuji tanpa database sungguhan dan
 * implementasi penyimpanan dapat diganti tanpa mengubah logika orkestrasi di sini.
 *
 * <h2>Konsep snapshot</h2>
 * <p>
 * Data dasbor control tower disajikan dalam bentuk {@link Snapshot} — potret data pada satu titik
 * waktu, berisi status ({@code STALE} untuk data lampau/belum ada, {@code READY} untuk data segar
 * siap dipakai), daftar {@link Metric} (KPI), dan daftar {@link Alert}. Pendekatan berbasis snapshot
 * ini memisahkan proses agregasi data (yang berpotensi mahal/lambat) dari proses penyajian data ke
 * pengguna: {@link #refresh(Filter, Date)} melakukan agregasi ulang dan menyimpan snapshot baru,
 * sementara {@link #loadInitial(Filter, Date)} hanya membaca snapshot terakhir yang tersimpan tanpa
 * memicu agregasi baru — pola umum untuk dasbor yang datanya tidak perlu selalu real-time namun
 * tetap harus responsif saat pertama dibuka.
 * </p>
 *
 * <h2>Kontrak validasi</h2>
 * <p>
 * Seluruh method publik menerapkan validasi input yang ketat dan melempar
 * {@link IllegalArgumentException}/{@link IllegalStateException} sedini mungkin (fail-fast) alih-
 * alih membiarkan data tidak valid merambat ke lapisan penyajian: {@code filter}/{@code now} wajib
 * diisi ({@link #validate(Filter, Date)}), metrik hasil agregasi tidak boleh mengandung entri
 * {@code null} maupun kode KPI duplikat ({@link #validateMetrics(List)} — duplikasi kode metrik
 * akan membuat dasbor menampilkan dua kartu KPI dengan label sama namun sumber berbeda, sehingga
 * sengaja ditolak sebagai kondisi error), dan snapshot yang akan diekspor
 * ({@link #loadForExport(String)}) harus berstatus {@code READY} (snapshot yang masih {@code STALE}
 * belum layak diekspor sebagai laporan resmi).
 * </p>
 */
public final class ControlTowerService {
	/** Abstraksi akses data (baca/tulis snapshot dan agregasi metrik/alert) — satu-satunya jalur kelas ini berinteraksi dengan penyimpanan data. */
	private final ControlTowerReadModelPort readModel;

	/**
	 * Membuat layanan control tower baru dengan implementasi akses data yang diberikan.
	 *
	 * @param readModel implementasi {@link ControlTowerReadModelPort} yang menyediakan operasi baca/
	 *                  tulis snapshot dan agregasi data; wajib diisi
	 * @throws IllegalArgumentException bila {@code readModel} bernilai {@code null}
	 */
	public ControlTowerService(ControlTowerReadModelPort readModel) {
		if (readModel == null) throw new IllegalArgumentException("readModel wajib diisi");
		this.readModel = readModel;
	}

	/**
	 * Memuat snapshot terakhir yang tersimpan untuk {@code filter} tertentu, tanpa memicu agregasi
	 * data baru — dipakai saat dasbor pertama kali dibuka agar tampilan langsung terisi dengan data
	 * (walau mungkin sudah agak lampau) tanpa menunggu proses agregasi selesai. Bila belum ada
	 * snapshot tersimpan sama sekali untuk filter tersebut, dibuatkan snapshot kosong sintetis
	 * berstatus {@code STALE} (id {@code "EMPTY-" + waktu}, metrik dan alert {@code null}) sebagai
	 * placeholder alih-alih mengembalikan {@code null}.
	 *
	 * @param filter kriteria/cakupan data dasbor (mis. tenant, rentang, kategori)
	 * @param now    waktu proses saat ini, dipakai untuk membangun id snapshot placeholder bila
	 *               diperlukan
	 * @return snapshot terakhir tersimpan, atau snapshot placeholder {@code STALE} bila belum ada
	 * @throws IllegalArgumentException bila {@code filter} atau {@code now} bernilai {@code null}
	 */
	public Snapshot loadInitial(Filter filter, Date now) {
		validate(filter, now);
		Snapshot snapshot = readModel.findLatest(filter);
		if (snapshot != null) return snapshot;
		return new Snapshot("EMPTY-" + now.getTime(), filter.key(), ControlTowerTypes.STATUS_STALE,
				now, new Date(0L), null, null);
	}

	/**
	 * Menjalankan agregasi data terbaru dan menyimpannya sebagai snapshot baru berstatus
	 * {@code READY}. Mengambil metrik ({@link ControlTowerReadModelPort#aggregateMetrics(Filter)})
	 * dan alert ({@link ControlTowerReadModelPort#aggregateAlerts(Filter)}, dipotong ke
	 * {@link Filter#getLimit()} alert pertama) dari sumber data, menormalkan hasil {@code null}
	 * menjadi daftar kosong ({@link #safeMetrics}/{@link #safeAlerts}), memvalidasi tidak ada metrik
	 * {@code null}/kode duplikat ({@link #validateMetrics(List)}), lalu membangun dan menyimpan
	 * {@link Snapshot} baru dengan id unik dari {@link #buildSnapshotId(Filter, Date)}.
	 *
	 * @param filter kriteria/cakupan data yang akan diagregasi ulang
	 * @param now    waktu proses saat ini, dicatat sebagai waktu pembaruan snapshot dan dipakai
	 *               membangun id snapshot
	 * @return snapshot baru berstatus {@code READY} yang sudah tersimpan
	 * @throws IllegalArgumentException bila {@code filter}/{@code now} {@code null}, atau hasil
	 *                                   agregasi metrik mengandung entri {@code null}/kode KPI
	 *                                   duplikat
	 */
	public Snapshot refresh(Filter filter, Date now) {
		validate(filter, now);
		List<Metric> metrics = safeMetrics(readModel.aggregateMetrics(filter));
		List<Alert> alerts = safeAlerts(readModel.aggregateAlerts(filter), filter.getLimit());
		validateMetrics(metrics);
		Snapshot snapshot = new Snapshot(buildSnapshotId(filter, now), filter.key(),
				ControlTowerTypes.STATUS_READY, now, now, metrics, alerts);
		readModel.save(snapshot);
		return snapshot;
	}

	/**
	 * Memuat satu snapshot spesifik berdasarkan id untuk keperluan ekspor laporan, dengan validasi
	 * bahwa snapshot tersebut sudah berstatus {@code READY} (bukan {@code STALE}/placeholder) —
	 * mencegah pembuatan laporan ekspor dari data yang belum sempat diagregasi dengan benar.
	 *
	 * @param snapshotId id snapshot yang akan diekspor; spasi di awal/akhir dipangkas sebelum dicari
	 * @return snapshot berstatus {@code READY} yang cocok dengan {@code snapshotId}
	 * @throws IllegalArgumentException bila {@code snapshotId} kosong/{@code null}
	 * @throws IllegalStateException    bila snapshot tidak ditemukan, atau ditemukan tetapi
	 *                                   statusnya belum {@code READY}
	 */
	public Snapshot loadForExport(String snapshotId) {
		if (snapshotId == null || snapshotId.trim().length() == 0)
			throw new IllegalArgumentException("snapshotId wajib diisi");
		Snapshot snapshot = readModel.findById(snapshotId.trim());
		if (snapshot == null) throw new IllegalStateException("snapshot laporan tidak ditemukan");
		if (!ControlTowerTypes.STATUS_READY.equals(snapshot.getStatus()))
			throw new IllegalStateException("snapshot laporan belum siap diekspor");
		return snapshot;
	}

	/**
	 * Memvalidasi bahwa parameter wajib {@code filter} dan {@code now} terisi, dipanggil di awal
	 * {@link #loadInitial(Filter, Date)} dan {@link #refresh(Filter, Date)}.
	 *
	 * @param filter filter yang divalidasi
	 * @param now    waktu proses yang divalidasi
	 * @throws IllegalArgumentException bila salah satu parameter {@code null}
	 */
	private static void validate(Filter filter, Date now) {
		if (filter == null) throw new IllegalArgumentException("filter wajib diisi");
		if (now == null) throw new IllegalArgumentException("waktu proses wajib diisi");
	}

	/**
	 * Menormalkan hasil agregasi metrik: mengembalikan salinan baru dari {@code metrics}, atau
	 * daftar kosong bila {@code metrics} bernilai {@code null} — mencegah {@link NullPointerException}
	 * di pemrosesan/penyajian berikutnya.
	 *
	 * @param metrics daftar metrik mentah dari {@link ControlTowerReadModelPort}, boleh {@code null}
	 * @return salinan daftar metrik yang tidak pernah {@code null}
	 */
	private static List<Metric> safeMetrics(List<Metric> metrics) {
		return metrics == null ? new ArrayList<Metric>() : new ArrayList<Metric>(metrics);
	}

	/**
	 * Menormalkan hasil agregasi alert sekaligus membatasinya ke {@code limit} entri pertama —
	 * mencegah dasbor kebanjiran alert dalam jumlah tak terbatas.
	 *
	 * @param alerts daftar alert mentah dari {@link ControlTowerReadModelPort}, boleh {@code null}
	 * @param limit  jumlah maksimum alert yang disertakan pada snapshot
	 * @return daftar alert yang tidak pernah {@code null}, dipotong maksimal {@code limit} entri
	 */
	private static List<Alert> safeAlerts(List<Alert> alerts, int limit) {
		List<Alert> result = new ArrayList<Alert>();
		if (alerts == null) return result;
		for (int i = 0; i < alerts.size() && result.size() < limit; i++) result.add(alerts.get(i));
		return result;
	}

	/**
	 * Memvalidasi integritas daftar metrik hasil agregasi sebelum disimpan sebagai bagian snapshot:
	 * tidak boleh ada entri {@code null}, dan setiap {@link Metric} harus memiliki kode definisi KPI
	 * ({@code metric.getDefinition().getCode()}) yang unik dalam satu snapshot.
	 *
	 * @param metrics daftar metrik yang akan divalidasi (diasumsikan sudah dinormalkan lewat
	 *                {@link #safeMetrics(List)}, tidak {@code null})
	 * @throws IllegalArgumentException bila ada metrik {@code null} atau kode KPI duplikat ditemukan
	 */
	private static void validateMetrics(List<Metric> metrics) {
		Set<String> codes = new HashSet<String>();
		for (Metric metric : metrics) {
			if (metric == null) throw new IllegalArgumentException("KPI tidak boleh null");
			String code = metric.getDefinition().getCode();
			if (!codes.add(code)) throw new IllegalArgumentException("kode KPI duplikat: " + code);
		}
	}

	/**
	 * Membangun id unik untuk snapshot baru dari kombinasi id tenant pada {@code filter} dan
	 * timestamp {@code now}, dengan format {@code "CT-" + tenantId + "-" + epochMillis}.
	 *
	 * @param filter filter yang menyediakan {@code tenantId}
	 * @param now    waktu proses yang dipakai sebagai bagian akhir id
	 * @return id snapshot unik siap pakai
	 */
	private static String buildSnapshotId(Filter filter, Date now) {
		return "CT-" + filter.getTenantId() + "-" + now.getTime();
	}
}
