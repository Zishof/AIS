package ais.action.master.sosial.test;

import java.math.BigDecimal;
import ais.action.master.sosial.helper.SocialFinancialInvariantService;
import ais.action.master.sosial.helper.SocialStateMachine;

public final class SocialFinancialInvariantSelfTest {
    private static int failures;
    public static void main(String[] args){
        SocialFinancialInvariantService.Snapshot s=SocialFinancialInvariantService.calculate(b("1000"),b("100"),b("700"),b("250"));
        eq(s.getNetSettled(),"900.00","net settled");eq(s.getUnallocated(),"200.00","unallocated");eq(s.getAllocationAvailable(),"450.00","allocation available");ok(!s.hasException(),"valid snapshot");
        SocialFinancialInvariantService.Snapshot bad=SocialFinancialInvariantService.calculate(b("100"),b("0"),b("120"),b("0"));ok(bad.hasException(),"over-allocation must be exception");
        eq(SocialFinancialInvariantService.totalCharged(b("1000"),b("50"),b("5")),"1055.00","total charged");
        SocialStateMachine.requireDonation("DRAFT","PENDING_PAYMENT");SocialStateMachine.requirePayment("CREATED","VA_ISSUED");deniedDonation("ALLOCATED","DRAFT");deniedPayment("PAID","PENDING");
        if(failures>0)throw new IllegalStateException(failures+" Social financial invariant failures.");System.out.println("SocialFinancialInvariantSelfTest OK");
    }
    private static BigDecimal b(String v){return new BigDecimal(v);} private static void eq(BigDecimal a,String b,String label){ok(a.compareTo(new BigDecimal(b))==0,label+": "+a);} private static void ok(boolean yes,String label){if(!yes){System.out.println("GAGAL: "+label);failures++;}}
    private static void deniedDonation(String a,String b){try{SocialStateMachine.requireDonation(a,b);ok(false,"donation transition should fail");}catch(IllegalStateException expected){}}
    private static void deniedPayment(String a,String b){try{SocialStateMachine.requirePayment(a,b);ok(false,"payment transition should fail");}catch(IllegalStateException expected){}}
}
