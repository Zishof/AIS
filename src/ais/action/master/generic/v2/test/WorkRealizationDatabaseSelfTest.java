package ais.action.master.generic.v2.test;import java.util.Calendar;import java.util.HashSet;import ais.common.newui.lkp.NewUiWorkRealizationService;import ais.common.newui.lkp.NewUiWorkRealizationService.Filter;import ais.common.newui.lkp.NewUiWorkRealizationService.Snapshot;import ais.database.model.lkp.KegiatanTugasJabatan;import ais.database.model.rab.SatuanKerja;/**
 * Harness uji manual (dijalankan lewat {@code main}, bukan unit test JUnit) untuk memverifikasi
 * bahwa {@link NewUiWorkRealizationService#load(Filter, java.util.Set, String)} dapat memuat
 * snapshot realisasi kerja pegawai langsung dari database nyata tanpa error. Membangun sebuah
 * {@link Filter} untuk bulan berjalan (periode {@code BULANAN}, halaman pertama 10 baris) tanpa
 * filter satuan kerja, memanggil service, lalu memeriksa bahwa jumlah baris hasil tidak melebihi
 * ukuran halaman dan bahwa daftar opsi pegawai/unit pada {@link Snapshot} terisi. Properti sistem
 * validasi Bean Validation/Hibernate Validator dimatikan lebih dulu agar harness tidak gagal hanya
 * karena constraint DDL yang tidak relevan dengan pengujian ini. Keluar dengan
 * {@link IllegalStateException} (exit code bukan 0) bila salah satu pemeriksaan gagal; keluar
 * normal (exit 0) dengan mencetak total baris bila semua pemeriksaan lolos.
 */
public final class WorkRealizationDatabaseSelfTest{private WorkRealizationDatabaseSelfTest(){}
	/** Menjalankan pemuatan snapshot realisasi kerja sekali dan memvalidasi bentuk hasilnya; lihat javadoc kelas untuk detail. */
	public static void main(String[]a){System.setProperty("javax.persistence.validation.mode","none");System.setProperty("hibernate.validator.apply_to_ddl","false");Calendar now=Calendar.getInstance();Filter f=new Filter();f.year=now.get(Calendar.YEAR);f.month=Integer.valueOf(now.get(Calendar.MONTH));f.period=KegiatanTugasJabatan.BULANAN;f.page=0;f.size=10;Snapshot s=new NewUiWorkRealizationService().load(f,new HashSet<SatuanKerja>(),null);check(s.total>=0&&s.rows.size()<=10,"rows");check(s.employees!=null&&s.units!=null,"options");System.out.println("WorkRealizationDatabaseSelfTest OK total="+s.total);System.exit(0);}private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
