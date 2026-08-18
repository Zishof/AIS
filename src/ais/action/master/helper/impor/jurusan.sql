INSERT INTO jurusan(
            apakah_prodi, kode_epsbed, nama, fakultas, 
            jenjang, akreditasi, buka, gelar, no_sk_akreditasi, 
            singkatan_gelar, tanggal_akhir_akreditasi, tanggal_akreditasi, 
            kode)


select false,
"KDPSTMSPST",
max("NMPSTMSPST"),
max(b.id) as fakultas,
(select id from jenjang where jenjang_epsbed = max("KDJENMSPST")) as jenjang,
max("KDSTAMSPST"),
true,
'',
max("NOMBAMSPST"),
'',
max("TGLABMSPST"),
max("TGLBAMSPST"),
"KDPSTMSPST"

from importepsbed."MSPST" a
inner join fakultas b on (a."KDFAKMSPST" = b.kode or a."KDPTIMSPST" = b.kode)
where "KDPSTMSPST" not in (select kode from jurusan)

group by "KDPSTMSPST";