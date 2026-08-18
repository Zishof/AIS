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

public class DefaultBarcodeGenerator implements BarcodeGenerator {

	@Override
	public String generateBarcode(BatchItemPunyaBarcode batchItemPunyaBarcode) {
		return generateBarcode(new ArrayList<String>(), batchItemPunyaBarcode);
	}

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
