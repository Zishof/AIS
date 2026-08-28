package ais.common.newui.menu.test;

import ais.common.newui.NewUiPermission;
import ais.common.newui.menu.NewUiHybridMenuNode;
import ais.common.newui.menu.NewUiHybridMenuRouteGuard;

/**
 * Self-test pemetaan action → privilege pada guard shell, khususnya aksi
 * workflow keuangan (tagihan): history/move_targets = read, generate/sync/
 * toggle = update, move = delete. Fail-closed: tanpa read semua ditolak dan
 * action tak dikenal selalu ditolak.
 */
public final class NewUiHybridMenuRouteGuardSelfTest {
    private NewUiHybridMenuRouteGuardSelfTest() { }

    private static NewUiHybridMenuNode node(boolean read, boolean create, boolean update,
            boolean delete) {
        NewUiHybridMenuNode value = new NewUiHybridMenuNode();
        value.setPermission(new NewUiPermission(read, create, update, delete, false, false));
        return value;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }

    public static void main(String[] args) {
        NewUiHybridMenuNode readOnly = node(true, false, false, false);
        check(NewUiHybridMenuRouteGuard.isActionAuthorized(null, readOnly, "list"), "list read");
        check(NewUiHybridMenuRouteGuard.isActionAuthorized(null, readOnly, "history"), "history = read");
        check(NewUiHybridMenuRouteGuard.isActionAuthorized(null, readOnly, "move_targets"), "move_targets = read");
        check(!NewUiHybridMenuRouteGuard.isActionAuthorized(null, readOnly, "generate"), "generate butuh update");
        check(!NewUiHybridMenuRouteGuard.isActionAuthorized(null, readOnly, "sync"), "sync butuh update");
        check(!NewUiHybridMenuRouteGuard.isActionAuthorized(null, readOnly, "toggle"), "toggle butuh update");
        check(!NewUiHybridMenuRouteGuard.isActionAuthorized(null, readOnly, "move"), "move butuh delete");
        check(!NewUiHybridMenuRouteGuard.isActionAuthorized(null, readOnly, "delete"), "delete butuh delete");
        check(!NewUiHybridMenuRouteGuard.isActionAuthorized(null, readOnly, "aksi_ngawur"), "unknown fail-closed");

        NewUiHybridMenuNode updater = node(true, false, true, false);
        check(NewUiHybridMenuRouteGuard.isActionAuthorized(null, updater, "generate"), "generate = update");
        check(NewUiHybridMenuRouteGuard.isActionAuthorized(null, updater, "sync"), "sync = update");
        check(NewUiHybridMenuRouteGuard.isActionAuthorized(null, updater, "toggle"), "toggle = update");
        check(!NewUiHybridMenuRouteGuard.isActionAuthorized(null, updater, "move"), "move bukan update");

        NewUiHybridMenuNode deleter = node(true, false, false, true);
        check(NewUiHybridMenuRouteGuard.isActionAuthorized(null, deleter, "move"), "move = delete");
        check(NewUiHybridMenuRouteGuard.isActionAuthorized(null, deleter, "delete"), "delete = delete");
        check(!NewUiHybridMenuRouteGuard.isActionAuthorized(null, deleter, "generate"), "generate bukan delete");

        NewUiHybridMenuNode noRead = node(false, true, true, true);
        check(!NewUiHybridMenuRouteGuard.isActionAuthorized(null, noRead, "list"), "tanpa read semua ditolak");
        check(!NewUiHybridMenuRouteGuard.isActionAuthorized(null, noRead, "generate"), "tanpa read generate ditolak");

        System.out.println("PASS NewUiHybridMenuRouteGuard action mapping self-test");
    }
}
