INSERT INTO dosen(
            mycode, kelamin, code, 
            nama, nidn,
            tanggallahir, tempatlahir,  
            fakultas, 
            jurusan, tetap)


select 
"NIDNN",
case "KDJEK" when 'L' then 'Laki-Laki' else 'Perempuan' end as kelamin,
"NIDNN",
"NMDOS",
"NIDNN",
"TGLHR",
"TPLHR",
b.fakultas,
b.id,
1

from importepsbed."021017" a
left join jurusan b on (a."KDPST" = b.kode)
where "NIDNN" not in (select nidn from dosen);

update dosen set code = '' where code is null;