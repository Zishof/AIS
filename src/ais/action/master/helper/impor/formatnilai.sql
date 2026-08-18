INSERT INTO formatnilai(
            persen, matakuliah_konversi,
            status_pertemuan)
select 0,id,1 from matakuliah where (id,1) not in (select matakuliah_konversi,status_pertemuan from formatnilai);


INSERT INTO formatnilai(
            persen, matakuliah_konversi,
            status_pertemuan)
select 20,id,2 from matakuliah where (id,2) not in (select matakuliah_konversi,status_pertemuan from formatnilai);

INSERT INTO formatnilai(
            persen, matakuliah_konversi,
            status_pertemuan)
select 30,id,3 from matakuliah where (id,3) not in (select matakuliah_konversi,status_pertemuan from formatnilai);


INSERT INTO formatnilai(
            persen, matakuliah_konversi,
            status_pertemuan)
select 50,id,4 from matakuliah where (id,4) not in (select matakuliah_konversi,status_pertemuan from formatnilai);