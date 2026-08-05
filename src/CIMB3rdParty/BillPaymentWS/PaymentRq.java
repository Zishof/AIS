/**
 * PaymentRq.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package CIMB3rdParty.BillPaymentWS;

public class PaymentRq  implements java.io.Serializable {
    private java.lang.String transactionID;

    private java.lang.String channelID;

    private java.lang.String terminalID;

    private java.lang.String transactionDate;

    private java.lang.String companyCode;

    private java.lang.String customerKey1;

    private java.lang.String customerKey2;

    private java.lang.String customerKey3;

    private java.lang.String language;

    private java.lang.String currency;

    private java.lang.Integer amount;

    private java.lang.Integer fee;

    private java.lang.Integer paidAmount;

    private java.lang.String referenceNumberTransaction;

    private java.lang.String flagPaymentList;

    private java.lang.String customerName;

    private java.lang.String additionalData1;

    private java.lang.String additionalData2;

    private java.lang.String additionalData3;

    private java.lang.String additionalData4;

    public PaymentRq() {
    }

    public PaymentRq(
           java.lang.String transactionID,
           java.lang.String channelID,
           java.lang.String terminalID,
           java.lang.String transactionDate,
           java.lang.String companyCode,
           java.lang.String customerKey1,
           java.lang.String customerKey2,
           java.lang.String customerKey3,
           java.lang.String language,
           java.lang.String currency,
           java.lang.Integer amount,
           java.lang.Integer fee,
           java.lang.Integer paidAmount,
           java.lang.String referenceNumberTransaction,
           java.lang.String flagPaymentList,
           java.lang.String customerName,
           java.lang.String additionalData1,
           java.lang.String additionalData2,
           java.lang.String additionalData3,
           java.lang.String additionalData4) {
           this.transactionID = transactionID;
           this.channelID = channelID;
           this.terminalID = terminalID;
           this.transactionDate = transactionDate;
           this.companyCode = companyCode;
           this.customerKey1 = customerKey1;
           this.customerKey2 = customerKey2;
           this.customerKey3 = customerKey3;
           this.language = language;
           this.currency = currency;
           this.amount = amount;
           this.fee = fee;
           this.paidAmount = paidAmount;
           this.referenceNumberTransaction = referenceNumberTransaction;
           this.flagPaymentList = flagPaymentList;
           this.customerName = customerName;
           this.additionalData1 = additionalData1;
           this.additionalData2 = additionalData2;
           this.additionalData3 = additionalData3;
           this.additionalData4 = additionalData4;
    }


    /**
     * Gets the transactionID value for this PaymentRq.
     * 
     * @return transactionID
     */
    public java.lang.String getTransactionID() {
        return transactionID;
    }


    /**
     * Sets the transactionID value for this PaymentRq.
     * 
     * @param transactionID
     */
    public void setTransactionID(java.lang.String transactionID) {
        this.transactionID = transactionID;
    }


    /**
     * Gets the channelID value for this PaymentRq.
     * 
     * @return channelID
     */
    public java.lang.String getChannelID() {
        return channelID;
    }


    /**
     * Sets the channelID value for this PaymentRq.
     * 
     * @param channelID
     */
    public void setChannelID(java.lang.String channelID) {
        this.channelID = channelID;
    }


    /**
     * Gets the terminalID value for this PaymentRq.
     * 
     * @return terminalID
     */
    public java.lang.String getTerminalID() {
        return terminalID;
    }


    /**
     * Sets the terminalID value for this PaymentRq.
     * 
     * @param terminalID
     */
    public void setTerminalID(java.lang.String terminalID) {
        this.terminalID = terminalID;
    }


    /**
     * Gets the transactionDate value for this PaymentRq.
     * 
     * @return transactionDate
     */
    public java.lang.String getTransactionDate() {
        return transactionDate;
    }


    /**
     * Sets the transactionDate value for this PaymentRq.
     * 
     * @param transactionDate
     */
    public void setTransactionDate(java.lang.String transactionDate) {
        this.transactionDate = transactionDate;
    }


    /**
     * Gets the companyCode value for this PaymentRq.
     * 
     * @return companyCode
     */
    public java.lang.String getCompanyCode() {
        return companyCode;
    }


    /**
     * Sets the companyCode value for this PaymentRq.
     * 
     * @param companyCode
     */
    public void setCompanyCode(java.lang.String companyCode) {
        this.companyCode = companyCode;
    }


    /**
     * Gets the customerKey1 value for this PaymentRq.
     * 
     * @return customerKey1
     */
    public java.lang.String getCustomerKey1() {
        return customerKey1;
    }


    /**
     * Sets the customerKey1 value for this PaymentRq.
     * 
     * @param customerKey1
     */
    public void setCustomerKey1(java.lang.String customerKey1) {
        this.customerKey1 = customerKey1;
    }


    /**
     * Gets the customerKey2 value for this PaymentRq.
     * 
     * @return customerKey2
     */
    public java.lang.String getCustomerKey2() {
        return customerKey2;
    }


    /**
     * Sets the customerKey2 value for this PaymentRq.
     * 
     * @param customerKey2
     */
    public void setCustomerKey2(java.lang.String customerKey2) {
        this.customerKey2 = customerKey2;
    }


    /**
     * Gets the customerKey3 value for this PaymentRq.
     * 
     * @return customerKey3
     */
    public java.lang.String getCustomerKey3() {
        return customerKey3;
    }


    /**
     * Sets the customerKey3 value for this PaymentRq.
     * 
     * @param customerKey3
     */
    public void setCustomerKey3(java.lang.String customerKey3) {
        this.customerKey3 = customerKey3;
    }


    /**
     * Gets the language value for this PaymentRq.
     * 
     * @return language
     */
    public java.lang.String getLanguage() {
        return language;
    }


    /**
     * Sets the language value for this PaymentRq.
     * 
     * @param language
     */
    public void setLanguage(java.lang.String language) {
        this.language = language;
    }


    /**
     * Gets the currency value for this PaymentRq.
     * 
     * @return currency
     */
    public java.lang.String getCurrency() {
        return currency;
    }


    /**
     * Sets the currency value for this PaymentRq.
     * 
     * @param currency
     */
    public void setCurrency(java.lang.String currency) {
        this.currency = currency;
    }


    /**
     * Gets the amount value for this PaymentRq.
     * 
     * @return amount
     */
    public java.lang.Integer getAmount() {
        return amount;
    }


    /**
     * Sets the amount value for this PaymentRq.
     * 
     * @param amount
     */
    public void setAmount(java.lang.Integer amount) {
        this.amount = amount;
    }


    /**
     * Gets the fee value for this PaymentRq.
     * 
     * @return fee
     */
    public java.lang.Integer getFee() {
        return fee;
    }


    /**
     * Sets the fee value for this PaymentRq.
     * 
     * @param fee
     */
    public void setFee(java.lang.Integer fee) {
        this.fee = fee;
    }


    /**
     * Gets the paidAmount value for this PaymentRq.
     * 
     * @return paidAmount
     */
    public java.lang.Integer getPaidAmount() {
        return paidAmount;
    }


    /**
     * Sets the paidAmount value for this PaymentRq.
     * 
     * @param paidAmount
     */
    public void setPaidAmount(java.lang.Integer paidAmount) {
        this.paidAmount = paidAmount;
    }


    /**
     * Gets the referenceNumberTransaction value for this PaymentRq.
     * 
     * @return referenceNumberTransaction
     */
    public java.lang.String getReferenceNumberTransaction() {
        return referenceNumberTransaction;
    }


    /**
     * Sets the referenceNumberTransaction value for this PaymentRq.
     * 
     * @param referenceNumberTransaction
     */
    public void setReferenceNumberTransaction(java.lang.String referenceNumberTransaction) {
        this.referenceNumberTransaction = referenceNumberTransaction;
    }


    /**
     * Gets the flagPaymentList value for this PaymentRq.
     * 
     * @return flagPaymentList
     */
    public java.lang.String getFlagPaymentList() {
        return flagPaymentList;
    }


    /**
     * Sets the flagPaymentList value for this PaymentRq.
     * 
     * @param flagPaymentList
     */
    public void setFlagPaymentList(java.lang.String flagPaymentList) {
        this.flagPaymentList = flagPaymentList;
    }


    /**
     * Gets the customerName value for this PaymentRq.
     * 
     * @return customerName
     */
    public java.lang.String getCustomerName() {
        return customerName;
    }


    /**
     * Sets the customerName value for this PaymentRq.
     * 
     * @param customerName
     */
    public void setCustomerName(java.lang.String customerName) {
        this.customerName = customerName;
    }


    /**
     * Gets the additionalData1 value for this PaymentRq.
     * 
     * @return additionalData1
     */
    public java.lang.String getAdditionalData1() {
        return additionalData1;
    }


    /**
     * Sets the additionalData1 value for this PaymentRq.
     * 
     * @param additionalData1
     */
    public void setAdditionalData1(java.lang.String additionalData1) {
        this.additionalData1 = additionalData1;
    }


    /**
     * Gets the additionalData2 value for this PaymentRq.
     * 
     * @return additionalData2
     */
    public java.lang.String getAdditionalData2() {
        return additionalData2;
    }


    /**
     * Sets the additionalData2 value for this PaymentRq.
     * 
     * @param additionalData2
     */
    public void setAdditionalData2(java.lang.String additionalData2) {
        this.additionalData2 = additionalData2;
    }


    /**
     * Gets the additionalData3 value for this PaymentRq.
     * 
     * @return additionalData3
     */
    public java.lang.String getAdditionalData3() {
        return additionalData3;
    }


    /**
     * Sets the additionalData3 value for this PaymentRq.
     * 
     * @param additionalData3
     */
    public void setAdditionalData3(java.lang.String additionalData3) {
        this.additionalData3 = additionalData3;
    }


    /**
     * Gets the additionalData4 value for this PaymentRq.
     * 
     * @return additionalData4
     */
    public java.lang.String getAdditionalData4() {
        return additionalData4;
    }


    /**
     * Sets the additionalData4 value for this PaymentRq.
     * 
     * @param additionalData4
     */
    public void setAdditionalData4(java.lang.String additionalData4) {
        this.additionalData4 = additionalData4;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof PaymentRq)) return false;
        PaymentRq other = (PaymentRq) obj;
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
            ((this.language==null && other.getLanguage()==null) || 
             (this.language!=null &&
              this.language.equals(other.getLanguage()))) &&
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
            ((this.referenceNumberTransaction==null && other.getReferenceNumberTransaction()==null) || 
             (this.referenceNumberTransaction!=null &&
              this.referenceNumberTransaction.equals(other.getReferenceNumberTransaction()))) &&
            ((this.flagPaymentList==null && other.getFlagPaymentList()==null) || 
             (this.flagPaymentList!=null &&
              this.flagPaymentList.equals(other.getFlagPaymentList()))) &&
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
              this.additionalData4.equals(other.getAdditionalData4())));
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
        if (getLanguage() != null) {
            _hashCode += getLanguage().hashCode();
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
        if (getReferenceNumberTransaction() != null) {
            _hashCode += getReferenceNumberTransaction().hashCode();
        }
        if (getFlagPaymentList() != null) {
            _hashCode += getFlagPaymentList().hashCode();
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
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(PaymentRq.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "PaymentRq"));
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
        elemField.setFieldName("language");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "Language"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
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
        elemField.setFieldName("referenceNumberTransaction");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "ReferenceNumberTransaction"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("flagPaymentList");
        elemField.setXmlName(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "FlagPaymentList"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
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
