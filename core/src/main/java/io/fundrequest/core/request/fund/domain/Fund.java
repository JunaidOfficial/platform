package io.fundrequest.core.request.fund.domain;

import io.fundrequest.core.infrastructure.repository.AbstractEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

@Table(name = "fund")
@Entity
public class Fund extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "funder")
    private String funder;

    @Column(name = "amount_in_wei")
    private BigDecimal amountInWei;

    @Column(name = "request_id")
    private Long requestId;

    protected Fund() {
    }

    Fund(String funder, BigDecimal amountInWei, Long requestId) {
        this.funder = funder;
        this.amountInWei = amountInWei;
        this.requestId = requestId;
    }

    void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getFunder() {
        return funder;
    }

    public BigDecimal getAmountInWei() {
        return amountInWei;
    }

    public Long getRequestId() {
        return requestId;
    }
}
