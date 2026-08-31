package ais.action.master.library.barcode;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.BatchItemPunyaBarcode;
import ais.database.model.library.ItemPunyaBarcode;

/**
 * Implementasi {@link BarcodeGenerator} berbasis nomor urut numerik 7 digit murni untuk barcode item
 * perpustakaan. Nomor baru dihitung dari nilai barcode numerik-7-digit tertinggi yang sudah ada di
 * {@link ItemPunyaBarcode} ({@code MAX(barcode)} dengan filter regex panjang 7 digit angka) ditambah
 * 1, lalu diperiksa ulang keunikannya terhadap tabel yang sama; bila ternyata sudah terpakai (kondisi
 * balapan/race saat pembuatan massal), nomor tersebut ditambahkan ke daftar pengecualian dan method
 * memanggil dirinya sendiri secara rekursif untuk mencoba nomor berikutnya.
 */
public class NomorUrut7DigitGenerator implements BarcodeGenerator {

	/** Menghasilkan barcode tanpa daftar pengecualian; mendelegasikan ke varian dengan daftar kosong. */
	@Override
	public String generateBarcode(BatchItemPunyaBarcode batchItemPunyaBarcode) {
		return generateBarcode(new ArrayList<String>(), batchItemPunyaBarcode);
	}

	/**
	 * Menghasilkan barcode numerik 7 digit berikutnya, menghindari nilai dalam
	 * {@code barcodePengecualian} maupun yang sudah tersimpan di database. Rekursif: bila kandidat
	 * ternyata sudah dipakai, kandidat itu ditambahkan ke {@code barcodePengecualian} dan method
	 * dipanggil ulang untuk mencoba nilai berikutnya (offset bertambah sesuai ukuran daftar).
	 *
	 * @param barcodePengecualian daftar barcode yang harus dihindari, dimodifikasi di tempat saat rekursi
	 * @param batchItemPunyaBarcode konteks batch item (tidak dipakai untuk membentuk nomor)
	 * @return barcode numerik 7 digit yang belum terpakai
	 */
	@Override
	public String generateBarcode(List<String> barcodePengecualian, BatchItemPunyaBarcode batchItemPunyaBarcode) {

		String s = "'^[0-9\\.]+$'";

		int penambahan = barcodePengecualian.size();
		Session session = HibernateUtil.currentNativeSession();
		String barcodeMax = ((String) session.createCriteria(ItemPunyaBarcode.class)
				.setProjection(Projections.max("barcode"))
				.add(Restrictions.sqlRestriction("char_length(barcode)=7 and barcode ~ " + s)).uniqueResult());
		long count = 0L;
		try {
			count = Long.parseLong(barcodeMax.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/barcode/NomorUrut7DigitGenerator.java:34");

		}
		String c = "0000000000000" + ((count + 1) + penambahan);

		String finalCode = (c.substring(c.length() - 7, c.length()));

		Integer jml = ((Number) session.createCriteria(ItemPunyaBarcode.class)
				.add(Restrictions.eq("barcode", finalCode)).setProjection(Projections.rowCount()).uniqueResult())
						.intValue();

		HibernateUtil.closeSession();
		if (jml > 0) {
			barcodePengecualian.add(finalCode);
			return generateBarcode(barcodePengecualian, batchItemPunyaBarcode);
		}

		return finalCode;
	}

}
