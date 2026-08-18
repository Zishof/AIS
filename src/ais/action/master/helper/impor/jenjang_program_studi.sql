ALTER TABLE jenjang_program_studi ALTER email TYPE character varying(255);
ALTER TABLE jenjang_program_studi ALTER nama_operator TYPE character varying(255);
ALTER TABLE jenjang_program_studi ALTER no_sk_akreditasi TYPE character varying(255);
ALTER TABLE jenjang_program_studi ALTER dimulai_dari_semester TYPE character varying(255);
ALTER TABLE jenjang_program_studi ALTER fax_ps TYPE character varying(255);
ALTER TABLE jenjang_program_studi ALTER frekuensi_kurikulum TYPE character varying(255);
ALTER TABLE jenjang_program_studi ALTER hp_operator TYPE character varying(255);
ALTER TABLE jenjang_program_studi ALTER no_sk_dikti TYPE character varying(255);
ALTER TABLE jenjang_program_studi ALTER pelaksanaan_kurikulum TYPE character varying(255);
ALTER TABLE jenjang_program_studi ALTER sks_lulus TYPE character varying(255);
ALTER TABLE jenjang_program_studi ALTER status TYPE character varying(255);
ALTER TABLE jenjang_program_studi ALTER status_akreditasi TYPE character varying(255);
ALTER TABLE jenjang_program_studi ALTER telp_ka_ps TYPE character varying(255);
ALTER TABLE jenjang_program_studi ALTER telp_ps TYPE character varying(255);

delete from jenjang_program_studi;

INSERT INTO jenjang_program_studi(
            dimulai_dari_semester, email, fax_ps, frekuensi_kurikulum, 
            hp_operator, nama_operator, nidn_ka_ps, nm_ka_ps, no_sk_akreditasi, 
            no_sk_dikti, pelaksanaan_kurikulum, sks_lulus, status, 
            status_akreditasi, tanggal_berdiri, telp_ka_ps, 
            telp_ps, tgl_akhir_sk_akreditasi, tgl_akhir_sk_dikti, tgl_mulai_sk_akreditasi, 
            tgl_mulai_sk_dikti, jenjang, jurusan, epsbed_tahun_hapus, epsbed_frekuensi_kurikulum, 
            epsbed_pelaksanaan_kurikulum, epsbed_status_jurusan, epsbed_status_akreditasi)


select
1,
"EMAILMSPST",
"FAKSIMSPST",
"KDFREMSPST",
"TELPSMSPST",
"NMOPRMSPST",
"NOKPSMSPST",
"NMPSTMSPST",
"NOMBAMSPST",
"NOMSKMSPST",
 "KDPELMSPST",
 "SKSTTMSPST",
 "STATUMSPST",
 "KDSTAMSPST",
 "TGAWLMSPST",
 "TELPSMSPST",
 "TELPOMSPST",
 "TGLABMSPST",
 "TGLAKMSPST",
 "TGLBAMSPST",
 "TGLSKMSPST",
 c.id as jenjang,
 d.id as jurusan,
 "MLSEMMSPST",
 e.id as frekuensi_kurikulum,
 f.id as pelaksanaan_kurikulum,
 g.id as status,
 h.id as status_akreditasi
 
from importepsbed."MSPST" a
left join perguruan_tinggi b on (a."KDPTIMSPST" = b.kode_perguruan_tinggi)
left join jenjang c on (c.jenjang_epsbed = a."KDJENMSPST")
left join jurusan d on (d.kode_epsbed = a."KDPSTMSPST")
left join epsbed.epsbed_frekuensi_kurikulum e on (e.kode = a."KDFREMSPST")
left join epsbed.epsbed_pelaksanaan_kurikulum f on (f.kode = a."KDPELMSPST")
left join epsbed.epsbed_status g on (g.kode = a."STATUMSPST")
left join epsbed.epsbed_status_akreditasi h on (h.kode = a."KDSTAMSPST")
where c.id is not null and d.id is not null;

--where a."NOMSKMSPST" not in (select no_sk_akreditasi from jenjang_program_studi);