/**
 * BillDetail.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package CIMB3rdParty.BillPaymentWS;

public class BillDetail  implements java.io.Serializable {
    private java.lang.String billCurrency;

    private java.lang.String billCode;

    private java.lang.Integer billAmount;

    private java.lang.String billReference;

    public BillDetail() {
    }

    public BillDetail(
           java.lang.String billCurrency,
           java.lang.String billCode,
           java.lang.Integer billAmount,
           java.lang.String billReference) {
           this.billCurrency = billCurrency;
           this.billCode = billCode;
           this.billAmount = billAmount;
           this.billReference = billReference;
    }


    /**
     * Gets the billCurrency value for this BillDetail.
     * 
     * @return billCurrency
     */
    public java.lang.String getBillCurrency() {
        return billCurrency;
    }


    /**
     * Sets the billCurrency value for this BillDetail.
     * 
     * @param billCurrency
     */
    public void setBillCurrency(java.lang.String billCurrency) {
        this.billCurrency = billCurrency;
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
     * Gets the billAmount value for this BillDetail.
     * 
     * @return billAmount
     */
    public java.lang.Integer getBillAmount() {
        return billAmount;
    }


    /**
     * Sets the billAmount value for this BillDetail.
     * 
     * @param billAmount
     */
    public void setBillAmount(java.lang.Integer billAmount) {
        this.billAmount = billAmount;
    }


    /**
     * Gets the billReference value for this BillDetail.
     * 
     * @return billReference
     */
    public java.lang.String getBillReference() {
        return billReference;
    }


    /**
     * Sets the billReference value for this BillDetail.
     * 
     * @param billReference
     */
    public void setBillReference(java.lang.String billReference) {
        this.billReference = billReference;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof BillDetail)) return false;
        BillDetail other = (BillDetail) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.billCurrency==null && other.getBillCurrency()==null) || 
             (this.billCurrency!=null &&
              this.billCurrency.equals(other.getBillCurrency()))) &&
            ((this.billCode==null && other.getBillCode()==null) || 
             (this.billCode!=null &&
              this.billCode.equals(other.getBillCode()))) &&
            ((this.billAmount==null && other.getBillAmount()==null) || 
             (this.billAmount!=null &&
              this.billAmount.equals(other.getBillAmount()))) &&
            ((this.billReference==null && other.getBillReference()==null) || 
             (this.billReference!=null &&
              this.billReference.equals(other.getBillReference())));
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
        if (getBillCurrency() != null) {
            _hashCode += getBillCurrency().hashCode();
        }
        if (getBillCode() != null) {
            _hashCode += getBillCode().hashCode();
        }
        if (getBillAmount() != null) {
            _hashCode += getBillAmount().hashCode();
        }
        if (getBillReference() != null) {
            _hashCode += getBillReference().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(BillDetail.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "BillDetail"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billCurrency");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "BillCurrency"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billCode");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "BillCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billAmount");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "BillAmount"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billReference");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "BillReference"));
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
