package ais.action.master.generic.v2;

import java.util.ArrayList;
import ais.action.master.generic.v2.GenericCrudSort;

/**
 * Layanan pencarian ringkas ("lookup") untuk kerangka kerja CRUD generik — dipakai kombobox/
 * lookup-picker pada form yang mengacu ke entitas lain. Mendelegasikan pengambilan data ke
 * {@link GenericCrudQueryService#list}, lalu memutuskan mode tampilan hasil: {@code "COMBO"} bila
 * jumlah total baris masih di bawah ambang batas {@link GenericCrudDefinition#getLookupThreshold()}
 * (cukup ditampilkan sebagai dropdown biasa), atau {@code "PAGED_LOOKUP"} bila melebihi ambang
 * (perlu tampilan pencarian dengan paginasi).
 */
public class GenericCrudLookupService {
    private final GenericCrudQueryService query = new GenericCrudQueryService();
    /**
     * Mengambil satu halaman hasil pencarian untuk lookup dan menentukan mode tampilannya.
     *
     * @param context  konteks permintaan (mendefinisikan entitas dan ambang batas lookup)
     * @param search   kata kunci pencarian bebas, boleh kosong
     * @param page     nomor halaman (1-based)
     * @param pageSize jumlah baris per halaman
     * @return hasil sukses berisi {@code mode} ("COMBO"/"PAGED_LOOKUP") dan {@code page} (data halaman)
     * @throws Exception diteruskan dari kegagalan query data
     */
    public GenericCrudResult lookup(GenericCrudRequestContext context, String search, int page, int pageSize) throws Exception {
        GenericCrudPage data = query.list(context, page, pageSize, search, new ArrayList(), null);
        java.util.Map result = new java.util.LinkedHashMap();
        result.put("mode", data.getTotal() <= context.getDefinition().getLookupThreshold() ? "COMBO" : "PAGED_LOOKUP");
        result.put("page", data);
        return GenericCrudResult.ok("Lookup berhasil.", result);
    }
}
