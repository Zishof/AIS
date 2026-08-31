package ais.common.home;

/**
 * Layanan pembangun metadata SEO (Search Engine Optimization) untuk halaman portal beranda
 * (home portal) publik sebuah institusi di AIS. Mengubah data domain ({@link HomePortalViewModel})
 * dan konfigurasi konten yang bisa diatur admin ({@link HomePortalSectionResolver}) menjadi
 * struktur {@link HomePortalViewModel.Seo} siap pakai berisi judul, deskripsi, URL kanonik,
 * gambar Open Graph, dan markup data terstruktur JSON-LD (schema.org) untuk mesin pencari.
 *
 * <p>
 * Setiap nilai SEO mengikuti pola "override lalu fallback": bila admin institusi mengisi nilai
 * kustom lewat {@code config} (dengan kunci berawalan {@code prefix}, mis.
 * {@code home_v3_meta_title}), nilai tersebut dipakai; bila kosong/tidak diisi, method jatuh
 * kembali ke nilai default yang diturunkan dari data institusi itu sendiri (nama institusi,
 * deskripsi, logo/hero image). JSON-LD yang dihasilkan memilih tipe schema.org yang berbeda
 * bergantung jenis institusi ({@code MedicalOrganization} untuk fasilitas kesehatan,
 * {@code CollegeOrUniversity} untuk perguruan tinggi, {@code School} untuk selainnya), sehingga
 * hasil pencarian yang menampilkan halaman ini dapat memakai rich snippet yang sesuai.
 * </p>
 *
 * <p>
 * Kelas ini tidak menyimpan state (semua field lokal per pemanggilan) dan aman dipakai bersama
 * (thread-safe) selama {@link HomePortalViewModel} dan {@link HomePortalSectionResolver} yang
 * diberikan tidak dimutasi oleh thread lain secara bersamaan.
 * </p>
 */
public class HomePortalSeoService {
    /**
     * Varian ringkas {@link #build(HomePortalViewModel, HomePortalSectionResolver, String,
     * String)} yang memakai prefix konfigurasi default {@code "home_v3"} — cocok untuk halaman
     * beranda versi utama/terbaru portal.
     */
    public HomePortalViewModel.Seo build(HomePortalViewModel vm, HomePortalSectionResolver config, String requestUrl) {
        return build(vm, config, requestUrl, "home_v3");
    }

    /**
     * Implementasi kanonik: membangun struktur {@link HomePortalViewModel.Seo} lengkap (judul,
     * deskripsi, URL kanonik, gambar OG, dan JSON-LD schema.org) untuk halaman portal beranda.
     *
     * <p>
     * Judul, deskripsi, dan gambar OG diambil lebih dulu dari konfigurasi admin
     * ({@code config.value(prefix + "_meta_title"/"_meta_description"/"_og_image", "")}); bila
     * nilai tersebut kosong, jatuh ke fallback berbasis data institusi lewat
     * {@link #nonEmpty(String, String)}. Blok JSON-LD dibangun manual sebagai string JSON
     * (bukan lewat library serialisasi) dengan tiap nilai teks melalui {@link #escape(String)}
     * untuk mencegah JSON yang rusak/tidak valid akibat karakter kutip, backslash, atau baris
     * baru pada data institusi.
     * </p>
     *
     * @param vm         model data institusi/portal yang menjadi sumber nilai SEO
     * @param config     resolver konfigurasi per-section yang menyediakan override admin,
     *                   dikunci dengan awalan {@code prefix}
     * @param requestUrl URL permintaan saat ini, dipakai apa adanya sebagai URL kanonik
     *                    ({@code null} menghasilkan kanonik kosong)
     * @param prefix     awalan kunci konfigurasi (mis. {@code "home_v3"}) untuk membedakan
     *                   beberapa versi/varian halaman beranda yang berbagi kelas ini
     * @return objek {@link HomePortalViewModel.Seo} berisi seluruh metadata SEO siap dirender
     *         ke {@code <head>} halaman
     */
    public HomePortalViewModel.Seo build(HomePortalViewModel vm, HomePortalSectionResolver config,
            String requestUrl, String prefix) {
        HomePortalViewModel.Seo seo = new HomePortalViewModel.Seo();
        seo.title = nonEmpty(config.value(prefix + "_meta_title", ""), vm.institution.name + " - Website Resmi");
        seo.description = nonEmpty(config.value(prefix + "_meta_description", ""), vm.description);
        seo.canonical = requestUrl == null ? "" : requestUrl;
        seo.image = nonEmpty(config.value(prefix + "_og_image", ""), vm.institution.heroUrl);
        String type = vm.institution.healthcare ? "MedicalOrganization"
                : (vm.institution.college ? "CollegeOrUniversity" : "School");
        StringBuilder json = new StringBuilder();
        json.append("{\"@context\":\"https://schema.org\",\"@type\":\"").append(type).append("\"");
        add(json, "name", vm.institution.name);
        add(json, "url", seo.canonical);
        add(json, "logo", vm.institution.logoUrl);
        add(json, "description", seo.description);
        add(json, "telephone", vm.institution.phone);
        add(json, "email", vm.institution.email);
        if (vm.institution.address != null && vm.institution.address.length() > 0) {
            json.append(",\"address\":{\"@type\":\"PostalAddress\",\"streetAddress\":\"")
                .append(escape(vm.institution.address)).append("\"}");
        }
        json.append("}");
        seo.jsonLd = json.toString();
        return seo;
    }

    /**
     * Menambahkan satu pasangan kunci-nilai ke buffer JSON-LD yang sedang dibangun, hanya bila
     * {@code value} tidak kosong (tidak {@code null} dan bukan hanya whitespace). Nilai
     * di-escape lewat {@link #escape(String)} sebelum disisipkan agar JSON tetap valid.
     *
     * @param json  buffer JSON-LD yang sedang dibangun, dimodifikasi di tempat
     * @param key   nama field JSON
     * @param value nilai field; field dilewati sepenuhnya bila kosong/{@code null}
     */
    private void add(StringBuilder json, String key, String value) {
        if (value != null && value.trim().length() > 0) json.append(",\"").append(key).append("\":\"").append(escape(value)).append("\"");
    }

    /**
     * Mengembalikan {@code value} (setelah di-trim) bila tidak kosong, atau {@code fallback}
     * bila {@code value} {@code null}/kosong/hanya whitespace.
     *
     * @param value    nilai utama yang diprioritaskan
     * @param fallback nilai cadangan bila {@code value} kosong
     * @return {@code value} yang sudah di-trim, atau {@code fallback}
     */
    private String nonEmpty(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value.trim();
    }

    /**
     * Meng-escape karakter yang dapat merusak struktur string JSON ({@code \}, {@code "}) serta
     * menormalkan baris baru ({@code \r}/{@code \n} menjadi spasi) dan menetralkan karakter
     * {@code <} (diubah menjadi escape unicode {@code <}) untuk mencegah suntikan tag HTML
     * saat JSON-LD ini dirender apa adanya di dalam elemen {@code <script>} pada halaman HTML.
     *
     * @param value teks mentah yang akan disisipkan ke dalam nilai string JSON
     * @return teks yang sudah aman disisipkan sebagai nilai string JSON/HTML
     */
    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ").replace("<", "\\u003c");
    }
}
