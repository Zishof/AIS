-- Menu.id dipisahkan dari kode hierarchy child/root. Gagal keras pada collision.
BEGIN;
DO $$
DECLARE c bigint;
BEGIN
 SELECT count(*) INTO c FROM public.menu
 WHERE (id BETWEEN 2000460500 AND 2000460528
        AND NOT ((id=2000460500 AND child=4605 AND root=46)
              OR (id BETWEEN 2000460501 AND 2000460528 AND child=id-2000000000 AND root=4605)))
    OR (child=4605 AND id<>2000460500)
    OR (child BETWEEN 460501 AND 460528 AND id<>2000000000+child)
    OR (lower(coalesce(url,'')) LIKE '/jurnal/admin/%'
        AND id NOT BETWEEN 2000460500 AND 2000460528);
 IF c<>0 THEN RAISE EXCEPTION 'Journal menu collision detected (% rows); seed aborted',c; END IF;
END $$;

INSERT INTO public.menu(id,label,big_icon,child,root,url,nomorurut,aktif)
VALUES (2000460500,'Jurnal','/img/svg/book.svg',4605,46,'/jurnal/admin/dashboard',0,true)
ON CONFLICT(id) DO NOTHING;

INSERT INTO public.menu(id,label,big_icon,child,root,url,nomorurut,aktif) VALUES
(2000460501,'Dashboard Jurnal','/img/svg/dashboard-chart.svg',460501,4605,'/jurnal/admin/dashboard',1,true),
(2000460502,'Master Jurnal','/img/svg/book.svg',460502,4605,'/jurnal/admin/masterJurnal',2,true),
(2000460503,'Bagian dan Kategori','/img/svg/table-list.svg',460503,4605,'/jurnal/admin/bagianKategori',3,true),
(2000460504,'Edisi dan Daftar Isi','/img/svg/table-list.svg',460504,4605,'/jurnal/admin/edisiDaftarIsi',4,true),
(2000460505,'Naskah dan Submission','/img/svg/pencil-square.svg',460505,4605,'/jurnal/admin/submission',5,true),
(2000460506,'Penugasan Editor','/img/svg/user-tie.svg',460506,4605,'/jurnal/admin/penugasanEditor',6,true),
(2000460507,'Reviewer dan Keahlian','/img/svg/user-tie.svg',460507,4605,'/jurnal/admin/reviewerKeahlian',7,true),
(2000460508,'Form dan Proses Review','/img/svg/check-square.svg',460508,4605,'/jurnal/admin/prosesReview',8,true),
(2000460509,'Copyediting','/img/svg/pencil-square.svg',460509,4605,'/jurnal/admin/copyediting',9,true),
(2000460510,'Produksi, Proof, dan Galley','/img/svg/table-list.svg',460510,4605,'/jurnal/admin/produksiGalley',10,true),
(2000460511,'Artikel dan Versi Publikasi','/img/svg/book.svg',460511,4605,'/jurnal/admin/publikasi',11,true),
(2000460512,'DOI, URN, dan Identifier','/img/svg/key.svg',460512,4605,'/jurnal/admin/identifier',12,true),
(2000460513,'Pengguna, Peran, dan Undangan','/img/svg/user-tie.svg',460513,4605,'/jurnal/admin/penggunaPeran',13,true),
(2000460514,'Pengumuman dan Sorotan','/img/svg/table-list.svg',460514,4605,'/jurnal/admin/pengumuman',14,true),
(2000460515,'Situs, Halaman, dan Navigasi','/img/svg/table-list.svg',460515,4605,'/jurnal/admin/situsNavigasi',15,true),
(2000460516,'Email dan Notifikasi','/img/svg/table-list.svg',460516,4605,'/jurnal/admin/emailNotifikasi',16,true),
(2000460517,'Langganan dan Hak Akses','/img/svg/key.svg',460517,4605,'/jurnal/admin/langganan',17,true),
(2000460518,'Pembayaran Jurnal','/img/svg/money-bills.svg',460518,4605,'/jurnal/admin/pembayaran',18,true),
(2000460519,'Statistik dan COUNTER','/img/svg/dashboard-chart.svg',460519,4605,'/jurnal/admin/statistik',19,true),
(2000460520,'Plugin dan Integrasi','/img/svg/table-list.svg',460520,4605,'/jurnal/admin/pluginIntegrasi',20,true),
(2000460521,'Import dari OJS','/img/svg/table-list.svg',460521,4605,'/jurnal/admin/importOjs',21,true),
(2000460522,'Pemetaan dan Rekonsiliasi Import','/img/svg/table-list.svg',460522,4605,'/jurnal/admin/rekonsiliasiImport',22,true),
(2000460523,'Laporan Jurnal','/img/svg/dashboard-chart.svg',460523,4605,'/jurnal/admin/laporan',23,true),
(2000460524,'Pengaturan Workflow','/img/svg/config-icon.svg',460524,4605,'/jurnal/admin/workflow',24,true),
(2000460525,'Template dan Kosakata','/img/svg/table-list.svg',460525,4605,'/jurnal/admin/templateKosakata',25,true),
(2000460526,'Job, Antrian, dan Integrasi Gagal','/img/svg/table-list.svg',460526,4605,'/jurnal/admin/jobIntegrasi',26,true),
(2000460527,'Audit Trail Jurnal','/img/svg/table-list.svg',460527,4605,'/jurnal/admin/audit',27,true),
(2000460528,'Pengaturan Sistem Jurnal','/img/svg/config-icon.svg',460528,4605,'/jurnal/admin/sistem',28,true)
ON CONFLICT(id) DO NOTHING;

-- Hanya pintu menu untuk administrator existing. Capability bisnis tetap
-- default-deny sampai jurnal_akses_json diatur pada tab TbmroleAction.
INSERT INTO public.job_has_menu(job,menu)
SELECT 'am',id FROM public.menu WHERE id BETWEEN 2000460500 AND 2000460528
ON CONFLICT DO NOTHING;
COMMIT;
