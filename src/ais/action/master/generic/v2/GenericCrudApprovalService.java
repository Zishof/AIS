package ais.action.master.generic.v2;

import java.io.Serializable;
import ais.action.master.generic.v2.adapter.GenericCrudApprovalAdapter;

/**
 * Layanan tipis pada framework CRUD generik {@code ais.action.master.generic.v2} yang menangani
 * operasi persetujuan/penolakan (approve/reject) satu baris data. Sebelum mendelegasikan ke
 * {@link GenericCrudApprovalAdapter} spesifik entitas, service ini SELALU memvalidasi hak akses
 * lewat {@link GenericCrudPrivilegeGuard} dan menolak permintaan (403 {@code APPROVAL_DISABLED})
 * bila entitas tidak menyediakan adapter persetujuan — dengan begitu entitas yang belum
 * diimplementasikan alur approval-nya gagal aman (fail closed), bukan diam-diam sukses.
 */
public class GenericCrudApprovalService {
    private final GenericCrudPrivilegeGuard privilege = new GenericCrudPrivilegeGuard();
    /**
     * Menyetujui satu baris data. Memvalidasi hak akses {@link GenericCrudOperation#APPROVE} lebih
     * dulu, lalu mendelegasikan ke {@link GenericCrudApprovalAdapter#approve}.
     *
     * @param context konteks permintaan (identitas pengguna) yang divalidasi hak aksesnya
     * @param id      id baris yang disetujui
     * @param reason  alasan/catatan persetujuan, boleh null
     * @param adapter adapter persetujuan spesifik entitas; bila {@code null}, permintaan ditolak
     * @return hasil operasi persetujuan
     * @throws Exception termasuk {@link GenericCrudException} 403 bila hak akses ditolak atau
     *                    adapter belum dikonfigurasi, atau galat lain dari adapter
     */
    public GenericCrudResult approve(GenericCrudRequestContext context, Serializable id, String reason, GenericCrudApprovalAdapter adapter) throws Exception {
        privilege.require(context, GenericCrudOperation.APPROVE);
        if (adapter == null) throw new GenericCrudException(403, "APPROVAL_DISABLED", "Approval adapter belum dikonfigurasi.");
        return adapter.approve(id, reason, context);
    }
    /**
     * Menolak satu baris data. Memvalidasi hak akses {@link GenericCrudOperation#REJECT} lebih dulu,
     * lalu mendelegasikan ke {@link GenericCrudApprovalAdapter#reject}.
     *
     * @param context konteks permintaan (identitas pengguna) yang divalidasi hak aksesnya
     * @param id      id baris yang ditolak
     * @param reason  alasan penolakan, boleh null
     * @param adapter adapter persetujuan spesifik entitas; bila {@code null}, permintaan ditolak
     * @return hasil operasi penolakan
     * @throws Exception termasuk {@link GenericCrudException} 403 bila hak akses ditolak atau
     *                    adapter belum dikonfigurasi, atau galat lain dari adapter
     */
    public GenericCrudResult reject(GenericCrudRequestContext context, Serializable id, String reason, GenericCrudApprovalAdapter adapter) throws Exception {
        privilege.require(context, GenericCrudOperation.REJECT);
        if (adapter == null) throw new GenericCrudException(403, "APPROVAL_DISABLED", "Approval adapter belum dikonfigurasi.");
        return adapter.reject(id, reason, context);
    }
}
