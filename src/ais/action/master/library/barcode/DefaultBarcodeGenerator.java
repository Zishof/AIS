package ais.action.master.library.barcode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.BatchItemPunyaBarcode;
import ais.database.model.library.ItemPunyaBarcode;

/**
 * Pembangkit barcode item perpustakaan baku (default), dipakai bila institusi tidak memerlukan
 * format khusus. Format nomor: {@code TAHUN-XXXXX-YYYY}, dengan {@code TAHUN} diambil dari
 * tanggal saldo awal/penerimaan pengadaan batch item, {@code XXXXX} adalah 5 digit terakhir id
 * batch, dan {@code YYYY} adalah 4 digit terakhir urutan barcode item yang sudah ada untuk
 * kombinasi item+perpustakaan yang sama (ditambah jumlah barcode yang sudah dipesan dalam proses
 * pembangkitan batch berjalan). Bila nomor hasil bentrok dengan barcode yang sudah ada,
 * method memanggil dirinya sendiri secara rekursif dengan nomor tersebut ditambahkan ke daftar
 * pengecualian.
 */
public class DefaultBarcodeGenerator implements BarcodeGenerator {

	/** Membangkitkan barcode baru untuk {@code batchItemPunyaBarcode} tanpa daftar pengecualian awal. */
	@Override
	public String generateBarcode(BatchItemPunyaBarcode batchItemPunyaBarcode) {
		return generateBarcode(new ArrayList<String>(), batchItemPunyaBarcode);
	}

	/**
	 * Membangkitkan barcode baru dengan format {@code TAHUN-XXXXX-YYYY}, menghindari nomor pada
	 * {@code barcodePengecualian} maupun yang sudah tersimpan di database; mengulang secara
	 * rekursif bila terjadi bentrok.
	 *
	 * @param barcodePengecualian daftar barcode yang harus dihindari, diperbarui di tempat saat
	 *                             terjadi bentrok
	 * @param batchItemPunyaBarcode batch item yang akan diberi barcode
	 * @return barcode baru yang belum dipakai
	 */
	@Override
	public String generateBarcode(List<String> barcodePengecualian, BatchItemPunyaBarcode batchItemPunyaBarcode) {
		String code = "0000000" + (batchItemPunyaBarcode.getId());
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		if (batchItemPunyaBarcode.getSaldoAwal() != null) {
			calendar.setTime(batchItemPunyaBarcode.getSaldoAwal().getTanggalPembuatan());
		} else if (batchItemPunyaBarcode.getPenerimaanPengadaanItem() != null) {
			calendar.setTime(batchItemPunyaBarcode.getPenerimaanPengadaanItem().getTanggalPembuatan());
		}
		code = calendar.get(Calendar.YEAR) + "-" + (code.substring(code.length() - 5, code.length()));

		int penambahan = barcodePengecualian.size();
		Session session = HibernateUtil.currentNativeSession();
		int count = ((Number) session.createCriteria(ItemPunyaBarcode.class)
				.add(Restrictions.eq("item", batchItemPunyaBarcode.getItem()))
				.add(Restrictions.eq("perpustakaan", batchItemPunyaBarcode.getPerpustakaan()))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		String c = "0000000" + ((count + 1) + penambahan);

		String finalCode = code + "-" + (c.substring(c.length() - 4, c.length()));

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
