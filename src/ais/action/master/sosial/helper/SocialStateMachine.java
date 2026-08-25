package ais.action.master.sosial.helper;

import java.util.Arrays;

/** Fail-closed state transition rules shared by API, callback, and finance services. */
public final class SocialStateMachine {
    private SocialStateMachine(){}
    public static void requireDonation(String current,String target){
        if(same(current,target))return;
        if("DRAFT".equals(current)&&one(target,"PENDING_PAYMENT","CANCELLED"))return;
        if("PENDING_PAYMENT".equals(current)&&one(target,"ALLOCATED","CANCELLED","EXPIRED"))return;
        if("ALLOCATED".equals(current)&&one(target,"PARTIALLY_DISTRIBUTED","DISTRIBUTED","COMPLETED"))return;
        if("PARTIALLY_DISTRIBUTED".equals(current)&&one(target,"DISTRIBUTED","COMPLETED"))return;
        throw new IllegalStateException("Transisi transaksi ditolak: "+current+" -> "+target);
    }
    public static void requirePayment(String current,String target){
        if(same(current,target))return;
        if("CREATED".equals(current)&&one(target,"VA_ISSUED","PENDING","PAID","MISMATCH","FAILED"))return;
        if(one(current,"VA_ISSUED","PENDING")&&one(target,"PAID","FAILED","EXPIRED","MISMATCH"))return;
        if("EXPIRED".equals(current)&&"PAID".equals(target))return;
        throw new IllegalStateException("Transisi pembayaran ditolak: "+current+" -> "+target);
    }
    public static void requireDistribution(String current,String target){if("APPROVED".equals(current)&&"POSTED".equals(target))return;throw new IllegalStateException("Transisi penyaluran ditolak: "+current+" -> "+target);}
    private static boolean one(String value,String... allowed){return Arrays.asList(allowed).contains(value);} private static boolean same(String a,String b){return a==null?b==null:a.equals(b);}
}
