package ais.action.iso8583;

import org.jpos.iso.ISOField;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.jpos.iso.ISOUtil;

import ais.common.Common;

public class BankNttServerService implements ISORequestListener {

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
