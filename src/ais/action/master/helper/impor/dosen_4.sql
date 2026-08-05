INSERT INTO dosen(
            ktp, alamat, mycode, kelamin, code, 
            nama, nidn, 
            tanggallahir, tempatlahir, fakultas, 
            jurusan, tetap)

select
"NODOSMSDO",
"TPLHRMSDO",
"KDPSTMSDO",
case "KDJEKMSDO" when 'L' then 'Laki-Laki' else 'Perempuan' end as kelamin,
"NIDNNMSDO" as nip,
"NMDOSMSDO",
"NIDNNMSDO",
null, --"TGLHRTBDOS",
"TPLHRMSDO",
b.fakultas,
b.id,
1 as tetap

from importepsbed."MSDOS" a
inner join jurusan b on (a."KDPSTMSDO" = b.kode)
where "TPLHRMSDO" not in (select nidn from dosen);

update dosen set code = '' where code is null;