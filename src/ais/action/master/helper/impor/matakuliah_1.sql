INSERT INTO matakuliah(kode, nama, sks, jurusan)
select
"KDKMKTBKM",
max("NAKMKTBKM") as nama,
to_number(max("SKSMKTBKM"),'99999') as sks,
b.id

from importepsbed."TBKMK" a
inner join jurusan b on (a."KDPSTTBKM" = b.kode)
where ("KDKMKTBKM",b.id) not in (select kode, jurusan from matakuliah)
group by "KDKMKTBKM",b.id