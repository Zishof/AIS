package ais.action.master.jurnal.test;

import ais.action.master.jurnal.JurnalMenuReconciler;
import ais.database.hibernate.HibernateUtil;

/** Read-only by default; applies only when the reconciler opt-in is present. */
public final class JurnalMenuDatabaseSelfTest {
    private JurnalMenuDatabaseSelfTest(){}
    public static void main(String[]args){
        String db=System.getenv("AIS_JURNAL_DB_NAME");
        if(db==null||"ais".equalsIgnoreCase(db))throw new IllegalStateException("Menu DB test wajib memakai clone.");
        System.setProperty("javax.persistence.validation.mode","none");
        try{
            JurnalMenuReconciler service=new JurnalMenuReconciler();
            JurnalMenuReconciler.Result before=service.inspect();
            if(!before.safe())throw new IllegalStateException("Collision menu: "+before.conflicts);
            if("true".equalsIgnoreCase(System.getenv("AIS_JURNAL_MENU_RECONCILE"))){
                service.reconcile("JRN_MENU_SELF_TEST");
                JurnalMenuReconciler.Result after=service.inspect();
                if(!after.safe()||after.unchanged.size()!=29)throw new IllegalStateException("Menu belum canonical setelah reconcile.");
                System.out.println("JurnalMenuDatabaseSelfTest OK applied=29 collision=0 idempotent");
            }else{
                System.out.println("JurnalMenuDatabaseSelfTest OK read-only inserts="+before.inserts.size()+" updates="+before.updates.size()+" unchanged="+before.unchanged.size()+" collision=0");
            }
        }finally{HibernateUtil.closeSession();}
        System.exit(0);
    }
}
