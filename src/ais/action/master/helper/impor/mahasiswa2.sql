update epsbed."MHS2014" set "TGLHRMSMH" = '19960627' where  "TGLHRMSMH" = '19962706';
update epsbed."MHS2014" set "TGLHRMSMH" = '19960119' where  "TGLHRMSMH" = '19960019';
update epsbed."MHS2014" set "TGLHRMSMH" = '19950930' where  "TGLHRMSMH" = '19953009';


INSERT INTO mahasiswa(
            alamat, 
            is_encripted, kelamin, 
            nama,pass, nim, 
            program, semester_mulai,
            tahunangkatan, tanggal_masuk, tanggal_lulus, 
            tanggallahir, tempatlahir, 
            waktu_kuliah, warganegara, agama, jenis_seleksi, 
            jenjang, jurusan, negara, 
            status, status_awal_mahasiswa)

select
max("TPLHRMSMH"),
false,
case max("KDJEKMSMH") when 'L' then 'Laki-Laki' else 'Perempuan' end as kelamin,
max("NMMHSMSMH"),
"NIMHSMSMH",
"NIMHSMSMH",
'Reguler',
'Ganjil',
to_number(max("TAHUNMSMH"),'99999'),
(case when max("TGMSKMSMH") = '' then null else date(substr(max("TGMSKMSMH"),1,4)||'-'||substr(max("TGMSKMSMH"),5,2)||'-'||substr(max("TGMSKMSMH"),7,2)) end) as tanggal_masuk,
(case when max("TGLLSMSMH") = '' then null else date(substr(max("TGLLSMSMH"),1,4)||'-'||substr(max("TGLLSMSMH"),5,2)||'-'||substr(max("TGLLSMSMH"),7,2)) end) as tanggal_lulus,
(case when max("TGLHRMSMH") = '' then null else date(substr(max("TGLHRMSMH"),1,4)||'-'||substr(max("TGLHRMSMH"),5,2)||'-'||substr(max("TGLHRMSMH"),7,2)) end) as tanggallahir,
max("TPLHRMSMH"),
'Pagi',
'WNI',
1,
2,
max(b.jenjang) as jenjang,
max(b.id) as jurusan,
114,
max(c.id) as status,
2

from epsbed."MHS2014" a
inner join jurusan b on (a."KDPSTMSMH" = b.kode)
left join status_mahasiswa c on (upper(trim(c.kode_epsbed)) = upper(trim("STMHSMSMH")))
where "NIMHSMSMH" not in (select nim from mahasiswa)
group by "NIMHSMSMH";


