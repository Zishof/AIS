package ais.action.master.library.barcode;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.BatchItemPunyaBarcode;
import ais.database.model.library.ItemPunyaBarcode;

public class NomorUrut8DigitGenerator implements BarcodeGenerator {

	@Override
	public String generateBarcode(BatchItemPunyaBarcode batchItemPunyaBarcode) {
		return generateBarcode(new ArrayList<String>(), batchItemPunyaBarcode);
	}

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
