/**
 * BillPaymentServiceSoap.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ws.billpayment.h2h.bankmandiri;

public interface BillPaymentServiceSoap extends java.rmi.Remote {
    public ws.billpayment.h2h.bankmandiri.ReversalResponse reverse(ws.billpayment.h2h.bankmandiri.ReversalRequest request) throws java.rmi.RemoteException;
    public ws.billpayment.h2h.bankmandiri.PaymentResponse payment(ws.billpayment.h2h.bankmandiri.PaymentRequest request) throws java.rmi.RemoteException;
    public ws.billpayment.h2h.bankmandiri.InquiryResponse inquiry(ws.billpayment.h2h.bankmandiri.InquiryRequest request) throws java.rmi.RemoteException;
    public java.lang.String echoTest(java.lang.String tx) throws java.rmi.RemoteException;
}
