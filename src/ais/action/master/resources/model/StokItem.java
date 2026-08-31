package ais.action.master.resources.model;

import java.util.Date;



/**
 * DTO (data transfer object) ringkas ringkasan stok satu koleksi/{@code Item} perpustakaan,
 * dipakai untuk menyerialisasi data katalog/stok ke konsumen REST (mis. endpoint resources
 * publik) tanpa mengekspos entitas Hibernate penuh. Field bersifat publik dan tanpa
 * getter/setter (pola POJO data-holder sederhana khas paket {@code resources.model}).
 */
public class StokItem {
	/** Id item. */
	public Long id;
	/** Nama/judul item. */
	public String nama;
	/** Nomor ISBN. */
	public String isbn;
	/** Catatan/keterangan tambahan. */
	public String catatan;
	/** Nama perpustakaan pemilik stok. */
	public String perpustakaan;
	/** Nama pengarang. */
	public String pengarang;
	/** Jumlah stok tersedia. */
	public Double stok;
	/** Jumlah total eksemplar. */
	public Double jumlah;
	/** Tanggal terkait (mis. tanggal terbit/input). */
	public Date tanggal;
	/** Nomor ISSN (untuk terbitan berkala). */
	public String issn;
	/** Tipe item (mis. buku/jurnal). */
	public String tipe;
	/** Jenis item. */
	public String jenis;
	/** URL/path gambar sampul. */
	public String gambar;
	/** Jumlah komentar pada item. */
	public Integer komentar;
	/** Id perpustakaan pemilik stok. */
	public Long perpustakaanId;
}