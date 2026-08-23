# Before–After Metrik AIS

Semua baris berstatus **belum diukur** sampai operator menjalankan instrumen pada lingkungan uji yang representatif. Angka tanpa kondisi pengujian tidak boleh dipakai sebagai bukti.

## Cara mengukur (Java 8 / Tomcat 9)
```bash
# GC dan heap (ganti <PID>)
jstat -gcutil <PID> 5000 12
# Thread
jstack <PID> | grep -c "^\""
# Histogram objek live (memicu full GC — hanya di dev/staging/maintenance window)
jmap -histo:live <PID> | head -40
# RSS (Windows)
tasklist /FI "PID eq <PID>"
```
Catat selalu: commit/tanggal source, opsi JVM, konfigurasi pool, jumlah data, beban (jumlah user/request), dan durasi warm-up.

## Tabel metrik
| Metrik | Baseline | Sesudah | Delta | Kondisi pengujian |
|---|---:|---:|---:|---|
| Startup time | belum diukur | | | |
| WAR size (production artifact) | belum diukur (source webapp 2,2 GB) | | | |
| RSS steady state | belum diukur | | | |
| Heap after full GC | belum diukur | | | |
| Old Gen occupancy | belum diukur | | | |
| Metaspace occupancy | belum diukur | | | |
| Full GC count/pause | belum diukur | | | |
| Live thread count | belum diukur | | | |
| Active/idle DB connections (per pool: utama, streaming, ojs, radius, openfire) | belum diukur | | | |
| HTTP session dan ZK desktop | belum diukur | | | |
| Cache entries (MemoryCacheUtil MAPS, Ehcache per region) | belum diukur | | | |
| Report peak heap/duration (kecil/sedang/besar) | belum diukur | | | |
| p50/p95/p99 latency endpoint representatif | belum diukur | | | |
| Throughput/error rate | belum diukur | | | |
