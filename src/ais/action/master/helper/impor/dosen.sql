INSERT INTO dosen(
            ktp, alamat, mycode, kelamin, code, 
            nama, nidn, 
            tanggallahir, tempatlahir, fakultas, 
            jurusan, tetap)

select
"NOKTPTBDOS",
"TPLHRTBDOS",
"KDPSTTBDOS",
case "KDJEKTBDOS" when 'L' then 'Laki-Laki' else 'Perempuan' end as kelamin,
case when "NIPNBTBDOS" is null or trim("NIPNBTBDOS") = '' then "NIPPBTBDOS" else "NIPNBTBDOS" end as nip,
"NMDOSTBDOS",
"NIDNNTBDOS",
"TGLHRTBDOS",
"TPLHRTBDOS",
b.fakultas,
b.id,
1 as tetap

from importepsbed."TBDOS" a
inner join jurusan b on (a."KDPSTTBDOS" = b.kode_epsbed)
inner join perguruan_tinggi c on (a."PTINDTBDOS" = c.kode_perguruan_tinggi)
where "NIDNNTBDOS" not in (select nidn from dosen);

update dosen set code = '' where code is null;