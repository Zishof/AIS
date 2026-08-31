package ais.common.inventory.controltower;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Kumpulan tipe data (value object) tak-berubah (immutable) yang dipertukarkan lewat
 * {@link ControlTowerReadModelPort} untuk fitur "Control Tower" inventory di AIS — sebuah dasbor
 * pemantauan yang menyajikan metrik (KPI) dan peringatan (alert) inventory secara teragregasi per
 * tenant/lokasi/periode. Kelas ini adalah kelas utilitas murni (final, konstruktor privat, tidak
 * bisa diinstansiasi) yang hanya berfungsi sebagai namespace bagi konstanta status/severity serta
 * lima kelas bersarang tak-berubah: {@link Filter}, {@link MetricDefinition}, {@link Metric},
 * {@link Alert}, dan {@link Snapshot}.
 *
 * <p>
 * Seluruh kelas bersarang di sini mengikuti pola yang sama: konstruktor memvalidasi ketat setiap
 * argumen (melempar {@link IllegalArgumentException} pada nilai tidak valid/kosong), field
 * bersifat {@code final} dan hanya diakses lewat getter, objek {@link Date} disalin
 * (defensive copy) saat masuk maupun keluar agar instance tidak dapat dimutasi dari luar, dan
 * koleksi ({@code List}) dibungkus tak-berubah lewat {@link #immutableCopy(List)}. Pola ini
 * menjadikan seluruh tipe di kelas ini aman dipakai lintas-thread tanpa sinkronisasi tambahan.
 * </p>
 */
public final class ControlTowerTypes {
	/** Status snapshot: data sudah lengkap dan siap ditampilkan/dipakai. */
	public static final String STATUS_READY = "READY";
	/** Status snapshot: data sudah usang (belum diperbarui sesuai jadwal) namun masih tersedia. */
	public static final String STATUS_STALE = "STALE";
	/** Status snapshot: proses pembuatan data gagal. */
	public static final String STATUS_FAILED = "FAILED";
	/** Tingkat keparahan alert: informasi biasa, tidak memerlukan tindakan segera. */
	public static final String SEVERITY_INFO = "INFO";
	/** Tingkat keparahan alert: peringatan, perlu diperhatikan. */
	public static final String SEVERITY_WARNING = "WARNING";
	/** Tingkat keparahan alert: kritis, memerlukan tindakan segera. */
	public static final String SEVERITY_CRITICAL = "CRITICAL";

	/** Kelas utilitas murni — tidak boleh diinstansiasi. */
	private ControlTowerTypes() { }

	/**
	 * Kriteria penyaringan tak-berubah untuk query Control Tower: tenant, lokasi opsional,
	 * rentang tanggal, dan parameter paginasi (limit/offset). Dipakai sebagai parameter di
	 * seluruh method {@link ControlTowerReadModelPort} yang membutuhkan penyaringan data.
	 */
	public static final class Filter {
		private final long tenantId;
		private final Long locationId;
		private final Date fromDate;
		private final Date toDate;
		private final int limit;
		private final int offset;

		/**
		 * Membuat kriteria filter baru, memvalidasi seluruh argumen sebelum menyimpannya.
		 *
		 * @param tenantId   id tenant/institusi, wajib bernilai positif (&gt; 0)
		 * @param locationId id lokasi/gudang untuk mempersempit hasil, boleh {@code null} berarti
		 *                   semua lokasi
		 * @param fromDate   awal periode, wajib diisi dan tidak boleh setelah {@code toDate}
		 * @param toDate     akhir periode, wajib diisi
		 * @param limit      jumlah maksimum baris hasil, wajib antara 1 dan 500
		 * @param offset     jumlah baris yang dilompati (paginasi), wajib &gt;= 0
		 * @throws IllegalArgumentException bila salah satu argumen tidak memenuhi aturan di atas
		 */
		public Filter(long tenantId, Long locationId, Date fromDate, Date toDate, int limit, int offset) {
			if (tenantId <= 0L) throw new IllegalArgumentException("tenantId wajib diisi");
			if (fromDate == null || toDate == null) throw new IllegalArgumentException("periode wajib diisi");
			if (fromDate.after(toDate)) throw new IllegalArgumentException("periode tidak valid");
			if (limit < 1 || limit > 500) throw new IllegalArgumentException("limit harus 1 sampai 500");
			if (offset < 0) throw new IllegalArgumentException("offset tidak boleh negatif");
			this.tenantId = tenantId;
			this.locationId = locationId;
			this.fromDate = new Date(fromDate.getTime());
			this.toDate = new Date(toDate.getTime());
			this.limit = limit;
			this.offset = offset;
		}

		/** @return id tenant/institusi pemilik filter ini */
		public long getTenantId() { return tenantId; }
		/** @return id lokasi/gudang, atau {@code null} bila mencakup semua lokasi */
		public Long getLocationId() { return locationId; }
		/** @return salinan awal periode (defensive copy, aman dimutasi pemanggil) */
		public Date getFromDate() { return new Date(fromDate.getTime()); }
		/** @return salinan akhir periode (defensive copy, aman dimutasi pemanggil) */
		public Date getToDate() { return new Date(toDate.getTime()); }
		/** @return jumlah maksimum baris hasil */
		public int getLimit() { return limit; }
		/** @return jumlah baris yang dilompati untuk paginasi */
		public int getOffset() { return offset; }
		/**
		 * Membentuk kunci string unik yang merepresentasikan seluruh kombinasi kriteria filter
		 * ini, cocok dipakai sebagai kunci cache (mis. kunci pencarian snapshot yang sudah
		 * dihitung untuk kombinasi filter yang identik).
		 *
		 * @return string gabungan seluruh field filter dipisah {@code "|"}
		 */
		public String key() {
			return tenantId + "|" + (locationId == null ? "ALL" : locationId.toString()) + "|"
					+ fromDate.getTime() + "|" + toDate.getTime() + "|" + limit + "|" + offset;
		}
	}

	/**
	 * Definisi statis satu jenis KPI/metrik Control Tower — metadata yang menjelaskan APA
	 * metrik ini (bukan nilainya, lihat {@link Metric} untuk nilai teragregasi aktual).
	 * Dipakai sebagai katalog/referensi agar setiap metrik memiliki pemilik (owner), sumber
	 * data, rute drill-down UI, dan kueri rekonsiliasi yang jelas.
	 */
	public static final class MetricDefinition {
		private final String code;
		private final String label;
		private final String owner;
		private final String source;
		private final String drillDownRoute;
		private final String reconciliationQuery;

		/**
		 * Membuat definisi metrik baru; seluruh parameter wajib diisi (tidak {@code null}/kosong)
		 * dan akan di-trim lewat {@link #required(String, String)}.
		 *
		 * @param code                kode unik metrik
		 * @param label               label tampilan metrik
		 * @param owner               pemilik/penanggung jawab metrik (tim/modul)
		 * @param source              sumber data metrik
		 * @param drillDownRoute      rute UI untuk melihat rincian di balik angka metrik
		 * @param reconciliationQuery kueri rekonsiliasi untuk memverifikasi keakuratan metrik
		 * @throws IllegalArgumentException bila ada parameter yang kosong/{@code null}
		 */
		public MetricDefinition(String code, String label, String owner, String source,
				String drillDownRoute, String reconciliationQuery) {
			this.code = required(code, "kode KPI");
			this.label = required(label, "label KPI");
			this.owner = required(owner, "owner KPI");
			this.source = required(source, "sumber KPI");
			this.drillDownRoute = required(drillDownRoute, "rute drill-down");
			this.reconciliationQuery = required(reconciliationQuery, "kueri rekonsiliasi");
		}

		/** @return kode unik metrik */
		public String getCode() { return code; }
		/** @return label tampilan metrik */
		public String getLabel() { return label; }
		/** @return pemilik/penanggung jawab metrik */
		public String getOwner() { return owner; }
		/** @return sumber data metrik */
		public String getSource() { return source; }
		/** @return rute UI untuk drill-down rincian metrik */
		public String getDrillDownRoute() { return drillDownRoute; }
		/** @return kueri rekonsiliasi metrik */
		public String getReconciliationQuery() { return reconciliationQuery; }
	}

	/**
	 * Nilai teragregasi aktual dari satu {@link MetricDefinition} untuk satu modul tertentu —
	 * pasangan hitung ({@code count}) dan nominal ({@code amount}) hasil agregasi data
	 * inventory pada periode/filter tertentu. Ini adalah "isi" dari metrik, sedangkan
	 * {@link MetricDefinition} adalah "definisinya".
	 */
	public static final class Metric {
		private final String module;
		private final MetricDefinition definition;
		private final long count;
		private final BigDecimal amount;

		/**
		 * Membuat nilai metrik baru.
		 *
		 * @param module     nama modul asal metrik (mis. "inventory", "purchasing")
		 * @param definition definisi metrik terkait, wajib tidak {@code null}
		 * @param count      jumlah hitung hasil agregasi, wajib &gt;= 0
		 * @param amount     nominal hasil agregasi; {@code null} dianggap {@link BigDecimal#ZERO}
		 * @throws IllegalArgumentException bila {@code module} kosong, {@code definition}
		 *                                   {@code null}, atau {@code count} negatif
		 */
		public Metric(String module, MetricDefinition definition, long count, BigDecimal amount) {
			this.module = required(module, "modul KPI");
			if (definition == null) throw new IllegalArgumentException("definisi KPI wajib diisi");
			if (count < 0L) throw new IllegalArgumentException("jumlah KPI tidak boleh negatif");
			this.definition = definition;
			this.count = count;
			this.amount = amount == null ? BigDecimal.ZERO : amount;
		}

		/** @return nama modul asal metrik */
		public String getModule() { return module; }
		/** @return definisi metrik terkait */
		public MetricDefinition getDefinition() { return definition; }
		/** @return jumlah hitung hasil agregasi */
		public long getCount() { return count; }
		/** @return nominal hasil agregasi, tidak pernah {@code null} */
		public BigDecimal getAmount() { return amount; }
	}

	/**
	 * Satu peringatan (alert) Control Tower yang merujuk ke entitas data tertentu (
	 * {@code referenceType}/{@code referenceId}) pada satu modul, lengkap dengan tingkat
	 * keparahan ({@link #SEVERITY_INFO}/{@link #SEVERITY_WARNING}/{@link #SEVERITY_CRITICAL}),
	 * judul, pesan, rute drill-down UI, dan waktu kejadian.
	 */
	public static final class Alert {
		private final String module;
		private final String referenceType;
		private final String referenceId;
		private final String severity;
		private final String title;
		private final String message;
		private final String drillDownRoute;
		private final Date occurredAt;

		/**
		 * Membuat alert baru.
		 *
		 * @param module         nama modul asal alert
		 * @param referenceType  tipe entitas yang menjadi rujukan alert (mis. nama tabel/kelas)
		 * @param referenceId    id entitas yang menjadi rujukan alert
		 * @param severity       tingkat keparahan; harus salah satu dari {@link #SEVERITY_INFO},
		 *                       {@link #SEVERITY_WARNING}, {@link #SEVERITY_CRITICAL}
		 * @param title          judul singkat alert
		 * @param message        pesan detail alert
		 * @param drillDownRoute rute UI untuk melihat rincian di balik alert
		 * @param occurredAt     waktu kejadian yang memicu alert, wajib diisi
		 * @throws IllegalArgumentException bila ada field teks wajib yang kosong, {@code severity}
		 *                                   bukan salah satu nilai yang valid, atau
		 *                                   {@code occurredAt} {@code null}
		 */
		public Alert(String module, String referenceType, String referenceId, String severity,
				String title, String message, String drillDownRoute, Date occurredAt) {
			this.module = required(module, "modul alert");
			this.referenceType = required(referenceType, "tipe referensi alert");
			this.referenceId = required(referenceId, "ID referensi alert");
			if (!SEVERITY_INFO.equals(severity) && !SEVERITY_WARNING.equals(severity)
					&& !SEVERITY_CRITICAL.equals(severity)) throw new IllegalArgumentException("severity tidak valid");
			this.severity = severity;
			this.title = required(title, "judul alert");
			this.message = required(message, "pesan alert");
			this.drillDownRoute = required(drillDownRoute, "rute drill-down alert");
			if (occurredAt == null) throw new IllegalArgumentException("waktu alert wajib diisi");
			this.occurredAt = new Date(occurredAt.getTime());
		}

		/** @return nama modul asal alert */
		public String getModule() { return module; }
		/** @return tipe entitas yang menjadi rujukan alert */
		public String getReferenceType() { return referenceType; }
		/** @return id entitas yang menjadi rujukan alert */
		public String getReferenceId() { return referenceId; }
		/** @return tingkat keparahan alert */
		public String getSeverity() { return severity; }
		/** @return judul singkat alert */
		public String getTitle() { return title; }
		/** @return pesan detail alert */
		public String getMessage() { return message; }
		/** @return rute UI untuk drill-down rincian alert */
		public String getDrillDownRoute() { return drillDownRoute; }
		/** @return salinan waktu kejadian alert (defensive copy) */
		public Date getOccurredAt() { return new Date(occurredAt.getTime()); }
	}

	/**
	 * Kumpulan hasil olahan Control Tower yang sudah "dibekukan" pada satu titik waktu untuk
	 * satu kombinasi filter tertentu ({@code filterKey}, biasanya berasal dari
	 * {@link Filter#key()}) — gabungan status kesiapan data, daftar {@link Metric}, dan daftar
	 * {@link Alert} yang siap ditampilkan tanpa perlu menghitung ulang. Inilah tipe yang
	 * dikembalikan/disimpan lewat {@link ControlTowerReadModelPort#findLatest(Filter)},
	 * {@link ControlTowerReadModelPort#findById(String)}, dan
	 * {@link ControlTowerReadModelPort#save(Snapshot)}.
	 */
	public static final class Snapshot {
		private final String snapshotId;
		private final String filterKey;
		private final String status;
		private final Date generatedAt;
		private final Date watermark;
		private final List<Metric> metrics;
		private final List<Alert> alerts;

		/**
		 * Membuat snapshot baru. Daftar {@code metrics} dan {@code alerts} disalin ke koleksi
		 * tak-berubah lewat {@link ControlTowerTypes#immutableCopy(List)} ({@code null} menjadi
		 * daftar kosong, bukan {@code null}).
		 *
		 * @param snapshotId  id unik snapshot
		 * @param filterKey   kunci filter yang menghasilkan snapshot ini (lihat {@link Filter#key()})
		 * @param status      status snapshot; harus salah satu dari {@link #STATUS_READY},
		 *                    {@link #STATUS_STALE}, {@link #STATUS_FAILED}
		 * @param generatedAt waktu snapshot ini dibuat, wajib diisi
		 * @param watermark   batas waktu data yang tercakup dalam snapshot (data watermark),
		 *                    wajib diisi
		 * @param metrics     daftar metrik hasil agregasi; boleh {@code null} (diperlakukan
		 *                    sebagai kosong)
		 * @param alerts      daftar alert hasil agregasi; boleh {@code null} (diperlakukan
		 *                    sebagai kosong)
		 * @throws IllegalArgumentException bila {@code snapshotId}/{@code filterKey} kosong,
		 *                                   {@code status} tidak valid, atau
		 *                                   {@code generatedAt}/{@code watermark} {@code null}
		 */
		public Snapshot(String snapshotId, String filterKey, String status, Date generatedAt,
				Date watermark, List<Metric> metrics, List<Alert> alerts) {
			this.snapshotId = required(snapshotId, "snapshotId");
			this.filterKey = required(filterKey, "filterKey");
			if (!STATUS_READY.equals(status) && !STATUS_STALE.equals(status)
					&& !STATUS_FAILED.equals(status)) throw new IllegalArgumentException("status snapshot tidak valid");
			if (generatedAt == null || watermark == null) throw new IllegalArgumentException("waktu snapshot wajib diisi");
			this.status = status;
			this.generatedAt = new Date(generatedAt.getTime());
			this.watermark = new Date(watermark.getTime());
			this.metrics = immutableCopy(metrics);
			this.alerts = immutableCopy(alerts);
		}

		/** @return id unik snapshot */
		public String getSnapshotId() { return snapshotId; }
		/** @return kunci filter yang menghasilkan snapshot ini */
		public String getFilterKey() { return filterKey; }
		/** @return status kesiapan snapshot */
		public String getStatus() { return status; }
		/** @return salinan waktu pembuatan snapshot (defensive copy) */
		public Date getGeneratedAt() { return new Date(generatedAt.getTime()); }
		/** @return salinan batas waktu data yang tercakup snapshot (defensive copy) */
		public Date getWatermark() { return new Date(watermark.getTime()); }
		/** @return daftar metrik tak-berubah, tidak pernah {@code null} */
		public List<Metric> getMetrics() { return metrics; }
		/** @return daftar alert tak-berubah, tidak pernah {@code null} */
		public List<Alert> getAlerts() { return alerts; }
	}

	/**
	 * Memvalidasi bahwa {@code value} tidak {@code null}/kosong/hanya whitespace, dan
	 * mengembalikannya dalam bentuk sudah di-trim. Dipakai seluruh konstruktor kelas bersarang
	 * di file ini untuk validasi field teks wajib secara seragam.
	 *
	 * @param value nilai yang divalidasi
	 * @param label nama field yang dipakai pada pesan error, agar pesan galat informatif
	 * @return {@code value} yang sudah di-trim
	 * @throws IllegalArgumentException bila {@code value} {@code null}/kosong/hanya whitespace
	 */
	private static String required(String value, String label) {
		if (value == null || value.trim().length() == 0) throw new IllegalArgumentException(label + " wajib diisi");
		return value.trim();
	}

	/**
	 * Membuat salinan tak-berubah (unmodifiable) dari {@code values}, memperlakukan {@code null}
	 * sebagai daftar kosong alih-alih melempar {@link NullPointerException}. Dipakai agar objek
	 * {@link Snapshot} tidak dapat dimutasi dari luar lewat referensi list yang dibagikan.
	 *
	 * @param <T>    tipe elemen daftar
	 * @param values daftar sumber, boleh {@code null}
	 * @return salinan tak-berubah dari {@code values}, atau daftar kosong bila {@code values}
	 *         {@code null}
	 */
	private static <T> List<T> immutableCopy(List<T> values) {
		if (values == null) return Collections.emptyList();
		return Collections.unmodifiableList(new ArrayList<T>(values));
	}
}
