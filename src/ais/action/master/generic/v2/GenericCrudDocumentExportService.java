package ais.action.master.generic.v2;

/** PDF/DOCX/PPTX harus memakai adapter/template entity dan default disabled. */
public class GenericCrudDocumentExportService {
    public GenericCrudResult enqueue(GenericCrudRequestContext context, String format) throws GenericCrudException {
        throw new GenericCrudException(403, "DOCUMENT_EXPORT_DISABLED", "Export " + format + " belum dikonfigurasi untuk entity ini.");
    }
}
