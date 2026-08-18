INSERT INTO detailperkuliahan(
            nilai_huruf, persetujuan, semester,
            nilai_ip, total_nilai, mahasiswa, 
            matakuliah_konversi)



select 
(case when max(a."NLAKHTRNLP") is null then 'E' else max(a."NLAKHTRNLP") end) as nilai_huruf,
1 as persetujuan,
case when trim(max(b."SEMESTBKMK")) = 'PP' then 9 else to_number(trim(max(b."SEMESTBKMK")),'99999') end as semester,
(case when max(a."BOBOTTRNLP") is null then 0 else max(a."BOBOTTRNLP") end) as nilai_ip,
(case max(a."BOBOTTRNLP") when 4.0 then 87 when 3.0 then 72 when 2.0 then 61 when 1.0 then 52 else 0 end) as total_nilai,
c.id as mahasiswa,
max(d.id) as matakuliah

from importepsbed."TRNLP" a
inner join importepsbed."TBKMK" b on (trim(a."KDKMKTRNLP") = trim(b."KDKMKTBKMK") /*and b."THSMSTBKMK" = a."THSMSTRNLP"*/ )
inner join mahasiswa c on (a."NIMHSTRNLP" = c.nim)
inner join jurusan b1 on (b."KDPSTTBKMK" = b1.kode)
inner join matakuliah d on (trim(b."KDKMKTBKMK") = trim(d.kode) and b1.id = d.jurusan)

where (c.id, d.id) not in (select mahasiswa, matakuliah_konversi from detailperkuliahan)
and (c.id, d.id) not in (select aa.mahasiswa, bb.matakuliah from detailperkuliahan aa inner join perkuliahan bb on (aa.perkuliahan=bb.id))
--and "BOBOTTRNLP" is not null 
and b."SEMESTBKMK" is not null
and trim(b."SEMESTBKMK") != ''

and (tahunangkatan + ((case when trim((b."SEMESTBKMK")) = 'PP' then 9 else to_number(trim((b."SEMESTBKMK")),'99999') end/2)-1) ) <= EXTRACT(YEAR FROM CURRENT_TIMESTAMP)

group by c.id, d.kode;