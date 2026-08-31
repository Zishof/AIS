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
 * Algoritma penomoran barcode koleksi perpustakaan (varian "YTB"), berpola
 * {@code <kode tipe item><kode perpustakaan>.<nomor urut 5 digit>.<nomor eksemplar>}: kode tipe
 * item + kode perpustakaan diikuti nomor urut (dihitung dari nilai maksimum segmen posisi 4-8 pada
 * barcode {@link ItemPunyaBarcode} yang sudah ada di perpustakaan tersebut, via SQL native yang
 * memvalidasi segmen tersebut berupa angka) dan nomor eksemplar ke- (jumlah
 * {@link ItemPunyaBarcode} yang sudah ada untuk kombinasi item+perpustakaan yang sama, ditambah 1).
 * Bila barcode hasil sudah terpakai, dicoba ulang secara rekursif dengan barcode tersebut
 * ditambahkan ke daftar pengecualian; percobaan dihentikan paksa (dikembalikan dengan prefiks
 * {@code "--"}) setelah lebih dari 10.050 percobaan gagal untuk mencegah rekursi tak berujung pada
 * proses batch yang sangat besar.
 */
public class YtbBarcodeGenerator implements BarcodeGenerator {

	/** Varian ringkas {@link #generateBarcode(List, BatchItemPunyaBarcode)} tanpa daftar pengecualian awal. */
	@Override
	public String generateBarcode(BatchItemPunyaBarcode batchItemPunyaBarcode) {
		return generateBarcode(new ArrayList<String>(), batchItemPunyaBarcode);
	}

	/**
	 * Menghasilkan barcode untuk {@code batchItemPunyaBarcode} sesuai pola kelas ini (lihat javadoc
	 * kelas). Rekursif: bila barcode yang dihasilkan ternyata sudah dipakai, dicoba lagi dengan
	 * barcode tersebut ditambahkan ke {@code barcodePengecualian} (juga dipakai sebagai penambah
	 * nomor urut agar percobaan berikutnya tidak mengulang nilai yang sama).
	 *
	 * @param barcodePengecualian daftar barcode yang harus dianggap sudah terpakai (dimutasi dan diteruskan pada percobaan ulang)
	 * @return barcode yang dihasilkan, atau nilai berprefiks {@code "--"} bila batas 10.050 percobaan terlampaui
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

		int count = ((Number) session.createSQLQuery(
				"select max(to_number(substr(barcode,4,5),'99999')) barcode from library.item_punya_barcode where substr(barcode,4,5) ~ '^[0-9\\.]+$' and perpustakaan="
						+ batchItemPunyaBarcode.getPerpustakaan().getId())
				.uniqueResult()).intValue();
		String c = "00000000" + ((count + 1) + penambahan);

		Integer jmlEks = ((Number) session.createCriteria(ItemPunyaBarcode.class)
				.add(Restrictions.eq("perpustakaan", batchItemPunyaBarcode.getPerpustakaan()))
				.add(Restrictions.eq("item", batchItemPunyaBarcode.getItem())).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();

		String depan = kodeJenisItem + kodePerpus + ".";
		String finalCode = depan + (c.substring(c.length() - 5, c.length())) + "." + (jmlEks + 1);

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
