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

public class UMJBarcodeGenerator implements BarcodeGenerator {

	@Override
	public String generateBarcode(BatchItemPunyaBarcode batchItemPunyaBarcode) {
		return generateBarcode(new ArrayList<String>(), batchItemPunyaBarcode);
	}

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
