/**
 * CIMB3RdParty_EchoRs.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package CIMB3rdParty.BillPaymentWS;

public class CIMB3RdParty_EchoRs implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7303622873958336653L;
	private java.lang.String echoResponse;

	public CIMB3RdParty_EchoRs() {
	}

	public CIMB3RdParty_EchoRs(java.lang.String echoResponse) {
		this.echoResponse = echoResponse;
	}

	/**
	 * Gets the echoResponse value for this CIMB3RdParty_EchoRs.
	 * 
	 * @return echoResponse
	 */
	public java.lang.String getEchoResponse() {
		return echoResponse;
	}

	/**
	 * Sets the echoResponse value for this CIMB3RdParty_EchoRs.
	 * 
	 * @param echoResponse
	 */
	public void setEchoResponse(java.lang.String echoResponse) {
		this.echoResponse = echoResponse;
	}

	private java.lang.Object __equalsCalc = null;

	public synchronized boolean equals(java.lang.Object obj) {
		if (!(obj instanceof CIMB3RdParty_EchoRs))
			return false;
		CIMB3RdParty_EchoRs other = (CIMB3RdParty_EchoRs) obj;
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (__equalsCalc != null) {
			return (__equalsCalc == obj);
		}
		__equalsCalc = obj;
		boolean _equals;
		_equals = true && ((this.echoResponse == null && other.getEchoResponse() == null)
				|| (this.echoResponse != null && this.echoResponse.equals(other.getEchoResponse())));
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
		if (getEchoResponse() != null) {
			_hashCode += getEchoResponse().hashCode();
		}
		__hashCodeCalc = false;
		return _hashCode;
	}

	// Type metadata
	private static org.apache.axis.description.TypeDesc typeDesc = new org.apache.axis.description.TypeDesc(
			CIMB3RdParty_EchoRs.class, true);

	static {
		typeDesc.setXmlType(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", ">CIMB3rdParty_EchoRs"));
		org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("echoResponse");
		elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "EchoResponse"));
		elemField.setXmlType(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "EchoMessage"));
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
	public static org.apache.axis.encoding.Serializer getSerializer(java.lang.String mechType,
			java.lang.Class _javaType, javax.xml.namespace.QName _xmlType) {
		return new org.apache.axis.encoding.ser.BeanSerializer(_javaType, _xmlType, typeDesc);
	}

	/**
	 * Get Custom Deserializer
	 */
	public static org.apache.axis.encoding.Deserializer getDeserializer(java.lang.String mechType,
			java.lang.Class _javaType, javax.xml.namespace.QName _xmlType) {
		return new org.apache.axis.encoding.ser.BeanDeserializer(_javaType, _xmlType, typeDesc);
	}

}
