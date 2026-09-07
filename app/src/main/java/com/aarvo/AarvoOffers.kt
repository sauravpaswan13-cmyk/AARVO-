package com.aarvo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Server-ready offer model. Final eligibility and discount must be revalidated by the backend at checkout. */
data class AarvoOffer(
    val code: String,
    val title: String,
    val subtitle: String,
    val type: OfferType,
    val value: Long,
    val minimumOrderPaise: Long = 0L,
    val maxDiscountPaise: Long? = null
)

enum class OfferType { PERCENT, FIXED }

object AarvoOffers {
    val featured = listOf(
        AarvoOffer("AARVO10", "10% OFF", "Extra savings on eligible orders", OfferType.PERCENT, 10L, 99900L, 50000L),
        AarvoOffer("WELCOME100", "₹100 OFF", "Welcome offer for new shoppers", OfferType.FIXED, 10000L, 149900L),
        AarvoOffer("FREESHOP", "Special Deal", "Limited-time marketplace offer", OfferType.PERCENT, 5L, 49900L, 25000L)
    )

    fun calculateDiscountPaise(offer: AarvoOffer, subtotalPaise: Long): Long {
        if (subtotalPaise < offer.minimumOrderPaise) return 0L
        return when (offer.type) {
            OfferType.FIXED -> offer.value.coerceAtMost(subtotalPaise)
            OfferType.PERCENT -> {
                val discount = subtotalPaise * offer.value / 100L
                (offer.maxDiscountPaise?.let { discount.coerceAtMost(it) } ?: discount).coerceAtMost(subtotalPaise)
            }
        }
    }
}

@Composable
fun AarvoOffersSection(
    modifier: Modifier = Modifier,
    onOfferSelected: (AarvoOffer) -> Unit = {}
) {
    Column(modifier.fillMaxWidth()) {
        Text("Offers & Deals", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Unlock extra savings with AARVO coupons", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AarvoOffers.featured, key = { it.code }) { offer ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(offer.title, fontWeight = FontWeight.Bold)
                            Text(offer.subtitle, style = MaterialTheme.typography.bodySmall)
                            Text("Use code: ${offer.code}", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { onOfferSelected(offer) }) { Text("Apply") }
                    }
                }
            }
        }
    }
}

fun formatOfferDiscount(offer: AarvoOffer): String = when (offer.type) {
    OfferType.FIXED -> "₹${offer.value / 100} OFF"
    OfferType.PERCENT -> "${offer.value}% OFF"
}
