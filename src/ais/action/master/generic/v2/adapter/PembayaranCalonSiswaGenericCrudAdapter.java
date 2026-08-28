package ais.action.master.generic.v2.adapter;
import java.util.Arrays;import java.util.List;import org.hibernate.Criteria;import org.hibernate.criterion.Restrictions;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.GenericCrudRequestContext;import ais.database.model.sekolah.PembayaranSiswa;
/** Riwayat pembayaran CALON siswa (menu 8755593): read-only dan selalu terfilter calonSiswa agar tidak tertukar dengan pembayaran siswa aktif; proses bayar tetap di PembayaranCalonSiswaAction/pem_online sampai kontrak L0 tersedia. */
@SuppressWarnings({"rawtypes","unchecked"})public final class PembayaranCalonSiswaGenericCrudAdapter extends GenericCrudAutoEntityAdapter{
    public PembayaranCalonSiswaGenericCrudAdapter(){super(PembayaranSiswa.class,false,null,true);}
    public void configure(GenericCrudDefinition d){PerkuliahanWorkflowGenericCrudAdapter.lock(d,"Pembayaran Calon Siswa","tanggalBayar");}
    public void applyDefaultFilters(Criteria criteria,GenericCrudRequestContext context)throws Exception{criteria.add(Restrictions.isNotNull("calonSiswa"));}
    public List getNaturalKeyProperties(){return Arrays.asList("calonSiswa","jenisBiayaSekolah","tanggalBayar","nominal");}
}
