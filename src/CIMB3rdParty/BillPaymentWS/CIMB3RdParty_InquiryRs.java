/**
 * CIMB3RdParty_InquiryRs.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package CIMB3rdParty.BillPaymentWS;

/**
 * DTO respons SOAP bill-payment untuk cimb3 rd party inquiry rs. Field kelas ini membawa status
 * dan data protokol yang diserialisasi oleh Apache Axis; pembentukan keputusan bisnis tetap milik
 * implementasi layanan.
 *
 * <p><b>Batas tanggung jawab:</b> tipe ini hanya memodelkan data pesan SOAP. Interface
 * {@link java.io.Serializable} (dan {@code Comparable}, bila ada) adalah kebutuhan binding/collection, bukan
 * tempat implementasi transaksi. Validasi, autentikasi, dan aturan pembayaran wajib tetap berada pada endpoint
 * atau service domain agar DTO wire tidak menjadi sumber aturan yang tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code CIMB3rdParty.BillPaymentWS.InquiryRs
 * inquiryRs}, {@code java.lang.Object __equalsCalc}, {@code boolean __hashCodeCalc}, {@code
 * org.apache.axis.description.TypeDesc typeDesc}; pembacaan/pencarian ({@code getInquiryRs()}, {@code
 * getTypeDesc()}, {@code getSerializer()}, {@code getDeserializer()}); mutasi data ({@code setInquiryRs()});
 * operasi domain lain ({@code equals()}, {@code hashCode()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor hanya membaca atau mengganti state DTO. Tidak ada I/O, transaksi,
 * autentikasi, maupun validasi bisnis; nilai wajib, format, dan makna status mengikuti WSDL serta implementasi
 * endpoint. Jangan menambahkan aturan domain ke binding ini.</p>
 */
public class CIMB3RdParty_InquiryRs  implements java.io.Serializable {
    private CIMB3rdParty.BillPaymentWS.InquiryRs inquiryRs;

    public CIMB3RdParty_InquiryRs() {
    }

    public CIMB3RdParty_InquiryRs(
           CIMB3rdParty.BillPaymentWS.InquiryRs inquiryRs) {
           this.inquiryRs = inquiryRs;
    }


    /**
     * Gets the inquiryRs value for this CIMB3RdParty_InquiryRs.
     * 
     * @return inquiryRs
     */
    public CIMB3rdParty.BillPaymentWS.InquiryRs getInquiryRs() {
        return inquiryRs;
    }


    /**
     * Sets the inquiryRs value for this CIMB3RdParty_InquiryRs.
     * 
     * @param inquiryRs
     */
    public void setInquiryRs(CIMB3rdParty.BillPaymentWS.InquiryRs inquiryRs) {
        this.inquiryRs = inquiryRs;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof CIMB3RdParty_InquiryRs)) return false;
        CIMB3RdParty_InquiryRs other = (CIMB3RdParty_InquiryRs) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.inquiryRs==null && other.getInquiryRs()==null) || 
             (this.inquiryRs!=null &&
              this.inquiryRs.equals(other.getInquiryRs())));
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
        if (getInquiryRs() != null) {
            _hashCode += getInquiryRs().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(CIMB3RdParty_InquiryRs.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "CIMB3rdParty_InquiryRs"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("inquiryRs");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "InquiryRs"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "InquiryRs"));
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
