package com.turki.core.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Available subscription plans with their features and limits.
 *
 * @property id Unique identifier for the plan
 * @property code Short code for the plan (e.g., "free", "basic", "premium")
 * @property name Display name of the plan
 * @property description Description of the plan's features
 * @property priceMonthly Monthly price in smallest currency unit (kopecks/cents)
 * @property priceYearly Yearly price in smallest currency unit (with discount)
 * @property maxLessons Maximum number of lessons available (null = unlimited)
 * @property maxReviewsPerDay Maximum review sessions per day (null = unlimited)
 * @property maxPracticePerDay Maximum practice sessions per day (null = unlimited)
 * @property hasAds Whether the plan shows advertisements
 * @property hasOfflineAccess Whether offline access is available
 * @property hasPrioritySupport Whether priority support is included
 * @property features JSON-encoded additional features
 * @property isActive Whether the plan is currently offered
 * @property sortOrder Display order in the plans list
 * @property createdAt When the plan was created
 * @property updatedAt When the plan was last updated
 */
@Serializable
data class SubscriptionPlan(
    val id: Int,
    val code: String,
    val name: String,
    val description: String,
    val priceMonthly: Long,
    val priceYearly: Long,
    val maxLessons: Int?,
    val maxReviewsPerDay: Int?,
    val maxPracticePerDay: Int?,
    val hasAds: Boolean,
    val hasOfflineAccess: Boolean,
    val hasPrioritySupport: Boolean,
    val features: String?, // JSON array of additional features
    val isActive: Boolean,
    val sortOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * User's active subscription.
 *
 * @property id Unique identifier
 * @property userId User's database ID
 * @property planId Subscription plan ID
 * @property status Current status of the subscription
 * @property startedAt When the subscription started
 * @property expiresAt When the subscription expires (null = never for lifetime)
 * @property cancelledAt When the subscription was cancelled (if applicable)
 * @property autoRenew Whether the subscription auto-renews
 * @property paymentProvider Payment provider used (e.g., "stripe", "yookassa", "telegram")
 * @property paymentProviderId External subscription ID from payment provider
 * @property metadata Additional metadata (JSON)
 * @property createdAt When the record was created
 * @property updatedAt When the record was last updated
 */
@Serializable
data class UserSubscription(
    val id: Long,
    val userId: Long,
    val planId: Int,
    val status: SubscriptionStatus,
    val startedAt: Instant,
    val expiresAt: Instant?,
    val cancelledAt: Instant?,
    val autoRenew: Boolean,
    val paymentProvider: String?,
    val paymentProviderId: String?,
    val metadata: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Subscription status.
 */
@Serializable
enum class SubscriptionStatus {
    /** Subscription is active and valid */
    ACTIVE,
    /** Subscription has expired */
    EXPIRED,
    /** Subscription was cancelled by user */
    CANCELLED,
    /** Payment failed, subscription suspended */
    SUSPENDED,
    /** Trial period */
    TRIAL,
    /** Pending activation (waiting for payment) */
    PENDING
}

/**
 * Payment transaction for subscriptions.
 *
 * @property id Unique identifier
 * @property userId User's database ID
 * @property subscriptionId Related subscription ID
 * @property amount Transaction amount in smallest currency unit
 * @property currency Currency code (e.g., "RUB", "USD")
 * @property status Transaction status
 * @property paymentProvider Payment provider used
 * @property paymentProviderId External transaction ID
 * @property description Transaction description
 * @property metadata Additional metadata (JSON)
 * @property createdAt When the transaction was created
 * @property completedAt When the transaction was completed
 */
@Serializable
data class PaymentTransaction(
    val id: Long,
    val userId: Long,
    val subscriptionId: Long?,
    val amount: Long,
    val currency: String,
    val status: PaymentStatus,
    val paymentProvider: String,
    val paymentProviderId: String?,
    val description: String?,
    val metadata: String?,
    val createdAt: Instant,
    val completedAt: Instant?
)

/**
 * Payment transaction status.
 */
@Serializable
enum class PaymentStatus {
    /** Payment is pending */
    PENDING,
    /** Payment completed successfully */
    COMPLETED,
    /** Payment failed */
    FAILED,
    /** Payment was refunded */
    REFUNDED,
    /** Payment was cancelled */
    CANCELLED
}
