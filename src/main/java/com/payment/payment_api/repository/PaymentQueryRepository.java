package com.payment.payment_api.repository;

import com.payment.payment_api.dto.PaymentSearchCondition;
import com.payment.payment_api.entity.Payment;
import com.payment.payment_api.entity.QPayment;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PaymentQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QPayment payment = QPayment.payment;

    public Page<Payment> search(PaymentSearchCondition condition, Pageable pageable) {
        BooleanBuilder builder = buildCondition(condition);

        List<Payment> content = queryFactory
                .selectFrom(payment)
                .where(builder)
                .orderBy(payment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(payment.count())
                .from(payment)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private BooleanBuilder buildCondition(PaymentSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        // 상태 필터
        if (condition.getStatus() != null) {
            builder.and(payment.status.eq(condition.getStatus()));
        }
        // 최소 금액
        if (condition.getMinAmount() != null) {
            builder.and(payment.amount.goe(condition.getMinAmount()));
        }
        // 최대 금액
        if (condition.getMaxAmount() != null) {
            builder.and(payment.amount.loe(condition.getMaxAmount()));
        }
        // 시작 날짜
        if (condition.getStartDateTime() != null) {
            builder.and(payment.createdAt.goe(condition.getStartDateTime()));
        }
        // 종료 날짜
        if (condition.getEndDateTime() != null) {
            builder.and(payment.createdAt.lt(condition.getEndDateTime()));
        }
        // 고객명 부분 검색
        if (StringUtils.hasText(condition.getCustomerName())) {
            builder.and(payment.customerName.containsIgnoreCase(condition.getCustomerName()));
        }

        return builder;
    }
}
