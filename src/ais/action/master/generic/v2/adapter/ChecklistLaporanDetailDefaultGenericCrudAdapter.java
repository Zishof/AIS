package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;

import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.rab.ChecklistLaporanDetailDefault;

/**
 * Adapter CRUD untuk Default Checklist Program / Kegiatan.
 *
 * <p>Layar ini sebelumnya READ_ONLY. Isinya master daftar periksa yang dipakai
 * sebagai bawaan laporan program/kegiatan — bukan angka anggaran, bukan
 * realisasi, dan tidak menyentuh uang. Itulah sebabnya ia satu-satunya layar di
 * cabang Anggaran yang dinaikkan.</p>
 *
 * <p><b>Hanya dua aturan, dan sengaja tidak ditambah.</b>
 * {@code ChecklistLaporanDetailDefaultAction.onSave} mewajibkan {@code kode} dan
 * {@code nama}. Pemeriksaan kode ganda <b>ada di layar lama tetapi
 * dikomentari</b>. Menghidupkannya di sini akan menolak penyimpanan yang selama
 * ini sah — perubahan aturan bisnis yang menyamar sebagai perbaikan. Kalau kode
 * ganda memang harus dilarang, itu keputusan tersendiri, bukan efek samping
 * konversi.</p>
 *
 * @see AbstractGenericCrudEntityAdapter
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ChecklistLaporanDetailDefaultGenericCrudAdapter
        extends AbstractGenericCrudEntityAdapter<ChecklistLaporanDetailDefault>
        implements GenericCrudScopeAdapter {

    public ChecklistLaporanDetailDefault createNew(GenericCrudRequestContext context) {
        return new ChecklistLaporanDetailDefault();
    }

    public void validateCreate(Map values, GenericCrudRequestContext context, List errors) {
        validate(values, errors);
    }

    public void validateUpdate(ChecklistLaporanDetailDefault current, Map values,
            GenericCrudRequestContext context, List errors) {
        validate(values, errors);
    }

    private void validate(Map values, List errors) {
        if (kosong(values.get("kode"))) errors.add("kode:Kode harus diisi");
        if (kosong(values.get("nama"))) errors.add("nama:Nama harus diisi");
    }

    private static boolean kosong(Object nilai) {
        return nilai == null || String.valueOf(nilai).trim().length() == 0;
    }

    /**
     * Baris baru dijadikan simpul akar yang utuh.
     *
     * <p>Entitas ini berbentuk pohon ({@code parent}, {@code deep}). Layar lama
     * menyusun kedalamannya saat menambah anak; formulir generik tidak punya
     * pohon untuk disorot, sehingga barisnya lahir sebagai akar. Menetapkan
     * {@code deep = 0} membuat akar itu utuh alih-alih berkedalaman kosong —
     * pembaca pohonnya kemudian tidak perlu menebak.</p>
     */
    public void beforeSave(Session session, ChecklistLaporanDetailDefault target,
            GenericCrudRequestContext context) throws Exception {
        if (target != null && target.getId() == null && target.getDeep() == null) {
            target.setDeep(Integer.valueOf(0));
        }
    }

    /**
     * Tidak ada hapus: modelnya tanpa kolom {@code aktif}.
     *
     * <p>Daftar periksa ini menjadi acuan laporan yang sudah tersusun;
     * menghapusnya permanen membuat laporan lama kehilangan butir yang dulu
     * dinilai.</p>
     */
    public boolean canDelete(ChecklistLaporanDetailDefault target,
            GenericCrudRequestContext context, List reasons) {
        reasons.add("Default Checklist tidak dapat dihapus: modelnya tidak punya penanda aktif, "
                + "sedangkan butirnya masih diacu laporan program/kegiatan yang sudah tersusun.");
        return false;
    }

    public List getNaturalKeyProperties() {
        List result = new ArrayList();
        result.add("kode");
        return result;
    }

    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void validateObjectScope(ais.database.model.GeneralValueObject object,
            GenericCrudRequestContext context) { }
}
