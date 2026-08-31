package ais.action.master.library.barcode;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.BatchItemPunyaBarcode;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.TipeItem;

/**
 * Algoritma penomoran barcode item perpustakaan khusus institusi UMJ: barcode dibentuk dari
 * {@code kode jenis item + "." + kode perpustakaan + 5 digit nomor urut global} (nomor urut
 * diambil dari nilai maksimum barcode yang sudah ada di seluruh {@code library.item_punya_barcode}
 * lewat SQL native, bukan per-perpustakaan/per-jenis).
 */
public class UMJBarcodeGenerator implements BarcodeGenerator {

	/** Seperti {@link #generateBarcode(List, BatchItemPunyaBarcode)}, tanpa daftar pengecualian awal. */
	@Override
	public String generateBarcode(BatchItemPunyaBarcode batchItemPunyaBarcode) {
		return generateBarcode(new ArrayList<String>(), batchItemPunyaBarcode);
	}

	/**
	 * Membangkitkan barcode: nomor urut berikutnya diambil dari nilai maksimum barcode yang ada
	 * (5 digit terakhir setelah karakter ke-5) ditambah jumlah kandidat yang sudah terbukti
	 * bentrok di {@code barcodePengecualian}, digabung kode jenis item dan kode perpustakaan. Bila
	 * hasil ternyata sudah dipakai {@link ItemPunyaBarcode} lain, nomor tersebut ditambahkan ke
	 * {@code barcodePengecualian} dan method memanggil dirinya sendiri secara rekursif. Sebagai
	 * pengaman terhadap rekursi tak berkesudahan, bila jumlah percobaan melebihi 10050, method
	 * berhenti dan mengembalikan nilai bertanda {@code "--"} + barcode kandidat terakhir (bukan
	 * barcode valid) alih-alih terus mencoba.
	 *
	 * @param barcodePengecualian barcode kandidat yang sudah terbukti bentrok pada percobaan sebelumnya
	 * @return barcode yang belum dipakai item manapun, atau nilai bertanda {@code "--"} bila batas percobaan terlampaui
	 */
	@Override
	public String generateBarcode(List<String> barcodePengecualian, BatchItemPunyaBarcode batchItemPunyaBarcode) {

		if (barcodePengecualian.size() > 10050) {
			return "--" + barcodePengecualian.get(barcodePengecualian.size() - 1);
		}

		TipeItem tipeItem = batchItemPunyaBarcode.getItem().getTipeItem();
		String kodeJenisItem = tipeItem.getKode();

		int penambahan = barcodePengecualian.size();
		Session session = HibernateUtil.currentNativeSession();

		String kodePerpus = batchItemPunyaBarcode.getPerpustakaan().getKode();

		// Integer tahun = calendar.get(Calendar.YEAR);
		// String thn = "." + (tahun.toString().substring(0, 1)) +
		// (tahun.toString().substring(2));
		// delete from library.item_punya_barcode where substr(barcode,6)!=''
		// and to_number(substr(barcode,6),'9999') > 6224;
		int count = ((Number) session
				.createSQLQuery(
						"select max(to_number(substr(barcode,5),'99999')) from library.item_punya_barcode where substr(barcode,5)!=''")
				.uniqueResult()).intValue();
		String c = "00000000" + ((count + 1) + penambahan);

		String depan = kodeJenisItem + "." + kodePerpus;
		String finalCode = depan + (c.substring(c.length() - 5, c.length()));

		Integer jml = ((Number) session.createCriteria(ItemPunyaBarcode.class)
				.add(Restrictions.eq("barcode", finalCode)).setProjection(Projections.rowCount()).uniqueResult())
						.intValue();

		System.out.println("finalCode => " + finalCode + ", jml = " + jml);

		HibernateUtil.closeSession();
		if (jml > 0) {
			barcodePengecualian.add(finalCode);
			return generateBarcode(barcodePengecualian, batchItemPunyaBarcode);
		}

		return finalCode;
	}

}
