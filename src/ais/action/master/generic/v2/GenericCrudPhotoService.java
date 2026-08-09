package ais.action.master.generic.v2;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.Serializable;
import ais.action.master.generic.v2.adapter.GenericCrudPhotoAdapter;

public class GenericCrudPhotoService {
    public String upload(GenericCrudRequestContext context, Serializable id, InputStream input,
            String fileName, String contentType, long length, GenericCrudPhotoAdapter adapter) throws Exception {
        new GenericCrudPrivilegeGuard().require(context, GenericCrudOperation.UPDATE);
        if (adapter == null) throw new GenericCrudException(403, "PHOTO_DISABLED", "Photo adapter belum dikonfigurasi.");
        if (!context.getDefinition().isPhotoEnabled()) throw new GenericCrudException(403, "PHOTO_DISABLED", "Foto belum diaktifkan untuk entity ini.");
        if (length < 1 || length > 5L * 1024L * 1024L) throw new GenericCrudException(400, "PHOTO_SIZE_INVALID", "Ukuran foto maksimal 5 MB.");
        if (!("image/jpeg".equals(contentType) || "image/png".equals(contentType) || "image/webp".equals(contentType))) {
            throw new GenericCrudException(400, "PHOTO_TYPE_INVALID", "Format foto tidak diizinkan.");
        }
        BufferedInputStream checked = input instanceof BufferedInputStream ? (BufferedInputStream) input : new BufferedInputStream(input);
        checked.mark(16); byte[] magic = new byte[12]; int read = checked.read(magic); checked.reset();
        boolean jpeg = read >= 3 && (magic[0] & 255) == 0xFF && (magic[1] & 255) == 0xD8 && (magic[2] & 255) == 0xFF;
        boolean png = read >= 8 && (magic[0] & 255) == 0x89 && magic[1] == 'P' && magic[2] == 'N' && magic[3] == 'G';
        boolean webp = read >= 12 && magic[0] == 'R' && magic[1] == 'I' && magic[2] == 'F' && magic[3] == 'F' && magic[8] == 'W' && magic[9] == 'E' && magic[10] == 'B' && magic[11] == 'P';
        if (!(jpeg || png || webp)) throw new GenericCrudException(400, "PHOTO_SIGNATURE_INVALID", "Isi file bukan image yang valid.");
        String safeName = fileName == null ? "photo" : fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        adapter.validate(safeName, contentType, length, context);
        return adapter.store(id, checked, safeName, contentType, context);
    }

    public void remove(GenericCrudRequestContext context, Serializable id, String reason,
            GenericCrudPhotoAdapter adapter) throws Exception {
        new GenericCrudPrivilegeGuard().require(context, GenericCrudOperation.UPDATE);
        if (adapter == null || !context.getDefinition().isPhotoEnabled())
            throw new GenericCrudException(403, "PHOTO_DISABLED", "Foto belum diaktifkan untuk entity ini.");
        adapter.remove(id, reason, context);
    }
}

