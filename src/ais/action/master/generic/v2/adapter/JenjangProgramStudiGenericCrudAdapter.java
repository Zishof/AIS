package ais.action.master.generic.v2.adapter;

import java.util.List;

import org.hibernate.Session;

import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.JenjangProgramStudi;

/**
 * Override native untuk JenjangProgramStudiAction.
 *
 * Action ZK lama membangun seluruh kontrol secara programatik, tetapi onSave
 * membuka/menutup transaksi sendiri. New UI mempertahankan aturan domainnya
 * (Jurusan dan Jenjang wajib, Jenjang boleh diturunkan dari Jurusan) sementara
 * commit/rollback tetap dimiliki GenericCrudMutationService.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class JenjangProgramStudiGenericCrudAdapter extends GenericCrudAutoEntityAdapter {
    public JenjangProgramStudiGenericCrudAdapter() {
        super(JenjangProgramStudi.class, false, null);
    }

    public void beforeSave(Session session, ais.database.model.GeneralValueObject value,
            GenericCrudRequestContext context) throws Exception {
        validateObjectScope(value, context);
        JenjangProgramStudi target = (JenjangProgramStudi) value;
        if (target.getJurusan() == null) {
            throw new GenericCrudException(400, "JURUSAN_REQUIRED", "Pilih salah satu Jurusan/Program Studi.");
        }
        if (target.getJenjang() == null) target.setJenjang(target.getJurusan().getJenjang());
        if (target.getJenjang() == null) {
            throw new GenericCrudException(400, "JENJANG_REQUIRED", "Pilih salah satu Jenjang.");
        }
    }

    public boolean canDelete(ais.database.model.GeneralValueObject value,
            GenericCrudRequestContext context, List reasons) {
        return true;
    }

    public void delete(Session session, ais.database.model.GeneralValueObject value,
            GenericCrudRequestContext context) throws Exception {
        validateObjectScope(value, context);
        session.delete(value);
    }
}
