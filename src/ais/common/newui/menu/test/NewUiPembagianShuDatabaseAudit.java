package ais.common.newui.menu.test;

import java.util.Calendar;
import org.hibernate.Session;
import ais.common.newui.menu.NewUiPembagianShuService;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.Koperasi;

public final class NewUiPembagianShuDatabaseAudit {
    public static void main(String[] args) {
        Session session=HibernateUtil.openSession(); Koperasi koperasi=null;
        try{koperasi=(Koperasi)session.createQuery("from Koperasi k order by k.id").setMaxResults(1).uniqueResult();}finally{session.close();}
        if(koperasi==null){System.out.println("NewUiPembagianShuDatabaseAudit SKIP no cooperative");System.exit(0);}
        NewUiPembagianShuService.Result result=new NewUiPembagianShuService().load(Calendar.getInstance().get(Calendar.YEAR),koperasi);
        System.out.println("NewUiPembagianShuDatabaseAudit OK cooperative="+koperasi.getId()+" rows="+result.rows.size()+" distributed="+result.distributed);System.exit(0);
    }
}
