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
 * Algoritma penomoran barcode item perpustakaan berupa nomor urut 8 digit murni numerik.
 * Mengambil barcode tertinggi yang sudah ada (dibatasi pada barcode berpanjang tepat 8 karakter
 * dan hanya berisi digit/titik lewat filter SQL {@code char_length(barcode)=8 and barcode ~ '^[0-9\.]+$'}),
 * menambahkannya dengan 1 plus jumlah pengecualian yang sudah dicoba, lalu memformatnya menjadi
 * 8 digit dengan padding nol di depan.
 */
public class NomorUrut8DigitGenerator implements BarcodeGenerator {

	/** Menghasilkan barcode tanpa daftar pengecualian awal; lihat {@link #generateBarcode(List, BatchItemPunyaBarcode)}. */
	@Override
	public String generateBarcode(BatchItemPunyaBarcode batchItemPunyaBarcode) {
		return generateBarcode(new ArrayList<String>(), batchItemPunyaBarcode);
	}

	/**
	 * Menyusun barcode 8 digit dari nomor urut tertinggi yang sudah dipakai (barcode numerik murni
	 * berpanjang 8) ditambah 1 plus ukuran {@code barcodePengecualian}. Bila barcode hasil sudah
	 * terpakai, memanggil diri sendiri secara rekursif dengan barcode tersebut ditambahkan ke
	 * {@code barcodePengecualian}.
	 *
	 * @param barcodePengecualian      daftar barcode yang harus dilewati (bertambah saat rekursi)
	 * @param batchItemPunyaBarcode    konteks batch item (tidak dipakai langsung dalam perhitungan)
	 * @return barcode 8 digit yang belum terpakai
	 */
	@Override
	public String generateBarcode(List<String> barcodePengecualian, BatchItemPunyaBarcode batchItemPunyaBarcode) {

		String s = "'^[0-9\\.]+$'";

		int penambahan = barcodePengecualian.size();
		Session session = HibernateUtil.currentNativeSession();
		String barcodeMax = ((String) session.createCriteria(ItemPunyaBarcode.class)
				.setProjection(Projections.max("barcode"))
				.add(Restrictions.sqlRestriction("char_length(barcode)=8 and barcode ~ " + s)).uniqueResult());
		long count = 0L;
		try {
			count = Long.parseLong(barcodeMax.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/barcode/NomorUrut8DigitGenerator.java:34");

		}
		String c = "0000000000000" + ((count + 1) + penambahan);

		String finalCode = (c.substring(c.length() - 8, c.length()));

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
