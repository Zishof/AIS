package ais.action.master.sosial.test;

import java.math.BigDecimal;
import ais.action.master.sosial.helper.SocialFinancialInvariantService;
import ais.action.master.sosial.helper.SocialStateMachine;

/**
 * Harness uji manual (dijalankan lewat {@code main}, bukan JUnit) yang memverifikasi invarian
 * perhitungan keuangan sosial di {@link SocialFinancialInvariantService} dan transisi status
 * sah/tidak-sah di {@link SocialStateMachine}.
 *
 * <p>
 * Mencakup: kasus normal (dana masuk-keluar-alokasi konsisten), kasus over-alokasi yang WAJIB
 * ditandai {@link SocialFinancialInvariantService.Snapshot#hasException() exception}, kasus
 * refund dengan alokasi berkurang, perhitungan total tertagih ({@code totalCharged}), serta
 * transisi status donasi/pembayaran yang valid dan yang seharusnya ditolak. Setiap kegagalan
 * dicatat lewat {@link #ok(boolean, String)} (menambah penghitung {@link #failures}, TIDAK
 * langsung melempar), dan {@code main} melempar {@link IllegalStateException} di akhir bila ada
 * kegagalan — sehingga satu proses uji melaporkan seluruh pelanggaran sekaligus, bukan berhenti
 * di kegagalan pertama.
 * </p>
 */
public final class SocialFinancialInvariantSelfTest {
    private static int failures;
    public static void main(String[] args){
        SocialFinancialInvariantService.Snapshot s=SocialFinancialInvariantService.calculate(b("1000"),b("100"),b("700"),b("250"));
        eq(s.getNetSettled(),"900.00","net settled");eq(s.getUnallocated(),"200.00","unallocated");eq(s.getAllocationAvailable(),"450.00","allocation available");ok(!s.hasException(),"valid snapshot");
        SocialFinancialInvariantService.Snapshot bad=SocialFinancialInvariantService.calculate(b("100"),b("0"),b("120"),b("0"));ok(bad.hasException(),"over-allocation must be exception");
        SocialFinancialInvariantService.Snapshot refund=SocialFinancialInvariantService.calculate(b("100"),b("20"),b("80"),b("30"));eq(refund.getNetSettled(),"80.00","refund net settled");eq(refund.getUnallocated(),"0.00","refund unallocated");eq(refund.getAllocationAvailable(),"50.00","refund allocation available");ok(!refund.hasException(),"refund with reduced allocation");
        eq(SocialFinancialInvariantService.totalCharged(b("1000"),b("50"),b("5")),"1055.00","total charged");
        SocialStateMachine.requireDonation("DRAFT","PENDING_PAYMENT");SocialStateMachine.requirePayment("CREATED","VA_ISSUED");deniedDonation("ALLOCATED","DRAFT");deniedPayment("PAID","PENDING");
        if(failures>0)throw new IllegalStateException(failures+" Social financial invariant failures.");System.out.println("SocialFinancialInvariantSelfTest OK");
    }
    private static BigDecimal b(String v){return new BigDecimal(v);} private static void eq(BigDecimal a,String b,String label){ok(a.compareTo(new BigDecimal(b))==0,label+": "+a);} private static void ok(boolean yes,String label){if(!yes){System.out.println("GAGAL: "+label);failures++;}}
    private static void deniedDonation(String a,String b){try{SocialStateMachine.requireDonation(a,b);ok(false,"donation transition should fail");}catch(IllegalStateException expected){}}
    private static void deniedPayment(String a,String b){try{SocialStateMachine.requirePayment(a,b);ok(false,"payment transition should fail");}catch(IllegalStateException expected){}}
}
