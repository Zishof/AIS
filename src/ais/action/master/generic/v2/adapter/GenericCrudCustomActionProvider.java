package ais.action.master.generic.v2.adapter;

import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;

/**
 * Kontrak penyedia aksi kustom (di luar create/read/update/delete/import baku) untuk satu entitas
 * pada framework CRUD generik {@code ais.action.master.generic.v2}. Implementasi mendaftarkan daftar
 * aksi yang tersedia (mis. tombol "Setujui", "Kirim ulang notifikasi") sesuai {@link
 * GenericCrudDefinition} dan konteks pengguna, lalu mengeksekusi aksi terpilih terhadap sekumpulan
 * baris terpilih.
 */
@SuppressWarnings("rawtypes")
public interface GenericCrudCustomActionProvider {
    /**
     * Menghasilkan daftar aksi kustom yang tersedia untuk entitas ini, biasanya dipakai untuk
     * merender tombol/menu aksi tambahan di layar CRUD generik.
     *
     * @param definition definisi CRUD generik entitas terkait
     * @param context    konteks permintaan (identitas pengguna, dsb.) yang menentukan aksi mana yang
     *                   relevan/diizinkan
     * @return daftar deskriptor aksi kustom (kunci, label, dsb.); boleh kosong bila tidak ada aksi
     * @throws Exception diteruskan apa adanya bila penyusunan daftar aksi gagal
     */
    List getActions(GenericCrudDefinition definition, GenericCrudRequestContext context) throws Exception;
    /**
     * Mengeksekusi satu aksi kustom terhadap baris-baris terpilih.
     *
     * @param actionKey   kunci aksi yang dipilih (sesuai salah satu hasil {@link #getActions})
     * @param selectedIds daftar id baris yang dipilih pengguna sebagai target aksi
     * @param parameters  parameter tambahan yang diisi pengguna untuk aksi ini (boleh kosong)
     * @param context     konteks permintaan yang sedang berjalan
     * @return hasil eksekusi aksi (status, pesan, data tambahan)
     * @throws Exception diteruskan apa adanya bila eksekusi aksi gagal
     */
    GenericCrudResult execute(String actionKey, List selectedIds, Map parameters, GenericCrudRequestContext context) throws Exception;
}
