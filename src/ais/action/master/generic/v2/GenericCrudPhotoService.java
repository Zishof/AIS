package ais.action.master.generic.v2;

import java.io.InputStream;
import java.io.Serializable;
import ais.action.master.generic.v2.adapter.GenericCrudPhotoAdapter;

public class GenericCrudPhotoService {
    public String upload(GenericCrudRequestContext context, Serializable id, InputStream input,
            String fileName, String contentType, long length, GenericCrudPhotoAdapter adapter) throws Exception {
        new GenericCrudPrivilegeGuard().require(context, GenericCrudOperation.UPDATE);
        if (adapter == null) throw new GenericCrudException(403, "PHOTO_DISABLED", "Photo adapter belum dikonfigurasi.");
        if (length < 1 || length > 5L * 1024L * 1024L) throw new GenericCrudException(400, "PHOTO_SIZE_INVALID", "Ukuran foto maksimal 5 MB.");
        if (!("image/jpeg".equals(contentType) || "image/png".equals(contentType) || "image/webp".equals(contentType))) {
            throw new GenericCrudException(400, "PHOTO_TYPE_INVALID", "Format foto tidak diizinkan.");
        }
        adapter.validate(fileName, contentType, length, context);
        return adapter.store(id, input, fileName, contentType, context);
    }
}
