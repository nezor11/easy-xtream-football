package com.footballxtream.billing

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The "buy me a coffee" tip as Google Play consumable in-app products. One instance per app.
 *
 * Nothing is unlocked by a purchase: it is a voluntary tip, consumed right away so it can be given
 * again. No backend: a purchase in state PURCHASED is thanked and consumed on the device.
 *
 * Where Google Play is not available (Fire TV, boxes without Play services) the state stays
 * [State.Unavailable] and the UI keeps offering the Ko-fi QR instead.
 */
class CoffeeBilling(context: Context, private val scope: CoroutineScope) : PurchasesUpdatedListener {

    sealed interface State {
        /** Not connected yet (or reconnecting). */
        data object Connecting : State
        /** No Google Play billing here, or the products are not configured: use the QR. */
        data object Unavailable : State
        /** Products fetched and purchasable, cheapest first. */
        data class Ready(val products: List<CoffeeProduct>) : State
    }

    sealed interface Event {
        data object Thanks : Event
        data object Failed : Event
    }

    /** A purchasable coffee: [name] and [price] come localized from Play Console. */
    data class CoffeeProduct(val id: String, val name: String, val price: String, val details: ProductDetails)

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow<State>(State.Connecting)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    /** Amazon devices have no Google Play; don't even try (and don't ship a Play purchase UI there). */
    private val supportedDevice = !Build.MANUFACTURER.equals("Amazon", ignoreCase = true)

    private val client: BillingClient? = if (supportedDevice) {
        BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
    } else {
        null
    }

    private var retries = 0

    fun start() {
        val c = client
        if (c == null) {
            _state.value = State.Unavailable
            return
        }
        if (c.isReady) return
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    retries = 0
                    queryProducts()
                    consumeLeftovers()
                } else {
                    Log.i(TAG, "billing unavailable: ${result.responseCode} ${result.debugMessage}")
                    _state.value = State.Unavailable
                }
            }

            override fun onBillingServiceDisconnected() {
                // Play services can drop the connection (e.g. after an update); retry a few times.
                if (retries++ < MAX_RETRIES) {
                    scope.launch {
                        delay(RETRY_DELAY_MS * retries)
                        start()
                    }
                } else {
                    _state.value = State.Unavailable
                }
            }
        })
    }

    private fun queryProducts() {
        val c = client ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                PRODUCT_IDS.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                },
            )
            .build()
        c.queryProductDetailsAsync(params) { result, queryResult ->
            val list = queryResult.productDetailsList
            if (result.responseCode != BillingClient.BillingResponseCode.OK || list.isEmpty()) {
                Log.i(TAG, "no products: ${result.responseCode} ${result.debugMessage}")
                _state.value = State.Unavailable
                return@queryProductDetailsAsync
            }
            val products = list.mapNotNull { pd ->
                // Play Console's current product model exposes purchase options as a list; the single
                // accessor only works for "backwards compatible" ones, so try the list first.
                val offer = pd.oneTimePurchaseOfferDetailsList?.firstOrNull()
                    ?: pd.oneTimePurchaseOfferDetails
                    ?: return@mapNotNull null
                CoffeeProduct(pd.productId, pd.name, offer.formattedPrice, pd) to offer.priceAmountMicros
            }.sortedBy { it.second }.map { it.first }
            _state.value = if (products.isEmpty()) State.Unavailable else State.Ready(products)
        }
    }

    /** A tip bought but not consumed (app killed mid-flow) is consumed now, so it can be given again. */
    private fun consumeLeftovers() {
        val c = client ?: return
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        c.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { consume(it, thank = false) }
            }
        }
    }

    /** Opens the Google Play purchase sheet for [product]. Needs the Activity that hosts the UI. */
    fun buy(activity: Activity, product: CoffeeProduct) {
        val c = client ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product.details)
                        .build(),
                ),
            )
            .build()
        val result = c.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.i(TAG, "launchBillingFlow: ${result.responseCode} ${result.debugMessage}")
            _events.tryEmit(Event.Failed)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK ->
                purchases.orEmpty()
                    .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    .forEach { consume(it, thank = true) }
            // Backing out of the sheet is not an error worth a message.
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            // Already owned but never consumed: consume it now, and thank anyway.
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> consumeLeftovers()
            else -> {
                Log.i(TAG, "purchase failed: ${result.responseCode} ${result.debugMessage}")
                _events.tryEmit(Event.Failed)
            }
        }
    }

    private fun consume(purchase: Purchase, thank: Boolean) {
        val c = client ?: return
        val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        c.consumeAsync(params) { result, _ ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && thank) _events.tryEmit(Event.Thanks)
        }
    }

    private companion object {
        const val TAG = "FXBilling"
        /** Consumable products configured in Play Console, in any order (sorted by price here). */
        val PRODUCT_IDS = listOf("coffee_small", "coffee_medium", "coffee_large")
        const val MAX_RETRIES = 3
        const val RETRY_DELAY_MS = 2_000L
    }
}
