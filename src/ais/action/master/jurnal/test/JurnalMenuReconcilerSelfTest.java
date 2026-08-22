package ais.action.master.jurnal.test;

import java.util.ArrayList;
import java.util.List;

import ais.action.master.jurnal.JurnalMenuReconciler;
import ais.database.model.Menu;

public final class JurnalMenuReconcilerSelfTest {
    private JurnalMenuReconcilerSelfTest(){}
    public static void main(String[]args){
        JurnalMenuReconciler service=new JurnalMenuReconciler();
        check(service.desired().size()==29,"parent + 28 child wajib tersedia");
        List<Menu> clean=new ArrayList<Menu>();
        JurnalMenuReconciler.Result empty=service.inspectRows(clean);
        check(empty.safe()&&empty.inserts.size()==29,"empty plan harus menghasilkan 29 insert");
        for(JurnalMenuReconciler.Desired d:service.desired())clean.add(row(d.id,d.root,d.child,d.label,d.url,d.order));
        JurnalMenuReconciler.Result stable=service.inspectRows(clean);
        check(stable.safe()&&stable.unchanged.size()==29&&stable.inserts.isEmpty()&&stable.updates.isEmpty(),"rerun harus idempoten");
        List<Menu> managedLegacy=new ArrayList<Menu>();
        managedLegacy.add(row(JurnalMenuReconciler.PARENT_ID,46L,4605L,"Jurnal","/jurnal/admin/dashboard",0));
        managedLegacy.add(row(2000460501L,4605L,460501L,"Dashboard Jurnal","/jurnal/admin/dashboard",1));
        check(service.inspectRows(managedLegacy).safe(),"label/URL lama antar-PK reserved harus dapat direkonsiliasi atomik");
        List<Menu> collision=new ArrayList<Menu>(clean);collision.add(row(999999L,99L,99L,"Menu lain","/jurnal/admin/dashboard",99));
        JurnalMenuReconciler.Result denied=service.inspectRows(collision);
        check(!denied.safe()&&!denied.conflicts.isEmpty(),"collision URL harus fail-closed");
        List<Menu> stolen=new ArrayList<Menu>();stolen.add(row(JurnalMenuReconciler.PARENT_ID,1L,2L,"Bukan jurnal","/lain",1));
        check(!service.inspectRows(stolen).safe(),"reserved ID dengan hierarchy lain harus ditolak");
        System.out.println("JurnalMenuReconcilerSelfTest OK 29-row-plan idempotent collision-fail-closed");
    }
    private static Menu row(long id,long root,long child,String label,String url,int order){Menu m=new Menu();m.setId(Long.valueOf(id));m.setRoot(Long.valueOf(root));m.setChild(Long.valueOf(child));m.setLabel(label);m.setUrl(url);m.setNomorUrut(Integer.valueOf(order));m.setAktif(Boolean.TRUE);return m;}
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
