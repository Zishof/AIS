package ais.action.master.generic.v2;

import java.util.Map;

/** Import wajib dry-run/job; pilot tetap disabled sampai mapping natural-key direview. */
@SuppressWarnings("rawtypes")
public class GenericCrudImportService {
    public GenericCrudResult preview(GenericCrudRequestContext context, Map request) throws GenericCrudException {
        if (!context.getDefinition().isImportEnabled()) {
            throw new GenericCrudException(403, "IMPORT_DISABLED", "Import belum diaktifkan untuk entity ini.");
        }
        return GenericCrudResult.error("IMPORT_JOB_REQUIRED", "Gunakan worker dry-run terkonfigurasi sebelum konfirmasi import.");
    }
}
