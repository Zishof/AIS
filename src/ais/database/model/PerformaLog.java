package ais.database.model;

// Catatan performa JVM (thread, memori, GC, deadlock). Polanya meniru ErrorLog,
// hanya saja isinya berupa metrik angka hasil pembacaan java.lang.management.

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

/**
 * Entity penyimpan SATU snapshot performa JVM aplikasi (eCampus) pada satu waktu.
 *
 * <p>
 * Berbeda dengan {@link ErrorLog} yang menyimpan teks error mentah, tabel ini menyimpan
 * metrik performa sebagai kolom angka sehingga dasbor dapat menghitung ringkasan tanpa
 * mengurai teks. Tujuannya: admin bisa melihat kondisi runtime Java (jumlah thread, thread
 * yang sedang WAITING/BLOCKED, jumlah deadlock, pemakaian heap, aktivitas garbage collector)
 * tanpa harus mengambil thread dump manual ke server.
 * </p>
 *
 * <p>
 * Data diisi oleh {@link ais.common.PerformaSnapshotUtil} yang membaca ThreadMXBean,
 * MemoryMXBean, GarbageCollectorMXBean, ClassLoadingMXBean, RuntimeMXBean dan
 * OperatingSystemMXBean. Kolom {@code keterangan} memuat laporan rinci versi teks
 * (termasuk cuplikan thread yang sedang menunggu/terblokir) untuk keperluan analisa/AI.
 * </p>
 *
 * <p>
 * Tabel ini SENGAJA tidak diberi anotasi {@code @Audited} (Envers). PerformaLog bersifat
 * append-only (hanya disisipkan, tidak pernah diubah/diedit), sehingga audit revisi tidak
 * memberi manfaat dan hanya menggandakan jumlah tulis ke schema new_audit. Karena
 * {@code hbm2ddl.auto=update}, tabel {@code public.performa_log} otomatis dibuat saat startup.
 * Jika ingin membuat manual, jalankan DDL berikut:
 * </p>
 *
 * <pre>
 * CREATE TABLE IF NOT EXISTS public.performa_log (
 *   id                    BIGSERIAL PRIMARY KEY,
 *   tanggal_dirubah       TIMESTAMP,
 *   oleh                  VARCHAR(255),
 *   oleh_id               VARCHAR(255),
 *   jumlah_thread         INTEGER,
 *   jumlah_thread_puncak  INTEGER,
 *   jumlah_daemon         INTEGER,
 *   total_thread_dimulai  BIGINT,
 *   jumlah_runnable       INTEGER,
 *   jumlah_waiting        INTEGER,
 *   jumlah_timed_waiting  INTEGER,
 *   jumlah_blocked        INTEGER,
 *   jumlah_new            INTEGER,
 *   jumlah_terminated     INTEGER,
 *   jumlah_deadlock       INTEGER,
 *   heap_dipakai          BIGINT,
 *   heap_maksimum         BIGINT,
 *   non_heap_dipakai      BIGINT,
 *   beban_sistem          DOUBLE PRECISION,
 *   jumlah_prosesor       INTEGER,
 *   jumlah_kelas          INTEGER,
 *   gc_jumlah             BIGINT,
 *   gc_waktu_millis       BIGINT,
 *   uptime_millis         BIGINT,
 *   keterangan            TEXT
 * );
 * </pre>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "public", name = "performa_log")
public class PerformaLog extends GeneralValueObject {

	private static final long serialVersionUID = 7825143902187364512L;

	private Long id;
	private String oleh;
	private String olehId;

	private Integer jumlahThread;
	private Integer jumlahThreadPuncak;
	private Integer jumlahDaemon;
	private Long totalThreadDimulai;

	private Integer jumlahRunnable;
	private Integer jumlahWaiting;
	private Integer jumlahTimedWaiting;
	private Integer jumlahBlocked;
	private Integer jumlahNew;
	private Integer jumlahTerminated;
	private Integer jumlahDeadlock;
	/**
	 * Jumlah thread TERBANYAK yang menumpuk pada SATU lock/objek yang sama (BLOCKED atau
	 * WAITING pada monitor yang sama), DI LUAR worker pool yang idle menunggu tugas. Ini
	 * sinyal kontensi lock NYATA (mis. pool koneksi c3p0 habis → ratusan thread di
	 * BasicResourcePool.awaitAvailable). Dipakai getStatusKesehatan() menggantikan rasio
	 * WAITING/total yang lama (yang salah menghitung worker idle → alarm palsu). Nullable
	 * agar baris lama (sebelum kolom ada) tetap terbaca.
	 */
	private Integer maxThreadSatuLock;

	private Long heapDipakai;
	private Long heapMaksimum;
	private Long nonHeapDipakai;

	private Long oldGenDipakai;
	private Long oldGenMaksimum;
	private Long metaspaceDipakai;
	private Long metaspaceMaksimum;

	private Double bebanSistem;
	private Double prosesCpuPersen;
	private Integer jumlahProsesor;
	private Integer jumlahKelas;

	private String topClass;

	private Long gcJumlah;
	private Long gcWaktuMillis;
	private Long uptimeMillis;

	private String keterangan;

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public PerformaLog() {
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	@Column(name = "jumlah_thread")
	public Integer getJumlahThread() {
		return jumlahThread;
	}

	public void setJumlahThread(Integer jumlahThread) {
		this.jumlahThread = jumlahThread;
	}

	@Column(name = "jumlah_thread_puncak")
	public Integer getJumlahThreadPuncak() {
		return jumlahThreadPuncak;
	}

	public void setJumlahThreadPuncak(Integer jumlahThreadPuncak) {
		this.jumlahThreadPuncak = jumlahThreadPuncak;
	}

	@Column(name = "jumlah_daemon")
	public Integer getJumlahDaemon() {
		return jumlahDaemon;
	}

	public void setJumlahDaemon(Integer jumlahDaemon) {
		this.jumlahDaemon = jumlahDaemon;
	}

	@Column(name = "total_thread_dimulai")
	public Long getTotalThreadDimulai() {
		return totalThreadDimulai;
	}

	public void setTotalThreadDimulai(Long totalThreadDimulai) {
		this.totalThreadDimulai = totalThreadDimulai;
	}

	@Column(name = "jumlah_runnable")
	public Integer getJumlahRunnable() {
		return jumlahRunnable;
	}

	public void setJumlahRunnable(Integer jumlahRunnable) {
		this.jumlahRunnable = jumlahRunnable;
	}

	@Column(name = "jumlah_waiting")
	public Integer getJumlahWaiting() {
		return jumlahWaiting;
	}

	public void setJumlahWaiting(Integer jumlahWaiting) {
		this.jumlahWaiting = jumlahWaiting;
	}

	@Column(name = "jumlah_timed_waiting")
	public Integer getJumlahTimedWaiting() {
		return jumlahTimedWaiting;
	}

	public void setJumlahTimedWaiting(Integer jumlahTimedWaiting) {
		this.jumlahTimedWaiting = jumlahTimedWaiting;
	}

	@Column(name = "jumlah_blocked")
	public Integer getJumlahBlocked() {
		return jumlahBlocked;
	}

	public void setJumlahBlocked(Integer jumlahBlocked) {
		this.jumlahBlocked = jumlahBlocked;
	}

	@Column(name = "jumlah_new")
	public Integer getJumlahNew() {
		return jumlahNew;
	}

	public void setJumlahNew(Integer jumlahNew) {
		this.jumlahNew = jumlahNew;
	}

	@Column(name = "jumlah_terminated")
	public Integer getJumlahTerminated() {
		return jumlahTerminated;
	}

	public void setJumlahTerminated(Integer jumlahTerminated) {
		this.jumlahTerminated = jumlahTerminated;
	}

	@Column(name = "jumlah_deadlock")
	public Integer getJumlahDeadlock() {
		return jumlahDeadlock;
	}

	public void setJumlahDeadlock(Integer jumlahDeadlock) {
		this.jumlahDeadlock = jumlahDeadlock;
	}

	@Column(name = "heap_dipakai")
	public Long getHeapDipakai() {
		return heapDipakai;
	}

	public void setHeapDipakai(Long heapDipakai) {
		this.heapDipakai = heapDipakai;
	}

	@Column(name = "heap_maksimum")
	public Long getHeapMaksimum() {
		return heapMaksimum;
	}

	public void setHeapMaksimum(Long heapMaksimum) {
		this.heapMaksimum = heapMaksimum;
	}

	@Column(name = "non_heap_dipakai")
	public Long getNonHeapDipakai() {
		return nonHeapDipakai;
	}

	public void setNonHeapDipakai(Long nonHeapDipakai) {
		this.nonHeapDipakai = nonHeapDipakai;
	}

	@Column(name = "old_gen_dipakai")
	public Long getOldGenDipakai() {
		return oldGenDipakai;
	}

	public void setOldGenDipakai(Long oldGenDipakai) {
		this.oldGenDipakai = oldGenDipakai;
	}

	@Column(name = "old_gen_maksimum")
	public Long getOldGenMaksimum() {
		return oldGenMaksimum;
	}

	public void setOldGenMaksimum(Long oldGenMaksimum) {
		this.oldGenMaksimum = oldGenMaksimum;
	}

	@Column(name = "metaspace_dipakai")
	public Long getMetaspaceDipakai() {
		return metaspaceDipakai;
	}

	public void setMetaspaceDipakai(Long metaspaceDipakai) {
		this.metaspaceDipakai = metaspaceDipakai;
	}

	@Column(name = "metaspace_maksimum")
	public Long getMetaspaceMaksimum() {
		return metaspaceMaksimum;
	}

	public void setMetaspaceMaksimum(Long metaspaceMaksimum) {
		this.metaspaceMaksimum = metaspaceMaksimum;
	}

	@Column(name = "beban_sistem")
	public Double getBebanSistem() {
		return bebanSistem;
	}

	public void setBebanSistem(Double bebanSistem) {
		this.bebanSistem = bebanSistem;
	}

	@Column(name = "proses_cpu_persen")
	public Double getProsesCpuPersen() {
		return prosesCpuPersen;
	}

	public void setProsesCpuPersen(Double prosesCpuPersen) {
		this.prosesCpuPersen = prosesCpuPersen;
	}

	@Column(name = "top_class", length = 255)
	public String getTopClass() {
		return topClass;
	}

	public void setTopClass(String topClass) {
		this.topClass = topClass;
	}

	@Column(name = "jumlah_prosesor")
	public Integer getJumlahProsesor() {
		return jumlahProsesor;
	}

	public void setJumlahProsesor(Integer jumlahProsesor) {
		this.jumlahProsesor = jumlahProsesor;
	}

	@Column(name = "jumlah_kelas")
	public Integer getJumlahKelas() {
		return jumlahKelas;
	}

	public void setJumlahKelas(Integer jumlahKelas) {
		this.jumlahKelas = jumlahKelas;
	}

	@Column(name = "gc_jumlah")
	public Long getGcJumlah() {
		return gcJumlah;
	}

	public void setGcJumlah(Long gcJumlah) {
		this.gcJumlah = gcJumlah;
	}

	@Column(name = "gc_waktu_millis")
	public Long getGcWaktuMillis() {
		return gcWaktuMillis;
	}

	public void setGcWaktuMillis(Long gcWaktuMillis) {
		this.gcWaktuMillis = gcWaktuMillis;
	}

	@Column(name = "uptime_millis")
	public Long getUptimeMillis() {
		return uptimeMillis;
	}

	public void setUptimeMillis(Long uptimeMillis) {
		this.uptimeMillis = uptimeMillis;
	}

	@Column(name = "keterangan", columnDefinition = "text", nullable = true)
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public String toString() {
		return (id == null ? "" : id.toString()) + "-" + getRingkasan();
	}

	// ===================== Nilai turunan (tidak disimpan) =====================

	private static int nz(Integer value) {
		return value == null ? 0 : value.intValue();
	}

	private static long nz(Long value) {
		return value == null ? 0L : value.longValue();
	}

	/** Persentase pemakaian heap (0..100). Mengembalikan -1 bila batas heap tidak diketahui. */
	@Transient
	public int getPersenHeap() {
		long max = nz(heapMaksimum);
		long used = nz(heapDipakai);
		if (max <= 0L) {
			return -1;
		}
		long persen = (used * 100L) / max;
		if (persen < 0L) {
			persen = 0L;
		}
		if (persen > 100L) {
			persen = 100L;
		}
		return (int) persen;
	}

	/** Persentase pemakaian metaspace (0..100). -1 bila batas tidak diketahui. */
	@Transient
	public int getPersenMetaspace() {
		return persen(metaspaceDipakai, metaspaceMaksimum);
	}

	/** Persentase pemakaian Old Generation (0..100). -1 bila batas tidak diketahui. */
	@Transient
	public int getPersenOldGen() {
		return persen(oldGenDipakai, oldGenMaksimum);
	}

	private static int persen(Long dipakai, Long maksimum) {
		long max = nz(maksimum);
		long used = nz(dipakai);
		if (max <= 0L) {
			return -1;
		}
		long p = (used * 100L) / max;
		if (p < 0L) {
			p = 0L;
		}
		if (p > 100L) {
			p = 100L;
		}
		return (int) p;
	}

	/** Jumlah thread yang sedang "menunggu" (WAITING + TIMED_WAITING + BLOCKED). */
	@Transient
	public int getJumlahMenunggu() {
		return nz(jumlahWaiting) + nz(jumlahTimedWaiting) + nz(jumlahBlocked);
	}

	@Column(name = "max_thread_satu_lock")
	public Integer getMaxThreadSatuLock() {
		return maxThreadSatuLock;
	}

	public void setMaxThreadSatuLock(Integer maxThreadSatuLock) {
		this.maxThreadSatuLock = maxThreadSatuLock;
	}

	/** Ambang jumlah thread yang menumpuk pada SATU lock yang sama (di luar worker idle). */
	private static final int AMBANG_KONTENSI_PERHATIAN = 8;
	private static final int AMBANG_KONTENSI_KRITIS = 50;

	/**
	 * Ambang jumlah thread BLOCKED (rebutan monitor). SATU-DUA thread BLOCKED itu transien &amp;
	 * normal (menunggu sesaat masuk blok synchronized) — bukan indikasi masalah. Masalah nyata =
	 * BANYAK thread BLOCKED (lock convoy). Karena itu 1-2 dianggap Normal.
	 */
	private static final int AMBANG_BLOCKED_PERHATIAN = 3;
	private static final int AMBANG_BLOCKED_KRITIS = 10;

	/**
	 * Status kesehatan sederhana berdasarkan ambang batas default:
	 * <ul>
	 * <li>KRITIS  : ada deadlock, heap/metaspace &gt;= 95%, &gt;= 10 thread BLOCKED, atau &gt;= 50
	 * thread menumpuk pada SATU lock yang sama (pola pool koneksi habis / lock global macet).</li>
	 * <li>PERHATIAN: heap/metaspace &gt;= 85%, thread &gt; 800, &gt;= 3 thread BLOCKED, atau &gt;= 8
	 * thread berebut satu lock yang sama.</li>
	 * <li>NORMAL  : selain kondisi di atas (termasuk 1-2 thread BLOCKED yang transien).</li>
	 * </ul>
	 *
	 * <p><b>Catatan penting.</b> (1) Versi lama memakai rasio {@code menunggu/total >= 70%} dengan
	 * {@code menunggu = WAITING + TIMED_WAITING + BLOCKED}. Itu SALAH: pada server sehat yang tidak
	 * jenuh, MAYORITAS thread adalah worker pool yang IDLE menunggu tugas ({@code TaskQueue.take}),
	 * sehingga rasio itu SELALU &ge; 70% dan "Perhatian" menyala di SETIAP snapshot (alarm palsu —
	 * terbukti pada 204/204 snapshot). Kini status memakai {@link #getMaxThreadSatuLock()}, yaitu
	 * jumlah thread yang berebut lock/objek yang SAMA di luar worker idle — sinyal kontensi NYATA
	 * yang tetap menjaring insiden seperti pool koneksi c3p0 habis (ratusan thread di
	 * {@code BasicResourcePool.awaitAvailable}). (2) {@code jumlahBlocked > 0} juga terlalu sensitif:
	 * 1 thread BLOCKED transien membuat status "Kritis" palsu (terbukti pada snapshot ID 523 &amp;
	 * 101, 09-07-2026, yang selain itu SEHAT: heap 5-14%, tanpa deadlock, GC dalam target). Maka
	 * BLOCKED kini memakai ambang ({@link #AMBANG_BLOCKED_PERHATIAN}/{@link #AMBANG_BLOCKED_KRITIS});
	 * convoy lock nyata tetap terjaring.</p>
	 */
	@Transient
	public String getStatusKesehatan() {
		int heap = getPersenHeap();
		int meta = getPersenMetaspace();
		int total = nz(jumlahThread);
		int kontensi = nz(maxThreadSatuLock);
		int blocked = nz(jumlahBlocked);

		if (nz(jumlahDeadlock) > 0 || (heap >= 0 && heap >= 95) || (meta >= 0 && meta >= 95)
				|| blocked >= AMBANG_BLOCKED_KRITIS || kontensi >= AMBANG_KONTENSI_KRITIS) {
			return "Kritis";
		}
		if ((heap >= 0 && heap >= 85) || (meta >= 0 && meta >= 85) || total > 800
				|| blocked >= AMBANG_BLOCKED_PERHATIAN || kontensi >= AMBANG_KONTENSI_PERHATIAN) {
			return "Perhatian";
		}
		return "Normal";
	}

	/** Warna heksadesimal untuk badge status (hijau/kuning/merah). */
	@Transient
	public String getStatusWarna() {
		String status = getStatusKesehatan();
		if ("Kritis".equals(status)) {
			return "#dc2626";
		}
		if ("Perhatian".equals(status)) {
			return "#d97706";
		}
		return "#16a34a";
	}

	/** Ringkasan satu baris yang mudah dibaca untuk daftar/dasbor. */
	@Transient
	public String getRingkasan() {
		StringBuilder builder = new StringBuilder(160);
		builder.append("Thread ").append(nz(jumlahThread));
		builder.append(" (menunggu ").append(getJumlahMenunggu()).append(")");
		int heap = getPersenHeap();
		if (heap >= 0) {
			builder.append(", Heap ").append(heap).append("%");
		}
		if (nz(jumlahDeadlock) > 0) {
			builder.append(", DEADLOCK ").append(nz(jumlahDeadlock));
		}
		builder.append(" - ").append(getStatusKesehatan());
		return builder.toString();
	}
}
