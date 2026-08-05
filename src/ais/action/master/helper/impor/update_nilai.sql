DROP TABLE temp_detailperkuliahan;

CREATE TABLE temp_detailperkuliahan
(
  id bigserial NOT NULL,
  nilai_huruf character varying(2),
  oleh character varying(255),
  persetujuan integer NOT NULL,
  semester integer,
  tanggal_dirubah timestamp without time zone,
  nilai_ip double precision,
  total_nilai double precision,
  mahasiswa bigint NOT NULL,
  matakuliah_konversi bigint,
  perkuliahan bigint,
  matakuliah_asli_sebelum_konversi bigint,
  ikuti_perkuliahan bigint,
  paket_perkuliahan bigint,
  tahunakademik character varying(255)
);


INSERT INTO temp_detailperkuliahan(
            id, nilai_huruf, oleh, persetujuan, semester, tanggal_dirubah, 
            nilai_ip, total_nilai, mahasiswa, matakuliah_konversi, perkuliahan, 
            matakuliah_asli_sebelum_konversi, ikuti_perkuliahan, paket_perkuliahan, 
            tahunakademik)
SELECT id, nilai_huruf, oleh, persetujuan, semester, tanggal_dirubah, 
       nilai_ip, total_nilai, mahasiswa, matakuliah_konversi, perkuliahan, 
       matakuliah_asli_sebelum_konversi, ikuti_perkuliahan, paket_perkuliahan, 
       tahunakademik
  FROM detailperkuliahan
  where perkuliahan is not null;


truncate detailperkuliahan cascade;

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
inner join importepsbed."TBKMK" b on (trim(a."KDKMKTRNLM") = trim(b."KDKMKTBKMK"))
inner join mahasiswa c on (a."NIMHSTRNLM" = c.nim)
inner join jurusan b1 on (b."KDPSTTBKMK" = b1.kode)
inner join matakuliah d on (trim(b."KDKMKTBKMK") = trim(d.kode) and b1.id = d.jurusan)

where (c.id, d.id) not in (select mahasiswa, matakuliah_konversi from detailperkuliahan)
and b."SEMESTBKMK" is not null
and trim(b."SEMESTBKMK") != ''

and (tahunangkatan + ((case when trim((b."SEMESTBKMK")) = 'PP' then 9 else to_number(trim((b."SEMESTBKMK")),'99999') end/2)-1) ) <= EXTRACT(YEAR FROM CURRENT_TIMESTAMP)

group by c.id, d.kode;


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


INSERT INTO nilai(jumlah, detailperkuliahan, formatnilai)
select
a.total_nilai,
a.id,
b.id

from detailperkuliahan a
inner join formatnilai b on (a.matakuliah_konversi = b.matakuliah_konversi)
where (a.id,b.id) not in (select detailperkuliahan, formatnilai from nilai);


delete from detailperkuliahan where (mahasiswa, matakuliah_konversi) in (select a.mahasiswa,b.matakuliah from temp_detailperkuliahan a inner join perkuliahan b on (a.perkuliahan = b.id));


INSERT INTO detailperkuliahan(
            id, nilai_huruf, oleh, persetujuan, semester, tanggal_dirubah, 
            nilai_ip, total_nilai, mahasiswa, matakuliah_konversi, perkuliahan, 
            matakuliah_asli_sebelum_konversi, ikuti_perkuliahan, paket_perkuliahan, 
            tahunakademik)
SELECT id, nilai_huruf, oleh, persetujuan, semester, tanggal_dirubah, 
       nilai_ip, total_nilai, mahasiswa, matakuliah_konversi, perkuliahan, 
       matakuliah_asli_sebelum_konversi, ikuti_perkuliahan, paket_perkuliahan, 
       tahunakademik
  FROM temp_detailperkuliahan a
  where perkuliahan is not null;