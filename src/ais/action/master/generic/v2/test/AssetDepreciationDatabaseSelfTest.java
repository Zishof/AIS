package ais.action.master.generic.v2.test;import java.util.Calendar;import ais.common.newui.asset.NewUiAssetDepreciationService;import ais.common.newui.asset.NewUiAssetDepreciationService.Filter;import ais.common.newui.asset.NewUiAssetDepreciationService.Snapshot;/**
 * Harness uji manual berbasis DATABASE SUNGGUHAN (dijalankan langsung via {@code main}, bukan dari
 * test runner/mocking) untuk {@link NewUiAssetDepreciationService} (laporan penyusutan asset,
 * antarmuka baru). Memuat satu halaman data ({@code page=0, size=10}) untuk rentang 6 bulan
 * terakhir dan memverifikasi hasil paging masuk akal (total tidak negatif, jumlah baris tidak
 * melebihi ukuran halaman) serta {@code options()} (daftar opsi filter) tersedia. Mencetak
 * ringkasan ke stdout dan keluar kode 0 bila lolos; melempar {@link IllegalStateException} pada
 * pemeriksaan pertama yang gagal.
 */
public final class AssetDepreciationDatabaseSelfTest{private AssetDepreciationDatabaseSelfTest(){}
	/** Menjalankan pemeriksaan paging dan ketersediaan opsi filter terhadap {@link NewUiAssetDepreciationService} menggunakan koneksi database sungguhan. */
	public static void main(String[]a){NewUiAssetDepreciationService s=new NewUiAssetDepreciationService();Filter f=new Filter();Calendar c=Calendar.getInstance();f.end=c.getTime();c.add(Calendar.MONTH,-6);f.start=c.getTime();f.page=0;f.size=10;Snapshot x=s.load(f);if(x.total<0||x.rows.size()>10)throw new IllegalStateException("paging invalid");if(s.options()==null)throw new IllegalStateException("options missing");System.out.println("AssetDepreciationDatabaseSelfTest OK total="+x.total);System.exit(0);}}
