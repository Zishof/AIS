/**
 * BillPaymentServiceSoapSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package CIMB3rdParty.BillPaymentWS;

public class BillPaymentServiceSoapSkeleton implements CIMB3rdParty.BillPaymentWS.BillPaymentServiceSoap, org.apache.axis.wsdl.Skeleton {
    private CIMB3rdParty.BillPaymentWS.BillPaymentServiceSoap impl;
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
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "CIMB3rdParty_InquiryRq"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "CIMB3rdParty_InquiryRq"), CIMB3rdParty.BillPaymentWS.CIMB3RdParty_InquiryRq.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("inquiry", _params, new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "CIMB3rdParty_InquiryRs"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "CIMB3rdParty_InquiryRs"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "inquiry"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("inquiry") == null) {
            _myOperations.put("inquiry", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("inquiry")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "CIMB3rdParty_PaymentRq"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "CIMB3rdParty_PaymentRq"), CIMB3rdParty.BillPaymentWS.CIMB3RdParty_PaymentRq.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("payment", _params, new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "CIMB3rdParty_PaymentRs"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "CIMB3rdParty_PaymentRs"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "payment"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("payment") == null) {
            _myOperations.put("payment", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("payment")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "CIMB3rdParty_EchoRq"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", ">CIMB3rdParty_EchoRq"), CIMB3rdParty.BillPaymentWS.CIMB3RdParty_EchoRq.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("echoTest", _params, new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "CIMB3rdParty_EchoRs"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", ">CIMB3rdParty_EchoRs"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "echoTest"));
        _oper.setSoapAction("");
        _myOperationsList.add(_oper);
        if (_myOperations.get("echoTest") == null) {
            _myOperations.put("echoTest", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("echoTest")).add(_oper);
    }

    public BillPaymentServiceSoapSkeleton() {
        this.impl = new CIMB3rdParty.BillPaymentWS.BillPaymentServiceSoapImpl();
    }

    public BillPaymentServiceSoapSkeleton(CIMB3rdParty.BillPaymentWS.BillPaymentServiceSoap impl) {
        this.impl = impl;
    }
    public CIMB3rdParty.BillPaymentWS.CIMB3RdParty_InquiryRs inquiry(CIMB3rdParty.BillPaymentWS.CIMB3RdParty_InquiryRq parameters) throws java.rmi.RemoteException
    {
        CIMB3rdParty.BillPaymentWS.CIMB3RdParty_InquiryRs ret = impl.inquiry(parameters);
        return ret;
    }

    public CIMB3rdParty.BillPaymentWS.CIMB3RdParty_PaymentRs payment(CIMB3rdParty.BillPaymentWS.CIMB3RdParty_PaymentRq parameters) throws java.rmi.RemoteException
    {
        CIMB3rdParty.BillPaymentWS.CIMB3RdParty_PaymentRs ret = impl.payment(parameters);
        return ret;
    }

    public CIMB3rdParty.BillPaymentWS.CIMB3RdParty_EchoRs echoTest(CIMB3rdParty.BillPaymentWS.CIMB3RdParty_EchoRq parameters) throws java.rmi.RemoteException
    {
        CIMB3rdParty.BillPaymentWS.CIMB3RdParty_EchoRs ret = impl.echoTest(parameters);
        return ret;
    }

}
