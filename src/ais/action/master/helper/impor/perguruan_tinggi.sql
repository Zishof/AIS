delete from perguruan_tinggi;
INSERT INTO perguruan_tinggi(
            alamat1, alamat2, email, faksimili, kode_perguruan_tinggi, 
            kode_pos, kode_yayasan, kota, nama, nomor_akta, tanggal_akta, 
            tanggal_awal_pendirian, telepon, website)
select
"ALMT1MSPTI",
"ALMT2MSPTI",
"EMAILMSPTI",
"FAKSIMSPTI",
"KDPTIMSPTI",
"KDPOSMSPTI",
"KDYYSMSPTI",
"KOTAAMSPTI",
"NMPTIMSPTI",
"NOMSKMSPTI",
"TGPTIMSPTI",
"TGAWLMSPTI",
"TELPOMSPTI",
"HPAGEMSPTI"

from importepsbed."MSPTI" 
where "NMPTIMSPTI" is not null;