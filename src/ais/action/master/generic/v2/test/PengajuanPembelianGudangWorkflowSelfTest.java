package ais.action.master.generic.v2.test;
import java.util.List;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;
import ais.action.master.generic.v2.adapter.PengajuanPembelianGudangWorkflowGenericCrudAdapter;
import ais.common.newui.inventory.NewUiPengajuanPembelianGudangService;
import ais.database.model.inventory.PengajuanPembelianGudang;
@SuppressWarnings("rawtypes") public final class PengajuanPembelianGudangWorkflowSelfTest{
 private PengajuanPembelianGudangWorkflowSelfTest(){} public static void main(String[]a)throws Exception{
  PengajuanPembelianGudangWorkflowGenericCrudAdapter x=new PengajuanPembelianGudangWorkflowGenericCrudAdapter();GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(PengajuanPembelianGudang.class);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);d.setImportEnabled(true);x.configure(d);
  check(!d.isCreateEnabled()&&d.isUpdateEnabled()&&!d.isDeleteEnabled()&&!d.isImportEnabled(),"lifecycle");List fs=d.getFields();for(int i=0;i<fs.size();i++){GenericCrudFieldDefinition f=(GenericCrudFieldDefinition)fs.get(i);check(!f.isUpdateable()||"status".equals(f.getProperty()),"status-only");}
  check(GenericCrudReviewedAdapterFactory.isReviewed(PengajuanPembelianGudang.class),"reviewed");check("BARU".equals(NewUiPengajuanPembelianGudangService.canonicalStatus("baru")),"canonical");boolean failed=false;try{NewUiPengajuanPembelianGudangService.canonicalStatus("LAIN");}catch(IllegalArgumentException expected){failed=true;}check(failed,"reject invalid");
  System.out.println("PengajuanPembelianGudangWorkflowSelfTest OK");System.exit(0);
 }private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}
}
