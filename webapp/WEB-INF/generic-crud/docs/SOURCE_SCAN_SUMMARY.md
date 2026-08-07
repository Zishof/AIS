# Ringkasan Pemindaian Source Snapshot

Scanner membaca snapshot source yang tersedia saat paket dibuat. Hasil ini bukan pengganti Hibernate runtime metadata dan harus dijalankan ulang pada Git terbaru.

## Ringkasan

| Metrik | Jumlah |
|---|---:|
| Subclass GeneralValueObject | 1501 |
| Concrete @Entity | 1490 |
| Abstract | 11 |
| Kandidat foto | 68 |
| Mempunyai Action existing | 925 |
| Mempunyai custom on* candidate | 912 |
| Alias page binding generated (disabled) | 1.543 |
| Alias JSP UI + service generated | 3.086 |

## Klasifikasi scanner

| Status | Jumlah | Makna |
|---|---:|---|
| ELIGIBLE_METADATA_FIRST | 346 | Kandidat reference sederhana; tetap disabled sampai runtime/menu/scope review. |
| ELIGIBLE_PARITY_FIRST | 13 | Mempunyai Action existing; parity test wajib. |
| REVIEW_REQUIRED | 1131 | Foto, custom action, transaksi/integrasi/internal, atau risiko lain. |
| EXCLUDED_ABSTRACT | 11 | Tidak menjadi halaman CRUD. |

## Modul dengan entity terbanyak

| Modul model | Concrete entity |
|---|---:|
| root | 464 |
| sekolah | 153 |
| sirs | 110 |
| sister | 87 |
| library | 78 |
| asset | 63 |
| employ | 56 |
| file | 45 |
| rab | 45 |
| payroll | 42 |
| akunting | 38 |
| koperasi | 31 |
| surat | 28 |
| inventory | 22 |
| kursus | 22 |
| penelitiandanpengabdian | 19 |
| epsbed | 15 |
| recruitment | 13 |
| kpi | 12 |
| sop | 11 |
| lkp | 9 |
| spi | 9 |
| antarjemput | 8 |
| spmi | 8 |
| kkn | 7 |

## Entity wajib adapter khusus awal

- `ais.database.model.Mahasiswa`: foto=True; actions=`ais/action/master/MahasiswaAction.java|ais/action/master/alumni/MahasiswaAction.java`; custom action candidates=48.
- `ais.database.model.sekolah.Siswa`: foto=True; actions=`ais/action/master/sekolah/SiswaAction.java`; custom action candidates=16.
- `ais.database.model.Dosen`: foto=True; actions=`ais/action/master/DosenAction.java`; custom action candidates=22.
- `ais.database.model.sekolah.Guru`: foto=True; actions=`ais/action/master/sekolah/GuruAction.java`; custom action candidates=8.
- `ais.database.model.Pegawai`: foto=True; actions=`ais/action/master/PegawaiAction.java|ais/action/master/bkd/PegawaiAction.java`; custom action candidates=11.
- `ais.database.model.Tbmuser`: foto=True; actions=`ais/action/maintenance/TbmuserAction.java`; custom action candidates=4.

## Catatan penting

- Scanner memakai source parsing untuk inventory; property field tetap harus diverifikasi melalui Hibernate `ClassMetadata`.
- Action mapping berbasis convention dan GenericCrudAction; beberapa entity dapat mempunyai lebih dari satu page/menu binding.
- 3.086 JSP alias di `generated_aliases_disabled/` hanya delegator dan semua seed tetap disabled.
- Jangan mengaktifkan entity transaksi, bank integration, audit/log, token, file content, queue/job, atau temporary secara otomatis.
