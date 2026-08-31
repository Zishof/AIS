package ais.action.master.generic.v2.adapter;

import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;

/**
 * Kontrak opsional bagi entitas framework CRUD generik ({@code generic/v2}) yang memerlukan
 * formulir isi/edit tidak-standar — mis. wizard multi-langkah, tab tersendiri per bagian data,
 * atau tampilan modal/drawer khusus — di luar formulir tabel-otomatis bawaan.
 *
 * <p>
 * Sebuah entitas mendaftarkan implementasi provider ini agar lapisan aksi generik dapat
 * bertanya {@link #getMode(GenericCrudRequestContext)} lebih dulu untuk menentukan mode
 * tampilan (lihat konstanta {@code MODE_*}), lalu memuat/menyimpan definisi & data formulir
 * lewat method lain di kontrak ini sesuai mode tersebut. Setiap tab formulir diidentifikasi
 * dengan {@code tabKey} bebas yang disepakati oleh implementasi masing-masing entitas.
 * </p>
 */
@SuppressWarnings("rawtypes")
public interface GenericCrudFormOverrideProvider {
    /** Formulir generik ditampilkan dalam panel geser (drawer) tanpa kustomisasi tab. */
    String MODE_GENERIC_DRAWER = "GENERIC_DRAWER";
    /** Formulir generik ditampilkan dalam jendela modal tanpa kustomisasi tab. */
    String MODE_GENERIC_MODAL = "GENERIC_MODAL";
    /** Formulir ditampilkan dalam drawer dengan beberapa tab kustom. */
    String MODE_TABBED_DRAWER = "TABBED_DRAWER";
    /** Formulir ditampilkan sebagai halaman penuh dengan navigasi tab. */
    String MODE_FULL_PAGE_TABS = "FULL_PAGE_TABS";
    /** Formulir ditampilkan sebagai alur wizard bertahap (langkah demi langkah). */
    String MODE_WIZARD = "WIZARD";
    /** Formulir sepenuhnya digantikan oleh komponen UI kustom milik entitas. */
    String MODE_CUSTOM_COMPONENT = "CUSTOM_COMPONENT";
    /** Formulir menjembatani ke layar/aksi lama (legacy) di luar kerangka generik. */
    String MODE_LEGACY_BRIDGE = "LEGACY_BRIDGE";

    /**
     * @param context konteks permintaan (entitas, user, parameter request) saat ini
     * @return salah satu konstanta {@code MODE_*} yang menentukan cara formulir dirender
     */
    String getMode(GenericCrudRequestContext context);

    /**
     * @param context    konteks permintaan saat ini
     * @param entity     entitas yang sedang diedit, atau {@code null}/instans kosong saat membuat baru
     * @param createMode {@code true} bila formulir dibuka untuk membuat data baru, {@code false} untuk edit
     * @return definisi formulir (susunan field/tab) yang akan dirender
     */
    GenericCrudFormDefinition getDefinition(GenericCrudRequestContext context, Object entity, boolean createMode) throws Exception;

    /**
     * @param tabKey  kunci tab yang diminta
     * @param context konteks permintaan saat ini
     * @param entity  entitas terkait
     * @return data awal (nilai field) untuk tab yang diminta
     */
    Map loadTab(String tabKey, GenericCrudRequestContext context, Object entity) throws Exception;

    /**
     * @param tabKey  kunci tab yang divalidasi
     * @param values  nilai field yang diisi user pada tab tersebut
     * @param context konteks permintaan saat ini
     * @param entity  entitas terkait
     * @return peta pesan galat validasi per field; kosong bila semua nilai valid
     */
    Map validateTab(String tabKey, Map values, GenericCrudRequestContext context, Object entity) throws Exception;

    /**
     * Menyimpan nilai satu tab formulir.
     *
     * @param tabKey          kunci tab yang disimpan
     * @param values          nilai field yang akan disimpan
     * @param context         konteks permintaan saat ini
     * @param entity          entitas terkait
     * @param optimisticToken token versi untuk deteksi konflik simpan-bersamaan (optimistic locking)
     * @return hasil operasi simpan (status, pesan, entitas terbaru)
     */
    GenericCrudResult saveTab(String tabKey, Map values, GenericCrudRequestContext context, Object entity, Object optimisticToken) throws Exception;

    /**
     * @param context konteks permintaan saat ini
     * @param entity  entitas yang sedang ditampilkan pada formulir
     * @return daftar aksi tambahan (tombol khusus) yang tersedia pada formulir entitas ini
     */
    List getFormActions(GenericCrudRequestContext context, Object entity) throws Exception;
}
