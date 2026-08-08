package ais.common.newui.menu;

import ais.database.model.RolePrivilage;

/** Permission DTO hybrid; seluruh nilai default fail-closed. */
public class NewUiPermission extends ais.common.newui.NewUiPermission {

    private static final long serialVersionUID = 1L;

    public NewUiPermission(boolean read, boolean create, boolean update, boolean delete,
            boolean approve, boolean reject) {
        super(read, create, update, delete, approve, reject);
    }

    public static NewUiPermission none() {
        return new NewUiPermission(false, false, false, false, false, false);
    }

    public static NewUiPermission from(RolePrivilage value) {
        if (value == null) return none();
        return new NewUiPermission(granted(value.getRead()), granted(value.getCreate()),
                granted(value.getUpdate()), granted(value.getDelete()),
                granted(value.getApprove()), granted(value.getReject()));
    }

    private static boolean granted(Integer value) {
        return value != null && value.intValue() == 1;
    }
}
