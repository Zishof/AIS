package ais.common;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.report.format1.sekolah.LaporanDepositSiswa;
import ais.action.report.format1.sekolah.LaporanPembayaranSiswa;
import ais.action.report.format1.sekolah.LaporanPembelianSiswa;
import ais.action.report.format1.sekolah.LaporanRaporSiswa;
import ais.action.report.format1.sekolah.LaporanRekapAbsenPiketPerTgl;
import ais.action.report.format1.sekolah.LaporanRekapAbsenSiswaPerTgl;
import ais.action.report.format1.sekolah.LaporanRekapPembayaranSiswa;
import ais.action.report.format1.sekolah.LaporanRincianPembayaranSiswa;
import ais.action.report.format1.sekolah.LaporanSaldoSiswa;
import ais.action.report.format1.sekolah.LaporanTunggakanSiswa;
import ais.database.model.Menu;
import ais.database.model.RolePrivilage;
import ais.database.model.Tbmrole;

// Pastikan import model sesuai package Anda
// import com.yourpackage.Menu;
// import com.yourpackage.Tbmrole;
// import com.yourpackage.RolePrivilage;
// import com.yourpackage.ConstantValues; 

public class MenuInitializer {

	public static void initMenus(Session session) {
		
		
//		String sqlAlterMenu = "DO $$ \n" +
//			    "BEGIN \n" +
//			    "    -- Alter kolom 'id' \n" +
//			    "    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'menu' AND column_name = 'id' AND data_type = 'integer') THEN \n" +
//			    "        ALTER TABLE public.menu ALTER COLUMN id TYPE bigint; \n" +
//			    "    END IF; \n" +
//			    "    \n" +
//			    "    -- Alter kolom 'root' \n" +
//			    "    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'menu' AND column_name = 'root' AND data_type = 'integer') THEN \n" +
//			    "        ALTER TABLE public.menu ALTER COLUMN root TYPE bigint; \n" +
//			    "    END IF; \n" +
//			    "    \n" +
//			    "    -- Alter kolom 'child' \n" +
//			    "    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'menu' AND column_name = 'child' AND data_type = 'integer') THEN \n" +
//			    "        ALTER TABLE public.menu ALTER COLUMN child TYPE bigint; \n" +
//			    "    END IF; \n" +
//			    "END $$;";
//
//			// Panggil method updateSql dengan timeout 0 (atau sesuai kebutuhan Anda)
//			ais.common.Common.updateSql60Menit(sqlAlterMenu);
		
		

		createMenu(session, 4266616L, "Aktifitas Harian Siswa", "/img/users16x16.png", 5701233L, 5701L,
				"/pages/master/sekolah/aktiftas_harian_siswa.zul", 14, true);

		// --- 1. Menu: Kelas Les Siswa ---
		createMenu(session, 18618L, "Kelas Les Siswa", "/img/User-Group-icon.png", 570106L, 5701L,
				"/pages/master/sekolah/kelas_les_siswa.zul", true);

		// Update Privilege untuk Menu 18618
		addPrivilegeToRoles(session, new Long[] { 18618L }, new String[] { "am", "keu", "amp", "admsek", "Akademik" });

		// --- 2. Menu Group: Siswa (Beranda, Kehadiran, Kuesioner, Rapor, dll) ---
		createMenu(session, 87657724L, "Beranda Siswa", "/img/Client-icon.png", 73L, 0L, null, false);

		createMenu(session, 87657725L, "Kehadiran Siswa", "/img/Document-icon.png", 73000L, 73L,
				LaporanRekapAbsenSiswaPerTgl.class.getName(), true);

		createMenu(session, 431898L, "Kuesioner Siswa", "/img/svg/pencil-square.svg", 73003L, 73L,
				"/pages/master/kuesioner_siswa.zul", true);

		createMenu(session, 127616L, "Rapor Siswa", "/img/users16x16.png", 73023L, 73L,
				LaporanRaporSiswa.class.getName(), true);

		createMenu(session, 48916L, "Catatan Siswa", "/img/users16x16.png", 73021L, 73L,
				"/pages/master/sekolah/catatan_siswa.zul", true);

		// --- 3. Setup Role Siswa ---
		ensureSiswaRoleAndPrivileges(session);

		// --- 4. Menu Group: Absensi & Jadwal ---
		createMenu(session, 8187125L, "Absen dan Kunjungan Siswa", "/img/Document-icon.png", 73000L, 73L,
				LaporanRekapAbsenPiketPerTgl.class.getName(), true);

		createMenu(session, 87657726L, "Jadwal Pelajaran", "/img/Document-icon.png", 73003L, 73L,
				"/pages/master/sekolah/jadwal_pelajaran.zul", true);

		// --- 5. Menu Group: Keuangan & Tabungan Siswa ---
		createMenu(session, 87657727L, "Tagihan Siswa", "/img/Document-icon.png", 73010L, 73L,
				"/pages/master/sekolah/tagihan.zul", true);

		createMenu(session, 87657728L, "Pembayaran Siswa", "/img/Document-icon.png", 73010L, 73L,
				"/pages/master/sekolah/pembayaran_siswa.zul", true);

		createMenu(session, 87657729L, "Pembayaran Online", "/img/Document-icon.png", 73015L, 73L,
				"/pages/master/sekolah/pembayaran_online.zul", true);

		createMenu(session, 87657730L, "Tabungan Siswa", "/img/Document-icon.png", 73020L, 73L,
				"/pages/master/sekolah/deposit_siswa.zul", true);

		createMenu(session, 872257730L, "Belanja Siswa", "/img/Document-icon.png", 73021L, 73L,
				"/common/mobile/pembelian.zul", true);

		// --- 6. Menu Group: Laporan Siswa ---
		createMenu(session, 87657735L, "Laporan Siswa", "/img/Document-icon.png", 788L, 0L, null, true);

		createMenu(session, 673271L, "Laporan Pembayaran Siswa", "/img/invoice-icon.png", 78800L, 788L,
				LaporanPembayaranSiswa.class.getName(), true);

		createMenu(session, 67327177L, "Laporan Tunggakan Siswa", "/img/invoice-icon.png", 78820L, 788L,
				LaporanTunggakanSiswa.class.getName(), true);

		createMenu(session, 67327111L, "Laporan Belanja Siswa", "/img/invoice-icon.png", 78801L, 788L,
				LaporanPembelianSiswa.class.getName(), true);

		createMenu(session, 673272L, "Laporan Rekap Pembayaran Siswa", "/img/invoice-icon.png", 78805L, 788L,
				LaporanRincianPembayaranSiswa.class.getName(), true);

		createMenu(session, 673273L, "Laporan Deposit Siswa", "/img/invoice-icon.png", 78806L, 788L,
				LaporanDepositSiswa.class.getName(), true);

		createMenu(session, 67327213L, "Laporan Saldo Siswa", "/img/invoice-icon.png", 78807L, 788L,
				LaporanSaldoSiswa.class.getName(), true);

		// --- 7. Menu Group: Inventory ---
		createMenu(session, 23887724L, "Sistem Inventory", "/img/Document-icon.png", 53L, 0L, null, false);
		createMenu(session, 23887725L, "Setup", "/img/Document-icon.png", 5300L, 53L, null, true);
		createMenu(session, 23865988L, "Toko dan Pedagang", "/img/Mimetypes-x-office-calendar-icon.png", 53000L, 5300L,
				"/pages/master/inventory/toko.zul", true);
		createMenu(session, 23881248L, "Jenis Produk", "/img/Mimetypes-x-office-calendar-icon.png", 530000L, 5300L,
				"/pages/master/inventory/jenis_produk.zul", true);
		createMenu(session, 23887726L, "Pendataan", "/img/Document-icon.png", 5301L, 53L, null, true);
		createMenu(session, 23834227L, "Produk", "/img/Document-icon.png", 53010L, 5301L,
				"/pages/master/inventory/produk.zul", true);
		// Menu terpisah persis di bawah Produk (sibling dlm grup Pendataan yg sama; SENGAJA bukan
		// anak dari node Produk -- nilai child 53010 milik Produk TABRAKAN dgn "Penjualan ke Siswa"
		// [data lama], menyarangkan di bawahnya membuat menu ini muncul ganda di dua cabang).
		createMenu(session, 23834228L, "Grup Produk", "/img/Mimetypes-x-office-calendar-icon.png", 53011L, 5301L,
				"/pages/master/inventory/grup_produk.zul", true);
		createMenu(session, 238854326L, "Transaksi", "/img/Document-icon.png", 5302L, 53L, null, true);
		createMenu(session, 238854316L, "Penjualan ke Siswa", "/img/Document-icon.png", 53010L, 5302L,
				"/pages/master/inventory/pembelian.zul", true);
		createMenu(session, 2980326L, "Laporan", "/img/Document-icon.png", 5303L, 53L, null, true);
		createMenu(session, 29803216L, "Laporan Belanja Siswa", "/img/Document-icon.png", 53030L, 5303L,
				LaporanPembelianSiswa.class.getName(), true);

		// --- 8. Menu Group: Sistem Sekolah ---
		createMenu(session, 887724L, "Sistem Sekolah", "/img/Document-icon.png", 57L, 0L, null, false);
		createMenu(session, 887725L, "Setup", "/img/Document-icon.png", 5700L, 57L, null, true);
		createMenu(session, 881248L, "Yayasan", "/img/Mimetypes-x-office-calendar-icon.png", 570000L, 5700L,
				"/pages/master/sekolah/yayasan.zul", true);
		createMenu(session, 881247L, "Jenis Sekolah", "/img/Mimetypes-x-office-calendar-icon.png", 570005L, 5700L,
				"/pages/master/sekolah/jenis_sekolah.zul", true);
		createMenu(session, 8813247L, "Penjurusan Sekolah", "/img/Mimetypes-x-office-calendar-icon.png", 570005L, 5700L,
				"/pages/master/sekolah/penjurusan_sekolah.zul", true);
		createMenu(session, 881249L, "Sekolah", "/img/house-icon.png", 570006L, 5700L,
				"/pages/master/sekolah/sekolah.zul", true);
		createMenu(session, 881229L, "Jenis Penilaian", "/img/Mimetypes-x-office-calendar-icon.png", 570007L, 5700L,
				"/pages/master/sekolah/jenis_penilaian.zul", true);
		createMenu(session, 891229L, "Jenis Guru", "/img/Mimetypes-x-office-calendar-icon.png", 570009L, 5700L,
				"/pages/master/sekolah/jenis_guru.zul", true);
		createMenu(session, 83459111L, "Kelompok Mata Pelajaran", "/img/Document-icon.png", 570008L, 5700L,
				"/pages/master/sekolah/kelompok_matapelajaran.zul", true);
		createMenu(session, 83459L, "Mata Pelajaran", "/img/Document-icon.png", 570008L, 5700L,
				"/pages/master/sekolah/matapelajaran.zul", true);
		createMenu(session, 865429L, "Jenis Jam Pelajaran", "/img/Mimetypes-x-office-calendar-icon.png", 570009L, 5700L,
				"/pages/master/sekolah/jenis_jadwal_pelajaran.zul", true);
		createMenu(session, 2345629L, "Jam Pelajaran", "/img/Mimetypes-x-office-calendar-icon.png", 570010L, 5700L,
				"/pages/master/sekolah/jam_pelajaran.zul", true);
		createMenu(session, 765899L, "Status Awal Siswa", "/img/Mimetypes-x-office-calendar-icon.png", 570011L, 5700L,
				"/pages/master/sekolah/status_awal_siswa.zul", true);
		createMenu(session, 276549L, "Konfigurasi Sekolah", "/img/config-icon.png", 570110L, 5700L,
				"/pages/master/konfigurasi_sekolah.zul", true);

		// --- 9. Menu Group: Pendataan Sekolah ---
		createMenu(session, 887726L, "Pendataan", "/img/Document-icon.png", 5701L, 57L, null, true);
		createMenu(session, 834227L, "Guru", "/img/User-Group-icon.png", 57010L, 5701L,
				"/pages/master/sekolah/guru.zul", true);
		createMenu(session, 887727L, "Siswa", "/img/User-Group-icon.png", 570100L, 5701L,
				"/pages/master/sekolah/siswa.zul", true);
		createMenu(session, 887728L, "Kelas Siswa", "/img/User-Group-icon.png", 570105L, 5701L,
				"/pages/master/sekolah/kelas_siswa.zul", true);
		createMenu(session, 542728L, "Jadwal Pelajaran", "/img/Mimetypes-x-office-calendar-icon.png", 570110L, 5701L,
				"/pages/master/sekolah/jadwal_pelajaran.zul", true);
		createMenu(session, 123448L, "Nilai Siswa", "/img/Mimetypes-x-office-calendar-icon.png", 570112L, 5701L,
				"/pages/master/sekolah/penilaian_siswa.zul", true);

		// --- 10. Menu Group: PSB (Penerimaan Siswa Baru) ---
		createMenu(session, 884322L, "Penerimaan Siswa Baru", "/img/Document-icon.png", 5702L, 57L, null, true);
		createMenu(session, 54327L, "Gelombang Pendaftaran", "/img/Mimetypes-x-office-calendar-icon.png", 570200L,
				5702L, "/pages/master/sekolah/gelombang_pendaftaran_psb.zul", true);
		createMenu(session, 187623L, "Kelompok Pendaftaran", "/img/Mimetypes-x-office-calendar-icon.png", 570201L,
				5702L, "/pages/master/sekolah/kelompok_pendaftaran.zul", true);
		createMenu(session, 18793L, "Calon Siswa", "/img/User-Group-icon.png", 570202L, 5702L,
				"/pages/master/sekolah/calon_siswa.zul", true);

		// --- 11. Menu Group: Keuangan Sekolah ---
		createMenu(session, 65884322L, "Keuangan Siswa", "/img/Document-icon.png", 5705L, 57L, null, true);
		createMenu(session, 6518793L, "Item Biaya", "/img/Money-icon.png", 570501L, 5705L,
				"/pages/master/sekolah/item_biaya_sekolah.zul", true);
		createMenu(session, 6518792L, "Jenis Biaya / Pembayaran", "/img/Money-Calculator-icon.png", 570502L, 5705L,
				"/pages/master/sekolah/jenis_biaya_sekolah.zul", true);
		createMenu(session, 6518796L, "Tagihan Pembayaran", "/img/Money-Bag-icon.png", 570503L, 5705L,
				"/pages/master/sekolah/pengaturan_biaya_sekolah.zul", true);
		createMenu(session, 65181292L, "Cara Pembayaran", "/img/Money-Calculator-icon.png", 570504L, 5705L,
				"/pages/master/sekolah/akun_pembayaran_siswa.zul", true);

		// Menu Group: Proses Pembayaran (Dengan Override Label/URL)
		createMenu(session, 65198762L, "Proses Pembayaran", "/img/Money-Calculator-icon.png", 570505L, 5705L, null,
				true);

		createMenu(session, 8755592L, "Pembayaran Siswa", "/img/Client-icon.png", 57050500L, 570505L,
				"/pages/master/sekolah/pem_online.zul?lbl_siswa=true", true);

		createMenu(session, 8755593L, "Pembayaran Calon Siswa", "/img/User-Preppy-Red-icon.png", 57050501L, 570505L,
				"/pages/master/sekolah/pem_online.zul?lbl_calon_siswa=true", true);

		createMenu(session, 8325592L, "Deposit Siswa", "/img/Client-icon.png", 57050502L, 570505L,
				"/pages/master/sekolah/deposit_siswa.zul", true);

		createMenu(session, 8555572L, "Posting Pembayaran Siswa", "/img/unit-completed-icon.png", 57050505L, 570505L,
				"/pages/master/sekolah/posting_pembayaran.zul", true);

		createMenu(session, 8555573L, "Posting Deposit Siswa", "/img/unit-completed-icon.png", 57050506L, 570505L,
				"/pages/master/sekolah/posting_deposit.zul", true);

		createMenu(session, 87532882L, "Proses Pembayaran", "/img/online-icon.png", 57050510L, 570505L,
				"/pages/master/sekolah/pembayaran_online.zul", true);

		// --- 12. Menu Group: Laporan Keuangan ---
		createMenu(session, 6769092L, "Laporan Pembayaran", "/img/invoice-icon.png", 570508L, 5705L, null, true);
		createMenu(session, 6769091L, "Pembayaran Siswa/Calon", "/img/invoice-icon.png", 57050801L, 570508L,
				LaporanPembayaranSiswa.class.getName(), true);
		createMenu(session, 676909177L, "Tunggakan Siswa", "/img/invoice-icon.png", 57050821L, 570508L,
				LaporanTunggakanSiswa.class.getName(), true);
		createMenu(session, 677681L, "Rekap Pembayaran Siswa/Calon", "/img/invoice-icon.png", 57050802L, 570508L,
				LaporanRincianPembayaranSiswa.class.getName(), true);
		createMenu(session, 645681L, "Deposit Siswa/Calon", "/img/invoice-icon.png", 57050803L, 570508L,
				LaporanDepositSiswa.class.getName(), true);
		createMenu(session, 765411L, "Laporan Belanja Siswa", "/img/invoice-icon.png", 57050804L, 570508L,
				LaporanPembelianSiswa.class.getName(), true);
		createMenu(session, 67354613L, "Laporan Saldo Siswa", "/img/invoice-icon.png", 57050805L, 570508L,
				LaporanSaldoSiswa.class.getName(), true);
		createMenu(session, 673272211L, "Rekap Tagihan Siswa", "/img/invoice-icon.png", 57050822L, 570508L,
				LaporanRekapPembayaranSiswa.class.getName(), true);

		// --- 13. Update Privilege untuk Rekap Tagihan ---
		addPrivilegeToRoles(session, new Long[] { 673272211L, 4266616L }, new String[] { "am", "keu", "amp" });
	}

	// ==========================================
	// HELPER METHODS (Private)
	// ==========================================

	private static void createMenu(Session session, Long id, String label, String bigIcon, Long child, Long root,
			String url, boolean aktif) {
		Integer nomorUrut = 0;
		createMenu(session, id, label, bigIcon, child, root, url, nomorUrut, aktif);
	}

	/**
	 * Membuat menu jika belum ada di database. Jika sudah ada, method ini tidak
	 * melakukan apa-apa (untuk menghindari override setting manual user), kecuali
	 * jika Anda ingin memaksa update URL, silakan tambahkan session.update().
	 */
	private static void createMenu(Session session, Long id, String label, String bigIcon, Long child, Long root,
			String url, Integer nomorUrut, boolean aktif) {
		
		try {
			Number count = (Number) session.createCriteria(Menu.class).add(Restrictions.idEq(id))
					.setProjection(Projections.rowCount()).uniqueResult();

			if (count != null && count.intValue() == 0) {
				Menu myMenu = new Menu();
				myMenu.setId(id);
				myMenu.setLabel(label);
				myMenu.setBigIcon(bigIcon);
				myMenu.setChild(child);
				myMenu.setRoot(root);
				myMenu.setUrl(url);
				myMenu.setAktif(aktif);
				myMenu.setNomorUrut(nomorUrut);
				
				try {
					session.getTransaction().begin();
					session.save(myMenu);
					session.getTransaction().commit();
				} catch (Exception e) {
					// 1. Batal/kembalikan transaksi pada database
					if (session.getTransaction() != null && session.getTransaction().isActive()) {
						session.getTransaction().rollback();
					}
					// 2. BERSIHKAN CACHE SESSION (Wajib agar tidak merembet ke proses lain!)
					session.clear();
					
					// Print pesan error secara diam-diam tanpa di throw ke atas (agar proses lanjut)
					System.err.println("Gagal insert menu " + label + " (Kemungkinan duplikat/race condition). Proses dilanjutkan.");
				}
				
			} else {
				// Opsional: Jika Anda ingin memastikan URL selalu terupdate meskipun menu sudah ada
				if (url != null) {
					Menu existingMenu = (Menu) session.get(Menu.class, id);
					if (existingMenu != null && !url.equals(existingMenu.getUrl())) {
						existingMenu.setUrl(url);
						
						try {
							session.getTransaction().begin();
							session.update(existingMenu);
							session.getTransaction().commit();
						} catch (Exception e) {
							// Proteksi yang sama jika update gagal
							if (session.getTransaction() != null && session.getTransaction().isActive()) {
								session.getTransaction().rollback();
							}
							session.clear();
							System.err.println("Gagal update menu " + label + " . Proses dilanjutkan.");
						}
					}
				}
			}
		} catch (Exception globalEx) {
			// Menangkap galat saat Select/Get data awal
			if (session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			session.clear();
			System.err.println("Terjadi kesalahan sistem saat mengecek Menu " + label + " : " + globalEx.getMessage());
		}
	}

	/**
	 * Menambahkan privilege (RolePrivilage) untuk Role tertentu ke Menu tertentu.
	 */
	private static void addPrivilegeToRoles(Session session, Long[] menuIds, String[] roleIds) {
		for (String roleId : roleIds) {
			for (Long menuId : menuIds) {
				try {
					// Cek apakah privilege sudah ada
					Criteria criteria = session.createCriteria(RolePrivilage.class);
					criteria.createAlias("role", "r");
					criteria.createAlias("menu", "m");
					criteria.add(Restrictions.eq("r.roleId", roleId));
					criteria.add(Restrictions.eq("m.id", menuId));
					criteria.setMaxResults(1);

					RolePrivilage privilage = (RolePrivilage) criteria.uniqueResult();

					if (privilage == null) {
						Tbmrole tbmrole = (Tbmrole) session.get(Tbmrole.class, roleId);

						if (tbmrole != null) {
							// Update list menu di Role (jika mappingnya bidirectional)
							// tbmrole.getMenus().add(new Menu(menuId));
							// session.update(tbmrole);

							// Buat RolePrivilage baru
							privilage = new RolePrivilage();
							privilage.setCreate(1);
							privilage.setRead(1);
							privilage.setDelete(1);
							privilage.setUpdate(1);
							privilage.setRole(tbmrole);
							privilage.setMenu(new Menu(menuId));

							session.saveOrUpdate(privilage);
						}
					}
				} catch (Exception e) {
					// Log error tapi jangan stop proses loop
					System.err.println(
							"Error adding privilege Role: " + roleId + " Menu: " + menuId + " -> " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Khusus untuk inisialisasi Role Siswa dan menu-menu terkait e-learning.
	 */
	private static void ensureSiswaRoleAndPrivileges(Session session) {
		// 1. Cek Role Siswa
		Tbmrole roleSiswa = (Tbmrole) session.get(Tbmrole.class, Tbmrole.SISWA);
		if (roleSiswa == null) {
			roleSiswa = new Tbmrole();
			roleSiswa.setRoleId(Tbmrole.SISWA);
			roleSiswa.setRoleName("Siswa");
			roleSiswa.setElearning(true);
			session.save(roleSiswa);
		} else {
			// Pastikan flag elearning aktif
			if (!Boolean.TRUE.equals(roleSiswa.getElearning())) {
				roleSiswa.setElearning(true);
				session.update(roleSiswa);
			}
		}

		// 2. Tambahkan Privilege Khusus Siswa
		Long[] siswaMenus = new Long[] { 431898L, 127616L, 48916L };
		for (Long mId : siswaMenus) {
			Criteria c = session.createCriteria(RolePrivilage.class).createAlias("role", "r").createAlias("menu", "m")
					.add(Restrictions.eq("r.roleId", Tbmrole.SISWA)).add(Restrictions.eq("m.id", mId)).setMaxResults(1);

			RolePrivilage priv = (RolePrivilage) c.uniqueResult();

			if (priv == null) {
				// Update collection menu di role jika perlu
				// roleSiswa.getMenus().add(new Menu(mId));
				// session.update(roleSiswa);

				priv = new RolePrivilage();
				priv.setCreate(1);
				priv.setRead(1);
				priv.setDelete(1);
				priv.setUpdate(1);
				priv.setRole(roleSiswa); // Gunakan object yang sudah di-load/create
				priv.setMenu(new Menu(mId));
				session.save(priv);
			}
		}
	}
}