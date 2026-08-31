package ais.service.registration;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.tenant.JenisUsahaTenant;
import ais.database.model.tenant.JenisUsahaTenantModule;

/**
 * <h3>Seed idempoten katalog Jenis Usaha tenant + mapping module bundle (database-driven).</h3>
 *
 * <p>Dipanggil dari {@code PendaftaranTenantServlet.init()} (load-on-startup) -- aman dipanggil
 * berulang: baris dicek per {@code code} (jenis usaha) dan per pasangan {@code (jenisUsaha,
 * moduleCode)} (mapping), HANYA insert yang belum ada; baris existing TIDAK ditimpa supaya
 * penyuntingan Platform Admin (nama/deskripsi/urutan/aktif) tidak dibatalkan restart.</p>
 *
 * <p>Isi bundle = §6.3 + §7 dokumen ERD paket prompt (14 jenis). Jenis pendidikan
 * (SEKOLAH/PERGURUAN_TINGGI/PESANTREN) di-set {@code requiresManualReview=true}: pemetaan ke
 * produk eCampus/eSchool/ePesantren existing menyangkut data institusi nyata, perlu
 * persetujuan admin (§8.3 "jenis usaha yang membutuhkan verifikasi khusus").</p>
 */
public final class JenisUsahaTenantSeedService {

	/** Kelas utilitas statis murni (semua anggota {@code static}); konstruktor privat mencegah instansiasi. */
	private JenisUsahaTenantSeedService() {
	}

	private static volatile boolean sudahSeed = false;

	/** [code, nama, deskripsi, icon, requiresManualReview(Y/T)] -- urutan tampil = urutan array. */
	private static final String[][] JENIS = {
			{ "APOTEK", "Apotek", "Penjualan obat resep/non-resep dengan batch & kedaluwarsa.", "local_pharmacy", "T" },
			{ "INVENTORY_SALES", "Inventory / Sales", "Distribusi & penjualan sales keliling (paritas 48 layar).", "inventory", "T" },
			{ "BENGKEL_MOBIL", "Bengkel Mobil", "Servis kendaraan: work order, mekanik, spare part.", "car_repair", "T" },
			{ "RETAIL_POS", "Toko / Retail POS", "Toko/gerai retail dengan kasir POS.", "storefront", "T" },
			{ "GUDANG_DISTRIBUSI", "Gudang / Distribusi", "Pergudangan, penerimaan, dan transfer stok.", "warehouse", "T" },
			{ "RESTORAN_KANTIN", "Restoran / Kantin", "POS F&B, menu/resep, bahan baku, meja/order.", "restaurant", "T" },
			{ "KOPERASI", "Koperasi", "Anggota, simpanan, dan unit usaha koperasi.", "groups", "T" },
			{ "KLINIK", "Klinik", "Pasien, pendaftaran, pelayanan, farmasi, billing.", "medical_services", "T" },
			{ "HOTEL_PENGINAPAN", "Hotel / Penginapan", "Kamar, reservasi, check-in/out, POS.", "hotel", "T" },
			{ "LOGISTIK", "Logistik", "Shipment, rute, gudang, pengiriman.", "local_shipping", "T" },
			{ "SEKOLAH", "Sekolah", "Manajemen sekolah (eSchool) sesuai entitlement.", "school", "Y" },
			{ "PERGURUAN_TINGGI", "Perguruan Tinggi", "Manajemen kampus (eCampus) sesuai entitlement.", "account_balance", "Y" },
			{ "PESANTREN", "Pesantren", "Manajemen pesantren (ePesantren) sesuai entitlement.", "mosque", "Y" },
			{ "LAINNYA", "Jasa Lainnya", "Jenis usaha lain -- jelaskan pada kolom keterangan.", "more_horiz", "T" },
	};

	/** [codeJenis, moduleCode...] -- union entitlement awal per jenis usaha (ERD §6.3/§7). */
	private static final String[][] MODUL = {
			{ "APOTEK", "POS", "MASTER_PRODUK", "INVENTORY", "BATCH_EXPIRY", "PEMBELIAN", "SUPPLIER",
					"CUSTOMER", "SALES", "HUTANG", "PIUTANG", "KAS_JURNAL", "LAPORAN", "FARMASI" },
			{ "INVENTORY_SALES", "POS", "MASTER_SUPPLIER", "MASTER_CUSTOMER", "MASTER_SALES", "PRODUK",
					"PERSEDIAAN", "STOK_OPNAME", "MASTER_HARGA", "PEMBELIAN", "HUTANG", "PENJUALAN",
					"PIUTANG", "NOTA_SALES", "KAS_JURNAL", "LABA_RUGI" },
			{ "BENGKEL_MOBIL", "CUSTOMER", "KENDARAAN", "WORK_ORDER", "JASA_SERVIS", "MEKANIK",
					"SPARE_PART", "INVENTORY", "PEMBELIAN", "SUPPLIER", "POS", "HUTANG_PIUTANG",
					"KAS_JURNAL", "LAPORAN_BENGKEL" },
			{ "RETAIL_POS", "POS", "PRODUK", "PROMO", "CUSTOMER", "STOK", "LAPORAN" },
			{ "GUDANG_DISTRIBUSI", "GUDANG", "TRANSFER_STOK", "PENERIMAAN", "PEMBELIAN", "PENJUALAN", "STOK" },
			{ "RESTORAN_KANTIN", "POS_FNB", "MENU_RESEP", "BAHAN_BAKU", "PRODUKSI", "MEJA_ORDER" },
			{ "KOPERASI", "ANGGOTA", "SIMPANAN", "HUTANG", "POS" },
			{ "KLINIK", "PASIEN", "PENDAFTARAN_PASIEN", "PELAYANAN", "FARMASI", "BILLING" },
			{ "HOTEL_PENGINAPAN", "PROPERTI", "KAMAR", "RESERVASI", "CHECKIN_CHECKOUT", "POS" },
			{ "LOGISTIK", "SHIPMENT", "RUTE", "GUDANG", "PENGIRIMAN" },
			{ "SEKOLAH", "ESCHOOL" },
			{ "PERGURUAN_TINGGI", "ECAMPUS" },
			{ "PESANTREN", "EPESANTREN" },
			{ "LAINNYA", "PROFIL" },
	};

	/** Jalankan seed sekali per JVM (idempoten di DB, guard memori hanya menghemat query startup). */
	public static void pastikanSeed() {
		if (sudahSeed) {
			return;
		}
		synchronized (JenisUsahaTenantSeedService.class) {
			if (sudahSeed) {
				return;
			}
			try {
				seed();
				sudahSeed = true;
			} catch (Exception e) {
				// Seed gagal TIDAK boleh menjatuhkan startup servlet -- katalog kosong membuat
				// wizard menolak submit (fail-closed), admin bisa memicu ulang lewat restart.
				ais.common.ErrorAuditUtil.record(e, "auto-audit JenisUsahaTenantSeedService.pastikanSeed");
			}
		}
	}

	private static void seed() {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			int urut = 0;
			for (String[] j : JENIS) {
				urut++;
				JenisUsahaTenant jenis = (JenisUsahaTenant) session.createCriteria(JenisUsahaTenant.class)
						.add(Restrictions.eq("code", j[0])).setMaxResults(1).uniqueResult();
				if (jenis == null) {
					jenis = new JenisUsahaTenant();
					jenis.setCode(j[0]);
					jenis.setNama(j[1]);
					jenis.setDeskripsi(j[2]);
					jenis.setIcon(j[3]);
					jenis.setDisplayOrder(Integer.valueOf(urut * 10));
					jenis.setAktif(Boolean.TRUE);
					jenis.setRequiresManualReview(Boolean.valueOf("Y".equals(j[4])));
					jenis.setDefaultModuleBundleCode(j[0]);
					jenis.setCreatedAt(new Date());
					jenis.setOleh("seed");
					jenis.setOlehId("seed");
					session.save(jenis);
				}
				seedModul(session, jenis);
			}
			session.getTransaction().commit();
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static void seedModul(Session session, JenisUsahaTenant jenis) {
		String[] baris = null;
		for (String[] m : MODUL) {
			if (m[0].equals(jenis.getCode())) {
				baris = m;
				break;
			}
		}
		if (baris == null) {
			return;
		}
		List<?> existing = session.createCriteria(JenisUsahaTenantModule.class)
				.add(Restrictions.eq("jenisUsahaTenant.id", jenis.getId())).list();
		for (int i = 1; i < baris.length; i++) {
			String moduleCode = baris[i];
			boolean ada = false;
			for (Object o : existing) {
				if (moduleCode.equals(((JenisUsahaTenantModule) o).getModuleCode())) {
					ada = true;
					break;
				}
			}
			if (ada) {
				continue;
			}
			JenisUsahaTenantModule m = new JenisUsahaTenantModule();
			m.setJenisUsahaTenant(jenis);
			m.setModuleCode(moduleCode);
			m.setDefaultEnabled(Boolean.TRUE);
			m.setRequired(Boolean.valueOf(i == 1));
			m.setDisplayOrder(Integer.valueOf(i * 10));
			m.setOleh("seed");
			m.setOlehId("seed");
			session.save(m);
		}
	}
}
