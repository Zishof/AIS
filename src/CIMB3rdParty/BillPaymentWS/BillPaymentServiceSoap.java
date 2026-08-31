/**
 * BillPaymentServiceSoap.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package CIMB3rdParty.BillPaymentWS;

/**
 * Kontrak operasi SOAP bill-payment. Interface hasil WSDL2Java ini menetapkan signature wire yang
 * harus sama pada stub klien, skeleton server, dan implementasi endpoint.
 *
 * <p><b>Batas tanggung jawab:</b> tipe ini mendeklarasikan kontrak {@link java.rmi.Remote}. Implementasi konkret
 * bertanggung jawab atas transaksi, resource, error handling, dan efek samping; pemanggil sebaiknya bergantung
 * pada kontrak ini agar tidak menggandakan integrasi.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi domain lain ({@code inquiry()}, {@code payment()}, {@code
 * echoTest()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> bergantung pada perannya, operasi dapat mengubah konfigurasi endpoint, membuat stub,
 * melakukan I/O jaringan, atau meneruskan request ke logika transaksi. Error transport tetap diteruskan sebagai
 * kontrak JAX-RPC/Axis; jangan menggandakan mapping WSDL di kelas lain.</p>
 */
public interface BillPaymentServiceSoap extends java.rmi.Remote {
    public CIMB3rdParty.BillPaymentWS.CIMB3RdParty_InquiryRs inquiry(CIMB3rdParty.BillPaymentWS.CIMB3RdParty_InquiryRq parameters) throws java.rmi.RemoteException;
    public CIMB3rdParty.BillPaymentWS.CIMB3RdParty_PaymentRs payment(CIMB3rdParty.BillPaymentWS.CIMB3RdParty_PaymentRq parameters) throws java.rmi.RemoteException;
    public CIMB3rdParty.BillPaymentWS.CIMB3RdParty_EchoRs echoTest(CIMB3rdParty.BillPaymentWS.CIMB3RdParty_EchoRq parameters) throws java.rmi.RemoteException;
}
