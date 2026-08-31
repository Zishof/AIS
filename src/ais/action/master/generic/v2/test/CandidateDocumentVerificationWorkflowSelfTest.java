package ais.action.master.generic.v2.test;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.adapter.CandidateDocumentVerificationWorkflowGenericCrudAdapter;import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;import ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiBerkas;/**
 * Harness uji manual (dijalankan lewat {@code main}, bukan JUnit) untuk memverifikasi konfigurasi
 * {@link CandidateDocumentVerificationWorkflowGenericCrudAdapter} pada entitas
 * {@link BiodataCalonMahasiswaPunyaVerifikasiBerkas}: memastikan adapter memaksa mode native-only
 * (create/update/delete dipaksa nonaktif walau diminta aktif di {@link GenericCrudDefinition} awal),
 * memastikan jumlah kolom kunci alami sesuai ekspektasi, dan memastikan entitas terdaftar sebagai
 * "reviewed" pada {@link GenericCrudReviewedAdapterFactory}. Kelas final tanpa instance (konstruktor
 * privat kosong); hanya dipakai sebagai titik masuk command-line untuk pengecekan cepat saat
 * pengembangan, bukan dipanggil dari kode produksi.
 */
@SuppressWarnings("rawtypes")public final class CandidateDocumentVerificationWorkflowSelfTest{private CandidateDocumentVerificationWorkflowSelfTest(){}
	/**
	 * Menjalankan rangkaian pengecekan tegas (assert manual via {@link #check}) terhadap adapter
	 * generic-CRUD untuk verifikasi berkas calon mahasiswa, lalu keluar dengan kode 0 bila semua
	 * lolos atau melempar {@link IllegalStateException} bila salah satu gagal.
	 *
	 * @param a argumen baris perintah, tidak dipakai
	 */
	public static void main(String[]a){CandidateDocumentVerificationWorkflowGenericCrudAdapter x=new CandidateDocumentVerificationWorkflowGenericCrudAdapter();GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(BiodataCalonMahasiswaPunyaVerifikasiBerkas.class);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);x.configure(d);check(!d.isCreateEnabled()&&!d.isUpdateEnabled()&&!d.isDeleteEnabled(),"native-only");List k=x.getNaturalKeyProperties();check(k.size()==2,"natural key");check(GenericCrudReviewedAdapterFactory.isReviewed(BiodataCalonMahasiswaPunyaVerifikasiBerkas.class),"reviewed");System.out.println("CandidateDocumentVerificationWorkflowSelfTest OK");System.exit(0);}private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
