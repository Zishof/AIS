delete from kurikulum_punya_matakuliah;
INSERT INTO kurikulum_punya_matakuliah(
            semester, tanggal_ditambahkan, kurikulum, 
            matakuliah)


select
case when trim(a."SEMESTBKM") = 'PP' then 9 else to_number(trim(a."SEMESTBKM"),'99999') end as semester,
CURRENT_TIMESTAMP,
c.id as kurikulum,
d.id as matakuliah
from importepsbed."TBKMK" a
left join jurusan b on (a."KDPSTTBKM" = b.kode)
left join kurikulum c on (('Kurikulum ' || b.nama || ' tahun ' || substring(a."THSMSTBKM",1,4)) = c.keterangan)
left join matakuliah d on (a."KDKMKTBKM" = d.kode and b.id = d.jurusan)
where a."SEMESTBKM" is not null and trim(a."SEMESTBKM") != ''
and ((case when trim(a."SEMESTBKM") = 'PP' then 9 else to_number(trim(a."SEMESTBKM"),'99999') end), c.id, d.id) not in (select semester,kurikulum,matakuliah from kurikulum_punya_matakuliah)
group by 
(case when trim(a."SEMESTBKM") = 'PP' then 9 else to_number(trim(a."SEMESTBKM"),'99999') end),
c.id,
d.id
