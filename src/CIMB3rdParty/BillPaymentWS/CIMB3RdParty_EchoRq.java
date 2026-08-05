/**
 * CIMB3RdParty_EchoRq.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package CIMB3rdParty.BillPaymentWS;

public class CIMB3RdParty_EchoRq  implements java.io.Serializable {
    private java.lang.String echoRequest;

    public CIMB3RdParty_EchoRq() {
    }

    public CIMB3RdParty_EchoRq(
           java.lang.String echoRequest) {
           this.echoRequest = echoRequest;
    }


    /**
     * Gets the echoRequest value for this CIMB3RdParty_EchoRq.
     * 
     * @return echoRequest
     */
    public java.lang.String getEchoRequest() {
        return echoRequest;
    }


    /**
     * Sets the echoRequest value for this CIMB3RdParty_EchoRq.
     * 
     * @param echoRequest
     */
    public void setEchoRequest(java.lang.String echoRequest) {
        this.echoRequest = echoRequest;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof CIMB3RdParty_EchoRq)) return false;
        CIMB3RdParty_EchoRq other = (CIMB3RdParty_EchoRq) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.echoRequest==null && other.getEchoRequest()==null) || 
             (this.echoRequest!=null &&
              this.echoRequest.equals(other.getEchoRequest())));
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
        if (getEchoRequest() != null) {
            _hashCode += getEchoRequest().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(CIMB3RdParty_EchoRq.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", ">CIMB3rdParty_EchoRq"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("echoRequest");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "EchoRequest"));
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
