package ais.action.master.generic.v2.adapter;import java.util.ArrayList;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.GenericCrudFieldDefinition;import ais.database.model.kursus.KomponenDataProdukKursus;@SuppressWarnings("rawtypes")/**
 * Adapter framework generic-CRUD-v2 (paket {@code ais.action.master.generic.v2}) untuk entitas
 * {@link KomponenDataProdukKursus} (komponen produk kursus: video, buku, ebook, latihan soal, ujian,
 * tatap muka, jarak jauh, ekstrakurikuler). Berbeda dari adapter CRUD generik biasa, kelas ini
 * mengunci definisi menjadi <b>read-only</b> — {@code configure} secara paksa menonaktifkan
 * create/update/delete/import serta menandai seluruh field sebagai tidak dapat dibuat/diubah lewat
 * layar CRUD generik, karena data komponen kursus dikelola lewat alur/aksi khusus modul kursus
 * (native workflow), bukan lewat form CRUD generik. Diverifikasi oleh
 * {@link ais.action.master.generic.v2.test.CourseComponentWorkflowSelfTest}.
 */
/**
 * Tipe khusus untuk course component workflow generic crud adapter. Kelas ini memberi nama dan
 * batas tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericCrudAutoEntityAdapter}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi
 * ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang
 * atau tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code configure()}, {@code
 * getNaturalKeyProperties}(). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
 *
 * @see GenericCrudAutoEntityAdapter
 */
/**
 * Tipe khusus untuk course component workflow generic crud adapter. Kelas ini memberi nama dan
 * batas tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericCrudAutoEntityAdapter}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi
 * ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang
 * atau tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code configure()}, {@code
 * getNaturalKeyProperties}(). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
 *
 * @see GenericCrudAutoEntityAdapter
 */
/**
 * Tipe khusus untuk course component workflow generic crud adapter. Kelas ini memberi nama dan
 * batas tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericCrudAutoEntityAdapter}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi
 * ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang
 * atau tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code configure()}, {@code
 * getNaturalKeyProperties}(). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
 *
 * @see GenericCrudAutoEntityAdapter
 */
/**
 * Tipe khusus untuk course component workflow generic crud adapter. Kelas ini memberi nama dan
 * batas tanggung jawab yang eksplisit pada perilaku yang diwarisi atau kontrak yang
 * diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericCrudAutoEntityAdapter}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi
 * ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang
 * atau tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getNaturalKeyProperties()}); operasi
 * domain lain ({@code configure()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see GenericCrudAutoEntityAdapter
 */
public final class CourseComponentWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter{public CourseComponentWorkflowGenericCrudAdapter(){super(KomponenDataProdukKursus.class,false,null,true);}
	/** Mengunci definisi CRUD generik entitas ini menjadi read-only: menonaktifkan create/update/delete/import, mengeset urutan tampil default berdasarkan {@code kode}, dan menandai semua field tidak dapat dibuat/diubah lewat layar generik. */
	public void configure(GenericCrudDefinition d){d.setDisplayName("Komponen Produk Kursus");d.setLifecycleStatus(GenericCrudDefinition.READ_ONLY);d.setCreateEnabled(false);d.setUpdateEnabled(false);d.setDeleteEnabled(false);d.setImportEnabled(false);d.setDefaultSortProperty("kode");d.setDefaultSortAscending(true);for(Object o:d.getFields()){GenericCrudFieldDefinition f=(GenericCrudFieldDefinition)o;f.setCreateable(false);f.setUpdateable(false);}}
	/** Mengembalikan pasangan kolom kunci alami entitas ini: {@code komponenProdukKursus} (induk produk kursus) dan {@code kode} (kode komponen). */
	public List getNaturalKeyProperties(){List x=new ArrayList();x.add("komponenProdukKursus");x.add("kode");return x;}}
