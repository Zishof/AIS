/**
 * PaymentResponse.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ws.billpayment.h2h.bankmandiri;

/**
 * DTO respons SOAP bill-payment untuk payment response. Field kelas ini membawa status dan data
 * protokol yang diserialisasi oleh Apache Axis; pembentukan keputusan bisnis tetap milik
 * implementasi layanan.
 *
 * <p><b>Batas tanggung jawab:</b> tipe ini hanya memodelkan data pesan SOAP. Interface
 * {@link java.io.Serializable} (dan {@code Comparable}, bila ada) adalah kebutuhan binding/collection, bukan
 * tempat implementasi transaksi. Validasi, autentikasi, dan aturan pembayaran wajib tetap berada pada endpoint
 * atau service domain agar DTO wire tidak menjadi sumber aturan yang tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code java.lang.String billInfo1}, {@code
 * java.lang.String billInfo2}, {@code java.lang.String billInfo3}, {@code java.lang.String billInfo4}, {@code
 * java.lang.String billInfo5}, {@code java.lang.String billInfo6}, {@code java.lang.String billInfo7}, {@code
 * java.lang.String billInfo8}; pembacaan/pencarian ({@code getBillInfo1()}, {@code getBillInfo2()}, {@code
 * getBillInfo3()}, {@code getBillInfo4()}, {@code getBillInfo5()}, {@code getBillInfo6()}); mutasi data ({@code
 * setBillInfo1()}, {@code setBillInfo2()}, {@code setBillInfo3()}, {@code setBillInfo4()}, {@code
 * setBillInfo5()}, {@code setBillInfo6()}); operasi domain lain ({@code equals()}, {@code hashCode()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor hanya membaca atau mengganti state DTO. Tidak ada I/O, transaksi,
 * autentikasi, maupun validasi bisnis; nilai wajib, format, dan makna status mengikuti WSDL serta implementasi
 * endpoint. Jangan menambahkan aturan domain ke binding ini.</p>
 */
public class PaymentResponse  implements java.io.Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 5358677838679063874L;

	private java.lang.String billInfo1;

    private java.lang.String billInfo2;

    private java.lang.String billInfo3;

    private java.lang.String billInfo4;

    private java.lang.String billInfo5;

    private java.lang.String billInfo6;

    private java.lang.String billInfo7;

    private java.lang.String billInfo8;

    private java.lang.String billInfo9;

    private java.lang.String billInfo10;

    private java.lang.String billInfo11;

    private java.lang.String billInfo12;

    private java.lang.String billInfo13;

    private java.lang.String billInfo14;

    private java.lang.String billInfo15;

    private java.lang.String billInfo16;

    private java.lang.String billInfo17;

    private java.lang.String billInfo18;

    private java.lang.String billInfo19;

    private java.lang.String billInfo21;

    private java.lang.String billInfo22;

    private java.lang.String billInfo23;

    private java.lang.String billInfo24;

    private java.lang.String billInfo25;

    private ws.billpayment.h2h.bankmandiri.Status status;

    public PaymentResponse() {
    }

    public PaymentResponse(
           java.lang.String billInfo1,
           java.lang.String billInfo2,
           java.lang.String billInfo3,
           java.lang.String billInfo4,
           java.lang.String billInfo5,
           java.lang.String billInfo6,
           java.lang.String billInfo7,
           java.lang.String billInfo8,
           java.lang.String billInfo9,
           java.lang.String billInfo10,
           java.lang.String billInfo11,
           java.lang.String billInfo12,
           java.lang.String billInfo13,
           java.lang.String billInfo14,
           java.lang.String billInfo15,
           java.lang.String billInfo16,
           java.lang.String billInfo17,
           java.lang.String billInfo18,
           java.lang.String billInfo19,
           java.lang.String billInfo21,
           java.lang.String billInfo22,
           java.lang.String billInfo23,
           java.lang.String billInfo24,
           java.lang.String billInfo25,
           ws.billpayment.h2h.bankmandiri.Status status) {
           this.billInfo1 = billInfo1;
           this.billInfo2 = billInfo2;
           this.billInfo3 = billInfo3;
           this.billInfo4 = billInfo4;
           this.billInfo5 = billInfo5;
           this.billInfo6 = billInfo6;
           this.billInfo7 = billInfo7;
           this.billInfo8 = billInfo8;
           this.billInfo9 = billInfo9;
           this.billInfo10 = billInfo10;
           this.billInfo11 = billInfo11;
           this.billInfo12 = billInfo12;
           this.billInfo13 = billInfo13;
           this.billInfo14 = billInfo14;
           this.billInfo15 = billInfo15;
           this.billInfo16 = billInfo16;
           this.billInfo17 = billInfo17;
           this.billInfo18 = billInfo18;
           this.billInfo19 = billInfo19;
           this.billInfo21 = billInfo21;
           this.billInfo22 = billInfo22;
           this.billInfo23 = billInfo23;
           this.billInfo24 = billInfo24;
           this.billInfo25 = billInfo25;
           this.status = status;
    }


    /**
     * Gets the billInfo1 value for this PaymentResponse.
     * 
     * @return billInfo1
     */
    public java.lang.String getBillInfo1() {
        return billInfo1;
    }


    /**
     * Sets the billInfo1 value for this PaymentResponse.
     * 
     * @param billInfo1
     */
    public void setBillInfo1(java.lang.String billInfo1) {
        this.billInfo1 = billInfo1;
    }


    /**
     * Gets the billInfo2 value for this PaymentResponse.
     * 
     * @return billInfo2
     */
    public java.lang.String getBillInfo2() {
        return billInfo2;
    }


    /**
     * Sets the billInfo2 value for this PaymentResponse.
     * 
     * @param billInfo2
     */
    public void setBillInfo2(java.lang.String billInfo2) {
        this.billInfo2 = billInfo2;
    }


    /**
     * Gets the billInfo3 value for this PaymentResponse.
     * 
     * @return billInfo3
     */
    public java.lang.String getBillInfo3() {
        return billInfo3;
    }


    /**
     * Sets the billInfo3 value for this PaymentResponse.
     * 
     * @param billInfo3
     */
    public void setBillInfo3(java.lang.String billInfo3) {
        this.billInfo3 = billInfo3;
    }


    /**
     * Gets the billInfo4 value for this PaymentResponse.
     * 
     * @return billInfo4
     */
    public java.lang.String getBillInfo4() {
        return billInfo4;
    }


    /**
     * Sets the billInfo4 value for this PaymentResponse.
     * 
     * @param billInfo4
     */
    public void setBillInfo4(java.lang.String billInfo4) {
        this.billInfo4 = billInfo4;
    }


    /**
     * Gets the billInfo5 value for this PaymentResponse.
     * 
     * @return billInfo5
     */
    public java.lang.String getBillInfo5() {
        return billInfo5;
    }


    /**
     * Sets the billInfo5 value for this PaymentResponse.
     * 
     * @param billInfo5
     */
    public void setBillInfo5(java.lang.String billInfo5) {
        this.billInfo5 = billInfo5;
    }


    /**
     * Gets the billInfo6 value for this PaymentResponse.
     * 
     * @return billInfo6
     */
    public java.lang.String getBillInfo6() {
        return billInfo6;
    }


    /**
     * Sets the billInfo6 value for this PaymentResponse.
     * 
     * @param billInfo6
     */
    public void setBillInfo6(java.lang.String billInfo6) {
        this.billInfo6 = billInfo6;
    }


    /**
     * Gets the billInfo7 value for this PaymentResponse.
     * 
     * @return billInfo7
     */
    public java.lang.String getBillInfo7() {
        return billInfo7;
    }


    /**
     * Sets the billInfo7 value for this PaymentResponse.
     * 
     * @param billInfo7
     */
    public void setBillInfo7(java.lang.String billInfo7) {
        this.billInfo7 = billInfo7;
    }


    /**
     * Gets the billInfo8 value for this PaymentResponse.
     * 
     * @return billInfo8
     */
    public java.lang.String getBillInfo8() {
        return billInfo8;
    }


    /**
     * Sets the billInfo8 value for this PaymentResponse.
     * 
     * @param billInfo8
     */
    public void setBillInfo8(java.lang.String billInfo8) {
        this.billInfo8 = billInfo8;
    }


    /**
     * Gets the billInfo9 value for this PaymentResponse.
     * 
     * @return billInfo9
     */
    public java.lang.String getBillInfo9() {
        return billInfo9;
    }


    /**
     * Sets the billInfo9 value for this PaymentResponse.
     * 
     * @param billInfo9
     */
    public void setBillInfo9(java.lang.String billInfo9) {
        this.billInfo9 = billInfo9;
    }


    /**
     * Gets the billInfo10 value for this PaymentResponse.
     * 
     * @return billInfo10
     */
    public java.lang.String getBillInfo10() {
        return billInfo10;
    }


    /**
     * Sets the billInfo10 value for this PaymentResponse.
     * 
     * @param billInfo10
     */
    public void setBillInfo10(java.lang.String billInfo10) {
        this.billInfo10 = billInfo10;
    }


    /**
     * Gets the billInfo11 value for this PaymentResponse.
     * 
     * @return billInfo11
     */
    public java.lang.String getBillInfo11() {
        return billInfo11;
    }


    /**
     * Sets the billInfo11 value for this PaymentResponse.
     * 
     * @param billInfo11
     */
    public void setBillInfo11(java.lang.String billInfo11) {
        this.billInfo11 = billInfo11;
    }


    /**
     * Gets the billInfo12 value for this PaymentResponse.
     * 
     * @return billInfo12
     */
    public java.lang.String getBillInfo12() {
        return billInfo12;
    }


    /**
     * Sets the billInfo12 value for this PaymentResponse.
     * 
     * @param billInfo12
     */
    public void setBillInfo12(java.lang.String billInfo12) {
        this.billInfo12 = billInfo12;
    }


    /**
     * Gets the billInfo13 value for this PaymentResponse.
     * 
     * @return billInfo13
     */
    public java.lang.String getBillInfo13() {
        return billInfo13;
    }


    /**
     * Sets the billInfo13 value for this PaymentResponse.
     * 
     * @param billInfo13
     */
    public void setBillInfo13(java.lang.String billInfo13) {
        this.billInfo13 = billInfo13;
    }


    /**
     * Gets the billInfo14 value for this PaymentResponse.
     * 
     * @return billInfo14
     */
    public java.lang.String getBillInfo14() {
        return billInfo14;
    }


    /**
     * Sets the billInfo14 value for this PaymentResponse.
     * 
     * @param billInfo14
     */
    public void setBillInfo14(java.lang.String billInfo14) {
        this.billInfo14 = billInfo14;
    }


    /**
     * Gets the billInfo15 value for this PaymentResponse.
     * 
     * @return billInfo15
     */
    public java.lang.String getBillInfo15() {
        return billInfo15;
    }


    /**
     * Sets the billInfo15 value for this PaymentResponse.
     * 
     * @param billInfo15
     */
    public void setBillInfo15(java.lang.String billInfo15) {
        this.billInfo15 = billInfo15;
    }


    /**
     * Gets the billInfo16 value for this PaymentResponse.
     * 
     * @return billInfo16
     */
    public java.lang.String getBillInfo16() {
        return billInfo16;
    }


    /**
     * Sets the billInfo16 value for this PaymentResponse.
     * 
     * @param billInfo16
     */
    public void setBillInfo16(java.lang.String billInfo16) {
        this.billInfo16 = billInfo16;
    }


    /**
     * Gets the billInfo17 value for this PaymentResponse.
     * 
     * @return billInfo17
     */
    public java.lang.String getBillInfo17() {
        return billInfo17;
    }


    /**
     * Sets the billInfo17 value for this PaymentResponse.
     * 
     * @param billInfo17
     */
    public void setBillInfo17(java.lang.String billInfo17) {
        this.billInfo17 = billInfo17;
    }


    /**
     * Gets the billInfo18 value for this PaymentResponse.
     * 
     * @return billInfo18
     */
    public java.lang.String getBillInfo18() {
        return billInfo18;
    }


    /**
     * Sets the billInfo18 value for this PaymentResponse.
     * 
     * @param billInfo18
     */
    public void setBillInfo18(java.lang.String billInfo18) {
        this.billInfo18 = billInfo18;
    }


    /**
     * Gets the billInfo19 value for this PaymentResponse.
     * 
     * @return billInfo19
     */
    public java.lang.String getBillInfo19() {
        return billInfo19;
    }


    /**
     * Sets the billInfo19 value for this PaymentResponse.
     * 
     * @param billInfo19
     */
    public void setBillInfo19(java.lang.String billInfo19) {
        this.billInfo19 = billInfo19;
    }


    /**
     * Gets the billInfo21 value for this PaymentResponse.
     * 
     * @return billInfo21
     */
    public java.lang.String getBillInfo21() {
        return billInfo21;
    }


    /**
     * Sets the billInfo21 value for this PaymentResponse.
     * 
     * @param billInfo21
     */
    public void setBillInfo21(java.lang.String billInfo21) {
        this.billInfo21 = billInfo21;
    }


    /**
     * Gets the billInfo22 value for this PaymentResponse.
     * 
     * @return billInfo22
     */
    public java.lang.String getBillInfo22() {
        return billInfo22;
    }


    /**
     * Sets the billInfo22 value for this PaymentResponse.
     * 
     * @param billInfo22
     */
    public void setBillInfo22(java.lang.String billInfo22) {
        this.billInfo22 = billInfo22;
    }


    /**
     * Gets the billInfo23 value for this PaymentResponse.
     * 
     * @return billInfo23
     */
    public java.lang.String getBillInfo23() {
        return billInfo23;
    }


    /**
     * Sets the billInfo23 value for this PaymentResponse.
     * 
     * @param billInfo23
     */
    public void setBillInfo23(java.lang.String billInfo23) {
        this.billInfo23 = billInfo23;
    }


    /**
     * Gets the billInfo24 value for this PaymentResponse.
     * 
     * @return billInfo24
     */
    public java.lang.String getBillInfo24() {
        return billInfo24;
    }


    /**
     * Sets the billInfo24 value for this PaymentResponse.
     * 
     * @param billInfo24
     */
    public void setBillInfo24(java.lang.String billInfo24) {
        this.billInfo24 = billInfo24;
    }


    /**
     * Gets the billInfo25 value for this PaymentResponse.
     * 
     * @return billInfo25
     */
    public java.lang.String getBillInfo25() {
        return billInfo25;
    }


    /**
     * Sets the billInfo25 value for this PaymentResponse.
     * 
     * @param billInfo25
     */
    public void setBillInfo25(java.lang.String billInfo25) {
        this.billInfo25 = billInfo25;
    }


    /**
     * Gets the status value for this PaymentResponse.
     * 
     * @return status
     */
    public ws.billpayment.h2h.bankmandiri.Status getStatus() {
        return status;
    }


    /**
     * Sets the status value for this PaymentResponse.
     * 
     * @param status
     */
    public void setStatus(ws.billpayment.h2h.bankmandiri.Status status) {
        this.status = status;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof PaymentResponse)) return false;
        PaymentResponse other = (PaymentResponse) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.billInfo1==null && other.getBillInfo1()==null) || 
             (this.billInfo1!=null &&
              this.billInfo1.equals(other.getBillInfo1()))) &&
            ((this.billInfo2==null && other.getBillInfo2()==null) || 
             (this.billInfo2!=null &&
              this.billInfo2.equals(other.getBillInfo2()))) &&
            ((this.billInfo3==null && other.getBillInfo3()==null) || 
             (this.billInfo3!=null &&
              this.billInfo3.equals(other.getBillInfo3()))) &&
            ((this.billInfo4==null && other.getBillInfo4()==null) || 
             (this.billInfo4!=null &&
              this.billInfo4.equals(other.getBillInfo4()))) &&
            ((this.billInfo5==null && other.getBillInfo5()==null) || 
             (this.billInfo5!=null &&
              this.billInfo5.equals(other.getBillInfo5()))) &&
            ((this.billInfo6==null && other.getBillInfo6()==null) || 
             (this.billInfo6!=null &&
              this.billInfo6.equals(other.getBillInfo6()))) &&
            ((this.billInfo7==null && other.getBillInfo7()==null) || 
             (this.billInfo7!=null &&
              this.billInfo7.equals(other.getBillInfo7()))) &&
            ((this.billInfo8==null && other.getBillInfo8()==null) || 
             (this.billInfo8!=null &&
              this.billInfo8.equals(other.getBillInfo8()))) &&
            ((this.billInfo9==null && other.getBillInfo9()==null) || 
             (this.billInfo9!=null &&
              this.billInfo9.equals(other.getBillInfo9()))) &&
            ((this.billInfo10==null && other.getBillInfo10()==null) || 
             (this.billInfo10!=null &&
              this.billInfo10.equals(other.getBillInfo10()))) &&
            ((this.billInfo11==null && other.getBillInfo11()==null) || 
             (this.billInfo11!=null &&
              this.billInfo11.equals(other.getBillInfo11()))) &&
            ((this.billInfo12==null && other.getBillInfo12()==null) || 
             (this.billInfo12!=null &&
              this.billInfo12.equals(other.getBillInfo12()))) &&
            ((this.billInfo13==null && other.getBillInfo13()==null) || 
             (this.billInfo13!=null &&
              this.billInfo13.equals(other.getBillInfo13()))) &&
            ((this.billInfo14==null && other.getBillInfo14()==null) || 
             (this.billInfo14!=null &&
              this.billInfo14.equals(other.getBillInfo14()))) &&
            ((this.billInfo15==null && other.getBillInfo15()==null) || 
             (this.billInfo15!=null &&
              this.billInfo15.equals(other.getBillInfo15()))) &&
            ((this.billInfo16==null && other.getBillInfo16()==null) || 
             (this.billInfo16!=null &&
              this.billInfo16.equals(other.getBillInfo16()))) &&
            ((this.billInfo17==null && other.getBillInfo17()==null) || 
             (this.billInfo17!=null &&
              this.billInfo17.equals(other.getBillInfo17()))) &&
            ((this.billInfo18==null && other.getBillInfo18()==null) || 
             (this.billInfo18!=null &&
              this.billInfo18.equals(other.getBillInfo18()))) &&
            ((this.billInfo19==null && other.getBillInfo19()==null) || 
             (this.billInfo19!=null &&
              this.billInfo19.equals(other.getBillInfo19()))) &&
            ((this.billInfo21==null && other.getBillInfo21()==null) || 
             (this.billInfo21!=null &&
              this.billInfo21.equals(other.getBillInfo21()))) &&
            ((this.billInfo22==null && other.getBillInfo22()==null) || 
             (this.billInfo22!=null &&
              this.billInfo22.equals(other.getBillInfo22()))) &&
            ((this.billInfo23==null && other.getBillInfo23()==null) || 
             (this.billInfo23!=null &&
              this.billInfo23.equals(other.getBillInfo23()))) &&
            ((this.billInfo24==null && other.getBillInfo24()==null) || 
             (this.billInfo24!=null &&
              this.billInfo24.equals(other.getBillInfo24()))) &&
            ((this.billInfo25==null && other.getBillInfo25()==null) || 
             (this.billInfo25!=null &&
              this.billInfo25.equals(other.getBillInfo25()))) &&
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
        if (getBillInfo1() != null) {
            _hashCode += getBillInfo1().hashCode();
        }
        if (getBillInfo2() != null) {
            _hashCode += getBillInfo2().hashCode();
        }
        if (getBillInfo3() != null) {
            _hashCode += getBillInfo3().hashCode();
        }
        if (getBillInfo4() != null) {
            _hashCode += getBillInfo4().hashCode();
        }
        if (getBillInfo5() != null) {
            _hashCode += getBillInfo5().hashCode();
        }
        if (getBillInfo6() != null) {
            _hashCode += getBillInfo6().hashCode();
        }
        if (getBillInfo7() != null) {
            _hashCode += getBillInfo7().hashCode();
        }
        if (getBillInfo8() != null) {
            _hashCode += getBillInfo8().hashCode();
        }
        if (getBillInfo9() != null) {
            _hashCode += getBillInfo9().hashCode();
        }
        if (getBillInfo10() != null) {
            _hashCode += getBillInfo10().hashCode();
        }
        if (getBillInfo11() != null) {
            _hashCode += getBillInfo11().hashCode();
        }
        if (getBillInfo12() != null) {
            _hashCode += getBillInfo12().hashCode();
        }
        if (getBillInfo13() != null) {
            _hashCode += getBillInfo13().hashCode();
        }
        if (getBillInfo14() != null) {
            _hashCode += getBillInfo14().hashCode();
        }
        if (getBillInfo15() != null) {
            _hashCode += getBillInfo15().hashCode();
        }
        if (getBillInfo16() != null) {
            _hashCode += getBillInfo16().hashCode();
        }
        if (getBillInfo17() != null) {
            _hashCode += getBillInfo17().hashCode();
        }
        if (getBillInfo18() != null) {
            _hashCode += getBillInfo18().hashCode();
        }
        if (getBillInfo19() != null) {
            _hashCode += getBillInfo19().hashCode();
        }
        if (getBillInfo21() != null) {
            _hashCode += getBillInfo21().hashCode();
        }
        if (getBillInfo22() != null) {
            _hashCode += getBillInfo22().hashCode();
        }
        if (getBillInfo23() != null) {
            _hashCode += getBillInfo23().hashCode();
        }
        if (getBillInfo24() != null) {
            _hashCode += getBillInfo24().hashCode();
        }
        if (getBillInfo25() != null) {
            _hashCode += getBillInfo25().hashCode();
        }
        if (getStatus() != null) {
            _hashCode += getStatus().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(PaymentResponse.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "PaymentResponse"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo1");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo1"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo2");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo2"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo3");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo3"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo4");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo4"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo5");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo5"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo6");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo6"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo7");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo7"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo8");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo8"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo9");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo9"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo10");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo10"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo11");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo11"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo12");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo12"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo13");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo13"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo14");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo14"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo15");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo15"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo16");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo16"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo17");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo17"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo18");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo18"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo19");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo19"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo21");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo21"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo22");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo22"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo23");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo23"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo24");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo24"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("billInfo25");
        elemField.setXmlName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "billInfo25"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
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
