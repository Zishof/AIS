package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;

import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.Kurikulum;

/**
 * Adapter CRUD untuk Kurikulum.
 *
 * <p>Menu Kurikulum sebelumnya berstatus READ_ONLY: pendaftaran otomatis tidak
 * dapat mengikat {@code KurikulumAction} karena Action itu tidak punya metode
 * init yang menerima entity {@code Kurikulum}. Akibatnya daftarnya tampil tetapi
 * tambah/ubah tidak tersedia sama sekali.</p>
 *
 * <p><b>Validasinya ditulis ulang dengan sadar, bukan dilewati.</b>
 * {@code KurikulumAction.onSave} membaca langsung dari widget ZK dan menolak
 * simpan pada lima keadaan. Mengaktifkan tambah/ubah tanpa memindahkan kelima
 * aturan itu akan menghasilkan baris kurikulum tanpa jurusan atau tanpa tahun
 * akademik — data yang salah, bukan galat, sehingga baru ketahuan jauh
 * kemudian.</p>
 *
 * <p>Kelima aturan itu, apa adanya dari layar lama:</p>
 * <ol>
 *   <li>tahun wajib diisi;</li>
 *   <li>jurusan wajib dipilih;</li>
 *   <li>tahun akademik wajib dipilih;</li>
 *   <li>jenis semester wajib dipilih;</li>
 *   <li>bila kurikulum OBE dicentang, tahun akademik OBE wajib dipilih.</li>
 * </ol>
 *
 * @see AbstractGenericCrudEntityAdapter
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class KurikulumGenericCrudAdapter extends AbstractGenericCrudEntityAdapter<Kurikulum>
        implements GenericCrudScopeAdapter {

    public Kurikulum createNew(GenericCrudRequestContext context) { return new Kurikulum(); }

    public void validateCreate(Map values, GenericCrudRequestContext context, List errors) {
        validate(values, errors);
    }

    public void validateUpdate(Kurikulum current, Map values, GenericCrudRequestContext context, List errors) {
        validate(values, errors);
    }

    /**
     * Kelima aturan simpan milik layar lama.
     *
     * <p>Pesannya sengaja menyebut kolomnya, bukan sekadar "data tidak lengkap":
     * formulir kurikulum punya belasan kolom, dan pesan umum memaksa operator
     * menebak yang mana.</p>
     */
    private void validate(Map values, List errors) {
        if (kosong(values.get("tahun"))) errors.add("tahun:Tahun wajib diisi");
        if (kosong(values.get("jurusan"))) errors.add("jurusan:Jurusan wajib dipilih");
        if (kosong(values.get("tahunAkademik"))) {
            errors.add("tahunAkademik:Tahun Akademik wajib dipilih");
        }
        if (kosong(values.get("jenisSemester"))) {
            errors.add("jenisSemester:Jenis semester wajib dipilih");
        }
        // Aturan bersyarat: hanya berlaku ketika OBE dinyalakan. Menjadikannya
        // wajib tanpa syarat akan memblokir kurikulum non-OBE yang sah.
        if (benar(values.get("obe")) && kosong(values.get("tahunAkademikObe"))) {
            errors.add("tahunAkademikObe:Berlaku Mulai Tahun Akademik OBE wajib diisi "
                    + "jika kurikulum OBE diaktifkan");
        }
    }

    private static boolean kosong(Object nilai) {
        return nilai == null || String.valueOf(nilai).trim().length() == 0;
    }

    /** Kotak centang dapat tiba sebagai boolean maupun sebagai teks "true"/"1". */
    private static boolean benar(Object nilai) {
        if (nilai instanceof Boolean) return ((Boolean) nilai).booleanValue();
        String v = nilai == null ? "" : String.valueOf(nilai).trim();
        return "true".equalsIgnoreCase(v) || "1".equals(v);
    }

    public boolean canDelete(Kurikulum target, GenericCrudRequestContext context, List reasons) {
        return true;
    }

    /**
     * Hapus adalah penonaktifan lunak, sama seperti seluruh master data lain.
     *
     * <p>Kurikulum diacu KRS, nilai, dan detail perkuliahan; menghapus barisnya
     * akan memutus riwayat akademik yang sudah terjadi. Menonaktifkan
     * menyembunyikannya dari pemakaian baru tanpa menghapus masa lalu.</p>
     */
    public void delete(Session session, Kurikulum target, GenericCrudRequestContext context) {
        target.setAktif(Boolean.FALSE);
        session.saveOrUpdate(target);
    }

    public List getNaturalKeyProperties() {
        List result = new ArrayList();
        result.add("nama");
        result.add("tahun");
        return result;
    }

    /** Kurikulum adalah master milik jurusan; scope institusi ditangani lapisan umum. */
    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void validateObjectScope(ais.database.model.GeneralValueObject object,
            GenericCrudRequestContext context) { }
}
