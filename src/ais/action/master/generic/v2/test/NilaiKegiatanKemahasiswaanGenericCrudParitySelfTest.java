package ais.action.master.generic.v2.test;
import java.util.List;import java.util.Map;
import ais.action.master.generic.v2.*;import ais.action.master.generic.v2.adapter.*;import ais.database.model.NilaiKegiatanKemahasiswaan;
@SuppressWarnings("rawtypes") public final class NilaiKegiatanKemahasiswaanGenericCrudParitySelfTest {
 private NilaiKegiatanKemahasiswaanGenericCrudParitySelfTest(){} public static void main(String[]a)throws Exception{
  NilaiKegiatanKemahasiswaanGenericCrudAdapter x=new NilaiKegiatanKemahasiswaanGenericCrudAdapter();GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(NilaiKegiatanKemahasiswaan.class);d.setCreateEnabled(true);d.setDeleteEnabled(true);d.setImportEnabled(true);x.configure(d);
  check(!d.isCreateEnabled()&&d.isUpdateEnabled()&&!d.isDeleteEnabled()&&!d.isImportEnabled(),"capability");
  GenericCrudRequestContext c=new GenericCrudRequestContext();set(c,"canRead",Boolean.TRUE);set(c,"canUpdate",Boolean.TRUE);List q=x.getActions(d,c);check(q.size()==1&&"initialize_score_matrix".equals(((Map)q.get(0)).get("actionKey")),"matrix action");check(x.getNaturalKeyProperties().contains("kodeUnik"),"natural key");System.out.println("NilaiKegiatanKemahasiswaanGenericCrudParitySelfTest OK");System.exit(0);
 }private static void set(Object o,String n,Object v)throws Exception{java.lang.reflect.Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}
}
