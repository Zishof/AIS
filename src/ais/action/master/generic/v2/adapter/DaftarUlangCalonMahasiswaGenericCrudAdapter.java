package ais.action.master.generic.v2.adapter;
import java.util.Arrays;import java.util.List;import org.hibernate.Criteria;import org.hibernate.criterion.Restrictions;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.GenericCrudRequestContext;import ais.database.model.Kegiatan;
/** Daftar ulang CALON mahasiswa (menu 1003000017): daftar Kegiatan read-only yang selalu terfilter calonMahasiswa; alur bayar/validasi tetap di DaftarUlangCalonMahasiswaAction sampai kontrak L0 tersedia. */
@SuppressWarnings({"rawtypes","unchecked"})public final class DaftarUlangCalonMahasiswaGenericCrudAdapter extends GenericCrudAutoEntityAdapter{
    public DaftarUlangCalonMahasiswaGenericCrudAdapter(){super(Kegiatan.class,false,null,true);}
    public void configure(GenericCrudDefinition d){PerkuliahanWorkflowGenericCrudAdapter.lock(d,"Daftar Ulang Calon Mahasiswa","tanggal");}
    public void applyDefaultFilters(Criteria criteria,GenericCrudRequestContext context)throws Exception{criteria.add(Restrictions.isNotNull("calonMahasiswa"));}
    public List getNaturalKeyProperties(){return Arrays.asList("kodeunik");}
}
