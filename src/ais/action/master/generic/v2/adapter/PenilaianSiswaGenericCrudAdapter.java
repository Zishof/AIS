package ais.action.master.generic.v2.adapter;

import org.hibernate.Session;

import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.GeneralValueObject;
import ais.database.model.sekolah.KelasSiswa;

/**
 * Override native untuk daftar kelas pada PenilaianSiswaAction.
 * Source Action menyimpan KelasSiswa (bukan kandidat scanner pertama
 * KelasSiswaPunyaSiswa) dan mewajibkan nama, yayasan, serta sekolah.
 */
@SuppressWarnings("rawtypes")
public final class PenilaianSiswaGenericCrudAdapter extends GenericCrudAutoEntityAdapter {
    public PenilaianSiswaGenericCrudAdapter() {
        super(KelasSiswa.class, true, null);
    }

    public void beforeSave(Session session, GeneralValueObject value,
            GenericCrudRequestContext context) throws Exception {
        validateObjectScope(value, context);
        KelasSiswa target = (KelasSiswa) value;
        if (target.getNama() == null || target.getNama().trim().length() == 0) {
            throw new GenericCrudException(400, "NAMA_KELAS_REQUIRED", "Nama Kelas wajib diisi.");
        }
        if (target.getYayasan() == null) {
            throw new GenericCrudException(400, "YAYASAN_REQUIRED", "Pilih Yayasan terlebih dahulu.");
        }
        if (target.getSekolah() == null) {
            throw new GenericCrudException(400, "SEKOLAH_REQUIRED", "Pilih Sekolah terlebih dahulu.");
        }
        if (target.getTahunAjaran() == null || target.getTahunAjaran().trim().length() == 0) {
            throw new GenericCrudException(400, "TAHUN_AJARAN_REQUIRED", "Tahun Ajaran wajib dipilih.");
        }
    }
}
