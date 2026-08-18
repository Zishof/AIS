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
b.id,
null,
1

from importepsbed."DOSTETAP_STIE" a
left join fakultas b on (a."PTIND" = b.kode)
where "NIDNN" not in (select nidn from dosen);

update dosen set code = '' where code is null;