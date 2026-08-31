/**
 * Status.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ws.billpayment.h2h.bankmandiri;

/**
 * DTO SOAP bill-payment untuk status. Tipe ini merepresentasikan bagian pesan wire dan hanya
 * menyimpan nilai yang akan diserialisasi atau dibaca oleh binding Apache Axis.
 *
 * <p><b>Batas tanggung jawab:</b> tipe ini hanya memodelkan data pesan SOAP. Interface
 * {@link java.io.Serializable} (dan {@code Comparable}, bila ada) adalah kebutuhan binding/collection, bukan
 * tempat implementasi transaksi. Validasi, autentikasi, dan aturan pembayaran wajib tetap berada pada endpoint
 * atau service domain agar DTO wire tidak menjadi sumber aturan yang tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code boolean isError}, {@code
 * java.lang.String errorCode}, {@code java.lang.String statusDescription}, {@code java.lang.Object
 * __equalsCalc}, {@code boolean __hashCodeCalc}, {@code org.apache.axis.description.TypeDesc typeDesc};
 * pembacaan/pencarian ({@code getErrorCode()}, {@code getStatusDescription()}, {@code getTypeDesc()}, {@code
 * getSerializer()}, {@code getDeserializer()}); mutasi data ({@code setIsError()}, {@code setErrorCode()},
 * {@code setStatusDescription()}); operasi domain lain ({@code isIsError()}, {@code equals()}, {@code
 * hashCode()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor hanya membaca atau mengganti state DTO. Tidak ada I/O, transaksi,
 * autentikasi, maupun validasi bisnis; nilai wajib, format, dan makna status mengikuti WSDL serta implementasi
 * endpoint. Jangan menambahkan aturan domain ke binding ini.</p>
 */
public class Status  implements java.io.Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 283997197415587629L;

	private boolean isError;

    private java.lang.String errorCode;

    private java.lang.String statusDescription;

    public Status() {
    }

    public Status(
           boolean isError,
           java.lang.String errorCode,
           java.lang.String statusDescription) {
           this.isError = isError;
           this.errorCode = errorCode;
           this.statusDescription = statusDescription;
    }


    /**
     * Gets the isError value for this Status.
     * 
     * @return isError
     */
    public boolean isIsError() {
        return isError;
    }


    /**
     * Sets the isError value for this Status.
     * 
     * @param isError
     */
    public void setIsError(boolean isError) {
        this.isError = isError;
    }


    /**
     * Gets the errorCode value for this Status.
     * 
     * @return errorCode
     */
    public java.lang.String getErrorCode() {
        return errorCode;
    }


    /**
     * Sets the errorCode value for this Status.
     * 
     * @param errorCode
     */
    public void setErrorCode(java.lang.String errorCode) {
        this.errorCode = errorCode;
    }


    /**
     * Gets the statusDescription value for this Status.
     * 
     * @return statusDescription
     */
    public java.lang.String getStatusDescription() {
        return statusDescription;
    }


    /**
     * Sets the statusDescription value for this Status.
     * 
     * @param statusDescription
     */
    public void setStatusDescription(java.lang.String statusDescription) {
        this.statusDescription = statusDescription;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Status)) return false;
        Status other = (Status) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            this.isError == other.isIsError() &&
            ((this.errorCode==null && other.getErrorCode()==null) || 
             (this.errorCode!=null &&
              this.errorCode.equals(other.getErrorCode()))) &&
            ((this.statusDescription==null && other.getStatusDescription()==null) || 
             (this.statusDescription!=null &&
              this.statusDescription.equals(other.getStatusDescription())));
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
        _hashCode += (isIsError() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        if (getErrorCode() != null) {
            _hashCode += getErrorCode().hashCode();
        }
        if (getStatusDescription() != null) {
            _hashCode += getStatusDescription().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Status.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "Status"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("isError");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "isError"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("errorCode");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "errorCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("statusDescription");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "statusDescription"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
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
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
