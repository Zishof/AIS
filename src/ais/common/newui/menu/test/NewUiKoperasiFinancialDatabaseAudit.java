package ais.common.newui.menu.test;

import java.io.ByteArrayOutputStream;

import ais.common.newui.menu.NewUiKoperasiFinancialService;
import ais.common.newui.menu.NewUiKoperasiFinancialService.Summary;

public final class NewUiKoperasiFinancialDatabaseAudit {
    private NewUiKoperasiFinancialDatabaseAudit() { }

    public static void main(String[] args) throws Exception {
        Summary value = NewUiKoperasiFinancialService.load();
        check(finite(value.totalAset) && finite(value.outstandingPokok)
                && finite(value.totalPpap) && finite(value.arusKasBersih), "nilai laporan tidak valid");
        check(value.totalAset >= 0D && value.outstandingPokok >= 0D && value.totalPpap >= 0D,
                "nilai non-negatif");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        NewUiKoperasiFinancialService.writeWorkbook(value, output);
        check(output.size() > 1000, "workbook kosong");
        System.out.println("NewUiKoperasiFinancialDatabaseAudit OK assets=" + value.totalAset
                + " outstanding=" + value.outstandingPokok + " xlsx=" + output.size());
        System.exit(0);
    }

    private static boolean finite(double value) {
        return !Double.isInfinite(value) && !Double.isNaN(value);
    }
    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
