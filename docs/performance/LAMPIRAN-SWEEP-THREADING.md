# Lampiran Fase 0 — Sweep Executor / Thread / ThreadLocal

Hasil inventaris statis `src/main/java` (2026-08-19). Belum ada pengukuran runtime.

## Ringkasan
| Pola | Jumlah |
|---|---|
| Call site `Executors.new*` | 53 |
| `new Thread(` | ±947 di 681 file (sebagian besar di `ais/action/`) |
| Kelas `extends TimerTask` | 28 (semua via Spring `timerFactory`) |
| Deklarasi `new ThreadLocal` | 37 (±25 punya `.remove()`) |
| Catatan | Mayoritas `new Timer(...)` adalah `org.zkoss.zul.Timer` (komponen UI per-desktop), bukan `java.util.Timer` |

## (a) Executor singleton DENGAN shutdown (sudah baik)
Semua dihentikan dari `AppStartupListener.contextDestroyed` (`AppStartupListener.java:313`):
`FlagAccessCache`, `ElearningFlagAccessCache`, `EntityAccessCache`, `DepositoAroScheduler`, `RepositorySyncScheduler`, `DatabasePerformanceSampler`, `PerformaSnapshotUtil`, thread `ais-startup-maintenance`, `Common.initCacheThread`, `LogMobileCleanupService` (via listener sendiri), `TenantProvisioningWorker` (via servlet destroy), Spring `timerFactory` (28 task), Ehcache manager, SessionFactory, JDBC driver.

## (b) Executor singleton TANPA shutdown — kandidat perbaikan P0/P1
| Lokasi | Kondisi | Catatan |
|---|---|---|
| `ais/common/AsyncTaskManager.java:22` | fixed pool via `DbThreadPool.safe(250)` (efektif ≤16, max 32), queue LinkedBlockingQueue TANPA batas, thread NON-daemon | **Prioritas tertinggi**: pool tersibuk aplikasi, tidak pernah shutdown → menahan classloader saat reload; queue tak terbatas |
| `ais/common/StokThresholdScheduler.java:61` | `mulai()` dipanggil di `AppStartupListener.java:269`, `hentikan()` ADA tapi tidak dipanggil di stop | Perbaikan satu baris |
| `ais/action/servlet/DoUpload.java:73` | `RETRY_SCHEDULER` non-daemon tanpa shutdown; baris 74 ada `Executors.newSingleThreadScheduledExecutor()` yatim (hasil dibuang) | Hapus baris yatim + wire shutdown |
| `ais/database/hibernate/AuditListener.java:~108` | sched 2 + pool 4, daemon, CallerRunsPolicy | Aman saat JVM exit; bocor classloader saat reload |
| `KegiatanPersistenceHelper.java:70`, `KegiatanProsesHeper.java:112`, `UjianRecomputeUtil.java:77`, `ProsesUjianHelper.java:617` | bounded, daemon, tanpa shutdown | Bocor classloader saat reload |
| `ais/delivery/email/sender/MailSender.java:79` | ThreadPoolExecutor 4/4, ArrayBlockingQueue(1000), CallerRunsPolicy, daemon | Konfigurasi pool terbaik di codebase; tanpa shutdown (mail antre hilang saat stop) |
| `ais/common/BacaTulisUtil.java:1052` | single-thread daemon; `flushPendingWrites()` dipanggil saat stop | Data aman, thread bocor |

## (c) Thread/pool per-request
- ±30 pool dibuat di dalam method (dibuat + shutdown di method yang sama); ±20 sudah lewat `DbThreadPool.safe(...)`, sisanya masih literal mentah: `PembayaranOnline.java:2813` (`min(50,total)`), `PengaturanBiaya.java:1181` (pool di dalam ENTITY class), dll.
- Raw `new Thread(...).start()` per aksi: `ais/action/servlet/Wa.java` (5 titik per jalur request WhatsApp), controller `newui` (3 file), 12 window laporan akreditasi, 8 window laporan keuangan, dsb.
- Thread startup-only (daemon, bounded by design): `AppStartupListener.java:81/300/414/884`.

## (d) ThreadLocal
**Bocor nyata (tidak pernah di-remove):**
1. **`ais/common/RequestContext.java:7` + `ais/common/ResponseContext.java:7`** — di-`set()` tiap request di `FilterJSP.java:53-54`; method `remove()` ada tapi TIDAK ada caller. Tiap worker thread Tomcat menahan `HttpServletRequest/Response` terakhir (berikut session/attributes/buffer) selamanya. **Kandidat perbaikan termurah-berdampak: tambah remove di `finally` FilterJSP.**
2. `Common.java:592-712, 18831` — ±55 `ThreadLocal<SimpleDateFormat/NumberFormat/...>` idiom formatter; benar secara fungsi, tapi footprint per-thread dan pin classloader saat reload.
3. `DashboardUiKit.java:53`, `CommonReport.java:97/104` — idiom formatter sama.
4. `ProfileUiHelper.java:99` `sembunyikanPegawai` — flag Boolean tanpa remove; nilai basi bisa bocor ke request berikutnya di thread yang sama (**bug korektness potensial**, bukan hanya RAM).
5. `ErrorAuditUtil.java:39/40` — `LAST_RESULT` menahan objek per thread tanpa batas waktu.

**Sudah benar:** ThreadLocal Hibernate (`HibernateUtil.MAP`, Ojs/Streaming/Radius), `AuditTrailHelper`, `Report.java`, dll. — dibersihkan di `finally` `FilterJSP` atau di titik pemakaian.
