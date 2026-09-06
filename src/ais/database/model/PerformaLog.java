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
 * <p>
 * <b>Catatan keamanan/privasi (ditinjau setelah dokumentasi {@link LogHostToHost}, yang
 * ternyata menyimpan payload request mentah).</b> Berbeda dengan {@code LogHostToHost}, entity
 * ini TIDAK menyimpan payload permintaan HTTP, parameter form, header, maupun data pribadi
 * pengguna aplikasi (nama, NIS/NIM, nomor rekening, dsb.) -- seluruh kolom numeriknya berasal
 * dari pembacaan {@code java.lang.management} (state JVM), bukan dari input pengguna. Satu-
 * satunya kolom teks bebas adalah {@link #keterangan} dan {@link #topClass}: keduanya dapat
 * memuat nama thread, nama kelas, dan cuplikan stack trace milik proses SERVER (bukan milik
 * pengguna), yang tetap merupakan informasi internal infrastruktur dan sebaiknya hanya
 * ditampilkan pada dasbor admin/operator, bukan pada tampilan yang dapat diakses pengguna
 * biasa. {@link #oleh}/{@link #olehId} menyimpan identitas SIAPA yang memicu snapshot, bukan
 * data korban/subjek -- risikonya rendah. Tabel ini juga tidak memiliki kolom tenant/satuan
 * kerja sama sekali; ini disengaja karena metrik JVM berlaku untuk satu proses aplikasi secara
 * keseluruhan, bukan data yang dimiliki satu tenant, sehingga tidak ada "kebocoran lintas
 * tenant" yang relevan di sini.
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

	/**
	 * Penanda versi serialisasi Java untuk {@code PerformaLog}. Tidak berpengaruh pada skema
	 * tabel; hanya menjaga kompatibilitas objek yang sempat diserialisasi (mis. disimpan di
	 * sesi HTTP atau cache) bila kelas ini diubah di kemudian hari.
	 */
	private static final long serialVersionUID = 7825143902187364512L;

	/** Id baris (primary key {@code public.performa_log.id}), diisi otomatis oleh basis data. */
	private Long id;
	/**
	 * Nama/identitas pengguna atau proses yang memicu pengambilan snapshot ini, sebagaimana
	 * diisi {@link ais.common.PerformaSnapshotUtil}. Boleh {@code null}; bila diisi lewat
	 * {@link #setOleh(String)}, teks kosong/hanya-spasi diabaikan diam-diam.
	 */
	private String oleh;
	/**
	 * Id pengguna/proses pemicu snapshot, pasangan dari {@link #oleh}. Boleh {@code null};
	 * bila diisi lewat {@link #setOlehId(String)}, teks kosong/hanya-spasi diabaikan diam-diam.
	 */
	private String olehId;

	/** Jumlah thread JVM yang hidup pada saat snapshot diambil ({@code ThreadMXBean}). */
	private Integer jumlahThread;
	/** Jumlah thread hidup TERBANYAK yang pernah tercatat sejak JVM dimulai (nilai puncak). */
	private Integer jumlahThreadPuncak;
	/** Jumlah thread daemon yang hidup pada saat snapshot diambil. */
	private Integer jumlahDaemon;
	/** Total kumulatif thread yang pernah dimulai sejak JVM hidup (bukan yang masih hidup). */
	private Long totalThreadDimulai;

	/** Jumlah thread berstatus {@code RUNNABLE} (sedang benar-benar berjalan/siap jalan). */
	private Integer jumlahRunnable;
	/** Jumlah thread berstatus {@code WAITING} (menunggu tanpa batas waktu, mis. worker idle). */
	private Integer jumlahWaiting;
	/** Jumlah thread berstatus {@code TIMED_WAITING} (menunggu dengan batas waktu/timeout). */
	private Integer jumlahTimedWaiting;
	/**
	 * Jumlah thread berstatus {@code BLOCKED} (menunggu monitor/lock milik thread lain).
	 * Dipakai {@link #getStatusKesehatan()} sebagai salah satu sinyal kontensi; lihat
	 * {@link #AMBANG_BLOCKED_PERHATIAN}/{@link #AMBANG_BLOCKED_KRITIS} untuk ambangnya.
	 */
	private Integer jumlahBlocked;
	/** Jumlah thread berstatus {@code NEW} (dibuat tetapi belum dijalankan). */
	private Integer jumlahNew;
	/** Jumlah thread berstatus {@code TERMINATED} (sudah selesai berjalan). */
	private Integer jumlahTerminated;
	/**
	 * Jumlah deadlock yang terdeteksi ({@code ThreadMXBean.findDeadlockedThreads()}). Nilai
	 * &gt; 0 langsung membuat {@link #getStatusKesehatan()} melaporkan "Kritis".
	 */
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

	/** Bytes heap yang terpakai saat snapshot diambil ({@code MemoryMXBean.getHeapMemoryUsage()}). */
	private Long heapDipakai;
	/** Batas maksimum bytes heap yang boleh dipakai JVM; dasar perhitungan {@link #getPersenHeap()}. */
	private Long heapMaksimum;
	/** Bytes memori non-heap yang terpakai (mis. metaspace, code cache) saat snapshot diambil. */
	private Long nonHeapDipakai;

	/** Bytes yang terpakai pada Old Generation heap saat snapshot diambil (bila tersedia). */
	private Long oldGenDipakai;
	/** Batas maksimum bytes Old Generation; dasar perhitungan {@link #getPersenOldGen()}. */
	private Long oldGenMaksimum;
	/** Bytes metaspace yang terpakai saat snapshot diambil. */
	private Long metaspaceDipakai;
	/** Batas maksimum bytes metaspace; dasar perhitungan {@link #getPersenMetaspace()}. */
	private Long metaspaceMaksimum;

	/** Beban sistem operasi rata-rata ({@code OperatingSystemMXBean.getSystemLoadAverage()}). */
	private Double bebanSistem;
	/** Persentase CPU yang dipakai proses JVM ini saja, bila tersedia dari MXBean vendor. */
	private Double prosesCpuPersen;
	/** Jumlah prosesor/core yang tersedia bagi JVM ({@code Runtime.availableProcessors()}). */
	private Integer jumlahProsesor;
	/** Jumlah kelas yang sedang dimuat di JVM ({@code ClassLoadingMXBean}). */
	private Integer jumlahKelas;

	/**
	 * Nama thread/kelas yang paling banyak berkontribusi pada kontensi pada saat snapshot
	 * diambil (mis. thread yang paling sering muncul memegang lock yang diperebutkan). Teks
	 * bebas yang diisi {@link ais.common.PerformaSnapshotUtil}; boleh {@code null}.
	 */
	private String topClass;

	/** Jumlah total siklus garbage collection yang sudah berjalan sejak JVM dimulai. */
	private Long gcJumlah;
	/** Total waktu (milidetik) yang dihabiskan seluruh siklus garbage collection. */
	private Long gcWaktuMillis;
	/** Lama JVM sudah berjalan (milidetik) sejak dimulai ({@code RuntimeMXBean.getUptime()}). */
	private Long uptimeMillis;

	/**
	 * Laporan performa dalam bentuk teks rinci, termasuk cuplikan thread yang sedang
	 * menunggu/terblokir. Dipakai untuk analisa manual atau oleh alat bantu AI. <b>Berpotensi
	 * memuat nama thread, nama kelas, dan cuplikan stack trace milik proses server</b> — bukan
	 * data pribadi pengguna aplikasi, tetapi tetap informasi internal server yang sebaiknya
	 * hanya terlihat oleh admin/operator, bukan disebarluaskan ke pengguna biasa. Tabel ini
	 * tidak memiliki kolom tenant/satuan kerja karena metrik JVM memang bersifat satu proses
	 * untuk seluruh aplikasi, bukan data milik satu tenant.
	 */
	private String keterangan;

	/**
	 * Waktu snapshot ini dibuat/diubah. Diinisialisasi ke waktu saat ini pada deklarasi field
	 * (bukan di constructor), sehingga nilainya sudah terisi begitu objek dibuat dengan
	 * {@code new PerformaLog()}, sebelum {@link #onUpdate()} sempat berjalan.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Constructor kosong; seluruh field diisi lewat setter oleh {@link ais.common.PerformaSnapshotUtil}. */
	public PerformaLog() {
	}

	/**
	 * Callback JPA {@code @PreUpdate}. Baris {@code performa_log} bersifat append-only (lihat
	 * catatan pada Javadoc kelas), sehingga dalam praktiknya method ini nyaris tidak pernah
	 * terpicu — hanya berjaga-jaga bila suatu saat ada kode yang mengubah baris yang sudah
	 * tersimpan.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * @return id baris ini; {@code null} sebelum baris pernah disimpan (nilai dibangkitkan
	 *         basis data lewat {@code IDENTITY}, kolom tidak {@code insertable})
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id id baris; dalam praktiknya tidak berpengaruh saat insert karena kolom
	 *           {@code id} bertanda {@code insertable = false} (nilai selalu dari basis data)
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return id pengguna/proses pemicu snapshot ini, lihat {@link #olehId} */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * @param olehId id pemicu snapshot; teks {@code null}, kosong, atau hanya-spasi diabaikan
	 *               diam-diam sehingga nilai lama (bila ada) tetap dipertahankan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * @param oleh nama pemicu snapshot; teks {@code null}, kosong, atau hanya-spasi diabaikan
	 *             diam-diam sehingga nilai lama (bila ada) tetap dipertahankan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pengguna/proses pemicu snapshot ini, lihat {@link #oleh} */
	public String getOleh() {
		return oleh;
	}

	/** @param tanggal_dirubah waktu snapshot ini dibuat/diubah */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu snapshot ini dibuat/diubah */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return {@link #jumlahThread} */
	@Column(name = "jumlah_thread")
	public Integer getJumlahThread() {
		return jumlahThread;
	}

	/** @param jumlahThread nilai baru untuk {@link #jumlahThread} */
	public void setJumlahThread(Integer jumlahThread) {
		this.jumlahThread = jumlahThread;
	}

	/** @return {@link #jumlahThreadPuncak} */
	@Column(name = "jumlah_thread_puncak")
	public Integer getJumlahThreadPuncak() {
		return jumlahThreadPuncak;
	}

	/** @param jumlahThreadPuncak nilai baru untuk {@link #jumlahThreadPuncak} */
	public void setJumlahThreadPuncak(Integer jumlahThreadPuncak) {
		this.jumlahThreadPuncak = jumlahThreadPuncak;
	}

	/** @return {@link #jumlahDaemon} */
	@Column(name = "jumlah_daemon")
	public Integer getJumlahDaemon() {
		return jumlahDaemon;
	}

	/** @param jumlahDaemon nilai baru untuk {@link #jumlahDaemon} */
	public void setJumlahDaemon(Integer jumlahDaemon) {
		this.jumlahDaemon = jumlahDaemon;
	}

	/** @return {@link #totalThreadDimulai} */
	@Column(name = "total_thread_dimulai")
	public Long getTotalThreadDimulai() {
		return totalThreadDimulai;
	}

	/** @param totalThreadDimulai nilai baru untuk {@link #totalThreadDimulai} */
	public void setTotalThreadDimulai(Long totalThreadDimulai) {
		this.totalThreadDimulai = totalThreadDimulai;
	}

	/** @return {@link #jumlahRunnable} */
	@Column(name = "jumlah_runnable")
	public Integer getJumlahRunnable() {
		return jumlahRunnable;
	}

	/** @param jumlahRunnable nilai baru untuk {@link #jumlahRunnable} */
	public void setJumlahRunnable(Integer jumlahRunnable) {
		this.jumlahRunnable = jumlahRunnable;
	}

	/** @return {@link #jumlahWaiting} */
	@Column(name = "jumlah_waiting")
	public Integer getJumlahWaiting() {
		return jumlahWaiting;
	}

	/** @param jumlahWaiting nilai baru untuk {@link #jumlahWaiting} */
	public void setJumlahWaiting(Integer jumlahWaiting) {
		this.jumlahWaiting = jumlahWaiting;
	}

	/** @return {@link #jumlahTimedWaiting} */
	@Column(name = "jumlah_timed_waiting")
	public Integer getJumlahTimedWaiting() {
		return jumlahTimedWaiting;
	}

	/** @param jumlahTimedWaiting nilai baru untuk {@link #jumlahTimedWaiting} */
	public void setJumlahTimedWaiting(Integer jumlahTimedWaiting) {
		this.jumlahTimedWaiting = jumlahTimedWaiting;
	}

	/** @return {@link #jumlahBlocked} */
	@Column(name = "jumlah_blocked")
	public Integer getJumlahBlocked() {
		return jumlahBlocked;
	}

	/** @param jumlahBlocked nilai baru untuk {@link #jumlahBlocked} */
	public void setJumlahBlocked(Integer jumlahBlocked) {
		this.jumlahBlocked = jumlahBlocked;
	}

	/** @return {@link #jumlahNew} */
	@Column(name = "jumlah_new")
	public Integer getJumlahNew() {
		return jumlahNew;
	}

	/** @param jumlahNew nilai baru untuk {@link #jumlahNew} */
	public void setJumlahNew(Integer jumlahNew) {
		this.jumlahNew = jumlahNew;
	}

	/** @return {@link #jumlahTerminated} */
	@Column(name = "jumlah_terminated")
	public Integer getJumlahTerminated() {
		return jumlahTerminated;
	}

	/** @param jumlahTerminated nilai baru untuk {@link #jumlahTerminated} */
	public void setJumlahTerminated(Integer jumlahTerminated) {
		this.jumlahTerminated = jumlahTerminated;
	}

	/** @return {@link #jumlahDeadlock} */
	@Column(name = "jumlah_deadlock")
	public Integer getJumlahDeadlock() {
		return jumlahDeadlock;
	}

	/** @param jumlahDeadlock nilai baru untuk {@link #jumlahDeadlock} */
	public void setJumlahDeadlock(Integer jumlahDeadlock) {
		this.jumlahDeadlock = jumlahDeadlock;
	}

	/** @return {@link #heapDipakai} */
	@Column(name = "heap_dipakai")
	public Long getHeapDipakai() {
		return heapDipakai;
	}

	/** @param heapDipakai nilai baru untuk {@link #heapDipakai} */
	public void setHeapDipakai(Long heapDipakai) {
		this.heapDipakai = heapDipakai;
	}

	/** @return {@link #heapMaksimum} */
	@Column(name = "heap_maksimum")
	public Long getHeapMaksimum() {
		return heapMaksimum;
	}

	/** @param heapMaksimum nilai baru untuk {@link #heapMaksimum} */
	public void setHeapMaksimum(Long heapMaksimum) {
		this.heapMaksimum = heapMaksimum;
	}

	/** @return {@link #nonHeapDipakai} */
	@Column(name = "non_heap_dipakai")
	public Long getNonHeapDipakai() {
		return nonHeapDipakai;
	}

	/** @param nonHeapDipakai nilai baru untuk {@link #nonHeapDipakai} */
	public void setNonHeapDipakai(Long nonHeapDipakai) {
		this.nonHeapDipakai = nonHeapDipakai;
	}

	/** @return {@link #oldGenDipakai} */
	@Column(name = "old_gen_dipakai")
	public Long getOldGenDipakai() {
		return oldGenDipakai;
	}

	/** @param oldGenDipakai nilai baru untuk {@link #oldGenDipakai} */
	public void setOldGenDipakai(Long oldGenDipakai) {
		this.oldGenDipakai = oldGenDipakai;
	}

	/** @return {@link #oldGenMaksimum} */
	@Column(name = "old_gen_maksimum")
	public Long getOldGenMaksimum() {
		return oldGenMaksimum;
	}

	/** @param oldGenMaksimum nilai baru untuk {@link #oldGenMaksimum} */
	public void setOldGenMaksimum(Long oldGenMaksimum) {
		this.oldGenMaksimum = oldGenMaksimum;
	}

	/** @return {@link #metaspaceDipakai} */
	@Column(name = "metaspace_dipakai")
	public Long getMetaspaceDipakai() {
		return metaspaceDipakai;
	}

	/** @param metaspaceDipakai nilai baru untuk {@link #metaspaceDipakai} */
	public void setMetaspaceDipakai(Long metaspaceDipakai) {
		this.metaspaceDipakai = metaspaceDipakai;
	}

	/** @return {@link #metaspaceMaksimum} */
	@Column(name = "metaspace_maksimum")
	public Long getMetaspaceMaksimum() {
		return metaspaceMaksimum;
	}

	/** @param metaspaceMaksimum nilai baru untuk {@link #metaspaceMaksimum} */
	public void setMetaspaceMaksimum(Long metaspaceMaksimum) {
		this.metaspaceMaksimum = metaspaceMaksimum;
	}

	/** @return {@link #bebanSistem} */
	@Column(name = "beban_sistem")
	public Double getBebanSistem() {
		return bebanSistem;
	}

	/** @param bebanSistem nilai baru untuk {@link #bebanSistem} */
	public void setBebanSistem(Double bebanSistem) {
		this.bebanSistem = bebanSistem;
	}

	/** @return {@link #prosesCpuPersen} */
	@Column(name = "proses_cpu_persen")
	public Double getProsesCpuPersen() {
		return prosesCpuPersen;
	}

	/** @param prosesCpuPersen nilai baru untuk {@link #prosesCpuPersen} */
	public void setProsesCpuPersen(Double prosesCpuPersen) {
		this.prosesCpuPersen = prosesCpuPersen;
	}

	/** @return {@link #topClass} */
	@Column(name = "top_class", length = 255)
	public String getTopClass() {
		return topClass;
	}

	/** @param topClass nilai baru untuk {@link #topClass} */
	public void setTopClass(String topClass) {
		this.topClass = topClass;
	}

	/** @return {@link #jumlahProsesor} */
	@Column(name = "jumlah_prosesor")
	public Integer getJumlahProsesor() {
		return jumlahProsesor;
	}

	/** @param jumlahProsesor nilai baru untuk {@link #jumlahProsesor} */
	public void setJumlahProsesor(Integer jumlahProsesor) {
		this.jumlahProsesor = jumlahProsesor;
	}

	/** @return {@link #jumlahKelas} */
	@Column(name = "jumlah_kelas")
	public Integer getJumlahKelas() {
		return jumlahKelas;
	}

	/** @param jumlahKelas nilai baru untuk {@link #jumlahKelas} */
	public void setJumlahKelas(Integer jumlahKelas) {
		this.jumlahKelas = jumlahKelas;
	}

	/** @return {@link #gcJumlah} */
	@Column(name = "gc_jumlah")
	public Long getGcJumlah() {
		return gcJumlah;
	}

	/** @param gcJumlah nilai baru untuk {@link #gcJumlah} */
	public void setGcJumlah(Long gcJumlah) {
		this.gcJumlah = gcJumlah;
	}

	/** @return {@link #gcWaktuMillis} */
	@Column(name = "gc_waktu_millis")
	public Long getGcWaktuMillis() {
		return gcWaktuMillis;
	}

	/** @param gcWaktuMillis nilai baru untuk {@link #gcWaktuMillis} */
	public void setGcWaktuMillis(Long gcWaktuMillis) {
		this.gcWaktuMillis = gcWaktuMillis;
	}

	/** @return {@link #uptimeMillis} */
	@Column(name = "uptime_millis")
	public Long getUptimeMillis() {
		return uptimeMillis;
	}

	/** @param uptimeMillis nilai baru untuk {@link #uptimeMillis} */
	public void setUptimeMillis(Long uptimeMillis) {
		this.uptimeMillis = uptimeMillis;
	}

	/**
	 * @return {@link #keterangan}, atau string kosong (bukan {@code null}) bila belum pernah
	 *         diisi -- aman dipakai langsung di tampilan tanpa pengecekan null tambahan
	 */
	@Column(name = "keterangan", columnDefinition = "text", nullable = true)
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan;
	}

	/** @param keterangan nilai baru untuk {@link #keterangan} */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return representasi teks singkat baris ini: id diikuti {@link #getRingkasan()},
	 *         dipisah tanda hubung -- dipakai pada log/debug, bukan tampilan pengguna akhir
	 */
	public String toString() {
		return (id == null ? "" : id.toString()) + "-" + getRingkasan();
	}

	// ===================== Nilai turunan (tidak disimpan) =====================

	/**
	 * Helper null-safe: mengubah {@link Integer} yang mungkin {@code null} menjadi {@code int}
	 * primitif, memakai 0 sebagai pengganti {@code null}. Dipakai seluruh method turunan di
	 * bawah agar tidak perlu mengulang pengecekan null pada tiap field nullable.
	 *
	 * @param value nilai yang mungkin {@code null}
	 * @return {@code value.intValue()}, atau 0 bila {@code value} {@code null}
	 */
	private static int nz(Integer value) {
		return value == null ? 0 : value.intValue();
	}

	/**
	 * Helper null-safe: mengubah {@link Long} yang mungkin {@code null} menjadi {@code long}
	 * primitif, memakai 0 sebagai pengganti {@code null}.
	 *
	 * @param value nilai yang mungkin {@code null}
	 * @return {@code value.longValue()}, atau 0L bila {@code value} {@code null}
	 */
	private static long nz(Long value) {
		return value == null ? 0L : value.longValue();
	}

	/**
	 * Persentase pemakaian heap (0..100), dihitung dari {@link #heapDipakai}/{@link #heapMaksimum}.
	 * Nilai turunan ({@code @Transient}, tidak disimpan ke basis data) -- dihitung ulang setiap
	 * dipanggil dari field lain yang sudah tersimpan.
	 *
	 * <p>Hasil dijepit ke rentang 0..100 walau pembagian mentahnya di luar rentang itu (mis.
	 * data race pada pembacaan MXBean yang membuat "dipakai" sesaat melebihi "maksimum").</p>
	 *
	 * @return persentase 0..100, atau -1 bila {@link #heapMaksimum} {@code null} atau &lt;= 0
	 *         (batas heap tidak diketahui)
	 */
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

	/**
	 * Persentase pemakaian metaspace (0..100), lihat {@link #persen(Long, Long)} untuk aturan
	 * pembulatan/penjepitan dan nilai -1.
	 *
	 * @return persentase pemakaian {@link #metaspaceDipakai} terhadap {@link #metaspaceMaksimum}
	 */
	@Transient
	public int getPersenMetaspace() {
		return persen(metaspaceDipakai, metaspaceMaksimum);
	}

	/**
	 * Persentase pemakaian Old Generation heap (0..100), lihat {@link #persen(Long, Long)}
	 * untuk aturan pembulatan/penjepitan dan nilai -1.
	 *
	 * @return persentase pemakaian {@link #oldGenDipakai} terhadap {@link #oldGenMaksimum}
	 */
	@Transient
	public int getPersenOldGen() {
		return persen(oldGenDipakai, oldGenMaksimum);
	}

	/**
	 * Helper bersama untuk {@link #getPersenMetaspace()} dan {@link #getPersenOldGen()}:
	 * menghitung persentase {@code dipakai/maksimum} dan menjepit hasilnya ke 0..100.
	 *
	 * @param dipakai   bytes yang terpakai; {@code null} diperlakukan sebagai 0
	 * @param maksimum  batas maksimum bytes; {@code null} atau &lt;= 0 berarti batas tidak diketahui
	 * @return persentase 0..100, atau -1 bila {@code maksimum} tidak diketahui
	 */
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

	/**
	 * Jumlah thread yang sedang "menunggu" dalam arti luas: {@link #jumlahWaiting} +
	 * {@link #jumlahTimedWaiting} + {@link #jumlahBlocked}. Nilai turunan, dipakai
	 * {@link #getRingkasan()} untuk ringkasan satu baris. <b>Catatan:</b> pada server sehat
	 * yang tidak jenuh, sebagian besar thread adalah worker pool idle berstatus
	 * {@code WAITING}/{@code TIMED_WAITING} -- jumlah ini karenanya BUKAN indikator kesehatan
	 * yang baik dengan sendirinya (lihat penjelasan pada {@link #getStatusKesehatan()} soal
	 * mengapa rasio menunggu/total pernah dipakai lalu ditinggalkan karena alarm palsu).
	 *
	 * @return total thread berstatus WAITING, TIMED_WAITING, dan BLOCKED
	 */
	@Transient
	public int getJumlahMenunggu() {
		return nz(jumlahWaiting) + nz(jumlahTimedWaiting) + nz(jumlahBlocked);
	}

	/** @return {@link #maxThreadSatuLock} */
	@Column(name = "max_thread_satu_lock")
	public Integer getMaxThreadSatuLock() {
		return maxThreadSatuLock;
	}

	/** @param maxThreadSatuLock nilai baru untuk {@link #maxThreadSatuLock} */
	public void setMaxThreadSatuLock(Integer maxThreadSatuLock) {
		this.maxThreadSatuLock = maxThreadSatuLock;
	}

	/**
	 * Ambang "Perhatian" untuk {@link #getMaxThreadSatuLock()}: jumlah thread yang menumpuk
	 * pada SATU lock yang sama (di luar worker idle) yang sudah dianggap kontensi nyata namun
	 * belum kritis. Dipakai {@link #getStatusKesehatan()}.
	 */
	public static final int AMBANG_KONTENSI_PERHATIAN = 8;
	/**
	 * Ambang "Kritis" untuk {@link #getMaxThreadSatuLock()}: jumlah thread pada satu lock yang
	 * sama yang menandakan pola pool koneksi habis/lock global macet. Dipakai
	 * {@link #getStatusKesehatan()}.
	 */
	public static final int AMBANG_KONTENSI_KRITIS = 50;

	/**
	 * Ambang jumlah thread BLOCKED (rebutan monitor). SATU-DUA thread BLOCKED itu transien &amp;
	 * normal (menunggu sesaat masuk blok synchronized) — bukan indikasi masalah. Masalah nyata =
	 * BANYAK thread BLOCKED (lock convoy). Karena itu 1-2 dianggap Normal.
	 */
	public static final int AMBANG_BLOCKED_PERHATIAN = 3;
	/** Ambang "Kritis" untuk {@link #jumlahBlocked} (lock convoy). Dipakai {@link #getStatusKesehatan()}. */
	public static final int AMBANG_BLOCKED_KRITIS = 10;
	/** Ambang "Perhatian" untuk {@link #getPersenHeap()}/{@link #getPersenMetaspace()} (persen 0..100). */
	public static final int AMBANG_MEMORI_PERHATIAN = 85;
	/** Ambang "Kritis" untuk {@link #getPersenHeap()}/{@link #getPersenMetaspace()} (persen 0..100). */
	public static final int AMBANG_MEMORI_KRITIS = 95;
	/** Ambang historis/informasional untuk tampilan; total thread idle tidak menentukan status. */
	public static final int AMBANG_THREAD_PERHATIAN = 800;
	/**
	 * Status kesehatan sederhana berdasarkan ambang batas default:
	 * <ul>
	 * <li>KRITIS  : ada deadlock, heap/metaspace &gt;= 95%, &gt;= 10 thread BLOCKED, atau &gt;= 50
	 * thread menumpuk pada SATU lock yang sama (pola pool koneksi habis / lock global macet).</li>
	 * <li>PERHATIAN: heap/metaspace &gt;= 85%, &gt;= 3 thread BLOCKED, atau &gt;= 8
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
		int kontensi = nz(maxThreadSatuLock);
		int blocked = nz(jumlahBlocked);

		if (nz(jumlahDeadlock) > 0 || (heap >= 0 && heap >= AMBANG_MEMORI_KRITIS)
				|| (meta >= 0 && meta >= AMBANG_MEMORI_KRITIS)
				|| blocked >= AMBANG_BLOCKED_KRITIS || kontensi >= AMBANG_KONTENSI_KRITIS) {
			return "Kritis";
		}
		if ((heap >= 0 && heap >= AMBANG_MEMORI_PERHATIAN)
				|| (meta >= 0 && meta >= AMBANG_MEMORI_PERHATIAN)
				|| blocked >= AMBANG_BLOCKED_PERHATIAN || kontensi >= AMBANG_KONTENSI_PERHATIAN) {
			return "Perhatian";
		}
		return "Normal";
	}

	/**
	 * Warna heksadesimal untuk badge status pada dasbor, diturunkan langsung dari
	 * {@link #getStatusKesehatan()}: merah untuk "Kritis", kuning/oranye untuk "Perhatian",
	 * hijau untuk "Normal" (nilai bawaan bila status tidak dikenali).
	 *
	 * @return kode warna heksadesimal (mis. {@code #dc2626})
	 */
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

	/**
	 * Ringkasan satu baris yang mudah dibaca untuk daftar/dasbor: jumlah thread beserta yang
	 * menunggu, persentase heap (bila diketahui), peringatan deadlock (bila ada), diakhiri
	 * status kesehatan dari {@link #getStatusKesehatan()}.
	 *
	 * @return teks ringkasan, mis. {@code "Thread 42 (menunggu 30), Heap 61% - Normal"}
	 */
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
