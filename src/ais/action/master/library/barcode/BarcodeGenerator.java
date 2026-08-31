package ais.action.master.library.barcode;

import java.util.List;

import ais.database.model.library.BatchItemPunyaBarcode;

/**
 * Kontrak dasar seluruh algoritma pembuatan nomor barcode item perpustakaan yang khas per
 * institusi (lihat implementasi seperti {@link IAINBatusangkarBarcodeGenerator}). Setiap
 * implementasi menentukan sendiri format/urutan penomoran; kontrak ini hanya menjamin bahwa
 * pemanggil dapat meminta satu nomor barcode baru untuk sebuah {@link BatchItemPunyaBarcode},
 * baik tanpa maupun dengan daftar nomor yang harus dihindari (untuk menghindari tabrakan saat
 * membuat banyak barcode sekaligus dalam satu batch sebelum masing-masing tersimpan ke database).
 */
public interface BarcodeGenerator {

	/** Menghasilkan satu nomor barcode baru untuk {@code batchItemPunyaBarcode} sesuai algoritma penomoran institusi terkait. */
	public String generateBarcode(BatchItemPunyaBarcode batchItemPunyaBarcode);

	/** Seperti {@link #generateBarcode(BatchItemPunyaBarcode)}, tetapi menghindari nomor yang sudah ada di {@code barcodePengecualian} — dipakai saat membuat banyak barcode sekaligus dalam satu batch agar tidak saling bertabrakan sebelum tersimpan ke database. */
	public String generateBarcode(List<String> barcodePengecualian, BatchItemPunyaBarcode batchItemPunyaBarcode);

}
