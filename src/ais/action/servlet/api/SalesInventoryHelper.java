package ais.action.servlet.api;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.common.EbisnisMenuKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/**
 * <h3>Helper varian "eBisnis Inventory &amp; Sales" (P1: fondasi role/menu/konteks aktor).</h3>
 *
 * <p>Semua logika bisnis aksi {@code si_*} hidup di sini (dipanggil {@link SalesInventoryApiDispatcher}),
 * mengikuti pembagian kerja yang sama dgn {@code KantinHelper} utk aksi POS lama. Konvensi status
 * juga sama ({@code "00"} sukses / {@code "91"} gagal + {@code description}) supaya
 * {@code PosApi.normalisasiStatusKantinHelper} bisa dipakai ulang tanpa cabang khusus.</p>
 */
public final class SalesInventoryHelper {

	private SalesInventoryHelper() {
	}

	// =============================================================================================
	// Seed role idempoten (FND-007) -- pola InitDataHelper (lookup by roleId -> create bila absen),
	// dipanggil dari ApiEBisnis.init() (sekali per load servlet; aman dipanggil berulang).
	// =============================================================================================

	private static volatile boolean seedSudahDicoba = false;

	/** Idempoten &amp; murah dipanggil berulang -- guard volatile per-JVM + lookup by PK di DB. */
	public static void pastikanSeedRole() {
		if (seedSudahDicoba) {
			return;
		}
		seedSudahDicoba = true;
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Tbmrole pemilik = (Tbmrole) session.createCriteria(Tbmrole.class)
					.add(Restrictions.eq("roleId", EbisnisActorContextResolver.ROLE_PEMILIK))
					.setMaxResults(1).uniqueResult();
			Tbmrole sales = (Tbmrole) session.createCriteria(Tbmrole.class)
					.add(Restrictions.eq("roleId", EbisnisActorContextResolver.ROLE_SALES_KELILING))
					.setMaxResults(1).uniqueResult();
			if (pemilik != null && sales != null) {
				return;
			}
			tx = session.beginTransaction();
			if (pemilik == null) {
				pemilik = new Tbmrole();
				pemilik.setRoleId(EbisnisActorContextResolver.ROLE_PEMILIK);
				pemilik.setRoleName("Pemilik Sales / Inventory");
				pemilik.setAktif(Boolean.TRUE);
				pemilik.setEbisnisMenu(menuRolePemilikJson());
				session.save(pemilik);
			}
			if (sales == null) {
				sales = new Tbmrole();
				sales.setRoleId(EbisnisActorContextResolver.ROLE_SALES_KELILING);
				sales.setRoleName("Sales Keliling");
				sales.setAktif(Boolean.TRUE);
				sales.setEbisnisMenu(menuRoleSalesJson());
				session.save(sales);
			}
			tx.commit();
			System.out.println("[SI-SEED] Role inventory_sales dipastikan ada (pemilik_sales_inventory, sales_keliling).");
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			// Boleh gagal saat start paling pertama (tabel/kolom belum siap) -- coba lagi di load
			// berikutnya, JANGAN menjatuhkan servlet.
			seedSudahDicoba = false;
			ais.common.ErrorAuditUtil.record(e, "SalesInventoryHelper.pastikanSeedRole");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Bungkus JSON {@code ebisnisMenu} dgn SEMUA kunci katalog di-set EKSPLISIT (tidak mengandalkan
	 * default {@code urai()}) -- role seed harus deterministik: menu di luar {@code aktifkan}
	 * eksplisit {@code false}, aksi CRUD di luar {@code crudAktif} eksplisit {@code false}.
	 */
	/* package */ static String bungkusMenuRole(java.util.Set<String> aktifkan,
			java.util.Map<String, java.util.Set<String>> crudAktif) throws Exception {
		// Visibilitas package (bukan private): di-reuse seed varian lain satu paket
		// (ApotikEmedikSeedHelper) -- format bungkus JSON role harus SATU sumber.
		JSONObject obj = new JSONObject();
		obj.put("supervisor", false);
		obj.put("berandaKantin", false);
		obj.put("landingKantin", false);
		JSONObject menu = new JSONObject();
		for (EbisnisMenuKatalog.Entri e : EbisnisMenuKatalog.DAFTAR) {
			menu.put(e.kunci, aktifkan.contains(e.kunci));
		}
		obj.put("menu", menu);
		JSONObject crud = new JSONObject();
		for (String kunci : EbisnisMenuKatalog.KUNCI_CRUD) {
			JSONObject baris = new JSONObject();
			java.util.Set<String> bolehAksi = crudAktif.get(kunci);
			for (int i = 0; i < EbisnisMenuKatalog.AKSI_CRUD.length; i++) {
				String aksi = EbisnisMenuKatalog.AKSI_CRUD[i];
				baris.put(aksi, bolehAksi != null && bolehAksi.contains(aksi));
			}
			crud.put(kunci, baris);
		}
		obj.put("crud", crud);
		return obj.toString();
	}

	/* package */ static java.util.Set<String> set(String... isi) {
		return new java.util.LinkedHashSet<String>(java.util.Arrays.asList(isi));
	}

	/* package */ static final java.util.Set<String> SEMUA_AKSI_CRUD = set("create", "update", "delete", "approve", "reject");

	/**
	 * Pemilik Sales/Inventory: seluruh menu varian Inventory &amp; Sales + menu POS existing yang
	 * layar 48-nya reuse (produk/anggota/stokopname/kulakan/penyedia via kunci existing +
	 * ringkasan/laporan/laporankeuangan utk dasbor &amp; laporan) -- lihat MAPPING csv kolom
	 * Hak_Pemilik ("Penuh sesuai scope toko/tenant"). CRUD penuh pada menu-menu itu.
	 */
	private static String menuRolePemilikJson() throws Exception {
		java.util.Set<String> menuAktif = set(
				// kunci varian baru (16)
				"master_supplier", "master_customer", "master_sales", "persediaan", "harga",
				"hutang", "penjualan_sales", "piutang", "surat_perintah_sales", "nota_sales",
				"biaya_sales", "pembelian_sales", "rekonsiliasi_sales", "kas_jurnal", "laba_rugi",
				"laporan_inventory_sales",
				// kunci existing yang layar 48 reuse langsung
				"produk", "anggota", "stokopname", "kulakan", "penyedia", "ringkasan",
				"laporan", "laporankeuangan", "riwayatsinkronisasi", "logerror", "konfigurasi");
		java.util.Map<String, java.util.Set<String>> crudAktif = new java.util.LinkedHashMap<String, java.util.Set<String>>();
		String[] crudPenuh = { "master_supplier", "master_customer", "master_sales", "harga", "hutang",
				"penjualan_sales", "piutang", "surat_perintah_sales", "nota_sales", "biaya_sales",
				"pembelian_sales", "kas_jurnal", "produk", "anggota", "stokopname", "kulakan", "penyedia" };
		for (int i = 0; i < crudPenuh.length; i++) {
			crudAktif.put(crudPenuh[i], SEMUA_AKSI_CRUD);
		}
		return bungkusMenuRole(menuAktif, crudAktif);
	}

	/**
	 * Sales Keliling: hanya scope tugas lapangan -- lihat PERINTAH_MASTER &sect;5.1.C. Master
	 * global (supplier/sales/harga) TIDAK tampil; customer/produk tampil BACA-SAJA (kunci menu
	 * aktif utk aksi list/katalog, tapi seluruh aksi CRUD-nya false -> mutasi ditolak server).
	 * Approve/reject SELALU false (sales dilarang menyetujui SPJ/menutup sesi final sendiri).
	 */
	private static String menuRoleSalesJson() throws Exception {
		java.util.Set<String> menuAktif = set(
				"master_customer", "persediaan", "penjualan_sales", "surat_perintah_sales",
				"nota_sales", "biaya_sales", "pembelian_sales",
				// baca-saja data pendukung lewat aksi existing (anggota_list/katalog/laporan sendiri)
				"anggota", "produk", "riwayatsinkronisasi", "logerror", "konfigurasi");
		java.util.Map<String, java.util.Set<String>> crudAktif = new java.util.LinkedHashMap<String, java.util.Set<String>>();
		crudAktif.put("penjualan_sales", set("create", "update"));
		crudAktif.put("nota_sales", set("create", "update"));
		crudAktif.put("biaya_sales", set("create", "update"));
		crudAktif.put("pembelian_sales", set("create"));
		return bungkusMenuRole(menuAktif, crudAktif);
	}

	// =============================================================================================
	// Konteks aktor (FND-006/FND-008)
	// =============================================================================================

	/**
	 * Tambahkan blok konteks aktor ke balasan aksi {@code konfigurasi} (ADITIF -- klien POS lama
	 * mengabaikan field baru ini). Dipanggil {@code PosApi.prosesKonfigurasi} dalam try/catch.
	 */
	public static void isiKonteksAktor(Tbmuser tbmuser, JSONObject hasil) throws Exception {
		EbisnisActorContextResolver.ActorContext ctx = EbisnisActorContextResolver.resolve(tbmuser);
		hasil.put("aktorInventorySales", ctx.toJson());
	}

	/**
	 * Aksi {@code si_actor_context} -- konteks aktor terselesaikan utk pemanggil SENDIRI (dipakai
	 * klien varian menentukan landing/menu + evidence P1; tidak menerima parameter identitas apa
	 * pun dari klien -- selalu dari token, tidak bisa dipalsukan).
	 */
	public static void aktorContext(Tbmuser tbmuser, JSONObject hasil) throws Exception {
		EbisnisActorContextResolver.ActorContext ctx = EbisnisActorContextResolver.resolve(tbmuser);
		hasil.put("status", "00");
		hasil.put("aktor", ctx.toJson());
	}
}
