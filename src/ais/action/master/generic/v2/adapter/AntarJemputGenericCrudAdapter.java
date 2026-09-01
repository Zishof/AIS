package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;

import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.GeneralValueObject;

/**
 * Adapter CRUD untuk kelima panel master Sistem Antar Jemput.
 *
 * <p><b>Satu kelas untuk lima panel, bukan lima kelas kembar.</b> Kesembilan
 * panel antar jemput dilayani SATU Action ZK ({@code AntarJemputAction}) dengan
 * satu rutin simpan yang bercabang menurut jenis panel. Menyalin adapter lima
 * kali akan menirukan kembarannya di sisi native, padahal yang berbeda hanya
 * entitas dan satu aturan validasi.</p>
 *
 * <p><b>Validasinya dipindahkan apa adanya</b> dari rutin simpan itu:</p>
 * <ul>
 *   <li>panel selain Kartu Penjemput: {@code nama} wajib diisi;</li>
 *   <li>panel Kartu Penjemput: {@code namaPenjemput} dan {@code nomorKartu}
 *       wajib diisi — dan {@code nama} justru TIDAK wajib, karena layar lama
 *       memang mengecualikannya.</li>
 * </ul>
 *
 * <p>Pengecualian pada Kartu itu mudah terlewat kalau aturannya ditulis ulang
 * dari ingatan alih-alih dibaca: mewajibkan {@code nama} di sana akan memblokir
 * penyimpanan kartu yang selama ini sah.</p>
 *
 * @see AbstractGenericCrudEntityAdapter
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class AntarJemputGenericCrudAdapter
        extends AbstractGenericCrudEntityAdapter<GeneralValueObject>
        implements GenericCrudScopeAdapter {

    private final Class<? extends GeneralValueObject> entity;

    /** true hanya untuk panel Kartu Penjemput, yang aturannya berbeda. */
    private final boolean kartuPenjemput;

    public AntarJemputGenericCrudAdapter(Class<? extends GeneralValueObject> entity,
            boolean kartuPenjemput) {
        this.entity = entity;
        this.kartuPenjemput = kartuPenjemput;
    }

    public GeneralValueObject createNew(GenericCrudRequestContext context) {
        try {
            return entity.newInstance();
        } catch (Exception gagal) {
            throw new IllegalStateException(
                    "Entitas antar jemput tidak dapat dibuat: " + entity.getName(), gagal);
        }
    }

    public void validateCreate(Map values, GenericCrudRequestContext context, List errors) {
        validate(values, errors);
    }

    public void validateUpdate(GeneralValueObject current, Map values,
            GenericCrudRequestContext context, List errors) {
        validate(values, errors);
    }

    private void validate(Map values, List errors) {
        if (kartuPenjemput) {
            if (kosong(values.get("namaPenjemput"))) {
                errors.add("namaPenjemput:Nama penjemput wajib diisi");
            }
            if (kosong(values.get("nomorKartu"))) {
                errors.add("nomorKartu:Nomor kartu wajib diisi");
            }
            return;
        }
        if (kosong(values.get("nama"))) errors.add("nama:Nama wajib diisi");
    }

    private static boolean kosong(Object nilai) {
        return nilai == null || String.valueOf(nilai).trim().length() == 0;
    }

    public boolean canDelete(GeneralValueObject target, GenericCrudRequestContext context,
            List reasons) {
        return true;
    }

    /**
     * Hapus adalah penonaktifan lunak.
     *
     * <p>Kendaraan, rute, dan jadwal yang pernah dipakai masih diacu transaksi
     * penjemputan serta log notifikasinya. Menonaktifkan membuatnya tidak lagi
     * terpilih untuk jadwal baru tanpa memutus riwayat perjalanan yang sudah
     * terjadi.</p>
     */
    public void delete(Session session, GeneralValueObject target,
            GenericCrudRequestContext context) throws Exception {
        /*
         * setAktif TIDAK ada pada GeneralValueObject — ia dideklarasikan pada
         * tiap entitas konkret. Karena adapter ini sengaja melayani lima entitas
         * sekaligus, pemanggilannya lewat refleksi. Alternatifnya lima kelas
         * adapter kembar hanya demi satu baris yang sama.
         *
         * Bila entitasnya ternyata tidak punya setAktif, penghapusan DITOLAK,
         * bukan dilanjutkan diam-diam: menghapus permanen adalah hal terakhir
         * yang boleh terjadi karena sebuah metode tidak ditemukan.
         */
        java.lang.reflect.Method setter;
        try {
            setter = target.getClass().getMethod("setAktif", new Class[] { Boolean.class });
        } catch (NoSuchMethodException tidakAda) {
            throw new ais.action.master.generic.v2.GenericCrudException(403, "DELETE_DISABLED",
                    "Entitas ini tidak punya penanda aktif sehingga tidak dapat dinonaktifkan.",
                    tidakAda);
        }
        setter.invoke(target, new Object[] { Boolean.FALSE });
        session.saveOrUpdate(target);
    }

    public List getNaturalKeyProperties() {
        List result = new ArrayList();
        result.add("kode");
        return result;
    }

    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void validateObjectScope(GeneralValueObject object,
            GenericCrudRequestContext context) { }
}
