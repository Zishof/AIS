/**
 * BillPaymentServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package CIMB3rdParty.BillPaymentWS;

public class BillPaymentServiceLocator extends org.apache.axis.client.Service implements CIMB3rdParty.BillPaymentWS.BillPaymentService {

    public BillPaymentServiceLocator() {
    }


    public BillPaymentServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public BillPaymentServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for BillPaymentServiceSoap
    private java.lang.String BillPaymentServiceSoap_address = "http://CIMB3rdParty/BillPaymentWS/BillPaymentService";

    public java.lang.String getBillPaymentServiceSoapAddress() {
        return BillPaymentServiceSoap_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String BillPaymentServiceSoapWSDDServiceName = "BillPaymentServiceSoap";

    public java.lang.String getBillPaymentServiceSoapWSDDServiceName() {
        return BillPaymentServiceSoapWSDDServiceName;
    }

    public void setBillPaymentServiceSoapWSDDServiceName(java.lang.String name) {
        BillPaymentServiceSoapWSDDServiceName = name;
    }

    public CIMB3rdParty.BillPaymentWS.BillPaymentServiceSoap getBillPaymentServiceSoap() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(BillPaymentServiceSoap_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getBillPaymentServiceSoap(endpoint);
    }

    public CIMB3rdParty.BillPaymentWS.BillPaymentServiceSoap getBillPaymentServiceSoap(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            CIMB3rdParty.BillPaymentWS.BillPaymentServiceSoapStub _stub = new CIMB3rdParty.BillPaymentWS.BillPaymentServiceSoapStub(portAddress, this);
            _stub.setPortName(getBillPaymentServiceSoapWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setBillPaymentServiceSoapEndpointAddress(java.lang.String address) {
        BillPaymentServiceSoap_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (CIMB3rdParty.BillPaymentWS.BillPaymentServiceSoap.class.isAssignableFrom(serviceEndpointInterface)) {
                CIMB3rdParty.BillPaymentWS.BillPaymentServiceSoapStub _stub = new CIMB3rdParty.BillPaymentWS.BillPaymentServiceSoapStub(new java.net.URL(BillPaymentServiceSoap_address), this);
                _stub.setPortName(getBillPaymentServiceSoapWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("BillPaymentServiceSoap".equals(inputPortName)) {
            return getBillPaymentServiceSoap();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "BillPaymentService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://CIMB3rdParty/BillPaymentWS", "BillPaymentServiceSoap"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("BillPaymentServiceSoap".equals(portName)) {
            setBillPaymentServiceSoapEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
