/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ais.action.ws;

import ais.action.ws.model.Response;
import ais.database.model.LogHostToHost;

/**
 * 
 * @author fauzi
 */
public class PembayaranBNIAction {

	private PembayaranAction action = new PembayaranAction();

	/**
	 * Sample method
	 */
	public String hello(String name) {
		return "Hello " + name;
	}

	public Response reversal(String language, String trxDateTime,
			String originalTrxDateTime, String transmissionDateTime,
			String originalTransmissionDateTime, String companyCode,
			String channelID, String billKey, String nominalPayment,
			String trxID, String currency, String financialReffNum) {
		String nama = ("=============================== BNI REVERSAL --> language="
				+ language
				+ ",trxDateTime="
				+ trxDateTime
				+ ",originalTrxDateTime="
				+ originalTrxDateTime
				+ ",transmissionDateTime="
				+ transmissionDateTime
				+ ",originalTransmissionDateTime="
				+ originalTransmissionDateTime
				+ ",companyCode="
				+ companyCode
				+ ",channelID="
				+ channelID
				+ ", billKey="
				+ billKey
				+ ",nominalPayment="
				+ nominalPayment
				+ ",trxID="
				+ trxID
				+ ",currency=" + currency + ",financialReffNum=" + financialReffNum);
		LogHostToHost logHostToHost = new LogHostToHost();
		logHostToHost.setInfo1(language);
		logHostToHost.setInfo2(trxDateTime);
		logHostToHost.setInfo3(originalTrxDateTime);
		logHostToHost.setInfo4(transmissionDateTime);
		logHostToHost.setInfo5(originalTransmissionDateTime);
		logHostToHost.setInfo6(companyCode);
		logHostToHost.setInfo7(channelID);
		logHostToHost.setInfo8(billKey);
		logHostToHost.setInfo9(nominalPayment);
		logHostToHost.setInfo10(trxID);
		logHostToHost.setInfo11(currency);
		logHostToHost.setInfo12(financialReffNum);
		return action.reversal(billKey, nama, logHostToHost, nominalPayment);
	}

	public Response inquiry(String language, String trxDateTime,
			String transmissionDateTime, String companyCode, String channelID,
			String billKey) {
		String nama = ("=============================== BNI INQUIRY --> language="
				+ language
				+ ",trxDateTime="
				+ trxDateTime
				+ ",transmissionDateTime="
				+ transmissionDateTime
				+ ",companyCode="
				+ companyCode
				+ ",channelID="
				+ channelID
				+ ",billKey=" + billKey);
		LogHostToHost logHostToHost = new LogHostToHost();
		logHostToHost.setInfo1(language);
		logHostToHost.setInfo2(trxDateTime);
		logHostToHost.setInfo4(transmissionDateTime);
		logHostToHost.setInfo6(companyCode);
		logHostToHost.setInfo7(channelID);
		logHostToHost.setInfo8(billKey);

		return action.inquery(billKey, nama, logHostToHost);
	}

	public Response pay(String language, String trxDateTime,
			String transmissionDateTime, String companyCode, String channelID,
			String billKey, String paymentDetail, String nominalPayment,
			String currency, String trxID, String financialReffNum) {
		String nama = ("=============================== BNI PAY --> language="
				+ language + ",trxDateTime=" + trxDateTime
				+ ",transmissionDateTime=" + transmissionDateTime
				+ ",companyCode=" + companyCode + ",channelID=" + channelID
				+ ", billKey=" + billKey + ",paymentDetail=" + paymentDetail
				+ ",nominalPayment=" + nominalPayment + ",trxID=" + trxID
				+ ",currency=" + currency + ",financialReffNum=" + financialReffNum);

		LogHostToHost logHostToHost = new LogHostToHost();
		logHostToHost.setInfo1(language);
		logHostToHost.setInfo2(trxDateTime);
		logHostToHost.setInfo4(transmissionDateTime);
		logHostToHost.setInfo6(companyCode);
		logHostToHost.setInfo7(channelID);
		logHostToHost.setInfo8(billKey);
		logHostToHost.setInfo9(nominalPayment);
		logHostToHost.setInfo10(trxID);
		logHostToHost.setInfo11(currency);
		logHostToHost.setInfo12(financialReffNum);
		logHostToHost.setInfo13(paymentDetail);

		return action.pay(billKey, trxID, trxDateTime, trxDateTime, channelID,
				companyCode, nominalPayment, nama, logHostToHost);
	}

}
