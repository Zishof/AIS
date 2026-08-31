/**
 * BillPaymentServiceSoap.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ws.billpayment.h2h.bankmandiri;

/**
 * Kontrak operasi SOAP bill-payment. Interface hasil WSDL2Java ini menetapkan signature wire yang
 * harus sama pada stub klien, skeleton server, dan implementasi endpoint.
 *
 * <p><b>Batas tanggung jawab:</b> tipe ini mendeklarasikan kontrak {@link java.rmi.Remote}. Implementasi konkret
 * bertanggung jawab atas transaksi, resource, error handling, dan efek samping; pemanggil sebaiknya bergantung
 * pada kontrak ini agar tidak menggandakan integrasi.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi domain lain ({@code reverse()}, {@code payment()}, {@code
 * inquiry()}, {@code echoTest()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> bergantung pada perannya, operasi dapat mengubah konfigurasi endpoint, membuat stub,
 * melakukan I/O jaringan, atau meneruskan request ke logika transaksi. Error transport tetap diteruskan sebagai
 * kontrak JAX-RPC/Axis; jangan menggandakan mapping WSDL di kelas lain.</p>
 */
public interface BillPaymentServiceSoap extends java.rmi.Remote {
    public ws.billpayment.h2h.bankmandiri.ReversalResponse reverse(ws.billpayment.h2h.bankmandiri.ReversalRequest request) throws java.rmi.RemoteException;
    public ws.billpayment.h2h.bankmandiri.PaymentResponse payment(ws.billpayment.h2h.bankmandiri.PaymentRequest request) throws java.rmi.RemoteException;
    public ws.billpayment.h2h.bankmandiri.InquiryResponse inquiry(ws.billpayment.h2h.bankmandiri.InquiryRequest request) throws java.rmi.RemoteException;
    public java.lang.String echoTest(java.lang.String tx) throws java.rmi.RemoteException;
}
