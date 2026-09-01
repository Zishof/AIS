package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;

import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.BankSoal;

/**
 * Adapter CRUD untuk Bank Soal.
 *
 * <p>Menu ini sebelumnya READ_ONLY karena pendaftaran otomatis tidak dapat
 * mengikat {@code BankSoalAction} pada entity {@code BankSoal}. Daftarnya tampil,
 * tetapi soal tidak dapat ditambah maupun diubah.</p>
 *
 * <p><b>Dua aturan simpan milik layar lama dipindahkan apa adanya:</b> isi soal
 * wajib terisi, dan jenis koreksi wajib dipilih. Aturan kedua penting: jenis
 * koreksi menentukan bagaimana jawaban dinilai, dan soal tanpa nilai itu akan
 * ikut terpakai pada ujian lalu gagal dinilai — kegagalan yang muncul jauh
 * setelah soalnya disimpan.</p>
 *
 * <p>Kolom lain (mata pelajaran, fakultas, jurusan, skor, lampiran, jenis
 * pilihan ganda) memang boleh kosong pada layar lama, jadi tidak dijadikan
 * wajib di sini. Menambah kewajiban yang tidak ada pada layar lama akan
 * memblokir pemakaian yang selama ini sah.</p>
 *
 * @see AbstractGenericCrudEntityAdapter
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class BankSoalGenericCrudAdapter extends AbstractGenericCrudEntityAdapter<BankSoal>
        implements GenericCrudScopeAdapter {

    public BankSoal createNew(GenericCrudRequestContext context) { return new BankSoal(); }

    public void validateCreate(Map values, GenericCrudRequestContext context, List errors) {
        validate(values, errors);
    }

    public void validateUpdate(BankSoal current, Map values, GenericCrudRequestContext context, List errors) {
        validate(values, errors);
    }

    private void validate(Map values, List errors) {
        if (kosong(values.get("soal"))) errors.add("soal:Isi soal wajib diisi");
        if (kosong(values.get("jenisKoreksi"))) {
            errors.add("jenisKoreksi:Jenis koreksi wajib dipilih");
        }
    }

    private static boolean kosong(Object nilai) {
        return nilai == null || String.valueOf(nilai).trim().length() == 0;
    }

    /**
     * Bank Soal <b>tidak</b> mendapat hapus.
     *
     * <p>Model {@code BankSoal} tidak punya kolom {@code aktif}, sehingga tidak
     * ada penonaktifan lunak yang bisa dilakukan. Yang tersisa hanyalah
     * penghapusan permanen — dan soal yang pernah dipakai masih diacu jawaban
     * serta nilai peserta, sehingga menghapus barisnya akan memutus hasil ujian
     * yang sudah terjadi.</p>
     *
     * <p>Alasan penolakannya disebutkan agar klien dapat menampilkannya, bukan
     * sekadar mematikan tombol tanpa keterangan. Perilaku {@code delete()}
     * bawaan induk (menolak dengan {@code DELETE_DISABLED}) sengaja tidak
     * ditimpa.</p>
     */
    public boolean canDelete(BankSoal target, GenericCrudRequestContext context, List reasons) {
        reasons.add("Bank Soal tidak dapat dihapus: modelnya tidak punya penanda aktif, "
                + "sedangkan soal yang pernah dipakai masih diacu jawaban dan nilai peserta.");
        return false;
    }

    public List getNaturalKeyProperties() { return new ArrayList(); }

    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void validateObjectScope(ais.database.model.GeneralValueObject object,
            GenericCrudRequestContext context) { }
}
