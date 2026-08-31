package ais.action.master.generic.v2.adapter;import java.util.ArrayList;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.GenericCrudFieldDefinition;import ais.database.model.akunting.Transitori;/**
 * Adapter CRUD generik untuk entitas akunting {@link Transitori}, dikonfigurasi sebagai
 * <b>hanya-baca</b>: seluruh mutasi (buat/ubah/hapus/impor) dinonaktifkan dan seluruh field
 * ditandai tidak dapat diisi/diubah lewat formulir generik. Diurutkan menurun berdasarkan
 * {@code id} secara default. Kunci alami entitas ini adalah kombinasi
 * {@code daftarPengajuanTransfer} dan {@code kode}.
 */
@SuppressWarnings("rawtypes")public final class TransitoriWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter{
	/** Membuat adapter untuk {@link Transitori} tanpa dukungan detail/anak (leaf entity). */
	public TransitoriWorkflowGenericCrudAdapter(){super(Transitori.class,false,null,true);}
	/** Menerapkan status hanya-baca (lifecycle {@code READ_ONLY}, semua mutasi nonaktif) pada definisi CRUD generik entitas ini. */
	public void configure(GenericCrudDefinition d){d.setDisplayName("Transitori");d.setLifecycleStatus(GenericCrudDefinition.READ_ONLY);d.setCreateEnabled(false);d.setUpdateEnabled(false);d.setDeleteEnabled(false);d.setImportEnabled(false);d.setDefaultSortProperty("id");d.setDefaultSortAscending(false);for(Object o:d.getFields()){GenericCrudFieldDefinition f=(GenericCrudFieldDefinition)o;f.setCreateable(false);f.setUpdateable(false);}}
	/** @return kolom kunci alami entitas: {@code daftarPengajuanTransfer} dan {@code kode}. */
	public List getNaturalKeyProperties(){List v=new ArrayList();v.add("daftarPengajuanTransfer");v.add("kode");return v;}}
