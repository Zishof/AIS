package ais.action.servlet.api;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.PasswordChecker;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.WaktuUtil;

/**
 * API perubahan password milik sendiri untuk aplikasi Android/Desktop.
 *
 * <p>Implementasi ini mempertahankan aturan {@code ChangePasswordWindow} ZK:
 * password lama harus cocok, password baru mengikuti {@link PasswordChecker},
 * nilai disimpan terenkripsi, tanggal perubahan diperbarui, dan user-access
 * eksternal disinkronkan. Endpoint tahap pertama dibatasi untuk Mahasiswa agar
 * tidak mencampur aturan password siswa/orang-tua/pegawai yang berbeda.</p>
 */
public final class PasswordSayaApi {
    private PasswordSayaApi() { }

    /**
     * Mengubah password akun {@link Tbmuser} yang sedang login.
     *
     * <p>Jalur ini dipakai menu administratif "Ubah Password". Identitas akun
     * selalu diambil dari token; {@code userId} dari klien tidak pernah
     * diterima. Password lama diverifikasi sebelum transaksi dan seluruh field
     * rahasia tetap online-only di sisi aplikasi.</p>
     */
    public static JSONObject ubahPengguna(HttpServletRequest req, JSONObject request) {
        Tbmuser caller = ApiUtil.currentUser(request, req);
        if (caller == null || caller.getUserId() == null) {
            return ApiHelperSupport.status("97", "Token pengguna tidak valid atau sudah berakhir.");
        }
        String oldPassword = ApiHelperSupport.optString(request, "password_lama").trim();
        String newPassword = ApiHelperSupport.optString(request, "password_baru").trim();
        String confirmation = ApiHelperSupport.optString(request, "konfirmasi_password").trim();
        JSONObject validation = validasiPassword(oldPassword, newPassword, confirmation);
        if (validation != null) {
            return validation;
        }

        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.openSession();
            transaction = session.beginTransaction();
            Tbmuser user = (Tbmuser) session.get(Tbmuser.class, caller.getUserId());
            if (user == null || !ApiHelperSupport.hasText(user.getUserPassword())) {
                ApiHelperSupport.rollbackQuietly(transaction);
                return ApiHelperSupport.status("99", "Data akun pengguna tidak ditemukan.");
            }
            String currentPassword = Common.desEncrypter.get().decrypt(user.getUserPassword().trim());
            if (!oldPassword.equals(currentPassword)) {
                ApiHelperSupport.rollbackQuietly(transaction);
                return ApiHelperSupport.status("98", "Password lama yang dimasukkan salah.");
            }
            user.setUserPassword(Common.desEncrypter.get().encrypt(newPassword));
            user.setIs_encripted(Boolean.TRUE);
            user.setUbahPasword(WaktuUtil.getDate());
            session.update(user);
            transaction.commit();

            Common.saveOrUpdateUserAccess(user, null, user.getUserId(), newPassword, user.getEmail());
            caller.setUserPassword(user.getUserPassword());
            caller.setIs_encripted(Boolean.TRUE);
            caller.setUbahPasword(user.getUbahPasword());
            return ApiHelperSupport.status("00", "Password pengguna berhasil diubah.");
        } catch (Exception e) {
            ApiHelperSupport.rollbackQuietly(transaction);
            Common.tampilErrorJikaAdmin(e);
            return ApiHelperSupport.errorResponse("Perubahan password gagal diproses.");
        } finally {
            ApiHelperSupport.closeOpenedSession(session);
        }
    }

    public static JSONObject ubahMahasiswa(HttpServletRequest req, JSONObject request) {
        Tbmuser caller = ApiUtil.currentUser(request, req);
        if (caller == null || caller.getMahasiswa() == null || caller.getMahasiswa().getId() == null) {
            return ApiHelperSupport.status("97", "Token mahasiswa tidak valid atau sudah berakhir.");
        }
        String oldPassword = ApiHelperSupport.optString(request, "password_lama").trim();
        String newPassword = ApiHelperSupport.optString(request, "password_baru").trim();
        String confirmation = ApiHelperSupport.optString(request, "konfirmasi_password").trim();
        JSONObject validation = validasiPassword(oldPassword, newPassword, confirmation);
        if (validation != null) {
            return validation;
        }

        Session session = null;
        Transaction transaction = null;
        Mahasiswa mahasiswa = null;
        try {
            session = HibernateUtil.openSession();
            transaction = session.beginTransaction();
            mahasiswa = (Mahasiswa) session.get(Mahasiswa.class, caller.getMahasiswa().getId());
            if (mahasiswa == null || !ApiHelperSupport.hasText(mahasiswa.getPass())) {
                ApiHelperSupport.rollbackQuietly(transaction);
                return ApiHelperSupport.status("99", "Data akun mahasiswa tidak ditemukan.");
            }
            String currentPassword = Common.desEncrypter.get().decrypt(mahasiswa.getPass().trim());
            if (!oldPassword.equals(currentPassword)) {
                ApiHelperSupport.rollbackQuietly(transaction);
                return ApiHelperSupport.status("98", "Password lama yang dimasukkan salah.");
            }
            mahasiswa.setPass(Common.desEncrypter.get().encrypt(newPassword));
            mahasiswa.setUbahPasword(WaktuUtil.getDate());
            session.update(mahasiswa);
            transaction.commit();

            // Sama seperti ZK: kredensial login eksternal ikut diperbarui setelah
            // transaksi utama berhasil, tanpa pernah mengembalikan password.
            Common.saveOrUpdateUserAccess(null, mahasiswa, mahasiswa.getNim(),
                    newPassword, mahasiswa.getEmail());
            caller.setMahasiswa(mahasiswa);
            return ApiHelperSupport.status("00", "Password mahasiswa berhasil diubah.");
        } catch (Exception e) {
            ApiHelperSupport.rollbackQuietly(transaction);
            Common.tampilErrorJikaAdmin(e);
            return ApiHelperSupport.errorResponse("Perubahan password gagal diproses.");
        } finally {
            ApiHelperSupport.closeOpenedSession(session);
        }
    }

    private static JSONObject validasiPassword(String oldPassword, String newPassword, String confirmation) {
        if (!ApiHelperSupport.hasText(oldPassword) || !ApiHelperSupport.hasText(newPassword)
                || !ApiHelperSupport.hasText(confirmation)) {
            return ApiHelperSupport.status("98", "Password lama, password baru, dan konfirmasi wajib diisi.");
        }
        if (!newPassword.equals(confirmation)) {
            return ApiHelperSupport.status("98", "Konfirmasi password baru tidak sama.");
        }
        if (!PasswordChecker.isValidPassword(newPassword)) {
            return ApiHelperSupport.status("98",
                    "Password minimal 8 karakter dan wajib mengandung huruf, angka, serta karakter spesial.");
        }
        return null;
    }
}
