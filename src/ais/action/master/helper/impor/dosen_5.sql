INSERT INTO dosen(
            ktp, alamat, mycode, kelamin, code, 
            nama, nidn, 
            tanggallahir, tempatlahir,
            jurusan, tetap)

select
'',
'',
"NIDNNMSDOS",
'',
"NIDNNMSDOS"  as nip,
"NMDOSMSDOS",
"NIDNNMSDOS",
null,
'',
null,
1 as tetap

from importepsbed."MSDOS" a
inner join perguruan_tinggi b on (a."KDPTIMSDOS" = b.kode_perguruan_tinggi);

update dosen set code = '' where code is null;