package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;

import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.AgamaAction;
import ais.action.master.generic.v2.GenericCrudException;
import ais.common.HeadlessBusinessRuleException;
import ais.database.model.Agama;

/**
 * Tipe khusus untuk agama generic crud adapter. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * AbstractGenericCrudEntityAdapter}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk
 * variasi ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak
 * bercabang atau tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code createNew()}, {@code validateCreate()},
 * {@code validateUpdate()}, {@code validate()}, {@code beforeSave()}, {@code canDelete()}, {@code delete()},
 * {@code getNaturalKeyProperties}(). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 *
 * @see AbstractGenericCrudEntityAdapter
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class AgamaGenericCrudAdapter extends AbstractGenericCrudEntityAdapter<Agama>
        implements GenericCrudScopeAdapter {

    public Agama createNew(GenericCrudRequestContext context) { return new Agama(); }

    public void validateCreate(Map values, GenericCrudRequestContext context, List errors) {
        validate(values, errors);
    }

    public void validateUpdate(Agama current, Map values, GenericCrudRequestContext context, List errors) {
        validate(values, errors);
    }

    private void validate(Map values, List errors) {
        Object nama = values.get("nama");
        if (nama == null || String.valueOf(nama).trim().length() == 0) { errors.add("nama:Nama wajib diisi"); }
    }

    public void beforeSave(Session session, Agama target, GenericCrudRequestContext context) throws Exception {
        try {
            new AgamaAction().executeHeadlessSave(target);
        } catch (HeadlessBusinessRuleException rejected) {
            throw new GenericCrudException(400, "EXISTING_ACTION_VALIDATION",
                    rejected.getMessage(), rejected);
        } catch (Exception failure) {
            if (failure instanceof GenericCrudException) throw failure;
            throw new GenericCrudException(500, "EXISTING_ACTION_FAILED",
                    "Lifecycle AgamaAction existing gagal dijalankan dan transaksi dibatalkan.", failure);
        }
    }

    public boolean canDelete(Agama target, GenericCrudRequestContext context, List reasons) { return true; }

    /** Delete biasa adalah soft deactivation. Permanent active-row delete memakai policy terpisah. */
    public void delete(Session session, Agama target, GenericCrudRequestContext context) {
        target.setAktif(Boolean.FALSE);
        session.saveOrUpdate(target);
    }

    public List getNaturalKeyProperties() {
        List result = new ArrayList();
        result.add("nama");
        return result;
    }

    /** Agama adalah tabel referensi global; adapter eksplisit ini mencegah scope implisit. */
    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void validateObjectScope(ais.database.model.GeneralValueObject object, GenericCrudRequestContext context) { }
}
