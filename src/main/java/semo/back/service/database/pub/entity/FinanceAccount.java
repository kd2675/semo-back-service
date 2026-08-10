package semo.back.service.database.pub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import semo.back.service.common.jpa.CommonDateEntity;

@Entity
@Table(name = "finance_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FinanceAccount extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "finance_account_id")
    private Long financeAccountId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "created_by_club_profile_id", nullable = false)
    private Long createdByClubProfileId;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "account_type_code", nullable = false, length = 20)
    private String accountTypeCode;

    @Column(name = "provider_name", length = 100)
    private String providerName;

    @Column(name = "masked_identifier", length = 120)
    private String maskedIdentifier;

    @Column(name = "holder_name", length = 100)
    private String holderName;

    @Column(name = "usage_scope_code", nullable = false, length = 20)
    private String usageScopeCode;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "default_collection", nullable = false)
    private boolean defaultCollection;

    @Column(name = "default_expense", nullable = false)
    private boolean defaultExpense;

    public void update(
            String displayName,
            String accountTypeCode,
            String providerName,
            String maskedIdentifier,
            String holderName,
            String usageScopeCode,
            boolean defaultCollection,
            boolean defaultExpense
    ) {
        this.displayName = displayName;
        this.accountTypeCode = accountTypeCode;
        this.providerName = providerName;
        this.maskedIdentifier = maskedIdentifier;
        this.holderName = holderName;
        this.usageScopeCode = usageScopeCode;
        this.defaultCollection = defaultCollection;
        this.defaultExpense = defaultExpense;
    }

    public void deactivate() {
        this.active = false;
        this.defaultCollection = false;
        this.defaultExpense = false;
    }

    public void clearCollectionDefault() {
        this.defaultCollection = false;
    }

    public void clearExpenseDefault() {
        this.defaultExpense = false;
    }
}
