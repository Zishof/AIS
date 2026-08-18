/**
 * ReversalResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ws.billpayment.h2h.bankmandiri;

public class ReversalResponse  implements java.io.Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 3432129076414059390L;
	private ws.billpayment.h2h.bankmandiri.Status status;

    public ReversalResponse() {
    }

    public ReversalResponse(
           ws.billpayment.h2h.bankmandiri.Status status) {
           this.status = status;
    }


    /**
     * Gets the status value for this ReversalResponse.
     * 
     * @return status
     */
    public ws.billpayment.h2h.bankmandiri.Status getStatus() {
        return status;
    }


    /**
     * Sets the status value for this ReversalResponse.
     * 
     * @param status
     */
    public void setStatus(ws.billpayment.h2h.bankmandiri.Status status) {
        this.status = status;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof ReversalResponse)) return false;
        ReversalResponse other = (ReversalResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.status==null && other.getStatus()==null) || 
             (this.status!=null &&
              this.status.equals(other.getStatus())));
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
        if (getStatus() != null) {
            _hashCode += getStatus().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(ReversalResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "ReversalResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("status");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "status"));
        elemField.setXmlType(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "Status"));
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
