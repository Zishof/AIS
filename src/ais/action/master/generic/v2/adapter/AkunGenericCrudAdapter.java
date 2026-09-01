package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.akunting.Akun;

/**
 * Adapter CRUD untuk Setup Kode Akun (bagan akun).
 *
 * <p>Menu ini sebelumnya READ_ONLY. Bagan akun adalah <b>master data</b>, bukan
 * transaksi keuangan — inilah bedanya dengan layar lain di cabang Akuntansi yang
 * sengaja tetap ditahan. Menambah atau mengubah kode akun tidak memindahkan
 * uang; ia menyiapkan tempat pencatatannya.</p>
 *
 * <p><b>Kelima aturan simpan milik layar lama dipindahkan.</b> Empat pertama
 * berupa kolom wajib. Yang kelima paling penting dan paling mudah terlewat:
 * <b>kode akun harus unik</b>. Kode ganda pada bagan akun tidak menimbulkan
 * galat apa pun — jurnal tetap terposting, lalu dua akun berbeda bercampur pada
 * laporan keuangan, dan itu baru ketahuan saat neraca tidak seimbang.</p>
 *
 * <p>Pemeriksaan keunikan sengaja dilakukan pada {@link #beforeSave}, bukan pada
 * {@code validateCreate}: hanya di sanalah tersedia {@link Session} yang sudah
 * berada di dalam transaksi yang sama dengan penyimpanannya. Menaruhnya di
 * validasi berbasis nilai berarti memeriksa di luar transaksi — jendela yang
 * membiarkan dua penyimpanan bersamaan lolos berdua.</p>
 *
 * @see AbstractGenericCrudEntityAdapter
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class AkunGenericCrudAdapter extends AbstractGenericCrudEntityAdapter<Akun>
        implements GenericCrudScopeAdapter {

    public Akun createNew(GenericCrudRequestContext context) { return new Akun(); }

    public void validateCreate(Map values, GenericCrudRequestContext context, List errors) {
        validate(values, errors);
    }

    public void validateUpdate(Akun current, Map values, GenericCrudRequestContext context, List errors) {
        validate(values, errors);
    }

    private void validate(Map values, List errors) {
        if (kosong(values.get("kode"))) errors.add("kode:Kode Akun wajib diisi");
        if (kosong(values.get("nama"))) errors.add("nama:Nama Akun wajib diisi");
        if (kosong(values.get("debetCredit"))) {
            errors.add("debetCredit:Debet / Credit wajib dipilih");
        }
        if (kosong(values.get("grupAkun"))) errors.add("grupAkun:Grup Akun wajib dipilih");
    }

    private static boolean kosong(Object nilai) {
        return nilai == null || String.valueOf(nilai).trim().length() == 0;
    }

    /**
     * Tolak kode akun yang sudah dipakai baris lain.
     *
     * <p>Baris yang sedang disunting dikecualikan dari pemeriksaan — tanpa itu,
     * menyimpan ulang baris yang sama tanpa mengubah kodenya akan ditolak oleh
     * dirinya sendiri.</p>
     */
    public void beforeSave(Session session, Akun target, GenericCrudRequestContext context) throws Exception {
        String kode = target == null || target.getKode() == null ? "" : target.getKode().trim();
        if (kode.length() == 0) return;
        Criteria c = session.createCriteria(Akun.class)
                .add(Restrictions.eq("kode", kode))
                .setProjection(Projections.rowCount());
        if (target.getId() != null) c.add(Restrictions.ne("id", target.getId()));
        Number n = (Number) c.uniqueResult();
        if (n != null && n.intValue() > 0) {
            throw new GenericCrudException(400, "KODE_AKUN_GANDA",
                    "Kode Akun \"" + kode + "\" sudah dipakai akun lain.");
        }
    }

    /**
     * Bagan akun <b>tidak</b> mendapat hapus.
     *
     * <p>Model {@code Akun} tidak punya kolom {@code aktif}, sehingga tidak ada
     * penonaktifan lunak yang bisa dilakukan; yang tersisa hanyalah penghapusan
     * permanen. Akun yang pernah dipakai masih diacu setiap baris jurnal yang
     * menyebutnya — menghapusnya memutus jejak akuntansi yang sudah terjadi.</p>
     */
    public boolean canDelete(Akun target, GenericCrudRequestContext context, List reasons) {
        reasons.add("Kode Akun tidak dapat dihapus: modelnya tidak punya penanda aktif, "
                + "sedangkan akun yang pernah dipakai masih diacu baris jurnal.");
        return false;
    }

    public List getNaturalKeyProperties() {
        List result = new ArrayList();
        result.add("kode");
        return result;
    }

    /** Bagan akun berlaku sepanjang institusi; scope ditangani lapisan umum. */
    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void validateObjectScope(ais.database.model.GeneralValueObject object,
            GenericCrudRequestContext context) { }
}
