package ais.common.newui;

import java.io.Serializable;

import ais.database.model.RolePrivilage;

/**
 * Value object hak akses untuk satu Menu pada role aktif New UI.
 *
 * <p>Semua nilai default <code>false</code> (fail-closed). Dibentuk dari
 * {@link ais.database.model.RolePrivilage} existing (kode 1 = diberikan), sehingga
 * konsisten dengan {@link ais.common.CommonPrivilages}.</p>
 *
 * <p>Kompatibel Java 1.6: tanpa lambda/stream/generic diamond/try-with-resources.
 * Wajib dibangun ketika session Hibernate masih aktif lalu disimpan sebagai DTO,
 * agar tidak memicu LazyInitializationException saat dirender di JSP.</p>
 */
public class NewUiPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean canRead;
    private final boolean canCreate;
    private final boolean canUpdate;
    private final boolean canDelete;
    private final boolean canApprove;
    private final boolean canReject;

    public NewUiPermission(boolean canRead, boolean canCreate, boolean canUpdate, boolean canDelete,
            boolean canApprove, boolean canReject) {
        this.canRead = canRead;
        this.canCreate = canCreate;
        this.canUpdate = canUpdate;
        this.canDelete = canDelete;
        this.canApprove = canApprove;
        this.canReject = canReject;
    }

    /** Semua false — dipakai bila tidak ada baris RolePrivilage (fail-closed). */
    public static NewUiPermission none() {
        return new NewUiPermission(false, false, false, false, false, false);
    }

    /** Bangun dari satu RolePrivilage existing. null → {@link #none()}. */
    public static NewUiPermission from(RolePrivilage rp) {
        if (rp == null) {
            return none();
        }
        return new NewUiPermission(isGranted(rp.getRead()), isGranted(rp.getCreate()), isGranted(rp.getUpdate()),
                isGranted(rp.getDelete()), isGranted(rp.getApprove()), isGranted(rp.getReject()));
    }

    private static boolean isGranted(Integer flag) {
        return flag != null && flag.intValue() == 1;
    }

    public boolean isCanRead() {
        return canRead;
    }

    public boolean isCanCreate() {
        return canCreate;
    }

    public boolean isCanUpdate() {
        return canUpdate;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public boolean isCanApprove() {
        return canApprove;
    }

    public boolean isCanReject() {
        return canReject;
    }

    /** true bila minimal satu hak diberikan. */
    public boolean isAny() {
        return canRead || canCreate || canUpdate || canDelete || canApprove || canReject;
    }
}
