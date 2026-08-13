package ais.common.newui.menu.test;

import ais.common.newui.menu.NewUiDepositoAroService;
import ais.common.newui.menu.NewUiDepositoAroService.Dashboard;

public final class NewUiDepositoAroDatabaseAudit {
    private NewUiDepositoAroDatabaseAudit() { }
    public static void main(String[] args) {
        Dashboard value = NewUiDepositoAroService.load();
        check(value.getRows() != null, "daftar ARO null");
        check(value.automaticCount >= 0 && value.dueCount >= 0
                && value.automaticValue >= 0D && value.dueValue >= 0D, "ringkasan ARO tidak valid");
        System.out.println("NewUiDepositoAroDatabaseAudit OK rows=" + value.getRows().size()
                + " automatic=" + value.automaticCount + " due=" + value.dueCount);
        System.exit(0);
    }
    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
