INSERT INTO mahasiswa(
            alamat, 
            is_encripted, kelamin, 
            nama,pass, nim, 
            program, semester_mulai,
            tahunangkatan, tanggal_masuk, tanggal_lulus, 
            tanggallahir, tempatlahir, 
            waktu_kuliah, warganegara, agama, jenis_seleksi, 
            jenjang, jurusan, negara, 
            status_awal_mahasiswa)

select
max("TPLHRMSMHS"),
false,
case max("KDJEKMSMHS") when 'L' then 'Laki-laki' else 'Perempuan' end as kelamin,
max("NMMHSMSMHS"),
"NIMHSMSMHS",
"NIMHSMSMHS",
'Reguler',
(case when substr(max("SMAWLMSMHS"),5,1) = '1' then 'Ganjil' else 'Genap' end) as jenis_semester,
to_number(max("TAHUNMSMHS"),'99999'),
max("TGMSKMSMHS"),
max("TGLLSMSMHS"),
max("TGLHRMSMHS"),
max("TPLHRMSMHS"),
'Pagi',
'WNI',
1,
2,
max(b.jenjang) as jenjang,
max(b.id) as jurusan,
114,
--max(c.id) as status,
2

from importepsbed."MSMHS" a
inner join jurusan b on (a."KDPSTMSMHS" = b.kode_epsbed)
left join status_mahasiswa c on (upper(trim(c.kode_epsbed)) = upper(trim("STMHSMSMHS")))
where "NIMHSMSMHS" not in (select nim from mahasiswa)
group by "NIMHSMSMHS";