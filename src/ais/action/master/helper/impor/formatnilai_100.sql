INSERT INTO formatnilai(
            persen, matakuliah_konversi,
            status_pertemuan)
select 100,id,4 from matakuliah where (id,4) not in (select matakuliah_konversi,status_pertemuan from formatnilai);