package ais.action.master.generic.v2;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.Serializable;
import ais.action.master.generic.v2.adapter.GenericCrudPhotoAdapter;

/**
 * Layanan unggah/hapus foto entitas pada framework CRUD generik v2 (action-layer), dipakai oleh
 * entitas yang mengaktifkan {@code photoEnabled} pada definisinya. Validasi dilakukan berlapis
 * sebelum penyimpanan didelegasikan ke {@link GenericCrudPhotoAdapter} spesifik entitas: hak akses
 * ({@code UPDATE}), status aktif fitur foto, ukuran (maks. 5 MB), tipe konten yang diizinkan
 * (JPEG/PNG/WebP), serta pencocokan <i>magic number</i> byte awal berkas terhadap format yang
 * diklaim — mencegah berkas berbahaya menyamar sebagai gambar hanya lewat header
 * {@code Content-Type} yang dipalsukan.
 */
public class GenericCrudPhotoService {
    /**
     * Mengunggah foto untuk rekaman {@code id} setelah melewati validasi berlapis: hak akses
     * {@code UPDATE}, adapter foto tersedia dan fitur foto aktif untuk entity ini, ukuran file
     * 1 byte sampai 5 MB, {@code contentType} salah satu dari {@code image/jpeg}/{@code image/png}/
     * {@code image/webp}, dan tanda tangan biner (magic number) berkas benar-benar cocok dengan
     * salah satu format tersebut (dibaca dari 12 byte pertama lewat {@code mark}/{@code reset}
     * pada {@link BufferedInputStream}, sehingga stream tetap utuh untuk dibaca ulang oleh
     * adapter). Nama file disanitasi (karakter di luar alfanumerik/{@code . _ -} diganti
     * {@code _}) sebelum diteruskan ke {@link GenericCrudPhotoAdapter#validate}/{@code #store}.
     *
     * @param context     konteks permintaan (aktor, definisi entity) untuk pengecekan hak akses
     * @param id          id rekaman pemilik foto
     * @param input       stream isi berkas foto
     * @param fileName    nama file asli (disanitasi sebelum dipakai)
     * @param contentType tipe MIME yang diklaim klien
     * @param length      ukuran berkas dalam byte
     * @param adapter     adapter penyimpanan foto spesifik entity; {@code null} berarti fitur
     *                    belum dikonfigurasi
     * @return referensi/path hasil penyimpanan foto dari adapter
     * @throws GenericCrudException bila validasi apa pun gagal (403/400 sesuai jenis pelanggaran)
     */
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

    /**
     * Menghapus foto rekaman {@code id} setelah memastikan hak akses {@code UPDATE} dan fitur foto
     * aktif untuk entity ini; penghapusan sesungguhnya didelegasikan ke
     * {@link GenericCrudPhotoAdapter#remove}.
     *
     * @param context konteks permintaan untuk pengecekan hak akses
     * @param id      id rekaman pemilik foto
     * @param reason  alasan penghapusan (diteruskan ke adapter, mis. untuk audit)
     * @param adapter adapter penyimpanan foto spesifik entity
     * @throws GenericCrudException bila adapter tidak tersedia atau fitur foto tidak aktif
     */
    public void remove(GenericCrudRequestContext context, Serializable id, String reason,
            GenericCrudPhotoAdapter adapter) throws Exception {
        new GenericCrudPrivilegeGuard().require(context, GenericCrudOperation.UPDATE);
        if (adapter == null || !context.getDefinition().isPhotoEnabled())
            throw new GenericCrudException(403, "PHOTO_DISABLED", "Foto belum diaktifkan untuk entity ini.");
        adapter.remove(id, reason, context);
    }
}

