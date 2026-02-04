package com.turki.core.database

import com.turki.core.domain.PaymentStatus
import com.turki.core.domain.PaymentTransaction
import com.turki.core.domain.SubscriptionPlan
import com.turki.core.domain.SubscriptionStatus
import com.turki.core.domain.UserSubscription
import com.turki.core.repository.SubscriptionRepository
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class SubscriptionRepositoryImpl : SubscriptionRepository {

    // Subscription Plans
    override suspend fun findAllPlans(activeOnly: Boolean): List<SubscriptionPlan> =
        DatabaseFactory.dbQuery {
            SubscriptionPlansTable.selectAll()
                .apply { if (activeOnly) where { SubscriptionPlansTable.isActive eq true } }
                .orderBy(SubscriptionPlansTable.sortOrder)
                .map { toSubscriptionPlan(it) }
        }

    override suspend fun findPlanById(id: Int): SubscriptionPlan? =
        DatabaseFactory.dbQuery {
            SubscriptionPlansTable.selectAll()
                .where { SubscriptionPlansTable.id eq id }
                .map { toSubscriptionPlan(it) }
                .singleOrNull()
        }

    override suspend fun findPlanByCode(code: String): SubscriptionPlan? =
        DatabaseFactory.dbQuery {
            SubscriptionPlansTable.selectAll()
                .where { SubscriptionPlansTable.code eq code }
                .map { toSubscriptionPlan(it) }
                .singleOrNull()
        }

    override suspend fun createPlan(plan: SubscriptionPlan): SubscriptionPlan =
        DatabaseFactory.dbQuery {
            val now = Clock.System.now()
            val id = SubscriptionPlansTable.insertAndGetId {
                it[code] = plan.code
                it[name] = plan.name
                it[description] = plan.description
                it[priceMonthly] = plan.priceMonthly
                it[priceYearly] = plan.priceYearly
                it[maxLessons] = plan.maxLessons
                it[maxReviewsPerDay] = plan.maxReviewsPerDay
                it[maxPracticePerDay] = plan.maxPracticePerDay
                it[hasAds] = plan.hasAds
                it[hasOfflineAccess] = plan.hasOfflineAccess
                it[hasPrioritySupport] = plan.hasPrioritySupport
                it[features] = plan.features
                it[isActive] = plan.isActive
                it[sortOrder] = plan.sortOrder
                it[createdAt] = now
                it[updatedAt] = now
            }
            plan.copy(id = id.value, createdAt = now, updatedAt = now)
        }

    override suspend fun updatePlan(plan: SubscriptionPlan): SubscriptionPlan =
        DatabaseFactory.dbQuery {
            val now = Clock.System.now()
            SubscriptionPlansTable.update({ SubscriptionPlansTable.id eq plan.id }) {
                it[name] = plan.name
                it[description] = plan.description
                it[priceMonthly] = plan.priceMonthly
                it[priceYearly] = plan.priceYearly
                it[maxLessons] = plan.maxLessons
                it[maxReviewsPerDay] = plan.maxReviewsPerDay
                it[maxPracticePerDay] = plan.maxPracticePerDay
                it[hasAds] = plan.hasAds
                it[hasOfflineAccess] = plan.hasOfflineAccess
                it[hasPrioritySupport] = plan.hasPrioritySupport
                it[features] = plan.features
                it[isActive] = plan.isActive
                it[sortOrder] = plan.sortOrder
                it[updatedAt] = now
            }
            plan.copy(updatedAt = now)
        }

    // User Subscriptions
    override suspend fun findActiveSubscription(userId: Long): UserSubscription? =
        DatabaseFactory.dbQuery {
            UserSubscriptionsTable.selectAll()
                .where {
                    (UserSubscriptionsTable.userId eq userId) and
                        (UserSubscriptionsTable.status eq SubscriptionStatus.ACTIVE.name)
                }
                .orderBy(UserSubscriptionsTable.createdAt, SortOrder.DESC)
                .map { toUserSubscription(it) }
                .firstOrNull()
        }

    override suspend fun findSubscriptionsByUser(userId: Long): List<UserSubscription> =
        DatabaseFactory.dbQuery {
            UserSubscriptionsTable.selectAll()
                .where { UserSubscriptionsTable.userId eq userId }
                .orderBy(UserSubscriptionsTable.createdAt, SortOrder.DESC)
                .map { toUserSubscription(it) }
        }

    override suspend fun createSubscription(subscription: UserSubscription): UserSubscription =
        DatabaseFactory.dbQuery {
            val now = Clock.System.now()
            val id = UserSubscriptionsTable.insertAndGetId {
                it[userId] = subscription.userId
                it[planId] = subscription.planId
                it[status] = subscription.status.name
                it[startedAt] = subscription.startedAt
                it[expiresAt] = subscription.expiresAt
                it[cancelledAt] = subscription.cancelledAt
                it[autoRenew] = subscription.autoRenew
                it[paymentProvider] = subscription.paymentProvider
                it[paymentProviderId] = subscription.paymentProviderId
                it[metadata] = subscription.metadata
                it[createdAt] = now
                it[updatedAt] = now
            }
            subscription.copy(id = id.value, createdAt = now, updatedAt = now)
        }

    override suspend fun updateSubscription(subscription: UserSubscription): UserSubscription =
        DatabaseFactory.dbQuery {
            val now = Clock.System.now()
            UserSubscriptionsTable.update({ UserSubscriptionsTable.id eq subscription.id }) {
                it[status] = subscription.status.name
                it[expiresAt] = subscription.expiresAt
                it[cancelledAt] = subscription.cancelledAt
                it[autoRenew] = subscription.autoRenew
                it[paymentProvider] = subscription.paymentProvider
                it[paymentProviderId] = subscription.paymentProviderId
                it[metadata] = subscription.metadata
                it[updatedAt] = now
            }
            subscription.copy(updatedAt = now)
        }

    override suspend fun cancelSubscription(subscriptionId: Long): Boolean =
        DatabaseFactory.dbQuery {
            val now = Clock.System.now()
            UserSubscriptionsTable.update({ UserSubscriptionsTable.id eq subscriptionId }) {
                it[status] = SubscriptionStatus.CANCELLED.name
                it[cancelledAt] = now
                it[autoRenew] = false
                it[updatedAt] = now
            } > 0
        }

    // Payment Transactions
    override suspend fun findTransactionsByUser(userId: Long): List<PaymentTransaction> =
        DatabaseFactory.dbQuery {
            PaymentTransactionsTable.selectAll()
                .where { PaymentTransactionsTable.userId eq userId }
                .orderBy(PaymentTransactionsTable.createdAt, SortOrder.DESC)
                .map { toPaymentTransaction(it) }
        }

    override suspend fun findTransactionById(id: Long): PaymentTransaction? =
        DatabaseFactory.dbQuery {
            PaymentTransactionsTable.selectAll()
                .where { PaymentTransactionsTable.id eq id }
                .map { toPaymentTransaction(it) }
                .singleOrNull()
        }

    override suspend fun createTransaction(transaction: PaymentTransaction): PaymentTransaction =
        DatabaseFactory.dbQuery {
            val now = Clock.System.now()
            val id = PaymentTransactionsTable.insertAndGetId {
                it[userId] = transaction.userId
                it[subscriptionId] = transaction.subscriptionId
                it[amount] = transaction.amount
                it[currency] = transaction.currency
                it[status] = transaction.status.name
                it[paymentProvider] = transaction.paymentProvider
                it[paymentProviderId] = transaction.paymentProviderId
                it[description] = transaction.description
                it[metadata] = transaction.metadata
                it[createdAt] = now
                it[completedAt] = transaction.completedAt
            }
            transaction.copy(id = id.value, createdAt = now)
        }

    override suspend fun updateTransaction(transaction: PaymentTransaction): PaymentTransaction =
        DatabaseFactory.dbQuery {
            PaymentTransactionsTable.update({ PaymentTransactionsTable.id eq transaction.id }) {
                it[status] = transaction.status.name
                it[paymentProviderId] = transaction.paymentProviderId
                it[metadata] = transaction.metadata
                it[completedAt] = transaction.completedAt
            }
            transaction
        }

    // Mappers
    private fun toSubscriptionPlan(row: ResultRow) = SubscriptionPlan(
        id = row[SubscriptionPlansTable.id].value,
        code = row[SubscriptionPlansTable.code],
        name = row[SubscriptionPlansTable.name],
        description = row[SubscriptionPlansTable.description],
        priceMonthly = row[SubscriptionPlansTable.priceMonthly],
        priceYearly = row[SubscriptionPlansTable.priceYearly],
        maxLessons = row[SubscriptionPlansTable.maxLessons],
        maxReviewsPerDay = row[SubscriptionPlansTable.maxReviewsPerDay],
        maxPracticePerDay = row[SubscriptionPlansTable.maxPracticePerDay],
        hasAds = row[SubscriptionPlansTable.hasAds],
        hasOfflineAccess = row[SubscriptionPlansTable.hasOfflineAccess],
        hasPrioritySupport = row[SubscriptionPlansTable.hasPrioritySupport],
        features = row[SubscriptionPlansTable.features],
        isActive = row[SubscriptionPlansTable.isActive],
        sortOrder = row[SubscriptionPlansTable.sortOrder],
        createdAt = row[SubscriptionPlansTable.createdAt],
        updatedAt = row[SubscriptionPlansTable.updatedAt]
    )

    private fun toUserSubscription(row: ResultRow) = UserSubscription(
        id = row[UserSubscriptionsTable.id].value,
        userId = row[UserSubscriptionsTable.userId].value,
        planId = row[UserSubscriptionsTable.planId].value,
        status = SubscriptionStatus.valueOf(row[UserSubscriptionsTable.status]),
        startedAt = row[UserSubscriptionsTable.startedAt],
        expiresAt = row[UserSubscriptionsTable.expiresAt],
        cancelledAt = row[UserSubscriptionsTable.cancelledAt],
        autoRenew = row[UserSubscriptionsTable.autoRenew],
        paymentProvider = row[UserSubscriptionsTable.paymentProvider],
        paymentProviderId = row[UserSubscriptionsTable.paymentProviderId],
        metadata = row[UserSubscriptionsTable.metadata],
        createdAt = row[UserSubscriptionsTable.createdAt],
        updatedAt = row[UserSubscriptionsTable.updatedAt]
    )

    private fun toPaymentTransaction(row: ResultRow) = PaymentTransaction(
        id = row[PaymentTransactionsTable.id].value,
        userId = row[PaymentTransactionsTable.userId].value,
        subscriptionId = row[PaymentTransactionsTable.subscriptionId]?.value,
        amount = row[PaymentTransactionsTable.amount],
        currency = row[PaymentTransactionsTable.currency],
        status = PaymentStatus.valueOf(row[PaymentTransactionsTable.status]),
        paymentProvider = row[PaymentTransactionsTable.paymentProvider],
        paymentProviderId = row[PaymentTransactionsTable.paymentProviderId],
        description = row[PaymentTransactionsTable.description],
        metadata = row[PaymentTransactionsTable.metadata],
        createdAt = row[PaymentTransactionsTable.createdAt],
        completedAt = row[PaymentTransactionsTable.completedAt]
    )
}
