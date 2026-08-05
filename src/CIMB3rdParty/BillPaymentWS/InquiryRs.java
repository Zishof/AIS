/**
 * InquiryRs.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package CIMB3rdParty.BillPaymentWS;

public class InquiryRs  implements java.io.Serializable {
    private java.lang.String transactionID;

    private java.lang.String channelID;

    private java.lang.String terminalID;

    private java.lang.String transactionDate;

    private java.lang.String companyCode;

    private java.lang.String customerKey1;

    private java.lang.String customerKey2;

    private java.lang.String customerKey3;

    private CIMB3rdParty.BillPaymentWS.BillDetail[] billDetailList;

    private java.lang.String currency;

    private java.lang.Integer amount;

    private java.lang.Integer fee;

    private java.lang.Integer paidAmount;

    private java.lang.String customerName;

    private java.lang.String additionalData1;

    private java.lang.String additionalData2;

    private java.lang.String additionalData3;

    private java.lang.String additionalData4;

    private java.lang.String flagPayment;

    private java.lang.String responseCode;

    private java.lang.String responseDescription;

    public InquiryRs() {
    }

    public InquiryRs(
           java.lang.String transactionID,
           java.lang.String channelID,
           java.lang.String terminalID,
           java.lang.String transactionDate,
           java.lang.String companyCode,
           java.lang.String customerKey1,
           java.lang.String customerKey2,
           java.lang.String customerKey3,
           CIMB3rdParty.BillPaymentWS.BillDetail[] billDetailList,
           java.lang.String currency,
           java.lang.Integer amount,
           java.lang.Integer fee,
           java.lang.Integer paidAmount,
           java.lang.String customerName,
           java.lang.String additionalData1,
           java.lang.String additionalData2,
           java.lang.String additionalData3,
           java.lang.String additionalData4,
           java.lang.String flagPayment,
           java.lang.String responseCode,
           java.lang.String responseDescription) {
           this.transactionID = transactionID;
           this.channelID = channelID;
           this.terminalID = terminalID;
           this.transactionDate = transactionDate;
           this.companyCode = companyCode;
           this.customerKey1 = customerKey1;
           this.customerKey2 = customerKey2;
           this.customerKey3 = customerKey3;
           this.billDetailList = billDetailList;
           this.currency = currency;
           this.amount = amount;
           this.fee = fee;
           this.paidAmount = paidAmount;
           this.customerName = customerName;
           this.additionalData1 = additionalData1;
           this.additionalData2 = additionalData2;
           this.additionalData3 = additionalData3;
           this.additionalData4 = additionalData4;
           this.flagPayment = flagPayment;
           this.responseCode = responseCode;
           this.responseDescription = responseDescription;
    }


    /**
     * Gets the transactionID value for this InquiryRs.
     * 
     * @return transactionID
     */
    public java.lang.String getTransactionID() {
        return transactionID;
    }


    /**
     * Sets the transactionID value for this InquiryRs.
     * 
     * @param transactionID
     */
    public void setTransactionID(java.lang.String transactionID) {
        this.transactionID = transactionID;
    }


    /**
     * Gets the channelID value for this InquiryRs.
     * 
     * @return channelID
     */
    public java.lang.String getChannelID() {
        return channelID;
    }


    /**
     * Sets the channelID value for this InquiryRs.
     * 
     * @param channelID
     */
    public void setChannelID(java.lang.String channelID) {
        this.channelID = channelID;
    }


    /**
     * Gets the terminalID value for this InquiryRs.
     * 
     * @return terminalID
     */
    public java.lang.String getTerminalID() {
        return terminalID;
    }


    /**
     * Sets the terminalID value for this InquiryRs.
     * 
     * @param terminalID
     */
    public void setTerminalID(java.lang.String terminalID) {
        this.terminalID = terminalID;
    }


    /**
     * Gets the transactionDate value for this InquiryRs.
     * 
     * @return transactionDate
     */
    public java.lang.String getTransactionDate() {
        return transactionDate;
    }


    /**
     * Sets the transactionDate value for this InquiryRs.
     * 
     * @param transactionDate
     */
    public void setTransactionDate(java.lang.String transactionDate) {
        this.transactionDate = transactionDate;
    }


    /**
     * Gets the companyCode value for this InquiryRs.
     * 
     * @return companyCode
     */
    public java.lang.String getCompanyCode() {
        return companyCode;
    }


    /**
     * Sets the companyCode value for this InquiryRs.
     * 
     * @param companyCode
     */
    public void setCompanyCode(java.lang.String companyCode) {
        this.companyCode = companyCode;
    }


    /**
     * Gets the customerKey1 value for this InquiryRs.
     * 
     * @return customerKey1
     */
    public java.lang.String getCustomerKey1() {
        return customerKey1;
    }


    /**
     * Sets the customerKey1 value for this InquiryRs.
     * 
     * @param customerKey1
     */
    public void setCustomerKey1(java.lang.String customerKey1) {
        this.customerKey1 = customerKey1;
    }


    /**
     * Gets the customerKey2 value for this InquiryRs.
     * 
     * @return customerKey2
     */
    public java.lang.String getCustomerKey2() {
        return customerKey2;
    }


    /**
     * Sets the customerKey2 value for this InquiryRs.
     * 
     * @param customerKey2
     */
    public void setCustomerKey2(java.lang.String customerKey2) {
        this.customerKey2 = customerKey2;
    }


    /**
     * Gets the customerKey3 value for this InquiryRs.
     * 
     * @return customerKey3
     */
    public java.lang.String getCustomerKey3() {
        return customerKey3;
    }


    /**
     * Sets the customerKey3 value for this InquiryRs.
     * 
     * @param customerKey3
     */
    public void setCustomerKey3(java.lang.String customerKey3) {
        this.customerKey3 = customerKey3;
    }


    /**
     * Gets the billDetailList value for this InquiryRs.
     * 
     * @return billDetailList
     */
    public CIMB3rdParty.BillPaymentWS.BillDetail[] getBillDetailList() {
        return billDetailList;
    }


    /**
     * Sets the billDetailList value for this InquiryRs.
     * 
     * @param billDetailList
     */
    public void setBillDetailList(CIMB3rdParty.BillPaymentWS.BillDetail[] billDetailList) {
        this.billDetailList = billDetailList;
    }


    /**
     * Gets the currency value for this InquiryRs.
     * 
     * @return currency
     */
    public java.lang.String getCurrency() {
        return currency;
    }


    /**
     * Sets the currency value for this InquiryRs.
     * 
     * @param currency
     */
    public void setCurrency(java.lang.String currency) {
        this.currency = currency;
    }


    /**
     * Gets the amount value for this InquiryRs.
     * 
     * @return amount
     */
    public java.lang.Integer getAmount() {
        return amount;
    }


    /**
     * Sets the amount value for this InquiryRs.
     * 
     * @param amount
     */
    public void setAmount(java.lang.Integer amount) {
        this.amount = amount;
    }


    /**
     * Gets the fee value for this InquiryRs.
     * 
     * @return fee
     */
    public java.lang.Integer getFee() {
        return fee;
    }


    /**
     * Sets the fee value for this InquiryRs.
     * 
     * @param fee
     */
    public void setFee(java.lang.Integer fee) {
        this.fee = fee;
    }


    /**
     * Gets the paidAmount value for this InquiryRs.
     * 
     * @return paidAmount
     */
    public java.lang.Integer getPaidAmount() {
        return paidAmount;
    }


    /**
     * Sets the paidAmount value for this InquiryRs.
     * 
     * @param paidAmount
     */
    public void setPaidAmount(java.lang.Integer paidAmount) {
        this.paidAmount = paidAmount;
    }


    /**
     * Gets the customerName value for this InquiryRs.
     * 
     * @return customerName
     */
    public java.lang.String getCustomerName() {
        return customerName;
    }


    /**
     * Sets the customerName value for this InquiryRs.
     * 
     * @param customerName
     */
    public void setCustomerName(java.lang.String customerName) {
        this.customerName = customerName;
    }


    /**
     * Gets the additionalData1 value for this InquiryRs.
     * 
     * @return additionalData1
     */
    public java.lang.String getAdditionalData1() {
        return additionalData1;
    }


    /**
     * Sets the additionalData1 value for this InquiryRs.
     * 
     * @param additionalData1
     */
    public void setAdditionalData1(java.lang.String additionalData1) {
        this.additionalData1 = additionalData1;
    }


    /**
     * Gets the additionalData2 value for this InquiryRs.
     * 
     * @return additionalData2
     */
    public java.lang.String getAdditionalData2() {
        return additionalData2;
    }


    /**
     * Sets the additionalData2 value for this InquiryRs.
     * 
     * @param additionalData2
     */
    public void setAdditionalData2(java.lang.String additionalData2) {
        this.additionalData2 = additionalData2;
    }


    /**
     * Gets the additionalData3 value for this InquiryRs.
     * 
     * @return additionalData3
     */
    public java.lang.String getAdditionalData3() {
        return additionalData3;
    }


    /**
     * Sets the additionalData3 value for this InquiryRs.
     * 
     * @param additionalData3
     */
    public void setAdditionalData3(java.lang.String additionalData3) {
        this.additionalData3 = additionalData3;
    }


    /**
     * Gets the additionalData4 value for this InquiryRs.
     * 
     * @return additionalData4
     */
    public java.lang.String getAdditionalData4() {
        return additionalData4;
    }


    /**
     * Sets the additionalData4 value for this InquiryRs.
     * 
     * @param additionalData4
     */
    public void setAdditionalData4(java.lang.String additionalData4) {
        this.additionalData4 = additionalData4;
    }


    /**
     * Gets the flagPayment value for this InquiryRs.
     * 
     * @return flagPayment
     */
    public java.lang.String getFlagPayment() {
        return flagPayment;
    }


    /**
     * Sets the flagPayment value for this InquiryRs.
     * 
     * @param flagPayment
     */
    public void setFlagPayment(java.lang.String flagPayment) {
        this.flagPayment = flagPayment;
    }


    /**
     * Gets the responseCode value for this InquiryRs.
     * 
     * @return responseCode
     */
    public java.lang.String getResponseCode() {
        return responseCode;
    }


    /**
     * Sets the responseCode value for this InquiryRs.
     * 
     * @param responseCode
     */
    public void setResponseCode(java.lang.String responseCode) {
        this.responseCode = responseCode;
    }


    /**
     * Gets the responseDescription value for this InquiryRs.
     * 
     * @return responseDescription
     */
    public java.lang.String getResponseDescription() {
        return responseDescription;
    }


    /**
     * Sets the responseDescription value for this InquiryRs.
     * 
     * @param responseDescription
     */
    public void setResponseDescription(java.lang.String responseDescription) {
        this.responseDescription = responseDescription;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof InquiryRs)) return false;
        InquiryRs other = (InquiryRs) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.transactionID==null && other.getTransactionID()==null) || 
             (this.transactionID!=null &&
              this.transactionID.equals(other.getTransactionID()))) &&
            ((this.channelID==null && other.getChannelID()==null) || 
             (this.channelID!=null &&
              this.channelID.equals(other.getChannelID()))) &&
            ((this.terminalID==null && other.getTerminalID()==null) || 
             (this.terminalID!=null &&
              this.terminalID.equals(other.getTerminalID()))) &&
            ((this.transactionDate==null && other.getTransactionDate()==null) || 
             (this.transactionDate!=null &&
              this.transactionDate.equals(other.getTransactionDate()))) &&
            ((this.companyCode==null && other.getCompanyCode()==null) || 
             (this.companyCode!=null &&
              this.companyCode.equals(other.getCompanyCode()))) &&
            ((this.customerKey1==null && other.getCustomerKey1()==null) || 
             (this.customerKey1!=null &&
              this.customerKey1.equals(other.getCustomerKey1()))) &&
            ((this.customerKey2==null && other.getCustomerKey2()==null) || 
             (this.customerKey2!=null &&
              this.customerKey2.equals(other.getCustomerKey2()))) &&
            ((this.customerKey3==null && other.getCustomerKey3()==null) || 
             (this.customerKey3!=null &&
              this.customerKey3.equals(other.getCustomerKey3()))) &&
            ((this.billDetailList==null && other.getBillDetailList()==null) || 
             (this.billDetailList!=null &&
              java.util.Arrays.equals(this.billDetailList, other.getBillDetailList()))) &&
            ((this.currency==null && other.getCurrency()==null) || 
             (this.currency!=null &&
              this.currency.equals(other.getCurrency()))) &&
            ((this.amount==null && other.getAmount()==null) || 
             (this.amount!=null &&
              this.amount.equals(other.getAmount()))) &&
            ((this.fee==null && other.getFee()==null) || 
             (this.fee!=null &&
              this.fee.equals(other.getFee()))) &&
            ((this.paidAmount==null && other.getPaidAmount()==null) || 
             (this.paidAmount!=null &&
              this.paidAmount.equals(other.getPaidAmount()))) &&
            ((this.customerName==null && other.getCustomerName()==null) || 
             (this.customerName!=null &&
              this.customerName.equals(other.getCustomerName()))) &&
            ((this.additionalData1==null && other.getAdditionalData1()==null) || 
             (this.additionalData1!=null &&
              this.additionalData1.equals(other.getAdditionalData1()))) &&
            ((this.additionalData2==null && other.getAdditionalData2()==null) || 
             (this.additionalData2!=null &&
              this.additionalData2.equals(other.getAdditionalData2()))) &&
            ((this.additionalData3==null && other.getAdditionalData3()==null) || 
             (this.additionalData3!=null &&
              this.additionalData3.equals(other.getAdditionalData3()))) &&
            ((this.additionalData4==null && other.getAdditionalData4()==null) || 
             (this.additionalData4!=null &&
              this.additionalData4.equals(other.getAdditionalData4()))) &&
            ((this.flagPayment==null && other.getFlagPayment()==null) || 
             (this.flagPayment!=null &&
              this.flagPayment.equals(other.getFlagPayment()))) &&
            ((this.responseCode==null && other.getResponseCode()==null) || 
             (this.responseCode!=null &&
              this.responseCode.equals(other.getResponseCode()))) &&
            ((this.responseDescription==null && other.getResponseDescription()==null) || 
             (this.responseDescription!=null &&
              this.responseDescription.equals(other.getResponseDescription())));
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
        if (getTransactionID() != null) {
            _hashCode += getTransactionID().hashCode();
        }
        if (getChannelID() != null) {
            _hashCode += getChannelID().hashCode();
        }
        if (getTerminalID() != null) {
            _hashCode += getTerminalID().hashCode();
        }
        if (getTransactionDate() != null) {
            _hashCode += getTransactionDate().hashCode();
        }
        if (getCompanyCode() != null) {
            _hashCode += getCompanyCode().hashCode();
        }
        if (getCustomerKey1() != null) {
            _hashCode += getCustomerKey1().hashCode();
        }
        if (getCustomerKey2() != null) {
            _hashCode += getCustomerKey2().hashCode();
        }
        if (getCustomerKey3() != null) {
            _hashCode += getCustomerKey3().hashCode();
        }
        if (getBillDetailList() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getBillDetailList());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getBillDetailList(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getCurrency() != null) {
            _hashCode += getCurrency().hashCode();
        }
        if (getAmount() != null) {
            _hashCode += getAmount().hashCode();
        }
        if (getFee() != null) {
            _hashCode += getFee().hashCode();
        }
        if (getPaidAmount() != null) {
            _hashCode += getPaidAmount().hashCode();
        }
        if (getCustomerName() != null) {
            _hashCode += getCustomerName().hashCode();
        }
        if (getAdditionalData1() != null) {
            _hashCode += getAdditionalData1().hashCode();
        }
        if (getAdditionalData2() != null) {
            _hashCode += getAdditionalData2().hashCode();
        }
        if (getAdditionalData3() != null) {
            _hashCode += getAdditionalData3().hashCode();
        }
        if (getAdditionalData4() != null) {
            _hashCode += getAdditionalData4().hashCode();
        }
        if (getFlagPayment() != null) {
            _hashCode += getFlagPayment().hashCode();
        }
        if (getResponseCode() != null) {
            _hashCode += getResponseCode().hashCode();
        }
        if (getResponseDescription() != null) {
            _hashCode += getResponseDescription().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(InquiryRs.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "InquiryRs"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("transactionID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "TransactionID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("channelID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "ChannelID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("terminalID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "TerminalID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("transactionDate");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "TransactionDate"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("companyCode");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "CompanyCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("customerKey1");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "CustomerKey1"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("customerKey2");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "CustomerKey2"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("customerKey3");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "CustomerKey3"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billDetailList");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "BillDetailList"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "ArrayOfBillDetail"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("currency");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "Currency"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("amount");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "Amount"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("fee");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "Fee"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("paidAmount");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "PaidAmount"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("customerName");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "CustomerName"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("additionalData1");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "AdditionalData1"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("additionalData2");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "AdditionalData2"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("additionalData3");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "AdditionalData3"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("additionalData4");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "AdditionalData4"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("flagPayment");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "FlagPayment"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("responseCode");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "ResponseCode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("responseDescription");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "ResponseDescription"));
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
