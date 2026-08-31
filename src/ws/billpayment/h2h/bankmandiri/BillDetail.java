/**
 * BillDetail.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ws.billpayment.h2h.bankmandiri;

/**
 * DTO SOAP bill-payment untuk bill detail. Tipe ini merepresentasikan bagian pesan wire dan hanya
 * menyimpan nilai yang akan diserialisasi atau dibaca oleh binding Apache Axis.
 *
 * <p><b>Batas tanggung jawab:</b> tipe ini hanya memodelkan data pesan SOAP. Interface
 * {@link java.io.Serializable} (dan {@code Comparable}, bila ada) adalah kebutuhan binding/collection, bukan
 * tempat implementasi transaksi. Validasi, autentikasi, dan aturan pembayaran wajib tetap berada pada endpoint
 * atau service domain agar DTO wire tidak menjadi sumber aturan yang tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code java.lang.String billCode}, {@code
 * java.lang.String billName}, {@code java.lang.String billShortName}, {@code java.lang.String billAmount},
 * {@code java.lang.String reference1}, {@code java.lang.String reference2}, {@code java.lang.String reference3},
 * {@code java.lang.Object __equalsCalc}; pembacaan/pencarian ({@code getBillCode()}, {@code getBillName()},
 * {@code getBillShortName()}, {@code getBillAmount()}, {@code getReference1()}, {@code getReference2()}); mutasi
 * data ({@code setBillCode()}, {@code setBillName()}, {@code setBillShortName()}, {@code setBillAmount()},
 * {@code setReference1()}, {@code setReference2()}); operasi domain lain ({@code equals()}, {@code hashCode()},
 * {@code compareTo()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> accessor hanya membaca atau mengganti state DTO. Tidak ada I/O, transaksi,
 * autentikasi, maupun validasi bisnis; nilai wajib, format, dan makna status mengikuti WSDL serta implementasi
 * endpoint. Jangan menambahkan aturan domain ke binding ini.</p>
 */
public class BillDetail implements java.io.Serializable, Comparable<BillDetail> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2722225557427345749L;

	private java.lang.String billCode = "";

	private java.lang.String billName = "";

	private java.lang.String billShortName = "";

	private java.lang.String billAmount = "";

	private java.lang.String reference1 = "";

	private java.lang.String reference2 = "";

	private java.lang.String reference3 = "";

	public BillDetail() {
	}

	public BillDetail(java.lang.String billCode, java.lang.String billName,
			java.lang.String billShortName, java.lang.String billAmount,
			java.lang.String reference1, java.lang.String reference2,
			java.lang.String reference3) {
		this.billCode = billCode;
		this.billName = billName;
		this.billShortName = billShortName;
		this.billAmount = billAmount;
		this.reference1 = reference1;
		this.reference2 = reference2;
		this.reference3 = reference3;
	}

	/**
	 * Gets the billCode value for this BillDetail.
	 * 
	 * @return billCode
	 */
	public java.lang.String getBillCode() {
		return billCode;
	}

	/**
	 * Sets the billCode value for this BillDetail.
	 * 
	 * @param billCode
	 */
	public void setBillCode(java.lang.String billCode) {
		this.billCode = billCode;
	}

	/**
	 * Gets the billName value for this BillDetail.
	 * 
	 * @return billName
	 */
	public java.lang.String getBillName() {
		return billName;
	}

	/**
	 * Sets the billName value for this BillDetail.
	 * 
	 * @param billName
	 */
	public void setBillName(java.lang.String billName) {
		this.billName = billName;
	}

	/**
	 * Gets the billShortName value for this BillDetail.
	 * 
	 * @return billShortName
	 */
	public java.lang.String getBillShortName() {
		return billShortName;
	}

	/**
	 * Sets the billShortName value for this BillDetail.
	 * 
	 * @param billShortName
	 */
	public void setBillShortName(java.lang.String billShortName) {
		this.billShortName = billShortName;
	}

	/**
	 * Gets the billAmount value for this BillDetail.
	 * 
	 * @return billAmount
	 */
	public java.lang.String getBillAmount() {
		return billAmount;
	}

	/**
	 * Sets the billAmount value for this BillDetail.
	 * 
	 * @param billAmount
	 */
	public void setBillAmount(java.lang.String billAmount) {
		this.billAmount = billAmount;
	}

	/**
	 * Gets the reference1 value for this BillDetail.
	 * 
	 * @return reference1
	 */
	public java.lang.String getReference1() {
		return reference1;
	}

	/**
	 * Sets the reference1 value for this BillDetail.
	 * 
	 * @param reference1
	 */
	public void setReference1(java.lang.String reference1) {
		this.reference1 = reference1;
	}

	/**
	 * Gets the reference2 value for this BillDetail.
	 * 
	 * @return reference2
	 */
	public java.lang.String getReference2() {
		return reference2;
	}

	/**
	 * Sets the reference2 value for this BillDetail.
	 * 
	 * @param reference2
	 */
	public void setReference2(java.lang.String reference2) {
		this.reference2 = reference2;
	}

	/**
	 * Gets the reference3 value for this BillDetail.
	 * 
	 * @return reference3
	 */
	public java.lang.String getReference3() {
		return reference3;
	}

	/**
	 * Sets the reference3 value for this BillDetail.
	 * 
	 * @param reference3
	 */
	public void setReference3(java.lang.String reference3) {
		this.reference3 = reference3;
	}

	private java.lang.Object __equalsCalc = null;

	public synchronized boolean equals(java.lang.Object obj) {
		if (!(obj instanceof BillDetail))
			return false;
		BillDetail other = (BillDetail) obj;
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
				&& ((this.billCode == null && other.getBillCode() == null) || (this.billCode != null && this.billCode
						.equals(other.getBillCode())))
				&& ((this.billName == null && other.getBillName() == null) || (this.billName != null && this.billName
						.equals(other.getBillName())))
				&& ((this.billShortName == null && other.getBillShortName() == null) || (this.billShortName != null && this.billShortName
						.equals(other.getBillShortName())))
				&& ((this.billAmount == null && other.getBillAmount() == null) || (this.billAmount != null && this.billAmount
						.equals(other.getBillAmount())))
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
		if (getBillCode() != null) {
			_hashCode += getBillCode().hashCode();
		}
		if (getBillName() != null) {
			_hashCode += getBillName().hashCode();
		}
		if (getBillShortName() != null) {
			_hashCode += getBillShortName().hashCode();
		}
		if (getBillAmount() != null) {
			_hashCode += getBillAmount().hashCode();
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
			BillDetail.class, true);

	static {
		typeDesc.setXmlType(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "BillDetail"));
		org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("billCode");
		elemField.setXmlName(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "billCode"));
		elemField.setXmlType(new javax.xml.namespace.QName(
				"http://www.w3.org/2001/XMLSchema", "string"));
		elemField.setMinOccurs(0);
		elemField.setNillable(false);
		typeDesc.addFieldDesc(elemField);
		elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("billName");
		elemField.setXmlName(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "billName"));
		elemField.setXmlType(new javax.xml.namespace.QName(
				"http://www.w3.org/2001/XMLSchema", "string"));
		elemField.setMinOccurs(0);
		elemField.setNillable(false);
		typeDesc.addFieldDesc(elemField);
		elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("billShortName");
		elemField.setXmlName(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "billShortName"));
		elemField.setXmlType(new javax.xml.namespace.QName(
				"http://www.w3.org/2001/XMLSchema", "string"));
		elemField.setMinOccurs(0);
		elemField.setNillable(false);
		typeDesc.addFieldDesc(elemField);
		elemField = new org.apache.axis.description.ElementDesc();
		elemField.setFieldName("billAmount");
		elemField.setXmlName(new javax.xml.namespace.QName(
				"bankmandiri.h2h.billpayment.ws", "billAmount"));
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

	@Override
	public int compareTo(BillDetail o) {
		try {
			if (billCode != null && o.billCode != null) {
				return new Integer(billCode).compareTo(new Integer(o.billCode));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ws/billpayment/h2h/bankmandiri/BillDetail.java:344");
		}
		if (billCode != null && o.billCode != null) {
			return (billCode).compareTo((o.billCode));
		}
		return 0;
	}

}
