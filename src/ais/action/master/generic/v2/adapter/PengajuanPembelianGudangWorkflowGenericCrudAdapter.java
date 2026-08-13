package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.common.newui.inventory.NewUiPengajuanPembelianGudangService;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.PengajuanPembelianGudang;

/** Parity lifecycle Action: pengajuan dibuat scheduler; operator hanya mengubah status. */
@SuppressWarnings({ "rawtypes" })
public final class PengajuanPembelianGudangWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter {
    public PengajuanPembelianGudangWorkflowGenericCrudAdapter() {
        super(PengajuanPembelianGudang.class, false, null, true);
    }
    public void configure(GenericCrudDefinition definition) {
        definition.setDisplayName("Pengajuan Pembelian Gudang");
        definition.setCreateEnabled(false); definition.setUpdateEnabled(true);
        definition.setDeleteEnabled(false); definition.setImportEnabled(false);
        definition.setDefaultSortProperty("id"); definition.setDefaultSortAscending(false); definition.setDefaultPageSize(200);
        List fields=definition.getFields();
        for(int i=0;i<fields.size();i++){GenericCrudFieldDefinition field=(GenericCrudFieldDefinition)fields.get(i);field.setCreateable(false);field.setUpdateable("status".equals(field.getProperty()));}
    }
    public void beforeSave(Session session, GeneralValueObject target, GenericCrudRequestContext context)throws Exception{
        PengajuanPembelianGudang value=(PengajuanPembelianGudang)target;
        value.setStatus(NewUiPengajuanPembelianGudangService.canonicalStatus(value.getStatus()));
        Tbmuser user=context==null?null:context.getUser();if(user!=null){value.setOleh(user.getUserNama());value.setOlehId(user.getUserId());}
        super.beforeSave(session,target,context);
    }
    public List getNaturalKeyProperties(){List values=new ArrayList();values.add("produk");values.add("gudangAsal");values.add("waktuDibuat");return values;}
}
