package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.database.model.kursus.KomponenDataProdukKursus;

/**
 * Adapter generic-CRUD-v2 untuk {@link KomponenDataProdukKursus}, yaitu komponen produk kursus
 * seperti video, buku, ebook, latihan soal, ujian, tatap muka, pembelajaran jarak jauh, dan
 * ekstrakurikuler.
 *
 * <p>Kelas ini sengaja mengunci definisi menjadi <b>read-only</b>. {@link #configure(GenericCrudDefinition)}
 * menonaktifkan create, update, delete, dan import serta menandai seluruh field tidak dapat dibuat atau
 * diubah dari layar CRUD generik. Data komponen tetap dikelola melalui workflow native modul kursus.
 * {@link #getNaturalKeyProperties()} mendeklarasikan pasangan kunci alami yang dipakai framework untuk
 * mengenali baris yang sama.</p>
 *
 * <p><b>Batas tanggung jawab:</b> lifecycle, validasi umum, dan akses data tetap dimiliki
 * {@link GenericCrudAutoEntityAdapter}. Perubahan yang berlaku bagi semua adapter harus ditempatkan pada
 * kelas induk atau service bersama; kelas ini hanya menampung konfigurasi dan kunci alami khusus komponen
 * kursus agar aturan CRUD tidak memiliki implementasi paralel.</p>
 *
 * <p><b>Efek samping:</b> {@code configure} mengubah objek definisi yang diterima, sedangkan
 * {@code getNaturalKeyProperties} hanya membentuk daftar nama properti baru. Kontrak ini diverifikasi oleh
 * {@link ais.action.master.generic.v2.test.CourseComponentWorkflowSelfTest}.</p>
 *
 * @see GenericCrudAutoEntityAdapter
 */
@SuppressWarnings("rawtypes")
public final class CourseComponentWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter{public CourseComponentWorkflowGenericCrudAdapter(){super(KomponenDataProdukKursus.class,false,null,true);}
	/** Mengunci definisi CRUD generik entitas ini menjadi read-only: menonaktifkan create/update/delete/import, mengeset urutan tampil default berdasarkan {@code kode}, dan menandai semua field tidak dapat dibuat/diubah lewat layar generik. */
	public void configure(GenericCrudDefinition d){d.setDisplayName("Komponen Produk Kursus");d.setLifecycleStatus(GenericCrudDefinition.READ_ONLY);d.setCreateEnabled(false);d.setUpdateEnabled(false);d.setDeleteEnabled(false);d.setImportEnabled(false);d.setDefaultSortProperty("kode");d.setDefaultSortAscending(true);for(Object o:d.getFields()){GenericCrudFieldDefinition f=(GenericCrudFieldDefinition)o;f.setCreateable(false);f.setUpdateable(false);}}
	/** Mengembalikan pasangan kolom kunci alami entitas ini: {@code komponenProdukKursus} (induk produk kursus) dan {@code kode} (kode komponen). */
	public List getNaturalKeyProperties(){List x=new ArrayList();x.add("komponenProdukKursus");x.add("kode");return x;}}
