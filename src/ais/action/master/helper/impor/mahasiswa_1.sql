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
null,--max("TGMSKMSMH"),
null,--max("TGLLSMSMH"),
null,--max("TGLHRMSMH"),
null,--max("TPLHRMSMH"),
'Pagi',
'WNI',
1,
2,
max(b.jenjang) as jenjang,
max(b.id) as jurusan,
114,
max(c.id) as status,
2

from importepsbed."MSMHS" a
inner join jurusan b on (a."KDPSTMSMH" = b.kode)
left join status_mahasiswa c on (upper(trim(c.kode_epsbed)) = upper(trim("STMHSMSMH")))
where "NIMHSMSMH" not in (select nim from mahasiswa)
group by "NIMHSMSMH";