INSERT INTO matakuliah(kode, nama, sks, jurusan)
select
trim("KDKMKTBKMK"),
max("NAKMKTBKMK") as nama,
max("SKSMKTBKMK") as sks,
b.id

from importepsbed."TBKMK" a
inner join jurusan b on (trim(a."KDPSTTBKMK") = trim(b.kode_epsbed))
where (trim("KDKMKTBKMK"),b.id) not in (select trim(kode), jurusan from matakuliah)
group by "KDKMKTBKMK",b.id;