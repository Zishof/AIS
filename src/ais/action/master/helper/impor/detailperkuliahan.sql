delete from detailperkuliahan where matakuliah_konversi is not null;

INSERT INTO detailperkuliahan(
            nilai_huruf, persetujuan, semester,
            nilai_ip, total_nilai, mahasiswa, 
            matakuliah_konversi)



select 
(case when max(a."NLAKHTRNLM") is null then 'E' else max(a."NLAKHTRNLM") end) as nilai_huruf,
1 as persetujuan,
case when trim(max(b."SEMESTBKMK")) = 'PP' then 9 else to_number(trim(max(b."SEMESTBKMK")),'99999') end as semester,
(case when max(a."BOBOTTRNLM") is null then 0 else max(a."BOBOTTRNLM") end) as nilai_ip,
(case max(a."BOBOTTRNLM") when 4.0 then 87 when 3.0 then 72 when 2.0 then 61 when 1.0 then 52 else 0 end) as total_nilai,
c.id as mahasiswa,
max(d.id) as matakuliah

from importepsbed."TRNLM" a
inner join importepsbed."TBKMK" b on (trim(a."KDKMKTRNLM") = trim(b."KDKMKTBKMK") /*and b."THSMSTBKMK" = a."THSMSTRNLM"*/ )
inner join mahasiswa c on (a."NIMHSTRNLM" = c.nim)
inner join jurusan b1 on (b."KDPSTTBKMK" = b1.kode)
inner join matakuliah d on (trim(b."KDKMKTBKMK") = trim(d.kode) and b1.id = d.jurusan)

where (c.id, d.id) not in (select mahasiswa, matakuliah_konversi from detailperkuliahan)
--and "BOBOTTRNLM" is not null 
and b."SEMESTBKMK" is not null
and trim(b."SEMESTBKMK") != ''

and (tahunangkatan + ((case when trim((b."SEMESTBKMK")) = 'PP' then 9 else to_number(trim((b."SEMESTBKMK")),'99999') end/2)-1) ) <= EXTRACT(YEAR FROM CURRENT_TIMESTAMP)

group by c.id, d.kode;

update detailperkuliahan a set oleh=(select jurusan||'' from mahasiswa b where b.id=a.mahasiswa);


INSERT INTO public.perkuliahan(
	program, semester, tahun_ajaran, jurusan, kurikulum, matakuliah, kapasitas_kelas, kurikulum_punya_matakuliah, status_penilaian)
	
	select
	'Reguler',a.semester,a.tahunakademik,c.jurusan,b1.kurikulum,a.matakuliah_konversi,50,b1.id,1
	
	from detailperkuliahan a
	inner join matakuliah b on (a.matakuliah_konversi=b.id)
	inner join mahasiswa c on (a.mahasiswa=c.id)
	left join kurikulum_punya_matakuliah b1 on (b1.matakuliah=a.matakuliah_konversi)
	where a.tahunakademik is not null
	group by a.semester,a.tahunakademik,c.jurusan,b1.kurikulum,a.matakuliah_konversi,b1.id;
	
	
	update detailperkuliahan aa set perkuliahan=(select max(id) from perkuliahan a where a.semester=aa.semester and a.tahun_ajaran=aa.tahunakademik and a.jurusan||''=aa.oleh and aa.matakuliah_konversi=a.matakuliah) where perkuliahan is null;
	
	update detailperkuliahan set matakuliah_konversi=null where perkuliahan is not null;
	
	delete from perkuliahan where id not in (select perkuliahan from detailperkuliahan group by perkuliahan);
	
	