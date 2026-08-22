package ais.action.master.jurnal.test;

import java.util.Date;
import ais.action.master.jurnal.JurnalUsageEventService;

public final class JurnalUsageEventSelfTest {
    public static void main(String[]args){Date d=new Date(1787414400000L);String a=JurnalUsageEventService.visitorHash("192.0.2.1","Browser",d),b=JurnalUsageEventService.visitorHash("192.0.2.1","Browser",d);check(a.equals(b)&&a.matches("[0-9a-f]{64}"),"hash tidak stabil");check(!a.contains("192.0.2.1"),"IP bocor");check("BOT".equals(JurnalUsageEventService.agentClass("ResearchBot/1"))&&"BROWSER".equals(JurnalUsageEventService.agentClass("Mozilla/5.0")),"agent class salah");check("example.org".equals(JurnalUsageEventService.referrerHost("https://Example.Org/path?q=secret")),"referrer host salah");check(JurnalUsageEventService.referrerHost("javascript:alert(1)")==null,"scheme invalid lolos");System.out.println("JurnalUsageEventSelfTest OK daily-HMAC no-IP bot-class referrer-host-only");}
    private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
}
