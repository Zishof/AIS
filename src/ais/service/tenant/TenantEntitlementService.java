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
