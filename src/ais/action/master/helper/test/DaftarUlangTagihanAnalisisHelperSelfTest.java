package ais.action.master.helper.test;

import ais.action.master.helper.DaftarUlangTagihanAnalisisHelper;

/**
 * Uji regresi tanpa database untuk urutan keputusan mesin analisis tagihan.
 *
 * <p>Dijalankan dengan {@code java -ea}. Test sengaja hanya memasok fakta ternormalisasi
 * sehingga kegagalan menunjukkan masalah pada klasifikasi, bukan pada Hibernate atau ZK.</p>
 */
public final class DaftarUlangTagihanAnalisisHelperSelfTest {

	private DaftarUlangTagihanAnalisisHelperSelfTest() {
	}

	public static void main(String[] args) {
		assertKode(dataDasar(), "PEMBAYARAN_SEBAGIAN");

		DaftarUlangTagihanAnalisisHelper.Data tanpaSetting = dataDasar();
		tanpaSetting.kandidatSetting = 0;
		tanpaSetting.itemBiayaAktif = 0;
		tanpaSetting.templateAkhir = 0;
		tanpaSetting.barisLayar = 0;
		assertKode(tanpaSetting, "SETTING_TIDAK_COCOK");

		DaftarUlangTagihanAnalisisHelper.Data tanpaItem = dataDasar();
		tanpaItem.itemBiayaAktif = 0;
		tanpaItem.barisLayar = 0;
		assertKode(tanpaItem, "ITEM_BIAYA_KOSONG");

		DaftarUlangTagihanAnalisisHelper.Data billingKosong = dataDasar();
		billingKosong.settingDefault = 0;
		billingKosong.settingBilling = 1;
		billingKosong.pengaturanBulanan = 0;
		billingKosong.barisLayar = 0;
		billingKosong.templateAkhir = 1;
		billingKosong.cicilan = 0;
		billingKosong.nilaiDibayarCommitted = 0.0d;
		assertKode(billingKosong, "BILLING_BELUM_DIBUAT");

		DaftarUlangTagihanAnalisisHelper.Data templateTidakCocok = dataDasar();
		templateTidakCocok.templateAkhir = 0;
		templateTidakCocok.hasilProduksi = 0;
		templateTidakCocok.barisLayar = 0;
		templateTidakCocok.cicilan = 0;
		templateTidakCocok.nilaiDibayarCommitted = 0.0d;
		assertKode(templateTidakCocok, "TEMPLATE_TIDAK_COCOK");

		DaftarUlangTagihanAnalisisHelper.Data nominalNol = dataDasar();
		nominalNol.nominalTagihanTampil = 0.0d;
		nominalNol.nilaiDibayarCommitted = 0.0d;
		assertKode(nominalNol, "NOMINAL_NOL");

		DaftarUlangTagihanAnalisisHelper.Data lebihBayar = dataDasar();
		lebihBayar.nilaiDibayarCommitted = 12000.0d;
		assertKode(lebihBayar, "PEMBAYARAN_MELEBIHI_TAGIHAN");

		DaftarUlangTagihanAnalisisHelper.Data lunas = dataDasar();
		lunas.nilaiDibayarCommitted = 10000.0d;
		assertKode(lunas, "LUNAS_MASIH_TAMPIL");

		DaftarUlangTagihanAnalisisHelper.Data belumBayar = dataDasar();
		belumBayar.cicilan = 0;
		belumBayar.nilaiDibayarCommitted = 0.0d;
		assertKode(belumBayar, "BELUM_DIBAYAR");

		DaftarUlangTagihanAnalisisHelper.Data sudahDibayarTakTampil = dataDasar();
		sudahDibayarTakTampil.barisLayar = 0;
		sudahDibayarTakTampil.nominalTagihanTampil = 0.0d;
		assertKode(sudahDibayarTakTampil, "TAGIHAN_TERBAYAR_TIDAK_TAMPIL");

		DaftarUlangTagihanAnalisisHelper.Data layarBelumSinkron = dataDasar();
		layarBelumSinkron.barisLayar = 0;
		layarBelumSinkron.cicilan = 0;
		layarBelumSinkron.nilaiDibayarCommitted = 0.0d;
		assertKode(layarBelumSinkron, "LAYAR_BELUM_SINKRON");

		DaftarUlangTagihanAnalisisHelper.Data queryProduksiKosong = dataDasar();
		queryProduksiKosong.barisLayar = 0;
		queryProduksiKosong.hasilProduksi = 0;
		queryProduksiKosong.cicilan = 0;
		queryProduksiKosong.nilaiDibayarCommitted = 0.0d;
		assertKode(queryProduksiKosong, "QUERY_PRODUKSI_KOSONG");

		System.out.println("DaftarUlangTagihanAnalisisHelperSelfTest: OK");
	}

	private static DaftarUlangTagihanAnalisisHelper.Data dataDasar() {
		DaftarUlangTagihanAnalisisHelper.Data data = new DaftarUlangTagihanAnalisisHelper.Data();
		data.identitas = "112200 - Anwar badrun";
		data.jenisPembayaran = "Daftar Ulang";
		data.statusAkademik = "Aktif";
		data.semester = 3;
		data.kandidatSetting = 1;
		data.itemBiayaAktif = 1;
		data.settingDefault = 1;
		data.templateAkhir = 1;
		data.hasilProduksi = 1;
		data.kegiatan = 1;
		data.cicilan = 1;
		data.barisLayar = 1;
		data.nominalTagihanTampil = 10000.0d;
		data.nilaiDibayarCommitted = 2000.0d;
		return data;
	}

	private static void assertKode(DaftarUlangTagihanAnalisisHelper.Data data, String expected) {
		String actual = DaftarUlangTagihanAnalisisHelper.analisis(data).getKode();
		if (!expected.equals(actual)) {
			throw new AssertionError("Expected " + expected + " but was " + actual);
		}
	}
}
