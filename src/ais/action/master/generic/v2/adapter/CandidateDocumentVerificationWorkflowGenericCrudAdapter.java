package ais.action.master.generic.v2.adapter;import java.util.ArrayList;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.GenericCrudFieldDefinition;import ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiBerkas;/**
 * Adapter kerangka kerja CRUD generik ({@code generic/v2}) untuk entitas
 * {@link BiodataCalonMahasiswaPunyaVerifikasiBerkas} (relasi berkas verifikasi kelengkapan
 * dokumen calon mahasiswa). Berbeda dari kebanyakan adapter generik lain, kelas ini mengunci
 * seluruh definisi CRUD menjadi <b>read-only murni</b>: {@link GenericCrudDefinition#READ_ONLY}
 * beserta create/update/delete/import semuanya dimatikan secara eksplisit di {@link #configure},
 * dan setiap field individual juga ditandai tidak dapat dibuat/diubah — data verifikasi berkas
 * hanya boleh dilihat lewat modul generik ini, perubahan sesungguhnya terjadi di alur bisnis
 * verifikasi berkas PMB yang khusus. Daftar diurutkan default berdasarkan
 * {@code tanggal_dirubah} menurun (terbaru lebih dulu).
 */
@SuppressWarnings("rawtypes")public final class CandidateDocumentVerificationWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter{
	/** Membuat adapter untuk entitas {@link BiodataCalonMahasiswaPunyaVerifikasiBerkas} tanpa opsi tambahan (audit nonaktif, filter default null, mode read-only). */
	public CandidateDocumentVerificationWorkflowGenericCrudAdapter(){super(BiodataCalonMahasiswaPunyaVerifikasiBerkas.class,false,null,true);}
	/** Mengunci definisi CRUD generik menjadi read-only penuh (create/update/delete/import mati, tiap field tidak dapat diedit) dan mengatur label tampilan serta urutan default. */
	public void configure(GenericCrudDefinition d){d.setDisplayName("Verifikasi Berkas Calon Mahasiswa");d.setLifecycleStatus(GenericCrudDefinition.READ_ONLY);d.setCreateEnabled(false);d.setUpdateEnabled(false);d.setDeleteEnabled(false);d.setImportEnabled(false);d.setDefaultSortProperty("tanggal_dirubah");d.setDefaultSortAscending(false);for(Object o:d.getFields()){GenericCrudFieldDefinition f=(GenericCrudFieldDefinition)o;f.setCreateable(false);f.setUpdateable(false);}}
	/** Mengembalikan pasangan properti ({@code biodataCalonMahasiswa}, {@code verifikasiKelengkapanCalonMahasiswa}) yang membentuk kunci alami relasi ini. */
	public List getNaturalKeyProperties(){List x=new ArrayList();x.add("biodataCalonMahasiswa");x.add("verifikasiKelengkapanCalonMahasiswa");return x;}}
