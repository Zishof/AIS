delete from badanhukum;
INSERT INTO badanhukum(
            id, alamat1, alamat2, alamatwebsite, email, faksimil, kode, kodepos, 
            kota, nama, namaakta, nomorpengesahan, tanggalakta, 
            tanggalawalpendirian, tanggalpengesahan, telepon)

select
1,
"ALMT1MSYYS",
"ALMT2MSYYS",
"HPAGEMSYYS",
"EMAILMSYYS",
"FAKSIMSYYS",
"KDYYSMSYYS",
"KDPOSMSYYS",
"KOTAAMSYYS",
"NMYYSMSYYS",
"NOMBNMSYYS",
"NOMSKMSYYS",
"TGLBNMSYYS",
"TGYYSMSYYS",
"TGAWLMSYYS",
"TELPOMSYYS"

from importepsbed."MSYYS"
where "KDYYSMSYYS" not in (select kode from badanhukum) order by "TGYYSMSYYS" desc limit 1;