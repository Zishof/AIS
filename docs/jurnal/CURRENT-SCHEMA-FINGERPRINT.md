# Current schema fingerprint

Run: 2026-08-23, read-only fingerprint setelah guarded Hibernate update pada clone streaming SIT. Baseline `ais` dan `streaming_ais` tidak dimutasi.

| Target clone | Total tabel | Kontrak jurnal | Hasil |
|---|---:|---:|---|
| `ais_jurnal_sit` | 3.322 | 12 tabel main | `PASS` |
| `streaming_ais_jurnal_sit` | 83 | 1 tabel `lampiran_jurnal` | `PASS` |

`lampiran_jurnal` mempunyai 19 kolom, dua unique constraint (`repo_bitstream_id`, `idempotency_key`), dan 0 baris setelah test cleanup. DDL dibuat dari annotation mapping `LampiranJurnal` melalui `hibernate.streaming.cfg.xml`; update hanya aktif bila environment opt-in bernilai true dan nama database cocok suffix clone SIT/UAT/demo/fixture.

State content canonical: `PENDING_CONTENT → CONTENT_STORED → VERIFIED → LINKED → AVAILABLE`. Metadata main tetap `RepoBitstream`, dan `contentRef` hanya scalar ID; tidak ada `@ManyToOne` lintas SessionFactory.

Evidence command class: `ais.action.master.jurnal.test.JurnalSchemaFingerprintSelfTest`.

