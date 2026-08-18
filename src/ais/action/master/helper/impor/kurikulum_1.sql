INSERT INTO kurikulum(
            keterangan, nama,tahun,jurusan)

select
'Kurikulum ' || b.nama || ' tahun ' || substring(a."THSMSTBKM",1,4),
'Kurikulum ' || b.nama || ' tahun ' || substring(a."THSMSTBKM",1,4),
to_number(substring(max(a."THSMSTBKM"),1,4),'99999'),
max(b.id) as jurusan
from importepsbed."TBKMK" a
inner join jurusan b on (a."KDPSTTBKM" = b.kode) 
group by 'Kurikulum ' || b.nama || ' tahun ' || substring(a."THSMSTBKM",1,4)
having 'Kurikulum ' || b.nama || ' tahun ' || substring(a."THSMSTBKM",1,4) not in (select nama from kurikulum)