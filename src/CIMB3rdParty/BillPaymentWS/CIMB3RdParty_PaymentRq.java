/**
 * CIMB3RdParty_PaymentRq.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package CIMB3rdParty.BillPaymentWS;

public class CIMB3RdParty_PaymentRq  implements java.io.Serializable {
    private CIMB3rdParty.BillPaymentWS.PaymentRq paymentRq;

    public CIMB3RdParty_PaymentRq() {
    }

    public CIMB3RdParty_PaymentRq(
           CIMB3rdParty.BillPaymentWS.PaymentRq paymentRq) {
           this.paymentRq = paymentRq;
    }


    /**
     * Gets the paymentRq value for this CIMB3RdParty_PaymentRq.
     * 
     * @return paymentRq
     */
    public CIMB3rdParty.BillPaymentWS.PaymentRq getPaymentRq() {
        return paymentRq;
    }


    /**
     * Sets the paymentRq value for this CIMB3RdParty_PaymentRq.
     * 
     * @param paymentRq
     */
    public void setPaymentRq(CIMB3rdParty.BillPaymentWS.PaymentRq paymentRq) {
        this.paymentRq = paymentRq;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof CIMB3RdParty_PaymentRq)) return false;
        CIMB3RdParty_PaymentRq other = (CIMB3RdParty_PaymentRq) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.paymentRq==null && other.getPaymentRq()==null) || 
             (this.paymentRq!=null &&
              this.paymentRq.equals(other.getPaymentRq())));
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
        if (getPaymentRq() != null) {
            _hashCode += getPaymentRq().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(CIMB3RdParty_PaymentRq.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "CIMB3rdParty_PaymentRq"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("paymentRq");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "PaymentRq"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "PaymentRq"));
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
