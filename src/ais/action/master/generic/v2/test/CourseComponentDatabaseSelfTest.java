package ais.action.master.generic.v2.test;import java.util.HashSet;import ais.common.newui.kursus.NewUiCourseComponentService;import ais.common.newui.kursus.NewUiCourseComponentService.Snapshot;import ais.database.model.kursus.KomponenProdukKursus;import ais.database.model.rab.SatuanKerja;/**
 * Harness uji manual (dijalankan lewat {@code main}, bukan JUnit) untuk memverifikasi bahwa
 * {@link NewUiCourseComponentService} dapat memuat snapshot komponen produk kursus untuk setiap
 * tipe yang terdaftar di {@link KomponenProdukKursus#s} tanpa melempar galat, dengan hasil yang
 * konsisten: jumlah baris tidak melebihi batas halaman (10) dan seluruh daftar opsi (unit kerja,
 * pegawai, item, ujian) tidak null. Properti sistem validasi Hibernate/JPA dimatikan lebih dulu
 * agar harness dapat berjalan di luar konteks server aplikasi penuh. Kegagalan pemeriksaan
 * dilaporkan lewat {@link IllegalStateException} (via {@link #check}) dan proses keluar dengan
 * status bukan nol secara implisit (exception tidak tertangkap); sukses ditandai baris
 * "CourseComponentDatabaseSelfTest OK" dan {@code System.exit(0)}.
 */
public final class CourseComponentDatabaseSelfTest{private CourseComponentDatabaseSelfTest(){}
	/** Menjalankan pemeriksaan snapshot untuk seluruh tipe komponen kursus; lihat javadoc kelas untuk detail. */
	public static void main(String[]a){System.setProperty("javax.persistence.validation.mode","none");System.setProperty("hibernate.validator.apply_to_ddl","false");NewUiCourseComponentService s=new NewUiCourseComponentService();for(String t:KomponenProdukKursus.s){Snapshot x=s.load(t,null,null,true,0,10,new HashSet<SatuanKerja>());check(x.total>=0&&x.rows.size()<=10,t);check(x.units!=null&&x.employees!=null&&x.items!=null&&x.exams!=null,"options");}System.out.println("CourseComponentDatabaseSelfTest OK types="+KomponenProdukKursus.s.length);System.exit(0);}private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
