/**
 * BillPaymentServiceSoapSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ws.billpayment.h2h.bankmandiri;

public class BillPaymentServiceSoapSkeleton implements ws.billpayment.h2h.bankmandiri.BillPaymentServiceSoap, org.apache.axis.wsdl.Skeleton {
    private ws.billpayment.h2h.bankmandiri.BillPaymentServiceSoap impl;
    private static java.util.Map _myOperations = new java.util.Hashtable();
    private static java.util.Collection _myOperationsList = new java.util.ArrayList();

    /**
    * Returns List of OperationDesc objects with this name
    */
    public static java.util.List getOperationDescByName(java.lang.String methodName) {
        return (java.util.List)_myOperations.get(methodName);
    }

    /**
    * Returns Collection of OperationDescs
    */
    public static java.util.Collection getOperationDescs() {
        return _myOperationsList;
    }

    static {
        org.apache.axis.description.OperationDesc _oper;
        org.apache.axis.description.FaultDesc _fault;
        org.apache.axis.description.ParameterDesc [] _params;
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "ReversalRequest"), ws.billpayment.h2h.bankmandiri.ReversalRequest.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("reverse", _params, new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "reverseResult"));
        _oper.setReturnType(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "ReversalResponse"));
        _oper.setElementQName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "reverse"));
        _oper.setSoapAction("bankmandiri.h2h.billpayment.ws/reverse");
        _myOperationsList.add(_oper);
        if (_myOperations.get("reverse") == null) {
            _myOperations.put("reverse", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("reverse")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "PaymentRequest"), ws.billpayment.h2h.bankmandiri.PaymentRequest.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("payment", _params, new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "paymentResult"));
        _oper.setReturnType(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "PaymentResponse"));
        _oper.setElementQName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "payment"));
        _oper.setSoapAction("bankmandiri.h2h.billpayment.ws/payment");
        _myOperationsList.add(_oper);
        if (_myOperations.get("payment") == null) {
            _myOperations.put("payment", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("payment")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "InquiryRequest"), ws.billpayment.h2h.bankmandiri.InquiryRequest.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("inquiry", _params, new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "inquiryResult"));
        _oper.setReturnType(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "InquiryResponse"));
        _oper.setElementQName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "inquiry"));
        _oper.setSoapAction("bankmandiri.h2h.billpayment.ws/inquiry");
        _myOperationsList.add(_oper);
        if (_myOperations.get("inquiry") == null) {
            _myOperations.put("inquiry", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("inquiry")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "tx"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("echoTest", _params, new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "echoTestResult"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        _oper.setElementQName(new javax.xml.namespace.QName("bankmandiri.h2h.billpayment.ws", "echoTest"));
        _oper.setSoapAction("bankmandiri.h2h.billpayment.ws/echoTest");
        _myOperationsList.add(_oper);
        if (_myOperations.get("echoTest") == null) {
            _myOperations.put("echoTest", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("echoTest")).add(_oper);
    }

    public BillPaymentServiceSoapSkeleton() {
        this.impl = new ws.billpayment.h2h.bankmandiri.BillPaymentServiceSoapImpl();
    }

    public BillPaymentServiceSoapSkeleton(ws.billpayment.h2h.bankmandiri.BillPaymentServiceSoap impl) {
        this.impl = impl;
    }
    public ws.billpayment.h2h.bankmandiri.ReversalResponse reverse(ws.billpayment.h2h.bankmandiri.ReversalRequest request) throws java.rmi.RemoteException
    {
        ws.billpayment.h2h.bankmandiri.ReversalResponse ret = impl.reverse(request);
        return ret;
    }

    public ws.billpayment.h2h.bankmandiri.PaymentResponse payment(ws.billpayment.h2h.bankmandiri.PaymentRequest request) throws java.rmi.RemoteException
    {
        ws.billpayment.h2h.bankmandiri.PaymentResponse ret = impl.payment(request);
        return ret;
    }

    public ws.billpayment.h2h.bankmandiri.InquiryResponse inquiry(ws.billpayment.h2h.bankmandiri.InquiryRequest request) throws java.rmi.RemoteException
    {
        ws.billpayment.h2h.bankmandiri.InquiryResponse ret = impl.inquiry(request);
        return ret;
    }

    public java.lang.String echoTest(java.lang.String tx) throws java.rmi.RemoteException
    {
        java.lang.String ret = impl.echoTest(tx);
        return ret;
    }

}
