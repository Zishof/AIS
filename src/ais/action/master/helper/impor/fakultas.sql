INSERT INTO fakultas(
            nama, kode, perguruan_tinggi)

select "NMFAKMSFAK","KDFAKMSFAK",b.id from importepsbed."MSFAK" a
left join perguruan_tinggi b on (a."KDPTIMSFAK" = b.kode_perguruan_tinggi)
where "KDFAKMSFAK" not in (select kode from fakultas);