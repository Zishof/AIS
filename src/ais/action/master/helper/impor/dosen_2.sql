INSERT INTO dosen(
            ktp, alamat, mycode, kelamin, code, 
            nama, nidn, 
            tanggallahir, tempatlahir, fakultas, 
            jurusan, tetap)

select
'',
'',
"NIDNNMSDOS",
'',
"NIDNNMSDOS"  as nip,
"NMDOSMSDOS" || ' ' || "GELARMSDOS",
"NIDNNMSDOS",
null,
'',
b.id,
null,
1 as tetap

from importepsbed."MSDOS_STT" a
inner join fakultas b on (a."KDPTIMSDOS" = b.kode);

update dosen set code = '' where code is null;