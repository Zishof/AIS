package ais.action.iso8583;

import org.jpos.iso.ISOField;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.jpos.iso.ISOUtil;

import ais.common.Common;

/**
 * Listener sisi server protokol ISO 8583 (protokol pertukaran pesan transaksi finansial
 * antar-bank, dipakai di sini untuk integrasi dengan Bank NTT via jPOS) yang menjawab pesan
 * masuk dari switching bank. Menangani dua jenis pesan: Network Management Request
 * ({@code NetManReq}, dijawab {@code NetManRes} sukses — semacam heartbeat/keepalive koneksi)
 * dan Inquiry Request ({@code InqReq}, dijawab {@code InqRes} sukses dengan nominal tagihan).
 * <p>
 * <b>Catatan implementasi</b>: pada cabang Inquiry, nominal ({@code field 4}) dan variabel
 * data privat tambahan ({@code LengthofAdditionalPrivateData}, {@code SwitcherReferenceNumber},
 * {@code LengthofBillName}, {@code BillName}) tampak berisi nilai contoh/placeholder tetap
 * (mis. {@code amount = "3600" + 12 digit nol}) dan variabel data privat yang dihitung tidak
 * pernah disisipkan ke pesan balasan {@code m} — mengindikasikan implementasi ini belum lengkap/
 * masih berupa kerangka pengembangan, bukan logika inquiry saldo/tagihan yang sesungguhnya.
 * Tidak ditemukan kredensial atau kunci rahasia tertanam pada kelas ini.
 * </p>
 */
public class BankNttServerService implements ISORequestListener {

	/**
	 * Menangani satu pesan ISO 8583 masuk: mengenali MTI (Message Type Indicator), menyusun pesan
	 * balasan yang sesuai (Network Management atau Inquiry), lalu mengirimkannya kembali lewat
	 * {@code source}. Semua kegagalan ditangkap dan dilaporkan lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}, bukan dilempar ulang.
	 *
	 * @return {@code true} bila pesan berhasil diproses dan dikirim balik, {@code false} bila terjadi kegagalan
	 */
	@SuppressWarnings("unused")
	@Override
	public boolean process(ISOSource source, ISOMsg m) {

		try {
			String mti = m.getMTI();

			String processingCode = m.getString(3);

			System.out.println("Receive message with MTI = " + mti + " and Processing Code = " + processingCode);

			if (mti.equals(ConstansValueIso8583.NetManReq)) {

				m = (ISOMsg) m.clone();
				m.setMTI(ConstansValueIso8583.NetManRes);
				m.set(new ISOField(39, ConstansValueIso8583.SUCCESS));

			} else if (mti.equals(ConstansValueIso8583.InqReq)) {

				m = (ISOMsg) m.clone();
				m.setMTI(ConstansValueIso8583.InqRes);
				m.set(new ISOField(39, ConstansValueIso8583.SUCCESS));

				String isiAmont = ISOUtil.padleft("0", 12, '0');

				String amount = "3600" + isiAmont;
				m.set(new ISOField(4, amount));
				
				String LengthofAdditionalPrivateData = ISOUtil.padleft("61", 3, '0');
				String SwitcherReferenceNumber = ISOUtil.padleft("0", 20, '0');
				String LengthofBillName = ISOUtil.padleft("0", 2, '0');
				String BillName= ISOUtil.padleft("0", 2, '0');
			}

			source.send(m);

			return true;

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}

		return false;
	}

}
