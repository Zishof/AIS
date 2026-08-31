package ais.service.tenant;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.model.tenant.JenisUsahaTenant;
import ais.database.model.tenant.JenisUsahaTenantModule;
import ais.database.model.tenant.PendaftaranTenant;
import ais.database.model.tenant.PendaftaranTenantJenisUsaha;
import ais.database.model.tenant.TenantModuleEntitlement;
import ais.database.model.tenant.TenantRegistry;

/**
 * <h3>Union module entitlement tenant dari jenis usaha terpilih (§6/§10.3 dokumen master).</h3>
 *
 * <p>Entitlement ≠ permission (invariant #13): baris di sini menandai modul TERSEDIA bagi
 * tenant; izin pengguna tetap role. Modul yang implementasi source-nya belum operasional
 * ditandai {@code PLANNED} (UI jujur, bukan tombol semu §6.3) -- daftar modul operasional
 * configurable `pendaftaran_modul_operasional` (CSV) supaya naik status tanpa compile ulang.</p>
 */
public final class TenantEntitlementService {

	/** Default modul yang SUDAH operasional di codebase ini (POS/koperasi + paritas 48 layar + eCampus/eSchool). */
	private static final String DEFAULT_OPERASIONAL = "POS,PRODUK,MASTER_PRODUK,INVENTORY,PERSEDIAAN,"
			+ "STOK_OPNAME,MASTER_HARGA,PEMBELIAN,SUPPLIER,MASTER_SUPPLIER,CUSTOMER,MASTER_CUSTOMER,"
			+ "MASTER_SALES,SALES,HUTANG,PIUTANG,HUTANG_PIUTANG,PENJUALAN,NOTA_SALES,KAS_JURNAL,"
			+ "LABA_RUGI,LAPORAN,BATCH_EXPIRY,FARMASI,ANGGOTA,SIMPANAN,STOK,PROMO,POS_FNB,MENU_RESEP,"
			+ "BAHAN_BAKU,MEJA_ORDER,GUDANG,ESCHOOL,ECAMPUS,PROFIL";

	private TenantEntitlementService() {
	}

	/**
	 * Tentukan apakah suatu modul sudah punya implementasi operasional di codebase ini (dan
	 * karenanya boleh berstatus {@link TenantModuleEntitlement#STATUS_ACTIVE}) atau masih
	 * {@link TenantModuleEntitlement#STATUS_PLANNED} (UI jujur -- §6.3: tidak menampilkan tombol
	 * semu untuk fitur yang belum ada). Daftar modul operasional dibaca dari konfigurasi
	 * {@code pendaftaran_modul_operasional} (CSV, dipisah koma/titik-koma/spasi) supaya modul
	 * yang baru selesai diimplementasikan dapat "naik status" ke ACTIVE lewat perubahan
	 * konfigurasi saja, tanpa compile ulang aplikasi; bila konfigurasi tidak terbaca (mis. belum
	 * ada baris konfigurasinya), jatuh ke {@link #DEFAULT_OPERASIONAL} yang berisi modul-modul
	 * yang memang sudah operasional di codebase ini per penulisan komentar tersebut (POS/koperasi
	 * + paritas 48 layar + eCampus/eSchool).
	 *
	 * @param moduleCode kode modul yang dicek, dibandingkan tidak peka huruf besar-kecil.
	 * @return {@code true} bila {@code moduleCode} ada pada daftar modul operasional.
	 */
	static boolean modulOperasional(String moduleCode) {
		String csv;
		try {
			csv = Common.getKonfigurasi("pendaftaran_modul_operasional", DEFAULT_OPERASIONAL).getNilai();
		} catch (Exception e) {
			csv = DEFAULT_OPERASIONAL;
		}
		String[] tokens = csv.toUpperCase().split("[,;\\s]+");
		for (int i = 0; i < tokens.length; i++) {
			if (moduleCode.equalsIgnoreCase(tokens[i].trim())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Terapkan union entitlement source BUSINESS_TYPE utk satu tenant, idempoten: baris
	 * (tenant, module, source) existing TIDAK diduplikasi/ditimpa. Berjalan dalam session/
	 * transaction pemanggil (step provisioning).
	 *
	 * <p>
	 * Dipanggil sebagai salah satu langkah provisioning tenant (lihat {@link
	 * TenantOnboardingService}): setiap {@link PendaftaranTenant} membawa satu atau lebih pilihan
	 * {@link ais.database.model.tenant.JenisUsahaTenant} (mis. tenant yang mendaftar sebagai
	 * "koperasi" DAN "apotek" sekaligus). Method ini menyatukan (union) modul default dari
	 * SELURUH jenis usaha yang dipilih -- bukan hanya jenis usaha pertama -- sehingga tenant
	 * multi-jenis-usaha mendapat gabungan modul dari semuanya, tanpa duplikat. Untuk keperluan
	 * audit trail ({@code selectedJenisUsaha}), setiap modul dicatat berasal dari jenis usaha
	 * PERTAMA (menurut urutan pilihan) yang membawanya -- bila dua jenis usaha sama-sama memuat
	 * modul yang sama, jenis usaha kedua tidak menimpa atribusi jenis usaha pertama.
	 * </p>
	 * <p>
	 * Untuk setiap modul hasil union, method mengecek lebih dulu apakah baris entitlement
	 * (tenant, moduleCode, source=BUSINESS_TYPE) sudah ada -- bila ya, dilewati (idempoten,
	 * aman dipanggil ulang mis. saat provisioning di-retry). Bila belum ada, baris baru dibuat
	 * dengan status {@link TenantModuleEntitlement#STATUS_ACTIVE} atau {@link
	 * TenantModuleEntitlement#STATUS_PLANNED} tergantung hasil {@link #modulOperasional(String)}.
	 * </p>
	 *
	 * @param session    Session/transaksi milik pemanggil (langkah provisioning); tidak dibuka/
	 *                   ditutup di sini.
	 * @param tenant     tenant yang menerima entitlement.
	 * @param permohonan permohonan pendaftaran yang membawa pilihan jenis usaha
	 *                   ({@link PendaftaranTenantJenisUsaha}) dan {@code selectedPlanVersion}
	 *                   yang dicatat pada setiap baris entitlement baru.
	 * @return jumlah baris entitlement baru yang dibuat.
	 */
	public static int terapkanDariJenisUsaha(Session session, TenantRegistry tenant, PendaftaranTenant permohonan) {
		// Union modul lintas jenis usaha terpilih; jenis usaha PERTAMA yang membawa modul
		// dicatat sbg asal (audit trail selectedJenisUsaha).
		Map<String, JenisUsahaTenant> union = new LinkedHashMap<String, JenisUsahaTenant>();
		List<?> pilihan = session.createCriteria(PendaftaranTenantJenisUsaha.class)
				.add(Restrictions.eq("pendaftaranTenant.id", permohonan.getId())).list();
		for (Object o : pilihan) {
			JenisUsahaTenant jenis = ((PendaftaranTenantJenisUsaha) o).getJenisUsahaTenant();
			if (jenis == null) {
				continue;
			}
			List<?> moduls = session.createCriteria(JenisUsahaTenantModule.class)
					.add(Restrictions.eq("jenisUsahaTenant.id", jenis.getId()))
					.add(Restrictions.eq("defaultEnabled", Boolean.TRUE)).list();
			for (Object m : moduls) {
				String kode = ((JenisUsahaTenantModule) m).getModuleCode();
				if (!union.containsKey(kode)) {
					union.put(kode, jenis);
				}
			}
		}

		int dibuat = 0;
		Date sekarang = new Date();
		for (Map.Entry<String, JenisUsahaTenant> e : union.entrySet()) {
			Number ada = (Number) session.createCriteria(TenantModuleEntitlement.class)
					.add(Restrictions.eq("tenant.id", tenant.getId()))
					.add(Restrictions.eq("moduleCode", e.getKey()))
					.add(Restrictions.eq("source", TenantModuleEntitlement.SOURCE_BUSINESS_TYPE))
					.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
			if (ada != null && ada.longValue() > 0) {
				continue;
			}
			TenantModuleEntitlement ent = new TenantModuleEntitlement();
			ent.setTenant(tenant);
			ent.setModuleCode(e.getKey());
			ent.setSource(TenantModuleEntitlement.SOURCE_BUSINESS_TYPE);
			ent.setStatus(modulOperasional(e.getKey()) ? TenantModuleEntitlement.STATUS_ACTIVE
					: TenantModuleEntitlement.STATUS_PLANNED);
			ent.setEffectiveFrom(sekarang);
			ent.setSelectedJenisUsaha(e.getValue());
			ent.setPlanVersion(permohonan.getSelectedPlanVersion());
			ent.setCreatedAt(sekarang);
			ent.setOleh("provisioning");
			ent.setOlehId("provisioning");
			session.save(ent);
			dibuat++;
		}
		return dibuat;
	}
}
