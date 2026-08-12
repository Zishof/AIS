package ais.common.newui.menu.test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ais.common.newui.menu.NewUiIndexToolbarParityService;
import ais.common.newui.menu.NewUiIndexToolbarParityService.Item;
import ais.common.newui.menu.NewUiModuleDashboardService;
import ais.common.newui.menu.NewUiModuleFunctionService;

/** Uji kontrak seluruh 29 toolbarbutton index.zul, termasuk utility shell. */
public final class NewUiIndexToolbarParitySelfTest {
    private NewUiIndexToolbarParitySelfTest() { }
    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
    public static void main(String[] args) {
        List<Item> items = NewUiIndexToolbarParityService.items();
        check(items.size() == 29, "seluruh toolbarbutton index.zul harus terdaftar");
        check(NewUiIndexToolbarParityService.isComplete(), "kontrak toolbar belum lengkap");
        Set<String> ids = new HashSet<String>(); Set<String> moduleKeys = new HashSet<String>();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            check(ids.add(item.getLegacyId()), "id toolbar duplikat: " + item.getLegacyId());
            check(item.getLegacyHandler() != null && item.getLegacyHandler().length() > 0,
                    "handler existing kosong: " + item.getLegacyId());
            check(item.getNativeMarker() != null && item.getNativeMarker().length() > 0,
                    "marker native kosong: " + item.getLegacyId());
            if (item.isModule()) {
                check(moduleKeys.add(item.getModuleKey()), "module toolbar duplikat: " + item.getModuleKey());
                check(NewUiModuleDashboardService.supports(item.getModuleKey()),
                        "dashboard native belum ada: " + item.getModuleKey());
                check(NewUiModuleFunctionService.supports(item.getModuleKey()),
                        "fungsi native belum ada: " + item.getModuleKey());
            }
        }
        check(moduleKeys.size() == 24, "jumlah toolbar modul harus 24");
        System.out.println("NewUiIndexToolbarParitySelfTest OK controls=" + items.size()
                + " modules=" + moduleKeys.size());
    }
}
