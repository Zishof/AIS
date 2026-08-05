INSERT INTO detailperkuliahan(
            nilai_huruf, persetujuan, semester,
            nilai_ip, total_nilai, mahasiswa, 
            matakuliah_konversi)



select 
max(a."NLAKHTRNL") as nilai_huruf,
1 as persetujuan,
case when trim(max(b."SEMESTBKM")) = 'PP' then 9 else to_number(trim(max(b."SEMESTBKM")),'99999') end as semester,
case when trim(max(a."BOBOTTRNL")) = '' then 0 else to_number(max(a."BOBOTTRNL"),'99999') end as nilai_ip,
case when trim(max(a."BOBOTTRNL")) = '' then 0 else (case to_number(trim(max(a."BOBOTTRNL")),'99999') when 4.0 then 80 when 3.0 then 70 when 2.0 then 60 when 1.0 then 50 else 40 end) end as total_nilai,
c.id as mahasiswa,
max(d.id) as matakuliah

from importepsbed."TRNLM" a
inner join importepsbed."TBKMK" b on (a."KDKMKTRNL" = b."KDKMKTBKM")
inner join mahasiswa c on (a."NIMHSTRNL" = c.nim)
inner join jurusan b1 on (b."KDPSTTBKM" = b1.kode)
inner join matakuliah d on (b."KDKMKTBKM" = d.kode and b1.id = d.jurusan)

where (c.id, d.id) not in (select mahasiswa, matakuliah_konversi from detailperkuliahan)
--and "BOBOTTRNL" is not null 
and b."SEMESTBKM" is not null
and trim(b."SEMESTBKM") != ''

and (tahunangkatan + ((case when trim((b."SEMESTBKM")) = 'PP' then 9 else to_number(trim((b."SEMESTBKM")),'99999') end/2)-1) ) <= EXTRACT(YEAR FROM CURRENT_TIMESTAMP)

group by c.id, d.kode;