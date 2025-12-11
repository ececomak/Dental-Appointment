package com.ece.dental_clinic.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_datetime")
    private LocalDateTime paymentDatetime = LocalDateTime.now();

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus = "SUCCESS";

    @Column(name = "transaction_no", length = 100)
    private String transactionNo;

    public Long getId() {
        return id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getPaymentDatetime() {
        return paymentDatetime;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setPaymentDatetime(LocalDateTime paymentDatetime) {
        this.paymentDatetime = paymentDatetime;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }
}