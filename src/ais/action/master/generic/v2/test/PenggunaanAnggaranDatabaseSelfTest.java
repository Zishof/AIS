package ais.action.master.generic.v2.test;import java.util.Calendar;import ais.common.newui.rab.NewUiPenggunaanAnggaranService;import ais.common.newui.rab.NewUiPenggunaanAnggaranService.Filter;import ais.common.newui.rab.NewUiPenggunaanAnggaranService.Snapshot;
/**
 * Harness uji manual (bukan JUnit) yang memerlukan koneksi database aktif untuk
 * {@link NewUiPenggunaanAnggaranService} (penggunaan anggaran/RAB).
 *
 * <p>
 * Memverifikasi: paginasi (jumlah baris hasil tidak melebihi ukuran halaman yang diminta),
 * kelengkapan facet ({@code workspaces} dan {@code sourceCounts} pada {@link Snapshot} tidak
 * null), id baris pertama terisi bila ada hasil, {@code history} mengembalikan daftar kosong
 * untuk id anggaran yang tidak valid ({@code -1}), dan {@code previewSources} mengembalikan
 * tepat 8 kriteria sumber untuk rentang tanggal 20 tahun ke belakang.
 * </p>
 */
public final class PenggunaanAnggaranDatabaseSelfTest{private PenggunaanAnggaranDatabaseSelfTest(){}
	/** Menjalankan seluruh skenario uji paginasi, facet, riwayat, dan kriteria sumber anggaran. */
	public static void main(String[]a){System.setProperty("javax.persistence.validation.mode","none");System.setProperty("hibernate.validator.apply_to_ddl","false");Filter f=new Filter();f.size=10;NewUiPenggunaanAnggaranService service=new NewUiPenggunaanAnggaranService();Snapshot s=service.load(f);check(s.total>=0&&s.rows.size()<=10,"paging");check(s.workspaces!=null&&s.sourceCounts!=null,"facets");if(!s.rows.isEmpty())check(s.rows.get(0).id!=null,"id");check(service.history(Long.valueOf(-1)).isEmpty(),"history");Calendar c=Calendar.getInstance();f.end=c.getTime();c.add(Calendar.YEAR,-20);f.start=c.getTime();check(service.previewSources(f).size()==8,"source criteria");System.out.println("PenggunaanAnggaranDatabaseSelfTest OK total="+s.total+" rows="+s.rows.size());System.exit(0);}/** Melempar {@link IllegalStateException} berisi {@code m} bila {@code v} bernilai {@code false}. */
private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
