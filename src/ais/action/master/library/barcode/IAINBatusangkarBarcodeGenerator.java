package ais.action.master.library.barcode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.BatchItemPunyaBarcode;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.TipeItem;

public class IAINBatusangkarBarcodeGenerator implements BarcodeGenerator {

	@Override
	public String generateBarcode(BatchItemPunyaBarcode batchItemPunyaBarcode) {
		return generateBarcode(new ArrayList<String>(), batchItemPunyaBarcode);
	}

	@Override
	public String generateBarcode(List<String> barcodePengecualian, BatchItemPunyaBarcode batchItemPunyaBarcode) {

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		if (batchItemPunyaBarcode.getSaldoAwal() != null) {
			calendar.setTime(batchItemPunyaBarcode.getSaldoAwal().getTanggalPembuatan());
		} else if (batchItemPunyaBarcode.getPenerimaanPengadaanItem() != null) {
			calendar.setTime(batchItemPunyaBarcode.getPenerimaanPengadaanItem().getTanggalPembuatan());
		}

		TipeItem tipeItem = batchItemPunyaBarcode.getItem().getTipeItem();
		String kodeJenisItem = tipeItem.getKode();

		int penambahan = barcodePengecualian.size();
		Session session = HibernateUtil.currentNativeSession();

		Integer tahun = calendar.get(Calendar.YEAR);
		String thn = "." + (tahun.toString().substring(0, 1)) + (tahun.toString().substring(2));

		int count = ((Number) session.createCriteria(ItemPunyaBarcode.class)
				.add(Restrictions.ilike("barcode", kodeJenisItem + ".", MatchMode.ANYWHERE))
				.add(Restrictions.ilike("barcode", thn, MatchMode.ANYWHERE)).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		String c = "00000000" + ((count + 1) + penambahan);

		String depan = kodeJenisItem + thn;
		String finalCode = depan + (c.substring(c.length() - 5, c.length()));
		System.out.println("finalCode => " + finalCode);

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
