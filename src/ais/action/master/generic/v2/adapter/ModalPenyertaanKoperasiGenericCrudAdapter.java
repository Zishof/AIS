package ais.action.master.generic.v2.adapter;

import java.util.Date;

import org.hibernate.Session;

import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.GeneralValueObject;
import ais.database.model.koperasi.ModalPenyertaanKoperasi;

/** Business-rule parity untuk ModalPenyertaanKoperasiAction.onSave. */
public final class ModalPenyertaanKoperasiGenericCrudAdapter extends GenericCrudAutoEntityAdapter {
    public ModalPenyertaanKoperasiGenericCrudAdapter() {
        super(ModalPenyertaanKoperasi.class, true, null, true);
    }

    public GeneralValueObject createNew(GenericCrudRequestContext context) throws Exception {
        ModalPenyertaanKoperasi value = (ModalPenyertaanKoperasi) super.createNew(context);
        value.setJenisPenyerta(ModalPenyertaanKoperasi.JENIS_ANGGOTA);
        value.setStatus(ModalPenyertaanKoperasi.STATUS_AKTIF);
        value.setTanggalMasuk(new Date()); value.setAktif(Boolean.TRUE);
        return value;
    }

    public void beforeSave(Session session, GeneralValueObject target,
            GenericCrudRequestContext context) throws Exception {
        ModalPenyertaanKoperasi value = (ModalPenyertaanKoperasi) target;
        if (value.getNamaPenyerta() == null || value.getNamaPenyerta().trim().length() == 0) {
            throw new GenericCrudException(400, "INVESTOR_NAME_REQUIRED", "Nama penyerta wajib diisi.");
        }
        if (value.getNominal() == null || value.getNominal().doubleValue() <= 0D) {
            throw new GenericCrudException(400, "INVALID_CAPITAL_VALUE",
                    "Nominal penyertaan harus lebih besar dari nol.");
        }
        if (value.getJenisPenyerta() == null || value.getJenisPenyerta().trim().length() == 0) {
            value.setJenisPenyerta(ModalPenyertaanKoperasi.JENIS_ANGGOTA);
        }
        if (value.getStatus() == null || value.getStatus().trim().length() == 0) {
            value.setStatus(ModalPenyertaanKoperasi.STATUS_AKTIF);
        }
        if (value.getTanggalMasuk() == null) value.setTanggalMasuk(new Date());
        super.beforeSave(session, target, context);
    }
}
