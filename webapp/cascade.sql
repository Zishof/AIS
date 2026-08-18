-- Tabel utama
ALTER TABLE asset.penerimaan_pengadaan_master_asset ALTER COLUMN keterangan       TYPE text;
ALTER TABLE asset.penerimaan_pengadaan_master_asset ALTER COLUMN keterangantermin TYPE text;
ALTER TABLE asset.penerimaan_pengadaan_master_asset ALTER COLUMN json_object      TYPE text;
ALTER TABLE asset.penerimaan_pengadaan_master_asset ALTER COLUMN kurir            TYPE text;

-- Tabel audit Envers
ALTER TABLE new_audit.penerimaan_pengadaan_master_asset__audit ALTER COLUMN keterangan       TYPE text;
ALTER TABLE new_audit.penerimaan_pengadaan_master_asset__audit ALTER COLUMN keterangantermin TYPE text;
ALTER TABLE new_audit.penerimaan_pengadaan_master_asset__audit ALTER COLUMN json_object      TYPE text;
ALTER TABLE new_audit.penerimaan_pengadaan_master_asset__audit ALTER COLUMN kurir            TYPE text;


-- update or delete on table "detail_setting_biaya" violates foreign key constraint "fk33037014b229979d" on table "detail_biaya"

  alter table detail_biaya
drop constraint fk33037014b229979d,
add constraint fk33037014b229979d
   foreign key (detail_setting_biaya)
   references detail_setting_biaya(id)
   on delete cascade;

--  update or delete on table "pegawai" violates foreign key constraint "fk9d3039c8e8944cb" on table "pengajuan_pegawai"
  alter table pengajuan_pegawai
drop constraint fk9d3039c8e8944cb,
add constraint fk9d3039c8e8944cb
   foreign key (pegawai)
   references pegawai(id)
   on delete cascade;
   
   -- update or delete on table "satuan_kerja" violates foreign key constraint "fkd6ecbaf0cfade50f" on table "pegawai"
   
     alter table pegawai
drop constraint fkd6ecbaf0cfade50f,
add constraint fkd6ecbaf0cfade50f
   foreign key (satuan_kerja)
   references rab.satuan_kerja (id)
   on delete set null;

--  update or delete on table "pengaturan_pembayaran_bulanan" violates foreign key constraint "fk59bea98af381abdb" on table "detail_kegiatan"
     alter table detail_kegiatan
drop constraint fk59bea98af381abdb,
add constraint fk59bea98af381abdb
   foreign key (pengaturan_pembayaran_bulanan)
   references pengaturan_pembayaran_bulanan (id)
   on delete cascade;
   
   
-- update or delete on table "pengaturan_pembayaran_bulanan" violates foreign key constraint "fk59bea98af381abdb" on table "detail_kegiatan"
     alter table detail_kegiatan
drop constraint fk59bea98af381abdb,
add constraint fk59bea98af381abdb
   foreign key (pengaturan_pembayaran_bulanan)
   references pengaturan_pembayaran_bulanan (id)
   on delete cascade;


-- update or delete on table "satuan_kerja" violates foreign key constraint "fk146aefebcfade50f" on table "sumber_dana"

     alter table rab.sumber_dana
drop constraint fk146aefebcfade50f,
add constraint fk146aefebcfade50f
   foreign key (satuan_kerja)
   references rab.satuan_kerja (id)
   on delete set null;


-- update or delete on table "satuan_kerja" violates foreign key constraint "fk714218aacfade50f" on table "klasifikasi_surat_keluar"

     alter table surat.klasifikasi_surat_keluar
drop constraint fk714218aacfade50f,
add constraint fk714218aacfade50f
   foreign key (satuan_kerja)
   references rab.satuan_kerja (id)
   on delete set null;
   
   
   
   
 --  update or delete on table "satuan_kerja" violates foreign key constraint "fk2db843cfade50f" on table "akun"
   
        alter table akunting.akun
drop constraint fk2db843cfade50f,
add constraint fk2db843cfade50f
   foreign key (satuan_kerja)
   references rab.satuan_kerja (id)
   on delete set null;
   
   
   -- update or delete on table "satuan_kerja" violates foreign key constraint "fkb1d9e5d7cfade50f" on table "jam_kerja_pegawai"
   
           alter table employ.jam_kerja_pegawai
drop constraint fkb1d9e5d7cfade50f,
add constraint fkb1d9e5d7cfade50f
   foreign key (satuan_kerja)
   references rab.satuan_kerja (id)
   on delete set null;
   
   
   
   --- update or delete on table "satuan_kerja" violates foreign key constraint "fk1351f35ccfade50f" on table "format_kpi"
           alter table format_kpi
drop constraint fk1351f35ccfade50f,
add constraint fk1351f35ccfade50f
   foreign key (satuan_kerja)
   references rab.satuan_kerja (id)
   on delete set null;
   

-- update or delete on table "satuan_kerja" violates foreign key constraint "fk2c2e749fcfade50f" on table "fakultas"
           alter table fakultas
drop constraint fk2c2e749fcfade50f,
add constraint fk2c2e749fcfade50f
   foreign key (satuan_kerja)
   references rab.satuan_kerja (id)
   on delete set null;




update or delete on table "satuan_kerja" violates foreign key constraint "fk76794e01cfade50f" on table "kegiatan_tugas_jabatan"

           alter table kegiatan_tugas_jabatan
drop constraint fk76794e01cfade50f,
add constraint fk76794e01cfade50f
   foreign key (satuan_kerja)
   references rab.satuan_kerja (id)
   on delete set null;





-- update or delete on table "detail_biaya" violates foreign key constraint "fkef68dfd2c3c8930" on table "cicilan_pembayaran"


  alter table cicilan_pembayaran
drop constraint fkef68dfd2c3c8930,
add constraint fkef68dfd2c3c8930
   foreign key (detail_biaya)
   references detail_biaya (id)
   on delete set null;

-- update or delete on table "alur_persetujuan_surat_keluar_status" violates foreign key constraint "fke7c6691049b59ac8" on table "surat_keluar"

  alter table surat.surat_keluar
drop constraint fke7c6691049b59ac8,
add constraint fke7c6691049b59ac8
   foreign key (alur_ditolak)
   references surat.alur_persetujuan_surat_keluar_status (id)
   on delete set null;

-- update or delete on table "anggota_koperasi" violates foreign key constraint "fk666dddc248c7d580" on table "virtual_account_bank"
  alter table virtual_account_bank
drop constraint fk666dddc248c7d580,
add constraint fk666dddc248c7d580
   foreign key (anggota_koperasi)
   references koperasi.anggota_koperasi(id)
   on delete cascade;
   
   -- update or delete on table "disposisi_sop" violates foreign key constraint "fkd05508dcbc255eb8" on table "cuti_dan_izin"
    alter table payroll.cuti_dan_izin
drop constraint fkd05508dcbc255eb8,
add constraint fkd05508dcbc255eb8
   foreign key (disposisi_sop)
   references disposisi_sop(id)
   on delete cascade; 
   

-- update or delete on table "anggota_koperasi" violates foreign key constraint "fka5c4b40a48c7d580" on table "tbmuser"
  alter table tbmuser
drop constraint fka5c4b40a48c7d580,
add constraint fka5c4b40a48c7d580
   foreign key (anggota_koperasi)
   references koperasi.anggota_koperasi(id)
   on delete cascade;


-- update or delete on table "pembelian_anggota_koperasi" violates foreign key constraint "fkccb60459139170b" on table "pembelian"
  alter table koperasi.pembelian
drop constraint fkccb60459139170b,
add constraint fkccb60459139170b
   foreign key (pembelian_anggota_koperasi)
   references koperasi.pembelian_anggota_koperasi(id)
   on delete cascade;


ALTER TABLE akunting."grup_transaksi" DROP CONSTRAINT "fka4d0de6f5ee1d762";


-- update or delete on table "proses_transfer_standing_instruction" violates foreign key constraint "fk4c0e751bc4496bff" on table "standing_instruction"

  alter table akunting.standing_instruction
drop constraint fk4c0e751bc4496bff,
add constraint fk4c0e751bc4496bff
   foreign key (proses_transfer_standing_instruction)
   references akunting.proses_transfer_standing_instruction(id)
   on delete set null;


-- update or delete on table "posting_history" violates foreign key constraint "fka38e558361a9291" on table "pembayaran_gaji"

  alter table payroll.pembayaran_gaji
drop constraint fka38e558361a9291,
add constraint fka38e558361a9291
   foreign key (posting_history)
   references akunting.posting_history(id)
   on delete set null;

--update or delete on table "diskon_mahasiswa" violates foreign key constraint "fk59bea98af757b16e" on table "detail_kegiatan"
--  Detail: Key (id)=(287) is still referenced from table "detail_kegiatan"
  
  
  alter table detail_kegiatan
drop constraint fk59bea98af757b16e,
add constraint fk59bea98af757b16e
   foreign key (diskon_mahasiswa_data)
   references diskon_mahasiswa(id)
   on delete cascade;
   
-- update or delete on table "disposisi_alur_sop" violates foreign key constraint "fk865ca00e5a1e9dd7" on table "disposisi_sop"

  alter table disposisi_sop
drop constraint fk865ca00e5a1e9dd7,
add constraint fk865ca00e5a1e9dd7
   foreign key (disposisi_end)
   references disposisi_alur_sop(id)
   on delete set null;
   
-- update or delete on table "posting_history" violates foreign key constraint "fk59bea98a361a9291" on table "detail_kegiatan"

  alter table detail_kegiatan
drop constraint fk59bea98a361a9291,
add constraint fk59bea98a361a9291
   foreign key (posting_history)
   references akunting.posting_history(id)
   on delete set null;
   

--  update or delete on table "disposisi_alur_sop" violates foreign key constraint "fk865ca00e357922de" on table "disposisi_sop"

  alter table disposisi_sop
drop constraint fk865ca00e357922de,
add constraint fk865ca00e357922de
   foreign key (disposisi_start)
   references disposisi_alur_sop(id)
   on delete set null;


-- update or delete on table "pembayaran_gaji" violates foreign key constraint "fk4c0e751b8b7251cf" on table "standing_instruction"
  alter table akunting.standing_instruction
drop constraint fk4c0e751b8b7251cf,
add constraint fk4c0e751b8b7251cf
   foreign key (pembayaran_gaji)
   references payroll.pembayaran_gaji(id)
   on delete cascade;



  alter table akunting.standing_instruction
drop constraint fk4c0e751bbc255eb8,
add constraint fk4c0e751bbc255eb8
   foreign key (disposisi_sop)
   references disposisi_sop(id)
   on delete cascade;
   
   
  alter table payroll.pembayaran_gaji 
drop constraint fka38e5582d534fb5,
add constraint fka38e5582d534fb5
   foreign key (standing_instruction)
   references akunting.standing_instruction(id)
   on delete cascade;


-- update or delete on table "daftar_pengajuan_transfer" violates foreign key constraint "fka38e5582ebd8e34" on table "pembayaran_gaji"
  alter table payroll.pembayaran_gaji
drop constraint fka38e5582ebd8e34,
add constraint fka38e5582ebd8e34
   foreign key (daftar_pengajuan_transfer)
   references akunting.daftar_pengajuan_transfer(id)
   on delete cascade;


-- update or delete on table "mahasiswa" violates foreign key constraint "fk85fb75fdc20aa61f" on table "log_mobile"

  alter table log_mobile
drop constraint fk85fb75fdc20aa61f,
add constraint fk85fb75fdc20aa61f
   foreign key (mahasiswa)
   references mahasiswa(id)
   on delete cascade;


--  update or delete on table "disposisi_sop" violates foreign key constraint "fk54defea3bc255eb8" on table "disposisi_alur_sop"

  alter table disposisi_alur_sop
drop constraint fk54defea3bc255eb8,
add constraint fk54defea3bc255eb8
   foreign key (disposisi_sop)
   references disposisi_sop(id)
   on delete cascade;
   
-- update or delete on table "disposisi_sop" violates foreign key constraint "fk86158e7bbc255eb8" on table "hasil_spmi"

  alter table hasil_spmi
drop constraint fk86158e7bbc255eb8,
add constraint fk86158e7bbc255eb8
   foreign key (disposisi_sop)
   references disposisi_sop(id)
   on delete cascade;
   
--  update or delete on table "hasil_spmi" violates foreign key constraint "fkf90c6b90b5ea6971" on table "hasil_temuan_spmi"


  alter table hasil_temuan_spmi
drop constraint fkf90c6b90b5ea6971,
add constraint fkf90c6b90b5ea6971
   foreign key (hasil_spmi)
   references hasil_spmi(id)
   on delete cascade;



-- update or delete on table "adjus_variable_penggajian" violates foreign key constraint "fka996e103c66234f4" on table "gaji_tabahan"
 
alter table payroll.gaji_tabahan
drop constraint fka996e103c66234f4,
add constraint fka996e103c66234f4
   foreign key (adjus_variable_penggajian)
   references payroll.adjus_variable_penggajian(id)
   on delete cascade;
   
--update or delete on table "detail_biaya" violates foreign key constraint "fkef68dfd2c3c8930" on table "cicilan_pembayaran"


  alter table cicilan_pembayaran
drop constraint fkef68dfd2c3c8930,
add constraint fkef68dfd2c3c8930
   foreign key (detail_biaya)
   references detail_biaya(id)
   on delete set null;
   
   
    alter table sirs.biaya
drop constraint fk5963582d7b3d58e,
add constraint fk5963582d7b3d58e
   foreign key (akun)
   references akunting.akun(id)
   on delete cascade;
   
   
 --  update or delete on table "setting_biaya_detail" violates foreign key constraint "fk3303701482807f75" on table "detail_biaya"
 
 
 
   alter table detail_biaya
drop constraint fk3303701482807f75,
add constraint fk3303701482807f75
   foreign key (setting_biaya_detail)
   references setting_biaya_detail(id)
   on delete set null;
   
   
 --  update or delete on table "saldo_awal_master_asset_detail" violates foreign key constraint "fk62d81980a09f31fd" on table "penerimaan_pengadaan_master_asset_detail"
   
    alter table asset.penerimaan_pengadaan_master_asset_detail
drop constraint fk62d81980a09f31fd,
add constraint fk62d81980a09f31fd
   foreign key (saldo_awal_master_asset_detail)
   references asset.saldo_awal_master_asset_detail(id)
   on delete set null;
   
   -- update or delete on table "posting_history" violates foreign key constraint "fk657fa83361a9291" on table "pajak"
   
       alter table akunting.pajak
drop constraint fk657fa83361a9291,
add constraint fk657fa83361a9291
   foreign key (posting_history)
   references akunting.posting_history(id)
   on delete set null;

-- update or delete on table "log_login" violates foreign key constraint "fk59b25d1cb268c463" on table "online_users"

alter table online_users
drop constraint fk59b25d1cb268c463,
add constraint fk59b25d1cb268c463
   foreign key (login)
   references log_login(id)
   on delete set null;

-- update or delete on table "penggantian_kas_kecil" violates foreign key constraint "fk584d1e62c59ba442" on table "daftar_pengajuan_transfer"

alter table akunting.daftar_pengajuan_transfer
drop constraint fk584d1e62c59ba442,
add constraint fk584d1e62c59ba442
   foreign key (penggantian_kas_kecil)
   references akunting.penggantian_kas_kecil(id)
   on delete cascade;
   
--   SELECT pg_cancel_backend(pid) FROM pg_stat_activity WHERE state = 'active' and pid <> pg_backend_pid();


-- update or delete on table "mahasiswa" violates foreign key constraint "fk666dddc2c20aa61f" on table "virtual_account_bank"

ALTER TABLE virtual_account_bank 
DROP CONSTRAINT fk666dddc2c20aa61f,
ADD CONSTRAINT fk666dddc2c20aa61f 
FOREIGN KEY (mahasiswa) 
REFERENCES mahasiswa(id) 
ON DELETE SET NULL;


-- update or delete on table "mahasiswa" violates foreign key constraint "fk59b25d1cc20aa61f" on table "online_users"
ALTER TABLE online_users 
DROP CONSTRAINT fk59b25d1cc20aa61f,
ADD CONSTRAINT fk59b25d1cc20aa61f 
FOREIGN KEY (mahasiswa) 
REFERENCES mahasiswa(id) 
ON DELETE SET NULL;


-- update or delete on table "mahasiswa" violates foreign key constraint "fk85fb75fdc20aa61f" on table "log_mobile"
ALTER TABLE log_mobile 
DROP CONSTRAINT fk85fb75fdc20aa61f,
ADD CONSTRAINT fk85fb75fdc20aa61f 
FOREIGN KEY (mahasiswa) 
REFERENCES mahasiswa(id) 
ON DELETE SET NULL;



ALTER TABLE kegiatan 
DROP CONSTRAINT fkffabc55cc20aa61f,
ADD CONSTRAINT fkffabc55cc20aa61f 
FOREIGN KEY (mahasiswa) 
REFERENCES mahasiswa(id) 
ON DELETE SET NULL;


-- update or delete on table "jenis_shift_punya_pegawai" violates foreign key constraint "fk6463daf7438528e5" on table "status_kehadiran_karyawan_harian"
alter table status_kehadiran_karyawan_harian
drop constraint fk6463daf7438528e5,
add constraint fk6463daf7438528e5
   foreign key (jenis_shift_punya_pegawai)
   references payroll.jenis_shift_punya_pegawai(id)
   on delete cascade;


ALTER TABLE pertemuan_file_content ALTER COLUMN link TYPE text;
ALTER TABLE pertemuan_file_content ALTER COLUMN keterangan TYPE text;

ALTER TABLE public.pertemuan_file_content_AUD ALTER COLUMN link TYPE text;
ALTER TABLE public.pertemuan_file_content_AUD ALTER COLUMN keterangan TYPE text;


   
ALTER TABLE pertemuan ALTER COLUMN catatan TYPE text;
ALTER TABLE new_audit.pertemuan__audit ALTER COLUMN catatan TYPE text;


   
ALTER TABLE pertemuan ALTER COLUMN topik TYPE text;
ALTER TABLE new_audit.pertemuan__audit ALTER COLUMN topik TYPE text;


ALTER TABLE pertemuan ALTER COLUMN metode_pembelajaran TYPE text;
ALTER TABLE new_audit.pertemuan__audit ALTER COLUMN metode_pembelajaran TYPE text;


ALTER TABLE pertemuan ALTER COLUMN keterangannilai TYPE text;
ALTER TABLE new_audit.pertemuan__audit ALTER COLUMN keterangannilai TYPE text;


ALTER TABLE mahasiswa ALTER COLUMN keterangan TYPE text;
ALTER TABLE new_audit.mahasiswa__audit ALTER COLUMN keterangan TYPE text;

ALTER TABLE sekolah.pembayaran_siswa ALTER COLUMN keterangan TYPE text;
ALTER TABLE new_audit.pembayaran_siswa__audit ALTER COLUMN keterangan TYPE text;


ALTER TABLE payroll.pembayaran_item_gaji_pegawai ALTER COLUMN keterangan TYPE text;
ALTER TABLE new_audit.pembayaran_item_gaji_pegawai__audit ALTER COLUMN keterangan TYPE text;


ALTER TABLE payroll.pembayaran_item_gaji_pegawai ALTER COLUMN defaultFormula TYPE text;
ALTER TABLE new_audit.pembayaran_item_gaji_pegawai__audit ALTER COLUMN defaultFormula TYPE text;




--ALTER TABLE pertemuan ALTER COLUMN keterangan TYPE text;
--ALTER TABLE new_audit.pertemuan__audit ALTER COLUMN keterangan TYPE text;
   
ALTER TABLE rab.workspace ALTER COLUMN nama TYPE text;
ALTER TABLE rab.workspace ALTER COLUMN keterangan TYPE text;

ALTER TABLE new_audit.workspace__audit ALTER COLUMN nama TYPE text;
ALTER TABLE new_audit.workspace__audit ALTER COLUMN keterangan TYPE text;


ALTER TABLE sekolah.siswa ALTER COLUMN kondisi_siswa TYPE text;
ALTER TABLE sekolah.siswa ALTER COLUMN status_dalam_keluarga TYPE text;


ALTER TABLE new_audit.siswa__audit ALTER COLUMN kondisi_siswa TYPE text;
ALTER TABLE new_audit.siswa__audit ALTER COLUMN status_dalam_keluarga TYPE text;


ALTER TABLE biodata_mahasiswa ALTER COLUMN asal_sma TYPE text;
ALTER TABLE new_audit.biodata_mahasiswa__audit  ALTER COLUMN asal_sma TYPE text;


ALTER TABLE biodata_calon_mahasiswa ALTER COLUMN asal_sma TYPE text;
ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN asal_sma TYPE text;




ALTER TABLE biodata_calon_mahasiswa ALTER COLUMN kecamatan_sekolah TYPE text;
ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN kecamatan_sekolah TYPE text;

ALTER TABLE biodata_calon_mahasiswa ALTER COLUMN keluarahan_calon TYPE text;
ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN keluarahan_calon TYPE text;

ALTER TABLE biodata_calon_mahasiswa ALTER COLUMN kemampuan_bahasa1 TYPE text;
ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN kemampuan_bahasa1 TYPE text;

ALTER TABLE biodata_calon_mahasiswa ALTER COLUMN kemampuan_bahasa2 TYPE text;
ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN kemampuan_bahasa2 TYPE text;

ALTER TABLE biodata_calon_mahasiswa ALTER COLUMN kemampuan_bahasa3 TYPE text;
ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN kemampuan_bahasa3 TYPE text;

ALTER TABLE biodata_calon_mahasiswa ALTER COLUMN kendaraan_kuliah TYPE text;
ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN kendaraan_kuliah TYPE text;

ALTER TABLE biodata_calon_mahasiswa ALTER COLUMN no_identitas TYPE text;
ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN no_identitas TYPE text;

ALTER TABLE biodata_calon_mahasiswa ALTER COLUMN no_ujian TYPE text;
ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN no_ujian TYPE text;

ALTER TABLE biodata_calon_mahasiswa ALTER COLUMN kecamatan_calon TYPE text;
ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN kecamatan_calon TYPE text;

ALTER TABLE biodata_calon_mahasiswa ALTER COLUMN nama_organisasi TYPE text;
ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN nama_organisasi TYPE text;

ALTER TABLE biodata_calon_mahasiswa ALTER COLUMN nama_wali TYPE text;
ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN nama_wali TYPE text;

 
ALTER TABLE biodata_calon_mahasiswa ALTER COLUMN nim TYPE text;
ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN nim TYPE text;

ALTER TABLE biodata_calon_mahasiswa ALTER COLUMN surat_izin_mengemudi TYPE text;
ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN surat_izin_mengemudi TYPE text;

ALTER TABLE biodata_calon_mahasiswa ALTER COLUMN program TYPE text;
ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN program TYPE text;



ALTER TABLE payroll.item_gaji_pegawai ALTER COLUMN defaultFormula TYPE text;
ALTER TABLE new_audit.item_gaji_pegawai__audit ALTER COLUMN defaultFormula TYPE text;


ALTER TABLE biodata_calon_mahasiswa ALTER COLUMN alamat_asal_sma TYPE text;
ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN alamat_asal_sma TYPE text;

ALTER TABLE biodata_calon_mahasiswa ALTER COLUMN email TYPE text;
ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN email TYPE text;


ALTER TABLE konfigurasi ALTER COLUMN keterangan TYPE text;
ALTER TABLE new_audit.konfigurasi__audit ALTER COLUMN keterangan TYPE text;


ALTER TABLE konfigurasi ALTER COLUMN nilai TYPE text;
ALTER TABLE new_audit.konfigurasi__audit ALTER COLUMN nilai TYPE text;



ALTER TABLE konfigurasi ALTER COLUMN nilaidikunci TYPE text;
ALTER TABLE new_audit.konfigurasi__audit ALTER COLUMN nilaidikunci TYPE text;

ALTER TABLE konfigurasi ALTER COLUMN nilai TYPE text;
ALTER TABLE new_audit.konfigurasi__audit ALTER COLUMN nilai TYPE text;


ALTER TABLE konfigurasi ALTER COLUMN keterangan TYPE text;
ALTER TABLE new_audit.konfigurasi__audit ALTER COLUMN keterangan TYPE text;



ALTER TABLE kegiatan ALTER COLUMN program TYPE text;
ALTER TABLE new_audit.kegiatan__audit ALTER COLUMN program TYPE text;


ALTER TABLE detail_biaya ALTER COLUMN keterangan TYPE text;
ALTER TABLE new_audit.detail_biaya__audit ALTER COLUMN keterangan TYPE text;

ALTER TABLE detail_biaya ALTER COLUMN nama TYPE text;
ALTER TABLE new_audit.detail_biaya__audit ALTER COLUMN nama TYPE text;


ALTER TABLE asset.pemesanan_pengadaan_master_asset ALTER COLUMN pengirimanpalinglambat TYPE TIMESTAMP;
ALTER TABLE new_audit.pemesanan_pengadaan_master_asset__audit ALTER COLUMN pengirimanpalinglambat TYPE TIMESTAMP;



-- update or delete on table "penerimaan_pengadaan_master_asset" violates foreign key constraint "fk8fb763b8f0061a78" on table "uang_muka"

     alter table uang_muka
drop constraint fk8fb763b8f0061a78,
add constraint fk8fb763b8f0061a78
   foreign key (penerimaan_pengadaan_master_asset)
   references asset.penerimaan_pengadaan_master_asset(id)
   on delete set null;


-- update or delete on table "va" violates foreign key constraint "fk666dddc2bfbf678b" on table "virtual_account_bank"

     alter table virtual_account_bank
drop constraint fk666dddc2bfbf678b,
add constraint fk666dddc2bfbf678b
   foreign key (va)
   references va(id)
   on delete set null;

 -- update or delete on table "jenis_jabatan" violates foreign key constraint "fkd716b2a95728e262" on table "pejabat"
 
 
     alter table rab.jenis_jabatan
drop constraint fkd716b2a95728e262,
add constraint fkd716b2a95728e262
   foreign key (pejabat)
   references rab.pejabat(id)
   on delete set null;
   
   
-- update or delete on table "pejabat" violates foreign key constraint "fk709462864d58bbe2" on table "alur_persetujuan_surat_masuk_status"


     alter table surat.alur_persetujuan_surat_masuk_status
drop constraint fk709462864d58bbe2,
add constraint fk709462864d58bbe2
   foreign key (pejabat)
   references rab.pejabat(id)
   on delete cascade;
   

-- update or delete on table "pejabat" violates foreign key constraint "fk99d330b34d58bbe2" on table "alur_persetujuan_surat_keluar_status"


     alter table surat.alur_persetujuan_surat_keluar_status
drop constraint fk99d330b34d58bbe2,
add constraint fk99d330b34d58bbe2
   foreign key (pejabat)
   references rab.pejabat(id)
   on delete cascade;

-- update or delete on table "workspace" violates foreign key constraint "fk5cdd7d855dfcbba" on table "permintaan_pengadaan_master_asset"

     alter table asset.permintaan_pengadaan_master_asset
drop constraint fk5cdd7d855dfcbba,
add constraint fk5cdd7d855dfcbba
   foreign key (workspace)
   references rab.workspace(id)
   on delete set null;
   
   --update or delete on table "workspace" violates foreign key constraint "fkfe965d745dfcbba" on table "pemesanan_pengadaan_master_asset"
   
        alter table asset.pemesanan_pengadaan_master_asset
drop constraint fkfe965d745dfcbba,
add constraint fkfe965d745dfcbba
   foreign key (workspace)
   references rab.workspace(id)
   on delete set null;
   
   
   -- update or delete on table "workspace" violates foreign key constraint "fk32576df05dfcbba" on table "penerimaan_pengadaan_master_asset"
  
          alter table asset.penerimaan_pengadaan_master_asset
drop constraint fk32576df05dfcbba,
add constraint fk32576df05dfcbba
   foreign key (workspace)
   references rab.workspace(id)
   on delete set null;
   
   -- update or delete on table "pembelian_anggota_koperasi" violates foreign key constraint "fk6d82b8c6c76fe29a" on table "draft_pembelian_anggota_koperasi"
   
   alter table koperasi.draft_pembelian_anggota_koperasi
   drop constraint fk6d82b8c6c76fe29a,
add constraint fk6d82b8c6c76fe29a
   foreign key (lunas)
   references koperasi.pembelian_anggota_koperasi(id)
   on delete cascade;
   
   -- update or delete on table "draft_pembelian_anggota_koperasi" violates foreign key constraint "fkcff1f7681569c694" on table "pembelian_anggota_koperasi"
   
      alter table koperasi.pembelian_anggota_koperasi
   drop constraint fkcff1f7681569c694,
add constraint fkcff1f7681569c694
   foreign key (draft_pembelian_anggota_koperasi)
   references koperasi.draft_pembelian_anggota_koperasi(id)
   on delete cascade;
   
   -- update or delete on table "draft_pembelian_anggota_koperasi" violates foreign key constraint "fk4a93ff271569c694" on table "draft_pembelian"
   
      alter table koperasi.draft_pembelian
drop constraint fk4a93ff271569c694,
add constraint fk4a93ff271569c694
   foreign key (draft_pembelian_anggota_koperasi)
   references koperasi.draft_pembelian_anggota_koperasi(id)
   on delete cascade;
   
   
   --update or delete on table "pembelian" violates foreign key constraint "fk4a93ff273cbab95" on table "draft_pembelian"
   
   alter table koperasi.draft_pembelian
drop constraint fk4a93ff273cbab95,
add constraint fk4a93ff273cbab95
   foreign key (lunas)
   references koperasi.pembelian(id)
   on delete set null;
   
alter table pengaduan alter column jenis_pengaduan drop not null;
 alter table pengaduan alter column nama drop not null;
 
 
 alter table koperasi.aturan_diskon alter column produk drop not null;
 
alter table akunting.proses_transfer alter column  bank_sumber_id drop not null;
 alter table new_audit.proses_transfer__audit alter column  bank_sumber_id drop not null;
 
 
      alter table akunting.daftar_pengajuan_transfer
drop constraint fk584d1e62886812dd,
add constraint fk584d1e62886812dd
   foreign key (proses_transfer)
   references akunting.proses_transfer(id)
   on delete set null;
 
  alter table uang_muka
drop constraint fk8fb763b82ebd8e34,
add constraint fk8fb763b82ebd8e34
   foreign key (daftar_pengajuan_transfer)
   references akunting.daftar_pengajuan_transfer(id)
   on delete set null;

   
    -- update or delete on table "sekolah" violates foreign key constraint "fk3618d933fa3b2d4" on table "pengumuman_akademis"
  
          alter table pengumuman_akademis
drop constraint fk3618d933fa3b2d4,
add constraint fk3618d933fa3b2d4
   foreign key (sekolah)
   references sekolah.sekolah(id)
   on delete set null;
   
ALTER TABLE sekolah.guru ALTER COLUMN jenis_kelamin TYPE text;
ALTER TABLE new_audit.guru__audit ALTER COLUMN jenis_kelamin TYPE text;

alter table sekolah.guru alter column jenis_kelamin drop not null;

alter table disposisi_alur_sop alter column alur_sop drop not null;
alter table disposisi_alur_sop alter column disposisi_sop drop not null;

alter table rab.penggunaan_anggaran alter column ref drop not null;

--  update or delete on table "sop" violates foreign key constraint "fkfa121cdb48ff6b9" on table "dokumen_alur_sop"


          alter table dokumen_alur_sop
drop constraint fkfa121cdb48ff6b9,
add constraint fkfa121cdb48ff6b9
   foreign key (sop)
   references sop(id)
   on delete cascade;
   
   --  update or delete on table "alur_sop" violates foreign key constraint "fka80d3862f18d5c5a" on table "alur_sop_has_parameter"
   
           alter table alur_sop_has_parameter
drop constraint fka80d3862f18d5c5a,
add constraint fka80d3862f18d5c5a
   foreign key (alur_sop)
   references alur_sop(id)
   on delete cascade;
   
   --  update or delete on table "tbmuser" violates foreign key constraint "fk85fb75fd2c3936ff" on table "log_mobile"
   
           alter table log_mobile
drop constraint fk85fb75fd2c3936ff,
add constraint fk85fb75fd2c3936ff
   foreign key (tbmuser)
   references tbmuser(userid)
  on update cascade on delete cascade;
  
  
  --  update or delete on table "disposisi_sop" violates foreign key constraint "fk5d2d20d3bc255eb8" on table "penggantian_kas_kecil"
  
             alter table akunting.penggantian_kas_kecil
drop constraint fk5d2d20d3bc255eb8,
add constraint fk5d2d20d3bc255eb8
   foreign key (disposisi_sop)
   references disposisi_sop(id)
   on delete cascade;
   
   -- update or delete on table "disposisi_sop" violates foreign key constraint "fk9155a136bc255eb8" on table "proses_transfer"
   
                alter table akunting.proses_transfer
drop constraint fk9155a136bc255eb8,
add constraint fk9155a136bc255eb8
   foreign key (disposisi_sop)
   references disposisi_sop(id)
   on delete cascade;
   
   --  update or delete on table "disposisi_sop" violates foreign key constraint "fkd9cca834bc255eb8" on table "pembayaran_pengadaan_master_asset"
   
   alter table asset.pembayaran_pengadaan_master_asset
drop constraint fkd9cca834bc255eb8,
add constraint fkd9cca834bc255eb8
   foreign key (disposisi_sop)
   references disposisi_sop(id)
   on delete cascade;
   
   --  update or delete on table "disposisi_sop" violates foreign key constraint "fkd05508dcbc255eb8" on table "cuti_dan_izin"
   -- update or delete on table "disposisi_sop" violates foreign key constraint "fkd05508dcbc255eb8" on table "cuti_dan_izin"
   
     alter table payroll.cuti_dan_izin
drop constraint fkd05508dcbc255eb8,
add constraint fkd05508dcbc255eb8
   foreign key (disposisi_sop)
   references disposisi_sop(id)
   on delete cascade; 
   
   
   -- update or delete on table "kegiatan" violates foreign key constraint "fk666dddc234a1992d" on table "virtual_account_bank"
   
   alter table virtual_account_bank
drop constraint fk666dddc234a1992d,
add constraint fk666dddc234a1992d
   foreign key (kegiatan)
   references kegiatan(id)
   on delete cascade; 
   
   -- update or delete on table "peminjaman_surat_item_detail" violates foreign key constraint "fk382f6dd1619f9c22" on table "kembali_surat_item_detail"
   
    alter table surat.kembali_surat_item_detail
drop constraint fk382f6dd1619f9c22,
add constraint fk382f6dd1619f9c22
   foreign key (peminjaman_surat_item_detail)
   references surat.peminjaman_surat_item_detail(id)
   on delete cascade;   
   
   -- update or delete on table "peminjaman_surat_item_detail" violates foreign key constraint "fk2705b83a619f9c22" on table "detail_transaksi"
   
       alter table library.detail_transaksi
drop constraint fk2705b83a619f9c22,
add constraint fk2705b83a619f9c22
   foreign key (peminjaman_surat_item_detail)
   references surat.peminjaman_surat_item_detail(id)
   on delete set null;
   
   -- update or delete on table "kembali_surat_item_detail" violates foreign key constraint "fk2705b83a4e85b830" on table "detail_transaksi"
   
   
   
          alter table library.detail_transaksi
drop constraint fk2705b83a4e85b830,
add constraint fk2705b83a4e85b830
   foreign key (kembali_surat_item_detail)
   references surat.kembali_surat_item_detail(id)
   on delete set null;
   
   
   -- update or delete on table "peminjaman_surat_item" violates foreign key constraint "fka348e2bf71880b11" on table "kembali_surat_item"
   
             alter table surat.kembali_surat_item
drop constraint fka348e2bf71880b11,
add constraint fka348e2bf71880b11
   foreign key (peminjaman_surat_item)
   references surat.peminjaman_surat_item(id)
   on delete cascade;
   
   -- update or delete on table "kembali_surat_item_detail" violates foreign key constraint "fk1044c1e84e85b830" on table "peminjaman_surat_item_detail
   
       alter table surat.peminjaman_surat_item_detail
drop constraint fk1044c1e84e85b830,
add constraint fk1044c1e84e85b830
   foreign key (kembali_surat_item_detail)
   references surat.kembali_surat_item_detail(id)
   on delete set null;
   
   --  update or delete on table "kembali_surat_item" violates foreign key constraint "fke619ea88f07eb32d" on table "peminjaman_surat_item"
   
   alter table surat.peminjaman_surat_item
drop constraint fke619ea88f07eb32d,
add constraint fke619ea88f07eb32d
   foreign key (kembali_surat_item)
   references surat.kembali_surat_item(id)
   on delete set null;
   
   
   -- update or delete on table "kembali_surat_item" violates foreign key constraint "fk382f6dd1f07eb32d" on table "kembali_surat_item_detail"
   
   
             
   alter table surat.kembali_surat_item_detail
drop constraint fk382f6dd1f07eb32d,
add constraint fk382f6dd1f07eb32d
   foreign key (kembali_surat_item)
   references surat.kembali_surat_item(id)
   on delete cascade;
   
   
   -- update or delete on table "siswa" violates foreign key constraint "fk85fb75fd1ab864e8" on table "log_mobile"
   
   alter table log_mobile
drop constraint fk85fb75fd1ab864e8,
add constraint fk85fb75fd1ab864e8
   foreign key (siswa)
   references sekolah.siswa(id)
   on delete cascade;
   
   
   -- update or delete on table "peminjaman_surat_item" violates foreign key constraint "fk1044c1e871880b11" on table "peminjaman_surat_item_detail"
   
      alter table surat.peminjaman_surat_item_detail
drop constraint fk1044c1e871880b11,
add constraint fk1044c1e871880b11
   foreign key (peminjaman_surat_item)
   references surat.peminjaman_surat_item(id)
   on delete cascade;
   
   
   --  update or delete on table "disposisi_sop" violates foreign key constraint "fka348e2bfbc255eb8" on table "kembali_surat_item"
   
    alter table surat.kembali_surat_item
drop constraint fka348e2bfbc255eb8,
add constraint fka348e2bfbc255eb8
   foreign key (disposisi_sop)
   references disposisi_sop(id)
   on delete cascade;
   
   
   -- update or delete on table "disposisi_sop" violates foreign key constraint "fke619ea88bc255eb8" on table "peminjaman_surat_item"
   
   
   
       alter table surat.peminjaman_surat_item
drop constraint fke619ea88bc255eb8,
add constraint fke619ea88bc255eb8
   foreign key (disposisi_sop)
   references disposisi_sop(id)
   on delete cascade;
   
   
   -- update or delete on table "kurikulum_sekolah" violates foreign key constraint "fk6135e04dbfdda4d" on table "kelas"
   
   alter table sekolah.kelas
drop constraint fk6135e04dbfdda4d,
add constraint fk6135e04dbfdda4d
   foreign key (kurikulum_sekolah_id)
   references sekolah.kurikulum_sekolah(id)
   on delete set null;
   
   
   -- update or delete on table "gelombang_pendaftaran" violates foreign key constraint "fk3c93a8832ba8921a" on table "paket_punya_gelombang_pendaftaran"
   
          alter table paket_punya_gelombang_pendaftaran
drop constraint fk3c93a8832ba8921a,
add constraint fk3c93a8832ba8921a
   foreign key (gelombang_pendaftaran)
   references gelombang_pendaftaran(id)
   on delete cascade;
   
   
   -- update or delete on table "tagihan" violates foreign key constraint "fk461556e39b5315e6" on table "bni_request_detail"
   
   alter table bni_request_detail
drop constraint fk461556e39b5315e6,
add constraint fk461556e39b5315e6
   foreign key (tagihan)
   references sekolah.tagihan(id)
   on delete cascade;
   
   -- update or delete on table "disposisi_sop" violates foreign key constraint "fk5d2d20d3bc255eb8" on table "penggantian_kas_kecil"
   
      alter table akunting.penggantian_kas_kecil
drop constraint fk5d2d20d3bc255eb8,
add constraint fk5d2d20d3bc255eb8
   foreign key (disposisi_sop)
   references disposisi_sop(id)
   on delete cascade;
   
   -- update or delete on table "disposisi_sop" violates foreign key constraint "fkc088a44abc255eb8" on table "kas_kecil"
   
         alter table akunting.kas_kecil
drop constraint fkc088a44abc255eb8,
add constraint fkc088a44abc255eb8
   foreign key (disposisi_sop)
   references disposisi_sop(id)
   on delete cascade;
   
   
   -- update or delete on table "jenis_shift_pegawai" violates foreign key constraint "fkf143218341a717be" on table "detail_jenis_shift_pegawai"
   
            alter table payroll.detail_jenis_shift_pegawai
drop constraint fkf143218341a717be,
add constraint fkf143218341a717be
   foreign key (jenis_shift_pegawai)
   references payroll.jenis_shift_pegawai(id)
   on delete cascade;
   
   
   --  update or delete on table "jenis_shift_pegawai" violates foreign key constraint "fkf1432183de8ea772" on table "detail_jenis_shift_pegawai"
   
    alter table payroll.detail_jenis_shift_pegawai
drop constraint fkf1432183de8ea772,
add constraint fkf1432183de8ea772
   foreign key (jenis_shift_pegawai)
   references payroll.jenis_shift_pegawai(id)
   on delete cascade;
   
   -- update or delete on table "detail_jenis_shift_pegawai" violates foreign key constraint "fk6463daf7ce67d5b3" on table "status_kehadiran_karyawan_harian"
   
   
       alter table payroll.status_kehadiran_karyawan_harian
drop constraint fk6463daf7ce67d5b3,
add constraint fk6463daf7ce67d5b3
   foreign key (detail_jenis_shift_pegawai)
   references payroll.detail_jenis_shift_pegawai(id)
   on delete cascade;
   
   --update or delete on table "stok_awal" violates foreign key constraint "fk2dae72ab9ad4b254" on table "detail_transaksi_asset"
      
       alter table asset.detail_transaksi_asset
drop constraint fk2dae72ab9ad4b254,
add constraint fk2dae72ab9ad4b254
   foreign key (stok_awal)
   references asset.stok_awal(id)
   on delete cascade;
   
   --  update or delete on table "orang_tua" violates foreign key constraint "fka5c4b40ad2f33ab6" on table "tbmuser"
   
          alter table tbmuser
drop constraint fka5c4b40ad2f33ab6,
add constraint fka5c4b40ad2f33ab6
   foreign key (orang_tua)
   references orang_tua(id)
   on delete set null;
   
   
 --   update or delete on table "orang_tua" violates foreign key constraint "fkd6ecbaf0d2f33ab6" on table "pegawai"
 
 
    
          alter table pegawai
drop constraint fkd6ecbaf0d2f33ab6,
add constraint fkd6ecbaf0d2f33ab6
   foreign key (orang_tua)
   references orang_tua(id)
   on delete set null;
   
   
   
   ALTER TABLE public.menu
    ALTER COLUMN id TYPE bigint;

ALTER TABLE public.menu
    ALTER COLUMN child TYPE bigint;

ALTER TABLE public.menu
    ALTER COLUMN root TYPE bigint;
	
	
	ALTER TABLE public.job_has_menu
    ALTER COLUMN menu TYPE bigint;
	
	ALTER TABLE public.role_privilage
    ALTER COLUMN menu TYPE bigint;
	
	
	-- update or delete on table "tagihan" violates foreign key constraint "fka4d0de6f9b5315e6" on table "grup_transaksi"
	
	
	       alter table akunting.grup_transaksi
drop constraint fka4d0de6f9b5315e6,
add constraint fka4d0de6f9b5315e6
   foreign key (tagihan)
   references sekolah.tagihan(id)
   on delete cascade;
   
   
   -- update or delete on table "detail_transaksi_pasien" violates foreign key constraint "fk59635826a5b56bf" on table "biaya"
   

       alter table sirs.biaya
drop constraint fk59635826a5b56bf,
add constraint fk59635826a5b56bf
   foreign key (detail_transaksi)
   references sirs.detail_transaksi_pasien(id)
   on delete set null;
   
   
   -- update or delete on table "detail_transaksi_layanan" violates foreign key constraint "fk5963582f772c582" on table "biaya"
   
   
       alter table sirs.biaya
drop constraint fk5963582f772c582,
add constraint fk5963582f772c582
   foreign key (detail_transaksi_layanan)
   references sirs.detail_transaksi_layanan(id)
   on delete set null;
   
   
   -- update or delete on table "kegiatan" violates foreign key constraint "fk666dddc234a1992d" on table "virtual_account_bank"
   
   	       alter table virtual_account_bank
drop constraint fk666dddc234a1992d,
add constraint fk666dddc234a1992d
   foreign key (kegiatan)
   references kegiatan(id)
   on delete cascade;
   
   -- update or delete on table "detail_setting_biaya" violates foreign key constraint "fk33037014b229979d" on table "detail_biaya"
   
      	       alter table detail_biaya
drop constraint fk33037014b229979d,
add constraint fk33037014b229979d
   foreign key (detail_setting_biaya)
   references detail_setting_biaya(id)
   on delete set null;
   
   --  update or delete on table "setting_biaya" violates foreign key constraint "fk3a9c336523361fda" on table "detail_setting_biaya"
   
        	       alter table detail_setting_biaya
drop constraint fk3a9c336523361fda,
add constraint fk3a9c336523361fda
   foreign key (setting_biaya)
   references setting_biaya(id)
   on delete cascade; 
   
   -- update or delete on table "jenis_pengajuan" violates foreign key constraint "fkb98040a6a1dceb4c" on table "pengajuan_mahasiswa"
   
   
         	       alter table pengajuan_mahasiswa
drop constraint fkb98040a6a1dceb4c,
add constraint fkb98040a6a1dceb4c
   foreign key (jenis_pengajuan)
   references jenis_pengajuan(id)
   on delete cascade;
   
   
   -- update or delete on table "setting_biaya" violates foreign key constraint "fkab1d273d23361fda" on table "setting_biaya_detail"
   
   alter table setting_biaya_detail
drop constraint fkab1d273d23361fda,
add constraint fkab1d273d23361fda
   foreign key (setting_biaya)
   references setting_biaya(id)
   on delete cascade;
   
   
   
   
   -- update or delete on table "transaksi_koperasi_detail" violates foreign key constraint "fkec200ae3707e6b7" on table "pembayaran_anggota_koperasi_detail"
   
      
   alter table koperasi.pembayaran_anggota_koperasi_detail
drop constraint fkec200ae3707e6b7,
add constraint fkec200ae3707e6b7
   foreign key (transaksi_koperasi_detail)
   references koperasi.transaksi_koperasi_detail(id)
   on delete set null;
   
   
   -- update or delete on table "transaksi_koperasi" violates foreign key constraint "fk584d1e62210c1f60" on table "daftar_pengajuan_transfer"
   
      alter table akunting.daftar_pengajuan_transfer
drop constraint fk584d1e62210c1f60,
add constraint fk584d1e62210c1f60
   foreign key (transaksi_koperasi)
   references koperasi.transaksi_koperasi(id)
   on delete cascade;
   
   
   --  update or delete on table "pembayaran_anggota_koperasi" violates foreign key constraint "fkec200ae3c240319e" on table "pembayaran_anggota_koperasi_detail"
   
   
         alter table koperasi.pembayaran_anggota_koperasi_detail
drop constraint fkec200ae3c240319e,
add constraint fkec200ae3c240319e
   foreign key (pembayaran_anggotaKoperasi_id)
   references koperasi.pembayaran_anggota_koperasi(id)
   on delete set null;
   
   
   -- update or delete on table "pembayaran_anggota_koperasi_detail" violates foreign key constraint "fkca52d97b8be50326" on table "transaksi_koperasi_detail"
   
            alter table koperasi.transaksi_koperasi_detail
drop constraint fkca52d97b8be50326,
add constraint fkca52d97b8be50326
   foreign key (pembayaran_anggota_koperasi_detail)
   references koperasi.pembayaran_anggota_koperasi_detail(id)
   on delete set null;
   
   -- update or delete on table "pembelian_anggota_koperasi" violates foreign key constraint "fkccb60459139170b" on table "pembelian"
   
   alter table koperasi.pembelian
drop constraint fkccb60459139170b,
add constraint fkccb60459139170b
   foreign key (pembelian_anggota_koperasi)
   references koperasi.pembelian_anggota_koperasi(id)
   on delete cascade;
   
   
   -- update or delete on table "disposisi_sop" violates foreign key constraint "fke923869cbc255eb8" on table "perbaikan_asset"
   
      alter table asset.perbaikan_asset
drop constraint fke923869cbc255eb8,
add constraint fke923869cbc255eb8
   foreign key (disposisi_sop)
   references disposisi_sop(id)
   on delete cascade;
   
   -- update or delete on table "disposisi_sop" violates foreign key constraint "fk331d7928bc255eb8" on table "penyedia_asset"
   
         alter table asset.penyedia_asset
drop constraint fk331d7928bc255eb8,
add constraint fk331d7928bc255eb8
   foreign key (disposisi_sop)
   references disposisi_sop(id)
   on delete cascade;


-- update or delete on table "permintaan_pengadaan_master_asset_detail" violates foreign key constraint "fkef7f694e2b2dbf8f" on table "penggunaan_anggaran"

         alter table rab.penggunaan_anggaran
drop constraint fkef7f694e2b2dbf8f,
add constraint fkef7f694e2b2dbf8f
   foreign key (permintaan_pengadaan_master_asset_detail)
   references asset.permintaan_pengadaan_master_asset_detail(id)
   on delete cascade;
   
   -- update or delete on table "disposisi_sop" violates foreign key constraint "fkef7f694ebc255eb8" on table "penggunaan_anggaran"
   
            alter table rab.penggunaan_anggaran
drop constraint fkef7f694ebc255eb8,
add constraint fkef7f694ebc255eb8
   foreign key (disposisi_sop)
   references disposisi_sop(id)
   on delete cascade;
   
   
   -- update or delete on table "detail_setting_biaya" violates foreign key constraint "fk33037014b229979d" on table "detail_biaya"
   
               alter table detail_biaya
drop constraint fk33037014b229979d,
add constraint fk33037014b229979d
   foreign key (detail_setting_biaya)
   references detail_setting_biaya(id)
   on delete cascade;
   
   
   --  update or delete on table "biodata_calon_mahasiswa" violates foreign key constraint "fka70972faaafa7aaa" on table "mahasiswa"
   
                  alter table mahasiswa
drop constraint fka70972faaafa7aaa,
add constraint fka70972faaafa7aaa
   foreign key (biodata_calon_mahasiswa_long)
   references biodata_calon_mahasiswa(id)
   on delete cascade;
   
   
   -- update or delete on table "paket" violates foreign key constraint "fk3c93a883e24c8b3d" on table "paket_punya_gelombang_pendaftaran"
   
                     alter table paket_punya_gelombang_pendaftaran
drop constraint fk3c93a883e24c8b3d,
add constraint fk3c93a883e24c8b3d
   foreign key (paket)
   references paket(id)
   on delete cascade;
   
   
   -- update or delete on table "paket" violates foreign key constraint "fkc46e59de24c8b3d" on table "upload_biodata_calon_mahasiswa"
   
   alter table upload_biodata_calon_mahasiswa
drop constraint fkc46e59de24c8b3d,
add constraint fkc46e59de24c8b3d
   foreign key (paket)
   references paket(id)
   on delete cascade;
   
   -- update or delete on table "disposisi_sop" violates foreign key constraint "fk3c6fc756bc255eb8" on table "pengecualian_jadwal_penilaian_dosen"
   
      alter table pengecualian_jadwal_penilaian_dosen
drop constraint fk3c6fc756bc255eb8,
add constraint fk3c6fc756bc255eb8
   foreign key (disposisi_sop)
   references disposisi_sop(id)
   on delete cascade;
   
   -- update or delete on table "pembayaran_gaji_punya_pegawai" violates foreign key constraint "fkdf6d3e9752aed9d1" on table "pembayaran_item_gaji_pegawai"
   
   
         alter table payroll.pembayaran_item_gaji_pegawai
drop constraint fkdf6d3e9752aed9d1,
add constraint fkdf6d3e9752aed9d1
   foreign key (pembayaran_gaji_punya_pegawai)
   references payroll.pembayaran_gaji_punya_pegawai(id)
   on delete cascade;
   
   
   -- update or delete on table "pembayaran_gaji" violates foreign key constraint "fk584d1e628b7251cf" on table "daftar_pengajuan_transfer"
   
   alter table akunting.daftar_pengajuan_transfer
drop constraint fk584d1e628b7251cf,
add constraint fk584d1e628b7251cf
   foreign key (pembayaran_gaji)
   references payroll.pembayaran_gaji(id)
   on delete cascade;
   
   -- update or delete on table "item_gaji_pegawai" violates foreign key constraint "fkdf6d3e97c655936b" on table "pembayaran_item_gaji_pegawai"
   
      alter table payroll.pembayaran_item_gaji_pegawai
drop constraint fkdf6d3e97c655936b,
add constraint fkdf6d3e97c655936b
   foreign key (item_gaji)
   references payroll.item_gaji_pegawai(id)
   on delete cascade;
   
   
   -- update or delete on table "biodata_calon_mahasiswa" violates foreign key constraint "fk86036ebc1e553bb2" on table "interview_punya_calon_mahasiswa"

alter table interview_punya_calon_mahasiswa
drop constraint fk86036ebc1e553bb2,
add constraint fk86036ebc1e553bb2
   foreign key (calon_mahasiswa)
   references biodata_calon_mahasiswa(id)
   on delete cascade;
   
   
-- update or delete on table "status_kehadiran_karyawan_harian" violates foreign key constraint "fk6463daf7ddb7e490" on table "status_kehadiran_karyawan_harian"
alter table status_kehadiran_karyawan_harian
drop constraint fk6463daf7ddb7e490,
add constraint fk6463daf7ddb7e490
   foreign key (next)
   references status_kehadiran_karyawan_harian(id)
   on delete set null;
   
alter table status_kehadiran_karyawan_harian
drop constraint fk6463daf7ddb25e84,
add constraint fk6463daf7ddb25e84
   foreign key (back)
   references status_kehadiran_karyawan_harian(id)
   on delete set null;
   
   
ALTER TABLE surat.alur_persetujuan_surat_keluar_status ALTER COLUMN keterangan TYPE text;
ALTER TABLE new_audit.alur_persetujuan_surat_keluar_status__audit  ALTER COLUMN keterangan TYPE text;

ALTER TABLE surat.alur_persetujuan_surat_masuk_status ALTER COLUMN keterangan TYPE text;
ALTER TABLE new_audit.alur_persetujuan_surat_masuk_status__audit  ALTER COLUMN keterangan TYPE text;


-- update or delete on table "workspace" violates foreign key constraint "fkef7f694e5dfcbba" on table "penggunaan_anggaran"
alter table rab.penggunaan_anggaran
drop constraint fkef7f694e5dfcbba,
add constraint fkef7f694e5dfcbba
   foreign key (workspace)
   references rab.workspace(id)
   on delete cascade;
   
-- update or delete on table "master_asset" violates foreign key constraint "fk29f51b952b0f2bae" on table "pemakaian_master_asset_detail"

alter table asset.pemakaian_master_asset_detail
drop constraint fk29f51b952b0f2bae,
add constraint fk29f51b952b0f2bae
   foreign key (master_asset)
   references asset.master_asset(id)
   on delete cascade;
   
   
-- update or delete on table "master_asset" violates foreign key constraint "fk2dae72aba4a272a9" on table "detail_transaksi_asset"

alter table asset.detail_transaksi_asset
drop constraint fk2dae72aba4a272a9,
add constraint fk2dae72aba4a272a9
   foreign key (master_asset)
   references asset.master_asset(id)
   on delete cascade;
   
   --  update or delete on table "master_asset" violates foreign key constraint "fk7ad8d3752b0f2bae" on table "saldo_awal_master_asset_detail"
   
   
   alter table asset.saldo_awal_master_asset_detail
drop constraint fk7ad8d3752b0f2bae,
add constraint fk7ad8d3752b0f2bae
   foreign key (master_asset)
   references asset.master_asset(id)
   on delete cascade;
   
   -- update or delete on table "saldo_awal_master_asset_detail" violates foreign key constraint "fk2dae72aba09f31fd" on table "detail_transaksi_asset"
     
	 alter table asset.detail_transaksi_asset
drop constraint fk2dae72aba09f31fd,
add constraint fk2dae72aba09f31fd
   foreign key (saldo_awal_master_asset_detail)
   references asset.saldo_awal_master_asset_detail(id)
   on delete cascade;
   
   --  update or delete on table "pemakaian_master_asset_detail" violates foreign key constraint "fk2dae72abcfaf4890" on table "detail_transaksi_asset"
   
   	 alter table asset.detail_transaksi_asset
drop constraint fk2dae72abcfaf4890,
add constraint fk2dae72abcfaf4890
   foreign key (pemakaian_master_asset_detail)
   references asset.pemakaian_master_asset_detail(id)
   on delete cascade;
   
   
ALTER TABLE pengaturan_pembayaran_bulanan ALTER COLUMN keterangan TYPE text;
ALTER TABLE new_audit.pengaturan_pembayaran_bulanan__audit  ALTER COLUMN keterangan TYPE text;

ALTER TABLE asset.permintaan_pengadaan_master_asset ALTER COLUMN keterangan TYPE text;
ALTER TABLE new_audit.permintaan_pengadaan_master_asset__audit  ALTER COLUMN keterangan TYPE text;

ALTER TABLE virtual_account_bank ALTER COLUMN keterangan TYPE text;
ALTER TABLE new_audit.virtual_account_bank__audit  ALTER COLUMN keterangan TYPE text;


-- update or delete on table "penyedia_asset" violates foreign key constraint "fk39c493cd10bf5ce" on table "penyedia_asset_punya_dokumen"

alter table asset.penyedia_asset_punya_dokumen
drop constraint fk39c493cd10bf5ce,
add constraint fk39c493cd10bf5ce
   foreign key (penyedia_asset)
   references asset.penyedia_asset(id)
   on delete cascade;
   
-- update or delete on table "penyedia_asset" violates foreign key constraint "fka5c4b40ad10bf5ce" on table "tbmuser"

alter table tbmuser
drop constraint fka5c4b40ad10bf5ce,
add constraint fka5c4b40ad10bf5ce
   foreign key (penyedia_asset)
   references asset.penyedia_asset(id)
   on delete set null;
   
   
-- update or delete on table "dosen" violates foreign key constraint "fk2c2e749fa1106e88" on table "fakultas"

alter table fakultas
drop constraint fk2c2e749fa1106e88,
add constraint fk2c2e749fa1106e88
   foreign key (pudek1)
   references dosen(id)
   on delete set null;
   


   
alter table fakultas
drop constraint fk2c2e749fa1106e89,
add constraint fk2c2e749fa1106e89
   foreign key (pudek2)
   references dosen(id)
   on delete set null;

   
alter table fakultas
drop constraint fk2c2e749fa1106e8a,
add constraint fk2c2e749fa1106e8a
   foreign key (pudek3)
   references dosen(id)
   on delete set null;
   
   
 --   update or delete on table "permintaan_pengadaan_master_asset_detail" violates foreign key constraint "fkef7f694e2b2dbf8f" on table "penggunaan_anggaran"
 
 
 alter table rab.penggunaan_anggaran
drop constraint fkef7f694e2b2dbf8f,
add constraint fkef7f694e2b2dbf8f
   foreign key (permintaan_pengadaan_master_asset_detail)
   references asset.permintaan_pengadaan_master_asset_detail(id)
   on delete cascade;
   

--  duplicate key value violates unique constraint "pemesanan_pengadaan_master_asset_kode_key"

ALTER TABLE asset.pemesanan_pengadaan_master_asset DROP CONSTRAINT pemesanan_pengadaan_master_asset_kode_key;


ALTER TABLE biodata_calon_mahasiswa DROP CONSTRAINT biodata_calon_mahasiswa_no_registrasi_key;


--CREATE INDEX idx_ais_flags_data_key_pattern
--ON ais_flags_data (flag_key text_pattern_ops);

CREATE INDEX CONCURRENTLY idx_ais_flags_data_varchar_pattern 
ON ais_flags_data (flag_key varchar_pattern_ops);


-- duplicate key value violates unique constraint "alur_persetujuan_surat_masuk_status_kodeunik_key"

ALTER TABLE surat.alur_persetujuan_surat_masuk_status DROP CONSTRAINT alur_persetujuan_surat_masuk_status_kodeunik_key;

--   duplicate key value violates unique constraint "ruang_paket_pmb_kode_unik_key"


ALTER TABLE ruang_paket_pmb DROP CONSTRAINT ruang_paket_pmb_kode_unik_key;


--  duplicate key value violates unique constraint "perkuliahan_feeder_kode_key"

ALTER TABLE perkuliahan DROP CONSTRAINT perkuliahan_feeder_kode_key;



CREATE UNIQUE INDEX ref_penggunaan_anggaran ON rab.penggunaan_anggaran(ref);


ALTER TABLE sekolah.tagihan DROP CONSTRAINT tagihan_pembayaran_siswa_detail_id_key;

--  update or delete on table "tagihan" violates foreign key constraint "fke50285e89b5315e6" on table "pembayaran_siswa_detail"
 alter table sekolah.pembayaran_siswa_detail
drop constraint fke50285e89b5315e6,
add constraint fke50285e89b5315e6
   foreign key (tagihan)
   references sekolah.tagihan(id)
   on delete set null;
   

 alter table akunting.grup_transaksi
drop constraint fka4d0de6f9b5315e6,
add constraint fka4d0de6f9b5315e6
   foreign key (tagihan)
   references sekolah.tagihan(id)
   on delete cascade;
   
 alter table sekolah.pengaturan_biaya_punya_siswa
drop constraint fkf5eeee286a3bee35,
add constraint fkf5eeee286a3bee35
   foreign key (calon_siswa)
   references sekolah.calon_siswa(id)
   on delete cascade;
   
 alter table deposit
drop constraint fk5ca7169e6a3bee35,
add constraint fk5ca7169e6a3bee35
   foreign key (calon_siswa)
   references sekolah.calon_siswa(id)
   on delete cascade;
   
   
-- update or delete on table "disposisi_sop" violates foreign key constraint "fk86889624bc255eb8" on table "pendataan_pelanggaran_pegawai"
 alter table employ.pendataan_pelanggaran_pegawai
drop constraint fk86889624bc255eb8,
add constraint fk86889624bc255eb8
   foreign key (disposisi_sop)
   references disposisi_sop(id)
   on delete cascade;
   
   
-- update or delete on table "pendataan_pelanggaran_pegawai" violates foreign key constraint "fk2ae30de8a21b52fc" on table "pelanggaran_pegawai_dan_hukuman_has_hukuman"
alter table employ.pelanggaran_pegawai_dan_hukuman_has_hukuman
drop constraint fk2ae30de8a21b52fc,
add constraint fk2ae30de8a21b52fc
   foreign key (pelanggaran_dan_hukuman_pegawai)
   references employ.pendataan_pelanggaran_pegawai(id)
   on delete cascade;
   
-- update or delete on table "pendataan_pelanggaran_pegawai" violates foreign key constraint "fkdd83c627376e077c" on table "pelanggaran_pegawai_dan_hukuman_has_pelanggaran"
alter table employ.pelanggaran_pegawai_dan_hukuman_has_pelanggaran
drop constraint fkdd83c627376e077c,
add constraint fkdd83c627376e077c
   foreign key (pelanggaran_pegawai_dan_hukuman)
   references employ.pendataan_pelanggaran_pegawai(id)
   on delete cascade;
   
   
   
-- update or delete on table "siswa" violates foreign key constraint "fk666dddc21ab864e8" on table "virtual_account_bank"

alter table virtual_account_bank
drop constraint fk666dddc21ab864e8,
add constraint fk666dddc21ab864e8
   foreign key (siswa)
   references sekolah.siswa(id)
   on delete cascade;
   
   
   -- update or delete on table "siswa" violates foreign key constraint "fk649c3f273bbf9874" on table "apresiasi_siswa"
   alter table sekolah.apresiasi_siswa
drop constraint fk649c3f273bbf9874,
add constraint fk649c3f273bbf9874
   foreign key (siswa_id)
   references sekolah.siswa(id)
   on delete cascade;
   
   
   --  update or delete on table "siswa" violates foreign key constraint "fk216b75b31ab864e8" on table "absen_piket_detail"
      alter table sekolah.absen_piket_detail
drop constraint fk216b75b31ab864e8,
add constraint fk216b75b31ab864e8
   foreign key (siswa)
   references sekolah.siswa(id)
   on delete cascade;
   
   
   
   -- update or delete on table "siswa" violates foreign key constraint "fk99714f6a3bbf9874" on table "pelanggaran_siswa"
   
         alter table sekolah.pelanggaran_siswa
drop constraint fk99714f6a3bbf9874,
add constraint fk99714f6a3bbf9874
   foreign key (siswa_id)
   references sekolah.siswa(id)
   on delete cascade;
   
   
   -- update or delete on table "siswa" violates foreign key constraint "fkf52bad5e1ab864e8" on table "diskon_siswa_punya_siswa"
   
   
            alter table sekolah.diskon_siswa_punya_siswa
drop constraint fkf52bad5e1ab864e8,
add constraint fkf52bad5e1ab864e8
   foreign key (siswa)
   references sekolah.siswa(id)
   on delete cascade;
   
   
   --  update or delete on table "apresiasi_siswa" violates foreign key constraint "fkbec2bf355f7438f5" on table "apresiasi_siswa_has_penghargaan"
   
               alter table sekolah.apresiasi_siswa_has_penghargaan
drop constraint fkbec2bf355f7438f5,
add constraint fkbec2bf355f7438f5
   foreign key (apresiasi_siswa)
   references sekolah.apresiasi_siswa(id)
   on delete cascade;
   
   
   --  update or delete on table "diskon_mahasiswa" violates foreign key constraint "fk59bea98af448ea3b" on table "detail_kegiatan"
   alter table detail_kegiatan
drop constraint fk59bea98af448ea3b,
add constraint fk59bea98af448ea3b
   foreign key (diskon_mahasiswa_data)
   references diskon_mahasiswa(id)
   on delete cascade;
   
   
   
   
   -- 1) Buat sequence-nya kembali (aman bila sudah ada)
CREATE SEQUENCE IF NOT EXISTS public.detail_log_login_id_seq;

-- 2) Jadikan DEFAULT kolom id → INSERT tanpa id akan memanggil nextval
ALTER TABLE public.detail_log_login
    ALTER COLUMN id SET DEFAULT nextval('public.detail_log_login_id_seq'::regclass);

-- 3) Kaitkan kepemilikan sequence ke kolom (ikut terhapus bila kolom dihapus)
ALTER SEQUENCE public.detail_log_login_id_seq OWNED BY public.detail_log_login.id;

-- 4) Selaraskan nilai sequence dengan id terbesar yang sudah ada (cegah tabrakan PK)
SELECT setval('public.detail_log_login_id_seq',
              COALESCE((SELECT MAX(id) FROM public.detail_log_login), 0) + 1,
              false);
			  
			  
			  
			  
			  
			  
ALTER TABLE library.item ALTER COLUMN by_statement TYPE text;
ALTER TABLE library.item ALTER COLUMN link         TYPE text;
ALTER TABLE library.item ALTER COLUMN keterangan   TYPE text;
ALTER TABLE library.item ALTER COLUMN penaklikan   TYPE text;
ALTER TABLE library.item ALTER COLUMN tempatterbit TYPE text;


ALTER TABLE new_audit.item__audit  ALTER COLUMN by_statement TYPE text;
ALTER TABLE new_audit.item__audit  ALTER COLUMN link TYPE text;
ALTER TABLE new_audit.item__audit  ALTER COLUMN keterangan TYPE text;
ALTER TABLE new_audit.item__audit  ALTER COLUMN penaklikan TYPE text;
ALTER TABLE new_audit.item__audit  ALTER COLUMN tempatterbit TYPE text;




-- 1) TABEL UTAMA: public.biodata_calon_mahasiswa ------------------------------
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name   = 'biodata_calon_mahasiswa'
          AND data_type    = 'character varying'
    LOOP
        EXECUTE format(
            'ALTER TABLE public.biodata_calon_mahasiswa ALTER COLUMN %I TYPE text',
            r.column_name
        );
        RAISE NOTICE '[UTAMA]  % -> text', r.column_name;
    END LOOP;
END $$;

-- 2) TABEL AUDIT: new_audit.biodata_calon_mahasiswa__audit --------------------
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT column_name
        FROM information_schema.columns
        WHERE table_schema = 'new_audit'
          AND table_name   = 'biodata_calon_mahasiswa__audit'
          AND data_type    = 'character varying'
    LOOP
        EXECUTE format(
            'ALTER TABLE new_audit.biodata_calon_mahasiswa__audit ALTER COLUMN %I TYPE text',
            r.column_name
        );
        RAISE NOTICE '[AUDIT]  % -> text', r.column_name;
    END LOOP;
END $$;