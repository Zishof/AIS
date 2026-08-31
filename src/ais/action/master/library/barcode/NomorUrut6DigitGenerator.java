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
 * Pembangkit barcode item perpustakaan dengan format nomor urut 6 digit polos (tanpa awalan
 * tahun/kode), mis. {@code "000123"}. Nomor berikutnya dihitung dari barcode numerik 6-digit
 * terbesar yang sudah tersimpan (dicari lewat pola SQL regex {@code char_length(barcode)=6 and
 * barcode ~ '^[0-9\.]+$'}), ditambah 1 dan ditambah jumlah barcode yang sudah dipesan dalam
 * proses pembangkitan batch berjalan. Bila nomor hasil bentrok dengan barcode yang sudah ada,
 * method memanggil dirinya sendiri secara rekursif dengan nomor tersebut ditambahkan ke daftar
 * pengecualian.
 */
public class NomorUrut6DigitGenerator implements BarcodeGenerator {

	/** Membangkitkan barcode baru untuk {@code batchItemPunyaBarcode} tanpa daftar pengecualian awal. */
	@Override
	public String generateBarcode(BatchItemPunyaBarcode batchItemPunyaBarcode) {
		return generateBarcode(new ArrayList<String>(), batchItemPunyaBarcode);
	}

	/**
	 * Membangkitkan barcode baru berupa nomor urut 6 digit, menghindari nomor pada
	 * {@code barcodePengecualian} maupun yang sudah tersimpan di database; mengulang secara
	 * rekursif bila terjadi bentrok.
	 *
	 * @param barcodePengecualian daftar barcode yang harus dihindari, diperbarui di tempat saat
	 *                             terjadi bentrok
	 * @param batchItemPunyaBarcode batch item yang akan diberi barcode (tidak dipakai langsung
	 *                             dalam pembentukan nomor pada varian ini, hanya diteruskan)
	 * @return barcode baru berupa string 6 digit yang belum dipakai
	 */
	@Override
	public String generateBarcode(List<String> barcodePengecualian, BatchItemPunyaBarcode batchItemPunyaBarcode) {

		String s = "'^[0-9\\.]+$'";

		int penambahan = barcodePengecualian.size();
		Session session = HibernateUtil.currentNativeSession();
		String barcodeMax = ((String) session.createCriteria(ItemPunyaBarcode.class)
				.setProjection(Projections.max("barcode"))
				.add(Restrictions.sqlRestriction("char_length(barcode)=6 and barcode ~ " + s)).uniqueResult());
		long count = 0L;
		try {
			count = Long.parseLong(barcodeMax.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/barcode/NomorUrut6DigitGenerator.java:34");

		}
		String c = "0000000000000" + ((count + 1) + penambahan);

		String finalCode = (c.substring(c.length() - 6, c.length()));

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
