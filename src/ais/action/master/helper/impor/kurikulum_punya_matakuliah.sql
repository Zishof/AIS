delete from kurikulum_punya_matakuliah;
INSERT INTO kurikulum_punya_matakuliah(
            semester, tanggal_ditambahkan, kurikulum, 
            matakuliah)


select
case when trim(a."SEMESTBKMK") = 'PP' then 9 else to_number(trim(a."SEMESTBKMK"),'99999') end as semester,
CURRENT_TIMESTAMP,
c.id as kurikulum,
d.id as matakuliah
from importepsbed."TBKMK" a
left join jurusan b on (trim(a."KDPSTTBKMK") = b.kode_epsbed)
left join kurikulum c on (('Kurikulum ' || b.nama || ' tahun ' || substring(a."THSMSTBKMK",1,4)) = c.keterangan)
left join matakuliah d on (trim(a."KDKMKTBKMK") = d.kode and b.id = d.jurusan)
where a."SEMESTBKMK" is not null and trim(a."SEMESTBKMK") != ''
and d.id is not null
and ((case when trim(a."SEMESTBKMK") = 'PP' then 9 else to_number(trim(a."SEMESTBKMK"),'99999') end), c.id, d.id) not in (select semester,kurikulum,matakuliah from kurikulum_punya_matakuliah)
group by 
(case when trim(a."SEMESTBKMK") = 'PP' then 9 else to_number(trim(a."SEMESTBKMK"),'99999') end),
c.id,
d.id
