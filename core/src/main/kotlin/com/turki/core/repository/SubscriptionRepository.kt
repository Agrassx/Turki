package com.turki.core.repository

import com.turki.core.domain.PaymentTransaction
import com.turki.core.domain.SubscriptionPlan
import com.turki.core.domain.UserSubscription

/**
 * Repository interface for subscription and payment data access.
 */
interface SubscriptionRepository {
    // Subscription Plans
    suspend fun findAllPlans(activeOnly: Boolean = true): List<SubscriptionPlan>
    suspend fun findPlanById(id: Int): SubscriptionPlan?
    suspend fun findPlanByCode(code: String): SubscriptionPlan?
    suspend fun createPlan(plan: SubscriptionPlan): SubscriptionPlan
    suspend fun updatePlan(plan: SubscriptionPlan): SubscriptionPlan

    // User Subscriptions
    suspend fun findActiveSubscription(userId: Long): UserSubscription?
    suspend fun findSubscriptionsByUser(userId: Long): List<UserSubscription>
    suspend fun createSubscription(subscription: UserSubscription): UserSubscription
    suspend fun updateSubscription(subscription: UserSubscription): UserSubscription
    suspend fun cancelSubscription(subscriptionId: Long): Boolean

    // Payment Transactions
    suspend fun findTransactionsByUser(userId: Long): List<PaymentTransaction>
    suspend fun findTransactionById(id: Long): PaymentTransaction?
    suspend fun createTransaction(transaction: PaymentTransaction): PaymentTransaction
    suspend fun updateTransaction(transaction: PaymentTransaction): PaymentTransaction
}
