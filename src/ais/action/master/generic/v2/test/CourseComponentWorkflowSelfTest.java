package ais.action.master.generic.v2.test;

import java.util.List;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.adapter.CourseComponentWorkflowGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;
import ais.common.newui.kursus.NewUiCourseComponentService;
import ais.database.model.kursus.KomponenDataProdukKursus;

/**
 * Harness regresi mandiri untuk {@link CourseComponentWorkflowGenericCrudAdapter} dan pemetaan komponen
 * produk kursus. Pemeriksaan mencakup delapan nama halaman yang dikenali oleh
 * {@link NewUiCourseComponentService#typeForPage(String)}, konfigurasi adapter yang wajib menonaktifkan
 * create/update/delete/import, dua properti kunci alami, serta status {@code reviewed} milik
 * {@link KomponenDataProdukKursus} pada {@link GenericCrudReviewedAdapterFactory}.
 *
 * <p><b>Batas tanggung jawab:</b> kelas ini hanya menjadi executable self-test, bukan bagian dari alur
 * produksi dan bukan tempat menyalin aturan domain. Tambahkan aturan bersama pada service atau adapter yang
 * diuji, lalu tambahkan assertion di sini untuk mencegah implementasi paralel dengan hasil berbeda.</p>
 *
 * <p>{@link #main(String[])} menjalankan seluruh skenario, mencetak pesan sukses, lalu mengakhiri JVM dengan
 * kode {@code 0}. Helper {@code check} melempar {@link IllegalStateException} pada kegagalan sehingga proses
 * berhenti dengan status gagal.</p>
 */
@SuppressWarnings("rawtypes")
public final class CourseComponentWorkflowSelfTest{private CourseComponentWorkflowSelfTest(){}
	/** Menjalankan seluruh pemeriksaan pemetaan halaman dan adapter komponen kursus; lihat javadoc kelas. */
	public static void main(String[]a){String[][]pages={{"komponen_data_video","Video"},{"komponen_data_buku","Buku"},{"komponen_data_ebook","Ebook"},{"komponen_data_latihan_soal","Latihan Soal"},{"komponen_data_ujian","Ujian"},{"komponen_data_tatap_muka","Pembelajaran Tatap Muka"},{"komponen_data_jarak_jauh","Pembelajaran Jarak Jauh"},{"komponen_data_ekstrakulikuler","Ekstra Kurikuler"}};for(String[]p:pages)check(p[1].equals(NewUiCourseComponentService.typeForPage(p[0])),p[0]);CourseComponentWorkflowGenericCrudAdapter x=new CourseComponentWorkflowGenericCrudAdapter();GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(KomponenDataProdukKursus.class);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);d.setImportEnabled(true);x.configure(d);check(!d.isCreateEnabled()&&!d.isUpdateEnabled()&&!d.isDeleteEnabled()&&!d.isImportEnabled(),"native-only");List k=x.getNaturalKeyProperties();check(k.size()==2,"keys");check(GenericCrudReviewedAdapterFactory.isReviewed(KomponenDataProdukKursus.class),"reviewed");System.out.println("CourseComponentWorkflowSelfTest OK");System.exit(0);}private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
