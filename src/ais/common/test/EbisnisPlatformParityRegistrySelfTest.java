package ais.common.test;

import java.util.List;

import ais.common.EbisnisPlatformParityRegistry;

/** Self-test permanen kontrak paritas platform; kompilasi ke .codex-build. */
public final class EbisnisPlatformParityRegistrySelfTest {
	private static int pemeriksaan;
	private EbisnisPlatformParityRegistrySelfTest() { }
	private static void benar(boolean nilai, String konteks) {
		pemeriksaan++;
		if (!nilai) throw new IllegalStateException(konteks);
	}
	private static void sama(String harapan, String aktual, String konteks) {
		benar(harapan.equals(aktual), konteks + ": harapan=" + harapan + ", aktual=" + aktual);
	}
	private static void sama(int harapan, int aktual, String konteks) {
		benar(harapan == aktual, konteks + ": harapan=" + harapan + ", aktual=" + aktual);
	}
	private static EbisnisPlatformParityRegistry.AccessPolicy admin() {
		return new EbisnisPlatformParityRegistry.AccessPolicy() {
			public boolean apakahAdmin() { return true; }
			public boolean diizinkan(String menuKey, String aksi) { return false; }
		};
	}
	private static EbisnisPlatformParityRegistry.AccessPolicy ditolak() {
		return new EbisnisPlatformParityRegistry.AccessPolicy() {
			public boolean apakahAdmin() { return false; }
			public boolean diizinkan(String menuKey, String aksi) { return false; }
		};
	}
	private static EbisnisPlatformParityRegistry.AccessPolicy kasirBolehLihat() {
		return new EbisnisPlatformParityRegistry.AccessPolicy() {
			public boolean apakahAdmin() { return false; }
			public boolean diizinkan(String menuKey, String aksi) {
				return "kasir_pos".equals(menuKey) && "view".equals(aksi);
			}
		};
	}
	private static void gagal(String platform, String menu, String aksi) {
		boolean gagal = false;
		try { EbisnisPlatformParityRegistry.resolve(platform, menu, aksi, 10, admin()); }
		catch (IllegalArgumentException e) { gagal = true; }
		benar(gagal, "input tidak valid wajib ditolak: " + platform + "/" + menu + "/" + aksi);
	}
	public static void main(String[] args) {
		EbisnisPlatformParityRegistry.validasi();
		List<EbisnisPlatformParityRegistry.PlatformProfile> platform = EbisnisPlatformParityRegistry.semuaPlatform();
		sama(4, platform.size(), "empat platform wajib terdaftar");
		String[] ids = new String[] { "desktop", "android", "jsp", "zkoss" };
		for (int i = 0; i < ids.length; i++) {
			EbisnisPlatformParityRegistry.PlatformProfile profil = EbisnisPlatformParityRegistry.platform(ids[i]);
			benar(profil != null, "profil platform tersedia: " + ids[i]);
			benar(profil.mendukung("responsive_navigation"), "navigasi responsif: " + ids[i]);
			benar(profil.mendukung("work_queue"), "work queue: " + ids[i]);
			benar(profil.mendukung("optimistic_lock"), "optimistic locking: " + ids[i]);
			benar(profil.mendukung("error_contract"), "kontrak error: " + ids[i]);
			benar(profil.mendukung("export_pdf"), "ekspor PDF: " + ids[i]);
			benar(profil.mendukung("export_excel"), "ekspor Excel: " + ids[i]);
			benar(profil.mendukung("print"), "cetak: " + ids[i]);
			benar(profil.mendukung("contextual_help"), "bantuan kontekstual: " + ids[i]);
			EbisnisPlatformParityRegistry.ResolvedAction lihat = EbisnisPlatformParityRegistry.resolve(ids[i], "kasir_pos", "view", 0, admin());
			sama("/ebisnis/kasir", lihat.canonicalRoute, "route kanonis sama: " + ids[i]);
			sama("kasir_pos:view", lihat.permissionKey, "permission kanonis sama: " + ids[i]);
			sama(EbisnisPlatformParityRegistry.ERROR_CONTRACT, lihat.errorContract, "kontrak error sama: " + ids[i]);
			sama(10, lihat.pageSize, "paging default: " + ids[i]);
			benar(lihat.visible && lihat.enabled, "admin bypass berlaku: " + ids[i]);
			benar(!lihat.writeOperation && !lihat.idempotencyRequired, "operasi baca tidak perlu idempotensi: " + ids[i]);
		}
		benar(EbisnisPlatformParityRegistry.platform("desktop").mendukung("offline_queue"), "Desktop mendukung antrean offline");
		benar(EbisnisPlatformParityRegistry.platform("android").mendukung("barcode_scan"), "Android mendukung scan barcode");
		benar(!EbisnisPlatformParityRegistry.platform("jsp").mendukung("offline_queue"), "JSP tidak mengklaim antrean offline");
		benar(!EbisnisPlatformParityRegistry.platform("zkoss").mendukung("qr_scan"), "ZKoss tidak mengklaim scan QR");
		EbisnisPlatformParityRegistry.ResolvedAction hasilDitolak = EbisnisPlatformParityRegistry.resolve("desktop", "kasir_pos", "view", 999, ditolak());
		benar(!hasilDitolak.visible && !hasilDitolak.enabled, "aksi terlarang wajib disembunyikan dan dinonaktifkan");
		sama("AKSES_DITOLAK", hasilDitolak.denialReason, "alasan penolakan stabil");
		sama(100, hasilDitolak.pageSize, "paging dibatasi maksimum");
		EbisnisPlatformParityRegistry.ResolvedAction kasir = EbisnisPlatformParityRegistry.resolve("android", "kasir_pos", "view", 25, kasirBolehLihat());
		benar(kasir.visible && kasir.enabled, "izin eksplisit non-admin harus dihormati");
		sama(25, kasir.pageSize, "paging eksplisit dipertahankan");
		EbisnisPlatformParityRegistry.ResolvedAction buat = EbisnisPlatformParityRegistry.resolve("desktop", "kasir_pos", "create", 10, admin());
		benar(buat.writeOperation && buat.idempotencyRequired, "create wajib idempotency key");
		benar(!buat.optimisticVersionRequired, "create belum memiliki versi existing");
		EbisnisPlatformParityRegistry.ResolvedAction edit = EbisnisPlatformParityRegistry.resolve("android", "pesanan_pelanggan", "edit_draft", 10, admin());
		benar(edit.writeOperation && edit.idempotencyRequired && edit.optimisticVersionRequired, "edit draft wajib idempotensi dan optimistic version");
		gagal("browser_lain", "kasir_pos", "view");
		gagal("desktop", "menu_tidak_ada", "view");
		gagal("desktop", "kasir_pos", "approve");
		System.out.println("Self-test EbisnisPlatformParityRegistry: LULUS (" + pemeriksaan + " pemeriksaan)");
	}
}
