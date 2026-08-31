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

/**
 * Algoritma penomoran barcode item perpustakaan khas institusi IAIN Batusangkar. Format barcode:
 * {@code <kodeJenisItem>.<digit1 tahun><2 digit terakhir tahun><5 digit urutan>}, mis. jenis item
 * "BK" tahun 2026 menjadi bentuk seperti {@code BK.226xxxxx} (potongan tahun diambil dari digit
 * pertama plus dua digit terakhir tahun, bukan tahun penuh — konvensi khas institusi ini). Tahun
 * yang dipakai diambil dari tanggal pembuatan saldo awal atau penerimaan pengadaan pada
 * {@link BatchItemPunyaBarcode}, bukan tanggal sistem saat ini. Urutan dihitung dari jumlah
 * {@link ItemPunyaBarcode} yang barcode-nya sudah cocok pola kode jenis + potongan tahun tersebut.
 */
public class IAINBatusangkarBarcodeGenerator implements BarcodeGenerator {

	/** Seperti {@link #generateBarcode(List, BatchItemPunyaBarcode)} tanpa daftar barcode yang harus dihindari. */
	@Override
	public String generateBarcode(BatchItemPunyaBarcode batchItemPunyaBarcode) {
		return generateBarcode(new ArrayList<String>(), batchItemPunyaBarcode);
	}

	/**
	 * Menghasilkan barcode baru untuk {@code batchItemPunyaBarcode} sesuai format khas IAIN
	 * Batusangkar (lihat javadoc kelas), menghindari nomor pada {@code barcodePengecualian}.
	 * Rekursif: bila barcode yang dihasilkan sudah terpakai, ditambahkan ke daftar pengecualian
	 * dan method memanggil dirinya sendiri untuk mencoba nomor berikutnya.
	 *
	 * @param barcodePengecualian     daftar barcode yang harus dihindari, dimutasi langsung saat
	 *                                terjadi bentrokan
	 * @param batchItemPunyaBarcode   item yang akan diberi barcode; menentukan jenis item dan
	 *                                tanggal acuan tahun
	 * @return barcode unik sesuai pola {@code kodeJenisItem + potonganTahun + urutan5digit}
	 */
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
