package ais.action.master.generic.v2.test;
import java.util.ArrayList;import java.util.List;import org.hibernate.Session;import org.hibernate.Transaction;import org.hibernate.criterion.Restrictions;import ais.common.newui.inventory.NewUiPembelianService;import ais.common.newui.inventory.NewUiPembelianService.LineInput;import ais.common.newui.inventory.NewUiPembelianService.Option;import ais.common.newui.inventory.NewUiPembelianService.Snapshot;import ais.database.hibernate.HibernateUtil;import ais.database.model.inventory.Pembelian;
/** Smoke create/read/update/delete nyata, selalu membersihkan invoice uji. */
@SuppressWarnings({"rawtypes","unchecked"}) public final class PembelianWorkflowDatabaseSelfTest{
 private PembelianWorkflowDatabaseSelfTest(){}
 public static void main(String[]a)throws Exception{
  System.setProperty("javax.persistence.validation.mode","none");System.setProperty("hibernate.validator.apply_to_ddl","false");
  String invoice="CODEX-POS-"+System.currentTimeMillis();NewUiPembelianService svc=new NewUiPembelianService();
  try{Snapshot initial=svc.load(null,null,null,null,null,0,10,null,null);check(!initial.shops.isEmpty(),"toko aktif");Option shop=initial.shops.get(0);List<Option> products=svc.findProducts(shop.id,null,null);check(!products.isEmpty(),"produk toko");Option product=products.get(0);List<LineInput> lines=new ArrayList<LineInput>();lines.add(new LineInput(product.id,1));Pembelian first=entity(svc.save(null,invoice,shop.id,null,null,"uji create",lines,null,null).id);check(first!=null&&invoice.equals(first.getKode()),"create");lines.clear();lines.add(new LineInput(product.id,2));svc.save(first.getId(),invoice,shop.id,null,null,"uji update",lines,null,null);Pembelian updated=entity(first.getId());check(updated!=null&&updated.getQty().doubleValue()==2&&"uji update".equals(updated.getKeterangan()),"update");Snapshot found=svc.load(invoice,shop.id,null,null,null,0,10,null,null);check(found.total==1,"read/search");svc.delete(first.getId(),null,null);check(entity(first.getId())==null,"delete");System.out.println("PembelianWorkflowDatabaseSelfTest OK");}
  finally{cleanup(invoice);}System.exit(0);
 }
 private static Pembelian entity(Long id){Session s=HibernateUtil.getSessionFactory().openSession();try{return(Pembelian)s.get(Pembelian.class,id);}finally{s.close();}}
 private static void cleanup(String invoice){Session s=null;Transaction t=null;try{s=HibernateUtil.getSessionFactory().openSession();t=s.beginTransaction();List rows=s.createCriteria(Pembelian.class).add(Restrictions.eq("kode",invoice)).list();for(int i=0;i<rows.size();i++)s.delete(rows.get(i));t.commit();}catch(Exception e){if(t!=null)try{t.rollback();}catch(Exception ignored){}}finally{if(s!=null)try{s.close();}catch(Exception ignored){}}}
 private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}
}
