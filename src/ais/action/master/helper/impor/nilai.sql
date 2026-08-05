INSERT INTO nilai(jumlah, detailperkuliahan, formatnilai)
select
a.total_nilai,
a.id,
b.id

from detailperkuliahan a
inner join formatnilai b on (a.matakuliah_konversi = b.matakuliah_konversi)
where (a.id,b.id) not in (select detailperkuliahan, formatnilai from nilai);