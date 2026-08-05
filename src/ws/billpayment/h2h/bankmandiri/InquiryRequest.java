/**
 * InquiryRequest.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ws.billpayment.h2h.bankmandiri;

public class InquiryRequest implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7778149594828082359L;

	private java.lang.String language;

	private java.lang.String trxDateTime;

	private java.lang.String transmissionDateTime;

	private java.lang.String companyCode;

	private java.lang.String channelID;

	private java.lang.String terminalID;

	private java.lang.String billKey1;

	private java.lang.String billKey2;

	private java.lang.String billKey3;

	private java.lang.String reference1;

	private java.lang.String reference2;

	private java.lang.String reference3;

	public String toString() {
		return "language = " + language + ", trxDateTime = " + trxDateTime
				+ ", transmissionDateTime = " + transmissionDateTime
				+ ", companyCode = " + companyCode + ", channelID = "
				+ channelID + ", terminalID = " + terminalID + ", billKey1 = "
				+ billKey1 + ", billKey2 = " + billKey2 + ", billKey3 = "
				+ billKey3 + ", reference1 = " + reference1 + ", reference2 = "
				+ reference2 + ", reference3 = " + reference3;
	}

	public InquiryRequest() {
	}

	public InquiryRequest(java.lang.String language,
			java.lang.String trxDateTime,
			java.lang.String transmissionDateTime,
			java.lang.String companyCode, java.lang.String channelID,
			java.lang.String terminalID, java.lang.String billKey1,
			java.lang.String billKey2, java.lang.String billKey3,
			java.lang.String reference1, java.lang.String reference2,
			java.lang.String reference3) {
		this.language = language;
		this.trxDateTime = trxDateTime;
		this.transmissionDateTime = transmissionDateTime;
		this.companyCode = companyCode;
		this.channelID = channelID;
		this.terminalID = terminalID;
		this.billKey1 = billKey1;
		this.billKey2 = billKey2;
		this.billKey3 = billKey3;
		this.reference1 = reference1;
		this.reference2 = reference2;
		this.reference3 = reference3;
	}

	/**
	 * Gets the language value for this InquiryRequest.
	 * 
	 * @return language
	 */
	public java.lang.String getLanguage() {
		return language;
	}

	/**
	 * Sets the language value for this InquiryRequest.
	 * 
	 * @param language
	 */
	public void setLanguage(java.lang.String language) {
		this.language = language;
	}

	/**
	 * Gets the trxDateTime value for this InquiryRequest.
	 * 
	 * @return trxDateTime
	 */
	public java.lang.String getTrxDateTime() {
		return trxDateTime;
	}

	/**
	 * Sets the trxDateTime value for this InquiryRequest.
	 * 
	 * @param trxDateTime
	 */
	public void setTrxDateTime(java.lang.String trxDateTime) {
		this.trxDateTime = trxDateTime;
	}

	/**
	 * Gets the transmissionDateTime value for this InquiryRequest.
	 * 
	 * @return transmissionDateTime
	 */
	public java.lang.String getTransmissionDateTime() {
		return transmissionDateTime;
	}

	/**
	 * Sets the transmissionDateTime value for this InquiryRequest.
	 * 
	 * @param transmissionDateTime
	 */
	public void setTransmissionDateTime(java.lang.String transmissionDateTime) {
		this.transmissionDateTime = transmissionDateTime;
	}

	/**
	 * Gets the companyCode value for this InquiryRequest.
	 * 
	 * @return companyCode
	 */
	public java.lang.String getCompanyCode() {
		return companyCode;
	}

	/**
	 * Sets the companyCode value for this InquiryRequest.
	 * 
	 * @param companyCode
	 */
	public void setCompanyCode(java.lang.String companyCode) {
		this.companyCode = companyCode;
	}

	/**
	 * Gets the channelID value for this InquiryRequest.
	 * 
	 * @return channelID
	 */
	public java.lang.String getChannelID() {
		return channelID;
	}

	/**
	 * Sets the channelID value for this InquiryRequest.
	 * 
	 * @param channelID
	 */
	public void setChannelID(java.lang.String channelID) {
		this.channelID = channelID;
	}

	/**
	 * Gets the terminalID value for this InquiryRequest.
	 * 
	 * @return terminalID
	 */
	public java.lang.String getTerminalID() {
		return terminalID;
	}

	/**
	 * Sets the terminalID value for this InquiryRequest.
	 * 
	 * @param terminalID
	 */
	public void setTerminalID(java.lang.String terminalID) {
		this.terminalID = terminalID;
	}

	/**
	 * Gets the billKey1 value for this InquiryRequest.
	 * 
	 * @return billKey1
	 */
	public java.lang.String getBillKey1() {
		return billKey1;
	}

	/**
	 * Sets the billKey1 value for this InquiryRequest.
	 * 
	 * @param billKey1
	 */
	public void setBillKey1(java.lang.String billKey1) {
		this.billKey1 = billKey1;
	}

	/**
	 * Gets the billKey2 value for this InquiryRequest.
	 * 
	 * @return billKey2
	 */
	public java.lang.String getBillKey2() {
		return billKey2;
	}

	/**
	 * Sets the billKey2 value for this InquiryRequest.
	 * 
	 * @param billKey2
	 */
	public void setBillKey2(java.lang.String billKey2) {
		this.billKey2 = billKey2;
	}

	/**
	 * Gets the billKey3 value for this InquiryRequest.
	 * 
	 * @return billKey3
	 */
	public java.lang.String getBillKey3() {
		return billKey3;
	}

	/**
	 * Sets the billKey3 value for this InquiryRequest.
	 * 
	 * @param billKey3
	 */
	public void setBillKey3(java.lang.String billKey3) {
		this.billKey3 = billKey3;
	}

	/**
	 * Gets the reference1 value for this InquiryRequest.
	 * 
	 * @return reference1
	 */
	public java.lang.String getReference1() {
		return reference1;
	}

	/**
	 * Sets the reference1 value for this InquiryRequest.
	 * 
	 * @param reference1
	 */
	public void setReference1(java.lang.String reference1) {
		this.reference1 = reference1;
	}

	/**
	 * Gets the reference2 value for this InquiryRequest.
	 * 
	 * @return reference2
	 */
	public java.lang.String getReference2() {
		return reference2;
	}

	/**
	 * Sets the reference2 value for this InquiryRequest.
	 * 
	 * @param reference2
	 */
	public void setReference2(java.lang.String reference2) {
		this.reference2 = reference2;
	}

	/**
	 * Gets the reference3 value for this InquiryRequest.
	 * 
	 * @return reference3
	 */
	public java.lang.String getReference3() {
		return reference3;
	}

	/**
	 * Sets the reference3 value for this InquiryRequest.
	 * 
	 * @param reference3
	 */
	public void setReference3(java.lang.String reference3) {
		this.reference3 = reference3;
	}

	private java.lang.Object __equalsCalc = null;

	public synchronized boolean equals(java.lang.Object obj) {
		if (!(obj instanceof InquiryRequest))
			return false;
		InquiryRequest other = (InquiryRequest) obj;
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (__equalsCalc != null) {
			return (__equalsCalc == obj);
		}
		__equalsCalc = obj;
		boolean _equals;
		_equals = true
				&& ((this.language == null && other.getLanguage() == null) || (this.language != null && this.language
						.equals(other.getLanguage())))
				&& ((this.trxDateTime == null && other.getTrxDateTime() == null) || (this.trxDateTime != null && this.trxDateTime
						.equals(other.getTrxDateTime())))
				&& ((this.transmissionDateTime == null && other
						.getTransmissionDateTime() == null) || (this.transmissionDateTime != null && this.transmissionDateTime
						.equals(other.getTransmissionDateTime())))
				&& ((this.companyCode == null && other.getCompanyCode() == null) || (this.companyCode != null && this.companyCode
						.equals(other.getCompanyCode())))
				&& ((this.channelID == null && other.getChannelID() == null) || (this.channelID != null && this.channelID
						.equals(other.getChannelID())))
				&& ((this.terminalID == null && other.getTerminalID() == null) || (this.terminalID != null && this.terminalID
						.equals(other.getTerminalID())))
				&& ((this.billKey1 == null && other.getBillKey1() == null) || (this.billKey1 != null && this.billKey1
						.equals(other.getBillKey1())))
				&& ((this.billKey2 == null && other.getBillKey2() == null) || (this.billKey2 != null && this.billKey2
						.equals(other.getBillKey2())))
				&& ((this.billKey3 == null && other.getBillKey3() == null) || (this.billKey3 != null && this.billKey3
						.equals(other.getBillKey3())))
				&& ((this.reference1 == null && other.getReference1() == null) || (this.reference1 != null && this.reference1
						.equals(other.getReference1())))
				&& ((this.reference2 == null && other.getReference2() == null) || (this.reference2 != null && this.reference2
						.equals(other.getReference2())))
				&& ((this.reference3 == null && other.getReference3() == null) || (this.reference3 != null && this.reference3
						.equals(other.getReference3())));
		__equalsCalc = null;
		return _equals;
	}

	private boolean __hashCodeCalc = false;

	public synchronized int hashCode() {
		if (__hashCodeCalc) {
			return 0;
		}
		__hashCodeCalc = true;
		int _hashCode = 1;
		if (getLanguage() != null) {
			_hashCode += getLanguage().hashCode();
		}
		if (getTrxDateTime() != null) {
			_hashCode += getTrxDateTime().hashCode();
		}
		if (getTransmissionDateTime() != null) {
			_hashCode += getTransmissionDateTime().hashCode();
		}
		if (getCompanyCode() != null) {
			_hashCode += getCompanyCode().hashCode();
		}
		if (getChannelID() != null) {
			_hashCode += getChannelID().hashCode();
		}
		if (getTerminalID() != null) {
			_hashCode += getTerminalID().hashCode();
		}
		if (getBillKey1() != null) {
			_hashCode += getBillKey1().hashCode();
		}
		if (getBillKey2() != null) {
			_hashCode += getBillKey2().hashCode();
		}
		if (getBillKey3() != null) {
			_hashCode += getBillKey3().hashCode();
		}
		if (getReference1() != null) {
			_hashCode += getReference1().hashCode();
		}
		if (getReference2() != null) {
			_hashCode += getReference2().hashCode();
		}
		if (getReference3() != null) {
			_hashCode += getReference3().hashCode();
		}
		__hashCodeCalc = false;
		return _hashCode;
	}

	// Type metadata
	private static org.apache.axis.description.TypeDesc typeDesc = new org.apache.axis.description.TypeDesc(
			InquiryRequest.class, true);

	static {
		typeDesc.setXmlType(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "InquiryRequest"));
		org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("language");
		elemField.setXmlName(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "language"));
		elemField.setXmlType(new javax.xml.namespace.QName(
				"http://www.w3.org/2001/XMLSchema", "string"));
		elemField.setMinOccurs(0);
		elemField.setNillable(false);
		typeDesc.addFieldDesc(elemField);
		elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("trxDateTime");
		elemField.setXmlName(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "trxDateTime"));
		elemField.setXmlType(new javax.xml.namespace.QName(
				"http://www.w3.org/2001/XMLSchema", "string"));
		elemField.setMinOccurs(0);
		elemField.setNillable(false);
		typeDesc.addFieldDesc(elemField);
		elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("transmissionDateTime");
		elemField.setXmlName(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "transmissionDateTime"));
		elemField.setXmlType(new javax.xml.namespace.QName(
				"http://www.w3.org/2001/XMLSchema", "string"));
		elemField.setMinOccurs(0);
		elemField.setNillable(false);
		typeDesc.addFieldDesc(elemField);
		elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("companyCode");
		elemField.setXmlName(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "companyCode"));
		elemField.setXmlType(new javax.xml.namespace.QName(
				"http://www.w3.org/2001/XMLSchema", "string"));
		elemField.setMinOccurs(0);
		elemField.setNillable(false);
		typeDesc.addFieldDesc(elemField);
		elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("channelID");
		elemField.setXmlName(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "channelID"));
		elemField.setXmlType(new javax.xml.namespace.QName(
				"http://www.w3.org/2001/XMLSchema", "string"));
		elemField.setMinOccurs(0);
		elemField.setNillable(false);
		typeDesc.addFieldDesc(elemField);
		elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("terminalID");
		elemField.setXmlName(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "terminalID"));
		elemField.setXmlType(new javax.xml.namespace.QName(
				"http://www.w3.org/2001/XMLSchema", "string"));
		elemField.setMinOccurs(0);
		elemField.setNillable(false);
		typeDesc.addFieldDesc(elemField);
		elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("billKey1");
		elemField.setXmlName(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "billKey1"));
		elemField.setXmlType(new javax.xml.namespace.QName(
				"http://www.w3.org/2001/XMLSchema", "string"));
		elemField.setMinOccurs(0);
		elemField.setNillable(false);
		typeDesc.addFieldDesc(elemField);
		elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("billKey2");
		elemField.setXmlName(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "billKey2"));
		elemField.setXmlType(new javax.xml.namespace.QName(
				"http://www.w3.org/2001/XMLSchema", "string"));
		elemField.setMinOccurs(0);
		elemField.setNillable(false);
		typeDesc.addFieldDesc(elemField);
		elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("billKey3");
		elemField.setXmlName(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "billKey3"));
		elemField.setXmlType(new javax.xml.namespace.QName(
				"http://www.w3.org/2001/XMLSchema", "string"));
		elemField.setMinOccurs(0);
		elemField.setNillable(false);
		typeDesc.addFieldDesc(elemField);
		elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("reference1");
		elemField.setXmlName(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "reference1"));
		elemField.setXmlType(new javax.xml.namespace.QName(
				"http://www.w3.org/2001/XMLSchema", "string"));
		elemField.setMinOccurs(0);
		elemField.setNillable(false);
		typeDesc.addFieldDesc(elemField);
		elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("reference2");
		elemField.setXmlName(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "reference2"));
		elemField.setXmlType(new javax.xml.namespace.QName(
				"http://www.w3.org/2001/XMLSchema", "string"));
		elemField.setMinOccurs(0);
		elemField.setNillable(false);
		typeDesc.addFieldDesc(elemField);
		elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("reference3");
		elemField.setXmlName(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "reference3"));
		elemField.setXmlType(new javax.xml.namespace.QName(
				"http://www.w3.org/2001/XMLSchema", "string"));
		elemField.setMinOccurs(0);
		elemField.setNillable(false);
		typeDesc.addFieldDesc(elemField);
	}

	/**
	 * Return type metadata object
	 */
	public static org.apache.axis.description.TypeDesc getTypeDesc() {
		return typeDesc;
	}

	/**
	 * Get Custom Serializer
	 */
	public static org.apache.axis.encoding.Serializer getSerializer(
			java.lang.String mechType, java.lang.Class _javaType,
			javax.xml.namespace.QName _xmlType) {
		return new org.apache.axis.encoding.ser.BeanSerializer(_javaType,
				_xmlType, typeDesc);
	}

	/**
	 * Get Custom Deserializer
	 */
	public static org.apache.axis.encoding.Deserializer getDeserializer(
			java.lang.String mechType, java.lang.Class _javaType,
			javax.xml.namespace.QName _xmlType) {
		return new org.apache.axis.encoding.ser.BeanDeserializer(_javaType,
				_xmlType, typeDesc);
	}

}
