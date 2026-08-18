package ais.action.master.library.barcode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.BatchItemPunyaBarcode;
import ais.database.model.library.ItemPunyaBarcode;

public class TahunDanNomorUrutGenerator implements BarcodeGenerator {

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

		String prefix = Common.getKonfigurasi("prefix_barcode_perpustakaan", "").getNilai();

		String tahun = calendar.get(Calendar.YEAR) + "";
		tahun = prefix + tahun.substring(2);

		int penambahan = barcodePengecualian.size();
		Session session = HibernateUtil.currentNativeSession();
		int count = ((Number) session.createCriteria(ItemPunyaBarcode.class)
				.add(Restrictions.ilike("barcode", tahun + "-", MatchMode.START))
				.add(Restrictions.eq("perpustakaan", batchItemPunyaBarcode.getPerpustakaan()))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		String c = "00000000" + ((count + 1) + penambahan);

		String finalCode = tahun + "-" + (c.substring(c.length() - 5, c.length()));

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
