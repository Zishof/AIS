package ais.action.master.generic.v2;

import java.util.Map;

/** Saved view DB memerlukan migration 001; tidak fallback global/insecure. */
@SuppressWarnings("rawtypes")
public class GenericCrudSavedViewService {
    public GenericCrudResult save(GenericCrudRequestContext context, String name, Map definition) throws GenericCrudException {
        if (name == null || name.trim().length() == 0) throw new GenericCrudException(400, "VIEW_NAME_REQUIRED", "Nama saved view wajib diisi.");
        throw new GenericCrudException(503, "MIGRATION_REQUIRED", "Jalankan migration Generic CRUD 001 sebelum saved view diaktifkan.");
    }
}
